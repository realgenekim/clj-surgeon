## NO-GO

1. **BLOCKING — the declared-name projector is not the claimed clock-field invariant, and its source witness can pass while an undeclared clock-derived field is publicly hashed.** The repaired ordinary path is real: `src/clj_surgeon/mcp_operation.clj:40-49` partitions the domain result and attaches request `elapsed_ms`, and the round-one command now prints `false` as claimed. Nested blocks and measured values in vectors also relocate correctly, preserving untouched structure (`src/clj_surgeon/measured.clj:142-188`).

   Exact command:

   ```sh
   ~/bin/suite-run bb -e "(require '[clj-surgeon.mcp-operation :as op] '[clj-surgeon.measured :as measured]) (let [ticks (atom [1000000 3500000]) seen (atom nil)] (op/invoke! {:clock-nanos #(let [x (first @ticks)] (swap! ticks subvec 1) x) :execute (fn [] {:ok true :receipt {:stable :fact}}) :summarize (constantly \"ok\") :serialize pr-str :callback (fn [_ _ result] (reset! seen result))}) (prn {:public-result @seen :hashed-channel (measured/hashed-channel @seen) :elapsed-survives-hash (contains? (measured/hashed-channel @seen) :elapsed_ms)}))"
   ```

   Verbatim output:

   ```text
   {:public-result {:ok true, :receipt {:stable :fact}, :measured {:elapsed_ms 2.5}}, :hashed-channel {:ok true, :receipt {:stable :fact}}, :elapsed-survives-hash false}
   EXIT 0
   ```

   Exact nested/vector command:

   ```sh
   ~/bin/suite-run bb -e "(require '[clj-surgeon.measured :as m]) (let [stable {:id 0 :facts [:a :b]} x {:stable stable :elapsed_ms 3.0 :measured {:prior_ms 1.0 :nested {:scan_ms 2.0}} :rows [{:id 1 :elapsed_ms 4.0} {:id 2 :measured {:scan_ms 5.0} :scan_ms 6.0}]} y (m/partition-measured x)] (prn {:partitioned y :hashed (m/hashed-channel y) :unpartitioned (m/unpartitioned-measured-paths y) :stable-identical (identical? stable (:stable y))}))"
   ```

   Verbatim output:

   ```text
   {:partitioned {:stable {:id 0, :facts [:a :b]}, :measured {:prior_ms 1.0, :nested {:scan_ms 2.0}, :elapsed_ms 3.0}, :rows [{:id 1, :measured {:elapsed_ms 4.0}} {:id 2, :measured {:scan_ms 6.0}}]}, :hashed {:stable {:id 0, :facts [:a :b]}, :rows [{:id 1} {:id 2}]}, :unpartitioned [], :stable-identical true}
   EXIT 0
   ```

   The invariant nevertheless depends entirely on the manual set at `src/clj_surgeon/measured.clj:115-130`. Both relocation and its diagnostic ask only whether a key is in that set (`:161`, `:167`, `:202`). The source witness does not associate a clock with a published field name; it compares only clock-call counts by `[file, enclosing form]` (`test/clj_surgeon/measured_invariant_test.clj:48-68,130-140`). On a scratch archive of `dd9d8b9`, I bound the existing hot-verification clock once and published its value under both declared `:elapsed_ms` and undeclared `:verification_wall_ms`, without changing the form's clock-read count.

   Exact scratch diff command:

   ```sh
   diff -u src/clj_surgeon/mcp_hot_verify.clj /tmp/mem003r2-fx/undeclared-field-review/src/clj_surgeon/mcp_hot_verify.clj || true
   ```

   Verbatim output:

   ```diff
   --- src/clj_surgeon/mcp_hot_verify.clj	2026-09-04 02:46:02.052271025 +0000
   +++ /tmp/mem003r2-fx/undeclared-field-review/src/clj_surgeon/mcp_hot_verify.clj	2026-09-04 02:50:36.061662457 +0000
   @@ -106,10 +106,12 @@
                                                             (keep #(or (:err %) (:out %))
                                                                   responses))))))})))
                (catch Exception error
   -              {:ok false
   -               :status :failed
   -               :error-type (or (:error-type (ex-data error))
   -                               :hot-verification-connection-failed)
   -               :error (.getMessage error)
   -               :elapsed_ms (/ (double (- (System/nanoTime) started))
   -                              1000000.0)})))))))
   +              (let [duration-ms (/ (double (- (System/nanoTime) started))
   +                                   1000000.0)]
   +                {:ok false
   +                 :status :failed
   +                 :error-type (or (:error-type (ex-data error))
   +                                 :hot-verification-connection-failed)
   +                 :error (.getMessage error)
   +                 :elapsed_ms duration-ms
   +                 :verification_wall_ms duration-ms}))))))))
   ```

   Exact attack command, run from that scratch archive:

   ```sh
   ~/bin/suite-run clojure -Sdeps '{:paths ["src" "test"] :deps {nrepl/nrepl {:mvn/version "1.3.1"}}}' -M -e "(require '[clojure.test :as t] '[clj-surgeon.measured-invariant-test :as inv] '[clj-surgeon.mcp-hot-verify :as hot] '[clj-surgeon.mcp-operation :as op] '[clj-surgeon.measured :as measured]) (let [channels (frequencies (map (comp :channel val) inv/clock-site-inventory)) raw (hot/verify! \".\" {:port-file \"definitely-missing-port\" :reload [] :tests []}) seen (atom nil) ticks (atom [0 1000000])] (op/invoke! {:clock-nanos #(let [x (first @ticks)] (swap! ticks subvec 1) x) :execute (constantly raw) :summarize (constantly \"ok\") :serialize pr-str :callback (fn [_ _ result] (reset! seen result))}) (prn {:inventoried-sites (count inv/clock-site-inventory) :classes channels}) (prn {:undeclared-field (:verification_wall_ms @seen) :hashed-field (:verification_wall_ms (measured/hashed-channel @seen)) :unpartitioned-paths (measured/unpartitioned-measured-paths @seen)}) (t/run-tests 'clj-surgeon.measured-invariant-test))"
   ```

   Verbatim output:

   ```text
   {:inventoried-sites 34, :classes {:receipt 14, :control 20}}
   {:undeclared-field 1.137065, :hashed-field 1.137065, :unpartitioned-paths []}

   Testing clj-surgeon.measured-invariant-test

   Ran 6 tests containing 16 assertions.
   0 failures, 0 errors.
   {:test 6, :pass 16, :fail 0, :error 0, :type :summary}
   EXIT 0
   ```

   This is a reproduced counterexample to `MCP-OP-TIME-005`, not a suspicion: the public result and parity subject contain the clock-derived field, the diagnostic reports none, and every invariant test passes. The blocker is not closed until the witness binds every published clock value to its declared output name (including aliases derived from an existing read), or the boundary uses an intrinsically typed measured value rather than a name vocabulary.

