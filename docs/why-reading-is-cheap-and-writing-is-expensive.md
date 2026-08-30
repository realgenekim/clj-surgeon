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

---

# Appendix B — copying is not cheaper than composing, and transcription did not drift

Date: 2026-08-29. Added by `opus-bench`. **Append-only: Appendix A and everything above it are unchanged.**

Appendix A measured how fast the model writes. It did not ask **what** it was writing. Every
emission number this project owns — the 56.5 tok/s above, the R² = 0.99886 linearity, the
~6 ms/char constant — was measured on **composed** output: text the model constructed. Nothing had
measured **transcription**: the model reproducing text sitting in its immediately preceding input.

That gap sat directly under the design then converging, in which the server pre-composes complete
candidate calls (free, at 15 µs/token) and the model accepts one by **echoing the candidate's
subject verbatim**. Two separate assumptions were load-bearing and neither had been tested:

1. **Speed.** If serving stacks accept long verbatim runs at a multiple of base decode rate —
   speculative decoding does exactly this — echoing an entire pre-composed call would cost little
   more than emitting a bare ordinal, and the safety-versus-brevity tension would mostly dissolve.
2. **Accuracy.** That a model asked to copy an identifier reproduces it **exactly**. If
   transcription is even slightly lossy, the design's central claim inverts: a garbled subject is a
   **wrong subject**, produced by the mechanism adopted to prevent wrong subjects.

Gene, on why both got measured rather than argued: *"Don't let logic or interpretation deter you /
them from firing off a cheap test and experiment to prove or disprove."*

## B.1 — Speed: copy versus compose

Same protocol as Appendix A. Anvil dev-a, `gpt-5.6-sol`, reasoning `low`, subscription route,
**n = 9 per condition**, interleaved C/B/D/E rotation. Probe and fold are the same committed
harness, extended with two conditions.

| Cond | Kind | n | route-adherent | not reproduced | decoded tok | delta over floor | tok/s |
|---|---|---|---|---|---|---|---|
| **B** | compose (constructs the sequence) | 9 | 1.00 | 0 | 1,234 | 21,888 ms | **56.4** |
| **D** | copy, unpredictable (random word block) | 9 | 1.00 | 0 | 1,228 | 22,757 ms | **54.0** |
| **E** | copy, same content as B (the sequence, supplied) | 9 | 1.00 | 0 | 1,198 | 22,167 ms | **54.0** |

Floor for this run: **3,854 ms** (MAD 357, n = 9) — within noise of Appendix A's 3,927 ms, measured
a few hours later. Token counts across B, D and E agree within 3%, so this compares **rate, not
volume**, and that was verified from the provider's usage report rather than estimated.

Three conditions rather than two, for the same reason Appendix A used three: D versus B changes both
the operation and the content, so on its own it cannot say which mattered. **E holds the content
fixed** — it supplies the identical integer sequence B composes — and varies only whether the model
had to construct it.

```
copy(unpredictable) / compose   = 0.96x
copy(same content)  / compose   = 0.96x
copy(unpredictable) / copy(predictable) = 1.00x
```

**There is no copy discount. Transcription runs at 0.96× composition — if anything marginally
slower, and well inside the spread.** Supplying the exact answer in the prompt did not speed up
emitting it. Predictability did not matter either: reproducing random words cost the same as
reproducing an integer sequence.

**What this settles.** The hypothesised 3–5× copy discount does not exist on this route. Whatever
speculative decoding is or is not doing here, it is **not** visible as a throughput gain the design
can spend. **The Appendix A numbers stand unmodified, and they apply to echoed output exactly as
they apply to authored output.** An echoed 500-token call costs the same ~8.9 s as a composed one.
Echoing a pre-composed call is a **safety** argument, not an economy one, and it must be justified
on those terms — the cost model does not subsidise it.

This is a loss for the leading proposal and it stays in the chart. Predeclared and reported: all 36
trials were route-adherent, and **all 27 emissions in B, D and E reproduced their expected text
byte-for-byte**, so no trial was excluded and no comparison rests on a truncated output.

## B.2 — Accuracy: does the model copy an identifier exactly?

A separate probe, because accuracy is not timing: `bench/measure_transcription_fidelity.clj` and
`bench/score_transcription_fidelity.clj`.

**The corpus is adversarial on purpose.** Easy names would have measured nothing. 24 identifiers in
the proposed `path#name` shape, built from six confusability traps:

| Trap | Example pair |
|---|---|
| near-identical file, same function | `views/review.clj#render-review` vs `views/reviews.clj#render-review` |
| underscore vs hyphen (the Clojure file/namespace trap) | `db/review_queries.clj#fetch-review` vs `#fetch_review` vs `review-queries.clj` |
| trailing punctuation only | `validate.clj#valid-review` vs `#valid-review?` vs `#valid-review!` |
| case only | `util/HTTPClient.clj` vs `util/HttpClient.clj` |
| long shared prefix, discriminator deep in the string | `..._summary_builder.clj#build-quarterly-summary` vs `#build-quarterly-summaries` vs `_builders.clj#...` |
| digit confusables | `v2_add_index.clj#migrate-v2` vs `v21_add_index.clj#migrate-v21` vs `v2_add_indexes.clj#migrate-v2` |

Three arms, n = 9, candidate order reshuffled every replicate so no single lucky layout could stand
in for the result:

- **F** — reproduce all 24 identifiers in order. Raw transcription fidelity.
- **S-echo** — select by description, answer with the **full identifier**.
- **S-ord** — select by description, answer with the **item number**.

S-echo versus S-ord is the design question stated exactly: same block, same selection task, same
difficulty, differing **only in how the chosen subject is encoded on the way out**.

| Arm | trials | route-adherent | answers | exact | exact rate | VALID-OTHER (dangerous) | garbage (safe) |
|---|---|---|---|---|---|---|---|
| F | 9/9 | 1.00 | 216 | 216 | **100%** | 0 | 0 |
| S-echo | 9/9 | 1.00 | 54 | 54 | **100%** | 0 | 0 |
| S-ord | 9/9 | 1.00 | 54 | 54 | **100%** | 0 | 0 |

**324 answers, zero errors, zero format failures.** Errors did not cluster on the near-identical
pairs because there were no errors: **130 of those answers had a target exactly one character from
a sibling** (`d=1`), and every one came back byte-exact.

| Characters separating target from nearest sibling | answers | errors |
|---|---|---|
| 1 | 130 | 0 |
| 2 | 31 | 0 |
| 3 | 35 | 0 |
| 11–19 | 74 | 0 |

**The assumption holds. Transcription is not lossy on this corpus and this model.** The specific
fear — that echoing an identifier would silently produce a *different valid identifier* — did not
materialise once.

### The caveat that matters more than the table

**Zero observed is not zero.** With no failures, the honest statement is an upper bound, not a
point estimate. By the rule of three, at 95% confidence:

| Arm | 0 errors in | true error rate bounded below |
|---|---|---|
| F | 216 | **1.39%** |
| S-echo | 54 | **5.56%** |
| S-ord | 54 | **5.56%** |

So this run supports *"identifier transcription is reliable"* and **cannot** support *"the
wrong-subject rate is under 1%"* for the selection path specifically. **If the design's safety case
needs a wrong-subject rate below ~1%, this experiment does not establish it** — that needs roughly
300 selection answers per arm, which is another twenty minutes on the same rig. Worth doing before
anyone leans on the number.

### Why the ordinal arm cannot be ranked by its accuracy alone

Both encodings scored 100%, so on this corpus they are **indistinguishable on accuracy**. Fable and
Sol both refused ordinals by argument; the measurement neither confirms nor refutes them, and that
is worth saying plainly rather than reading a preference into a tie.

But the tie hides a structural asymmetry the fold makes explicit, and it is the reason a raw
accuracy comparison between the two would have been misleading even had it separated them:

- **A mistyped identifier is usually GARBAGE.** It names no candidate. The server refuses it. Safe,
  loud, recoverable.
- **A wrong ordinal is almost always a VALID OTHER CANDIDATE.** Nearly every way to get a number
  wrong yields another number in range. The server accepts it and mutates the wrong subject,
  returning `ok=true`.

**Ordinal errors land in the dangerous bucket by construction; identifier errors mostly do not.**
Equal error rates therefore do not mean equal risk, and the two encodings cannot be ranked on a
headline accuracy number. This is the same wrong-index failure recorded in the body of this document
— *"replacing file paths with numeric indices cut payload 23.67% and silently mutated the wrong
file, returning `ok=true`"* — and the measurement is consistent with design rule 3 above: **never
trade identity for brevity.** Appendix A already showed the trade buys nothing worth having; input
is 1,000× cheaper than output, so identity is close to free to carry.

## What Appendix B changes

1. **The copy discount does not exist.** Delete it from any cost model that assumed it. Echoed
   output is priced exactly like authored output: **~17.7 ms per token, 0.96× compose.**
