(ns clj-surgeon.battery-parallel-runner
  "TEST-ISO-013/015 -- shared battery and landing-gate process coordinator.
   Gate suites derive bounded width; fast and integration use separate waves.

   Historical battery rationale:

   THE PROBLEM. `make test-battery` was 809 s serial on a 16-core box
   (receipt: docs/observations/battery-ledger.edn, sha afde6652, 2026-09-08),
   and it is `battery-fresh`'s only evidence -- so every landing pays it. The
   lane is 35 namespaces of COLD LAUNCHER DRIVES: each one spends its wall
   waiting on a child `java`/`bb`/`clj-kondo`/`git` it started. A suite whose
   cost is other processes' startup is the textbook case for running it wide.

   WHY PROCESSES AND NOT THREADS. kaocha's own parallel work (PR #234, open
   since 2021) runs futures per namespace inside ONE JVM, serialises `require`,
   and measured 6-10% on I/O-bound suites -- with dynamic vars and atoms named
   as the hazard. Every one of those hazards is live here: `clojure.test`'s
   report counters are dynamic, `ns-isolation` snapshots JVM-GLOBAL facts
   (descendant pids, /proc/self/fd, var root identities, live threads), and
   two namespaces sharing a JVM would make every one of those observations
   unattributable. In-JVM threading cannot work for this lane, and would
   destroy the per-namespace attribution the fixture exists to buy.

   So the split is ACROSS PROCESSES, one JVM per lane, by measured
   per-namespace timings -- the shape andreacrotti.pro's 2020 parallel-CI
   write-up reports at ~4x on 4 shards. This repository does not use kaocha
   (`clj-surgeon.mcp-test-runner` is its own lane runner), so there is no
   kaocha hook to extend and nothing to fork: the split is done here.

   THE FLOOR IS THE SLOWEST NAMESPACE, AND IT IS REPORTED. No partition of
   whole namespaces can finish before its largest unit. This runner prints
   that floor with the namespace that owns it on every run, so `why is it not
   faster` is answered on the screen rather than by a bisection.

   THE PROBE EMITS FACTS; THE FOLD EMITS VERDICTS. A lane child runs its
   slice with the SAME per-namespace snapshot fixture and writes what it
   OBSERVED -- counters, walls, per-namespace violations, its own temp-leak
   result -- to an EDN file. It renders no lane-level verdict, because it
   holds no lane: the `Ran N tests` summary and the TEST-ISO-007 LANE budget
   are both statements about the union, and a child that answered them from
   its own slice would report a 5-minute lane as 40 seconds. This coordinator
   folds every verdict over the union of all lanes, so the semantics are
   identical to the serial run by construction.

   A BAD COST TABLE COSTS WALL, NEVER CORRECTNESS. The timings only decide
   which namespace lands in which lane. Every namespace in the manifest's
   battery lane runs exactly once whatever the table says, and an unknown
   namespace is scheduled FIRST (charged the largest known cost), so a new
   battery namespace degrades the makespan rather than the verdict."
  (:require
   [babashka.process :as proc]
   [clj-surgeon.gate-memory :as mem]
   [clj-surgeon.lane-manifest :as lm]
   [clj-surgeon.ns-isolation :as iso]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :as t]))

(def walls-path
  "Where the measured per-namespace walls are recorded, so the NEXT run
   rebalances on what the LAST run actually cost. Rewritten (not appended) on
   every complete run: it is the current best estimate, not an event log --
   the event log is `docs/observations/battery-ledger.edn`."
  "docs/observations/battery-namespace-walls.edn")

(def fallback-wall-ms
  "The estimate for a namespace with no measurement, and the seed the very
   first run schedules on. Deliberately LARGE: an unknown unit scheduled
   first can only lengthen a lane it was going to be in anyway, while an
   unknown unit scheduled last can be the whole tail."
  300000)

(def lane-timeout-ms
  "A lane that has not finished by this wall is DESTROYED, tree and all, and
   named as a failure. The battery's own TEST-ISO-007 lane ceiling is 1800 s;
   a lane holding at most a fraction of the work and still running past it is
   wedged, and a wedged lane must fail loudly rather than hold the landing
   gate open forever."
  (long (* 30 60 1000)))

(def default-lanes
  "Lanes when the caller names none. Measured on anvil-server (16 cores,
   shared with other seats): eight lanes produced the retained 218 s median
   while preserving the serial verdicts namespace by namespace."
  8)

;; ---------------------------------------------------------------------------
;; the summary -- the same shape a serial run prints
;; ---------------------------------------------------------------------------

(defn print-summary!
  "The union summary, in the EXACT shape the serial lane prints it.

   A plain function rather than a `defmethod t/report :summary`, deliberately:
   installing a summary method is a JVM-GLOBAL mutation of `clojure.test`'s
   dispatch table, and this namespace is loaded by its own fast-lane witness --
   which would have quietly changed how the FAST lane reports itself. A
   coordinator that corrupts another lane to render its own output is the
   verifier being blind to its own subject."
  [m notes]
  (t/with-test-out
    (println "\nRan" (:test m) "tests containing"
             (+ (:pass m) (:fail m) (:error m)) "assertions.")
    (println (:fail m) "failures," (:error m) "errors.")
    ;; @spec TEST-ISO-013 -- the NAMED skips, reprinted from the lane children.
    ;; `admit-patch-test` installs this same block in the serial lane and reads
    ;; the messages from its own atoms; those atoms do not cross a process
    ;; boundary, so the coordinator renders the identical block from the notes
    ;; the lanes emitted. Printed in both states, including zero, so a
    ;; non-zero count appears where a reader already looks for the failure
    ;; count -- Astra's finding on today's receipt: ONE skipped recovery
    ;; precondition under an overall PASS must never be hidden by the pass.
    (let [notes (or notes {})]
      (println (or (:precondition-skipped m) 0) "preconditions skipped.")
      (doseq [message (:precondition-skipped notes)] (println "  SKIPPED ·" message))
      (println (or (:precondition-failed m) 0) "preconditions failed.")
      (doseq [message (:precondition-failed notes)] (println "  FAILED ·" message)))))

;; ---------------------------------------------------------------------------
;; the schedule
;; ---------------------------------------------------------------------------

(def prerequisite-stages
  "@spec TEST-ISO-013 -- the battery's DEPENDENCY DAG, read out of the Makefile.

   THE EDGE, and why today's receipt carries a skip. `make test` runs
   `admit-transaction-recovery-battery` and THEN `landing-gate`; the battery
   consumes that target's receipt at
   `target/admit-transaction-recovery-battery-receipt.edn`. `make test-battery`
   has no Make prerequisite edge. Instead, with the now-default
   `BATTERY_PREREQS=1`, this coordinator runs a missing declared stage before
   opening any lane. With `BATTERY_PREREQS=0` -- or when the fast suite is run
   alone -- `admit-patch-test` can reach the check with no receipt on disk and
   records `1 preconditions skipped` under an overall PASS. That is the entire
   DAG for this lane:

     admit-transaction-recovery-battery  ->  clj-surgeon.admit-patch-test
     (everything else)                       independent, any lane, any order

   RUNNING IT IS DEFAULT-ON (`BATTERY_PREREQS=1`), because the normal battery
   is evidence for `make test`, which owns this prerequisite. Satisfying it
   does NOT change what is asserted: MEASURED 2026-09-08, `admit-patch-test` is
   168 tests / 4 325 assertions in BOTH modes, and only `:skipped` moves, 1 to
   0. That is the behaviour MCP-OP-ADMIT-147 demands of that witness -- it
   shall spend the SAME number of assertions in both states, so that the count
   itself stops being evidence of which machine ran it -- so an assertion
   count that DID move with the mode would be the bug, not the contract. The
   serial comparison remains available explicitly with `BATTERY_PREREQS=0`,
   for its ORDER and its one-JVM shape rather than for its count. With the flag
   on, the stage runs before any lane, a failing stage is a named failure, and a
   precondition still skipped afterwards is RED rather than counted: having
   declared that the prerequisites are satisfied, a remaining skip is a broken
   declaration."
  [{:make-target "admit-transaction-recovery-battery"
    :produces "target/admit-transaction-recovery-battery-receipt.edn"
    :consumers '[clj-surgeon.admit-patch-test]
    :why (str "the transaction-recovery battery publishes the kinds it drove; "
              "admit-patch-test reads that receipt to prove the enumeration by "
              "EXECUTION rather than by its structural checks alone")}])

