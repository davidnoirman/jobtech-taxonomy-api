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
   [environ.core :refer [env]]
   [ring.util.http-response :as resp]
   [jobtech-taxonomy-api.middleware.formats :as formats]
   [jobtech-taxonomy-api.middleware.cors :as cors]
   [jobtech-taxonomy-api.middleware :as middleware]
   [jobtech-taxonomy-api.db.versions :as v]
   [jobtech-taxonomy-api.db.concepts :as concepts]
   [jobtech-taxonomy-api.db.events :as events]
   [jobtech-taxonomy-api.db.search :as search]
   [jobtech-taxonomy-api.db.core :as core]
   [taxonomy :as types]
   [jobtech-taxonomy-api.db.information-extraction :as ie]
   [clojure.tools.logging :as log]
   [clojure.spec.alpha :as s]
   [spec-tools.core :as st]
   [spec-tools.data-spec :as ds]
   ))

;; Status:
;;   - refine auth-merge of develop (remove below auth functions), and apikey.clj
;;   - adjust tests

(defmacro par [type desc]
  "Use this to make parameter declarations somewhat tidier."
  `(st/spec {:spec ~type :description ~desc}))

(defn auth
  "Middleware used in routes that require authentication. If request is not
   authenticated a 401 not authorized response will be returned"
  [handler]
  (fn [request]
    (if (authenticated? request)
      (handler request)
      (resp/unauthorized (types/map->nsmap {:error "Not authorized"})))))

(defn authorized-private?
  [handler]
  (fn [request]
    (if (= (http/-get-header request "api-key") (middleware/get-token :admin))
      (handler request)
      (resp/unauthorized (types/map->nsmap {:error "Not authorized"})))))

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
    {:swagger {:tags ["Public"]}

     :middleware [cors/cors auth]}

    ["/versions"
     {:summary "Show the available versions."
      :get {:responses {200 {:body types/versions-spec}
                        401 {:body types/unauthorized-spec}
                        500 {:body types/error-spec}}
            :handler (fn [{{{:keys [_]} :query} :parameters}]
                       {:status 200
                        :body (map types/map->nsmap (v/get-all-versions))})}}]

    ["/changes"
     {
      :summary      "Show the history from a given version."
      :parameters {:query {:fromVersion (par int? "Changes from this version, exclusive"),
                           (ds/opt :toVersion) (par int? "Changes to this version, inclusive"),
                           (ds/opt :offset) (par int? "Return list offset (from 0)"),
                           (ds/opt :limit) (par int? "Return list limit")}}

      :get {:responses {200 {:body types/events-spec}
                        500 {:body types/error-spec}}
            :handler (fn [{{{:keys [fromVersion toVersion offset limit]} :query} :parameters}]
                       (log/info (str "GET /changes"
                                      " fromVersion:" fromVersion
                                      " toVersion: " toVersion
                                      " offset: " offset
                                      " limit: " limit))
                       {:status 200
                        :body (vec (map types/map->nsmap (events/get-all-events-from-version-with-pagination fromVersion toVersion offset limit)))})}}]

    ["/concepts"
     {
      :summary      "Get concepts. Supply at least one search parameter."
      :parameters {:query {(ds/opt :id) (par string? "ID of concept"),
                           (ds/opt :preferredLabel) (par string? "Textual name of concept"),
                           (ds/opt :type) (par string? "Restrict to concept type"),
                           (ds/opt :deprecated) (par boolean? "Restrict to deprecation state"),
                           (ds/opt :relation) (par #{"broader" "narrower" "related" "occupation_name_affinity"} "Relation type"),
                           (ds/opt :related-ids) (par string? "OR-restrict to these relation IDs (white space separated list)"),
                           (ds/opt :offset) (par int? "Return list offset (from 0)"),
                           (ds/opt :limit) (par int? "Return list limit"),
                           (ds/opt :version) (par int? "Version to use")}}
      :get {:responses {200 {:body types/concepts-spec}
                        500 {:body types/error-spec}}
            :handler (fn [{{{:keys [id preferredLabel type deprecated relation
                                    related-ids offset limit version]} :query} :parameters}]
                       (log/info (str "GET /concepts "
                                      "id:" id
                                      " preferredLabel:" preferredLabel
                                      " type:" type
                                      " deprecated:" deprecated
                                      " offset:" offset
                                      " limit:" limit))
                       {:status 200
                        :body (vec (map types/map->nsmap (concepts/find-concepts
                                                          {:id id
                                                           :preferredLabel preferredLabel
                                                           :type type
                                                           :deprecated deprecated
                                                           :relation relation
                                                           :related-ids (list related-ids)
                                                           :offset offset
                                                           :limit limit
                                                           :version version
                                                           }

                                                          )))})}}]

    (map #(let [kwnam %
                nam (str (name kwnam))]

            [(str "/concepts/" nam)
             {
              :summary      (str "Get " nam ". Supply at least one search parameter.")
              :parameters {:query {(ds/opt :id) (par string? "ID of concept")
                                   (ds/opt :preferredLabel) (par string? "Textual name of concept")
                                   (ds/opt :type) (par string? "Restrict to concept type"),
                                   ;;(ds/opt :type) (par #{"ssyk_level_1" "ssyk_level_2" "ssyk_level_3" "ssyk_level_4" } "Restrict to concept type")
                                   (ds/opt :deprecated) (par boolean? "Restrict to deprecation state")
                                   (ds/opt :relation) (par #{"broader" "narrower" "related" "occupation_name_affinity"} "Relation type")
                                   (ds/opt :related-ids) (par string? "OR-restrict to these relation IDs (white space separated list)")

                                   (ds/opt :code) (par string? nam)
                                   (ds/opt :offset) (par int? "Return list offset (from 0)")
                                   (ds/opt :limit) (par int? "Return list limit")
                                   (ds/opt :version) (par int? "Version to use")}}
              :get {:responses {200 {:body (keyword types/taxonomy-namespace (str "concepts-" nam))}
                                500 {:body types/error-spec}}
                    :handler (fn [{{{:keys [id preferredLabel type deprecated relation
                                            related-ids offset limit version code]} :query} :parameters}]
                               (log/info (str "GET /concepts "
                                              "id:" id
                                              " preferredLabel:" preferredLabel
                                              " type:" type
                                              " deprecated:" deprecated
                                              " offset:" offset
                                              " code: " code
                                              " limit:" limit))
                               {:status 200
                                :body (vec (map types/map->nsmap
                                                (concepts/find-concepts (cond-> {:id id
                                                                                 :preferredLabel preferredLabel
                                                                                 :type type
                                                                                 :deprecated deprecated
                                                                                 :relation relation
                                                                                 :related-ids (list related-ids)
                                                                                 :offset offset
                                                                                 :limit limit
                                                                                 :version version
                                                                                 :extra-where-attributes []
                                                                                 :extra-pull-fields [kwnam]}
                                                                          code
                                                                          (update :extra-where-attributes concat {kwnam code})

                                                                          (nil? code)
                                                                          (update :extra-where-attributes concat {kwnam '_})

                                                                          )
                                                                        )
                                                ))})}}]


            )
         types/taxonomy-extra-attributes)

    ["/search"
     {
      :summary      "Autocomplete from query string"
      :parameters {:query {:q (par string? "Substring to search for"),
                           (ds/opt :type) (par string? "Type to search for"),
                           (ds/opt :relation) (par string? "Relation to search for"),
                           (ds/opt :related-ids) (par string? "List of relation IDs to search for"),
                           (ds/opt :offset) (par int? "Return list offset (from 0)"),
                           (ds/opt :limit) (par int? "Return list limit"),
                           (ds/opt :version) (par int? "Version to search for")}}
      :get {:responses {200 {:body types/search-spec}
                        500 {:body types/error-spec}}
            :handler (fn [{{{:keys [q type relation relation-ids offset limit version]}
                            :query} :parameters}]
                       (log/info (str "GET /search"
                                      " q:" q
                                      " type:" type
                                      " offset:" offset
                                      " limit:" limit
                                      " version: " version))
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
                       (log/info (str "GET /replaced-by-changes"
                                      " from-version: " fromVersion
                                      " toVersion: " toVersion))
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
      :summary "Relation types. Type 'narrower' is not listed here, as it exists as the inverse of 'broader'."
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
                         :body (vec (map types/map->nsmap (ie/parse-text text)))})}}]]


   ["/private"
    {:swagger {:tags ["Private"]}
     :middleware [cors/cors auth authorized-private?]}

    ["/delete-concept"
     {
      :summary      "Retract the concept with the given ID."
      :parameters {:query {:id (par string? "ID of concept")}}
      :delete {:responses {200 {:body types/ok-spec}
                           404 {:body types/error-spec}
                           500 {:body types/error-spec}}
               :handler (fn [{{{:keys [id]} :query} :parameters}]
                          (log/info "DELETE /concept")
                          (if (core/retract-concept id)
                            {:status 200 :body (types/map->nsmap {:message "ok"}) }
                            {:status 404 :body (types/map->nsmap {:error "not found"}) }))}}]

    ["/concept"
     {
      :summary      "Assert a new concept."
      :parameters {:query {(ds/opt :type) (par string? "Concept type"),
                           (ds/opt :definition) (par string? "Definition"),
                           (ds/opt :preferredLabel) (par string? "Preferred label")}}
      :post {:responses {200 {:body types/ok-concept-spec}
                         409 {:body types/error-spec}
                         500 {:body types/error-spec}}
             :handler (fn [{{{:keys [type definition preferredLabel]} :query} :parameters}]
                        (log/info "POST /concept")
                        (let [[result timestamp new-concept] (concepts/assert-concept type definition preferredLabel)]
                          (if result
                            {:status 200 :body (types/map->nsmap {:time timestamp :concept new-concept}) }
                            {:status 409 :body (types/map->nsmap {:error "Can't create new concept since it is in conflict with existing concept."}) })))}}]

    ["/replace-concept"
     {
      :summary      "Replace old concept with a new concept."
      :parameters {:query {:old-concept-id (par string? "Old concept ID"),
                           :new-concept-id (par string? "New concept ID")}}
      :post {:responses {200 {:body types/ok-spec}
                         404 {:body types/error-spec}
                         500 {:body types/error-spec}}
             :handler (fn [{{{:keys [old-concept-id new-concept-id]} :query} :parameters}]
                        (log/info "POST /concept")
                        (if (core/replace-deprecated-concept old-concept-id new-concept-id)
                          {:status 200 :body (types/map->nsmap {:message "ok"}) }
                          {:status 404 :body (types/map->nsmap {:error "not found"}) }))}}]

    ["/versions"
     {
      :summary "Creates a new version tag in the database."
      :parameters {:query {:new-version-id (par int? "New version ID")}}
      :post {:responses {200 {:body types/ok-spec}
                         406 {:body types/error-spec}
                         500 {:body types/error-spec}}
             :handler (fn [{{{:keys [new-version-id]} :query} :parameters}]
                        (log/info (str "POST /versions" new-version-id))
                        (let [result (v/create-new-version new-version-id)]
                          (if result
                            {:status 200 :body (types/map->nsmap {:message "A new version of the Taxonomy was created."}) }
                            {:status 406 :body (types/map->nsmap {:error (str new-version-id " is not the next valid version id!")}) }
                            )))}}]
    ]])
