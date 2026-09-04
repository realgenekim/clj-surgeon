## NO-GO

1. **BLOCKING — the advertised hashed/measured partition is not the only route by which measured receipt data enters the hash subject.** `clj-surgeon.measured/hashed-channel` removes only a map entry whose key is exactly `:measured` (`src/clj_surgeon/measured.clj:50`, `src/clj_surgeon/measured.clj:66-93`). The shared MCP operation finalizer measures wall time and associates it directly as top-level `:elapsed_ms` (`src/clj_surgeon/mcp_operation.clj:31-39`, clock at `src/clj_surgeon/mcp_operation.clj:56`). A real public result containing a receipt therefore retains the measured value after projection. This violates the stated ruling, “a measured wall-clock field can never live inside a parity hash,” and the review brief explicitly makes any such value blocking.

   Exact command:

   ```sh
   ~/bin/suite-run bb -e "(require '[clj-surgeon.mcp-operation :as op] '[clj-surgeon.measured :as measured]) (let [ticks (atom [1000000 3500000]) seen (atom nil)] (op/invoke! {:clock-nanos #(let [x (first @ticks)] (swap! ticks subvec 1) x) :execute (fn [] {:ok true :receipt {:stable :fact}}) :summarize (constantly \"ok\") :serialize pr-str :callback (fn [_ _ result] (reset! seen result))}) (prn {:public-result @seen :hashed-channel (measured/hashed-channel @seen) :elapsed-survives-hash (contains? (measured/hashed-channel @seen) :elapsed_ms)}))"
   ```

   Verbatim output:

   ```text
   {:public-result {:ok true, :receipt {:stable :fact}, :elapsed_ms 2.5}, :hashed-channel {:ok true, :receipt {:stable :fact}, :elapsed_ms 2.5}, :elapsed-survives-hash true}
   [exit_code=0]
   ```

   The journal has the same class of leak: it measures `:window-ns` with `System/nanoTime` at `src/clj_surgeon/txn_journal.clj:2270` and returns it as `[:commit-window :max-ns]` at `src/clj_surgeon/txn_journal.clj:2356`, outside `:measured`.

   Exact command:

   ```sh
   rg -n 'elapsed_ms|clock-nanos|System/nanoTime' src/clj_surgeon/mcp_operation.clj
   rg -n 'defn hashed-channel|measured-key|dissoc measured-key' src/clj_surgeon/measured.clj
   rg -n ':window-ns|:commit-window|:max-ns' src/clj_surgeon/txn_journal.clj
   ```

   Verbatim output:

   ```text
   39:    (assoc domain-result :elapsed_ms elapsed-ms)))
   55:  [{:keys [clock-nanos execute summarize serialize callback]
   56:    :or {clock-nanos #(System/nanoTime)
   58:  (let [started-ns (clock-nanos)
   60:        finished-ns (clock-nanos)
   50:(def measured-key
   64:  {measured-key m})
   66:(defn hashed-channel
   80:                 (if (= k measured-key)
   67:   receipt's own `:max-ns` over nine commits after a five-commit warmup:
   105:   :commit-window commit-window
   2227:   Returns {:ok true :window-ns n :reread? b}, {:conflict-refusal m}, or
   2270:                       :window-ns (- (System/nanoTime) opened)
   2356:                      :commit-window (assoc commit-window :max-ns window-ns)
   2411:                                  (max window-ns (long (:window-ns outcome 0)))
   ```

   The wider write-site sweep was `rg -n 'System/(nanoTime|currentTimeMillis)|:scan_ms|:elapsed_ms|:wall[_-]ms|:window-ns|:max-ns|:created-at|:updated-at|:expires-at|:started-at|:finished-at|:uptime-ms' src/clj_surgeon --glob '*.clj'`. Its receipt-reachable findings divide as follows: only `parse_admission.clj:410` places `scan_ms` under the partition; `mcp_operation.clj:39`, `mcp_change_buffer.clj:1276-1539`, `mcp_cold_verify.clj:128,162`, and `mcp_hot_verify.clj:97,114` publish measured `elapsed_ms` outside it; `txn_journal.clj:2270,2356` publishes the measured commit window outside it. Battery `wall-ms` is harness data, while process expiry, snapshot age, telemetry retention, onboarding deadlines, and journal lifecycle timestamps are control/state metadata rather than parity receipts. Persisted cold-verification receipts are a concrete reachable case: `public-job` removes creation/update clocks but retains `elapsed_ms` (`src/clj_surgeon/mcp_cold_verify.clj:46-56,128,162`).

   Verbatim receipt-reachable rows from that sweep:

   ```text
   src/clj_surgeon/mcp_change_buffer.clj:1264:   (let [started (System/nanoTime)]
   src/clj_surgeon/mcp_change_buffer.clj:1276:          :elapsed_ms (/ (double (- (System/nanoTime) started)) 1000000.0)
   src/clj_surgeon/mcp_cold_verify.clj:49:      (dissoc :workspace-root :created-at-ms :updated-at-ms :receipt-file :job)
   src/clj_surgeon/mcp_cold_verify.clj:128:                    :elapsed_ms (:elapsed_ms process)
   src/clj_surgeon/mcp_cold_verify.clj:162:             :elapsed_ms (/ (double (- (System/nanoTime) started)) 1000000.0)
   src/clj_surgeon/mcp_operation.clj:39:    (assoc domain-result :elapsed_ms elapsed-ms)))
   src/clj_surgeon/mcp_hot_verify.clj:97:                   :elapsed_ms (/ (double (- (System/nanoTime) started))
   src/clj_surgeon/mcp_hot_verify.clj:114:               :elapsed_ms (/ (double (- (System/nanoTime) started))
   src/clj_surgeon/txn_journal.clj:2270:                       :window-ns (- (System/nanoTime) opened)
   src/clj_surgeon/txn_journal.clj:2356:                      :commit-window (assoc commit-window :max-ns window-ns)
   src/clj_surgeon/parse_admission.clj:410:      {:scan_ms (if meter
   ```

   There is also a namespace collision in the converse direction: parser admission already uses `:measured` for deterministic node/depth counts (`src/clj_surgeon/parse_admission.clj:506`). The global projector silently drops those deterministic facts. This is not the blocking leak above, but it disproves the claim that `:measured` is an unambiguous repository-wide partition.

   Exact command:

   ```sh
   ~/bin/suite-run bb -e "(require '[clj-surgeon.parse-admission :as admission] '[clj-surgeon.measured :as measured]) (let [src (str \"(\" (apply str (repeat 150 \"(\")) \"x\" (apply str (repeat 151 \")\"))) r (admission/refusal \"/tmp/mem003-fx/tower.clj\" src)] (prn {:raw-measured (:measured r) :hashed-measured (:measured (measured/hashed-channel r)) :raw-observed (:observed r) :hashed-observed (:observed (measured/hashed-channel r))}))"
   ```

   Verbatim output:

   ```text
   {:raw-measured {:parse-nodes 152, :parse-depth 151}, :hashed-measured nil, :raw-observed 151, :hashed-observed 151}
   [exit_code=0]
   ```

   The narrower structure-sharing claim is reproduced and correct on an actual `run-ls-tree` EDN result: untouched records/forms remain identical while only the enclosing result/receipt path is rebuilt.

   Exact command:

   ```sh
   ~/bin/suite-run bb -e "(require '[clj-surgeon.core :as core] '[clj-surgeon.measured :as measured]) (let [r (core/run-ls-tree {:dir \"/tmp/mem003-fx/structure-tree\" :format :edn}) h (measured/hashed-channel r)] (prn {:result-identical (identical? r h) :first-record-identical (identical? (first r) (first h)) :forms-identical (identical? (:forms (first r)) (:forms (first h))) :receipt-identical (identical? (last r) (last h)) :raw-scan-ms (get-in (last r) [:receipt :resources :measured :scan_ms]) :hashed-scan-ms (get-in (last h) [:receipt :resources :measured :scan_ms]) :hashed-bytes (get-in (last h) [:receipt :resources :bytes_scanned])}))"
   ```

   Verbatim output:

   ```text
   {:result-identical false, :first-record-identical true, :forms-identical true, :receipt-identical false, :raw-scan-ms 1.57, :hashed-scan-ms nil, :hashed-bytes 36}
   [exit_code=0]
   ```

