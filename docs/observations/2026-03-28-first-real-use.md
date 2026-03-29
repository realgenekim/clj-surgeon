# Ethnographic Observation: First Real Use of clj-surgeon

**Date:** 2026-03-28
**Context:** Claude Code session attempting to eliminate forward declares in `writer.state` (2768 lines, 7 declares, 6 removable)

## What Happened

### Phase 1: Tool discovery and naming (painful)

The AI loaded the skill but called `ns-surgeon` (old name). Failed. Then the bbin ghost binary at `~/.local/bin/ns-surgeon` interfered. Two failed attempts before the correct `clj-surgeon` command ran.

**Lesson:** Rename operations leave ghosts. The bbin install cached the old path. Even after uninstalling, the skill text still said `ns-surgeon` in example commands. The AI reads the skill literally.

### Phase 2: :declares works perfectly

```bash
clj-surgeon :op :declares :file src/writer/state.clj
# => 7 declares, 6 removable, 1 needed
```

Instant. Clear. The AI immediately knew which declares to target. **This is the tool working as designed.**

### Phase 3: :mv hits the declare-not-defn bug

The AI ran `:mv` to move `split-into-pills`. The tool matched the `(declare split-into-pills)` (line 1007) instead of `(defn split-into-pills ...)` (line 1927). Moved the one-line declare, not the 7-line function. Forward ref unchanged.

**Root cause:** `find-form` matched the first occurrence of the name. Fixed by adding `(not= "declare" first-child)` guard.

**Lesson:** When a file has both `(declare foo)` and `(defn foo ...)`, any name-based operation MUST skip declares. This is a universal rule for clj-surgeon, not just `:mv`.

### Phase 4: Manual editing hell (the real pain)

After fixing `:mv`, the AI successfully moved forms. But then it had to:

1. Read the file to find each `(declare ...)` line
2. Edit to delete it
3. Re-run `:declares` to verify
4. Repeat for each declare

This is **exactly the grep-read-edit loop we built clj-surgeon to eliminate**. The AI was using clj-surgeon for the structural move but falling back to manual editing for the delete. The irony was palpable.

**The AI also had to:**
- Pipe `:ls` output through `bb -e '...'` to filter forms by name
- Manually identify which form to use as the `:before` target
- Deal with cascading line-number shifts after each move

### Phase 5: The transition! trap

`transition!` (defined at line 2655) depends on `settle-editing-state` (defined at line 2423) but is called at line 442. Moving transition! to line 442 would create a NEW forward ref to settle-editing-state.

The AI realized it needed to move BOTH functions. Then discovered `log-event!` (used inside transition!) was also a forward ref. The dependency chain cascaded — moving one function required moving three.

**The AI ended up manually copy-pasting entire function blocks using the Edit tool** — exactly the workflow clj-surgeon was supposed to replace. But the tool only moves one form at a time and doesn't understand dependency chains.

## Missing Operations (Discovered Through Use)

### 1. `:fix-declares` — The Compound Operation

What the AI wanted to type:
```bash
clj-surgeon :op :fix-declares :file state.clj
```

What it should do:
1. Find removable declares
2. For each: compute the dependency chain (what else needs to move)
3. Move everything in the right order (leaves first)
4. Delete the stale declare lines
5. Return a summary

**This is the #1 missing operation.** Everything else works. This is the workflow gap.

### 2. `:mv` should auto-delete matching declare

When you move `split-into-pills` above its caller, the `(declare split-into-pills)` is dead. The tool knows this. It should offer to delete it (or do it by default with a flag).

### 3. Better `:before` target discovery

The AI had to manually find which form to use as the `:before` target by piping `:ls` through `bb` and filtering. The tool should accept:
```bash
clj-surgeon :op :mv :file state.clj :form split-into-pills :before-first-caller true
```
And figure out the right `:before` target automatically from the forward-ref data.

### 4. Dependency-aware bulk move

Moving `transition!` required also moving `settle-editing-state`, `log-wal!`, `wal-snapshot`, `wal-file`, `events-file`, and `log-event!`. The AI had to discover this chain manually.

```bash
clj-surgeon :op :mv-with-deps :file state.clj :form transition!
```
Should compute the full dependency chain and move everything in topological order.

## Quantitative Observations

| Metric | Value |
|--------|-------|
| Total declares to fix | 6 |
| Time spent | ~15 minutes of AI time |
| Tool invocations | ~20 (declares, ls, mv, ls again...) |
| Manual Edit tool calls | ~8 (delete declares, move blocks) |
| Times piped through `bb` | ~10 (filtering EDN output) |
| Bug found | 1 (declare-not-defn matching) |
| Successful structural moves | 4 |
| Manual copy-paste blocks | 2 (transition! + log-event! chains) |

## The Meta-Observation

**clj-surgeon's analysis is excellent. Its actions are too atomic.**

`:declares` instantly tells you what's removable. `:topo` gives the optimal ordering. `:deps` shows the dependency chain. But then you have to orchestrate 20 individual commands to execute what should be one operation.

The tool is like having a brilliant surgeon who can see exactly what needs to happen (X-ray) but operates with tweezers instead of a scalpel. The diagnosis is instant. The treatment is manual.

**The fix:** Compound operations that chain the atomic ones. `:fix-declares` is the first. `:extract` (from the vision doc) is the second. These are ~50-100 lines each because they compose existing primitives.

## What Worked Perfectly

- `:declares` — instant, accurate, actionable
- `:topo` — correctly identified the 4-form mutual recursion cycle
- `:ls` / `:outline` — form boundaries were always exact
- `:deps` — dependency analysis caught the transition! → settle-editing-state chain
- The declare-skip fix — once applied, `:mv` correctly targeted defns

## Phase 6: The Cascading Dependency Disaster

After moving `transition!` + `log-event!` + WAL block to the top of the file:

```
make runtests-once → "Unable to resolve symbol: app-state"
```

Because `log-event!` at line 28 references `app-state` which is `defonce`d at line 124. Fix: add `(declare app-state)`.

```
make runtests-once → "Unable to resolve symbol: active-project-file"
```

Because `save-active-project!` was moved above `active-project-file`. Fix: add another declare? Move another def? Each fix creates a new forward ref.

**This is Whac-A-Mole.** Moving one function up creates new forward refs to things that are defined below the new position. The AI is struggling because **`:mv` doesn't check that the moved form's dependencies are satisfied at the destination.** It just cuts and pastes.

**What `:mv` should do:** Before executing, verify that every symbol the form references is either:
1. Defined above the destination line, OR
2. A var that will be resolved at call-time (dynamic, via declare), OR
3. Defined in another namespace (qualified reference)

If any dependency is unresolved at the destination, return an error: "Cannot move X above line Y because it depends on Z (defined at line W). Move Z first, or use `:mv-with-deps`."

**What `:fix-declares` would avoid entirely:** By computing the full dependency chain upfront and moving in topological order, it would never create a new unresolved reference. Move leaves first, then their callers.

## Behavioral Pattern: The AI's Workflow

The AI followed a consistent pattern across all 6 declares:

```
1. :declares → identify target
2. :ls | bb filter → find first caller
3. :ls | bb filter → find the defn location
4. :mv :dry-run → preview
5. :mv → execute
6. :declares → verify
7. Edit → delete stale declare
8. Repeat
```

Steps 2, 3, 6, and 7 should not exist. The tool should handle them internally. That's the next iteration.
