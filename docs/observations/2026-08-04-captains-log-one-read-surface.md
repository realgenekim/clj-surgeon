# Captain's Log: one read surface

The first X-ray experiment found a real capability. The second began when we
noticed that a successful feature gate had been mistaken for a finished
product. We had merged to `main` too early.

This log records the attempt to falsify X-ray, make wall clock the primary
efficiency metric after correctness, and reduce the tool to the smallest API an
agent can use in one shot.

## The audit changed the claim

The released checksum result remains strong: four of four clean agents were
correct, versus three of four before X-ray, and median wall time fell from
37.13 seconds to 29.19 seconds. That proved X-ray deserved to exist. It did not
prove the implementation was a local maximum.

Three adversarial reviews found immediate counterevidence:

- a CLJC branch-local definition was invisible to `[:form NAME]`, allowing a
  plausible computation over zero matches;
- one integer arrived beside roughly 13.5 KB of repeated selected source;
- the benchmark had omitted the strongest composed alternative,
  `clj-surgeon :q ... | bb -e ...`.

The benchmark also scheduled `pre` first despite a counterbalancing promise and
included incorrect runs in efficiency medians. The synthetic checksum fixture
was periodic enough for agents to infer formulas instead of processing source.

The maximality claim was false. Work moved to `audit/xray-maximality`, and
`main` was frozen.

## Candidate v1: safer and much smaller evidence

The first audit candidate added:

- CLJC-aware named selection, including `#?@` branch forms and an optional
  platform;
- `xray-one`, which passes one value directly and refuses zero or many before
  invoking the analyzer;
- compact evidence with addresses, ranges, trace, per-match hashes, a selection
  hash, and the complete-file hash;
- local error repair under 1 KB rather than a 3 KB symbol wall followed by a
  second help call.

The permanent suite grew from 422 tests / 2,993 assertions to 426 tests / 3,051
assertions. No prior test was weakened. Formatter, linter, shell checks, CLI
tests, install tests, and benchmark self-tests remained green.

On clj-surgeon's own 292-line `ops-registry`, the exact same computed answer
produced these local process results:

| Metric | Full evidence | Compact evidence | Change |
|---|---:|---:|---:|
| Median, 8 alternating | 292 ms | 193 ms | 34% faster |
| Output | 29.1 KB | 1.4 KB | 95% fewer bytes |

That proved transport and printing improved. It did not prove agent wall time
improved.

## The real-program pilot said no

The new task asked clean agents to compute category frequencies, the total
number of required arguments, and sorted paired operations from the real
`ops-registry` hash-map. An independent rewrite-clj scorer derived the expected
answer. Any source mutation failed the run.

The first version-matched pilot was correct on both sides:

| Treatment | Wall | Shell calls | Source output |
|---|---:|---:|---:|
| Released X-ray | 72.7 s | 5 | 61.1 KB |
| Compact `xray-one` | 75.3 s | 8 | 9.2 KB |

Candidate v1 lost the primary metric. The transcript explained why. The agent
first selected the definition's docstring, then the `(hash-map ...)` syntax,
then treated that syntax as an evaluated map, then returned a lazy sequence,
and finally repaired the computation. Compact refusals made every failed probe
cheap, but there were too many probes.

This was an API-authoring loss, not an interpreter-speed loss.

The distinction is foundational: X-ray exposes concrete Clojure syntax as
Clojure data; it does not evaluate the selected program. A selected
`(hash-map :a 1 :b 2)` is a list beginning with the symbol `hash-map`, not an
already constructed map. Likewise, a named `def` selection is the complete
defining form, not only its initializer. The skill and local repair must teach
that boundary before an agent spends a probe rediscovering it. Computed results
must also be concrete EDN, so a lazy sequence should be realized with `vec` or
another concrete collection constructor before it is returned.

## The fearsome composed competitor was slower

Two clean agents received a 756-byte skill that taught one source-bearing
pipeline: structural `:q` output into Babashka. Both were correct.

| Treatment | Median wall | Shell calls |
|---|---:|---:|
| `:q \| bb` | 101.2 s | 9 |
| Released X-ray | 72.7 s | 5 |
| Compact candidate | 75.3 s | 8 |

The direct composition is an important escape hatch, but it did not displace
the integrated interpreter. One agent needed twelve commands to discover the
query grammar and the definition's value shape. The other needed five. The
interpreter is not gratuitous machinery: it removes a real orchestration seam.

## The surface-area realization

The documentation still said:

> `:xray` does not replace `:q`.

That boundary is useful as behavior and expensive as product surface. Literal
inspection and computation do not necessarily need different commands.

The next candidate makes `:xray :expr` the single read algebra:

```clojure
(form 'transition)                              ; literal form evidence
(-> (form 'transition) (match :finish) right)  ; literal relationship
(-> (form 'retry-policy) (compute f))           ; exact-one value
(-> (form 'events) (match :event) (aggregate f)) ; many values
```

A plain path returns exact source. `compute` returns compact evidence and
refuses zero or many. `aggregate` receives a vector. Therefore the primary API
no longer needs:

- a separate `:q` operation;
- terminals named `xray` and `xray-one` inside an operation already named
  X-ray;
- `:evidence :full`, because a literal path is the full-evidence route.

During the experiment, old operations, EDN paths, and terminal names remain as
compatibility inputs. The first deletion is conceptual: remove them from the
primary skill and help, then observe blank agents. Runtime aliases are cheap;
parallel mental models are not.

This is deliberately an ablation, not a declaration. The next clean-context
benchmark must compare candidate v1 with this unified surface on the same real
program, independent scorer, private workspaces, counterbalanced order, and
parallel execution. The candidate earns promotion only if every run remains
correct and source-preserving while median wall time and command count improve.

The terminal names encode cardinality, not different engines:

