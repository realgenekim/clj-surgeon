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

## Orthogonality Principle

Select a tool from the source of intent. Do not select it from the requested
safety strength:

```text
bounded evidence or prepared semantic basis -> inspect_clojure
exact changes stated as data                -> edit_clojure
one bounded Clojure rule computes changes   -> transform_clojure
namespace movement and caller semantics     -> extract_clojure
inspect-produced basis plus decisions       -> apply_clojure_plan
```

Parse, stale-source guards, atomic commit, read-back, rollback, receipts, and
verification are cross-cutting mutation capabilities. Exact verification must
not force an otherwise explicit edit into `apply_clojure_plan`. Likewise, a
stronger rollback policy does not change extraction intent into plan-apply
intent.

Every pair of public tools must be disjoint or have one documented composition.
If both tools accept the same complete standalone intent, the next design phase
must add a schema ratchet or reject the five-stop surface.

## Pairwise Operation Matrix

| Pair | Intended boundary or composition | Current overlap to audit | Falsifier or schema ratchet |
|---|---|---|---|
| Inspect / edit | Inspect supplies evidence only when judgment is still required. A complete exact edit calls edit directly. | Agents can inspect as ritual before an already-decided edit. | A direct exact-edit cohort must use zero inspection. A returned exact anchor may compose with edit only when the task required that evidence. |
| Inspect / transform | Inspect can establish exact selections before a separately judged rule. A fully supplied transform calls transform directly. | Both can select structural forms. | Transform must own its declared selections and frozen guards. It must not require an outline or inspect preflight. |
| Inspect / extract | Inspect answers general evidence questions. Extract owns namespace movement, dependency closure, visibility, and caller disposition. | Current inspect supports `plan-extraction`. | Retire or redirect the public extraction-planning branch before claiming orthogonality. An extraction request must not require inspect first. |
| Inspect / apply plan | This is the intended preparation chain. Inspect returns the exact basis and `next_call`; the model fills only decision holes. | A caller can attempt to construct basis fields manually. | Apply-plan must require and validate inspect-produced provenance, frozen source identity, and complete decisions. An unproven direct call refuses before effects. |
| Edit / transform | Edit states exact changes as data. Transform states one bounded Clojure rule and previews by default. | Current editor accepts `programs` alone; transform can commit the same standalone SCI rule. | Program-only requests belong to transform. Edit may include a program only beside at least one explicit edit or owner deletion when one mixed atomic chord is required. |
| Edit / extract | Edit changes named syntax. Extract owns namespace creation, movement closure, requires, visibility, and callers. | A manual delete-and-insert sequence can imitate part of extraction. | Edit must not accept extraction policy or create an extraction destination. A move expressed as manual text operations fails the route gate. |
| Edit / apply plan | Edit is direct explicit intent. Apply-plan consumes only an inspect-produced basis and decisions. | The current broad apply tool accepts compact edits directly. | The five-stop apply-plan schema accepts no standalone edit, change, extraction, or computed-program request. |
| Transform / extract | Transform computes changes inside declared structural selections. Extract moves namespace owners and resolves dependency/caller policy. | A program could attempt to delete and recreate moved forms. | Transform cannot create the extraction destination or claim dependency and caller proof. Namespace movement routes to extract. |
| Transform / apply plan | Transform owns a caller-supplied SCI rule. Apply-plan owns decisions over an inspect-produced basis. | Prepared decisions and computed replacements can both change several sites. | Apply-plan accepts decision data, not executable SCI. Transform accepts no retained semantic basis. |
| Extract / apply plan | These tools do not compose. Extract handles both direct success and its own exact retry. | The earlier draft incorrectly routed extraction continuation through apply-plan. | Any extraction payload or extraction continuation sent to apply-plan refuses. Extract's completed `next_call` targets extract again. |

### Cross-cutting verification matrix

