# Failure-Atomic Namespace Extraction

**Status:** Implemented and live-installed 2026-08-10
**Motivating issue:** `clj-surgeon-x0p`

## Outcome

`:extract!` compiles the complete future source and target namespaces from one
source snapshot, parses both candidates before any write, commits them as one
recoverable transaction, verifies both files after writing, and returns a
reversible receipt. It never removes a neighboring form merely because that
form follows an extracted region without a blank line.

The production regression is a valid large namespace where several adjacent
early forms are extracted and the next unrelated `defn` starts immediately
after the final extracted form. The old implementation applied stale line
ranges one form at a time and unconditionally removed one extra line. It
deleted the next `defn` opener and then inserted a malformed require.

## Bitter-Lesson Boundary

The model chooses the forms, target namespace, architecture, and migration
order. The kernel owns only mechanical extraction: exact ranges, require
syntax, candidate parsing, snapshot guards, atomic replacement, rollback,
read-back verification, and receipts.

This work does not infer minimal requires, rewrite callers, select extraction
boundaries, or decide namespace ownership. Those remain separate decisions.

## Public Contract

Planning remains non-mutating:

```bash
clj-surgeon :op :extract :file src/app.clj \
  :forms '[helper render]' :to src/app/render.clj
```

Application accepts an optional explicit receipt path:

```bash
clj-surgeon :op :extract! :file src/app.clj \
  :forms '[helper render]' :to src/app/render.clj \
  :receipt-out /tmp/app-render-extraction.edn
```

Success returns source and target hashes, parse/read-back verification, the
actions performed, and the receipt path when requested. The receipt contains
both original and result hashes plus enough guarded inverse data to restore the
source and remove the newly created target only while both still equal the
recorded extraction result.

Refusal returns a stable `:error-type`, `:source-unchanged true`, and
`:target-unchanged true`. Existing target files refuse; extraction never
overwrites them. A receipt path must end in `.edn` and must not alias either
source file.

## Safety Invariants

- Read and hash the source snapshot once before candidate construction.
- Compute all removals against that original snapshot; never reread and apply
  stale line positions incrementally.
- Remove one trailing line only when that line is actually blank.
- Preserve every unrelated source byte, including the next form, comments,
  metadata, reader conditionals, and line endings.
- Add the target require with rewrite-clj syntax operations, never a regular
  expression over indentation.
- Parse both complete future namespaces before writing either file.
- Refuse an existing target file.
- Recheck the source hash immediately before the first write.
- Use atomic per-file replacements. If the second write or read-back proof
  fails, restore the original source and remove only the exact target candidate
  created by this transaction.
- Parse and hash both read-back files before reporting success.
- Never publish a receipt before the source and target verification succeeds.

## Implementation Shape

Extract a public pure candidate builder that accepts source text, selected form
ranges, target namespace, and alias. It returns source and target candidates or
structured refusal data. Use retained line chunks or source offsets so newline
bytes remain exact.

Reuse `clj-surgeon.cljc.require-ops/insert-into-require` for syntax-aware
require insertion and `rewrite-clj.parser/parse-string-all` for complete-source
validation. The imperative shell uses `file-ops/atomic-write!`, explicit
snapshot guards, handled rollback, and a staged receipt.

## Test Plan

| Dimension | Required cases |
|---|---|
| Neighbor layout | blank line after extraction; immediate next `defn`; attached comment on next form |
| Selection | singleton; several adjacent forms; disjoint forms; missing form |
| Source syntax | comments, metadata, CRLF/LF, no final newline, reader conditionals |
| Require shape | existing require; no require clause; alias collision; valid indentation after formatting |
| Target state | absent succeeds; existing target refuses unchanged |
| Snapshot | current hash succeeds; stale source refuses before write |
| Candidate validation | invalid source candidate; invalid target candidate; neither file written |
| Commit failure | first write fails; second write fails and rolls back; rollback failure is explicit |
| Read-back | wrong source or target hash refuses and recovers |
| Receipt | optional path; invalid path; aliasing path; staged only after verification; guarded inverse |

Pure tests use literal source strings and parsed ranges. The field regression
uses a minimized valid fixture derived from the 4,541-line production
namespace: fifteen adjacent early forms followed immediately by an unrelated
documented `defn`. It proves the starting fixture parses, both candidates
parse, the neighbor remains byte-for-byte, and the require is syntactically
valid.

Boundary tests use temporary files only for atomic write, rollback, read-back,
and receipt publication. CLI tests cover parsing, help, EDN output, successful
exit, refusal exit, and the documented command.

## Documentation and Release Checklist

- Update `:extract` / `:extract!` help and README with the receipt and
  no-overwrite contract.
- Update the advanced skill reference if the invocation changes.
- Add the production incident and final evidence to the Captain's Log.
- Close `clj-surgeon-x0p` only after the faithful fixture and real copied
  program invocation pass.

## Verification Gates

1. New field regression fails on the old implementation for the reported
   orphaned-next-form reason.
2. Pure candidate matrix passes.
3. Focused extraction and CLI suites pass.
4. Changed Clojure files are formatted.
5. clj-kondo is clean for changed namespaces.
6. `make test` passes without weakened assertions.
7. One copied real-program extraction returns parse/read-back proof and a
   reversible receipt; applying the inverse restores the original hashes.

## Definition of Done

The issue is complete when the production-derived adjacent-form fixture cannot
lose its neighboring `defn`, every pre-commit failure leaves both paths
unchanged, every handled mid-commit failure restores the starting state, and a
successful extraction returns independently verified source and target hashes
plus a guarded reversible receipt.

## Completion Evidence

The production-shaped fixture moves fifteen adjacent forms followed immediately
by an unrelated documented `defn`. Both future files parse and verify. The
guarded inverse restores the exact original SHA-256 and proves the target
absent. Existing-target, stale-source, second-write rollback, repeated-undo,
and drift refusals are covered by focused tests.

Final gates: clj-kondo reported 0 errors and 0 warnings; the main suite passed
612 tests and 5,293 assertions; the MCP suite passed 127 tests and 1,063
assertions; full `make test` passed. `clj-surgeon-x0p` was closed only after the
copied-program CLI round-trip succeeded.
