---
name: clj-surgeon
description: >-
  Invoke before using Read, Edit, grep, sed, or cat on Clojure/Script/CLJC; prefer persistent MCP structural reads and verified transactions.
---

# clj-surgeon

## Discover the hot entrance first

Before a shell process reads Clojure, inspect the deferred/all-tools catalog for `inspect_clojure`, `edit_clojure`, `transform_clojure`, `apply_clojure_changes`, and `mcp__cclsp__*`; initial absence proves nothing.
Route: inspect_clojure -> cclsp graph query -> CLI fallback -> native fallback. cclsp reads cross-file semantics; clj-surgeon owns exact source and writes.

## Join one shared workspace stack

- If tools are absent or use the wrong repository, run `clj-surgeon up [WORKSPACE]` once. It joins the shared hot processes. Do not add a server.
- On typed `semantic-provider-warming`, preserve session, PID, anchor, and `next_call`; never restart. Wait, then retry once.
- Otherwise inspect one stall with cclsp `inspect_runtime`; for invalid sessions, missing tools, or false-green health, run `clj-surgeon recover [WORKSPACE]` once. Continue only on `:recovered`; otherwise execute its `:report-command` and workspace-scoped `:fallback-command`. Never loop.
- Set canonical caller `workspace_root` on non-default requests and preserve prepared roots.
  For sibling Vars, never guess `../`; cclsp returns the owner root and route evidence.
- After a tool schema change, run `make mcp-reload` in the source checkout.
  Start a new agent session only when its cached schema blocks the call.

## Read one coherent snapshot

- Batch known forms, outlines, matches, and X-rays in one `inspect_clojure`
  call. One `read_complete=true` result is terminal evidence.
- Named forms include `source_anchor`; copy it into cclsp `resolve_var_surface`
  or send up to four related ordered anchors once with `resolve_var_surfaces`.
- When exact sites are unknown, call `inspect_clojure` with `mode=prepare-change`, one concise `intent`, and either `subject` or an ordered `subjects` array. Its authority-labeled surface already unions resolved references with exact `#'name` and `(var name)` callers.
- For an unindexed known owner, prepare with project-relative `file` plus exact
  top-level `form`; this proves source, not a reference surface.
- Preparation returns complete owners, owner authority, one basis, and one
  `next_call`. Do not repeat resolution or source reads.
- Do not split a known batch. On bounded work, stop after three unsuccessful
  source reads and choose one different route.

## Compile one decision into one transaction

- For a prepared basis, copy `next_call`, fill every `null` with one keep,
  complete replacement, or whole-owner delete, and apply once.
- For an already-decided batch, prefer one compact `edit_clojure` call with
  `workspace_root`, required `edits`, and optional `programs`. Each literal edit
  contains `file`, `within: {form}`, `from`, `to`, and optional positive
  `matches` (default 1). Each computed program contains `file`, `expression`,
  and exact `expect.matches` plus `expect.max_changed_characters`. Use programs
  only for bounded repeated or derived relations; keep supplied literals as
  direct edits. All items compile against one frozen snapshot and commit as one
  atomic transaction; they are not sequential.
  Do not send `changes`, top-level `expect`, `verify`, `basis`, or `decisions`.
  Compact edits preserve unrelated bytes and do not format the whole file.
- Use `transform_clojure` when one bounded pure relation derives replacements for one or more known leaves. Supply one SCI path ending in `transform`, exact `expect.matches`, and `expect.max_changed_characters`; preview is the default.
  Set `commit=true` only for an already-decided relation over narrow nodes; commit parses, hash-fences, reads back, and receipts. Narrow around comments. Use semantic preparation or a tested refactor operation for caller completeness or namespace mechanics.
- Use heavyweight `apply_clojure_changes` only for a prepared semantic basis,
  an operation absent from the compact editor, or when formatter/linter/test
  gates must participate in rollback. Do not pay its schema and verification
  ceremony for an ordinary already-decided edit batch. For a necessary direct
  heavyweight change, send one `changes` request with `id`, `files`,
  `forms` or `owner`, `expect`, one action, and aggregate counts. Optional
  `forms` may name one method as `{kind: defmethod, name: render, dispatch: :card}`.
  When the file and complete owner list are supplied, call this route directly:
  its guards replace a preflight read. Delete named owners with `forms` plus `delete=true`; never create markers, wait for cclsp, or patch cleanup natively.
- For a namespace move, send one `extraction` with `file`, absent `to`, `forms`, `require_policy`, every exact `caller_changes` or explicit `ignored_caller_files`, and exact counts. It creates, redirects, removes, verifies, and receipts once; direct extraction reports structural candidates, not semantic completeness.
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
- Formatting and hot laws run inside `verify=fast|full`; hot failure rolls back.
  `verification_complete=true` is terminal. On cold pending, copy `next_call` once; its status keeps the same undo receipt. Never replay or poll.

## Fall back deliberately

Native Write is right for new files; native patching is right for a smaller or
clearer literal edit, JavaScript, prose, comments, top-level insertion, or an
unsupported edit. Verification policy must not depend on editor choice. When
the task warrants formatting, linting, or testing, run it once after the whole
mutation for compact and native routes alike; do not add a call merely because
Surgeon performed the edit. Use the CLI only when MCP is unavailable or
unsupported.
Read [advanced MCP routes](references/mcp-advanced.md), [CLI fallback](references/cli-fallback.md), or [advanced CLI operations](references/advanced-operations.md) only when the current task requires one.
