# clj-surgeon

A babashka CLI and Claude Code skill for exploring Clojure codebases via the AST and making structural refactorings — move forms, fix declares, rename namespaces, extract to new files. Parses Clojure code as data (not text) using [rewrite-clj](https://github.com/clj-commons/rewrite-clj), returning EDN.

**You'll want this if you:**
- Need to split up giant `.clj` files that Claude Code is prone to creating — deterministically move forms (within and between files), along with their dependencies
- Want to get rid of `(declare ...)` forms (which Claude Code is prone to creating) by automatically reordering `defn`s and `def`s
- Want Claude Code to explore a Clojure codebase via the AST instead of reading entire files — 100x faster, 150x fewer tokens
- Want one exact top-level form by name or containing line without reconstructing a `sed` range

**Origin story:** I watched Claude Code spend 45 minutes refactoring a 5,000-line `views.clj` file — painfully extracting functions, moving them, reading and re-reading to get the ordering right, burning through context window. It was doing the right things, just agonizingly slowly. So I asked it: *"What would the ideal tool be to help you manipulate beautiful Clojure homoiconic EDN files?"* clj-surgeon was born 45 minutes later.

**Built in one session. From zero to production use in 4 hours.** 13 ops, ~1500 lines of Clojure, zero dependencies beyond babashka.

All CLI commands are available to [Claude Code](https://claude.ai/claude-code) as a [skill](https://code.claude.com/docs/en/skills) — you don't need to learn all the commands, because Claude Code already knows how to use them. See [Teach Claude Code](#teach-claude-code).

## Measured Performance

In a planning session (writer, 5 files, ~5000 lines), two approaches explored the same codebase simultaneously — clj-surgeon outlines vs. Explore agents reading the files:

- **150x more token-efficient.** clj-surgeon outlined 5 files (command_center.clj 821 lines, routes.clj 2894 lines, state.clj 614 lines, processes.clj 508 lines, process_handlers.clj 198 lines) in ~1,000 tokens total. The Explore agents burned ~150K tokens producing similar information.
- **100x faster.** clj-surgeon returned in milliseconds. The Explore agents took ~100 seconds.

~5,000 lines of code, fully mapped — namespace structure, function signatures, dependency relationships — in ~200 tokens per file.

The result: Claude explores a codebase without reading entire files, so it stays fast and doesn't fill up its context window.

## Headline Feats

**Self-surgery:** Renaming namespaces is something I often want to do but avoid because it's so invasive. But I was able to rename this project from `ns-surgeon` to `clj-surgeon` in less than one second — 10 files, 10 file moves, every ns declaration and `:require` entry updated by walking and manipulating the EDN data structure. No more painful watching Claude do grep-and-replace. Symbol node replacement in the parse tree. Aliases preserved, string literals untouched, parens always balanced.

**One-command declare cleanup:** A 2768-line production namespace with 7 forward declares. One command:

```
$ clj-surgeon :op :fix-declares! :file src/writer/state.clj

{:summary {:moves 7, :declares-deleted 5, :skipped 2}}

$ make runtests-once
337 tests, 1056 assertions, 0 failures.
```

5 declares eliminated (3 direct moves + 2 with leaf deps pulled along), 2 unsafe moves correctly skipped (they have non-leaf dependency chains).

Previously, Claude spent 15+ minutes in Whac-A-Mole: move `transition!` up → `"Unable to resolve symbol: app-state"` → add a declare → `"Unable to resolve symbol: active-project-file"` → each fix creates a new forward ref. The [ethnographic observation](docs/observations/2026-03-28-first-real-use.md) captured it: *"clj-surgeon's analysis is excellent. Its actions are too atomic."* So we built `:fix-declares` — a compound operation that moves leaves first in topological order, never creating a new unresolved reference.

## Install

```bash
git clone https://github.com/realgenekim/clj-surgeon.git
cd clj-surgeon
make install    # → ~/bin/clj-surgeon and ~/.codex/skills/clj-surgeon
```

`make install` installs the CLI and symlinks the repository-owned Codex skill
from `skills/clj-surgeon` into `${CODEX_HOME:-$HOME/.codex}/skills`. Run
`make install-cli` or `make install-codex-skill` to install only one artifact.
Installation refuses to overwrite an existing non-symlink skill directory.

To put the CLI somewhere else, pass its complete file path as `CLI_DEST`. The
installer creates the parent directory and handles paths containing spaces:

```bash
make install-cli CLI_DEST="$HOME/.local/bin/clj-surgeon"
```

`CLI_DEST` affects only the CLI. Set `CODEX_HOME` independently when the Codex
skill also needs a non-default home:

```bash
make install \
  CLI_DEST="$HOME/.local/bin/clj-surgeon" \
  CODEX_HOME="$HOME/.config/codex"
```

### Requirements

Two binaries must be on `PATH`:

- **[babashka](https://babashka.org/)** (`bb`) — the runtime. rewrite-clj and cheshire are built in.
- **[clj-kondo](https://github.com/clj-kondo/clj-kondo)** — used by `:ls`, `:outline`, `:fix-declares!`, and any op that needs forward-reference detection. Without it those ops fail.

No-sudo install (e.g. inside the Claude Code sandbox, where `sudo` is blocked):

```bash
mkdir -p ~/bin   # ~/.profile already prepends ~/bin to PATH on Ubuntu/Debian
bash <(curl -s https://raw.githubusercontent.com/babashka/babashka/master/install) --dir ~/bin
bash <(curl -s https://raw.githubusercontent.com/clj-kondo/clj-kondo/master/script/install-clj-kondo) --dir ~/bin
```

`make install` will warn (but not fail) if either binary is missing.

What changed since you installed? See [CHANGELOG.md](CHANGELOG.md).

## Teach Claude Code

Add this line to your project's `CLAUDE.md`:

```
Read <path-to-clj-surgeon>/skill.md — it teaches you when and how to use clj-surgeon for Clojure structural operations.
```

`skill.md` contains the exact one-shot read/edit routes and points uncommon
move, extraction, dependency, and CLJC work to the repository-owned advanced
reference. One line, and Claude reaches for clj-surgeon automatically without
loading every operation into each simple task.

I added it to my global `~/.claude/CLAUDE.md` because it's so freaking useful — Claude uses it in every Clojure project without being asked. You may eventually want to do the same. Here's what I put in mine:

> **For Clojure codebase exploration**: ALWAYS use `/clj-surgeon` before spawning Explore agents or reading .clj files. Measured: 150x more token-efficient than Explore agents (5 files, ~5000 lines mapped in ~1000 tokens vs ~150K tokens). Returns in milliseconds vs ~100 seconds. Use `:ls` for an unknown file's form boundaries (~50 tokens per file). When a top-level name, containing line, or distinctive literal text is known, use `:show-form` as the first source inspection; do not run `:ls` solely as a preflight. Use `:show-form :contains` instead of an `rg -n` line-number bridge inside one Clojure file. Use `:grep-form` for file-wide structural search, and a bounded text read only when context genuinely spans forms. Keep `rg` for broad cross-file discovery. Only spawn Explore agents for targeted follow-up questions with specific file paths.

## Operations

### Read-only operations

#### `:ls` / `:outline` — See the skeleton of a namespace

```bash
clj-surgeon :op :ls :file src/writer/state.clj
```

Every top-level form with exact line boundaries, types, names, arglists, and forward reference detection. 236 forms in a 2768-line file, returned in ~200ms.

#### `:show-form` / `:cat` — Read one complete top-level form

Use a name:

```bash
clj-surgeon :op :show-form :file src/writer/state.clj :form transition!
# Equivalent structural-shell spelling:
clj-surgeon :op :cat :file src/writer/state.clj :form transition!
```

Or use a line contained by the form:

```bash
clj-surgeon :op :show-form :file src/writer/state.clj :line 1134
```

Or select the one form containing distinctive literal text:

```bash
clj-surgeon :op :cat :file src/writer/state.clj :contains :finish
```

Supply exactly one of `:form`, `:line`, or `:contains`. `:contains` is a
case-sensitive literal substring, not a regular expression. It searches form
source, strings, docstrings, and attached comments. Multiple occurrences in
one form succeed; occurrences in multiple forms refuse with bounded candidate
evidence. CLI values remain literal text, so keyword-shaped searches such as
`:finish` do not need an EDN-string workaround. For ambiguous reader-conditional
definitions, add `:platform :clj` or `:platform :cljs`. Success returns the
exact parsed form source, type, name when present, platforms, line range,
attached-comment start, and complete-file source hash. Missing or ambiguous
selectors return structured EDN and exit nonzero; the command never chooses
the first match.

`:cat` is a strict alias. It never dumps the complete file, and its result
retains the canonical machine identity `:operation :show-form`.

When a top-level name, containing line, or distinctive text is known, use
`:show-form` as the first
source inspection instead of reconstructing a `sed` range. Do not run `:ls`
solely as a preflight. Continue to use `rg` for broad textual discovery,
`:grep-form` for nested structural syntax, and bounded text reads for context
that genuinely spans forms or files. Inside one Clojure file, use
`:show-form :contains` instead of `rg -n` followed by `:show-form :line`; do not
print a large outline merely to discover the line.

#### `:ls-tree` / `:tree` / `:map` — Map an entire directory of repos

```bash
# Map a single project
clj-surgeon :op :ls-tree :dir .

# Search across ALL your repos in seconds
clj-surgeon :op :ls-tree :dir ~/src.local/ :grep "postgres|jdbc"
```

Discovers projects via `deps.edn`/`project.clj`, reads their `:paths`, outlines every `.clj` file. With `:grep`, uses ripgrep to find matching files first — turns a 90-second full scan into a 3-second surgical search.

**The value:** "Which of my 20 repos talks to Postgres?" answered in one command, 3 seconds, ~3K tokens. An Explore agent takes 80+ seconds and 55K tokens for the same question.

See [docs/ls-tree.md](docs/ls-tree.md) for full documentation.

#### `:ls-deps` — Transitive dependency tree

```bash
clj-surgeon :op :ls-deps :file state.clj :form transition!
```

The full dependency chain as a nested tree — shows which deps are leaves, which have their own deps, and which are circular. Use this to check whether a form can be safely moved — if all its deps are leaves, it's a clean extraction.

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

### CLJ / CLJS / CLJC operations

LLMs and humans both struggle with reader conditionals. Forms inside `#?(:clj ...)` aren't lists — pre-existing AST tools silently miss them, and free-form LLM editing produces malformed splices, divergent aliases that don't line up, and silently-dropped `:refer` lists.

These ops are **deterministic**: same input always produces the same output, and the tool refuses to emit malformed reader conditionals.

The pre-existing read-only ops (`:ls`, `:deps`, `:topo`, `:ls-deps`, `:ls-extract`, `:declares`) are **reader-conditional-aware** — `(defn foo …)` inside `#?(:clj …)` shows up in the outline with `:platforms [:clj]` and participates in dependency analysis.

#### `:cljc-merge` — Combine parallel CLJ + CLJS files into one CLJC

```bash
clj-surgeon :op :cljc-merge :clj src/foo.clj :cljs src/foo.cljs :out src/foo.cljc
```

Handles shared requires, one-sided requires (`#?@(:clj […])` / `#?@(:cljs […])`), divergent aliases (the `dom`/`dom-server` pattern: same alias bound to different namespaces), divergent `:refer` lists, npm string requires routed to `:cljs`, and per-form body collisions emitted as `#?(:clj … :cljs …)`. Throws clearly on ns docstrings, attr-maps, unsupported sub-forms, name mismatches, and body-count mismatches rather than producing wrong output.

#### `:cljc-split` — Inverse of merge

```bash
clj-surgeon :op :cljc-split :file src/foo.cljc :clj-out src/foo.clj :cljs-out src/foo.cljs
```

Round-trip-tested: `(merge → split → merge)` is a fixed point.

#### `:cljc-add-require` — Platform-aware require addition

```bash
clj-surgeon :op :cljc-add-require :file src/foo.cljc \
  :platform :cljs :ns goog.string :as gstr :out src/foo.cljc
```

`:platform` is `:clj`, `:cljs`, or `:cljc`. Detects alias collisions (refuses to bind one alias to two namespaces on the same platform). Preserves npm string literals (`:ns "react"` stays a string, doesn't get coerced to a symbol).

#### `:cljc-analyze` — Structured classification for LLM consumption

```bash
clj-surgeon :op :cljc-analyze :clj src/foo.clj :cljs src/foo.cljs    # pair
clj-surgeon :op :cljc-analyze :file src/foo.cljc                      # single CLJC
```

Returns EDN with shared / one-sided / divergent require buckets and per-platform top-level form summaries — everything an LLM needs to plan an edit.

### Write operations

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

`mv` writes its input file directly unless `:dry-run true` is present. An LLM
should use this branching workflow, not jump straight to `:mv-with-deps`:

```bash
# 1. Orient and preview the exact requested move. Neither command writes.
clj-surgeon :op :ls :file state.clj
clj-surgeon :op :mv :file state.clj :form foo :before bar :dry-run true

# 2a. Only when the preview returns :ok true, review :plan/:diff and execute.
clj-surgeon :op :mv :file state.clj :form foo :before bar
```

Moves a named form (including its preceding comment header) before another
form. Before writing, `:mv` validates the complete candidate. If the move would
strand a dependency or caller, it refuses with structured EDN, changes no
bytes, and exits nonzero.

A dependency refusal is a decision point for an agent:

```clojure
{:error-type :would-strand-dependencies
 :form "foo"
 :before "bar"
 :stranded [{:name "config"
             :defined-at 80       ; original source line
             :would-be-at 94      ; line in the rejected candidate
             :required-before 40}]
 :recommended-action :preview-dependency-closure
 :recommended-command
 "clj-surgeon :op :mv-with-deps :file state.clj :form foo :before bar :dry-run true"
 :apply-command
 "clj-surgeon :op :mv-with-deps :file state.clj :form foo :before bar"}
```

Run the safe `:recommended-command`, then inspect all of
`:plan/:added-forms`, `:move-order`, and `:diff`. Execute `:apply-command` only
after consenting to every added form. The successful preview repeats
`:apply-command`, so it is self-contained. `:mv-with-deps` always forces
`:with-deps true`, even if a caller also supplies `:with-deps false`.

```bash
# 2b. Dependency refusal only: preview the widened move, then apply after review.
clj-surgeon :op :mv-with-deps :file state.clj :form foo :before bar :dry-run true
clj-surgeon :op :mv-with-deps :file state.clj :form foo :before bar
```

`:mv-with-deps` is the opt-in alias for `:mv :with-deps true`. It moves only
the minimum transitive dependency closure required at the selected destination,
lists every added form in the plan, and never silently adds declarations or
moves callers. For `:would-strand-users`, cycles, ambiguity, unsupported source
layouts, or any other refusal, stop and choose a different destination or
refactor; do not force the alias.

A dry run is an informational preview, not a saved hash-bound application
artifact. Preview again after any source change. After writing, rerun `:ls`,
audit any now-redundant declaration with `:declares` or `:fix-declares`, and run
the target repository's formatter, linter, compiler, and tests. Shell-quote
names containing special characters, for example `:form '*state*'`.

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

#### `:grep-form` / `:find-subform` / `:replace-subform` — Nested structural edits

Use `:grep-form` for file-wide structural search. It is a strict alias for
`:find-subform`, not a text regular expression. Matching ignores formatting,
and `_` matches exactly one subtree:

```bash
clj-surgeon :op :grep-form :file src/views.clj \
  :match '(post! "/api/items" _)'
```

Add `:inside` only when the containing top-level form is already known or when
you need to narrow multiple matches:

```bash
clj-surgeon :op :find-subform :file src/views.clj :inside render \
  :match '(post! "/api/items" _)'
```

Plan exactly one replacement, review the EDN and diff, then apply that exact
hash-guarded plan:

```bash
clj-surgeon :op :replace-subform :file src/views.clj :inside render \
  :match '(post! "/api/items" _)' \
  :with '(items/actions surface)' \
  :plan-out plan.edn

clj-surgeon :op :replace-subform! :plan plan.edn
```

Search reports every match; replacement refuses zero or multiple matches.
Each search match includes its enclosing top-level name in `:inside` when one
is mechanically available. Reuse that value directly to narrow a replacement;
do not run `:show-form :line` merely to recover the owner.
`:match` and `:with` must each contain exactly one complete Clojure form—trailing
syntax is an error. The plan is a versioned EDN artifact with stable
`:operation`, `:selector`, hash, edit, diff, and provenance fields. Its `:diff`
is a standard unified diff, suitable for human or agent review.

Application validates the plan version, unchanged source snapshot, recorded
address, exact before text, complete rewritten-file parse, and result hash. It
then atomically replaces the target file and reads it back. A successful EDN
receipt includes `:applied-edit` plus `:verified` whole-file parse,
`:read-back-hash`, and atomic-write evidence. Do not repeat those exact checks
with `rg`, `:show-form`, `git diff`, or `shasum`; the reviewed plan is the
edit-level diff. Proceed to relevant formatter, linter, compiler, and test
commands, then review the aggregate repository diff once. If atomic replacement is unavailable,
the command fails and does not fall back to a weaker write. Every error is
concise EDN and every error exits nonzero.

Apply the reviewed plan directly with `:replace-subform!`. Do not edit the plan
with `apply_patch` or another text tool. If the intended edit changes, generate
a new plan.

A `case` clause, `cond` branch, map entry, or binding pair is adjacent sibling
syntax, not a synthetic wrapper list. Until a sibling-span lens exists, match
an independently readable contained value or expression. When sibling text
identifies the target, use `:cat :contains` on that key, guard, or name to get
its owner and surrounding form in one read. See
[issue #21](https://github.com/realgenekim/clj-surgeon/issues/21).

Run plan generation as a standalone shell command. Observe and review its
result before a separate apply command; never chain planning and application.

In its [first production use](docs/observations/2026-07-11-subform-first-real-use.md),
`:ls` reduced a 4,036-line, 322-form namespace to the relevant synchronization
and admission forms. Three `:find-subform` queries then uniquely located the
Save Draft Hiccup vector, an unsafe browser-sync mutation, and a session-save
call by semantic path. The practical division was clear: `rg` remained faster
for broad discovery, while structural search was substantially safer for
repeated Hiccup actions and large handler forms. This first report validated
discovery; the follow-up below validated the replacement workflow separately.

A follow-up production edit supplied that proof: `:find-subform` uniquely
selected a 13-line inline-JavaScript “Edit in Draft” button, and the reviewed
plan replaced it with a shared single-flight command call. `:replace-subform!`
returned the exact result hash recorded by the plan.

When a replacement contains Clojure that generates JavaScript, quote the whole
`:with` form with single shell quotes and use `pr-str` for generated JavaScript
string literals:

```bash
clj-surgeon :op :replace-subform \
  :file src/writer/views/book_workshop.clj \
  :inside editor-panel \
  :match '[:button {:style _ :onclick _} "Edit in Draft →" [:span.kbd-hint _ "^Enter"]]' \
  :with '[:button {:onclick (str "editBookNodeInDraft(" (pr-str id) ",this.closest(" (pr-str ".bw-editor-panel") "))")} "Edit in Draft →"]' \
  :plan-out plan.edn
```

JavaScript `\xNN` escapes are not valid EDN/Clojure string escapes. Prefer
`pr-str`, or valid Clojure escapes such as `\"`, `\\`, and `\u0027`.

A broader [session-history ethnography](docs/observations/2026-07-12-lenses-in-the-wild.md)
found 38 direct production lens invocations across UI, state, storage, and route
work. Beyond replacement, agents used zero/one match results as executable
hypotheses about code shape and used sequential hash-bound plans to refactor a
fast-moving dirty worktree safely.

## Custom Defining Forms

Real-world Clojure codebases don't just use `defn`. Libraries like [Guardrails](https://github.com/fulcrologic/guardrails) (`>defn`, `>defn-`), [Malli](https://github.com/metosin/malli) (`mu/defn`), and [Schema](https://github.com/plumatic/schema) (`s/defn`) define their own `defn`-like macros. Large projects add their own: `defendpoint`, `defenterprise`, `defsetting`.

clj-surgeon recognizes all of these. Every operation — `:ls`, `:deps`, `:topo`, `:ls-deps`, `:ls-extract`, `:extract`, `:fix-declares` — uses a single classification module (`forms.clj`) to decide what counts as a definition, what has arglists, and what is private.

**How it works:**

1. **Core forms** — `defn`, `defn-`, `def`, `defonce`, `defmacro`, etc. — matched exactly.
2. **Namespace-qualified forms** — `mu/defn`, `s/defn`, `malli.util/defn-` — the alias is stripped and the local name is matched against core forms. This works regardless of what alias the project uses (`mu/defn`, `m/defn`, `malli/defn` all resolve to `:defn`).
3. **Explicit aliases** — forms with non-standard names (`>defn`, `>defn-`) that can't be auto-detected from the local name. These are listed in `forms.clj`.

**To add support for a new macro,** check whether it's already handled:

```clojure
;; Namespace-qualified forms just work — no config needed
(mu/defn foo ...)     ; ✓ auto-detected: local name "defn" → :defn
(s/defn bar ...)      ; ✓ auto-detected: local name "defn" → :defn
(my.lib/defn- baz ..) ; ✓ auto-detected: local name "defn-" → :defn- (private)
```

If the macro has a non-standard name (doesn't end in a core form name after `/`), add one line to `explicit-aliases` in `src/clj_surgeon/forms.clj`:

```clojure
(def explicit-aliases
  {">defn"          :defn
   ">defn-"         :defn-
   "defendpoint"    :defn      ;; your custom macro here
   "defenterprise"  :defn})
```

## How Claude Code Uses This

clj-surgeon ships as a Claude Code [skill](https://code.claude.com/docs/en/skills) — a markdown file that tells Claude when to run each command. In practice:

**Before reading a large file,** Claude runs `:ls` to get form boundaries in ~50 tokens instead of reading 2000+ lines blind. It then `Read`s only the specific line ranges it needs.

**When it sees a `declare`,** Claude runs `:fix-declares!` instead of manually moving forms one by one. The tool handles dependency checking, leaf-pulling, and declare deletion — the AI just runs one command and verifies with tests.

**When planning an extraction,** Claude runs `:ls-deps` to see the full dependency tree, then `:ls-extract` to find the minimal extraction unit. It uses this information to decide whether to extract to a new namespace or just reorder within the file.

**When renaming,** Claude runs `:rename-ns` to see the plan (every file that would change, every require that would update), then `:rename-ns!` to execute. No grep-and-replace, no missed references.

The pattern: the tool does mechanical work (parsing, moving, rewriting requires). Claude decides what to move and where.

## How This Tool Was Designed

Each feature was created by watching Claude Code work on real refactoring tasks and identifying where it struggled.

1. **Watch the AI work.** Give Claude Code a real refactoring task on a real codebase. Don't help.
2. **Identify the pain.** Where does it burn tokens re-reading files? Where does it play Whac-A-Mole? Where does each fix create a new problem?
3. **Separate bookkeeping from judgment.** The AI knows *what* to move and *where*. It struggles with *precisely cutting forms from a 2768-line file without breaking anything*.
4. **Build the smallest tool.** `:ls` was first — form boundaries in 50 tokens instead of reading 2000 lines. Then `:mv`, then `:fix-declares`, each born from watching the AI hit the next mechanical bottleneck.
5. **Use it, break it, fix it.** Every bug in [The Journey](#the-journey-built-used-broken-fixed) was found by watching the AI hit it in real time. Tests came after.

The full session transcripts are in [docs/observations/](docs/observations/). If you're building tools for AI-assisted development, start there.

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
  forms.clj          # single source of truth for defining-form classification
  core.clj           # CLI entry point, :op dispatch
  outline.clj        # rewrite-clj form boundary parser (CLJC-aware)
  forward_refs.clj   # clj-kondo forward-ref detection
  move.clj           # form reordering within a file
  analyze.clj        # dep graph, topo sort, dep tree, closure (CLJC-aware)
  rename.clj         # namespace prefix rename (AST surgery)
  fix_declares.clj   # compound op: eliminate removable declares + pull leaf deps
  extract.clj        # compound op: move forms to a new namespace file
  cljc/
    walk.clj         # reader-conditional-aware top-level form walker
    merge.clj        # CLJ + CLJS → CLJC (divergent aliases, npm, body collisions)
    split.clj        # CLJC → CLJ + CLJS (inverse of merge)
    require_ops.clj  # platform-aware add-require (with alias-collision detection)
    analyze.clj      # structured CLJC classification for LLM consumption
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

[cclsp](https://github.com/dazld/cclsp) by Dan Peddle ([@dazld](https://github.com/dazld)) — Claude Code LSP integration — was also eye-opening. Even just reading the list of clojure-lsp commands available through it (`clean-ns`, `extract-function`, `inline-symbol`, `move-to-let`, `cycle-privacy`, `create-test`...) was mind-bending. These structural operations are *possible*. I had trouble using them reliably, especially given the aforementioned clojure-lsp move-form issue, but seeing what the LSP could do shaped what clj-surgeon tries to do — the same structural manipulations, via rewrite-clj, without the LSP's edge-case fragility.

## Design Principle: Stay Dumb, Stay Useful

After watching agents use the kernel in production, a clear principle emerged: **build tools for bookkeeping, not for thinking.**

The valuable ops eliminate mechanical work — precisely cutting 18 forms from a 2768-line file, rewriting requires across 10 namespaces, topologically sorting a dependency graph. The AI knows *what* to do but the mechanics are slow, error-prone, or burn context window. clj-surgeon does bookkeeping and manipulation.

The principle:

> **The tool does the mechanical work. The AI decides what to do. The compiler catches mistakes. The AI fixes what the compiler reports.**

clj-surgeon stays dumb. The AI stays smart (and is getting smarter all the time: the bitter lesson).

Also: I keep thinking the project name should be `clj-scalpel`, because you the programmer are the surgeon, and this is just a tool.  But it's just harder to type, I have `clj-surgeon` referenced everywhere already, so I guess I'm sticking with this name.  I guess the tool is the surgeon, and you're head of the medical ward!
