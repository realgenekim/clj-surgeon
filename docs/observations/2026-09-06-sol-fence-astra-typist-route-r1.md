## Findings

1. Safety blocker in the exact `origin/MCP/main..981372ee` range: the legacy [bin/typist-run](/home/forge/src/clj-surgeon-fence/bin/typist-run:56) remains a live provider path that violates the stated key and pinning contracts.

   - Groq and OpenRouter key-file locations can come from `TYPIST_*_KEY_FILE`, rather than being restricted to `~/secrets/*.edn`.
   - Bench preflight reads the key file before point of use.
   - OpenRouter adds `provider.order` and `allow_fallbacks: false` only if `TYPIST_OPENROUTER_ORDER` is set.
   - `--model` permits an arbitrary model override.
   - A raw exception string is copied into candidate records and then the EDN receipt. A non-networked probe demonstrated that an exception containing the dummy key produces `OpenRouter request failed: dummy-review-key`.

   The required fix is to retire this runner’s live-provider path or give it the same contract as `typist_transport.py`: fixed `~/secrets` paths, mandatory fixed model/upstream, unconditional no-fallback routing, and generic/redacted error records.

2. The range is 24 commits, not approximately 10. The nine commits `9ea769ec..fb678c57` are the apparent intended executor slice. That narrower slice’s production executor passed this review; the HOLD results from judging the exact range requested, which also contains the unsafe prototype runner.

3. Dogfood accounting is off by one, but behavior is correct. The source patch renamed two definitions and eleven source call sites, not ten:

   - `finding-identity`: eight occurrences before = one definition plus seven calls.
   - `(field `: four calls, plus its separately renamed definition.

   All legitimate callers were updated; this is not a functional defect.

## Dogfood result

`981372ee` changes only the source and frozen test files. The focused test passes 20/20 assertions. No old external caller remains under `src/` or `test/`.

The broad tree grep has expected non-caller hits:

- Historical strings in `dev/experiments/astra_typist_real1.clj`.
- Independent private `field` helpers in `mcp_contract.clj` and `mcp_inspect.clj`.
- Shell helpers and prose.

The scoped diagnostic and external-caller greps are empty.

Both protected spans remain intact: the docstring still says “location-independent identity,” and the local binding remains named `identity`; only its function call changed.

## Executor result

The new executor path itself satisfies the examined contracts:

- [typist_transport.py](/home/forge/src/clj-surgeon-fence/bin/typist_transport.py:16) fixes both secret paths, never accepts keys through stdin/environment, pins OpenRouter to Cerebras with fallback disabled, validates the returned upstream, and emits generic errors.
- It is not registered in `core/ops-registry` or an MCP tool. The separate prototype mission entrance requires an explicit `owner_forms` mission and apply.
- [mission_typist_executor.clj](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/mission_typist_executor.clj:303) stores the inverse and receipt hash before writing.
- [mission_cli.clj](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/mission_cli.clj:438) supplies the hash stored in the mission ledger to undo.
- [mission_typist_executor_test.clj](/home/forge/src/clj-surgeon-fence/test/clj_surgeon/mission_typist_executor_test.clj:42) proves a wrong hash refuses, the stored hash succeeds, and original bytes are restored exactly.
- [mission_candidate_test.clj](/home/forge/src/clj-surgeon-fence/test/clj_surgeon/mission_candidate_test.clj:34) includes an explicit outside-owner attack.
- The mapped suite passed 79 tests and 737 assertions.
- The 32910d14 production entrance guard still returns typed `:development-only` with exit 1.
- No prohibited ports were contacted. Final worktree status was clean.

## Command transcript

```text
$ git status --short --branch
## HEAD (no branch)

$ git rev-parse HEAD
981372eebc6f38df2652a7bbf30c87b542bd1b58

$ git rev-parse 981372ee
981372eebc6f38df2652a7bbf30c87b542bd1b58

$ git rev-parse origin/MCP/main
a6c101120ddf7b1c2a99a6daae75ec5d955e14c2

$ git merge-base origin/MCP/main 981372ee
3fb7607d591f19bc389bf6d0bc754ddc2e731376
```

```text
$ git diff --name-status 981372ee^ 981372ee
M	src/clj_surgeon/diagnostic_delta.clj
M	test/clj_surgeon/diagnostic_delta_test.clj
[exit 0]

$ git diff --stat 981372ee^ 981372ee
 src/clj_surgeon/diagnostic_delta.clj       | 26 +++++++++++++-------------
 test/clj_surgeon/diagnostic_delta_test.clj |  4 ++--
 2 files changed, 15 insertions(+), 15 deletions(-)
[exit 0]
```

The substantive diff was exclusively:

