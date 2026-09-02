# Captain's log — the predictions that failed, and what that teaches

Date: 2026-08-29
Seat: mayor@skiff
Written because Gene asked for the failure of predictions to be recorded, not just the failure of
designs. The designs are in the companion logs; this one is about the forecasts.

**Tonight's discipline was to register a number BEFORE running.** That worked. It also produced a
scoreboard nobody has looked at: **how good were the predictions?**

## The scoreboard

**Roughly 4 held. Roughly 9 failed.** And they failed with a pattern.

### Held

| prediction | outcome |
|---|---|
| copyable share of write bytes >= 60% | **83.6%** |
| refusal frequency >= 15% of missions | **68.4% — 4x the prediction** |
| the emission model TRANSFERS to production (R^2 > 0.9) | **R^2 = 0.9807, n=59, 76.3x spread** |
| refusal-retry payloads >= 50% similar | **57.8% at >=95% byte overlap, median 99.6%** |

### Failed

| prediction | predicted | measured |
|---|---|---|
| addressable market >= 50% | >=50% | **23.6%** |
| emission rate | ~6 ms/char | **3.5237 ms/byte** — everything rescaled by 0.59 |
| EDN token saving | **-14% writes / -11% reads** | **+1.4% / +12.3% — WRONG SIGN** |
| EDN fanout saving non-negative in every stratum | non-negative | **negative at 3-5 and 6+ forms** |
| declared-intent net saving | **+850 B** | **-1,626 B — WRONG SIGN, +56.6% growth** |
| backslash depth >=4 discriminates failure | discriminates | **clean depths all 9; corrupt [7,9] and [5,5,5]** |
| parse gate catches >=30 of 42 loops | >=30 | **0 of 8 genuine** |
| copy is cheaper than compose (hoped 3-5x) | a discount | **0.96x — no discount, slightly slower** |
| doctrine attribution | 55% | **45.7%** (above the 35% floor, below the estimate) |

## The pattern, and it is the finding

**Predictions about WHETHER A PHENOMENON EXISTS held. Predictions about MAGNITUDE or SIGN failed.**

Every "is this real?" forecast landed: refusals are frequent, payloads are copyable, retries repeat
themselves, the emission model transfers. **Every "how big, and which direction?" forecast missed —
and three of them missed the SIGN, not merely the size.**

EDN was predicted to save 14% of tokens and cost 1.4%. Declared-intent was predicted to save 850
bytes and cost 1,626. Copying was predicted to be 3-5x cheaper and came back 4% more expensive.

**Three sign errors in one day, by careful reasoners with the full corpus in front of them.**

## Why the failures were worth more than the successes

**A registered failed prediction kills a design. An unregistered wrong guess is noise.**

- The backslash-depth failure **cancelled the carrier experiment** before it ran.
- The parse-gate failure **stopped a gate that would have sat on all 463 native writes** to catch
  zero real failures.
- The EDN sign error **stopped a format migration** touching every caller, test, and doc.
- The declared-intent sign error **stopped a guard** whose own author had ruled it SHIP-MODIFIED an
  hour earlier.

**Four builds prevented, at the cost of writing a number down first.**

And one reviewer said it about itself, unprompted, while ranking its own proposals:

> **"This is the only item on the board whose number came from measurement rather than my
> prediction — and my predictions are 0-for-3."**

**It then ranked that item ABOVE its own top pick on expected value times confidence.** That is the
behaviour the practice is for.

## The relationship to the six withdrawals

These are two different failure modes and they should not be conflated.

**A WITHDRAWAL is a measurement that was wrong** — six of them, all single-source, all relayed as
fact before being checked.

**A FAILED PREDICTION is a forecast that was wrong** — nine of them, all registered in advance, all
reported as failed rather than quietly loosened.

**The withdrawals were failures of verification. The failed predictions were the system working.**
The difference is entirely whether the number was written down before or after the data arrived.

## What this earns

1. **Predict EXISTENCE, not MAGNITUDE — and if you must predict a magnitude, predict the SIGN
   explicitly and separately.** Three sign errors in one day says the sign is the part we are worst
   at and the part we assume hardest.
2. **A prediction without a kill criterion is a wish.** Every kill tonight fired because the
   threshold was named first. The one prediction reported as "partial" (attribution at 45.7%) was
   useful precisely because it had BOTH a floor (35%) and an estimate (55%) — it landed between
   them, and that gap was informative.
3. **Report a failed prediction as failed. Never loosen the threshold after seeing the data.** This
   happened twice tonight and both times it killed something expensive. A prediction that survives
   only after adjustment is not evidence.
4. **Track the forecaster's record and use it as a weight.** A reasoner that is 0-for-3 on
   magnitudes should have its magnitude claims discounted and its existence claims trusted — and
   should say so about itself, as one did.
