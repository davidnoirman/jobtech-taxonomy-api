(ns taxonomy
  (:require
    [clojure.spec.alpha :as sp]
    [spec-tools.core :as st]
    [spec-tools.data-spec :as ds]))

(def taxonomy-namespace "taxonomy")

(defn map->nsmap
  "Apply our nice namespace to the supplied structure."
  [m]
  (reduce-kv (fn [acc k v]
               (let [new-kw (if (keyword? k)
                              (keyword (str taxonomy-namespace) (name k))
                              k)
                     new-v (if (= (type v) clojure.lang.PersistentArrayMap )
                             (map->nsmap v)
                             (if (or (= (type v) clojure.lang.PersistentList )
                                     (= (type v) clojure.lang.PersistentVector )
                                     (= (type v) clojure.lang.LazySeq ))
                               (map map->nsmap v)
                               v))]
                 (assoc acc new-kw new-v)))
             {} m))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;; Output response types

;; General fundamentals
(sp/def ::id (st/spec string?))
(sp/def ::type (st/spec string?))
(sp/def ::deprecated (st/spec boolean?))
(sp/def ::preferred-label (st/spec string?))
(sp/def ::definition (st/spec string?))
(sp/def ::event-type (st/spec string?))
(sp/def ::version (st/spec int?))

(sp/def ::broader (st/spec int?))
(sp/def ::narrower (st/spec int?))
(sp/def ::related (st/spec int?))
(sp/def ::affinity (st/spec int?))


(sp/def ::concept-relations
  (st/spec
   {:name ::concept-relations
    :spec (sp/keys :req [::broader ::narrower ::related ::affinity] )}
   )
  )


;; concept
;; ssyk-concept
;; ssyk-shallow-concept








;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; ssyk-2012
(sp/def ::ssyk-code-2012 (st/spec string?))



(sp/def ::concept-ssyk-shallow
  (st/spec
   {:name ::concept-ssyk-shallow
    :spec (sp/keys :req [::id ::type ::ssyk-code-2012]
                   :opt [::definition ::deprecated ::preferred-label ::concept-relations])}))

(sp/def ::replaced-by-ssyk
  (ds/spec
   {:name ::replaced-by-ssyk    :spec (sp/coll-of ::concept-ssyk-shallow )}))

(sp/def ::concept-ssyk
  (ds/spec
   {:name ::concept-ssyk
    :spec (sp/keys :req [::id ::type ::ssyk-code-2012 ::preferred-label]
                   :opt [::definition ::deprecated ::replaced-by-ssyk ::concept-relations])}))

(sp/def ::concepts-ssyk
  (ds/spec
   {:name ::concepts-ssyk
    :spec (sp/coll-of ::concept-ssyk )}))

(def concepts-spec-ssyk-2012 ::concepts-ssyk)

;; eures
(sp/def ::eures-code-2014 (st/spec string?))

(sp/def ::concept-employment-duration-shallow
  (st/spec
   {:name ::concept-employment-duration-shallow
    :spec (sp/keys :req [::id ::type ::eures-code-2014]
                   :opt [::definition ::deprecated ::preferred-label ::concept-relations])}))

(sp/def ::replaced-by-employment-duration
   (ds/spec
    {:name ::replaced-by-employment-duration
     :spec (sp/coll-of ::concept-employment-duration-shallow )}))

(sp/def ::concept-employment-duration
   (ds/spec
    {:name ::concept-employment-duration
     :spec (sp/keys :req [::id ::type ::eures-code-2014 ::preferred-label]
                    :opt [::definition ::deprecated ::replaced-by-employment-duration ::concept-relations])}))

(sp/def ::concepts-employment-duration
   (ds/spec
    {:name ::concepts-employment-duration
     :spec (sp/coll-of ::concept-employment-duration)}))

(def concepts-spec-eures-code ::concepts-employment-duration)


;; nuts-level-3
(sp/def ::nuts-level-3-code-2013 (st/spec string?))
(sp/def ::national-nuts-level-3-code-2019 (st/spec string?))


