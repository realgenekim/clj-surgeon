(ns acme.retry-policy-test
  (:require [acme.retry-policy :as policy]
            [clojure.test :refer [deftest is run-tests]]))

(deftest resilient-policy
  (is (= {:connect-timeout-ms 400
          :request-timeout-ms 800
          :retry-jitter-ms 75
          :retry-jitter-cap-ms 150
          :profile "resilient"}
         (policy/connection-policy)))
  (is (= 275 (policy/backoff-ms 1)))
  (is (= "resilient:400:800:75" (policy/policy-summary))))

(defn -main [& _]
  (let [{:keys [fail error]} (run-tests 'acme.retry-policy-test)]
    (when (pos? (+ fail error))
      (System/exit 1))))
