# Performance versus native over time

**Date:** 2026-08-28  
**Scope:** correctness-gated complete-task wall time from retained matched
benchmark receipts  
**Data:**
[2026-08-28-performance-vs-native-timeline.tsv](2026-08-28-performance-vs-native-timeline.tsv)

## Bottom line

The cleanest longitudinal story is one frozen task: the 15-owner Sessionize
extraction. Surgeon advanced from a public plan-plus-apply route at 2.47x native
to a promoted one-action terminal transaction at 6.36x native. The gains came
from deleting model-managed decision boundaries, not from making the parser a
few milliseconds faster.

```text
Same frozen 15-owner extraction; retained native = 122.278 s

2026-08-26  public plan -> apply              49.941 s   2.47x  ##########...............
2026-08-26  direct extraction                 37.871 s   3.23x  ########................
2026-08-27  fused exact verification          27.471 s   4.45x  ######..................
2026-08-27  + terminal response               21.815 s   5.61x  ####....................
2026-08-27  promoted executable chord         19.216 s   6.36x  ####....................
                                                     lower wall is better ->
```

The broader portfolio shows the crossover rather than a universal win:

```text
Task shape                                      Surgeon / native speed

one tiny exact edit, symmetric natural route    0.90x   native wins
small real provider-optional commit              0.48x   native wins
six small edits, Anvil                           1.05x   near tie
17 exact owner deletions                         1.66x
30-edit cleanup, local                           3.17x
30-edit cleanup, Anvil                           4.61x
30-edit cleanup, later one-file cohort           5.80x
51-edit / 9-file cleanup                         4.94x
15-owner extraction, promoted product            6.36x
cross-caller extraction probes                   6.37x--9.69x  (N=1/model)
```

The empirical boundary is now crisp: Surgeon wins when structural intent lets
the model avoid reading, reproducing, and repairing a large source surface.
Native remains the correct control—and often the winner—for one small visible
patch whose complete context fits in one bounded read.

## Reading the data honestly

The TSV contains 34 aggregate observations from August 4 through August 28.
Every row records task family, route, caller/model, sample sizes, complete wall,
native wall, correctness, byte-exactness, evidence source, evidence commit, and
the important caveat.

These points are not one continuous benchmark. The visualization may connect
only repeated instances of the same frozen family. In particular:

- the five-step 15-owner extraction line uses the same retained 122.278-second
  native control and the same semantic scorer;
- public-CFP cleanup and submission-row cleanup are distinct historical
  counterfactuals with different edit surfaces;
- cross-caller rows use each model's own matched native control and have one
  pair per model;
- a semantic-correct row may retain presentation drift when whitespace was not
  part of the task's meaning contract;
- exactness and sample size remain visible rather than being converted into a
  single confidence score.

The separate “best replicated demonstration” frontier is descriptive only. It
answers “what magnitude has any matched N>=2 cohort demonstrated by this date?”
It does not claim that one benchmark improved monotonically or that the latest
tool is that fast on every task.

## Canonical evidence families

| Family | Earliest retained matched result | Latest/highest retained result | Claim boundary |
|---|---:|---:|---|
| Read/search | 0.96x--1.68x on 2026-08-04 | 1.84x native-read comparison on 2026-08-07 | Result-shape and presentation vary; not an edit claim. |
| Small exact edit | 1.24x on 2026-08-07 | 0.90x in the symmetric 2026-08-24 natural route | Native is usually faster when one bounded read supplies patch context. |
| Six-edit batch | 0.99x CLI tie on 2026-08-06 | 2.46x one-shot MCP on 2026-08-07; 1.05x Anvil control on 2026-08-25 | Batching alone does not guarantee a large win. |
| Owner cleanup | 1.66x for 17 deletions | 4.61x exact Anvil public-CFP and 4.94x nine-file submission cleanup | Gains scale when names replace large deleted source surfaces. |
| 15-owner extraction | 2.47x public plan | 6.36x promoted product | Same frozen task; the strongest causal longitudinal line. |
| Cross-caller extraction | not applicable | 6.37x--9.69x across Opus/Fable/Terra/Sol | One matched pair per model; do not pool callers. |

## Exclusions and retained negative evidence

- The first 2026-08-23 Anvil comparison is excluded because nested Bubblewrap
  prevented native filesystem/process tools while MCP ran outside that failed
  boundary.
- The cclsp outgoing-call 3.03x wall ratio is excluded because the native arm
  was incorrect.
- A 60.185-second native delete/edit/delete pilot is excluded because there is
  no comparable Surgeon result.
- The early exact-nested no-read native lanes are excluded from speed ratios
  because native was 0/6 exact; they remain reliability evidence.
- The first submission-row cohort's semantic-but-byte-red rows are not silently
  promoted into exact aggregates. The single exact pair is recorded separately;
  the later corrected two-pair cohort is the stronger result.
- The 2026-08-28 tool-name tournament is not treated as a product learning
  curve. Only canonical catalog U is retained as contextual timing because the
  experiment varied public vocabulary and its wall separation was ordinary
  service/model noise.

## Sources and retained artifacts

The normalized rows cite their canonical Captain's Log. The strongest raw or
archived corroboration includes:

- `bench/results/2026-08-04-native-control-v2-gpt-5.6-sol-medium/` and
  `bench/results/2026-08-04-v13-vs-2026-07-12-gpt-5.6-sol-medium/`;
- `bench/results/2026-08-24-edit-clojure-sol-high-v3/`,
  `bench/results/2026-08-24-edit-clojure-matched-skill-sol-high-10x/`, and
  `bench/results/2026-08-24-edit-clojure-native-sol-high-3x/`;
- `/Users/genekim/src.local/clj-surgeon-bench-archive/2026-08-27/clj-surgeon-cross-caller-anvil-573e240-r1.tar.gz`;
- the immutable hashes and seat-specific result paths recorded in
  [the 4.61x cleanup receipt](2026-08-25-captains-log-the-compiled-cleanup-hit-four-point-six-x.md),
  [the 6.36x terminal receipt](2026-08-27-captains-log-terminal-proof-ended-the-second-plan.md),
  and
  [the 4.94x submission cleanup receipt](2026-08-28-captains-log-tool-names-enter-the-real-arena.md).

## Smallest next improvement

Generate this view from the TSV as part of benchmark receipt publication, and
require each new matched result to name its frozen task family, correctness
law, candidate commit, native control, and sample size. That makes the graph a
guard against benchmark drift instead of a manually curated victory chart.