2. **BLOCKING — recovery is not the only public-result bypass; the SDK adapter publishes an output-schema-invalid failure outside `invoke!`.** The five normal tool handlers do use `mcp-operation/invoke!`, and `recovery/recover!` explicitly places its out-of-band receipt clocks under `:measured` at `src/clj_surgeon/recovery.clj:25-96`. But `src/clj_surgeon/mcp_server.clj:143-158` catches any exception raised by the handler/invocation and constructs `mcp-adapter-failure` directly. That result has neither `measured` nor `elapsed_ms`, despite every canonical output schema requiring it. This path is reachable whenever domain execution, finalization, summary rendering, or serialization throws.

   Exact executable command (no server started):

   ```sh
   ~/bin/suite-run clojure -Sdeps '{:deps {io.modelcontextprotocol.sdk/mcp {:mvn/version "0.17.2"} io.github.bhauman/clojure-mcp {:git/tag "v0.2.6" :git/sha "35a660b"} org.eclipse.jetty.ee10/jetty-ee10-servlet {:mvn/version "12.0.13"} org.slf4j/slf4j-nop {:mvn/version "2.0.17"} nrepl/nrepl {:mvn/version "1.3.1"}}}' -M -e "(require '[clj-surgeon.mcp-server :as server]) (let [spec (server/create-structured-async-tool {:name \"boom\" :description \"boom\" :schema {:type \"object\"} :output-schema {:type \"object\" :properties {\"measured\" {:type \"object\"}} :required [\"measured\"]} :annotations {:title \"Boom\" :read-only true :destructive false :idempotent true :open-world false :return-direct false} :tool-fn (fn [_ _ _] (throw (ex-info \"boom\" {})))}) result (.block (.apply (.call spec) nil {})) structured (.structuredContent result)] (prn {:is-error (.isError result) :structured-content structured :has-measured (contains? structured \"measured\") :result-methods (sort (distinct (map #(.getName %) (.getMethods (class result))))) }))"
   ```

   Verbatim output:

   ```text
   {:is-error true, :structured-content {"error_type" "mcp-adapter-failure", "ok" false, "error" "boom", "operation" "boom"}, :has-measured false, :result-methods ("builder" "content" "equals" "getClass" "hashCode" "isError" "meta" "notify" "notifyAll" "structuredContent" "toString" "wait")}
   EXIT 0
   ```

   The advertised schemas themselves are correctly migrated on the ordinary path:

   ```sh
   ~/bin/suite-run clojure -Sdeps '{:deps {io.modelcontextprotocol.sdk/mcp {:mvn/version "0.17.2"} io.github.bhauman/clojure-mcp {:git/tag "v0.2.6" :git/sha "35a660b"} org.eclipse.jetty.ee10/jetty-ee10-servlet {:mvn/version "12.0.13"} org.slf4j/slf4j-nop {:mvn/version "2.0.17"} nrepl/nrepl {:mvn/version "1.3.1"}}}' -M -e "(require '[clj-surgeon.mcp-server :as s]) (doseq [tool (s/make-tools nil \".\")] (let [schema (:output-schema tool)] (prn {:tool (:name tool) :required (:required schema) :top-level-elapsed (get-in schema [:properties \"elapsed_ms\"]) :measured-elapsed (get-in schema [:properties \"measured\" :properties \"elapsed_ms\"]) :measured-required (get-in schema [:properties \"measured\" :required])})))"
   ```

   ```text
   {:tool "inspect_clojure", :required ["ok" "operation" "measured"], :top-level-elapsed nil, :measured-elapsed {:type "number", :minimum 0}, :measured-required ["elapsed_ms"]}
   {:tool "apply_clojure_changes", :required ["ok" "measured"], :top-level-elapsed nil, :measured-elapsed {:type "number", :minimum 0}, :measured-required ["elapsed_ms"]}
   {:tool "edit_clojure", :required ["ok" "measured"], :top-level-elapsed nil, :measured-elapsed {:type "number", :minimum 0}, :measured-required ["elapsed_ms"]}
   {:tool "transform_clojure", :required ["ok" "measured"], :top-level-elapsed nil, :measured-elapsed {:type "number", :minimum 0}, :measured-required ["elapsed_ms"]}
   {:tool "alias_migration", :required ["ok" "measured"], :top-level-elapsed nil, :measured-elapsed {:type "number", :minimum 0}, :measured-required ["elapsed_ms"]}
   EXIT 0
   ```

