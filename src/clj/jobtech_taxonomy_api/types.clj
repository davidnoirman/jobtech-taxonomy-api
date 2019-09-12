(ns jobtech-taxonomy-api.types
  (:require
    [clojure.spec.alpha :as sp]
    [spec-tools.core :as st]
    [spec-tools.data-spec :as ds]))

(defn map->nsmap
  [m]
  (reduce-kv (fn [acc k v]
               (let [new-kw (if (and (keyword? k)
                                     (not (qualified-keyword? k)))
                              (keyword (str "jobtech-taxonomy-api.types") (name k))
                              k) ]
                 (assoc acc new-kw v)))
             {} m))

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


;;;; handy debug tools...
;;  (sp/valid? versions-spec example-version-response)
;;  (sp/explain versions-spec example-version-response)
;;  (sp/conform versions-spec example-version-response)
;;  (sp/get-spec ::ver)
;;  (keys (st/registry #"jobtech-taxonomy-api.types.*"))
