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

(defn fetch-concepts [q type offset limit version]

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


(defn get-concepts-by-search [q type offset limit version]
  (parse-find-concept-datomic-result (d/q (fetch-concepts q type offset limit version)))
  )


(comment

  {:query
   {:find
    [(pull ?c [:concept/id :concept/type :concept/preferred-label])],
    :in [$ ?q],
    :where
    [(not [?c :concept/deprecated true])
     [?c :concept/preferred-label ?preferred-label]
     [(.matches ?preferred-label ?q)]]},
   :args
   [{:t 7, :next-t 8, :db-name "jobtech-taxonomy-henrik-dev-4", :database-id "30fa12b7-a9b8-40d7-aec0-3c13b4979946", :as-of 13194139533319, :type :datomic.client/db}
    "(?i:.*J.*)"],
   :offset 0,
   :limit -1}

  '[:find (pull ?c [:concept/id :concept/type :concept/preferred-label])
    :in $ ?c-broader-id
    :where
    [?c :concept/id ?c-id]
    [?re :relation/concept-1 ?c-broader-id]
    [?re :relation/concept-2 ?c]
    ]




  (def get-relation-graph-query
    '[:find ?c1id ?c2id
      :in $ ?relation-type
      :where
      [?re :relation/type      ?relation-type]
      [?re :relation/concept-1 ?c1]
      [?re :relation/concept-2 ?c2]
      [?c1 :concept/id ?c1id]
      [?c2 :concept/id ?c2id]])



  )
