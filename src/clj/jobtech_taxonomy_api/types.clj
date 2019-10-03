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

;; /changes
(sp/def ::concept-without-replace
  (ds/spec
   {:name ::concept-without-replace
    :spec (sp/keys :req [::id ::type]
                   :opt [::definition ::deprecated ::preferredLabel])}))

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
