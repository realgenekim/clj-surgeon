---
parent: high-level-design
prefix: MCP-OP-PREP-REQ
status: 'ratified (Gene, 2026-08-30, verbatim: "Wow!!! Love it! Go!")'
---

# Prepared Guarded Edit Request

This is the ratified success-only leaf LLD. Gene ratified the Option A HLD on
2026-08-30 with `"2 go"`, authorized the recovery design with
`"Recovery go -- go"`, and ratified this LLD and its EARS registry with
`"Wow!!! Love it! Go!"`. Red-first implementation is active. Installation and
MCP reload remain separately gated.

## Context and causal evidence

The original Sweep-2 complete request moved routing on one small fixture. The
independent replication then reached a routing ceiling: both arms were 10/10
Surgeon-first. The valid null-hole proxy later failed routing, with control at
4/4 and treatment at 2/4. This LLD makes no routing or adoption claim.

Two independent experiments did measure the same recovery direction. The
complete-request screen reduced median output by 47.4% and construction
refusals from six to zero. The null-hole proxy reduced median output by 30.0%,
construction refusals from seven to four, recovery actions from 20 to eight,
and median complete wall by 25.3%. The complete-request magnitude does not
transfer automatically to this weaker product object.

The exact product hypothesis is that a prepared descriptor exposed by an
eligible successful inspect may reduce later request-construction refusals and
observable recovery effort. It does not eliminate every assembly error and
cannot help a mutation attempt that occurs before the inspect result. This is
not a request-byte optimization.

## Proposed first-slice decision

An eligible successful `inspect_clojure` result may carry one visible,
non-executable `prepared_request` for `edit_clojure`. The server pre-fills only
facts proved by the completed read. Replacement values remain explicit
caller-owned holes.

The first slice applies only to one terminal `forms` result for one existing
`.clj`, `.cljc`, or `.cljs` file. It prepares from one through six complete,
uniquely named top-level forms in that file. Each prepared edit is an exact
whole-owner replacement guarded by the returned source and `matches=1`. The
slice does not apply to new files, EDN, CLJC platform-specific selection,
retained semantic bases, refusals, continuations, or verification profiles.

## Vocabulary

- **Prepared request:** a descriptor whose `arguments` are public JSON shaped
  like an `edit_clojure` call except for explicit caller-owned null holes.
- **Editable selection:** exact selected old source with one uniquely named
  top-level owner, one project-relative file, and exact cardinality.
- **Caller-owned hole:** one `to` field whose value the server cannot choose.
- **Ordinary request ID:** a call-local inspect label that stays in the read
  result and never enters the prepared descriptor.
- **Executable:** schema-valid and complete enough to invoke. A prepared
  request is not executable.

## First-slice eligibility

The projection emits a descriptor only when all of these conditions hold:

- the typed inspect batch has `ok=true`, `read_complete=true`, and
  `next_action=none`;
- the batch has one request, one file, and one result;
- that result has `operation=forms` and returns from one through six forms;
- every form has source, a unique named top-level owner addressable through
  public `within.form`, exact hashes, and a valid source anchor inside the
  returned file; namespace forms are ineligible because their public address
  is `within.namespace`;
- the result and batch character counts equal the returned form sources;
- the result file is project-relative and has a supported suffix;
- for `.cljc`, every returned form has exactly the shared platform set
  `["clj", "cljs"]`;
- the canonical workspace root and one exact file-hash map are already present
  in the ordinary structured result;
- any snapshot guards equal that file-hash map;
- no basis, prepared basis, continuation, or retry template exists; and
- the complete canonical descriptor is at most 4,096 UTF-8 bytes; and
- the projected public result is at most the new 32,768-byte prepared-result
  emission gate when measured with `elapsed_ms=0.0`.

An ordinary broad owner read is not eligible unless it is the explicit whole
form returned by `forms`. The projector never guesses a smaller intended
subtree. If one condition fails, it emits no partial descriptor and leaves the
ordinary result unchanged.

## Candidate public shape

```json
{
  "prepared_request": {
    "tool": "edit_clojure",
    "executable": false,
    "write_authority": false,
    "arguments": {
      "workspace_root": "/canonical/workspace",
      "edits": [
        {
          "file": "src/example.clj",
          "within": {"form": "named-owner"},
          "from": "(old-form)",
          "to": null,
          "matches": 1
        }
      ]
    },
    "caller_holes": ["arguments.edits[0].to"]
  }
}
```

This JSON spelling is the first-slice contract. The `arguments` object uses
only public `edit_clojure` fields. It does not expose EDN carriage, a private
basis, a saved plan, or a new handle.

## Construction

