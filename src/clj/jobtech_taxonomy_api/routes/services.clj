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
    ))

;;
;; Status:
;;   - unsure if reitit swagger supports the "authorize" button in swagger.
;;   - request parameters need refinement (to support optional parameters)
;;
;;

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
                     :description "Jobtech taxonomy services"}}}

   [ "" {:no-doc true}
    ["/swagger.json"
     {:get (swagger/create-swagger-handler)}]

    ["/swagger-ui*"
     {:get (swagger-ui/create-swagger-ui-handler
            {:url "/v1/taxonomy/swagger.json"
             :config {:validator-url nil}})}]]

   ["/public"
    {:swagger {:tags ["Public"]
               :auth-rules authenticated? ;; I wish this produced an Authorize button in Swagger, like in Compojure.
               }
     :middleware [cors/cors]} ;; activate buddy auth by adding: token-auth auth

    ["/versions"
    {:summary "Show the available versions."
     :get {:responses {200 {:body types/versions-spec}}
           :handler (fn [{{{:keys [_]} :query} :parameters}]
                      {:status 200
                       :body (map types/map->nsmap (v/get-all-versions))})}}]


   ["/changes"
    {
     :summary      "Show the history from a given version."
     :parameters {:query {(ds/opt :fromVersion) int?, :toVersion int?, :offset int?, :limit int?}}

     :get {:responses {200 {:body types/events-spec}
                       500 {:body types/error-spec}}
           :handler (fn [{{{:keys [fromVersion toVersion offset limit]} :query} :parameters}]
                       (log/info (str "GET /changes fromVersion:" fromVersion " toVersion: " toVersion  " offset: " offset " limit: " limit))
                       {:status 200
                       :body (vec (map types/map->nsmap (events/get-all-events-from-version-with-pagination fromVersion toVersion offset limit)))})}}]


   ["/concepts"
    {
     :summary      "Get concepts."
     :parameters {:query {:id string?, :preferredLabel string?, :type string?,
                          :deprecated boolean?, :offset int?, :limit int?, :version int?}}
     ;;:parameters {:query types/concepts-params} ;; FIXME: for optional params
     :get {:responses {200 {:body types/concepts-spec}
                       500 {:body types/error-spec}}
           :handler (fn [{{{:keys [id preferredLabel type deprecated offset limit version]} :query} :parameters}]
                      (log/info (str "GET /concepts " "id:" id " preferredLabel:" preferredLabel " type:" type " deprecated:" deprecated " offset:" offset " limit:" limit))
                       {:status 200
                        :body (vec (map types/map->nsmap (concepts/find-concepts id preferredLabel type deprecated offset limit version)))})}}]

   ["/search"
    {
     :summary      "Autocomplete from query string"
     :parameters {:query {:q string?, :type string?, :offset int?,
                          :limit int?, :version int?}}
     ;;:parameters {:query types/search-params} ;; FIXME: for optional params
     :get {:responses {200 {:body types/search-spec}
                       500 {:body types/error-spec}}
           :handler (fn [{{{:keys [q type offset limit version]} :query} :parameters}]
                      (log/info (str "GET /search q:" q " type:" type " offset:" offset " limit:" limit  " version: " version))
                       {:status 200
                        :body (vec (map types/map->nsmap (search/get-concepts-by-search q type offset limit version)))})}}]

   ["/replaced-by-changes"
    {
     :summary      "Show the history of concepts being replaced from a given version."
     :parameters {:query {:fromVersion int?, :toVersion int?}}
     ;;:parameters {:query types/search-params} ;; FIXME: for optional params
     :get {:responses {200 {:body types/replaced-by-changes-spec}
                       500 {:body types/error-spec}}
           :handler (fn [{{{:keys [fromVersion toVersion]} :query} :parameters}]
                      (log/info (str "GET /replaced-by-changes from-version: " fromVersion " toVersion: " toVersion))
                       {:status 200
                        :body (vec (map types/map->nsmap (events/get-deprecated-concepts-replaced-by-from-version fromVersion toVersion)))})}}]

   ["/concept/types"
    {
     :summary "Return a list of all taxonomy types."
     :parameters {:query {:version int?}}
     ;;:parameters {:query types/types-params} ;; FIXME: for optional params
     :get {:responses {200 {:body types/concept-types-spec}
                       500 {:body types/error-spec}}
           :handler (fn [{{{:keys [version]} :query} :parameters}]
                      (log/info (str "GET /concept/types version: " version ))
                       {:status 200
                        :body (vec (core/get-all-taxonomy-types version))})}}]

   ["/parse-text"
    {
     :summary "Finds all concepts in a text."
     :parameters {:query {:text string?}}
     ;;:parameters {:query types/types-params} ;; FIXME: for optional params
     :get {:responses {200 {:body types/parse-text-spec}
                       500 {:body types/error-spec}}
           :handler (fn [{{{:keys [text]} :query} :parameters}]
                      (log/info (str "GET /parse-text text: " text ))
                       {:status 200
                        :body (vec (map types/map->nsmap (ie/parse-text text)))})}}]

   ]])
