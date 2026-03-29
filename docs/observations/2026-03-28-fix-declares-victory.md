# Ethnographic Study: From Zero to Declare-Free in One Session

**Date:** 2026-03-28
**Duration:** ~4 hours (tool creation + two real-world attempts)
**Subject:** Building and using clj-surgeon to eliminate forward declares in a 2768-line production Clojure namespace

## The Story Arc

### Act 1: The Dream (0:00 - 0:30)

It starts with a forward reference bug. Claude adds `(declare rebuild-ai-paragraphs!)` to `state.clj` — the seventh declare in a file that's grown to 2768 lines. Gene asks: "What if you could see the *structure* of a namespace?"

Claude describes what it wants: an outline showing every form with exact line boundaries. Gene pushes: "Could we do this without clojure-lsp?" They discover clojure-lsp's `move-form` has been broken since 2021 (issue #566). Gene says: "Just copy the form from one place to the other, like vi and sed."

Claude over-engineers. Gene pulls it back: "Do you really need more than that?" Claude admits: no. The outline is the only missing primitive. Everything else is just Read + Edit with precise line numbers.

### Act 2: The Build (0:30 - 2:00)

Gene says: "Maybe it's a bb script? My goodness. Exciting!"

The realization that rewrite-clj is **built into babashka** changes everything. Zero dependencies. 5ms startup. They scaffold `ns-surgeon` in minutes.

**What gets built, fast:**
- `:outline` — form boundaries via rewrite-clj zipper (works first try)
- `:mv` — reorder forms within a file
- Forward ref detection via clj-kondo (one shell command)
- `:declares` — which declares are removable? (topological sort)
- `:deps` — intra-namespace call graph (AST walk)
- `:closure` — minimal extractable unit
- `:topo` — optimal form ordering

**The self-rename:** Gene says "rename to clj-surgeon." Claude builds `:rename-ns` — AST-level namespace prefix replacement. Then runs it on itself. 10 files renamed, all tests green. The commit message: "Rename ns-surgeon → clj-surgeon using :rename-ns! (self-surgery)."

**Key numbers at this point:** 8 ops, 30 tests, 126 assertions, ~500 lines of Clojure.

### Act 3: First Real Use — The Humbling (2:00 - 3:00)

Gene launches a second Claude Code session to eliminate declares in `state.clj` using clj-surgeon.

**Bug 1: Ghost binary.** The AI calls `ns-surgeon` (old name). Then finds a stale bbin install at `~/.local/bin/ns-surgeon` pointing to the deleted directory. Two failures before finding `clj-surgeon`.

**Bug 2: Declare-not-defn.** `:mv` matches the `(declare split-into-pills)` at line 1007 instead of `(defn split-into-pills ...)` at line 1927. Moves the one-line declare, not the function. Forward ref unchanged.

**Bug 3: Whac-A-Mole.** After fixing `:mv` to skip declares, the AI manually moves `transition!` + `log-event!` to the top of the file. Tests fail: "Unable to resolve symbol: app-state." Adds `(declare app-state)`. Tests fail again: "Unable to resolve symbol: active-project-file." Each fix creates a new forward ref. The AI is doing exactly the grep-read-edit loop the tool was supposed to eliminate.

**The observation Gene makes:** "I think we need a delete-declare operation. Watching it edit the file is painful. Hahaha."

### Act 4: :fix-declares — The Compound Operation (3:00 - 3:45)

The insight from the humbling: **clj-surgeon's analysis is excellent. Its actions are too atomic.**

`:declares` instantly tells you what's removable. `:topo` gives the optimal ordering. `:deps` shows the dependency chain. But executing requires 20 individual commands.

The solution: `:fix-declares` — one command that does everything:
1. Find removable declares
2. Check each form's dependencies at the destination (prevent Whac-A-Mole)
3. Move safe forms (leaves first, topological order)
4. Delete stale declare lines
5. Skip unsafe moves with warnings

**Bug 4: Metadata blindness.** `(def ^:private events-file ...)` — the analyzer returns `"^:private"` as the name instead of `"events-file"`. So `log-event!`'s dependency on `events-file` is invisible. The fix: skip metadata nodes when extracting names (same fix outline.clj already had).

