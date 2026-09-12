(require '[clojure.edn :as edn]
         '[clojure.pprint :as pprint]
         '[clj-surgeon.battery-parallel-runner :as runner])

(let [dir "docs/observations/2026-09-12-bbtower-block-b/attempt21/"
      read-receipt #(edn/read-string (slurp (str dir % "-receipt.edn")))
      summarize (fn [receipt]
                  (assoc (select-keys receipt [:state :git-head :source-digest :wall-ms
                                                :started-at :completed-at :problems :stages])
                         :suites (mapv #(select-keys % [:suite :state :result :measurements
                                                        :problems :leak-failures])
                                       (:suites receipt))))
      restricted (read-receipt "restricted")
      prewarm (read-receipt "prewarm")
      current-digest (runner/source-digest)
      agreement (= current-digest (:source-digest restricted) (:source-digest prewarm))
      passed (and agreement (= :passed (:state restricted) (:state prewarm)))
      report {:verdict (if passed :pass :no-go)
              :recorded-at (str (java.time.Instant/now))
              :findings [{:id :F3 :status :repaired
                          :red {:state :probe-failed :reloads 36 :tests 0
                                :witness {:test 1 :pass 1 :fail 7 :error 0}
                                :log "checks/red.log"}
                          :green {:kind :probe-target-not-a-test-namespace
                                  :reloads 0 :authorized-roots ["test"]
                                  :log "checks/focused.log"}}
                         {:id :sleep-pin :status :repaired
                          :from-line 253 :to-line 288 :log "checks/sleep-pin.log"}]
              :measurements {:focused {:test 8 :pass 930 :fail 0 :error 0
                                       :log "checks/focused.log"}
                             :fast {:invocations 1 :exit 2 :test 1243 :pass 12315
                                    :fail 2 :error 0 :sum-ms 45480 :makespan-ms 25785
                                    :isolation-violations 0 :skipped 0 :log "fast.log"}
                             :restricted (assoc (summarize restricted)
                                                :log "restricted-gate.log"
                                                :receipt "restricted-receipt.edn")
                             :prewarm (assoc (summarize prewarm)
                                             :log "prewarm-gate.log"
                                             :receipt "prewarm-receipt.edn")}
              :current-source-digest current-digest
              :gate-source-digests-agree agreement
              :commits {:prior-evidence "404066a1" :red "f5b07a9f"
                        :boundary "0e992f4f" :sleep-pin "14ad99e7"
                        :evidence "The commit containing this report, committed last."}
              :least_sure ["Canonical-root authorization does not claim protection against concurrent filesystem replacement between discovery and reload."]
              :disagreements []
              :owed ["Independent Sol/Fable review and acceptance; no landing or certification claimed."]}]
  (spit (str dir "report.edn") (with-out-str (pprint/pprint report)))
  (prn (select-keys report [:verdict :recorded-at :current-source-digest
                           :gate-source-digests-agree]))
  (doseq [mode [:restricted :prewarm]]
    (prn {mode (get-in report [:measurements mode])}))
  (System/exit (if passed 0 1)))
