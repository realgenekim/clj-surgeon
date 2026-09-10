---
parent: insert-forms-design
prefix: INSERT-FORMS
status: implemented
---

# insert_forms v1 atomic requirements

Source authority: 2026-09-10-insert-forms-contract-astra.md, sections 1–9.
IDs are permanent. The original eighteen witnesses remain required; fix round 1 adds six named regression witnesses across four namespaces.

- [x] **INSERT-FORMS-001**: When a unique root owner is selected, the planner shall insert at its requested adjacent boundary.

  Witness: `insert-forms-after-prefix-named-defn`. Misreading: infer success from parsing or a write alone. Boundary: the exact variants frozen in the named witness and the source contract remain binding.

- [x] **INSERT-FORMS-002**: When a testing path is supplied, the planner shall resolve each label only among direct children of the selected parent.

  Witness: `insert-forms-deftest-nested-testing`. Misreading: infer success from parsing or a write alone. Boundary: the exact variants frozen in the named witness and the source contract remain binding.

- [x] **INSERT-FORMS-003**: When payload syntax is malformed anywhere before EOF, the planner shall refuse with :payload-parse-error.

  Witness: `insert-forms-unbalanced-payload-refuses`. Misreading: infer success from parsing or a write alone. Boundary: the exact variants frozen in the named witness and the source contract remain binding.

- [x] **INSERT-FORMS-004**: When the captured bytes differ from the guard digest, the planner shall refuse with :source-hash-mismatch.

  Witness: `insert-forms-stale-hash-refuses`. Misreading: infer success from parsing or a write alone. Boundary: the exact variants frozen in the named witness and the source contract remain binding.

- [x] **INSERT-FORMS-005**: When source contains non-code owner lookalikes, the planner shall exclude those lookalikes from owner matching.

  Witness: `insert-forms-comment-string-anchor-decoys`. Misreading: infer success from parsing or a write alone. Boundary: the exact variants frozen in the named witness and the source contract remain binding.

- [x] **INSERT-FORMS-006**: When a selection does not have exactly one match, the planner shall return the exact cardinality refusal.

  Witness: `insert-forms-anchor-cardinality`. Misreading: infer success from parsing or a write alone. Boundary: the exact variants frozen in the named witness and the source contract remain binding.

- [x] **INSERT-FORMS-007**: When a body boundary is requested, the planner shall count only immediate effective body children.

  Witness: `insert-forms-body-boundaries`. Misreading: infer success from parsing or a write alone. Boundary: the exact variants frozen in the named witness and the source contract remain binding.

- [x] **INSERT-FORMS-008**: When a defn body is selected, the planner shall resolve its declared arity after excluding recognized header forms.

  Witness: `insert-forms-defn-headers-and-arities`. Misreading: infer success from parsing or a write alone. Boundary: the exact variants frozen in the named witness and the source contract remain binding.

- [x] **INSERT-FORMS-009**: When a payload is supplied, the planner shall require its exact effective form count in source order.

  Witness: `insert-forms-payload-count-and-order`. Misreading: infer success from parsing or a write alone. Boundary: the exact variants frozen in the named witness and the source contract remain binding.

- [x] **INSERT-FORMS-010**: When a splice is planned, the planner shall preserve original trivia bytes while indenting only payload lines outside literals.

  Witness: `insert-forms-trivia-and-indentation`. Misreading: infer success from parsing or a write alone. Boundary: the exact variants frozen in the named witness and the source contract remain binding.

- [x] **INSERT-FORMS-011**: When an input exceeds a declared preparse bound, the operation shall refuse with :limit-exceeded.

  Witness: `insert-forms-reader-safety-and-limits`. Misreading: infer success from parsing or a write alone. Boundary: the exact variants frozen in the named witness and the source contract remain binding.

