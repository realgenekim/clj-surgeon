# :extract Implementation Plan

## What It Does

Move named forms from one namespace to a new namespace file.

```bash
# Plan
clj-surgeon :op :extract :file src/writer/state.clj \
  :forms '[distillery-add-ai-response! enter-distillery! rebuild-ai-paragraphs!]' \
  :to src/writer/state/distillery.clj

# Execute
clj-surgeon :op :extract! :file src/writer/state.clj \
  :forms '[...]' :to src/writer/state/distillery.clj
```

## The Algorithm (MVP)

### Step 1: Parse source, find forms
Use `:ls` to get form boundaries. Match requested names. Error if any not found.

### Step 2: Copy source ns form as template for new file
**This is the key insight.** Don't try to compute requires from scratch — copy
the ENTIRE `(ns ...)` form from the source file, change only the namespace
name. The new file gets all the same requires, imports, and refers.

Yes, it'll have extra requires. That's fine — `clean-ns` strips them later.
Over-include is safe. Under-include breaks compilation.

Why this works:
- Every `:require` the extracted forms need is already in the source ns form
- Every `:import` (Java classes like `Instant`, `UUID`) is already there
- Every `:refer` is already there
- Macro requires (`>defn` from guardrails) are already there

Why the "compute from scratch" approach is risky:
- `required-aliases` only finds namespace-qualified symbols (`str/join`)
- It misses bare `:refer`'d symbols, imported Java classes, macros
- Each miss is a compilation failure the human has to debug

### Step 3: Topologically sort the extracted forms
Within the extracted set, order forms so no forward refs. Use `:topo` data
filtered to just the extracted names.

### Step 4: Write the new file
```clojure
(ns writer.state.distillery           ;; new ns name (derived from file path)
  (:require [writer.state :as state]  ;; require the SOURCE (for non-extracted deps)
            [clojure.string :as str]  ;; copied from source ns form
            ...))                     ;; all other requires from source

(defn distillery-add-ai-response! ...) ;; extracted forms in topo order
(defn enter-distillery! ...)
...
```

Note: we add a require for the SOURCE namespace. The extracted forms likely
call functions that stay in the source (e.g., `app-state`, `log-event!`).

### Step 5: Remove forms from source file
Cut from bottom to top (highest line number first) to avoid shifting line
numbers. Same approach as `:fix-declares!`.

### Step 6: Add require for new namespace in source file
The source file may still have forms that call the extracted functions.
Add `[writer.state.distillery :as distillery]` to its ns form.

### Step 7: Report callers that need updating
Scan all .clj files for requires of the source namespace. If any use
`:refer` with an extracted function name, or if any call the extracted
functions via the source alias, report them.

**Don't auto-update callers.** Report and let the AI decide. This limits
blast radius to 2 files (source + new) and leaves cross-project changes
to human judgment.

### Step 8: Validate
Run `clj-kondo --lint` on both the new file and the source file.
Report any errors in the plan output.

## Risks and Mitigations

### Risk 1: Circular requires
If extracted forms call source functions AND source functions call extracted
forms, we get a circular require: source → new → source.

**Detection:** Check if any non-extracted form in the source depends on an
extracted form. If yes, warn: "Circular: source calls extracted form X.
You'll need a delegation alias or a third namespace."

**Mitigation:** The plan phase flags this. Human decides whether to proceed
(with a delegation alias) or adjust the form list.

**Likelihood:** Medium. Common when extracting from a god file where
everything calls everything.

### Risk 2: Missing requires in new file
The "copy entire ns form" approach prevents this for the initial extraction.
But if someone later runs `clean-ns` and the extracted form only SOMETIMES
calls a function (conditional branch), `clean-ns` might strip it.

**Mitigation:** We don't run `clean-ns` automatically. We just note "run
clean-ns when ready" in the output. The human decides when.

**Likelihood:** Low. Only matters after manual cleanup.

