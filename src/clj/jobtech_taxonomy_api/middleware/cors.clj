(ns jobtech-taxonomy-api.middleware.cors)

;;  CORS function borrowed from:
;;
;;  https://github.com/lipas-liikuntapaikat/lipas/blob/master/webapp/src/clj/lipas/backend/middleware.clj

(def allow-methods "GET, PUT, PATCH, POST, DELETE, OPTIONS")

(def allow-headers "api-key, Authorization, Content-Type")

(def expose-headers "access-control-allow-origin,access-control-allow-methods,access-control-allow-headers")

(defn add-cors-headers [resp]
  (update resp :headers assoc
          "Access-Control-Allow-Origin" "*"
          "Access-Control-Allow-Methods" allow-methods
          "Access-Control-Allow-Headers" allow-headers
          "Access-Control-Expose-Headers" expose-headers))

(defn cors
  "Cross-origin Resource Sharing (CORS) middleware. Allow requests from all
   origins, all http methods and Authorization and Content-Type headers."
  [handler]
  (fn [request]
    (add-cors-headers
      (if (= :options (:request-method request))
        {:status 200}
        (handler request)))))
