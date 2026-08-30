# Result: action-native naming clears after fresh replication

Freeze commit: `965dcac7e066ee7cd1bc834771ae65bea1b2f031`

The excluded two-start control pilot was valid and sub-ceiling: 1/2
structural-first, 2/2 semantically correct, and 0/2 wrong-subject.

The initial 24-pair cohort routed structural-first in 2/24 A (`clj_surgeon`)
and 6/24 B (`edit_clojure`): **+16.7 percentage points**. That exceeded the
frozen +10pp advance gate. The already-frozen fresh 24-pair replication routed
1/24 A and 6/24 B: **+20.8 percentage points**, clearing the required +20pp.
The final verdict is **clears-screen**.

Across both measured cohorts, all 96/96 starts were environment-valid and
semantically correct. Invalid calls, wrong-owner edits, and wrong-subject were
each 0/96.

Raw streams and fixture-scoped MCP logs are sealed in
`action-native-name-raw-streams.tar.gz` (SHA-256
`2b239ae90f404f69a4737a5d31ec155c274fc880ad4d47cbd58bc9298f58a2d3`).
The 889-entry manifest SHA-256 is
`c2e3716550fcb79ed8398f4072fce1bd9ef7d98117155b21dc4339b1cd54fce1`.

Replay from `bench/routing_tranche_20260830`:

```bash
python3 run_screen.py self-test
env -u OPENAI_API_KEY python3 run_screen.py pilot --screen action-native-name
env -u OPENAI_API_KEY python3 run_screen.py cohort --screen action-native-name
env -u OPENAI_API_KEY python3 run_screen.py replicate --screen action-native-name
env -u OPENAI_API_KEY python3 run_screen.py archive --screen action-native-name
```
