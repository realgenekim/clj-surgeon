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
2. Before moving code, classify the recovered contract. If the refactor re-expresses a load-bearing promise that a later edit could silently narrow while existing tests stay green, run `linked-intent-testing`’s five steps, link an independent boundary witness, and take the highest fitting ratchet rung. A source scan may witness an architecture promise; it never substitutes for observable behavior. If the change is only semantics-preserving relocation—with no new or consolidated authority, policy, boundary, refusal, or behavior—record `LID: skipped — pure move; no promise added or changed`, then preserve the motivating real-program fixture. Failure to name the promise is a stop, not a skip.
   Skip rule: The skip is earned only by an affirmative diff-level claim: the change relocates or renames code while preserving public inputs, outputs, failures, side effects, authority, and all boundary ownership; it neither consolidates competing copies into a new source of truth nor creates a policy seam. The complete behavior must already fit in ordinary direct examples, and a future narrowing could not plausibly stay green by editing those examples alongside the code. Record the one-line reason in the refactor record. Inability to articulate a promise is missing discovery, not proof of purity. By this rule, neither refactor supplied by Gene qualifies for a skip.
   (Ratified 2026-09-07: Fable proposed the gate, Astra confirmed it as a classification gate — `safe-refactor` decides whether a promise gate fires; `linked-intent-testing` stays the sole authority on how a promise is registered and ratcheted. Never copy its five steps or registry schema here.)
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

### Timings on record (2026-09-07 pair-1, Curtain Call, same caller model, fresh worktrees)
| task | native median to green | tool median to green |
|---|---|---|
| 3 owners / 3 files (+ new ns, 3 requires, 3 deletions) | 124 s | 181 s |
| 21 sites / 6 owners / 1 file | 117 s | 198 s |
Verify was 85 s in every run; the difference is compose time. With owners and counts
known, native wins; use the tool for discovery-heavy fan-outs and for alias migration.
