(ns jobtech-taxonomy-api.webhooks
  (:require
   [jobtech-taxonomy-api.config :refer [env]]
   [clojure.tools.logging :as log]
   [clj-http.client :as client]))

(defn send-notification [client version]
  "A client is a map: {:url callback-url :headers map-of-headers}. A useful map-of-headers value could be {\"api-key\" \"hejhopp\"}."
  (let [{:keys [url headers]} client]
    (try
      (client/post url {:body (format "{\"version\": %d}" version)
                       :headers headers
                       :content-type :json
                       :socket-timeout 1000      ;; in milliseconds
                       :connection-timeout 1000  ;; in milliseconds
                       :accept :json})
      (catch Exception e (log/error {:what :uncaught-exception
                                     :exception e})))))

(defn send-notifications [clients version]
  (map #(send-notification % version) clients))

(defn get-client-list-from-conf! []
  "Set webhook-clients either in config.edn or the environment, for example:
 webhook-clients='[{:url \"https://postman-echo.com/post\" :headers {}}]'"
  (set (keys (get env :webhook-clients))))
