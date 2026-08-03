# Exact Top-Level Form Read with `:show-form`

**Status:** Implemented and verified
**Motivating incident:** [production migration ethnography](../observations/2026-08-02-production-migration-in-the-wild.md), Episode 5

## Outcome

An agent can read one complete top-level Clojure form in one command, either by
its unqualified name or by a line that the form contains:

```bash
clj-surgeon :op :show-form :file state.clj :form transition!
clj-surgeon :op :show-form :file state.clj :line 1134
clj-surgeon :op :cat :file state.clj :form transition!
```

The command returns one EDN map with the exact form source, location,
classification, platforms, selector, and file snapshot hash. It never writes.
It refuses missing, conflicting, invalid, absent, or ambiguous selectors with a
stable nonzero error.

`:cat` is a strict alias for `:show-form`. It accepts the identical argument
contract, never dumps the whole file, and preserves `:operation :show-form` in
success and refusal data. Help exposes the alias; executable remedies use the
explicit canonical name.

## Bitter-Lesson Boundary

`:show-form` supplies mechanical perception: exact top-level boundaries,
names, line containment, reader-conditional platforms, source text, and a
snapshot hash. The caller still decides why the form matters, what it means,
and whether or how to change it.

The operation does not:

- choose a likely form when selection is ambiguous;
- search arbitrary nested syntax;
- infer architecture, ownership, or desired edits;
- mutate source or produce an application plan;
- replace `rg` for broad discovery or `:find-subform` for structural patterns;
- add operation telemetry or usage receipts.

This is a general structural read primitive, not encoded migration knowledge.

## Public Contract

### Arguments

| Argument | Requirement | Meaning |
|---|---|---|
| `:file` | required | Clojure, ClojureScript, or CLJC source file |
| `:form` | exactly one of `:form`/`:line` | Unqualified top-level form name |
| `:line` | exactly one of `:form`/`:line` | Positive one-based line contained by a top-level form |
| `:platform` | optional | Keyword platform used to disambiguate reader-conditional forms |

`:form` accepts a symbol or string representation of an unqualified name.
`:line` must be a positive integer. `:platform`, when present, must be a
keyword. A line in a contiguous leading comment block selects the form to which
`:ls` already attaches that block. A blank line between forms selects nothing.

### Success

```clojure
{:operation :show-form
 :file "state.clj"
 :selector {:form transition!}
 :type defn
 :name transition!
 :platforms [:clj]
 :line 1128
 :end-line 1157
 :comment-start 1125
 :source "(defn transition! ...)"
 :source-hash "<sha-256-of-complete-file>"}
```

`:source` is the exact parsed top-level list form. It excludes preceding
comments; `:comment-start` identifies an attached contiguous comment block.
`:line` remains the first line of the parsed form. `:source-hash` hashes the
complete input file, not only the returned form.

For `:line` selection, `:selector` is `{:line 1134}`. When supplied,
`:platform` is included in `:selector` for either selection mode.

### Refusals

Every refusal includes `:operation :show-form`, `:file`, `:selector` when one
can be constructed, `:error`, `:error-type`, and `:source-hash` when source was
available.

| Condition | `:error-type` | Required additional data |
|---|---|---|
| no selector | `:missing-selector` | `:required-one-of [:form :line]` |
| missing file at dispatch | `:missing-arguments` | canonical operation, selector, and `:missing [:file]` |
| both selectors | `:conflicting-selectors` | `:supplied-selectors [:form :line]` |
| qualified/invalid form | `:invalid-form-selector` | rejected `:form` |
| nonpositive/noninteger line | `:invalid-line` | rejected `:line` |
| nonkeyword platform | `:invalid-platform` | rejected `:platform` |
| name absent after platform filter | `:form-not-found` | `:match-count 0` and an executable `:ls` remedy |
| line outside all forms | `:line-not-in-form` | `:match-count 0` and an executable `:ls` remedy |
| multiple matches | `:ambiguous-form` | total `:match-count`, at most 10 candidate locations, truncation flag, and platform remedy when applicable |
| parse failure | `:invalid-source` | parser message |

