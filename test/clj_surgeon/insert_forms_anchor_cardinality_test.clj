(ns clj-surgeon.insert-forms-anchor-cardinality-test
  {:lane :fast}
  (:require
   [clj-surgeon.insert-forms-support :as h]
   [clojure.test :refer [deftest is]]))

;; @spec INSERT-FORMS-006
;; INTENT-TEST: INSERT-FORMS-006
(deftest insert-forms-anchor-cardinality

  (doseq [[s error n] [["(defn ab [] 2)" :anchor-not-found 0]
                       ["(defn a [] 1)\n(defn a [] 2)" :anchor-multiple-matches 2]]]
    (let [r (h/refused s (h/request s) error)]
      (is (= 1 (:expected r))) (is (= n (:actual r)))))
  (let [s "(deftest t (testing \"x\" 1) (testing \"x\" 2))"]
    (h/refused s (assoc (h/request s) :anchor
                   (assoc (h/body "deftest" "t" {:position "first"})
                          :testing_path [{:label "x" :expect 1}]))
               :anchor-multiple-matches)))
