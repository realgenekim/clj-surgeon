# Containing-line structural root

**Status:** Implemented and verified on 2026-08-04
**Motivating incident:** [A live production session split the read and edit
verdicts](../observations/2026-08-04-captains-log-one-read-surface.md#a-live-production-session-split-the-read-and-edit-verdicts)

## Outcome

`:xray` and `:edit` can start at the top-level form that contains a known
one-based source line. The caller can then navigate to one nested leaf without
naming the enclosing form.

The primary read and edit surface is:

```text
ls / cat / grep-form / xray / deps  →  edit plan  →  apply  →  receipt
```

The README teaches only the canonical operation names. Existing command aliases
remain accepted and appear once in the README compatibility note.

## Bitter-Lesson Boundary

The feature adds one general structural root. It does not infer that a custom
macro defines a var, guess the macro's name position, or add a built-in entry
for one project macro.

Projects can continue to configure custom defining forms. The containing-line
root covers unconfigured forms, anonymous top-level forms, and unfamiliar
macros without encoding project semantics.

The caller still decides which line, descendant, and replacement express the
intended change. The tool supplies deterministic containment, exact cardinality,
lossless replacement, hashes, and refusal data.

## Public Contract

The Clojure expression surface gains `(line N)`:

```clojure
(-> (line 42)
    (match '(old-reader account-id))
    (replace '(new-reader account-id)))
```

The EDN query surface gains `[:line N]`:

```clojure
[[:line 42]
 [:find (old-reader account-id)]
 [:replace (new-reader account-id)]]
```

`[:line N]` must be the first query step. `N` must be a positive integer. The
step selects one top-level form whose source or attached comment contains that
line. It does not select every node on the line.

If one top-level form contains the line, later steps operate only inside that
form. If no form contains the line, the operation refuses with
`:line-not-in-form`. If reader-conditional ranges overlap, the operation
refuses with bounded candidate evidence instead of choosing one branch.

Literal `:xray` returns the selected top-level source. `:edit` uses the same
root and existing transformation contract. Without `:expect`, `:edit` remains
plan-only. With an exact literal `:expect`, it can apply one guarded leaf
replacement in the same invocation.

The receipt includes `[:line N]` in the query and trace. A successful leaf
replacement changes only the selected concrete-syntax node. It preserves all
surrounding whitespace, comments, metadata, reader syntax, and file
permissions.

## Canonical operation names

README examples and primary guidance use only:

- `:ls` for top-level inventory;
- `:cat` for one known top-level form;
- `:grep-form` for known syntax with an unknown owner;
- `:xray` for structural relationships and computation;
- `:edit` for plans and guarded edits.

Compatibility aliases remain runtime inputs. This feature does not remove or
change their behavior.

## Shell-safe selectors

Shell command examples must single-quote arbitrary form names and Clojure
source arguments. Names can contain shell metacharacters such as `>`, `?`,
`*`, and `$`.

```bash
clj-surgeon :op :cat :file src/app.clj :form 'response->view'
```

Machine-oriented remedies must continue to provide argument vectors. Any
copyable shell command must quote unsafe arguments. A documented command must
not create an unintended file when a form name contains `>`.

## Safety Invariants

1. Invalid line roots refuse before plan or source writes.
2. Gap lines and overlapping ranges never select the nearest or first form.
3. A line root never expands a replacement beyond the terminal selected node.
4. Zero or multiple descendant matches preserve the existing refusal types.
5. `:expect` compares the exact selected leaf, including comments, metadata,
   reader syntax, and token spelling.
6. A successful guarded edit verifies the plan artifact, whole-file parse,
   atomic write, read-back hash, and existing file permissions.
7. Existing named-form, navigation, span, partition, and transform behavior
   remains unchanged.
8. Existing tests are retained. New tests increase the permanent contract.

## Implementation Shape

Add `line` to the capability-limited edit/X-ray builders and SCI bindings. It
produces `[[:line N]]` and performs no I/O.

Extend the pure structural query parser to accept `[:line N]` only at index
zero. Extend the source index with lossless top-level containment ranges,
including attached comments and reader-conditional platform evidence. Apply
the root before descendant navigation.

Keep the existing structural query engine, plan builder, executor, outline
parser, and single-form reader. Do not add a second parser or a custom-macro
heuristic.

Update the canonical operation guidance in README, help, and the shared skill.
Retain compatibility behavior and tests.

## Adversarial Review

### Stale line selects a different form

A line is a physical locator. If a caller reuses an old line after unrelated
edits, the line can identify another form. The tool cannot infer the caller's
former intent.

Decision: retain the existing safety split. Plan-only edits require diff
review. A one-call edit requires an exact leaf `:expect`; the caller explicitly
asserts the current physical location and selected before-state. The plan and
apply use the same complete-file source hash.

### The same expected leaf exists in the wrong form

An exact leaf expectation cannot distinguish two semantically different forms
that contain identical syntax.

Decision: do not claim semantic identity. The line is part of the caller's
declared selector. Documentation must describe it as a physical root. Callers
that require semantic identity must configure the defining macro or use a
named form.

### Comments precede the defining form

Selecting a line in an attached comment must agree with `:cat :line`. Selecting
a blank gap must refuse. The implementation must reuse or exactly match the
single-form reader's attachment rules.

### Reader-conditionals expose overlapping ranges

Two branch-local forms can share physical container ranges. The query must
retain platform evidence and refuse ambiguity. It must not pick the first
branch or silently prefer `:clj`.

### Custom macros have arbitrary name positions

Some macros place a route, keyword, or options map before a logical name.
Automatic second-element naming would create false identities.

Decision: no heuristic. Project configuration remains the semantic extension
point. The line root is deliberately syntactic.

### Broad replacements destroy layout

Replacing a complete `let` with a one-line form is legal but can be poor
source surgery.

Decision: the field regression selects and replaces one nested call. The test
asserts the complete expected file bytes, not only semantic equality. Help
recommends narrowing the selector when only one leaf changes.

### Shell parses selector text before the CLI

An unquoted `>` can redirect the CLI receipt into a file before clj-surgeon
starts.

Decision: quote all arbitrary selector examples. Add a shell-level regression
that executes the documented form-name command in a temporary directory and
asserts that it creates no extra file.

### A general root makes the shortcut operation redundant

Even with `(line N)`, file-wide structural search remains a distinct common
route. It starts from known syntax when the owner and line are both unknown.

Decision: retain `:grep-form` in the primary API.

## Test Plan

### Pure behavior matrix

| Case | Expected result |
|---|---|
| line at top-level start, interior, or end | the same one form |
| line in attached comment | the attached form |
| line in blank gap | `:line-not-in-form` |
| zero, negative, fractional, string, or missing line | `:invalid-query` |
| `[:line N]` after another step | `:invalid-query` |
| two root steps | `:invalid-query` |
| unnamed top-level macro | selected by containment |
| three identical leaves in three owners | only the line-selected owner is searched |
| two identical leaves inside the selected owner | `:ambiguous-match` |
| no matching leaf inside the selected owner | `:no-match` |
| CLJC overlapping branch ranges | bounded ambiguity refusal |
| literal X-ray line root | exact source and trace |
| computed X-ray line root | ordinary data and compact evidence |
| plan-only line-root edit | plan saved; source unchanged |
| matching `:expect` | one leaf changes; verified apply |
| mismatching `:expect` | source and existing plan unchanged |

### Field-failure regression

Add an anonymized fixture with:

- three custom top-level macro calls that have no project configuration;
- the same reader call in all three owners;
- a comment beside the middle call;
- multiline bindings and body formatting.

The regression targets the middle owner by line, replaces only the reader
call, and asserts the complete expected bytes. The comment and multiline
layout must remain unchanged.

### Shell boundary

Run the documented `:cat :form 'source->target'` command through a real shell
inside a temporary directory. Assert exit behavior, selected form, and absence
of an unintended `target` file.

## Documentation and Release Checklist

- Add `(line N)` and `[:line N]` to `:xray` and `:edit` help.
- Teach line roots and narrow leaf replacement in README.
- Keep only canonical names in the README body; list compatibility aliases once
  at the bottom.
- Add `:grep-form` to the canonical README list.
- Update the canonical Codex and Claude skill package without exceeding its
  line ceiling.
- Update the advanced reference if line-root detail does not fit the core
  skill.
- Add a changelog entry.
- Record the anonymized field result in the Captain's Log.

## Verification Gates

1. Run the repository formatter on every changed Clojure file.
2. Run targeted pure, CLI, help, install, and skill-drift tests.
3. Run clj-kondo on changed Clojure source and tests.
4. Run `make test` without weakening or deleting an existing test.
5. Run a real CLI plan-only trial and a guarded-apply trial on the fixture.
6. Run the documented shell-safe form-name command through a real shell.
7. Run the agent-writing diagnostic on changed agent-facing documents.
8. Run `make install` from the committed tree and verify installed receipts.
9. Give a clean agent the anonymized edit task and require the exact one-leaf
   result without help or textual fallback.

## Definition of Done

The feature is complete when an unconfigured custom top-level macro can be
selected by containing line, one repeated nested call can be changed with an
exact guarded leaf edit, all surrounding bytes remain unchanged, shell-safe
selectors create no artifacts, canonical guidance contains no legacy names
outside the compatibility note, and every existing plus new test passes.

## Verification Result

- The formatter completed on every changed Clojure file.
- clj-kondo reported zero errors and zero warnings on changed source, tests,
  and the real-program-derived fixture. The fixture also executed in Babashka.
- The focused gate passed 126 tests and 1,880 assertions.
- The complete repository gate passed 478 tests and 3,756 assertions. All
  benchmark harness, retention-policy, and evidence-manifest self-tests passed.
- Strict agent-text lint reported zero errors for the canonical 90-line skill.
  Clear-mode README lint reported zero errors.
- A real CLI trial returned one literal X-ray match and one verified guarded
  edit receipt. The diff replaced only the target call.
- A fresh-context agent completed the task with its first guarded edit call,
  no help call, no source read, and no text fallback. Byte comparison found
  only the three bytes that changed `old` to `new`.
