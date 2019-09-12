(ns jobtech-taxonomy-api.db.search
  (:refer-clojure :exclude [type])
  (:require
   [schema.core :as s]
   [datomic.client.api :as d]
   [jobtech-taxonomy-api.db.database-connection :refer :all]
   [jobtech-taxonomy-api.db.api-util :refer :all]
   [clojure.set :as set]
   )
  )


(defn ignore-case [string]
  (str "(?i:" string  ".*)"))


(def initial-concept-query
  '{:find [(pull ?c [:concept/id
                     :concept/type
                     :concept/preferred-label
                     ])]
    :in [$ ?q]
    :args []
    :where [
            (not [?c :concept/deprecated true])
            [?c :concept/preferred-label ?preferred-label]
            [(.matches ^String ?preferred-label ?q)]
            ]
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
;; relation related-ids
(defn fetch-concepts [q type relation related-ids offset limit version]

  (cond-> initial-concept-query

    true
    (-> (update :args conj (get-db version))
        (update :args conj (ignore-case q))

        )

    type
    (-> (update :in conj '?type)
        (update :args conj type)
        (update :where conj '[?c :concept/type ?type])
        )

    (and relation related-ids)
    (->
     (update :in conj '?relation '[?related-ids ...])
     (update :args conj relation related-ids)
     (update :where conj '[?cr :concept/id ?related-ids]
                         '[?r :relation/concept-1 ?c]
                         '[?r :relation/concept-2 ?cr]
                         '[?r :relation/type ?relation])
     )

    offset
    (assoc :offset offset)

    limit
    (assoc :limit limit)

    true
    remap-query
    )
  )


(def get-concepts-by-search-schema
  "The response schema for the query below."
  [{:id s/Str
    :type s/Str
    (s/optional-key :preferredLabel) s/Str}])


(defn get-concepts-by-search [q type relation related-ids offset limit version]
  (parse-find-concept-datomic-result (d/q (fetch-concepts q type relation related-ids offset limit version)))
  )


(comment

  (def s-query
    '[:find (pull ?c [:concept/id
                      :concept/type
                      :concept/preferred-label
                      ])
      :in $ [?cr-id ...] ?rt
      :where

      [?cr :concept/id ?cr-id]
      [?r :relation/concept-1 ?c]
      [?r :relation/concept-2 ?cr]
      [?r :relation/type ?rt]
      ])

  [
   (not [?c :concept/deprecated true])
   [?c :concept/preferred-label ?preferred-label]
   [(.matches ^String ?preferred-label ?q)]
   ]

  )
