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
3. Choose the cheapest authority. Use native `rg` and `apply_patch` for a
   known small edit. Use the installed `clj-surgeon` CLI for unknown owners,
   extraction, dependency-aware movement, namespace renames, declaration repair,
   and guarded multi-owner changes.
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

## Timing rule

Measure complete verified task time, including orientation, retries, emission,
and proof. Surgeon can win by several times on high-fan-out structural work,
but native tools usually win a one-site edit or literal search. Keep the native
route when the structural operation cannot remove enough model interaction to
pay for itself.
