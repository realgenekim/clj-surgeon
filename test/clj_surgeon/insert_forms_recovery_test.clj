(ns clj-surgeon.insert-forms-recovery-test
  {:lane :fast}
  (:require [clj-surgeon.insert-forms :as insert]
            [clj-surgeon.insert-forms-support :as h]
            [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]))

;; @spec INSERT-FORMS-024
;; INTENT-TEST: INSERT-FORMS-024
(deftest insert-forms-planned-receipt-recovery
  (h/with-file h/source
    (fn [_ file req]
      ;; Model the crash window: publication completes but the durable planned
      ;; receipt never receives the outcome. No production file is edited.
      (with-redefs [insert/finalize-detail! (fn [_ result] result)]
        (let [result (insert/execute! req)
              detail (edn/read-string (slurp (:receipt_details_path result)))
              recover (ns-resolve 'clj-surgeon.insert-forms 'recovery-status)]
          (is (= "planned" (get-in detail [:receipt :state])))
          (is (= "(defn a [] 1)\n(defn b [] 3)\n(defn ab [] 2)\n" (slurp file)))
          (is (some? recover) "Recovery reader must exist.")
          (when recover
            (is (= :published (recover detail (h/sha (slurp file)))))
            (is (= :not-published (recover detail (h/sha h/source))))
            (is (= :target-changed (recover detail (h/sha "external"))))))))))
