---
name: clj-surgeon
description: Inspect and modify Clojure, ClojureScript, and CLJC structurally with the clj-surgeon CLI. Use for outlining large namespaces, mapping dependencies, finding or replacing nested forms, extracting forms, eliminating declares, moving forms, renaming namespaces, or performing deterministic CLJC operations. Prefer it when textual search or patching would be ambiguous, formatting-sensitive, or context-expensive.
---

# clj-surgeon

Use `clj-surgeon` from `PATH`. Its source is normally at
`~/src.local/clj-surgeon/`. Run `clj-surgeon --help` or
`clj-surgeon :op <operation> --help` for current CLI details.

## Orient efficiently

When you do not yet know the relevant form name or containing line, start a
large Clojure file with `:ls`:

```bash
clj-surgeon :op :ls :file src/writer/state.clj
```

Use the returned form names and line ranges to read only relevant source. For
cross-project discovery, use:

```bash
clj-surgeon :op :ls-tree :dir .
clj-surgeon :op :ls-tree :dir ~/src.local :grep 'postgres|jdbc'
```

Use `rg` for broad textual discovery. When you know distinctive text but not
the containing form, use `rg -n` to get one line number, then call
`:show-form :line`; do not print a large `:ls` outline just to discover that
line. Use structural search when syntax identity or repeated nested code makes
textual matches ambiguous.

When you already know a top-level form name or a line inside it, make
`:show-form` the first source inspection. Do not run `:ls` solely as a
preflight. Use `:show-form` instead of reconstructing a `sed` range or using
another text reader:

```bash
clj-surgeon :op :show-form :file src/writer/state.clj :form transition!
clj-surgeon :op :show-form :file src/writer/state.clj :line 1134
clj-surgeon :op :cat :file src/writer/state.clj :form transition!
```

Supply exactly one of `:form` or `:line`. For an ambiguous CLJC definition,
add `:platform :clj` or `:platform :cljs`. Inspect the exact `:source`, form
location, platforms, and complete-file `:source-hash`. Stop on ambiguity; the
command never selects the first match. Use a bounded text read only when the
needed context genuinely spans forms or is not structurally addressable.
`:cat` is a strict alias for `:show-form`; it never dumps the complete file.

## Find nested syntax

Use `:grep-form` for file-wide structural search. It is a strict alias for
`:find-subform`, not a text regular expression:

```bash
clj-surgeon :op :grep-form \
  :file src/writer/views/book_workshop.clj \
  :match '(ds/post-action* "/api/book/new-node" _)'
```

Add `:inside` only when you already know the containing top-level form or need
to narrow multiple matches:

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
- A `case` clause, `cond` branch, map entry, or binding pair is adjacent sibling
  syntax, not a synthetic wrapper list. Until sibling-span operations exist,
  match an independently readable contained value or expression.
- Run plan generation as a standalone shell command. Observe and review its
  result before a separate apply command; never chain planning and application.
- Apply the saved plan; do not rerun the selector as an implicit mutation.
- Do not edit the plan with `apply_patch` or another text tool. If the intended
  edit changes, generate a new plan.
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

### Move forms safely

Always begin with the narrow, non-mutating operation:

```bash
clj-surgeon :op :ls :file src/my/ns.clj
clj-surgeon :op :mv :file src/my/ns.clj \
  :form foo :before bar :dry-run true
```

Branch on the exit status and EDN result:

- On `:ok true`, inspect `:plan/:diff`, then run the returned
  `:apply-command`. It moves only the requested form.
- On `:error-type :would-strand-dependencies`, run the returned
  `:recommended-command`. It is a non-mutating `:mv-with-deps` preview.
  Review `:plan/:requested-forms`, `:added-forms`, `:move-order`, and `:diff`.
  Show every added form to the user and obtain explicit consent before running
  that preview's `:apply-command`.
- On `:would-strand-users`, a cycle, ambiguity, unsupported layout, or any
  other refusal, stop. Choose another destination or refactor; do not force the
  dependency alias.

`:mv-with-deps` is exactly `:mv :with-deps true` and always forces that option.
It never moves callers or adds declarations. Both previews leave the file
unchanged. A move dry run is not a saved, hash-bound plan, so preview again
after any source change. After writing, rerun `:ls`, audit declarations with
`:declares`, and run the repository formatter, linter, compiler, and tests.

After `:extract!`, compile and test; resolve bare references through
qualification or parameters rather than introducing circular dependencies.

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
