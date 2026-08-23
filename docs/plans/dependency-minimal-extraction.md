# Dependency-Minimal Namespace Extraction

**Status:** Implemented and verified 2026-08-10
**Motivating issue:** `clj-surgeon-to4`

## Outcome

`:extract` and `:extract!` compile a destination namespace header from the
forms that move. They do not copy the source namespace's complete `:require`
clause. The source receives a destination require only when a remaining
top-level form refers to a moved Var.

The motivating program has 17 source requires, six dependencies used by the
extracted closure, an existing `schedule` alias, and no remaining source
caller. The correct result has six destination dependencies and no new source
require. It must load on the program classpath and remain reversible.

## Bitter-Lesson Boundary

The caller chooses the forms, destination, architecture, and migration order.
The kernel performs mechanical work only:

- compute free-symbol dependencies of the selected forms;
- retain matching source require entries;
- identify remaining source forms that refer to moved Vars;
- allocate a deterministic non-colliding alias when a source require is
  necessary;
- compile, parse, write, verify, and reverse both files.

The kernel does not infer module boundaries, rename moved Vars, redesign a
public API, or decide whether a side-effect-only dependency is architecturally
necessary.

## Public Contract

The existing commands remain unchanged:

```bash
clj-surgeon :op :extract :file src/app.clj \
  :forms '[helper render]' :to src/app/render.clj

clj-surgeon :op :extract! :file src/app.clj \
  :forms '[helper render]' :to src/app/render.clj \
  :receipt-out /tmp/app-render-extraction.edn
```

Planning returns these additional facts:

- `:target-requires`: retained destination require namespaces;
- `:omitted-target-requires`: source require namespaces proved unused;
- `:remaining-source-callers`: remaining owners and moved Vars that they use;
- `:source-referred-forms`: sorted moved Vars that the source must refer;
- `:source-require-added`: `true` only when that set is non-empty.

When remaining callers exist, the source entry is:

```clojure
[target.ns :as collision-free-alias :refer [moved-a moved-b]]
```

The `:refer` list preserves existing unqualified source calls. The alias gives
future edits one stable qualifier. The base alias is the destination's final
namespace segment. If that alias belongs to another namespace, suffixes start
at `2` and increase until unused: `schedule2`, `schedule3`, and so on.

When no remaining caller exists, the source bytes after removed ranges remain
unchanged. No destination require, blank clause, or log entry is added.

## Supported Require Proof

The first implementation minimizes direct vector libspecs in a `.clj`,
`.cljs`, or `.cljc` `:require` clause. Reader-conditional entries remain an
explicit refusal.

A libspec is required when at least one condition is true:

- an extracted free symbol uses its `:as` or `:as-alias` prefix;
- an extracted free symbol uses the complete namespace as a qualifier;
- an extracted free symbol is named by its `:refer` vector;
- an extracted free symbol is a target name in its `:rename` map;
- it uses `:refer :all`, which is retained conservatively.

An unused aliased or explicitly referred libspec is omitted. A direct libspec
whose use cannot be proved, such as a side-effect-only require, must not be
silently deleted. The operation refuses with
`:unsupported-require-minimization` and names the entry. Reader-conditional
and prefix-list require shapes also refuse in this increment. The source and
target remain unchanged.

These refusals are preferable to copying dependency noise or claiming that a
syntactic scan proved runtime side effects unnecessary. A later increment can
add an explicit retention decision without weakening this contract.

## Safety Invariants

- Read and hash the source once before planning.
- Compute target dependencies from free symbols, not raw text.
- Exclude comments, strings, quoted data, local bindings, and discarded forms
  from dependency evidence.
- Preserve retained require entries exactly.
- Preserve every unrelated source byte.
- Do not add a source require when no remaining source owner uses a moved Var.
- Never bind an alias that already names another namespace.
- Sort generated `:refer` symbols and alias suffix selection deterministically.
- Parse both complete candidates before the first write.
- Refusal leaves the source unchanged and the target absent.
- Successful application verifies read-back hashes and publishes the existing
  guarded inverse receipt.
- Undo restores the exact original source and removes the exact created target.

## Implementation Shape

