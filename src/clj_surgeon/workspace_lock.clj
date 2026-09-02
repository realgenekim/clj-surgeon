(ns clj-surgeon.workspace-lock
  "Serialise workspace writes across threads and across server processes.

  The transaction kernel guards a write with a compare-and-swap on the source
  hash, which is a check-then-act pair with no mutual exclusion between the
  check and the act. Two writers that read the same bytes both pass the check.
  Measured on one file with eight concurrent one-line commits, four of six
  trials lost an edit that its own receipt reported as committed, and some runs
  ended in the kernel's manual-recovery state.

  A hash guard answers `did this change?`. It cannot answer `may I write now?`.
  This namespace answers the second question and leaves the first where it is:
  one JVM monitor per canonical workspace root serialises threads, and an
  advisory file lock under an existing `.clj-surgeon` directory serialises
  separate server processes on the same tree."
  (:require
   [clojure.java.io :as io])
  (:import
   (java.nio.channels FileChannel OverlappingFileLockException)
   (java.nio.file OpenOption StandardOpenOption)))

(def lock-directory-name ".clj-surgeon")
(def lock-file-name "write.lock")

(defonce ^:private monitors (atom {}))

(defn- monitor-for
  "One process-lifetime monitor object per canonical workspace root."
  [root]
  (let [key (str root)]
    (get (swap! monitors
                (fn [current]
                  (if (contains? current key)
                    current
                    (assoc current key (Object.)))))
         key)))

(defn advisory-lock-file
  "The cross-process lock file, or nil when the workspace has no state dir.

  The directory is never created here. A workspace that has never been
  onboarded gets thread-level serialisation only, which is the honest
  behaviour: this namespace refuses to scatter directories through a tree it
  was only asked to write one file in."
  [root]
  (let [directory (io/file (str root) lock-directory-name)]
    (when (.isDirectory directory)
      (io/file directory lock-file-name))))

;; @spec MCP-OP-ADMIT-088
(defn- open-lock-channel
  "Open the advisory lock file, or refuse in a way a caller can publish.

  A tree can be hostile or merely broken: the state directory may be
  read-only, or `write.lock` may already be a directory. Either way the gate
  must say what it could not lock and leave the source alone, rather than
  surfacing an IOException as an unexplained tool failure."
  [^java.io.File file]
  (try
    (FileChannel/open (.toPath file)
                      (into-array OpenOption
                                  [StandardOpenOption/CREATE
                                   StandardOpenOption/WRITE]))
    (catch Exception error
      (throw (ex-info (str "Cannot take the workspace write lock at "
                           (.getPath file) ": " (.getMessage error))
                      {:error-type :workspace-lock-unavailable
                       :lock-path (.getPath file)
                       :cause-error-type (.getName (class error))})))))

(defn- with-advisory-lock
  [^java.io.File file thunk]
  (with-open [channel (open-lock-channel file)]
    (let [lock (try
                 (.lock channel)
                 (catch OverlappingFileLockException _
                   ;; Another thread of this JVM holds it. The monitor above
                   ;; makes this unreachable; treat it as already held rather
                   ;; than failing a write that is correctly serialised.
                   nil))]
      (try
        (thunk)
        (finally
          (when lock (.release lock)))))))

;; @spec MCP-OP-ADMIT-087
(defn lock-scope
  "How far the lock this root can take actually reaches.

  `:cross-process` when an advisory file lock is available, `:process` when
  serialisation is only between the threads of one server. The difference is
  a real difference in the guarantee, so a receipt has to be able to state it
  rather than leaving a reader to infer it from a design document."
  [root]
  (if-let [file (advisory-lock-file root)]
    {:scope :cross-process :lock-path (.getPath file)}
    {:scope :process}))

(defn call-with-workspace-write-lock
  "Run thunk with exclusive write authority over one canonical workspace root."
  [root thunk]
  (locking (monitor-for root)
    (if-let [file (advisory-lock-file root)]
      (with-advisory-lock file thunk)
      (thunk))))

(defmacro with-workspace-write-lock
  [root & body]
  `(call-with-workspace-write-lock ~root (fn [] ~@body)))
