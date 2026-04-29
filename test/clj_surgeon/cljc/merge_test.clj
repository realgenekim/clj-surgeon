(ns clj-surgeon.cljc.merge-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [clojure.tools.reader :as r]
            [clojure.tools.reader.reader-types :as rt]
            [clj-surgeon.cljc.merge :as m]))

(defn- parse-forms
  "Parse a CLJC source string into a vector of forms with reader conditionals
   preserved as ReaderConditional records. Two sources that differ only in
   whitespace/formatting produce equal vectors."
  [src]
  (let [rdr (rt/string-push-back-reader src)]
    (->> (repeatedly #(r/read {:eof ::end :read-cond :preserve} rdr))
         (take-while #(not= ::end %))
         vec)))

(defn- read-fixture [path]
  (slurp (str "test-fixtures/cljc/merge/" path)))

(defn- check-merge [name]
  (let [clj-src       (read-fixture (str name "/in.clj"))
        cljs-src      (read-fixture (str name "/in.cljs"))
        expected-cljc (read-fixture (str name "/expected.cljc"))
        got-cljc      (m/merge-files clj-src cljs-src)]
    {:expected (parse-forms expected-cljc)
     :got      (parse-forms got-cljc)
     :got-src  got-cljc}))

;; ============================================================
;; Behavioural fixtures
;; ============================================================

(deftest dom-divergent-merge
  (testing "Same alias to two different namespaces (dom/dom-server) collapses
            into a single #?@(:clj ... :cljs ...) splice; shared requires and
            shared body forms are emitted plain."
    (let [{:keys [expected got]} (check-merge "dom-divergent")]
      (is (= expected got)))))

(deftest identical-requires-merge
  (testing "When both files are identical, the CLJC has no reader conditionals."
    (let [{:keys [expected got]} (check-merge "identical-requires")]
      (is (= expected got)))))

(deftest one-sided-cljs-require
  (testing "A require present only in CLJS is wrapped in #?@(:cljs [...])
            with no :clj branch emitted."
    (let [{:keys [expected got]} (check-merge "one-sided-cljs")]
      (is (= expected got)))))

(deftest one-sided-clj-require
  (testing "A require present only in CLJ is wrapped in #?@(:clj [...])
            with no :cljs branch emitted."
    (let [{:keys [expected got]} (check-merge "one-sided-clj")]
      (is (= expected got)))))

(deftest npm-asymmetric-merge
  (testing "An npm string require on the CLJS side stays in the :cljs branch
            of the splice; CLJ-only requires go in the :clj branch; the
            divergent dom alias is grouped in the same splice."
    (let [{:keys [expected got]} (check-merge "npm-asymmetric")]
      (is (= expected got)))))

(deftest body-collision-default
  (testing "When both sides define the same top-level symbol with different
            bodies and no collision rule applies, the default is to wrap each
            definition in its own reader conditional branch."
    (let [{:keys [expected got]} (check-merge "collision-default")]
      (is (= expected got)))))

(deftest refer-asymmetric-merge
  (testing "Same ns alias on both sides but different :refer lists must NOT be
            silently dropped — both versions must end up in the platform splice."
    (let [{:keys [expected got got-src]} (check-merge "refer-asymmetric")]
      (is (= expected got))
      ;; Belt-and-suspenders: also check that 'split' didn't get lost.
      (is (str/includes? got-src "split")
          "the :cljs-only refer 'split' must appear in the merged source"))))

(deftest no-requires-merge
  (testing "Both inputs lack a :require form — output ns has no :require either."
    (let [{:keys [expected got]} (check-merge "no-requires")]
      (is (= expected got)))))

;; ============================================================
;; Error paths
;; ============================================================

(defn- thrown-msg [f]
  (try (f) nil
       (catch Exception e (.getMessage e))))

(deftest missing-ns-throws
  (let [msg (thrown-msg #(m/merge-files "(def x 1)" "(def x 1)"))]
    (is (some? msg))
    (is (str/includes? msg "ns form"))))

(deftest ns-name-mismatch-throws
  (let [msg (thrown-msg #(m/merge-files "(ns foo.a)" "(ns foo.b)"))]
    (is (some? msg))
    (is (str/includes? msg "ns names differ"))))

(deftest unsupported-ns-subform-throws
  (let [msg (thrown-msg #(m/merge-files "(ns foo (:require [a]) (:import java.util.UUID))"
                                        "(ns foo (:require [a]))"))]
    (is (some? msg))
    (is (str/includes? msg "import"))))

(deftest ns-docstring-throws
  (let [msg (thrown-msg #(m/merge-files "(ns foo \"docstring\" (:require [a]))"
                                        "(ns foo \"docstring\" (:require [a]))"))]
    (is (some? msg))
    (is (str/includes? msg "docstring"))))

(deftest unmatched-body-counts-emit-strict-split
  (testing "When CLJ and CLJS have different numbers of body forms, the merge
            falls back to a strict reader-conditional split: each side's full
            body wrapped in its own #?@(:clj [...]) / #?@(:cljs [...]) branch.
            The output is mechanically correct; the LLM can refactor it later."
    (let [{:keys [expected got]} (check-merge "unmatched-counts")]
      (is (= expected got)))))

(deftest only-clj-has-body-emits-clj-only-splice
  (testing "If CLJS has no body forms at all, output uses just #?@(:clj [...])."
    (let [src (m/merge-files "(ns foo) (def a 1)" "(ns foo)")]
      (is (str/includes? src "#?@(:clj"))
      (is (not (str/includes? src ":cljs"))))))
