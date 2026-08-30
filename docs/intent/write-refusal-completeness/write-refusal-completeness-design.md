---
parent: mcp-operation-contract-design
prefix: MCP-OP-WRITE-REFUSAL
status: pre-ratification
---

# Write-Side Refusal Completeness

Status: pre-ratification leaf LLD. Gene ratified entry into the LID design
phase on 2026-08-30 with the verbatim decision `1 go, 2 go, 3 go`. This leaf,
its requirements, tests, and implementation are not ratified.

## Context

The write-side refusal audit at
`audit/write-refusal-completeness-20260830@e418c851` found 13 constructor sites
that discard finite recovery evidence already available when they refuse. Nine
sites return a truncated projection and four return no candidate vocabulary.

The highest-priority site is the generic `expect-count-mismatch` refusal in
`compile-intent-edits`. It fired seven times in the retained window. Four of
those refusals were followed by an `inspect_clojure` recovery read: 4/7, or
57.1%. The compiler had already computed `per-form-counts` and resolved match
records before it refused.

The mechanism is not inferred from those seven events alone. Sweep lane 1 at
`docs/sweep-lane1-complete-refusal-ab@c1e89d5d` held the other conditions fixed
and measured 0/10 recovery reads with complete owner vocabulary versus 10/10
with truncated vocabulary. The installed read-side contract at
`stable-read-request-normalization-20260830@c55de227` supplies the house
discipline: return mechanically known bounded evidence, state truncation, keep
hypotheses separate from authority, and never turn partial evidence into write
authority.

## Boundary

This leaf applies only to the 13 audited refusal sites reachable from
`edit_clojure`, `apply_clojure_changes`, and `transform_clojure`:

| Priority | Requirement | Audited refusal family | Sites |
|---:|---|---|---:|
| 1 | `MCP-OP-WRITE-REFUSAL-001` | Generic scoped `expect-count-mismatch` | 1 |
| 2 | `MCP-OP-WRITE-REFUSAL-002` | Named, defmethod, and namespace owner mismatch | 2 |
| 3 | `MCP-OP-WRITE-REFUSAL-003` | Compact-location proof failure | 1 |
| 4 | `MCP-OP-WRITE-REFUSAL-004` | Transform selection/count and comment-losslessness gates | 4 |
| 5 | `MCP-OP-WRITE-REFUSAL-005` | Extraction missing-form selection | 1 |
| 6 | `MCP-OP-WRITE-REFUSAL-006` | Retained-basis compact subform mismatch | 1 |
| 7 | `MCP-OP-WRITE-REFUSAL-007` | Binding identity and comment-losslessness gates | 2 |
| 8 | `MCP-OP-WRITE-REFUSAL-008` | Verification-profile admission | 1 |
|  |  | **Total** | **13** |

The order follows the audit: observed firings first, then deficient class.
Items 002 through 008 all had zero retained-window firings and are tied
empirically. Their displayed order follows route breadth, not measured demand.
Implementation must start with 001 and may not use an unobserved family to
delay it.

This leaf adds refusal evidence only. It does not change request admission,
selection, compilation, formatting, verification, commit, rollback, undo,
success results, CLI behavior, or the meaning of an existing refusal. Complete
sites in the audit remain outside the leaf. Undo-only and prepare-change-only
paths remain outside unless one of the three named public write entrances
reaches an audited site.

## Completeness law

At each in-scope refusal, clj-surgeon must project the finite candidate facts
that the refusing stage already computed. It must not reread source, call a
semantic provider, rerun a selector, recapture a newer snapshot, or infer a
candidate to make the payload appear complete.

A candidate inventory is published under `write_refusal_evidence`. It uses
deterministic family-specific rows and these common fields:

- `version=1`;
- `family`, one stable family name defined below;
- `available_count`: the exact number known to the refusing stage;
- `returned_count`: the number of candidate rows in the payload;
- `omitted_count`: `available_count - returned_count`;
- `truncated`: whether `omitted_count` is nonzero;
- `items`: the returned candidate rows;
- `authority=false` and `write_authority=false`; and
- `snapshot_guards`, only when the refusing stage captured source and the
  candidate evidence depends on that frozen snapshot.

`available_count` must describe the same universe as the candidate rows. A
producer that knows only a bounded prefix must not label the prefix as the
complete source universe. It reports the complete count when known, the exact
returned and omitted counts, and the producer's truncation state.

