# Feature Plans

Use this directory for non-trivial features and refactorings. A plan is the
reviewable contract that lets an implementation agent make one complete pass
without rediscovering repository standards or guessing what "done" means.

Plans complement, and must conform to:

- [the project vision](../vision.md);
- [the testing guidelines](../testing-guidelines.md);
- [the repository instructions](../../CLAUDE.md).

Active plans:

- [Recover and report failure](recover-and-report-failure.md) — one bounded
  real-transaction reset button plus privacy-safe, fingerprinted local Bead
  reporting; no health-only success and no recovery loops.
- [Binding-aware local rename](binding-aware-local-rename.md) — preserve an
  external `:keys` data contract while renaming only one resolved local binding
  and its usages through the existing verified transaction.
- [Live MCP contract and semantic/source handshake](live-contract-and-semantic-source-handshake.md) — complete: one idempotent shared multi-workspace stack, live contract synchronization, exact-root semantic recovery, and session-bound independently verified evidence before basis storage.
- [Proof-carrying change buffer](proof-carrying-change-buffer.md) — implemented
  experiment that resolves one fully qualified Var, returns bounded exact
  decision sites and the next `apply_clojure_changes` call, then applies
  explicit `keep` or `replace` decisions through retained addresses.
- [Typed MCP inspect entrance](typed-mcp-inspect-entrance.md) — batch ordered
  forms, outlines, structural matches, and capability-limited X-ray against
  once-read snapshots; its first experiment passed correctness but missed the
  2× hypothesis and 30% keep threshold.
- [Three Rounds roadmap](three-rounds-roadmap.md) — design direction ranking
  six levers by deleted model deliberation rounds, plus the internal-substrate
  doctrine (atlas inside, algebra outside); the 3x target is the whole loop
  compiled to `inspect -> decide -> change and verify`.
- [Atlas paper exercises](atlas-paper-exercises.md) — seven real goals on
  this codebase run through the atlas design on paper; produced five design
  changes including workspace-scoped match and exploration-mode budgets.
- [Typed MCP change entrance](typed-mcp-change-entrance.md) — implemented typed
  guarded transactions; revalidating the wall-time keep gate after stale
  direct-change guidance caused one recovery round in every clean run.
- [Representative edit portfolio](representative-edit-portfolio.md) — frozen
  prompt/snapshot/diff capsules for hill-climbing complete agent editing
  workflows against native and local-microscope controls.
- [Representative MCP read portfolio](representative-read-portfolio.md) —
  frozen structural questions and semantic answers for measuring whether one
  hot batched read call beats current CLI and native controls.
- [Structural change language](structural-change-language.md) — paper design
  for compiling scoped selections and edit operators into one guarded
  transaction; includes 15 edit exercises and fail-closed boundaries.
- [Literal replacement source fidelity](literal-replacement-source-fidelity.md)
  — preserve reader shorthand, comments, commas, metadata, and layout for
  literal replacements written inline in `:edit :expr`.
- [Containing-line structural root](containing-line-edit-root.md) — select an
  unnamed top-level form by physical line, then perform one lossless nested
  read or guarded edit.

## Required plan sections

Copy this structure and remove sections only when they genuinely do not apply:

```markdown
# Feature name

**Status:** Proposed | Accepted design | Implemented
**Motivating issue/incidents:** links and field evidence

## Outcome
Observable user result in a few sentences.

## Bitter-Lesson Boundary
Why this is mechanical leverage rather than encoded architectural judgment;
explicit non-goals.

## Public Contract
Exact commands/APIs and success, refusal, no-op, and side-effect behavior.

## Safety Invariants
Properties that must hold for every branch.

## Implementation Shape
Pure core, I/O shell, affected seams, compatibility constraints.

## Test Plan
Contract-exhaustive pure matrix, field-failure regression, real-program-derived
fixtures, CLI/boundary tests, and mutation/no-write assertions.

## Documentation and Release Checklist
Help, README, skill, examples, changelog, migration notes.

## Verification Gates
Formatter, targeted tests, lint/compile, full suite, end-to-end invocation, and
a clean-context agent simulation for agent-facing CLI workflows.

## Definition of Done
One falsifiable paragraph describing complete delivery.
```

## Quality rules

- Record decisions and contracts, not a chronological implementation diary.
- Name the production failure that earns the feature's complexity.
- Include a valid starting fixture; otherwise a transformation test cannot
  prove that the operation introduced or prevented a failure.
- Make the pure behavior matrix exhaustive across the feature's semantic
  dimensions and important intersections.
- Require real-program-derived evidence without making tests brittle against a
  changing live source tree.
- Put stable user-visible diagnostics and exit behavior in the plan.
- State unsupported cases and require them to fail closed.
- Keep architecture and ownership decisions with the agent/human unless the
  feature is explicitly designed and approved to encode them.

The plan is complete when another capable agent can implement, document, and
verify the feature without asking what the public behavior, safety boundary,
test depth, or completion evidence should be.
