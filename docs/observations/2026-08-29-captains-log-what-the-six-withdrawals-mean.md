# Captain's log — what the six withdrawals mean, and where the road goes

Date: 2026-08-29, end of a 25-hour run
Seat: mayor@skiff
Written plainly, at Gene's request, after he asked what the withdrawal table actually means.

Companions: the three narrative logs of this date, and
`2026-08-29-measurements-and-how-to-repeat-them.md`, which holds every number and its method.

---

## 1. What the table says

**Six numbers were wrong. Each one was wrong in a different way.**

That matters more than the shared pattern. Everyone will notice that all six were single-source.
Fewer will notice that "verify before relaying" is **not one check. It is six questions.**

| # | The number | How it was wrong |
|---|---|---|
| 1 | candidate-list truncation at 10 | **The mechanism did not exist.** The cap applied to non-authoritative hypotheses, not to owner lists. 137/137 refusals already returned complete lists. |
| 2 | "191 mechanically derivable corrections" | **The claim over-read the data.** The candidate vocabulary is hint-only. The data never said "authoritative." |
| 3 | 21.2 hours of ceremony, 812x | **One outlier owned the total.** A single turn at `coverage_ratio` 0.0046 supplied 54.2%; 41.5M of its 41.7M ms were unattributed gap. |
| 4 | +490 ms per-turn catalog tax | **Nobody ran it twice.** One observation. A 98-call counterbalanced cohort found ~40.75 ms and no byte slope. |
| 5 | ~17% silent body corruption | **The instrument never touched its subject.** The harness did not call MCP and did not write files; `parse-ok` meant outer carriage only. |
| 6 | 42 write->error->rewrite loops, >=30 catchable | **The metric definition produced the number.** 8 loops at 60s, 14 at 300s, 16 at 900s. And of 8 genuine accidents, **0** were catchable by a post-state reader parse. |

**Ask those six questions of any number before repeating it.** They are cheap. Each one killed a
claim today:

1. Does the mechanism I am describing actually exist in the code?
2. Does the evidence authorize the strength of my claim, or only a weaker one?
3. Is this aggregate dominated by one row? What is its coverage?
4. Has anyone run it twice?
5. Did the instrument touch the thing it claims to measure?
6. Would a different reasonable metric definition change the number materially?

---

## 2. So what

**The cost of being wrong six times was almost nothing.** No quota, no build, no shipped defect.
Most of the kills were arithmetic over data already on disk.

**The cost of being right about even one of them would have been a week.** The sixth would have put
a parse gate in front of **all 463 native writes** in the fleet to catch **zero** of the eight real
failures.

**That is the trade this day made: six kills, no shipped mistakes.** It is not the trade anyone sets
out to make, and it is the correct one.

---

## 3. What is now closed

Each of these has a written kill criterion in the measurements reference. **Do not reopen one
without new physics.**

- **Making the write call smaller.** Seven designs died. The law is settled: **compress repetition,
  never compress identity.**
- **Changing the carriage format.** EDN saves bytes and costs tokens. **Tokens are what you pay.**
- **Editing our own instructions.** Behavior does not follow the clauses — the repo with no push
  rule had the highest git-action rate.
- **Shrinking the tool catalog.** No byte slope, and under-declaration hides tools from the model.
- **Fixing body corruption.** There is nothing to fix. The real path committed 5/5 hard cases exact
  at wire backslash depth 9.
- **Guarding native writes.** It would have caught none of the eight genuine failures.

---

## 4. What is now open — the part worth reading twice

**The measurements are a pricing engine.** We can now cost a proposal in minutes: 1,284x read/write,
3.5237 ms/byte emission, 0.96x copy/compose, ~222-620 output tokens per turn, constrained decoding
live on tool arguments. **That is why seven designs died fast instead of slowly.** Every future idea
gets priced before it gets built. This is the day's real asset.

**The read half is the product, and nobody has looked at it.** The model called `inspect_clojure`
**443 times** and the write path **seven**. We spent 25 hours improving the seven. **The model has
already told us what it values, and we were not listening.** The measured, earned, unbuilt
18.512% read-request reduction is sitting there.

**Adoption is a routing question, not a tool question.** The model can see the tool, is loaded with
it, consults it, and chooses not to write with it. **That is a decision made at a specific instant,
and nobody has studied that instant.**

**And nobody has asked the model why.** We measured behavior and inferred causes. **Every cause we
inferred has since been refuted:** visibility (443 inspect calls), catalog cost (does not
reproduce), corruption (does not exist), doctrine (no dose-response). The obvious experiment has
not been run.

---

## 5. The road ahead, in order

1. **Finish the Claude baseline.** Every adoption figure comes from a corpus that is 100% Codex.
   **If Claude routes differently, this is a model property and not a tool property** — and that one
   number decides which program to run next. In flight now.
2. **Study the read path.** Sixty times the traffic, zero optimization work, and one measured prize
   already earned.
3. **Stop proposing write-path designs.** The ceiling is **6.0% of mutations**, reproduced twice and
   cleared of its self-hosting confound. That is not where the work is.
4. **Keep the screen.** The zero-model screen killed six ideas for free today. **It is the most
   valuable thing built in 25 hours, and right now it is a habit rather than a system.** Making it a
   system is worth more than any design in the graveyard.

---

## 6. The sentence to carry forward

**A single-source number is a hypothesis wearing a decimal point.**

Six of them reached Gene as fact before they were checked. All six were relayed by this seat. The
corrections came from seats that re-derived rather than trusted — and in every case the seat
doing the checking had every incentive to confirm and chose not to.

**The measurements that survived had sample sizes, controls, floor conditions, and predictions
registered before the data. The ones that died had one observation and a compelling story.**

**Prefer six kills and a reference document to one shipped design built on a number that was 54%
an idle turn.**
