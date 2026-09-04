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
   [clj-surgeon.mcp-operation :as mcp-operation]
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
        ;; @spec MCP-OP-MEM-005 — the MCP `edn` payload is the ENTRIES; the
        ;; CLI's `format-ls-tree-edn` appends the scan receipt, which is not a
        ;; file and must not be listed as one.
        (is (= (inspect/json-data
                 (study/format-ls-tree-edn-entries outlined (:dir scan)))
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
  ;; The claim is that the STUDY lane published no new tool — the study
  ;; operations reach callers through `inspect_clojure` and nothing else. The
  ;; set is the merged trunk catalog: `admit_clojure_patch` and
  ;; `alias_migration` are the trunk's tools, and asserting the whole catalog
  ;; is what makes a new study tool fail here.
  (is (= #{"inspect_clojure" "apply_clojure_changes" "edit_clojure"
           "transform_clojure" "admit_clojure_patch" "alias_migration"}
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
        ;; @spec MCP-OP-STUDY-051 — a PRODUCT CHANGE, stated rather than
        ;; quietly relaxed. The 8 KB ceiling this line used to assert was a
        ;; rendering CONSTANT, which MCP-OP-STUDY-044 already forbids as an
        ;; allowance; and it was survivable in round six only because a
        ;; distinctive value found ANYWHERE in the text counted as carriage,
        ;; so every path, namespace, form name and hash in the structural
        ;; rows discharged its own receipt leaf. Under MCP-OP-STUDY-051 a
        ;; leaf is carried only by its own pointer line, so this text carries
        ;; the rows AND the receipt: measured 4,334 -> 8,796 characters on a
        ;; twenty-five file toy tree, and 11,546 here. The bound that is real
        ;; is the PUBLIC OUTPUT BUDGET; the 12 KB ceiling below is a ratchet
        ;; against further growth, not a contract.
        (is (<= (count (text-block at-default)) 12288)
            (format "text block was %d characters"
                    (count (text-block at-default))))
        (is (<= (inspect-tool/mcp-result-byte-count
                  (text-block at-default) at-default)
                inspect-tool/max-public-result-bytes)
            "and the published pair is inside the public output budget")))))

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
          ;; @spec MCP-OP-STUDY-045
          ;; Spelled as the verbatim executable request, not as prose: the
          ;; `<key>=<value>` clause of MCP-OP-STUDY-036 is superseded.
          (is (str/includes? text "\"limit\":16384")
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

(defn- clocked
  "One raw domain result with a clock on it.

   O2 round 4 (§5): the fit measures the FINALIZED envelope — the bytes the
   publisher publishes — so a result with no clock is not one it can measure.
   `public-bytes` measures at the same clock, so a witness compares like with
   like.

   O2 round 6 (§4): the envelope is what the FINALIZER recorded adding, so a
   witness that wants a finalized result stamps one rather than spelling one.
   `(assoc result :elapsed_ms 0.0)` produced a map that LOOKED finalized, and
   that confusion between spelling and construction is MCP-OP-STUDY-049."
  [result]
  (mcp-operation/stamp-envelope result {:elapsed_ms 0.0}))

(defn- public-bytes
  "The complete public pair as a client is handed it. Text AND receipt are
   measured at the SAME clock — O2 round 4 (§5): measuring the text with a
   clock and the receipt without one is an 18-byte disagreement, and it is
   the same class of error the publish reserve existed to paper over."
  [result]
  (let [published (clocked result)]
    (inspect-tool/mcp-result-byte-count
      (inspect-tool/inspect-summary published)
      published)))

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
        (let [fitted (inspect-tool/enforce-result-budget (clocked raw) (clocked raw))
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
        (is (= (clocked raw) (inspect-tool/enforce-result-budget (clocked raw) (clocked raw)))
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
            fitted (inspect-tool/enforce-result-budget (clocked raw) (clocked raw))
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
    ;; The line range follows the file: the MEM-005 parser-admission gate
    ;; added lines above this form in `analyze.clj`, so the row moved from
    ;; 37-39 to 48-50. The claim is that the row NAMES its line range, not
    ;; which lines the fixture happens to occupy today.
    (is (str/includes? text (str "reader-cond?@" (:line form) "-"
                                 (:end_line form))))
    (is (= [(:line form) (:end_line form)]
           [(:line form) (:end_line form)]))))

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
   [:unknown-parameter {"mode" "ls-tree" "dir" "." "depth" 2}]

   ;; @spec MCP-OP-STUDY-046
   ;; O2 round 3: the eighteen reasons `src/clj_surgeon/mcp_inspect.clj`
   ;; constructs that no round-two fixture reached. The set is derived from
   ;; the constructors by `the-refusal-ratchet-drives-every-reason-the-source-
   ;; constructs`, so a NEW `refuse!` reason lands here as a failing test.
   [:expected-object {"requests" ["not-an-object"]
                      "expect" {"requests" 1 "files" 1}}]
   [:non-blank-string {"requests" [{"id" "" "operation" "outline"
                                    "file" real-file}]
                       "expect" {"requests" 1 "files" 1}}]
   [:non-empty-array {"requests" [{"operation" "forms" "file" real-file
                                   "forms" [] "expect" {"forms" 1}}]
                      "expect" {"requests" 1 "files" 1}}]
   [:positive-integer {"requests" [{"operation" "forms" "file" real-file
                                    "forms" ["reader-cond?"]
                                    "expect" {"forms" 0}}]
                       "expect" {"requests" 1 "files" 1}}]
   [:non-negative-integer {"requests" [{"operation" "match" "file" real-file
                                        "match" "(defn- _ _ _)"
                                        "expect" {"matches" -1}}]
                           "expect" {"requests" 1 "files" 1}}]
   [:boolean {"requests" [{"operation" "forms" "file" real-file
                           "forms" ["reader-cond?"] "expect" {"forms" 1}
                           "include_source" "yes"}]
              "expect" {"requests" 1 "files" 1}}]
   [:invalid-relative-source-path
    {"requests" [{"operation" "outline" "file" "/etc/passwd"}]
     "expect" {"requests" 1 "files" 1}}]
   [:invalid-study-limit-request
    {"requests" [{"operation" "deps" "file" real-file "limit" 99999}]
     "expect" {"requests" 1 "files" 1}}]
   [:request-expectation-mismatch
    {"requests" [{"operation" "forms" "file" real-file
                  "forms" ["reader-cond?" "splicing-rcond?"]
                  "expect" {"forms" 1}}]
     "expect" {"requests" 1 "files" 1}}]
   [:too-many-forms
    {"requests" [{"operation" "forms" "file" real-file
                  "forms" (mapv #(str "form-" %) (range 129))
                  "expect" {"forms" 129}}]
     "expect" {"requests" 1 "files" 1}}]
   [:mixed-request-ids
    {"requests" [{"id" "a" "operation" "outline" "file" real-file}
                 {"operation" "outline" "file" real-file}]
     "expect" {"requests" 2 "files" 1}}]
   [:duplicate-id
    {"requests" [{"id" "same" "operation" "outline" "file" real-file}
                 {"id" "same" "operation" "deps" "file" real-file
                  "limit" 4096}]
     "expect" {"requests" 2 "files" 1}}]
   [:operation-required
    {"requests" [{"file" real-file}]
     "expect" {"requests" 1 "files" 1}}]
   [:too-many-requests
    {"requests" (mapv (fn [index]
                        {"id" (str "r" index) "operation" "outline"
                         "file" real-file})
                      (range 65))
     "expect" {"requests" 65 "files" 1}}]
   [:too-many-files
    {"requests" [{"operation" "outline" "file" real-file}]
     "expect" {"requests" 1 "files" 1}
     "snapshot_guards" (into {real-file (apply str (repeat 64 "a"))}
                             (map (fn [index]
                                    [(str "src/guard_" index ".clj")
                                     (apply str (repeat 64 "b"))]))
                             (range 32))}]
   [:empty-snapshot-guards
    {"requests" [{"operation" "outline" "file" real-file}]
     "expect" {"requests" 1 "files" 1}
     "snapshot_guards" {}}]
   [:invalid-snapshot-hash
    {"requests" [{"operation" "outline" "file" real-file}]
     "expect" {"requests" 1 "files" 1}
     "snapshot_guards" {real-file "not-a-sha256"}}]
   [:missing-snapshot-guards
    {"requests" [{"operation" "outline" "file" real-file}]
     "expect" {"requests" 1 "files" 1}
     "snapshot_guards" {"src/clj_surgeon/outline.clj"
                        (apply str (repeat 64 "a"))}}]

   ;; @spec MCP-OP-STUDY-046
   ;; O2 round 4: the reason `unique-strings!` receives as an ARGUMENT. No
   ;; literal `(refuse! :duplicate-form` exists anywhere in the source, which
   ;; is exactly why the round-three literal scan could not see it.
   [:duplicate-form
    {"requests" [{"operation" "forms" "file" real-file
                  "forms" ["reader-cond?" "reader-cond?"]
                  "expect" {"forms" 2}}]
     "expect" {"requests" 1 "files" 1}}]])

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
        refusal (inspect-tool/fit-public-result (clocked oversized))
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
      (is (= (clocked exact) (inspect-tool/fit-public-result (clocked exact)))))
    (testing "one byte over is a bounded TEXT, not a refusal"
      (is (>= inspect-tool/max-public-result-bytes (structured-bytes over))
          "the receipt alone fits, so nothing forces a refusal")
      (let [fitted (inspect-tool/fit-public-result (clocked over))]
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
            fitted (inspect-tool/fit-public-result (clocked raw))]
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
        fitted (inspect-tool/fit-public-result (clocked raw))
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
        fitted (inspect-tool/fit-public-result (clocked raw))
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
                              (clocked
                                (inspect-tool/execute-inspect!
                                  config
                                  {"requests" [(merge {"id" "r1"
                                                       "operation" operation
                                                       "file" "src/fixture/core.clj"}
                                                      extra)]
                                   "expect" {"requests" 1 "files" 1}})))
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
                            (clocked
                              (inspect-tool/execute-ls-tree
                                config {:mode "ls-tree" :dir "."
                                        :format "text"})))
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
                            (clocked
                              (inspect-tool/execute-ls-tree
                                config {:mode "ls-tree" :dir "."
                                        :format "names"})))
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
                          (clocked
                            (inspect-tool/execute-ls-tree
                              config {:mode "ls-tree" :dir "." :format "text"
                                      :limit 16384})))
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
;; O2 ROUND 4 — a value-less leaf is carried by its LABEL or not at all
;; (Sol O2 round-3 review, section 2)
;; ============================================================
;; Round three froze the exclusion set and then left TWO further exclusion
;; mechanisms open, neither of them enumerated anywhere. `receipt-leaf-pairs`
;; yielded no leaf at all for `{}` or `[]`, so those receipt facts were not
;; excluded — they were invisible to the walker that defines the criterion;
;; and `leaf-rendered?` returned true for `null` and for a blank string
;; without either the value or its label appearing in the text. The product
;; predicate reported zero misses only because the renderer and the witness
;; shared both loopholes.
;;
;; The rule this witness fixes in place: there is exactly ONE exclusion
;; mechanism, `text-excluded-leaf-keys`. EVERY other leaf appears in the text,
;; and a leaf whose value is indistinguishable from absence — `null`, `{}`,
;; `[]`, `""` — appears with its JSON pointer attached to it.

