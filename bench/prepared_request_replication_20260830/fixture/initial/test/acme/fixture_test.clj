(ns acme.fixture-test
  (:require
   [acme.analytics-policy :as analytics]
   [acme.checkout-client :as client]
   [acme.checkout-policy :as policy]))

(defn -main []
  (let [budget (policy/resilience-budget)
        options (client/request-options)]
    (assert (= {:connect-timeout-ms 400
                :request-timeout-ms 1200
                :profile "resilient"}
               budget))
    (assert (= "checkout-resilient" (policy/policy-label)))
    (assert (= "resilient" (:client-profile options)))
    (assert (= {:connect-timeout-ms 250
                :request-timeout-ms 900
                :profile "legacy"}
               (analytics/retry-budget)))
    (println "4 assertions passed")))
