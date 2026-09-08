# Row 3: frozen byte and layout rules cannot both hold

**Verdict: not admitted under Sol's original row-3 design.** The standalone
schema is built on `astra/namespace-split`; the selected eight-file task cannot
satisfy its frozen O4/O5 acceptance rules. This is no performance or routing win.

Marvin golden `9f9cf61441a7f2a984b55226d5d1ca6cbeb6706f`, parent
`d170f3d5edea6faa39396ea8b3418e29b2e2b4b1`, supplies two contradiction classes:

- Both `capture_archive.clj` and `reducer_session.clj` put the new target on
  `(:require`'s opener line. Deleting that whole line removes the opener and
  produces an invalid seed. Preserving the opener or reconstructing the parent's
  hanging header requires an explicit construction amendment.
- `codex_app_server.clj` has only three `clojure.*` requires, with `)` on its
  final `[clojure.string :as str])` line. O5 places the target last. A new line
  after that line is outside the clause; a line before it is out of order;
  moving the closer changes an inherited byte forbidden by O4.

The faithful header fixtures and independent oracle retain these facts. The
original requested positive eight-header witness initially failed; it was not
made green by weakening the oracle. The final strict tests distinguish the
natural atomic refusal from seven expressible header transformations, using
parent headers for the two opener cases. This smaller capability evidence is
explicitly not the row-3 fixture or an accepted D result.

The new MCP `require_change` and CLI `:require-change!` share one pure compiler
and the existing extraction transaction, synchronous proof and durable guarded
undo. The registered [REQUIRE-CHANGE-001..014](../intent/require-change/require-change-specs.md)
own ordered alias reuse/allocation, explicit adds/removals/counts/hashes, stable
comment-preserving line splices, source confinement, refusal/rollback and bounded
receipts. The existing compact symbol/require pair remains unchanged.

One untimed in-image public MCP handler invocation on the literal frozen seed
returned `require-parse-failed`, `mutation_attempted=false`,
`source_unchanged=true`, `verification_complete=false`; independent byte replay
confirmed all eight source files unchanged. The parser refusal ran no profile
commands. This is retained prerequisite refusal evidence, not D1 or a cohort.
The external grader's exact lines were:

```text
O2 GOLDEN: FAIL
O4 ZERO-CHURN: FAIL
O5 REQUIRE-BLOCK PAPERCUTS: 9
```

These are failures of an incomplete candidate; they are not a successful mutation
misreported as accepted. The exact historical golden loaded successfully; its
serialized lint baseline contains 37 errors, 99 warnings and 23 infos. Candidate
load, both full Marvin suites, candidate lint delta and blind acceptance remain
unearned because no admissible candidate exists under this design.

The first old-schema warm witness run had 52 failures / 0 errors. The final
focused strict capability run passed 21 tests / 209 assertions. The new cold
boundary lane passed 12 tests / 89 assertions with no isolation violation.
The final `make test` passed: 814 JVM tests / 10,373 assertions and 887 Babashka
tests / 7,853 assertions, with zero failures or errors. The independent Python
oracle's four tests passed. The independent help-only caller review passed after
concrete source-pinning and proof-review gaps were repaired. Full gate attempts, all negative results,
request/receipt/profile, exact hashes and the blocked freeze are retained under
`/var/tmp/forge/plan2/cellC/`; the final gate and commit identity are in
`astra-row3-report.md`. No cohort, shared installation, push or merge was performed.

Recorded: 2026-09-08T08:56:05.125982+00:00
