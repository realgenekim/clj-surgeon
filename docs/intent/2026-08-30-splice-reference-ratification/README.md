# Selective Splice-by-Reference: Ratification Packet

Date: 2026-08-30 PT

Status: proposed HLD decision and LLD skeleton. No EARS registry, tests, product
code, installation, or runtime change is authorized by this packet.

Gene authorized the design phase with: `Go!! Make it a reality that we can
trust!!! Go!!` This packet stops for ratification before the LID arrow enters
EARS, tests, or code.

## Decision requested

Choose how clj-surgeon may let a caller avoid re-emitting exact old source that
the server just returned.

The recommendation is **Option B: a slot-bound splice reference inside the
existing prepared-request and `edit_clojure` surfaces**. The reference may
compress only the old bytes of an exact selection that the preceding read
already proved. It may not select a file, owner, or arbitrary candidate span.
The completed request lowers to the existing canonical compact edit and
transaction path.

The fallback is to retain the experiment as evidence and ship nothing if the
parallel twin-span replication cannot prove that a valid reference for one
span is impossible to use for another span.

## Problem

Large exact replacements can require the caller to emit the same old source
twice: first as read selection evidence and then as the guarded `from` value in
the write. This is expensive when the old source is a wall of text and the
caller already had to read it.

The problem is not universal. The retained corpus contains 332 `from`/`to`
pairs across 195 writes. The median write saves zero under the realistic
selective projection. Only 82 pairs across 60 writes are economically
eligible, and their projected saving is 49,656 of 630,138 write bytes, or
7.9% corpus-wide. A universal request grammar or extra read turn would charge
the common case for a minority wall class.

The safety problem is stricter than byte preservation. A well-formed but wrong
reference can identify a real current span and still mutate the wrong effect
site. A snapshot proves that a selected span exists and is current. It does
not prove that the caller intended that span. The experiment avoided this
problem with one span per owner and a fixture-only expected-target oracle that
the generic product does not possess.

## Evidence and limits

### Surviving mechanism

The synthetic screen at `588893fc` compared ordinary re-quotation with
snapshot-bound `from_ref` values on one four-owner task:

| Measure | Ordinary quote | Reference | Change |
|---|---:|---:|---:|
| Exact completed tasks | 8/8 | 8/8 | tied |
| Median mutation tokens | 899 | 493 | -45.16% |
| Median mutation UTF-8 bytes | 2,844 | 1,567 | -44.90% |
| Strict reference use | 0/8 | 8/8 | +8 |
| Wrong-subject attempts | 0 | 0 | tied |
| Median MCP round trips | 3 | 2 | descriptive only |

The screen proves that Sol/high can use references and that request emission
falls on that fixture. It does not prove product safety or complete-wall
performance. The proxy knew the exact intended target content and rejected a
reference paired with any other `to`; the product cannot know intended new
meaning.

### Material class

The retained overlap study at `b51f802` found 86.1% direct copy overlap and
70.5% longest-common-subsequence repetition across the paired old and new
payloads. Selective removal of repeated old-source emission is economically
interesting for wall-sized replacements, not for the median write.

The walls exhibit at `5d3834e7` measured 603,327 `from`/`to` bytes and 136,387
tokens across 247 writes. The projected destination decode burden was 35.4
minutes over eight days. Its champion edit had 95% self-overlap. These are
corpus and price measurements, not observed savings from a product feature.

### Stopped alternatives that still bind this design

- The verb census at `abaf7e65` found that closed verbs cover only 16.0% of
  emitted target bytes. The 84.1% escape hatch remains the dominant content
  class. This design does not create a growing transform algebra.
- The rename screen at `47aff4d1` showed that a much smaller strict schema did
  not make Sol use it reliably. Product safety must come from validation, not
  from expecting schema fluency. Spark's 2/2 result is encouraging but does
  not weaken the contract.
