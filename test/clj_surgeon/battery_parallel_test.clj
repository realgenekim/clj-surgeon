(ns clj-surgeon.battery-parallel-test
  "@spec TEST-ISO-013 -- the witnesses for the battery lane run wide.

   Every one of these is a fold over DATA: the scheduler, the lane-failure
   classifier, the union, and the summary renderer are pure, so the ways a
   parallel suite silently runs LESS than a serial one are all provable here,
   in the fast lane, with no JVM launched and no minutes spent."
  {:lane :fast}
  (:require
   [clj-surgeon.battery-parallel-runner :as bp]
   [clj-surgeon.gate-memory :as mem]
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
  ;; @spec TEST-ISO-016 -- changing runtime preserves the original runner inventory.
  (is (= ["clojure" "-J-Xmx512m" "-M:clj-surgeon/test-deps" "-m" "run-all"
          "--emit-edn" "receipt.edn" "--ns" "clj-surgeon.intent-transaction-test"]
         (bp/lane-command "-J-Xmx512m" "receipt.edn" '[clj-surgeon.intent-transaction-test])))
  (is (= "clj-surgeon.mcp-test-runner"
         (nth (bp/lane-command "-J-Xmx512m" "receipt.edn" '[clj-surgeon.splice-envelope-test]) 4)))
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

(defn complete-emission [namespaces]
  {:namespaces namespaces
   :runs (mapv (fn [n] {:namespace n :counters {:test 1 :pass 1 :fail 0 :error 0}
                        :elapsed-ms 1 :violations []}) namespaces)
   :result {:test (count namespaces) :pass (count namespaces) :fail 0 :error 0}
   :leak-fail 0})

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
                  :emitted (complete-emission '[clj-surgeon.a-test])}])))))

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
  (testing "NEW bb budget is enforced even without JVM isolation; span is separate"
    (with-redefs [lm/namespace-runtimes {'example.bb-test :bb}]
      (doseq [[wall expected] [[374149 0] [374150 1]]]
        (let [result (atom nil)
              out (with-out-str
                    (binding [*err* *out*]
                      (reset! result
                              (bp/report! [{:namespace 'example.bb-test :elapsed-ms wall :violations []}]
                                          [{:index 0 :runtime :bb :started-ms 100 :completed-ms 120
                                            :wall-ms 20 :exit 0 :namespaces ['example.bb-test]}]
                                          25 false))))]
          (is (= expected @result))
          (is (str/includes? out (str "bb-runtime: serial-equivalent " wall " ms; budget 374149 ms; makespan 20 ms")))
          (when (pos? expected)
            (is (str/includes? out "bb lane took 374150 ms, over its 374149 ms budget")))))))
  ;; @spec TEST-ISO-007 -- the number the fleet pays is the SUM of the
  ;; namespaces' walls. A child holding a slice would report a five-minute
  ;; lane as forty seconds, which is a budget that can never fire.
  (let [runs (mapv (fn [i] {:namespace (nth (lm/namespaces-for :battery) i)
                            :elapsed-ms 1000000 :counters {} :violations []})
                   (range 3))
        out (with-out-str (binding [*err* *out*]
                            (with-redefs [lm/namespace-runtimes {}]
                              (bp/report! runs [] 12345))))]
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

;; @spec TEST-ISO-014
(deftest launcher-matrix-cells-remain-independently-shardable
  (let [n 'clj-surgeon.reader-eval-fence-test
        units (bp/shard-units [n] bp/shardable {} {})]
    (is (nil? (bp/shard-refusal n)) "no fixtures or test-ns-hook")
    (is (every? #(= 1 (count %)) units) "each deftest is an independent unit")
    (is (= (set (bp/test-var-names n)) (set (map first units)))
        "the shard allowance covers the whole loaded namespace")))

;; @spec TEST-ISO-014
(deftest grouped-shards-retain-measured-per-deftest-walls
  (testing "new cells have a namespace-share estimate before their first run"
    (is (= 100 (bp/unit-cost
                 '{clj-surgeon.reader-eval-fence-test 1300} {}
                 '[clj-surgeon.reader-eval-fence-test/new-cell]))
        "unmeasured cells must not pack behind measured work at a token 1 ms")
    (is (= 37 (bp/unit-cost
                '{clj-surgeon.reader-eval-fence-test 1300}
                '{clj-surgeon.reader-eval-fence-test/new-cell 37}
                '[clj-surgeon.reader-eval-fence-test/new-cell]))))
  (let [n-sym (gensym "measured-shard-")
        n (create-ns n-sym)
        a (intern n (with-meta 'a {:test #(is true)}) (fn []))
        b (intern n (with-meta 'b {:test #(is false)}) (fn []))
        measured (atom {})
        result (atom nil)]
    (try
      (binding [clojure.test/*test-out* (java.io.StringWriter.)]
        (reset! result (runner/test-vars-of n-sym [a b] measured)))
      (is (= {:test 2 :pass 1 :fail 1 :error 0} @result)
          "timing a grouped shard preserves its failures and counters")
      (is (= #{'a 'b} (set (keys @measured))))
      (is (every? #(and (integer? %) (not (neg? %))) (vals @measured)))
      (finally (remove-ns n-sym))))
  (let [path (io/file (System/getProperty "java.io.tmpdir")
                      (str "battery-cell-walls-" (System/nanoTime) ".edn"))]
    (try
      (bp/write-walls!
        (.getPath path) [{:namespace 'n :elapsed-ms 999}]
        [{:emitted {:runs [{:namespace 'n :sharded true :vars '[a b]
                            :elapsed-ms 999 :var-walls-ms '{a 80 b 900}}]}}])
      (is (= '{n/a 80 n/b 900}
             (:var-walls-ms (edn/read-string (slurp path))))
          "measured individual walls survive a grouped shard without averaging")
      (finally (io/delete-file path true)))))

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
              :emitted (complete-emission '[clj-surgeon.mission-test
                                            clj-surgeon.reader-eval-fence-test])}]
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
    (doseq [k [":namespace" ":counters" ":elapsed-ms" ":violations" ":sharded" ":vars" ":var-walls-ms"]]
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

;; @spec TEST-ISO-015
(deftest gate-width-is-resource-bounded
  (let [width (requiring-resolve 'clj-surgeon.battery-parallel-runner/gate-width)]
    (is (= 8 (width 16 32768)))
    (is (= 11 (width 64 20000)))
    (is (= 2 (width 4 32768)))
    (is (= 1 (width 1 4096)))
    (is (= 2 (width 64 5120)))
    (is (thrown? clojure.lang.ExceptionInfo (width 16 2048)))))

;; @spec TEST-ISO-015
;; THE FLOOR IS PUBLISHED, NOT DISCOVERED BY REFUSAL. The skiff's operator was
;; told the gate could not read his memory, granted GATE_MEMAVAIL_MIB=3072 --
;; twice a lane's charge, an honest guess -- and was bounced with
;; `{:memory-mib 3072 :required-mib 3584}` and no statement of where 3584 came
;; from or how much to grant instead. One constant now feeds the refusal, the
;; preflight and docs/install/skiff.md.
(deftest the-single-lane-floor-is-named-with-its-formula-and-its-override
  (let [width (requiring-resolve 'clj-surgeon.battery-parallel-runner/gate-width)]
    (testing "the floor IS the formula, so it cannot be a stale literal"
      (is (= 3584 mem/single-lane-floor-mib))
      (is (= mem/single-lane-floor-mib (+ mem/reserve-mib mem/lane-charge-mib))))
    (testing "one MiB below the floor refuses; the floor itself opens one lane"
      (is (thrown? clojure.lang.ExceptionInfo (width 16 (dec mem/single-lane-floor-mib))))
      (is (= 1 (width 16 mem/single-lane-floor-mib))))
    (testing "the refusal names the grant, the floor, the formula and the override"
      (let [error (try (width 16 3072) (catch clojure.lang.ExceptionInfo e e))
            message (ex-message error)]
        (is (str/includes? message "3072 MiB available"))
        (is (str/includes? message mem/floor-note))
        (is (str/includes? message "GATE_MEMAVAIL_MIB"))
        (is (= {:memory-mib 3072 :required-mib 3584 :reserve-mib 2048 :lane-charge-mib 1536}
               (select-keys (ex-data error) [:memory-mib :required-mib :reserve-mib :lane-charge-mib])))))
    (testing "the preflight line carries the floor exactly when the gate would refuse"
      (with-redefs [mem/available-mib (fn [] 3072)]
        (let [line (mem/preflight-line)]
          (is (str/starts-with? line "BELOW-FLOOR 3072 MiB available"))
          (is (str/includes? line mem/floor-note))))
      (with-redefs [mem/available-mib (fn [] 3584)]
        (is (str/starts-with? (mem/preflight-line) "OK 3584 MiB available"))))
    (testing "the python semaphore refuses in the coordinator's own words"
      (let [python (slurp (io/file "test/gate_slot.py"))]
        (is (str/includes? python "RESERVE_MIB = 2048"))
        (is (str/includes? python "LANE_CHARGE_MIB = 1536"))
        (is (str/includes? python "MiB floor = reserve %d + %d per lane"))))))

;; @spec TEST-ISO-015
;; ONE READER -- the darwin arithmetic, witnessed on the linux box that has
;; the suite. The skiff (2026-09-10) refused `available memory is unknown
;; {:os "Mac OS X"}` while its own preflight had just reported the reader
;; green. Two defects were behind that one line and BOTH are pinned here and
;; in `test/gate_memory_one_reader_test.sh`:
;;   1. the preflight never called a reader (it checked that `vm_stat` EXISTED);
;;   2. the reader could not have worked if it had -- it handed babashka's
;;      process API a COLLECTION where that API takes varargs, so it launched a
;;      program named `(vm_stat)`. Linux never noticed, because linux answers
;;      from /proc/meminfo and never reaches the shell-out at all.
;;
;; (Spelled "babashka's process API" and not the namespace: this is a FAST-lane
;;  namespace, and `no-fast-lane-namespace-spells-a-child-process` reads the
;;  source for that spelling. It is right to -- a fast-lane test must launch no
;;  child -- and it caught this comment on the first full run.)
(deftest darwin-memory-reader-is-the-documented-arithmetic
  (let [stat (str "Mach Virtual Memory Statistics: (page size of 16384 bytes)\n"
                  "Pages free:                              300000.\n"
                  "Pages active:                            111111.\n"
                  "Pages inactive:                          100000.\n"
                  "Pages speculative:                        20000.\n"
                  "Pages throttled:                              0.\n"
                  "Pages wired down:                        222222.\n"
                  "Pages purgeable:                          10000.\n")]
    (testing "free + inactive + speculative + purgeable, at the reported page size"
      ;; 430 000 pages x 16 KiB = 7 045 120 000 B = 6718 MiB. `Pages active`
      ;; and `Pages wired down` are NOT reclaimable and must not be counted.
      (is (= 6718 (mem/darwin-mib-from stat 68719476736))))
    (testing "hw.memsize caps it, so a misparse cannot invent capacity"
      (is (= 1024 (mem/darwin-mib-from stat (* 1024 1024 1024)))))
    (testing "a 4 KiB page box is read at ITS page size, not at a constant"
      (is (= 1679 (mem/darwin-mib-from (str/replace stat "16384" "4096") 68719476736))))
    (testing "no reclaimable class parsed is a TYPED refusal naming the step"
      (let [error (try (mem/darwin-mib-from "Mach Virtual Memory Statistics:\n" 1)
                       (catch clojure.lang.ExceptionInfo e e))]
        (is (= "gate-refused: available memory is unknown" (ex-message error)))
        (is (= {:source :darwin :step "vm_stat" :reason :no-reclaimable-classes}
               (select-keys (ex-data error) [:source :step :reason])))
        (is (str/includes? (:remedy (ex-data error)) "GATE_MEMAVAIL_MIB")
            "every refusal names the operator override")))))

;; @spec TEST-ISO-015
(deftest memory-refusals-name-the-step-that-could-not-answer
  (testing "/proc/meminfo without MemAvailable refuses by REASON, not by platform"
    (let [error (try (mem/linux-mib-from "MemTotal:  16384 kB\n")
                     (catch clojure.lang.ExceptionInfo e e))]
      (is (= {:source :linux :step "/proc/meminfo" :reason :no-memavailable-line}
             (select-keys (ex-data error) [:source :step :reason])))))
  (testing "the preflight line is the reader's own answer, never a source name"
    (with-redefs [mem/available-mib (fn [] (throw (ex-info "gate-refused: available memory is unknown"
                                                    {:step "vm_stat"})))]
      (let [line (mem/preflight-line)]
        (is (str/starts-with? line "REFUSED gate-refused: available memory is unknown"))
        (is (str/includes? line ":step \"vm_stat\""))))
    (with-redefs [mem/available-mib (fn [] 4096)]
      (is (str/starts-with? (mem/preflight-line) "OK 4096 MiB available")))))

;; @spec TEST-ISO-015
(deftest gate-census-rejects-every-loss-and-duplicate
  (let [problems (requiring-resolve 'clj-surgeon.battery-parallel-runner/census-problems)
        expected '[a b c]]
    (is (empty? (problems expected expected)))
    (doseq [observed ['[] '[a] '[b c] '[a b] '[a b c c] '[a b c d] '[a a c]]]
      (is (seq (problems expected observed)) (str observed)))))

;; @spec TEST-ISO-015
(deftest gate-parity-is-per-namespace
  (let [delta (requiring-resolve 'clj-surgeon.battery-parallel-runner/parity-delta)
        a {'a {:test 2 :pass 3 :fail 0 :error 0}
           'b {:test 1 :pass 4 :fail 0 :error 0}}]
    (is (empty? (delta a a)))
    (is (seq (delta a (assoc-in a ['a :pass] 4))))
    (is (seq (delta a (assoc a 'a (a 'b) 'b (a 'a)))))
    (is (seq (delta a (dissoc a 'a))))))

;; @spec TEST-ISO-015
(deftest serial-cannot-authorize-a-landing
  (let [eligible (requiring-resolve 'clj-surgeon.battery-parallel-runner/landing-eligible?)]
    (is (true? (eligible false [])))
    (is (false? (eligible true [])))
    (is (false? (eligible false [:lost-shard])))))

;; @spec TEST-ISO-015 -- Sol GATE-LANES-FENCE-002, omitted alias/audit prewarm.
(deftest prewarm-membership-and-authority-are-explicit
  (let [select-default (requiring-resolve 'run-all/default-namespaces)]
    (is (= '[a c] (select-default '[a b c] '{a :bb b :jvm c :bb} :bb)))
    (is (= '[b] (select-default '[a b c] '{a :bb b :jvm c :bb} :jvm)))
    (is (= [] (select-default '[unknown] {} :bb))))
  (let [stages (requiring-resolve 'clj-surgeon.battery-parallel-runner/gate-stages)
        full (stages false false)
        warm (stages false true)]
    (is (= ["admit-transaction-recovery-battery" "battery-fresh"
            "alias-migration-test" "mcp-test" "test-bb"
            "test-bb-diagnostic" "repository-hygiene" "intent-audit"] (mapv :target full)))
    (is (= (filterv #(not= "battery-fresh" (:target %)) full) warm))
    (is (= (count full) (count (set (map :target full)))))
    (is (= (mapv :target full) (bp/gate-targets false)))
    (is (false? (bp/landing-eligible? false true [])))
    (is (true? (bp/landing-eligible? false false [])))
    (is (false? (bp/landing-eligible? true true [])))))

;; @spec TEST-ISO-015 -- Sol GATE-LANES-FENCE-001, every runtime shares flock.
(deftest worker-admission-wraps-argv-without-shell-interpolation
  (let [command (requiring-resolve 'clj-surgeon.battery-parallel-runner/slot-command)]
    (doseq [argv [["java" "-Xmx512m"] ["bb" "task.clj"]
                  ["make" "mcp-test-checks"] ["sh" "-c" "exit 7"]]]
      (let [wrapped (command argv)]
        (is (= ["python3" "-B"] (subvec wrapped 0 2)))
        (is (.endsWith ^String (nth wrapped 2) "/test/gate_slot.py"))
        (is (= (into ["--"] argv) (subvec wrapped 3)))))))

;; @spec TEST-ISO-015 -- round-three prewarm parent/child evidence path regression.
(deftest prewarm-pool-stores-evidence-under-the-parent-run
  (let [seen (atom nil)]
    (with-redefs [bp/prepare-suite! (fn [opts]
                                      (reset! seen opts)
                                      (throw (ex-info "captured preparation" {})))]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"captured preparation"
            (bp/run-gate-pool! {"--prewarm" "true"} "run-id" {:lanes 8}
              "target/gate-prewarm/run-id"))))
    (is (= "target/gate-prewarm/run-id/alias" (get @seen "--work-dir")))))

;; @spec TEST-ISO-015
(deftest gate-refuses-malformed-and-lost-child-facts
  (let [facts (complete-emission '[a b])
        lane {:index 0 :namespaces '[a b] :exit 0 :log "child.log" :emitted facts}]
    (is (empty? (bp/lane-failures [lane])))
    (doseq [bad [(dissoc facts :runs) (assoc facts :runs [])
                 (update facts :runs conj (first (:runs facts)))
                 (assoc-in facts [:result :test] 1)
                 (assoc-in facts [:runs 0 :counters :pass] -1)
                 (assoc-in facts [:runs 0 :expected-vars] '[missing-test])
                 (assoc facts :leak-fail nil)]]
      (is (seq (bp/lane-failures [(assoc lane :emitted bad)]))))
    (is (seq (bp/lane-failures [(assoc lane :exit 7)])))
    (is (seq (bp/lane-failures [(assoc lane :emitted nil)])))))

;; @spec TEST-ISO-015
(deftest gate-preserves-fast-before-integration
  (let [lanes {'f1 :fast 'f2 :fast 'i1 :integration 'i2 :integration}]
    (is (= '[[f2 f1] [i2 i1]] (bp/gate-phases '[i2 f2 i1 f1] lanes)))
    (is (= '[[f1 f2] [i1 i2]] (bp/gate-phases '[f1 f2 i1 i2] lanes)))
    (is (= [[] []] (bp/gate-phases [] lanes)))))

;; @spec TEST-ISO-015 -- fresh clone 2026-09-09: two of eight CLI starts
;; read a concurrently written .cpcache and lost clojure.main before any test.
(deftest a-cold-checkout-prepares-the-worker-classpath
  (let [command (requiring-resolve 'clj-surgeon.battery-parallel-runner/worker-preparation-command)
        expected ["clojure" "-J-Xms64m" "-J-Xmx512m" "-Spath" "-M:clj-surgeon/test-deps"]]
    (is (= expected (command [{:runtime :jvm}])))
    (is (= expected (command [{:runtime :bb} {:runtime :jvm} {:runtime :jvm}])))
    (is (nil? (command [{:runtime :bb}])))
    (is (nil? (command [])))))

;; @spec TEST-ISO-015 -- competing suites consume one shared width budget.
(deftest gate-pool-overlaps-suites-without-multiplying-width
  (let [pool (requiring-resolve 'clj-surgeon.battery-parallel-runner/run-pool!)
        started (java.util.concurrent.CountDownLatch. 3)
        active (atom 0)
        peak (atom 0)
        seen (atom #{})
        jobs [{:suite "alias"} {:suite "mcp"} {:suite "bb"}]
        result (pool jobs 3
                     (fn [{:keys [suite] :as job}]
                       (swap! peak max (swap! active inc))
                       (swap! seen conj suite)
                       (.countDown started)
                       (try
                         (assoc job :overlapped? (.await started 10 java.util.concurrent.TimeUnit/SECONDS))
                         (finally (swap! active dec)))))]
    (is (= 3 @peak))
    (is (= #{"alias" "mcp" "bb"} @seen))
    (is (every? :overlapped? result))
    (is (= jobs (mapv #(dissoc % :overlapped?) result)))
    (is (= 0 @active))
    (is (= (vec (range 11)) (pool (range 11) 1 identity)))
    (let [completed (atom [])]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"pool job failed"
            (bp/run-pool! (range 3) 1
                          (fn [n]
                            (swap! completed conj n)
                            (when (zero? n) (throw (ex-info "failed job" {})))))))
      (is (= [0 1 2] @completed) "a failed job cannot abandon its queued siblings"))
    (is (thrown? clojure.lang.ExceptionInfo (bp/run-pool! [] 0 identity)))))
