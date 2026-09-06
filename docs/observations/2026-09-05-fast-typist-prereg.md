# Preregistration — the fast typist: candidate search under a cheap verifier (2026-09-05)

Ratified by Gene 21:5xZ: "yes, gpt-oss-120b via groq; use that for experiment, and tell astra about it."
Design by the pane agent (status line gpt-5.6-luna at the time) in its riff of 21:40Z; predictions are its, recorded before any run.

## Question
Does an insanely fast typist (hosted gpt-oss-120b on Groq, ~1k tok/s) reach a VERIFIED result sooner than one careful author (gpt-5.6-sol), when both are judged by the same proof gate and every cost is charged?

## Arms
- **N (control):** one Sol request from the frozen dossier → proof gate.
- **F (typist):** five gpt-oss-120b requests in parallel from the same dossier, temperature as documented per run → each through the same proof gate; the arm's result is the FIRST candidate to verify.
- **One-site control:** a single known-site edit run in both arms; native/Sol is expected to win; if F wins here the gate is suspect, not the typist good.

## Fixture
One frozen scope-roots dossier (the mission-ledger scope-roots mission on the materialized six-helper tree) with a known correct answer and an independent acceptance check that is NOT the gate (a behavioural witness). Both arms receive byte-identical prompts. Setup, request, queue time, transport, gate time, and refusal recovery are all inside the clock; startup is in the headline.

## Predictions (pane agent, 21:40Z)
- F first-verified wall 25–45 s; N 45–75 s.
- F refusal or wrong-candidate rate 10–20% of candidates.

## Decision rule
KILL the idea if the median first-verified wall of F is not at least 20% lower than N, or if ANY accepted F candidate passes the gate with a semantic mismatch against the independent acceptance check (that is a gate defect and stops the experiment; the gate is repaired before any rerun).
KEEP if F clears 20% with zero semantic mismatches; then the second experiment (repair search on refusals: several bounded next-call candidates differing only in the named missing field, no write authority) is run under its own preregistration.

## Reporting
Table first (arm, runs, median wall, p90, first-verified rate, semantic mismatches), then one line of learning, one caveat. Failed candidates are retained and counted, never dropped. No ratio from a partial cohort. Minimum cohort: 6 runs per arm, interleaved N/F.

## Not measured here
Whole-file rewrite as a primitive (needs the preimage/diff contract first: content hash + preimage, owner multiset inventory, changed-region budget, canonical diff with untouched ranges equal to the preimage, parse/lint/behaviour gates, atomic rollback with read-back hashes). Speculative pre-staging (rejected for now: multiplies proof and cleanup).

## Blockers
Groq API key on this seat as a 600 file (asked of the mayor 21:41Z). Bench script staged at /var/tmp/forge/groq-oss-bench.sh; runner to be written from it once the key lands.
