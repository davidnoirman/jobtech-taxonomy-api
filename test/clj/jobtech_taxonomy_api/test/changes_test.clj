(ns jobtech-taxonomy-api.test.changes-test
  (:require [clojure.test :as test]
            [jobtech-taxonomy-api.test.test-utils :as util]
            [jobtech-taxonomy-api.db.events :as events]
            [jobtech-taxonomy-api.db.concepts :as concept]
            [jobtech-taxonomy-api.db.versions :as versions]
            [jobtech-taxonomy-api.db.core :as core]
            [jobtech-taxonomy-api.db.concepts :as db-concepts]
            [taxonomy]
            ))

(test/use-fixtures :each util/fixture)

;; to make this test work, the database needs a version asserted
#_(test/deftest ^:integration-changes-test-0 changes-test-0
  (test/testing "test event stream"
    (concept/assert-concept "skill" "cykla" "cykla")
    (let [[status body] (util/send-request-to-json-service
                         :get "/v1/taxonomy/main/changes"
                         :headers [(util/header-auth-user)]
                         :query-params [{:key "from-version", :val "0"}])
          an-event (first body)
          found-concept (first (db-concepts/find-concepts-including-unpublished {:preferred-label "cykla" :type "skill" :deprecated false :offset 0 :limit 1}))]
      (test/is (= "CREATED" (:taxonomy/event-type an-event)))

      (test/is (= "cykla" (get found-concept :concept/preferred-label))))))



(comment
  0 ta ut nuvarande version
  1 skapa concept
  2 publicera ny version
  3 updatera concept
  4 publicera ny version

  kontrollera att rätt event har skapats,
  1 testa att created eventet skapas i rätt version
  1 testa att ändra preferred label

  )


(defn call-changes-service [after-version to-version-inclusive]
  (let [[status body] (util/send-request-to-json-service
                       :get "/v1/taxonomy/main/changes"
                       :headers [(util/header-auth-user)]
                       :query-params [{:key "after-version", :val after-version}
                                      {:key "to-version-inclusive" :val to-version-inclusive}
                                      ])]
    [status body]
    )
  )

(defn create-concept-and-version []
  (let [_ (versions/create-version-0)
        ;; version (versions/get-current-version-id)
        version 0
        next-version 1
        concept-pl (str (gensym "cykla-"))
        ;;_ (println "createing concept " concept-pl)
        [result timestamp new-concept] (concept/assert-concept "skill" concept-pl concept-pl)
   ;;     _ (println new-concept)
        _ (versions/create-new-version next-version)
        [statud body] (call-changes-service version next-version)
        ;;_ (println body)
        created-event (first body)
        ]
    [version next-version created-event new-concept]
    )
  )

(test/deftest ^:integration-changes-test-0 test-changes-create-concept
  (test/testing "Test create concept and read changes created event"
    (let [[version next-version created-event new-concept] (create-concept-and-version)
          concept-from-event (:taxonomy/changed-concept created-event)
          ]

      (test/is (= (:concept/preferred-label new-concept) (:taxonomy/preferred-label concept-from-event)))
      (test/is (= (:concept/id new-concept) (:taxonomy/id concept-from-event)))
      (test/is (= (:concept/type new-concept) (:taxonomy/type concept-from-event)))
      (test/is (= next-version (:taxonomy/version created-event)))
      (test/is (= "CREATED" (:taxonomy/event-type created-event)))
      )
    )
  )


(defn update-concept-and-publish-version []
  (let [ [version next-version created-event new-concept]  (create-concept-and-version)
        next-next-version (inc next-version)
        new-concept-pl (str (gensym "hoppa-"))
        {:keys [time concept]} (concept/accumulate-concept (:concept/id new-concept) "skill" nil new-concept-pl)
        _ (versions/create-new-version next-next-version)
        [status body] (call-changes-service next-version next-next-version)
        updated-event (first body)
        ]
    [next-version next-next-version updated-event concept]
    )
  )

(test/deftest ^:integration-changes-test-1 test-changes-update-concept
  (test/testing "Test update concept and read changes update event"
    (let [[version next-version event concept] (update-concept-and-publish-version)
          concept-from-event (:taxonomy/changed-concept event)
          ]

      (test/is (= (:concept/preferred-label concept) (:taxonomy/preferred-label concept-from-event)))
      (test/is (= (:concept/id concept) (:taxonomy/id concept-from-event)))
      (test/is (= (:concept/type concept) (:taxonomy/type concept-from-event)))
      (test/is (= next-version (:taxonomy/version event)))
      (test/is (= "UPDATED" (:taxonomy/event-type event)))
      )
    )
  )


(defn deprecate-concept-and-publish-version []
  (let [ [version next-version created-event new-concept]  (create-concept-and-version)
        next-next-version (inc next-version)
        _ (core/retract-concept (:concept/id new-concept))
        _ (versions/create-new-version next-next-version)
        [status body] (call-changes-service next-version next-next-version)
        event (first body)
        ]
    [next-version next-next-version event new-concept]
    )
  )

(test/deftest ^:integration-changes-test-2 test-changes-deprecate-concept
  (test/testing "Test update concept and read changes deprecate event"
    (let [[version next-version event concept] (deprecate-concept-and-publish-version)
          concept-from-event (:taxonomy/changed-concept event)
          ]

      (test/is (= (:concept/preferred-label concept) (:taxonomy/preferred-label concept-from-event)))
      (test/is (= (:concept/id concept) (:taxonomy/id concept-from-event)))
      (test/is (= (:concept/type concept) (:taxonomy/type concept-from-event)))
      (test/is (:taxonomy/deprecated concept-from-event))
      (test/is (= next-version (:taxonomy/version event)))
      (test/is (= "DEPRECATED" (:taxonomy/event-type event)))
      )
    )
  )
