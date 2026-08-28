# Repository Agent Instructions

Read and follow [CLAUDE.md](CLAUDE.md) before making changes. It is the
canonical repository instruction file for all coding agents, regardless of
vendor or runtime.

Its Clojure routing and Surgeon-owned semantic escalation rules apply before native
Read, Edit, grep, sed, or cat touches existing source. Load the working-tree
skill before acting.

Use the hottest capable entrance: prefer the persistent `inspect_clojure` and
`apply_clojure_changes` MCP tools. Use `~/bin/clj-surgeon` only when MCP is
unavailable, the operation is not exposed there, or the CLI itself is under
test.

For non-trivial feature work, `CLAUDE.md` requires the design, planning,
testing, documentation, and verification standards that must be satisfied
before the work is complete. Do not treat those linked documents as optional
background reading.

## LID

- Mode: Scoped
- Version: 1.3.0

## LID Scope

Paths in scope:

- `src/clj_surgeon/mcp_*.clj`
- `src/clj_surgeon/mcp_server.clj`
- `test/clj_surgeon/mcp_*_test.clj`
- `docs/high-level-design.md`
- `docs/intent/mcp-operation-contract/**`
- `docs/intent/operation-algebra/**`
- `src/clj_surgeon/operation_algebra.clj`
- `src/clj_surgeon/intent_transaction.clj`
- `src/clj_surgeon/core.clj`
- `test/clj_surgeon/operation_algebra_test.clj`
- `test/clj_surgeon/intent_transaction_test.clj`
- `test/clj_surgeon/cli_dispatch_test.clj`
- `Makefile`

## Linked-Intent Development (MANDATORY)

Consult the `linked-intent-dev` skill for changes in the scoped paths. Walk
each change through the arrow of intent in one direction:

```text
HLD → LLDs → EARS → Tests → Code
```

- New features, refactors, and bug fixes use the full six-phase workflow.
- Stop after each phase for user review.
- Write design and requirement documents so they carry current intent without
  relying on conversation history.
- Within one leaf segment, cascade approved intent through requirements, tests,
  and code. Pause before crossing into another segment.

### Navigation

| What you need | Where to look |
|---|---|
| High-level design | `docs/high-level-design.md` |
| MCP operation-contract design | `docs/intent/mcp-operation-contract/` |
| EARS specifications | Beside the owning design as `*-specs.md` |
| Decision documents | `docs/decisions/` or the owning segment's `decisions/` |

### Code annotations

Use `@spec` comments to connect each specified behavior to its implementation
entry point and its direct witness tests. Do not annotate every helper.
