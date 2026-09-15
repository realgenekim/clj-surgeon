# Repaired replay — diff-impact selection through the gate's own executors vs the full fast lane

Preregistration (with the 2026-09-15T05:10:19Z Addendum): `/home/forge/src/clj-surgeon-records/docs/observations/2026-09-14-diff-impact-measure/preregistration.md`
Subject: trunk `5cc35402` (`origin/MCP/main`), which contains `cf237f96` as an ancestor; the landing was observed at **2026-09-15T09:32:04Z** after a 5-hour wait (cap extended from 3 h by the coordinator at 08:10Z; the branch went through a third review round after a gate-envelope refusal).
Executed 2026-09-15, 09:33:22Z–12:10:56Z, on Anvil (16 CPUs). **Nothing committed.**

## 1. Headline table

Wall is the whole invocation, `date +%s%N` immediately around it, exit captured directly from `$?`. n=2 per arm per diff, NATIVE first then alternating (native → tool-fast → tool-all, twice, per diff; diff order d5, d2, d1, d3, d4).

* **NATIVE** (unchanged): `bb -Xmx1g -Djava.io.tmpdir=$TMPDIR -m clj-surgeon.battery-parallel-runner --suite fast` — the tip's OWN runner, full fast lane, in the diff's tip worktree.
* **TOOL-FAST**: trunk's landed `test/diff_impact.clj BASE OUT fixed-point fast` — selection, then selected ∩ fast-lane members through the gate's fast runner (`--selected`, partial receipt).
* **TOOL-ALL**: the same with scope `all` — the selected set across every lane (fast / battery / mcp / dedicated), reported separately and never blended.

| diff | arm | wall r1 (ms) | wall r2 (ms) | mean (ms) | selection wall (ms) | execution wall (ms) | selected fast ∩ / all | HOLD / nothing-selected | exit r1/r2 | failing namespaces | catch k/k | ratio TOOL-FAST/NATIVE | NATIVE spread (noise gate) | verdict |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| **D1** `df0c9e1c^..df0c9e1c` | NATIVE | 29 381 | 30 684 | 30 033 | — | — | — (94 fast ran) | — | 1 / 1 | `splice-envelope-test` | — | — | **4.34 %** | — |
| | TOOL-FAST | 33 300 | 33 070 | 33 185 | 4 083 / 4 206 | 29 112 / 28 749 | **45 / 92** | none — `:selected`, 0 unmatched | 1 / 1 | `splice-envelope-test` | **1/1** | **1.105** | 4.34 % | **TOOL-FAST SLOWER** |
| | TOOL-ALL | 1 330 440 | 1 330 704 | 1 330 572 | 4 460 / 4 206 | 1 325 881 / 1 326 389 | — / **92** | none — `:selected`, 0 unmatched | 1 / 1 | `splice-envelope-test` (+ `txn-journal-test`, run 1 only) | — | 44.3× | — | — |
| **D2** `00566756..8aedb65e` | NATIVE | 30 192 | 27 881 | 29 037 | — | — | — (95 fast ran) | — | 0 / 0 | none | — | — | **7.96 %** | — |
| | TOOL-FAST | 12 740 | 12 722 | 12 731 | 4 311 / 4 361 | 8 321 / 8 271 | **7 / 9** | none — `:selected`, 0 unmatched | 0 / 0 | none | **vacuous (0/0)** | **0.438** | 7.96 % | **TOOL-FAST FASTER** |
| | TOOL-ALL | 38 159 | 26 413 | 32 286 | 4 160 / 4 126 | 33 900 / 22 172 | — / **9** | none — `:selected`, 0 unmatched | 0 / 0 | none | — | 1.1× | — | — |
| **D3** `ad05e27c^..ad05e27c` | NATIVE | 29 972 | 29 418 | 29 695 | — | — | — (94 fast ran) | — | 1 / 1 | `lane-manifest-test` | — | — | **1.87 %** | — |
| | TOOL-FAST | 34 129 | 34 139 | 34 134 | 4 235 / 4 525 | 29 796 / 29 516 | **51 / 101** | none — `:selected`, 0 unmatched | 1 / 1 | `lane-manifest-test` | **1/1** | **1.149** | 1.87 % | **TOOL-FAST SLOWER** |
| | TOOL-ALL | 785 459 | 771 983 | 778 721 | 4 194 / 4 044 | 781 163 / 767 832 | — / **101** | none — `:selected`, 0 unmatched | 1 / 1 | `lane-manifest-test`, `receipt-artifacts-boundary-test`, `txn-journal-test` | — | 26.2× | — | — |
| **D4** `7bf44cfe^..7bf44cfe` | NATIVE | 30 695 | 27 240 | 28 967 | — | — | — (95 fast ran) | — | 0 / 0 | none | — | — | **11.93 %** | — |
| | TOOL-FAST | 31 420 | 31 511 | 31 465 | 4 315 / 4 207 | 26 995 / 27 192 | **46 / 93** | none — `:selected`, 0 unmatched | 0 / 0 | none | **vacuous (0/0)** | **1.086** | 11.93 % | **TIE** (inside the gate) |
| | TOOL-ALL | 803 501 | 789 172 | 796 337 | 4 334 / 4 113 | 799 073 / 784 944 | — / **93** | none — `:selected`, 0 unmatched | 1 / 1 | none (exit 1 from a `:partial-child-census-mismatch` battery problem, not a test) | — | 27.5× | — | — |
| **D5** `6e77580f^..6e77580f` | NATIVE | 26 745 | 27 841 | 27 293 | — | — | — (95 fast ran) | — | 0 / 0 | none | — | — | **4.02 %** | — |
| | TOOL-FAST | 6 994 | 7 105 | 7 050 | 4 276 / 4 379 | 2 639 / 2 651 | **5 / 6** | none — `:selected`, 0 unmatched; **not** `:nothing-selected`, **not** HOLD | 0 / 0 | none | **vacuous (0/0)** | **0.258** | 4.02 % | **TOOL-FAST FASTER** |
| | TOOL-ALL | 18 691 | 18 493 | 18 592 | 4 383 / 4 186 | 14 228 / 14 223 | — / **6** | none — `:selected`, 0 unmatched | 0 / 0 | none | — | 0.7× | — | — |

