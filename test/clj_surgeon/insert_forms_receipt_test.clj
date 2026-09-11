(ns clj-surgeon.insert-forms-receipt-test
  {:lane :fast}
  (:require [clojure.test :refer [deftest is testing]]
            [clj-surgeon.insert-forms]
            [clj-surgeon.insert-forms-support]
            [clj-surgeon.mcp-inspect-tool]
            [clojure.edn]
            [clojure.java.io]
            [clojure.string]
))



;; @spec INSERT-FORMS-004
;; INTENT-TEST: INSERT-FORMS-004


;; @spec INSERT-FORMS-012
;; INTENT-TEST: INSERT-FORMS-012

(deftest insert-forms-snapshot-guard
  (testing "insert-forms-stale-hash-refuses"

  (let [s (str clj-surgeon.insert-forms-support/source "; changed\n")
        result (clj-surgeon.insert-forms-support/refused s (clj-surgeon.insert-forms-support/request clj-surgeon.insert-forms-support/source) :source-hash-mismatch)]
    (is (= "refresh-source" (:next_action result)))
    (is (= (clj-surgeon.insert-forms-support/sha clj-surgeon.insert-forms-support/source) (:expected result)))
    (is (= (clj-surgeon.insert-forms-support/sha s) (:actual result)))))
  (testing "insert-forms-portable-read-receipt"
  (clj-surgeon.insert-forms-support/with-file clj-surgeon.insert-forms-support/source
    (fn [dir _ req]
      (let [read-result (clj-surgeon.mcp-inspect-tool/execute-inspect! {:project-root (str dir)}
                          {:requests [{:id "r" :operation "outline" :file (:file req)}]
                           :expect {:requests 1 :files 1}})
            projection (get-in read-result [:read_receipts (:file req)])]
        (is (= {:version 1 :read_complete true :workspace_root (:workspace_root req)
                :file (:file req) :sha256 (clj-surgeon.insert-forms-support/sha clj-surgeon.insert-forms-support/source)} projection))
        (is (= "committed" (:state (clj-surgeon.insert-forms/execute! (assoc req :guard {:read_receipt projection}))))))))

  (let [projection {:version 1 :read_complete true :workspace_root "/fixture"
                    :file "src/example.clj" :sha256 (clj-surgeon.insert-forms-support/sha clj-surgeon.insert-forms-support/source)}
        req (assoc (clj-surgeon.insert-forms-support/request clj-surgeon.insert-forms-support/source) :guard {:read_receipt projection})]
    (clj-surgeon.insert-forms-support/accepted clj-surgeon.insert-forms-support/source req "(defn a [] 1)\n(defn b [] 3)\n(defn ab [] 2)\n")
    (doseq [p [(assoc projection :workspace_root "/wrong")
               (assoc projection :file "other.clj")
               (assoc projection :read_complete false) (dissoc projection :sha256)]]
      (clj-surgeon.insert-forms-support/refused clj-surgeon.insert-forms-support/source (assoc req :guard {:read_receipt p}) :invalid-guard))
    (clj-surgeon.insert-forms-support/refused clj-surgeon.insert-forms-support/source (assoc req :guard {:read_receipt projection :sha256 (clj-surgeon.insert-forms-support/sha clj-surgeon.insert-forms-support/source)})
               :invalid-guard)))
)



;; @spec INSERT-FORMS-014
;; INTENT-TEST: INSERT-FORMS-014


;; @spec INSERT-FORMS-024
;; INTENT-TEST: INSERT-FORMS-024


;; @spec INSERT-FORMS-016
;; INTENT-TEST: INSERT-FORMS-016


;; @spec INSERT-FORMS-017
;; INTENT-TEST: INSERT-FORMS-017

