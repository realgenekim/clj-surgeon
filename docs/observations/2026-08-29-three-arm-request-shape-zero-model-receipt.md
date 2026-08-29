# Three-arm request-shape screen: zero-model receipt

Date: 2026-08-29

Durable owner: `clj-surgeon-45j`

Status: zero-model compiler, schema, falsifier, and real Codex registry gates
passed. GO for the frozen `F A B B A F` capture-only cohort after explicit
approval. No model, Anvil, product edit, install, reload, shared-port call, or
fixture mutation occurred.

## Immutable identity

- Production base: `54aae16f340033dc6d9452043b335c6bb98dea04`
- Production tree: `937ee8032c8697594bb1b6b5b7036747b8bb9517`
- Experiment candidate: `b298b8b6306bdd3d7d4713234e65f5cbc2bc7aa4`
- Experiment tree: `08bdfc92678b80d869d19f782842d71c263b8a1f`
- Product source/test/design diff SHA-256: `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`
- Experiment code diff SHA-256: `5ca7eeb151c6d1fda13211ad10186c0a5428b14f0a0e3e0c5bc67a3b0fe4d157`
- Protocol SHA-256: `635bc7e8f9467cf4e03a458d672665d3245e2a2686c9aad9b142192b6e447157`
- Harness SHA-256: `d9b3e2ec8385d3989de0af241eaac48983e57dd614f80e3ef21ff9350f5692d1`
- Scorer SHA-256: `69a66bddad9590eaf7be54c03cd42ffa5375af2d9bbf7726d0d5e2f1e1cbb880`
- Observer SHA-256: `a3b8fc5b8259f14fb5ea0f306c24a013b78e9f8a1989adb1eee1a62c9160bc18`
- Frozen task SHA-256: `789809060a52d647197cf1fb5ade2cc0a76992209a0223991c7a51179f44d8e1`
- Frozen capsule SHA-256: `7d985f4d30acdf871f615b174e0f6c37338539253e6591cf898f96c26f39d4b9`

All code is under `bench/` or `dev/experiments/`. The amended protocol is the
only observation change before this receipt.

## What was built

The accepted capture server, event clock, Codex registry observer, task, and
canonical transaction compiler remain the execution path. One experiment-only
namespace adds three pure public-request projections:

```text
F  flat request -----------------------------------+
                                                    |
A  file_groups -> exact local rows ----------------+--> existing validator
                                                    |    and transaction compiler
B  symbol_migration -> exact symbol rows -----------+
   require_change + frozen sources -> ns rows ------+
```

The A and B expanders return canonical flat requests or typed refusals. They do
not write, cache a plan, call a model, search by similarity, or compile a
second transaction representation.

## Exact zero-model result

| Arm | Exact frozen future | Decision coverage | Arguments | Client-visible surface |
|---|---|---|---:|---:|
| F: flat | yes | complete | 6,353 bytes | 8,442 bytes |
| A: `file_groups` | yes | complete | 5,189 bytes | 10,949 bytes |
| B: `symbol_migration` + `require_change` | yes | complete | 2,703 bytes | 10,803 bytes |

Every arm compiled to the same authority-bearing change multiset and aggregate
expectation. Every arm produced the exact 51-match, 9-file frozen future and
all nine expected file hashes.

A is 18.3 percent smaller than F and meets its 82-percent argument budget. B
is 57.5 percent smaller than F and is only 42.5 percent of F's arguments. The
2,703-byte B result supersedes the provisional 3,600-byte measurement, which
used a different relation spelling. B reuses Faraday's accepted ordered
`symbol_migration` language verbatim.

The surface cost remains visible: A adds 2,507 pre-call bytes and B adds 2,361.
Only a fresh model cohort can establish whether deleting request decisions is
worth that catalog growth.

## Decision coverage

F and A coverage is derived from their expanded requests rather than assigned
from the oracle. B coverage compares the exact ordered nine-file migration
corpus and all 23 ordered owner/from/count rows against the accepted basis. It
also proves:

