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
(sp/def ::preferredLabel (st/spec string?))
(sp/def ::definition (st/spec string?))
(sp/def ::eventType (st/spec string?))
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; ssyk-2012
(sp/def ::ssyk-2012 (st/spec string?))

(sp/def ::concept-without-replace-ssyk-2012
 (st/spec
  {:name ::concept-without-replace-ssyk-2012
   :spec (sp/keys :req [::id ::type ::ssyk-2012]
                  :opt [::definition ::deprecated ::preferredLabel ::concept-relations])}))

(sp/def ::replacedBy-ssyk-2012
  (ds/spec
   {:name ::replacedBy-ssyk-2012    :spec (sp/coll-of ::concept-without-replace-ssyk-2012 )}))

(sp/def ::concept-with-replace-ssyk-2012
  (ds/spec
   {:name ::concept-with-replace-ssyk-2012
    :spec (sp/keys :req [::id ::type ::ssyk-2012 ::preferredLabel]
                   :opt [::definition ::deprecated ::replacedBy-ssyk-2012 ::concept-relations])}))

(sp/def ::concepts-ssyk
  (ds/spec
   {:name ::concepts-ssyk-2012
    :spec (sp/coll-of ::concept-with-replace-ssyk-2012 )}))

(def concepts-spec-ssyk-2012 ::concepts-ssyk)

;; eures
(sp/def ::eures-code (st/spec string?))

(sp/def ::concept-without-replace-eures-code
  (st/spec
   {:name ::concept-without-replace-eures-code
    :spec (sp/keys :req [::id ::type ::eures-code]
                   :opt [::definition ::deprecated ::preferredLabel ::concept-relations])}))

 (sp/def ::replacedBy-eures-code
   (ds/spec
    {:name ::replacedBy-eures-code
     :spec (sp/coll-of ::concept-without-replace-eures-code )}))

 (sp/def ::concept-with-replace-eures-code
   (ds/spec
    {:name ::concept-with-replace-eures-code
     :spec (sp/keys :req [::id ::type ::eures-code ::preferredLabel]
                    :opt [::definition ::deprecated ::replacedBy-eures-code ::concept-relations])}))

(sp/def ::concepts-employment-duration
   (ds/spec
    {:name ::concepts-eures-code
     :spec (sp/coll-of ::concept-with-replace-eures-code )}))

(def concepts-spec-eures-code ::concepts-employment-duration)

;; nuts-level-3
(sp/def ::nuts-level-3-code (st/spec string?))

(sp/def ::concept-without-replace-nuts-level-3-code
  (st/spec
   {:name ::concept-without-replace-nuts-level-3-code
    :spec (sp/keys :req [::id ::nuts-level-3-code ::type]
                   :opt [::definition ::deprecated ::preferredLabel ::concept-relations])}))

 (sp/def ::replacedBy-nuts-level-3-code
   (ds/spec
    {:name ::replacedBy-nuts-level-3-code
     :spec (sp/coll-of ::concept-without-replace-nuts-level-3-code )}))

 (sp/def ::concept-with-replace-nuts-level-3-code
   (ds/spec
    {:name ::concept-with-replace-nuts-level-3-code
     :spec (sp/keys :req [::id ::type ::nuts-level-3-code ::preferredLabel]
                    :opt [::definition ::deprecated ::replacedBy-nuts-level-3-code ::concept-relations])}))

 (sp/def ::concepts-region
   (ds/spec
    {:name ::concepts-nuts-level-3-code
     :spec (sp/coll-of ::concept-with-replace-nuts-level-3-code )}))

 (def concepts-spec-nuts-level-3-code ::concepts-region)

;; country
(sp/def ::country-code (st/spec string?))

