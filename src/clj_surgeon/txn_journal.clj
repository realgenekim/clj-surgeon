(ns clj-surgeon.txn-journal
  "A disk-journaled transaction: hashes and spans resident, bytes on disk.

   The contract is OPTIMISTIC SERIALIZABILITY with conflict detection and exact
   rollback. It is not snapshot isolation. A writer that ignores the project
   lock can still land between this transaction's revalidation and its rename;
   what the journal guarantees is that such a race is DETECTED at read-back and
   that every path is restored to the exact pre-image bytes pinned before the
   first mutation. See `contract`.

   The memory posture is the reason the machinery exists. The transaction value
   retains, per open transaction, a bounded set of write-set records and one
   running digest. The read set - every file whose facts influenced the plan -
   is streamed to a sorted manifest on disk as it is observed and is never held
   as a collection, so heap is bounded by the WORK the receipt carries rather
   than by the size of the repository. Rollback bytes are pinned into a
   content-addressed store under the workspace's own state directory before any
   live write, so no source string is retained merely to make rollback possible.

   Adopted by no verb yet. This is the kernel; adoption is a separate build."
  (:require
   [clj-surgeon.file-ops :as file-ops]
   [clj-surgeon.mcp-paths :as mcp-paths]
   [clj-surgeon.mcp-workspace :as workspace]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.io File FileOutputStream)
   (java.nio.file Files LinkOption StandardCopyOption CopyOption)
   (java.nio.file.attribute PosixFilePermissions)
   (java.security MessageDigest)))

;; ------------------------------------------------------------ the contract

(defn contract
  ;; @spec MCP-OP-MEM-014
  "State the isolation this kernel provides, and the isolation it refuses to claim."
  []
  {:isolation :optimistic-serializability
   :snapshot-isolation false
   :detects [:read-set-modification
             :scope-membership-change
             :write-set-drift
             :read-back-mismatch
             :unpinned-write]
   :guarantees ["Every edit was planned against the pre-image digest recorded in the manifest."
                "No path is knowingly overwritten unless it still holds that digest."
                "Exact rollback bytes are durable before any path is changed."
                "A crash leaves a journal from which every begun path is restored and verified."]
   :does-not-promise ["A simultaneous repository snapshot against a writer that ignores the lock."
                      "Instantaneous atomicity of a multi-file commit to an unrelated reader."
                      "Protection against a writer that ignores the lock and races the rename; such a write is detected at read-back and rolled over, not prevented."]})

(def ^:private compact-isolation
  {:isolation :optimistic-serializability
   :snapshot-isolation false})

;; ------------------------------------------------------------------ limits

(def default-limits
  "Admission ceilings. A request may lower them; none may be raised past the
   server hard maximum, which is what `hard-limits` names."
  {:max-read-set-files 20000
   :max-staged-files 2000
   :max-journal-bytes (* 512 1024 1024)})

(def hard-limits
  {:max-read-set-files 200000
   :max-staged-files 20000
   :max-journal-bytes (* 4 1024 1024 1024)})

;; ------------------------------------------------------------------ digest

