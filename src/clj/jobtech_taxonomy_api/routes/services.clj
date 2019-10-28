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
;;   - adjust tests
;;   - fixa replaced-by-modell i /changes, /replaced-by-changes, /search, /private/concept


(defn log-info [message]
  (log/info message)
  )

(defn auth
  "Middleware used in routes that require authentication. If request is not
   authenticated a 401 not authorized response will be returned"
  [handler]
  (fn [request]
    (let [api-key (http/-get-header request "api-key")]
      (if (or (middleware/authenticate-user api-key)
              (middleware/authenticate-admin api-key))
        (handler request)
        (resp/unauthorized (types/map->nsmap {:error "Not authorized"}))))))

(defn authorized-private?
  [handler]
  (fn [request]
    (if (middleware/authenticate-admin  (http/-get-header request "api-key"))
      (handler request)
      (resp/unauthorized (types/map->nsmap {:error "Not authorized"})))))

(defn service-routes []
  ["/v1/taxonomy"
   {:coercion spec-coercion/coercion
    :muuntaja formats/instance
    :swagger {:id ::api
              :info {:version "0.10.0"
                     :title "Jobtech Taxonomy"
                     :description "Jobtech taxonomy services."}

              :securityDefinitions {:api_key {:type "apiKey" :name "api-key" :in "header"}}}}

   [ "" {:no-doc true}
    ["/swagger.json"
     {:get (swagger/create-swagger-handler)}]

    ["/swagger-ui*"
     {:get (swagger-ui/create-swagger-ui-handler
            {:url "/v1/taxonomy/swagger.json"
             :config {:validator-url nil
                      :operationsSorter "alpha"

                      }})}]]

   ["/main"
    {:swagger {:tags ["Main"]}

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
      :parameters {:query {:fromVersion (taxonomy/par int? "Changes from this version, exclusive"),
                           (ds/opt :toVersion) (taxonomy/par int? "Changes to this version, inclusive"),
                           (ds/opt :offset) (taxonomy/par int? "Return list offset (from 0)"),
                           (ds/opt :limit) (taxonomy/par int? "Return list limit")}}

      :get {:responses {200 {:body types/events-spec}
                        500 {:body types/error-spec}}
            :handler (fn [{{{:keys [fromVersion toVersion offset limit]} :query} :parameters}]
                       (log/info (str "GET /changes"
                                      " fromVersion:" fromVersion
                                      " toVersion: " toVersion
                                      " offset: " offset
                                      " limit: " limit))
                       {:status 200
                        :body (let [events (doall (events/get-all-events-from-version-with-pagination fromVersion toVersion offset limit))
                                    ;; This is needed to squeeze /changes :concept into the same namespace as the other's :concept
                                    renamed (map #(clojure.set/rename-keys % {:concept :changed-concept}) events)]
                                (vec (map types/map->nsmap renamed )))})}}]

    ["/concepts"
     {
      :summary      "Get concepts. Supply at least one search parameter."
      :parameters {:query {(ds/opt :id) (taxonomy/par string? "ID of concept"),
                           (ds/opt :preferred-label) (taxonomy/par string? "Textual name of concept"),
                           (ds/opt :type) (taxonomy/par string? "Restrict to concept type"),
                           (ds/opt :deprecated) (taxonomy/par boolean? "Restrict to deprecation state"),
                           (ds/opt :relation) (taxonomy/par #{"broader" "narrower" "related" "occupation_name_affinity"} "Relation type"),
                           (ds/opt :related-ids) (taxonomy/par string? "OR-restrict to these relation IDs (white space separated list)"),
                           (ds/opt :offset) (taxonomy/par int? "Return list offset (from 0)"),
                           (ds/opt :limit) (taxonomy/par int? "Return list limit"),
                           (ds/opt :version) (taxonomy/par int? "Version to use")}}
      :get {:responses {200 {:body types/concepts-spec}
                        500 {:body types/error-spec}}
            :handler (fn [{{{:keys [id preferred-label type deprecated relation
                                    related-ids offset limit version]} :query} :parameters}]
                       (log/info (str "GET /concepts "
                                      "id:" id
                                      " preferred-label:" preferred-label
                                      " type:" type
                                      " deprecated:" deprecated
                                      " offset:" offset
                                      " limit:" limit))
                       {:status 200
                        :body (vec (map types/map->nsmap (concepts/find-concepts
                                                          {:id id
                                                           :preferred-label preferred-label
                                                           :type type
                                                           :deprecated deprecated
                                                           :relation relation
                                                           :related-ids (when related-ids (list related-ids))
                                                           :offset offset
                                                           :limit limit
                                                           :version version
                                                           }

                                                          )))})}}]





    ["/replaced-by-changes"
     {
      :summary      "Show the history of concepts being replaced from a given version."
      :parameters {:query {:fromVersion (taxonomy/par int? "From taxonomy version"),
                           (ds/opt :toVersion) (taxonomy/par int? "To taxonomy version (default: latest version)")}}
      :get {:responses {200 {:body types/replaced-by-changes-spec}
                        500 {:body types/error-spec}}
            :handler (fn [{{{:keys [fromVersion toVersion]} :query} :parameters}]
                       (log/info (str "GET /replaced-by-changes"
                                      " from-version: " fromVersion
                                      " toVersion: " toVersion))
                       {:status 200
                        :body (let [events (events/get-deprecated-concepts-replaced-by-from-version fromVersion toVersion)
                                    ;; This is needed to squeeze :concept into the same namespace as the other's :concept
                                    renamed (map #(clojure.set/rename-keys % {:concept :concept-with-replace}) events)]

                                (vec (map types/map->nsmap renamed)))})}}]

    ["/concept/types"
     {
      :summary "Return a list of all taxonomy types."
      :parameters {:query {(ds/opt :version) (taxonomy/par int? "Taxonomy version (default: latest version)")}}
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

    ]



   ["/specific"


    {:swagger {:tags ["Specific Types"]
               :description "Exposes concept with detailed information such as codes from external standards."
               }

     :middleware [cors/cors auth]}

    (map

     #(types/create-detailed-endpoint % log-info concepts/find-concepts )

         types/taxonomy-extra-attributes)

    ]


   ["/suggesters"
    {:swagger {:tags ["Suggesters"]

               }

     :middleware [cors/cors auth]}

    ["/autocomplete"
     {
      :summary      "Autocomplete from query string"
      :description "Help end-users to find relevant concepts from the taxonomy"
      :parameters {:query {:query-string (taxonomy/par string? "String to search for"),
                           (ds/opt :type) (taxonomy/par string? "Type to search for"),
                           (ds/opt :relation) (taxonomy/par string? "Relation to search for"),
                           (ds/opt :related-ids) (taxonomy/par string? "List of relation IDs to search for"),
                           (ds/opt :offset) (taxonomy/par int? "Return list offset (from 0)"),
                           (ds/opt :limit) (taxonomy/par int? "Return list limit"),
                           (ds/opt :version) (taxonomy/par int? "Version to search for")}}
      :get {:responses {200 {:body types/autocomplete-spec}
                        500 {:body types/error-spec}}
            :handler (fn [{{{:keys [query-string type relation relation-ids offset limit version]}
                            :query} :parameters}]

                       (log/info (str "GET /search"
                                      " query-string:" query-string
                                      " type:" type
                                      " offset:" offset
                                      " limit:" limit
                                      " version: " version))
                       {:status 200
                        :body (vec (map types/map->nsmap
                                        (search/get-concepts-by-search
                                         query-string type nil nil offset limit version)))})}}]

    ["/parse-text"
     {
      :summary "This end point is in Alpha. Finds all concepts in a text."
      :description "This end point is in Alpha. Help end-users to find relevant concepts from the taxonomy"
      :parameters {:query {:text (taxonomy/par string? "Substring to search for")}}
      :post {:responses {200 {:body types/parse-text-spec}
                         500 {:body types/error-spec}}
             :handler (fn [{{{:keys [text]} :query} :parameters}]
                        (log/info (str "GET /parse-text text: " text ))
                        {:status 200
                         :body (vec (map types/map->nsmap (ie/parse-text text)))})}}]

    ]

   ["/private"
    {:swagger {:tags ["Private"]}
     :middleware [cors/cors auth authorized-private?]}

    ;; for now used by tests for simple private access. TODO: replace that test call, with call to delete


    ["/delete-concept"
     {
      :summary      "Retract the concept with the given ID."
      :parameters {:query {:id (taxonomy/par string? "ID of concept")}}
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
      :parameters {:query {(ds/opt :type) (taxonomy/par string? "Concept type"),
                           (ds/opt :definition) (taxonomy/par string? "Definition"),
                           (ds/opt :preferred-label) (taxonomy/par string? "Preferred label")}}
      :post {:responses {200 {:body types/ok-concept-spec}
                         409 {:body types/error-spec}
                         500 {:body types/error-spec}}
             :handler (fn [{{{:keys [type definition preferred-label]} :query} :parameters}]
                        (log/info "POST /concept")
                        (let [[result timestamp new-concept] (concepts/assert-concept type definition preferred-label)]
                          (if result
                            {:status 200 :body (types/map->nsmap {:time timestamp :concept new-concept}) }
                            {:status 409 :body (types/map->nsmap {:error "Can't create new concept since it is in conflict with existing concept."}) })))}}]

    ["/replace-concept"
     {
      :summary      "Replace old concept with a new concept."
      :parameters {:query {:old-concept-id (taxonomy/par string? "Old concept ID"),
                           :new-concept-id (taxonomy/par string? "New concept ID")}}
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
      :parameters {:query {:new-version-id (taxonomy/par int? "New version ID")}}
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
