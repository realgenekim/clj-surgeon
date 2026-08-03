(ns clj-surgeon.help-test
  (:require
   [babashka.process :as proc]
   [clj-surgeon.core :as core]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

;; ============================================================
;; resolve-op
;; ============================================================

(deftest resolve-op-canonical-ops
  (testing "canonical ops resolve to themselves"
    (is (= :ls (core/resolve-op :ls)))
    (is (= :ls-tree (core/resolve-op :ls-tree)))
    (is (= :extract (core/resolve-op :extract)))
    (is (= :cljc-merge (core/resolve-op :cljc-merge)))))

(deftest resolve-op-aliases
  (testing "aliases resolve to canonical"
    (is (= :ls (core/resolve-op :outline)))
    (is (= :ls-tree (core/resolve-op :tree)))
    (is (= :ls-tree (core/resolve-op :map)))
    (is (= :ls-tree (core/resolve-op :outline-tree)))
    (is (= :show-form (core/resolve-op :cat)))
    (is (= :show-form (core/resolve-op "cat")))
    (is (= :find-subform (core/resolve-op :grep-form)))
    (is (= :find-subform (core/resolve-op "grep-form")))
    (is (= :mv (core/resolve-op :mv-with-deps)))
    (is (= :mv (core/resolve-op "mv-with-deps")))))

(deftest resolve-op-unknown
  (testing "unknown ops return nil"
    (is (nil? (core/resolve-op :bogus)))
    (is (nil? (core/resolve-op :nope)))))

(deftest resolve-op-string-ops
  (testing "bare string ops (CLI value without leading colon) resolve like keywords"
    ;; Regression: `clj-surgeon :op ls-tree` parses the value as the string
    ;; "ls-tree"; contains? on the sorted-map registry then threw
    ;; ClassCastException (Keyword vs String) instead of dispatching.
    (is (= :ls-tree (core/resolve-op "ls-tree")))
    (is (= :ls (core/resolve-op "ls")))
    (is (= :ls-tree (core/resolve-op "tree")))
    (is (= :ls (core/resolve-op "outline")))))

(deftest resolve-op-non-keyword-junk-is-nil-not-crash
  (testing "nil and unknown strings return nil instead of throwing"
    (is (nil? (core/resolve-op nil)))
    (is (nil? (core/resolve-op "bogus")))
    (is (nil? (core/resolve-op 42)))))

(deftest parse-args-then-resolve-op-documented-usage
  (testing "the documented CLI invocation `:op ls-tree` resolves end to end"
    (is (= :ls-tree (core/resolve-op (:op (core/parse-args [":op" "ls-tree"])))))
    (is (= :ls-tree (core/resolve-op (:op (core/parse-args [":op" ":ls-tree"])))))))

(deftest dispatch-string-op-ls-tree-documented-usage
  (testing "parse-args -> resolve-op -> run accepts the documented bare string op"
    (let [opts (core/parse-args [":op" "ls-tree" ":dir" "src"])]
      (is (= "ls-tree" (:op opts)))
      (is (= :ls-tree (core/resolve-op (:op opts))))
      (let [out (with-out-str (core/run opts))]
        (is (str/includes? out "clj_surgeon/core.clj"))
        (is (str/includes? out "ns: clj-surgeon.core"))))))

(deftest dispatch-keyword-op-ls-tree
  (testing "parse-args -> resolve-op -> run still accepts keyword ops"
    (let [opts (core/parse-args [":op" ":ls-tree" ":dir" "src"])]
      (is (= :ls-tree (:op opts)))
      (is (= :ls-tree (core/resolve-op (:op opts))))
      (let [out (with-out-str (core/run opts))]
        (is (str/includes? out "clj_surgeon/core.clj"))
        (is (str/includes? out "ns: clj-surgeon.core"))))))

(deftest dispatch-unknown-ops-return-error-maps-not-exceptions
  (testing "unknown bare string op returns a clean EDN error"
    (let [out (with-out-str (core/run (core/parse-args [":op" "bogus"])))]
      (is (= {:error (str "Unknown op: bogus. Valid ops: "
                          (str/join ", " (sort (keys core/ops-registry))))
              :error-type :unknown-operation}
             (edn/read-string out)))))
  (testing "unknown keyword op returns a clean EDN error"
    (let [out (with-out-str (core/run (core/parse-args [":op" ":bogus"])))]
      (is (= {:error (str "Unknown op: :bogus. Valid ops: "
                          (str/join ", " (sort (keys core/ops-registry))))
              :error-type :unknown-operation}
             (edn/read-string out))))))

;; ============================================================
;; ops-registry completeness
;; ============================================================

(deftest registry-has-all-ops
  (testing "registry contains every canonical op"
    (let [expected #{:ls :ls-tree :show-form :mv :declares :deps :topo
                     :ls-extract :ls-deps
                     :rename-ns :rename-ns!
                     :fix-declares :fix-declares!
                     :extract :extract!
                     :find-subform :replace-subform :replace-subform!
                     :cljc-merge :cljc-split :cljc-add-require :cljc-analyze}]
      (is (= expected (set (keys core/ops-registry)))))))

(deftest registry-entries-have-required-keys
  (testing "every entry has :handler, :desc, :args, :examples, :category"
    (doseq [[op-key op-def] core/ops-registry]
      (is (fn? (:handler op-def)) (str op-key " missing :handler"))
      (is (string? (:desc op-def)) (str op-key " missing :desc"))
      (is (map? (:args op-def)) (str op-key " missing :args"))
      (is (vector? (:examples op-def)) (str op-key " missing :examples"))
      (is (#{:read :write :cljc} (:category op-def)) (str op-key " invalid :category")))))

(deftest registry-pairs-are-symmetric
  (testing "plan/execute pairs point at each other"
    (doseq [[op-key {:keys [pair]}] core/ops-registry
            :when pair]
      (is (contains? core/ops-registry pair)
          (str op-key " :pair " pair " not in registry"))
      (is (= op-key (:pair (get core/ops-registry pair)))
          (str op-key " and " pair " are not symmetric")))))

;; ============================================================
;; format-global-help
;; ============================================================

(deftest global-help-contains-all-canonical-ops
  (let [help (core/format-global-help core/ops-registry)]
    (testing "contains every canonical op name"
      (doseq [op-key (keys core/ops-registry)]
        (is (str/includes? help (name op-key))
            (str "missing " op-key " in global help"))))
    (testing "contains category headers"
      (is (str/includes? help "Read-only"))
      (is (str/includes? help "Write"))
      (is (str/includes? help "CLJC")))
    (testing "contains usage line"
      (is (str/includes? help "Usage:")))
    (testing "contains quick start examples"
      (is (str/includes? help "Quick start:")))
    (testing "does not make the false only-bang-ops-write claim"
      (is (not (str/includes? help "only ! variants write files")))
      (is (str/includes? help ":mv writes unless :dry-run true")))))

(deftest global-help-excludes-aliases
  (let [help (core/format-global-help core/ops-registry)]
    (testing "aliases appear in parens, not as separate lines"
      ;; "outline" should appear as "(aliases: outline)" not as a left-column entry
      ;; Count how many times "outline" appears as a command name at start of line
      (let [lines (str/split-lines help)
            cmd-lines (filter #(re-find #"^\s{4}\S" %) lines)]
        (is (not (some #(re-find #"^\s{4}outline\s" %) cmd-lines))
            "alias 'outline' should not appear as its own command line")))))

(deftest global-help-op-names-resolve
  (let [help (core/format-global-help core/ops-registry)
        help-op-names (->> (str/split-lines help)
                           (keep #(second (re-matches #"\s{4}([a-z][a-z0-9!-]*)\s{2,}.+" %)))
                           (remove #{"clj-surgeon"})
                           set)]
    (testing "every op name printed in global help resolves from the parser's string form"
      (is (= (set (map name (keys core/ops-registry))) help-op-names))
      (doseq [op-name help-op-names]
        (is (some? (core/resolve-op op-name))
            (str "global help prints unresolved op " op-name))))))

;; ============================================================
;; format-op-help
;; ============================================================

(deftest op-help-shows-required-args
  (let [help (core/format-op-help :extract (get core/ops-registry :extract))]
    (testing "shows (required) marker"
      (is (str/includes? help "(required)"))
      (is (str/includes? help ":file"))
      (is (str/includes? help ":forms"))
      (is (str/includes? help ":to")))
    (testing "shows see-also pair"
      (is (str/includes? help "See also: :op extract!")))
    (testing "shows examples"
      (is (str/includes? help "Examples:")))))

(deftest op-help-shows-optional-args
  (let [help (core/format-op-help :deps (get core/ops-registry :deps))]
    (testing ":form arg is optional (no required marker)"
      (is (str/includes? help ":form"))
      ;; :file is required, :form is not — check both appear
      (is (str/includes? help "(required) Clojure source file"))
      ;; :form line should NOT have (required)
      (let [form-line (->> (str/split-lines help)
                           (filter #(str/includes? % ":form"))
                           first)]
        (is (not (str/includes? form-line "(required)"))
            ":form should be optional")))))

(deftest op-help-shows-aliases
  (let [help (core/format-op-help :ls (get core/ops-registry :ls))]
    (testing "shows aliases"
      (is (str/includes? help "Aliases: outline")))))

(deftest show-form-help-documents-the-one-shot-read-contract
  (let [help (core/format-op-help :show-form
                                  (get core/ops-registry :show-form))]
    (testing "both selectors and CLJC disambiguation are discoverable"
      (is (str/includes? help ":form"))
      (is (str/includes? help ":line"))
      (is (str/includes? help ":platform"))
      (is (str/includes? help "exactly one selector"))
      (is (str/includes? help "instead of reconstructing a sed range"))
      (is (str/includes? help "do not run :ls solely as a preflight"))
      (is (str/includes? help "distinctive text"))
      (is (str/includes? help "rg -n")))
    (testing "the exact documented invocations are printed"
      (is (str/includes? help ":op :show-form :file src/my/ns.clj :form transition!"))
      (is (str/includes? help ":op :show-form :file src/my/ns.clj :line 1134")))
    (testing "the structural-shell alias is discoverable"
      (is (str/includes? help "Aliases: cat")))
    (testing "ambiguity fails closed"
      (is (str/includes? help "never chooses the first match")))))

(deftest replace-subform-apply-help-forbids-plan-editing
  (let [help (core/format-op-help :replace-subform!
                                  (get core/ops-registry :replace-subform!))]
    (is (str/includes? help "Do not edit the plan"))
    (is (str/includes? help "never chain it with application"))
    (is (str/includes? help "generate a new plan"))
    (is (str/includes? help ":replace-subform!"))))

(deftest find-subform-help-teaches-one-shot-file-wide-structural-grep
  (let [help (core/format-op-help :find-subform
                                  (get core/ops-registry :find-subform))]
    (is (str/includes? help "Aliases: grep-form"))
    (is (str/includes? help "Omit :inside for file-wide structural search"))
    (is (str/includes? help ":op :grep-form"))
    (is (str/includes? help "not regular expressions"))))

;; ============================================================
;; parse-val — string to value (pure)
;; ============================================================

(deftest parse-val-keywords
  (is (= :ls (core/parse-val ":ls")))
  (is (= :edn (core/parse-val ":edn")))
  (is (= :cljs (core/parse-val ":cljs"))))

(deftest parse-val-booleans
  (is (= true (core/parse-val "true")))
  (is (= false (core/parse-val "false"))))

(deftest parse-val-preserves-numeric-strings-for-operation-specific-meaning
  (testing "the generic parser does not corrupt numeric grep terms or identifiers"
    (is (= "1134" (core/parse-val "1134")))
    (is (= "-7" (core/parse-val "-7")))
    (is (= "12.5" (core/parse-val "12.5")))
    (is (= "12a" (core/parse-val "12a")))))

(deftest parse-val-edn-vectors
  (is (= '[foo bar] (core/parse-val "[foo bar]")))
  (is (= '[a] (core/parse-val "[a]"))))

(deftest parse-val-edn-maps
  (is (= {:a 1} (core/parse-val "{:a 1}"))))

(deftest parse-val-strings-pass-through
  (is (= "src/my/ns.clj" (core/parse-val "src/my/ns.clj")))
  (is (= "postgres|jdbc" (core/parse-val "postgres|jdbc"))))

;; ============================================================
;; parse-args — arg list to opts map (pure)
;; ============================================================

(deftest parse-args-basic-key-value-pairs
  (is (= {:op :ls :file "src/my/ns.clj"}
         (core/parse-args [":op" ":ls" ":file" "src/my/ns.clj"]))))

(deftest parse-args-multiple-args
  (is (= {:op :mv :file "state.clj" :form "foo" :before "bar" :dry-run true}
         (core/parse-args [":op" ":mv" ":file" "state.clj"
                           ":form" "foo" ":before" "bar" ":dry-run" "true"]))))

(deftest parse-args-with-deps-boolean
  (is (= {:op :mv :file "state.clj" :form "foo" :before "bar"
          :with-deps true}
         (core/parse-args [":op" ":mv" ":file" "state.clj"
                           ":form" "foo" ":before" "bar"
                           ":with-deps" "true"]))))

(deftest mv-help-documents-guard-and-dependency-alias
  (let [help (core/format-op-help :mv (get core/ops-registry :mv))]
    (is (str/includes? help "with-deps"))
    (is (str/includes? help "mv-with-deps"))
    (is (str/includes? help "presets :with-deps true"))
    (is (str/includes? help "dependency guards"))
    (is (str/includes? help "Safe workflow:"))
    (is (str/includes? help "Always preview plain :mv"))
    (is (str/includes? help ":would-strand-dependencies"))
    (is (str/includes? help ":would-strand-users"))
    (is (str/includes? help ":recommended-command"))
    (is (str/includes? help ":apply-command"))
    (is (str/includes? help ":added-forms"))
    (is (str/includes? help ":move-order"))
    (is (str/includes? help "not a saved, hash-bound plan"))
    (is (str/includes? help
                       ":mv :file src/my/ns.clj :form foo :before bar :dry-run true"))))

(deftest parse-args-edn-vector-arg
  (is (= {:op :extract :file "src/s.clj" :forms '[distill refine] :to "src/d.clj"}
         (core/parse-args [":op" ":extract" ":file" "src/s.clj"
                           ":forms" "[distill refine]" ":to" "src/d.clj"]))))

(deftest parse-args-preserves-structural-forms-for-the-lens-parser
  (is (= "[:button {:onclick \"\\x27\"}]"
         (:with (core/parse-args [":op" ":replace-subform"
                                  ":with" "[:button {:onclick \"\\x27\"}]"]))))
  (is (= "(inc 1) (inc 2)"
         (:match (core/parse-args [":op" ":find-subform"
                                   ":match" "(inc 1) (inc 2)"])))))

(deftest parse-args-help-flag-standalone
  (is (= {:help true}
         (core/parse-args ["--help"]))))

(deftest parse-args-dash-h-standalone
  (is (= {:help true}
         (core/parse-args ["-h"]))))

(deftest parse-args-help-with-op
  (is (= {:op :ls :help true}
         (core/parse-args [":op" ":ls" "--help"]))))

(deftest parse-args-help-before-op
  (is (= {:op :ls :help true}
         (core/parse-args ["--help" ":op" ":ls"]))))

(deftest parse-args-empty
  (is (= {} (core/parse-args []))))

(deftest parse-args-help-stripped-from-pairs
  (testing "--help doesn't interfere with key-value pairing"
    (is (= {:op :ls :file "foo.clj" :help true}
           (core/parse-args [":op" ":ls" "--help" ":file" "foo.clj"])))))

;; ============================================================
;; Subprocess CLI tests — actual command-line parsing end-to-end
;;
;; Shells out to `bb -m clj-surgeon.core` as a separate process,
;; testing exactly what a user or agent would see.
;; ============================================================

(def ^:private project-src
  "Absolute path to this project's src/ dir, for subprocess classpath."
  (let [this-file (io/file "test/clj_surgeon/help_test.clj")]
    (str (.getAbsolutePath
           (io/file (.getParentFile (.getParentFile (.getParentFile this-file))) "src")))))

(defn- run-cli
  "Run clj-surgeon CLI as subprocess, return {:exit :out :err}."
  [& args]
  (apply proc/shell {:out :string :err :string :continue true}
         "bb" "-cp" project-src "-m" "clj-surgeon.core" args))

;; --- No args → global help ---

(deftest cli-no-args-shows-help
  (let [{:keys [exit out]} (run-cli)]
    (testing "exits successfully"
      (is (zero? exit)))
    (testing "shows global help with usage line"
      (is (str/includes? out "clj-surgeon"))
      (is (str/includes? out "Usage:")))
    (testing "shows all categories"
      (is (str/includes? out "Read-only"))
      (is (str/includes? out "Write"))
      (is (str/includes? out "CLJC")))
    (testing "shows quick start examples"
      (is (str/includes? out "Quick start:")))))

;; --- --help flag → global help ---

(deftest cli-help-flag-shows-help
  (let [{:keys [exit out]} (run-cli "--help")]
    (testing "exits successfully"
      (is (zero? exit)))
    (testing "shows same global help as no-args"
      (is (str/includes? out "Usage:"))
      (is (str/includes? out "Read-only")))))

(deftest cli-dash-h-flag-shows-help
  (let [{:keys [exit out]} (run-cli "-h")]
    (testing "-h works like --help"
      (is (zero? exit))
      (is (str/includes? out "Usage:")))))

;; --- Per-op --help ---

(deftest cli-op-help-shows-details
  (let [{:keys [exit out]} (run-cli ":op" ":ls" "--help")]
    (testing "exits successfully"
      (is (zero? exit)))
    (testing "shows op name"
      (is (str/includes? out "clj-surgeon :op ls")))
    (testing "shows description"
      (is (str/includes? out "List forms")))
    (testing "shows required args"
      (is (str/includes? out ":file"))
      (is (str/includes? out "(required)")))
    (testing "shows examples"
      (is (str/includes? out "Examples:")))))

(deftest cli-op-help-with-pair
  (let [{:keys [exit out]} (run-cli ":op" ":extract" "--help")]
    (testing "shows see-also for plan/execute pair"
      (is (zero? exit))
      (is (str/includes? out "See also: :op extract!")))))

(deftest cli-op-help-for-alias
  (let [{:keys [exit out]} (run-cli ":op" ":tree" "--help")]
    (testing "alias resolves and shows help for canonical op"
      (is (zero? exit))
      (is (str/includes? out "Map namespaces across a directory tree")))))

;; --- Unknown op ---

(deftest cli-unknown-op-shows-error
  (let [{:keys [exit out]} (run-cli ":op" ":bogus")]
    (testing "returns a failing exit status and prints an EDN error"
      (is (pos? exit))
      (is (= :unknown-operation (:error-type (edn/read-string out)))))
    (testing "error message mentions the bad op"
      (is (str/includes? out "Unknown op: :bogus")))
    (testing "error lists valid ops from registry"
      (is (str/includes? out ":ls"))
      (is (str/includes? out ":extract"))
      (is (str/includes? out ":cljc-merge")))))

;; --- --help with unknown op ---

(deftest cli-help-unknown-op-is-an-error
  (let [{:keys [exit out]} (run-cli ":op" ":bogus" "--help")]
    (testing "does not disguise an invalid command as successful help"
      (is (pos? exit))
      (is (= :unknown-operation (:error-type (edn/read-string out)))))))

;; --- Normal dispatch still works ---

(deftest cli-normal-dispatch-works
  (let [{:keys [exit out]} (run-cli ":op" ":ls" ":file" "src/clj_surgeon/core.clj")]
    (testing "normal :ls dispatch returns outline"
      (is (zero? exit))
      (is (str/includes? out "clj-surgeon.core"))
      (is (str/includes? out ":forms")))))

;; --- Alias dispatch still works ---

(deftest cli-alias-dispatch-works
  (let [{:keys [exit out]} (run-cli ":op" ":outline" ":file" "src/clj_surgeon/forms.clj")]
    (testing ":outline alias dispatches same as :ls"
      (is (zero? exit))
      (is (str/includes? out "clj-surgeon.forms")))))

(deftest cli-documented-string-op-dispatch-works
  (let [{:keys [exit out]} (run-cli ":op" "ls-tree" ":dir" "src")]
    (testing "-main accepts documented bare op values"
      (is (zero? exit))
      (is (str/includes? out "clj_surgeon/core.clj"))
      (is (str/includes? out "ns: clj-surgeon.core")))))
