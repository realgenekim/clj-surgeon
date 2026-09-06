# LAND: HOLD

Reviewed detached `HEAD` at `ebbf438958acce4a7e1177362849bec6271bf824`, matching `origin/astra/typist-route`. No source edits, commits, pushes, battery, `make mcp-test`, or forbidden-port contacts occurred.

Three landing blockers:

1. `test-fast` fails: `mission_git_process.clj` starts subprocesses without spawn-ledger enrollment.
2. The newest battery receipt is `d326f3d2`, an ancestor 86 commits behind this tip with a different tree.
3. Literal `:comment-follows-rewrite` grep is not empty, despite the runtime escape hatch being inactive.

| Section | Verdict |
|---|---|
| 1. Executor safety | **GO** |
| 2. Git seam | **GO** |
| 3. Forms lowering | **HOLD** |
| 4. Kernel commit/undo | **GO** |
| 5. Fast suite | **HOLD** |
| 6. Battery freshness | **HOLD** |
| 7. External write/network surface | **GO-WITH-FIX** |

## 1. Executor safety — GO

The production transport reads each key immediately before dispatch. The prior key remains only in a local list until the candidate finishes, to detect cross-attempt echoes. It is absent from returned records. Provider/model/upstream are closed and pinned.

The offline dummy-key execution passed, including a fake provider request, redaction, `strace`, and artifact canaries:

```console
$ TYPIST_OFFLINE=1 bin/typist-run-test
...
openrouter request body: {"max_tokens": 6000, "model": "openai/gpt-oss-120b", "provider": {"allow_fallbacks": false, "order": ["Cerebras", "Groq"]}, "temperature": 0.0, "usage": {"include": true}}
openrouter pins the upstream order unconditionally         ok
openrouter uses the REGISTRY model                         ok  openai/gpt-oss-120b
an UNPINNED upstream is the typed refusal upstream-mismatch ok  upstream-mismatch
...
NO in-process key path touches /home/forge/secrets         ok
...
no openat under strace names /home/forge/secrets           ok  openat lines=2176 hits=[]
...
the ~/.codex/sessions PATH SET is identical before and after ok  added=[] removed=[]
~/.clj-surgeon/events.jsonl did not grow                   ok  before=(True, 22665) after=(True, 22665)
no suite receipt matches the provider-priced canary        ok  []
typist-run-test: all checks ok
```

The production transport’s fake-provider suite, including key echo and cross-attempt redaction:

```console
$ python3 test/python/typist_transport_test.py -v
...
test_cross_attempt_key_redaction ... ok
test_no_opt_in_and_primary_success ... ok
test_key_echo ... ok
test_pin ... ok
test_transport_redaction ... ok
test_typed_error_and_escaped_key_redaction ... ok

----------------------------------------------------------------------
Ran 29 tests in 0.006s

OK
```

The event ledger’s closed-field, ID, scrub, byte, and permissions policy was also driven directly:

```console
$ ~/bin/suite-run clojure -M:clj-surgeon/test-deps -e '<ledger policy probe>'
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
java.nio.file.attribute.FileAttribute
{:valid-id M-42,
 :unsafe-id sha256:8019840839389693,
 :unknown-present? false,
 :secret-present? false,
 :line-bytes 321,
 :line-limit 4096,
 :parent-mode rwx------,
 :file-mode rw-------}
```

Raw IDs are retained only under the narrower `^M-[0-9]{1,12}$`, a subset of the requested `^M-[0-9]+$`. Unknown fields are dropped, secret-shaped strings scrubbed, lines bounded to 4096 UTF-8 bytes, parent mode tightened to 0700, and ledger file mode to 0600.

Focused event tests:

```console
$ ~/bin/suite-run clojure -M:clj-surgeon/test-deps -e "(require ...telemetry/events namespaces...) (clojure.test/run-tests ...)"
Testing clj-surgeon.telemetry-events-test
Testing clj-surgeon.mission-events-test
Testing clj-surgeon.mission-phase-events-test
Testing clj-surgeon.mission-provider-fallback-events-test

Ran 39 tests containing 294 assertions.
0 failures, 0 errors.
{:test 39, :pass 294, :fail 0, :error 0, :type :summary}
```

## 2. Git seam — GO

The preserved Opus harness was rerun against this detached tip.

Identity and publication/undo:

```console
$ GIT_AUTHOR_NAME=forge-anvil GIT_AUTHOR_EMAIL=forge-anvil@anvil \
  GIT_AUTHOR_DATE='2026-09-06T00:00:00Z' \
  GIT_COMMITTER_NAME=forge-anvil GIT_COMMITTER_EMAIL=forge-anvil@anvil \
  GIT_COMMITTER_DATE='2026-09-06T00:00:00Z' \
  GIT_CONFIG_GLOBAL=/var/tmp/forge/review-ebbf4389-fx/poison.gitconfig \
  GIT_DIR=/var/tmp/forge/review-ebbf4389-fx/poison.git \
  GIT_INDEX_FILE=/var/tmp/forge/review-ebbf4389-fx/outer-poison-index \
  GIT_EDITOR=false GIT_SSH_COMMAND=false \
  ~/bin/suite-run clojure -M:clj-surgeon/test-deps \
  -i /var/tmp/forge/review-ebbf4389-fx/probe.clj

PROBE surviving-GIT-vars                 ("GIT_AUTHOR_DATE" "GIT_AUTHOR_EMAIL" "GIT_AUTHOR_NAME" "GIT_COMMITTER_DATE" "GIT_COMMITTER_EMAIL" "GIT_COMMITTER_NAME" "GIT_TERMINAL_PROMPT")
PROBE b-fixture-state                    :verified
PROBE b-commit-exit                      0
PROBE b-poison-index-created             false
PROBE b-author                           "forge-anvil <forge-anvil@anvil>"
PROBE b-committer                        "forge-anvil <forge-anvil@anvil>"
PROBE b-repo-config-name                 "Gene Kim"
PROBE b-committed-paths                  ["src/fixture/core.clj"]
PROBE b-undo-exit                        1
PROBE b-undo-head-unchanged              true
PROBE b-source-still-mutated             true
PROBE b-ledger-next-action               nil
PROBE b-show-exit                        0
PROBE b-show-mentions-publication        true
PROBE b-show-next-action                 nil
```

Both hidden-gitlink mechanisms:

```console
PROBE config-ignoreSubmodules-cached-diff-default "src/fixture/core.clj"
PROBE config-ignoreSubmodules-cached-diff-none "src/fixture/core.clj\nsub"
PROBE config-ignoreSubmodules-exit       1
PROBE config-ignoreSubmodules-head-unchanged true
PROBE config-ignoreSubmodules-sidecar-cleared true
PROBE config-ignoreSubmodules-retry-exit 0
PROBE config-ignoreSubmodules-retry-head-moved true
PROBE config-ignoreSubmodules-retry-paths ["src/fixture/core.clj"]

PROBE gitmodules-ignore-all-cached-diff-default "src/fixture/core.clj"
PROBE gitmodules-ignore-all-cached-diff-none "src/fixture/core.clj\nsub"
PROBE gitmodules-ignore-all-exit         1
PROBE gitmodules-ignore-all-head-unchanged true
PROBE gitmodules-ignore-all-sidecar-cleared true
PROBE gitmodules-ignore-all-retry-exit   0
PROBE gitmodules-ignore-all-retry-head-moved true
PROBE gitmodules-ignore-all-retry-paths  ["src/fixture/core.clj"]
```

Typed refusal, metadata-loss seam, and earlier regressions:

```console
$ ~/bin/suite-run clojure -M:clj-surgeon/test-deps \
  -i /var/tmp/forge/review-ebbf4389-fx/probe2.clj

PROBE gitlink-exit                       1
PROBE gitlink-error-type                 :git-staged-scope
PROBE gitlink-head-unchanged             true

PROBE m-ok                               false
PROBE m-error-type                       :git-publication-metadata-failed
PROBE m-metadata-recorded                false
PROBE m-git-ref-updated                  true
PROBE m-ref-actually-moved               true
PROBE m-sidecar-exists                   true
PROBE m-ledger-has-publication           false
PROBE m-undo-exit                        1
PROBE m-undo-error                       "mission-undo-after-git-publication"
PROBE m-undo-status                      :published
PROBE m-undo-source-still-mutated        true
PROBE m-retry-exit                       1
PROBE m-retry-error                      :git-publication-recovery-required
PROBE m-retry-head-unchanged             true

PROBE h-escape-refusal                   :git-invalid-provenance
PROBE h-git-calls-made                   0
PROBE f-branch-refuses-main              false
PROBE f-branch-refuses-mcp-main          false
PROBE a-nothing-staged                   [1 :git-staged-scope]
PROBE b-extra-staged                     [1 :git-staged-scope]
PROBE g-readonly-exit                    1
PROBE g-readonly-typed                   {:error-type :git-boundary-failed,
                                          :git-ref-updated :unknown,
                                          :index-staging false,
                                          :source-mutation-attempted false,
                                          :hooks-run false}
PROBE g-head-unchanged                   true
PROBE g-source-unchanged                 true
PROBE g-recovery-error                   :git-publication-recovery-required
PROBE g-hook-fired                       ""
PROBE g-untracked-still-untracked        true
PROBE g-unrelated-blob-unchanged         true
```

