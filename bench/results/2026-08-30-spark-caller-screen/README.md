# GPT-5.3-Codex-Spark caller-screen receipts

This directory is the compact, replayable evidence for the synthetic screen at
product commit `c55de2279826af5ed21c90981591479dd2e802b2`.

- `availability.tsv` records every requested alias.  The subscription catalog's
  canonical name, `gpt-5.3-codex-spark`, was the only accepted Spark name.
- `baseline.tsv` is the three-pair Spark/Sol speed baseline.
- `runs.tsv` is the rescored Surgeon matrix.  The raw harness initially counted
  only an explicit `ok=false` as an MCP failure; the final scorer also counts a
  failed tool status or typed `error_type`.  This changes read refusal counts to
  5/2/2 and recovery refusal counts to 1/1/1, with no model reruns.
- `model-catalog.json`, `model-catalog.tsv`, and
  `catalog-model-identifiers.txt` are the refreshed subscription catalog.
- `raw-streams.tar.gz` contains all evaluated JSONL/stderr streams, prompts,
  MCP logs and telemetry, walls, and source manifests.  Its SHA-256 is in the
  adjacent `.sha256` file.
- `pre-amendment-alias-probes.tar.gz` contains the first three rejected alias
  probes made before the catalog-discovered canonical identifier was added.
- `identity.tsv`, `MANIFEST.sha256`, and `replay-command.txt` bind source,
  harness, CLI, files, and replay.

These numbers are synthetic screen evidence.  They are not an acid-test
performance claim.