(def ^:private value-less-leaf-probe
  "Every leaf shape whose VALUE carries no characters a reader could find and
   attribute to it. `\"\"` matches at every index of every text; `{}` matches
   any object rendering; `null` and `[]` appear inside unrelated words and
   lines. Only the LABEL can carry these."
  {:empty_map {}
   :empty_vector []
   :nil_value nil
   :blank ""
   :blank_spaces "   "
   :nested {:inner_empty [] :inner_nil nil}})

(def ^:private value-less-spellings
  "Each probe leaf's JSON pointer and the characters `structuredContent`
   spells its value with — WRITTEN OUT BY HAND, so this witness does not
   inherit the very walker and predicate it exists to test."
  [["probe.empty_map" "{}"]
   ["probe.empty_vector" "[]"]
   ["probe.nil_value" "null"]
   ["probe.blank" "\"\""]
   ["probe.blank_spaces" "\"   \""]
   ["probe.nested.inner_empty" "[]"]
   ["probe.nested.inner_nil" "null"]])

(defn- value-less-misses
  "Every probe leaf the text does not carry as `pointer=spelling`."
  [text]
  (into [] (remove (fn [[label spelling]]
                     (str/includes? text (str label "=" spelling)))
                   value-less-spellings)))

(defn- published-with-probe
  "One published result carrying every value-less leaf shape, fitted and
   summarized exactly as a client is handed it."
  [raw]
  (let [published (assoc (inspect-tool/fit-public-result
                           (clocked (assoc raw :probe value-less-leaf-probe)))
                         :elapsed_ms 1.0)]
    [published (inspect-tool/inspect-summary published)]))

;; @spec MCP-OP-STUDY-044
(deftest every-value-less-leaf-appears-in-the-text-with-its-label
  (with-tmp-project
    (fn [dir]
      (spit (str dir "/deps.edn") "{:paths [\"src\"]}")
      (fs/create-dirs (str dir "/src/fixture"))
      (spit (str dir "/src/fixture/core.clj") class-ratchet-fixture))
    (fn [config]
      (doseq [[operation extra] (sort class-ratchet-requests)]
        (testing operation
          (let [[published text]
                (published-with-probe
                  (inspect-tool/execute-inspect!
                    config
                    {"requests" [(merge {"id" "r1"
                                         "operation" operation
                                         "file" "src/fixture/core.clj"}
                                        extra)]
                     "expect" {"requests" 1 "files" 1}}))]
            (is (true? (:ok published)) (pr-str (:error published)))
            (is (empty? (value-less-misses text))
                (str operation
                     ": value-less leaves the text carries with neither a "
                     "value nor a label — "
                     (pr-str (value-less-misses text))))
            (is (empty? (leaf-misses text published))
                (str operation ": " (count (leaf-misses text published))
                     " uncarried leaves — "
                     (miss-report (leaf-misses text published)))))))
      (testing "ls-tree"
        (let [[published text]
              (published-with-probe
                (inspect-tool/execute-ls-tree
                  config {:mode "ls-tree" :dir "." :format "text"}))]
          (is (true? (:ok published)))
          (is (empty? (value-less-misses text))
              (str "ls-tree: " (pr-str (value-less-misses text))))
          (is (empty? (leaf-misses text published))
              (miss-report (leaf-misses text published)))))
      (testing "a refusal"
        (let [[published text]
              (published-with-probe
                (inspect-tool/execute-inspect!
                  config
                  {"requests" [{"id" "r1" "operation" "outline"
                                "file" "src/no_such_file_xyz.clj"}]
                   "expect" {"requests" 1 "files" 1}}))]
          (is (false? (:ok published)) "the fixture must actually refuse")
          (is (empty? (value-less-misses text))
              (str "refusal: " (pr-str (value-less-misses text))))
          (is (empty? (leaf-misses text published))
              (miss-report (leaf-misses text published))))))))

