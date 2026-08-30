(ns splice-reference.fixture)

(def retry-policy
  {:max-attempts 5
   :initial-delay-ms 250
   :multiplier 2.0
   :max-delay-ms 8000
   :retryable-statuses #{408 425 429 500 502 503 504}
   :idempotency-required? true
   :respect-retry-after? true})

(def cache-policy
  {:ttl-ms 300000
   :refresh-ahead-ms 30000
   :max-entries 2000
   :eviction :least-recently-used
   :compress-values? false
   :record-hit-rate? true
   :namespace-prefix "splice-screen:v1"})

(def alert-rules
  [{:signal :latency-p99-ms
    :warning 750
    :critical 1500
    :window-minutes 10
    :minimum-samples 100}
   {:signal :error-rate-percent
    :warning 2.0
    :critical 5.0
    :window-minutes 5
    :minimum-samples 200}
   {:signal :queue-depth
    :warning 500
    :critical 1000
    :window-minutes 15
    :minimum-samples 50}])

(def rollout-policy
  {:stages [{:name :canary :traffic-percent 5 :hold-minutes 20}
            {:name :regional :traffic-percent 25 :hold-minutes 45}
            {:name :broad :traffic-percent 100 :hold-minutes 60}]
   :abort-on #{:latency-regression :error-budget-breach}
   :require-manual-approval? true
   :rollback-window-minutes 120})
