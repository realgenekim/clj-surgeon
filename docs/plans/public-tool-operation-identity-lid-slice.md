# HLD Checkpoint: Separate Public Tool Names from Operation Identity

**Status:** HLD checkpoint only. Stop before LLD, EARS, tests, or code.

**Parent intent:** `OP-ALG-IDENTITY-001`, `MCP-OP-COVERAGE-001`, and
`MCP-OP-RELAY-001..005`

**Owning decision:** `clj-surgeon-x9d`

**Base:** `3c1ffee8a52e28c3603edb33fe9b69b937b2da01`

## Problem

The current mutation handler conflates three facts:

1. the public MCP tool that the caller selected;
2. the internal operation family selected by request shape;
3. the legacy operation string shown in structured and human results.

Both `edit_clojure` and `apply_clojure_changes` use
`handle-clj-change`. The handler does not receive the selected public tool
name. `request-operation` instead inspects raw fields:

```text
edits | programs | delete_owners  -> "edit_clojure"
everything else                  -> "apply_clojure_changes"
```

Therefore an `apply_clojure_changes` call with compact `edits` reports
`operation="edit_clojure"`. Meanwhile, mutation telemetry reports every call
as `tool="apply_clojure_changes"`.

This coupling makes a public rename unsafe. The public string participates in
terminal-response eligibility, summaries, recovery, registry coverage,
telemetry, and benchmark scoring. A cosmetic rename can change behavior or
measurement even when the mutation kernel remains identical.

## Desired Outcome

Preserve current public schemas, tool names, response bytes, summaries,
terminal-response eligibility, recovery, telemetry fields, benchmarks, and
runtime behavior while separating these internal facts:

```clojure
{:public-tool-name "edit_clojure" | "apply_clojure_changes"
 :operation-kind   :mutation/compact | :mutation/managed
 :legacy-operation "edit_clojure" | "apply_clojure_changes"}
```

The public tool name comes only from the trusted registry entry. Request data
cannot select it.

The operation kind comes from the existing request-family rule. It is stable
internal data and does not grant effects.

The legacy operation spelling remains an output projection. The first ratchet
does not change it.

## Architecture

```text
tools/call.name
      |
      v
trusted public route --------------------------+
  public-tool-name                             |
                                               v
request fields -> request-operation-kind -> operation context
                                               |
                                               +-> execute existing kernel once
                                               |
                                               +-> terminal eligibility by kind
                                               |
                                               +-> legacy operation projection
                                               |
                                               +-> telemetry identity fields
```

The two public registry entries use separate stable wrapper Vars. Each wrapper
passes its trusted public name to one shared private handler. The shared handler
still executes the existing request validator, compiler, transaction runtime,
summary, and callback path exactly once.

The first ratchet can keep two operation kinds because that is the current
observable distinction:

| Operation kind | Existing request evidence | Legacy operation string |
|---|---|---|
| `:mutation/compact` | At least one of `edits`, `programs`, or `delete_owners` | `edit_clojure` |
| `:mutation/managed` | Basis decisions, explicit changes, extraction, or a malformed request without compact fields | `apply_clojure_changes` |

Later LLD review can split managed requests into prepared basis, explicit
changes, and extraction. That split is not required to make renaming safe.

The context does not contain effects, lifecycle authority, or policy selected
by request data. The existing operation algebra remains the authority for
those facts.

## Exact Coupling Inventory and Disposition

