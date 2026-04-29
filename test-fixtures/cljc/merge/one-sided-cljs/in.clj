(ns my.app.thing
  (:require
   [clojure.string :as str]))

(defn name->upper [n]
  (str/upper-case n))
