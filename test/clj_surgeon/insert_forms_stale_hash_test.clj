(ns clj-surgeon.insert-forms-stale-hash-test
  {:lane :fast}
  (:require
   [clj-surgeon.insert-forms :as insert]
   [clj-surgeon.insert-forms-support :as h]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]])
  (:import
   (java.nio.file Files)))

;; @spec INSERT-FORMS-004
;; INTENT-TEST: INSERT-FORMS-004
(deftest insert-forms-stale-hash-refuses

  (let [s (str h/source "; changed\n")
        result (h/refused s (h/request h/source) :source-hash-mismatch)]
    (is (= "refresh-source" (:next_action result)))
    (is (= (h/sha h/source) (:expected result)))
    (is (= (h/sha s) (:actual result)))))
