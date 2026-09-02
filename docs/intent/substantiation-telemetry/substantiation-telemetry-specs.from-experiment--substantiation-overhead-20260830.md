---
parent: substantiation-telemetry-design
prefix: MCP-OP-SUBST
status: 'ratified in advance (Gene, 2026-08-30, verbatim: "Go on all!!!")'
client_metadata_privacy: 'decided A (conn, 2026-08-30, under Gene window authority; subject to Gene override at review)'
---

# Substantiation telemetry specifications

These IDs are active and stable. They must not be reused if a requirement is
removed.

## Ledger and privacy

- [x] **MCP-OP-SUBST-001**: When substantiation telemetry is enabled, every public MCP tool call shall append exactly one canonical start record before execution and one canonical finish record after domain completion under one server-owned call ID; sequence shall be strict, every record shall bind the prior record SHA-256, and a missing, duplicate, reordered, reused-ID, or broken-chain record shall make report input invalid rather than disappear from the denominator.

- [x] **MCP-OP-SUBST-002**: A substantiation record shall contain only closed enums, booleans, counts, timings, allowlisted field presence, and session-local HMAC-SHA-256 identity tokens. It shall never contain source, prose, path, owner or namespace text, matcher, replacement, command, URL, credential, account, receipt, raw request, raw response, raw client metadata, public content hash, or hidden reasoning. A plain digest of a low-cardinality subject shall refuse privacy validation.

- [x] **MCP-OP-SUBST-003**: At server start, substantiation telemetry shall create a new private append-only segment and in-memory random HMAC key, publish only its key ID, refuse an existing segment, and never edit, truncate, compact, or reuse an active segment. Retention shall not delete an active or marker-referenced segment.

- [x] **MCP-OP-SUBST-004**: If a start append fails, the tool shall not execute. If a finish append fails after domain completion, the existing domain result shall remain the only public result, the substantiation state shall latch unhealthy, one structured alarm shall reach stderr and health, the durable unmatched start shall remain, and every later tool call shall refuse before execution until an operator starts a new segment.

## Caller and call shape

- [x] **MCP-OP-SUBST-005**: The observer shall derive transport session, client name, and client version only from the server exchange and shall record each as a session-local HMAC identity token, never raw metadata. It shall record caller model with its provenance when an authenticated transport exposes it and otherwise record exactly `unknown` with source `not-exposed`; request fields shall never assert caller, session, turn, model, or telemetry authority. This client-metadata clarification is SUBST-CLIENT-1 Option A, conn-decided on 2026-08-30 under Gene's window authority and subject to Gene override at review.

- [x] **MCP-OP-SUBST-006**: For inspect, edit, apply, and transform calls, the pure projector shall emit operation, source-free subject tokens, request/result cardinalities, allowed semantic-kind and field-presence enums, refusal type, success/commit/verification/source-unchanged facts, and elapsed time sufficient for the registered counters, while the public request, execution, structured result, summary, callback, and schema remain unchanged when the ledger is healthy.

## Feature adoption

- [x] **MCP-OP-SUBST-007**: For read normalization, the observer shall count operation omission, ID omission, server-generated IDs, and mixed-ID refusal from actual public request/result shapes. Explicit IDs or explicit `forms` shall not be counted as shorthand use, and a refused mixed batch shall not be counted as a successful generated-ID call.

- [x] **MCP-OP-SUBST-008**: When an eligible inspect result emits a prepared request, telemetry shall record only eligibility, emission, hole count, and a session-keyed canonical skeleton token. A later edit call shall count as consumed only when replacing exactly the allowed caller-hole values with the fixed sentinel reproduces that token in the same session; committed shall additionally require `ok=true` and `committed=true`. Subject overlap, approximate shape, caller markers, and a refused write shall not count as committed consumption.

- [x] **MCP-OP-SUBST-009**: For WRITE-REFUSAL-001, the observer shall count each `expect-count-mismatch` firing, available/returned/omitted rows, truncation, continuation presence, and continuation inertness without storing row values. Continuation use shall require a later request whose closed continuation query token equals the emitted token; a copied or forged telemetry field shall not count.

- [x] **MCP-OP-SUBST-010**: A complete refusal shall open one bounded recovery episode. The report shall classify the first qualifying action within seven completed clj-surgeon calls or ten minutes as same-file reread, direct corrected retry, other next action, or abandoned using only session and subject tokens. It shall never infer the intended owner from a later success or silently exclude a chain with incomplete evidence.

- [x] **MCP-OP-SUBST-011**: For the first recovery read, the observer shall retain source-free owner and locator tokens, duplicate multiplicity, semantic-kind enums, body/dependency/hash presence, cap state, and explicit evidence completeness sufficient to project the frozen consumption-gap episode shape. Missing selector, answer, uniqueness, model, or result evidence shall remain explicit unknown or unclassifiable and shall never be inferred from a later mutation.

