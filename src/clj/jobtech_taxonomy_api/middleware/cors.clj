(ns jobtech-taxonomy-api.middleware.cors)

;;  CORS function borrowed from:
;;
;;  https://github.com/lipas-liikuntapaikat/lipas/blob/master/webapp/src/clj/lipas/backend/middleware.clj

(def allow-methods "GET, PUT, PATCH, POST, DELETE, OPTIONS")

(def allow-headers "Authorization, Content-Type")

(defn add-cors-headers [resp]
  (-> resp
      (assoc-in [:headers "Access-Control-Allow-Origin"] "*")
      (assoc-in [:headers "Access-Control-Allow-Methods"] allow-methods)
      (assoc-in [:headers "Access-Control-Allow-Headers"] allow-headers)))

(defn cors
  "Cross-origin Resource Sharing (CORS) middleware. Allow requests from all
   origins, all http methods and Authorization and Content-Type headers."
  [handler]
  (fn [request]
    (let [response (handler request)]
      (add-cors-headers response))))
