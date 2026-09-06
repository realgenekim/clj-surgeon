Reviewed detached `b4ec5548012895fe919d67defd4821caf5d8e4b2`, matching `fable/typist-real-repo`. I pinned the harness-start base as `d4fd0803d869ac4d20ce353c6fa41c19b520ed44`; `origin/MCP/main` advanced during review. No source edits remain.

Verdicts:

```text
(1) EVENTS LEDGER: HOLD
(2) PROVIDER FENCE: GO — carried forward from r1
(3) RUNNER SAFETY: HOLD
(4) LANE MANIFEST / FAST GATE: GO
(5) COST ACCOUNTING: HOLD
```

## (1) Events ledger — HOLD

The original mission-ID, byte-bound, permission, scrubber, nesting, and cleanup defects are repaired. The required “cannot shadow named fields” property is not.

[`extra-fields`](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/telemetry_events.clj:210) accepts arbitrary key types. String keys `"ok"` and `"tool"` survive beside keyword keys `:ok` and `:tool`; both serialize to the same JSON names. The later duplicate shadows the trusted fields:

```text
shadow-json= {"reasoning_tokens":null,"completion_tokens":null,"wall_ms":null,"tool":"t","mission_id":null,"error_type":null,"ts":null,"pid":null,"ok":true,"kind":"k","cost_usd":null,"tool":"shadow-tool","ok":"<redacted>","upstream":null,"prompt_tokens":null,"provider":null,"seat":null}
shadow-parsed-ok= <redacted> shadow-parsed-tool= shadow-tool
```

Arbitrary field names also bypass the string scrubber:

```text
raw= {"reasoning_tokens":null,"completion_tokens":null,"wall_ms":null,"tool":"t","mission_id":null,"error_type":null,"ts":"2026-09-06T02:40:30.525680716Z","pid":1037147,"ok":true,"kind":"k","gsk_FIELDNAMECANARY":7,"cost_usd":null,"upstream":null,"prompt_tokens":null,"provider":null,"seat":"forge"}
key-shaped-field-name-reached-file= true
```

Required fix: constrain/normalize extra keys before merging, reject every JSON-name collision with named fields, and prevent unsanitized field names from carrying secrets.

The focused suite itself is green:

```text
$ java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main -e "(require 'clojure.test 'clj-surgeon.telemetry-events-test) (clojure.test/run-tests 'clj-surgeon.telemetry-events-test)"
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge

Testing clj-surgeon.telemetry-events-test

Ran 16 tests containing 100 assertions.
0 failures, 0 errors.
{:test 16, :pass 100, :fail 0, :error 0, :type :summary}
```

The 10 KB seat/multibyte and permission probe produced:

```text
line-bytes= 1426
under-limit= true
raw-canaries= []
mission-id= sha256:0eaced1b76c4ca50 valid-raw= M-90210
nested-present= false vector-present= false
parent-mode= rwx------
```

## (3) Runner safety — HOLD

Ordinary escapes correctly refuse with exit 4. The self-test passes:

```text
$ bin/typist-run-test
...
== write fence ==
TYPIST_FX=/home/forge is refused                           ok
--fixture /tmp/x is refused                                ok
a symlink under the fence pointing outside is refused      ok
NW with a workspace outside the scratch fence is refused   ok
...and it refused before any codex call                    ok

typist-run-test: all checks ok
typist-run-test-exit=0
```

Independent escapes also refused:

```text
$ TYPIST_FX=/var/tmp/forge/../../home/forge bin/typist-run --arm fake
typist-run: refusing — TYPIST_FX artifact root outside the write fence (/var/tmp/forge/../../home/forge resolves to /var/home/forge, which is under neither /var/tmp/forge nor /home/forge/src/clj-surgeon-fence)
exit=4

$ bin/typist-run --arm fake --fixture /var/tmp/forge/typist-r2-link.BwmhNm/outside/trailing-child
typist-run: refusing — --fixture preimage directory outside the write fence (... resolves to /home/forge/trailing-child, which is under neither /var/tmp/forge nor /home/forge/src/clj-surgeon-fence)
exit=4
```

But NW does not refuse before writes. [`materialize_fixture`](/home/forge/src/clj-surgeon-fence/bin/typist-run:2824) runs before the NW-only check at [line 2855](/home/forge/src/clj-surgeon-fence/bin/typist-run:2855). `strace` proved it:

```text
$ TYPIST_FX=/home/forge/src/clj-surgeon-fence/target/r2-nw-fence-probe bin/typist-run --arm NW
typist-run: refusing — NW: sandbox unavailable — workspace not under the fence (...)
exit=4
codex-execve: none
```

Writes before refusal:

```text
mkdir(".../target/r2-nw-fence-probe", 0777) = 0
mkdir(".../fixture-preimage", 0777) = 0
openat(.../fixture-preimage/deps.edn, O_WRONLY|O_CREAT|O_TRUNC, 0666) = 3
openat(.../fixture-preimage/.clj-surgeon.edn, O_WRONLY|O_CREAT|O_TRUNC, 0666) = 3
openat(.../fixture-preimage/src/fixture/util.clj, O_WRONLY|O_CREAT|O_TRUNC, 0666) = 3
openat(.../fixture-preimage/src/fixture/scope.clj, O_WRONLY|O_CREAT|O_TRUNC, 0666) = 3
openat(.../fixture-preimage/test/fixture/scope_test.clj, O_WRONLY|O_CREAT|O_TRUNC, 0666) = 3
openat(.../fixture-preimage.manifest.edn, O_WRONLY|O_CREAT|O_TRUNC, 0666) = 3
```

Required fix: perform the NW scratch-only refusal before mission selection, fixture materialization, gate resolution, resident startup, or any filesystem mutation.

## (4) Lane manifest / fast gate — GO

The exact requested command passed at the updated pins:

```text
$ ~/bin/suite-run clojure -M:clj-surgeon/test-fast
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
lanes: fast -- 41 namespace(s), home-isolated true
...
Ran 458 tests containing 4385 assertions.
0 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.

test-isolation: 0 violations across 41 namespace(s) (TEST-ISO-002/003/004/005/007/010)
suite-exit=0
nsiso-grep-exit=1
```

There was no `temp-leak:` line. Runtime manifest evidence:

```text
$ clojure -M:clj-surgeon/test-deps -e "(require '[clj-surgeon.lane-manifest :as lm]) (println {:fast (count (lm/namespaces-for :fast)) :integration (count (lm/namespaces-for :integration)) :battery (count (lm/namespaces-for :battery)) :manifest (count lm/manifest) :mission-test (get lm/manifest 'clj-surgeon.mission-test)})"
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
{:fast 41, :integration 5, :battery 15, :manifest 61, :mission-test :battery}
```

## (5) Cost accounting — HOLD

The good parts execute correctly:

```text
prices_date= 2026-09-06
pinned_prices= {'groq': {'in': 0.15, 'out': 0.6}, 'openrouter:Cerebras': {'in': 0.35, 'out': 0.75}, 'openrouter:Groq': {'in': 0.15, 'out': 0.6}}
groq-priced => (0.00135, 'table:2026-09-06:groq')
unknown-upstream => (None, None)
missing-prompt => (None, None)
missing-completion => (None, None)
provider-wins => (0.12345678, 'provider')
spark-unmetered => (0.0, 'unmetered:subscription')
run-cost-all-unpriced= None
openrouter-usage-include= {'include': True}
authorization-carried-to-request= True
candidate-cost= 0.000521 provider
receipt-has-key= False
```

The pinned rates matched the official [Groq models page](https://console.groq.com/docs/models) and [OpenRouter endpoint API](https://openrouter.ai/api/v1/models/openai/gpt-oss-120b/endpoints).

However, [`cost_report`](/home/forge/src/clj-surgeon-fence/bin/typist-run:2650) turns absent usage into aggregate token zeros, then applies the table rate. One unpriceable Groq call becomes a priced zero:

```text
groq                            1          0           0          0         1      $0.0000
TOTAL                           1                                           1      $0.0000
...
:cost_usd 0.000
:cost_source "table:2026-09-06"
:total_usd 0.000
:unpriced_calls 0
```

A mixed group also discards provider-authoritative cost:

```text
mixed-row-written= ('0.0000022', 'table:2026-09-06')
mixed-correct-sum= 0.12345788
```

A real CLI drive confirms candidate/run unknowns remain `nil`, while the report total still becomes zero:

```text
$ TYPIST_FX=/var/tmp/forge/typist-r2-cost-cli.fvLbm6 bin/typist-run --cost-report --fx /var/tmp/forge/typist-r2-cost-cli.fvLbm6
...
no-model-call                   1          0           0          0         1      unknown
TOTAL                           1                                           1      $0.0000
1 call(s) have no rate and are UNKNOWN, not zero.
cost-report-exit=0

:cost_usd nil
:cost_source "no rate"
:total_usd 0.000
:unpriced_calls 1
```

Required fix: aggregate each candidate’s existing `cost_usd`/`cost_source`; never substitute zero tokens for missing usage, preserve provider-reported values per candidate, and emit `nil` when an aggregate has no priceable calls.

Final cleanliness:

```text
$ git status --short
[no output]

$ ls /var/tmp/forge | grep nsiso
[no output]
nsiso-grep-exit=1
```

No prohibited port was contacted, and `make mcp-test` was not run.