| Terminal | Cardinality | Analyzer input | Behavior |
|---|---|---|---|
| `compute` | Exactly one | `f(value)` | Refuse zero or many |
| `aggregate` | Zero or more | `f([values...])` | Preserve source order |

Both are read-only and receive parsed syntax-as-data.

## Candidate v2: simplification won a near-tie

A one-replicate pilot favored the unified surface by 21%, 37.5 seconds versus
47.7 seconds, with four calls on each side. Because agent variance is large,
the keep decision used four counterbalanced replicates run with parallelism
four. Every run was independently exact and preserved the source hash.

| Metric | Candidate v1 | Unified X-ray | Change |
|---|---:|---:|---:|
| Correct | 4/4 | 4/4 | Tied |
| Median wall | 75.2 s | 69.0 s | 8% faster |
| Median shell calls | 8 | 7 | 1 fewer |
| Median input tokens | 170,596 | 143,640 | 16% fewer |
| Median output tokens | 2,693 | 2,070 | 23% fewer |

The wall improvement is below the 10% threshold for a decisive neighboring
win. It is nevertheless a favorable near-tie on the primary metric and better
on every secondary metric. Because candidate v2 also removes a command
boundary, a second query language, a redundant terminal name, and an evidence
flag from the primary mental model, the evidence supports keeping the
conceptual simplification. It does not yet support deleting compatibility
inputs from the runtime.

The paired runs were noisy. Unified X-ray was 26% and 39% faster in the first
two pairs, then 19% and 65% slower in the next two. Medians are more honest than
choosing the appealing pairs, but four replicates are not a precision timing
study.

More importantly, no clean agent achieved the ideal skill-read plus one X-ray
answer. Agents still guessed the `def` shape, treated `(hash-map ...)` syntax
as a map, or returned a lazy sequence. Some used a literal `:cat`; others used
small computed probes such as `count`, a shape map, or `vec (take 7 value)`.
The surviving bottleneck is discovery after the first incorrect computation,
not the distinction between `compute` and `aggregate`.

The next hill-climb candidate should make a failed computation locally
instructive. Instead of dumping full help or forcing a second source read, an
analyzer refusal can return a bounded structural summary of the selected input:
its collection kind, count, child kinds, and list heads. For example, the
summary should reveal that a selected definition is a four-element list whose
last child is a list headed by `hash-map`, without repeating the 29 KB form.
That is specific repair evidence, not task-specific policy.

## The human naming test rejected `compute` / `aggregate`

Before another clean run, a human reader asked what the two terminals meant.
That question is evidence: the names described implementation but hid their
cardinality contract. `compute` did not say “exactly one.” `aggregate` sounded
like a SQL reduction even though its function could return any concrete EDN.

Five alternatives were considered: `inspect-one` / `inspect-all`, `query-one`
/ `query-all`, `analyze-one` / `analyze-all`, `one` / `all`, and `expect-one` /
`collect-all`. The next candidate collapses the pair into one CLI-flavored verb
with database-flavored cardinality:

```clojure
(-> path (inspect :one f)) ; refuse zero or many, then call f(value)
(-> path (inspect :all f)) ; call f([values...]) in source order
```

`inspect` signals read-only code examination. `:one` and `:all` are data, not
two more magic terminal names. The old terminal spellings remain compatibility
aliases until the candidate earns promotion.

## Candidate v3: smaller refusals led to slower agents

Candidate v3 combined the naming change with bounded `:input-summary` repair.
The one-run pilot was neutral: both versions were exact in six calls, and the
candidate finished 4% faster. The four-run parallel gate reversed that result:

| Metric | Unified v2 | `inspect` + summary | Change |
|---|---:|---:|---:|
| Correct | 4/4 | 4/4 | Tied |
| Median wall | 59.8 s | 69.6 s | 16% slower |
| Median shell calls | 8 | 9 | 1 more |
| Median input tokens | 143,579 | 183,126 | 28% more |
| Median source output | 35.3 KB | 16.2 KB | 54% fewer |

The summary did what it promised mechanically: agents stopped dumping the full
form as often, and source output fell by more than half. That did not satisfy
the product gate. Agents used the newly visible shape as an invitation to issue
more semantic probes. Smaller output is not a win when wall time rises.

This candidate is rejected as a combined change. It also confounded the new
`inspect :one` / `inspect :all` spelling with automatic input summaries. The
next bounded neighbor must remove the summary from the default path and test
the naming change alone. Do not blame or promote the names from this result.

## Candidate v4: `inspect` alone also lost wall time

Candidate v4 removed automatic summaries and isolated only the naming change.
The one-pair pilot had `inspect` 24% slower but one call shorter. Four
counterbalanced replicates again kept correctness perfect and rejected the
candidate on efficiency:

| Metric | `compute` / `aggregate` | `inspect :one` / `:all` |
|---|---:|---:|
| Correct | 4/4 | 4/4 |
| Median wall | 67.7 s | 78.4 s |
| Median shell calls | 8 | 9 |
| Median input tokens | 176,696 | 189,374 |

Three of four paired `inspect` runs were slower. Agents accepted the syntax
without naming errors; they still spent time navigating to and interpreting
the definition initializer. Human comprehension favors `inspect`, but the
measured wall result does not. Do not call aesthetic preference an efficiency
win.

The next neighbor removes the redundant verb. Inside an operation already
named X-ray, database-style `one` / `all` states only the cardinality contract:

```clojure
(-> path (one f)) ; exact-one or refuse
(-> path (all f)) ; vector in source order
```

This is smaller than both previous pairs and directly answers the human
question that rejected `compute` / `aggregate`. Compatibility aliases remain
available during the test.

## Candidate v5: `one` / `all` also lost

The one-pair pilot favored `one` / `all` by 26%, with eight calls on both
sides. The four-run gate rejected that attractive pilot:

