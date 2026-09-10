(ns clj-surgeon.insert-forms-deftest-nested-testing-test
  {:lane :fast}
  (:require
   [clj-surgeon.insert-forms :as insert]
   [clj-surgeon.insert-forms-support :as h]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]])
  (:import
   (java.nio.file Files)))

;; @spec INSERT-FORMS-002
;; INTENT-TEST: INSERT-FORMS-002
(deftest insert-forms-deftest-nested-testing

  (let [s "(deftest t\n  (testing \"outer\"\n    (testing \"inner\"\n      (is true)))\n  (testing \"other\" (testing \"inner\" (is false))))"
        a (assoc (h/body "deftest" "t" {:position "last"})
                 :testing_path [{:label "outer" :expect 1} {:label "inner" :expect 1}])
        r (assoc (h/request s) :anchor a :payload {:text "(is (= 1 1))" :forms 1})]
    (h/accepted s r "(deftest t\n  (testing \"outer\"\n    (testing \"inner\"\n      (is true)\n      (is (= 1 1))\n))\n  (testing \"other\" (testing \"inner\" (is false))))")))
