# Native control battery: does `:xray` or the outline cut what an agent reads? (2026-11-22)

Gene, 2026-11-22: "I still think the :xray has to be helpful, and we should encourage it's use?" Phase 1 step B asked for
the cheapest experiment that could settle it with a native control in every cell. This is that experiment's receipt.
Harness: `bench/native-control/` (pull requests 32 to 36). Raw rows: `~/handoffs/surgeon-seat-battery/rows.tsv` and
`/var/tmp/forge/native-control/runs/` on Anvil (never committed).

## Design

- 12 frozen read tasks over two of the owner's own repositories, pinned: curtaincall-cfp 3d6be00e and
  marvin-voice-remote d170f3d. Four exact lookups of a sibling value inside forms of 158 to 324 lines (T01 to T04); four
  inventories (T05 the 215-route `make-routes` vector, T06 the 155-route additive `routes` def, T07 the 32 event-case
  branches of `step` in a 3,720-line file, T08 the 71 public defns of a 2,072-line file); four controls, ordinary
  named-form lookups (three arglists, one ns alias; T09 to T12).
- Three arms, fresh Codex context each (gpt-6-astra, medium), randomized order, two repetitions: 72 runs. Arm 1: native
  shell reads only. Arm 2: arm 1 plus the outline `clj-surgeon :op :ls`. Arm 3: arm 2 plus `:op :xray` with one worked
  example. The arm text is the only boot text in the copied repository (267, 483 and 1,235 characters). Native fall-back
  allowed and counted in every arm.
- Expected values computed with the Clojure reader over the pinned files, independent of the tool, recomputed by an
  independent reviewer against `git show <sha>:path`; scoring blind, exact or set equality after normalization.
- Arms 2 and 3 ran the CLI from pull request 30 (`seat/ls-accepts-kondo-findings`), because on main the outline refuses
  9 of 10 of these files (clj-kondo exit 2/3 read as analysis failure). The isolation defects found on the way (login
  shell re-adding `~/bin`, the `~/bin/clj-kondo` shim exclusion, the read-only sandbox blocking the analyzer admission)
  are each guarded in `run.sh` and were proven by hand runs before the battery; a first battery that leaked the installed
  launcher was discarded.

## Result

Correctness saturated: 72 of 72 answers correct in every arm. The tasks did not discriminate on correctness; what
follows is cost only. Medians per class and arm (two repetitions of four tasks each):

| Class | Arm | Correct | Actions | Tool-output chars | Input tokens | Wall s |
|---|---|---:|---:|---:|---:|---:|
| exact | arm1 native | 8/8 | 1.5 | 3,285 | 39,816 | 9.1 |
| exact | arm2 + outline | 8/8 | 1.0 | 2,984 | 31,656 | 8.6 |
| exact | arm3 + outline + xray | 8/8 | 1.0 | 838 | 31,512 | 8.5 |
| inventory | arm1 | 8/8 | 2.0 | 24,883 | 58,577 | 45.8 |
| inventory | arm2 | 8/8 | 2.0 | 20,487 | 54,494 | 44.8 |
| inventory | arm3 | 8/8 | 1.0 | 17,850 | 37,538 | 40.4 |
| control | arm1 | 8/8 | 1.0 | 1,328 | 31,165 | 8.0 |
| control | arm2 | 8/8 | 1.0 | 11,061 | 35,285 | 7.0 |
| control | arm3 | 8/8 | 1.0 | 11,061 | 35,619 | 7.3 |

Per task, median tool-output chars (arm1 / arm2 / arm3) and arm 3 against the better of arms 1 and 2:

| Task | Class | arm1 | arm2 | arm3 | arm3 cut |
|---|---|---:|---:|---:|---:|
| T01 handle-rough-place :body for :store/stale-command | exact | 8,845 | 7,550 | 895 | 88% |
| T02 step handler for :cmd/latest-reply | exact | 584 | 290 | 708 | 2.4x worse (xray's receipt is 700 chars; arm 2's two native runs read 505 and 76) |
| T03 schedule-page script :src | exact | 6,176 | 10,931 | 1,648 | 73% |
| T04 make-routes handler for GET /people/outreach | exact | 779 | 960 | 780 | 0% |
| T05 215 routes | inventory | 24,883 | 26,315 | 25,155 | about 0%, slightly worse (the answer itself is 215 lines) |
| T06 155 routes | inventory | 21,442 | 20,487 | 20,376 | 1% (one xray refusal, repaired natively) |
| T07 32 case branches in a 3,720-line file | inventory | 96,034 | 20,350 | 9,868 | 52% |
| T08 71 public defns of a 2,072-line file | inventory | 57,650 | 14,850 | 14,850 | 0% (the outline alone is the answer) |
| T09 arglist of handle-label-toggle | control | 277 | 9,697 | 9,697 | 35x worse |
| T10 arglist of permute-agenda-items! | control | 530 | 14,127 | 14,127 | 27x worse |
| T11 ns alias | control | 2,101 | 1,076 | 138 | no tool used in any arm; native-route variance, not a tool effect |
| T12 arglist of handle-playback-failed | control | 6,053 | 12,424 | 12,424 | 2x worse on chars; actions fell from 3 and 4 to 1 |

