---
parent: high-level-design
prefix: MCP-OP-PREP-REQ
status: pre-ratification
---

# Prepared Guarded Edit Request

This is a leaf LLD skeleton. The current ratification decision is the HLD
option and its non-product proxy experiment only. This skeleton is not yet
ratifiable as the final LLD and authorizes no tests or implementation.

## Context and causal evidence

The prepared guarded request was the only Sweep-2 intervention with causal
routing credit. The tested object was complete executable JSON in the user
prompt. The proposed product object is a non-executable template in a tool
result. Transfer is unproved and must be killed cheaply if it does not appear.

The product hypothesis is that a visible prepared template removes mechanical
call assembly and makes the structural mutation route salient. It is not a
request-byte optimization.

## Proposed first-slice decision

An eligible successful `inspect_clojure` result may carry one visible,
non-executable `prepared_request` for `edit_clojure`. The server pre-fills only
facts proved by the completed read. Replacement values remain explicit
caller-owned holes.

The candidate first slice applies to existing `.clj` files in repository
source or test roots. The final LLD must define those roots and the exact
eligible inspect operations before ratification. The slice does not apply to
new files, EDN, CLJC platform branches, retained semantic bases, refusals, or
verification profiles.

## Vocabulary

- **Prepared request:** a descriptor whose `arguments` are public JSON shaped
  like an `edit_clojure` call except for explicit caller-owned null holes.
- **Editable selection:** exact selected old source with one uniquely named
  top-level owner, one project-relative file, and exact cardinality.
- **Caller-owned hole:** one `to` field whose value the server cannot choose.
- **Evidence label:** a normalized inspect request ID used only to explain the
  origin of a template item.
- **Executable:** schema-valid and complete enough to invoke. A prepared
  request is not executable.

## First-slice eligibility

These decisions block LLD ratification. The current candidate boundary is:

- the typed inspect batch completes successfully;
- every prepared item comes from an exact editable selection;
- every selected item is inside one uniquely named top-level owner;
- selected source and exact cardinality are present;
- the canonical workspace and project-relative file are known;
- the complete prepared request fits the existing result budget; and
- no ambiguity, continuation, semantic-provider judgment, or retained basis is
  involved.

A broad owner read does not authorize the server to guess which subtree the
caller will change. It may qualify only as an explicit whole-owner selection,
subject to the result budget. Otherwise no prepared request is emitted.

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

The exact JSON spelling remains a ratification decision. The final shape must
use only public `edit_clojure` fields. It must not expose EDN carriage, a
private basis, a saved plan, or a new handle.

## Construction

The design shall define one pure projection from completed read result data to
either a complete prepared request or an omission reason. The projection must
not read source again, call a semantic provider, infer a replacement, or
persist state.

Each prepared edit repeats explicit file and owner identity. Request IDs may
link result evidence to template items, but the IDs do not replace identity.

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

Static coaching text may name the tool and the hole-filling action. It must not
interpolate source, request, user, file, or network content into instruction
text. Those values remain structured data.

## Salience contract

The structured result contains the complete prepared descriptor. The concise
summary preserves the ordinary read's dynamic labels, counts, and elapsed
evidence. One additional static sentence states: if the caller independently
decides that these exact selections are the intended mutation subjects, fill
every replacement hole and submit `prepared_request.arguments` once to
`edit_clojure`; otherwise, ignore the template.

The summary must not claim that the user approved the edit, that the server
verified replacement intent, or that mutation is the next required action.
The ordinary read retains `next_action=none`. Clients must not auto-consume or
auto-execute the template.

## Omission and refusal behavior

If any eligibility condition fails, the result contains no partial prepared
request. The ordinary read success or refusal stays unchanged. The result may
include a typed omission reason for telemetry, but omission is not a read
failure.

The final LLD must fix the behavior matrix for:

| Case | Candidate outcome |
|---|---|
| Exact selected source inside one named owner | One prepared request. |
| Several exact selections with complete identity | One ordered prepared batch, if within budget. |
| Source omitted | No prepared request. |
| Zero or several owners | No prepared request. |
| Broad owner read without explicit whole-owner selection | No prepared request. |
| Selector refusal or continuation | Existing refusal; no prepared request. |
| Result budget exceeded | No partial template; ordinary read remains complete. |
| Source changes before apply | Ordinary stale-source or match refusal; no write. |
| Caller leaves a hole null | Ordinary public schema refusal; no write. |
| Caller fills malformed replacement | Ordinary compiler refusal; no write. |

## Execution equivalence

After the caller fills every hole, `prepared_request.arguments` is an ordinary
public `edit_clojure` request. The existing schema, compact compiler, frozen
source capture, exact owner and match guards, atomic transaction, read-back,
receipt, and rollback laws remain authoritative.

Preparation grants no alternate executor and no inherited authority. The
server may recapture current source during the ordinary edit call. Any drift
must fail through the existing guard contract.

## Result budget

The final LLD must state one byte and item budget. The template must never
truncate identity, old source, counts, holes, or guards. If the complete object
does not fit, omit it. Result bytes are not a reason to compress identity.

## Telemetry

The server may record only template eligibility and emission. The experiment
controller or client, not the product result contract, owns all later route and
task evidence. That external evidence must distinguish:

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

## Adoption experiment gate

Before product implementation, an experiment-only MCP response proxy preserves
the production catalog and handlers and changes only the successful inspect
projection. It adds the frozen null-hole prepared descriptor in treatment and
performs no product edit or source mutation. The packet's eight-attempt
unforced proxy experiment is the first gate. All sessions remain in the
denominator.
Prepared-result evidence cannot be promoted from a forced-inspection or
exposed-only cohort.

The experiment advances the LLD only at 3/4 prepared Surgeon-first attempts,
at least +25 percentage points over control, no correctness loss, no refusal
increase, and zero mutations across four additional read-only safety attempts,
two per arm. Complete task wall, observable client actions, and tool calls
remain descriptive losses and cannot rescue a failed routing or safety gate.

## Non-goals

- No dedicated public `prepare_edit` tool in the first slice.
- No executable `next_call` from an ordinary read.
- No replacement inference, generation, or repair.
- No prepared request on a refusal.
- No retained basis, plan ID, or opaque selection handle.
- No EDN request encoding.
- No identity compression or positional subject authority.
- No verifier selection.
- No adoption or speed claim before the clean-context experiment passes.

## Verification skeleton

After ratification, the test phase must cover:

1. a pure eligibility and projection matrix;
2. exact public schema parity after every hole is filled;
3. no template for every ineligible or ambiguous case;
4. no source or plan I/O during projection;
5. ordinary stale-source, owner, count, and malformed-replacement refusals;
6. static summary text with no untrusted interpolation;
7. exact preservation of read result IDs, hashes, and elapsed evidence; and
8. a read-only intent that retains `next_action=none` and never mutates; and
9. one real MCP response followed by one ordinary guarded edit on a copied
   real-program fixture.

## Alternatives

The competing options and deciding assumptions are in [README.md](README.md).
The dedicated preparation operation and refusal-correction surface remain
separate options. Neither is implied by ratifying this skeleton.

## Open ratification questions

1. Which inspect operations can prove an editable selection without a second
   read or server judgment?
2. Should the first slice allow a prepared batch or exactly one edit?
3. What byte and item budget is small enough for every eligible result?
4. Is the concise summary necessary for salience, or does it create an
   instruction-authority concern?
5. Does the unforced transfer experiment clear the registered gate?

These questions must be closed in a post-experiment LLD draft. A distinct Gene
ratification is required before the EARS registry can activate.