(sp/def ::concept-region-shallow
  (st/spec
   {:name ::concept-region-shallow
    :spec (sp/keys :req [::id ::nuts-level-3-code-2013 ::type]
                   :opt [::definition ::deprecated ::preferred-label ::concept-relations ::national-nuts-level-3-code-2019])}))

 (sp/def ::replaced-by-region
   (ds/spec
    {:name ::replaced-by-region
     :spec (sp/coll-of ::concept-region-shallow )}))

 (sp/def ::concept-region
   (ds/spec
    {:name ::concept-region
     :spec (sp/keys :req [::id ::type ::nuts-level-3-code-2013 ::preferred-label]
                    :opt [::definition ::deprecated ::replaced-by-region ::concept-relations
                          ::national-nuts-level-3-code-2019
                          ])}))

 (sp/def ::concepts-region
   (ds/spec
    {:name ::concepts-region
     :spec (sp/coll-of ::concept-region)}))

 (def concepts-spec-nuts-level-3-code ::concepts-region)

;; country
(sp/def ::iso-3166-1-alpha-3-2013 (st/spec string?))
(sp/def ::iso-3166-1-alpha-2-2013 (st/spec string?))

(sp/def ::concept-country-shallow
  (st/spec
   {:name ::concept-country-shallow
    :spec (sp/keys :req [::id  ::iso-3166-1-alpha-3-2013  ::iso-3166-1-alpha-2-2013 ::type]
                   :opt [::definition ::deprecated ::preferred-label ::concept-relations])}))

 (sp/def ::replaced-by-country
   (ds/spec
    {:name ::replaced-by-country
     :spec (sp/coll-of ::concept-country-shallow)}))

 (sp/def ::concept-country
   (ds/spec
    {:name ::concept-country
     :spec (sp/keys :req [::id ::type  ::iso-3166-1-alpha-3-2013  ::iso-3166-1-alpha-2-2013 ::preferred-label]
                    :opt [::definition ::deprecated ::replaced-by-country ::concept-relations])}))

 (sp/def ::concepts-country
   (ds/spec
    {:name ::concepts-country
     :spec (sp/coll-of ::concept-country)}))

 (def concepts-spec-country-code ::concepts-country)

;; id ;; TODO: make completions

(sp/def ::concept-id-shallow
  (st/spec
   {:name ::concept-id-shallow
    :spec (sp/keys :req [::id ::type]
                   :opt [::definition ::deprecated ::preferred-label ::concept-relations])}))

 (sp/def ::replaced-by-id
   (ds/spec
    {:name ::replaced-by-id
     :spec (sp/coll-of ::concept-id-shallow )}))

 (sp/def ::concept-id
   (ds/spec
    {:name ::concept-id
     :spec (sp/keys :req [::id ::type ::preferred-label]
                    :opt [::definition ::deprecated ::replaced-by-id ::concept-relations])}))

 (sp/def ::concepts-id
   (ds/spec
    {:name ::concepts-id
     :spec (sp/coll-of ::concept-id )}))

 (def concepts-spec-id ::concepts-id)

;; isco-08
(sp/def ::isco-code-08 (st/spec string?))

(sp/def ::concept-isco-shallow
  (st/spec
   {:name ::concept-isco-shallow
    :spec (sp/keys :req [::id ::isco-code-08 ::type]
                   :opt [::definition ::deprecated ::preferred-label ::concept-relations])}))

 (sp/def ::replaced-by-isco
   (ds/spec
    {:name ::replaced-by-isco
     :spec (sp/coll-of ::concept-isco-shallow)}))

 (sp/def ::concept-isco
   (ds/spec
    {:name ::concept-isco
     :spec (sp/keys :req [::id ::type ::isco-code-08 ::preferred-label]
                    :opt [::definition ::deprecated ::replaced-by-isco ::concept-relations])}))

 (sp/def ::concepts-isco
   (ds/spec
    {:name ::concepts-isco
     :spec (sp/coll-of ::concept-isco )}))

 (def concepts-spec-isco-08 ::concepts-isco)