- [x] **INSERT-FORMS-012**: When a portable read receipt is used as guard, the planner shall require its complete path-bound digest projection.

  Witness: `insert-forms-portable-read-receipt`. Misreading: infer success from parsing or a write alone. Boundary: the exact variants frozen in the named witness and the source contract remain binding.

- [x] **INSERT-FORMS-013**: When an insertion is accepted, preservation evidence shall inventory every original root expression by ordinal.

  Witness: `insert-forms-other-top-level-hashes`. Misreading: infer success from parsing or a write alone. Boundary: the exact variants frozen in the named witness and the source contract remain binding.

- [x] **INSERT-FORMS-014**: When commit encounters a failure, the operation shall report the proven mutation outcome without overwriting intervening external bytes.

  Witness: `insert-forms-atomic-multiform-and-race`. Misreading: infer success from parsing or a write alone. Boundary: the exact variants frozen in the named witness and the source contract remain binding.

- [x] **INSERT-FORMS-015**: When a target path violates confinement or regular-file identity rules, the operation shall refuse without changing target bytes.

  Witness: `insert-forms-path-confinement`. Misreading: infer success from parsing or a write alone. Boundary: the exact variants frozen in the named witness and the source contract remain binding.

- [x] **INSERT-FORMS-016**: When a receipt is projected, its splice accounting shall equal the actual UTF-8 bytes and line ranges.

  Witness: `insert-forms-receipt-accounting`. Misreading: infer success from parsing or a write alone. Boundary: the exact variants frozen in the named witness and the source contract remain binding.

- [x] **INSERT-FORMS-017**: When any v1 result is returned, it shall omit terminal_response.

  Witness: `insert-forms-terminal-response-eligibility`. Misreading: infer success from parsing or a write alone. Boundary: the exact variants frozen in the named witness and the source contract remain binding.

- [x] **INSERT-FORMS-018**: When either CLI request-file spelling is invoked, it shall return the same operation result as the MCP entrance.

  Witness: `insert-forms-cli-mcp-parity`. Misreading: infer success from parsing or a write alone. Boundary: the exact variants frozen in the named witness and the source contract remain binding.


- [x] **INSERT-FORMS-019**: When candidate structure differs from the requested sibling insertion, the operation shall refuse :candidate-structure-mismatch before publication.

  Witness: `insert-forms-candidate-structure-refuses`. Misreading: parsing alone proves placement. Boundary: a one-byte offset defect must fail even with valid candidate syntax.

- [x] **INSERT-FORMS-020**: When source or candidate parsing fails, the operation shall refuse with the corresponding parse error before publication.

  Witness: `insert-forms-parse-errors-refuse`. Misreading: a parse failure can become generic invalid-request. Boundary: exact error type and unchanged disk bytes.

- [x] **INSERT-FORMS-021**: When inserting siblings, the planner shall reproduce the existing gap without adding redundant blank lines or orphaning body closers.

  Witness: `insert-forms-existing-separators`. Misreading: always append a newline. Boundary: top-level blank gaps, attached comments, body first/last/empty, payload ending in a newline, CRLF.

- [x] **INSERT-FORMS-022**: When a validation refusal is returned, its remedy shall name the missing decision for that refusal type.

  Witness: `insert-forms-refusal-remedies`. Misreading: a generic revise-request sentence suffices. Boundary: stale guards need a fresh read; ambiguous arities name bounded candidates.

- [x] **INSERT-FORMS-023**: When refusal candidates are returned, each shall include kind, name, and line.

  Witness: `insert-forms-candidate-envelope`. Misreading: byte offsets replace owner identity. Boundary: owner, testing-label, and arity candidates retain the ten-entry cap.

- [x] **INSERT-FORMS-024**: When recovering a planned receipt, the recovery reader shall classify the target by comparing its digest with result_hash before source_hash.

  Witness: `insert-forms-planned-receipt-recovery`. Misreading: planned means no write and permits blind replay. Boundary: candidate present, original present, and unrelated bytes are distinct verdicts.
