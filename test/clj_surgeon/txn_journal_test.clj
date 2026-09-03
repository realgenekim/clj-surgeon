(ns clj-surgeon.txn-journal-test
  "Witnesses for the disk-journaled transaction kernel.

   Every ceiling witness has Sol's shape: the request exactly at the limit
   succeeds, and the request one unit past it refuses BEFORE the effect the
   limit exists to bound. Every conflict witness asserts the observable
   outcome a caller sees - bytes on disk and a typed error - never an internal
   call."
  (:require
   [clj-surgeon.file-ops :as file-ops]
   [clj-surgeon.memory.child :as child]
   [clj-surgeon.scope-stream :as scope]
   [clj-surgeon.txn-journal :as journal]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [clojure.walk :as walk])
  (:import
   (java.nio.file Files)))

;; ---------------------------------------------------------------- helpers

(defn- temp-dir
  [label]
  (let [dir (io/file (or (System/getenv "CLJ_SURGEON_MEMORY_TMP") "/home/forge/tmp")
                     (str "clj-surgeon-txn-" label "-"
                          (System/currentTimeMillis) "-"
                          (long (rand 1000000))))]
    (.mkdirs dir)
    (.getCanonicalPath dir)))

(defn- delete-tree!
  [root]
  (let [file (io/file root)]
    (when (.exists file)
      (doseq [child (reverse (file-seq file))]
        (Files/deleteIfExists (.toPath child))))))

(defn- write-file!
  [root relative content]
  (let [target (io/file root relative)]
    (.mkdirs (.getParentFile target))
    (spit target content)
    (.getCanonicalPath target)))

(defn- workspace!
  "A workspace with `n` numbered source files plus an isolated state home."
  [label n]
  (let [root (temp-dir label)
        state (temp-dir (str label "-state"))
        paths (vec (for [i (range n)]
                     (write-file! root (format "src/f%03d.clj" i)
                                  (str "(ns f" i ") (def v " i ")\n"))))]
    {:root root :state-home state :paths paths}))

(defn- cleanup!
  [{:keys [root state-home]}]
  (delete-tree! root)
  (delete-tree! state-home))

(defn- begin!
  [{:keys [root state-home]} opts]
  (journal/begin! root (merge {:state-home state-home} opts)))

(defn- record-scope!
  "Record every path in sorted order as the transaction's read set."
  [txn paths]
  (reduce (fn [acc path]
            (if (:ok acc)
              (journal/record-read! txn path)
              (reduced acc)))
          {:ok true}
          (sort paths)))

(defn- bytes-of [path] (slurp path))

(defn- boot-id-now
  []
  (let [f (io/file "/proc/sys/kernel/random/boot_id")]
    (when (.isFile f) (str/trim (slurp f)))))

(defn- plant-lock!
  "Write a project LOCK naming `holder`, the way a crashed process leaves one."
  ^java.io.File [{:keys [root state-home]} holder]
  (let [dir (io/file (journal/transactions-dir root state-home))
        lock (io/file dir "LOCK")]
    (.mkdirs dir)
    (spit lock (pr-str holder))
    lock))

(defn- lock-age-basis
  "The stamp the legacy break measures a claim's age against: the NEWEST of
   the lock's mtime and ctime.

   A witness pins that boundary with the CLOCK - `recover!`'s `:now-ms` seam -
   and never by back-dating the file, because back-dating mtime is exactly the
   forgery the newest-of-two rule exists to refuse, and ctime cannot be set
   through the filesystem API at all."
  [^java.io.File lock]
  (let [ctime (.toMillis ^java.nio.file.attribute.FileTime
                (Files/getAttribute (.toPath lock) "unix:ctime"
                                    ^"[Ljava.nio.file.LinkOption;"
                                    (into-array java.nio.file.LinkOption
                                                [java.nio.file.LinkOption/NOFOLLOW_LINKS])))]
    (max (.lastModified lock) ctime)))

(defn- file-key
  "A file's (device, inode) identity, the way the kernel reads it."
  [^java.io.File f]
  (str (.fileKey (Files/readAttributes
                   (.toPath f)
                   java.nio.file.attribute.BasicFileAttributes
                   ^"[Ljava.nio.file.LinkOption;"
                   (into-array java.nio.file.LinkOption
                               [java.nio.file.LinkOption/NOFOLLOW_LINKS])))))

(defn- reaped-pid
  "A pid that certainly named a process and certainly names none now."
  []
  (let [process (.start (ProcessBuilder. ["sleep" "0"]))]
    (.waitFor process)
    (.pid process)))

(defn- hold-publish-lock!
  "Spawn a SEPARATE JVM holding this workspace's PUBLISH.lock for `hold-ms`.

   Returns once the child has actually taken the lock, so the caller's clock
   starts when the contention is real rather than when the process was asked
   for."
  [{:keys [root state-home]} hold-ms]
  (let [command [(str (io/file (System/getProperty "java.home") "bin" "java"))
                 "-cp" (System/getProperty "java.class.path")
                 "clojure.main" "-m" "clj-surgeon.txn-lock-child"
                 (journal/transactions-dir root state-home)
                 (str hold-ms)]
        process (.start (ProcessBuilder. ^java.util.List (vec command)))
        ^java.io.BufferedReader reader (io/reader (.getInputStream process))]
    (loop [line (.readLine reader)]
      (cond
        (nil? line) (throw (ex-info "the lock child died before it held the lock" {}))
        (= "HELD" line) process
        :else (recur (.readLine reader))))))

(defn- commit-one-change!
  "Commit one edit to `path` and return {:receipt r :h0 s}."
  [ws path future-source]
  (let [h0 (bytes-of path)
        txn (begin! ws {})]
    (record-scope! txn (:paths ws))
    (journal/seal-read-set! txn)
    (journal/pin! txn path)
    (journal/stage! txn path future-source)
    {:receipt (journal/commit! txn) :h0 h0}))

;; ------------------------------------------------- MCP-OP-MEM-006 ceilings

;; @spec MCP-OP-MEM-006
(deftest the-default-journal-quota-admits-what-the-default-read-path-admits
  (testing "Sol's finding: the RED/GREEN workload needed a 2 GiB override
            because the DEFAULT journal quota refused it. Two default ceilings
            that cannot both be satisfied is a ceiling set that has never been
            derived. A journal holds one PRE-image and one FUTURE image of every
            byte the read path admitted, so the rule is exact."
    (let [aggregate (:max-aggregate-bytes scope/default-limits)
          quota (:max-journal-bytes journal/default-limits)]
      (is (>= quota (* 2 aggregate))
          (str "the default journal quota (" quota " B) must admit two images of "
               "the default aggregate read ceiling (" aggregate " B), or a scope "
               "the reader accepts is one the journal refuses to stage"))
      (is (>= (:max-journal-bytes journal/hard-limits) quota)
          "and the default may never exceed the server hard maximum"))))

;; ------------------------------------------------- MCP-OP-MEM-014 contract

