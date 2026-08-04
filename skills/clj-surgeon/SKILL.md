---
name: clj-surgeon
description: Inspect and modify Clojure, ClojureScript, and CLJC structurally with the clj-surgeon CLI. Use for outlining large namespaces, selecting a complete form, navigating from a case key, cond guard, map key, or binding to its peer value, finding or replacing nested forms, mapping dependencies, extracting forms, eliminating declares, moving forms, renaming namespaces, or deterministic CLJC operations. Prefer it when textual reads or patches would be ambiguous, formatting-sensitive, or context-expensive.
---

# clj-surgeon

Use `clj-surgeon` from `PATH`. Run `clj-surgeon --help` or
`clj-surgeon :op OP --help` only when the exact routes below do not cover the
task.

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

`:cat` is a strict alias for canonical `:show-form`; it never dumps the file.
Supply exactly one of `:form`, `:line`, or `:contains`. `:contains` is a
case-sensitive literal, not a regex. It returns one containing top-level form
or refuses on ambiguity. Add `:platform :clj` or `:platform :cljs` only to
disambiguate CLJC branches. The CLI preserves the `:contains` value as text, so
keyword-shaped literals such as `:finish` need no EDN-string workaround.

Use `rg` for broad cross-file discovery. Do not use `rg -n` merely to convert
distinctive text into a line for `:show-form`.

## Navigate and update syntax like data

Use `:q` when one piece of syntax identifies a related node. The query is an
EDN pipeline over the concrete syntax tree, not evaluated Clojure:

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

End that same path with `[:replace FORM]` to emit one guarded plan:

```bash
clj-surgeon :op :q :file src/state.clj \
  :query '[[:form transition] [:find :finish] :right [:replace (assoc state :status :complete)]]' \
  :plan-out plan.edn
clj-surgeon :op :replace-subform! :plan plan.edn
```

`:q` never writes source. A terminal replacement refuses unless the path
selects exactly one node, then returns that node, the trace, one diff, and the
source/result hashes. Review the plan before the separate apply command; never
chain them. When the requested relationship and replacement are already exact,
the updater can be the first non-mutating call. Run a read query first when the
choice still requires judgment.

## Find nested syntax

Use file-wide structural search when the enclosing form is unknown:

```bash
clj-surgeon :op :grep-form :file src/views.clj \
  :match '(post! "/api/items" _)'
```

`_` matches one subtree. Each named match reports an `:inside` value that can
be copied directly into a narrowed search or replacement. Add `:inside` only
when already known or when choosing among multiple matches:

```bash
clj-surgeon :op :grep-form :file src/views.clj :inside render \
  :match '(post! "/api/items" _)'
```

When a peer key, guard, or binding identifies the intended subtree, prefer one
`:q` pipeline over reading its owner and reconstructing a separate match. Do
not grep a repeated expression and then cat its owner merely to recover sibling
context.

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

Do not edit the plan; when intent changes, generate a new plan. Successful apply
returns `:applied-edit` and a `:verified` read-back receipt. Trust that receipt
for exact replay/hash/parse evidence; do not repeat it with `rg`, `show-form`,
`git diff`, or `shasum`. The reviewed plan diff is the edit-level change review;
the receipt proves that exact result was atomically written and reparsed. Do not
reread related forms solely to verify byte preservation. When a task asks only
to verify this exact edit, the reviewed plan plus successful receipt completes
that request. Still run relevant repository formatters, linters, compilers, and
tests. Review an aggregate Git diff only when the surrounding task already
establishes a Git worktree or explicitly requests that review; never probe
`.git` solely to decide whether to repeat the edit-level evidence.

A `case` clause, `cond` branch, map entry, or binding pair is adjacent sibling
syntax, not a synthetic wrapper list. Use `:q` peer navigation when the sibling
relationship identifies the target; use `:replace-subform` when one independent
subtree pattern already identifies it exactly.

## Advanced operations

Read [references/advanced-operations.md](references/advanced-operations.md)
before moving or extracting forms, fixing declares, renaming namespaces, or
performing CLJC operations. Those workflows have additional preview,
dependency, and verification requirements.

For every operation, stop on nonzero status or EDN `:error`. Format changed
Clojure files before linting or testing.
