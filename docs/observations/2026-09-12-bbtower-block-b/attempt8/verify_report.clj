(require '[clojure.edn :as edn]
         '[clojure.string :as str]
         '[clj-surgeon.lane-manifest :as lm])

(let [root "docs/observations/2026-09-12-bbtower-block-b/attempt8/"
      read-data #(edn/read-string (slurp (str root %)))
      report (read-data "report.edn")
      inputs (read-data "report-input.edn")
      sum (fn [runs p] (reduce + (map :elapsed-ms (filter p runs))))
      fast (read-data "test-fast/receipt.edn")
      step2-runtimes (assoc lm/namespace-runtimes 'clj-surgeon.intent-transaction-test :bb)
      bb-sum (sum (:runs fast) #(= :bb (get step2-runtimes (:namespace %))))
      fast-sum (sum (:runs fast) #(= :fast (lm/lane-of (:namespace %))))
      ceiling (long (Math/ceil (/ (* (double bb-sum) 60000) fast-sum)))]
  (assert (= 245773 bb-sum))
  (assert (= 36944 fast-sum))
  (assert (= 399155 ceiling (get-in report [:measurements :bb-ceiling-ms])))
  (doseq [row (:prewarm inputs)]
    (let [r (read-data (:receipt row))
          mcp (first (filter #(= "mcp" (:suite %)) (:suites r)))
          fs (sum (:runs mcp) #(= :fast (lm/lane-of (:namespace %))))
          integration (sum (:runs mcp) #(= :integration (lm/lane-of (:namespace %))))]
      (assert (= :passed (:state r)))
      (assert (false? (:landing? r)))
      (assert (= (:final-code-head report) (:git-head r)))
      (assert (= (:wall-ms row) (:wall-ms r)))
      (assert (= fs (:fast-sum-ms row)))
      (assert (= integration (:integration-sum-ms row)))
      (assert (< fs 60000))
      (assert (< integration 240000))
      (assert (every? #(and (= :passed (:state %))
                         (zero? (+ (get-in % [:result :fail]) (get-in % [:result :error]))))
                      (:suites r)))))
  (let [jvm (read-data "feature-jvm.edn") bb (read-data "feature-bb.edn")
        m (get lm/runtime-measurements 'clj-surgeon.mcp-feature-thread-test)]
    (assert (= (:result jvm) (:result bb)))
    (assert (= (:elapsed-ms jvm) (:jvm-ms m)))
    (assert (= (:elapsed-ms bb) (:bb-ms m)))
    (assert (= :jvm (get lm/namespace-runtimes 'clj-surgeon.mcp-feature-thread-test))))
  (doseq [prefix ["" "cli-clean/"]]
    (let [bb (slurp (str root prefix "cli-bb.stdout"))
          jvm (slurp (str root prefix "cli-jvm.stdout"))
          normalized (fn [s] (str/replace s (:receipt-file (edn/read-string s)) "RECEIPT-PATH"))]
      (assert (= (count bb) (count jvm)))
      (assert (= (normalized bb) (normalized jvm)))
      (assert (not= bb jvm))))
  (let [gate (slurp (str root "gate.md"))]
    (doseq [{:keys [log line]} (get-in report [:measurements :gate-lines])]
      (assert (str/includes? (slurp log) line))
      (assert (str/includes? gate line))))
  (prn {:report-verified true :prewarm-attempts 2 :final-code-head (:final-code-head report)
        :step2-fast-ms fast-sum :step2-bb-ms bb-sum :new-bb-ceiling-ms ceiling}))
