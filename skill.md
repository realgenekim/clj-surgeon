---
name: clj-surgeon
description: "Clojure structural ops: outline, nested find/replace, extract to new ns, fix-declares, deps tree, topo sort, form move, namespace rename, and CLJC operations — babashka + rewrite-clj"
---

# clj-surgeon: Structural Operations on Clojure Namespaces

A babashka CLI tool at `~/bin/clj-surgeon`. Source at `~/src.local/clj-surgeon/`.

## When to Use

- **Before exploring ANY Clojure codebase** — `:ls-tree` maps an entire directory of repos in seconds. "Which repo does X?" answered in one command instead of spawning Explore agents
- **Before reading a large .clj/.cljs/.cljc file** — `:ls` first (50 tokens vs 2000+); the outline now surfaces forms inside `#?(:clj …)` / `#?@(:cljs […])` with `:platforms` tags
- **When searching across multiple repos** — `:ls-tree :grep "pattern"` finds matching projects/files with full API surface, ~3 seconds across thousands of files
- **When extracting forms to a new namespace** — `:extract!` does it in one command
- **When you see a `declare`** — `:fix-declares!` eliminates removable ones
- **When reordering forms** — `:mv` moves a defn above its caller
- **When renaming a namespace** — `:rename-ns!` for structural AST rename
- **When understanding dependencies** — `:ls-deps` shows the full transitive tree
- **When a small target is buried inside a large form** — use `:find-subform`,
  then the versioned `:replace-subform` / `:replace-subform!` plan workflow
- **When converting a CLJ + CLJS pair to CLJC** — `:cljc-merge` does it deterministically (handles divergent aliases like `dom`/`dom-server`, npm requires, body-form collisions)
- **When splitting an unwieldy CLJC back into separate files** — `:cljc-split`
- **Before deciding what surgery to apply to CLJC** — `:cljc-analyze` returns a classification map (shared / one-sided / divergent requires + per-platform forms)
- **When adding a require to a CLJC file** — `:cljc-add-require` (refuses to introduce alias collisions; preserves npm string literals)

## All Operations

### :find-subform — Find nested syntax without editing

Use this inside a large `defn`, Hiccup tree, route table, schema, rules map,
`let`, or state transformation. Matching ignores formatting. The symbol `_`
matches exactly one arbitrary subtree.

```bash
clj-surgeon :op :find-subform \
  :file src/writer/views/book_workshop.clj \
  :inside book-workshop-pane \
  :match '(ds/post-action* "/api/book/new-node" _)'
```

It reports every match with source, location, semantic path, replay address,
and source hash. Zero or many matches are useful discovery results.

### :replace-subform / :replace-subform! — Plan and apply one nested edit

```bash
clj-surgeon :op :replace-subform \
  :file src/writer/views/book_workshop.clj \
  :inside book-workshop-pane \
  :match '(ds/post-action* "/api/book/new-node" _)' \
  :with '(book-tree/creation-actions surface)' \
  :plan-out plan.edn

clj-surgeon :op :replace-subform! :plan plan.edn
```

Planning refuses unless `:match` and `:with` each contain exactly one complete
form and the selector finds exactly one subtree. Review the unified `:diff` in
the saved, versioned plan. Applying replays the recorded address rather than
rerunning the selector; it validates source/result hashes and reparses the
complete future file before atomically replacing the target. Any error is EDN
with a nonzero exit status. If atomic replacement is unavailable, application
fails without weakening the write contract.

For nested Clojure-to-JavaScript strings, single-quote the complete shell
argument and use `pr-str` to generate JavaScript literals. JavaScript `\xNN`
escapes are not Clojure/EDN escapes.

### :extract / :extract! — Move forms to a new namespace (THE BIG ONE)

```bash
# Plan
clj-surgeon :op :extract :file src/writer/state.clj \
  :forms '[distill refine helper]' :to src/writer/state/distillery.clj

# Execute
clj-surgeon :op :extract! :file src/writer/state.clj \
  :forms '[distill refine helper]' :to src/writer/state/distillery.clj
```

One command:
1. Creates new namespace file with forms in topological order
2. Copies source `(ns ...)` form as template (over-includes requires — safe)
3. Removes extracted forms from source
4. Adds require for new namespace to source
5. Reports callers that may need updating

**After extraction, run `make runtests-once`.** The compiler catches bare references (`app-state`, `*io-enabled*`). Fix by qualifying (`state/app-state`) or passing as parameter (better architecture). Circular deps mean you need to redesign — pass the atom in, extract the pure core.

### :fix-declares / :fix-declares! — Eliminate unnecessary declares

```bash
clj-surgeon :op :fix-declares :file state.clj     # plan
clj-surgeon :op :fix-declares! :file state.clj    # execute
```

