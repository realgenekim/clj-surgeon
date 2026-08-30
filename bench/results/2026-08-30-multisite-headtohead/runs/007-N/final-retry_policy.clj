(ns acme.retry-policy)

(def ^:private retry-delay-ms 125)

(def ^:private retry-limit 6)

(def ^:private max-delay-ms 8000)

(defn normalize-attempt
  [attempt]
  (max 0 (long attempt)))

;; The first caller establishes the immediate delay used after a failure.
;; Its surrounding arithmetic is intentionally unrelated to the rename.
(defn initial-backoff
  [attempt]
  (+ retry-delay-ms
     (* 25 (normalize-attempt attempt))))

(defn bounded-limit
  [configured]
  (min retry-limit (max 1 configured)))

(defn retryable-status?
  [status]
  (contains? #{408 425 429 500 502 503 504} status))

(defn terminal-status?
  [status]
  (contains? #{400 401 403 404 409 410 422} status))

;; The second caller applies the cap after exponential growth.
;; Keeping the helper separate makes each call site independently owned.
(defn capped-backoff
  [attempt]
  (let [factor (bit-shift-left 1 (normalize-attempt attempt))]
    (min max-delay-ms
         (* retry-delay-ms factor))))

(defn retry-budget
  [configured]
  {:attempts (bounded-limit configured)
   :deadline-ms (* 2 max-delay-ms)})

(defn jitter-offset
  [attempt]
  (mod (* 37 (inc (normalize-attempt attempt))) 97))

(defn with-jitter
  [delay attempt]
  (+ delay (jitter-offset attempt)))

;; The third caller describes the inclusive timing window.
;; The map shape remains fixed across the requested change.
(defn retry-window
  [attempt]
  (let [base (capped-backoff attempt)]
    {:minimum-ms retry-delay-ms
     :selected-ms base
     :maximum-ms (with-jitter base attempt)}))

(defn permitted-attempt?
  [attempt configured]
  (< (normalize-attempt attempt)
     (bounded-limit configured)))

(defn next-attempt
  [attempt]
  (inc (normalize-attempt attempt)))

(defn retry-decision
  [status attempt configured]
  (and (retryable-status? status)
       (not (terminal-status? status))
       (permitted-attempt? attempt configured)))

;; The fourth caller materializes a deterministic schedule for diagnostics.
;; The vector intentionally spans several lines to make the owner distinct.
(defn retry-schedule
  [configured]
  (mapv (fn [attempt]
          {:attempt attempt
           :delay-ms (+ retry-delay-ms
                        (capped-backoff attempt))})
        (range (bounded-limit configured))))

(defn retry-headers
  [attempt]
  {"x-retry-attempt" (str (normalize-attempt attempt))
   "x-retry-source" "policy"})

(defn retry-context
  [status attempt configured]
  {:status status
   :attempt (normalize-attempt attempt)
   :configured configured
   :allowed? (retry-decision status attempt configured)})

(defn policy-kind
  [status]
  (cond
    (retryable-status? status) :retryable
    (terminal-status? status) :terminal
    :else :unknown))

;; The fifth caller also owns the only requested prose replacement.
;; No other string or comment is part of the task.
(defn policy-summary
  "Returns the legacy retry delay summary."
  [configured]
  (str "attempts=" (bounded-limit configured)
       ",base-ms=" retry-delay-ms
       ",cap-ms=" max-delay-ms))

(defn policy-snapshot
  [status attempt configured]
  {:kind (policy-kind status)
   :context (retry-context status attempt configured)
   :window (retry-window attempt)
   :headers (retry-headers attempt)
   :summary (policy-summary configured)})

(defn eligible-delays
  [status configured]
  (if (retryable-status? status)
    (retry-schedule configured)
    []))

(defn exhausted?
  [attempt configured]
  (not (permitted-attempt? attempt configured)))

(defn final-decision
  [status attempt configured]
  (if (retry-decision status attempt configured)
    :retry
    :stop))