```diff
-(defn- field
+(defn- finding-field

-(defn finding-identity
+(defn finding-fingerprint

-  {:filename (normalize-filename (field finding :filename))
+  {:filename (normalize-filename (finding-field finding :filename))
...
-          (let [identity (finding-identity finding)
+          (let [identity (finding-fingerprint finding)
...
-    (is (= (frequencies (map delta/finding-identity (:introduced left)))
+    (is (= (frequencies (map delta/finding-fingerprint (:introduced left)))
```

```text
$ git show 981372ee^:src/clj_surgeon/diagnostic_delta.clj | rg -n 'let \[identity|location-independent identity'
23:  "Return the location-independent identity used for diagnostic multiset deltas.
59:          (let [identity (finding-identity finding)

$ git show 981372ee:src/clj_surgeon/diagnostic_delta.clj | rg -n 'let \[identity|location-independent identity'
23:  "Return the location-independent identity used for diagnostic multiset deltas.
59:          (let [identity (finding-fingerprint finding)
```

```text
$ git show 981372ee^:src/clj_surgeon/diagnostic_delta.clj | rg -o 'finding-identity|\(field ' | sort | uniq -c
      4 (field
      8 finding-identity

$ git show 981372ee:src/clj_surgeon/diagnostic_delta.clj | rg -o 'finding-fingerprint|\(finding-field ' | sort | uniq -c
      4 (finding-field
      8 finding-fingerprint
```

```text
$ rg -n 'finding-identity|\(field ' src/clj_surgeon/diagnostic_delta.clj test/clj_surgeon/diagnostic_delta_test.clj
[no output; exit 1]

$ rg -n 'finding-identity|diagnostic-delta/field|delta/field' src test
[no output; exit 1]
```

The broad tree grep’s meaningful residual hits were:

```text
$ rg -n 'finding-identity|\(field ' dev/experiments/astra_typist_real1.clj
11:(def owners ["field" "finding-identity" "valid-finding?" "representative-difference" "diagnostic-delta"])
19:        request {:workspace_root root :intent "Rename finding-identity to finding-fingerprint and field to finding-field; preserve all behavior, docs and local bindings."
23:                                   (= owner "finding-identity") (assoc :new-owner "finding-fingerprint"))) owners)
42:                                          (str/replace "finding-identity" "finding-fingerprint")
43:                                          (str/replace "(field " "(finding-field ")

$ rg -n '^\(defn-? field\b|^\(defn field\b' src/clj_surgeon/mcp_inspect.clj src/clj_surgeon/mcp_contract.clj
src/clj_surgeon/mcp_contract.clj:122:(defn- field-name
src/clj_surgeon/mcp_contract.clj:129:(defn- field
src/clj_surgeon/mcp_inspect.clj:40:(defn- field-name
src/clj_surgeon/mcp_inspect.clj:49:(defn- field
```

```text
$ java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main -e "(require 'clojure.test 'clj-surgeon.diagnostic-delta-test) (clojure.test/run-tests 'clj-surgeon.diagnostic-delta-test)"
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge

Testing clj-surgeon.diagnostic-delta-test

Ran 2 tests containing 20 assertions.
0 failures, 0 errors.
{:test 2, :pass 20, :fail 0, :error 0, :type :summary}
[exit 0]
```

```text
$ git rev-list --count origin/MCP/main..981372ee
24

$ git log --reverse --oneline 9ea769ec^..fb678c57
9ea769ec feat(mission): add pure typist admission and frozen dossier seam
60af7a81 fix(typist): take Fable mid-file hunk parser correction
ccfb1273 prototype(typist): lower owner-keyed forms over frozen spans
86591ee9 feat: add bounded pinned typist provider transport
047fbd24 feat: persist and dispatch optional owner forms mission plans
19d29295 fix: persist owner forms recovery before executor writes
0e6f9b99 feat: stream bounded typist candidates in completion order
c18106b3 fix: bind owner forms undo to stored mission receipt hash
fb678c57 prototype(typist): prove and commit frozen owner forms through the ledger
```

```text
$ rg -n --hidden --glob '!.git/**' 'gsk_|sk-or-' .
[no output; exit 1]

$ git log --oneline -G'gsk_|sk-or-' origin/MCP/main..981372ee -- .
[no output; exit 0]

$ git log --oneline -G'Authorization:[[:space:]]*Bearer[[:space:]]+[A-Za-z0-9_-]{12,}' origin/MCP/main..981372ee -- .
[no output; exit 0]
```

The secret/pinning search exposed:

```text
bin/typist_transport.py:17: 'openrouter-cerebras':(...,'/home/forge/secrets/openrouter.edn','Cerebras'),
bin/typist_transport.py:18: 'groq':(...,'/home/forge/secrets/groq.edn','Groq')}
bin/typist_transport.py:32: if c['route']=='openrouter-cerebras':p['provider']={'order':['Cerebras'],'allow_fallbacks':False}

bin/typist-run:56:GROQ_KEY_FILE = os.environ.get("TYPIST_GROQ_KEY_FILE", "/home/forge/secrets/groq.edn")
bin/typist-run:846:        "key_file": os.environ.get("TYPIST_OPENROUTER_KEY_FILE",
bin/typist-run:897:        order = os.environ.get("TYPIST_OPENROUTER_ORDER")
bin/typist-run:898:        if order and "openrouter" in cfg["url"]:
bin/typist-run:899:            payload["provider"] = {"order": [...],
bin/typist-run:900:                                   "allow_fallbacks": False}
bin/typist-run:1223:    ap.add_argument("--model", default=None,
```

Non-networked proof that the legacy runner sends an unpinned, overridden request:

```text
$ env -u TYPIST_OPENROUTER_ORDER python3 -B - <<'PY'
...monkeypatched urlopen and dummy key loader...
PY
{"max_tokens": 6000, "messages": [{"content": "prompt", "role": "user"}], "model": "review-model-override", "temperature": 0.0}
[exit 0]
```

There is no `provider` object, so default OpenRouter fallback remains possible.

Non-networked proof that raw exception text can leak a key-shaped value:

```text
$ python3 -B - <<'PY'
...dummy key plus monkeypatched urlopen raising RuntimeError('dummy-review-key')...
PY
OpenRouter request failed: dummy-review-key
[exit 0]
```

```text
$ python3 test/python/typist_transport_test.py
.............
----------------------------------------------------------------------
Ran 13 tests in 0.002s

OK
[exit 0]

$ python3 test/python/typist_parser_test.py
..
----------------------------------------------------------------------
Ran 2 tests in 0.000s

OK
[exit 0]
```

```text
$ java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main -e "(require 'clojure.test 'clj-surgeon.cli-dispatch-test) (let [c (ref clojure.test/*initial-report-counters*)] (binding [clojure.test/*report-counters* c] (clojure.test/test-vars [#'clj-surgeon.cli-dispatch-test/up-is-explicitly-development-only #'clj-surgeon.cli-dispatch-test/cli-up-refusal-is-nonzero-and-typed])) (prn @c) (shutdown-agents) (System/exit (if (and (zero? (:fail @c)) (zero? (:error @c))) 0 1)))"
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
{:test 2, :pass 12, :fail 0, :error 0}
[exit 0]

$ java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main -m clj-surgeon.core up
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
{:error-type :development-only,
 :next_call ["clj-surgeon" "up" "<WORKSPACE>" "--force"],
 :error
 "clj-surgeon up is development-only and changes workspace agent configuration; rerun with --force"}
[exit 1]
```

```text
$ java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main -e "(require 'clojure.test 'clj-surgeon.diagnostic-delta-test 'clj-surgeon.mission-test 'clj-surgeon.mission-candidate-test 'clj-surgeon.mission-candidate-race-test 'clj-surgeon.mission-forms-test 'clj-surgeon.mission-typist-test 'clj-surgeon.mission-typist-executor-test 'clj-surgeon.lane-manifest-test) (let [r (apply clojure.test/run-tests '[clj-surgeon.diagnostic-delta-test clj-surgeon.mission-test clj-surgeon.mission-candidate-test clj-surgeon.mission-candidate-race-test clj-surgeon.mission-forms-test clj-surgeon.mission-typist-test clj-surgeon.mission-typist-executor-test clj-surgeon.lane-manifest-test])] (shutdown-agents) (System/exit (if (and (zero? (:fail r)) (zero? (:error r))) 0 1)))"
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge

Testing clj-surgeon.diagnostic-delta-test
Testing clj-surgeon.mission-test
Testing clj-surgeon.mission-candidate-test
Testing clj-surgeon.mission-candidate-race-test
Testing clj-surgeon.mission-forms-test
Testing clj-surgeon.mission-typist-test
Testing clj-surgeon.mission-typist-executor-test
Testing clj-surgeon.lane-manifest-test

Ran 79 tests containing 737 assertions.
0 failures, 0 errors.
[exit 0]

$ git status --porcelain=v1
[no output; exit 0]
```

**(1) THE DOGFOOD COMMIT 981372ee: GO**

**(2) THE EXECUTOR COMMITS, exact `origin/MCP/main..981372ee` range: HOLD — eliminate or harden `bin/typist-run`’s environment-selected keys, optional OpenRouter pin/model override, and unredacted receipt error path.**