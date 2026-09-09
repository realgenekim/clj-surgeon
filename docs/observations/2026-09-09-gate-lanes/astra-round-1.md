# Astra gate lanes — timeboxed fence report

Written 2026-09-09T04:41:24.519911+00:00

**NO-GO: implementation is committed; the requested fence is incomplete.**

Commit `ebaf7aec0dbdc1b77f36cc70a3db91c71bd5a180` on `astra/gate-lanes`, from `d2c3aa806eb0addda0ca20b8135e6d61d74f5742`. Author/committer `forge-anvil <forge-anvil@anvil>`; trailer `Co-Authored-By: Gene Kim <genek@itrevolution.com>`. No push.

Three timed attempts were made at the selected width; **2 completed the entire gate successfully**. A three-success performance fence was not obtained. One retained four-worker full attempt stopped because `mcp-compact-relations-test` took **8,002 ms against its unchanged 8,000 ms budget**. This is a reliability concern, not a passing performance result. Do not treat this report as approval for ship/Sol to land the branch.

The two-hour timebox began 02:42:38Z and ends 04:42:38Z. The requested interim report was written at 04:12:38Z. This report replaces it with actual completed evidence and the remaining work.

## Measure first: unchanged trunk

### Baseline before any change

Written 2026-09-09T02:44:26.940124+00:00

Tree d2c3aa806eb0addda0ca20b8135e6d61d74f5742. Inherited locked make test exited 0; timestamps 02:24:37–02:33:07 UTC (510 s at one-second resolution). Mixed stamp formats normalized by PID; nested commands are not added twice. Audit was absent from make test and measured separately on the unchanged tree.

| Phase / scope | Wall s | Exit |
|---|---:|---:|
| make --no-print-directory admit-transaction-recovery-battery | 12.467 | 0 |
| make --no-print-directory battery-fresh | 1.943 | 0 |
| make --no-print-directory alias-migration-test | 81.428 | 0 |
| clojure -J-Xms64m -J-Xmx512m -M:clj-surgeon/mcp-test | 99.358 | 0 |
| make --no-print-directory mcp-test | 216.902 | 0 |
| bb test/run_all.clj | 189.750 | 0 |
| make --no-print-directory test-bb | 191.715 | 0 |
| make --no-print-directory repository-hygiene | 1.977 | 0 |
| make --no-print-directory landing-gate | 495.986 | 0 |
| intent audit (separate unchanged-tree run) | 0.943 | 0 |

The mcp-test target includes fast+integration JVM (99.358 s), Python/shell oracles and eight shell self-tests. Babashka is one invocation, 49 namespaces. The Make admit target includes ~10 s recursive Make evaluation/runner setup; the recovery arms themselves are sub-second. Whole baseline plus separate audit: 510.943 s (rounded whole-run stamp).


Evidence: baseline-stamps.jsonl, baseline.log, baseline-exit=0, baseline-table.md, baseline-phases.json. The inherited completed baseline was explicitly transferred to this session. The table was written before implementation. Babashka was one process over 49 namespaces; JVM fast+integration was 62 namespaces.

## Whole make test after the change

| Attempt | Whole wall s | Exit | Landing receipt |
|---|---:|---:|---|
| timed-budget-red | 179.290 | 2 | no |
| timed-1 | 368.239 | 0 | yes |
| timed-2 | 366.205 | 0 | yes |

Observed passing-run median: **367.222 s**, compared with the approximately **510 s** baseline (1.39× baseline/final, 28.0% less wall). This is preliminary evidence from 2 completed runs, not the requested three-run acceptance proof. Final make test also includes the intent audit; the old Make gate did not.

| Required phase | timed-1 | timed-2 |
|---|---:|---:|
| admit-transaction-recovery-battery | 11.320 | 11.211 |
| battery-fresh | 2.122 | 1.980 |
| alias-migration-test | 82.064 | 82.467 |
| mcp-test | 193.409 | 191.388 |
| test-bb | 71.780 | 71.607 |
| repository-hygiene | 1.998 | 1.980 |
| intent-audit | 0.104 | 0.112 |

Process-pool makespans below exclude Make/coordinator startup and the MCP shell checks. The JVM pool includes its fast and integration waves.

| Process suite | timed-1 s | timed-2 s |
|---|---:|---:|
| alias | 78.648 | 79.275 |
| mcp | 73.788 | 73.655 |
| bb | 68.453 | 68.298 |

