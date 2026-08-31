---
parent: prepared-request-actions-design
prefix: MCP-OP-PREP-ACT
status: 'ratified in advance subject to adversarial PASS (Gene, 2026-08-30, verbatim: "If build 1 and 2 are go -- get adversarial review and build and go")'
---

# Prepared Request Confirm and Preview Specifications

These IDs are stable and must not be reused. They remain active gaps until the
packet's independent adversarial review passes, frozen red exists, and product
code proves them. Installation remains separately gated.

## Confirmation publication and registry

- [ ] **MCP-OP-PREP-ACT-001**: When an eligible `prepared_request` survives the existing normalized 32,768-byte inspect-result gate, clj-surgeon shall compute the descriptor SHA-256 over the existing exact canonical descriptor bytes and trial-associate one separate inert `prepared_confirmation` object containing the lowercase 64-hex digest, `expires_in_ms=300000`, `session_bound=true`, `commit_single_use=true`, `executable=false`, and `write_authority=false`. If that complete augmented result also fits 32,768 bytes, it shall register the exact descriptor and complete frozen target-file hash map under `[boot-epoch, stable-mcp-session-key, descriptor-sha256]` and surface the augmented result. Otherwise it shall return the saved prepared result by identical object identity and register nothing. Registration and the surfaced confirmation shall occur together or not at all.

- [ ] **MCP-OP-PREP-ACT-002**: The process-local confirmation registry shall retain at most 32 live entries per MCP session and 256 total, expire entries 300,000 monotonic milliseconds after the latest identical descriptor is surfaced in the same session, and evict deterministically by oldest expiry, oldest issue, then lexical digest. It shall retain at most 64 source-free terminal tombstones per session and 512 total for 300,000 ms so local consumed, expired, and evicted states remain distinguishable; tombstones shall contain no descriptor, root, identity, source hash, or fill. Reload, restart, session end, and shutdown shall destroy live entries and tombstones. The registry shall never persist, cross sessions, search source, or use caller-supplied session identity.

- [ ] **MCP-OP-PREP-ACT-003**: When the same registry key receives identical canonical descriptor bytes, clj-surgeon may refresh that entry. When an injected or real digest collision presents different canonical bytes, clj-surgeon shall remove both entries, disable confirmation for that boot, and return `prepared-confirmation-hash-collision` with source unchanged. No collision path shall select one descriptor, compare only parsed maps, or overwrite an existing entry.

- [ ] **MCP-OP-PREP-ACT-004**: A transport shall offer confirmation only when it supplies one stable unforgeable session key to both inspect and edit handlers. Stdio shall bind the one server connection; HTTP shall bind the SDK session. IP address, workspace root, request ID, descriptor digest, or caller data shall not act as session identity. A transport without a proved join shall preserve today's prepared result byte-for-byte and register no entry. A lookup from another session shall return the same `prepared-confirmation-unknown` response as a digest never served in that session and shall reveal no cross-session existence.

## Confirm and commit

- [ ] **MCP-OP-PREP-ACT-005**: The alternate `edit_clojure` confirmation request shall accept exactly `confirm`, `fill`, and optional literal `preview=true`. `confirm` shall be lowercase 64-hex. `fill` shall contain every and only the retained ordered `caller_holes` path once, with one nonblank string per path. The shape shall forbid root, edits, programs, deletions, relations, verification, expect, basis, retry, preview/result hashes, and every unknown field. Shape failure shall occur before registry lookup and leave source unchanged.

- [ ] **MCP-OP-PREP-ACT-006**: After same-session lookup, clj-surgeon shall route only to the entry's retained canonical workspace, read every retained target file once, and require its complete file-hash map to equal the served snapshot. It shall then replace every and only the declared null holes and validate the reconstructed arguments through the exact ordinary public `edit_clojure` schema. Workspace routing, snapshot, fill, schema, compiler, transaction, read-back, receipt, rollback, and result laws shall remain terminal; digest or registry presence shall grant none of their authority.