**Bug 5: Declares polluting the dependency graph.** `intra-ns-deps` included `(declare log-event!)` as a zero-dependency entry. When `fix-declares` looked up `log-event!`'s deps, it found the declare (empty) before the defn (has deps). Fix: exclude declares from the dependency graph entirely.

### Act 5: Victory (3:45 - 4:00)

```bash
$ clj-surgeon :op :fix-declares! :file src/writer/state.clj

{:summary {:moves 3, :declares-deleted 3, :skipped 4}}
```

```bash
$ make runtests-once
337 tests, 1056 assertions, 0 failures.
```

```bash
$ clj-surgeon :op :declares :file src/writer/state.clj
{:summary {:total 0, :removable 0, :needed 0}}
```

**Before:** 7 declares. AI spent 15+ minutes in Whac-A-Mole.
**After:** One command. 3 declares eliminated safely. 4 remaining are genuinely load-bearing (dependencies below destination). 337 tests pass.

## What Was Built

| When | What | Tests |
|------|------|-------|
| Hour 1 | `:outline`, `:mv`, forward refs | 11 tests |
| Hour 2 | `:declares`, `:deps`, `:topo`, `:closure` | 21 tests |
| Hour 2.5 | `:rename-ns` / `:rename-ns!` (self-surgery) | 28 tests |
| Hour 3 | Bug fixes from first real use | 32 tests |
| Hour 3.5 | `:fix-declares` / `:fix-declares!` | 42 tests |

**Final tally:** 10 ops, 42 tests, 157 assertions, ~700 lines of Clojure, zero deps beyond babashka.

## Bugs Found and Fixed

| # | Bug | Root Cause | Fix | Test Added |
|---|-----|-----------|------|------------|
| 1 | `:mv` moves declare, not defn | `find-form` matches first name occurrence | Skip `(declare ...)` forms | test-move-skips-declare |
| 2 | `z/next` infinite walk | Walking whole file, not just the form subtree | `z/of-string` on form text to scope traversal | (fixed symbols-in-form) |
| 3 | Topo sort direction reversed | Kahn's algorithm on wrong edge direction | Flip to dep-count (zero-dependency forms first) | test-topological-sort |
| 4 | Metadata names: `^:private` instead of actual name | `z/down z/right` hits metadata node | Check `n/tag` for `:meta`, walk to rightmost child | test-intra-ns-deps-skips-declares |
| 5 | Declares in dependency graph | `(declare foo)` returns empty deps, masks real `(defn foo)` deps | Exclude declares from `intra-ns-deps` | test-intra-ns-deps-skips-declares |

Every bug was found through real-world use, not unit tests. The tests were written *after* each bug to prevent regression. This is the natural TDD cycle for tools: build → use → break → fix → test → repeat.

## The Homoiconicity Dividend

The session proved something concrete: **structural operations on Clojure code are trivially implementable because the code is data.**

- Namespace rename (AST symbol replacement): ~170 lines
- Dependency graph (tree walk): ~30 lines
- Topological sort: ~40 lines
- Forward ref detection: one clj-kondo command
- Form boundary detection: built into rewrite-clj metadata
- Require inference: filter qualified symbols against alias map (~10 lines)

In a non-homoiconic language, each of these would require a full parser, import resolver, and module system integration. In Clojure, they're tree walks on the same S-expression zipper.

The total codebase is ~700 lines. The equivalent IntelliJ plugin would be 50,000+.

## What's Next

The 4 remaining declares in `state.clj` need `:mv-with-deps` — move a form AND all its dependencies as a group, in topological order. This is the next compound operation, and it composes the same primitives:

1. `:deps` to find the dependency chain
2. `:topo` to order them
3. `:mv` to move each one (leaves first)
4. Delete stale declares

Estimated effort: ~50-80 lines on top of existing primitives.

Beyond that: `:extract` (move forms to a new namespace), `:suggest-split` (find natural partition boundaries), and `:find` (structural pattern matching on the AST).

Each is 50-100 lines. Each composes existing primitives. The hard part was building the primitives. That's done.
