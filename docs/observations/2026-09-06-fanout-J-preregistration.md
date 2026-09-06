# Fan-out J — pre-registration, SERVED DISCOVERY route vs native (17:09Z, before any J arm; runs in the next timing slot after Astra's whole-task pair releases its window)

Hand-probe receipts (this seat, /var/tmp/forge/j-probe/): prepare-change on 8171 REFUSED semantic-provider-unavailable (cclsp 7890 is another seat's; known, inb-41c1cc) in 0.49 s. A batched inspect_clojure  over the 20 files returned 20 requests · 59 matches · every match carrying "inside" = the owning top-level form, in 0.33 s (match-result.json). That is exactly the [{file, within{form}, matches}] list the accepted apply call needs.

One named change vs cohort I: the J actor is told to obtain the owner list from ONE inspect_clojure match batch (after rg -l) instead of hand-typing or regex-scripting it. Everything else identical to I (seed 0eecb55a, owners.json, proof-b witness, runner/detector, -c registration, servers on 181c365c). Runner: 'J' arm kind = 'I' with SERVED_PREFIX; re-frozen; freeze output recorded.
Schedule: J1, J2, J3, J4 (clock; native and I medians from cohort I retained; positional pairing J_k with I_k for reporting; no new controls).
Hypothesis (falsifiable): J median wall < I median (53.4 s) by more than 10 s, 4/4 correct, ≤ 1 refusal per arm. Falsifier: J median ≥ 43.4 s, or any J incorrect, or ≥ 2 refusals in any arm.
Expectation (Fable, from the ethnography): discovery+compose 19–45 s → ~3–8 s (rg + one 0.3 s inspect + building the list from a returned JSON); J median 28–38 s ≈ 2.5–3.4x vs native 96.6 s. Astra's line: (appended verbatim when posted; the cohort does not wait).
Limitations: same as I plus: the actor must still map "inside" names to per-file counts (a small script or hand count); the fixture's aliases (db / mdb / unqualified) remain the actor's to determine.
