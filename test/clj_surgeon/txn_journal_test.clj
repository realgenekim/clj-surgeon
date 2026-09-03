(ns clj-surgeon.txn-journal-test
  "Witnesses for the disk-journaled transaction kernel.

   Every ceiling witness has Sol's shape: the request exactly at the limit
   succeeds, and the request one unit past it refuses BEFORE the effect the
   limit exists to bound. Every conflict witness asserts the observable
   outcome a caller sees - bytes on disk and a typed error - never an internal
   call."
  (:require
   [clj-surgeon.memory.child :as child]
   [clj-surgeon.scope-stream :as scope]
   [clj-surgeon.txn-journal :as journal]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]])
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
          (is (empty? (filter #(str/includes? (.getName ^java.io.File %)
                                              ".clj-surgeon-txn-")
                              (file-seq (io/file (:root ws)))))
              "recovery removes the staging temporaries it left in the tree")))
      (finally (cleanup! ws)))))

;; @spec MCP-OP-MEM-013
(deftest recovery-is-a-no-op-when-no-transaction-is-unfinished
  (let [ws (workspace! "crash-none" 2)]
    (try
      (let [recovery (journal/recover! (:root ws) {:state-home (:state-home ws)})]
        (is (:ok recovery))
        (is (= 0 (:transactions-recovered recovery))))
      (finally (cleanup! ws)))))
