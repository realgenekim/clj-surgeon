# Captain's log — six designs died, and then we found the tool was barely used

Date: 2026-08-29, late evening
Seat: mayor@skiff
Third and final log of the day. Companions:
`2026-08-29-captains-log-three-ways-to-name-the-wrong-thing.md` (the law),
`2026-08-29-captains-log-the-ledger-had-two-sides.md` (the reversals).

**Read the corrections section first.** Four figures in the companion logs are wrong, and one of
them is the headline.

## The corrections, before anything else

**"21.2 hours of post-decision ceremony" and "812x" are WITHDRAWN.** Re-derived from a fresh
collector run: the count reproduces exactly — 279 turns, 5,499 post-decision actions, median 12 —
but the WALL does not. **One turn supplies 11.52 h, 54.2% of the entire figure, and contains four
post-decision actions.** Its clock `coverage_ratio` is **0.0046**: 41.5 million of its 41.7 million
ms are `unattributed-gap`, which the measurement skill's own text forbids relabeling as work. 125 of
279 turns have coverage under 50% and hold 68% of the total. **Honest range: 5.68 h (completed clock
items only) to 9.74 h (excluding the idle turn). The ratio is 218x-373x, not 812x.**

**The candidate-list truncation prize is WITHDRAWN.** The cap applies only to non-authoritative
hypotheses. Refusals already returned complete `available_owners` in **137/137 cases with zero
truncation** — and 119 of those 137 were still followed by a redundant same-file recovery read.

**"191 mechanically derivable corrections" is WITHDRAWN** — the candidate vocabulary is hint-only.

**The per-edit JSON baseline was 321 bytes, not 331.** Mine.

**Four corrections, every one produced by a seat re-deriving rather than trusting.** The pattern is
the finding: *a study's claim is a hypothesis until a second seat reproduces it.*

## The graveyard — six designs, all killed on evidence, mostly for zero tokens

| design | cause of death |
|---|---|
| positional line numbers | mutated the WRONG FILE, `ok=true`. Purged, not gated. |
| opaque mnemonic labels | mutated the WRONG OWNER, `ok=true`, `verification_complete=true`. |
| guarded labels | the guard cost 975 of 982 bytes — **99.3% of the prize it protected**. |
| EDN carriage | saved 4.9% of write BYTES; **cost 1.4% write / 12.3% read TOKENS**. |
| single-anchor splice | `after` ALREADY ships meaning `to`; the proposal inverted it and **fails OPEN**. |
| declared-intent compression | **93.7% of requests net NEGATIVE** — nothing to amortize. |

**The law that survived all six: COMPRESS REPETITION, NEVER COMPRESS IDENTITY.** Error detection
runs on redundancy; compression removes redundancy; a maximally compressed subject reference is by
construction maximally undetectable when wrong. Measured twice: re-adding the redundancy cost almost
exactly what removing it saved.

**And the premise that killed the last three, which nobody checked for six attempts: THE CORPUS HAS
REPETITION IN STRUCTURE, NOT IN SUBJECTS.** The flagship request has 47 write occurrences across
**44 distinct subjects — only 3 repeat**; 162 of 190 requests have **zero** repeated subjects. That
is exactly why closed relations won 7.35x (it compressed the shared *shape*) and exactly why every
subject-compression scheme failed on arithmetic before it ever reached safety. **Six designs, one
unexamined premise.**

## What was measured, and what it cost to learn

**Reading is 1,284x cheaper than writing.** ~14 microseconds per input token; ~17.7 ms per output
token. n=9 interleaved on Anvil dev-a, with a tiny/tiny condition subtracting the fixed floor, plus
two controls nobody requested: **linearity** flat across an eight-fold input range, and a **network**
check putting upload at 5.4% of the delta — so 1,284x is a LOWER bound. Prompt caching verifiably
excluded.

**But read REQUESTS are output.** The model typed **455,185 bytes into reads against 630,138 into
writes**. We had conflated "read results are free" with "reading is free" and spent an evening on
the smaller side of the ledger. *Results are free; requests are not.*

**Emission is 3.5237 ms/byte in production** (R^2 0.9807, n=59, 76.3x byte spread) — **not the 6
ms/char measured on the bench.** Every magnitude computed before that point rescales by 0.59.

**And bytes are a proxy for tokens that BREAKS when the format changes.** That is what killed EDN:
bytes fell, tokens rose. Then generation time was measured directly and **tracked count at 0.988
ms/token**, so there was no coherence discount either — closing a domain gap an earlier
integer-sequence null had left open.

**Copying is not cheaper than composing: 0.96x.** A condition holding content fixed and varying only
whether the model had to construct it found no discount. Predictability made no difference.

**Constrained decoding is LIVE on tool arguments** — schema enforced 5/5, invalid JSON *and* invalid
EDN both permitted 5/5 without a schema. So production JSON is **enforced to zero malformation at
the sampler**, a guarantee a self-policed carriage cannot inherit. That, not fragility, is why EDN
loses: **enforcement prevents at zero cost; a validator detects and charges a retry turn.**

