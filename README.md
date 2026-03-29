# clj-surgeon

Structural operations on Clojure namespaces. A babashka CLI that parses Clojure code as data (not text) using rewrite-clj, returning EDN.

**The headline feat:** clj-surgeon renamed itself from `ns-surgeon` to `clj-surgeon` — 10 files, 10 file moves, every ns declaration and `:require` entry updated in under 1 second. Not grep-and-replace. AST surgery: symbol nodes swapped in the parse tree, aliases preserved, string literals untouched, parens always balanced.

## Install

```bash
cd ~/src.local/ns-surgeon   # (yes, the repo is still named ns-surgeon)
make install                # → ~/bin/clj-surgeon
```

Requires [babashka](https://babashka.org/) (rewrite-clj and cheshire are built in).

## Operations

### `:outline` — See the skeleton of a namespace

```bash
clj-surgeon :op :outline :file src/writer/state.clj
```

Every top-level form with exact line boundaries, types, names, arglists, and forward reference detection. 236 forms in a 2768-line file, returned in ~200ms.

### `:declares` — Audit which `declare` statements are actually needed

```bash
clj-surgeon :op :declares :file src/writer/state.clj
# => {:summary {:total 7, :removable 6, :needed 1}}
```

Uses topological sort to distinguish band-aid declares (just reorder the file) from genuine mutual recursion (keep the declare). In `state.clj`: 6 of 7 declares are removable.

### `:mv` — Reorder a form within a file

```bash
clj-surgeon :op :mv :file state.clj \
  :form rebuild-ai-paragraphs! :before import-latest-fanout! :dry-run true
```

Moves a named form (including its preceding comment header) before another form. Always dry-run first.

### `:deps` — Intra-namespace call graph

```bash
clj-surgeon :op :deps :file state.clj :form sync-draft!
# => {:name "sync-draft!", :depends-on #{"log-event!" "app-state" "sync-draft-tx"}}
```

Which functions call which, within the same file. Walks the AST — not grep.

### `:topo` — Topological sort

```bash
clj-surgeon :op :topo :file state.clj
# => {:sorted [...225 forms...], :cycles ["distillery-add-ai-response!" ...4 forms...]}
```

The optimal ordering that eliminates all forward references. Forms in `:cycles` have genuine mutual recursion and need `declare`.

### `:closure` — Minimal extractable unit

```bash
clj-surgeon :op :closure :file state.clj :form rebuild-ai-paragraphs!
```

The target form + all private helpers it exclusively depends on. This is the smallest set you could extract to a new namespace.

### `:rename-ns` / `:rename-ns!` — Rename a namespace prefix

```bash
# Dry run — see the plan
clj-surgeon :op :rename-ns :from ns-surgeon :to clj-surgeon :root .

# Execute
clj-surgeon :op :rename-ns! :from ns-surgeon :to clj-surgeon :root .
```

Walks every `.clj` file's AST. Renames ns declarations and `:require` entries structurally (not text replace). Computes file moves. Flags non-Clojure files for manual review.

**This is how clj-surgeon renamed itself.** 10 files, 28 tests still green.

## Why?

Renaming a Clojure namespace used to be a 45-minute ordeal: grep, read, edit, grep again, move files, fix the ones you missed. Even with AI assistants, it's agonizing — they do the same grep-read-edit loop, burning context window on mechanical work.

clojure-lsp's `move-form` has been broken since 2021 ([issue #566](https://github.com/clojure-lsp/clojure-lsp/issues/566)). We sidestep it entirely.

**The insight:** Clojure code is data. A namespace form isn't text to grep — it's a list to walk. rewrite-clj gives us the AST with position tracking. Renames, dependency analysis, topological sort — they're all tree walks. 170 lines for `:rename-ns`. 50 lines for `:topo`. Because homoiconicity makes structural operations trivial.

See [docs/why-clj-surgeon.md](docs/why-clj-surgeon.md) for the full story.

## Testing

```bash
make test   # 28 tests, 116 assertions
```

All analysis functions are pure (string in, data out). Side effects are isolated to `:mv`, `:rename-ns!`, and `execute!`. Tests use temp files and temp directories — no fixture pollution.

## Architecture

```
src/clj_surgeon/
  core.clj          # CLI entry point, :op dispatch
  outline.clj       # rewrite-clj form boundary parser
  forward_refs.clj  # clj-kondo forward-ref detection
  move.clj          # form reordering within a file
  analyze.clj       # dependency graph, topo sort, closure, dead code
  rename.clj        # namespace prefix rename (AST surgery)
```

~500 lines of Clojure total. Zero dependencies beyond babashka.
