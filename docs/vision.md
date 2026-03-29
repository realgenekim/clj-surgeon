# ns-surgeon: A Babashka Scalpel for Clojure Namespaces

## The Problem

Clojure files grow. `state.clj` hits 2800 lines. `routes.clj` hits 2100. You
know you should split them. You know which functions belong together. But moving
a function between files is terrifying — did you get the closing paren? Did the
comment above belong to this function or the previous one? Did you miss a
`require`? So you add another `(declare ...)` and move on.

**AI assistants have the same problem, but worse.** When Claude Code opens a
2800-line file, it burns 2000+ tokens of context window just to *orient*. It
reads and re-reads, scrolling blindly through 200-line functions trying to find
boundaries. It guesses where forms start and end. It adds `declare` statements
as band-aids because reordering feels risky.

## The Solution

A babashka CLI that gives you **the skeleton of any Clojure namespace in EDN**:

```bash
$ bb -m ns-surgeon.core :op :outline :file src/writer/state.clj

{:ns writer.state
 :lines 2768
 :form-count 236
 :forms [{:type defonce, :name app-state, :line 42, :end-line 231}
         {:type defn, :name sync-draft!, :line 367, :end-line 408, :args "[{:keys [draft context ...]}]"}
         {:type defn, :name import-latest-fanout!, :line 1025, :end-line 1085}
         ...
         {:type defn, :name rebuild-ai-paragraphs!, :line 1856, :end-line 1905}]
 :forward-refs [{:name rebuild-ai-paragraphs!, :used-at 1052, :defined-at 1856, :gap 804}
                {:name transition!, :used-at 442, :defined-at 2655, :gap 2213}]}
```

And a **move command** that reorders forms safely:

```bash
$ bb -m ns-surgeon.core :op :mv :file src/writer/state.clj \
    :form rebuild-ai-paragraphs! :before import-latest-fanout! :dry-run true

{:ok true
 :plan {:form "rebuild-ai-paragraphs!"
        :from-line 1856
        :to-before "import-latest-fanout!"
        :to-line 1025
        :direction :up}}

$ bb -m ns-surgeon.core :op :mv :file src/writer/state.clj \
    :form rebuild-ai-paragraphs! :before import-latest-fanout!

{:ok true, :file "src/writer/state.clj", :form "rebuild-ai-paragraphs!",
 :moved-from 1856, :moved-to 1025, :lines-moved 50}
```

## Why Babashka?

- **rewrite-clj is built in** — form-level AST parsing, zero deps
- **5ms startup** — not 5 seconds like `clj -X`
- **cheshire is built in** — parse clj-kondo JSON output natively
- **Install with bbin** — `bbin install . --as ns-surgeon`, globally available
- **Or just run it** — `bb -m ns-surgeon.core :op :outline :file my.clj`

No JVM boot. No nREPL needed. No server running. Just a script that reads
Clojure and returns EDN.

## What It Does

### `:op :outline` — The X-Ray

Parses a Clojure file with rewrite-clj and returns every top-level form with:

- **`:line` / `:end-line`** — exact boundaries (where to cut)
- **`:type`** — `defn`, `def`, `defonce`, `>defn`, `declare`, etc.
- **`:name`** — the symbol, with metadata stripped
- **`:args`** — arglist string for function-like forms
- **`:comment-start`** — line where preceding comment header begins

Plus **forward reference detection** via clj-kondo: vars used before they're
defined in the same namespace.

### `:op :mv` — The Scalpel

Moves a named form before another named form in the same file:

1. Finds the form by name using rewrite-clj
2. Identifies its comment header (contiguous `;` lines above)
3. Cuts the form (including comments) from source location
4. Pastes it before the destination form
5. Returns what it did as EDN

With `:dry-run true`, shows the plan without modifying anything.

## The Workflow (Before/After)

### Before ns-surgeon

```
Human: "Move rebuild-ai-paragraphs! above import-latest-fanout! in state.clj"
Claude: *reads lines 1-50* ... *reads 1000-1100* ... *reads 1800-1900* ...
        *reads 1820-1880* ... "I think the form is lines 1830-1870?"
        *edits* ... *edits again* ... "Let me check if I got the parens right"
        *reads again* ... *fixes off-by-one* ...
```