(defn pending-prerequisites
  "The stages whose product is not on disk. A stage whose receipt already
   exists is not re-run: the receipt is the evidence, and re-running a
   minutes-scale battery to reproduce a receipt already present is cost with
   no verdict attached."
  [stages]
  (vec (remove #(.exists (io/file (:produces %))) stages)))

(defn run-prerequisite!
  "Runs one stage's make target, inheriting stdio. Returns its exit code."
  [{:keys [make-target]}]
  (println (format "battery-parallel: prerequisite `make %s` ..." make-target))
  (flush)
  (:exit @(proc/process {:out :inherit :err :inherit
                         :dir (System/getProperty "user.dir")}
                        "make" "--no-print-directory" make-target)))

(def serial-groups
  "@spec TEST-ISO-013 -- namespaces that must stay in ONE lane, in this order,
   each group with the prerequisite that binds it.

   EMPTY TODAY, AND THAT IS AN AUDITED FINDING RATHER THAN AN OVERSIGHT. The
   battery lane was read for cross-namespace prerequisites before this shipped:

     * `admit-patch-test` reads `target/admit-transaction-recovery-battery-receipt.edn`,
       which is the RECOVERY prerequisite Astra named -- but that receipt is
       produced by `make admit-transaction-recovery-battery`, a different
       target, never by another battery namespace. Its absence is already a
       COUNTED, NAMED skip inside the run, which is why the summary block above
       exists.
     * `repository-hygiene-test` shells out to git, but only `ls-files` and
       `check-ignore` -- it reads the INDEX, not the working tree, so a lane
       writing beside it cannot move its verdict.
     * `mcp-alias-migration-test`'s dot-slash-src strings are scope arguments
       inside a per-test temp workspace, not paths into this repository.
     * every other battery namespace takes its scratch space from its own
       temp root, and TEST-ISO-006 gives each lane child a private
       `java.io.tmpdir` of its own.

   The mechanism ships anyway, exercised by its own witness, because the next
   prerequisite must have a place to be DECLARED rather than discovered when a
   lane split makes it flaky."
  [])

(defn apply-serial-groups
  "Folds `serial-groups` into the schedulable units: each group becomes ONE
   unit whose cost is the sum of its members', so the packer can never split
   it across lanes. Returns [unit ...] where a unit is a vector of namespaces
   to run in that order."
  [namespaces groups]
  (let [grouped (into #{} (mapcat identity) groups)
        singles (mapv vector (remove grouped namespaces))]
    (into (mapv vec groups) singles)))

(defn read-walls
  "namespace symbol -> measured wall in ms, from `walls-path`. Missing,
   unreadable or partial is not an error: it degrades to the fallback."
  [path]
  (let [f (io/file path)]
    (if (.exists f)
      (try (let [m (edn/read-string (slurp f))]
             (if (map? m) (:walls-ms m {}) {}))
           (catch Exception _ {}))
      {})))

(def shardable
  "@spec TEST-ISO-013 -- namespaces split ACROSS LANES AT THE DEFTEST LEVEL,
   with the shard count and the reason each one earns it.

   WHY THIS EXISTS AT ALL. The makespan floor of a whole-namespace partition
   is its largest namespace, and this lane's largest is not close: measured
   2026-09-08 on anvil-server, `reader-eval-fence-test` is 461.8 s of an
   816.3 s lane -- 57% of the work in ONE unit. Split by namespace alone the
   battery cannot finish under 7.7 minutes no matter how many JVMs it is
   given, which is a target that was never reachable rather than one that was
   missed. Splitting that namespace's independent `deftest`s across lanes is the only
   move that lowers the floor.

   THE PRECONDITION, ASSERTED RATHER THAN ASSUMED. A namespace may be sharded
   only if it declares NO `:once` or `:each` fixtures: `clojure.test/test-vars`
   runs the once-fixtures around whatever group it is handed, so a sharded
   namespace with a once-fixture would run that fixture once PER SHARD -- a
   different program wearing the same test names. The coordinator refuses to
   shard a namespace whose loaded ns metadata carries either, by name, and
   degrades it to a whole unit rather than running it wrong.

   WHAT A SHARD DOES NOT DECIDE. Its own TEST-ISO-007 ceiling: that is a claim
   about the whole namespace, so the shards' counters, walls and violations are
   merged back into ONE run before any fold, and the budget is re-derived from
   the SUM. Within-namespace ORDER is not preserved and was never defined --
   `test-ns` runs `(vals (ns-interns ns))`, which is hash order; shards select
   by sorted name, which is at least reproducible."
  {'clj-surgeon.reader-eval-fence-test
   ;; @spec TEST-ISO-014 -- seven original witnesses become twelve when the
   ;; six-pair loop is split; the pair-coverage witness is the thirteenth.
   {:shards 13
    :reason (str "461.8 s of an 816.3 s lane in ONE namespace (2026-09-08, "
                 "anvil-server) -- about twenty cold launcher drives, each a "
                 "real child JVM or bb. TEST-ISO-014 splits the 206 s loop "
                 "into six cells; no fixtures, thirteen independent "
                 "deftests, so the split is safe and it is the only split "
                 "that lowers the lane's floor.")}})

(defn test-var-names
  "The `deftest` vars of `n`, sorted, as qualified symbols -- read from the
   LOADED namespace rather than scanned out of its source, because a scan reads
   a spelling and `ns-interns` reads what the JVM actually has."
  [n]
  (->> (ns-interns n) vals (filter (comp :test meta))
       (map #(symbol (str n) (name (symbol %)))) sort vec))

(defn fixture-refusal
  "Why a namespace with ns-metadata `m` may not be sharded, or nil. PURE, so
   both branches are witnessed without a namespace shaped to produce them."
  [n m]
  (let [once (seq (:clojure.test/once-fixtures m))
        each (seq (:clojure.test/each-fixtures m))]
    (when (or once each)
      (format (str "%s declares %s fixture(s); a sharded namespace would run them "
                   "once PER SHARD instead of once per namespace")
              n (str/join " and " (remove nil? [(when once "once") (when each "each")]))))))

(defn shard-refusal
  "Why `n` may not be sharded, or nil. Fixtures are the whole question."
  [n]
  (if-let [nso (find-ns n)]
    (or (fixture-refusal n (meta nso))
        ;; `clojure.test/test-ns` dispatches to this hook instead of its normal
        ;; `test-vars` path. It is therefore a namespace-level fixture even
        ;; though `use-fixtures` did not register it in namespace metadata.
        (when (ns-resolve nso 'test-ns-hook)
          (format (str "%s declares test-ns-hook; sharding would bypass the "
                       "namespace's custom test-ns program") n)))
    (format "%s is not loaded, so its deftests cannot be read" n)))

(defn shard-vars
  "Split `vars` into at most `n` groups by `var-walls` (LPT), evenly by sorted
   name when nothing has been measured. PURE, and every var lands in exactly
   one group -- a var dropped here is a test that stops running.

   `unknown-ms` is what an UNMEASURED var is charged. It must be the
   namespace's own share, never a token 1: the first run charged 1 ms per var
   and LPT correctly packed all seven of `reader-eval-fence-test`'s shards into
   ONE lane -- 461.8 s of work estimated at 7 ms, which is a schedule computed
   from a number nobody measured. Charging each var the namespace's wall
   divided by its shard count spreads them on the very first run, and the
   measured numbers replace the estimate on the second."
  ([vars var-walls n] (shard-vars vars var-walls n 1))
  ([vars var-walls n unknown-ms]
   (let [cost #(get var-walls % unknown-ms)]
     (->> (sort-by (juxt (comp - cost) str) vars)
          (reduce (fn [bins v]
                    (let [i (first (apply min-key (fn [[_ l]] l)
                                          (map-indexed (fn [i b] [i (:load b)]) bins)))]
                      (-> bins (update-in [i :vars] conj v) (update-in [i :load] + (cost v)))))
                  (vec (repeat (max 1 (min n (count vars))) {:vars [] :load 0})))
          (mapv :vars)
          (filterv seq)))))

(defn shard-units
  "Expands `namespaces` into schedulable units, splitting every DECLARED
   shardable namespace into `:shards` groups of its deftest vars by measured
   var cost (LPT), or evenly by sorted name when nothing has been measured.
   A namespace that cannot be sharded degrades to one whole unit, loudly."
  [namespaces decls var-walls walls]
  (vec
    (mapcat
      (fn [n]
        (if-let [{:keys [shards]} (get decls n)]
          (do (try (require n) (catch Throwable t
                                 (println (format "battery-parallel: cannot load %s to shard it (%s)"
                                                  n (.getMessage t)))))
              (if-let [why (shard-refusal n)]
                (do (println (str "battery-parallel: NOT sharding -- " why)) [[n]])
                (shard-vars (test-var-names n) var-walls shards
                            (quot (get walls n fallback-wall-ms) (max 1 shards)))))
          [[n]]))
      namespaces)))

(defn namespace-budget-violation
  "@spec TEST-ISO-007 re-derived over the SUM of a sharded namespace's shards
   -- the wall a serial run would have measured. Same intent, same resource,
   same wording as `ns-isolation/budget-violations`, because a reader must not
   have to notice which path produced the line."
  [n total-ms]
  (let [lane (lm/lane-of n)]
    (when (contains? (get iso/enforced-intents-by-lane lane #{}) "TEST-ISO-007")
      (let [budget (get iso/namespace-budget-overrides n
                        (get iso/lane-default-budget-ms lane
                             iso/default-namespace-budget-ms))]
        (when (> total-ms budget)
          {:intent "TEST-ISO-007" :namespace n :resource "time budget"
           :detail (format (str "ran for %d ms, over its %d ms budget; declare an override in "
                                "clj-surgeon.ns-isolation/namespace-budget-overrides WITH the "
                                "reason, or move it to a slower lane")
                           total-ms budget)})))))

(defn merge-shard-runs
  "The shards of ONE namespace, folded back into the single run a serial lane
   would have produced: counters added, walls summed, violations concatenated,
   and the per-namespace time budget re-derived over the sum."
  [n rs]
  (if (= 1 (count rs))
    (first rs)
    (let [total (reduce + (map :elapsed-ms rs))]
      {:namespace n
       :counters (apply merge-with + (map :counters rs))
       :elapsed-ms total
       :shards (count rs)
       :violations (vec (concat (mapcat :violations rs)
                                (keep identity [(namespace-budget-violation n total)])))})))

(defn cost-of
  "The estimated cost of one schedulable UNIT (a vector of namespaces that must
   run in one lane, usually of length 1)."
  [walls unit]
  (reduce + (map #(get walls % fallback-wall-ms) unit)))

(defn unit-cost
  "The cost of a unit that may be whole namespaces, var shards, or both."
  [walls var-walls unit]
  (reduce + (map #(if (namespace %)
                    ;; @spec TEST-ISO-014 -- the final lane packer must use
                    ;; the same unmeasured namespace share as shard-vars.
                    (get var-walls %
                         (let [n (symbol (namespace %))]
                           (quot (get walls n fallback-wall-ms)
                                 (max 1 (get-in shardable [n :shards] 1)))))
                    (get walls % fallback-wall-ms))
                 unit)))

(defn partition-lanes
  "LPT (longest-processing-time-first) bin packing of `units` into `n` lanes by
   `walls`. Returns a vector of n vectors of namespace symbols.

   LPT rather than round-robin because the lane the fleet waits on is the
   LONGEST one: round-robin over a set with one 460 s unit and thirty 5 s
   units can put two large units in one lane and idle five JVMs. LPT's
   makespan is within 4/3 of optimal, which for this shape is the difference
   between `bounded by the slowest namespace` and `bounded by luck`.

   Deterministic: ties break on the unit's printed form, so the same tree and
   the same timings produce the same schedule and a failure is reproducible."
  ([units walls n] (partition-lanes units walls {} n))
  ([units walls var-walls n]
   (let [n (max 1 n)
         cost #(unit-cost walls var-walls %)]
     (->> (sort-by (juxt (comp - cost) str) units)
          (reduce (fn [lanes unit]
                    (let [i (first (apply min-key
                                          (fn [[_ load]] load)
                                          (map-indexed (fn [i l] [i (:load l)]) lanes)))]
                      (-> lanes
                          (update-in [i :namespaces] into unit)
                          (update-in [i :load] + (cost unit)))))
                  (vec (repeat n {:namespaces [] :load 0})))
          (mapv :namespaces)))))

(defn floor-unit
  "The makespan floor: the largest single UNIT. No partition can finish before
   its largest indivisible unit, and saying so is the difference between a
   target that is missed and a target that was never reachable."
  ([units walls] (floor-unit units walls {}))
  ([units walls var-walls]
   (when (seq units) (apply max-key #(unit-cost walls var-walls %) units))))

;; ---------------------------------------------------------------------------
;; the lanes
;; ---------------------------------------------------------------------------

(defn lane-command
  "The child command for one lane. `--emit-edn` first so that `--ns`'s
   rest-of-argv shape is untouched."
  [java-opts out-path namespaces]
  (into (into ["clojure"] (remove str/blank? (str/split (or java-opts "") #"\s+")))
        (concat ["-M:clj-surgeon/test-deps" "-m" "clj-surgeon.mcp-test-runner"
                 "--emit-edn" (str out-path) "--ns"]
                (map str namespaces))))

(defn slot-command [argv]
  (into ["python3" "-B" (.getCanonicalPath (io/file "test/gate_slot.py")) "--"] argv))

(defn- run-lane!
  [{:keys [index namespaces java-opts work-dir runtime phase suite target checkout-root]}]
  (let [out-path (io/file work-dir (format "lane-%d.edn" index))
        log-path (io/file work-dir (format "lane-%d.out" index))
        err-path (io/file work-dir (format "lane-%d.err" index))
        t0 (System/nanoTime)
        started (System/currentTimeMillis)]
    (io/delete-file out-path true)
    (let [p (apply proc/process
                   {:out :write :out-file log-path
                    :err :write :err-file err-path
                    :dir (or checkout-root (System/getProperty "user.dir"))}
                   (slot-command (cond target ["make" "--no-print-directory" target]
                                   (= :bb runtime)
                                   (into ["bb" "-Xmx512m" "test/run_all.clj" "--emit-edn" (str out-path) "--ns"] (map str namespaces))
                                   :else (lane-command java-opts out-path namespaces))))
          exit (deref (future (:exit @p)) lane-timeout-ms ::timeout)
          timed-out? (= ::timeout exit)]
      (when timed-out? (proc/destroy-tree p) (try @p (catch Exception _ nil)))
      {:index index :suite suite :started-ms started :completed-ms (System/currentTimeMillis)
       :phase phase
       :namespaces (vec namespaces)
       :exit (if timed-out? :timeout exit)
       :wall-ms (quot (- (System/nanoTime) t0) 1000000)
       :log (str log-path)
       :err-log (str err-path)
       :emitted (when (.exists out-path)
                  (try (edn/read-string (slurp out-path))
                       (catch Exception e {::unreadable (.getMessage e)})))})))

;; @spec TEST-ISO-015 -- one executor admission budget across runtime suites.
(defn run-pool! [jobs width run-job]
  (when-not (pos-int? width)
    (throw (ex-info "gate-refused: pool width must be positive" {:width width})))
  (let [executor (java.util.concurrent.Executors/newFixedThreadPool width)]
    (try
      (let [pending (mapv (fn [job]
                            (.submit executor
                                     ^java.util.concurrent.Callable
                                     (reify java.util.concurrent.Callable
                                       (call [_] (run-job job))))) jobs)
            ;; Drain every started/queued child before refusing a failed job.
            ;; Interrupting siblings here could leave their processes alive.
            results (mapv (fn [p]
                            (try {:value (.get ^java.util.concurrent.Future p)}
                                 (catch java.util.concurrent.ExecutionException e
                                   {:error (.getCause e)}))) pending)
            errors (keep :error results)]
        (when (seq errors)
          (throw (ex-info "gate-refused: pool job failed"
                          {:failed-jobs (count errors)} (first errors))))
        (mapv :value results))
      (finally
        (.shutdownNow executor)
        (.awaitTermination executor 30 java.util.concurrent.TimeUnit/SECONDS)))))

;; @spec TEST-ISO-015 -- cold CLI caches must be complete before fan-out.
(defn worker-preparation-command [plan]
  (when (some #(= :jvm (:runtime %)) plan)
    ["clojure" "-J-Xms64m" "-J-Xmx512m" "-Spath" "-M:clj-surgeon/test-deps"]))

(defn prepare-worker-runtime! [plan]
  (when-let [command (worker-preparation-command plan)]
    (let [started (System/nanoTime)
          result @(apply proc/process
                    {:out :string :err :inherit
                     :extra-env {"JAVA_TOOL_OPTIONS" (str (System/getenv "JAVA_TOOL_OPTIONS") " -Xmx512m")}}
                    (slot-command command))
          receipt {:command command :exit (:exit result)
                   :wall-ms (quot (- (System/nanoTime) started) 1000000)}]
      (when (or (not= 0 (:exit result)) (str/blank? (:out result)))
        (throw (ex-info "gate-refused: JVM worker classpath preparation failed" receipt)))
      receipt)))

(defn execute-plan! [plan width]
  (let [started (System/nanoTime)
        preparation (prepare-worker-runtime! plan)
        active (atom 0)
        peak (atom 0)
        phases (atom [])
        lanes (vec (mapcat
                     (fn [[phase jobs]]
                       (let [t0 (System/nanoTime)
                             results (run-pool! (sort-by (comp - :estimated-ms) jobs) width
                                                (fn [job]
                                                  (swap! peak max (swap! active inc))
                                                  (try (run-lane! job)
                                                       (finally (swap! active dec)))))]
                         (swap! phases conj {:phase phase :process-count (count jobs)
                                             :wall-ms (quot (- (System/nanoTime) t0) 1000000)})
                         results))
                     (sort-by key (group-by :phase plan))))]
    {:lanes lanes :phases @phases :preparation preparation :peak-worker-count @peak
     :wall-ms (quot (- (System/nanoTime) started) 1000000)}))

;; ---------------------------------------------------------------------------
;; the fold -- every verdict, over the union
;; ---------------------------------------------------------------------------

(defn selector-namespace
  "The namespace a lane selector names: a bare namespace symbol is itself, and
   a var selector `<namespace>/<deftest>` answers its namespace."
  [sel]
  (if (namespace sel) (symbol (namespace sel)) sel))

;; @spec TEST-ISO-015 -- pure admission decisions, shared by every gate entrance.
;;
;; The floor, the formula and the override are read from
;; `clj-surgeon.gate-memory` so that the refusal, `bin/install-preflight` and
;; docs/install/skiff.md cannot drift apart. On 2026-09-10 the skiff's operator
;; declared GATE_MEMAVAIL_MIB=3072 -- an honest grant, larger than a whole lane
;; charge -- and was bounced by `{:memory-mib 3072 :required-mib 3584}` with no
;; statement anywhere of where 3584 came from or of how much to grant instead.
(defn gate-width [cpus memory-mib]
  (let [by-memory (quot (- memory-mib mem/reserve-mib) mem/lane-charge-mib)]
    (when (< by-memory 1)
      (throw (ex-info (format "gate-refused: insufficient memory for a bounded lane -- %d MiB available, %s"
                              memory-mib mem/floor-note)
                      {:memory-mib memory-mib
                       :required-mib mem/single-lane-floor-mib
                       :reserve-mib mem/reserve-mib
                       :lane-charge-mib mem/lane-charge-mib
                       :remedy (str "grant at least " mem/single-lane-floor-mib
                                    " with GATE_MEMAVAIL_MIB")})))
    (max 1 (min (max 1 (quot cpus 2)) by-memory))))

(defn- shell-out
  "Trimmed stdout of a command, or nil when it does not exist or fails.
   A missing binary is a normal answer on a foreign platform, not an error."
  [& argv]
  (let [{:keys [exit out]} (apply mem/shell-result argv)]
    (when (= 0 exit) out)))

(defn machine-cpus
  "The CPU count the gate divides. `nproc` is GNU coreutils and is absent on
   darwin; `sysctl -n hw.ncpu` is the same figure there. The JVM's own count is
   the last resort on any platform that has neither."
  []
  (or (some-> (shell-out "nproc") parse-long)
      (some-> (shell-out "sysctl" "-n" "hw.ncpu") parse-long)
      (.availableProcessors (Runtime/getRuntime))))

;; @spec TEST-ISO-015 -- ONE reader, and this is the call the gate makes.
;;
;; The reader itself lives in `clj-surgeon.gate-memory` so that
;; `bin/install-preflight` can execute THE SAME CODE without loading this
;; coordinator. Before 2026-09-10 the preflight decided for itself that darwin
;; memory was readable (`command -v vm_stat`) and printed a source; the gate's
;; reader then refused on the same box minutes later. A preflight that prints a
;; source the gate cannot use is the defect, and it can only be closed by there
;; being one implementation to be green about.
(defn machine-memory-available-mib
  "Available MiB, or a typed refusal naming the step that could not answer and
   the documented override."
  []
  (mem/available-mib))

(defn gate-slot-root
  "The directory the box-wide semaphore publishes its slots under, or
   \"abstract\" on a platform with an abstract Unix namespace -- ASKED OF
   `test/gate_slot.py`, which is the module that chooses it. The landing
   receipt carries it because the skiff's `AF_UNIX path too long` named
   neither the path nor the limit nor who picked them, and a receipt that
   cannot say where its own semaphore lives cannot be used to diagnose one."
  []
  (let [{:keys [exit out]} (mem/shell-result "python3" "-B" "test/gate_slot.py" "--print-root")]
    (if (= 0 exit) out (str "unknown (" (pr-str exit) ")"))))

(defn gate-admission-mode
  "The guarantee level of the box-wide slot semaphore, carried in the landing
   receipt. :abstract-socket -- a split semaphore is UNREPRESENTABLE (linux
   abstract namespace: no path, no inode, no root to disagree about).
   :path-socket -- it is DETECTED, not impossible (darwin has no abstract
   namespace; see test/gate_slot.py for the exact residual hole)."
  []
  (let [declared (System/getenv "GATE_SLOT_BACKEND")
        os-name (str/lower-case (or (System/getProperty "os.name") ""))]
    (cond (= "path-socket" declared) :path-socket
          (= "abstract" declared) :abstract-socket
          (str/includes? os-name "linux") :abstract-socket
          :else :path-socket)))

(defn machine-capacity []
  (let [cpus (machine-cpus)
        memory (machine-memory-available-mib)]
    {:cpus cpus :memory-mib memory :lanes (gate-width cpus memory)
     :admission (gate-admission-mode)
     :heap-mib 512
     :reserve-mib mem/reserve-mib
     :lane-charge-mib mem/lane-charge-mib
     :floor-mib mem/single-lane-floor-mib}))

(defn census-problems [expected observed]
  (cond-> []
    (not= (set expected) (set observed))
    (conj {:kind :namespace-census-mismatch
           :missing (vec (sort (remove (set observed) expected)))
           :unexpected (vec (sort (remove (set expected) observed)))})
    (not= (count observed) (count (set observed)))
    (conj {:kind :duplicate-namespace})))

(defn parity-delta [control candidate]
  (vec (for [n (sort (into (set (keys control)) (keys candidate)))
             :when (not= (get control n) (get candidate n))]
         {:namespace n :control (get control n) :candidate (get candidate n)})))

(defn landing-eligible?
  ([debug? problems] (landing-eligible? debug? false problems))
  ([debug? prewarm? problems]
   (and (not debug?) (not prewarm?) (empty? problems))))

(defn bb-namespaces []
  (or (some-> (re-find #"(?s)\(def namespaces\s+'(\[.*?\])\)"
                       (slurp "test/run_all.clj")) second edn/read-string)
      (throw (ex-info "gate-refused: unreadable Babashka namespace census" {}))))

(defn suite-namespaces [suite]
  (case suite
    "battery" (lm/namespaces-for :battery)
    "fast" (lm/namespaces-for :fast)
    "mcp" (vec (mapcat lm/namespaces-for [:fast :integration]))
    "bb" (bb-namespaces)
    "alias" '[clj-surgeon.mcp-alias-migration-test clj-surgeon.receipt-artifacts-boundary-test]
    (throw (ex-info "gate-refused: unknown suite" {:suite suite}))))

(defn source-digest []
  (let [md (java.security.MessageDigest/getInstance "SHA-256")
        files (concat (map io/file ["Makefile" "deps.edn" "bb.edn"])
                      (mapcat #(filter (fn [f] (.isFile ^java.io.File f))
                                       (file-seq (io/file %)))
                              ["src" "test" "resources" "docs/intent"]))]
    (doseq [f (sort-by str files)]
      (.update md (.getBytes (str f "\u0000") "UTF-8"))
      (.update md (java.nio.file.Files/readAllBytes (.toPath ^java.io.File f))))
    (apply str (map #(format "%02x" (bit-and 255 %)) (.digest md)))))

(defn tree-census []
  (let [discovered (vec (sort (keep (fn [f]
                                      (when (and (.isFile ^java.io.File f)
                                                 (re-find #"_test\.cljc?$" (.getName ^java.io.File f)))
                                        (or (some-> (re-find #"(?m)^\(ns\s+(?:\^\{[^}]*\}\s+)?([a-z][a-z0-9.\-]+)"
                                                             (slurp f)) second symbol)
                                            (throw (ex-info "gate-refused: unreadable test namespace declaration"
                                                            {:file (str f)})))))
                                (file-seq (io/file "test")))))
        accounted (vec (sort (into (set (keys lm/manifest))
                                   (concat (keys lm/excluded) (bb-namespaces)))))]
    {:discovered discovered :discovered-count (count discovered)
     :accounted accounted :accounted-count (count accounted)
     :problems (census-problems accounted discovered)}))

(defn child-data-problems [{:keys [namespaces runs result leak-fail]}]
  (cond
    (not (and (sequential? namespaces) (seq namespaces) (vector? runs)
              (map? result) (nat-int? leak-fail)
              (every? (fn [r] (and (symbol? (:namespace r))
                                (nat-int? (:elapsed-ms r))
                                (vector? (:violations r))
                                (map? (:counters r))
                                (every? #(nat-int? (get (:counters r) %))
                                        [:test :pass :fail :error]))) runs)))
    [{:kind :malformed-child-result}]
    :else (cond-> (census-problems namespaces (mapv :namespace runs))
            (not= result (apply merge-with + (map :counters runs)))
            (conj {:kind :child-counter-mismatch})
            (some #(and (some? (:expected-vars %))
                        (seq (remove (set (:executed-vars %)) (:expected-vars %)))) runs)
            (conj {:kind :loaded-test-coverage-drift}))))

(defn suite-receipt-problems [suite receipt digest]
  (vec (concat
         (when-not (and (= :passed (:state receipt))
                        (false? (:debug receipt))
                        (= digest (:source-digest receipt))
                        (= suite (:suite receipt))
                        (zero? (get-in receipt [:result :precondition-skipped] 0)))
           [{:kind :invalid-suite-receipt :suite suite}])
         (census-problems (suite-namespaces suite) (map :namespace (:runs receipt))))))

(defn gate-phases
  "Global fast-before-integration barrier: integration may write checkout files
   that other processes' fast isolation snapshots would observe."
  [namespaces lane-map]
  (mapv (fn [phase] (filterv #(= phase (get lane-map %)) namespaces))
        [:fast :integration]))

(def gate-stage-manifest
  [{:target "admit-transaction-recovery-battery" :kind :before}
   {:target "battery-fresh" :kind :before}
   {:target "alias-migration-test" :kind :suite :suite "alias"}
   {:target "mcp-test" :kind :suite :suite "mcp"}
   {:target "test-bb" :kind :suite :suite "bb"}
   {:target "repository-hygiene" :kind :after}
   {:target "intent-audit" :kind :audit}])

;; @spec TEST-ISO-015 -- execution and print-gate-stages share this manifest.
(defn gate-stages [debug? prewarm?]
  (mapv #(cond-> % (and debug? (= :suite (:kind %))) (update :target str "-serial"))
        (remove #(and prewarm? (= "battery-fresh" (:target %))) gate-stage-manifest)))

(defn gate-targets [debug?]
  (mapv :target (gate-stages debug? false)))

(defn run-gate-stage! [run-id target]
  (let [start (System/nanoTime)
        _ (println "gate-stage:" target "started" (str (java.time.Instant/now)))
        _ (flush)
        rc (:exit @(apply proc/process
                     {:out :inherit :err :inherit
                      :extra-env {"JAVA_TOOL_OPTIONS" (str (System/getenv "JAVA_TOOL_OPTIONS") " -Xmx512m")
                                  "CLJ_SURGEON_GATE_RUN_ID" run-id}}
                     (slot-command ["make" "--no-print-directory" target])))]
    {:target target :exit rc :wall-ms (quot (- (System/nanoTime) start) 1000000)}))

(declare run-gate-pool!)

(defn run-gate! [opts]
  (let [debug? (= "true" (get opts "--debug-serial"))
        prewarm? (= "true" (get opts "--prewarm"))
        run-id (str (java.util.UUID/randomUUID))
        base-dir (io/file (cond debug? "target/gate-serial" prewarm? "target/gate-prewarm" :else "target/gate-parallel"))
        work-dir (io/file base-dir run-id)
        output (io/file (if prewarm? "target/landing-gate-prewarm.edn" "target/landing-gate.edn"))
        _ (io/delete-file output true)
        _ (when prewarm? (io/delete-file (io/file "target/landing-gate.edn") true))
        _ (.mkdirs work-dir)
        _ (spit (io/file base-dir "latest-run") run-id)
        started (str (java.time.Instant/now))
        t0 (System/nanoTime)
        digest (source-digest)
        census (tree-census)
        manifest (gate-stages debug? prewarm?)
        required-suites (vec (keep :suite manifest))
        capacity (update (machine-capacity) :lanes min
                         (reduce + (map (comp count suite-namespaces) required-suites)))
        ;; BEFORE any stage: what this box was measured to have, what the gate
        ;; will spend it on, and the floor -- so the arithmetic behind a later
        ;; refusal is already on the screen the operator kept.
        capacity (assoc capacity :slot-root (gate-slot-root))
        _ (println (format "gate-capacity: %d MiB available, %d cpus, %d lane(s); slots %s %s; %s"
                           (:memory-mib capacity) (:cpus capacity) (:lanes capacity)
                           (name (:admission capacity)) (:slot-root capacity)
                           mem/floor-note))
        _ (when debug? (println "SERIAL/NOT-A-GATE: debugging only; no landing receipt"))
        _ (doseq [s required-suites]
            (io/delete-file (io/file work-dir s "receipt.edn") true))
        stages (atom [])
        pool (atom nil)]
    (when (seq (:problems census))
      (throw (ex-info "gate-refused: tree namespace census" census)))
    (doseq [{:keys [target kind suite]} manifest]
      (let [stage (if (and (= :suite kind) (not debug?))
                    (do
                      (when-not @pool (reset! pool (run-gate-pool! opts run-id capacity work-dir)))
                      (get-in @pool [:stages suite]))
                    ;; Serial suite targets coordinate their own workers; never
                    ;; hold a slot around a coordinator that waits for slots.
                    (if (= :suite kind)
                      (let [start (System/nanoTime)
                            rc (:exit @(proc/process {:out :inherit :err :inherit
                                                      :extra-env {"CLJ_SURGEON_GATE_RUN_ID" run-id}}
                                         "make" "--no-print-directory" target))]
                        {:target target :exit rc :wall-ms (quot (- (System/nanoTime) start) 1000000)})
                      (run-gate-stage! run-id target)))]
        (swap! stages conj stage)
        (spit (io/file work-dir "stages.edn") (pr-str @stages))
        (println "gate-stage:" (pr-str stage))
        (when-not (= 0 (:exit stage))
          (throw (ex-info "gate-refused: required stage failed" stage)))))
    (let [suites (mapv #(edn/read-string (slurp (io/file work-dir % "receipt.edn")))
                       required-suites)
          problems (vec (concat
                          (when-not (= digest (source-digest)) [{:kind :tree-changed-during-gate}])
                          (when-not debug?
                            (mapcat #(suite-receipt-problems (:suite %) % digest) suites))))
          receipt {:state (if (seq problems) :failed :passed)
                   :landing? (landing-eligible? debug? prewarm? problems)
                   :prewarm? (and prewarm? (not debug?))
                   :run-id run-id
                   :git-head (str/trim (:out @(proc/process {:out :string} "git" "rev-parse" "HEAD")))
                   :git-tree (str/trim (:out @(proc/process {:out :string} "git" "rev-parse" "HEAD^{tree}")))
                   :started-at started :completed-at (str (java.time.Instant/now))
                   :wall-ms (quot (- (System/nanoTime) t0) 1000000)
                   :source-digest digest :capacity capacity :namespace-census census
                   :stages @stages :pool (dissoc @pool :stages) :suites suites :problems problems}]
      (spit (io/file work-dir "stages.edn") (pr-str @stages))
      (spit (io/file work-dir "receipt.edn") (pr-str receipt))
      (when (and (= :passed (:state receipt)) (not debug?)) (spit output (pr-str receipt)))
      (println "landing-gate:" (pr-str (dissoc receipt :suites :namespace-census)))
      (when (seq problems) (throw (ex-info "gate-refused: incomplete landing proof" {:problems problems})))
      (shutdown-agents))))

(defn lane-failures
  "Lanes that did not deliver a readable result. A lane that died before
   writing its EDN has counters nobody can add up, so it is a NAMED failure
   rather than a lane that contributed zero tests -- which is exactly how a
   parallel suite silently gets faster by running less."
  [lanes]
  (vec (keep (fn [{:keys [index exit namespaces emitted log]}]
               (cond
                 (= :timeout exit)
                 (format "lane %d TIMED OUT after %d ms and was destroyed; namespaces: %s (log %s)"
                         index lane-timeout-ms (str/join " " namespaces) log)

                 (nil? emitted)
                 (format "lane %d exited %s WITHOUT writing its result; namespaces: %s (log %s)"
                         index exit (str/join " " namespaces) log)

                 (::unreadable emitted)
                 (format "lane %d wrote an unreadable result (%s); namespaces: %s (log %s)"
                         index (::unreadable emitted) (str/join " " namespaces) log)

                 ;; Compared as NAMESPACES, not as selectors: a lane asked for
                 ;; seven var shards of one namespace correctly reports that ONE
                 ;; namespace, and round one failed a perfectly good lane for it.
                 ;; The claim being checked is "every namespace this lane was
                 ;; given produced a result", and that is a claim about
                 ;; namespaces.
                 (not= (set (map selector-namespace namespaces))
                       (set (:namespaces emitted)))
                 (format (str "lane %d ran %s but was asked for %s -- a lane that "
                              "silently ran less is the failure this check exists for (log %s)")
                         index (pr-str (vec (:namespaces emitted)))
                         (pr-str (vec (distinct (map selector-namespace namespaces)))) log)

                 (seq (child-data-problems emitted))
                 (format "lane %d invalid child facts: %s (log %s)" index
                         (pr-str (child-data-problems emitted)) log)

                 (not= 0 exit)
                 (format "lane %d exited %s (log %s)" index exit log)

                 :else nil))
             lanes)))

(defn union-runs
  "Every per-namespace run across every lane, in MANIFEST order -- so the
   isolation report reads the same whichever lane a namespace landed in."
  [lanes battery-namespaces]
  (let [by-ns (group-by :namespace (for [l lanes r (:runs (:emitted l))] r))]
    (vec (keep (fn [n] (when-let [rs (seq (get by-ns n))] (merge-shard-runs n rs)))
               battery-namespaces))))

(defn report!
  "Prints the union summary, the per-namespace walls, the schedule and the
   isolation verdict. Returns the isolation violation count."
  ([runs lanes wall-ms] (report! runs lanes wall-ms true))
  ([runs lanes wall-ms isolation?]
   (let [vs (into (vec (mapcat :violations runs))
                  ;; @spec TEST-ISO-007 -- the LANE budget, over the union.
                  ;; The sum of the namespaces' walls is what a serial run
                  ;; would have paid, so this is the same number the serial
                  ;; lane is held to; the parallel makespan is reported
                  ;; separately and never substituted for it.
                  (keep (fn [[lane rs]]
                          (iso/lane-budget-violation lane (reduce + (map :elapsed-ms rs))))
                        (when isolation? (group-by (comp lm/lane-of :namespace) runs))))]
     (binding [*out* *err*]
       (println (format "\nnamespace walls (%d, slowest first, serial-equivalent total %d ms):"
                        (count runs) (reduce + (map :elapsed-ms runs))))
       (doseq [r (sort-by (comp - :elapsed-ms) runs)]
         (println (format "  %8d ms  %s" (:elapsed-ms r) (:namespace r))))
       (println (format "\nlanes (%d), makespan %d ms:" (count lanes) wall-ms))
       (doseq [l (sort-by :index lanes)]
         (println (format "  lane %d  %8d ms  exit %s  %s"
                          (:index l) (:wall-ms l) (:exit l)
                          (str/join " " (:namespaces l)))))
       (if (seq vs)
         (do (println (format "\nTEST-ISOLATION: %d violation(s) -- the suite's own purity rules, per namespace:" (count vs)))
             (doseq [v vs] (println "  " (iso/message v))))
         (println (if isolation?
                    (format "\ntest-isolation: 0 violations across %d namespace(s) (TEST-ISO-002/003/004/005/007/010)" (count runs))
                    "bb-isolation: existing per-process temp leak contract; no JVM probe claim"))))
     (count vs))))

(defn write-walls!
  "Records what every namespace -- and every measured SHARD's vars -- cost, for
   the next run's schedule."
  [path runs lanes]
  (io/make-parents (io/file path))
  (let [previous-var-walls
        (try (or (:var-walls-ms (edn/read-string (slurp path))) {})
             (catch Exception _ {}))
        shard-runs (for [l lanes r (:runs (:emitted l)) :when (:sharded r)] r)
        ;; @spec TEST-ISO-014 -- prefer actual test-var measurements, even
        ;; from grouped shards. Older receipts only measured isolated vars;
        ;; retain that fallback, never divide an aggregate into guessed walls.
        var-walls (into (sorted-map)
                        (into previous-var-walls
                              (for [r shard-runs
                                    [v wall] (or (:var-walls-ms r)
                                                 (when (= 1 (count (:vars r)))
                                                   {(first (:vars r)) (:elapsed-ms r)}))]
                                [(symbol (str (:namespace r)) (str v))
                                 wall])))]
    (spit path
          (with-out-str
            (println ";; TEST-ISO-013 -- measured walls of the battery lane.")
            (println ";; REWRITTEN by `make test-battery` on every complete run; it is the")
            (println ";; current estimate the next run's bin-packing reads, not an event log.")
            (println ";; A stale or absent entry costs makespan, never correctness.")
            (prn {:walls-ms (into (sorted-map) (map (juxt :namespace :elapsed-ms)) runs)
                  :var-walls-ms var-walls})))))

;; ---------------------------------------------------------------------------

(defn prepare-suite!
  [opts]
  (let [suite (get opts "--suite" "battery")
        battery? (= "battery" suite)
        debug? (= "true" (get opts "--debug-serial"))
        _ (when (and (not battery?) (contains? opts "--lanes"))
            (throw (ex-info "gate-refused: width is automatic; use --debug-serial true for diagnostics" {})))
        inventory (suite-namespaces suite)
        run-id (or (::run-id opts) (System/getenv "CLJ_SURGEON_GATE_RUN_ID") (str (java.util.UUID/randomUUID)))
        _ (when-not (re-matches #"[A-Za-z0-9-]+" run-id)
            (throw (ex-info "gate-refused: invalid run identity" {})))
        capacity (when-not battery? (or (::capacity opts) (machine-capacity)))
        digest (when-not battery? (source-digest))
        lanes-n (if battery?
                  (or (some-> (get opts "--lanes") parse-long)
                      (some-> (System/getenv "BATTERY_LANES") parse-long) default-lanes)
                  (if debug? 1 (min (count inventory) (:lanes capacity))))
        java-opts (if battery?
                    (or (get opts "--java-opts") (System/getenv "BATTERY_CHILD_JAVA_OPTS") "-J-Xms64m -J-Xmx512m")
                    "-J-Xms64m -J-Xmx512m")
        work-dir (doto (io/file (or (get opts "--work-dir")
                                    (if battery? "target/battery-parallel"
                                        (str (if debug? "target/gate-serial/" "target/gate-parallel/") run-id "/" suite))))
                   (.mkdirs))
        _ (io/delete-file (io/file work-dir "receipt.edn") true)
        _ (when debug? (println "SERIAL/NOT-A-GATE:" suite))
        effective-walls-path (if battery? walls-path (str "target/gate-parallel/" suite "/walls.edn"))
        ;; THE INVENTORY IS THE GATE'S OWN MEMBERSHIP, never a list typed here:
        ;; `lane-manifest` is the single source of truth for what the battery
        ;; lane contains, and a namespace added to it is scheduled by the next
        ;; run without anyone remembering to edit this file.
        prereqs? (or (not battery?)
                     (contains? #{"1" "true" "yes"}
                                (or (get opts "--prereqs")
                                    (System/getenv "BATTERY_PREREQS") "1")))
        battery-namespaces inventory
        walls-file (let [f (io/file effective-walls-path)]
                     (if (.exists f)
                       (try (or (edn/read-string (slurp f)) {}) (catch Exception _ {}))
                       (if battery? {}
                           (try (get (edn/read-string (slurp "docs/observations/gate-namespace-walls.edn"))
                                     (keyword suite) {})
                                (catch Exception _ {})))))
        walls (:walls-ms walls-file {})
        var-walls (:var-walls-ms walls-file {})
        units (-> battery-namespaces
                  (apply-serial-groups (if battery? serial-groups []))
                  (->> (mapcat #(shard-units % (if battery? shardable {}) var-walls walls)))
                  vec)
        unknown (vec (remove walls battery-namespaces))
        floor (floor-unit units walls var-walls)
        waves (cond
                (and debug? (not battery?)) [[(vec inventory)]]
                (= "mcp" suite) (mapv #(partition-lanes (mapv vector %) walls var-walls lanes-n)
                                      (gate-phases inventory lm/manifest))
                :else [(partition-lanes units walls var-walls lanes-n)])
        plan (vec (mapcat (fn [phase groups]
                            (map (fn [namespaces] {:phase phase :namespaces namespaces}) groups))
                    (range) waves))]
    (println (format "battery-parallel: %d namespace(s) in %d unit(s) over %d lane(s); floor is %s at %d ms"
                     (count battery-namespaces) (count units) lanes-n
                     (str/join "+" floor) (unit-cost walls var-walls floor)))
    (when (seq unknown)
      (println (format (str "battery-parallel: %d namespace(s) have no measured wall and are "
                            "scheduled first at the %d ms fallback: %s")
                       (count unknown) fallback-wall-ms (str/join " " unknown))))
    (doseq [[i {:keys [namespaces phase]}] (map-indexed vector plan)
            :let [ns-syms namespaces]]
      (println (format "  process %d phase %d (est %d ms): %s" i phase
                       (unit-cost walls var-walls ns-syms)
                       (str/join " " ns-syms))))
    (flush)
    ;; The DAG's one edge, honoured BEFORE any lane opens. Consumers are named
    ;; so a reader can see which lane the edge actually binds.
    (let [pending (if (and battery? prereqs?) (pending-prerequisites prerequisite-stages) [])
          prereq-failures
          (vec (keep (fn [stage]
                       (println (format "battery-parallel: %s is absent; %s consume(s) it"
                                        (:produces stage)
                                        (str/join " " (:consumers stage))))
                       (let [rc (run-prerequisite! stage)]
                         (when-not (zero? rc)
                           (format "prerequisite `make %s` failed (exit %s); consumers: %s"
                                   (:make-target stage) rc
                                   (str/join " " (:consumers stage))))))
                     pending))]
      (when-not prereqs?
        (println (str "battery-parallel: BATTERY_PREREQS is off -- prerequisite stages are "
                      "NOT run and an unmet precondition stays a counted, named skip "
                      "(the serial lane's semantics)")))
      {:suite suite :battery? battery? :debug? debug? :run-id run-id :capacity capacity :digest digest :lanes-n lanes-n :work-dir work-dir :effective-walls-path effective-walls-path :battery-namespaces battery-namespaces :prereqs? prereqs? :prereq-failures prereq-failures :waves waves
       :plan (mapv (fn [i entry]
                     (assoc entry :index i :java-opts java-opts :suite suite
                            :estimated-ms (unit-cost walls var-walls (:namespaces entry))
                            :work-dir work-dir :runtime (if (= suite "bb") :bb :jvm)))
                   (range) (remove (comp empty? :namespaces) plan))})))

(defn finish-suite!
  [{:keys [suite battery? debug? run-id capacity digest lanes-n work-dir effective-walls-path battery-namespaces prereqs? prereq-failures waves]} lanes wall-ms]
  ;; The complete transcript, in LANE ORDER, so a parallel run's output can
  ;; be diffed against a serial one instead of being interleaved noise.
  (doseq [l (sort-by :index lanes)]
    (println (format "\n========== lane %d (%s) exit %s, %d ms =========="
                     (:index l) (str/join " " (:namespaces l)) (:exit l) (:wall-ms l)))
    (when (.exists (io/file (:log l))) (print (slurp (:log l))))
    (when (.exists (io/file (:err-log l)))
      (let [e (slurp (:err-log l))]
        (when-not (str/blank? e)
          (println (format "---------- lane %d stderr ----------" (:index l)))
          (print e)))))
  (flush)
  (let [broken (lane-failures lanes)
        runs (union-runs lanes battery-namespaces)
        missing (vec (remove (set (map :namespace runs)) battery-namespaces))
        result (if (seq runs)
                 (apply merge-with + (map :counters runs))
                 {:test 0 :pass 0 :fail 0 :error 0})
        notes (apply merge-with into (keep (comp :notes :emitted) lanes))
        _ (print-summary! result notes)
        iso-fail (report! runs lanes wall-ms (not= suite "bb"))
        leak-fail (reduce + (keep (comp :leak-fail :emitted) lanes))]
    (when (seq broken)
      (binding [*out* *err*]
        (println (format "\nBATTERY-LANE: %d lane failure(s):" (count broken)))
        (doseq [b broken] (println "  " b))))
    (when (seq missing)
      (binding [*out* *err*]
        (println (format (str "\nBATTERY-LANE: %d battery namespace(s) produced NO result: %s "
                              "-- a battery that ran less is a failure, never a pass.")
                         (count missing) (str/join " " missing)))))
    (when (and (empty? broken) (empty? missing))
      (write-walls! effective-walls-path runs lanes))
    (when (seq prereq-failures)
      (binding [*out* *err*]
        (println (format "\nBATTERY-PREREQ: %d prerequisite stage(s) failed:" (count prereq-failures)))
        (doseq [f prereq-failures] (println "  " f))))
    ;; @spec TEST-ISO-013 -- the skip count in the RECEIPT, always; RED only
    ;; when the caller declared the prerequisites satisfied.
    (let [skipped (or (:precondition-skipped result) 0)
          skipped-red (if (and prereqs? (pos? skipped)) skipped 0)]
      (spit (io/file work-dir "skipped") (str skipped))
      (when (pos? skipped-red)
        (binding [*out* *err*]
          (println (format (str "\nBATTERY-PREREQ: %d precondition(s) still skipped with "
                                "BATTERY_PREREQS=1 -- having declared the prerequisites "
                                "satisfied, a remaining skip is a broken declaration, not a note.")
                           skipped))))
      (println (format "battery-parallel: makespan %d ms over %d lane(s); serial-equivalent %d ms; skipped %d"
                       wall-ms lanes-n (reduce + (map :elapsed-ms runs)) skipped))
      (let [census-errors (when-not battery? (census-problems battery-namespaces (mapv :namespace runs)))
            changed? (and (not battery?) (not= digest (source-digest)))
            failures (+ (:fail result) (:error result) iso-fail leak-fail
                        (count broken) (count missing) (count census-errors)
                        (count prereq-failures) skipped-red
                        (if changed? 1 0))
            receipt {:suite suite :runtime (if (= suite "bb") :bb :jvm)
                     :state (if (zero? failures) :passed :failed) :debug debug? :run-id run-id
                     :source-digest digest :capacity capacity :lane-count lanes-n :process-count (count lanes)
                     :phase-count (count waves)
                     :namespace-census {:expected battery-namespaces :observed (mapv :namespace runs)
                                        :expected-count (count battery-namespaces) :observed-count (count runs)}
                     :lanes (mapv #(dissoc % :emitted) lanes) :runs runs :result result
                     :isolation-failures iso-fail :leak-failures leak-fail
                     :wall-ms wall-ms :problems (vec (concat broken census-errors
                                                       (when changed? [:tree-changed-during-suite])))}]
        (spit (io/file work-dir "receipt.edn") (pr-str receipt))
        receipt))))

(defn run-suite! [opts]
  (let [context (prepare-suite! opts)
        execution (execute-plan! (:plan context) (:lanes-n context))
        receipt (finish-suite! context (:lanes execution) (:wall-ms execution))]
    (when-not (= :passed (:state receipt))
      (throw (ex-info "gate-refused: suite failed" (select-keys receipt [:suite :problems :result]))))
    receipt))

(defn run-gate-pool! [opts run-id capacity work-dir]
  (let [contexts (mapv #(prepare-suite! (assoc opts "--suite" %
                                          "--work-dir" (str (io/file work-dir %))
                                          ::run-id run-id ::capacity capacity))
                       (keep :suite gate-stage-manifest))
        ;; The fast snapshots observe this checkout. The original shell checks,
        ;; alias battery and BB workers join integration only after they drain.
        plan (conj (vec (for [context contexts job (:plan context)]
                          (assoc job :phase (if (and (= "mcp" (:suite job)) (zero? (:phase job))) 0 1))))
                   {:suite "shell" :phase 1 :target "mcp-test-checks" :estimated-ms 110000
                    :work-dir work-dir :index 0})
        execution (execute-plan! plan (:lanes capacity))
        lanes (:lanes execution)
        shell (first (filter #(= "shell" (:suite %)) lanes))
        receipts (mapv (fn [context]
                         (let [children (filterv #(= (:suite context) (:suite %)) lanes)
                               wall (- (apply max (map :completed-ms children))
                                       (apply min (map :started-ms children)))]
                           (finish-suite! context children wall))) contexts)]
    (doseq [f [(:log shell) (:err-log shell)] :when (.exists (io/file f))]
      (print (slurp f)))
    (spit (io/file work-dir "pool.edn") (pr-str (dissoc execution :lanes)))
    (when (or (not= 0 (:exit shell)) (some #(not= :passed (:state %)) receipts))
      (throw (ex-info "gate-refused: runtime pool failed"
                      {:shell-exit (:exit shell) :suites (mapv #(select-keys % [:suite :state :problems]) receipts)})))
    {:target "runtime-pool" :exit 0 :wall-ms (:wall-ms execution)
     :stages (into {} (for [{:keys [target suite]} gate-stage-manifest :when suite
                            :let [receipt (first (filter #(= suite (:suite %)) receipts))]]
                        [suite {:target target :exit 0 :wall-ms (:wall-ms receipt)}]))
     :phases (:phases execution) :preparation (:preparation execution)
     :peak-worker-count (:peak-worker-count execution)
     :shell-checks (dissoc shell :emitted)}))

(defn -main [& args]
  (try
    (let [opts (apply hash-map (map str args))]
      (cond (= "true" (get opts "--print-gate-stages"))
            (doseq [target (gate-targets false)] (println target))
            (= "gate" (get opts "--suite")) (run-gate! opts)
            :else (run-suite! opts))
      (shutdown-agents))
    (catch Throwable e
      (binding [*out* *err*]
        (println "gate-refused:" (.getMessage e) (pr-str (ex-data e))))
      (shutdown-agents)
      (System/exit 1))))
