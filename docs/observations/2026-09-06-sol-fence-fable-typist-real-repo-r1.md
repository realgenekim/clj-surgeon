Review subject: detached `HEAD` `0fc841223fc670a1fd598a0631f2a2f77bab59d0`. The harness-start `origin/MCP/main` was `8b12587aff853115ab082abfc9cf877545450f9f`; its merge-base with the subject is `3fb7607d…`. `f2efc87c` is an ancestor. The shared remote-tracking ref advanced during review, so I kept the original base SHA pinned. Worktree is clean. No prohibited port was contacted; the only listener started was port 7960.

## 1. Events ledger

The focused test passes:

```text
$ java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main -e "(require 'clojure.test 'clj-surgeon.telemetry-events-test) (clojure.test/run-tests 'clj-surgeon.telemetry-events-test)"
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge

Testing clj-surgeon.telemetry-events-test

Ran 9 tests containing 34 assertions.
0 failures, 0 errors.
{:test 9, :pass 34, :fail 0, :error 0, :type :summary}
```

Red-team results:

- Content isolation fails. [`mission-id`](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/mcp_telemetry.clj:91) copies the caller’s raw full-mode `mission_id` into the supposedly content-free ledger. Execution put both a key-shaped canary and a file-content canary into the JSON:

```text
:ledger {"wall_ms":1,"tool":"inspect_clojure","mission_id":"gsk_LEDGER_CANARY|FILE-CONTENT-CANARY","error_type":null,"ts":"2026-09-06T02:05:05.962155523Z","pid":340276,"ok":true,"kind":"mcp-call","seat":"forge"}
```

- Lines are not bounded. `SURGEON_SEAT` is copied without truncation, and the over-limit fallback does not remove or bound it:

```text
huge-line-bytes=5185 limit=4096 json=True
```

- File permissions reach `0600`.
- A newly created parent reaches `0700`, but an existing parent is never corrected because chmod only occurs when `.mkdirs` returns true in [`append-line!`](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/telemetry_events.clj:132):

```text
existing-dir-mode=755
ledger-mode=600
new-dir-mode=700 new-file-mode=600
```

- A forced write failure remains non-fatal and is counted:

```text
:write-failure-return nil
:dropped 1
:tool-return-after-write-failure tool.call
```

- The hook is before the `:off` guard in [`emit!`](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/mcp_telemetry.clj:125), and execution confirms an off-mode server still lands:

```text
:off-return nil
:off-ledger-exists true
```

- `collector --events` works:

```text
$ python3 skills/study-agent-usage/scripts/collect_agent_usage.py --events /var/tmp/forge/ledger-review.LvuNVP/existing/events.jsonl
events: 1 (seats: forge 1, pids: 1, dropped: 0)
  file: /var/tmp/forge/ledger-review.LvuNVP/existing/events.jsonl
  window: 2026-09-06T02:05:05.962155523Z .. 2026-09-06T02:05:05.962155523Z
  outcomes: ok 1, refused 0
  kinds: mcp-call 1

  tool                             ok  refused
  inspect_clojure                   1        0
```

- No response change was observed for `apply_clojure_changes`, `helper_extraction`, `inspect_clojure`, or `admit_clojure_patch` beyond the append; existing MCP telemetry/tool tests passed in the fast run. Forced filesystem failure also preserved the wrapper return. The three ledger defects above remain blocking.

Required fix: never persist a raw mission identifier; validate/hash it and scrub key-like values, bound every serialized field by UTF-8 bytes with a guaranteed final ceiling, and enforce `0700` on an existing parent before writing.

## 2. Provider fence

All three prior HOLD items are closed.

### (a) Key-file selection

```text
$ rg -n -C 3 'os\.environ' bin/typist-run
77-
78-T0 = time.time()
79-
80:FX = os.environ.get("TYPIST_FX", "/var/tmp/forge/typist-fx")
81-HERE = os.path.dirname(os.path.abspath(__file__))
...
420:REAL1_REPO = os.environ.get("TYPIST_REAL1_REPO",
421-                            "/home/forge/src/clj-surgeon-fable-typist")
```

The remaining environment reads select the artifact root and real-repository source, not key files. Key paths are fixed at `/home/forge/secrets/{groq,openrouter}.edn`.

```text
$ bin/typist-run --arm fake --keys-dir /home/forge
typist-run: refusing — keys-dir outside the test fence (/home/forge does not resolve under /var/tmp/forge)
exit(/home/forge)=4

$ bin/typist-run --arm fake --keys-dir /tmp
typist-run: refusing — keys-dir outside the test fence (/tmp does not resolve under /var/tmp/forge)
exit(/tmp)=4

$ bin/typist-run --arm fake --keys-dir /var/tmp/forge/../escape
typist-run: refusing — keys-dir outside the test fence (/var/tmp/escape does not resolve under /var/tmp/forge)
exit(../escape)=4
```

