(ns acme.retry-policy)

(def ^:private connect-timeout-ms 250)

(def ^:private request-timeout-ms 250)

(def ^:private jitter-ms 75)

(defn backoff-ms [attempt]
  (+ (* attempt 200) jitter-ms))

(defn connection-policy []
  {:connect-timeout-ms connect-timeout-ms
   :request-timeout-ms request-timeout-ms
   :retry-jitter-ms jitter-ms
   :retry-jitter-cap-ms (* 2 jitter-ms)
   :profile "legacy"})

(defn policy-summary []
  (str "legacy"
       ":" connect-timeout-ms
       ":" request-timeout-ms
       ":" jitter-ms))
