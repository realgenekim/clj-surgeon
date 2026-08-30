(ns acme.checkout-policy)

(def ^:private connect-timeout-ms 250)
(def ^:private request-timeout-ms 900)

(defn retry-budget []
  {:connect-timeout-ms connect-timeout-ms
   :request-timeout-ms request-timeout-ms
   :profile "legacy"})

(defn policy-label []
  (str "checkout-" "legacy"))
