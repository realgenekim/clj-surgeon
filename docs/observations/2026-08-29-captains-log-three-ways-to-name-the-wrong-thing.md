# Captain's log — three ways to name the wrong thing

Date: 2026-08-29
Seat: forge@skiff (mayor)
Status: chronicle. Two refusals shipped, one design floor discovered, one measurement in flight.

## The day in one sentence

**We tried three separate ways to make the model type less, and all three failed the same way:
each one saved bytes by compressing *which thing we are talking about*, and each one produced a
confident, verified, wrong-subject mutation — or could not survive review.**

## Strike one, this morning: positional line numbers

The idea was to replace file paths with numeric indices into a list the tool had already returned.
It worked. It cut client-visible payload **23.67%**.

It also **mutated the wrong file and returned `ok=true`.**

Purged the same day at Gene's direction — not gated, not warned, **purged**:

> *"I think a. Positional line numbers if I understand correctly should be purged. Unsafe, against
> ethos of tool, etc."*

The word that decided the fix was *ethos*. A gate would have kept the entrance open. Purging closed
the class.

## Strike two, tonight: mnemonic labels

Same shape, better disguise. Give every readable form a short label — `mn/s01`, `mn/s02` — and let
the model reference targets by label instead of spelling out file and owner every time.

**The prize was real and large.** Measured on the exact retained request, not estimated:

| | flat | after closed relations |
|---|---|---|
| request bytes | 6,509 | 2,871 |
| chars spent naming the subject | 1,492 | 1,098 |
| subject share of request | 22.92% | 38.24% |
| of those, repeated within the same request | 50.34% | 32.51% |

Two-character labels project **2,871 -> 1,889 bytes: -34.204%**, *on top of* the 7.35x we already
won. After the biggest win of the program, **38% of what the model still types is nothing but
saying which thing it means.**

Then SURGEON2 tried to break it, model-free, and broke it in one shot.

The fixture: two real owners, `alpha` and `beta`, **both containing identical `:old`**. Labels
frozen from a real basis: `alpha=mn/s01`, `beta=mn/s02`. Stated intent: change `alpha`. Submitted:
the **valid** label for `beta`, with `:old -> :new`.

**Result: `ok=true`, `verification_complete=true`, source changed. `alpha` untouched. `beta`
mutated. Parse valid. Every gate green.**

Its verdict, which is the sentence to remember:

> *"Snapshot binding rejects unknown, expired, stale, or cross-workspace labels. It cannot reject a
> consistently wrong but valid label: that request is observationally identical to intentionally
> selecting the other owner."*

Disposition: **labels GO for reading and navigation. NO-GO as authority to change code.** It also
cancelled its own planned Anvil cohort — correctly, because no amount of behavioral data repairs a
deterministic defect.

## Strike three, in progress: the 52-character minimal form

SURGEON1 designed a minimal literal-relay request at roughly **52 characters** to isolate the
per-turn floor.

It **failed the safety and decision audit.** The smallest defensible form is **760 characters** —
**14.6x larger than the idea.** No floor number exists yet; the harness is still in zero-token
construction.

Not a bug this time. A **floor**: safety, not cleverness, sets the minimum size of a request.

## Why they are the same failure

A name carries its meaning inside itself. A label or an index carries meaning **only by reference
to a table the tool holds and the request does not.**

That difference decides who can catch a mistake:

- Misspell a name and the tool **refuses** — no such owner.
- Name the wrong owner and the mistake is **legible**: it is written in the request, and a human,
  a reviewer, or the model re-reading its own words can see it.
- Emit the wrong **valid** label and **nobody can see anything**. The request contains no subject to
  disagree with. The tool checks the label exists, checks the old form matches, and proceeds. It is
  behaving correctly. It has no evidence a mistake occurred, because **there is no evidence to
  have.**

The fixture's cruelest detail is that both owners held the same `:old`. In ordinary code, differing
content would often have caught the slip — the edit would fail because the old form was absent.
**That protection is incidental, not designed.** SURGEON2 removed the luck and the defect stood up
immediately.

So the class is: **any scheme where the model emits an opaque token standing in for the subject can
produce a silently wrong mutation that passes every check the server is able to run.** Positional
indices and mnemonic labels are two instances. There will be more, and they will look clever.

## Why the one big win did not have this problem

**Closed relations won 7.35x — 289.507s to 39.369s — and never compressed identity.** It compresses
**repetition**: state a relationship once, and the tool expands it into N exact edits, **each one
naming its target in full.** The subject stays completely explicit in every effect. Only the
saying-it-again goes away.

**Compress repetition. Never compress identity.** Everything that obeyed this won. Everything that
violated it was refused — twice by a machine, once by review.

This was written down before two of the three strikes happened, as rule 3 of
`docs/why-reading-is-cheap-and-writing-is-expensive.md`. It was one example then. It is a law now,
confirmed by agents that were actively trying to prove the opposite.

## What it cost to learn

**Strike two cost zero model calls and about six minutes.** No cohort, no Anvil quota, no
metered spend. A fixture, a frozen basis, and a deliberately wrong pointer.

That is the argument for the zero-model screen as standard practice: **the cheapest experiment in
the program produced its most important refusal.** The ideas that die here are the ones that would
otherwise have died in production, silently, having already written to somebody's source tree.

