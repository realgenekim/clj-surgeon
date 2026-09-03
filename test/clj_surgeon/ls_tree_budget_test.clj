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
   [babashka.process :as proc]
   [clj-surgeon.core :as core]
   [clj-surgeon.ls-tree-snapshot :as snapshot]
   [clj-surgeon.result-budget :as budget]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing use-fixtures]]))

;; Pinned manifest snapshots are FILES. Every witness in this namespace runs
;; against a throwaway state root so a test never writes into — or reads from —
;; the operator's real `~/.local/state/clj-surgeon`.
(use-fixtures :once
  (fn [f]
    (let [root (str (fs/create-temp-dir {:prefix "ls-tree-cursor-state"}))]
      (try
        (binding [snapshot/*state-root* root] (f))
        (finally (fs/delete-tree root))))))

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
        (let [nc (:next_call rcpt)
              parsed (budget/parse-cursor (:cursor nc))]
          (is (= :ls-tree (:op nc)))
          (is (= dir (:dir nc)))
          (is (= (dec fixture-count) (:max-results nc)))
          (is (some? parsed) "the cursor parses as a cursor")
          (is (= (dec fixture-count) (:offset parsed))
              "the cursor resumes at the first record this page did not encode")
          (is (= 64 (count (:cursor-id parsed))))
          (is (= 64 (count (:mac parsed)))))))))

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
(deftest a-cursor-is-refused-once-a-pinned-file-is-gone
  (with-project [dir fixture-count "ls-tree-budget-stale"]
    (let [page-1 (core/run-ls-tree {:dir dir :format :edn :max-results 5})
          cursor (get-in (receipt page-1) [:next_call :cursor])]
      ;; mod007 is on page 2. Deleting it is the same class of fact as swapping
      ;; its bytes: the pinned manifest promised a record this page cannot
      ;; honestly serve.
      (fs/delete (str dir "/src/fixt/mod007.clj"))
      (let [r (core/run-ls-tree {:dir dir :format :edn :max-results 5
                                 :cursor cursor})]
        (is (= :stale-result-cursor (:error-type r))
            "a page whose pinned record is gone refuses")
        (is (false? (:complete r)))
        (is (true? (:source-unchanged r)))
        (is (= "src/fixt/mod007.clj" (get-in r [:limit :file]))
            "the refusal names the missing file")
        (is (nil? (get-in r [:limit :observed]))
            "a deleted file observes no content digest at all")
        (is (some? (get-in r [:limit :requested])))))))

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
      (testing "the text carries a real cursor, resuming at the first record it
                did not encode"
        (let [token (second (re-find #":cursor (\S+)" text))]
          (is (= 4 (:offset (budget/parse-cursor token)))))))
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

;; ============================================================
;; The refusals that only exist once the manifest is PINNED
;;
;; The three witnesses above are Sol's counterexamples and failed before the
;; snapshot existed. These two name behaviour that had no representation at
;; all: a cursor whose snapshot is gone, and a cursor this server really did
;; mint whose offset is not in the manifest. They are new-behaviour witnesses,
;; so their falsifiability is shown by construction rather than by history —
;; each asserts the specific error-type, and a page that served records
;; instead, or an empty vector, fails them.
;; ============================================================

;; @spec MCP-OP-MEM-003
(deftest a-cursor-whose-pinned-snapshot-is-gone-is-unknown
  (with-project [dir fixture-count "ls-tree-budget-pruned"]
    (let [page-1 (core/run-ls-tree {:dir dir :format :edn :max-results 5})
          cursor (get-in (receipt page-1) [:next_call :cursor])
          cursor-id (:cursor-id (budget/parse-cursor cursor))]
      (is (some? cursor-id))
      (fs/delete-tree (snapshot/cursor-dir dir))
      (let [r (core/run-ls-tree {:dir dir :format :edn :max-results 5
                                 :cursor cursor})]
        (is (= :unknown-result-cursor (:error-type r))
            "an expired or pruned snapshot is named as such, not re-derived
             from whatever the tree holds now")
        (is (false? (:complete r)))
        (is (true? (:source-unchanged r)))
        (is (nil? (get-in r [:next_call :cursor]))
            "the remedy is a rescan, so the refusal offers no cursor")))))

;; @spec MCP-OP-MEM-003
(deftest a-genuine-cursor-past-the-end-of-its-manifest-is-out-of-range
  (with-project [dir 3 "ls-tree-budget-range"]
    (let [page-1 (core/run-ls-tree {:dir dir :format :edn :max-results 2})
          cursor (get-in (receipt page-1) [:next_call :cursor])
          cursor-id (:cursor-id (budget/parse-cursor cursor))
          snap (snapshot/read-meta dir cursor-id)
          ;; Minted by the server's own authenticator, so the mac verifies and
          ;; this is NOT the forged case. Offsets past the end are never issued
          ;; — the last page carries no cursor — so the only way to reach the
          ;; range check is to mint one.
          genuine (budget/cursor-token
                    cursor-id 99 (snapshot/mac cursor-id 99 (:secret snap)))
          r (core/run-ls-tree {:dir dir :format :edn :max-results 2
                               :cursor genuine})]
      (is (= 3 (:total snap)))
      (is (= :result-cursor-out-of-range (:error-type r))
          "a real cursor at an unreal position refuses; it never returns an
           empty vector a caller would read as a complete result")
      (is (= {:kind :result-offset :requested 99 :observed 3} (:limit r))
          "the refusal names the offset asked for and the manifest it is not in")
      (is (false? (:complete r)))
      (is (true? (:source-unchanged r))))))

;; ============================================================
;; A page reads only its OWN slice
;;
;; Finding 7 measured `O(pages x N)`: every continuation page re-walked the
;; whole manifest, folding 10,000 stat rows to serve 1,000 records. Pinning
;; removes the re-walk, and this witness gates it structurally rather than by
;; timing: page 2 must not call discovery at all.
;; ============================================================

;; @spec MCP-OP-MEM-003
(deftest a-continuation-page-does-no-discovery
  (with-project [dir fixture-count "ls-tree-budget-no-rewalk"]
    (let [page-1 (core/run-ls-tree {:dir dir :format :edn :max-results 5})
          cursor (get-in (receipt page-1) [:next_call :cursor])
          discoveries (atom 0)
          v #'core/discover-projects
          real @v]
      ;; `alter-var-root` rather than `with-redefs`: the var is private and this
      ;; namespace runs under both babashka and the JVM.
      (alter-var-root v (constantly (fn [d] (swap! discoveries inc) (real d))))
      (try
        (let [page-2 (core/run-ls-tree {:dir dir :format :edn :max-results 5
                                        :cursor cursor})]
          (is (= 5 (count (entry-files page-2))) "the page is served")
          (is (zero? @discoveries)
              "a continuation is served from the pinned manifest; it does not
               glob, walk, or stat the tree a second time"))
        ;; The counter is not decorative: a scan WITHOUT a cursor must move it,
        ;; or the assertion above proves nothing about the redefinition.
        (let [fresh (core/run-ls-tree {:dir dir :format :edn})]
          (is (= fixture-count (count (entry-files fresh))))
          (is (= 1 @discoveries)
              "a fresh scan does discover — the counter is wired in"))
        (finally (alter-var-root v (constantly real)))))))

;; ============================================================
;; Malformed NUMBERS refuse; they never escape as exceptions
;;
;; Sol finding 3: ordinary malformed cursors and ceilings were typed, but a
;; well-FORMED forty-digit integer was not. `Long/parseLong` threw
;; `NumberFormatException` out of the operation, so the caller got a stack
;; trace where the contract promises a receipt. An untyped throw is not a
;; smaller version of a refusal — it carries no limit, no observed value, no
;; remedy, and no `:source-unchanged`, which is the field that tells a caller
;; its repository was not touched.
;; ============================================================

(def ^:private forty-digits "1234567890123456789012345678901234567890")

;; @spec MCP-OP-MEM-003
(deftest an-unrepresentable-max-results-is-typed-not-thrown
  (with-project [dir 3 "ls-tree-budget-bignum-ceiling"]
    (testing "as a string"
      (is (= :invalid (budget/parse-ceiling forty-digits)))
      (let [r (core/run-ls-tree {:dir dir :format :edn :max-results forty-digits})]
        (is (= :invalid-result-ceiling (:error-type r)))
        (is (false? (:complete r)))
        (is (true? (:source-unchanged r)))
        (is (= budget/max-result-records (get-in r [:limit :server-max])))))
    (testing "as an integer too large to be a record count"
      (is (= :invalid (budget/parse-ceiling (* 1000000000000N 1000000000000N))))
      (let [r (core/run-ls-tree {:dir dir :format :edn
                                 :max-results (* 1000000000000N 1000000000000N)})]
        (is (= :invalid-result-ceiling (:error-type r)))))
    (testing "a ceiling that IS representable still resolves"
      (is (= 2 (budget/resolve-ceiling "2"))))))

;; @spec MCP-OP-MEM-003
(deftest an-unrepresentable-cursor-offset-is-typed-not-thrown
  (with-project [dir 3 "ls-tree-budget-bignum-offset"]
    (let [page-1 (core/run-ls-tree {:dir dir :format :edn :max-results 2})
          cursor (get-in (receipt page-1) [:next_call :cursor])
          huge (edit-offset cursor (apply str (repeat 40 "9")))]
      (is (nil? (budget/parse-cursor huge))
          "a forty-digit offset is not a cursor this server could have minted")
      (let [r (core/run-ls-tree {:dir dir :format :edn :max-results 2
                                 :cursor huge})]
        (is (= :invalid-result-cursor (:error-type r)))
        (is (false? (:complete r)))
        (is (true? (:source-unchanged r))))
      (testing "the ordinary offset in the same token still parses"
        (is (= 2 (:offset (budget/parse-cursor cursor))))))))

;; ============================================================
;; The ceiling AT its shipped value
;;
;; Sol finding 4: the ceiling behaviour is correct, but every checked-in
;; witness exercised a caller-LOWERED fixture ceiling — R=12 — while the value
;; that actually ships is `max-result-records` = 1,000. A witness below the
;; hard ceiling proves the paging arithmetic and proves nothing about the
;; constant: lowering the constant to 12 would leave it green, and raising it
;; to a million would too. This one binds to the shipped number.
;;
;; It is deliberately cheap. 1,001 two-form files scan in about 100 ms, so the
;; strongest witness in the file is also one of the fastest.
;; ============================================================

(defn- ceiling-project!
  "`n` files of two forms each — the smallest thing that still produces one
   record per file, so a witness at N = 1,001 costs a tenth of a second."
  [n prefix]
  (let [dir (str (fs/create-temp-dir {:prefix prefix}))
        src (str dir "/src/fixt")]
    (fs/create-dirs src)
    (spit (str dir "/deps.edn") "{:paths [\"src\"]}")
    (dotimes [i n]
      (spit (format "%s/m%04d.clj" src i)
            (format "(ns fixt.m%04d)\n(def v %d)\n" i i)))
    dir))

;; @spec MCP-OP-MEM-003
(deftest the-server-ceiling-binds-at-its-shipped-value-not-at-a-fixture-value
  (is (= 1000 budget/max-result-records)
      "the witnesses below are written against this number; changing it is a
       decision that must re-measure the retention it was derived from")
  ;; The fixture sizes are LITERAL, not derived from the constant. Deriving
  ;; them would make the whole witness scale with whatever the constant became,
  ;; so lowering it to 900 would leave every behavioural assertion green — a
  ;; test that cannot see the change it exists to see.
  (let [at (ceiling-project! 1000 "ls-tree-ceiling-at")
        over (ceiling-project! 1001 "ls-tree-ceiling-over")]
    (try
      (testing "exactly R records is a COMPLETE result, with no receipt"
        (let [r (core/run-ls-tree {:dir at :format :edn})]
          (is (= 1000 (count (entry-files r))))
          (is (nil? (receipt r))
              "at the ceiling the caller is told nothing, because nothing was
               withheld")))
      (testing "R+1 candidates yield R records and a typed continuation"
        (let [r (core/run-ls-tree {:dir over :format :edn})
              rc (get-in (receipt r) [:receipt :result_ceiling])]
          (is (= 1000 (count (entry-files r))))
          (is (= {:limit 1000
                  :server_max 1000
                  :offset 0
                  :returned 1000
                  :total 1001
                  :remaining 1}
                 (select-keys rc [:limit :server_max :offset :returned
                                  :total :remaining])))
          (testing "and the continuation serves the one remaining record"
            (let [page-2 (core/run-ls-tree
                           {:dir over :format :edn
                            :cursor (get-in (receipt r) [:next_call :cursor])})]
              (is (= 1 (count (entry-files page-2))))
              (is (nil? (receipt page-2)) "the last page is complete")))))
      (testing "R+1 with :complete refuses, naming the shipped cap"
        (let [r (core/run-ls-tree {:dir over :format :edn :complete true})]
          (is (= :result-ceiling-exceeded (:error-type r)))
          (is (= {:kind :result-records
                  :requested 1000
                  :server-max 1000
                  :observed 1001
                  :fits 1000}
                 (:limit r)))))
      (finally (fs/delete-tree at) (fs/delete-tree over)))))

;; ============================================================
;; The empty scan — a boundary this branch fixed incidentally
;; ============================================================
;;
;; `run-ls-tree` destructures `:format` out of its own opts map, which SHADOWS
;; `clojure.core/format` for the whole body. The empty-scan branch called
;; `format` to say what it had not found, so a scan that found nothing threw
;; `NullPointerException` where it meant to print. The message now lives in
;; `no-clojure-files-message`, outside the shadow.
;;
;; Sol's review (2026-09-03, finding 11) confirmed the fix by hand and recorded
;; that NOTHING named or executed the boundary. A fix without a witness is a
;; fix the next refactor can undo, and the symptom — a throw on the one input
;; that carries no data — is exactly the shape nobody tests.
;;
;; ASSERT ON THE MESSAGE, NOT ON THE ABSENCE OF AN EXCEPTION. Re-introducing
;; the shadow to check this witness fails first produced NO exception text at
;; all: the CLI's top-level handler caught the NPE and rendered it as
;;
;;     {:error nil, :error-type :invalid-arguments}
;;
;; with exit 1. So the defect's real signature is a receipt that names NOTHING
;; while looking like an ordinary typed refusal — and both an exit-code check
;; and a grep for "Exception" pass straight through it. Only the message
;; assertions below fall over, which is why they are the witness.
;;
;; The witness runs the REAL CLI in a subprocess, because the defect is a var
;; shadow at a call site plus an exit code, and neither survives being tested
;; in-process: `System/exit` would take the suite down with it, and a pure call
;; to the extracted fn cannot reproduce a shadow it now sits outside of.
;; Testing delivery, not identity.

(defn- run-cli-ls-tree
  "The `ls-tree` op through the real CLI, in babashka, returning `{:exit :out
   :err}`. `bb -cp src` is the same invocation `cli-dispatch-test` uses."
  [& args]
  (let [src (str (fs/absolutize "src"))]
    (apply proc/shell {:out :string :err :string :continue true}
           "bb" "-cp" src "-m" "clj-surgeon.core" ":op" "ls-tree" args)))

;; @spec MCP-OP-MEM-003
(deftest an-empty-scan-names-what-it-searched-and-exits-one
  (let [empty-dir (str (fs/create-temp-dir {:prefix "ls-tree-empty"}))]
    (try
      (testing "a directory holding no Clojure file"
        (let [{:keys [exit out err]} (run-cli-ls-tree ":dir" empty-dir)]
          (is (= 1 exit)
              (str "an empty scan is a failed scan, not a crash; stderr: " err))
          (is (str/includes? out "No Clojure files found under")
              "it says what it did not find")
          (is (str/includes? out empty-dir)
              "and names the directory it searched, so the caller can see the
               scope was wrong rather than the tool")
          (is (not (str/includes? out ":invalid-arguments"))
              "an empty scan is not an argument error; the shadowed-`format`
               defect surfaced here, as a caught NPE rendered into a typed
               refusal whose :error was nil")
          (is (not (str/includes? (str out err) "NullPointerException"))
              "and nothing throws in the open either")))
      (testing "a grep that matches nothing names the pattern too"
        (let [{:keys [exit out err]}
              (run-cli-ls-tree ":dir" empty-dir ":grep" "zzz-no-such-symbol")]
          (is (= 1 exit) (str "stderr: " err))
          (is (str/includes? out "matching 'zzz-no-such-symbol'")
              "a scan narrowed by :grep says which narrowing found nothing")
          (is (not (str/includes? out ":invalid-arguments")))))
      (finally (fs/delete-tree empty-dir)))))

;; @spec MCP-OP-MEM-003
(deftest the-empty-scan-message-is-pure-and-covers-both-shapes
  (testing "extracted from the shadow, and therefore callable"
    (is (= "No Clojure files found under /tmp/nowhere"
           (core/no-clojure-files-message "/tmp/nowhere" nil)))
    (is (= "No Clojure files found under /tmp/nowhere matching 'defn foo'"
           (core/no-clojure-files-message "/tmp/nowhere" "defn foo")))))

;; ============================================================
;; The cursor is CONTENT-ADDRESSED — an unchanged tree scans identically
;; and pins nothing new
;;
;; The memory battery caught this AFTER the pinned snapshot landed. It hashes
;; each arm's output across five reps of an identical operation over an
;; identical corpus, and reported
;;
;;     FAIL reference-mismatch {:op :cli-ls-tree, :n 10000, :phase :warm,
;;                              :observed "nondeterministic:4"}
;;
;; — four distinct output hashes. Diffed line by line: 98,361 characters, ONE
;; differing line, and it was the cursor. `snapshot/new-id` minted two random
;; UUIDs per ceiling-binding scan, so the id named the SCAN rather than the
;; tree. The same randomness meant an unchanged tree pinned a new 1.4 MB
;; snapshot every scan — four identical scans left four snapshots totalling
;; 5.4 MB, each paying a full 10,000-file content-digest pass, and nothing
;; could tell that the tree had not moved.
;;
;; The repair is to address the snapshot by WHAT IT CONTAINS: the manifest
;; digest, folded over the ordered rows and each file's content digest under a
;; projection version. Two properties follow, and the witnesses below are one
;; each: an unchanged tree renders identically because its id is a function of
;; the tree, and it REUSES the pinned snapshot because the id is the only
;; thing addressing it. The third witness holds the line the id change could
;; have quietly crossed — the mac stays keyed on a per-snapshot secret that is
;; never published — and the fourth says what reuse costs: a snapshot is
;; trusted for its CONTENT, never for its filename.
;; ============================================================

(defn- cursor-of
  "The continuation cursor of an EDN result, or nil when it carries none."
  [result]
  (get-in (receipt result) [:next_call :cursor]))

(defn- snapshot-ids
  "The ids of every pinned snapshot under `dir`'s state directory."
  [dir]
  (->> (fs/glob (str (snapshot/cursor-dir dir)) "*.edn")
       (map #(str/replace (str (fs/file-name %)) #"\.edn$" ""))
       sort
       vec))

(defn- rows-path
  [dir cursor-id]
  (str (snapshot/cursor-dir dir) "/" cursor-id ".rows"))

;; @spec MCP-OP-MEM-003
(deftest two-scans-of-an-unchanged-tree-are-byte-identical-and-pin-one-snapshot
  (with-project [dir fixture-count "ls-tree-budget-deterministic"]
    (let [a (core/run-ls-tree {:dir dir :max-results 5})
          b (core/run-ls-tree {:dir dir :max-results 5})
          a-edn (core/run-ls-tree {:dir dir :format :edn :max-results 5})
          b-edn (core/run-ls-tree {:dir dir :format :edn :max-results 5})]
      (is (string? a) "the text encoding is what the battery hashes")
      (is (str/includes? a ":cursor ")
          "the ceiling binds, so the result really does carry a cursor — a
           witness for determinism must first prove the varying field is there")
      (is (= a b)
          "two scans of an unchanged tree render BYTE-IDENTICAL text, cursor
           included; a random cursor-id differed in exactly this one line")
      (is (= a-edn b-edn)
          "and the EDN encoding likewise, receipt and next_call included")
      (is (= 1 (count (snapshot-ids dir)))
          "an unchanged tree pins ONE snapshot however often it is scanned;
           four identical scans used to leave four snapshots")
      (is (empty? (fs/glob (str (snapshot/cursor-dir dir)) "*.tmp"))
          "and no build temporaries are left behind"))))

;; @spec MCP-OP-MEM-003
(deftest a-changed-tree-mints-a-new-cursor-id
  (with-project [dir fixture-count "ls-tree-budget-content-id"]
    (let [id-of #(:cursor-id (budget/parse-cursor
                               (cursor-of (core/run-ls-tree
                                            {:dir dir :format :edn
                                             :max-results 5}))))
          before (id-of)]
      (is (some? before))
      (is (= before (id-of))
          "identity is the TREE, not the scan")
      (spit (str dir "/src/fixt/mod003.clj") (tiny-source 903))
      (let [after (id-of)]
        (is (not= before after)
            "content that moved is a different manifest and gets a different
             id; a stat- or scan-derived id could not say so")
        (is (= #{before after} (set (snapshot-ids dir)))
            "the changed tree pins its OWN snapshot beside the old one, one
             per distinct tree state")))))

;; @spec MCP-OP-MEM-003
(deftest a-receipt-holder-cannot-mint-a-cursor-for-another-offset
  (with-project [dir fixture-count "ls-tree-budget-forge-mac"]
    (let [page-1 (core/run-ls-tree {:dir dir :format :edn :max-results 5})
          rc (get-in (receipt page-1) [:receipt :result_ceiling])
          cursor (cursor-of page-1)
          {:keys [cursor-id offset]} (budget/parse-cursor cursor)
          published (:manifest_digest rc)
          secret (:secret (snapshot/read-meta dir cursor-id))
          target 10]
      (is (= 5 offset) "the holder was issued exactly one offset")
      (is (= cursor-id published)
          "the cursor-id IS the published manifest digest — content-addressed,
           and therefore no longer usable as a MAC key")
      (is (and (string? secret) (= 64 (count secret))))
      (is (not (str/includes? (pr-str page-1) secret))
          "the secret is never published: not in the receipt, not in the token")
      (testing "every mac a receipt holder could build from published material"
        (doseq [forged [(snapshot/sha256-hex (str cursor-id ":" target ":" published))
                        (snapshot/sha256-hex (str cursor-id ":" target))
                        (snapshot/sha256-hex (str published ":" target))
                        (snapshot/sha256-hex (str cursor-id target published))
                        published
                        cursor-id]]
          (let [token (budget/cursor-token cursor-id target forged)
                r (core/run-ls-tree {:dir dir :format :edn :max-results 5
                                     :cursor token})]
            (is (map? r)
                (str "a cursor minted from published material must refuse: "
                     token))
            (is (= :invalid-result-cursor (:error-type r))
                (str "expected a typed refusal, got " (pr-str (:error-type r)))))))
      (testing "the same offset with the SERVER's key is servable — so the
                refusals above are the mac's doing, not the offset's"
        (let [genuine (budget/cursor-token
                        cursor-id target (snapshot/mac cursor-id target secret))
              r (core/run-ls-tree {:dir dir :format :edn :max-results 5
                                   :cursor genuine})]
          (is (= 2 (count (entry-files r)))
              (str "offset 10 of a 12-record manifest holds 2 records; got "
                   (pr-str r))))))))

;; @spec MCP-OP-MEM-003
(deftest a-snapshot-whose-rows-do-not-match-its-digest-is-rebuilt-not-trusted
  (with-project [dir fixture-count "ls-tree-budget-corrupt"]
    (let [page-1 (core/run-ls-tree {:dir dir :format :edn :max-results 5})
          stale-cursor (cursor-of page-1)
          cursor-id (:cursor-id (budget/parse-cursor stale-cursor))
          rows (rows-path dir cursor-id)
          pinned (slurp rows)]
      ;; Rewrite the pinned rows so every row names the FIRST file. A snapshot
      ;; trusted on the strength of its filename would serve mod000 five times
      ;; as page 2 of a tree that holds twelve distinct files.
      (spit rows (str (str/join "\n" (repeat fixture-count
                                             (first (str/split-lines pinned))))
                      "\n"))
      (is (not= pinned (slurp rows)) "the witness actually corrupted the rows")
      (let [again (core/run-ls-tree {:dir dir :format :edn :max-results 5})
            fresh (cursor-of again)]
        (is (= cursor-id (:cursor-id (budget/parse-cursor fresh)))
            "the TREE has not moved, so its content address has not moved")
        (is (= pinned (slurp rows))
            "the corrupt rows were REBUILT from the tree; a snapshot is trusted
             for its content, never for its name")
        (is (= 1 (count (snapshot-ids dir))))
        (let [page-2 (core/run-ls-tree {:dir dir :format :edn :max-results 5
                                        :cursor fresh})]
          (is (= 5 (count (distinct (entry-files page-2))))
              "and page 2 serves five DISTINCT files, which the corrupt
               manifest could not have")
          (is (= 5 (count (entry-files page-2)))))
        (testing "the discarded snapshot's authenticator is discarded with it"
          (let [r (core/run-ls-tree {:dir dir :format :edn :max-results 5
                                     :cursor stale-cursor})]
            (is (map? r))
            (is (contains? #{:invalid-result-cursor :unknown-result-cursor}
                           (:error-type r))
                (str "expected a typed refusal, got " (pr-str (:error-type r))))))))))