Candidate rows contain source-free identity and location facts only. Depending
on the family, these facts can include the project-relative file, owner name
and kind, namespace or clause kind, binding name, retained site ID, exact
same-name status, line range, structural address, or violated profile rule.
The leaf does not add source bodies, replacement text, commands, secrets,
absolute paths, receipt paths, or verifier output to candidate rows.

## Payload bound and oversize inventories

This leaf proposes a write-side result bound because the active write
entrances do not yet have one. The complete public MCP result must be at most
32,768 UTF-8 bytes. Measurement serializes this complete JSON object with the
same generator used by the active read-side meter:

```text
{content: [{type: "text", text: SUMMARY}],
 structuredContent: RESULT,
 isError: true}
```

The byte count includes the concise text summary and structured result. A
refusal can return at most 128 candidate rows even when more rows would fit.
Candidate rows use the smallest source-free identity that preserves the
caller's next decision. Ratification would approve these proposed bounds; it
would not claim that an active write-side bound already exists.

If the complete finite inventory fits, the refusal returns every candidate and
sets `returned_count=available_count`, `omitted_count=0`, and
`truncated=false`. Deterministic order is family-specific. When presentation
inherits caller order, the selector digest includes the exact ordered vector
and does not coerce it to a set. Remaining ties use project-relative file,
owner kind and name, source range, and structural address as applicable.

Domain projection reserves 128 bytes for the finalizer's `elapsed_ms` field and
its rendered summary text. If more than 128 rows exist or the complete
inventory does not fit, the refusal returns the largest deterministic prefix
of at most 128 rows whose pre-finalization result and summary use at most
32,640 bytes. It also returns exact counts, `truncated=true`, the applicable
frozen snapshot guards, and a bounded `candidate_continuation` descriptor when
that complete descriptor fits within the reserved domain budget.

The continuation descriptor has this closed shape:

| Field | Meaning |
|---|---|
| `version` | Integer `1`. |
| `executable` | Boolean `false`. |
| `authority` | Boolean `false`. |
| `write_authority` | Boolean `false`. |
| `operation` | Original public write entrance. |
| `refusal_type` | Original stable refusal type. |
| `family` | Stable evidence family. |
| `subject` | Closed family-specific request identity plus mandatory `selector_sha256` from the projection table below. |
| `candidate_query_sha256` | SHA-256 of canonical `[version, operation, refusal_type, family, subject, ordering_version, snapshot_guards]`. |
| `ordering_version` | Integer `1`. |
| `snapshot_guards` | Complete frozen project-relative file-hash map for the candidate universe, or an empty map when the refusal precedes source capture. |
| `next_offset` | Equal to `returned_count`. |
| `page_limit` | Integer `128`. |
| `remaining_count` | Equal to `omitted_count`. |

`candidate_query_sha256` hashes the UTF-8 bytes of canonical EDN for the stated
vector. Canonical EDN uses keyword keys, recursively sorted maps, vectors for
ordered collections, and `pr-str`. The projector renders each prefix from zero
through `min(128, available_count)` as a pre-finalization result and summary
and selects the largest prefix whose measured bytes do not exceed 32,640. It
does not estimate row bytes.

Each `selector_sha256` hashes the canonical source-free projection of all
request or configuration fields that determine candidate generation for that
family. It excludes replacement text, commands, secrets, and other fields that
cannot change the candidate universe. Every caller-ordered collection retains
its exact vector order in this digest. A future page request must resupply an
explicit read selector whose canonical digest is identical.

The descriptor names one exact frozen candidate query. It is evidence for a
separately ratified read-only page operation. It is not a request and contains
no tool arguments, candidate selection, program body, replacement, commit
flag, verifier, `next_call`, or executable retry. This leaf does not add the
page operation, a private cursor executor, or retained server state.

If the zero-row pre-finalization result and summary would still exceed 32,640
bytes, clj-surgeon
replaces the family payload with a fixed fail-empty domain projection. That
domain projection contains only `version=1`, the stable `operation`, original
`error_type`, the family's stable `failed_stage`, `ok=false`,
`source_unchanged=true`,
`mutation_attempted=false`, `write_authority=false`, numeric `available_count`,
`returned_count=0`, `omitted_count=available_count`, `truncated=true`,
`write_refusal_evidence_omitted=output-budget`, and
`limits={public_result_bytes:32768,candidate_rows:128}`. Its concise summary is
the constant `Write refusal evidence exceeds the public MCP output budget.`
It omits dynamic error text, paths, IDs, names, per-file and per-form maps,
candidate rows, guards, descriptors, commands, and source. The active MCP
finalizer then adds its mandatory finite, non-negative `elapsed_ms` and common
operation envelope. The 32,768-byte meter measures that finalized result and
its text summary, not the domain map alone. The original stable refusal type
and the family stage remain visible, but no unbounded dynamic value survives.
The later test design must prove the complete finalized fixed projection is
below the byte bound before any requirement activates.

