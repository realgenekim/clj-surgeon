All three item 4b defects are fixed and pushed to `kaocha-sublime` **origin master**, final fix **`70fac7882e92877fcc3d52cec659cc649770a5b1`**. The repository is clean and the remote branch was verified at that SHA. No other repository was pushed.

Source: `clj-surgeon-records` `50db3993`, `docs/observations/2026-09-08-agent-grammar-consumers.md`. Work began 2026-09-08 21:32:06 UTC; this report was written 2026-09-08 21:49:21 UTC, within the 45-minute timebox. Native reads and patches were used.

Commits, in order:

- `164578ad5e79c6aa96a1134a111034cb63077f2f` — red regression tests and linked intents for all three defects.
- `18b0cef673a94e8585e48be775d7f28fea1a9bd5` — load failures close with `load-error`, retaining `FAIL kind=load`.
- `70fac7882e92877fcc3d52cec659cc649770a5b1` — requested probe scope, separate focus counts, explicit closure convention, FOLLOW guards, grammar and documentation.

The linked promises are `AGENT-REQUESTED-SCOPE-020`, `AGENT-LOAD-REASON-021`, and `AGENT-CLOSURE-BASIS-022` in `docs/intent/registry.edn`, with corresponding code and test tags. Existing declared-gate, lease, arithmetic, and parent-outcome contracts remain tested.

Contract decisions:

1. **Coverage describes the recorded scope.** `probe/run` records `scope={"kind":"probe","requested":[...]}` with the exact ordered caller targets. Source namespaces are reloaded; supplied test namespaces/Vars select the requested tests. A named Var can therefore be complete while its namespace is incomplete. `partial-namespaces` retains the whole-namespace result for namespace FOLLOW. Missing requested Vars, pending tests, and required fail-fast omissions cannot manufacture a complete probe.
2. **Skip causes are separate.** New DONE `skipped` counts declared skip-meta/skip-ID exclusions; `skipped_by_focus` counts focus omissions, with declared exclusions taking precedence and no double counting. EDN records `:totals :skipped-by-focus`, `:skip-rules`, `:declared-skips`, and `:focus-rules`. Adapter-injected suite skips are focus provenance, not declared configuration. The EDN `:unexecuted-tests` count retains omissions explained by neither declaration nor focus, such as fail-fast; these remain incomplete.
3. **The convention is `closure_basis="discovered-namespaces-v1"`.** The EDN key is `:closure-basis`. Total counts all namespace groups in Kaocha's discovered/result tree, including focus-omitted groups. Selected counts evaluated groups and groups covered by declared exclusions. Counts are per suite, not deduplicated namespace names or test Vars. Empty discovered groups count; suites skipped before loading contribute no invented namespace/leaf counts. The receipt names probe-skipped suites in `:focus-rules :skip-suites`. Namespace-to-Var focus does not rebase the denominator. A convention change requires a new basis value.
4. **A full-scope FOLLOW never accepts a probe DONE, including legacy 1/1 probes.** New non-probe full acceptance additionally requires `scope.kind=full`, complete coverage and equal closure counts, alongside the existing candidate/identity/outcome checks. Ordinary selected runner runs identify their namespace scope; automatic follow-up parent verdict behavior remains intact.
5. **An unreadable target is distinct from an unreadable record.** Failed probe reload emits `FAIL kind=load`, then `STATUS refused/load-error`, exits 2 and releases the lease. Malformed/oversize records retain `protocol-error`.

The new scope/basis/focus-count group is additive in v1 and validated all-or-none. Legacy replay preserves absence: it does not fabricate a basis or reinterpret the old combined `skipped` count.

Validation completed on the final code:

| Check | Result |
|---|---|
| Red-first `clojure -M:test` before implementation | 68 tests, 493 assertions; **21 failures, 0 errors** (including the not-yet-implemented intent links) |
| After the load-reason-only commit | 20 failures, 0 errors; the typo-reason regression is green |
| Final `clojure -M:test` | **73 tests, 529 assertions, 0 failures, 0 errors** |
| `~/bin/clj-kondo --lint src test` | **0 errors, 0 warnings** |
| `git diff --check` | PASS |
| Repository process witnesses | PASS: actual SIGKILL, isolated concurrent JVMs, warm first-load/second-run receipt paths, fixed-ID collision |
| Repository CLI witnesses | PASS: leaf, source, dependency, unknown-path, tracker and clean selections |
| Immutable receipt projection through the shared Babashka reader | **9 receipts PASS**, including legacy records and the fixed cold gate; projected DONE bytes equal the runner-owned stream and bind the actual file digest |
| Fixed pinned MVR cold gate | **579 tests / 7,833 assertions**, 0 failures/errors, scope full, closure 54/54, exit 0; DONE wall 14,139 ms, entrance wall 18.45 s |
| Fixed pinned MVR full-token FOLLOW after cold gate | exit 0; byte-identical DONE replay |

The added tests cover namespace/Var scope and actual namespace-token acceptance, missing targets, exact-identity full-token refusal at 1/1, legacy-probe refusal, grammar group validation/legacy absence, declared metadata and ID skips, adapter-skipped suites, stable namespace/Var denominators, and fail-fast accounting. The process/CLI scripts ran with only their fixture/output roots relocated under `/var/tmp/forge/kaocha-defects-2/`; retained wrapper copies document those substitutions. Logs and verification scripts are in `/var/tmp/forge/kaocha-defects-2/evidence`. No performance comparison is claimed.

Specimen provenance and limits:

- Fresh detached worktree: `/var/tmp/forge/kaocha-defects-2/mvr`, created directly at MVR `56a4984` from `nrepl/test-alias`. The baseline used its committed `b1a838a` pins and unchanged `bin/test-probe` / `make test-probe` entrance.
- The fixed image used the **actual published Git pin** `70fac7882e92877fcc3d52cec659cc649770a5b1`, replacing both pins in that isolated worktree's `deps.edn`. No local-root override or hand-rolled probe adapter was used. Both images were launched with `clojure -M:nrepl`, using the specimen's explicit 1 GiB heap policy and test data directory. Both owned image PIDs were stopped; the cold gate JVM also exited.
- Baseline and fixed probes of the existing `marvin-voice-remote.over-test` discover 54 groups and run 4 tests. The source observation's 1/55 used an additional witness namespace; this report does not pretend those are the same fixture. A temporary two-Var namespace here separately proves one configured skip versus 579 focus omissions, then is removed before the 579-test cold gate. Its exact source is retained as `scope_contract_witness_test.clj` beside the logs. Only the two dependency pins remain changed in the specimen.
- The real warm probe's full-token FOLLOW exits 2 with `identity-unproved`: plain warm image attestation remains unknown. That real record proves non-acceptance, **not the scope-specific refusal branch**. The exact-identity 1/1 regression test independently proves that branch returns `uncovered/scope-missing`; no specimen receipt was edited to manufacture exact identity.

Consumer changes required:

- **`~/bin/coldstart-grade` must be updated before repinning consumers.** At inspection, `RUNDONE_OPTIONAL_KEYS` contains only `parent_outcome`; `_validate_run_done` rejects each of the new fields as an unknown key. Register `scope`, `closure_basis`, and `skipped_by_focus` as an optional legacy-compatible group, validate them together (including scope shape, supported basis and nonnegative focus count), and retain absence as an unknown legacy convention. This file was inspected, not changed.
- Its `agent_v1_binding` and any other full-gate reader must enforce the full requested scope as well as mode, identity, coverage and closure. It already filters for `mode=gate`; new records must also have `scope.kind=full`. A green probe with a 1/1 denominator is never full-gate evidence. Preserve primary/parent-outcome selection checks.
- Strict STATUS reason enums must accept `load-error` as a readable terminal load refusal, distinct from protocol corruption. Receipt readers must understand the new EDN fields and the changed meaning of `skipped`; historical combined counts must not be compared to new declared-only counts without accounting for `skipped_by_focus` and the basis marker.
- MVR's `:run-tests` and `:nrepl` pins, and curtaincall-cfp's shared `:run-tests` pin, can move to `70fac7882e92877fcc3d52cec659cc649770a5b1` after their strict reader is updated. Their existing `probe/run` adapters already relay these fields verbatim; no adapter rewrite is required. This task did not change or push either consumer repository or the grader.

