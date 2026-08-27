# Three-day progress assessment: the scalpel became real, but behavior still lags

<!-- agent-usage-window-end: 2026-08-27T02:20:10Z -->

**Window:** 2026-08-24 02:20:10Z through 2026-08-27 02:20:10Z; 2026-08-23
19:20:10 PDT through 2026-08-26 19:20:10 PDT.

**Decision:** The last three days produced two genuine product breakthroughs:
clj-surgeon no longer needs a 2 GiB heap, and a complete structural decision can
now execute as one guarded transaction that materially beats the observed and
champion native routes on source-volume-heavy changes. The remaining constraint
is no longer editor speed. It is getting agents to recognize a complete decision,
play one transaction, and stop.

## Sampling and exclusions

The bounded collector examined 49 Codex sessions and 49 Claude sessions. It
classified 39 Codex and 28 Claude sessions as Clojure-relevant. The complete
privacy-safe receipt is `/tmp/clj-surgeon-agent-usage-last-3-days.json` and has
schema `clj-surgeon.agent-usage-ethnography.v3`. It emitted hashed session keys,
no transcript prose, no workspace paths, no source bodies, and no raw service
events.

The service join covers 396 clj-surgeon MCP requests, eight cclsp MCP requests,
and six underlying clojure-lsp requests. Provider counts are larger because they
also include CLI operations and calls not represented in the retained service
window. Old and current interfaces coexist in this three-day window, so the
aggregate is a migration record rather than a clean post-release cohort.

Claude history does not expose the same complete-turn segmentation as Codex in
this receipt. Provider adoption can be compared, but complete-turn wall cannot
be compared honestly. Likewise, Surgeon-using Codex turns are selected toward
larger and harder work; their wall time is not a matched native control.

## Adoption scoreboard

| Measure | Codex | Claude |
|---|---:|---:|
| Sessions in window | 49 | 49 |
| Clojure-relevant sessions | 39 | 28 |
| Skill visible in relevant sessions | 39 | 17 |
| Sessions that loaded the skill | 25 | 0 |
| Recorded skill loads | 70 | 0 |
| clj-surgeon invocations | 901 | 1 |
| Native patch/edit actions | 409 `apply_patch` | 170 edits |

Codex made 507 CLI and 394 MCP clj-surgeon calls. The dominant legacy CLI
operations were 341 `:cat` and 133 `:ls` calls. The model used
`inspect_clojure` 338 times, `apply_clojure_changes` 44 times, and the new
`edit_clojure` 12 times. Only six recorded route features contained a computed
`transform`. SCI is therefore a verified product capability, not yet the
dominant production modality.

Claude is the clearest adoption loss. Seventeen relevant sessions could see the
skill, but none loaded it and only one `inspect_clojure` call appeared. The
box-wide Codex and Claude routing installation landed late in this window, so
the next bounded receipt is the fair post-install measurement.

## What production traffic says

The MCP service completed 326 of 396 requests and safely refused 70. Median
direct service wall was 202 ms and p90 was 1.824 s. Inspection had 168 ms median
and 1.039 s p90; mutation had 1.362 s median and 6.553 s p90. Across all 396
calls, direct tool wall totaled 346.175 seconds.

The service read 557 files and returned 1,307,962 source characters. It executed
26 multi-edit applies and 15 multi-file applies. The median applied transaction
contained two edits in one file; the largest contained 51 edits across 12
files. This is real batching, not only benchmark traffic.

The direct tool is not the dominant clock. Among 56 completed Codex turns that
used Surgeon, median complete-turn wall was 14.51 minutes, median Surgeon calls
were four, and median route phases were ten. Median direct Surgeon wall share
was 0.39%. Twenty-eight of 56 turns later used native patching, and 39 had six
or more route phases.

The recurring shape is therefore:

```text
structural read -> another read -> model decision -> native fallback -> verify
```

not:

```text
complete decision -> one guarded transaction -> proportional verification
```

This observational cohort cannot say that Surgeon is slower than native: the
no-Surgeon turns include short status and non-mutation work, while Surgeon is
selected for harder tasks. It does say that shaving another 100 ms from a read
will not materially improve complete work. Decision fragmentation is still the
production bottleneck.

## Breakthrough 1: startup memory stopped being a 2 GiB tax

