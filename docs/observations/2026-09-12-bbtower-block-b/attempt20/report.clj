(require '[clojure.edn :as edn]
         '[clojure.pprint :as pp])

(let [root "docs/observations/2026-09-12-bbtower-block-b/attempt20/"
      read-e #(edn/read-string (slurp (str root %)))
      table (read-e "runtime-table.edn")
      gate-summary (fn [path]
                     (let [r (read-e path)]
                       (assoc (select-keys r [:state :wall-ms :stages :problems :run-id
                                              :git-head :source-digest :prewarm? :landing?])
                              :receipt path
                              :suites (mapv #(select-keys % [:suite :state :result :measurements
                                                             :leak-failures]) (:suites r)))))
      report {:verdict :pass
              :findings {:f1 :six-run-conservative-runtime-rule
                         :f2 :complete-bounded-probe-edn
                         :spec-receipt :executed-key-set-witness}
              :measurements {:namespaces (count table)
                             :samples (reduce + (for [[_ row] table runtime [:jvm :bb]]
                                                  (get-in row [runtime :n])))
                             :table "runtime-table.edn"
                             :processes "measurement-processes.jsonl"
                             :reassignments (into (sorted-map)
                                                  (filter (fn [[_ r]] (not= (:runtime r) (:previous-runtime r))))
                                                  table)}
              :fast (assoc (select-keys (read-e "fast-run/receipt.edn")
                                        [:state :result :measurements :leak-failures :problems])
                           :receipt "fast-run/receipt.edn" :exit (read-e "fast.exit"))
              :restricted-diagnostic (gate-summary "restricted-receipt.edn")
              :real-prewarm (gate-summary "prewarm-receipt.edn")
              :gate-log "gate.md"
              :commits ["8a742959553da31925354823d2a467c19ef0e967"
                        "ae7b76bcc2463054f5c6a3f020a9e43cbc9ff8b6"
                        "09588a14a784e220907462f46676900402bc80aa"]
              :least_sure ["Six controls describe this box and snapshot; future drift needs new controls."
                           "The bounded projection assumes the production verdict's closed scalar fields."]
              :disagreements []
              :owed ["Independent fence review and any landing authorization remain outside this build."
                     "Prior intent-transaction contract escape is retained; successful controls do not retire it."]}]
  (spit (str root "report.edn") (with-out-str (pp/pprint report))))
