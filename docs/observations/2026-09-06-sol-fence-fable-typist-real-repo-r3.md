# r3 verdict

**Overall LAND: no.**

```text
(1) EVENTS LEDGER: GO
(3) RUNNER SAFETY: GO-WITH-FIX — TYPIST_OFFLINE is bypassable through shell/os.system
(5) COST ACCOUNTING: HOLD — cost-report receipt rounds provider 0.12345678 to 0.123
```

Reviewed detached `f0c180da28099327814fb558b322721370130f24`, matching local and remote `fable/typist-real-repo`. The harness-start `origin/MCP/main` pin was `e190dd058725449f34504f6eaee6a45f94f01f1d`; it advanced externally during review to `55f40d66add447029ca0ff1e0c11f7ff3df447b2`.

## (1) Events ledger — GO

All requested cases pass:

- `:Ok` and `"ok"` are rejected as reserved-name collisions.
- `:o-k` safely becomes `o_k`.
- Key-shaped names never reach the file.
- A 200-character name and its value never reach the file.
- Duplicate normalized names retain one value and increment `dropped_fields`.
- Combined probe produced exactly `dropped_fields=6`.
- Focused suite passed 17 tests / 128 assertions.

```text
$ java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main -e "(require 'clojure.test 'clj-surgeon.telemetry-events-test) (clojure.test/run-tests 'clj-surgeon.telemetry-events-test)"
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge

Testing clj-surgeon.telemetry-events-test

Ran 17 tests containing 128 assertions.
0 failures, 0 errors.
{:test 17, :pass 128, :fail 0, :error 0, :type :summary}
```

The independent `record!` probe used `:Ok`, `"ok"`, `:o-k`, two key-shaped names, a 200-character name, and `:some-extra`/`"some_extra"`:

```text
$ java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main - <<'CLJ'
[inline ledger probe]
CLJ
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
dropped_fields= 6
ok= true tool= trusted-tool o_k= ordinary-extra
duplicate-survivor= DUP_A
shadow-values-reached-file= false
key-shaped-names-reached-file= false
200-char-name-or-value-reached-file= false
duplicate-drop-count-correct= true
raw= {"reasoning_tokens":null,"completion_tokens":null,"wall_ms":null,"tool":"trusted-tool","mission_id":null,"some_extra":"DUP_A","error_type":null,"ts":"2026-09-06T02:57:50.434516357Z","dropped_fields":6,"pid":1386243,"ok":true,"kind":"k","cost_usd":null,"upstream":null,"prompt_tokens":null,"provider":null,"o_k":"ordinary-extra","seat":"forge"}
```

## (3) Runner safety — GO-WITH-FIX

The original r2 ordering defect is closed. An instrumented execution proved exit 4 before mission selection, fixture materialization, gate resolution, or resident construction:

```text
$ TYPIST_OFFLINE=1 TYPIST_FX=/home/forge/src/clj-surgeon-fence/target/sol-r3-order-probe python3 - <<'PY'
[instrument select_mission, materialize_fixture, resolve_gate_commands and Resident]
PY
typist-run: refusing — NW: sandbox unavailable — workspace not under the fence (/home/forge/src/clj-surgeon-fence/target/sol-r3-order-probe/warm-ws-scope-roots resolves to /home/forge/src/clj-surgeon-fence/target/sol-r3-order-probe/warm-ws-scope-roots, not under /var/tmp/forge). Arm NW runs codex with --dangerously-bypass-approvals-and-sandbox; its ONLY containment is this directory, so a workspace outside it is an unsandboxed child with no fence at all.
exit= 4
called= {'select_mission': False, 'materialize_fixture': False, 'resolve_gate_commands': False, 'resident_start': False}
fx-created= False
```

A second run omitted `TYPIST_OFFLINE`, so the early NW refusal—not the offline guard—was responsible. `strace` showed no model-executor `execve`, the fresh `TYPIST_FX` remained absent, and the codex PID set did not change:

```text
$ TYPIST_FX="$fx" strace -f -e trace=execve -o "$trace" bin/typist-run --arm NW --gate-mode resident
typist-run: refusing — NW: sandbox unavailable — workspace not under the fence (/home/forge/src/clj-surgeon-fence/target/sol-r3-nw-process-probe/warm-ws-scope-roots resolves to /home/forge/src/clj-surgeon-fence/target/sol-r3-nw-process-probe/warm-ws-scope-roots, not under /var/tmp/forge). Arm NW runs codex with --dangerously-bypass-approvals-and-sandbox; its ONLY containment is this directory, so a workspace outside it is an unsandboxed child with no fence at all.
exit=4
fx-created=no
codex-process-set-changed=no
new-codex-pids=
model-executor-execve=none
--- all execve basenames ---
typist-run
python3
python3
python3
python3
python3
python3
python3
python3
python3
python3
python3
python3
1453358 +++ exited with 4 +++
```

The new guard in [bin/typist-run](/home/forge/src/clj-surgeon-fence/bin/typist-run:109) correctly refuses direct, PATH-resolved, and relative `codex` invocations, plus `urllib.request.urlopen`, with exit 6. But it is not a hard subprocess boundary: a shell trampoline, Bash alias, and `os.system` all executed a harmless fake `codex` symlink successfully.

