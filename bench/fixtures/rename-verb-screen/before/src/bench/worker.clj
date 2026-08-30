(ns bench.worker
  (:require
   [bench.retry :as retry]))

(defn next-job [attempt]
  {:attempt attempt :jitter (retry/jitter-ms attempt)})

(defn park-worker [attempt]
  (Thread/sleep (retry/jitter-ms attempt)))

(defn worker-deadline [now-ms attempt]
  (+ now-ms (retry/jitter-ms attempt)))
