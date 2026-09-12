(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.java.shell :as sh]
         '[clojure.string :as str])

(let [root "docs/observations/2026-09-12-bbtower-block-b/attempt10/"
      read-data #(edn/read-string (slurp (str root %)))
      ceiling (read-data "ceiling-inputs.edn")
      fast (read-data "test-fast/receipt.edn")
      prewarms (vec (for [i [1 2]
                         :let [prefix (str "prewarm-" i "/")
                               path (str prefix "receipt.edn")]
                         :when (.exists (io/file (str root prefix)))
                         :let [r (if (.exists (io/file (str root path)))
                                   (read-data path)
                                   {:state :failed :landing? false :prewarm? true
                                    :stages (read-data (str prefix "stages.edn"))
                                    :suites (mapv #(read-data (str prefix % "/receipt.edn")) ["alias" "mcp" "bb"])})]]
                     {:receipt (when (.exists (io/file (str root path))) path)
                      :log (str "prewarm-" i ".log")
                      :state (:state r) :landing? (:landing? r)
                      :prewarm? (:prewarm? r) :git-head (:git-head r)
                      :wall-ms (:wall-ms r) :stages (:stages r)
                      :suites (mapv #(select-keys % [:suite :state :wall-ms :measurements
                                                     :result :isolation-failures :problems]) (:suites r))}))
      integration (slurp (str root "test-integration.log"))
      integration-ms (parse-long (second (re-find #"namespace walls \(7, slowest first, total (\d+) ms\)" integration)))
      final-gate (last prewarms)
      git (fn [& args]
            (let [r (apply sh/sh "git" args)]
              (assert (zero? (:exit r)) (:err r))
              (str/trim (:out r))))
      report {:verdict (if (= :passed (:state final-gate)) :go-with-review :no-go)
              :recorded-at (str (java.time.Instant/now))
              :baseline "dcd6d26b" :final-code-head (git "rev-parse" "HEAD")
              :findings
              [{:id 1 :status :fixed :evidence ["servlet-plant-red.log" "dead-row-red.log" "dead-valid-row-red.log" "probe-final-green.log"]
                :finding "Per-owner refusal literal coverage includes indirect calls and :kind spellings; both set differences fail."}
               {:id 2 :status :fixed :choice :a :evidence ["probe-boolean-red.log" "probe-final-green.log"]
                :finding "Probe carries no verification booleans; pending cold proof is retained for all verdicts."}
               {:id 3 :status :built-ratification-pending :evidence ["ceiling-derivation.log" "ceiling-run.log" "ceiling-red.log" "ceiling-green.log"]
                :finding "Ceiling uses shipped runtimes and frozen same-run cadence, without a runtime-map patch."}
               {:id 4 :status :fixed :evidence ["diagnostic-red.log" "diagnostic-green.log" "diagnostic-gate-final.log"]
                :finding "Default diagnostic selects its runtime and runs as a required post-pool prewarm/landing stage."}
               {:id 5 :status :fixed :evidence ["lane-moves.md" "lane-budget-red.log" "lane-budget-final.log" "fixture-scan-green.log" "census-proof.log"]
                :finding "47 unassigned owners acquire cadence: 33 fast, 14 battery; no override. Actual bb walls receive parent-side namespace budgets."}]
              :measurements
              {:ceiling (select-keys ceiling [:receipt :bb-ms :fast-ms :ceiling-ms :makespan-ms])
               :test-fast {:log "test-fast.log" :receipt "test-fast/receipt.edn"
                           :state (:state fast) :wall-ms (:wall-ms fast)
                           :measurements (:measurements fast) :result (:result fast)
                           :isolation-failures (:isolation-failures fast)
                           :refusal "Inert old-path assertion mistaken for a fixture; repaired in c6220242 and verified in the subsequent prewarm."
                           :outer-command-wall :invalid-timer-output}
               :encounter {:log "encounter-score.log" :state :refused :reason :encounter-not-found}
               :lint {:logic-log "lint-logic-final.log" :logic-errors 0 :logic-warnings 0
                      :baseline-log "lint-base.log" :candidate-log "lint.log"
                      :baseline-errors 2 :baseline-warnings 7 :new-diagnostics 0}
               :census {:log "census-proof.log" :test-files 159 :test-name-changes []
                        :before 1690 :after 2559 :existing-tests-adopted 869 :removed []}
               :test-integration {:log "test-integration.log" :sum-ms integration-ms
                                  :state :passed :tests 168 :assertions 3798 :isolation-failures 0}
               :outline-runtime-repair {:bb-ms 20859 :jvm-ms 8306
                                        :bb-receipt "outline-bb.edn" :jvm-receipt "outline-jvm.edn"
                                        :runtime :jvm :cadence :integration :budget-ms 20000}
               :prewarm prewarms}
              :commits (str/split-lines (git "log" "--reverse" "--format=%H %s" "dcd6d26b..HEAD"))
              :report-commit :containing-commit
              :least_sure ["Single-run timings are not variance-certified performance claims."
                           "The standalone fast transcript is a retained failure before the scanner repair; final prewarm supplies repaired fast-membership proof."
                           "The shell date timer produced an invalid result; only coordinator makespan is retained as fast timing evidence."]
              :disagreements ["No disagreement with the red-team defects. Pure negative assertions remain in fast; their source-scan false positives were repaired instead of moving them."]
              :owed ["Fable ratification of 374149 ms and independent acceptance; no landing or certification claimed."
                     "Inherited shared CLI workspace-status output is unbounded on dirty trees; runtime selection does not fix it."
                     "Encounter scoring refused encounter-not-found receipt-booleans; the owner must restore or identify its registration before transfer can be scored."]}]
  (assert (= 374149 (:ceiling-ms ceiling)))
  (assert (= 59304 integration-ms))
  (spit (str root "report.edn") (pr-str report))
  (spit (str root "gate.md")
        (apply str
               (for [file ["test-fast.log" "test-integration.log" "prewarm-1.log" "prewarm-2.log"]
                     :when (.exists (io/file (str root file)))]
                 (str "# " file " — verbatim census, budget and gate lines\n\n```text\n"
                      (str/join "\n" (filter #(re-find #"(?i)(census|budget|makespan|serial-equivalent|test-isolation|TEST-ISO|gate-stage:|gate-capacity:|landing-gate:|make-landing|^real )" %)
                                             (str/split-lines (slurp (str root file)))))
                      "\n```\n\n"))))
  (prn (select-keys report [:verdict :final-code-head :measurements :commits])))