;; education-field-code-2020
(sp/def ::sun-education-field-code-2020 (st/spec string?))

(sp/def ::concept-sun-education-field-shallow
  (st/spec
   {:name ::concept-sun-education-field-shallow
    :spec (sp/keys :req [::id ::sun-education-field-code-2020 ::type]
                   :opt [::definition ::deprecated ::preferred-label ::concept-relations])}))

 (sp/def ::replaced-by-sun-education-field
   (ds/spec
    {:name ::replaced-by-sun-education-field
     :spec (sp/coll-of ::concept-sun-education-field-shallow )}))

 (sp/def ::concept-sun-education-field
   (ds/spec
    {:name ::concept-sun-education-field
     :spec (sp/keys :req [::id ::type ::sun-education-field-code-2020 ::preferred-label]
                    :opt [::definition ::deprecated ::replaced-by-sun-education-field ::concept-relations])}))

 (sp/def ::concepts-sun-education-field
   (ds/spec
    {:name ::concepts-sun-education-field
     :spec (sp/coll-of ::concept-sun-education-field )}))

 (def concepts-spec-sun-education-field-code-2020 ::concepts-sun-education-field)

;; sun-education-level-code-2020
(sp/def ::sun-education-level-code-2020 (st/spec string?))

(sp/def ::concept-sun-education-level-shallow
  (st/spec
   {:name ::concept-sun-education-level-shallow
    :spec (sp/keys :req [::id ::sun-education-level-code-2020 ::type]
                   :opt [::definition ::deprecated ::preferred-label ::concept-relations])}))

 (sp/def ::replaced-by-sun-education-level
   (ds/spec
    {:name ::replaced-by-sun-education-level
     :spec (sp/coll-of ::concept-sun-education-level-shallow )}))

 (sp/def ::concept-sun-education-level
   (ds/spec
    {:name ::concept-sun-education-level
     :spec (sp/keys :req [::id ::type ::sun-education-level-code-2020 ::preferred-label]
                    :opt [::definition ::deprecated ::replaced-by-sun-education-level ::concept-relations])}))

 (sp/def ::concepts-sun-education-level
   (ds/spec
    {:name ::concepts-sun-education-level
     :spec (sp/coll-of ::concept-sun-education-level)}))

 (def concepts-spec-sun-education-level-code-2020 ::concepts-sun-education-level)

;; sni-level-code
(sp/def ::sni-level-code-2007 (st/spec string?))

(sp/def ::concept-sni-level-shallow
  (st/spec
   {:name ::concept-sni-level-shallow
    :spec (sp/keys :req [::id ::sni-level-code-2007 ::type]
                   :opt [::definition ::deprecated ::preferred-label ::concept-relations])}))

 (sp/def ::replaced-by-sni-level
   (ds/spec
    {:name ::replaced-by-sni-level
     :spec (sp/coll-of ::concept-sni-level-shallow )}))

 (sp/def ::concept-sni-level
   (ds/spec
    {:name ::concept-sni-level
     :spec (sp/keys :req [::id ::type ::sni-level-code-2007 ::preferred-label]
                    :opt [::definition ::deprecated ::replaced-by-sni-level ::concept-relations])}))

 (sp/def ::concepts-sni-level
   (ds/spec
    {:name ::concepts-sni-level
     :spec (sp/coll-of ::concept-sni-level )}))

 (def concepts-spec-sni-level-code ::concepts-sni-level)



;; language




;;:pull-expression :concept.external-standard/
;;}

