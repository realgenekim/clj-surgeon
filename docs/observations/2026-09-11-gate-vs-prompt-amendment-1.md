# Amendment 1 to the gate-vs-prompt pre-registration (2026-09-11T03:09Z) — adopted from Astra's review; 145ef7d2 stands as the original bets

Astra's verdict: "amend before scoring; add H0 and fresh AC/N controls; change the predictions." Adopted in full (2026-09-11-gate-vs-prompt-astra-review.md §6 is the replacement registration text). Fable's original predictions in 145ef7d2 remain on the record as historical bets and are scored as written.

## Design as amended
Six Claude arms on the same 13 tasks (frozen 12 + E4) and both callers, all contemporaneous: AC (control, re-run fresh), N (native policy, re-run fresh), H0 (AC + H's recognizer/scope/exit/logging, message = "not run" only), H (H0 + the repair bundle: verb name + request-file example generated ONLY from the attempted command and the source, "target unresolved" when the form is not unique; .edn writes under a designated request-only directory exempt, identically in H0/H), G (AC + PostToolUse admission of the exact delta against a pre-edit snapshot; ledger records write occurred / candidate rejected / restoration verified-or-unknown; a restored wrong write remains an attempted defect; E4's known wrong and correct candidates replayed through the detector as a preflight), S (AC unchanged + the clj-surgeon skill project-scoped; preflight S-SKILL-EXPOSURE archives the init skills array and the resolved skill hash; installed → offered → invoked → used are four counts). 156 scored runs + 12 N calibrations (T-03, six per caller) + delivery probes counted separately. Deadline 900 s extends through required verification; KEEP_WORKTREE for every run; classification joins tool outcomes with surviving writes (blocked Bash = attempt; gate call = verification; reverted write ≠ final producer); all hook-exposed runs blindly audited.

## Estimands and bets (two bettors)
Primary mechanism estimand: H − H0 in tasks whose final intended bytes were produced by Surgeon, per caller; accepted Surgeon completions separately. Exposure (a hook that fired) is reported first; no exposure = "the deployed hook did not reach this caller on these tasks", never "the model decides".
| bettor | AC (O/S) | N | H0 | H | G | S |
|---|---|---|---|---|---|---|
| Fable (145ef7d2, Surgeon final ≥) | 3/0 measured | – | – | 7/13 and 4/13 | ≈AC | ≈AC ±1 |
| Astra (review §6, Surgeon finals /13) | 3/0 | 0/0 | 3/0 | 4/0 | 3/0 | 3/0 |
Astra's exposure bet: H/H0 hook fires in 0–2 tasks per caller. Astra's practical-repair bar: H must beat H0 by ≥2 accepted Surgeon completions without losing more than one accepted completion. Fable's bar: H ≥ 7/13 Opus, ≥ 4/13 Sonnet.

## Codex side (Astra's design, Astra's bets: 2026-09-11-gate-vs-prompt-codex-arm-astra.md)
Callers gpt-6-astra and gpt-5.6-sol on the same 13 tasks; arms L (bare AGENTS.md), P (plate), M (MCP via repo-level .codex/config.toml), X0/X1 (PATH shim on sed/python3 refusing .clj writes, without/with repair). Astra's main bet: M − L ≥ 3 Surgeon-final tasks for Astra and ≥ 2 for Sol without losing more than one accepted completion; X0/X1 exposure 3/13 Astra, 0/13 Sol. Built and run by an Opus agent from Astra's spec; Astra does not touch the runner.
