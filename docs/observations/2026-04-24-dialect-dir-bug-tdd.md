# Ethnographic Observation: TDD Fix for Dialect-Directory Namespace Bug

**Date:** 2026-04-24
**Context:** GitHub issue #1 — `:extract` derives wrong namespace when project uses `src/{clj,cljs,cljc}` layout

## What Happened

### The Bug

`file-path->ns-name` had two independent defects:

1. **Hardcoded `/src/` split** — for dialect-split layouts (`src/clj/`, `src/cljs/`, `src/cljc/`), the dialect directory leaked into the namespace as a segment. `src/clj/myapp/core.clj` became `clj.myapp.core` instead of `myapp.core`.

2. **Regex `\.clj$` missed `.cljs` and `.cljc`** — the anchored regex only stripped `.clj`. ClojureScript and CLJC extensions survived as trailing namespace segments: `myapp.helpers.cljs`.

Both bugs only affected the *destination* path (source-file namespaces come from the `(ns ...)` form via `outline`).

### The Fix Process (strict TDD)

**Red phase — safety net first:**
Before touching any production code, added 5 pure unit tests pinning the *current correct behavior* of `file-path->ns-name` for standard `src/` layouts. These are the regression guardrails: if we break the happy path, they catch it immediately.

Then added 9 failing tests covering:
- `.cljs` and `.cljc` extension stripping (standard layout)
- Dialect directory stripping with explicit `source-paths` arg
- Maven-style `src/main/clojure/` layout
- Longest-prefix-wins when multiple source roots match
- Integration test: `plan` with dialect-split temp project and `:source-paths`

**Green phase — minimal implementation:**
- Added `source-paths-from-deps-edn` — reads `:paths` and alias `:extra-paths` from `deps.edn`
- Made `file-path->ns-name` a multi-arity fn: `(path)` and `(path source-paths)`
  - Tries all source paths, picks longest matching prefix (most specific root wins)
  - Falls back to old `/src/` splitting behavior as last resort
  - Fixed regex to `\.clj[sc]?$`
- Threaded `:source-paths` through `plan` as optional key (keeps plan pure when testing with explicit paths)
- `execute!` already delegates to `plan` with full opts map — no change needed

**Result:** 62 tests, 227 assertions, 0 failures.

### Design Decision: Purity

The issue author noted `plan` is currently pure. We preserved this by:
- Making `source-paths` an explicit parameter that tests pass directly (pure)
- The `deps.edn` read happens only as a default when no paths are supplied
- `plan` itself does no I/O beyond what it already did (`slurp file`, `outline`)

## What Went Well

- **TDD discipline paid off.** Writing safety-net tests first meant we could refactor `file-path->ns-name` with confidence that the standard layout still worked.
- **Pure unit tests are fast and readable.** No temp directories needed for the core `file-path->ns-name` tests — just string-in, string-out. Kent Beck would approve.
- **The multi-arity pattern** (`[path]` / `[path source-paths]`) kept backward compatibility while enabling the fix. No existing callers needed updating.

## Observations About the Tool's Test Architecture

The existing extract tests all use temp filesystem projects (`create-temp-project!` / `delete-recursive!`). This is appropriate for integration tests of `plan` and `execute!`, but `file-path->ns-name` is a pure string transformation — it didn't need filesystem scaffolding. The new pure unit tests demonstrate a complementary testing style: fast, focused, no cleanup.

## Metrics

- Lines changed in production code: ~35 (mostly the new `file-path->ns-name`)
- Lines added in tests: ~65
- Time from bug confirmation to all-green: ~10 minutes
- Existing test count preserved: all 58 original tests still pass
- New tests added: 4 (1 regression safety, 1 extension fix, 1 dialect dirs, 1 integration)
