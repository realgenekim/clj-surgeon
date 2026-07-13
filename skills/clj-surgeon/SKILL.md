---
name: clj-surgeon
description: Inspect and modify Clojure, ClojureScript, and CLJC structurally with the clj-surgeon CLI. Use for outlining large namespaces, mapping dependencies, finding or replacing nested forms, extracting forms, eliminating declares, moving forms, renaming namespaces, or performing deterministic CLJC operations. Prefer it when textual search or patching would be ambiguous, formatting-sensitive, or context-expensive.
---

# clj-surgeon

Use `clj-surgeon` from `PATH`. Its source is normally at
`~/src.local/clj-surgeon/`. Run `clj-surgeon --help` or
`clj-surgeon :op <operation> --help` for current CLI details.

## Orient efficiently

Run `:ls` before reading a large Clojure file:

```bash
clj-surgeon :op :ls :file src/writer/state.clj
```

Use the returned form names and line ranges to read only relevant source. For
cross-project discovery, use:

```bash
clj-surgeon :op :ls-tree :dir .
clj-surgeon :op :ls-tree :dir ~/src.local :grep 'postgres|jdbc'
```

Use `rg` for broad textual discovery. Use structural search when syntax identity
or repeated nested code makes textual matches ambiguous.

## Find nested syntax

Use `:find-subform` when the target is inside a large `defn`, Hiccup tree, route
table, schema, rule map, `let`, or state transformation:

```bash
clj-surgeon :op :find-subform \
  :file src/writer/views/book_workshop.clj \
  :inside book-workshop-pane \
  :match '(ds/post-action* "/api/book/new-node" _)'
```

The `_` symbol matches exactly one subtree. Whitespace does not affect
matching. Inspect `:match-count`, `:matches`, semantic `:path`, source, and hash.
Zero or many matches are valid discovery evidence.

## Replace one nested subtree

Always separate discovery, planning, review, and application:

```bash
clj-surgeon :op :replace-subform \
  :file src/writer/views/book_workshop.clj \
  :inside book-workshop-pane \
  :match '(ds/post-action* "/api/book/new-node" _)' \
  :with '(book-tree/creation-actions surface)' \
  :plan-out plan.edn

clj-surgeon :op :replace-subform! :plan plan.edn
```

Before applying, review `:plan-version`, `:operation`, `:file`, `:selector`,
`:edits`, unified `:diff`, source/result hashes, and `:provenance`.

Honor these invariants:

- Supply exactly one complete Clojure form in both `:match` and `:with`.
- Refine `:inside` or the pattern until replacement finds exactly one subtree.
- Match the complete boundary intended for replacement.
- Apply the saved plan; do not rerun the selector as an implicit mutation.
- Use one plan per edit and sequence plans for multiple edits.
- Stop on nonzero status or an EDN `:error`.
- Run the project's formatter, linter, and tests afterward.

Application is hash-bound, reparses the complete result, and requires atomic
filesystem replacement. It fails closed if the plan or source is stale.

For nested Clojure-to-JavaScript strings, single-quote the entire shell argument
and generate JavaScript literals with `pr-str`. JavaScript `\xNN` escapes are
not valid Clojure/EDN string escapes.

## Inspect dependencies and extraction boundaries

```bash
clj-surgeon :op :deps :file state.clj :form sync-draft!
clj-surgeon :op :ls-deps :file state.clj :form transition!
clj-surgeon :op :ls-extract :file state.clj :form rebuild-ai-paragraphs!
clj-surgeon :op :topo :file state.clj
clj-surgeon :op :declares :file state.clj
```

Use these outputs as evidence. Decide architecture, ownership, naming, and API
boundaries yourself; clj-surgeon performs bookkeeping rather than judgment.

## Perform top-level edits

Plan before executing paired write operations:

```bash
clj-surgeon :op :extract :file src/writer/state.clj \
  :forms '[distill refine helper]' :to src/writer/state/distillery.clj
clj-surgeon :op :extract! :file src/writer/state.clj \
  :forms '[distill refine helper]' :to src/writer/state/distillery.clj

clj-surgeon :op :fix-declares :file src/writer/state.clj
clj-surgeon :op :fix-declares! :file src/writer/state.clj

clj-surgeon :op :rename-ns :from old-prefix :to new-prefix :root .
clj-surgeon :op :rename-ns! :from old-prefix :to new-prefix :root .
```

Use `:mv` with `:dry-run true` before moving a form within a file. After
`:extract!`, compile and test; resolve bare references through qualification or
parameters rather than introducing circular dependencies.

## Work with CLJC

Use the deterministic CLJC operations instead of manually splicing reader
conditionals:

```bash
clj-surgeon :op :cljc-analyze :clj src/foo.clj :cljs src/foo.cljs
clj-surgeon :op :cljc-merge :clj src/foo.clj :cljs src/foo.cljs :out src/foo.cljc
clj-surgeon :op :cljc-split :file src/foo.cljc :clj-out src/foo.clj :cljs-out src/foo.cljs
clj-surgeon :op :cljc-add-require :file src/foo.cljc \
  :platform :cljs :ns goog.string :as gstr :out src/foo.cljc
```

Inspect with `:cljc-analyze` before deciding how to reconcile divergent forms or
requires. Let the tool preserve reader-conditionals and reject alias collisions.
