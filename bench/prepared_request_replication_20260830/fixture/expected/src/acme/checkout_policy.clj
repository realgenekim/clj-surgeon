(ns acme.checkout-policy)

(def ^:private connect-timeout-ms 400)
(def ^:private request-timeout-ms 1200)

(defn resilience-budget []
  {:connect-timeout-ms connect-timeout-ms
   :request-timeout-ms request-timeout-ms
   :profile "resilient"})

(defn policy-label []
  (str "checkout-" "resilient"))
