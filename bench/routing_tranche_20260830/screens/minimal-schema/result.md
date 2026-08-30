# Result: minimal easy-path schema is killed

Freeze commit: `965dcac7e066ee7cd1bc834771ae65bea1b2f031`

The excluded two-start control pilot was valid and sub-ceiling: 1/2
structural-first, 2/2 semantically correct, and 0/2 wrong-subject.

In the 24-pair frozen cohort, general-schema A routed structural-first in 18/24
and minimal-schema B in 16/24: **−8.3 percentage points**. The preregistered
kill rule required at least +15pp and no more than +5pp increases in invalid
calls or wrong-owner edits. Observed safety increases were both 0pp. The screen
is therefore **killed for insufficient routing lift**, not for safety.

All 48/48 starts were environment-valid and semantically correct. Invalid
calls, wrong-owner edits, and wrong-subject were each 0/48.

Raw streams and fixture-scoped MCP logs are sealed in
`minimal-schema-raw-streams.tar.gz` (SHA-256
`defd764c4e77d990bc5a2f8cd6e8efb065a2dce7558dc7dd989cebce1231430c`).
The 455-entry manifest SHA-256 is
`46bd736f34234d14fc7ce7f82f8f7bb7e090a63b64c8076f764640e5bc92c072`.

Replay from `bench/routing_tranche_20260830`:

```bash
python3 run_screen.py self-test
env -u OPENAI_API_KEY python3 run_screen.py pilot --screen minimal-schema
env -u OPENAI_API_KEY python3 run_screen.py cohort --screen minimal-schema
env -u OPENAI_API_KEY python3 run_screen.py archive --screen minimal-schema
```