;; {:query-field "iso-639-3-alpha-3-2007"

(sp/def ::iso-639-3-alpha-2-2007 (st/spec string?))
(sp/def ::iso-639-3-alpha-3-2007 (st/spec string?))

(sp/def ::concept-language-shallow
  (st/spec
   {:name ::concept-language-shallow
    :spec (sp/keys :req [::id ::iso-639-3-alpha-2-2007 ::iso-639-3-alpha-3-2007 ::type ::preferred-label]
                   :opt [::definition ::deprecated  ::concept-relations])}))

(sp/def ::replaced-by-language
  (ds/spec
   {:name ::replaced-by-language
    :spec (sp/coll-of ::concept-language-shallow )}))

(sp/def ::concept-language
  (ds/spec
   {:name ::concept-language
    :spec (sp/keys :req [::id ::type  ::iso-639-3-alpha-2-2007 ::iso-639-3-alpha-3-2007 ::preferred-label]
                   :opt [::definition ::deprecated ::replaced-by-language ::concept-relations])}))

(sp/def ::concepts-language
  (ds/spec
   {:name ::concepts-language
    :spec (sp/coll-of ::concept-language )}))

(def concepts-spec-sni-level-code ::concepts-language)




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn generate-concept-types [] )


;; /versions
(sp/def ::ver
  (ds/spec
   {:name ::ver
    :spec {::version int?
           ::timestamp inst?}}))

(sp/def ::versions
  (ds/spec
   {:name ::versions
    :spec (sp/coll-of ::ver )}))

(def versions-spec ::versions)

;; /changes
(sp/def ::concept
  (st/spec
   {:name ::concept
    :spec (sp/keys :req [::id ::type]
                   :opt [::definition ::deprecated ::preferred-label])}))

(sp/def ::event
  (ds/spec
   {:name ::event
    :spec (sp/keys :req [::event-type
                         ::version
                         ::concept])}))

(sp/def ::events
  (ds/spec
   {:name ::events
    :spec (sp/coll-of ::event )}))

(def events-spec ::events)

;; /concepts

(sp/def ::replaced-by
  (ds/spec
   {:name ::replaced-by
    :spec (sp/coll-of ::concept )}))

(sp/def ::concept-with-replace
  (ds/spec
   {:name ::concept-with-replace
    :spec (sp/keys :req [::id ::type ::preferred-label]
                   :opt [::definition ::deprecated ::replaced-by])}))

(sp/def ::concepts
  (ds/spec
   {:name ::concepts
    :spec (sp/coll-of ::concept-with-replace )}))

(def concepts-spec ::concepts)

;; /search

(sp/def ::search-result
  (ds/spec
   {:name ::search-result
    :spec (sp/keys :req [::id ::type ::preferred-label])}))

(sp/def ::search-results
  (ds/spec
   {:name ::search-results
    :spec (sp/coll-of ::search-result )}))

(def search-spec ::search-results)

;; /replaced-by-changes

(sp/def ::concept
  (ds/spec
   {:name ::concept
    :spec ::concept-with-replace}))

(sp/def ::replaced-by-change
  (ds/spec
   {:name ::replaced-by-change
    :spec (sp/keys :req [::version ::concept])}))

(sp/def ::replaced-by-changes
  (ds/spec
   {:name ::replaced-by-changes
    :spec (sp/coll-of ::replaced-by-change )}))

(def replaced-by-changes-spec ::replaced-by-changes)

;; /concept/types

(sp/def ::concept-types
  (ds/spec
   {:name ::concept-types
    :spec (sp/coll-of string? )}))

(def concept-types-spec ::concept-types)

;; /relation/types

(sp/def ::relation-types
  (ds/spec
   {:name ::relation-types
    :spec (sp/coll-of string? )}))

(def relation-types-spec ::relation-types)

;; /parse-text

(sp/def ::concepts-without-replace
  (ds/spec
   {:name ::concepts-without-replace
    :spec (sp/coll-of ::concept )}))

(def parse-text-spec ::concepts-without-replace)




;; Error message
(sp/def ::err (ds/spec {:name "error"
                        :spec {::error (st/spec string?)}}))
(def error-spec ::err)

(sp/def ::ok (ds/spec {:name "ok"
                       :spec {::message (st/spec string?)}}))
(def ok-spec ::ok)

