(ns clj-surgeon.test-permissions
  (:require
   [babashka.process :as proc]
   [clojure.string :as str]))

(defn root? []
  (= "0" (str/trim (:out (proc/shell {:out :string} "id" "-u")))))
