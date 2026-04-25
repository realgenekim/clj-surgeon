(ns my.app.thing
  (:require
   [clojure.string :as str]
   [goog.string :as gstr]))

(defn name->upper [n]
  (str/upper-case n))