| Metric | `compute` / `aggregate` | `one` / `all` |
|---|---:|---:|
| Correct | 4/4 | 4/4 |
| Median wall | 63.0 s | 89.1 s |
| Median shell calls | 7 | 9 |
| Median input tokens | 147,671 | 190,071 |
| Median output tokens | 2,223 | 2,880 |

The cardinality words were human-readable and clean agents used them without
syntax confusion. They did not improve the primary metric. This is the third
warning against trying to solve a structural navigation bottleneck by renaming
the analysis terminal.

## Stop naming; define the algebra

The next human question was more important: why have two terminals at all?
Could the tool simply return either a singleton or a vector?

It must not return an untagged dynamic union. One selected vector-valued form
and two selected scalar forms can both be represented as `[1 2]`. A function
cannot distinguish them. A query that broadens from one match to two would
silently change the analyzer input type and could produce a plausible wrong
answer.

The first type sketch still had two eliminators. The stronger simplification is
to keep the selection representation stable:

```text
select        : Path a -> Source -> Either Refusal (Selection a)
Selection a   = ordered Vector a plus evidence
expect-count  : Nat -> Selection a -> Either CardinalityError (SelectionN n a)
analyze       : Selection a -> (Vector a -> b) -> Either AnalysisError b
result        : Either Refusal (Evidence x ConcreteEDN b)
```

The laws are more important than the spelling:

1. Never infer scalar versus vector from the runtime match count.
2. Always pass a vector; one selected vector is `[[...]]`, never `[...]`.
3. Validate any requested cardinality before invoking user computation without
   changing the vector representation.
4. Preserve source order for every selection.
5. Never invoke the analyzer after truncation, parse failure, or cardinality
   refusal.
6. Require bounded concrete EDN output; keep evidence independent of the
   computed value.
7. Make writes unrepresentable in the X-ray program.

Candidate v6 follows directly:

```clojure
(-> path (analyze f))
(-> path (expect-count 1) (analyze (fn [[value]] ...)))
```

`analyze` is the only computation terminal. `expect-count` is an optional
refinement in the compiled selection program. It checks cardinality before the
function runs but never unwraps the selection. The LLM retains the semantic
work of interpreting source data; the kernel retains parsing, source order,
evidence, optional count validation, and fail-closed execution.

The key example distinguishes the formerly ambiguous cases:

```text
one selected vector [1 2] -> analyzer input [[1 2]]
two selected scalars 1, 2 -> analyzer input [1 2]
```

This is the Bitter Lesson boundary: make the substrate uniform and let the
model interpret it. Do not ask the runtime to guess a sum type from cardinality.

## Candidate v6: the type was clean; the workload was slower

The one-pair pilot favored stable selection: 53.7 seconds and four calls versus
58.4 seconds and seven calls. Four parallel replicates rejected promotion on
the primary metric:

| Metric | `compute` / `aggregate` | Stable selection |
|---|---:|---:|
| Correct | 4/4 | 4/4 |
| Median wall | 83.9 s | 102.2 s |
| Median shell calls | 10 | 11 |
| Median input tokens | 212,366 | 230,472 |
| Median source output | 30.6 KB | 19.2 KB |

As in the prior candidates, smaller output did not buy lower wall time. The
transcripts nevertheless showed that agents understood the stable vector
immediately. They wrote `(fn [[form]] ...)`; none confused one selected vector
with multiple selected values. The extra work came from navigating and
interpreting the selected definition, not from the input type.

Every candidate independently guessed how many `down` / `right` steps reached
the initializer of a `def`. Agents tried the docstring, the complete definition,
and the `(hash-map ...)` child; several read the whole form to settle the
question. That is repeated mechanical work and therefore belongs in the tool.

The next neighbor keeps the type law and adds one general structural operator:

```clojure
(-> (form 'ops-registry) initializer (expect-count 1) (analyze f))
```

`initializer` should support `def`, select zero nodes when no initializer is
present, preserve exact source, and never evaluate constructor syntax. It must
not absorb semantic interpretation such as converting `(hash-map ...)` to a
map. This is the Bitter Lesson boundary in miniature: the kernel locates the
right-hand side; the model understands it.

That last boundary was challenged immediately: why make every agent notice and
repair the map-literal versus `hash-map` distinction? An explicit `as-map`
helper would still require a choice. Every repeated choice costs wall time.

Candidate v7 therefore adds a canonical computed-data view:

| Exact source | Computed analyzer value |
|---|---|
| `{:a 1}` | `{:a 1}` |
| `(hash-map :a 1)` | `{:a 1}` |
| `(array-map :a 1)` | `{:a 1}` |
| `(merge a b)` | `(merge a b)` |

This is syntactic normalization, not evaluation. The implementation pairs
already-parsed key/value forms only for known map-shaped constructors. It never
invokes the head or its arguments. Odd constructor arguments refuse.
Unsupported calls remain lists. Literal X-ray and evidence retain exact source
and hashes, while computed results declare
`:data-view :canonical-collections`.

This slightly moves the Bitter Lesson boundary, but in the correct direction:
normalize a universal representation mismatch once in the substrate; leave
schema meaning to the model. The tool answers “map or `hash-map`?” before an
agent can ask. It still leaves “what does `:args` mean?” and “who owns `:pair`?”
to general reasoning.

## Candidate v7: the first decisive audit win

The one-pair pilot was exact on both sides and favored canonical initializer
data by 40%: 70.5 seconds versus 117.1 seconds, six calls versus twelve, and
roughly half the input tokens. The four-run parallel gate confirmed the result:

| Metric | Best prior | Canonical initializer | Change |
|---|---:|---:|---:|
| Correct | 4/4 | 4/4 | Tied |
| Median wall | 75.5 s | 51.5 s | 32% faster |
| Median shell calls | 8 | 5 | 3 fewer |
| Median input tokens | 157,515 | 104,269 | 34% fewer |
| Median output tokens | 2,366 | 1,665 | 30% fewer |
| Median source output | 8.5 KB | 5.3 KB | 38% fewer |

