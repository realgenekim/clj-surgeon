# Captain's Log: the source became data

The experiment began with a simple proposition: an agent editing Clojure is
already thinking in Clojure, so let it use pure Clojure to compute over the
syntax it selects. Do not invent a jq imitation when the host language already
has a better collection algebra.

That proposition was attractive. Attraction was not the keep gate.

## The operation

`:xray` ends a read-only structural path with one sandboxed pure function:

```clojure
(-> (form 'audit-report)
    (match :events)
    right
    (xray #(frequencies (map :category (first %)))))
```

The function receives selected forms as Clojure data. The command returns its
bounded EDN `:value` beside the exact matches, addresses, trace, cardinality,
and complete-file hash. It cannot write source or a plan. SCI exposes pure
collection capabilities, not files, processes, namespaces, mutable references,
classes, or host interop.

The outer vector matters. One selected vector is delivered as a one-element
selection vector, so the analyzer operates on `(first values)`. A clean agent
missed that distinction. The skill, help, README, and permanent tests now teach
it directly.

## We invalidated our first victory

Four collaboration probes all chose `:xray` and returned the right answer. That
looked excellent until we noticed that the reporting question asked whether
the agent had used `:xray`. The feature name itself had primed the choice.

The first formal prompt had a second defect: it showed the exact expected map
while asking the agent to return that map. Although the agents still inspected
source, the prompt had leaked the answer. We discarded both results as adoption
evidence.

The corrected harness never names the candidate operation and never reveals
the answer. It gives pre and post agents isolated homes, commit-specific CLIs
and skills, private workspaces, exact scorers, and bounded parallel execution.
Every run retains its prompt, command transcript, response, timing, usage, tool
output bytes, source hashes, and adoption flags.

The complete hill climb is worth preserving because the negative stages shaped
the product:

| Stage | Valid adoption evidence? | Voluntary X-ray | Correct | Median wall | What changed next |
|---|---:|---:|---:|---:|---|
| Collaboration probes named X-ray | no | 4 / 4 | 4 / 4 | not comparable | Remove feature-name priming |
| Seven-branch prompt leaked answer | no | 0 / 4 | 4 / 4 | 17.75 s post | Remove expected values; require real aggregation |
| Sixty-event aggregation, old guidance | yes | 2 / 4 | 4 / 4 | 23.73 s post | Teach aggregation and the singleton wrapper |
| Sixty-event aggregation, final guidance | yes | 4 / 4 | 4 / 4 | 18.39 s post | Keep guidance; add computational stress |
| 300-event weighted checksum | yes | 4 / 4 | 4 / 4 | 29.19 s post | Keep the feature |

```text
voluntary adoption across valid post stages
old guidance    2/4 |███████████████████                   | 50%
final guidance  4/4 |██████████████████████████████████████| 100%

post wall clock across the aggregation hill climb
old guidance  23.73s |████████████████████████|
final         18.39s |██████████████████      | 23% faster
```

## Routine aggregation found the teaching boundary

The first valid aggregation task contained sixty unknown event maps and asked
for point totals by category. Before specific guidance, only two of four post
agents chose `:xray`. One initially reduced the outer selection vector instead
of the selected event vector. Two chose `:q` and did the arithmetic in model
reasoning.

We changed the guidance, not the task. It now says that counts, sums,
frequencies, and grouping belong in `:xray`, and its example demonstrates
`(first values)`. The final replication was exact in all eight runs:

```text
routine aggregation       OLD :q          NEW :xray
correct                    4 / 4           4 / 4
voluntary xray             —               4 / 4
median wall                22.54 s          18.39 s
median shell calls         2               2
median input               47,272          47,627 tokens
median output              586             482 tokens
source output              3,134           3,383 bytes
```

This was a real wall-clock and output win, but not yet decisive. The baseline
model could calculate the visible totals without another shell command. Exact
provenance cost 249 bytes. A symmetrical API would not justify the feature on
that result alone.

## The stress probe crossed the keep gate

The stress task selected 300 event maps and required a source-ordered weighted
checksum. It multiplied each one-based index by points and a category weight,
summed the products, and took a modulus. This is still ordinary pure collection
work, but it is unsafe to reconstruct by eye.

```text
weighted checksum         OLD :q          NEW :xray
correct                    3 / 4           4 / 4
voluntary xray             —               4 / 4
median wall                37.13 s          29.19 s
median shell calls         3               2
median input               92,001          50,666 tokens
median output              1,249           718 tokens
source output              13,271          13,559 bytes
```

```text
wall clock
OLD  37.13s |█████████████████████████████████████|
NEW  29.19s |█████████████████████████████        | 21% faster

input tokens
OLD  92,001 |█████████████████████████████████████|
NEW  50,666 |████████████████████                 | 45% fewer

output tokens
OLD   1,249 |█████████████████████████████████████|
NEW     718 |█████████████████████                | 43% fewer
```

The result is compelling because wall time, correctness, calls, and context all
move together. Provenance adds only 288 source-output bytes and preserves the
evidence needed to audit the computation. No treatment run added a source read.

