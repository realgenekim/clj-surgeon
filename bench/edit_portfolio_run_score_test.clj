#!/usr/bin/env bb

(ns edit-portfolio-run-score-test
  (:require
   [clojure.test :refer [deftest is run-tests testing]]
   [edit-portfolio-run-score :as score]))

(def valid-compact-route
  {:mcp-calls 1
   :inspect-calls 0
   :edit-calls 1
   :extraction-calls 0
   :plan-calls 0
   :transform-calls 0
   :file-changes 0
   :shell-calls 0
   :mcp-successes 1
   :mcp-failures 0
   :failed-mutation-actions 0
   :post-decision-source-commands 0
   :verified true
   :single-change-transaction true})

(deftest compact-route-contract
  (is (score/compact-route-adherent? valid-compact-route))
  (doseq [[field invalid]
          [[:mcp-calls 2]
           [:inspect-calls 1]
           [:edit-calls 0]
           [:extraction-calls 1]
           [:plan-calls 1]
           [:transform-calls 1]
           [:file-changes 1]
           [:shell-calls 1]
           [:mcp-successes 0]
           [:mcp-failures 1]
           [:failed-mutation-actions 1]
           [:post-decision-source-commands 1]
           [:verified false]
           [:single-change-transaction false]]]
    (testing (name field)
      (is (false? (score/compact-route-adherent?
                    (assoc valid-compact-route field invalid)))))))

(deftest source-inventory-contract
  (is (score/source-path? "src/a.clj"))
  (is (false? (score/source-path? ".clj-surgeon-receipts/run.edn")))
  (is (= {:source-set-exact true
          :expected ["src/a.clj" "src/config.edn"]
          :actual ["src/a.clj" "src/config.edn"]
          :unexpected []
          :missing []}
         (score/compare-source-paths
           ["src/a.clj" "src/config.edn"]
           ["src/config.edn" "src/a.clj"])))
  (is (= {:source-set-exact false
          :expected ["src/a.clj"]
          :actual ["src/a.clj" "src/stray.clj"]
          :unexpected ["src/stray.clj"]
          :missing []}
         (score/compare-source-paths
           ["src/a.clj"]
           ["src/a.clj" "src/stray.clj"]))))

(deftest score-layers-remain-independent
  (is (= {:semantic-correct true
          :exact-correct true
          :route-adherent false
          :source-set-exact true
          :correct false}
         (score/finalize-outcome
           {:target-semantic-correct true
            :target-exact-correct true
            :route-adherent false
            :source-set-exact true
            :treatment-adherent true}))
      "route failure must not erase source correctness evidence")
  (is (= {:semantic-correct false
          :exact-correct false
          :route-adherent true
          :source-set-exact false
          :correct false}
         (score/finalize-outcome
           {:target-semantic-correct true
            :target-exact-correct true
            :route-adherent true
            :source-set-exact false
            :treatment-adherent true}))
      "an unexpected source file fails the result without falsifying route evidence")
  (is (= {:semantic-correct true
          :exact-correct false
          :route-adherent true
          :source-set-exact true
          :correct true}
         (score/finalize-outcome
           {:target-semantic-correct true
            :target-exact-correct false
            :route-adherent true
            :source-set-exact true
            :treatment-adherent true}))
      "presentation-only drift remains correct"))

(defn -main
  [& _]
  (let [{:keys [fail error]} (run-tests 'edit-portfolio-run-score-test)]
    (when (pos? (+ fail error))
      (System/exit 1))))

(apply -main *command-line-args*)
