(ns clj-surgeon.fix-declares-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-surgeon.fix-declares :as fix]
            [clojure.string :as str]
            [clojure.java.io :as io]))

(defn with-temp-file [source f]
  (let [tmp (java.io.File/createTempFile "fix-declares" ".clj")]
    (spit tmp source)
    (try
      (f (.getAbsolutePath tmp))
      (finally
        (.delete tmp)))))

;; ============================================================
;; Simple case: one declare, one forward ref
;; ============================================================

(def simple-forward-ref
  "(ns my.app)

(declare helper)

(defn main []
  (helper 42))

(defn helper [x]
  (inc x))
")

(deftest test-plan-simple
  (with-temp-file simple-forward-ref
    (fn [path]
      (let [p (fix/plan path)]
        (testing "finds one removable declare"
          (is (= 1 (-> p :summary :removable))))
        (testing "action has correct form name"
          (is (= "helper" (-> p :actions first :name))))
        (testing "action has move-before target"
          (is (some? (-> p :actions first :move-before))))
        (testing "no unsafe actions"
          (is (= 0 (-> p :summary :unsafe))))))))

(deftest test-execute-simple
  (with-temp-file simple-forward-ref
    (fn [path]
      (let [result (fix/execute! path)
            new-source (slurp path)]
        (testing "moves happened"
          (is (pos? (-> result :summary :moves))))
        (testing "declare deleted"
          (is (not (str/includes? new-source "(declare helper)"))))
        (testing "defn helper now before defn main"
          (is (< (str/index-of new-source "(defn helper")
                 (str/index-of new-source "(defn main"))))
        (testing "all forms still present"
          (is (str/includes? new-source "(defn helper"))
          (is (str/includes? new-source "(defn main")))
        (testing "parens balanced"
          (let [opens (count (filter #(= \( %) new-source))
                closes (count (filter #(= \) %) new-source))]
            (is (= opens closes))))))))

;; ============================================================
;; Multiple declares
;; ============================================================

(def multi-declares
  "(ns my.app)

(declare foo)
(declare bar)

(defn caller-a []
  (foo 1))

(defn caller-b []
  (bar 2))

(defn foo [x]
  (+ x 10))

(defn bar [x]
  (* x 20))
")

(deftest test-execute-multiple
  (with-temp-file multi-declares
    (fn [path]
      (let [result (fix/execute! path)
            new-source (slurp path)]
        (testing "both declares removed"
          (is (not (str/includes? new-source "(declare foo)")))
          (is (not (str/includes? new-source "(declare bar)"))))
        (testing "foo before caller-a"
          (is (< (str/index-of new-source "(defn foo")
                 (str/index-of new-source "(defn caller-a"))))
        (testing "bar before caller-b"
          (is (< (str/index-of new-source "(defn bar")
                 (str/index-of new-source "(defn caller-b"))))))))

;; ============================================================
;; Mixed: some removable, some needed (mutual recursion)
;; ============================================================

(def mixed-declares
  "(ns my.app)

(declare ping)
(declare helper)

(defn main []
  (helper (ping 5)))

(defn helper [x]
  (inc x))

(defn ping [x]
  (when (pos? x)
    (pong (dec x))))

(defn pong [x]
  (when (pos? x)
    (ping (dec x))))
")

(deftest test-plan-mixed
  (with-temp-file mixed-declares
    (fn [path]
      (let [p (fix/plan path)]
        (testing "finds helper as removable"
          (is (some #(= "helper" (:name %)) (:actions p))))
        (testing "ping is in a cycle — needed declare"
          (is (some #{"ping"} (:needed-declares p))))))))

;; ============================================================
;; Leaf dep-pulling: form has deps below target, but all are leaves
;; ============================================================

(def pullable-deps
  "(ns my.app)

(declare foo)

(defn caller []
  (foo 42))

(def config {:x 1})

(defn foo [x]
  (+ x (:x config)))
")

(deftest test-plan-pullable
  (with-temp-file pullable-deps
    (fn [path]
      (let [p (fix/plan path)]
        (testing "foo has pull-deps (config is a leaf)"
          (let [action (first (:actions p))]
            (is (= "foo" (:name action)))
            (is (contains? (:pull-deps action) "config"))
            (is (nil? (:unresolved-deps action)))))
        (testing "summary shows safe-with-pull"
          (is (= 1 (-> p :summary :safe-with-pull))))))))

(deftest test-execute-pullable
  (with-temp-file pullable-deps
    (fn [path]
      (let [result (fix/execute! path)
            new-source (slurp path)]
        (testing "config moved above caller (pulled)"
          (is (< (str/index-of new-source "(def config")
                 (str/index-of new-source "(defn caller"))))
        (testing "foo moved above caller"
          (is (< (str/index-of new-source "(defn foo")
                 (str/index-of new-source "(defn caller"))))
        (testing "declare deleted"
          (is (not (str/includes? new-source "(declare foo)"))))
        (testing "parens balanced"
          (let [opens (count (filter #(= \( %) new-source))
                closes (count (filter #(= \) %) new-source))]
            (is (= opens closes))))))))

;; ============================================================
;; Unsafe move detection: form depends on something below target
;; ============================================================

(def unsafe-move
  "(ns my.app)

(declare foo)

(defn caller []
  (foo 42))

(defn deep-helper [x]
  (+ x 1))

(defn compute [x]
  (deep-helper x))

(defn foo [x]
  (compute x))
")

(deftest test-plan-unsafe
  (with-temp-file unsafe-move
    (fn [path]
      (let [p (fix/plan path)]
        (testing "detects unresolved non-leaf dep"
          (is (= 1 (-> p :summary :unsafe))))
        (testing "flags compute as unresolved"
          (let [action (first (:actions p))]
            (is (contains? (:unresolved-deps action) "compute"))))))))

(deftest test-execute-skips-unsafe
  (with-temp-file unsafe-move
    (fn [path]
      (let [result (fix/execute! path)
            new-source (slurp path)]
        (testing "unsafe move skipped"
          (is (= 1 (-> result :summary :skipped))))
        (testing "declare still present (not safe to remove)"
          (is (str/includes? new-source "(declare foo)")))
        (testing "foo NOT moved (would break)"
          (is (> (str/index-of new-source "(defn foo")
                 (str/index-of new-source "(defn caller"))))))))

;; ============================================================
;; No declares — nothing to do
;; ============================================================

(def no-declares
  "(ns my.app)

(defn a [] 1)
(defn b [] (a))
")

(deftest test-no-declares
  (with-temp-file no-declares
    (fn [path]
      (let [p (fix/plan path)]
        (testing "nothing to do"
          (is (= 0 (-> p :summary :removable))))))))

;; ============================================================
;; Stale declare — defn already above all callers
;; ============================================================

(def stale-declare
  "(ns my.app)

(declare helper)

(defn helper [x]
  (inc x))

(defn main []
  (helper 42))
")

(deftest test-stale-declare
  (with-temp-file stale-declare
    (fn [path]
      (let [p (fix/plan path)]
        (testing "declare is stale"
          (is (= 1 (-> p :summary :stale))))))))

(deftest test-execute-stale-declare
  (with-temp-file stale-declare
    (fn [path]
      (let [result (fix/execute! path)
            new-source (slurp path)]
        (testing "stale declare deleted"
          (is (not (str/includes? new-source "(declare helper)"))))
        (testing "defn helper still present and above main"
          (is (str/includes? new-source "(defn helper"))
          (is (< (str/index-of new-source "(defn helper")
                 (str/index-of new-source "(defn main"))))))))
