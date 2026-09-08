(ns ^{:lane :fast} clj-surgeon.battery-parallel-test
  "@spec TEST-ISO-013 -- the witnesses for the battery lane run wide.

   Every one of these is a fold over DATA: the scheduler, the lane-failure
   classifier, the union, and the summary renderer are pure, so the ways a
   parallel suite silently runs LESS than a serial one are all provable here,
   in the fast lane, with no JVM launched and no minutes spent."
  (:require
   [clj-surgeon.battery-parallel-runner :as bp]
   [clj-surgeon.lane-manifest :as lm]
   [clj-surgeon.mcp-test-runner :as runner]
   [clj-surgeon.ns-isolation :as iso]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(def ^:private walls
  '{clj-surgeon.a-test 400000
    clj-surgeon.b-test 10000
    clj-surgeon.c-test 9000
    clj-surgeon.d-test 8000})

(def ^:private units (mapv vector (sort (keys walls))))

;; ---------------------------------------------------------------------------
;; clause 1 -- the inventory is the gate's own membership
;; ---------------------------------------------------------------------------

(deftest the-inventory-is-the-lane-manifests-battery-lane
  (testing "the scheduler reads the manifest, so a namespace added there is run"
    ;; A hard-coded inventory is how a namespace joins the manifest and
    ;; silently stops being run: the gate stays green while covering less.
    (let [src (slurp (io/file "test/clj_surgeon/battery_parallel_runner.clj"))]
      (is (str/includes? src "(lm/namespaces-for :battery)")
          "the battery namespaces must come from clj-surgeon.lane-manifest")
      (is (str/includes? src "(defn -main")
          "read from the entry point that actually runs, not a doc")))
  (testing "no unit exists that the inventory did not supply"
    ;; The strongest form of the claim: hand the scheduler an EMPTY inventory
    ;; and nothing comes back. A declaration that names a namespace (a serial
    ;; group, a shard budget) may only CONSTRAIN what the manifest supplied --
    ;; it can never add a namespace, and it can never keep one alive after the
    ;; manifest drops it.
    (is (= [] (bp/apply-serial-groups [] bp/serial-groups)))
    (is (= [] (bp/shard-units [] bp/shardable {} {})))
    (is (every? (set (lm/namespaces-for :battery)) (keys bp/shardable)))
    (is (every? (set (lm/namespaces-for :battery))
                (mapcat identity bp/serial-groups)))))

;; ---------------------------------------------------------------------------
;; clause 2 -- everything runs exactly once; a lane that ran less FAILS
;; ---------------------------------------------------------------------------

(deftest every-namespace-is-scheduled-exactly-once-across-the-lanes
  (doseq [n [1 2 3 6 11]]
    (testing (str n " lane(s)")
      (let [plan (bp/partition-lanes units walls n)
            scheduled (mapcat identity plan)]
        (is (= (sort (keys walls)) (sort scheduled))
            "every unit lands in exactly one lane, and none is dropped")
        (is (= (count scheduled) (count (set scheduled)))
            "no namespace is scheduled twice"))))
  (testing "the real battery lane, at the real default width"
    (let [makefile (slurp (io/file "Makefile"))
          nss (lm/namespaces-for :battery)
          plan (bp/partition-lanes (bp/apply-serial-groups nss bp/serial-groups)
                                   (bp/read-walls bp/walls-path)
                                   bp/default-lanes)]
      (is (= 8 bp/default-lanes))
      (is (str/includes? makefile "BATTERY_LANES ?= 8")
          "the reviewed parallel width is the Makefile default")
      (is (= (sort nss) (sort (mapcat identity plan)))))))

(deftest one-lane-is-the-serial-shape
  (testing "BATTERY_LANES=1 is one lane holding every namespace"
    (let [plan (bp/partition-lanes units walls 1)]
      (is (= 1 (count plan)))
      (is (= (sort (keys walls)) (sort (first plan))))))
  (testing "a width below one is still one lane, never zero"
    (is (= 1 (count (bp/partition-lanes units walls 0))))))

(deftest a-lane-that-ran-less-than-it-was-asked-for-is-a-named-failure
  (let [asked '[clj-surgeon.a-test clj-surgeon.b-test]
        lane {:index 3 :namespaces asked :exit 0 :log "/tmp/lane-3.out"
              :emitted {:namespaces '[clj-surgeon.a-test]
                        :runs [] :result {:test 0 :pass 0 :fail 0 :error 0}}}
        [msg :as msgs] (bp/lane-failures [lane])]
    (is (= 1 (count msgs))
        "a lane whose result names fewer namespaces than it was asked for fails")
    (is (str/includes? msg "lane 3"))
    (is (str/includes? msg "clj-surgeon.b-test")
        "the refusal names the namespace that did not run")
    (is (str/includes? msg "/tmp/lane-3.out")
        "and the log a reader has to open"))
  (testing "a lane that ran exactly what it was asked for is not a failure"
    (is (= [] (bp/lane-failures
               [{:index 0 :namespaces '[clj-surgeon.a-test] :exit 0 :log "x"
                 :emitted {:namespaces '[clj-surgeon.a-test]}}])))))

(deftest a-lane-that-timed-out-or-wrote-nothing-is-a-named-failure
  (testing "timeout"
    (let [[msg] (bp/lane-failures [{:index 1 :namespaces '[clj-surgeon.a-test]
                                    :exit :timeout :log "L" :emitted nil}])]
      (is (str/includes? msg "TIMED OUT"))
      (is (str/includes? msg "clj-surgeon.a-test"))))
  (testing "died without writing a result"
    (let [[msg] (bp/lane-failures [{:index 2 :namespaces '[clj-surgeon.a-test]
                                    :exit 137 :log "L" :emitted nil}])]
      (is (str/includes? msg "WITHOUT writing its result"))
      (is (str/includes? msg "137"))))
  (testing "wrote an unreadable result"
    (let [[msg] (bp/lane-failures
                 [{:index 4 :namespaces '[clj-surgeon.a-test] :exit 0 :log "L"
                   :emitted {:clj-surgeon.battery-parallel-runner/unreadable "EOF"}}])]
      (is (str/includes? msg "unreadable")))))

;; ---------------------------------------------------------------------------
;; clause 3 -- the union is where the verdicts are folded
;; ---------------------------------------------------------------------------

(deftest the-union-is-taken-in-manifest-order-whichever-lane-ran-it
  (let [lanes [{:index 0 :emitted {:runs [{:namespace 'clj-surgeon.c-test :elapsed-ms 3}]}}
               {:index 1 :emitted {:runs [{:namespace 'clj-surgeon.a-test :elapsed-ms 1}
                                          {:namespace 'clj-surgeon.b-test :elapsed-ms 2}]}}]
        order '[clj-surgeon.a-test clj-surgeon.b-test clj-surgeon.c-test]]
    (is (= order (mapv :namespace (bp/union-runs lanes order)))
        "the report reads the same whichever lane a namespace landed in")))

(deftest the-lane-budget-is-folded-over-the-union-not-per-lane
  ;; @spec TEST-ISO-007 -- the number the fleet pays is the SUM of the
  ;; namespaces' walls. A child holding a slice would report a five-minute
  ;; lane as forty seconds, which is a budget that can never fire.
  (let [runs (mapv (fn [i] {:namespace (nth (lm/namespaces-for :battery) i)
                            :elapsed-ms 1000000 :counters {} :violations []})
                   (range 3))
        out (with-out-str (binding [*err* *out*] (bp/report! runs [] 12345)))]
    (is (str/includes? out "TEST-ISOLATION: 1 violation")
        "3 x 1 000 000 ms is over the battery lane's 1 800 000 ms budget")
    (is (str/includes? out "3000000 ms, over its 1800000 ms budget"))
    (is (str/includes? out "serial-equivalent total 3000000 ms")
        "the serial-equivalent total is reported, never replaced by the makespan")
    (is (str/includes? out "makespan 12345 ms"))))

;; ---------------------------------------------------------------------------
;; clause 4 -- named summary lines survive the process boundary
;; ---------------------------------------------------------------------------

(deftest a-named-precondition-skip-survives-the-lane-split
  ;; Today's serial receipt: 743 tests, 0 failures, and `1 preconditions
  ;; skipped` naming the command that clears it. The count rides the counters
  ;; (so it merges by addition); the MESSAGE lives in an atom in the lane
  ;; child's JVM, and an atom does not cross a process boundary.
  (let [sw (java.io.StringWriter.)
        _ (binding [clojure.test/*test-out* sw]
            (bp/print-summary! {:test 743 :pass 13756 :fail 0 :error 0
                                :precondition-skipped 1}
                               {:precondition-skipped ["no battery receipt · run `make admit-transaction-recovery-battery`"]}))
        out (str sw)]
    (is (str/includes? out "Ran 743 tests containing 13756 assertions."))
    (is (str/includes? out "0 failures, 0 errors."))
    (is (str/includes? out "1 preconditions skipped."))
    (is (str/includes? out "SKIPPED · no battery receipt")
        "the skip's MESSAGE, not merely its count, reaches the reader")
    (is (str/includes? out "0 preconditions failed.")
        "both buckets print in both states, so zero is visible too")))

(deftest the-note-vars-the-summary-reprints-still-exist
  ;; The coordinator reprints these by resolving a named var in the lane child.
  ;; A rename would make `resolve` return nil and the skip would vanish under a
  ;; PASS -- silently, which is the exact failure this whole clause is for. So
  ;; the names are pinned at the file that owns them.
  (doseq [[k sym] runner/summary-note-vars]
    (let [file (io/file "test" (str (-> (namespace sym)
                                        (str/replace "-" "_")
                                        (str/replace "." "/"))
                                    ".clj"))]
      (is (.exists file) (str k " names a namespace with no source file: " sym))
      (is (str/includes? (slurp file) (str "(def " (name sym)))
          (str "the var " sym " the parallel summary reprints no longer exists"))))
  (is (contains? runner/summary-note-vars :precondition-skipped)
      "the skipped-precondition bucket is the one today's receipt carries"))

;; ---------------------------------------------------------------------------
;; clause 5 -- a declared serial group is never split
;; ---------------------------------------------------------------------------

(deftest a-serial-group-is-never-split-across-lanes
  (let [groups '[[clj-surgeon.a-test clj-surgeon.b-test]]
        us (bp/apply-serial-groups (sort (keys walls)) groups)]
    (is (= 3 (count us)) "the two grouped namespaces became ONE schedulable unit")
    (is (some #(= '[clj-surgeon.a-test clj-surgeon.b-test] %) us))
    (doseq [n [2 3 4 8]]
      (let [plan (bp/partition-lanes us walls n)
            lane-of (into {} (for [[i l] (map-indexed vector plan) x l] [x i]))]
        (is (= (lane-of 'clj-surgeon.a-test) (lane-of 'clj-surgeon.b-test))
            (str "at " n " lanes the group stayed together"))
        (is (= (sort (keys walls)) (sort (mapcat identity plan)))))))
  (testing "the group's cost is the sum, so the packer sees its real size"
    (is (= 410000 (bp/cost-of walls '[clj-surgeon.a-test clj-surgeon.b-test]))))
  (testing "today's declared groups are audited, and empty"
    (is (= [] bp/serial-groups))
    (is (= (count (lm/namespaces-for :battery))
           (count (bp/apply-serial-groups (lm/namespaces-for :battery)
                                          bp/serial-groups))))))

;; ---------------------------------------------------------------------------
;; clauses 6 and 7 -- a bad cost table costs wall, never correctness
;; ---------------------------------------------------------------------------

(deftest an-unmeasured-namespace-is-scheduled-first-and-still-runs
  (let [nss '[clj-surgeon.a-test clj-surgeon.b-test clj-surgeon.unmeasured-test]
        plan (bp/partition-lanes (mapv vector nss) walls 3)]
    (is (= (sort nss) (sort (mapcat identity plan)))
        "an unmeasured namespace still runs -- the table decides placement, not membership")
    (is (= bp/fallback-wall-ms (bp/cost-of walls '[clj-surgeon.unmeasured-test]))
        "and is charged the large fallback, so it is placed first rather than last"))
  (testing "a missing or unreadable walls file degrades, never throws"
    (is (= {} (bp/read-walls "docs/observations/no-such-walls-file.edn")))))

(deftest the-floor-is-the-largest-unit
  (is (= '[clj-surgeon.a-test] (bp/floor-unit units walls)))
  (is (= 400000 (bp/cost-of walls (bp/floor-unit units walls)))
      "no partition of whole units can finish before its largest unit")
  (testing "a serial group raises the floor, because it cannot be split"
    (let [us (bp/apply-serial-groups (sort (keys walls))
                                     '[[clj-surgeon.a-test clj-surgeon.b-test]])]
      (is (= 410000 (bp/cost-of walls (bp/floor-unit us walls)))))))

;; ---------------------------------------------------------------------------
;; the child's half of the contract
;; ---------------------------------------------------------------------------

(deftest the-lane-child-is-asked-for-exactly-its-slice
  (let [cmd (bp/lane-command "-J-Xmx512m" "/tmp/out.edn" '[clj-surgeon.a-test clj-surgeon.b-test])]
    (is (= ["clojure" "-J-Xmx512m" "-M:clj-surgeon/test-deps" "-m"
            "clj-surgeon.mcp-test-runner" "--emit-edn" "/tmp/out.edn" "--ns"
            "clj-surgeon.a-test" "clj-surgeon.b-test"]
           cmd))
    (is (< (.indexOf cmd "--emit-edn") (.indexOf cmd "--ns"))
        "--emit-edn precedes --ns, whose argument is the REST of argv")))

(deftest emit-edn-is-stripped-from-argv-wherever-it-appears
  ;; It has to survive the TEST-ISO-006 re-exec, which forwards the ORIGINAL
  ;; argv, and it must not disturb `--ns`'s rest-of-argv shape.
  (is (= ["/o.edn" ["--ns" "a" "b"]]
         (runner/strip-emit-edn ["--emit-edn" "/o.edn" "--ns" "a" "b"])))
  (is (= ["/o.edn" ["--ns" "a" "b"]]
         (runner/strip-emit-edn ["--ns" "a" "b" "--emit-edn" "/o.edn"])))
  (is (= [nil ["battery"]] (runner/strip-emit-edn ["battery"])))
  (is (= {:emit-edn "/o.edn" :explicit '[a b]}
         (runner/parse-args ["--emit-edn" "/o.edn" "--ns" "a" "b"])))
  (is (= {:emit-edn nil :lanes [:battery]} (runner/parse-args ["battery"]))))

;; ---------------------------------------------------------------------------
;; the deftest-level shards -- the only thing that lowers the floor
;; ---------------------------------------------------------------------------

(deftest every-deftest-of-a-sharded-namespace-lands-in-exactly-one-shard
  (let [vars '[n/a n/b n/c n/d n/e n/f n/g]]
    (doseq [n [1 2 3 7 20]]
      (let [groups (bp/shard-vars vars {} n)
            flat (mapcat identity groups)]
        (is (= (sort vars) (sort flat))
            (str "at " n " shards every deftest still runs, exactly once"))
        (is (= (count flat) (count (set flat))))
        (is (<= (count groups) (min n (count vars)))
            "never more groups than shards asked for, or vars to fill them")
        (is (every? seq groups) "and never an empty shard, which is a wasted JVM")))
    (testing "measured var costs drive the split; the largest var is alone first"
      (let [groups (bp/shard-vars vars '{n/a 400000 n/b 10} 2)]
        (is (some #(= '[n/a] %) groups)
            "the 400 s deftest gets a shard to itself before anything joins it")))))

(deftest a-namespace-with-fixtures-is-never-sharded
  ;; `test-vars` runs :once fixtures around whatever group it is handed, so a
  ;; sharded namespace with a once-fixture runs it once PER SHARD -- a
  ;; different program wearing the same test names.
  (is (nil? (bp/fixture-refusal 'n {})))
  (doseq [[k label] {:clojure.test/once-fixtures "once"
                     :clojure.test/each-fixtures "each"}]
    (let [why (bp/fixture-refusal 'n {k [identity]})]
      (is (some? why) (str "a " label " fixture must refuse the shard"))
      (is (str/includes? why label))
      (is (str/includes? why "once PER SHARD"))))
  (testing "an unloaded namespace is refused rather than guessed at"
    (is (str/includes? (bp/shard-refusal 'clj-surgeon.no-such-namespace-at-all)
                       "is not loaded")))
  (testing "test-ns-hook is a fixture path that namespace metadata does not name"
    (let [n-sym 'clj-surgeon.battery-parallel-hook-probe
          n (create-ns n-sym)
          calls (atom [])]
      (try
        (let [v (intern n (with-meta 'sample {:test #(swap! calls conj :test)}) nil)]
          (intern n 'test-ns-hook (fn [] (swap! calls conj :hook)))
          (is (nil? (bp/fixture-refusal n-sym (meta n)))
              "the metadata-only guard would let this namespace slip")
          (is (str/includes? (bp/shard-refusal n-sym) "test-ns-hook"))
          (clojure.test/test-ns n-sym)
          (is (= [:hook] @calls) "the serial path runs the namespace hook")
          (reset! calls [])
          (runner/test-vars-of n-sym [v])
          (is (= [:test] @calls) "the shard path bypasses it and runs the var"))
        (finally (remove-ns n-sym))))))

(deftest a-sharded-namespace-is-folded-back-into-one-run-before-any-fold
  (let [n 'clj-surgeon.reader-eval-fence-test
        shards [{:namespace n :sharded true :vars '[a b] :elapsed-ms 200000
                 :counters {:test 3 :pass 30 :fail 0 :error 0} :violations []}
                {:namespace n :sharded true :vars '[c] :elapsed-ms 260000
                 :counters {:test 4 :pass 40 :fail 1 :error 0} :violations []}]
        merged (bp/merge-shard-runs n shards)]
    (is (= 7 (:test (:counters merged))) "counters add")
    (is (= 1 (:fail (:counters merged))) "and a failure in ANY shard survives")
    (is (= 460000 (:elapsed-ms merged))
        "the wall is the SUM -- the number a serial run would have measured")
    (is (= 2 (:shards merged)))
    (is (= [] (:violations merged))
        "460 s is inside this namespace's declared 1 000 000 ms override"))
  (testing "one shard is returned untouched"
    (let [r {:namespace 'x :elapsed-ms 1 :counters {} :violations []}]
      (is (= r (bp/merge-shard-runs 'x [r])))))
  (testing "an aggregate shard never flattens prior isolated-var measurements"
    (let [path (io/file (System/getProperty "java.io.tmpdir")
                        (str "battery-var-walls-" (System/nanoTime) ".edn"))]
      (try
        (spit path (pr-str {:var-walls-ms '{n/a 100 n/b 200}}))
        (bp/write-walls!
          (.getPath path) [{:namespace 'n :elapsed-ms 999}]
          [{:emitted {:runs [{:namespace 'n :sharded true :vars '[a b]
                              :elapsed-ms 999}
                             {:namespace 'n :sharded true :vars '[c]
                              :elapsed-ms 30}]}}])
        (let [recorded (:var-walls-ms (edn/read-string (slurp path)))]
          (is (= 100 (get recorded 'n/a)))
          (is (= 200 (get recorded 'n/b)))
          (is (= 30 (get recorded 'n/c))))
        (finally (io/delete-file path true))))))

(deftest the-re-derived-time-budget-is-word-for-word-the-serial-one
  ;; A shard cannot judge the namespace's ceiling, so the coordinator derives
  ;; it from the sum. If the two paths worded it differently a reader would
  ;; have to notice WHICH path produced the line to know what it meant.
  (let [n 'clj-surgeon.mcp-process-test
        over (+ 1 (get iso/lane-default-budget-ms :battery))
        serial (first (iso/budget-violations n {:instant-ns 0}
                                             {:instant-ns (* over 1000000)}
                                             iso/namespace-budget-overrides
                                             (get iso/lane-default-budget-ms :battery)))
        parallel (bp/namespace-budget-violation n over)]
    (is (some? serial))
    (is (= (iso/message serial) (iso/message parallel))
        "the sharded path's refusal is word for word the serial path's"))
  (testing "a declared override is honoured on the SUM, not on a shard"
    (is (nil? (bp/namespace-budget-violation 'clj-surgeon.reader-eval-fence-test 900000))
        "900 s is inside the 1 000 000 ms declared override")
    (is (some? (bp/namespace-budget-violation 'clj-surgeon.reader-eval-fence-test 1000001))))
  (testing "a lane that does not enforce TEST-ISO-007 never gets the line"
    (is (nil? (bp/namespace-budget-violation 'clj-surgeon.no-lane-test 999999999)))))

(deftest the-declared-shard-target-is-the-namespace-that-owns-the-floor
  ;; The declaration is a COST claim, and a cost claim that nobody rechecks is
  ;; how a shard budget outlives its reason.
  (let [walls (bp/read-walls bp/walls-path)]
    (doseq [[n {:keys [shards reason]}] bp/shardable]
      (is (contains? (set (lm/namespaces-for :battery)) n)
          (str n " is declared shardable but is not in the battery lane"))
      (is (and (integer? shards) (> shards 1)))
      (is (and (string? reason) (seq reason)) "a shard count with no reason is a guess")
      (when (seq walls)
        (is (= n (first (apply max-key val walls)))
            (str n " no longer owns the lane's largest wall; re-read the walls "
                 "file before trusting this shard count"))))))

;; ---------------------------------------------------------------------------
;; the prerequisite DAG
;; ---------------------------------------------------------------------------

(deftest the-prerequisite-dag-names-its-target-its-product-and-its-consumers
  (is (str/includes? (slurp (io/file "Makefile")) "BATTERY_PREREQS ?= 1")
      "make test-battery satisfies the prerequisite by default")
  (doseq [{:keys [make-target produces consumers why]} bp/prerequisite-stages]
    (is (re-find (re-pattern (str "(?m)^" make-target ":")) (slurp (io/file "Makefile")))
        (str "`make " make-target "` must be a real target"))
    (is (str/includes? (slurp (io/file "test/clj_surgeon/admit_patch_test.clj")) produces)
        "the product path must be the one the consumer actually reads")
    (is (every? (set (lm/namespaces-for :battery)) consumers)
        "a consumer that is not in the battery lane is a stale edge")
    (is (and (string? why) (seq why)))))

(deftest a-prerequisite-already-satisfied-is-not-re-run
  (is (= [] (bp/pending-prerequisites [{:produces "Makefile"}]))
      "a stage whose product is on disk is evidence, not work to redo")
  (is (= 1 (count (bp/pending-prerequisites [{:produces "no/such/receipt.edn"}])))))

;; ---------------------------------------------------------------------------
;; The three defects run one found, each pinned by the shape that produced it.
;; ---------------------------------------------------------------------------

(deftest a-correctly-sharded-lane-is-not-a-lane-that-ran-less
  ;; RUN 1, 2026-09-08: the battery exited 1 and the ledger recorded :fail
  ;; because this check compared SELECTORS against the NAMESPACES a child
  ;; reports. A lane asked for seven var shards of one namespace correctly
  ;; reports that ONE namespace. The claim is "every namespace this lane was
  ;; given produced a result", and that is a claim about namespaces.
  (let [asked '[clj-surgeon.mission-test
                clj-surgeon.reader-eval-fence-test/a-non-string-paths-entry-never-reaches-io-file
                clj-surgeon.reader-eval-fence-test/the-oracle-names-every-evaluator-it-claims-to-fence]
        lane {:index 1 :namespaces asked :exit 0 :log "L"
              :emitted {:namespaces '[clj-surgeon.mission-test
                                      clj-surgeon.reader-eval-fence-test]}}]
    (is (= [] (bp/lane-failures [lane]))
        "a lane that ran every namespace it was given is not a failure"))
  (testing "and a genuinely short lane still fails, named, in the same shape"
    (let [[msg] (bp/lane-failures
                 [{:index 1 :exit 0 :log "L"
                   :namespaces '[clj-surgeon.mission-test
                                 clj-surgeon.reader-eval-fence-test/the-oracle-names-every-evaluator-it-claims-to-fence]
                   :emitted {:namespaces '[clj-surgeon.mission-test]}}])]
      (is (str/includes? msg "reader-eval-fence-test"))
      (is (not (str/includes? msg "/the-oracle"))
          "the refusal names the NAMESPACE that produced no result, not a selector")))
  (is (= 'clj-surgeon.reader-eval-fence-test
         (bp/selector-namespace 'clj-surgeon.reader-eval-fence-test/a-non-string-paths-entry-never-reaches-io-file)))
  (is (= 'clj-surgeon.mission-test (bp/selector-namespace 'clj-surgeon.mission-test))))

(deftest a-lane-child-reports-that-it-sharded-and-which-vars
  ;; RUN 1: the child computed :sharded and :vars and the EDN writer's
  ;; select-keys dropped both. The coordinator could therefore never re-derive
  ;; a sharded namespace's TEST-ISO-007 budget, and never record a per-var
  ;; cost -- so the schedule could not improve on the next run. A field the
  ;; producer computes and the serialiser discards is invisible in exactly the
  ;; way a passing suite cannot show.
  (let [src (slurp (io/file "test/clj_surgeon/mcp_test_runner.clj"))
        emit (re-find #"(?s):runs \(mapv #\(select-keys % \[[^]]*\]\)" src)]
    (is (some? emit) "the lane child's result writer must be readable here")
    (doseq [k [":namespace" ":counters" ":elapsed-ms" ":violations" ":sharded" ":vars"]]
      (is (str/includes? emit k)
          (str "the lane child must emit " k " -- the coordinator folds over it")))))

(deftest a-lane-of-only-shards-is-still-a-battery-lane-for-home-isolation
  ;; RUN 1 survived this only by luck: every sharded lane happened to also
  ;; carry a whole battery namespace. `lane-of` on a var SELECTOR answers nil,
  ;; so a lane holding only shards of a battery namespace would have been
  ;; launched on a throwaway $HOME (TEST-ISO-006) -- losing ~/.m2 and
  ;; ~/.gitlibs for precisely the cold children that lane exists to drive.
  (let [shard 'clj-surgeon.reader-eval-fence-test/no-real-launcher-evaluates-a-build-file-it-discovers]
    (is (nil? (lm/lane-of shard))
        "a var selector is in no manifest -- which is the trap")
    (is (= :battery (lm/lane-of (runner/selector-namespace shard)))
        "read through the selector and the lane is battery")
    (is (str/includes? (slurp (io/file "test/clj_surgeon/mcp_test_runner.clj"))
                       "(lm/lane-of (selector-namespace %))")
        "the home-isolation decision must read through the selector")))