Verbatim records follow. Each fenced block is copied directly from retained entrance stdout without JSON reserialization, key reordering, abbreviation, or altered values. The receipt paths name the original runner-owned files.

Baseline namespace probe: 4 executed tests, 575 combined skips, unlabeled closure 1/54; exit 0

```text
RUN-START {"v":1,"candidate":"895771c1f06d794be5125daa1851c969ce7c6fdc5cb3c5bed42c1384e1e7228b","trigger_omitted":0,"mode":"probe","active":"/var/tmp/forge/kaocha-defects-2/mvr/target/kaocha-runs/2026-09-08T21-34-37-701924255Z-7a2ddf51-e6ab-4461-b574-30ff0589083d.edn.tmp","id":"2026-09-08T21-34-37-701924255Z-7a2ddf51-e6ab-4461-b574-30ff0589083d","trigger":[],"closure":null,"session":"5d41c2f3-fe9d-4fbf-88da-96549c4741a2"}
RUN-DONE {"receipt":"/var/tmp/forge/kaocha-defects-2/mvr/target/kaocha-runs/2026-09-08T21-34-37-701924255Z-7a2ddf51-e6ab-4461-b574-30ff0589083d.edn","omitted":0,"v":1,"parent":null,"candidate":"895771c1f06d794be5125daa1851c969ce7c6fdc5cb3c5bed42c1384e1e7228b","tests":4,"pending":0,"auto_followup":false,"wall_ms":5182,"mode":"probe","coverage":"complete","outcome":"pass","shown":0,"skipped":575,"fail":0,"id":"2026-09-08T21-34-37-701924255Z-7a2ddf51-e6ab-4461-b574-30ff0589083d","error":0,"sha256":"54852baa5c8f6847fc07dc0dade470ad5298c87aeee1ae9a715409ca158d75d1","pass":40,"assertions":40,"closure":{"selected":1,"total":54}}
```

Baseline typo: readable load FAIL, then misleading protocol-error; exit 2

```text
RUN-START {"v":1,"candidate":"6a482e577bb90798ff6bca7705a9e7d665d0c10b214e4a49a6b60e7e68f81ba7","trigger_omitted":0,"mode":"probe","active":"/var/tmp/forge/kaocha-defects-2/mvr/target/kaocha-runs/2026-09-08T21-34-43-135693186Z-e4c7906a-3b47-4017-b4a7-4c436f303525.edn.tmp","id":"2026-09-08T21-34-43-135693186Z-e4c7906a-3b47-4017-b4a7-4c436f303525","trigger":[],"closure":null,"session":"5d41c2f3-fe9d-4fbf-88da-96549c4741a2"}
FAIL {"v":1,"candidate":"6a482e577bb90798ff6bca7705a9e7d665d0c10b214e4a49a6b60e7e68f81ba7","file":null,"mode":"probe","unavailable":["context","expr","expected","diff"],"event":1,"line":null,"expr":null,"id":"2026-09-08T21-34-43-135693186Z-e4c7906a-3b47-4017-b4a7-4c436f303525","kind":"load","expected":null,"truncated":["actual"],"context":null,"actual":"java.io.FileNotFoundException: Could not locate marvin_voice_remote/no_such_namespace_test__init.class, marvin_voice_remote/no_such_namespace_test.clj or marvin_voice_remote/no_such_namespace_test.cljc on classpath. Please check that namespaces wit","test":null,"diff":null}
STATUS {"receipt":null,"identity":"unknown","elapsed_ms":null,"v":1,"phase":"unknown","state":"refused","token":null,"reason":"protocol-error","active":"/var/tmp/forge/kaocha-defects-2/mvr/target/kaocha-runs/2026-09-08T21-34-43-135693186Z-e4c7906a-3b47-4017-b4a7-4c436f303525.edn.tmp","id":"2026-09-08T21-34-43-135693186Z-e4c7906a-3b47-4017-b4a7-4c436f303525"}
```

