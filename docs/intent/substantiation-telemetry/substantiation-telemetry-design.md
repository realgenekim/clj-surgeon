---
parent: high-level-design
prefix: MCP-OP-SUBST
status: 'ratified in advance (Gene, 2026-08-30, verbatim: "Go on all!!!")'
client_metadata_privacy: 'decided A (conn, 2026-08-30, under Gene window authority; subject to Gene override at review)'
---

# Substantiation telemetry

## Outcome

Clj-surgeon shall record a compact, privacy-safe event ledger that can prove
feature use and recovery-chain shape without retaining prose, source, paths,
requests, replacement values, commands, URLs, credentials, or hidden
reasoning. One paved report shall turn those events into measured counts,
explicitly noncausal before/after comparisons, and visibly labeled
projections. The report cannot call a projection measured.

This leaf observes product behavior. It never chooses a tool, subject,
replacement, retry, verifier, or promotion verdict.

## Why the existing telemetry is insufficient

`mcp_telemetry` records one `tool.call` event. Metrics mode keeps only coarse
cardinalities. Full mode stores the local request and response, but the
retained study artifacts did not preserve an event-level caller join or enough
deidentified result semantics to classify the first recovery read. The frozen
consumption-gap classifier therefore covered 0 of 119 episodes.

Adding more content to full telemetry is the wrong repair. It would preserve
private values and still leave claims logic distributed across studies. The
repair is a separate ledger whose only vocabulary is identity-preserving
tokens, enums, booleans, counts, field presence, timing, and digest evidence.

## Competing options

### A. Expand current full telemetry — rejected

This is mechanically small because requests and responses already reach
`record-call!`. It is wrong because the implementation would retain more raw
content, make privacy depend on which telemetry mode happened to be active,
and require each analysis to invent its own redaction. It also cannot detect a
missing call-completion record without a start record.

Assumption underneath: local-only storage makes raw content acceptable. The
standing privacy contract rejects that assumption.

### B. Separate event-level substantiation ledger — selected

Each public MCP call gets one start record and one finish record. Values that
must compare across calls are replaced with session-local HMAC-SHA-256 tokens.
Every record is hash-chained and sequence-numbered. The report consumes only
complete pairs through an explicit marker. A missing finish is an observable
gap, not a silently absent call.

Cost: one compact append before execution, one after execution, pure shape
projection, a session-local secret, and one report compiler. This adds no
public field and changes no healthy request or result.

Assumption underneath: within-session equality, counts, and result semantic
kinds are enough to measure adoption and classify recovery without content.
The frozen classifier is the direct falsifier.

### C. Recover joins offline from agent transcripts — rejected

This avoids product code but repeats the failed study boundary. Transcripts,
MCP telemetry, and caller metadata do not share a durable event key. Rejoining
afterward either preserves prohibited content or guesses chronology. The
frozen 0/119 coverage is executable counter-evidence.

Assumption underneath: timestamps and tool names are a sufficient join key.
They are not.

## Architecture

```text
MCP exchange + public params
          |
          v
   call.start projector -----> append-only substantiation JSONL
          |                         sequence + previous hash
          v
   unchanged tool execution
          |
          v
   call.finish projector ----> append-only substantiation JSONL
                                     |
                                     v
                         marker-bounded pure report fold
                                     |
                  +------------------+------------------+
                  |                                     |
        classifier episodes.json              weekly report.json/md
        + episodes.sha256                      + report receipt
```

### Owning seams

| Path | Responsibility |
|---|---|
| `src/clj_surgeon/mcp_substantiation.clj` | Closed event projection, HMAC tokens, descriptor fingerprinting, append-chain compiler, state health, and append shell. |
| `src/clj_surgeon/mcp_operation.clj` | Optional pre-execution and post-result observation hooks around the existing single public finalizer. It owns no telemetry vocabulary. |
| Public tool handlers | Pass exchange, public tool identity, params, and finalized structured result to the observer. They do not construct events. |
| `src/clj_surgeon/mcp_http_server.clj` | Start/stop the separate ledger state and expose unhealthy ledger state through server health. |
| `bench/substantiation_report.clj` | Pure marker-bounded fold and claims compiler. |
| `bench/substantiation_report_io.clj` | Read named ledger/marker/registry/baseline bytes and write returned artifacts once. |
| `bench/fixtures/substantiation_telemetry/feature_registry.edn` | Stable feature IDs, allowed stages, and claims policy. Future elaborator receipts register here without a ledger schema change. |
| `bench/fixtures/substantiation_telemetry/baseline.edn` | Exact pre-install marker and historical aggregate authority. |
| `Makefile` | One `substantiation-report` target and a zero-model self-test target. |

The existing request compiler, mutation transaction, writer, verifier,
rollback, prepared-request projector, refusal projector, and public schemas
remain unchanged.

## Ledger envelope

Each line is canonical JSON with these closed fields:

```clojure
{:schema "clj-surgeon.substantiation-event.v1"
 :sequence 42
 :event_id "uuid"
 :observed_at "RFC3339"
 :phase "start"                 ; start | finish
 :call_id "uuid"
 :previous_event_sha256 "64hex" ; nil only on sequence 1
 :event_sha256 "64hex"
 :transport
 {:session_token "hmac"
  :turn_token nil
  :client_name "session-hmac"
  :client_version "session-hmac"
  :caller_model "unknown"
  :caller_model_source "not-exposed"}
 :tool "inspect_clojure"
 :operation "forms"
 :request_shape {...}           ; start only
 :result_shape {...}            ; finish only
 :features [{:feature_id "prepared-request"
             :stage "emitted"
             :counts {:descriptors 1}
             :dimensions {:eligible true}}]}
```

`event_sha256` covers canonical UTF-8 JSON of the record without
`event_sha256`. `previous_event_sha256` must equal the prior accepted record.
Sequence is strict and starts at one. Start and finish share one server-owned
`call_id`. A finish without its start, two finishes, a reused call ID, a chain
break, or a sequence gap makes the report input invalid.

The ledger is append-only. No product path edits, truncates, compacts, rotates,
or deletes an active file. Retention can delete only a separately sealed
segment that is older than policy and is not named by an open marker.

## Privacy and equality

The ledger stores no source, prose, path, owner name, namespace name, matcher,
replacement, command, URL, receipt, raw request, raw response, account, or
reasoning. It stores:

- closed operation and result-shape enums;
- booleans and cardinalities;
- UTF-8 byte and character counts;
- field-presence sets from an allowlist;
- per-session HMAC-SHA-256 tokens for file, owner, locator, descriptor, and
  request subjects; and
- elapsed milliseconds already owned by the public operation clock.

The session secret is random, held in memory, and never written. Its public
key ID is SHA-256 of the secret and identifies the equality domain without
revealing the secret. Tokens compare only inside that domain. A plain SHA of a
file or owner name is forbidden because a small repository vocabulary is easy
to enumerate.

Transport session identity is HMAC-tokenized. MCP 0.17.2 exposes client name,
client version, and session ID, but initialize metadata is client-controlled.
The ledger therefore records session-local HMAC tokens for all three values;
raw client name or version text is forbidden. It records caller model as
`unknown` and source as `not-exposed` unless a future transport supplies an
authenticated model field. Caller-provided tool arguments may never assert
model identity. This is SUBST-CLIENT-1 Option A, decided by the conn on
2026-08-30 under Gene's window authority and subject to Gene override at
review.

## Call/result facts

### Inspect calls

The request shape records request count, operation per subrequest, whether the
operation or ID was omitted, server-generated-ID count, mixed-ID refusal, file
tokens, owner/locator tokens, selector cardinality, and expectation presence.

The result shape records success/refusal, refusal type, read completeness,
result count, operation, field-presence enums, semantic kinds, owner and
locator token rows, duplicate multiplicity, source-body presence and character
count, dependency/hash presence, location-cap state, prepared-request
eligibility/emission, and normalized descriptor fingerprint. It never stores
the body or public content hash.

### Write and transform calls

The request shape records tool, public operation, subject file/owner/locator
tokens, edit/change counts, guard presence, prepared-template fingerprint when
the incoming shape exactly matches a prior descriptor skeleton, and no text.

The result shape records success/refusal, committed/verified/source-unchanged,
refusal type, refusal family, available/returned/omitted row counts,
truncation, continuation presence and inertness, and ordinary count fields.
It never stores candidate row values, source, replacements, receipts, hashes,
or errors.

## Feature counters

Feature observations use the common envelope
`feature_id`, `stage`, integer `counts`, and closed scalar `dimensions`. The
event schema does not change when a future feature, including the elaborator,
is registered. An unknown feature ID remains valid evidence but is excluded
from public claims until the feature registry names its stages and policy.

The first registry contains:

- `read-normalization`: `operation-omitted`, `ids-omitted`, `ids-generated`,
  `mixed-ids-refused`;
- `prepared-request`: `emitted`, `consumed`, `committed`, `refused`;
- `complete-refusal`: `fired`, `same-file-reread`, `direct-corrected-retry`,
  `other-next-action`, `abandoned`;
- `write-refusal-001`: `fired`, `rows-returned`, `continuation-returned`,
  `continuation-consumed`; and
- the reserved `elaborator.*` namespace using the same common envelope.

### Prepared-request matching

When a prepared descriptor is emitted, telemetry canonicalizes its public
arguments after replacing every caller hole with a fixed sentinel. It HMACs
that skeleton and records only the token and hole count. A later
`edit_clojure` request is `consumed` only when replacing its allowed hole
values with the same sentinel yields the exact token in the same transport
session. `committed` requires that consumed call to return `ok=true` and
`committed=true`. Approximate field overlap, matching subjects alone, or a
caller-supplied telemetry field cannot count as consumption.