The 128-byte timing reserve is part of the proposed contract. It must cover the
serialized `elapsed_ms` key and every elapsed-time character added to the
summary for the full finite non-negative duration range produced from the
request clock's two signed 64-bit nanosecond readings. The post-finalization
32,768-byte measurement is an invariant check only. It does not truncate,
rewrite, or otherwise change domain fields after timing is added. A red witness
must prove the reserve across the clock extrema before activation.

The continuation descriptor retains no write authority and no permission to
reuse the refused mutation. A later read must start from an explicit
caller-chosen read request and must validate every frozen guard. This leaf does
not add a candidate-page operation or a private cursor executor. If the
existing read surface cannot express the required guarded page, implementation
must stop at the LID boundary and return for a separate read-surface design;
it must not silently truncate, add server state, or make the write refusal
executable.

This rule deliberately distinguishes bounded presentation from completeness.
An in-budget payload is complete in one refusal. An oversize payload is honest
about omitted rows and, when the closed descriptor fits, names the exact frozen
candidate query for a separately authorized read design. Ratification of this
leaf does not authorize that separate design.

## No-write-authority invariant

Every in-scope refusal remains a refusal. Additional evidence cannot change
`ok=false`, make the transaction successful, or weaken any existing
source-unchanged, mutation-attempted, verification, rollback, or recovery
fact.

No in-scope refusal may publish an executable `next_call`, an executable retry
template, a prepared edit request, a corrected mutation, a replacement value,
a selected candidate, a widened file or owner set, a relaxed guard, or a
verification command. Candidate order, uniqueness, exact same-name status,
line position, source proximity, and a single remaining candidate do not grant
selection or write authority.

This is the prepared-request adversary boundary applied to failure results. A
server may publish old-source identity and finite recovery evidence that it
mechanically proved. It does not know the caller's intended replacement or
which alternative subject the caller intends. A later caller-authored request
therefore enters ordinary public validation with zero inherited retry or write
authority.

## Family projections

Each family uses the common envelope above and the following closed
projection. A field marked conditional is omitted when its stated condition is
false. It is never encoded as `null` or zero to imply missing evidence.

| ID / `family` | Subject identity | Required family fields | Required item fields | Order | Guards |
|---|---|---|---|---|---|
| 001 / `generic-count-mismatch` | `change_index`, optional caller `change_id`, `selector_sha256` over the exact ordered file vector, scope, matcher, and match expectation | `expected_count`, `actual_count`, complete `per_file_counts`; `per_form_counts` only for a form-scoped change | `file`, `scope_kind`; conditional `owner_kind`, `owner_name`; `line`, `end_line`, `address` | ordered file vector, then scope kind, owner kind/name, line, address | Required |
| 002 / `owner-resolution` | `change_index`, optional `change_id`, `resolution_kind`, `requested_owner`, `selector_sha256` over the exact ordered file vector, kind, and requested owner | `resolution_kind` in `named`, `defmethod`, `namespace` | `file`, `owner_kind`, `owner_name`, `line`, `end_line`, `same_name` | ordered file vector, then owner kind/name and line | Required |
| 003 / `compact-location` | `change_index`, optional `change_id`, `selector_sha256` over the compact edit fields that determine location proof | `relations`, one diagnostic per applicable relation with `name`, `failed_predicates`, and `observed_counts` | flattened candidate rows with `relation`, `file`, and conditional kind, name, clause kind, line, and address | relation order below, then file, kind/name, line, address | Required |
| 004 / `transform-selection-losslessness` | optional `program_index`, optional `change_id`, `selector_sha256` equal to the completed query SHA-256 | `gate` in `selection-too-large`, `expected-count-mismatch`, `comment-sensitive` | `file`, conditional owner kind/name, `line`, `end_line`, `address`, `comment_bearing` | program index, file, owner, line, address | Required |
| 005 / `extraction-owner-selection` | `source_file`, ordered `requested_names`, `selector_sha256` over both | ordered `missing_names` | `name`, `owner_type`, `line`, `end_line` | direct source order | Required |
| 006 / `retained-basis-subform` | `basis_id`, `site_id`, `selector_sha256` over the retained site's exact subform selector | `match_count` | `file`, `owner`, `line`, `end_line`, `address` | `find-subforms` result order | Required |
| 007 / `binding-rename` | optional `change_index`, `file`, `owner`, `binding`, `selector_sha256` over exact binding-selection fields | `gate` in `binding-identity-ambiguous`, `comment-sensitive-binding` | `candidate_kind` in `binder`, `comment-bearing`; `line`, `column`, `end_line`, `end_column`; conditional `binding_path` | source position, candidate kind | Required |
| 008 / `verification-profile` | `requested_profile`, `selector_sha256` over requested profile and the ordered source-free candidate universe | `gate` in `unknown-profile`, `invalid-exact-profile`; `configuration_universe_sha256` over ordered profile names or violation IDs | `profile_name` for unknown profile, or one `violation_id` from the closed list below | lexical profile name or violation-list order | Omitted |

