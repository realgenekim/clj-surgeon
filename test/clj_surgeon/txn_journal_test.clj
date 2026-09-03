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
          (is (= [:recheck-digest :recheck-identity :journal-write-begin :rename]
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
          (is (= [:recheck-digest :recheck-identity :journal-write-begin :rename]
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
