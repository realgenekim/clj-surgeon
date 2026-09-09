# Astra gate lanes, round 3

Completed 2026-09-09T06:58:38.683614+00:00. Branch `astra/gate-lanes`, final SHA
`1c762f73bf49db62a1996b2e2afd8b76badb8f8f`. Local commits `3def037f` and `1c762f73`; both authored and
committed by `forge-anvil <forge-anvil@anvil>` with
`Co-Authored-By: Gene Kim <genek@itrevolution.com>`. No push; no `~/bin` changes.

Sol's verdict was read in full. The repository-side repairs are verified on the
final SHA. Installed ship/land consumption remains the ship builder's work;
this report does not claim a new Sol GO or a live land/push result.

| Finding | Fix | Witness |
|---|---|---|
| GATE-LANES-FENCE-001 | Every coordinator worker command enters `test/gate_slot.py`. An admission flock serializes live MemAvailable sampling, occupancy counting and reservation under `/var/tmp/forge/gate-slots/`. The worker inherits its slot lock across exec for its lifetime. JVM, BB, shell, classpath preparation and prerequisite/after stages use this entrance. Coordinators hold no slot while waiting for workers. | Two simultaneous fixture coordinators each completed 18 mixed JVM/BB/shell jobs: exits 0/0, walls 10.167/9.695 s. Across 493 locked directory samples, aggregate peak 8, derived width 8, zero bound violations, minimum MemAvailable 23,734 MiB. The final real gate pair also remained at aggregate 8. |
| GATE-LANES-FENCE-002 | `landing-gate-prewarm` executes the coordinator manifest minus only `battery-fresh`. `print-gate-stages` reads the same manifest that drives execution and required suites. Successful prewarm publishes `:landing? false :prewarm? true`; full publishes the opposite. | Both final receipts pass exact membership/exit/authority assertions, bind the same source digest and git head/tree, and contain all expected suites. No landing receipt existed in the prewarm checkout. The named prewarm membership test covers alias and audit; missing-alias, missing-audit and extra-stage set probes return `stage-set-mismatch`. |
| Other attack results, item 5 | Removed the stale `min(4, ...)` comment in Makefile and the same stale cap in README. | Final diff and public Make dry resolution are clean. |

Slot eligibility is recomputed on **every acquire attempt**, including retries;
startup executor width is only a packing limit. All locked slot files count,
including holders above a newly reduced width. Only IDs below the live width
are eligible for new acquisition. Slot inodes are retained, never unlinked;
files alone do not denote occupancy. A reduced width waits for holders to drain;
unknown or insufficient memory refuses. Existing workers are not killed merely
because live available memory falls. Five Python boundary tests cover capacity
boundaries, live shrink/growth and high-slot occupancy, inherited locks, nonzero
exit, SIGKILL, failed exec and unknown capacity. Captured live holders include
`java`, `bb` and `make`, rather than only admission helpers.

Other attack results 1–4 required no new repair: TEST-ISO-007's 18,000 ms ceiling
and 60,000 ms union budget remain, receipt source/head/tree binding remains,
the fast-before-integration barrier remains, and serial diagnostics still deny
landing authority. Item 6 is addressed on this side by the consumable prewarm
surface and independent required membership; installed consumer enforcement and
live landing are not claimed.

The exact receipt key path is **`[:stages]`**, a vector whose entries have
**`:target` and `:exit`**. It is flat and includes alias/MCP/BB targets; the
former `runtime-pool` aggregate now lives at `[:pool]`. The MCP target includes
the unchanged shell/oracle sequence, with its actual exit at
`[:pool :shell-checks :exit]`. Per-suite receipts retain each child exit and
namespace facts. Per-run complete receipts are also retained.

Exact ship command values:

```sh
GATES_PREWARM_CMD='make landing-gate-prewarm'
GATES_PHASE_B_CMD='make battery-fresh'
```

The producer receipt is `target/landing-gate-prewarm.edn`. The retained final
prewarm receipt is **`/var/tmp/forge/gate-lanes/r3/prewarm-receipt.edn`**.
Land must compare its `:stages` targets union `{battery-fresh}` with
`make print-gate-stages` on the merged tree, otherwise rerun with
`reason=stage-set-mismatch`. Membership supplements successful stage exits,
prewarm authority flags and the existing tree/toolchain checks.

Exact `make print-gate-stages` stdout, confirmed deterministic:

```text
admit-transaction-recovery-battery
battery-fresh
alias-migration-test
mcp-test
test-bb
repository-hygiene
intent-audit
```

| Final verification, both gates started together | Command wall s | Receipt wall s | Exit |
|---|---:|---:|---:|
| `flock /home/forge/tmp/suite.lock make test` | 221.447 | 216.519 | 0 |
| `make landing-gate-prewarm` in an owned clone of the same SHA | 269.672 | 265.263 | 0 |

Thus **one full gate while a second gate runs beside it took
221.447 seconds**, including entrance and lock queue.
The gate pair produced 2655 samples, peak 8 occupied slots,
live width 8 throughout, minimum MemAvailable 17,299 MiB,
and zero aggregate bound violations. 2,118 samples contain admitted holders
from both checkout roots. These are contention measurements, not speedup claims.

Both gates report identical suite totals:

| Suite | Namespaces | Tests | Assertions | Fail/error/isolation/leak |
|---|---:|---:|---:|---|
| alias | 2 | 164 | 3,495 | 0/0/0/0 |
| MCP | 62 | 898 | 11,197 | 0/0/0/0 |
| BB | 49 | 891 | 7,888 | 0/0/0/0 |

Final focused Clojure verification: **64 tests / 428 assertions, 0 failures,
0 errors**. Python: **5 tests, green**. Formatter completed; lint through
`~/bin/clj-kondo` returned **0 errors / 0 warnings**. The named deftest ledger
was regenerated through its direct entrance (1,644 names); only the three new
witness names were added. Final full gate includes hygiene and intent audit.
`git diff --check` and the final worktree are clean.

A preliminary pair is retained under `initial-pair/`: its full gate completed
successfully in 221.989 s on `3def037f`. Review then found that prewarm children
were defaulting to `gate-parallel` while their parent read `gate-prewarm`;
remaining owned prewarm processes were stopped at 239.019 s. The regression
`prewarm-pool-stores-evidence-under-the-parent-run` failed with a missing child
work directory, then passed after the parent supplied it explicitly. Commit
`1c762f73` fixes that path. The final pair above was repeated to verify the
correction on the final SHA. Consequently there were **two full invocations**,
including this preliminary green run, rather than the requested one; there
were no additional full repetitions. The stopped run's owned temp root was
removed, and its aborted status is preserved rather than counted as green.

Evidence is under `/var/tmp/forge/gate-lanes/r3/`: `fixture-result.json`,
`fixture-samples.json`, `fixture-0.edn`, `fixture-1.edn`, `gate-result.json`,
`gate-samples.json`, `live-inherited-locks.json`, `live-runtime-locks.jsonl`,
`full.log`, `prewarm.log`, `full-receipt.edn`, `prewarm-receipt.edn`,
`receipt-summary.json`, `full-run/`, `prewarm-run/`, focused red/green logs,
`python-focused.log`, `format.log`, `kondo.log` and the membership probe.
The fixture and prewarm checkout were removed after evidence preservation;
the owned nREPL was stopped. Shared slot files remain in place.
