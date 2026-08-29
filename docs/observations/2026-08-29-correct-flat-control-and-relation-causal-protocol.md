# Correct Flat Control and Closed-Relation Causal Protocol

Date: 2026-08-29

Bead: `clj-surgeon-45j`

The HLD, mechanism correction, and this protocol are one approval unit. Later
corrections supersede the earlier commit labels recorded in repository history.

## Why the denominator changed twice

The first F/A/B screen retained an important ugly result, but both its first
interpretation and its capture-only correctness labels were wrong. The two flat
callers and both file-group callers did not omit the namespace edits or the
bespoke `detail-controls` edit. They constructed all 33 edit rows and all 14
owner deletions.

All four non-relation callers addressed the nine namespace edits as named
forms, for example:

```json
{"within":{"form":"sample.views.log"}}
```

The canonical spelling is the namespace owner:

```json
{"within":{"namespace":true}}
```

The historical capture scorer called the generic transaction compiler directly
and therefore refused `change-owner-mismatch`. Production does not take that
path. Its compact-location normalizer proves that an exact namespace name in
`within.form` identifies the namespace when no competing top-level owner has
that name, then emits the canonical namespace location.

Replaying the immutable calls through the current product path corrected the
record:

| Arm | Historical capture score | Product-equivalent replay |
|---|---:|---:|
| F flat | 0/2 | 2/2 exact |
| A file groups | 0/2 | 1/2 exact; one real schema failure |
| B closed relations | 2/2 | 2/2 exact after pure expansion |

Every successful replay compiled 51 matches across 9 files to all frozen future
hashes. The capture scorer's omission of compact-location normalization caused
the false negatives.

The relation hypothesis is therefore narrower and cleaner: it may make the
same correct decision cheaper to state. It does not rescue flat correctness.
The first screen remains capture-only, so its timing is descriptive rather than
a product speed claim.

There was a second harness confound. The cohort's one-tool surface replaced the
production `edit_clojure` description with the generic change-tool description.
The production description already distinguishes `{form}` from
`{namespace:true}` and `{namespace:name}`. The experimental description instead
led with “Each edits item contains file, within {form},” while later mixing in a
typed namespace-owner spelling from the direct-change language. The nested
schema was correct, but the high-salience prose was not the production contract.
Future arms must preserve the candidate's production description byte-for-byte.

## Authoritative correct flat control

The correct flat control is the retained `oracle-request` in
`dev/experiments/owner_aware_symbol_migration.clj`, without `workspace_root`.
For the smallest equivalent spelling, omit `matches: 1`; the compact editor
already defines one as the default.

```text
9 namespace rows, within.namespace=true
1 bespoke detail-controls row
23 owner-scoped symbol rows
14 exact owner deletions
--------------------------------------
33 edit rows / 37 edit matches
51 total edits / 9 files
```

The explicit-count oracle is 6,353 compact JSON bytes. Eliding the thirty
default `matches: 1` fields produces a 5,993-byte flat control. Counts 3, 2,
and 2 remain explicit.

## Pure equality proof

A standalone 512 MiB analysis nREPL at PID 55192, CWD
`/Users/genekim/src.local/clj-surgeon`, loaded only the experiment compilers and
immutable archived calls. It performed no product, runtime, or source mutation.

The 2,715-byte B request was lowered in two pure steps:

1. `symbol_migration` produced the 23 ordinary owner-scoped rows.
2. `require_change` produced the nine ordinary namespace rows from the frozen
   clauses.

Prepending those namespace rows to the one bespoke row and the 23 symbol rows,
while retaining the deletion group, yielded a map equal to the 6,353-byte flat
oracle:

```text
expanded map equals flat map       true
expanded edit rows                 33
expanded edit matches              37
affected files                     9
deleted owners                     14
canonical flat SHA-256             d329b306e52c5e253eb78aa39ca39650b7fbb0e8140661f26085c3ba586ef0c4
canonical expanded SHA-256         d329b306e52c5e253eb78aa39ca39650b7fbb0e8140661f26085c3ba586ef0c4
```

Raw JSON hashes may differ because map key order is not authority. Canonical
request equality is the required precondition; the real product cohort must
also retain normalized transaction and future hashes.

## Product-equivalent replay proof

The same bounded 512 MiB analysis nREPL replayed the retained calls through
`mcp_contract/validate-tool-params`, `tool-params->transaction`,
`mcp_compact_location/normalize-spec`, and the generic transaction compiler.
It performed no source mutation.

Both flat requests became exact. Each produced nine
`namespace-name-in-form` normalization records and the complete 51-match,
9-file future. The first grouped request did the same after its pure file-group
expansion. The second grouped request remains inadmissible and is not counted
as a correct performance observation.

The historical scorer's direct call to the experimental migration compiler is
therefore not a faithful product oracle. Future harnesses must traverse the
candidate's complete public admission and normalization path before scoring.

The corrected retained timing comparison is:

| Arm | Product-equivalent exact | Prompt-to-call midpoint | Capture wall midpoint | Payload |
|---|---:|---:|---:|---:|
| F normalized flat | 2/2 | 65.841 s | 68.500 s | 6,470 B |
| B closed relation | 2/2 | 48.912 s | 51.500 s | 2,715 B |

