(ns clj-surgeon.rename-alias-receipt-test
  {:lane :fast}
  (:require [clojure.test :refer [deftest is testing]]
            [clj-surgeon.insert-forms-oracle]
            [clj-surgeon.insert-forms-plan]
            [clj-surgeon.insert-forms-support]
            [clj-surgeon.intent-transaction]
            [clj-surgeon.rename-alias]
            [clj-surgeon.rename-alias-oracle]
            [clj-surgeon.rename-alias-plan]
            [clj-surgeon.rename-alias-test]
            [clj-surgeon.txn-journal]
            [clojure.edn]
            [clojure.java.io]
            [clojure.string]
            [clojure.walk]
))


(def source (str clj-surgeon.rename-alias-test/header "(def y events/x)\n(def untouched 1)\n#_ignored\n"))

(defn disk-fault [change]
  (clj-surgeon.insert-forms-support/with-file source
    (fn [dir target _]
      (let [req (assoc (clj-surgeon.rename-alias-test/request {clj-surgeon.rename-alias-test/file source} 1) :workspace_root (.getCanonicalPath dir))
            commit clj-surgeon.intent-transaction/commit-compiled!
            result (with-redefs [clj-surgeon.intent-transaction/commit-compiled!
                                 (fn [compiled io-fns]
                                   (commit compiled
                                           (update io-fns :write-source!
                                                   (fn [write]
                                                     (fn [f text]
                                                       (write f text)
                                                       (when (= text (get-in compiled [:future-sources f]))
                                                         (spit (clojure.java.io/file dir f) (change text))))))))]
                     (clj-surgeon.rename-alias/execute! req))
            disk (slurp target)
            detail (when (:receipt_details_path result) (clojure.edn/read-string (slurp (:receipt_details_path result))))]
        {:receipt result :detail detail :disk disk}))))


;; @spec RENAME-ALIAS-009
;; INTENT-TEST: RENAME-ALIAS-009


;; @spec RENAME-ALIAS-012
;; INTENT-TEST: RENAME-ALIAS-012


;; @spec RENAME-ALIAS-013
;; INTENT-TEST: RENAME-ALIAS-013


;; @spec RENAME-ALIAS-013
;; INTENT-TEST: RENAME-ALIAS-013


;; @spec RENAME-ALIAS-013
;; INTENT-TEST: RENAME-ALIAS-013

