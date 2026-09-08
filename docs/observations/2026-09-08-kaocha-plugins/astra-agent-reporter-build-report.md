Built and pushed the agent grammar v1 implementation to `origin/master` at `9b837026292ca4517c523b8e63ea19c063f22a8e`. Items 2 and 3, plus item 1's plugin heap-admission half, are implemented in `/home/forge/src/kaocha-sublime`. Final implementation source is `a4da89b`; the later commit refreshes accepted evidence. The worktree is clean and local/remote master agree. Completed 2026-09-08 19:33 UTC, before the two-hour checkpoint of the three-hour timebox (work began about 17:53 UTC).

The adopted grammar block is retained unchanged in [docs/agent-v1.md](/home/forge/src/kaocha-sublime/docs/agent-v1.md). The implementation uses fixed ASCII prefixes followed by compact JSON, a separate STATUS shape, content-manifest MARK identity, and one runner-owned event file per run. `kaocha-status` projects authenticated receipt data. START precedes load/rescan; DONE follows final receipt rename and atomic LATEST publication. No heartbeat is emitted by default.

The small commits on master, all pushed:

```text
1a17a08 test: pin agent grammar identity and bounded output intents red
8f9ad07 feat: encode bounded agent records and content candidate manifests
2b94d97 feat: bind agent lifecycle and token follow to atomic run receipts
8ac3b78 fix: attest loaded bytes and close agent admission and reload edges
7e91384 docs: specify agent adapters and add bounded process acceptance harnesses
d0177f9 test: retain final agent lifecycle and MVR 512m acceptance receipts
a4da89b fix: extract concrete equality evidence without realizing lazy tails
9b83702 test: refresh accepted records after concrete equality projection fix
```

MARK binds canonical workspace, runner session, config/classpath generation, required full-suite or namespace scope, and content hashes including deleted requested paths. Identical candidate/scope marks coalesce. FOLLOW emits exactly one STATUS or matching RUN-DONE: exit 0 requires exact candidate, required coverage and pass; exit 1 proves an exact candidate's failure/error; exit 2 covers unknown, incomplete, refused, timeout and other non-accepting states. `--since` remains available with identity unknown; `--legacy` is explicitly human compatibility and cannot be combined into token acceptance.

Loaded-byte evidence is stronger than equal pre/post file hashes. The watcher binds the actual accepted event generation to reload; hashes repair same-mtime tracker misses; inode change times reject transient save-and-restore. Cold Kaocha entry can attest its initial image. An ordinary pre-existing warm REPL image emits probe records but stays identity unknown until its loaded image is attested. Config/classpath changes require restart. This conservative boundary prevents disk-green from becoming an unsupported claim about warm Vars.

FAIL previews are bounded before printing: expected/actual 256 encoded ASCII bytes each, expression/context 160, diff 512; traversal depth 6, 128 visited nodes, cooperative 10 ms. Lazy values and custom objects remain opaque. Safe concrete equality evidence yields up to three changed paths from `clojure.data/diff`; normal clojure.test Cons wrappers are unwrapped only when their stored tail is a concrete list. An arbitrary lazy Cons tail is never realized. Each FAIL is at most 2 KiB; each run shows at most 20 FAIL events and at most 16 KiB of FAIL lines. DONE retains exact shown/omitted counts. Captured output has separate 16 KiB/test and 64 KiB/run diagnostic caps.

The linked-intent-testing skill was applied from `/home/forge/src/claude-skills-nrepl/linked-intent-testing/SKILL.md`. Eight new stable intents AGENT-CANDIDATE-006 through AGENT-BUDGET-013 join the five existing STATUS intents in [the registry](/home/forge/src/kaocha-sublime/docs/intent/registry.edn). The normal test suite enforces both directions between registry, implementation and test tags. Red evidence includes the initial missing implementation, diff/schema, capture, stale-green heap refusal, START trigger timing and real equality-event regressions. The last regression first failed four assertions because safe equality values were opaque; it then passed with an additional lazy-tail non-realization check. These are retained failures, not reconstructed predictions.

The final checks are **49 tests / 353 assertions, zero failures or errors**, and `~/bin/clj-kondo --lint src test` with **zero errors and warnings**. Final process, watch and MVR harnesses all returned success after source freeze. Legacy process and affected CLI regressions also passed on the final implementation. [The evidence index](/home/forge/src/kaocha-sublime/evidence/agent-v1/README.md) distinguishes accepted logs from exploratory failures; [acceptance.json](/home/forge/src/kaocha-sublime/evidence/agent-v1/acceptance.json) records the source revision, results and timing vectors. Fifteen retained DONE records have receipt copies with matching SHA256 digests.