(sp/def ::ok-concept (ds/spec {:name "ok"
                               :spec {::time inst?
                                      ::concept ::concept }}))
(def ok-concept-spec ::ok-concept)

(sp/def ::unauthorized (ds/spec {:name "unauthorized"
                                 :spec {::error (st/spec string?)}}))
(def unauthorized-spec ::unauthorized)


(declare koncept-spec)

(sp/def :concept/id (st/spec string?))
(sp/def :concept/type (st/spec string?))
(sp/def :concept/deprecated (st/spec boolean?))
(sp/def :concept/preferred-label (st/spec string?))
(sp/def :concept/definition (st/spec string?))

(def koncept
  {(ds/req :id) :concept/id
   (ds/req :type) :concept/type
   (ds/req :preferred-label) :concept/preferred-label
   (ds/opt :definition) :concept/definition
   (ds/opt :deprecated) :concept/deprecated
   (ds/opt :replaced-by) koncept-spec
   }
  )

(def koncept-spec
  (ds/spec
   {:name :taxonomy/Koncept
    :spec koncept
    }
   )
  )

;; TODO add language

;; TODO FIX implicit driving licence spec!!


;; driving-licence
(sp/def ::driving-licence-code-2013 (st/spec string?))

(sp/def ::concept-without-replace-driving-licence
  (st/spec
   {:name ::concept-without-replace-driving-licence
    :spec (sp/keys :req [::id ::type ::driving-licence-code-2013]
                   :opt [::definition ::deprecated ::preferred-label ::concept-relations])}))

(sp/def ::replaced-by-driving-licence
  (ds/spec
   {:name ::replaced-by-driving-licence
    :spec (sp/coll-of ::concept-without-replace-driving-licence )}))

(sp/def ::concept-driving-licence
  (ds/spec
   {:name ::concept-driving-licence
    :spec (sp/keys :req [::id ::type ::driving-licence-code-2013 ::preferred-label]
                   :opt [::definition ::deprecated ::replaced-by-driving-licence ::concept-relations])}))

(sp/def ::concepts-driving-licence
  (ds/spec
   {:name ::concepts-driving-licence
    :spec (sp/coll-of ::concept-driving-licence )}))

(def concepts-spec-driving-licence-code ::concepts-driving-licence)







