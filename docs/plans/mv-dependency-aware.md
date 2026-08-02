# Dependency-Aware `:mv` and `:mv-with-deps`

**Status:** Implemented (2026-08-01)

**Motivating issue:** [#20](https://github.com/realgenekim/clj-surgeon/issues/20)

**Field evidence:** mothership `walk-files` / `skip-dirs` failure and the earlier
`writer.state` `transition!` cascading-dependency failure

## Outcome

Make a requested within-file move safe and self-explanatory without turning
clj-surgeon into an architectural refactoring engine.

- Plain `:mv` continues to mean "move exactly this form before that form."
- Before writing, it constructs and validates the complete candidate source.
- If the move introduces unresolved intra-namespace references, it refuses,
  names the newly stranded forms, performs no write, and recommends the exact
  equivalent `:mv-with-deps` command.
- `:mv-with-deps` is a preset alias for `:mv :with-deps true`.
- The opt-in operation moves only the minimum transitive dependency set needed
  to make that user-selected move valid.
- Neither form silently introduces declarations or makes ownership decisions.

This is a mechanical correctness guard plus explicit consent to a wider edit.
The user still chooses the form and destination.

## Why This Passes the Bitter-Lesson Test

This feature removes mechanics the agent already knows it wants: compute which
definitions must precede the requested destination, present the widened edit,
and move them without clipping syntax. It does not choose architecture.

The feature must not:

- infer a subsystem or cluster that "belongs together";
- pull private or conceptually related helpers unless required by dependency
  order at the selected destination;
- move callers merely to make a downward move work;
- reorder the whole namespace;
- cross namespace boundaries;
- introduce `(declare ...)` automatically;
- pretend to solve arbitrary macro expansion, runtime initialization order, or
  every Clojure name-resolution rule.

When the safe mechanical answer is ambiguous, fail closed and return evidence
for the agent. This plan exists because the same concrete failure occurred in
real work more than once and an unimplemented regression TODO already describes
it; it is not an AST-driven speculative feature.

## Public Contract

### Safe exact move

```bash
clj-surgeon :op :mv \
  :file src/app/analysis.clj \
  :form walker \
  :before consumer
```

If the candidate introduces no new dependency-order violation, move only
`walker` and return success.

### Guarded refusal with a concrete recommendation

```clojure
{:error "Moving walker before consumer would strand dependencies"
 :error-type :would-strand-dependencies
 :operation :mv
 :file "src/app/analysis.clj"
 :form "walker"
 :before "consumer"
 :direction :up
 :stranded [{:name "skips"
             :defined-at 8
             :would-be-at 12
             :required-before 5}]
 :recommended-action :preview-dependency-closure
 :recommended-command
 "clj-surgeon :op :mv-with-deps :file src/app/analysis.clj :form walker :before consumer :dry-run true"
 :apply-command
 "clj-surgeon :op :mv-with-deps :file src/app/analysis.clj :form walker :before consumer"
 :remedies {:with-deps "Move only the dependencies required at the destination"
            :manual "Move skips first, then retry"}}
```

Requirements:

- `:error` remains human-readable and `:error-type` remains stable and
  machine-readable.
- `:stranded` is deterministically ordered and includes source evidence.
- The recommended command is copy/pasteable, uses `:mv-with-deps`, and is a
  non-mutating dry run. The mutating command is separately named
  `:apply-command` so an agent cannot mistake preview for consent.
- CLI exit status is nonzero.
- The file remains byte-for-byte unchanged.

### Explicit dependency-expanded move

These commands are behaviorally identical:

```bash
clj-surgeon :op :mv-with-deps \
  :file src/app/analysis.clj \
  :form walker \
  :before consumer

clj-surgeon :op :mv \
  :file src/app/analysis.clj \
  :form walker \
  :before consumer \
  :with-deps true
```

Dry-run output must disclose the widened scope:

```clojure
{:ok true
 :plan {:operation :mv
        :requested-forms ["walker"]
        :added-forms ["normalize-skip" "skips"]
        :move-order ["normalize-skip" "skips" "walker"]
        :before "consumer"
        :with-deps true
        :source-hash "..."
        :result-hash "..."
        :diff "..."}}
```

`:mv-with-deps` must never hide which forms it added.

### Downward moves

Moving a definition downward can strand callers rather than dependencies. Plain
`:mv` must detect the complete candidate delta and refuse with a distinct
diagnostic such as `:would-strand-users`.

`:with-deps` does not mean "move dependent callers too." That would widen the
operation from dependency bookkeeping into program restructuring. Recommend a
manual choice instead.

### Declarations

An existing declaration is part of the baseline ordering model. The guard must
compare newly introduced violations with the valid starting program rather than
reject every textual forward edge.

Do not implement `:declare true` in this feature. `(declare x)` can make a name
compile while leaving an eager `def` initializer to read an unbound var, and it
does not safely convert an earlier macro use. Automatic declarations require a
separate, conservatively scoped design.

## Safety Invariants

1. A refused move never changes the file.
2. A successful plain move changes only the requested form and its attached
   comment header/whitespace.
3. A successful dependency-expanded move changes only the requested form plus
   the disclosed minimum required dependency closure.
4. The candidate introduces no new unresolved intra-namespace edge.
5. Existing unrelated forward-reference debt does not block an otherwise safe
   move; compare the before/after violation sets.
6. The complete result parses before any write.
7. Planning and execution use the same candidate and hashes.
8. Errors are stable EDN and shell failures.
9. Ambiguous, cyclic, or unsupported cases fail closed.
10. The canonical and alias commands produce equivalent plans and results.

## Dependency Semantics

Model safety as the delta between dependency-order violations in the source and
candidate:

```clojure
(new-violations source candidate)
;; => (set/difference (violations candidate) (violations source))
```

The positional graph must account for definitions and declarations. It must
also distinguish outgoing edges from the moved form and incoming edges from
users so diagnostics explain the correct remedy.

For an upward `:with-deps` move:

1. Start with the requested form.
2. Find its dependencies not satisfied at the destination.
3. Recursively add only dependencies that would themselves be unsatisfied
   after relocation.
4. Leave already-satisfied dependencies in place.
5. Produce deterministic dependency-before-user ordering while minimizing
   movement and preserving source order where it is already valid.
6. Construct the complete candidate and validate the global before/after delta.

Moving a definition earlier is monotonic for its other users, so a shared
dependency is not inherently unsafe. Candidate-wide validation remains the
general protection; do not reject merely because a dependency has other users.

### Analyzer limitation that must be resolved

The current `symbols-in-form` token walk is not scope- or quote-aware. It can
mistake a local binding or quoted symbol for a top-level dependency:

```clojure
(def config 1)
(defn f [config] config) ; local, not the top-level var
(defn g [] 'config)      ; data, not a var reference
```

It is unsafe to auto-pull forms from that graph without correction. Before
shipping, either:

- make dependency extraction sufficiently lexical and quote-aware for the
  supported syntax; or
- use the graph to propose a closure and a stronger semantic check such as
  clj-kondo to validate/reference the candidate.

Do not broaden this feature into a homegrown language server. Clearly define
the supported syntax and fail closed where the analysis cannot make the
promised decision.

## Implementation Shape

Honor the functional-core/imperative-shell standard:

1. Extract a public pure planner that accepts source text plus normalized move
   options and returns either a complete plan or structured refusal.
2. Keep form discovery, candidate construction, violation delta, dependency
   closure, and output formatting independently testable with source strings
   and data.
3. Make the file wrapper read once, delegate to the pure planner, verify the
   unchanged snapshot, and atomically install the already-planned result.
4. Reparse the complete candidate before returning a plan.
5. Route plain `:mv`, `:mv :with-deps true`, dry-run, and `:mv-with-deps`
   through the same planner.
6. Extend registry alias metadata to support preset defaults, or add an equally
   explicit thin wrapper. The current alias map only renames an operation and
   cannot inject `:with-deps true`.

Likely files:

- `src/clj_surgeon/move.clj`: pure planner and thin file application.
- `src/clj_surgeon/analyze.clj`: supported dependency/reference semantics.
- `src/clj_surgeon/core.clj`: option registration, preset alias, help, dispatch.
- `test/clj_surgeon/move_test.clj`: pure contract matrix and minimal I/O tests.
- `test/clj_surgeon/analyze_test.clj`: lexical/quote dependency regressions.
- `test/clj_surgeon/help_test.clj` and `cli_dispatch_test.clj`: public CLI.
- `test-fixtures/mv/`: faithful real-program-derived fixtures.
- `README.md`, `skills/clj-surgeon/SKILL.md`, and `CHANGELOG.md`: public usage.

## Test Plan

### Test architecture

- The exhaustive matrix is pure: source string and options in, plan/refusal and
  candidate source out.
- Table-driven tests cover repeated shapes without filesystem setup.
- A small number of file tests prove unchanged bytes on refusal and exact
  installation on success.
- Subprocess tests prove CLI EDN, exit codes, help, and alias default injection.
- Real-program-derived fixtures prove the motivating failures and realistic
  dependency depth.

### Pure behavior matrix

| Case | Plain `:mv` | `:mv-with-deps` |
|---|---|---|
| Upward move, no dependencies | moves requested form | identical result; no added forms |
| Upward move, all dependencies above destination | moves requested form | identical result; no added forms |
| Upward move strands one direct dependency | refuses and recommends alias | pulls exactly that dependency |
| Upward move strands a transitive chain | refuses with immediate and/or complete evidence | pulls minimum transitive closure dependency-first |
| Upward move uses a shared dependency | refuses if position requires it | moves it earlier; other users remain valid |
| Some dependencies already satisfied | reports only unsatisfied names | leaves satisfied definitions untouched |
| Existing declaration satisfies an edge | does not report a new violation | does not pull unnecessarily |
| Existing unrelated forward-reference debt | ignores unchanged debt | candidate must add no new debt |
| Dependency cycle | precise fail-closed error | refuses; does not invent declarations |
| Downward move with no intervening users | moves requested form | identical result |
| Downward move strands an intervening caller | `:would-strand-users` refusal | same refusal; does not move callers |
| Source equals destination | explicit no-op or stable error, documented | identical contract |
| Missing source/destination | stable existing not-found errors | identical errors |
| Duplicate/ambiguous definition | refuses with candidates | identical refusal |
| Metadata-wrapped named forms | finds and moves exact definition | dependencies remain correct |
| Attached comment header | travels exactly once | travels with each disclosed moved form |
| Two top-level forms share one physical line | `:unsupported-source-layout` refusal | same fail-closed refusal |
| Local shadows a top-level name | no false dependency | does not pull shadowed top-level form |
| Quoted/syntax data names a top-level var | no false dependency for supported quoting | does not pull data-only symbol |
| `def`, `defonce`, `defn`, and `defmacro` edges | supported cases explicit | unsupported eager/macro cases fail closed |
| Reader-conditional/platform-specific definitions | platform-correct or explicit unsupported refusal | never merges platform meanings silently |
| Dry-run | returns full candidate metadata, writes nothing | discloses complete widened scope |
| Alias versus flag | n/a | plans and resulting bytes are identical |

Each refusal test asserts the complete stable fields relevant to callers, not
only `(:error result)`.

### Motivating fixture: valid baseline

The issue's minimized source must include the declaration that makes the
starting program valid:

```clojure
(ns mv-strands-dep)

(declare walker)

(defn consumer [dir]
  (walker dir))

(def skips
  #{"target" ".git"})

(defn walker [d]
  (contains? skips d))
```

Required assertions:

1. The baseline cold-lints or compiles.
2. Old/plain behavior would place `walker` above `skips`.
3. New plain `:mv` returns `:would-strand-dependencies`, names `skips`,
   recommends the exact `:mv-with-deps` command, exits nonzero, and preserves
   bytes.
4. `:mv-with-deps` reports `skips` in `:added-forms`, moves `skips` before
   `walker` before `consumer`, and touches no unrelated form.
5. The complete result cold-lints or compiles.

### Real-program-derived corpus

Add fixtures with provenance comments for:

- mothership: `walk-files` moved above `run-kondo` while depending on
  `skip-dirs`;
- `writer.state`: a minimized multi-level `transition!` dependency chain that
  originally caused cascading unresolved vars;
- clj-surgeon self-surgery: select a real dependency chain from this repository
  and plan against a copied source snapshot, proving the tool can explain its
  own code without touching the working file.

The mothership and writer fixtures should be faithful minimized snapshots. The
self-surgery corpus case may load the repository source as data but must assert
structural invariants rather than brittle absolute line numbers.

### CLI and help tests

Test the exact documented invocations through the subprocess harness:

- `:op mv-with-deps` as a bare string and keyword-shaped value;
- `:op mv :with-deps true`;
- equivalence of plans/results;
- `mv --help` documents `:with-deps`;
- global/help alias text resolves through dispatch;
- refusal prints one EDN map to stdout, no stack trace to stderr, and exits
  nonzero;
- success exits zero.

### Mutation tests

- Refusal leaves source bytes and modification time unchanged where feasible.
- Dry-run never writes.
- A stale source between plan and apply is rejected if the new planner exposes
  durable plan application.
- A dependency-expanded result appears atomically; no test can observe a state
  where only part of the group moved.
- Complete results parse and the real-program fixtures lint/compile.

## Documentation and Release Checklist

- [x] `README.md` documents guarded refusal and the recommended alias.
- [x] `skills/clj-surgeon/SKILL.md` tells agents to run plain `:mv`, read the
      refusal, and use `:mv-with-deps` only after reviewing added forms.
- [x] `clj-surgeon :op mv --help` shows `:with-deps` and its safety semantics.
- [x] Alias help identifies `mv-with-deps` as a preset, not a second algorithm.
- [x] `CHANGELOG.md` records the new refusal contract and compatibility impact.
- [x] Issue #20's exact field failure is linked from the regression fixture.

## Verification Gates

Before declaring the feature complete:

1. Format only changed Clojure files with the repository formatter or
   `npx @chrisoakman/standard-clojure-style fix <changed-files>`.
2. Run targeted move, analyze, help, and CLI-dispatch tests.
3. Run clj-kondo or cold compilation on every promised valid candidate in the
   real-program fixture set.
4. Run `make test` and report test/assertion counts.
5. Run the documented refusal command and both dependency-expanded spellings;
   capture EDN and exit status.
6. Review the diff for undisclosed moved forms and unrelated formatting churn.
7. Confirm all unsupported cases return structured fail-closed errors.

## Implementation Evidence

- The public pure entry point is `move/plan-move`; the file wrapper delegates to
  it and uses one atomic replacement for the complete candidate.
- The motivating mothership fixture preserves the exact `walk-files`,
  `skip-dirs`, and `run-kondo` relationship from issue #20, including the
  declaration that makes the baseline valid. The writer-state fixture exercises
  a transitive dependency chain, and the dogfood test plans a guarded move over
  clj-surgeon's own `analyze.clj` source.
- The pure and boundary matrix covers exact success, guarded direct and
  transitive refusal, minimal expansion, declarations, shared dependencies,
  pre-existing debt, downward users, cycles, ambiguity, no-op, local scope,
  quoted data, namespaced destructuring, destructuring defaults, eager defs,
  macros, comments, metadata, unsupported same-line layouts, dry-run, actual
  writes, CLI exit behavior, and alias/flag equivalence.
- Both real-program fixtures cold-lint before and after the dependency-expanded
  move. The complete repository suite passes with 297 tests and 1,268
  assertions. Changed files pass formatting and clj-kondo with zero warnings.
- A real plain CLI invocation against a copied mothership fixture exits 1 with
  `:error-type :would-strand-dependencies`, names `skip-dirs`, recommends the
  exact non-mutating `:mv-with-deps` preview, and leaves the bytes unchanged.
  The successful preview repeats the separate `:apply-command`; it exits 0
  after review and discloses
  `:added-forms ["skip-dirs"]` and `:move-order ["skip-dirs" "walk-files"]`.
- Unsupported macro-expansion and platform-reader semantics are not guessed.
  The planner's supported lexical analyzer handles the documented common
  binding forms; ambiguous and cyclic closures fail closed.
- Three clean-context agent simulations exercised help-only discovery,
  README-plus-help planning, and an adversarial real CLI refusal/remedy flow.
  Their findings produced the safe preview/apply split, corrected write-safety
  help, explicit agent branching, and accurate original/candidate line evidence.

## Definition of Done

The feature is done only when plain `:mv` cannot recreate either motivating
failure, its refusal recommends a copy/pasteable `:mv-with-deps` dry run,
the alias and flag produce the same disclosed minimum closure, downward caller
breakage is also guarded, the valid baseline and transformed fixtures compile,
all refusal paths preserve bytes, public documentation agrees with CLI help,
and the complete repository suite passes.
