(ns clj-surgeon.insert-forms-receipt-test
  {:lane :fast}
  (:require
   [clj-surgeon.insert-forms :as insert]
   [clj-surgeon.insert-forms-support :as h]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]])
  (:import
   (java.nio.file Files)))

;; @spec INSERT-FORMS-016
;; INTENT-TEST: INSERT-FORMS-016
(deftest insert-forms-receipt-accounting

  (h/with-file h/source
    (fn [_ _ req]
      (let [result (insert/execute! (assoc req :payload {:text (apply str (repeat 1000 "1\n")) :forms 1000}))]
        (is (= true (:ok result)))
        (is (<= (alength (.getBytes (insert/receipt-text result) "UTF-8")) 4096)))))
  (let [r (insert/plan h/source (h/request h/source))
        receipt (:receipt r)]
    (is (= "81f14e0bae64ca76014a2c2368a7ca83dedb374ef244f8c5019b22ab61eb2b12" (:source_hash receipt)))
    (is (= "e51722e43edda0df0cc6689090281ba4ab7e946806711b266950d370d865d247" (:result_hash receipt)))
    (is (= 15 (:bytes_added receipt)))
    (is (= "a" (get-in r [:detail :resolved_anchor :owner :name])))
    (is (seq (get-in r [:detail :trivia_spans])))
    (is (= {:offset 13 :length 15 :sha256 "6e460fd413f380e275e9c769cb90498cf7804956fd893f56816ef521a9c323ef"} (:splice receipt)))
    (is (= {:start 1 :end 2} (:line_range receipt)))
    (is (= [{:ordinal 1 :start_line 2 :end_line 2}] (:inserted_form_ranges receipt)))
    (is (= {:prefix_sha256 "1d1f4497724ad81799012397fd4781c16ca5cc6fcbaec3a42d75dfd80f2d160e"
            :suffix_sha256 "c35754ffc009bb12060d89d0f63814c80d264267a52f6528392ac3e02a3bafe4"
            :other_forms_checked 2 :other_forms_unchanged true} (:preservation receipt)))))