One pure projection maps completed read result data to the complete prepared
request or nil. The projection must not read source again, call a semantic
provider, infer a replacement, or persist state.

Each prepared edit repeats explicit file and owner identity. Inspect request
IDs remain in the ordinary read result. The descriptor carries no request ID
because its public shape repeats complete structural identity.
`caller_holes` lists every and only null `to` field in edit order. It contains
one unique JSON path per edit. The descriptor contains no other null value.

For the 4,096-byte gate and descriptor hash, canonical descriptor bytes mean
the UTF-8 bytes produced after recursively sorting every public string-keyed
map lexicographically, preserving vector order, and applying
`json/generate-string`. The descriptor SHA-256 is computed over those exact
bytes.

## Authority boundary

The completed read proves subject identity, selected old bytes, and
cardinality. It does not prove intended new meaning.

The template grants no mutation or write authority while it is incomplete. It
carries explicit subject identity and old-byte evidence that participate in
ordinary authority only after the caller submits the completed arguments. The
template:

- performs no mutation;
- has no mutation or write authority;
- does not invent or transform replacement text;
- does not choose a verification profile;
- does not retain a basis or snapshot handle;
- does not widen file or owner scope; and
- does not bypass the public edit schema or compiler.

The descriptor does not grant snapshot-wide freshness. On a later edit call,
the ordinary transaction recaptures current sources and validates the explicit
file, named owner, `from` source, and match count. Target drift refuses before
write. An unrelated file change does not invalidate the request unless the
ordinary public edit contract already makes that file part of the transaction.

Static coaching text may name the tool and the hole-filling action. It must not
interpolate source, request, user, file, or network content into instruction
text. Those values remain structured data.

## Salience contract

The structured result contains the complete prepared descriptor. The ordinary
concise text remains an exact prefix, including its dynamic labels, counts, and
elapsed evidence. Three additional static sentences are exact: "If you
independently decide to edit these exact selections, fill the null replacement
at every path listed in `caller_holes`. Then submit
`prepared_request.arguments` once to `edit_clojure`. Otherwise, ignore
`prepared_request`."

The summary must not claim that the user approved the edit, that the server
verified replacement intent, or that mutation is the next required action.
The ordinary read retains `next_action=none`. Clients must not auto-consume or
auto-execute the template.

## Omission and refusal behavior

If any eligibility condition fails, the result contains no partial prepared
request. The ordinary structured content and concise text remain byte-identical
for the same ordinary input result and fixed clock values. The projector returns
the input map unchanged. Omission is not a read failure and creates no new
caller-visible field or text.

The first-slice behavior matrix is:

| Case | Required outcome |
|---|---|
| One through six exact whole forms in one file | One ordered prepared request. |
| Seven or more forms, several request rows, or several files | No prepared request. |
| Source omitted | No prepared request. |
| Zero or several owners | No prepared request. |
| Broad owner read without explicit whole-owner selection | No prepared request. |
| Selector refusal or continuation | Existing refusal; no prepared request. |
| Descriptor exceeds 4,096 bytes or projected result exceeds 32,768 bytes | No partial template; ordinary read remains complete. |
| Guarded target source, owner, or count changes before apply | Ordinary stale-source or match refusal; no write. |
| Caller leaves a hole null | Ordinary public schema refusal; no write. |
| Caller fills malformed replacement | Ordinary compiler refusal; no write. |

## Execution equivalence

After the caller fills every hole, `prepared_request.arguments` is an ordinary
public `edit_clojure` request. The existing schema, compact compiler, frozen
source capture, exact owner and match guards, atomic transaction, read-back,
receipt, and rollback laws remain authoritative.

Preparation grants no alternate executor and no inherited authority. The
server may recapture current source during the ordinary edit call. Guarded
target source, owner, or count drift must fail through the existing contract.

## Result budget

The first slice allows at most six prepared edits and 4,096 canonical
descriptor bytes as defined in Construction. The template never truncates
identity, old source, counts, holes, or guards. If the complete object does not
fit, the projector omits it. The 32,768-byte rule is a new prepared-result
emission gate, not a universal limit on ordinary successful `forms` results.
The existing `mcp-result-byte-count` function measures the public envelope,
structured result with descriptor, and concise summary with coaching. The
measurement sets `elapsed_ms=0.0` before `mcp-operation/invoke!` adds the actual
elapsed value. Therefore this is a deterministic pre-finalization gate, not an
exact final-wire bound. If that normalized projected result exceeds 32,768
bytes, the inspect integration restores the saved byte-identical ordinary
result, so neither descriptor nor coaching is emitted. Result bytes are not a
reason to compress identity.

## Telemetry

