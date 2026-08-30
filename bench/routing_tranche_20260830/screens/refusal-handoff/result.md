# Result: native refusal handoff clears the frozen screen

Initial freeze commit: `965dcac7e066ee7cd1bc834771ae65bea1b2f031`
and pre-cohort ceiling-repair freeze commit:
`1d40a91a7e2745fe5dc5aa3735445822bbf62f7e`.

The original excluded control pilot was valid but at ceiling (2/2
structural-first), so no cohort spend followed it. The frozen replacement
ladder retained a second 2/2 pair (`f03/f04`) and selected the next valid pair
(`f05/f06`) at 1/2 structural-first. All six screening starts were exact and
wrong-subject-free.

In the 24-pair cohort, voluntary structural-first was 16/24 A and 19/24 B.
Five B runs chose native first and received the non-mutating refusal. All 5/5
transitioned immediately to `edit_clojure` and completed through that forced
route: **100%**, above the frozen 70% kill threshold. End-to-end semantic
success was 24/24 in both arms, a 0pp treatment drop versus the >10pp kill
threshold. The final verdict is **clears-screen**.

All 48/48 cohort starts were environment-valid and semantically correct.
Invalid calls, wrong-owner edits, and wrong-subject were each 0/48.

Raw streams and fixture-scoped MCP logs are sealed in
`refusal-handoff-raw-streams.tar.gz` (SHA-256
`6d7b02d10ec245b90f83c0884277211240b208e19b3ecc2c043742f6e837bf2f`).
The 494-entry manifest SHA-256 is
`0615f6dc66e1e2edde81d4455f7de222bb978f74090818367c775427a80f5cf5`.

Replay from `bench/routing_tranche_20260830`:

```bash
python3 run_screen.py self-test
env -u OPENAI_API_KEY python3 run_screen.py pilot --screen refusal-handoff
env -u OPENAI_API_KEY python3 run_refusal_replacement.py pilot
env -u OPENAI_API_KEY python3 run_refusal_replacement.py cohort
env -u OPENAI_API_KEY python3 run_screen.py archive --screen refusal-handoff
```
