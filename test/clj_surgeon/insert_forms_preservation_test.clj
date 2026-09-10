(ns clj-surgeon.insert-forms-preservation-test
  {:lane :fast}
  (:require
   [clj-surgeon.insert-forms :as insert]
   [clj-surgeon.insert-forms-oracle :as oracle]
   [clj-surgeon.insert-forms-support :as h]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]))

;; @spec INSERT-FORMS-013
;; INTENT-TEST: INSERT-FORMS-013
(deftest insert-forms-other-top-level-hashes

  (let [s "(defn a [] 1)\n#_(def a :discard)\n42\n42\n; semicolon survives\n(comment :keep)\n"
        r (insert/plan s (h/request s))]
    (is (= true (:ok r)))
    (is (= 5 (get-in r [:receipt :preservation :other_forms_checked])))
    (is (= true (get-in r [:receipt :preservation :other_forms_unchanged])))
    (when (:ok r)
      (let [b (:candidate r) receipt (:receipt r)]
        (doseq [bad [(str/replace b "(comment :keep)" "(comment  :keep)")
                     (str/replace b "#_(def a :discard)\n" "")
                     (str/replace b "; semicolon survives\n" "")
                     (str "(defn b [] 3)\n" s)]]
          (is (= false (:ok (oracle/verify s bad (h/request s) (assoc receipt :result_hash (h/sha bad)))))))
        (is (= false (:ok (oracle/verify s b (h/request s) (assoc receipt :result_hash (apply str (repeat 64 "0")))))))))
    (is (= [1 2 3 4 5] (mapv :before_index (get-in r [:detail :preservation_entries])))))
  (let [s "(defn a [] 1)\n(defn z [] 2)\n"
        req (assoc (h/request s) :anchor (h/body "defn" "a" {:position "last"})
              :payload {:text "42" :forms 1})
        result (insert/plan s req)
        wrong "(defn a [] 1)\n(defn z [] 2\n42\n)\n"]
    (is (:ok result))
    (is (false? (:ok (oracle/verify s wrong req
                       (assoc (:receipt result) :result_hash (h/sha wrong))))))))

;; Wrong-body insertion must fail even when its overall digest is internally consistent.

;; The oracle must reject deliberately corrupted candidates even with a recomputed whole hash.
;; These checks belong to the required preservation witness, not a new census name.
