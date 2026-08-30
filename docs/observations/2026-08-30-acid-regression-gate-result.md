# Acid test Part A: installed normalization is regression-free

Date: 2026-08-30

Verdict: **PASS — no candidate install regression.**

## Exact experiment identity

- Doctrine: `CLAUDE.md` at
  `8539e7ea4ed9df8dc1e51b057ba58f9760e2a28e`
- Product under test:
  `c55de2279826af5ed21c90981591479dd2e802b2`
- Frozen harness and preregistration:
  `62785e1fa4474719ace4c7987b54186a4c6d9936`
- Host and subject: `anvil-server`, `dev-a`
- Model route: `gpt-5.6-sol`, high reasoning, ChatGPT subscription
- Task: historical 15-form Sessionize extraction with 63 remaining caller
  occurrences
- Route: first-action fused `apply_clojure_changes`, exact project verifier,
  terminal relay
- Scorer: `bench/score_format_extraction.clj` through the common edit-portfolio
  outcome scorer

The immediate capacity fence passed at load 1.63 on 16 CPUs, with 23,549 MiB
available memory, 232,083 MiB available disk, ChatGPT authentication, zero
active benchmark processes, a clean harness checkout, and exact product and
harness commits.

## Results

| Run | Wall | Emitted output | Actions | One-shot | Semantic | Exact presentation | Route/source set |
|---|---:|---:|---:|---|---|---|---|
| 1 | 18.096 s | 425 tokens | 1 | yes | pass | no | exact |
| 2 | 17.949 s | 404 tokens | 1 | yes | pass | no | exact |
| Median | **18.0225 s** | **414.5 tokens** | **1** | **2/2** | **2/2** | **0/2** | **2/2** |

Both calls exactly matched the preregistered logical call arguments, selected
`apply_clojure_changes` first, completed one successful verified transaction,
and returned exactly:

```text
Done — changes committed and exact verification completed.
```

The common semantic scorer reported, in both runs:

```text
parseable=true
meaning-preserved=true
moved-owner-count=15
remaining-caller-occurrence-count=63
errors=[]
```

Exact presentation remains a secondary metric for this historical extraction,
as in the README cohort. Both new results differed from the frozen after bytes
only in accepted presentation while passing the unchanged meaning-preservation
score. This is reported rather than promoted to exact-byte success.

## Regression decision

The preregistered regression threshold was 23.5906 seconds: 10 percent slower
than the README Sol/high headline of 21.446 seconds. The observed median was
18.0225 seconds, **15.96 percent faster** than that headline and 6.21 percent
faster than the earlier 19.216-second promoted-product median.

Therefore the just-installed read-request normalization surface is
**regression-free by the acid-test gate**. It is not a candidate install
regression. Against the retained same-task native receipt of 207.898 seconds,
the descriptive speedup is 11.54x; no native arm was repaid.

Wrong-subject count: **0**. Both runs moved the required 15 owners, preserved
all 63 required caller occurrences, changed only the accepted source set, and
reported no semantic errors.

## Raw evidence and replay

The raw streams remain on Anvil at:

```text
/srv/fleet/dev-a/clj-surgeon-acid-results/part-a-c55-20260830-001
```

They include both `events.jsonl` streams, event clocks, exact prompts, client
tool surfaces, call arguments, semantic score receipts, diffs, terminal
receipts, start/final hashes, stderr, and the aggregate `runs.tsv`.

- Raw manifest SHA-256:
  `d3414b796ef6a7de73290cfdeb217b5c249e9e7b6e8b2e6e07f3c424e12e78ed`
- Raw archive SHA-256:
  `813da5c3bf753f5d7aae0e3292a542da3ab674cda6a615451a5f39a882ee87f7`
- Verified control copy:
  `/private/tmp/clj-surgeon-acid-archives-20260830/part-a/part-a-c55-20260830-001.tar.gz`

Replay from the frozen harness checkout:

```bash
BENCH_MODEL=gpt-5.6-sol BENCH_REASONING=high \
BENCH_POST_COMMIT=c55de2279826af5ed21c90981591479dd2e802b2 \
BENCH_RUN_MATRIX='mcp:mcp-extraction-fused-tool-first-no-skill' \
BENCH_TASKS=sessionize-format-extraction BENCH_INCLUDE_COMPACT=false \
BENCH_REPLICATES=2 BENCH_PARALLELISM=1 BENCH_RETENTION=local \
BENCH_SANDBOX_MODE=danger-full-access \
BENCH_MCP_JAVA_OPTS='-J-Xms64m -J-Xmx512m' \
BENCH_RESULT_DIR=/absolute/result/path make benchmark-edit-portfolio
```
