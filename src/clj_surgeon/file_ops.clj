(ns clj-surgeon.file-ops
  "Shared fail-closed filesystem operations."
  (:require
   [clojure.java.io :as io])
  (:import
   (java.nio.file CopyOption Files StandardCopyOption)))

(defn atomic-write!
  "Atomically replace file with UTF-8 source or throw without replacing it."
  [file source]
  (let [target (io/file file)
        parent (.getParentFile (.getAbsoluteFile target))
        tmp (java.io.File/createTempFile ".clj-surgeon-" ".tmp" parent)
        options (into-array CopyOption [StandardCopyOption/ATOMIC_MOVE
                                        StandardCopyOption/REPLACE_EXISTING])]
    (try
      (spit tmp source)
      (Files/move (.toPath tmp) (.toPath target) options)
      (finally
        (when (.exists tmp)
          (.delete tmp))))))
