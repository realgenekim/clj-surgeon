# Repository Agent Instructions

On first action after compaction or a fresh start, read docs/plans/2026-11-22-reshape-plan.md.

Read and follow [CLAUDE.md](CLAUDE.md) before making changes. It is the
canonical repository instruction file for all coding agents, regardless of
vendor or runtime.

Load the working-tree skill before calling clj-surgeon.

Native `rg` plus a native patch is the default route for reading and editing
Clojure. Do not prefer `inspect_clojure` or `apply_clojure_changes`. Call
clj-surgeon only for a measured winner (see the plan above and the routing
block in your global CLAUDE.md). Gene's ruling, 2026-09-02, verbatim: "Pull
'Surgeon is available and expected' from every Clojure agent prompt today. It
is a standing 2x tax with no measured return, on every delegated Clojure job in
the fleet, and it is a one-line doctrine edit. Leave the server running for
agents that choose it; stop telling them to. The data says they will decline
it, and decline it correctly." Receipts:
`docs/observations/2026-09-02-captains-log-bridge-wall-clock-ideal-program.md`.

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
