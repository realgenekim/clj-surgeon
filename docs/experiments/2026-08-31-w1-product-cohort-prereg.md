# W1 product-shaped cohort preregistration

Frozen: 2026-08-31, before any model episode.

## Question

For a product-shaped whole-form Clojure edit, does the W1 prepared-confirmation
loop reduce caller emission while preserving exactness, and what wall-time cost
does its explicit preview round trip add relative to today's ordinary compact
edit route?

This is a latency and caller-effort comparison. No routing ceiling or
sub-ceiling logic applies.

## Immutable product and executor

- Product commit: `05f5a1962e5a0c5aa0365c673994eca9024c1a44`
  (`stable-prepared-confirm-preview-20260831`, supplied published identity).
- Product server: one isolated process launched from this fresh worktree, on an
  OS-assigned loopback port. Ports 7888 and 7890 are forbidden.
- Shared server and home checkout: forbidden.
- Transport: streamable HTTP. One Codex process initializes one MCP session per
  episode and reuses that session for every call in the episode. A successful
  Arm C preview and commit from the read-served confirmation is the positive
  same-session witness.
- Executor: Codex subscription, `gpt-5.6-sol`, reasoning effort `high`, fresh
  ephemeral Codex home per episode, serial execution.
- Tokenizer: `tiktoken==0.11.0`, encoding `o200k_base`. The runner records the
  installed package receipt.

The requested `designing-experiments` skill was unavailable in the executing
session. This document and the committed harness implement the frozen-prereg
boundary directly.

## Frozen task and corpus

Use the existing `bench/fixtures/edit_portfolio/pair-view-expect-edit` capsule.
Read exactly the named whole owner `route-event` from
`src/bench/pair_view.clj`. Change only the `:finish` result's status from
`:done` to `:complete`, preserving the comment, audit payload, and every
unrelated byte.

This is a wall-class task: the semantic decision is one keyword substitution,
while the structurally guarded replacement is the complete multi-line owner.
The committed `before` and `after` trees are the exact byte oracle. Each arm in
a pair starts from a fresh copy of the same `before` tree and must reach the
same `after` tree.

## Arms

- **C — confirmation plus preview:** one eligible `inspect_clojure` read; one
  `edit_clojure` call containing only the served `confirm`, caller-owned `fill`,
  and literal `preview=true`; review the returned inert diff; one commit call
  repeating the same `confirm` and `fill` without `preview`.
- **O — ordinary compact edit:** the same eligible `inspect_clojure` read;
  ignore the served prepared request and confirmation as execution entrances;
  compose and submit the complete ordinary `edit_clojure` arguments from the
  read evidence in one commit call.

Both arms see the same published read result. This makes O a conservative
control because the descriptor is visible even though the prompt forbids using
its compact execution entrance.

## Sample and fixed schedule

Eight matched pairs, 16 serial episodes, with the pair order counterbalanced in
four `C O O C` blocks:

```text
01 C  01 O  02 O  02 C
03 C  03 O  04 O  04 C
05 C  05 O  06 O  06 C
07 C  07 O  08 O  08 C
```

Every post-token episode counts. Infrastructure failure before a model token may
be repaired and rerun only if the repair is arm-independent and recorded.
No episode is dropped for being slow or incorrect.

## Outcomes and clocks

Primary wall outcome: complete `codex exec` episode wall, measured outside the
process after the server is ready and fixture setup is complete.

The caller stream is timestamped on receipt. For each MCP call, record
start-to-completion wall for read, preview (C only), and commit. Also retain the
model gap from each completed phase to the next call start. These arrival clocks
may contain small pipe scheduling overhead; they are used symmetrically.

Caller emission is the UTF-8 length and `o200k_base` token count of the compact
JSON serialization of every caller-emitted MCP `arguments` object in the
episode. Report read, preview, commit, mutation-only, and total emission. Codex
turn usage is retained separately and is not relabeled as caller emission.

Turns are reported in two honest units: user-visible Codex turns and MCP action
boundaries. The expected route has one user-visible turn in both arms and three
versus two MCP calls for C versus O.

## Gates and scoring

An exact episode must satisfy all of the following:

1. source inventory and every byte equal the committed `after` tree;
2. no unexpected source file exists;
3. the successful terminal commit receipt says committed and verification
   complete;
4. the only structural read names `src/bench/pair_view.clj` and `route-event`;
5. the route is exactly `inspect, edit-preview, edit-commit` for C or
   `inspect, edit-commit` for O;
6. no shell, native file-change, or other MCP action occurs; and
7. wrong-subject count is zero.

Exactness must be equal between arms before any efficiency claim is admitted.
All episodes remain in the wall and emission tables even if the gate fails.

## Analysis frozen in advance

Report per-arm medians, raw values, C-minus-O median deltas, percentage deltas
using O as denominator, and median within-pair C-minus-O deltas for complete
wall and caller emission. Also report exact/total, route-adherent/total,
wrong-subject counts, user-visible turns, and MCP calls.

No significance claim is planned at `n=8` per arm. The wall verdict is the sign
and magnitude actually observed. C is allowed to be slower because preview is
an extra round trip.

Preview value is observed only if the preview exposes a defect that changes or
prevents the subsequent commit, with event evidence, while the comparable O
route would not have exposed it before attempting commit. If neither arm errs,
the verdict must say **UNOBSERVED in this cohort**. A clean preview is not
retrospectively counted as an error caught.

## Retention

Raw prompts, timestamped Codex streams, stderr, workspaces, MCP telemetry, and
package receipts remain in one local result directory and one content-addressed
archive outside Git. Git retains this preregistration, the replayable harness
and scorer, per-episode compact metrics, aggregate JSON, a report, the archive
SHA-256, product/harness SHAs, and the required commit trailers.

