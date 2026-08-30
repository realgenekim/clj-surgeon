---
parent: write-refusal-completeness-design
status: pre-ratification consistency review
---

# Write-Side Refusal Completeness Consistency Report

Review target: the corrected post-REFUTE draft of
`write-refusal-completeness-design.md` and
`write-refusal-completeness-specs.md` on 2026-08-30.

## LID checks

| Check | Method | Result |
|---|---|---|
| Coverage | Mapped every deficient audit row to one requirement family and summed the mapping. | Pass: 1 + 2 + 1 + 4 + 1 + 1 + 2 + 1 = 13 sites. All eight audit candidates retain stable IDs and 001 remains first. |
| Contradiction | Compared the draft with the HLD bookkeeping and structural-authority tenets, active `MCP-OP-EDIT-010`, `MCP-OP-EDIT-015`, `MCP-OP-EDIT-018`, `MCP-OP-EDIT-024`, `MCP-OP-VERIFY-001..002`, `MCP-OP-READ-DIAG-001..003`, `MCP-OP-READ-HYP-001..002`, `MCP-OP-READ-CONT-001..002`, and the read-normalization leaf. | Pass: the draft adds mechanically available evidence without changing refusal outcomes, selection authority, write authority, retry authority, verification ownership, or snapshot meaning. The 32,768-byte write envelope is explicitly proposed rather than attributed to active intent. |
| Implicit scoping | Read each EARS statement with the registry-wide closed continuation paragraph but without its section heading. Checked actor, public entrance, refusal trigger, evidence universe, exact byte and row bounds, authority, and forbidden effects. | Pass: every requirement names clj-surgeon, the applicable entrance and refusal family, the already-computed evidence boundary, the 32,768-byte and 128-row overflow branch, and the no-executable-retry rule. |
| Context-free | Removed conversation history and read the complete packet as the review unit. Checked status, evidence base, 13-site boundary, priority, payload rule, authority law, and phase stop. | Pass: the complete packet carries those facts without transcript context. Each EARS statement supplies its family trigger and behavior and is interpreted with the registry-wide normative continuation and fixed-fallback paragraph. |

## Coverage ledger

| Audit row | Requirement |
|---|---|
| `intent_transaction.clj:877` generic count mismatch | `MCP-OP-WRITE-REFUSAL-001` |
| `intent_transaction.clj:617` named/defmethod owner mismatch | `MCP-OP-WRITE-REFUSAL-002` |
| `intent_transaction.clj:649` namespace owner mismatch | `MCP-OP-WRITE-REFUSAL-002` |
| `mcp_compact_location.clj:209-216` location proof | `MCP-OP-WRITE-REFUSAL-003` |
| `mcp_program_tool.clj:110-112` transform selection bound | `MCP-OP-WRITE-REFUSAL-004` |
| `mcp_program_tool.clj:115-120` transform expected-count mismatch | `MCP-OP-WRITE-REFUSAL-004` |
| `mcp_program_tool.clj:244-250` program losslessness | `MCP-OP-WRITE-REFUSAL-004` |
| `mcp_program_tool.clj:347-351` one-shot losslessness | `MCP-OP-WRITE-REFUSAL-004` |
| `extract.clj:253-280` to `mcp_extraction.clj:203-210` | `MCP-OP-WRITE-REFUSAL-005` |
| `mcp_change_buffer.clj:1053-1065` retained compact match | `MCP-OP-WRITE-REFUSAL-006` |
| `binding_rename.clj:230-233` binding ambiguity | `MCP-OP-WRITE-REFUSAL-007` |
| `binding_rename.clj:149-151` comment-sensitive binding | `MCP-OP-WRITE-REFUSAL-007` |
| `mcp_change_buffer.clj:1312-1330,1464,1542` profile admission | `MCP-OP-WRITE-REFUSAL-008` |

The coverage sum uses the audit's 13-row classification. It does not inflate
the count from raw line ranges or from several call sites that share one audit
row and one public payload law.

## Boundary findings

The corrected result bound is a proposed write contract: 32,768 UTF-8 bytes
for the complete public MCP JSON result and at most 128 candidate rows. The
packet does not claim that the active write entrances already enforce it.
Domain projection reserves 128 bytes for finalized timing and admits only a
prefix whose pre-finalization result and summary use at most 32,640 bytes. The
post-finalization measurement is an invariant check and does not alter domain
fields.

The oversize continuation rule is intentionally non-executable. Its closed
subject includes a family-specific selector digest. The outer query hash,
ordering version, and guards identify one frozen candidate query. Every
caller-ordered vector remains ordered inside the digest. This leaf adds no
consumer. LID requires a separate read leaf before any page operation can
exist. A descriptor cannot become a write retry.

If dynamic maps, names, paths, or guards make the zero-row pre-finalization
result and summary exceed 32,640 bytes, the fixed fail-empty projection removes
every unbounded dynamic value. It preserves only stable refusal identity and stage, bounded safety
booleans fixed to the audited pre-write outcome, numeric totals, fixed limits,
and the overflow reason. The fixed object is a domain projection. The active
finalizer still adds mandatory `elapsed_ms`, and the meter checks the complete
finalized result and summary. Each family has one closed stage value. Existing
stages are preserved; constructors without one publish the registered family
stage. The packet does not pretend that every current constructor already
publishes one.

The corrected 001 projection no longer assumes every generic change has form
scope or an owner. The corrected 003 projection covers only the two omitted-
location relations that can emit `compact-location-unresolved`. The corrected
008 projection uses a closed nine-ID violation vocabulary, explicit
short-circuit rules, no top-level source guards, and no
`exact-profile-not-project-owned` scope expansion.

Independent REFUTE review of the exact technical packet returned **SURVIVES**
after the threshold correction. The reviewed SHA-256 values and complete
finding disposition are recorded in
`write-refusal-completeness-adversarial-review.md`.

Verdict: **the packet passes the four LID consistency checks and independent
adversarial review; it remains unratified, deferred, and
non-implementation-authorizing.**
