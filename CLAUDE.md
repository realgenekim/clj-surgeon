# clj-surgeon

Babashka CLI tool for structural operations on Clojure namespaces.

## Required reading

Before non-trivial feature or refactoring work, read:

- [docs/vision.md](docs/vision.md) for the bookkeeping-versus-judgment boundary,
  plan contracts, and fail-closed design principles.
- [docs/testing-guidelines.md](docs/testing-guidelines.md) for the one-shot
  feature standard and required test layers.
- The applicable plan in [docs/plans/](docs/plans/) when one exists.

When working in this repository, the working-tree [skill.md](skill.md)
supersedes any installed clj-surgeon skill; installed copies are stable
snapshots that may lag the branch.

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

## Benchmark harness architecture

- Treat `bench/run_clean_codex.sh` and `bench/run_clean_claude.sh` as thin
  caller adapters, not independent applications. Before adding logic to either
  script, ask whether a pure data transformation or shared policy can move to
  a tested Babashka namespace and be called by both harnesses.
- Use the strangler pattern. Characterize current behavior, extract one seam,
  route both harnesses through it, and keep the old shell path until parity is
  proved. Do not rewrite a working harness wholesale.
- Keep shell for irreducible process wiring such as environment isolation,
  redirection, signals, and launching the caller. Put task catalogs, fixture
  validation, schedules, scoring, retention policy, summaries, and receipt
  construction in shared Babashka components whenever practical.
- Do not copy a Codex harness feature into the Claude harness or vice versa.
  Extract the common contract first; keep only caller-specific invocation and
  telemetry parsing in the adapters.
- Every extracted seam needs pure self-tests plus the existing boundary
  self-tests on both harnesses. Extraction must preserve or strengthen the
  evidence and retention contracts.

## Key conventions

- Public pure functions for testable logic: `source-paths-from-config`, `filter-projects-by-hits`, `format-file-text`, `format-ls-tree-text`, `extract-ns-requires`
- Private I/O wrappers delegate immediately to pure functions; for example,
  `extract-source-paths` delegates to `source-paths-from-config`.
- All ops return EDN data, not side effects (except `!`-suffixed ops which write files)
- Errors use a human-readable `:error` string plus a stable keyword
  `:error-type` and structured diagnostic fields.
- Before native Read, Edit, grep, sed, or cat touches an existing Clojure,
  ClojureScript, or CLJC file, load the working-tree skill and use
  clj-surgeon. Native Write remains appropriate for new files; use native
  editing for unsupported prose- or comment-heavy changes. For a file over
  500 lines, use `:ls` first when the relevant form is unknown. When a
  top-level name or containing line is already known, use `:cat` as the first
  source inspection; do not run `:ls` solely as a preflight or reconstruct a
  `sed` range.
- When distinctive text is known but its containing form is not, use
  `:cat :contains` to select that form in one command; do not manufacture
  a line with `rg -n` or print a large outline. Use `rg` for broad cross-file
  discovery. Use `:match-form` for file-wide structural patterns; each named
  match reports reusable `:inside` ownership. Add `:inside` only when the
  parent is known or ambiguity needs narrowing. `:match` accepts one EDN form
  pattern, not a regular expression.
- When sibling syntax identifies a target—a `case` key, `cond` guard, map key,
  or binding name—read it with `:xray :expr "(-> (form 'transition) (match
  :finish) right)"`. For a known literal replacement, use the same path with
  `:edit`, terminal `replace`, and `:expect` to apply and verify in one call.
  Do not grep a repeated expression and then read its owner merely to recover
  sibling context.
- When a physical line identifies one otherwise unnamed top-level owner, start
  an X-ray or edit path with `(line N)`, then use `match` or navigation to
  select the exact nested syntax. The line can be inside the form or in its
  attached comment. Blank gaps and overlapping owners must refuse. Prefer
  `(form 'NAME)` when semantic identity is known.
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
  evaluating it. Computed X-ray shallowly normalizes a selected map literal or
  top-level `hash-map`/`array-map` syntax to one map view; nested constructor
  syntax and exact evidence remain source-shaped. The SCI sandbox limits
  capabilities but does not prove termination, so analyzers must perform
  bounded work. For CLJC, pass `:clj` or `:cljs` to `form`.
- When a replacement is computed or its before-state is not declared, generate
  a replacement plan in a standalone shell command. Observe and review it
  before running a separate apply command; never chain planning and
  application. When the intended relationship, literal replacement, and exact
  before-state are known, one `:edit` call with `:expect` may be the first
  source-bearing action. Omit `:plan-out` unless an audit artifact must be
  retained. Read first only when the choice requires a separate judgment. A
  successful verified apply receipt proves exact replay,
  read-back hash, atomic write, and whole-file parse; do not reread the edited
  or neighboring forms solely to reproduce that evidence. Review an aggregate
  Git diff only when task context already establishes a worktree or explicitly
  requests it; do not probe `.git` merely to repeat edit-level evidence.
- A literal `replace` or `replace-span` written inline in `:expr` preserves its
  exact source spelling, including `#()`, comments, commas, metadata, and
  multiline layout. Keep the literal at the terminal builder when spelling
  matters. A computed replacement and the `:query` surface contain only data
  and use canonical printing.
- Format changed Clojure files before linting or testing. Use the repository's
  formatter when configured; otherwise run
  `npx @chrisoakman/standard-clojure-style fix <changed-files>`.
- Do not declare a feature complete from a green legacy suite alone. Show the
  new tests that fail before the implementation, pass afterward, and cover the
  real invocation that motivated the work.
