# Clean Codex benchmark

Run the controlled pre/post pilot:

```bash
make benchmark-clean-codex
```

The run starts 32 ephemeral Codex sessions and can consume substantial tokens.
It runs four sessions in parallel by default. Set `BENCH_PARALLELISM=1` only
when a serial control is intentional. Override the pinned defaults only when
intentionally creating a separate benchmark series:

```bash
BENCH_MODEL=gpt-5.6-sol \
BENCH_REASONING=medium \
BENCH_PARALLELISM=4 \
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

The `computed-edit` task tests whether a clean agent can derive a replacement
from an unknown selected value in one plan call. It is the keep gate for the
native `transform` builder:

```bash
BENCH_TASKS=computed-edit \
BENCH_CONTEXTS=matched-skill \
BENCH_INCLUDE_COMPACT=false \
BENCH_REPLICATES=4 \
make benchmark-clean-codex
```

The `xray-summary` task tests routine aggregation. The `xray-checksum` stress
task tests whether one read call replaces computation that is unsafe to perform
by eye. Together they form the keep gate for the read-only `xray` builder:

```bash
BENCH_PRE_COMMIT=ad726c6 \
BENCH_POST_COMMIT=HEAD \
BENCH_TASKS='xray-summary xray-checksum' \
BENCH_CONTEXTS=matched-skill \
BENCH_INCLUDE_COMPACT=false \
BENCH_REPLICATES=4 \
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
