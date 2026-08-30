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

---

# Appendix A — the measurement, made

Date: 2026-08-29. Added by `opus-bench`. **Append-only: nothing above this line was changed.**

The section above ends by naming the experiment we had not run. We ran it.

## What was measured, and where

| | |
|---|---|
| Machine | Anvil `dev-a` (`anvil-server`), 16 cores, Linux 7.0.0-22-generic x86_64 |
| Load during the run | 1.00–1.34 one-minute average, on 16 cores — an idle box |
| Route | `codex exec` on the ChatGPT subscription. No `OPENAI_API_KEY` exists in this profile |
| Model | `gpt-5.6-sol`, reasoning effort `low`, codex-cli 0.147.0 |
| Replicates | **n = 9 per condition**, run in an interleaved C/A/B rotation so drift lands on every condition equally |
| Token counts | The provider's own usage report, not a local tokenizer. **Tokens, not characters.** |
| Probe | `bench/measure_prefill_decode_ratio.sh` |
| Fold | `bench/score_prefill_decode_ratio.clj` |
| Evidence | `bench/results/2026-08-29-prefill-decode-ratio/` |

The environment is recorded because it is evidence, not a footnote. A load of 664 on a laptop
invalidated a gate earlier in this project's life.

## The three conditions

Two conditions cannot separate the fixed per-turn floor from prefill, so we ran three.

| Condition | Prompt | Output | n | Median wall | MAD | Min | Max |
|---|---|---|---|---|---|---|---|
| **A** large read | ~1,048,000 chars | 1 token | 9 | **6,954 ms** | 441 ms | 6,513 ms | 9,521 ms |
| **B** large write | 53 chars | 1,239 tokens | 9 | **25,781 ms** | 264 ms | 25,303 ms | 28,636 ms |
| **C** floor | 53 chars | 1 token | 9 | **3,927 ms** | 280 ms | 3,285 ms | 5,752 ms |

Spread is reported as **median absolute deviation**, which one slow trial cannot inflate. Every
condition-A replicate used a **freshly randomised** filler payload — all nine payload hashes are
distinct, and reported `cached_input_tokens` never rose above 14,080 against 233,949 input tokens.
The provider's prefix cache was genuinely defeated; we timed prefill, not a cache lookup.

## THE FLOOR — 3.927 s, and it is the most important number here

**A turn that reads nothing and writes nothing costs 3.927 s (MAD 280 ms, n = 9).**

Of that, **321 ms** is local: process start, config load, session setup, measured from process
launch to the `turn.started` event. **The remaining ~3.6 s is network and server-side and we cannot
reach into it.**

Loading this project's real config, including the clj-surgeon MCP server, raises the floor to
**4,417 ms** (MAD 463 ms, n = 5) — **+490 ms per turn just to have the tools declared.**

This is a floor *underneath* the ~11 s floor recorded earlier in this document. The two are not in
conflict and neither supersedes the other. The 11 s figure came from real working turns carrying
tool round-trips and high reasoning effort; 3.927 s is what remains when all of that is stripped
away. **It is the irreducible per-turn tax, and no amount of payload shrinking will ever touch it.**

## PREFILL — 72,529 tokens/second

```
marginal input tokens   219,544   (233,949 in condition A  -  14,405 in condition C)
delta                     3,027 ms  (6,954  -  3,927)
prefill rate             72,529 tokens/second
```

**Corrected for network.** Condition A ships ~1 MB and condition C ships ~1 KB, so the delta
contains upload time. A control POSTing the identical 1 MB to the provider's edge with an invalid
credential — body uploaded, then rejected, no model invoked — round-trips in **163 ms median**
(max 349 ms, n = 5). That is **5.4% of the delta**. Subtracting it gives **76,657 tokens/second**.
Because the correction can only make prefill look faster, the uncorrected 72,529 is a **lower bound**.

**And it is linear.** The obvious objection is that a million-character prompt is a special case.
It is not. Four input sizes, each against its own contemporaneous floor:

| Marginal input tokens | Delta | Rate | Resolved above noise? |
|---|---|---|---|
| 27,458 | 676 ms | 67,299 tok/s | no — within the noise floor |
| 54,857 | 1,082 ms | 50,700 tok/s | yes |
| 109,706 | 677 ms | 65,146 tok/s | no — within the noise floor |
| 219,544 | 3,027 ms | 72,529 tok/s | yes (n = 9) |

The rate holds between **50,700 and 72,529 tok/s** across an eight-fold size range. Take **~65,000
tok/s** as the central estimate. Note the two "no" rows: **at 27k and even at 110k marginal input
tokens the cost of reading is still not distinguishable from the noise in the floor.** That is the
BOUNDED result from the body of this document, reproduced deliberately and at fourteen times the
scale — 14,706 characters was never going to show up.

## DECODE — 56.5 tokens/second

```
marginal decoded tokens     1,234   (1,239 in condition B  -  5 in condition C)
delta                      21,854 ms  (25,781  -  3,927)
decode rate                  56.5 tokens/second
```

Reasoning tokens are counted as decode. They are produced serially through the same forward pass
and cost the same time whether or not anyone sees them.

