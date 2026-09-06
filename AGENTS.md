# Repository Agent Instructions

**MAIN IS FROZEN (Gene, 2026-09-04): nobody merges or pushes to `main`, the mayor included, until Gene names a tested, months-dogfooded winner. Work lands on branches only. See CLAUDE.md, first section.**

Read and follow [CLAUDE.md](CLAUDE.md) before making changes. It is the
canonical repository instruction file for all coding agents, regardless of
vendor or runtime.

Clojure edit routing is governed by ONE canonical text: the `clj-surgeon`
skill, section "Edit routing (measured 2026-09-06, build >= 13c12401)". Read it
before the first Clojure edit of a task. The installed CLI is the production
entrance; persistent MCP is explicit development work, not an unconditional
preference, and no MCP server is started for an ordinary edit. Native `rg` plus
`apply_patch` stays a legitimate production default for a known small literal
change; the `bin/mission` executor is tried first only when scope, proof
profile, provider permission, and measured admission facts already fit. The
table below is that section reproduced verbatim; change it there first.

| Situation | Route |
|---|---|
| Owner and line already known | Direct bounded read: `:op :cat :file F :form NAME`, or `sed -n 'A,Bp'` on the known range. No outline. |
| Owner unknown in a large file | One outline or one search (`:op :ls`, or `rg`), then read the named form. |
| Source already held in context | No reread. |
| Known small literal change in one region | Native `rg` plus `apply_patch`. This stays a legitimate production default. |
| Bounded mechanical edit (rename across call sites, move helpers, thread a parameter, add a require across namespaces) **and** scope, proof profile, provider permission, and measured admission facts already fit | Try the `bin/mission` executor first. Do not invent a profile or a prior to force eligibility. |
| Complete reference discovery required | Surgeon semantic preparation. `rg` is not a closure proof. |
| New code, new tests, prose, non-Clojure | Native. Ineligible for this executor on this build; not forbidden territory. |
| Tonight's mandated dogfood experiment, eligible edit | Executor first, then one ledger line. |
| Fan-out via per-form MCP writes; `apply_clojure_changes` with a namespace owner; forms-scoped `find`+`replace` for insertion | Do not use. Measured losers 2026-09-02, not re-measured since. |

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
- `src/clj_surgeon/relation_census.clj`
- `src/clj_surgeon/census_pool.clj`
- `docs/intent/relation-census/**`
- `test/clj_surgeon/relation_census_test.clj`
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
