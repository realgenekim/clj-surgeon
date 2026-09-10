(ns clj-surgeon.insert-forms-payload-count-test
  {:lane :fast}
  (:require
   [clj-surgeon.insert-forms :as insert]
   [clj-surgeon.insert-forms-support :as h]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]])
  (:import
   (java.nio.file Files)))

;; @spec INSERT-FORMS-009
;; INTENT-TEST: INSERT-FORMS-009
(deftest insert-forms-payload-count-and-order

  (doseq [text ["" "; only comment\n" "1 2"]]
    (h/refused h/source (assoc (h/request h/source) :payload {:text text :forms 1})
               :payload-form-count-mismatch))
  (h/refused h/source (assoc (h/request h/source) :payload {:text "#_1 2" :forms 1})
             :unsupported-payload-syntax)
  (h/accepted h/source (assoc (h/request h/source) :payload {:text "'x #foo/bar 2" :forms 2})
              "(defn a [] 1)\n'x #foo/bar 2\n\n(defn ab [] 2)\n"))