All full runs used `flock /home/forge/tmp/suite.lock`. Final timing attempts use the inherited shell-stamp wrapper and separate output paths. Nested phase/body walls are not added twice. Ambient shared-host contention remains a limitation; one old baseline is not a statistical distribution.

## Coordinator, Makefile and automatic width

There is one coordinator: test/clj_surgeon/battery_parallel_runner.clj, generalized in place from TEST-ISO-013. Battery keeps its existing entry and scheduling policy. Gate suites select the existing manifest (JVM), the sole BB vector in test/run_all.clj, and the original two alias/artifact namespaces.

- `make test` calls `landing-gate`, which invokes that same coordinator with `--suite gate`. It owns recovery, freshness, namespace suites, hygiene and intent audit.
- Existing `mcp-test`, `test-fast`, `test-bb` and `alias-migration-test` now use the coordinator. Original MCP Python oracles and shell self-tests remain in one shared Make recipe.
- `make test-serial` and component debugging targets print SERIAL/NOT-A-GATE and cannot publish a landing receipt. There is no GATE_LANES flag.
- The gate maximum is **four concurrent workers**. JVM fast workers all finish before any integration worker starts. The combined JVM suite launches eight child processes across two waves, never eight simultaneously. Whole namespaces preserve fixture/hook semantics; the battery per-Var splitter still refuses fixtures/test-ns-hook.

Formula: `min(4, max(1, floor(nproc/2)), floor((MemAvailableMiB-2048)/1536))`, additionally capped by suite namespace count. This host has 16 CPUs. Reserve 2048 MiB, charge 1536 MiB per worker (512 MiB heap plus native/child allowance); below 3584 MiB or unknown available memory refuses. Both JVM and BB workers, including BB temp-isolation re-exec, have a 512 MiB heap maximum. A JDK procfs bug made buffered slurp fail; direct NIO now reads memory. The invalid one-worker fallback probe was cancelled and is not eight-worker evidence.

Measured costs seed docs/observations/gate-namespace-walls.edn; later runs update ignored target timing data. Costs never determine membership. Each invocation uses private child receipt paths and removes old landing evidence before starting.

## Width probes and namespace parity

| Probe | Command wall s | Exit | Scope |
|---|---:|---:|---|
| control-current-alias | 82.967 | 0 | one lane |
| control-current-mcp | 102.000 | 0 | one lane |
| control-current-bb | 182.612 | 0 | one lane |
| tune-4-mcp | 54.784 | 0 | before global barrier |
| tune-4-bb | 69.119 | 0 | before global barrier |
| tune-8-mcp | 62.363 | 1 | before global barrier |
| candidate-final-mcp | 76.451 | 0 | global barrier |

Four/eight probes used the same measured seed. Eight-worker JVM admission failed existing isolation/time checks, so an eight-worker BB probe was not admitted. The original four-worker probes had identical per-namespace counters. The global barrier then addressed cross-process checkout observations; its JVM candidate passed in 76.451 s including coordinator startup. Four is the best observed admitted tested width, not a universal optimum.

Final one-lane controls were rerun on the exact commit: JVM alone, then alias and BB each in a single worker overlapping each other under the suite lock. Those overlapping control walls are not serial performance estimates. All controls pass. The battery comparator is reused, extended only to parse declared/executed Var fields and reject empty evidence. Each completed full gate is compared against these exact-commit controls, namespace by namespace.

| Runtime suite | Namespaces | Tests | Assertions | Control failures/errors/skips |
|---|---:|---:|---:|---|
| JVM fast+integration | 62 | 889 | 11096 | 0/0/0 |
| Babashka | 49 | 891 | 7888 | 0/0/0 |
| Alias/artifact | 2 | 164 | 3495 | 0/0/0 |

- timed-1/alias: all-zero per-namespace deltas
- timed-1/mcp: all-zero per-namespace deltas
- timed-1/bb: all-zero per-namespace deltas
- timed-2/alias: all-zero per-namespace deltas
- timed-2/mcp: all-zero per-namespace deltas
- timed-2/bb: all-zero per-namespace deltas

Baseline JVM was 883 tests / 11,028 assertions. Six new coordinator witnesses and a fixed-count loaded-namespace metadata audit produce 889 / 11,096. The audit still checks every loaded namespace, with one conditional assertion per manifest entry. Its former assertion count depended on co-loaded namespaces. BB and alias/artifact counts are unchanged. Tree-derived pins: 610 adopted / 1,635 manifest tests, 254 non-MCP intent IDs, 22 TEST-ISO IDs.