B reached the complete first call 16.929 seconds (25.7 percent) sooner and its
capture wall was 17.0 seconds (24.8 percent) lower. Because the arm surfaces
differed and the harness did not mutate or verify, this is the signal that
earns the next experiment—not its conclusion.

## One candidate, one visible surface

The causal cohort must use one immutable product candidate that implements
both the legacy flat route and the paired closed-relation route. Every run sees
the same tool name, description, schemas, annotations, server instructions,
model, effort, task, fixture, verifier, and client binary.

The common prompt defines both representations. The only arm difference is one
assignment byte:

```text
Assigned shape: F
Assigned shape: R
```

This intentionally charges the relation schema surface to both arms. Comparing
an old flat-only server with a relation server would confound request shape with
schema discoverability and product code.

Before the first model token, prove:

1. candidate commit, tree, binary, task, fixture, scorer, harness, verifier,
   and tool-surface hashes are frozen;
2. both oracle requests pass the public schema and real compiler;
3. relation expansion equals the normalized flat canonical transaction;
4. both compile to 51 edits, 9 files, and all nine frozen future hashes;
5. both select the same exact verifier and terminal-response contract;
6. both routes traverse the same compact-location normalizer, while the flat
   route never invokes relation lowering; and
7. a fresh client registry exposes exactly one tool and one identical surface,
   using the production compact-editor description rather than the generic
   change-tool prose.

## Smallest useful cohort

Run one serial, counterbalanced screen on one Anvil seat. Every run receives a
new isolated workspace copied from the same frozen starting tree and verified
byte-for-byte before model launch; a fresh Codex home/session; a fresh receipt
directory; and a new private MCP server. The runner starts the server before the
timed turn, performs the same bounded surface/identity warm-up, and then proves
that request, receipt, and tool-call caches are empty. Dependency and build
artifacts are read-only and shared equally. The server stops after the run.
These lifecycle rules are fixed for every arm and cannot change between blocks.

```text
Block 1: N R R N
```

`N` is the already-correct normalized flat route. `R` is relation lowering
followed by that same normalizer and generic compiler.

Every attempt is retained after model launch. Do not replace an incorrect,
slow, or treatment-nonadherent run. For an arm, `median` means the ordinary
sorted-sample median; with two observations it is the arithmetic mean of the
two central values. Block improvement is
`(median(T_verified_N) - median(T_verified_R)) / median(T_verified_N)`.
The screen stops if either N is incorrect, either R is incorrect, or block-one
improvement is less than 15 percent. A stopped screen is evidence against
promotion, not permission to tune the prompt under the same protocol identity.

If all four calls are exact and the signal is at least 15 percent, run the
predeclared complementary block:

```text
Block 2: R N N R
```

At `N=8`, promotion requires:

- 4/4 exact first calls for each arm;
- one `edit_clojure` call per run and no other action;
- identical canonical transaction and future hashes;
- exact verification inside the transaction;
- the R median lower than the N median in each counterbalanced block; and
- pooled improvement of at least 20 percent, using the same formula over all
  four `T_verified` observations per arm.

The cohort is capped at eight. A borderline result is not promoted.

## Per-run route and correctness gate

An admitted run must:

- emit `edit_clojure` as its first item, without a preamble;
- use its assigned representation;
- make exactly one MCP call;
- perform zero reads, searches, shell commands, native file changes, refusals,
  retries, recovery, or fallback;
- cover all four decision classes exactly once;
- compile to the frozen canonical transaction;
- report 51 edits and exactly 9 files;
- commit and read back every frozen future hash;
- run the same project-owned exact verifier inside the transaction;
- return `verification_complete=true`, verifier identity and output evidence,
  receipt/read-back hashes, and `next_action=none`; and
- make no second tool call.

An independent post-turn scorer proves exact bytes, source inventory, parse,
and approved meaning preservation. It does not run inside the timed turn.

## Clocks

Retain raw events and report these clocks separately:

| Clock | Boundary |
|---|---|
| `T_emit` | turn start to the observer event containing the complete tool arguments |
| `T_tool` | observed tool-call start to observed tool completion |
| `T_server` | server-authoritative operation envelope |
| `T_post` | tool completion to final turn completion |
| `T_verified` | turn start to final completion, only for an exact first-call verified run |

The primary product metric is `T_verified`. Do not subtract server time from
it. `T_emit` tests the materialization mechanism. If R wins complete wall but
does not reduce `T_emit`, the proposed explanation is falsified even if the
product result remains useful.

## Optional serialization ablation

If the production-realistic cohort wins, a separate relay ablation may supply
each exact canonical request verbatim and ask the model only to emit it. That
isolates payload-generation cost from schema recognition and structural
planning. It cannot replace the production cohort and does not establish
spontaneous usability.

## Claim boundary

The relation hypothesis fails if it loses any first-call correctness, changes
transaction or verifier semantics, needs a second turn, or fails the 20 percent
complete-wall gate at `N=8`. Smaller payload alone is not success.

No model, Anvil, install, reload, shared runtime, or product mutation is
authorized by this protocol. Product work remains behind the HLD approval
gate.

The historical owner-aware shell runner remains capture-only. It may run a
two-arm `--pilot` for call-shape reconnaissance, but it refuses cohort mode
before authentication or model launch. This protocol requires a new
real-mutation runner after the HLD, LLD, EARS, red-test, implementation, and
verification gates approve that boundary.
