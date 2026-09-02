(ns clj-surgeon.mcp-close-losers-test
  "Witnesses for the measured-loser write shapes closed at the MCP server.

   Evidence: docs/observations/2026-09-02-captains-log-bridge-wall-clock-ideal-program.md
   receipts 08:57Z (425 lines across five files), 08:58Z (+508/-476 against the
   canonical +59/-34) and 09:01Z (separation by verb shape, 3 of 3 against 0 of 4)."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-contract :as contract]
   [clj-surgeon.mcp-extraction-plan :as extraction-plan]
   [clj-surgeon.mcp-tool :as mcp-tool]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(defn- wire
  "The map shape the real server hands to validation: `execute-request!` round
   trips every request through `json/parse-string ... true`, so the keys that
   reach the refusal are KEYWORDS, not strings."
  [params]
  (json/parse-string (json/generate-string params) true))

(def ^:private probe-source
  (str "(ns app.a\n"
       "  (:require [clojure.string :as str]\n"
       "            [clojure.set :as set]))\n\n"
       ";; a leading comment that must not move\n"
       "(def config {:a 1})\n\n"
       "(defn b [y]\n      (set/union #{y}   #{1}))\n"))

(defn- temp-workspace []
  (let [dir (io/file (str (System/getProperty "java.io.tmpdir")
                          "/clj-surgeon-close-losers-" (System/nanoTime)))]
    (.mkdirs (io/file dir "src/app"))
    (spit (io/file dir "src/app/a.clj") probe-source)
    ;; One file that is exactly one top-level form, so an edit whose find
    ;; covers that form leaves no untouched gap at all.
    (spit (io/file dir "src/app/only.clj") "(def x  [1  2])\n")
    dir))

(defn- delete-tree!
  [dir]
  (doseq [file (reverse (file-seq dir))]
    (io/delete-file file true)))

(defn- through-the-server
  "Drive the same entry point production uses and report the result together
   with whether any byte on disk changed."
  ([params] (through-the-server params {}))
  ([params config-extra]
  (let [workspace (temp-workspace)
        file (io/file workspace "src/app/a.clj")
        before (slurp file)]
    (try
      (let [result (mcp-tool/execute-request!
                     (merge
                       {:project-root (.getPath workspace)
                        :receipt-dir (.getPath (io/file workspace "receipts"))}
                       config-extra)
                     params)]
        {:result result
         :bytes-written? (not= before (slurp file))
         :after (slurp file)
         :source-unchanged? (= before (slurp file))})
      (finally (delete-tree! workspace))))))

(defn- namespace-owner-insert-params
  "The l1 A-1 / A-3 shape: add one require to two namespaces through an owner of
   kind namespace."
  []
  {"changes"
   [{"id" "add-clock"
     "files" ["src/app/reducer_session.clj" "src/app/reducer_lab.clj"]
     "owner" {"kind" "namespace"}
     "find" "[clojure.set :as set]"
     "insert_after" ["[app.clock :as clock]"]
     "expect" {"matches" 2 "each_file" 1}}]
   "expect" {"changes" 1 "edits" 2 "files" 2}})

(defn- namespace-owner-replace-params
  []
  {"changes"
   [{"id" "swap-import"
     "files" ["src/app/reducer_lab.clj"]
     "owner" {"kind" "namespace" "name" "app.reducer-lab"}
     "find" "(java.util Date)"
     "replace" "(java.time Instant)"
     "expect" {"matches" 1}}]
   "expect" {"changes" 1 "edits" 1 "files" 1}})

(defn- namespace-owner-insert-nonrequire-params
  []
  {"changes"
   [{"id" "add-def"
     "files" ["src/app/reducer_lab.clj"]
     "owner" {"kind" "namespace"}
     "find" "(def a 1)"
     "insert_before" ["(def zero 0)"]
     "expect" {"matches" 1}}]
   "expect" {"changes" 1 "edits" 1 "files" 1}})

;; @spec MCP-OP-CLOSE-001
(deftest namespace-owner-insertion-is-refused-before-any-source-is-read
  (let [result (contract/validate-tool-params (namespace-owner-insert-params))]
    (is (false? (:ok result))
        "owner kind namespace plus find plus insert_after is a measured loser")
    (is (= :whole-file-reprint-refused (:error-type result)))
    (is (true? (:source-unchanged result)))
    (is (false? (:mutation-attempted result)))
    (is (false? (:write-authority result)))))

;; @spec MCP-OP-CLOSE-001
(deftest namespace-owner-replacement-is-refused
  (let [result (contract/validate-tool-params (namespace-owner-replace-params))]
    (is (false? (:ok result)))
    (is (= :whole-file-reprint-refused (:error-type result)))))

;; @spec MCP-OP-CLOSE-002
;; @spec MCP-OP-CLOSE-003
(deftest a-refused-namespace-owner-replacement-carries-a-complete-redirect
  (let [result (contract/validate-tool-params (namespace-owner-replace-params))
        next-call (:next-call result)]
    (is (= "edit_clojure" (:tool next-call)))
    (is (= [{:file "src/app/reducer_lab.clj"
             :within {:namespace "app.reducer-lab"}
             :from "(java.util Date)"
             :to "(java.time Instant)"
             :matches 1}]
           (get-in next-call [:arguments :edits]))
        "a one-form replacement is derivable from the refused request alone")
    (is (nil? (:missing result)) "nothing is missing; the call runs unchanged")
    (is (= "call_edit_clojure_once" (:next-action result)))))

;; @spec MCP-OP-CLOSE-002
(deftest the-redirect-is-completed-with-the-workspace-the-router-resolved
  (let [attach (requiring-resolve
                 'clj-surgeon.mcp-close-losers/attach-workspace-root)]
    (is (= {:ok false
            :next_call {:tool "edit_clojure"
                        :arguments {:edits [] :workspace_root "/abs/repo"}}}
           (attach {:ok false
                    :next_call {:tool "edit_clojure" :arguments {:edits []}}}
                   "/abs/repo"))
        "the validator never sees workspace_root; the router does")
    (is (= {:ok true :edits 1} (attach {:ok true :edits 1} "/abs/repo"))
        "a result without a redirect is returned unchanged")))

;; @spec MCP-OP-CLOSE-002
;; @spec MCP-OP-CLOSE-003
(deftest a-refused-require-insertion-redirects-to-require-change-and-names-what-is-missing
  (let [result (contract/validate-tool-params (namespace-owner-insert-params))
        next-call (:next-call result)]
    (is (= "edit_clojure" (:tool next-call)))
    (is (= {:add {:lib "app.clock" :as "clock"}
            :files [{:file "src/app/reducer_session.clj"}
                    {:file "src/app/reducer_lab.clj"}]}
           (get-in next-call [:arguments :require-change]))
        "Y-5 proved require_change is the zero-churn path for this exact change")
    (is (= ["symbol_migration"] (:missing result))
        "editor-hybrid-schema binds require_change to symbol_migration")
    (is (= "fill_next_call_then_call_edit_clojure_once" (:next-action result)))))

;; @spec MCP-OP-CLOSE-003
(deftest a-refused-non-require-insertion-names-the-fields-it-cannot-derive
  (let [result (contract/validate-tool-params
                 (namespace-owner-insert-nonrequire-params))
        next-call (:next-call result)]
    (is (= "edit_clojure" (:tool next-call)))
    (is (= "(def a 1)" (get-in next-call [:arguments :edits 0 :from])))
    (is (= {:namespace true} (get-in next-call [:arguments :edits 0 :within])))
    (is (= ["from" "to"] (:missing result))
        "an insertion has no one-form-to-one-form spelling; the caller widens it")
    (is (= "fill_next_call_then_call_edit_clojure_once" (:next-action result)))))

;; @spec MCP-OP-CLOSE-009
(deftest the-measured-winners-are-not-refused
  (testing "within plus from/to (A-0, A-4, Y-0: zero churn)"
    (let [result (contract/validate-tool-params
                   {"edits" [{"file" "src/app/reducer_lab.clj"
                              "within" {"namespace" true}
                              "from" "(java.util Date)"
                              "to" "(java.time Instant)"
                              "matches" 1}]})]
      (is (not= :whole-file-reprint-refused (:error-type result)))))
  (testing "forms-scoped replace stays open; it is gated on measured drift"
    (let [result (contract/validate-tool-params
                   {"changes"
                    [{"id" "swap"
                      "files" ["src/app/reducer_lab.clj"]
                      "forms" ["reduce-session"]
                      "find" "(old-call x)"
                      "replace" "(new-call x)"
                      "expect" {"matches" 1}}]
                    "expect" {"changes" 1 "edits" 1 "files" 1}})]
      (is (:ok result))))
  (testing "a namespace-owner delete is already refused by its own contract"
    (let [result (contract/validate-tool-params
                   {"changes"
                    [{"id" "drop"
                      "files" ["src/app/reducer_lab.clj"]
                      "forms" ["reduce-session"]
                      "delete" true
                      "expect" {"matches" 1}}]
                    "expect" {"changes" 1 "edits" 1 "files" 1}})]
      (is (not= :whole-file-reprint-refused (:error-type result))))))

;; @spec MCP-OP-CLOSE-013
(deftest the-refusal-fires-on-the-map-shape-the-real-server-actually-passes
  (testing "keyword keys, the shape execute-request! produces"
    (let [result (contract/validate-tool-params
                   (wire (namespace-owner-insert-params)))]
      (is (false? (:ok result))
          "a string-key-only reader is dead on the real server")
      (is (= :whole-file-reprint-refused (:error-type result)))
      (is (= ["symbol_migration"] (:missing result)))))
  (testing "string keys, the shape a hand-built test produces"
    (let [result (contract/validate-tool-params
                   (namespace-owner-insert-params))]
      (is (= :whole-file-reprint-refused (:error-type result)))))
  (testing "both shapes agree field for field"
    (is (= (dissoc (contract/validate-tool-params
                     (namespace-owner-replace-params)) :elapsed_ms)
           (dissoc (contract/validate-tool-params
                     (wire (namespace-owner-replace-params))) :elapsed_ms)))))

;; @spec MCP-OP-CLOSE-013
(deftest the-loser-is-refused-through-the-entry-point-production-uses
  (let [{:keys [result source-unchanged?]}
        (through-the-server
          {"changes" [{"id" "swap"
                       "files" ["src/app/a.clj"]
                       "owner" {"kind" "namespace" "name" "app.a"}
                       "find" "[clojure.set :as set]"
                       "replace" "[clojure.set :as sets]"
                       "expect" {"matches" 1}}]
           "expect" {"changes" 1 "edits" 1 "files" 1}})]
    (is (false? (:ok result)) (pr-str result))
    (is (= "whole-file-reprint-refused" (:error_type result)))
    (is (true? (:source_unchanged result)))
    (is source-unchanged? "a refused shape writes zero bytes")
    (is (= "edit_clojure" (get-in result [:next_call :tool])))
    (is (string? (get-in result [:next_call :arguments :workspace_root]))
        "the router completes the redirect with the workspace it resolved")))

;; @spec MCP-OP-CLOSE-014
(deftest every-action-that-restages-a-namespace-owner-is-refused
  (doseq [[label action]
          [["replace" {"replace" "[clojure.set :as sets]"}]
           ["insert_before" {"insert_before" ["[app.clock :as clock]"]}]
           ["insert_after" {"insert_after" ["[app.clock :as clock]"]}]
           ["assoc_entry" {"assoc_entry" {"key" ":b" "value" "2"}}]]]
    (testing label
      (let [params {"changes" [(merge {"id" "x"
                                       "files" ["src/app/a.clj"]
                                       "owner" {"kind" "namespace"}
                                       "find" "{:a 1}"
                                       "expect" {"matches" 1}}
                                      action)]
                    "expect" {"changes" 1 "edits" 1 "files" 1}}]
        (is (= :whole-file-reprint-refused
               (:error-type (contract/validate-tool-params params)))
            (str label " restages the whole file and must be refused"))
        (is (= :whole-file-reprint-refused
               (:error-type (contract/validate-tool-params (wire params))))
            (str label " must also be refused on the real key shape"))))))

;; @spec MCP-OP-CLOSE-014
(deftest an-assoc-entry-redirect-derives-its-replacement-and-names-what-is-missing
  (let [result (contract/validate-tool-params
                 (wire {"changes" [{"id" "ae"
                                    "files" ["src/app/a.clj"]
                                    "owner" {"kind" "namespace"}
                                    "find" "{:a 1}"
                                    "assoc_entry" {"key" ":b" "value" "2"}
                                    "expect" {"matches" 1}}]
                        "expect" {"changes" 1 "edits" 1 "files" 1}}))]
    (is (= "{:a 1 :b 2}" (get-in result [:next-call :arguments :edits 0 :to]))
        "the replacement is spliced before the closing brace, as the kernel does")
    (is (= ["from"] (:missing result))
        "assoc_entry matched its map by value; only the caller holds the exact
         source spelling that `from` needs")))

;; @spec MCP-OP-CLOSE-015
(deftest a-compiler-that-reprints-is-refused-through-the-real-path
  (let [reprint
        (fn [source edits]
          (reduce (fn [text _]
                    (str/replace text "(set/union #{y}   #{1})"
                                 "(set/union #{y} #{1})"))
                  source edits))
        {:keys [result source-unchanged?]}
        (with-redefs-fn {#'clj-surgeon.intent-transaction/apply-edits reprint}
          #(through-the-server
             {"changes" [{"id" "swap"
                          "files" ["src/app/a.clj"]
                          "forms" ["b"]
                          "find" "#{1}"
                          "replace" "#{2}"
                          "expect" {"matches" 1 "each_form" 1 "each_file" 1}}]
              "expect" {"changes" 1 "edits" 1 "files" 1}}))]
    (is (false? (:ok result)) (pr-str result))
    (is (= "reprint-outside-span-refused" (:error_type result)))
    (is (true? (:source_unchanged result)))
    (is source-unchanged? "the sentinel refuses before any byte is written")))

;; @spec MCP-OP-CLOSE-009
(deftest the-winners-still-commit-through-the-real-path
  (testing "within plus from/to (A-0, A-4, Y-0: zero churn)"
    (let [{:keys [result bytes-written?]}
          (through-the-server
            {"edits" [{"file" "src/app/a.clj"
                       "within" {"form" "b"}
                       "from" "#{1}"
                       "to" "#{2}"}]})]
      (is (:ok result) (pr-str result))
      (is bytes-written?)
      (is (= 0 (:byte_drift_outside_span result)))))
  (testing "a forms-scoped replace stays open and reports zero drift"
    (let [{:keys [result bytes-written?]}
          (through-the-server
            {"changes" [{"id" "r"
                         "files" ["src/app/a.clj"]
                         "forms" ["b"]
                         "find" "#{1}"
                         "replace" "#{2}"
                         "expect" {"matches" 1 "each_form" 1 "each_file" 1}}]
             "expect" {"changes" 1 "edits" 1 "files" 1}})]
      (is (:ok result) (pr-str result))
      (is bytes-written?)
      (is (= 0 (:byte_drift_outside_span result))))))

;; @spec MCP-OP-CLOSE-016
(deftest a-complete-redirect-replays-unchanged-and-commits
  (let [workspace (temp-workspace)
        file (io/file workspace "src/app/a.clj")
        config {:project-root (.getPath workspace)
                :receipt-dir (.getPath (io/file workspace "receipts"))}]
    (try
      (let [refusal
            (mcp-tool/execute-request!
              config
              {"changes" [{"id" "swap"
                           "files" ["src/app/a.clj"]
                           "owner" {"kind" "namespace" "name" "app.a"}
                           "find" "[clojure.set :as set]"
                           "replace" "[clojure.set :as sets]"
                           "expect" {"matches" 1}}]
               "expect" {"changes" 1 "edits" 1 "files" 1}})]
        (is (= "whole-file-reprint-refused" (:error_type refusal)))
        (is (nil? (:missing refusal))
            "a namespace-scoped replace is fully derivable")
        (let [replay (mcp-tool/execute-request!
                       (assoc config :public-operation "edit_clojure")
                       (json/parse-string
                         (json/generate-string
                           (get-in refusal [:next_call :arguments]))))]
          (is (:ok replay) (pr-str replay))
          (is (= 0 (:byte_drift_outside_span replay)))
          (is (str/includes? (slurp file) "[clojure.set :as sets]")
              "the caller's intended change landed by running the redirect as
               it was handed to them")
          (is (str/includes? (slurp file) "(set/union #{y}   #{1})")
              "and nothing else in the file moved")))
      (finally (delete-tree! workspace)))))

;; @spec MCP-OP-CLOSE-020
(deftest the-prepared-basis-route-also-runs-the-closed-shape-refusal
  (let [{:keys [result source-unchanged?]}
        (through-the-server
          {"basis" "prepared-0000"
           "decisions" []
           "changes" [{"id" "swap"
                       "files" ["src/app/a.clj"]
                       "owner" {"kind" "namespace" "name" "app.a"}
                       "find" "[clojure.set :as set]"
                       "replace" "[clojure.set :as sets]"
                       "expect" {"matches" 1}}]})]
    (is (false? (:ok result)) (pr-str result))
    (is (= "whole-file-reprint-refused" (:error_type result))
        "a basis request skips validate-tool-params, so the refusal has to be
         applied on that route explicitly or it is simply absent there")
    (is source-unchanged?)))

(defn- extraction-workspace []
  (let [dir (io/file (str (System/getProperty "java.io.tmpdir")
                          "/clj-surgeon-close-extract-" (System/nanoTime)))]
    (.mkdirs (io/file dir "src/sample"))
    (spit (io/file dir "src/sample/core.clj")
          (str "(ns sample.core\n"
               "  (:require [clojure.string :as str]))\n\n"
               "(defn helper [x] (str/upper-case x))\n\n"
               "(defn retained []   :ok)\n"))
    dir))

(defn- run-extraction
  [workspace config-extra]
  (let [plan (extraction-plan/plan!
               {:project-root (.getPath workspace)}
               {:mode "plan-extraction"
                :file "src/sample/core.clj"
                :to "src/sample/moved.clj"
                :forms ["helper"]
                :require_policy "copy-all"})]
    (mcp-tool/execute-request!
      (merge {:project-root (.getPath workspace)
              :receipt-dir (.getPath (io/file workspace "receipts"))}
             config-extra)
      {:extraction {:file "src/sample/core.clj"
                    :to "src/sample/moved.clj"
                    :forms ["helper"]
                    :require_policy "copy-all"
                    :source_hash (:source_hash plan)
                    :caller_changes []
                    :ignored_caller_files []}})))

;; @spec MCP-OP-CLOSE-019
(deftest an-extraction-leaves-the-files-it-modifies-byte-identical
  (testing "the winner still commits, and untouched source keeps its spacing"
    (let [workspace (extraction-workspace)]
      (try
        (let [result (run-extraction workspace {})]
          (is (:ok result) (pr-str result))
          (is (str/includes? (slurp (io/file workspace "src/sample/core.clj"))
                             "(defn retained []   :ok)")
              "the modified file keeps every byte outside the extracted forms"))
        (finally (delete-tree! workspace)))))
  (testing "a staging step cannot even hand back a modified file"
    ;; Finding: extraction has a prior keyset guard. `with-future-sources`
    ;; refuses when the formatter returns a file set that does not match what
    ;; it was given, and the formatter is only ever given the created files.
    ;; So a formatter cannot reach a modified file on this route at all. The
    ;; drift gate stays as an independent second backstop for the case where
    ;; the keyset matches but the bytes do not; it has no reachable path today
    ;; and is recorded as such rather than claimed as a caught defect.
    (let [workspace (extraction-workspace)
          source (io/file workspace "src/sample/core.clj")
          before (slurp source)
          overreaching-formatter
          (fn [_root _command future-sources]
            {:ok true :status :complete
             :file-count (count future-sources)
             :changed-file-count 1 :elapsed_ms 0.1
             :future-sources
             (assoc future-sources
                    "src/sample/core.clj"
                    (str "(ns sample.core\n"
                         "  (:require [clojure.string :as str]))\n\n"
                         "(defn retained [] :ok)\n"))})]
      (try
        (let [result (run-extraction
                       workspace
                       {:formatter ["fixture-formatter" "{files}"]
                        :format-candidates! overreaching-formatter})]
          (is (false? (:ok result)) (pr-str result))
          (is (= "invalid-future-sources" (:error_type result))
              "the keyset guard refuses before the drift gate is reached")
          (is (= before (slurp source))
              "either way, a modified file reaches commit byte-identical"))
        (finally (delete-tree! workspace))))))

;; @spec MCP-OP-CLOSE-021
;; @spec MCP-OP-FMT-004
;; @spec MCP-OP-FMT-008
(deftest a-span-covering-the-whole-form-is-still-bounded
  ;; Probe R5, re-scoped by bead 46o.
  ;;
  ;; R5 found that when a `find` covered an entire top-level form there were no
  ;; untouched gaps left, the gap-only measurement was vacuously zero, and a
  ;; formatter that turned double spaces into tabs committed reporting
  ;; `byte_drift_outside_span` 0. Round three answered it by refusing any byte
  ;; the formatter changed at all.
  ;;
  ;; 46o replaces that answer rather than removing it. The formatter is now
  ;; handed one top-level form at a time and its output must be the same tokens
  ;; and comments in the same order (MCP-OP-FMT-005), so it may move layout and
  ;; may do nothing else. Managed formatting therefore commits again — that is
  ;; the point of the bead — and what bounds it is no longer a vacuous gap
  ;; count but two positive claims: the tokens are identical, and every byte
  ;; outside the edited forms is identical.
  (let [tabbing-formatter
        (fn [_root _command future-sources]
          {:ok true :status :complete
           :file-count (count future-sources)
           :changed-file-count 1 :elapsed_ms 0.1
           :future-sources
           (into {} (map (fn [[file source]]
                           [file (str/replace source "  " "\t\t")]))
                 future-sources)})
        workspace (temp-workspace)
        target (io/file workspace "src/app/only.clj")]
    (try
      (let [result
            (mcp-tool/execute-request!
              {:project-root (.getPath workspace)
               :receipt-dir (.getPath (io/file workspace "receipts"))
               :formatter ["fixture-formatter" "{files}"]
               :format-candidates! tabbing-formatter}
              {"changes" [{"id" "whole-form"
                           "files" ["src/app/only.clj"]
                           "forms" ["x"]
                           "find" "(def x  [1  2])"
                           "replace" "(def x  [9  9])"
                           "expect" {"matches" 1 "each_form" 1 "each_file" 1}}]
               "expect" {"changes" 1 "edits" 1 "files" 1}})]
        (is (:ok result) (pr-str result))
        (is (= 0 (:byte_drift_from_expected result))
            "the scoped format is an authorized stage, so the expectation the
             commit gate measures is the post-format image")
        (is (= 0 (:byte_drift_outside_span result)))
        (is (= "(def x\t\t[9\t\t9])\n" (slurp target))
            "layout inside the edited form is the formatter's to decide")
        (is (= 1 (get-in result [:format :changed-form-count])))
        (is (= :top-level-forms (get-in result [:format :scope]))))
      (finally (delete-tree! workspace)))))

;; @spec MCP-OP-FMT-002
;; @spec MCP-OP-FMT-004
(deftest the-same-formatter-cannot-reach-a-form-the-change-did-not-edit
  ;; The claim R5's vacuous zero could not make. `a.clj` carries an ns form, a
  ;; leading comment and two more top-level forms; the edit touches exactly one
  ;; of them, and the same aggressive formatter runs.
  (let [tabbing-formatter
        (fn [_root _command future-sources]
          {:ok true :status :complete
           :file-count (count future-sources)
           :changed-file-count 1 :elapsed_ms 0.1
           :future-sources
           (into {} (map (fn [[file source]]
                           [file (str/replace source "  " "\t\t")]))
                 future-sources)})
        workspace (temp-workspace)
        target (io/file workspace "src/app/a.clj")]
    (try
      (let [result
            (mcp-tool/execute-request!
              {:project-root (.getPath workspace)
               :receipt-dir (.getPath (io/file workspace "receipts"))
               :formatter ["fixture-formatter" "{files}"]
               :format-candidates! tabbing-formatter}
              {"changes" [{"id" "widen"
                           "files" ["src/app/a.clj"]
                           "forms" ["b"]
                           "find" "#{1}"
                           "replace" "#{2}"
                           "expect" {"matches" 1 "each_form" 1 "each_file" 1}}]
               "expect" {"changes" 1 "edits" 1 "files" 1}})
            after (slurp target)]
        (is (:ok result) (pr-str result))
        (is (= 0 (:byte_drift_from_expected result)))
        (is (str/includes? after ";; a leading comment that must not move")
            "a comment between forms is not part of any form")
        (is (str/includes? after "(def config {:a 1})")
            "an untouched form keeps its bytes")
        (is (str/includes? after "  (:require [clojure.string :as str]")
            "so does the ns form the change never named")
        (is (str/includes? after "#{2}")
            "and the requested change did land"))
      (finally (delete-tree! workspace)))))

;; @spec MCP-OP-CLOSE-021
(deftest a-winner-commits-with-both-drift-numbers-zero
  (let [{:keys [result bytes-written? after]}
        (through-the-server
          {"edits" [{"file" "src/app/a.clj"
                     "within" {"form" "b"}
                     "from" "#{1}"
                     "to" "#{2}"}]})]
    (is (:ok result) (pr-str result))
    (is bytes-written?)
    (is (= 0 (:byte_drift_outside_span result)))
    (is (= 0 (:byte_drift_from_expected result)))
    (is (str/includes? after "(set/union #{y}   #{2})")
        "the requested bytes landed and nothing else moved")))

;; @spec MCP-OP-CLOSE-022
(deftest every-staged-file-must-be-measurable-not-merely-those-in-the-guard
  (let [gate (requiring-resolve
               'clj-surgeon.intent-transaction/gate-splice-drift)]
    (testing "a staged file absent from the guard names itself and refuses"
      (let [refusal (gate {:ok true
                           :future-sources {"a.clj" "x" "b.clj" "y"}
                           :splice-guard {"a.clj" {:reference "x" :spans []}}}
                          true)]
        (is (false? (:ok refusal)))
        (is (= :splice-guard-missing (:error-type refusal)))
        (is (= "b.clj" (:file refusal))
            "walking the guard instead of the staged files commits b.clj
             unmeasured")))
    (testing "a guard entry with no reference bytes is typed, never an exception"
      (let [refusal (gate {:ok true
                           :future-sources {"a.clj" "x"}
                           :splice-guard {"a.clj" {:reference nil :spans []}}}
                          true)]
        (is (false? (:ok refusal)))
        (is (= :splice-guard-missing (:error-type refusal)))
        (is (= "a.clj" (:file refusal)))))
    (testing "an exemption recorded in the guard is honoured"
      (is (:ok (gate {:ok true
                      :future-sources {"new.clj" "anything"}
                      :splice-guard {"new.clj" {:exempt :created-file}}}
                     true))))
    (testing "absence is never an exemption"
      (is (false? (:ok (gate {:ok true
                              :future-sources {"new.clj" "anything"}
                              :splice-guard {}}
                             true)))))))