2. **Identity-echo survives on safety, not on price.** It is not nearly-free relative to an ordinal;
   it costs full freight per token. It is still the right choice, because Appendix A shows input is
   1,000× cheaper than output and because ordinal errors are structurally silent while transcription
   errors are structurally loud — but the argument is safety and failure-kind, never throughput.
3. **Shrinking what the model authors remains the only emission lever that works.** Nothing about
   how the text is derived changes its cost. Only its length does.

## What B cannot separate

1. **A 24-candidate block.** Confusability grows with catalogue size; this says nothing about 500
   candidates.
2. **One model, one reasoning effort, one hour.** A cheaper model, a longer context, or a loaded
   server could all transcribe worse. Re-run rather than quote.
3. **Selection difficulty is controlled but not realistic** — the descriptions were written by the
   same author as the corpus, so real ambiguity is understated.
4. **Byte equality after trimming is the whole test.** A downstream resolver that normalises case or
   separators would mask errors this counts — and would introduce wrong-subject risk this does not
   measure.
5. **The speed arms cannot see server-side batching or speculative decoding directly.** They observe
   only that no throughput advantage reached the client. A discount that exists but is not passed
   through is indistinguishable, from here, from one that does not exist — and for design purposes
   they are the same thing.

## Confidence

**MEASURED, ours, 2026-08-29.** Copy/compose emission ratio **0.96×** (n = 9 per condition, token
counts matched within 3%, all emissions byte-exact). Identifier transcription fidelity **324/324
exact** across three arms on an adversarial 24-identifier corpus, with the true error rate bounded
below **1.39%** (F) and **5.56%** (selection arms) at 95%.

Re-run with:

```bash
RATIO_CONDITIONS="C B D E" RATIO_COPY_WORDS=1229 \
  bench/measure_prefill_decode_ratio.sh OUT_DIR
bb bench/score_prefill_decode_ratio.clj OUT_DIR --markdown

bb bench/measure_transcription_fidelity.clj OUT_DIR 9
bb bench/score_transcription_fidelity.clj OUT_DIR --markdown
```

---

# Appendix C — X5: JSON versus EDN as the request carriage

Date: 2026-08-29. Added by `opus-bench`. **Append-only: the body, Appendix A and Appendix B are unchanged.**

Gene, on seeing the byte case: *"From JSON to EDN. Wow. Seems like total no brainer!! (Might be
less training data, but I can't imagine LLMs screwing up EDN. I've never seen that in the wild!)"*
And on what would decide it: *"Writes are expensive for now. I just don't see a good objection for
not doing this, unless writing EDN is unacceptably error prone."*

So the malformed-request rate is the decision. Four things were measured. **One of them changes the
question, and it is not the rate.**

## C.0 — The confound, measured first, because it governs how every rate must be read

MCP tool arguments are JSON, and providers commonly enforce the tool's JSON schema with
**constrained decoding** — the model is not merely unlikely to emit malformed arguments, it is
**prevented** at the sampler. Put EDN inside a single JSON string and the payload leaves that
guarantee: the schema can then only assert that a string is a string.

If that is live on this route, a JSON arm scoring zero would measure **the decoder, not the model**.
So it was established empirically rather than from documentation. Three probes, n = 5 each,
**unanimous 5/5 on all three**:

| Probe | What was asked | Result |
|---|---|---|
| **P1** | With `--output-schema` requiring `{"value": <integer>}`, emit `"value": "not-a-number"` **plus** an extra key | **Schema enforced, 5/5** |
| **P2** | With no schema, emit deliberately invalid JSON | **Permitted, 5/5** |
| **P3** | With no schema, emit deliberately invalid EDN | **Permitted, 5/5** |

P1 is unambiguous. Instructed explicitly to emit a string and an extra key, the platform returned
`{"value":0}` — an integer, no extra key, every time. Without a schema it emitted the invalid JSON
*and* the invalid EDN verbatim, exactly as asked.

**Verdict: PROTECTED-WHEN-SCHEMA-SUPPLIED.** This route does apply structural enforcement to
schema-bearing output and does not otherwise.

**What follows is decisive and no rate can overturn it.** Production tool-call JSON is
**structurally incapable** of being malformed — its rate is not low, it is *enforced to zero*. EDN
inside a string cannot inherit that. So the honest comparison is not "JSON's error rate versus
EDN's"; it is **"an enforced-zero guarantee versus a rate we would have to police ourselves."**

