(ns clj-surgeon.mcp-study-test
  "MCP read-entrance witnesses for the study operations.

   @spec MCP-OP-STUDY-001 MCP-OP-STUDY-002 MCP-OP-STUDY-003
   @spec MCP-OP-STUDY-004 MCP-OP-STUDY-005 MCP-OP-STUDY-006
   @spec MCP-OP-STUDY-007 MCP-OP-STUDY-008 MCP-OP-STUDY-009
   @spec MCP-OP-STUDY-010 MCP-OP-STUDY-011 MCP-OP-STUDY-012"
  (:require
   [babashka.fs :as fs]
   [cheshire.core :as json]
   [clj-surgeon.core :as core]
   [clj-surgeon.mcp-inspect :as inspect]
   [clj-surgeon.mcp-inspect-tool :as inspect-tool]
   [clj-surgeon.mcp-paths :as mcp-paths]
   [clj-surgeon.mcp-telemetry :as telemetry]
   [clj-surgeon.mcp-tool :as mcp-tool]
   [clj-surgeon.outline :as outline]
   [clj-surgeon.parallel :as parallel]
   [clj-surgeon.study :as study]
   [clojure.string :as str]
   [clojure.walk :as walk]
   [clojure.test :refer [deftest is testing]]))

(def ^:private project-root (System/getProperty "user.dir"))
(def ^:private config {:project-root project-root})
(def ^:private real-file "src/clj_surgeon/analyze.clj")
(def ^:private fixture-dir "test-fixtures/cljc/existing-ops")

(defn- run
  [params]
  (inspect-tool/execute-inspect! config params))

(defn- one
  ([operation extra] (one operation extra {}))
  ([operation extra top]
   (run (merge {"requests" [(merge {"operation" operation
                                    "file" real-file}
                                   extra)]
                "expect" {"requests" 1 "files" 1}}
               top))))

(defn- result-of
  [response]
  (first (:results response)))

;; ============================================================
;; Operations
;; ============================================================

;; @spec MCP-OP-STUDY-002
(deftest deps-request-returns-a-bounded-call-graph
  (let [response (one "deps" {"limit" 16384})
        result (result-of response)]
    (is (true? (:ok response)))
    (is (true? (:read_complete response)))
    (is (= "deps" (:operation result)))
    (is (= real-file (:file result)))
    (is (string? (:file_hash result)))
    (is (false? (:truncated result)))
    (is (pos? (:returned result)))
    (is (= (:form_count result) (:returned result))))
  (testing "one owner"
    (let [result (result-of (one "deps" {"form" "dep-tree"}))]
      (is (= "dep-tree" (get-in result [:deps :name])))
      (is (= 1 (:returned result))))))

;; @spec MCP-OP-STUDY-003
(deftest topo-request-returns-order-and-cycles
  (let [result (result-of (one "topo" {"limit" 16384}))]
    (is (= "topo" (:operation result)))
    (is (vector? (get-in result [:topo :sorted])))
    (is (vector? (get-in result [:topo :cycles])))
    (is (contains? (:topo result) :has_cycles?))))

;; @spec MCP-OP-STUDY-004
(deftest ls-deps-request-returns-a-dependency-tree
  (let [result (result-of (one "ls-deps" {"form" "extraction-closure"
                                          "limit" 16384}))]
    (is (= "extraction-closure" (get-in result [:dep_tree :name])))))

;; @spec MCP-OP-STUDY-005
(deftest ls-extract-request-returns-the-minimal-closure
  (let [result (result-of (one "ls-extract" {"form" "extraction-closure"}))]
    (is (= "extraction-closure" (get-in result [:closure :target])))
    (is (= ["extraction-closure"]
           (mapv :name (get-in result [:closure :forms]))))))

;; @spec MCP-OP-STUDY-001
(deftest ls-tree-mode-returns-a-bounded-tree
  (let [response (run {"mode" "ls-tree" "dir" fixture-dir "format" "text"
                       "limit" 16384})]
    (is (true? (:ok response)))
    (is (= "ls-tree" (:mode response)))
    (is (= fixture-dir (:dir response)))
    (is (false? (:truncated response)))
    (is (true? (:read_complete response)))
    (is (= 7 (:file_count response)))
    (is (= 7 (:returned response)))
    (is (str/includes? (:tree response) "total: 7 files"))
    (is (not (str/includes? (:tree response) project-root))
        "a receipt must not leak host-absolute paths"))
  (testing "edn format returns rows, not a string"
    (let [response (run {"mode" "ls-tree" "dir" fixture-dir
                         "format" "edn" "limit" 16384})]
      (is (vector? (:files response)))
      (is (= 7 (count (:files response))))
      (is (nil? (:tree response))))))