;; @spec MCP-OP-MEM-014
(deftest contract-states-optimistic-serializability-not-snapshot-isolation
  (testing "the kernel names what it guarantees and what it refuses to claim"
    (let [contract (journal/contract)]
      (is (= :optimistic-serializability (:isolation contract)))
      (is (false? (:snapshot-isolation contract)))
      (is (some #(str/includes? % "ignores the lock") (:does-not-promise contract))
          "the honest limit is stated, not implied")
      (is (contains? (set (:detects contract)) :read-set-modification))
      (is (contains? (set (:detects contract)) :read-back-mismatch)))))

;; @spec MCP-OP-MEM-014
(deftest every-receipt-carries-the-isolation-contract
  (let [ws (workspace! "isolation-receipt" 2)]
    (try
      (let [txn (begin! ws {})
            _ (record-scope! txn (:paths ws))
            _ (journal/seal-read-set! txn)
            path (first (sort (:paths ws)))
            _ (journal/pin! txn path)
            _ (journal/stage! txn path "(ns f0) (def v :changed)\n")
            receipt (journal/commit! txn)]
        (is (:ok receipt))
        (is (= :optimistic-serializability (get-in receipt [:isolation :isolation])))
        (is (false? (get-in receipt [:isolation :snapshot-isolation])))
        (is (> 4096 (count (pr-str receipt)))
            "the receipt is bounded and carries no file contents")
        (is (pos? (get-in receipt [:reserved :journal-bytes-peak]))
            "the receipt reports the admission accountant's peak reservation so
             a battery can read reserved-peak instead of reporting UNMEASURED")
        (is (= (:max-journal-bytes journal/default-limits)
               (get-in receipt [:reserved :journal-bytes-max]))))
      (finally (cleanup! ws)))))

;; @spec MCP-OP-MEM-014
(deftest a-writer-that-ignores-the-lock-is-detected-not-prevented
  (testing "the honest limit: an external write between rename and read-back is
            caught by read-back verification and rolled back to H0"
    (let [ws (workspace! "external-writer" 2)
          [p0 p1] (sort (:paths ws))]
      (try
        (let [h0 (bytes-of p0)
              txn (begin! ws {})
              _ (record-scope! txn (:paths ws))
              _ (journal/seal-read-set! txn)
              _ (journal/pin! txn p0)
              _ (journal/stage! txn p0 "(ns f0) (def v :ours)\n")
              ;; a writer that never took the lock lands between our rename and
              ;; our read-back
              receipt (journal/commit!
                        txn {:after-publish (fn [path]
                                              (when (= path p0)
                                                (spit path "(ns f0) (def v :theirs)\n")))})]
          (is (false? (:ok receipt)))
          (is (= :txn-read-back-mismatch (:error-type receipt)))
          (is (= p0 (:path receipt)))
          (is (true? (:rolled-back receipt)))
          (is (= h0 (bytes-of p0))
              "the pinned pre-image is restored; the racing write is lost, and
               the contract says so rather than claiming it was prevented")
          (is (= (slurp p1) (bytes-of p1))))
        (finally (cleanup! ws))))))

;; @spec MCP-OP-MEM-007
;; @spec MCP-OP-MEM-014
(deftest a-writer-that-lands-before-the-pre-image-recheck-is-refused
  (testing "Sol's injection, moved to the only place the transaction can still
            refuse: the staged bytes are already copied into the target's own
            directory, and the writer lands BEFORE the pre-image recheck. The
            expensive half of publication is outside the window, so this is
            detected and nothing is written."
    (let [ws (workspace! "recheck-window" 3)
          paths (sort (:paths ws))
          victim (first paths)]
      (try
        (let [txn (begin! ws {})
              _ (record-scope! txn paths)
              _ (journal/seal-read-set! txn)
              _ (journal/pin! txn victim)
              _ (journal/stage! txn victim "(ns f0) (def v :ours)\n")
              receipt (journal/commit!
                        txn {:before-recheck
                             (fn [path]
                               (when (= path victim)
                                 (spit victim "(ns f0) (def v :theirs)\n")))})]
          (is (false? (:ok receipt)))
          (is (= :txn-conflict (:error-type receipt)))
          (is (= victim (:path receipt)))
          (is (= 0 (:files-written receipt))
              "the racing writer is detected before the rename, not after it")
          (is (= "(ns f0) (def v :theirs)\n" (bytes-of victim))
              "the other writer's bytes survive; this transaction wrote none"))
        (finally (cleanup! ws))))))

;; @spec MCP-OP-MEM-007
;; @spec MCP-OP-MEM-014
(deftest the-residual-recheck-to-rename-window-is-reported-not-hidden
  (testing "atomic rename is not compare-and-swap. A writer that ignores the
            publish lock and lands INSIDE the recheck-to-rename window is
            overwritten. The kernel does not pretend otherwise: the window's
            bound is named in the contract and carried in every receipt."
    (let [ws (workspace! "commit-window" 2)
          paths (sort (:paths ws))
          victim (first paths)]
      (try
        (let [txn (begin! ws {})
              _ (record-scope! txn paths)
              _ (journal/seal-read-set! txn)
              _ (journal/pin! txn victim)
              _ (journal/stage! txn victim "(ns f0) (def v :ours)\n")
              receipt (journal/commit!
                        txn {:in-commit-window
                             (fn [path]
                               (when (= path victim)
                                 (spit victim "(ns f0) (def v :theirs)\n")))})]
          (is (:ok receipt) "the window is real: the write inside it is not caught")
          (is (= "(ns f0) (def v :ours)\n" (bytes-of victim))
              "and the racing bytes are lost, which is what the receipt must say")
          (is (= [:recheck-stat :recheck-identity :journal-write-begin :rename]
                 (get-in receipt [:commit-window :ops]))
              "the receipt names every operation inside the window")
          (is (false? (get-in receipt [:commit-window :staging-copy-inside]))
              "the staged bytes are copied into the target directory BEFORE the
               window opens, so no byte copying happens inside it")
          (is (pos? (get-in receipt [:commit-window :max-ns]))
              "the widest observed window is measured, not asserted")
          (is (some #(str/includes? % "recheck")
                    (:does-not-promise (journal/contract)))
              "the contract states the window it cannot close"))
        (finally (cleanup! ws))))))

;; ------------------------------------------------- MCP-OP-MEM-012 retention

;; @spec MCP-OP-MEM-007
(deftest the-commit-window-holds-no-full-file-read
  (testing "Opus round 2 on the window: the digest recheck INSIDE the lock was
            a full re-read of the target, so the residual window scaled with
            file size - 846 us measured at 1 KB, 3.0 ms at 2 MiB. `:staging-copy-inside
            false` was literally true and still hid an O(size) read. The digest
            is now taken BEFORE the lock, and inside the lock only the NOFOLLOW
            stat is compared."
    (let [ws (workspace! "window-o1" 1)
          path (first (:paths ws))
          big (apply str (repeat (* 2 1024 1024) \x))]
      (try
        (spit path (str ";" big "\n"))
        (let [txn (begin! ws {})
              _ (record-scope! txn [path])
              _ (journal/seal-read-set! txn)
              _ (journal/pin! txn path)
              _ (journal/stage! txn path (str ";" big "\n;; changed\n"))
              receipt (journal/commit! txn)
              window (:commit-window receipt)]
          (is (:ok receipt))
          (is (= [:recheck-stat :recheck-identity :journal-write-begin :rename]
                 (:ops window))
              "no digest read is named inside the window any more")
          (is (true? (:digest-computed-before-lock window)))
          (is (= [:kind :file-key :size :mtime-ns :ctime-ns] (:stat-fields window))
              "and the receipt names exactly what the in-lock comparison reads")
          (is (= 0 (:digest-rereads receipt))
              "an uncontended commit re-reads nothing inside the lock, whatever
               the target's size - which is the whole claim")
          (is (pos? (:max-ns window))))
        (finally (cleanup! ws))))))

;; @spec MCP-OP-MEM-007
(deftest a-same-size-write-before-the-recheck-is-still-refused
  (testing "the guard on the O(1) recheck: moving the digest outside the lock
            is only sound if the in-lock stat can tell that the target moved.
            This injection replaces the target with bytes of EXACTLY the same
            length through the same inode, so size and file identity are
            unchanged and only the timestamps differ. It must still refuse.

            Green on both sides of the change by construction - before it, the
            in-lock full re-read caught this; after it, the stat comparison
            must."
    (let [ws (workspace! "same-size-recheck" 2)
          paths (sort (:paths ws))
          victim (first paths)]
      (try
        (let [txn (begin! ws {})
              _ (record-scope! txn paths)
              _ (journal/seal-read-set! txn)
              _ (journal/pin! txn victim)
              _ (journal/stage! txn victim "(ns f0) (def v :ours)\n")
              receipt (journal/commit!
                        txn {:before-recheck
                             (fn [path]
                               (when (= path victim)
                                 (spit victim "(ns f0) (def v :thrs)\n")))})]
          (is (false? (:ok receipt)))
          (is (= :txn-conflict (:error-type receipt)))
          (is (= :digest (:conflict receipt)))
          (is (= 0 (:files-written receipt)))
          (is (= "(ns f0) (def v :thrs)\n" (bytes-of victim))
              "the same-length write survives; this transaction wrote none"))
        (finally (cleanup! ws))))))

;; @spec MCP-OP-MEM-012
(deftest the-read-set-is-streamed-to-disk-and-never-retained
  (testing "20000 recorded read-set entries produce 20000 manifest lines and
            zero resident per-path records"
    (let [ws (workspace! "retention" 1)]
      (try
        (let [txn (begin! ws {:max-read-set-files 30000})
              content "(ns x)\n"
              path (first (:paths ws))]
          (dotimes [i 20000]
            (journal/record-read!
              txn {:path (format "/synthetic/%06d.clj" i)
                   :bytes (count content)
                   :sha256 (journal/sha256-string content)
                   :mode "rw-r--r--"}))
          (journal/seal-read-set! txn)
          (is (= 20000 (journal/manifest-line-count txn)))
          (is (= 0 (journal/retained-record-count txn))
              "no per-path record of the read set stays in the transaction value")
          (is (= 0 (journal/retained-content-bytes txn))
              "no source text of any kind stays in the transaction value")
          (journal/rollback! txn)
          (is (= "(ns f0) (def v 0)\n" (bytes-of path))))
        (finally (cleanup! ws))))))

;; @spec MCP-OP-MEM-012
(deftest staged-future-source-lives-on-disk-not-in-the-transaction
  (let [ws (workspace! "no-future-map" 6)]
    (try
      (let [txn (begin! ws {})
            _ (record-scope! txn (:paths ws))
            _ (journal/seal-read-set! txn)]
        (doseq [path (sort (:paths ws))]
          (journal/pin! txn path)
          (journal/stage! txn path (str (slurp path) ";; staged\n")))
        (is (= 0 (journal/retained-content-bytes txn))
            "the future source of six staged files is zero resident bytes")
        (is (= 6 (journal/staged-file-count txn)))
        (let [receipt (journal/commit! txn)]
          (is (:ok receipt))
          (is (every? #(str/ends-with? (slurp %) ";; staged\n") (:paths ws)))))
      (finally (cleanup! ws)))))

;; @spec MCP-OP-MEM-012
(deftest the-read-set-ceiling-admits-exactly-its-limit-and-refuses-the-next
  (testing "exactly F recorded; F+1 refuses before the manifest grows"
    (let [ws (workspace! "read-set-ceiling" 1)]
      (try
        (let [txn (begin! ws {:max-read-set-files 4})
              entry (fn [i] {:path (format "/synthetic/%03d.clj" i)
                             :bytes 8 :sha256 (journal/sha256-string "x")
                             :mode "rw-r--r--"})
              at-limit (mapv #(journal/record-read! txn (entry %)) (range 4))
              past (journal/record-read! txn (entry 4))]
          (is (every? :ok at-limit) "exactly four is admitted")
          (is (false? (:ok past)))
          (is (= :txn-read-set-too-large (:error-type past)))
          (is (= 4 (:max-files past)))
          (is (some? (:next_call past)) "the refusal names a narrowing next call")
          (is (= 4 (journal/manifest-line-count txn))
              "the refused entry never reached the manifest")
          (journal/rollback! txn))
        (finally (cleanup! ws))))))

;; @spec MCP-OP-MEM-012
(deftest the-manifest-refuses-an-unsorted-read-set
  (let [ws (workspace! "manifest-order" 1)]
    (try
      (let [txn (begin! ws {})
            e (fn [p] {:path p :bytes 1 :sha256 (journal/sha256-string "x")
                       :mode "rw-r--r--"})
            first-ok (journal/record-read! txn (e "/b.clj"))
            backwards (journal/record-read! txn (e "/a.clj"))]
        (is (:ok first-ok))
        (is (= :txn-manifest-unsorted (:error-type backwards))
            "a sorted manifest written as it streams cannot accept a step back")
        (journal/rollback! txn))
      (finally (cleanup! ws)))))

;; ---------------------------------------------------- MCP-OP-MEM-006 pinning

;; @spec MCP-OP-MEM-006
(deftest an-unpinned-write-is-refused-before-any-byte-changes
  (let [ws (workspace! "unpinned" 3)
        [p0 p1] (sort (:paths ws))]
    (try
      (let [before (mapv bytes-of (:paths ws))
            txn (begin! ws {})
            _ (record-scope! txn (:paths ws))
            _ (journal/seal-read-set! txn)
            _ (journal/pin! txn p0)
            _ (journal/stage! txn p0 "(ns f0) (def v :a)\n")
            ;; staged without pinning: the pre-image has no durable copy
            _ (journal/stage! txn p1 "(ns f1) (def v :b)\n")
            receipt (journal/commit! txn)]
        (is (false? (:ok receipt)))
        (is (= :txn-unpinned-write (:error-type receipt)))
        (is (= p1 (:path receipt)))
        (is (= 0 (:files-written receipt)) "the refusal precedes the first write")
        (is (= before (mapv bytes-of (:paths ws)))
            "every byte on disk is unchanged"))
      (finally (cleanup! ws)))))

;; @spec MCP-OP-MEM-006
(deftest the-journal-quota-admits-exactly-its-limit-and-restores-on-injected-failure
  (testing "Sol's witness: pinned plus staged bytes exactly equal to quota Q
            complete even when the last write is injected to fail, and every
            file is restored to H0"
    (let [ws (workspace! "quota-exact" 3)
          paths (sort (:paths ws))]
      (try
        (let [h0 (mapv bytes-of paths)
              future-source (mapv #(str (slurp %) ";; q\n") paths)
              quota (+ (reduce + (map #(count (.getBytes ^String % "UTF-8")) h0))
                       (reduce + (map #(count (.getBytes ^String % "UTF-8"))
                                      future-source)))
              txn (begin! ws {:max-journal-bytes quota})
              _ (record-scope! txn paths)
              _ (journal/seal-read-set! txn)
              pins (mapv #(journal/pin! txn %) paths)
              stages (mapv (fn [p s] (journal/stage! txn p s)) paths future-source)
              last-path (last paths)
              receipt (journal/commit!
                        txn {:publish-fn
                             (fn [target source]
                               (if (= target last-path)
                                 (throw (ex-info "injected write failure"
                                                 {:error-type :injected}))
                                 (journal/publish-file! target source)))})]
          (is (every? :ok pins) "every pre-image fits exactly inside the quota")
          (is (every? :ok stages) "every future file fits exactly inside the quota")
          (is (= quota (journal/journal-bytes txn))
              "the quota is consumed exactly, not approximately")
          (is (false? (:ok receipt)))
          (is (= :txn-write-failed (:error-type receipt)))
          (is (= :injected (:cause-error-type receipt)))
          (is (true? (:rolled-back receipt)))
          (is (= h0 (mapv bytes-of paths))
              "every file is restored to its H0 bytes")
          (is (every? true? (map (fn [p h] (= (journal/sha256-string h)
                                              (journal/sha256-file p)))
                                 paths h0))
              "restoration is verified by digest, not assumed"))
        (finally (cleanup! ws))))))

;; @spec MCP-OP-MEM-006
(deftest one-byte-past-the-journal-quota-refuses-with-zero-writes
  (let [ws (workspace! "quota-past" 3)
        paths (sort (:paths ws))]
    (try
      (let [h0 (mapv bytes-of paths)
            future-source (mapv #(str (slurp %) ";; q\n") paths)
            quota (dec (+ (reduce + (map #(count (.getBytes ^String % "UTF-8")) h0))
                          (reduce + (map #(count (.getBytes ^String % "UTF-8"))
                                         future-source))))
            txn (begin! ws {:max-journal-bytes quota})
            _ (record-scope! txn paths)
            _ (journal/seal-read-set! txn)
            results (doall
                      (for [[p s] (map vector paths future-source)
                            r [(journal/pin! txn p) (journal/stage! txn p s)]]
                        r))
            refusal (first (remove :ok results))]
        (is (some? refusal) "one byte past the quota must refuse")
        (is (= :txn-journal-quota-exceeded (:error-type refusal)))
        (is (= quota (:max-bytes refusal)))
        (is (some? (:next_call refusal)))
        (is (= h0 (mapv bytes-of paths)) "zero writes reached the workspace"))
      (finally (cleanup! ws)))))

;; ------------------------------------------- MCP-OP-MEM-006 confinement

;; @spec MCP-OP-MEM-006
(deftest a-path-outside-the-workspace-can-be-neither-pinned-nor-staged
  (testing "the journal takes absolute paths, so confinement is not implied by
            the caller: pin and stage route to the same mcp-paths resolver every
            other write surface uses"
    (let [ws (workspace! "confinement" 2)
          outside (workspace! "confinement-outside" 1)
          victim (first (:paths outside))]
      (try
        (let [before (bytes-of victim)
              txn (begin! ws {})
              pinned (journal/pin! txn victim)
              staged (journal/stage! txn victim "(ns evil)\n")
              inside (journal/pin! txn (first (sort (:paths ws))))]
          (is (false? (:ok pinned)))
          (is (= :txn-path-outside-workspace (:error-type pinned)))
          (is (false? (:ok staged)))
          (is (= :txn-path-outside-workspace (:error-type staged)))
          (is (:ok inside) "a path inside the workspace is still admitted")
          (is (= 0 (journal/staged-file-count txn)))
          (is (= before (bytes-of victim)))
          (journal/rollback! txn))
        (finally (cleanup! ws) (cleanup! outside))))))

;; @spec MCP-OP-MEM-006
(deftest a-path-with-a-parent-traversal-segment-is-refused-before-canonicalisation
  (testing "Sol's finding: `<root>/src/../src/f000.clj` was PINNED, because
            getCanonicalPath deletes the `..` before the resolver that rejects
            traversal ever sees it. Canonicalisation is not a confinement check;
            it is what hides the need for one. The lexical refusal comes first."
    (let [ws (workspace! "lexical-traversal" 2)
          inside (first (sort (:paths ws)))
          traversing (str (:root ws) "/src/../src/"
                          (.getName (io/file inside)))]
      (try
        (let [before (bytes-of inside)
              txn (begin! ws {})
              pinned (journal/pin! txn traversing)
              staged (journal/stage! txn traversing "(ns evil)\n")]
          (is (false? (:ok pinned)))
          (is (= :txn-path-outside-workspace (:error-type pinned)))
          (is (= :lexical-parent-traversal (:cause pinned))
              "the refusal names the lexical rule, not a canonical accident")
          (is (false? (:ok staged)))
          (is (= :lexical-parent-traversal (:cause staged)))
          (is (= 0 (journal/staged-file-count txn))
              "the traversing path reached neither the object store nor staging")
          (is (= before (bytes-of inside)))
          (is (:ok (journal/pin! txn inside))
              "the same file, named without traversal, is still admitted")
          (journal/rollback! txn))
        (finally (cleanup! ws))))))

(defn- swap-for-symlink!
  "Replace `path` with a symbolic link to `twin`, which holds identical bytes."
  [path twin]
  (Files/delete (.toPath (io/file path)))
  (Files/createSymbolicLink (.toPath (io/file path))
                            (.toPath (io/file twin))
                            (make-array java.nio.file.attribute.FileAttribute 0)))

;; @spec MCP-OP-MEM-006
;; @spec MCP-OP-MEM-007
(deftest a-pinned-file-replaced-by-a-symlink-to-identical-bytes-is-a-conflict
  (testing "Sol's finding: content is not identity. A regular file swapped for a
            symbolic link to the same bytes has the same digest, passed
            revalidation, and the commit replaced the LINK. The pinned NOFOLLOW
            type and file identity are what tell the two apart."
    (let [ws (workspace! "identity-revalidate" 2)
          victim (first (sort (:paths ws)))
          twin (write-file! (:root ws) "src/twin.clj" (slurp victim))]
      (try
        (let [txn (begin! ws {})
              _ (record-scope! txn [victim])
              _ (journal/seal-read-set! txn)
              pinned (journal/pin! txn victim)
              _ (journal/stage! txn victim "(ns f0) (def v :ours)\n")
              _ (swap-for-symlink! victim twin)
              after-swap (journal/sha256-file victim)
              receipt (journal/commit! txn)]
          (is (:ok pinned))
          (is (= after-swap (:sha256 pinned))
              "the bytes read through the link are identical to the pinned bytes:
               only identity has changed, and a digest cannot see it")
          (is (false? (:ok receipt)))
          (is (= :txn-conflict (:error-type receipt)))
          (is (= :identity-changed (:conflict receipt)))
          (is (= :regular (get-in receipt [:expected-identity :kind])))
          (is (= :symlink (get-in receipt [:actual-identity :kind])))
          (is (= 0 (:files-written receipt)))
          (is (= (slurp twin) (slurp victim))
              "the link and its target are untouched"))
        (finally (cleanup! ws))))))

;; @spec MCP-OP-MEM-007
(deftest a-file-swapped-for-a-symlink-inside-the-commit-loop-is-a-conflict
  (testing "identity is rechecked under the publish lock immediately before the
            rename, not only at revalidation"
    (let [ws (workspace! "identity-recheck" 2)
          victim (first (sort (:paths ws)))
          twin (write-file! (:root ws) "src/twin.clj" (slurp victim))]
      (try
        (let [txn (begin! ws {})
              _ (record-scope! txn [victim])
              _ (journal/seal-read-set! txn)
              _ (journal/pin! txn victim)
              _ (journal/stage! txn victim "(ns f0) (def v :ours)\n")
              receipt (journal/commit!
                        txn {:before-recheck
                             (fn [path]
                               (when (= path victim)
                                 (swap-for-symlink! victim twin)))})]
          (is (false? (:ok receipt)))
          (is (= :txn-conflict (:error-type receipt)))
          (is (= :identity-changed (:conflict receipt)))
          (is (= 0 (:files-written receipt)))
          (is (= (slurp twin) (slurp victim))
              "neither the link nor the file it points at was replaced")
          (is (= [:recheck-stat :recheck-identity :journal-write-begin :rename]
                 (get-in (journal/contract) [:commit-window :ops]))
              "the identity recheck is inside the window the contract names"))
        (finally (cleanup! ws))))))

;; ------------------------------------------- MCP-OP-MEM-007 lock + read set

;; @spec MCP-OP-MEM-007
(deftest the-project-lock-admits-one-transaction
  (let [ws (workspace! "lock" 2)]
    (try
      (let [held (begin! ws {})
            second-attempt (begin! ws {})]
        (is (string? (:txid held)))
        (is (false? (:ok second-attempt)))
        (is (= :txn-lock-held (:error-type second-attempt)))
        (is (= (:txid held) (:holder-txid second-attempt))
            "the refusal names the holder")
        (journal/rollback! held)
        (is (:txid (begin! ws {}))
            "the lock is released when the transaction ends"))
      (finally (cleanup! ws)))))

;; @spec MCP-OP-MEM-013
(deftest a-lock-left-by-a-dead-holder-is-broken-once-and-named
  (testing "Opus round 2, blocker 1: a LOCK whose process is gone deadlocked
            the workspace PERMANENTLY. Nothing read the recorded pid back, so
            `begin!` refused for ever and `recover!` - the remedy the refusal
            itself named - returned ok with the lock still in place."
    (let [ws (workspace! "stale-lock" 2)]
      (try
        (let [dead (reaped-pid)
              lock (plant-lock! ws {:txid "ghost-1" :pid dead :boot-id (boot-id-now)})
              txn (begin! ws {})]
          (is (string? (:txid txn))
              "a dead holder's lock does not deadlock the workspace")
          (is (= :stale-holder (get-in txn [:lock-broken :reason])))
          (is (= :process-not-alive (get-in txn [:lock-broken :cause])))
          (is (= dead (get-in txn [:lock-broken :pid]))
              "the receipt names the pid whose lock it broke")
          (is (= "ghost-1" (get-in txn [:lock-broken :holder-txid])))
          (is (str/includes? (slurp (io/file (:dir txn) "journal.log")) "lock-broken\t")
              "and the break is a durable journal line, not only a return value")
          (journal/rollback! txn)
          (is (not (.isFile lock)) "the transaction that broke it also released it"))
        (finally (cleanup! ws))))))

;; @spec MCP-OP-MEM-013
(deftest a-lock-held-by-a-live-process-is-never-broken
  (testing "the other half: breaking a stale lock must not become breaking
            ANY lock. A real child process holds this one."
    (let [ws (workspace! "live-lock" 2)
          child (.start (ProcessBuilder. ["sleep" "30"]))]
      (try
        (let [lock (plant-lock! ws {:txid "live-1" :pid (.pid child)
                                    :boot-id (boot-id-now)})
              refused (begin! ws {})]
          (is (false? (:ok refused)))
          (is (= :txn-lock-held (:error-type refused)))
          (is (= (.pid child) (:holder-pid refused)))
          (is (true? (:holder-live refused)))
          (is (.isFile lock) "a live holder's lock survives the attempt")
          (let [recovery (journal/recover! (:root ws) {:state-home (:state-home ws)})]
            (is (nil? (:lock-broken recovery))
                "and recovery does not break it either")
            (is (.isFile lock))))
        (finally
          (.destroyForcibly child)
          (.waitFor child)
          (cleanup! ws))))))

;; @spec MCP-OP-MEM-013
(deftest a-lock-whose-start-ticks-do-not-match-its-live-pid-is-stale
  (testing "a pid is unique only within one boot and is reused. A LOCK naming
            a pid that IS alive but did not start when the lock says is a
            recycled number, not a holder."
    (let [ws (workspace! "recycled-pid" 2)
          child (.start (ProcessBuilder. ["sleep" "30"]))]
      (try
        (let [_ (plant-lock! ws {:txid "recycled-1" :pid (.pid child)
                                 :start-ticks 1
                                 :boot-id (boot-id-now)})
              txn (begin! ws {})]
          (is (string? (:txid txn)))
          (is (= :start-ticks-mismatch (get-in txn [:lock-broken :cause])))
          (journal/rollback! txn))
        (finally
          (.destroyForcibly child)
          (.waitFor child)
          (cleanup! ws))))))

;; @spec MCP-OP-MEM-013
(deftest recovery-releases-a-lock-whose-holder-is-gone-even-with-nothing-to-recover
  (testing "Opus's exact reproduction: a LOCK naming a dead pid with NO
            transaction directory beside it. `recover!` released only
            `(when (seq results))`, so the one case that strands a workspace
            was the one case recovery did not clear."
    (let [ws (workspace! "stale-lock-recover" 2)]
      (try
        (let [dead (reaped-pid)
              lock (plant-lock! ws {:txid "ghost-2" :pid dead :boot-id (boot-id-now)})
              recovery (journal/recover! (:root ws) {:state-home (:state-home ws)})]
          (is (:ok recovery))
          (is (= 0 (:transactions-recovered recovery))
              "there is nothing to recover; the lock is the whole problem")
          (is (= :stale-holder (get-in recovery [:lock-broken :reason])))
          (is (= dead (get-in recovery [:lock-broken :pid])))
          (is (not (.isFile lock)))
          (let [txn (begin! ws {})]
            (is (string? (:txid txn)) "the workspace is usable again")
            (journal/rollback! txn)))
        (finally (cleanup! ws))))))

;; @spec MCP-OP-MEM-007
(deftest a-changed-read-only-file-refuses-the-whole-transaction
  (testing "with exactly the maximum read set, changing the FINAL read-only
            file yields zero writes and a typed conflict naming that path"
    (let [ws (workspace! "read-set-conflict" 5)
          paths (sort (:paths ws))
          write-target (first paths)
          last-read-only (last paths)]
      (try
        (let [h0 (mapv bytes-of paths)
              txn (begin! ws {:max-read-set-files 5})
              recorded (record-scope! txn paths)
              _ (journal/seal-read-set! txn)
              _ (journal/pin! txn write-target)
              _ (journal/stage! txn write-target "(ns f0) (def v :new)\n")
              ;; a file nobody writes, but whose facts shaped the plan
              _ (spit last-read-only "(ns f4) (def v :drifted)\n")
              receipt (journal/commit! txn)]
          (is (:ok recorded) "exactly five files is admitted")
          (is (false? (:ok receipt)))
          (is (= :txn-conflict (:error-type receipt)))
          (is (= last-read-only (:path receipt))
              "the refusal names the file that drifted")
          (is (= 0 (:files-written receipt)))
          (is (= (first h0) (bytes-of write-target))
              "the write target is untouched"))
        (finally (cleanup! ws))))))

;; @spec MCP-OP-MEM-007
(deftest a-new-file-in-the-scope-refuses-the-whole-transaction
  (testing "scope membership, not only file content, is revalidated: a caller
            or alias could hide in a file that appeared after planning"
    (let [ws (workspace! "scope-membership" 3)
          paths (sort (:paths ws))]
      (try
        (let [walk (fn [] (sort (map #(.getCanonicalPath ^java.io.File %)
                                     (filter #(.isFile ^java.io.File %)
                                             (file-seq (io/file (:root ws) "src"))))))
              txn (begin! ws {:scope-walk walk})
              _ (record-scope! txn paths)
              _ (journal/seal-read-set! txn)
              _ (journal/pin! txn (first paths))
              _ (journal/stage! txn (first paths) "(ns f0) (def v :new)\n")
              _ (write-file! (:root ws) "src/f999.clj" "(ns f999) (def v 999)\n")
              receipt (journal/commit! txn)]
          (is (false? (:ok receipt)))
          (is (= :txn-scope-membership-changed (:error-type receipt)))
          (is (= 0 (:files-written receipt)))
          (is (= "(ns f0) (def v 0)\n" (bytes-of (first paths)))))
        (finally (cleanup! ws))))))

;; @spec MCP-OP-MEM-007
(deftest a-scope-that-swaps-members-without-changing-its-count-refuses
  (testing "Sol's witness: membership [a b] planned, membership [c d] observed,
            equal count. A count is not a set. The sealed membership digest is
            what the transaction planned against, and it is what is compared."
    (let [ws (workspace! "scope-swap" 4)
          paths (sort (:paths ws))
          [a b c d] paths
          swapped? (atom false)]
      (try
        (let [h0 (mapv bytes-of paths)
              walk (fn [] (if @swapped? [c d] [a b]))
              txn (begin! ws {:scope-walk walk})
              _ (record-scope! txn [a b])
              _ (journal/seal-read-set! txn)
              _ (journal/pin! txn a)
              _ (journal/stage! txn a "(ns f0) (def v :new)\n")
              _ (reset! swapped? true)
              receipt (journal/commit! txn)]
          (is (false? (:ok receipt)))
          (is (= :txn-scope-membership-changed (:error-type receipt)))
          (is (= :scope-membership (:conflict receipt))
              "the refusal names the conflict class, not only its code")
          (is (not= (:planned-digest receipt) (:observed-digest receipt))
              "the sealed digest is what disagrees; the counts are equal")
          (is (= 2 (:planned-files receipt)))
          (is (= 2 (:observed-files receipt))
              "equal counts: only the digest can tell these two scopes apart")
          (is (= 0 (:files-written receipt)))
          (is (= h0 (mapv bytes-of paths)) "no byte of the workspace changed"))
        (finally (cleanup! ws))))))

;; @spec MCP-OP-MEM-007
(deftest a-write-set-file-that-drifts-after-revalidation-refuses-before-its-write
  (testing "the write set is rechecked immediately before each replacement"
    (let [ws (workspace! "write-set-drift" 4)
          paths (sort (:paths ws))]
      (try
        (let [h0 (mapv bytes-of paths)
              txn (begin! ws {})
              _ (record-scope! txn paths)
              _ (journal/seal-read-set! txn)
              _ (doseq [p paths]
                  (journal/pin! txn p)
                  (journal/stage! txn p (str (slurp p) ";; s\n")))
              victim (nth paths 2)
              receipt (journal/commit!
                        txn {:before-publish
                             (fn [path]
                               (when (= path victim)
                                 (spit victim "(ns f2) (def v :stolen)\n")))})]
          (is (false? (:ok receipt)))
          (is (= :txn-conflict (:error-type receipt)))
          (is (= victim (:path receipt)))
          (is (true? (:rolled-back receipt)))
          (is (= (take 2 h0) (mapv bytes-of (take 2 paths)))
              "the two files already written are rolled back to H0")
          (is (= "(ns f2) (def v :stolen)\n" (bytes-of victim))
              "the drifting file keeps the other writer's bytes: this
               transaction restores what IT changed and never clobbers a write
               it did not make")
          (is (= (last h0) (bytes-of (last paths)))
              "the paths never begun are untouched"))
        (finally (cleanup! ws))))))

;; ------------------------------------------ MCP-OP-MEM-006 pre-image lifetime

(defn- transaction-dir
  [ws txid]
  (io/file (journal/transactions-dir (:root ws) (:state-home ws)) txid))

;; @spec MCP-OP-MEM-006
(deftest a-committed-transaction-keeps-its-pre-images-and-can-be-undone
  (testing "Sol's adoption blocker: commit DELETED the transaction directory, so
            the receipt named a change nobody could reverse. The pre-image
            journal outlives the commit until something explicitly forgets it."
    (let [ws (workspace! "undo" 3)
          paths (sort (:paths ws))
          [p0 p1] paths]
      (try
        (let [h0 (mapv bytes-of paths)
              txn (begin! ws {})
              _ (record-scope! txn paths)
              _ (journal/seal-read-set! txn)
              _ (doseq [p [p0 p1]]
                  (journal/pin! txn p)
                  (journal/stage! txn p (str (slurp p) ";; changed\n")))
              receipt (journal/commit! txn)
              dir (transaction-dir ws (:txid receipt))]
          (is (:ok receipt))
          (is (true? (:retained receipt))
              "the receipt says its own recovery material was kept")
          (is (.isDirectory dir) "the transaction directory survives the commit")
          (is (= 2 (count (.listFiles (io/file dir "objects"))))
              "both pinned pre-images are still on disk")
          (is (not= (take 2 h0) (mapv bytes-of [p0 p1])) "the commit did change the tree")

          (let [undone (journal/undo! (:root ws) (:txid receipt)
                                      {:state-home (:state-home ws)})]
            (is (:ok undone))
            (is (= 2 (count (:paths undone))))
            (is (every? #(= :verified (:status %)) (:paths undone))
                "every restored path is verified against the digest pinned at H0")
            (is (= (take 2 h0) (mapv bytes-of [p0 p1]))
                "undo! puts the exact H0 bytes back")
            (is (= (nth h0 2) (bytes-of (nth paths 2)))
                "a path the transaction never wrote is not touched by undo"))

          (let [forgotten (journal/forget! (:root ws) (:txid receipt)
                                           {:state-home (:state-home ws)})]
            (is (:ok forgotten))
            (is (not (.exists dir))
                "an explicit forget! is the thing that removes the journal")))
        (finally (cleanup! ws))))))

;; @spec MCP-OP-MEM-006
(deftest a-quota-sweep-refuses-a-journal-a-receipt-still-references
  (testing "the other half of the retention policy: an explicit forget! is a
            decision, but a sweep running because disk is short must not
            silently destroy a receipt somebody is still holding"
    (let [ws (workspace! "lease-refcount" 2)
          path (first (sort (:paths ws)))]
      (try
        (let [txn (begin! ws {})
              _ (record-scope! txn (:paths ws))
              _ (journal/seal-read-set! txn)
              _ (journal/pin! txn path)
              _ (journal/stage! txn path "(ns f0) (def v :new)\n")
              receipt (journal/commit! txn)
              txid (:txid receipt)
              opts {:state-home (:state-home ws)}
              dir (transaction-dir ws txid)
              refused (journal/evict! (:root ws) txid opts)
              released (journal/release-receipt! (:root ws) txid opts)
              evicted (journal/evict! (:root ws) txid opts)]
          (is (:ok receipt))
          (is (false? (:ok refused)))
          (is (= :txn-journal-referenced (:error-type refused)))
          (is (= 1 (:receipt-refs refused))
              "the lease says how many receipts are holding it")
          (is (some? (:next_call refused))
              "and the refusal names the call that would release it")
          (is (:ok released))
          (is (:ok evicted) "once nothing references it, the sweep may reclaim it")
          (is (not (.exists dir)))
          (let [after (journal/undo! (:root ws) txid opts)]
            (is (false? (:ok after)))
            (is (= :txn-journal-missing (:error-type after))
                "an evicted receipt says it cannot be undone rather than
                 pretending it can")))
        (finally (cleanup! ws))))))

;; @spec MCP-OP-MEM-007
(deftest an-ordinary-atomic-write-cooperates-only-when-the-kernel-binds-it
  (testing "Opus round 2, finding 4: `with-publish-lock*` had exactly ONE call
            site in the whole repository, so the advisory lock excluded nobody
            who existed and the residual window was the normal path rather
            than the exception. Every other writer in the repo publishes
            through `file-ops/atomic-write!`. That verb can now cooperate, and
            the kernel is what turns it on - cooperation is per-writer, and the
            witness proves both halves against a lock a SEPARATE process holds."
    (let [ws (workspace! "cooperating-writes" 2)
          path (second (sort (:paths ws)))
          dir (journal/transactions-dir (:root ws) (:state-home ws))]
      (try
        (let [child (hold-publish-lock! ws 2500)
              unbound-start (System/nanoTime)
              _ (file-ops/atomic-write! path "(ns f1) (def v :unbound)\n")
              unbound-ms (quot (- (System/nanoTime) unbound-start) 1000000)
              bound-start (System/nanoTime)
              _ (journal/with-cooperating-writes
                  dir
                  (fn [] (file-ops/atomic-write! path "(ns f1) (def v :bound)\n")))
              bound-ms (quot (- (System/nanoTime) bound-start) 1000000)]
          (is (nil? file-ops/*publish-lock-dir*)
              "the default is unchanged: an ordinary writer takes no lock")
          (is (< unbound-ms 400)
              (str "an unbound atomic-write! is a non-cooperating writer and "
                   "does not wait; it returned after " unbound-ms " ms"))
          (is (>= bound-ms 900)
              (str "a bound one waits for the holder; it returned after "
                   bound-ms " ms"))
          (is (= "(ns f1) (def v :bound)\n" (bytes-of path)))
          (.waitFor child))

        (is (= :nested
               (file-ops/with-publish-lock*
                 dir
                 (fn []
                   (journal/with-cooperating-writes
                     dir
                     (fn []
                       (file-ops/atomic-write! path "(ns f1) (def v :nested)\n")
                       :nested)))))
            "a cooperating write INSIDE the lock must not deadlock on itself;
             FileChannel locks are per-JVM and re-locking throws")
        (is (= "(ns f1) (def v :nested)\n" (bytes-of path)))
        (finally (cleanup! ws))))))

;; @spec MCP-OP-MEM-007
(deftest a-second-thread-waits-for-the-publish-lock-instead-of-throwing
  (testing "Opus round 3, blocker 1: `FileChannel/lock` is a PER-PROCESS view
            of the OS lock, so a second THREAD in the same JVM does not block
            on it - it throws `OverlappingFileLockException`. The re-entrancy
            guard was a dynamic BINDING, so nothing serialised two threads
            before the OS lock was taken and the opt-in that finally gave the
            advisory lock a non-empty referent was unsafe in the direction it
            was added for. A process-wide monitor keyed by the lock file's
            canonical path is what makes threads queue."
    (let [ws (workspace! "publish-lock-threads" 2)
          path (second (sort (:paths ws)))
          dir (journal/transactions-dir (:root ws) (:state-home ws))
          holding (promise)
          release (promise)]
      (try
        (let [holder (future (file-ops/with-publish-lock*
                               dir
                               (fn [] (deliver holding true)
                                 (deref release 10000 :timeout))))
              _ (deref holding 10000 nil)
              started (System/nanoTime)
              writer (future
                       (try (journal/with-cooperating-writes
                              dir
                              (fn []
                                (file-ops/atomic-write!
                                  path "(ns f1) (def v :second-thread)\n")
                                :wrote))
                            (catch Throwable cause
                              {:threw (.getName (class cause))})))]
          (Thread/sleep 400)
          (is (not (realized? writer))
              (str "a second thread must WAIT for the holder rather than "
                   "throwing past it; it had already returned "
                   (pr-str (when (realized? writer) @writer))))
          (deliver release :released)
          (is (= :wrote (deref writer 10000 :timeout))
              "and once the holder lets go it completes normally")
          (is (>= (quot (- (System/nanoTime) started) 1000000) 400)
              "having actually waited")
          (is (= "(ns f1) (def v :second-thread)\n" (bytes-of path)))
          (is (= :released (deref holder 10000 :timeout))))
        (finally (cleanup! ws))))))

;; @spec MCP-OP-MEM-007
(deftest a-future-spawned-inside-the-publish-lock-takes-the-lock-itself
  (testing "Opus round 3, blocker 2: the re-entrancy guard was a DYNAMIC var,
            and Clojure conveys dynamic bindings to `future`, `send`, `pmap`
            and every `bound-fn`. The guard therefore said 'some frame on my
            binding stack took the lock', never 'this thread holds it', so a
            future spawned inside the lock - the first concurrency primitive an
            adopting verb reaches for, and the one house rules name for
            Surgeon's per-file plan phases - inherited the claim and took NO
            lock at all. Re-entrancy is a property of a THREAD."
    (let [ws (workspace! "publish-lock-future" 2)
          path (second (sort (:paths ws)))
          dir (journal/transactions-dir (:root ws) (:state-home ws))]
      (try
        ;; (a) the owning thread still holds it: a future is a different
        ;;     thread and must queue behind it.
        (let [done (promise)
              writer (file-ops/with-publish-lock*
                       dir
                       (fn []
                         (let [spawned (future
                                         (journal/with-cooperating-writes
                                           dir
                                           (fn []
                                             (file-ops/atomic-write!
                                               path "(ns f1) (def v :from-future)\n")
                                             (deliver done true)
                                             :wrote)))]
                           (Thread/sleep 400)
                           (is (not (realized? done))
                               "a future is a DIFFERENT thread; it may not write
                                under the owning thread's claim")
                           spawned)))]
          (is (= :wrote (deref writer 20000 :timeout))
              "and it completes once the owning thread lets go")
          (is (= "(ns f1) (def v :from-future)\n" (bytes-of path))))

        ;; (b) the conveyed claim against a lock ANOTHER JVM holds. This is the
        ;;     defect in its realisable form: the future carries the binding out
        ;;     of the owning thread's extent and then writes with no OS lock.
        (let [gate (promise)
              spawned (file-ops/with-publish-lock*
                        dir
                        (fn []
                          (future
                            (deref gate 30000 :timeout)
                            (journal/with-cooperating-writes
                              dir
                              (fn []
                                (file-ops/atomic-write!
                                  path "(ns f1) (def v :after-the-child)\n")
                                :wrote)))))
              child (hold-publish-lock! ws 2000)
              started (System/nanoTime)]
          (deliver gate :go)
          (is (= :wrote (deref spawned 30000 :timeout)))
          (let [elapsed-ms (quot (- (System/nanoTime) started) 1000000)]
            (is (>= elapsed-ms 900)
                (str "the future must take the publish lock ITSELF and wait for "
                     "the other process; it returned after " elapsed-ms " ms")))
          (is (= "(ns f1) (def v :after-the-child)\n" (bytes-of path)))
          (.waitFor child))
        (finally (cleanup! ws))))))

;; @spec MCP-OP-MEM-007
;; @spec MCP-OP-MEM-013
(deftest a-commit-racing-a-sibling-thread-does-not-strand-the-project-lock
  (testing "the blast radius of blocker 1. The `OverlappingFileLockException`
            escaped `publish-one!`, which is wrapped only around `prepare-fn`,
            so `commit!` never reached `finish!`: the transaction stayed
            `:sealed` and the project LOCK stayed behind a LIVE pid - which
            `begin!` may not break and `recover!` may not break either. The
            workspace was deadlocked for the life of the process, which is the
            exact failure the round this door arrived in was written to end."
    (let [ws (workspace! "commit-sibling-thread" 2)
          path (first (sort (:paths ws)))
          dir (journal/transactions-dir (:root ws) (:state-home ws))
          lock (io/file dir "LOCK")
          holding (promise)
          release (promise)]
      (try
        (let [txn (begin! ws {})]
          (record-scope! txn (:paths ws))
          (journal/seal-read-set! txn)
          (journal/pin! txn path)
          (journal/stage! txn path "(ns f0) (def v :committed)\n")
          (let [holder (future (file-ops/with-publish-lock*
                                 dir
                                 (fn [] (deliver holding true)
                                   (deref release 10000 :timeout))))
                _ (deref holding 10000 nil)
                receipt (future (try (journal/commit! txn)
                                     (catch Throwable cause
                                       {:threw (.getName (class cause))})))]
            (Thread/sleep 400)
            (is (not (realized? receipt))
                (str "the commit must wait for the sibling thread's lock; it "
                     "had already returned "
                     (pr-str (when (realized? receipt) @receipt))))
            (deliver release :released)
            (let [result (deref receipt 20000 :timeout)]
              (is (:ok result) (str "the commit completed: " (pr-str result)))
              (is (= 1 (:files-written result))))
            (is (= :released (deref holder 10000 :timeout))))
          (is (not (.isFile lock)) "and the project LOCK is released")
          (is (= "(ns f0) (def v :committed)\n" (bytes-of path)))
          (let [next-txn (begin! ws {})]
            (is (string? (:txid next-txn))
                "so the workspace is not deadlocked for the life of the process")
            (when (:txid next-txn) (journal/rollback! next-txn))))
        (finally (cleanup! ws))))))

;; @spec MCP-OP-MEM-007
;; @spec MCP-OP-MEM-013
(deftest a-commit-that-throws-inside-the-lock-still-releases-the-project-lock
  (testing "the general form of blocker 1, independent of its cause: any
            exception raised between taking the publish lock and returning
            left `commit!` without a `finish!`, and a transaction without a
            `finish!` is a project LOCK nobody releases. The lock must be
            released and the transaction marked on EVERY exception path, not
            only on the ones the kernel anticipated."
    (let [ws (workspace! "commit-throws" 2)
          path (first (sort (:paths ws)))
          dir (journal/transactions-dir (:root ws) (:state-home ws))
          lock (io/file dir "LOCK")]
      (try
        (let [txn (begin! ws {})]
          ;; keep the directory so the durable marking can be read back
          (swap! (:state txn) assoc :retain-dir? true)
          (record-scope! txn (:paths ws))
          (journal/seal-read-set! txn)
          (journal/pin! txn path)
          (journal/stage! txn path "(ns f0) (def v :never)\n")
          (let [thrown (try (journal/commit!
                              txn
                              {:in-commit-window
                               (fn [_] (throw (ex-info "injected inside the lock" {})))})
                            (catch Throwable cause cause))]
            (is (instance? Throwable thrown)
                "the exception itself is not swallowed into a false receipt")
            (is (not (.isFile lock))
                "but the project LOCK is released on every exception path")
            (is (= :rolled-back
                   (:status (read-string (slurp (io/file (:dir txn) "state.edn")))))
                "and the transaction is marked finished rather than left :sealed")
            (is (str/includes? (slurp (io/file (:dir txn) "journal.log")) "rolled-back")
                "with the durable journal line beside it")
            (let [next-txn (begin! ws {})]
              (is (string? (:txid next-txn)) "so the workspace is still usable")
              (when (:txid next-txn) (journal/rollback! next-txn)))))
        (finally (cleanup! ws))))))

;; @spec MCP-OP-MEM-007
(deftest a-throw-after-the-commit-is-recorded-does-not-un-commit-it
  (testing "Opus round 4, blocker 2: `finish-after-throw!` had no terminal
            status guard. `finish!` publishes, appends `committed` to the
            journal, writes `state.edn` `:committed`, RELEASES the project
            LOCK, and only then does its bookkeeping tail - reclaiming staging
            and writing the lease. An ENOSPC in that tail reached the catch,
            which rolled every written path back to H0: the durable record and
            the tree then disagree permanently, `recover!` will not touch a
            `:committed` journal and `undo!` refuses it as a digest conflict.
            The revert also ran AFTER the LOCK was released, outside every
            lock this build exists to take. Past a terminal status the tail is
            bookkeeping, and a failure there degrades to a bare release."
    (let [ws (workspace! "finish-tail-throw" 2)
          path (first (sort (:paths ws)))
          new-source "(ns f0) (def v :committed-and-staying)\n"
          dir (journal/transactions-dir (:root ws) (:state-home ws))
          lock (io/file dir "LOCK")]
      (try
        (let [txn (begin! ws {:txid "TAIL"})
              _ (record-scope! txn (:paths ws))
              _ (journal/seal-read-set! txn)
              _ (journal/pin! txn path)
              _ (journal/stage! txn path new-source)
              journal-dir (io/file (:dir txn))
              thrown (atom nil)]
          (with-redefs-fn
            {#'journal/write-lease!
             (fn [& _] (throw (java.io.IOException. "ENOSPC writing lease.edn")))}
            (fn []
              (try (journal/commit! txn)
                   (catch Throwable cause (reset! thrown cause)))))
          (is (instance? java.io.IOException @thrown)
              (str "the bookkeeping failure is still raised: " (pr-str @thrown)))
          (is (= "ENOSPC writing lease.edn" (.getMessage ^Throwable @thrown)))
          (is (= new-source (bytes-of path))
              "the bytes the journal calls committed are the bytes on disk")
          (is (= :committed
                 (:status (read-string (slurp (io/file journal-dir "state.edn")))))
              "and state.edn still says so")
          (is (= "committed" (last (str/split-lines (slurp (io/file journal-dir "journal.log")))))
              "as does the journal's last line")
          (is (not (.isFile lock)) "the project LOCK is released")
          (let [next-txn (begin! ws {})]
            (is (string? (:txid next-txn)) "so the workspace is still usable")
            (when (:txid next-txn) (journal/rollback! next-txn))))
        (finally (cleanup! ws))))))

;; @spec MCP-OP-MEM-013
(deftest breaking-a-stale-lock-cannot-delete-a-live-holders-fresh-lock
  (testing "Opus round 3, blocker 3: the break was a READ followed by an
            UNCONDITIONAL delete. Between the two, a second transaction can
            legitimately break the same stale lock and take it - and the first
            breaker then deletes the LIVE holder's brand new LOCK and takes it
            as well. Two live transactions then hold the project lock at once,
            and the first to finish deletes the other's claim. A break must
            remove the exact claim it judged, or remove nothing."
    (let [ws (workspace! "lock-break-race" 2)
          child (.start (ProcessBuilder. ["sleep" "30"]))
          dir (journal/transactions-dir (:root ws) (:state-home ws))
          lock (io/file dir "LOCK")]
      (try
        (plant-lock! ws {:txid "ghost-3" :pid (reaped-pid) :boot-id (boot-id-now)})
        (let [refused (begin! ws {:txid "B-BREAKER"
                                  :before-break
                                  (fn [_]
                                    ;; A breaks the same ghost and acquires,
                                    ;; while B is mid-break. A is genuinely
                                    ;; alive, so its lock is inviolable.
                                    (.delete lock)
                                    (spit lock (pr-str {:txid "A-LIVE"
                                                        :pid (.pid child)
                                                        :boot-id (boot-id-now)})))})]
          (is (false? (:ok refused))
              (str "the breaker must refuse rather than take a live holder's "
                   "lock: " (pr-str refused)))
          (is (= :txn-lock-held (:error-type refused)))
          (is (= "A-LIVE" (:holder-txid refused))
              "and it names the holder it found, not the ghost it judged")
          (is (true? (:holder-live refused)))
          (is (.isFile lock) "the live holder's LOCK survives")
          (is (= "A-LIVE" (:txid (read-string (slurp lock))))
              "unchanged, still naming the live holder")
          (is (not (.isFile (io/file dir "LOCK.broken.B-BREAKER")))
              "and the breaker left no claim of its own behind"))
        (finally
          (.destroyForcibly child)
          (.waitFor child)
          (cleanup! ws))))))

;; @spec MCP-OP-MEM-013
(deftest a-broken-lock-is-renamed-to-the-claim-it-broke
  (testing "the other half: a genuine break must still happen, and it must
            leave the exact claim it removed on disk under its breaker's name.
            That is what makes the break checkable after the fact - the receipt
            line says a lock was broken, and the tombstone says WHICH one."
    (let [ws (workspace! "lock-break-tombstone" 2)
          dir (journal/transactions-dir (:root ws) (:state-home ws))
          lock (io/file dir "LOCK")]
      (try
        (let [dead (reaped-pid)
              holder {:txid "ghost-4" :pid dead :boot-id (boot-id-now)}
              _ (plant-lock! ws holder)
              txn (begin! ws {:txid "B-TOMB"})
              tomb (io/file dir "LOCK.broken.B-TOMB")]
          (is (string? (:txid txn)) "the stale lock is still broken")
          (is (= :process-not-alive (get-in txn [:lock-broken :cause])))
          (is (.isFile tomb) "and the claim it broke is kept as evidence")
          (is (= "ghost-4" (:txid (read-string (slurp tomb))))
              "naming the holder the break judged, byte for byte")
          (is (= "B-TOMB" (:txid (read-string (slurp lock))))
              "while the LOCK now names the breaker")
          (journal/rollback! txn))
        (finally (cleanup! ws))))))

;; @spec MCP-OP-MEM-013
(defn- displaced-count
  "The kernel's running count of claims a break displaced and did not restore.

   Resolved rather than referred so this witness fails on its ASSERTION when
   the count does not exist yet, not on a namespace that will not compile."
  []
  (if-let [v (resolve 'clj-surgeon.txn-journal/displaced-claim-count)] (v) 0))

(defn- create-lock-if-absent!
  "Create the LOCK holding `content`, create-if-absent, the way `write-lock!`
   does. Returns true when this caller is the one that created it."
  [dir content]
  (let [^java.io.File lock (io/file dir "LOCK")
        ^java.io.File tmp (java.io.File/createTempFile ".LOCK-" ".tmp" (io/file dir))]
    (try
      (spit tmp content)
      (Files/createLink (.toPath lock) (.toPath tmp))
      true
      (catch java.nio.file.FileAlreadyExistsException _ false)
      (finally (Files/deleteIfExists (.toPath tmp))))))

;; @spec MCP-OP-MEM-013
(deftest the-breaks-restore-cannot-clobber-a-third-acquirers-claim
  (testing "Opus round 4, blocker 1: the break's RESTORE was `Files/move` with
            no REPLACE_EXISTING, which the JDK implements as a `statx` of the
            target followed by `rename(2)` - and POSIX rename replaces
            unconditionally. A third acquirer that creates its LOCK between
            those two syscalls is destroyed silently, which is round three's
            two-live-holders end state reached THROUGH the fix. The restore
            must be create-if-absent at the kernel level, and a restore that
            refuses must be reported rather than dropped: the displaced claim
            then survives only inside the tombstone, and its owner never
            learns it lost the lock unless somebody says so."
    (let [;; the restore exists only on the fallback a filesystem without
          ;; `link(2)` takes: the ordinary path claims the tombstone name by
          ;; `Files/createLink` and never moves the LOCK, so there is no gap
          ;; for a third acquirer to land in. `:unsafe-break-by-move true`
          ;; forces the path this witness is about.
          dir (temp-dir "lock-restore-storm")
          lock (io/file dir "LOCK")
          pid (.pid (java.lang.ProcessHandle/current))
          ;; the claim the break believes it judged: deliberately unlike
          ;; anything on disk, so the RESTORE branch runs every time
          judged {:holder {:txid "GHOST"}
                  :content "{:txid \"GHOST\"}"
                  :content-sha256 "deadbeef"
                  :file-key "(dev=0,ino=0)"}
          created (atom [])
          running (atom true)
          hammer (Thread.
                   (fn []
                     (loop [n 0]
                       (when @running
                         (let [content (pr-str {:txid (str "THIRD-" n)
                                                :pid pid :boot-id "b"})]
                           (if (create-lock-if-absent! dir content)
                             (do (swap! created conj content) (recur (inc n)))
                             (recur n)))))))
          cursor (atom 0)
          clobbered (atom 0)
          checked (atom 0)
          displaced (atom [])
          before-count (displaced-count)]
      (try
        (create-lock-if-absent! dir (pr-str {:txid "VICTIM-0" :pid pid :boot-id "b"}))
        (.start hammer)
        ;; enough iterations to land a documented number of third-party
        ;; claims INSIDE the restore gap, with a hard cap so a loaded box
        ;; cannot turn a race witness into an unbounded loop
        (loop [i 0]
         (when (and (< i 20000) (< (long @checked) 50))
          (when-not (.exists lock)
            (create-lock-if-absent! dir (pr-str {:txid (str "VICTIM-" i)
                                                 :pid pid :boot-id "b"})))
          (let [outcome (@#'journal/break-lock! dir judged (str "BRK-" i)
                                                {:unsafe-break-by-move true})
                seen @created
                pending (subvec seen (min @cursor (count seen)))
                _ (reset! cursor (count seen))
                on-disk (into #{} (keep (fn [^java.io.File f]
                                          (when (.isFile f)
                                            (try (slurp f) (catch Exception _ nil))))
                                        (.listFiles (io/file dir))))]
            (when (false? (:restored outcome)) (swap! displaced conj outcome))
            (doseq [content pending]
              (swap! checked inc)
              (when-not (contains? on-disk content) (swap! clobbered inc)))
            ;; the tombstones are accounted for above; keep the directory small
            (doseq [^java.io.File f (.listFiles (io/file dir))]
              (when (.startsWith (.getName f) "LOCK.broken") (.delete f))))
          (recur (inc i))))
        (reset! running false)
        (.join hammer 5000)

        (is (>= (long @checked) 50)
            (str "the storm must actually land third-party claims inside the "
                 "restore gap, or it proves nothing: checked=" @checked))
        (is (zero? @clobbered)
            (str "a third acquirer's live claim must never be destroyed by the "
                 "restore: " @clobbered " of " @checked " were"))
        (is (pos? (count @displaced))
            "and the storm must exercise the refused restore at least once")
        (let [untyped (remove #(and (= :holder-changed (:cause %))
                                    (false? (:restored %))
                                    (string? (:tombstone %))
                                    (keyword? (:restore-cause %)))
                              @displaced)]
          (is (zero? (count untyped))
              (str "every refused restore is a TYPED outcome naming the "
                   "tombstone the displaced claim is in; first untyped: "
                   (pr-str (first untyped)))))
        (is (= (+ before-count (count @displaced)) (displaced-count))
            "and every one of them is counted, so the bucket is visible")
        (finally
          (reset! running false)
          (.join hammer 5000)
          (delete-tree! dir))))))

;; @spec MCP-OP-MEM-013
(deftest a-displaced-claim-is-reported-to-the-caller-that-displaced-it
  (testing "the other half of blocker 1. When the restore refuses, the claim
            that was renamed away is NOT put back: the LOCK names the third
            acquirer and the displaced claim survives only inside the
            tombstone. Both callers used to keep `(:broken outcome)` alone, so
            `:restored false` and its cause were dropped and the refusal named
            the third acquirer with no hint that a claim had been displaced -
            a silent refusal with no owner. Both must surface it."
    (let [ws (workspace! "lock-displaced" 2)
          child (.start (ProcessBuilder. ["sleep" "30"]))
          dir (journal/transactions-dir (:root ws) (:state-home ws))
          lock (io/file dir "LOCK")]
      (try
        (plant-lock! ws {:txid "ghost-5" :pid (reaped-pid) :boot-id (boot-id-now)})
        (let [;; the claim the break judges is replaced by a LIVE holder's own
              ;; claim before the rename, so the recheck mismatches and the
              ;; restore runs; a THIRD acquirer then lands in the restore gap
              refused (begin! ws {:txid "B-DISPLACER"
                                  :unsafe-break-by-move true
                                  :before-break
                                  (fn [_]
                                    (.delete lock)
                                    (spit lock (pr-str {:txid "A-LIVE"
                                                        :pid (.pid child)
                                                        :boot-id (boot-id-now)})))
                                  :before-restore
                                  (fn [_]
                                    (spit lock (pr-str {:txid "C-THIRD"
                                                        :pid (.pid child)
                                                        :boot-id (boot-id-now)})))})
              displaced (:lock-break-displaced refused)]
          (is (false? (:ok refused)))
          (is (= :txn-lock-held (:error-type refused)))
          (is (= "C-THIRD" (:txid (read-string (slurp lock))))
              "the third acquirer's claim is untouched")
          (is (some? displaced)
              (str "and the refusal SAYS a claim was displaced: " (pr-str refused)))
          (is (false? (:restored displaced)))
          (is (= :holder-changed (:cause displaced)))
          (is (pos? (long (:displaced-claims-total displaced 0)))
              "with the running count of displacements this process has made")
          (let [tomb (io/file dir (:tombstone displaced))]
            (is (.isFile tomb)
                "the displaced claim is on disk, in the tombstone the refusal names")
            (is (= "A-LIVE" (:txid (read-string (slurp tomb))))
                "byte for byte the claim that was renamed away")))
        (finally
          (.destroyForcibly child)
          (.waitFor child)
          (cleanup! ws))))))

;; @spec MCP-OP-MEM-013
(deftest recovery-reports-a-claim-its-break-displaced
  (testing "the same displacement seen from the second caller. `recover!` kept
            `(:broken outcome)` too, so a recovery that displaced a claim
            returned an ordinary success."
    (let [ws (workspace! "lock-displaced-recover" 2)
          child (.start (ProcessBuilder. ["sleep" "30"]))
          dir (journal/transactions-dir (:root ws) (:state-home ws))
          lock (io/file dir "LOCK")]
      (try
        (plant-lock! ws {:txid "ghost-6" :pid (reaped-pid) :boot-id (boot-id-now)})
        (let [result (journal/recover!
                       (:root ws)
                       {:state-home (:state-home ws)
                        :unsafe-break-by-move true
                        :before-break (fn [_]
                                        (.delete lock)
                                        (spit lock (pr-str {:txid "A-LIVE"
                                                            :pid (.pid child)
                                                            :boot-id (boot-id-now)})))
                        :before-restore (fn [_]
                                          (spit lock (pr-str {:txid "C-THIRD"
                                                              :pid (.pid child)
                                                              :boot-id (boot-id-now)})))})
              displaced (:lock-break-displaced result)]
          (is (nil? (:lock-broken result)) "nothing was broken")
          (is (some? displaced)
              (str "but a claim was displaced, and the receipt says so: "
                   (pr-str result)))
          (is (= :holder-changed (:cause displaced)))
          (is (false? (:restored displaced)))
          (is (= "A-LIVE" (:txid (read-string (slurp (io/file dir (:tombstone displaced))))))
              "and names the tombstone the displaced claim is in"))
        (finally
          (.destroyForcibly child)
          (.waitFor child)
          (cleanup! ws))))))


;; @spec MCP-OP-MEM-013
(deftest a-broken-lock-tombstone-is-visible-counted-and-retired
  (testing "Opus round 4, finding 5: the tombstone is called evidence and
            nothing reads it. `recover!` and `retained-transactions` both
            filter `.isDirectory`, so no verb listed, counted, billed or
            retired a `LOCK.broken.*` file - one 30,000-break storm left
            20,356 of them permanent in a single transactions directory. A
            receipt nobody can read and nobody can retire is not a receipt: it
            must appear in the sweep's own listing, be counted in recovery's
            own value, and be bounded by a published age."
    (let [ws (workspace! "lock-tombstones" 2)
          dir (io/file (journal/transactions-dir (:root ws) (:state-home ws)))
          opts {:state-home (:state-home ws)}
          fresh (io/file dir "LOCK.broken.FRESH")
          stale (io/file dir "LOCK.broken.STALE")]
      (try
        (.mkdirs dir)
        (spit fresh (pr-str {:txid "ghost-fresh" :pid 1}))
        (spit stale (pr-str {:txid "ghost-stale" :pid 2}))
        ;; Both tombstones were made NOW and ctime cannot be moved backwards
        ;; through the filesystem API at all, which is the point of the fix:
        ;; back-dating mtime is exactly the forgery the newest-of-two rule
        ;; refuses, so a witness separates them with the CLOCK. FRESH's basis
        ;; is pushed an hour ahead of STALE's, and the sweep is read at a
        ;; clock one millisecond past STALE's retention.
        (.setLastModified fresh (+ (System/currentTimeMillis) 3600000))
        (let [rows (journal/retained-transactions (:root ws) opts)
              tombstone-rows (filter #(= :broken-lock (:kind %)) rows)]
          (is (= 2 (count tombstone-rows))
              (str "the sweep's listing sees both tombstones: " (pr-str rows)))
          (is (= #{"LOCK.broken.FRESH" "LOCK.broken.STALE"}
                 (set (map :txid tombstone-rows)))
              "each named by the file it is")
          (is (every? #(and (number? (:bytes %)) (number? (:age-ms %)))
                      tombstone-rows)
              "with the bytes it bills and the age that retires it"))

        (let [clock (+ (lock-age-basis stale) (long journal/broken-lock-retention-ms) 1)
              result (journal/recover! (:root ws) (assoc opts :now-ms clock))
              counted (:broken-locks result)]
          (is (some? counted)
              (str "recovery counts them in its own value: " (pr-str result)))
          (is (= 2 (:found counted)) "both were found")
          (is (= 1 (:pruned counted)) "the one past the published age is retired")
          (is (= 1 (:remaining counted)))
          (is (pos? (long (:retention-ms counted 0)))
              "and the age it used is published, not implicit")
          (is (.isFile fresh) "a fresh break's evidence is kept")
          (is (not (.exists stale)) "an old one is not kept for ever"))
        (finally (cleanup! ws))))))

;; @spec MCP-OP-MEM-013
(deftest a-tombstone-name-that-would-collide-refuses-instead-of-overwriting
  (testing "the other half of finding 5. The tombstone is named for the
            BREAKER's txid, `begin!` accepts `:txid` from its caller, and the
            rename carried REPLACE_EXISTING - so two breaks by one txid
            overwrote each other silently and the file this build calls
            evidence was destroyed by name. A collision is a typed refusal
            taken BEFORE the LOCK is touched, never a replacement."
    (let [ws (workspace! "lock-tombstone-collision" 2)
          dir (io/file (journal/transactions-dir (:root ws) (:state-home ws)))
          lock (io/file dir "LOCK")
          tomb (io/file dir "LOCK.broken.SAME-TXID")]
      (try
        (plant-lock! ws {:txid "ghost-a" :pid (reaped-pid) :boot-id (boot-id-now)})
        (let [first-txn (begin! ws {:txid "SAME-TXID"})]
          (is (string? (:txid first-txn)) "the first break happens")
          (is (= "ghost-a" (:txid (read-string (slurp tomb)))))
          (journal/rollback! first-txn))

        (plant-lock! ws {:txid "ghost-b" :pid (reaped-pid) :boot-id (boot-id-now)})
        (let [refused (begin! ws {:txid "SAME-TXID"})]
          (is (false? (:ok refused))
              (str "the second break refuses rather than overwriting the first "
                   "break's evidence: " (pr-str refused)))
          (is (= :txn-lock-held (:error-type refused)))
          (is (= :tombstone-exists (get-in refused [:lock-break-refused :cause]))
              "with the reason the break did not happen, named")
          (is (= "LOCK.broken.SAME-TXID"
                 (get-in refused [:lock-break-refused :tombstone])))
          (is (= "ghost-a" (:txid (read-string (slurp tomb))))
              "the first break's receipt is intact")
          (is (= "ghost-b" (:txid (read-string (slurp lock))))
              "and the claim it refused to break was never touched"))
        (finally (cleanup! ws))))))

;; @spec MCP-OP-MEM-013
(deftest a-refused-break-names-the-file-that-blocked-it-and-how-it-clears
  (testing "Opus round 8, finding 2, probe B1. A crash between
            `mark-break-linked!` and `Files/createLink` leaves an ORPHAN
            SIDECAR - a marker with no tombstone - and the next break by the
            SAME txid is refused for that name, repeatably, until the
            published retention sweep retires it. The refusal is fail-safe
            (the LOCK is untouched), typed, counted and self-clearing, so the
            lockout is acceptable.

            The REMEDY was not. It said `check that the transactions directory
            is writable and has space, then retry` - the directory IS
            writable, there IS space, and retrying fails identically for a
            day. A refusal whose remedy points at the wrong cause is worse
            than a bare cause, because it sends the owner to chmod and df
            while the actual fix is one named file. House rule 17: a refusal
            names its owner AND what they can actually do."
    (let [ws (workspace! "sidecar-name-taken" 2)
          dir (io/file (journal/transactions-dir (:root ws) (:state-home ws)))
          lock (io/file dir "LOCK")
          orphan (io/file dir "LOCK.broken-at.STABLE-TXID")]
      (try
        (plant-lock! ws {:txid "ghost-o" :pid (reaped-pid)
                         :boot-id (boot-id-now)})
        ;; exactly what the crash leaves: the marker claimed, the evidence
        ;; name never taken
        (spit orphan (pr-str {:tombstone "LOCK.broken.STABLE-TXID"
                              :phase :linked
                              :linked-at-ms (System/currentTimeMillis)}))
        (let [refused (begin! ws {:txid "STABLE-TXID"})
              line (:lock-break-refused refused)
              remedy (str (:remedy line))]
          (is (false? (:ok refused))
              (str "the break is refused before the LOCK is touched: "
                   (pr-str refused)))
          (is (= :evidence-unrecordable (:cause line))
              (str "with the cause named: " (pr-str line)))
          (is (= "LOCK.broken-at.STABLE-TXID" (:blocking-sidecar line))
              (str "and the FILE that blocked it named, so the owner has "
                   "something to act on: " (pr-str line)))
          (is (str/includes? remedy "LOCK.broken-at.STABLE-TXID")
              (str "the remedy names that file: " remedy))
          (is (str/includes? remedy "recover")
              (str "and says what clears it - recovery retiring the orphan: "
                   remedy))
          (is (str/includes? remedy (str journal/broken-lock-retention-ms))
              (str "on the PUBLISHED retention, quoted rather than implied: "
                   remedy))
          (is (not (str/includes? remedy "writable"))
              (str "and never sends the owner to chmod for a name collision: "
                   remedy))
          (is (not (str/includes? remedy "space"))
              (str "nor to df: " remedy))
          (is (.isFile lock) "the LOCK is untouched")
          (is (= "ghost-o" (:txid (read-string (slurp lock))))
              "and still names the holder it refused to break")
          (is (not (.isFile (io/file dir "LOCK.broken.STABLE-TXID")))
              "and no tombstone was made"))
        (finally (cleanup! ws))))))

;; @spec MCP-OP-MEM-013
(def ^:private receipt-file-keys
  "The keys under which a receipt names a file in the transactions directory.

   `:tombstone` is the evidence a resolution or a break is about;
   `:blocking-sidecar` and `:sidecar` are a file that REFUSED a break. Opus
   round 8, finding 3: the walk covered `:tombstone` alone, so the ONE refusal
   that names a file was the one refusal the standing invariant could not
   see."
  [:tombstone :blocking-sidecar :sidecar])

(defn- receipt-file-names
  "Every place a receipt NAMES a file in the transactions directory, with the
   `:evidence` word its own map carries about that file, if any.

   Walked rather than enumerated, so a key added later is covered by the
   invariant on the day it appears rather than on the day someone remembers
   this witness. The enclosing map is what is collected, not the bare string,
   because the question the invariant asks is not `does this file exist` but
   `does this receipt account for the file it names`."
  [receipt]
  (let [named (atom [])]
    (walk/postwalk
      (fn [x]
        (when (map? x)
          (doseq [k receipt-file-keys
                  :when (string? (get x k))]
            (swap! named conj {:name (get x k)
                               :key k
                               :evidence (:evidence x)
                               :in x})))
        x)
      receipt)
    @named))

(defn- unaccounted-names
  "The names a receipt carries that neither exist nor say what became of them."
  [dir receipt]
  (remove (fn [entry]
            (or (.isFile (io/file dir ^String (:name entry)))
                (some? (:evidence entry))))
          (receipt-file-names receipt)))

;; @spec MCP-OP-MEM-013
(deftest a-break-does-not-retire-the-evidence-it-just-created
  (testing "Opus round 5, blocker 1. The tombstone is made by renaming the
            LOCK, and a rename PRESERVES mtime, while the prune measured
            retention against that mtime - so the evidence inherited the age
            of the claim it was evidence OF. Breaking a crashed holder's
            two-day-old lock therefore returned a receipt naming
            `LOCK.broken.recover-...` beside a directory that no longer
            contained it, in the same call, with no race and no injection
            point: the ordinary case a break exists for. Evidence carries its
            OWN creation time or the retention rule is measured against the
            wrong clock."
    (let [ws (workspace! "tombstone-own-age" 2)
          dir (io/file (journal/transactions-dir (:root ws) (:state-home ws)))
          lock (io/file dir "LOCK")]
      (try
        (plant-lock! ws {:txid "CRASHED-HOLDER"
                         :pid (reaped-pid)
                         :boot-id (boot-id-now)})
        ;; a holder that crashed two days ago - older than the published
        ;; retention age, which is what makes this deterministic
        (.setLastModified lock (- (System/currentTimeMillis) (* 2 24 60 60 1000)))
        (let [result (journal/recover! (:root ws) {:state-home (:state-home ws)})
              broken (:lock-broken result)
              counted (:broken-locks result)]
          (is (= :process-not-alive (:cause broken))
              (str "the stale claim is broken: " (pr-str result)))
          (is (string? (:tombstone broken))
              "and the receipt names the evidence it left")
          (is (.isFile (io/file dir (:tombstone broken)))
              (str "which must be on disk when the call that minted the name "
                   "returns: " (:tombstone broken)))
          (is (zero? (long (:pruned counted 0)))
              (str "the break's own evidence is not old enough to retire: "
                   (pr-str counted)))
          (is (= 1 (:remaining counted))
              (str "it is a standing count of one, not an empty bucket: "
                   (pr-str counted))))
        (finally (cleanup! ws))))))

;; @spec MCP-OP-MEM-013
(deftest a-receipt-never-names-a-file-that-is-not-there
  (testing "house rule 20 as a standing witness over `recover!`'s own value.
            A receipt must name its subject; a name whose file the same call
            destroyed is worse than no name at all, because it terminates the
            investigation it exists to start.

            Opus round 7, finding 3, probe C. As written this ran ONE
            scenario - a dead holder and a break - so it never reached a
            revert, and was silent on the entire path round seven added a
            DELETION to: `recover!` unlinking a tombstone it names in the same
            receipt. Driven through a revert it failed outright. The invariant
            it should have carried is not `every named file exists` - a revert
            that names the tombstone it deleted and says `:evidence :removed`
            satisfies house rule 20 and is strictly better than silence - but
            EVERY NAME EITHER EXISTS AT RETURN OR ITS OWN MAP SAYS WHAT BECAME
            OF IT. In that form it also constrains finding 1's fix, because an
            `:evidence :retained` resolution has to leave the file there.

            Driven through all three resolutions: a break, a revert, and a
            finish."
    (let [check!
          (fn [label dir result]
            (let [named (receipt-file-names result)
                  unaccounted (unaccounted-names dir result)]
              (is (seq named)
                  (str label ": the receipt names at least one file: "
                       (pr-str result)))
              (is (empty? unaccounted)
                  (str label ": and every name it carries EXISTS at return or "
                       "carries an explicit :evidence key saying what became "
                       "of it; unaccounted: "
                       (pr-str (map :name unaccounted)) " in "
                       (pr-str result)))
              ;; and the two words are not interchangeable: a resolution that
              ;; says the file is retained has to have left it there
              (doseq [entry named]
                (when (= :retained (:evidence entry))
                  (is (.isFile (io/file dir ^String (:name entry)))
                      (str label ": :evidence :retained must mean the file is "
                           "on disk: " (pr-str (:in entry))))))))]

      ;; (a) a break: the evidence it minted is on disk
      (let [ws (workspace! "receipt-names-exist" 2)
            dir (io/file (journal/transactions-dir (:root ws) (:state-home ws)))
            lock (io/file dir "LOCK")
            opts {:state-home (:state-home ws)}]
        (try
          (plant-lock! ws {:txid "CRASHED-HOLDER"
                           :pid (reaped-pid)
                           :boot-id (boot-id-now)})
          (.setLastModified lock (- (System/currentTimeMillis)
                                    (* 2 24 60 60 1000)))
          (check! "break" dir (journal/recover! (:root ws) opts))
          (let [rows (filter #(contains? #{:broken-lock :interrupted-break}
                                         (:kind %))
                             (journal/retained-transactions (:root ws) opts))
                missing (remove #(.isFile (io/file dir ^String (:txid %))) rows)]
            (is (empty? missing)
                (str "and the sweep lists no row for a file that is not there: "
                     (pr-str missing))))
          (finally (cleanup! ws))))

      ;; (b) a REVERT: the one path that deletes a file it names
      (let [ws (workspace! "receipt-names-revert" 2)
            child (.start (ProcessBuilder. ["sleep" "30"]))
            dir (io/file (journal/transactions-dir (:root ws) (:state-home ws)))
            opts {:state-home (:state-home ws)}
            lock (io/file dir "LOCK")
            tomb (io/file dir "LOCK.broken.REVERT-ME")]
        (try
          (plant-lock! ws {:txid "A-LIVE" :pid (.pid child)
                           :boot-id (boot-id-now)})
          (Files/createLink (.toPath tomb) (.toPath lock))
          (let [result (journal/recover! (:root ws) opts)]
            (is (= :interrupted-break-reverted
                   (:resolution (first (:interrupted-breaks result))))
                (str "the scenario really is a revert: " (pr-str result)))
            (check! "revert" dir result))
          (finally
            (.destroyForcibly child)
            (.waitFor child)
            (cleanup! ws))))

      ;; (c) a FINISH: the resolution that must LEAVE the file there
      (let [ws (workspace! "receipt-names-finish" 2)
            dir (io/file (journal/transactions-dir (:root ws) (:state-home ws)))
            opts {:state-home (:state-home ws)}
            lock (io/file dir "LOCK")]
        (try
          (plant-lock! ws {:txid "ghost-r3" :pid (reaped-pid)
                           :boot-id (boot-id-now)})
          (let [claim (@#'journal/read-lock-claim lock)]
            (@#'journal/break-lock!
              dir claim "FINISH-ME"
              {:before-unlink (fn [_] (throw (java.io.IOException. "crash")))}))
          (let [result (journal/recover! (:root ws) opts)]
            (is (= :interrupted-break-finished
                   (:resolution (first (:interrupted-breaks result))))
                (str "the scenario really is a finish: " (pr-str result)))
            (check! "finish" dir result))
          (finally (cleanup! ws))))

      ;; (d) SIX CONCURRENT `recover!` calls over ONE live claim - the shape
      ;;     Opus round 8, finding 1 measured through the PUBLIC verb, with no
      ;;     forgery, no hand cleanup and no crash. `recover!` takes no mutual
      ;;     exclusion before resolving, so one call's REVERT deletes a
      ;;     tombstone another call has already listed; the second then
      ;;     resolved an ABSENT file and asserted `:evidence :retained :stamp
      ;;     :ok` about it - 188 of 240 lines - and minted the orphan sidecar
      ;;     it would report tomorrow, 40 of them, doing it. The single-caller
      ;;     scenarios above cannot reach it because they never have a
      ;;     concurrent deleter.
      (let [ws (workspace! "receipt-names-storm" 2)
            child (.start (ProcessBuilder. ["sleep" "60"]))
            dir (io/file (journal/transactions-dir (:root ws) (:state-home ws)))
            opts {:state-home (:state-home ws)}
            lock (io/file dir "LOCK")
            tombs 20
            callers 6]
        (try
          (plant-lock! ws {:txid "A-LIVE" :pid (.pid child)
                           :boot-id (boot-id-now)})
          (doseq [i (range tombs)]
            (Files/createLink
              (.toPath (io/file dir (str "LOCK.broken.STORM-" i)))
              (.toPath lock)))
          (let [barrier (java.util.concurrent.CyclicBarrier. (int callers))
                pool (java.util.concurrent.Executors/newFixedThreadPool
                       (int callers))
                receipts (try
                           (mapv #(.get ^java.util.concurrent.Future %)
                                 (.invokeAll
                                   pool
                                   (vec (repeat callers
                                                (fn []
                                                  (.await barrier)
                                                  (journal/recover!
                                                    (:root ws) opts))))))
                           (finally (.shutdown pool)))
                lines (vec (mapcat :interrupted-breaks receipts))
                lied (filterv
                       (fn [line]
                         (and (= :retained (:evidence line))
                              (not (.isFile (io/file dir ^String (:tombstone line))))))
                       lines)
                orphans (filterv (fn [^java.io.File f]
                                   (.startsWith (.getName f) "LOCK.broken-at."))
                                 (vec (.listFiles dir)))]
            (is (seq lines)
                (str "the storm really resolved interrupted breaks: "
                     (count lines) " lines from " callers " callers"))
            (is (zero? (count lied))
                (str "no resolution says :evidence :retained for a tombstone "
                     "that is not on disk - " (count lied) " of " (count lines)
                     " lines did, e.g. " (pr-str (first lied))))
            (is (zero? (count orphans))
                (str "and no resolve MINTED the orphan sidecar it would report "
                     "tomorrow - " (count orphans) " minted, e.g. "
                     (pr-str (some-> ^java.io.File (first orphans) .getName)))))
          (finally
            (.destroyForcibly child)
            (.waitFor child)
            (cleanup! ws)))))))

;; @spec MCP-OP-MEM-013
(deftest a-resolution-never-says-retained-for-a-file-it-did-not-keep
  (testing "Opus round 8, finding 1, probes E1 and E4. `:evidence :retained`
            is a POSITIVE assertion about the state at return, and round
            seven's witness asserts it verbatim - but the code emitted it, and
            `:stamp :ok` beside it, with no check that the tombstone was ever
            there. `touch-tombstone!` was the twin of the bug the round-eight
            branch had already fixed two functions away: it caught its own
            `setLastModifiedTime` failure and returned the clock REGARDLESS,
            so `stamp-tombstone!`'s truth value came from the SIDECAR write
            alone and said nothing about the file the receipt names.

            A receipt must be honest AT CONSTRUCTION, not merely at return: a
            false green terminates the investigation it exists to start."
    ;; (a) the swallowed failure itself
    (let [dir (temp-dir "touch-absent")]
      (try
        (is (nil? (@#'journal/touch-tombstone!
                    (io/file dir "LOCK.broken.NOT-THERE")
                    (System/currentTimeMillis)))
            "setting the mtime of a file that is not there is not a stamp")
        (finally (delete-tree! dir))))

    ;; (b) the typed result both resolution branches read
    (let [ws (workspace! "stamp-typed" 2)
          dir (io/file (journal/transactions-dir (:root ws) (:state-home ws)))
          lock (io/file dir "LOCK")
          present (io/file dir "LOCK.broken.HERE")
          absent (io/file dir "LOCK.broken.GONE")]
      (try
        (plant-lock! ws {:txid "ghost-s" :pid (reaped-pid)
                         :boot-id (boot-id-now)})
        (Files/createLink (.toPath present) (.toPath lock))
        (let [kept (@#'journal/stamp-tombstone! present)
              gone (@#'journal/stamp-tombstone! absent)]
          (is (= :retained (:evidence kept))
              (str "a tombstone that is on disk is retained: " (pr-str kept)))
          (is (= :ok (:stamp kept))
              (str "and both halves of its stamp happened: " (pr-str kept)))
          (is (= :vanished (:evidence gone))
              (str "a tombstone that is NOT on disk was not kept by this "
                   "call, and the word for that is not :retained: "
                   (pr-str gone)))
          (is (not= :ok (:stamp gone))
              (str "and a stamp that never touched a file is not :ok: "
                   (pr-str gone)))
          (is (not (.isFile (io/file dir "LOCK.broken-at.GONE")))
              "and no sidecar is minted beside a tombstone that is not there"))
        (finally (cleanup! ws))))

    ;; (c) probe E1: the uncorroborated branch handed a tombstone that is not
    ;;     there - deterministic, the state a concurrent revert leaves
    (let [ws (workspace! "resolve-absent" 2)
          child (.start (ProcessBuilder. ["sleep" "30"]))
          transactions (journal/transactions-dir (:root ws) (:state-home ws))
          dir (io/file transactions)
          tomb (io/file dir "LOCK.broken.GONE")]
      (try
        (plant-lock! ws {:txid "A-LIVE" :pid (.pid child)
                         :boot-id (boot-id-now)})
        (let [line (@#'journal/resolve-interrupted-break!
                     transactions tomb false nil)]
          (is (not (.isFile tomb))
              "the tombstone really was absent for the whole call")
          (is (= :vanished (:evidence line))
              (str "the resolution never says :retained for a file it did "
                   "not keep: " (pr-str line)))
          (is (not= :ok (:stamp line))
              (str "and never :stamp :ok for a file it never stamped: "
                   (pr-str line)))
          (is (not (.isFile (io/file dir "LOCK.broken-at.GONE")))
              (str "and the call mints no orphan sidecar doing it: "
                   (pr-str (mapv (fn [^java.io.File f] (.getName f))
                                 (vec (.listFiles dir)))))))
        (finally
          (.destroyForcibly child)
          (.waitFor child)
          (cleanup! ws))))))

;; @spec MCP-OP-MEM-013
(deftest a-stamp-mints-no-sidecar-for-a-tombstone-that-is-not-there
  (testing "Opus round 8, finding 1, the other half - probe E4, where 40
            orphan sidecars were minted beside 0 tombstones in one storm.
            `stamp-broken-at!` writes a DIFFERENT file from the one it stamps,
            and it never looked at the one it stamps: handed a tombstone that
            was already gone it happily created `LOCK.broken-at.<name>` next
            to nothing, so the resolve that lied about keeping the evidence
            also MANUFACTURED the orphan it would report tomorrow. The sweep
            lists and retires those, so the accumulation is bounded - but a
            bucket a correct call fills is not a bucket, it is a leak with a
            broom behind it."
    (let [ws (workspace! "stamp-absent" 2)
          dir (io/file (journal/transactions-dir (:root ws) (:state-home ws)))
          lock (io/file dir "LOCK")
          absent (io/file dir "LOCK.broken.NEVER-WAS")
          present (io/file dir "LOCK.broken.HERE")]
      (try
        (plant-lock! ws {:txid "ghost-sb" :pid (reaped-pid)
                         :boot-id (boot-id-now)})
        (is (nil? (@#'journal/stamp-broken-at! absent))
            "a stamp for a file that is not there is not a stamp")
        (is (not (.isFile (io/file dir "LOCK.broken-at.NEVER-WAS")))
            (str "and it mints no sidecar beside a tombstone that is not "
                 "there: "
                 (pr-str (sort (mapv (fn [^java.io.File f] (.getName f))
                                     (vec (.listFiles dir)))))))
        ;; and the ordinary case is untouched: a tombstone that IS there is
        ;; stamped exactly as before
        (Files/createLink (.toPath present) (.toPath lock))
        (let [stamped (@#'journal/stamp-broken-at! present)]
          (is (number? stamped)
              (str "a tombstone that is on disk is still stamped: "
                   (pr-str stamped)))
          (is (.isFile (io/file dir "LOCK.broken-at.HERE"))
              "and its sidecar is written"))
        (finally (cleanup! ws))))))

;; @spec MCP-OP-MEM-013
(deftest the-standing-invariant-sees-the-file-a-refusal-names
  (testing "Opus round 8, finding 3. `:evidence` carried two different
            meanings: `:retained`/`:removed` say WHAT BECAME OF the file a
            resolution names, and `:sidecar-name-taken` said WHICH FILE
            BLOCKED the break. `unaccounted-names` treats ANY `:evidence`
            value as an account of the file, so the day one refusal carried
            both keys the witness would have passed vacuously - and
            `receipt-file-names` walked only maps with a string `:tombstone`,
            so the ONE refusal that names a file was the one refusal the
            standing invariant could not see.

            The blocking file gets its own key, and the walk covers it."
    (let [ws (workspace! "blocking-sidecar-seen" 2)
          dir (io/file (journal/transactions-dir (:root ws) (:state-home ws)))
          orphan (io/file dir "LOCK.broken-at.STABLE-TXID")]
      (try
        (plant-lock! ws {:txid "ghost-b3" :pid (reaped-pid)
                         :boot-id (boot-id-now)})
        (spit orphan (pr-str {:tombstone "LOCK.broken.STABLE-TXID"
                              :phase :linked}))
        (let [refused (begin! ws {:txid "STABLE-TXID"})
              line (:lock-break-refused refused)
              named (receipt-file-names refused)]
          (is (= "LOCK.broken-at.STABLE-TXID" (:blocking-sidecar line))
              (str "the blocking file has its OWN key, so it cannot be "
                   "mistaken for a word about what became of a file: "
                   (pr-str line)))
          (is (nil? (:evidence line))
              (str "and `:evidence` is not overloaded to carry it: "
                   (pr-str line)))
          (is (= #{"LOCK.broken-at.STABLE-TXID"} (set (map :name named)))
              (str "the walk behind the standing invariant SEES it: "
                   (pr-str named)))
          (is (empty? (unaccounted-names dir refused))
              (str "and it is accounted for, because the file that blocked "
                   "the break is on disk: "
                   (pr-str (unaccounted-names dir refused))))
          ;; and the invariant is a witness rather than a description: it
          ;; fires on a refusal that names a file which is not there
          (let [forged (assoc-in refused
                                 [:lock-break-refused :blocking-sidecar]
                                 "LOCK.broken-at.NOT-THERE")]
            (is (= ["LOCK.broken-at.NOT-THERE"]
                   (mapv :name (unaccounted-names dir forged)))
                (str "a refusal naming a file that is not there is "
                     "UNACCOUNTED: "
                     (pr-str (unaccounted-names dir forged))))))
        (finally (cleanup! ws))))))

;; @spec MCP-OP-MEM-013
(deftest a-break-interrupted-between-the-link-and-the-unlink-is-not-a-break
  (testing "Opus round 5, finding 4. A crash between `Files/createLink` and
            the unlink leaves a tombstone that is a second link to the LIVE
            LOCK: same (device, inode), byte-identical content. The sweep
            reported it as `:kind :broken-lock` - evidence of a break that
            never happened, naming a claim that currently holds the lock - and
            a later break by that txid was refused because the name was taken.
            It is an INTERRUPTED break, and recovery finishes it: the holder
            is dead by the same rule every other break obeys, so the LOCK side
            is unlinked and the evidence stands."
    (let [ws (workspace! "interrupted-break-finish" 2)
          dir (io/file (journal/transactions-dir (:root ws) (:state-home ws)))
          opts {:state-home (:state-home ws)}
          lock (io/file dir "LOCK")
          tomb (io/file dir "LOCK.broken.B-CRASH")]
      (try
        (plant-lock! ws {:txid "ghost-8" :pid (reaped-pid) :boot-id (boot-id-now)})
        (let [crashed (begin! ws {:txid "B-CRASH"
                                  :before-unlink
                                  (fn [_]
                                    (throw (java.io.IOException.
                                             "killed between the link and the unlink")))})]
          (is (false? (:ok crashed))
              (str "the break did not complete: " (pr-str crashed)))
          (is (.isFile lock) "the LOCK is still there")
          (is (.isFile tomb) "and so is the half-made tombstone")
          (is (= (file-key lock) (file-key tomb))
              (str "one inode, two names - which is what makes it not a break: "
                   (file-key lock) " vs " (file-key tomb))))

        (let [row (first (filter #(contains? #{:broken-lock :interrupted-break}
                                             (:kind %))
                                 (journal/retained-transactions (:root ws) opts)))]
          (is (= :interrupted-break (:kind row))
              (str "the sweep may not call it a break that happened: "
                   (pr-str row))))

        (let [result (journal/recover! (:root ws) opts)
              finished (first (:interrupted-breaks result))]
          (is (some? finished)
              (str "recovery names what it resolved: " (pr-str result)))
          (is (= :interrupted-break-finished (:resolution finished)))
          (is (= "LOCK.broken.B-CRASH" (:tombstone finished)))
          (is (not (.exists lock))
              "the holder is dead by the ordinary rule, so the break finishes")
          (is (.isFile tomb) "and its evidence stands")
          (is (= "ghost-8" (:txid (read-string (slurp tomb))))
              "still the exact claim that was judged")
          (is (nil? (:lock-broken result))
              "recovery does not claim a break it did not perform"))
        (finally (cleanup! ws))))))

;; @spec MCP-OP-MEM-013
(deftest an-interrupted-break-of-a-LIVE-holders-lock-is-reverted
  (testing "the other half of finding 4. The same half-made tombstone, but the
            LOCK names a holder that is alive - so the break must not be
            finished on its behalf. Recovery removes the extra link and says
            it did; the live holder's LOCK is untouched, and no evidence of a
            break that never happened is left behind for the sweep to bill."
    (let [ws (workspace! "interrupted-break-revert" 2)
          child (.start (ProcessBuilder. ["sleep" "30"]))
          dir (io/file (journal/transactions-dir (:root ws) (:state-home ws)))
          opts {:state-home (:state-home ws)}
          lock (io/file dir "LOCK")
          tomb (io/file dir "LOCK.broken.B-CRASH")]
      (try
        (plant-lock! ws {:txid "A-LIVE" :pid (.pid child) :boot-id (boot-id-now)})
        ;; exactly what a crash between the link and the unlink leaves
        (Files/createLink (.toPath tomb) (.toPath lock))
        (let [result (journal/recover! (:root ws) opts)
              reverted (first (:interrupted-breaks result))]
          (is (some? reverted)
              (str "recovery names what it resolved: " (pr-str result)))
          (is (= :interrupted-break-reverted (:resolution reverted)))
          (is (= "LOCK.broken.B-CRASH" (:tombstone reverted)))
          (is (.isFile lock) "the live holder's LOCK is untouched")
          (is (= "A-LIVE" (:txid (read-string (slurp lock)))))
          (is (not (.exists tomb))
              "and the evidence of a break that never happened is gone")
          (is (nil? (:lock-broken result))
              "nothing was broken"))
        (let [rows (filter #(contains? #{:broken-lock :interrupted-break} (:kind %))
                           (journal/retained-transactions (:root ws) opts))]
          (is (empty? rows)
              (str "and the sweep bills nothing for it: " (pr-str rows))))
        (finally
          (.destroyForcibly child)
          (.waitFor child)
          (cleanup! ws))))))

;; @spec MCP-OP-MEM-013
(deftest the-link-break-cannot-displace-a-claim-at-all
  (testing "the strictly stronger property the createLink order buys. The
            break no longer MOVES the LOCK, so between claiming the tombstone
            name and unlinking the LOCK the claim is in two places rather than
            none: there is no gap for a third acquirer to land in, nothing to
            restore, and nothing to displace. The same injection that produces
            a displaced claim on the break-by-move fallback must produce none
            here - and must leave the live holder's LOCK exactly as it was."
    (let [ws (workspace! "link-break-no-displacement" 2)
          child (.start (ProcessBuilder. ["sleep" "30"]))
          dir (journal/transactions-dir (:root ws) (:state-home ws))
          lock (io/file dir "LOCK")
          before (displaced-count)]
      (try
        (plant-lock! ws {:txid "ghost-7" :pid (reaped-pid) :boot-id (boot-id-now)})
        (let [refused (begin! ws {:txid "B-LINKER"
                                  :before-break
                                  (fn [_]
                                    (.delete lock)
                                    (spit lock (pr-str {:txid "A-LIVE"
                                                        :pid (.pid child)
                                                        :boot-id (boot-id-now)})))})]
          (is (false? (:ok refused)))
          (is (= :txn-lock-held (:error-type refused)))
          (is (nil? (:lock-break-displaced refused))
              (str "nothing was displaced, because nothing was moved: "
                   (pr-str refused)))
          (is (= "A-LIVE" (:txid (read-string (slurp lock))))
              "the live holder's claim is untouched")
          (is (not (.exists (io/file dir "LOCK.broken.B-LINKER")))
              "and the breaker left no evidence of a break that never happened")
          (is (= before (displaced-count))
              "the displacement bucket did not move"))
        (finally
          (.destroyForcibly child)
          (.waitFor child)
          (cleanup! ws))))))

;; @spec MCP-OP-MEM-013
(deftest two-breakers-sharing-a-txid-cannot-destroy-each-others-evidence
  (testing "Opus round 5, finding 3. The tombstone-collision guard was
            `.exists` followed by a `Files/move` carrying ATOMIC_MOVE - a
            check-then-act in front of `rename(2)`, which on POSIX replaces
            unconditionally - and the `FileAlreadyExistsException` catch
            behind it cannot fire on ext4. `begin!` takes `:txid` verbatim
            from its caller, so two breakers sharing one txid raced: measured
            13 of 4,000, the judged claim destroyed and BOTH breakers handed
            `LOCK.broken.SAME-TXID` as the place to find it, a receipt naming
            a subject only one of them owned. The name must be claimed by a
            primitive that is create-if-absent IN THE KERNEL, so exactly one
            breaker owns it and the other is refused."
    (let [dir (temp-dir "tombstone-collision-race")
          lock (io/file dir "LOCK")
          pid (.pid (java.lang.ProcessHandle/current))
          running (atom true)
          hammer-n (atom 0)
          hammer (Thread.
                   (fn []
                     (loop []
                       (when @running
                         (create-lock-if-absent!
                           dir (pr-str {:txid (str "HAMMER-" (swap! hammer-n inc))
                                        :pid pid :boot-id "b"}))
                         (recur)))))
          destroyed (atom 0)
          engaged (atom 0)
          shared-name (atom 0)
          sample (atom nil)
          pool (java.util.concurrent.Executors/newFixedThreadPool 2)]
      (try
        (.start hammer)
        ;; bounded by its OUTCOME - enough rounds in which both breakers
        ;; actually reached the name, or the first destroyed claim - with a
        ;; hard cap so a loaded box cannot turn a race witness into an
        ;; unbounded loop
        (loop [i 0]
          (when (and (< i 4000)
                     (zero? (long @destroyed))
                     (< (long @engaged) 200))
            (doseq [^java.io.File f (.listFiles (io/file dir))]
              (when (.startsWith (.getName f) "LOCK.broken") (.delete f)))
            (.delete lock)
            (let [victim (pr-str {:txid (str "VICTIM-" i) :pid pid :boot-id "b"})]
              (create-lock-if-absent! dir victim)
              (let [judged (@#'journal/read-lock-claim lock)]
                (when (= victim (:content judged))
                  (let [barrier (java.util.concurrent.CyclicBarrier. 2)
                        task (fn []
                               (fn []
                                 (.await barrier)
                                 (@#'journal/break-lock! dir judged "SAME-TXID")))
                        outcomes (mapv #(.get ^java.util.concurrent.Future %)
                                       (.invokeAll pool [(task) (task)]))
                        on-disk (into #{} (keep (fn [^java.io.File f]
                                                  (when (.isFile f)
                                                    (try (slurp f)
                                                         (catch Exception _ nil))))
                                                (.listFiles (io/file dir))))]
                    (when (every? #(not= :lock-vanished (:cause %)) outcomes)
                      (swap! engaged inc))
                    (when (< 1 (count (filter #(true? (:broken %)) outcomes)))
                      (swap! shared-name inc))
                    (when-not (contains? on-disk victim)
                      (swap! destroyed inc)
                      (compare-and-set! sample nil
                                        [i (pr-str outcomes) (pr-str on-disk)]))))))
            (recur (inc i))))
        (reset! running false)
        (.join hammer 5000)

        (is (zero? (long @destroyed))
            (str "a break may never destroy the claim another break judged: "
                 @destroyed " round(s) destroyed it; first: " (pr-str @sample)))
        (is (zero? (long @shared-name))
            (str "and two breakers may never both own one tombstone name: "
                 @shared-name " round(s) did"))
        (is (>= (long @engaged) 200)
            (str "the race must actually be exercised, or it proves nothing: "
                 @engaged " engaged round(s)"))
        (finally
          (reset! running false)
          (.join hammer 5000)
          (.shutdownNow pool)
          (delete-tree! dir))))))

;; @spec MCP-OP-MEM-013
(deftest the-sweep-and-the-prune-read-one-clock-for-a-tombstone
  (testing "Opus round 5, finding 2: this build hardened ONE timestamp read
            and left two others reading the stamp it had just declared
            insufficient. `retained-transactions` bills a tombstone's age from
            `.lastModified` alone, so a `cp -p`, `rsync -t`, `tar -x` or a
            restore from backup - all of which PRESERVE mtime - makes the
            sweep report evidence as past retention while recovery, reading
            the newest of mtime and ctime, correctly keeps it. Two verbs
            disagreeing about one file is a composite state with no
            authority."
    (let [ws (workspace! "tombstone-one-clock" 2)
          dir (io/file (journal/transactions-dir (:root ws) (:state-home ws)))
          opts {:state-home (:state-home ws)}
          copied (io/file dir "LOCK.broken.RESTORED-FROM-BACKUP")]
      (try
        (.mkdirs dir)
        (spit copied (pr-str {:txid "ghost-copied" :pid 1}))
        ;; what `cp -p` leaves: an old mtime on a file created moments ago
        (.setLastModified copied (- (System/currentTimeMillis)
                                    (* 30 24 60 60 1000)))
        (let [row (first (filter #(= "LOCK.broken.RESTORED-FROM-BACKUP" (:txid %))
                                 (journal/retained-transactions (:root ws) opts)))]
          (is (some? row) "the sweep lists it")
          (is (number? (:age-ms row)))
          (is (< (long (:age-ms row)) (long journal/broken-lock-retention-ms))
              (str "and reads its age off the same basis recovery does, or the "
                   "two verbs disagree about one file: " (pr-str row))))
        (let [result (journal/recover! (:root ws) opts)]
          (is (.isFile copied)
              (str "recovery keeps it, which is the reading the sweep must "
                   "agree with: " (pr-str (:broken-locks result)))))
        (finally (cleanup! ws))))))

;; @spec MCP-OP-MEM-013
(deftest the-sweep-never-bills-a-tombstone-that-is-not-there
  (testing "the other half of finding 2. `lastModified` of a missing file is
            0, which the sweep reads as infinitely old and bills as
            `:bytes 0`: a file that vanishes between the listing and the stat
            produced a row with an epoch-sized age for a file that was gone -
            822 of 9,831 rows under concurrency. An ABSENT file is absent, not
            a zero and not infinitely old, and no row may describe one.

            The rule is about ABSENCE, not about zero bytes: a file caught
            between its create and its write is PRESENT and zero-length, and
            since round 6's finding 3 it gets a row typed
            `:status :empty-evidence` - which is exactly the shape a lister
            racing this churn sees, and exactly what the break's own receipt
            and the prune's `:remaining` already said about it."
    (let [ws (workspace! "tombstone-absent-rows" 2)
          dir (io/file (journal/transactions-dir (:root ws) (:state-home ws)))
          opts {:state-home (:state-home ws)}
          running (atom true)
          churn (Thread.
                  (fn []
                    (loop []
                      (when @running
                        (let [made (doall
                                     (for [i (range 40)]
                                       (let [f (io/file dir (str "LOCK.broken.CHURN-" i))]
                                         (spit f (pr-str {:txid (str "ghost-" i) :pid 1}))
                                         f)))]
                          (doseq [^java.io.File f made] (.delete f)))
                        (recur)))))
          rows (atom [])]
      (try
        (.mkdirs dir)
        (.start churn)
        (loop [i 0]
          (when (and (< i 4000) (< (count @rows) 400))
            (swap! rows into (filter #(= :broken-lock (:kind %))
                                     (journal/retained-transactions (:root ws) opts)))
            (recur (inc i))))
        (reset! running false)
        (.join churn 5000)
        (is (>= (count @rows) 400)
            (str "the sweep must actually read the directory while files are "
                 "vanishing, or it proves nothing: " (count @rows) " rows"))
        (let [ghosts (remove #(and (number? (:age-ms %))
                                   (not (neg? (long (:age-ms %))))
                                   (< (long (:age-ms %)) 1000000000000)
                                   (or (pos? (long (:bytes % 0)))
                                       (= :empty-evidence (:status %))))
                             @rows)]
          (is (zero? (count ghosts))
              (str "no row may bill a file that is not there: " (count ghosts)
                   " of " (count @rows) "; first: " (pr-str (first ghosts)))))
        (finally
          (reset! running false)
          (.join churn 5000)
          (cleanup! ws))))))

;; @spec MCP-OP-MEM-013
(deftest the-break-by-move-restore-fallback-refuses-a-lock-that-reappeared
  (testing "the fallback `restore-lock!` takes on a filesystem with no
            `link(2)` at all. It is check-then-act and its docstring says so;
            what it must never do is replace a claim that arrived while the
            tombstone was away."
    (let [dir (temp-dir "restore-fallback")
          lock (io/file dir "LOCK")
          tomb (io/file dir "LOCK.broken.T")]
      (try
        (spit tomb (pr-str {:txid "displaced" :pid 1}))
        (spit lock (pr-str {:txid "third-acquirer" :pid 2}))
        (let [outcome (@#'journal/restore-by-move! tomb lock)]
          (is (false? (:restored outcome))
              (str "it refuses on EEXIST: " (pr-str outcome)))
          (is (= :lock-reappeared (:restore-cause outcome)))
          (is (= :move (:restore-path outcome)))
          (is (= "third-acquirer" (:txid (read-string (slurp lock))))
              "the claim that arrived is untouched")
          (is (= "displaced" (:txid (read-string (slurp tomb))))
              "and the displaced claim is still in its tombstone"))
        (.delete lock)
        (let [outcome (@#'journal/restore-by-move! tomb lock)]
          (is (true? (:restored outcome)) "and it restores when the LOCK is free")
          (is (= "displaced" (:txid (read-string (slurp lock))))))
        (finally (delete-tree! dir))))))

;; @spec MCP-OP-MEM-013
(deftest finishing-a-transaction-does-not-delete-a-lock-it-no-longer-owns
  (testing "the release side of the same defect. `release-lock!` was a bare
            `deleteIfExists`, so a transaction whose claim had been replaced -
            by any of the races above, or by a hand-edited state directory -
            deleted whoever's LOCK it found when it finished. A release must
            unlink its OWN claim and nothing else."
    (let [ws (workspace! "lock-release-scope" 2)
          child (.start (ProcessBuilder. ["sleep" "30"]))
          dir (journal/transactions-dir (:root ws) (:state-home ws))
          lock (io/file dir "LOCK")]
      (try
        (let [txn (begin! ws {})]
          (is (string? (:txid txn)))
          (.delete lock)
          (spit lock (pr-str {:txid "SOMEBODY-ELSE" :pid (.pid child)
                              :boot-id (boot-id-now)}))
          (journal/rollback! txn)
          (is (.isFile lock)
              "finishing must not unlink a claim this transaction does not own")
          (is (= "SOMEBODY-ELSE" (:txid (read-string (slurp lock))))))
        (finally
          (.destroyForcibly child)
          (.waitFor child)
          (cleanup! ws))))))

;; @spec MCP-OP-MEM-013
(deftest a-legacy-format-lock-is-named-and-fails-closed
  (testing "Opus round 3, finding 4: a LOCK written by any earlier build
            carries a pid and nothing else. With no boot id and no start ticks
            both mismatch clauses are dead, so only `:process-not-alive` could
            fire - and a REUSED pid then reads as a live holder for as long as
            that number is in use, which is round 2's permanent deadlock
            reachable on any workspace whose LOCK predates the checkable
            triple. An unverifiable format is an UNKNOWN: it fails closed with
            a typed cause that names the format, never as 'live for ever'."
    (let [ws (workspace! "legacy-lock" 2)
          child (.start (ProcessBuilder. ["sleep" "30"]))
          dir (journal/transactions-dir (:root ws) (:state-home ws))
          lock (io/file dir "LOCK")
          opts {:state-home (:state-home ws)}]
      (try
        (plant-lock! ws {:txid "old-format" :pid (.pid child)})
        (let [refused (begin! ws {})]
          (is (false? (:ok refused)))
          (is (= :txn-lock-held (:error-type refused)))
          (is (= :legacy-format (:holder-cause refused))
              "the cause names the format rather than pretending to a judgement")
          (is (= :pid-only (:holder-format refused)))
          (is (false? (:holder-live refused))
              "it is not claimed to be live either; it is unreadable")
          (is (str/includes? (:error refused) "earlier build"))
          (is (true? (get-in refused [:next_call :break_legacy_lock]))
              "and the refusal names the remedy that can clear it"))
        (is (nil? (:lock-broken (journal/recover! (:root ws) opts)))
            "plain recovery does not break a format it cannot check")
        (is (.isFile lock))
        (is (= "old-format" (:txid (read-string (slurp lock)))))
        (finally
          (.destroyForcibly child)
          (.waitFor child)
          (cleanup! ws))))))

;; @spec MCP-OP-MEM-013
(deftest the-legacy-lock-break-demands-the-receipt-of-the-holders-death
  (testing "the remedy is not a waiver. `recover! :break-legacy-lock true`
            breaks an unverifiable claim only on a receipt of its holder's
            death - the recorded pid naming no live process AND the claim older
            than `legacy-lock-break-age-ms` - and refuses when either half is
            missing. `:now-ms` pins the clock so the age boundary is exact
            rather than a race with the wall."
    (let [ws (workspace! "legacy-lock-break" 2)
          child (.start (ProcessBuilder. ["sleep" "30"]))
          dir (journal/transactions-dir (:root ws) (:state-home ws))
          lock (io/file dir "LOCK")
          opts {:state-home (:state-home ws) :break-legacy-lock true}]
      (try
        ;; half one missing: the recorded pid is ALIVE, however old the claim
        (plant-lock! ws {:txid "legacy-live" :pid (.pid child)})
        (let [stamp (lock-age-basis lock)]
          (is (nil? (:lock-broken (journal/recover!
                                    (:root ws)
                                    (assoc opts :now-ms (+ stamp
                                                           (* 10 journal/legacy-lock-break-age-ms))))))
              "a live recorded pid is not a receipt of death at any age")
          (is (.isFile lock)))
        (.delete lock)

        ;; half two missing: the pid is dead, the claim is one millisecond
        ;; short of the age the remedy requires
        (let [dead (reaped-pid)]
          (plant-lock! ws {:txid "legacy-dead" :pid dead})
          (let [stamp (lock-age-basis lock)]
          (is (nil? (:lock-broken (journal/recover!
                                    (:root ws)
                                    (assoc opts :now-ms
                                           (+ stamp (dec journal/legacy-lock-break-age-ms))))))
              "one millisecond under the required age is still a refusal")
          (is (.isFile lock))

          ;; exactly at the age, with the pid dead: broken, and named
          (let [recovery (journal/recover!
                           (:root ws)
                           (assoc opts :now-ms (+ stamp journal/legacy-lock-break-age-ms)))]
            (is (= :stale-holder (get-in recovery [:lock-broken :reason])))
            (is (= :legacy-format (get-in recovery [:lock-broken :cause])))
            (is (= dead (get-in recovery [:lock-broken :pid])))
            (is (not (.isFile lock)) "the claim is gone")
            (let [txn (begin! ws {})]
              (is (string? (:txid txn)) "and the workspace is usable again")
              (when (:txid txn) (journal/rollback! txn))))))
        (finally
          (.destroyForcibly child)
          (.waitFor child)
          (cleanup! ws))))))


;; @spec MCP-OP-MEM-013
(deftest a-legacy-locks-age-is-the-newest-of-its-two-stamps
  (testing "Opus round 4, finding 6: the legacy break's receipt has two halves,
            and only ONE of them is checkable. The pid half reads the process
            table. The age half read `lastModified` alone - and mtime is
            settable by any process and is PRESERVED by `cp -p`, `rsync -t`,
            `tar -x` and a restore from backup, while ctime is not: a workspace
            restored from a snapshot presents a legacy LOCK that is old by
            mtime and was created moments ago in this boot. The age must be
            measured against the NEWEST of the two stamps, so that a claim is
            old only when both of them are. And `lastModified` of a file that
            is not there returns 0, which made an absent lock read as
            infinitely old."
    (let [ws (workspace! "legacy-lock-backdated" 2)
          dir (journal/transactions-dir (:root ws) (:state-home ws))
          lock (io/file dir "LOCK")
          opts {:state-home (:state-home ws) :break-legacy-lock true}]
      (try
        (plant-lock! ws {:txid "old-format" :pid (reaped-pid)})
        (.setLastModified lock (- (System/currentTimeMillis)
                                  (* 11 24 60 60 1000)))
        (let [recovery (journal/recover! (:root ws) opts)]
          (is (nil? (:lock-broken recovery))
              (str "a claim whose mtime says eleven days and whose ctime says "
                   "moments ago is not a receipt of a holder's death: "
                   (pr-str recovery)))
          (is (.isFile lock) "so the claim is still there"))
        (is (false? (@#'journal/legacy-lock-dead?
                      (io/file dir "LOCK.not-a-file") {:pid (reaped-pid)} nil))
            "and a lock file that is not there is not infinitely old")
        (finally (cleanup! ws))))))

;; @spec MCP-OP-MEM-014
(deftest the-commit-window-states-its-measured-size-term-not-a-flat-bound
  (testing "Opus round 3, finding 5: the contract value, the module docstring
            and MEM-007 all said the window's bound is `O(1)` in the target's
            size, and this build's own numbers say the 2 MiB window is still
            1.9x the 1 KB one. MEM-014's own rule - every statement about an
            instrument shall be true of it in general - is the rule that
            sentence failed. The claim is now the measurement."
    (let [window journal/commit-window
          contract (journal/contract)]
      (is (string? (:size-term window))
          "the contract value carries the residual size term, not a flat bound")
      (is (str/includes? (:size-term window) "1.9x")
          "stating the measured ratio")
      (is (str/includes? (:size-term window) "not eliminated"))
      (is (not (str/includes? (pr-str contract) "O(1)"))
          "and no receipt claims a bound the instrument does not have")
      (is (= window (:commit-window contract))))))

;; @spec MCP-OP-MEM-006
;; @spec MCP-OP-MEM-007
(deftest undo-refuses-a-target-another-writer-changed-after-the-commit
  (testing "Opus round 2, blocker 3: `undo!` wrote with NEITHER the publish
            lock NOR any recheck. It is a live user-facing operation, not a
            startup path - the one writer inside the kernel that ignored the
            kernel's own lock. Undo is a write, and a write that does not
            recheck clobbers whatever landed after the commit."
    (let [ws (workspace! "undo-conflict" 2)
          path (first (sort (:paths ws)))]
      (try
        (let [{:keys [receipt h0]} (commit-one-change! ws path "(ns f0) (def v :new)\n")
              txid (:txid receipt)
              opts {:state-home (:state-home ws)}
              h1 (bytes-of path)]
          (is (:ok receipt))
          (spit path "SOMEBODY-ELSE\n")
          (let [refused (journal/undo! (:root ws) txid opts)]
            (is (false? (:ok refused)))
            (is (= :txn-undo-conflict (:error-type refused)))
            (is (= path (:path refused)) "the refusal names the path it would have clobbered")
            (is (= :digest (:conflict (first (:conflicts refused)))))
            (is (= 0 (:files-written refused)))
            (is (= "SOMEBODY-ELSE\n" (bytes-of path))
                "the other writer's bytes survive; undo! wrote nothing"))
          (spit path h1)
          (let [undone (journal/undo! (:root ws) txid opts)]
            (is (:ok undone) "once the target is back at H1 the undo is safe again")
            (is (= h0 (bytes-of path)))))
        (finally (cleanup! ws))))))

;; @spec MCP-OP-MEM-007
(deftest undo-waits-for-the-publish-lock-another-process-holds
  (testing "the other half of blocker 3: undo! took 2.5 ms straight through a
            lock another JVM was holding. It must go through the same publish
            lock the commit path takes."
    (let [ws (workspace! "undo-lock" 2)
          path (first (sort (:paths ws)))]
      (try
        (let [{:keys [receipt h0]} (commit-one-change! ws path "(ns f0) (def v :new)\n")
              txid (:txid receipt)
              opts {:state-home (:state-home ws)}
              child (hold-publish-lock! ws 1500)
              started (System/nanoTime)
              undone (journal/undo! (:root ws) txid opts)
              elapsed-ms (quot (- (System/nanoTime) started) 1000000)]
          (is (:ok receipt))
          (is (:ok undone))
          (is (>= elapsed-ms 900)
              (str "undo! must WAIT for the publish lock; it returned after "
                   elapsed-ms " ms"))
          (is (= h0 (bytes-of path)))
          (.waitFor child))
        (finally (cleanup! ws))))))

;; @spec MCP-OP-MEM-006
(deftest a-committed-journal-whose-lease-cannot-be-read-is-not-evictable
  (testing "Opus round 2, blocker 2: the retention refcount FAILED OPEN. A
            missing lease defaulted to `:receipt-refs 0` / `:evictable true`,
            so deleting one file by hand let a quota sweep destroy the
            pre-images and the committed receipt became permanently
            un-undoable. An unknown refcount is not zero."
    (let [ws (workspace! "lease-unreadable" 2)
          path (first (sort (:paths ws)))]
      (try
        (let [h0 (bytes-of path)
              txn (begin! ws {})
              _ (record-scope! txn (:paths ws))
              _ (journal/seal-read-set! txn)
              _ (journal/pin! txn path)
              _ (journal/stage! txn path "(ns f0) (def v :new)\n")
              receipt (journal/commit! txn)
              txid (:txid receipt)
              opts {:state-home (:state-home ws)}
              dir (transaction-dir ws txid)]
          (is (:ok receipt))
          (Files/deleteIfExists (.toPath (io/file dir "lease.edn")))

          (let [row (first (filter #(= txid (:txid %))
                                   (journal/retained-transactions (:root ws) opts)))]
            (is (= :unreadable (:lease row))
                "the sweep's own listing says the refcount could not be read")
            (is (= 1 (:receipt-refs row)) "an unknown refcount is not zero")
            (is (false? (:evictable row))))

          (let [refused (journal/evict! (:root ws) txid opts)]
            (is (false? (:ok refused)))
            (is (= :txn-lease-unreadable (:error-type refused)))
            (is (= :unreadable (:lease refused)) "the receipt names why")
            (is (some? (:next_call refused))))
          (is (= 1 (count (.listFiles (io/file dir "objects"))))
              "the pre-image a refused eviction must not destroy is still there")

          (let [undone (journal/undo! (:root ws) txid opts)]
            (is (:ok undone))
            (is (= h0 (bytes-of path))
                "and the receipt is still undoable, which is the whole point"))

          (spit (io/file dir "lease.edn") "{:txid ")
          (is (= :txn-lease-unreadable
                 (:error-type (journal/evict! (:root ws) txid opts)))
              "an UNPARSABLE lease is the same unknown as a missing one")

          (let [without (journal/forget! (:root ws) txid opts)]
            (is (false? (:ok without))
                "even a deliberate forget! refuses while the refcount is unknown")
            (is (= :txn-lease-unreadable (:error-type without))))

          (let [with-receipt (journal/forget! (:root ws) txid
                                              (assoc opts :receipt receipt))]
            (is (:ok with-receipt)
                "a caller that PRESENTS the commit receipt may forget it")
            (is (not (.exists dir))))

          (is (false? (:ok (journal/undo! (:root ws) txid opts)))
              "and afterwards undo! says it cannot rather than pretending"))
        (finally (cleanup! ws))))))

;; @spec MCP-OP-MEM-006
;; @spec MCP-OP-MEM-013
(deftest a-failed-restoration-keeps-its-journal-and-refuses-eviction
  (testing "the second half of Sol's blocker: a failed rollback deleted the only
            material that could still repair the tree. A restoration that did
            not verify keeps everything and refuses to be evicted."
    (let [ws (workspace! "restore-failed" 3)
          paths (sort (:paths ws))
          [p0 p1] paths]
      (try
        (let [txn (begin! ws {})
              _ (record-scope! txn paths)
              _ (journal/seal-read-set! txn)
              _ (doseq [p [p0 p1]]
                  (journal/pin! txn p)
                  (journal/stage! txn p (str (slurp p) ";; changed\n")))
              txid (:txid txn)
              dir (transaction-dir ws txid)
              receipt (journal/commit!
                        txn {:after-publish
                             (fn [path]
                               ;; the pre-image p0 needs to roll back with is
                               ;; destroyed while the transaction is mid-flight
                               (when (= path p0)
                                 (doseq [object (.listFiles (io/file dir "objects"))]
                                   (Files/delete (.toPath object)))))
                             :publish-fn
                             (fn [target source]
                               (if (= target p1)
                                 (throw (ex-info "injected" {:error-type :injected}))
                                 (journal/publish-prepared! target source)))})]
          (is (false? (:ok receipt)))
          (is (= :txn-write-failed (:error-type receipt)))
          (is (false? (:rolled-back receipt))
              "the restoration could not be verified, and the receipt says so")
          (is (.isDirectory dir)
              "the journal of a failed restoration is NOT deleted: it is the only
               material a human or a later recovery can work from")
          (is (.isFile (io/file dir "journal.log")))
          (is (.isFile (io/file dir "manifest.tsv")))
          (is (= :restore-failed (:status (read-string (slurp (io/file dir "state.edn")))))
              "the state names the failure rather than claiming a clean rollback")

          (let [evicted (journal/evict! (:root ws) txid {:state-home (:state-home ws)})]
            (is (false? (:ok evicted)))
            (is (= :txn-journal-retained (:error-type evicted)))
            (is (.isDirectory dir) "a quota sweep may not reclaim it"))

          (let [forgotten (journal/forget! (:root ws) txid {:state-home (:state-home ws)})]
            (is (false? (:ok forgotten)))
            (is (= :txn-journal-retained (:error-type forgotten))
                "not even an explicit forget! discards unrepaired recovery material")
            (is (.isDirectory dir))))
        (finally (cleanup! ws))))))

;; ------------------------------------------ MCP-OP-MEM-013 crash recovery

(defn- crash-arm
  [ws crash-point]
  (child/run-arm {:main-ns 'clj-surgeon.txn-crash-child
                  :xmx "256m"
                  :args [(:root ws) (:state-home ws) (name crash-point)]}))

;; @spec MCP-OP-MEM-013
(deftest recovery-restores-a-transaction-killed-between-pin-and-rename
  (let [ws (workspace! "crash-pin" 4)
        paths (sort (:paths ws))]
    (try
      (let [h0 (mapv bytes-of paths)
            arm (crash-arm ws :after-pin)]
        (is (not= 0 (:exit arm)) (str "the child must die: " (:out arm)))
        (is (= h0 (mapv bytes-of paths)) "nothing was written before the kill")
        (let [recovery (journal/recover! (:root ws) {:state-home (:state-home ws)})]
          (is (:ok recovery))
          (is (= 1 (:transactions-recovered recovery)))
          (is (= h0 (mapv bytes-of paths)))
          (is (= 0 (count (:paths recovery)))
              "no path had been begun, so recovery restores none")
          (is (:txid (begin! ws {}))
              "recovery releases the lock the dead transaction held")))
      (finally (cleanup! ws)))))

;; @spec MCP-OP-MEM-013
(deftest recovery-restores-a-transaction-killed-between-renames
  (let [ws (workspace! "crash-rename" 4)
        paths (sort (:paths ws))]
    (try
      (let [h0 (mapv bytes-of paths)
            arm (crash-arm ws :between-renames)]
        (is (not= 0 (:exit arm)) (str "the child must die: " (:out arm)))
        (is (not= h0 (mapv bytes-of paths))
            "the kill lands after at least one file was already replaced")
        (let [recovery (journal/recover! (:root ws) {:state-home (:state-home ws)})]
          (is (:ok recovery))
          (is (= h0 (mapv bytes-of paths))
              "every partially applied path is restored to H0")
          (is (= 2 (count (:paths recovery)))
              "recovery restores exactly the paths the journal recorded as begun,
               and an empty restoration list is not a pass")
          (is (every? #(= :verified (:status %)) (:paths recovery)))
          (is (empty? (filter #(str/starts-with? (.getName ^java.io.File %)
                                                 ".clj-surgeon-publish-")
                              (file-seq (io/file (:root ws)))))
              "recovery removes the staging temporaries it left in the tree.
               The old form looked for `.clj-surgeon-txn-`, a prefix no publish
               temporary has ever carried, so it could not have failed.")))
      (finally (cleanup! ws)))))

;; @spec MCP-OP-MEM-013
(deftest recovery-sweeps-only-the-publish-temporaries-its-own-journal-recorded
  (testing "Opus round 2, finding 8: the sweep walked the parent directory of
            every begun path and deleted EVERY `.clj-surgeon-publish-*` sibling.
            Combined with state-home-scoped locking - two state homes on one
            workspace root do not exclude each other - one workspace's recovery
            could delete another in-flight transaction's PREPARED temporary
            between its prepare and its rename. The sweep is now scoped to the
            temp names this journal's own `write-begin` lines recorded."
    (let [ws (workspace! "sweep-scope" 4)
          paths (sort (:paths ws))
          stranger (io/file (.getParentFile (io/file (first paths)))
                            ".clj-surgeon-publish-STRANGER.tmp")]
      (try
        (let [arm (crash-arm ws :between-renames)]
          (is (not= 0 (:exit arm)) (str "the child must die: " (:out arm)))
          (spit stranger "another transaction's prepared bytes\n")
          (let [recovery (journal/recover! (:root ws) {:state-home (:state-home ws)})]
            (is (:ok recovery))
            (is (.isFile stranger)
                "a temporary this journal never recorded is not this recovery's
                 to delete")
            (is (= "another transaction's prepared bytes\n" (slurp stranger))
                "and it is untouched, not merely present")
            (is (empty? (filter #(and (str/starts-with? (.getName ^java.io.File %)
                                                        ".clj-surgeon-publish-")
                                      (not= (.getName ^java.io.File %)
                                            (.getName stranger)))
                                (file-seq (io/file (:root ws)))))
                "while its OWN leftovers are gone")))
        (finally
          (Files/deleteIfExists (.toPath stranger))
          (cleanup! ws))))))

;; @spec MCP-OP-MEM-013
(deftest recovery-is-a-no-op-when-no-transaction-is-unfinished
  (let [ws (workspace! "crash-none" 2)]
    (try
      (let [recovery (journal/recover! (:root ws) {:state-home (:state-home ws)})]
        (is (:ok recovery))
        (is (= 0 (:transactions-recovered recovery))))
      (finally (cleanup! ws)))))

;; --------------------------------- MCP-OP-MEM-013, Opus round 6's residuals

;; @spec MCP-OP-MEM-013
(deftest every-lock-broken-receipt-names-the-primitive-that-took-the-claim
  (testing "Opus round 6, finding 1. Both break primitives produce
            `:break-path` and BOTH callers threw it away - `acquire-lock!` and
            `recover!` kept `(select-keys outcome [:tombstone
            :content-sha256])` - so `begin!` and `recover!` published an
            identical `:lock-broken` line whether the break was the
            kernel-atomic `link(2)` or the check-then-act rename this branch
            measured destroying 13 judged claims in 4,000 races. And the opt
            that selects that rename was `:no-hard-links`, an ordinary
            descriptive word on the PUBLIC `begin!`/`recover!` surface, one
            keyword away from a caller who meant nothing by it. The opt is
            now `:unsafe-break-by-move`, which nobody types by accident, and
            the receipt says which path ran."
    (let [ws (workspace! "break-path-receipt" 2)
          opts {:state-home (:state-home ws)}
          dead (fn [label] {:txid label :pid (reaped-pid) :boot-id (boot-id-now)})]
      (try
        (plant-lock! ws (dead "ghost-atomic"))
        (let [broken (:lock-broken (journal/recover! (:root ws) opts))]
          (is (some? broken) "the dead holder's lock is broken")
          (is (= :link (:break-path broken))
              (str "and the receipt names the kernel-atomic primitive that "
                   "took it: " (pr-str broken))))

        (plant-lock! ws (dead "ghost-move"))
        (let [broken (:lock-broken (journal/recover!
                                     (:root ws)
                                     (assoc opts :unsafe-break-by-move true)))]
          (is (some? broken))
          (is (= :move (:break-path broken))
              (str "the fallback is reachable only under a name that says it "
                   "is unsafe, and the receipt echoes it: " (pr-str broken))))

        (plant-lock! ws (dead "ghost-retired-name"))
        (let [broken (:lock-broken (journal/recover!
                                     (:root ws)
                                     (assoc opts :no-hard-links true)))]
          (is (= :link (:break-path broken))
              (str "and the retired name selects nothing: an unknown option "
                   "may not silently choose the measured check-then-act path: "
                   (pr-str broken))))

        (plant-lock! ws (dead "ghost-begin"))
        (let [txn (begin! ws {:unsafe-break-by-move true})]
          (is (= :move (:break-path (:lock-broken txn)))
              (str "begin! carries it too, or the caller that broke a lock "
                   "cannot tell how: " (pr-str (:lock-broken txn))))
          (journal/rollback! txn))
        (finally (cleanup! ws))))))

;; @spec MCP-OP-MEM-013
(deftest a-stamp-in-the-future-is-unreadable-and-an-age-is-never-negative
  (testing "Opus round 6, finding 2. Retention is measured against the NEWEST
            of the file's basis and the sidecar's `:broken-at-ms`, which is
            fail-safe against a stamp in the PAST and fail-open against one in
            the FUTURE: any writer of the transactions directory - or a clock
            that steps forward once and back - makes a tombstone permanent and
            publishes `:age-ms -315360000000`, a negative age no clock
            produced, unclamped and untyped, while MEM-013 promises evidence
            'bound by a published retention age that recovery enforces'. Sol's
            shape at the published tolerance: one millisecond under it is a
            stamp, the tolerance itself is not a time."
    (let [ws (workspace! "future-stamp" 2)
          dir (io/file (journal/transactions-dir (:root ws) (:state-home ws)))
          opts {:state-home (:state-home ws)}
          tomb (io/file dir "LOCK.broken.FORGED-FUTURE")
          side (io/file dir "LOCK.broken-at.FORGED-FUTURE")
          ;; ten years ahead, exactly the reviewer's forgery
          base (+ (System/currentTimeMillis) (* 10 365 24 60 60 1000))
          row (fn [now-ms]
                (first (filter #(= "LOCK.broken.FORGED-FUTURE" (:txid %))
                               (journal/retained-transactions
                                 (:root ws) (cond-> opts now-ms (assoc :now-ms now-ms))))))]
      (try
        (.mkdirs dir)
        (spit tomb (pr-str {:txid "forged" :pid 1}))
        (spit side (pr-str {:tombstone "LOCK.broken.FORGED-FUTURE"
                            :broken-at-ms base}))

        (let [r (row nil)]
          (is (some? r) "the sweep lists it")
          (is (not (neg? (long (:age-ms r))))
              (str "a published age is never negative: " (pr-str r))))

        (let [under (row (- base journal/broken-lock-stamp-tolerance-ms -1))]
          (is (= :ok (:stamp under))
              (str "one millisecond UNDER the tolerance is an ordinary stamp: "
                   (pr-str under)))
          (is (zero? (long (:age-ms under)))
              (str "and an age that would be negative is clamped at 0, never "
                   "published as a negative number: " (pr-str under))))

        (let [at (row (- base journal/broken-lock-stamp-tolerance-ms))]
          (is (= :unreadable (:stamp at))
              (str "AT the tolerance the stamp is not a time, and the row says "
                   "so: " (pr-str at)))
          (is (> (long (:age-ms at)) (long journal/broken-lock-retention-ms))
              (str "the age falls back to the file's own basis, which is what "
                   "makes the published bound enforceable: " (pr-str at))))

        (let [result (journal/recover!
                       (:root ws)
                       (assoc opts :now-ms (- base journal/broken-lock-stamp-tolerance-ms)))]
          (is (= 1 (:pruned (:broken-locks result)))
              (str "and recovery retires it: a bound any writer of the "
                   "directory can lift is not a bound: "
                   (pr-str (:broken-locks result))))
          (is (not (.exists tomb)))
          (is (not (.exists side)) "the sidecar goes with the evidence it stamped"))
        (finally (cleanup! ws))))))

;; @spec MCP-OP-MEM-013
(deftest an-orphan-stamp-sidecar-is-listed-and-retired-like-the-evidence-it-stamped
  (testing "the smaller half of finding 2. A `LOCK.broken-at.*` whose
            tombstone was removed by anything but the prune or the revert is
            listed by nothing and pruned by nothing - a small unbounded
            accumulation of exactly the kind `broken-lock-retention-ms` exists
            to prevent, in the same directory it was written to bound."
    (let [ws (workspace! "orphan-sidecar" 2)
          dir (io/file (journal/transactions-dir (:root ws) (:state-home ws)))
          opts {:state-home (:state-home ws)}
          side (io/file dir "LOCK.broken-at.ORPHAN")
          rows (fn [] (filter #(= :orphan-sidecar (:kind %))
                              (journal/retained-transactions (:root ws) opts)))]
      (try
        (.mkdirs dir)
        (spit side (pr-str {:tombstone "LOCK.broken.ORPHAN"
                            :broken-at-ms (System/currentTimeMillis)}))
        (let [r (first (rows))]
          (is (some? r)
              (str "the sweep lists the orphan: " (pr-str (rows))))
          (is (= "LOCK.broken-at.ORPHAN" (:txid r)))
          (is (= :txn/recover (:retired-by r))
              "and names the verb that retires it"))

        (let [counted (:orphan-sidecars (:broken-locks (journal/recover! (:root ws) opts)))]
          (is (= {:found 1 :pruned 0 :remaining 1} counted)
              (str "recovery counts it in a standing bucket, fresh: "
                   (pr-str counted)))
          (is (.isFile side) "and keeps it while it is inside retention"))

        (let [counted (:orphan-sidecars
                        (:broken-locks
                          (journal/recover!
                            (:root ws)
                            (assoc opts :now-ms (+ (System/currentTimeMillis)
                                                   (* 2 journal/broken-lock-retention-ms))))))]
          (is (= {:found 1 :pruned 1 :remaining 0} counted)
              (str "and retires it on the SAME published retention: "
                   (pr-str counted)))
          (is (not (.exists side))))
        (finally (cleanup! ws))))))

;; @spec MCP-OP-MEM-013
(deftest a-zero-length-tombstone-is-listed-by-the-sweep-that-counts-it
  (testing "Opus round 6, finding 3. An externally created empty LOCK - the
            kernel never leaves one, `write-lock!` links a fully written
            temporary into place - is broken by `recover!` on the
            `:no-recorded-holder` cause, and the tombstone it leaves is
            zero-length. `:lock-broken` NAMES that file, `:broken-locks`
            counts it in `:found` and `:remaining`, and the listing dropped it
            on `(pos? bytes)`: three verbs, one file, two of them saying it is
            there and the third saying it is not, while MEM-013 requires the
            evidence be visible to the same listing that reads retained
            journals. No claim is lost - an empty LOCK carries none - what is
            lost is agreement, and a receipt naming a file the listing denies
            is the shape this branch keeps producing. Both count it, typed
            `:status :empty-evidence`, because the alternative (neither names
            it) hides the fact that a lock was broken at all; a file that
            VANISHED between the listing and the stat still gets no row, which
            is the rule the 822 ghost rows bought."
    (let [ws (workspace! "empty-tombstone" 2)
          dir (io/file (journal/transactions-dir (:root ws) (:state-home ws)))
          opts {:state-home (:state-home ws)}
          lock (io/file dir "LOCK")]
      (try
        (.mkdirs dir)
        (spit lock "")
        (is (zero? (.length lock)) "a foreign, zero-length LOCK")
        (let [result (journal/recover! (:root ws) opts)
              named (:tombstone (:lock-broken result))
              tomb (io/file dir ^String named)]
          (is (string? named)
              (str "the break names its evidence: " (pr-str result)))
          (is (.isFile tomb))
          (is (zero? (.length tomb)) "and that evidence is zero-length")
          (is (= 1 (:found (:broken-locks result)))
              (str "the prune counts it: " (pr-str (:broken-locks result))))
          (let [row (first (filter #(= named (:txid %))
                                   (journal/retained-transactions (:root ws) opts)))]
            (is (some? row)
                (str "and so does the listing, or a receipt names a file the "
                     "sweep denies: "
                     (pr-str (journal/retained-transactions (:root ws) opts))))
            (is (= :broken-lock (:kind row)))
            (is (= :empty-evidence (:status row))
                (str "typed, because a tombstone carrying no claim is not the "
                     "same evidence as one that does: " (pr-str row)))
            (is (zero? (long (:bytes row))))
            (is (number? (:age-ms row)))))
        (finally (cleanup! ws))))))

;; @spec MCP-OP-MEM-013
(deftest an-interrupted-break-stays-typed-after-its-holder-releases-the-lock
  (testing "Opus round 6, finding 4(i), probe A. An interrupted break was
            recognised ONLY by sharing an inode with the live LOCK, which is
            evidence that expires: the moment the wrongly-broken holder
            releases its own claim - cleanly, txid-scoped, exactly as it
            should - the inode is gone and the sweep silently re-types the
            tombstone as `:kind :broken-lock :status :lock-broken`. A break
            that never happened is then billed for 24 hours as evidence of one,
            naming a holder that released cleanly, blocking every later break
            by that txid with `:tombstone-exists`, and `recover!` reports
            `{:found 1 :remaining 1}` with no resolution at all. State
            inferred from a neighbour dies with the neighbour: an interrupted
            break must be typed by its OWN marker.

            REVISED at round 8, finding 1. Round seven closed this by
            REVERTING on the marker alone, and that made the marker the first
            mechanism on this branch whose forgery DELETED evidence: this
            state and a COMPLETED break wearing a hand-written `:phase
            :linked` sidecar are the same bytes on disk, so a rule that
            deletes one deletes the other. The marker still types the file -
            it is never silently re-typed as a break that happened, and
            recovery still names it and says what it did - but with no LOCK to
            corroborate it the resolution is UNCORROBORATED: stamped with its
            own creation time and KEPT, on the ordinary published retention.
            A duplicate tombstone that retires in a day is the price of never
            deleting a real break's evidence on a claim any directory writer
            can forge."
    (let [ws (workspace! "interrupted-after-release" 2)
          child (.start (ProcessBuilder. ["sleep" "30"]))
          dir (journal/transactions-dir (:root ws) (:state-home ws))
          opts {:state-home (:state-home ws)}
          lock (io/file dir "LOCK")
          tomb (io/file dir "LOCK.broken.BRK-INTERRUPTED")
          row (fn [] (first (filter #(= "LOCK.broken.BRK-INTERRUPTED" (:txid %))
                                    (journal/retained-transactions (:root ws) opts))))]
      (try
        (plant-lock! ws {:txid "A-LIVE" :pid (.pid child) :boot-id (boot-id-now)})
        (let [claim (@#'journal/read-lock-claim lock)
              outcome (@#'journal/break-lock!
                        dir claim "BRK-INTERRUPTED"
                        {:before-unlink
                         (fn [_] (throw (java.io.IOException.
                                          "killed between the link and the unlink")))})]
          (is (= :break-failed (:cause outcome))
              (str "the break died inside the window: " (pr-str outcome)))
          (is (.isFile tomb) "the half-made tombstone is there")
          (is (.isFile lock) "and so is the LOCK it was linked from"))

        (is (= :interrupted-break (:kind (row)))
            (str "typed while the LOCK is still there: " (pr-str (row))))

        (is (true? (@#'journal/release-lock! dir "A-LIVE"))
            "now the wrongly-broken holder releases its OWN claim, cleanly")
        (is (not (.exists lock)))

        (let [after (row)]
          (is (= :uncorroborated-marker (:status after))
              (str "the marker still types it - it is never silently re-typed "
                   "as a break that happened: " (pr-str after)))
          (is (= :broken-lock (:kind after))
              (str "but with no LOCK to corroborate it, the file is typed as "
                   "the break it may well be: " (pr-str after))))

        (let [result (journal/recover! (:root ws) opts)
              resolved (first (:interrupted-breaks result))]
          (is (= 1 (count (:interrupted-breaks result)))
              (str "recovery resolves it rather than passing over it: "
                   (pr-str result)))
          (is (= "LOCK.broken.BRK-INTERRUPTED" (:tombstone resolved)))
          (is (= :interrupted-break-uncorroborated (:resolution resolved))
              (str "the claim it was linked from is gone, so nothing can "
                   "corroborate the marker - and a marker any directory "
                   "writer can forge is not grounds to delete evidence: "
                   (pr-str resolved)))
          (is (= :retained (:evidence resolved)) (pr-str resolved))
          (is (.isFile tomb) "the evidence is kept")
          (is (= 1 (:found (:broken-locks result)))
              (str "counted, on the ordinary published retention: "
                   (pr-str (:broken-locks result)))))

        (let [late (journal/recover!
                     (:root ws)
                     (assoc opts :now-ms (+ (System/currentTimeMillis)
                                            (* 25 60 60 1000))))]
          (is (= 1 (:pruned (:broken-locks late)))
              (str "and it retires like any other evidence rather than "
                   "accumulating: " (pr-str (:broken-locks late))))
          (is (not (.exists tomb))))
        (finally
          (.destroyForcibly child)
          (.waitFor child)
          (cleanup! ws))))))

;; @spec MCP-OP-MEM-013
(deftest recovery-resolves-every-interrupted-break-in-one-call
  (testing "Opus round 6, finding 4(ii), probe M1. `interrupted-break-file`
            took the FIRST match while the prune excluded ALL of them, so with
            two interrupted breaks at once the listing typed one and published
            the other as `:kind :broken-lock :status :lock-broken` - false
            evidence of a break, naming the claim that currently holds the
            lock - and two `recover!` calls were needed to clear them.
            Self-healing, and wrong while it lasts. One recovery resolves
            every one of them, and both typing rules find them: the marker
            written at link time, and the inode a crash before that marker
            leaves."
    (let [ws (workspace! "interrupted-many" 2)
          child (.start (ProcessBuilder. ["sleep" "30"]))
          dir (journal/transactions-dir (:root ws) (:state-home ws))
          opts {:state-home (:state-home ws)}
          lock (io/file dir "LOCK")
          tomb-a (io/file dir "LOCK.broken.BRK-A")
          tomb-b (io/file dir "LOCK.broken.BRK-B")
          rows (fn [] (into {} (map (juxt :txid :kind))
                            (filter #(contains? #{:broken-lock :interrupted-break}
                                                (:kind %))
                                    (journal/retained-transactions (:root ws) opts))))]
      (try
        (plant-lock! ws {:txid "A-LIVE" :pid (.pid child) :boot-id (boot-id-now)})
        ;; the first through the real break, which marks its own window
        (let [claim (@#'journal/read-lock-claim lock)]
          (@#'journal/break-lock!
            dir claim "BRK-A"
            {:before-unlink (fn [_] (throw (java.io.IOException. "crash")))}))
        ;; the second exactly as a crash BEFORE the marker leaves it
        (Files/createLink (.toPath tomb-b) (.toPath lock))
        (is (.isFile tomb-a))
        (is (.isFile tomb-b))

        (is (= {"LOCK.broken.BRK-A" :interrupted-break
                "LOCK.broken.BRK-B" :interrupted-break}
               (rows))
            (str "EVERY interrupted break is typed, not the first of them: "
                 (pr-str (rows))))

        (let [result (journal/recover! (:root ws) opts)]
          (is (= 2 (count (:interrupted-breaks result)))
              (str "and ONE recovery resolves all of them: " (pr-str result)))
          (is (= #{"LOCK.broken.BRK-A" "LOCK.broken.BRK-B"}
                 (set (map :tombstone (:interrupted-breaks result)))))
          (is (every? #(= :interrupted-break-reverted (:resolution %))
                      (:interrupted-breaks result))
              "the holder is alive, so neither break is finished on its behalf")
          (is (not (.exists tomb-a)))
          (is (not (.exists tomb-b)))
          (is (.isFile lock) "and the live holder's LOCK is untouched")
          (is (= "A-LIVE" (:txid (read-string (slurp lock)))))
          (is (zero? (:found (:broken-locks result)))
              (str "nothing is billed: " (pr-str (:broken-locks result)))))
        (finally
          (.destroyForcibly child)
          (.waitFor child)
          (cleanup! ws))))))

;; @spec MCP-OP-MEM-013
(deftest a-completed-break-wearing-a-linked-marker-keeps-its-evidence
  (testing "Opus round 7, finding 1, probes FORGE and AI. The `:phase :linked`
            marker is a CLAIM BY THE BREAKER, and any writer of the
            transactions directory can write one - the same threat model the
            forgeable stamp was fixed for one commit earlier. Dropped beside a
            COMPLETED break it made `recover!` type real evidence as an
            interrupted break and REVERT it: the sidecar and the tombstone
            both unlinked, `:broken-locks {:found 0}`, and a break that
            genuinely happened erased from the directory whose whole purpose
            is bounded, counted, KEPT evidence. Round six's worst forgery kept
            a file for ever - fail-safe; this one removed it. A marker the
            LOCK cannot corroborate - the LOCK gone, or naming a different
            inode - is UNCORROBORATED: the evidence is stamped with its own
            creation time and KEPT, typed so a reader can see the marker was
            not believed, and retired on the ordinary retention. The revert is
            reserved for a match the inode rule confirms."
    (let [ws (workspace! "marker-uncorroborated" 2)
          dir (io/file (journal/transactions-dir (:root ws) (:state-home ws)))
          opts {:state-home (:state-home ws)}
          lock (io/file dir "LOCK")
          row (fn [name] (first (filter #(= name (:txid %))
                                        (journal/retained-transactions
                                          (:root ws) opts))))]
      (try
        (plant-lock! ws {:txid "CRASHED-HOLDER"
                         :pid (reaped-pid)
                         :boot-id (boot-id-now)})
        (let [broken (:lock-broken (journal/recover! (:root ws) opts))
              tomb-name (:tombstone broken)
              tomb (io/file dir ^String tomb-name)
              side (io/file dir (str "LOCK.broken-at."
                                     (subs tomb-name (count "LOCK.broken."))))]
          (is (string? tomb-name)
              (str "a REAL break happened: " (pr-str broken)))
          (is (not (.exists lock)) "the stale claim is gone")
          (is (.isFile tomb) "and its evidence is on disk")

          ;; one hand-written sidecar - a directory writer's forgery, and
          ;; exactly what a swallowed stamp failure used to leave behind
          (spit side (pr-str {:tombstone tomb-name
                              :phase :linked
                              :linked-at-ms (System/currentTimeMillis)}))

          (is (= :broken-lock (:kind (row tomb-name)))
              (str "a marker no LOCK corroborates does not make real evidence "
                   "an interrupted break: " (pr-str (row tomb-name))))
          (is (= :uncorroborated-marker (:status (row tomb-name)))
              (str "and the row says the marker was not believed: "
                   (pr-str (row tomb-name))))

          (let [result (journal/recover! (:root ws) opts)
                resolved (first (filter #(= tomb-name (:tombstone %))
                                        (:interrupted-breaks result)))]
            (is (.isFile tomb)
                (str "the break's own evidence survives the forgery: "
                     (pr-str result)))
            (is (some? resolved)
                (str "recovery names what it did with it: " (pr-str result)))
            (is (= :interrupted-break-uncorroborated (:resolution resolved))
                (pr-str resolved))
            (is (= :retained (:evidence resolved))
                (str "and says the file is still there: " (pr-str resolved)))
            (is (= :marker-uncorroborated (:cause resolved))
                (pr-str resolved))
            (is (= 1 (:found (:broken-locks result)))
                (str "it is billed as the break it is: "
                     (pr-str (:broken-locks result))))
            (is (= 1 (:remaining (:broken-locks result)))
                (pr-str (:broken-locks result))))

          ;; and it is not permanent: the resolve stamps it, so it retires on
          ;; the ordinary published retention like any other break's evidence
          (let [late (journal/recover!
                       (:root ws)
                       (assoc opts :now-ms (+ (System/currentTimeMillis)
                                              (* 25 60 60 1000))))]
            (is (= 1 (:pruned (:broken-locks late)))
                (str "retained on the NORMAL retention, not for ever: "
                     (pr-str (:broken-locks late))))
            (is (not (.exists tomb)))))
        (finally (cleanup! ws))))))

;; @spec MCP-OP-MEM-013
(deftest a-break-that-cannot-record-itself-does-not-happen
  (testing "Opus round 7, finding 1, the other half. `stamp-broken-at!`
            swallowed its own write failure and returned the clock regardless,
            so `break-by-link!` unlinked the LOCK on a success it had not
            achieved: the claim was destroyed and NOTHING on disk recorded
            that a break had taken it. Worse, the `:phase :linked` marker the
            failed write was supposed to overwrite stayed, so the completed
            break wore the marker of an interrupted one and the next recovery
            reverted it - the path by which a real break's evidence was
            deleted. A break the kernel could not record is a break that did
            not happen: the write failure is typed, the LOCK is left exactly
            as it was, our own extra link is dropped, and the refusal is
            reported to the caller rather than swallowed."
    (let [ws (workspace! "break-evidence-unrecordable" 2)
          dir (io/file (journal/transactions-dir (:root ws) (:state-home ws)))
          opts {:state-home (:state-home ws)}
          lock (io/file dir "LOCK")
          ;; the sidecar the break must write is replaced by a DIRECTORY
          ;; between the recheck and the stamp: an ordinary I/O failure at the
          ;; one write that records the break, with the link already claimed
          block! (fn [^java.io.File tomb]
                   (let [side (io/file dir (str "LOCK.broken-at."
                                                (subs (.getName tomb)
                                                      (count "LOCK.broken."))))]
                     (Files/deleteIfExists (.toPath side))
                     (.mkdirs side)
                     (spit (io/file side "occupied") "x")))]
      (try
        (plant-lock! ws {:txid "CRASHED-HOLDER"
                         :pid (reaped-pid)
                         :boot-id (boot-id-now)})
        (let [before (slurp lock)
              result (journal/recover! (:root ws)
                                       (assoc opts :before-unlink block!))
              tombs (filter #(str/starts-with? (.getName ^java.io.File %)
                                               "LOCK.broken.")
                            (seq (.listFiles dir)))]
          (is (nil? (:lock-broken result))
              (str "a break that could not write its own evidence is not a "
                   "break that happened: " (pr-str result)))
          (is (= :evidence-unrecordable
                 (get-in result [:lock-break-refused :cause]))
              (str "and the refusal reaches the caller, typed: "
                   (pr-str result)))
          (is (.isFile lock)
              "the claim it could not record taking is exactly where it was")
          (is (= before (slurp lock))
              "byte for byte")
          (is (empty? tombs)
              (str "and the half-made evidence is not left behind: "
                   (pr-str (map #(.getName ^java.io.File %) tombs)))))
        (finally (cleanup! ws))))))

;; @spec MCP-OP-MEM-013
(deftest the-break-claims-its-marker-before-it-claims-the-name
  (testing "Opus round 7, finding 2, probes AW1 and AZ. The marker was written
            AFTER `Files/createLink`, so between those two statements a break
            left a tombstone with NO marker: typed `:interrupted-break` while
            the LOCK was there by the inode rule alone, and re-typed
            `:kind :broken-lock :status :lock-broken` the moment the
            wrongly-broken holder released its own claim - round six's defect,
            narrowed to one `spit` rather than eliminated. Reachable without a
            crash in a two-statement window, too, because `spit` TRUNCATES
            before it writes: a process killed mid-write left a zero-length
            marker that `break-phase` read as absent.

            The order is the fix. The marker is claimed FIRST, with the same
            create-if-absent primitive the break itself uses, so from the
            instant the tombstone link exists its marker is already on disk
            and the window contains no state neither typing rule can see. What
            an interruption before the link leaves is a marker with no
            tombstone - an ORPHAN SIDECAR, which this branch already lists,
            counts and retires. And both sidecar writes are atomic renames
            rather than truncating `spit`s, so a partially written marker is
            not a state on disk at all."
    (let [ws (workspace! "marker-before-link" 2)
          dir (io/file (journal/transactions-dir (:root ws) (:state-home ws)))
          opts {:state-home (:state-home ws)}
          lock (io/file dir "LOCK")
          observed (atom nil)
          sidecar (fn [^java.io.File tomb]
                    (io/file dir (str "LOCK.broken-at."
                                      (subs (.getName tomb)
                                            (count "LOCK.broken.")))))]
      (try
        (plant-lock! ws {:txid "CRASHED-HOLDER"
                         :pid (reaped-pid)
                         :boot-id (boot-id-now)})
        (let [result (journal/recover!
                       (:root ws)
                       (assoc opts :before-link
                              (fn [^java.io.File tomb]
                                (let [side (sidecar tomb)]
                                  (reset! observed
                                          {:marker (when (.isFile side)
                                                     (:phase (read-string (slurp side))))
                                           :sidecar-bytes (when (.isFile side) (.length side))
                                           :tombstone-present (.exists tomb)})))))
              tomb-name (:tombstone (:lock-broken result))]
          (is (= :linked (:marker @observed))
              (str "the marker is on disk BEFORE the name is claimed: "
                   (pr-str @observed)))
          (is (false? (:tombstone-present @observed))
              (str "and the tombstone is not there yet, so there is no instant "
                   "at which a link exists without its marker: "
                   (pr-str @observed)))
          (is (pos? (long (:sidecar-bytes @observed 0)))
              (str "written whole, never truncated-then-filled: "
                   (pr-str @observed)))
          (is (string? tomb-name)
              (str "and the break still completes: " (pr-str result)))
          (let [side (sidecar (io/file dir ^String tomb-name))]
            (is (number? (:broken-at-ms (read-string (slurp side))))
                (str "with the marker replaced by the break's own stamp: "
                     (slurp side)))))

        ;; and what an interruption in that window leaves is not a break
        (let [ghost (io/file dir "LOCK.broken-at.GHOST")]
          (spit ghost (pr-str {:tombstone "LOCK.broken.GHOST"
                               :phase :linked
                               :linked-at-ms (System/currentTimeMillis)}))
          (let [row (first (filter #(= "LOCK.broken-at.GHOST" (:txid %))
                                   (journal/retained-transactions (:root ws) opts)))]
            (is (= :orphan-sidecar (:kind row))
                (str "a marker with no tombstone is an orphan sidecar, which "
                     "this branch lists: " (pr-str row))))
          (let [late (journal/recover!
                       (:root ws)
                       (assoc opts :now-ms (+ (System/currentTimeMillis)
                                              (* 25 60 60 1000))))]
            (is (= 1 (:pruned (:orphan-sidecars (:broken-locks late))))
                (str "counts, and retires on the published retention: "
                     (pr-str (:broken-locks late))))
            (is (not (.exists ghost)))))
        (finally (cleanup! ws))))))

;; @spec MCP-OP-MEM-013
(deftest an-unreadable-stamp-is-counted-by-the-sweep
  (testing "Opus round 7, finding 4, probes F and F59. A stamp
            `broken-lock-stamp-tolerance-ms` or more ahead of the clock is
            correctly refused as a time - but the fact was typed PER ROW in
            `retained-transactions` and counted by NOTHING, so a caller had to
            go and read the rows to learn it. The tolerance's own justification
            is scoped to `two writers of one directory on one host`, and the
            deployment that breaks it is two hosts sharing a filesystem: every
            tombstone the skewed host writes then reads `:stamp :unreadable`,
            silently, with `:broken-locks` unchanged. Harmless to retention -
            the basis falls back to the file's own and the age clamps to
            zero, so the file retires late rather than early - and invisible to
            the operator whose clock has drifted. That is the same defect class
            as the `:vanished` bucket the 822 ghost rows produced: a condition
            typed per row and absent from the standing count. A non-zero
            `:unreadable-stamps` is an alarm about a clock, not an archive."
    (let [skewed
          (fn [label skew-ms]
            (let [ws (workspace! label 2)
                  dir (io/file (journal/transactions-dir (:root ws) (:state-home ws)))
                  opts {:state-home (:state-home ws)}]
              (try
                (plant-lock! ws {:txid "CRASHED-HOLDER"
                                 :pid (reaped-pid)
                                 :boot-id (boot-id-now)})
                (let [tomb-name (:tombstone (:lock-broken
                                              (journal/recover! (:root ws) opts)))
                      side (io/file dir (str "LOCK.broken-at."
                                             (subs tomb-name
                                                   (count "LOCK.broken."))))]
                  ;; the skewed host wrote BOTH halves from its own clock
                  (spit side (pr-str {:tombstone tomb-name
                                      :broken-at-ms (+ (System/currentTimeMillis)
                                                       skew-ms)}))
                  {:row (first (filter #(= tomb-name (:txid %))
                                       (journal/retained-transactions
                                         (:root ws) opts)))
                   :bucket (:broken-locks (journal/recover! (:root ws) opts))})
                (finally (cleanup! ws)))))
          ahead (skewed "stamp-skew-61s" 61000)
          under (skewed "stamp-skew-59s" 59000)]
      (is (= :unreadable (:stamp (:row ahead)))
          (str "61 s of skew is not a time: " (pr-str (:row ahead))))
      (is (= 1 (:unreadable-stamps (:bucket ahead)))
          (str "and the sweep COUNTS it, so an operator does not have to read "
               "the rows to find out the stamp mechanism stopped working: "
               (pr-str (:bucket ahead))))
      (is (= 1 (:remaining (:bucket ahead)))
          (str "the evidence is still standing, not retired early: "
               (pr-str (:bucket ahead))))
      (is (= :ok (:stamp (:row under)))
          (str "59 s of skew is within the published tolerance: "
               (pr-str (:row under))))
      (is (zero? (long (:unreadable-stamps (:bucket under) -1)))
          (str "and the count is a zero that is present, not an absent key: "
               (pr-str (:bucket under)))))))