- The postmortem at `a78c660` rejects opaque, mnemonic, positional, and
  structural-path labels when they select mutation subjects. A new write
  grammar is guilty until it compresses repeated structure, clears a
  whole-turn threshold on a material class, preserves canonical subject and
  effect identity, and wins complete verified wall in the correct
  discovered-or-supplied stratum.
- The prepared-request leaf at `b445a8c` proves one reusable boundary: an
  already-required exact successful read may return a non-executable object
  that repeats full subject identity, leaves only caller-owned holes, and
  later enters the ordinary public writer.

## Trust laws

The design must satisfy all of these laws together:

1. **Compress repetition, never subject identity.** Every request repeats the
   complete project-relative file and typed owner identity. A reference never
   chooses either.
2. **A read selection precedes a reference.** The server may issue a reference
   only for source that the successful read selected exactly. It may not
   enumerate arbitrary candidate spans and ask the caller to choose.
3. **One live slot per owner.** The first slice admits at most one referenced
   selection per canonical file and named owner in one prepared batch. A new
   reference for that owner invalidates the older live reference. This makes
   the twin-span same-owner class ineligible instead of heuristically ranked.
4. **All or none.** A prepared batch is submitted with every generated slot in
   the original order or without references. Partial, duplicated, omitted,
   reordered, or mixed literal/reference batches refuse before source capture.
5. **Snapshot and manifest fenced.** Server-generated references bind the
   workspace generation, canonical file, complete typed owner, owner-token
   proof, exact selection range, selected-source SHA-256, file SHA-256, batch
   order, and manifest SHA-256. A reference is not valid after any bound fact
   changes.
6. **No numeric tolerance.** The integration independently reparses the
   returned source and proves exact owner-token and selection containment.
   CRLF and metadata are handled structurally, never with offset slack.
7. **Caller owns new meaning.** The server does not predict, verify, or repair
   `to`. After the caller fills it, the reference compiler restores the exact
   old bytes and lowers to the ordinary canonical edit compiler.
8. **Receipts resolve the pointer.** Success and refusal evidence echo the
   reference label, edit index, full file and owner identity, selection hash,
   snapshot hash, manifest hash, and resolution outcome. The label alone is
   never evidence.
9. **No extra read for savings.** References piggyback only on an already-
   required successful exact read. The feature cannot create a setup turn.
10. **Fail closed, do not recover automatically.** Unknown, expired, stale,
    cross-owner, wrong-manifest, ambiguous, or unavailable references refuse
    before mutation with source unchanged. No fuzzy repair or best match is
    allowed.

## Competing HLD options

### Option A: free-standing references on read results

Every eligible exact read publishes a reference catalog. `edit_clojure`
accepts any advertised `from_ref` together with full file, owner, and `to`.

Why it might be right: this is closest to the successful experiment. It is
simple to explain, works without a prepared descriptor, and maximizes reuse of
one read across several later edits.

Cost: the catalog creates a new choice. Two valid references in the same owner
can both satisfy snapshot, cardinality, and old-source guards. Choosing the
wrong one can still produce a coherent wrong effect. Readable previews improve
legibility but do not supply independent effect authority. The experiment's
expected-target oracle cannot be generalized.

Assumption underneath: the parallel adversarial replication proves that
similar and same-owner references cannot impersonate one another without
repeating enough effect identity to erase the saving. Until that proof exists,
this option is **not recommended**.

### Option B: slot-bound references in prepared requests — recommended

An eligible successful exact read may return one non-executable
`prepared_request` whose ordinary edit rows contain full file and owner
identity, one immutable server-generated old-source reference per distinct
owner, `matches=1`, and caller-owned `to` holes. The batch manifest fixes every
non-hole field and slot order. The public writer accepts the completed object,
resolves every slot from one retained snapshot basis, reconstructs exact
ordinary `from` values, and invokes the existing compact compiler and
transaction once.

