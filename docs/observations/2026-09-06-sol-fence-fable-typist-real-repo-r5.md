Overall LAND: no — HOLD.

Reviewed `38b41a0a4335c120cf8b52414d2ea82e8e49c0c6` against `origin/MCP/main` at `17b1bc656981a86e069d2ef262436f450cd951f7`. No edits; tree remains clean.

### Findings

1. **(b) NO REAL KEY REACHABLE — HOLD**

The full suite passed, but `strace` proved it read the real credentials:

```text
openat(... "/home/forge/secrets/openrouter.edn", O_RDONLY|O_CLOEXEC) = 3  # 4 times
openat(... "/home/forge/secrets/groq.edn", O_RDONLY|O_CLOEXEC) = 3        # 1 time
```

Cause: [`probe_request`](/home/forge/src/clj-surgeon-fence/bin/typist-run-test:227) monkeypatches only `urlopen`, then calls `arm_f` five times while `KEYS_DIR` is unset. Request construction reaches [`provider_key`](/home/forge/src/clj-surgeon-fence/bin/typist-run:2233), which opens the fixed real key before invoking the fake transport.

There is also a direct subprocess outside `run_runner` at [`bin/typist-run-test:601`](/home/forge/src/clj-surgeon-fence/bin/typist-run-test:601):

```text
execve(... "typist-run", "--print-key-paths")  # no --keys-dir
```

It intentionally resolves and prints `/home/forge/secrets/groq.edn`. Therefore both claims are false:

- every runner subprocess carries the dummy `--keys-dir`;
- no resolved path touches `/home/forge/secrets`.

`--print-key-paths` itself did resist printing values. With known dummy canaries plus `--arm F --provider groq`, output contained only the contract and paths—no `gsk_DUMMY123` or `sk-or-DUMMY456`.

2. **(a) Chokepoint-not-sandbox surfaces — GO-WITH-FIX**

The module docstrings and runtime banner correctly disclaim confinement, and ordinary/cost-report receipts carry:

```clojure
:offline true
:offline_contract "chokepoint-not-sandbox"
```

Two gaps prevent “everywhere”:

- An executed offline, preflight-blocked bench produced a receipt with neither field; the bench writer at [`bin/typist-run:2629`](/home/forge/src/clj-surgeon-fence/bin/typist-run:2629) omits them.
- The stale source comment at [`bin/typist-run:156`](/home/forge/src/clj-surgeon-fence/bin/typist-run:156) still says the PATH shim “cannot start the real binary” and “catches everything,” contradicting r4’s demonstrated bypasses.

3. **(c) NO SESSION / NO SPEND witnessed — GO**

The suite quoted:

```text
the ~/.codex/sessions PATH SET is identical before and after ok  added=[] removed=[]
~/.clj-surgeon/events.jsonl did not grow                   ok  before=(True, 20781) after=(True, 20781)
no receipt under the suite fx tree records a provider call ok  []
typist-run-test: all checks ok
```

This witnesses no session and no spend during this run. It does not support “could not have spent,” because real keys were loaded.

4. **(d) Telemetry split — GO**

With the environment set:

```text
{:default /home/forge/.clj-surgeon/events.jsonl,
 :resolved /var/tmp/forge/sol-r5-telemetry-probe/events.jsonl}
Ran 18 tests containing 135 assertions.
0 failures, 0 errors.
```

With it unset:

```text
{:default /home/forge/.clj-surgeon/events.jsonl,
 :resolved /home/forge/.clj-surgeon/events.jsonl}
Ran 18 tests containing 135 assertions.
0 failures, 0 errors.
```

The split at [`telemetry_events.clj:70`](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/telemetry_events.clj:70) and [`telemetry_events.clj:94`](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/telemetry_events.clj:94) is correct.

5. **Pinned fast suite — GO**

Exact requested command:

```text
$ ~/bin/suite-run clojure -M:clj-surgeon/test-fast
lanes: fast -- 41 namespace(s), home-isolated true
Ran 460 tests containing 4420 assertions.
0 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.
test-isolation: 0 violations across 41 namespace(s)
```

The `TEST-ISO-003` dirty-tree note was absent. No prohibited port or `make mcp-test` invocation was made.