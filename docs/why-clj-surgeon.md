# Why clj-surgeon Exists

## The Pain

Renaming a namespace in Clojure is a 45-minute ordeal. You know the drill:

1. Grep for every reference to the old name
2. Read each file to understand the context
3. Manually edit the ns form
4. Manually edit every `:require` entry
5. Rename the directory (ns-to-path conversion)
6. Move the files
7. Hope you didn't miss one
8. Run tests. Fix the one you missed. Repeat.

Even with Claude Code, it's agonizing to watch. The AI does the same thing
you'd do — grep, read 2000 lines, squint at the context, copy-paste, grep
again. It takes 45 minutes of watching it fumble through the same mechanical
process. And you're sitting there thinking: "the machine knows what a namespace
form looks like. Why is it reading the whole file to find it?"

clojure-lsp's `move-form` was supposed to solve this. It's been broken since
2021 (issue #566). The maintainers say a half-working version is worse than
none. They're right.

## The Insight

**Clojure code is data.** A namespace declaration isn't text to grep — it's
a list you can walk. A `:require` entry isn't a string to sed — it's a
vector with symbols you can replace.

```clojure
;; This is text:
(ns ns-surgeon.core
  (:require [ns-surgeon.outline :as outline]))

;; But to rewrite-clj, it's THIS:
[:list
  [:token ns]
  [:token ns-surgeon.core]    ;; <-- rename this symbol node
  [:list
    [:keyword :require]
    [:vector
      [:token ns-surgeon.outline]  ;; <-- and this one
      [:keyword :as]
      [:token outline]]]]          ;; <-- this stays
```

The rename is a tree walk: find symbol nodes that match the old prefix,
replace them with the new prefix. Not text replacement (which hits strings
and comments). Not regex (which can't parse nested structure). **Structural
replacement on the AST.** Correct by construction.

## What It Took

clj-surgeon's `:rename-ns` is ~170 lines of Clojure. It:

1. Finds all .clj files under a root directory
2. Parses each with rewrite-clj (the same parser clojure-lsp uses)
3. Walks the ns form — renames the declaration and every matching `:require`
4. Computes file moves (namespace→path convention)
5. Flags non-Clojure files that reference the old name
6. Returns the plan as EDN (dry-run by default)
7. Executes: writes renamed files, creates new directories

The dogfood test: **clj-surgeon renamed itself from `ns-surgeon` to
`clj-surgeon`.** 10 .clj files, 10 file moves, all namespace declarations
and requires updated, all 28 tests still passing.

Time: **under 1 second.** Not 45 minutes.

## Before / After

### Before clj-surgeon (Claude Code + grep)

```
Human: "rename ns-surgeon to clj-surgeon"
Claude: let me search for all references...
        *grep ns-surgeon* — 47 matches across 14 files
        *read file 1* (200 lines of context burned)
        *edit ns form* — hope I got the right symbol
        *read file 2* ...
        *edit require* — did I break the formatting?
        *grep again* — 43 matches remaining
        ... (repeat 12 more times) ...
        *mkdir clj_surgeon*
        *move files one by one*
        *run tests* — 3 failures, missed a require
        *fix, fix, fix*
        "Done!" (45 minutes later)
```

### After clj-surgeon

```
Human: "rename ns-surgeon to clj-surgeon"
Claude: *runs* clj-surgeon :op :rename-ns :from ns-surgeon :to clj-surgeon :root .
        "Plan: 10 files to update, 10 to move, 4 non-clj to review. Execute?"
Human: "yes"
Claude: *runs* clj-surgeon :op :rename-ns! :from ns-surgeon :to clj-surgeon :root .
        "Done. 12 actions. 4 non-clj files flagged for review."
        *manually updates Makefile, bb.edn, docs, script*
        *runs tests* — 28 tests, 116 assertions, 0 failures
        "Renamed. All green."
```

Time: **under 2 minutes** including the manual non-clj fixes and test run.

## Why babashka?

- **rewrite-clj is built in** — zero dependencies for AST parsing
- **cheshire is built in** — parse clj-kondo JSON for analysis
- **5ms startup** — call it 20 times in a session, no penalty
- **Clojure reading Clojure** — no translation layer, no impedance mismatch

## Why Not clojure-lsp?

clojure-lsp's `move-form` (issue #566):
- Doesn't add missing requires to destination
- **Deletes aliases still in use** by other vars
- Moving private forms fails silently
- Last meaningful fix: January 2023

We sidestep it entirely. rewrite-clj gives us the same parser clojure-lsp
uses internally. We just do the operations ourselves — correctly.

## The Homoiconicity Dividend

In Java, a namespace rename means parsing imports (different syntax per
language version), resolving modules (classpath, module-info.java, package
declarations), handling star imports, static imports, and reflection.
IntelliJ's rename refactoring is 50,000+ lines of code.

In Clojure, it's a tree walk. Because the code is already a data structure.
The parser is `read`. The AST is a list. A rename is `(z/replace node
(symbol new-name))`.

170 lines. That's the homoiconicity dividend.

## What's Next

clj-surgeon today:

| Op | What | Lines |
|---|---|---|
| `:outline` | Form boundaries + forward refs | outline.clj + forward_refs.clj |
| `:mv` | Reorder forms within a file | move.clj |
| `:declares` | Which declares are removable? | core.clj (uses analyze) |
| `:deps` | Intra-namespace call graph | analyze.clj |
| `:topo` | Optimal form ordering | analyze.clj |
| `:closure` | Minimal extractable unit | analyze.clj |
| `:rename-ns` | Rename namespace prefix (dry-run) | rename.clj |
| `:rename-ns!` | Execute the rename | rename.clj |

What we haven't built but could:

- `:extract` — move forms to a new namespace (closure + rename + file creation)
- `:dead-code` — unreferenced private forms across a project
- `:reorder` — auto-reorder entire file to eliminate all removable declares

Each is 50-100 lines of Clojure, standing on the same rewrite-clj foundation.
The hard part was the insight. The code is trivial because the language makes
it trivial.