3. **BLOCKING — an in-repository structuredContent consumer still reads top-level `elapsed_ms` and silently loses the server clock.** `bench/event_timing.clj:133-136` reads `structured_content.elapsed_ms` / `structuredContent.elapsed_ms` and never checks `measured.elapsed_ms`. Its result feeds several active benchmark harnesses (`bench/run_clean_codex.sh:999-1013,2020-2035` and the screen runners). Its self-test fixture at `bench/event_timing.clj:314` still supplies the legacy top-level shape, so the stale reader is green.

   Exact commands, against equal one-event fixtures differing only in old versus current result shape:

   ```sh
   ~/bin/suite-run bb bench/event_timing.clj summarize /tmp/mem003r2-fx/event-current.jsonl /tmp/mem003r2-fx/event-one-clock.tsv
   ~/bin/suite-run bb bench/event_timing.clj summarize /tmp/mem003r2-fx/event-legacy.jsonl /tmp/mem003r2-fx/event-legacy-clock.tsv
   ~/bin/suite-run bb bench/event_timing.clj --self-test
   ```

   Verbatim output:

   ```text
   {:schema :clj-surgeon.benchmark-event-timing/v1, :schema-version 1, :clock :observer-received-at-harness, :event-count 1, :observer-window-ms 0.0, :observations [{:line-byte-count 210, :observer-utc-ms 1000, :server "clj-surgeon", :item-id "m", :tool "inspect_clojure", :item-type "mcp_tool_call", :item-status "completed", :event-kind :mcp-tool-call-completed, :sequence 1, :observer-monotonic-ns 1000000, :event-type "item.completed"}], :transitions [], :item-spans {:complete [], :incomplete []}}
   {:schema :clj-surgeon.benchmark-event-timing/v1, :schema-version 1, :clock :observer-received-at-harness, :event-count 1, :observer-window-ms 0.0, :observations [{:line-byte-count 197, :observer-utc-ms 1000, :server "clj-surgeon", :item-id "m", :tool "inspect_clojure", :item-type "mcp_tool_call", :item-status "completed", :event-kind :mcp-tool-call-completed, :sequence 1, :server-authoritative-elapsed-ms 12.5, :observer-monotonic-ns 1000000, :event-type "item.completed"}], :transitions [], :item-spans {:complete [], :incomplete []}}
   benchmark event timing self-test passed
   EXIT 0
   ```

   The usage collector's `elapsed_ms` reads at `skills/study-agent-usage/scripts/collect_agent_usage.py:1398-1411` are for separate LSP/MCP telemetry events, not structuredContent. The anvil-arms scorer reads watcher-generated action durations (`bench/anvil-arms/watch.py:881-907`, `score.py:425-427`). MCP summaries now use `mcp-operation/elapsed-ms`, and the battery hashes `measured/hashed-channel`. Those consumers are not stale. `bench/event_timing.clj` is the reproduced silent zero.