Baseline next probe on the same image; exit 0

```text
RUN-START {"v":1,"candidate":"895771c1f06d794be5125daa1851c969ce7c6fdc5cb3c5bed42c1384e1e7228b","trigger_omitted":0,"mode":"probe","active":"/var/tmp/forge/kaocha-defects-2/mvr/target/kaocha-runs/2026-09-08T21-34-43-395542450Z-c9b76a9a-5b3f-40b1-8ca9-7ee80f826480.edn.tmp","id":"2026-09-08T21-34-43-395542450Z-c9b76a9a-5b3f-40b1-8ca9-7ee80f826480","trigger":[],"closure":null,"session":"5d41c2f3-fe9d-4fbf-88da-96549c4741a2"}
RUN-DONE {"receipt":"/var/tmp/forge/kaocha-defects-2/mvr/target/kaocha-runs/2026-09-08T21-34-43-395542450Z-c9b76a9a-5b3f-40b1-8ca9-7ee80f826480.edn","omitted":0,"v":1,"parent":null,"candidate":"895771c1f06d794be5125daa1851c969ce7c6fdc5cb3c5bed42c1384e1e7228b","tests":4,"pending":0,"auto_followup":false,"wall_ms":148,"mode":"probe","coverage":"complete","outcome":"pass","shown":0,"skipped":575,"fail":0,"id":"2026-09-08T21-34-43-395542450Z-c9b76a9a-5b3f-40b1-8ca9-7ee80f826480","error":0,"sha256":"a5d76c6ccb77fcaab5e5788ab2e2e3f3b6c7da2cd61717e9bf3a6e6200e48b7f","pass":40,"assertions":40,"closure":{"selected":1,"total":54}}
```

Fixed namespace probe: requested scope and separate focus omissions; exit 0

```text
RUN-START {"v":1,"candidate":"3178b39f17a9bba13a53b71a201cad430214f44b1acfcc271aa3a1bd6ee88542","trigger_omitted":0,"mode":"probe","active":"/var/tmp/forge/kaocha-defects-2/mvr/target/kaocha-runs/2026-09-08T21-45-01-655843776Z-ca58c2d3-ffd9-4f14-818d-482d6867c8be.edn.tmp","id":"2026-09-08T21-45-01-655843776Z-ca58c2d3-ffd9-4f14-818d-482d6867c8be","trigger":[],"closure":null,"session":"1b90d590-fc84-497d-9cbe-0e771ae58a8e"}
RUN-DONE {"receipt":"/var/tmp/forge/kaocha-defects-2/mvr/target/kaocha-runs/2026-09-08T21-45-01-655843776Z-ca58c2d3-ffd9-4f14-818d-482d6867c8be.edn","omitted":0,"v":1,"parent":null,"candidate":"3178b39f17a9bba13a53b71a201cad430214f44b1acfcc271aa3a1bd6ee88542","tests":4,"pending":0,"auto_followup":false,"skipped_by_focus":575,"wall_ms":5571,"mode":"probe","coverage":"complete","outcome":"pass","shown":0,"scope":{"kind":"probe","requested":["marvin-voice-remote.over-test"]},"skipped":0,"fail":0,"id":"2026-09-08T21-45-01-655843776Z-ca58c2d3-ffd9-4f14-818d-482d6867c8be","closure_basis":"discovered-namespaces-v1","error":0,"sha256":"fa9bd27dd698e484d51aa23064111555adc10a499f5cf656c9a0533c8153b909","pass":40,"assertions":40,"closure":{"selected":1,"total":54}}
```

Fixed single-Var probe: same total 54, complete requested Var scope; exit 0