HOLD item (a): **CLOSED**.

### (b) Model and OpenRouter pin

```text
$ bin/typist-run-test
== scrubber ==
scrub('OpenRouter request failed: gsk_DUM')                ok  'OpenRouter request failed: <redacted>'
scrub('key sk-or-v1_abcDEF-09 rejected')                   ok  'key <redacted> rejected'
scrub('Authorization: Bearer sk-live.ABC-')                ok  'Authorization: <redacted>'
scrub('nothing secret here')                               ok  'nothing secret here'
scrub('two gsk_AAA and sk-or-BBB')                         ok  'two <redacted> and <redacted>'
scrub is pure (same input, same output)                    ok
scrub passes non-strings through                           ok
scrub is total (no key survives a receipt string)          ok
== typed errors ==
classify HTTPError 404 -> http-4xx                         ok
classify HTTPError 502 -> http-5xx                         ok
classify TimeoutError -> timeout                           ok
classify URLError -> transport                             ok
classify ValueError -> parse                               ok
classify Exception('gsk_DUMMY123') -> transport, no text   ok
generic_error carries no exception text                    ok
== routing pin ==
  openrouter request body: {"max_tokens": 6000, "model": "openai/gpt-oss-120b", "provider": {"allow_fallbacks": false, "order": ["Cerebras", "Groq"]}, "temperature": 0.0}
openrouter pins the upstream order unconditionally         ok
openrouter uses the REGISTRY model                         ok  openai/gpt-oss-120b
no --model flag is registered                              ok
a pinned upstream is accepted                              ok  None
an UNPINNED upstream is the typed refusal upstream-mismatch ok  upstream-mismatch
a missing upstream is refused too                          ok
groq carries NO provider block (it is not a router)        ok
groq uses the registry model                               ok
the retired TYPIST_OPENROUTER_ORDER cannot change the pin  ok
== key fence ==
no TYPIST_*_KEY_FILE read remains                          ok
groq key path is fixed                                     ok
openrouter key path is fixed                               ok
--keys-dir redirects the groq key file                     ok
preflight passes on a mode-600 dummy key                   ok
preflight refuses a key file that is not mode 600          ok
preflight refuses a missing key file                       ok
== --keys-dir fence ==
--keys-dir /home/forge is refused                          ok  typist-run: refusing — keys-dir outside the test fence (/home/forge does not resolve under /var/tmp/forge)
--keys-dir /tmp is refused                                 ok  typist-run: refusing — keys-dir outside the test fence (/tmp does not resolve under /var/tmp/forge)
--keys-dir /var/tmp/forge/../../etc is refused             ok  typist-run: refusing — keys-dir outside the test fence (/var/etc does not resolve under /var/tmp/forge)

typist-run-test: all checks ok
```

Independent env-empty, monkeypatched-`urlopen` probe:

```text
environment-keys=[]
url=https://openrouter.ai/api/v1/chat/completions
auth-present=True
model=openai/gpt-oss-120b
provider={"allow_fallbacks": false, "order": ["Cerebras", "Groq"]}
refusal=None
```

HOLD item (b): **CLOSED**.

### (c) Exception text

A monkeypatched request raised:

```text
RuntimeError("Authorization: Bearer gsk_DUMMY123; raw-exception-canary")
```

Candidate/receipt evidence:

```text
candidate-error=OpenRouter request failed (transport)
candidate-error-class=transport
candidate-has-gsk=False
record-has-gsk=False
run-dir=/var/tmp/forge/typist-error-review.uhHhuf
grep-exit=1 (1 means no matches)
```

The grep was:

```text
$ rg -n 'gsk_DUMMY123|raw-exception-canary|Bearer' /var/tmp/forge/typist-error-review.uhHhuf
[no output; exit 1]
```

HOLD item (c): **CLOSED**.

## 3. Remaining runner safety

All three real whole-file missions execute successfully:

```text
arm=fake mission=real-1 edit_form=whole-file run=fake-real-1-wf-1788660559-422827-0 first_verified_s=0.06 candidates=1 apply_ok=1 gate_ok=1 accept_ok=1 semantic_mismatch=0 refusals=0 tokens=unknown reasoning_tokens=unknown
exit(real-1,cold)=0
resident: port=7960 startup_wall_s=1.61 classpath_sha256=9a8b7e40a359d83c cwd=/var/tmp/forge/typist-missions-review.K3OY4I/real-2/resident-real-2-422921
arm=fake mission=real-2 edit_form=whole-file run=fake-real-2-wf-1788660559-422921-0 first_verified_s=1.83 candidates=1 apply_ok=1 gate_ok=1 accept_ok=1 semantic_mismatch=0 refusals=0 tokens=unknown reasoning_tokens=unknown
exit(real-2,resident)=0
resident: port=7960 startup_wall_s=1.61 classpath_sha256=9a8b7e40a359d83c cwd=/var/tmp/forge/typist-missions-review.K3OY4I/real-2j/resident-real-2j-423951
arm=fake mission=real-2j edit_form=whole-file run=fake-real-2j-wf-1788660561-423951-0 first_verified_s=7.27 candidates=1 apply_ok=1 gate_ok=1 accept_ok=1 semantic_mismatch=0 refusals=0 tokens=unknown reasoning_tokens=unknown
exit(real-2j,resident)=0
listeners-after:
```

A live lifecycle probe confirms loopback, reserved band, and termination:

```text
selected-port=7960 in-band=True
pid=408366
listen-before=State  Recv-Q Send-Q      Local Address:Port Peer Address:PortProcess | LISTEN 0 50 [::ffff:127.0.0.1]:7960 *:* users:(("java",pid=408366,fd=80))
alive=True
returncode-after-stop=143
listen-after=State Recv-Q Send-Q Local Address:Port Peer Address:PortProcess
```

Arm NW is not fenced to the workspace. [`WARM_SANDBOX`](/home/forge/src/clj-surgeon-fence/bin/typist-run:2196) supplies `--dangerously-bypass-approvals-and-sandbox`; `cwd=ws` is only a working directory, not confinement. Codex documents that option as:

```text
--dangerously-bypass-approvals-and-sandbox
    Skip all confirmation prompts and execute commands without sandboxing. EXTREMELY
    DANGEROUS. Intended solely for running in environments that are externally sandboxed
```

An executed child wrote outside the warm workspace:

```text
workspace=/var/tmp/forge/nw-fence-review.JFP4IN/warm-ws-real-1
outside-canary=/var/tmp/forge/nw-fence-review.JFP4IN/outside-workspace-canary
outside-workspace=True
outside-write-succeeded=True
bypass-flag-present=True
candidate-source=codex exec fork gpt-5.6-sol (context-warm)
```

There is a second containment problem: `TYPIST_FX` is unconstrained, and `--fixture` is converted to an arbitrary absolute path. [`materialize_fixture`](/home/forge/src/clj-surgeon-fence/bin/typist-run:849) can `rmtree` and recreate that path. Therefore the runner cannot claim it writes only beneath `/var/tmp/forge` or the worktree.

Required fix: either use a real external filesystem sandbox rooted at the warm workspace or refuse NW when one is unavailable; additionally canonicalize and refuse `TYPIST_FX`/writable fixture paths outside `/var/tmp/forge` or the approved worktree.

## 4. Lane manifest and fast lane

The pins changed consistently:

- fast: 40 → 41
- manifest namespaces: 59 → 60
- adopted tests: 221 → 230
- declared tests: 1142 → 1151

The exact gate:

```text
$ ~/bin/suite-run clojure -M:clj-surgeon/test-fast
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
lanes: fast -- 41 namespace(s), home-isolated true

Testing clj-surgeon.lane-manifest-test

FAIL in (every-test-namespace-on-disk-is-accounted-for) (lane_manifest_test.clj:87)
disk -> manifest: a new test namespace cannot silently never run
1 test namespace(s) on disk belong to no lane and are not declared in clj-surgeon.lane-manifest/excluded: clj-surgeon.mission-test
expected: (empty? unaccounted)
  actual: (not (empty? (clj-surgeon.mission-test)))

[...]

Ran 451 tests containing 4318 assertions.
1 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.

test-isolation: 0 violations across 41 namespace(s) (TEST-ISO-002/003/004/005/007/010)
temp-leak: 1 entries left under /var/tmp/forge/clj-surgeon-suite-443828-639158bd: nsiso-clj-surgeon.telemetry-events-test
[exit 2]
```

`clj-surgeon.mission-test` is the only `clojure.test` failure, but it is not the only gate failure: `telemetry-events-test` also leaks its namespace temp directory. The standalone focused run left the same class of fixtures under `/var/tmp/forge/nsiso-clj-surgeon.telemetry-events-test`; I removed only the review-created temporary artifacts afterward.

Required fix: give `mission-test` a lane or an explicit exclusion, and add fixture cleanup to `telemetry-events-test`.

Final verdicts:

```text
(1) THE EVENTS LEDGER: HOLD
(2) THE PROVIDER FENCE: GO — HOLD (a), (b), and (c) are CLOSED
(3) THE REST / RUNNER SAFETY: HOLD
(4) LANE MANIFEST / FAST GATE: HOLD
```