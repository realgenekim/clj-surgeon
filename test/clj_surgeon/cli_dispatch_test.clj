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
  (:require [clojure.test :refer [deftest is testing]]
            [clj-surgeon.core :as core]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [babashka.process :as proc]))

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
    (testing "help text yields a sane number of op names"
      (is (>= (count ops) (count core/ops-registry))
          (str "extracted only " (count ops) " names — extraction regex drifted?")))
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
  (let [out (run->output {:op :bogus})]
    (let [parsed (edn/read-string out)]
      (is (map? parsed))
      (is (str/includes? (:error parsed) "Unknown op")))))

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
      (is (str/includes? out ":extract")))))

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
      (is (zero? exit))
      (is (str/includes? out "Unknown op"))
      (is (not (str/includes? (str out err) "ClassCastException"))
          "ClassCastException leaked to the user again"))))

(deftest cli-bare-string-op-help-resolves
  (let [{:keys [exit out]} (run-cli ":op" "tree" "--help")]
    (testing "--help with a bare-string alias shows the canonical op's help"
      (is (zero? exit))
      (is (str/includes? out "Map namespaces across a directory tree")))))
