---
parent: mcp-operation-contract-design
prefix: INSERT-FORMS
status: implemented
---

# insert_forms v1

One request inserts a nonempty payload at one structural boundary in one existing
Clojure file. The authoritative contract is reproduced in contract.md, with its
byte examples and refusal vocabulary intact. This leaf adds no batch, preview,
replacement, formatting, alias inference, or behavioral verification.

The pure planner consumes captured source and a closed request; its output carries
candidate bytes, receipt facts, and durable detail. The I/O shell captures under
the workspace lock, uses the guarded transaction and staged atomic publication,
and persists inverse evidence outside the workspace. CLI and MCP delegate to this
shell. The independent acceptance walk shares only the CST parser.

The user authorized the full RED-to-GREEN sequence on fable/insert-forms. Tests
precede implementation; each witness namespace is a separate commit. Requirements
remain unchecked until implementation and its direct witnesses pass.

## Entrances and evidence

Both `clj-surgeon :insert-forms! :request-file X` and
`clj-surgeon :op :insert-forms! :request-file X` read exactly one bounded EDN map.
See [contract.md](contract.md) for the complete request grammar and byte examples.
The MCP tool is `insert_forms`; it takes the same map as JSON. Closed schemas
reject preview, replacement, batches and verification profiles.

Completed structural inspect results now expose `read_receipts`, keyed by relative
file, for use as `guard.read_receipt`. A native digest remains sufficient.
The planner returns a typed result; the I/O shell persists planned inverse evidence
before publication and finalizes the artifact with the actual outcome and observed
source digest. Read-back proves the write only. A result never includes
`terminal_response`, and `verification_complete` remains false.

The preparse limits are 2 MiB request, 8 MiB source, 1 MiB payload, 1,000 payload
forms, depth 512 and 100,000 lexical units. Reindentation and combined candidate
size are projected before materialization and capped at 16 MiB. Root indexes,
trivia byte offsets and literal membership use indexed/swept lookups.

The eighteen directly linked witness namespaces include an independent oracle
that reconstructs the splice and hashes every other root form. They reject
neighbor formatting, comment removal, misplaced body insertion and forged hashes.
Frozen examples derive from census-named namespaces at base bd124492; their
provenance does not claim the exact historical incident bytes.

## Fix round 1 after Opus GO-WITH-FIX

Candidate placement is guarded before publication; per-form comparisons compute
preservation booleans independently of that guard. Dynamic identity seams allow
witnesses to perturb the splice offset or candidate text without changing source
code. Deleting the structure guard must make the offset-defect witness fail.

The §3 spacing ruling reuses existing whitespace before the insertion and copies
its separator after the payload. Body closers stay with the payload unless the
payload ends with a newline or trailing comment, in which case the closer is indented.
No formatter runs. Candidate diagnostics include kind, name, and line; each
refusal type explains its missing decision, including fresh guards for stale reads.

`clj-surgeon.insert-forms/recovery-status` documents recovery from a planned durable
receipt and classifies a fresh target digest as published, not-published, or changed.
It does not replay, finalize, or clean up automatically. The six new regression
witnesses are INSERT-FORMS-019 through INSERT-FORMS-024. Final validation for this
round is the complete insert suite, census regeneration and diff review, test-fast,
and lint 0/0; landing-gate-prewarm belongs to the ship's fast lane.

### clj-splice port (2026-09-11)

Nested and literal intervals now come from clj-splice, with Surgeon retaining owner, body, header and layout policy. Mixed LF/CRLF is admitted: inserted separators use the first source newline (LF if absent), and every original byte is preserved. CRLF inside literals remains literal data. Bare CR, BOM and measured tab indentation admission remain deferred. The bounded lexical/shape preflight remains until rewrite-clj provides a parser-level resource budget; spans supplies literal discovery for indentation.
