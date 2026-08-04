# Confirm Bounded Claude Fable and Opus Use

**Status:** Complete; bounded Fable and Opus matrix passed on 2026-08-04
**Severity:** P0 release blocker

## Evidence

Four `claude -p` children were launched inside one blocking orchestration call.
One or more stalled before invoking clj-surgeon, withholding all results for
3,347 seconds until the user aborted. An escalated process check also stalled.
A later Fable smoke wrapped in a hard 30-second `gtimeout` remained blocked for
345 seconds, proving the command never started inside that execution boundary.

All four fixture hashes remained unchanged and no plan artifact appeared.
This is infrastructure failure, not evidence about Fable, Opus, or
clj-surgeon.

## Required Outcome

After one bounded authentication smoke succeeds, run Fable and Opus on:

1. the independently scored real `ops-registry` X-ray aggregation; and
2. a comment-preserving `pair_view.clj` edit with separate plan and apply.

Run the four children in parallel, but give every child an independent
90-second deadline, output file, state receipt, and cleanup path. Stream each
result independently. One stalled child must not block or erase the others.

## Tests and Verification

- Record requested aliases and resolved model IDs.
- Hash the exact installed skill and fixture for each child.
- Retain prompt, tool calls, wall time, usage, final answer, score, source
  hashes, and diff.
- Read tasks must preserve bytes; edit tasks must match the exact expected file.
- Timeouts and infrastructure failures are first-class results.
- Add a fast harness self-test that proves deadlines and partial-result
  preservation without calling a model service.

## Done When

Fable and Opus each complete both tasks correctly through the actual installed
Claude skill, and a deliberately stalled fake child cannot delay or destroy the
other receipts.

## Resolution

`bench/run_clean_claude.sh` now owns the prompts, independent scoring, model
aliases, 90-second per-child process deadlines, clean workspaces, immutable CLI
and installed-skill validation, streamed terminal receipts, and durable raw
results. Its fake-child self-test completed in about 1.02 seconds: successful
and failing receipts survived before a ten-second fake child was terminated at
its independent one-second deadline, and all partial raw output remained.

The paid 2x2 matrix is archived at
`bench/results/2026-08-04-claude-fable-opus-v2/`. Every child loaded the exact
stable installed skill (`SKILL.md` SHA-256 `2d9c5421480f047a66349d0af2cef399730ab9da5fb9d3d45b1ddea31d251775`;
package source SHA-256 `23156b3dc2661bbf611c2a194dcfb35dc6e314bcdbcf23a999eae9411c25883f`).

| Task | Alias | Resolved model | Wall | Result |
|---|---|---|---:|---|
| real `ops-registry` X-ray | `fable` | `claude-fable-5` | 36.971s | exact; source unchanged |
| guarded `pair_view.clj` edit | `fable` | `claude-fable-5` | 25.677s | exact; separate plan/apply |
| real `ops-registry` X-ray | `opus` | `claude-opus-5` | 41.274s | exact; source unchanged |
| guarded `pair_view.clj` edit | `opus` | `claude-opus-5` | 26.220s | exact; separate plan/apply |

Fable's read and edit plan were one-shot after skill loading. Opus reached the
same exact results with repair: five X-ray calls on the read, and a rejected
first edit plan followed by a correct narrower plan. The archive records these
as non-one-shot rather than overstating the result.

Repeatable acceptance is exposed as `make benchmark-claude-skill`; the
non-service proof is `make benchmark-claude-skill-self-test`. The bounded
Codex peer is `make benchmark-codex-skill`, and `make benchmark-agent-skills`
runs both skill batteries.
