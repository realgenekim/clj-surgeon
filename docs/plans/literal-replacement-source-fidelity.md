# Literal replacement source fidelity

**Status:** Implemented and verified on 2026-08-04
**Motivating incident:** [A guarded production edit expanded reader shorthand
into `fn*`](../observations/2026-08-04-captains-log-one-read-surface.md#one-call-guarded-edits-worked-source-spelling-did-not)

## Outcome

A literal `replace` or `replace-span` written inline in `:edit :expr` keeps the
caller's exact replacement source. Reader shorthand, comments, commas,
metadata, and multiline layout survive planning and verified application.

The observed command can contain this replacement:

```clojure
{:asset-url #(views/static %)}
```

The plan edit's `:after`, diff, saved source, and apply receipt's
`:applied-edit :after` must contain the same spelling. Those source-bearing
fields must not contain an expanded `fn*` form. The selector query remains
semantic data and can display the equivalent expanded value.

## Bitter-Lesson Boundary

The implementation preserves syntax that the caller supplied. It does not
guess that an arbitrary `fn*` form should become `#()`, invent formatting, or
encode project style.

The evaluated query remains the semantic authority. Raw syntax is eligible
only when SCI parses it to the same replacement value. Computed replacements
and the `:query` data surface have no lexical source to preserve. They keep the
existing canonical printing behavior.

## Public Contract

This guarded edit preserves the inline replacement exactly:

```bash
clj-surgeon :op :edit :file src/page.clj \
  :expr "(-> (form 'page) (match '{:dev-mode? dev-mode?}) (replace '{:dev-mode? dev-mode? :head {:asset-url #(views/static %)}}))" \
  :expect '{:dev-mode? dev-mode?}' \
  :plan-out plan.edn
```

The contract applies to terminal `replace` and `replace-span` calls in direct
or threaded edit expressions.

The contract does not apply when:

- a local binding or computation produces the replacement;
- `transform` produces the replacement;
- the caller supplies `:query` data instead of `:expr` source.

Those cases continue to produce concrete, reviewable plans with canonical
printing.

## Safety Invariants

1. Source retention must not evaluate replacement code.
2. Raw replacement syntax must parse as exactly one complete form.
3. The raw form must equal the replacement value produced by SCI.
4. A mismatch or extraction failure must fall back to canonical printing. It
   must not change an existing accepted edit or create a new refusal.
5. Quoted data that contains a list headed by `replace` or `replace-span` must
   not be mistaken for the terminal builder.
6. Query metadata must remain in memory. Saved EDN plans must contain only
   concrete source strings and existing replay data.
7. Plan-only operation must not write source. Guarded application must retain
   all existing hash, exact-before, parse, atomic-write, and read-back checks.

## Implementation Shape

`compile-query` already has both representations that the operation needs:

- the original edit expression string;
- the evaluated query vector.

After evaluation, it finds the executable terminal replacement builder in the
lossless rewrite-clj syntax tree. It ignores candidate calls inside quote or
syntax-quote data. It takes the terminal replacement arguments, removes only
their outer data quote, and parses each source with SCI without evaluating it.

When the parsed value equals the evaluated terminal value, `compile-query`
stores the raw source and evaluated values in query metadata. Vector equality
and the public query data remain unchanged.

`evaluate-lens` captures that metadata before query validation removes it. It
uses an override only when the metadata values still equal the terminal query
values. Existing one-form parsing then validates the raw source before the
planner builds a concrete edit.

## Adversarial Review

The design rejects three tempting shortcuts:

1. Do not rewrite every `fn*` form back to `#()`. Explicit `fn*` is valid source,
   and the expanded form does not reveal the caller's original spelling.
2. Do not search the expression string with a regular expression. Nested maps,
   comments, strings, direct calls, spans, and quoted fake builders require a
   Clojure syntax tree.
3. Do not store executable source in the saved plan. The plan must remain a
   concrete, data-only replay artifact.

Extraction is advisory. If lossless extraction cannot prove identity, the
existing canonical plan remains the safe compatibility path.

## Test Plan

### Expression extraction matrix

- threaded `replace` with shorthand nested in a map;
- direct `replace` with an explicit `fn*` form;
- `replace-span` with two shorthand functions;
- a replacement that contains quoted data headed by `replace`;
- a computed local replacement, which records no source override;
- strings and scalar replacements;
- expression parsing failure, which retains the existing structured refusal.

### Planning and application matrix

- plan-only result preserves exact shorthand in `:after` and `:diff`;
- guarded CLI application preserves exact source bytes in the file, saved plan,
  and receipt;
- multiline replacement preserves indentation, comments, var quote, and
  anonymous-function shorthand;
- explicit `fn*` remains explicit;
- `:query` and computed replacements retain canonical behavior;
- `:expect` mismatch and every existing refusal remain no-write operations;
- saved plan replay produces the recorded result hash.

### Field regressions

The real-program-derived shapes include:

- shorthand nested under `:asset-url` in a layout options map;
- shorthand nested inside a predicate in a larger conditional;
- replacement of the degraded predicate with a named partial application.

Tests use anonymized fixtures and names. They do not retain source repository or
domain context.

## Documentation and Release Checklist

- Add the preservation contract and its boundary to `:edit` help.
- Add one shorthand example to README.
- Update the canonical skill, native Claude copy, and root entrance without
  exceeding the 90-line ceiling.
- Update `CLAUDE.md`, the changelog, and the Captain's Log.
- Keep canonical and Claude skill packages byte-identical.

## Verification Gates

1. Show the field regression failing before the implementation and passing
   afterward.
2. Format every changed Clojure file.
3. Run clj-kondo on changed source and tests with zero errors and warnings.
4. Run focused edit DSL, planner, CLI, help, and installation tests.
5. Run the complete repository suite without weakening a test.
6. Run a real CLI guarded edit and compare exact file bytes, plan source, diff,
   and receipt.
7. Run agent-text diagnostics and skill validation.
8. Run `make install` from the committed tree and verify all receipts.

## Definition of Done

The feature is complete when literal terminal replacements retain their exact
source spelling through planning and verified application, computed routes keep
their existing behavior, every adversarial and existing test passes, all
agent-facing surfaces teach the boundary, and the installed CLI and skills come
from the pushed `main` commit.

## Verification Result

- The field regression failed before implementation with eight focused
  assertion failures. The file, plan, diff, and receipt contained expanded
  `fn*` source.
- The focused edit DSL and edit gate passed 56 tests and 785 assertions after
  the implementation and final help contracts.
- The complete repository gate passed 483 tests and 3,810 assertions. This is
  five tests and 54 assertions more than the preceding release. No existing
  test was removed or weakened.
- The formatter completed on all changed Clojure files. clj-kondo reported
  zero errors and zero warnings on changed source, tests, and the executable
  fixture.
- The real guarded CLI trial preserved `#(str %)` in the file, edit `:after`,
  diff, saved plan, and applied-edit receipt. The receipt verified whole-file
  parsing, atomic write, and an equal read-back hash.
- Benchmark harness, retention-policy, and evidence-manifest checks passed.
- The canonical skill stayed at 90 lines. The Claude package remained
  byte-identical, strict agent-text lint reported zero errors, and the skill
  package validator passed.