Every run preserved the source hash and produced the independently scored exact
answer. The post times were also comparatively tight: 46.8, 62.4, 51.9, and
51.2 seconds. No post agent called `:cat`, `:show-form`, text readers, `:q`, or
help. All used X-ray expressions exclusively.

This is the strongest audit result because it removed questions rather than
adding instructions. Agents used `initializer` immediately and received one
canonical map whether the source used a map literal or `(hash-map ...)`. The
remaining probes investigated the real registry schema: whether `:args` held a
map and whether `:pair` lived on the operation specification. Those are
semantic questions, not representation accidents.

Candidate v7 clears the 10% neighboring-win threshold on the primary metric and
improves every secondary metric without weakening correctness, source safety,
or tests. Keep it as the new hill-climb leader. Do not yet claim one-shot or
local maximality: the median still contains five shell calls.

## Candidate v8: remove schema questions with ordinary Clojure

The v7 transcripts exposed one remaining repeated detour. Agents first wrote
an analysis that assumed `:args` was sequential:

```clojure
(mapcat :args (vals registry))
```

In this registry, `:args` is a map, so that expression returns map entries,
not argument-spec maps. Agents then spent calls asking whether `:args` values
were maps or vectors, inspecting key frequencies, and repairing the traversal
to `(mapcat (comp vals :args) ...)`. The answer was correct, but the route was
not one-shot.

The wrong response would be another repository-specific operator such as
`arg-specs`. That would move domain knowledge into clj-surgeon, enlarge the API,
and teach the model less reusable machinery. The more general response is
already in Clojure: when container shape is irrelevant, recursively traverse
the data and select by meaning.

```clojure
(->> (tree-seq coll? seq registry)
     (filter map?)
     (filter #(= true (:required %)))
     count)
```

Candidate v8 therefore exposes pure `clojure.core/tree-seq` inside the SCI
analyzer and gives one short instruction: use `tree-seq coll? seq` with a
predicate when nested map-versus-vector shape is irrelevant. This is not a new
X-ray algebra term. It is ordinary Clojure operating on the stable selection
vector and canonical collection view. The implementation remains ignorant of
`:args`, `:required`, operation registries, and every future application schema.

The hypothesis is deliberately narrow: a familiar, general traversal should
prevent the model from asking a representation question it does not need to
answer. The risk is equally clear. `tree-seq` can visit maps, map entries, and
their contents, so a vague predicate may count unintended nested values. Exact
cardinality and domain predicates remain the agent's responsibility. The tool
must not silently guess what a record is.

Candidate v8 is not yet the leader. It survives only if clean-context agents
stay exact and beat or closely approximate v7 on wall time, with shell calls,
source output, and tokens as secondary evidence. Until that parallel benchmark
passes, v7 remains the proven maximum and this section records an experiment,
not a product claim.

### Candidate v8 result: near win, weak activation

The first valid adjacent pilot lost badly: v8 was exact but took 89.9 seconds
and eight calls versus v7 at 62.6 seconds and five calls. A different pilot
showed a tantalizing exact v8 route in one source call and 23.2 seconds, but its
control collided with an accidentally concurrent benchmark resume. Exclude
that entire directory from comparisons. It is qualitative evidence that the
route is possible, not timing evidence.

The clean four-replicate parallel gate was much closer:

| Metric | v7 leader | v8 `tree-seq` | Change |
|---|---:|---:|---:|
| Correct | 4/4 | 4/4 | Tied |
| Median wall | 47.626 s | 43.805 s | 8% faster |
| Median shell calls | 5 | 5 | Tied |
| Median input tokens | 103,588 | 94,241 | 9% fewer |
| Median uncached tokens | 19,201 | 18,302 | 5% fewer |
| Median output tokens | 1,619 | 1,562 | 4% fewer |
| Median source output | 5.3 KB | 5.1 KB | 2% fewer |

Every run was independently exact and preserved the source hash. However, only
one of four v8 agents used `tree-seq`, and it did so after other attempts. The
other three followed schema-specific routes despite reading the skill. The 8%
wall improvement therefore cannot be attributed confidently to the new
capability and does not clear the 10% neighboring-win rule.

This exposes an activation gap, not an algebra gap. The capability is general,
small, and safe, but the instruction “traverse with `tree-seq coll? seq`” does
not reliably turn into action. One final bounded neighbor will state the route
as executable policy:

```text
When a predicate identifies the desired descendants, skip a separate
map-versus-vector query. Use (filter predicate (tree-seq coll? seq value)).
```

If that wording does not make the route reliable and improve wall time, stop
trying to retrain the model. Keep v7 as the measured product leader and treat
native traversal as optional Clojure capability rather than the headline.

### Candidate v9 result: decisive activation win

The clarified instruction made every clean agent use `tree-seq`, compared with
one of four under v8. The four-replicate gate against v7 was exact throughout:

| Metric | v7 leader | v9 contract | Change |
|---|---:|---:|---:|
| Correct | 4/4 | 4/4 | Tied |
| Median wall | 50.299 s | 39.432 s | 22% faster |
| Median shell calls | 5 | 5 | Tied |
| Median input tokens | 104,339 | 94,271 | 10% fewer |
| Median uncached tokens | 15,966 | 14,783 | 7% fewer |
| Median output tokens | 1,568 | 1,382 | 12% fewer |
| Median source output | 5.5 KB | 4.5 KB | 17% fewer |

Every source hash was unchanged. One v9 agent read the skill, issued one X-ray,
and returned the exact result in 31.6 seconds. The wording therefore crossed
both gates: reliable activation and more than 10% median wall improvement. v9
replaces v7 as the measured leader.

