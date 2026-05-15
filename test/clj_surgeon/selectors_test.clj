(ns clj-surgeon.selectors-test
  "Tests for the field-extraction selector DSL.

   Strategy: parse each Metabase macro shape with rewrite-clj, then run
   `resolve-fields` against the form zloc with various selector specs.
   Assert the extracted values match what we'd want :ls to emit."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [rewrite-clj.zip :as z]
            [clj-surgeon.selectors :as sel]))

;; ============================================================
;; Helpers — parse a form string, give us a zloc on the outer list.
;; ============================================================

(defn- form-zloc
  "Parse a single top-level form string into a zloc pointing at the list."
  [s]
  (z/of-string s))

;; ============================================================
;; field-order — topo sort by :right-of / :join refs
;; ============================================================

(deftest test-field-order-no-deps
  (testing "fields with no refs come back in any order"
    (let [order (sel/field-order {:name [:nth 1]
                                  :arglist [:find-first :vector]})]
      (is (= #{:name :arglist} (set order))))))

(deftest test-field-order-right-of-dep
  (testing ":right-of forces its referent to be resolved first"
    (let [order (sel/field-order {:path     [:right-of :method]
                                  :method   [:find-first :keyword]
                                  :arglist  [:find-first :vector]})]
      (is (< (.indexOf order :method) (.indexOf order :path))
          ":method must appear before :path"))))

(deftest test-field-order-join-deps
  (testing ":join references multiple fields"
    (let [order (sel/field-order
                 {:name    [:join " " :method :path]
                  :method  [:find-first :keyword]
                  :path    [:right-of :method]})]
      (is (< (.indexOf order :method) (.indexOf order :name)))
      (is (< (.indexOf order :path)   (.indexOf order :name))))))

(deftest test-field-order-cycle-throws
  (testing "cyclic refs throw at field-order time"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Cyclic field"
                          (sel/field-order {:a [:right-of :b]
                                            :b [:right-of :a]})))))

(deftest test-field-order-dangling-ref-throws
  (testing "ref to unknown field throws"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"unknown field"
                          (sel/field-order {:name [:right-of :ghost]})))))

;; ============================================================
;; resolve-fields against real Metabase macro shapes
;; ============================================================

(deftest test-defn-defaults
  (testing "plain defn — name + arglist via :defn default selectors"
    (let [z (form-zloc "(defn greet \"doc\" [user] (str \"hi \" user))")
          out (sel/resolve-fields z
                {:name    [:nth 1]
                 :arglist [:find-first :vector]})]
      (is (= "greet" (:name out)))
      (is (= "[user]" (:arglist out))))))

(deftest test-mu-defn-with-meta-arglist
  (testing "mu/defn with ^String meta arglist — meta auto-unwrapped"
    (let [z (form-zloc
             "(mu/defn prefix :- ::schema
                \"doc\"
                ^String [k :- :string]
                (subs k 0 3))")
          out (sel/resolve-fields z
                {:name    [:nth 1]
                 :arglist [:find-first :vector]})]
      (is (= "prefix" (:name out)))
      (is (= "[k :- :string]" (:arglist out))
          "outer ^String stripped; inner :- preserved"))))

(deftest test-defendpoint-synth-name
  (testing "defendpoint — method + path -> synthesized name"
    (let [z (form-zloc
             "(api.macros/defendpoint :get \"/:key\"
                \"doc\"
                [{k :key} :- [:map [:key string?]]]
                body)")
          out (sel/resolve-fields z
                {:method  [:find-first :keyword]
                 :path    [:right-of :method]
                 :name    [:join " " :method :path]
                 :arglist [:find-first :vector]})]
      (is (= ":get" (:method out)))
      (is (= "\"/:key\"" (:path out)))
      (is (= ":get \"/:key\"" (:name out))
          "synthesized name combines method + path")
      (is (str/starts-with? (:arglist out) "[{k :key}")))))

(deftest test-defenterprise-ee-namespace
  (testing "defenterprise — name, ee-namespace, arglist"
    (let [z (form-zloc
             "(defenterprise enable-custom-viz?
                \"doc\"
                metabase-enterprise.custom-viz-plugin.settings
                []
                false)")
          out (sel/resolve-fields z
                {:name         [:nth 1]
                 :docstring    [:when-type :string [:nth 2]]
                 :ee-namespace [:when-type :symbol [:right-of :docstring]]
                 :arglist      [:find-first :vector]})]
      (is (= "enable-custom-viz?" (:name out)))
      (is (= "\"doc\"" (:docstring out)))
      (is (= "metabase-enterprise.custom-viz-plugin.settings"
             (:ee-namespace out)))
      (is (= "[]" (:arglist out))))))

(deftest test-defenterprise-multi-arg
  (testing "defenterprise — arglist is the FIRST vector child (skips ee-ns symbol)"
    (let [z (form-zloc
             "(defenterprise multi-arg-fn
                \"doc\"
                metabase-enterprise.somewhere
                [a b c]
                (+ a b c))")
          out (sel/resolve-fields z
                {:name    [:nth 1]
                 :arglist [:find-first :vector]})]
      (is (= "multi-arg-fn" (:name out)))
      (is (= "[a b c]" (:arglist out))))))

(deftest test-defsetting-shape
  (testing "defsetting — just name, no arglist field needed"
    (let [z (form-zloc
             "(defsetting application-name
                (deferred-tru \"doc\")
                :encryption :no
                :default \"Metabase\")")
          out (sel/resolve-fields z
                {:name [:nth 1]})]
      (is (= "application-name" (:name out)))
      (is (not (contains? out :arglist))))))

;; ============================================================
;; :when-type — type-mismatch returns nil, not the wrong value
;; ============================================================

(deftest test-when-type-rejects-mismatch
  (testing "when-type returns nil if the inner selector hits the wrong type"
    (let [z (form-zloc "(defn foo (bar) [a] body)")  ;; child 2 is a list, not string
          out (sel/resolve-fields z
                {:name    [:nth 1]
                 :docstring {:select [:when-type :string [:nth 2]]
                             :optional? true}})]
      (is (= "foo" (:name out)))
      (is (not (contains? out :docstring))
          ":docstring slot was a list, not string — field omitted"))))

;; ============================================================
;; :find-first — meta auto-unwrap
;; ============================================================

(deftest test-find-first-unwraps-meta
  (testing ":find-first :vector finds meta-wrapped vector"
    (let [z (form-zloc "(defn foo ^String [a b] body)")
          out (sel/resolve-fields z {:arglist [:find-first :vector]})]
      (is (= "[a b]" (:arglist out))))))

;; ============================================================
;; Optional fields — missing on non-optional throws
;; ============================================================

(deftest test-required-field-missing-throws
  (testing "non-optional field that doesn't resolve throws"
    (let [z (form-zloc "(defn foo)")] ;; no arglist
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Required field"
                            (sel/resolve-fields z
                              {:arglist {:select [:find-first :vector]
                                         :optional? false}}))))))

(deftest test-optional-field-missing-omitted
  (testing "optional field that doesn't resolve is omitted from result"
    (let [z (form-zloc "(defn foo)")]
      (is (= {} (sel/resolve-fields z
                  {:arglist {:select [:find-first :vector]
                             :optional? true}}))))))
