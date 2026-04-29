(ns my.app.refers
  (:require
   [clojure.string :as str :refer [join]]))

(defn run [xs]
  (join "," xs))
