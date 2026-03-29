# clj-surgeon: Vision & Roadmap

## What We Proved Today (2026-03-28)

In one session, starting from zero, we built a babashka CLI that:

1. **Outlines a 2768-line namespace in 200ms** — every form with exact line boundaries
2. **Detects that 6 of 7 declares are removable** — topological sort separates band-aids from genuine mutual recursion
3. **Moves forms structurally** — comment headers travel with their form, parens always balanced, declares skipped
4. **Renamed itself** — `ns-surgeon` → `clj-surgeon`, 10 files, every ns declaration and `:require` entry updated by walking the AST. Not grep. Not sed. Symbol node replacement.
5. **Maps the intra-namespace call graph** — `sync-draft!` depends on `{log-event! app-state sync-draft-tx}`, computed by walking the AST subtree
6. **Infers requires for extraction** — if you pulled `sync-draft!` into a new namespace, it needs `[clojure.string :as str]` and `[taoensso.timbre :as log]`
7. **Finds the minimal extractable unit** — the closure of a form plus all private helpers only it uses

**30 tests. 126 assertions. ~500 lines of Clojure. Zero dependencies beyond babashka.**

All of this is possible because **Clojure code is data**. The parser is `read`. The AST is a list. A rename is `(z/replace node (symbol new-name))`. In Java this would be 50,000 lines of IntelliJ plugin code.

---

## What's Next: Beyond Line-Level Operations

Everything we built today operates on **forms** — top-level `(defn ...)` blocks. But rewrite-clj gives us the full AST. We can go deeper.

### :extract — Move forms to a new namespace (the big one)

We have all the pieces:
- `:closure` knows the minimal set of forms to extract
- `required-aliases` knows what `:require` entries the extracted forms need
- `:rename-ns` knows how to create files and update requires across a project
- `:mv` knows how to cut forms precisely

Wire them together:

```bash
clj-surgeon :op :extract \
  :file src/writer/state.clj \
  :forms '[distillery-add-ai-response! enter-distillery! rebuild-ai-paragraphs!]' \
  :to src/writer/state/distillery.clj \
  :dry-run true
```

Returns: the new file content, the updated old file, all require changes across the project. Execute and it's done.

**Effort: ~100 lines.** The hard parts (closure computation, require inference, file writing) already exist.

### :reorder — Auto-eliminate all removable declares in one shot

We have topological sort. We have `:mv`. Instead of moving forms one at a time:

```bash
clj-surgeon :op :reorder :file src/writer/state.clj :dry-run true
```

Computes the optimal form ordering, shows which forms move where, and how many declares become deletable. Execute and the entire file is reordered — every form after its dependencies, zero unnecessary declares.

**Effort: ~50 lines** on top of existing topo sort + move.

### :dead-code — Find unreferenced forms across an entire project

Right now `unreferenced-forms` works within a single file. Scale it:

```bash
clj-surgeon :op :dead-code :root src/
```

For every private function in every namespace: is it called by any form in any file? If not, it's dead. clj-kondo's analysis gives us cross-file var-usages for free.

**Effort: ~40 lines** (shell out to clj-kondo, cross-reference with outlines).

---

## The Exotic Stuff: What Homoiconicity Uniquely Enables

These are things that are **impossible or prohibitively expensive** in non-homoiconic languages. In Clojure, they're tree walks.

### Structural Search (not grep — pattern matching on the AST)

```bash
# Find every (swap! app-state (fn [st] ...)) pattern
clj-surgeon :op :find :file state.clj :pattern '(swap! app-state (fn [_] ...))'

# Find every defn that calls log-event! but not transition!
clj-surgeon :op :find :file state.clj :calls 'log-event!' :not-calls 'transition!'
```

Grep finds text. This finds **structure**. `(swap! app-state f)` and `(swap!  app-state  f)` and `(swap! app-state\n  f)` are all the same AST shape. Grep would need three different patterns. The zipper matches all three with one walk.

This is the Clojure equivalent of `ast-grep` or Semgrep — but it's 20 lines because S-expressions ARE the AST.

### Extract Pure from Impure (the -tx pattern, automated)

You spent weeks manually extracting `-tx` pure functions from `swap!`-wrapping mutators. The AST can detect the pattern:

```bash
clj-surgeon :op :find-extractable-pure :file state.clj
```

Walks every `defn` ending in `!`. Finds `(swap! app-state ...)` calls. The lambda passed to `swap!` is the pure core. Returns:

```clojure
[{:mutator "save-session!"
  :line 2400
  :swap-body "(fn [st] (assoc-in st [:session :saved] true))"
  :suggested-name "save-session-tx"
  :deps #{:session}}
 ...]
```

The tool sees the structure because the structure is data. A `swap!` call is a list with `swap!` as the first element and a function as the third. The function's body is the pure logic. Extract it, name it, wire it up.

### Macro Expansion Awareness

`>defn` isn't `defn`. It has a spec vector between args and body:

```clojure
(>defn my-fn [x y] [int? string? => map?] {:x x :y y})
;;                  ^^^^^^^^^^^^^^^^^^^^^^^^ this is the spec, not the body
```

Because we're walking the AST (not regex-matching), we can teach clj-surgeon about project-specific macros:

```clojure
;; In a config file:
{:macro-forms {">defn"  {:name 1 :args 2 :spec 3 :body 4}
               "defcache" {:name 1 :opts 2 :body 3}}}
```

Now `:outline` shows arglists correctly for `>defn`. `:extract` handles the spec vector. `:find` matches inside the body, not the spec. **The tool understands your macros because you told it the shape.**

### Semantic Diff (not line diff — form diff)

```bash
clj-surgeon :op :diff :file state.clj :against main
```

Instead of "lines 1830-1870 changed", returns:

```clojure
{:added [{:name "new-helper" :type defn :line 450}]
 :removed [{:name "old-util" :type defn- :line 890}]
 :moved [{:name "rebuild-ai-paragraphs!" :from 1830 :to 1018}]
 :modified [{:name "sync-draft!" :line 367
             :changes [:body-changed :arglist-same]}]}
```

Git diff shows text changes. This shows **semantic changes**: which functions were added, removed, moved, modified. A move isn't a deletion + addition — it's the same form at a different location. The AST makes this trivial to detect (same name + same body = moved).

### Dependency-Aware Splitting

```bash
clj-surgeon :op :suggest-split :file state.clj
```

Uses the intra-ns call graph to find **natural partition boundaries** — clusters of forms that are tightly connected internally but loosely connected externally. Graph partitioning algorithms (even simple connected-component analysis) on the adjacency list from `:deps`:

```clojure
{:clusters
 [{:name "distillery"
   :forms ["distillery-add-ai-response!" "enter-distillery!" ...]
   :internal-edges 23
   :external-edges 4
   :suggested-ns "writer.state.distillery"}
  {:name "session"
   :forms ["save-session!" "load-session!" "normalize-state" ...]
   :internal-edges 12
   :external-edges 3
   :suggested-ns "writer.state.session"}]
 :shared-utilities ["get-state" "app-state" "log-event!"]
 :extraction-order ["distillery" "session"]}  ;; least coupled first
```

The call graph already contains this information. No AI needed — just graph algorithms on data we already compute.

### The Holy Grail: Safe Auto-Refactor

Combine everything:

```bash
clj-surgeon :op :refactor :file state.clj \
  :strategy :split-by-cluster :dry-run true
```

1. `:suggest-split` identifies clusters
2. `:closure` computes minimal extractable units
3. `required-aliases` infers requires
4. `:extract` creates new namespace files
5. `:rename-ns` updates all cross-project references
6. `:reorder` eliminates declares in each new file
7. `clj-kondo --lint` validates everything compiles
8. Returns the complete plan as EDN

**One command to split a 2800-line monolith into well-structured modules.** Dry-run shows every file that would be created and modified. Execute and it's done.

Is this ambitious? Yes. Is it feasible? **Every component already exists or is <100 lines from existing.** The homoiconicity dividend compounds — each new operation builds on the tree-walking primitives we already have.

---

## The Meta-Insight

The traditional refactoring toolchain:

```
Source → Parser → AST → Analysis → Transform → Printer → Source
         ^^^^                                    ^^^^^^^
         50K lines (language-specific)           50K lines (preserve formatting)
```

The Clojure refactoring toolchain:

```
Source → rewrite-clj → Zipper → Tree walk → Zipper → rewrite-clj → Source
         ^^^^^^^^^^                                    ^^^^^^^^^^
         already built                                 already built
         (built into babashka)                         (preserves formatting)
```

**The parser and printer are free.** They're built into the language. The analysis is a `filter` on a `map`. The transform is a `z/replace` on a zipper. Everything between "read the file" and "write the file" is 10-50 lines of Clojure per operation.

That's why clj-surgeon is 500 lines and does what 50,000-line IDE plugins do. Not because we're smarter. Because Clojure is homoiconic.

---