## Fault and isolation evidence

- Named deliberately failing `clj-surgeon.battery-parallel-test/gate-width-is-resource-bounded`: full make test exits **2**, exactly one assertion failure, no landing receipt. See fault-named-test.log/json.
- Lost shard: an alias child replaced with `sh -c "exit 0"` produces no result. The coordinator exits **1**, naming clj-surgeon.mcp-alias-migration-test. See fault-lost-shard.log and fault-lost-shard-exit.
- Unreadable test namespace declaration: a deliberately malformed test file is refused by tree census, exit **1**. See fault-unreadable-census.log/edn.
- Complete serial-debug entrance passed in 497.816 s earlier in development and issued no landing receipt. Final exact-commit component controls also use the explicit diagnostic route; the landing-eligibility witness remains green.
- JVM isolation and time budgets are folded over the union; BB retains its existing per-process temp-leak contract. No limit or allowlist was relaxed. Change-buffer fixtures now clear retained bases they own before and after tests.
- An eight-worker failure exposed integration checkout files in another worker’s fast snapshots. The global barrier fixes that interference. A later four-worker timed run still exceeded a namespace time budget by 2 ms; that red result is retained and is not dismissed as a pass.

## Floors and remaining serial work

| Suite | Slowest namespace in first complete passing run | Namespace wall s |
|---|---|---:|
| alias | clj-surgeon.mcp-alias-migration-test | 68.431 |
| mcp | clj-surgeon.mcp-feature-thread-test | 45.532 |
| bb | clj-surgeon.parser-admission-test | 68.255 |

A whole namespace is indivisible, so its wall is a floor for its phase, before startup. JVM phases and runtime suite stages are ordered; the complete gate floor includes their floors plus serial work, rather than only the globally slowest namespace. Recovery precedes consumers. Freshness, hygiene and audit remain serial authority checks. Original Python/shell checks retain their sequence and shared-file assumptions. Only one runtime pool is active at a time so the memory allowance applies coherently.

## Receipt fields and census

Landing receipt: `state`, `landing?`, `run-id`, `git-head`, `git-tree`, `started-at`, `completed-at`, `wall-ms`, `source-digest`, `capacity`, `namespace-census`, `stages`, `suites`, `problems`. Suite receipts include `runtime`, `state`, `debug`, run/source identity, `lane-count` (maximum concurrency), `process-count`, `phase-count`, expected/observed namespace lists and counts, each child phase/exit/log path, per-namespace counters/walls, declared/executed Vars, aggregate result, isolation/leak counts and problems.

Tree census is **149 discovered / 149 accounted**, exact sets and no duplicates. Runtime suite censuses separately require every selected namespace to execute. Accounting includes manifest, BB and explicit exclusions; it does not claim the landing gate reruns every cold battery. Missing, duplicate, malformed, failed, skipped or changed-tree evidence is red. Ordinary runs use declared/executed Var coverage as a cheap drift witness, not a second serial parity run.

## Existing callers

Captured 2026-09-09T02:56:02.775561+00:00

## /home/forge/bin/ship
SHA256 466f51a835497ae15b4d13e2f3280abb5f0d0624594f56128511d9687fdc057f
96: AUDIT_CMD=${SHIP_AUDIT_CMD:-"clojure -M -e (require 'clj-surgeon.mcp-intent-contract)(prn (select-keys (clj-surgeon.mcp-intent-contract/audit-current-repository) [:ok :violations]))"}
117: # Set SHIP_GATES_PREWARM_CMD='' to turn the lane off.
118: GATES_PREWARM_CMD=${SHIP_GATES_PREWARM_CMD-"$HOME/bin/suite-run make admit-transaction-recovery-battery mcp-test test-bb repository-hygiene"}
127: GATES_PHASE_B_CMD=${SHIP_GATES_PHASE_B_CMD-"$HOME/bin/suite-run make battery-fresh"}
133: GATES_AUDIT_CMD=${SHIP_GATES_AUDIT_CMD:-"clojure -M -e \"(require 'clj-surgeon.mcp-intent-contract)(prn (select-keys (clj-surgeon.mcp-intent-contract/audit-current-repository) [:ok :violations]))\""}
134: GATES_LANES=${SHIP_GATES_LANES:-"admit-transaction-recovery-battery battery-fresh mcp-test repository-hygiene test-bb"}
324:   line="SHIP status=$status run=$RUN candidate=$CAND base=$BASE landed=$LANDED ship_elapsed=${elapsed}s review_elapsed=$REVIEW_EL battery_elapsed=$BATTERY_EL gates_elapsed=$GATES_EL fastlane=$FASTLANE fastlane_elapsed=$FASTLANE_EL gates=$GATES_DISPOSITION attempts=$ATTEMPTS fix_attempts=$FIX_ATTEMPTS auto_closes=$AUTOFIXES initial_candidate=$INITIAL_CAND final_candidate=$CAND fix_commits=$(echo ${FIX_COMMITS:--} | tr ' ' ',') patch_producer=$(printf '%s' "${PATCH_PRODUCER:--}" | tr ' ' '_') independent_review=$INDEPENDENT_REVIEW reason=$STOP_REASON events_hash=$eh ledger_ref=$LEDGER request_to_landed=unknown"

