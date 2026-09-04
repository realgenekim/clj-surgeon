## NO-GO

1. **PASS — checkout provenance is exact.** `HEAD:1`

   Exact command:

   ```text
   git status --short --branch && git rev-parse HEAD && git show -s --format='%H %s' HEAD
   ```

   Verbatim output:

   ```text
   ## HEAD (no branch)
   94e4a00c7194bcd614b1ab32a223f01e1652d29a
   94e4a00c7194bcd614b1ab32a223f01e1652d29a GREEN MCP-OP-ADMIT-149: narrow the error-type exemption to refusals
   ```

2. **BLOCKING — the fast MCP gate is red on the clean checkout because its gitignored battery receipt is absent.** `test/clj_surgeon/admit_patch_test.clj:5348` turns an ambient battery artefact into three ordinary failures, while `Makefile:195` does not make `mcp-test` depend on `admit-transaction-recovery-battery`. A named assertion is honest evidence of the missing precondition, but it is not an owned fixture or a skip bucket: a clean clone cannot pass the claimed merge gate. The gate must either own the prerequisite (for example, run the battery before the dependent witness) or classify its absence in a counted non-failure bucket while a separate merge target owns the battery proof.

   Exact command:

   ```text
   ~/bin/suite-run env TMPDIR=/var/tmp/forge/gate7-review-fx/tmp JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=/var/tmp/forge/gate7-review-fx/tmp clojure -M:clj-surgeon/mcp-test /var/tmp/forge/gate7-review-fx/run_admit.clj; gate_rc=$?; echo EXIT_CODE=$gate_rc
   ```

   Verbatim output (failure bodies and terminal summary):

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge/gate7-review-fx/tmp

   Testing clj-surgeon.admit-patch-test

   FAIL in (a-battery-only-kind-names-a-target-that-exists-and-drives-it) (admit_patch_test.clj:5348)
   PRECONDITION UNMET · no battery receipt at target/admit-transaction-recovery-battery-receipt.edn · run `make admit-transaction-recovery-battery` to prove :transaction-recovery-required by execution rather than by the structural checks alone
   expected: present?
     actual: false

   FAIL in (a-battery-only-kind-names-a-target-that-exists-and-drives-it) (admit_patch_test.clj:5352)
   the battery ran and did NOT publish the kind its exemption claims: :transaction-recovery-required · receipt nil
   expected: (and present? (contains? (set (:kinds-published record)) kind))
     actual: false

   FAIL in (a-battery-only-kind-names-a-target-that-exists-and-drives-it) (admit_patch_test.clj:5357)
   the receipt names the target that wrote it
   expected: (and present? (= target (:target record)))
     actual: false

   Ran 761 tests containing 10534 assertions.
   3 failures, 0 errors.
   EXIT_CODE=3
   ```

3. **MUST FIX — MCP-OP-ADMIT-144's claimed `bound-receipt` ratchet does not witness the “cheap correct move first” behavior.** `src/clj_surgeon/mcp_admit_tool.clj:2376` skips the reduction ladder when removing `next_call` alone makes the receipt fit. In a disposable export I replaced only that branch with `(reduce-receipt-to-budget faced)`. The sabotaged receipt dropped both `hashes` and the honest `payload_trim_unavailable` notice before dropping the call, while the entire focused suite stayed green at `164/4220/0`. The unmodified tip keeps `hashes` and reports no receipt omissions for the same probe. This is a real witness gap in the round-seven promise, although the production code at `94e4a00c` has the correct branch.

   Exact command (after the one-hunk sabotage was confirmed applied):

   ```text
   ~/bin/suite-run env TMPDIR=/var/tmp/forge/gate7-review-fx/tmp JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=/var/tmp/forge/gate7-review-fx/tmp bash -c 'cp=$(clojure -Spath -A:clj-surgeon/mcp-test); java -Xss2m -cp "$cp" clojure.main /var/tmp/forge/gate7-review-fx/probe_round7.clj 2>&1 | rg "^\\{:probe :large-facts-with-boundary-call"; probe_rc=${PIPESTATUS[0]}; java -cp "$cp" clojure.main /var/tmp/forge/gate7-review-fx/run_admit.clj 2>&1 | rg "^(Ran|[0-9]+ failures|\\{:test)"; suite_rc=${PIPESTATUS[0]}; echo PROBE_EXIT=$probe_rc SUITE_EXIT=$suite_rc; test $probe_rc -eq 0 -a $suite_rc -eq 0'; gate_rc=$?; echo EXIT_CODE=$gate_rc
   ```

   Verbatim output:

   ```text
   {:probe :large-facts-with-boundary-call, :kind :invalid-patch, :next-call-omitted true, :receipt-omitted-fields ["hashes" "payload_trim_unavailable"], :hashes-present false, :json-bytes 717, :text-chars 872}
   Ran 164 tests containing 4220 assertions.
   0 failures, 0 errors.
   {:test 164, :pass 4220, :fail 0, :error 0}
   PROBE_EXIT=0 SUITE_EXIT=0
   EXIT_CODE=0
   ```

4. **PASS — the round-seven bound resists the requested converse attacks, and neither face loses a structured leaf in the attacked receipts.** `src/clj_surgeon/mcp_admit_tool.clj:2135`, `:2189`, `:2304`, `:2350`. Bulk in every bulk-capable identity key, a 30,000-character error, a huge map, and an oversize call publishes at 3,358 JSON bytes / 4,294 text characters with the original `:invalid-patch` kind, `next_call` explicitly omitted, and zero missing short JSON leaves. A genuinely over-budget value passed directly to `redescribe-published-receipt` keeps `receipt_over_budget=true`. `payload_trim_unavailable` survives reduction and is present on the text face. The exact-budget and one-byte-over calls are both omitted because, once the envelope is counted, neither can fit unchanged; the refusal has no follow-up call of its own. With a 32,000-character call plus a large `hashes` fact section, the call gives ground and `hashes` survives at the unmodified tip.

   Exact command:

   ```text
   ~/bin/suite-run env TMPDIR=/var/tmp/forge/gate7-review-fx/tmp JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=/var/tmp/forge/gate7-review-fx/tmp bash -c 'cp=$(clojure -Spath -A:clj-surgeon/mcp-test); exec java -Xss2m -cp "$cp" clojure.main /var/tmp/forge/gate7-review-fx/probe_round7.clj'; gate_rc=$?; echo EXIT_CODE=$gate_rc
   ```

   Verbatim output:

   ```text
   {:receipt-over-budget nil, :json-bytes 3358, :fits true, :missing-count 0, :ok false, :kind :invalid-patch, :probe :all-identity-plus-call-plus-error, :next-call-omitted true, :identity-bounded ["operation" "mode" "source-unchanged" "mutation_attempted" "pre_image_binding" "lock_scope" "verification_complete" "verification_status"], :text-chars 4294}
   {:probe :redescribe-genuine-over-budget, :fit-before false, :over-before true, :over-after true, :residual-after 60000}
   {:probe :payload-trim-unavailable, :payload-trim-unavailable "no trimmable collection carried content, so this bound removed nothing", :payload-truncated nil, :json-bytes 374, :text-chars 525, :missing-count 0, :missing-sample []}
   {:own-next-call nil, :next-call-present false, :json-bytes 680, :encoded 32640, :missing-count 0, :kind :invalid-patch, :probe :next-call-boundary, :next-call-omitted true, :target 32640, :text-chars 791}
   {:own-next-call nil, :next-call-present false, :json-bytes 680, :encoded 32641, :missing-count 0, :kind :invalid-patch, :probe :next-call-boundary, :next-call-omitted true, :target 32641, :text-chars 791}
   {:probe :marker-in-next-call, :verbatim true, :kind :invalid-patch}
   {:probe :large-facts-with-boundary-call, :kind :invalid-patch, :next-call-omitted true, :receipt-omitted-fields nil, :hashes-present true, :json-bytes 10954, :text-chars 13571}
   {:probe :throwable-marker-json, :kind :admit-tool-error, :enumerated true, :json-bytes 942, :text-chars 1612, :error-truncated true, :marker-present true, :missing-count 0}
   {:probe :decode-constraint, :kind :request-decode-constraint-exceeded, :enumerated true, :blocked-by :request-decode-constraint-exceeded, :patch-bytes nil, :remedy "this is a JSON decoder limit on the request's own structure, not the patch's size: the patch is 355 UTF-8 bytes, inside the 262144-byte admission limit. Shorten the request field the decoder names above -- splitting the patch will not change this answer"}
   {:probe :planted-kind, :threw java.lang.IllegalArgumentException, :message "admit gate refusal kind is not enumerated: :planted-runtime-kind -- add it to clj-surgeon.mcp-admit-tool/admit-refusal-kinds with a fixture that drives it through the entrance, or stop constructing it", :enumerated false}
   EXIT_CODE=0
   ```

5. **PASS — the declared refusal enumeration, new decoder kind, handler catch bound, and executable text receipt hold at the real HTTP edge.** `src/clj_surgeon/mcp_admit_tool.clj:229`, `:295`, `:2435`, `:2939`. The declaration now contains 34 kinds (the prior 33 plus `:request-decode-constraint-exceeded`). The planted dynamic kind throws a plain `IllegalArgumentException` and is absent from the declaration. The handler is the sole registered admit surface and both catch arms route through `bound-receipt`; the suite recorder wraps both `execute-request!` and `handle-admit-clojure-patch`, so I found no MCP publication route outside its scope. The injected `Error` containing both the cut marker and JSON publishes as enumerated `:admit-tool-error`, inside both budgets, with zero missing short leaves. A live invalid-patch call on explicit port 8144 returned a copyable `next_call` whose JSON appears verbatim and last in text, and every empty/null leaf visible in structuredContent was spelled in text.

   Exact commands:

   ```text
   env TMPDIR=/var/tmp/forge/gate7-review-fx/tmp JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=/var/tmp/forge/gate7-review-fx/tmp clojure -J-Xms64m -J-Xmx512m -X:clj-surgeon/mcp :project-dir '"/var/tmp/forge/gate7-review-fx/export-base"' :port 8144 :telemetry :full :telemetry-dir '"/var/tmp/forge/gate7-review-fx/telemetry"' :run-id '"gate7-review"' :nrepl-port :none :ready-file '"/var/tmp/forge/gate7-review-fx/ready.edn"' :log-file '"/var/tmp/forge/gate7-review-fx/server.log"'
   curl -sS http://127.0.0.1:8144/healthz
   curl -sS -H 'Content-Type: application/json' -H 'Accept: application/json, text/event-stream' -H 'Mcp-Session-Id: ea7c2032-faf4-4c52-89ef-0c53e4e2286b' -X POST http://127.0.0.1:8144/mcp --data '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"admit_clojure_patch","arguments":{"patch":"(ns x)","mode":"preview","verify":"none"}}}'
   ```

   Verbatim output (readiness and receipt tail):

   ```text
   clj-surgeon MCP: persistent server ready on http://127.0.0.1:8144/mcp
   {"ok":true,"server":"clj-surgeon","tool_runtime":"ready","tool_registry":"ready"}
   facts · ok=false · operation=admit-patch-refused · source-unchanged=true · mutation_attempted=false · pre_image_binding=unbound · lock_scope=none · error=patch is in neither accepted grammar; its first line is "(ns x)" · next_call.arguments.mode=preview · next_call.arguments.verify=none · next_call.arguments.workspace_root=/var/tmp/forge/gate7-review-fx/export-base · next_call.blocked_by=invalid-patch · next_call.expected_headers.apply-patch=*** Begin Patch · next_call.expected_headers.unified-diff=--- a/path/to/file.clj   (or a leading `diff --git` line) · next_call.note=resend the same patch text in the patch field; it is deliberately not echoed here · next_call.patch_field=patch · next_call.patch_sha256=967e3d32490c56443c95d639e81b337e6d0339ad9baf8ec8b57736ca90f9e94e · next_call.tool=admit_clojure_patch · byte_drift_outside_hunks=0 · committed=false · detectors_not_run=null · elapsed_ms=16.2481 · error-type=invalid-patch · expected-headers.apply-patch=*** Begin Patch · expected-headers.unified-diff=--- a/path/to/file.clj   (or a leading `diff --git` line) · files=[] · grammars-tried[0]=apply-patch · grammars-tried[1]=unified-diff · hashes={} · hazards=[] · lint_delta.ran=false · mode=preview · offending-line=(ns x) · owners.added=[] · owners.changed=[] · owners.removed=[] · protected_node_drift={} · tests.failed=0 · tests.namespaces=[] · tests.passed=0 · tests.ran=false · tests.skipped=0 · verification_complete=false · verification_reasons=[] · verification_status=unverified · workspace-root=/var/tmp/forge/gate7-review-fx/export-base
   next_call · {"tool":"admit_clojure_patch","arguments":{"mode":"preview","verify":"none","workspace_root":"/var/tmp/forge/gate7-review-fx/export-base"},"patch_field":"patch","patch_sha256":"967e3d32490c56443c95d639e81b337e6d0339ad9baf8ec8b57736ca90f9e94e","note":"resend the same patch text in the patch field; it is deliberately not echoed here","blocked_by":"invalid-patch","expected_headers":{"apply-patch":"*** Begin Patch","unified-diff":"--- a/path/to/file.clj   (or a leading `diff --git` line)"}}
   ```

   The server was then stopped by its recorded session; `ss -ltnp | rg ':(8144|8145|8146)\\b' || true` produced no output.

6. **PASS on timing; BLOCKING on ownership — the recovery battery is stable, and all four receipt states are honest.** `test/admit_transaction_recovery_battery.clj:98`, `test/clj_surgeon/admit_patch_test.clj:5344`. Three battery runs produced 9/9 passing arms, every arm on its first attempt. Present is green; deleted gives the three failures in finding 2; corrupted produces one EDN-read error; wrong kind produces one named failure. This discharges timing nondeterminism but confirms the ambient-state design defect. My ruling on MCP-OP-ADMIT-147's deliberate `- [ ]`: carrying a witness-only gap without a source marker is honest traceability, but the gap cannot coexist with a GO merge gate. A counted failure is the correct shape only for a separately declared prerequisite gate; inside `mcp-test` on a clean clone it remains an unowned fixture and is blocking.

   Exact battery command:

   ```text
   ~/bin/suite-run env TMPDIR=/var/tmp/forge/gate7-review-fx/tmp JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=/var/tmp/forge/gate7-review-fx/tmp bash -c 'for run in 1 2 3; do echo BATTERY_RUN=$run; make admit-transaction-recovery-battery || exit; done'; gate_rc=$?; echo EXIT_CODE=$gate_rc
   ```

   Verbatim output:

   ```text
   BATTERY_RUN=1
   PASS n=8 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=133
   PASS n=32 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=130
   PASS n=64 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=208
   admit-transaction-recovery-battery: 3/3 arms passed
   battery receipt · target/admit-transaction-recovery-battery-receipt.edn · kinds #{:transaction-recovery-required}
   BATTERY_RUN=2
   PASS n=8 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=137
   PASS n=32 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=140
   PASS n=64 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=179
   admit-transaction-recovery-battery: 3/3 arms passed
   battery receipt · target/admit-transaction-recovery-battery-receipt.edn · kinds #{:transaction-recovery-required}
   BATTERY_RUN=3
   PASS n=8 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=137
   PASS n=32 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=150
   PASS n=64 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=169
   admit-transaction-recovery-battery: 3/3 arms passed
   battery receipt · target/admit-transaction-recovery-battery-receipt.edn · kinds #{:transaction-recovery-required}
   EXIT_CODE=0
   ```

   Exact focused commands used for each controlled state were the same Java runner under `~/bin/suite-run`, with CWD set to the state export. Verbatim summaries:

   ```text
   PRESENT:  Ran 164 tests containing 4220 assertions. 0 failures, 0 errors. {:test 164, :pass 4220, :fail 0, :error 0} EXIT_CODE=0
   DELETED:  Ran 164 tests containing 4220 assertions. 3 failures, 0 errors. {:test 164, :pass 4217, :fail 3, :error 0} EXIT_CODE=1
   CORRUPT:  Ran 164 tests containing 4216 assertions. 0 failures, 1 errors. {:test 164, :pass 4215, :fail 0, :error 1} EXIT_CODE=1
   WRONG:    Ran 164 tests containing 4220 assertions. 1 failures, 0 errors. {:test 164, :pass 4219, :fail 1, :error 0} EXIT_CODE=1
   ```

7. **PASS — the round-seven RED→GREEN pairs are genuine; six of the seven advertised negative controls reproduced, while the seventh exposed finding 3.** `test/clj_surgeon/admit_patch_test.clj:6272`, `:6410`, `:6495`, `:6544`, `:6594`. Running each new test Var at its RED and GREEN commit produced: `62678b0d 13 fail → 8db6ab4e 0`; `bddec539 5 → d720545a 0`; `7b4653d4 4 → 70bd27d0 0`; `d52a844b 4 → 09da2841 0`; `fd3af580 3 → 94e4a00c 0`. The smaller 144/145 counts are because this command executes the new Var without namespace fixtures; the complete RED commit messages report the additional fixture witnesses. On current-tip sabotage exports, stale re-description gave 4 failures, the 145 reverse gave 17, 146 gave 4, the absent 147 receipt gave 3, 148 gave 4, and 149 gave 3. Removing only the separately claimed outer cheap-move branch gave zero, as finding 3 records; therefore the builder's `7/7 (2,4,17,4,3,4,3)` claim does not reproduce as a seven-for-seven ratchet.

   Exact RED→GREEN command:

   ```text
   ~/bin/suite-run env TMPDIR=/var/tmp/forge/gate7-review-fx/tmp JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=/var/tmp/forge/gate7-review-fx/tmp bash -c 'for spec in "62678b0d:a-receipt-that-fits-never-says-it-is-over-budget" "8db6ab4e:a-receipt-that-fits-never-says-it-is-over-budget" "bddec539:a-trim-that-trimmed-nothing-is-not-recorded-as-a-truncation" "d720545a:a-trim-that-trimmed-nothing-is-not-recorded-as-a-truncation" "7b4653d4:every-catch-arm-at-the-handler-publishes-through-the-bound" "70bd27d0:every-catch-arm-at-the-handler-publishes-through-the-bound" "d52a844b:a-decoder-limit-is-not-reported-as-the-patch-being-too-large" "09da2841:a-decoder-limit-is-not-reported-as-the-patch-being-too-large" "fd3af580:the-error-type-exemption-holds-only-where-its-reason-holds" "94e4a00c:the-error-type-exemption-holds-only-where-its-reason-holds"; do c=${spec%%:*}; v=${spec#*:}; d=/var/tmp/forge/gate7-review-fx/sha-$c; echo SHA=$c VAR=$v; (cd "$d" && cp=$(clojure -Spath -A:clj-surgeon/mcp-test) && java -cp "$cp" clojure.main /var/tmp/forge/gate7-review-fx/run_vars.clj "$v"); echo CASE_EXIT=$?; done'
   ```

   Verbatim summaries:

   ```text
   SHA=62678b0d ... {:test 1, :pass 8, :fail 13, :error 0}
   SHA=8db6ab4e ... {:test 1, :pass 21, :fail 0, :error 0}
   SHA=bddec539 ... {:test 1, :pass 5, :fail 5, :error 0}
   SHA=d720545a ... {:test 1, :pass 10, :fail 0, :error 0}
   SHA=7b4653d4 ... {:test 1, :pass 8, :fail 4, :error 0}
   SHA=70bd27d0 ... {:test 1, :pass 12, :fail 0, :error 0}
   SHA=d52a844b ... {:test 1, :pass 5, :fail 4, :error 0}
   SHA=09da2841 ... {:test 1, :pass 9, :fail 0, :error 0}
   SHA=fd3af580 ... {:test 1, :pass 3, :fail 3, :error 0}
   SHA=94e4a00c ... {:test 1, :pass 6, :fail 0, :error 0}
   ```

8. **GATES — every non-ambient gate reproduces; the exact MCP gate and focused admit gate do not.** `Makefile:180`, `Makefile:195`, `Makefile:214`, `src/clj_surgeon/mcp_intent_contract.clj:159`. The exact untouched-checkout MCP command is `761/10534/3`, not the claimed `/0`. The exact same commit in a real disposable clone with the genuine battery receipt present is `761/10534/0`, proving the three failures are solely the invisible prerequisite. Babashka is `814/6724/0`; oracle passes; the named intent audit is `369/0`; analyzer is `3/3`; the focused suite is `164/4220/3` absent and `164/4220/0` present; the recovery battery is `3/3` on all three runs.

   Exact commands and verbatim terminal outputs:

   ```text
   $ ~/bin/suite-run clojure -M:clj-surgeon/mcp-test
   Ran 761 tests containing 10534 assertions.
   3 failures, 0 errors.
   EXIT_CODE=3

   $ (cd /var/tmp/forge/gate7-review-fx/clone-present && ~/bin/suite-run clojure -M:clj-surgeon/mcp-test)
   Ran 761 tests containing 10534 assertions.
   0 failures, 0 errors.
   EXIT_CODE=0

   $ ~/bin/suite-run bb test/run_all.clj
   Ran 814 tests containing 6724 assertions.
   0 failures, 0 errors.
   EXIT_CODE=0

   $ make mcp-operation-oracle
   # @spec MCP-OP-ORACLE-001
   swipl -q -f test/mcp_operation_contract_oracle.pl
   mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]
   EXIT_CODE=0

   $ ~/bin/suite-run clojure -M -e "(require '[clj-surgeon.mcp-intent-contract :as audit]) (let [r (audit/audit-current-repository)] (prn {:ok (:ok r) :specs (count (:specs r)) :violations (count (:violations r))}))"
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   {:ok true, :specs 369, :violations 0}
   EXIT_CODE=0

   $ make admit-analyzer-memory-self-test
   clojure -J-Xms64m -J-Xmx512m \
     -Sdeps '{:deps {nrepl/nrepl {:mvn/version "1.3.1"}}}' \
     -M test/admit_analyzer_memory_selftest.clj
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   PASS n=100 findings=600 analyzer-bytes=83606 ran=true introduced=300 heap-start-MiB=24 heap-peak-MiB=35 budget-MiB=409 max-heap-MiB=512 wall-ms=64
   PASS n=1000 findings=6000 analyzer-bytes=847706 ran=true introduced=3000 heap-start-MiB=23 heap-peak-MiB=31 budget-MiB=409 max-heap-MiB=512 wall-ms=245
   PASS n=10000 findings=60000 analyzer-bytes=8596706 ran=true introduced=30000 heap-start-MiB=23 heap-peak-MiB=127 budget-MiB=409 max-heap-MiB=512 wall-ms=2038
   admit-analyzer-memory-self-test: 3/3 arms passed at -Xmx512m
   EXIT_CODE=0
   ```

9. **MERGEABILITY — exactly one conflict, the claimed `.PHONY` keep-all line.** `Makefile:55`. Against current `origin/MCP/main` at `ef1b4f702740317c31a9717a21913bb9663d5ef4`, `git merge-tree` reports only `Makefile`. The base has none of the three added words, this branch adds `admit-transaction-recovery-battery`, and trunk adds `fanout-selftests tmp-leak-ratchet-self-test`; keeping all three resolves the sole conflict. This mechanical conflict does not change the NO-GO ruling.

   Exact command:

   ```text
   git rev-parse origin/MCP/main && git show -s --format='%H %s' origin/MCP/main && git merge-tree --write-tree HEAD origin/MCP/main; gate_rc=$?; echo EXIT_CODE=$gate_rc
   ```

   Verbatim output:

   ```text
   ef1b4f702740317c31a9717a21913bb9663d5ef4
   ef1b4f702740317c31a9717a21913bb9663d5ef4 queue: O2 r7 row
   b417b51f490a8479109a36b550f211b2ecd566c1
   100644 38494a98e70b30f7634b3be3ba94878fb2dda94c 1	Makefile
   100644 3d276e3a29cc5055fbcf2e9dab56ecbc0ee9d26f 2	Makefile
   100644 f12d2750e9953e47479d34ed93ab7aeae8e5b5ae 3	Makefile

   Auto-merging Makefile
   CONFLICT (content): Merge conflict in Makefile
   EXIT_CODE=1
   ```

10. **Cleanup and final provenance pass.** `HEAD:1`. No source, test, documentation, tracked fixture, index, ref, or commit in the review clone was changed. The explicit 8144 server was stopped, all fixtures under `/var/tmp/forge/gate7-review-fx` were removed, and the checkout remains detached at the requested SHA with empty `git status --porcelain` output.

    Exact commands:

    ```text
    find /var/tmp/forge/gate7-review-fx -depth -delete && test ! -e /var/tmp/forge/gate7-review-fx; echo FIXTURES_REMOVED=$?
    git rev-parse HEAD; git status --porcelain; test ! -e /var/tmp/forge/gate7-review-fx; echo FIXTURES_REMOVED=$?; ss -ltnp | rg ':(8144|8145|8146)\\b' || true
    ```

    Verbatim output:

    ```text
    FIXTURES_REMOVED=0
    94e4a00c7194bcd614b1ab32a223f01e1652d29a
    FIXTURES_REMOVED=0
    ```

## NO-GO

`94e4a00c` is not GO on its own for `MCP/main`: it has only the single trivial `.PHONY` merge conflict, but its exact clean-checkout MCP gate fails three assertions on an unowned gitignored battery receipt, and the stated “cheap move first” behavior can be sabotaged while all focused assertions remain green; this lane does not land.