- 27 declared symbol matches;
- all 9 changed paths;
- the 3 non-default counts;
- one exact added lib/alias and all 3 exact removals;
- the one complete bespoke edit; and
- all 14 exact deleted owners.

B derives only repeated target strings, default counts, and require-clause
text from frozen source. A row permutation still compiles semantically, but it
fails both ordered decision coverage and treatment adherence. This prevents
canonical transaction comparison from erasing the model-facing construction
law.

## Schema and refusal gates

The first draft exposed two accidental schema holes. Both became permanent
witnesses before client preflight:

1. candidate fields are explicit standalone top-level authority branches;
   legacy `delete_owners` or `edits` cannot mask an incomplete candidate
   schema; and
2. a local A edit preserves the closed field-pair relation but removes the
   ordinary `file`/`files` ownership relation, because its enclosing group owns
   the file.

The schema authority accepts minimal candidate-only A and B requests. It
rejects a local A row that smuggles its own file.

The pure gates also reject or disqualify:

- mixed flat and grouped edits;
- a local grouped file, duplicate file group, or duplicate expanded row;
- duplicate or missing frozen require files;
- reader-conditional require entries;
- target alias collisions or non-unique removals;
- duplicate symbol rows; and
- reordered B owner rows.

The controller's single bounded-heap `--self-test` invocation passed 18 tests
and 91 assertions with zero failures or errors. This includes the complete
5-test, 23-assertion candidate-admission suite. An earlier controller omitted
that file and therefore ran only 12 tests and 63 assertions; the manually
combined 17/86 claim was true of two commands but false of the launch artifact.
The earlier receipt and archive remain immutable superseded evidence.

The scorer also treats the completed agent message as correctness evidence.
It reads the last completed `agent_message` from `events.jsonl`, retains its
exact bytes in `score.edn`, and requires the response to equal `call captured`.
The permanent falsifier proves that a varied terminal response is incorrect.

## Real client-visible preflight

Three private capture-only servers were started serially. A fresh Codex
app-server registry inspection saw exactly one tool, `edit_clojure`, in every
arm. Each advertised/client projection matched after only the two already
accepted SDK normalizations: `annotations: null` to `{}` and removal of the
top-level input-schema `anyOf`.

The controller stopped before prompt submission. It made zero model calls and
zero mutation actions.

- Preflight summary SHA-256: `8529d17af1f1e59cd849c585a30e7ea49fe24b094c02c09e278382c3c18da084`
- Run-config SHA-256: `34bb6455d5da3eef29928c7cc83a1cfec51d4d4f59e34f090500ab9e011913ca`
- Prerequisite report SHA-256: `f7541e933a2131abb2a255b04430ab0eb1d31d30d86c858f15052d9b21089d01`
- Raw archive: `/Users/genekim/src.local/clj-surgeon-bench-archive/2026-08-29/three-arm-request-shape-preflight-b298b8b-20260829T082259Z.tar.gz`
- Archive SHA-256: `d2eba34d2a5b89f6ba9227c16ba7804b4619e7c04a78ef4252f0e8a7b936ffad`

The real token-free registry preflight was rerun because `run-config.json`
binds both the exact Git head and the harness SHA. The public surfaces did not
change, but retaining the old preflight would not prove the repaired launch
artifact. All three fresh registry projections passed with zero model calls
and zero mutation actions.

## Verdict

**GO** for exactly six fresh, serial Sol/high capture-only runs in this order:

```text
F A B B A F
```

Require every identity, schema, one-tool, treatment-adherence, decision-
coverage, compiler, and frozen-future gate before scoring performance. A
post-token harness failure consumes the whole cohort. Do not retry a single
arm or add runs selectively.

This is **NO-GO** for product integration. The experiment-only require parser
and relation expanders are deliberately larger than any earned product seam.
Passing compiler tests only authorizes the model screen; it does not authorize
promotion.

Machine-readable evidence is in
`docs/observations/evidence/three-arm-request-shape-b298b8b.edn`.
