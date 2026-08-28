(ns cli-mcp-transport-differential-test
  ;; @spec OP-ALG-PARITY-001, OP-ALG-PERF-001
  (:require
   [babashka.process :as process]
   [cli-mcp-transport-differential :as differential]
   [clj-surgeon.core :as core]
   [clojure.test :refer [deftest is run-tests testing]]))

(deftest direct-cli-stdin-preserves-the-intended-edn
  (doseq [[operation intended]
          [[:cat differential/read-cli-spec]
           [:change differential/change-cli-spec]
           [:cat differential/selector-cli-spec]]]
    (testing (name operation)
      (let [envelope (differential/cli-stdin-envelope operation intended)]
        (is (= intended (:parsed-request envelope)))
        (is (= (:intended-hash envelope)
               (differential/data-hash (:parsed-request envelope))))
        (is (= 64 (count (get-in envelope [:stdin-bytes :sha256]))))
        (is (= 64 (count (get-in envelope [:shell-program-bytes :sha256]))))))))

(deftest quoted-heredoc-survives-where-the-legacy-printf-shape-does-not
  (let [intended differential/change-cli-spec
        expected (str (pr-str intended) "\n")
        legacy-program (differential/legacy-printf-shell-program intended)
        heredoc-program (differential/literal-heredoc-shell-program intended)
        legacy @(process/process ["/bin/zsh" "-c" legacy-program]
                                 {:out :string :err :string})
        heredoc @(process/process ["/bin/zsh" "-c" heredoc-program]
                                  {:out :string :err :string})]
    (is (not (zero? (:exit legacy))))
    (is (not= expected (:out legacy)))
    (is (zero? (:exit heredoc)) (:err heredoc))
    (is (= expected (:out heredoc)))
    (is (= intended (core/parse-spec-document (:out heredoc) "literal heredoc")))))

(deftest batched-read-has-semantic-parity
  (let [result (differential/read-differential)]
    (is (get-in result [:equivalence :correct]))
    (is (get-in result [:equivalence :semantic-facts-equal]))
    (is (= (get-in result [:equivalence :cli-facts-hash])
           (get-in result [:equivalence :mcp-facts-hash])))))

(deftest generic-change-compiles-to-the-identical-kernel-intent
  (let [result (differential/change-differential)]
    (is (get-in result [:equivalence :correct]))
    (is (get-in result [:equivalence :transaction-edn-equal]))
    (is (get-in result [:equivalence :compiled-intent-equal]))
    (is (get-in result [:equivalence :future-hashes-equal]))
    (is (= (get-in result [:cli :compiled-intent-hash])
           (get-in result [:mcp :compiled-intent-hash])))))

(deftest selector-refusal-and-exact-retry-share-owner-facts
  (let [result (differential/selector-differential)]
    (is (get-in result [:equivalence :correct]))
    (is (get-in result [:equivalence :owner-hypothesis-facts-equal]))
    (is (= "editor-gesture-schema"
           (get-in result [:equivalence :selected-owner])))
    (is (false? (get-in result [:equivalence :selection-authority])))
    (is (get-in result [:equivalence :retry-semantic-facts-equal]))
    (is (= (get-in result [:equivalence :cli-facts-hash])
           (get-in result [:equivalence :mcp-facts-hash])))))

(deftest complete-report-is-parity-only-not-a-speed-claim
  (let [report (differential/report)]
    (is (= differential/candidate-commit (:candidate-commit report)))
    (is (= 0 (:model-calls report)))
    (is (= 0 (:analyzer-launches report)))
    (is (= 3 (count (:strata report))))
    (is (:all-correct report))
    (is (:all-semantic-parity report))))

(let [result (run-tests 'cli-mcp-transport-differential-test)]
  (when (pos? (+ (:fail result) (:error result)))
    (System/exit 1)))