4. **BLOCKING — the amended EARS rows describe the intended new wire, but the intent chain and current code do not agree end to end.** RESULT-001/002/003, TIME-003/004, SCHEMA-001, ASYNC-001/002/004, new TIME-005, and EXIT-001 were amended in `docs/intent/mcp-operation-contract/mcp-operation-contract-specs.md:14-36`. They match normal finalized results and the async fields. They do not match the adapter failure in finding 2 or the undeclared-name result in finding 1. Moreover, the owning LLD still says top-level `elapsed_ms`, every result reaches one choke point, and the finalizer only adds elapsed (`docs/intent/mcp-operation-contract/mcp-operation-contract-design.md:47,78,92,114,280,505`); the HLD still says top-level at `docs/high-level-design.md:1001`. This scoped change skipped the mandatory HLD → LLD → EARS direction.

   Exact command:

   ```sh
   git diff --name-only 0a38e3d..dd9d8b9 | rg '^(docs/high-level-design\.md|docs/intent/mcp-operation-contract/)' || true
   rg -n 'shared finalizer records elapsed_ms|Associates authoritative|top-level public request clock|output schema declares|single publication choke|adds elapsed_ms only' docs/intent/mcp-operation-contract/mcp-operation-contract-design.md
   rg -n 'Every public MCP operation returns.*elapsed_ms' docs/high-level-design.md
   ```

   Verbatim output:

   ```text
   docs/intent/mcp-operation-contract/mcp-operation-contract-specs.md
   47:+-- shared finalizer records elapsed_ms
   78:4. Associates authoritative `elapsed_ms` with the result.
   92:top-level public request clock, and it is never silently overwritten without
   114:Every public output schema declares `elapsed_ms` as a required number with a
   280:- the outcome reaches the single publication choke point and finalizer ;
   505:-> shared finalizer adds elapsed_ms only
   1001:- Every public MCP operation returns a finite, non-negative `elapsed_ms` on
   EXIT 0
   ```

