# Captain's log, 2026-08-30 (late watch) — Spark, and the inverse surprise

*Written by mayor@skiff. Gene's side quest, verbatim: "codex 5.3-spark has reputation of
being very fast. Please measure it, and see how it works with surgeon -- any surprises
would be interesting!!!" One frozen characterization screen later, the surprise arrived —
and it points the same direction as everything else this day ratified. Receipts:
`experiment/spark-caller-screen-20260830` @ 2b2a417a, raw archive SHA 25406e5c…,
12/12 exact outcomes, wrong-subject 0/12. Screen evidence, not acid-test proof.*

## The findings

The canonical model name is `gpt-5.3-codex-spark` — all three intuitive aliases were
rejected, so the exact accepted name rides in the replay command. The reputation is
earned: 1.31× faster than gpt-5.6-sol on trivial prompts (3.95s vs 5.19s median, 3/3
pairs).

Against the newest Surgeon surface:

- **Guarded writes: 3/3 exact one-shot.** The compact edit grammar with explicit guards
  gave the fast model nothing to fumble.
- **Refusal recovery: 3/3 one-turn, zero rereads.** Deliberately triggered
  `expect-count-mismatch`; Spark recovered from the refusal payload alone every time.
  The complete-vocabulary refusal law — installed yesterday, proven causal at −100pp on
  a frontier caller — holds for a speed-tuned caller too.
- **Structural reads: correct but 0/3 one-shot** — 5, 2, and 2 schema refusals,
  including hallucinated fields. The newest operation-less shorthand went unadopted
  0/3 (omitted IDs 2/3).
- **Routing on the six-edit chord: native 3/3**, all exact — but with erratic action
  counts (3, 1, and 9 native actions for the same task).

## The inverse surprise

> "Spark understood the compact write grammar and refusal causal law extremely well,
> yet repeatedly hallucinated fields in the more permissive read schema."

Intuition says a fast, cheap model fails on the strict surface and coasts on the
permissive one. The measurement says the opposite: **the strict, guarded surface is
where the fast model is reliable; the permissive surface is where it flails.**
Constraint is not a tax on weak callers — it is their handrail.

And the second observation completes the picture:

> "Surgeon itself consumed roughly 0.09–0.16s per cell; nearly all wall time was
> model-side request construction and recovery."

The server is never the bottleneck. The caller's typing is. Which is the read/write
economics of the whole program, seen from a new caller's seat.

## Why this mattered the same afternoon

Hours earlier, Gene ratified the prepared-request recovery slice ("Wow!!! Love it!
Go!") on a twice-replicated claim: server-built templates with caller-owned holes
delete the call-assembly error class. The open question underneath it was the
weak-caller hypothesis — could strict templates plus complete refusals make cheap,
fast models reliable Surgeon drivers, so judgment models decide and cheap models
execute?

Spark's profile is the first supporting datum: flawless at filling constrained forms
(3/3 writes, 3/3 one-turn recoveries), unreliable at freeform construction (0/3
one-shot reads, hallucinated fields). That is *exactly* the caller the prepared request
serves. The LLD's Spark-class safety strata — specced before this screen reported —
now have a measured subject.

## Footnotes for the record

- The load-sensitive cold-admission flake bit this lane's suite run too (2 failures,
  exact rerun green) — the third sighting in two days. SURGEON2's deterministic fix
  (`fix/cold-verifier-deterministic-clock-20260830` @ 7dccc39) waits on the release
  train and is earning its keep before it ships.
- Aliases failing until the canonical `gpt-5.3-codex-spark` was found is a tiny
  instance of the day's theme: names are load-bearing; guesses about them are not.
