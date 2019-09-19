(ns jobtech-taxonomy-api.routes.services
  (:require
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]
   [reitit.coercion.spec :as spec-coercion]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.multipart :as multipart]
   [reitit.ring.middleware.parameters :as parameters]
   [buddy.auth.accessrules :refer [restrict]]
   [buddy.auth.middleware :refer [wrap-authentication]]
   [buddy.auth :refer [authenticated?]]
   [buddy.auth.http :as http]
   [buddy.auth.backends :refer [jws]]
   [environ.core :refer [env]]
   [ring.util.http-response :as resp]
   [jobtech-taxonomy-api.middleware.formats :as formats]
   [jobtech-taxonomy-api.middleware.cors :as cors]
   [jobtech-taxonomy-api.db.versions :as v]
   [jobtech-taxonomy-api.db.concepts :as concepts]
   [jobtech-taxonomy-api.db.events :as events]
   [jobtech-taxonomy-api.db.search :as search]
   [jobtech-taxonomy-api.db.core :as core]
   [jobtech-taxonomy-api.types :as types]
   [jobtech-taxonomy-api.db.information-extraction :as ie]
   [clojure.tools.logging :as log]
   [clojure.spec.alpha :as s]
   [spec-tools.core :as st]
   [spec-tools.data-spec :as ds]
   ))

;;
;; Status:
;;   - conclude integration of relation and relation-ids (nils are passed to db-functions in the mean time)
;;     - how to express request parameters as drop-down choice lists
;;   - refine auth-merge of develop (remove below auth functions), and apikey.clj
;;   - catch exceptions and format error messages
;;   - adjust tests


