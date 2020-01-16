(ns jobtech-taxonomy-api.db.daynotes
  (:refer-clojure :exclude [type])
  (:require
   [datomic.client.api :as d]
   [schema.core :as s]
   [jobtech-taxonomy-api.db.api-util :as u]
   [jobtech-taxonomy-api.db.database-connection :refer :all]
   [jobtech-taxonomy-api.config :refer [env]]
   [mount.core :refer [defstate]]
   [clojure.set :refer :all]
   )
  )

;; TODO Utred om vi ska ha egna attribut för created updated at

(def show-concept-entity-id
  '[:find ?e ?concept-id
    :in $ ?concept-id
    :where
    [?e :concept/id ?concept-id ]
    ]
  )

(def show-concept-relations-entity-ids
  '[:find ?e ?concept-id
    :in $ ?concept-id
    :where
    [?e :concept/id ?concept-id ]
    ]
  )


#_(or-join [?c]
           [?r :relation/concept-1 ?c]
           [?r :relation/concept-2 ?c]
           )

(def fetch-all-relations-entity-ids-for-concept-query
  '[:find ?r ?inst ?added ;;?user-id
    :in $ ?concept-id
    :where
    [?c :concept/id ?concept-id]
    [?r :relation/concept-1 ?c ?tx ?added]
    #_(or [?r :relation/concept-1 ?c ?tx ?added]
        [?r :relation/concept-2 ?c ?tx ?added])
    ;;[?tx :taxonomy-user/id ?user-id]
    [?tx :db/txInstant ?inst]
    ]
  )

(def fetch-all-relations-entity-ids-for-concept-query-2
  '[:find ?r ?inst ?added ?user-id ?pl-1 ?pl-2
    :in $ ?concept-id-1
    :where
    [?c1 :concept/id ?concept-id-1]
    [?c1 :concept/preferred-label ?pl-1]
    [?c2 :concept/id ?concept-id-2]
    [?c2 :concept/preferred-label ?pl-2]

    (or-join [?r ?c1 ?c2 ?tx ?added]
             (and      [?r :relation/concept-1 ?c1 ?tx ?added]
                       [?r :relation/concept-2 ?c2 ?tx ?added]
                       )
             (and
              [?r :relation/concept-1 ?c2 ?tx ?added]
              [?r :relation/concept-2 ?c1 ?tx ?added]
              )
             )
    [?tx :taxonomy-user/id ?user-id]
    [?tx :db/txInstant ?inst]
    ]
  )
;; list transactions made by user "0"
;; (d/q '[:find ?tx  :in $ ?user-id :where [?tx :taxonomy-user/id ?user-id]] (get-db-hist (get-db))  "0")

;; (d/q '[:find ?tx ?user-id :in $ :where [?tx :taxonomy-user/id ?user-id]] (get-db-hist (get-db)) )

(defn get-concept-entity-id [concept-id]
  (ffirst (d/q show-concept-entity-id (get-db) concept-id)))

(defn get-entity-history
  "Takes an entity and shows all the transactions that touched this entity.
  Pairs well with clojure.pprint/print-table"
  [eid]
  (->> eid
       (d/q
        '[:find ?e ?attr ?v ?tx ?added ?inst ?user-id
          :in $ ?e
          :where
          [?e ?a ?v ?tx ?added]
          [?tx :db/txInstant ?inst]
          [?tx :taxonomy-user/id ?user-id]
          [?a :db/ident ?attr]]
        (get-db-hist (get-db)))
       (map #(->> %
                  (map vector [:e :a :v :tx :added :inst :user-id])
                  (into {})))
       (sort-by :tx)))


(defn group-by-transaction-and-entity-id [entity-history]
  (group-by  (juxt #(:tx %) #(:e %)) entity-history)
  )

(defn is-deprecated-event? [entity-transaction-datoms]
  (some #(and  (=  :concept/deprecated (:a %) )  (:v %)) entity-transaction-datoms )
  )

(defn is-created-event? [entity-transaction-datoms]
  (some #(and (:added %) (= :concept/id (:a %))) entity-transaction-datoms )
  )

(defn is-update-event?  [entity-transaction-datoms]
  (not (apply = (map #(:added %) entity-transaction-datoms)))
  )


(defn created-event-reducer [acc element]
  (-> acc
      (assoc (:a element) (:v element))
      (assoc :timestamp (:inst element))
      (assoc :transaction-id (:tx element ))
      (assoc :user-id (:user-id element))
      )
  )

(defn reduce-created-event [entity-transaction-datoms]
  (let [concept (reduce created-event-reducer {} entity-transaction-datoms)
        timestamp (:timestamp concept)
        transaction-id (:transaction-id concept)
        user-id (:user-id concept)
        concept-no-timestamp (-> concept
                                 (dissoc  :timestamp)
                                 (dissoc :transaction-id)
                                 (dissoc :user-id)
                                 )
        ]
    {:event-type "CREATED"
     :timestamp timestamp
     :transaction-id transaction-id
     :user-id user-id
     :concept concept-no-timestamp
     }
    )
  )

(defn handle-updated-attribute [grouped]
  (let [attribute (first grouped)
        changes (second grouped)
        added (first (map :v (filter :added changes)))
        removed (first (map :v (filter #(not (:added %)) changes)))
        ]
    {:attribute attribute
     :new-value added
     :old-value removed
     }
    )
  )

(defn reduce-updated-event [entity-transaction-datoms]
  (let [grouped (group-by :a entity-transaction-datoms)
        changes (map handle-updated-attribute grouped)
        timestamp (:inst (first entity-transaction-datoms))
        transaction-id (:tx (first entity-transaction-datoms))
        ]

    {:event-type "UPDATED"
     :timestamp timestamp
     :transaction-id transaction-id
     :changes changes
     }
    )
  )

(defn reduce-deprecated-event [entity-transaction-datoms]
  (let [first-datom (first entity-transaction-datoms)]
    {:event-type "DEPRECATED"
     :timestamp (:inst first-datom)
     :transaction-id (:tx first-datom)
     })

  )

(defn map-entity-transaction-datoms-to-event [group-of-datoms]
  (let [datoms (second group-of-datoms)]
    (cond
      (is-deprecated-event? datoms) (reduce-deprecated-event datoms)
      (is-created-event? datoms) (reduce-created-event datoms)
      (is-update-event? datoms) (reduce-updated-event datoms)
      )
   )
  )

(defn map-entity-transaction-datoms-to-events [entity-transaction-datoms]
  (map map-entity-transaction-datoms-to-event entity-transaction-datoms)
  )

(defn get-automatic-day-note-for-concept [concept-id]
  (-> concept-id
      get-concept-entity-id
      get-entity-history
      group-by-transaction-and-entity-id
      map-entity-transaction-datoms-to-events
      )
  )
