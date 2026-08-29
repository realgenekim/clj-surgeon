(ns owner-aware-call-construction-prereq-test
  (:require
   [clojure.test :refer [deftest is run-tests]]
   [owner-aware-call-construction-prereq :as prereq]))

(deftest current-product-closes-every-retained-prerequisite
  (let [result (prereq/prerequisite-report)]
    (is (:all-prerequisites-green result))
    (is (= {:runs 8 :exact 8} (get-in result [:relations :canonical])))
    (is (= {:runs 8 :exact 8} (get-in result [:relations :old-new])))
    (is (= {:runs 8 :exact 8}
           (get-in result [:relations :before-after])))
    (is (= 12 (get-in result [:candidate-migration :runs])))
    (is (get-in result [:candidate-migration :all-owner-rows-preserved]))
    (is (= 0 (:model-calls result)))
    (is (= 0 (:mutation-actions result)))
    (is (every? true? (vals (:falsifiers result))))))

(deftest current-surfaces-and-payloads-are-measured
  (let [result (prereq/prerequisite-report)]
    (is (pos? (get-in result [:surface :control :surface-bytes])))
    (is (> (get-in result [:surface :candidate :surface-bytes])
           (get-in result [:surface :control :surface-bytes])))
    (is (< (get-in result [:emitted-arguments :candidate-bytes])
           (get-in result [:emitted-arguments :control-bytes])))
    (is (<= (get-in result [:emitted-arguments :candidate-bytes])
            (get-in result [:emitted-arguments :candidate-budget])))))

(deftest pilot-gate-requires-one-correct-run-per-arm
  (let [run (fn [arm correct]
              {:arm arm
               :correct correct
               :geometry {:prompt-to-call-ms 1000.0}
               :payload {:bytes 100}})]
    (is (= {:one-per-arm true :both-correct true}
           (:gate (prereq/pilot-report
                    [(run :control true) (run :candidate true)]))))
    (is (false? (get-in (prereq/pilot-report
                          [(run :control true) (run :candidate false)])
                        [:gate :both-correct])))))

(defn -main [& _]
  (let [{:keys [fail error]} (run-tests)]
    (when (pos? (+ fail error))
      (System/exit 1))))