4 blind reads, 500+ tokens of context burned, boundary guessing.

### After ns-surgeon

```
Human: "Move rebuild-ai-paragraphs! above import-latest-fanout! in state.clj"
Claude: *runs* bb -m ns-surgeon.core :op :mv :file state.clj \
              :form rebuild-ai-paragraphs! :before import-latest-fanout! :dry-run true
        "Plan: move lines 1856-1905 to before line 1025. Execute?"
Human: "yes"
Claude: *runs without :dry-run*
        "Done. Moved 50 lines. Running tests..."
```

1 command, 0 guessing, exact boundaries.

### For extraction (future :op :extract)

```
Human: "The distillery forms in views.clj should be their own namespace"
Claude: *runs* bb -m ns-surgeon.core :op :outline :file views.clj
        "I see 47 forms. 18 have 'distillery' in the name, lines 634-1890.
         Want me to extract them to writer.views.distillery?"
Human: "yes"
Claude: *uses outline data to Read exact line ranges*
        *creates new file with ns declaration*
        *deletes forms from original*
        *updates requires*
        "Done. 18 forms moved. Tests pass."
```

The outline gives Claude the structural awareness to do extraction with
existing tools (Read, Edit, Write). No special extraction engine needed.

## Design Principles

1. **EDN in, EDN out** — Clojure talking to Clojure. No JSON translation.
2. **rewrite-clj for parsing** — correct by construction. Handles reader
   macros, metadata, strings with parens, `#_` discard. The same parser
   clojure-lsp uses internally.
3. **clj-kondo for analysis** — var usages, forward refs, namespace deps.
   One shell command, rich JSON, already installed everywhere.
4. **Dry-run by default for mutations** — show the plan, then execute.
5. **Minimal scope** — outline + move. That's it. The human (or AI) knows
   what to move and where. The tool just makes it precise and safe.

## Future Ops (When We Need Them)

| Op | What | Effort |
|----|------|--------|
| `:extract` | Move forms to a new namespace file, update requires | Medium |
| `:deps` | Show intra-namespace call graph (which defn calls which) | Small (clj-kondo) |
| `:forward-refs` | Standalone forward-ref report with suggested reorder | Small (done, embedded in outline) |
| `:reorder` | Auto-topological-sort to eliminate all declares | Medium |

Each builds on `:outline`. Each is independently useful. None is needed today
— `:outline` and `:mv` cover 90% of the actual pain.

## Installation

```bash
# Option 1: Just run it (no install)
cd ~/src.local/ns-surgeon
bb -m ns-surgeon.core :op :outline :file /path/to/my/ns.clj

# Option 2: bbin (global install)
cd ~/src.local/ns-surgeon
bbin install . --as ns-surgeon
ns-surgeon :op :outline :file src/writer/state.clj

# Option 3: alias
alias nso='bb -m ns-surgeon.core --cp ~/src.local/ns-surgeon/src'
nso :op :outline :file state.clj
```

## What We Didn't Build (And Why)

- **No clojure-lsp dependency** — clojure-lsp's `move-form` is broken (issue
  #566, unfixed since 2021). We sidestep it entirely with rewrite-clj.
- **No MCP server** — a bb script is simpler, faster, and more composable.
  Any MCP client can call it via bash.
- **No cluster analysis** — the human can see the clusters in the outline.
- **No require fixup automation** — the AI knows what the requires should be.
- **No TypeScript** — Clojure all the way down.

## The Kernel

This is a **20-line idea** that solves a **daily pain point**:

```clojure
(let [zloc (z/of-file "state.clj")]
  (loop [zloc zloc, forms []]
    (if (nil? zloc) forms
      (recur (z/right zloc)
             (if (z/list? zloc)
               (conj forms {:name (some-> zloc z/down z/right z/string)
                            :line (:row (meta (z/node zloc)))
                            :end  (:end-row (meta (z/node zloc)))})
               forms)))))
```

Everything else is polish. The kernel is: **rewrite-clj gives you form
boundaries. Once you have boundaries, you can cut and paste precisely.**

That's it. That's ns-surgeon.
