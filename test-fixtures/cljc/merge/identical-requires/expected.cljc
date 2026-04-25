(ns my.app.core
  (:require
   [clojure.string :as str]))

(defn upper [s]
  (str/upper-case s))