**Median TOOL-FAST/NATIVE: D1–D4 = 1.096; D1/D3/D4 = 1.105.** No HOLD and no `:nothing-selected` fired on any diff: all five returned `:selected` with **0 unmatched files**. First attempt: **5/5 NATIVE, 5/5 TOOL-FAST, 5/5 TOOL-ALL** — no refusal, no missing receipt, no retry anywhere in the 30 timed cells (contrast 2026-09-14, where a stray lease file refused d5 with exit 90).

### Gate worker count (required, both arms)

`capacity` is identical on both arms: **`{:cpus 16, :memory-mib 24709, :lanes 8, :admission :abstract-socket, :heap-mib 512, :reserve-mib 2048, :lane-charge-mib 1536, :floor-mib 3584}`** — the gate admits **8 workers** on this 16-CPU host, confirming Astra's note. Observed per receipt:

| arm | suite | `:lane-count` | `:process-count` |
|---|---|---|---|
| NATIVE (94–95 fast ns) | fast | 8 | 15–16 |
| TOOL-FAST D1/D3/D4 (45–51 ns) | fast | 8 | 13–16 |
| TOOL-FAST D2 (7 ns) / D5 (5 ns) | fast | 7 / 5 | 7 / 5 |
| TOOL-ALL | battery | 8 | 8 |
| TOOL-ALL | mcp | 7 | 7 |
| TOOL-ALL | **dedicated** | **1** | **1** (serial by contract) |

`:lane-count` is the admitted width (8, or fewer when the subset is smaller); `:process-count` exceeds it on the fast suite because that suite runs a `:jvm` wave and a `:bb` wave through the same 8-wide pool. Yesterday's "16 parallel lanes" was the partition count, not the admitted worker count.

### Catch preservation

