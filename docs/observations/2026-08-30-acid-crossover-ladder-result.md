# Acid test Part B: discovery crossover is beyond 32 changes

Date: 2026-08-30

Verdict: **no same-family crossover was observed through 32 changes and six
files.** Native won all four measured rungs. The honest same-family estimate is
a lower bound, **crossover greater than 32 changes / greater than six files**,
with no measured upper bound.

## Exact experiment identity

- Doctrine: `CLAUDE.md` at
  `8539e7ea4ed9df8dc1e51b057ba58f9760e2a28e`
- Product under test:
  `c55de2279826af5ed21c90981591479dd2e802b2`
- Frozen fixture/preregistration commit:
  `62785e1fa4474719ace4c7987b54186a4c6d9936`
- Part B harness commit:
  `091a28a67edab252a43b471a5bda15ba70de9bca`
- Harness tree: `637d3eeea348bddc5bb3894cf0f6b18903976e66`
- Host and subject: `anvil-server`, `dev-a`
- Model route: `gpt-5.6-sol`, high reasoning, ChatGPT subscription
- Scorer: the same hidden edit-portfolio semantic, exact-byte, source-set, and
  route scorer for both arms

The immediate capacity fence passed at load 3.17 on 16 CPUs, with 23,033 MiB
available memory, 232,045 MiB available disk, a clean exact harness checkout,
ChatGPT authentication, and zero active benchmark processes. Postflight was
also clean with zero active benchmark processes.

### Prelaunch wiring correction

The initially committed convenience script inherited the portfolio pair
wrapper's supplied-decision contexts. That contradicted the already frozen
discovery-vs-discovery prose. Before any Part B model call or outcome, commit
`091a28a` changed only the route wiring to the preregistered arms:

```text
Surgeon: mcp:mcp-exploratory-rule-no-skill
native:  native:no-skill
```

The task bytes, fixture hashes, product, scorer, schedule, predictions, metrics,
and stop law did not change. Part A had already run, but it used a separate
historical extraction task and supplied-decision route; no ladder outcome
existed to tune against.

## Matched results

Wall medians below are the midpoint of both declared observations, including
the incorrect rung-16 Surgeon position. Correctness remains a separate gate.
Actions are observed tool round trips, so discovery and mutation are both
charged.

| Changes / files | Arm | Raw wall observations | Median wall | Output tokens | Actions | Correct | Correct one-shot |
|---|---|---|---:|---:|---:|---:|---:|
| 3 / 1 | Surgeon | 76.804, 53.374 s | 65.089 s | 2,795.5 | 5.0 | 2/2 | 2/2 |
| 3 / 1 | native | 58.607, 49.303 s | **53.955 s** | 2,221.0 | 5.0 | 2/2 | 2/2 |
| 8 / 2 | Surgeon | 82.168, 107.903 s | 95.036 s | 4,051.0 | 6.5 | 2/2 | 2/2 |
| 8 / 2 | native | 72.692, 86.551 s | **79.622 s** | 3,118.0 | 6.0 | 2/2 | 2/2 |
| 16 / 4 | Surgeon | 97.224, 108.593 s | 102.909 s | 4,624.0 | 7.0 | 1/2 | 1/2 |
| 16 / 4 | native | 69.046, 63.319 s | **66.183 s** | 2,830.0 | 5.5 | 2/2 | 2/2 |
| 32 / 6 | Surgeon | 163.029, 118.412 s | 140.721 s | 6,954.5 | 6.5 | 2/2 | 1/2 |
| 32 / 6 | native | 97.212, 97.584 s | **97.398 s** | 4,316.0 | 6.5 | 2/2 | 2/2 |

The per-rung `native median / Surgeon median` ratios were 0.8289, 0.8378,
0.6431, and 0.6921. Native was respectively 1.206x, 1.194x, 1.555x, and
1.445x faster. The preregistered monotonicity expectation failed: the ratio
improved slightly from 3 to 8 changes, then regressed sharply at 16.

