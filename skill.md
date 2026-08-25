---
name: clj-surgeon
description: >-
  Route Clojure structural reads and writes when owner scope, guards, batching, or computed transformations matter; prefer compact MCP editing and defer heavyweight semantics.
---

# clj-surgeon

Optimize complete verified task time, not tool adoption. Choose the smallest
route that consumes evidence already supplied by the task.

## Route by work shape

- Use native `rg` for broad discovery. For bounded structural reads, batch all
  known forms in one `inspect_clojure`; a complete result is terminal evidence.
- For an already-decided batch, call compact `edit_clojure` once with
  `workspace_root`, required `edits`, and optional `programs`. Each literal edit
  has `file`, `within: {form}`, `from`, `to`, and positive `matches`. Each
  computed program has `file`, `expression`, exact `expect.matches`, and
  `expect.max_changed_characters`. Keep supplied literals as direct edits; use
  programs only for bounded repeated or derived relations.
- Use native patching when one literal edit is smaller or clearer, or for prose,
  comments, new files, and unsupported operations.
- Use heavyweight `apply_clojure_changes` only for a prepared semantic basis,
  an operation absent from compact editing, or gates that must participate in
  rollback. Do not pay its schema ceremony for ordinary exact edits.

## Preserve the one-shot advantage

- All edits and programs compile against one frozen snapshot and commit as one
  atomic transaction; they are not sequential.
- Per-edit old forms and counts are stale-source guards. Count mismatch, overlap,
  parse failure, or changed source refuses the complete request before mutation.
- Do not preflight-read an already-supplied decision or reread terminal evidence.
- Verification policy must not depend on editor choice. When the task warrants
  formatting, linting, or testing, run it once after the complete mutation for
  native and Surgeon routes alike.

Read [advanced MCP routes](skills/clj-surgeon/references/mcp-advanced.md), [CLI fallback](skills/clj-surgeon/references/cli-fallback.md), or [advanced CLI operations](skills/clj-surgeon/references/advanced-operations.md) only when the current task requires one.
