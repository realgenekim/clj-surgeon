(ns splice-reference.fixture)

(def retry-policy
  {:max-attempts 7
   :initial-delay-ms 400
   :multiplier 2.0
   :max-delay-ms 12000
   :jitter-strategy :full
   :retryable-statuses #{408 425 429 500 502 503 504}
   :idempotency-required? true
   :respect-retry-after? true})

(def cache-policy
  {:ttl-ms 900000
   :refresh-ahead-ms 60000
   :stale-while-revalidate-ms 120000
   :max-entries 5000
   :eviction :least-recently-used
   :compress-values? true
   :record-hit-rate? true
   :namespace-prefix "splice-screen:v2"})

(def alert-rules
  [{:signal :latency-p99-ms
    :warning 650
    :critical 1200
    :window-minutes 10
    :minimum-samples 150}
   {:signal :error-rate-percent
    :warning 1.5
    :critical 4.0
    :window-minutes 5
    :minimum-samples 250}
   {:signal :queue-depth
    :warning 400
    :critical 800
    :window-minutes 15
    :minimum-samples 75}])

(def rollout-policy
  {:stages [{:name :canary :traffic-percent 3 :hold-minutes 30}
            {:name :regional :traffic-percent 20 :hold-minutes 60}
            {:name :broad :traffic-percent 100 :hold-minutes 90}]
   :abort-on #{:latency-regression :error-budget-breach :saturation}
   :require-manual-approval? true
   :rollback-window-minutes 180})