| diff | NATIVE fast-lane failures | TOOL-FAST failures | k/k | TOOL-ALL's extra (battery-lane) catches, listed separately |
|---|---|---|---|---|
| D1 | `clj-surgeon.splice-envelope-test` | `clj-surgeon.splice-envelope-test` | **1/1** | `clj-surgeon.txn-journal-test` — **run 1 only; run 2's battery receipt reported no failing namespace** (flaky, recorded as observed) |
| D2 | none | none | **vacuous (0/0)** | none |
| D3 | `clj-surgeon.lane-manifest-test` | `clj-surgeon.lane-manifest-test` | **1/1** | `clj-surgeon.receipt-artifacts-boundary-test`, `clj-surgeon.txn-journal-test` — the diff's *intended* red, invisible to a fast-only lane, caught in both runs |
| D4 | none | none | **vacuous (0/0)** | none |
| D5 | none | none | **vacuous (0/0)** | none |

**Zero NATIVE failures were missed by the selection on any diff.** The kill-switch class did not fire.

### Why TOOL-FAST cannot win on a broad diff — the makespan floor, measured

| diff | NATIVE fast members / makespan | TOOL-FAST fast members / makespan | floor unit NATIVE | floor unit TOOL-FAST |
|---|---|---|---|---|
| D1 | 94 / 28 991 ms | 45 / 28 377 ms | `worktree-lifecycle-test` @ 300 000 ms est. | `workspace-onboarding-test` @ 300 000 ms est. |
| D2 | 95 / 29 769 ms | 7 / 7 531 ms | `worktree-lifecycle-test` | `lane-manifest-test` @ 2 098 ms |
| D3 | 94 / 29 552 ms | 51 / 29 076 ms | `worktree-lifecycle-test` | `workspace-onboarding-test` @ 300 000 ms est. |
| D4 | 95 / 30 285 ms | 46 / 26 233 ms | `worktree-lifecycle-test` | `mcp-compact-relations-test` @ 7 769 ms |
| D5 | 95 / 26 447 ms | 5 / 2 031 ms | `mcp-compact-relations-test` @ 8 010 ms | `lane-manifest-test` @ 2 413 ms |

Cutting 94 namespaces to 45 removed **614 ms** of makespan. Cutting 95 to 7 removed **22 238 ms**.

### Supplementary control — identical execution machinery, full fast lane (n=1 per diff)

Astra required "identical execution machinery." NATIVE runs the *tip's* runner; TOOL runs *trunk's*. To price that difference, trunk's selection-aware runner was also run at full fast scope (no `--selected`) on each tip:

| diff | NATIVE (tip runner) mean | PARITY (trunk runner, full fast lane) | delta |
|---|---|---|---|
| D1 | 30 033 ms | 30 014 ms (exit 1, `splice-envelope-test`) | −0.1 % |
| D2 | 29 037 ms | 27 541 ms (exit 0) | −5.2 % |
| D3 | 29 695 ms | 30 083 ms (exit 1, `lane-manifest-test`) | +1.3 % |
| D4 | 28 967 ms | 28 052 ms (exit 0) | −3.2 % |
| D5 | 27 293 ms | 27 933 ms (exit 0) | +2.3 % |

Every delta is inside that diff's NATIVE noise gate. The executor is genuinely the same machinery in both arms; the only difference the headline ratios measure is **the namespace set**. That is the repair the Addendum asked for, and it holds.

### TOOL-ALL cost breakdown (operational result, never blended into the speed claim)

| diff | fast | mcp | battery | dedicated | total execution |
|---|---|---|---|---|---|
| D1 | 27 330 ms | 57 437 ms | **709 297 ms** | **517 293 ms** (3 ns, 1 lane, serial) | 1 325 881 ms |
| D3 | 30 392 ms | 57 515 ms | 169 131 ms | **509 942 ms** | 781 163 ms |
| D4 | 27 576 ms | 57 125 ms | 187 344 ms | **512 374 ms** | 799 073 ms |
| D2 | 7 022 ms | — | 13 271 ms | — | 33 900 ms |
| D5 | 2 018 ms | — | 11 303 ms | — | 14 228 ms |

On the three broad diffs the **dedicated** suite alone — three namespaces, one lane, serial by contract — costs ~510 s, about 17× the entire NATIVE fast lane. The battery lane is the second cost; D1's battery was 4× D3/D4's on the same 37 selected namespaces (709 s vs 169/187 s), an unexplained per-tip difference recorded as observed.

