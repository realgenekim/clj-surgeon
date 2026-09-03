(ns clj-surgeon.txn-journal
  "A disk-journaled transaction: hashes and spans resident, bytes on disk.

   The contract is OPTIMISTIC SERIALIZABILITY with conflict detection and exact
   rollback. It is not snapshot isolation. An atomic rename is not a
   compare-and-swap: the pre-image recheck and the rename are two syscalls, and
   a writer that does not take the publish lock can land between them. That
   RESIDUAL WINDOW is bounded to a digest recheck, an identity recheck, one
   journal fsync and one rename - the staged bytes are copied into the target's
   own directory BEFORE the window opens - it is measured into every receipt,
   and the pinned pre-image journal is its recovery. Everything earlier than the
   window is detected and refused; a write inside it is overwritten, and the
   contract says so rather than claiming it was prevented. See `contract`.

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
   (java.nio.channels FileChannel)
   (java.nio.file Files LinkOption OpenOption StandardCopyOption StandardOpenOption CopyOption)
   (java.nio.file.attribute BasicFileAttributes PosixFilePermissions)
   (java.security MessageDigest)))

;; ------------------------------------------------------------ the contract

(def commit-window
  "What is left between a path's pre-image recheck and its replacement.

   This is the honest statement of what optimistic serializability costs. The
   window cannot be closed on a POSIX filesystem - rename replaces, it does not
   compare-and-swap - so the kernel narrows it instead and names its bound:
   the staged bytes are already in the target's own directory, so no byte
   copying happens inside, and what remains is a digest read, an identity stat,
   one fsync and one rename, taken under the workspace publish lock."
  {:ops [:recheck-digest :recheck-identity :journal-write-begin :rename]
   :staging-copy-inside false
   :lock :workspace-publish-lock
   :residual "a writer that does not take the publish lock and lands inside this window is overwritten; the pinned pre-image journal is the recovery"})

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
                "The recheck and the rename are taken under the workspace publish lock."
                "Exact rollback bytes are durable before any path is changed."
                "A crash leaves a journal from which every begun path is restored and verified."]
   :commit-window commit-window
   :does-not-promise ["A simultaneous repository snapshot against a writer that ignores the lock."
                      "Instantaneous atomicity of a multi-file commit to an unrelated reader."
                      "Protection against a writer that ignores the lock and races the rename; such a write is detected at read-back and rolled over, not prevented."
                      (str "Exclusion of a writer that does not take the publish lock and lands inside the residual recheck-to-rename window: "
                           "an atomic rename is not a compare-and-swap, so such a write is OVERWRITTEN rather than detected. "
                           "The window holds no byte copying and is measured into every receipt; the pinned pre-image journal is its recovery.")]})

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

(defn path-identity
  "The NOFOLLOW type and filesystem identity of `path`.

   Read with `NOFOLLOW_LINKS`, so a symbolic link reports ITSELF rather than
   what it points at, and `fileKey` is the (device, inode) pair on every
   filesystem that has one. Two files can hold identical bytes and still not be
   the same file; a content digest cannot tell them apart and this can."
  [path]
  (try
    (let [attrs (Files/readAttributes
                  (.toPath (io/file path))
                  BasicFileAttributes
                  ^"[Ljava.nio.file.LinkOption;"
                  (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))]
      {:kind (cond
               (.isSymbolicLink attrs) :symlink
               (.isRegularFile attrs) :regular
               (.isDirectory attrs) :directory
               :else :other)
       :file-key (str (.fileKey attrs))})
    (catch Exception _ {:kind :absent :file-key nil})))

