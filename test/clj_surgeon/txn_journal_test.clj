(ns clj-surgeon.txn-journal-test
  "Witnesses for the disk-journaled transaction kernel.

   Every ceiling witness has Sol's shape: the request exactly at the limit
   succeeds, and the request one unit past it refuses BEFORE the effect the
   limit exists to bound. Every conflict witness asserts the observable
   outcome a caller sees - bytes on disk and a typed error - never an internal
   call."
  (:require
   [clj-surgeon.memory.child :as child]
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

;; ------------------------------------------------- MCP-OP-MEM-012 retention

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
