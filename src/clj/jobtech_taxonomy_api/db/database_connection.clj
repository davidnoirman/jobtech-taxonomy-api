(ns jobtech-taxonomy-api.db.database-connection
  (:require
    [datahike.api :as d]
    [mount.core :refer [defstate]]
    [jobtech-taxonomy-api.config :refer [env]]))




(def uri "datahike:mem://jobtech-v13")
;; I'll move the config to the env after testing
#_(def config {:backend :pg
             :host "localhost"
             :port 5432
             :username "alice"
             :password "foobar"
               :path "/pg_example"})

(def config {:backend :mem
             :host "jobtech-v13"})

#_(defn get-client []
  (d/client (:datomic-cfg env)))

#_(defstate ^{:on-reload :noop} conn
  :start (do (println (str "start:conn " (:datomic-name env))) (d/connect (get-client)  {:db-name (:datomic-name env)}))
  ;;:stop (da/release (get-conn))
  )

(defstate
  ^{:on-reload :noop} conn
  :start (do
           (println (str "start:conn " config))
           (if-not (d/database-exists? config)
             (do
               (d/create-database config)
               ;; only for local testing if database does not exist locally, remove if you connect to remote database
               (let [conn (d/connect config)] 
                 (d/transact conn (read-string (slurp "scrap/schema.edn")))
                 conn))
             (d/connect config)))
  :stop (d/release conn))

(defn get-conn []
  (d/connect config))

(def get-database-instance-from-version-query
  '[:find ?txid
    :in $ ?version
    :where
    [?t :taxonomy-version/id ?version ?txid]
    ]
  )

(def get-latest-released-database-instance-query
  '[:find (max ?txid)
    :in $
    :where
    [?t :taxonomy-version/id _ ?txid]
    ]
  )

(defn get-transaction-id-from-version [version]
  (ffirst (d/q get-database-instance-from-version-query (d/db (get-conn))  version))
  )

(defn get-latest-released-database-transaction-id []
  (ffirst (d/q get-latest-released-database-instance-query (d/db (get-conn))))
  )

;; This cannot use the conn var, as it will destroy the
;; integration tests (where the conn is made before the
;; tests fill the databases with test data).
  (defn get-db "posting nil as the version will fetch the latest released database"
  ([]
   (d/db (get-conn)))
  ([version]
   (if version
     (d/as-of (d/db (get-conn)) (get-transaction-id-from-version version))
     (d/as-of (d/db (get-conn)) (get-latest-released-database-transaction-id)))))

#_(defn get-conn "" []
  (d/connect (get-client)  {:db-name (:datomic-name env)}))