2. **The MEM-003 RED/GREEN witness is genuine, and deleting the new meter does make it red.** At `d273d448`, the focused witness produced exactly five failures; at `d66b5683`, the same 12 assertions passed. `clojure.test/run-tests` itself returns a summary rather than exiting nonzero, hence the shell exit 0 at the red SHA.

   Exact command at each checkout:

   ```sh
   ~/bin/suite-run bb -e "(require 'clj-surgeon.measured-channel-test) (clojure.test/run-tests 'clj-surgeon.measured-channel-test)"
   ```

   Verbatim output at `d273d448`:

   ```text
   Testing clj-surgeon.measured-channel-test

   Ran 3 tests containing 12 assertions.
   5 failures, 0 errors.
   {:test 3, :pass 7, :fail 5, :error 0, :type :summary}
   [exit_code=0]
   ```

   Verbatim output at `d66b5683`:

   ```text
   Testing clj-surgeon.measured-channel-test

   Ran 3 tests containing 12 assertions.
   0 failures, 0 errors.
   {:test 3, :pass 12, :fail 0, :error 0, :type :summary}
   [exit_code=0]
   ```

   For the sabotage, I removed only the `measured/measured {:scan_ms ...}` half of `meter-resources` in the disposable checkout `/tmp/mem003-fx/meter_sabotage` at `d66b5683`, retaining `bytes_scanned`.

   Exact command:

   ```sh
   cd /tmp/mem003-fx/meter_sabotage && ~/bin/suite-run bb -e "(require 'clj-surgeon.measured-channel-test) (clojure.test/run-tests 'clj-surgeon.measured-channel-test)"
   ```

   Verbatim output:

   ```text
   Testing clj-surgeon.measured-channel-test

   FAIL in (the-hashed-channel-drops-the-measured-fields-and-nothing-else) (/tmp/mem003-fx/meter_sabotage/test/clj_surgeon/measured_channel_test.clj:147)
   the fix is a partition, not a smaller hash
   the unprojected result no longer carries the meter at all
   expected: (str/includes? (pr-str (last a)) "scan_ms")
     actual: (not (str/includes? "{:receipt {:resources {:bytes_scanned 1520}}}" "scan_ms"))

   FAIL in (two-scans-of-an-unchanged-tree-agree-on-the-hashed-channel) (/tmp/mem003-fx/meter_sabotage/test/clj_surgeon/measured_channel_test.clj:106)
   a wall-clock reading may not live inside a value another row hashes
   the receipt publishes no measured scan cost at all
   expected: (number? (measured-scan-ms a))
     actual: (not (number? nil))

   FAIL in (two-scans-of-an-unchanged-tree-agree-on-the-hashed-channel) (/tmp/mem003-fx/meter_sabotage/test/clj_surgeon/measured_channel_test.clj:106)
   a wall-clock reading may not live inside a value another row hashes
   the scan really ran and the clock really measured it
   expected: (pos? (or (measured-scan-ms a) 0))
     actual: (not (pos? 0))

   Ran 3 tests containing 12 assertions.
   3 failures, 0 errors.
   {:test 3, :pass 9, :fail 3, :error 0, :type :summary}
   [exit_code=0]
   ```

