(ns clj-surgeon.rename-alias-candidate-test
  {:lane :fast}
  (:require
   [clj-surgeon.insert-forms-plan :as forms]
   [clj-surgeon.insert-forms-support :as h]
   [clj-surgeon.rename-alias :as sut]
   [clj-surgeon.rename-alias-plan :as p]
   [clj-surgeon.rename-alias-test :as r]
   [clj-surgeon.txn-journal :as journal]
   [clojure.test :refer [deftest is]]))

(def source (str r/header "(def y events/x)\n(def untouched 1)\n"))
(defn fault-result [seam fault]
  (h/with-file source
    (fn [dir file _]
      (let [req (assoc (r/request {r/file source} 1) :workspace_root (.getCanonicalPath dir))
            result (with-bindings {seam fault} (sut/execute! req))]
        (is (= :candidate-structure-mismatch (:error-type result)) (pr-str result))
        (is (= source (slurp file)))
        result))))

;; @spec RENAME-ALIAS-012
;; INTENT-TEST: RENAME-ALIAS-012
(deftest candidate-role-recount-refuses
  (fault-result #'p/*candidate-roles* (constantly [])))

;; @spec RENAME-ALIAS-012
;; INTENT-TEST: RENAME-ALIAS-012
(deftest candidate-form-preservation-refuses
  (fault-result #'p/*preservation-tree*
                #(forms/tree (str (:source %) " ") :candidate)))

;; @spec RENAME-ALIAS-012
;; INTENT-TEST: RENAME-ALIAS-012
(deftest candidate-inverse-identity-refuses
  (fault-result #'p/*inverse-evidence* #(assoc-in % [0 :before] "broken")))

;; @spec RENAME-ALIAS-012
;; INTENT-TEST: RENAME-ALIAS-012
(deftest candidate-parse-refuses
  (h/with-file source
    (fn [dir file _]
      (let [req (assoc (r/request {r/file source} 1) :workspace_root (.getCanonicalPath dir))
            result (binding [p/*candidate-text* (constantly "(")] (sut/execute! req))]
        (is (= :candidate-parse-error (:error-type result)))
        (is (= source (slurp file)))))))

;; @spec RENAME-ALIAS-012
;; INTENT-TEST: RENAME-ALIAS-012
(deftest replacement-read-back-refuses
  (h/with-file source
    (fn [dir file _]
      (let [req (assoc (r/request {r/file source} 1) :workspace_root (.getCanonicalPath dir))
            armed (atom false) sha journal/sha256-file
            result (with-redefs [journal/sha256-file
                                 (fn [path] (if (compare-and-set! armed true false)
                                              (h/sha "faulty read") (sha path)))]
                     (sut/execute! req {:read-back #(reset! armed true)}))]
        (is (= :io-error (:error-type result)) (pr-str result))
        (is (= "rolled-back" (:state result)))
        (is (= source (slurp file)))))))