The old default combined eager CIDER middleware with a 2,048 MiB heap cap. The
new runtime uses plain nREPL for hot reload and a 512 MiB cap. In the retained
production-matched profile:

| Runtime | Max heap | Ready | Peak RSS | Heap after forced GC |
|---|---:|---:|---:|---:|
| Eager CIDER control | 2,048 MiB | 7.33 s | 1,003.6 MiB | 60.4 MiB |
| Plain nREPL | 512 MiB | 6.99 s | 465.9 MiB | 41.1 MiB |

The complete old-to-new move cut peak RSS 53.6% while preserving hot reload.
The runtime also passed at 384 and 256 MiB; 512 MiB remains the default for
headroom. The final remaining analysis-nREPL escape hatch was closed at the end
of this window: `make nrepl` now inherits the same 64 MiB initial and 512 MiB
maximum heap. A live replacement JVM reported `max-memory=536870912`.

The shared cclsp architecture also became bounded: four active slots, eight
resident slots, one initializer, a ten-minute TTL, CWD-bearing inventory, and
dead-agent reaping that protects live coding sessions. The live measured broker
plus one worker was about 482 MB, below the 2.5 GiB converged-family target.

This is a decisive answer to the question that started the sprint: the 2 GiB
startup heap was not intrinsic to Clojure parsing or project state. It was a
runtime/layout choice.

## Breakthrough 2: compiled decisions can beat native materialization

The central architecture is now a small set of route-specific chords:

- `edit_clojure` for one exact guarded batch across forms and files;
- SCI-backed `programs` for bounded computed relations;
- grouped exact owner deletion and namespace-form addressing;
- direct extraction when all material decisions are supplied or mechanically
  provable;
- planning/refusal only when a genuine caller or architectural decision remains.

The strongest controlled evidence is not universal, but it is real:

| Frozen task | Surgeon | Native/control | Result |
|---|---:|---:|---|
| Six small replacements | 29.893 s | 31.378 s | 4.7% lower wall; model floor dominates |
| Real extraction cleanup | 32.546 s | 150.138 s | 4.61x; all six Anvil outputs exact |
| Complete 15-form extraction | 37.871 s | 122.278 s | 3.23x; both routes 2/2 correct |
| Complete extraction, champion native | 37.871 s | 142.545 s | 3.76x; both routes 2/2 correct |
| Public plan then apply | 49.941 s | 37.871 s direct | public plan costs 12.070 s / 24.2% |

The mechanism is source-volume elision, not a faster parser. Native must read
and render much of the changed source as a patch. Surgeon can transmit owner
identities, exact old subforms, counts, and decisions whose size is closer to
the decision than to the source. On a tiny visible literal edit, native remains
the right route and has measured wins. On a large already-decided structural
change, the transaction becomes the model's equivalent of an expert editor
chord.

The latest internal extraction compiler extended the fast route without
guessing. A supplied decision completed in 39.150 seconds; omitted but
mechanically provable visibility and counts completed in 37.500 seconds. Both
used one apply and one exact lint with zero discovery or refusal. An external
caller with an alias collision refused before write in 110.219 ms with a
completed plan, and a stale source hash refused in 3.804 ms. The compiler derives
facts; it does not choose architecture.

## Read recovery: a useful crossover, not a universal win

Complete owner vocabulary plus non-authoritative Levenshtein hypotheses fixed a
real failure mode: the semantically correct owner can be rank seven even when a
lexical suggestion is rank one. In the hard stratum, complete evidence reduced
wall from 25.293 to 18.527 seconds and eliminated five native discovery calls.
In the easy rank-one stratum, the richer refusal was initially 15.5% slower
because cautious prompting caused extra search. Combined wall improved only
2.35%.

A 2x2 experiment then separated result shape from instruction. A compact
`plan_id` handle missed its own release gates and was rejected. The earned
change was a 162-byte decision contract: listed owners are real frozen-snapshot
evidence; ranking is non-authoritative; semantic selection among those owners
is allowed; the exact retry verifies the choice. That sentence is now in the
live MCP refusal and is linked to a permanent test. This is exemplary hill
climbing: keep the small mechanism, preserve the negative result, delete the
unearned architecture.

## Other durable progress

