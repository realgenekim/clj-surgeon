(ns clj-surgeon.insert-forms-spacing-test
  {:lane :fast}
  (:require [clj-surgeon.insert-forms-support :as h]
            [clojure.test :refer [deftest]]))

;; @spec INSERT-FORMS-021
;; INTENT-TEST: INSERT-FORMS-021
(deftest insert-forms-existing-separators
  (doseq [[source expected]
          [["(defn a [] 1)\n\n(def z 2)\n"
            "(defn a [] 1)\n\n(defn b [] 3)\n\n(def z 2)\n"]
           ["(defn a [] 1) ; tail\n\n(def z 2)\n"
            "(defn a [] 1) ; tail\n\n(defn b [] 3)\n\n(def z 2)\n"]]]
    (h/accepted source (h/request source) expected))
  (doseq [[source boundary payload expected]
          [["(deftest t\n  1\n\n  2)" {:position "after-child" :child 1} "3"
            "(deftest t\n  1\n\n  3\n\n  2)"]
           ["(deftest t\n  1\n\n  2)" {:position "last"} "3"
            "(deftest t\n  1\n\n  2\n\n  3)"]
           ["(defn a [])" {:position "first"} "1" "(defn a []\n  1)"]
           ["(deftest t\n)" {:position "last"} "3" "(deftest t\n  3)"]
           ["(deftest t)" {:position "last"} "3\n" "(deftest t\n  3\n  )"]
           ["(deftest t\n  1\n  2)" {:position "first"} "3\n"
            "(deftest t\n  3\n  1\n  2)"]]]
    (h/accepted source
                (assoc (h/request source)
                       :anchor (if (= source "(defn a [])")
                                 (h/body "defn" "a" boundary)
                                 (h/body "deftest" "t" boundary))
                       :payload {:text payload :forms 1}) expected)))
