# Fan-out B cohort result — deterministic Surgeon route (D) vs native (N), fresh gpt-6-astra/medium sessions, held-out repo the-gene-maven @0eecb55a, 20 files / 54 owners / 59 sites

Run 10:13–10:45Z under owner=fable purpose=fanout-B; freeze sha256 02c77d53…; schedule C1..C6 then N1,D1,D2,N2,N3,D3,D4,N4; no retries; Astra ADMIT 10:12Z. Rows: /var/tmp/forge/cell-prep/runner-b/cohort/results.jsonl (copied beside this file).

| arm | wall s | actor s | correct | load1 | surgeon att/ref/ok | tokens total | models |
|---|---|---|---|---|---|---|---|
| C1 | 73.8 | 73.8 | True | 0.86 | 0/0/0 | 267651 | gpt-6-astra |
| C2 | 121.3 | 121.3 | True | 1.35 | 0/0/0 | 329515 | gpt-6-astra |
| C3 | 110.7 | 110.7 | True | 2.62 | 0/0/0 | 424596 | gpt-6-astra |
| C4 | 98.4 | 98.4 | True | 3.10 | 0/0/0 | 405018 | gpt-6-astra |
| C5 | 103.0 | 103.0 | True | 1.38 | 0/0/0 | 293772 | gpt-6-astra |
| C6 | 85.8 | 85.8 | True | 2.34 | 0/0/0 | 277726 | gpt-6-astra |
| N1 | 115.7 | 115.7 | True | 2.20 | 0/0/0 | 342778 | gpt-6-astra |
| D1 | 274.1 | 274.1 | False | 2.33 | 9/7/2 | 1157680 | gpt-6-astra |
| D2 | 191.3 | 191.3 | False | 2.45 | 9/7/2 | 668640 | gpt-6-astra |
| N2 | 96.8 | 96.8 | True | 2.23 | 0/0/0 | 374607 | gpt-6-astra |
| N3 | 63.2 | 63.2 | True | 1.00 | 0/0/0 | 177511 | gpt-6-astra |
| D3 | 190.3 | 190.3 | False | 1.51 | 9/7/2 | 985622 | gpt-6-astra |
| D4 | 312.9 | 312.9 | True | 1.90 | 10/7/3 | 1383541 | gpt-6-astra |
| N4 | 60.0 | 60.0 | True | 2.44 | 0/0/0 | 225975 | gpt-6-astra |

controls: n=6 median 100.7 s mean 98.8 s SD 15.6 s (2SD floor 31.2 s)
N pairs: n=4 median 80.0 s
D pairs: n=4 median 232.7 s; correct 1/4
paired multiplier (N median / D median), correctness ignored: 0.34x

Preregistered expectations (before any run): Fable N 60–180 s / D < 60 s; Astra N 20–60 s / D 25–90 s.

## Reading (per the pre-registration; both expectations were wrong for D)
- Native with scripts: 10/10 correct (6 controls + 4 pairs), median 80–101 s, SD 15.6 s. Astra's native expectation (20–60 s) was optimistic; mine (60–180 s) held.
- Deterministic route: 1/4 correct, median 232.7 s — about 2.9x SLOWER than native, with 2–4x the tokens. Every D arm made 9–10 Surgeon attempts and was refused 7 times on the same shape: `apply_clojure_changes` invalid-compact-relation at [symbol_migration files 0] ("Each migration file must be [file, rows]") — a sentence present in structuredContent and ABSENT from the text block the model reads, so each fresh session retried the same wrong shape. D4 eventually succeeded on its third accepted call and passed the witness at 312.9 s.
- D1's witness failure is under Astra's independent source review (paren-moved require insertion vs the line-subsequence delta; inb-7c05bc); D2/D3 failures not yet classified. No relabeling; all outcomes retained.
- What this narrows: the deterministic verbs did not replace discovery/verification decisions for a fresh caller; the refusal contract, not the operation, was the wall. The prepared-typist results (1.98x/1.82x on the 3-owner task) are a different class and stand. Fix in flight on fable/refusal-text-shape (text ⊇ structured error + a filled example); a re-run is a NEW preregistered cell, not a retry of this one.
- Costs charged: preparation 0.10 s mechanical + apparatus build (two builders, ~3 h of review rounds), review/correction wall in the log; proof per arm ~2.3 s.

## Corrections (Astra, 10:48Z, accepted)
- Dispersion: the controls' SAMPLE SD is 17.0683 s (2SD 34.1366 s); the 15.6 / 31.2 s above are population SD. Full-boundary sample SD 17.0252 s.
- Complete medians from the frozen summary.json: N 84.6824 s, D 237.5750 s, N/D 0.3564 (D 2.8055x slower). Frozen pre-proof medians remain separate.
- Root-cause reading softened: Astra's D1 trace shows the structured error sentence WAS visible to the actor and actively used; the remaining failures were the add-only existing-target contract, a wrong alias route, and a native workaround — a route mismatch, not only missing text. Therefore B2 (refusal-text-only re-run) is NOT ratified and stays unrun; the text improvement proceeds through review on its own merits. Next step per Astra: execute the existing ordinary batched route once on a fresh fixture with the original proof (prepared offline on root), then a bounded fresh-caller recovery pilot if justified.
- D1–D3 remain frozen failures with the layout-mismatch disclosure; no post-hoc quality promotion.