| Coupling | Current location | HLD disposition |
|---|---|---|
| Request shape returns a public-looking string | `mcp_tool/request-operation` | Replace internally with `request-operation-kind`; preserve a separate legacy-string projection. |
| Shared handler cannot identify selected tool | `mcp_tool/handle-clj-change` and both registry entries | Add two thin public wrapper Vars that pass trusted tool identity to the shared handler. |
| Terminal response checks `operation == "apply_clojure_changes"` | `mcp_tool/exact-terminal-response` | Require `:mutation/managed` and a consistent legacy projection. Prove the full eligibility truth table is unchanged. |
| Summary heading reads `:operation` | `mcp_tool/concise-summary` | Preserve the legacy response field and exact text. Do not expose the new kind in user-facing content. |
| Kernel success initially emits `operation="apply_clojure_changes"` | `mcp_contract/kernel-success->result` | Preserve this intermediate behavior unless the LLD proves removal is smaller. The final legacy projection remains authoritative. |
| Every mutation event records `tool="apply_clojure_changes"` | `mcp_telemetry/call-event` | Preserve `tool` for compatibility. Add `public_tool` and `operation_kind` as source-free internal evidence. |
| Outcome classes are keyed by public name | `mcp_server/outcome-classes-by-tool` | Keep this public-name key. Add a cross-product witness for allowed kinds per public tool. |
| Recovery requires and calls exact public tools | `mcp_recovery` | Keep public names. Recovery tests prove no change. |
| Server instructions name public tools | `mcp_server/server-instructions` | Keep current text in this ratchet. Naming work owns later changes. |
| Benchmark route gates match event tool names | `bench/run_clean_codex.sh` and Claude adapter | Keep matching actual public call names. Do not infer kind from tool name. Add kind evidence only after harness parity. |
| Registry and smoke tests assert exact catalog names | MCP server, HTTP, stdio, recovery, and install tests | Keep exact expected catalogs and schema hashes. |
| Installed Codex and Claude routing names tools directly | `resources/`, skill package, and install tests | Keep exact bytes. This ratchet requires no client migration. |

## Behavior-Preserving Witnesses

The next LID phases must define permanent witnesses for this matrix. The HLD
does not authorize those tests yet.

### Pure identity matrix

| Selected public tool | Request shape | Public tool | Kind | Legacy operation |
|---|---|---|---|---|
| `edit_clojure` | compact | `edit_clojure` | `:mutation/compact` | `edit_clojure` |
| `apply_clojure_changes` | compact | `apply_clojure_changes` | `:mutation/compact` | `edit_clojure` |
| `apply_clojure_changes` | explicit changes | `apply_clojure_changes` | `:mutation/managed` | `apply_clojure_changes` |
| `apply_clojure_changes` | extraction | `apply_clojure_changes` | `:mutation/managed` | `apply_clojure_changes` |
| `edit_clojure` | invalid noncompact input | `edit_clojure` | `:mutation/managed` | current refusal bytes unchanged |

The last row proves that a trusted public name does not authorize a request
family forbidden by that tool's schema.

### Terminal-response parity

Replay the current total projection corpus before and after the ratchet:

- eligible project-owned exact pass;
- wrong legacy operation;
- compact exact request through the broad public tool;
- non-exact success;
- pending verification;
- refusal, rollback, failure, and unverified state;
- malformed receipt, read-back, verifier, or contradiction evidence.

The before and after terminal-response values must be byte-identical for every
case. The promoted extraction must retain its terminal response.

### Public byte parity

For successful compact and managed mutations and their typed refusals, compare:

- callback success or error channel;
- visible summary bytes;
- structured result after removing no fields;
- output schema and tool description hashes;
- catalog order and names;
- receipt bytes, receipt hash, future source hashes, and undo behavior.

All values must be identical. The first ratchet adds no public response field.

### Telemetry compatibility

For each route, the projection over every pre-ratchet telemetry field must be
identical. New source-free fields must report:

```text
actual public call name + operation kind + legacy operation spelling
```

This closes the current natural-history mismatch without rewriting old logs.

### Boundary and performance gates

- Registry coverage remains exact for all current public tools and outcomes.
- Recovery performs the same guarded mutation with the same public name.
- Codex and Claude harness self-tests preserve existing route counts.
- No analyzer, formatter, verifier, source writer, receipt publisher, or
  callback path is added.
- The no-model callback cohort stays within 5% at p50 and p95.
- One fresh Sol/high exact edit and one extraction canary preserve first-call
  route, semantic correctness, terminal relay, and complete-wall gate.

## Option M: First-Person LLM-User Critique

Corrected Option M has five stops:

```text
inspect_clojure      bounded evidence and preparation
edit_clojure         decided literal edits and a mixed atomic chord
transform_clojure    bounded SCI transform; preview by default
extract_clojure      one-shot extraction when mechanically complete;
                     pre-write frozen plan only for genuine unknowns
apply_clojure_plan   plan_id plus only genuine decisions; then execute
```

As an LLM user, I prefer this organ to the current broad
`apply_clojure_changes` schema. Each stop describes the decision I am making,
not the transaction kernel underneath it.

The best interaction is extraction:

```text
I know source, destination, and forms
    -> extract_clojure
       -> mechanically complete: commit + verify + terminal response
       -> genuine unknown: frozen plan_id + exact decision holes
          -> apply_clojure_plan(plan_id, decisions)
```

This preserves the measured 12.070-second deletion of a mandatory public plan
phase. It also removes hashes, counts, selectors, and other kernel bookkeeping
from the continuation. I do not have to reconstruct the next call.

I would still hesitate at four boundaries:

1. **Edit versus transform.** If `edit_clojure` accepts computed programs in a
   mixed chord, I need one rule: use edit when the commit decision is complete;
   use transform when I need to preview a computed relation.
2. **Inspect versus extract.** If the extraction target and forms are known, I
   want to call `extract_clojure` directly. Inspection must not become a
   preflight ritual.
3. **Plan identity.** A `plan_id` must bind workspace, source hashes, contract
   version, exact candidate universe, and decision holes. Concurrent agents,
   process restart, expiration, and source drift must refuse safely. A missing
   receipt is unverified, never safe to replay.
4. **Compatibility.** A visible `apply_clojure_changes` alias beside the five
   stops gives me two plausible keys and can restore the current hesitation.
   Prefer an unlisted stale-client alias or a scheduled fresh-session cutover.

The prior compact-`plan_id` experiment remains a warning, not a veto. It cut
visible bytes by only 49.5% to 52.4% and did not reduce deliberation. Option M
is materially different only if the plan handle appears after a genuine
unknown, contains exact decision holes, and removes copied bookkeeping. Do not
claim a speed gain from the handle alone.

My preferred name inside Option M is still `apply_clojure_plan`, because it
mutates and should normally arrive as an exact `next_call`. The model should
not select it from an open-ended catalog decision. `continue_clojure_plan`
would describe the state transition more precisely, but “continue” hides the
write effect. The current name is the safer warning.

## Option M Adversarial Gates

Identity separation is a prerequisite, not approval to publish Option M.
Option M must pass these additional gates:

1. A mechanically complete extraction remains one tool call, one commit, one
   exact verifier, and one terminal response.
2. Its complete wall does not regress the current Sol extraction median by
   more than 5%.
3. A genuine-unknown extraction performs no write and returns one
   snapshot-bound plan with only genuine holes.
4. `apply_clojure_plan` accepts only the returned plan identity and those
   decisions. It accepts no copied hashes, counts, selectors, or commands.
5. Source drift, plan expiry, contract drift, wrong workspace, duplicate
   application, and server restart have typed fail-closed outcomes.
6. A plan store, if any, has explicit ownership, budget, lifetime, and crash
   behavior. A content-addressed token must not leak source or authority.
7. Fresh Sol/high chooses the correct stop on exact edit, computed preview,
   direct extraction, and extraction continuation tasks without repository
   coaching.
8. Fable and Opus may use deferred discovery, but Sol retains its direct
   first-action path.
9. No visible compatibility alias duplicates a large schema unless a matched
   cohort proves no selection or complete-wall regression.
10. Current `edit_clojure` and extraction semantic scorers remain unchanged.

## Scope and Non-Goals

This HLD slice does not:

- rename or add a public tool;
- add `operation_kind` to public structured output;
- change schemas, descriptions, routing, recovery, or benchmark prompts;
- change terminal-response eligibility;
- implement `plan_id`, a plan store, or Option M;
- change effect authority or the operation-algebra catalog;
- install, reload, restart, or call the shared MCP runtime.

## LID Phase Boundary

This document is the HLD checkpoint. If approved, the next phase adds one LLD
leaf under the operation-algebra intent segment. That LLD will select exact
internal data names and the trusted wrapper shape. EARS requirements, red
tests, and code follow in separate reviewed phases.

The smallest expected later code overlap is:

- `src/clj_surgeon/mcp_tool.clj`;
- `src/clj_surgeon/mcp_telemetry.clj`;
- `src/clj_surgeon/mcp_server.clj` only for registry cross-product evidence;
- their focused tests and the reload manifest only if wrapper Vars require it.

The naming harness owns candidate catalogs, descriptions, prompts, and model
cohorts. It must not edit the identity ratchet's handler or telemetry seam in
parallel. The identity slice must land first or remain a separately
cherry-pickable prerequisite.

## Decision

Approve the identity separation as a behavior-preserving prerequisite. Do not
approve a public rename or Option M from this HLD alone.