## Predictions versus outcomes

| Rung | Preregistered direction | Outcome |
|---|---|---|
| 3 | native wins clearly | confirmed |
| 8 | native wins by less | confirmed direction; gap narrowed slightly |
| 16 | first likely Surgeon win | falsified; native won and Surgeon was 1/2 correct |
| 32 | Surgeon wins clearly | falsified; native won by 1.445x |

The crossover band requested before launch does not exist inside the measured
ladder. Combining this lower bound with the historical 51-edit/nine-file chord
would produce a superficial `32--51` bracket, but that would confound the very
decision boundary the experiment exposed: every ladder arm had to discover the
decision, while the 51-edit chord supplied the complete owners, forms,
replacements, and cardinalities. It is not honest to call that a same-family
change-count estimate.

## Correctness and wrong-subject audit

Native was semantic-correct and exact in 8/8 runs. Surgeon was semantic-correct
and exact in 7/8. The incorrect 16-change Surgeon run applied nine edits: it
renamed the definition and active keyword/wire literals, but missed the
resolved symbol references in all three caller files. It changed no unrelated
source and preserved every migration-history string and comment.

All 16 source-set receipts were exact. A retained-diff audit found zero changed
history-decoy lines. Therefore **wrong-subject = 0**. The rung-16 outcome is an
incomplete-subject failure, not a mutation of the wrong subject.

`Correct one-shot` requires task correctness plus exactly one accepted mutation
attempt. Native was 8/8. Surgeon was 6/8: the incorrect rung-16 mutation does
not count, and one correct 32-change run needed two refused apply shapes before
its successful transaction.

## Why the crossover did not appear

Every Surgeon run first attempted semantic Var-surface preparation and received
two `semantic-provider-unavailable` refusals before recovering through bounded
structural inspection. Three runs also incurred an `unknown-fields` read
refusal. Across eight Surgeon runs, that is 19 discovery failures. One
32-change run added two apply-shape refusals. The successful atomic mutations
themselves were fast—generally tens to low hundreds of milliseconds—but model
recovery, additional output, and extra actions dominated complete wall time.

The measured distinction is therefore decision completeness, not change count
alone:

```text
discovery required, up to 32 changes / 6 files  -> native measured faster
complete exact decision, 51 edits / 9 files     -> Surgeon measured 4.944x faster
```

For discovery-required work above 32 changes, performance remains unmeasured.
Atomicity, rollback, or fused verification may still justify Surgeon for
correctness, but not from a measured speed claim.

## Scoped routing consequence

Use native discovery plus native patching for this measured
rename/reference/literal class through 32 changes and six files. Use Surgeon
for a complete exact multi-owner or multi-file decision when its guarded chord
collapses fragile actions; the existing 51-edit/nine-file supplied-decision
receipt remains the large-class speed anchor. Do not route discovery-required
work to Surgeon solely because its estimated change count exceeds 32.

## Raw evidence and replay

The raw streams remain on Anvil at:

```text
/srv/fleet/dev-a/clj-surgeon-acid-results/part-b-c55-20260830-001
```

The directory retains all 16 prompts, `events.jsonl` streams, event clocks,
client surfaces, MCP receipts, commands, diffs, semantic scores, terminal
receipts, start/final hashes, the aggregate run table, action audit,
wrong-subject audit, and rung fold.

- Raw manifest SHA-256:
  `8e61d4bb5ea3c84aace00003750ffee31088c0e1274bd0834eed4e5fbd9117ca`
- Raw archive SHA-256:
  `2d5f2f3da58c1e9e7f991b175a560089b61504eb72ff82474408068d2885e8d3`
- Verified control copy:
  `/private/tmp/clj-surgeon-acid-archives-20260830/part-b/part-b-c55-20260830-001.tar.gz`

Replay from exact harness commit `091a28a`:

```bash
bash bench/run_anvil_acid_crossover_ladder.sh \
  /absolute/result/path \
  c55de2279826af5ed21c90981591479dd2e802b2
```