Why it might be right: the caller no longer chooses among a catalog. It fills
only new-meaning holes in a descriptor whose effect slots were already fixed
by the read. This composes with the installed prepared-request pattern and the
ordinary `edit_clojure` executor. It adds no third operation, writer, receipt,
or rollback path.

Cost: it requires a bounded retained reference basis, exact manifest
validation, expiry and consumption rules, and a new prepared edit variant.
The first slice excludes multiple selected spans in one owner, so its reach is
smaller than a free-standing catalog. A static input-schema branch is still
visible to all callers even though the descriptor is emitted selectively.

Assumption underneath: one exact read selection per distinct owner covers a
material share of the wall class, and a fixed descriptor is materially easier
to fill than re-emitting old source. The complete-wall cohort must prove this;
the synthetic reference screen alone does not.

### Option C: dedicated `splice_edit` operation

A new public tool or operation accepts references and replacement fragments
under a separate compact schema.

Why it might be right: eligibility, budgets, and errors could be isolated from
the general editor. A narrow tool description may make the new grammar more
salient.

Cost: this creates a third grammar and likely a second compiler or executor
boundary. It must duplicate admission, source capture, formatting,
verification, rollback, receipts, and routing guidance or build adapters back
to them. The rename screen warns that a smaller schema can still reduce
adoption. The catalog-surface experiment found no general performance case for
shrinking or multiplying tools.

Assumption underneath: a dedicated surface wins enough authorability or wall
time to pay for duplicated product contracts. Current evidence does not
support that assumption. This option is **not recommended**.

## Preferred LLD skeleton

If Gene ratifies Option B, the next LID phase creates the leaf
`docs/intent/splice-reference/splice-reference-design.md` with prefix
`MCP-OP-SPLICE-REF`. The LLD should contain the following sections.

### Context and scope

- This is a success-side extension of prepared requests.
- It applies only to exact source-bearing read selections that already expose
  canonical workspace, project-relative file, one named top-level owner,
  exact selection source, exact hashes, and a structural source anchor.
- The first slice supports one selected span per distinct owner and one through
  six owners in one file, subject to the existing prepared-result budget.
- It does not apply to read refusals, broad owner catalogs, continuations,
  semantic candidate lists, namespace-only identities, ambiguous CLJC
  platform selections, or results that omit exact source.

### Public shape

The existing `prepared_request` remains `executable=false` and
`write_authority=false`. Its candidate shape is:

```json
{
  "prepared_request": {
    "tool": "edit_clojure",
    "executable": false,
    "write_authority": false,
    "reference_manifest": {
      "version": 1,
      "basis_id": "server-generated",
      "manifest_sha256": "...",
      "expires_at": "bounded server time"
    },
    "arguments": {
      "workspace_root": "/canonical/workspace",
      "edits": [
        {
          "file": "src/example.clj",
          "within": {"form": "named-owner"},
          "from_ref": "~named-owner/selection",
          "to": null,
          "matches": 1
        }
      ]
    },
    "caller_holes": ["arguments.edits[0].to"]
  }
}
```

The spelling is illustrative until LLD ratification. The LLD must freeze the
closed schema and canonical bytes. The readable label is presentation; the
retained basis and exact manifest are validation evidence. Neither grants
subject authority without the repeated file and typed owner.

### Pure projection

One pure projector receives a completed exact read result and either returns a
complete candidate manifest or nil. It performs no I/O, selection, semantic
search, replacement inference, or source reread. It proves exact eligibility,
one selection per owner, owner-token identity, complete counts, result budget,
and the selective economic threshold.

The stateful integration stores only the complete proved reference records
needed for later lowering. The LLD must define bounded expiry, one-live-slot-
per-owner invalidation, single successful consumption, workspace generation,
and cleanup. Missing state always refuses; it never reconstructs a reference
from a similar current span.

### Reference lowering