### Recovery-chain matching

A complete refusal opens one recovery episode keyed by call ID, session token,
and file-token set. The report examines the next seven completed clj-surgeon
calls or ten minutes, whichever ends first, matching the frozen baseline law.

- The first inspect whose file-token set intersects is
  `same-file-reread`.
- The first write call with the same subject-token set is
  `direct-corrected-retry`; its eventual success is separate.
- A different actionable call is `other-next-action`.
- No qualifying call before the bound is `abandoned`.

The report never infers the intended owner from a later successful mutation.
For classifier projection, it carries only the refusal and first-read tokens,
semantic kinds, cap/duplicate facts, and explicit evidence completeness. A
missing required selector or answer token remains `indeterminate` exactly as
the frozen classifier requires.

## Marker and digest fencing

The report input names one start marker and one end marker. A marker binds:

- ledger canonical path and file SHA-256 at read time;
- first and last included sequence;
- last included `event_sha256`;
- session key ID;
- report window start/end UTC;
- installed commit/tag and report compiler commit/tree;
- feature registry SHA-256;
- baseline receipt and marker SHA-256; and
- classifier SHA-256.

The I/O shell reads the ledger once, verifies its complete chain, projects the
closed window, and writes canonical `episodes.json`, `episodes.sha256`,
`report.json`, `report.md`, and `receipt.edn`. The classifier consumes the
digest-fenced `episodes.json` unchanged. No report can silently move a marker
or skip an invalid line.

## Claims compiler

Each output fact carries one of:

- `measured`: directly counted from complete ledger events;
- `observed-before-after`: same definition compared with the frozen historical
  aggregate, without causal attribution;
- `projected`: arithmetic using an explicitly named measured rate; or
- `unavailable`: required evidence absent.

Only direct counts and direct durations may be measured. Recovery-chain deltas
against the pre-install window are `observed-before-after`. Decode seconds
derived from 3.5237 ms per emitted byte are `projected`. The report refuses a
request, fixture, registry entry, or renderer that assigns `measured` to a
derived or historical-comparison value.

The human report leads with count-first lines:

```text
Prepared requests: 41 emitted; 18 consumed; 16 committed. [MEASURED]
Complete refusals: 12; 7 direct retries; 3 same-file rereads. [MEASURED]
Same-file reread share: 25%, versus 57% in the named baseline. [OBSERVED BEFORE/AFTER]
Decode-equivalent saving at 3.5237 ms/byte: 14.2 s. [PROJECTED]
```

Zero use is printed, not omitted. Invalid or incomplete evidence produces an
`INVALID` report and nonzero exit, never a clean zero.

## Failure behavior

The start append is a pre-execution gate. If it fails, the tool does not run.
The finish append occurs after the domain result exists. If it fails after a
mutation, the existing public result is still returned; the ledger state is
latched unhealthy, stderr receives one structured alarm, `/healthz` becomes
unready, and the next tool call refuses before execution. The durable start
without a finish makes the gap visible to the report. No retry can fabricate
the missing finish or reuse a call ID.

This is fail-loud without turning a telemetry failure into an ambiguous second
mutation. Recovery requires a new ledger segment and explicit operator action.

## Overhead and parity gates

Before an install card:

1. A pure 10,000-event projection screen must have p95 below 0.5 ms per event.
2. A 1,000-call local append screen must have p50 below 1 ms, p95 below 5 ms,
   and maximum below 25 ms per appended record.
3. A zero-model live HTTP differential must run at least 100 calls per arm on
   the same isolated server/fixture schedule. Candidate p50 server time may
   increase by at most 2 ms and p95 by at most 5 ms.
4. Eligible prepared reads, operation-less reads, mixed-ID refusals, complete
   write refusals, ordinary write successes, and transforms must have exact
   normalized public-result parity with substantiation off versus on.
5. Every event must stay below 32,768 UTF-8 bytes; the weekly report must state
   total ledger bytes and bytes per completed call.

A miss is a release NO-GO. It is not removed as an outlier or reframed as an
acceptable observability tax after measurement.

## Exclusions

- no raw request or response mirror;
- no source, prose, path, owner, replacement, command, URL, credential, or
  reasoning storage;
- no caller-authored model or telemetry authority;
- no retry selection, prepared-request execution, write authority, or tool
  behavior change;
- no automatic baseline advancement;
- no causal claim from a weekly before/after report;
- no performance-promotion authority;
- no model, network, install, or reload action inside report generation; and
- no schema change for future elaborator feature receipts.

## Rollback

The feature is additive and separately configured. Rollback disables the
substantiation observer and report target, removes no ledger bytes, and leaves
the existing product telemetry and public tool contract intact. An unhealthy
ledger cannot be hidden by disabling and reenabling the same segment; recovery
starts a new explicitly marked segment.
