(ns jobtech-taxonomy-api.db.versions
  (:refer-clojure :exclude [type])
  (:require
   [schema.core :as s]
   [datomic.client.api :as d]
   [jobtech-taxonomy-api.db.database-connection :refer :all]
   [jobtech-taxonomy-api.db.api-util :refer :all]
   [taxonomy :as types]
   [clojure.set :as set]
   )
  )

(def show-version-instance-ids
  '[:find ?inst ?version
    :in $
    :where
    [?t :taxonomy-version/id ?version ?tx]
    [?tx :db/txInstant ?inst]
    [(not= ?version 0)] ;; 0 is a dummy version to make it easier to code queries
    ]
  )


(def show-latest-version
  '[:find (max ?version)
    :in $
    :where
    [?t :taxonomy-version/id ?version]
    ]
  )

(defn- convert-response [[timestamp version]]
  {:timestamp timestamp
   :version version
   }
  )

;; get all versions
(defn get-all-versions []
  "all versions"
  (sort-by :version
           (map convert-response (d/q show-version-instance-ids (get-db))))
  )


(defn get-current-version-id []
  (:version (last (get-all-versions))))

(defn get-next-version-id []
  (inc (get-current-version-id)))

(defn is-the-new-version-id-correct? [new-version-id]
  (let [current-version (ffirst (d/q show-latest-version (get-db)))]
    (if (and (nil? current-version) (= 0 new-version-id))
      true
      (= new-version-id (inc current-version))
      )
    )
  )


(defn create-new-version [new-version-id]
  (if (is-the-new-version-id-correct? new-version-id)
    (do
      (d/transact (get-conn) {:tx-data [ {:taxonomy-version/id new-version-id}  ]})
      (last (get-all-versions))
      )
    nil
    )
  )
