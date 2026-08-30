# Captain's log: the native pre-landing parse-gate prize did not reproduce

Date: 2026-08-29

## Decision

Do not put a reader-parse gate in front of every native Clojure write on the
strength of the retained six-day corpus.

The independent replay found 14 candidate write -> error -> same-target write
chains within five minutes. Bounded manual review classified eight as genuine
causal coding loops, three as intentional red experiments, and three as
non-causal or undecidable pairings. None of the eight genuine loops was a
post-state reader error. The only candidate with a reader-error signature was
a malformed REPL expression, not malformed source on disk.

The predeclared gate required at least 15 genuine loops per week. Eight genuine
loops in the 5.89-day window is about 9.5 per week. The more important product
predicate is zero: a pre-landing source parse would have prevented 0/8 reviewed
genuine loops.

## Frozen population and independent method

The audit used one `study-agent-usage` collection with the half-open window
`2026-08-24T07:00:00Z` through `2026-08-30T04:17:08.306112Z`.

- Collector receipt:
  `/private/tmp/clj-surgeon-native-gate-audit-receipt.json`
- Collector receipt SHA-256:
  `bea3cd54ea936737f7bb8684136742a11871683a2feff5cb8e710757db800883`
- Independent audit program:
  `dev/experiments/native_prelanding_gate_audit.py`
- Audit program SHA-256 before this log:
  `df34e541a92d8175bbca5d302bf8ddd9088cf01d95b382fb5521855d99c436ef`
- Complete generated audit SHA-256:
  `17d391631f8ece76a5a566033df6c9f59c23c580e898c3d66b1467f0384473b4`
- Base commit:
  `79b4cfe064495761956080332df53c2394bc2a9b`

The program independently parses only the rollout files named by the collector
receipt. It admits a session only when its original `session_meta` timestamp is
inside the frozen window. It identifies successful `apply_patch` updates to
existing `.clj`, `.cljs`, or `.cljc` files, then pairs an error-bearing tool
output with the latest prior write and the first later write that shares a
target. The five-minute bound is the headline; 60-second and 15-minute results
are retained as sensitivity checks.

The `122` count in the prior census is the collector evidence-file count. Its
own session-start population is `118`. This audit independently reproduces
that `118` exactly.

## Claim versus replay

| Measure | Single-source claim | Independent replay |
|---|---:|---:|
| Session starts in the exact window | not stated | 118 |
| Clojure-writing sessions | 26 | 27 |
| Writing sessions with reader/compile-bearing outputs | 11 | 11 |
| Successful existing-Clojure native writes | 444 | 463 |
| Reader/compile-bearing tool outputs | 131 error events | 44 distinct outputs; 66 matched signature occurrences |
| Tight write -> error -> rewrite loops | 42 | 8 at 60 s; 14 at 300 s; 16 at 900 s |
| Sessions contributing 300-second loops | not stated | 4 |
| Genuine causal 300-second loops | not stated | 8 |
| Post-state reader-parse-catchable genuine loops | claimed at least 30 | **0** |
| File update occurrences | 444 used as denominator | 541 occurrences across 463 writes |
| Top 13 target files | 192 | 221 |
| Maximum updates to one target | 34 | 33 |

The source and independent counts are close on population and file
concentration, but not on errors, loops, or preventable loops. The error count
also needs a unit. One tool output can repeat the same signature in a stack
trace. This audit therefore reports both distinct outputs and signature
occurrences, and does not treat either as an independent defect count.

## Bounded review of every five-minute candidate

The table contains privacy-safe session keys and rollout ordinals. No prompt or
source body is retained here.

| Session/write ordinal | Review | Causal? | Reader gate? | Error shape |
|---|---|---|---:|---|
| `216732d7abe4/1926` | genuine accidental | yes | no | missing namespace require |
| `216732d7abe4/1993` | genuine accidental | yes | no | classpath and test wiring |
| `216732d7abe4/8111` | intentional experiment | expected red | no | absent product seam |
| `216732d7abe4/9380` | genuine accidental | yes | no | wrong library function |
| `216732d7abe4/9404` | genuine accidental | yes | no | missing imported class |
| `216732d7abe4/9861` | intentional experiment | expected red | no | tests preceded implementation |
| `216732d7abe4/12120` | genuine accidental | yes | no | forward reference |
| `216732d7abe4/19100` | genuine accidental | yes | no | missing helper |
| `216732d7abe4/21788` | undecidable | no | no | test invocation named an absent Var |
| `52b31dc01678/1606` | genuine accidental | yes | no | forward declaration |
| `c54a7c4283f9/182` | genuine accidental | yes | no | stale symbol reference |
| `c54a7c4283f9/647` | undecidable | no | no | malformed REPL expression |
| `471c92d91531/363` | undecidable | no | no | next write did not repair the reported error |
| `471c92d91531/571` | intentional experiment | expected red | no | explicit red witness |

The manual classification is deliberately conservative. Prototype mistakes
remain genuine accidents even when they occurred in experiment code. That
choice favors the proposed gate. It still leaves only eight causal loops and
zero preventable ones.

## Why a source parser would not have helped

The genuine mistakes were compile- or integration-level failures: missing
requires, classpath wiring, absent imports or helpers, stale symbols, and
forward references. Every resulting file can be reader-valid. A pre-landing
parse cannot prove namespace resolution, class availability, SCI support, or
test-harness correctness.

The raw detector found ten outputs containing reader-error phrases. Bounded
inspection found REPL-input failures and outputs that quoted reader errors as
test or analysis data. The sole such output inside a five-minute candidate
loop was a malformed REPL expression. It was not a post-write parse failure.
This is why error text alone must not become gate-benefit evidence.

## False-refusal exposure and parser choice

The 463 admitted writes include two `.cljc` target actions and two actions that
contain reader conditionals; those sets can overlap. No patch was classified as
containing a custom tagged literal. A zero-model probe showed that the
repository's `rewrite-clj` parser accepted representative reader-conditional,
tagged-literal, and regex source, three of three.

This is not proof of zero false refusals. The retained patch calls do not carry
an immutable complete post-state for every target, so the exact number of valid
post-states that a proposed parser would reject is not reconstructible. A JVM
reader with the wrong dialect or data-reader policy can reject valid CLJC or
project tags. A `rewrite-clj` syntax parser has the safer shape, but placing it
in front of all native writes still creates a new availability dependency for
no observed catchable loop.

If this option is ever reopened, the minimum screen is a shadow-only gate over
complete candidate bytes. It must cover `.clj`, `.cljs`, and both `.cljc`
branches, preserve unknown tagged literals as syntax, and report false
refusals before it receives write authority. It must not infer that a reader
pass is semantic verification.

## Counterfactual and stop

Had the claimed `>=30/42` catch rate reproduced, a deterministic native-channel
gate would have been attractive because it required no model adoption. It did
not reproduce. Under the explicit event and causality rules, the proposed gate
would have blocked none of the reviewed accidental loops while sitting on all
463 native Clojure writes.

The option is therefore stopped. Do not build a product gate, wrapper, or
model cohort from this corpus. Reopen only with a fresh, independently retained
population containing at least 15 genuine post-write source-reader loops per
week and a shadow parser that demonstrates materially more prevented defects
than false refusals.

The honest program-level conclusion is that this last per-call optimization
prize was contamination and causal overreach. The durable gains from the study
remain its measured economics and safety laws; they do not create an obligation
to ship a mechanism after its predicate fails.