Finds removable declares, moves defns above callers (with leaf dep-pulling), deletes stale declares, skips unsafe moves with warnings.

**Proven:** 2768-line state.clj, 7 declares → 5 eliminated, 2 correctly skipped, 337 tests passing.

### :ls / :outline — Form boundaries + forward refs

```bash
clj-surgeon :op :ls :file src/writer/state.clj
```

Returns: `{:ns :lines :form-count :forms [{:type :name :line :end-line :args}] :forward-refs [...]}`

### :ls-tree / :tree / :map — Map an entire directory tree of repos

```bash
# Map a single project
clj-surgeon :op :ls-tree :dir .

# THE KILLER FEATURE: surgical cross-repo search
clj-surgeon :op :ls-tree :dir ~/src.local/ :grep "postgres|jdbc|next.jdbc"

# EDN output for machine consumption
clj-surgeon :op :ls-tree :dir . :format :edn
```

Discovers projects via `deps.edn`/`project.clj`/`bb.edn`, reads their `:paths`, outlines every `.clj/.cljs/.cljc` file. Returns ns names, requires, and all form signatures.

**With `:grep`:** Uses ripgrep to find matching files first (~0.3s), then only parses those. Turns "search 4,444 files" from 90 seconds into 3 seconds.

**When to reach for this:**
- "Which of my repos does X?" (cross-repo capability discovery)
- "What's the API surface of this project?" (onboarding)
- "What depends on library Y?" (impact analysis)
- Any time you'd spawn an Explore agent to scan multiple directories

**Performance:** Single repo: 0.25s. 10 repos: 1.3s. `:grep` across 4,444 files: 3.4s.

**Requires ripgrep (rg) for `:grep` fast path.** Falls back to system grep if not installed (much slower). Install: `brew install ripgrep`.

### :ls-deps — Transitive dependency tree

```bash
clj-surgeon :op :ls-deps :file state.clj :form transition!
```

Full dep chain as nested tree. Shows which deps are leaves, which have their own deps, which are circular.

### :ls-extract — Minimal extractable unit

```bash
clj-surgeon :op :ls-extract :file state.clj :form rebuild-ai-paragraphs!
```

Target form + all private helpers it exclusively depends on.

### :deps — Intra-namespace call graph

```bash
clj-surgeon :op :deps :file state.clj :form sync-draft!
```

### :topo — Topological sort

```bash
clj-surgeon :op :topo :file state.clj
```

### :declares — Audit declares (read-only)

```bash
clj-surgeon :op :declares :file state.clj
```

### :mv — Reorder a form within a file

```bash
clj-surgeon :op :mv :file state.clj :form foo :before bar :dry-run true
clj-surgeon :op :mv :file state.clj :form foo :before bar
clj-surgeon :op :mv-with-deps :file state.clj :form foo :before bar :dry-run true
clj-surgeon :op :mv-with-deps :file state.clj :form foo :before bar
```

Plain `:mv` fails closed if the candidate would strand dependencies or callers
and writes only when `:dry-run true` is absent. On `:ok true`, review
`:plan/:diff` and run the returned `:apply-command`. On
`:would-strand-dependencies`, run the safe `:recommended-command`, review
`:plan/:requested-forms`, `:added-forms`, `:move-order`, and `:diff`, show all
added forms to the user, and obtain explicit consent before running that
preview's `:apply-command`. Stop on `:would-strand-users` or any other refusal.
The alias is exactly `:mv :with-deps true`; it never moves callers or adds
declarations. Preview again after any source change, because move dry runs are
not saved hash-bound plans. After writing, rerun `:ls`, audit `:declares`, and
run the repository formatter, linter, compiler, and tests.

### :rename-ns / :rename-ns! — Rename namespace prefix

```bash
clj-surgeon :op :rename-ns :from old-prefix :to new-prefix :root .
clj-surgeon :op :rename-ns! :from old-prefix :to new-prefix :root .
```

### :cljc-merge — Combine CLJ + CLJS into a single CLJC

```bash
clj-surgeon :op :cljc-merge :clj src/foo.clj :cljs src/foo.cljs :out src/foo.cljc
```

Same alias bound to different namespaces (e.g. `dom` → `fulcro.dom-server` in CLJ, `fulcro.dom` in CLJS) collapses into a single `#?@(:clj […] :cljs […])` splice. NPM string requires route to `:cljs`. Body forms identical on both sides emit shared; differing bodies emit `#?(:clj … :cljs …)`. Throws on ns docstrings, attr-maps, `:import`, ns-name mismatches, body-count mismatches.

### :cljc-split — Split a CLJC into parallel CLJ + CLJS

