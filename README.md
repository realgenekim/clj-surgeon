# clj-surgeon

Structural operations on Clojure namespaces. A babashka CLI that parses Clojure code as data (not text) using [rewrite-clj](https://github.com/clj-commons/rewrite-clj), returning EDN.

**Built in one session. From zero to production use in 4 hours.** 11 ops, 49 tests, 181 assertions, ~800 lines of Clojure, zero dependencies beyond babashka.

**Designed for AI-assisted development.** clj-surgeon is a [Claude Code](https://claude.ai/claude-code) skill — Claude knows how to use every operation, when to call each one, and how to compose them. The tool provides structural visibility (the X-ray); the AI provides judgment (what to do about it). See [How Claude Code Uses This](#how-claude-code-uses-this).

## Headline Feats

**Self-surgery:** clj-surgeon renamed itself from `ns-surgeon` to `clj-surgeon` — 10 files, 10 file moves, every ns declaration and `:require` entry updated by walking the AST. Not grep-and-replace. Symbol node replacement in the parse tree. Aliases preserved, string literals untouched, parens always balanced. Under 1 second.

**One-command declare cleanup:** A 2768-line production namespace with 7 forward declares. One command:

```
$ clj-surgeon :op :fix-declares! :file src/writer/state.clj

{:summary {:moves 7, :declares-deleted 5, :skipped 2}}

$ make runtests-once
337 tests, 1056 assertions, 0 failures.
```

5 declares eliminated (3 direct moves + 2 with leaf deps pulled along), 2 unsafe moves correctly skipped (they have non-leaf dependency chains). Previously this took 15+ minutes of manual grep-read-edit-test cycles, with the AI playing Whac-A-Mole as each move created new forward refs.

## Install

```bash
cd ~/src.local/clj-surgeon
make install    # → ~/bin/clj-surgeon
```

Requires [babashka](https://babashka.org/) (rewrite-clj and cheshire are built in).

## Operations

### Visibility (the X-ray)

#### `:ls` / `:outline` — See the skeleton of a namespace

```bash
clj-surgeon :op :ls :file src/writer/state.clj
```

Every top-level form with exact line boundaries, types, names, arglists, and forward reference detection. 236 forms in a 2768-line file, returned in ~200ms.

#### `:ls-deps` — Transitive dependency tree

```bash
clj-surgeon :op :ls-deps :file state.clj :form transition!
```

The full dependency chain as a nested tree — shows which deps are leaves, which have their own deps, and which are circular. This is the X-ray that tells you whether a refactoring is safe.

```
transition! (line 2655)
├── log-event! (line 2600)
│   └── events-file ◆ leaf
├── log-wal! (line 2627)
│   └── wal-file ◆ leaf
├── transition-tx (line 2642)
│   └── settle-editing-state ◆ leaf
└── wal-snapshot ◆ leaf
```

#### `:ls-extract` — Minimal extractable unit

```bash
clj-surgeon :op :ls-extract :file state.clj :form rebuild-ai-paragraphs!
```

The target form + all private helpers it exclusively depends on. This is the smallest set you could extract to a new namespace without breaking anything.

#### `:declares` — Audit which declares are needed

```bash
clj-surgeon :op :declares :file src/writer/state.clj
```

#### `:deps` — Intra-namespace call graph

```bash
clj-surgeon :op :deps :file state.clj :form sync-draft!
```

#### `:topo` — Topological sort (optimal form ordering)

```bash
clj-surgeon :op :topo :file state.clj
```

### Actions (the scalpel)

#### `:fix-declares` / `:fix-declares!` — Eliminate unnecessary declares

```bash
clj-surgeon :op :fix-declares :file state.clj     # plan (dry run)
clj-surgeon :op :fix-declares! :file state.clj    # execute
```

The compound operation:
1. Finds removable declares via topological sort
2. Checks each form's dependencies at the destination
3. Pulls leaf deps along with their dependents (no Whac-A-Mole)
4. Moves safe defns above their first callers
5. Deletes stale declare lines
6. Skips truly unsafe moves (non-leaf dep chains) with warnings

#### `:mv` — Reorder a form within a file

```bash
clj-surgeon :op :mv :file state.clj :form foo :before bar :dry-run true
clj-surgeon :op :mv :file state.clj :form foo :before bar
```

Moves a named form (including preceding comment header) before another form. Skips `declare` forms — always targets the actual `defn`. Always dry-run first.

#### `:rename-ns` / `:rename-ns!` — Rename a namespace prefix

```bash
clj-surgeon :op :rename-ns :from old-prefix :to new-prefix :root .   # plan
clj-surgeon :op :rename-ns! :from old-prefix :to new-prefix :root .  # execute
```

Walks every `.clj` file's AST. Renames ns declarations and `:require` entries structurally (not text replace). Computes file moves. Flags non-Clojure files for manual review. **This is how clj-surgeon renamed itself.**

## How Claude Code Uses This

clj-surgeon ships as a Claude Code [skill](https://docs.anthropic.com/en/docs/claude-code/skills) that teaches the AI when and how to use each operation. In practice:

**Before reading a large file,** Claude runs `:ls` to get form boundaries in ~50 tokens instead of reading 2000+ lines blind. It then `Read`s only the specific line ranges it needs.

**When it sees a `declare`,** Claude runs `:fix-declares!` instead of manually moving forms one by one. The tool handles dependency checking, leaf-pulling, and declare deletion — the AI just runs one command and verifies with tests.

**When planning an extraction,** Claude runs `:ls-deps` to see the full dependency tree, then `:ls-extract` to find the minimal extraction unit. It uses this information to decide whether to extract to a new namespace or just reorder within the file.

**When renaming,** Claude runs `:rename-ns` to see the plan (every file that would change, every require that would update), then `:rename-ns!` to execute. No grep-and-replace, no missed references.

The design philosophy: **give the AI better visibility, not cleverer automation.** The tool provides the X-ray; the AI provides the judgment. Simple tools that compose beat complex tools that guess.

## Why This Works

Clojure code is data. A namespace form isn't text to grep — it's a list to walk. [rewrite-clj](https://github.com/clj-commons/rewrite-clj) gives us the AST with position tracking. Every operation is a tree walk:

| Operation | Lines of Clojure | In a non-homoiconic language |
|-----------|:---:|---|
| Namespace rename | ~170 | 50,000+ (IntelliJ plugin) |
| Dependency graph | ~30 | Full language server |
| Topological sort | ~40 | (on top of dep graph) |
| Dep tree visualization | ~30 | (on top of dep graph) |
| Forward ref detection | 1 clj-kondo call | Custom parser + resolver |
| Form boundaries | Built into rewrite-clj | Tree-sitter + custom queries |
| Require inference | ~10 | Module resolution engine |
| Leaf dep-pulling | ~30 | (on top of dep graph + form mover) |

This is the homoiconicity dividend: the parser and printer are free (built into babashka), analysis is a `filter` on a `map`, transforms are `z/replace` on a zipper. ~800 lines total does what 50,000-line IDE plugins do.

clojure-lsp's `move-form` has been broken since 2021 ([issue #566](https://github.com/clojure-lsp/clojure-lsp/issues/566)). We sidestep it entirely.

## The Journey: Built, Used, Broken, Fixed

Every bug was found through real-world use on a 2768-line production file, not synthetic tests. Tests were written after each bug to prevent regression:

1. **`:mv` matches declare, not defn** — `find-form` hit the first name occurrence. Fixed: skip `declare` forms.
2. **`z/next` infinite walk** — traversed entire file, not just one form. Fixed: scope zipper to form subtree.
3. **Topo sort reversed** — Kahn's algorithm on wrong edge direction. Fixed: process zero-dependency forms first.
4. **Metadata blindness** — `(def ^:private events-file ...)` returned `"^:private"` as the name. Fixed: detect `:meta` node tag, walk to rightmost child.
5. **Declares in dep graph** — `(declare foo)` returned empty deps, masking real `(defn foo)` deps. Fixed: exclude declares from analysis.

See [docs/observations/](docs/observations/) for full ethnographic studies of the build-use-fix cycle.

## Testing

```bash
make test   # 49 tests, 181 assertions
```

All analysis functions are pure (string/zipper in, data out). Side effects are isolated to `!` variants. Tests use temp files and temp directories — no fixture pollution.

## Architecture

```
src/clj_surgeon/
  core.clj           # CLI entry point, :op dispatch
  outline.clj        # rewrite-clj form boundary parser
  forward_refs.clj   # clj-kondo forward-ref detection
  move.clj           # form reordering within a file
  analyze.clj        # dep graph, topo sort, dep tree, closure, dead code
  rename.clj         # namespace prefix rename (AST surgery)
  fix_declares.clj   # compound op: eliminate removable declares + pull leaf deps
```

~800 lines of Clojure total. Zero dependencies beyond babashka.
