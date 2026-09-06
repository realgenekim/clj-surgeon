# Fast typist on a REAL repository file — the falsifier fired (2026-09-06, 00:34–00:44Z)

Mission `real-1` (fable/typist-real-repo): in clj-surgeon's own `src/clj_surgeon/diagnostic_delta.clj`, rename `finding-identity` → `finding-fingerprint` and private `field` → `finding-field`: 2 definitions + 10 call sites, two natural traps (a `let` binding named `identity`, a docstring using "identity" as a concept). Gate = the real focused test namespace under bb, 0.06 s (JVM 0.75 s); independent acceptance never loads the gate's namespace. Preregistered first (2026-09-06-fast-typist-real-1-prereg.md); raw log 2026-09-06-fast-typist-real-1-ab.log; quiet window held, `slot -t`, load 0.83–1.21 throughout.

| arm | rounds verified | candidates verified | first-verified wall, sorted (s) | median | max |
|---|---|---|---|---|---|
| N — cold Sol (`codex exec`, one process per round) | 3/4 | 3/4 | 17.43 29.68 52.02 | **29.68** | 52.02 |
| F — five gpt-oss-120b on Cerebras (pinned) per round | **0/4** | **0/20** | — | none | none |

Prereg vs observed: gate does not dominate (held: 0.06 s); N median 45–120 s (missed low: 29.7); F median 8–25 s (no median: nothing verified); F ≥ 3x faster (falsified); F ≥ 3/4 rounds (0/4); dominant F failure = the traps (wrong mechanism: F never reached the gate; every candidate failed at apply).

## Why every fast candidate failed: whitespace fidelity, not semantics
All 20 F candidates failed `git apply` and then the context-anchored applier ("anchor: no match"). Per-hunk analysis of the retained bytes: (1) continuation indentation of a multi-line form off by two spaces (16 → 14 under the `[`); (2) a paren miscount (`))))))` for `)))))`); (3) whole-block re-indent (every `+` line shifted one space). The runner does not repair whitespace inside a line, by design; the gate never saw a candidate. Arm N's one failure was a hunk header with wrong line counts.

One apparatus defect found and fixed before the counted run: `parse_unified` appended a phantom trailing empty context line to the last hunk (invisible on the toy fixtures, whose last hunk runs to end of file; fatal on a real file whose last hunk sits mid-file). The first eight rounds were void for that reason; `--replay` of the retained pre-fix Sol candidate verified after the fix. Calls used: 44 (cap was 40; the rerun cost the overage; reported, not hidden).

## Reading
The 8–31x measured on the five-file fixtures does not carry to a real Clojure file as a unified-diff typist. The fast model's output is semantically right often enough (the toy fixtures showed 33–100% single-candidate rates) but a diff demands byte-exact context lines against real, deeply indented Clojure, and gpt-oss-120b cannot hold indentation and paren counts to the byte at 1.6k tok/s. Cold Sol can (3/4).

This is not a verdict on the typist; it is a verdict on the EDIT FORM. A unified diff makes whitespace the model's job. Two forms take it away: (a) whole-file rewrite behind the preimage/diff contract (Astra's job 2; the model emits the entire file, no anchoring; a 150-line file is ~1 s at Cerebras speed; the verifier bounds it with byte identity outside a changed-region budget); (b) FORM-level requests — the typist emits the replacement s-expressions keyed by Var, and the kernel prints them through the formatter, so indentation is never the model's output. (b) is the Surgeon-shaped answer and is exactly the mission ledger's request shape. Prediction for the next A/B, recorded now: F-forms single-candidate verified rate on real-1 ≥ 50% (from 0/20); F-whole-file ≥ 40%; cold Sol unchanged. Falsifier: F-forms below 25%.

## Standing
Real-repo, unified-diff typist: NEGATIVE, preregistered, recorded. Arm T (2026-09-02) negative stands. Toy-fixture ratios stay labelled as fixture results. Next: the edit-form A/B above; Astra owns the form-level executor (pieces 2–3), Fable builds the whole-file arm in the runner as the control.

## A/B 2 (00:52Z) — the whole-file edit form on the same real file: the prediction held by 2x

