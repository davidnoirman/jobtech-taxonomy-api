(ns jobtech-taxonomy-api.db.concepts
  (:refer-clojure :exclude [type])
  (:require
   [schema.core :as s]
   [datomic.client.api :as d]
   [jobtech-taxonomy-database.nano-id :as nano]
   [jobtech-taxonomy-api.db.database-connection :refer :all]
   [jobtech-taxonomy-api.db.api-util :refer :all]
   [jobtech-taxonomy-api.db.api-util :as api-util]
   [clojure.set :as set]
   ))

(comment
  "To understand the idea behind the following code read this blog post
   https://grishaev.me/en/datomic-query/ "
  )


(def concept-pull-pattern [:concept/id
                           :concept/type
                           :concept/definition
                           :concept/preferred-label
                           :concept/deprecated
                           {:concept/replaced-by [:concept/id
                                                  :concept/definition
                                                  :concept/type
                                                  :concept/preferred-label
                                                  :concept/deprecated
                                                  ]}
                           ])

(def initial-concept-query
  '{:find [(pull ?c pull-pattern)
           (sum  ?broader-relation-weight)
           (sum  ?narrower-relation-weight)
           (sum  ?related-relation-weight)
           (sum  ?substitutability-to-relation-weight)
           (sum  ?substitutability-from-relation-weight)
           ]
    :with [ ?uniqueness]
    :in [$ pull-pattern]
    :args []
    :where []
    :offset 0
    :limit -1
    })

(def relation-pull-pattern [:relation/concept-1
                            :relation/concept-2
                            :relation/type
                            :relation/description
                            :relation/substitutability-percentage
                           ])