(defn sha256-string
  "Lowercase hex SHA-256 of a string's UTF-8 bytes."
  [^String value]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256")
                        (.getBytes value "UTF-8"))]
    (apply str (map #(format "%02x" (bit-and 0xff %)) digest))))

(defn sha256-file
  "Lowercase hex SHA-256 of a file, read in bounded chunks and never retained."
  [path]
  (let [digest (MessageDigest/getInstance "SHA-256")
        buffer (byte-array 65536)]
    (with-open [in (io/input-stream (io/file path))]
      (loop []
        (let [read (.read in buffer)]
          (when (pos? read)
            (.update digest buffer 0 read)
            (recur)))))
    (apply str (map #(format "%02x" (bit-and 0xff %)) (.digest digest)))))

;; ----------------------------------------------------------------- refusals

(defn- refusal
  [error-type message extra]
  (merge {:ok false
          :error-type error-type
          :error message
          :isolation compact-isolation
          :source_unchanged true}
         extra))

;; ------------------------------------------------------------------- files

(defn- posix-mode
  [path]
  (try
    (PosixFilePermissions/toString
      (Files/getPosixFilePermissions (.toPath (io/file path))
                                     (make-array LinkOption 0)))
    (catch Exception _ "unknown")))

(defn publish-file!
  "Replace `target` with the bytes of `source-file` through one atomic rename."
  [target source-file]
  (file-ops/atomic-publish! target source-file))

(defn- copy-file!
  [source target]
  (Files/copy (.toPath (io/file source))
              (.toPath (io/file target))
              ^"[Ljava.nio.file.CopyOption;"
              (into-array CopyOption [StandardCopyOption/REPLACE_EXISTING])))

(defn- sync-stream!
  [^FileOutputStream stream]
  (.flush stream)
  (.sync (.getFD stream)))

(defn- append-journal!
  "Append one durable journal line. Progress that is not fsynced is not progress."
  [txn line]
  (let [^FileOutputStream stream (:journal-stream @(:state txn))]
    (.write stream (.getBytes ^String (str line "\n") "UTF-8"))
    (sync-stream! stream)))

(defn- write-state!
  [txn status extra]
  (spit (io/file (:dir txn) "state.edn")
        (pr-str (merge {:txid (:txid txn)
                        :workspace-root (:workspace-root txn)
                        :status status}
                       extra))))

;; -------------------------------------------------------------------- lock

(defn- lock-file
  [transactions-dir]
  (io/file transactions-dir "LOCK"))

(defn- acquire-lock!
  [transactions-dir txid]
  (let [lock (lock-file transactions-dir)]
    (try
      (Files/createFile (.toPath lock)
                        (make-array java.nio.file.attribute.FileAttribute 0))
      (spit lock (pr-str {:txid txid
                          :pid (.pid (java.lang.ProcessHandle/current))
                          :acquired-at (str (java.time.Instant/now))}))
      {:ok true}
      (catch java.nio.file.FileAlreadyExistsException _
        (let [holder (try (read-string (slurp lock)) (catch Exception _ {}))]
          (refusal :txn-lock-held
                   (str "Another transaction holds the project lock: " (:txid holder))
                   {:holder-txid (:txid holder)
                    :holder-pid (:pid holder)
                    :next_call {:op :txn/recover
                                :workspace_root nil}
                    :remedy "Wait for the holder, or run recovery if its process is gone."}))))))

(defn- release-lock!
  [transactions-dir]
  (Files/deleteIfExists (.toPath (lock-file transactions-dir))))

;; ------------------------------------------------------------------- begin

(defn- new-txid []
  (str (System/currentTimeMillis) "-"
       (format "%08x" (long (rand Integer/MAX_VALUE)))))

(defn begin!
  ;; @spec MCP-OP-MEM-007
  ;; @spec MCP-OP-MEM-012
  "Open a transaction under the workspace's own durable state directory.

   Returns the transaction value, or a typed refusal. Nothing about the
   repository is read here: the read set arrives one entry at a time through
   `record-read!` and goes straight to the sorted manifest on disk."
  [workspace-root {:keys [state-home txid scope-walk] :as opts}]
  (let [resolved (workspace/canonical-root workspace-root)]
    (if-not (:ok resolved)
      (refusal :txn-workspace-refused (:error resolved) {})
      (let [root (:workspace-root resolved)
            transactions (workspace/transactions-dir root state-home)
            _ (.mkdirs (io/file transactions))
            txid (or txid (new-txid))
            lock (acquire-lock! transactions txid)]
        (if-not (:ok lock)
          lock
          (let [dir (io/file transactions txid)
                objects (io/file dir "objects")
                staging (io/file dir "staging")
                limits (reduce-kv (fn [acc k v]
                                    (assoc acc k (min (get opts k v) (get hard-limits k v))))
                                  {}
                                  default-limits)]
            (.mkdirs objects)
            (.mkdirs staging)
            (let [txn {:txid txid
                       :workspace-root root
                       ;; resolved once: confinement must not cost a realpath
                       ;; syscall per pinned file
                       :real-root (mcp-paths/real-root root)
                       :transactions-dir transactions
                       :dir (.getCanonicalPath dir)
                       :objects-dir (.getCanonicalPath objects)
                       :staging-dir (.getCanonicalPath staging)
                       :manifest-path (.getCanonicalPath (io/file dir "manifest.tsv"))
                       :limits limits
                       :state (atom
                                {:manifest-stream (FileOutputStream.
                                                    (io/file dir "manifest.tsv") true)
                                 :journal-stream (FileOutputStream.
                                                   (io/file dir "journal.log") true)
                                 :membership-digest (MessageDigest/getInstance "SHA-256")
                                 :read-set-count 0
                                 :last-path nil
                                 :sealed? false
                                 :journal-bytes 0
                                 :pinned {}
                                 :staged {}
                                 :written []
                                 :scope-walk scope-walk})}]
              (write-state! txn :open {:started-at (str (java.time.Instant/now))})
              (append-journal! txn (str "begin\t" txid))
              txn)))))))

;; -------------------------------------------------------------- read set

(defn- read-entry
  [path-or-entry]
  (if (map? path-or-entry)
    path-or-entry
    (let [path (.getCanonicalPath (io/file path-or-entry))]
      {:path path
       :bytes (Files/size (.toPath (io/file path)))
       :sha256 (sha256-file path)
       :mode (posix-mode path)})))

(defn record-read!
  ;; @spec MCP-OP-MEM-012
  "Record one read-set entry into the sorted manifest as it streams.

   Entries must arrive in ascending path order. The transaction retains the
   previous path and a running membership digest - two values, not a
   collection - so a repository-sized read set costs a repository-sized FILE
   and a constant amount of heap."
  [txn path-or-entry]
  (let [{:keys [path bytes sha256 mode] :as entry} (read-entry path-or-entry)
        state (:state txn)
        {:keys [sealed? last-path read-set-count]} @state
        limit (get-in txn [:limits :max-read-set-files])]
    (cond
      sealed?
      (refusal :txn-read-set-sealed
               "The read set is sealed; a new entry cannot influence a plan already made"
               {:path path :next_call nil})

      (and last-path (not (pos? (compare path last-path))))
      (refusal :txn-manifest-unsorted
               (str "Read-set entry " path " does not follow " last-path)
               {:path path :previous-path last-path :next_call nil})

      (>= read-set-count limit)
      (refusal :txn-read-set-too-large
               (str "The read set reached its ceiling of " limit " files at " path)
               {:path path
                :max-files limit
                :observed-at-least (inc read-set-count)
                :next_call {:op :txn/begin
                            :scope {:narrow-to "a subtree or namespace closure that fits the ceiling"}}
                :remedy (str "Narrow the scope so fewer than " limit " files influence the plan.")})

      :else
      (let [line (str read-set-count "\t" path "\t" bytes "\t" sha256 "\t" mode "\n")
            ^FileOutputStream stream (:manifest-stream @state)
            ^MessageDigest digest (:membership-digest @state)]
        (.write stream (.getBytes line "UTF-8"))
        (.update digest (.getBytes (str path "\t" sha256 "\n") "UTF-8"))
        (swap! state (fn [s] (-> s
                                 (assoc :last-path path)
                                 (update :read-set-count inc))))
        {:ok true :path path :sha256 sha256}))))

(defn seal-read-set!
  ;; @spec MCP-OP-MEM-007
  "Close the manifest and freeze the scope-membership digest."
  [txn]
  (let [state (:state txn)
        ^FileOutputStream stream (:manifest-stream @state)
        ^MessageDigest digest (:membership-digest @state)
        membership (apply str (map #(format "%02x" (bit-and 0xff %)) (.digest digest)))]
    (sync-stream! stream)
    (.close stream)
    (swap! state assoc :sealed? true :membership-digest-hex membership
           :manifest-stream nil :membership-digest nil)
    (append-journal! txn (str "sealed\t" (:read-set-count @state) "\t" membership))
    (write-state! txn :sealed {:read-set-files (:read-set-count @state)
                               :membership-digest membership})
    {:ok true
     :read-set-files (:read-set-count @state)
     :membership-digest membership}))

;; ---------------------------------------------------------------- retention

(def ^:private retained-string-limit 512)

(defn- walk-value
  "Fold `f` over every plain-data value reachable from `value`."
  [value f acc]
  (let [value (if (instance? clojure.lang.IDeref value) @value value)]
    (cond
      (map? value) (reduce-kv (fn [a k v] (walk-value v f (walk-value k f a))) (f acc value) value)
      (or (vector? value) (set? value) (seq? value) (list? value))
      (reduce (fn [a v] (walk-value v f a)) (f acc value) value)
      :else (f acc value))))

(defn retained-record-count
  ;; @spec MCP-OP-MEM-012
  "Number of per-path records reachable from the transaction value.

   A generic walk, not a hand-maintained counter: any map keyed by absolute
   paths, and any map carrying a `:path`, is counted wherever a maintainer
   parks it. The read set must contribute zero however large it is; the write
   set is bounded by `:max-staged-files` and is expected to contribute."
  [txn]
  (walk-value
    (:state txn)
    (fn [acc value]
      (cond
        (and (map? value) (contains? value :path)) (inc acc)
        (and (map? value) (seq value)
             (every? #(and (string? %) (str/starts-with? % "/")) (keys value)))
        (+ acc (count value))
        :else acc))
    0))

(defn retained-content-bytes
  ;; @spec MCP-OP-MEM-012
  "Total characters of every reachable string longer than 512 characters.

   A path, a hex digest and a transaction id are all far shorter than that;
   source text is not. A maintainer who parks a future-source or original-source
   map back into the transaction value makes this number nonzero."
  [txn]
  (walk-value
    (:state txn)
    (fn [acc value]
      (if (and (string? value) (> (count value) retained-string-limit))
        (+ acc (count value))
        acc))
    0))

(defn manifest-line-count
  "Lines in the on-disk read-set manifest, counted without holding it."
  [txn]
  (with-open [reader (io/reader (io/file (:manifest-path txn)))]
    (reduce (fn [n _] (inc n)) 0 (line-seq reader))))

(defn staged-file-count [txn] (count (:staged @(:state txn))))
(defn journal-bytes [txn] (:journal-bytes @(:state txn)))

;; ------------------------------------------------------------------- quota

(defn- admit-journal-bytes!
  "Reserve `bytes` of journal quota, or refuse before anything is copied."
  [txn bytes path kind]
  (let [state (:state txn)
        limit (get-in txn [:limits :max-journal-bytes])
        used (:journal-bytes @state)]
    (if (> (+ used bytes) limit)
      (refusal :txn-journal-quota-exceeded
               (str "Pinned and staged bytes would reach " (+ used bytes)
                    ", above the journal quota of " limit)
               {:path path
                :kind kind
                :max-bytes limit
                :observed-at-least (+ used bytes)
                :next_call {:op :txn/begin
                            :scope {:narrow-to "fewer files to modify, or a higher explicit journal quota"}}
                :remedy "Stage fewer files in one transaction, or raise max-journal-bytes."})
      (do (swap! state (fn [st]
                         (let [used (+ (:journal-bytes st) bytes)]
                           (-> st
                               (assoc :journal-bytes used)
                               (update :journal-bytes-peak (fnil max 0) used)))))
          {:ok true}))))

;; ------------------------------------------------------------- confinement

(defn- confined-path
  "Resolve `path` as a workspace-relative source path, or return a refusal.

   The journal takes absolute paths, so the path is relativised against the
   transaction's canonical root and handed to the SAME `mcp-paths` resolver every
   other write surface uses. A path outside the root relativises to segments
   containing `..`, which that resolver already rejects, so this adds no second
   confinement rule - it routes to the existing one."
  [txn path]
  (let [root (or (:real-root txn) (mcp-paths/real-root (:workspace-root txn)))
        absolute (.toPath (io/file (.getCanonicalPath (io/file path))))]
    (if-not (.startsWith absolute root)
      (refusal :txn-path-outside-workspace
               (str path " is outside the transaction's workspace root")
               {:path (str path)
                :workspace-root (.toString root)
                :next_call nil})
      (let [relative (.toString (.relativize root absolute))
            resolved (mcp-paths/resolve-source-path root relative)]
        (if (:ok resolved)
          {:ok true :path (:path resolved)}
          (refusal :txn-path-outside-workspace
                   (or (:error resolved) "The path is refused by workspace confinement")
                   {:path (str path)
                    :cause-error-type (:error_type resolved)
                    :next_call nil}))))))

;; --------------------------------------------------------------------- pin

(defn pin!
  ;; @spec MCP-OP-MEM-006
  "Copy a path's exact pre-image bytes into the transaction's object store.

   No path may be written until this has succeeded for it. The object is named
   by its own digest, so pinning the same bytes twice costs one copy."
  [txn path]
  (let [confined (confined-path txn path)
        path (or (:path confined) (.getCanonicalPath (io/file path)))
        file (io/file path)]
    (cond
      (not (:ok confined)) confined

      (not (.isFile file))
      (refusal :txn-pin-target-missing (str "Cannot pin a missing file: " path)
               {:path path :next_call nil})
      :else
      (let [bytes (Files/size (.toPath file))
            admitted (admit-journal-bytes! txn bytes path :pin)]
        (if-not (:ok admitted)
          admitted
          (let [digest (sha256-file path)
                object (io/file (:objects-dir txn) digest)]
            (when-not (.exists object)
              (copy-file! file object))
            (swap! (:state txn) assoc-in [:pinned path]
                   {:sha256 digest :bytes bytes :object (.getCanonicalPath object)})
            (append-journal! txn (str "pin\t" path "\t" digest "\t" bytes))
            {:ok true :path path :sha256 digest :bytes bytes}))))))

;; ------------------------------------------------------------------- stage

(defn stage!
  ;; @spec MCP-OP-MEM-006
  ;; @spec MCP-OP-MEM-012
  "Write a path's future bytes to a staging file. Nothing is retained in heap."
  [txn path content]
  (let [confined (confined-path txn path)
        path (or (:path confined) (.getCanonicalPath (io/file path)))
        state (:state txn)
        bytes (count (.getBytes ^String content "UTF-8"))
        staged (:staged @state)
        limit (get-in txn [:limits :max-staged-files])]
    (cond
      (not (:ok confined)) confined

      (and (not (contains? staged path)) (>= (count staged) limit))
      (refusal :txn-staged-files-too-many
               (str "The write set reached its ceiling of " limit " files at " path)
               {:path path :max-files limit
                :observed-at-least (inc (count staged))
                :next_call {:op :txn/begin
                            :scope {:narrow-to "fewer files to modify in one transaction"}}})

      :else
      (let [admitted (admit-journal-bytes! txn bytes path :stage)]
        (if-not (:ok admitted)
          admitted
          (let [path-id (format "%08d" (count staged))
                staging (io/file (:staging-dir txn) (str path-id ".new"))]
            (with-open [out (FileOutputStream. staging)]
              (.write out (.getBytes ^String content "UTF-8"))
              (sync-stream! out))
            (swap! state assoc-in [:staged path]
                   {:path-id path-id
                    :result-hash (sha256-string content)
                    :bytes bytes
                    :staging (.getCanonicalPath staging)})
            (append-journal! txn (str "stage\t" path "\t" path-id "\t" bytes))
            {:ok true :path path :bytes bytes}))))))

;; -------------------------------------------------------------- revalidate

(defn- manifest-entries
  "A reducible over the manifest: [path sha256] one line at a time."
  [txn]
  (reify clojure.lang.IReduceInit
    (reduce [_ f init]
      (with-open [reader (io/reader (io/file (:manifest-path txn)))]
        (reduce (fn [acc line]
                  (let [[_ path _ sha _] (str/split line #"\t" 5)]
                    (f acc [path sha])))
                init
                (line-seq reader))))))

(defn- conflict
  [path expected actual]
  (refusal :txn-conflict
           (str "The read set changed under the transaction at " path)
           {:path path
            :expected-hash expected
            :actual-hash actual
            :files-written 0
            :next_call {:op :txn/begin
                        :scope {:replan "re-plan against the current bytes"}}
            :remedy "Re-plan: a file whose facts shaped this plan no longer holds the bytes it was planned against."}))

(defn revalidate!
  ;; @spec MCP-OP-MEM-007
  "Re-hash the whole semantic read set, and re-walk scope membership.

   Every file whose facts influenced the plan is checked, not only the files
   about to be written: a caller or alias that shaped the plan can live in a
   file the transaction never touches. When the transaction was opened with a
   `:scope-walk`, membership is re-derived so an ADDED file - which could
   introduce a new caller - is a conflict too."
  [txn]
  (let [state @(:state txn)]
    (if-not (:sealed? state)
      (refusal :txn-not-sealed "The read set must be sealed before revalidation" {})
      (let [walk (:scope-walk state)
            walked (when walk (vec (walk)))
            result
            (reduce
              (fn [acc [path expected]]
                (let [file (io/file path)]
                  (cond
                    (not (.isFile file))
                    (reduced (conflict path expected nil))

                    :else
                    (let [actual (sha256-file path)]
                      (if (= expected actual)
                        (update acc :checked inc)
                        (reduced (conflict path expected actual)))))))
              {:ok true :checked 0}
              (manifest-entries txn))]
        (cond
          (not (:ok result)) result

          (and walked (not= (count walked) (:checked result)))
          (refusal :txn-scope-membership-changed
                   (str "The scope holds " (count walked) " files; " (:checked result)
                        " were planned against")
                   {:planned-files (:checked result)
                    :observed-files (count walked)
                    :files-written 0
                    :next_call {:op :txn/begin
                                :scope {:replan "re-plan against the current scope"}}
                    :remedy "Re-plan: files entered or left the scope after planning."})

          :else result)))))

;; ------------------------------------------------------------------ commit

(defn- restore-path!
  [txn path]
  (let [{:keys [sha256 object]} (get-in @(:state txn) [:pinned path])]
    (try
      (publish-file! path object)
      (let [actual (sha256-file path)]
        {:path path
         :status (if (= actual sha256) :verified :restore-hash-mismatch)
         :expected-hash sha256
         :actual-hash actual})
      (catch Exception e
        {:path path :status :restore-failed :error (.getMessage e)}))))

(defn- rollback-written!
  [txn]
  (let [written (:written @(:state txn))]
    (mapv #(restore-path! txn %) (reverse written))))

(defn- finish!
  [txn status]
  (let [state @(:state txn)]
    (when-let [^FileOutputStream stream (:manifest-stream state)]
      (try (.close stream) (catch Exception _ nil)))
    (append-journal! txn (name status))
    (write-state! txn status {:finished-at (str (java.time.Instant/now))})
    (when-let [^FileOutputStream stream (:journal-stream state)]
      (try (.close stream) (catch Exception _ nil)))
    (release-lock! (:transactions-dir txn))
    (when-not (:retain-dir? state)
      (doseq [file (reverse (file-seq (io/file (:dir txn))))]
        (Files/deleteIfExists (.toPath file))))))

(defn rollback!
  ;; @spec MCP-OP-MEM-006
  "Restore every path this transaction began writing, verify each, and end it."
  [txn]
  (let [restored (rollback-written! txn)]
    (finish! txn :rolled-back)
    {:ok (every? #(= :verified (:status %)) restored)
     :rolled-back true
     :isolation compact-isolation
     :paths restored}))

(defn commit!
  ;; @spec MCP-OP-MEM-006
  ;; @spec MCP-OP-MEM-007
  ;; @spec MCP-OP-MEM-014
  "Revalidate, then replace each staged path through one atomic rename.

   The order is load-bearing. Every staged path must already be pinned; the
   whole read set is revalidated before the first write; each path's pre-image
   digest is rechecked immediately before its own replacement; progress is
   fsynced to the journal on both sides of every rename; and the result is read
   back and verified. Any failure rolls every begun path back to its pinned
   bytes and verifies the restoration."
  ([txn] (commit! txn {}))
  ([txn {:keys [publish-fn before-publish after-publish]
         :or {publish-fn publish-file!}}]
   (let [state (:state txn)
         staged (:staged @state)
         pinned (:pinned @state)
         unpinned (first (sort (remove #(contains? pinned %) (keys staged))))]
     (cond
       unpinned
       (do (finish! txn :rolled-back)
           (refusal :txn-unpinned-write
                    (str "No durable pre-image was pinned for " unpinned)
                    {:path unpinned :files-written 0 :next_call nil
                     :remedy "Pin every write-set path before commit; rollback bytes must be durable first."}))

       :else
       (let [validation (revalidate! txn)]
         (if-not (:ok validation)
           (do (finish! txn :rolled-back)
               (assoc validation :files-written 0))
           (let [paths (sort (keys staged))]
             (append-journal! txn (str "commit-begin\t" (count paths)))
             (loop [remaining paths written 0]
               (if (empty? remaining)
                 (do (finish! txn :committed)
                     {:ok true
                      :committed true
                      :txid (:txid txn)
                      :files-written written
                      :read-set-files (:read-set-count @state)
                      :reserved {:journal-bytes (:journal-bytes @state)
                                 :journal-bytes-peak (:journal-bytes-peak @state 0)
                                 :journal-bytes-max (get-in txn [:limits :max-journal-bytes])
                                 :staged-files (count staged)
                                 :staged-files-max (get-in txn [:limits :max-staged-files])}
                      :isolation compact-isolation})
                 (let [path (first remaining)
                       {:keys [result-hash staging]} (get staged path)
                       h0 (get-in pinned [path :sha256])]
                   (when before-publish (before-publish path))
                   (let [current (sha256-file path)]
                     (if (not= current h0)
                       (let [restored (rollback-written! txn)]
                         (finish! txn :rolled-back)
                         (merge (conflict path h0 current)
                                {:files-written written
                                 :rolled-back (every? #(= :verified (:status %)) restored)
                                 :recovery restored}))
                       (let [published
                             (try
                               (append-journal! txn (str "write-begin\t" path "\t" h0))
                               (publish-fn path staging)
                               (swap! state update :written conj path)
                               (append-journal! txn (str "write-done\t" path "\t" result-hash))
                               (when after-publish (after-publish path))
                               {:ok true}
                               (catch Exception cause
                                 {:ok false :cause cause}))]
                         (if-not (:ok published)
                           (let [restored (rollback-written! txn)]
                             (finish! txn :rolled-back)
                             (refusal :txn-write-failed
                                      (str "Writing " path " failed: "
                                           (.getMessage ^Exception (:cause published)))
                                      {:path path
                                       :files-written written
                                       :cause-error-type (:error-type (ex-data (:cause published)))
                                       :rolled-back (every? #(= :verified (:status %)) restored)
                                       :recovery restored
                                       :next_call nil}))
                         (let [actual (sha256-file path)]
                           (if (= actual result-hash)
                             (recur (rest remaining) (inc written))
                             (let [restored (rollback-written! txn)]
                               (finish! txn :rolled-back)
                               (refusal :txn-read-back-mismatch
                                        (str "The bytes read back from " path
                                             " are not the bytes this transaction wrote")
                                        {:path path
                                         :expected-hash result-hash
                                         :actual-hash actual
                                         :files-written (inc written)
                                         :rolled-back (every? #(= :verified (:status %)) restored)
                                         :recovery restored
                                         :next_call nil
                                         :remedy "Another writer that does not hold the project lock raced this rename; the contract detects that rather than preventing it."}))))))))))))))))))

;; ---------------------------------------------------------------- recovery

(defn- journal-lines
  [dir]
  (let [file (io/file dir "journal.log")]
    (if (.isFile file)
      (with-open [reader (io/reader file)]
        (vec (line-seq reader)))
      [])))

(defn- recover-transaction!
  [dir]
  (let [lines (journal-lines dir)
        pins (reduce (fn [acc line]
                       (let [[op path digest] (str/split line #"\t")]
                         (if (= "pin" op) (assoc acc path digest) acc)))
                     {}
                     lines)
        begun (vec (distinct
                     (keep (fn [line]
                             (let [[op path] (str/split line #"\t")]
                               (when (= "write-begin" op) path)))
                           lines)))
        restored
        (mapv (fn [path]
                (let [digest (get pins path)
                      object (io/file dir "objects" digest)]
                  (try
                    (if (.isFile object)
                      (do (publish-file! path (.getCanonicalPath object))
                          (let [actual (sha256-file path)]
                            {:path path
                             :status (if (= actual digest) :verified :restore-hash-mismatch)
                             :expected-hash digest
                             :actual-hash actual}))
                      {:path path :status :pre-image-missing :expected-hash digest})
                    (catch Exception e
                      {:path path :status :restore-failed :error (.getMessage e)}))))
              (reverse begun))]
    ;; remove any staging temporary the dead process left inside the tree
    (doseq [line lines]
      (let [[op path] (str/split line #"\t")]
        (when (= "write-begin" op)
          (doseq [^File sibling (.listFiles (.getParentFile (io/file path)))]
            (when (str/starts-with? (.getName sibling) ".clj-surgeon-publish-")
              (Files/deleteIfExists (.toPath sibling)))))))
    {:txid (.getName (io/file dir))
     :paths restored
     :ok (every? #(= :verified (:status %)) restored)}))

(defn recover!
  ;; @spec MCP-OP-MEM-013
  "Roll back every unfinished transaction found for this workspace.

   Recovery reads only the journal: the pin lines say which pre-image bytes are
   durable, and the write-begin lines say which paths were begun. Each restored
   path is verified against the digest that was pinned before the first write."
  ([workspace-root] (recover! workspace-root {}))
  ([workspace-root {:keys [state-home]}]
   (let [transactions (workspace/transactions-dir workspace-root state-home)
         dir (io/file transactions)
         candidates (when (.isDirectory dir)
                      (filter (fn [^File d]
                                (and (.isDirectory d)
                                     (let [state (io/file d "state.edn")]
                                       (or (not (.isFile state))
                                           (not (contains? #{:committed :rolled-back}
                                                           (:status (read-string (slurp state)))))))))
                              (.listFiles dir)))
         results (mapv (fn [^File d]
                         (let [result (recover-transaction! (.getCanonicalPath d))]
                           (spit (io/file d "state.edn")
                                 (pr-str {:txid (.getName d) :status :rolled-back
                                          :recovered-at (str (java.time.Instant/now))}))
                           (doseq [file (reverse (file-seq d))]
                             (Files/deleteIfExists (.toPath file)))
                           result))
                       candidates)]
     (when (seq results)
       (release-lock! transactions))
     {:ok (every? :ok results)
      :transactions-recovered (count results)
      :isolation compact-isolation
      :paths (vec (mapcat :paths results))})))
