(ns bench.assets)

(defn asset-links [paths]
  ;; Reader shorthand is part of the source contract.
  (map #(views/static %) paths))

(defn email-asset-links [paths]
  (map #(views/static %) paths))
