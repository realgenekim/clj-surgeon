---
parent: mcp-operation-contract-design
prefix: MCP-OP-HELPER
---

# Selected-helper closure extraction (`helper_extraction`) — design (HLD → LLD)

Status: intent under independent review (Astra, 2026-09-05 window). Plan of record:
`docs/plans/helper-closure-extraction.md`, revision 3. Nothing registered below is implemented.

## HLD — what the operation is

One public MCP operation that takes four decisions — which helpers, which destination namespace,
what callers may call it (`alias_policy`), and which verification profile — and derives everything
the caller previously had to prepare: the owners, the reference closure, the caller partition, the
per-caller alias, one guarded transaction, and its terminal proof. The request is constant in the
number of callers; the receipt is O(1). Where a real judgment remains, the operation refuses with
bounded evidence and the one unresolved decision, and never with an escape hatch.

The operation composes three existing mechanisms and adds one planner:

| layer | existing | new |
|---|---|---|
| retirement + destination | `clj-surgeon.extract` / `mcp-extraction` (`extraction` change, `require_policy :minimal`, source-local lowering) | — |
| caller discovery + splice | `clj-surgeon.alias-migration` (`ns-bound-names`, `ns-declared-name`, `string-mentions`, whole-form splice) | reference discovery beyond head-position calls; caller partition |
| transaction + proof | the kernel entrance `alias_migration` uses (`execute-request!` with `changes`), hot rollback, read-back, inverse receipt | admitted-profile validation before write; fresh-process proof |
| planner | — | `clj-surgeon.helper-extraction` (pure, Babashka-safe): request + sources → plan or typed refusal |

## LLD — the derivation (each step is a refusal boundary)

1. Owners: each `helpers[i]` resolves to exactly one top-level `defn`/`defn-`/`def` in `from.file`;
   otherwise `ambiguous-owner` (evidence: every owner found, line and kind; `next_call nil`).
2. Body dependencies of the selected owners: moved→moved is rewritten to the destination symbol;
   moved→retained-public refuses `retained-dependency`; moved→retained-private refuses
   `private-dependency`; a namespace-sensitive form in a moved body refuses
   `namespace-sensitive-body` (explicit v1 refusal). Def initializers are admitted only for constant literals in v1.
3. References across all admitted roots (`src`, `test`, `.clj-surgeon.edn :source-roots`): head
   calls, first-class uses, `:refer`ed bare symbols, fully qualified symbols with or without a
   require, admitted def initializers. Unsupported bindings refuse `unsupported-binding`.
4. Partition: `moved-only` (replace the require), `mixed` (retain the old require, add one),
   `qualified-only` (rewrite the qualified symbol and add one destination require for admitted static callers; refuse where load semantics cannot be established), `untouched`. A supported reference
   outside `scope.paths` refuses `caller-outside-scope`. The source file counts once.
5. Alias per rewritten caller: first `alias_policy` entry colliding with nothing bound in that
   file; else `alias-policy-exhausted`.
6. `expect.caller_files`, when supplied, must equal the derived count; else `expect-mismatch`.
7. Profile capability validated (synchronous, rollback-capable, runnable now); else
   `verification-preflight-unavailable`, nothing staged.
8. One transaction: the `extraction` change + one whole-form `find`/`replace` per rewritten form
   (`expect {:matches 1}`), through the kernel entrance; proof in a fresh process; terminal
   states `committed`, `verification-failed` (restored), `verification-timeout` (restored),
   `rollback-failed` (`source_unchanged false`, recovery-required evidence).

## Planner and boundary surfaces (the shapes the witnesses bind to)

- `clj-surgeon.helper-extraction/plan` `[request sources]` → `{:ok true :plan {...} :receipt {...}}` or
  `{:ok false :error_type "helper-extraction-…" :next_call nil …evidence}` (`error_type` is a string,
  the repository convention). `:plan` carries `:destination {:file :source}`, `:files [{:file
  :partition :alias :edits [{:original :replacement}]}]` and `:transactions [{:changes [{:kind
  "extraction" …} …caller whole-form changes]}]`; `:receipt` is the O(1) success receipt minus the
  kernel fields (`committed`, `undo_receipt`, `receipt_hash`, `elapsed_ms`).
- `clj-surgeon.helper-extraction/refusal-types` is the closed set of v1 `error_type`s.
- `clj-surgeon.mcp-helper-extraction` exposes `tool` (registration map), `admitted-profiles`,
  `terminal-states`, and `terminal-receipt` (kernel result + plan → receipt with the terminal
  state and typed verification).
- `:refer` callers are rewritten to an alias require of the destination (as `alias_migration` does);
  preserving `:refer` is not a v1 behavior.
- A genuinely mutually recursive selected pair (needs `declare`) is out of v1 scope: `declare` +
  definition of a selected name refuses `ambiguous-owner`. Selected closures are directed chains.
- Lane registration during the RED phase: the witness namespace is `excluded` from every gate lane
  with its own `make helper-extraction-red` target (the repository's existing pattern for
  not-yet-implemented witnesses); it moves into the fast/integration lane in the GREEN change.

## Receipt

Counts and histograms only: helpers, source_retired, destination_created, caller_files, partition
{moved_only mixed qualified_only untouched}, sites, retained_sites, alias_histogram, verification
{profile status(checks-completed | …) structural_callers helper_behaviors compiled_callers ok}, kernel_status (the kernel's own terminal state, retained separately), closure {roots grammar dynamic_references "not-claimed"},
details_path, undo_receipt, receipt_hash, elapsed_ms. Never a file list.

## What it does not claim

Dynamic references, macro-generated calls, strings, `resolve`; whole-application compilation;
any speed result. Those are measured, not claimed (plan §"Measurement plan and falsifier").
