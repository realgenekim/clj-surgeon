# Retained rename-verb screen receipts

These are the compact committed receipts for the frozen experiment at harness
commit `15c3acbe99f38bfb8d1ce242f8450b8a137de7c5`.

- `summary.json`, `runs.jsonl`, and `runs.tsv` are the registered aggregate and
  per-run scores.
- `failure-audit.json` distinguishes source-unchanged refusals from unintended
  mutations without changing the registered score or decision.
- `raw-streams.tgz` contains every Codex JSONL/stderr stream, prompt, emitted
  request, MCP surface/stdout/stderr/readiness/telemetry receipt, score, and
  final fixture source. It intentionally excludes downloaded Codex plugin and
  catalog caches, ephemeral Codex homes, and auth symlinks. Its internal
  `SHA256SUMS` verifies 269 retained files.
- `full-local-raw-archive.sha256` records the original 428 MB harness archive,
  whose bulk was repeated downloaded Codex caches rather than experiment
  evidence. The original remains at the absolute path named in that receipt on
  the experiment host.
- Top-level `SHA256SUMS` verifies the committed receipt set.

The exact replay command is in `replay-command.txt`. Replaying creates a new
output directory; it does not overwrite these receipts.
