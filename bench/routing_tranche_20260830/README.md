# Routing-lens first tranche (2026-08-30)

Four frozen A/B screens test distinct reasons Codex might choose a structural
Clojure mutation tool: incumbent-description salience, action-native naming,
minimal call schema, and native refusal handoff. The harness uses only 12
synthetic fixtures and a fixture-scoped local stdio MCP registry. It never
contacts, installs, or reloads a shared clj-surgeon runtime.

Every screen has a committed freeze, a two-start control pilot excluded from
the cohort, and 24 interleaved pairs (24 attempts per arm). Each fixture appears
twice in each arm in alternating `ABBA`/`BAAB` order. The executor is exactly
Codex `gpt-5.6-sol` at high reasoning via ChatGPT subscription auth; the runner
refuses an `OPENAI_API_KEY`.

Exact replay commands are in `replay.sh`. The naming screen runs its fresh
second cohort only if its initial +10pp advance gate clears. Raw Codex JSONL,
MCP logs, process receipts, diffs, scores, aggregates, SHA-256 manifests, and
deterministic archives are retained below `screens/`.
