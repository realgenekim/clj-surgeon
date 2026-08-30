# Why reading is cheap and writing is expensive

Date: 2026-08-29

Written for anyone who needs to know *why* this project's measurements came out the way they
did, and therefore which design moves will keep paying.

## The one-line answer

**A model reads far faster than it writes — plausibly 50 to 100 times faster.** The two are not
the same operation, and the gap is architectural rather than an implementation detail.

## They are structurally different operations

**Reading — often called *prefill* — happens all at once.** The model takes the whole prompt and
processes every token in parallel, in one wide matrix operation where each token attends to all
the others simultaneously. A 10,000-token prompt is not 10,000 steps. It is closer to one wide
step, and that is the shape of math accelerators are built for.

**Writing — *decode* — happens one token at a time and cannot be parallelised.** To produce token
N+1 the model must first have produced token N. That dependency is inherent to generating text.
Every single token requires a complete forward pass through the entire model.

## The bottleneck differs, and that is the real story

**Reading is limited by compute** — operations per second. That is what the hardware is best at.

**Writing is limited by memory bandwidth.** For each token the model streams its entire weight set
from memory into the compute units. For a large model that is hundreds of gigabytes of memory
traffic *per token*, and the chip spends most of that time waiting for data rather than
calculating.

So writing is not slow because the arithmetic is harder. **It is slow because you must haul the
whole model through memory once per token, sequentially, and you cannot skip ahead.**

## The ratio: what we measured, and what we are repeating

Keep these two apart. This project's house rule is that a remembered figure and a measured one do
not get the same confidence.

**MEASURED, ours.** Emission costs **~6 ms per authored character**, which is roughly **40
tokens/second**. Output-token count predicted emission time at **R² = 0.99886** — predicted delta
21.417 s against observed 21.453 s. This is the firmest number in the corpus.

**BOUNDED, ours.** We never timed reading directly. What we have is a bound: deleting **14,706
characters** of client-visible input (23,096 → 8,390 bytes) produced **no detectable speedup** —
pre-first-call time actually rose 5.2%. So processing ~14,700 characters of input sits **below our
noise floor**. The same 14,706 characters as *output* would cost roughly **88 seconds**.

That is a large lower bound on the ratio, but it is not a clean measurement — the experiment was
designed to test schema size, not to time prefill.

**REPEATED, not ours, moderate confidence.** Published figures commonly put single-stream prefill
in the low thousands to tens of thousands of tokens/second and decode in the tens to low hundreds,
which implies a ratio around **50–100×** with wide variance by model, hardware, and batching.
**Treat this as an order of magnitude, not a spec.**

## How this explains our own results

| Change | Direction | Result |
|---|---|---|
| Deleted 63.7% of client-visible tool bytes (input) | less to **read** | 5.2% faster overall, and **5.2% slower** before the first call |
| Closed relations: 6,509 → 2,871 request chars (output) | less to **write** | **34% faster** verified completion |

The asymmetry shows up exactly where the architecture says it should. **Reading was never the
cost.** Every win this project has recorded came from reducing what the model *authors*.

It also explains the surprise in the promoted result: **recorded reasoning tokens went *up* in the
faster arm while visible output fell 51%.** The model did not think less. It typed less, and
typing is the expensive half.

## What follows for design

1. **Enrich results freely.** Receipts, refusals, labels, and read output are prefill. Making them
   richer is close to free, and it is the cheapest lever available.
2. **Shrink requests hard.** Every character the model authors costs ~6 ms.
3. **Never trade identity for brevity.** Replacing file paths with numeric indices cut payload
   23.67% and silently mutated the *wrong file*, returning `ok=true`. The cheapness of input is
   what lets us keep identity explicit and still emit little — spend input to protect the subject.
4. **A novel grammar can cost more than it saves.** One arm was smaller *and* slower (83.703 s vs
   65.841 s). Bytes predict only within a vocabulary the model already reads fluently.

## What will change, and what will not

**The gap will narrow. It will not invert.** Speculative decoding, better batching, and faster
memory all attack decode — but the serial dependency is fundamental. You cannot produce the fifth
token before the fourth.

**And the ~11-second per-turn floor is neither prefill nor decode.** It is scheduling, queueing,
and request setup. That is why handing the model a fully pre-composed 608-byte call saved only
**2.205 s**. Faster inference shrinks the decode term and leaves the floor untouched — **so as
hardware improves, turns matter more and characters matter less.**

## The measurement we have not made

Two calls would settle the ratio for the exact models and hardware we run on: one with a large
prompt and a one-token output, one with a small prompt and a large output. Time both.

It is not academic. **The whole "input is free, output is expensive" strategy rests on this
number.** At 100× the case for enriching results is obvious. **At 10×, a result that triples in
size starts to cost something**, and ideas like read-time labels need a size budget rather than a
blank cheque.

Related: `docs/observations/2026-08-29-captains-log-the-model-typed-less-it-did-not-think-less.md`,
`docs/observations/2026-08-29-post-surgeon-boundary-decomposition.md`,
`docs/observations/2026-08-29-captains-log-two-seconds-were-call-construction-eleven-were-not.md`,
`docs/observations/2026-08-29-wrong-index-ended-emission-composition.md`.