The other transcripts exposed the next substrate defect. Two agents wrote
idiomatic, pure Clojure on their first attempt, but SCI refused it because the
allowlist omitted `for` in one run and `val` in another. Both agents recovered,
but the tool had forced valid Clojure into a smaller undocumented dialect. That
contradicts the product promise and the earlier instruction to let the model
use Clojure rather than inventing another query language.

Candidate v10 states the a-priori data contract directly:

```text
analyze input = one Vector of ordinary Clojure data
```

It tells the caller to write one total function over that contract instead of
issuing a separate shape-discovery query. It also admits the missing general,
pure core vocabulary—`for`, `key`, and `val`—and turns both rejected first
expressions into permanent pure regressions. It does not expose `type`: class
objects are not EDN, and collection predicates express the safe distinction.

`for` required a real safety design rather than one allowlist entry. The macro
expands into `lazy-seq`, `loop*`, `recur`, and chunk machinery. Admitting those
symbols directly made the existing nontermination regression hang, exactly as
that test was designed to reveal. Candidate v10 therefore separates public
source vocabulary from private macro-expansion vocabulary. A pre-expansion
check still refuses direct `loop`, `recur`, `lazy-seq`, and chunk internals;
SCI may use them only behind an authored `for`. The exact comprehension now
succeeds while `(loop [] (recur))` still refuses before source I/O.

This is the sharper answer to “make probing unnecessary.” The tool cannot know
every application's field semantics a priori. It can make representation fully
predictable, let one function branch or traverse over ordinary data, and stop
rejecting the Clojure already nearest in the model's latent space.

### Candidate v10 result: the Clojure substrate wins

The four-run parallel comparison against v9 was exact and source-preserving on
both sides:

| Metric | v9 leader | v10 pure Clojure | Change |
|---|---:|---:|---:|
| Correct | 4/4 | 4/4 | Tied |
| Median wall | 50.025 s | 37.552 s | 25% faster |
| Median shell calls | 5 | 3 | 2 fewer |
| Median input tokens | 104,470 | 65,982 | 37% fewer |
| Median uncached tokens | 20,810 | 14,782 | 29% fewer |
| Median output tokens | 1,488 | 1,188 | 20% fewer |
| Median source output | 5.9 KB | 2.7 KB | 53% fewer |

One v10 agent was exactly one-shot after reading the skill: one X-ray, 22.9
seconds, and 1.4 KB of source output. None of the four happened to use `for`;
one used the newly admitted `val`. All four used `tree-seq`. The performance
gain therefore belongs mainly to the explicit analyzer-input contract and
reliable shape-independent traversal. Safe `for` support remains justified by
the earlier first-attempt field failure and its permanent regression, not by
claiming credit for this timing result.

Three agents still made semantic validation calls. Two first used a
shape-dependent `mapcat :args`; another returned the correct whole-registry
`tree-seq` answer and then verified that no unrelated `:required` map had been
counted. That caution is rational. The caller knows the domain phrase
“argument specs,” while clj-surgeon knows only data.

Candidate v11 tests the general Clojure solution: scope traversal below known
semantic parents and traverse their children in the same comprehension.

```clojure
(for [parent parents
      node (tree-seq coll? seq (child parent))
      :when (predicate node)]
  node)
```

This is not an operation-registry helper. It works for any irregular nested
data, uses ordinary Clojure, handles map or vector children uniformly, and
keeps the domain boundary in the model. It survives only if it reduces the
remaining source calls and clears the 2 KB median source-output gate without
slowing the v10 leader.

### Candidate v11 result: reject prescriptive comprehension guidance

The scoped-comprehension treatment was exact in all four runs, and every agent
used `for`. It nevertheless lost decisively:

| Metric | v10 leader | v11 scoped `for` | Change |
|---|---:|---:|---:|
| Correct | 4/4 | 4/4 | Tied |
| Median wall | 27.529 s | 35.411 s | 29% slower |
| Median shell calls | 3 | 4 | 1 more |
| Median input tokens | 56,458 | 75,547 | 34% more |
| Median output tokens | 947 | 1,300 | 37% more |
| Median source output | 2.1 KB | 3.4 KB | 60% more |

The example activated syntax, not semantics. One agent wrote a nested `for`
directly over `(:args spec)`. Because that value was a map, each iteration
bound a map entry rather than an argument-spec map and produced the wrong
intermediate count. The agent recovered, but only after help and more calls.

Keep `for`, `key`, and `val` in the safe pure-Clojure substrate: they remove
real dialect traps and have permanent tests. Remove the prescriptive `for`
example from primary help and skills. v10 remains the leader. The lesson is
the Bitter Lesson again: make general computation available, state the stable
data contract, and avoid prompt-level recipes that merely move the guess.

### Candidate v12: make the skill pay rent

The canonical skill reached its tested 240-line ceiling. Every clean session
reads it before touching source, including roughly 130 lines about operations
irrelevant to the computed-read task. That context costs wall time and tokens
even when cached, and it competes with the actual program for attention.

Candidate v12 applies progressive disclosure. A 107-line primary skill keeps:

- the smallest-read router (`:ls`, `:cat`, `:grep-form`, `:xray`);
- the complete analyzer input, canonical-data, and `tree-seq` contract;
- exact X-ray, edit, transform, partition, and compatibility examples;
- plan/apply separation, refusal, read-back, and receipt safety;
- the sibling and `outermost` placement rules agents repeatedly guessed.

Dependency, extraction, declare, move, rename, and CLJC workflows remain in the
existing advanced-operations reference and are loaded only for those tasks.
All 1,219 assertions across the five agent-surface test namespaces pass, and
the skill validator accepts the package. The permanent ceiling tightens from
240 to 120 lines. The candidate survives only if clean agents remain exact and
beat or match v10 on wall time; brevity cannot purchase forgotten safety.

