;; Read-only diagnostic of retained Block A facts, not a fresh test execution.
(require '[clojure.edn :as edn]
         '[clj-surgeon.battery-parallel-runner :as runner]
         '[clj-surgeon.lane-manifest :as lm]
         '[clj-surgeon.ns-isolation :as iso])
(let [runs (edn/read-string
             (slurp "docs/observations/2026-09-12-bbtower-block-b/retained-walls.edn"))
      fast (filter #(= :fast (lm/lane-of (:namespace %))) runs)
      registry (edn/read-string (slurp "docs/intent/receipt-booleans/registry.edn"))]
  (println "RETAINED-FACT REPLAY; no test namespace executed")
  (prn :lane-budgets iso/lane-budget-ms)
  (prn :fast-members (count fast) :fast-sum-ms (reduce + (map :elapsed-ms fast)))
  (prn :fast-only-violation-count (runner/report! (vec fast) [] 83233))
  (prn :probe-boolean-registration (get-in registry [:verbs "probe"]))
  (prn :registered-boolean-verbs (vec (sort (keys (:verbs registry))))))
