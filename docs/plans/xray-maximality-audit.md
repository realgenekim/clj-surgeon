# X-Ray Maximality Audit

**Status:** Active on `audit/xray-maximality`; `main` is frozen pending proof

## Why This Exists

The first X-ray release passed its feature keep gate, but it was merged before
we tested the stronger product claim: that the current design is a local
maximum for fast, safe structural computation. A keep decision is not a
maximality decision.

This audit must try to falsify the release. It must preserve the feature only
if the best surviving design beats the fastest safe alternative on wall-clock
time after correctness, source preservation, and one-shot behavior pass.

## Known Counterevidence

1. A branch-local definition in an existing CLJC fixture is invisible to
   `[:form NAME]`. X-ray can therefore return a plausible value computed from
   zero selections.
2. The checksum treatment returned about 13.5 KB of source evidence beside one
   integer. A compact projection of the same result is under 1 KB.
3. The benchmark omitted the strongest composed baseline:
   `clj-surgeon :q ... | bb -e ...`.
4. The benchmark runner always schedules `pre` first although its plan calls
   for counterbalancing, and its summary includes incorrect runs in efficiency
   medians.
5. The stress fixture is periodic and its scorer contains hard-coded expected
   values. It is not an independent real-program test.
6. Several plans and GitHub issues still describe shipped work as pending.

These findings disprove any current claim of maximality.

## Product Changes Under Test

### 0. One read surface

Test whether `:xray :expr` can own both literal and computed structural reads:

```clojure
(form 'transition)                         ; literal full evidence
(-> (form 'transition) (match :finish) right) ; literal relationship
(-> (form 'retry-policy) (expect-count 1) (analyze f)) ; exact count
(-> (form 'events) (match :event) (analyze f)) ; ordered vector
```

This candidate removes `:q`, `xray-one`, and `:evidence :full` from the primary
mental model. Literal X-ray is the full-evidence route; computed X-ray is the
compact route. Keep the old operation, EDN query spelling, and terminal names
as compatibility inputs during the experiment. Do not delete them until the
unified surface wins clean-agent literal, relationship, exact-one, aggregate,
and refusal tasks.

If performance is comparable, prefer the smaller documented API. Runtime
aliases are cheap; parallel concepts in the skill and help are not.

### 1. CLJC-aware named selection

`[:form NAME]` must see branch-local definitions in `#?` and `#?@` forms.
An optional platform selector must choose one branch. An unqualified selector
must return honest ambiguity, never a false zero.

The Clojure builder should remain threadable:

```clojure
(form 'load-starred-post :cljs)
```

### 2. Exact-one computed read

Add an exact-one X-ray terminal, tentatively `xray-one`. It receives the
selected value directly and refuses zero or many matches before invoking the
analyzer:

```clojure
(-> (form 'ops-registry)
    (xray-one #(count (rest %))))
```

Generic `xray` remains the aggregation terminal and continues to receive a
vector of selected values. This separates intentional empty/many aggregation
from a singular selector whose failure must be loud.

### 3. Compact evidence

Make computed reads return compact, hash-backed provenance by default:

```clojure
{:value ...
 :match-count ...
 :trace ...
 :matches [{:address ... :line ... :end-line ... :source-hash ...}]
 :selection-hash ...
 :source-hash ...}
```

`:evidence :full` preserves the existing exact-source result for inspection.
Compact mode must not omit addresses, ranges, cardinality, trace, or hashes.
The old full-evidence contract remains testable and explicitly available.

### 4. Local repair diagnostics

Missing and invalid X-ray input must include the relevant invocation and
terminal signatures in the refusal itself. Do not print the complete symbol
allowlist by default and then require a separate `--help` call. Target: under
1 KB for common authoring failures without weakening safety detail.

### 5. Stable selection and canonical initializer data

Test one computed input type: `analyze` always receives an ordered vector.
`expect-count` may refine cardinality before invocation but never unwraps the
vector. Add `initializer` to select a `def` right-hand side without variable
`down` / `right` guesses.

