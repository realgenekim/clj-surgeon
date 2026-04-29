(ns my.app.thing
  (:require
   [clojure.string :as str]
   #?@(:cljs [[goog.string :as gstr]])))

(defn name->upper [n]
  (str/upper-case n))