One pure lowerer accepts the retained manifest, completed public arguments,
and current frozen source facts. It either returns the exact ordinary
`edit_clojure` request with literal `from` values or one typed pre-write
refusal. It validates the complete manifest before resolving any row. It may
not partially lower or mix reference and literal rows.

The integration invokes the existing public validator, compact compiler,
source capture, formatter, transaction, verifier, rollback, undo, and receipt
paths after lowering. No splice-specific writer or transaction is permitted.

### Selective offering

The projector emits nothing unless all of these conditions hold:

- the read was already required and completed successfully;
- every reference slot has one distinct full named-owner identity;
- the exact conventional request and reference request can be serialized by a
  pure oracle before emission;
- net canonical request reduction is at least 25%;
- net reduction is at least 1,250 UTF-8 bytes, the smallest threshold cleared
  by the winning screen's 1,277-byte median reduction; and
- the complete descriptor fits the existing 4,096-byte descriptor and
  32,768-byte projected-result gates.

If any condition fails, the result remains the byte-identical ordinary read
result. There is no omission cue and no request-side tax. The static schema
branch must be measured separately; a model-bearing promotion requires no
regression on eligible correctness and no measurable median penalty on an
ineligible control stratum.

The 1,250-byte threshold is a candidate ratification decision, not a claim
that every such request removes a turn. The complete-wall experiment remains
authoritative.

### Relationship to sibling leaves

Prepared request:

```text
successful exact read
  -> optional ordinary prepared_request with literal from
  -> optional wall-class prepared_request with slot-bound from_ref
  -> caller fills to holes
  -> edit_clojure validates and lowers once
```

The splice leaf reuses the prepared descriptor, hole, coaching, output-budget,
and ordinary-writer laws. It must not change the installed literal prepared
path or make literal prepared requests depend on retained state.

Write-refusal completeness:

```text
typed write refusal -> source-free write_refusal_evidence -> caller decision
```

The refusal leaf remains evidence-only. It never emits a splice reference,
prepared request, corrected mutation, selected candidate, or executable retry.
The splice leaf never decorates or consumes a refusal. The leaves share no
payload builder, budget, or authority.

### Telemetry and receipts

Product telemetry may record only eligibility, emission, manifest hash,
reference count, resolution outcome, and bounded refusal type. It must not
record source, replacement text, absolute paths, or prepared arguments.

Mutation receipts must echo every resolved reference with complete canonical
file and owner identity, exact selected-source and snapshot hashes, manifest
hash, edit index, and whether lowering reached the ordinary compiler. A
reference label without this evidence is not terminal proof.

## Candidate EARS registry

These IDs are stable candidates for the next phase. They are not active EARS
and must not receive implementation or test checkmarks before ratification.

