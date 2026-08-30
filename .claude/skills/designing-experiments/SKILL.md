---
name: designing-experiments
description: Fire before designing, running, or interpreting any experiment, screen, or benchmark in this repo — routing studies, performance comparisons, telemetry mining, A/B cohorts. Encodes the earned method: ground in empirical agent usage, preregister with kill criteria, verify the fixture can express the effect, and let only Anvil matched pairs mint performance claims.
---

# Designing experiments like a scientist

Every rule here was earned in the 2026-08-28..30 program: 12 surviving measurements,
10+ preregistered kills, 7 withdrawn single-source figures, two ceiling instruments
caught before wasted spend, and one sign-flipping effect that would have shipped as a
product thesis. The method below is what separated the survivors from the withdrawn.

## 1. Ground in empirical agent usage FIRST

An experiment starts from what agents actually did, not from what seems plausible.
Before designing, mine the retained telemetry (the `study-agent-usage` tooling and its
privacy contract) and the actuals: firing rates, refusal rates, task-class
distributions, recovery costs. A hypothesis with no usage footprint gets a zero-model
trace study before it gets a cohort.

- **State REACH**: what measured share of real usage does this lever touch? A lever on
  a 0.5%-share path cannot buy fleet seconds no matter how large its per-call effect.
- **Pick the granularity that matches the unit of value.** Measured per native ACTION,
  81% of external writes looked small; per TASK TURN, 4 of 9 reached ≥15 hunks.
  Surgeon's unit is the decision — action-granularity data made the winnable market
  invisible. Choose the row unit before counting, and justify it.
- **Check circularity.** This repo's own history is saturated with its own research; a
  rate measured here may not survive contact with external corpora (the 6.0% → 44.74%
  addressable repricing). Repository-stratify or label the boundary.

## 2. The evidence ladder — and Anvil is the acid test

1. **Zero-model trace study** on retained telemetry: cheapest, kills weak hypotheses
   free (three turn-mining hypotheses died in one afternoon for $0).
2. **Synthetic screen** (small n, frozen fixture): buys an option; may KILL a claim,
   may never mint one.
3. **Replication** (n≥10/arm, fresh fixture): required before any build rests on a
   screen's number.
4. **The acid test — a matched serial Anvil comparison at an exact product commit, on a
   real historical decision, same semantic scorer in both arms.** This is the ONLY rung
   that mints a performance claim (CLAUDE.md § The acid test). Report per-arm wall
   seconds, emitted output tokens, action count, one-shot rate, speedup, task class.

Never skip rungs upward; skipping downward (an acid test without a screen) merely
wastes Anvil time, not truth.

## 3. Preregister, freeze, and let the gate refuse you

- Commit the full protocol — fixtures, arms, schedule, scorer, counting rules —
  **before the first model call**. Register the prediction WITH a magnitude and a kill
  criterion. After launch, nothing is tuned: not the prompt, not the scorer, not the
  classification. A failed run is retained and scored, never rewritten.
- **Verify the fixture can express the effect.** Pilot the control arm first; if the
  control is at ceiling (10/10, 4/4), the instrument cannot move and the cohort must
  not spend. Two ceilings were caught in one day by preregistered sub-ceiling gates —
  each looked like a result and was actually a broken instrument.
- Predeclare validity fields (environment_valid, semantic_correct, route_adherent) and
  report wrong-subject explicitly; it must be 0 or loudly explained.
- Small n with a registered power bound is honest; a null at small n is NEVER reported
  as equivalence — state the interval.

## 4. Interpreting results without fooling yourself

- **Sign instability across fixtures means the effect is not real.** +50pp → 0pp →
  −50pp is not "mixed evidence" to average; it is a verdict. Close the claim.
- **Secondaries cannot rescue a failed primary** — but a secondary that replicates
  independently in the same direction (recovery: −47% then −30%) may open a NEW
  forward-only gate with its own preregistration. It never rescores the closed cohort.
- **Every verdict carries a price model, named in one line.** Raw token counts mislead:
  input (prefill, ~72,529 tok/s, cacheable) and output (decode, ~56.5 tok/s) differ
  ~1,284×. Fold facts into verdicts with the measured asymmetry or the fold is wrong.
- **A single-source number is a hypothesis wearing a decimal point.** No magnitude is
  relayed as fact until reproduced or receipted; seven figures died in one night for
  violating this.

## 5. Receipts, delivery, and the graveyard

- Retain raw streams, per-episode artifacts, SHA-256 manifests, and one replay command.
  Commit and push after each completed screen — a lane's "committed" is not
  "delivered"; harvests verify the SHA at origin.
- **Negative results are purchases.** Record kills in the graveyard with their evidence
  grade and the registered reopening condition. Reopening requires a NEW
  preregistration — a stopped cohort's data may never be relabeled.
- When an acid test moves the performance map, the routing language agents actually
  read (`skill.md`, installed agent-routing text) is rewritten in the same change —
  the skill text is the actuator; an unshipped boundary bought nothing.

## Self-check before launching any cohort

1. What real usage data says this lever touches real traffic — and at which granularity?
2. Is the protocol frozen and committed, with magnitude prediction and kill criterion?
3. Did the control pilot prove the fixture is sub-ceiling — can the instrument move?
4. Which rung of the ladder is this, and am I about to claim one rung higher than I ran?
5. What price model will the verdict fold use, and can I name it in one line?