3. **The streaming merge now has a load-bearing meter witness; the nil-limit and discovery hardening work, but the requested global “no `System/exit`” property is false.** The ordinary-scan witness is at `test/clj_surgeon/ls_tree_memory_test.clj:174-203`; its anti-vacuity assertion at line 196 fails when the streaming encoder drops the measured block. The meter is threaded through `stream-outlines!`, `encode-page`, and both encoders in `src/clj_surgeon/core.clj:792-975,1083-1110`.

   I sabotaged only the streaming `encode-page` handoff in `/tmp/mem003-fx/stream_sabotage`, changing it to pass `(dissoc (admission/meter-resources meter) measured/measured-key)`.

   Exact command:

   ```sh
   cd /tmp/mem003-fx/stream_sabotage && ~/bin/suite-run bb -e "(require 'clj-surgeon.ls-tree-memory-test) (clojure.test/run-tests 'clj-surgeon.ls-tree-memory-test)"
   ```

   Verbatim failing output:

   ```text
   Testing clj-surgeon.ls-tree-memory-test

   FAIL in (streaming-and-batch-encoders-agree-over-this-repository) (/tmp/mem003-fx/stream_sabotage/test/clj_surgeon/ls_tree_memory_test.clj:174)
   every result under the ceiling is byte-identical to the batch path
   src: the meter is dark — nothing was published to project
   expected: (some? (get-in (last actual-edn) [:receipt :resources :measured :scan_ms]))
     actual: (not (some? nil))

   FAIL in (streaming-and-batch-encoders-agree-over-this-repository) (/tmp/mem003-fx/stream_sabotage/test/clj_surgeon/ls_tree_memory_test.clj:174)
   every result under the ceiling is byte-identical to the batch path
   test: the meter is dark — nothing was published to project
   expected: (some? (get-in (last actual-edn) [:receipt :resources :measured :scan_ms]))
     actual: (not (some? nil))
   ── outline concurrency: measured peak 18, declared pool 18, window 72

   Ran 4 tests containing 21 assertions.
   2 failures, 0 errors.
   {:test 4, :pass 19, :fail 2, :error 0, :type :summary}
   [exit_code=0]
   ```

   The `find` invocations retain `-H` and `-print0` (`src/clj_surgeon/core.clj:261-267,365-372`), the typed root refusal remains at `src/clj_surgeon/core.clj:1039`, and the MEM-005 `StackOverflowError` catch remains at `src/clj_surgeon/core.clj:546`. The streaming text encoder’s nil-limit branch is at `src/clj_surgeon/core.clj:907-921` and completed in a direct injected stack-overflow test rather than calling `format "%d"` on nil.

   Exact command:

   ```sh
   ~/bin/suite-run bb -e "(require '[clj-surgeon.core :as core] '[clj-surgeon.parse-admission :as admission] '[clojure.string :as str]) (let [real @#'core/safe-outline out (with-redefs-fn {#'core/safe-outline (fn [f] (if (str/ends-with? (str f) \"overflow.clj\") (admission/stack-overflow-refusal f) (real f)))} #(core/run-ls-tree {:dir \"/tmp/mem003-fx/nil-limit-tree\"}))] (prn {:completed true :named-overflow (str/includes? out \"stack_overflow_during_parse\") :has-limit-word (str/includes? out \"stack_overflow_during_parse limit\")}) (println out))"
   ```

   Verbatim output:

   ```text
   {:completed true, :named-overflow true, :has-limit-word false}
   src/fixture/ok.clj  2 lines, 1 forms
     ns: fixture.ok
     2: defn hello []

   src/fixture/overflow.clj  0 lines, 0 forms

   ── total: 2 files, 1 forms
   ── parser_admission_refused: 1 file(s)
      src/fixture/overflow.clj  stack_overflow_during_parse
   ── resources: bytes_scanned 36
   ── measured (not hashed): scan_ms 1.472

   [exit_code=0]
   ```

   Root refusal also returns without killing the caller:

   ```sh
   ~/bin/suite-run bb -e "(require '[clj-surgeon.core :as core]) (prn (core/run-ls-tree {:dir \"/tmp/mem003-fx/does-not-exist\" :format :edn})) (println :survived)"
   ```

   ```text
   {:error ":ls-tree :dir must be an existing directory: \"/tmp/mem003-fx/does-not-exist\"", :error-type :workspace-root-not-a-directory, :dir "/tmp/mem003-fx/does-not-exist", :next-action "pass_an_existing_directory_path"}
   :survived
   [exit_code=0]
   ```

   However, the literal source grep requested by the brief does not say “no `System/exit`”; it finds six sites, including the empty ordinary-scan branch at `src/clj_surgeon/core.clj:1127`. `test/clj_surgeon/core_discovery_test.clj:56-59` explicitly calls that pre-existing debt.

   Exact command and verbatim output:

   ```sh
   rg -n '"find" "-H"|"-print0"|System/exit|catch StackOverflowError|\(defn ls-tree-root-refusal' src/clj_surgeon --glob '*.clj'
   ```

   ```text
   src/clj_surgeon/agent_routing.clj:160:      (System/exit 2))))
   src/clj_surgeon/core.clj:261:            args (concat ["find" "-H" (find-start-token dir)]
   src/clj_surgeon/core.clj:267:                          ")" "-print0"])
   src/clj_surgeon/core.clj:365:                     "find" "-H" (find-start-token dir)
   src/clj_surgeon/core.clj:372:                     "-print0")]
   src/clj_surgeon/core.clj:546:      (catch StackOverflowError _
   src/clj_surgeon/core.clj:1039:(defn ls-tree-root-refusal
   src/clj_surgeon/core.clj:1127:          (System/exit 1))
   src/clj_surgeon/core.clj:1331:    (System/exit 1))
   src/clj_surgeon/core.clj:2194:        (System/exit 1)))
   src/clj_surgeon/core.clj:2200:      (System/exit 1))))
   ```

