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

(defn get-concept-entity-id [concept-id]
  (ffirst (d/q show-concept-entity-id (get-db) concept-id)))

(defn get-entity-history
  "Takes an entity and shows all the transactions that touched this entity.
  Pairs well with clojure.pprint/print-table"
  [eid]
  (->> eid
       (d/q
        '[:find ?e ?attr ?v ?tx ?added ?inst
          :in $ ?e
          :where
          [?e ?a ?v ?tx ?added]
          [?tx :db/txInstant ?inst]
          [?a :db/ident ?attr]]
        (get-db-hist (get-db)))
       (map #(->> %
                  (map vector [:e :a :v :tx :added :inst])
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
      )
  )

(defn reduce-created-event [entity-transaction-datoms]
  (let [concept (reduce created-event-reducer {} entity-transaction-datoms)
        timestamp (:timestamp concept)
        transaction-id (:transaction-id concept)
        concept-no-timestamp (-> concept
                                 (dissoc  :timestamp)
                                 (dissoc :transaction-id)
                                 )
        ]
    {:event-type "CREATED"
     :timestamp timestamp
     :transaction-id transaction-id
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

(comment
  "example data"
   (def example-event-history {[13194139533325 22390454788030565]
 [{:e 22390454788030565,
   :a :concept/id,
   :v "t1G1_sg9_Pkk",
   :tx 13194139533325,
   :added true,
   :inst #inst "2019-11-20T15:40:47.394-00:00"}
  {:e 22390454788030565,
   :a :concept/type,
   :v "skill",
   :tx 13194139533325,
   :added true,
   :inst #inst "2019-11-20T15:40:47.394-00:00"}
  {:e 22390454788030565,
   :a :concept/definition,
   :v "Hoppa som en groda",
   :tx 13194139533325,
   :added true,
   :inst #inst "2019-11-20T15:40:47.394-00:00"}
  {:e 22390454788030565,
   :a :concept/preferred-label,
   :v "Hoppning",
   :tx 13194139533325,
   :added true,
   :inst #inst "2019-11-20T15:40:47.394-00:00"}],
 [13194139533326 22390454788030565]
 [{:e 22390454788030565,
   :a :concept/preferred-label,
   :v "dansa",
   :tx 13194139533326,
   :added true,
   :inst #inst "2019-11-20T16:14:05.364-00:00"}
  {:e 22390454788030565,
   :a :concept/preferred-label,
   :v "Hoppning",
   :tx 13194139533326,
   :added false,
   :inst #inst "2019-11-20T16:14:05.364-00:00"}],
 [13194139533327 22390454788030565]
 [{:e 22390454788030565,
   :a :concept/preferred-label,
   :v "dansa",
   :tx 13194139533327,
   :added false,
   :inst #inst "2019-11-20T16:14:59.807-00:00"}
  {:e 22390454788030565,
   :a :concept/preferred-label,
   :v "dansa3",
   :tx 13194139533327,
   :added true,
   :inst #inst "2019-11-20T16:14:59.807-00:00"}],
 [13194139533458 22390454788030565]
 [{:e 22390454788030565,
   :a :concept/deprecated,
   :v true,
   :tx 13194139533458,
   :added true,
   :inst #inst "2019-12-16T15:11:34.692-00:00"}]})


  (def deprecated-entity-transaction-datoms
    [{:e 22390454788030565,
      :a :concept/deprecated,
      :v true,
      :tx 13194139533458,
      :added true,
      :inst #inst "2019-12-16T15:11:34.692-00:00"}])


  (def created-entity-transaction-datoms
    [{:e 22390454788030565,
      :a :concept/id,
      :v "t1G1_sg9_Pkk",
      :tx 13194139533325,
      :added true,
      :inst #inst "2019-11-20T15:40:47.394-00:00"}
     {:e 22390454788030565,
      :a :concept/type,
      :v "skill",
      :tx 13194139533325,
      :added true,
      :inst #inst "2019-11-20T15:40:47.394-00:00"}
     {:e 22390454788030565,
      :a :concept/definition,
      :v "Hoppa som en groda",
      :tx 13194139533325,
      :added true,
      :inst #inst "2019-11-20T15:40:47.394-00:00"}
     {:e 22390454788030565,
      :a :concept/preferred-label,
      :v "Hoppning",
      :tx 13194139533325,
      :added true,
      :inst #inst "2019-11-20T15:40:47.394-00:00"}]
    )

  (def updated-entity-transaction-datoms
    [{:e 22390454788030565,
      :a :concept/preferred-label,
      :v "dansa",
      :tx 13194139533326,
      :added true,
      :inst #inst "2019-11-20T16:14:05.364-00:00"}
     {:e 22390454788030565,
      :a :concept/preferred-label,
      :v "Hoppning",
      :tx 13194139533326,
      :added false,
      :inst #inst "2019-11-20T16:14:05.364-00:00"}]
    )

  (def two-updated-entity-transaction-datoms
    {[13194139533326 22390454788030565]
     [{:e 22390454788030565,
       :a :concept/preferred-label,
       :v "dansa",
       :tx 13194139533326,
       :added true,
       :inst #inst "2019-11-20T16:14:05.364-00:00"}
      {:e 22390454788030565,
       :a :concept/preferred-label,
       :v "Hoppning",
       :tx 13194139533326,
       :added false,
       :inst #inst "2019-11-20T16:14:05.364-00:00"}],
     [13194139533327 22390454788030565]
     [{:e 22390454788030565,
       :a :concept/preferred-label,
       :v "dansa",
       :tx 13194139533327,
       :added false,
       :inst #inst "2019-11-20T16:14:59.807-00:00"}
      {:e 22390454788030565,
       :a :concept/preferred-label,
       :v "dansa3",
       :tx 13194139533327,
       :added true,
       :inst #inst "2019-11-20T16:14:59.807-00:00"}]},
    )

  ;;(group-by-transaction-and-entity-id (get-entity-history 22390454788030565))

  )