| Witness | Observed result | Evidence |
|---|---|---|
| Fast save/run before MARK | Completed content matches; FOLLOW returns byte-identical DONE, exit 0 | `process/agent_witnesses.json`, clean stream |
| Identical marks | Same content and required scope yield the same token | Process harness assertions |
| Overwrite, preserving mtime | Original token returns superseded, exit 2 | Superseded STATUS below |
| New candidate versus old LATEST green | New token times out, exit 2; no borrowed green | `old-green-refused` trace |
| Edit outside watched roots | Uncovered, scope-missing, exit 2 | `uncovered` trace |
| Deletion manifest and isolated namespace | Deleted requested path retained; watcher reload removes stale namespace Vars after Enter | Unit tests and `watch-deletion-after-enter` |
| Required coverage | Full scope rejects focused/partial/pending results; namespace scope requires all tests; exact red remains exit 1 | Acceptance state-space and primary/coverage tests |
| Automatic follow-up | Child carries parent ID/digest and `auto_followup=true`; FOLLOW selects the matching primary | `automatic-followup-primary`, watch receipts |
| Running then SIGKILL | Exact active PID/start identity; killed STATUS, exit 2; START remains and DONE is absent | `killed-active.edn`, `killed.events`, watch trace |
| PID reuse / reap | Wrong start identity never becomes live/green; interrupted identity retained | Dead/recycled PID and reap unit tests |
| START / DONE publication | START exists before loading/rescan with actual trigger batch; no DONE at LATEST publication entry; receipt already exists | Publication and watcher trigger tests |
| Assertion and load errors | FAIL kind fail/error/load; load has null test Var, exact available source location; terminal error outranks fail | Unit projection, load stream, watch reload-error trace |
| Huge lazy map / throwing printer | 100,000-entry map with infinite lazy values stays bounded; realization counter remains zero; throwing print-method is never called | `unsafe-values` process fixture and encoder tests |
| Forged newline, ANSI, bidi in test name | Exactly one valid ASCII JSON record; escaped data cannot introduce another prefix | `forged-test-name-and-large-preview-cannot-inject-records` |
| Forged DONE in stdout | Captured as diagnostics; clean owned stream contains only genuine START/DONE | Clean fixture and its 25-byte diagnostic |
| Byte and event budgets | 25 failures: 20 shown, 5 omitted; separate unit case reaches byte cap before event cap | Red stream below; budget tests |
| Ordinary equality map | Concrete expected/actual and changed invoice total included without evaluation or lazy traversal | FAIL below; equality-wrapper red/green regression |
| Warm / cold modes | Two warm repl/run calls have distinct probe records and returned paths; CLI is gate; plain warm identity stays unknown | `warm-probe` streams and `unattested-warm-image` |
| Transient edit then restore | Hash equality cannot certify changed loaded bytes: identity unknown, exit 2 | `cold-transient-refused`, inode test |
| Watch reload failure and repair | Exact red error, then fresh complete green after full watched-root recovery | `watch-load-error`, `watch-reload-repaired` |
| Heap admission | Explicit requested/effective 512 MiB accepted; absent argument evidence refuses before test load with reason heap-cap; stale SESSION green also refused | Heap tests and `heap-plugin-refusal` trace |
| Full MVR suite at 512 MiB | 579 tests, 7,833 assertions, closure 54/54, complete pass; full-scope FOLLOW exits 0 | `mvr.events`, `mvr.edn`, MVR trace |
| Existing receipt/selection behavior | Exact-PID kill, isolated concurrent JVMs, warm first load, fixed-ID collision; six affected-selection CLI cases pass | `legacy-process.log`, `legacy-cli.log` |

Paths in the table are relative to [evidence/agent-v1](/home/forge/src/kaocha-sublime/evidence/agent-v1); named process traces and copied streams live in its `process/` directory. Unit witnesses are in [agent_test.clj](/home/forge/src/kaocha-sublime/test/kaocha_sublime/agent_test.clj) and the existing receipt/status suites.

These are the **real records verbatim**, read from the accepted owned streams and STATUS transcripts. JSON member order is unchanged. The clean run emitted exactly these two lines (1,050 bytes including LF):