The stable family stages are:

| Family | `failed_stage` |
|---|---|
| `generic-count-mismatch` | `intent-compilation` |
| `owner-resolution` | `owner-resolution` |
| `compact-location` | `compact-location` |
| `transform-selection-losslessness` | `transform-program` |
| `extraction-owner-selection` | `extraction-planning` |
| `retained-basis-subform` | `retained-basis-apply` |
| `binding-rename` | `binding-analysis` |
| `verification-profile` | `verification-profile-admission` |

For 001, `scope_kind` is one of `form`, `namespace`, or `root`. Form-scoped
rows require `owner_kind` and `owner_name`. Namespace-scoped rows require the
namespace owner identity. Root-scoped rows omit owner fields. This prevents an
unscoped match from acquiring a fictitious owner. `per_form_counts` is absent
unless the refused change supplied form scope.

For 003, `compact-location-unresolved` occurs only when location is omitted and
both candidate relations fail. The closed relation order is therefore
`namespace-clause`, then `complete-named-owner`. Each relation reports every
failed predicate from this closed vocabulary:

- `namespace-count-one`;
- `single-file`;
- `complete-namespace-clause-pair`;
- `same-clause-kind`;
- `declared-count-exact`;
- `direct-namespace-child-count-exact`;
- `namespace-descendant-count-exact`;
- `whole-file-count-exact`;
- `complete-named-owner-pair`;
- `same-owner-kind-and-name`;
- `direct-owner-count-one`;
- `lossless-fingerprint-exact`; and
- `platform-unambiguous`.

The diagnostic does not claim that exactly one relation or predicate failed.
It does not run an inapplicable relation merely to populate the payload.

For 008, `unknown-verification-profile` returns profile-name rows and no rule
violations. An invalid exact profile reports violations in this stable order:

1. `profile-absent`;
2. `definition-not-map`;
3. `definition-fields-not-exact`;
4. `acceptance-not-exact-exit`;
5. `timeout-missing-or-out-of-range`;
6. `command-count-not-one`;
7. `command-not-nonempty-string-vector`;
8. `files-placeholder-present`; and
9. `hot-or-cold-profile-present`.

If the definition is absent, only `profile-absent` applies. If the definition
exists but is not a map, only `definition-not-map` applies. If the definition
is a map, clj-surgeon evaluates every remaining rule and returns all failures
in the listed order. The distinct `exact-profile-not-project-owned` refusal is
outside the audited 13-site payload law and remains unchanged.

These IDs project the admission rules owned by `MCP-OP-VERIFY-001..002`. The
payload reports rule identity only. It does not return the rejected command,
arguments, timeout value, environment, secret, or a fallback profile.

## Behavior matrix

| Condition | Required outcome |
|---|---|
| Complete finite inventory has at most 128 rows and the complete result is at most 32,768 bytes | Return all candidate rows and exact zero-omission facts. |
| Producer has only a bounded prefix plus a total | Return at most 128 known rows, exact returned and omitted counts, truncation state, and applicable frozen guards. |
| Complete inventory exceeds either bound | Reserve 128 timing bytes and return the largest prefix whose pre-finalization result and summary fit within 32,640 bytes, plus exact counts, applicable guards, and the closed inert descriptor. |
| Zero-row pre-finalization result and summary exceed 32,640 bytes | Return the fixed fail-empty domain projection with the family stage and no unbounded dynamic value; finalize it with mandatory `elapsed_ms` before measuring the complete MCP result. |
| Candidate generation would require a reread, provider call, or newer snapshot | Omit invented evidence and retain the original refusal. |
| One candidate remains | Return it with `authority=false`; do not select it or retry. |
| Refusal occurs before source capture | Return contract or configured vocabulary facts only; do not invent snapshot guards. |
| Refusal occurs after effects begin | Preserve the existing rollback or recovery envelope; candidate evidence cannot claim source unchanged. |
| Candidate contains source, command, secret, or absolute path | Omit that field while retaining source-free identity and exact counts. |