Ambiguity is evidence. The operation never selects the first candidate.

### Invocation remedies

Existing errors gain a `:remedies` map only when explicit, valid arguments
identify the named-form/containing-line read job:

- An unknown operation with `:file` and exactly one of `:form`/`:line`
  recommends the corresponding `:show-form` command.
- `:find-subform` with `:line` but without required `:match` retains
  `:missing-arguments` and recommends `:show-form :line`.

Each remedy contains `:operation`, `:reason`, `:command`, and `:command-args`.
The argument vector is the machine contract. The rendered command is the
copy/paste interface. No fuzzy operation matching or general intent engine is
added. Invalid, qualified, nonpositive, overflowing, or incorrectly typed
selectors do not receive a remedy that would necessarily fail again.

### Plan application guidance

`:replace-subform! --help`, both repository skill documents, and the README
must state:

> Apply the reviewed plan directly with `:replace-subform!`. Do not edit the
> plan with `apply_patch` or another text tool. If the intended edit changes,
> generate a new plan.

This is documentation and tested usage guidance. It does not change plan
execution semantics.

## Safety Invariants

1. `:show-form` performs no writes for success or refusal.
2. Success contains exactly one form selected under the declared platform.
3. Returned source is exactly `z/string` for the selected parsed form.
4. Selection never depends on textual substring or approximate name matching.
5. Name comparison does not turn qualified names into unqualified names.
6. Line selection uses parser positions plus the existing attached-comment
   boundary; it does not guess across blank lines.
7. Reader-conditional branches retain their platform sets and never merge.
8. Existing `:ls` output remains byte-for-byte compatible apart from changes
   explicitly required to expose a pure source API internally.
9. Errors print one EDN value to stdout, no stack trace to stderr, and exit
   nonzero.

## Implementation Shape

Refactor `clj-surgeon.outline` into a functional core and thin I/O shell:

- `top-level-form-records` takes a filename, source string, and immutable
  project-alias map and returns the parsed records used by both `:ls` and
  `:show-form`, including exact `:source` data for structural callers. Its
  two-argument arity uses an empty alias map and reads no global state.
- `outline-source` accepts the same explicit alias data and returns the existing
  public outline map without embedding each form's source.
- `outline` remains the compatibility I/O wrapper that slurps once, snapshots
  configured aliases, and calls `outline-source`.

Add `clj-surgeon.show-form`:

- `select-form` is pure: source string plus options, including optional
  immutable `:project-aliases`, in; success/refusal map out.
- `show-file` is the thin wrapper that slurps the requested file and snapshots
  configured aliases.
- command rendering and invocation-remedy construction are pure.

Add one `:show-form` entry to `ops-registry`. Keep required-selector validation
inside `select-form`, because the registry cannot express exactly-one-of
arguments. Register `:cat` as its strict alias. Add narrow remedy attachment to
the existing dispatch error path.

## Structural-Shell API Direction

The durable vocabulary is intentionally small:

```text
ls -> cat -> grep-form/deps/users -> plan -> apply -> verify
```

- `:ls` remains inventory and is never overloaded to return form source.
- `:cat` retrieves exactly one explicitly selected top-level form and refuses
  a missing selector rather than dumping a file.
- `:grep-form` is a strict structural-shell alias for the already file-wide
  `:find-subform`; `:inside` is optional narrowing, not a mandatory parent-form
  guess. The canonical implementation and machine vocabulary remain
  `find-subform` for compatibility.
- Zero and multiple matches remain useful read evidence.
- `rg` remains broad lexical discovery. Structural wildcards never pretend to
  be regular expressions.

