# Fan-out J result — served discovery route: LOSS on both preregistered counts (cohort 17:12:52–17:16:48Z, rc 0, 4 arms, no apparatus fault)

Preregistration: docs/observations/2026-09-06-fanout-J-preregistration.md (before any run). Receipts in docs/observations/2026-09-06-fanout-J/. Servers 7906/8171 on 181c365c; client 0.153.3; load 1.3 at start.

| Arm | actor wall | proof-inclusive | correct | Surgeon calls | witness error |
|---|---|---|---|---|---|
| J1 | 56.1 s | 60.6 s | no | 2 | db-helper-does-not-forward-to-next.jdbc |
| J2 | 46.0 s | 50.6 s | no | 2 | same |
| J3 | 61.6 s | 66.3 s | no | 2 | same |
| J4 | 54.2 s | 58.8 s | no | 2 | same |

J actor median 55.2  proof-inclusive median 59.7  tokens [254836, 253235, 342485, 299565]
Reference (cohort I, same seed/witness/runner): I actor median 53.4 s / proof-inclusive 57.8 s, 4/4 correct; N 96.6 / 101.2 s, 3/4.

Verdict against the preregistration: FALSIFIED. J median is not below 43.4 s (it is ≈ I), and 0/4 are correct under the witness. Every J arm made exactly the two mandated Surgeon calls (inspect match batch + apply), no refusals recorded in the result rows (rollout-level counts in the ethnography when filed).
Correctness note: all four failed the same witness predicate, variadic-forwarder?, because the helper body reads (apply next.jdbc/execute! args) — fully qualified — where the witness requires the alias spelling (apply jdbc/execute! args). Functionally equivalent; the preregistered correctness is the witness, so J is incorrect as registered, and the witness's spelling sensitivity is the SAME layout/spelling false-negative class Astra named for N3. Why all four J actors chose the qualified spelling while all four I actors chose the alias is the ethnography's first question.
Wall note: served discovery did not remove the compose sink. Where the time went instead is the ethnography's second question. Until it answers, the honest reading is: the 19–45 s the I arms spent composing was not the marginal cost of composing — it was the actors' cost of understanding the task, which a served list did not shorten.
Retained losses cited: cohort B (deterministic route, 2.8x loss), this cohort.