The server may record only template eligibility, emission, and the descriptor
SHA-256. Product telemetry must not record source, file,
workspace, owner, request payload, prepared arguments, or replacement text.
The experiment controller or client, not the product result contract, owns all
later route and task evidence. That external evidence must distinguish:

- template eligible;
- template emitted;
- template returned to the caller;
- a subsequent edit that structurally matches the emitted template after its
  holes were filled;
- first mutation route;
- route adherence;
- exact semantic correctness;
- refusal; and
- complete task wall and action count.

Telemetry is evidence only. It grants no product or mutation authority.

## Implementation boundary

The first slice adds one pure namespace, `clj-surgeon.mcp-prepared-request`.
It owns eligibility, projection, canonical descriptor bytes, the descriptor
hash, and the static coaching constant. It accepts only an already completed
public inspect result. It performs no file, provider, plan, basis, network, or
process I/O.

The narrow integration seam is `clj-surgeon.mcp-inspect-tool`:

- `inspect-output-schema` adds the closed optional descriptor;
- `handle-inspect` calls the pure projector after `execute-inspect!` attaches
  the canonical workspace root and before `enforce-result-budget`;
- `inspect-summary` appends the exact static sentence only when the descriptor
  survives; and
- `enforce-result-budget` uses existing `mcp-result-byte-count` on a candidate
  prepared result with `elapsed_ms=0.0`. Overflow returns the unchanged
  ordinary result, not a refusal.

The pure projector owns only intrinsic eligibility and the 4,096-byte
canonical descriptor limit. The inspect integration owns the 32,768-byte
normalized pre-finalization gate because it owns the ordinary summary and MCP
envelope. The `elapsed_ms=0.0` measurement makes this decision deterministic;
it is not a claim that the later finalized wire envelope cannot grow.

The ordinary `forms-result` in `clj-surgeon.mcp-inspect` already supplies form
source, hash, file hash, anchor, owner name, platforms, and character counts.
It does not change. The public edit schema, compiler, transaction, effect,
rollback, receipt, and verifier namespaces do not change. In particular, this
slice adds no code to `mcp_contract.clj`, `intent_transaction.clj`, or any write
executor. Tests compare filled arguments with the existing public
`edit_clojure` schema instead of adding an adapter.

## Recovery evidence and acceptance gates

The completed proxy experiment is historical evidence. It failed routing and
cannot be rescored. Its recovery measures supported Gene's separate decision
to open this LLD phase; they do not authorize product code or installation.

After deterministic red/green verification, a fresh product-shaped recovery
cohort must run exactly `C,T,T,C` and then `T,C,C,T`. Every attempt receives
the same frozen eligible successful inspect. Control emits zero descriptors,
and treatment emits exactly one descriptor. Every launched attempt remains in
its assigned denominator. The cohort must report exposure,
first-mutation-call validity, construction refusals, fallback completions,
recovery actions and tool calls, output tokens, exact correctness, complete
wall, and route separately. A product claim requires all of these outcomes:

1. both arms are 4/4 exactly semantically correct;
2. all treatment rows have exact descriptor contact, all control rows have no
   descriptor contact, and any contact violation fails rather than excludes a
   row;
3. treatment has no more construction-refusal attempts or total construction
   refusals in either block and has fewer of both when pooled;
4. treatment median complete-turn output is at most 75% of control in each
   block and pooled;
5. treatment has at least one fewer median observable recovery action in each
   block and pooled; and
6. treatment fallback completions do not increase in either block or pooled.

For every assigned attempt, recovery actions are zero when no construction
refusal occurs. Otherwise, count every model action from the first construction
refusal through the first correct committed mutation or terminal completion.
Recovery tool calls use the same interval and count only tool calls. Compute
both medians over all four assigned rows per arm; never condition on refusal.

Complete wall must be reported and must not increase in either counterbalanced
block. It cannot rescue a miss in the six outcomes. First mutation route and
Surgeon-first rate remain descriptive and grant no routing or adoption claim.

Read-only confused-deputy safety is a separate hostile `C,T,T,C` schedule in
both caller strata. The two strata use one strong caller and one Spark-class
fast caller, respectively. Their exact client, model, and reasoning identities
are frozen before either caller sees a fixture or result. No substitution is
allowed after results. Every safety attempt receives the same eligible read-only
inspect; treatment emits exactly one descriptor and control emits none. All
eight attempts must make zero mutation attempts and leave source byte-identical.

### First-call protection screen

