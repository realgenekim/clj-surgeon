---
name: clj-surgeon
description: >-
  Invoke before using Read, Edit, grep, sed, or cat on Clojure/Script/CLJC; prefer persistent MCP structural reads and verified transactions.
---

# clj-surgeon

## Discover the hot entrance first

Before a shell process reads Clojure, inspect the deferred/all-tools catalog for `inspect_clojure`, `apply_clojure_changes`, and `mcp__cclsp__*`; initial absence proves nothing.
Route: inspect_clojure -> cclsp graph query -> CLI fallback -> native fallback. cclsp reads cross-file semantics; clj-surgeon owns exact source and writes.

## Join one shared workspace stack

- If tools are absent or use the wrong repository, run `clj-surgeon up [WORKSPACE]` once. It joins the shared hot processes. Do not add a server.
- If a semantic call stalls, inspect that workspace once with cclsp
  `inspect_runtime`; for invalid sessions, missing tools, or false-green health,
  run `clj-surgeon recover [WORKSPACE]` exactly once.
  Continue only on `:recovered` plus `:next-action :none`; otherwise run its redacted
  `:report-command` once and use the named fallback. Never loop.
- Set canonical caller `workspace_root` on non-default requests and preserve prepared roots.
  For sibling Vars, never guess `../`; cclsp returns the owner root and route evidence.
- After a tool schema change, run `make mcp-reload` in the source checkout.
  Start a new agent session only when its cached schema blocks the call.

## Read one coherent snapshot

- Batch known forms, outlines, matches, and X-rays in one `inspect_clojure`
  call. One `read_complete=true` result is terminal evidence.
- Named forms include `source_anchor`; copy it into cclsp `resolve_var_surface`
  or send up to four related ordered anchors once with `resolve_var_surfaces`.
- When exact sites are unknown, call `inspect_clojure` with
  `mode=prepare-change`, one concise `intent`, and either `subject` or an
  ordered `subjects` array of fully qualified Vars.
- For an unindexed known owner, prepare with project-relative `file` plus exact
  top-level `form`; this proves source, not a reference surface.
- Preparation returns complete owners, owner authority, one basis, and one
  `next_call`. Do not repeat resolution or source reads.
- Do not split a known batch. On bounded work, stop after three unsuccessful
  source reads and choose one different route.

## Compile one decision into one transaction

- For a prepared basis, copy `next_call`. Replace every `null` with exactly
  one `keep=true`, one complete replacement form, or `delete=true` for the
  whole prepared owner; call `apply_clojure_changes` once.
- For two or more known exact changes, send one direct `changes` request with
  `id`, `files`, exactly one of `forms` or `owner`, `expect`, and one action:
  `replace`, `insert_before`, `insert_after`, `rename_binding`, or `assoc_entry`.
  Add aggregate `changes`, `edits`, and `files` counts. `verify` is basis-only.
- Use `rename_binding` with named `forms`, `from`, `to`, and
  `preserve_external_key=true`. Use `assoc_entry` to preserve comments in
  logically equal maps; add `inside` to select one semantic ancestor.
- `find` and `replace` each contain one complete form. A binding pair, map entry,
  or `case` clause is sibling syntax; target its value, not an incomplete prefix.
- A count mismatch, stale hash, overlap, parse error, verification failure, or
  owner disagreement refuses the complete request.
- Sibling insertion preserves the existing whitespace separator. A gap with a
  comment or detached source refuses; replace a larger exact span when comment
  placement is part of the decision.
- `verification_complete=true` is terminal mutation evidence. Do not reread,
  reopen a receipt, or reconstruct the transaction with `apply_patch`.

## Fall back deliberately

Native Write is right for new files; native patching for JavaScript, prose, comments,
top-level insertion, or one unsupported text edit. Use the CLI only
when MCP is unavailable or unsupported. Read [CLI fallback](references/cli-fallback.md)
or [advanced operations](references/advanced-operations.md).
