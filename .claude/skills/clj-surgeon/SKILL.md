---
name: clj-surgeon
description: >-
  Use for advanced clj-surgeon workflows: semantic preparation, computed preview, extraction or movement, CLI fallback, MCP recovery, and troubleshooting. Do not invoke for ordinary inspect_clojure or edit_clojure calls; always-loaded routing and tool schemas cover them.
---

# Advanced clj-surgeon routes

Optimize complete verified task time. Do not load this skill for an ordinary
bounded structural read or already-decided compact edit.

## Load only the required reference

- Read [advanced MCP routes](references/mcp-advanced.md) for semantic
  preparation, standalone computed preview, heavyweight rollback-gated
  transactions, hot-process recovery, or schema reloads.
- Read [CLI fallback](references/cli-fallback.md) only when persistent MCP is
  unavailable, the operation is not exposed there, or the CLI is under test.
- Read [advanced CLI operations](references/advanced-operations.md) for
  dependency-aware extraction, form movement, namespace rename, or CLJC work.

Do not reopen a reference already consumed in this task. Return to the compact
route as soon as the advanced condition is resolved.

## Prepared confirmation session affinity

A prepared confirmation is bound to the MCP session that served it. For
Streamable HTTP, retain and reuse `Mcp-Session-Id`. For stdio, use the same stdio connection.
If the caller cannot preserve session affinity, submit the served
`prepared_request.arguments` as ordinary explicit edit arguments.

Use ok to distinguish success from refusal.
Never infer the outcome from descriptor or digest presence.