## /home/forge/bin/land
SHA256 4a1eae94a7e289b631683d629a411e8f382ed5bbc17b8b556dffa88968635972
44: GATES_CMD=${LAND_GATES_CMD:-"~/bin/suite-run make test"}
45: AUDIT_CMD=${LAND_AUDIT_CMD:-"clojure -M -e \"(require 'clj-surgeon.mcp-intent-contract)(prn (select-keys (clj-surgeon.mcp-intent-contract/audit-current-repository) [:ok :violations]))\""}
88:   gates=("$GATES_CMD")

## /home/forge/bin/suite-run
SHA256 3ed03376913478a9e8453cfa9f3c87d983f40bea2149f54bc6f5687d6998f056
13:   if flock -n /home/forge/tmp/suite-$n.lock true 2>/dev/null; then exec flock /home/forge/tmp/suite-$n.lock nice -n "${SUITE_NICE:-10}" "$@"; fi
15: exec flock /home/forge/tmp/suite-$(( (RANDOM % 3) + 1 )).lock nice -n "${SUITE_NICE:-10}" "$@"

## /home/forge/bin/receipt-chain
SHA256 fedf998c2bb635cdd59fbc55b7d0350e2f5f8a01e27dd600776784df93e11e3d
83: # the recovery receipt before its consumer; this script called `make test-battery` DIRECTLY, so the
93: flock /home/forge/tmp/suite.lock make test-battery > "$BLOG" 2>&1; rc=$?


All four captured scripts pass bash -n. `caller-make-test-dry.log` and `caller-prewarm-dry.log` verify Make target resolution. Land has no dry mode; its merge/push path was not invoked. Land still calls suite-run make test, suite-run forwards arguments, and receipt-chain still calls locked make test-battery. Installed caller scripts were not edited.

The captured ship prewarm already omits alias/artifact. This pre-existing coverage gap is for Sol’s fence decision; prewarm alone is not claimed as complete landing proof. Captured hashes distinguish those observations from any later external script edits.

## Checks and next work

Focused regression checks: 120 tests / 1,186 assertions green; final global-barrier coordinator witnesses: 32 tests / 189 assertions green. Formatter and git diff checks pass. Lint ran through ~/bin/clj-kondo, with no new errors/warnings in changed runners; admit_patch_test retains the same five pre-existing warnings as the base.
Shared battery regression on the exact commit in an owned disposable worktree: exit 0, 164.750 s; 752 tests / 13,828 assertions, zero failures/errors/skips. Ledger/wall mutations stayed in that worktree; logs and receipts are retained.

Remaining fence work: investigate the narrow compact-relations time-budget margin without weakening isolation/budgets, then obtain a third complete passing measurement. If code changes, re-establish same-snapshot parity and all three complete timings. The three attempts include a red result, so this is not a passing performance fence. The next entrance remains `flock /home/forge/tmp/suite.lock make test`; no parallelism flag is required.

Evidence: `/var/tmp/forge/gate-lanes/`. Key paths: baseline-*, control-final/{alias,mcp,bb}/, candidate-final/mcp/, tune-{4,8}/, timed-*-measure.json/log, timed-*-landing-gate.edn, parity-timed-*-*.txt, fault-*, battery-regression-*, evidence-summary.json. Failed and cancelled development trials are retained as such. Owned fault clone, battery worktree and task-private temp roots were removed after preserving evidence; the branch worktree is clean. No forbidden service port was used and no push was performed.