| Tool outcome | May verify? | Selection rule |
|---|---:|---|
| `inspect_clojure` read or basis | no mutation verification | Read-only by contract. |
| `edit_clojure` commit | yes | Still selected because intent is exact data. |
| `transform_clojure` preview | no | Preview has no mutation to verify. |
| `transform_clojure` commit | yes | Still selected because intent is a bounded rule. |
| `extract_clojure` commit or retry | yes | Still selected because intent is namespace movement. |
| `apply_clojure_plan` commit | yes | Selected only because inspect produced the basis. |

The current narrow editor omits `verify`. That is a known public-shape gap
under this principle. Do not preserve a non-orthogonal escape hatch where an
agent calls the broad plan tool only to obtain exact verification. A later LLD
must preserve the compact editor's byte-exact behavior while deciding how a
project-owned verifier composes with each mutation stop.

## Option M: First-Person LLM-User Critique

Corrected Option M has five stops:

```text
inspect_clojure      bounded evidence and preparation
edit_clojure         decided literal edits and a mixed atomic chord
transform_clojure    bounded SCI transform; preview by default
extract_clojure      one-shot extraction when mechanically complete;
                     otherwise return its completed exact retry next_call
apply_clojure_plan   basis + decisions from inspect next_call only
```

As an LLM user, I prefer this organ to the current broad
`apply_clojure_changes` schema. Each stop describes the decision I am making,
not the transaction kernel underneath it.

The best interaction is extraction:

```text
I know source, destination, and forms
    -> extract_clojure
       -> mechanically complete: commit + verify + terminal response
       -> genuine unknown: completed frozen extraction next_call
          -> fill exact decision holes
          -> extract_clojure again

I have an inspect-produced semantic basis
    -> fill only the returned decision holes
    -> apply_clojure_plan(basis, decisions)
```

This preserves the measured 12.070-second deletion of a mandatory public plan
phase. The extraction continuation remains the current completed,
snapshot-bound `next_call`; there is no executable extraction `plan_id` or
server plan store. I copy the returned call and fill only genuine decisions.

`apply_clojure_plan` is different. It is the narrow consumer of a semantic
basis returned by `inspect_clojure`. I should never choose it for extraction or
construct its provenance from scratch.

I would still hesitate at four boundaries:

1. **Edit versus transform.** If `edit_clojure` accepts computed programs in a
   mixed chord, I need one rule: use edit when the commit decision is complete;
   use transform when I need to preview a computed relation.
2. **Inspect versus extract.** If the extraction target and forms are known, I
   want to call `extract_clojure` directly. Inspection must not become a
   preflight ritual.
3. **Continuation identity.** Both returned calls must bind workspace, source
   hashes, contract version, exact candidate universe, and decision holes.
   Concurrent source changes and stale retries must refuse safely. A missing
   receipt is unverified, never safe to replay.
4. **Compatibility.** A visible `apply_clojure_changes` alias beside the five
   stops gives me two plausible keys and can restore the current hesitation.
   Prefer an unlisted stale-client alias or a scheduled fresh-session cutover.

The prior compact-`plan_id` experiment was rejected. It cut visible bytes by
only 49.5% to 52.4% and did not reduce deliberation. Option M does not reopen
that handle. It uses the existing completed extraction `next_call` and the
existing inspect basis contract, each with exact decision holes.

My preferred name inside Option M is still `apply_clojure_plan`, because it
mutates and arrives as an exact `next_call` from inspection. The model should
not select it from an open-ended catalog decision. `continue_clojure_plan`
would describe the state transition more precisely, but “continue” hides the
write effect. `apply` is the safer warning.

## First-Person Naming Riff

Gene's proposed semantic boundary is right:

```text
state exact changes as data
versus
state one bounded Clojure rule that computes changes
```

As an LLM user, I prefer the short canonical pair
`edit_clojure` / `transform_clojure` with those two sentences at the start of
their descriptions.

