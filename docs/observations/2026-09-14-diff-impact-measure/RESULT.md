# Diff-impact selection vs the full fast lane — executed result

Preregistration: `/home/forge/src/clj-surgeon-records/docs/observations/2026-09-14-diff-impact-measure/preregistration.md`
Subject trunk: `866df39b`. Executed 2026-09-14, 14:28Z–18:37Z, on Anvil. Nothing committed.

## 1. Headline table

Wall is the whole invocation, `date +%s%N` around it, exit captured directly. n=2 per arm, NATIVE first, alternating.
NATIVE = `bb -Xmx1g -Djava.io.tmpdir=$TMPDIR -m clj-surgeon.battery-parallel-runner --suite fast` (the recipe line of the cadence-`:fast` Makefile target `test-fast`), run in the diff's tip worktree.
TOOL = trunk's `test/diff_impact.clj` in `fixed-point` mode over the diff's `base..tip`, run in the diff's tip worktree.

| diff | arm | wall run1 (ms) | wall run2 (ms) | selected | HOLD / nothing-selected | exit r1/r2 | failing namespaces | catch k/k | ratio TOOL/NATIVE | NATIVE spread (noise gate) | TIE? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| **D1** `df0c9e1c^..df0c9e1c` | NATIVE | 30 238 | 30 103 | — (94 ran) | — | 1 / 1 | `clj-surgeon.splice-envelope-test` | — | — | 0.45 % | — |
| | TOOL | 2 611 254 | 2 599 404 | 92 | none — `:selected`, 0 unmatched | 1 / 1 | `splice-envelope-test`, `fast-lane-isolation-test` | **1/1** | **86.35×** | 0.45 % | no |
| **D2** `00566756..8aedb65e` | NATIVE | 32 022 | 27 254 | — (95 ran) | — | 0 / 0 | none | — | — | 16.09 % | — |
| | TOOL | 45 304 | 44 078 | 9 | none — `:selected`, 0 unmatched | 0 / 0 | none | **vacuous** | **1.51×** | 16.09 % | no |
| **D3** `ad05e27c^..ad05e27c` | NATIVE | 31 165 | 30 564 | — (94 ran) | — | 1 / 1 | `clj-surgeon.lane-manifest-test` | — | — | 1.95 % | — |
| | TOOL | 1 984 985 | 1 985 224 | 101 | none — `:selected`, 0 unmatched | 1 / 1 | `lane-manifest-test`, `receipt-artifacts-boundary-test`, `txn-journal-test`, `fast-lane-isolation-test` | **1/1** | **64.32×** | 1.95 % | no |
| **D4** `7bf44cfe^..7bf44cfe` | NATIVE | 29 985 | 28 750 | — (95 ran) | — | 0 / 0 | none | — | — | 4.21 % | — |
| | TOOL | 2 060 091 | 2 064 283 | 93 | none — `:selected`, 0 unmatched | 1 / 1 | `fast-lane-isolation-test` | **vacuous** | **70.22×** | 4.21 % | no |
| **D5** `6e77580f^..6e77580f` | NATIVE | 32 615 | 29 733 | — (95 ran) | — | 0 / 0 | none | — | — | 9.24 % | — |
| | TOOL | 25 748 (retry) | 25 208 | 6 | none — `:selected`, 0 unmatched; **not** `:nothing-selected` | 0 / 0 | none | **vacuous** | **0.82×** | 9.24 % | no |

Median ratio D1–D4 = **67.27×**. No cell is inside its noise gate, so nothing is a TIE.

Refusals (recorded, not hidden):