Add a small pure namespace-header compiler. Its input is source namespace text,
selected form text, moved Var names, remaining dependency records, and the
destination namespace. Its output contains the destination header, optional
source require specification, retained/omitted dependency evidence, and any
structured refusal.

Keep filesystem discovery, snapshot reads, atomic writes, rollback, and receipt
publication in `clj-surgeon.extract`. Reuse `analyze/free-symbols-in-form` and
`analyze/intra-ns-deps`. Extend the require insertion primitive to accept an
optional sorted `:refer` vector while preserving its existing arity.

This extraction is also the first coherence boundary from the overnight
architecture review: dependency selection becomes a pure compiler instead of
remaining embedded in the 500-line extraction shell.

## Test Plan

### Pure behavior matrix

| Dimension | Cases |
|---|---|
| Alias use | used alias retained; unused alias omitted; full namespace qualifier retained |
| Unqualified use | used `:refer` retained; unused `:refer` omitted; renamed refer target retained |
| Conservative use | `:refer :all` retained; side-effect-only entry refuses |
| Syntax | comments and strings do not retain a dependency; quoted symbols do not retain it; locals do not retain it |
| Source callers | none adds no require; one caller refers one Var; several callers deduplicate and sort Vars |
| Alias allocation | base free; base collision; several suffix collisions; existing target binding |
| Candidate state | valid candidates; invalid source; invalid target; unsupported require shape |
| Mutation | dry-run; execute; stale source; existing target; rollback; undo; drifted undo |

### Field-failure regression

Use a minimized fixture derived from
`cfp-scheduler-killer.views` at the `clj-surgeon-to4` incident. The valid source
has 17 requires, six used by the moved closure, an existing unrelated
`schedule` alias, and no remaining caller. Assert the exact six target
dependencies, no new source require, both namespace loads, and exact undo.

### Boundary evidence

Use an isolated temporary project and a cold Clojure subprocess to require the
future source and destination namespaces. Parsing alone is insufficient. The
fixture supplies small local stub namespaces for all retained dependencies so
the gate does not depend on an external repository.

## Documentation and Release Checklist

- Update `:extract` and `:extract!` help with dependency-minimal behavior and
  unsupported-shape refusal.
- Update README and the advanced skill reference.
- Add the production incident and completion evidence to the Captain's Log.
- Keep the older failure-atomic plan unchanged as the prior safety layer.
- Run `make mcp-reload` and `make install` only after all gates pass.

## Verification Gates

1. The production fixture fails on the old implementation because it copies 17
   requires and adds a colliding source alias.
2. The pure dependency and alias matrices pass.
3. Focused extraction, require-operation, CLI, help, and install tests pass.
4. The cold subprocess requires both generated namespaces.
5. The formatter changes only task files.
6. clj-kondo reports no errors or warnings for changed namespaces.
7. `make mcp-test` and `make test` pass without weaker assertions.
8. A copied-program dry-run, apply, and undo return exact parse/read-back/hash
   evidence.

## Definition of Done

`clj-surgeon-to4` is complete when the production-shaped extraction produces a
destination with exactly the proved dependency namespaces, leaves a callerless
source without a destination require, chooses a deterministic free alias when
callers remain, loads both candidates in a cold runtime, and restores the exact
starting bytes through `undo-extract!`.

## Completion Evidence

- The production-shaped regression now retains only the dependencies proved by
  the moved forms. A callerless source receives no destination require.
- The pure header compiler covers aliases, `:as-alias`, qualified symbols,
  `:refer`, `:rename`, namespaced keywords, alias collisions, metadata,
  docstrings, and conservative refusals for unsupported require shapes.
- A cold runtime loads both generated namespaces. The inverse receipt restores
  the source byte-for-byte and proves that the target is absent.
- The final main suite passed 630 tests and 5,396 assertions. The MCP suite
  passed 131 tests and 1,079 assertions. clj-kondo reported no errors or
  warnings in the changed Clojure files.
- The shared MCP reloaded without restart. A live `inspect_clojure` request
  resolved the new compiler in 46 ms.
- The installed CLI extracted 15 adjacent forms, retained only
  `clojure.string`, added no source require, verified both files, and completed
  an exact undo.
