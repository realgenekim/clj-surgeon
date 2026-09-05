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

## One-site control (22:03–22:05Z) — the control fired: NOT KEEP; the apparatus needs a warm author

Preregistered expectation: native/Sol wins or ties a single known-site edit; "if F wins here the gate is suspect, not the typist good." Raw log: 2026-09-05-fast-typist-onesite-1.log.

| arm | runs | first-verified wall, sorted (s) | median | max | verified candidates | semantic mismatches |
|---|---|---|---|---|---|---|
| N — one Sol author, codex exec | 6 | 7.79 8.22 13.74 18.79 20.95 21.39 | **16.27** | 21.39 | 6/6 | 0 |
| F — five gpt-oss-120b (Groq) | 6 | 0.85 0.94 0.96 0.98 1.61 1.95 | **0.97** | 1.95 | 29/30 | 0 |

F won the control by 17x, so by the prereg's own rule the apparatus, not the typist, is under suspicion. Reading the numbers: a one-line literal change cost arm N 7.8–21.4 s, which is the cold `codex exec` process (startup, session, model call) — the N clock is dominated by harness startup, not authoring. That means cohort 1's 5x is at least partly the same artifact: both cohorts compare a cold Sol process against warm API calls. The gate itself is not suspect (the cheat diff that edits only the test goes gate-green and acceptance-red, semantic_mismatch=1, proven in the fake arm), but the comparison is not yet the one the prereg meant.

