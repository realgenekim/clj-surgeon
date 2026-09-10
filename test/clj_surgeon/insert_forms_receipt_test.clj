(ns clj-surgeon.insert-forms-receipt-test
  {:lane :fast}
  (:require
   [clj-surgeon.insert-forms :as insert]
   [clj-surgeon.insert-forms-support :as h]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]))

;; @spec INSERT-FORMS-016
;; INTENT-TEST: INSERT-FORMS-016
(deftest insert-forms-receipt-accounting
  (h/with-file h/source
    (fn [dir _ req]
      (let [relative (str (str/join "/" (repeat 16 (apply str (repeat 125 "x")))) "/example.clj")
            file (io/file dir relative)
            _ (io/make-parents file)
            _ (spit file h/source)
            result (insert/execute! (assoc req :file relative))]
        (is (= :limit-exceeded (:error-type result)))
        (is (= "refused" (:state result)))
        (is (false? (:mutation_attempted result)))
        (is (= h/source (slurp file)))
        (is (<= (count (insert/receipt-text result)) 4096)))))

  (h/with-file h/source
    (fn [_ file req]
      (let [result (insert/execute! (assoc req :payload {:text (str "9" (apply str (repeat 20000 "a"))) :forms 1}))]
        (is (= :payload-parse-error (:error-type result)))
        (is (= h/source (slurp file)))
        (is (<= (alength (.getBytes (insert/receipt-text result) "UTF-8")) 4096)))))
  (h/with-file h/source
    (fn [_ _ req]
      (let [result (insert/execute! (assoc req :payload {:text (apply str (repeat 1000 "1\n")) :forms 1000}))]
        (is (= true (:ok result)))
        (is (<= (alength (.getBytes (insert/receipt-text result) "UTF-8")) 4096)))))
  (let [r (insert/plan h/source (h/request h/source))
        receipt (:receipt r)]
    (is (= "81f14e0bae64ca76014a2c2368a7ca83dedb374ef244f8c5019b22ab61eb2b12" (:source_hash receipt)))
    (is (= "42941c271c531c0c058a984996826c4b65c2cec5d355b09965d8f03b409f0454" (:result_hash receipt)))
    (is (= 14 (:bytes_added receipt)))
    (is (= "a" (get-in r [:detail :resolved_anchor :owner :name])))
    (is (seq (get-in r [:detail :trivia_spans])))
    (is (= {:offset 14 :length 14 :sha256 "b30dac5980dce85bdbf4130d40cdb14f8f4acc4640cfaacbe9eeb5b24dc6e5b4"} (:splice receipt)))
    (is (= {:start 2 :end 2} (:line_range receipt)))
    (is (= [{:ordinal 1 :start_line 2 :end_line 2}] (:inserted_form_ranges receipt)))
    (is (= {:prefix_sha256 "9f7f8b28df3ae36aea970ba670a8939a2cd35620f77ce6cdb5723fb221c9a948"
            :suffix_sha256 "83f9249d855af8169bc768f86b07677f3ff636f1b477ed7399bd06511fbe7a7f"
            :other_forms_checked 2 :other_forms_unchanged true} (:preservation receipt)))))
