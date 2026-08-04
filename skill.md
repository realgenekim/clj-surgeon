---
name: clj-surgeon
description: Inspect and modify Clojure, ClojureScript, and CLJC structurally with the clj-surgeon CLI. Use for outlining large namespaces, selecting a complete form by name, line, or distinctive text, finding or replacing nested forms, mapping dependencies, extracting forms, eliminating declares, moving forms, renaming namespaces, or deterministic CLJC operations. Prefer it when textual reads or patches would be ambiguous, formatting-sensitive, or context-expensive.
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

## Find nested syntax

Use file-wide structural search when the enclosing form is unknown:

```bash
clj-surgeon :op :grep-form :file src/views.clj \
  :match '(post! "/api/items" _)'
```

`_` matches one subtree. Each named match reports an `:inside` value that can
be copied directly into a narrowed search or replacement. Add `:inside` only
when already known or when choosing among multiple matches.

When sibling text identifies the intended subtree—a `case` key, `cond` guard,
map key, or binding name—use `:cat :contains` on that text first. It returns the
owner and surrounding form in one read. Do not grep an expression that may
repeat elsewhere and then cat its owner merely to recover sibling context.

## Replace one exact subtree

Run discovery, planning, and application as separate commands. Never chain
plan generation and application.

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
reread related forms solely to verify byte preservation. Still run relevant
repository formatters, linters, compilers, and tests, and review the aggregate
workspace diff once when a repository is available.

A `case` clause, `cond` branch, map entry, or binding pair is adjacent sibling
syntax, not a synthetic wrapper list. Match an independently readable contained
expression until sibling-span operations exist.

## Advanced operations

Read
[the advanced operations reference](skills/clj-surgeon/references/advanced-operations.md)
before moving or extracting forms, fixing declares, renaming namespaces, or
performing CLJC operations.

For every operation, stop on nonzero status or EDN `:error`. Format changed
Clojure files before linting or testing.
