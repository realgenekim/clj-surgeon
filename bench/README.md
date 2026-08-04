# Clean Codex benchmark

Run the controlled pre/post pilot:

```bash
make benchmark-clean-codex
```

The run starts 32 ephemeral Codex sessions and can consume substantial tokens.
Override the pinned defaults only when intentionally creating a separate
benchmark series:

```bash
BENCH_MODEL=gpt-5.6-sol \
BENCH_REASONING=medium \
BENCH_RESULT_DIR=/tmp/clj-surgeon-benchmark \
make benchmark-clean-codex
```

For a two-session harness smoke test:

```bash
BENCH_TASKS=named-form \
BENCH_CONTEXTS=matched-skill \
BENCH_INCLUDE_COMPACT=false \
make benchmark-clean-codex
```

Resume a stopped result directory without rerunning completed rows:

```bash
BENCH_RESUME=true \
BENCH_RESULT_DIR=/tmp/clj-surgeon-benchmark \
make benchmark-clean-codex
```

The output directory contains `runs.tsv`, `summary.md`, and one directory per
run with the exact prompt, raw JSONL, stderr, final response, command list,
fixture hashes, and diff. The runner uses isolated Codex homes and
commit-specific CLI wrappers. It never changes the checkout under test.

See [the experiment plan](../docs/plans/clean-codex-benchmark.md) for the matrix,
scoring contract, and confounds.
