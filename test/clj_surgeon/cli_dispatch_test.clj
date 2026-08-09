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

(deftest cli-bare-string-op-dispatches
  (let [{:keys [exit out err]} (run-cli ":op" "ls" ":file" "src/clj_surgeon/forms.clj")]
    (testing "documented bare-string usage works end to end"
      (is (zero? exit) (str "stderr: " err))
      (is (str/includes? out "clj-surgeon.forms")))))

(deftest cli-bare-string-alias-dispatches
  (let [{:keys [exit out]} (run-cli ":op" "outline" ":file" "src/clj_surgeon/forms.clj")]
    (testing "bare-string ALIAS dispatches too"
      (is (zero? exit))
      (is (str/includes? out "clj-surgeon.forms")))))

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