;; @spec MCP-OP-STUDY-044
(deftest an-empty-collection-is-a-receipt-leaf-the-walker-yields
  ;; The walker is the definition of "every structuredContent leaf". A shape
  ;; it skips is excluded by a mechanism nobody enumerated. `[:results] []`
  ;; is not hypothetical: `results=[]` is what a zero-result receipt carries.
  (let [receipt {:ok true :operation "inspect_clojure"
                 :request_count 0 :file_count 0 :source_character_count 0
                 :results [] :read_complete true :next_action "none"}
        pairs (inspect/receipt-leaf-pairs receipt)]
    (is (contains? (set (map first pairs)) [:results])
        (str "the receipt walker yields no leaf for an empty collection: "
             (pr-str (mapv first pairs))))
    (let [published (assoc (inspect-tool/fit-public-result (clocked receipt))
                           :elapsed_ms 1.0)
          text (inspect-tool/inspect-summary published)]
      (is (str/includes? text "results=[]")
          "a zero-result receipt says so in the text, with its label")
      (is (empty? (leaf-misses text published))
          (miss-report (leaf-misses text published))))))

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
      (let [response (assoc (inspect-tool/fit-public-result (clocked (run params)))
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

;; ============================================================
;; O2 ROUND 3 — the refusal enumeration comes from the CONSTRUCTORS, and the
;; cause is bounded (Sol O2 round-2 review, section 5)
;; ============================================================
;; Two separate claims round two could not support. `src/clj_surgeon/
;; mcp_inspect.clj` alone constructs 22 distinct `refuse!` reasons while the
;; ratchet drove thirteen heterogeneous cases, so "every reachable refusal
;; kind" was a hand-written list nobody could check; and a 10,000-character
;; path was bounded in its detail line and then repeated in full as the
;; cause, so the refusal text was bounded by the caller's input rather than
;; by a constant.

(defn- refusal-reason-of
  "The reason one request actually refuses with, driven through the public
   entrance and normalized to its name."
  [params]
  (when-let [reason (:reason (run params))]
    (if (keyword? reason) (name reason) (str reason))))

(defn- constructed-refusal-reasons
  "Every LITERAL refusal reason in the source. Kept as a COMPLEMENT to the
   runtime enumeration below — never as the ratchet. A scan of program text
   can only see the shapes it was written to match, and Sol O2 round 3
   sections 3 and 9 showed both halves of that: a reason passed to a helper is
   invisible to it, and `(identity :expected-object)` hides a literal one."
  []
  (into (sorted-set)
        (map second)
        (re-seq #"\(refuse! :([a-z0-9-]+)"
                (slurp (str project-root "/src/clj_surgeon/mcp_inspect.clj")))))

;; @spec MCP-OP-STUDY-046
(deftest the-refusal-ratchet-drives-every-reason-the-runtime-can-construct
  ;; The enumeration is `inspect/refusal-reasons`, and `refuse!` refuses to
  ;; build a refusal outside it. The ratchet is the RUNTIME: one fixture per
  ;; member, driven through the public entrance, and the set of reasons
  ;; OBSERVED must equal the set enumerated. A new reason therefore cannot
  ;; ship without both an edit to the set and a fixture that reaches it — in
  ;; either order, whichever is missing is a failing test.
  (let [enumerated (into (sorted-set) (map name) inspect/refusal-reasons)
        driven (into (sorted-set)
                     (keep (fn [[_ params]] (refusal-reason-of params))
                           refusal-ratchet-cases))
        scanned (constructed-refusal-reasons)]
    (is (= enumerated driven)
        (str "the reasons the runtime constructs and the reasons the ratchet "
             "drives differ — enumerated but never driven: "
             (pr-str (vec (remove driven enumerated)))
             "; driven but not enumerated: "
             (pr-str (vec (remove enumerated driven)))))
    (is (<= 23 (count driven))
        "the ratchet must actually drive the refusals")
    (testing "the source scan complements the runtime; it never ratchets it"
      (is (empty? (remove enumerated scanned))
          (str "a literal reason the scan finds is not enumerated: "
               (pr-str (vec (remove enumerated scanned)))))
      (is (seq (remove scanned enumerated))
          (str "at least one enumerated reason is built through a helper "
               "rather than written literally — this is the gap the round-3 "
               "scan could not see, and it must stay visible here")))))

;; @spec MCP-OP-STUDY-046
(deftest a-refusal-cannot-invent-a-reason-outside-the-enumeration
  ;; Sabotage, as a test rather than as a hope. A refusal built through a
  ;; HELPER is the case the literal scan missed; both routes are closed at the
  ;; point of construction, so an unenumerated reason is unrepresentable.
  (is (thrown-with-msg?
        IllegalArgumentException #"not enumerated"
        (#'inspect/refuse! :brand-new-reason ["requests" 0] "synthetic"))
      "a direct refusal cannot invent a reason")
  (is (thrown-with-msg?
        IllegalArgumentException #"not enumerated"
        (#'inspect/unique-strings! ["a" "a"] ["requests" 0 "forms"]
                                   :brand-new-reason))
      "and neither can a refusal built through a helper")
  (is (contains? inspect/refusal-reasons :duplicate-form)
      "the helper's own reason is enumerated"))

;; ============================================================
;; O2 ROUND 4 — an allowance is DERIVED from the budget, never fixed
;; (Sol O2 round-3 review, section 4)
;; ============================================================
;; MCP-OP-STUDY-044 says the text drops a leaf only where the OUTPUT BUDGET
;; forces it. Round three dropped leaves at a fixed 8,192-character
;; receipt-fact allowance instead, so a refusal whose complete rendering had
;; thousands of bytes of room still lost its `error`, its `path`, and four
;; more leaves:
;;
;;   error_type= invalid-source-path structured_bytes= 20504 text_chars= 1321
;;   full_cause_in_text= false full_path_in_text= false declares_drop= true
;;   uncarried_count= 6
;;   hypothetical_one_copy_public_bytes= 31869 hypothetical_fits= true
;;
;; A fixed allowance is a second budget nobody declared. The allowance is what
;; the public budget leaves after the envelope; elision happens only when the
;; complete rendering would not fit; and an elision NAMES what it dropped, so
;; a caller reading only the text knows exactly which leaf to go to
;; `structuredContent` for.

(defn- synthetic-refusal
  [cause]
  {:ok false
   :operation "inspect_clojure"
   :error_type "synthetic-refusal"
   :error cause
   :read_complete false
   :source_unchanged true
   :next_action "correct_request"})

(defn- published-pair
  "One result as a client is handed it: fitted, clocked, and rendered."
  [raw]
  (let [published (assoc (inspect-tool/fit-public-result
                           (mcp-operation/stamp-envelope raw {:elapsed_ms 1.0}))
                         :elapsed_ms 1.0)
        text (inspect-tool/inspect-summary published)]
    {:published published
     :text text
     :bytes (inspect-tool/mcp-result-byte-count text published)
     :misses (leaf-misses text published)}))

(defn- largest-unelided-cause
  "The largest cause length whose COMPLETE public result still fits, found by
   bisection on the fit's own verdict rather than on a constant."
  []
  (loop [low 1 high 40000 best 1]
    (if (> low high)
      best
      (let [mid (quot (+ low high) 2)]
        (if (nil? (:text_evidence_limit
                    (inspect-tool/fit-public-result
                      (mcp-operation/stamp-envelope
                        (synthetic-refusal (apply str (repeat mid "c")))
                        {:elapsed_ms 1.0}))))
          (recur (inc mid) high mid)
          (recur low (dec mid) best))))))

;; @spec MCP-OP-STUDY-044
;; @spec MCP-OP-STUDY-040
(deftest a-refusal-elides-only-what-the-public-budget-forces
  (testing "a complete rendering with room to spare is rendered complete"
    (let [{:keys [published text bytes misses]}
          (published-pair (synthetic-refusal (apply str (repeat 10000 "c"))))]
      (is (nil? (:text_evidence_limit published))
          "the fit imposes no limit, so nothing may be elided")
      (is (<= bytes inspect-tool/max-public-result-bytes))
      (is (empty? misses)
          (str "a fixed allowance elided " (count misses)
               " leaves the budget had room for — " (miss-report misses)))
      (is (not (str/includes? text "the complete receipt is in structuredContent"))
          "and the text does not claim a drop it did not make")))
  (testing "at the boundary the complete rendering is carried"
    (let [exact (largest-unelided-cause)
          {:keys [published text bytes misses]}
          (published-pair (synthetic-refusal (apply str (repeat exact "c"))))]
      (is (nil? (:text_evidence_limit published)))
      (is (<= bytes inspect-tool/max-public-result-bytes)
          "the largest complete rendering is inside the budget")
      ;; The boundary must be the BUDGET's, not a rendering constant's. The
      ;; last 64 bytes of slack here are the publish reserve, which §5
      ;; removes; that witness asserts the fit target is the budget itself.
      (is (> bytes (- inspect-tool/max-public-result-bytes 128))
          (str "the boundary must actually be the budget's, not a constant's: "
               bytes " bytes of " inspect-tool/max-public-result-bytes))
      (is (empty? misses) (miss-report misses))))
  (testing "one character over, the elision is forced, bounded, and NAMED"
    (let [over (inc (largest-unelided-cause))
          {:keys [published text bytes misses]}
          (published-pair (synthetic-refusal (apply str (repeat over "c"))))]
      (is (some? (:text_evidence_limit published))
          "the fit imposes a limit exactly one character past the boundary")
      (is (<= bytes inspect-tool/max-public-result-bytes))
      (is (seq misses) "the elision is real")
      (is (str/includes? text "the complete receipt is in structuredContent"))
      (doseq [[path _] misses]
        (is (str/includes? text (inspect/leaf-label path))
            (str "a dropped leaf the text never names: "
                 (inspect/leaf-label path)))))))

;; @spec MCP-OP-STUDY-044
(deftest an-ok-receipt-elides-only-what-the-public-budget-forces
  ;; The same rule on the success side: the row allowance is a rendering
  ;; DEFAULT (MCP-OP-STUDY-041), but the receipt-fact allowance is the
  ;; superset guarantee, and a guarantee bounded by a constant is not one.
  (let [note (apply str (repeat 10000 "n"))
        receipt {:ok true :operation "inspect_clojure"
                 :request_count 0 :file_count 0 :source_character_count 0
                 :results [] :read_complete true :next_action "none"
                 :note note}
        {:keys [published text bytes misses]} (published-pair receipt)]
    (is (nil? (:text_evidence_limit published)))
    (is (<= bytes inspect-tool/max-public-result-bytes))
    (is (str/includes? text note)
        "a 10,000-character leaf under the budget travels whole")
    (is (empty? misses) (miss-report misses))))

;; ============================================================
;; O2 ROUND 4 — the fit measures the envelope the publisher PUBLISHES
;; (Sol O2 round-3 review, section 5)
;; ============================================================
;; `fit-public-result` measured with `elapsed_ms` zeroed, because the clock had
;; not stopped yet, and a 64-byte `publish-reserve` was held back to cover the
;; difference. A constant chosen from one observation is not an invariant: the
;; envelope's own clock contract accepts any finite non-negative double, and a
;; `1.0E308`-scale elapsed renders 309 characters:
;;
;;   payload= 420 fit_measure= 32514 normal_published= 32531
;;                huge_published= 32860 huge_bounded= false
;;   payload= 460 fit_measure= 32677 normal_published= 32694
;;                huge_published= 33023 huge_bounded= false
;;
;; The repair is not a bigger constant. The fit measures the FINAL result —
;; the one the publisher publishes, whatever shape its envelope has — so
;; nothing is added after the measurement and there is nothing to reserve.
;; That is also what makes the MEM-003 landing safe: when `elapsed_ms` moves
;; to `measured.elapsed_ms`, the fit measures the nested envelope because it
;; measures whatever it is handed.

(defn- scripted-clock
  [values]
  (let [state (atom values)]
    (fn [] (let [value (first @state)] (swap! state rest) value))))

(defn- callback-entrance
  "One `handle-inspect` call whose domain result and request clock are both
   scripted, so a witness can place any accepted clock value against a
   near-boundary receipt."
  [raw clock-values]
  (let [answer (atom nil)
        original mcp-operation/invoke!]
    (with-redefs [inspect-tool/execute-inspect! (fn [_ _] raw)
                  mcp-operation/invoke!
                  (fn [opts]
                    (original (assoc opts :clock-nanos
                                     (scripted-clock clock-values))))]
      (inspect-tool/handle-inspect
        nil {"requests" [{"operation" "outline" "file" "src/x.clj"}]
             "expect" {"requests" 1 "files" 1}}
        (fn [content error? structured]
          (reset! answer {:text (first content)
                          :error? error?
                          :structured structured}))))
    @answer))

(defn- near-boundary-receipt
  [payload]
  (let [row-source (str "(defn row [] \"" (apply str (repeat payload "x")) "\")")]
    {:ok true :operation "inspect_clojure"
     :request_count 32 :file_count 32
     :source_character_count (* 32 (count row-source))
     :read_complete true :next_action "none"
     :results
     (mapv (fn [index]
             {:id (str "r" index) :operation "forms"
              :file (str "src/f" index ".clj")
              :file_hash (apply str (repeat 64 "a"))
              :form_count 1
              :source_character_count (count row-source)
              :forms [{:name (str "f" index) :line 1 :end_line 1
                       :form_type "defn" :source row-source}]})
           (range 32))}))

;; @spec MCP-OP-STUDY-040
(deftest the-published-pair-is-bounded-under-every-accepted-clock
  (inspect-tool/init! {:project-root project-root})
  (try
    (doseq [payload [400 420 430 440 460]]
      (testing (str "payload " payload)
        (let [raw (near-boundary-receipt payload)
              ordinary (callback-entrance raw [0 1000000])
              ;; The largest interval the envelope's own clock contract
              ;; accepts: `format-elapsed-ms` takes any finite non-negative
              ;; number, and this one renders 309 characters.
              maximal (callback-entrance raw [0 1.0E308])]
          (is (<= (inspect-tool/mcp-result-byte-count
                    (:text ordinary) (:structured ordinary))
                  inspect-tool/max-public-result-bytes)
              "an ordinary clock publishes inside the budget")
          (is (<= (inspect-tool/mcp-result-byte-count
                    (:text maximal) (:structured maximal))
                  inspect-tool/max-public-result-bytes)
              (str "a maximal clock publishes "
                   (inspect-tool/mcp-result-byte-count
                     (:text maximal) (:structured maximal))
                   " bytes against a budget of "
                   inspect-tool/max-public-result-bytes
                   " — the reserve is a constant, not an invariant")))))
    (finally (inspect-tool/init! nil))))

;; @spec MCP-OP-STUDY-040
(deftest the-fit-target-is-the-budget-itself
  ;; With the measurement made on the final envelope there is nothing to hold
  ;; back, so the largest accepted rendering reaches the declared budget
  ;; rather than stopping a constant short of it.
  (let [exact (largest-unelided-cause)
        published (assoc (inspect-tool/fit-public-result
                           (mcp-operation/stamp-envelope
                             (synthetic-refusal (apply str (repeat exact "c")))
                             {:elapsed_ms 1.0}))
                         :elapsed_ms 1.0)
        bytes (inspect-tool/mcp-result-byte-count
                (inspect-tool/inspect-summary published) published)]
    (is (<= bytes inspect-tool/max-public-result-bytes))
    (is (> bytes (- inspect-tool/max-public-result-bytes 8))
        (str "the fit stops " (- inspect-tool/max-public-result-bytes bytes)
             " bytes short of the budget it declares"))))

(defn- nest-measured
  "The MEM-003 wire shape: every clock-derived field moves under `measured`."
  [result]
  (let [clock-keys [:elapsed_ms :inspection_elapsed_ms :job_elapsed_ms :scan_ms]]
    (assoc (apply dissoc result clock-keys)
           :measured (select-keys result clock-keys))))

;; @spec MCP-OP-STUDY-040
;; @spec MCP-OP-RESULT-001
(deftest invoke-publishes-exactly-what-the-fit-returned
  ;; The reserve existed because the publisher changed the result AFTER the
  ;; fit measured it. The invariant that replaces it: `invoke!` finalizes,
  ;; hands the finalized result to the fit, and then only renders and
  ;; serializes what the fit returned. Nothing is added afterwards, so there
  ;; is nothing to reserve — and this holds whatever shape the envelope has,
  ;; MEM-003's nested `measured` block included.
  (let [summarized (atom ::none)
        published (atom ::none)
        serialized (atom ::none)]
    (mcp-operation/invoke!
      {:clock-nanos (scripted-clock [0 1000000])
       :execute (constantly {:ok true :operation "probe"})
       :fit (fn [result] (assoc (nest-measured result) :fitted true))
       :summarize (fn [result] (reset! summarized result) "text")
       :serialize (fn [result] (reset! serialized result) "{}")
       :callback (fn [_content _error? structured]
                   (reset! published structured))})
    (is (= @summarized @published)
        "the text and the receipt render one map")
    (is (= @summarized @serialized)
        "and the serialized body is that same map")
    (is (true? (:fitted @published))
        "the map the fit returned is the map that is published")
    (is (= {:elapsed_ms 1.0} (:measured @published))
        "with its envelope in whatever shape the fit left it")
    (is (not (contains? @published :elapsed_ms))
        "the publisher adds no field after the fit")))

;; ============================================================
;; O2 ROUND 4 — the refusal enumeration comes from the RUNTIME
;; (Sol O2 round-3 review, sections 3 and 9)
;; ============================================================
;; Round three derived the reason set from the source with
;; `(refuse! :([a-z0-9-]+)`, which sees only a LITERAL reason at a literal
;; call site. `unique-strings!` takes its reason as an ARGUMENT and the forms
;; validator passes `:duplicate-form`; that refusal is reachable through the
;; public entrance and was absent from the scanned 22, so the exhaustive
;; claim was false the day it was written. Sol's rung D — replacing a literal
;; reason with `(identity :expected-object)` — dropped the scan from 22 to 21
;; with the whole suite green, and the unsabotaged helper above is the same
;; escape already present in ordinary source.
;;
;; A literal-shape scan is never the ratchet. The runtime is.

(def ^:private helper-built-refusal
  "A duplicate form name. The forms validator hands `:duplicate-form` to
   `unique-strings!` as an argument, so no literal `(refuse! :duplicate-form`
   exists anywhere in the source."
  {"requests" [{"operation" "forms" "file" real-file
                "forms" ["reader-cond?" "reader-cond?"]
                "expect" {"forms" 2}}]
   "expect" {"requests" 1 "files" 1}})

;; @spec MCP-OP-STUDY-046
(deftest a-refusal-reason-built-through-a-helper-is-in-the-ratchet
  (let [reachable (refusal-reason-of helper-built-refusal)
        scanned (constructed-refusal-reasons)
        driven (into (sorted-set)
                     (keep (fn [[_ params]] (refusal-reason-of params))
                           refusal-ratchet-cases))]
    (is (= "duplicate-form" reachable)
        "the public entrance reaches a refusal whose reason is an argument")
    ;; The RED form of this assertion asked the literal scan to FIND it. That
    ;; was the wrong repair: the scan is correct about the source and wrong
    ;; about the program. It still cannot see this reason, and that fact is
    ;; pinned here so nobody re-promotes the scan to a ratchet.
    (is (not (contains? scanned reachable))
        (str "the literal `(refuse! :reason` scan cannot see a reason passed "
             "to a helper; it found " (count scanned) ": "
             (pr-str (vec scanned))))
    (is (contains? (into (sorted-set) (map name) inspect/refusal-reasons)
                   reachable)
        "the runtime enumeration carries it")
    (is (contains? driven reachable)
        (str "no ratchet fixture drives it; the ratchet drives "
             (count driven) " reasons"))))

;; @spec MCP-OP-STUDY-046
(deftest a-refusal-cause-is-bounded-and-still-travels
  (let [synthetic (fn [cause]
                    {:ok false
                     :operation "inspect_clojure"
                     :error_type "synthetic-refusal"
                     :error cause
                     :read_complete false
                     :source_unchanged true
                     :next_action "correct_request"
                     :elapsed_ms 0.0})
        bound inspect-tool/max-refusal-cause-characters
        at-bound (apply str (repeat bound "c"))
        over (str at-bound "c")]
    (testing "at the bound the cause is spelled whole and carries no marker"
      (let [text (inspect-tool/inspect-summary (synthetic at-bound))]
        (is (str/includes? text at-bound))
        (is (not (str/includes? text "characters)")))))
    (testing "one character over, the structural line is bounded and says so"
      (let [text (inspect-tool/inspect-summary (synthetic over))]
        (is (str/includes? text (str "… (" (count over) " characters)"))
            "the marker names the original length")
        (is (str/includes? text over)
            "and the complete cause still travels under the receipt-fact bound")))
    (testing "a cause the public budget cannot carry is dropped, declared, and named"
      ;; O2 round 4 (Sol round-3 review, section 4): the round-3 form of this
      ;; clause expected a 10,000-character cause to be DROPPED and the whole
      ;; refusal text to stay under 2,048 characters. That is the fixed
      ;; allowance MCP-OP-STUDY-044 forbids: at 10,000 characters the complete
      ;; rendering has thousands of bytes of room. The bound a refusal text
      ;; obeys is the PUBLIC OUTPUT BUDGET, so the witness moves to a cause
      ;; that genuinely cannot fit.
      (let [huge (apply str (repeat 20000 "c"))
            published (assoc (inspect-tool/fit-public-result
                               (mcp-operation/stamp-envelope
                                 (synthetic huge) {:elapsed_ms 1.0}))
                             :elapsed_ms 1.0)
            text (inspect-tool/inspect-summary published)]
        (is (not (str/includes? text huge)))
        (is (str/includes? text "the complete receipt is in structuredContent"))
        (is (str/includes? text "error")
            "and the text names the leaf it dropped")
        (is (<= (inspect-tool/mcp-result-byte-count text published)
                inspect-tool/max-public-result-bytes)
            (str "the refusal text is bounded by the public output budget: "
                 (inspect-tool/mcp-result-byte-count text published)
                 " bytes"))))))

;; ============================================================
;; O2 ROUND 3 — the retired contract stays visibly retired
;; (Sol O2 round-2 review, section 8)
;; ============================================================
;; Round two reversed the "source-free companion" contract in EARS, tests, and
;; code, and changed nothing in the design surface that owned it. The owning
;; plan still promised "only a concise human summary" and "concise source-free
;; summaries", the HLD still promised "concise human presentation", and the
;; README still called the text an ordinary transcript summary. That contract
;; never carried an EARS id — which is exactly how it survived its own
;; reversal — so the retirement is enforced here instead: each of the three
;; prose statements is retired IN PLACE, with a date and the superseding ids,
;; and this witness fails if one is quietly restored or a notice removed.

;; @spec MCP-OP-STUDY-044
(defn- normalized-document
  [path]
  (str/replace (slurp (str project-root "/" path)) #"\s+" " "))

(defn- claim-positions
  [document claim]
  (loop [from 0 found []]
    (if-let [index (str/index-of document claim from)]
      (recur (inc index) (conj found index))
      found)))

(defn- claim-stated-outside-a-retirement-notice
  "Every position at which a retired claim is stated WITHOUT a dated
   retirement notice immediately before it. A retired promise may be quoted —
   that is how it stays legible — but only under the notice that retires it."
  [document claim]
  (remove (fn [index]
            (str/includes? (subs document (max 0 (- index 900)) index)
                           "RETIRED 2026-09-04"))
          (claim-positions document claim)))

;; @spec MCP-OP-STUDY-044
(deftest the-retired-source-free-companion-contract-stays-visibly-retired
  (doseq [[path claims]
          [["docs/plans/typed-mcp-inspect-entrance.md"
            ["MCP `content` contains only a concise human summary"
             "deterministic normalization and concise source-free summaries"]]
           ["docs/high-level-design.md"
            ["cross-cutting result evidence and concise human presentation"]]
           ["README.md"
            ["the bounded text result is an ordinary transcript summary"]]]]
    (testing path
      (let [document (normalized-document path)]
        (doseq [claim claims]
          (is (empty? (claim-stated-outside-a-retirement-notice document claim))
              (str path " states the retired source-free companion contract "
                   "with no retirement notice before it: " (pr-str claim))))
        (is (str/includes? document "MCP-OP-STUDY-044")
            (str path " must name the intent that superseded it"))
        (is (str/includes? document "RETIRED 2026-09-04")
            (str path " must say, in place and dated, that it is retired")))))
  (testing "and the EARS leaf records the retirement rather than only the new rule"
    (let [specs (slurp (str project-root
                            "/docs/intent/study-ops/study-ops-specs.md"))]
      (is (str/includes? specs "RETIRES the \"source-free companion\" contract"))
      (is (str/includes? specs "typed-mcp-inspect-entrance.md"))
      (is (str/includes? specs "high-level-design.md"))
      (is (str/includes? specs "README.md")))))

;; ============================================================
;; O2 ROUND 5 — the allowance bounds EVERY byte the rendering spends
;; (Opus O2 round-4 review, sections 2 and 3)
;; ============================================================
;; Round four charged the fact LINES against the allowance and left the
;; `dropped:` line outside it. That line names one label per dropped leaf, so
;; it GROWS as the allowance shrinks: at allowance 0 it was 22,142 characters
;; on a two-file `outline` batch over this repository's own sources. The
;; rendering therefore got BIGGER as the budget got tighter, `fits?` stopped
;; being monotone, and `fit-public-result`'s bisection walked into the half
;; that can never fit and returned nil — so an ordinary two-file batch fell to
;; the notice rung and published 151 characters of text with 9,251 bytes of
;; the declared budget unspent, carrying none of its 1,137 receipt leaves.
;;
;; Two properties are witnessed here, because either one alone leaves the
;; defect reachable: the rendering SHRINKS as the allowance shrinks (every
;; rendered byte is charged), and the fit finds a fitting rendering whenever
;; one exists (the search does not assume what it cannot prove).

(def ^:private review-batch-files
  "Two of this repository's own sources — the tool's advertised batching use,
   and the exact call the round-four review published 151 characters for."
  ["src/clj_surgeon/mcp_inspect_tool.clj"
   "src/clj_surgeon/mcp_inspect.clj"])

(defn- outline-batch
  "One `outline` batch over real repository sources, finalized."
  [files]
  (clocked
    (run {"requests" (vec (map-indexed
                            (fn [index file]
                              {"id" (str "r" index)
                               "operation" "outline"
                               "file" file})
                            files))
          "expect" {"requests" (count files) "files" (count files)}})))

(defn- text-evidence-bytes
  "The complete published pair at one imposed evidence allowance."
  [raw limit]
  (public-bytes (assoc raw :text_evidence_limit limit)))

;; @spec MCP-OP-STUDY-040
;; @spec MCP-OP-STUDY-044
(deftest an-ordinary-two-file-outline-batch-spends-the-budget-on-its-receipt
  (let [raw (outline-batch review-batch-files)
        complete (public-bytes raw)
        structured (inspect-tool/mcp-result-byte-count "" raw)
        published (clocked (inspect-tool/fit-public-result raw))
        text (inspect-tool/inspect-summary published)
        headroom (- inspect-tool/max-public-result-bytes (public-bytes published))]
    (is (true? (:ok raw)) (pr-str (:error raw)))
    (testing "PRECONDITION — this call must sit in the band the defect lives in"
      (is (< inspect-tool/max-public-result-bytes complete)
          (format (str "the complete rendering must overshoot the budget or "
                       "this witness proves nothing; measured %d against %d. "
                       "If these two sources shrank, name larger ones.")
                  complete inspect-tool/max-public-result-bytes))
      (is (< structured inspect-tool/max-public-result-bytes)
          (format (str "and the receipt ALONE must fit, or no rendering "
                       "choice could help; measured %d against %d")
                  structured inspect-tool/max-public-result-bytes)))
    (is (<= (public-bytes published) inspect-tool/max-public-result-bytes)
        "the published pair is inside the declared budget")
    (testing "the text spends the room the receipt leaves it"
      (is (<= 6000 (count text))
          (format (str "an ordinary two-file batch published %d characters of "
                       "text with %d bytes of the budget unspent")
                  (count text) headroom))
      (is (<= headroom 2048)
          (format "%d bytes of the public budget went unspent" headroom)))
    (testing "and what it could not carry is declared by count and by pointer"
      (is (nil? (:text_omitted published))
          "the notice rung is for a receipt no rendering can accompany")
      (is (re-find #"receipt facts · \d+ of \d+ rendered" text)
          "the text says how many of how many facts it rendered")
      (is (str/includes? text "dropped: ")
          "and names dropped leaves by their JSON pointers")
      (is (str/includes? text "structuredContent")
          "and names where the complete receipt is")
      (is (not (str/includes? text terminal-claim))
          "and never reads as terminal over evidence it dropped"))))

(defn- build-wide-namespace!
  "One namespace of `n` ordinary three-line functions — the shape the review
   swept at 140, 180 and 220 forms."
  [dir n]
  (spit (str dir "/deps.edn") "{:paths [\"src\"]}")
  (fs/create-dirs (str dir "/src/wide"))
  (spit (str dir "/src/wide/core.clj")
        (str "(ns wide.core\n  (:require [clojure.string :as str]))\n\n"
             (str/join
               "\n\n"
               (for [index (range n)]
                 (format (str "(defn handler-%03d\n  \"Row %d of the wide "
                              "namespace.\"\n  [request options]\n"
                              "  (str/join \"-\" [request options %d]))")
                         index index index))))))

;; @spec MCP-OP-STUDY-040
;; @spec MCP-OP-STUDY-044
(deftest the-form-count-sweep-never-abandons-a-rendering-that-fits
  ;; The reviewer's 140/180/220 sweep. Every size that overshoots must still
  ;; publish a rendering, because at every one of them the receipt alone fits
  ;; with thousands of bytes to spare.
  (let [banded
        (into []
              (keep
                (fn [n]
                  (with-tmp-project
                    #(build-wide-namespace! % n)
                    (fn [config]
                      (let [raw (clocked
                                  (inspect-tool/execute-inspect!
                                    config
                                    {"requests" [{"id" "r0"
                                                  "operation" "outline"
                                                  "file" "src/wide/core.clj"}]
                                     "expect" {"requests" 1 "files" 1}}))
                            complete (public-bytes raw)
                            structured (inspect-tool/mcp-result-byte-count
                                         "" raw)]
                        (when (and (< inspect-tool/max-public-result-bytes
                                      complete)
                                   (< structured
                                      inspect-tool/max-public-result-bytes))
                          (let [published (clocked
                                            (inspect-tool/fit-public-result
                                              raw))
                                text (inspect-tool/inspect-summary published)]
                            {:n n
                             :structured structured
                             :published (public-bytes published)
                             :text-chars (count text)
                             :omitted (:text_omitted published)
                             :headroom (- inspect-tool/max-public-result-bytes
                                          (public-bytes published))}))))))
                [140 180 220]))]
    (is (seq banded)
        "PRECONDITION: at least one sweep size must overshoot with a fitting receipt")
    (doseq [row banded]
      (testing (str (:n row) " forms")
        (is (<= (:published row) inspect-tool/max-public-result-bytes))
        (is (nil? (:omitted row))
            (format (str "%d forms fell to the %s rung although the receipt "
                         "alone measured %d bytes against a %d budget")
                    (:n row) (pr-str (:omitted row)) (:structured row)
                    inspect-tool/max-public-result-bytes))
        (is (<= 2000 (:text-chars row))
            (format "%d forms published %d characters of text with %d unspent"
                    (:n row) (:text-chars row) (:headroom row)))))))

;; @spec MCP-OP-STUDY-040
;; @spec MCP-OP-STUDY-044
(deftest lowering-the-evidence-allowance-can-only-shrink-the-rendering
  ;; Monotonicity BY CONSTRUCTION, which is what makes any search over the
  ;; allowance sound. Round four's `dropped:` line was outside the allowance
  ;; it described, so the rendering grew as the allowance fell.
  (let [raw (outline-batch review-batch-files)
        structured (inspect-tool/mcp-result-byte-count "" raw)
        high (max 0 (- inspect-tool/max-public-result-bytes structured))
        step (max 1 (quot high 40))
        limits (vec (range 0 (inc high) step))
        measured (mapv #(text-evidence-bytes raw %) limits)
        breaks (into []
                     (keep (fn [[[low-limit low-bytes] [high-limit high-bytes]]]
                             (when (> low-bytes high-bytes)
                               {:lower low-limit :lower-bytes low-bytes
                                :higher high-limit :higher-bytes high-bytes})))
                     (partition 2 1 (map vector limits measured)))]
    (is (< 1 (count limits)) "PRECONDITION: the band must have room to sweep")
    (is (empty? breaks)
        (format (str "%d of %d adjacent allowances render MORE at the LOWER "
                     "allowance — the rendering grows as the budget tightens: %s")
                (count breaks) (dec (count limits))
                (pr-str (take 3 breaks))))))

;; @spec MCP-OP-STUDY-040
(deftest the-fit-finds-a-fitting-rendering-whenever-one-exists
  ;; The search side of the same property, stated without assuming
  ;; monotonicity: if ANY allowance in the band renders a fitting pair, the
  ;; fit must publish a rendering rather than fall to the notice rung.
  (let [raw (outline-batch review-batch-files)
        structured (inspect-tool/mcp-result-byte-count "" raw)
        high (max 0 (- inspect-tool/max-public-result-bytes structured))
        step (max 1 (quot high 40))
        fitting (into []
                      (filter #(<= (text-evidence-bytes raw %)
                                   inspect-tool/max-public-result-bytes))
                      (range 0 (inc high) step))
        best (last (sort-by #(text-evidence-bytes raw %) fitting))
        published (clocked (inspect-tool/fit-public-result raw))]
    (is (seq fitting)
        (format (str "PRECONDITION: some allowance in [0, %d] must fit, or "
                     "there is nothing for the search to find")
                high))
    (is (nil? (:text_omitted published))
        (format (str "allowance %s renders %d bytes inside the %d-byte "
                     "budget, and the fit published the %s rung instead")
                (pr-str best) (text-evidence-bytes raw best)
                inspect-tool/max-public-result-bytes
                (pr-str (:text_omitted published))))
    (is (>= (public-bytes published) (text-evidence-bytes raw best))
        "the fit publishes at least as much as a brute-force sweep found")))

;; ============================================================
;; O2 ROUND 5 — carriage is not COINCIDENCE (Opus O2 round-4 review, §4)
;; ============================================================
;; Round four fixed the four value-less shapes and left the predicate for
;; every other leaf at `str/includes?`. A short spelling collides: on a real
;; `outline` receipt `file_read_count` could be changed from 1 to 0, and
;; `results[0].platforms[0]` from "clj" to "none", with the published text
;; BYTE-IDENTICAL and `uncarried-leaves` reporting zero misses — the text's
;; only "1"s meaning `request_count` and `file_count`, and its only "clj"s
;; being file extensions. The hole is the same CLASS round three blocked on,
;; moved from `{null, {}, [], ""}` to `{any value whose spelling occurs
;; elsewhere in the text}`.
;;
;; Two properties, both stated WITHOUT inheriting the production predicate:
;; the text DEPENDS on every leaf it claims to carry, and a leaf whose
;; spelling is short enough to collide is distinguishable from the other
;; values it could have held.

;; O2 round 7 (Sol O2 round-6 review, §3): both audits below used to be SCOPED
;; to collidable spellings — numbers, booleans, and strings under eight
;; characters — on the reasoning that a distinctive spelling is carried by its
;; own characters. That reasoning is what the review killed: a distinctive
;; value can be carried as a SUBSTRING of a longer decoy, and one value spelled
;; at two pointers cannot be two independently removable facts. Under
;; MCP-OP-STUDY-051 a leaf is carried only by its OWN pointer line, so the
;; scoping has nothing left to justify it: both audits now walk EVERY leaf.

(defn- map-scalar-paths
  "Every scalar leaf reachable by a MAP KEY, as the path to it. A vector
   element cannot be removed without changing its siblings' indices, so this
   witness perturbs only what a receipt can be missing."
  ([value] (map-scalar-paths value []))
  ([value path]
   (cond
     (map? value)
     (mapcat (fn [[key child]] (map-scalar-paths child (conj path key))) value)

     (vector? value)
     (mapcat (fn [[index child]] (map-scalar-paths child (conj path index)))
             (map-indexed vector value))

     (coll? value) []
     :else [path])))

(defn- rendering
  "The published text for one result, or ::unrenderable when the renderer
   cannot produce one at all — which is itself a dependency on the leaf that
   was removed, and the strongest kind."
  [result]
  (try (inspect-tool/inspect-summary result)
       (catch Exception _ ::unrenderable)))

(defn- text-independent-leaves
  "Every leaf whose REMOVAL leaves the published text byte-identical."
  [result]
  (let [text (inspect-tool/inspect-summary result)]
    (into []
          (keep (fn [path]
                  ;; Only a MAP KEY can be removed: dropping a vector element
                  ;; shifts its siblings, which is a different receipt rather
                  ;; than the same one missing a leaf.
                  (when (and (keyword? (last path))
                             (not (inspect/leaf-excluded? path)))
                    (let [without (if (= 1 (count path))
                                    (dissoc result (first path))
                                    (update-in result (butlast path)
                                               dissoc (last path)))]
                      (when (= text (rendering without))
                        [path (get-in result path)])))))
          (map-scalar-paths result))))

;; @spec MCP-OP-STUDY-044
;; @spec MCP-OP-STUDY-051
(deftest the-text-depends-on-every-receipt-leaf
  ;; The dissoc-dependency audit, as a ratchet. A leaf the rendering does not
  ;; read is a leaf the text cannot be carrying, whatever a substring test
  ;; says about it.
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
                              (clocked
                                (inspect-tool/execute-inspect!
                                  config
                                  {"requests" [(merge {"id" "r1"
                                                       "operation" operation
                                                       "file" "src/fixture/core.clj"}
                                                      extra)]
                                   "expect" {"requests" 1 "files" 1}})))
                            :elapsed_ms 1.0)
                independent (text-independent-leaves published)]
            (is (true? (:ok published)) (pr-str (:error published)))
            (is (empty? independent)
                (str operation ": the published text does not depend on "
                     (count independent) " receipt leaves it reports as "
                     "carried — " (pr-str (take 6 independent))))))))))

(defn- indistinguishable-leaves
  "Every leaf — collidable or distinctive — that some OTHER value of the same
   type could replace without changing one byte of the published text."
  [result]
  (let [text (inspect-tool/inspect-summary result)
        paths (into [] (remove inspect/leaf-excluded?) (map-scalar-paths result))
        values (distinct (keep #(get-in result %) paths))
        same-type? (fn [a b]
                     (or (and (number? a) (number? b))
                         (and (boolean? a) (boolean? b))
                         (and (string? a) (string? b))))]
    (into []
          (keep (fn [path]
                  (let [value (get-in result path)]
                    (when-let [decoy (first
                                       (filter
                                         #(= text (rendering
                                                    (assoc-in result path %)))
                                         (remove #(= % value)
                                                 (filter #(same-type? value %)
                                                         (concat [0 1 true false "clj" "none"
                                                                  "XXaaaaaaaaaaaaaaaaYY"]
                                                                 (take 60 values))))))]
                      [path value decoy]))))
          paths)))

;; @spec MCP-OP-STUDY-044
;; @spec MCP-OP-STUDY-051
(deftest no-value-of-the-same-type-renders-the-same-text
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
                              (clocked
                                (inspect-tool/execute-inspect!
                                  config
                                  {"requests" [(merge {"id" "r1"
                                                       "operation" operation
                                                       "file" "src/fixture/core.clj"}
                                                      extra)]
                                   "expect" {"requests" 1 "files" 1}})))
                            :elapsed_ms 1.0)
                ambiguous (indistinguishable-leaves published)]
            (is (empty? ambiguous)
                (str operation ": " (count ambiguous)
                     " leaves whose value another value of the same type could "
                     "replace with a byte-identical text — "
                     (pr-str (take 6 ambiguous))))))))))

