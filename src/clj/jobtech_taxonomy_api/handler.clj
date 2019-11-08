(ns jobtech-taxonomy-api.handler
  (:require
   [taxonomy :as types]
   [jobtech-taxonomy-api.middleware :as middleware]
   [jobtech-taxonomy-api.routes.services :refer [service-routes]]
   [reitit.swagger-ui :as swagger-ui]
   [reitit.ring :as ring]
   [ring.middleware.content-type :refer [wrap-content-type]]
   [ring.middleware.webjars :refer [wrap-webjars]]
   [jobtech-taxonomy-api.env :refer [defaults]]
   [mount.core :as mount]
   [muuntaja.core :as m]
   [reitit.ring.middleware.parameters :as parameters]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.multipart :as multipart]
   [reitit.ring.middleware.exception :as exception]
   [reitit.ring.coercion :as coercion]
   [reitit.coercion :as compile-coercion]
   ))

(mount/defstate init-app
  :start ((do (types/generate-concept-types)
              (or (:init defaults) (fn []))))
  :stop  ((or (:stop defaults) (fn []))))


(defn handler [exception request]
  {:status 500
   :body {:message (str exception)
          ;;:type 500
          ;;:uri (:uri request)
          }})

(mount/defstate app
  :start
  (ring/ring-handler
   (ring/router
    (service-routes)
    {:data {:coercion reitit.coercion.spec/coercion
            :muuntaja m/instance
            :middleware [;; query-params & form-params
                         parameters/parameters-middleware
                         ;; content-negotiation
                         muuntaja/format-negotiate-middleware
                         ;; encoding response body
                         muuntaja/format-response-middleware
                         ;; exception handling
                         (exception/create-exception-middleware
                          (merge
                           exception/default-handlers
                           {::exception/default handler

                            ;; print stack-traces for all exceptions
                            ::exception/wrap (fn [handler e request]
                                               (.printStackTrace e)
                                               (handler e request))}))

                         ;; decoding request body
                         muuntaja/format-request-middleware
                         ;; coercing response bodys
                         coercion/coerce-response-middleware
                         ;; coercing request parameters
                         coercion/coerce-request-middleware
                         ;; multipart
                         multipart/multipart-middleware]}})
   (ring/routes
    (ring/create-resource-handler
     {:path "/"})
    (wrap-content-type (wrap-webjars (constantly nil)))
    (ring/create-default-handler))))
