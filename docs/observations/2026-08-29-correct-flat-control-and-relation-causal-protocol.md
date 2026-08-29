# Correct Flat Control and Closed-Relation Causal Protocol

Date: 2026-08-29

Bead: `clj-surgeon-45j`

Product HLD review base: `f5d58ce`; the mechanism correction is committed with
this protocol.

## Why the denominator changed

The first F/A/B screen retained an important ugly result, but its first
interpretation was wrong. The two flat callers and both file-group callers did
not omit the namespace edits or the bespoke `detail-controls` edit. They
constructed all 33 edit rows and all 14 owner deletions.

All four non-relation callers addressed the nine namespace edits as named
forms, for example:

```json
{"within":{"form":"sample.views.log"}}
```

The compact editor requires the namespace owner:

```json
{"within":{"namespace":true}}
```

The compiler therefore refused `change-owner-mismatch` before mutation. The
closed relation was 2/2 because `require_change` asks the model for the real
decision—file, exact add, and optional exact removal—while the compiler derives
the namespace address and guarded clause replacement.

This changes the mechanism from “relation fields remind the model about omitted
work” to “relation fields remove a derived structural-address decision.” It
does not change the no-speed-claim boundary: the first screen was capture-only
and the F/A arms did not complete.

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
3. relation expansion equals the flat canonical transaction;
4. both compile to 51 edits, 9 files, and all nine frozen future hashes;
5. both select the same exact verifier and terminal-response contract;
6. the flat route never invokes relation lowering; and
7. a fresh client registry exposes exactly one tool and one identical surface.

## Smallest useful cohort

Run one serial, counterbalanced screen on one Anvil seat with fresh state per
run:

```text
Block 1: F R R F
```

Every attempt is retained after model launch. Do not replace an incorrect,
slow, or treatment-nonadherent run. The screen stops if either F is incorrect,
either R is incorrect, or R improves complete verified midpoint by less than
15 percent. A stopped screen is evidence against promotion, not permission to
tune the prompt under the same protocol identity.

If all four calls are exact and the signal is at least 15 percent, run the
predeclared complementary block:

```text
Block 2: R F F R
```

At `N=8`, promotion requires:

- 4/4 exact first calls for each arm;
- one `edit_clojure` call per run and no other action;
- identical canonical transaction and future hashes;
- exact verification inside the transaction;
- R faster in both counterbalanced blocks; and
- at least 20 percent lower pooled complete verified midpoint or median.

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
