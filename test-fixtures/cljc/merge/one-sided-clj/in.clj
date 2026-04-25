(ns my.app.persist
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]))

(defn save [s]
  (str/trim s))
