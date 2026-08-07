---
name: clj-surgeon
description: >-
  Invoke before using Read, Edit, grep, sed, or cat on existing Clojure, ClojureScript, or CLJC files.
  Use clj-surgeon for compact structural reads, analysis, nested edits, dependencies, moves, and renames.
---

# clj-surgeon

Before Read, Edit, grep, sed, or cat touches an existing Clojure file, use `clj-surgeon` from `PATH`.

## Minimize total turns

- For cross-file definitions, references, implementations, and call hierarchy, search the deferred MCP catalog for `mcp__cclsp__*` before falling back to source. For several known structural questions, prefer `inspect_clojure` when available. Use published tool schemas; do not guess arguments or reread evidence that already names its owner. Treat Surgeon as a lens, not a quota. On a bounded feature, stop after three source reads; choose one batch, X-ray, or native route instead of reconstructing a namespace form by form.
- Native Write is right for new files. Use native tools for JavaScript, tests, and prose/comments. Use a normal patch when the requested Clojure change cannot be stated as exact structural substitutions.
- Do not use cclsp rename or edit tools. Use clj-surgeon for structural writes, guarded transactions, and receipts. A known literal edit may be the first source-bearing call: one `:edit` with `:expect` applies and verifies it. Do not pre-read known relationships or use Surgeon after tests merely to prove parsing.
- Stop on nonzero exit or EDN `:error`. Call `clj-surgeon :op OP --help` only when the routes below do not cover the task.

## Smallest structural route

- Unknown top-level form: `clj-surgeon :op :ls :file FILE`.
- Known owner name(s), containing line, or distinctive text: call `:cat` with exactly one of `:form`, `:forms`, `:line`, or `:contains`.
  Use `:forms '[a b c]'` for one ordered, all-or-nothing snapshot capped at 65,536 source characters; do not run `:ls` solely as a preflight.
- Known owners across files: pipe one manifest noninteractively: `printf '%s\n' '{:reads [{:file "FILE" :forms [a b]}] :expect {:file-count 1 :form-count 2}}' | clj-surgeon :op :cat :spec-file - :format :semantic`. This compact behavior view omits comments/layout and may expand shorthand; omit `:format` for exact-source EDN. Never invoke `:spec-file -` and wait for later input.
- Unknown owner, known EDN form pattern: call `:match-form`. `:match` is not regex; use bounded `rg -l` for textual discovery, then one `:cat :contains` or `:cat :line`.
- Related syntax or computed facts: call `:xray`. Use `(line N)` for an unnamed top-level owner.
- Exact nested edit: call `:edit`. Use `transform` when the replacement depends on selected source.

Quote names containing shell syntax: `:form 'source->target'` or `:form 'ready?'`. `_` matches exactly one subtree; use `(loop _ _)` for a two-argument loop. There is no variadic wildcard.
`:cat` never dumps a file. Use it instead of reconstructing a `sed` range; reserve `rg` for broad discovery.

## Structural path primer

A path starts at `(form 'NAME)` or `(line N)`. `match` searches each current form, and `where` filters matches. Navigation skips whitespace and comments:

- `right`: next structural sibling.
- `left`: previous structural sibling.
- `up`: structural parent.
- `down`: first structural child.
- `(match :href) right` selects a map value. It does not mean "last."
- `span 2` selects one pair; `partition-all 2` groups the sibling suffix into pairs.
- `outermost` keeps selected nodes with no selected ancestor. Use `up` before it.
- `initializer` selects a `def` right-hand side without evaluating it.

A `case` clause, `cond` branch, map entry, or binding pair is sibling syntax, not a synthetic wrapper list. In EDN paths, use `:up :outermost`, not `:outermost :up`.
For all `cond` pairs, use `(match 'cond) up outermost down right (partition-all 2)`.

## X-ray ordinary Clojure data

