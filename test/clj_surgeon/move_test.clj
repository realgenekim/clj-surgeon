(ns clj-surgeon.move-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-surgeon.move :as move]
            [clojure.string :as str]))

(def test-source
  "(ns my.app)

(defn first-fn []
  :first)

(defn second-fn []
  :second)

(defn third-fn []
  (first-fn)
  :third)
")

(defn with-temp-file [source f]
  (let [tmp (java.io.File/createTempFile "ns-surgeon-move" ".clj")]
    (spit tmp source)
    (try
      (f (.getAbsolutePath tmp))
      (finally
        (.delete tmp)))))

(deftest test-dry-run
  (with-temp-file test-source
    (fn [path]
      (let [result (move/move-form {:file path
                                    :form "third-fn"
                                    :before "second-fn"
                                    :dry-run true})]
        (testing "dry run succeeds"
          (is (:ok result)))
        (testing "shows plan"
          (is (= "third-fn" (-> result :plan :form)))
          (is (= "second-fn" (-> result :plan :to-before))))
        (testing "file unchanged"
          (is (= test-source (slurp path))))))))

(deftest test-move-form-up
  (with-temp-file test-source
    (fn [path]
      (let [result (move/move-form {:file path
                                    :form "third-fn"
                                    :before "first-fn"})]
        (testing "move succeeds"
          (is (:ok result)))
        (testing "third-fn now appears before first-fn"
          (let [new-source (slurp path)
                third-pos (str/index-of new-source "third-fn")
                first-pos (str/index-of new-source "first-fn")]
            (is (some? third-pos))
            (is (some? first-pos))
            (is (< third-pos first-pos))))
        (testing "all forms still present"
          (let [new-source (slurp path)]
            (is (str/includes? new-source "first-fn"))
            (is (str/includes? new-source "second-fn"))
            (is (str/includes? new-source "third-fn"))))
        (testing "source is valid clojure (parens balanced)"
          (let [new-source (slurp path)
                opens (count (filter #(= \( %) new-source))
                closes (count (filter #(= \) %) new-source))]
            (is (= opens closes))))))))

(deftest test-move-form-down
  (with-temp-file test-source
    (fn [path]
      (let [result (move/move-form {:file path
                                    :form "first-fn"
                                    :before "third-fn"})]
        (testing "move succeeds"
          (is (:ok result)))
        (testing "first-fn now appears after second-fn but before third-fn"
          (let [new-source (slurp path)
                second-pos (str/index-of new-source "(defn second-fn")
                first-pos (str/index-of new-source "(defn first-fn")
                third-pos (str/index-of new-source "(defn third-fn")]
            (is (< second-pos first-pos))
            (is (< first-pos third-pos))))))))

(deftest test-move-nonexistent-form
  (with-temp-file test-source
    (fn [path]
      (let [result (move/move-form {:file path
                                    :form "nope"
                                    :before "first-fn"})]
        (is (:error result))
        (is (str/includes? (:error result) "nope"))))))

(deftest test-move-skips-declare
  (let [source "(ns my.app)

(declare my-fn)

(defn caller []
  (my-fn 42))

(defn my-fn [x]
  (+ x 1))
"]
    (with-temp-file source
      (fn [path]
        (testing "dry-run targets the defn, not the declare"
          (let [result (move/move-form {:file path
                                        :form "my-fn"
                                        :before "caller"
                                        :dry-run true})]
            (is (:ok result))
            ;; Should find the defn (line 8), NOT the declare (line 3)
            (is (> (-> result :plan :from-line) 5))))
        (testing "move actually moves the defn body"
          (let [result (move/move-form {:file path
                                        :form "my-fn"
                                        :before "caller"})]
            (is (:ok result))
            (let [new-source (slurp path)
                  defn-pos (str/index-of new-source "(defn my-fn")
                  caller-pos (str/index-of new-source "(defn caller")]
              ;; defn my-fn should now appear BEFORE defn caller
              (is (some? defn-pos))
              (is (some? caller-pos))
              (is (< defn-pos caller-pos)))))))))

(deftest test-move-with-declare-and-defn
  (let [source "(ns my.app)

(declare helper)

(defn main []
  (helper 1))

(defn middle []
  :ok)

(defn helper [x]
  (inc x))
"]
    (with-temp-file source
      (fn [path]
        (testing "moves defn helper before main, declare stays"
          (let [result (move/move-form {:file path
                                        :form "helper"
                                        :before "main"})]
            (is (:ok result))
            (let [new-source (slurp path)]
              ;; defn helper should appear before defn main
              (is (< (str/index-of new-source "(defn helper")
                     (str/index-of new-source "(defn main")))
              ;; declare should still be in the file (we don't auto-remove it)
              (is (str/includes? new-source "(declare helper)"))
              ;; all forms still present
              (is (str/includes? new-source "(defn middle")))))))))

(deftest test-move-with-comments
  (let [source "(ns my.app)

;; Helper function
;; Does important stuff
(defn helper []
  :help)

(defn main []
  (helper))
"]
    (with-temp-file source
      (fn [path]
        (let [result (move/move-form {:file path
                                      :form "main"
                                      :before "helper"})]
          (is (:ok result))
          (testing "comments stay with their form"
            (let [new-source (slurp path)]
              ;; helper's comments should still precede helper
              (let [comment-pos (str/index-of new-source ";; Helper function")
                    helper-pos (str/index-of new-source "(defn helper")]
                (is (< comment-pos helper-pos))))))))))

;; ============================================================
;; Dependency validation: :mv should warn when destination
;; creates new unresolved references (the Whac-A-Mole bug)
;; ============================================================

(deftest test-move-creates-new-forward-ref
  (let [source "(ns my.app)

(declare foo)

(defn bar []
  (foo 42))

(def config {:x 1})

(defn foo [x]
  (+ x (:x config)))
"]
    (with-temp-file source
      (fn [path]
        (testing "moving foo above bar creates new forward ref to config"
          ;; foo depends on config (line 8). Moving foo to before bar (line 5)
          ;; means foo references config before it's defined.
          ;; The tool should detect this and warn.
          (let [result (move/move-form {:file path
                                        :form "foo"
                                        :before "bar"
                                        :dry-run true})]
            (is (:ok result))
            ;; TODO: once we add :unresolved-deps to dry-run output,
            ;; test that it warns about config
            ;; (is (contains? (set (:unresolved-deps (:plan result))) "config"))
            ))))))

(deftest test-move-safe-when-deps-above
  (let [source "(ns my.app)

(def config {:x 1})

(declare foo)

(defn bar []
  (foo 42))

(defn foo [x]
  (+ x (:x config)))
"]
    (with-temp-file source
      (fn [path]
        (testing "moving foo above bar is safe because config is above both"
          (let [result (move/move-form {:file path
                                        :form "foo"
                                        :before "bar"
                                        :dry-run true})]
            (is (:ok result))
            ;; config is at line 3, destination is line 7
            ;; foo's dependency (config) is satisfied
            ))))))