```text
RUN-START {"v":1,"candidate":"22c1fb83e54e4d4bc154dabc908c138ec35d7ea510c98da744da1985fef9ef7f","trigger_omitted":0,"mode":"probe","active":"/var/tmp/forge/kaocha-defects-2/mvr/target/kaocha-runs/2026-09-08T21-45-07-491323571Z-c56e8458-464a-44ce-be55-fa1a900df510.edn.tmp","id":"2026-09-08T21-45-07-491323571Z-c56e8458-464a-44ce-be55-fa1a900df510","trigger":[],"closure":null,"session":"1b90d590-fc84-497d-9cbe-0e771ae58a8e"}
RUN-DONE {"receipt":"/var/tmp/forge/kaocha-defects-2/mvr/target/kaocha-runs/2026-09-08T21-45-07-491323571Z-c56e8458-464a-44ce-be55-fa1a900df510.edn","omitted":0,"v":1,"parent":null,"candidate":"22c1fb83e54e4d4bc154dabc908c138ec35d7ea510c98da744da1985fef9ef7f","tests":1,"pending":0,"auto_followup":false,"skipped_by_focus":578,"wall_ms":134,"mode":"probe","coverage":"complete","outcome":"pass","shown":0,"scope":{"kind":"probe","requested":["marvin-voice-remote.over-test/over-signoff?-test"]},"skipped":0,"fail":0,"id":"2026-09-08T21-45-07-491323571Z-c56e8458-464a-44ce-be55-fa1a900df510","closure_basis":"discovered-namespaces-v1","error":0,"sha256":"471ebb9e87caaba8cdc4b6c1fbf3da1680c09ac9014ff23070c45509b6f52964","pass":9,"assertions":9,"closure":{"selected":1,"total":54}}
```

Fixed typo: FAIL kind=load followed by refused/load-error; exit 2

```text
RUN-START {"v":1,"candidate":"21fbad6d9935fbcc08d1b7bb22c476e73b658272eb8a9196964cde19a180f91b","trigger_omitted":0,"mode":"probe","active":"/var/tmp/forge/kaocha-defects-2/mvr/target/kaocha-runs/2026-09-08T21-45-07-863246421Z-d6df7353-8c99-44b5-a4f7-db11bcb51eb9.edn.tmp","id":"2026-09-08T21-45-07-863246421Z-d6df7353-8c99-44b5-a4f7-db11bcb51eb9","trigger":[],"closure":null,"session":"1b90d590-fc84-497d-9cbe-0e771ae58a8e"}
FAIL {"v":1,"candidate":"21fbad6d9935fbcc08d1b7bb22c476e73b658272eb8a9196964cde19a180f91b","file":null,"mode":"probe","unavailable":["context","expr","expected","diff"],"event":1,"line":null,"expr":null,"id":"2026-09-08T21-45-07-863246421Z-d6df7353-8c99-44b5-a4f7-db11bcb51eb9","kind":"load","expected":null,"truncated":["actual"],"context":null,"actual":"java.io.FileNotFoundException: Could not locate marvin_voice_remote/no_such_namespace_test__init.class, marvin_voice_remote/no_such_namespace_test.clj or marvin_voice_remote/no_such_namespace_test.cljc on classpath. Please check that namespaces wit","test":null,"diff":null}
STATUS {"receipt":null,"identity":"unknown","elapsed_ms":null,"v":1,"phase":"unknown","state":"refused","token":null,"reason":"load-error","active":"/var/tmp/forge/kaocha-defects-2/mvr/target/kaocha-runs/2026-09-08T21-45-07-863246421Z-d6df7353-8c99-44b5-a4f7-db11bcb51eb9.edn.tmp","id":"2026-09-08T21-45-07-863246421Z-d6df7353-8c99-44b5-a4f7-db11bcb51eb9"}
```

Fixed next probe on the same image; exit 0