### Risk 3: Multi-form removal shifts line numbers
Removing form A shifts the line numbers of form B below it.

**Mitigation:** Remove from bottom to top. Proven approach in
`:fix-declares!` — same code pattern.

**Likelihood:** Low (already solved).

### Risk 4: Extracted forms reference each other via unqualified names
If `enter-distillery!` calls `rebuild-ai-paragraphs!` by bare name, and
both are extracted, the calls still work (they're in the same new namespace).
No issue.

But if `enter-distillery!` calls `app-state` (which stays in source),
it needs to use `state/app-state` in the new file. If the source used
bare `app-state` (because it's in the same ns), extraction breaks it.

**Detection:** Find symbols in extracted forms that reference non-extracted
forms using bare (unqualified) names. These are the `def`s and `defonce`s
at the top of the source file — `app-state`, `*io-enabled*`, etc.

**Mitigation:** The plan flags these: "Extracted forms reference these
unqualified symbols that stay in source: app-state, *io-enabled*.
You'll need to qualify them after extraction."

**Likelihood:** HIGH. This is the most common issue. Every extracted form
that reads `@app-state` will need to change to `@state/app-state`.

**Alternative mitigation:** Don't extract forms that reference the source
atom directly. Only extract "pure" forms or forms that take state as a
parameter. This is actually better architecture — it's the -tx pattern.

### Risk 5: Alias collision
If the source file already has `(:require [writer.state :as state])` and
we add `(:require [writer.state.distillery :as distillery])`, the aliases
could collide if another file already uses `:as distillery` for something
else.

**Mitigation:** Use the last segment of the new namespace as the default
alias. Check for collisions in the plan phase. Let human override.

**Likelihood:** Low.

## What We Don't Handle (Explicitly Out of Scope)

1. **Updating callers** — Report which files need changes, don't change them
2. **Running `clean-ns`** — Note it in output, don't run it
3. **Qualifying bare references** — Flag them, don't rewrite them
4. **Moving tests** — Report which test files reference extracted forms
5. **Creating delegation aliases** — The safe-refactor skill says don't

## Implementation Structure

```
src/clj_surgeon/extract.clj

(defn plan [{:keys [file forms to]}]
  ;; Returns: {:new-file-content :source-changes :callers-to-update
  ;;           :bare-refs-to-qualify :circular-deps :kondo-errors}
  ...)

(defn execute! [{:keys [file forms to]}]
  ;; 1. Write new file
  ;; 2. Remove forms from source
  ;; 3. Add require to source ns form
  ;; 4. Return report
  ...)
```

Estimated: ~120 lines. Uses: `outline.clj` (form boundaries), `analyze.clj`
(deps, topo sort, required-aliases), rewrite-clj (ns form manipulation).

## The Honest Assessment

**The easy parts (~60% of the work):**
- Finding and cutting forms (proven in `:mv` and `:fix-declares!`)
- Writing the new file (string concatenation)
- Topological ordering (proven in `:topo`)
- Removing forms from source (proven in `:fix-declares!`)

**The medium parts (~30% of the work):**
- Manipulating the ns form (add require to source, copy+modify for new file)
- Detecting circular deps
- Detecting bare unqualified references

**The hard part (~10% but high risk):**
- Risk 4 (bare references to `app-state`, `*io-enabled*`). This is the
  thing most likely to cause "extraction compiles but fails at runtime."
  The mitigation (flag and report) is adequate but not automatic.

## Decision

Build it. The easy+medium parts cover the mechanical pain. The hard part
(bare refs) is a reporting problem, not a solving problem — flag it and
let the AI fix the qualifications. That's judgment work the AI is good at.

The safe-refactor skill took 90 minutes for 7 extractions. With `:extract`,
the mechanical part drops to ~10 minutes. The remaining 35 minutes is all
judgment: reviewing plans, qualifying bare refs, running tests, committing.
