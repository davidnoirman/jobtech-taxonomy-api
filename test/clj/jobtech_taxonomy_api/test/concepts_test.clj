(ns jobtech-taxonomy-api.test.concepts-test
  (:require [clojure.test :as test]
            [jobtech-taxonomy-api.test.test-utils :as util]
            [jobtech-taxonomy-api.db.events :as events]
            [jobtech-taxonomy-api.db.core :as core]
            [jobtech-taxonomy-api.db.concepts :as concept]
            [jobtech-taxonomy-database.nano-id :as nano]
            ))

(test/use-fixtures :each util/fixture)

(test/deftest ^:integration-concepts-test-0 concepts-test-0
  (test/testing "test assert concept"
    (concept/assert-concept "skill" "cyklade" "cykla")
    (let [[status body] (util/send-request-to-json-service
                         :get "/v1/taxonomy/main/concepts"
                         :headers [(util/header-auth-user)]
                         :query-params [{:key "type", :val "skill"}])
          found-concept (first (concept/find-concepts-including-unpublished {:preferred-label "cykla"}))]
      (test/is (= "cykla" (get found-concept :concept/preferred-label))))))

(test/deftest ^:integration-concepts-test-1 concepts-test-0
  (test/testing "test concept relation 'related'"
    (let [id-1 (get-in (nth (concept/assert-concept "skill" "pertest0" "pertest0") 2) [:concept/id])
          id-2 (get-in (nth (concept/assert-concept "skill" "pertest1" "pertest1") 2) [:concept/id])
          [tx rel] (concept/assert-relation id-1 id-2 "related" "desc" 0)
          rels (concept/find-relations-including-unpublished {:concept-1 id-1 :concept-2 id-2 :type "related" :limit 1})]
      (test/is (not (nil? rel)))
      (test/is (> (count rels) 0)))))