```text
RUN-START {"v":1,"candidate":"3178b39f17a9bba13a53b71a201cad430214f44b1acfcc271aa3a1bd6ee88542","trigger_omitted":0,"mode":"probe","active":"/var/tmp/forge/kaocha-defects-2/mvr/target/kaocha-runs/2026-09-08T21-45-08-120661189Z-f772cc32-f932-44c0-8b84-badebba43800.edn.tmp","id":"2026-09-08T21-45-08-120661189Z-f772cc32-f932-44c0-8b84-badebba43800","trigger":[],"closure":null,"session":"1b90d590-fc84-497d-9cbe-0e771ae58a8e"}
RUN-DONE {"receipt":"/var/tmp/forge/kaocha-defects-2/mvr/target/kaocha-runs/2026-09-08T21-45-08-120661189Z-f772cc32-f932-44c0-8b84-badebba43800.edn","omitted":0,"v":1,"parent":null,"candidate":"3178b39f17a9bba13a53b71a201cad430214f44b1acfcc271aa3a1bd6ee88542","tests":4,"pending":0,"auto_followup":false,"skipped_by_focus":575,"wall_ms":149,"mode":"probe","coverage":"complete","outcome":"pass","shown":0,"scope":{"kind":"probe","requested":["marvin-voice-remote.over-test"]},"skipped":0,"fail":0,"id":"2026-09-08T21-45-08-120661189Z-f772cc32-f932-44c0-8b84-badebba43800","closure_basis":"discovered-namespaces-v1","error":0,"sha256":"c8f150342965df0d66ede6180176b2214904cf7407db0019a2e4c9d6b24a98e5","pass":40,"assertions":40,"closure":{"selected":1,"total":54}}
```

Fixed valid plus nonexistent Var: partial/incomplete despite one passing Var; exit 2

```text
RUN-START {"v":1,"candidate":"dbef8a591899929b31a4dba49d9dd9591f9646134e0c50b85bf1a91a57904958","trigger_omitted":0,"mode":"probe","active":"/var/tmp/forge/kaocha-defects-2/mvr/target/kaocha-runs/2026-09-08T21-45-08-502442628Z-0bb2af84-eb44-4648-8e7c-76aab151b0f1.edn.tmp","id":"2026-09-08T21-45-08-502442628Z-0bb2af84-eb44-4648-8e7c-76aab151b0f1","trigger":[],"closure":null,"session":"1b90d590-fc84-497d-9cbe-0e771ae58a8e"}
RUN-DONE {"receipt":"/var/tmp/forge/kaocha-defects-2/mvr/target/kaocha-runs/2026-09-08T21-45-08-502442628Z-0bb2af84-eb44-4648-8e7c-76aab151b0f1.edn","omitted":0,"v":1,"parent":null,"candidate":"dbef8a591899929b31a4dba49d9dd9591f9646134e0c50b85bf1a91a57904958","tests":1,"pending":0,"auto_followup":false,"skipped_by_focus":578,"wall_ms":113,"mode":"probe","coverage":"partial","outcome":"incomplete","shown":0,"scope":{"kind":"probe","requested":["marvin-voice-remote.over-test/over-signoff?-test","marvin-voice-remote.over-test/no-such-test"]},"skipped":0,"fail":0,"id":"2026-09-08T21-45-08-502442628Z-0bb2af84-eb44-4648-8e7c-76aab151b0f1","closure_basis":"discovered-namespaces-v1","error":0,"sha256":"790c03cad5b6ea486c8758d909649e31a2b7f6d52252b25cc8d31702774bb644","pass":9,"assertions":9,"closure":{"selected":1,"total":54}}
```

Fixed declared-skip witness: 1 configured exclusion and 579 focus omissions; exit 0

```text
RUN-START {"v":1,"candidate":"1a1e510e33347f5ebae32d35db3446ae81be1525ee08d93e000e5b3242e271b7","trigger_omitted":0,"mode":"probe","active":"/var/tmp/forge/kaocha-defects-2/mvr/target/kaocha-runs/2026-09-08T21-45-35-923871942Z-272ba2da-f702-4328-ba30-9f53b9e518c4.edn.tmp","id":"2026-09-08T21-45-35-923871942Z-272ba2da-f702-4328-ba30-9f53b9e518c4","trigger":[],"closure":null,"session":"1b90d590-fc84-497d-9cbe-0e771ae58a8e"}
RUN-DONE {"receipt":"/var/tmp/forge/kaocha-defects-2/mvr/target/kaocha-runs/2026-09-08T21-45-35-923871942Z-272ba2da-f702-4328-ba30-9f53b9e518c4.edn","omitted":0,"v":1,"parent":null,"candidate":"1a1e510e33347f5ebae32d35db3446ae81be1525ee08d93e000e5b3242e271b7","tests":1,"pending":0,"auto_followup":false,"skipped_by_focus":579,"wall_ms":93,"mode":"probe","coverage":"complete","outcome":"pass","shown":0,"scope":{"kind":"probe","requested":["marvin-voice-remote.scope-contract-witness-test"]},"skipped":1,"fail":0,"id":"2026-09-08T21-45-35-923871942Z-272ba2da-f702-4328-ba30-9f53b9e518c4","closure_basis":"discovered-namespaces-v1","error":0,"sha256":"3462cc90ece13ec7fb0b7f940802653c1ed6088eb528a6f26aa9585efa6d0adc","pass":1,"assertions":1,"closure":{"selected":1,"total":55}}
```

