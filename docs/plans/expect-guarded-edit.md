# Plan: `:expect` — the optional one-call guarded edit

Decided 2026-08-04. Motivation and competitive analysis:
`docs/observations/2026-08-04-captains-log-a-clean-claude-caller.md`.

## Observable contract

`:edit` gains one optional argument, `:expect FORM`.

- **Without `:expect`** every existing behavior is byte-identical to today:
  plan-only, apply later via `:replace-subform!`. This remains the default
  and the documented route; Codex and the benchmarked Claude flows need no
  change.
- **With `:expect`** the command becomes a one-call guarded edit: it
  computes the plan exactly as today, then compares the selected form
  (the plan's before, parsed as Clojure data) against the parsed
  `:expect` form using structural equality (whitespace- and
  comment-insensitive). On equality it saves the plan artifact to
  `:plan-out` AND applies it atomically in the same invocation, returning
  the union of plan evidence and the `:replace-subform!` apply receipt
  plus `:mode :expect-guarded`. On inequality it refuses.

The doctrine survives intact: the review gate is not removed, it is
mechanized. `:expect` is the caller's declared before-state — the same
epistemic role as the built-in Edit tool's `old_string`, with structural
rather than textual matching. The saved plan remains the audit artifact.

## Non-goals

- No `:expect` on any other operation.
- No wildcard, pattern, or partial `:expect` matching; exact structural
  equality only.
- No change to `:replace-subform!`.
- No skill text change in this plan (line budget; revisit separately).

## Failure data (from the field, 2026-08-04)

The audit-payload trap: `route-event`'s `:finish` result is
`(assoc state :status :done :audit (:audit payload))` while a caller
believing the skill example expects `(assoc state :status :done)`. Two
agents (Fable manual battery, Opus benchmark baseline) each planned the
destructive whole-form replacement and were saved only by eyeball review.
With `:expect '(assoc state :status :done)'` the same mistake becomes a
one-call structured refusal showing both forms.

## Behavior matrix

| # | Input | Outcome |
|---|---|---|
| 1 | no `:expect` | today's behavior, unchanged (plan-only, all existing refusals) |
| 2 | `:expect` equal to selection, `:expr` route | plan saved + applied + verified in one call; exit 0; `:mode :expect-guarded` |
| 3 | `:expect` equal to selection, `:query` route | same as 2 |
| 4 | `:expect` ≠ selection | exit nonzero; `:error-type :expect-mismatch`; `:expected` and `:actual` as data plus `:actual-source`; file bytes unchanged; no apply; a pre-existing `:plan-out` artifact is preserved |
| 5 | `:expect` unparseable (zero or multiple forms, reader error) | exit nonzero; `:error-type :invalid-expect`; refused before selection/source read where feasible |
| 6 | `:expect` with zero/ambiguous selection | existing selection refusals win, unchanged error types |
| 7 | `:expect` with a getter-only pipeline (no replace/transform) | existing invalid-edit refusal, unchanged |
| 8 | `:expect` equal modulo whitespace/comments in source | equality holds (structural comparison of parsed forms) |
| 9 | apply-stage failure after match (hash race, parse failure) | existing `:replace-subform!` refusal semantics; source restored/untouched per current atomic-write guarantees |

## Real-program evidence

Demonstrate on a /tmp copy of `bench/fixtures/bench/pair_view.clj`:
matrix row 4 with the naive expectation (the trap refuses), then row 2
with the true form (one-call verified apply, byte-diff shows one edit).

## Documentation updates

- `:edit` help: `:expect` argument line, one example, safe-workflow note
  ("optional; without it, plan and apply separately as before").
- CHANGELOG entry under Unreleased.
- README `:edit` section: one-call guarded form.
- Skill: explicitly deferred (90-line budget); tracked as follow-up.

## Verification gates

- New tests fail before implementation, pass after; every matrix row has
  a named test; CLI-level coverage for rows 2, 4, 5 (help, parsing, EDN
  output, nonzero exits, documented invocation).
- `make test` fully green, including drift suites.
- The real-program evidence run above, executed and recorded.