| Candidate ID | Required behavior |
|---|---|
| `MCP-OP-SPLICE-REF-001` | Emit only for an eligible successful exact read, one through six distinct named owners, one selected span per owner, complete source/hash/anchor evidence, and the selective economic threshold. Otherwise preserve the ordinary result by identical object identity. |
| `MCP-OP-SPLICE-REF-002` | Publish one complete non-executable prepared descriptor with full file/owner identity, exact manifest evidence, only `to` holes, and no other null or caller-authoritative field. |
| `MCP-OP-SPLICE-REF-003` | Bind every server-generated reference to workspace generation, canonical file, typed owner, owner-token proof, exact selection range and hash, file snapshot, ordered batch manifest, expiry, and single-use state. |
| `MCP-OP-SPLICE-REF-004` | Permit at most one live referenced selection per canonical file/owner. A new reference invalidates an older one; same-owner multi-span results are ineligible. |
| `MCP-OP-SPLICE-REF-005` | Accept only the complete original reference batch in original order. Refuse mixed, partial, omitted, duplicated, added, reordered, cross-manifest, or literal/reference submissions before source mutation. |
| `MCP-OP-SPLICE-REF-006` | Reparse returned source and prove owner-token identity and exact selection containment without numeric tolerance, fuzzy matching, line authority, or positional caller input. |
| `MCP-OP-SPLICE-REF-007` | Lower a valid complete reference batch to exact literal `from` plus caller-owned `to`, then invoke the existing public schema, compact compiler, transaction, formatter, verifier, rollback, undo, and receipt paths once. |
| `MCP-OP-SPLICE-REF-008` | Return a typed source-unchanged pre-write refusal for unknown, expired, consumed, stale, ambiguous, wrong-owner, wrong-manifest, missing-state, or cardinality-changing references. Never repair, rebind, or auto-select. |
| `MCP-OP-SPLICE-REF-009` | Echo complete resolved identity and lowering evidence in success and refusal receipts; never treat the short label as sufficient evidence. |
| `MCP-OP-SPLICE-REF-010` | Preserve existing prepared requests, write refusals, generic edits, programs, extraction, retained-basis operations, CLI behavior, and unsupported CLJC paths without invoking reference projection or lowering. |
| `MCP-OP-SPLICE-REF-011` | Enforce the descriptor/result budget and selective threshold with a pure exact serializer. Overflow or sub-threshold candidates produce the unchanged ordinary read result and no omission cue. |
| `MCP-OP-SPLICE-REF-012` | Before promotion, pass the twin-span hostile matrix and a counterbalanced eligible/ineligible product cohort with exact correctness, zero wrong-subject attempts, exact route adherence, the preregistered emission threshold, and lower complete verified wall in both order blocks and pooled. |

## Falsifier table

Any false green is a design contradiction, not a test gap to waive.

| Falsifier | Required result | Why it matters |
|---|---|---|
| Two similar spans in the same owner receive two simultaneously valid references | Descriptor omitted or second issuance invalidates the first before either can mutate | Prevents twin-span impersonation rather than ranking it |
| Wrong valid reference from another owner is paired with the intended `to` | Typed pre-write wrong-owner/manifest refusal; all files byte-identical | Full subject identity must remain authoritative |
| Two `to` holes or references are swapped | Manifest/slot refusal before lowering | Exact final bytes cannot erase a wrong-effect attempt |
| One reference is omitted, duplicated, added, or reordered | Complete-batch refusal | All-or-none is an authority law, not presentation advice |
| Literal `from` and `from_ref` are mixed | Source-blind admission refusal | Prevents partial escape from manifest authority |
| Same old bytes appear twice in one owner | No descriptor or exact ambiguity refusal | Content/cardinality cannot choose intent |
| Similar labels differ by one character | Unknown or wrong-manifest refusal; never nearest-label recovery | Readability does not authorize fuzzy selection |
| Reference comes from an earlier snapshot, reconnect, expired basis, or consumed batch | Typed stale/missing-basis refusal | A label cannot outlive its proof |
| File changes outside, inside, before, or after the selected span | Exact frozen-snapshot refusal under the declared scope | No positional replay against drift |
| Metadata, comments, CRLF, Unicode, regex literals, or reader conditionals alter apparent ranges | Structural exact proof or omission; no numeric tolerance | PREP-BLOB-2 identity law must compose |
| Duplicate named owners, defmethods, namespace forms, or platform-specific CLJC selections are present | Omit unless full typed identity is uniquely supported by a separately ratified slice | A string owner name is not universal identity |
| Caller supplies a fabricated label, range, hash, expiry, or manifest | Missing-state or manifest refusal | Caller data cannot mint a reference |
| Conventional request saves less than 25% or 1,250 bytes | No descriptor; ordinary result identical | Median writes pay no request ceremony |
| Descriptor or projected result exceeds its bound | No partial descriptor and no truncation | Identity and guards are never shortened for budget |
| Reference lowering produces bytes different from conventional literal lowering | Refusal; no write | The optimization may not alter canonical effects |
| Model cohort is exact but uses a fixture-only target oracle | No product claim | The generic server does not know intended new meaning |
| Eligible treatment is smaller but slower or causes more refusals | Stop; no promotion | Bytes are mechanism evidence, complete verified wall is outcome |