| Candidate | First-person reaction | Literal truth |
|---|---|---|
| `edit_clojure_literals` | I expect only literal `from`/`to` replacements. | False for owner deletion, binding rename, `assoc_entry`, and any mixed computed program. |
| `edit_clojure_explicitly` | I understand that I must state the changes rather than supply a rule. The adverb feels awkward but the boundary is mostly true. | Best long-form challenger. It still needs a definition of explicit insertion, deletion, rename, and association. |
| `edit_clojure_by_example` | I expect the tool to infer a generalized rewrite from examples. That sounds unsafe. | False for exact data and dangerous because it implies inference. |
| `edit_clojure_forms` | I expect whole-form operations. I may avoid it for nested edits or EDN. | Too broad and too narrow at once. Transform and extract also operate on forms. |
| `transform_clojure_with_clojure` | I immediately understand that the rule itself is Clojure. The name is memorable but long and repetitive. | True only if every transform program is one bounded Clojure/SCI function. |

Under the proposed widened edit chord, “literal” is not the common law:

| Edit action | Stated as exact data? | Literally a source replacement? |
|---|---:|---:|
| Replace | yes | yes |
| Insert complete forms | yes | yes for inserted source |
| Delete exact owners | yes | no |
| Rename one resolved binding | yes | no; it is a bounded relation |
| Associate one key/value | yes | no; it is a structural action |
| Mixed computed program | only as justified chord composition | no; a rule computes the result |

The truthful umbrella is “explicit changes as data,” not “literals.”

I would test `edit_clojure_explicitly` and
`transform_clojure_with_clojure` as description-first challengers. I would not
rename the proven tools unless fresh Sol and Fable callers select them more
accurately or materially faster.

### Bang means effect, not intent

The official MCP tool-name character set does not permit `!`. Canonical names
must use letters, digits, `_`, `.`, or `-`.

| Portable alternative | Benefit | Cost |
|---|---|---|
| `edit_clojure_commit` | The write effect is explicit and the suffix groups preview and commit variants. | Adds another public stop and selects by lifecycle rather than intent. |
| `commit_clojure_edit` | Begins with the irreversible action. | Sorts away from its preview sibling and still duplicates the intent surface. |
| `edit_clojure.commit` | Resembles a namespaced method and uses a permitted character. | Dot handling and display quality vary across clients. |
| Human title `edit_clojure!` | Can communicate effect without a nonportable canonical name. | MCP clients and models do not consistently expose or use titles. It cannot be the safety boundary. |

Do not recommend a nonportable bang name. Use the operation kind, lifecycle,
tool annotations, description, and receipt contract to state effects. Add a
separate commit-named tool only if a caller experiment proves that preview and
commit cannot coexist safely under one intent-selected stop.

## Option M Adversarial Gates

Identity separation is a prerequisite, not approval to publish Option M.
Option M must pass these additional gates:

1. A mechanically complete extraction remains one tool call, one commit, one
   exact verifier, and one terminal response.
2. Its complete wall does not regress the current Sol extraction median by
   more than 5%.
3. A genuine-unknown extraction performs no write and returns one completed,
   snapshot-bound extraction `next_call` with only genuine decision holes.
4. The extraction retry targets `extract_clojure`, never
   `apply_clojure_plan`.
5. `apply_clojure_plan` accepts only an inspect-produced basis plus complete
   decisions. It accepts no extraction request or standalone direct change.
6. Source drift, contract drift, wrong workspace, and duplicate application
   have typed fail-closed outcomes.
7. Fresh Sol/high chooses the correct stop on exact edit, computed preview,
   direct extraction, and extraction continuation tasks without repository
   coaching.
8. Fable and Opus may use deferred discovery, but Sol retains its direct
   first-action path.
9. No visible compatibility alias duplicates a large schema unless a matched
   cohort proves no selection or complete-wall regression.
10. Current `edit_clojure` and extraction semantic scorers remain unchanged.
11. The pairwise matrix has no unexplained standalone overlap. Program-only
    edit versus transform and inspect planning versus extract are mandatory
    falsifiers.
12. Exact verification composes with each mutation intent without routing the
    caller through the wrong stop.

## Scope and Non-Goals

This HLD slice does not:

- rename or add a public tool;
- add `operation_kind` to public structured output;
- change schemas, descriptions, routing, recovery, or benchmark prompts;
- change terminal-response eligibility;
- implement an extraction `plan_id`, a plan store, or Option M;
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