(sp/def ::concept-without-replace-country-code
  (st/spec
   {:name ::concept-without-replace-country-code
    :spec (sp/keys :req [::id ::country-code ::type]
                   :opt [::definition ::deprecated ::preferredLabel ::concept-relations])}))

 (sp/def ::replacedBy-country-code
   (ds/spec
    {:name ::replacedBy-country-code
     :spec (sp/coll-of ::concept-without-replace-country-code )}))

 (sp/def ::concept-with-replace-country-code
   (ds/spec
    {:name ::concept-with-replace-country-code
     :spec (sp/keys :req [::id ::type ::country-code ::preferredLabel]
                    :opt [::definition ::deprecated ::replacedBy-country-code ::concept-relations])}))

 (sp/def ::concepts-country
   (ds/spec
    {:name ::concepts-country-code
     :spec (sp/coll-of ::concept-with-replace-country-code )}))

 (def concepts-spec-country-code ::concepts-country)

;; id ;; TODO: make completions

(sp/def ::concept-without-replace-id
  (st/spec
   {:name ::concept-without-replace-id
    :spec (sp/keys :req [::id ::type]
                   :opt [::definition ::deprecated ::preferredLabel ::concept-relations])}))

 (sp/def ::replacedBy-id
   (ds/spec
    {:name ::replacedBy-id
     :spec (sp/coll-of ::concept-without-replace-id )}))

 (sp/def ::concept-with-replace-id
   (ds/spec
    {:name ::concept-with-replace-id
     :spec (sp/keys :req [::id ::type ::preferredLabel]
                    :opt [::definition ::deprecated ::replacedBy-id ::concept-relations])}))

 (sp/def ::concepts-id
   (ds/spec
    {:name ::concepts-id
     :spec (sp/coll-of ::concept-with-replace-id )}))

 (def concepts-spec-id ::concepts-id)

;; isco-08
(sp/def ::isco-08 (st/spec string?))

(sp/def ::concept-without-replace-isco-08
  (st/spec
   {:name ::concept-without-replace-isco-08
    :spec (sp/keys :req [::id ::isco-08 ::type]
                   :opt [::definition ::deprecated ::preferredLabel ::concept-relations])}))

 (sp/def ::replacedBy-isco-08
   (ds/spec
    {:name ::replacedBy-isco-08
     :spec (sp/coll-of ::concept-without-replace-isco-08 )}))

 (sp/def ::concept-with-replace-isco-08
   (ds/spec
    {:name ::concept-with-replace-isco-08
     :spec (sp/keys :req [::id ::type ::isco-08 ::preferredLabel]
                    :opt [::definition ::deprecated ::replacedBy-isco-08 ::concept-relations])}))

 (sp/def ::concepts-isco
   (ds/spec
    {:name ::concepts-isco-08
     :spec (sp/coll-of ::concept-with-replace-isco-08 )}))

 (def concepts-spec-isco-08 ::concepts-isco)

;; education-field-code-2020
(sp/def ::education-field-code-2020 (st/spec string?))

(sp/def ::concept-without-replace-sun-education-field-code-2020
  (st/spec
   {:name ::concept-without-replace-sun-education-field-code-2020
    :spec (sp/keys :req [::id ::education-field-code-2020 ::type]
                   :opt [::definition ::deprecated ::preferredLabel ::concept-relations])}))

 (sp/def ::replacedBy-sun-education-field-code-2020
   (ds/spec
    {:name ::replacedBy-sun-education-field-code-2020
     :spec (sp/coll-of ::concept-without-replace-sun-education-field-code-2020 )}))

 (sp/def ::concept-with-replace-sun-education-field-code-2020
   (ds/spec
    {:name ::concept-with-replace-sun-education-field-code-2020
     :spec (sp/keys :req [::id ::type ::education-field-code-2020 ::preferredLabel]
                    :opt [::definition ::deprecated ::replacedBy-sun-education-field-code-2020 ::concept-relations])}))

 (sp/def ::concepts-sun-education-field
   (ds/spec
    {:name ::concepts-sun-education-field-code-2020
     :spec (sp/coll-of ::concept-with-replace-sun-education-field-code-2020 )}))

 (def concepts-spec-sun-education-field-code-2020 ::concepts-sun-education-field)

