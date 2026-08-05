# Feature Plans

Use this directory for non-trivial features and refactorings. A plan is the
reviewable contract that lets an implementation agent make one complete pass
without rediscovering repository standards or guessing what "done" means.

Plans complement, and must conform to:

- [the project vision](../vision.md);
- [the testing guidelines](../testing-guidelines.md);
- [the repository instructions](../../CLAUDE.md).

Active plans:

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
