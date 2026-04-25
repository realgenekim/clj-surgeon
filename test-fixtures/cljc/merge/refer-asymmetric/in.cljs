(ns my.app.refers
  (:require
   [clojure.string :as str :refer [join split]]))

(defn run [xs]
  (join "," xs))