```text
RUN-START {"v":1,"candidate":"2e94156b11661a566f617e565851a72866cb5f108bae73e994c1c627488fa341","trigger_omitted":0,"mode":"gate","active":"/var/tmp/forge/kaocha-agent-v1/clean/target/kaocha-runs/2026-09-08T19-29-23-008990389Z-0d8c20d0-1acc-481f-b555-d16ce5e5ac30.edn.tmp","id":"2026-09-08T19-29-23-008990389Z-0d8c20d0-1acc-481f-b555-d16ce5e5ac30","trigger":[],"closure":null,"session":"c79a7aeb-5337-41c6-9e86-80270228210c"}
RUN-DONE {"receipt":"/var/tmp/forge/kaocha-agent-v1/clean/target/kaocha-runs/2026-09-08T19-29-23-008990389Z-0d8c20d0-1acc-481f-b555-d16ce5e5ac30.edn","omitted":0,"v":1,"parent":null,"candidate":"2e94156b11661a566f617e565851a72866cb5f108bae73e994c1c627488fa341","tests":1,"pending":0,"auto_followup":false,"wall_ms":142,"mode":"gate","coverage":"complete","outcome":"pass","shown":0,"skipped":0,"fail":0,"id":"2026-09-08T19-29-23-008990389Z-0d8c20d0-1acc-481f-b555-d16ce5e5ac30","error":0,"sha256":"2c7d89ea28c9e5721f56cda8c37aba94629df7bff2349d72ddd6159b0445df48","pass":1,"assertions":1,"closure":{"selected":1,"total":1}}
```

The first FAIL of the 25-failure run:

```text
FAIL {"v":1,"candidate":"e3a09e1865d226dfcf8ddf5e44a32cc98aca0c3ff6918f0e84261b7e6f0aface","file":"test/agent_case/core_test.clj","mode":"gate","unavailable":[],"event":1,"line":2,"expr":"(= {:invoice {:total 42}} {:invoice {:total 43}})","id":"2026-09-08T19-29-27-316845095Z-82ecc766-be41-4b32-9274-ee91cfb9ac93","kind":"fail","expected":"{:invoice {:total 42}}","truncated":[],"context":"invoice total","actual":"{:invoice {:total 43}}","test":"agent-case.core-test/red","diff":"[{:path [:invoice :total], :expected 42, :actual 43}]"}
```

Its DONE preserves total failure and shown/omitted counts:

```text
RUN-DONE {"receipt":"/var/tmp/forge/kaocha-agent-v1/fail25/target/kaocha-runs/2026-09-08T19-29-27-316845095Z-82ecc766-be41-4b32-9274-ee91cfb9ac93.edn","omitted":5,"v":1,"parent":null,"candidate":"e3a09e1865d226dfcf8ddf5e44a32cc98aca0c3ff6918f0e84261b7e6f0aface","tests":1,"pending":0,"auto_followup":false,"wall_ms":186,"mode":"gate","coverage":"complete","outcome":"fail","shown":20,"skipped":0,"fail":25,"id":"2026-09-08T19-29-27-316845095Z-82ecc766-be41-4b32-9274-ee91cfb9ac93","error":0,"sha256":"dff91af4880ed9997d83cd493b8efa0f1faf65a524528fb9cf3ef6ee62560e9a","pass":0,"assertions":25,"closure":{"selected":1,"total":1}}
```

The superseded STATUS:

```text
STATUS {"receipt":null,"identity":"unknown","elapsed_ms":null,"v":1,"phase":"unknown","state":"superseded","token":"92de50b0eaade5ce02a8ada17e57fcf5969a795940b429eeb5aed49add5ac044","reason":"candidate-changed","active":null,"id":null}
```

The killed STATUS:

```text
STATUS {"receipt":null,"identity":"exact","elapsed_ms":322,"v":1,"phase":"run","state":"killed","token":"2a1bebb6fc34037079753f86ac05ebd089d50ca42b78f93152abee7216bebece","reason":"process-dead","active":"/var/tmp/forge/kaocha-agent-v1/watch/target/kaocha-runs/2026-09-08T19-29-24-443267647Z-8ee3e414-5f36-410b-9488-00e0cecc8981.edn.tmp","id":"2026-09-08T19-29-24-443267647Z-8ee3e414-5f36-410b-9488-00e0cecc8981"}
```

The killed process was PID `1130173` with start identity `2026-09-08T19:29:18.970Z`. Its [active reservation](/home/forge/src/kaocha-sublime/evidence/agent-v1/process/killed-active.edn) remains unfinished. The [owned interrupted stream](/home/forge/src/kaocha-sublime/evidence/agent-v1/process/killed.events) contains START only; SIGKILL did not manufacture DONE or revive the older LATEST pass.

| Final measurement | Clean fixture | 25-failure fixture | MVR full suite |
|---|---:|---:|---:|
| Protocol bytes, including LF | 1,050 | 11,806 | 1,088 |
| Protocol records | 2 | 22 | 2 |
| FAIL shown / omitted | 0 / 0 | 20 / 5 | 0 / 0 |
| Whole JVM process wall, seconds | 3.528 | 2.792 | 18.828 |
| Receipt RUN-DONE wall, milliseconds | 142 | 186 | 14606 |
| FOLLOW wall, 10-call median, milliseconds | 93.898 | 95.978 | 135.788 |

