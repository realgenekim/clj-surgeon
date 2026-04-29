(ns my.app.persist
  (:require
   [clojure.string :as str]
   #?@(:clj [[clojure.java.io :as io]])))

(defn save [s]
  (str/trim s))
