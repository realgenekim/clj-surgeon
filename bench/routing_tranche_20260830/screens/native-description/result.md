# Result: native-description routing rule clears the frozen screen

Freeze commit: `965dcac7e066ee7cd1bc834771ae65bea1b2f031`

The excluded two-start control pilot was valid and sub-ceiling: 1/2
structural-first, 2/2 semantically correct, and 0/2 wrong-subject.

In the 24-pair frozen cohort, A routed structural-first in 9/24 and B in
24/24: **+62.5 percentage points**. The preregistered kill rule was lift below
+15pp or reversal on more than 4 of 12 fixtures. Observed reversals were 0/12,
so the screen verdict is **clears-screen**.

All 48/48 starts were environment-valid and semantically correct. Invalid
calls, wrong-owner edits, and wrong-subject were each 0/48.

Raw streams and fixture-scoped MCP logs are sealed in
`native-description-raw-streams.tar.gz` (SHA-256
`5daca013eca53ec2762da41416a041f333212c4f330b1d5a83d2ffd2d7a8aa04`).
The 455-entry manifest SHA-256 is
`0f6b408da029b8c7447675a8623cf552934513759be06ecd3450709dc18c2175`.

Replay from `bench/routing_tranche_20260830`:

```bash
python3 run_screen.py self-test
env -u OPENAI_API_KEY python3 run_screen.py pilot --screen native-description
env -u OPENAI_API_KEY python3 run_screen.py cohort --screen native-description
env -u OPENAI_API_KEY python3 run_screen.py archive --screen native-description
```
