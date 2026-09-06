---
name: safe-refactor
description: Safely plan and execute small, reversible Clojure refactors with contract-first verification.
---

# Safe refactor

Use this skill when a change crosses namespace or file boundaries. The goal is
an independently green, reviewable commit, not a smaller file at any cost.

## Workflow

1. Recover the owning design and write the observable contract: inputs, output,
   refusal behavior, side effects, and invariants.
2. Add or confirm boundary tests before moving code. Preserve a real-program
   fixture for the shape that motivated the refactor.
3. Choose the route with the edit-routing table below, then the cheapest
   operation for it: the installed `clj-surgeon` CLI for unknown owners,
   extraction, dependency-aware movement, namespace renames, declaration
   repair, and guarded multi-owner changes.
4. Preview first. For a structural operation, inspect the EDN plan/receipt and
   verify owners, dependencies, formatting scope, and expected match counts.
5. Apply one coherent operation, run focused tests and lint, inspect the diff,
   then commit. Do not batch unrelated extractions.
6. Repeat only after the previous commit is green. Never hide a migration behind
   a compatibility alias unless the contract explicitly requires one; migrate
   direct callers and test the public boundary.

## Useful CLI routes

```bash
clj-surgeon :op :ls-tree :dir . :grep "pattern"
clj-surgeon :op :cat :file src/app/core.clj :form my-fn
clj-surgeon :op :extract! ...
clj-surgeon :op :mv-with-deps ...
clj-surgeon :op :rename-ns! ...
clj-surgeon :op :fix-declares! ...
```

Stop on a nonzero exit or EDN `:error`. A refusal is evidence to narrow the
intent, not a cue to retry blindly. `clj-surgeon up` is a development-only MCP
onboarding command and requires `--force`; it is not part of the production
refactor path.

## Edit routing

The canonical Clojure edit-routing table lives in the `clj-surgeon` skill,
section "Edit routing (measured 2026-09-06, build >= 13c12401)". It is
reproduced here verbatim; change it there first.

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

Reads: owner and line known, read the form directly with no outline; owner
unknown in a large file, one outline or one search first; source already held,
no reread.

## Timing rule

Measure complete verified task cost: orientation, refusals, retries, emission,
and proof. The tie-break is that cost, not the existence of a receipt. Surgeon
can win on high-fan-out structural work; native tools usually win a one-site
edit or literal search. Keep the native route when the structural operation
cannot remove enough model interaction to pay for itself.
