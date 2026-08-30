---
parent: prepared-request-design
prefix: MCP-OP-PREP-REQ
status: hld-ratified-requirements-deferred
---

# Prepared Guarded Edit Request Specifications

These candidate IDs are stable and must not be reused if a requirement is
deleted. Every requirement remains deferred. Gene ratified only the Option A
HLD and proxy experiment on 2026-08-30. These requirements stay deferred until
the experiment gate or a separately ratified recovery-oriented gate passes,
the open LLD decisions close, and Gene separately ratifies the completed design
and registry. No test or code annotation may claim these requirements before
that later ratification.

## Eligibility and projection

- [D] **MCP-OP-PREP-REQ-001**: When one successful typed `inspect_clojure` batch satisfies the ratified eligibility and result-budget rules, and every selected item has exact old source, exact cardinality, one project-relative file, and one uniquely named top-level owner, clj-surgeon shall publish one complete ordered `prepared_request` for all selected items and shall not publish a partial template.

- [D] **MCP-OP-PREP-REQ-002**: When clj-surgeon publishes a prepared request, its arguments shall contain the canonical workspace root, and each prepared edit shall contain an explicit project-relative file, explicit named-owner scope, exact selected old source as `from`, exact match count, and one explicit caller-owned null `to` hole; every field other than the holes shall belong to the public `edit_clojure` JSON contract.

- [D] **MCP-OP-PREP-REQ-003**: When a normalized inspect request ID labels source evidence used by a prepared edit, the prepared result may repeat that ID only as call-local evidence; it shall repeat the file, named owner, old source, and cardinality in the prepared edit and shall not use the ID as selector, snapshot, basis, or write authority.

## Authority and salience

- [D] **MCP-OP-PREP-REQ-004**: When clj-surgeon publishes a prepared request, it shall mark the object `executable=false` and `write_authority=false`; while incomplete, the object shall grant no mutation or write authority, and clj-surgeon shall not invent replacement text, select or widen a subject, retain server state, create a basis or plan, choose verification, execute a mutation, or publish the object as an executable `next_call`.

- [D] **MCP-OP-PREP-REQ-005**: When a prepared request is rendered, structured content shall contain the complete prepared object, and one static server-owned coaching sentence shall state that the caller may fill every replacement hole and submit `prepared_request.arguments` only if the caller independently decides to edit the exact selections; that sentence shall not interpolate source, request, user, file, workspace, or network content, and ordinary dynamic read labels, counts, elapsed evidence, and `next_action=none` shall remain unchanged.

## Omission and execution boundary

- [D] **MCP-OP-PREP-REQ-006**: If an inspect result omits selected source, has zero or several possible owners, lacks exact cardinality, contains a refusal or continuation, requires semantic judgment, uses a retained basis, has an unsupported file type, or exceeds the prepared-request result budget, clj-surgeon shall publish no prepared request and shall preserve the ordinary read success or refusal classification without guessing, widening, or retaining state.

- [D] **MCP-OP-PREP-REQ-007**: When a caller fills every prepared replacement hole and invokes `edit_clojure`, clj-surgeon shall validate and execute the object through the ordinary public schema and guarded transaction path; stale old source, changed ownership, count mismatch, a null or malformed replacement, or any ordinary compiler refusal shall leave every source file unchanged.

- [D] **MCP-OP-PREP-REQ-008**: A prepared request shall not change the authority or behavior of inspect request IDs, snapshot continuations, executable read retries, retained semantic bases, extraction plans, generic changes, computed programs, CLI operations, verification profiles, or any non-`edit_clojure` entrance.

- [D] **MCP-OP-PREP-REQ-009**: The public inspect output schema shall expose one closed optional `prepared_request` descriptor; its null-bearing `arguments` shall be explicitly non-executable, and after the caller fills every declared hole, those arguments shall validate exactly through the existing public `edit_clojure` input schema without an adapter, private field, or alternate executor.

## Falsifiers

| ID | Defensible opposite | Required witness families after ratification |
|---|---|---|
| `MCP-OP-PREP-REQ-001` | A best-effort or partial template is still useful. | Exact single selection; complete batch; missing one item; over-budget batch. |
| `MCP-OP-PREP-REQ-002` | The model can reconstruct omitted identity or guards. | Exact public fields; one hole per item; no private fields; schema-valid after filling. |
| `MCP-OP-PREP-REQ-003` | Generated IDs can replace repeated structural identity. | Two calls with equal IDs; changed file; changed owner; reordered batch. |
| `MCP-OP-PREP-REQ-004` | A read may imply replacement or write authority. | Null holes; no mutation; no basis; no plan; no verifier; no executable next call. |
| `MCP-OP-PREP-REQ-005` | Hidden structured data alone supplies enough salience. | Visible static summary; structured object; source-like hostile strings never enter coaching text. |
| `MCP-OP-PREP-REQ-006` | Ambiguous or large reads may emit a truncated template. | No source; duplicate owner; unknown owner; continuation; retained basis; unsupported type; overflow. |
| `MCP-OP-PREP-REQ-007` | Preparation may bypass ordinary stale-source or schema checks. | Source drift; owner drift; count drift; null hole; malformed replacement; successful parity. |
| `MCP-OP-PREP-REQ-008` | Another artifact's authority can be reused for convenience. | Generated IDs; continuation; retry; basis; extraction; program; CLI; verifier isolation. |
| `MCP-OP-PREP-REQ-009` | A result descriptor may use a private request language. | Output-schema projection; null-hole refusal; filled-argument schema parity; unknown-field refusal. |

## Candidate adoption falsifier

The first model experiment is not an EARS product requirement. It is the gate
for returning with a completed LLD and EARS registry for separate ratification:

- eight fresh sessions, four control and four prepared-result;
- counterbalanced order;
- identical unforced prompt, catalog, source, task, caller, model, and scorer;
- every session retained in the primary denominator;
- at least 3/4 prepared Surgeon-first attempts;
- at least +25 percentage points over control;
- no correctness loss or refusal increase; and
- zero mutations across four additional read-only safety attempts, two per
  arm.

Complete task wall, observable client actions, and tool calls remain in the
loss chart. They do not rescue a failed routing or safety gate. If the
experiment misses any gate, these requirements remain deferred and product
implementation does not begin. Construction-refusal and recovery-output
outcomes remain reportable evidence for the ratified HLD, but cannot activate
these requirements without a separate gate decision.
