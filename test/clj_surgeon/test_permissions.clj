(ns clj-surgeon.test-permissions
  (:require
   [babashka.process :as proc]
   [clojure.string :as str]))

(defn check-permission-refusal? [description]
  (let [uid (str/trim (:out (proc/shell {:out :string} "id" "-u")))]
    (if (= "0" uid)
      (do
        (println (str "Skipping " description " as UID 0 because root can bypass POSIX write permissions."))
        false)
      true)))
