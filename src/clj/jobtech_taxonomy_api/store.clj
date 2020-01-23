(ns jobtech-taxonomy-api.store
    (:require [konserve.filestore :as filestore]
              [konserve.core :as k]
              [mount.core :as mount]
              [jobtech-taxonomy-api.config :refer [env]]
              [clojure.core.async :as async :refer [<!!]]))

;; Note: We use the thread blocking operations <!! here only to synchronize
;; with the REPL. <!! is blocking IO and does not compose well with async
;; contexts, so prefer composing your application with go and <! instead.

(defn store-new [name]
  (<!! (filestore/new-fs-store name)))

(mount/defstate ^{:on-reload :noop} store
  :start
  (when (env :filestore)
    (store-new (env :filestore))))

(defn store-delete-store [& [store-arg]]
  (let [s (or store-arg store)]
    (filestore/delete-store s)))

(defn store-update [key val & [store-arg]]
  (let [s (or store-arg store)]
    (<!! (k/assoc-in s [key] val))))

(defn store-get [key & [store-arg]]
  (let [s (or store-arg store)]
    (<!! (k/get-in s [key]))))

(defn store-list-keys [& [store-arg]]
  (let [s (or store-arg store)]
    (<!! (filestore/list-keys s))))

(defn store-exists? [key & [store-arg]]
  (let [s (or store-arg store)]
    (<!! (k/exists? s key))))

(defn store-delete [key & [store-arg]]
  (let [s (or store-arg store)]
    (<!! (k/dissoc s :bar))))