* **d5 TOOL run 1 refused, exit 90**, `Diff-impact refused: lease or receipt-chain process active` — an external `/var/tmp/forge/jvm-lease*` file existed at 14:31:12Z (the seat's `~/bin/ship`-family tooling writes those). Re-run at 18:36:24Z: 25 748 ms, exit 0. Original row preserved in `ledger-with-refusal.tsv`.
* **Runner parity is impossible, recorded as a refusal for that cell.** The instruction "use the fast-lane invocation for BOTH arms so the only difference is the namespace set" cannot be honoured: `battery-parallel-runner` has no namespace-subset option — `suite-namespaces` reads the lane manifest, and `census-problems`/`suite-receipt-problems` refuse a receipt whose namespaces are not exactly the suite. So the TOOL arm runs `test/diff_impact.clj`'s own executor: one cold `clojure -J-Xmx1g -M:clj-surgeon/test-deps` child **per namespace, serially**, across **all** lanes, while NATIVE runs the 94–95 fast-lane namespaces across 16 parallel lanes. The two arms differ in executor and in lane scope, not only in namespace set.
* **The trunk lane runner cannot be run against a tip tree.** Putting `wt-tool/test` first on the classpath would shadow the tip's own test namespaces — i.e. run trunk's tests, not the diff's. The runner *argv* is trunk's; the runner code and the tests both resolve from the tip snapshot.

### Comparable sub-measurement (derived from the same runs, free)

Because the TOOL arm records each namespace's own wall, the fast-lane-member subset can be priced separately:

| diff | selected | of which fast-lane | TOOL child wall, all lanes (ms) | TOOL child wall, fast-lane members only (ms) | NATIVE fast lane, 94–95 ns, parallel (ms) |
|---|---|---|---|---|---|
| D1 | 92 | 45 | 2 606 962 / 2 594 846 | 224 991 / 224 738 | 30 238 / 30 103 |
| D2 | 9 | 7 | 40 985 / 39 655 | 19 951 / 19 363 | 32 022 / 27 254 |
| D3 | 101 | 51 | 1 980 434 / 1 980 944 | 251 296 / 251 047 | 31 165 / 30 564 |
| D4 | 93 | 46 | 2 055 627 / 2 059 763 | 224 869 / 228 177 | 29 985 / 28 750 |
| D5 | 6 | 5 | 21 498 / 20 876 | 11 468 / 11 021 | 32 615 / 29 733 |

Even restricted to fast-lane members, serial cold-JVM execution of *half* the lane costs 7.4× (D1) what the parallel runner spends on *all* of it. Selection itself is cheap: `list` mode is 4.2–4.4 s, and the fixed-point run's selection + launcher overhead is 4.3 s (D1 2 611 254 − 2 606 962).

### How the arms were built (stated, as required)

Six detached worktrees from `/home/forge/src/clj-surgeon-land`: `wt-tool` @ `866df39b`, `wt-d1`..`wt-d5` at each diff's tip. Every arm ran with cwd = the diff's tip worktree, which is what fixes the analysed tree, the git range, the lane manifest, the runner code and every test child to the tip snapshot. Only two things come from `wt-tool`: the selector **script** (`$wt-tool/test/diff_impact.clj`, passed as an absolute path to `bb`) and the selector **library** (`wt-tool/src` prefixed onto the bb classpath, because `src/clj_surgeon/diff_impact.clj` does not exist at any of the five tips). Envelope parity between trunk's and each tip's `receipt-artifacts/current-envelope` was verified in every worktree before the runs, so the launcher/child environment check could not fire spuriously. The worktrees were left clean (`git status --porcelain` = 0 lines) — `target/` is gitignored, so NATIVE's receipts cannot enter the TOOL arm's `git diff --name-only BASE --` or its file census. `test/diff-impact`'s launcher body (env-override refusal, `flock`, lease/receipt-chain precondition, per-run 0700 `TMPDIR`, `seat-tmp-guard.sh`) is reproduced verbatim in `tool-arm.sh`.

## 2. One line of learning

**Selection reduced the namespace count by 2–15× and lost no catch, but the tool arm was 64–86× slower on four of five diffs, because the win from running fewer namespaces is dwarfed by the loss from running them serially in cold JVMs across all lanes instead of in parallel across one — test selection is a scheduling question, and on a 16-lane box the gate has already spent the parallelism that selection is trying to save.**

## 3. One caveat

The preregistration's arms are internally inconsistent and the bets were written against the other reading: it defines NATIVE as "the full fast lane" (94–95 namespaces, 30 s parallel) but prices D1 as "92 of ~165 selected → 0.6", where ~165 is the whole cross-lane inventory. The TOOL arm selects across **all** lanes (D1: 45 fast, 37 battery, 7 integration, 3 unlaned) while NATIVE runs the fast lane only, so no ratio here is a like-for-like comparison of the same work — it is a comparison of *selected-everywhere, serial, cold* against *fast-lane, parallel, pooled*. Read the ratios as a verdict on the tool's **executor and lane scope**, not on its **selection**; the selection column and the catch column are the parts that are directly comparable, and those are the parts the selector wins.

## 4. Bets scored against the preregistration

| # | bet (as sealed) | observed | verdict |
|---|---|---|---|
| 1 | wall ratio D1 ≈ 0.6 | 86.35× | **BROKE** |
| 2 | wall ratio D2 < 0.2 | 1.51× | **BROKE** |
| 3 | wall ratio D3 < 0.3 | 64.32× | **BROKE** |
| 4 | wall ratio D4 < 0.2 | 70.22× | **BROKE** |
| 5 | wall ratio D5 ≈ 0 (nothing run) | 0.82×; 6 namespaces selected and run, status `:selected`, not `:nothing-selected` and not HOLD | **BROKE** |
| 6 | median ratio D1–D4 < 0.35 | 67.27× | **BROKE** |
| 7 | catch preservation D1 = 1/1 | 1/1 — NATIVE's only red, `clj-surgeon.splice-envelope-test`, was selected (via `source-scan src/clj_surgeon/probe.clj`) and red under TOOL | **HELD** |
| 8 | catch preservation D3 = k/k, k ≥ 1 | 1/1, k = 1 — NATIVE's only fast-lane red, `clj-surgeon.lane-manifest-test`, was selected (via `data-file` and `source-scan`) and red under TOOL. Note: the diff's *intended* red (`txn-journal-test`, `receipt-artifacts-boundary-test`) is battery-lane, invisible to the NATIVE arm, and TOOL found both. | **HELD** |
| 9 | catch preservation D2 vacuous | vacuous (0 NATIVE failures) | **HELD** |
| 10 | catch preservation D4 vacuous | vacuous (0 NATIVE failures) | **HELD** |
| 11 | first-attempt 5/5 TOOL | **4/5** — d5 run 1 refused with exit 90 and a named reason (external lease file); green on retry | **BROKE** |
| 12 | first-attempt 5/5 NATIVE | 5/5 | **HELD** |
| 13 | kill-switch: any NATIVE failure absent from the TOOL selection loses regardless of wall | 0 missed across all five diffs | **HELD — selector not killed** |

Also worth recording against the frozen expectations table, though not a numbered bet: **D5's stated expectation (`:nothing-selected` via a docs allowlist, or HOLD, zero namespaces run) did not hold** — the docs-only diff (`docs/observations/battery-ledger.edn`, `battery-namespace-walls.edn`) selected 6 namespaces through `data-file` edges and ran all six. There is no docs allowlist in the selector on this snapshot; data files are first-class edges.

One TOOL-only failure recurs in every large run and is an artifact of the tool's executor, not of the diff: `clj-surgeon.fast-lane-isolation-test` fails under `fixed-point` on all of D1/D3/D4 while passing under NATIVE, because it asserts that a fast-lane JVM is launched with `-Duser.home` inside its own run root. The parallel runner does that; `test/diff_impact.clj`'s child launcher does not — `user.home is /home/forge`, and the "throwaway" home is found to contain `.m2`, `.gitlibs`, `.ssh`, `.config`. The tree's own isolation ratchet is detecting that the selector's runner is not the gate's fast lane.

## 5. Setup cost

| item | cost |
|---|---|
| `git worktree add --detach` × 6 | 732 ms each measured (~4.4 s total), 145 MB per worktree |
| `clojure -Spath -M:clj-surgeon/test-deps` warm × 6 worktrees | 18 683 ms total |
| 5 discovery `list` passes (pipeline validation, not results) | ~25 s |
| 5 `list`-mode receipt passes (the reported selection receipts) | 22 015 ms total (4 215 / 4 338 / 4 208 / 4 444 / 4 364 ms) |
| envelope-parity check across 5 tips | ~10 s |
| **total setup** | **≈ 80 s of machine time** |

Every arm wall in the tables above is a **warm-classpath** wall: the six worktrees were pre-warmed before any timed cell, so run 1 of each arm does not pay a cold `.cpcache`.

Gating: the Astra builder (pid 2469802) was waited out to exit (`kill -0` loop) before the first timed run; 1-minute load average was read from `/proc/loadavg` before every arm launch and was below 4.0 at every one (observed range 2.31–3.98; each launch's load is in the ledger).

## 6. Raw log paths

Ledger (authoritative, one row per cell): `/var/tmp/forge/measure-fx/diff-impact/ledger.tsv`
Ledger including the original refused row: `/var/tmp/forge/measure-fx/diff-impact/ledger-with-refusal.tsv`
Driver transcript: `/var/tmp/forge/measure-fx/diff-impact/driver.out`
Machine-readable result: `/var/tmp/forge/measure-fx/diff-impact/RESULT.edn`

Scripts actually executed: `native-arm.sh`, `tool-arm.sh`, `driver.sh`, `listmode.sh`, `report.clj` (all in `/var/tmp/forge/measure-fx/diff-impact/`).

Arm logs (`/var/tmp/forge/measure-fx/diff-impact/logs/`):
`d1-native-1.log` `d1-native-2.log` `d1-tool-1.log` `d1-tool-2.log`
`d2-native-1.log` `d2-native-2.log` `d2-tool-1.log` `d2-tool-2.log`
`d3-native-1.log` `d3-native-2.log` `d3-tool-1.log` `d3-tool-2.log`
`d4-native-1.log` `d4-native-2.log` `d4-tool-1.log` `d4-tool-2.log`
`d5-native-1.log` `d5-native-2.log` `d5-tool-1.log` `d5-tool-2.log`
`d1-list.log` … `d5-list.log`, and the discovery passes `disco-d1-list.log` … `disco-d5-list.log`

NATIVE suite receipts (per run): `/var/tmp/forge/measure-fx/diff-impact/wt-<d>/target/gate-parallel/cell-<d>-native-<run>/fast/receipt.edn`
(lane transcripts beside them as `lane-N.out`)

TOOL selection receipts and per-namespace results: `/var/tmp/forge/measure-fx/diff-impact/out/<d>-tool-<run>/`
— `impact-fixed-point.edn` / `.json` (selection inventory), `results-fixed-point.edn` (one observation per namespace: exit, process-exit, counters, wall-ms, log, receipt), and `fixed-point/<namespace>.log` / `.edn` per namespace.
TOOL list-mode receipts: `/var/tmp/forge/measure-fx/diff-impact/out/<d>-list/impact-list.edn` / `.json`.
Discovery selections: `/var/tmp/forge/measure-fx/diff-impact/out/disco-<d>/`.

Worktrees: `/var/tmp/forge/measure-fx/diff-impact/wt-tool` (866df39b), `wt-d1` (df0c9e1c), `wt-d2` (8aedb65e), `wt-d3` (ad05e27c), `wt-d4` (7bf44cfe), `wt-d5` (6e77580f).