The nine Git namespaces remain green:

```console
$ ~/bin/suite-run clojure -M:clj-surgeon/test-deps -e "(require ...nine Git test namespaces...) (clojure.test/run-tests ...)"
Ran 34 tests containing 263 assertions.
0 failures, 0 errors.
{:test 34, :pass 263, :fail 0, :error 0, :type :summary}
```

The two nonblocking Opus findings remain unchanged:

- Without seat identity variables, repository identity is used (`Gene Kim`).
- An `:uncertain` publication sidecar blocks retry/undo but has no automated clear operation.

No Git remote/push surface was found:

```console
$ rg -n '"(push|fetch|pull|clone|remote|ls-remote)"|https?://' src/clj_surgeon/mission_git*.clj
# no output
```

## 3. Forms lowering — HOLD

Execution behavior is correct:

```console
$ ~/bin/suite-run clojure -M:clj-surgeon/test-deps -e '<forms lowering probe>'
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
{:node-fingerprint-preserved true,
 :kept-ok true,
 :dropped-error :forms-comment-lost,
 :moved-error :forms-comment-moved,
 :outside-prefix-identical true,
 :outside-suffix-identical true}
```

```console
$ ~/bin/suite-run clojure -M:clj-surgeon/test-deps -e "(require ...mission-forms tests...) (clojure.test/run-tests ...)"
Testing clj-surgeon.mission-forms-source-test
Testing clj-surgeon.mission-forms-test

Ran 28 tests containing 132 assertions.
0 failures, 0 errors.
{:test 28, :pass 132, :fail 0, :error 0, :type :summary}
```

But the explicit literal grep criterion fails:

```console
$ rg -n --hidden -g '!.git/**' ':comment-follows-rewrite' . || true
./src/clj_surgeon/mission_forms_source.clj:42:    shipped a `:comment-follows-rewrite` Boolean; it was removed because it
./test/clj_surgeon/mission_forms_source_test.clj:374:  ;; :comment-follows-rewrite Boolean here; it restored the reproduced false
./test/clj_surgeon/mission_forms_source_test.clj:406:      (let [r (forms/compile-forms (assoc (span-basis guard-span) :comment-follows-rewrite true)
./docs/observations/2026-09-06-live-astra-typist-commentary.md:89:... `:comment-follows-rewrite` ...
```

The line 406 test proves the old option is inert—it still gets `:forms-comment-moved`—but “no occurrence anywhere” is false. On the literal requested contract, this section is HOLD. If the intended criterion was “no active production option,” its execution is GO.

## 4. Kernel commit/undo exactness — GO

```console
$ ~/bin/suite-run clojure -M:clj-surgeon/test-deps -e '<real fixture commit/undo probe>'
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
{:committed true,
 :changed true,
 :bad-hash-refusal :typist-invalid-undo-hash,
 :undo-ok true,
 :restored-byte-exact true}
```

Full executor namespace:

```console
$ ~/bin/suite-run clojure -M:clj-surgeon/test-deps -e "(require ...mission-typist-executor-test...) (clojure.test/run-tests ...)"
Testing clj-surgeon.mission-typist-executor-test

Ran 9 tests containing 56 assertions.
0 failures, 0 errors.
{:test 9, :pass 56, :fail 0, :error 0, :type :summary}
```

## 5. Fast suite — HOLD

Exact required command:

```console
$ nice -n 10 ~/bin/suite-run clojure -M:clj-surgeon/test-fast
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
lanes: fast -- 49 namespace(s), home-isolated true
...
FAIL in (every-src-spawn-site-records-into-the-ledger) (ns_isolation_test.clj:233)
1 src spawn site(s) that do not append to clj-surgeon.spawn-ledger, so a child they start and reap is invisible to TEST-ISO-002: src/clj_surgeon/mission_git_process.clj
expected: (empty? offenders)
  actual: (not (empty? ("src/clj_surgeon/mission_git_process.clj")))
...
Ran 522 tests containing 4985 assertions.
1 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.

test-isolation: 0 violations across 49 namespace(s) (TEST-ISO-002/003/004/005/007/010)
```

The failing source is [mission_git_process.clj](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/mission_git_process.clj:1), detected by [ns_isolation_test.clj](/home/forge/src/clj-surgeon-fence/test/clj_surgeon/ns_isolation_test.clj:233). Runtime isolation saw no violation, but the enumerated spawn-site completeness contract is red.