### Candidate v12 result: compact context pays rent

The 107-line skill preserved exactness and source hashes in all four runs while
crossing the plan's compact-evidence gate:

| Metric | v10, 240 lines | v12, 107 lines | Change |
|---|---:|---:|---:|
| Correct | 4/4 | 4/4 | Tied |
| Median wall | 28.285 s | 26.004 s | 8% faster |
| Median shell calls | 3 | 2 | 1 fewer |
| Median input tokens | 66,217 | 44,819 | 32% fewer |
| Median uncached tokens | 12,637 | 11,795 | 7% fewer |
| Median output tokens | 984 | 874 | 11% fewer |
| Median source output | 2.7 KB | 1.5 KB | 43% fewer |

Three of four compact-skill agents were truly one-shot after the required skill
read. The fourth made one validation call. The wall gain is below the automatic
10% rule, but the treatment wins every secondary measure, removes 133 always-
loaded lines, passes all safety-surface tests, and makes the documented product
smaller. Under the audit tie-break, v12 replaces v10 as the product leader.

Candidate v13 is the final compression neighbor. It removes another 103 words
and 711 bytes while preserving every tested route and safety contract: 89
lines, 563 words, and 4,050 bytes. The permanent line ceiling becomes 90. If
this final trim does not deliver another 10% wall win, the hill is locally
maximal under the audit rule; keep the fastest compact variant and stop shaving
prompt prose.

### Candidate v13 result: the hill is locally maximal

All eight sessions in the final adjacent comparison were exact, unchanged, and
one-shot after the required skill read:

| Metric | v12, 107 lines | v13, 89 lines | Change |
|---|---:|---:|---:|
| Correct | 4/4 | 4/4 | Tied |
| Median wall | 23.764 s | 24.462 s | 3% slower |
| Median shell calls | 2 | 2 | Tied |
| Median input tokens | 44,702 | 44,480 | <1% fewer |
| Median uncached tokens | 9,630 | 7,360 | 24% fewer |
| Median output tokens | 723 | 802 | 11% more |
| Median source output | 1.4 KB | 1.4 KB | 2% more |
| Median total tool output | 6.2 KB | 5.5 KB | 11% fewer |

There is no wall-clock win to claim. The 698 ms difference is small relative
to model-service variance, while both treatments achieved the ideal two-command
session in every run. v13 retains every tested concept and safety contract in
18 fewer lines and 711 fewer bytes. Under the close-result rule, keep v13 for
the smaller always-loaded artifact; do not claim it is faster than v12.

The local-maximality condition is now satisfied. After v10 became the last
candidate to improve median wall by more than 10%, three bounded neighbors did
not produce another such gain: prescriptive scoped `for` lost by 29%, the
107-line skill won by 8% plus compelling one-shot/simplicity evidence, and the
89-line skill tied within noise. Every candidate preserved correctness and
source bytes, and rejected designs remain tagged and documented.

The final product shape is therefore:

```text
:xray        one structural read plus one caller-supplied terminating function
analyze      Vector<ordinary Clojure data> -> bounded EDN
tree-seq     shape-independent nested traversal
:edit        the same Clojure path, producing a reviewed immutable plan
skill        89-line router; advanced workflows load on demand
```

This is not proof that clj-surgeon can never improve. It is proof that this
bounded hill climb has reached its stopping rule. Further work needs a new
task, a new field failure, or a stronger competing design—not more prose
shaving on this benchmark.

The likely destination is one Clojure substrate with a tiny Unix façade:

```text
:xray     universal structural read and pure computation
:cat      one known top-level form
:ls       compact namespace outline
:grep-form optional structural-search shortcut
```

Whether `:grep-form` survives primary documentation is an empirical question.
Whether compatibility aliases survive is a maintenance question. Neither is a
reason to make clean agents learn two query languages.

## The new hill-climb rule

Correctness and unchanged source are gates. Among correct designs, compare
wall time first, then shell calls, source output, tokens, and recovery behavior.
If two designs are close, prefer the one with fewer documented concepts.

Call the API locally maximal only after three bounded neighboring experiments
fail to improve median wall by at least 10% without weakening correctness,
safety, or the permanent tests.

The feature was worth building. The product is not done.

## The current release beats the July baseline

The release comparison used the newest origin commit available on or before
July 28. That commit was `cc0f306` from July 12; there was no newer commit in
the requested interval. The current candidate was `477dca9`. Each lane used
GPT-5.6 sol at medium reasoning, four independent clean sessions per task, and
the skill that matched its executable. The native control had neither the
clj-surgeon executable nor its skill.

Correctness is a gate. The efficiency medians below include only correct runs.
“Exact” also checks the requested presentation for reads and every source byte
for edits.

| Task | Lane | Correct | Exact | Median wall | Shell calls | Input tokens | Source output |
|---|---|---:|---:|---:|---:|---:|---:|
| Named-form read | July baseline | 4/4 | 4/4 | 28.792 s | 4 | 72,000 | 6,720 B |
| Named-form read | Current | 4/4 | 4/4 | 22.709 s | 2 | 43,971 | 1,461 B |
| Named-form read | Native control | 4/4 | 4/4 | 23.949 s | 2 | 41,379 | 2,322 B |
| Semantic form read | July baseline | 4/4 | 4/4 | 39.444 s | 5 | 82,806 | 7,883 B |
| Semantic form read | Current | 4/4 | 4/4 | 28.384 s | 2 | 44,016 | 1,531 B |
| Semantic form read | Native control | 4/4 | 4/4 | 27.347 s | 3 | 56,778 | 4,244 B |
| Structural search | July baseline | 4/4 | 3/4 | 35.959 s | 4 | 76,213 | 663 B |
| Structural search | Current | 4/4 | 2/4 | 24.222 s | 2 | 43,672 | 596 B |
| Structural search | Native control | 4/4 | 3/4 | 40.627 s | 3 | 56,285 | 844 B |
| Guarded edit | July baseline | 4/4 | 4/4 | 53.502 s | 8 | 176,294 | 37,443 B |
| Guarded edit | Current | 4/4 | 4/4 | 52.832 s | 8 | 116,114 | 10,542 B |
| Guarded edit | Native control | 0/4 | 0/4 | — | — | — | — |

