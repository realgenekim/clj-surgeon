(ns bench.consumer)

(defn current-handler [request]
  (assoc request :route :current))

(defn route-request [request]
  (bench.legacy-adapter/legacy-adapter request))

(defn unrelated-route [request]
  (assoc request :route :unrelated))
