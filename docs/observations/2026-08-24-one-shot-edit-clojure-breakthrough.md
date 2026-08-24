# `edit_clojure` became a one-shot guarded keystroke

**Question:** Can a fresh coding agent make the exact nested edit in one MCP
call, without source reads, quoting repair, formatter drift, or a second apply?

**Answer:** Yes. Three of three directly routed fresh Sol/high callers finished
with one MCP call and no other tool action. More importantly, ten of ten callers
in the realistic MCP-plus-installed-skill condition selected `edit_clojure`,
made one successful guarded mutation, and produced the exact byte hash.

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

## Installed-skill cohort

This cohort received no task-specific tool hint. It had the three MCP tools and
the packaged clj-surgeon skill that real repositories install.

| Replicate | Workspace CWD | Exact | Successful mutation | MCP calls | Reads before/after edit | Wall |
|---|---|---|---|---:|---|---:|
| 1 | `/private/tmp/clj-surgeon-benchmark-setup.VmgY8m/workspaces/01-r01-pair-view-expect-edit-matched-skill-mcp` | yes | one `edit_clojure` | 2 | 0 / 1 | 48.719 s |
| 2 | `/private/tmp/clj-surgeon-benchmark-setup.VmgY8m/workspaces/02-r02-pair-view-expect-edit-matched-skill-mcp` | yes | one `edit_clojure` | 3 | 2 / 0 | 44.316 s |
| 3 | `/private/tmp/clj-surgeon-benchmark-setup.VmgY8m/workspaces/03-r03-pair-view-expect-edit-matched-skill-mcp` | yes | one `edit_clojure` | 1 | 0 / 0 | 41.593 s |

All three loaded the skill, selected the intended editor tool, had zero failed
mutations, and produced the exact final hash. Median wall time was 44.316
seconds, including the shell call that loaded the full skill. The remaining
variation is redundant structural reading, not mutation repair.

Structured evidence is retained in
`bench/results/2026-08-24-edit-clojure-matched-skill-sol-high-v2`.

## Final 10/10 admission cohort

The final cohort repeated the realistic condition with the synchronized,
66-line installed skill, fresh `gpt-5.6-sol`/high callers, and MCP JVMs capped
at `-Xmx512m`. It achieved 10/10 exact presentation, 10/10 `edit_clojure` as
the first mutation, ten successful guarded mutation transactions, zero failed
mutations, and zero MCP failures. Median wall time was 52.275 seconds. The
median run used two MCP calls: optional structural discovery plus the edit.

The first seven ran with concurrency two; the final three ran sequentially
after local load rose. Structured evidence and the immutable raw-evidence
receipt are retained in
`bench/results/2026-08-24-edit-clojure-matched-skill-sol-high-10x`.

An exploratory no-skill/no-hint cohort selected MCP zero of three times. Native
editing was exact in two of three runs and took a 68.112-second median across
the correct runs. This is useful negative evidence: publishing a perfect
primitive does not by itself overcome a model's native-edit prior. A routing
instruction or skill remains part of the product.

## What this proves—and does not

This proves the named interface can make the supplied exact nested edit as a
single guarded mutation both when directly routed and when discovered through
the installed skill. It also proves why the route was previously fickle: the
surface was hidden, then over-capable, and the packaged skill lagged its
development copies.

It disproves reliable unaided selection in this small sample. Nor does one
fixture prove every editing shape; multi-edit batching, stale-source refusal,
comment-bearing larger subtrees, and location-handle workflows remain separate
strata.
