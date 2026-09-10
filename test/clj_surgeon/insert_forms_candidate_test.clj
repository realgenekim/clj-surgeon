(ns clj-surgeon.insert-forms-candidate-test
  {:lane :fast}
  (:require [clj-surgeon.insert-forms :as insert]
            [clj-surgeon.insert-forms-plan :as p]
            [clj-surgeon.insert-forms-support :as h]
            [clojure.test :refer [deftest is]]))

;; @spec INSERT-FORMS-019
;; INTENT-TEST: INSERT-FORMS-019
(deftest insert-forms-candidate-structure-refuses
  ;; Opus F1: splice one byte inside the anchor; syntax still parses.
  (h/with-file h/source
    (fn [_ file req]
      (binding [p/*splice-offset* dec]
        (let [result (insert/execute! req)]
          (is (= :candidate-structure-mismatch (:error-type result)))
          (is (= "refused" (:state result)))
          (is (= h/source (slurp file))))))))

;; @spec INSERT-FORMS-020
;; INTENT-TEST: INSERT-FORMS-020
(deftest insert-forms-parse-errors-refuse
  (doseq [[source perturb error]
          [["(defn a [] 1" identity :source-parse-error]
           [h/source (constantly "(") :candidate-parse-error]]]
    (h/with-file source
      (fn [_ file req]
        (binding [p/*candidate-text* perturb]
          (let [result (insert/execute! req)]
            (is (= error (:error-type result)) (pr-str result))
            (is (= "refused" (:state result)))
            (is (= source (slurp file)))))))))
