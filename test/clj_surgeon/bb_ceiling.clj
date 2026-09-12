(ns clj-surgeon.bb-ceiling
  "Reproduce TEST-ISO-007 from recorded runtimes and unchanged lane cadence."
  (:refer-clojure :exclude [derive])
  (:require
   [clj-surgeon.lane-manifest :as lm]
   [clojure.edn :as edn]))

(def calibration-receipt
  "docs/observations/2026-09-12-bbtower-block-b/attempt10/ceiling-run/receipt.edn")

;; @spec TEST-ISO-007
(defn derive [receipt]
  (assert (= :passed (:state receipt)) "Calibration receipt must have passed")
  (let [runs (:runs receipt)
        runtime-entries (vec (for [lane (:lanes receipt) n (:namespaces lane)]
                               [n (:runtime lane)]))
        runtimes (into {} runtime-entries)
        _ (assert (and (= (count runtime-entries) (count runtimes))
                       (= (set (keys runtimes)) (set (map :namespace runs)))
                       (every? #{:bb :jvm} (vals runtimes)))
                  "Calibration requires one recorded execution runtime per namespace")
        _ (assert (and (seq runs)
                       (= (count runs) (count (set (map :namespace runs))))
                       (every? #(and (lm/lane-of (:namespace %))
                                     (contains? lm/namespace-runtimes (:namespace %))
                                     (integer? (:elapsed-ms %))
                                     (<= 0 (:elapsed-ms %))) runs))
                  "Calibration requires unique, measured, shipped-classified namespaces")
        bb (reduce + 0 (map :elapsed-ms (filter #(= :bb (runtimes (:namespace %))) runs)))
        fast (reduce + 0 (map :elapsed-ms (filter #(= :fast (lm/lane-of (:namespace %))) runs)))]
    (assert (pos? fast) "Calibration fast-cadence sum must be positive")
    {:bb-runtime-sum-ms bb
     :fast-cadence-sum-ms fast
     :ceiling-ms (quot (+ (* bb 60000) (dec fast)) fast)}))

(defn from-file [path]
  (derive (edn/read-string (slurp path))))

(when (= *file* (System/getProperty "babashka.file"))
  (assert (= 1 (count *command-line-args*))
          "Usage: bb test/clj_surgeon/bb_ceiling.clj <receipt.edn>")
  (prn (from-file (first *command-line-args*))))