Adjacent sibling sequences are a separate structural gap: `case` clauses,
`cond` branches, binding pairs, and map entries are not independently readable
wrapper forms. A general sibling-span lens—not special-case semantic logic—is
tracked in [issue #21](https://github.com/realgenekim/clj-surgeon/issues/21).

## Test Plan

### Pure behavior matrix

| Selector/shape | Expected result |
|---|---|
| symbol name | exact named form |
| string name from CLI parser | same exact form |
| standalone `/` name | accepted as an unqualified Clojure symbol |
| qualified name | `:invalid-form-selector` |
| missing name | `:form-not-found` plus `:ls` remedy |
| duplicate name, same platform | `:ambiguous-form`; bounded locations and total count |
| more than 10 duplicate names | first 10 locations plus truncation evidence |
| duplicate name, different platforms | ambiguous without `:platform`; exact with it |
| metadata-wrapped name | exact name and source preserved |
| project-config custom macro name | same name semantics as `:ls` |
| identical pure args under changed global alias atom | identical result |
| line at form start/interior/end | same exact form |
| line in attached comment block | attached form |
| line in blank gap | `:line-not-in-form` |
| line in `(comment ...)` form | exact comment form |
| line in namespace form | exact namespace form |
| noninteger/zero/negative line | `:invalid-line` |
| no selector | `:missing-selector` |
| both selectors | `:conflicting-selectors` |
| invalid platform | `:invalid-platform` |
| `.clj`, `.cljs`, shared `.cljc` | correct platform vectors |
| `#?` and `#?@` branch forms | platform retained; exact branch source |
| invalid Clojure source | `:invalid-source` |

Pure compatibility tests compare `outline-source` with the existing `:ls`
contract and prove that source text does not leak into outline records.

### Field-failure and real-program evidence

Add a faithful minimized migration fixture containing a named database writer
inside realistic metadata, comments, helper forms, and a reader-conditional
neighbor. The two motivating goals must pass in one command:

1. Show the named writer.
2. Show the form containing its observed line.

Dogfood against copied/current clj-surgeon source by showing `format-op-help`
by name and by an interior line. Assert structural fields and source content,
not brittle absolute line numbers in permanent tests.

### CLI, help, and remedy tests

- Exact documented name and line commands return EDN and exit zero.
- All refusal types return EDN and exit nonzero without stack traces.
- Global help lists `show-form` as read-only.
- Global and per-operation help expose `cat` only as an alias.
- `:cat` returns the same canonical operation, selector, source, and hash as
  `:show-form`; bare `:cat :file ...` refuses instead of dumping the file.
- Per-operation help documents exactly-one selector and `:platform`.
- Unknown `:get` with a form returns an executable `:show-form` remedy.
- Line-only `:find-subform` returns an executable `:show-form` remedy.
- Remedy `:command-args` reproduce the successful invocation, including a file
  path containing spaces.
- Help, README, changelog, and both skill files agree on the operation and plan
  application rule; anti-drift tests enforce the agent-facing text.

### Clean-context simulations

Run fresh, ephemeral Codex sessions with no conversation history and only the
installed CLI/skill plus realistic files:

1. “Show the complete top-level form named `format-op-help`.” Expected first
   task command: one `:show-form :form` call after optional global help.
2. “Show the form containing line N.” Expected first task command: one
   `:show-form :line` call after optional global help.
3. “Apply this reviewed plan.” Expected mutation command: one direct
   `:replace-subform! :plan`; no `apply_patch` or plan edit.
4. Start with the historically guessed `:get`. Expected behavior: read the
   returned remedy and execute its exact `:show-form` command without browsing
   every operation.
5. Find a form from distinctive text without knowing its name. Expected route:
   one `rg -n` lookup followed by `:show-form :line`, not a complete `:ls`.
6. Find repeated nested syntax without knowing its parent. Expected route: one
   file-wide `:grep-form`, no help detour and no invented `:inside` requirement.
7. Edit a value in a `case` clause. Expected route: select an independently
   readable contained expression, generate the plan in a standalone command,
   review it, then apply in a separate command.

Record command counts and repair any help or remedy ambiguity exposed by the
fresh sessions. Permanent deterministic assertions protect the wording and
structured fields that made the session succeed.

### Recorded clean-context results

The baseline session had no one-shot reader. It visited global help, `:ls`,
dependency help, and nested-search help while trying to recover the complete
form. In the first post-implementation sessions, both agents used
`:show-form` successfully and avoided text-range readers, but each performed a
redundant `:ls` preflight because the installed skill still instructed agents
to outline every large namespace first.

After the instruction was repaired, two new ephemeral sessions used exactly
one Clojure-source command each: one `:show-form :form` and one
`:show-form :line`. Neither used `:ls`, `sed`, `awk`, `head`, `tail`, or a
reconstructed line range on the source file. Both returned the same complete
`format-op-help` source and complete-file hash.

The historical `:get` scenario recovered in two CLI calls: the guessed command
failed nonzero with an executable remedy, and that exact `:show-form` command
succeeded without help. The reviewed-plan scenario used one direct
`:replace-subform! :plan` mutation and did not inspect, edit, or regenerate the
plan. A strict `:cat` alias was then installed and verified to preserve the
canonical operation and selected source while bare `:cat` refused.

A final fresh session was asked to treat clj-surgeon as a structural shell and
“cat” the named form. It selected `:cat :form` as its only Clojure-source
inspection, skipped `:ls`, and explicitly recognized `:show-form` as the
canonical result operation.

Two malformed-call experiments tested whether every error should print global
help. Bare `:cat` and the historical `:get` each recovered in exactly two CLI
calls from concise, local EDN guidance without `--help`. Global help would have
been a token wall. The durable policy is the smallest sufficient structured
error: missing arguments, the local contract, and executable remedies when a
high-confidence correction exists.

The next discovery experiments exposed three more instruction defects. A
semantic lookup printed the complete 40-form outline merely to identify one
line; the repaired route is `rg -n` followed by `:show-form :line`. A file-wide
structural query opened help because the documentation implied `:inside` was
required, even though the implementation already supported whole-file search;
`:grep-form` now makes that route explicit. Finally, a `case` edit invented a
synthetic wrapper around two sibling forms and then chained plan generation,
application, and verification. The selector recovered by targeting the
contained expression, but the chain erased the review decision boundary.
Help, README, and both skills now teach sibling syntax honestly and require a
standalone planning command followed by separately reviewed application.

## Documentation and Release Checklist

- [x] Global and `show-form --help` output
- [x] README read workflow and exact examples
- [x] `skills/clj-surgeon/SKILL.md`
- [x] legacy root `skill.md`
- [x] `CHANGELOG.md`
- [x] dated follow-up in the motivating ethnography
- [x] Captain's Log experiment record
- [x] anti-drift tests for operation name, arguments, remedies, and plan rule

## Verification Gates

1. Format only changed Clojure files with Standard Clojure Style.
2. Run targeted outline, show-form, help, and CLI tests.
3. Run clj-kondo on every changed Clojure source and test namespace.
4. Run `make test` and report test/assertion counts.
5. Run both documented commands on the real clj-surgeon source.
6. Run each executable remedy and compare its result with direct invocation.
7. Run `make install`; verify the installed CLI and repository-owned skill.
8. Run all seven clean-context simulations and record before/after command
   counts and any defects found.
9. Review the complete diff for unrelated formatting or weakened assertions.

All gates passed. Focused help, show-form, and structural-lens tests: 84 tests /
662 assertions. Full suite: 330 tests / 1,620 assertions. clj-kondo reported
zero errors and zero warnings.
The installed CLI, repository-owned skill symlink, direct selectors, strict
`:cat` alias, executable remedies, real-source dogfood, and seven clean-context
scenarios were all exercised.

An adversarial post-implementation review found eight contract gaps that the
first green suite missed. All became permanent improvements: option-specific
line coercion, canonical context on missing-file refusals, remedy validation,
bounded ambiguity evidence, explicit immutable project-alias input to the pure
core, support for the standalone `/` name, and honest start/interior/end plus
`.cljs` and complete CLI-refusal coverage. No test was weakened.

## Definition of Done

The feature is complete when a fresh agent can read one named or
line-containing top-level form with one documented command, every ambiguous or
invalid selection fails closed with stable EDN and nonzero status, the two
historical wrong commands provide executable recovery, reviewed plan
application requires no generic text edit, all agent-facing surfaces agree,
and targeted, full-suite, installed-tool, real-source, and fresh-session gates
pass without weakening an existing test.