;; sun-education-level-code-2020
(sp/def ::sun-education-level-code-2020 (st/spec string?))

(sp/def ::concept-without-replace-sun-education-level-code-2020
  (st/spec
   {:name ::concept-without-replace-sun-education-level-code-2020
    :spec (sp/keys :req [::id ::sun-education-level-code-2020 ::type]
                   :opt [::definition ::deprecated ::preferredLabel ::concept-relations])}))

 (sp/def ::replacedBy-sun-education-level-code-2020
   (ds/spec
    {:name ::replacedBy-sun-education-level-code-2020
     :spec (sp/coll-of ::concept-without-replace-sun-education-level-code-2020 )}))

 (sp/def ::concept-with-replace-sun-education-level-code-2020
   (ds/spec
    {:name ::concept-with-replace-sun-education-level-code-2020
     :spec (sp/keys :req [::id ::type ::sun-education-level-code-2020 ::preferredLabel]
                    :opt [::definition ::deprecated ::replacedBy-sun-education-level-code-2020 ::concept-relations])}))

 (sp/def ::concepts-sun-education-level
   (ds/spec
    {:name ::concepts-sun-education-level-code-2020
     :spec (sp/coll-of ::concept-with-replace-sun-education-level-code-2020 )}))

 (def concepts-spec-sun-education-level-code-2020 ::concepts-sun-education-level)

;; sni-level-code
(sp/def ::sni-level-code (st/spec string?))

(sp/def ::concept-without-replace-sni-level-code
  (st/spec
   {:name ::concept-without-replace-sni-level-code
    :spec (sp/keys :req [::id ::sni-level-code ::type]
                   :opt [::definition ::deprecated ::preferredLabel ::concept-relations])}))

 (sp/def ::replacedBy-sni-level-code
   (ds/spec
    {:name ::replacedBy-sni-level-code
     :spec (sp/coll-of ::concept-without-replace-sni-level-code )}))

 (sp/def ::concept-with-replace-sni-level-code
   (ds/spec
    {:name ::concept-with-replace-sni-level-code
     :spec (sp/keys :req [::id ::type ::sni-level-code ::preferredLabel]
                    :opt [::definition ::deprecated ::replacedBy-sni-level-code ::concept-relations])}))

 (sp/def ::concepts-sni-level
   (ds/spec
    {:name ::concepts-sni-level-code
     :spec (sp/coll-of ::concept-with-replace-sni-level-code )}))

 (def concepts-spec-sni-level-code ::concepts-sni-level)



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
                   :opt [::definition ::deprecated ::preferredLabel])}))

(sp/def ::event
  (ds/spec
   {:name ::event
    :spec (sp/keys :req [::eventType
                         ::version
                         ::concept])}))

(sp/def ::events
  (ds/spec
   {:name ::events
    :spec (sp/coll-of ::event )}))

(def events-spec ::events)

;; /concepts

(sp/def ::replacedBy
  (ds/spec
   {:name ::replacedBy
    :spec (sp/coll-of ::concept )}))

(sp/def ::concept-with-replace
  (ds/spec
   {:name ::concept-with-replace
    :spec (sp/keys :req [::id ::type ::preferredLabel]
                   :opt [::definition ::deprecated ::replacedBy])}))

(sp/def ::concepts
  (ds/spec
   {:name ::concepts
    :spec (sp/coll-of ::concept-with-replace )}))

(def concepts-spec ::concepts)

;; /search

(sp/def ::search-result
  (ds/spec
   {:name ::search-result
    :spec (sp/keys :req [::id ::type ::preferredLabel])}))

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
(sp/def :concept/preferredLabel (st/spec string?))
(sp/def :concept/definition (st/spec string?))

