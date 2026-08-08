(ns bench.legacy-adapter)

(defn legacy-adapter [request]
  (bench.consumer/current-handler request))
