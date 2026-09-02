# Independent Verification: WRITE-REFUSAL-001 First Green

Date: 2026-08-30

## Verdict

**GO** for the implementation half at exact product candidate
`9af88fbae9ee720613599feaf8cf58432c5898bb`, tree
`6f9bc30316eb6417977c07c86caf8eb146dfbdb8`.

This verification found no contradiction with ratified
`MCP-OP-WRITE-REFUSAL-001`. It authorizes no install, reload, or shared-runtime
action.

## Identity and intent

- Product candidate: `9af88fbae9ee720613599feaf8cf58432c5898bb`
- Product tree: `6f9bc30316eb6417977c07c86caf8eb146dfbdb8`
- Producer receipt: `4bb5783894e487320e35fc746497b6d7cddda34a`
- Producer receipt tree: `03be51e44d8d000f918ae44bcaf20f9097347614`
- Independent worktree: detached and clean before the audit
- Ratification authority: `6d558cb3`, Gene's verbatim `Go`

The candidate design blob
`eb607755568365786f1f9da7530491823b0475d3` exactly matches the permanent
ratified design blob. The candidate specification blob
`fcdba0b4014226671fc5524bd8574f4c5a9c3b0e` differs from the permanent
ratified specification only by activating `MCP-OP-WRITE-REFUSAL-001` from
`[D]` to `[x]`. No normative requirement text drifted.

## Independent case matrix

| Case | Result |
|---|---|
| Complete form-scoped mismatch | PASS: exact aggregate, per-file, per-form, three ordered rows, and snapshot guards |
| Closed root identity | PASS: no invented owner and no per-form counts |
| Closed namespace identity | PASS: exact namespace owner identity |
| Zero matches | PASS: complete empty investigation, no truncation or authority |
| Row boundary | PASS: 128 rows returned; row 129 omitted with exact counts and inert continuation |
| Byte boundary | PASS: 32,640-byte pre-finalization candidate accepted; 32,641-byte candidate truncated, which maps to the ratified 32,768/32,769 boundary with the 128-byte reserve |
| Longest fitting UTF-8 prefix | PASS: one-row prefix was 1,360 bytes and fit; two-row result was 34,856 bytes and did not; returned/omitted were 1/1 |
| Fail-empty | PASS: oversized dynamic evidence produced the fixed output-budget envelope, no evidence map or dynamic error text, and no authority |
| Continuation binding | PASS: operation and snapshot changes alter `candidate_query_sha256`; ordered file changes alter `selector_sha256` |
| Both MCP entrances | PASS: evidence preserved `edit_clojure` and `apply_clojure_changes` exactly; both refusals left source unchanged |
| Default compiler path | PASS: candidate and frozen-red base produced identical result SHA-256 `41c3f3217b17206a20e5a769c37b1a7c8c68d30e8b5c73df6ecf359316fe7686` without write-refusal context |

## Independent falsifiers

Two duplicate compiler match records remained two evidence rows. The
projection did not silently deduplicate the compiler's known universe.

A body-leak probe used distinctive matcher and replacement values
`:secret-old-7f9d` and `:secret-new-4c2a`. Neither value appeared in the
serialized refusal. A recursive scan found no `next_call`, `prepared_request`,
replacement, selected-candidate, or candidate keys and zero truthy authority,
write-authority, or executable fields. The three compiler matches were all
reported and the source remained byte-identical.

## Red, green, and milestone evidence

- Frozen red `ff15b954`: 6 tests, 54 assertions, exactly 39 failures, 0 errors.
- Focused candidate: 6 tests, 54 assertions, 0 failures, 0 errors.
- Core: 647 tests, 5,562 assertions, 0 failures, 0 errors.
- Analyzer contract: 4 tests, 20 assertions, 0 failures, 0 errors.
- MCP: 319 tests, 3,656 assertions, exactly 2 failures, 0 errors. Both failures
  are the already-characterized load-sensitive cold-admission race in
  `cold-clj-kondo-admission-timeout-is-unverified`.
- The same cold namespace immediately passed alone: 7 tests, 50 assertions,
  0 failures, 0 errors.
- Exact clj-kondo: 0 errors, 0 warnings.
- MCP stdio smoke and the heap, admission-path, analyzer-target, cclsp,
  benchmark, retention, and evidence-manifest ancillary gates passed.

The milestone gate started at load averages `6.41 7.01 6.35`. Its nonzero
exit is fully explained by the two known cold-admission assertions; no
candidate-specific test failed.

## Scope

The audit changed no product, intent, or test file. It performed no install,
MCP reload, server restart, or shared publication.
