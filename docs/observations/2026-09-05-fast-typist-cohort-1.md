# Fast typist — cohort 1 result (2026-09-05, 21:59–22:01Z, Anvil)

Preregistration: docs/observations/2026-09-05-fast-typist-prereg.md. Runner: clj-surgeon bridge/mission-ledger d80bf0a1 `bin/typist-run`. Raw log: 2026-09-05-fast-typist-cohort-1.log (every line the runner printed; receipts under /var/tmp/forge/typist-fx/<run>/receipt.edn with retained candidate bytes).

| arm | runs | first-verified wall, sorted (s) | median | p90 (max of 6) | verified candidates | semantic mismatches | refusals |
|---|---|---|---|---|---|---|---|
| N — one Sol author (gpt-5.6-sol, medium), codex exec | 6 | 8.55 9.54 9.63 15.19 17.17 24.74 | **12.41** | 24.74 | 6/6 | 0 | 0 |
| F — five gpt-oss-120b candidates in parallel (Groq, T=0.7), first to verify | 6 | 1.89 2.29 2.36 2.38 2.49 2.54 | **2.37** | 2.54 | 25/30 (83%) | 0 | 0 |

Decision rule (prereg): kill below 20% median gain or on any semantic mismatch. Observed: median gain 81% (12.41 → 2.37 s), zero semantic mismatches. **Cohort 1 passes the rule. KEEP is NOT declared yet: the one-site control (native/Sol expected to win) has not run; it is running next under the same runner.**

Predictions (pane agent, before any run): F 25–45 s vs N 45–75 s (direction right, both absolute ranges far too slow — the gate is bb-fast and the dossier small); F bad-candidate rate 10–20% (observed 17%).

One line of learning: with a millisecond gate, five parallel typist candidates reach a verified result 5x sooner than one careful author on a bounded dossier, and the typist's mistakes (17%) are absorbed by the gate.

One caveat: the fixture is a five-file purpose-built project with a bb-scale gate; on a real repo with a JVM gate the gate time dominates both arms and the ratio shrinks toward 1. Arm N tokens are unknown (codex exec exposes no usage); arm F charged 3.2–3.9k completion tokens per run, 75–80% of them reasoning.

Protocol amendment recorded before the cohort: the applier accepts context-anchored hunks (git apply first, anchored fallback, exactly one verbatim match, no whitespace repair). Without it 0/5 candidates applied in the shakedown; the model emits bare `@@` headers — the same shape as Codex apply_patch. Tool-shape finding: the accepted edit form for a typist is a context-anchored hunk, not a line-numbered diff.

Not a claim about: whole tasks (arm T, 2026-09-02, remains negative for the typist inside a Sol-driven task), discovery, or verification cost.