- [ ] **MCP-OP-PREP-ACT-007**: A commit-shaped confirmation shall consume the entry atomically after exact fill and ordinary public-schema admission and before transaction execution. Success, stale or compiler refusal, rollback, crash, or unverified outcome shall not permit replay. Malformed shape and hole mismatch shall not consume the entry, but expiry and capacity bounds shall continue. A consumed confirmation shall never imply that mutation succeeded; only the ordinary transaction receipt may do so.

- [ ] **MCP-OP-PREP-ACT-008**: Confirmation refusals shall use the closed error vocabulary `invalid-prepared-confirmation`, `prepared-confirmation-unknown`, `prepared-confirmation-expired`, `prepared-confirmation-evicted`, `prepared-confirmation-consumed`, `prepared-confirmation-hash-collision`, `prepared-confirmation-hole-mismatch`, `prepared-confirmation-snapshot-drift`, and `prepared-confirmation-preview-limit`. `prepared-confirmation-unknown` shall cover never-served, restart-lost, and other-session digests without distinguishing them. Local bounded tombstones shall make consumed, expired, and evicted exact while retained. Each refusal shall name the failed stage, exact source-free counts or expected/provided/missing/extra hole paths when applicable, `source_unchanged=true`, `mutation_attempted=false`, `write_authority=false`, and one non-executable remedy that is complete for that refusal type. It shall return no source, replacement, selected candidate, prepared request, next call, receipt, inverse, or terminal success response. Reconstructed ordinary refusals shall preserve their own exact public type and evidence.

## Preview

- [ ] **MCP-OP-PREP-ACT-009**: When an exact confirmation request also contains literal `preview=true`, clj-surgeon shall perform the same session, fill, routing, and frozen target-file checks as commit and shall invoke the ordinary pure compile path with capabilities restricted to source reads. It shall invoke no writer, receipt publisher, formatter, verifier, rollback, process launcher, or lower-layer exit, and shall leave every source byte unchanged.

- [ ] **MCP-OP-PREP-ACT-010**: A successful preview shall return `operation=edit_clojure-preview`, `lifecycle=preview`, `committed=false`, `mutation_attempted=false`, `write_authority=false`, `receipt=false`, `source_unchanged=true`, descriptor and fill SHA-256s, complete old snapshot guards, complete future-file hashes, exact changed-file and changed-character counts, one complete unified diff, an honest verification forecast, canonical preview SHA-256, and `next_action=none`. It shall contain no inverse, receipt path, verification-complete claim, terminal response, executable continuation, or commit token.

- [ ] **MCP-OP-PREP-ACT-011**: The complete preview diff shall be limited to 16,384 UTF-8 bytes and 256 lines, and the normalized complete MCP result shall be limited to 32,768 UTF-8 bytes. If any bound fails, clj-surgeon shall return `prepared-preview-output-limit` with exact required and allowed byte/line counts and no partial source or diff. It shall not truncate, paginate, retain a larger preview, or claim preview completeness.

- [ ] **MCP-OP-PREP-ACT-012**: Because the first slice targets `edit_clojure`, `verification_forecast` shall contain exactly `will_run=false`, `profile=null`, and `reason=edit_clojure-does-not-authorize-transaction-verification`. Preview shall not run or guess a verifier, treat parse or compile success as semantic correctness, or imply that a later commit will pass read-back or validation.

- [ ] **MCP-OP-PREP-ACT-013**: Preview shall not consume the confirmation, but no descriptor shall execute more than three preview calls. A later commit shall repeat `confirm+fill` without accepting any preview hash, diff, future hash, or preview object; it shall perform a fresh snapshot check, ordinary schema admission, single-use consumption, and full transaction. Source drift after preview shall refuse before write. Preview shall never refresh expiry or authorize commit.

## Compatibility, evidence, and promotion