## The Bitter Lesson: Build Tools for Bookkeeping, Not for Thinking

After building 13 ops and using them on a real codebase, a clear pattern emerged:
**the valuable ops eliminate mechanical work. The wasteful ops replace judgment.**

### The Test: "Is This Bookkeeping or Judgment?"

**Bookkeeping** = the AI knows what to do but the mechanics are slow, error-prone,
or burn context window. These are the ops worth building:

- `:ls` — reading a 2768-line file to find form boundaries (bookkeeping)
- `:extract!` — cutting 18 forms, writing a new file, fixing requires (bookkeeping)
- `:fix-declares!` — moving forms in topo order, deleting stale declares (bookkeeping)
- `:rename-ns!` — updating ns declarations and requires across 10 files (bookkeeping)
- `:mv` — precisely cutting a form with its comment header (bookkeeping)

**Judgment** = the AI already has the information and can make the decision.
Building a tool replaces flexible reasoning with rigid pattern matching:

- `:diff` — the AI can read `git diff` and understand what changed
- `:dead-code` — `clj-kondo` already warns; the AI can grep and check
- `:suggest-split` — the AI can look at `:deps` + `:ls` and see the clusters
- `:find-extractable-pure` — the AI knows the -tx pattern (it's in CLAUDE.md)

### The -tx Extraction Example

Consider extracting a pure `-tx` function from a `swap!` mutator:

```clojure
;; BEFORE: pure logic buried in swap!
(defn archive-critique-pill! [idx]
  (swap! app-state
    (fn [st]
      (let [pills (get-in st [:distillery :lanes 0 :pills])
            pill (nth pills idx nil)]
        (when pill
          (-> st
              (update-in [:distillery :lanes 0 :pills] ...)
              (update-in [:distillery :archived] (fnil conj []) pill)
              (update-in [:distillery :focus-index] ...)))))))

;; AFTER: pure core extracted
(defn archive-critique-pill-tx [st idx]
  (let [pills (get-in st [:distillery :lanes 0 :pills])
        pill (nth pills idx nil)]
    (if pill
      (-> st ...)
      st)))  ;; when → if returning st (pure fns don't return nil)

(defn archive-critique-pill! [idx]
  (swap! app-state archive-critique-pill-tx idx))
```

Could a `:find-extractable-pure` op detect this pattern? Yes — find every
`(swap! app-state (fn [st] ...))` and extract the lambda. ~40 lines of code.

But the **judgment** is everything:
- `when` must become `if` returning `st` (pure functions shouldn't return nil)
- Should `idx` be a param or should the caller pass the pill directly?
- Is the `focus-index` clamping logic correct? Should the test verify the edge case?
- Does the extracted function need a Guardrails `>defn` spec?

The detection (finding the pattern) is trivially useful. The extraction is
30 seconds of editing guided by architectural judgment. A tool that automates
the extraction would get the mechanics right but miss every judgment call.

**Where clj-surgeon DOES help:** After you've manually extracted 20 `-tx`
functions (judgment work), you want to move them all to `writer.state.transforms`
(bookkeeping). That's `:extract!`.

### The Principle

> **Build tools that eliminate mechanical work. Don't build tools that
> replace judgment. The AI is good at judgment. The AI is bad at precisely
> cutting 18 forms from a 2768-line file.**

The compiler catches what the tool doesn't detect. The AI fixes what the
compiler reports. The tool does the bookkeeping in between.

---

## Current State (13 ops, 58 tests)

| Op | Status | Category |
|---|---|---|
| `:ls` / `:outline` | **DONE** | Visibility |
| `:ls-deps` | **DONE** | Visibility |
| `:ls-extract` | **DONE** | Visibility |
| `:deps` | **DONE** | Visibility |
| `:topo` | **DONE** | Visibility |
| `:declares` | **DONE** | Visibility |
| `:mv` | **DONE** | Action |
| `:fix-declares!` | **DONE** | Compound action |
| `:extract!` | **DONE** | Compound action |
| `:rename-ns!` | **DONE** | Compound action |
| `:dead-code` | BITTER LESSON | clj-kondo does this; AI can grep |
| `:find` | BITTER LESSON | AI + grep handles 95% of cases |
| `:suggest-split` | BITTER LESSON | AI + `:deps` + `:ls` = judgment |
| `:diff` | BITTER LESSON | AI + `git diff` = judgment |
| `:find-extractable-pure` | BITTER LESSON | Detection useful, extraction is judgment |
| `:refactor` | BITTER LESSON | Compound ops + AI judgment = this already |
