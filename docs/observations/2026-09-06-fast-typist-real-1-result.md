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
