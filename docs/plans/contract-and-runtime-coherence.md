# Contract and Runtime Coherence

**Status:** Implemented and verified
**Motivating issue:** `clj-surgeon-kbb`

## Outcome

The local clj-surgeon and cclsp stack has one authoritative description for
each boundary that has repeatedly drifted:

- clj-surgeon's published direct-change schema and its request validator agree
  by construction;
- cclsp reports and acts on one workspace lifecycle projection for cold,
  warming, ready, failed, and stale language-server children.

A developer changes one data definition or state transition. Tests fail before
the shared services can publish a contradictory contract or readiness claim.

## Field Evidence

Recent Beads record repeated variants of the same defects:

- `clj-surgeon-a5q`, `clj-surgeon-pgu`, `clj-surgeon-0ck`, and
  `clj-surgeon-e3n` found fields or grammar that differed between the model
  schema, handler, and validator;
- `clj-surgeon-swr`, `clj-surgeon-9gj`, `clj-surgeon-k6q`,
  `clj-surgeon-fhv`, and `clj-surgeon-bfn` found lifecycle state that differed
  between health, initialization, semantic admission, and recovery;
- the highest-churn implementation files are also the owners of these
  duplicated truths: `mcp_contract.clj`, `mcp_tool.clj`, cclsp
  `tools/symbols.ts`, `lsp-client.ts`, and `lsp/server-manager.ts`.

## Bitter-Lesson Boundary

This refactoring encodes mechanics, not architectural judgment. It does not
choose edits, infer retries, lengthen deadlines, or hide provider failure. It
removes duplicate representations of facts the caller already depends on.

## Public Contract

No successful request or response shape changes.

- Existing direct and basis-backed MCP requests remain valid.
- Unknown fields, invalid action combinations, stale sessions, and failed
  initialization still refuse.
- A workspace lifecycle value is closed data: `cold`, `warming`, `ready`,
  `failed`, or `stale`.
- Health, runtime inspection, recovery, and semantic refusals must report the
  same lifecycle for the same runtime snapshot.
- Refactoring failures do not mutate source or restart shared services.

## Safety Invariants

- The JSON Schema is the public contract; validator field sets cannot silently
  grow or shrink away from it.
- Contract checks compare nested owner, action, expectation, and aggregate
  fields, not only top-level keys.
- Lifecycle projection is pure and depends only on one captured runtime
  snapshot and the current generation identity.
- Projection does not start, stop, recover, or await a process.
- Request admission uses the same projection that health and diagnostics show.
- Existing timeouts, cancellation, and shared-initialization ownership remain
  unchanged.
- Existing dirty-worktree changes in both repositories are preserved.

## Implementation Shape

### clj-surgeon

Move the direct-change JSON Schema to a dependency-free schema namespace.
Derive validator field sets from that schema, or expose a pure contract-shape
projection that mechanically proves parity for every closed object. Keep
semantic validation in `mcp-contract`; do not attempt to replace it with a
generic JSON Schema engine.

### cclsp

Add one pure workspace lifecycle projector. Feed it the server state,
generation identity, and failure facts. Reuse its result in runtime status,
warming refusals, health, and targeted recovery. Keep process management and
JSON-RPC effects in their current owners.

## Test Plan

### Surgeon contract matrix

| Boundary | Required proof |
|---|---|
| Top-level request | published and accepted keys match |
| Direct change | owner, selector, action, and expectation keys match |
| Nested actions | rename and assoc-entry keys match |
| Aggregate expectation | all required counts match |
| Basis route | basis fields remain disjoint from direct fields |
| Drift canary | adding a field to only one representation fails |

### cclsp lifecycle matrix

| Inputs | State |
|---|---|
| no child | `cold` |
| child, initialization pending | `warming` |
| initialized current generation | `ready` |
| initialization error | `failed` |
| initialized obsolete generation or dead child | `stale` |

Test each state as plain data. Add narrow integrations proving that status and
semantic admission consume the same projection. Do not create subprocesses for
the pure matrix.

## Verification Gates

1. Existing focused tests pass before refactoring.
2. New parity and lifecycle tests fail for deliberately divergent fixtures.
3. clj-surgeon MCP and full suites pass without weaker assertions.
4. cclsp formatting, lint, typecheck, and full tests pass.
5. `make mcp-reload` publishes the unchanged Surgeon schema hash or explains
   an intentional contract change.
6. One real structural request and one real semantic runtime inspection agree
   with health after reload.

## Definition of Done

One data definition owns the direct-change surface, one pure state projection
owns cclsp workspace lifecycle, and every public consumer derives from those
facts. A one-sided contract or lifecycle edit fails a focused test before it
can reach the shared services.

## Completion Evidence

- Surgeon now derives top-level and nested direct-change field sets from
  `clj-surgeon.mcp-schema`; the parity matrix includes a deliberate drift
  canary.
- cclsp now projects `cold`, `warming`, `ready`, `failed`, and `stale` from one
  pure lifecycle function consumed by runtime status and semantic refusal.
- Surgeon MCP verification passed 136 tests and 1,102 assertions. cclsp passed
  311 tests with 976 expectations, plus clean TypeScript typecheck and Biome.
- `make mcp-reload` published the contract through the live server without a
  process restart.