;; @spec MCP-OP-STUDY-011
(deftest ls-tree-names-format-is-a-compact-table-of-contents
  (let [response (run {"mode" "ls-tree" "dir" fixture-dir "limit" 16384})]
    (testing "names is the default rendering when grep is absent"
      (is (= "names" (:format response))))
    (is (true? (:ok response)))
    (is (true? (:read_complete response)))
    (is (= 7 (:file_count response)))
    (is (vector? (:files response)))
    (is (nil? (:tree response)))
    (is (= 7 (count (:files response))))
    (doseq [entry (:files response)]
      (is (= #{:file :ns :form_count :line_count} (set (keys entry)))
          "a names entry must be exactly {file, ns, form_count, line_count}")))
  (testing "grep flips the default back to text"
    (let [response (run {"mode" "ls-tree" "dir" fixture-dir "grep" "ns"
                         "limit" 16384})]
      (is (= "text" (:format response)))
      (is (string? (:tree response)))
      (is (nil? (:files response)))))
  (testing "text and edn remain reachable on explicit request regardless of grep"
    (is (= "text" (:format (run {"mode" "ls-tree" "dir" fixture-dir
                                 "format" "text" "limit" 16384}))))
    (is (= "edn" (:format (run {"mode" "ls-tree" "dir" fixture-dir
                                "format" "edn" "limit" 16384}))))))

;; ============================================================
;; A many-file scratch fixture, created and torn down per test, so the
;; default-limit/names fit and the grep-vs-ns_grep witnesses run against a
;; real-shaped tree without committing hundreds of fixture files.
;; ============================================================

(def ^:private files-over-the-returned-source-budget
  "Every file in this repository larger than `:per-request-source` (65,536).
   Each one was unreadable by `outline` until the budget stopped charging
   characters READ against a limit on characters RETURNED."
  ["src/clj_surgeon/intent_transaction.clj"
   "test/clj_surgeon/intent_transaction_test.clj"
   "src/clj_surgeon/worktree_lifecycle_io.clj"
   "test/clj_surgeon/mcp_tool_test.clj"
   "src/clj_surgeon/mcp_change_buffer.clj"
   "src/clj_surgeon/core.clj"
   "src/clj_surgeon/mcp_inspect_tool.clj"])

;; @spec MCP-OP-STUDY-016
;; @spec MCP-OP-STUDY-020
(deftest a-read-that-returns-no-source-is-not-charged-the-source-budget
  ;; Regression witness. MCP-OP-STUDY-016 redefined `source_character_count`
  ;; as the characters a request READ, and `enforce-output-budget` charged
  ;; that number against `per-request-source` — a bound on characters
  ;; RETURNED. `outline` returns a derived structure and no source at all, so
  ;; every one of the seven files below refused with
  ;; `error_type inspect-output-limit`, `scope request_source` and
  ;; `next_action request_less_evidence`: a remedy no caller can act on,
  ;; because the request was already the smallest one that answers the
  ;; question.
  (doseq [file files-over-the-returned-source-budget]
    (testing file
      (let [expected (count (slurp file))
            response (run {"requests" [{"operation" "outline" "file" file}]
                           "expect" {"requests" 1 "files" 1}})
            result (result-of response)]
        (is (< 65536 expected)
            "the fixture only bites while the file exceeds the source budget")
        (is (true? (:ok response)))
        (is (true? (:read_complete response)))
        (is (= expected (:source_character_count result))
            "the receipt still reports the characters it READ"))))
  (testing "every study operation over an oversized file answers too"
    (let [file "src/clj_surgeon/intent_transaction.clj"
          expected (count (slurp file))]
      (doseq [[operation extra]
              [["deps" {"limit" 16384}]
               ["topo" {"limit" 16384}]
               ["ls-extract" {"form" "execute-change!" "limit" 16384}]]]
        (testing operation
          (let [response (run {"requests" [(merge {"operation" operation
                                                   "file" file}
                                                  extra)]
                               "expect" {"requests" 1 "files" 1}})]
            (is (true? (:ok response)))
            (is (= expected (:source_character_count (result-of response))))))))))

;; @spec MCP-OP-STUDY-020
(deftest the-source-budget-counts-the-source-a-result-returns
  (testing "a derived structure is charged what it hands back, not what it read"
    ;; This block asserted the stronger claim that outline returns NO source.
    ;; It does return some — each form's `:args`, verbatim from the file
    ;; (MCP-OP-STUDY-034) — and the charge was 0 only because the walk did not
    ;; know that key. What MCP-OP-STUDY-020 promises is that the charge is
    ;; what CROSSES THE WIRE: for a 126,596-character file, 2,696 characters
    ;; of arglists, and not the file.
    (let [result (result-of
                   (run {"requests" [{"operation" "outline"
                                      "file" "src/clj_surgeon/intent_transaction.clj"}]
                         "expect" {"requests" 1 "files" 1}}))
          charged (inspect/returned-source-character-count result)]
      (is (pos? charged) "the arglists it returns are file source")
      (is (< 65536 (:source_character_count result))
          "while the source it READ is far over the budget")
      (is (< (* 10 charged) (:source_character_count result))
          "the charge is a small fraction of the file, never the file")
      (is (true? (:ok (inspect/enforce-output-budget
                        [result] inspect/default-output-limits)))
          "so an outline of any size file this repository holds still answers")))
  (testing "source a result really does return is still charged"
    (let [result (result-of
                   (run {"requests" [{"operation" "forms"
                                      "file" "src/clj_surgeon/parallel.clj"
                                      "forms" ["bounded-map"]
                                      "expect" {"forms" 1}}]
                         "expect" {"requests" 1 "files" 1}}))]
      (is (pos? (inspect/returned-source-character-count result)))
      (is (= (:source_character_count result)
             (inspect/returned-source-character-count result))
          "a forms read returns exactly the source it counts")
      (is (false? (:ok (inspect/enforce-output-budget
                         [result]
                         (assoc inspect/default-output-limits
                                :per-request-source 1))))
          "returned source still refuses when it exceeds the budget"))))

(defn- write-clj-file!
  [path ns-form & body-lines]
  (fs/create-dirs (fs/parent path))
  (spit (str path)
       (str/join "\n" (concat [ns-form] body-lines))))

(defn- with-scratch-project
  "Populate dir (project-relative to project-root) via build!, run thunk,
   then always delete it — even if the assertions throw."
  [rel-path build! thunk]
  (let [dir (str (fs/path project-root rel-path))]
    (try
      (build! dir)
      (thunk)
      (finally
        (fs/delete-tree dir)))))

;; @spec MCP-OP-STUDY-011
;; @spec MCP-OP-STUDY-007
(deftest ls-tree-names-default-fits-a-many-file-tree-well-inside-the-ceiling
  ;; The field evidence this fixes: over a 116-file directory, the old
  ;; text-only rendering stayed truncated (`read_complete=false`) even at the
  ;; 16384-character ceiling, returning only 13 of 116 files with
  ;; `next_action=narrow_scope` and no executable continuation — permanently
  ;; out of reach. `names` must make a LARGER (190-file) tree fully reachable
  ;; at that same ceiling, via the normal bounded-receipt continuation.
  (with-scratch-project
    "test-fixtures/study/scratch-190"
    (fn [dir]
      (dotimes [i 190]
        (write-clj-file!
          (str dir "/src/scratch/ns" i ".clj")
          (format "(ns scratch.ns%d)" i)
          (format "(defn f%d [] :ok)" i))))
    (fn []
      (let [rel-dir "test-fixtures/study/scratch-190"
            at-default (run {"mode" "ls-tree" "dir" rel-dir})]
        (testing "names is the default rendering, and the default limit alone bounds it"
          (is (true? (:ok at-default)))
          (is (= "names" (:format at-default)))
          (is (= 190 (:file_count at-default)))
          (is (= inspect-tool/ls-tree-default-limit (:limit at-default))
              "the receipt reports the default it was actually bounded by")
          (when (:truncated at-default)
            (is (= "raise_limit_or_narrow_scope" (:next_action at-default)))
            (is (= "inspect_clojure" (get-in at-default [:next_call :tool])))))
        (testing "raising to the ceiling completes the whole 190-file tree"
          (let [response (run {"mode" "ls-tree" "dir" rel-dir "limit" 16384})]
            (is (true? (:ok response)))
            (is (= "names" (:format response)))
            (is (true? (:read_complete response))
                "190 names-format entries must fit well inside the 16384 ceiling")
            (is (false? (:truncated response)))
            (is (= 190 (:file_count response)))
            (is (= 190 (:returned response)))
            (is (= 190 (count (:files response))))
            (is (<= (inspect/json-character-count (:files response)) 16384))
            (doseq [entry (:files response)]
              (is (= #{:file :ns :form_count :line_count} (set (keys entry)))))))))))

;; @spec MCP-OP-STUDY-012
(deftest ls-tree-ns-grep-filters-by-path-not-content
  (with-scratch-project
    "test-fixtures/study/scratch-ns-grep"
    (fn [dir]
      ;; Two real matches: path/namespace names folds or store.
      (write-clj-file! (str dir "/src/cfp_scheduler_killer/folds.clj")
                       "(ns cfp-scheduler-killer.folds)"
                       "(defn fold-matches [] :ok)")
      (write-clj-file! (str dir "/src/cfp_scheduler_killer/store.clj")
                       "(ns cfp-scheduler-killer.store)"
                       "(defn save! [] :ok)")
      ;; Decoys: content mentions folds/store, but the path/namespace does not.
      (write-clj-file! (str dir "/src/cfp_scheduler_killer/scheduler.clj")
                       "(ns cfp-scheduler-killer.scheduler)"
                       ";; restore the schedule store on boot"
                       "(defn boot [] :ok)")
      (write-clj-file! (str dir "/src/cfp_scheduler_killer/api.clj")
                       "(ns cfp-scheduler-killer.api)"
                       ";; handles folds of matching requests"
                       "(defn handler [] :ok)"))
    (fn []
      (let [rel-dir "test-fixtures/study/scratch-ns-grep"]
        (testing "ns_grep narrows to the two path/namespace matches only"
          (let [response (run {"mode" "ls-tree" "dir" rel-dir
                               "ns_grep" "folds|store" "format" "edn"})]
            (is (true? (:ok response)))
            (is (= 2 (:file_count response)))
            (is (= #{"src/cfp_scheduler_killer/folds.clj"
                     "src/cfp_scheduler_killer/store.clj"}
                   (set (map :file (:files response)))))))
        (testing "content grep over-matches the same pattern, including the decoys"
          (let [response (run {"mode" "ls-tree" "dir" rel-dir
                               "grep" "folds|store" "format" "edn"})]
            (is (true? (:ok response)))
            (is (= 4 (:file_count response))
                "grep matches file bodies, so both decoy comments count too")))))))

(defn- counting-outlines
  "Run thunk with every `outline/outline` call counted. `safe-outline` — the
   only thing that opens a source file during :ls-tree — goes through this
   var, so the count is the number of files actually PARSED."
  [thunk]
  (let [calls (atom 0)
        real-outline outline/outline]
    (with-redefs [outline/outline (fn [& args]
                                    (swap! calls inc)
                                    (apply real-outline args))]
      (let [value (thunk)]
        [value @calls]))))

(defn- write-scratch-project!
  [dir file-count]
  (fs/create-dirs dir)
  (spit (str (fs/path dir "deps.edn")) "{:paths [\"src\"]}")
  (dotimes [i file-count]
    (write-clj-file! (str dir "/src/scratch/ns" i ".clj")
                     (format "(ns scratch.ns%d)" i)
                     (format "(defn f%d [] :ok)" i))))

;; @spec MCP-OP-STUDY-015
(deftest ls-tree-refuses-an-oversized-tree-before-parsing-it
  ;; Before this, `ls-tree` outlined — that is, opened and parsed — the WHOLE
  ;; tree before any bound applied: the red team measured 1072 files, 618 MB
  ;; of heap and 2.86 s to return three files, ~0.55 MB per file linear. The
  ;; cap is on DISCOVERY, which only lists names, so the refusal must arrive
  ;; with nothing parsed.
  (with-scratch-project
    "test-fixtures/study/scratch-3000"
    (fn [dir] (write-scratch-project! dir 3000))
    (fn []
      (let [rel-dir "test-fixtures/study/scratch-3000"
            started (System/nanoTime)
            [response outlined] (counting-outlines
                                  #(run {"mode" "ls-tree" "dir" rel-dir}))
            elapsed-ms (/ (- (System/nanoTime) started) 1e6)]
        (is (false? (:ok response)))
        (is (= "study-tree-too-large" (:error_type response)))
        (is (= 2001 (:file_count response))
            "the refusal names the count the walk reached — one past the cap")
        (is (= 2000 (:max_files response)) "and the cap")
        (is (true? (:observed_at_least response))
            "and says that count is a floor, not the tree's true size")
        (is (str/includes? (:error response) "at least 2001"))
        (is (str/includes? (:remedy response) "max_files")
            "and the remedy that raises it")
        (is (false? (:read_complete response)))
        (is (true? (:source_unchanged response)))
        (is (nil? (:tree response)))
        (is (nil? (:files response)))
        (is (< outlined 50)
            "an oversized tree must be refused before it is parsed")
        (is (< elapsed-ms 20000)
            "and the refusal must not cost a whole-tree parse")))))

;; @spec MCP-OP-STUDY-015
;; @spec MCP-OP-STUDY-021
(deftest discovery-counts-and-walks-each-file-once
  ;; MCP-OP-STUDY-015 claimed discovery bounds its work before any receipt
  ;; bound applies. It did not: `max_files` was compared against a count
  ;; `discover-projects` had already materialised in full, and nothing
  ;; deduplicated files across projects. 500 sibling `deps.edn` files each
  ;; declaring `:paths [".."]` all resolve to the SAME directory, so the same
  ;; 500 files were walked 500 times and counted 250,500 times, in 8.4 s.
  (with-scratch-project
    "test-fixtures/study/scratch-overlap-500"
    (fn [dir]
      (fs/create-dirs (str dir "/src"))
      (dotimes [i 500]
        (write-clj-file! (str dir "/src/ns" i ".clj")
                         (format "(ns ns%d)" i)
                         (format "(defn f%d [] :ok)" i)))
      (dotimes [i 500]
        (fs/create-dirs (str dir "/p" i))
        (spit (str dir "/p" i "/deps.edn") "{:paths [\"..\"]}")))
    (fn []
      (let [started (System/nanoTime)
            response (run {"mode" "ls-tree"
                           "dir" "test-fixtures/study/scratch-overlap-500"})
            elapsed-ms (/ (- (System/nanoTime) started) 1e6)]
        (is (true? (:ok response))
            "500 distinct files are far under the 2000-file cap")
        (is (= 500 (:file_count response))
            "the same file reached through 500 projects is still one file")
        (is (< elapsed-ms 1000)
            (str "one walk per source DIRECTORY, not per project; took "
                 elapsed-ms " ms")))))
  (testing "a root project declaring [\".\"] beside a nested project"
    (with-scratch-project
      "test-fixtures/study/scratch-overlap-nested"
      (fn [dir]
        (fs/create-dirs (str dir "/nested/src"))
        (spit (str dir "/deps.edn") "{:paths [\".\"]}")
        (spit (str dir "/nested/deps.edn") "{:paths [\"src\"]}")
        (write-clj-file! (str dir "/nested/src/a.clj") "(ns a)" "(defn a-fn [] :ok)")
        (write-clj-file! (str dir "/top.clj") "(ns top)" "(defn top-fn [] :ok)"))
      (fn []
        (let [response (run {"mode" "ls-tree"
                             "dir" "test-fixtures/study/scratch-overlap-nested"
                             "format" "edn" "limit" 16384})
              listed (map :file (:files response))]
          (is (true? (:ok response)))
          (is (= 2 (:file_count response))
              "a two-file tree reported five files before deduplication")
          (is (= ["nested/src/a.clj" "top.clj"] (sort listed)))
          (is (= (count listed) (count (distinct listed)))
              "and printed each of them two and three times over"))))))

;; @spec MCP-OP-STUDY-021
(deftest a-capped-discovery-stops-walking-rather-than-counting-afterwards
  ;; The cap has to stop the WALK. With 40 sibling projects over one 60-file
  ;; source tree and a cap of 10, only the directories reached before the cap
  ;; was passed may be listed at all.
  (with-scratch-project
    "test-fixtures/study/scratch-cap-walk"
    (fn [dir]
      (dotimes [i 40]
        (fs/create-dirs (str dir "/p" i "/src"))
        (spit (str dir "/p" i "/deps.edn") "{:paths [\"src\"]}")
        (dotimes [j 3]
          (write-clj-file! (str dir "/p" i "/src/ns" i "_" j ".clj")
                           (format "(ns p%d.ns%d)" i j)
                           "(defn f [] :ok)"))))
    (fn []
      (let [response (run {"mode" "ls-tree"
                           "dir" "test-fixtures/study/scratch-cap-walk"
                           "max_files" 10})]
        (is (false? (:ok response)))
        (is (= "study-tree-too-large" (:error_type response)))
        (is (= 10 (:max_files response)))
        (is (<= (:file_count response) 12)
            (str "discovery must stop at the cap, not walk all 120 files: "
                 (:file_count response)))
        (is (str/includes? (:error response) "at least")
            "a halted discovery says its count is a floor, not a total")
        (is (nil? (:tree response)))
        (is (nil? (:files response))))))
  (testing "a single oversized project stops at the cap too"
    ;; This block used to assert the opposite — that one oversized project
    ;; names its EXACT count — which was true only because the cap stopped
    ;; between candidates and let a whole candidate materialise first. Under
    ;; MCP-OP-STUDY-033 the walk stops at `cap + 1` inside the candidate, so
    ;; the count it can honestly report is a floor.
    (with-scratch-project
      "test-fixtures/study/scratch-cap-one"
      (fn [dir] (write-scratch-project! dir 60))
      (fn []
        (let [response (run {"mode" "ls-tree"
                             "dir" "test-fixtures/study/scratch-cap-one"
                             "max_files" 10})]
          (is (false? (:ok response)))
          (is (= 11 (:file_count response))
              "the walk stops one file past the cap")
          (is (true? (:observed_at_least response)))
          (is (str/includes? (:error response) "at least")
              "so the count it reports is a floor, and the receipt says so"))))))

;; @spec MCP-OP-STUDY-015
(deftest ls-tree-outlines-only-the-files-the-receipt-can-carry
  ;; The other half of the same defect: even under the cap, a receipt that
  ;; returns a few dozen files must not parse hundreds.
  (with-scratch-project
    "test-fixtures/study/scratch-400"
    (fn [dir] (write-scratch-project! dir 400))
    (fn []
      (let [rel-dir "test-fixtures/study/scratch-400"
            [response outlined] (counting-outlines
                                  #(run {"mode" "ls-tree" "dir" rel-dir}))]
        (is (true? (:ok response)))
        (is (= 400 (:file_count response)))
        (is (true? (:truncated response)))
        (is (pos? (:returned response)))
        (is (< (:returned response) 400))
        (is (< outlined 400)
            "the whole tree must not be parsed to render a bounded receipt")
        (is (<= outlined (+ (:returned response) 16))
            "at most one outlining batch beyond what the receipt returned")))))

;; ============================================================
;; Confinement
;; ============================================================

;; @spec MCP-OP-STUDY-006
(deftest ls-tree-refuses-outside-the-workspace-without-scanning
  (doseq [[dir expected]
          [["../../etc" "invalid-relative-directory-path"]
           ["/etc" "invalid-relative-directory-path"]
           ["src/../.." "invalid-relative-directory-path"]
           ["src/does-not-exist" "directory-not-found"]
           [real-file "path-not-directory"]]]
    (testing dir
      (let [response (run {"mode" "ls-tree" "dir" dir})]
        (is (false? (:ok response)))
        (is (= expected (:error_type response)))
        (is (false? (:read_complete response)))
        (is (true? (:source_unchanged response)))
        (is (= "correct_request" (:next_action response)))
        (is (= "inspect_clojure" (get-in response [:next_call :tool])))
        (is (nil? (:tree response)))
        (is (nil? (:files response)))))))

;; @spec MCP-OP-STUDY-006
;; @spec MCP-OP-STUDY-014
(deftest ls-tree-does-not-read-through-a-symlink-out-of-the-root
  ;; The MCP-OP-STUDY-006 falsifier the suite promised ("a symlink out of the
  ;; root") but did not have. Two escapes, both executed against the branch
  ;; bytes: `find` reports a symlink by the LINK's own name, so
  ;; `src/leak.clj -> /etc/passwd` matched `-name '*.clj'` and was outlined —
  ;; that is, slurped; and `:paths ["../../.."]` in a scanned deps.edn reached
  ;; `find` unnormalized and moved the whole scan outside the root.
  ;;
  ;; `<fixture>/a/b` is the scanned directory, so the traversal escape lands on
  ;; `<fixture>` and its decoy file — bounded, deterministic, and off the
  ;; workspace's real sources.
  (with-scratch-project
    "test-fixtures/study/scratch-confine"
    (fn [dir]
      (let [scan-root (str (fs/path dir "a" "b"))]
        (write-clj-file! (str (fs/path scan-root "proj" "src" "real.clj"))
                         "(ns real)"
                         "(defn only-file [] :ok)")
        (spit (str (fs/path scan-root "proj" "deps.edn")) "{:paths [\"src\"]}")
        (fs/create-sym-link (str (fs/path scan-root "proj" "src" "leak.clj"))
                            "/etc/passwd")
        (fs/create-dirs (str (fs/path scan-root "escape")))
        (spit (str (fs/path scan-root "escape" "deps.edn"))
              "{:paths [\"../../..\"]}")
        (write-clj-file! (str (fs/path dir "decoy.clj"))
                         "(ns decoy)"
                         "(defn decoy-fn [] :no)")))
    (fn []
      (let [rel-dir "test-fixtures/study/scratch-confine/a/b"]
        (doseq [output-format ["names" "edn" "text"]]
          (testing output-format
            (let [response (run {"mode" "ls-tree" "dir" rel-dir
                                 "format" output-format "limit" 16384})
                  payload (pr-str (select-keys response [:tree :files]))]
              (is (true? (:ok response)))
              (is (= 1 (:file_count response))
                  "exactly the one real source file inside the scan root")
              (is (= 1 (:returned response)))
              (is (str/includes? payload "real.clj"))
              (is (not (str/includes? payload "leak"))
                  "a .clj symlink resolving out of the root must not be listed")
              (is (not (str/includes? payload "root:"))
                  "and its bytes must never be read")
              (is (not (str/includes? payload "decoy"))
                  "an unnormalized :paths traversal must not move the scan out"))))))))

;; @spec MCP-OP-STUDY-006
(deftest ls-tree-refuses-a-directory-without-clojure-sources
  (let [response (run {"mode" "ls-tree" "dir" "docs/intent/study-ops"})]
    (is (false? (:ok response)))
    (is (= "no-clojure-files" (:error_type response)))
    (is (str/starts-with? (:error response) "No Clojure files found under "))
    (is (some? (:next_call response)))))

(def ^:private absent-pattern
  "A pattern no file in this workspace can contain, spelled so that this
   file does not contain it either."
  (str "zzzz" "nosuch" "pattern"))

;; @spec MCP-OP-STUDY-019
(deftest ls-tree-validates-its-own-parameters-server-side
  ;; The ls-tree branch skipped `validate-inspect-params` entirely: an unknown
  ;; `format` fell through the render `case` to the text branch while the
  ;; receipt echoed the caller's raw string back as if it had been honoured,
  ;; and an unknown top-level key was silently ignored.
  (testing "format is checked against the enum, never echoed"
    (let [response (run {"mode" "ls-tree" "dir" fixture-dir "format" "EDN"})]
      (is (false? (:ok response)))
      (is (= "invalid-format" (:error_type response)))
      (is (= "EDN" (:format response)) "the refusal names what was rejected")
      (is (= ["edn" "names" "text"] (:supported response)))
      (is (false? (:read_complete response)))
      (is (true? (:source_unchanged response)))
      (is (nil? (:tree response)))
      (is (nil? (:files response)))
      (is (some? (:next_call response)))
      (is (nil? (get-in response [:next_call :arguments :format]))
          "the continuation must not repeat the rejected value")))
  (testing "an unknown top-level key refuses instead of being ignored"
    (let [response (run {"mode" "ls-tree" "dir" fixture-dir "formatt" "edn"})]
      (is (false? (:ok response)))
      (is (= "unknown-parameter" (:error_type response)))
      (is (= ["formatt"] (:unknown response)))
      (is (some? (:supported response)))
      (is (nil? (:tree response)))
      (is (nil? (:files response)))))
  (testing "every documented parameter is accepted"
    (let [response (run {"mode" "ls-tree" "dir" fixture-dir "format" "edn"
                         "grep" "ns" "ns_grep" "logging" "limit" 16384
                         "max_files" 100})]
      (is (true? (:ok response)))
      (is (= "edn" (:format response))))))

(defn- output-schema-violations
  "Every receipt key whose value contradicts the type `inspect-output-schema`
   declares for it. The MCP adapter validates structured output against that
   schema, so a violation is exactly what reaches a caller as `isError` with
   no `error_type` — the failure this guards against."
  [receipt]
  (let [matches? (fn [declared value]
                   (some (fn [type]
                           (case type
                             "string" (string? value)
                             "integer" (integer? value)
                             "number" (number? value)
                             "boolean" (boolean? value)
                             "object" (map? value)
                             "array" (sequential? value)
                             "null" (nil? value)
                             true))
                         (if (vector? declared) declared [declared])))]
    (vec (for [[field schema] (:properties inspect-tool/inspect-output-schema)
               :let [key (keyword field)
                     declared (:type schema)]
               :when (and declared (contains? receipt key)
                          (not (matches? declared (get receipt key))))]
           {:field field :value (pr-str (get receipt key))}))))

(defn- object-schema-violations
  "Minimal structural check of `value` (a map with string keys) against a
   JSON-Schema-shaped object schema: every `:required` key present, every
   declared `:enum` honoured, and — when `:additionalProperties` is false —
   no key outside `:properties`. Same hand-rolled-checker shape as
   `output-schema-violations` above, one level deeper, so a `paths_unresolved`
   entry can be validated against its own published item schema and not just
   against \"is the container an array\"."
  [schema value]
  (let [{:keys [properties required]} schema
        additional? (:additionalProperties schema)]
    (vec (concat
          (for [k required :when (not (contains? value k))]
            {:missing k})
          (keep (fn [[k v]]
                  (let [prop (get properties k)]
                    (cond
                      (nil? prop)
                      (when (false? additional?) {:unknown-key k})
                      (and (:enum prop) (not (contains? (set (:enum prop)) v)))
                      {:field k :not-in-enum v})))
                value)))))

;; @spec MCP-OP-STUDY-022
(deftest ls-tree-refuses-an-uncompilable-ns-grep-with-a-typed-error
  ;; `ns_grep` compiled its pattern with `re-pattern` once PER FILE and under
  ;; no guard, so `"["` threw a raw PatternSyntaxException out of the read
  ;; entrance — the adapter turned it into `mcp-adapter-failure` carrying a
  ;; Java message, with no error_type, no read_complete and no continuation.
  (testing "an uncompilable ns_grep is a typed refusal, not an exception"
    (let [response (run {"mode" "ls-tree" "dir" fixture-dir "ns_grep" "["})]
      (is (false? (:ok response)))
      (is (= "invalid-ns-grep-pattern" (:error_type response)))
      (is (false? (:read_complete response)))
      (is (true? (:source_unchanged response)))
      (is (string? (:next_action response)))
      (is (not= "mcp-adapter-failure" (:error_type response)))
      (is (empty? (output-schema-violations response)))))
  (testing "a compilable ns_grep still scans"
    (let [response (run {"mode" "ls-tree" "dir" fixture-dir "ns_grep" "logging"})]
      (is (true? (:ok response)))
      (is (empty? (output-schema-violations response))))))

;; @spec MCP-OP-STUDY-022
(deftest ls-tree-refuses-a-wrongly-typed-parameter-server-side
  ;; `ns_grep 5` threw a ClassCastException out of the entrance. `grep 5` was
  ;; worse: it passed the `^-` guard (`starts-with?` stringifies its
  ;; argument), scanned for the pattern "5", and returned a receipt whose
  ;; `grep` was an integer — which fails this tool's own OUTPUT schema and
  ;; reaches the caller as `isError` with no error_type at all. `limit "x"`
  ;; did the same through `invalid-study-limit`, which echoed the string into
  ;; an integer-typed field.
  (testing "a wrongly typed parameter is a typed refusal, not a scan"
    (doseq [[label request expected-parameter]
            [["grep" {"mode" "ls-tree" "dir" fixture-dir "grep" 5} "grep"]
             ["limit" {"mode" "ls-tree" "dir" fixture-dir "limit" "x"} "limit"]
             ["max_files" {"mode" "ls-tree" "dir" fixture-dir "max_files" "x"} "max_files"]
             ["ns_grep" {"mode" "ls-tree" "dir" fixture-dir "ns_grep" 5} "ns_grep"]]]
      (testing label
        (let [response (run request)]
          (is (false? (:ok response)))
          (is (= "invalid-parameter-type" (:error_type response)))
          (is (= [expected-parameter] (map :parameter (:invalid response))))
          (is (false? (:read_complete response)))
          (is (true? (:source_unchanged response)))
          (is (not= "mcp-adapter-failure" (:error_type response)))
          (is (nil? (:tree response)))
          (is (nil? (:files response)))
          (is (empty? (output-schema-violations response))
              "a receipt that breaks its own output schema reaches the caller as isError")
          (is (nil? (get-in response [:next_call :arguments (keyword expected-parameter)]))
              "the continuation must not repeat the rejected value"))))))

;; @spec MCP-OP-STUDY-007
(deftest ls-tree-refusal-serves-no-continuation-identical-to-the-request
  ;; `ls-tree-refusal` attached `{:dir "."}` unconditionally, so a failed
  ;; scan AT the root handed back the exact request that had just failed.
  ;;
  ;; The pattern is assembled from pieces so the literal it greps for cannot
  ;; occur in this file — a spelled-out pattern would match this very test
  ;; source and the scan would succeed.
  (doseq [request [{"mode" "ls-tree" "dir" "." "grep" absent-pattern}
                   {"mode" "ls-tree" "grep" absent-pattern}]]
    (testing (pr-str request)
      (let [response (run request)]
        (is (false? (:ok response)))
        (is (= "no-clojure-files" (:error_type response)))
        (is (nil? (:next_call response))
            "the {:dir \".\"} continuation IS the call just made")
        (is (= "narrow_scope" (:next_action response)))
        (is (string? (:remedy response))))))
  (testing "a refusal whose continuation can still advance keeps it"
    (let [response (run {"mode" "ls-tree" "dir" "docs/intent/study-ops"})]
      (is (false? (:ok response)))
      (is (= "no-clojure-files" (:error_type response)))
      (is (= "." (get-in response [:next_call :arguments :dir])))
      (is (= "correct_request" (:next_action response))))))

;; @spec MCP-OP-STUDY-007
;; @spec MCP-OP-STUDY-023
(deftest spelling-a-default-does-not-bring-the-self-returning-call-back
  ;; The identical-call check compares the proposed continuation against the
  ;; arguments of the call just made. `ls-tree-next-call` never carried
  ;; `:limit` while `ls-tree-request-arguments` kept it, so a caller who
  ;; spelled the default — `limit 4096`, exactly what the receipt reports back
  ;; — made the two differ, and the refusal served an executable call that was
  ;; the request that had just failed.
  (doseq [request [{"mode" "ls-tree" "dir" "." "grep" absent-pattern "limit" 4096}
                   {"mode" "ls-tree" "dir" "." "grep" absent-pattern
                    "limit" 4096 "format" "names"}
                   {"mode" "ls-tree" "dir" "." "grep" absent-pattern
                    "limit" 4096 "max_files" 2000}]]
    (testing (pr-str request)
      (let [response (run request)]
        (is (false? (:ok response)))
        (is (= "no-clojure-files" (:error_type response)))
        (is (nil? (:next_call response))
            "the continuation IS the call just made, default spelled or not")
        (is (= "narrow_scope" (:next_action response))))))
  (testing "and a continuation that really can advance still carries the limit"
    (let [response (run {"mode" "ls-tree" "dir" "docs/intent/study-ops"
                         "limit" 4096})]
      (is (false? (:ok response)))
      (is (= "." (get-in response [:next_call :arguments :dir])))
      (is (= 4096 (get-in response [:next_call :arguments :limit]))
          "a continuation that drops a supplied field is a different call"))))

;; @spec MCP-OP-STUDY-006
(deftest ls-tree-refuses-a-flag-shaped-grep-or-ns-grep-over-the-wire
  ;; A pattern beginning with '-' (e.g. "--pre=/bin/sh") would otherwise
  ;; reach ripgrep looking like a flag; must refuse before any scan runs.
  (testing "grep"
    (let [response (run {"mode" "ls-tree" "dir" fixture-dir
                         "grep" "--pre=/bin/sh"})]
      (is (false? (:ok response)))
      (is (= "invalid-grep-pattern" (:error_type response)))
      (is (false? (:read_complete response)))
      (is (some? (:next_call response)))))
  (testing "ns_grep"
    (let [response (run {"mode" "ls-tree" "dir" fixture-dir "ns_grep" "-x"})]
      (is (false? (:ok response)))
      (is (= "invalid-ns-grep-pattern" (:error_type response)))
      (is (false? (:read_complete response))))))

;; @spec MCP-OP-STUDY-006
;; @spec MCP-OP-STUDY-026
(deftest no-ls-tree-refusal-publishes-a-host-absolute-path
  ;; MCP-OP-STUDY-006 forbids publishing a host-absolute path, and the two
  ;; kernel refusals broke it: `no-clojure-files` and `study-tree-too-large`
  ;; embedded the CANONICAL scan root, so a caller who asked for
  ;; "docs/intent/study-ops" was told about
  ;; "/home/…/clj-surgeon-study/docs/intent/study-ops".
  (with-scratch-project
    "test-fixtures/study/scratch-absolute-paths"
    (fn [dir] (write-scratch-project! dir 30))
    (fn []
      (let [rel-dir "test-fixtures/study/scratch-absolute-paths"]
        (doseq [request [{"mode" "ls-tree" "dir" "docs/intent/study-ops"}
                         {"mode" "ls-tree" "dir" rel-dir "max_files" 5}
                         {"mode" "ls-tree" "dir" rel-dir "grep" absent-pattern}
                         {"mode" "ls-tree" "dir" rel-dir "ns_grep" absent-pattern}
                         {"mode" "ls-tree" "dir" rel-dir "ns_grep" "["}
                         {"mode" "ls-tree" "dir" rel-dir "ns_grep" "-x"}
                         {"mode" "ls-tree" "dir" rel-dir "grep" "--pre=/bin/sh"}
                         {"mode" "ls-tree" "dir" rel-dir "format" "EDN"}
                         {"mode" "ls-tree" "dir" rel-dir "limit" 99999}
                         {"mode" "ls-tree" "dir" rel-dir "max_files" 0}
                         {"mode" "ls-tree" "dir" rel-dir "formatt" "edn"}
                         {"mode" "ls-tree" "dir" rel-dir "grep" 5}
                         {"mode" "ls-tree" "dir" "no-such-directory"}]]
          (testing (pr-str request)
            (let [response (run request)]
              (is (false? (:ok response)))
              (is (not (re-find #"(?m)(^|\s)/[^\s]+" (str (:error response))))
                  (str "the message names a host-absolute path: "
                       (pr-str (:error response))))
              (is (not (str/includes? (str (:error response)) project-root))
                  "and must not publish where the workspace lives"))))
        (testing "the refusals that name a directory name the one the caller asked for"
          (let [empty-dir (run {"mode" "ls-tree" "dir" "docs/intent/study-ops"})
                too-large (run {"mode" "ls-tree" "dir" rel-dir "max_files" 5})]
            (is (= "No Clojure files found under docs/intent/study-ops"
                   (:error empty-dir)))
            (is (str/includes? (:error too-large) (str "under " rel-dir)))))))))

;; @spec MCP-OP-STUDY-025
(deftest a-rejected-pattern-is-named-but-never-handed-back
  ;; `invalid-format` was given the drop treatment; the two pattern refusals
  ;; were not, so their `{:dir "."}` continuation still carried the pattern
  ;; that had just been rejected — an executable call that fails identically.
  (doseq [[label request field]
          [["flag-shaped grep" {"mode" "ls-tree" "dir" fixture-dir
                                "grep" "--pre=/bin/sh"} :grep]
           ["flag-shaped ns_grep" {"mode" "ls-tree" "dir" fixture-dir
                                   "ns_grep" "-x"} :ns_grep]
           ["uncompilable ns_grep" {"mode" "ls-tree" "dir" fixture-dir
                                    "ns_grep" "("} :ns_grep]]]
    (testing label
      (let [response (run request)
            rejected (get request (name field))]
        (is (false? (:ok response)))
        (is (= rejected (get response field))
            "the refusal still names what was rejected")
        (is (some? (:next_call response)))
        (is (nil? (get-in response [:next_call :arguments field]))
            "the continuation must not repeat the rejected pattern")
        (is (= fixture-dir (get-in response [:next_call :arguments :dir]))
            "and must keep the scope the caller already chose"))))
  (testing "the same treatment invalid-format already had"
    (let [response (run {"mode" "ls-tree" "dir" fixture-dir "format" "EDN"})]
      (is (= "EDN" (:format response)))
      (is (nil? (get-in response [:next_call :arguments :format]))))))

;; ============================================================
;; Bounding
;; ============================================================

;; @spec MCP-OP-STUDY-007
(deftest a-study-receipt-is-bounded-and-says-so
  (testing "a tight limit truncates at row granularity with a continuation"
    (let [response (one "deps" {"limit" 200})
          result (result-of response)]
      (is (true? (:truncated result)))
      (is (pos? (:omitted result)))
      (is (< (:returned result) (:form_count result)))
      (is (<= (inspect/json-character-count (:deps result)) 200))
      (is (= "raise_limit_or_narrow_scope" (:next_action result)))
      (is (= "inspect_clojure" (get-in result [:next_call :tool])))
      (testing "and the batch refuses to call itself terminal"
        (is (false? (:read_complete response)))
        (is (true? (:truncated response)))
        (is (= "raise_limit_or_narrow_scope" (:next_action response))))))

  (testing "an atomic tree refuses rather than returning half a tree"
    (let [response (one "ls-deps" {"form" "extraction-closure" "limit" 50})]
      (is (false? (:ok response)))
      (is (= "study-output-limit" (:error_type response)))
      (is (= 50 (:limit response)))
      (is (pos? (:required response)))
      (is (= "inspect_clojure" (get-in response [:next_call :tool])))))

  (testing "an oversized atomic result at the ceiling serves no call that cannot advance"
    ;; `study-oversized` computed its continuation as
    ;; `(min study-max-limit (max required limit))`, which EQUALS limit at the
    ;; ceiling: the receipt handed back the exact call just made. No single
    ;; file in this repository has an ls-deps tree over 16384 characters, so
    ;; the ceiling is lowered to reach the branch on real bytes.
    (with-redefs [inspect/study-max-limit 50]
      (let [response (one "ls-deps" {"form" "extraction-closure" "limit" 50})]
        (is (false? (:ok response)))
        (is (= "study-output-limit" (:error_type response)))
        (is (= 50 (:limit response)))
        (is (pos? (:required response)))
        (is (nil? (:next_call response))
            "an executable call identical to the one just made is a loop")
        (is (= "narrow_scope" (:next_action response)))
        (is (string? (:remedy response))))))

  (testing "ls-tree truncates whole files and stays a valid tree"
    (let [response (run {"mode" "ls-tree" "dir" fixture-dir "format" "text"
                         "limit" 700})]
      (is (true? (:truncated response)))
      (is (false? (:read_complete response)))
      (is (< (:returned response) (:file_count response)))
      (is (<= (count (:tree response)) 700))
      (is (str/includes? (:tree response) "total:"))
      (is (= 16384 (get-in response [:next_call :arguments :limit])))))

  (testing "at the maximum limit no continuation is served that cannot advance"
    (let [response (run {"mode" "ls-tree" "dir" "src" "format" "text"
                         "limit" 16384})]
      (is (true? (:truncated response)))
      (is (nil? (:next_call response))
          "an executable call identical to the one just made is a loop")
      (is (= "narrow_scope" (:next_action response)))
      (is (string? (:remedy response))))
    ;; No single file in this repository has a deps result over 16384
    ;; characters, so the ceiling is lowered to reach the same branch on real
    ;; bytes rather than asserted on a fixture that cannot occur.
    (with-redefs [inspect/study-max-limit 200]
      (let [result (result-of (one "deps" {"limit" 200}))]
        (is (true? (:truncated result)))
        (is (nil? (:next_call result)))
        (is (= "narrow_scope" (:next_action result)))
        (is (string? (:remedy result))))))

  (testing "a limit above the maximum refuses"
    (let [response (one "deps" {"limit" 99999})]
      (is (false? (:ok response)))
      (is (= "invalid-study-limit" (:reason response))))
    (let [response (run {"mode" "ls-tree" "dir" fixture-dir "limit" 99999})]
      (is (false? (:ok response)))
      (is (= "invalid-study-limit" (:error_type response))))))


;; @spec MCP-OP-STUDY-007
;; @spec MCP-OP-STUDY-027
(deftest an-atomic-result-too-big-for-the-ceiling-serves-no-continuation
  ;; `raisable?` asked only whether the proposed limit DIFFERED from the one
  ;; just used. An atomic result needing more than the 16,384 ceiling can
  ;; never be returned at any limit, so at limit 4096 the receipt handed back
  ;; an executable `limit 16384` that is known, right here, to fail exactly as
  ;; this call did. Raising has to be able to succeed, not merely to change
  ;; the number.
  (let [file "src/clj_surgeon/intent_transaction.clj"
        response (run {"requests" [{"operation" "ls-deps"
                                    "file" file
                                    "form" "execute-mcp-change!"
                                    "limit" 4096}]
                       "expect" {"requests" 1 "files" 1}})]
    (is (false? (:ok response)))
    (is (= "study-output-limit" (:error_type response)))
    (is (= 4096 (:limit response)))
    (is (< inspect/study-max-limit (:required response))
        (str "the fixture only bites while one atomic result cannot fit the "
             "ceiling; required was " (:required response)))
    (is (nil? (:next_call response))
        "a continuation that cannot succeed is a loop with extra steps")
    (is (= "narrow_scope" (:next_action response)))
    (is (string? (:remedy response))))
  (testing "a result that the ceiling CAN carry still gets its continuation"
    (let [response (run {"requests" [{"operation" "ls-deps"
                                      "file" "src/clj_surgeon/analyze.clj"
                                      "form" "extraction-closure"
                                      "limit" 50}]
                         "expect" {"requests" 1 "files" 1}})]
      (is (= "study-output-limit" (:error_type response)))
      (is (<= (:required response) inspect/study-max-limit))
      (is (= "inspect_clojure" (get-in response [:next_call :tool])))
      (is (= "raise_limit_or_narrow_scope" (:next_action response))))))

;; @spec MCP-OP-STUDY-018
(deftest bound-rows-charges-the-arrays-own-characters
  ;; The array's own brackets and separators were not charged, so a kept
  ;; payload could be exactly limit+1 characters: each row paid for one
  ;; separator, which covers n of the n+1 punctuation characters an n-element
  ;; JSON array actually costs. Fixed-width names make the overflow limits
  ;; deterministic rather than incidental.
  (let [rows (mapv (fn [i] {:name (format "form-%03d" i) :depends_on []})
                   (range 60))]
    (doseq [n (range 1 401)]
      (let [[kept _omitted _truncated?] (inspect/bound-rows rows n)
            cost (inspect/json-character-count kept)]
        (is (<= cost (max 2 n))
            (str "limit " n ": kept " (count kept) " rows costing " cost))
        (when (seq kept)
          (is (<= cost n)
              (str "limit " n ": a non-empty payload must fit its limit")))))))

;; @spec MCP-OP-STUDY-018
;; @spec MCP-OP-STUDY-030
(deftest every-ls-tree-format-has-a-documented-empty-receipt-floor
  ;; MCP-OP-STUDY-018 named the floor for row payloads — the empty array's two
  ;; characters — and left `text` undocumented, so its 38-character empty
  ;; receipt at `limit 1` looked like a bound violation rather than the floor
  ;; it is. A receipt that showed nothing must still say how much it omitted;
  ;; that line IS the floor.
  (testing "text bottoms out at its own total line, larger than limit 1"
    (let [response (run {"mode" "ls-tree" "dir" fixture-dir
                         "format" "text" "limit" 1})]
      (is (true? (:ok response)))
      (is (zero? (:returned response)))
      (is (true? (:truncated response)))
      (is (false? (:read_complete response)))
      (is (= 7 (:file_count response)))
      (is (= (str "── total: " (:file_count response) " files; 0 shown, "
                  (:omitted response) " omitted\n")
             (:tree response))
          "the floor is the total line and nothing else")
      (is (> (count (:tree response)) 1)
          "a text receipt cannot be smaller than the line that reports what it omitted")))
  (testing "and that line is 36 + 2 x digits(file_count) characters wide"
    ;; The floor was DOCUMENTED as "38 characters for a two-digit tree", which
    ;; is a one-digit tree's floor attached to the wrong width: `shown` is 0
    ;; and `omitted` equals `file_count`, so the count is spelled twice and
    ;; every extra digit costs two characters. The test computes the width
    ;; from the documented formula rather than pinning one number, so the
    ;; source comment and the payload cannot disagree again in silence.
    (let [floor (fn [file-count] (+ 36 (* 2 (count (str file-count)))))]
      (is (= [38 40 42 46] (mapv floor [7 50 100 20000]))
          "the formula, stated once, by hand")
      (doseq [file-count [7 50 100]]
        (testing (str file-count " files")
          (with-scratch-project
            "test-fixtures/study/scratch-floor"
            (fn [dir] (write-scratch-project! dir file-count))
            (fn []
              (let [response (run {"mode" "ls-tree"
                                   "dir" "test-fixtures/study/scratch-floor"
                                   "format" "text" "limit" 1})]
                (is (zero? (:returned response)))
                (is (= file-count (:file_count response)))
                (is (= (floor file-count) (count (:tree response)))))))))))
  (testing "names and edn bottom out at the empty array's two characters"
    (doseq [format ["names" "edn"]]
      (testing format
        (let [response (run {"mode" "ls-tree" "dir" fixture-dir
                             "format" format "limit" 1})]
          (is (true? (:ok response)))
          (is (zero? (:returned response)))
          (is (= [] (:files response)))
          (is (= 2 (inspect/json-character-count (:files response)))))))))

;; @spec MCP-OP-STUDY-016
(deftest a-study-receipt-counts-the-source-it-read
  ;; `source_character_count` was hardcoded 0 for every study operation and
  ;; for outline, so the receipt understated what it read AND the per-request
  ;; source budget in `enforce-output-budget` could never see a study read.
  (let [expected (count (slurp real-file))]
    (doseq [[operation extra]
            [["deps" {"limit" 16384}]
             ["deps" {"form" "dep-tree" "limit" 16384}]
             ["topo" {"limit" 16384}]
             ["ls-deps" {"form" "extraction-closure" "limit" 16384}]
             ["ls-extract" {"form" "extraction-closure" "limit" 16384}]
             ["outline" {}]]]
      (testing (str operation " " (pr-str extra))
        (let [result (result-of (one operation extra))]
          (is (= expected (:source_character_count result))
              "the receipt must report the bytes it actually read"))))))

;; @spec MCP-OP-STUDY-016
(deftest topo-counts-and-bounds-its-cycles
  ;; `:cycles` was outside the budget entirely and `form_count` counted only
  ;; `:sorted`, so a file that is ALL cycle reported `form_count 0` while
  ;; listing every cycle member whatever the limit said.
  (with-scratch-project
    "test-fixtures/study/scratch-cycles"
    (fn [dir]
      (apply write-clj-file!
             (str dir "/src/cyc.clj")
             "(ns cyc)"
             (concat
               [(str "(declare "
                     (str/join " " (map #(str "f" %) (range 40)))
                     ")")]
               (map #(format "(defn f%d [] (f%d))" % (mod (inc %) 40))
                    (range 40)))))
    (fn []
      (let [file "test-fixtures/study/scratch-cycles/src/cyc.clj"
            request (fn [limit]
                      (result-of
                        (run {"requests" [{"operation" "topo"
                                           "file" file
                                           "limit" limit}]
                              "expect" {"requests" 1 "files" 1}})))]
        (testing "an all-cycle file reports the forms it found"
          (let [result (request 16384)]
            (is (empty? (get-in result [:topo :sorted]))
                "nothing can be topologically ordered in a pure cycle")
            (is (seq (get-in result [:topo :cycles])))
            (is (pos? (:form_count result))
                "form_count counted only :sorted, so it reported 0 here")
            (is (= (count (get-in result [:topo :cycles])) (:returned result)))
            (is (= (count (slurp file)) (:source_character_count result)))))
        (testing "and charges them to the same byte budget"
          (let [limit 200
                result (request limit)]
            (is (true? (:truncated result)))
            (is (pos? (:omitted result)))
            (is (< (count (get-in result [:topo :cycles]))
                   (:form_count result)))
            (is (<= (inspect/json-character-count (:topo result)) limit)
                "the whole topo payload, cycles included, stays inside limit")))))))

;; @spec MCP-OP-STUDY-016
(deftest an-atomic-study-payload-refuses-instead-of-exceeding-its-budget
  (testing "one adjacency row is atomic and cannot be silently oversized"
    (let [response (one "deps" {"form" "dep-tree" "limit" 20})]
      (is (false? (:ok response)))
      (is (= "study-output-limit" (:error_type response)))
      (is (= 20 (:limit response)))
      (is (pos? (:required response)))))
  (testing "an extraction closure charges its envelope, not only its forms"
    (let [response (one "ls-extract" {"form" "extraction-closure" "limit" 20})]
      (is (false? (:ok response)))
      (is (= "study-output-limit" (:error_type response)))))
  (testing "a closure that fits its envelope stays inside the whole budget"
    (let [limit 400
          result (result-of (one "ls-extract" {"form" "extraction-closure"
                                               "limit" limit}))]
      (is (<= (inspect/json-character-count (:closure result)) limit)))))

;; @spec MCP-OP-STUDY-024
(deftest per-project-headers-report-true-counts-and-name-dropped-projects
  ;; The per-project header counted the outlines it was handed, so a project
  ;; showing 9 of 50 files announced itself as `(9 files, N forms)` — the same
  ;; body-contradicts-its-own-receipt defect MCP-OP-STUDY-017 fixed for the
  ;; total line. Worse, a project the byte budget never reached was dropped
  ;; from the body entirely while `project_count` still counted it.
  (with-scratch-project
    "test-fixtures/study/scratch-two-projects"
    (fn [dir]
      (doseq [project ["alpha" "beta"]]
        (fs/create-dirs (str dir "/" project))
        (spit (str dir "/" project "/deps.edn") "{:paths [\"src\"]}")
        (dotimes [i 50]
          (write-clj-file! (str dir "/" project "/src/" project "/ns" i ".clj")
                           (format "(ns %s.ns%d)" project i)
                           (format "(defn f%d [] :ok)" i)))))
    (fn []
      (let [rel-dir "test-fixtures/study/scratch-two-projects"
            header (fn [tree project]
                     (first (filter #(str/starts-with? % (str "── " project " "))
                                    (str/split-lines tree))))
            counted (fn [tree project]
                      (count (filter #(str/starts-with? % (str project "/src/"))
                                     (str/split-lines tree))))]
        (testing "a header names the files DISCOVERY found, and how many are shown"
          (let [response (run {"mode" "ls-tree" "dir" rel-dir
                               "format" "text" "limit" 5000})
                tree (:tree response)]
            (is (true? (:truncated response)))
            (is (= 100 (:file_count response)))
            (is (= 2 (:project_count response)))
            (doseq [project ["alpha" "beta"]]
              (let [line (header tree project)
                    shown (counted tree project)]
                (is (some? line) (str project " must be named"))
                (is (= (if (= 50 shown)
                         (str "── " project " (50 files, 50 forms)")
                         (str "── " project " (50 files; " shown " shown)"))
                       line)
                    "the header must report 50 discovered, not the number shown")))))
        (testing "a project the bound never reached is still named"
          (let [response (run {"mode" "ls-tree" "dir" rel-dir
                               "format" "text" "limit" 600})
                tree (:tree response)]
            (is (true? (:truncated response)))
            (is (= 2 (:project_count response)))
            (is (zero? (counted tree "beta"))
                "600 characters cannot reach the second project's files")
            (is (= "── beta (50 files; 0 shown)" (header tree "beta"))
                "a project counted by project_count must appear in the body")
            (is (= (str "── alpha (50 files; " (counted tree "alpha") " shown)")
                   (header tree "alpha")))
            (is (<= (count tree) 600))))))))

;; @spec MCP-OP-STUDY-017
(deftest a-truncated-tree-text-total-agrees-with-its-own-receipt
  ;; The text body's `── total: N files` counted only the files it SHOWED, so
  ;; a receipt reading `file_count 1072, returned 3` ended its own body with
  ;; `total: 3 files`. The two numbers in one payload disagreed.
  (let [response (run {"mode" "ls-tree" "dir" "src" "format" "text"
                       "limit" 2000})]
    (is (true? (:truncated response)))
    (is (< (:returned response) (:file_count response)))
    (is (str/includes? (:tree response)
                       (str "total: " (:file_count response)))
        "the body's total must be the receipt's true file_count")
    (is (str/includes? (:tree response)
                       (str (:omitted response) " omitted"))))
  (testing "a complete tree keeps the byte-identical complete total line"
    (let [response (run {"mode" "ls-tree" "dir" fixture-dir "format" "text"
                         "limit" 16384})]
      (is (false? (:truncated response)))
      (is (str/includes? (:tree response) "total: 7 files, ")))))

;; ============================================================
;; Refusals
;; ============================================================

;; @spec MCP-OP-STUDY-010
(deftest a-missing-owner-refuses-with-a-factual-vocabulary
  (doseq [operation ["deps" "ls-deps" "ls-extract"]]
    (testing operation
      (let [response (one operation {"form" "no-such-form"})]
        (is (false? (:ok response)))
        (is (= "study-form-not-found" (:error_type response)))
        (is (= "no-such-form" (:form response)))
        (is (false? (:read_complete response)))
        (is (true? (:source_unchanged response)))
        (is (seq (:available_owners response)))
        (is (pos? (:available_owner_count response)))
        (is (= "REPLACE-WITH-ONE-EXACT-OWNER"
               (get-in response [:next_call :arguments :requests 0 :form])))))))

;; @spec MCP-OP-STUDY-010
(deftest an-unknown-study-operation-names-the-complete-vocabulary
  (let [response (one "ls-mv" {})]
    (is (false? (:ok response)))
    (is (= "unknown-operation" (:reason response)))
    (is (= ["deps" "forms" "ls-deps" "ls-extract" "match" "outline" "topo"
            "xray"]
           (:supported response)))))

;; ============================================================
;; The parallel strategy
;; ============================================================

;; @spec MCP-OP-STUDY-028
(deftest an-mcp-ls-tree-outlines-through-the-bounded-parallel-strategy
  ;; Nothing pinned the strategy: `clj-surgeon.parallel/bounded-map` could be
  ;; swapped for serial `map` in `ls-tree-bounded` and every test still
  ;; passed, silently giving back the measured 10x an ls-tree over a real tree
  ;; gains. The kernel deliberately defaults to serial `map` so it keeps
  ;; loading under babashka, so the JVM entrance passing the pool is the ONLY
  ;; thing that makes the parallel path run — and it went unwitnessed.
  (let [calls (atom 0)
        real-bounded-map parallel/bounded-map]
    (with-redefs [parallel/bounded-map (fn [f coll]
                                         (swap! calls inc)
                                         (real-bounded-map f coll))]
      (let [response (run {"mode" "ls-tree" "dir" fixture-dir
                           "format" "text" "limit" 16384})]
        (is (true? (:ok response)))
        (is (pos? (:returned response)))
        (is (pos? @calls)
            "an MCP ls-tree that outlines through serial map fails here")))))

;; @spec MCP-OP-STUDY-028
(deftest the-strategy-changes-the-order-of-work-and-nothing-else
  ;; `upmap` yields in COMPLETION order. `outline-take` re-keys by file, so a
  ;; partial receipt must be identical whichever strategy produced it — this
  ;; is what makes the strategy safe to pin rather than merely fast.
  (let [scan (study/ls-tree {:dir fixture-dir :max-files 2000})
        projects (:projects scan)
        total (study/total-file-count projects)]
    (is (true? (:ok scan)))
    (doseq [n (range 1 total)]
      (testing (str "n = " n " of " total)
        (is (= (study/outline-take projects n (atom {}) map)
               (study/outline-take projects n (atom {})
                                   parallel/bounded-map)))))))

;; ============================================================
;; One kernel
;; ============================================================

;; @spec MCP-OP-STUDY-008
(deftest study-ops-both-entrances-call-one-kernel
  (let [source (slurp real-file)
        big {"limit" 16384}]
    (testing "deps"
      (let [kernel (study/deps source {})]
        (is (= (inspect/json-data kernel)
               (:deps (result-of (one "deps" big)))))
        (is (= kernel (core/run-deps {:file real-file})))))

    (testing "deps for one owner"
      (let [kernel (study/deps source {:form "dep-tree"})]
        (is (= (inspect/json-data kernel)
               (:deps (result-of (one "deps" (assoc big "form" "dep-tree"))))))
        (is (= kernel (core/run-deps {:file real-file :form "dep-tree"})))))

    (testing "topo"
      (let [kernel (study/topo source)]
        (is (= (inspect/json-data kernel)
               (:topo (result-of (one "topo" big)))))
        (is (= kernel (core/run-topo {:file real-file})))))

    (testing "ls-deps"
      (let [kernel (study/ls-deps source {:form "extraction-closure"})]
        (is (= (inspect/json-data kernel)
               (:dep_tree
                 (result-of
                   (one "ls-deps" (assoc big "form" "extraction-closure"))))))
        (is (= kernel
               (core/run-ls-deps {:file real-file
                                  :form "extraction-closure"})))))

    (testing "ls-extract"
      (let [kernel (study/ls-extract source {:form "extraction-closure"})]
        (is (= (inspect/json-data kernel)
               (:closure
                 (result-of
                   (one "ls-extract"
                        (assoc big "form" "extraction-closure"))))))
        (is (= kernel
               (core/run-closure {:file real-file
                                  :form "extraction-closure"})))))

    (testing "ls-tree renders through the one formatter"
      (let [scan (study/ls-tree {:dir fixture-dir})
            outlined (study/outline-all (:projects scan))
            response (run {"mode" "ls-tree" "dir" fixture-dir "format" "text"
                           "limit" 16384})]
        (is (true? (:ok scan)))
        (is (= (study/format-ls-tree-text outlined (:dir scan))
               (:tree response)))
        (is (= (inspect/json-data
                 (study/format-ls-tree-edn outlined (:dir scan)))
               (:files (run {"mode" "ls-tree" "dir" fixture-dir
                             "format" "edn" "limit" 16384}))))
        (is (= (inspect/json-data
                 (study/format-ls-tree-names outlined (:dir scan)))
               (:files (run {"mode" "ls-tree" "dir" fixture-dir
                             "limit" 16384})))
            "names is the default rendering with no grep, and is the same one formatter")))

    (testing "the CLI ls-tree and a max-limit MCP receipt are one tree"
      ;; Bounding is MCP-only, so the parity witness compared projections at
      ;; one limit on non-truncating inputs and never compared the CLI's whole
      ;; rendering against a real receipt. The two entrances must agree on the
      ;; tree string AND on how many files it covers.
      (let [cli-text (core/run-ls-tree {:dir fixture-dir})
            discovered (study/ls-tree {:dir fixture-dir})
            response (run {"mode" "ls-tree" "dir" fixture-dir "format" "text"
                           "limit" 16384})]
        (is (string? cli-text))
        (is (false? (:truncated response))
            "at the maximum limit this fixture must not truncate, or the
             comparison would be between a whole tree and a partial one")
        (is (= cli-text (:tree response))
            "the same tree string from both entrances")
        (is (= (:file-count discovered) (:file_count response)))
        (is (= (:file_count response) (:returned response)))
        (is (str/includes? cli-text
                           (str "total: " (:file_count response) " files")))))))

;; ============================================================
;; No write authority
;; ============================================================

;; @spec MCP-OP-STUDY-009
(deftest the-read-entrance-exposes-no-write-operation
  (let [inspect-tool-def (first (filter #(= "inspect_clojure" (:name %))
                                        (mcp-tool/all-tools)))
        request-variants (get-in inspect-tool/inspect-schema
                                 [:properties "requests" :items :oneOf])
        operations (set (keep #(get-in % [:properties "operation" :const])
                              request-variants))
        modes (set (get-in inspect-tool/inspect-schema
                           [:properties "mode" :enum]))]
    (is (true? (get-in inspect-tool-def [:annotations :read-only])))
    (is (false? (get-in inspect-tool-def [:annotations :destructive])))
    (is (= #{"forms" "outline" "match" "xray"
             "deps" "topo" "ls-deps" "ls-extract"}
           operations))
    (is (= #{"prepare-change" "plan-extraction" "ls-tree"} modes))
    (doseq [write-op ["mv" "mv-with-deps" "rename-ns" "rename-ns!"
                      "fix-declares" "fix-declares!" "extract!"
                      "replace-subform!"]]
      (is (not (contains? operations write-op)))
      (is (not (contains? modes write-op))))))

;; @spec MCP-OP-STUDY-001
(deftest the-public-tool-catalog-did-not-grow
  (is (= #{"inspect_clojure" "apply_clojure_changes" "edit_clojure"
           "transform_clojure"}
         (set (map :name (mcp-tool/all-tools))))))

;; ============================================================
;; The ns_grep match budget
;; ============================================================

;; @spec MCP-OP-STUDY-031
(deftest a-catastrophic-ns-grep-pattern-refuses-inside-a-step-budget
  ;; `ns_grep` was compiled ONCE under a guard (MCP-OP-STUDY-022) and then
  ;; MATCHED without one. `java.util.regex` is a backtracking engine, so a
  ;; caller-supplied pattern is caller-supplied CPU: `(.*.*.*.*.*.*)*x` over
  ;; the 67 files of `src/` — ordinary 36-character repository paths, no
  ;; adversarial file names needed — burned 43,589 ms through the real
  ;; entrance and still returned `ok=true`. At the default `max_files` 2000
  ;; the same one call is ~21 minutes; at the 20,000 ceiling, ~3.5 hours.
  ;; Nothing on that path could stop it: `future-cancel` cannot interrupt a
  ;; running matcher, so the bound has to live INSIDE the match.
  (doseq [pattern ["(.*.*.*.*)*x" "(.*.*.*.*.*.*)*x"]]
    (testing pattern
      (let [started (System/nanoTime)
            [response outlined] (counting-outlines
                                  #(run {"mode" "ls-tree" "dir" "src"
                                         "ns_grep" pattern}))
            elapsed-ms (/ (- (System/nanoTime) started) 1e6)]
        (is (false? (:ok response)))
        (is (= "ns-grep-match-budget-exceeded" (:error_type response)))
        (is (< elapsed-ms 200)
            (str "a caller-supplied pattern must not be able to spend "
                 "unbounded CPU in the read entrance; took " elapsed-ms " ms"))
        (is (zero? outlined)
            "and no file is opened before the pattern is refused")
        (is (false? (:read_complete response)))
        (is (true? (:source_unchanged response)))
        (is (nil? (:tree response)))
        (is (nil? (:files response)))
        (is (str/includes? (str (:error response)) pattern)
            "the refusal names the pattern it rejected")
        (is (pos-int? (:match_budget response)))
        (is (str/includes? (str (:error response))
                           (str (:match_budget response)))
            "and the budget it exceeded")
        (is (string? (:remedy response)))
        (is (= {:mode "ls-tree" :dir "src"}
               (get-in response [:next_call :arguments]))
            "the continuation drops the pattern and keeps the caller's scope")))))

;; @spec MCP-OP-STUDY-031
;; @spec MCP-OP-STUDY-012
(deftest the-match-budget-does-not-change-which-files-ns-grep-selects
  ;; A guard that changes the answer is a new defect. The oracle here is
  ;; hand-written — plain `re-find` over the same scan-relative paths, with
  ;; the same '_'/'-' equivalence MCP-OP-STUDY-012 defines — so it cannot
  ;; agree with the kernel by construction.
  (let [scan (study/ls-tree {:dir "src" :max-files 2000})
        root (:dir scan)
        rel (fn [f] (str (fs/relativize (fs/path root) (fs/path f))))
        oracle (fn [pattern]
                 (let [re (re-pattern pattern)]
                   (set (for [project (:projects scan)
                              file (:files project)
                              :let [path (rel file)]
                              :when (or (re-find re path)
                                        (re-find re (str/replace path "_" "-")))]
                          path))))]
    (is (true? (:ok scan)))
    (doseq [pattern ["mcp" "study" "^clj_surgeon/mcp.*tool"
                     "(?i)inspect|study|paths|outline"
                     "clj.surgeon.(mcp|study|core|analyze|outline|paths)"]]
      (testing pattern
        (let [filtered (study/filter-projects-by-ns-grep
                         (:projects scan) root pattern)]
          (is (seq (oracle pattern)) "the oracle must select something")
          (is (= (oracle pattern)
                 (set (map rel (mapcat :files filtered))))
              "the budgeted matcher selects exactly what an unguarded one does"))))))

;; @spec MCP-OP-STUDY-031
(deftest the-ns-grep-budget-refuses-at-the-step-after-the-budget
  ;; A ceiling is only a ceiling if it is asserted AT the ceiling. Exactly
  ;; `budget` character reads must be answered and the very next one must
  ;; refuse: an `>=` where a `>` belongs leaves every other witness here green.
  (let [subject "clj_surgeon/mcp_inspect_tool.clj"
        headroom 10000000]
    (doseq [[pattern expected-hit] [["zzz" false]
                                    ["mcp" true]
                                    ["(?i)inspect|study" true]]]
      (testing pattern
        (let [re (study/compile-pattern pattern)
              measured (study/ns-grep-pool headroom)
              hit (study/ns-grep-hit? re subject measured)
              used (- headroom (aget ^longs measured 0))]
          (is (= expected-hit hit))
          (is (< 0 used 1000)
              (str "an honest pattern over a 31-character path costs tens of "
                   "character reads, not thousands: " used))
          (is (= expected-hit
                 (study/ns-grep-hit? re subject (study/ns-grep-pool used)))
              "a match costing exactly the budget is answered, not refused")
          (let [thrown (try
                         (study/ns-grep-hit?
                           re subject (study/ns-grep-pool (dec used)))
                         (catch clojure.lang.ExceptionInfo error error))]
            (is (instance? clojure.lang.ExceptionInfo thrown)
                "one character read past the budget refuses")
            (is (= :ns-grep-match-budget-exceeded
                   (:error-type (ex-data thrown))))
            (is (= (dec used) (:budget (ex-data thrown)))
                "and the refusal carries the budget it exhausted")))))))

;; @spec MCP-OP-STUDY-031
(deftest the-ns-grep-budget-is-pooled-across-the-scan-not-spent-per-match
  ;; A PER-MATCH budget still lets a pattern that costs just under it be paid
  ;; once per file, which is `max_files` multiplied by the budget rather than
  ;; bounded by it. The pass therefore shares one allowance, sized by the tree
  ;; discovery actually found.
  (let [scan (study/ls-tree {:dir "src" :max-files 2000})
        discovered (study/total-file-count (:projects scan))
        response (run {"mode" "ls-tree" "dir" "src"
                       "ns_grep" "(.*.*.*.*.*.*)*x"})]
    (is (pos? discovered))
    (is (= "ns-grep-match-budget-exceeded" (:error_type response)))
    (is (= (study/ns-grep-scan-budget (:projects scan) (:dir scan))
           (:match_budget response))
        "the whole pass gets one allowance of steps-per-file x files found")
    (is (empty? (output-schema-violations response))
        "and the new receipt field does not break the tool's output schema")))

;; @spec MCP-OP-STUDY-032
;; @spec MCP-OP-STUDY-024
(deftest the-empty-text-receipt-still-names-every-project-it-counted
  ;; MCP-OP-STUDY-024 made a project the byte budget never reached read
  ;; `0 shown` instead of vanishing — but only for `returned >= 1`. The
  ;; `returned = 0` receipt was rendered from an EMPTY project vector, so the
  ;; body was the total line alone while `project_count` still said 2: the
  ;; same body-contradicts-its-own-receipt defect, surviving one file below
  ;; the fix. It also left MCP-OP-STUDY-024 and MCP-OP-STUDY-030 in direct
  ;; contradiction; STUDY-024 wins and STUDY-030's floor clause is superseded.
  (with-scratch-project
    "test-fixtures/study/scratch-empty-receipt"
    (fn [dir]
      (doseq [project ["alpha" "beta"]]
        (fs/create-dirs (str dir "/" project))
        (spit (str dir "/" project "/deps.edn") "{:paths [\"src\"]}")
        (dotimes [i 50]
          (write-clj-file! (str dir "/" project "/src/" project "/ns" i ".clj")
                           (format "(ns %s.ns%d)" project i)
                           (format "(defn f%d [] :ok)" i)))))
    (fn []
      (doseq [limit [100 1]]
        (testing (str "limit " limit)
          (let [response (run {"mode" "ls-tree"
                               "dir" "test-fixtures/study/scratch-empty-receipt"
                               "format" "text" "limit" limit})
                lines (vec (remove str/blank?
                                   (str/split-lines (str (:tree response)))))]
            (is (true? (:ok response)))
            (is (zero? (:returned response))
                "this limit must reach no file at all")
            (is (= 2 (:project_count response)))
            (is (= ["── alpha (50 files; 0 shown)"
                    "── beta (50 files; 0 shown)"
                    "── total: 100 files; 0 shown, 100 omitted"]
                   lines)
                "a project counted by project_count appears in the body at every limit")))))))

;; @spec MCP-OP-STUDY-033
;; @spec MCP-OP-STUDY-021
(deftest the-cap-stops-the-walk-inside-one-oversized-project
  ;; MCP-OP-STUDY-021 said `max_files` is applied DURING accumulation and
  ;; stops the walk. It stopped the walk BETWEEN candidates: a whole candidate
  ;; project was walked and canonicalised before its count was compared to the
  ;; cap. One project of 3000 files at `max_files 10` therefore ran the full
  ;; `find` and called `real-path-within` — one `toRealPath` syscall each —
  ;; 3001 times to refuse at a cap of 10, and then reported `3000` as an exact
  ;; total. The cap has to stop the accumulation itself, and a count taken at
  ;; the cap is a FLOOR, which the receipt must say out loud.
  (with-scratch-project
    "test-fixtures/study/scratch-cap-inside"
    (fn [dir] (write-scratch-project! dir 3000))
    (fn []
      (let [calls (atom 0)
            real-path-within mcp-paths/real-path-within]
        (with-redefs [mcp-paths/real-path-within
                      (fn [& args]
                        (swap! calls inc)
                        (apply real-path-within args))]
          (let [response (run {"mode" "ls-tree"
                               "dir" "test-fixtures/study/scratch-cap-inside"
                               "max_files" 10})]
            (is (false? (:ok response)))
            (is (= "study-tree-too-large" (:error_type response)))
            (is (= 10 (:max_files response)))
            (is (<= @calls 12)
                (str "a cap of 10 must not canonicalise 3000 files: "
                     @calls " calls. The bound is cap+1 walk entries plus one "
                     "per build file discovery confined (here, the project's "
                     "own deps.edn)"))
            (is (= 11 (:file_count response))
                "the walk stops one file past the cap, which is what proves it")
            (is (true? (:observed_at_least response))
                "a count taken at the cap is a floor, and the receipt says so")
            (is (str/includes? (:error response) "at least")
                "and the message says so too")
            (is (str/includes? (:error response) "11"))
            (is (empty? (output-schema-violations response)))))))))

(defn- verbatim-args-characters
  "Hand-written oracle: every `:args` string anywhere in a receipt. `outline`
   emits each form's arglist VERBATIM from the file, so these characters are
   file source that crossed the wire, whatever key they arrived under."
  [result]
  (let [total (atom 0)]
    (walk/postwalk
      (fn [node]
        (when (and (map-entry? node)
                   (contains? #{:args "args"} (key node))
                   (string? (val node)))
          (swap! total + (count (val node))))
        node)
      result)
    @total))

;; @spec MCP-OP-STUDY-034
;; @spec MCP-OP-STUDY-020
(deftest an-outline-charges-the-verbatim-arglists-it-returns
  ;; MCP-OP-STUDY-020 moved the source budget onto the source a result
  ;; RETURNS, and `returned-source-character-count` counts `:source` — the
  ;; only key it knew about. `outline` never emits `:source`; it emits each
  ;; form's `:args`, lifted verbatim out of the file. On
  ;; `src/clj_surgeon/intent_transaction.clj` that is 2,696 characters of file
  ;; text crossing the wire while the source budget scored the request 0. The
  ;; mechanism was right and its coverage was a naming convention.
  (let [file "src/clj_surgeon/intent_transaction.clj"
        result (result-of (one "outline" {"file" file}))
        args (verbatim-args-characters result)]
    (is (< 2000 args) (str "the outline returns verbatim arglists: " args))
    (is (= args (inspect/returned-source-character-count result))
        "and every one of those characters is charged as returned source")
    (testing "the charge is a bound, asserted at the bound"
      (let [wide {:per-request-result 1000000 :aggregate-result 1000000}]
        (is (true? (:ok (inspect/enforce-output-budget
                          [result] (assoc wide :per-request-source args))))
            "a result whose returned source costs exactly the budget passes")
        (let [refusal (inspect/enforce-output-budget
                        [result] (assoc wide :per-request-source (dec args)))]
          (is (false? (:ok refusal)) "one character more refuses")
          (is (= "inspect-output-limit" (:error_type refusal)))
          (is (= "request_source" (:scope refusal))))))))

;; @spec MCP-OP-STUDY-034
(deftest arglists-alone-can-exhaust-the-source-budget-and-refuse-typed
  ;; The production shape of the same hole: a file whose ARGLISTS alone exceed
  ;; the per-request source budget must refuse typed rather than be returned,
  ;; and must refuse as a SOURCE overrun — which is what names the remedy
  ;; correctly for a caller who cannot make an outline any smaller.
  (with-scratch-project
    "test-fixtures/study/scratch-wide-args"
    (fn [dir]
      (fs/create-dirs dir)
      (spit (str dir "/wide.clj")
            (str "(ns wide)\n"
                 (str/join
                   "\n"
                   (for [i (range 700)]
                     (format "(defn f%d [%s] :ok)"
                             i
                             (str/join " " (map #(str "arg" i "-" %)
                                                (range 12)))))))))
    (fn []
      (let [file "test-fixtures/study/scratch-wide-args/wide.clj"
            response (one "outline" {"file" file})]
        (is (false? (:ok response)))
        (is (= "inspect-output-limit" (:error_type response)))
        (is (= "request_source" (:scope response))
            "arglists are returned SOURCE, so this is a source overrun")
        (is (> (:actual response) 65536))
        (is (= 65536 (:limit response)))
        (is (false? (:read_complete response)))
        (is (true? (:source_unchanged response)))
        (is (nil? (:results response))
            "no partial result is returned; a truncated outline is not an option")))))

;; @spec MCP-OP-STUDY-035
;; @spec MCP-OP-STUDY-014
(deftest a-source-path-through-a-symlink-is-a-named-skip-not-a-silent-miss
  ;; `walk-clj-files` runs `find` with the default `-P`, which is what keeps a
  ;; symlinked project root from being descended (MCP-OP-STUDY-014) — and what
  ;; makes a `:paths` entry that IS a symlink yield nothing at all. The scan
  ;; then reported `no-clojure-files` and offered "scan a directory that
  ;; contains Clojure sources", which is exactly wrong: the directory does
  ;; contain them. A confinement decision the caller cannot see is a silent
  ;; false negative, so it is named in the receipt with the reason.
  (testing "a project whose only declared path is a symlink"
    (with-scratch-project
      "test-fixtures/study/scratch-symlink-only"
      (fn [dir]
        (fs/create-dirs (str dir "/realsrc"))
        (spit (str dir "/deps.edn") "{:paths [\"srclink\"]}")
        (write-clj-file! (str dir "/realsrc/z.clj") "(ns z)" "(defn z [] :ok)")
        (fs/create-sym-link (str dir "/srclink") "realsrc"))
      (fn []
        (let [response (run {"mode" "ls-tree"
                             "dir" "test-fixtures/study/scratch-symlink-only"})]
          (is (false? (:ok response)))
          (is (= "no-clojure-files" (:error_type response)))
          (is (= [{:project "scratch-symlink-only"
                   :path "srclink"
                   :reason "symlink"}]
                 (:paths_unresolved response))
              "the skipped declaration is named, with its reason")
          (is (str/includes? (:remedy response) "srclink")
              "and the remedy names it too, so it is executable")
          (is (empty? (output-schema-violations response)))))))
  (testing "a project that also has a real source directory still scans"
    (with-scratch-project
      "test-fixtures/study/scratch-symlink-mixed"
      (fn [dir]
        (fs/create-dirs (str dir "/realsrc"))
        (spit (str dir "/deps.edn") "{:paths [\"src\" \"srclink\"]}")
        (write-clj-file! (str dir "/src/a.clj") "(ns a)" "(defn a [] :ok)")
        (write-clj-file! (str dir "/realsrc/z.clj") "(ns z)" "(defn z [] :ok)")
        (fs/create-sym-link (str dir "/srclink") "realsrc"))
      (fn []
        (let [response (run {"mode" "ls-tree"
                             "dir" "test-fixtures/study/scratch-symlink-mixed"
                             "format" "edn" "limit" 16384})]
          (is (true? (:ok response)))
          (is (= 1 (:file_count response))
              "the symlinked path contributes nothing, as it always did")
          (is (= ["src/a.clj"] (mapv :file (:files response))))
          (is (= [{:project "scratch-symlink-mixed"
                   :path "srclink"
                   :reason "symlink"}]
                 (:paths_unresolved response))
              "but a successful receipt still says what it could not reach")
          (is (empty? (output-schema-violations response))))))))

;; ============================================================
;; The paths_unresolved item shape is published, not just its container
;; (round-4 re-review fix 6)
;; ============================================================

;; @spec MCP-OP-STUDY-035
(deftest paths-unresolved-item-schema-is-published-and-constrains-reason
  ;; `"paths_unresolved" {:type "array"}` constrained only the container: a
  ;; caller had no machine-readable contract for the `{project path reason}`
  ;; shape every entry actually carries, published only in prose and in
  ;; STUDY-035. The item schema must be declared, and it must reject an
  ;; unknown `reason`, a missing required field, and an undeclared key.
  (let [item-schema (get-in inspect-tool/inspect-output-schema
                            [:properties "paths_unresolved" :items])
        valid-item {"project" "p" "path" "src" "reason" "symlink"}]
    (is (some? item-schema)
        "the array must publish an entry shape, not just \"is an array\"")
    (is (empty? (object-schema-violations item-schema valid-item))
        "a real production item validates cleanly")
    (is (seq (object-schema-violations
              item-schema (assoc valid-item "reason" "no-such-reason")))
        "an unknown reason is rejected by the published enum")
    (is (seq (object-schema-violations item-schema (dissoc valid-item "path")))
        "a missing required field is rejected")
    (is (seq (object-schema-violations item-schema (assoc valid-item "extra" "x")))
        "an undeclared key is rejected (additionalProperties false)")))

;; ============================================================
;; One call must be sufficient IN THE TEXT BLOCK (O2 / PF-5)
;; ============================================================
;; Field evidence, E6-Lb PF-4 (`/home/forge/tmp/arms/e6/pf4/c2.json`): an
;; `ls-tree` receipt's `content[0].text` was 146 characters — a header, a
;; truncation notice, and an arrow — while the table of contents lived only in
;; `structuredContent.tree`. A text-only client (codex) is handed nothing. The
;; witnesses below assert what a text-only caller actually receives.

(defn- with-tmp-project
  "Build a throwaway project under /tmp and run `thunk` with a config rooted
   at it. `/tmp` keeps a scratch tree out of a shared repository checkout."
  [build! thunk]
  (let [dir (str (fs/create-temp-dir {:prefix "o2-fx-"}))]
    (try
      (build! dir)
      (thunk {:project-root dir})
      (finally
        (fs/delete-tree dir)))))

(def ^:private toy-namespaces
  ["alpha" "beta" "gamma" "delta" "epsilon"
   "zeta" "eta" "theta" "iota" "kappa"])

(defn- write-toy-namespace!
  "One realistically shaped source file: an ns with two requires and three
   top-level forms, each carrying an arglist and a multi-line span."
  [dir index name]
  (write-clj-file!
    (str dir "/src/toy/" name ".clj")
    (format "(ns toy.%s\n  (:require [clojure.string :as str]\n            [clojure.set :as set]))"
            name)
    ""
    (format "(def ^:private tick-%s (System/currentTimeMillis))" name)
    ""
    (format "(defn start-%s\n  [opts]\n  (let [t (System/currentTimeMillis)]\n    (str/join \",\" [(:id opts) t %d])))"
            name index)
    ""
    (format "(defn stop-%s\n  [state reason]\n  (set/union #{reason} (:tags state)))" name)))

(defn- build-toy-project!
  [dir n]
  (spit (str dir "/deps.edn") "{:paths [\"src\"]}")
  (dotimes [i n]
    (let [base (nth toy-namespaces (mod i (count toy-namespaces)))
          name (if (< i (count toy-namespaces)) base (str base i))]
      (write-toy-namespace! dir i name))))

(defn- text-block
  "The exact string a text-only MCP client renders for an ls-tree result."
  [result]
  (inspect-tool/ls-tree-summary (assoc result :elapsed_ms 1.0)))

(defn- form-row-lines
  "Every rendered outline row — `  <line>[-<end>]: <type> <name>`. These are
   the hit line numbers a caller reads to know where to edit."
  [text]
  (filterv #(re-matches #"\s+\d+(-\d+)?: \S+ \S+.*" %) (str/split-lines text)))

(defn- files-named
  [text]
  (set (re-seq #"toy/\w+\.clj" text)))

;; @spec MCP-OP-STUDY-036
(deftest ls-tree-text-block-carries-the-rows-not-just-a-header
  ;; RED before the fix: `content[0].text` is a 113-character header with zero
  ;; namespaces and zero rows, while `structuredContent.tree` holds 2,064
  ;; characters of table of contents.
  (with-tmp-project
    #(build-toy-project! % 10)
    (fn [config]
      (let [result (inspect-tool/execute-ls-tree
                     config {:mode "ls-tree" :dir "." :format "text"})
            text (text-block result)]
        (is (true? (:ok result)))
        (is (true? (:read_complete result)))
        (is (= 10 (:file_count result)))
        (testing "every namespace the tree carries is in the text a client sees"
          (doseq [name toy-namespaces]
            (is (str/includes? text (str "ns: toy." name))
                (format "text block must name toy.%s" name))))
        (testing "the per-form rows are in the text block"
          (is (<= 22 (count (form-row-lines text)))
              (format "expected >= 22 form rows in content[0].text, got %d"
                      (count (form-row-lines text)))))
        (testing "the requires summary travels with them"
          (is (str/includes? text "requires: [clojure.string :as str]")))
        (testing "and the whole text block stays inside the O2 budget"
          (is (<= (count text) 8192)
              (format "text block was %d characters" (count text))))))))

;; @spec MCP-OP-STUDY-037
(deftest ls-tree-default-limit-completes-a-ten-file-tree-in-one-call
  ;; O2's pass line, on a toy tree: one call, at whatever limit the caller gets
  ;; without asking, must return >= 22 hit line numbers across >= 10 files with
  ;; read_complete=true in <= 8 KB of TEXT.
  (with-tmp-project
    #(build-toy-project! % 10)
    (fn [config]
      (let [result (inspect-tool/execute-ls-tree
                     config {:mode "ls-tree" :dir "."
                             :grep "System/currentTimeMillis"})
            text (text-block result)]
        (is (true? (:ok result)))
        (is (= "text" (:format result))
            "grep still selects the per-form view")
        (is (true? (:read_complete result))
            "a ten-file toy tree must be complete in one call")
        (is (= 10 (:returned result)))
        (is (<= 10 (count (files-named text)))
            "at least ten distinct files named in the text block")
        (is (<= 22 (count (form-row-lines text)))
            "at least twenty-two hit line numbers in the text block")
        (is (<= (count text) 8192)
            (format "text block was %d characters" (count text)))))))

;; @spec MCP-OP-STUDY-037
(deftest ls-tree-default-limit-is-raised-to-admit-a-real-table-of-contents
  ;; The measured defect: `format=text` at the old default of 4096 admitted
  ;; only 2 of the 10 files of a real `src`. The default must admit a tree the
  ;; old default truncated, and must stay inside the O2 text budget.
  (is (= 8192 inspect-tool/ls-tree-default-limit))
  (with-tmp-project
    #(build-toy-project! % 25)
    (fn [config]
      (let [at-old (inspect-tool/execute-ls-tree
                     config {:mode "ls-tree" :dir "." :format "text"
                             :limit 4096})
            at-default (inspect-tool/execute-ls-tree
                         config {:mode "ls-tree" :dir "." :format "text"})]
        (is (false? (:read_complete at-old))
            "the old default truncates this tree — the raise is load-bearing")
        (is (= 8192 (:limit at-default)))
        (is (true? (:read_complete at-default))
            "the raised default completes it in one call")
        (is (= 25 (:returned at-default)))
        (is (<= (count (text-block at-default)) 8192))))))

;; @spec MCP-OP-STUDY-036
;; @spec MCP-OP-STUDY-038
(deftest ls-tree-text-block-appends-the-continuation-hint-when-truncated
  ;; A truncated text block must say, IN THE TEXT, what to send next. The
  ;; ceiling stays a typed boundary: a tree that fits returns complete, and the
  ;; next file over returns truncated carrying the hint.
  (with-tmp-project
    #(build-toy-project! % 60)
    (fn [config]
      (let [truncated (inspect-tool/execute-ls-tree
                        config {:mode "ls-tree" :dir "." :format "text"
                                :limit 4096})
            text (text-block truncated)]
        (is (false? (:read_complete truncated)))
        (is (= "raise_limit_or_narrow_scope" (:next_action truncated)))
        (testing "the rows it DID fit are still rendered"
          (is (pos? (count (form-row-lines text)))))
        (testing "and the text names the exact continuation to send"
          (is (str/includes? text "next call: inspect_clojure")
              "the continuation names the tool")
          (is (str/includes? text "limit=16384")
              "the continuation names the raised limit")))))
  (with-tmp-project
    #(build-toy-project! % 90)
    (fn [config]
      (testing "at the ceiling the text carries the narrow-scope remedy instead"
        (let [at-ceiling (inspect-tool/execute-ls-tree
                           config {:mode "ls-tree" :dir "." :format "text"
                                   :limit 16384})
              text (text-block at-ceiling)]
          (is (false? (:read_complete at-ceiling)))
          (is (= "narrow_scope" (:next_action at-ceiling)))
          (is (str/includes? text "maximum limit")
              "the remedy is in the text, not only in structuredContent")
          (is (not (str/includes? text "next call:"))
              "no continuation is offered that just replays this call"))))))

;; @spec MCP-OP-STUDY-038
(deftest ls-tree-ceiling-is-a-boundary-n-fits-and-n-plus-one-does-not
  ;; The typed boundary, witnessed at the edge rather than asserted about a
  ;; constant: N toy files complete at the ceiling, N+1 truncate and say so.
  (let [n (atom nil)]
    (with-tmp-project
      #(build-toy-project! % 77)
      (fn [config]
        (let [result (inspect-tool/execute-ls-tree
                       config {:mode "ls-tree" :dir "." :format "text"
                               :limit 16384})]
          (reset! n (:file_count result))
          (is (true? (:read_complete result))
              "77 toy files (16,370 characters) fit under the 16384 ceiling")
          (is (= 77 (:returned result))))))
    (with-tmp-project
      #(build-toy-project! % 78)
      (fn [config]
        (let [result (inspect-tool/execute-ls-tree
                       config {:mode "ls-tree" :dir "." :format "text"
                               :limit 16384})]
          (is (false? (:read_complete result))
              "the seventy-eighth does not")
          (is (= 76 (:returned result)))
          (is (= "narrow_scope" (:next_action result)))
          (is (str/includes? (text-block result) "maximum limit")))))
    (is (= 77 @n))))

;; @spec MCP-OP-STUDY-039
(deftest ls-tree-entrance-announces-the-workspace-it-serves
  ;; The unit witnesses prove the recorder. This proves the ENTRANCE calls it:
  ;; without a real call emitting the event, a cohort still cannot bind an arm
  ;; to a connection, which is the whole point.
  (let [telemetry-dir (str (fs/create-temp-dir {:prefix "o2-tel-"}))]
    (try
      (with-tmp-project
        #(build-toy-project! % 3)
        (fn [config]
          (let [state (telemetry/start! {:mode :metrics :directory telemetry-dir
                                         :run-id "o2-witness"})
                config (assoc config :telemetry state)]
            (inspect-tool/execute-inspect! config {"mode" "ls-tree" "dir" "."})
            (inspect-tool/execute-inspect! config {"mode" "ls-tree" "dir" "."})
            (let [lines (remove str/blank? (str/split-lines (slurp (:file state))))
                  parsed (mapv #(json/parse-string % true) lines)
                  started (filterv #(= "session.start" (:event %)) parsed)
                  calls (filterv #(= "tool.call" (:event %)) parsed)]
              (is (= 2 (count calls)) "both calls were recorded")
              (is (= 1 (count started))
                  "the workspace is announced once, not once per call")
              (is (= (telemetry/workspace-key (:project-root config))
                     (:workspace_key (first started)))
                  "and it names the root a cohort can recompute")
              (is (= "o2-witness" (:run_id (first started))))))))
      (finally
        (fs/delete-tree telemetry-dir)))))

;; ============================================================
;; The public MCP result is TEXT AND STRUCTURE TOGETHER (O2 round 2)
;; ============================================================
;; Field evidence (O2 re-review, 2026-09-03): rendering the ls-tree payload
;; into the text block roughly DOUBLES the result on the wire, and
;; `ls-tree dir=src grep=defn limit=16384` over `/home/forge/tmp/arms/e6/pf3`
;; measured 34,042 bytes of complete public result against the 32,768-byte
;; budget the tool declares and enforces for three other modes. `ls-tree` fell
;; to `:else raw-result`, so nothing refused, trimmed, or warned. The fixture
;; below reproduces that overshoot inside the repository so the bound is
;; witnessed by CI rather than by one machine's scratch tree.

(defn- public-bytes
  [result]
  (inspect-tool/mcp-result-byte-count
    (inspect-tool/inspect-summary (assoc result :elapsed_ms 0.0))
    result))

;; @spec MCP-OP-STUDY-040
(deftest ls-tree-public-result-is-bounded-by-the-declared-output-budget
  (with-tmp-project
    #(build-toy-project! % 78)
    (fn [config]
      (let [raw (inspect-tool/execute-ls-tree
                  config {:mode "ls-tree" :dir "." :format "text"
                          :limit 16384})
            over (public-bytes raw)]
        (is (< inspect-tool/max-public-result-bytes over)
            (format (str "the fixture must actually overshoot or the witness "
                         "proves nothing; measured %d bytes against %d")
                    over inspect-tool/max-public-result-bytes))
        (let [fitted (inspect-tool/enforce-result-budget raw raw)
              text (inspect-tool/inspect-summary (assoc fitted :elapsed_ms 0.0))]
          (is (<= (public-bytes fitted) inspect-tool/max-public-result-bytes)
              (format "the enforced result was %d bytes" (public-bytes fitted)))
          (testing "the overshoot is typed, not silent"
            (is (or (false? (:ok fitted))
                    (pos-int? (:text_evidence_limit fitted)))
                "an over-budget result is a typed truncation or a refusal"))
          (testing "and the text says it was abridged and what to do instead"
            (is (str/includes? text "text abridged"))
            (is (str/includes? text "structuredContent"))))))))

;; @spec MCP-OP-STUDY-040
(deftest a-fitting-result-is-returned-untouched
  (with-tmp-project
    #(build-toy-project! % 10)
    (fn [config]
      (let [raw (inspect-tool/execute-ls-tree
                  config {:mode "ls-tree" :dir "." :format "text"})]
        (is (>= inspect-tool/max-public-result-bytes (public-bytes raw)))
        (is (= raw (inspect-tool/enforce-result-budget raw raw))
            "a result inside the budget is never rewritten")))))

;; @spec MCP-OP-STUDY-040
(deftest a-names-payload-is-abridged-on-whole-entries
  ;; Half a JSON object is not evidence. The `names`/`edn` payload travels as
  ;; data, so an abridged rendering must still parse.
  (with-tmp-project
    #(build-toy-project! % 200)
    (fn [config]
      (let [raw (inspect-tool/execute-ls-tree
                  config {:mode "ls-tree" :dir "." :format "names"
                          :limit 16384})
            fitted (inspect-tool/enforce-result-budget raw raw)
            text (inspect-tool/inspect-summary (assoc fitted :elapsed_ms 0.0))
            payload (first (filter #(str/starts-with? % "[")
                                   (str/split-lines text)))]
        (is (<= (public-bytes fitted) inspect-tool/max-public-result-bytes))
        (when (:text_evidence_limit fitted)
          (is (str/includes? text "text abridged"))
          (is (sequential? (json/parse-string payload true))
              "the abridged names payload must still parse as JSON")
          (is (< (count (json/parse-string payload true)) (:returned fitted))
              "and it must carry fewer whole entries than the receipt"))))))

;; ============================================================
;; EVERY mode's text carries the rows, not a count (O2 round 2)
;; ============================================================
;; Reviewer findings 6, 7, 8 (2026-09-03): O2 fixed `ls-tree` and left the same
;; defect standing on eight other modes, all from one function. `deps` /
;; `topo` / `ls-deps` / `ls-extract` rendered `request-1: deps · 27 of 27 rows`
;; — a COUNT where the rows were — `outline` named only the first and last of
;; 28 forms, and `forms` rendered `131 source characters` instead of the source
;; a caller asked for. A truncated result carried `next_call` in
;; `structuredContent` and offered the text only `next action
;; raise_limit_or_narrow_scope`.

(defn- summary-of
  [response]
  (inspect-tool/inspect-summary (assoc response :elapsed_ms 1.0)))

(defn- evidence-rows
  "The rows a text block renders, in rendered order. Row lines are the only
   lines that carry the `· ` row marker at four-space indent, so a source body
   or a notice can never be mistaken for one."
  [text]
  (into []
        (keep #(second (re-matches #"^    · (.*)$" %)))
        (str/split-lines text)))

;; @spec MCP-OP-STUDY-041
(deftest deps-text-carries-every-row-the-receipt-carries
  (let [response (one "deps" {"limit" 16384})
        result (result-of response)
        text (summary-of response)]
    (is (= 27 (:returned result)))
    (is (= (count (:deps result)) (count (evidence-rows text)))
        (format "TEXT rendered %d rows for a receipt of %d"
                (count (evidence-rows text)) (count (:deps result))))
    (doseq [row (:deps result)]
      (is (str/includes? text (:name row))
          (format "TEXT must name %s" (:name row))))
    (testing "and a row's dependencies travel with it"
      (is (str/includes? text "forms-from-rcond")))))

;; @spec MCP-OP-STUDY-041
(deftest a-truncated-result-spells-its-continuation-in-the-text
  (let [response (one "deps" {"limit" 200})
        result (result-of response)
        text (summary-of response)]
    (is (true? (:truncated result)))
    (is (some? (:next_call result)) "the receipt carries a continuation")
    (is (str/includes? text "next call:")
        "and the text must spell it — a text-only client sees no next_call")
    (is (str/includes? text "\"limit\":16384")
        "including the argument that makes it advance")))

;; @spec MCP-OP-STUDY-041
(deftest topo-text-carries-the-order-not-its-size
  (let [response (one "topo" {"limit" 16384})
        result (result-of response)
        text (summary-of response)]
    (doseq [name (get-in result [:topo :sorted])]
      (is (str/includes? text name) (format "TEXT must name %s" name)))
    (doseq [name (get-in result [:topo :cycles])]
      (is (str/includes? text name) (format "TEXT must name cycle %s" name)))))

;; @spec MCP-OP-STUDY-041
(deftest ls-deps-and-ls-extract-text-carry-their-trees
  (let [tree-text (summary-of (one "ls-deps" {"form" "extraction-closure"
                                              "limit" 16384}))
        closure (one "ls-extract" {"form" "extraction-closure" "limit" 16384})
        closure-text (summary-of closure)]
    (is (str/includes? tree-text "intra-ns-deps")
        "the dependency tree's members, not a count of one row")
    (is (str/includes? tree-text "free-symbols-in-form"))
    (doseq [form (get-in (result-of closure) [:closure :forms])]
      (is (str/includes? closure-text (:name form))))))

;; @spec MCP-OP-STUDY-041
(deftest outline-text-carries-every-form-and-its-line-range
  (let [response (one "outline" {})
        forms (get-in (result-of response) [:outline :forms])
        text (summary-of response)]
    (is (= 28 (count forms)))
    (is (= (count forms) (count (evidence-rows text)))
        (format "TEXT rendered %d rows for an outline of %d forms"
                (count (evidence-rows text)) (count forms)))
    (doseq [form forms]
      (is (str/includes? text (:name form)))
      (is (str/includes? text (str (:line form)))
          (format "%s must carry its line range" (:name form))))))

;; @spec MCP-OP-STUDY-041
(deftest forms-text-carries-the-source-a-caller-asked-for
  (let [response (one "forms" {"forms" ["reader-cond?"] "expect" {"forms" 1}
                               "include_source" true})
        form (first (:forms (result-of response)))
        text (summary-of response)]
    (is (= 131 (count (:source form))))
    (is (str/includes? text "(defn- reader-cond? [zloc]")
        "include_source asks for the source; the text must carry it")
    (is (str/includes? text "reader-cond?@37-39"))))

;; ============================================================
;; match / xray may never drop evidence silently (O2 round 2)
;; ============================================================
;; Reviewer finding 4 (2026-09-03), measured at 26e4810 through
;; `mcp-operation/invoke!`: `match "(defn- _ _ _)"` on `analyze.clj` returned
;; `match_count=7`, seven matches and 3,529 characters of compact JSON in
;; `structuredContent`, and 206 characters of TEXT containing NO match source
;; at all — because `compact-json` returned nil above 1,024 characters and the
;; whole line vanished inside a `when-let`. The text still read
;; `✓ terminal evidence · read_complete=true · next action none`. A text-only
;; caller was told the answer was complete and handed none of it, with no flag
;; to contradict it. The pre-O2 `ls-tree` at least said `read_complete=false`.

;; @spec MCP-OP-STUDY-041
(deftest match-text-carries-every-match-body-above-the-old-1024-cap
  (let [response (one "match" {"match" "(defn- _ _ _)"})
        result (result-of response)
        text (summary-of response)]
    (is (= 7 (:match_count result)))
    (is (< 1024 (count (json/generate-string
                         (mapv #(select-keys % [:inside :source])
                               (:matches result)))))
        "the evidence must be above the cap that used to erase it")
    (is (= 7 (count (evidence-rows text))))
    (doseq [match (:matches result)]
      (is (str/includes? text (first (str/split-lines (:source match))))
          (format "TEXT must carry the body of %s" (:inside match))))))

;; @spec MCP-OP-STUDY-041
(deftest xray-text-carries-its-value
  (let [response (one "xray" {"expression"
                                 "(-> (form 'reader-cond?) (xray count))"})
        result (result-of response)
        text (summary-of response)]
    (is (contains? result :value))
    (is (str/includes? text (str "value " (:value result))))))

;; @spec MCP-OP-STUDY-041
;; @spec MCP-OP-STUDY-040
(deftest dropped-evidence-is-never-reported-as-terminal
  (let [response (one "match" {"match" "(defn- _ _ _)"})
        abridged (assoc response :text_evidence_limit 300)
        text (inspect-tool/inspect-summary (assoc abridged :elapsed_ms 1.0))]
    (is (< (count (evidence-rows text)) 7)
        "the allowance must actually bite, or the witness proves nothing")
    (is (not (str/includes?
               text "✓ terminal evidence · read_complete=true · next action none"))
        "a text block that dropped evidence may never call the read terminal")
    (is (str/includes? text "! text abridged · read_complete=true"))
    (is (str/includes? text "structuredContent.results[request-1]")
        "and it must name where the evidence it dropped can be read")
    (is (str/includes? text "narrow the request"))))

;; ============================================================
;; Every refusal's text names its cause, its evidence, and its remedy
;; ============================================================
;; Reviewer findings 5 and 10 (2026-09-03). `diagnostic?` required BOTH a
;; `failed_request` and a `:failures` entry; a study refusal has neither, so
;; the `available owners (n/n): …` line was skipped — while the sentence that
;; refers to that list was guarded only by `(seq available-owners)` and printed
;; anyway. Measured at 26e4810, `ls-deps form=no-such-form-xyz`:
;; `structuredContent` carried `:error` ("No top-level form named
;; no-such-form-xyz in src/clj_surgeon/analyze.clj") and 27 `available_owners`;
;; the TEXT carried NEITHER, and read:
;;
;;   inspect_clojure
;;     refused · study-form-not-found · 55.12 ms
;;     All listed owners are real snapshot evidence; ranking is
;;     non-authoritative. …
;;   → correct_request
;;
;; The ls-tree refusal branch (`mcp_inspect_tool.clj:857`) already printed the
;; cause AND the next action. That shape is the class fix.

;; @spec MCP-OP-STUDY-042
(deftest a-study-refusal-text-names-its-cause-and-its-owners
  (let [response (one "ls-deps" {"form" "no-such-form-xyz"})
        text (summary-of response)]
    (is (false? (:ok response)))
    (is (= "study-form-not-found" (:error_type response)))
    (is (= 27 (count (:available_owners response))))
    (testing "the cause travels in the text, not only in structuredContent"
      (is (str/includes? text (:error response))))
    (testing "and so do the owners the sentence refers to"
      (doseq [owner (:available_owners response)]
        (is (str/includes? text owner)
            (format "TEXT must list owner %s" owner))))
    (testing "and the next action"
      (is (str/includes? text (:next_action response))))))

;; @spec MCP-OP-STUDY-042
(deftest the-owners-vocabulary-sentence-prints-only-over-a-printed-list
  (let [vocabulary "All listed owners are real snapshot evidence"
        with-owners (summary-of (one "ls-deps" {"form" "no-such-form-xyz"}))
        without-owners (summary-of (run {"requests" [{"operation" "deps"}]
                                         "expect" {"requests" 1 "files" 1}}))]
    (is (str/includes? with-owners vocabulary))
    (is (not (str/includes? without-owners vocabulary))
        "a sentence about a list that is not printed is worse than silence")))

;; @spec MCP-OP-STUDY-042
(deftest every-refusal-text-names-error-type-cause-and-next-action
  (doseq [[label params]
          [[:missing-fields {"requests" [{"operation" "deps"}]
                             "expect" {"requests" 1 "files" 1}}]
           [:unknown-fields {"requests" [{"operation" "deps"
                                          "file" real-file
                                          "pattern" "x"}]
                             "expect" {"requests" 1 "files" 1}}]
           [:invalid-xray {"requests" [{"operation" "xray" "file" real-file
                                        "expression" "*"}]
                           "expect" {"requests" 1 "files" 1}}]
           [:study-form-not-found {"requests" [{"operation" "ls-extract"
                                                "file" real-file
                                                "form" "no-such-form-xyz"}]
                                   "expect" {"requests" 1 "files" 1}}]
           [:invalid-study-limit {"mode" "ls-tree" "dir" "." "limit" 99999}]
           [:dir-not-found {"mode" "ls-tree" "dir" "no-such-dir-xyz"}]]]
    (testing (name label)
      (let [response (run params)
            text (summary-of response)]
        (is (false? (:ok response)))
        (is (str/includes? text (or (:error_type response)
                                    (str (:reason response))))
            "the text names the error type")
        (is (str/includes? text (:error response))
            "the text names the cause, verbatim")
        (is (or (str/includes? text (str (:next_action response)))
                (str/includes? text "next call:"))
            "the text names the next action or spells the next call")
        (when (:remedy response)
          (is (str/includes? text (:remedy response))
              "the text carries the remedy"))))))

;; ============================================================
;; Rendered rows EQUAL receipt rows, in order (O2 round 2)
;; ============================================================
;; Reviewer finding 9 (2026-09-03): the O2 witnesses caught row LOSS and not
;; row DISAGREEMENT. Mutating `ls-tree-payload-text` to drop 40% of the rows
;; produced 7 failures and dropping the payload produced 15 — but REVERSING
;; the row order produced 49 pass / 0 fail, and adding a phantom row the
;; receipt does not contain produced 49 pass / 0 fail. A disagreement is
;; structurally impossible in the shipped code, which is the right design;
;; nothing held a future refactor to it. These witnesses do.

(defn- ls-tree-rendered-rows
  "The payload rows a client actually reads out of an ls-tree text block."
  [text]
  (->> (str/split-lines text)
       (drop 2)
       (take-while #(not (re-matches #"^[!✓→].*" %)))
       (remove str/blank?)
       vec))

(defn- ls-tree-receipt-rows
  [result]
  (->> (str/split-lines (str/trim (:tree result)))
       (remove str/blank?)
       vec))

;; @spec MCP-OP-STUDY-041
(deftest rendered-rows-equal-receipt-rows-in-order-for-every-mode
  (doseq [[label operation extra]
          [["deps" "deps" {"limit" 16384}]
           ["topo" "topo" {"limit" 16384}]
           ["ls-deps" "ls-deps" {"form" "extraction-closure" "limit" 16384}]
           ["ls-extract" "ls-extract" {"form" "extraction-closure"
                                       "limit" 16384}]
           ["outline" "outline" {}]
           ["forms" "forms" {"forms" ["reader-cond?" "splicing-rcond?"]
                             "expect" {"forms" 2}}]
           ["match" "match" {"match" "(defn- _ _ _)"}]
           ["xray" "xray" {"expression"
                           "(-> (form 'reader-cond?) (xray count))"}]]]
    (testing label
      (let [response (one operation extra)
            result (result-of response)
            rows (inspect/result-rows result)]
        (is (seq rows) "a mode with no rows proves nothing here")
        (is (= rows (evidence-rows (summary-of response)))
            "the rendered rows are the receipt's rows, in the receipt's order")))))

;; @spec MCP-OP-STUDY-036
;; @spec MCP-OP-STUDY-041
(deftest ls-tree-rendered-rows-equal-its-receipt-rows-in-order
  (with-tmp-project
    #(build-toy-project! % 10)
    (fn [config]
      (let [result (inspect-tool/execute-ls-tree
                     config {:mode "ls-tree" :dir "." :format "text"})]
        (is (= (ls-tree-receipt-rows result)
               (ls-tree-rendered-rows (text-block result))))))))

(defn- with-mutation
  "Run `thunk` with `mutate` wrapped around `target`, then restore it."
  [target mutate thunk]
  (let [original @target]
    (try
      (alter-var-root target (constantly (mutate original)))
      (thunk)
      (finally
        (alter-var-root target (constantly original))))))

;; @spec MCP-OP-STUDY-041
(deftest a-reordered-or-invented-row-fails-the-equality-witness
  ;; The ratchet itself. Without these two mutations the equality assertion
  ;; above could be satisfied by a renderer that agrees on the SET of rows and
  ;; lies about their order or their number — the exact pair the O2 suite
  ;; missed.
  (let [response (one "deps" {"limit" 16384})
        result (result-of response)
        agree? #(= (inspect/result-rows result)
                   (evidence-rows (summary-of response)))]
    (is (agree?) "baseline agrees")
    (testing "reversed row ORDER must fail"
      (with-mutation
        #'clj-surgeon.mcp-inspect/render-evidence
        (fn [original]
          (fn [result allowance]
            (update (original result allowance) :lines #(vec (reverse %)))))
        #(is (not (agree?))
             "a renderer that reverses the rows must not pass")))
    (testing "a phantom row the receipt does not contain must fail"
      (with-mutation
        #'clj-surgeon.mcp-inspect/render-evidence
        (fn [original]
          (fn [result allowance]
            (update (original result allowance) :lines
                    #(conj (vec %) "    · phantom-row defn@1 → (none)"))))
        #(is (not (agree?))
             "a renderer that invents a row must not pass")))
    (is (agree?) "and the mutation is restored")))

;; @spec MCP-OP-STUDY-036
;; @spec MCP-OP-STUDY-041
(deftest a-reordered-or-invented-ls-tree-row-fails-the-equality-witness
  (with-tmp-project
    #(build-toy-project! % 10)
    (fn [config]
      (let [result (inspect-tool/execute-ls-tree
                     config {:mode "ls-tree" :dir "." :format "text"})
            agree? #(= (ls-tree-receipt-rows result)
                       (ls-tree-rendered-rows (text-block result)))]
        (is (agree?) "baseline agrees")
        (testing "reversed row ORDER must fail"
          (with-mutation
            #'clj-surgeon.mcp-inspect-tool/ls-tree-payload-block
            (fn [original]
              (fn [result]
                (update (original result) :text
                        #(str/join "\n" (reverse (str/split-lines %))))))
            #(is (not (agree?)))))
        (testing "a phantom row must fail"
          (with-mutation
            #'clj-surgeon.mcp-inspect-tool/ls-tree-payload-block
            (fn [original]
              (fn [result]
                (update (original result) :text
                        #(str % "\n  99: defn phantom-form []"))))
            #(is (not (agree?)))))
        (is (agree?) "and the mutation is restored")))))

;; ============================================================
;; THE CLASS RATCHET: a new mode or refusal cannot ship text-blind
;; ============================================================
;; O2 fixed one of ten members of this class and shipped. The re-review found
;; the other nine by hand. That is the failure worth ratcheting: not any one
;; mode's rendering, but the fact that nothing made the NEXT mode prove it
;; renders. The two tables below are derived from the tool's PUBLISHED schema,
;; so an operation or mode added to `inspect_clojure` without a rendering — or
;; without an entry here — fails this witness before it can reach a client.

(defn- schema-operations
  []
  (set (keep #(get-in % [:properties "operation" :const])
             (get-in inspect-tool/inspect-schema
                     [:properties "requests" :items :oneOf]))))

(defn- schema-modes
  []
  (set (get-in inspect-tool/inspect-schema [:properties "mode" :enum])))

(def ^:private class-ratchet-fixture
  (str "(ns fixture.core\n"
       "  (:require [clojure.string :as str]))\n"
       "\n"
       "(defn- helper [x]\n"
       "  (str/upper-case (str x)))\n"
       "\n"
       "(defn greet [name]\n"
       "  (helper name))\n"
       "\n"
       "(defn shout [name]\n"
       "  (str (greet name) \"!\"))\n"))

(def ^:private class-ratchet-requests
  "One request per published `operation`. The key set is asserted equal to the
   schema's, so a NEW operation lands here as a failing test rather than as a
   text block carrying a count."
  {"forms" {"forms" ["greet"] "expect" {"forms" 1}}
   "outline" {}
   "match" {"match" "(str _ _)"}
   "xray" {"expression" "(-> (form 'greet) (xray count))"}
   "deps" {"limit" 16384}
   "topo" {"limit" 16384}
   "ls-deps" {"form" "shout" "limit" 16384}
   "ls-extract" {"form" "shout" "limit" 16384}})

(def ^:private class-ratchet-mode-coverage
  "Every published `mode`, and where its text block is witnessed.

   `prepare-change` and `plan-extraction` carry their own dedicated summaries
   and their own suites; they are named here so that a mode added to the
   vocabulary cannot pass unclassified."
  {"ls-tree" :witnessed-here
   "prepare-change" :witnessed-in-mcp-inspect-tool-test
   "plan-extraction" :witnessed-in-mcp-extraction-plan-test})

;; @spec MCP-OP-STUDY-041
(deftest every-published-mode-renders-its-rows-in-the-text
  (is (= (schema-operations) (set (keys class-ratchet-requests)))
      (str "every published operation needs an entry in the class ratchet; "
           "unclassified: "
           (pr-str (into (vec (remove (set (keys class-ratchet-requests))
                                      (schema-operations)))
                         (remove (schema-operations)
                                 (keys class-ratchet-requests))))))
  (is (= (schema-modes) (set (keys class-ratchet-mode-coverage)))
      "every published mode is classified as witnessed here or named elsewhere")
  (with-tmp-project
    (fn [dir]
      (spit (str dir "/deps.edn") "{:paths [\"src\"]}")
      (fs/create-dirs (str dir "/src/fixture"))
      (spit (str dir "/src/fixture/core.clj") class-ratchet-fixture))
    (fn [config]
      (doseq [[operation extra] (sort class-ratchet-requests)]
        (testing operation
          (let [response (inspect-tool/execute-inspect!
                           config
                           {"requests" [(merge {"id" "r1"
                                                "operation" operation
                                                "file" "src/fixture/core.clj"}
                                               extra)]
                            "expect" {"requests" 1 "files" 1}})
                result (result-of response)
                text (inspect-tool/inspect-summary
                       (assoc response :elapsed_ms 1.0))
                rows (inspect/result-rows result)]
            (is (true? (:ok response)) (pr-str (:error response)))
            (is (seq rows)
                (str operation " publishes no rows: `result-evidence` has no "
                     "case for it, so its text block is a count"))
            (is (= rows (evidence-rows text))
                "rendered rows equal receipt rows, in receipt order")
            (is (str/includes? text (:id result))
                "and the block is addressable by its request id"))))
      (testing "ls-tree"
        (let [result (inspect-tool/execute-ls-tree
                       config {:mode "ls-tree" :dir "." :format "text"})]
          (is (= (ls-tree-receipt-rows result)
                 (ls-tree-rendered-rows (text-block result)))))))))

;; @spec MCP-OP-STUDY-042
(def ^:private refusal-ratchet-cases
  "Every refusal kind the ratchet drives, as `[label params]`. Named once so
   the cause witness, the leaf-coverage witness, and the enumeration witness
   all drive exactly the same set."
  [[:missing-fields {"requests" [{"operation" "deps"}]
                             "expect" {"requests" 1 "files" 1}}]
           [:unknown-fields {"requests" [{"operation" "deps"
                                          "file" real-file "pattern" "x"}]
                             "expect" {"requests" 1 "files" 1}}]
           [:expectation-mismatch {"requests" [{"operation" "outline"
                                                "file" real-file}]
                                   "expect" {"requests" 2 "files" 1}}]
           [:unsupported-operation {"requests" [{"operation" "extract!"
                                                 "file" real-file}]
                                    "expect" {"requests" 1 "files" 1}}]
           [:invalid-xray {"requests" [{"operation" "xray" "file" real-file
                                        "expression" "*"}]
                           "expect" {"requests" 1 "files" 1}}]
           [:form-not-found {"requests" [{"operation" "forms" "file" real-file
                                          "forms" ["no-such-form-xyz"]
                                          "expect" {"forms" 1}}]
                             "expect" {"requests" 1 "files" 1}}]
           [:study-form-not-found {"requests" [{"operation" "ls-deps"
                                                "file" real-file
                                                "form" "no-such-form-xyz"}]
                                   "expect" {"requests" 1 "files" 1}}]
           [:file-not-found {"requests" [{"operation" "outline"
                                          "file" "src/no_such_file_xyz.clj"}]
                             "expect" {"requests" 1 "files" 1}}]
           [:match-expectation {"requests" [{"operation" "match"
                                             "file" real-file
                                             "match" "(defn- _ _ _)"
                                             "expect" {"matches" 99}}]
                                "expect" {"requests" 1 "files" 1}}]
           [:invalid-study-limit {"mode" "ls-tree" "dir" "." "limit" 99999}]
           [:invalid-format {"mode" "ls-tree" "dir" "." "format" "EDN"}]
           [:dir-not-found {"mode" "ls-tree" "dir" "no-such-dir-xyz"}]
   [:unknown-parameter {"mode" "ls-tree" "dir" "." "depth" 2}]])

;; @spec MCP-OP-STUDY-042
(deftest every-refusal-kind-renders-its-cause-and-carries-no-unrendered-fact
  (doseq [[label params] refusal-ratchet-cases]
    (testing (name label)
      (let [response (run params)
            text (summary-of response)]
        (is (false? (:ok response)) "the fixture must actually refuse")
        (is (str/includes? text "refused ·"))
        (is (str/includes? text (:error response))
            "the cause travels verbatim in the text")
        (is (or (str/includes? text (str (:next_action response)))
                (str/includes? text "next call:")
                (str/includes? text "retry"))
            "and a next action, or the next call spelled")
        (when (:remedy response)
          (is (str/includes? text (:remedy response))))
        ;; The class ratchet proper: EVERY key a refusal carries is either
        ;; rendered as structure (header, cause, owners, continuation) or
        ;; printed as a `key: value` detail line. A new refusal field is
        ;; therefore carried into the text the day it is added, and a
        ;; deliberate exclusion has to be written into
        ;; `refusal-structural-keys` where a reviewer can see it.
        (let [unrendered (remove
                           (fn [key]
                             (or (contains? inspect-tool/refusal-structural-keys
                                            key)
                                 (str/includes? text (str "  " (name key) ": "))))
                           (keys response))]
          (is (empty? unrendered)
              (str "a refusal carries facts the text never renders: "
                   (pr-str (vec unrendered)))))))))

;; @spec MCP-OP-STUDY-040
(deftest a-receipt-that-cannot-fit-even-text-free-is-a-typed-refusal
  ;; The other side of the bound. Real reads reach an earlier gate first —
  ;; `batch-source-limit-exceeded` for `forms`, the per-request result budget
  ;; for the study operations — so this branch is witnessed directly rather
  ;; than left to a fixture that may never reach it. A receipt whose
  ;; structured content ALONE crosses the budget cannot be rescued by any
  ;; rendering choice, and must say so in the vocabulary the tool already uses.
  (let [oversized {:ok true
                   :operation "inspect_clojure"
                   :mode "ls-tree"
                   :request_count 1 :file_count 1
                   :source_character_count 0
                   :results []
                   :tree (apply str (repeat 40000 "x"))}
        refusal (inspect-tool/fit-public-result oversized)
        text (inspect-tool/inspect-summary (assoc refusal :elapsed_ms 0.0))]
    (is (false? (:ok refusal)))
    (is (= "inspect-output-limit" (:error_type refusal)))
    (is (= "public_result" (:scope refusal)))
    (is (= inspect-tool/max-public-result-bytes
           (get-in refusal [:limits :public_result_bytes])))
    (is (< inspect-tool/max-public-result-bytes
           (get-in refusal [:required :public_result_bytes])))
    (is (>= inspect-tool/max-public-result-bytes
            (public-bytes refusal))
        "and the refusal itself fits")
    (testing "the text names the measured bytes, the budget, and the remedy"
      (is (str/includes? text (:error refusal)))
      (is (str/includes? text (:remedy refusal)))
      (is (str/includes? text "narrow_scope")))))

;; ============================================================
;; O2 ROUND 3 — the bound is a bound on the TEXT, and a refusal is the
;; last resort (Sol O2 round-2 review, section 2)
;; ============================================================
;; Three separate ways the round-2 implementation broke its own stated
;; contract, each reproduced from the review's own probe:
;;
;;   1. `fit-public-result` refuses after four unsuccessful halvings of the
;;      text allowance. At one byte over the budget the receipt ALONE measured
;;      32,558 bytes — 210 under the 32,768 budget — and the caller got a
;;      typed refusal instead of the answer with a bounded text block.
;;   2. A 32-result batch whose receipt measures 31,549 bytes is refused,
;;      because `min-evidence-characters` holds a 512-character floor per
;;      result that the budget is never allowed to lower.
;;   3. `render-evidence` always renders the first row, counts it as shown,
;;      and silently omits its body — so a 10,000-character form body vanished
;;      from the text under `✓ terminal evidence · read_complete=true · next
;;      action none`. And `ls-tree-summary` printed `! text abridged · 97 of
;;      200 rows` and `✓ complete tree · read_complete=true` in one block.

(defn- structured-bytes
  "The public result measured with NO text block: the floor no rendering
   choice can get under."
  [result]
  (inspect-tool/mcp-result-byte-count "" result))

(def ^:private terminal-claim
  "✓ terminal evidence · read_complete=true · next action none")

;; @spec MCP-OP-STUDY-040
(deftest a-receipt-whose-structured-content-fits-is-bounded-never-refused
  (let [base {:ok true :operation "inspect_clojure"
              :request_count 0 :file_count 0
              :source_character_count 0 :results [] :padding ""}
        pad (fn [n] (assoc base :padding (apply str (repeat n "x"))))
        ;; The bound a fitted result is measured against is the declared
        ;; budget less `publish-reserve`: the fit measures with the clock
        ;; stopped at zero and the publisher renders the real elapsed time
        ;; into both the text block and `structuredContent`. The size is
        ;; SEARCHED rather than computed, because since MCP-OP-STUDY-044 one
        ;; more padding character no longer costs exactly one more byte.
        exact (loop [low 0 high inspect-tool/max-fitted-result-bytes
                     best (pad 0)]
                (if (> low high)
                  best
                  (let [mid (quot (+ low high) 2)
                        probe (pad mid)]
                    (if (<= (public-bytes probe)
                            inspect-tool/max-fitted-result-bytes)
                      (recur (inc mid) high probe)
                      (recur low (dec mid) best)))))
        over (update exact :padding str "x")]
    (testing "at the bound the result passes through unchanged"
      (is (>= inspect-tool/max-fitted-result-bytes (public-bytes exact)))
      (is (< inspect-tool/max-fitted-result-bytes (public-bytes over))
          "and one character more is over it")
      (is (= exact (inspect-tool/fit-public-result exact))))
    (testing "one byte over is a bounded TEXT, not a refusal"
      (is (>= inspect-tool/max-public-result-bytes (structured-bytes over))
          "the receipt alone fits, so nothing forces a refusal")
      (let [fitted (inspect-tool/fit-public-result over)]
        (is (true? (:ok fitted))
            (str "refused with " (structured-bytes over)
                 " bytes of receipt under a "
                 inspect-tool/max-public-result-bytes " byte budget"))
        (is (>= inspect-tool/max-public-result-bytes (public-bytes fitted)))))))

;; @spec MCP-OP-STUDY-040
(deftest the-per-result-evidence-floor-never-forces-a-refusal
  (with-tmp-project
    (fn [dir]
      (spit (str dir "/deps.edn") "{:paths [\"src\"]}")
      (fs/create-dirs (str dir "/src/fixture"))
      (dotimes [i 32]
        (spit (str dir "/src/fixture/f" i ".clj")
              (format "(ns fixture.f%d)\n(defn f%d [] \"%s\")\n"
                      i i (apply str (repeat 100 "x"))))))
    (fn [config]
      (let [raw (inspect-tool/execute-inspect!
                  config
                  {"requests" (mapv (fn [i]
                                      {"id" (str "r" i) "operation" "forms"
                                       "file" (str "src/fixture/f" i ".clj")
                                       "forms" [(str "f" i)]
                                       "expect" {"forms" 1}
                                       "include_source" true})
                                    (range 32))
                   "expect" {"requests" 32 "files" 32}})
            fitted (inspect-tool/fit-public-result raw)]
        (is (true? (:ok raw)))
        (is (>= inspect-tool/max-public-result-bytes (structured-bytes raw))
            "the 32-result receipt fits with room to spare")
        (is (true? (:ok fitted))
            (str "refused a receipt of " (structured-bytes raw)
                 " bytes because 32 results claim a 512-character floor each"))
        (is (>= inspect-tool/max-public-result-bytes (public-bytes fitted)))))))

;; @spec MCP-OP-STUDY-041
(deftest a-row-whose-body-the-text-dropped-is-never-terminal-evidence
  (let [huge (apply str (repeat 10000 "z"))
        raw {:ok true :operation "inspect_clojure"
             :request_count 1 :file_count 1
             :source_character_count 10000
             :read_complete true :next_action "none"
             :results [{:id "one" :operation "forms" :file "src/huge.clj"
                        :file_hash "deadbeef" :form_count 1
                        :source_character_count 10000
                        :forms [{:name "huge" :line 1 :end_line 1
                                 :form_type "def" :source huge}]}]}
        fitted (inspect-tool/fit-public-result raw)
        text (inspect-tool/inspect-summary (assoc fitted :elapsed_ms 0.0))]
    (is (true? (:ok fitted)))
    (is (or (str/includes? text huge)
            (str/includes? text "text abridged"))
        "the body travels, or the text says it did not")
    (is (not (str/includes? text terminal-claim))
        "a text that dropped a row body never claims terminal evidence")))

;; @spec MCP-OP-STUDY-040
(deftest an-abridged-tree-never-also-claims-a-complete-tree
  (let [tree (str/join "\n"
                       (map #(format "src/f%03d.clj  %s"
                                     % (apply str (repeat 90 "x")))
                            (range 200)))
        raw {:ok true :operation "inspect_clojure" :mode "ls-tree"
             :dir "." :format "text" :project_count 1
             :file_count 200 :returned 200 :omitted 0
             :tree tree :truncated false :read_complete true
             :next_action "none"}
        fitted (inspect-tool/fit-public-result raw)
        text (inspect-tool/inspect-summary (assoc fitted :elapsed_ms 0.0))]
    (is (true? (:ok fitted)))
    (is (not (and (str/includes? text "text abridged")
                  (str/includes? text "complete tree · read_complete=true")))
        "one block cannot say both that rows were dropped and that the tree is complete")
    (is (>= inspect-tool/max-public-result-bytes (public-bytes fitted)))))

;; @spec MCP-OP-STUDY-040
(deftest the-read-entrance-publishes-nothing-larger-than-the-budget
  ;; The bound at the ENTRANCE a client actually calls, not at the pure
  ;; function: `handle-inspect` is the callback both the stdio server and the
  ;; HTTP server publish through.
  (with-tmp-project
    #(build-toy-project! % 78)
    (fn [config]
      (let [calls (atom [])]
        (try
          (inspect-tool/init! config)
          (inspect-tool/handle-inspect
            nil {"mode" "ls-tree" "dir" "." "format" "text" "limit" 16384}
            (fn [content error? structured]
              (swap! calls conj {:text (first content)
                                 :error? error?
                                 :structured structured})))
          (let [{:keys [text error? structured]} (first @calls)]
            (is (false? error?))
            (is (>= inspect-tool/max-public-result-bytes
                    (inspect-tool/mcp-result-byte-count text structured))
                "the published pair is inside the declared budget")
            (is (not (and (str/includes? text "text abridged")
                          (str/includes? text "complete tree · read_complete=true")))))
          (finally (inspect-tool/init! nil)))))))

;; ============================================================
;; O2 ROUND 3 — the strict criterion: `content[0].text` carries EVERY
;; `structuredContent` leaf (Sol O2 round-2 review, section 3)
;; ============================================================
;; Round two rendered SELECTED row fields. That is enough for the narrower row
;; strings MCP-OP-STUDY-041 asserts, and it is not the criterion: a projection
;; is a decision nobody can review, and the fields it leaves out are invisible
;; until someone walks the receipt by hand. Sol did, and found 182 uncarried
;; leaves over nine modes.
;;
;; This is ONE machine-checked witness. It parses the receipt into the JSON
;; shape a client is handed, walks every leaf, and asserts each value appears
;; verbatim in the text block, through the same predicate the renderer uses.

(def ^:private refusal-structural-keys-names
  "The structural-key set as one searchable string, so a witness can say out
   loud that the key it is probing really is a member."
  (str/join " " (map name inspect-tool/refusal-structural-keys)))

(defn- leaf-misses
  [text result]
  (inspect/uncarried-leaves text result))

(defn- miss-report
  [misses]
  (str/join "; "
            (map (fn [[path value]]
                   (str (pr-str path) " = "
                        (pr-str (if (and (string? value) (> (count value) 60))
                                  (str (subs value 0 60) "…")
                                  value))))
                 (take 6 misses))))

;; @spec MCP-OP-STUDY-044
(deftest every-published-mode-text-carries-every-structured-content-leaf
  (with-tmp-project
    (fn [dir]
      (spit (str dir "/deps.edn") "{:paths [\"src\"]}")
      (fs/create-dirs (str dir "/src/fixture"))
      (spit (str dir "/src/fixture/core.clj") class-ratchet-fixture))
    (fn [config]
      (doseq [[operation extra] (sort class-ratchet-requests)]
        (testing operation
          (let [published (assoc
                            (inspect-tool/fit-public-result
                              (inspect-tool/execute-inspect!
                                config
                                {"requests" [(merge {"id" "r1"
                                                     "operation" operation
                                                     "file" "src/fixture/core.clj"}
                                                    extra)]
                                 "expect" {"requests" 1 "files" 1}}))
                            :elapsed_ms 1.0)
                text (inspect-tool/inspect-summary published)
                misses (leaf-misses text published)]
            (is (true? (:ok published)) (pr-str (:error published)))
            (is (empty? misses)
                (str operation ": " (count misses)
                     " receipt leaves the text does not carry — "
                     (miss-report misses))))))
      (testing "ls-tree"
        (let [published (assoc
                          (inspect-tool/fit-public-result
                            (inspect-tool/execute-ls-tree
                              config {:mode "ls-tree" :dir "."
                                      :format "text"}))
                          :elapsed_ms 1.0)
              text (inspect-tool/inspect-summary published)
              misses (leaf-misses text published)]
          (is (true? (:ok published)))
          (is (empty? misses)
              (str "ls-tree: " (count misses)
                   " receipt leaves the text does not carry — "
                   (miss-report misses)))))
      (testing "ls-tree format=names"
        (let [published (assoc
                          (inspect-tool/fit-public-result
                            (inspect-tool/execute-ls-tree
                              config {:mode "ls-tree" :dir "."
                                      :format "names"}))
                          :elapsed_ms 1.0)
              text (inspect-tool/inspect-summary published)
              misses (leaf-misses text published)]
          (is (empty? misses)
              (str "ls-tree names: " (count misses) " — "
                   (miss-report misses))))))))

;; @spec MCP-OP-STUDY-044
;; @spec MCP-OP-STUDY-040
(deftest a-budget-abridged-text-declares-its-drop-and-is-never-terminal
  ;; The other side of the criterion. A text the public output budget forced
  ;; to drop rows or facts cannot carry every leaf — and must say exactly
  ;; that, name where the complete receipt is, and never read as terminal.
  (with-tmp-project
    #(build-toy-project! % 200)
    (fn [config]
      (let [published (assoc
                        (inspect-tool/fit-public-result
                          (inspect-tool/execute-ls-tree
                            config {:mode "ls-tree" :dir "." :format "text"
                                    :limit 16384}))
                        :elapsed_ms 1.0)
            text (inspect-tool/inspect-summary published)
            misses (leaf-misses text published)]
        (is (seq misses)
            "the fixture must actually reach the budget for this to mean anything")
        (is (str/includes? text "abridged")
            "a text that could not carry the receipt says so")
        (is (str/includes? text "structuredContent")
            "and names where the complete receipt is")
        (is (not (str/includes? text "✓ complete tree · read_complete=true")))
        (is (not (str/includes? text terminal-claim)))
        (is (>= inspect-tool/max-public-result-bytes (public-bytes published)))))))

;; @spec MCP-OP-STUDY-044
(deftest the-excluded-leaf-set-is-frozen-and-every-member-carries-its-reason
  ;; An exclusion is a deliberate, reviewable decision. Growing this set is a
  ;; failing test, not a silent projection.
  (is (= #{:workspace_root} inspect/text-excluded-leaf-keys)
      "a new exclusion needs a reason in the docstring and an edit here")
  (is (str/includes? (:doc (meta #'inspect/text-excluded-leaf-keys))
                     "workspace_root")
      "every excluded key names, at its definition, why it is excluded"))

;; ============================================================
;; O2 ROUND 3 — a name in the structural-key set is not a free pass
;; (Sol O2 round-2 review, section 6)
;; ============================================================
;; The round-2 refusal ratchet treats every member of
;; `refusal-structural-keys` as rendered WITHOUT proving that any renderer
;; consumes it. Sol reproduced the escape in a temp clone: add a top-level
;; refusal fact, name it in the exclusion set, supply no renderer, and all
;; 5,998 assertions stayed green. "A new refusal cannot ship text-blind" was
;; therefore false — the set was the escape hatch.
;;
;; The fix is not a longer list. It is the SAME criterion the success modes
;; got in MCP-OP-STUDY-044: whatever the receipt carries, the text carries.
;; Then membership in the structural set decides only WHERE a fact is
;; rendered, never WHETHER — and the escape is unrepresentable rather than
;; merely detected.

;; @spec MCP-OP-STUDY-044
(deftest a-key-named-in-the-structural-set-is-not-a-free-pass
  ;; Sol's escape, reproduced without editing src: three keys that are all
  ;; named in `refusal-structural-keys` and that no refusal renderer prints.
  (let [refusal {:ok false
                 :operation "inspect_clojure"
                 :error_type "synthetic-refusal"
                 :error "a synthetic refusal driving the class ratchet"
                 :read_complete false
                 :source_unchanged true
                 :next_action "correct_request"
                 :elapsed_ms 0.0
                 :file_hashes {"src/fixture.clj" "HIDDEN-REFUSAL-FACT"}
                 :dir "src/some-scope"
                 :limit 4096}
        text (inspect-tool/inspect-summary refusal)]
    (is (str/includes? refusal-structural-keys-names "file_hashes"))
    (is (str/includes? text "HIDDEN-REFUSAL-FACT")
        "a value under a key the ratchet calls structural still reaches the text")
    (is (str/includes? text "src/some-scope"))
    (is (str/includes? text "4096"))
    (is (empty? (leaf-misses text refusal))
        (str "the refusal carries facts the text never renders: "
             (miss-report (leaf-misses text refusal))))))

;; @spec MCP-OP-STUDY-044
(deftest every-refusal-kind-text-carries-every-structured-content-leaf
  (doseq [[label params] refusal-ratchet-cases]
    (testing (name label)
      (let [response (assoc (inspect-tool/fit-public-result (run params))
                            :elapsed_ms 1.0)
            text (inspect-tool/inspect-summary response)
            misses (leaf-misses text response)]
        (is (false? (:ok response)) "the fixture must actually refuse")
        (is (empty? misses)
            (str (name label) ": " (count misses)
                 " receipt leaves the text does not carry — "
                 (miss-report misses)))))))

;; ============================================================
;; O2 ROUND 3 — a continuation in the text is the VERBATIM executable
;; request (Sol O2 round-2 review, section 4)
;; ============================================================
;; The typed `deps` continuation replays: the text spells the tool and the
;; compact JSON argument object, and a caller can paste it. `ls-tree` spelled
;; `next call: inspect_clojure mode=ls-tree dir=. format=text limit=16384` —
;; neither a JSON tool-argument object nor a shell command, so it is
;; retypeable guidance, not an executable continuation. Two renderings of one
;; concept is how a caller learns not to trust either.

(defn- next-call-line
  [text]
  (first (keep #(second (re-find #"next call: (.*)$" %))
               (str/split-lines text))))

(defn- next-call-arguments
  "The arguments a text-block continuation actually spells, parsed."
  [text]
  (when-let [line (next-call-line text)]
    (let [[tool arguments] (str/split line #" " 2)]
      {:tool tool
       :arguments (try (json/parse-string arguments)
                       (catch Exception _ ::unparseable))})))

(defn- receipt-arguments
  [call]
  (json/parse-string
    (json/generate-string (inspect/json-data (:arguments call)))))

;; @spec MCP-OP-STUDY-045
(deftest a-next-call-in-the-text-is-the-request-structured-content-carries
  (testing "typed mode"
    (let [response (one "deps" {"limit" 200})
          result (result-of response)
          text (summary-of response)
          spelled (next-call-arguments text)]
      (is (some? (:next_call result)) "the fixture must actually continue")
      (is (= "inspect_clojure" (:tool spelled)))
      (is (= (receipt-arguments (:next_call result)) (:arguments spelled))
          "the text spells exactly the request structuredContent carries")))
  (testing "ls-tree mode"
    (with-tmp-project
      #(build-toy-project! % 60)
      (fn [config]
        (let [result (inspect-tool/execute-ls-tree
                       config {:mode "ls-tree" :dir "." :format "text"
                               :limit 4096})
              text (text-block result)
              spelled (next-call-arguments text)]
          (is (some? (:next_call result)) "the fixture must actually continue")
          (is (= "inspect_clojure" (:tool spelled)))
          (is (not= ::unparseable (:arguments spelled))
              (str "the continuation is not an executable request: "
                   (pr-str (next-call-line text))))
          (is (= (receipt-arguments (:next_call result)) (:arguments spelled))
              "the text spells exactly the request structuredContent carries")
          (testing "and it replays"
            (let [replayed (inspect-tool/execute-ls-tree
                             config
                             (walk/keywordize-keys (:arguments spelled)))]
              (is (true? (:ok replayed)))
              (is (< (:returned result) (:returned replayed))
                  "a continuation that does not advance is a loop"))))))))
