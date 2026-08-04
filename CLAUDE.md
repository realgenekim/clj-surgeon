# clj-surgeon

Babashka CLI tool for structural operations on Clojure namespaces.

## Required reading

Before non-trivial feature or refactoring work, read:

- [docs/vision.md](docs/vision.md) for the bookkeeping-versus-judgment boundary,
  plan contracts, and fail-closed design principles.
- [docs/testing-guidelines.md](docs/testing-guidelines.md) for the one-shot
  feature standard and required test layers.
- The applicable plan in [docs/plans/](docs/plans/) when one exists.

If a non-trivial feature has no plan, write one in `docs/plans/` before
implementation. A useful plan fixes the observable contract, non-goals,
failure data, exhaustive behavior matrix, real-program evidence, documentation
updates, and verification gates. It is not a chronological coding diary.

## Testing

- Run: `make test`
- **Read [docs/testing-guidelines.md](docs/testing-guidelines.md)** before writing tests.
- Core rule: pure functions take data and return data. Test them with literals, not temp files.
- If you need a temp file to test a function, the function needs refactoring, not the test.
- Every field failure gets a named regression test and a faithful fixture or
  source literal that records its provenance.
- Exhaust the pure behavior matrix; use filesystem and subprocess tests only
  for contracts that genuinely require those boundaries.
- A CLI feature is incomplete until help, parsing, dispatch, EDN output,
  nonzero error exits, and the documented invocation are tested.
- A write feature is incomplete until refusal leaves bytes unchanged and a
  successful candidate is reparsed, linted or compiled as appropriate, and
  exercised on a real-program-derived fixture.

## Architecture

- `forms.clj` — single source of truth for form classification (what is a defn, what is private, etc.)
- `outline.clj` — parse a file into structured form data (line boundaries, names, arglists, requires)
- `analyze.clj` — dependency analysis, topo sort, extraction closures (all pure, takes zippers)
- `core.clj` — CLI dispatch, ls-tree pipeline, formatting

## Key conventions

- Public pure functions for testable logic: `source-paths-from-config`, `filter-projects-by-hits`, `format-file-text`, `format-ls-tree-text`, `extract-ns-requires`
- Private I/O wrappers delegate immediately to pure functions; for example,
  `extract-source-paths` delegates to `source-paths-from-config`.
- All ops return EDN data, not side effects (except `!`-suffixed ops which write files)
- Errors use a human-readable `:error` string plus a stable keyword
  `:error-type` and structured diagnostic fields.
- For a large Clojure file, use `:ls` first when the relevant form is unknown.
  When a top-level name or containing line is already known, use `:show-form`
  as the first source inspection; do not run `:ls` solely as a preflight or
  reconstruct a `sed` range.
- When distinctive text is known but its containing form is not, use
  `:show-form :contains` to select that form in one command; do not manufacture
  a line with `rg -n` or print a large outline. Use `rg` for broad cross-file
  discovery. Use `:grep-form` for file-wide structural patterns; each named
  match reports reusable `:inside` ownership. Add `:inside` only when the
  parent is known or ambiguity needs narrowing.
- When sibling text identifies an edit—a `case` key, `cond` guard, map key, or
  binding name—use the structural lens getter
  `:q :query '[[:form transition] [:find :finish] :right]'`. Add
  `[:replace FORM]` to the same pipeline to emit one hash-bound plan, then apply
  it separately with `:replace-subform!`. The query never writes source. Do not
  grep a repeated expression and then read its owner merely to recover sibling
  context.
- When the adjacent forms are themselves the intended object, use
  `[[:form transition] [:find :finish] [:span 2]]`. A terminal
  `[:replace-span :finish (assoc state :status :complete)]` requires equal
  replacement arity and preserves every comment and whitespace byte between
  the peers; apply its plan separately with `:replace-subform!`.
- When a task asks for every pair in a `case`, `cond`, map, binding vector, or
  other flat sibling run, start at the first member and use
  `[:partition-all 2]`. Do not read the owner and manually count children or
  issue one query per key. The final shorter span is explicit and must be
  interpreted by the caller. clj-surgeon does not infer a default or error.
- When repeated nested heads make that first outer member unknown, promote the
  heads to their owner nodes and filter contained owners with
  `[:find cond] :up :outermost` before navigating to the children. Placement is
  significant: use `:up :outermost`, not `:outermost :up`. When the first outer
  guard is already known, anchor there directly because that route is shorter.
- Use `:xray :expr` as the primary structural read surface. Write one pure Clojure
  path, such as `(-> (form 'transition) (match :finish) right)`, to
  return literal source evidence. End it with `analyze`; its function always
  receives a vector in match order. Add `expect-count` before `analyze` when
  cardinality must be exact; refusal occurs before the function runs and does
  not change its input type. The computed `:value` has compact hash evidence;
  literal paths retain full source. X-ray never writes source or a plan. For
  a selected `def`, use `initializer` to select its right-hand side without
  evaluating it. Computed X-ray canonicalizes map literals and
  `hash-map`/`array-map` syntax to one map view while exact evidence remains
  source-shaped. For CLJC, pass `:clj` or `:cljs` to `form`.
- Generate a replacement plan in a standalone shell command. Observe and
  review it before running a separate apply command; never chain planning and
  application. When the intended relationship and replacement are already
  exact, a `:q` query ending in `[:replace ...]` may be the first non-mutating
  call; read first only when the choice requires a separate judgment. A
  successful verified apply receipt proves exact replay,
  read-back hash, atomic write, and whole-file parse; do not reread the edited
  or neighboring forms solely to reproduce that evidence. Review an aggregate
  Git diff only when task context already establishes a worktree or explicitly
  requests it; do not probe `.git` merely to repeat edit-level evidence.
- Format changed Clojure files before linting or testing. Use the repository's
  formatter when configured; otherwise run
  `npx @chrisoakman/standard-clojure-style fix <changed-files>`.
- Do not declare a feature complete from a green legacy suite alone. Show the
  new tests that fail before the implementation, pass afterward, and cover the
  real invocation that motivated the work.
