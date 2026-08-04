(ns clj-surgeon.file-ops
  "Shared fail-closed filesystem operations."
  (:require
   [clojure.java.io :as io])
  (:import
   (java.nio.file CopyOption Files LinkOption StandardCopyOption)))

(defn- preserve-existing-permissions! [target tmp]
  (when (.exists target)
    (try
      (let [link-options (make-array LinkOption 0)
            permissions (Files/getPosixFilePermissions (.toPath target)
                          link-options)]
        (Files/setPosixFilePermissions (.toPath tmp) permissions))
      (catch UnsupportedOperationException _
        (.setReadable tmp (.canRead target) false)
        (.setWritable tmp (.canWrite target) false)
        (.setExecutable tmp (.canExecute target) false)))))

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
      (preserve-existing-permissions! target tmp)
      (Files/move (.toPath tmp) (.toPath target) options)
      (finally
        (when (.exists tmp)
          (.delete tmp))))))
