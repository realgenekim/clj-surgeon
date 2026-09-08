# Astra battery floor report

Written 2026-09-08T22:12:23.025614+00:00.

**PASS.** One locked default `make test-battery` finished in **172 s**, versus
Gene's historical **216 s** baseline: **44 s / 20.4% lower**. The coordinator's
measured makespan was **169.508 s**. This is one run against the supplied
historical baseline, not a fresh matched comparison.

- Branch: `astra/namespace-split`, `/home/forge/src/clj-surgeon-split`.
- Final commit: `37406447eee154ac1b8d011e8dffa7d522bae37e`.
- Measured implementation commit: `5cdd5dcc6875790de0332e29b32876ae50aa1d94`.
- Both commits use `forge-anvil <forge-anvil@anvil>` and
  `Co-Authored-By: Gene Kim <genek@itrevolution.com>`.
- First action was `git fetch origin && git merge origin/MCP/main`, which
  fast-forwarded to `0e0307639fc00e3481df9648bd3c25370b3eb535`.
- No push, merge to main, or ship invocation. Ship v3 owns the subsequent review
  and any GO-WITH-FIX closure. No pipeline verdict is claimed here.

## Change and preservation

TEST-ISO-014 registers six stable deftests in `clj-surgeon.reader-eval-fence-test`.
They call the original single-pair body, preserving all six real launches, both
assertions per launch, and cleanup. `preservation.log` proves structural equality
of that body and the launcher helpers against the fetched baseline.

The coverage witness derives pairs by executing the loaded cell bodies with
only their shared launch body replaced by a recorder. It checks the frozen
runtime × build-file set, six cells/six launches, and exactly one launch per
cell. A negative probe moving two launches into one cell while preserving the
pair set fails the last assertion. Shard eligibility is witnessed with no
fixtures or test-ns-hook; all 13 namespace deftests remain independent units.

Actual per-var timing now crosses the lane receipt boundary even when several
vars share a lane. Both packing stages use a namespace-share estimate before a
cell has a measurement; the final packer previously still charged only 1 ms.
Measured walls replace estimates only by execution. The namespace's 1,000,000 ms
exception remains because TEST-ISO-013 judges the SUM of shard walls, not the
parallel makespan.

## Observed battery

Command: `flock /home/forge/tmp/suite.lock env -u BATTERY_LANES -u BATTERY_PREREQS make test-battery`
(the evidence wrapper also timestamps and captures exit status).
Defaults: eight lanes, prerequisites enabled. Start: 2026-09-08T22:07:01Z;
end: 2026-09-08T22:09:55Z; exit **0**.
Temporary storage: `/var/tmp/forge/battery-floor/tmp`; JVMs inherit
`JAVA_TOOL_OPTIONS=-Xmx512m -Djava.io.tmpdir=/var/tmp/forge/battery-floor/tmp`.
The coordinator and lane JVMs also use the bounded Make defaults.

**752 tests / 13,828 assertions; 0 failures, 0 errors,
0 preconditions skipped, 0 preconditions failed, 0 isolation violations across
36 namespaces.** The prior 746 / 13,825 verdict is preserved: the split adds
five test Vars and the coverage witness adds one test / three assertions.
The twelve original matrix assertions still run. The required prerequisite
receipt already existed and was consumed with prerequisites enabled.

All deftests below are in `clj-surgeon.reader-eval-fence-test`:

| Deftest | Measured seconds |
|---|---:|
| `no-real-launcher-evaluates-discovered-bb-bb-edn` | 0.354 |
| `no-real-launcher-evaluates-discovered-bb-deps-edn` | 0.308 |
| `no-real-launcher-evaluates-discovered-bb-project-clj` | 0.298 |
| `no-real-launcher-evaluates-discovered-jvm-bb-edn` | 67.749 |
| `no-real-launcher-evaluates-discovered-jvm-deps-edn` | 66.946 |
| `no-real-launcher-evaluates-discovered-jvm-project-clj` | 68.044 |

The six walls sum to **203.699 s**. The requested expectation of six equal
~35 s cells is **not supported**: JVM launches cost 66.946–68.044 s and Babashka
launches 0.298–0.354 s. Equalizing them would require changing the exact launches
or inventing timings. These measurements come from actual per-var receipts,
and `receipt-check.log` verifies all six occur once and equal the generated
walls file.

The next indivisible floor is
`no-real-launcher-follows-a-build-file-path-out-of-the-tree`, **136.888 s**.
The longest lane was lane 4 at **169.502 s**, carrying alias migration plus the
JVM deps.edn cell and other work. Reader-fence aggregate wall: **486.127 s**;
serial-equivalent sum across the battery: **901.904 s**.

## Verification and evidence

- Fail-first: missing cells and grouped timing produced **3 failures / 0 errors**
  in `red.log`. `unmeasured-red.log` records the 1 ms versus 100 ms cost mismatch.
- Full affected cold run: **83 tests / 851 assertions**, all pass, zero isolation
  violations (`cold.log`).
- Final cold focused run after the last witness/packing changes: **72 tests /
  783 assertions**, all pass (`final-focused.log`, `final-focused.edn`).
- Scheduler Babashka check: **26 tests / 156 assertions**, all pass
  (`scheduler-bb.log`).
- `~/bin/clj-kondo`: **0 errors / 0 warnings** (`kondo.log`).
- Intent audit passes (`intent-audit.edn`); diff whitespace check passes.
- Exactly **one** `make test-battery` invocation: `battery.log`, `battery-exit`,
  `battery-started`, `battery-ended`, and archived `battery-parallel/` receipts.
- Original and generated walls: `before-walls.edn`, `after-walls.edn`.
  The repository walls file was rewritten only by the battery runner.
- `prerequisite-receipt.edn`, `candidate.patch`, `candidate-sha`, and `final-sha`
  retain the measured subject. The final commit adds only generated battery
  evidence and the tech-tree observation; implementation equals the measured
  candidate.

All named evidence files are under `/var/tmp/forge/battery-floor/`.
Worktree is clean. Stopped after the branch commits and this report; no push.