## 6. Battery freshness — HOLD

The battery was not run.

```console
$ tail -n 1 docs/observations/battery-ledger.edn
{:sha "d326f3d2a1d7ca96d0aaf9766d22c25f99a6d821",
 :started "2026-09-06T04:05:16Z",
 :wall_s 699,
 :verdict :pass,
 :host "anvil-server"}

$ git merge-base --is-ancestor d326f3d2 HEAD; echo "ancestor_exit=$?"
ancestor_exit=0

$ git rev-list --left-right --count d326f3d2...HEAD
0	86

$ git diff --shortstat d326f3d2..HEAD
89 files changed, 9969 insertions(+), 673 deletions(-)
```

The newest receipt is therefore not this tip: it is an ancestor 86 reachable commits behind and has a different tree.

## 7. External write/network surface — GO-WITH-FIX

No unsolicited live network path was executed. Live calls require an explicit runner arm/bench or an explicit `mission apply`/`mission run`. Models and upstreams are pinned.

Disclosed external-state surfaces:

- Mission events automatically append outside the workspace to `~/.clj-surgeon/events.jsonl`; the executed policy probe showed parent 0700/file 0600.
- Executor artifacts default outside the workspace to `~/.local/state/clj-surgeon/typist/mission-*` after explicit apply. The mission directory is 0700.
- `--arm NW` explicitly passes `--dangerously-bypass-approvals-and-sandbox`; it is opt-in and confined only by convention/cwd, not by a kernel boundary.
- The offline harness is explicitly a cooperative chokepoint, not a sandbox.
- The new Git process wrapper is generic subprocess machinery and currently missing required spawn-ledger enrollment—the fast-suite blocker above.

Static surface:

```console
$ rg -n 'default-events-file|\.local/state/clj-surgeon/typist|ap\.add_argument\("--arm"|opener\.open|urllib\.request\.urlopen|dangerously-bypass-approvals-and-sandbox' \
    src/clj_surgeon/telemetry_events.clj \
    src/clj_surgeon/mission_typist_executor.clj \
    bin/typist-run bin/typist_transport.py

bin/typist_transport.py:98: with opener.open(req,timeout=timeout) as response:
src/clj_surgeon/mission_typist_executor.clj:290: (str (System/getProperty "user.home") "/.local/state/clj-surgeon/typist")
src/clj_surgeon/telemetry_events.clj:86: (str (io/file (System/getProperty "user.home") ".clj-surgeon" "events.jsonl"))
bin/typist-run:2734:WARM_SANDBOX = ["--dangerously-bypass-approvals-and-sandbox"]
bin/typist-run:3132:    ap.add_argument("--arm", choices=["N", "NW", "F", "fake"])
```

Observed permissions:

```console
$ stat -c '%a %n' "$HOME/.local/state" "$HOME/.local/state/clj-surgeon" "$HOME/.local/state/clj-surgeon/typist"
755 /home/forge/.local/state
755 /home/forge/.local/state/clj-surgeon
755 /home/forge/.local/state/clj-surgeon/typist

$ find "$HOME/.local/state/clj-surgeon/typist" -mindepth 1 -maxdepth 1 -type d -printf '%m %f\n'
700 mission-2979536802821331676

$ find "$HOME/.local/state/clj-surgeon/typist" -mindepth 2 -maxdepth 2 -type f -printf '%m %f\n'
644 authority.edn
644 candidate-0.edn
644 candidates.edn
644 transport-close.edn
644 undo.edn
```

The artifact files are 0644 but protected by their 0700 mission directory. Still, these are outside-workspace writes without a separate artifact-location opt-in, hence GO-WITH-FIX rather than an unqualified GO.

## Final workspace state

A Python test generated `bin/__pycache__/`; I removed only those generated `.pyc` files. Final status is clean and still pinned:

```console
$ git rev-parse HEAD origin/astra/typist-route origin/MCP/main
ebbf438958acce4a7e1177362849bec6271bf824
ebbf438958acce4a7e1177362849bec6271bf824
a07607da5b20c55614ab36c4f05544515ae05307

$ git merge-base HEAD 3dda2a61
3dda2a61e00ab2a51ddcf594a060a4e61fd10ae3

$ git status --short --branch
## HEAD (no branch)
```

`origin/MCP/main` moved during the audit through records activity, but detached `HEAD` and `origin/astra/typist-route` remained exactly `ebbf4389`.

**Overall LAND: HOLD.** The minimum re-fence is: enroll `mission_git_process.clj` in the spawn ledger and get `test-fast` green; resolve the literal comment-option grep contract; then produce a battery receipt at the resulting exact tip.