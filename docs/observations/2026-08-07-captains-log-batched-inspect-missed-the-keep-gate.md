# Captain's Log: Batched inspect missed the keep gate

**Date:** 2026-08-07

The typed read experiment succeeded as a protocol and failed its performance
gate. `inspect_clojure` resolved the complete representative portfolio in one
call with no shell calls, process startups, failed calls, or partial evidence.
All four counterbalanced MCP runs were correct. The 27.969-second median was
13.8% below the current CLI structural route's 32.442 seconds—not 2x faster and
not the minimum credible 30% improvement.

## What shipped in the experiment

The server exposes exactly two tools: read-only `inspect_clojure` and guarded
`apply_clojure_changes`. Inspect accepts an ordered batch of `forms`, `outline`,
`match`, and capability-limited `xray` requests. It captures each canonical
project-relative Clojure file once, evaluates against immutable snapshots, and
returns complete structured evidence or refuses the complete batch. Both tools
share the same root-confinement authority.

The direct portfolio used five requests over four files. It returned seven
ordered named forms from the two benchmark summary files, a 713-line outline
with 45 named forms, two exact structural matches despite comment and string
decoys, and an X-ray aggregation value of 20. Four files were read exactly once.
The result included 12,426 exact source characters and a SHA-256 snapshot hash
for every file.

## Counterbalanced result

| Route | Correct | Median wall | Calls per run | Result bytes |
|---|---:|---:|---|---:|
| persistent inspect MCP | 4/4 | 27.969 s | 1 MCP, 0 shell | 24,093.5 |
| CLI structural route | 4/4 | 32.442 s | 0 MCP, 10 shell | 33,182.0 |
| native read/grep | 3/4 | 51.500 s | 0 MCP, 4 shell | 24,724.0 |

Inspect returned 27.4% fewer result-envelope bytes than CLI and used 26.8%
fewer input tokens, but complete-task time fell only 13.8%. Direct inspect
evaluation had a 132.5-millisecond median; the persistent server's cold
bootstrap was 6.694 seconds and was measured separately. The remaining time is
primarily caller deliberation and transcript handling, not file parsing.

The native route's fourth run reported 7,898 source characters instead of the
expected 7,896. It remains a failed run. Correctness-gated native timing uses
the three correct runs; the negative evidence was not repaired or discarded.

## Caller defects became permanent evidence

1. The first caller guessed an unsupported X-ray expression. The tool
   description now gives a runnable computed-X-ray example, and a test pins it.
2. A successful caller repeated the complete MCP call because the concise text
   did not expose enough terminal facts. The summary now says
   `read_complete=true` and `next_action=none`, includes bounded per-operation
   facts, and has permanent source-bounded summary tests.
3. One caller counted a namespace declaration as a requested definition. The
   benchmark contract now defines the outline count precisely.
4. A caller confused request paths with distinct canonical files. The schema
   explains that repeated paths count once; the portfolio declares its exact
   expected file count.
5. Codex JSONL names the result field `structured_content`, while the harness
   initially expected the MCP wire spelling `structuredContent`. The scorer now
   accepts the observed event contract, and its self-test exercises it.

## Decision

Keep the implementation on its isolated experimental branch for further
analysis, but do not promote it on this benchmark. Batching clearly wins call
count, startup count, and result-envelope size. Those wins did not translate
into the hypothesized 2x complete-task improvement or the minimum 30% keep
signal. A next experiment must target caller deliberation and summary usability
without returning oversized source twice. It must preserve the current
correctness, confinement, one-read, all-or-nothing, and closed-world boundaries.

The in-process outline deliberately leaves `forward_refs` empty. The CLI
enriches that field with clj-kondo, but launching subprocesses from the MCP read
tool is outside this slice. Treat that difference as a documented non-goal,
not hidden equivalence.
