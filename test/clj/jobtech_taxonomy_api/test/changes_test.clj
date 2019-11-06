(ns jobtech-taxonomy-api.test.changes-test
  (:require [clojure.test :as test]
            [jobtech-taxonomy-api.test.test-utils :as util]
            [jobtech-taxonomy-api.db.events :as events]
            [jobtech-taxonomy-api.db.concepts :as concept]
            ;;[jobtech-taxonomy-api.db.versions :as versions]
            [jobtech-taxonomy-api.db.core :as core]
            [jobtech-taxonomy-api.db.concepts :as db-concepts]
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