For the three read tasks, current clj-surgeon was 21%, 28%, and 33% faster
than the July baseline. It cut median source output by 78% and 81% on the two
form-read tasks. The guarded edit tied on wall time while using 34% fewer input
tokens and 72% less source output.

The native control is the important check against self-congratulation. It tied
current clj-surgeon on the easy named-form read and was 4% faster on the
semantic read, although it used one more call and 2.8 times as much source
output. Current clj-surgeon was 40% faster on structural search. On the edit,
all four native agents changed the requested token but also deleted an
unrelated trailing blank byte. The exact-byte preservation gate therefore
scored all four runs incorrect. Both clj-surgeon lanes were exact in all eight
edit runs.

The structural-search presentation regression also remains visible: current
returned the requested exact presentation in two of four runs, versus three of
four for both comparators. The answers were structurally correct, but this is
not a metric to hide.

## Claude validates the same safety contract

A separate 2-by-2 clean-context matrix exercised one computed read and one
guarded edit with both Claude Fable and Claude Opus. All four runs were exact.

| Model | Task | Correct | Wall time |
|---|---|---:|---:|
| Fable | Guarded edit | yes | 25.677 s |
| Opus | Guarded edit | yes | 26.220 s |
| Fable | Computed X-ray | yes | 36.971 s |
| Opus | Computed X-ray | yes | 41.274 s |

The edit prompt asked only for the `:status` value to change from `:done` to
`:complete`; the fixture deliberately retained an adjacent audit payload and
comment. Each agent planned and applied in separate commands, produced the
exact expected bytes, and left the repository source unchanged. These timings
are not yet a Claude-versus-Codex comparison because the existing Codex runs
used different fixtures. The repository-owned matched-agent Make targets close
that measurement gap.

## A live production session split the read and edit verdicts

An anonymized production session supplied a different kind of evidence. The
agent diagnosed and repaired a real data-display failure across several large
Clojure namespaces. This section omits the source repository, domain, user
data, paths, storage systems, and source-specific identifiers.

The relevant source set contained 7,014 lines and 318,455 bytes. The session
used clj-surgeon in 29 shell actions. Those command texts contained 42
clj-surgeon occurrences, including parallel calls and help calls. Associated
process receipts accounted for approximately 8.8 seconds of execution and
25,815 output tokens.

| Surface | Observed result | Assessment |
|---|---|---|
| Structural reading | The agent read exact forms from several large namespaces instead of dumping complete files. | High value |
| Runtime diagnosis | Persistent evaluation and browser checks found the missing behavioral contract. | Complementary; source structure was not runtime truth |
| Exact existing-form edit | One small vector change used a hash-fenced plan and verified apply. | Safe, but two calls |
| Custom-macro edit | The caller tried several selectors, requested help, broadened the replacement, and later repaired formatting. | Too costly |
| Source preservation | A seven-line form became one dense line before final diff review caught it. | Failed the one-shot quality goal |
| Shell safety | An unquoted `>` in a form name redirected an error receipt into an unintended file. | Guidance gap |

The reading lens earned a 9/10 assessment. It bounded source exposure and made
structural ownership visible. The mutation surface earned 5/10. It remained
safe, but it stopped composing at a custom defining macro and forced a broad
replacement. Overall utility was 8/10 because runtime diagnosis dominated the
session and the final change was correct.

There was no paired native-control run, so token savings are estimates rather
than benchmark results. Reading all relevant files would have exposed roughly
75,000 to 85,000 tokens. A competent `rg` and narrow-range workflow would
probably have exposed 35,000 to 50,000. The observed tool-bearing receipts
contained about 25,800 output tokens. The likely saving is therefore 10,000 to
25,000 tokens against a competent caller and more against a full-file reader.

Wall-clock benefit was smaller. The complete session took approximately 13.5
minutes, while tool-bearing process execution took less than nine seconds.
Runtime and browser verification consumed most of the elapsed time. The custom-
macro editing detour consumed roughly 70 seconds and erased much of the source-
navigation gain. Without clj-surgeon, a direct patch could have been faster but
would not have supplied structural ambiguity checks, immutable plans, or read-
back hashes.

At observation time, the session established a sharper product boundary:
clj-surgeon was an excellent source lens, but it was not the default mutation
surface for every Clojure form. The next acceptance case had to select a leaf
inside an unnamed custom top-level macro, apply one `:expect`-guarded edit,
preserve surrounding layout and comments, and create no unintended files when
a form name contained shell metacharacters.

### The containing-line root closed the observed edit gap

The remediation adds `(line N)` and `[:line N]` as top-level roots for X-ray
and edit paths. A line can identify the opening line, an interior line, the
closing line, or a contiguous comment attached above one top-level form. The
root does not infer a custom macro name and does not select the nested leaf. A
following `match` or navigation step performs that selection.

The faithful fixture contains three unconfigured custom macro forms with the
same nested call. The accepted one-call command selects the middle owner by an
interior line, matches its call, declares the exact old call with `:expect`,
and replaces only that call. The test asserts the complete expected file bytes:
the other two calls, attached top-level comment, nested comment, and multiline
layout remain unchanged.

The refusal matrix covers invalid and misplaced roots, blank gaps, ambiguity
inside one owner, two reader-conditional owners on one line, and more than 100
same-line owners with bounded candidate evidence. A separate Zsh regression
runs a generated command for a form name containing `>` and proves that the
shell creates no redirected file. This closes both failures without teaching
the tool source-specific macro semantics.

