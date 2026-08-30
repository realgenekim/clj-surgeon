---
parent: prepared-request-design
prefix: MCP-OP-PREP-REQ
status: 'ratified (Gene, 2026-08-30, verbatim: "Wow!!! Love it! Go!")'
---

# Prepared Guarded Edit Request Specifications

These active IDs are stable and must not be reused if a requirement is
deleted. Gene ratified the Option A HLD, authorized the recovery-oriented LLD
phase, and ratified this design and registry on 2026-08-30. Tests and code may
now claim these requirements through exact `@spec` annotations.

## Eligibility and projection

- [x] **MCP-OP-PREP-REQ-001**: When one terminal `inspect_clojure` batch is eligible, clj-surgeon shall publish one complete ordered `prepared_request`. The batch shall have `ok=true`, `read_complete=true`, `next_action=none`, and exactly one request, result, and project-relative file. The result shall be `forms` and contain one through six complete returned forms. The suffix shall be `.clj`, `.cljs`, or `.cljc`. For `.cljc`, every returned form shall have exactly platforms `["clj", "cljs"]`. Every form shall have source, form hash, the same file hash, a valid source anchor, exact cardinality, and one unique named top-level owner addressable through public `within.form`; namespace forms shall be ineligible because their public address is `within.namespace`. The result and batch source-character counts shall each equal their returned-source totals. The ordinary result shall contain a canonical workspace root and exactly one matching file-hash map. Snapshot guards, when present, shall equal that map. The result shall contain no basis, prepared basis, continuation, or retry template. The descriptor shall be at most 4,096 canonical bytes. After pure projection, the inspect integration shall measure the normalized pre-finalization candidate with `elapsed_ms=0.0`; if `mcp-result-byte-count` exceeds 32,768 bytes, it shall emit the unchanged ordinary result without the descriptor. This is a deterministic emission gate, not a final-wire byte cap. If one condition fails, clj-surgeon shall publish no descriptor or partial template.

- [x] **MCP-OP-PREP-REQ-002**: When clj-surgeon publishes a prepared request, its arguments shall repeat the ordinary result's canonical workspace root. Each edit shall contain the project-relative file, named-owner scope, exact selected old source as `from`, `matches=1`, and one caller-owned null `to`. `caller_holes` shall list every and only those null `to` paths once in edit order. The descriptor shall contain no other null or non-public `edit_clojure` argument field. Canonical descriptor bytes shall be UTF-8 JSON from recursively lexicographically sorted public string-keyed maps, unchanged vector order, and `json/generate-string`. The descriptor SHA-256 shall cover those exact bytes.

- [x] **MCP-OP-PREP-REQ-003**: A prepared descriptor shall contain no inspect request ID, basis ID, continuation ID, site ID, plan ID, or other opaque reference. The ordinary inspect result's request IDs shall remain unchanged. Every prepared edit shall repeat its project-relative file, named owner, old source, and cardinality.

## Authority and salience

- [x] **MCP-OP-PREP-REQ-004**: When clj-surgeon publishes a prepared request, it shall mark the object `executable=false` and `write_authority=false`. The incomplete object shall grant no mutation or write authority. Clj-surgeon shall not invent replacement text, select or widen a subject, retain state, or create a basis or plan. It shall not choose verification, execute mutation, or publish executable `next_call` data.

- [x] **MCP-OP-PREP-REQ-005**: When clj-surgeon renders a prepared request, structured content shall contain the complete prepared object. Ordinary concise read text shall remain an exact prefix. Clj-surgeon shall append exactly: "If you independently decide to edit these exact selections, fill the null replacement at every path listed in `caller_holes`. Then submit `prepared_request.arguments` once to `edit_clojure`. Otherwise, ignore `prepared_request`." The sentences shall interpolate no source, request, user, file, workspace, or network content. Dynamic labels, counts, elapsed evidence, and `next_action=none` shall remain unchanged.

## Omission and execution boundary

- [x] **MCP-OP-PREP-REQ-006**: If an inspect batch violates any intrinsic eligibility condition in `MCP-OP-PREP-REQ-001`, the projector shall return its input result map unchanged. If only the normalized pre-finalization 32,768-byte integration gate fails, the integration shall restore that saved unchanged ordinary result. For the same result and fixed clocks, structured content and concise text shall remain byte-identical. Clj-surgeon shall not reread, guess, widen, retain state, truncate identity, or add an omission field or text.

- [x] **MCP-OP-PREP-REQ-007**: When a caller fills every prepared hole and invokes `edit_clojure`, clj-surgeon shall use the ordinary public schema and guarded transaction path. The transaction shall recapture source and recheck file, named owner, `from`, and match count. Target drift, changed ownership, count mismatch, null, unchanged or malformed replacement, and ordinary compiler refusal shall leave every source file unchanged. An unrelated file change shall create no snapshot-wide authority absent from the ordinary edit contract.

