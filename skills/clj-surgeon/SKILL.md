---
name: clj-surgeon
description: >-
  Inspect and modify Clojure, ClojureScript, and CLJC structurally with the
  clj-surgeon CLI. Use for compact form reads, pure analysis, precise nested
  edits, sibling navigation, dependencies, extraction, moves, and renames.
---

# clj-surgeon

Use `clj-surgeon` from `PATH`. Stop on a nonzero exit or EDN `:error`. Run
`clj-surgeon :op OP --help` only when these routes do not cover the task.

## Choose the smallest structural route

- Unknown top-level form: `clj-surgeon :op :ls :file FILE`.
- Known name, line, or distinctive text: call `:cat` directly with exactly one
  of `:form`, `:line`, or `:contains`.
- Unknown enclosing form but known structural pattern: call `:grep-form`.
- Related nested syntax or computed facts: call `:xray`.
- Exact nested edit: call `:edit`, review its plan, then apply separately.

```bash
clj-surgeon :op :cat :file src/app.clj :form transition
clj-surgeon :op :cat :file src/app.clj :line 1134
clj-surgeon :op :cat :file src/app.clj :contains :finish
clj-surgeon :op :grep-form :file src/app.clj :match '(post! "/api/items" _)'
```

`:cat` is the strict alias for canonical `:show-form`; it never dumps a file.
Use `:show-form` instead of reconstructing a `sed` range. Do not run `:ls`
solely as a preflight. A distinctive text selector is a case-sensitive literal
and refuses zero or multiple containing forms. Use `rg` only for broad
cross-file discovery, not to manufacture a line number.

## X-ray syntax as ordinary Clojure data

Use one pure Clojure expression. Plain paths return exact source. `analyze`
always receives one vector of ordinary Clojure data and returns compact
`:value` plus hashes. `expect-count` refuses before analysis without changing
the vector type. `initializer` selects a `def` right-hand side without
evaluating it.

```bash
clj-surgeon :op :xray :file src/policy.clj \
  :expr "(-> (form 'audit-report) initializer (expect-count 1) (analyze (fn [[report]] (frequencies (map :category (:events report))))))"
```

Write one total function over that contract instead of a separate
shape-discovery query. Map literals and `hash-map` / `array-map` syntax share a
canonical map view. Identify shape-independent descendants with
`(filter predicate (tree-seq coll? seq value))`. Return concrete EDN, not a
lazy sequence. X-ray never writes source or a plan.

Compose `form`, `match`, `where`, `right`, `left`, `up`, `down`, `outermost`,
`initializer`, `span`, and `partition-all`. In CLJC, use `(form 'name :clj)` or
`:cljs`. Use `:up :outermost`, not `:outermost :up`, to remove contained owners.

For alternating sibling runs, use `partition-all` instead of repeated reads:

```bash
clj-surgeon :op :xray :file src/state.clj \
  :expr "(-> (form 'transition) (match 'case) up down right right (partition-all 2))"
```

## Plan and apply exact edits separately

When intent is exact, `:edit` may be the first source-bearing command. It never
changes source. Supply `:plan-out` and exactly one of `:query` or `:expr`.

```bash
clj-surgeon :op :edit :file src/state.clj \
  :expr "(-> (form 'transition) (match :finish) right (replace '(assoc state :status :complete)))" \
  :plan-out plan.edn
clj-surgeon :op :replace-subform! :plan plan.edn
```

When replacement depends on the selected form, use pure `transform`; the plan
saves only its concrete replacement, never executable code:

```bash
clj-surgeon :op :edit :file src/policy.clj \
  :expr "(-> (form 'retry-policy) (match :delays) right (transform #(mapv (partial + 100) %)))" \
  :plan-out plan.edn
```

Do not preflight whether the plan path exists. Review the diff and hashes after
plan generation. Never chain plan generation and application. Do not edit the
plan; when intent changes, generate a new plan. A successful apply returns
`:verified` read-back hash and whole-file parse evidence. Trust that receipt;
do not reproduce the plan with `apply_patch`.

A `case` clause, `cond` branch, map entry, or binding pair is adjacent sibling
syntax, not a synthetic wrapper list. Use `right` for one peer, `span 2` for
one pair, and `partition-all 2` for the full run.

The compatibility `:q` spelling remains available for literal paths such as
`[[:form transition] [:find :finish] :right]`, spans such as `[:span 2]`, and
partitions such as `[:partition-all 2]`. Terminal `[:replace FORM]` and
`[:replace-span FORM FORM]` generate the same plan for later
`:replace-subform!`; prefer the Clojure `:xray` / `:edit` surface.

## Advanced operations

Before dependencies, extraction, declare repair, moves, namespace renames, or
CLJC operations, read [references/advanced-operations.md](references/advanced-operations.md).
Those workflows add preview, dependency, formatting, and verification gates.