**Zero false claims shipped in 22 hours.** Five gates stopped things: a wrong-index silent write, a
shipped CLI defect, a scorer weaker than its own law, a pressure-confounded MCP gate, and an
evidence mismatch on row 4. The count that matters is not how many ideas we shipped. It is how many
wrong ones we caught before they were ours to live with.

## Open

**The prefill/decode ratio is running on Anvil dev-a — n=9, interleaved.** It decides whether the
read side closes as a program. If reading is 50-100x cheaper than writing, we spend input freely to
keep identity explicit, and the 38% subject-naming share stops being a target and becomes a cost of
correctness we are happy to pay. **If it is closer to 10x, read-time enrichment needs a byte budget
rather than a blank cheque.**

The unresolved question SURGEON2 left on the table, being screened now: is there an **independent
effect identity** that binds a label to the intended owner *and* operation? If the answer is
"detected, not unrepresentable," the right result is a second no.

Related: `docs/why-reading-is-cheap-and-writing-is-expensive.md`,
`docs/observations/2026-08-29-captains-log-the-model-typed-less-it-did-not-think-less.md`,
`docs/observations/2026-08-29-wrong-index-ended-emission-composition.md`.

---

## Postscript, same evening: the theory got its number

Two results landed within the hour, and together they close the day's argument with arithmetic
rather than principle.

### The ratio: reading is 1,284x cheaper than writing

Measured on Anvil dev-a, n=9 per condition, one interleaved cohort, three conditions with a
tiny/tiny floor condition used to subtract fixed cost:

| condition | median |
|---|---|
| A large input, tiny output | 6.954 s |
| B tiny input, large output | 25.781 s |
| C tiny/tiny (the floor) | 3.927 s |

`A - C` = 3.027 s bought **219,544 input tokens -> 72,529 tok/s**.
`B - C` = 21.854 s bought **1,234 output tokens -> 56.5 tok/s**.
**Ratio 1,284x. ~14 microseconds to read a token; ~17.7 milliseconds to write one.**

It read a 220,000-token document in three seconds. **Writing that same document would take 65
minutes.**

**It independently confirms the afternoon's emission constant.** That experiment, unrelated in
design and run by a different seat, measured ~6 ms per authored character. At ~3 chars/token,
17.7 ms/token is ~5.9 ms/char. **Neither experiment was built to check the other.**

Our own decision rule, written before the measurement: *"At 100x the case for enriching results is
obvious. At 10x, a result that triples in size starts to cost something."* At **1,284x** the read
side is closed. **Spend input lavishly. Shave only what the model authors.**

Caveats, stated: MEASURED but not replicated; the 72,529 figure includes upload, so the ratio is a
**lower bound**.

A number nobody asked for: the raw API floor is **3.927 s**, while our agent loop shows **~11 s per
turn**. That ~7-second gap is **harness overhead we own** — now the largest unexplained cost in the
system, and unlike inference speed it is ours to fix.

### The guard: safety consumes 99.3% of the label prize

SURGEON2's constructibility screen, zero model calls, receipt `2d40036d`.

The proposal was to make a wrong-but-valid label detectable by binding each label to a content
guard. **Verdict: STOP, again.** A content guard is **rung 4 (detected), not rung 5
(unrepresentable)** — a coherent wrong label carrying its own matching guard remains perfectly
representable.

Worse, the guard does not even cover the corpus. In the retained request: **12/23 symbol rows share
`from`+`matches`; 9/9 require rows share add/remove shapes; and 14/14 deletions admit no guard at
all.** Only 35 of 47 rows are addressable.

Then the cost, charged honestly against the 1,889-byte label bound. The fitted model is exact —
**205 chars fixed overhead plus 35 chars per guard-character, across 35 guarded sites:**

| guard width | request bytes |
|---|---|
| 8 | 2,374 |
| 16 | 2,654 |
| **22 (128-bit)** | **2,864** |
| 32 | 3,214 |
| 64 | 4,334 |

**Current R is 2,871 bytes. A credible 128-bit guard leaves 7 bytes saved — 0.244%.**

**The prize was 982 bytes. After paying for safety, 7 survive. The guard consumes 99.3% of the
win.** To keep even a 20% saving, guards would have to be <= 5 characters, which is not a guard.

Using full source as the discriminator does work — **0 duplicate pairs across all 44 sites** — and
costs **16,613 characters**, nearly six times the unoptimized request.

### Why the two results are the same result

An hour before the guard numbers existed, we wrote the explanation in prose: **error detection runs
on redundancy; compression is the removal of redundancy; so a maximally compressed subject reference
is by construction maximally undetectable when wrong.**

The guard screen is that sentence with a price tag. **Re-adding the redundancy costs almost exactly
what removing it saved** — 982 bytes out, 975 bytes back. That is not a coincidence or a bad
implementation. **It is the same quantity, measured twice, going in opposite directions.**

And the ratio explains why the whole trade was mispriced from the start: **we were compressing the
side of the ledger that costs 14 microseconds per token, and paying in the one currency that has no
insurance — knowing which thing we meant.**

**Final disposition: labels GO for reading and navigation. NO-GO as mutation authority, twice,
now on arithmetic. Do not reopen without independent effect identity that is not a content digest.**
