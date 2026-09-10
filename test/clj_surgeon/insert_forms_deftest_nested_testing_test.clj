(ns clj-surgeon.insert-forms-deftest-nested-testing-test
  {:lane :fast}
  (:require
   [clj-surgeon.insert-forms-support :as h]
   [clojure.test :refer [deftest]]))

;; @spec INSERT-FORMS-002
;; INTENT-TEST: INSERT-FORMS-002
(deftest insert-forms-deftest-nested-testing

  (let [f (h/fixtures) s (:test-text f)
        req (assoc (h/request s)
                   :anchor (assoc (h/body "deftest" "admitted" {:position "last"})
                                  :testing_path [{:label (:outer-label f) :expect 1}
                                                 {:label (:inner-label f) :expect 1}])
                   :payload {:text "(is (= 1 1))" :forms 1})]
    (h/accepted s req (str (subs s 0 (- (count s) 3)) "\n      (is (= 1 1)))))")))
  (let [s "(deftest t\n  (testing \"outer\"\n    (testing \"inner\"\n      (is true)))\n  (testing \"other\" (testing \"inner\" (is false))))"
        a (assoc (h/body "deftest" "t" {:position "last"})
                 :testing_path [{:label "outer" :expect 1} {:label "inner" :expect 1}])
        r (assoc (h/request s) :anchor a :payload {:text "(is (= 1 1))" :forms 1})]
    (h/accepted s r "(deftest t\n  (testing \"outer\"\n    (testing \"inner\"\n      (is true)\n      (is (= 1 1))))\n  (testing \"other\" (testing \"inner\" (is false))))")))