(def taxonomy-extra-attributes
  (sort-by :endpoint-name
           [
            {:endpoint-name "driving-licence"
             :extra-attributes [{:query-field "driving-licence-code-2013"
                                 :where-field :concept.external-standard/driving-licence-code-2013
                                 :pull-expression :concept.external-standard/driving-licence-code-2013
                                 }

                                {:pull-expression {:concept.external-standard/implicit-driving-licences [:concept/id
                                                                                       :concept.external-standard/driving-licence-code-2013]}

                                 }
                                ]
             }

            {:endpoint-name "ssyk"
             :extra-attributes [{:query-field "ssyk-2012"
                                 :where-field :concept.external-standard/ssyk-code-2012
                                 :pull-expression :concept.external-standard/ssyk-code-2012
                                 }]}

            {:endpoint-name "employment-duration"
             :extra-attributes [{:query-field "eures-code-2014"
                                 :where-field :concept.external-standard/eures-code-2014
                                 :pull-expression :concept.external-standard/eures-code-2014
                                 }]
             }

            {:endpoint-name "region"
             :extra-attributes [{:query-field "nuts-level-3-code-2013"
                                 :where-field :concept.external-standard/nuts-level-3-code-2013
                                 :pull-expression :concept.external-standard/nuts-level-3-code-2013
                                 }

                                {:query-field "national-nuts-level-3-code-2019"
                                 :where-field :concept.external-standard/national-nuts-level-3-code-2019
                                 :pull-expression :concept.external-standard/national-nuts-level-3-code-2019
                                 }
                                ]
             }

            {:endpoint-name "country"
             :extra-attributes [
                                {:query-field "iso-3166-1-alpha-2-2013"
                                 :where-field :concept.external-standard/iso-3166-1-alpha-2-2013
                                 :pull-expression :concept.external-standard/iso-3166-1-alpha-2-2013
                                 }

                                {:query-field "iso-3166-1-alpha-3-2013"
                                 :where-field :concept.external-standard/iso-3166-1-alpha-3-2013
                                 :pull-expression :concept.external-standard/iso-3166-1-alpha-3-2013}
                                ]
             }

            {:endpoint-name "isco"
             :extra-attributes [{:query-field "isco-08"
                                 :where-field :concept.external-standard/isco-code-08
                                 :pull-expression :concept.external-standard/isco-code-08
                                 }]
             }

            {:endpoint-name "sun-education-field"
             :extra-attributes [{:query-field "sun-education-field-code-2020"
                                 :where-field :concept.external-standard/sun-education-field-code-2020
                                 :pull-expression :concept.external-standard/sun-education-field-code-2020
                                 }]
             }


            {:endpoint-name "sun-education-level"
             :extra-attributes [{:query-field "sun-education-level-code-2020"
                                 :where-field  :concept.external-standard/sun-education-level-code-2020
                                 :pull-expression :concept.external-standard/sun-education-level-code-2020
                                 }]
             }


            {:endpoint-name "sni-level"
             :extra-attributes [{:query-field "sni-level-code-2007"
                                 :where-field :concept.external-standard/sni-level-code-2007
                                 :pull-expression :concept.external-standard/sni-level-code-2007
                                 }]
             }

            {:endpoint-name "language"
             :extra-attributes [{:query-field "iso-639-3-alpha-2-2007"
                                 :where-field :concept.external-standard/iso-639-3-alpha-2-2007
                                 :pull-expression :concept.external-standard/iso-639-3-alpha-2-2007
                                 }

                                {:query-field "iso-639-3-alpha-3-2007"
                                 :where-field :concept.external-standard/iso-639-3-alpha-3-2007
                                 :pull-expression :concept.external-standard/iso-639-3-alpha-3-2007
                                 }
                                ]
             }

            ]))




;; TODO add language


