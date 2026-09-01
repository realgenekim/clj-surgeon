---
name: clj-surgeon
description: >-
  Use for advanced clj-surgeon workflows: semantic preparation, computed preview, extraction or movement, CLI fallback, MCP recovery, and troubleshooting. Do not invoke for ordinary inspect_clojure or edit_clojure calls; always-loaded routing and tool schemas cover them.
---

# Advanced clj-surgeon routes

Optimize complete verified task time. Do not load this skill for an ordinary
bounded structural read or already-decided compact edit.

## Trip-wire: `{:error "Unknown op: ."}` means YOUR SYNTAX, not a broken tool

Every call is **`:op <name>` plus key-value pairs** — there are no positional
arguments. `clj-surgeon outline file.clj` (positional guess) fails with
`Unknown op: .`, and an agent that guesses instead of reading this skill will
conclude the tool is broken and silently fall back to native edits — the exact
silent-adoption-killer the routing experiments measured. (Field-verified on the
bridge seat 2026-08-31: CLI + wrapper work perfectly when called as documented.)

Known-good smoke test:

```bash
clj-surgeon :op :ls :file src/my/ns.clj
```


## Load only the required reference

- Read [advanced MCP routes](skills/clj-surgeon/references/mcp-advanced.md) for semantic
  preparation, standalone computed preview, heavyweight rollback-gated
  transactions, hot-process recovery, or schema reloads.
- Read [CLI fallback](skills/clj-surgeon/references/cli-fallback.md) only when persistent MCP is
  unavailable, the operation is not exposed there, or the CLI is under test.
- Read [advanced CLI operations](skills/clj-surgeon/references/advanced-operations.md) for
  dependency-aware extraction, form movement, namespace rename, or CLJC work.

Do not reopen a reference already consumed in this task. Return to the compact
route as soon as the advanced condition is resolved.
