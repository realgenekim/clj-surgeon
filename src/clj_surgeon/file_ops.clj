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

(defn atomic-create!
  ;; @spec MCP-OP-EDIT-035
  "Atomically publish new UTF-8 source or throw without replacing any target.

   A same-directory hard link gives the fully written temporary inode its
   destination name with create-if-absent semantics. Unlike ATOMIC_MOVE, this
   cannot replace a target that appears between the caller's guard and the
   publication primitive."
  [file source]
  (let [target (io/file file)
        parent (.getParentFile (.getAbsoluteFile target))
        tmp (java.io.File/createTempFile ".clj-surgeon-create-" ".tmp" parent)]
    (try
      (spit tmp source)
      (Files/createLink (.toPath target) (.toPath tmp))
      (finally
        (when (.exists tmp)
          (.delete tmp))))))

(defn revalidate-create-target!
  ;; @spec MCP-OP-EDIT-036
  "Reject a create target whose existing ancestor chain is no longer confined.

   workspace-root is the real root captured during path planning. Existing
   descendants are checked without following their final path component, so a
   symlink introduced after planning cannot redirect publication."
  [workspace-root file]
  (when workspace-root
    (let [no-follow (into-array java.nio.file.LinkOption
                                [java.nio.file.LinkOption/NOFOLLOW_LINKS])
          root (.toRealPath (.toPath (io/file workspace-root))
                            (make-array java.nio.file.LinkOption 0))
          target (.normalize (.toAbsolutePath (.toPath (io/file file))))
          ancestors
          (loop [candidate (.getParent target)
                 found ()]
            (cond
              (nil? candidate)
              (throw (ex-info "Creation target has no confined root ancestor"
                              {:error-type :target-ancestor-changed
                               :file file :path file}))

              (= candidate root)
              (vec found)

              (not (.startsWith candidate root))
              (throw (ex-info "Creation target escaped its planned workspace root"
                              {:error-type :target-ancestor-changed
                               :file file :path file
                               :workspace-root (.toString root)}))

              :else
              (recur (.getParent candidate) (conj found candidate))))]
      (doseq [ancestor ancestors]
        (when (Files/exists ancestor no-follow)
          (when (Files/isSymbolicLink ancestor)
            (throw (ex-info "Creation target ancestor became a symbolic link"
                            {:error-type :target-ancestor-changed
                             :file file :path file
                             :ancestor (.toString ancestor)})))
          (when-not (Files/isDirectory ancestor no-follow)
            (throw (ex-info "Creation target ancestor is no longer a directory"
                            {:error-type :target-ancestor-changed
                             :file file :path file
                             :ancestor (.toString ancestor)})))
          (let [real-ancestor (.toRealPath ancestor no-follow)]
            (when-not (.startsWith real-ancestor root)
              (throw (ex-info "Creation target ancestor escaped its workspace root"
                              {:error-type :target-ancestor-changed
                               :file file :path file
                               :ancestor (.toString ancestor)
                               :workspace-root (.toString root)})))))))
    true))
