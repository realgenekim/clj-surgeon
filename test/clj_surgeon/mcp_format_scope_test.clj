(ns clj-surgeon.mcp-format-scope-test
  "End-to-end witnesses for scoping the managed formatter to the edited forms.

   Evidence: docs/observations/2026-09-02-captains-log-the-big-aha-and-reset.md,
   \"churn attributed\" — on the `l1` cohort the staging formatter reformatted
   whole files, turning 93 lines of work into +508/-476. Design:
   docs/intent/mcp-operation-contract/format-scope-design.md."
  (:require
   [clj-surgeon.mcp-change-buffer :as change-buffer]
   [clj-surgeon.mcp-formatter :as formatter]
   [clj-surgeon.mcp-tool :as mcp-tool]
   [clj-surgeon.structural-lens :as structural-lens]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

;; ---------------------------------------------------------------------------
;; The l1 churn fixture
;;
;; A file with an ns form carrying unsorted requires, a comment block between
;; forms, and two defns whose spacing a formatter would happily normalize. The
;; change adds one require. Before this bead the whole file went through the
;; formatter; the witness below measures what that would still cost and proves
;; the scoped stage does not pay it.

(def ^:private l1-source
  (str "(ns app.reducer-session\n"
       "  (:require [clojure.string :as str]\n"
       "            [clojure.set :as set])\n"
       "  (:import (java.util Date)))\n"
       "\n"
       ";; a comment block between forms\n"
       ";; that a formatter must never see\n"
       "\n"
       "(defn session-id [x]\n"
       "  (str/join \",\"    [x x]))\n"
       "\n"
       "(defn merge-sessions [a b]\n"
       "      (set/union #{a}   #{b}))\n"))

(defn- reindent-clauses
  "Re-lay continuation lines to a fixed indent — pure layout, exactly what a
   formatter is for, and admissible under the clause-normalised stream."
  [text]
  (str/replace text #"\n\s+\[" "\n   ["))

(defn- squeeze-spaces
  "The other thing a whole-file formatter does to untouched source."
  [text]
  (str/replace text #"(?m)([^ \n])  +([^ \n])" "$1 $2"))

(defn- l1-formatter
  "A deterministic stand-in for the managed formatter. It changes layout in
   every kind of form, so scope is the only thing that can bound it, and it
   changes nothing else, so it is admissible under MCP-OP-FMT-005.

   It deliberately does NOT sort `:require` clauses. The sanctioned reorder is
   the one behaviour a fixture cannot honestly imitate, so it is witnessed
   against the real pinned binary instead — `mcp_format_scope_real_test`."
  [_root _command sources]
  {:ok true :status :complete
   :file-count (count sources)
   :changed-file-count (count sources)
   :elapsed_ms 0.1
   :future-sources (update-vals sources (comp squeeze-spaces reindent-clauses))})

(defn- temp-dir
  [label]
  (let [dir (io/file (str (System/getProperty "java.io.tmpdir")
                          "/clj-surgeon-fmt-scope-" label "-" (System/nanoTime)))]
    (.mkdirs (io/file dir "src/app"))
    dir))

(defn- delete-tree!
  [dir]
  (doseq [file (reverse (file-seq dir))]
    (io/delete-file file true)))

(defn- utf8-bytes
  [^String text]
  (alength (.getBytes text "UTF-8")))

(defn- changed-bytes
  "UTF-8 bytes that differ between two texts, both sides charged, the same way
   `splice-drift/text-difference` charges them."
  [a b]
  (if (= a b)
    0
    (let [prefix (count (take-while true? (map = a b)))
          tail-a (subs a prefix)
          tail-b (subs b prefix)
          suffix (count (take-while true? (map = (reverse tail-a)
                                             (reverse tail-b))))]
      (+ (utf8-bytes (subs tail-a 0 (- (count tail-a) suffix)))
         (utf8-bytes (subs tail-b 0 (- (count tail-b) suffix)))))))

;; @spec MCP-OP-FMT-002
;; @spec MCP-OP-FMT-004
;; @spec MCP-OP-FMT-008
(deftest the-l1-churn-fixture-reformats-only-the-ns-form
  ;; The measured l1 shape: one require added to an ns form. It is witnessed at
  ;; `format-scoped-candidates!` rather than through `apply_clojure_changes`
  ;; because the wire shape that expresses it — an owner of kind `namespace`
  ;; with `find` plus `insert_after` — is a closed loser and is refused before
  ;; any source is read (MCP-OP-CLOSE-001). The splice guard below is exactly
  ;; what `compile-file` builds for that one raw insertion: the post-image, and
  ;; the inserted region's span in the post-image's own coordinates.
  (let [insertion "\n            [app.clock :as clock]"
        anchor "[clojure.set :as set]"
        offset (+ (str/index-of l1-source anchor) (count anchor))
        post (str (subs l1-source 0 offset) insertion (subs l1-source offset))
        guard {"src/app/reducer_session.clj"
               {:reference post
                :spans [{:offset offset :length (count insertion)}]}}
        whole-file (get (:future-sources
                          (l1-formatter nil nil {"x" post}))
                        "x")
        result (formatter/format-scoped-candidates!
                 "/tmp" ["fixture-formatter" "{files}"]
                 {"src/app/reducer_session.clj" post}
                 guard
                 l1-formatter)
        after (get (:future-sources result) "src/app/reducer_session.clj")]
    (is (:ok result) (pr-str result))
    (is (= 1 (:form-count result)) "one form was staged: the ns form")
    (is (= 1 (:changed-form-count result)))
    (testing "the requested require survived"
      (is (str/includes? after "[app.clock :as clock]")))
    (testing "only the ns form's bytes changed"
      (is (str/includes? after ";; a comment block between forms")
          "the comment block is byte-identical")
      (is (str/includes? after "  (str/join \",\"    [x x]))")
          "the first defn keeps its odd spacing verbatim")
      (is (str/includes? after "      (set/union #{a}   #{b}))")
          "so does the second")
      (let [tail (subs l1-source (str/index-of l1-source "\n\n;; a comment"))]
        (is (str/ends-with? after tail)
            "every byte after the ns form is unchanged")))
    (testing "the whole-file stage this replaces would still churn"
      (let [scoped-churn (changed-bytes post after)
            whole-churn (changed-bytes post whole-file)]
        (is (pos? whole-churn))
        (is (< scoped-churn whole-churn)
            (str "scoped " scoped-churn " vs whole-file " whole-churn))
        (println "  l1 fixture:" (utf8-bytes post) "bytes staged,"
                 (utf8-bytes after) "after; scoped stage changed" scoped-churn
                 "bytes, a whole-file stage would change" whole-churn)))
    (testing "the guard it hands on is the post-format image and its form spans"
      (let [entry (get (:splice-guard result)
                       "src/app/reducer_session.clj")]
        (is (= after (:reference entry)))
        (is (= 1 (count (:spans entry))))
        (is (= (structural-lens/source-hash after)
               (structural-lens/source-hash (:reference entry))))))))

;; @spec MCP-OP-FMT-002
(deftest an-edit-inside-one-defn-reformats-only-that-defn
  (let [workspace (temp-dir "defn")
        target (io/file workspace "src/app/reducer_session.clj")]
    (try
      (spit target l1-source)
      (let [result
            (mcp-tool/execute-request!
              {:project-root (.getPath workspace)
               :receipt-dir (.getPath (io/file workspace "receipts"))
               :formatter ["fixture-formatter" "{files}"]
               :format-candidates! l1-formatter}
              {"changes"
               [{"id" "widen"
                 "files" ["src/app/reducer_session.clj"]
                 "forms" ["merge-sessions"]
                 "find" "#{b}"
                 "replace" "#{b 1}"
                 "expect" {"matches" 1 "each_form" 1 "each_file" 1}}]
               "expect" {"changes" 1 "edits" 1 "files" 1}})
            after (slurp target)]
        (is (:ok result) (pr-str result))
        (is (= 0 (:byte_drift_from_expected result)))
        (is (str/includes? after "#{b 1}"))
        (is (str/includes? after "  (:require [clojure.string :as str]\n")
            "the ns form the change never named keeps its unsorted requires")
        (is (str/includes? after "  (str/join \",\"    [x x]))")
            "and the other defn keeps its spacing")
        (is (str/includes? after "(set/union #{a} #{b 1})")
            "while the edited defn was reformatted"))
      (finally (delete-tree! workspace)))))

;; @spec MCP-OP-FMT-002
;; @spec MCP-OP-FMT-003
(deftest two-edits-in-two-forms-format-both-and-nothing-between-them
  (let [workspace (temp-dir "two")
        target (io/file workspace "src/app/reducer_session.clj")]
    (try
      (spit target l1-source)
      (let [result
            (mcp-tool/execute-request!
              {:project-root (.getPath workspace)
               :receipt-dir (.getPath (io/file workspace "receipts"))
               :formatter ["fixture-formatter" "{files}"]
               :format-candidates! l1-formatter}
              {"changes"
               [{"id" "first"
                 "files" ["src/app/reducer_session.clj"]
                 "forms" ["session-id"]
                 "find" "\",\""
                 "replace" "\";\""
                 "expect" {"matches" 1 "each_form" 1 "each_file" 1}}
                {"id" "second"
                 "files" ["src/app/reducer_session.clj"]
                 "forms" ["merge-sessions"]
                 "find" "#{b}"
                 "replace" "#{b 1}"
                 "expect" {"matches" 1 "each_form" 1 "each_file" 1}}]
               "expect" {"changes" 2 "edits" 2 "files" 1}})
            after (slurp target)]
        (is (:ok result) (pr-str result))
        (is (= 0 (:byte_drift_from_expected result)))
        (is (= 2 (get-in result [:format :form-count])))
        (is (str/includes? after "\";\""))
        (is (str/includes? after "#{b 1}"))
        (is (str/includes? after ";; a comment block between forms")
            "the comment block between the two edited forms is untouched")
        (is (str/includes? after "  (:require [clojure.string :as str]\n")
            "and so is the ns form"))
      (finally (delete-tree! workspace)))))

;; @spec MCP-OP-FMT-003
(deftest a-form-that-grows-does-not-corrupt-the-form-after-it
  (let [grow-formatter
        (fn [_root _command sources]
          {:ok true :status :complete :file-count (count sources)
           :changed-file-count (count sources) :elapsed_ms 0.1
           ;; Re-lay every form across more lines: each one gets longer, so a
           ;; splice in ascending order would write the second form at an
           ;; offset the first one already moved.
           :future-sources (update-vals sources
                                        #(str/replace % #"\s+" "\n  "))})
        workspace (temp-dir "grow")
        target (io/file workspace "src/app/grow.clj")]
    (try
      ;; Two forms separated by a comment: a splice in ascending order would
      ;; write the second form at an offset the first one already moved.
      (spit target "(def a  1)\n;; a gap comment\n(def b  2)\n")
      (let [result
            (mcp-tool/execute-request!
              {:project-root (.getPath workspace)
               :receipt-dir (.getPath (io/file workspace "receipts"))
               :formatter ["fixture-formatter" "{files}"]
               :format-candidates! grow-formatter}
              {"changes"
               [{"id" "a"
                 "files" ["src/app/grow.clj"]
                 "forms" ["a"]
                 "find" "1"
                 "replace" "11"
                 "expect" {"matches" 1 "each_form" 1 "each_file" 1}}
                {"id" "b"
                 "files" ["src/app/grow.clj"]
                 "forms" ["b"]
                 "find" "2"
                 "replace" "22"
                 "expect" {"matches" 1 "each_form" 1 "each_file" 1}}]
               "expect" {"changes" 2 "edits" 2 "files" 1}})
            after (slurp target)]
        (is (:ok result) (pr-str result))
        (is (= "(def\n  a\n  11)\n;; a gap comment\n(def\n  b\n  22)\n" after)
            "both forms grew, the comment between them stayed put, and neither
             splice landed at a moved offset")
        (is (= 0 (:byte_drift_from_expected result))))
      (finally (delete-tree! workspace)))))

;; @spec MCP-OP-FMT-002
(deftest the-require-change-verb-across-three-namespaces-still-commits-clean
  ;; The measured winner (l1 Y-5: nine namespaces, zero churn). Editor gestures
  ;; remain exempt from the formatter — that exemption predates the churn
  ;; finding and is not this leaf's to lift — so this proves the exemption
  ;; survived the rewiring: no formatter runs and both drift numbers stay zero.
  (let [workspace (temp-dir "require")
        files ["src/app/one.clj" "src/app/two.clj" "src/app/three.clj"]
        formatter-calls (atom 0)]
    (try
      (doseq [[index file] (map-indexed vector files)]
        (spit (io/file workspace file)
              (str "(ns app." (name (nth [:one :two :three] index)) "\n"
                   "  (:require [app.old :as old]))\n"
                   "\n"
                   "(defn go [] (old/run    1))\n")))
      (let [before (mapv #(slurp (io/file workspace %)) files)
            result
            (mcp-tool/execute-request!
              {:project-root (.getPath workspace)
               :receipt-dir (.getPath (io/file workspace "receipts"))
               :formatter ["fixture-formatter" "{files}"]
               :format-candidates! (fn [& args]
                                     (swap! formatter-calls inc)
                                     (apply l1-formatter args))}
              {"symbol_migration"
               {"target_alias" "clock"
                "target_rule" "preserve-name"
                "columns" ["owner" "from" "matches"]
                "files" (mapv (fn [file] [file [["go" "old/run" 1]]]) files)}
               "require_change"
               {"add" {"lib" "app.clock" "as" "clock"}
                "files" (mapv (fn [file]
                                {"file" file
                                 "remove" {"lib" "app.old" "as" "old"}})
                              files)}})]
        (is (:ok result) (pr-str result))
        (is (zero? @formatter-calls)
            "editor gestures stay exempt; that exemption predates this bead")
        (is (= 0 (:byte_drift_from_expected result)))
        (is (= 0 (:byte_drift_outside_span result)))
        (doseq [[index file] (map-indexed vector files)]
          (let [after (slurp (io/file workspace file))]
            (is (str/includes? after "[app.clock :as clock]"))
            (is (str/includes? after "(clock/run    1)")
                (str file " keeps every byte the change did not name"))
            (is (not= (nth before index) after)))))
      (finally (delete-tree! workspace)))))

;; @spec MCP-OP-FMT-007
(deftest the-prepared-basis-route-formats-under-the-same-scope
  ;; Round two of close-losers disabled this route's formatter outright,
  ;; because the only formatter the server had restaged whole files. It is on
  ;; again here, under the top-level-form scope.
  (change-buffer/clear-bases!)
  (let [workspace (temp-dir "basis")
        target (io/file workspace "src/app/reducer_session.clj")
        _ (spit target l1-source)
        source-hash (structural-lens/source-hash l1-source)
        session "fmt-scope-session"
        reference {:lsp_session session
                   :file "src/app/reducer_session.clj"
                   :file_path (.getCanonicalPath target)
                   :source_sha256 source-hash
                   :owner "merge-sessions"
                   :owner_details {:name "merge-sessions" :start_line 12}
                   :range {:start {:line 12 :character 6}
                           :end {:line 12 :character 15}}
                   :line 13 :character 7}
        prepared (change-buffer/prepare-change!
                   {:project-root (.getPath workspace)
                    :semantic-resolver
                    (fn [_]
                      {:ok true
                       :version 2
                       :lsp_session session
                       :definition (assoc reference :name "merge-sessions")
                       :references [reference]})}
                   {:subject "app.reducer-session/merge-sessions"
                    :intent "widen the union"})]
    (try
      (is (:ok prepared) (pr-str prepared))
      (let [site (first (:decision-sites prepared))
            result
            (mcp-tool/execute-request!
              {:project-root (.getPath workspace)
               :receipt-dir (.getPath (io/file workspace "receipts"))
               :formatter ["fixture-formatter" "{files}"]
               :format-candidates! l1-formatter
               :verification-profiles {"fast" {:commands ["ignored"]}}
               :verify! (fn [_ profile _ _]
                          {:ok true :profile profile :checks []})}
              {"basis" (:basis prepared)
               "decisions" [{"site" (:id site)
                             "replace"
                             (str "(defn merge-sessions [a b]\n"
                                  "      (set/union #{a}   #{b 1}))")}]})
            after (slurp target)]
        (is (:ok result) (pr-str result))
        (is (= 0 (:byte_drift_from_expected result))
            "the basis route commits with the formatter on")
        (is (str/includes? after "#{b 1}"))
        (is (str/includes? after ";; a comment block between forms")
            "the comment block between forms is untouched")
        (is (str/includes? after "  (:require [clojure.string :as str]\n")
            "and so is the ns form this change never named"))
      (finally
        (change-buffer/clear-bases!)
        (delete-tree! workspace)))))

;; @spec MCP-OP-FMT-005
(deftest a-formatter-that-changes-code-rather-than-layout-is-refused
  (let [workspace (temp-dir "altered")
        target (io/file workspace "src/app/a.clj")
        before "(ns app.a)\n\n(defn go [] :old)\n"
        rewriting (fn [_root _command sources]
                    {:ok true :status :complete :file-count (count sources)
                     :changed-file-count 1 :elapsed_ms 0.1
                     :future-sources (update-vals
                                       sources
                                       #(str/replace % ":new" ":smuggled"))})]
    (try
      (spit target before)
      (let [result
            (mcp-tool/execute-request!
              {:project-root (.getPath workspace)
               :receipt-dir (.getPath (io/file workspace "receipts"))
               :formatter ["fixture-formatter" "{files}"]
               :format-candidates! rewriting}
              {"changes"
               [{"id" "go"
                 "files" ["src/app/a.clj"]
                 "forms" ["go"]
                 "find" ":old"
                 "replace" ":new"
                 "expect" {"matches" 1 "each_form" 1 "each_file" 1}}]
               "expect" {"changes" 1 "edits" 1 "files" 1}})]
        (is (false? (:ok result)) (pr-str result))
        (is (= "format-altered-form" (:error_type result)))
        (is (true? (:source_unchanged result)))
        (is (false? (:mutation_attempted result)))
        (is (= before (slurp target))))
      (finally (delete-tree! workspace)))))

;; @spec MCP-OP-FMT-009
(deftest a-formatter-that-fails-refuses-the-whole-transaction
  (let [workspace (temp-dir "failed")
        target (io/file workspace "src/app/a.clj")
        before "(ns app.a)\n\n(defn go [] :old)\n"]
    (try
      (spit target before)
      (doseq [[label process expected]
              [["failure" {:finished? true :exit 2 :elapsed_ms 1.0
                           :output "bad"} "formatter-failed"]
               ["timeout" {:finished? false :exit nil :elapsed_ms 120000.0
                           :output "late"} "formatter-timeout"]]]
        (testing label
          (let [result
                (mcp-tool/execute-request!
                  {:project-root (.getPath workspace)
                   :receipt-dir (.getPath (io/file workspace "receipts"))
                   :formatter ["fixture-formatter" "{files}"]
                   :format-candidates!
                   (fn [root command sources]
                     (formatter/format-candidates!
                       root command sources (fn [_ _] process)))}
                  {"changes"
                   [{"id" "go"
                     "files" ["src/app/a.clj"]
                     "forms" ["go"]
                     "find" ":old"
                     "replace" ":new"
                     "expect" {"matches" 1 "each_form" 1 "each_file" 1}}]
                   "expect" {"changes" 1 "edits" 1 "files" 1}})]
            (is (false? (:ok result)) (pr-str result))
            (is (= expected (:error_type result)))
            (is (true? (:source_unchanged result)))
            (is (= before (slurp target))))))
      (finally (delete-tree! workspace)))))

;; @spec MCP-OP-FMT-006
(deftest a-staged-file-with-no-guard-is-refused-rather-than-formatted-whole
  (testing "absence of a reference refuses; it is never an unscoped format"
    (let [result (formatter/format-scoped-candidates!
                   "/tmp" ["fixture" "{files}"]
                   {"a.clj" "(def x 1)\n"}
                   {}
                   (fn [& _] (throw (ex-info "formatter ran" {}))))]
      (is (false? (:ok result)))
      (is (= :format-scope-unmeasurable (:error-type result)))
      (is (= "a.clj" (:file result)))
      (is (true? (:source-unchanged result)))))
  (testing "a guard entry with no reference bytes is typed, never an exception"
    (let [result (formatter/format-scoped-candidates!
                   "/tmp" ["fixture" "{files}"]
                   {"a.clj" "(def x 1)\n"}
                   {"a.clj" {:reference nil :spans []}}
                   (fn [& _] (throw (ex-info "formatter ran" {}))))]
      (is (false? (:ok result)))
      (is (= :format-scope-unmeasurable (:error-type result)))))
  (testing "an exemption recorded in the guard leaves the file unformatted"
    (let [result (formatter/format-scoped-candidates!
                   "/tmp" ["fixture" "{files}"]
                   {"new.clj" "(def x 1)\n"}
                   {"new.clj" {:exempt :created-file}}
                   (fn [& _] (throw (ex-info "formatter ran" {}))))]
      (is (:ok result))
      (is (= 0 (:form-count result)))
      (is (= {"new.clj" "(def x 1)\n"} (:future-sources result)))))
  (testing "an edit that encloses no form stages nothing and runs no formatter"
    (let [result (formatter/format-scoped-candidates!
                   "/tmp" ["fixture" "{files}"]
                   {"a.clj" "(def x 1)\n\n(def y 2)\n"}
                   {"a.clj" {:reference "(def x 1)\n\n(def y 2)\n"
                             :spans [{:offset 10 :length 0}]}}
                   (fn [& _] (throw (ex-info "formatter ran" {}))))]
      (is (:ok result))
      (is (= 0 (:form-count result))))))

;; @spec MCP-OP-FMT-005
(deftest a-formatter-that-returns-something-other-than-one-form-is-refused
  (let [result (formatter/format-scoped-candidates!
                 "/tmp" ["fixture" "{files}"]
                 {"a.clj" "(def x 1)\n"}
                 {"a.clj" {:reference "(def x 1)\n"
                           :spans [{:offset 0 :length 9}]}}
                 (fn [_ _ sources]
                   {:ok true :future-sources
                    (update-vals sources (constantly "(def x 1)\n(def y 2)\n"))}))]
    (is (false? (:ok result)))
    (is (= :format-fragment-not-one-form (:error-type result)))
    (is (true? (:source-unchanged result)))))

;; @spec MCP-OP-FMT-005
(deftest a-bag-preserving-semantic-rewrite-does-not-commit
  ;; Red-team probes p2 and p2b. Both doubles preserve the token MULTISET of
  ;; the form they are given, so the check this leaf shipped first admitted
  ;; them and p2b landed the corruption on disk. Both are refused now, and both
  ;; are driven through `execute-request!` so the claim is about the server and
  ;; not about a pure function.
  (testing "p2: swap-if-branches inside the very form the change edits"
    (let [workspace (temp-dir "swap-if")
          target (io/file workspace "src/app/t.clj")
          before (str "(ns app.t)\n\n(defn authorize [user]\n"
                      "  (audit 1)\n  (if (admin? user) (grant) (deny)))\n")
          swap-if (fn [_root _command sources]
                    {:ok true :status :complete :file-count (count sources)
                     :changed-file-count 1 :elapsed_ms 0.1
                     :future-sources
                     (update-vals sources
                                  #(str/replace
                                     % "(if (admin? user) (grant) (deny))"
                                     "(if (admin? user) (deny) (grant))"))})]
      (try
        (spit target before)
        (let [result
              (mcp-tool/execute-request!
                {:project-root (.getPath workspace)
                 :receipt-dir (.getPath (io/file workspace "receipts"))
                 :formatter ["fixture-formatter" "{files}"]
                 :format-candidates! swap-if}
                {"changes"
                 [{"id" "audit"
                   "files" ["src/app/t.clj"]
                   "forms" ["authorize"]
                   "find" "(audit 1)"
                   "replace" "(audit 2)"
                   "expect" {"matches" 1 "each_form" 1 "each_file" 1}}]
                 "expect" {"changes" 1 "edits" 1 "files" 1}})]
          (is (false? (:ok result)) (pr-str result))
          (is (= "format-altered-form" (:error_type result)))
          (is (true? (:source_unchanged result)))
          (is (false? (:mutation_attempted result)))
          (is (= before (slurp target))
              "the file on disk is unchanged, byte for byte"))
        (finally (delete-tree! workspace)))))
  (testing "p2b: a :refer symbol moved between two sibling require clauses"
    ;; The clause normalisation is what makes a sorted `:require` admissible,
    ;; so it is also the one place a symbol could hide while moving. It cannot:
    ;; whole clause SUBTREES are sorted, so a clause changing position is
    ;; normalised away and a symbol changing clause is not.
    ;;
    ;; The list under test lives inside a defn rather than an ns form because
    ;; no accepted wire shape edits an ns form — `owner {kind namespace}` is a
    ;; closed loser (MCP-OP-CLOSE-001), and the prepared-basis route refuses to
    ;; address inside one (`semantic-owner-not-found`, measured 2026-09-02).
    ;; The check does not look at where the clause list sits, so this is the
    ;; same code path an ns form takes.
    (let [workspace (temp-dir "refer-swap")
          target (io/file workspace "src/app/t.clj")
          before (str "(ns app.t)\n\n"
                      "(defn config []\n"
                      "  (audit 1)\n"
                      "  (:require [app.safe :refer [check]]\n"
                      "            [app.unsafe :refer []]))\n")
          swap-refer (fn [_root _command sources]
                       {:ok true :status :complete :file-count (count sources)
                        :changed-file-count 1 :elapsed_ms 0.1
                        :future-sources
                        (update-vals
                          sources
                          #(-> %
                               (str/replace "app.safe :refer [check]"
                                            "app.safe :refer []")
                               (str/replace "app.unsafe :refer []"
                                            "app.unsafe :refer [check]")))})]
      (try
        (spit target before)
        (let [result
              (mcp-tool/execute-request!
                {:project-root (.getPath workspace)
                 :receipt-dir (.getPath (io/file workspace "receipts"))
                 :formatter ["fixture-formatter" "{files}"]
                 :format-candidates! swap-refer}
                {"changes"
                 [{"id" "audit"
                   "files" ["src/app/t.clj"]
                   "forms" ["config"]
                   "find" "(audit 1)"
                   "replace" "(audit 2)"
                   "expect" {"matches" 1 "each_form" 1 "each_file" 1}}]
                 "expect" {"changes" 1 "edits" 1 "files" 1}})
              after (slurp target)]
          (is (false? (:ok result)) (pr-str result))
          (is (= "format-altered-form" (:error_type result)))
          (is (true? (:source_unchanged result)))
          (is (= before after)
              "the corruption p2b landed on disk does not land any more")
          (is (not (str/includes? after "app.unsafe :refer [check]"))))
        (finally (delete-tree! workspace))))))

;; @spec MCP-OP-FMT-010
(deftest a-file-churned-before-the-formatter-is-refused-not-laundered
  ;; Red-team probe p3(c): without this the scoped format rewrites the guard to
  ;; point at the churned image, and the commit gate that exists to catch the
  ;; churn measures against it and reports drift 0.
  (let [reference "(def a 1)\n\n;; important comment\n\n(def b 2)\n"
        churned "(def a 1)\n\n;; IMPORTANT COMMENT WAS REWRITTEN\n\n(def b 2)\n"
        result (formatter/format-scoped-candidates!
                 "/tmp" ["fixture" "{files}"]
                 {"t.clj" churned}
                 {"t.clj" {:reference reference
                           :spans [{:offset 7 :length 1}]}}
                 (fn [& _] (throw (ex-info "formatter ran" {}))))]
    (is (false? (:ok result)))
    (is (= :format-scope-candidate-mismatch (:error-type result)))
    (is (= "t.clj" (:file result)))
    (is (true? (:source-unchanged result)))
    (is (nil? (:splice-guard result))
        "and no guard is handed on, so nothing downstream measures the churn")))

;; @spec MCP-OP-FMT-011
(deftest an-unparseable-staged-file-is-refused-even-beside-a-good-one
  ;; Red-team probe p4(b2): the good file's forms make the reduce run, and the
  ;; bad file's guard is quietly rewritten to the unparseable churned bytes.
  (let [good "(def a 1)\n"
        result (formatter/format-scoped-candidates!
                 "/tmp" ["fixture" "{files}"]
                 {"good.clj" good
                  "bad.clj" "(def x 1\n;; CHURNED BY AN EARLIER STAGE\n"}
                 {"good.clj" {:reference good :spans [{:offset 5 :length 1}]}
                  "bad.clj" {:reference "(def x 1)\n"
                             :spans [{:offset 5 :length 1}]}}
                 (fn [& _] (throw (ex-info "formatter ran" {}))))]
    (is (false? (:ok result)))
    (is (contains? #{:format-scope-candidate-mismatch
                     :format-scope-unparseable-candidate}
                   (:error-type result)))
    (is (= "bad.clj" (:file result)))
    (is (nil? (:splice-guard result))
        "the whole transaction is refused; no file's guard is replaced")))

;; @spec MCP-OP-FMT-009
(deftest a-formatter-that-returns-nil-or-throws-is-a-typed-refusal
  ;; Red-team probe p4: both paths returned an UNTYPED error reporting
  ;; source_unchanged false, while the file on disk was in fact unchanged — a
  ;; receipt that told the caller the opposite of the truth.
  (let [before "(ns app.t)\n\n(defn go []\n  (audit 1)\n  (inc 1))\n"
        change {"id" "c" "files" ["src/app/t.clj"] "forms" ["go"]
                "find" "(audit 1)" "replace" "(audit 2)"
                "expect" {"matches" 1 "each_form" 1 "each_file" 1}}]
    (doseq [[label double]
            [["returns nil" (fn [& _] nil)]
             ["returns a non-map" (fn [& _] :not-a-map)]
             ["throws" (fn [& _] (throw (ex-info "formatter exploded" {})))]]]
      (testing label
        (let [workspace (temp-dir "misbehave")
              target (io/file workspace "src/app/t.clj")]
          (try
            (spit target before)
            (let [result
                  (mcp-tool/execute-request!
                    {:project-root (.getPath workspace)
                     :receipt-dir (.getPath (io/file workspace "receipts"))
                     :formatter ["fixture-formatter" "{files}"]
                     :format-candidates! double}
                    {"changes" [change]
                     "expect" {"changes" 1 "edits" 1 "files" 1}})]
              (is (false? (:ok result)) (pr-str result))
              (is (= "formatter-failed" (:error_type result)))
              (is (true? (:source_unchanged result))
                  "the file is unchanged and the receipt must say so")
              (is (= before (slurp target))))
            (finally (delete-tree! workspace))))))))