(defn- hex
  [^bytes bs]
  (apply str (map #(format "%02x" (bit-and 0xff %)) bs)))

(defn- scope-membership-digest
  "Fold a scope walk into ONE digest over path, type and file identity.

   Membership is a SET, and a count is not a set: `[a b]` and `[c d]` have the
   same count and are different scopes. The digest is order-sensitive by
   construction, so the walk must yield a deterministic ascending order - an
   unstable walk reads as a changed scope, which is fail-closed. The kernel
   retains only the digest and the count; what the caller's walk itself holds
   is the caller's business."
  [walk]
  (let [digest (MessageDigest/getInstance "SHA-256")]
    (loop [entries (seq (walk)) n 0]
      (if-not entries
        {:digest (hex (.digest digest)) :files n}
        (let [path (first entries)
              {:keys [kind file-key]} (path-identity path)]
          (.update digest (.getBytes (str path "\t" (name kind) "\t" file-key "\n")
                                     "UTF-8"))
          (recur (next entries) (inc n)))))))

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

(defn prepare-publish!
  "Copy a staged path's future bytes into the TARGET's own directory.

   The expensive half of publication, done before the pre-image recheck so the
   recheck-to-rename window holds no byte copying."
  ^File [target source-file]
  (file-ops/prepare-publish! target source-file))

(defn publish-prepared!
  "Rename a prepared temporary over `target`. One atomic rename, nothing else."
  [target prepared]
  (file-ops/publish-prepared! target prepared))

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

(defn- publish-lock-file
  ^File [transactions-dir]
  (io/file transactions-dir "PUBLISH.lock"))

(defn- with-publish-lock*
  "Run `f` while holding the workspace's advisory publish lock.

   An OS advisory lock (`flock` semantics through `FileChannel/lock`) on the
   workspace's own state root, taken around the pre-image recheck and the
   rename. It excludes any writer that ASKS for it - a second clj-surgeon
   transaction, a cooperating editor - and it cannot exclude one that does not.
   That is the whole of what an advisory lock buys, and the residual window is
   documented rather than papered over."
  [txn f]
  (let [file (publish-lock-file (:transactions-dir txn))]
    (with-open [channel (FileChannel/open
                          (.toPath file)
                          ^"[Ljava.nio.file.OpenOption;"
                          (into-array OpenOption [StandardOpenOption/CREATE
                                                  StandardOpenOption/WRITE]))]
      (let [lock (.lock channel)]
        (try (f) (finally (.release lock)))))))

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
  "Close the manifest and freeze both membership digests.

   Two different memberships are sealed here. The READ-SET digest covers every
   entry recorded through `record-read!`. The SCOPE digest covers the walk the
   transaction was opened with, and it is what makes an equal-count swap of
   scope members - `[a b]` planned, `[c d]` observed - a conflict."
  [txn]
  (let [state (:state txn)
        ^FileOutputStream stream (:manifest-stream @state)
        ^MessageDigest digest (:membership-digest @state)
        membership (hex (.digest digest))
        walk (:scope-walk @state)
        scope (when walk (scope-membership-digest walk))]
    (sync-stream! stream)
    (.close stream)
    (swap! state assoc :sealed? true :membership-digest-hex membership
           :scope-digest-hex (:digest scope) :scope-files (:files scope)
           :manifest-stream nil :membership-digest nil)
    (append-journal! txn (str "sealed\t" (:read-set-count @state) "\t" membership
                              "\t" (:digest scope) "\t" (:files scope)))
    (write-state! txn :sealed {:read-set-files (:read-set-count @state)
                               :membership-digest membership
                               :scope-digest (:digest scope)
                               :scope-files (:files scope)})
    (cond-> {:ok true
             :read-set-files (:read-set-count @state)
             :membership-digest membership}
      scope (assoc :scope-digest (:digest scope)
                   :scope-files (:files scope)))))

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

(defn- outside-workspace
  [path root cause message]
  (refusal :txn-path-outside-workspace message
           {:path (str path)
            :cause cause
            :workspace-root (str root)
            :next_call nil}))

(defn- confined-path
  "Resolve `path` as a workspace-relative source path, or return a refusal.

   THE LEXICAL CHECK COMES FIRST, and that ordering is the whole point.
   `getCanonicalPath` deletes `..` segments before any resolver can object, so
   `<root>/src/../src/in.clj` canonicalises to a path inside the root and a
   confinement rule expressed only in terms of the canonical form never fires.
   A parent-traversal segment, a relative path and an absolute path that does
   not lie under the root are therefore refused on the RAW string, before
   canonicalisation touches it.

   What remains after that is the ordinary route: the path is relativised
   against the transaction's canonical root and handed to the SAME `mcp-paths`
   resolver every other write surface uses, so the symlink-escape and
   not-a-regular-file rules stay in one place."
  [txn path]
  (let [root (or (:real-root txn) (mcp-paths/real-root (:workspace-root txn)))
        raw (str path)
        segments (str/split (str/replace raw "\\" "/") #"/" -1)
        ^File file (io/file raw)]
    (cond
      (some #(= ".." %) segments)
      (outside-workspace raw root :lexical-parent-traversal
                         (str raw " contains a parent-traversal segment and is refused"
                              " before canonicalisation can remove it"))

      (not (.isAbsolute file))
      (outside-workspace raw root :not-absolute
                         (str raw " is not absolute; the journal takes absolute paths"
                              " named under the workspace's real root"))

      (not (.startsWith (.toPath file) root))
      (outside-workspace raw root :outside-root
                         (str raw " is not named under the transaction's workspace root"))

      :else
      (let [absolute (.toPath (io/file (.getCanonicalPath file)))]
        (if-not (.startsWith absolute root)
          (outside-workspace raw root :outside-root
                             (str path " is outside the transaction's workspace root"))
          (let [relative (.toString (.relativize root absolute))
                resolved (mcp-paths/resolve-source-path root relative)]
            (if (:ok resolved)
              {:ok true :path (:path resolved)}
              (refusal :txn-path-outside-workspace
                       (or (:error resolved) "The path is refused by workspace confinement")
                       {:path (str path)
                        :cause :resolver-refused
                        :cause-error-type (:error_type resolved)
                        :next_call nil}))))))))

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
                object (io/file (:objects-dir txn) digest)
                identity (path-identity path)]
            (when-not (.exists object)
              (copy-file! file object))
            (swap! (:state txn) assoc-in [:pinned path]
                   {:sha256 digest :bytes bytes :identity identity
                    :object (.getCanonicalPath object)})
            (append-journal! txn (str "pin\t" path "\t" digest "\t" bytes
                                      "\t" (name (:kind identity))
                                      "\t" (:file-key identity)))
            {:ok true :path path :sha256 digest :bytes bytes
             :identity identity}))))))

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

