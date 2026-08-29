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
- Experiment candidate: `a9a9d5dd50f099b8569a86f6c0897898f2d3fbb1`
- Experiment tree: `766f688e50d63b270543fc3a64eab5994f1df591`
- Product source/test/design diff SHA-256: `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`
- Experiment code diff SHA-256: `f521facbbcd84922af2f57cc8725bec6c99970d883f77d678ef63602055a7f6c`
- Protocol SHA-256: `635bc7e8f9467cf4e03a458d672665d3245e2a2686c9aad9b142192b6e447157`
- Harness SHA-256: `4d1b71d6bf4c767fcde1057e2344fff8fb4311a7bf59b5993f85e3a1ace3706d`
- Scorer SHA-256: `ef833ebc0c533735390a8ce05bed1d69c9dc2c86b1e01a02bc3412399fb0282a`
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

The complete test gate passed 17 logical tests and 86 assertions with zero
failures or errors.

## Real client-visible preflight

Three private capture-only servers were started serially. A fresh Codex
app-server registry inspection saw exactly one tool, `edit_clojure`, in every
arm. Each advertised/client projection matched after only the two already
accepted SDK normalizations: `annotations: null` to `{}` and removal of the
top-level input-schema `anyOf`.

The controller stopped before prompt submission. It made zero model calls and
zero mutation actions.

- Preflight summary SHA-256: `921b367355bfc5423688c364ad6aa4cb817a586d71abe4e77802f4c8a6216693`
- Run-config SHA-256: `4a0117517f61846a9fef48bf9e40d958dc8254f19026216ab6ed1fd6d975180c`
- Prerequisite report SHA-256: `f7541e933a2131abb2a255b04430ab0eb1d31d30d86c858f15052d9b21089d01`
- Raw archive: `/Users/genekim/src.local/clj-surgeon-bench-archive/2026-08-29/three-arm-request-shape-preflight-a9a9d5d-20260829T081400Z.tar.gz`
- Archive SHA-256: `73bdf5c21a3a3d9d19599d02b20aa2efd4a672513bec912aebc486b72f057b60`

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
`docs/observations/evidence/three-arm-request-shape-a9a9d5d.edn`.
