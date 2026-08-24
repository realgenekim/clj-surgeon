# `edit_clojure` became a one-shot guarded keystroke

**Question:** Can a fresh coding agent make the exact nested edit in one MCP
call, without source reads, quoting repair, formatter drift, or a second apply?

**Answer:** Yes in the routed local cohort: three of three fresh Sol/high callers
selected `edit_clojure`, produced the exact byte hash, and finished with one MCP
call and no other tool action.

## The path was diagnostic, not cosmetic

The first fresh cohort appeared to reject the new tool. The clean benchmark
config had a hard-coded two-tool allowlist, so `edit_clojure` was absent from the
model-visible surface. The harness self-test now requires all three tools.

The first correctly wired caller selected `edit_clojure` immediately, but the
tool still exposed optional `verify`. Sol supplied `verify=fast`; the formatter
gate rejected the intentionally byte-exact fixture's pre-existing style twice.
The third call omitted verification and succeeded. That was an interface defect,
not agent stubbornness.

The final narrow contract exposes only:

```text
workspace_root? + edits[{file, within{form}, from, to, matches?}]
```

The per-edit `from` subtree and `matches` count are the compare-and-swap guard.
Parse, all-or-nothing write, read-back hashes, receipt, and undo remain mandatory.
Formatter, linter, test, and other action gates remain available through
`apply_clojure_changes`; they are intentionally absent from the keystroke-like
editor entrance.

## Final routed cohort

Task: inside `route-event`, replace only `:done` with `:complete`; preserve the
attached comment, audit payload, and every unrelated byte. Model:
`gpt-5.6-sol`, reasoning `high`.

| Replicate | Workspace CWD | Exact | First mutation | MCP calls | Other tool actions | Wall |
|---|---|---|---|---:|---:|---:|
| 1 | `/private/tmp/clj-surgeon-benchmark-setup.iFmER2/workspaces/01-r01-pair-view-expect-edit-mcp-hint-no-skill-mcp` | yes | `edit_clojure` | 1 | 0 | 21.990 s |
| 2 | `/private/tmp/clj-surgeon-benchmark-setup.iFmER2/workspaces/02-r02-pair-view-expect-edit-mcp-hint-no-skill-mcp` | yes | `edit_clojure` | 1 | 0 | 25.417 s |
| 3 | `/private/tmp/clj-surgeon-benchmark-setup.iFmER2/workspaces/03-r03-pair-view-expect-edit-mcp-hint-no-skill-mcp` | yes | `edit_clojure` | 1 | 0 | 23.528 s |

Median wall time was 23.528 seconds. Median uncached input was 8,337 tokens.
All three final hashes were
`ac1d08366599cce00e7c6fe2440e43e83aeb8af647018bfca31f89231faf5d32`.

Structured evidence is retained in
`bench/results/2026-08-24-edit-clojure-sol-high-v3`. Full raw evidence is
archived outside Git with the SHA-256 receipt in that directory.

## What this proves—and does not

This proves the named interface can make the supplied exact nested edit as a
single guarded gesture when the caller is routed to it. It also proves why the
route was previously fickle: the surface was hidden, then over-capable.

It does not yet prove unaided selection. A separate no-hint cohort must answer
whether the tool name and description alone cause fresh agents to choose it.
Nor does one fixture prove every editing shape; multi-edit batching, stale-source
refusal, comment-bearing larger subtrees, and location-handle workflows remain
separate strata.