5. **The `System/exit` repair is reproduced and is not a remaining blocker.** The comment-stripping inventory sees ten live calls, all in `-main`; the wider search found no `Runtime/halt` or `shutdown-agents` exit route. `run-fresh-scan` and `run-ls-tree` now return typed data (`src/clj_surgeon/core.clj:1112-1140,1357-1366`) without killing the calling JVM. The real CLI still exits 1 and prints both messages on stdout. The missing-`:dir` CLI is rejected one layer earlier as `:missing-arguments`, while the library returns `:missing-required-argument`; both are typed.

   Exact inventory command:

   ```sh
   ~/bin/suite-run bb -e "(require '[clj-surgeon.measured-invariant-test :as inv]) (prn {:system-exit-sites (@#'clj-surgeon.measured-invariant-test/scan #\"System/exit\") :allow-list inv/exit-allow-list})"
   ```

   Verbatim output:

   ```text
   {:system-exit-sites {["src/clj_surgeon/agent_routing.clj" "-main"] 1, ["src/clj_surgeon/core.clj" "-main"] 2, ["src/clj_surgeon/memory_battery_runner.clj" "-main"] 4, ["src/clj_surgeon/workspace_onboarding.clj" "-main"] 1, ["src/clj_surgeon/worktree_lifecycle_io.clj" "-main"] 2}, :allow-list #{"-main"}}
   EXIT 0
   ```

   Exact wider search and verbatim output:

   ```sh
   rg -n 'System/exit|\(exit([[:space:]]|\))|shutdown-agents|Runtime/(halt|exit)|\.halt\(' src --glob '*.clj'
   ```

   ```text
   src/clj_surgeon/core.clj:1128:      ;; `(System/exit 1)` from inside a library function: every caller that
   src/clj_surgeon/core.clj:1358:      ;; A missing :dir used to print and `(System/exit 1)` from inside this
   src/clj_surgeon/core.clj:2213:        (System/exit 1)))
   src/clj_surgeon/core.clj:2219:      (System/exit 1))))
   src/clj_surgeon/agent_routing.clj:160:      (System/exit 2))))
   src/clj_surgeon/workspace_onboarding.clj:543:      (System/exit 1))))
   src/clj_surgeon/mcp_contract.clj:1088:             (str " (exit " (:exit verification) ")"))
   src/clj_surgeon/mcp_contract.clj:1097:           (when (some? (:exit failed)) (str " (exit " (:exit failed) ")"))
   src/clj_surgeon/worktree_lifecycle_io.clj:1842:      (System/exit 2))
   src/clj_surgeon/worktree_lifecycle_io.clj:1847:      (System/exit 5))))
   src/clj_surgeon/memory_battery_runner.clj:584:      (System/exit (run-attest! root scales)))
   src/clj_surgeon/memory_battery_runner.clj:592:        (System/exit exit)
   src/clj_surgeon/memory_battery_runner.clj:618:              (System/exit (if (seq (:tool-errors observation))
   src/clj_surgeon/memory_battery_runner.clj:626:              (System/exit
   EXIT 0
   ```

   Exact library and CLI commands:

   ```sh
   mkdir -p /tmp/mem003r2-fx/empty-scan-round2
   ~/bin/suite-run bb -e "(require '[clj-surgeon.core :as core]) (prn {:missing-dir (core/run-ls-tree {:format :edn})}) (prn {:empty-scan (core/run-ls-tree {:dir \"/tmp/mem003r2-fx/empty-scan-round2\" :format :edn})}) (println :caller-survived)"
   set +e
   out=$(~/bin/suite-run bb -cp src -m clj-surgeon.core :op ls-tree :format :edn); rc=$?; printf 'MISSING-DIR STDOUT:\n%s\n[exit_code=%d]\n' "$out" "$rc"
   out=$(~/bin/suite-run bb -cp src -m clj-surgeon.core :op ls-tree :dir /tmp/mem003r2-fx/empty-scan-round2 :format :edn); rc=$?; printf 'EMPTY-SCAN STDOUT:\n%s\n[exit_code=%d]\n' "$out" "$rc"
   ```

   Verbatim output:

   ```text
   {:missing-dir {:error ":ls-tree :dir is required", :error-type :missing-required-argument, :argument :dir, :next-action "pass_a_directory_path"}}
   {:empty-scan {:error "No Clojure files found under /tmp/mem003r2-fx/empty-scan-round2", :error-type :no-clojure-files, :dir "/tmp/mem003r2-fx/empty-scan-round2", :grep nil, :next-action "widen_the_scan_root_or_relax_the_grep"}}
   :caller-survived
   MISSING-DIR STDOUT:
   {:error "Missing required arguments: :dir",
    :error-type :missing-arguments,
    :missing [:dir]}
   [exit_code=1]
   EMPTY-SCAN STDOUT:
   {:error
    "No Clojure files found under /tmp/mem003r2-fx/empty-scan-round2",
    :error-type :no-clojure-files,
    :dir "/tmp/mem003r2-fx/empty-scan-round2",
    :grep nil,
    :next-action "widen_the_scan_root_or_relax_the_grep"}
   [exit_code=1]
   ```

