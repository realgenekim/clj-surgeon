# Pitch: Remaining Operations and the Bitter Lesson Filter

## What the Safe-Refactor Skill Does Manually

The `/safe-refactor` skill documents a 3-hour manual grind for breaking apart
monolithic files. It was born from painful incidents — merge disasters, silent
regressions, 15-minute-per-domain extraction loops.

clj-surgeon already automates the analysis phase (`:ls`, `:deps`, `:ls-deps`,
`:ls-extract`, `:topo`, `:declares`). Here's how the remaining ops would
collapse the execution phase.

## The No-Brainer: `:extract`

The safe-refactor skill's Phase 3 is the bottleneck:

> 1. Create the target file with proper namespace and requires
> 2. Move functions from monolith to new file (cut, not copy)
> 3. Update monolith requires to import from new namespace
> 4. Update all callers

Today this is 15-20 minutes of manual editing per domain, 7 domains = 90
minutes of mechanical work. With `:extract`, it's one command per domain.

**Effort:** ~100 lines. Every piece exists — form cutting (`:mv`), require
inference (`required-aliases`), file creation (`:rename-ns`), topological
ordering (`:topo`).

See [extract-plan.md](extract-plan.md) for the detailed implementation plan.

## Probably Worth Building

### `:suggest-split` — Auto-Generate the Extraction Table

The skill's Phase 2: "Count lines and domains. Map function dependencies.
Identify shared helpers. Produce an extraction table."

Today the AI reads 2768 lines to do this. `:suggest-split` would use graph
partitioning on `:deps` data to find natural clusters.

**But:** The AI can already look at `:deps` output and `:ls` output and figure
out the clusters. It doesn't need a graph partitioning algorithm — it just
needs the data, which it already has. This might be bitter-lesson territory.

**Verdict:** Build only if we find ourselves running `:deps` + `:ls` + manual
clustering more than twice. Right now, build `:extract` and use the AI's
judgment for clustering.

## Bitter Lesson Territory (Don't Build Yet)

### `:diff` — Semantic Diff

Cool idea: show which functions were added/removed/moved instead of line-level
diff. Would catch the merge-revert disaster from the safe-refactor skill.

**But:** `git diff` + the AI reading it works. The AI can see "this function
was deleted" from a regular diff. Building a semantic diff engine is building
cleverness when the AI already has judgment. And `clj-kondo` catches the
compilation failures that matter.

### `:dead-code` — Unreferenced Forms

`clj-kondo` already warns about unused public vars. And the AI can run
`:deps` to check if anything references a form. Building a project-wide dead
code scanner duplicates what `clj-kondo` does better.

### `:find` — Structural Search

Elegant idea: match S-expression patterns instead of text patterns. But
`grep` + AI judgment handles 95% of cases. The AI knows that `(swap!
app-state ...)` is a pattern — it doesn't need a structural matcher to
find them. It can just grep and read.

### `:find-extractable-pure` — Auto-detect -tx Patterns

The AI already knows the -tx pattern (it's in CLAUDE.md). When it reads a
`swap!`-wrapping function, it knows the lambda is the pure core. Teaching a
tool to detect this is replacing AI judgment with a rigid pattern matcher.

## The Principle

**Build tools that eliminate mechanical work. Don't build tools that replace
judgment.**

The AI is good at: deciding what to extract, which cluster makes sense,
whether a function is dead, what a diff means.

The AI is bad at: precisely cutting 18 forms from a 2768-line file, computing
which `:require` entries a new namespace needs, moving forms without clipping
parens, updating cross-file requires without missing one.

`:extract` is pure mechanical work. Build it.
`:suggest-split` is judgment with data. Give the AI better data (`:deps`, `:ls`).
`:diff` is judgment on `git diff` output. The AI already has this.