(def koncept
  {(ds/req :id) :concept/id
   (ds/req :type) :concept/type
   (ds/req :preferredLabel) :concept/preferredLabel
   (ds/opt :definition) :concept/definition
   (ds/opt :deprecated) :concept/deprecated
   (ds/opt :replacedBy) koncept-spec
   }
  )

(def koncept-spec
  (ds/spec
   {:name :taxonomy/Koncept
    :spec koncept
    }
   )
  )





;; driving-licence
(sp/def ::driving-licence-code (st/spec string?))

(sp/def ::concept-without-replace-driving-licence-code
  (st/spec
   {:name ::concept-without-replace-driving-licence-code
    :spec (sp/keys :req [::id ::type ::driving-licence-code]
                   :opt [::definition ::deprecated ::preferredLabel ::concept-relations])}))

(sp/def ::replacedBy-driving-licence-code
  (ds/spec
   {:name ::replacedBy-driving-licence-code
    :spec (sp/coll-of ::concept-without-replace-driving-licence-code )}))

(sp/def ::concept-with-replace-driving-licence-code
  (ds/spec
   {:name ::concept-with-replace-driving-licence-code
    :spec (sp/keys :req [::id ::type ::driving-licence-code ::preferredLabel]
                   :opt [::definition ::deprecated ::replacedBy-driving-licence-code ::concept-relations])}))

(sp/def ::concepts-driving-licence
  (ds/spec
   {:name ::concepts-driving-licence-code
    :spec (sp/coll-of ::concept-with-replace-driving-licence-code )}))

(def concepts-spec-driving-licence-code ::concepts-driving-licence)







(def taxonomy-extra-attributes

  [
   {:endpoint-name "driving-licence"
    :extra-attributes [{:query-field-name "driving-licence-code-2013"
                        :db-field-name :concept.external-standard/driving-licence-code-2013}
                       {:db-field-name :concept.implicit-driving-licences }
                       ]
    }

   {:endpoint-name "ssyk"
    :extra-attributes [{:query-field-name "ssyk-2012"
                        :db-field-name :concept.external-standard/ssyk-2012}]}

   {:endpoint-name "employment-duration"
    :extra-attributes [{:query-field-name "eures-code"
                        :db-field-name :concept.external-standard/eures-code}]
    }

   {:endpoint-name "region"
    :extra-attributes [{:query-field-name "nuts-level-3-code-2013"
                        :db-field-name :concept.external-standard/nuts-level-3-code-2013}

                       {:query-field-name "national-nuts-level-3-code-2019"
                        :db-field-name :concept.external-standard/national-nuts-level-3-code-2019}
                       ]
    }

   {:endpoint-name "country"
    :extra-attributes [
                       {:query-field-name "iso-3166-1-alpha-2-2013"
                        :db-field-name :concept.external-standard/iso-3166-1-alpha-2-2013}
                       ]
    }

   {:endpoint-name "isco"
    :extra-attributes [{:query-field-name "isco-08"
                        :db-field-name :concept.external-standard/isco-08}]
    }

   {:endpoint-name "sun-education-field"
    :extra-attributes [{:query-field-name "sun-education-field-code-2020"
                        :db-field-name :concept.external-standard/sun-education-field-code-2020}]
    }


   {:endpoint-name "sun-education-level"
    :extra-attributes [{:query-field-name "sun-education-level-code-2020"
                        :db-field-name  :concept.external-standard/sun-education-level-code-2020}]
    }


   {:endpoint-name "sni-level"
    :extra-attributes [{:query-field-name "sni-level-code-2007"
                        :db-field-name :concept.external-standard/sni-level-code-2007}]
    }
   ]

  )




;; TODO add language