6. **`memory-red` is green in all three required locked runs, but “best of 3” is an optimistic latency-floor gate; the OWED intent row is a filed follow-up, not an independent landing blocker.** `bench/parser_admission/red_witness.clj:113-139,185-208` keeps the 50 ms thresholds, defaults to three reps, reports every rep and host load, and gates on the minimum. The favorable argument is legitimate for estimating uncontended algorithm cost when scheduler noise only adds delay: all reps must become slow for a stable regression to pass. The unfavorable argument is equally real: the three giant passes below each depended on one 13 ms sample while other samples reached 47–65 ms, so this rule deliberately says nothing about intermittent/tail latency and increases false-green probability if the requirement was reliability rather than lower-envelope cost. The existing memory-boundedness prose already says near-line host-sensitive failures are rerun and host load is recorded (`docs/intent/memory-boundedness/memory-boundedness-specs.md:243-246`), but it does not specify minimum-of-three or the new output shape. Because this is a benchmark witness policy, not a product result/schema change, I rule that omission a filed follow-up rather than an additional blocker; it must be ratified before anyone cites this gate as a tail-latency guarantee.

   Exact command (each Make recipe itself acquires `/home/forge/tmp/suite.lock`):

   ```sh
   set +e
   for n in 1 2 3; do
     root="/tmp/mem003r2-fx/memory-red-round2-$n"
     if test -e "$root"; then printf 'root exists; refusing: %s\n' "$root"; exit 97; fi
     printf 'RUN %d ROOT %s\n' "$n" "$root"
     env PARSER_RED_ROOT="$root" make memory-red PARSER_RED_EXPECT=green
     rc=$?
     printf '[memory-red run=%d exit_code=%d]\n' "$n" "$rc"
   done
   ```

   Verbatim timing/verdict output:

   ```text
   RUN 1 ROOT /tmp/mem003r2-fx/memory-red-round2-1
   host — 16 cores, load 8.02 5.37 5.32 3/1972 754963
   PASS   nested cold: refuses in under 50 ms            {:best-wall-ms 30, :wall-ms [30 32 30], :scan-ms [20 19 18]}
   PASS   nested warm: refuses in under 50 ms            {:best-wall-ms 6, :wall-ms [18 18 6], :scan-ms [2 2 12]}
   PASS   giant 128m: admission scan under 50 ms         {:best-scan-ms 13, :scan-ms [47 13 57], :wall-ms [104 102 105]}
   memory-red: 6/6 assertions held (expect=green)
   [memory-red run=1 exit_code=0]
   RUN 2 ROOT /tmp/mem003r2-fx/memory-red-round2-2
   host — 16 cores, load 9.04 5.93 5.50 3/2014 766092
   PASS   nested cold: refuses in under 50 ms            {:best-wall-ms 34, :wall-ms [37 37 34], :scan-ms [19 20 17]}
   PASS   nested warm: refuses in under 50 ms            {:best-wall-ms 17, :wall-ms [19 20 17], :scan-ms [3 2 2]}
   PASS   giant 128m: admission scan under 50 ms         {:best-scan-ms 13, :scan-ms [13 14 61], :wall-ms [104 105 126]}
   memory-red: 6/6 assertions held (expect=green)
   [memory-red run=2 exit_code=0]
   RUN 3 ROOT /tmp/mem003r2-fx/memory-red-round2-3
   host — 16 cores, load 11.15 6.89 5.85 13/2101 783217
   PASS   nested cold: refuses in under 50 ms            {:best-wall-ms 30, :wall-ms [31 47 30], :scan-ms [20 18 17]}
   PASS   nested warm: refuses in under 50 ms            {:best-wall-ms 5, :wall-ms [18 20 5], :scan-ms [2 2 13]}
   PASS   giant 128m: admission scan under 50 ms         {:best-scan-ms 13, :scan-ms [55 13 65], :wall-ms [109 110 113]}
   memory-red: 6/6 assertions held (expect=green)
   [memory-red run=3 exit_code=0]
   ```

