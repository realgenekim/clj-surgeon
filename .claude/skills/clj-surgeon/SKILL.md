---
name: clj-surgeon
description: >-
  Inspect and modify Clojure, ClojureScript, and CLJC structurally with the
  clj-surgeon CLI. Use for compact reads, pure analysis, precise nested edits,
  sibling navigation, dependencies, extraction, moves, and renames.
---

# clj-surgeon

Use `clj-surgeon` from `PATH`. Stop on nonzero exit or EDN `:error`; call
`clj-surgeon :op OP --help` only when these routes do not cover the task.

## Smallest structural route

- Unknown top-level form: `clj-surgeon :op :ls :file FILE`.
- Known name, line, or distinctive text: call `:cat` with exactly one of
  `:form`, `:line`, or `:contains`; do not run `:ls` solely as a preflight.
- Unknown owner, known pattern: call `:grep-form`; add `:inside` only to narrow.
- Related nested syntax or computed facts: call `:xray`.
- Exact nested edit: call `:edit`, review its plan, then apply separately;
  derive a computed replacement with `transform` instead of retyping one.

```bash
clj-surgeon :op :cat :file src/app.clj :form transition
clj-surgeon :op :cat :file src/app.clj :contains :finish
clj-surgeon :op :grep-form :file src/app.clj :match '(post! "/api/items" _)'
```

`_` matches exactly one subtree; pattern arity is exact, so use `(loop _ _)`
for a two-argument loop. There is no variadic wildcard. `:cat` strictly aliases
`:show-form` and never dumps a file. Use it instead of reconstructing a `sed`
range. Distinctive text refuses zero or multiple forms. Reserve `rg` for broad
cross-file discovery, not manufacturing a line number.

## X-ray ordinary Clojure data

Plain paths return exact source. `analyze` receives one vector of ordinary Clojure data
and returns compact `:value` plus hashes. `expect-count` refuses
before analysis without changing that vector. `initializer` selects a `def`
right-hand side without evaluating it.

```bash
clj-surgeon :op :xray :file src/policy.clj \
  :expr "(-> (form 'audit-report) initializer (expect-count 1) (analyze (fn [[report]] (frequencies (map :category (:events report))))))"
```

Write one total pure Clojure function instead of a shape-discovery query. Map
literals and `hash-map` / `array-map` share a canonical map view. Identify
shape-independent descendants with `(filter predicate (tree-seq coll? seq value))`.
Return concrete EDN, not a lazy sequence. X-ray never writes source or a plan.
Compose `form`, `match`, `where`, `right`, `left`, `up`, `down`, `outermost`,
`initializer`, `span`, and `partition-all`. For CLJC use `(form 'name :clj)` or
`:cljs`. Use `:up :outermost`, not `:outermost :up`.

## Plan and apply separately

`:edit` never changes source. Supply `:plan-out` and exactly one of `:query` or
`:expr`; it may be the first source-bearing command when intent is exact.

```bash
clj-surgeon :op :edit :file src/state.clj \
  :expr "(-> (form 'transition) (match :finish) right (replace '(assoc state :status :complete)))" \
  :plan-out plan.edn
clj-surgeon :op :replace-subform! :plan plan.edn
```

The plan saves `transform`'s concrete replacement, never executable code:

```bash
clj-surgeon :op :edit :file src/policy.clj \
  :expr "(-> (form 'retry-policy) (match :delays) right (transform #(mapv (partial + 100) %)))" \
  :plan-out plan.edn
```

Do not preflight whether the plan path exists. Review diff and hashes after
plan generation. Never chain plan generation and application. Do not edit the
plan; when intent changes, generate a new plan. Apply returns `:verified`
read-back hash and whole-file parse evidence. Trust it; never reproduce the
plan with `apply_patch`.

A `case` clause, `cond` branch, map entry, or binding pair is sibling syntax,
not a synthetic wrapper list. Use `right` for one peer, `span 2` for one pair,
and `partition-all 2` for the run. Compatibility `:q` accepts
`[[:form transition] [:find :finish] :right]`, `[:span 2]`, and
`[:partition-all 2]`; `[:replace FORM]` or `[:replace-span FORM FORM]` plans
for later `:replace-subform!`. Prefer the Clojure `:xray` / `:edit` surface.

For dependencies, extraction, declares, moves, renames, or CLJC operations,
read [references/advanced-operations.md](references/advanced-operations.md).
