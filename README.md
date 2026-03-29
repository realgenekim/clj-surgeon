# clj-surgeon

Structural operations on Clojure namespaces. A babashka CLI that parses Clojure code as data (not text) using rewrite-clj, returning EDN.

**Built in one session. From zero to production use in 4 hours.** 10 ops, 42 tests, 157 assertions, ~700 lines of Clojure, zero dependencies beyond babashka.

## Headline Feats

**Self-surgery:** clj-surgeon renamed itself from `ns-surgeon` to `clj-surgeon` — 10 files, 10 file moves, every ns declaration and `:require` entry updated by walking the AST. Not grep-and-replace. Symbol node replacement in the parse tree. Aliases preserved, string literals untouched, parens always balanced. Under 1 second.

**One-command declare cleanup:** A 2768-line production namespace with 7 forward declares. One command eliminates the safe ones:

```
$ clj-surgeon :op :fix-declares! :file src/writer/state.clj

{:summary {:moves 3, :declares-deleted 3, :skipped 4}}

$ make runtests-once
337 tests, 1056 assertions, 0 failures.
```

3 declares eliminated, 3 defns moved above their callers, 4 unsafe moves correctly skipped (they have unresolved dependencies at the destination). All automatically. Previously this took 15+ minutes of manual grep-read-edit-test cycles, with the AI playing Whac-A-Mole as each move created new forward refs.

## Install

```bash
cd ~/src.local/clj-surgeon
make install    # → ~/bin/clj-surgeon
```

Requires [babashka](https://babashka.org/) (rewrite-clj and cheshire are built in).

## Operations

### `:ls` / `:outline` — See the skeleton of a namespace

```bash
clj-surgeon :op :ls :file src/writer/state.clj
```

Every top-level form with exact line boundaries, types, names, arglists, and forward reference detection. 236 forms in a 2768-line file, returned in ~200ms.

### `:fix-declares` / `:fix-declares!` — Eliminate unnecessary declares

```bash
# Plan (dry run)
clj-surgeon :op :fix-declares :file src/writer/state.clj

# Execute
clj-surgeon :op :fix-declares! :file src/writer/state.clj
```

The compound operation. In one command:
1. Finds removable declares (topological sort)
2. Checks each form's dependencies at the destination (prevents Whac-A-Mole)
3. Moves safe defns above their first callers (leaves first)
4. Deletes stale declare lines
5. Skips unsafe moves with warnings showing unresolved deps

### `:declares` — Audit which declares are needed

```bash
clj-surgeon :op :declares :file src/writer/state.clj
```

### `:mv` — Reorder a form within a file

```bash
clj-surgeon :op :mv :file state.clj \
  :form rebuild-ai-paragraphs! :before import-latest-fanout! :dry-run true
```

Moves a named form (including preceding comment header) before another form. Skips `declare` forms — always targets the actual `defn`. Always dry-run first.

### `:deps` — Intra-namespace call graph

```bash
clj-surgeon :op :deps :file state.clj :form sync-draft!
# => {:name "sync-draft!", :depends-on #{"log-event!" "app-state" "sync-draft-tx"}}
```

Which functions call which, within the same file. Walks the AST — not grep. Skips metadata (`^:private`, `^:dynamic`) correctly.

### `:topo` — Topological sort

```bash
clj-surgeon :op :topo :file state.clj
```

The optimal ordering that eliminates all forward references. Forms in `:cycles` have genuine mutual recursion and need `declare`.

### `:closure` — Minimal extractable unit

```bash
clj-surgeon :op :closure :file state.clj :form rebuild-ai-paragraphs!
```

The target form + all private helpers it exclusively depends on.

### `:rename-ns` / `:rename-ns!` — Rename a namespace prefix

```bash
clj-surgeon :op :rename-ns :from old-prefix :to new-prefix :root .
clj-surgeon :op :rename-ns! :from old-prefix :to new-prefix :root .
```

Walks every `.clj` file's AST. Renames ns declarations and `:require` entries structurally. Computes file moves. Flags non-Clojure files for manual review. **This is how clj-surgeon renamed itself.**

## Why This Works

Clojure code is data. A namespace form isn't text to grep — it's a list to walk. rewrite-clj gives us the AST with position tracking. Every operation is a tree walk:

| Operation | Lines of code | Equivalent in Java/TypeScript |
|-----------|:---:|---|
| Namespace rename | ~170 | 50,000+ (IntelliJ plugin) |
| Dependency graph | ~30 | Full language server |
| Topological sort | ~40 | (on top of dep graph) |
| Forward ref detection | 1 clj-kondo call | Custom parser + resolver |
| Form boundaries | Built into rewrite-clj | Tree-sitter + custom queries |
| Require inference | ~10 | Module resolution engine |

clojure-lsp's `move-form` has been broken since 2021 ([issue #566](https://github.com/clojure-lsp/clojure-lsp/issues/566)). We sidestep it entirely.

## The Journey: 5 Bugs in 4 Hours

Every bug was found through real-world use, fixed, and regression-tested:

1. **`:mv` matches declare, not defn** — `find-form` hit the first name occurrence. Fixed: skip `declare` forms.
2. **`z/next` infinite walk** — traversed entire file, not just one form. Fixed: scope zipper to form subtree.
3. **Topo sort reversed** — Kahn's algorithm on wrong edge direction. Fixed: process zero-dependency forms first.
4. **Metadata blindness** — `(def ^:private events-file ...)` returned `"^:private"` as the name. Fixed: detect `:meta` node tag, walk to rightmost child.
5. **Declares in dep graph** — `(declare foo)` returned empty deps, masking real `(defn foo)` deps. Fixed: exclude declares from analysis.

See [docs/observations/](docs/observations/) for the full ethnographic studies.

## Testing

```bash
make test   # 42 tests, 157 assertions
```

All analysis functions are pure (string in, data out). Side effects are isolated to `execute!` functions. Tests use temp files and temp directories — no fixture pollution.

## Architecture

```
src/clj_surgeon/
  core.clj           # CLI entry point, :op dispatch
  outline.clj        # rewrite-clj form boundary parser
  forward_refs.clj   # clj-kondo forward-ref detection
  move.clj           # form reordering within a file
  analyze.clj        # dependency graph, topo sort, closure, dead code
  rename.clj         # namespace prefix rename (AST surgery)
  fix_declares.clj   # compound op: eliminate removable declares
```

~700 lines of Clojure total. Zero dependencies beyond babashka.