Same mission, same preimage, same gate and acceptance, same provider (Cerebras, pinned), k=5; only the EDIT FORM changed: the model emits the complete new file inside one fence; the applier replaces the file's bytes, then a changed-region budget (3x the known-good diff's changed lines = 78) runs before the gate. Preregistered first (prereg.md "whole-file arm"); raw log 2026-09-06-fast-typist-real-1-ab2-wholefile.log; quiet window held, load 2.1–2.3 (not a quiet box; affects only sub-second judging).

| arm | rounds verified | candidates verified | first-verified wall, sorted (s) | median | max |
|---|---|---|---|---|---|
| F diff form (A/B 1) | 0/4 | 0/20 | — | none | — |
| **F whole-file form** | **4/4** | **16/20 (80%)** | 1.41 1.72 2.06 2.86 | **1.89** | 2.86 |
| N cold Sol (A/B 1) | 3/4 | 3/4 | 17.43 29.68 52.02 | 29.68 | 52.02 |
| fanout, whole-file (2 rounds) | 2/2 | 10/10 | 2.36 5.91 | 4.14 | 5.91 |

Prediction ≥40% single-candidate: observed 80%. Rounds ≥3/4: 4/4. Apply failures: 0/30 (the diff form's failure class is gone entirely). The four whole-file losses are a new class: SILENT DRIFT IN UNRELATED BYTES — a closing paren dropped from a line the mission never named (2), the namespace name rewritten in file-path spelling `clj_surgeon.diagnostic-delta` (2). All four carried exactly one extra changed line (28 vs 26); the changed-region budget cannot see a one-line drift, the gate caught every one. Replay of the four retained diff-form runs under the fixed parser: still 0/20 — the diff loss was real.

Reading: on a real Clojure file, five whole-file candidates from a 1.6k tok/s model reach a verified 12-site rename in a median 1.89 s, every round; one cold Sol author needs 29.7 s and fails one round in four. That is 15.7x on wall, cold vs cold, on real bytes, with a 0.06 s gate. The remaining failure class (drift outside the intended spans) is exactly what a FORM-level request removes by construction: the kernel prints only the named forms and cannot touch the rest. So the bar for Astra's forms arm is: match 80% or better, with untouched-region identity guaranteed rather than checked.

Caveats that stay: one mission, one file, k=5, a bb gate; the gate cost that would erase this on a JVM-gated mission was not measured tonight (the JVM run of the same test namespace was 0.75 s, so the ratio there would still be ~10x); warm Sol not measured.

## A/B 3 (01:08–01:13Z) — the multiplier as a function of gate cost

Same change, same preimage, dossier, acceptance, provider (Cerebras k=5, whole-file form); only the PROOF GATE changes: real-1 = focused test under bb (0.06 s); real-2 = the same test on the JVM (0.75 s); real-2j = four real namespaces on the JVM that transitively exercise the change (65 tests / 518 assertions, 7.1 s). A heavier honest gate does not exist in this repo for a two-file candidate workspace (the 17 s and 70 s candidates are order-dependent or read repo-relative fixtures; recorded as a finding). Cold Sol was run under the 7.1 s gate. Preregistered; raw log 2026-09-06-fast-typist-real-2-ab3-gates.log; quiet window; load 2.7–3.3.

| gate | cost | F whole-file k=5: rounds / candidates | F median first-verified | cold Sol median | ratio |
|---|---|---|---|---|---|
| bb focused test (real-1) | 0.06 s | 4/4, 16/20 | 1.89 s | 29.68 s (earlier session) | 15.7x |
| JVM focused test (real-2) | 0.75 s | 3/3, 13/15 | 2.34 s | ≈18.4 s (reconstructed: this session's Sol model wall 17.7 s + one gate) | ≈7.9x |
| JVM four namespaces (real-2j) | 7.1 s | 3/3, 10/15 | 8.55 s | **24.80 s** (measured, 3/3) | **2.90x** |

Decomposition of one real-2j round, per candidate: model 1.0–1.5 s; copy 14 ms; apply 1 ms; gate 6.8–7.1 s; acceptance 13 ms. The gate is 86% of a candidate's cost at 7 s and 1.5% at 0.06 s. Candidates are judged SERIALLY (only the model calls are parallel), so first-verified ≈ model wall + the gates of every candidate up to and including the first verified; in 5 of 6 rounds the winner sat first or second, so parallel gating would move the median only from 8.55 to ~8.3 s and cannot change the trend: the ceiling on the ratio is Sol_model / (F_model + gate), which collapses toward 1 as the gate grows.

Predictions vs observed: absolute F medians missed LOW (I priced the winner as if it sat late in the queue; it usually sits first — the serial penalty is charged on failures ahead of the winner, not on k); the ratios hit at both ends (5–15x at 0.75 s → 7.9x; 1.5–3x at heavy → 2.90x); monotone collapse hit; rate unchanged hit (87% / 67% per candidate).

Reading for Gene's question ("neighborhood multiplier vs native"): on a real file with real gates, the fast typist's multiplier over a cold careful author is ~16x at a 0.06 s gate, ~8x at 0.75 s, ~3x at 7 s. The gate, not the model, sets the number. Cheap, narrow, honest proof profiles are therefore the highest-leverage piece of the machinery — which is what the mission ledger's per-mission profile is for.

## A/B 4 (01:17–01:24Z) — the warm native comparator (Astra's protocol)

Arm NW = context-warm / process-cold Sol: one frozen orientation session (dossier + source loaded, READY, nothing solved; warm-up 10.96 s recorded, not charged), then per trial a fresh preimage in place and `codex exec fork <id>` with the trial prompt, so every trial starts from an identical orientation transcript and nothing is learned across trials. Native free to script or batch. Clock = fork spawn to independent acceptance. Four variance controls then four interleaved NW/F rounds (the preregistered six controls became four: two codex calls were spent discovering that fork's read-only sandbox was broken on this box — bwrap loopback failure — so the first orientation had loaded nothing; that session and its trial are VOID and recorded; the rerun uses the seat's fenced sandbox bypass, stated in every receipt as :sandbox "bypass"; cold Sol in A/B 1 ran read-only and emitted a diff, so this is a real difference, not pure warmth). Raw log 2026-09-06-fast-typist-real-1-ab4-warm.log; quiet window; load 1.1–3.1.

| arm | n | verified | first-verified wall, sorted (s) | median | max |
|---|---|---|---|---|---|
| NW — context-warm / process-cold Sol | 8 | **8/8** | 19.72 19.77 22.04 23.37 23.37 24.27 24.89 25.27 | **23.37** | 25.27 |
| F — Cerebras whole-file k=5 | 4 | 4/4 (18/20) | 1.14 1.72 2.04 3.26 | **1.88** | 3.26 |
| N — cold Sol (A/B 1) | 4 | 3/4 | 17.43 29.68 52.02 | 29.68 | 52.02 |

Ratio warm: **12.4x** (cold was 15.7x). Warming the context bought Sol ~21% of wall and, more importantly, reliability: 3/4 → 8/8, and all eight retained diffs are byte-identical (one batched apply_patch block each; zero answer variance). Decomposition: fork process overhead ≈ 0.95 s (well under the 3.5 s cold floor); judging 0.05 s; the rest is the model turn, 18.7–24.2 s — and 7 of 8 trials spent a self-verification turn of Sol's own choosing (git diff, rg for the trap spellings, running the bb test) inside the charged wall.

Predictions vs observed: NW median 6–12 s → 23.37 s (missed HIGH: I priced Sol's turn without its self-check); F ~1.9 s → 1.88 (hit); warm ratio 3–6x → 12.4x (missed; the typist's lead survives warmth); falsifier (NW < 3 s) did not fire. F's two losses are the known ns-name drift, caught by the gate.

Standing after A/B 1–4 on one real file: the typist's advantage is set by the gate (16x → 8x → 3x as the gate goes 0.06 → 0.75 → 7.1 s) and only mildly by Sol's warmth (16x → 12x). What remains on Sol's side is the model turn itself, most of it the model verifying its own work — which is precisely the work the ledger's gate does for the typist in 50 ms.
