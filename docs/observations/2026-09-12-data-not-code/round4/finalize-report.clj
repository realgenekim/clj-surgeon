(require '[clojure.edn :as edn]
         '[clojure.pprint :as pprint]
         '[cheshire.core :as json]
         '[clj-surgeon.battery-parallel-runner :as runner])

(let [dir "docs/observations/2026-09-12-data-not-code/round4/"
      gate (edn/read-string (slurp (str dir "prewarm-final-receipt.edn")))
      command (json/parse-string (slurp (str dir "prewarm-final-command.json")) true)
      report (edn/read-string (slurp (str dir "report.edn")))
      digest (runner/source-digest)
      final (-> report
                (assoc :verdict :pass
                       :recorded-at (str (java.time.Instant/now))
                       :verified-source-digest digest
                       :release-approved false)
                (assoc-in [:measurements :prewarm-final]
                          {:exit (:exit command)
                           :state (:state gate)
                           :prewarm? (:prewarm? gate)
                           :landing? (:landing? gate)
                           :gate-wall-ms (:wall-ms gate)
                           :command-wall-ms (:wall_ms command)
                           :source-digest digest
                           :problems (:problems gate)
                           :stages (:stages gate)
                           :suites (mapv #(select-keys % [:suite :state :result :measurements :problems])
                                         (:suites gate))
                           :receipt "prewarm-final-receipt.edn"}))]
  (assert (= :passed (:state gate)))
  (assert (zero? (:exit command)))
  (assert (empty? (:problems gate)))
  (assert (= digest (:source-digest gate)))
  (assert (every? #(zero? (:exit %)) (:stages gate)))
  (spit (str dir "report.edn") (with-out-str (pprint/pprint final)))
  (prn {:verdict (:verdict final) :source-digest digest
        :gate-wall-ms (:wall-ms gate) :stages (count (:stages gate))}))
