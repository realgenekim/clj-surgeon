(ns clj-surgeon.insert-forms-preservation-test
  {:lane :fast}
  (:require
   [clj-surgeon.insert-forms :as insert]
   [clj-surgeon.insert-forms-oracle :as oracle]
   [clj-surgeon.insert-forms-support :as h]
   [clojure.java.io :as io]
   [clojure.string :as str]
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
    (when (:ok r)
      (let [b (:candidate r) receipt (:receipt r)]
        (doseq [bad [(str/replace b "(comment :keep)" "(comment  :keep)")
                     (str/replace b "#_(def a :discard)\n" "")
                     (str "(defn b [] 3)\n" s)]]
          (is (= false (:ok (oracle/verify s bad (h/request s) (assoc receipt :result_hash (h/sha bad)))))))
        (is (= false (:ok (oracle/verify s b (h/request s) (assoc receipt :result_hash (apply str (repeat 64 "0")))))))))
    (is (= [1 2 3 4 5] (mapv :before_index (get-in r [:detail :preservation_entries]))))))

;; The oracle must reject deliberately corrupted candidates even with a recomputed whole hash.
;; These checks belong to the required preservation witness, not a new census name.
