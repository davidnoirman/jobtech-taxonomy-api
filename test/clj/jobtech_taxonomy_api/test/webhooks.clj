(ns jobtech-taxonomy-api.test.webhooks
  (:require [clojure.test :as test]
            [jobtech-taxonomy-api.webhooks :as webhooks]))

(test/deftest ^:webhook-test-0 test-webhooks-0
  (test/testing "Test using webhooks"
    (let [result (webhooks/send-notification {:url "https://postman-echo.com/post"
                                              :headers {}}
                                             69)]
      (test/is (= (:status result) 200)))))
