(ns jobtech-taxonomy-api.routes.services
  (:require
    [reitit.swagger :as swagger]
    [reitit.swagger-ui :as swagger-ui]
    [reitit.coercion.spec :as spec-coercion]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [reitit.ring.middleware.multipart :as multipart]
    [reitit.ring.middleware.parameters :as parameters]
    [jobtech-taxonomy-api.middleware.formats :as formats]
    [jobtech-taxonomy-api.db.versions :as v]
    [jobtech-taxonomy-api.types :as types]
    ))

(defn service-routes []
  ["/v1/taxonomy/public"
   {:coercion spec-coercion/coercion
    :muuntaja formats/instance
    :swagger {:id ::api}}

   ["" {:no-doc true}

    ["/swagger.json"
     {:get (swagger/create-swagger-handler)}]

    ["/swagger-ui/*"
     {:get (swagger-ui/create-swagger-ui-handler
             {:url "/v1/taxonomy/public/swagger.json" ;; FIXME can this be excluded?
              :config {:validator-url nil}})}]]

   ["/versions"
    {:get {:responses {200 {:body types/versions-spec}}
           :handler (fn [{{{:keys [_]} :multipart} :parameters}]
                      {:status 200
                       :body (vec (map types/map->nsmap (v/get-all-versions)))
                       ;; :body types/example-version-response
                       })}}]])
