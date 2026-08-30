# Frozen screen: action-native structural tool naming

Hypothesis: Codex avoids unfamiliar product identity but chooses a tool whose
name directly describes the action.

The sole model-visible arm difference is structural tool name: A exposes
`clj_surgeon`; B exposes `edit_clojure`. Schema, description, behavior, result,
position, prompt, fixture, and scorer remain byte-identical.

Prediction: A 20% and B 45% structural-first, a +25pp lift. After 24 paired
runs, kill if lift is below 10pp. Otherwise advance to the already-frozen fresh
24-pair replication, where the name clears only if B-A is at least +20pp.
Wrong-subject must be 0 in every stage. The frozen 2-run A pilot must first be
valid and sub-ceiling.