```text
$ TYPIST_OFFLINE=1 PROBE_DIR="$probe" python3 - <<'PY'
[try direct/PATH/relative, shell, alias, os.system and urlopen routes]
PY
typist-run: refusing — offline: spawn of codex refused (TYPIST_OFFLINE=1)
typist-run: refusing — offline: spawn of codex refused (TYPIST_OFFLINE=1)
typist-run: refusing — offline: network open refused (TYPIST_OFFLINE=1)
direct relative ./codex through subprocess.run: REFUSED, exit=6
PATH-resolved codex through subprocess.run: REFUSED, exit=6
shell trampoline through subprocess.run: BYPASSED, rc=0
bash alias through subprocess.run: BYPASSED, rc=0
os.system absolute fake-codex: BYPASSED, rc=0
urllib.request.urlopen: REFUSED, exit=6
```

Named fix: guard indirect shell execution and `os.system`/related spawn surfaces, or narrow offline mode to an allowlisted executable set. Add these three bypasses to `bin/typist-run-test`.

## (5) Cost accounting — HOLD

The aggregation semantics are repaired:

```text
run-cost-one-priced-one-unpriced= 0.12345678
run-cost-all-unpriced= None
provider-candidate-cost= (0.12345678, 'provider')
```

Unpriced-only reporting is also correct:

```text
groq                            1          0           0          0         1      unknown
TOTAL                           1                                           1      unknown
1 call(s) have no rate and are UNKNOWN, not zero.
...
:cost_usd nil
:cost_source "no rate"
:total_usd nil
:unpriced_calls 1
unpriced-only-contains-$0.00= False
```

But the required provider figure is not preserved in the generated report receipt. Input `0.12345678` becomes `0.123`:

```text
run-cost-one-priced-one-unpriced= 0.12345678
provider-candidate-cost= (0.12345678, 'provider')
...
groq                            2         10          10          0         1      $0.1235
TOTAL                           2                                           1      $0.1235
...
:rows [{:key "groq"
        :calls 2
        ...
        :cost_usd 0.123
        :cost_source "provider"}]
:total_usd 0.123
:unpriced_calls 1
mixed-contains-provider-verbatim= False
```

Cause: the generic EDN serializer in [bin/typist-run](/home/forge/src/clj-surgeon-fence/bin/typist-run:974) emits ordinary floats using `%.3f`. The new self-test verifies `candidate_cost` before serialization and only checks `$0.1235` in the display, so it misses the lossy receipt.

Required fix: serialize cost figures without three-decimal rounding and add an assertion that the generated report EDN contains the exact provider value `0.12345678`.

## Required gates

```text
$ bin/typist-run-test
...
== write fence ==
NW with an out-of-fence workspace exits 4                  ok
...and NOTHING was created under TYPIST_FX                 ok
NW with a workspace outside the scratch fence is refused   ok
...and it refused BEFORE materializing anything            ok
...and it refused before any codex call                    ok
== offline guard ==
TYPIST_OFFLINE=1 is read once, at import                   ok
offline refuses a codex spawn (exit 6)                     ok  exit=6
offline refuses a claude spawn (exit 6)                    ok  exit=6
offline refuses a /usr/local/bin/codex spawn (exit 6)      ok  exit=6
offline leaves ordinary tools (clojure) alone              ok
the guard sits at the subprocess CHOKEPOINT, not at call sites ok  exit=6
offline refuses a network open (exit 6)                    ok  exit=6
== cost accounting ==
run_cost of one priced + one unpriced is the priced one    ok
run_cost of all-unpriced is None, never 0.0                ok
candidate_cost keeps the provider's own figure verbatim    ok
--cost-report: an unpriceable groq row prints unknown      ok
--cost-report: the TOTAL of only-unpriced rows is unknown  ok
--cost-report: no $0.0000 anywhere in the table            ok
--cost-report: the receipt's :total_usd is nil, not 0      ok
--cost-report: a mixed group keeps the provider's figure   ok
--cost-report: the unpriced member is still counted UNKNOWN ok

no codex session was created by this suite                 ok

typist-run-test: all checks ok
```

```text
$ ~/bin/suite-run clojure -M:clj-surgeon/test-fast
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
lanes: fast -- 41 namespace(s), home-isolated true
...
Testing clj-surgeon.telemetry-events-test
Testing clj-surgeon.workspace-onboarding-test

Ran 459 tests containing 4413 assertions.
0 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.

test-isolation: 0 violations across 41 namespace(s) (TEST-ISO-002/003/004/005/007/010)
```

Final pins and cleanliness:

```text
$ git rev-parse HEAD
f0c180da28099327814fb558b322721370130f24
$ git rev-parse fable/typist-real-repo
f0c180da28099327814fb558b322721370130f24
$ git rev-parse origin/fable/typist-real-repo
f0c180da28099327814fb558b322721370130f24
$ git status --short
[no output]
```

No prohibited port was contacted, and `make mcp-test` was not run.