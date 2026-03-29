(ns ns-surgeon.move-test
  (:require [clojure.test :refer [deftest is testing]]
            [ns-surgeon.move :as move]
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
