# TEST-ISO-013 verdict parity: the 1-lane control against the 8-lane run

**Tip `bb333d33`, anvil-server, both runs under `flock /home/forge/tmp/suite.lock`.**

The one isolation risk named at the end of the first report was that per-namespace
assertion counts had never been compared against a serial control -- only the
union totals had. This closes it.

| | control | parallel |
|---|---|---|
| invocation | `make test-battery BATTERY_LANES=1` | `make test-battery BATTERY_LANES=8` |
| wall | 818 s | 218 s |
| receipts | `target/battery-parallel/lane-0.edn` | `lane-0.edn` … `lane-7.edn` |
| log | `/var/tmp/forge/battery-fx/logs/parallel-serial-control.log` | `…/parallel-r3.log` (second run in file) |

Compared per namespace: `:test`, assertions (`:pass + :fail + :error`, the same
arithmetic the summary line prints), `:fail`, `:error`, and
`:precondition-skipped`. The comparator reads the lane children's OWN EDN
receipts, not the rendered summary, so it cannot be fooled by a summary that
renders correctly over wrong data.

## The diff table -- all zero

| namespace | control t/a | parallel t/a | diff |
|---|---:|---:|---:|
| `admit-patch-test` | 168 / 4325 | 168 / 4325 | 0 |
| `cell-b-oracle-test` | 2 / 8 | 2 / 8 | 0 |
| `core-discovery-test` | 7 / 35 | 7 / 35 | 0 |
| `mcp-alias-migration-test` | 143 / 3331 | 143 / 3331 | 0 |
| `mcp-cold-verify-test` | 7 / 50 | 7 / 50 | 0 |
| `mcp-feature-thread-sed-test` | 1 / 1 | 1 / 1 | 0 |
| `mcp-helper-extraction-test` | 51 / 1191 | 51 / 1191 | 0 |
| `mcp-inspect-cold-job-test` | 1 / 7 | 1 / 7 | 0 |
| `mcp-prepared-wire-test` | 5 / 31 | 5 / 31 | 0 |
| `mcp-process-test` | 19 / 71 | 19 / 71 | 0 |
| `mcp-relation-census-launcher-test` | 5 / 186 | 5 / 186 | 0 |
| `mcp-relation-census-test` | 82 / 2533 | 82 / 2533 | 0 |
| `mission-commit-cli-test` | 4 / 44 | 4 / 44 | 0 |
| `mission-display-test` | 14 / 109 | 14 / 109 | 0 |
| `mission-events-test` | 8 / 63 | 8 / 63 | 0 |
| `mission-fallback-test` | 8 / 85 | 8 / 85 | 0 |
| `mission-git-boundary-test` | 4 / 19 | 4 / 19 | 0 |
| `mission-git-fence-test` | 5 / 34 | 5 / 34 | 0 |
| `mission-git-identity-test` | 3 / 5 | 3 / 5 | 0 |
| `mission-git-ledger-test` | 3 / 27 | 3 / 27 | 0 |
| `mission-git-process-test` | 2 / 8 | 2 / 8 | 0 |
| `mission-git-submodule-test` | 2 / 15 | 2 / 15 | 0 |
| `mission-phase-events-test` | 7 / 31 | 7 / 31 | 0 |
| `mission-provider-fallback-events-test` | 8 / 67 | 8 / 67 | 0 |
| `mission-publication-test` | 7 / 87 | 7 / 87 | 0 |
| `mission-run-test` | 11 / 79 | 11 / 79 | 0 |
| `mission-test` | 27 / 329 | 27 / 329 | 0 |
| `mission-typist-executor-admission-test` | 2 / 9 | 2 / 9 | 0 |
| `mission-typist-executor-test` | 11 / 73 | 11 / 73 | 0 |
| `mission-usage-executor-test` | 2 / 17 | 2 / 17 | 0 |
| **`reader-eval-fence-test`** (the sharded one) | **7 / 74** | **7 / 74** | **0** |
| `receipt-artifacts-boundary-test` | 21 / 164 | 21 / 164 | 0 |
| `repository-hygiene-test` | 4 / 14 | 4 / 14 | 0 |
| `require-change-boundary-test` | 12 / 89 | 12 / 89 | 0 |
| `txn-journal-test` | 80 / 545 | 80 / 545 | 0 |
| **TOTAL** | **743 / 13 756** | **743 / 13 756** | **0** |

35 namespaces both sides. `fail/error/skipped`: 0 / 0 / 1 both sides.
**VERDICT: identical per namespace.**

### The comparator was proved able to FAIL first

A comparison tool that can only say "identical" is worthless, so both refusal
branches were driven before the real comparison was trusted:

* one namespace's counters shrunk by one test / nine assertions ->
  `** t-1 a-9 **`, `TOTAL ** MISMATCH **`, exit 1;
* one namespace's run deleted outright -> `** NAMESPACE MISSING **`, exit 1.

## The two receipt modes, so a ledger reader can tell them apart