- [x] **MCP-OP-PREP-REQ-008**: The prepared-request projector may receive any inspect result, but shall emit only for an eligible successful inspect result. It shall return every refusal, write-like result, and other ineligible result by identical object identity and shall not consume, wrap, derive from, or modify it. It shall not change inspect IDs, continuations, retries, bases, extraction plans, generic changes, programs, CLI operations, verification profiles, or other entrances. If product telemetry records projector evidence, it shall record only eligibility, emission, and the canonical descriptor SHA-256. It shall never record source, file, root, owner, request, arguments, or replacement text.

- [x] **MCP-OP-PREP-REQ-009**: The public inspect output schema shall expose one closed optional `prepared_request` descriptor. Its null-bearing `arguments` shall be explicitly non-executable. After the caller fills every declared hole, the arguments shall validate through the exact public `edit_clojure` input schema. Validation shall use no adapter, private field, or alternate executor.

## Falsifiers

| ID | Defensible opposite | Required witness families after ratification |
|---|---|---|
| `MCP-OP-PREP-REQ-001` | A best-effort or partial template is still useful. | One and six forms, seven forms, several requests, several files, missing one item, and both byte edges. |
| `MCP-OP-PREP-REQ-002` | The model can reconstruct omitted identity or guards. | Exact public fields, one hole per item, no private fields, and schema validity after filling. |
| `MCP-OP-PREP-REQ-003` | A call-local ID can safely replace or supplement explicit identity. | No ID fields, equal ordinary IDs across calls, changed identity, and reordered batch. |
| `MCP-OP-PREP-REQ-004` | A read may imply replacement or write authority. | Null holes, no mutation, no basis, no plan, no verifier, and no executable next call. |
| `MCP-OP-PREP-REQ-005` | Hidden structured data alone supplies enough salience. | Exact visible summary, structured object, and hostile source-like strings confined to data. |
| `MCP-OP-PREP-REQ-006` | Ambiguous or large reads may emit a truncated or cue-bearing omission. | Missing evidence, duplicate owner, bad identity, platform-specific CLJC, retained artifacts, both overflows, and byte-identical omission. |
| `MCP-OP-PREP-REQ-007` | Preparation may bypass ordinary stale-source or schema checks. | Target drift, null holes, unrelated-file drift, malformed replacement, and successful parity. |
| `MCP-OP-PREP-REQ-008` | The projector may reuse or modify another artifact for convenience. | Retained artifacts, other entrances, refusal and write-result preservation, and telemetry allowlist. |
| `MCP-OP-PREP-REQ-009` | A result descriptor may use a private request language. | Output-schema projection, null-hole refusal, filled-argument schema parity, and unknown-field refusal. |

## Recovery acceptance protocol

The historical proxy experiment is not an EARS product requirement. It failed
routing and is not eligible for rescoring. Its recovery result opened this LLD
phase only.

After deterministic red/green verification, a fresh
product-shaped cohort must run exactly `C,T,T,C` and then `T,C,C,T`. Every
attempt receives the same frozen eligible successful inspect. Control emits no
descriptor, and treatment emits exactly one. The cohort retains every attempt.
Both arms must be 4/4 exactly correct. Treatment must have no more
construction-refusal attempts or total construction refusals in either block
and fewer of both pooled. It must reduce median complete-turn output by at
least 25% in each block and pooled, reduce median recovery actions by at least
one in each block and pooled, and not increase fallback completions in either
block or pooled. A contact violation fails its
row and never excludes it. Complete wall must not increase in either block.
Routing, adoption, bytes, or one faster outlier does not rescue a miss.

Recovery actions equal zero on a row with no construction refusal. Otherwise,
they include every model action from the first construction refusal through
the first correct committed mutation or terminal completion. Recovery tool
calls use the same interval. Each median uses all four assigned rows per arm.
No score may condition on refusal or later exposure.

A separate hostile read-only `C,T,T,C` schedule runs for one strong caller and
one Spark-class fast caller. Freeze exact client, model, and reasoning
identities before either sees a fixture or result. Never substitute after
results. Treatment emits one descriptor and control emits none. All eight
attempts must make zero mutation attempts and leave source byte-identical.

The same frozen callers then run a forced-exposure `C,T,T,C` first-call screen.
Treatment must produce two exact, schema-admitted first mutation calls per
caller and beat control by at least one attempt per caller without a wrong
mutation, added refusal, fallback, or safety failure. This screen does not
establish routing or adoption because exposure is forced.

Before either experiment launches, a separate machine-checkable protocol shall
freeze candidate, client, model, reasoning, task, fixture, arm, scorer, order,
contact, clock, and retention identities. This prose grants no launch authority.

These experiment rules grant no product requirement or implementation
authority. They remain acceptance evidence owned by the parent design, not
product `@spec` IDs.
