---
name: clj-surgeon
description: >-
  Inspect and modify Clojure, ClojureScript, and CLJC structurally with the clj-surgeon CLI.
  Use for outlines, form reads, peer navigation, nested edits, dependencies, extraction,
  moves, renames, and CLJC operations when text is ambiguous or context-expensive.
---

# clj-surgeon

Use `clj-surgeon` from `PATH`. Run help only when the exact routes below do not
cover the task: `clj-surgeon --help` or `clj-surgeon :op OP --help`.

## Read the smallest structural object

Use `:ls` only when the relevant form is unknown:

```bash
clj-surgeon :op :ls :file src/my/ns.clj
```

When a name, containing line, or distinctive literal is known, skip `:ls` and
read the complete form directly:

```bash
clj-surgeon :op :cat :file src/my/ns.clj :form transition!
clj-surgeon :op :cat :file src/my/ns.clj :line 1134
clj-surgeon :op :cat :file src/my/ns.clj :contains 'Per-command help'
clj-surgeon :op :cat :file src/my/ns.clj :contains :finish
```

Use `:show-form` instead of reconstructing a `sed` range. Do not run `:ls`
solely as a preflight when any direct selector is known.

`:cat` is a strict alias for canonical `:show-form`. It never dumps the file.
Supply exactly one of `:form`, `:line`, or `:contains`. `:contains` is a
case-sensitive literal, not a regex. It returns one containing top-level form
or refuses on ambiguity.

Add `:platform :clj` or `:platform :cljs` only to
disambiguate CLJC branches. The CLI preserves the `:contains` value as text, so
keyword-shaped literals such as `:finish` need no EDN-string workaround.

Use `rg` for broad cross-file discovery. Do not use `rg -n` merely to convert
distinctive text into a line for `:show-form`.

## Read and edit syntax like data

Use `:q` to read when one piece of syntax identifies a related node. The query
is an EDN pipeline over the concrete syntax tree, not evaluated Clojure:

```bash
clj-surgeon :op :q :file src/state.clj \
  :query '[[:form transition] [:find :finish] :right]'
```

This reads the value paired with `:finish`. The same `:right` step moves from a
`cond` guard to its result, a map key to its value, or a binding name to its
initializer. Semantic navigation skips whitespace and comments but the tool
preserves them in the file. Compose `[:form NAME]`, `[:find PATTERN]`,
`[:where {:tag TAG}]`, `[:where {:parent-tag TAG}]`, and
`:right`/`:left`/`:up`/`:down`. `_` matches one subtree inside a `:find`
pattern. A read reports zero, one, or many matches and a per-step count trace.

Use `:xray` for counts, sums, frequencies, grouping, or other pure computation over selected source. Do not reconstruct those answers manually from `:q`:

```bash
clj-surgeon :op :xray :file src/policy.clj :expr "(-> (form 'audit-report) (match :events) right (xray-one #(frequencies (map :category %))))"
```

`xray-one` receives one intended value and refuses zero or many; generic `xray`
receives a vector. The `:value` has compact hash evidence; use `:evidence :full`
for source. It never writes. Use `:q` without computation. In CLJC, select a
branch with `(form 'name :clj)` or `[:form name :cljs]`.

When the path and replacement are already exact, use `:edit` with that same
path ending in `[:replace FORM]`. The plan can be the first source-bearing
command. Do not pre-read only to reconstruct the supplied relationship.

```bash
clj-surgeon :op :edit :file src/state.clj \
  :query '[[:form transition] [:find :finish] :right [:replace (assoc state :status :complete)]]' \
  :plan-out plan.edn
clj-surgeon :op :replace-subform! :plan plan.edn
```

`:edit` also accepts a sandboxed pure Clojure expression instead of the EDN
query. Use `:expr` when collection operations, local bindings, or higher-order
functions make the path or replacement clearer:

```bash
clj-surgeon :op :edit :file src/state.clj \
  :expr "(-> (form 'transition) (match :finish) right (replace '(assoc state :status :complete)))" \
  :plan-out plan.edn
clj-surgeon :op :replace-subform! :plan plan.edn
```

When the replacement depends on the selected form, use `transform` and avoid a
separate read:

```bash
clj-surgeon :op :edit :file src/policy.clj \
  :expr "(-> (form 'retry-policy) (match :delays) right (transform #(mapv (partial + 100) %)))" \
  :plan-out plan.edn
clj-surgeon :op :replace-subform! :plan plan.edn
```

`transform` receives the exactly-one selected form as Clojure data and does not
run for zero or many matches. The plan stores only concrete replacement data,
never the function. Use `transform` only when the current form determines it.

Supply exactly one of `:query` and `:expr`. Literal paths are often shorter as
`:query`. `:expr` provides pure `clojure.core` collections and structural
builders. SCI excludes I/O, processes, namespaces, mutable references, and host
interop. Refusals include allowed symbols, signatures, and a remedy.

