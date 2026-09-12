(require '[clj-surgeon.battery-parallel-runner :as runner]
         '[clj-surgeon.lane-manifest :as lm])

(let [[arm output] *command-line-args*
      original-suite runner/suite-namespaces
      runtime-var (ns-resolve 'clj-surgeon.lane-manifest 'namespace-runtimes)
      prepare (fn [] (runner/prepare-suite! {"--suite" "fast" "--work-dir" output}))
      context (with-redefs [runner/suite-namespaces
                            (if (= arm "b")
                              (fn [_] (vec (lm/namespaces-for :fast)))
                              original-suite)]
                (if (= arm "b")
                  (with-redefs-fn {runtime-var (zipmap (lm/namespaces-for :fast) (repeat :jvm))} prepare)
                  (prepare)))
      plan (mapv (fn [job]
                   (cond-> (assoc job :java-opts "-J-Xms64m -J-Xmx1g")
                     (= arm "b") (assoc :runtime :jvm)))
                 (:plan context))
      _ (spit (str output "/measurement-plan.edn") (pr-str plan))
      execution (runner/execute-plan! plan (:lanes-n context))
      receipt (runner/finish-suite! context (:lanes execution) (:wall-ms execution))]
  (spit (str output "/execution.edn") (pr-str (dissoc execution :lanes)))
  (println "MEASUREMENT" arm (:state receipt) (:wall-ms receipt))
  (shutdown-agents)
  (System/exit (if (= :passed (:state receipt)) 0 1)))
