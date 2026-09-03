(ns clj-surgeon.ls-tree-budget-test
  "Millisecond-scale witnesses for the bounded `ls-tree` output budget.

   These are the BEHAVIOUR half of the intent: a result exactly at the ceiling
   is complete and byte-identical to the unbounded path, one record past it is
   a typed continuation or a typed refusal, and a continuation cursor is bound
   to the manifest so a page taken after the tree changed refuses instead of
   interleaving two repositories.

   The RETENTION half — that what is held tracks the ceiling and not the file
   count — needs a heap meter and lives in `clj-surgeon.ls-tree-memory-test`,
   in the JVM suite."
  (:require
   [babashka.fs :as fs]
   [clj-surgeon.core :as core]
   [clj-surgeon.result-budget :as budget]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

;; ============================================================
;; Fixtures — a project of N tiny files, deterministic in path order
;; ============================================================

(defn- tiny-source [i]
  (format "(ns fixt.mod%03d)\n\n(defn f%03d [x] (inc x))\n\n(def v%03d %d)\n"
          i i i i))

(defn- make-project!
  "A single project of `n` tiny files under a fresh temp directory. Returns the
   directory. File names are zero-padded so `sort` order is index order, which
   is what makes the ORDER assertions below meaningful."
  [n prefix]
  (let [dir (str (fs/create-temp-dir {:prefix prefix}))
        src (str dir "/src/fixt")]
    (fs/create-dirs src)
    (spit (str dir "/deps.edn") "{:paths [\"src\"]}")
    (dotimes [i n]
      (spit (format "%s/mod%03d.clj" src i) (tiny-source i)))
    dir))

(defn- entry-files
  "The `:file` of every record in an EDN result, in result order. Receipt maps
   carry no `:file` and drop out."
  [result]
  (vec (keep :file result)))

(defn- receipt
  "The trailing receipt map of an EDN result, or nil when the result is a plain
   complete vector."
  [result]
  (when (vector? result)
    (let [last-entry (peek result)]
      (when (and (map? last-entry) (nil? (:file last-entry)))
        last-entry))))

(def ^:private fixture-count 12)

(defmacro ^:private with-project
  [[binding n prefix] & body]
  `(let [~binding (make-project! ~n ~prefix)]
     (try ~@body
          (finally (fs/delete-tree ~binding)))))

;; ============================================================
;; The ceiling itself
;; ============================================================

;; @spec MCP-OP-MEM-003
(deftest a-request-may-lower-the-ceiling-and-may-never-raise-it
  (testing "no request resolves to the server cap"
    (is (= budget/max-result-records (budget/resolve-ceiling nil))))
  (testing "a smaller request lowers it"
    (is (= 25 (budget/resolve-ceiling 25)))
    (is (= 25 (budget/resolve-ceiling "25"))))
  (testing "a larger request is clamped to the server cap, never honoured"
    (is (= budget/max-result-records
           (budget/resolve-ceiling (* 100 budget/max-result-records)))))
  (testing "a malformed ceiling is named, not silently promoted to the cap"
    (is (= :invalid (budget/parse-ceiling 0)))
    (is (= :invalid (budget/parse-ceiling -3)))
    (is (= :invalid (budget/parse-ceiling "all")))
    (is (= :invalid (budget/parse-ceiling :everything)))
    (is (nil? (budget/parse-ceiling nil)))))

;; @spec MCP-OP-MEM-003
(deftest a-result-exactly-at-the-ceiling-is-complete
  (with-project [dir fixture-count "ls-tree-budget-at"]
    (let [at (core/run-ls-tree {:dir dir :format :edn :max-results fixture-count})
          unbounded (core/run-ls-tree {:dir dir :format :edn})]
      (is (= fixture-count (count (entry-files at)))
          "every candidate is encoded when the ceiling is exactly the count")
      (is (nil? (receipt at))
          "a complete result carries no ceiling receipt")
      (is (= unbounded at)
          "at the ceiling the bounded result is identical to the unbounded one"))))

;; @spec MCP-OP-MEM-003
(deftest one-record-past-the-ceiling-yields-a-typed-continuation
  (with-project [dir fixture-count "ls-tree-budget-over"]
    (let [r (core/run-ls-tree {:dir dir :format :edn
                               :max-results (dec fixture-count)})
          rcpt (receipt r)
          ceiling (get-in rcpt [:receipt :result_ceiling])]
      (is (= (dec fixture-count) (count (entry-files r)))
          "exactly R records are encoded")
      (is (some? ceiling) "the result carries a result_ceiling receipt")
      (is (= {:limit (dec fixture-count)
              :offset 0
              :returned (dec fixture-count)
              :total fixture-count
              :remaining 1}
             (select-keys ceiling [:limit :offset :returned :total :remaining])))
      (is (= 64 (count (:manifest_digest ceiling))))
      (testing "the continuation names the exact call that resumes it"
        (let [nc (:next_call rcpt)]
          (is (= :ls-tree (:op nc)))
          (is (= dir (:dir nc)))
          (is (= (dec fixture-count) (:max-results nc)))
          (is (= {:offset (dec fixture-count)
                  :manifest-digest (:manifest_digest ceiling)}
                 (budget/parse-cursor (:cursor nc)))))))))

;; @spec MCP-OP-MEM-003
(deftest a-complete-request-past-the-ceiling-refuses-and-names-what-fits
  (with-project [dir fixture-count "ls-tree-budget-refuse"]
    (let [r (core/run-ls-tree {:dir dir :format :edn
                               :max-results (dec fixture-count)
                               :complete true})]
      (is (map? r) "a refusal is one typed map, not a truncated vector")
      (is (= :result-ceiling-exceeded (:error-type r)))
      (is (false? (:complete r)))
      (is (true? (:source-unchanged r)))
      (is (= {:kind :result-records
              :requested (dec fixture-count)
              :server-max budget/max-result-records
              :observed fixture-count
              :fits (dec fixture-count)}
             (:limit r))
          "the refusal names R, the server cap, what it observed, and what fits")
      (is (string? (:remedy r)))
      (is (not (str/includes? (str/lower-case (:remedy r)) "heap"))
          "the remedy narrows the scope; it never says raise the heap")
      (is (some? (get-in r [:next_call :cursor]))))))

;; @spec MCP-OP-MEM-003
(deftest a-complete-request-inside-the-ceiling-is-not-refused
  (with-project [dir fixture-count "ls-tree-budget-complete-ok"]
    (let [r (core/run-ls-tree {:dir dir :format :edn
                               :max-results fixture-count
                               :complete true})]
      (is (vector? r))
      (is (= fixture-count (count (entry-files r)))))))

;; ============================================================
;; The cursor, and what it is bound to
;; ============================================================

;; @spec MCP-OP-MEM-003
(deftest the-continuation-cursor-pages-the-remainder-exactly-once
  (with-project [dir fixture-count "ls-tree-budget-page"]
    (let [page-size 5
          page-1 (core/run-ls-tree {:dir dir :format :edn :max-results page-size})
          cursor-1 (get-in (receipt page-1) [:next_call :cursor])
          page-2 (core/run-ls-tree {:dir dir :format :edn :max-results page-size
                                    :cursor cursor-1})
          cursor-2 (get-in (receipt page-2) [:next_call :cursor])
          page-3 (core/run-ls-tree {:dir dir :format :edn :max-results page-size
                                    :cursor cursor-2})
          whole (core/run-ls-tree {:dir dir :format :edn})]
      (is (= 5 (count (entry-files page-1))))
      (is (= 5 (count (entry-files page-2))))
      (is (= 2 (count (entry-files page-3))))
      (is (nil? (receipt page-3)) "the last page is complete")
      (testing "the pages concatenate to the whole result, in the same order"
        (is (= (entry-files whole)
               (into (into (entry-files page-1) (entry-files page-2))
                     (entry-files page-3))))))))

;; @spec MCP-OP-MEM-003
(deftest a-cursor-is-refused-once-the-tree-has-changed
  (with-project [dir fixture-count "ls-tree-budget-stale"]
    (let [page-1 (core/run-ls-tree {:dir dir :format :edn :max-results 5})
          cursor (get-in (receipt page-1) [:next_call :cursor])]
      (spit (str dir "/src/fixt/mod999.clj") (tiny-source 999))
      (let [r (core/run-ls-tree {:dir dir :format :edn :max-results 5
                                 :cursor cursor})]
        (is (= :stale-result-cursor (:error-type r))
            "a page bound to a manifest that no longer exists refuses")
        (is (false? (:complete r)))
        (is (true? (:source-unchanged r)))
        (is (= (:manifest-digest (budget/parse-cursor cursor))
               (get-in r [:limit :requested])))
        (is (not= (get-in r [:limit :requested])
                  (get-in r [:limit :observed])))))))

;; @spec MCP-OP-MEM-003
(deftest a-malformed-cursor-is-refused-rather-than-ignored
  (with-project [dir 4 "ls-tree-budget-badcursor"]
    (let [r (core/run-ls-tree {:dir dir :format :edn :cursor "not-a-cursor"})]
      (is (= :invalid-result-cursor (:error-type r)))
      (is (false? (:complete r))))))

;; ============================================================
;; Byte-identity with the batch path, below the ceiling
;; ============================================================

(defn- batch-result
  "The unbounded batch encoder: discover, outline everything, format the whole
   retained set. This is the path `run-ls-tree` used before the budget, kept
   here as the differential oracle."
  [dir output-format]
  (let [abs (str (fs/absolutize dir))
        projects (#'core/outline-all-files (#'core/discover-projects abs))]
    (if (= :edn output-format)
      (core/format-ls-tree-edn projects abs)
      (core/format-ls-tree-text projects abs))))

;; @spec MCP-OP-MEM-003
(deftest under-the-ceiling-the-streamed-result-equals-the-batch-result
  (testing "single project"
    (with-project [dir fixture-count "ls-tree-budget-diff-single"]
      (is (= (batch-result dir :text) (core/run-ls-tree {:dir dir})))
      (is (= (batch-result dir :edn) (core/run-ls-tree {:dir dir :format :edn})))))
  (testing "multiple projects — the per-project headers carry counts that are
            only known after that project's last file is encoded"
    (let [parent (str (fs/create-temp-dir {:prefix "ls-tree-budget-diff-multi"}))]
      (try
        (doseq [[nm n] [["alpha" 3] ["beta" 5] ["gamma" 2]]]
          (let [p (str parent "/" nm) src (str p "/src/fixt")]
            (fs/create-dirs src)
            (spit (str p "/deps.edn") "{:paths [\"src\"]}")
            (dotimes [i n] (spit (format "%s/mod%03d.clj" src i) (tiny-source i)))))
        (is (= (batch-result parent :text) (core/run-ls-tree {:dir parent})))
        (is (= (batch-result parent :edn)
               (core/run-ls-tree {:dir parent :format :edn})))
        (finally (fs/delete-tree parent))))))

;; @spec MCP-OP-MEM-003
(deftest a-parse-error-under-the-ceiling-still-reads-exactly-as-before
  (let [dir (str (fs/create-temp-dir {:prefix "ls-tree-budget-broken"}))]
    (try
      (fs/create-dirs (str dir "/src"))
      (spit (str dir "/deps.edn") "{:paths [\"src\"]}")
      (spit (str dir "/src/good.clj") "(ns good)\n(defn g [] :ok)\n")
      (spit (str dir "/src/bad.clj") "(defn unclosed [")
      (is (= (batch-result dir :text) (core/run-ls-tree {:dir dir})))
      (is (= (batch-result dir :edn) (core/run-ls-tree {:dir dir :format :edn})))
      (finally (fs/delete-tree dir)))))

;; ============================================================
;; The text encoding of a bounded result
;; ============================================================

;; @spec MCP-OP-MEM-003
(deftest the-text-encoding-names-the-ceiling-and-the-resuming-call
  (with-project [dir fixture-count "ls-tree-budget-text"]
    (let [text (core/run-ls-tree {:dir dir :max-results 4})]
      (is (string? text))
      (is (str/includes? text "── total: 4 files"))
      (is (str/includes? text "── result_ceiling: 4 record(s), 4 of 12 file(s)"))
      (is (str/includes? text "next_call: clj-surgeon :op :ls-tree"))
      (is (str/includes? text ":cursor 4:")))
    (testing "a complete text request past the ceiling refuses in text"
      (let [text (core/run-ls-tree {:dir dir :max-results 4 :complete true})]
        (is (string? text))
        (is (str/includes? text "── result-ceiling-exceeded"))
        (is (str/includes? text "remedy:"))))))

;; ============================================================
;; Order stability
;; ============================================================

;; @spec MCP-OP-MEM-003
(deftest the-record-order-is-stable-across-runs-and-across-ceilings
  (with-project [dir fixture-count "ls-tree-budget-order"]
    (let [whole (entry-files (core/run-ls-tree {:dir dir :format :edn}))]
      (is (= whole (sort whole)) "records come out in path order")
      (is (= whole (entry-files (core/run-ls-tree {:dir dir :format :edn})))
          "the same scan twice gives the same order")
      (is (= (vec (take 7 whole))
             (entry-files (core/run-ls-tree {:dir dir :format :edn
                                             :max-results 7})))
          "a bounded result is the PREFIX of the unbounded one, not a sample"))))

;; ============================================================
;; Cursor INTEGRITY — Sol's executed counterexamples (2026-09-03)
;;
;; The review that refused this branch found the cursor was bound to a
;; STAT-derived digest: `<path>\t<size>\t<mtime>` per candidate. Three things
;; follow from that, and all three are silent-wrong-result failures rather
;; than refusals, which is why they are BLOCKERs and not fixes:
;;
;;   1. a file whose bytes change while its path, size and mtime are preserved
;;      pages as unchanged, so page 2 serves content from a tree page 1 never
;;      saw;
;;   2. a cursor minted against one root is accepted against a DIFFERENT root
;;      whose files happen to carry the same stats;
;;   3. the offset half of the cursor is neither authenticated nor
;;      range-checked, so an edited offset past the end returns an empty
;;      vector with no receipt — presented as a complete result.
;;
;; The remedy these witnesses gate is PINNING: the first page writes an
;; immutable manifest snapshot (paths + per-file CONTENT digests) into the
;; workspace state root, and later pages are served from that snapshot.
;; ============================================================

(defn- mtime [path] (.lastModified (java.io.File. (str path))))
(defn- set-mtime! [path ms] (.setLastModified (java.io.File. (str path)) (long ms)))

(defn- swap-bytes-preserving-stat!
  "Replace `path`'s bytes with `content` and restore its recorded mtime.

   `tiny-source` renders the same number of bytes for every single-digit
   index, so writing source `j` over file `i` changes the bytes while leaving
   path, size and mtime identical — the exact shape a stat-derived digest
   cannot see."
  [path content]
  (let [before (mtime path)
        size-before (.length (java.io.File. (str path)))]
    (spit path content)
    (set-mtime! path before)
    (assert (= size-before (.length (java.io.File. (str path))))
            "the swap must preserve size, or it is not this counterexample")
    (assert (= before (mtime path))
            "the swap must preserve mtime, or it is not this counterexample")))

;; @spec MCP-OP-MEM-003
(deftest a-cursor-refuses-when-a-pinned-file-s-bytes-changed-under-a-preserved-stat
  (with-project [dir fixture-count "ls-tree-budget-byteswap"]
    (let [page-1 (core/run-ls-tree {:dir dir :format :edn :max-results 5})
          cursor (get-in (receipt page-1) [:next_call :cursor])
          victim (str dir "/src/fixt/mod007.clj")]
      (is (some? cursor) "page 1 issues a cursor")
      (swap-bytes-preserving-stat! victim (tiny-source 8))
      (let [r (core/run-ls-tree {:dir dir :format :edn :max-results 5
                                 :cursor cursor})]
        (is (map? r)
            "a page whose pinned content no longer matches refuses; it does not
             serve records from a tree page 1 never saw")
        (is (= :stale-result-cursor (:error-type r)))
        (is (false? (:complete r)))
        (is (true? (:source-unchanged r)))
        (is (str/includes? (str (:error r) (pr-str (:limit r))) "mod007.clj")
            "the refusal NAMES the file whose bytes moved")))))

;; @spec MCP-OP-MEM-003
(deftest a-cursor-minted-against-another-root-is-refused
  (let [a (make-project! fixture-count "ls-tree-budget-root-a")
        b (make-project! fixture-count "ls-tree-budget-root-b")]
    (try
      ;; Make the two trees stat-identical: same relative paths, same sizes,
      ;; and now the same mtimes. A stat-derived manifest digest cannot tell
      ;; them apart; a pinned snapshot can, because it lives under the root it
      ;; was taken from.
      (doseq [i (range fixture-count)]
        (let [rel (format "/src/fixt/mod%03d.clj" i)]
          (set-mtime! (str b rel) (mtime (str a rel)))))
      (set-mtime! (str b "/deps.edn") (mtime (str a "/deps.edn")))
      (let [page-1 (core/run-ls-tree {:dir a :format :edn :max-results 5})
            cursor (get-in (receipt page-1) [:next_call :cursor])
            r (core/run-ls-tree {:dir b :format :edn :max-results 5
                                 :cursor cursor})]
        (is (map? r)
            "a cursor is not portable between roots, however alike their stats")
        (is (contains? #{:unknown-result-cursor :stale-result-cursor}
                       (:error-type r))
            (str "expected a typed cursor refusal, got " (pr-str (:error-type r))))
        (is (false? (:complete r))))
      (finally (fs/delete-tree a) (fs/delete-tree b)))))

(defn- edit-offset
  "Rewrite the OFFSET field of a cursor token, whatever its shape: the
   stat-digest form this witness was written against is `<offset>:<digest>`,
   the pinned form that replaces it is `<cursor-id>:<offset>:<mac>`. Editing by
   FIELD rather than by regex keeps the witness meaningful across the change it
   gates."
  [cursor n]
  (let [parts (str/split cursor #":")]
    (str/join ":" (case (count parts)
                    2 (assoc parts 0 (str n))
                    3 (assoc parts 1 (str n))
                    parts))))

;; @spec MCP-OP-MEM-003
(deftest an-edited-offset-never-yields-an-empty-result-presented-as-complete
  (with-project [dir 3 "ls-tree-budget-forged"]
    (let [page-1 (core/run-ls-tree {:dir dir :format :edn :max-results 2})
          cursor (get-in (receipt page-1) [:next_call :cursor])
          forged (edit-offset cursor 99)
          r (core/run-ls-tree {:dir dir :format :edn :max-results 2
                               :cursor forged})]
      (is (not= cursor forged) "the witness actually edited the offset")
      (is (map? r)
          "an offset this server did not mint is refused; it never returns an
           empty vector that a caller would read as a complete result")
      (is (contains? #{:invalid-result-cursor :result-cursor-out-of-range}
                     (:error-type r))
          (str "expected a typed refusal, got " (pr-str r)))
      (is (false? (:complete r))))))
