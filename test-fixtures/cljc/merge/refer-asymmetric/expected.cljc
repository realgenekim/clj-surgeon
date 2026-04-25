(ns my.app.refers
  (:require
   #?@(:clj  [[clojure.string :as str :refer [join]]]
       :cljs [[clojure.string :as str :refer [join split]]])))

(defn run [xs]
  (join "," xs))
