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


(def s-query
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

    [?r :relation/concept-1 ?c1]

    [?r :relation/concept-2 ?c2]
    [?r :relation/type ?relation-type]

    [?c1 :concept/type ?c1-type]
    [?c2 :concept/type ?c2-type]
    ])


(def g-query
  '{:query
   {:find
    [
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
                ])]


    :in [$],
    :where
    [
     [?r :relation/concept-1 ?c1]
     [?r :relation/concept-2 ?c2]
     [?r :relation/type "substitutability"]
     ]
    }
   :args nil;;(get-db)

   :offset 0
   :limit 1}
  )