(deftest insert-forms-publication-evidence
  (testing "insert-forms-atomic-multiform-and-race"

  (doseq [[stage state kind unchanged]
          [[:stage "failed" :io-error true]
           [:before-recheck "refused" :source-changed-before-commit false]
           [:read-back "rolled-back" :io-error true]
           [:external-after-write "recovery-required" :commit-outcome-unknown false]]]
    (clj-surgeon.insert-forms-support/with-file clj-surgeon.insert-forms-support/source
      (fn [_ file req]
        (let [req (assoc req :payload {:text "(def b 2)\n(def c 3)" :forms 2})
              external (str clj-surgeon.insert-forms-support/source "; external\n")
              hook (fn [& _]
                     (when (#{:before-recheck :external-after-write} stage) (spit file external))
                     (when-not (= :before-recheck stage) (throw (java.io.IOException. "injected"))))
              result (clj-surgeon.insert-forms/execute! req {stage hook})]
          (is (= state (:state result)) (pr-str result))
          (is (= kind (:error-type result)))
          (let [detail (clojure.edn/read-string (slurp (:receipt_details_path result)))]
            (is (= (:state result) (get-in detail [:receipt :state])))
            (is (= (:next_action result) (get-in detail [:receipt :next_action])))
            (is (= (clj-surgeon.insert-forms-support/sha (slurp file)) (:observed_source_hash detail))))
          (is (= (if unchanged clj-surgeon.insert-forms-support/source external) (slurp file))))))))
  (testing "insert-forms-planned-receipt-recovery"
  (clj-surgeon.insert-forms-support/with-file clj-surgeon.insert-forms-support/source
    (fn [_ file req]
      ;; Model the crash window: publication completes but the durable planned
      ;; receipt never receives the outcome. No production file is edited.
      (with-redefs [clj-surgeon.insert-forms/finalize-detail! (fn [_ result] result)]
        (let [result (clj-surgeon.insert-forms/execute! req)
              detail (clojure.edn/read-string (slurp (:receipt_details_path result)))
              recover (ns-resolve 'clj-surgeon.insert-forms 'recovery-status)]
          (is (= "planned" (get-in detail [:receipt :state])))
          (is (= "(defn a [] 1)\n(defn b [] 3)\n(defn ab [] 2)\n" (slurp file)))
          (is (some? recover) "Recovery reader must exist.")
          (when recover
            (is (= :published (recover detail (clj-surgeon.insert-forms-support/sha (slurp file)))))
            (is (= :not-published (recover detail (clj-surgeon.insert-forms-support/sha clj-surgeon.insert-forms-support/source))))
            (is (= :target-changed (recover detail (clj-surgeon.insert-forms-support/sha "external"))))))))))
  (testing "insert-forms-receipt-accounting"
  (clj-surgeon.insert-forms-support/with-file clj-surgeon.insert-forms-support/source
    (fn [dir _ req]
      (let [relative (str (clojure.string/join "/" (repeat 16 (apply str (repeat 125 "x")))) "/example.clj")
            file (clojure.java.io/file dir relative)
            _ (clojure.java.io/make-parents file)
            _ (spit file clj-surgeon.insert-forms-support/source)
            result (clj-surgeon.insert-forms/execute! (assoc req :file relative))]
        (is (= :limit-exceeded (:error-type result)))
        (is (= "refused" (:state result)))
        (is (false? (:mutation_attempted result)))
        (is (= clj-surgeon.insert-forms-support/source (slurp file)))
        (is (<= (count (clj-surgeon.insert-forms/receipt-text result)) 4096)))))

  (clj-surgeon.insert-forms-support/with-file clj-surgeon.insert-forms-support/source
    (fn [_ file req]
      (let [result (clj-surgeon.insert-forms/execute! (assoc req :payload {:text (str "9" (apply str (repeat 20000 "a"))) :forms 1}))]
        (is (= :payload-parse-error (:error-type result)))
        (is (= clj-surgeon.insert-forms-support/source (slurp file)))
        (is (<= (alength (.getBytes (clj-surgeon.insert-forms/receipt-text result) "UTF-8")) 4096)))))
  (clj-surgeon.insert-forms-support/with-file clj-surgeon.insert-forms-support/source
    (fn [_ _ req]
      (let [result (clj-surgeon.insert-forms/execute! (assoc req :payload {:text (apply str (repeat 1000 "1\n")) :forms 1000}))]
        (is (= true (:ok result)))
        (is (<= (alength (.getBytes (clj-surgeon.insert-forms/receipt-text result) "UTF-8")) 4096)))))
  (let [r (clj-surgeon.insert-forms/plan clj-surgeon.insert-forms-support/source (clj-surgeon.insert-forms-support/request clj-surgeon.insert-forms-support/source))
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
  (testing "insert-forms-terminal-response-eligibility"

  (let [r (clj-surgeon.insert-forms/plan clj-surgeon.insert-forms-support/source (clj-surgeon.insert-forms-support/request clj-surgeon.insert-forms-support/source))]
    (is (= true (:ok r)))
    (is (= false (get-in r [:receipt :verification_complete])))
    (is (= {:tier "parse+byte-preservation" :behavior "not-run"} (get-in r [:receipt :verification])))
    (is (not (contains? (:receipt r) :terminal_response))))
  (clj-surgeon.insert-forms-support/with-file clj-surgeon.insert-forms-support/source
    (fn [_ _ req]
      (let [r (clj-surgeon.insert-forms/execute! req)]
        (is (= "committed" (:state r)))
        (is (not (contains? r :terminal_response)))
        (is (= false (:verification_complete r)))))))
)