All three carriage arms below are **free text and therefore equally unguarded**. That is the right
control — it isolates format from protection by removing protection from both — but it means
**none of them is production JSON**, and no arm here should be read as such.

## C.1 — Malformed-request rate: no difference detectable, and n is the limit

10 adversarial fixtures × 3 replicates × 3 arms, counterbalanced, dev-a, strict parsers.

**A measurement bug worth recording:** Cheshire's default factory *accepts* a literal newline inside
a JSON string, which strict JSON forbids. Left on, it would have handed the JSON arm a free pass on
precisely the hazard where EDN is structurally better — EDN strings may contain literal newlines,
JSON strings may not — and biased the study toward "no difference." Every JSON parse here runs with
that leniency disabled.

| Arm | trials | route-adherent | malformed | rate | median out bytes | median out tokens |
|---|---|---|---|---|---|---|
| J (JSON args) | 30 | 1.00 | **0** | 0.0% | 168 | 64 |
| E-wrapped (EDN in a JSON string) | 30 | 1.00 | **0** | 0.0% | 197 | 84 |
| E-raw (bare EDN) | 30 | 1.00 | **0** | 0.0% | 164 | 66 |

**Zero malformed requests anywhere.** Not one fixture — not backslash-heavy regexes, not literal
newlines, not reader conditionals, not a JSON blob embedded in the payload — produced unparseable
output in any carriage. **Gene's field intuition is not contradicted.**

**But this run cannot decide the question, and the reason is n.** By the rule of three, 0 in 30
bounds each true rate only **below 10%**. The decision threshold is around **2 percentage points**.
**A run that can only see 10% cannot resolve 2%.** Resolving that needs roughly 150 trials per arm.
Predeclared prediction (EDN ≤ JSON) held trivially; the kill criterion did not fire; neither fact
carries information at this n. **Reported as "no difference detectable at this n," not as parity.**

## C.2 — Well-formed but wrong: the failure that actually happened

Malformed output is refused loudly and costs a retry turn. **Output that parses cleanly and means
something else is accepted and executes.** These were scored separately, and only the second class
occurred.

**C.2a — E-wrapped has a corruption channel the pure arms do not.** One fixture, one arm:

```
expected:  {:ok "✓ done" :warn "café — retry" :tab "a\tb"}
got:       {:ok "✓ done"      :warn "café — retry"           :tab "a<TAB>b"}
```

The Clojure source legitimately *contains the six characters* `✓`. Passing through **two**
decoders — JSON string, then EDN string — the outer layer consumed the escape and produced the
glyph. It parsed perfectly. **Double encoding is a silent corruption channel, and it belongs
specifically to the deployable shape that was proposed.** E-raw, with one decode, did not have it;
neither did J.

**C.2b — the larger finding: the dangerous failure is carriage-independent.** On a realistic
48-line, 1,546-byte Clojure payload (C.3 below), **3 of 18 write requests silently corrupted the
payload — 2 in JSON, 1 in EDN — and all three corrupted the *same line*:**

```
want:  (str/replace "\\" "\\\\")
got:   (str/replace "\""  "\\\\")
```

The model substituted a quote for a backslash. Well-formed in both carriages. **~17% of realistic
writes silently wrong, and switching carriage does not fix it** — it is an escaping-fidelity
failure, not a format failure. Note the contrast with Appendix B, where 324/324 identifier
transcriptions were byte-exact: **short identifiers transcribe perfectly; long backslash-heavy
bodies do not.**

**This outranks the whole EDN question.** A malformed rate of 0% sat beside a silent-wrong rate near
17%, and only the first one was being argued about.

## C.3 — Generation time versus token count: does the kill stand?

A zero-model screen killed EDN on token **count** (writes +1.389% tokens despite −4.931% bytes;
reads +12.253% tokens). Gene's objection: *"I wonder if EDN/CLJ are activated at the same time, so
maybe it washes out?"* — i.e. a static tokenizer measures count, not **generation time**, and the
payload is Clojure.

Three conditions on the Appendix A protocol, n = 9, both arms carrying **byte-identical real
Clojure**, only the carriage differing:

| Arm | n | median wall | MAD | decoded tok | out bytes |
|---|---|---|---|---|---|
| C floor | 9 | 3,996 ms | 431 | 5 | 2 |
| J | 9 | 14,226 ms | 419 | 590 | 1,755 |
| E | 9 | 15,144 ms | 642 | 650 | 1,751 |