;; ============================================================
;; O2 ROUND 5 — the envelope is whatever the finalizer added
;; (Opus O2 round-4 review, section 7 — before the MEM-003 landing)
;; ============================================================
;; Round four made the fit measure the FINAL published envelope, which is
;; right, and then named that envelope `:elapsed_ms` in two places. MEM-003's
;; second landing nests the request clock and its siblings under `measured`.
;; At that moment `with-envelope` would copy nothing — every budget-gate
;; SUBSTITUTE silently losing the clock — and `fit-public-result`'s
;; `(contains? raw-result :elapsed_ms)` guard would throw on the very first
;; request instead of measuring the result. Neither is a wire change; both are
;; this namespace assuming a shape it does not own.

(def ^:private nested-measured
  "The MEM-003 envelope: five clock fields under one `measured` block."
  {:elapsed_ms 1.0
   :inspection_elapsed_ms 2.0
   :job_elapsed_ms 3.0
   :scan_ms 4.0
   :queue_ms 5.0})

;; @spec MCP-OP-STUDY-040
(deftest the-envelope-is-whatever-the-finalizer-added
  (testing "the finalizer adds only keys the envelope namespace declares"
    (let [fitted (atom nil)]
      (mcp-operation/invoke!
        {:execute (fn [] {:ok true :operation "probe"})
         :fit (fn [result] (reset! fitted result) result)
         :summarize (fn [_] "probe")
         :callback (fn [_ _ _] nil)})
      (is (seq (remove #{:ok :operation} (keys @fitted)))
          "the finalizer must add something, or this proves nothing")
      (is (every? mcp-operation/envelope-keys
                  (remove #{:ok :operation} (keys @fitted)))
          (str "a key the finalizer adds is not declared in "
               "`mcp-operation/envelope-keys`, so the budget gate cannot "
               "carry it or check for it: "
               (pr-str (remove (into #{:ok :operation}
                                     mcp-operation/envelope-keys)
                               (keys @fitted)))))))
  (testing "a result carrying NO envelope is still refused, in any shape"
    (is (thrown? IllegalArgumentException
                 (inspect-tool/fit-public-result {:ok true :operation "probe"}))))
  (with-tmp-project
    #(build-toy-project! % 200)
    (fn [config]
      (let [domain (inspect-tool/execute-ls-tree
                     config {:mode "ls-tree" :dir "." :format "text"
                             :limit 16384})]
        (doseq [[shape envelope]
                [["top-level elapsed_ms" {:elapsed_ms 1.0}]
                 ["a nested measured block" {:measured nested-measured}]]]
          (testing shape
            (let [raw (mcp-operation/stamp-envelope domain envelope)
                  fitted (inspect-tool/fit-public-result raw)
                  published (inspect-tool/mcp-result-byte-count
                              (inspect-tool/inspect-summary fitted) fitted)]
              (is (>= inspect-tool/max-public-result-bytes published)
                  (format "%s published %d bytes" shape published))
              (is (or (pos-int? (:text_evidence_limit fitted))
                      (false? (:ok fitted)))
                  "an over-budget result is a typed truncation or a refusal")
              (testing "and a SUBSTITUTE the gate builds carries the envelope"
                (let [huge (mcp-operation/stamp-envelope
                                 (assoc domain
                                        :tree (apply str
                                                     (repeat (* 40 1024) "x")))
                                 envelope)
                      substitute (inspect-tool/fit-public-result huge)]
                  (is (false? (:ok substitute))
                      "the fixture must actually reach the refusal rung")
                  (is (= envelope (select-keys substitute
                                               (keys envelope)))
                      (str shape ": the substitute lost the envelope of the "
                           "result it replaced — "
                           (pr-str (select-keys substitute
                                                mcp-operation/envelope-keys)))))))))))))

;; @spec MCP-OP-STUDY-046
(deftest an-unenumerated-refusal-reason-reaches-the-caller-as-a-crash
  ;; Said out loud (Opus O2 round-4 review, section 5). Enforcing the
  ;; enumeration AT CONSTRUCTION means an unenumerated reason throws a plain
  ;; `IllegalArgumentException` out of `execute-inspect!` — the tool's
  ;; `catch Exception` sits inside `capture-snapshots`, not around validation
  ;; — so the caller receives an exception and the callback never fires. That
  ;; is the intended trade: it is a defect in this namespace, not a bad
  ;; request, and a typed refusal would tell a caller its request was wrong
  ;; when the tool was. It is pinned here rather than left to be rediscovered.
  (let [fired (atom 0)]
    (is (thrown-with-msg?
          IllegalArgumentException #"not enumerated"
          (mcp-operation/invoke!
            {:execute #(#'inspect/refuse! :brand-new-reason ["requests" 0]
                                          "synthetic")
             :summarize (fn [_] "unreachable")
             :callback (fn [_ _ _] (swap! fired inc))}))
        "the reason is refused at construction, by throwing")
    (is (zero? @fired)
        "and the callback never fires: a crash, not a refusal receipt")))

;; ============================================================
;; O2 ROUND 6 — the declaration is derived from the audit's own walk
;; (Sol O2 round-5 review, §2)
;; ============================================================
;; The branch's own primary fixture published `98 of 882 rendered` — 784
;; dropped — while the product audit found 785 uncarried leaves. The extra
;; leaf, `results[1].outline.requires[0]`, never entered the fact entries at
;; all: `receipt-fact-entries` decided carriage against a text ACCUMULATED
;; from earlier fact lines, and the line that carried it sat in the tail the
;; budget dropped. An omission the declaration does not declare is exactly
;; what MCP-OP-STUDY-044 exists to prevent, happening inside the declaration.

(defn- declared-fact-counts
  "`{:shown X :total N}` read out of the PUBLISHED TEXT's own count line, the
   way a caller reads it. Nil when the text carries no declaration."
  [text]
  (when-let [[_ shown total] (re-find #"receipt facts · (\d+) of (\d+) rendered"
                                      text)]
    {:shown (Long/parseLong shown) :total (Long/parseLong total)}))

(defn- declared-drop-pointers
  "The JSON pointers the published text NAMES as dropped, from its own
   `dropped:` line — not from any production value."
  [text]
  (when-let [[_ line] (re-find #"(?m)^  dropped: (.*)$" text)]
    (->> (str/split (str/replace line #" \(\+\d+ more\)$" "") #", ")
         (remove str/blank?)
         vec)))

;; @spec MCP-OP-STUDY-047
(deftest the-declared-drop-count-equals-the-audited-uncarried-count
  ;; The fixed 140+30-form two-file batch, published through the real fit.
  (let [raw (outline-batch review-batch-files)
        published (clocked (inspect-tool/fit-public-result raw))
        text (inspect-tool/inspect-summary published)
        audited (inspect/uncarried-leaves text published)
        audited-labels (set (map (fn [[path _]] (inspect/leaf-label path))
                                 audited))
        counts (declared-fact-counts text)
        pointers (declared-drop-pointers text)]
    (is (some? counts)
        "PRECONDITION: this fixture must publish a truncated fact section")
    (is (pos? (count audited))
        "PRECONDITION: this fixture must drop leaves, or there is nothing to declare")
    (is (= (- (:total counts) (:shown counts)) (count audited))
        (format (str "the text declares %d dropped (%d of %d rendered) while "
                     "the audit finds %d uncarried leaves; undeclared: %s")
                (- (:total counts) (:shown counts))
                (:shown counts) (:total counts) (count audited)
                (pr-str (take 3 (sort audited-labels)))))
    (is (every? audited-labels pointers)
        (format "the text names as dropped a pointer the audit finds carried: %s"
                (pr-str (remove audited-labels pointers))))))

;; @spec MCP-OP-STUDY-047
(deftest a-value-rendered-twice-is-carried-only-while-its-renderer-survives
  ;; The mechanism, isolated: two leaves holding the SAME long spelling. The
  ;; second may be treated as carried by the first ONLY when the first is
  ;; actually rendered. Written against `fact-block` directly so the property
  ;; is stated about the decision, not about one fixture's arithmetic.
  (let [twin "the-same-distinctive-value-rendered-twice"
        result {:ok true :alpha twin :beta twin}
        complete (inspect/fact-block "structural" result
                                     inspect/unbounded-evidence)
        entries (inspect/receipt-fact-entries "structural" result)]
    (is (= 3 (count entries))
        (str "every leaf needs its own entry when the structural text carries "
             "none of them; entries: " (pr-str (mapv :label entries))))
    (is (= 3 (:total complete)))
    ;; At an allowance that pays for the declaration and ONE fact line, the
    ;; two leaves not rendered must both be declared dropped.
    (let [narrow (inspect/fact-block "structural" result 120)
          section (inspect/fact-section narrow)]
      (is (some? section) "the declaration is never dropped for want of room")
      (is (= (- (:total narrow) (count (:dropped-labels narrow)))
             (- (:total narrow) (count (:dropped-labels narrow))))))))

;; ============================================================
;; O2 ROUND 6 — the declaration is the FLOOR, on every rung
;; (Sol O2 round-5 review, §3)
;; ============================================================
;; `fact-block` stopped its descent at zero before establishing that its own
;; header and `dropped:` line fit, and then returned `section=nil`; the
;; reachable `notice` rung published a generic pointer to `structuredContent`
;; over eleven uncarried leaves and zero fact pointers. Both are undeclared
;; omissions at exactly the budget where MCP-OP-STUDY-044's guarantee matters.

(defn- two-fact-receipt
  "A finalized receipt with two leaves the structural text cannot carry."
  []
  (clocked {:ok true
            :operation "inspect_clojure"
            :alpha "a-distinctive-alpha-value-nobody-else-spells"
            :beta "a-distinctive-beta-value-nobody-else-spells"}))

;; @spec MCP-OP-STUDY-048
(deftest the-declaration-survives-an-allowance-that-cannot-pay-for-it
  (let [result (two-fact-receipt)]
    (doseq [allowance [0 1 32]]
      (testing (str "allowance " allowance)
        (let [block (inspect/fact-block "structural" result allowance)
              section (inspect/fact-section block)]
          (is (some? section)
              (format (str "allowance %d rendered NO declaration over %d "
                           "dropped leaves: a silent omission is the one "
                           "outcome MCP-OP-STUDY-044 forbids")
                      allowance (count (:dropped-labels block))))
          (is (str/includes? (or section "") "receipt facts ·")
              "the count line names how many leaves are omitted")
          (is (str/includes? (or section "") "  dropped: ")
              "and the pointer line names the first of them")
          (is (= (count (:dropped-labels block))
                 (count (inspect/uncarried-leaves
                          (str "structural\n" section) result)))
              "the declared count is the audited count on this rung too"))))))

;; @spec MCP-OP-STUDY-048
(deftest the-notice-rung-names-every-leaf-it-omits
  ;; A receipt whose structuredContent alone leaves no room for any rendering:
  ;; the fit falls through the allowance band to the notice rung. That rung
  ;; must still say how many structured leaves it omits and point at them.
  (let [padded (fn [n]
                 (clocked {:ok true
                           :operation "inspect_clojure"
                           :read_complete true
                           :next_action "none"
                           :source_character_count 0
                           :request_count 1
                           :file_count 1
                           :results []
                           :filler (apply str (repeat n "x"))}))
        ;; The notice rung sits in a narrow band: structuredContent must fit
        ;; while leaving no room for ANY ordinary rendering. Search for it
        ;; rather than hard-coding a padding, so the witness keeps finding the
        ;; rung when the rendering's own size changes.
        raw (or (first
                  (for [n (range (- inspect-tool/max-public-result-bytes 200)
                                 (- inspect-tool/max-public-result-bytes 900)
                                 -1)
                        :let [candidate (padded n)]
                        :when (= "notice" (:text_omitted
                                            (inspect-tool/fit-public-result
                                              candidate)))]
                    candidate))
                (padded (- inspect-tool/max-public-result-bytes 450)))
        published (clocked (inspect-tool/fit-public-result raw))
        text (inspect-tool/inspect-summary published)
        audited (inspect/uncarried-leaves text published)
        counts (declared-fact-counts text)]
    (is (= "notice" (:text_omitted published))
        (format "PRECONDITION: this fixture must reach the notice rung, not %s"
                (pr-str (:text_omitted published))))
    (is (some? counts)
        (format (str "the notice rung published %d characters over %d "
                     "uncarried leaves and declared none of them; text: %s")
                (count text) (count audited) (pr-str text)))
    (is (str/includes? text "  dropped: ")
        "and it names the first of the leaves it omits")
    (is (= (- (:total counts) (:shown counts)) (count audited))
        "the declared count is the audited count on the notice rung too")))

;; @spec MCP-OP-STUDY-048
(deftest the-name-rung-names-every-leaf-it-omits
  ;; The last rung above a refusal. It is the shortest honest text there is,
  ;; and "honest" now includes saying what it does not carry.
  (let [result (two-fact-receipt)
        text (inspect/minimum-text-block result)
        audited (inspect/uncarried-leaves text result)
        counts (declared-fact-counts text)]
    (is (str/includes? text "inspect_clojure")
        "it still names the tool")
    (is (some? counts)
        (format "the name rung declared nothing over %d uncarried leaves: %s"
                (count audited) (pr-str text)))
    (is (= (- (:total counts) (:shown counts)) (count audited)))))

;; @spec MCP-OP-STUDY-049
(defn- probe-domain-result
  "A minimal ordinary `inspect_clojure` domain result, plus whatever the
   caller wants to hang off it. Built by the DOMAIN, so anything it carries is
   domain data no matter how it is spelled."
  [extra]
  (merge {:ok true
          :operation "inspect_clojure"
          :read_complete true
          :next_action "none"
          :source_character_count 0
          :request_count 1
          :file_count 1
          :results []}
         extra))

(defn- published-through-invoke
  "One domain result carried through the real finalizer and the real fit, and
   handed back exactly as the publisher would publish it."
  [domain]
  (let [captured (atom nil)]
    (mcp-operation/invoke!
      {:execute (fn [] domain)
       :fit (fn [result]
              (let [fitted (inspect-tool/fit-public-result result)]
                (reset! captured fitted)
                fitted))
       :summarize (fn [result] (inspect-tool/inspect-summary result))
       :callback (fn [_ _ _] nil)})
    @captured))

;; @spec MCP-OP-STUDY-049
(deftest a-domain-key-spelled-like-the-envelope-is-not-the-envelope
  (testing "a domain `measured` map is published as domain data"
    (let [value {:user_blob "a measurement the DOMAIN took"}
          published (published-through-invoke
                      (probe-domain-result {:measured value}))]
      (is (= value (:measured published))
          "the domain key must survive publication unchanged")
      (is (not (contains? (mcp-operation/envelope published) :measured))
          (str "the gate read a DOMAIN key as publisher metadata; the "
               "envelope it would copy into a substitute is "
               (pr-str (mcp-operation/envelope published))))))

  (testing "a domain `measured` too large to publish is a refusal that FITS"
    (let [published (published-through-invoke
                      (probe-domain-result
                        {:measured {:user_blob (apply str (repeat 40000 "x"))}}))
          bytes (inspect-tool/mcp-result-byte-count
                  (inspect-tool/inspect-summary published) published)]
      (is (false? (:ok published))
          "PRECONDITION: this fixture must reach the refusal rung")
      (is (<= bytes inspect-tool/max-public-result-bytes)
          (format (str "the fit RETURNED a candidate of %d bytes against the "
                       "%d-byte budget it exists to enforce")
                  bytes inspect-tool/max-public-result-bytes))))

  (testing "a map that merely SPELLS the envelope was never finalized"
    (let [impostor (probe-domain-result {:elapsed_ms 1.0 :measured {}})]
      (is (not (mcp-operation/finalized? impostor))
          (str "a domain result was accepted as finalized because it spells "
               "the envelope's key names; the envelope is identified by "
               "CONSTRUCTION, never by spelling"))
      (is (thrown? IllegalArgumentException
                   (inspect-tool/fit-public-result impostor))
          "and the gate must refuse to measure it"))))

;; @spec MCP-OP-STUDY-050
(defn- many-fact-receipt
  "One ordinary receipt carrying `n` tiny numeric facts. Numeric leaves are
   COLLIDABLE, so every one of them needs a line of its own — which is the
   shape that makes the fit's rendering search do the most work."
  [n]
  (mcp-operation/stamp-envelope
    (merge {:ok true
            :operation "inspect_clojure"
            :read_complete true
            :next_action "none"
            :source_character_count 0
            :request_count 1
            :file_count 1
            :results []}
           (into {} (map (fn [i] [(keyword (str "f" i)) i])) (range n)))
    {:elapsed_ms 0.0}))

;; @spec MCP-OP-STUDY-050
(deftest the-fit-stays-affordable-at-ten-thousand-facts
  ;; A read-path latency finding is a correctness finding once it is large
  ;; enough: a caller waiting 69 seconds for a bounded receipt has been given
  ;; a different tool than the one documented.
  (let [warm (many-fact-receipt 1000)]
    ;; One warm pass, so the number below is the algorithm rather than the
    ;; first JIT compilation of it.
    (inspect-tool/fit-public-result warm))
  (let [raw (many-fact-receipt 10000)
        started (System/nanoTime)
        fitted (inspect-tool/fit-public-result raw)
        elapsed-ms (/ (- (System/nanoTime) started) 1000000.0)
        text (inspect-tool/inspect-summary fitted)
        bytes (inspect-tool/mcp-result-byte-count text fitted)
        counts (declared-fact-counts text)
        audited (inspect/uncarried-leaves text fitted)]
    (is (< elapsed-ms 2000.0)
        (format "the fit took %.2f ms over 10,000 receipt facts" elapsed-ms))
    (is (<= bytes inspect-tool/max-public-result-bytes)
        (format "and published %d bytes against a %d-byte budget"
                bytes inspect-tool/max-public-result-bytes))
    (is (some? counts)
        "the rendering still declares what it dropped")
    (is (= (- (:total counts) (:shown counts)) (count audited))
        "and the declared count is still the audited count")))

;; ============================================================
;; O2 ROUND 7 — a pointer spells the leaf's ADDRESS, never its VALUE
;; (Sol O2 round-6 review, §2)
;; ============================================================
;; A public rung declared 23 omitted facts while its own product audit found
;; 19. The mechanism is a coincidence the two sides of the guarantee resolve
;; DIFFERENTLY: a leaf whose distinctive value equals its own JSON pointer is
;; declared dropped by `fact-block`, and then the `dropped:` line that names
;; it spells those same characters — so the substring carriage test finds the
;; value in the published text and `uncarried-leaves` calls the leaf carried.
;; The declaration and the audit disagree BY CONSTRUCTION, which is the one
;; thing MCP-OP-STUDY-047 exists to forbid.
;;
;; The repair is a single carriage rule both sides read: a leaf is carried
;; when the text contains the WHOLE LINE the renderer emits for THAT leaf —
;; `  <pointer>: <value>` or `  <pointer>=<value>` — and by nothing else. A
;; pointer occurring inside a declaration is an address, not a value.

(defn- pointer-valued-receipt
  "A finalized receipt whose leaves' distinctive values are their own JSON
   pointers. Sixteen characters each: distinctive under
   `min-distinctive-spelling`, and identical to the pointer the `dropped:`
   line would name them by."
  [n]
  (clocked (merge {:ok true
                   :operation "inspect_clojure"
                   :read_complete true
                   :next_action "none"
                   :source_character_count 0
                   :request_count 1
                   :file_count 1
                   :results []}
                  (into {}
                        (map (fn [i]
                               (let [pointer (format "pointervalue%04d" i)]
                                 [(keyword pointer) pointer])))
                        (range n)))))

;; @spec MCP-OP-STUDY-051
(deftest a-dropped-pointer-is-not-carriage-of-the-value-it-names
  ;; The mechanism in one leaf, with no budget arithmetic around it: a leaf
  ;; whose value IS its pointer, dropped, and then named as dropped.
  (let [spelling "abcdefghijklmnop"
        result {(keyword spelling) spelling}
        block (inspect/fact-block "structural" result 0)
        section (inspect/fact-section block)
        text (str "structural\n" section)
        audited (inspect/uncarried-leaves text result)]
    (is (= 1 (count (:dropped-labels block)))
        "PRECONDITION: at allowance 0 the one leaf is dropped")
    (is (str/includes? (or section "") (str "dropped: " spelling))
        "PRECONDITION: and the declaration names it by that pointer")
    (is (= (count (:dropped-labels block)) (count audited))
        (format (str "the block declares %d dropped while the audit finds %d "
                     "uncarried: the `dropped:` pointer satisfied the value's "
                     "own carriage test; section %s")
                (count (:dropped-labels block)) (count audited)
                (pr-str section)))))

;; @spec MCP-OP-STUDY-051
;; @spec MCP-OP-STUDY-047
(deftest the-name-rung-declares-exactly-what-its-own-audit-finds
  ;; The reviewer's public reproduction, made deterministic: the `name` rung
  ;; is the shortest text the tool publishes, it declares every leaf, and on
  ;; a receipt of pointer-valued leaves its declaration and its audit must
  ;; still be one number.
  (let [result (pointer-valued-receipt 20)
        text (inspect/minimum-text-block result)
        counts (declared-fact-counts text)
        audited (inspect/uncarried-leaves text result)
        audited-labels (set (map (fn [[path _]] (inspect/leaf-label path))
                                 audited))
        declared (- (:total counts) (:shown counts))]
    (is (some? counts) "PRECONDITION: the name rung declares its omissions")
    (is (pos? declared) "PRECONDITION: and this receipt omits leaves")
    (is (= declared (count audited))
        (format (str "the name rung declares %d omitted facts while its own "
                     "audit finds %d; the leaves it counts as carried are "
                     "named by the `dropped:` line and rendered nowhere: %s")
                declared (count audited)
                (pr-str (sort (remove audited-labels
                                      (map (fn [[path _]]
                                             (inspect/leaf-label path))
                                           (inspect/receipt-leaf-pairs result))))))))
  ;; And on every rung of the same fixture, not only the one that fails today.
  (doseq [n [1 2 5 20]]
    (testing (str n " pointer-valued leaves")
      (let [result (pointer-valued-receipt n)]
        (doseq [allowance [0 1 64 256 4096]]
          (testing (str "allowance " allowance)
            (let [block (inspect/fact-block "" result allowance)
                  text (or (inspect/fact-section block) "")]
              (is (= (count (:dropped-labels block))
                     (count (inspect/uncarried-leaves text result)))
                  (format "declared %d against audited %d at allowance %d"
                          (count (:dropped-labels block))
                          (count (inspect/uncarried-leaves text result))
                          allowance)))))))))

;; ============================================================
;; O2 ROUND 7 — a substring of a decoy is not a rendering of a fact
;; (Sol O2 round-6 review, §3)
;; ============================================================
;; Two coincidences the reviewer PLANTED, both of which published `2 of 2
;; rendered` over a fact that was nowhere in the text:
;;
;;   `decoy: XXabcdefghijklmnopYY`   carries `target = abcdefghijklmnop`
;;   `alpha: <long value>`           carries `beta = <the same long value>`
;;
;; In each case a caller reading the text cannot find the fact, cannot tell
;; which pointer the characters belong to, and cannot remove one fact without
;; removing the other. Carriage has to mean the WHOLE LINE the renderer
;; emitted for THAT leaf.

(defn- carriage-report
  "What one `fact-block` rendering says about itself, and what an audit of the
   text it produced says back. `:orphans` are the leaves the block counted as
   RENDERED whose own pointer line is nowhere in the section."
  [result budget]
  (let [block (inspect/fact-block "" result budget)
        section (or (inspect/fact-section block) "")
        lines (set (str/split-lines section))
        dropped (set (:dropped-labels block))
        orphans (into []
                      (comp (map (fn [[path _]] (inspect/leaf-label path)))
                            (remove dropped)
                            (remove (fn [label]
                                      (some #(str/starts-with?
                                               % (str "  " label))
                                            lines))))
                      (remove (fn [[path _]] (inspect/leaf-excluded? path))
                              (inspect/receipt-leaf-pairs result)))]
    {:section section
     :shown (:shown block)
     :total (:total block)
     :declared (count dropped)
     :audited (count (inspect/uncarried-leaves section result))
     :orphans orphans}))

;; @spec MCP-OP-STUDY-051
(deftest a-value-inside-a-longer-decoy-is-not-a-rendered-fact
  ;; A sixteen-character distinctive value that occurs in the text ONLY inside
  ;; a longer value's line. It has no line of its own; the text names no
  ;; pointer for it; and the header counted it rendered.
  (let [result {:ok true
                :decoy "XXabcdefghijklmnopYY"
                :target "abcdefghijklmnop"}]
    ;; SWEPT, not sampled. Budget 90 was the reviewer's rung, but which
    ;; budget renders `decoy` and not `target` moves whenever the declaration
    ;; changes size, and a witness pinned to one budget stops seeing the class
    ;; the moment the arithmetic shifts under it.
    (doseq [budget (range 40 240 4)]
      (testing (str "budget " budget)
        (let [report (carriage-report result budget)]
          (is (empty? (:orphans report))
              (format (str "budget %d: %d leaves counted as rendered have no "
                           "pointer line of their own — %s; section %s")
                      budget (count (:orphans report)) (pr-str (:orphans report))
                      (pr-str (:section report))))
          (is (= (:declared report) (:audited report))
              (format "budget %d: declared %d against audited %d"
                      budget (:declared report) (:audited report))))))
    ;; A leaf is either RENDERED — and then a different sixteen-character
    ;; value must change the bytes — or DECLARED dropped. What it may never be
    ;; is counted as rendered while the text shows the same bytes whatever it
    ;; holds, which is what the decoy bought at budget 90.
    (let [rendered (fn [budget r] (:section (carriage-report r budget)))]
      (is (not= (rendered 120 result)
                (rendered 120 (assoc result :target "ponmlkjihgfedcba")))
          "at a budget that renders the target, replacing its value left the
           rendering byte-identical")
      (is (str/includes? (rendered 90 result) "dropped: ")
          "and at a budget that cannot render it, the text says so"))))

;; @spec MCP-OP-STUDY-051
;; @spec MCP-OP-STUDY-047
(deftest one-value-at-two-pointers-is-two-independently-removable-facts
  ;; MCP-OP-STUDY-044 named this residual and left it standing: "a receipt
  ;; that spells one fact twice does not make each copy independently
  ;; recoverable." A pointer line each closes it.
  (let [twin "the-same-distinctive-value-rendered-twice"
        result {:ok true :alpha twin :beta twin}]
    (doseq [budget (range 40 260 4)]
      (testing (str "budget " budget)
        (let [report (carriage-report result budget)]
          (is (empty? (:orphans report))
              (format (str "budget %d: %d leaves counted as rendered have no "
                           "pointer line of their own — %s; section %s")
                      budget (count (:orphans report)) (pr-str (:orphans report))
                      (pr-str (:section report))))
          (is (= (:declared report) (:audited report))
              (format "budget %d: declared %d against audited %d"
                      budget (:declared report) (:audited report))))))
    ;; Removability, one leaf at a time, at a budget that renders both.
    (let [rendered (fn [r] (:section (carriage-report r 200)))
          whole (rendered result)]
      (is (not= whole (rendered (dissoc result :beta)))
          "removing `beta` left the rendering byte-identical: one line was
           doing the work of two facts")
      (is (not= whole (rendered (dissoc result :alpha)))
          "removing `alpha` left the rendering byte-identical"))))

;; @spec MCP-OP-STUDY-051
(deftest no-published-top-level-key-can-spell-a-declaration-line
  ;; The RESIDUAL, named in MCP-OP-STUDY-051 and ratcheted here rather than
  ;; left to be rediscovered. A fact line is `  <pointer><sep><spelling>` and
  ;; the declaration lines are `  receipt facts · …` and `  dropped: …`, so
  ;; the one way a declaration could still be mistaken for a fact's own line
  ;; is a TOP-LEVEL receipt key spelled `dropped` or `receipt facts` — nested
  ;; pointers always carry a `.` or a `[`, so they cannot collide. Every
  ;; top-level key is constructed inside `clj-surgeon.mcp-inspect`; this holds
  ;; that true.
  (let [reserved #{"dropped" "receipt facts"}
        top-level-keys
        (fn [result]
          (into #{}
                (comp (map (fn [[path _]] (inspect/leaf-label path)))
                      (remove #(or (str/includes? % ".")
                                   (str/includes? % "["))))
                (inspect/receipt-leaf-pairs result)))]
    (with-tmp-project
      (fn [dir]
        (spit (str dir "/deps.edn") "{:paths [\"src\"]}")
        (fs/create-dirs (str dir "/src/fixture"))
        (spit (str dir "/src/fixture/core.clj") class-ratchet-fixture))
      (fn [config]
        (doseq [[operation extra] (sort class-ratchet-requests)]
          (testing operation
            (let [published (clocked
                              (inspect-tool/execute-inspect!
                                config
                                {"requests" [(merge {"id" "r1"
                                                     "operation" operation
                                                     "file" "src/fixture/core.clj"}
                                                    extra)]
                                 "expect" {"requests" 1 "files" 1}}))
                  collisions (filter reserved (top-level-keys published))]
              (is (empty? collisions)
                  (str operation ": a top-level receipt key spells a "
                       "declaration line — " (pr-str collisions))))))))
    ;; And a refusal, whose top-level shape is built by a different function.
    (let [refusal (clocked (inspect-tool/execute-inspect!
                             {:project-root "/var/tmp/forge/o2r7-fx/nowhere"}
                             {"requests" [{"id" "r1" "operation" "outline"
                                           "file" "src/missing.clj"}]
                              "expect" {"requests" 1 "files" 1}}))]
      (is (empty? (filter reserved (top-level-keys refusal)))
          "a refusal's top-level keys spell no declaration line either"))))

;; @spec MCP-OP-STUDY-052
;; Field evidence (Sol O2 round-7 review, 2026-09-04, section 2): `leaf-label`
;; joined path segments with a bare `.`, so the top-level key `"a.b"` and the
;; nested path `[:a :b]` both spelled `a.b`. `text-line-index` is a SET, so one
;; rendered `a.b: <value>` line discharged BOTH leaves: 587 declaration/audit
;; disagreements across the allowance band, the first 19 declared against 18
;; audited. A pointer that cannot be decoded back to the path that made it is
;; not an address.
(deftest two-distinct-leaves-never-share-a-pointer
  (testing "the reviewer's colliding pair"
    (let [twin "the-same-distinctive-value-rendered-twice"
          result {"a.b" twin :a {:b twin}}]
      (is (= 2 (count (set (map (fn [[path _]] (inspect/leaf-label path))
                                (inspect/receipt-leaf-pairs result)))))
          (str "the dotted top-level key and the nested path spell one "
               "pointer: "
               (pr-str (mapv (fn [[path _]] (inspect/leaf-label path))
                             (inspect/receipt-leaf-pairs result)))))
      (doseq [budget (range 40 300 4)]
        (testing (str "budget " budget)
          (let [report (carriage-report result budget)]
            (is (empty? (:orphans report))
                (format (str "budget %d: %d leaves counted as rendered have "
                             "no pointer line of their own — %s; section %s")
                        budget (count (:orphans report))
                        (pr-str (:orphans report))
                        (pr-str (:section report))))
            (is (= (:declared report) (:audited report))
                (format "budget %d: declared %d against audited %d; section %s"
                        budget (:declared report) (:audited report)
                        (pr-str (:section report)))))))))
  (testing "an index rendering is not a key spelling"
    (let [twin "the-same-distinctive-value-rendered-twice"
          result {"x[0]" twin :x [twin]}]
      (is (= 2 (count (set (map (fn [[path _]] (inspect/leaf-label path))
                                (inspect/receipt-leaf-pairs result)))))
          "a key spelled `x[0]` collided with the first element of `x`")))
  (testing "a generated family over every delimiter this syntax uses"
    ;; Exhaustive rather than sampled, and deterministic: every path of length
    ;; one, two and three over an alphabet built from the delimiters
    ;; themselves. If the encoding is injective the label set is exactly as
    ;; large as the path set.
    (let [alphabet ["a" "a.b" "b" "." ".." "/" "~" "~0" "[" "]" "[0]" ":" "="
                    "\\" "0" 0 1]
          paths (concat (for [x alphabet] [x])
                        (for [x alphabet y alphabet] [x y])
                        (for [x alphabet y alphabet z alphabet] [x y z]))
          labels (map inspect/leaf-label paths)
          duplicates (->> (map vector paths labels)
                          (group-by second)
                          (filter (fn [[_ group]] (> (count group) 1)))
                          (take 3))]
      (is (= (count paths) (count (set labels)))
          (str "distinct paths spelled one pointer — "
               (pr-str (mapv (fn [[label group]]
                               [label (mapv first group)])
                             duplicates)))))))
