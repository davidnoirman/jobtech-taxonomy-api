(ns jobtech-taxonomy-api.authentication-service
  (:require
   [clj-http.conn-mgr :as conn]
   [clj-http.client :as client]
   [muuntaja.core :as m]
   ))



;; (def keymanager-api-key (System/getenv "KEY_MANAGER_API_KEY"))
;; (def keymanager-url (System/getenv "KEY_MANAGER_API_URL") )



(def password (System/getenv "KEY_MANAGER_API_KEY"))

(def keymanager-url
  (str "https://" "taxonomy-apikey-reader" ":"  password  "@" "780bc2c2a86c4c8abdd6236b4e557f13.eu-west-1.aws.found.io:9243/apikeys/_search?q=_id:taxonomy"))

(def state (atom { }))


;; https://github.com/dakrone/clj-http#caching
(defn setup-connection []
  (let [cm (conn/make-reusable-conn-manager {})
        caching-client (:http-client (client/get keymanager-url
                                                 {:connection-manager cm :cache true}))
        ]
    {:cm cm
     :client caching-client
     }
    )
  )


(defn get-state []
  (let [current-state @state]
    (if (empty? current-state)
      (reset! state (setup-connection))
      current-state
      )
    )
  )


(defn call-api []
  (let [current-state (get-state)]
    (client/get keymanager-url {:connection-manager (:cm current-state) :http-client (:client current-state) :cache true} ))
  )

(defn parse-response [response]
  (m/decode "application/json" (:body response))
  )

(defn valid-keys []
  (set (map name (keys (get-in  (parse-response (call-api)) [:hits :hits 0 :_source]  ))))
  )

(defn is-valid-key? [a-key]
  (contains? (valid-keys) a-key)
  )
