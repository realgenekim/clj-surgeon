# Captain's Log: Closed Relations Earned a Hold

Date: 2026-08-29

Bead: `clj-surgeon-45j`

Final product candidate: `b36d494`

Corrected scorer head: `304716b1362dba494b0ebe22ea55f3983b93bd7b`

Decision: **HOLD promotion. Keep the facade and its safety witnesses; do not
install or publish it as a promoted route.**

## The architecture succeeded

Closed Compact Relations are now one pure request facade over the existing
transaction engine. A relation request names one exact symbol migration and
one exact require change. The facade lowers those relations to ordinary
compact edits. The established path still owns source capture, path
canonicalization, compact-location normalization, generic compilation,
mutation, read-back, exact verification, receipts, and rollback.

The change did not add a second executor, cache, plan identity, or retry
authority. `edit_clojure` still refuses verifier selection. Only the compact
`apply_clojure_changes` entrance can run the exact project-owned verifier in
the same transaction. The implemented product laws are covered by
`MCP-OP-EDIT-020..024` and `MCP-OP-EDIT-026..027`.

`MCP-OP-EDIT-025` remains active. It is the performance-promotion law, and the
experiment did not satisfy it.

## Four immutable cohorts made the request legible

Each failure removed one ambiguity and became a permanent witness. No failed
cohort was silently repaired or overwritten.

| Candidate | What the model emitted | Semantic and exact-verification result | Decision |
|---|---|---:|---|
| `3ccefbf` | Invented top-level wrappers such as `changes`, `R`, and `representation`. | 0/4 | Stop. Teach the public top-level shape. |
| `c83e37d` | Correct top-level fields but invented nested row, edit, and deletion shapes. | 0/4 | Stop. Publish the closed nested grammar. |
| `8b0ed8a` | Three exact calls; one relation arm used namespace scope for the owner `detail-controls`. | 3/4 | Stop. Disambiguate namespace and named-owner scope. |
| `b36d494` | Four semantically correct, one-shot, exact-verified transactions. | 4/4 semantic and verified; 3/4 canonical admission | Stop before Block 2. One flat request violated frozen canonical transaction order. |

The archives are immutable:

| Candidate | Archive SHA-256 |
|---|---|
| `3ccefbf` | `eb8c2b343136fe0ffb54522d593494cbfd04b64fe1478719ac116a2abc4fa4f4` |
| `c83e37d` | `fb8b185eb11ac3ac7a26530c48b7f5abd8cfae8f9595c5191b6e6edf42c92e46` |
| `8b0ed8a` | `760cf221d834952104943ee0756acc1abc228a87c2d38f5957eebbdfe7f6fcbf` |
| `b36d494` | `c4f58c3719418bd8e483f978f538c31b1c921772f2828006c6ea98ffb109cc09` |

They are stored under
`clj-surgeon-bench-archive/2026-08-29/<candidate>-cohort-<timestamp>.tar.gz`.

## The final Block 1 signal was large

All four `b36d494` runs used Codex Sol/high, one public
`apply_clojure_changes` call, the same relation-capable surface, the same task,
the same fixture, and the same exact verifier. All four produced the correct
future source, exact verification, and no shell fallback.

| Run | Arm | `T_emit` | `T_verified` | Complete wall |
|---|---|---:|---:|---:|
| N1 | normalized flat | 51.832 s | 57.208 s | 57.951 s |
| R1 | closed relation | 38.039 s | 42.027 s | 42.800 s |
| R2 | closed relation | 30.244 s | 34.986 s | 35.959 s |
| N2 | normalized flat | 59.356 s | 64.305 s | 65.341 s |
| midpoint | normalized flat | **55.594 s** | **60.757 s** | **61.646 s** |
| midpoint | closed relation | **34.141 s** | **38.506 s** | **39.380 s** |
| reduction | R versus N | **38.6%** | **36.6%** | **36.1%** |

