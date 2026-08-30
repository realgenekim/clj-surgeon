# Adoption-gap attribution: distribution is real, crossover remains unknown

Date: 2026-08-30

## Question

The external-corpus census measured successful structural mutations at 1.48%
and an addressable native-write upper bound of 44.74%. Two explanations imply
different work:

- **H-DIST:** agents did not receive Surgeon's routing guidance.
- **H-CLASS:** agents received the guidance, but most writes were in a task
  class where native editing was the correct route.

This follow-up binds routing guidance to each retained native write at call
time and describes the retained write shapes. It makes no model calls and does
not inspect or publish prompt prose, source, patches, commands, tool arguments,
session identifiers, or repository paths.

## Frozen evidence

- Source census commit: `28ee81f4f1294f0436b09a89a3b9f6b96eb62226`
- Window: `2026-08-22T00:00:00Z` through
  `2026-08-30T02:09:33.141926Z`
- Source report SHA-256:
  `bd7dd552e31b2e5fd068b6bfdcabec6b616376e6d58f1f9b9ea2b37be801e670`
- Attribution report SHA-256:
  `626d419df01d4de57529184dc805a0746120e45de5fe5246ac48f8de3ba0e4c0`
- External-A evidence manifest SHA-256:
  `b401b57a2fe3e6409750cc264d0f5ae80450127c26885aa2c98bdee0b2803c61`
- External-B evidence manifest SHA-256:
  `72af782956b01afd2699c78edede4733ff147c9f50509ce18cda52899da9701d`

The attribution fold fails closed if repository identity, evidence-manifest
identity, successful-write count, or addressable-write count differs from the
source census.

## H-DIST result

`guidance-present` means the managed routing block was in the agent's
instruction bundle before the write call. `guidance-absent` means an
instruction bundle was present but did not contain the managed block.
`undeterminable` means the retained evidence did not establish either state
before the call. These categories prove delivery into the call context; they
do not prove how the text arrived on disk.

### External A

All 63 successful in-repository native writes had routing guidance present.
None updated an established Clojure source or test file, so External A supplies
no evidence about Clojure editor routing.

### External B

| Population | Present | Absent | Undeterminable | Total |
|---|---:|---:|---:|---:|
| All successful in-repository native writes | 112 | 98 | 56 | 266 |
| Addressable established Clojure source/test writes | 63 (52.94%) | 24 (20.17%) | 32 (26.89%) | 119 |

The final instruction state across the 12 active evidence files was five
present, five absent, and two undeterminable.

**Conclusion:** distribution explains a real part of the adoption gap, but it
cannot explain the whole gap. At least 20.17% of the addressable writes lacked
the guidance. At least 52.94% occurred after the guidance was delivered. The
remaining 26.89% cannot be assigned from retained evidence.

## H-CLASS result

The 119 addressable External-B writes had these observable shapes:

| Metric per write action | Distribution | Median | p90 | Max |
|---|---|---:|---:|---:|
| Files | 98 one; 16 two; 5 three-to-four | 1 | 2 | 3 |
| Patch hunks | 97 one-to-three; 22 four-to-fourteen | 2 | 5 | 10 |
| Changed lines | 3 one-to-three; 44 four-to-fourteen; 47 fifteen-to-fifty; 25 fifty-one-plus | 21 | 85 | 226 |

The same actions grouped into nine retained task turns:

| Metric per task turn | Distribution | Median | p90 | Max |
|---|---|---:|---:|---:|
| Files | 1 one; 2 two; 4 three-to-four; 1 five-to-eight; 1 nine-plus | 4 | 19 | 19 |
| Patch hunks | 3 one-to-three; 2 four-to-fourteen; 2 fifteen-to-fifty; 2 fifty-one-plus | 12 | 150 | 150 |
| Native write actions | 3 one-to-three; 4 four-to-fourteen; 1 fifteen-to-fifty; 1 fifty-one-plus | 5 | 63 | 63 |

The acid-test map is semantic, not textual:

- Native won the measured small cases: two files and three changes, and a
  five-reference single-file rename.
- Surgeon won the measured large cases: a 15-form extraction with 63 callers,
  and a 51-edit transaction across nine files.

Patch hunks do not prove semantic change count, decision completeness,
named-form count, caller/reference count, or whether one atomic structural
decision spans several native writes. Consequently:

> **The fraction of the 44.74% addressable base where Surgeon is measured or
> predicted to win is undeterminable from this corpus.**

For orientation only, not as a performance classification: 97/119 individual
write actions were at or below the two-file/three-hunk proxy, while no
individual action reached the 15-hunk or nine-file/51-hunk proxy. After
grouping by task turn, 3/9 were at or below the small proxy, 4/9 reached at
least 15 hunks, and 1/9 reached nine files and 51 hunks. These proxies point in
opposite directions and demonstrate why action-level patch size cannot answer
the crossover question.

## Decision

Do not claim that better routing text will lift the whole 44.74% upper bound.

1. Treat the 20.17% known-absent slice as an earned distribution problem.
2. Resolve the 26.89% undeterminable slice with a direct, privacy-safe delivery
   witness before repricing it.
3. Do not reprice the 52.94% guidance-present slice until the crossover lane
   measures representative intermediate task classes.

An exact win fraction requires retained task-level facts that the current
census intentionally lacks: semantic edit count, decision completeness,
named-owner/form count, reference count, and the atomic decision shared across
native writes. A matched crossover study can then sample the observed
one-file, two-file, three-to-four-file, and multi-write turn strata without
guessing from patch hunks.

## Reproduction

The pure fold is
`dev/experiments/adoption_gap_attribution.py`; its focused witnesses are in
`dev/experiments/adoption_gap_attribution_test.py`. The privacy-safe output is
`bench/results/2026-08-29-external-corpus-shape-census/adoption-gap-attribution.json`.
