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

(sp/def ::name string?)
(sp/def ::street string?)
(sp/def ::city #{:tre :hki})
(sp/def ::address (sp/keys :req-un [::street ::city]))
(sp/def ::user (sp/keys :req-un [::id ::name ::address]))
(sp/def ::age pos-int?)

;; define a data structure
(def person
  {::age ::age
   :boss boolean?
   (ds/req :name) string?
   (ds/opt :description) string?
   :languages #{keyword?}
   :aliases [(ds/or {:maps {:alias string?}
                     :strings string?})]})

;; create spec from data
(def person-spec
  (ds/spec
    {:name ::person
     :spec person}))

;; example data
(def some-person
  {::age 63
   :boss true
   :name "Liisa"
   :languages #{:clj :cljs}
   :aliases [{:alias "Lissu"} "Liisu"]
   :description "Liisa is a valid boss"})

;;; List currently defined specs in this ns:
;; (keys (st/registry #"jobtech-taxonomy-api-with-spec.routes.services.*"))
;;; Validate a data against a spec:
;; (sp/valid? person-spec some-person)

(defn service-routes []
  ["/api"
   {:coercion spec-coercion/coercion
    :muuntaja formats/instance
    :swagger {:id ::api}}

   ["" {:no-doc true}

    ["/swagger.json"
     {:get (swagger/create-swagger-handler)}]

    ["/api-docs/*"
     {:get (swagger-ui/create-swagger-ui-handler
             {:url "/api/swagger.json"
;;             {:url "/test.json"
              :config {:validator-url nil}})}]]

   ["/tst"
    {:get {:responses {200 {:body person-spec}}
           :handler (fn [{{{:keys [_]} :multipart} :parameters}]
                      {:status 200
                       :body some-person})}}]])
