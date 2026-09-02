# Captain's log, 2026-08-30 (day watch) — the day the routing story died twice and a better one was standing behind it

*Written by mayor@skiff at the conn through Gene's morning autonomous window ("Increase
performance. Pick promising hills to climb, delegate and supervise fantastic explorations.
Have something to brag about in 4 hours!") and the ratification session that followed.
Companion to the same-day night log (`the-night-the-arrow-walked-itself`). Every figure
below carries a branch@sha receipt; the ones that died are recorded with as much care as
the ones that lived.*

## The shape of the day

Six work lanes and four ideation lenses ran in parallel across three seats and a Sol
fleet. By afternoon: one adoption lever found at +62.5pp and then immediately humbled by
its own follow-up; the program's favorite hypothesis killed by sign-flip across three
fixtures; its replacement replicated twice before lunch; the ultimate measure of
performance written into doctrine; and a fast cheap model put through its paces against
the newest surface. Gene ratified three builds with five words total ("1 go, 2 go, 3 go";
"Recovery go") and the relay system carried each verbatim.

## The routing story: +50pp → 0pp → −50pp, and what killed it

The prepared-request routing claim — the morning's top build candidate — died the way
claims should die: expensively cheap.

1. The original screen (n=4/arm) measured +50pp: a prepared guarded request moved
   Surgeon-first routing 2/4 → 4/4. (`experiment/differential-routing-interview-20260829`
   @ a9afcd13)
2. The replication at n=10/arm measured **0pp — both arms 10/10** — a ceiling, no
   headroom, kill criterion triggered as registered.
   (`experiment/prepared-request-replication-20260830` @ 6277e067)
3. SURGEON1's proxy cohort, on a sub-ceiling fixture with the real product shape,
   measured **−50pp: control 4/4, treatment 2/4.** Showing callers a non-executable
   prepared_request descriptor made two of four route *away*.
   (`experiment/prepared-request-proxy-screen-20260830` @ ab5759e5)

Sign instability across three fixtures is not noise to average — it is the signature of
an effect that was never real. Routing is recorded **unresolved-negative** and nothing
may cite it. Total cost of learning this: three preregistered cohorts on the
subscription route, roughly a day. Cost of NOT learning it: a product surface built on a
number that flips sign per fixture.

## The story standing behind it: recovery, twice replicated

Both cohorts that killed routing measured the same secondary, independently, same
direction:

- Replication: repair-episode output **−47.4%** (1,711 → 899 median tokens),
  construction refusals 6 → 0.
- Proxy cohort: output **−30%**, construction refusals 7 → 4, recovery actions **−60%**
  (20 → 8), complete wall **−25.3% (69.3s → 51.7s)**.

A prepared request does not change where agents *route*; it changes how badly they
*fumble* — the template with guards prefilled and caller-owned holes deletes the
call-assembly error class. Gene: **"Recovery go."** The LLD gate opened under the
ratified HLD Option A with the value claim rewritten to what replicated, and an explicit
prohibition on citing routing anywhere in the leaf.

Then the actuals landed under it. Codex history holds a 2026-08-23 ethnography window:
**228 Surgeon calls, 25 MCP refusals — ~11% of calls hit the failure path.** Two other
windows corroborate the band (~9% production repair loops; 7 firings with a 57%
recovery-read tax on the dominant deficient write-refusal site). At ~17.6s saved per
episode × ~11% incidence, the recovery gate is worth **~2 seconds per Surgeon call in
expectation** — and it defends the one-shot rate that the 4.9–9.7× headline numbers
silently assume. Caveat, filed with the number: this corpus is research-heavy; the
repository-stratified rate is unconfirmed.

## The adoption story: a ceiling, an attribution, and a granularity inversion

The morning's routing tranche (`experiment/routing-tranche-20260830` @ f8f9500,
wrong-subject 0/240) found: native-description salience **+62.5pp** (9/24 → 24/24),
action-native naming +20.8pp replicated, refusal handoff 5/5 with 0pp success drop, and
minimal schema **killed at −8.3pp** — leaner is worse.

