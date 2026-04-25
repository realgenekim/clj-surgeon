(ns my.app.persist
  (:require
   [clojure.string :as str]))

(defn save [s]
  (str/trim s))
