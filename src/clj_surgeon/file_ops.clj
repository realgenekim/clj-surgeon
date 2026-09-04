(ns clj-surgeon.file-ops
  "Shared fail-closed filesystem operations."
  (:require
   [clojure.java.io :as io])
  (:import
   (java.io File)
   (java.nio.channels FileChannel)
   (java.util.concurrent.locks ReentrantLock)
   (java.nio.file CopyOption Files LinkOption OpenOption StandardCopyOption
                  StandardOpenOption)))

;; ------------------------------------------------------- the publish lock

(def ^:dynamic *publish-lock-dir*
  "The workspace transactions directory whose `PUBLISH.lock` this thread's
   `atomic-write!` calls must take, or nil.

   Cooperation with the transaction kernel's publish lock is PER-WRITER and
   OPT-IN, and this var is the opt-in. An advisory lock excludes only writers
   that ask for it; before this var existed the kernel's own commit path was
   the only caller in the repository, so the lock excluded nobody who existed
   and the kernel's residual recheck-to-rename window was the normal path
   rather than the exception. Binding it - see
   `clj-surgeon.txn-journal/with-cooperating-writes` - makes every ordinary
   atomic write on this thread a cooperating writer. Leaving it nil is the
   default and changes nothing."
  nil)

(defn publish-lock-file
  "The workspace's advisory publish lock file."
  ^File [transactions-dir]
  (io/file transactions-dir "PUBLISH.lock"))

(defonce ^{:private true
           :doc "One in-JVM mutex per canonical lock path, process-wide.

   `FileChannel/lock` is a PER-PROCESS view of the OS lock, not a per-thread
   one: a second thread in the same JVM that calls `.lock` on a file this JVM
   already holds gets `OverlappingFileLockException` thrown at it rather than
   blocking. Two cooperating threads therefore cannot serialise on the OS lock
   alone - they have to serialise BEFORE it, in this process, which is what
   this table is. It is keyed by the lock file's canonical path so that two
   spellings of one workspace share one monitor."}
  publish-monitors
  (atom {}))

(defn- publish-monitor
  ^ReentrantLock [^String lock-path]
  (or (get @publish-monitors lock-path)
      (get (swap! publish-monitors
                  (fn [table]
                    (if (contains? table lock-path)
                      table
                      (assoc table lock-path (ReentrantLock.)))))
           lock-path)))

(defn with-publish-lock*
  "Run `f` while holding the workspace's advisory publish lock.

   An OS advisory lock (`flock` semantics through `FileChannel/lock`) on the
   workspace's own state root. It excludes any writer that ASKS for it - a
   second clj-surgeon transaction, a cooperating editor - and it cannot
   exclude one that does not. That is the whole of what an advisory lock buys,
   and the residual window is documented rather than papered over.

   Re-entrant on the same thread and the same directory, because the JVM's own
   lock table is not.

   THREADS SERIALISE IN THIS PROCESS BEFORE THE OS LOCK IS TAKEN. The OS lock
   is per-process: a second thread calling `.lock` on a file this JVM already
   holds is thrown `OverlappingFileLockException` rather than made to wait, and
   an exception is not mutual exclusion - it escaped `commit!` before
   `finish!` could release the project lock and stranded the workspace behind
   a LIVE pid no recovery is permitted to break. The per-path `ReentrantLock`
   is what turns that throw back into a wait.

   RE-ENTRANCY IS THE MONITOR'S OWN HOLD COUNT, WHICH IS PER THREAD. It used to
   be a dynamic var, and Clojure CONVEYS dynamic bindings to `future`, `send`,
   `pmap` and every `bound-fn`: a future spawned inside the lock inherited the
   claim, carried it out of the owning thread's dynamic extent, and then wrote
   with no lock at all against a lock another process was holding. A thread
   either holds this monitor or it does not, and nothing it spawns can inherit
   that."
  [transactions-dir f]
  (let [^File file (publish-lock-file transactions-dir)
        _ (.mkdirs (.getParentFile (.getAbsoluteFile file)))
        ^ReentrantLock monitor (publish-monitor (.getCanonicalPath file))]
    (.lock monitor)
    (try
      (if (> (.getHoldCount monitor) 1)
        ;; THIS thread is already inside the OS lock; re-entering it would
        ;; throw. `getHoldCount` counts holds by the current thread and by no
        ;; other, so nothing this thread spawned can answer yes here.
        (f)
        (with-open [channel (FileChannel/open
                              (.toPath file)
                              ^"[Ljava.nio.file.OpenOption;"
                              (into-array OpenOption [StandardOpenOption/CREATE
                                                      StandardOpenOption/WRITE]))]
          (let [lock (.lock channel)]
            (try
              (f)
              (finally (.release lock))))))
      (finally (.unlock monitor)))))

(defn with-publish-lock-dir*
  "Run `f` with every `atomic-write!` on this thread taking `transactions-dir`'s
   publish lock."
  [transactions-dir f]
  (binding [*publish-lock-dir* transactions-dir] (f)))

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
  "Atomically replace file with UTF-8 source or throw without replacing it.

   Takes the workspace publish lock when `*publish-lock-dir*` is bound, which
   is what makes an ordinary writer a COOPERATING one for the transaction
   kernel. Unbound - the default - it takes no lock and behaves exactly as
   before."
  [file source]
  (let [write!
        (fn []
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
                  (.delete tmp))))))]
    (if-let [dir *publish-lock-dir*]
      (with-publish-lock* dir write!)
      (write!))))

(defn prepare-publish!
  "Copy `source-file` into `file`'s OWN directory and return the temporary.

   This is the EXPENSIVE half of publication, and it is a separate verb so a
   caller that must recheck the target immediately before replacing it can do
   the copying BEFORE the recheck. Everything that remains between a recheck
   and the replacement is then one rename. The temporary carries the target's
   current permissions, so the rename preserves them."
  ^java.io.File [file source-file]
  (let [target (io/file file)
        parent (.getParentFile (.getAbsoluteFile target))
        tmp (java.io.File/createTempFile ".clj-surgeon-publish-" ".tmp" parent)]
    (try
      (Files/copy (.toPath (io/file source-file))
                  (.toPath tmp)
                  ^"[Ljava.nio.file.CopyOption;"
                  (into-array CopyOption [StandardCopyOption/REPLACE_EXISTING]))
      (preserve-existing-permissions! target tmp)
      tmp
      (catch Throwable cause
        (.delete tmp)
        (throw cause)))))

(defn publish-prepared!
  "Rename a prepared temporary over `file`. One atomic rename and nothing else."
  [file ^java.io.File tmp]
  (Files/move (.toPath tmp)
              (.toPath (io/file file))
              ^"[Ljava.nio.file.CopyOption;"
              (into-array CopyOption [StandardCopyOption/ATOMIC_MOVE
                                      StandardCopyOption/REPLACE_EXISTING])))

(defn atomic-publish!
  "Atomically replace `file` with the contents of `source-file`.

   `atomic-write!` takes a string; a transaction journal holds its future
   bytes in a staging FILE that may live on another filesystem, where
   ATOMIC_MOVE is not available. Publication therefore copies the staging file
   into the target's own directory first and renames from there, so the
   observable replacement is still one atomic rename and the target's
   permissions survive it."
  [file source-file]
  (let [tmp (prepare-publish! file source-file)]
    (try
      (publish-prepared! file tmp)
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
