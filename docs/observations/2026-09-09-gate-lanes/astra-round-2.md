# Astra gate lanes, round two

Written 2026-09-09T05:57:38.153004+00:00. Commit `39a3b6bc2fc66e587c22d53a26b8d84d7e45d46c` on `astra/gate-lanes`, with new commits above
`ebaf7aec0dbdc1b77f36cc70a3db91c71bd5a180`. Author/committer: forge-anvil
<forge-anvil@anvil>; Gene Kim co-author trailer. No push. Controls, injected
faults and the three final timings use this commit; the tracked worktree is clean.
The 90-minute timebox began 04:47:25Z, with an interim due at 05:47:25Z and
deadline 06:17:25Z. The interim was written at 05:47:25.001Z; this final report replaces it.

| Measurement | Whole wall s | Completed zero-exit gates | Landing receipts |
|---|---|---|---|
| Original baseline | 510 (one-second stamps) | 1 | Historical entrance |
| Round 1 | 367.222 median; 368.239 / 366.205 | 2 of 3 attempts | 2 |
| Round 2 | 166.143 median; 166.143 / 165.935 / 167.854 | 3 consecutive final runs | 3 |

The final median is 3.070× baseline/final and 2.210×
round-one/final: 67.4% and 54.8% less wall,
respectively. The original baseline measured audit separately at 0.943 s;
rounds 1 and 2 include it in `make test`. Round 1's retained 179.290 s attempt
exited 2 on the 8,002/8,000 ms budget. Its 367.222 s median uses its two
completed gates. These are one historical baseline and small repetition sets.

Every final command was `flock /home/forge/tmp/suite.lock make test`, with no
width flag. The three invocations ran consecutively, with per-namespace receipt
comparisons between them. All attempts below are retained.

| Attempt | Whole wall s | Exit | Landing receipt |
|---|---|---|---|
| Development trial, before final commit | 173.191 | 0 | true |
| Final 1 | 166.143 | 0 | true |
| Final 2 | 165.935 | 0 | true |
| Final 3 | 167.854 | 0 | true |

| Non-overlapping phase (s) | Final 1 | Final 2 | Final 3 |
|---|---|---|---|
| admit-transaction-recovery-battery | 10.464 | 10.651 | 11.036 |
| battery-fresh | 1.911 | 1.859 | 1.974 |
| JVM classpath preparation | 0.021 | 0.023 | 0.023 |
| Fast JVM wave | 25.903 | 25.078 | 25.749 |
| Shared integration / alias / BB / shell pool | 120.726 | 121.154 | 121.919 |
| repository-hygiene | 1.878 | 1.800 | 1.894 |
| intent-audit | 0.104 | 0.097 | 0.096 |
| Entrance / preparation / remaining overhead | 5.134 | 5.272 | 5.161 |

Runtime spans below overlap. MCP spans its fast and integration children; the shell sequence is nested in the shared pool. These rows must not be summed.

| Suite / scope | Round 1 spans s | Final 1 s | Final 2 s | Final 3 s |
|---|---|---|---|---|
| alias | 78.648 / 79.275 | 85.680 | 85.904 | 85.748 |
| mcp | 73.788 / 73.655 | 85.320 | 84.100 | 85.569 |
| bb | 68.453 / 68.298 | 69.437 | 69.259 | 69.325 |
| MCP shell/oracle sequence | Included in 193.409 / 191.388 s mcp-test target | 120.725 | 121.153 | 121.918 |

The Makefile extracts the existing Python, Prolog and shell checks into
`mcp-test-checks`, used by both the component target and the complete gate.
Their internal sequence is preserved. The existing TEST-ISO-013 coordinator
prepares and folds all runtime suites, using one fixed-size executor per phase.
One job exception drains sibling jobs before refusal. Battery and component
entrances use the same executor; namespace membership still comes from the
manifest, the existing BB vector and the two alias/artifact owners. Recovery
precedes consumers; freshness, hygiene and audit remain serial. Whole namespace
fixtures/hooks are preserved. Gate JVM and BB workers have 512 MiB heap caps; the classpath resolver is
also bounded at 512 MiB.