(defmacro par [type desc]
  "Use this to make parameter declarations somewhat tidier."
  `(st/spec {:spec ~type :description ~desc}))


(def detailed-enpoint-query-base
  {
   (ds/opt :id) (par string? "ID of concept")
   (ds/opt :preferred-label) (par string? "Textual name of concept")
   (ds/opt :type) (par string? "Restrict to concept type"),
   ;;(ds/opt :type) (par #{"ssyk_level_1" "ssyk_level_2" "ssyk_level_3" "ssyk_level_4" } "Restrict to concept type")
   (ds/opt :deprecated) (par boolean? "Restrict to deprecation state")
   (ds/opt :relation) (par #{"broader" "narrower" "related" "occupation_name_affinity"} "Relation type")
   (ds/opt :related-ids) (par string? "OR-restrict to these relation IDs (white space separated list)")

   ;;   (ds/opt :code) (par string? name) ;; TODO Create one for each attribute
   (ds/opt :offset) (par int? "Return list offset (from 0)")
   (ds/opt :limit) (par int? "Return list limit")
   (ds/opt :version) (par int? "Version to use")

   }
  )


(defn create-extra-query-spec-field [query-field-name]
  { (ds/opt (keyword query-field-name)) (par string? query-field-name)}
  )

(defn add-extra-queries [extra-query-attributes]
  (let [query-field-names   (filter #(:query-field %) extra-query-attributes)]
    (map #(create-extra-query-spec-field (:query-field %)) query-field-names ))
  )


(def test-data-query-map
  {:parameters
   {:query {:id nil
            :preferred-label nil
            :type nil
            :deprecated nil
            :relation nil
            :related-ids nil
            :offset nil
            :limit nil
            :version nil
            :driving-licence-code-2013 nil

            }
    }})

(def test-extra-query-data {:endpoint-name "driving-licence"
                            :extra-attributes [{:query-field-name "driving-licence-code-2013"
                                                :db-field-name :concept.external-standard/driving-licence-code-2013}
                                               {:db-field-name :concept.implicit-driving-licences }
                                               ]
                            })

;; (add-extra-queries (:extra-attributes (first taxonomy-extra-attributes )))

(defn get-pull-expression-from-extra-attributes [extra-attributes]
  (map :pull-expression extra-attributes)
  )

(defn get-query-fields-from-extra-attributes-as-keywords [extra-attributes]
  (remove nil? (map #(-> % :query-field keyword) extra-attributes))
  )


(defn compose-extra-where-attribute [extra-attribute query-params]
  (let [
        db-field-name (:where-field extra-attribute)
        field-name (-> extra-attribute :query-field keyword)
        value (when (get query-params field-name )
                (field-name query-params))]
    [db-field-name value])
  )

(defn compose-extra-where-attributes [query-params extra-attributes]
  (filter second (map
                #(compose-extra-where-attribute % query-params)
                (filter :query-field extra-attributes)
                ))
  )

;; TODO rename extra-attributes to endpoint-meta-data or something bigger

(defn build-db-function-args [query-params-map endpoint-meta-data ]

  (let [query-params (:query (:parameters query-params-map))
    ;    _ (println query-params)
        extra-attributes (:extra-attributes endpoint-meta-data)
        ;;    renamed-query-params (clojure.set/rename-keys query-params {:preferred-label :preferred-label})
        renamed-query-params-fixed-related-ids (update query-params :related-ids vector)


     ;   _ (println renamed-query-params)

     ;   _ (println "EXTRA ATTRUBUTES")
    ;    _ (println extra-attributes)

        extra-pull-fields (get-pull-expression-from-extra-attributes extra-attributes)
  ;;      _ (println "EXTRA PULL FIELDS")
   ;;     _ (println extra-pull-fields)

        query-field-names-as-keywords (get-query-fields-from-extra-attributes-as-keywords extra-attributes)

;;        _ (println  "QK" query-field-names-as-keywords)

        extra-where-attributes (compose-extra-where-attributes query-params extra-attributes)
;;       _  (println "EXTRA WHERE ATTRIBUTES")
;;       _ (println  extra-where-attributes )

        renamed-query-params-with-extra-pull-fields (assoc renamed-query-params-fixed-related-ids :extra-pull-fields extra-pull-fields)

 ;       _ (println    renamed-query-params-with-extra-pull-fields)

        renamed-query-params-with-extra-pull-fields-and-extra-where-attributes (assoc renamed-query-params-with-extra-pull-fields :extra-where-attributes extra-where-attributes)



;        _ (println  renamed-query-params-with-extra-pull-fields-and-extra-where-attributes)
        ]
    renamed-query-params-with-extra-pull-fields-and-extra-where-attributes
    )
  )

;; (:query (:parameters query-params-map)
;; TODO rename extra-attributes to endpoint-meta-data or something bigger
(defn compose-handler-function [ extra-attributes logging-function db-function]
  (fn [args]
    (logging-function (str "\n" (:query (:parameters args))) )
     {:status 200
      :body (vec (map map->nsmap (db-function (build-db-function-args args extra-attributes))))})
  )

(defn create-detailed-endpoint [extra-attributes logging-function db-function]

  (let [endpoint-name (:endpoint-name extra-attributes)
        extra-query-attributes (:extra-attributes extra-attributes)
        extra-queries (add-extra-queries  extra-query-attributes)
        query (into detailed-enpoint-query-base extra-queries)
        ]
    [(str "/concepts/" endpoint-name)
     {
      :summary      (str "Get " endpoint-name ". Supply at least one search parameter.")
      :parameters {:query query}
      :get {:responses {200 {:body (keyword taxonomy-namespace (str "concepts-" endpoint-name))}

                        500 {:body error-spec}}
            :handler (compose-handler-function extra-attributes  logging-function db-function)

            }}]))
