(ns clj-surgeon.insert-forms-unbalanced-payload-test
  {:lane :fast}
  (:require
   [clj-surgeon.insert-forms :as insert]
   [clj-surgeon.insert-forms-support :as h]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]])
  (:import
   (java.nio.file Files)))

;; @spec INSERT-FORMS-003
;; INTENT-TEST: INSERT-FORMS-003
(deftest insert-forms-unbalanced-payload-refuses

  (doseq [text [")" "(defn b []" "(def b 1) ]"]]
    (h/refused h/source (assoc (h/request h/source) :payload {:text text :forms 1})
               :payload-parse-error)))
