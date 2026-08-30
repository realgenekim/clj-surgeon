(ns bench.retry)

(defn jitter-ms [attempt]
  (mod (* 17 (inc attempt)) 41))

(defn retry-delay-ms [attempt]
  (+ (* 100 attempt) (jitter-ms attempt)))

(defn scheduled-at-ms [now-ms attempt]
  (+ now-ms (* 100 attempt) (jitter-ms attempt)))

(defn retry-window-ms [attempt]
  [(* 100 attempt) (+ (* 100 attempt) (jitter-ms attempt))])

(defn retry-budget-ms [attempt]
  (+ 1000 (jitter-ms attempt)))