(defmacro par [type desc]
  `(st/spec {:spec ~type :description ~desc}))

;; :parameters {:query {(ds/opt :text) (st/spec {:spec string? :description "Substring to search forX."})}}

(defn auth
  "Middleware used in routes that require authentication. If request is not
   authenticated a 401 not authorized response will be returned"
  [handler]
  (fn [request]
    (if (authenticated? request)
      (handler request)
      (resp/unauthorized {:error "Not authorized"}))))

(def token-backend
  (jws {:secret (env :api-key) :options {:alg :hs512}}))

(defn token-auth
  "Middleware used on routes requiring token authentication"
  [handler]
  (wrap-authentication handler token-backend))

(defn service-routes []
  ["/v1/taxonomy"
   {:coercion spec-coercion/coercion
    :muuntaja formats/instance
    :swagger {:id ::api
              :info {:version "0.10.0"
                     :title "Jobtech Taxonomy"
                     :description "Jobtech taxonomy services"}
              :securityDefinitions {:api_key {:type "apiKey" :name "api-key" :in "header"}}}}

   [ "" {:no-doc true}
    ["/swagger.json"
     {:get (swagger/create-swagger-handler)}]

    ["/swagger-ui*"
     {:get (swagger-ui/create-swagger-ui-handler
            {:url "/v1/taxonomy/swagger.json"
             :config {:validator-url nil}})}]]

   ["/public"
    {:swagger {:tags ["Public"]
               :auth-rules authenticated?}

     :middleware [cors/cors auth]}

    ["/versions"
     {:summary "Show the available versions."
      :get {:responses {200 {:body types/versions-spec}}
            :handler (fn [{{{:keys [_]} :query} :parameters}]
                       {:status 200
                        :body (map types/map->nsmap (v/get-all-versions))})}}]


    ["/changes"
     {
      :summary      "Show the history from a given version."
      :parameters {:query {:fromVersion (par int? "Changes from this version, exclusive"),
                           (ds/opt :toVersion) (par int? "Changes to this version, inclusive"),
                           (ds/opt :offset) (par int? "Return list offset"),
                           (ds/opt :limit) (par int? "Return list limit")}}

      :get {:responses {200 {:body types/events-spec}
                        500 {:body types/error-spec}}
            :handler (fn [{{{:keys [fromVersion toVersion offset limit]} :query} :parameters}]
                       (log/info (str "GET /changes fromVersion:" fromVersion " toVersion: " toVersion  " offset: " offset " limit: " limit))
                       {:status 200
                        :body (vec (map types/map->nsmap (events/get-all-events-from-version-with-pagination fromVersion toVersion offset limit)))})}}]


    ["/concepts"
     {
      :summary      "Get concepts. Supply at least one search parameter."
      :parameters {:query {(ds/opt :id) (par string? "ID of concept"),
                           (ds/opt :preferredLabel) (par string? "Textual name of concept"),
                           (ds/opt :type) (par string? "Restrict to concept type"),
                           (ds/opt :deprecated) (par boolean? "Restrict to deprecation state"),
                           (ds/opt :relation) (par string? "Relation type"),
                           (ds/opt :related-ids) (par string? "Restrict to these relation IDs (white space separated list)"),
                           (ds/opt :offset) (par int? "Return list offset"),
                           (ds/opt :limit) (par int? "Return list limit"),
                           (ds/opt :version) (par int? "Version to use")}}
      :get {:responses {200 {:body types/concepts-spec}
                        500 {:body types/error-spec}}
            :handler (fn [{{{:keys [id preferredLabel type deprecated relation related-ids offset limit version]} :query} :parameters}]
                       (log/info (str "GET /concepts " "id:" id " preferredLabel:" preferredLabel " type:" type
                                      " deprecated:" deprecated " offset:" offset " limit:" limit))
                       {:status 200
                        :body (vec (map types/map->nsmap (concepts/find-concepts id preferredLabel type deprecated nil nil offset limit version)))})}}]

    ["/search"
     {
      :summary      "Autocomplete from query string"
      :parameters {:query {:q (par string? "Substring to search for"),
                           (ds/opt :type) (par string? "Type to search for"),
                           (ds/opt :relation) (par string? "Relation to search for"),
                           (ds/opt :related-ids) (par string? "List of relation IDs to search for"),
                           (ds/opt :offset) (par int? "Return list offset"),
                           (ds/opt :limit) (par int? "Return list limit"),
                           (ds/opt :version) (par int? "Version to search for")}}
      :get {:responses {200 {:body types/search-spec}
                        500 {:body types/error-spec}}
            :handler (fn [{{{:keys [q type relation relation-ids offset limit version]}
                            :query} :parameters}]
                       (log/info (str "GET /search q:" q " type:" type " offset:"
                                      offset " limit:" limit  " version: " version))
                       {:status 200
                        :body (vec (map types/map->nsmap
                                        (search/get-concepts-by-search
                                         q type nil nil offset limit version)))})}}]

    ["/replaced-by-changes"
     {
      :summary      "Show the history of concepts being replaced from a given version."
      :parameters {:query {:fromVersion (par int? "From taxonomy version"),
                           (ds/opt :toVersion) (par int? "To taxonomy version (default: latest version)")}}
      :get {:responses {200 {:body types/replaced-by-changes-spec}
                        500 {:body types/error-spec}}
            :handler (fn [{{{:keys [fromVersion toVersion]} :query} :parameters}]
                       (log/info (str "GET /replaced-by-changes from-version: " fromVersion " toVersion: " toVersion))
                       {:status 200
                        :body (vec (map types/map->nsmap (events/get-deprecated-concepts-replaced-by-from-version fromVersion toVersion)))})}}]

    ["/concept/types"
     {
      :summary "Return a list of all taxonomy types."
      :parameters {:query {(ds/opt :version) (par int? "Taxonomy version (default: latest version)")}}
      :get {:responses {200 {:body types/concept-types-spec}
                        500 {:body types/error-spec}}
            :handler (fn [{{{:keys [version]} :query} :parameters}]
                       (log/info (str "GET /concept/types version: " version ))
                       {:status 200
                        :body (vec (core/get-all-taxonomy-types version))})}}]

    ["/relation/types"
     {
      :summary "Relation graphs."
      :get {:responses {200 {:body types/relation-types-spec}
                        500 {:body types/error-spec}}
            :handler (fn [{{{:keys []} :query} :parameters}]
                       (log/info (str "GET /relation/types"))
                       {:status 200
                        :body (vec (core/get-relation-types))})}}]

    ["/parse-text"
     {
      :summary "Finds all concepts in a text."
      :parameters {:query {:text (par string? "Substring to search for")}}
      :post {:responses {200 {:body types/parse-text-spec}
                         500 {:body types/error-spec}}
             :handler (fn [{{{:keys [text]} :query} :parameters}]
                        (log/info (str "GET /parse-text text: " text ))
                        {:status 200
                         :body (vec (map types/map->nsmap (ie/parse-text text)))})}}]]])
