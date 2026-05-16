(ns clj-surgeon.forms-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-surgeon.forms :as forms]))

;; ============================================================
;; classify — the heart of form classification
;; ============================================================

(deftest test-classify-core-forms
  (testing "every core Clojure defining form classifies to its canonical kind"
    (is (= :def         (forms/classify "def")))
    (is (= :defn        (forms/classify "defn")))
    (is (= :defn-       (forms/classify "defn-")))
    (is (= :defonce     (forms/classify "defonce")))
    (is (= :defmacro    (forms/classify "defmacro")))
    (is (= :defmethod   (forms/classify "defmethod")))
    (is (= :defmulti    (forms/classify "defmulti")))
    (is (= :defprotocol (forms/classify "defprotocol")))
    (is (= :defrecord   (forms/classify "defrecord")))
    (is (= :deftype     (forms/classify "deftype")))
    (is (= :declare     (forms/classify "declare")))))

(deftest test-classify-explicit-aliases
  (testing "Guardrails >defn and >defn- are recognized"
    (is (= :defn  (forms/classify ">defn")))
    (is (= :defn- (forms/classify ">defn-")))))

(deftest test-classify-namespace-qualified-auto-detection
  (testing "alias/defn auto-resolves regardless of what alias is used"
    (is (= :defn  (forms/classify "mu/defn")))
    (is (= :defn  (forms/classify "m/defn")))
    (is (= :defn  (forms/classify "malli/defn")))
    (is (= :defn  (forms/classify "s/defn")))
    (is (= :defn  (forms/classify "schema.core/defn"))))
  (testing "alias/defn- auto-resolves as private"
    (is (= :defn- (forms/classify "mu/defn-")))
    (is (= :defn- (forms/classify "m/defn-"))))
  (testing "other core forms work through / too"
    (is (= :def      (forms/classify "my.ns/def")))
    (is (= :defmacro (forms/classify "util/defmacro")))
    (is (= :defonce  (forms/classify "state/defonce")))))

(deftest test-classify-non-defining-forms
  (testing "non-defining forms return nil"
    (is (nil? (forms/classify "ns")))
    (is (nil? (forms/classify "let")))
    (is (nil? (forms/classify "if")))
    (is (nil? (forms/classify "str/join")))
    (is (nil? (forms/classify nil)))))

(deftest test-classify-no-false-positives-on-slash
  (testing "qualified symbols that aren't defining forms return nil"
    (is (nil? (forms/classify "str/join")))
    (is (nil? (forms/classify "db/query")))
    (is (nil? (forms/classify "my.ns/default")))
    (is (nil? (forms/classify "my.ns/defn-routes")))))

;; ============================================================
;; defining-form? — boolean predicate
;; ============================================================

(deftest test-defining-form?
  (testing "true for all recognized forms"
    (is (forms/defining-form? "defn"))
    (is (forms/defining-form? ">defn"))
    (is (forms/defining-form? "mu/defn"))
    (is (forms/defining-form? "def"))
    (is (forms/defining-form? "declare")))
  (testing "false for non-defining forms"
    (is (not (forms/defining-form? "ns")))
    (is (not (forms/defining-form? "let")))
    (is (not (forms/defining-form? nil)))))

;; ============================================================
;; private-form? — the bug that was: >defn- must be private
;; ============================================================

(deftest test-private-form?
  (testing "defn- variants are all private"
    (is (forms/private-form? "defn-"))
    (is (forms/private-form? ">defn-"))
    (is (forms/private-form? "mu/defn-"))
    (is (forms/private-form? "m/defn-")))
  (testing "public forms are not private"
    (is (not (forms/private-form? "defn")))
    (is (not (forms/private-form? ">defn")))
    (is (not (forms/private-form? "mu/defn")))
    (is (not (forms/private-form? "def")))
    (is (not (forms/private-form? nil)))))

;; ============================================================
;; has-arglists? — defn-like forms have arg vectors
;; ============================================================

(deftest test-has-arglists?
  (testing "defn and defn- variants have arglists"
    (is (forms/has-arglists? "defn"))
    (is (forms/has-arglists? "defn-"))
    (is (forms/has-arglists? ">defn"))
    (is (forms/has-arglists? ">defn-"))
    (is (forms/has-arglists? "mu/defn"))
    (is (forms/has-arglists? "mu/defn-")))
  (testing "def, defonce, defmacro etc do not"
    (is (not (forms/has-arglists? "def")))
    (is (not (forms/has-arglists? "defonce")))
    (is (not (forms/has-arglists? "defmacro")))
    (is (not (forms/has-arglists? "defmulti")))
    (is (not (forms/has-arglists? nil)))))

;; ============================================================
;; Integration: the >defn- extraction-closure bug
;; This test documents the bug we found — extraction-closure
;; must recognize >defn- as private so shared helpers get pulled in.
;; ============================================================

(deftest test-private-form-bug-regression
  (testing "the exact bug: (= \"defn-\" type) missed >defn- and mu/defn-"
    (is (forms/private-form? ">defn-")
        ">defn- must be private — extraction-closure was missing this")
    (is (forms/private-form? "mu/defn-")
        "mu/defn- must be private — same class of bug")))

;; ============================================================
;; deftest classification (PR #13, escherize)
;; ============================================================

(deftest test-classify-deftest
  (testing "deftest is recognized as a defining form"
    (is (= :deftest (forms/classify "deftest")))
    (is (forms/defining-form? "deftest")))
  (testing "deftest is not private"
    (is (not (forms/private-form? "deftest"))))
  (testing "deftest does not have arglists"
    (is (not (forms/has-arglists? "deftest")))))
