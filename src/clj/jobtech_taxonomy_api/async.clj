(ns jobtech-taxonomy-api.async
  (:require
   [clojure.core.async :as async]))

;; I don't like having a state here. But I don't like the idea of
;; risking running multiple jobs in parallel for the same client
;; neither.
(def jobs
  "A map with the currently scheduled jobs. The purpose is to avoid
scheduling multiple jobs with the same ID."
  (atom {}))

(defn register-active-job [id]
  (swap! jobs assoc (symbol (str id)) 'active))

(defn deregister-job [id]
  (swap! jobs dissoc (symbol (str id))))

(defn job-active? [id]
  (get @jobs (symbol (str id))))

(defn run-job [id job]
  (let [active (job-active? id)]
    (if (and active
             (apply (:fun job) []))
      (deregister-job id)
      (async/go
        (let [new-delay  (apply (:delay-fun job) [ (:delay job)])]
          (async/<! (async/timeout (* (:delay job) 1000)))
          (run-job id (assoc job :delay new-delay)))))))

(defn commit-job [id fun delay delay-fun]
  (when (not (job-active? id))
    (register-active-job id)
    (run-job id {:fun fun :delay delay :delay-fun delay-fun})))
