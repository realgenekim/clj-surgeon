(ns clj-surgeon.extract-rewire-test
  "Caller-rewiring tests. The real-bytes cases read clj-surgeon's own current
   sources as inputs and compare against expectations transcribed by hand from
   the rf1 reference extraction of mcp-exact-verify out of mcp-change-buffer."
  (:require
   [clj-surgeon.extract-rewire :as rewire]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

;; ============================================================
;; Expectation builders — hand-written edits with asserted counts
;; ============================================================

(defn- replace-exactly
  "Replace `from` with `to` in `src`, asserting `from` occurs exactly `n` times.
   A drifting repo file therefore fails loudly instead of silently weakening
   the expectation."
  [src from to n]
  (let [hits (count (re-seq (re-pattern (java.util.regex.Pattern/quote from)) src))]
    (when-not (= n hits)
      (throw (ex-info (str "expected " n " occurrence(s) of " (pr-str from)
                           " but found " hits)
                      {:from from :expected n :found hits})))
    (str/replace src from to)))

(defn- expect-edits
  "Apply a vector of [from to count] whole-string edits in order."
  [src edits]
  (reduce (fn [acc [from to n]] (replace-exactly acc from to n)) src edits))

(defn- first-difference
  "nil when the two strings are byte-identical; otherwise a compact report so a
   failing 1500-line comparison does not dump the whole file."
  [expected actual]
  (when (not= expected actual)
    (let [e (str/split expected #"\n" -1)
          a (str/split actual #"\n" -1)]
      {:expected-line-count (count e)
       :actual-line-count (count a)
       :first-diff (first (keep-indexed
                            (fn [i l] (when (not= l (nth a i ::missing))
                                        {:line (inc i)
                                         :expected l
                                         :actual (nth a i ::missing)}))
                            e))})))

;; ============================================================
;; qualify-owner-call-sites — literal cases
;; ============================================================

(def ^:private tiny-source
  (str "(ns app.core)\n"
       "\n"
       ";; run-process! in a comment stays put\n"
       "(def marker :expand-command)\n"
       "\n"
       "(defn- run-check!\n"
       "  [root command]\n"
       "  (let [{:keys [exit] :as process}\n"
       "        (run-process! root command)]\n"
       "    (if (admission-unverified? process)\n"
       "      {:ok false :exit exit :tag :run-process! :note \"run-process! text\"}\n"
       "      {:ok true})))\n"
       "\n"
       "(defn other\n"
       "  [x]\n"
       "  (run-process! x))\n"))

(def ^:private tiny-expected
  (str "(ns app.core)\n"
       "\n"
       ";; run-process! in a comment stays put\n"
       "(def marker :expand-command)\n"
       "\n"
       "(defn- run-check!\n"
       "  [root command]\n"
       "  (let [{:keys [exit] :as process}\n"
       "        (ev/run-process! root command)]\n"
       "    (if (ev/admission-unverified? process)\n"
       "      {:ok false :exit exit :tag :run-process! :note \"run-process! text\"}\n"
       "      {:ok true})))\n"
       "\n"
       "(defn other\n"
       "  [x]\n"
       "  (run-process! x))\n"))

;; @spec MCP-OP-EXTRACT-006
(deftest qualify-owner-call-sites-is-scoped-and-byte-preserving
  (testing "only the named owner changes; keywords, strings and comments do not"
    (let [result (rewire/qualify-owner-call-sites
                   tiny-source
                   [{:owner "run-check!"
                     :moved-vars ["run-process!" "admission-unverified?"
                                  "expand-command"]}]
                   "ev")]
      (is (:ok result) (pr-str result))
      (is (nil? (first-difference tiny-expected (:source result))))
      (is (= tiny-expected (:source result)))
      (is (= [{:owner "run-check!" :var "run-process!" :count 1}
              {:owner "run-check!" :var "admission-unverified?" :count 1}
              {:owner "run-check!" :var "expand-command" :count 0}]
             (:rewrites result)))
      (is (= [{:owner "run-check!" :var "expand-command"}]
             (:unmatched result))))))

;; @spec MCP-OP-EXTRACT-006
(deftest qualify-owner-call-sites-refuses-rather-than-guesses
  (testing "a let-bound local shadowing a moved var refuses the whole operation"
    (let [src (str "(defn- run-check!\n"
                   "  [root]\n"
                   "  (let [run-process! (fn [x] x)]\n"
                   "    (run-process! root)))\n")
          result (rewire/qualify-owner-call-sites
                   src [{:owner "run-check!" :moved-vars ["run-process!"]}] "ev")]
      (is (false? (:ok result)))
      (is (= :shadowed-moved-var (:error-type result)))
      (is (= "run-check!" (:owner result)))
      (is (= "run-process!" (:var result)))
      (is (nil? (:source result)))))

  (testing "a parameter shadowing a moved var refuses"
    (let [src "(defn- run-check!\n  [expand-command]\n  (expand-command 1))\n"
          result (rewire/qualify-owner-call-sites
                   src [{:owner "run-check!" :moved-vars ["expand-command"]}] "ev")]
      (is (false? (:ok result)))
      (is (= :shadowed-moved-var (:error-type result)))
      (is (= "expand-command" (:var result)))))

  (testing "an owner the planner named but the source does not define refuses"
    (let [result (rewire/qualify-owner-call-sites
                   tiny-source
                   [{:owner "no-such-owner" :moved-vars ["run-process!"]}]
                   "ev")]
      (is (false? (:ok result)))
      (is (= :unknown-rewire-owner (:error-type result)))
      (is (= "no-such-owner" (:owner result)))))

  (testing "a blank alias refuses"
    (let [result (rewire/qualify-owner-call-sites tiny-source [] "")]
      (is (false? (:ok result)))
      (is (= :invalid-rewire-alias (:error-type result))))))

;; ============================================================
;; qualify-owner-call-sites — real bytes
;; ============================================================

(def ^:private change-buffer-path "src/clj_surgeon/mcp_change_buffer.clj")

(def ^:private remaining-source-callers
  [{:owner "run-check!" :moved-vars ["run-process!" "admission-unverified?"]}
   {:owner "diagnostic-command" :moved-vars ["expand-command"]}
   {:owner "run-diagnostic-check!" :moved-vars ["run-process!" "admission-unverified?"]}
   {:owner "run-verification!" :moved-vars ["expand-command"]}])

;; @spec MCP-OP-EXTRACT-006
(deftest qualify-owner-call-sites-reproduces-the-reference-source-rewiring
  (let [src (slurp change-buffer-path)
        expected (expect-edits
                   src
                   [["\n        (run-process! project-root command)]\n"
                     "\n        (exact-verify/run-process! project-root command)]\n" 1]
                    ["\n    (if (admission-unverified? process)\n"
                     "\n    (if (exact-verify/admission-unverified? process)\n" 2]
                    ["\n  (into (expand-command command files)\n"
                     "\n  (into (exact-verify/expand-command command files)\n" 1]
                    ["\n        (run-process! project-root (diagnostic-command command files))]\n"
                     "\n        (exact-verify/run-process! project-root (diagnostic-command command files))]\n" 1]
                    ["\n                                                 (expand-command command files)))\n"
                     "\n                                                 (exact-verify/expand-command command files)))\n" 1]
                    ["\n                            #(expand-command % files))))]\n"
                     "\n                            #(exact-verify/expand-command % files))))]\n" 1]])
        result (rewire/qualify-owner-call-sites
                 src remaining-source-callers "exact-verify")]
    (is (:ok result) (pr-str (dissoc result :source)))
    (is (nil? (first-difference expected (:source result))))
    (is (= [] (:unmatched result)))
    (is (= 7 (reduce + (map :count (:rewrites result)))))
    (testing "the moved defns themselves and non-owner call sites are untouched"
      (is (str/includes? (:source result) "\n(defn run-process!\n"))
      (is (str/includes? (:source result) "\n       :argv (expand-command command [])})))\n"))
      (is (str/includes? (:source result) "\n    (admission-unverified? process)\n")))))

;; ============================================================
;; requalify-caller — literal cases
;; ============================================================

(def ^:private tiny-caller-add
  (str "(ns app.fmt\n"
       "  \"Doc.\"\n"
       "  (:require\n"
       "   [app.buffer :as buf]\n"
       "   [clojure.string :as str]\n"
       "   [clojure.test :refer [deftest is testing]]))\n"
       "\n"
       "(defn run [x]\n"
       "  (buf/run-process! x)\n"
       "  (buf/still-here x))\n"))

(def ^:private tiny-caller-add-expected
  (str "(ns app.fmt\n"
       "  \"Doc.\"\n"
       "  (:require\n"
       "   [app.buffer :as buf]\n"
       "   [app.verify :as verify]\n"
       "   [clojure.string :as str]\n"
       "   [clojure.test :refer [deftest is testing]]))\n"
       "\n"
       "(defn run [x]\n"
       "  (verify/run-process! x)\n"
       "  (buf/still-here x))\n"))

;; @spec MCP-OP-EXTRACT-007
(deftest requalify-caller-adds-keeps-and-never-touches-refer-entries
  (let [result (rewire/requalify-caller
                 {:source tiny-caller-add
                  :old-alias "buf"
                  :old-ns "app.buffer"
                  :target-ns "app.verify"
                  :alias "verify"
                  :moved-vars ["run-process!" "expand-command"]})]
    (is (:ok result) (pr-str result))
    (is (= :added (:require-action result)))
    (is (= 1 (:rewrites result)))
    (is (nil? (first-difference tiny-caller-add-expected (:source result))))
    (is (= tiny-caller-add-expected (:source result)))
    (testing "the :refer-bearing libspec neither blocks nor changes"
      (is (str/includes? (:source result)
                         "[clojure.test :refer [deftest is testing]]")))))

;; @spec MCP-OP-EXTRACT-007
(deftest requalify-caller-replaces-when-nothing-else-uses-the-old-alias
  (let [src (str "(ns app.fmt\n"
                 "  (:require\n"
                 "   [app.buffer :as buf]\n"
                 "   [clojure.string :as str]))\n"
                 "\n"
                 "(defn run [x]\n"
                 "  (buf/run-process! x))\n")
        expected (str "(ns app.fmt\n"
                      "  (:require\n"
                      "   [app.verify :as verify]\n"
                      "   [clojure.string :as str]))\n"
                      "\n"
                      "(defn run [x]\n"
                      "  (verify/run-process! x))\n")
        result (rewire/requalify-caller
                 {:source src :old-alias "buf" :old-ns "app.buffer"
                  :target-ns "app.verify" :alias "verify"
                  :moved-vars ["run-process!"]})]
    (is (:ok result) (pr-str result))
    (is (= :replaced (:require-action result)))
    (is (= 1 (:rewrites result)))
    (is (= expected (:source result)))))

;; @spec MCP-OP-EXTRACT-007
(deftest requalify-caller-leaves-unmoved-vars-and-reports-unchanged-requires
  (testing "old-alias/<var-not-moved> is not touched"
    (let [src (str "(ns app.fmt\n  (:require\n   [app.buffer :as buf]))\n"
                   "\n(defn run [x] (buf/other x))\n")
          result (rewire/requalify-caller
                   {:source src :old-alias "buf" :old-ns "app.buffer"
                    :target-ns "app.verify" :alias "verify"
                    :moved-vars ["run-process!"]})]
      (is (:ok result) (pr-str result))
      (is (= 0 (:rewrites result)))
      (is (str/includes? (:source result) "(buf/other x)"))))

  (testing "an already-present target require is left alone"
    (let [src (str "(ns app.fmt\n"
                   "  (:require\n"
                   "   [app.buffer :as buf]\n"
                   "   [app.verify :as verify]))\n"
                   "\n"
                   "(defn run [x] (buf/run-process! (buf/other x)))\n")
          expected (str "(ns app.fmt\n"
                        "  (:require\n"
                        "   [app.buffer :as buf]\n"
                        "   [app.verify :as verify]))\n"
                        "\n"
                        "(defn run [x] (verify/run-process! (buf/other x)))\n")
          result (rewire/requalify-caller
                   {:source src :old-alias "buf" :old-ns "app.buffer"
                    :target-ns "app.verify" :alias "verify"
                    :moved-vars ["run-process!"]})]
      (is (:ok result) (pr-str result))
      (is (= :unchanged (:require-action result)))
      (is (= expected (:source result))))))

;; @spec MCP-OP-EXTRACT-007
(deftest requalify-caller-refuses-malformed-caller-files
  (testing "no ns form"
    (let [result (rewire/requalify-caller
                   {:source "(defn run [x] (buf/run-process! x))\n"
                    :old-alias "buf" :old-ns "app.buffer"
                    :target-ns "app.verify" :alias "verify"
                    :moved-vars ["run-process!"]})]
      (is (false? (:ok result)))
      (is (= :no-ns-form (:error-type result)))))

  (testing "two ns forms"
    (let [result (rewire/requalify-caller
                   {:source "(ns a)\n(ns b)\n(defn run [x] (buf/run-process! x))\n"
                    :old-alias "buf" :old-ns "app.buffer"
                    :target-ns "app.verify" :alias "verify"
                    :moved-vars ["run-process!"]})]
      (is (false? (:ok result)))
      (is (= :multiple-ns-forms (:error-type result)))))

  (testing "no :require clause when one is needed"
    (let [result (rewire/requalify-caller
                   {:source "(ns app.fmt)\n(defn run [x] (buf/run-process! x))\n"
                    :old-alias "buf" :old-ns "app.buffer"
                    :target-ns "app.verify" :alias "verify"
                    :moved-vars ["run-process!"]})]
      (is (false? (:ok result)))
      (is (= :no-require-clause (:error-type result)))))

  (testing "the target alias is already bound to a different namespace"
    (let [result (rewire/requalify-caller
                   {:source (str "(ns app.fmt\n  (:require\n"
                                 "   [app.buffer :as buf]\n"
                                 "   [somewhere.else :as verify]))\n"
                                 "(defn run [x] (buf/run-process! (buf/other x)))\n")
                    :old-alias "buf" :old-ns "app.buffer"
                    :target-ns "app.verify" :alias "verify"
                    :moved-vars ["run-process!"]})]
      (is (false? (:ok result)))
      (is (= :alias-collision (:error-type result))))))

;; ============================================================
;; requalify-caller — real bytes
;; ============================================================

(def ^:private public-moved-vars
  ["expand-command" "run-process!" "admission-unverified?"
   "compile-exact-profile" "classify-exact-process-outcome"
   "run-exact-verification!"])

(defn- rewire-real-caller [path]
  (rewire/requalify-caller
    {:source (slurp path)
     :old-alias "change-buffer"
     :old-ns "clj-surgeon.mcp-change-buffer"
     :target-ns "clj-surgeon.mcp-exact-verify"
     :alias "exact-verify"
     :moved-vars public-moved-vars}))

;; @spec MCP-OP-EXTRACT-007
(deftest requalify-caller-reproduces-the-reference-formatter-replace
  (let [path "src/clj_surgeon/mcp_formatter.clj"
        src (slurp path)
        expected (expect-edits
                   src
                   [["   [clj-surgeon.mcp-change-buffer :as change-buffer]\n"
                     "   [clj-surgeon.mcp-exact-verify :as exact-verify]\n" 1]
                    ["change-buffer/run-process!" "exact-verify/run-process!" 1]
                    ["change-buffer/expand-command" "exact-verify/expand-command" 1]])
        result (rewire-real-caller path)]
    (is (:ok result) (pr-str (dissoc result :source)))
    (is (= :replaced (:require-action result)))
    (is (= 2 (:rewrites result)))
    (is (nil? (first-difference expected (:source result))))
    (is (not (str/includes? (:source result) "change-buffer")))))

;; @spec MCP-OP-EXTRACT-007
(deftest requalify-caller-reproduces-the-reference-tool-add
  (let [path "src/clj_surgeon/mcp_tool.clj"
        src (slurp path)
        expected (expect-edits
                   src
                   [["   [clj-surgeon.mcp-contract :as contract]\n"
                     (str "   [clj-surgeon.mcp-contract :as contract]\n"
                          "   [clj-surgeon.mcp-exact-verify :as exact-verify]\n") 1]
                    ["change-buffer/compile-exact-profile"
                     "exact-verify/compile-exact-profile" 2]
                    ["change-buffer/run-exact-verification!"
                     "exact-verify/run-exact-verification!" 2]])
        result (rewire-real-caller path)]
    (is (:ok result) (pr-str (dissoc result :source)))
    (is (= :added (:require-action result)))
    (is (= 4 (:rewrites result)))
    (is (nil? (first-difference expected (:source result))))
    (testing "the still-referenced change-buffer require is kept"
      (is (str/includes? (:source result)
                         "[clj-surgeon.mcp-change-buffer :as change-buffer]")))))

;; @spec MCP-OP-EXTRACT-007
(deftest requalify-caller-reproduces-the-reference-test-namespace-add
  (let [path "test/clj_surgeon/mcp_change_buffer_test.clj"
        src (slurp path)
        expected (expect-edits
                   src
                   [["   [clj-surgeon.mcp-change-buffer :as change-buffer]\n"
                     (str "   [clj-surgeon.mcp-change-buffer :as change-buffer]\n"
                          "   [clj-surgeon.mcp-exact-verify :as exact-verify]\n") 1]
                    ["change-buffer/compile-exact-profile"
                     "exact-verify/compile-exact-profile" 3]
                    ["change-buffer/classify-exact-process-outcome"
                     "exact-verify/classify-exact-process-outcome" 2]
                    ["change-buffer/run-exact-verification!"
                     "exact-verify/run-exact-verification!" 5]
                    ;; origin/main's host-independent clj-kondo assertion added
                    ;; one `expand-command` call to this reference file; the
                    ;; var is in `public-moved-vars`, so requalifying it is the
                    ;; correct behaviour and only this count was stale
                    ["change-buffer/expand-command"
                     "exact-verify/expand-command" 1]])
        result (rewire-real-caller path)]
    (is (:ok result) (pr-str (dissoc result :source)))
    (is (= :added (:require-action result)))
    (is (= 11 (:rewrites result)))
    (is (nil? (first-difference expected (:source result))))
    (testing "the :refer-bearing clojure.test entry neither refuses nor changes"
      (is (str/includes? (:source result)
                         "[clojure.test :refer [deftest is testing]]")))))

;; @spec MCP-OP-EXTRACT-031
(deftest an-alias-that-is-not-a-symbol-refuses
  (let [source (str "(ns app.only-moved\n"
                    "  (:require\n"
                    "   [app.core :as core]))\n\n"
                    "(defn go [x] (core/moved x))\n")]
    (testing "qualify-owner-call-sites refuses a non-symbol alias"
      (doseq [bad ["a b" "a/b" "" "  " "[x]" "a\nb" "1st" "a;b" "a(b"]]
        (let [result (rewire/qualify-owner-call-sites
                       "(defn owner [] (moved 1))"
                       [{:owner "owner" :moved-vars ["moved"]}]
                       bad)]
          (is (false? (:ok result)) (str "admitted " (pr-str bad)))
          (is (= :invalid-rewire-alias (:error-type result))
              (str "and refused it typed: " (pr-str bad)))
          (is (nil? (:source result)) "no refusal carries a source"))))

    (testing "requalify-caller refuses one too"
      (let [result (rewire/requalify-caller
                     {:source source
                      :old-alias "core"
                      :old-ns "app.core"
                      :target-ns "app.moved"
                      :alias "a b"
                      :moved-vars ["moved"]})]
        (is (false? (:ok result)))
        (is (= :invalid-rewire-alias (:error-type result)))
        (is (nil? (:source result)))))

    (testing "ordinary aliases still pass"
      (doseq [good ["moved" "app.moved" "str" "a->b" "s'" "*x*" "core-2"]]
        (let [result (rewire/requalify-caller
                       {:source source
                        :old-alias "core"
                        :old-ns "app.core"
                        :target-ns "app.moved"
                        :alias good
                        :moved-vars ["moved"]})]
          (is (true? (:ok result)) (str "refused a valid alias: " (pr-str good))))))))
