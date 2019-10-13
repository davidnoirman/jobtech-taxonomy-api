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


(def taxonomy-extra-attributes
  [:concept.external-standard/ssyk-2012
   :concept.external-standard/eures-code
   :concept.external-standard/driving-licence-code
   :concept.external-standard/nuts-level-3-code
   :concept.external-standard/country-code
   :concept.external-standard/isco-08
   :concept.external-standard/sun-education-field-code-2020
   :concept.external-standard/sun-education-level-code-2020
   :concept.external-standard/sni-level-code
   ])



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
   {:name ::replacedBy-ssyk-2012
    :spec (sp/coll-of ::concept-without-replace-ssyk-2012 )}))

(sp/def ::concept-with-replace-ssyk-2012
  (ds/spec
   {:name ::concept-with-replace-ssyk-2012
    :spec (sp/keys :req [::id ::type ::ssyk-2012 ::preferredLabel]
                   :opt [::definition ::deprecated ::replacedBy-ssyk-2012 ::concept-relations])}))

(sp/def ::concepts-ssyk-2012
  (ds/spec
   {:name ::concepts-ssyk-2012
    :spec (sp/coll-of ::concept-with-replace-ssyk-2012 )}))

(def concepts-spec-ssyk-2012 ::concepts-ssyk-2012)

;; eures
(sp/def ::eures (st/spec string?))

(sp/def ::concept-without-replace-eures-code
  (st/spec
   {:name ::concept-without-replace-eures-code
    :spec (sp/keys :req [::id ::type ::eures]
                   :opt [::definition ::deprecated ::preferredLabel ::concept-relations])}))

 (sp/def ::replacedBy-eures-code
   (ds/spec
    {:name ::replacedBy-eures-code
     :spec (sp/coll-of ::concept-without-replace-eures-code )}))

 (sp/def ::concept-with-replace-eures-code
   (ds/spec
    {:name ::concept-with-replace-eures-code
     :spec (sp/keys :req [::id ::type ::eures ::preferredLabel]
                    :opt [::definition ::deprecated ::replacedBy-eures-code ::concept-relations])}))

 (sp/def ::concepts-eures-code
   (ds/spec
    {:name ::concepts-eures-code
     :spec (sp/coll-of ::concept-with-replace-eures-code )}))

 (def concepts-spec-eures-code ::concepts-eures-code)

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

 (sp/def ::concepts-driving-licence-code
   (ds/spec
    {:name ::concepts-driving-licence-code
     :spec (sp/coll-of ::concept-with-replace-driving-licence-code )}))

 (def concepts-spec-driving-licence-code ::concepts-driving-licence-code)

;; nuts-level-3
(sp/def ::nuts-level-3 (st/spec string?))

(sp/def ::concept-without-replace-nuts-level-3-code
  (st/spec
   {:name ::concept-without-replace-nuts-level-3-code
    :spec (sp/keys :req [::id ::nuts-level-3 ::type]
                   :opt [::definition ::deprecated ::preferredLabel ::concept-relations])}))

 (sp/def ::replacedBy-nuts-level-3-code
   (ds/spec
    {:name ::replacedBy-nuts-level-3-code
     :spec (sp/coll-of ::concept-without-replace-nuts-level-3-code )}))

 (sp/def ::concept-with-replace-nuts-level-3-code
   (ds/spec
    {:name ::concept-with-replace-nuts-level-3-code
     :spec (sp/keys :req [::id ::type ::nuts-level-3 ::preferredLabel]
                    :opt [::definition ::deprecated ::replacedBy-nuts-level-3-code ::concept-relations])}))

 (sp/def ::concepts-nuts-level-3-code
   (ds/spec
    {:name ::concepts-nuts-level-3-code
     :spec (sp/coll-of ::concept-with-replace-nuts-level-3-code )}))

 (def concepts-spec-nuts-level-3-code ::concepts-nuts-level-3-code)

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

 (sp/def ::concepts-country-code
   (ds/spec
    {:name ::concepts-country-code
     :spec (sp/coll-of ::concept-with-replace-country-code )}))

 (def concepts-spec-country-code ::concepts-country-code)

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

 (sp/def ::concepts-isco-08
   (ds/spec
    {:name ::concepts-isco-08
     :spec (sp/coll-of ::concept-with-replace-isco-08 )}))

 (def concepts-spec-isco-08 ::concepts-isco-08)

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

 (sp/def ::concepts-sun-education-field-code-2020
   (ds/spec
    {:name ::concepts-sun-education-field-code-2020
     :spec (sp/coll-of ::concept-with-replace-sun-education-field-code-2020 )}))

 (def concepts-spec-sun-education-field-code-2020 ::concepts-sun-education-field-code-2020)

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

 (sp/def ::concepts-sun-education-level-code-2020
   (ds/spec
    {:name ::concepts-sun-education-level-code-2020
     :spec (sp/coll-of ::concept-with-replace-sun-education-level-code-2020 )}))

 (def concepts-spec-sun-education-level-code-2020 ::concepts-sun-education-level-code-2020)

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

 (sp/def ::concepts-sni-level-code
   (ds/spec
    {:name ::concepts-sni-level-code
     :spec (sp/coll-of ::concept-with-replace-sni-level-code )}))

 (def concepts-spec-sni-level-code ::concepts-sni-level-code)



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
(sp/def ::concept-without-replace
  (st/spec
   {:name ::concept-without-replace
    :spec (sp/keys :req [::id ::type]
                   :opt [::definition ::deprecated ::preferredLabel])}))

(sp/def ::event
  (ds/spec
   {:name ::event
    :spec (sp/keys :req [::eventType
                         ::version
                         ::concept-without-replace])}))

(sp/def ::events
  (ds/spec
   {:name ::events
    :spec (sp/coll-of ::event )}))

(def events-spec ::events)

;; /concepts

(sp/def ::replacedBy
  (ds/spec
   {:name ::replacedBy
    :spec (sp/coll-of ::concept-without-replace )}))

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
    :spec (sp/coll-of ::concept-without-replace )}))

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
                                      ::concept ::concept-without-replace}}))
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