Width = min(floor(nproc/2), floor((MemAvailableMiB − 2048)/1536)), additionally
capped by namespace count. The existing single-CPU case retains one funded
worker. Unknown memory or less than 3,584 MiB refuses. The fixed min(4, …) is
gone. A shell-check job consumes one slot, so pools never multiply the width.
The reserve remains 2,048 MiB and the charge remains 1,536 MiB per worker.

| Run | nproc | Admission MemAvailable MiB | Derived width | Minimum sampled MiB | Peak active pool jobs | Samples |
|---|---|---|---|---|---|---|
| 1 | 16 | 24042 | 8 | 18707.875 | 8 | 167 |
| 2 | 16 | 24046 | 8 | 18548.070 | 8 | 166 |
| 3 | 16 | 24040 | 8 | 18524.098 | 8 | 168 |

MemAvailable was sampled at a one-second cadence throughout each final command.
The lowest sample was 18524.098 MiB, 16476.098 MiB above the reserve.
Peak count is measured executor occupancy, including the shell job; it is not
the number of JVM threads or descendant subprocesses. No reserve breach was
observed, so this experiment supplies no reason to increase the charge.

The budget overrun is the TEST-ISO-007 namespace isolation oracle, not a
test-body assertion or coordinator deadline. `run-namespace-with-snapshot`
records `System/nanoTime` snapshots around namespace execution and isolation
observation. `ns-isolation/budget-violations` computes
`ms = quot(after.instant-ns − before.instant-ns, 1000000)` and rejects exactly
`(> ms budget)`. The compact-relations namespace previously inherited the
8,000 ms fast default; 8,002 ms therefore refused. The coordinator's separate
child timeout is 30 minutes and was not involved.

The fix is a documented 18,000 ms entry in `namespace-budget-overrides`, under
**TEST-ISO-007**, about twice the earlier measured eight-worker 8,836 ms. Wall
time remains the oracle: per-thread CPU time would omit waits and delegated
work. The named witness `compact-relations-has-a-declared-contention-margin`
accepts 8,002 and 18,000 ms and rejects 18,001 ms with the declared ceiling.
It failed against the original override map. The 60,000 ms fast-lane union
budget is unchanged; no oracle, allowlist or isolation check was deleted.

| Corrected repetition | Width | Compact-relations wall s | Fast pool wall s | Diagnostic command wall s | Exit |
|---|---|---|---|---|---|
| 1 | 8 | 7.325 | 29.919 | 91.249 | 0 |
| 2 | 8 | 8.379 | 27.073 | 88.405 | 0 |
| 3 | 8 | 8.358 | 26.682 | 88.033 | 0 |
| 4 | 8 | 8.869 | 27.034 | 88.398 | 0 |
| 5 | 8 | 8.852 | 27.195 | 88.500 | 0 |

These runs launch eight competing JVM workers. Initial under-load repetitions
recorded one-minute load averages from 10.806 to 21.442 and namespace walls of
7.548 / 7.658 / 7.549 / 7.546 / 7.341 s; all five suite commands exited 1
because the new budget witness had not yet been reflected in the corpus pins.
The pin reader subsequently derived 612 adopted / 1,637 manifest tests, with
25 isolation and 33 coordinator tests. The corrected five repetitions above
all exited 0; their budget code remains unchanged at the final commit. Their diagnostic `clojure -e` wrapper retained a roughly 60 s
agent-pool idle tail after test completion; the normal `-main` entrance calls
`shutdown-agents`, as the complete gate timings show. Both sets of evidence
are retained; namespace walls and complete command walls are distinct columns.

The round-one eight-worker file interference was concrete: other workers'
fast snapshots saw `.hot-transaction-*/.app-nrepl-port`,
`.hot-transaction-*/src/app/render.clj` and `.hot-verify-*.port` created or
deleted by integration tests. A global fast barrier prevents those observations
on a shared checkout. Separate checkout roots remove that observation channel.

| MCP checkout experiment | Pool wall s | Command wall s | Setup s | Exit | Isolation violations |
|---|---|---|---|---|---|
| Separate roots, mixed local order | 63.690 | 518.091 including suite-lock queue | 3.063 | 1 | 6 |
| Separate roots, local fast-before-integration | 61.448 | 62.898 | 3.063 | 0 | 0 |
| Shared root, global barrier | 84.604 | 86.011 | 0 | 0 | 0 |