- [x] **MCP-OP-SUBST-012**: Feature observations shall use the common versioned feature ID, stage, integer counts, and closed scalar dimensions envelope. Unknown feature IDs, including future `elaborator.*` IDs, shall remain valid ledger evidence but shall be excluded from public claims until the digest-fenced feature registry names their stages and evidence policy; adding such a registry row shall require no ledger schema change.

## Marker, report, and claims

- [x] **MCP-OP-SUBST-013**: A report window shall bind canonical ledger path and file digest, exact first/last sequence and last event digest, key ID, UTC start/end, installed commit/tag, compiler commit/tree, feature-registry digest, baseline receipt/marker digest, and frozen classifier digest. The I/O shell shall verify the complete chain once and write canonical episode, digest, report, and receipt files without moving either marker or skipping an invalid record.

- [x] **MCP-OP-SUBST-014**: The paved `make substantiation-report` target shall print every registered feature with zero counts retained, window and marker identity, coverage and gap counts, prepared emitted/consumed/committed, normalization use, complete-refusal recovery outcomes, WRITE-REFUSAL-001 rows/continuations, ledger bytes, and caller/client strata where supported. Invalid or incomplete input shall exit nonzero and shall not render a clean report.

- [x] **MCP-OP-SUBST-015**: The claims compiler shall type each fact exactly as measured, observed-before-after, projected, or unavailable. Only direct ledger counts and durations may be measured; historical comparisons shall be observed-before-after; arithmetic using the 3.5237 ms/byte rate or any other rate shall be projected. Any request, registry entry, fixture, or renderer attempt to label a derived, historical, missing, or incomplete fact measured shall refuse before report publication.

- [x] **MCP-OP-SUBST-016**: Substantiation evidence shall carry `promotion_authority=false` and shall never satisfy a performance promotion gate, advance a baseline, authorize install, or turn a projected saving into a product speed claim. A caller request to use it as promotion evidence shall refuse while preserving the ledger and report unchanged.

## Verification and operations

- [x] **MCP-OP-SUBST-017**: Before installation, pure projection shall meet p95 below 0.5 ms over 10,000 events; local append shall meet p50 below 1 ms, p95 below 5 ms, and maximum below 25 ms over 1,000 calls; each event shall be at most 32,768 UTF-8 bytes; and the report shall publish actual ledger bytes per completed call. Any miss is release NO-GO.

- [x] **MCP-OP-SUBST-018**: Before installation, a zero-model live HTTP differential with at least 100 calls per arm shall show candidate p50 server time no more than 2 ms slower and p95 no more than 5 ms slower, and exact normalized public-result parity for eligible prepared reads, operation-less reads, mixed-ID refusals, complete write refusals, ordinary write successes, and transforms. A semantic or overhead miss is release NO-GO and no outlier may be removed after observation.

- [x] **MCP-OP-SUBST-019**: Report generation, self-test, install, and reload shall launch no model or network cohort, mutate no product source or baseline, and write only caller-named confined output. Installation and shared MCP reload shall remain separately authorized after independent verification and measurement.

## Falsifier matrix

| Law | Required permanent counterexample |
|---|---|
| `SUBST-001` | Missing finish, duplicate finish, reordered lines, reused call ID, chain break. |
| `SUBST-002` | Source/path/owner/replacement/URL/receipt leak and plain-digest dictionary probe. |
| `SUBST-003` | Existing segment, active deletion, marker-referenced deletion, key reuse. |
| `SUBST-004` | Injected start failure and finish failure after a successful mutation result. |
| `SUBST-005` | Caller-forged model/session fields; exchange with no model. |
| `SUBST-006` | All four tools with observer off/on normalized parity. |
| `SUBST-007` | Explicit IDs, all omitted IDs, mixed IDs, explicit/omitted operation. |
| `SUBST-008` | Exact fill, changed subject, changed guard, extra field, refusal, commit. |
| `SUBST-009` | Complete rows, bounded rows, forged continuation, inertness. |
| `SUBST-010` | Same-file read, different-file read, direct retry, eighth call, >10 minutes. |
| `SUBST-011` | Duplicate location, cap overflow, names-only, body fetch, incomplete evidence. |
| `SUBST-012` | Registered feature, unknown feature, `elaborator.*` without schema change. |
| `SUBST-013` | Wrong marker, moved window, wrong file digest, wrong classifier digest. |
| `SUBST-014` | Zero-use feature, gap, invalid line, incomplete start. |
| `SUBST-015` | Derived value labeled measured and missing value coerced to zero. |
| `SUBST-016` | Promotion request and automatic baseline advance. |
| `SUBST-017` | Both threshold edges and oversized event. |
| `SUBST-018` | Semantic mismatch and both overhead edges. |
| `SUBST-019` | Network/model invocation and output-root escape. |
