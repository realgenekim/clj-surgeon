# Captain's Log: two seconds were call construction; eleven were not

Date: 2026-08-29

## Headline

A same-model, same-task, same-product A–B–B–A cohort isolated the cost of
constructing the already-decided extraction request. Giving Sol/high the exact
populated argument object saved about 2.2 seconds before the first tool call.
It did not eliminate the roughly 11-second fixed model/service boundary.

The literal relay route completed at a 17.054-second midpoint. Against the
retained correct native baseline of 122.278 seconds, that is 7.17 times faster.
This is an oracle upper bound, not yet a product claim: the treatment prompt
supplied the answer.

## Cohort identity

- Candidate: `51efa0591fc4083fa875a4754b7d56d88497b26b`
- Model: `gpt-5.6-sol`, reasoning `high`
- Task: frozen Sessionize 15-owner format extraction
- Order: control, literal relay, literal relay, control
- Parallelism: one
- Product surface: the same full production MCP catalog in every arm
- Raw result root: `/tmp/clj-surgeon-materialization-abba-51efa05`
- Planned four-run raw-tree manifest SHA-256:
  `e4024579a8199f479a4e9ec80583dfbaa2a64e0611b89a0ccf8619e70f72475f`
- Root-normalized logical argument SHA-256 in every arm:
  `01d502300c9e6af22e22e69f5680a4ed767ecc7fa64e4c9bce1d91b78bdfba47`
- Codex-visible catalog projection SHA-256 in every arm:
  `1d0397c7d7b3720533efe1fbd3890768c663543542062c821d99b18d4b5b8f53`

Every planned arm emitted `apply_clojure_changes` as its first item, supplied
canonical arguments equal to the frozen object, performed exactly one MCP
call, used no shell or native file tool, completed the same exact verified
extraction, and passed the semantic scorer.

## Results

| Order | Arm | Complete wall | Pre-first-call | Server authoritative | Receipt interpretation |
|---:|---|---:|---:|---:|---:|
| 1 | Control | 18.554 s | 13.160 s | 3.100 s | 1.380 s |
| 2 | Literal relay | 15.867 s | 10.066 s | 3.596 s | 1.318 s |
| 3 | Literal relay | 18.241 s | 12.000 s | 3.172 s | 2.194 s |
| 4 | Control | 20.364 s | 13.317 s | 3.907 s | 2.290 s |

Midpoints:

| Metric | Control | Literal relay | Improvement |
|---|---:|---:|---:|
| Pre-first-call | 13.238 s | 11.033 s | 2.205 s / 16.7% |
| Complete wall | 19.459 s | 17.054 s | 2.405 s / 12.4% |
| Speed versus retained native | 6.28x | 7.17x | +0.89x |

Both paired positions favored literal relay. The first pair improved pre-call
wall by 23.5 percent; the second improved by 9.9 percent. The pooled 16.7
percent improvement misses the stricter 20 percent promotion gate selected by
the independent adversarial review.

The control midpoint is within 1.3 percent of the earlier 19.216-second product
median. That calibration supports the phase comparison and argues against a
large route drift.

## Interpretation

The experiment rejects two extreme stories:

1. Call construction is not free. Supplying the exact object reduced both
   paired pre-call intervals and cut about 2.2 seconds at the midpoint.
2. Call construction is not the dominant remaining cost. Even the oracle arm
   spent about 11.0 seconds before emitting an already-complete 608-byte call.

The remaining interval includes model scheduling, cached-input processing,
inference, hidden reasoning, output materialization, and transport. The event
stream cannot separate those components. Reasoning-token counts exist, but
there is no timed reasoning event or argument-stream clock; calling the whole
interval “thinking time” would overstate the evidence.

The product implication is proportional. Pre-filling a mechanically complete
request may earn a small win where the decision already exists, but it cannot
deliver another 2–5x by itself. The much larger gains still come from deleting
whole model boundaries: one compiled mutation, fused exact verification, and a
terminal relay.

## Harness incident and repair

After the four declared runs, the general benchmark harness started its legacy
default compact-skill bonus arms. The first unwanted arm completed after
188.133 seconds and failed; a second was interrupted. These runs occurred only
after the complete A–B–B–A cohort and are excluded by identity, not erased from
the raw result directory.

The harness now defaults bonus arms off, requires the exact serial C–R–R–C
matrix when the literal context appears, requires zero shell calls and the
extraction tool as the first selected tool, and records normalized argument and
client-visible catalog hashes. This converts an observed foot-gun into a
permanent fail-closed boundary.

## Decision and next hill

Keep the literal-relay result as a measured oracle bound. Do not promote a new
request API from this cohort. The 20-percent pre-call gate missed, and the
remaining fixed boundary is much larger than the removable construction slice.

The next hill should therefore target a route where one mechanically justified
change can delete an entire model boundary, not merely shorten one. The closed
relation compiler for the 51-edit decision remains the highest-value candidate
once its Linked-Intent approval gate opens. While it is gated, the honest
ungated work is to classify retained multi-boundary routes for a narrower,
high-coverage compiled chord rather than revive the rejected general read
graph.