A fresh-context agent then read only the repo-local skill and received the
fixture path, target line, and requested leaf change. Its first and only edit
call used `(line 14)` with the exact old leaf in `:expect`. It called no help,
performed no source read, and used no text editor. The guarded receipt verified
the write, and byte comparison showed equal file lengths with exactly three
changed bytes: `old` became `new`. Every unrelated byte remained exact.

### One-call guarded edits worked; source spelling did not

A second anonymized production session used the installed `0461f72` release.
The target forms had stable names, so `(form 'NAME)` was the correct root.
Forcing the new `(line N)` root would have made the selectors less semantic.

The session issued four guarded write calls. Every call used `:edit`, an exact
literal `:expect`, and `:plan-out`. Each call planned, applied, parsed the whole
result, wrote atomically, and returned a matching read-back hash. The agent made
zero `:replace-subform!` calls and never replayed a saved plan. This is the
intended replacement for the earlier two-action plan-and-apply ceremony.

| Surface | Observed result | Assessment |
|---|---|---|
| Root selection | Named-form roots and exact nested matches each selected one subtree. | Correct |
| Guarded write | `:edit` plus `:expect` completed planning and application in one call. | Correct |
| Receipt use | The agent did not reopen or replay a plan, but it made one unnecessary 209-line form read after a verified receipt. | Better, not minimal |
| Source spelling | Reader shorthand in the replacement became its expanded `fn*` representation. | Semantically valid, not source-faithful |
| New-form layout | A compound replacement was initially emitted on one line. | Required formatting |
| Final scope | The source diff was one five-line hunk, and the behavioral gate passed with no browser errors. | Correct |

The source-spelling failure caused the main detour. The replacement expression
contained an anonymous-function shorthand. The Clojure reader expanded that
shorthand before clj-surgeon rendered the replacement, so the source received
an equivalent `fn*` form. The agent used another guarded structural edit to
replace that form with an existing named function value. The result was simpler,
but the repair should not have been necessary.

Formatting exposed a second boundary. The repository formatter changed a
legacy 1,920-line source file and produced a 2,632-line diff for a four-line
feature. The agent reversed the complete file diff, reapplied the guarded edit,
and used a text patch only to arrange the four inserted lines. The final diff
was exact, and a real browser verified the complete behavior. However, reversing
a complete file diff is safe only when that file has no unrelated prior edits.
It is not an acceptable general recovery procedure for a dirty worktree.

The mutation assessment rises from 5/10 in the first production observation to
8/10 here. Structural selection, expectation fencing, atomic application, and
receipt verification now compose correctly. Source spelling and local layout
still prevent clj-surgeon from being the exclusive editing surface.

The next acceptance case should require all of the following results:

1. A replacement that contains reader shorthand preserves that shorthand in
   the emitted source.
2. A caller can provide an exact multiline replacement layout without changing
   unrelated bytes.
3. Formatting remains local to the changed form or replacement region.
4. A successful guarded receipt causes no verification-only source reread.
5. Recovery reverses only the agent's own edit, even when the file was already
   dirty.

### Literal source retention closed the reader-expansion gap

The repair keeps two representations during expression compilation. The
evaluated query remains the semantic authority. The lossless rewrite-clj tree
supplies the terminal replacement's raw source. SCI parses that raw source
without evaluating it. clj-surgeon retains the source only when the parsed form
equals the evaluated replacement value.

This is preservation, not reverse inference. The implementation does not guess
that an arbitrary `fn*` form came from anonymous-function shorthand. An
explicit `fn*` stays explicit. A computed replacement and the `:query` data
surface continue to use canonical printing because they have no caller-provided
lexical source.

The first regression run failed in the expected field shape. The file, diff,
plan edit's `:after`, and receipt's `:applied-edit :after` all contained
`(fn* [%1] (str %1))` instead of `#(str %)`. After the repair, the same guarded
CLI call preserved `#(str %)` in each source-bearing field. The selector query
remains semantic data and can display the equivalent `fn*` value. The receipt
verified a whole-file parse, atomic write, and matching read-back hash. The Git
diff changed one map and no unrelated byte.

The adversarial matrix also covers shorthand inside a larger conditional, two
shorthand functions in `replace-span`, direct and threaded builders, explicit
`fn*`, a quoted fake `replace` call, computed fallback, and a multiline form
with a comment and var quote. The multiline test proves that the mechanism
preserves exact layout and reader syntax rather than special-casing `#()`.

This closes the source-spelling and inserted-layout gaps inside clj-surgeon.
Repository-wide formatter churn and recovery of an agent's changes in an
already dirty file remain separate workflow problems.

The complete gate passed 483 tests and 3,810 assertions: five tests and 54
assertions more than the preceding release. The new tests failed before the
implementation, and no existing expectation was weakened. Formatter,
clj-kondo, benchmark harness, retention, evidence-manifest, skill-validation,
and agent-text gates also passed.

## Audit checkpoint

The earlier audit boundaries are now remediated:

1. The 20 valid raw-run directories are archived with hashes, and the evidence
   verifier checks the manifest, archive set, hashes, and explicit exclusions.
2. Fable and Opus have passed the clean-context read/edit matrix.
3. Stable installation now creates content-addressed, read-only copies with
   receipts. `make install-dev` is the explicit branch-live mode.
4. `skills/clj-surgeon/` is the single authoritative skill package. The native
   Claude package is byte-identical, and the compact root file is a legacy
   entrance with only its valid relative reference changed.
5. The quoted-data regression has a permanent exhaustive test: executable
   sandbox symbols refuse before source I/O, while the same symbols inside
   quoted program data remain searchable.

The exact branch state, benchmark manifest, review findings, and takeover
instructions remain in [the maximality handoff](../plans/xray-maximality-handoff.md).