Real full-token FOLLOW of a clean fixed warm probe; exit 2, identity remains unknown

```text
STATUS {"receipt":"/var/tmp/forge/kaocha-defects-2/mvr/target/kaocha-runs/2026-09-08T21-45-56-856007300Z-f691c9cd-188f-49f6-ac32-789daca28c5c.edn","identity":"unknown","elapsed_ms":null,"v":1,"phase":"unknown","state":"unavailable","token":"44b9bc37e8f074079334ca1cad9cad5a12b3945ee566abec733b1ca08c4fcbc0","reason":"identity-unproved","active":null,"id":"2026-09-08T21-45-56-856007300Z-f691c9cd-188f-49f6-ac32-789daca28c5c"}
```

Fixed cold gate on the specimen with only dependency pins changed; exit 0

```text
RUN-START {"v":1,"candidate":"b92474b2f15906867768b2d8c3eeabf9b1392420ed1ea50b16b5e168551fb4bc","trigger_omitted":0,"mode":"gate","active":"/var/tmp/forge/kaocha-defects-2/mvr/target/kaocha-runs/gate-20260908T214623-4164404-d292788330fc.edn.tmp","id":"gate-20260908T214623-4164404-d292788330fc","trigger":[],"closure":null,"session":"a4ecaf7b-5cf0-45b4-88e2-69a70c595212"}
RUN-DONE {"receipt":"/var/tmp/forge/kaocha-defects-2/mvr/target/kaocha-runs/gate-20260908T214623-4164404-d292788330fc.edn","omitted":0,"v":1,"parent":null,"candidate":"b92474b2f15906867768b2d8c3eeabf9b1392420ed1ea50b16b5e168551fb4bc","tests":579,"pending":0,"auto_followup":false,"skipped_by_focus":0,"wall_ms":14139,"mode":"gate","coverage":"complete","outcome":"pass","shown":0,"scope":{"kind":"full","requested":[]},"skipped":0,"fail":0,"id":"gate-20260908T214623-4164404-d292788330fc","closure_basis":"discovered-namespaces-v1","error":0,"sha256":"d7b71cc993b55146363a93175b2d6bb037c9981095cb218491f8d842d03ee7de","pass":7833,"assertions":7833,"closure":{"selected":54,"total":54}}
```

Full-token FOLLOW of that fixed cold gate; exit 0

```text
RUN-DONE {"receipt":"/var/tmp/forge/kaocha-defects-2/mvr/target/kaocha-runs/gate-20260908T214623-4164404-d292788330fc.edn","omitted":0,"v":1,"parent":null,"candidate":"b92474b2f15906867768b2d8c3eeabf9b1392420ed1ea50b16b5e168551fb4bc","tests":579,"pending":0,"auto_followup":false,"skipped_by_focus":0,"wall_ms":14139,"mode":"gate","coverage":"complete","outcome":"pass","shown":0,"scope":{"kind":"full","requested":[]},"skipped":0,"fail":0,"id":"gate-20260908T214623-4164404-d292788330fc","closure_basis":"discovered-namespaces-v1","error":0,"sha256":"d7b71cc993b55146363a93175b2d6bb037c9981095cb218491f8d842d03ee7de","pass":7833,"assertions":7833,"closure":{"selected":54,"total":54}}
```

