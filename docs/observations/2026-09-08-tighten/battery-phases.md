# `make test-battery` — critical path, measured before any change

Tree: `origin/MCP/main` @ `23417eb0`, worktree `/var/tmp/forge/battery-fx/wt`.
Box: anvil-server, 16 cores, 30 GB. Run under `flock /home/forge/tmp/suite.lock`.
Serial receipt: `/var/tmp/forge/battery-fx/logs/serial-baseline.log`
(`{:sha "23417eb0…", :started "2026-09-08T16:39:28Z", :wall_s 839, :verdict :pass}`).

## Phases of the target

| phase | what it is | wall |
|---|---|---|
| `make test-battery` total | date stamp to date stamp, incl. ledger append | **839 s** |
| ↳ JVM start + deps resolve + re-exec (TEST-ISO-006) + `require` of 35 ns | before the first `Testing` line | ~21 s (839 − 816.3 − ledger ~1 s) |
| ↳ **the 35 namespaces** (`t/test-ns` between two isolation probes) | the work | **816.3 s (97.3 %)** |
| ↳ isolation fold + tmp-leak sweep + summary | pure folds over the run's own data | < 1 s |
| ↳ `bb battery_ledger.clj append` | one `bb` start, one line appended | ~1 s |

**The isolation checks are not on the critical path.** TEST-ISO-002/003/004/005/007/010
are two `probe` calls around each namespace (directory listings, `/proc/self/fd`,
`/proc/net/tcp`, var-root identities, thread ids) plus a pure fold. They cost
milliseconds; and for the `:battery` lane only TEST-ISO-007 (the time budget) is
*enforced* at all (`ns-isolation/enforced-intents-by-lane`).

## The 35 namespaces, slowest first (measured walls)

| ms | namespace | share |
|---:|---|---:|
| 461 790 | `clj-surgeon.reader-eval-fence-test` | **56.6 %** |
| 68 165 | `clj-surgeon.mcp-alias-migration-test` | 8.4 % |
| 65 483 | `clj-surgeon.mcp-relation-census-launcher-test` | 8.0 % |
| 42 225 | `clj-surgeon.mission-run-test` | 5.2 % |
| 39 755 | `clj-surgeon.mcp-relation-census-test` | 4.9 % |
| 22 573 | `clj-surgeon.mcp-prepared-wire-test` | 2.8 % |
| 22 208 | `clj-surgeon.mission-publication-test` | 2.7 % |
| 19 613 | `clj-surgeon.mission-commit-cli-test` | 2.4 % |
| 18 173 | `clj-surgeon.txn-journal-test` | 2.2 % |
| 14 587 | `clj-surgeon.mcp-process-test` | 1.8 % |
| 8 960 | `clj-surgeon.admit-patch-test` | 1.1 % |
| 6 582 | `clj-surgeon.mission-display-test` | 0.8 % |
| 5 566 | `clj-surgeon.mission-events-test` | 0.7 % |
| 4 032 | `clj-surgeon.core-discovery-test` | 0.5 % |
| 3 286 | `clj-surgeon.mission-typist-executor-test` | 0.4 % |
| 2 419 | `clj-surgeon.receipt-artifacts-boundary-test` | 0.3 % |
| 1 920 | `clj-surgeon.mcp-helper-extraction-test` | 0.2 % |
| 1 255 | `clj-surgeon.require-change-boundary-test` | 0.2 % |
| 1 190 | `clj-surgeon.cell-b-oracle-test` | 0.1 % |
| 912 | `clj-surgeon.mission-git-ledger-test` | |
| 769 | `clj-surgeon.mission-test` | |
| 700 | `clj-surgeon.mcp-cold-verify-test` | |
| 639 | `clj-surgeon.mcp-feature-thread-sed-test` | |
| 541 | `clj-surgeon.mission-git-identity-test` | |
| 527 | `clj-surgeon.mission-fallback-test` | |
| 511 | `clj-surgeon.mission-git-fence-test` | |
| 476 | `clj-surgeon.mission-git-boundary-test` | |
| 359 | `clj-surgeon.mission-git-process-test` | |
| 303 | `clj-surgeon.mission-git-submodule-test` | |
| 161 | `clj-surgeon.repository-hygiene-test` | |
| 135 | `clj-surgeon.mission-provider-fallback-events-test` | |
| 119 | `clj-surgeon.mcp-inspect-cold-job-test` | |
| 113 | `clj-surgeon.mission-typist-executor-admission-test` | |
| 110 | `clj-surgeon.mission-phase-events-test` | |
| 100 | `clj-surgeon.mission-usage-executor-test` | |

## The consequence, stated before the work

**One namespace is 56.6 % of the lane.** A partition of WHOLE namespaces has a
makespan floor equal to its largest unit, so no number of JVMs can take
`make test-battery` below **461.8 s (7.7 min)** while `reader-eval-fence-test`
is one unit. The 5-minute target is therefore not reachable by lane-splitting
alone — it needs that namespace split at the `deftest` level as well.

The remaining 34 namespaces sum to 354.5 s. Spread over the lanes that are not
carrying `reader-eval-fence` shards, they are not the binding constraint at any
width above about 3.

## Verdict semantics that must be preserved

- `Ran 743 tests containing 13756 assertions.` / `0 failures, 0 errors.`
- `1 preconditions skipped.` + `SKIPPED · no battery receipt at
  target/admit-transaction-recovery-battery-receipt.edn · run
  `make admit-transaction-recovery-battery``  ← a NAMED line under an overall PASS
