(ns clj-surgeon.insert-forms-preservation-test
  {:lane :fast}
  (:require
   [clj-surgeon.insert-forms :as insert]
   [clj-surgeon.insert-forms-support :as h]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]])
  (:import
   (java.nio.file Files)))

;; @spec INSERT-FORMS-013
;; INTENT-TEST: INSERT-FORMS-013
(deftest insert-forms-other-top-level-hashes

  (let [s "(defn a [] 1)\n#_(def a :discard)\n42\n42\n(comment :keep)\n"
        r (insert/plan s (h/request s))]
    (is (= true (:ok r)))
    (is (= 5 (get-in r [:receipt :preservation :other_forms_checked])))
    (is (= true (get-in r [:receipt :preservation :other_forms_unchanged])))
    (is (= [1 2 3 4 5] (mapv :before_index (get-in r [:detail :preservation_entries]))))))
