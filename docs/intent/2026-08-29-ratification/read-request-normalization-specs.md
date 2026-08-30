---
parent: read-request-normalization-design
prefix: MCP-OP-READ-NORM
status: draft-for-ratification
---

# Closed Inspect Request Normalization Specifications

IDs in this draft are stable and must not be reused if a requirement is
deleted. These requirements are not active until the parent design and this
registry are ratified.

## Request IDs

- [ ] **MCP-OP-READ-NORM-001**: When every request in one typed `inspect_clojure` batch supplies a nonblank `id`, clj-surgeon shall preserve every supplied ID in input order and shall retain the existing duplicate-ID refusal; when no request supplies `id`, clj-surgeon shall assign deterministic call-local IDs `request-1` through `request-N` in input order before ordinary request validation.
- [ ] **MCP-OP-READ-NORM-002**: If one typed `inspect_clojure` batch contains both a request with `id` and a request without `id`, clj-surgeon shall refuse the complete batch with reason `:mixed-request-ids` before snapshot capture or request evaluation, shall publish no source, result, continuation, basis, or executable next call, and shall report source unchanged and read not started.
- [ ] **MCP-OP-READ-NORM-003**: When clj-surgeon assigns a call-local request ID, every success row, selector failure, completed or pending continuation ID, retry-template request, retry hole, and concise label for that subrequest shall use the identical generated ID; the ID shall grant no cross-call, snapshot, selector, basis, or write authority.

## Operation inference

- [ ] **MCP-OP-READ-NORM-004**: When one typed inspect subrequest omits `operation` and supplies exactly the complete forms-owned shape consisting of `file`, a non-empty `forms` array, exact `expect.forms`, optional `include_source`, and optional `id`, clj-surgeon shall normalize that request to `operation=forms` before ordinary request validation and shall produce the same selected forms, source hashes, result rows, and refusal behavior as the corresponding explicit forms request.
- [ ] **MCP-OP-READ-NORM-005**: If a typed inspect subrequest omits `operation` and does not supply the complete unambiguous forms-owned shape, including a file-only, match, xray, partial-forms, unknown-field, or mixed-variant shape, clj-surgeon shall refuse the complete batch with reason `:operation-required` before snapshot capture or request evaluation and shall not default, rank, or infer another operation.

## Falsifiers

| ID | Defensible opposite to test | Required witness families |
|---|---|---|
| `MCP-OP-READ-NORM-001` | Server IDs may depend on file, operation, or prior calls. | Two files with equal operations; repeated file with different operations; two separate calls reusing the same call-local sequence; explicit unique and duplicate IDs. |
| `MCP-OP-READ-NORM-002` | Missing IDs may be filled around caller IDs without ambiguity. | Omitted then explicit; explicit then omitted; generated spelling colliding with an explicit ID; loader invoked zero times. |
| `MCP-OP-READ-NORM-003` | Positional order is enough even when projections carry IDs. | Success; first-request failure; later selector failure with completed siblings; retry-template holes; two calls with identical generated strings. |
| `MCP-OP-READ-NORM-004` | Dominant frequency alone makes `forms` a safe blind default. | Explicit versus omitted operation on identical complete forms requests; metadata-only forms; selector refusal; exact source and result hashes. |
| `MCP-OP-READ-NORM-005` | An operation-less request can choose the nearest compatible variant. | File only; match with and without `inside`; xray expression; missing forms; missing `expect`; cross-variant fields; unknown field; loader invoked zero times. |