`BATTERY_PREREQS=1` **changes what is asserted** and therefore changes the
counts above. `admit-patch-test` reads
`target/admit-transaction-recovery-battery-receipt.edn`; with no receipt on
disk it records a counted, named skip and asserts **4 141**; with the receipt
present it asserts **4 143**.

| mode | ledger line | measured totals |
|---|---|---|
| `BATTERY_PREREQS=0` | `:skipped 1` | 743 tests / 13 756 assertions (35 ns, pre-merge tree) |
| `BATTERY_PREREQS=1` (now the default) | `:skipped 0`, RED if not | 746 tests / 13 825 assertions (36 ns, merged tree) |

**CORRECTION, measured 2026-09-08 on the fence run.** An earlier draft of this
table predicted 13 758 for `PREREQS=1`, reading `admit-patch-test`'s own note
that it asserts "4 141 with no battery receipt and 4 143 with one". That note is
STALE relative to this tree: `admit-patch-test` measures **168 tests / 4 325
assertions in BOTH modes** -- identical in the `PREREQS=0` parity receipt and in
the `PREREQS=1` fence receipt. Satisfying the precondition moves `:skipped`
from 1 to 0 and moves NOTHING ELSE.

The whole 746 / 13 825 - 743 / 13 756 difference is the MERGE, not the mode:
trunk's Batch 3 added one battery namespace, `split-proof-gate-boundary-test`,
measured here at 3 tests / 69 assertions. 743 + 3 = 746; 13 756 + 69 = 13 825;
35 + 1 = 36 namespaces.

So `:skipped` is the ONLY receipt field that distinguishes the two modes on this
tree -- which is a cleaner contract than the one this doc first claimed, and the
reason to keep the field. A `:skipped 1` line under `BATTERY_PREREQS=1` is RED
by construction.

## What this control does and does NOT close

**Closes:** lane splitting does not change any namespace's test count,
assertion count, failures, errors, or skips -- including the deftest-sharded
namespace, which reports 7 / 74 on both sides.

**Does not close, stated exactly:**

1. **The control is the 1-LANE COORDINATOR PATH, not `make test-battery-serial`.**
   Two differences remain unmeasured per namespace: (a) `BATTERY_LANES=1` runs
   its units in LPT cost order, not manifest order, so `BATTERY_LANES=1` is the
   serial SHAPE but not the serial ORDER; (b) both sides drive the sharded
   namespace through `test-vars`, so `test-vars`-vs-`test-ns` parity for that
   one namespace is proved only at the UNION level (743 / 13 756, matching the
   839 s `test-battery-serial` baseline receipt). Closing both costs one
   `clojure -M:clj-surgeon/test-deps -m clj-surgeon.mcp-test-runner --emit-edn X battery`
   run, about 14 minutes.
2. **Within-namespace order is not preserved for the sharded namespace** --
   `test-ns` runs `(vals (ns-interns ns))`, hash order, so it was never defined.
   TEST-ISO-008 (order independence) remains a filed gap.

## DEFECT FOUND BY THIS RUN, not fixed here

**A `BATTERY_LANES=1` run POISONS the measured per-deftest costs for the next
wide run -- and 1 is the shipped default.**

With one lane every shard of a namespace lands in that lane, the child merges
them into a single `test-vars` call, and `write-walls!` can only attribute the
namespace's wall EVENLY across its vars. Measured, before and after this control
run:

| deftest | measured (run 2, 8 lanes) | after one 1-lane run |
|---|---:|---:|
| `no-real-launcher-evaluates-a-build-file-it-discovers` | **204 052 ms** | 66 362 ms |
| `no-real-launcher-follows-a-build-file-path-out-of-the-tree` | 138 425 ms | 66 362 ms |
| `a-non-string-paths-entry-never-reaches-io-file` | 67 974 ms | 66 362 ms |
| `the-fence-does-not-refuse-an-ordinary-path-under-a-symlinked-root` | 67 974 ms | 66 362 ms |
| `a-deeply-nested-build-file-is-refused-typed-not-overflowed` | 5 055 ms | 66 362 ms |
| `no-source-in-this-repository-calls-the-evaluating-reader` | 1 147 ms | 66 362 ms |
| `the-oracle-names-every-evaluator-it-claims-to-fence` | 1 147 ms | 66 362 ms |

The real distribution spans 204 s to 1.1 s; the flattened one claims they are
all equal. It is a SCHEDULE-QUALITY defect, never a correctness one -- every
namespace still runs exactly once and every verdict is unchanged -- but it
means a routine default-mode run silently undoes the measurement that makes the
wide run fast, and the next wide run pays it back in makespan.

The walls file in this commit is RESTORED to run 2's measured values; the
control run's flattened rewrite was discarded.

**The fix, named but deliberately not applied here** (this commit is the
comparison only): record a var's wall ONLY when the shard isolated it --
`(= 1 (count (:vars r)))` -- and otherwise leave the existing entry untouched,
so a coarse observation can never overwrite a fine one.
