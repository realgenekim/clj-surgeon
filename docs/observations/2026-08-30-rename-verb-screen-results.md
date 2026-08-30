# Rename verb screen: the bytes won and Sol reliability killed it

**Decision:** KILL the proposed product surface after this screen.  
**Date:** 2026-08-30  
**Product source:** `c55de2279826af5ed21c90981591479dd2e802b2`  
**Preregistration:** `47fa7a682ae612f4bc14185fffb7652635a49aed`  
**Frozen harness:** `15c3acbe99f38bfb8d1ce242f8450b8a137de7c5`

## Answer

The compact verb delivered the predicted magnitude when the caller used it,
but `gpt-5.6-sol` did not use it reliably enough.

| Caller / arm | Exact | Adopted exact verb | One shot | Refused request shapes | Completed-request median |
|---|---:|---:|---:|---:|---:|
| Sol V, compact verb | 4/6 | 4/6 | 4/6 | 2/6 | 64 B / 19 `o200k_base` tokens |
| Sol T, current `edit_clojure` | 5/6 | n/a | 5/6 | 1/6 | 1,000 B / 302 `o200k_base` tokens |
| Spark V bonus | 2/2 | 2/2 | 2/2 | 0/2 | 64 B / 19 `o200k_base` tokens |

Among completed Sol renames, V reduced emitted request size by **936 bytes,
93.6%**, and the registered request-token estimate by **283 tokens, 93.7%**.
That exceeded the 90% point prediction and the 50% magnitude gate. Every Sol
and Spark run that emitted the exact three-field verb produced the exact frozen
two-file result with all eight sites renamed.

The reliability gate failed. Sol V completed 4/6 versus T's 5/6, so V lost
correctness and the preregistered decision is KILL. The Spark 2/2 bonus supports
the weak-caller hypothesis for this cheap verb, but two synthetic cells do not
reverse the Sol cohort or authorize product code.

## What Sol actually emitted

The two V failures did not make small mistakes in the three-field tuple. Sol
ignored the advertised closed grammar and reconstructed verbose, nonexistent
APIs from the task's site inventory:

1. `01-V-r1` emitted an 826-byte `renames` array with eight rows.
2. `04-V-r2` emitted a 904-byte `edits` array using
   `owner`/`old_form`/`new_form` rows.

Both calls were refused as `invalid-rename-verb` before write authority and
left the fixture byte-identical to the before tree. The current-surface control
also fumbled once: `08-T-r4` emitted an 824-byte `edits` array using the
unsupported `owner`/`old`/`new` row shape. The published handler refused the
unknown `owner` field before mutation.

This directly falsifies the assumption that a strict closed schema makes Sol
reliable by itself. In these runs, the Codex client transported model-emitted
arguments that violated the advertised schema; server-side closed validation
remained essential. Spark followed the same V schema in both bonus cells.

## Wrong-subject audit

The registered scorer deliberately used an unusually conservative definition:
any final manifest unequal to the frozen after tree counted as
`wrong_subject=1`. It therefore flags all three source-unchanged refusals, and
the registered aggregate reports `any_wrong_subject=true`.

A post-hoc raw-stream audit separates failure to complete from mutation of the
wrong thing:

- unexpected source paths: 0;
- missing source paths: 0;
- unintended mutation runs: 0;
- successful exact after trees: 11/11;
- refused original before trees: 3/3.

This distinction does not revise a score, exclude a run, or change the KILL.
Correctness loss independently killed V. It does show that the fail-closed
server boundary worked and that no caller damaged an unintended subject.

## Secondary observations

Conditioned on completed Sol runs, median whole-turn output fell from 929 to
375.5 tokens, **59.6% lower**, and median model wall fell from 25.518 to 13.351
seconds, **47.7% lower**. These were secondary, completion-conditioned screen
observations, not preregistered performance claims. The mechanism is plausible:
successful V runs stopped constructing eight repetitive edit rows. The two V
schema fumbles erased that advantage for those attempts.

All 14 evaluated rename cells completed in one Codex turn. No model retried a
refused call. This made the first emitted request shape the complete adoption
and correctness boundary.

## SURPRISES

1. **Closed schema was not constrained decoding for Sol.** Two of six V calls
   emitted large invented grammars despite the three-field enum surface.
2. **The current control was not perfect either.** One of six T calls invented
   a superficially plausible but unsupported compact row shape.
3. **Adoption was deterministic in effect.** Every exact verb call—four Sol and
   two Spark—lowered through the published handler to the exact eight-site
   result.
4. **Spark was cleaner on the cheapest action.** The bonus was 2/2 adoption and
   exactness at 64 bytes, consistent with the weak-caller hypothesis but too
   small to promote.
5. **The preregistered wrong-subject proxy was over-conservative.** It treated
   fail-closed unchanged source as wrong-subject; the retained audit preserves
   the score and exposes the semantic distinction.

## Receipts

The compact structured results are [summary.json](../../dev/experiments/rename_verb_screen_results_20260830/summary.json),
[runs.tsv](../../dev/experiments/rename_verb_screen_results_20260830/runs.tsv),
and [failure-audit.json](../../dev/experiments/rename_verb_screen_results_20260830/failure-audit.json).
The SHA-manifested [raw-streams.tgz](../../dev/experiments/rename_verb_screen_results_20260830/raw-streams.tgz)
retains every raw model and MCP stream while excluding irrelevant downloaded
Codex caches and auth symlinks. Its SHA-256 is
`c07096d8f04baf5d48b8798d2465fe6989abaf00a1c39c26dfe5da95c83b73d2`.

The original harness archive remains local at 448,315,133 bytes with SHA-256
`007adb61477141d9930da5f3163f948e7101ded9ee82aa548680cb14c14b5314`.
The committed receipt manifest and exact replay command are in the
[receipt directory](../../dev/experiments/rename_verb_screen_results_20260830/README.md).

## Verification

The experiment-specific gate passed six Clojure tests / 39 assertions, three
Python scorer tests, both V/T model-visible surface preflights, shell and Python
syntax checks, formatting, and both receipt manifests. The cold repository
suite passed its 647-test / 5,562-assertion fast layer and 4-test /
20-assertion analyzer layer. Its 300-test MCP layer repeatedly reported two
assertions in the pre-existing cold clj-kondo timeout-classification test; that
exact Var passed alone with seven assertions. Product `src/` and `test/` are
unchanged from `c55de227`. The complete command receipt is
[verification.md](../../dev/experiments/rename_verb_screen_results_20260830/verification.md).