(deftest rename-alias-publication-evidence
  (testing "rename-alias-transaction-faults"
  (doseq [[stage wanted] [[:stage "failed"] [:before-recheck "refused"] [:read-back "rolled-back"]
                          [:second-replacement "rolled-back"] [:external-after-write "recovery-required"]
                          [:restore "recovery-required"] [:receipt-finalize "recovery-required"]]]
    (clj-surgeon.insert-forms-support/with-file (str clj-surgeon.rename-alias-test/header "events/x")
      (fn [dir target _]
        (let [second-file (clojure.java.io/file dir "src/b.clj")
              source (slurp target) _ (spit second-file source)
              req (assoc (clj-surgeon.rename-alias-test/request {clj-surgeon.rename-alias-test/file source "src/b.clj" source} 2) :workspace_root (.getCanonicalPath dir))
              fault (fn []
                      (when (#{:before-recheck :external-after-write} stage) (spit target "external\n"))
                      (when-not (= stage :before-recheck) (throw (java.io.IOException. "injected"))))
              hooks (cond-> {stage fault} (= stage :restore) (assoc :read-back #(throw (java.io.IOException. "read"))))
              r (clj-surgeon.rename-alias/execute! req hooks)]
          (is (= wanted (:state r)) (pr-str r))
          (is (not (true? (:committed r))))
          (when-let [path (:receipt_details_path r)]
            (let [text (slurp path) detail (clojure.edn/read-string text)]
              (is (= (:receipt_hash r) (clj-surgeon.insert-forms-support/sha text)))
              (is (= #{clj-surgeon.rename-alias-test/file "src/b.clj"} (set (keys (get-in r [:transaction :file_states])))))
              (is (map? (clj-surgeon.rename-alias/recovery-status detail {clj-surgeon.rename-alias-test/file (clj-surgeon.insert-forms-support/sha (slurp target)) "src/b.clj" (clj-surgeon.insert-forms-support/sha (slurp second-file))})))))
          (when (#{"failed" "rolled-back"} wanted)
            (is (= source (slurp target) (slurp second-file))))
          (when (= stage :before-recheck)
            (is (= :source-changed-before-commit (:error-type r)))
            (is (false? (:mutation_attempted r))))
          (when (= stage :external-after-write) (is (= "external\n" (slurp target)))))))))
  (testing "replacement-read-back-refuses"
  (clj-surgeon.insert-forms-support/with-file clj-surgeon.rename-alias-test/candidate-source
    (fn [dir file _]
      (let [req (assoc (clj-surgeon.rename-alias-test/request {clj-surgeon.rename-alias-test/file clj-surgeon.rename-alias-test/candidate-source} 1) :workspace_root (.getCanonicalPath dir))
            armed (atom false) sha clj-surgeon.txn-journal/sha256-file
            result (with-redefs [clj-surgeon.txn-journal/sha256-file
                                 (fn [path] (if (compare-and-set! armed true false)
                                              (clj-surgeon.insert-forms-support/sha "faulty read") (sha path)))]
                     (clj-surgeon.rename-alias/execute! req {:read-back #(reset! armed true)}))]
        (is (= :io-error (:error-type result)) (pr-str result))
        (is (= "rolled-back" (:state result)))
        (is (= clj-surgeon.rename-alias-test/candidate-source (slurp file)))))))
  (testing "receipt-observes-disk-neighbor-corruption"
  (let [{:keys [receipt detail disk]} (disk-fault #(clojure.string/replace % "untouched 1" "untouched 2"))
        per-file (first (:per_file detail))]
    (is (= :commit-outcome-unknown (:error-type receipt)))
    (is (false? (:other_forms_unchanged receipt)))
    (is (false? (get-in per-file [:preservation :other_forms_unchanged])))
    (is (false? (:write_verified receipt)))
    (is (= (clj-surgeon.insert-forms-support/sha disk) (get-in receipt [:read_back_hashes clj-surgeon.rename-alias-test/file])))
    (is (= (clj-surgeon.insert-forms-support/sha disk) (:read_back_hash per-file)))
    (is (not= (:result_hash per-file) (:read_back_hash per-file)))))
  (testing "receipt-observes-disk-trivia-and-discard-corruption"
  (doseq [[change field] [[#(str % " ") :gaps_unchanged]
                          [#(clojure.string/replace % "#_ignored" "#_changed") :discards_unchanged]]]
    (let [{:keys [receipt detail]} (disk-fault change)]
      (is (= :commit-outcome-unknown (:error-type receipt)))
      (is (false? (get-in detail [:per_file 0 :preservation field]))))))
  (testing "receipt-details-contains-observes-artifact"
  (clj-surgeon.insert-forms-support/with-file source
    (fn [dir _ _]
      (let [write clj-surgeon.rename-alias/write-detail!
            req (assoc (clj-surgeon.rename-alias-test/request {clj-surgeon.rename-alias-test/file source} 1) :workspace_root (.getCanonicalPath dir))
            result (with-redefs [clj-surgeon.rename-alias/write-detail!
                                 (fn [path detail]
                                   (write path (clojure.walk/postwalk #(if (map? %) (dissoc (into {} %) :sites) %) detail)))]
                     (clj-surgeon.rename-alias/execute! req))
            detail (when (:receipt_details_path result) (clojure.edn/read-string (slurp (:receipt_details_path result))))]
        (is (= "committed" (:state result)) (pr-str result))
        (is (not (contains? detail :sites)))
        (is (false? (boolean (some #{"sites"} (:details_contains result)))))
        (is (some #{"per_file"} (:details_contains result)))))))
)