The first separate-root trial changed local ordering and exposed mutations of
six runtime-config containers in mcp-program-tool-test (program, admit,
inspect, relation-census, tool and runtime). Preserving local order removed
those violations. The ordered separate-root and barrier runs have all-zero
per-namespace counter deltas across 62 namespaces. Eight independent clone
roots plus local ordering cost 65.961 s including setup, 20.049 s less than
the 86.011 s shared-root control. Thus roots are cheaper in this measured MCP
scope. The complete mixed runtime/shell pool on separate roots was not measured;
this commit retains the global barrier and its measured complete gate. All
eight owned checkout roots were removed after retaining the receipts and logs.

| Control suite | Namespaces | Tests | Assertions | Width-one command s | Fail/error/skip | Namespace deltas, final 1/2/3 |
|---|---|---|---|---|---|---|
| alias | 2 | 164 | 3495 | 83.142 | 0/0/0 | 0 / 0 / 0 |
| mcp | 62 | 892 | 11114 | 101.649 | 0/0/0 | 0 / 0 / 0 |
| bb | 49 | 891 | 7888 | 182.777 | 0/0/0 | 0 / 0 / 0 |

All controls use `--debug-serial true`, print SERIAL/NOT-A-GATE, and emit
diagnostic suite receipts with `debug=true`. They issue no landing receipt.
The reused battery comparator compares each namespace's tests, assertions,
failures, errors and skips; all nine final comparisons are zero. All controls
and final gate receipts have the same source digest. The ordinary gate retains
declared/executed Var coverage, child-exit validation, exact namespace sets,
duplicate/missing checks and zero skips. The serial landing-eligibility witness
also remains enforced.

| Injected fault | Exit | Wall s | Landing receipt after run |
|---|---|---|---|
| named-test | 2 | 170.706 | false |
| lost-shard | 1 | 157.464 | false |
| unreadable-census | 2 | 4.985 | false |

The first named-failure attempt on commit 8f2306b2 exited 2 in 169.743 s,
with the intended assertion plus two unrelated children failing to load
clojure.main in a fresh clone. The installed CLI resolves and reads a shared
.cpcache file; the failure was consistent with concurrent cold cache creation.
The coordinator now resolves the test-deps classpath once with bounded
`clojure -Spath` before any JVM worker starts, refuses a nonzero/empty result,
and records preparation wall. BB-only pools skip it. The new command witness
failed before implementation. The controls and all three faults were repeated
on the final commit. The final named-failure clone starts without .cpcache;
its receipts require every namespace, exactly one failure, zero errors and
zero isolation violations. Original controls and faults remain under
pre-classpath-fix/; that group's lost-shard and unreadable-census runs exited
1 and 2 in 160.438 and 4.997 s with no landing artifact.

The named failing test is
`clj-surgeon.battery-parallel-test/gate-width-is-resource-bounded`; its 8-worker
expectation is deliberately changed to −1 in an owned clone. Lost-shard
injection replaces the alias-migration child command with `sh -c "exit 0"`,
so it exits successfully without delivering namespace evidence; the coordinator
names the missing `clj-surgeon.mcp-alias-migration-test`. The census injection
adds an unreadable `test/r2_unreadable_test.clj` declaration and is refused
before workers launch. Each fault begins with a stale landing artifact and
ends with none. The owned fault clone was removed.

| Run | Slowest namespace | Namespace s | Serial authority work s | Namespace + serial lower bound s | Indivisible shell sequence s |
|---|---|---|---|---|---|
| 1 | clj-surgeon.mcp-alias-migration-test | 72.486 | 14.357 | 86.843 | 120.725 |
| 2 | clj-surgeon.mcp-alias-migration-test | 72.396 | 14.407 | 86.803 | 121.153 |
| 3 | clj-surgeon.mcp-alias-migration-test | 73.337 | 15.000 | 88.337 | 121.918 |

The namespace-plus-serial number is an optimistic lower bound excluding
startup, the fast barrier and shell work. With the current plan, the observed
fast-wave duration plus the longer of the mixed runtime jobs and the sequential
shell chain, plus serial authority work and entrance overhead, determines the
complete wall. The shell chain is now longer than any individual namespace. In final run 1,
the optimistic namespace-plus-authority floor is 86.843 s. The current
schedule instead pays its 25.903 s fast wave before the 120.725 s shell
chain, plus 14.357 s of authority work: 160.985 s before entrance/preparation
overhead. That identifies the constraint after overlapping the three suites.