- `0 preconditions failed.`
- `test-isolation: 0 violations across 35 namespace(s)`
- one appended ledger line, exit 0

---

# After: the parallel battery

`make test-battery BATTERY_LANES=N` runs the same 35 namespaces over N JVM lane
children and folds every verdict over the union. `BATTERY_LANES=1` is the serial
shape and is the DEFAULT; `make test-battery-serial` keeps the original recipe
verbatim as the control.

## Run 1 (8 lanes) — the COLD-START schedule

Cold start because the shard-level var costs had never been measured. The
namespace walls were seeded from the serial baseline, so the 34 whole
namespaces packed almost perfectly (four lanes at 34.7 s estimated, within
80 ms of each other), but all seven `reader-eval-fence-test` shards were
charged 1 ms each and LPT correctly packed them into ONE lane. That lane then
carried the whole 461.8 s.

    battery-parallel: 35 namespace(s) in 41 unit(s) over 8 lane(s);
                      floor is clj-surgeon.mcp-alias-migration-test at 68165 ms

| lane | estimated | contents |
|---|---:|---|
| 0 | 34 685 ms | txn-journal, mcp-process, +4 |
| 1 | 34 681 ms | mission-commit-cli, admit-patch, +6, **and all 7 reader-eval-fence shards** |
| 2 | 34 759 ms | mission-publication, mission-display, +6 |
| 3 | 34 721 ms | mcp-prepared-wire, mission-events, +6 |
| 4 | 39 755 ms | mcp-relation-census |
| 5 | 42 225 ms | mission-run |
| 6 | 65 483 ms | mcp-relation-census-launcher |
| 7 | 68 165 ms | mcp-alias-migration |

**The fix this measurement bought:** an unmeasured var was charged 1 ms, which
is a schedule computed from a number nobody measured. It is now charged the
namespace's own wall divided by its shard count, so the shards spread on the
FIRST run and the measured numbers replace the estimate on the second.

## Runs 2–4 (8 lanes) — STEADY STATE, with measured var costs

Once run 1's measurement was in the walls file the seven shards spread one per
lane, and the floor dropped from a 461.8 s namespace to a 206 s **deftest**.

| run | wall | verdict | tests / assertions | failures | skipped | isolation |
|---|---:|---|---|---:|---:|---|
| serial control | **839 s** | pass | 743 / 13 756 | 0 | 1 | 0 across 35 ns |
| run 1 (cold start) | 542 s | **fail** (own bug, see below) | 743 / 13 756 | 0 | 1 | 0 across 35 ns |
| run 2 | **267 s** | pass | 743 / 13 756 | 0 | 1 | 0 across 35 ns |
| run 3 | **217 s** | pass | 743 / 13 756 | 0 | 1 | 0 across 35 ns |
| run 4 | **218 s** | pass | 743 / 13 756 | 0 | 1 | 0 across 35 ns |
| injected | 223 s | **fail** (as designed) | 744 / 13 757 | **1** | 1 | 0 across 35 ns |

**Median of the three clean runs: 218 s (3 min 38 s). Speed-up 3.85x.**
Load during the window ranged 5.5 → 21.3 (the box is shared with ~29 other
sessions; 21.3 was measured at a run START, i.e. ambient, not caused by it).

Run 1 failed on a defect in this change, not in the tree: the lane-integrity
check compared a lane's SELECTORS against the NAMESPACES its child reports, so
a correctly sharded lane was refused as "a lane that silently ran less". Two
more defects were found in the same run (the serialiser dropping `:sharded`,
and home isolation reading a selector's lane). All three are fixed and pinned.

## The measured per-deftest costs of the sharded namespace

| ms | deftest |
|---:|---|
| **205 921** | `no-real-launcher-evaluates-a-build-file-it-discovers` |
| 142 474 | `no-real-launcher-follows-a-build-file-path-out-of-the-tree` |
| 72 222 | `a-non-string-paths-entry-never-reaches-io-file` |
| 71 690 | `the-fence-does-not-refuse-an-ordinary-path-under-a-symlinked-root` |
| 10 037 | `a-deeply-nested-build-file-is-refused-typed-not-overflowed` |
| 2 397 | `no-source-in-this-repository-calls-the-evaluating-reader` |
| 262 | `the-oracle-names-every-evaluator-it-claims-to-fence` |

**The floor is now 206 s in one `deftest`** — a `doseq` over 2 runtimes x 3
build files, six cold child JVM/bb launches. Below ~3.5 min needs that `doseq`
split into separate `deftest`s (or a matrix), which `ns-isolation`'s own
override comment already asks for: *"OWED: split the launcher drives into
matrix cells (the spec's own plan) and retire this line."*

## The dependency DAG, read out of the Makefile

    make test ──> admit-transaction-recovery-battery ──> target/…-receipt.edn
                                                              │ consumed by
                                                              ▼
                                                   clj-surgeon.admit-patch-test
    make test ──> landing-gate ──> battery-fresh · mcp-test · test-bb · repository-hygiene

    make test-battery ──> (NO prerequisite edge)  ← why a direct invocation records
                                                    `1 preconditions skipped`

Every other battery namespace is independent: no ordering, no shared port, no
shared state root (each lane child gets its own `java.io.tmpdir` via
TEST-ISO-006). `serial-groups` is therefore EMPTY, audited, with the mechanism
shipped and witnessed so the next prerequisite is declared rather than
discovered.
