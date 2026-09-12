(require '[clojure.edn :as edn]
         '[clj-surgeon.lane-manifest :as lm])

(let [path "docs/observations/2026-09-12-bbtower-block-b/attempt10/ceiling-run/receipt.edn"
      receipt (edn/read-string (slurp path))
      inputs (edn/read-string (slurp "docs/observations/2026-09-12-bbtower-block-b/attempt10/ceiling-inputs.edn"))
      runs (:runs receipt)
      bb (reduce + (map :elapsed-ms (filter #(= :bb (lm/namespace-runtimes (:namespace %))) runs)))
      fast (reduce + (map :elapsed-ms (filter #(= :fast (get (:cadence-map inputs) (:namespace %))) runs)))
      ceiling (quot (+ (* bb 60000) (dec fast)) fast)]
  (assert (= :passed (:state receipt)))
  ;; The repair reassigns outline-corpus (integration), absent from this run.
  ;; Every actually measured member still agrees with the shipped runtime map.
  (assert (= (select-keys lm/namespace-runtimes (map :namespace runs))
             (select-keys (:runtime-map inputs) (map :namespace runs))))
  (assert (= [bb fast ceiling] ((juxt :bb-ms :fast-ms :ceiling-ms) inputs)))
  (println "receipt:" path)
  (println "makespan-ms:" (:wall-ms receipt))
  (println "bb-runtime: serial-equivalent" bb "ms")
  (println "fast-cadence: serial-equivalent" fast "ms")
  (println "ceiling = ceil(" bb "* 60000 /" fast ") =" ceiling "ms")
  (doseq [r (sort-by (comp - :elapsed-ms) runs)
          :when (nil? (get (:cadence-map inputs) (:namespace r)))]
    (prn (select-keys r [:namespace :elapsed-ms])))
  (prn {:reproduced true :measured-members-runtime-map-matches-shipped true}))