```
J:  585 marginal tokens in 10,230 ms  ->  17.49 ms/token  (57.2 tok/s)
E:  645 marginal tokens in 11,148 ms  ->  17.28 ms/token  (57.9 tok/s)
```

**The two ratios, EDN over JSON, never collapsed:**

| | ratio |
|---|---|
| token count | **1.103** |
| generation time | **1.090** |
| **ms per token** | **0.988** |
| output bytes | 0.998 |

**Time tracks count.** Per-token generation speed is at parity (0.988, a 1.2% difference well inside
the spread), and both arms land on Appendix A's decode rate — 57.2 and 57.9 tok/s against 56.5. EDN
costs ~10% more tokens and therefore ~9% more time. **There is no domain-coherence discount. The
token-count kill stands on generation time as well, and token count was a sound proxy for cost here.**

The stronger finding the coordinator hoped for — that emission has been priced by a proxy twice
over — **did not materialise**. Reported explicitly, as asked, rather than folded into a verdict.

**Honesty on n:** the 918 ms J/E total-time gap does **not** clear the combined spread (2,122 ms), so
the ~9% total-time difference is **not resolved** at n = 9. The robust part is the per-token rate
parity, which is what answers the mechanism question.

**Relation to Appendix B:** B found copy/compose at 0.96× with predictability making no difference,
but measured it on integer sequences. C.3 **extends that null to real Clojure content in both arms**
— no coherence discount there either. The two results agree; C.3 closes the domain gap B left open.

## What X5 concludes

1. **The byte and token case is dead.** EDN costs ~10% more tokens on this payload, ~9% more time,
   and the per-token rate is identical. Nothing here rescues it.
2. **The robustness case is unproven, not disproven.** Zero malformed in 30 per arm; the true rates
   are bounded only below 10% and the decision needs 2%. **~150 trials per arm would settle it.**
3. **The guarantee is the real cost, and it is not a rate.** Production tool-call JSON is
   schema-enforced at the sampler. Moving the payload into a string trades a **provider-enforced
   structural guarantee** for one we implement — and the two are not equivalent: constrained decoding
   **prevents** the error at zero cost, while a server-side validator **detects** it afterward and
   charges a full retry turn. At Appendix A prices a retry turn is ~3.9 s and ~222 output tokens,
   so a self-policed carriage must be *better* than enforced-zero to break even, and it cannot be.
4. **The failure worth fixing is not the carriage.** ~17% of realistic writes were silently wrong in
   **both** formats, always on backslash escaping. That is where the next experiment should go.

**Recommendation: do not migrate the carriage.** Not because EDN is error-prone — it never once
broke — but because the measured gains are negative, the robustness gain is unproven at this n, and
the migration would forfeit an enforced guarantee for a policed one. **Spend the effort on escaping
fidelity for long payloads instead**, where a 17% silent-wrong rate is sitting in production traffic
today regardless of which carriage carries it.

## What C cannot separate

1. **All three carriage arms are free text.** None is a real tool call, so absolute rates are not
   production rates. The comparison between carriages transfers; the levels do not.
2. **n = 30 per arm bounds rates only below 10%.** Everything in C.1 is limited by this.
3. **C.3 measures one payload**, not a corpus. It tests whether time tracks count on a representative
   write; it does not re-price the corpus screen.
4. **Ten fixtures chosen to be adversarial** over-represent hazards relative to real write traffic,
   which inflates absolute rates in every arm — deliberately.
5. **One model, one reasoning effort, one hour.** A weaker model is exactly where a carriage
   difference would appear, and this run does not test one.
6. **The constrained-decoding finding is about this route.** It was established empirically here and
   should be re-established anywhere it is relied upon.

## Confidence

**MEASURED, ours, 2026-08-29.** Constrained decoding **active** when a schema is supplied (5/5),
**absent** otherwise (5/5 both formats). Malformed rate **0/30 in all three carriages**, true rates
bounded below 10% at 95%. Well-formed-but-wrong **3/18 on a realistic payload, in both carriages,
always on backslash escaping**. Generation time ratio **1.090** against token count ratio **1.103**,
per-token rate ratio **0.988**.

Re-run with:

```bash
bb bench/probe_constrained_decoding.clj OUT_DIR 5
bb bench/measure_carriage_errors.clj OUT_DIR 3
bb bench/score_carriage_errors.clj OUT_DIR --markdown
bb bench/measure_carriage_generation_time.clj OUT_DIR 9
bb bench/score_carriage_generation_time.clj OUT_DIR --markdown
```