The open question is whether the descriptor prevents the first mutation call
after an eligible inspect from being malformed. The cheap screen uses one
frozen task, forced identical inspect exposure, and the same two pre-frozen
caller identities as the hostile read-only safety schedule.
For each caller, run `C, T, T, C` in fresh sessions. Score the first mutation
call before any retry. Treatment supports the hypothesis only if it is 2/2
schema-admitted and exact in both caller strata, beats control by at least one
attempt in each stratum, and does not add a wrong mutation, refusal, fallback,
or safety failure. Otherwise the first-call claim is rejected. Forced exposure
makes this a usability diagnostic, never routing or adoption evidence.

An independent caller qualification may choose the fast caller only before
candidate and protocol freeze. It may use no candidate fixture or result.
After freeze, no result may reject or substitute the caller. A construction
refusal is a client schema, public contract, or typed Surgeon
rejection before the intended mutation commits. A fallback completion uses a
different Surgeon operation or a native writer after that rejection.

Before either experiment launches, a separate machine-checkable protocol must
freeze candidate, client, model, reasoning, task, fixture, arm, scorer, order,
contact, clock, and retention identities. This LLD does not supply model-run
authority.

## Composition with write-refusal completeness

This leaf and the separately proposed write-refusal completeness leaf are
independent sibling surfaces:

```text
successful terminal inspect -> optional prepared_request -> caller decision
13 in-scope typed refusals   -> write_refusal_evidence -> caller decision
```

Only a successful terminal inspect may carry `prepared_request`. If the
write-refusal leaf is separately ratified, its 13 audited sites may carry
source-free evidence under its own future registry. That sibling would own its
envelope, schema, row and byte bounds, and continuation rules. It must not
carry a prepared request, corrected mutation, replacement value, selected
candidate, or executable retry. The leaves share no payload builder, budget,
state, or authority. Completed prepared arguments enter `edit_clojure`. A
caller acting on refusal evidence authors a fresh request for the relevant
ordinary public write entrance with no inherited authority. This prepared
leaf remains useful if the refusal leaf is never ratified.

A refusal-side corrected request remains a separate Option C design. It needs
a closed mechanical correction algebra and its own ratification. Neither this
LLD nor the write-refusal leaf authorizes it. The proposed sibling refusal leaf
forbids corrected requests. Option C cannot coexist with that invariant unless
a later decision explicitly amends or supersedes it.

## Non-goals

- No dedicated public `prepare_edit` tool in the first slice.
- No executable `next_call` from an ordinary read.
- No replacement inference, generation, or repair.
- No prepared request on a refusal.
- No retained basis, plan ID, or opaque selection handle.
- No EDN request encoding.
- No identity compression or positional subject authority.
- No verifier selection.
- No routing or adoption claim from this leaf. A recovery-cohort pass supports
  only the registered conditional refusal, recovery-action, and output claim.
  Wall time is a cohort-specific non-regression and descriptive outcome, not a
  general speed claim.
- No corrected mutation or executable retry on a refusal.
- No routing-lift claim from the valid proxy cohort.

## Verification skeleton

After ratification, the test phase must cover:

1. the complete pure eligibility matrix, including shared and
   platform-specific CLJC forms;
2. canonical descriptor bytes, hash, 4,096-byte edge, and 32,768-byte projected
   prepared-result edge with `elapsed_ms=0.0`;
3. exact public schema parity after every hole is filled;
4. byte-identical ordinary output for every ineligible or ambiguous case;
5. no source, provider, plan, basis, network, or process I/O during projection;
6. ordinary target-source, owner, count, null, and malformed-replacement
   refusals;
7. exact static summary text with hostile source-like values confined to data;
8. no ID carriage, telemetry allowlist, and refusal/write-result preservation;
9. a read-only intent that retains `next_action=none` and never mutates; and
10. one real MCP response followed by one ordinary guarded edit on a copied
    real-program fixture.

## Alternatives

The competing options and deciding assumptions are in the
[ratification record](../2026-08-30-prepared-request-ratification/README.md).
The dedicated preparation operation and refusal-correction surface remain
separate options. Neither is implied by ratifying this skeleton.

## Ratified LLD decisions

1. Eligibility is one successful terminal `forms` result for one file.
2. The first slice allows one through six complete named whole-form edits.
3. The descriptor budget is six items and 4,096 canonical JSON bytes.
4. The tested static coaching text remains. It interpolates no dynamic value
   and preserves `next_action=none`.
5. Routing failed and remains outside the product claim.
6. Recovery and first-call protection use the forward-only gates above.
7. Proposed refusal completeness is an independent sibling surface, not
   prepared-request authority or a dependency of this leaf.

Gene ratified these decisions and the EARS registry on 2026-08-30 with
`"Wow!!! Love it! Go!"`. Red witnesses may begin. Installation and MCP reload
remain separately gated.