- Every public MCP operation now reports one authoritative server-owned elapsed
  time through a common finalizer, including typed refusals.
- The edit portfolio now contains 13 historical tasks and 26 before/after
  targets, including counterfactuals from real large refactors rather than only
  synthetic one-line edits.
- Global routing now tells Codex and Claude to use compact structural edits only
  where they remove material work and to keep native patching for small literal,
  prose, and new-file changes.
- Verification became proportional to task risk instead of an editor tax.
- Warm nREPL-focused testing and cold milestone gates are both explicit paved
  roads.
- The load-spike side investigation produced a one-minute, 30-minute flight
  recorder and isolated a separate Git/GitHub refresh storm. That is not a
  clj-surgeon JVM defect, but removing persistent heap and child-process fanout
  reduces the laptop's vulnerability to the same storms.

## Losses, invalidated hypotheses, and debt

1. **Production routes are still fragmented.** Half of Surgeon-using completed
   Codex turns fell back to native patching, while direct tool wall was below
   one percent of complete wall.
2. **Claude adoption is effectively zero in this window.** Installation and
   visibility are necessary, not sufficient.
3. **Refusal volume is material.** Seventy of 396 service calls refused. Forty-one
   were selector failures; seven were invalid intent forms and six were invalid
   MCP requests. Refusals were safe, but recovery turns are not free.
4. **SCI has not yet earned a universal-editor role.** The interpreter is ideal
   for a pure relation that replaces repeated textual payload, but native patch
   can also batch. Only six computed-transform route features appeared.
5. **The benchmark advantage has a crossover.** Six small replacements saved
   only 4.7%; the 4.61x win appears when source volume and owner count are large.
6. **Several attractive mechanisms failed honestly.** Narration suppression did
   not remove planning latency. A compact plan handle did not meet its 60%/3 s
   gate. Levenshtein ranking alone was not a global speedup. These were stopped,
   not rationalized.
7. **The sprint produced substantial surface area.** The branch moved through
   roughly 170 commits and 441 changed paths; most added volume is frozen
   fixtures, receipts, and observations, not product code. That evidence is
   valuable, but future hills should prefer small pure seams and delete rejected
   experimental machinery promptly.

## Progress against the product goal

```text
capability implemented       yes
mechanism verified           yes
self-hosted                  yes
fresh callers succeed        yes
controlled 2–5x gate         yes, on large supplied structural decisions
universal production win     no, and no longer claimed
production behavior changed  partially; Codex yes, Claude not yet
```

The strongest conclusion is narrower and better than “Clojure tools beat
native editing.” A model that already knows the complete structural decision
can execute it faster and more safely when the editor accepts the decision in
its natural compressed form. A model that does not know the decision should
use broad discovery or request a mechanically bounded plan. Small visible edits
remain native-positive.

## Smallest falsifiable next improvement

Use the next marker-bounded production window to test whether the installed
routing changes actual behavior. For complete supplied-decision mutations, the
gate should be:

- at least 80% use one structural mutation call;
- no more than three route phases before proportional verification;
- no more than 10% fall back to native patching after a successful structural
  read or mutation;
- Claude loads or invokes the route in a material fraction of relevant sessions;
- no correctness or refusal-safety regression.

If this gate fails, do not optimize parser or transport latency. Inspect the
first decision boundary that caused another read or fallback, then make that
specific transition cheaper. The next product hill is adoption and route
compression, not another editor kernel.

## Evidence

- [MCP startup heap breakthrough](2026-08-24-mcp-startup-heap-breakthrough.md)
- [The scalpel must earn every call](2026-08-24-ethnography-the-scalpel-must-earn-every-call.md)
- [The compiled cleanup hit 4.61x](2026-08-25-captains-log-the-compiled-cleanup-hit-four-point-six-x.md)
- [The best plan was no plan](2026-08-26-captains-log-the-best-plan-was-no-plan.md)
- [Single-pass extraction survived adversarial acceptance](2026-08-26-captains-log-single-pass-extraction-survived-adversarial-acceptance.md)
- [Selector evidence has a crossover](2026-08-26-captains-log-selector-evidence-has-a-crossover.md)
- [Prompt wins; plan handle stops](2026-08-26-captains-log-prompt-wins-plan-handle-stops.md)
