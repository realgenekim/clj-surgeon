#!/usr/bin/env bb
(ns diff-impact-executor-parity
  (:require
   [clj-surgeon.battery-parallel-runner :as bp]
   [clojure.edn :as edn]
   [clojure.string :as str]))

;; @spec DIFF-IMPACT-007 -- run after a full-fast/selected-fast fixture pair.
(let [[full-file results-file] *command-line-args*
      _ (when-not (and full-file results-file)
          (throw (ex-info "Usage: bb test/diff_impact_executor_parity.clj FULL_RECEIPT SCOPED_RESULTS" {})))
      full (edn/read-string (slurp full-file))
      observations (map edn/read-string (remove str/blank? (str/split-lines (slurp results-file))))
      fast (first (filter #(= :fast (:scope %)) observations))
      selected (:selected fast)
      verdict (fn [r] (let [{:keys [test fail error]} (:counters r)]
                        (and (pos-int? test) (= 0 fail error) (empty? (:violations r)))))
      outcomes (fn [runs] (into {} (map (juxt :namespace verdict) runs)))
      delta (bp/parity-delta (select-keys (outcomes (:runs full)) selected)
              (outcomes (:runs fast)))]
  (assert (= "fast" (:suite full)))
  (assert (= :passed (:state full)))
  (assert (not (:partial full)))
  (assert (seq selected))
  (assert (string? (:source-digest full)))
  (assert (every? #(= (:source-digest full) (:source-digest %)) (:receipts fast)))
  (assert (empty? (bp/census-problems selected (map :namespace (:runs fast)))))
  (assert (every? #(and (:partial %) (= (:selection-sha fast) (:selection-sha %))) (:receipts fast)))
  (assert (empty? delta) (pr-str delta))
  (assert (zero? (:exit fast)))
  (assert (seq (:receipts fast)))
  (assert (every? #(and (= :passed (:state %)) (empty? (:problems %))) (:receipts fast)))
  (prn {:parity :passed :selected selected :outcomes (outcomes (:runs fast))
        :full-wall-ms (:wall-ms full) :selected-wall-ms (:wall-ms fast)
        :selected-total-wall-ms (:total-wall-ms fast) :delta delta}))
