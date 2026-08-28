(ns operation-algebra-commit-parity-test
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is run-tests testing]]
   [operation-algebra-commit-parity :as parity]))

(deftest fake-terminal-results-are-deterministic-and-effect-free
  (let [compiled {:original-sources {"app.clj" parity/original-source}
                  :future-sources {"app.clj" parity/future-source}
                  :changed-file-count 1}
        calls (atom {:fake-commit 0})]
    (testing "success"
      (is (:ok (parity/fake-commit :success calls compiled))))
    (testing "stale source"
      (is (= :source-hash-mismatch
             (:error-type
               (parity/fake-commit :stale-source calls compiled)))))
    (testing "restored write failure"
      (let [result (parity/fake-commit :write-failure calls compiled)]
        (is (= :transaction-write-failed (:error-type result)))
        (is (true? (:rolled-back result)))))
    (is (= 3 (:fake-commit @calls)))))

(deftest retained-cutover-witness-is-exact-and-shadow-safe
  ;; @spec OP-ALG-PARITY-002, OP-ALG-RECEIPT-001, OP-ALG-RECEIPT-002,
  ;; OP-ALG-SHADOW-001
  (let [result (parity/report (System/getProperty "user.dir")
                              (.getCanonicalPath
                                (io/file
                                  "dev/experiments/operation_algebra_commit_parity.clj")))]
    (is (= parity/candidate-commit (:candidate-commit result)))
    (is (= parity/pre-cutover-commit (:pre-cutover-commit result)))
    (is (= (set parity/scenarios) (set (keys (:parity result)))))
    (is (:all-case-gates-pass result))
    (is (:exact-receipt-source result))
    (is (:pre-accepts-candidate-receipt result))
    (is (:candidate-accepts-pre-receipt result))
    (is (:cross-version-inverse-equal result))
    (is (:candidate-live-projection-exact result))
    (is (:candidate-live-source-exact result))
    (is (:shadow-safe result))
    (is (= 1 (:authoritative-live-commits result)))
    (is (= 1 (:live-receipts result)))
    (is (zero? (:model-calls result)))
    (is (zero? (:analyzer-launches result)))
    (is (:all-correct result))))

(let [result (run-tests 'operation-algebra-commit-parity-test)]
  (when (pos? (+ (:fail result) (:error result)))
    (System/exit 1)))
