(ns jobtech-taxonomy-api.routes.services
  (:require
    [reitit.swagger :as swagger]
    [reitit.swagger-ui :as swagger-ui]
    [reitit.ring.coercion :as coercion]
    [reitit.coercion.spec :as spec-coercion]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [reitit.ring.middleware.multipart :as multipart]
    [reitit.ring.middleware.parameters :as parameters]
    [jobtech-taxonomy-api.middleware.formats :as formats]
    [jobtech-taxonomy-api.middleware.exception :as exception]
    [ring.util.http-response :refer :all]
    [clojure.java.io :as io]
    [clojure.spec.alpha :as sp]
    [spec-tools.json-schema :as json-schema]
    [spec-tools.core :as st]
    [spec-tools.data-spec :as ds]))

(sp/def ::date inst?)
(sp/def ::version string?)

;; define a data structure
(def versions
  {::version ::version
   ::date ::date})

;; create spec from data
(def versions-spec
  (ds/spec
   {:name ::versions  ;; set a sensible name of the response model
    :spec versions})) ;; point to the spec def

;;; List currently defined specs in this ns:
;; (keys (st/registry #"jobtech-taxonomy-api-with-spec.routes.services.*"))
;;; Validate a data against a spec:
;; (sp/valid? person-spec some-person)

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
    {:get {:responses {200 {:body versions-spec}}
           :handler (fn [{{{:keys [_]} :multipart} :parameters}]
                      {:status 200
                       :body nil})}}]])