7. **The single battery run reproduced the builder's expected INCOMPLETE state exactly.** I used the required fresh `MEMBAT_ROOT=/home/forge/tmp/membat-mem003r2-review`, never set `MEMBAT_ALLOW_ANY_ROOT`, held `/home/forge/tmp/suite.lock` continuously across the explicit reference build and the one `memory-battery` invocation, and did not retry. There are zero `reference-mismatch` lines, exactly two held-growth failures (10.1/3.0 and 41.0/6.4), and four unmeasured reserved-peak entries. The two failures are pre-existing at `2556a38`; `docs/observations/2026-09-03-integration-branch.md:338-342` records 9.8/3.0 and 40.9/6.5 (and the then-existing cli failure).

   Exact command:

   ```sh
   set +e
   flock /home/forge/tmp/suite.lock bash -c 'set -e
   env MEMBAT_ROOT=/home/forge/tmp/membat-mem003r2-review make memory-battery-reference
   env MEMBAT_ROOT=/home/forge/tmp/membat-mem003r2-review make memory-battery'
   rc=$?
   printf '[locked-reference-and-single-battery exit_code=%d]\n' "$rc"
   exit 0
   ```

   Verbatim terminal verdict:

   ```text
   verdict: FAIL (INCOMPLETE)   exit 1
     FAIL held-scales-with-n {:op :rename-ns-plan-full-match, :profile :default, :observed 10.1, :limit 3.0, :small-n-observed 1.0, :slack-mb 2.0}
     FAIL held-scales-with-n {:op :workspace-sources-read-all, :profile :default, :observed 41.0, :limit 6.4, :small-n-observed 4.4, :slack-mb 2.0}
     UNMEASURED reserved-peak-over-budget {:op :cli-ls-tree, :detail "no operation on this branch reports an attributable reserved peak; the sampled process-wide peak is not a substitute"}
     UNMEASURED reserved-peak-over-budget {:op :rename-ns-plan-full-match, :detail "no operation on this branch reports an attributable reserved peak; the sampled process-wide peak is not a substitute"}
     UNMEASURED reserved-peak-over-budget {:op :rename-ns-plan-narrow, :detail "no operation on this branch reports an attributable reserved peak; the sampled process-wide peak is not a substitute"}
     UNMEASURED reserved-peak-over-budget {:op :workspace-sources-read-all, :detail "no operation on this branch reports an attributable reserved peak; the sampled process-wide peak is not a substitute"}

   receipt: /home/forge/tmp/membat-mem003r2-review/receipts/20260904T033420.005441151Z-battery.edn
   make: *** [Makefile:885: memory-battery] Error 1
   [locked-reference-and-single-battery exit_code=2]
   ```

   Exact parity-count command and output:

   ```sh
   printf 'reference-mismatch-count='
   rg -n 'reference-mismatch' /home/forge/tmp/membat-mem003r2-review/receipts/20260904T033420.005441151Z-battery.edn | wc -l
   ```

   ```text
   reference-mismatch-count=0
   ```