```bash
clj-surgeon :op :cljc-split :file src/foo.cljc :clj-out src/foo.clj :cljs-out src/foo.cljs
```

### :cljc-add-require — Platform-aware require addition

```bash
clj-surgeon :op :cljc-add-require :file src/foo.cljc \
  :platform :cljs :ns goog.string :as gstr :out src/foo.cljc
```

`:platform` is `:clj`, `:cljs`, or `:cljc`. Throws on alias collision. NPM string requires use `:ns "react"` (string), not `:ns react` (symbol).

### :cljc-analyze — Structured classification

```bash
clj-surgeon :op :cljc-analyze :clj src/foo.clj :cljs src/foo.cljs
clj-surgeon :op :cljc-analyze :file src/foo.cljc
```

Returns a map with `{:requires {:shared … :clj-only … :cljs-only … :divergent …} :forms-clj […] :forms-cljs […]}`. Use this to plan a merge or surgical edit instead of reading both files.

## Workflows

### Change syntax nested inside a large form

1. Run `:find-subform`; inspect `:match-count`, paths, and source.
2. Match the complete subtree you intend to replace.
3. Run `:replace-subform` with `:plan-out plan.edn`.
4. Review `:selector`, `:edits`, `:diff`, hashes, and `:provenance`.
5. Apply only that artifact with `:replace-subform! :plan plan.edn`.
6. Run the project's formatter, linter, and tests.

If search is ambiguous, refine `:inside` or the pattern. Never choose an
occurrence by line number or weaken the exactly-one guard.

### Extract forms to a new namespace

```bash
# 1. See what's in the file
clj-surgeon :op :ls :file state.clj

# 2. Check the dependency tree of what you want to extract
clj-surgeon :op :ls-deps :file state.clj :form distillery-add-ai-response!

# 3. See the minimal extraction unit
clj-surgeon :op :ls-extract :file state.clj :form distillery-add-ai-response!

# 4. Extract (plan first, then execute)
clj-surgeon :op :extract :file state.clj \
  :forms '[form1 form2 form3]' :to src/writer/state/distillery.clj
clj-surgeon :op :extract! :file state.clj \
  :forms '[form1 form2 form3]' :to src/writer/state/distillery.clj

# 5. Compiler catches bare refs — fix them, then:
make runtests-once
```

### Eliminate unnecessary declares

```bash
clj-surgeon :op :fix-declares :file state.clj     # plan
clj-surgeon :op :fix-declares! :file state.clj    # execute
make runtests-once                                  # verify
```

### Orient before reading a large file

```bash
clj-surgeon :op :ls :file state.clj
# => 236 forms, 2768 lines — now Read only the lines you need
```

### Convert a CLJ + CLJS pair into one CLJC file

```bash
# 1. Inspect the divergence first — what's shared, what's platform-specific?
clj-surgeon :op :cljc-analyze :clj src/foo.clj :cljs src/foo.cljs

# 2. Merge deterministically
clj-surgeon :op :cljc-merge :clj src/foo.clj :cljs src/foo.cljs :out src/foo.cljc

# 3. Verify with a round trip
clj-surgeon :op :cljc-split :file src/foo.cljc

# 4. Delete the originals once tests pass
rm src/foo.clj src/foo.cljs
```

If the merge throws (e.g. ns docstring), the source has something the tool refuses to silently rewrite — fix by hand and retry.

## Important Notes

- **~5ms startup** — babashka, not JVM. Call it freely.
- **Returns EDN** — pipe through `bb -e '(let [d (read)] ...)'` to filter
- **Errors are EDN and nonzero** — treat any nonzero status as a failed operation
- **Use `:plan-out` for lens plans** — do not depend on shell redirection
- **Quote nested match/replacement forms** — single shell quotes preserve their syntax
- **One lens plan, one edit** — sequence reviewed plans for multiple changes
- **All analysis is pure** — side effects only in `!` variants
- **`:forms` arg takes EDN vector** — `:forms '[foo bar baz]'`
- **After `:extract!`, always run tests** — the compiler catches bare references instantly
- **Skips metadata** — handles `^:private`, `^:dynamic` correctly
- **Skips declares** — `:mv` and `:deps` target actual defns

## Proactive Usage

**Before reading any .clj/.cljs/.cljc file over 500 lines, run `:ls` first.**

**When the user asks to split a large file,** use `:ls-deps` to see the dependency tree, `:ls-extract` to find natural extraction units, then `:extract!` to execute.

**When you see or add a `(declare ...)`, run `:fix-declares!`.**

**Before manually reconciling a CLJ + CLJS pair**, run `:cljc-analyze` and consider `:cljc-merge`. The tool deterministically handles divergent aliases, npm requires, and body-form collisions that humans and LLMs both routinely get wrong.