- [ ] **MCP-OP-PREP-ACT-014**: Intrinsically ineligible reads, prepared descriptors removed by either byte gate, refusals, continuations, other inspect modes, writes, CLI results, unsupported transports, and other tools shall remain byte-identical for fixed clocks and shall register no descriptor or emit any confirmation, preview, omission cue, coaching, or telemetry event. The original fill-and-submit-complete-arguments route shall remain valid and unchanged.

- [ ] **MCP-OP-PREP-ACT-015**: Eligible confirmation results shall preserve the ordinary concise prefix and exact current three-sentence prepared coaching byte-for-byte and shall add no prose. The public tool description and schema shall explain the alternate request. Dynamic digest, source, file, owner, root, user, session, or network values shall remain structured data. Product telemetry shall record only eligibility/emission, digest, source-free session lifecycle class, age bucket, preview count, refusal class, request/result byte counts, phase clocks, and ordinary receipt hash; it shall never record descriptor bytes, identity, guards, fill, replacement, diff, source, or session key.

- [ ] **MCP-OP-PREP-ACT-016**: The W1 emission and complete-wall benefit shall remain labeled projected until one retained same-task measurement reports complete descriptor versus confirm/fill bytes and tokens and one counterbalanced live cohort reports first-call correctness, route adherence, emission, server wall, result-to-next-action wall, complete verified wall, refusals, expiry, recovery, and every assigned loss. A smaller request alone shall not justify promotion. W2 promotion shall require exact preview facts, zero effects, no false receipt/authority, and no increase in incorrect commits or stale retries.

- [ ] **MCP-OP-PREP-ACT-017**: Before implementation annotations land, the repository intent audit shall include this exact registry. Red tests shall directly witness every active ID. The implementation shall use injected monotonic clock, digest, boot epoch, session key, and registry capacity seams and throw-on-call effect spies so expiry, collision, cross-session, eviction, replay, preview, budget, and no-effect outcomes are deterministic rather than raced.

- [ ] **MCP-OP-PREP-ACT-018**: Installation and shared-runtime publication shall require a separate Gene approval after frozen red, Sol/SURGEON1 implementation, independent SURGEON2 verification, full affected and milestone gates, live W1/W2 measurement, exact rollback, and one real live proof of same-session confirm commit plus preview-then-stale refusal. No design, hash, preview, or benchmark result shall authorize install by itself.

- [ ] **MCP-OP-PREP-ACT-019**: Where public instructions describe prepared confirmation, clj-surgeon shall state that Streamable HTTP callers must retain the serving `Mcp-Session-Id` from the eligible `inspect_clojure` read through the `edit_clojure` confirmation and that stdio callers must retain one connection. The public tool descriptions and clj-surgeon skill shall state that a caller which creates a new MCP session per call must submit the ordinary explicit `prepared_request.arguments` instead. The guidance shall not imply portable confirmation, cross-session lookup, global registry state, or write authority.

- [ ] **MCP-OP-PREP-ACT-020**: Every prepared-confirmation lookup result and public confirmation result shall contain one boolean `ok` field. A caller shall distinguish success from refusal only by `ok`: `ok=true` permits consumption of the returned success data, while `ok=false` requires reading `error_type`, structured evidence, and the visible remedy. Descriptor, digest, or optional-field presence shall not act as an outcome discriminator.

- [ ] **MCP-OP-PREP-ACT-021**: If `invalid-prepared-confirmation` structured evidence contains ordered `invalid_fields`, then the visible refusal shall render the same fields and no others as one canonical JSON array literal with canonical escaping. It shall not concatenate an unquoted caller-supplied field name into prose. A `prepared-confirmation-unknown` visible refusal shall state both safe routes in one sentence: reuse the serving MCP session or submit ordinary explicit edit arguments. The unknown refusal shall not distinguish never-served, restart-lost, or other-session digests and shall not reveal whether another session served the digest.

## Falsifier table

