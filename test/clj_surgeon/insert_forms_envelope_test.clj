(ns clj-surgeon.insert-forms-envelope-test
  {:lane :fast}
  (:require [clj-surgeon.insert-forms :as insert]
            [clj-surgeon.insert-forms-plan :as p]
            [clj-surgeon.insert-forms-support :as h]
            [clojure.test :refer [deftest is]]))

;; @spec INSERT-FORMS-022
;; INTENT-TEST: INSERT-FORMS-022
(deftest insert-forms-refusal-remedies
  (doseq [[error words]
          [[:invalid-request #"closed request schema"]
           [:invalid-path #"regular.*canonical workspace"]
           [:invalid-guard #"exactly one.*sha256.*read_receipt"]
           [:unsupported-source #"UTF-8.*LF.*CRLF"]
           [:unsupported-payload-syntax #"reader-discard.*reader-eval.*reader conditionals"]
           [:limit-exceeded #"Reduce.*reported limit"]
           [:source-hash-mismatch #"Refresh the guard from a fresh read"]
           [:source-changed-before-commit #"changed before publication.*fresh read"]
           [:unsupported-indentation #"spaces.*tabs"]
           [:unsupported-owner-shape #"supported.*body header"]
           [:anchor-not-found #"existing.*owner or direct testing label"]
           [:anchor-multiple-matches #"unique.*candidates"]
           [:anchor-ambiguous #"arity.*candidates.*\[\]"]
           [:anchor-index-out-of-range #"1-based.*reported.*count"]
           [:payload-parse-error #"Repair.*payload.*line.*column"]
           [:source-parse-error #"Repair.*source.*fresh guard"]
           [:candidate-parse-error #"candidate.*parser.*do not replay"]
           [:candidate-structure-mismatch #"candidate.*placement.*do not replay"]
           [:payload-form-count-mismatch #"payload.forms.*effective.*count"]]]
    (let [result (p/refusal (ex-info "refusal" {:error-type error :at [:fixture]
                                               :candidates [{:kind "arity" :name "[]" :line 1}]}))]
      (is (= error (:error-type result)))
      (is (re-find words (:remedy result)) (pr-str result)))))

;; @spec INSERT-FORMS-023
;; INTENT-TEST: INSERT-FORMS-023
(deftest insert-forms-candidate-envelope
  (doseq [[source anchor error expected]
          [["(defn a [] 1)\n(defn a [] 2)\n" nil :anchor-multiple-matches
            [{:kind "defn" :name "a" :line 1} {:kind "defn" :name "a" :line 2}]]
           ["(defn a\n  ([] 1)\n  ([x] x))" (h/body "defn" "a" {:position "last"})
            :anchor-ambiguous [{:kind "arity" :name "[]" :line 2}
                               {:kind "arity" :name "[x]" :line 3}]]]]
    (h/with-file source
      (fn [_ file req]
        (let [result (insert/execute! (cond-> req anchor (assoc :anchor anchor)))]
          (is (= error (:error-type result)))
          (is (= source (slurp file)))
          (is (= expected (mapv #(select-keys % [:kind :name :line]) (:candidates result))))
          (is (false? (:candidates_truncated result))))))))