Decision: NOT KEEP, NOT KILL. Cohort 2 must (a) give arm N a warm author (a resident Sol session, or charge only the model's own turn) and (b) add F with k=1, so that k=5 vs k=1 isolates the value of parallel search from the raw speed of the model. Predictions for cohort 2, recorded now: F k=1 median ≈ 1.5–2.5 s with a lower verified-rate (~75–85% per candidate); N-warm one-site ≈ 3–6 s; if N-warm on scope-roots is under 5 s, the typist's advantage on bounded dossiers is mostly model speed, not search, and the search claim is dead.

Tool-shape findings that survive regardless: all 29 applying F candidates landed via the anchored applier and zero via git apply (the typist speaks context-anchored hunks); the one failure was a one-space context error the applier refused rather than repaired.

## F with k=1 (22:08Z) — the parallel-search claim is dead on these dossiers; the speed claim is model speed plus a cheap gate

Prediction recorded above: k=1 median 1.5–2.5 s with a lower per-candidate verified rate (~75–85%). Raw log: 2026-09-05-fast-typist-k1.log.

| arm | mission | runs | first-verified wall, sorted (s) | median | verified |
|---|---|---|---|---|---|
| F k=1 | scope-roots | 6 | 1.14 1.33 1.55 1.73 1.75 2.71 | **1.64** | 6/6 |
| F k=5 | scope-roots | 6 | (cohort 1) | 2.37 | 25/30 candidates, 6/6 runs |
| F k=1 | onesite | 6 | 0.45 0.62 0.83 0.84 1.02 1.42 | **0.84** | 6/6 |
| F k=5 | onesite | 6 | (control) | 0.97 | 29/30 candidates, 6/6 runs |

k=1 is FASTER than k=5 and lost nothing: 12/12 single candidates verified (prediction of a lower rate: wrong). So on these two dossiers five parallel candidates buy no first-verified time (they cost it: parallel requests and serial gating add ~0.1–0.7 s) and no reliability that a single candidate lacked. The "search under a cheap verifier" mechanism is not what produced the cohort-1 result; the result is a fast model (≈480 tok/s) plus a millisecond gate against a cold-process author. Search can only pay when single-candidate reliability is materially below 1, which these dossiers do not exercise; that is a fixture-difficulty limitation, and the next dossier must be one where a single candidate fails often (the retained f0 defect class, a multi-hunk fan-out, or a real-repo file).

Standing: NOT KEEP for the search claim; a bounded-dossier speed claim is real but only against a cold author; the warm-author N arm (cohort 2) decides whether any of it survives against Sol.

## Fan-out dossier (22:13–22:15Z) — parallel search buys the answer where single candidates fail

Third mission `fanout`: rename `normalize` → `normalize-path` across three files (definition + five call sites) and thread a `:strict?` option through two paths; two traps (a docstring example and a string literal that must NOT change); 6 hunks; bb gate of 7 tests / 19 assertions in ~0.3 s; acceptance requires every authorized file changed and no other. Built without seeing results; not tuned afterwards. Raw log: 2026-09-05-fast-typist-fanout-1.log; retained candidates under /var/tmp/forge/typist-fx/F-fanout-*.

| arm | rounds verified | first-verified wall, sorted (s) | median | max | per-candidate verified | tokens/run (completion) |
|---|---|---|---|---|---|---|
| F k=1 | **2/6** | 3.87 5.66 | 4.77 | 5.66 | 2/6 (33%) | 1.7–2.7k |
| F k=5 | **6/6** | 4.98 5.29 5.44 5.63 5.66 6.50 | 5.54 | 6.50 | 15/30 (50%) | 10.2–11.4k |

Semantic mismatches 0, refusals 0. Pooled single-candidate rate 17/36 = 47%, inside the 40–70% band the dossier was designed for. Five parallel candidates cost ~0.8 s of median wall and ~6x tokens and turned 2/6 rounds into 6/6. So the search mechanism is real exactly where the prereg said it would be: where a single candidate's reliability is materially below 1. On the easy dossiers it bought nothing; here it is the difference between an answer and none.

Failure signatures (19 retained failures): 13 never applied — the model over-escaped quotes in context lines (`\"/\"` where the file holds `"/"`), so no verbatim anchor matched and the applier refused (correctly; no repair); 4 applied but missed a call site, so the tree failed to compile at the gate; 2 other. Runner gap to fix before the next cohort: the "anchor: no match" refusal does not name the file block.

Cold Sol (arm N) on this dossier: running now for the cold headline; k=5 is the typist arm to compare, with k=1 reported beside it.

### Cold Sol on the fan-out dossier (22:15–22:20Z) — the cold headline for the hard task

Raw log: 2026-09-05-fast-typist-fanout-N.log. Same dossier bytes, same gate, same acceptance; one `codex exec` (gpt-5.6-sol, medium) per round, startup charged.

| arm | rounds verified | first-verified wall, sorted (s) | median (verified rounds) | max | notes |
|---|---|---|---|---|---|
| N — cold Sol | **5/6** | 20.96 22.66 26.66 27.77 42.63 | 26.66 | 42.63 | one round applied but failed the gate |
| F k=5 — five gpt-oss-120b (Groq) | **6/6** | 4.98 5.29 5.44 5.63 5.66 6.50 | 5.54 | 6.50 | 15/30 candidates verified |
| F k=1 | 2/6 | 3.87 5.66 | 4.77 | 5.66 | |

Cold headline, fan-out: five parallel typist candidates verified in every round at a median 5.54 s; one cold Sol author verified in five of six at a median 26.66 s (4.8x). The careful author is not immune to this dossier either (one gate-red round). Under Astra's cohort-2 rule this is a COLD comparison with startup charged on both sides; the warm comparison (resident sessions on both sides) is still owed and may shrink the ratio substantially, since Sol's rounds carry a process start each time and the typist's do not.

Standing after all four cohorts tonight: (1) the search mechanism is real where single candidates fail (fan-out: 2/6 → 6/6 for ~0.8 s and ~6x tokens) and worthless where they do not (easy dossiers: 12/12 at k=1); (2) the typist's edit format is context-anchored hunks and its two failure classes are over-escaped quotes in context and missed call sites, both caught by apply/gate, never repaired; (3) every speed ratio tonight is cold-vs-cold with a millisecond gate on a five-file fixture; none of it is a whole-task or real-repo claim, and arm T (2026-09-02) stays negative for the typist inside a Sol-driven task.