Plain paths return exact source. `analyze` receives one vector of ordinary Clojure data and returns compact `:value` plus hashes.
End a literal path with `expect-count` to return exact source only at that cardinality; before `analyze`, it refuses before analysis. `initializer` selects a `def` right-hand side unevaluated.

```bash
clj-surgeon :op :xray :file src/policy.clj \
  :expr "(-> (form 'audit-report) initializer (expect-count 1) (analyze (fn [[report]] (frequencies (map :category (:events report))))))"
```

Write one total pure Clojure function instead of a shape-discovery query. When keys are uncertain, return a shape echo; scope counts to named keys and reserve `tree-seq` for unknown shapes.
Return concrete EDN, not a lazy sequence. X-ray is capability-limited, not termination-proof; analyzers must perform bounded work. X-ray never writes. For CLJC use `(form 'name :clj)` or `:cljs`.

## Compile one complete edit plan

When files, named owners, exact targets, replacements, and counts are known, use one guarded transaction. Do not split one known plan into repeated edit calls.
Send a nontrivial spec on stdin with `:spec-file -`, like `kubectl apply -f -`; do not embed it in a shell argument. Each named owner must resolve exactly once. Use `:each-form` or `:each-file` when a total match count alone would allow the wrong distribution. Exact `:find` does not support `_`, regex, or fuzzy matching.
```bash
clj-surgeon :op :change! :spec-file - :receipt-out /tmp/api-change.edn <<'EDN'
{:changes [{:id :body-class :in ["src/ui.clj"] :forms [shell reader] :find ":body" :do [:replace ":body.page"] :expect {:matches 2 :each-form 1}}] :expect {:changes 1 :edits 2 :files 1}}
EDN
```
A count mismatch, overlap, parse error, or stale hash refuses the complete transaction. Success returns compact read-back evidence and saves a hash-fenced inverse.
Use `:change` with the same spec for review. Do not open the saved receipt; run `clj-surgeon :op :undo-change! :receipt /tmp/api-change.edn` while every result hash matches. Scoped changes currently support literal `[:replace SOURCE]` only. Use the single-edit route below for relational or computed replacements. Legacy exact `:intents` remain accepted; never mix the two schemas.

## Guarded edit or plan and apply

Supply one of `:query` or `:expr`. `:edit` may be the first source-bearing command and never changes source without `:expect`.
Use `:expect BEFORE-FORM` only with a literal replacement. It applies and verifies only when the selection equals the declared source.
Omit `:plan-out` unless an audit artifact must be retained. Without `:expect`, `:plan-out` is required and the command is plan-only.

```bash
clj-surgeon :op :edit :file src/state.clj :expr "(-> (form 'transition) (match :finish) right (replace '(assoc state :status :complete)))" :expect '(assoc state :status :done)'
clj-surgeon :op :edit :file src/policy.clj :expr "(-> (form 'retry-policy) (match :delays) right (transform #(mapv (partial + 100) %)))" :plan-out plan.edn
clj-surgeon :op :replace-subform! :plan plan.edn
```

Whitespace does not affect `:expect`; comments, metadata, and reader syntax must match. On mismatch, narrow the selection—`(match :done) (replace :complete)`—or review a plan. Never use `:expect` with `transform`; generated after-state requires review. Plans store concrete replacement data, never executable code.
Literal replacements inline in `:expr` preserve `#()`, comments, commas, metadata, and layout. Computed replacements and `:query` use canonical printing. Read `:after` and `:diff` for exact source. Selector queries are semantic data.
`transform` receives quoted syntax, not runtime values. Do not preflight whether plan paths exist. Review hashes and do not reopen the plan file. Do not edit the plan; generate a new plan.
Never chain plan generation and application. Trust the apply receipt's `:verified` read-back hash and whole-file parse. Never reproduce a plan with `apply_patch`.

For dependencies, extraction, declares, moves, renames, or CLJC operations, read [references/advanced-operations.md](references/advanced-operations.md). Compatibility aliases include `:find-subform` and `:grep-form`.
