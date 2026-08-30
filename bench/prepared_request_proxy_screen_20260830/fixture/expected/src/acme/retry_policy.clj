(ns acme.retry-policy)

(def ^:private connect-timeout-ms 400)

(def ^:private request-timeout-ms 800)

(def ^:private retry-jitter-ms 75)

(defn backoff-ms [attempt]
  (+ (* attempt 200) retry-jitter-ms))

(defn connection-policy []
  {:connect-timeout-ms connect-timeout-ms
   :request-timeout-ms request-timeout-ms
   :retry-jitter-ms retry-jitter-ms
   :retry-jitter-cap-ms (* 2 retry-jitter-ms)
   :profile "resilient"})

(defn policy-summary []
  (str "resilient"
       ":" connect-timeout-ms
       ":" request-timeout-ms
       ":" retry-jitter-ms))
