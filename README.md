# clj-surgeon

Structural operations on Clojure namespaces. A babashka CLI that parses Clojure code as data (not text) using [rewrite-clj](https://github.com/clj-commons/rewrite-clj), returning EDN.

**Origin story:** I watched Claude Code spend 45 minutes refactoring a 5,000-line `views.clj` file — painfully extracting functions, moving them, reading and re-reading to get the ordering right, burning through context window. It was doing the right things, just agonizingly slowly. So I asked it: *"What would the ideal tool be to help you manipulate beautiful Clojure homoiconic EDN files?"* clj-surgeon was born 45 minutes later.

**Built in one session. From zero to production use in 4 hours.** 13 ops, ~1500 lines of Clojure, zero dependencies beyond babashka.

**Designed for AI-assisted development.** clj-surgeon is a [Claude Code](https://claude.ai/claude-code) skill — Claude knows how to use every operation, when to call each one, and how to compose them. The tool provides structural visibility (the X-ray); the AI provides judgment (what to do about it). See [How Claude Code Uses This](#how-claude-code-uses-this).

## Measured Performance

In a planning session (writer, 5 files, ~5000 lines), two approaches explored the same codebase simultaneously — clj-surgeon outlines vs. Explore agents reading the files:

- **150x more token-efficient.** clj-surgeon outlined 5 files (command_center.clj 821 lines, routes.clj 2894 lines, state.clj 614 lines, processes.clj 508 lines, process_handlers.clj 198 lines) in ~1,000 tokens total. The Explore agents burned ~150K tokens producing similar information.
- **100x faster.** clj-surgeon returned in milliseconds. The Explore agents took ~100 seconds.

~5,000 lines of code, fully mapped — namespace structure, function signatures, dependency relationships — in ~200 tokens per file.

**Give the AI structural visibility so it reads less and understands more.** The tool provides the X-ray; the AI provides the judgment. The result is faster exploration, smaller context windows, and better-informed decisions.

## Headline Feats

**Self-surgery:** clj-surgeon renamed itself from `ns-surgeon` to `clj-surgeon` — 10 files, 10 file moves, every ns declaration and `:require` entry updated by walking and manipulating the EDN data structure, in less than one second. No more painful watching Claude do grep-and-replace. Symbol node replacement in the parse tree. Aliases preserved, string literals untouched, parens always balanced.

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
git clone https://github.com/realgenekim/clj-surgeon.git
cd clj-surgeon
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

#### `:extract` / `:extract!` — Move forms to a new namespace

```bash
clj-surgeon :op :extract :file src/writer/state.clj \
  :forms '[rebuild-ai-paragraphs! enter-distillery!]' \
  :to src/writer/state/distillery.clj    # plan (dry run)

clj-surgeon :op :extract! :file src/writer/state.clj \
  :forms '[rebuild-ai-paragraphs! enter-distillery!]' \
  :to src/writer/state/distillery.clj    # execute
```

The compound extraction operation:
1. Finds named forms and their exclusive private helpers (`:ls-extract` closure)
2. Copies the source ns form as a template (over-include requires, don't under-include)
3. Writes forms to the new file in topological order
4. Removes extracted forms from the source file
5. Adds a require for the new namespace to the source file
6. Reports callers that may need updating

Planning is pure — only `:extract!` writes files. The compiler catches bare refs instantly. The AI fixes what the compiler reports.

## How Claude Code Uses This

clj-surgeon ships as a Claude Code [skill](https://docs.anthropic.com/en/docs/claude-code/skills) that teaches the AI when and how to use each operation. In practice:

**Before reading a large file,** Claude runs `:ls` to get form boundaries in ~50 tokens instead of reading 2000+ lines blind. It then `Read`s only the specific line ranges it needs.

**When it sees a `declare`,** Claude runs `:fix-declares!` instead of manually moving forms one by one. The tool handles dependency checking, leaf-pulling, and declare deletion — the AI just runs one command and verifies with tests.

**When planning an extraction,** Claude runs `:ls-deps` to see the full dependency tree, then `:ls-extract` to find the minimal extraction unit. It uses this information to decide whether to extract to a new namespace or just reorder within the file.

**When renaming,** Claude runs `:rename-ns` to see the plan (every file that would change, every require that would update), then `:rename-ns!` to execute. No grep-and-replace, no missed references.

The design philosophy: **give the AI better visibility, not cleverer automation.** The tool provides the X-ray; the AI provides the judgment. Simple tools that compose beat complex tools that guess.

**The one line that makes it all automatic.** This is what I put in my global `CLAUDE.md` (instructions Claude sees in every conversation):

> **For Clojure codebase exploration**: ALWAYS use `/clj-surgeon` outline before spawning Explore agents or reading .clj files. Measured: 150x more token-efficient than Explore agents (5 files, ~5000 lines mapped in ~1000 tokens vs ~150K tokens). Returns in milliseconds vs ~100 seconds. Use `:ls` for form boundaries (~50 tokens per file), then `Read` only the specific line ranges you need. Only spawn Explore agents for targeted follow-up questions with specific file paths.

With that single instruction, Claude reaches for clj-surgeon first in every Clojure project — no prompting needed.

## Why This Works

Everyone knows Clojure code is data. A namespace form isn't text to grep — it's a list to walk. [rewrite-clj](https://github.com/clj-commons/rewrite-clj) gives us the AST with position tracking. Every operation is a tree walk:

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

This is the homoiconicity dividend: the parser and printer are free (built into babashka), analysis is a `filter` on a `map`, transforms are `z/replace` on a zipper. ~1500 lines total does what 50,000-line IDE plugins do.

clojure-lsp's `move-form` issue has been open since 2021 ([issue #566](https://github.com/clojure-lsp/clojure-lsp/issues/566)). We sidestep all the issues they highlight, by leaving that to the coding agent.

## The Journey: Built, Used, Broken, Fixed

I guess Claude Code wants to brag about this.  We found a bunch of bugs through real-world use on a 2768-line production file, not synthetic tests. Tests were written after each bug to prevent regression:

1. **`:mv` matches declare, not defn** — `find-form` hit the first name occurrence. Fixed: skip `declare` forms.
2. **`z/next` infinite walk** — traversed entire file, not just one form. Fixed: scope zipper to form subtree.
3. **Topo sort reversed** — Kahn's algorithm on wrong edge direction. Fixed: process zero-dependency forms first.
4. **Metadata blindness** — `(def ^:private events-file ...)` returned `"^:private"` as the name. Fixed: detect `:meta` node tag, walk to rightmost child.
5. **Declares in dep graph** — `(declare foo)` returned empty deps, masking real `(defn foo)` deps. Fixed: exclude declares from analysis.

See [docs/observations/](docs/observations/) for full ethnographic studies of the build-use-fix cycle.  (I now do this with all my projects, so Claude Code can figure out what might make work easier.)

## Testing

```bash
make test
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
  extract.clj        # compound op: move forms to a new namespace file
```

Zero dependencies beyond babashka.

## Prior Art & Acknowledgments

clj-surgeon exists because of the incredible tools it builds on:

- **[rewrite-clj](https://github.com/clj-commons/rewrite-clj)** by Yannick Scherer ([@xsc](https://github.com/xsc)), maintained by Lee Read ([@lread](https://github.com/lread)) — the foundation. Every operation in clj-surgeon is a tree walk on the zipper that rewrite-clj provides. Without it, this project would be 50,000 lines instead of 1,500.
- **[babashka](https://github.com/babashka/babashka)** by Michiel Borkent ([@borkdude](https://github.com/borkdude)) — rewrite-clj and cheshire are built in, so clj-surgeon has zero external dependencies. Startup in milliseconds, not seconds.
- **[clj-kondo](https://github.com/clj-kondo/clj-kondo)** by Michiel Borkent ([@borkdude](https://github.com/borkdude)) — forward reference detection is a single shell-out to clj-kondo. We get static analysis for free.

Two things from Eric Dallo ([@ericdallo](https://github.com/ericdallo)) were especially catalytic:

The long-open [clojure-lsp#566](https://github.com/clojure-lsp/clojure-lsp/issues/566) ("Moving vars/function to a different namespace," filed 2021, still open) was thought-provoking. It's a genuinely hard problem in a language server — you need to handle every edge case correctly, because the tool acts alone. But it's also a perfect example of the bitter lesson: the hard part isn't cutting forms and rewriting requires — that's bookkeeping. The hard part is *deciding what to move and where* — that's judgment. A language server has to solve both. clj-surgeon only solves the bookkeeping and leaves the judgment to the LLM, which is why it's 1,500 lines instead of an open issue for 5 years.

Eric's talk on [ECA](https://github.com/editor-code-assistant/eca) (Editor Code Assistant) at Clojure/Conj 2025 was mind-expanding — it got me thinking about the problem of AI-assisted code manipulation with fresh eyes. The idea that AI pair programming should be editor-agnostic and work at the structural level resonated deeply with what became clj-surgeon's approach.

## Roadmap: Stay Dumb, Stay Useful

After building 13 ops and watching the AI use them, a clear principle emerged: **build tools for bookkeeping, not for thinking.**

The valuable ops eliminate mechanical work — precisely cutting 18 forms from a 2768-line file, rewriting requires across 10 namespaces, topologically sorting a dependency graph. The AI knows *what* to do but the mechanics are slow, error-prone, or burn context window. clj-surgeon does bookkeeping and manipulation.

The tempting-but-wrong ops replace judgment — detecting patterns, suggesting splits, finding dead code. The AI is *good* at judgment. It can read `:deps` output and see the clusters. It can read `git diff` and understand what changed. Building a tool for that replaces flexible reasoning with rigid pattern matching.

| Considered | Why not | What to do instead |
|---|---|---|
| `:suggest-split` | AI + `:deps` + `:ls` = judgment | AI reads the dep graph and decides |
| `:dead-code` | clj-kondo already warns | AI greps and checks |
| `:diff` (semantic) | AI reads `git diff` | AI compares form names across versions |
| `:find-extractable-pure` | Detection easy, extraction is judgment | AI knows the pattern, uses `:extract!` after |

The principle:

> **The tool provides the X-ray. The AI provides the judgment. The compiler catches what the tool misses. The AI fixes what the compiler reports.**

Future ops will stay on the bookkeeping side of this line. clj-surgeon stays dumb. The AI stays smart (and is getting smarter all the time: the bitter lesson).

Also: I keep thinking the project name should be `clj-scalpel`, because you the programmer are the surgeon, and this is just a tool.  But it's just harder to type, I have `clj-surgeon` referenced everywhere already, so I guess I'm sticking wit this name.  I guess the tool is the surgeon, and you're head of the medical ward!