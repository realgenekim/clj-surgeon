(ns clj-surgeon.insert-forms-body-boundaries-test
  {:lane :fast}
  (:require
   [clj-surgeon.insert-forms-support :as h]
   [clojure.test :refer [deftest]]))

;; @spec INSERT-FORMS-007
;; INTENT-TEST: INSERT-FORMS-007
(deftest insert-forms-body-boundaries

  (doseq [[s boundary expected]
          [["(deftest t\n  1\n  2)" {:position "first"} "(deftest t\n  3\n\n  1\n  2)"]
           ["(deftest t\n  1\n  2)" {:position "last"} "(deftest t\n  1\n  2\n  3\n)"]
           ["(deftest t\n  1\n  2)" {:position "after-child" :child 1} "(deftest t\n  1\n  3\n\n  2)"]
           ["(deftest t)" {:position "first"} "(deftest t\n  3\n)"]
           ["(deftest t)" {:position "last"} "(deftest t\n  3\n)"]]]
    (h/accepted s (assoc (h/request s) :anchor (h/body "deftest" "t" boundary)
                    :payload {:text "3" :forms 1}) expected))
  (doseq [[boundary error] [[{:position "after-child" :child 0} :invalid-request]
                            [{:position "after-child" :child 3} :anchor-index-out-of-range]
                            [{:position "first" :child 1} :invalid-request]]]
    (let [s "(deftest t 1 (testing \"x\" 2 3))"]
      (h/refused s (assoc (h/request s) :anchor (h/body "deftest" "t" boundary)) error))))