| ID | Defensible opposite | Permanent witness families |
|---|---|---|
| `MCP-OP-PREP-ACT-001` | A digest can reconstruct the descriptor without state. | Hash-only lookup in an empty registry; exact two-stage budget/store ordering; augmented-overflow restores the prepared result by identity; descriptor object remains inert. |
| `MCP-OP-PREP-ACT-002` | Unbounded/global retention is simpler. | TTL edges, live/tombstone per-session/global capacity, deterministic eviction, restart/reload/session cleanup, tombstone redaction. |
| `MCP-OP-PREP-ACT-003` | SHA collision can pick first/last. | Injected same digest with unequal canonical bytes; equal bytes refresh; boot disable. |
| `MCP-OP-PREP-ACT-004` | Root, IP, or digest can identify a caller. | Two stdio/HTTP sessions with same root/hash; identical unknown response across never-served/other-session cases; unsupported session join; forged session fields. |
| `MCP-OP-PREP-ACT-005` | Best-effort holes are useful. | Missing, extra, reordered, duplicate-after-JSON, blank, non-string, mixed full/compact fields, preview false/null. |
| `MCP-OP-PREP-ACT-006` | Stored identity can bypass router or stale guards. | Wrong/removed root, target-file unrelated drift, owner drift, old-source drift, ordinary schema/compile refusal. |
| `MCP-OP-PREP-ACT-007` | Retry after unknown outcome is safe. | Success replay, rollback replay, crash after consume, invalid fill before consume, receipt/result disagreement. |
| `MCP-OP-PREP-ACT-008` | One generic unknown error is sufficient. | Every lifecycle/error type, complete hole vocabulary, source and executable-field absence. |
| `MCP-OP-PREP-ACT-009` | Preview may reuse commit execution then roll back. | Throwing writer/receipt/formatter/verifier/rollback/process spies and byte-identical filesystem. |
| `MCP-OP-PREP-ACT-010` | A preview can look like success if `committed=false`. | Closed result schema, absent terminal response/inverse/verification claim, hostile source confined to diff data. |
| `MCP-OP-PREP-ACT-011` | A partial diff is better than refusal. | Exact 16,384/16,385-byte, 256/257-line, and 32,768/32,769-result edges. |
| `MCP-OP-PREP-ACT-012` | Parse success predicts verification. | Parse-valid semantic error, verifier never called, exact false forecast. |
| `MCP-OP-PREP-ACT-013` | Preview hash may fence commit. | Changed source after preview, changed fill, preview replay, fourth preview, commit containing preview artifacts. |
| `MCP-OP-PREP-ACT-014` | An omission cue helps unsupported callers. | Every ineligible/result-budget/transport sibling by identical object and bytes. |
| `MCP-OP-PREP-ACT-015` | More coaching and rich telemetry improve recovery. | Exact old coaching bytes; hostile values stay out of prose/logs; exact telemetry allowlist. |
| `MCP-OP-PREP-ACT-016` | Byte deletion proves speed. | Pure byte/token measure plus counterbalanced live losses and complete-wall gate. |
| `MCP-OP-PREP-ACT-017` | Wall-clock/session/digest integration tests are enough. | Fake clock/digest/session/capacity and throw-on-effect deterministic red/green. |
| `MCP-OP-PREP-ACT-018` | Design approval implies release approval. | Absent verification/measurement/install receipts block publication. |
| `MCP-OP-PREP-ACT-019` | Session affinity is an SDK detail callers can infer. | HTTP session retention, stdio connection retention, per-call-session explicit fallback, and absence of portable-authority claims in both public descriptions and skill text. |
| `MCP-OP-PREP-ACT-020` | Descriptor or digest presence can distinguish success. | Success and refusal shapes with misleading optional-field presence; exact boolean `ok` branch and typed `error_type` only on refusal. |
| `MCP-OP-PREP-ACT-021` | Structured evidence alone is sufficient, raw field names are safe prose, or unknown can recommend only another read. | Ordered invalid-field visible parity with hostile field names and canonical JSON escaping; exact one-sentence two-route unknown remedy; byte-identical unknown for never-served, restart-lost, and cross-session lookup. |
