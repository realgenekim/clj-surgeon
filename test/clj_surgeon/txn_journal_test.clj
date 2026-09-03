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
    (let [dir (temp-dir "lock-restore-storm")
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
          (let [outcome (@#'journal/break-lock! dir judged (str "BRK-" i))
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
              (when (.startsWith (.getName f) "LOCK.broken.") (.delete f))))
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
        (.setLastModified stale (- (System/currentTimeMillis)
                                   (* 30 24 60 60 1000)))
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

        (let [result (journal/recover! (:root ws) opts)
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
(defn- receipt-file-names
  "Every value in a receipt that NAMES a file in the transactions directory.

   Walked rather than enumerated, so a key added later is covered by the
   invariant on the day it appears rather than on the day someone remembers
   this witness."
  [receipt]
  (let [names (atom [])]
    (walk/postwalk
      (fn [x]
        (when (and (map-entry? x)
                   (contains? #{:tombstone} (key x))
                   (string? (val x)))
          (swap! names conj (val x)))
        x)
      receipt)
    @names))

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
            investigation it exists to start. Every path-naming key in the
            receipt, and every tombstone row the sweep lists beside it, must
            resolve to a file that exists when the call returns."
    (let [ws (workspace! "receipt-names-exist" 2)
          dir (io/file (journal/transactions-dir (:root ws) (:state-home ws)))
          lock (io/file dir "LOCK")
          opts {:state-home (:state-home ws)}]
      (try
        (plant-lock! ws {:txid "CRASHED-HOLDER"
                         :pid (reaped-pid)
                         :boot-id (boot-id-now)})
        (.setLastModified lock (- (System/currentTimeMillis) (* 2 24 60 60 1000)))
        (let [result (journal/recover! (:root ws) opts)
              named (receipt-file-names result)
              missing (remove #(.isFile (io/file dir ^String %)) named)]
          (is (seq named)
              (str "the receipt names at least one file: " (pr-str result)))
          (is (empty? missing)
              (str "and every name it carries exists at return; missing: "
                   (pr-str missing) " in " (pr-str result))))
        (let [rows (filter #(contains? #{:broken-lock :interrupted-break} (:kind %))
                           (journal/retained-transactions (:root ws) opts))
              missing (remove #(.isFile (io/file dir ^String (:txid %))) rows)]
          (is (empty? missing)
              (str "and the sweep lists no row for a file that is not there: "
                   (pr-str missing))))
        (finally (cleanup! ws))))))

;; @spec MCP-OP-MEM-013
(deftest the-no-hard-links-restore-fallback-refuses-a-lock-that-reappeared
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
          (is (= :no-hard-links (:restore-path outcome)))
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