For computed analysis only, canonicalize map literals and syntactic
`hash-map` / `array-map` constructor calls to a map without executing source.
Odd constructor arguments refuse; unsupported calls remain lists. Literal
X-ray and evidence retain exact source, while computed results declare the
data view. This candidate must beat the real `ops-registry` task on wall time;
smaller output alone is insufficient.

### 6. A-priori analyzer contract and natural Clojure

Make the computed input contract complete before the first call: `analyze`
always receives one ordered vector of ordinary Clojure data. Agents should
write one total function over that value rather than issue a separate
container-shape query. `tree-seq` provides shape-independent traversal; a
nested `for` scopes that traversal below semantically known parents.

Do not turn SCI into a smaller surprise language. Admit demonstrated pure core
idioms such as `key`, `val`, and `for`. Because `for` expands through loop and
chunk internals, keep those symbols private to macro expansion and continue to
refuse direct nonterminating `loop` / `recur` source before file I/O.

## Permanent Test Matrix

All prior tests remain. Add tests for:

1. `#?` and `#?@` branch-local named forms, shared forms, metadata, duplicate
   names, platform selection, absent platforms, and stable query order;
2. exact-one success, zero refusal, many refusal, analyzer non-invocation on
   refusal, direct selected-value input, and composition with every read step;
3. compact evidence for node, span, partition, zero, one, and many selections;
4. per-match and whole-selection hash determinism and source sensitivity;
5. byte-for-byte full evidence under `:evidence :full`;
6. CLI argument validation before source I/O, help, README, skill, installed
   command, unchanged source, and structured nonzero refusals;
7. diagnostic byte bounds and executable local remedies;
8. benchmark summarization that excludes incorrect runs from all efficiency
   statistics, plus counterbalanced scheduling tests;
9. stable vector input, count refinement without unwrapping, initializer
   selection across every `def` arity, canonical map constructors, malformed
   constructor refusal, unsupported-call preservation, and exact literal
   evidence.
10. shape-independent map/vector traversal, idiomatic `key` / `val` / `for`,
    direct loop refusal, macro-expansion isolation, help and skill propagation,
    and the exact clean-agent expressions that previously failed.

Use the existing real-program-derived CLJC fixture and `ops-registry` in
`src/clj_surgeon/core.clj`; do not replace them with toy-only evidence.

## Benchmark Matrix

The real irregular task computes category frequencies, total required
arguments, and sorted paired operations from clj-surgeon's own 296-line
`ops-registry` form.

Compare:

| Route | Purpose |
|---|---|
| `:q` plus model reconstruction | observed old behavior |
| `:q \| bb` | strongest composed old-tool baseline |
| current full-evidence X-ray | released design |
| compact exact-one X-ray | candidate design |
| direct Babashka/Clojure | execution-floor control |

Run at least four counterbalanced clean sessions per treatment in parallel.
Keep a small serial calibration to detect contention artifacts. Score only
correct, unchanged-source runs in wall/token/call medians. Generate expected
answers independently after version archives are built, and retain raw prompts,
transcripts, commands, timings, token use, scorers, and fixture hashes.

Repeat the winning route on one unrelated irregular structural shape. Include
a trivial negative control where `:q` should remain preferable.

## Gates

Correctness and safety are absolute gates. No speed result can compensate for
a wrong value, a false-success selection, a source mutation, or weaker tests.

Keep a candidate only if it:

- is correct in every clean run;
- uses no extra source-bearing call;
- reduces computed-read source output below 2 KB on the large task;
- improves median wall time by at least 10% over full-evidence X-ray, or proves
  another compelling one-shot benefit without slowing it;
- remains competitive with `:q | bb` while retaining structural provenance;
- recovers from common authoring errors without a separate help call.

Call the hill locally maximal only after three consecutive bounded experiments
fail to improve median wall time by 10% without weakening correctness, safety,
or the permanent tests.

## Release Discipline

Do all work on the audit branch. Do not merge, install as the default, close
issues, or update headline claims until the gates pass and the raw evidence is
durable. If the candidate fails, preserve the negative result and either keep
the released behavior explicitly qualified or prepare a normal forward revert
for review. Never hide the experiment by rewriting shared history.