Adoption when optional: the outline was used in 17 of 24 arm-2 runs, the tools in 22 of 24 arm-3 runs; 25 Surgeon calls,
one refusal (`:invalid-xray-result`: a `#'var` reader form is not EDN; the agent fell back to `sed -n` in the same run).
Wall per arm over 24 runs: 543 s, 514 s, 514 s, equal within noise. Median input tokens were LOWER with the tools
(49k, 36.5k, 36.1k over all runs), consistent with fewer re-read actions outweighing the longer boot text.

## Verdicts, by the brief's rule (promote only if tool output falls 30% or more at equal actions and correctness)

- **xray, exact lookups inside large forms: PROMOTE.** 72% less tool output at the same action count and correctness.
  Two of four tasks are large wins (88%, 73%), one is neutral (T04) and one is 2.4x worse (T02): when one `rg` line
  answers the question, xray's 700-char receipt is the floor and costs more than the grep. The class median rests on
  the two winning tasks; the per-task median cut is about 36%.
- **xray, inventories: not promoted on this rule** (13% cut), but it halves the median action count and wins where the
  question is file-wide (T07: 96k to 9.9k chars). Where the answer is itself hundreds of lines (T05, T06) nothing cuts
  the reading, and xray's `analyze` refuses on var-reader forms, which route tables are full of.
- **outline (`ls`), inventories over large files: wins exactly where the outline IS the answer** (T08 58k to 15k; T07
  96k to 20k), 18% median cut, below the rule.
- **outline and xray on known-name lookups: NEGATIVE** on the three control tasks where a tool was used (35x, 27x and
  2x more tool output; T11 used no tool in any arm). Told the tools exist, the agent outlines a 1,400-line file
  (9.7k chars) where one `rg -A5` returned 277, though on T12 it also cut actions from 3 or 4 to 1. This is the 2x-cost pattern the 2026-09-02 program measured, reproduced
  in miniature: encouragement without a class boundary costs more than it saves on the most common question.

## What this does not show, and what would

- Two repetitions per cell and no six-run variance floor (the test doctrine's condition 2), so wall and small
  tool-output differences are not claims. Spread between repetitions is large (T01 arm 2: 4,322 and 10,779; T08 arm 1:
  12,609 and 102,692). Only the effects that repeat identically or are tenfold (T09, T10, T07) are clearly outside any
  floor; the exact-class 72% rests on two tasks.
- Tool-output characters are a proxy for tokens read; Codex's own truncation of long outputs is not modelled.
- All 72 answers were correct, so these tasks cannot show a correctness cost or benefit; harder tasks would.
- Form size is NOT the dividing line: all four exact tasks sit inside forms of 158 to 324 lines, and two of them did not
  benefit. What the rows support is "xray wins when no single grep line holds the answer". That wording became the
  fourth arm's routing block (next section), which adds "the branch for one key, every pair inside one form" and so
  names the T02 suspect itself.

## The fourth arm (2026-11-23, 24 runs; mayor's ruling 5)

Arm 4 is arm 3's text plus one routing block, written after seeing the twelve tasks: one greppable line (a form's
header, an alias) goes to `rg`; a value no single grep line holds, or a computation over one form, goes to `:xray`; a
whole-file question goes to `:ls`. Same tasks, two repetitions, same CLI. 24 of 24 correct. Rows:
`~/handoffs/surgeon-seat-battery/arm4/`.

| Class | arm1 | arm2 | arm3 | arm4 | arm4 vs best of arm1/arm2 | arm4 vs arm3 |
|---|---:|---:|---:|---:|---:|---:|
| control, median tool chars | 1,328 | 11,061 | 11,061 | 366 | 72% less, but no tool was used: native against native | 97% less |
| exact | 3,285 | 2,984 | 838 | 838 | 72% less, PROMOTE | equal |
| inventory | 24,883 | 20,487 | 17,850 | 17,850 | 13% less, not promoted | equal |

- The control-class regression is gone: told where the answer sits, the agent used no tool on any known-name lookup.
  The 72% against arm 1 is native-route variance at two repetitions (T10 read 1.3x more than arm 1), so the claim is
  "roughly equal to native", not a promotion. The exact-class win is kept exactly (xray used on all four tasks).
- Arm 4 text 1,717 characters; tools used in 16 of 24 runs, 21 calls, 2 refusals; wall 516 s; median input tokens 34.7k.
- T02 is still 2.4x worse than one grep, as the arm-4 reviewer predicted from the "branch for one key" example in the
  routing text; that example is the first suspect, not the boundary.
- Inventories: T05 doubled (two `:xray` refusals on route maps that hold `#'var` reader forms, then native reading);
  T07 read 16,370 chars in both runs against arm 3's median 9,868, which is arm 3's first run exactly (its second was
  3,367), so repetition spread, not a regression; T06 improved slightly (19,330 against 20,377). The route-table
  refusal is the tool's defect (`analyze` cannot return a var form), not the text's.
- Caveats as before, plus one: the routing text was tuned on these tasks, so held-out tasks are needed before the
  boundary is called general.

## Decision asked of Gene and the mayor

Keep `:xray` on main. Encourage it for one class only, values that no single grep line holds, with that boundary in the
trigger text, and do not encourage the outline for known-name lookups. The fourth arm confirms that this text removes
the control-class regression and keeps the exact-class win; it leaves T02 (one-line answers) and T05 (var forms refuse)
unresolved, and its text was tuned on these twelve tasks. The gate before any boot-file change is now a held-out task
set, not these twelve.