Against the retained 122.278-second native denominator, these Block 1
midpoints are descriptively about 1.98x for normalized flat and 3.11x for
closed relations. These are not promoted Surgeon-versus-native results:
Block 2 never ran, and the causal cohort did not pass its identity gate.

## Why Block 2 did not run

The scorer first exposed one real instrumentation defect. Product receipts are
durable workspace receipts under the user's state directory. The scorer
incorrectly required the receipt path to remain inside the temporary benchmark
workspace, even though that workspace is deliberately removed after the run.
Commit `4eff9c8` corrected only that path assumption. It retained absolute
canonical spelling, receipt hash, read-back hashes, verifier evidence, and
workspace identity. The corrected scorer passed 6 tests and 107 assertions.

Corrected rescoring then left one failure. N1 compiled the same request edit
multiset in a different order:

```text
frozen N/R/N2 canonical transaction
  9 namespace edits
  23 symbol edits
  1 bespoke detail-controls edit
  14 owner deletions

N1 transaction
  9 namespace edits
  1 bespoke detail-controls edit
  23 symbol edits
  14 owner deletions
```

The expected transaction hash was
`da06cf18ae5d3b43e50eec1c03c9ff979234a5d6411f9f121ee07d833a2fec3c`.
N1 produced
`8cd2d478fcdfb713b3b4d10a1ceee3823a4613bb5ac78da218f27b9e7d395847`.
Both routes produced the same nine future hashes and N1 was exact verified, but
the experiment had predeclared exact ordered canonical transaction identity as
an admission condition. Therefore Block 1 did not authorize Block 2.

An independent audit removed only `workspace_root` from N1 and N2, sorted their
edits by canonical JSON, and obtained the same request-multiset SHA-256:
`48c68e27921875d78397364de0662070fdf622fb6c258e14b975ac418cbdaec6`.
That proves an order-only request difference for this frozen fixture. It does
not prove that edits commute in general or that their canonical transactions
are equal.

Weakening that gate after seeing the result would make the attractive timing
number easier to publish and the experiment less trustworthy. We stopped.

## What we learned

The descriptive attempts are consistent with a narrow and useful hypothesis:
a closed relation can make one complete structural decision substantially
cheaper for a model to state. This invalid cohort does not prove a causal win
or rate. It also does not prove that smaller schemas are faster. The extraction-only surface
experiment had already removed 63.7 percent of visible tool bytes and bought
only a 5.2-percent complete-wall change. Here both arms saw the same enlarged
surface; only the request language changed.

The result also uncovered the next architectural question. Are independent,
disjoint edit rows an unordered semantic set that Surgeon should canonicalize,
or is caller order part of transaction authority? Today the system and the
benchmark treat order as authority. Changing that law may reduce accidental
request variance, but it requires its own design, collision/overlap proof, and
fresh cohort. This retained cohort cannot be reused to promote a revised law.

## Decision and next gate

- Keep the pure facade and safety witnesses on the experimental branch.
- Leave `MCP-OP-EDIT-025` unchecked.
- Do not install, reload, or advertise relation lowering as promoted.
- Preserve the four archives and the corrected scorer as the experiment record.
- Open a separate decision hill for canonical ordering of proven-disjoint edits.
- First earn any order-invariant authority with a zero-model
  permutation-and-overlap matrix. If that law changes, freeze a new candidate
  and run a fresh whole `N R R N` then conditional `R N N R` cohort. Do not
  rerun N1 alone or rescore this cohort into a pass.

## Method lesson

Kent Beck's rule paid twice: when the experiment was difficult to state, we
made the request language and harness easier to change; then each failed model
call became a smaller permanent grammar witness. `(N * K * sigma) / t` paid in
the independent adversarial lane: it found false scorer authority before the
main lane could quote an exciting number.

The product mechanism is promising. The promotion claim is not yet earned.
That distinction is the result.
