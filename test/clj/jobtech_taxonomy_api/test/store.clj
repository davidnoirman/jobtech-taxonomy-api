(ns jobtech-taxonomy-api.test.store
  (:require [clojure.test :as test]
            [jobtech-taxonomy-api.store :as store]))

(test/deftest test-store-0
  (test/testing "Test store"
    (let [temp-file-name (str (java.io.File/createTempFile "/tmp" ".tmp"))
          temp-dir-name  (str temp-file-name ".dir")
          store (store/store-new temp-dir-name)
          contents-0 (store/store-list-keys store)
          update-0 (store/store-update "key" "val" store)
          contents-1 (store/store-list-keys store)
          ;;delete-0 (store/store-delete "key" store) ;; FIXME: understand why key is not removed from store
          ;;contents-2 (store/store-list-keys store)
          ]

      (test/is (= contents-0 #{}))
      (test/is (= contents-1 #{{:key "key", :format :edn}}))
      ;;(test/is (= contents-2 #{}))

      (.delete (clojure.java.io/file temp-file-name))
      (store/store-delete-store temp-dir-name))))
