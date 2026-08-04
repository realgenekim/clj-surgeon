# Benchmark evidence retention policy

Decided 2026-08-04 (see
`docs/observations/2026-08-04-captains-log-a-clean-claude-caller.md`).

## What lives in git

- **Structured regression data**: `runs.tsv`, per-run `terminal.tsv`,
  `score.tsv`, `tool-contract.tsv`, `commands.tsv`, `usage.json`,
  `resolved-model.txt`, request/matrix TSVs, install receipts, and every
  `*.sha256` anchor. This is the time series performance-regression
  comparisons consume; it is small, diffable, and versioned with the code
  that produced it.
- **Summaries and manifests**: `SUMMARY.md`, `MANIFEST.sha256`,
  `manifest.edn`.
- **Manifest-gated compressed archives**: the
  `2026-08-04-xray-maximality-raw/*.tar.gz` set stays in-repo because
  `make test` verifies its hashes on every run (`verify-benchmark-evidence`)
  and the set is bounded and immutable.

## What is archived out of git

Raw run bulk — `raw.jsonl` transcripts, per-run workspaces, prompts,
stderr, and final-answer text — for the uncompressed 2026-08-04 result
directories. Location:

    ~/src.local/clj-surgeon-bench-archive/2026-08-04/<result-dir>.tar.gz

Each in-repo `MANIFEST.sha256` / `*.sha256` anchor was computed before
archiving, so archived bytes remain tamper-evident. Durable off-laptop
housing (GCS bucket) is the intended follow-up home for these tarballs.

## Policy for future benchmark runs

Harness runs may write full evidence locally, but commits keep only the
structured files above; raw bulk is tarred to the archive before commit.
Rationale: benchmark cadence produces multiple result trees per day;
structured scores serve regression testing, while transcripts serve
forensics and belong in an immutable archive, not the working tree.
