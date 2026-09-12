(require '[clojure.edn :as edn]
         '[clojure.string :as str]
         '[clj-surgeon.lane-manifest :as lm]
         '[clj-surgeon.battery-parallel-runner :as runner])

(def root "docs/observations/2026-09-12-bbtower-block-b/attempt7/")
(defn receipt [name]
  (edn/read-string (slurp (str root name "/receipt.edn"))))
(defn walls [receipt]
  (into {} (map (juxt :namespace :elapsed-ms) (:runs receipt))))
(defn total [wall-map members]
  (reduce + (map wall-map members)))

(let [names ["a-base" "b-subject-jvm" "c-subject" "test-fast"]
      receipts (mapv receipt names)
      wall-maps (mapv walls receipts)
      members (set (keys (first wall-maps)))
      original-bb (set (runner/bb-namespaces))
      _ (assert (= 61 (count members)))
      _ (assert (= members (set (keys (second wall-maps)))))
      _ (doseq [ws (drop 2 wall-maps)] (assert (every? #(contains? ws %) members)))
      rows (mapv (fn [name r ws]
                   {:arm name :fast-members (count members)
                    :fast-sum-ms (total ws members)
                    :all-sum-ms (reduce + (vals ws))
                    :makespan-ms (:wall-ms r) :state (:state r)
                    :width (:lane-count r) :processes (:process-count r)
                    :original-bb-sum-ms (when (every? #(contains? ws %) original-bb)
                                          (total ws original-bb))
                    :bb-runtime-sum-ms (when (contains? #{"c-subject" "test-fast"} name)
                                         (reduce + (for [[n wall] ws
                                                         :when (= :bb (get lm/namespace-runtimes n))] wall)))
                    :log (str name ".log") :receipt (str name "/receipt.edn")})
                 names receipts wall-maps)
      fast (last rows)
      [aw bw cw] wall-maps
      deltas (->> members
                  (map (fn [n] {:namespace n :a-ms (aw n) :b-ms (bw n) :c-ms (cw n)
                                 :c-minus-a-ms (- (cw n) (aw n))
                                 :runtime-c (get lm/namespace-runtimes n)}))
                  (sort-by (comp - :c-minus-a-ms)) (take 10) vec)
      gate-lines (vec (for [name names
                            line (str/split-lines (slurp (str root name ".log")))
                            :when (re-find #"(?i)census|budget|gate-refused" line)]
                        {:log (str name ".log") :line line}))
      findings ["The original 61 fast members are identical at base and subject."
                "The dominant delta is splice-envelope-test under bb; its subject JVM wall remains small."
                "No coordinator contention cause is established. Step 2 forbids runtime changes for speed; no speculative coordinator patch was made."
                "make test-fast exceeded the unchanged 60000 ms ceiling; steps 3 through 6 were stopped."
                "The corrected hybrid measurement and make test-fast also fail the CLI-output length witness in intent-transaction-test."
                "The first b/c logs were written in the checkout and invalidated isolation. They are retained as contaminated, not acceptance evidence."]
      least-sure ["Single observations, no variance floor: these are localization findings, not a certified performance claim."
                  "Base uses JVM coordination; subject uses bb. Runtime assignments change between JVM-only b and shipped c by experimental design."
                  "Scheduling cost caches differ and b updates the subject fast cache before c; saved plans expose the resulting grouping. Width is eight throughout, but process counts differ."
                  "The repeated b/c pair is sequential after a, not a new matched a/b/c wave; base was not rerun."
                  "All a/b/c JVM workers explicitly use 1 GiB; absolute sums remain provisional. Make test-fast uses the shipped explicit 512 MiB JVM command-line cap with the same JAVA_TOOL_OPTIONS."
                  "Secondary contention cannot be excluded; the measurements do not isolate a small effect from ordinary variation."
                  "Coordinator makespan is timed inside execute-plan!, not complete shell launch wall. JVM classpath preparation uses its shipped 512 MiB cap."]
      owed ["Fable review of the finding and authority to address fast-member bb runtime cost without violating step 2."
            "A supported implementation fix and a passing fast lane."
            "A NEW bb ceiling, derived only after a passing step 2 measurement, its TEST-ISO-007 declaration and boundary witnesses; Fable ratification."
            "Probe refusal registry/completeness witness and receipt booleans with false seams for the third verb."
            "Second receipt-boolean encounter scoring: not attempted because step 4 was not reached."
            "Tip feature-thread bb/JVM measurements, measured runtime assignment and unchanged integration gate."
            "Final-tip prewarm and any authorized repair/retry; zero prewarm attempts were used."
            "Independent review; no built/certified probe claim."]
      dogfood [{:file "measure.clj" :intent "Create a/b/c measurement harness and save exact plans/receipts"
                :mechanism :native-apply-patch :refusal nil :repair-text-sufficed :not-applicable}
               {:file "measure.clj" :intent "Force b runtime map before partitioning, so disabling bb does not create spurious split jobs"
                :mechanism :native-apply-patch :refusal nil :repair-text-sufficed :not-applicable}
               {:file "summarize.clj" :intent "Create and complete this form-based receipt summarizer; derive report numbers without manual transcription"
                :mechanism :native-apply-patch :refusal nil :repair-text-sufficed :not-applicable}
               {:file "summarize.clj" :intent "Remove unused row destructuring after lint named the unused bindings"
                :mechanism :native-apply-patch :refusal nil :repair-text-sufficed :not-applicable}]
      report {:verdict :stopped :stop-step 2 :generated-utc (str (java.time.Instant/now))
              :base "eae1e43280635be9b4e317e176d29476fd358702"
              :measured-subject "25137b25b6145b0870d3c006ccf0c33ccb10e312"
              :findings findings
              :measurements {:arms rows :top-deltas deltas
                             :bb-sum-ms (:original-bb-sum-ms fast)
                             :bb-chosen-ceiling-ms nil :bb-ceiling-reason :stopped-before-registration
                             :feature-thread {:bb-ms nil :jvm-ms nil :status :not-run}
                             :integration-sum-ms nil :prewarm-attempts 0 :prewarm-lines []
                             :budget-and-refusal-lines gate-lines}
              :commits [{:sha (first *command-line-args*) :role :tech-tree-finding}]
              :artifact-commit "This directory is committed last; obtain its SHA from git log -- this directory."
              :least_sure least-sure
              :disagreements ["The requested coordinator-only fix is not supported by the dominant measured cause; no runtime or budget change was made."
                              "Fable has not reviewed or ratified this attempt; no agreement is inferred."]
              :owed owed :dogfood dogfood
              :production-source-edits []
              :environment {:JAVA_TOOL_OPTIONS "-Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx"
                            :TMPDIR "/var/tmp/forge/bbtower-fx" :_JAVA_OPTIONS :unset}
              :prewarm-attempts 0}
      table (str "| Arm | Original fast sum (ms) | All namespace sum (ms) | Makespan (ms) | Width / processes | Result | Logs |\n"
                 "|---|---:|---:|---:|---|---|---|\n"
                 (apply str (for [r rows]
                              (str "| " (:arm r) " | " (:fast-sum-ms r) " | " (:all-sum-ms r)
                                   " | " (:makespan-ms r) " | " (:width r) " / " (:processes r)
                                   " | " (name (:state r)) " | [log](" (:log r) "), [receipt](" (:receipt r) ") |\n"))))
      delta-table (str "| Namespace | a JVM (ms) | b JVM (ms) | c (ms) | c − a (ms) | c runtime |\n"
                       "|---|---:|---:|---:|---:|---|\n"
                       (apply str (for [d deltas]
                                    (str "| " (:namespace d) " | " (:a-ms d) " | " (:b-ms d)
                                         " | " (:c-ms d) " | " (:c-minus-a-ms d) " | " (:runtime-c d) " |\n"))))]
  (spit (str root "report.edn") (pr-str report))
  (spit (str root "meter.tsv")
        (str "arm\tfast_sum_ms\tall_sum_ms\tcoordinator_makespan_ms\tstate\tlog\treceipt\n"
             (apply str (for [r rows]
                          (str (str/join "\t" (map r [:arm :fast-sum-ms :all-sum-ms :makespan-ms :state :log :receipt])) "\n")))))
  (spit (str root "slowdown.md")
        (str "# Slowdown localization — stopped at step 2\n\n" table "\n"
             "The comparison charges exactly the original 61 fast members in every arm; c additionally runs the shipped bb union. The top deltas below are computed from the three linked receipts, sorted by c minus a.\n\n"
             delta-table "\n"
             "The dominant cost is execution of splice-envelope-test under bb. Its b JVM measurement is close to a; the hybrid increase concentrates in portable namespaces executing under bb. This is not evidence that the bb union was mistakenly charged to fast. Neither a common heap cap nor unchanged automatic width explains this concentration. Both runners require namespaces before starting their namespace timers, so JVM process startup is not charged to these namespace walls (test/run_all.clj and test/clj_surgeon/mcp_test_runner.clj).\n\n"
             "A scheduling barrier does not remove intrinsic bb execution cost. No coordinator-only fix is established by these observations, and changing runtime classification for speed is explicitly forbidden in step 2. The one required make test-fast was run unchanged and triggered the stop.\n\n"
             "## Method and limitations\n\n"
             (str/join "\n" (map #(str "- " %) least-sure)) "\n\n"
             "Run a used the existing verified detached base checkout and the JVM test-deps entrance. Runs b/c used bb and measure.clj at the subject. The harness changes only worker heap options in a/c; b additionally selects the original fast set and forces JVM before planning. Exact worker plans are in each measurement-plan.edn. The coordinator's printed estimates precede the harness heap override.\n\n"
             "The first b/c pair is preserved in b-subject-jvm-contaminated*/c-subject-contaminated*. A log modification under docs/observations triggered the b ns-isolation witness. The corrected pair wrote live evidence under /var/tmp/forge/bbtower-fx/attempt7 and was copied here only after all suites ended. Base logs were already outside the base checkout. No failed observation is presented as green.\n\n"
             "Reproduction: from the appropriate checkout, use JAVA_TOOL_OPTIONS='-Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx', TMPDIR=/var/tmp/forge/bbtower-fx and unset _JAVA_OPTIONS. For a: clojure -J-Xms64m -J-Xmx1g -M:clj-surgeon/test-deps ABS/measure.clj a EXTERNAL-OUTPUT. For b/c: bb -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx ABS/measure.clj ARM EXTERNAL-OUTPUT. Run sequentially. make test-fast used the same environment. No outer timeout or shared-server entrance was used.\n"))
  (spit (str root "gate.md")
        (str "# Gate evidence\n\nNo prewarm ran: the fast stop fired first. Consequently there are no prewarm refusal-census or budget lines. The following measured budget/refusal lines are reproduced verbatim, with their source logs.\n\n"
             (apply str (for [{:keys [log line]} gate-lines]
                          (str "[" log "](" log ")\n\n```text\n" line "\n```\n")))))
  (spit (str root "REPORT.md")
        (str "# Block B — STOPPED\n\n"
             "The required make test-fast reports **" (:fast-sum-ms fast)
             " ms against 60,000 ms**. Step 2's stop is honored; steps 3–6 were not started. No production source, budget, membership or runtime classification changed. No push, tag, shared install or prewarm was performed.\n\n"
             table "\n"
             "## Finding and decision\n\n"
             (str/join "\n" (map #(str "- " %) findings)) "\n\n"
             "The coordinator-only implementation is **not completed**: the evidence does not support claiming that scheduling fixes the dominant runtime cost. A speculative barrier was not substituted for a demonstrated fix. Detailed deltas, exact commands, discarded-run failures and qualifications are in [slowdown.md](slowdown.md). The separate intent-transaction failure is in [c lane 0](c-subject/lane-0.out) and [fast lane 0](test-fast/lane-0.out).\n\n"
             "## BB policy and remaining contract\n\n"
             "No bb ceiling was selected. The original bb inventory's sum in the required fast run is " (:original-bb-sum-ms fast)
             " ms; all bb-runtime namespaces sum to " (:bb-runtime-sum-ms fast)
             " ms ([receipt](test-fast/receipt.edn)). These are distinct sets and neither is an admitted ceiling. Step 3 requires a new declaration and Fable review after step 2 passes; it is not restoration of a pre-existing budget. Feature-thread, integration and prewarm measurements are unknown.\n\n"
             "## DOGFOOD — every source edit\n\n"
             "| File | Intent | Mechanism | Refusal type | Repair text sufficed |\n|---|---|---|---|---|\n"
             (apply str (for [d dogfood]
                          (str "| [" (:file d) "](" (:file d) ") | " (:intent d)
                               " | native apply_patch on exact forms | none | n/a |\n")))
             "\nOnly observation tooling was edited. measure.clj was executed through the real base and subject coordinator entrances; summarize.clj derives this report from their receipts. Runtime-map adaptation was made before b ran. The log-location repair came from the failing isolation assertion, not an editor refusal. No Surgeon operation was attempted and no tool refusal is invented. docs/tech-tree.md records the finding through a separate native prose patch. report.edn is serialized from Clojure data by summarize.clj. Initial lint identified unused bindings (repaired) and duplicate requires from analyzing both standalone user-namespace scripts together. Each script then passed separately through ~/bin/clj-kondo with zero errors/warnings; see lint-initial.log, lint-measure.log and lint-summarize.log.\n\n"
             "## Least sure and disagreements\n\n"
             (str/join "\n" (map #(str "- " %) least-sure))
             "\n\nFable has not reviewed this attempt. The disagreement is with the assumption that a coordinator-only fix follows from the measured slowdown. Independent acceptance remains external.\n\n"
             "## Owed\n\n" (str/join "\n" (map #(str "- " %) owed))
             "\n\nFinding commit: `" (first *command-line-args*) "`. This directory is committed last; its own commit SHA is intentionally not self-embedded. Measured source: `25137b25b6145b0870d3c006ccf0c33ccb10e312`; base: `eae1e43280635be9b4e317e176d29476fd358702`. [Machine report](report.edn), [meter](meter.tsv), [verbatim gate lines](gate.md).\n"))
  (prn {:verdict :stopped :fast-sum-ms (:fast-sum-ms fast) :top-delta (first deltas)
        :rows rows :generated-files ["slowdown.md" "gate.md" "REPORT.md" "report.edn" "meter.tsv"]}))