## Decisions and alternatives

| Decision | Selected approach | Alternatives rejected | Rationale |
|---|---|---|---|
| Scope | The 13 audited deficient sites only. | Rewrite every write refusal; change complete sites opportunistically. | The audit supplies exact source ownership and a closed coverage proof. |
| Priority | Implement 001 first; retain the audit order for the remaining tied families. | Start with an easier zero-firing site; rank tied sites as measured demand. | 001 alone has observed firings and recovery-read tax. |
| Result bounds | Limit the complete JSON MCP result to 32,768 UTF-8 bytes and candidate rows to 128. | Assume an existing write bound; use source-character count; allow unbounded rows. | This creates an exact testable write contract and matches the active read-side byte measurement. |
| Candidate completeness | Return the complete finite inventory when it fits. Otherwise reserve timing bytes and use the largest prefix admitted by the 32,640-byte pre-finalization budget, exact counts, applicable frozen guards, and the closed inert descriptor. | Unbounded payloads; silent truncation; unconditional fail-empty overflow. | This preserves the result bound without disguising omission. |
| Continuation identity | Bind version, entrance, refusal, family, closed subject, query SHA-256, ordering version, guards, offset, and bounds. | Family plus offset only; executable write retry; automatic paging; retained mutable server state. | The descriptor denotes one frozen candidate query without becoming a request. |
| Continuation authority | Require a separately ratified read-only page operation before omitted rows can be fetched. | Treat the descriptor as a tool call or private cursor executor. | Refusal completeness cannot implicitly expand the read language. |
| Candidate representation | Use family-specific source-free rows plus common count, truncation, guard, and authority facts. | One lossy generic row; raw source bodies. | Each family retains the discriminating facts it already computed without expanding payloads or leaking source. |
| Ordering | Use deterministic, authority-free order. | Rank becomes selection; preserve incidental map iteration. | Stable presentation supports tests and comparison but cannot imply intent. |
| Existing envelopes | Preserve each refusal's current error type, mutation, verification, rollback, and recovery semantics; add one stable family stage. | Pretend every constructor already has a stage; normalize all refusals into one new outcome. | Evidence completeness must not erase stronger existing safety facts. |
| Retry policy | Publish no executable retry, correction, prepared request, or `next_call`. | Correct one apparent typo automatically; emit a ready-to-submit guarded mutation. | A refusal proves old facts, not intended replacement or alternative subject. |
| Read-side consistency | Reuse exact counts, deterministic bounded vocabularies, truncation disclosure, snapshot guards, and `authority=false`. | Invent write-only meanings for the same fields. | Identical evidence concepts should carry identical authority across read and write results. |

## Non-goals

- No product code, tests, `@spec` annotations, install, MCP reload, or cohort.
- No new executable continuation or candidate-page operation.
- No automatic candidate selection, fuzzy correction, or replacement inference.
- No source-body, command, secret, verifier-output, or absolute-path expansion.
- No change to success payloads, commit, rollback, undo, or verification policy.
- No claim that the 57.1% retained-window rate is a causal estimate or a
  prediction for the other seven requirement families.

## Deferred verification design

After separate explicit authorization to activate red witnesses, the test
phase must activate one requirement family at a time, beginning with
`MCP-OP-WRITE-REFUSAL-001`. Design and EARS ratification alone leaves every
requirement `[D]`. Permanent witnesses must prove exact candidate
coverage, deterministic ordering, honest truncation counts, public-envelope
bounds, zero executable retry fields, zero inherited write authority, and
unchanged source on every pre-write refusal. Tests for any post-effect path
must assert its existing rollback or recovery facts instead of assuming
`source_unchanged=true`.

The 001 field-failure witness must reproduce the generic count mismatch with
already-computed per-form and match records and prove that the refusal returns
them without another parse, source read, or semantic call. Later families must
use the same proportional pure, boundary, and real-program evidence required
by the repository testing guide.

## Ratification boundary

Ratifying this leaf and its deferred registry would authorize the later test
design phase only. Every requirement would remain `[D]`. A separate explicit
decision is required to activate red witnesses. Another decision is required
before product code, installation, or MCP reload. Stop here for Gene's design
and EARS decision.