**We deliberately do not convert this to milliseconds-per-character.** Condition B emits an integer
sequence, which tokenises at ~1.87 chars/token against roughly 4 for prose or code. The
character-level figure in the body of this document (~6 ms/char, ~40 tok/s) came from a different
route and a different model; **56.5 tok/s is the same order of magnitude, which is all that should
be claimed.**

## THE RATIO — ~1,000×, an order of magnitude above what we were repeating

```
prefill 72,529 tok/s  /  decode 56.5 tok/s  =  1,284x
```

Using the slowest observed prefill sample instead, the conservative floor is **897×**. The honest
range is **900–1,300×; call it 10³.**

**The 50–100× figure we were repeating is wrong for this route — low by roughly an order of
magnitude.** It should be retired here, not because published figures are bad, but because they
average over hardware, models, batch regimes, and serving stacks that are not ours. This is the
whole reason the house rule separates a remembered number from a measured one.

## What this changes

Gene asked for this because *"it helps us understand the constraint we're optimizing for."* Here is
the constraint, in the two forms that are actually usable at a design desk.

**1. One output token costs about as much time as a thousand input tokens.** The question posed at
the end of the body — *at 10×, does a result that triples in size start to cost something?* — is
answered. **No.** Tripling a 500-token result adds 1,000 input tokens, which costs **15
milliseconds**. Read-time labels, richer receipts, explicit refusals and named identity do not need
a size budget. **The blank cheque is justified.** Rule 1 in "What follows for design" stands, and
now stands on a measurement.

**2. The floor is worth a quarter of a million input tokens.** At these rates:

| The 3.927 s floor equals | |
|---|---|
| ~222 output tokens | writing is what a turn is worth |
| ~255,000 input tokens | reading is essentially free by comparison |

**So the ranking of optimisations is fixed by arithmetic, not by taste:**

1. **Remove a turn.** Worth 3.9 s. Nothing else comes close.
2. **Remove output tokens.** Worth ~17.7 ms each.
3. **Remove input tokens.** Worth ~0.015 ms each — **1,000× less than output, and 260,000 input
   tokens less than a single turn.**

**Never spend a turn to save input. Never spend output to save input.** Any design that adds a
round-trip in order to hand the model less to read is losing by three orders of magnitude, and
three separate optimisations aimed at the wrong constraint today would have been caught by
multiplying two numbers from this table.

The closing line of the body predicted this: *"as hardware improves, turns matter more and
characters matter less."* The measurement says the future arrived already — **one turn costs what
~222 output tokens cost, and what ~255,000 input tokens cost.**

## What this measurement CANNOT separate

Stated plainly, because a ratio with unnamed confounds is worth less than a ratio with named ones.

1. **Server-side queueing and batching.** Invisible from the client. A turn may wait behind other
   tenants; that time lands in the floor and in both deltas. This is the largest unbounded
   confound, and it is the most likely explanation for the 9,521 ms and 5,752 ms outliers.
2. **Network, bounded but not eliminated.** Measured at 163 ms for 1 MB and subtracted as an upper
   bound, but the control hits `api.openai.com` while the trials hit the subscription backend. Same
   provider, possibly not the same path.
3. **Attention shape at long context.** We measured wall-clock cost per token, not work done per
   token. If the serving stack uses sparse or chunked attention past some length, "tokens read per
   second" is a throughput observation, not a claim about computation. The linearity across four
   sizes argues against a sharp regime change, but cannot rule one out.
4. **`codex exec` is an agent wrapper, not a raw completion.** The floor includes CLI startup and
   session setup that a direct API call would not pay. It is the right floor for *this fleet*,
   which reaches models through exactly this wrapper, and the wrong floor for anyone else.
5. **Provider-reported token counts are taken on trust.** They are authoritative for billing; we
   assume they are authoritative for work.
6. **One model, one provider, one datacentre, one hour.** Nothing here generalises to Claude, to
   another region, or to this same model under load. **Re-run the probe rather than quoting these
   numbers at a different seat.**
7. **Reasoning effort was pinned to `low`.** Production work runs `high`. That changes decode
   volume — and therefore turn cost — enormously. It does not change the *rates* measured here,
   but it means a real turn costs far more than 3.927 s.

## Confidence, moved

**MEASURED, ours, as of 2026-08-29.** Prefill **~65,000 tok/s** (range 50,700–72,529), decode
**56.5 tok/s**, ratio **~1,000×** (range 900–1,300), fixed per-turn floor **3.927 s** clean and
**4.417 s** with this project's MCP config loaded. n = 9 per condition, medians with MAD spread,
on `gpt-5.6-sol` via `codex exec` on Anvil dev-a.

The "REPEATED, not ours, moderate confidence — 50–100×" claim in the body is **superseded for this
route**. Left in place above, as the house rule requires, so the correction stays legible.

Re-run with:

```bash
bench/measure_prefill_decode_ratio.sh OUT_DIR          # probe: emits facts
bb bench/score_prefill_decode_ratio.clj OUT_DIR --markdown   # fold: emits verdicts
```
