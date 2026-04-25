---
name: clj-surgeon
description: "Clojure structural ops: outline, extract to new ns, fix-declares, deps tree, topo sort, form move, namespace rename, CLJC merge/split/add-require/analyze — babashka + rewrite-clj"
user-invocable: true
---

# clj-surgeon: Structural Operations on Clojure Namespaces

A babashka CLI tool at `~/bin/clj-surgeon`. Source at `~/src.local/clj-surgeon/`. 103 tests, 327 assertions.

## When to Use

- **Before reading a large .clj/.cljs/.cljc file** — `:ls` first (50 tokens vs 2000+); the outline now surfaces forms inside `#?(:clj …)` / `#?@(:cljs […])` with `:platforms` tags
- **When extracting forms to a new namespace** — `:extract!` does it in one command
- **When you see a `declare`** — `:fix-declares!` eliminates removable ones
- **When reordering forms** — `:mv` moves a defn above its caller
- **When renaming a namespace** — `:rename-ns!` for structural AST rename
- **When understanding dependencies** — `:ls-deps` shows the full transitive tree
- **When converting a CLJ + CLJS pair to CLJC** — `:cljc-merge` does it deterministically (handles divergent aliases like `dom`/`dom-server`, npm requires, body-form collisions)
- **When splitting an unwieldy CLJC back into separate files** — `:cljc-split`
- **Before deciding what surgery to apply to CLJC** — `:cljc-analyze` returns a classification map (shared / one-sided / divergent requires + per-platform forms)
- **When adding a require to a CLJC file** — `:cljc-add-require` (refuses to introduce alias collisions; preserves npm string literals)

## All Operations

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
```

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
