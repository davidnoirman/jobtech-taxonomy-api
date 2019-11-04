(ns jobtech-taxonomy-api.db.graph
  (:refer-clojure :exclude [type])
  (:require
   [schema.core :as s]
   [datomic.client.api :as d]
   [jobtech-taxonomy-database.nano-id :as nano]
   [jobtech-taxonomy-api.db.database-connection :refer :all]
   [jobtech-taxonomy-api.db.api-util :refer :all]
   [jobtech-taxonomy-api.db.api-util :as api-util]
   [clojure.set :as set]
   )
  )

(defn handle-relations [query relation concept-1-type concept-2-type]
  {:pre [(contains? #{"broader" "narrower" "related" "substitutability"} relation)]}
  (cond
    (= "broader" relation)
    (-> query
        (update :in into '[?relation-type ?c1-type ?c2-type])
        (update :args into [relation concept-1-type concept-2-type])
        (update :where into '[
                              [?r :relation/concept-1 ?c1]
                              [?r :relation/concept-2 ?c2]
                              [?r :relation/type ?relation-type]
                              [?c1 :concept/type ?c1-type]
                              [?c2 :concept/type ?c2-type]
                              ])
        )

    (= "narrower" relation)
    (-> query
        (update :in into '[?relation-type ?c1-type ?c2-type])
        (update :args into ["broader" concept-1-type concept-2-type])
        (update :where into '[
                              [?r :relation/concept-1 ?c1]
                              [?r :relation/concept-2 ?c2]
                              [?r :relation/type ?relation-type]
                              [?c1 :concept/type ?c2-type]
                              [?c2 :concept/type ?c1-type]
                              ])
        )

    (= "related" relation)
    (-> query
        (update :in into '[?relation-type ?c1-type ?c2-type])
        (update :args into ["related" concept-1-type concept-2-type])
        (update :where into '[
                              [or [and [?r :relation/concept-1 ?c1] [?r :relation/concept-2 ?c2]]
                                  [and [?r :relation/concept-1 ?c2] [?r :relation/concept-2 ?c1]]]
                              [?r :relation/type ?relation-type]
                              [?c1 :concept/type ?c2-type]
                              [?c2 :concept/type ?c1-type]
                              ])
        )

    (= "substitutability" relation)
    (-> query
        (update :in into '[?relation-type ?c1-type ?c2-type])
        (update :args into ["substitutability" concept-1-type concept-2-type])
        (update :where into '[
                              [?r :relation/concept-1 ?c1]
                              [?r :relation/concept-2 ?c2]
                              [?r :relation/type ?relation-type]
                              [?c1 :concept/type ?c1-type]
                              [?c2 :concept/type ?c2-type]
                              ])
        )
    ))


#_(def s-query
  '[:find

    (pull ?r [{:relation/concept-1 [:concept/id]}
              {:relation/concept-2 [:concept/id]}
              :relation/type
              :relation/substitutability-percentage
              ])

    (pull ?c1 [:concept/id
              :concept/preferred-label
              :concept/type
               ] )
    (pull ?c2 [:concept/id
              :concept/preferred-label
              :concept/type
               ] )

    :in $ ?relation-type ?c1-type ?c2-type
    :where

    [?c1 :concept/type ?c1-type]
    [?c2 :concept/type ?c2-type]
    [?r :relation/type ?relation-type]
    [?r :relation/concept-1 ?c1]
    [?r :relation/concept-2 ?c2]


    ])


(def initial-graph-query
  '{:find [ (pull ?r [{:relation/concept-1 [:concept/id]}
                      {:relation/concept-2 [:concept/id]}
                      :relation/type
                      :relation/substitutability-percentage
                      ])

           (pull ?c1 [:concept/id
                      :concept/preferred-label
                      :concept/type
                      ] )
           (pull ?c2 [:concept/id
                      :concept/preferred-label
                      :concept/type
                      ] )]
    :in [$]
    :args []
    :where []
    :offset 0
    :limit -1
    })

(defn remap-query
  [{args :args offset :offset limit :limit :as m}]
  {:query (-> m
              (dissoc :args)
              (dissoc :offset)
              (dissoc :limit)
              )
   :args args
   :offset offset
   :limit limit
   }
  )

(defn build-graph-query [relation-type concept-1-type concept-2-type offset limit version]
  {:pre [relation-type concept-1-type concept-2-type]}

  (cond-> initial-graph-query

    true
    (-> (update :args conj (get-db version))
        (handle-relations relation-type concept-1-type concept-2-type)
        )

    offset
    (assoc :offset offset)

    limit
    (assoc :limit limit)

    true
    remap-query
    )
  )


;; (d/q (build-graph-query  "related" "skill"  "isco-level-4"  nil 3  nil))

(def initial-graph-response
  {:graph
   {:nodes []
    :edges []
    }
   }
  )

(defn create-edge [{:relation/keys [concept-1 concept-2 type substitutability-percentage]}]
  (cond-> {:source (:concept/id concept-1)
           :target (:concept/id concept-2)
           :relation type
           }
    substitutability-percentage
    (assoc :substitutability-percentage substitutability-percentage)
    )
  )

(defn create-node [{:concept/keys [id preferred-label type]}]
  {:id id
   :preferred-label preferred-label
   :type type}
  )

(defn db-graph-response-reducer [acc [relation-data concept-1 concept-2]]
  (-> acc
      (update-in [:graph :edges] #(conj % (create-edge relation-data)))
      (update-in [:graph :nodes]  #(conj % (create-node concept-1)) )
      (update-in [:graph :nodes]  #(conj % (create-node concept-2)) )
      )
  )


(defn fetch-graph [relation-type source-concept-type target-concept-type offset limit version]
  (reduce db-graph-response-reducer initial-graph-response
          (d/q (build-graph-query relation-type source-concept-type target-concept-type offset limit version)))
  )