4. **The predicted merge break is reproduced exactly and its repair is real.** At merge commit `d6fcc069`, the whole suite has 9 failures; at `88161592`, it has none. The assertion count increases by one later at the review tip.

   Exact command at both SHAs:

   ```sh
   ~/bin/suite-run bb test/run_all.clj
   ```

   Verbatim terminal summary at `d6fcc069`:

   ```text
   Ran 867 tests containing 6997 assertions.
   9 failures, 0 errors.
   [exit_code=9]
   ```

   Verbatim terminal summary at `88161592`:

   ```text
   Ran 867 tests containing 6998 assertions.
   0 failures, 0 errors.
   [exit_code=0]
   ```

5. **The requested battery result is UNREPRODUCED — SUSPICION, because the review’s safety constraint and the battery’s own root guard are mutually incompatible.** I made the single permitted invocation, under `flock`, with a fresh root under `/tmp/mem003-fx` and without `MEMBAT_ALLOW_ANY_ROOT`. The generator hard-codes `/home/forge/tmp` as its only allowed prefix (`bench/memory_battery/generate_tree.clj:403-434`) and refused before creating or measuring the corpus. I did not run the battery a second time. Therefore the claimed zero `reference-mismatch`, two current `held-scales-with-n` lines, and four UNMEASURED lines are not presented as reproduced facts.

   Exact command:

   ```sh
   set +e
   if test -e /tmp/mem003-fx/battery-0a38e3d; then
     printf 'fixture already exists; refusing to reuse it\n'
     printf '[exit_code=97]\n'
     exit 0
   fi
   flock /home/forge/tmp/suite.lock env MEMBAT_ROOT=/tmp/mem003-fx/battery-0a38e3d make memory-battery
   rc=$?
   printf '[exit_code=%d]\n' "$rc"
   exit 0
   ```

   Verbatim terminal excerpt:

   ```text
   bb bench/memory_battery/generate_tree.clj --root "/tmp/mem003-fx/battery-0a38e3d" --scales "100,1000,10000"
   REFUSED: MEMBAT_ROOT resolves outside /home/forge/tmp: /tmp/mem003-fx/battery-0a38e3d
   {:reason :membat-root-outside-allowed, :root "/tmp/mem003-fx/battery-0a38e3d", :resolved "/tmp/mem003-fx/battery-0a38e3d", :allowed "/home/forge/tmp", :remedy "point MEMBAT_ROOT under /home/forge/tmp, or set MEMBAT_ALLOW_ANY_ROOT=1"}
   make[1]: *** [Makefile:861: memory-battery-generate] Error 2
   make: *** [Makefile:877: memory-battery] Error 2
   [exit_code=2]
   ```

   The two builder-reported remaining held-growth failures are pre-existing, not introduced here. The first-landing composition record `docs/observations/2026-09-03-integration-branch.md:338-342` records `rename-ns-plan-full-match` at 9.8/3.0 and `workspace-sources-read-all` at 40.9/6.5 (alongside a third, then-unfixed `cli-ls-tree` failure). That establishes provenance even though this review could not remeasure the current 10.0/3.0 and 40.9/6.4 values.

   Exact command and verbatim output:

   ```sh
   sed -n '333,344p' docs/observations/2026-09-03-integration-branch.md
   ```

   ````text
   **Three `held-scales-with-n` FAILs — all KNOWN, all by design.** The
   memory-battery lane's GO says so in as many words: "main is RED under it BY
   DESIGN".

   ```
   FAIL held-scales-with-n {:op :cli-ls-tree, :profile :default, :observed 95.6, :limit 11.7, :small-n-observed 9.7, :slack-mb 2.0}
   FAIL held-scales-with-n {:op :rename-ns-plan-full-match, :profile :default, :observed 9.8, :limit 3.0, :small-n-observed 1.0, :slack-mb 2.0}
   FAIL held-scales-with-n {:op :workspace-sources-read-all, :profile :default, :observed 40.9, :limit 6.5, :small-n-observed 4.5, :slack-mb 2.0}
   ```
   ````

   `INCOMPLETE` is honest rather than a false green: `reserved-peak-mb` returns nil unless an operation result has its own `:reserved` or `:resources` accountant (`src/clj_surgeon/memory_battery.clj:228-253`), and `reserved-check` converts nil into `:unmeasured` (`src/clj_surgeon/memory_battery.clj:255-275`). But on this branch it is also a gate that cannot go green: invoking each of the four battery operations on a small real tree yields nil for every reserved peak. Thus this is an honest non-pass, not a currently satisfiable release gate.

   Exact command:

   ```sh
   ~/bin/suite-run clojure -Sdeps '{:paths ["src" "bench"] :deps {babashka/fs {:mvn/version "0.5.30"} babashka/process {:mvn/version "0.6.23"}}}' -M -e "(require '[clj-surgeon.memory-battery-runner :as runner] '[clj-surgeon.memory-battery :as battery]) (doseq [op runner/ops] (let [result ((:run op) \"/tmp/mem003-fx/structure-tree\")] (prn {:operation (:id op) :result-kind (cond (map? result) :map (vector? result) :vector :else (str (type result))) :reserved-peak-mb (battery/reserved-peak-mb result)})))"
   ```

   Verbatim output:

   ```text
   {:operation :cli-ls-tree, :result-kind :vector, :reserved-peak-mb nil}
   {:operation :workspace-sources-read-all, :result-kind :map, :reserved-peak-mb nil}
   {:operation :rename-ns-plan-narrow, :result-kind :map, :reserved-peak-mb nil}
   {:operation :rename-ns-plan-full-match, :result-kind :map, :reserved-peak-mb nil}
   ```

