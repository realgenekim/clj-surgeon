# Captain's log — the ledger had two sides and we only ever looked at one

Date: 2026-08-29, evening
Seat: mayor@skiff
Companion to: `2026-08-29-captains-log-three-ways-to-name-the-wrong-thing.md`

The earlier log chronicled three refusals and the law behind them. This one chronicles what
happened when we stopped arguing and measured. **Almost every conclusion in the first log survived.
Almost every PRIORITY in it did not.**

## The measurement that reframed the problem

Reading is **1,284x cheaper than writing**. ~14 microseconds to read a token; ~17.7 milliseconds to
write one. Three conditions on Anvil dev-a, n=9 interleaved, with a tiny/tiny condition subtracting
the fixed floor. Prefill 72,529 tok/s; decode 56.5 tok/s.

It read a 220,000-token document in three seconds. Writing that document would take 65 minutes.

Controls nobody asked for, which is why it is trustworthy: **linearity** across an eight-fold input
range (67,299 / 50,700 / 65,146 / 72,529 tok/s — flat, so not a large-prompt artifact); **network**
at 163 ms for a 1 MB POST, only 5.4% of the delta, making 1,284x a LOWER bound; and **caching
verifiably excluded** (distinct payload hashes, cached tokens never above 14,080 of 233,949).

It also independently confirmed the afternoon's ~6 ms/char emission constant from a completely
different experiment. Two unrelated measurements, one constant, neither built to check the other.

**And the floor decomposed.** Of the 3.927 s raw floor, 321 ms is local process start and ~3.6 s is
network and server. But loading the real config with the MCP server raises it to **4,417 ms**:
**+490 ms per turn, paid on every turn, purely to declare the tools.** That single line retroactively
explained an old puzzle — cutting 63.7% of the tool catalog had bought only 5.2%, and 490 ms
recurring is exactly the size the decomposition predicts. A result from the previous day, explained
by a measurement taken to answer something else.

## Then the corpus was counted, and three things reversed

Five predictions were registered IN ADVANCE with promotion and demotion thresholds, then tested.
That ordering is the whole reason the results are worth anything.

**REVERSAL 1 — we conflated "read results are free" with "reading is free."**

Read results are input, and input is nearly free. **But the model TYPES the read request, and that
is output, priced identically to a write.**

    bytes typed into READ requests:  455,185
    bytes typed into WRITE requests: 630,138        ratio 0.72 : 1

**Read-request golf measures 18.5% — 3.1x the entire write-side prize.** We spent an evening
optimizing the smaller side of a ledger whose other side we had defined out of existence with a
slogan. *Results are free; requests are not.*

**REVERSAL 2 — the write side is far smaller than anyone guessed.**

    RULE 10.4%  |  COPIED 73.0%  |  DECIDED 16.7%

**The median write contains 181 bytes of genuine judgment.** Across eight days the entire corpus
holds **6.2 minutes of irreducible authorship inside 37 minutes of typing.** And every write-side
identity encoding debated all evening, combined, is worth **4.25% — 94 seconds over 8 days.**
Identity locators are **4.3%** of write bytes. The opaque-handle schemes we refused on principle
were also, it turns out, aiming at almost nothing.

**REVERSAL 3 — format closure loses on the corpus it was designed for.**

The `path#owner` concatenation saves 8,109 bytes on writes and **loses 91,372 on reads — net -20%.**
The mechanism is one number: **writes name 1.00 form each; reads name 2.05.** Concatenating identity
is cheaper only when you name one thing. It was fan-out-dependent, not a principle, and the corpus
fan-out runs the wrong way.

## The predictions, scored honestly

| # | prediction | result |
|---|---|---|
| 1 | copyable share >= 60% | **CONFIRMED** — 83.6% from the immediately preceding read |
| 2 | addressable market >= 50% | **FALSIFIED** — 23.6% strict, below even the 30% niche floor |
| 3 | refusal-retry identity >= 50% | **SPLIT** — 1.6% by SHA, but 57.8% at >=95% byte copy, median overlap 99.6% |
| 4 | refusal frequency >= 15% | **CONFIRMED 4x** — 68.4% of missions |
| 5 | emission model transfers, ~6 ms/char, R^2 > 0.9 | **correlation CONFIRMED, rate FALSIFIED** |

Prediction 2 failed for an instructive reason: **48.2% of writes have no preceding read at all —
they follow a refusal.** The market wasn't small because reads are rare; it was small because
refusals dominate the corpus.

Prediction 5 is the one that mattered most. n=59, **76.3x byte spread, R^2 = 0.9807**, slope
**3.5237 ms/byte — not 6.** The emission model transfers to the production harness, and **every
magnitude computed all evening rescales by 0.59.** The study also flagged that its read-arm R^2 of
0.28 is a spread artifact (14.5x, floor-dominated) and refused to report it as a weak result. That
refusal is worth more than the number.

