# GPT-5.3 Spark clj-surgeon caller screen

**Status:** preregistered before the first evaluated model call  
**Date:** 2026-08-30  
**Product source:** `c55de2279826af5ed21c90981591479dd2e802b2`
(`origin/release/closed-relations-published`)  
**Harness:** `bench/run_spark_caller_screen.sh`

## Question and evidence class

Does the subscription-backed Codex model named `gpt-5.3-spark` combine its
reported low latency with correct, economical use of the current clj-surgeon
MCP contract?

Every result in this study is **synthetic screen evidence**, not an acid-test
performance claim.  This label follows the doctrine ratified at
`8539e7ea4ed9df8dc1e51b057ba58f9760e2a28e`: only a matched serial Anvil
comparison on a real historical decision can prove a performance improvement.

## Frozen availability rule

The harness first snapshots the subscription account's refreshed Codex model
catalog with `codex debug models`.  It then invokes these names in order:

1. `gpt-5.3-spark`
2. `5.3-spark`
3. `spark`
4. `gpt-5.3-codex-spark`

The fourth name is a preregistration amendment made after the refreshed
subscription catalog exposed that exact canonical identifier.  The first three
probes had already returned HTTP 400 before any model response or completed
turn; therefore no evaluated model behavior was visible when the canonical
name was added.  The failed aliases and this amendment remain in the receipts.

Each probe uses a fresh Codex home containing only the existing ChatGPT auth
receipt, `--ignore-user-config`, `--ignore-rules`, an empty workspace,
`model_reasoning_effort="high"`, and the prompt `Reply with exactly:
SPARK_OK`.  A name is reachable only when Codex exits zero and the last agent
message is exactly `SPARK_OK`.  The first reachable name is frozen for every
Spark cell.  If none is reachable, the harness stops before all characterization
cells; the refreshed catalog is the authority for reporting exactly which
subscription models were available.  No API key or metered API route is used.

## Common controls

- Calls are serial.  Each run gets a fresh Codex home and workspace.
- All model runs use reasoning effort `high`, `--ephemeral`, and a 180-second
  timeout.
- Surgeon cells get a fresh isolated 512 MB MCP process on an ephemeral port,
  launched from a detached worktree whose source commit and tree are exactly
  the product source above.  MCP bootstrap finishes before measured model wall
  starts and is recorded separately.
- End-to-end wall is monotonic process wall from immediately before
  `codex exec` until process exit.  Token counts come from the final
  `turn.completed.usage` object in the raw JSONL stream.
- Fixtures are copied from the product commit.  Exact output bytes are scored
  against their frozen `after` trees.  Read-only cells are scored against their
  frozen `before` trees.
- `wrong-subject` means any changed or additional Clojure source path outside
  the frozen expected source set, or any expected source file whose final bytes
  differ from the frozen outcome.  It must be zero; every occurrence is
  retained and reported rather than excluded.
- All attempts are retained.  No correctness-based rerolls are permitted.

## Cells

### Speed baseline (three matched pairs)

The three prompts are `Reply with exactly: BASELINE_1`, `_2`, and `_3`.
Within-pair order is Spark/Sol, Sol/Spark, Spark/Sol.  The comparator is
`gpt-5.6-sol` at the same reasoning effort and with the same isolation.  Report
all six walls and token counts, medians, paired differences, and the ratio of
Sol median wall to Spark median wall.  `n=3` per model is a screen, not a
population estimate.

### A. Structural read (n=3)

Fixture: `exact-nested-edit/before`.  Ask for exact complete forms
`route-event` and `normalize-record` from `src/bench/pair_view.clj` using one
`inspect_clojure` call, with no shell or mutation, then exactly `READ_OK`.
The prompt does not mention request normalization.  Score correctness,
one-call behavior, mutation absence, and independently record whether the
model's first request used the just-published operation-less forms shape and
whether it omitted request IDs throughout.  Explicit `operation` or `id` is
correct but does not count as unprompted shorthand adoption.

### B. Guarded write (n=3)

Fixture: `exact-nested-edit`.  The prompt supplies file, owner `route-event`,
exact `from` value `:done`, exact `to` value `:complete`, and `matches=1`, and
requires one `edit_clojure` call without preflight.  Score exact final bytes,
one successful mutation call, zero failed mutations, and no native mutation.

### C. Refusal recovery (n=3)

Same fixture and desired outcome.  The prompt deliberately requires the first
`edit_clojure` call to use `matches=2`.  After the expected
`expect-count-mismatch`, the caller must recover only from the refusal result:
no shell/source read and one corrected retry.  A one-turn recovery is exactly
two mutation calls, the first refused with unchanged source and the second
successful and exact.  Extra reads, repeated refusals, or a third mutation are
flailing and remain visible.

### D. Mid-size routing chord (n=3)

Fixture: `decision-batch-edit`, the closest already-calibrated class to the
requested approximately-eight-change/two-file chord: six heterogeneous exact
changes across two files.  The task offers both native inspection/editing and
the complete four-tool Surgeon catalog, supplies the complete decision, and
asks the caller to choose the fastest safe route.  Score exact bytes,
wrong-subject, first mutation route, total action count, failed mutations,
wall, and tokens.  Context only—not a matched result—is the recorded Sol/high
evidence for this identical fixture: local full-catalog MCP walls 24.547,
23.142, and 24.907 seconds (median 24.547), and the later Anvil class medians
29.893 seconds compact versus 31.378 seconds native.

## Surprise hunt and reporting

The final observation must have an explicit `SURPRISES` section.  It must
inspect schema refusals, request-shape variance, unprompted shorthand adoption,
refusal vocabulary use, recovery action count, latency versus exactness,
terminal-receipt handling, routing at the six-edit boundary, and any difference
between direct tool elapsed time and complete model wall.  Positive and
negative surprises have equal standing.

## Receipts and replay

Retain every Codex JSONL stream, stderr stream, prompt, MCP stdout/stderr,
telemetry, fixture before/after hash, final source manifest, CLI/catalog
identity, and exit status.  Commit compact structured evidence plus a
SHA-256-manifested compressed raw archive.  The result directory must include a
single replay command using this harness, the exact accepted Spark name, the
product worktree path parameter, and the auth-file parameter.  Commits use
author `sol <sol@skiff>` and trailer
`Co-Authored-By: Gene Kim <genek@itrevolution.com>`.
