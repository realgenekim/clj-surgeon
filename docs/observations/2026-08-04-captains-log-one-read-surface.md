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

```text
                         full evidence     compact evidence
median, 8 alternating        292 ms             193 ms
output                        29.1 KB             1.4 KB
change                                             -34% wall
                                                   -95% bytes
```

That proved transport and printing improved. It did not prove agent wall time
improved.

## The real-program pilot said no

The new task asked clean agents to compute category frequencies, the total
number of required arguments, and sorted paired operations from the real
`ops-registry` hash-map. An independent rewrite-clj scorer derived the expected
answer. Any source mutation failed the run.

The first version-matched pilot was correct on both sides:

```text
released X-ray       72.7 s     5 shell calls     61.1 KB source output
compact xray-one     75.3 s     8 shell calls      9.2 KB source output
```

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

```text
:q | bb          median 101.2 s     9 shell calls
released X-ray          72.7 s      5 shell calls
compact candidate       75.3 s      8 shell calls
```

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