## The two largest costs need no new syntax at all

**A constant.** Selection refusals **truncate the candidate list to 10 when the median file has 27
owners.** 191 such refusals; **88.5% are then answered by the model performing a full recovery read
of a file the server had already parsed.** 225 boundaries, **33.5 minutes**, 2.72 MB. And **one
refusal in 283 carried a next call.** A hard-coded 10 costs more wall clock than every encoding idea
generated tonight, combined — and the 1,284x ratio is precisely the licence to return the whole
list, because result bytes are input.

**Ceremony.** **5,499 actions occur after the last structural decision, across 279 turns.** Median
12. **21.2 hours of wall clock.** **Zero of 279 turns ended at their decision.** Only 63 were typed
`verify`. The work finishes and the turn continues for twelve more actions.

## Carriage: the cost was structure, not escaping

Measured on the real 6,894-byte request while briefing Gene's "edit clj with clj" idea:

    Clojure content inside from/to ............ 2,595 bytes  37.6%
    EVERYTHING ELSE .......................... 4,299 bytes  62.4%
    JSON keys + colons across 33 edits ....... ~1,254 bytes
    extra bytes from JSON string escaping .....    31 bytes   1.2%

**Escaping is a rounding error. Nearly two thirds of the request is carriage, not cargo.** The same
edit in EDN reader syntax is 331 -> 270 bytes, **18.4%** — and carriage is paid on *every* request,
including the read side where the money now is.

The strongest argument for the idea is not bytes. It is **fluency**. Today's sharpest negative
result was an arm that was smaller AND slower (83.703 s vs 65.841 s) because bytes predict wall time
only inside a vocabulary the model reads fluently. Every prior compression idea moved *away* from
what the model knows. Clojure is plausibly the most fluent vocabulary available for editing Clojure.
The danger is equally clear: **Clojure-SHAPED is not Clojure**, and a bespoke op vocabulary wearing
parentheses could be the least fluent thing yet tried.

## What the gates did

**Seven gates stopped something today. Zero false claims shipped in 23 hours.**

The evening's two were the instructive ones, and both were seats refusing THEIR OWN work:

**SURGEON1 refused its own experiment twice before spending a token.** First: the observer's nonzero
exit was being discarded — a watchdog that cannot fail — and candidate identity was fenced once
before a four-arm loop, so arms 2-4 ran on trust. Second: the Codex executable identity was not
fenced between arms, and unknown actionable event types were silently dropped. Its own words: *"I
will not spend the four calls on an instrument that can still certify a changed client or hidden
prior action."* It then tested the repair by **injecting the failure the guard was supposed to
catch** — fake drift after position 1 must prevent position 2.

**SURGEON2 killed two designs model-free**, the second with an exact cost model (205 chars fixed +
35 per guard-char across 35 sites) showing a 128-bit guard leaves **7 bytes of 982 — the guard
consumes 99.3% of the prize it protects.**

## The decision I made, and why

**I stopped SURGEON1's floor probe.** Not because either audit was wrong — both found real
false-green classes and I would have accepted either. **Because the value of the target collapsed
while the instrument was being hardened.** The corpus study had just priced the entire write-side
term at 94 seconds over 8 days and rescaled every magnitude by 0.59. Two rounds of hardening a rig
for a question that had lost its value is sunk cost wearing the costume of rigour.

Gene's instruction that settled it, verbatim: *"Don't let logic or interpretation deter you / them
from firing off a cheap test and experiment to prove or disprove."*

Instrument discipline and analysis paralysis look identical from inside. **The distinguishing
question is not "is this rigorous" — it is "is the thing I am about to measure still worth
knowing."**

## What I would tell the next seat

**Every large finding today was discovered while looking for something else.** The 1,284x ratio came
from asking whether to shrink a schema. The read-request cost came from counting a corpus after we
had already designed for the opposite. The 490 ms catalog tax explained a previous day's puzzle. The
21.2 hours of ceremony was an extra count someone requested as an afterthought.

**And the pattern behind that: we were confident about the wrong variable four separate times, and
each time a cheap count settled in minutes what argument had not settled in hours.** Two frontier
models reasoned their way to "ordinal selection is unsafe" and were probably right — but nobody had
built the fixture, and Gene had to say so.

The law from the first log stands, unamended: **compress repetition, never compress identity.**
What the evening added is its companion: **measure the term before optimizing it.** A 4.25% prize
defended by excellent reasoning is still a 4.25% prize.

Related: `docs/why-reading-is-cheap-and-writing-is-expensive.md` (Appendix A),
`docs/observations/2026-08-29-write-side-emission-and-read-side-encoding-study.md`,
`docs/observations/2026-08-29-captains-log-three-ways-to-name-the-wrong-thing.md`.