Tree census is 149 discovered / 149 accounted, with exact sets and no
duplicates. This accounting includes the manifest, BB inventory and declared
redirections; it does not claim that `make test` executes every cold battery.
Suite execution censuses are independently exact. The final cold-start witness brings the derived corpus pins to 613 adopted /
1,638 manifest tests (34 coordinator tests). The derived non-MCP intent
count remains 254, including 22 TEST-ISO IDs. Landing receipts bind run ID,
source digest, git head/tree, times, capacity, tree census, phase and suite
receipts, namespace counters/walls and problems. New phase evidence includes
shared-pool peak occupancy and per-child start/completion timestamps. Missing,
unreadable, nonzero, skipped or changed-source evidence prevents publication.

Installed ship, land, suite-run and receipt-chain scripts were not edited.
`bash -n` exits 0 for each, and Make dry resolution exits 0 for `make test`,
the existing prewarm, the prewarm with alias added, and `make test-battery`.
Land has no dry landing mode, so its merge/push path was not invoked. It still
resolves to `~/bin/suite-run make test`; suite-run forwards argv using its
existing three slot locks. These measured full gates explicitly use the
separate suite.lock. Receipt-chain still runs recovery before its locked
`make test-battery` consumer. Ship changed externally since round 1 only in
its worktree-cleanup path; its gate commands are unchanged. Captured SHA256s:

| Caller | SHA256 |
|---|---|
| ship | b5eb34392f2bc9af94a17b8479e7dc2247576d988f0bfac39b0570ced7896635 |
| land | 4a1eae94a7e289b631683d629a411e8f382ed5bbc17b8b556dffa88968635972 |
| suite-run | 3ed03376913478a9e8453cfa9f3c87d983f40bea2149f54bc6f5687d6998f056 |
| receipt-chain | fedf998c2bb635cdd59fbc55b7d0350e2f5f8a01e27dd600776784df93e11e3d |

For Sol: ship's pre-existing prewarm command omits alias-migration-test.
The desired prewarm target list, `make test`'s target coverage minus
battery-fresh, is:
`admit-transaction-recovery-battery alias-migration-test mcp-test test-bb repository-hygiene`.
For phase-A-only metadata, that is the corresponding SHIP_GATES_LANES value.
In the current ship implementation SHIP_GATES_LANES describes the combined
two-phase receipt, so its corrected value is:
`admit-transaction-recovery-battery battery-fresh alias-migration-test mcp-test test-bb repository-hygiene`.
The audit has its own audit-invocation field. SHIP_GATES_LANES is metadata,
not a scheduler: SHIP_GATES_PREWARM_CMD must also add alias-migration-test.
Literal equality with today's `make test` minus battery-fresh additionally
requires the intent audit in prewarm; ship currently runs it after phase B.
No installed caller correction was made in this task.

Final focused isolation run: 84 tests / 584 assertions, zero failures/errors
and zero isolation violations. Formatter completed; lint through ~/bin/clj-kondo
reported zero errors/warnings; git diff --check is clean. A plain focused
invocation had observed another process's temp directory in the shared temp
root; the retained failed log is followed by the successful private-root
runner evidence. Development compilation and census failures are retained.

The shared battery executor regression ran in an owned clone of the final
commit: exit 0, 164.096 s. Its logs, children, ledger and measured
walls were preserved before removing the clone; generated journals did not
change this branch. Counters: 752 tests / 13828
assertions; zero failures/errors/skips.

Evidence is `/var/tmp/forge/gate-lanes/r2/`: timed-*-measure.json,
timed-*-memory.json, timed-*-landing-gate.edn, control/, parity-timed-*-*.txt,
fault-*.json/log, budget-final-8-*/, checkout-*.json/log, barrier-component/,
focused-classpath.log, lint-classpath.log, caller-final-*-dry.log and
battery-regression-*. Original baseline and round-one evidence remain at
`/var/tmp/forge/gate-lanes/`. No forbidden service port was used.
