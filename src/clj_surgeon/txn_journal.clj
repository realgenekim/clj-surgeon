(ns clj-surgeon.txn-journal
  "A disk-journaled transaction: hashes and spans resident, bytes on disk.

   The contract is OPTIMISTIC SERIALIZABILITY with conflict detection and exact
   rollback. It is not snapshot isolation. An atomic rename is not a
   compare-and-swap: the pre-image recheck and the rename are two syscalls, and
   a writer that does not take the publish lock can land between them. That
   RESIDUAL WINDOW is bounded to one NOFOLLOW stat comparison, one journal
   fsync and one rename - the staged bytes are copied into the target's own
   directory and the pre-image digest is taken BEFORE the window opens, so
   nothing inside the window reads or copies the target and its size term is
   reduced about four-fold rather than to zero - it is measured into every
   receipt,
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
   (java.nio.file Files LinkOption StandardCopyOption CopyOption)
   (java.nio.file.attribute BasicFileAttributes FileTime PosixFilePermissions)
   (java.security MessageDigest)))

(set! *warn-on-reflection* true)

;; ------------------------------------------------------------ the contract

(def commit-window
  "What is left between a path's pre-image recheck and its replacement.

   This is the honest statement of what optimistic serializability costs. The
   window cannot be closed on a POSIX filesystem - rename replaces, it does not
   compare-and-swap - so the kernel narrows it instead and names its bound.

   THE SIZE TERM IS REDUCED, NOT ELIMINATED, and stating it as `O(1)` was the
   defect MEM-014's own rule forbids - a statement about an instrument must be
   true of that instrument in general, not only of the case that motivated it.
   `:staging-copy-inside false` was literally true - no byte COPYING happened
   inside - while the pre-image digest recheck inside the lock re-read the
   whole target, so the window grew with the file: 846 us at 1 KB, 3.0 ms at
   2 MiB, measured. The digest is now taken BEFORE the lock with a stat on
   each side of it, and inside the lock only that stat is compared: NOFOLLOW
   type, (device, inode), size, modification time and change time. A target
   whose whole stat is unchanged still holds the bytes the digest described.
   If the stat moved, the digest is re-read inside the lock and the receipt
   says so in `:digest-rereads`, so the fast path can never be mistaken for a
   skipped check. What remains inside is one stat, one fsync and one rename,
   and NONE of the three reads the target - but the fsync's cost still tracks
   the writeback the pre-lock staging copy left behind, so the width is not
   flat in the target's size. Measured on ext4, 2026-09-03, medians of the
   receipt's own `:max-ns` over nine commits after a five-commit warmup:
   633,980 ns at 1 KB and 1,204,517 ns at 2 MiB - 1.9x - against 672,439 ns
   and 2,939,460 ns - 4.4x - before the digest moved out of the lock. The size
   term fell about four-fold; it did not reach zero, and `:size-term` says so
   in every receipt.

   The residual is unchanged in KIND and now smaller in width: a modification
   that leaves type, identity, size and both nanosecond timestamps identical
   is indistinguishable from no modification, and that is the same class of
   thing the contract already refuses to claim it closes."
  {:ops [:recheck-stat :recheck-identity :journal-write-begin :rename]
   :staging-copy-inside false
   :digest-computed-before-lock true
   :stat-fields [:kind :file-key :size :mtime-ns :ctime-ns]
   :lock :workspace-publish-lock
   :size-term (str "reduced ~4x, not eliminated: measured on ext4 2026-09-03 the median window was "
                   "633980 ns at 1 KB and 1204517 ns at 2 MiB (1.9x), against 672439 ns and 2939460 ns "
                   "(4.4x) before the digest moved out of the lock; nothing inside the window reads or "
                   "copies the target, and what still scales is writeback from the pre-lock staging copy "
                   "paid by the in-window fsync")
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
                "The recheck and the rename are taken under the workspace publish lock, and the recheck inside it is one stat rather than a re-read."
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
   server hard maximum, which is what `hard-limits` names.

   `max-journal-bytes` is DERIVED, not chosen. The journal holds one PRE-image
   and one FUTURE image of every byte a transaction stages, so the rule is

       max-journal-bytes >= 2 x scope-stream's max-aggregate-bytes

   and at the reader's 512 MiB default that is 1 GiB. The previous 512 MiB
   default broke the rule and was the reason the red-to-green memory arm had to
   override it: a scope the READ path admitted was one the journal refused to
   stage, which is a ceiling set that had never been derived rather than a
   deliberate policy. `the-default-journal-quota-admits-what-the-default-read-path-admits`
   is the witness.

   The HARD maxima are independent server caps and are not derived from each
   other: a request that raises the aggregate read ceiling above 2 GiB must
   raise `max-journal-bytes` explicitly, and is refused by a named ceiling with
   a `next_call` if it does not."
  {:max-read-set-files 20000
   :max-staged-files 2000
   :max-journal-bytes (* 1024 1024 1024)})

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
    (apply str (map #(format "%02x" (bit-and 0xff (long %))) digest))))

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
    (apply str (map #(format "%02x" (bit-and 0xff (long %))) (.digest digest)))))

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
   filesystem that has one.

   What this catches, and a content digest cannot: a TYPE change - a regular
   file replaced by a symbolic link to byte-identical content. What it does NOT
   catch, measured on ext4 2026-09-03: a file's own recreation. `fileKey`
   carries no generation counter, so deleting a pinned regular file and
   recreating it with identical bytes can be handed the SAME (device, inode),
   and the commit succeeds with no conflict. The instrument distinguishes types
   and the inode changes the OS happens to expose; it does not distinguish two
   files that hold the same bytes in general, and the earlier form of this
   docstring said it did."
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

(defn- path-stat
  "The NOFOLLOW type, filesystem identity, size and timestamps of `path`.

   What makes the pre-image recheck O(1). A target whose type, (device, inode),
   size, modification time and change time are all what they were when the
   digest was taken OUTSIDE the lock still holds the bytes that digest
   described, so the expensive read need not happen inside. When any field
   moved, the caller re-reads inside the lock and records that it did."
  [path]
  (try
    (let [p (.toPath (io/file path))
          ^"[Ljava.nio.file.LinkOption;" nofollow (into-array LinkOption [LinkOption/NOFOLLOW_LINKS])
          attrs (Files/readAttributes p BasicFileAttributes nofollow)]
      {:kind (cond
               (.isSymbolicLink attrs) :symlink
               (.isRegularFile attrs) :regular
               (.isDirectory attrs) :directory
               :else :other)
       :file-key (str (.fileKey attrs))
       :size (.size attrs)
       :mtime-ns (.to (.lastModifiedTime attrs) java.util.concurrent.TimeUnit/NANOSECONDS)
       :ctime-ns (try
                   (let [^java.nio.file.attribute.FileTime changed
                         (Files/getAttribute p "unix:ctime" nofollow)]
                     (.to changed java.util.concurrent.TimeUnit/NANOSECONDS))
                   (catch Exception _ nil))})
    (catch Exception _ {:kind :absent :file-key nil :size nil
                        :mtime-ns nil :ctime-ns nil})))

(defn- identity-token
  "A path's NOFOLLOW identity rendered as one tab-free journal token."
  [identity]
  (str (name (:kind identity :absent)) "|" (:file-key identity)))

(defn- hex
  [^bytes bs]
  (apply str (map #(format "%02x" (bit-and 0xff (long %))) bs)))

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
  ^File [transactions-dir]
  (io/file transactions-dir "LOCK"))

(def ^:private lock-format
  "The LOCK payload's format version. 1 was pid-only and is unverifiable; 2
   records the checkable triple of pid, start ticks and boot id."
  2)

(defn- boot-id
  "This kernel boot's identity, or nil where the OS does not publish one.

   A pid is unique only WITHIN one boot. After a reboot the same number names a
   different process, so a LOCK a crash left behind would read as held by a
   live holder for as long as that number is in use again."
  []
  (let [file (io/file "/proc/sys/kernel/random/boot_id")]
    (when (.isFile file)
      (try (str/trim (slurp file)) (catch Exception _ nil)))))

(defn- process-handle
  "The `ProcessHandle` for `pid`, or nil when no such process exists."
  ^java.lang.ProcessHandle [pid]
  (try
    (let [^java.util.Optional found (java.lang.ProcessHandle/of (long pid))]
      (when (.isPresent found) (.get found)))
    (catch Exception _ nil)))

(defn- process-start-ticks
  "Epoch milliseconds at which `pid` started, or nil when unknown."
  [pid]
  (try
    (when-let [^java.lang.ProcessHandle handle (process-handle pid)]
      (let [^java.util.Optional started (.startInstant (.info handle))]
        (when (.isPresent started)
          (.toEpochMilli ^java.time.Instant (.get started)))))
    (catch Exception _ nil)))

(defn- holder-identity
  "The triple that makes a recorded holder CHECKABLE from another process.

   A pid alone is not an identity: it is reused within a boot and repeated
   across boots. Pid plus the holder's own start ticks plus the boot id names
   one process and no other."
  []
  (let [pid (.pid (java.lang.ProcessHandle/current))]
    {:pid pid
     :start-ticks (process-start-ticks pid)
     :boot-id (boot-id)}))

(defn- stale-holder
  "Why the LOCK's recorded holder cannot be a live process, or nil if it can.

   Fail closed on every ambiguity: a holder that cannot be DISPROVED is
   treated as live. An unreadable holder is not proof of death - it is named
   `:no-recorded-holder`, `begin!` refuses it, and only the explicit `recover!`
   remedy acts on it."
  [holder]
  (let [pid (:pid holder)
        recorded-boot (:boot-id holder)
        current-boot (boot-id)
        ^java.lang.ProcessHandle handle (when pid (process-handle pid))]
    (cond
      (nil? pid) :no-recorded-holder
      ;; A claim from a build that recorded a pid and nothing else. Both
      ;; mismatch clauses below are dead for it, so only :process-not-alive
      ;; could ever fire - and a REUSED pid then reads as a live holder for as
      ;; long as the number is in use, which is a lock nobody can ever break.
      ;; The format is an UNKNOWN, so it fails closed with its own name rather
      ;; than being silently treated as either live or dead.
      (and current-boot (nil? recorded-boot)) :legacy-format
      (and recorded-boot current-boot (not= recorded-boot current-boot)) :boot-id-mismatch
      (nil? handle) :process-not-alive
      (not (.isAlive handle)) :process-not-alive
      (and (:start-ticks holder)
           (not= (:start-ticks holder) (process-start-ticks pid))) :start-ticks-mismatch
      :else nil)))

(def legacy-lock-break-age-ms
  "How old a LEGACY-format LOCK must be before `recover!` will break it.

   A legacy claim carries a pid and nothing else, so nothing about it can be
   checked: a reused pid is indistinguishable from the original holder. The
   explicit `:break-legacy-lock` remedy therefore demands a two-part receipt
   rather than a judgement - the recorded pid must name no live process AND
   the claim must be at least this old - and refuses when either half is
   missing. One hour is long enough that no transaction this kernel opens is
   still running behind it, and short enough to be a remedy.

   WHICH HALF IS CHECKABLE. The pid half is: it reads the process table now.
   The age half is a HEURISTIC over file timestamps, and it is the weaker of
   the two - a new-format LOCK carries `:acquired-at` in the claim itself, and
   a legacy one carries nothing, which is exactly why the timestamps are used
   at all."
  3600000)

(defn- lock-age-basis-ms
  "The newest of a lock file's modification and change times, or nil.

   mtime alone is not evidence of age. Any process may set it, and `cp -p`,
   `rsync -t`, `tar -x` and a restore from backup all PRESERVE it while giving
   the file a fresh ctime - so a workspace restored from a snapshot presents a
   legacy LOCK that is old by mtime and was created moments ago in this boot.
   ctime cannot be set to an arbitrary value through the filesystem API at
   all; it only moves forward, on every change to the file. Taking the NEWEST
   of the two therefore means a claim reads as old only when BOTH stamps are,
   which is the direction that fails closed.

   nil when the file is not there: `lastModified` returns 0 for an absent
   file, which would otherwise read as infinitely old."
  [^File lock]
  (when (.isFile lock)
    (let [mtime (.lastModified lock)
          ctime (try
                  (let [^java.nio.file.attribute.FileTime changed
                        (Files/getAttribute (.toPath lock) "unix:ctime"
                                            ^"[Ljava.nio.file.LinkOption;"
                                            (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))]
                    (.toMillis changed))
                  (catch Exception _ nil))]
      (max (long mtime) (long (or ctime mtime))))))

(defn- legacy-lock-dead?
  "The receipt a legacy claim's break requires, and nothing weaker.

   Two halves, both necessary: the recorded pid names no live process, and the
   claim is at least `legacy-lock-break-age-ms` old, measured against
   `lock-age-basis-ms` - the NEWEST of mtime and ctime, because mtime alone is
   settable and survives a copy. Neither half alone is a receipt: a pid absent
   right now can be a process that has not started yet on a recycled number,
   and an old lock can belong to a long-running holder. A lock file that is
   not there is not old, it is absent."
  [^File lock holder now-ms]
  (let [pid (:pid holder)
        ^java.lang.ProcessHandle handle (when pid (process-handle pid))
        dead? (or (nil? handle) (not (.isAlive handle)))
        basis (lock-age-basis-ms lock)
        age (when basis (- (long (or now-ms (System/currentTimeMillis))) (long basis)))]
    (boolean (and dead? age (>= (long age) (long legacy-lock-break-age-ms))))))

(def ^:private breakable-causes
  "The causes that PROVE the recorded holder is gone.

   `:no-recorded-holder` and `:legacy-format` are deliberately absent: an
   unparsable LOCK and a claim written before the checkable triple existed are
   both unknowns, and `begin!` must not break a lock on an unknown."
  #{:boot-id-mismatch :process-not-alive :start-ticks-mismatch})

(defn- read-holder
  [^File lock]
  (try (read-string (slurp lock)) (catch Exception _ {})))

(defn- lock-broken-line
  "The typed receipt line a broken lock leaves behind."
  [holder cause]
  {:reason :stale-holder
   :cause cause
   :pid (:pid holder)
   :holder-txid (:txid holder)
   :broken-at (str (java.time.Instant/now))})

(defn- lock-file-key
  "The LOCK's filesystem identity, or nil when it is not there."
  [^File lock]
  (try
    (let [attrs (Files/readAttributes
                  (.toPath lock)
                  BasicFileAttributes
                  ^"[Ljava.nio.file.LinkOption;"
                  (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))]
      (str (.fileKey attrs)))
    (catch Exception _ nil)))

(defn- read-lock-claim
  "The LOCK's holder together with the IDENTITY of the claim that was read.

   A holder value alone cannot be broken safely: between reading it and acting
   on it the file can be replaced by a live holder's own claim, and then a
   break removes something the breaker never judged. The content and the
   (device, inode) pair are what make the judgement re-checkable at the moment
   it is acted on."
  [^File lock]
  (let [content (try (slurp lock) (catch Exception _ nil))]
    {:holder (if content
               (try (read-string content) (catch Exception _ {}))
               {})
     :content content
     :content-sha256 (when content (sha256-string content))
     :file-key (lock-file-key lock)}))

(defn- release-lock!
  "Unlink the LOCK, but only when it still names this transaction.

   A bare `deleteIfExists` deletes whoever's claim it finds. That is how one
   transaction's ordinary `finish!` removes a DIFFERENT live transaction's
   lock, which turns a lost race into a shared lock nobody notices. There is
   deliberately no unguarded arity: an unconditional delete left in reach is a
   defect waiting for its next caller."
  [transactions-dir txid]
  (let [^File lock (lock-file transactions-dir)]
    (if (= txid (:txid (read-holder lock)))
      (Files/deleteIfExists (.toPath lock))
      false)))

(def ^:private displaced-claims
  "How many claims a break has renamed away and then FAILED to put back.

   A refusal nobody counts is indistinguishable from silent loss. The restore
   refuses whenever a third acquirer landed in the gap, and the claim that was
   renamed away then survives only inside the tombstone: its owner's
   `release-lock!` finds a LOCK that does not name it, returns false, and
   learns nothing. This is the bucket that makes that visible, and both
   callers report it beside the outcome that produced it."
  (atom 0))

(defn displaced-claim-count
  "How many claims this process has displaced without restoring them.

   Non-zero is an alarm, not an archive: each one is a holder that lost the
   project lock without being told."
  []
  @displaced-claims)

(def broken-lock-retention-ms
  "How long a `LOCK.broken.*` tombstone is kept before recovery retires it.

   The tombstone is the break's evidence: the exact claim that was removed,
   beside the journal line that says a lock was broken. Evidence nothing
   sweeps is not durability, it is accumulation - one 30,000-break storm left
   20,356 of them permanent in a single transactions directory, invisible to
   the quota, to recovery and to every listing. A day is long enough that
   whoever is investigating a break still finds it, and short enough that the
   directory cannot grow without bound."
  86400000)

(def broken-lock-stamp-tolerance-ms
  "How far into the future a tombstone's own stamp may read before it is
   UNREADABLE rather than a time.

   The stamp lives in a `LOCK.broken-at.*` sidecar in the transactions
   directory, and retention is measured against the NEWEST of it and the
   file's own mtime/ctime basis - the direction that keeps evidence rather
   than retires it early. That direction is fail-safe against a stamp in the
   PAST and fail-open against one in the FUTURE: any writer of the directory,
   or one forward step of a clock that later steps back, makes a file
   permanent and publishes an age no clock produced. Measured: a stamp ten
   years ahead gave `:age-ms -315360000000`, `:pruned 0`, and a tombstone
   nothing would ever retire.

   A stamp this far ahead or further is therefore not a time: the file falls
   back to its own mtime/ctime basis and the row says `:stamp :unreadable`.
   The tolerance is a clock-skew allowance, not a grace period - a minute is
   wider than any skew between two writers of one directory on one host, and
   far narrower than the retention it would otherwise defeat."
  60000)

(def ^:private tombstone-prefix
  "The prefix every break's evidence file carries."
  "LOCK.broken.")

(defn- broken-lock-files
  "Every `LOCK.broken.*` tombstone in a transactions directory."
  [transactions-dir]
  (let [dir (io/file transactions-dir)]
    (vec (sort-by #(.getName ^File %)
                  (filter (fn [^File f]
                            (and (.isFile f)
                                 (str/starts-with? (.getName f) tombstone-prefix)))
                          (seq (or (.listFiles dir) (make-array File 0))))))))

(def ^:private broken-at-prefix
  "The prefix of a tombstone's own-creation-time sidecar.

   Deliberately NOT `LOCK.broken.` - `broken-lock-files` selects on that
   prefix, and a sidecar that answered it would be listed, billed and retired
   as a tombstone in its own right. `LOCK.broken-at.` shares no prefix with
   it: the character after `LOCK.broken` is `-`, not `.`."
  "LOCK.broken-at.")

(defn- broken-at-file
  "The sidecar that records when a tombstone was MADE."
  ^File [^File tomb]
  (io/file (.getParentFile tomb)
           (str broken-at-prefix
                (subs (.getName tomb) (count tombstone-prefix)))))

(defn- broken-at-files
  "Every `LOCK.broken-at.*` stamp sidecar in a transactions directory."
  [transactions-dir]
  (let [dir (io/file transactions-dir)]
    (vec (sort-by #(.getName ^File %)
                  (filter (fn [^File f]
                            (and (.isFile f)
                                 (str/starts-with? (.getName f) broken-at-prefix)))
                          (seq (or (.listFiles dir) (make-array File 0))))))))

(defn- orphan-sidecar-files
  "Every stamp sidecar whose tombstone is no longer there.

   The prune deletes a sidecar and then its tombstone, and the interrupted
   revert does the same, so an ORPHAN is what anything else leaves: a hand
   cleanup, a partial copy, a failed delete of the tombstone half. Listed by
   nothing and retired by nothing, it is a small unbounded accumulation of
   exactly the kind `broken-lock-retention-ms` exists to prevent - in the
   directory that bound was written for."
  [transactions-dir]
  (let [dir (io/file transactions-dir)]
    (filterv (fn [^File side]
               (not (.isFile
                      (io/file dir (str tombstone-prefix
                                        (subs (.getName side)
                                              (count broken-at-prefix)))))))
             (broken-at-files transactions-dir))))

(defn- read-broken-at-ms
  "What a stamp sidecar records: the creation time, nil, or `:unreadable`.

   `nil` is 'this file records no break time', which a link-time `:phase`
   marker legitimately does not - a break that BEGAN is not a break that
   happened, and its marker is not a stamp. `:unreadable` is 'there is a
   sidecar here and it does not carry a time', which is a different fact and
   must not read as an absent one."
  [^File side]
  (when (.isFile side)
    (try
      (let [m (read-string (slurp side))
            v (:broken-at-ms m)]
        (cond
          (number? v) (long v)
          (:phase m) nil
          :else :unreadable))
      (catch Exception _ :unreadable))))

(defn- delete-quietly!
  "Unlink a path if it is there, and never throw for it.

   Used only where the path is one this code created and the caller is
   already on a failure route: a cleanup that throws turns a typed refusal
   into an untyped one and loses the cause the caller needs."
  [^File f]
  (try (Files/deleteIfExists (.toPath f)) (catch Exception _ false)))

(defn- sidecar-temp!
  "A temporary beside a sidecar, carrying `payload`, or nil.

   The name starts with a dot so neither `broken-lock-files` nor
   `broken-at-files` can ever select it: a working file that answered one of
   those prefixes would be listed, counted and retired as evidence in its own
   right."
  ^File [^File side payload]
  (try
    (let [^File tmp (File/createTempFile ".LOCK-brk-" ".tmp" (.getParentFile side))]
      (try (spit tmp (pr-str payload)) tmp
           (catch Exception cause (delete-quietly! tmp) (throw cause))))
    (catch Exception _ nil)))

(defn- create-sidecar!
  "Write a sidecar ONLY if that name is free, and say which happened:
   `:created`, `:name-taken`, `:unsupported` (no `link(2)` on this filesystem,
   which is the caller's fallback rather than a refusal), or `:failed`.

   `Files/createLink` from a fully written temporary, for the same two reasons
   the LOCK itself is written that way: it is create-if-absent IN THE KERNEL,
   so claiming the name is the kernel's own refusal rather than a stat this
   code performs, and no reader can ever observe a half-written sidecar."
  [^File side payload]
  (if-let [^File tmp (sidecar-temp! side payload)]
    (try
      (Files/createLink (.toPath side) (.toPath tmp))
      :created
      (catch java.nio.file.FileAlreadyExistsException _ :name-taken)
      ;; a filesystem with no `link(2)` at all: the caller's fallback, NOT a
      ;; refusal. Reporting this as an unrecordable break would make the
      ;; break-by-move path unreachable on exactly the filesystems it exists
      ;; for.
      (catch UnsupportedOperationException _ :unsupported)
      (catch Exception _ :failed)
      (finally (delete-quietly! tmp)))
    :failed))

(defn- replace-sidecar!
  "Overwrite a sidecar in one step, or return nil having changed nothing.

   `spit` TRUNCATES before it writes, so a process killed mid-write left a
   zero-length sidecar that `break-phase` read as no marker at all and
   `read-broken-at-ms` read as `:unreadable` - a partially written file
   presenting as a state the kernel has a rule for. An atomic rename over the
   name means the sidecar is always exactly one whole map or the other."
  [^File side payload]
  (when-let [^File tmp (sidecar-temp! side payload)]
    (try
      (Files/move (.toPath tmp) (.toPath side)
                  ^"[Ljava.nio.file.CopyOption;"
                  (into-array CopyOption [StandardCopyOption/ATOMIC_MOVE
                                          StandardCopyOption/REPLACE_EXISTING]))
      true
      (catch Exception _ (delete-quietly! tmp) nil))))

(defn- stamp-broken-at!
  "Write the break's OWN creation time into the tombstone's sidecar, and
   return it - or NIL when that write did not happen.

   Separate from the mtime half because it is safe to call while the tombstone
   is still a second link to the LOCK: it writes a DIFFERENT file, and touches
   no shared inode.

   IT RETURNS NIL ON A FAILED WRITE, and that return value is load-bearing.
   It used to swallow the exception and return the clock regardless, so
   `break-by-link!` unlinked the LOCK on a success this function had not
   achieved: the claim was destroyed and nothing on disk recorded that a break
   had taken it. Worse, the `:phase :linked` marker this write is what
   overwrites, so the completed break wore the marker of an interrupted one
   and the next recovery reverted it - the path by which a real break's
   evidence was deleted. A break the kernel could not record is a break that
   did not happen."
  [^File tomb]
  (let [now (System/currentTimeMillis)]
    (when (replace-sidecar! (broken-at-file tomb)
                            {:tombstone (.getName tomb)
                             :broken-at (str (java.time.Instant/ofEpochMilli now))
                             :broken-at-ms now})
      now)))

(defn- touch-tombstone!
  "Give the tombstone's own inode the break's creation time, and return it.

   Only once the break OWNS the file: setting mtime through one link sets it
   on the inode BOTH names, so doing this while the tombstone is still a
   second link would re-date the LOCK of a holder whose break has not even
   completed."
  [^File tomb now]
  (try
    (Files/setLastModifiedTime (.toPath tomb) (FileTime/fromMillis (long now)))
    (catch Exception _ nil))
  now)

(defn- stamp-tombstone!
  "Give the break's evidence its OWN creation time, twice over.

   A tombstone is made by moving or linking the LOCK, and BOTH preserve the
   claim's mtime - so the evidence inherited the age of the claim it is
   evidence OF, and a lock older than `broken-lock-retention-ms` produced a
   tombstone that was already past retention the instant it existed. The same
   `recover!` then deleted it and returned a receipt naming a file that was no
   longer there. Measured: `{:found 1 :pruned 1 :remaining 0}` beside
   `:tombstone \"LOCK.broken.recover-...\"`, deterministic, through the public
   verb.

   The stamp is the file's own mtime AND a sidecar carrying `:broken-at-ms`,
   because neither alone is enough: mtime is what a directory listing shows
   and what `lock-age-basis-ms` reads, and the sidecar is what survives a
   `cp -p` or a `tar -x` that puts the inherited mtime back. Retention is
   measured against the NEWEST of them, which is the direction that keeps
   evidence rather than the one that retires it.

   Called only once the tombstone is a file this break OWNS: never while it is
   still a second link to a live LOCK, because setting mtime through one link
   sets it on the inode both names."
  [^File tomb]
  (let [stamped (stamp-broken-at! tomb)]
    ;; the mtime half is set from the clock even when the sidecar write
    ;; failed, so the evidence is still retired on a time this run produced
    ;; rather than on the age it inherited from the claim it broke
    (touch-tombstone! tomb (or stamped (System/currentTimeMillis)))
    stamped))

(defn- mark-break-linked!
  "Record in the tombstone's OWN sidecar that a break has claimed the evidence
   name and has not yet unlinked the LOCK.

   Without this, an interrupted break was recognised only by sharing an inode
   with the live LOCK - state inferred from a NEIGHBOUR, which dies with the
   neighbour. The moment the wrongly-broken holder released its own claim,
   cleanly and txid-scoped, the sweep silently re-typed the tombstone as a
   break that happened, billed it for a day as evidence of one, and refused
   every later break by that txid for a name recording nothing.

   Written BEFORE `Files/createLink`, not after it. After it there was a
   window - two statements wide, and reachable without any crash because
   `spit` truncates before it writes - in which a tombstone existed with NO
   marker: typed by the inode rule alone while the LOCK was there, and
   re-typed as a break that happened the moment the wrongly-broken holder
   released its own claim. Claiming the marker first means that from the
   instant the link exists its marker is already on disk, and what an
   interruption before the link leaves is a marker with no tombstone - an
   ORPHAN SIDECAR, which this branch lists, counts and retires.

   `create-sidecar!` rather than a write, so the name is claimed by the same
   create-if-absent primitive the break uses: a sidecar already there belongs
   to another break's evidence, and overwriting it would turn that break's
   stamp into this one's marker. Returns `:created`, `:name-taken`,
   `:unsupported` or `:failed`, all four of which the caller acts on.

   Cleared by `stamp-broken-at!` BEFORE the unlink, so a crash between the
   unlink and the stamp can never leave a completed break wearing this
   marker - and a stamp write that FAILS now refuses the unlink rather than
   leaving one."
  [^File tomb]
  (create-sidecar! (broken-at-file tomb)
                   {:tombstone (.getName tomb)
                    :phase :linked
                    :linked-at-ms (System/currentTimeMillis)}))

(defn- break-phase
  "What a tombstone's own sidecar says about the break that made it: `:linked`
   while the LOCK has not been unlinked yet, nil once it has."
  [^File tomb]
  (let [^File side (broken-at-file tomb)]
    (when (.isFile side)
      (try (:phase (read-string (slurp side))) (catch Exception _ nil)))))

(defn- evidence-stat
  "The size and the age basis of one piece of break evidence, from ONE stat,
   or nil when the file is not there.

   Two separate reads made a file that VANISHED indistinguishable from a
   present empty one: `lastModified` of a missing file is 0, which reads as
   infinitely old, and `.length` of a missing file is 0 as well - so the row
   that billed `:bytes 0` against an epoch-sized age was the same shape a
   present zero-length file has. The old listing hid that by dropping every
   zero-byte row; once a present empty tombstone is a row of its own (round
   6, finding 3), the two must be told apart at the source.
   `Files/readAttributes` throws `NoSuchFileException` for a file that is
   gone, and binds the size to the SAME observation as the times, so a row
   that says `:bytes 0` means it."
  [^File f]
  (try
    (let [attrs (Files/readAttributes (.toPath f)
                                      "unix:size,lastModifiedTime,ctime"
                                      ^"[Ljava.nio.file.LinkOption;"
                                      (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))
          mtime (.toMillis ^FileTime (get attrs "lastModifiedTime"))
          ctime (some-> ^FileTime (get attrs "ctime") (.toMillis))]
      {:bytes (long (get attrs "size"))
       ;; the newest of mtime and ctime, for the reason `lock-age-basis-ms`
       ;; states: mtime is settable and survives a copy, ctime is not
       :basis (max (long mtime) (long (or ctime mtime)))})
    (catch Exception _ nil)))

(defn- evidence-basis
  "The stamp a piece of break evidence is retired against, and how its own
   sidecar read: `{:basis ms :stamp :ok|:absent|:unreadable}`, or nil when the
   file is not there.

   The basis is the newest of what `lock-age-basis-ms` reads off the file -
   itself the newest of mtime and ctime - and the `:broken-at-ms` the sidecar
   records. Newest, because every way that value can be wrong EXCEPT ONE makes
   evidence look older than it is, and retiring evidence early is the failure
   that matters.

   That one exception is a stamp in the FUTURE, and it is the whole reason
   this returns a `:stamp` as well as a number. `max` is fail-safe against a
   stamp in the past and fail-OPEN against one ahead of the clock: any writer
   of the transactions directory - or a clock that steps forward once and back
   - made a tombstone permanent and published `:age-ms -315360000000`, an age
   no clock produced, while the requirement promises evidence bound by a
   published retention age that recovery enforces. A stamp
   `broken-lock-stamp-tolerance-ms` or more ahead of the clock is therefore
   not a time: the file falls back to its OWN basis and the row says
   `:stamp :unreadable`, which is a typed fact rather than a silent one."
  [^File f ^File side now]
  (when-let [stat (evidence-stat f)]
    (let [basis (long (:basis stat))
          raw (read-broken-at-ms side)]
      (merge
        (select-keys stat [:bytes])
        (cond
          (nil? raw) {:basis basis :stamp :absent}
          (= :unreadable raw) {:basis basis :stamp :unreadable}
          (>= (long raw) (+ (long now) (long broken-lock-stamp-tolerance-ms)))
          {:basis basis :stamp :unreadable}
          :else {:basis (max basis (long raw)) :stamp :ok})))))

(defn- evidence-age
  "How old a piece of break evidence is and how its stamp read, or `:absent`.

   The age is CLAMPED at zero. A negative age is not a measurement - it is a
   clock disagreeing with a stamp - and publishing one puts a number in a
   receipt that no clock produced and no reader can act on.

   `lastModified` of a missing file is 0, which reads as infinitely old and
   bills `:bytes 0`; a file that vanished between a listing and its stat is
   ABSENT, which is neither an age nor a zero."
  [^File f ^File side now]
  (if-let [found (evidence-basis f side now)]
    {:age-ms (max 0 (- (long now) (long (:basis found))))
     :stamp (:stamp found)
     :bytes (long (:bytes found))}
    :absent))

(defn- tombstone-age
  "`evidence-age` for a tombstone, read against its own stamp sidecar."
  [^File tomb now]
  (evidence-age tomb (broken-at-file tomb) now))

(defn- interrupted-break-entries
  "Every tombstone whose break may not have completed, paired with what the
   LOCK can still say about it: `[file :corroborated]` or
   `[file :uncorroborated-marker]`.

   The two rules are not equal evidence and must not be collapsed into one
   boolean. The (device, inode) rule is CORROBORATION BY THE KERNEL: a
   tombstone that shares the present LOCK's inode is a second link to a claim
   that is still there, which is the one state a completed break can never be
   in, because a completed break unlinks the LOCK. The `:phase :linked` marker
   is a CLAIM BY THE BREAKER, written into a file any writer of the
   transactions directory can write - the same threat model the forgeable
   stamp was fixed for.

   So when the marker matches and the LOCK does NOT - it is gone, or it names
   a different inode - nothing can corroborate it, and the two states it
   cannot tell apart are a break that never happened and a break that did.
   Measured: one hand-written sidecar beside a genuine break made `recover!`
   revert it, unlink the evidence and return `:broken-locks {:found 0}`. Round
   six's worst forgery kept a file for ever, which is fail-safe; that one
   removed it. Evidence is never deleted on an uncorroborated match: the
   marker-only case is stamped and KEPT, typed so a reader can see the marker
   was not believed."
  [transactions-dir]
  (let [^File lock (lock-file transactions-dir)
        key (lock-file-key lock)]
    (into []
          (keep (fn [^File f]
                  (cond
                    (and key (= key (lock-file-key f))) [f :corroborated]
                    (= :linked (break-phase f)) [f :uncorroborated-marker]
                    :else nil)))
          (broken-lock-files transactions-dir))))

(defn- interrupted-break-files
  "EVERY tombstone whose break never completed.

   A crash between `Files/createLink` and the unlink leaves a `LOCK.broken.*`
   holding the claim's bytes with the LOCK still in place. It is not evidence
   of a break, because the break never happened - reporting it as one names a
   claim that currently holds the lock, and a later break by that txid is
   refused for a name that records nothing. `recover!` resolves them; the
   sweep types them apart from a break that happened.

   TWO rules, because one of them expires. The tombstone's own `:phase
   :linked` marker survives anything that happens to the LOCK afterwards -
   including the wrongly-broken holder's own clean release, after which the
   inode rule can no longer see it at all and the sweep silently re-typed the
   file as a break that happened. The (device, inode) rule catches the crash
   that lands before the marker is written, and old tombstones from builds
   that wrote no marker.

   EVERY match, not the first: the prune excluded all of them while the
   listing typed one, so with two interrupted breaks at once the second was
   published as `:kind :broken-lock` - false evidence of a break, naming the
   claim that currently holds the lock - until a second `recover!` ran."
  [transactions-dir]
  (mapv (fn [entry] (nth entry 0)) (interrupted-break-entries transactions-dir)))

(defn- prune-broken-locks!
  "Retire the tombstones older than `broken-lock-retention-ms`, and count them.

   Returns the bucket both `recover!` and any caller reading it can see. A
   non-zero `:remaining` is a standing count, not an archive.

   `:vanished` counts the entries whose file was gone between the listing and
   the stat. They are deliberately NOT in `:found`: `lastModified` of a
   missing file is 0, which reads as infinitely old, so the old count found
   them, failed to delete them, and inflated `:remaining` with files that no
   longer existed - 822 of 9,831 rows under concurrency. An absent file is
   absent, which is neither an age nor a member of a standing count.

   `:orphan-sidecars` is the same discipline one file down. A
   `LOCK.broken-at.*` whose tombstone is gone was listed by nothing and
   retired by nothing; it is retired here on the SAME published retention, and
   counted, because a bucket nothing sweeps is accumulation whatever its size.

   `:unreadable-stamps` is that discipline applied to a CONDITION rather than
   a file. A stamp `broken-lock-stamp-tolerance-ms` or more ahead of the clock
   is correctly refused as a time, but the fact was typed per row and counted
   by nothing - so on two hosts sharing one filesystem, where the drift the
   tolerance was sized for does not hold, every tombstone the skewed host
   writes reads `:stamp :unreadable` and no standing count says the stamp
   mechanism has stopped working. Harmless to retention, invisible to the
   operator whose clock drifted: the same class as the `:vanished` bucket the
   822 ghost rows produced. A non-zero value here is an alarm about a clock,
   not an archive."
  [transactions-dir now-ms]
  (let [now (long (or now-ms (System/currentTimeMillis)))
        tombstones (broken-lock-files transactions-dir)
        ;; a tombstone whose break never completed is not a break that
        ;; happened: retiring it here would resolve it silently
        live (interrupted-break-files transactions-dir)
        ;; read BEFORE the tombstone prune, so a sidecar this call deletes
        ;; alongside its own tombstone is never counted as an orphan
        orphans (mapv (fn [^File f] [f (evidence-age f f now)])
                      (orphan-sidecar-files transactions-dir))
        aged (mapv (fn [^File f] [f (tombstone-age f now)])
                   (remove (set live) tombstones))
        retire! (fn [entries retire-one!]
                  (let [present (filterv (fn [entry] (map? (nth entry 1))) entries)
                        pruned (long (reduce
                                       (fn [n entry]
                                         (let [^File f (nth entry 0)
                                               age (long (:age-ms (nth entry 1)))]
                                           (if (and (>= age (long broken-lock-retention-ms))
                                                    (retire-one! f))
                                             (unchecked-inc (long n))
                                             (long n))))
                                       0
                                       present))
                        found (long (count present))]
                    {:found found
                     :pruned pruned
                     :remaining (- found pruned)
                     :vanished (- (long (count entries)) found)}))
        unreadable (long (count (filter (fn [entry]
                                          (= :unreadable
                                             (:stamp (nth entry 1))))
                                        (concat aged orphans))))
        tombs (retire! aged (fn [^File f]
                              (Files/deleteIfExists
                                (.toPath ^File (broken-at-file f)))
                              (Files/deleteIfExists (.toPath f))))
        sidecars (retire! orphans (fn [^File f]
                                    (Files/deleteIfExists (.toPath f))))]
    {:found (:found tombs)
     :pruned (:pruned tombs)
     :remaining (:remaining tombs)
     :vanished (:vanished tombs)
     :interrupted (long (count live))
     :orphan-sidecars (select-keys sidecars [:found :pruned :remaining])
     :unreadable-stamps unreadable
     :retention-ms broken-lock-retention-ms}))

(defn- displaced-line
  "The typed, counted line a refused restore is reported as, or nil.

   Both callers used to keep `(:broken outcome)` alone, so `:restored false`
   and its cause were discarded and the refusal named the third acquirer with
   no hint that a claim had been displaced - a silent refusal with no owner."
  [outcome]
  (when (false? (:restored outcome))
    (assoc (select-keys outcome [:cause :restored :restore-cause
                                 :restore-path :restore-message :tombstone])
           :displaced-claims-total (displaced-claim-count))))

(defn- restore-by-move!
  "The restore for a filesystem with no hard links at all.

   `Files/move` with ATOMIC_MOVE and no REPLACE_EXISTING is the weakest form
   that is still honest: the existence check is the JDK's, not the kernel's,
   so this path is CHECK-THEN-ACT and says so rather than claiming otherwise.
   ext4 - where this kernel and every one of its witnesses run - never reaches
   it, because `link(2)` is available there."
  [^File tomb ^File lock]
  (if (.exists lock)
    (do (swap! displaced-claims inc)
        {:restored false :restore-cause :lock-reappeared :restore-path :move})
    (try
      (Files/move (.toPath tomb) (.toPath lock)
                  ^"[Ljava.nio.file.CopyOption;"
                  (into-array CopyOption [StandardCopyOption/ATOMIC_MOVE]))
      {:restored true :restore-path :move}
      (catch Exception cause
        (swap! displaced-claims inc)
        {:restored false :restore-cause :restore-failed
         :restore-path :move :restore-message (.getMessage cause)}))))

(defn- restore-lock!
  "Put a claim that was renamed away back, without clobbering whoever arrived.

   `Files/move` with no REPLACE_EXISTING READS as create-if-absent and is not
   one: the JDK stats the target and then calls `rename(2)`, and POSIX rename
   replaces unconditionally, so a third acquirer that creates its LOCK between
   those two syscalls is destroyed silently - measured, 145 of 423 third-party
   claims in one 2,000-break storm. `Files/createLink` IS create-if-absent at
   the kernel level: `link(2)` fails EEXIST, and it is the same primitive
   `write-lock!` acquires with. The tombstone is unlinked only once the link
   has succeeded, so a refused restore leaves the displaced claim on disk
   under a name its caller is told."
  [^File tomb ^File lock]
  (try
    (Files/createLink (.toPath lock) (.toPath tomb))
    (Files/deleteIfExists (.toPath tomb))
    {:restored true}
    (catch java.nio.file.FileAlreadyExistsException _
      (swap! displaced-claims inc)
      {:restored false :restore-cause :lock-reappeared})
    (catch UnsupportedOperationException _ (restore-by-move! tomb lock))
    (catch Exception cause
      (swap! displaced-claims inc)
      {:restored false :restore-cause :restore-failed
       :restore-message (.getMessage cause)})))

(defn- break-by-move!
  "The break for a filesystem with no hard links at all.

   `rename(2)` is the only primitive left, and it replaces unconditionally, so
   the name guard here is `.exists` followed by a move: CHECK-THEN-ACT, which
   this docstring says rather than claims otherwise. Measured on the branch
   that shipped it as the only path: two breakers sharing one txid destroyed
   the judged claim, and BOTH were handed the same tombstone name. ext4 -
   where this kernel and every one of its witnesses run - never reaches it,
   because `link(2)` is available there. `:unsafe-break-by-move true` is
   the only way to reach it where `link(2)` works, and it is spelled that way
   deliberately: the previous name, `:no-hard-links`, was an ordinary
   descriptive word on the PUBLIC `begin!`/`recover!` surface that silently
   selected this path, and no receipt said which path had run."
  [_transactions-dir ^File lock ^File tomb claim opts]
  (if (.exists tomb)
    {:broken false :cause :tombstone-exists :tombstone (.getName tomb)}
    (try
      (Files/move (.toPath lock) (.toPath tomb)
                  ^"[Ljava.nio.file.CopyOption;"
                  (into-array CopyOption [StandardCopyOption/ATOMIC_MOVE]))
      (let [moved-content (try (slurp tomb) (catch Exception _ nil))]
        (if (and (some? (:content claim))
                 (= (:content claim) moved-content)
                 (= (:file-key claim) (lock-file-key tomb)))
          {:broken true
           :tombstone (.getName tomb)
           :content-sha256 (:content-sha256 claim)
           :broken-at-ms (stamp-tombstone! tomb)
           :break-path :move}
          ;; somebody else's claim: put it back untouched and break nothing
          (do
            ;; the seam a witness needs to put a third acquirer in the gap
            (when-let [hook (:before-restore opts)] (hook tomb))
            (let [outcome (restore-lock! tomb lock)]
              ;; a refused restore leaves the displaced claim in the
              ;; tombstone, so that file is evidence too and is retained
              ;; from ITS creation, not from the claim's mtime
              (when (false? (:restored outcome)) (stamp-tombstone! tomb))
              (cond-> (merge {:broken false :cause :holder-changed} outcome)
                (false? (:restored outcome)) (assoc :tombstone (.getName tomb)))))))
      (catch java.nio.file.NoSuchFileException _
        {:broken false :cause :lock-vanished})
      (catch java.nio.file.FileAlreadyExistsException _
        {:broken false :cause :tombstone-exists :tombstone (.getName tomb)})
      (catch Exception cause
        {:broken false :cause :break-failed :message (.getMessage cause)}))))

(defn- break-by-link-finish!
  "Everything after the tombstone name is held: prove the linked file is the
   judged claim, record the break, and unlink the LOCK."
  [^File lock ^File tomb claim opts]
  (try
    (let [linked-content (try (slurp tomb) (catch Exception _ nil))]
      (if (and (some? (:content claim))
               (= (:content claim) linked-content)
               (= (:file-key claim) (lock-file-key tomb))
               ;; and the LOCK still IS that claim, so the unlink below takes
               ;; the judged claim rather than whatever replaced it
               (= (:file-key claim) (lock-file-key lock)))
        (do
          ;; the seam a witness needs to crash between the link and the unlink
          (when-let [hook (:before-unlink opts)] (hook tomb))
          ;; the break's own creation time is recorded BEFORE the unlink, and
          ;; the mtime half after it. A crash between the unlink and the stamp
          ;; would otherwise leave a break that DID happen still wearing the
          ;; `:phase :linked` marker, and recovery would revert real evidence.
          ;; The reverse crash - stamped, LOCK not yet unlinked - is still an
          ;; interrupted break, and the inode rule types it.
          (if-let [broken-at (stamp-broken-at! tomb)]
            (do
              (Files/deleteIfExists (.toPath lock))
              (touch-tombstone! tomb broken-at)
              {:broken true
               :tombstone (.getName tomb)
               :content-sha256 (:content-sha256 claim)
               :broken-at-ms broken-at
               :break-path :link})
            ;; the break cannot write its own creation time, so it cannot be
            ;; recorded - and an unrecorded break is a destroyed claim with
            ;; nothing on disk saying who took it, plus a tombstone still
            ;; wearing the `:phase :linked` marker for recovery to revert.
            ;; Leave the LOCK exactly where it is, drop our own extra link,
            ;; and hand the caller the cause.
            (do (delete-quietly! (broken-at-file tomb))
                (delete-quietly! tomb)
                {:broken false
                 :cause :evidence-unrecordable
                 :restored true
                 :restore-path :lock-never-left})))
        ;; not the claim that was judged. The LOCK never left, so there is
        ;; nothing to put back and nothing to displace: drop our own link,
        ;; and the marker that said we held it.
        (do (Files/deleteIfExists (.toPath ^File (broken-at-file tomb)))
            (Files/deleteIfExists (.toPath tomb))
            {:broken false
             :cause :holder-changed
             :restored true
             :restore-path :lock-never-left})))
    (catch java.nio.file.NoSuchFileException _
      {:broken false :cause :lock-vanished})
    (catch Exception cause
      {:broken false :cause :break-failed :message (.getMessage cause)})))

(defn- break-by-link!
  "Claim the tombstone NAME first, and unlink the LOCK only once it is held.

   The order is the whole fix. A break used to rename the LOCK onto the
   tombstone name and check afterwards, which made the name guard a
   check-then-act in front of `rename(2)`: two breakers sharing one txid -
   and `begin!` takes `:txid` verbatim from its caller - destroyed the judged
   claim 13 times in 4,000 races, with BOTH handed the same tombstone name for
   a claim only one of them owned.

   `Files/createLink` is create-if-absent IN THE KERNEL: `link(2)` fails
   EEXIST, and that refusal is the atomic guard, not a stat this code performs
   itself. It also never moves the LOCK, so between the link and the unlink
   the claim is in TWO places rather than none - a third acquirer cannot land
   in a gap that no longer exists, and the whole displacement class goes with
   it. What was a restore is now simply unlinking our own extra link.

   THE RESIDUAL, exactly. Unlinking the LOCK side is a stat of its (device,
   inode) followed by `unlink(2)`, because POSIX has no unlink-if-inode: a
   claim that replaces the LOCK between those two syscalls is removed. That
   window contains no I/O and is entered only after the linked claim has been
   proved to be the judged one. A crash inside it leaves a tombstone that is a
   second link to the LIVE LOCK, which `recover!` resolves - as one of its
   `:interrupted-breaks` - rather than reporting as a break that happened. The
   tombstone's own `:phase :linked` marker says so too, and outlives that
   inode.

   THE MARKER IS CLAIMED BEFORE THE NAME. Written after the link, it left a
   two-statement window in which a tombstone existed with no marker at all -
   typed by the inode rule alone, and re-typed as a break that happened the
   moment the wrongly-broken holder released its own claim. Written first,
   every state this function can be interrupted in is one both typing rules
   can see: a marker with no tombstone (an orphan sidecar, which the sweep
   lists and retires), or a tombstone that has always had its marker."
  [transactions-dir ^File lock ^File tomb claim opts]
  (let [^File side (broken-at-file tomb)]
    (case (mark-break-linked! tomb)
      ;; this break's evidence name is already in use. Overwriting it would
      ;; turn another break's stamp into this one's marker - the forgery
      ;; finding 1 exists for, performed by the kernel itself - so refuse
      ;; BEFORE the LOCK is touched.
      :name-taken
      (if (.isFile tomb)
        {:broken false :cause :tombstone-exists :tombstone (.getName tomb)}
        {:broken false :cause :evidence-unrecordable
         :evidence :sidecar-name-taken
         :sidecar (.getName side)})

      ;; nothing can record that this break began, so it does not begin
      :failed {:broken false :cause :evidence-unrecordable}

      ;; no `link(2)` on this filesystem, so neither the marker nor the break
      ;; can be claimed that way: the whole break takes the documented
      ;; check-then-act fallback, which writes its own stamp by rename
      :unsupported (break-by-move! transactions-dir lock tomb claim opts)

      ;; :created - the marker is on disk, so the window the link below opens
      ;; contains no state either typing rule can miss
      (let [linked (try
                     ;; the seam a witness needs to observe the ORDER, and to
                     ;; interrupt a break before it has claimed any name
                     (when-let [hook (:before-link opts)] (hook tomb))
                     (Files/createLink (.toPath tomb) (.toPath lock))
                     :linked
                     (catch java.nio.file.FileAlreadyExistsException _
                       :tombstone-exists)
                     (catch java.nio.file.NoSuchFileException _ :lock-vanished)
                     (catch UnsupportedOperationException _ :unsupported)
                     (catch Exception cause
                       {:broken false :cause :break-failed
                        :message (.getMessage cause)}))]
        (if-not (= :linked linked)
          ;; the name was never claimed, so the marker records nothing that
          ;; happened: take it back rather than leave an orphan behind
          (do (delete-quietly! side)
              (case linked
                :tombstone-exists {:broken false :cause :tombstone-exists
                                   :tombstone (.getName tomb)}
                :lock-vanished {:broken false :cause :lock-vanished}
                :unsupported (break-by-move! transactions-dir lock tomb claim opts)
                linked))
          (break-by-link-finish! lock tomb claim opts))))))

(def ^:private break-refusal-causes
  "The break outcomes that are a REFUSAL with an owner rather than a failure.

   Both reach the caller as `:lock-break-refused`, because a refusal nobody
   hears is indistinguishable from a silent loss: recovery once dropped the
   `:tombstone-exists` case entirely and returned an ordinary success having
   broken nothing."
  #{:tombstone-exists :evidence-unrecordable})

(defn- break-refusal-remedy
  "What a caller can actually do about a refused break."
  [cause]
  (case cause
    :evidence-unrecordable
    (str "The break could not write the evidence file that records when it "
         "happened, so it left the claim alone: an unrecorded break is a "
         "destroyed claim with nothing on disk naming who took it. Check that "
         "the transactions directory is writable and has space, then retry.")
    (str "A tombstone of that name already exists, so breaking would destroy "
         "the evidence of an earlier break. Retry with a different :txid, or "
         "retire the tombstone by running recovery.")))

(defn- break-lock!
  "Take EXACTLY the stale claim that was read out of the way, or nothing.

   Breaking used to be a read followed by an unconditional delete, and the gap
   between the two is real: a second transaction can break the same stale claim
   and acquire inside it, after which the first breaker deletes a LIVE holder's
   brand new LOCK and acquires as well. Two live transactions then hold the
   project lock and the first to finish unlinks the other's claim.

   The break now CLAIMS THE TOMBSTONE NAME FIRST, by `Files/createLink` from
   the LOCK - create-if-absent in the kernel - and unlinks the LOCK only after
   the linked file has been proved, by content AND (device, inode), to be the
   claim that was judged, and only while the LOCK still names that same inode.
   The claim is therefore in two places or one, never none, so no third
   acquirer can land in a gap and no restore is needed.

   WHAT THIS GUARANTEES, exactly. An existing tombstone of that name is a
   typed refusal - `link(2)` returning EEXIST, the kernel's own verdict, not a
   stat this code performs - taken BEFORE the LOCK is touched, never a
   replacement. A claim that is not the judged one is never removed: the
   outcome is `{:broken false :cause :holder-changed :restored true
   :restore-path :lock-never-left}` and the LOCK is untouched.

   WHAT IT DOES NOT. Unlinking the LOCK is a stat followed by `unlink(2)` with
   no I/O between, because POSIX has no unlink-if-inode; and on a filesystem
   with no `link(2)` at all the fallback `break-by-move!` is the old
   check-then-act, which says so in its own docstring. `:unsafe-break-by-move
   true` forces that fallback where `link(2)` exists - named so that no caller
   reaches the measured check-then-act path by typing an ordinary word - and
   the outcome carries `:break-path :link` or `:break-path :move`, which BOTH
   callers put in the `:lock-broken` line they publish.

   The tombstone is the break's evidence, named for the BREAKER, whose txid
   comes from the caller; `stamp-tombstone!` gives it its own creation time and
   `recover!` retires it past `broken-lock-retention-ms`."
  ([transactions-dir claim txid] (break-lock! transactions-dir claim txid nil))
  ([transactions-dir claim txid opts]
   (let [^File lock (lock-file transactions-dir)
         ^File tomb (io/file transactions-dir (str tombstone-prefix txid))]
     (if (:unsafe-break-by-move opts)
       (break-by-move! transactions-dir lock tomb claim opts)
       (break-by-link! transactions-dir lock tomb claim opts)))))

(defn- write-lock!
  "Create the LOCK ALREADY populated, or fail because one exists.

   The payload is written into a temporary first and given the LOCK's name by
   a same-directory hard link, which is create-if-absent. No reader can then
   observe an empty lock and mistake an unwritten claim for an unowned one."
  [^File lock txid]
  (let [^File tmp (File/createTempFile ".LOCK-" ".tmp" (.getParentFile lock))]
    (try
      (spit tmp (pr-str (merge {:txid txid
                                ;; stamped so a later build can name this
                                ;; format rather than infer it from absence
                                :lock-format lock-format
                                :acquired-at (str (java.time.Instant/now))}
                               (holder-identity))))
      (Files/createLink (.toPath lock) (.toPath tmp))
      (finally (Files/deleteIfExists (.toPath tmp))))))

(defn- try-write-lock!
  "Create the LOCK, or report that one already exists. A separate verb because
   `recur` cannot cross a `try`."
  [^File lock txid]
  (try
    (write-lock! lock txid)
    true
    (catch java.nio.file.FileAlreadyExistsException _ false)))

(defn- acquire-lock!
  ;; @spec MCP-OP-MEM-013
  "Take the project lock, breaking one whose holder is PROVABLY gone.

   A LOCK is a claim by a PROCESS, and a process can die between making the
   claim and releasing it. Nothing used to read the recorded pid back, so a
   crash after the transaction directory was finished - or any hand-cleaned
   journal - deadlocked the workspace for ever, and the refusal's own remedy
   did not clear it. The claim now records pid, start ticks and boot id, and a
   lock that fails all three is broken exactly ONCE with a typed line. A LIVE
   holder's lock is never broken, however old it is."
  [transactions-dir txid opts]
  (let [^File lock (lock-file transactions-dir)]
    (loop [attempt 0 broken nil displaced nil refused-break nil]
      (if (try-write-lock! lock txid)
        (cond-> {:ok true}
          broken (assoc :lock-broken broken)
          displaced (assoc :lock-break-displaced displaced)
          refused-break (assoc :lock-break-refused refused-break))
        (let [claim (read-lock-claim lock)
              holder (:holder claim)
              cause (stale-holder holder)]
          (if (and (contains? breakable-causes cause) (zero? attempt))
            (do (when-let [hook (:before-break opts)] (hook claim))
                (let [outcome (break-lock! transactions-dir claim txid opts)]
                  (recur (inc attempt)
                         (if (:broken outcome)
                           (merge (lock-broken-line holder cause)
                                  ;; :break-path travels with the receipt: a
                                  ;; reader must be able to tell the
                                  ;; kernel-atomic break from the fallback
                                  ;; check-then-act one that produced this
                                  ;; evidence
                                  (select-keys outcome [:tombstone :content-sha256
                                                        :break-path]))
                           broken)
                         (or (displaced-line outcome) displaced)
                         (or (when (contains? break-refusal-causes
                                               (:cause outcome))
                               (select-keys outcome [:cause :tombstone]))
                             refused-break))))
            (let [legacy? (= cause :legacy-format)]
              (refusal :txn-lock-held
                       (if legacy?
                         (str "The project lock was written by an earlier build - a pid and no boot id, "
                              "so a reused pid cannot be told from a live holder: " (:txid holder))
                         (str "Another transaction holds the project lock: " (:txid holder)))
                       (cond-> {:holder-txid (:txid holder)
                                :holder-pid (:pid holder)
                                :holder-live (nil? cause)
                                :holder-cause cause
                                :next_call {:op :txn/recover
                                            :workspace_root nil}
                                :remedy "Wait for the holder, or run recovery if its process is gone."}
                         displaced
                         (assoc :lock-break-displaced displaced)

                         refused-break
                         (assoc :lock-break-refused
                                (assoc refused-break
                                       :remedy (break-refusal-remedy
                                                 (:cause refused-break))))

                         legacy?
                         (assoc :holder-format :pid-only
                                :lock-format-expected lock-format
                                :next_call {:op :txn/recover
                                            :workspace_root nil
                                            :break_legacy_lock true}
                                :remedy (str "This LOCK predates the checkable holder triple, so it is "
                                             "unreadable rather than unbreakable. Run recovery with "
                                             ":break-legacy-lock true; it breaks the claim only on a "
                                             "receipt of the holder's death - the recorded pid naming no "
                                             "live process, which is checked against the process table, "
                                             "AND the lock older than " legacy-lock-break-age-ms
                                             " ms, measured against the NEWEST of its mtime and ctime "
                                             "because mtime alone is settable and survives a copy.")))))))))))

(defn with-cooperating-writes
  ;; @spec MCP-OP-MEM-007
  "Run `f` with every `file-ops/atomic-write!` on this thread cooperating.

   Cooperation with the publish lock is PER-WRITER, and this is the opt-in.
   The kernel's own commit path was for a long time the only caller of the
   lock in the whole repository, which made the advisory lock's promise -
   \"it excludes any writer that asks for it\" - true with an empty referent.
   Every other source-mutating path in this repo publishes through
   `file-ops/atomic-write!`; wrapping one in this makes it a writer the
   kernel's recheck-to-rename window actually excludes.

   `transactions-dir` may be a transaction value or the directory itself."
  [transactions-dir f]
  (file-ops/with-publish-lock-dir*
    (if (map? transactions-dir) (:transactions-dir transactions-dir) transactions-dir)
    f))

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
   `record-read!` and goes straight to the sorted manifest on disk.

   `:before-break` is an injection point, the same kind `commit!` offers: it is
   called with the claim a stale-lock break has judged, immediately before the
   break acts on it, so a witness can put a live holder's claim in the way."
  ;; `:before-break` is read straight from `opts` by `acquire-lock!`
  [workspace-root {:keys [state-home txid scope-walk] :as opts}]
  (let [resolved (workspace/canonical-root workspace-root)]
    (if-not (:ok resolved)
      (refusal :txn-workspace-refused (:error resolved) {})
      (let [root (:workspace-root resolved)
            transactions (workspace/transactions-dir root state-home)
            _ (.mkdirs (io/file transactions))
            txid (or txid (new-txid))
            lock (acquire-lock! transactions txid opts)]
        (if-not (:ok lock)
          lock
          (let [dir (io/file transactions txid)
                objects (io/file dir "objects")
                staging (io/file dir "staging")
                limits (reduce-kv (fn [acc k v]
                                    (assoc acc k (min (long (get opts k v))
                                                      (long (get hard-limits k v)))))
                                  {}
                                  default-limits)]
            (.mkdirs objects)
            (.mkdirs staging)
            (let [txn (cond-> {:txid txid
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
                                 :scope-walk scope-walk})}
                        (:lock-broken lock)
                        (assoc :lock-broken (:lock-broken lock))

                        (:lock-break-displaced lock)
                        (assoc :lock-break-displaced (:lock-break-displaced lock)))]
              (write-state! txn :open {:started-at (str (java.time.Instant/now))})
              (append-journal! txn (str "begin\t" txid))
              (when-let [broken (:lock-broken lock)]
                ;; the break is durable evidence, not only a return value
                (append-journal! txn (str "lock-broken\t" (:pid broken) "\t"
                                         (name (:cause broken)))))
              (when-let [displaced (:lock-break-displaced lock)]
                ;; so is a claim this acquisition moved and could not put back
                (append-journal! txn (str "lock-displaced\t" (:tombstone displaced) "\t"
                                          (name (:restore-cause displaced)))))
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
        {:keys [sealed? last-path]} @state
        read-set-count (long (:read-set-count @state))
        limit (long (get-in txn [:limits :max-read-set-files]))]
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
      (let [acc (long acc)]
        (cond
          (and (map? value) (contains? value :path)) (inc acc)
          (and (map? value) (seq value)
               (every? #(and (string? %) (str/starts-with? % "/")) (keys value)))
          (+ acc (count value))
          :else acc)))
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
      (let [acc (long acc)]
        (if (and (string? value) (> (count value) (long retained-string-limit)))
          (+ acc (count value))
          acc)))
    0))

(defn manifest-line-count
  "Lines in the on-disk read-set manifest, counted without holding it."
  [txn]
  (with-open [reader (io/reader (io/file (:manifest-path txn)))]
    (reduce (fn [n _] (inc (long n))) 0 (line-seq reader))))

(defn staged-file-count [txn] (count (:staged @(:state txn))))
(defn journal-bytes [txn] (:journal-bytes @(:state txn)))

;; ------------------------------------------------------------------- quota

(defn- admit-journal-bytes!
  "Reserve `bytes` of journal quota, or refuse before anything is copied."
  [txn bytes path kind]
  (let [state (:state txn)
        bytes (long bytes)
        limit (long (get-in txn [:limits :max-journal-bytes]))
        used (long (:journal-bytes @state))]
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
                         (let [used (+ (long (:journal-bytes st)) bytes)]
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

      (not (.startsWith (.toPath file) ^java.nio.file.Path root))
      (outside-workspace raw root :outside-root
                         (str raw " is not named under the transaction's workspace root"))

      :else
      (let [^java.nio.file.Path root root
            ^java.nio.file.Path absolute (.toPath (io/file (.getCanonicalPath file)))]
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
        limit (long (get-in txn [:limits :max-staged-files]))]
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
  (doseq [^File file (reverse (file-seq (io/file dir)))]
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
     ;; TERMINAL from here on. The journal and `state.edn` now record this
     ;; transaction's outcome, and everything below is bookkeeping: nothing
     ;; that fails past this line may revert what the record already says
     ;; happened. `finish-after-throw!` reads this.
     (swap! (:state txn) assoc :finished? true)
     (when-let [^FileOutputStream stream (:journal-stream state)]
       (try (.close stream) (catch Exception _ nil)))
     (release-lock! (:transactions-dir txn) (:txid txn))
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

   The order is the whole point. BOTH expensive steps happen first, outside the
   publish lock and outside the window: the staged bytes are copied into the
   target's own directory, and the target's current digest is read with a stat
   on each side of it. Inside the lock only that stat is compared - a target
   whose type, identity, size and timestamps are unchanged still holds the
   bytes the digest described - so no read of the target happens inside the
   window and its size term falls about four-fold rather than to zero, which is
   what the contract's `:size-term` measures. When the stat moved, the digest
   IS re-read inside the
   lock and the receipt reports it, so the fast path can never be mistaken for
   a skipped check.

   Returns {:ok true :window-ns n :reread? b}, {:conflict-refusal m}, or
   {:failed cause}."
  [txn path {:keys [h0 identity0 staging prepare-fn publish-fn before-recheck
                    in-commit-window]}]
  (let [prepared (try {:tmp (prepare-fn path staging)}
                      (catch Exception cause {:failed cause}))]
    (if (:failed prepared)
      prepared
      (let [^File tmp (:tmp prepared)
            ;; the O(size) read, outside the lock. A stat on each side says
            ;; whether it describes a file that held still while it was read.
            stat-before (path-stat path)
            pre-digest (try (sha256-file path) (catch Exception _ nil))
            stat-after (path-stat path)
            pre-stable? (boolean (and pre-digest (= stat-before stat-after)))]
        (try
          (when before-recheck (before-recheck path))
          (file-ops/with-publish-lock* (:transactions-dir txn)
            (fn []
              (let [opened (System/nanoTime)
                    stat-now (path-stat path)
                    reread? (not (and pre-stable? (= stat-now stat-after)))
                    current (if reread? (sha256-file path) pre-digest)
                    identity-now (select-keys stat-now [:kind :file-key])]
                (cond
                  (not= current h0)
                  {:conflict-refusal (assoc (conflict path h0 current)
                                            :conflict :digest)}

                  (not= identity-now identity0)
                  {:conflict-refusal (identity-conflict path identity0 identity-now)}

                  :else
                  (do
                    (when in-commit-window (in-commit-window path))
                    ;; the temp's NAME goes in the line, so recovery's orphan
                    ;; sweep can delete this transaction's own leftovers and
                    ;; nobody else's
                    (append-journal! txn (str "write-begin\t" path "\t" h0
                                              "\t" (.getName tmp)))
                    (try
                      (publish-fn path tmp)
                      {:ok true
                       :window-ns (- (System/nanoTime) opened)
                       :reread? reread?}
                      (catch Exception cause {:failed cause})))))))
          (finally
            (when (.exists tmp) (.delete tmp))))))))

(defn- finish-after-throw!
  "End a transaction whose commit raised something no path anticipated.

   A transaction that never reaches `finish!` is a project LOCK nobody
   releases, and a LOCK held by a LIVE pid is one neither `begin!` nor
   `recover!` is permitted to break - so an uncaught exception inside the
   commit deadlocked the workspace for the life of the process. Every
   exception path must end the transaction; the exception itself is re-thrown
   by the caller rather than swallowed into a false receipt.

   The last resort is releasing the lock alone: if the rollback or the
   bookkeeping is what failed, an unreleased lock would turn one failure into
   a permanent one.

   PAST A TERMINAL STATUS THERE IS NOTHING TO ROLL BACK. `finish!` publishes,
   appends its status to the journal, writes `state.edn` and releases the
   project LOCK before its tail reclaims staging and writes the lease; an
   ENOSPC in that tail used to reach this catch and revert every written path
   to H0, leaving the durable record saying `:committed` and the tree saying
   otherwise - a disagreement `recover!` will not touch, because `:committed`
   is terminal, and `undo!` refuses as a digest conflict. Those revert writes
   also ran after the LOCK was released, outside every lock. So past the
   terminal flag this degrades to the bare release, which is idempotent: a
   lock this transaction no longer names is not unlinked."
  [txn]
  (if (:finished? @(:state txn))
    (try (release-lock! (:transactions-dir txn) (:txid txn))
         (catch Throwable _ nil))
    (try
      (finish! txn :rolled-back (rollback-written! txn))
      (catch Throwable _
        (try (release-lock! (:transactions-dir txn) (:txid txn))
             (catch Throwable _ nil))))))

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
   (try
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
             (loop [remaining paths written 0 window-ns 0 rereads 0]
               (if (empty? remaining)
                 (let [finished (finish! txn :committed)]
                     {:ok true
                      :committed true
                      :retained (:retained finished)
                      :txid (:txid txn)
                      :files-written written
                      :read-set-files (:read-set-count @state)
                      :commit-window (assoc commit-window :max-ns window-ns)
                      :digest-rereads rereads
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
                       ;; H1: what this commit LEFT BEHIND. `undo!` rechecks
                       ;; the target against it before republishing H0, so a
                       ;; write that landed after the commit is refused rather
                       ;; than clobbered.
                       (append-journal! txn (str "write-done\t" path "\t" result-hash
                                                 "\t" (identity-token (path-identity path))))
                       (when after-publish (after-publish path))
                       (let [actual (sha256-file path)]
                         (if (= actual result-hash)
                           (recur (rest remaining) (inc written)
                                  (max window-ns (long (:window-ns outcome 0)))
                                  (if (:reread? outcome) (inc rereads) rereads))
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
                                       :remedy "Another writer that does not hold the project lock raced this rename; the contract detects that rather than preventing it."}))))))))))))))
    ;; EVERY exception path ends the transaction; the throw itself is
    ;; re-raised rather than converted into a receipt.
    (catch Throwable cause
      (finish-after-throw! txn)
      (throw cause)))))

;; ---------------------------------------------------------------- recovery

(defn- journal-lines
  [dir]
  (let [file (io/file dir "journal.log")]
    (if (.isFile file)
      (with-open [reader (io/reader file)]
        (vec (line-seq reader)))
      [])))

(defn- restore-begun!
  "Republish every begun path from its pinned object and verify each.

   The orphan sweep is scoped to the temporaries THIS journal recorded. It used
   to delete every `.clj-surgeon-publish-*` sibling of every begun path, which
   - with two state homes on one workspace root, which do not exclude each
   other - let one workspace's recovery delete another in-flight transaction's
   PREPARED temp between its prepare and its rename. The narrow case the
   scoping gives up: a process killed between `prepare-publish!` and the
   `write-begin` line leaves a temp no journal names. That is litter beside the
   target, never a lost or corrupted byte."
  [dir pins begun temps]
  (let [restored
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
    ;; remove the staging temporaries THIS journal recorded and no others
    (doseq [path begun
            tmp-name (get temps path)]
      (Files/deleteIfExists
        (.toPath (io/file (.getParentFile (io/file path)) tmp-name))))
    {:txid (.getName (io/file dir))
     :paths restored
     :ok (every? #(= :verified (:status %)) restored)}))

(defn- undo-conflicts
  "Paths whose CURRENT state is not the state the commit left behind (H1).

   `undo!` is a WRITE, and a write that does not recheck clobbers whatever
   landed after the commit. Crash recovery cannot ask this question - a killed
   transaction left the tree part-written on purpose - so only a deliberate
   undo of a COMMITTED receipt makes it."
  [posts begun]
  (vec (keep (fn [path]
               (let [post (get posts path)]
                 (cond
                   (nil? post)
                   {:path path :conflict :no-recorded-result}

                   (not (.isFile (io/file path)))
                   {:path path :conflict :missing :expected-hash (:sha256 post)}

                   :else
                   (let [current (sha256-file path)
                         token (identity-token (path-identity path))]
                     (cond
                       (not= current (:sha256 post))
                       {:path path :conflict :digest
                        :expected-hash (:sha256 post) :actual-hash current}

                       (and (:identity-token post)
                            (not= token (:identity-token post)))
                       {:path path :conflict :identity
                        :expected-identity (:identity-token post)
                        :actual-identity token}

                       :else nil)))))
             begun)))

(defn- restore-from-journal!
  "Restore every path a journal recorded as BEGUN, from its own pinned objects.

   The single restoration primitive: crash recovery and a deliberate `undo!` of
   a committed receipt are the same act read from the same lines. Only
   `write-begin` paths are touched - a pinned path the transaction never wrote
   is not this journal's business, and republishing it would clobber a write
   somebody else made.

   It runs under the workspace PUBLISH lock, the same lock the commit path
   takes, whenever the caller knows which workspace this journal belongs to.
   `undo!` additionally passes `:expect-post-commit?`, which rechecks every
   begun path's digest AND identity against what the commit recorded as H1 and
   refuses the WHOLE undo, with zero writes, if any path moved."
  ([dir] (restore-from-journal! dir {}))
  ([dir {:keys [transactions-dir expect-post-commit?]}]
   (let [lines (journal-lines dir)
        pins (reduce (fn [acc line]
                       (let [[op path digest] (str/split line #"\t")]
                         (if (= "pin" op) (assoc acc path digest) acc)))
                     {}
                     lines)
        posts (reduce (fn [acc line]
                        (let [[op path digest token] (str/split line #"\t")]
                          (if (= "write-done" op)
                            (assoc acc path {:sha256 digest :identity-token token})
                            acc)))
                      {}
                      lines)
        begun (vec (distinct
                     (keep (fn [line]
                             (let [[op path] (str/split line #"\t")]
                               (when (= "write-begin" op) path)))
                           lines)))
        temps (reduce (fn [acc line]
                        (let [[op path _ tmp] (str/split line #"\t")]
                          (if (and (= "write-begin" op) tmp)
                            (update acc path (fnil conj #{}) tmp)
                            acc)))
                      {}
                      lines)
        restore!
        (fn []
          (let [conflicts (when expect-post-commit?
                            (seq (undo-conflicts posts (reverse begun))))]
            (if conflicts
              {:txid (.getName (io/file dir)) :conflicts (vec conflicts)
               :paths [] :ok false}
              (restore-begun! dir pins begun temps))))]
     (if transactions-dir
       (file-ops/with-publish-lock* transactions-dir restore!)
       (restore!)))))

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
  ^File [workspace-root txid state-home]
  (io/file (transactions-dir workspace-root state-home) txid))

(defn- read-lease
  "The lease of a retained journal, or the fail-CLOSED stand-in for one that
   cannot be read.

   `(:receipt-refs lease 0)` and `(:evictable lease true)` defaulted a journal
   whose lease was missing to \"nobody holds it, sweep it\" - so deleting one
   file by hand let a quota `evict!` destroy the pre-images and left a
   committed receipt permanently un-undoable. The status survives in
   `state.edn`; the REFCOUNT does not, and an unknown refcount is not zero. A
   lease that cannot be read is therefore an unknown that refuses, and only an
   explicit `forget!` by a caller that PRESENTS the commit receipt may clear
   it."
  [dir]
  (if-let [lease (read-edn-file (io/file dir "lease.edn"))]
    (assoc lease :lease :readable)
    {:lease :unreadable
     :receipt-refs 1
     :evictable false}))

(defn retained-transactions
  ;; @spec MCP-OP-MEM-006
  "Every journal still on disk for this workspace, with its lease - AND every
   break's tombstone beside them.

   What a quota sweep reads. `:evictable false` means the row is a
   `:restore-failed` journal: the tree is not at H0 and this is the only
   material that can repair it.

   The listing used to filter `.isDirectory`, which made `LOCK.broken.*` files
   invisible to it, to the quota and to recovery alike - a receipt nobody
   could read and nobody could retire, accumulating without bound. They are
   rows now, `:kind :broken-lock`, carrying the bytes they bill and the age
   that retires them. `recover!` is what retires one; `evict!` operates on
   journals only, so a tombstone row says `:evictable false` and names what
   does retire it."
  ([workspace-root] (retained-transactions workspace-root {}))
  ([workspace-root {:keys [state-home now-ms]}]
   (let [transactions (transactions-dir workspace-root state-home)
         dir (io/file transactions)
         now (long (or now-ms (System/currentTimeMillis)))
         ;; a tombstone whose break never completed is not one that happened
         interrupted (into {}
                           (map (fn [entry]
                                  [(.getName ^File (nth entry 0)) (nth entry 1)]))
                           (interrupted-break-entries transactions))]
     (into
       (vec (for [^File d (sort-by #(.getName ^File %)
                                   (seq (or (.listFiles dir) (make-array File 0))))
                  :when (.isDirectory d)
                  :let [lease (read-lease d)
                        state (read-edn-file (io/file d "state.edn"))]]
              {:txid (.getName d)
               :kind :transaction
               :status (or (:status lease) (:status state))
               :receipt-refs (:receipt-refs lease 1)
               :evictable (:evictable lease false)
               :lease (:lease lease)
               :bytes (dir-bytes d)}))
       ;; A file that vanished between the listing and the stat is ABSENT:
       ;; `lastModified` of a missing file is 0, which the old row read as
       ;; infinitely old and billed `:bytes 0` - 822 of 9,831 rows under
       ;; concurrency. A PRESENT zero-length one is a different fact and gets
       ;; a row: the break's own receipt names it and the prune counts it in
       ;; `:remaining`, so a listing that denied it made three verbs disagree
       ;; about one file. It carries no claim, and says so in its status.
       (concat
         (for [^File f (broken-lock-files transactions)
               :let [aged (tombstone-age f now)
                     bytes (:bytes aged 0)]
               :when (map? aged)]
           ;; the two typing rules are not equal evidence. Sharing the
           ;; PRESENT LOCK's inode is corroboration by the kernel and types an
           ;; interrupted break; a `:phase :linked` marker the LOCK cannot
           ;; confirm is a claim by the breaker that any directory writer can
           ;; forge, so the file is typed as the break it may well be and the
           ;; status says the marker was not believed.
           (let [cls (get interrupted (.getName f))]
             {:txid (.getName f)
              :kind (if (= :corroborated cls) :interrupted-break :broken-lock)
              :status (case cls
                        :corroborated :lock-break-interrupted
                        :uncorroborated-marker :uncorroborated-marker
                        (if (zero? (long bytes)) :empty-evidence :lock-broken))
              :receipt-refs 0
              :evictable false
              :retired-by :txn/recover
              :age-ms (:age-ms aged)
              :stamp (:stamp aged)
              :retention-ms broken-lock-retention-ms
              :bytes bytes}))
         ;; a stamp sidecar whose tombstone is gone is retired by `recover!`
         ;; on the same published retention, so it is a row on the same terms
         (for [^File f (orphan-sidecar-files transactions)
               :let [aged (evidence-age f f now)]
               :when (map? aged)]
           {:txid (.getName f)
            :kind :orphan-sidecar
            :status :orphan-sidecar
            :receipt-refs 0
            :evictable false
            :retired-by :txn/recover
            :age-ms (:age-ms aged)
            :stamp (:stamp aged)
            :retention-ms broken-lock-retention-ms
            :bytes (:bytes aged)}))))))

(defn undo!
  ;; @spec MCP-OP-MEM-006
  "Restore every path a RETAINED journal wrote, back to its pinned H0 bytes.

   This is what makes a commit receipt a receipt: the transaction directory
   outlives the commit precisely so its answer can be reversed and the
   reversal verified against the digest pinned before the first write.

   Undo is a WRITE, and it goes through the same publish lock and the same
   recheck discipline as the commit that created it: it takes the workspace
   PUBLISH lock, and it refuses - with zero writes - any path whose current
   digest or identity is not what the commit recorded as H1. Republishing H0
   over a later writer's bytes is not an undo, it is a silent clobber."
  ([workspace-root txid] (undo! workspace-root txid {}))
  ([workspace-root txid {:keys [state-home]}]
   (let [dir (journal-dir workspace-root txid state-home)]
     (if-not (.isDirectory dir)
       (refusal :txn-journal-missing
                (str "No retained journal for " txid
                     "; its pre-images cannot be republished")
                {:txid txid :next_call nil
                 :remedy "A journal that was forgotten or evicted cannot be undone. Retain it until the receipt no longer needs to be reversible."})
       (let [result (restore-from-journal!
                      (.getCanonicalPath dir)
                      {:transactions-dir (transactions-dir workspace-root state-home)
                       :expect-post-commit? true})
             conflicts (:conflicts result)]
         (if (seq conflicts)
           (refusal :txn-undo-conflict
                    (str "The current bytes of " (:path (first conflicts))
                         " are not the bytes this commit left behind; undoing"
                         " would clobber a write that landed after it")
                    {:txid txid
                     :path (:path (first conflicts))
                     :conflicts conflicts
                     :files-written 0
                     :next_call nil
                     :remedy "Another writer changed the target after the commit. Reconcile that change first, or forget! this journal deliberately; the pre-images are still here."})
           (assoc result :isolation compact-isolation :undone true)))))))

(defn- presents-receipt?
  "True when the caller handed back the very commit receipt this journal is the
   recovery material for. Holding the receipt is the only evidence available
   that nobody else still needs the pre-images once the lease is unreadable."
  [txid receipt]
  (boolean (and (map? receipt)
                (= txid (:txid receipt))
                (true? (:committed receipt)))))

(defn- discard-journal!
  [workspace-root txid {:keys [state-home receipt]} quota-driven?]
  (let [dir (journal-dir workspace-root txid state-home)
        lease (read-lease dir)
        state (read-edn-file (io/file dir "state.edn"))
        status (or (:status lease) (:status state))]
    (cond
      (not (.isDirectory dir))
      (refusal :txn-journal-missing (str "No retained journal for " txid)
               {:txid txid :next_call nil})

      (and (= :unreadable (:lease lease))
           (or quota-driven? (not (presents-receipt? txid receipt))))
      (refusal :txn-lease-unreadable
               (str "The lease of " txid " is missing or unparsable, so how many"
                    " receipts still hold its pre-images is UNKNOWN")
               {:txid txid
                :lease :unreadable
                :status status
                :receipt-refs 1
                :evictable false
                :next_call {:op :txn/forget :txid txid :receipt :the-commit-receipt}
                :remedy "An unknown refcount is not zero. A quota sweep never reclaims this journal; only an explicit forget! by a caller that presents the commit receipt may."})

      (= :restore-failed status)
      (refusal :txn-journal-retained
               (str "The journal of " txid " records a restoration that did not verify"
                    " and is the only material that can repair the tree")
               {:txid txid
                :status status
                :next_call nil
                :remedy "Repair the tree from this journal - or accept the divergence deliberately - before removing it. A failed restoration is never evicted by a quota."})

      (and quota-driven? (pos? (long (:receipt-refs lease 0))))
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
  "Discard a retained journal deliberately. Refuses a failed restoration, and
   refuses a journal whose lease cannot be read unless the caller presents the
   commit receipt in `:receipt`."
  ([workspace-root txid] (forget! workspace-root txid {}))
  ([workspace-root txid opts]
   (discard-journal! workspace-root txid opts false)))

(defn evict!
  ;; @spec MCP-OP-MEM-006
  "Reclaim a retained journal under quota pressure.

   Stricter than `forget!` on purpose: a sweep that runs because disk is short
   must not silently destroy a receipt somebody is still holding, and must
   never destroy unrepaired recovery material - nor a journal whose refcount
   it cannot read, which no receipt presented to a SWEEP can unlock."
  ([workspace-root txid] (evict! workspace-root txid {}))
  ([workspace-root txid opts]
   (discard-journal! workspace-root txid opts true)))

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

(defn- resolve-interrupted-break!
  "Finish or revert ONE break that never completed, and say which.

   The LOCK decides, and it is re-read here rather than passed in, because
   finishing one interrupted break unlinks it:

   - the LOCK is still that same claim and its holder is DEAD by the ordinary
     rule - the same rule every break obeys, a legacy claim still needing the
     explicit remedy AND a receipt of death: FINISH it. The LOCK side is
     unlinked, the evidence is stamped with its own creation time, and it
     becomes a break that happened.
   - the LOCK is still that same claim and its holder is ALIVE: REVERT. The
     extra link goes, the claim is untouched.
   - the LOCK is gone, or names a different inode: nothing corroborates the
     `:phase :linked` marker that put this file here, and the two states it
     cannot tell apart are a break that never happened and a break that DID.
     KEEP it, stamped with its own creation time and typed
     `:marker-uncorroborated`.

   Every line names the tombstone it acted on and says what became of it -
   `:evidence :retained` for a finish and for an uncorroborated marker,
   `:evidence :removed` for a revert - because a receipt naming a file that is
   not there, with no word about why, is the shape this kernel has had to fix
   twice.

   THE REVERT IS THE NARROW CASE, and it used to be the wide one. It deleted
   the sidecar and the tombstone whenever the LOCK could not confirm the
   match, which made the marker the first mechanism on this branch whose
   FORGERY DESTROYED EVIDENCE: one hand-written `:phase :linked` sidecar
   dropped beside a genuine break - by any writer of the transactions
   directory, or by a stamp write that failed and said nothing - and the next
   `recover!` reverted a completed break, unlinked its evidence and returned
   `:broken-locks {:found 0}`. Round six's worst forgery kept a file for ever,
   which is fail-safe; that one removed it, in the directory whose whole
   purpose is bounded, counted, KEPT evidence. So the revert now happens only
   where the kernel itself corroborates the match - the LOCK is present and IS
   the linked claim, which a completed break can never leave - and a marker
   nobody can corroborate keeps its file. The cost is a duplicate tombstone
   where two interrupted breaks over one claim used to leave one: the first
   finishes it and the rest are kept as uncorroborated, retired on the
   ordinary published retention rather than deleted on a claim we cannot
   check."
  [transactions-dir ^File tomb break-legacy-lock now-ms]
  (let [^File lock (lock-file transactions-dir)
        same-claim? (boolean (when-let [key (lock-file-key lock)]
                               (= key (lock-file-key tomb))))
        claim (when (.isFile lock) (read-lock-claim lock))
        holder (:holder claim)
        cause (when claim (stale-holder holder))
        dead? (boolean
                (and same-claim? cause
                     (or (not= cause :legacy-format)
                         (and break-legacy-lock
                              (legacy-lock-dead? lock holder now-ms)))))]
    (cond
      dead?
      (do (Files/deleteIfExists (.toPath lock))
          (stamp-tombstone! tomb)
          {:tombstone (.getName tomb)
           :resolution :interrupted-break-finished
           ;; the file this line names is on disk when the call returns
           :evidence :retained
           :holder-txid (:txid holder)
           :holder-cause cause})

      ;; the LOCK is present and IS the claim this file is a second link to,
      ;; which is corroboration by the kernel: a completed break unlinks the
      ;; LOCK, so it can never be in this state. Dropping our extra link
      ;; cannot destroy a break that happened.
      same-claim?
      (do (Files/deleteIfExists (.toPath ^File (broken-at-file tomb)))
          (Files/deleteIfExists (.toPath tomb))
          {:tombstone (.getName tomb)
           :resolution :interrupted-break-reverted
           ;; and this line names a file this call DELETED, which is
           ;; said rather than left for a reader to discover: a
           ;; receipt must name its subject, and a name whose file is
           ;; gone must say why it is gone
           :evidence :removed
           :holder-txid (:txid holder)
           :holder-live (boolean (and claim (nil? cause)))
           :lock-present (.isFile lock)})

      ;; a `:phase :linked` marker alone, with no LOCK to confirm it. The
      ;; marker is a claim by the breaker and a claim any directory writer can
      ;; forge is not evidence - but neither is it grounds to delete evidence.
      ;; Stamp it so it is retired on the ordinary retention rather than on
      ;; the age it inherited from the claim, and keep it.
      :else
      (let [stamped (stamp-tombstone! tomb)]
        {:tombstone (.getName tomb)
         :resolution :interrupted-break-uncorroborated
         ;; the file this line names is on disk when the call returns
         :evidence :retained
         :cause :marker-uncorroborated
         :stamp (if stamped :ok :unrecorded)
         :holder-txid (:txid holder)
         :holder-live (boolean (and claim (nil? cause)))
         :lock-present (.isFile lock)}))))

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
   partial failure becomes a permanent one.

   `:break-legacy-lock true` is the remedy for a LOCK an earlier build wrote -
   a pid and no boot id, which nothing can check. It is not a waiver: the break
   still demands a receipt of the holder's death, being that the recorded pid
   names no live process AND the claim is at least `legacy-lock-break-age-ms`
   old, and refuses when either half is missing. `:now-ms` supplies the clock
   that age is measured against, so a witness can pin the boundary exactly."
  ([workspace-root] (recover! workspace-root {}))
  ([workspace-root {:keys [state-home break-legacy-lock now-ms] :as opts}]
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
                         (let [result (restore-from-journal!
                                        (.getCanonicalPath d)
                                        {:transactions-dir transactions})
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
     ;; The lock is a claim by a PROCESS. Recovery releases it whenever no LIVE
     ;; holder exists - including when it recovered nothing, which is exactly
     ;; the stranded case: a crash after the transaction directory was
     ;; finished leaves a LOCK with no journal beside it, and releasing only
     ;; `(when (seq results))` left that workspace deadlocked for ever.
     (let [^File lock (lock-file transactions)
           ;; A crash between the break's `Files/createLink` and its unlink
           ;; leaves one inode under two names. That is neither a break nor a
           ;; free lock, and nothing else in this verb can tell the difference:
           ;; resolve it FIRST, and say which way it went.
           ;; EVERY one of them, in ONE call: the listing used to type the
           ;; first and publish the rest as breaks that happened, and a second
           ;; `recover!` was needed to clear each of them. The LOCK is re-read
           ;; per tombstone because finishing one unlinks it.
           interrupted
           (mapv (fn [^File tomb]
                   (resolve-interrupted-break! transactions tomb
                                               break-legacy-lock now-ms))
                 (interrupted-break-files transactions))
           claim (when (.isFile lock) (read-lock-claim lock))
           holder (:holder claim)
           cause (when claim (stale-holder holder))
           ;; the same compare-and-break the acquisition path uses: recovery
           ;; may not delete a claim that changed under it either
           ;; recovery is the remedy for an UNREADABLE claim as well as a
           ;; provably dead one, so it acts on any cause - but through the same
           ;; compare-and-break: it may not delete a claim that changed under it.
           ;; A LEGACY claim is the one exception: it needs the explicit remedy
           ;; AND the receipt of its holder's death.
           breakable? (and claim cause
                           (or (not= cause :legacy-format)
                               (and break-legacy-lock
                                    (legacy-lock-dead? lock holder now-ms))))
           outcome (when breakable?
                     (do (when-let [hook (:before-break opts)] (hook claim))
                         (break-lock! transactions claim
                                      (str "recover-" (new-txid)) opts)))
           broken (when (:broken outcome)
                    (merge (lock-broken-line holder cause)
                           ;; the same :break-path the acquisition path
                           ;; publishes: which primitive took the claim is
                           ;; part of the break's receipt, not an internal
                           (select-keys outcome [:tombstone :content-sha256
                                                 :break-path])))
           ;; a break that moved a claim and could not put it back is a
           ;; refusal with an owner, not a silent success
           displaced (displaced-line outcome)
           ;; and a break refused because the name was taken is a refusal
           ;; nobody heard: recovery dropped it entirely, so the one verb
           ;; whose job is to clear a stale lock returned an ordinary success
           ;; having broken nothing
           refused-break (when (contains? break-refusal-causes (:cause outcome))
                           (select-keys outcome [:cause :tombstone]))]
       (cond-> {:ok (every? :ok results)
                :transactions-recovered (count results)
                :isolation compact-isolation
                :paths (vec (mapcat :paths results))
                ;; the break's evidence is bounded and counted: a bucket that
                ;; nothing sweeps and nothing reports is accumulation, not
                ;; durability. Pruned AFTER this run's own break, so a
                ;; tombstone made a moment ago is never retired by the
                ;; recovery that made it.
                :broken-locks (prune-broken-locks! transactions now-ms)}
         (seq interrupted) (assoc :interrupted-breaks interrupted)
         broken (assoc :lock-broken broken)
         displaced (assoc :lock-break-displaced displaced)
         refused-break (assoc :lock-break-refused refused-break))))))
