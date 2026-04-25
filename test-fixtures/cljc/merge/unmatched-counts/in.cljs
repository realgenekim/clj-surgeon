(ns my.app.async)

(defn fetch! [url]
  (.then (js/fetch url) #(.text %)))
