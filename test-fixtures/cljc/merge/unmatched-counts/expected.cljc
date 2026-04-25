(ns my.app.async)

#?@(:clj  [(defn fetch-impl [url]
             (slurp url))

           (defn fetch! [url]
             (fetch-impl url))]
    :cljs [(defn fetch! [url]
             (.then (js/fetch url) #(.text %)))])