## The product boundary

`:xray` does not replace `:q`.

- Use `:q` to return literal syntax or one structural relationship.
- Use `:xray` when counts, sums, grouping, checksums, or another derived fact
  would otherwise be reconstructed by the model or a downstream command.
- Keep computation pure, bounded, and read-only.
- Keep the selected source and structural provenance beside the answer.

Native `apply_patch` is also a fearsome competitor. It is deeply represented in
model training and has almost no learning cost. When the caller already supplies
one unique old/new text edit and ordinary patch review is sufficient,
`apply_patch` is the performance bar and can remain the default. clj-surgeon
must earn its extra machinery through structural discovery, ambiguity refusal,
byte preservation, source-derived transformation, hash-fenced replay, or
verified evidence.

```text
exact supplied unique text                 → apply_patch
unknown or ambiguous Clojure target        → clj-surgeon
repeated lookalikes or peer relationships  → clj-surgeon
replacement derived from selected source  → clj-surgeon transform
computed structural read                   → clj-surgeon :xray
```

Do not optimize for clj-surgeon use as an adoption metric. Optimize for correct
work at lower wall time. If a structural route is slower and buys no measured
correctness, call, context, or safety benefit, use the native patch tool.

This is the Bitter Lesson boundary. The kernel supplies a general structural
selection and safe execution substrate. The model supplies ordinary Clojure
collection logic. We did not add `:sum-points`, `:cond-frequencies`, or a macro
ontology. One general capability solved both aggregation and checksum tasks.

## Benchmark lineage: do not lose the climb that came before

X-ray is the latest step in a measured sequence, not an isolated win. The full
transcripts and caveats remain in the earlier
[structural-shell Captain's Log](2026-08-02-captains-log-the-file-became-a-structural-shell.md),
[clean Codex benchmark](2026-08-03-clean-codex-benchmark.md), and
[native-edit Captain's Log](2026-08-03-captains-log-will-the-agent-choose-the-scalpel.md).
These are the headline adjacent comparisons.

| Experiment | Old wall | New wall | Old calls | New calls | Old input | New input |
|---|---:|---:|---:|---:|---:|---:|
| Matched-skill read/edit suite, four tasks | 147.1 s | 118.1 s | 21 | 16 | 379,433 | 274,091 |
| Exact peer edit loop | 67.4 s | 49.3 s | 10 | 4 | 157,481 | 77,421 |
| Source-derived `transform` edit | 42.36 s | 28.22 s | 5 | 3 | 102,837 | 65,806 |
| Source-derived `:xray` checksum | 37.13 s | 29.19 s | 3 | 2 | 92,001 | 50,666 |

```text
wall-clock lineage (each pair is its own adjacent experiment)

matched suite old 147.1s |████████████████████████████████████████|
matched suite new 118.1s |████████████████████████████████        | 20% faster

peer edit old      67.4s |████████████████████████████████████████|
peer edit new      49.3s |█████████████████████████████           | 27% faster

transform old     42.36s |████████████████████████████████████████|
transform new     28.22s |███████████████████████████             | 33% faster

xray old          37.13s |████████████████████████████████████████|
xray new          29.19s |███████████████████████████████         | 21% faster
```

```text
shell-call lineage

matched suite  21 → 16   █████████████████████ → ████████████████
peer edit      10 →  4   ██████████            → ████
transform       5 →  3   █████                 → ███
xray            3 →  2   ███                   → ██
```

The prior studies also preserved negative evidence. A noun-shaped `:edit`
facade initially increased median wall time from 39.2 to 46.0 seconds and was
chosen by only one of four aware agents. A 1,015-byte compact skill used 74%
more input and 81% more commands than the precise production skill. Those
failures are why the current rule is empirical: prefer the smallest general
capability that lowers wall time in clean context, not the smallest API or
instruction file that looks elegant.

## The 240-line concern is valid

The canonical skill is exactly 240 lines because one standard read can consume
it. That is a transport ceiling, not an optimum. It still costs about 10 KB per
clean session. Shortening it without measurement could delete the very routing
rules that changed X-ray adoption from two of four to four of four.

If skill size becomes a measured wall-clock bottleneck, a future experiment can
minimize the always-loaded decision table and move task-specific material behind
explicit references. Do not spend cycles trying to coach away useful model
priors. Any shorter treatment must preserve one-shot adoption, refusal safety,
and the existing edit benchmarks. Until then, 240 is a tested upper bound, not
a claim of perfection.

## Test posture

The implementation began with a larger red contract than code: zero, one, and
many matches; nodes, spans, and partitions; stable order; every concrete EDN
family; invalid paths and functions; analyzer exceptions; lazy, host, and
oversized results; truncation; unsafe pre-I/O refusal; real nested syntax; CLI
round trips; unchanged bytes; help; README; both skills; and the 240-line cap.

The clean experiments then added eight permanent documentation assertions,
explicit `:xray` instrumentation, a parallel harness, routine aggregation, and
the checksum stress scorer. Tests were never weakened. The feature survived
because the hard result became both faster and more reliable, not because the
syntax felt native.