(defmacro par [type desc]
  "Use this to make parameter declarations somewhat tidier."
  `(st/spec {:spec ~type :description ~desc}))


(def detailed-enpoint-query-base
  {(ds/opt :id) (par string? "ID of concept")
   (ds/opt :preferredLabel) (par string? "Textual name of concept")
   (ds/opt :type) (par string? "Restrict to concept type"),
  ;;(ds/opt :type) (par #{"ssyk_level_1" "ssyk_level_2" "ssyk_level_3" "ssyk_level_4" } "Restrict to concept type")
   (ds/opt :deprecated) (par boolean? "Restrict to deprecation state")
   (ds/opt :relation) (par #{"broader" "narrower" "related" "occupation_name_affinity"} "Relation type")
  (ds/opt :related-ids) (par string? "OR-restrict to these relation IDs (white space separated list)")

   ;;   (ds/opt :code) (par string? name) ;; TODO Create one for each attribute
  (ds/opt :offset) (par int? "Return list offset (from 0)")
  (ds/opt :limit) (par int? "Return list limit")
  (ds/opt :version) (par int? "Version to use")}
  )


(defn create-extra-query-spec-field [query-field-name]
  { (ds/opt (keyword query-field-name)) (par string? query-field-name)}
  )

(defn add-extra-queries [extra-query-attributes]
  (let [query-field-names   (filter #(:query-field-name %) extra-query-attributes)]
    (map #(create-extra-query-spec-field (:query-field-name %)) query-field-names ))
  )


(def test-data-query-map
  {:parameters
   {:query {:id nil
            :preferredLabel nil
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

(defn get-db-field-names-from-extra-attributes [extra-attributes]
  (map :db-field-name extra-attributes)
  )

(defn get-query-field-names-from-extra-attributes-as-keywords [extra-attributes]
  (remove nil? (map #(-> % :query-field-name keyword) extra-attributes))
  )

(defn compose-extra-where-attribute [extra-attribute query-params]
  (let [
        db-field-name (:db-field-name extra-attribute)
        field-name (-> extra-attribute :query-field-name keyword)
        value (if (get query-params  field-name )
                (field-name query-params)
                '_
                )]
    [db-field-name value])
  )

(defn compose-extra-where-attributes [query-params extra-attributes]
  (map
   #(compose-extra-where-attribute % query-params)
   extra-attributes
       )
  )

;; TODO rename extra-attributes to endpoint-meta-data or something bigger

(defn build-db-function-args [query-params-map endpoint-meta-data ]

  (let [query-params (:query (:parameters query-params-map))
    ;    _ (println query-params)
        extra-attributes (:extra-attributes endpoint-meta-data)
        renamed-query-params (clojure.set/rename-keys query-params {:preferredLabel :preferred-label})

     ;   _ (println renamed-query-params)

     ;   _ (println "EXTRA ATTRUBUTES")
    ;    _ (println extra-attributes)

        extra-pull-fields (get-db-field-names-from-extra-attributes extra-attributes)
        _ (println "EXTRA PULL FIELDS")
        _ (println extra-pull-fields)

        query-field-names-as-keywords (get-query-field-names-from-extra-attributes-as-keywords extra-attributes)

        _ (println  "QK" query-field-names-as-keywords)

        extra-where-attributes (compose-extra-where-attributes query-params extra-attributes)
       _  (println "EXTRA WHERE ATTRIBUTES")
       _ (println  extra-where-attributes )

        renamed-query-params-with-extra-pull-fields (assoc renamed-query-params :extra-pull-fields extra-pull-fields)

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
        ]
    [(str "/concepts/" endpoint-name)
     {
      :summary      (str "Get " endpoint-name ". Supply at least one search parameter.")
      :parameters {:query (into detailed-enpoint-query-base (add-extra-queries  extra-query-attributes))}
      :get {:responses {200 #_{:body (keyword taxonomy-namespace (str "concepts-" endpoint-name))

                               }
                        {:body any?}

                        500 {:body error-spec}}
            :handler (compose-handler-function extra-attributes  logging-function db-function)

            }}]))
