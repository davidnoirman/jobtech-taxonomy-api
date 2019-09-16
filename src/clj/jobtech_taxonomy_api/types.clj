(ns jobtech-taxonomy-api.types
  (:require
    [clojure.spec.alpha :as sp]
    [spec-tools.core :as st]
    [spec-tools.data-spec :as ds]))


(defn map->nsmap
  "Apply our nice namespace to the supplied structure."
  [m]
  (reduce-kv (fn [acc k v]
               (let [new-kw (if (keyword? k)
                              (keyword (str "jobtech-taxonomy-api.types") (name k))
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


;; Error message
(sp/def ::error (ds/spec {:name (st/spec string?)
                          :spec {::type (st/spec string?)
                                 ::message (st/spec string?)}}))
(def error-spec ::error)


;;;; Output response types

;; General fundamentals
(sp/def ::id (st/spec string?))
(sp/def ::type (st/spec string?))
(sp/def ::deprecated (st/spec boolean?))
(sp/def ::preferredLabel (st/spec string?))
(sp/def ::definition (st/spec string?))


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

(def example-version-response
  [{::timestamp #inst "2019-08-15T10:39:20.814-00:00", ::version 1}
   {::timestamp #inst "2019-08-15T10:39:56.547-00:00", ::version 2}])


;; /changes
(sp/def ::concept-without-replace
  (ds/spec
   {:name ::concept-without-replace
    :spec (sp/keys :req [::id ::type] :opt [::definition ::deprecated ::preferredLabel])}))

(sp/def ::event
  (ds/spec
   {:name ::event
    :spec {::eventType (st/spec string?)
           ::version (st/spec int?)
           ::concept ::concept-without-replace}}))

(sp/def ::events
  (ds/spec
   {:name ::events
    :spec (sp/coll-of ::event )}))

(def events-spec ::events)


(def example-events-response
  [#:jobtech-taxonomy-api.types{:eventType "UPDATED",
                                :version 2,
                                :concept #:jobtech-taxonomy-api.types{:id "a7uU_j21_mkL",
                                                                      :type "employment_duration",
                                                                      :preferredLabel "Tillsvidare"}}
   #:jobtech-taxonomy-api.types{:eventType "UPDATED",
                                :version 2,
                                :concept #:jobtech-taxonomy-api.types{:id "Sy9J_aRd_ALx",
                                                                      :type "employment_duration",
                                                                      :preferredLabel "3 månader – upp till 6 månader"}}])


;; /concepts

(sp/def ::replacedBy
  (ds/spec
   {:name ::replacedBy
    :spec (sp/coll-of ::concept-without-replace )}))

(sp/def ::concept-with-replace
  (ds/spec
   {:name ::concept-with-replace
    :spec (sp/keys :req [::id ::type ::preferredLabel] :opt [::definition ::deprecated ::replacedBy])}))

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

;;;; handy debug tools...
;;  (sp/valid? versions-spec example-version-response)
;;  (sp/valid? events-spec example-events-response)
;;  (sp/valid? replaced-by-changes-spec example-replaced-by-changes-spec)
;;  (sp/explain versions-spec example-version-response)
;;  (sp/explain events-spec example-events-response)
;;  (sp/conform versions-spec example-version-response)
;;  (sp/conform events-spec example-events-response)
;;  (sp/get-spec ::ver)
;;  (sp/get-spec events-spec)
;;  (keys (st/registry #"jobtech-taxonomy-api.types.*"))
;;  (require '[spec-tools.json-schema :as jsc])
;;  (jsc/transform events-spec)
