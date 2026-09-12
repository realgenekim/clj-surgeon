(require '[clojure.test :as t])

(let [[runtime ns-name output] *command-line-args*
      n (symbol ns-name)
      _ (require n)
      started (System/nanoTime)
      result (t/run-tests n)
      wall (long (/ (- (System/nanoTime) started) 1000000))
      row {:runtime (keyword runtime) :namespace n :elapsed-ms wall :result result}]
  (spit output (pr-str row))
  (prn row)
  (shutdown-agents)
  (System/exit (if (zero? (+ (:fail result) (:error result))) 0 1)))
