(require '[babashka.process :as p]
         '[clojure.edn :as edn]
         '[clojure.pprint :as pp]
         '[clojure.string :as str])

(def out "docs/observations/2026-09-12-bbtower-block-b/attempt2/")
(def base "eae1e43280635be9b4e317e176d29476fd358702")
(def subject "d4736b46e016b8244b4db0eea0519ae62768a622")
(defn git [& args] (:out @(apply p/process {:out :string :err :inherit} "git" args)))
(defn excerpt [s lo hi]
  (str/join "\n" (map-indexed #(str (+ lo %1) ": " %2)
                                (take (inc (- hi lo)) (drop (dec lo) (str/split-lines s))))))
(def historical (edn/read-string (slurp "/var/tmp/forge/bbtower-fx/fast-before-receipt.edn")))
(def evidence
  {:recorded-at (str (java.time.Instant/now))
   :subject (str/trim (git "rev-parse" "HEAD"))
   :base base
   :jvms-launched 0 :prewarm-attempts 0
   :sentinel (System/getenv "CLJ_SURGEON_TMPDIR_REEXEC")
   :java-tool-options (System/getenv "JAVA_TOOL_OPTIONS")
   :historical-base {:namespace-count (count (:runs historical))
                     :sum-ms (reduce + (map :elapsed-ms (:runs historical)))
                     :makespan-ms (:wall-ms historical)
                     :process-count (:process-count historical)
                     :receipt "/var/tmp/forge/bbtower-fx/fast-before-receipt.edn"}
   :base-entrance (excerpt (git "show" (str base ":Makefile")) 1113 1117)
   :worker-command (excerpt (slurp "test/clj_surgeon/battery_parallel_runner.clj") 432 439)
   :isolation-java-child (excerpt (slurp "test/clj_surgeon/tmp_leak_support.clj") 183 187)
   :isolation-parent-waits (excerpt (slurp "test/clj_surgeon/tmp_leak_support.clj") 390 400)
   :runner-isolation-call (excerpt (slurp "test/clj_surgeon/mcp_test_runner.clj") 316 320)
   :base-isolation-parent-waits (excerpt (git "show" (str base ":test/clj_surgeon/tmp_leak_support.clj")) 390 400)})
(spit (str out "admission-evidence.edn") (with-out-str (pp/pprint evidence)))
(def unblock "Fable must authorize and reseal either a peak-JVM allowance covering coordinators, waiting isolation parents and children, or a revised matched launcher protocol preserving isolation with one live JVM. A worker-width limit alone is insufficient.")
(def report
  {:schema "report/v1" :verdict :blocked
   :manifest_id "4bd681b0898afdb09881555c1be0398fb84c55964345672d79c32797adcf2936"
   :findings [{:severity :blocker :file "test/clj_surgeon/tmp_leak_support.clj" :line 398
               :reproduction "Read admission-evidence.edn or run bb docs/observations/2026-09-12-bbtower-block-b/attempt2/record.clj from the subject root. No JVM is launched. The JVM worker starts an isolation JVM and blocks on its exit; base additionally retains its JVM coordinator."
               :exact_output (:isolation-parent-waits evidence)}]
   :measurements
   [{:name "Historical base receipt, not fresh arm a" :before (:historical-base evidence) :after nil :receipt (str out "admission-evidence.edn")}
    {:name "Fresh matched a/b/c sums, makespans and ten largest deltas" :before nil :after nil :receipt (str out "slowdown.md")}
    {:name "Step 2 fast and bb sums; step 3 new bb ceiling" :before {:fast-budget-ms 60000 :bb-budget nil} :after nil :receipt (str out "slowdown.md")}
    {:name "Feature-thread bb/JVM walls and integration sum" :before {:integration-budget-ms 240000} :after nil :receipt (str out "REPORT.md")}
    {:name "Prewarm attempts and emitted census/budget lines" :before 0 :after 0 :receipt (str out "gate.md")}]
   :commits []
   :least_sure ["No fresh runtime observation establishes the slowdown cause; contention, heap, width and startup remain hypotheses."
                "The minimum JVM allowance for all battery descendants has not been enumerated; two is only the demonstrated worker lower bound."]
   :disagreements ["The sealed one-JVM peak conflicts with the required unchanged coordinator/worker/isolation launch topology. One admitted suite is not one live JVM."]
   :owed [{:what "Step 1 matched a/b/c measurements, top deltas and supported cause" :owner "Fable" :unblock unblock}
          {:what "Steps 2–3 coordinator fix, one test-fast run, measured bb ceiling and boundary witnesses" :owner "Astra" :unblock "Resume after step 1 admission is resolved; use its measured cause. Fable ratifies the new bb ceiling."}
          {:what "Step 4 probe refusals, completeness and boolean false seams; encounter score" :owner "Astra" :unblock "Resume the frozen numbered task order after steps 1–3. Encounter entrance has not been invoked."}
          {:what "Step 5 feature-thread bb/JVM meter, runtime witness and one test-integration run" :owner "Astra" :unblock "Resolve JVM admission and complete preceding steps."}
          {:what "Step 6 final-tip prewarm, bounded repair/final prewarm, verbatim gate lines; manifest battery obligation" :owner "Astra" :unblock "Resolve admission for every descendant and complete implementation. Zero prewarm attempts have been consumed."}
          {:what "Independent acceptance and bb ceiling ratification" :owner "Fable" :unblock "Review the completed implementation and actual measured evidence externally."}]})
(spit (str out "report.edn") (with-out-str (pp/pprint report)))
(println (pr-str (:historical-base evidence)))
(println "Recorded :blocked; JVM launches 0; prewarm attempts 0.")