8. **The RED→GREEN witness and all requested ordinary gates reproduce, but green suites do not exercise findings 1–3.** At `ea7c6cf` the focused invariant namespace has nine failures; at `419d3d9` it has none. `clojure.test/run-tests` returns a summary rather than a nonzero process status, so both focused shell exits are 0.

   Exact historical command at each disposable archive:

   ```sh
   git archive ea7c6cf | tar -x -C /tmp/mem003r2-fx/red-ea7c6cf
   git archive 419d3d9 | tar -x -C /tmp/mem003r2-fx/green-419d3d9
   ~/bin/suite-run bb -e "(require 'clj-surgeon.measured-invariant-test) (clojure.test/run-tests 'clj-surgeon.measured-invariant-test)"
   ```

   Verbatim summaries:

   ```text
   SHA ea7c6cf19863fcb947e9396aae9205b34842b34
   Ran 6 tests containing 16 assertions.
   9 failures, 0 errors.
   {:test 6, :pass 7, :fail 9, :error 0, :type :summary}
   [RED exit_code=0]
   SHA 419d3d9f0bc6c1d82cdf0e522c9893f0893072b2
   Ran 6 tests containing 16 assertions.
   0 failures, 0 errors.
   {:test 6, :pass 16, :fail 0, :error 0, :type :summary}
   [GREEN exit_code=0]
   ```

   Exact current commands and verbatim terminal outputs:

   ```sh
   ~/bin/suite-run bb test/run_all.clj
   ```

   ```text
   Ran 873 tests containing 7016 assertions.
   0 failures, 0 errors.
   EXIT 0
   ```

   ```sh
   ~/bin/suite-run clojure -M:clj-surgeon/mcp-test
   ```

   ```text
   Ran 601 tests containing 6359 assertions.
   0 failures, 0 errors.
   EXIT 0
   ```

   ```sh
   make mcp-operation-oracle
   ```

   ```text
   # @spec MCP-OP-ORACLE-001
   swipl -q -f test/mcp_operation_contract_oracle.pl
   mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]
   [mcp-operation-oracle exit_code=0]
   ```

   ```sh
   ~/bin/suite-run bb -e "(require '[clj-surgeon.mcp-intent-contract :as c]) (let [a (c/audit-current-repository)] (prn {:ok (:ok a) :specs (count (:specs a)) :violations (count (:violations a))}))"
   ```

   ```text
   {:ok true, :specs 259, :violations 0}
   [intent-audit exit_code=0]
   ```

   ```sh
   ~/bin/suite-run make txn-kernel-warning-check
   ```

   ```text
   clojure -M test/kernel_warning_check.clj
   kernel warning check: 2 namespace(s), 0 warning(s)
   [txn-kernel-warning-check exit_code=0]
   ```

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
   [memory-battery-self-test exit_code=0]
   ```

   ```sh
   env CLJ_SURGEON_MEMORY_TMP=/tmp/mem003r2-fx/kernel-memory-round2 make memory-red-kernel
   ```

   ```text
   RED exit: 3
   RED out: Terminating due to java.lang.OutOfMemoryError: Java heap space
   GREEN journal receipt: {:tree-hash 55423110f805a112cd6b353252ccd5183e035dfb8fe4b50da52e5f310a762440, :arm :journal, :work {:walk-entries 604, :files-discovered 600, :files-read 600, :source-bytes 314772270, :largest-file-bytes 524621, :receipt-records 0, :receipt-bytes 0}, :memory {:xmx-mb 256.0, :heap-used-start-mb 27.94945526123047, :heap-used-peak-mb 254.08148956298828, :heap-used-end-mb 11.201042175292969, :heap-after-gc-peak-mb 238.95120239257812, :heap-retained-peak-mb 14.47869873046875, :wall-ms 163491}, :read-set-files 600, :commit-error nil, :committed true, :refusals [], :files 600, :reserved {:staged-files 600, :aggregate-bytes 314772270, :heap-reserved-peak-bytes 29446956, :path-list-bytes 68180, :journal-bytes-max 1073741824, :staged-files-max 2000, :aggregate-bytes-max 536870912, :journal-bytes-peak 629544540, :journal-bytes 629544540, :work-budget-bytes 201326592, :discovered-files 600, :parse-factor 56}, :files-written 600}
   FLATNESS 60 {:xmx-mb 256.0, :heap-used-start-mb 79.94684600830078, :heap-used-peak-mb 254.10598754882812, :heap-used-end-mb 13.28936767578125, :heap-after-gc-peak-mb 234.99163818359375, :heap-retained-peak-mb 14.355316162109375, :wall-ms 18852}
   FLATNESS 600 {:xmx-mb 256.0, :heap-used-start-mb 39.62690734863281, :heap-used-peak-mb 253.3577117919922, :heap-used-end-mb 11.199012756347656, :heap-after-gc-peak-mb 230.2861785888672, :heap-retained-peak-mb 14.722358703613281, :wall-ms 172721}

   Ran 4 tests containing 25 assertions.
   0 failures, 0 errors.
   [memory-red-kernel exit_code=0]
   ```

9. **Checkout and non-mutation proof.** No Surgeon server was started for review work, no commit/push/stash/add was performed, and the repository stayed at the requested clean tip. Test suites' own ephemeral test servers used their internally allocated test ports.

   Exact landing-range command and verbatim output:

   ```sh
   git log --oneline 0a38e3d..dd9d8b9
   ```

   ```text
   dd9d8b93 docs: round-2 composition record — the invariant, the System/exit decision, and the battery under a root the guard accepts
   079e567f the partition means one thing in both directions: parser admission's deterministic counts leave `:measured`
   e0732620 memory-red: the reviewer and the builder were both right — the timing lines were single samples
   419d3d9f GREEN: one publication boundary owns the partition; System/exit belongs to entrypoints
   ea7c6cfc RED: the partition is a site fix, not an invariant — measured data reaches the hash by a second route
   EXIT 0
   ```

   Exact command:

   ```sh
   git rev-parse HEAD
   git status --short
   git diff --exit-code
   printf '[exit_code=%d]\n' "$?"
   ```

   Verbatim output:

   ```text
   dd9d8b936bb06e71c77d3c651d83da60819b86e5
   [exit_code=0]
   ```

## NO-GO

This tip may not land on MCP/main on top of `2556a38` until the undeclared-clock ratchet, adapter exception result, stale benchmark reader, and HLD/LLD contract are closed and re-witnessed.