Every TOOL-ALL battery receipt on D1/D3/D4 carried a `{:kind :partial-child-census-mismatch}` problem, which is what turned D4's TOOL-ALL exit to 1 with **no failing test**. That is an executor-level finding in the landed code, not a property of the diffs, and it is reported here rather than smoothed over.

### How the arms were built, and how classpath shadowing was avoided

Six detached worktrees under `/var/tmp/forge/measure-fx/diff-impact-replay/`: `wt-tool` @ `5cc35402` (trunk, created **after** the landing was confirmed), `wt-d1`…`wt-d5` at each diff's tip. **Every arm runs with cwd = the diff's tip worktree**, which is what pins the analysed tree, the git range, the lane manifest, `ns-isolation`, `gate-memory`, `test/run_all.clj`, `test/gate_slot.py`, and — decisively — every spawned lane child (`run-lane!` passes `:dir` = `checkout-root` = `user.dir`, and the child argv is `clojure -M:clj-surgeon/test-deps …` / `bb test/run_all.clj …`, resolved from the tip's own `deps.edn`/`bb.edn`). **No test namespace is ever loaded from trunk.**

Only the *parent* (selector + lane coordinator) process borrows from trunk, through two additions to the bb classpath, in this order: `$wt-tool/src : $SHADOW : src : test : libs/clj-splice/src`.

1. `$wt-tool/src` — because `src/clj_surgeon/diff_impact.clj` does not exist at any of the five tips (and `probe_state.clj` lacks `state-root`/`admit-state-root!` at `ad05e27c`). This shadows trunk's `src` for the parent only.
2. `$SHADOW` — a directory holding **exactly three files**: trunk's `clj_surgeon/battery_parallel_runner.clj` (the only genuine shadow: the tips' runner has no `selection-classification`/`project-selection`/`run-suite! --selected`), plus `clj_surgeon/gate_obligations.clj` and `clj_surgeon/toolchain_identity.clj`, which the landed runner requires and which **do not exist at any tip** (verified: `git ls-tree` returns 0 matches at all five), so those two shadow nothing.

`test/` is **not** shadowed — putting `$wt-tool/test` on the path would have run trunk's tests instead of the diff's, which is the trap named in the 2026-09-14 report. The tip's `lane_manifest.clj` therefore decides fast-lane membership for both arms, and every `lm/…`, `iso/…`, `mem/…` symbol the landed runner uses was verified present at all five tips before any run. All five tip worktrees were `git status --porcelain` clean before and their `git diff --name-only BASE --` reproduced the five frozen file sets exactly.

### Gating and setup

Before **every** one of the 35 timed launches: `/home/forge/bin/ship --builders` had to report `SHIP-BUILDERS none` **and** `/proc/loadavg` 1-minute had to be < 4.0, waiting in 30 s steps otherwise. Every launch's load is in `ledger.tsv` column 5; the observed range across all 35 launches was **2.70 – 3.95**. The landing wait itself ran a 60 s poll from 06:07:53Z (`git fetch` + `merge-base --is-ancestor`) and logged every tick.

Setup (untimed, before any cell): 5 tip worktrees `git worktree add --detach` at 744–791 ms each; `clojure -Spath -M:clj-surgeon/test-deps` warm across the 5 tips = 6 993 ms total, plus `wt-tool`; three pipeline-validation passes on d5 (fast, all, parity) against the branch commit `cf237f96` and one re-validation (`disco2-d5-fast`, 7 149 ms) against the landed tip `5cc35402` after the landed `test/diff_impact.clj` turned out to differ from `cf237f96`'s (new `fast-scope-empty` refusal and `selection-classification`). Every wall in the headline table is a warm-classpath wall. Total setup ≈ 110 s of machine time.

## 2. One line of learning

**With the executor handicap removed the selector is exactly as fast as the set it selects is small: it wins 2.3×–3.9× when selection drops the lane below its slowest unit (D2 7 of 95, D5 5 of 95) and wins nothing at all when it does not (D1 45 of 94 and D3 51 of 94 both still contain a ~28 s tail, so halving the namespace count bought 614 ms and the 4.3 s selection pass more than spent it) — a parallel lane's wall is its makespan floor, not its namespace count, so test selection pays only where it changes which namespace is the floor.**