## Verification and promotion skeleton

After HLD and LLD ratification, the red-first phase must include:

1. a pure eligibility matrix, including the exact threshold boundaries and
   every ineligible ordinary-result identity case;
2. exact manifest canonicalization, expiry, invalidation, consumption, and
   complete-batch validation;
3. twin-span, similar-label, duplicate-content, cross-owner, cross-snapshot,
   reconnect, reordered, mixed, partial, and fabricated-reference witnesses;
4. LF, CRLF, comments, metadata, Unicode, regex, reader-conditional, CLJC,
   duplicate-owner, and defmethod owner-token witnesses;
5. reference-to-literal lowering parity through the existing public compiler;
6. exact future bytes, formatter, verifier, rollback, undo, and resolved
   receipt evidence on a copied real-program wall fixture;
7. zero invocation from write refusals, generic literal edits, programs,
   extraction, CLI, and unrelated prepared requests; and
8. one no-model integrated canary proving an emitted prepared reference can be
   filled, lowered, committed, verified, undone, and then refused when stale.

The model-bearing gate uses a real historical supplied-decision wall fixture.
It compares ordinary prepared literal `from` rows with prepared slot-bound
references on the same product surface. It counterbalances both directions,
keeps every launched attempt, and reports correctness, route adherence,
wrong-subject, request tokens/bytes, construction refusals, tool calls,
`T_emit`, `T_apply_verified`, and complete verified wall separately.

Promotion requires:

- every attempt exact, verified, route-adherent, and first-call correct;
- zero wrong-subject or reference-repair attempts;
- treatment reference use in every assigned treatment attempt;
- at least 25% lower median mutation emission in each block and pooled;
- lower complete verified wall in each block and at least 20% pooled; and
- no regression in a separately retained ineligible control stratum.

An emission win cannot rescue a safety, correctness, route, or wall miss.

## Open gate: adversarial replication

The requested branch `experiment/splice-adversarial-replication-20260830` was
not present in the local or remote branch inventory when this packet was
prepared. Therefore this packet does not claim its outcome.

Ratification may choose the HLD option now, but the LID arrow must not enter
EARS until the replication receipt is incorporated. Any valid wrong-span
mutation, any same-owner simultaneous reference pair, or any success that
depends on the fixture's expected-target oracle rejects Option B as written.
The smallest allowed repair is narrower eligibility. It is not a guard,
heuristic, or tolerance that lets the caller choose among valid spans.

## Recommendation and fallback

Ratify **Option B** for LLD completion only. It preserves the useful mechanism
while moving authority from a caller-chosen reference catalog into a fixed,
non-executable prepared descriptor. It reuses the ordinary compact editor and
transaction after one exact lowering step. Its first slice is intentionally
narrow: already-required exact reads, wall-sized old source, distinct named
owners, one live span per owner, and all-or-none submission.

Fallback: if the adversarial replication or the pure corpus projection shows
that this restriction cannot cover a material wall class, stop. Keep
`588893fc` as evidence that references reduce emission in a synthetic task, but
do not add a public label, schema branch, retained basis, or executor. Do not
rescue the feature with a dedicated tool, numeric coordinate, fuzzy match,
content guard, or fixture-specific target oracle.

## References

- `588893fc` — splice-reference synthetic screen and immutable evidence
- `b51f802` — retained `from`/`to` overlap and selective economics
- `5d3834e7` — walls-of-text exhibit and decode-price distribution
- `abaf7e65` — transform-verb expressibility census
- `47aff4d1` — rename-verb adoption screen
- `a78c660` — AceJump postmortem and four-part promotion law
- `b445a8c` — installed prepared-request LLD, EARS, and owner-token proof
- `ff15b954` — ratified write-refusal sibling boundary and frozen red checkpoint
