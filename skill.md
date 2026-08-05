---
name: clj-surgeon
description: >-
  Inspect and modify Clojure, ClojureScript, and CLJC structurally with the
  clj-surgeon CLI. Use for compact reads, pure analysis, precise nested edits,
  sibling navigation, dependencies, extraction, moves, and renames.
---

# clj-surgeon

Use `clj-surgeon` from `PATH`. Stop on nonzero exit or EDN `:error`. Call
`clj-surgeon :op OP --help` only when these routes do not cover the task.

## Smallest structural route

- Unknown top-level form: `clj-surgeon :op :ls :file FILE`.
- Known owner name, containing line, or distinctive text: call `:cat` with
  exactly one selector. Do not run `:ls` solely as a preflight.
- Unknown owner, known pattern: call `:grep-form`. Add `:inside` only to narrow.
- Related syntax or computed facts: call `:xray`. Start with `(line N)` when a
  physical line identifies one otherwise unnamed top-level owner.
- Exact nested edit: call `:edit`, review its plan, then apply separately;
  derive a computed replacement with `transform` instead of retyping one.

```bash
clj-surgeon :op :cat :file src/app.clj :contains :finish
```

Quote names containing shell syntax: `:form 'source->target'` or `:form 'ready?'`.
`_` matches exactly one subtree. Pattern arity is exact, so use `(loop _ _)`
for a two-argument loop. There is no variadic wildcard. `:cat` never dumps a
file: use it instead of reconstructing a `sed` range. A distinctive text
selector refuses zero or multiple forms. Reserve
`rg` for broad cross-file discovery, not manufacturing a line number.

## X-ray ordinary Clojure data

Plain paths return exact source. `analyze` receives one vector of ordinary Clojure data and returns compact `:value` plus hashes.
`expect-count` refuses before analysis. `initializer` selects a `def` right-hand side unevaluated.

```bash
clj-surgeon :op :xray :file src/policy.clj \
  :expr "(-> (form 'audit-report) initializer (expect-count 1) (analyze (fn [[report]] (frequencies (map :category (:events report))))))"
```

`(form 'NAME)` selects semantic identity. `(line N)` selects the one top-level
form whose range or attached comment contains N. Gaps and overlapping owners
refuse. It selects the owner, so follow it with `match` to select a nested leaf.

Write one total pure Clojure function instead of a shape-discovery query. When keys are uncertain,
return a shape echo. Scope counts to named keys. Reserve `tree-seq` for unknown shapes.
Return concrete EDN, not a lazy sequence.
X-ray never writes. Compose `form`, `line`, `match`, `where`, navigation,
`initializer`, `span`, and `partition-all`. For CLJC use `(form 'name :clj)`
or `:cljs`. Use `:up :outermost`, not `:outermost :up`.

## Guarded edit or plan and apply

Supply `:plan-out` and one of `:query` or `:expr`. `:edit` may be the first
source-bearing command and never changes source without `:expect`. Use
`:expect BEFORE-FORM` only with a literal replacement. It saves and applies
only when the selection equals the declared source.

```bash
clj-surgeon :op :edit :file src/state.clj \
  :expr "(-> (form 'transition) (match :finish) right (replace '(assoc state :status :complete)))" \
  :expect '(assoc state :status :done)' :plan-out plan.edn
clj-surgeon :op :edit :file src/cache.clj \
  :expr "(-> (line 412) (match '(old-reader account-id)) (replace '(new-reader account-id)))" \
  :expect '(old-reader account-id)' :plan-out plan.edn
clj-surgeon :op :edit :file src/policy.clj :expr "(-> (form 'retry-policy) (match :delays) right (transform #(mapv (partial + 100) %)))" :plan-out plan.edn
clj-surgeon :op :replace-subform! :plan plan.edn
```

Whitespace does not affect `:expect`. Comments, metadata, and reader syntax
must match. On mismatch, narrow the selection—`(match :done) (replace
:complete)` preserves its surroundings—or review a plan. Never use `:expect`
with `transform`: its generated after-state requires review. Plans store concrete replacement data,
never executable code.
Literal replacements inline in `:expr` preserve `#()`, comments, commas, metadata, and layout. Computed replacements and `:query` use canonical printing. Read `:after` and `:diff` for exact source. Selector queries are semantic data.
`transform` receives quoted syntax, not runtime values. Do not preflight whether
plan paths exist. Review hashes and do not reopen the plan file. Do not edit the plan. Instead, generate a new plan.
Never chain plan generation and application. Trust the apply receipt's `:verified` read-back
hash and whole-file parse. Never reproduce a plan with `apply_patch`.

A `case` clause, `cond` branch, map entry, or binding pair is sibling syntax, not a
synthetic wrapper list: `right` selects one peer, `span 2` one pair, and
`partition-all 2` the run. Prefer the Clojure `:xray` / `:edit` surface.

For dependencies, extraction, declares, moves, renames, or CLJC operations, read [the advanced operations reference](skills/clj-surgeon/references/advanced-operations.md).
