(ns my.app.async)

(defn fetch-impl [url]
  (slurp url))

(defn fetch! [url]
  (fetch-impl url))