## 3. One caveat

The catch column is carried entirely by two single-namespace reds on a 16-CPU box in one morning, and one of the three TOOL-ALL extra catches (`txn-journal-test` on D1) appeared in run 1 and vanished in run 2 — so "zero missed catches, 2/2 preserved" is evidence about these five frozen historical diffs and this snapshot's flakiness, not a soundness property; prospective transfer still needs the sealed encounter accepted by an independent owner, exactly as the preregistration says.

## 4. Bets from the Addendum, scored

Scoring rule, stated before the verdicts: a bet given as a **range** holds if the observed value is inside the range; a bet given as a **point** holds if the observed value is within **±25 %** relative (the low-side tolerance implied by Fable's own `0.8 (0.6–1.1)` range). Ratios are mean-of-2 TOOL-FAST over mean-of-2 NATIVE.

### Fable

| # | bet (as sealed) | observed | verdict |
|---|---|---|---|
| F1 | D1/D3/D4 median **0.8** (plausible 0.6–1.1) | **1.105** (D1 1.105, D3 1.149, D4 1.086) | **BROKE** (just above the 1.1 ceiling) |
| F2 | D2 **0.45** | **0.438** (−2.7 %) | **HELD** |
| F3 | D5 **0.35** | **0.258** (−26.3 %) | **BROKE** — marginally, and in the *favourable* direction: the win was bigger than predicted |
| F4 | catch preservation D1 **1/1** | 1/1 — `splice-envelope-test` selected via `source-scan src/clj_surgeon/probe.clj` and red under TOOL-FAST | **HELD** |
| F5 | catch preservation D3 **k/k**, k ≥ 1 | 1/1, k = 1 — `lane-manifest-test` selected via `data-file docs/intent/lock-break-interleaving/lock-break-interleaving-specs.md` and `source-scan src/clj_surgeon/receipt_artifacts.clj`, and red under TOOL-FAST | **HELD** |
| F6 | first-attempt **5/5 both arms** | 5/5 NATIVE, 5/5 TOOL-FAST (also 5/5 TOOL-ALL); no refusal, no HOLD, no retry in 30 timed cells | **HELD** |
| F7 | TOOL-ALL **slower** than NATIVE on D1/D3/D4 (extra catches are its value, not wall) | 44.3× / 26.2× / 27.5× slower; extra catches on D3 (2) and D1 (1, flaky) | **HELD** |

Fable: **5 HELD, 2 BROKE.**

### Astra

| # | bet (as sealed) | observed | verdict |
|---|---|---|---|
| A1 | D1/D3/D4 median **0.9** (plausible 0.7–1.2) | **1.105** | **HELD** |
| A2 | D2 **0.5** (plausible 0.35–0.75) | **0.438** | **HELD** |
| A3 | D5 **0.4** (plausible 0.3–0.65) | **0.258** | **BROKE** (below the 0.3 floor) |
| A4 | selection alone costs **~4.3 s** — a **0.14 floor** against a 30 s lane | selection wall 4 044–4 525 ms, mean **4 257 ms**; floor = 4 257 / 29 005 = **0.147** | **HELD** |
| A5 | D5: **six namespaces were selected, not zero** | 6 selected across all lanes (5 of them fast-lane); status `:selected`, not `:nothing-selected`, no docs allowlist | **HELD** |
| A6 | "namespace counts cannot predict the longest parallel lane" | D4 selected 46 of 95 (48 %) and cost 1.086× NATIVE; D1 45 of 94 and cost 1.105× | **HELD** |
| A7 | would **not** bet on an across-all-lanes win against fast-only native | TOOL-ALL lost 26×–44× on D1/D3/D4 | **HELD** (declined bet, vindicated) |

Astra: **6 HELD, 1 BROKE.** Astra's ranges were better calibrated than Fable's on every diff.

## 5. The decision rule, applied

> "Decision rule (Astra's, accepted): absent a TOOL-FAST win beyond the noise gate with preserved catches, native routing stays and the next transfer candidate is chosen; no further apparatus on the selector."

Applied to the frozen five:

* **Wins beyond the noise gate: 2 of 5** — D2 (0.438 vs a 7.96 % gate) and D5 (0.258 vs a 4.02 % gate). Both are diffs on which NATIVE was green, so both carry **vacuous** catch preservation.
* **Losses beyond the noise gate: 2 of 5** — D1 (1.105 vs 4.34 %) and D3 (1.149 vs 1.87 %). **These are precisely the two diffs that carry a real catch.** Both catches were preserved (1/1 each), and both cost more wall than running everything.
* **TIE: 1 of 5** — D4 (1.086 inside an 11.93 % gate).
* Median across D1–D4 = **1.096**; across D1/D3/D4 = **1.105**.

**Verdict: the rule is not satisfied. Native routing stays.** There is no TOOL-FAST win on the catch-bearing class; the median is a small loss; the two wins are on the narrow-diff class (a docs/data-file diff and a test-side runner diff) where selection falls below the lane's makespan floor. Per the rule's own terms: **choose the next transfer candidate, and build no further apparatus on the selector.**

Two things the rule's verdict should not be read to erase, both measured here and both new relative to 2026-09-14: (a) the executor repair *worked* — the 64×–86× handicap is gone, the parity control shows the two arms now share machinery to within noise, and first-attempt is 5/5 everywhere; (b) the selector's **correctness** record on these fixtures is clean — zero missed catches, zero HOLDs, zero unmatched files, and TOOL-ALL surfaced three battery-lane reds a fast-only lane cannot see. The rule rejects the *speed* claim, not the selection.

One executor defect found and reported rather than smoothed: every TOOL-ALL battery receipt on D1/D3/D4 carries `{:kind :partial-child-census-mismatch}`, which alone flipped D4's TOOL-ALL exit to 1 with no failing test.

## 6. Raw log paths

Root: `/var/tmp/forge/measure-fx/diff-impact-replay/`

* **Ledger** (authoritative, one row per timed cell, with launch UTC and the 1-min load at launch): `ledger.tsv` — 35 rows (5 diffs × {native, tool-fast, tool-all} × 2, plus 5 parity controls).
* **Machine-readable result**: `RESULT.edn`. Intermediate: `analysis.edn`.
* **Driver transcripts**: `driver.out` (cells), `parity.out` (control), `poll-landed.log` (the landing poll, 06:07:53Z → 09:32:04Z, including the 08:10Z cap extension note).
* **Scripts actually executed**: `native-arm.sh`, `tool-arm.sh`, `parity-arm.sh`, `driver.sh`, `parity.sh`, `analyze.clj`, `report.clj`, `scripts/poll-landed.sh`.
* **Arm logs**: `logs/d{1..5}-{native,tool-fast,tool-all}-{1,2}.log`, `logs/d{1..5}-parity-1.log`; pipeline-validation passes `logs/disco-d5-{fast,all,parity}.log` (branch commit) and `logs/disco2-d5-fast.log` (landed tip).
* **NATIVE / PARITY suite receipts**: `wt-<d>/target/gate-parallel/cell-<d>-<arm>-<run>/fast/receipt.edn`, lane transcripts beside them as `lane-N.out` / `lane-N.err` / `lane-N.edn`.
* **TOOL selection receipts and partial suite receipts**: `out/<d>-<arm>-<run>/impact-fixed-point.edn` / `.json` (selection inventory with typed reasons), `results-fixed-point.edn` (one scoped observation per line: selection wall, execution wall, total, exit, classification, per-suite receipts), and `out/<d>-<arm>-<run>/fixed-point/<scope>/<suite>/receipt.edn` + `<suite>.log` + per-lane files.
* **Worktrees**: `wt-tool` (`5cc35402`, trunk), `wt-tool-pre` (`cf237f96`, validation only), `wt-d1` (`df0c9e1c`), `wt-d2` (`8aedb65e`), `wt-d3` (`ad05e27c`), `wt-d4` (`7bf44cfe`), `wt-d5` (`6e77580f`); `shadow/` and `shadow-pre/` (three files each).