Then SURGEON2's step-0 transfer screen produced the day's most instructive stop: the
REAL installed surface (actual `make install-agent-routing` into fresh homes) already
routes **4/4 at ceiling** on the comparable class — the preregistered sub-ceiling gate
refused to run treatment, and no LID design was allowed to start.
(`experiment/routing-adoption-live-config-20260830` @ d66fcf3) The mock's 9/24 baseline
had no routing guidance; production already ships most of the content lever. The +62.5pp
was real and mostly already ours.

So where does 1.48% external adoption come from? The attribution audit
(`audit/adoption-gap-attribution-20260830` @ 628e6d1) split it: **≥20.17%
guidance-absent** (a genuine distribution problem), 26.89% undeterminable, and **52.94%
guidance-present** — writes that routed native *with the guidance in context*. Whether
those were misses or correct small-class routing is exactly what the crossover ladder is
measuring. And inside that audit, the day's sharpest structural insight: **per native
ACTION, 81% of external writes look small; per TASK TURN, 4 of 9 reach ≥15 hunks with a
native-action median of 5 and max of 63.** Surgeon's unit is the decision; native's unit
is the fragment. The winnable market is invisible at action granularity and appears at
decision granularity — phase deletion, seen from the demand side.

## Doctrine shipped

- **The acid test** (CLAUDE.md + AGENTS.md, `docs/acid-test-doctrine-20260830` @
  8539e7e, ratified by Gene): matched serial Anvil comparisons on real historical
  decisions at an exact product commit are the ONLY proof instrument; local pairs are
  replication; screens buy options and may never mint performance claims; priced deltas
  are `projected` until converted; every claim names its task class; **the skill text is
  the actuator** — a measured boundary that never reaches the routing language agents
  read bought nothing.
- **Mayor skill Rules 5–6 + the option portfolio** (claude-skills @ e35e22d): a verdict
  carries a price model the conn must audit (the +317T/−12T repricing); lane harvests
  verify delivery at origin (one of four flawless lanes forgot to push); Gene's "Buying
  options always cheap!" written in as the vindicated engine, with the
  screen→replicate→build→dual-verify→install ladder. Disclosure preserved: that skill
  commit was mis-authored as Gene (env vars forgotten once); pushed history stays, the
  label error stands disclosed.

## Kills of the day (each a purchase)

- Prepared-request routing lift — sign-flip, three fixtures. Dead.
- Minimal schema — −8.3pp. Leaner routing surfaces are worse. Dead.
- All three turn-mining hypotheses (`experiment/turn-waste-mining-20260830` @ 62269e7):
  one-packet prep (82% episodes need genuinely new choices), batchable reads (only 4.5%
  independent), schema-discovery friction (6.8%). Three clean kills, zero model calls.
- The 5-reference multi-site rename vs native — native by 12.3s median
  (`experiment/multisite-headtohead-20260830` @ b06f41e). The small class belongs to
  native, now with a receipt; the crossover band is the ladder's job.

## Still in flight at log time

Crossover ladder + install regression gate (Anvil, dev-a); write-refusal ratification
packet (13 deficient sites, 8 spec candidates, mechanism already causally proven);
README economics section (prefill vs decode, the exploration bill); the spark caller
screen — a speed-tuned model against the newest surface, refusal-recovery under the
complete-vocabulary law, with a mandated SURPRISES section.

## The lesson worth carrying

Yesterday's log ended with "a measurement is not a verdict." Today extends it: **a
replication is not a confirmation until the fixture can actually express the effect** —
two ceilings in one day (the replication arms at 10/10, the transfer screen control at
4/4) each looked like results and were actually instruments that couldn't move. The
preregistered sub-ceiling gates caught both before a single wasted treatment token. The
discipline is no longer the mayor checking the seats; the seats now stop themselves.
