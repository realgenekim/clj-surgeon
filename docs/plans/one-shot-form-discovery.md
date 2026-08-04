# One-Shot Form Discovery and Verified Apply Receipts

**Status:** Accepted for implementation

## Outcome

Remove avoidable lexical-coordinate and structural-owner conversion calls from
the common Clojure read/edit loop without replacing broad `rg` discovery or
weakening the plan-review boundary.

The motivating clean-context benchmark found the same tax in all eight
tool-aware tasks where the containing form was initially unknown:

- distinctive text required `rg -n`, followed by `:show-form :line`;
- file-wide structural matches required `:find-subform`, followed by
  `:show-form :line`, because each match omitted its enclosing form name;
- successful plan application was followed by `rg`, `:show-form`, or `shasum`
  calls that repeated facts already checked by the hash-bound applier.

## Observable Contract

### Literal `:contains` selector

`show-form` and its strict `cat` alias accept exactly one of `:form`, `:line`,
or `:contains`:

```bash
clj-surgeon :op :show-form :file src/my/ns.clj :contains 'distinctive text'
clj-surgeon :op :cat :file src/my/ns.clj :contains 'distinctive text'
clj-surgeon :op :cat :file src/my/ns.clj :contains :finish
```

`:contains` is a nonblank, case-sensitive literal substring. It is not a
regular expression. It searches the exact source owned by top-level form
records, including attached comments, strings, and docstrings.

At the CLI boundary, the value following `:contains` remains raw literal text
instead of going through generic EDN coercion. Keyword-, boolean-, number-, and
collection-shaped text therefore works without an EDN-string workaround. The
pure selector API remains strict and refuses non-string programmatic values.

- If exactly one top-level form contains the literal, return the existing
  `:show-form` success record plus `:occurrence-count` and bounded occurrence
  locations.
- Multiple occurrences inside that same form still select one form.
- If more than one top-level form contains the literal, refuse with
  `:error-type :ambiguous-form`, bounded candidate locations, and no source
  dump.
- If no top-level form contains the literal, refuse with
  `:error-type :contains-not-found`.
- Empty/non-string values refuse with `:invalid-contains-selector`.
- Conflicting selectors refuse with the existing `:conflicting-selectors`.
- `:platform` filters candidates before uniqueness is decided.
- Success and refusal are read-only and always include the complete-file
  `:source-hash`.

### File-wide structural ownership

Every `:find-subform` / `:grep-form` match includes `:inside`, naming the
enclosing top-level definition when one is mechanically available. This field
is directly reusable as the `:inside` selector for `:replace-subform`.

The existing top-level query field, paths, addresses, source, line data,
match-count, and hash remain unchanged. Scoped queries continue to report the
same `:inside` value at the result level and per match.

### Verified apply receipt

After `:replace-subform!` atomically writes the planned result, it reads the
file back and compares the exact bytes with the planned result hash. Success
returns:

- `:operation :replace-subform!`;
- `:file`;
- `:source-hash` and `:result-hash`;
- the single `:applied-edit` without the future complete-file source;
- `:verified` with `:whole-file-parsed true`, `:atomic-write true`, and the
  matching `:read-back-hash`.

A read-back mismatch returns nonzero failure data. Planning and application
remain separate commands. The receipt proves exact structural replay; it does
not replace repository formatting, linting, compilation, or tests. The reviewed
plan is the edit-level diff; do not reread the edited or neighboring forms to
reproduce receipt evidence. Review an aggregate Git diff only when task context
already establishes a worktree or explicitly requests it; do not probe `.git`
merely to repeat edit-level evidence.

## Behavior Matrix

| Dimension | Cases |
|---|---|
| selector | form, line, contains, none, every conflicting pair/triple |
| contains value | nonblank string, empty, whitespace, keyword, number |
| literal result | zero forms, one form/one occurrence, one form/many occurrences, many forms |
| source ownership | code, string, docstring, attached comment, unattached gap/comment |
| form shape | named, unnamed, metadata/comment preservation |
| platform | CLJ, CLJS, ambiguous CLJC, explicit CLJC platform |
| ambiguity | bounded candidates and truncation |
| CLI | canonical op, cat alias, help, EDN stdout, success/nonzero refusal |
| structural ownership | scoped, file-wide, named owner, unnamed top-level form |
| apply | success, stale source, invalid plan/result, atomic-write failure, read-back mismatch |

## Real-Program Evidence

- The exact phrase `Per-command help` must select `format-op-help` from the
  896-line `src/clj_surgeon/core.clj` in one source command.
- Two identical `(assoc state :status :done)` expressions in the benchmark
  fixture must be returned with distinct owners, `transition` and
  `unrelated-finish`.
- A fresh Codex session must change only the `:finish` expression without
  using `rg`, `sed`, or a line-number bridge.

## Non-Goals

- Do not add regex semantics or replace broad, cross-file `rg` discovery.
- Do not add fuzzy or best-match selection.
- Do not return every matching form's full source on ambiguity.
- Do not combine plan generation and application.
- Do not claim semantic verification from parse/hash verification.
- Do not add macro-specific `case`, `cond`, binding, or map judgment.

## User-Facing Surfaces

Update global/per-operation help, README, installed and legacy skills,
CHANGELOG, the benchmark route card, and anti-drift tests together. The quick
route must contain exact executable syntax; operation names alone are not
sufficient.

## Verification Gates

1. Add failing pure tests for the complete selector/ambiguity matrix.
2. Add CLI and help assertions for `:show-form` and `:cat :contains`.
3. Add structural-owner tests with the motivating duplicate expression.
4. Add receipt success and failure tests; every refusal preserves bytes.
5. Format changed Clojure files and run targeted tests.
6. Run clj-kondo and the complete test suite without weakening assertions.
7. Install the CLI/skill and run the documented real invocation.
8. Run fresh isolated Codex sessions for semantic read and exact edit tasks;
   compare commands, tokens, latency, correctness, and review boundaries with
   the recorded pre-feature sessions.