(def initial-relation-query
  '{:find [(pull ?r pull-pattern) ]
    ;;:with [ ?uniqueness]
    :in [$ pull-pattern]
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




(defn handle-relations [query relation related-ids]

  (cond
    (= "related" relation)
    (-> query
        (update :in conj '?relation '[?related-ids ...])
        (update :args conj relation related-ids)
        (update :where conj  '[?cr :concept/id ?related-ids]
                '[or [and [?r :relation/concept-1 ?cr] [?r :relation/concept-2 ?c]]
                  [and [?r :relation/concept-1 ?c] [?r :relation/concept-2 ?cr]]]
                '[?r :relation/type ?relation])
        )

    (= "narrower" relation)
    (-> query
        (update :in conj '?relation '[?related-ids ...])
        (update :args conj "broader" related-ids)
        (update :where conj
                '[?cr :concept/id ?related-ids]
                '[?r :relation/concept-1 ?c]
                '[?r :relation/concept-2 ?cr]
                '[?r :relation/type ?relation])
        )

    (= "broader" relation)
    (-> query
        (update :in conj '?relation '[?related-ids ...])
        (update :args conj relation related-ids)
        (update :where conj
                '[?cr :concept/id ?related-ids]
                '[?r :relation/concept-1 ?cr]
                '[?r :relation/concept-2 ?c]
                '[?r :relation/type ?relation])
        )

    (= "substitutability-from" relation)
    (-> query
        (update :in conj '?relation '[?related-ids ...])
        (update :args conj "substitutability" related-ids)
        (update :where conj
                '[?cr :concept/id ?related-ids]
                '[?r :relation/concept-1 ?c]
                '[?r :relation/concept-2 ?cr]
                '[?r :relation/type ?relation])
        )

    (= "substitutability-to" relation)
    (-> query
        (update :in conj '?relation '[?related-ids ...])
        (update :args conj "substitutability" related-ids)
        (update :where conj
                '[?cr :concept/id ?related-ids]
                '[?r :relation/concept-1 ?cr]
                '[?r :relation/concept-2 ?c]
                '[?r :relation/type ?relation])
        )
    )
  )

(defn handle-extra-where-attribute [query [key value]]
  (update query :where conj ['?c key value])
  )

(defn handle-extra-where-attributes [query extra-where-attributes]
  (reduce handle-extra-where-attribute query extra-where-attributes)
  )



(defn fetch-concepts [{:keys [id preferred-label type deprecated relation related-ids offset limit db pull-pattern extra-where-attributes]}]
  {:pre [pull-pattern]}

  (cond-> initial-concept-query

    true
    (->
     (update :args conj db)
     (update :args conj pull-pattern)
     )

    id
    (-> (update :in conj '?id)
        (update :args conj id)
        (update :where conj '[?c :concept/id ?id])
        )

    preferred-label
    (-> (update :in conj '?case-insensitive-preferred-label)
        (update :args conj (api-util/str-to-pattern-lazy preferred-label))
        (update :where conj '[?c :concept/preferred-label ?preferred-label])
        (update :where conj '[(.matches ^String ?preferred-label ?case-insensitive-preferred-label)])
        )

    type
    (-> (update :in conj '[?type ...])
        (update :args conj type)
        (update :where conj '[?c :concept/type ?type])
        )

    (not-empty extra-where-attributes)
    (handle-extra-where-attributes extra-where-attributes)

    deprecated
    (-> (update :in conj '?deprecated)
        (update :args conj deprecated)
        (update :where conj '[?c :concept/deprecated ?deprecated])
        )

    (and relation related-ids)
    (handle-relations relation related-ids)

    true
    (-> (update :where conj
                '(or-join [?c
                           ?uniqueness
                           ?related-relation-weight
                           ?broader-relation-weight
                           ?narrower-relation-weight
                           ?substitutability-to-relation-weight
                           ?substitutability-from-relation-weight]
             (and
              [?broader-relation :relation/concept-1 ?c]
              [?broader-relation :relation/type "broader"]
              [(identity ?broader-relation) ?uniqueness]
              [(ground 1) ?broader-relation-weight]
              [(ground 0) ?narrower-relation-weight]
              [(ground 0) ?related-relation-weight]
              [(ground 0) ?substitutability-to-relation-weight]
              [(ground 0) ?substitutability-from-relation-weight]
              )
             (and
              [?narrower-relation :relation/concept-2 ?c]
              [?narrower-relation :relation/type "broader"]
              [(identity ?narrower-relation) ?uniqueness]
              [(ground 1) ?narrower-relation-weight]
              [(ground 0) ?broader-relation-weight]
              [(ground 0) ?related-relation-weight]
              [(ground 0) ?substitutability-to-relation-weight]
              [(ground 0) ?substitutability-from-relation-weight]
              )
             (and
              [?related-relation :relation/concept-1 ?c]
              [?related-relation :relation/type "related"]
              [(identity ?related-relation) ?uniqueness]
              [(ground 1) ?related-relation-weight]
              [(ground 0) ?narrower-relation-weight]
              [(ground 0) ?broader-relation-weight]
              [(ground 0) ?substitutability-to-relation-weight]
              [(ground 0) ?substitutability-from-relation-weight]
              )
             (and
              [?substitutability-to-relation :relation/concept-2 ?c]
              [?substitutability-to-relation :relation/type "substitutability"]
              [(identity  ?substitutability-to-relation) ?uniqueness]
              [(ground 1) ?substitutability-to-relation-weight]
              [(ground 0) ?narrower-relation-weight]
              [(ground 0) ?related-relation-weight]
              [(ground 0) ?broader-relation-weight]
              [(ground 0) ?substitutability-from-relation-weight]
              )
             (and
              [?substitutability-from-relation :relation/concept-1 ?c]
              [?substitutability-from-relation :relation/type "substitutability"]
              [(identity  ?substitutability-from-relation) ?uniqueness]
              [(ground 1) ?substitutability-from-relation-weight]
              [(ground 0) ?narrower-relation-weight]
              [(ground 0) ?related-relation-weight]
              [(ground 0) ?broader-relation-weight]
              [(ground 0) ?substitutability-to-relation-weight]
              )
             (and
              [(identity ?c) ?uniqueness]
              [(ground 0) ?broader-relation-weight]
              [(ground 0) ?related-relation-weight]
              [(ground 0) ?substitutability-relation-weight]
              [(ground 0) ?narrower-relation-weight]
              [(ground 0) ?substitutability-to-relation-weight]
              [(ground 0) ?substitutability-from-relation-weight]
              ))))

    offset
    (assoc :offset offset)

    limit
    (assoc :limit limit)

    true
    remap-query
    )
  )

(defn fetch-relations [{:keys [concept-1 concept-2 type pull-pattern db offset limit]}]
  {:pre [pull-pattern]}

  (cond-> initial-relation-query

    true
    (->
     (update :args conj db)
     (update :args conj pull-pattern)
     )

    type
    (-> (update :in conj '?type)
        (update :args conj type)
        (update :where conj '[?r :relation/type ?type])
        )

    concept-1
    (-> (update :in conj '?c1)
        (update :args conj concept-1)
        (update :where conj '[?c1e :concept/id ?c1]
                            '[?r :relation/concept-1 ?c1e])
        )

    concept-2
    (-> (update :in conj '?c2)
        (update :args conj concept-2)
        (update :where conj '[?c2e :concept/id ?c2]
                            '[?r :relation/concept-2 ?c2e])
        )

    offset
    (assoc :offset offset)

    limit
    (assoc :limit limit)

    true
    remap-query
    )
  )

(defn find-concepts-by-db
  ([args]
   (let [ result (d/q (fetch-concepts args))
         parsed-result (parse-find-concept-datomic-result result)]
     parsed-result
     ))
  )

(defn find-relations-by-db [args]
   (d/q (fetch-relations args)))

;;add extra-concept-fields to pull pattern

(defn add-find-concepts-args [args]
  (let [pull-pattern (if (:extra-pull-fields args)
                      (concat concept-pull-pattern (:extra-pull-fields args))
                      concept-pull-pattern
                      )
        db (if (:version args)
             (get-db (:version args))
             (get-db))]
    (-> args
        (assoc :db db)
        (assoc :pull-pattern pull-pattern)
        )))

(defn find-concepts
  "Supply version: Use nil as value to get the latest published database."
  [args]
  #_{:pre [(every? #(contains? args %) [:version :preferred-label])
           ]}
  (println args) ;; TODO clean println
  (find-concepts-by-db (add-find-concepts-args args)))

;;"TODO expose this as a private end point for the editor"
(defn find-concepts-including-unpublished [args]
  (find-concepts-by-db (add-find-concepts-args args)))


(defn add-find-relations-args [args]
  (let [pull-pattern relation-pull-pattern
        db (if (:version args)
             (get-db (:version args))
             (get-db))]
    (-> args
        (assoc :db db)
        (assoc :pull-pattern pull-pattern)
        )))

(defn find-relations-including-unpublished [args]
  (find-relations-by-db (add-find-relations-args args)))

(def replaced-by-concept-schema
  {:id s/Str
   :type s/Str
   :definition s/Str
   :preferred-label s/Str
   (s/optional-key :deprecated) s/Bool
   }
  )

(def number-of-relations-schema

  {:broader s/Num
   :narrower s/Num
   :related s/Num
   :substitutability-to s/Num
   :substitutability-from s/Num
   }
  )

(def concept-schema
  {:id s/Str
   :type s/Str
   :definition s/Str
   :preferred-label s/Str
   (s/optional-key :relations) number-of-relations-schema
   (s/optional-key :deprecated) s/Bool
   (s/optional-key :replaced-by)  [replaced-by-concept-schema]
   }
  )

(def find-concepts-schema
  "The response schema for /concepts. Beta for v0.9."
  [concept-schema ])

(defn assert-concept-part [type desc preferred-label]
  (let* [new-concept {:concept/id (nano/generate-new-id-with-underscore)
                      :concept/definition desc
                      :concept/type type
                      :concept/preferred-label preferred-label
                      }

         tx        [ new-concept]
         result     (d/transact (get-conn) {:tx-data tx})]
         [result new-concept]))

(defn assert-concept "" [type desc preferred-label]
  (let [existing (find-concepts-including-unpublished {:preferred-label preferred-label :type type})]
    (if (> (count existing) 0)
      [false nil]
      (let [[result new-concept] (assert-concept-part type desc preferred-label)
            timestamp (if result (nth (first (:tx-data result)) 2) nil)]
        [result timestamp  new-concept]))))


(comment

  ;; When we want to get typed data, use the dynamic pattern input
  ;; dynamic pattern input
  (d/q '[:find [(pull ?e pattern) ...]
         :in $ ?artist pattern
         :where [?e :release/artists ?artist]]
       db
       led-zeppelin
       [:release/name])


  )
