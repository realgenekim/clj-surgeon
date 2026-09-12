(require '[babashka.fs :as fs]
         '[babashka.process :as proc]
         '[clj-surgeon.lane-manifest :as lm]
         '[clojure.edn :as edn]
         '[clojure.string :as str])

(def root "docs/observations/2026-09-12-bbtower-block-b/attempt8/")
(def prior "docs/observations/2026-09-12-bbtower-block-b/attempt7/")
(defn read-data [path] (when (fs/exists? path) (edn/read-string (slurp path))))
(defn read-log [path] (when (fs/exists? path) (slurp path)))
(defn summed [runs predicate] (reduce + (map :elapsed-ms (filter predicate runs))))
(defn receipt-row [label path]
  (when-let [r (read-data path)]
    {:label label :receipt path :state (:state r) :result (:result r)
     :fast-sum-ms (summed (:runs r) #(= :fast (lm/lane-of (:namespace %))))
     :all-sum-ms (summed (:runs r) (constantly true))
     :makespan-ms (:wall-ms r) :width (:lane-count r) :processes (:process-count r)}))

(let [inputs (read-data (str root "report-input.edn"))
      old-rows (mapv #(receipt-row % (str prior % "/receipt.edn"))
                     ["a-base" "b-subject-jvm" "c-subject"])
      fast-rows (mapv #(receipt-row % (str root % "/receipt.edn"))
                      ["test-fast" "test-fast-after-cli"])
      feature (mapv #(read-data (str root "feature-" % ".edn")) ["jvm" "bb"])
      cli (read-data (str root "cli-comparison.edn"))
      logs (sort (map str (fs/glob root "*.log")))
      gate-lines (vec (for [path logs line (str/split-lines (slurp path))
                            :when (re-find #"(?i)census|budget|gate-refused|bb-runtime|TEST-ISOLATION" line)]
                        {:log path :line line}))
      commits (str/split-lines (:out @(proc/process ["git" "log" "--reverse" "--format=%H %s"
                                                     "61c75586..HEAD"] {:out :string})))
      findings ["The runtime rule reassigns splice-envelope, insert-forms and rename-alias-receipt to JVM; rename-alias remains bb at 6478/3265 = 1.984073506891271, within the 2.0 threshold."
                "clj-splice's envelope test is 34x slower under bb than the JVM: 834 ms JVM versus 28,591 ms bb (attempt7/a-base and c-subject). That bounds where the babashka-first plan can put heavy rewrite-clj work. This is a finding, not a decision."
                "The first attempt8 fast sum is 36,944 ms. Its audit-link failure was repaired; the second fast run exposed a command-selection regression, also repaired with 40 coordinator tests / 250 assertions. Neither changed a budget."
                "CLI stdout has no bb-only leak. A controlled private-index replay prints 26,581 characters on BOTH runtimes against 2,876-character receipts; current-index outputs are 1,279 on both. Raw outputs differ only in receipt-path UUID. Normalized output bytes agree. Unbounded shared workspace-status paths remain owed; the JVM contract escape does not fix them."
                "The NEW bb ceiling is 399,155 ms: ceil(245,773 * 60,000 / 36,944). This is new intent, not a restored base budget."
                "Probe now registers seven native-failure refusal rows and its false verification_complete seam as the third verb. Four contract tests passed 84 assertions. Built, not certified."
                "Encounter scoring was attempted through the installed entrance and refused encounter-not-found receipt-booleans. The installed seed describes the class, but the active ledger has no registration; no score or acceptance was invented."]
      report (merge
               {:verdict (:verdict inputs) :findings findings
                :measurements {:attempt7 old-rows :fast fast-rows
                               :attempt7-top-deltas (get-in (read-data (str prior "report.edn")) [:measurements :top-deltas])
                               :bb-step2-sum-ms 245773 :bb-ceiling-ms 399155
                               :feature-thread feature :cli cli
                               :integration (:integration inputs) :prewarm (:prewarm inputs)
                               :gate-lines gate-lines}
                :commits commits
                :least_sure ["Single observations on one box; no variance floor or routing-performance certification."
                             "Step1 uses attempt7 receipts, not new matched controls."
                             "Both tip namespace measurements use 1 GiB; Make workers retain their shipped explicit heap caps."
                             "The historical 11,806-character stdout was not saved, so its exact bytes cannot be claimed reconstructed."
                             "Raw CLI byte identity across separate mutations is prevented by the generated receipt UUID."
                             "Observation files are committed after gate execution, as instructed; the final archive commit is not itself a measured source snapshot."]
                :disagreements ["The CLI output defect is shared and workspace-dependent, not specific to bb."
                                "The bb ceiling charges bb-runtime namespace walls, separately from overlapping cadence budgets."
                                "Independent Fable acceptance and ceiling ratification remain external."]
                :owed (:owed inputs)
                :base "eae1e43280635be9b4e317e176d29476fd358702"
                :started-head "61c755860829e55a5c1ba9f01fcd0427992bb3c1"
                :generated-utc (str (java.time.Instant/now))
                :measured-feature-head "9d76115a"
                :dogfood-table "dogfood.md"}
               (select-keys inputs [:stopped-at :final-code-head]))]
  (spit (str root "report.edn") (str (pr-str report) "\n"))
  (spit (str root "gate.md")
        (str "# Verbatim refusal-census, budget and gate lines\n\n"
             (apply str (for [[path rows] (group-by :log gate-lines)]
                          (str "## " path "\n\n```text\n"
                               (str/join "\n" (map :line rows)) "\n```\n\n")))))
  (spit (str root "reassignments.md")
        (str "# Runtime rule: TEST-ISO-016\n\nPortable AND bb/JVM <= 2.0; unmeasured assignments stay unchanged. Contract failures carry an explicit reason. The original 35 bb-assigned fast members are all paired.\n\n"
             "| Namespace | JVM ms | bb ms | Ratio | Decision | Logs / reason |\n|---|---:|---:|---:|---|---|\n"
             (apply str (for [[n m] (sort-by key lm/runtime-measurements)]
                          (format "| %s | %d | %d | %.6f | %s | %s; %s%s |\n"
                                  n (:jvm-ms m) (:bb-ms m) (:ratio m)
                                  (if (= :jvm (lm/namespace-runtimes n)) "reassign JVM" "retain bb")
                                  (:jvm-log m) (:bb-log m)
                                  (if-let [reason (:contract-failure m)] (str "; " reason) ""))))
             "\n## Unmeasured: retained assignments\n\n"
             (apply str (for [[n r] (sort-by key lm/unmeasured-runtimes)] (str "- " n " " r "\n")))))
  (spit (str root "meter.tsv")
        (str "subject\truntime\twall_ms\tlog\n"
             (apply str (for [r feature :when r]
                          (str (:namespace r) "\t" (:runtime r) "\t" (:elapsed-ms r)
                               "\tfeature-" (name (:runtime r)) ".log\n")))
             (apply str (for [r fast-rows :when r]
                          (str (:label r) " original fast\thybrid\t" (:fast-sum-ms r)
                               "\t" (:receipt r) "\n")))))
  (spit (str root "REPORT.md")
        (str "# Block B attempt 8 — " (name (:verdict report)) "\n\n"
             (:summary inputs) "\n\n## Findings\n\n"
             (str/join "\n\n" findings)
             "\n\n## Measurements\n\n| Run | Original fast sum ms | All namespace sum ms | Makespan ms | State | Receipt |\n|---|---:|---:|---:|---|---|\n"
             (apply str (for [r (concat old-rows fast-rows) :when r]
                          (format "| %s | %d | %d | %d | %s | %s |\n"
                                  (:label r) (:fast-sum-ms r) (:all-sum-ms r) (:makespan-ms r)
                                  (:state r) (:receipt r))))
             "\nFeature-thread at 9d76115a:\n\n"
             (apply str (for [r feature :when r]
                          (str "- " (:runtime r) ": " (:elapsed-ms r) " ms, " (pr-str (:result r))
                               " ([log](feature-" (name (:runtime r)) ".log)).\n")))
             (format "\nIntegration passed: **%d ms / %d ms**, %d tests / %d assertions, zero failures, errors or isolation violations ([log](test-integration.log)).\n"
                     (get-in inputs [:integration :sum-ms]) (get-in inputs [:integration :budget-ms])
                     (get-in inputs [:integration :tests]) (get-in inputs [:integration :assertions]))
             "\n| Prewarm | Wall ms | Fast sum ms | Integration sum ms | bb suite runtime sum ms | State | Receipt |\n|---|---:|---:|---:|---:|---|---|\n"
             (apply str (for [r (:prewarm inputs)]
                          (format "| %d | %d | %d | %d | %d | %s | [receipt](%s) |\n"
                                  (:attempt r) (:wall-ms r) (:fast-sum-ms r)
                                  (:integration-sum-ms r) (:bb-suite-runtime-sum-ms r)
                                  (:state r) (:receipt r))))
             "\nBoth prewarms have landing? false by contract. Neither used a repair. The bb suite and MCP suite can overlap in namespace membership; their sums are not additive. Full counters, separate bb spans and paths are in report.edn.\n"
             "\n\n[Verbatim gate lines](gate.md), [runtime decisions and unmeasured inventory](reassignments.md), "
             "[DOGFOOD every source edit](dogfood.md), [machine report](report.edn).\n\n"
             "## Limits and disagreements\n\n"
             (str/join "\n\n" (concat (:least_sure report) (:disagreements report)))
             "\n\n## Owed\n\n" (str/join "\n\n" (:owed report))
             "\n\n[Shared CLI defect detail](owed.md). No push, tag, shared install or merge. "
             "The observation directory is committed last; its SHA is not self-embedded.\n\n## Commits\n\n"
             (str/join "\n" (map #(str "- `" % "`") commits)) "\n"))
  (prn (select-keys report [:verdict :stopped-at :final-code-head])))
