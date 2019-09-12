(ns jobtech-taxonomy-api.types
  (:require
    [clojure.spec.alpha :as sp]
    [spec-tools.core :as st]
    [spec-tools.data-spec :as ds]))

(defn map->nsmap
  "Apply our nice namespace to the supplied structure."
  [m]
  (reduce-kv (fn [acc k v]
               (let [new-kw (if (and (keyword? k)
                                     (not (qualified-keyword? k)))
                              (keyword (str "jobtech-taxonomy-api.types") (name k))
                              k)
                     new-v (if (= (type v) clojure.lang.PersistentArrayMap )
                             (map->nsmap v)
                             v)]
                 (assoc acc new-kw new-v)))
             {} m))


;;;; Input parameter types
(sp/def ::fromVersion int?)
(sp/def ::toVersion int?)
(sp/def ::offset  int?)
(sp/def ::limit  int?)
(sp/def ::changes-params (sp/keys :opt [::fromVersion ::toVersion ::offset ::limit]))
(def changes-params ::changes-params)


;; Error message
(sp/def ::error (ds/spec {:name (st/spec string?)
                          :spec {::type (st/spec string?)
                                 ::message (st/spec string?)}}))
(def error-spec ::error)


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


;; /concept
(sp/def ::id (st/spec string?))
(sp/def ::type (st/spec string?))
(sp/def ::deprecated (st/spec boolean?))
(sp/def ::preferredLabel (st/spec string?))

(sp/def ::concept
  (ds/spec
   {:name ::concept
    :spec (sp/keys :req [::id ::type] :opt [::deprecated ::preferredLabel])}))

(sp/def ::event
  (ds/spec
   {:name ::event
    :spec {::eventType (st/spec string?)
           ::version (st/spec int?)
           ::concept ::concept}}))

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


;;;; handy debug tools...
;;  (sp/valid? versions-spec example-version-response)
;;  (sp/valid? events-spec example-events-responseE)
;;  (sp/explain versions-spec example-version-response)
;;  (sp/explain events-spec example-events-response)
;;  (sp/conform versions-spec example-version-response)
;;  (sp/conform events-spec example-events-response)
;;  (sp/get-spec ::ver)
;;  (sp/get-spec events-spec)
;;  (keys (st/registry #"jobtech-taxonomy-api.types.*"))