The ten raw FOLLOW samples, in milliseconds:

```text
clean: 92.876608, 105.177307, 97.594613, 94.528405, 93.267383, 89.147905, 111.829184, 105.698018, 87.659913, 88.504109
25-failure: 95.593652, 99.231199, 93.460093, 92.487895, 89.444728, 92.276057, 96.361615, 98.819230, 103.324618, 99.176246
mvr: 138.712137, 151.037312, 136.040170, 133.560109, 134.238177, 136.154360, 147.815501, 135.536133, 132.314079, 129.800570
```

Each FOLLOW measurement includes launching a fresh Babashka process and authenticating an already completed receipt; it is not edit-to-verified latency. The final process/watch/MVR harnesses ran concurrently on the shared host. These are absolute observations, not an idle-host promise or a speedup comparison. Protocol-byte figures exclude receipt storage and diagnostics: the clean fixture separately captured 25 bytes of forged stdout, the red fixture captured zero, and each process log had 61 bytes of JAVA_TOOL_OPTIONS diagnostics. No raw stdout supplied protocol evidence.

Every Kaocha JVM started during this work carried `-Xms64m -Xmx512m`. MVR ran in the owned detached worktree `/var/tmp/forge/kaocha-agent-v1/mvr-equality-final`, specimen commit `9439370`, with guardrails enabled and the isolated JSONL test store. Its receipt records `requested-xms=-Xms64m`, `requested-xmx=-Xmx512m`, requested/effective/ceiling all **536870912 bytes**, and admission true. The owned process deadline was 180 seconds. Peak sampled RSS was **632,148 KiB (617.3 MiB)**; RSS includes native/JVM overhead and is not the Java heap cap. The actual command, classpath, PID, wall, and complete result are retained in [the MVR trace](/home/forge/src/kaocha-sublime/evidence/agent-v1/process/agent_mvr_witness.json).

To reconcile the mandatory caps with testing missing cap evidence, the absence witness injects an empty admission input-argument view into an **actually capped** JVM. It observes `STATUS refused` with reason `heap-cap` and proves the test body was never loaded. No uncapped Kaocha JVM was launched. Unit state-space checks separately exercise missing explicit evidence, excessive requested/effective caps, and permitted 512 MiB admission. Ordinary non-agent receipts also record requested/effective heap facts.

Fable's **item 4 remains adapter work**: migrate `make test-probe`, nREPL envelopes and coldstart-grade/R12 using [the documented old PROBE/GATE adapter contract](/home/forge/src/kaocha-sublime/docs/agent-v1.md). Parse the owned stream; reject unknown versions/keys, duplicate keys, malformed/oversize/conflicting records; authenticate receipt schema/digest, workspace, session/image/config, token and required gates independently. Deduplicate by run/type and FAIL event number. Preserve full required coverage when selecting a primary or focused follow-up, and recover a committed receipt if terminal output was lost. Old PROBE/GATE text alone cannot prove candidate identity; non-Kaocha counts must remain null. Warm pass cannot discharge a separately required cold gate. Var-changing eval outside the owned reload must call `kaocha-sublime.image/invalidate!`. The plugin half of heap admission is complete here; specimen launch-cap adoption and bounded bypass-output capture remain batch 5's launcher work.

Fable's **item 5 remains the independent blind-reader and crossover witness**. Supply these clean, FAIL, load-error, unsafe-value, superseded and killed artifacts without coaching. Ask whether it is running, whether it is the reader's candidate, whether required checks passed, and where the failure is; retain wrong answers and extra receipt reads. Then run the matched warm crossover with identical checks/caps and count apparatus actions, model returns, emitted bytes and edit-to-verified wall. The automated process reader here is a correctness oracle, not that blinded usability witness. No comparative performance gain, model-action reduction, free-choice adoption or blind-reader success is claimed.

The remaining implementation limits are explicit: exact image attestation currently requires Linux inode change-time support; unsupported observation stays unknown. Upstream Kaocha scheduling still misses some rename saves and ignores delete notifications, so the deletion process witness used Enter to schedule rescan. MARK/FOLLOW never fabricates a run. Config/classpath edits require bounded restart, and untracked eval requires image invalidation. Cooperative filesystem/run identity does not authenticate arbitrary in-process tampering, runtime-only coupling or unreported external state. Raw output bypassing Kaocha capture remains launcher-owned. These boundaries are documented rather than converted into accepted green.