`:edit` requires `:file`, `:plan-out`, and one authoring surface. It never
changes source. Review its edit, trace, diff, and hashes before apply. Never
chain plan generation and application. Use `:q` first only for judgment.

Do not preflight whether the task-specific `:plan-out` path exists. Successful
planning atomically replaces that artifact. Any refusal preserves it.

When the returned diff is exact, apply that saved plan next with
`:replace-subform!`. Never reproduce the plan with `apply_patch`, a text edit,
or a second equivalent plan. The verified executor is the source-changing
step.

Select a meaningful peer pair as one lossless slice with `[:span 2]`:

```bash
clj-surgeon :op :edit :file src/state.clj \
  :query '[[:form transition] [:find :finish] [:span 2] [:replace-span :finish (assoc state :status :complete)]]' \
  :plan-out plan.edn
clj-surgeon :op :replace-subform! :plan plan.edn
```

`[:span N]` includes the current node and its next `N-1` semantic siblings and
never crosses their parent. `[:replace-span FORM ...]` requires the same number
of forms, so comments and whitespace between peers remain byte-for-byte intact.
Use a span when the pair or flattened `#(...)` body is itself the object under
inspection. Use `:right` when only the peer value is the target.

## Enumerate sibling pairs in one read

When the first sibling is known, enumerate every pair with `[:partition-all 2]`:

```bash
clj-surgeon :op :q :file src/state.clj :query '[[:form transition] [:find case] :up :down :right :right [:partition-all 2]]'
```

When repeated nested heads make the first outer sibling unknown, promote heads
to owners before retaining maximal owners:

```bash
clj-surgeon :op :q :file src/policy.clj :query '[[:form classify-request] [:find cond] :up :outermost :down :right [:partition-all 2]]'
```

Use `:up :outermost`, not `:outermost :up`. Head symbols do not contain one
another. When the first outer guard is known, anchor there because it is
shorter. Results contain exact source, addresses, gaps, and partition evidence.
Use `:right` for one known value. Use `[:span 2]` for one known pair. Use
`[:partition-all 2]` for all pairs in a sibling suffix. Multiple partitions are
read evidence and refuse mutation. Use an exact anchor and `[:span 2]` for a
singular pair edit.

## Find nested syntax

Use file-wide structural search when the enclosing form is unknown:

```bash
clj-surgeon :op :grep-form :file src/views.clj \
  :match '(post! "/api/items" _)'
```

`_` matches one subtree. Copy each named match's `:inside` value directly into
a narrowed search or replacement. Add `:inside` only
when already known or when choosing among multiple matches:

```bash
clj-surgeon :op :grep-form :file src/views.clj :inside render \
  :match '(post! "/api/items" _)'
```

When a peer key, guard, or binding identifies the intended subtree, prefer one
`:q` read or `:edit` plan. Do not read its owner and reconstruct a separate
match. Do not grep a repeated expression and then cat its owner only to recover
sibling context.

## Replace one exact subtree

Keep planning and application as separate commands. Never chain them. Use a
read command first only when selecting the intended replacement requires a
separate judgment.

```bash
clj-surgeon :op :replace-subform :file src/views.clj :inside render \
  :match '(post! "/api/items" _)' \
  :with '(items/actions surface)' :plan-out plan.edn
```

Review the selector, single edit, diff, source/result hashes, and provenance.
Then apply the unchanged saved plan in a later command:

```bash
clj-surgeon :op :replace-subform! :plan plan.edn
```

Do not edit the plan. When intent changes, generate a new plan. Successful apply
returns `:applied-edit` and a `:verified` read-back receipt.

Trust that receipt for exact replay/hash/parse evidence. Do not repeat it with
`rg`, `show-form`, `git diff`, or `shasum`.

The reviewed plan diff is the edit-level change review. The receipt proves that
clj-surgeon atomically wrote and reparsed that exact result. Do not reread
related forms solely to verify byte preservation. When a task asks only to
verify this exact edit, the reviewed plan plus successful receipt completes that
request.

Still run relevant repository formatters, linters, compilers, and tests. Review
an aggregate Git diff only when the surrounding task already establishes a Git
worktree or explicitly requests that review. Never probe
`.git` solely to decide whether to repeat the edit-level evidence.

A `case` clause, `cond` branch, map entry, or binding pair is adjacent sibling
syntax, not a synthetic wrapper list. Use `:edit` peer navigation when the
sibling relationship identifies the edit target. Use `:replace-subform` when
one independent subtree pattern already identifies it exactly.

## Advanced operations

Read [references/advanced-operations.md](references/advanced-operations.md)
before moving or extracting forms, fixing declares, renaming namespaces, or
performing CLJC operations. Those workflows have additional preview,
dependency, and verification requirements.

For every operation, stop on nonzero status or EDN `:error`. Format changed Clojure files before linting or testing.