## The defect that outranks every optimization in this log

**On a realistic 48-line payload, 3 of 18 writes SILENTLY CORRUPTED THE SAME LINE.** Two in JSON, one
in EDN — **carriage-independent**. Always backslash escaping: `(str/replace "\\" ...)` became
`(str/replace "\"" ...)`. **~17%, in production traffic today.**

A second channel belongs specifically to EDN-in-a-JSON-string: a literal `✓` was consumed as a
unicode escape by the OUTER layer and parsed cleanly as a checkmark. **Double decoding, silent.**

**This is the same class we refused three times — a well-formed request meaning something other than
intended — except the corruption is in the CONTENT, not the IDENTITY.** Every guard designed today
protects the subject. **None protects the body.**

And the contrast is the lesson: **short identifiers transcribed 324/324 byte-exact; long
backslash-heavy bodies did not.** We proved fidelity on the easy case and generalized it.

## The finding that may reframe the whole program

Incidental to a screen about something else:

> **Of 277 write-bearing turns, only 28 contained any clj-surgeon MCP mutation. 274 of 277 LAST
> DECISIONS were NATIVE file changes. Only 3 were surgeon calls.**

**ADOPTION x PER-CALL GAIN = ACTUAL VALUE.** This program has spent 23 hours maximizing the second
term and never measured the first. A verified 7.35x on a channel carrying ~1% of mutations is,
fleet-wide, approximately nothing.

**Whether that is alarming depends on a denominator nobody has computed:** how many of those native
edits were *correctly* native — new files, prose, non-Clojure, whole-file rewrites — versus
mutations to existing Clojure forms where the structural editor was the better route and went
unused. **That addressable share, not 1%, is the real ceiling on everything here.** It is being
computed now.

And the honest counter-argument, which must be weighed rather than dismissed: **a tool with a 17%
silent-corruption rate on long payloads may be one the model is RIGHT to avoid.** Fix the corruption
before chasing the adoption.

## The gates

**Seven gates stopped something today. Zero false claims shipped in 23 hours.**

Twice, seats refused **their own** experiments before spending a token. SURGEON1 found its observer's
nonzero exit was being discarded — a watchdog that cannot fail — and candidate identity fenced once
before a four-arm loop, so arms 2-4 ran on trust; then, after repair, an unfenced Codex binary and
silently dropped event types. Its words: *"I will not spend the four calls on an instrument that can
still certify a changed client or hidden prior action."* It then tested the repair **by injecting
the failure the guard was supposed to catch.**

SURGEON2 killed two designs model-free, the second with an exact fitted cost model. The bench agent
**caught a bug in its own instrument before running** — Cheshire accepts a literal newline inside a
JSON string, which strict JSON forbids; left on, it would have handed JSON a free pass on precisely
the hazard where EDN is structurally better.

**And a seat refused my own count.** I claimed 27 config-only worktrees; CFP3 verified 12 within its
authorized set, found 11 carried a byte-identical patch and **one did not**, and excluded it.

## What I got wrong, recorded so the next seat does not repeat it

I relayed four study figures as measured fact; **all four were later corrected by other seats.**
I hand-verified a doctrine clause count against **three files that were not in the loaded stack** —
the corpus is 100% Codex. I told Gene the read side could be **"as lavish as you like"** when
standing context is re-prefilled every turn and only free below ~10k tokens. I quoted **ms/byte** all
evening as though bytes were the priced unit. I proposed an explicit terminal signal **that already
ships** — `verification_complete=true`, `next_action="none"`, with doctrine commanding compliance —
and has **0/279**. And I used **`git push -f`** on a branch with zero unique commits, breaking a rule
that exists precisely so that judgment call never gets made.

**And one decision I would make again:** I stopped SURGEON1's floor probe after its second
self-refusal. Both audits were correct. **The target's value had collapsed while the instrument was
being hardened.** Instrument discipline and analysis paralysis look identical from inside; the
distinguishing question is not *"is this rigorous"* but *"is the thing I am about to measure still
worth knowing."*

## What the next seat should read first

**Every large finding today arrived while looking for something else.** The 1,284x ratio came from
asking whether to shrink a schema. The read-request cost came from counting a corpus we had already
designed against. The 490 ms per-turn catalog tax explained a *previous day's* puzzle. The 17%
corruption surfaced inside an experiment about error rates. The 1% adoption number was incidental to
a screen about ceremony.

**The generalization: we were confident about the wrong variable five separate times, and every time
a cheap count settled in minutes what argument had not settled in hours.**

So the first log's law stands, and the second log's companion stands, and this log adds a third:

1. **Compress repetition, never compress identity.**
2. **Measure the term before optimizing it.**
3. **Measure ADOPTION before optimizing the per-call cost — and verify the premise your design
   depends on before designing six of them.**