(defn- identity-conflict
  [path expected actual]
  (refusal :txn-conflict
           (str "The path " path " no longer names the file whose pre-image was pinned")
           {:path path
            :conflict :identity-changed
            :expected-identity expected
            :actual-identity actual
            :files-written 0
            :next_call {:op :txn/begin
                        :scope {:replan "re-plan against the file that is there now"}}
            :remedy (str "Re-plan: the bytes may still match, but the path now names a "
                         "different filesystem object - a new inode, or a symbolic link "
                         "where a regular file was pinned. Writing through it would "
                         "replace something this transaction never read.")}))

(defn- identity-drift
  "The first pinned path whose NOFOLLOW type or file identity is not the pinned
   one, as a typed conflict, or nil."
  [txn]
  (some (fn [[path {:keys [identity]}]]
          (let [current (path-identity path)]
            (when (not= identity current)
              (identity-conflict path identity current))))
        (sort-by key (:pinned @(:state txn)))))

(defn revalidate!
  ;; @spec MCP-OP-MEM-007
  "Re-hash the whole semantic read set, re-check pinned identity, and re-walk
   scope membership.

   Every file whose facts influenced the plan is checked, not only the files
   about to be written: a caller or alias that shaped the plan can live in a
   file the transaction never touches. Every PINNED path is checked twice over -
   its bytes and its NOFOLLOW type and file identity - because a regular file
   swapped for a symbolic link to identical bytes has the same digest and is not
   the same file. When the transaction was opened with a `:scope-walk`,
   membership is re-derived and compared against the digest sealed at plan time
   - not against its COUNT - so an ADDED file, a REMOVED file, and a swap that
   leaves the count unchanged are all conflicts."
  [txn]
  (let [state @(:state txn)]
    (if-not (:sealed? state)
      (refusal :txn-not-sealed "The read set must be sealed before revalidation" {})
      (let [walk (:scope-walk state)
            walked (when walk (scope-membership-digest walk))
            drift (identity-drift txn)
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

          drift drift

          (and walked (not= (:digest walked) (:scope-digest-hex state)))
          (refusal :txn-scope-membership-changed
                   (str "The scope this plan was sealed against is not the scope on disk: "
                        (:scope-files state) " files planned, " (:files walked) " observed")
                   {:conflict :scope-membership
                    :planned-files (:scope-files state)
                    :observed-files (:files walked)
                    :planned-digest (:scope-digest-hex state)
                    :observed-digest (:digest walked)
                    :files-written 0
                    :next_call {:op :txn/begin
                                :scope {:replan "re-plan against the current scope"}}
                    :remedy "Re-plan: the set of files in the scope is not the set that shaped this plan."})

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

(defn- delete-tree!
  [dir]
  (doseq [file (reverse (file-seq (io/file dir)))]
    (Files/deleteIfExists (.toPath file))))

(defn- dir-bytes
  [dir]
  (reduce + 0 (map #(.length ^File %) (filter #(.isFile ^File %) (file-seq (io/file dir))))))

(defn- write-lease!
  "Record why this journal is retained and what still references it.

   A journal is not garbage the moment its transaction ends. A committed
   transaction's receipt is only undoable while its pre-images exist, and a
   restoration that did NOT verify has left the tree in a state only this
   journal can describe. The lease is the refcount a quota sweep must consult."
  [txn status]
  (spit (io/file (:dir txn) "lease.edn")
        (pr-str {:txid (:txid txn)
                 :status status
                 :receipt-refs (if (= :committed status) 1 0)
                 :evictable (not= :restore-failed status)
                 :retained-at (str (java.time.Instant/now))
                 :bytes (dir-bytes (:dir txn))})))

(defn- reclaim-staging!
  "Drop the staging files of a committed transaction.

   Their bytes are now the bytes in the tree; the PRE-images are what a receipt
   needs to be undoable, and those stay."
  [txn]
  (doseq [^File file (.listFiles (io/file (:staging-dir txn)))]
    (Files/deleteIfExists (.toPath file))))

(defn- finish!
  "End the transaction, and decide whether its journal survives it.

   Retention policy, and the reason for each row:

   | outcome          | journal   | why |
   |------------------|-----------|-----|
   | `:committed`     | RETAINED  | a receipt that cannot be undone is not a receipt; the pre-images are the undo |
   | `:rolled-back`   | discarded | every path verified back at H0, so there is nothing left to recover |
   | `:restore-failed`| RETAINED  | the tree is not at H0 and this is the ONLY material that can repair it |

   The third row is the one that matters. Deleting the journal of a failed
   restoration destroys the evidence and the repair material at exactly the
   moment both are needed."
  ([txn status] (finish! txn status nil))
  ([txn status restored]
   (let [state @(:state txn)
         failed? (boolean (seq (remove #(= :verified (:status %)) restored)))
         status (if (and (= :rolled-back status) failed?) :restore-failed status)
         retain? (or (contains? #{:committed :restore-failed} status)
                     (:retain-dir? state))]
     (when-let [^FileOutputStream stream (:manifest-stream state)]
       (try (.close stream) (catch Exception _ nil)))
     (append-journal! txn (name status))
     (write-state! txn status {:finished-at (str (java.time.Instant/now))
                               :retained retain?
                               :restore-failed failed?})
     (when-let [^FileOutputStream stream (:journal-stream state)]
       (try (.close stream) (catch Exception _ nil)))
     (release-lock! (:transactions-dir txn))
     (if retain?
       (do (when (= :committed status) (reclaim-staging! txn))
           (write-lease! txn status))
       (delete-tree! (:dir txn)))
     {:status status :retained retain?})))

(defn rollback!
  ;; @spec MCP-OP-MEM-006
  "Restore every path this transaction began writing, verify each, and end it."
  [txn]
  (let [restored (rollback-written! txn)
        ok (every? #(= :verified (:status %)) restored)
        finished (finish! txn :rolled-back restored)]
    {:ok ok
     :rolled-back true
     :status (:status finished)
     :retained (:retained finished)
     :isolation compact-isolation
     :paths restored}))

(defn- publish-one!
  "Prepare, recheck and rename ONE staged path.

   The order is the whole point. Copying the staged bytes into the target's own
   directory happens FIRST, outside the publish lock and outside the window,
   because it is the only expensive step. The digest recheck, the `write-begin`
   fsync and the rename then happen together under the publish lock, so a
   cooperating writer cannot land between them and a non-cooperating one has
   only two stats, one fsync and one rename to hit.

   Returns {:ok true :window-ns n}, {:conflict-refusal m}, or {:failed cause}."
  [txn path {:keys [h0 identity0 staging prepare-fn publish-fn before-recheck
                    in-commit-window]}]
  (let [prepared (try {:tmp (prepare-fn path staging)}
                      (catch Exception cause {:failed cause}))]
    (if (:failed prepared)
      prepared
      (let [^File tmp (:tmp prepared)]
        (try
          (when before-recheck (before-recheck path))
          (with-publish-lock* txn
            (fn []
              (let [opened (System/nanoTime)
                    current (sha256-file path)
                    identity-now (path-identity path)]
                (cond
                  (not= current h0)
                  {:conflict-refusal (assoc (conflict path h0 current)
                                            :conflict :digest)}

                  (not= identity-now identity0)
                  {:conflict-refusal (identity-conflict path identity0 identity-now)}

                  :else
                  (do
                    (when in-commit-window (in-commit-window path))
                    (append-journal! txn (str "write-begin\t" path "\t" h0))
                    (try
                      (publish-fn path tmp)
                      {:ok true :window-ns (- (System/nanoTime) opened)}
                      (catch Exception cause {:failed cause})))))))
          (finally
            (when (.exists tmp) (.delete tmp))))))))

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
  ([txn {:keys [prepare-fn publish-fn before-publish before-recheck
                in-commit-window after-publish]
         :or {prepare-fn prepare-publish!
              publish-fn publish-prepared!}}]
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
             (loop [remaining paths written 0 window-ns 0]
               (if (empty? remaining)
                 (let [finished (finish! txn :committed)]
                     {:ok true
                      :committed true
                      :retained (:retained finished)
                      :txid (:txid txn)
                      :files-written written
                      :read-set-files (:read-set-count @state)
                      :commit-window (assoc commit-window :max-ns window-ns)
                      :reserved {:journal-bytes (:journal-bytes @state)
                                 :journal-bytes-peak (:journal-bytes-peak @state 0)
                                 :journal-bytes-max (get-in txn [:limits :max-journal-bytes])
                                 :staged-files (count staged)
                                 :staged-files-max (get-in txn [:limits :max-staged-files])}
                      :isolation compact-isolation})
                 (let [path (first remaining)
                       {:keys [result-hash staging]} (get staged path)
                       h0 (get-in pinned [path :sha256])
                       _ (when before-publish (before-publish path))
                       outcome (publish-one! txn path
                                             {:h0 h0
                                              :identity0 (get-in pinned [path :identity])
                                              :staging staging
                                              :prepare-fn prepare-fn
                                              :publish-fn publish-fn
                                              :before-recheck before-recheck
                                              :in-commit-window in-commit-window})]
                   (cond
                     (:conflict-refusal outcome)
                     (let [restored (rollback-written! txn)]
                       (finish! txn :rolled-back restored)
                       (merge (:conflict-refusal outcome)
                              {:files-written written
                               :rolled-back (every? #(= :verified (:status %)) restored)
                               :recovery restored}))

                     (:failed outcome)
                     (let [^Exception cause (:failed outcome)
                           restored (rollback-written! txn)]
                       (finish! txn :rolled-back restored)
                       (refusal :txn-write-failed
                                (str "Writing " path " failed: " (.getMessage cause))
                                {:path path
                                 :files-written written
                                 :cause-error-type (:error-type (ex-data cause))
                                 :rolled-back (every? #(= :verified (:status %)) restored)
                                 :recovery restored
                                 :next_call nil}))

                     :else
                     (do
                       (swap! state update :written conj path)
                       (append-journal! txn (str "write-done\t" path "\t" result-hash))
                       (when after-publish (after-publish path))
                       (let [actual (sha256-file path)]
                         (if (= actual result-hash)
                           (recur (rest remaining) (inc written)
                                  (max window-ns (:window-ns outcome 0)))
                           (let [restored (rollback-written! txn)]
                             (finish! txn :rolled-back restored)
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
                                       :remedy "Another writer that does not hold the project lock raced this rename; the contract detects that rather than preventing it."}))))))))))))))))

;; ---------------------------------------------------------------- recovery

(defn- journal-lines
  [dir]
  (let [file (io/file dir "journal.log")]
    (if (.isFile file)
      (with-open [reader (io/reader file)]
        (vec (line-seq reader)))
      [])))

(defn- restore-from-journal!
  "Restore every path a journal recorded as BEGUN, from its own pinned objects.

   The single restoration primitive: crash recovery and a deliberate `undo!` of
   a committed receipt are the same act read from the same lines. Only
   `write-begin` paths are touched - a pinned path the transaction never wrote
   is not this journal's business, and republishing it would clobber a write
   somebody else made."
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
    ;; remove any staging temporary a dead process left inside the tree
    (doseq [line lines]
      (let [[op path] (str/split line #"\t")]
        (when (= "write-begin" op)
          (doseq [^File sibling (.listFiles (.getParentFile (io/file path)))]
            (when (str/starts-with? (.getName sibling) ".clj-surgeon-publish-")
              (Files/deleteIfExists (.toPath sibling)))))))
    {:txid (.getName (io/file dir))
     :paths restored
     :ok (every? #(= :verified (:status %)) restored)}))

;; ------------------------------------------------------- retained journals

(defn transactions-dir
  "The directory this workspace's transaction journals live in."
  ([workspace-root] (transactions-dir workspace-root nil))
  ([workspace-root state-home]
   (workspace/transactions-dir workspace-root state-home)))

(defn- read-edn-file
  [file]
  (let [f (io/file file)]
    (when (.isFile f)
      (try (read-string (slurp f)) (catch Exception _ nil)))))

(defn- journal-dir
  [workspace-root txid state-home]
  (io/file (transactions-dir workspace-root state-home) txid))

(defn retained-transactions
  ;; @spec MCP-OP-MEM-006
  "Every journal still on disk for this workspace, with its lease.

   What a quota sweep reads. `:evictable false` means the row is a
   `:restore-failed` journal: the tree is not at H0 and this is the only
   material that can repair it."
  ([workspace-root] (retained-transactions workspace-root {}))
  ([workspace-root {:keys [state-home]}]
   (let [dir (io/file (transactions-dir workspace-root state-home))]
     (vec (for [^File d (sort-by #(.getName ^File %)
                                 (seq (or (.listFiles dir) (make-array File 0))))
                :when (.isDirectory d)
                :let [lease (read-edn-file (io/file d "lease.edn"))
                      state (read-edn-file (io/file d "state.edn"))]]
            {:txid (.getName d)
             :status (or (:status lease) (:status state))
             :receipt-refs (:receipt-refs lease 0)
             :evictable (:evictable lease true)
             :bytes (dir-bytes d)})))))

(defn undo!
  ;; @spec MCP-OP-MEM-006
  "Restore every path a RETAINED journal wrote, back to its pinned H0 bytes.

   This is what makes a commit receipt a receipt: the transaction directory
   outlives the commit precisely so its answer can be reversed and the
   reversal verified against the digest pinned before the first write."
  ([workspace-root txid] (undo! workspace-root txid {}))
  ([workspace-root txid {:keys [state-home]}]
   (let [dir (journal-dir workspace-root txid state-home)]
     (if-not (.isDirectory dir)
       (refusal :txn-journal-missing
                (str "No retained journal for " txid
                     "; its pre-images cannot be republished")
                {:txid txid :next_call nil
                 :remedy "A journal that was forgotten or evicted cannot be undone. Retain it until the receipt no longer needs to be reversible."})
       (let [result (restore-from-journal! (.getCanonicalPath dir))]
         (assoc result :isolation compact-isolation :undone true))))))

(defn- discard-journal!
  [workspace-root txid state-home quota-driven?]
  (let [dir (journal-dir workspace-root txid state-home)
        lease (read-edn-file (io/file dir "lease.edn"))
        state (read-edn-file (io/file dir "state.edn"))
        status (or (:status lease) (:status state))]
    (cond
      (not (.isDirectory dir))
      (refusal :txn-journal-missing (str "No retained journal for " txid)
               {:txid txid :next_call nil})

      (= :restore-failed status)
      (refusal :txn-journal-retained
               (str "The journal of " txid " records a restoration that did not verify"
                    " and is the only material that can repair the tree")
               {:txid txid
                :status status
                :next_call nil
                :remedy "Repair the tree from this journal - or accept the divergence deliberately - before removing it. A failed restoration is never evicted by a quota."})

      (and quota-driven? (pos? (:receipt-refs lease 0)))
      (refusal :txn-journal-referenced
               (str "The journal of " txid " is still referenced by "
                    (:receipt-refs lease 0) " receipt(s)")
               {:txid txid
                :receipt-refs (:receipt-refs lease 0)
                :next_call {:op :txn/release-receipt :txid txid}
                :remedy "Release the receipt's reference first, or forget! the journal explicitly."})

      :else
      (do (delete-tree! dir)
          {:ok true :txid txid :forgotten true}))))

(defn forget!
  ;; @spec MCP-OP-MEM-006
  "Discard a retained journal deliberately. Refuses a failed restoration."
  ([workspace-root txid] (forget! workspace-root txid {}))
  ([workspace-root txid {:keys [state-home]}]
   (discard-journal! workspace-root txid state-home false)))

(defn evict!
  ;; @spec MCP-OP-MEM-006
  "Reclaim a retained journal under quota pressure.

   Stricter than `forget!` on purpose: a sweep that runs because disk is short
   must not silently destroy a receipt somebody is still holding, and must
   never destroy unrepaired recovery material."
  ([workspace-root txid] (evict! workspace-root txid {}))
  ([workspace-root txid {:keys [state-home]}]
   (discard-journal! workspace-root txid state-home true)))

(defn release-receipt!
  ;; @spec MCP-OP-MEM-006
  "Drop the receipt reference that keeps a committed journal out of a sweep."
  ([workspace-root txid] (release-receipt! workspace-root txid {}))
  ([workspace-root txid {:keys [state-home]}]
   (let [file (io/file (journal-dir workspace-root txid state-home) "lease.edn")
         lease (read-edn-file file)]
     (if-not lease
       (refusal :txn-journal-missing (str "No retained journal for " txid)
                {:txid txid :next_call nil})
       (do (spit file (pr-str (assoc lease :receipt-refs 0
                                     :released-at (str (java.time.Instant/now)))))
           {:ok true :txid txid :receipt-refs 0})))))

(defn recover!
  ;; @spec MCP-OP-MEM-013
  "Roll back every unfinished transaction found for this workspace.

   Recovery reads only the journal: the pin lines say which pre-image bytes are
   durable, and the write-begin lines say which paths were begun. Each restored
   path is verified against the digest that was pinned before the first write.

   A recovery whose restoration VERIFIED discards its journal, because the tree
   is back at H0 and there is nothing left to recover from. A recovery that did
   NOT verify keeps everything and records `:restore-failed`: deleting the only
   material that can repair the tree, at the moment repair is needed, is how a
   partial failure becomes a permanent one."
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
                         (let [result (restore-from-journal! (.getCanonicalPath d))
                               status (if (:ok result) :rolled-back :restore-failed)]
                           (spit (io/file d "state.edn")
                                 (pr-str {:txid (.getName d) :status status
                                          :retained (not (:ok result))
                                          :restore-failed (not (:ok result))
                                          :recovered-at (str (java.time.Instant/now))}))
                           (if (:ok result)
                             (delete-tree! d)
                             (spit (io/file d "lease.edn")
                                   (pr-str {:txid (.getName d)
                                            :status :restore-failed
                                            :receipt-refs 0
                                            :evictable false
                                            :retained-at (str (java.time.Instant/now))
                                            :bytes (dir-bytes d)})))
                           result))
                       candidates)]
     (when (seq results)
       (release-lock! transactions))
     {:ok (every? :ok results)
      :transactions-recovered (count results)
      :isolation compact-isolation
      :paths (vec (mapcat :paths results))})))
