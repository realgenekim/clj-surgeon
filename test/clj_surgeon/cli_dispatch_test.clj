(ns clj-surgeon.cli-dispatch-test
  "CLI dispatch regression tests (kc-0nc, ed6ad99 follow-up).

   Covers what help_test.clj's regression block doesn't:
   - help/parser ANTI-DRIFT: ops are extracted FROM the rendered global help text
     and asserted to resolve — if help ever mentions an op the parser can't
     dispatch, this fails (the reverse direction of registry->help checks).
   - the {:error} path of `run` as a unit: unknown ops (string AND keyword)
     print a clean EDN error map and never throw.
   - subprocess CLI with BARE STRING ops — the exact invocation shape that
     threw ClassCastException before ed6ad99 (`:op ls-tree`, no leading colon)."
  (:require
   [babashka.process :as proc]
   [clj-surgeon.core :as core]
   [clj-surgeon.forward-refs :as fwd]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

;; ============================================================
;; help/parser anti-drift — every op the help text shows must resolve
;; ============================================================

(defn- ops-mentioned-in-help
  "Extract op names from the rendered global help: the left-column command
   names (4-space-indented first token) plus every name inside an
   `(aliases: ...)` group."
  [help]
  (let [lines       (str/split-lines help)
        cmd-names   (->> lines
                         (keep #(second (re-find #"^\s{4}(\S+)\s" %)))
                         ;; drop non-op lines that share the indent (usage/examples)
                         (remove #(str/starts-with? % "clj-surgeon"))
                         (remove #(str/starts-with? % ":op")))
        alias-names (->> (re-seq #"\(aliases?: ([^)]+)\)" help)
                         (mapcat (fn [[_ g]] (map str/trim (str/split g #",")))))]
    (->> (concat cmd-names alias-names)
         (map #(str/replace % #"^:" ""))
         (remove str/blank?)
         set)))

(deftest help-text-ops-all-resolve-anti-drift
  (let [help (core/format-global-help core/ops-registry)
        ops  (ops-mentioned-in-help help)]
    (testing "help text exposes exactly the preferred caller operations"
      (is (= (->> (keys core/ops-registry)
                  (remove core/hidden-from-primary-help)
                  (map (comp name core/public-op-name))
                  set)
             ops)))
    (testing "every op shown in help resolves as a bare STRING (CLI shape)"
      (doseq [op ops]
        (is (some? (core/resolve-op op))
            (str "help mentions '" op "' but resolve-op can't dispatch the string form"))))
    (testing "every op shown in help resolves as a KEYWORD"
      (doseq [op ops]
        (is (some? (core/resolve-op (keyword op)))
            (str "help mentions '" op "' but resolve-op can't dispatch the keyword form"))))))

(deftest every-registry-op-and-alias-resolves-as-bare-string
  (testing "string round-trip for the FULL registry, not samples — parser can
            dispatch every canonical op and alias exactly as a CLI would send it"
    (doseq [op-key (keys core/ops-registry)]
      (is (= op-key (core/resolve-op (name op-key)))
          (str "canonical op " op-key " does not resolve from its bare string")))
    (doseq [[alias-kw canonical] @#'core/alias->canonical]
      (is (= canonical (core/resolve-op (name alias-kw)))
          (str "alias " alias-kw " does not resolve from its bare string")))))

;; ============================================================
;; run — unknown op returns clean {:error}, never throws
;; ============================================================

(defn- run->output
  "Call core/run capturing stdout; returns the printed string.
   The assertion that this CALL RETURNS is itself the no-throw regression."
  [opts]
  (with-out-str (core/run opts)))

(deftest run-unknown-string-op-prints-error-map-no-throw
  (let [out (run->output {:op "bogus"})]
    (testing "prints an EDN map with :error (pre-ed6ad99 this threw ClassCastException)"
      (let [parsed (edn/read-string out)]
        (is (map? parsed))
        (is (contains? parsed :error))
        (is (str/includes? (:error parsed) "Unknown op"))))))

(deftest run-unknown-keyword-op-prints-error-map-no-throw
  (let [out (run->output {:op :bogus})
        parsed (edn/read-string out)]
    (is (map? parsed))
    (is (str/includes? (:error parsed) "Unknown op"))))

(deftest run-nil-and-junk-ops-print-error-no-throw
  (testing "nil, numbers, and garbage strings all reach the friendly error"
    (doseq [bad [nil 42 "" "no-such-op" :also-not-real]]
      (let [out (run->output {:op bad})]
        (is (str/includes? out "Unknown op")
            (str "op " (pr-str bad) " did not produce the clean error"))))))

(deftest run-error-lists-valid-ops
  (testing "the error names valid ops so the caller can self-correct"
    (let [out (run->output {:op "bogus"})]
      (is (str/includes? out ":ls"))
      (is (str/includes? out ":extract"))
      (is (str/includes? out ":cat"))
      (is (not (str/includes? out ":show-form"))))))

;; ============================================================
;; Subprocess CLI — bare-string ops end to end
;; (the documented usage `:op ls-tree` that crashed before ed6ad99)
;; ============================================================

(def ^:private project-src
  (let [this-file (io/file "test/clj_surgeon/cli_dispatch_test.clj")]
    (str (.getAbsolutePath
           (io/file (.getParentFile (.getParentFile (.getParentFile this-file))) "src")))))

(defn- run-cli
  [& args]
  (apply proc/shell {:out :string :err :string :continue true}
         "bb" "-cp" project-src "-m" "clj-surgeon.core" args))

(defn- run-outline-without-semantic-enrichment [operation]
  (let [fixture (java.io.File/createTempFile "clj-surgeon-cli-dispatch" ".clj")]
    (try
      (spit fixture "(defn dispatch-sentinel [] :ok)\n")
      (run-cli ":op" operation ":file" (.getAbsolutePath fixture))
      (finally
        (.delete fixture)))))

(deftest cli-bare-string-op-dispatches
  (let [{:keys [exit out err]} (run-outline-without-semantic-enrichment "ls")]
    (testing "documented bare-string usage works end to end"
      (is (zero? exit) (str "stderr: " err))
      (is (str/includes? out "dispatch-sentinel")))))

(deftest cli-bare-string-alias-dispatches
  (let [{:keys [exit out]} (run-outline-without-semantic-enrichment "outline")]
    (testing "bare-string ALIAS dispatches too"
      (is (zero? exit))
      (is (str/includes? out "dispatch-sentinel")))))

(deftest cli-bare-string-unknown-op-clean-error
  (let [{:keys [exit out err]} (run-cli ":op" "bogus")]
    (testing "the pre-fix crash shape now exits clean with the friendly error"
      (is (pos? exit))
      (is (str/includes? out "Unknown op"))
      (is (not (str/includes? (str out err) "ClassCastException"))
          "ClassCastException leaked to the user again"))))

(deftest cli-structural-errors-are-edn-and-exit-nonzero
  (let [tmp (java.io.File/createTempFile "clj-surgeon-cli-lens" ".clj")]
    (spit tmp "(ns x)\n(defn f [] [(inc 1) (inc 1)])\n")
    (try
      (testing "ambiguous replacement is a shell failure"
        (let [{:keys [exit out err]}
              (run-cli ":op" "replace-subform"
                       ":file" (.getAbsolutePath tmp)
                       ":inside" "f"
                       ":match" "(inc 1)"
                       ":with" "(bump 1)")
              result (edn/read-string out)]
          (is (pos? exit))
          (is (= 2 (:match-count result)))
          (is (contains? result :error))
          (is (str/blank? err))))
      (testing "reader errors are concise data, not stack traces"
        (let [{:keys [exit out err]}
              (run-cli ":op" "replace-subform"
                       ":file" (.getAbsolutePath tmp)
                       ":inside" "f"
                       ":match" "(inc 1)"
                       ":with" "[:button {:onclick \"\\x27\"}]")
              result (edn/read-string out)]
          (is (pos? exit))
          (is (= :invalid-replacement (:error-type result)))
          (is (not (str/includes? (str out err) "Stack trace")))
          (is (not (str/includes? (str out err) "ExceptionInfo")))))
      (testing "missing required lens forms are shell failures"
        (let [{:keys [exit out]}
              (run-cli ":op" "replace-subform"
                       ":file" (.getAbsolutePath tmp)
                       ":inside" "f"
                       ":match" "(inc 1)")
              result (edn/read-string out)]
          (is (pos? exit))
          (is (= :missing-arguments (:error-type result)))
          (is (= [:with] (:missing result)))))
      (finally
        (.delete tmp)))))

(deftest cli-replacement-apply-emits-a-read-back-verified-receipt
  (let [source-file (java.io.File/createTempFile "clj-surgeon-cli-receipt" ".clj")
        plan-file (java.io.File/createTempFile "clj-surgeon-cli-receipt" ".edn")]
    (spit source-file "(ns receipt)\n(defn finish [state]\n  (assoc state :status :done))\n")
    (try
      (let [planned (run-cli ":op" "replace-subform"
                             ":file" (.getAbsolutePath source-file)
                             ":inside" "finish"
                             ":match" "(assoc state :status :done)"
                             ":with" "(assoc state :status :complete)"
                             ":plan-out" (.getAbsolutePath plan-file))
            plan (edn/read-string (:out planned))
            applied (run-cli ":op" "replace-subform!"
                             ":plan" (.getAbsolutePath plan-file))
            receipt (edn/read-string (:out applied))]
        (is (zero? (:exit planned)) (:err planned))
        (is (zero? (:exit applied)) (:err applied))
        (is (= :replace-subform! (:operation receipt)))
        (is (= (.getAbsolutePath source-file) (:file receipt)))
        (is (= (:source-hash plan) (:source-hash receipt)))
        (is (= (:result-hash plan) (:result-hash receipt)))
        (is (= (:result-hash receipt)
               (get-in receipt [:verified :read-back-hash])))
        (is (true? (get-in receipt [:verified :whole-file-parsed])))
        (is (true? (get-in receipt [:verified :atomic-write])))
        (is (= "(assoc state :status :complete)"
               (get-in receipt [:applied-edit :after])))
        (is (str/includes? (slurp source-file) ":status :complete")))
      (finally
        (.delete source-file)
        (.delete plan-file)))))

(deftest cli-bare-string-op-help-resolves
  (let [{:keys [exit out]} (run-cli ":op" "tree" "--help")]
    (testing "--help with a bare-string alias shows the canonical op's help"
      (is (zero? exit))
      (is (str/includes? out "Map namespaces across a directory tree")))))

(deftest cli-recovery-commands-teach-the-complete-one-shot-contract
  (let [recover (run-cli "recover" "--help")
        report (run-cli "report-failure" "--help")]
    (is (zero? (:exit recover)))
    (is (str/includes? (:out recover) "one exact semantic surface"))
    (is (str/includes? (:out recover) "one guarded write"))
    (is (str/includes? (:out recover) "semantic-provider-warming"))
    (is (str/includes? (:out recover) "next_call"))
    (is (str/includes? (:out recover) "fallback-command"))
    (is (zero? (:exit report)))
    (is (str/includes? (:out report) "--receipt PATH"))
    (is (str/includes? (:out report) "Never uploads source"))))

(deftest cli-mv-refusal-is-edn-nonzero-and-preserves-source
  (let [source (slurp "test-fixtures/mv/mothership_stranded_dep.clj")
        tmp (java.io.File/createTempFile "clj-surgeon-cli-mv-guard" ".clj")]
    (spit tmp source)
    (try
      (let [{:keys [exit out err]}
            (run-cli ":op" "mv"
                     ":file" (.getAbsolutePath tmp)
                     ":form" "walk-files"
                     ":before" "run-kondo")
            result (edn/read-string out)]
        (is (pos? exit))
        (is (= :would-strand-dependencies (:error-type result)))
        (is (= ["skip-dirs"] (mapv :name (:stranded result))))
        (is (str/includes? (:recommended-command result)
                           ":op :mv-with-deps"))
        (is (str/ends-with? (:recommended-command result) ":dry-run true"))
        (is (= :preview-dependency-closure (:recommended-action result)))
        (is (str/includes? (:apply-command result) ":op :mv-with-deps"))
        (is (str/blank? err))
        (is (= source (slurp tmp))))
      (finally (.delete tmp)))))

(deftest cli-mv-with-deps-alias-injects-the-same-option-as-the-flag
  (let [source (slurp "test-fixtures/mv/mothership_stranded_dep.clj")
        alias-file (java.io.File/createTempFile "clj-surgeon-cli-mv-alias" ".clj")
        flag-file (java.io.File/createTempFile "clj-surgeon-cli-mv-flag" ".clj")]
    (spit alias-file source)
    (spit flag-file source)
    (try
      (let [alias-run (run-cli ":op" "mv-with-deps"
                               ":file" (.getAbsolutePath alias-file)
                               ":form" "walk-files"
                               ":before" "run-kondo"
                               ":dry-run" "true")
            flag-run (run-cli ":op" "mv"
                              ":file" (.getAbsolutePath flag-file)
                              ":form" "walk-files"
                              ":before" "run-kondo"
                              ":with-deps" "true"
                              ":dry-run" "true")
            alias-result (edn/read-string (:out alias-run))
            flag-result (edn/read-string (:out flag-run))
            contract-keys [:requested-forms :added-forms :move-order
                           :before :direction :with-deps :source-hash
                           :result-hash]]
        (is (zero? (:exit alias-run)))
        (is (zero? (:exit flag-run)))
        (is (= (select-keys (:plan alias-result) contract-keys)
               (select-keys (:plan flag-result) contract-keys)))
        (is (= ["skip-dirs"] (get-in alias-result [:plan :added-forms])))
        (is (true? (get-in alias-result [:plan :with-deps])))
        (is (str/includes? (:apply-command alias-result) ":op :mv-with-deps"))
        (is (= source (slurp alias-file)))
        (is (= source (slurp flag-file))))
      (finally
        (.delete alias-file)
        (.delete flag-file)))))

(deftest cli-mv-with-deps-alias-executes-the-disclosed-group
  (let [source (slurp "test-fixtures/mv/mothership_stranded_dep.clj")
        tmp (java.io.File/createTempFile "clj-surgeon-cli-mv-execute" ".clj")]
    (spit tmp source)
    (try
      (let [{:keys [exit out err]}
            (run-cli ":op" ":mv-with-deps"
                     ":file" (.getAbsolutePath tmp)
                     ":form" "walk-files"
                     ":before" "run-kondo")
            result (edn/read-string out)
            moved (slurp tmp)]
        (is (zero? exit))
        (is (:ok result))
        (is (= ["skip-dirs"] (get-in result [:plan :added-forms])))
        (is (< (str/index-of moved "(def skip-dirs")
               (str/index-of moved "(defn walk-files")))
        (is (< (str/index-of moved "(defn walk-files")
               (str/index-of moved "(defn run-kondo")))
        (is (str/blank? err)))
      (finally (.delete tmp)))))

;; ============================================================
;; rf2-2 — :ls never fails an outline that parses
;; Field provenance: cohort rf1, run rf1-g1-B-2 call 11 ran
;;   bb -m clj-surgeon.core :op :ls :file src/clj_surgeon/mcp_exact_verify.clj
;; on the file :extract! had just written and got
;;   {:error-type :forward-reference-analysis-failed :exit 2 :diagnostic ""
;;    :error "Forward-reference analysis failed"}
;; The same bytes (sha256 3e31539658283d67646e7fc37af4f5fc0854e903f8c8efaddd0effd6403e81d6)
;; outline cleanly through inspect_clojure. The agent believed the refusal and
;; spent 15 of its 48 returns reordering forms that were already in a valid
;; order. Cause: the extracted file carried four unused-import warnings, clj-kondo
;; exits non-zero when it has FINDINGS, and the analysis stage read that as
;; failure; the diagnostic was empty because findings go to stdout and it read
;; stderr.
;; ============================================================

(def ^:private rf2-lint-warning-fixture
  "A parseable namespace carrying exactly the field's defect shape: imports that
   nothing in the file references, which make clj-kondo exit non-zero."
  (str "(ns clj-surgeon-rf2-outline-fixture\n"
       "  (:import\n"
       "   (java.nio.file LinkOption Path Paths)\n"
       "   (java.util UUID)))\n"
       "\n"
       "(defn first-form [] :first)\n"
       "\n"
       "(defn second-form [] (first-form))\n"))

;; @spec MCP-OP-LS-002
(deftest ls-outlines-a-file-whose-forward-reference-analysis-fails
  (testing "a failed analysis decorates the outline; it never replaces it"
    ;; @spec MCP-OP-LS-002
    (let [outline {:ns 'app.core :file "src/app/core.clj" :lines 12
                   :form-count 2 :forms [{:name 'a} {:name 'b}]}
          decorated (core/outline-with-forward-refs
                      outline
                      {:ok false
                       :error-type :forward-reference-analysis-failed
                       :note "Forward-reference analysis was unavailable."})]
      (is (= :unavailable (:forward-refs decorated)))
      (is (string? (:note decorated)))
      (is (= 1 (count (str/split-lines (:note decorated))))
          "the note is one line")
      (is (nil? (:error decorated)))
      (is (nil? (:error-type decorated)))
      (is (= (dissoc outline :forward-refs)
             (dissoc decorated :forward-refs :note))
          "every other outline field survives untouched")))

  (testing "a successful analysis is carried through unchanged"
    (let [outline {:ns 'app.core :form-count 1}
          refs [{:name 'later :used-at 3 :defined-at 9 :gap 6}]]
      (is (= refs (:forward-refs (core/outline-with-forward-refs
                                   outline {:ok true :forward-refs refs}))))
      (is (= [] (:forward-refs (core/outline-with-forward-refs
                                 outline {:ok true :forward-refs nil}))))))

  (testing "the real CLI outlines a file whose analyzer reports findings"
    ;; @spec MCP-OP-LS-001
    ;; @spec MCP-OP-LS-002
    (let [fixture (java.io.File/createTempFile "clj-surgeon-rf2-ls" ".clj")]
      (try
        (spit fixture rf2-lint-warning-fixture)
        (let [{:keys [exit out]} (run-cli ":op" ":ls"
                                          ":file" (.getAbsolutePath fixture))
              result (edn/read-string out)]
          (is (zero? exit)
              "an outline that parses exits zero whatever the analyzer says")
          (is (nil? (:error result))
              (str "the outline must not be replaced by a refusal: " (pr-str result)))
          (is (= 'clj-surgeon-rf2-outline-fixture (:ns result)))
          (is (= 2 (:form-count result)))
          (is (= ["first-form" "second-form"]
                 (mapv #(str (:name %)) (:forms result)))))
        (finally (.delete fixture))))))

;; @spec MCP-OP-LS-001
(deftest forward-reference-analysis-is-not-failed-by-analyzer-findings
  (testing "an analyzer finding-count exit is not an analysis failure"
    (let [fixture (java.io.File/createTempFile "clj-surgeon-rf2-fwd" ".clj")]
      (try
        (spit fixture rf2-lint-warning-fixture)
        (let [analysis (fwd/try-detect-forward-refs
                         (.getAbsolutePath fixture)
                         'clj-surgeon-rf2-outline-fixture)]
          (is (:ok analysis)
              (str "findings are not a failure: " (pr-str analysis)))
          (is (vector? (:forward-refs analysis))))
        (finally (.delete fixture))))))

;; @spec MCP-OP-LS-003
(deftest a-forward-reference-analysis-refusal-carries-a-diagnostic
  (testing "a genuine analyzer failure names why, drawn from its own output"
    (let [analysis (fwd/try-detect-forward-refs
                     "/nonexistent/clj-surgeon-rf2/definitely-absent.clj"
                     'absent.ns)]
      (is (false? (:ok analysis)))
      (is (keyword? (:error-type analysis)))
      (is (not (str/blank? (:note analysis)))
          "an empty diagnostic is a refusal that cannot be recovered from")
      (is (not (str/blank? (str (:diagnostic analysis)))))
      (is (str/includes? (str (:diagnostic analysis)) "file does not exist")
          (str "the diagnostic must quote the analyzer's own output, not a "
               "constant: " (pr-str (:diagnostic analysis)))))))

;; ============================================================
;; rf2-1 (vii) — an unknown CLI argument is a typed refusal, never a
;; success receipt for work that did not happen.
;; Field provenance: cohort rf1, runs rf1-g1-A-1 call 08 and rf1-g2-A-1 call 07
;; both ran
;;   :op :extract! ... :public-forms '[admission-unverified?]'
;; `:public-forms` is not an argument of :extract!. It was accepted, ignored,
;; and reported as `ok` with a complete verified receipt; the form was written
;; `defn-` anyway. One agent discovered it only from a later `git diff`; the
;; other reported it as a tool defect. A success receipt for work that did not
;; happen is worse than a refusal, because it terminates investigation.
;; ============================================================

;; @spec MCP-OP-EXTRACT-010
(deftest an-unknown-cli-argument-is-refused-with-the-accepted-keys
  (testing "the exact rf1 invocation is now refused, not silently ignored"
    (let [refusal (core/unknown-argument-refusal
                    :extract!
                    (get core/ops-registry :extract!)
                    {:op :extract!
                     :file "src/clj_surgeon/mcp_change_buffer.clj"
                     :forms ['expand-command]
                     :to "src/clj_surgeon/mcp_exact_verify.clj"
                     :public-forms ['admission-unverified?]})]
      (is (some? refusal) ":public-forms must not be silently accepted")
      (is (= :unsupported-arguments (:error-type refusal)))
      (is (= [:public-forms] (:unknown refusal)))
      (is (contains? (set (:accepted refusal)) :public)
          "the refusal names the real spelling of what the caller wanted")
      (is (contains? (set (:accepted refusal)) :forms))
      (is (true? (:source-unchanged refusal)))))

  (testing "a complete, correct invocation is not refused"
    (is (nil? (core/unknown-argument-refusal
                :extract!
                (get core/ops-registry :extract!)
                {:op :extract!
                 :file "a.clj" :forms ['x] :to "b.clj"
                 :public ['x] :doc "d" :alias "b" :rewire-callers false
                 :require-policy :minimal :receipt-out "/tmp/r.edn"}))))

  (testing "--help never counts as an unknown argument"
    (is (nil? (core/unknown-argument-refusal
                :ls (get core/ops-registry :ls)
                {:op :ls :file "a.clj" :help true}))))

  (testing "the refusal reaches the real CLI and changes no bytes"
    (let [fixture (java.io.File/createTempFile "clj-surgeon-rf2-unknown" ".clj")
          target (str (.getAbsolutePath fixture) ".target.clj")
          original "(ns rf2.unknown.arg.fixture)\n\n(defn- only-form [] :ok)\n"]
      (try
        (spit fixture original)
        (let [{:keys [exit out]} (run-cli ":op" ":extract!"
                                          ":file" (.getAbsolutePath fixture)
                                          ":forms" "[only-form]"
                                          ":to" target
                                          ":public-forms" "[only-form]")
              result (edn/read-string out)]
          (is (= 1 exit) "an unknown argument exits nonzero")
          (is (= :unsupported-arguments (:error-type result)))
          (is (= [:public-forms] (:unknown result)))
          (is (= original (slurp fixture)) "the source is unchanged")
          (is (not (.exists (io/file target))) "no target was created"))
        (finally
          (.delete fixture)
          (.delete (io/file target)))))))