6. **The intent-registry delta is exactly one ID and ownership is stated in both directions.** The base audit is 256/0; the tip is 257/0; set subtraction yields only `MCP-OP-MEM-003`. `docs/intent/memory/memory-transaction-specs.md:28-47` points MEM-001/011 to memory-boundedness using deferred rows, while `docs/intent/memory-boundedness/memory-boundedness-specs.md:13-24` points back and explains the source markers. With zero unknown-witness violations, no audited marker is orphaned.

   Exact commands:

   ```sh
   cd /tmp/mem003-fx/base && ~/bin/suite-run bb -e "(require '[clj-surgeon.mcp-intent-contract :as c]) (let [a (c/audit-current-repository)] (prn {:ok (:ok a) :specs (count (:specs a)) :violations (count (:violations a))}))"
   cd /tmp/mem003-fx/current && ~/bin/suite-run bb -e "(require '[clj-surgeon.mcp-intent-contract :as c]) (let [a (c/audit-current-repository)] (prn {:ok (:ok a) :specs (count (:specs a)) :violations (count (:violations a))}))"
   ~/bin/suite-run bb -e "(require '[clj-surgeon.mcp-intent-contract :as c] '[clojure.set :as set]) (let [base (set (keys (:specs (c/audit-current-repository \"/tmp/mem003-fx/base\")))) tip (set (keys (:specs (c/audit-current-repository \"/tmp/mem003-fx/current\"))))] (prn {:added (sort (set/difference tip base)) :removed (sort (set/difference base tip))}))"
   ```

   Verbatim outputs:

   ```text
   {:ok true, :specs 256, :violations 0}
   [exit_code=0]
   {:ok true, :specs 257, :violations 0}
   [exit_code=0]
   {:added ("MCP-OP-MEM-003"), :removed ()}
   [exit_code=0]
   ```

7. **Gate ledger: the ordinary and MCP suites match the claimed counts, but `memory-red` is red and the mandatory full battery could not start.** This is an independent release blocker in addition to finding 1. All JVM gates below were run through `~/bin/suite-run`; the MCP suite used the required direct alias, never `make mcp-test`. No Surgeon MCP server was started.

   Exact command:

   ```sh
   ~/bin/suite-run bb test/run_all.clj
   ```

   Verbatim terminal output:

   ```text
   Ran 867 tests containing 6999 assertions.
   0 failures, 0 errors.
   [exit_code=0]
   ```

   Exact command:

   ```sh
   ~/bin/suite-run clojure -M:clj-surgeon/mcp-test
   ```

   Verbatim terminal output:

   ```text
   Ran 601 tests containing 6326 assertions.
   0 failures, 0 errors.
   [exit_code=0]
   ```

   Exact command and verbatim output:

   ```sh
   make mcp-operation-oracle
   ```

   ```text
   # @spec MCP-OP-ORACLE-001
   swipl -q -f test/mcp_operation_contract_oracle.pl
   mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]
   [exit_code=0]
   ```

   The intent-audit gate is reproduced in finding 6 (257 specs, zero violations, exit 0).

   Exact command and verbatim output:

   ```sh
   ~/bin/suite-run make txn-kernel-warning-check
   ```

   ```text
   clojure -M test/kernel_warning_check.clj
   kernel warning check: 2 namespace(s), 0 warning(s)
   [exit_code=0]
   ```

   Exact command and verbatim output:

   ```sh
   ~/bin/suite-run make memory-battery-self-test
   ```

   ```text
   bb bench/memory_battery/generate_tree.clj --self-test
   generate_tree verification self-test: ok
   generate_tree root-marker self-test: ok
   generate_tree self-test: ok
   bb -e "(require 'clj-surgeon.memory-battery-test 'clojure.test) (let [r (clojure.test/run-tests 'clj-surgeon.memory-battery-test)] (System/exit (+ (:fail r) (:error r))))"

   Testing clj-surgeon.memory-battery-test

   Ran 32 tests containing 171 assertions.
   0 failures, 0 errors.
   [exit_code=0]
   ```

   The parser RED gate failed its wall-clock threshold at `bench/parser_admission/red_witness.clj:161-163`. This is a measured 52 ms against a strict `<50 ms` test, so it may be host-sensitive, but it is reproduced rather than speculative.

   Exact command:

   ```sh
   PARSER_RED_ROOT=/tmp/mem003-fx/parser-red ~/bin/suite-run make memory-red PARSER_RED_EXPECT=green
   ```

   Verbatim output:

   ```text
   FAIL giant 128m: admission scan under 50 ms {:wall-ms 104, :scan-ms 52}
   memory-red: 5/6 assertions held (expect=green) — FAIL
   make: *** [Makefile:926: memory-red] Error 1
   [exit_code=2]
   ```

   Exact command:

   ```sh
   CLJ_SURGEON_MEMORY_TMP=/tmp/mem003-fx/kernel-memory ~/bin/suite-run make memory-red-kernel
   ```

   Verbatim terminal summary:

   ```text
   FLATNESS 60 {:wall-ms 17001}
   FLATNESS 600 {:wall-ms 161411}

   Ran 4 tests containing 25 assertions.
   0 failures, 0 errors.
   [exit_code=0]
   ```

8. **Checkout and review-order proof.** I first read `docs/observations/2026-09-04-integration-2-mem003.md`, then `docs/observations/2026-09-03-integration-branch.md`, then inspected `git log --oneline 2556a38..0a38e3d` and the diff. The relevant landing spine is `d273d448` → `d66b5683` → merge `d6fcc069` → `88161592` → tip. The review checkout remained unchanged.

   Exact command and verbatim output:

   ```sh
   git rev-parse HEAD
   git status --short
   git diff --exit-code
   printf '[exit_code=%d]\n' "$?"
   ```

   ```text
   0a38e3d8c7032776127f16a723429bc3d9afd024
   [exit_code=0]
   ```

## NO-GO

The mayor must verify that every clock-derived receipt field—not only `scan_ms`—is forced through one non-hashed partition, then rerun a satisfiable locked battery and the currently red parser gate before landing on top of `2556a38`.
