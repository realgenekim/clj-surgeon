# The Code Reader/Explorer Frontier

**Status:** Exploration portfolio, not an accepted implementation plan

## The product ambition

clj-surgeon should feel less like a collection of syntax commands and more like
an expert editor's structural instrument panel. The model supplies questions
and judgment. The kernel owns parsing, addresses, snapshots, counts, evidence,
and compact receipts.

The performance target is cognitive as much as temporal:

```text
ask one coherent question
  -> receive one bounded, trustworthy answer
  -> keep exploring from durable evidence
```

A reader should not need to remember which files were inspected, copy source
between calls, reconcile facts obtained from different snapshots, or reread
code merely to recover context that the tool already possessed.

The benchmark evidence points to this frontier. A guarded write transaction
can complete in about 90 milliseconds while the complete agent task still
takes tens of seconds. Naturalistic work has also produced long runs of
independent structural reads and hundreds of thousands of returned source
characters. The opportunity is therefore not a marginally faster parser. It
is fewer model/tool boundaries, less source in working context, and continuity
between perception and action.

## The central amplifier: a snapshot-bound decision workspace

The immediate complement to the intent compiler is a workspace of immutable
structural selections: Vim marks and Emacs markers, but hash-bound and
proof-carrying.

```text
batch inspection
  -> named selections on one source snapshot
  -> one guarded decision transaction
  -> one terminal verification receipt

refusal
  -> one stable diagnostic
  -> one violated assertion
  -> one minimal machine-readable fix-it
```

`inspect_clojure` could return named selection handles rather than requiring
the caller to copy exact source into a later mutation request:

```clojure
{:snapshot "snap-7d91"
 :selections
 {:body-slots {:handle "sel-a1" :matches 2 :selection-hash "..."}
  :title-arg  {:handle "sel-b4" :matches 1 :selection-hash "..."}}}
```

`apply_clojure_changes` could then consume those handles with declared
operators and aggregate counts. The compiler would own concrete addresses,
stale-snapshot checks, write ordering, parsing, rollback, and receipts. When a
prompt already supplies the complete decision, the existing direct one-call
route remains shorter; handles exist for work where inspection genuinely
informs judgment.

The power-editor correspondence is useful:

| Editor capability | Structural counterpart |
|---|---|
| Text objects and narrowing | File, owner, path, and span scopes |
| Marks and markers | Snapshot-bound selection handles |
| Operator + motion + count | Edit operator + structural selection + expectation |
| `:global`, `:argdo`, `:bufdo` | Selection sets across owners and files |
| `.` and keyboard macros | One transform over a complete selection set |
| Registers | Exact captured source retained by the kernel |
| Quickfix | Structured diagnostics with explicit fix-its |
| Undo tree | Hash-fenced inverse receipts |

The workspace must not become hidden mutable editor state. Snapshots,
selections, failed compilations, and fix-its must be immutable,
content-addressed, serializable, bounded, and explicit in every consuming
request.

## The tree needs call graphs

A tree without call graphs is a table of contents. The tree says where code
lives; graph edges say why a node matters and what could be affected by a
change.

The tree should remain the navigational spine because containment gives every
node one understandable place:

```text
project -> namespace -> top-level owner -> branch -> exact syntax
```

Typed graph edges should overlay that spine:

```text
entry roots
    |
    v
transitive callers
    |
    v
direct callers -> selected node -> direct callees
                                      |
                                      v
                              effect boundaries
                         database / network / state / UI
```

The two navigation families then remain small and composable:

```text
up / down / left / right       structural space
follow :callers                relationship space
follow :callees
follow :tests
follow :history
follow :runtime
```

### The a-priori node dossier

Before opening a form's body, a reader should be able to request one compact
dossier that answers:

> Where am I? How can execution reach this? Where can it go? What state can it
> touch? Which evidence supports those claims?

For example:

```clojure
{:node app.state/transition
 :kind :defn
 :location {:file "src/app/state.clj" :lines [42 91]}
 :signature {:args [[state event]] :visibility :public}

 :called-by
 [{:node app.events/dispatch!
   :sites 1
   :edge :direct-var-call}
  {:node app.replay/replay-event!
   :sites 1
   :edge :direct-var-call}]

 :calls
 [{:node clojure.core/assoc
   :sites 4
   :effect :pure}
  {:node app.audit/record-transition!
   :sites 1
   :effect :stateful}
  {:node app.events/publish!
   :sites 1
   :effect :external-io}]

 :dynamic-calls
 [{:site "handler invocation"
   :resolution :unknown
   :reason :map-selected-callback}]

 :reachable-from
 [{:root app.server/handle-event
   :shortest-path
   [app.server/handle-event
    app.events/dispatch!
    app.state/transition]}]

 :tests
 [{:node app.state-test/transition-finishes-test
   :relationship :direct-call}
  {:node app.events-test/dispatch-test
   :relationship :transitive}]

 :state {:reads [:event/type :status]
         :writes [:status]}

 :evidence
 {:source-snapshot "7d91..."
  :static-authority :clj-kondo
  :runtime-authority nil
  :unresolved-edge-count 1}}
```

The default should be the node's bounded neighborhood, not the complete
transitive graph. The reader can expand surprising callers, request witness
paths from selected roots, or follow one edge to an effect boundary.

### Reuse clj-kondo; do not rebuild its analyzer

clj-kondo already exports most of the expensive static facts needed to build
the first graph:

- namespace definitions and usages;
- var definitions;
- resolved var usages with `:from`, `:from-var`, `:to`, and `:name`;
- call-shaped usages identified by `:arity`;
- exact source ranges and CLJ/CLJS platform identity;
- local definitions and usages;
- protocol methods and implementations;
- multimethod dispatch information;
- keywords, Java references, and hook-supplied context.

Its official
[analysis-data contract](https://cljdoc.org/d/clj-kondo/clj-kondo/2025.09.19/doc/analysis-data)
explicitly supports building external tools such as var-usage search,
namespace graphs, unused-var analysis, and AST views. Its
[JVM API](https://cljdoc.org/d/clj-kondo/clj-kondo/2026.04.15/doc/running-on-the-jvm)
allows a persistent server to call `clj-kondo.core/run!` in-process rather than
starting one subprocess per graph query.

A var usage already contains enough information for a direct static edge:

```clojure
;; clj-kondo analysis record
{:from app.events
 :from-var dispatch!
 :to app.state
 :name transition
 :arity 2
 :filename "src/app/events.clj"
 :row 42
 :col 5}

;; normalized graph edge
{:from app.events/dispatch!
 :to app.state/transition
 :type :direct-var-call
 :arity 2
 :site {:file "src/app/events.clj" :row 42 :col 5}}
```

Reversing the edge index produces `:called-by`. Pure graph transformations
then compute namespace neighborhoods, fan-in and fan-out, strongly connected
components, bounded transitive closures, and shortest witness paths.

A bounded probe at this document's checkpoint used the locally installed
clj-kondo `2023.10.20` against `src/clj_surgeon/core.clj`. It returned 50 var
definitions and 957 var usages, including the resolved call from
`clj-surgeon.core/run-outline` to `clj-surgeon.outline/outline` with its arity
and source location. The product integration should pin and test a modern
clj-kondo library instead of accidentally freezing itself to that older local
binary.

### Ownership boundary

| Capability | Authority |
|---|---|
| Namespace alias and Var resolution | clj-kondo |
| Call-shaped var usages | clj-kondo |
| Locals and lexical usages | clj-kondo |
| Protocol implementation records | clj-kondo |
| Project-specific macro models | clj-kondo configuration and hooks |
| Lossless source, comments, and exact nodes | clj-surgeon and rewrite-clj |
| Snapshot-bound node handles | clj-surgeon |
| Forward and reverse graph indexes | clj-surgeon |
| Witness paths, closures, SCCs, and fan-in/out | clj-surgeon |
| Candidate-future graph | clj-surgeon rerunning analysis on compiled future files |
| Loaded and observed calls | a named nREPL or runtime tracer |
| Historical relationships | Git |
| Compact agent-facing dossier | clj-surgeon MCP |

The join between clj-kondo and clj-surgeon is important. clj-kondo's hooks use
rewrite nodes that intentionally omit whitespace, which is appropriate for
analysis but insufficient for comment- and layout-preserving edits. Its
[hooks contract](https://cljdoc.org/d/clj-kondo/clj-kondo/2026.04.15/doc/hooks)
can teach the analyzer about unfamiliar macros and attach application-specific
context. clj-surgeon should join each resulting usage range to the smallest
containing lossless node on the same source snapshot:

```text
clj-kondo resolved usage
  + file / range / platform
  + lossless source tree and hash
  -> proof-carrying call-site node
```

Every graph edge then lands on a node that can be expanded, compared, marked,
guarded, or used as an edit target.

### Three call graphs, never one false truth

Clojure's dynamic features prevent any static analyzer from producing a
complete call graph. The atlas should preserve three separate evidence layers:

1. **Statically possible.** Resolved Vars, protocols, known multimethods,
   callbacks modeled by hooks, and unresolved dynamic sites.
2. **Currently loaded.** Vars, multimethod tables, protocol extensions, and
   registrations in one explicitly named runtime.
3. **Actually observed.** Bounded call and branch evidence from one explicitly
   named test, request, or trace window.

The differences are useful:

```text
statically possible - loaded       missing registration or namespace
loaded - observed                  possible but unexercised path
observed - statically resolved     dynamic-analysis blind spot
```

Every edge should name its type and authority:

```clojure
:direct-var-call
:var-reference
:local-call
:macro-expansion
:protocol-dispatch
:multimethod-dispatch
:higher-order-reference
:callback-registration
:data-driven-handler
:reflective-resolution
:java-interop
:cljs-interop
:generated-usage
:unresolved
```

Zero statically resolved callers must never be rendered as “dead.” It means
only that the declared static authorities found no edge. Calls through maps,
registries, `apply`, `requiring-resolve`, protocols, reflection, macros, and
`eval` require explicit uncertainty or another authority.

### Candidate-state graph review

The transaction compiler already constructs complete future files before it
writes them. The same clj-kondo analysis can run over that candidate snapshot
and produce a bounded graph delta:

```clojure
{:call-graph-delta
 {:edges-added
  [[app.orders/create! app.audit/record!]]

  :edges-removed
  [[app.orders/create! legacy.audit/write!]]

  :newly-unreachable
  [legacy.audit/write!]

  :new-unresolved-sites
  []}}
```

This does not claim the future program behaves correctly. It answers the
mechanical review question: did the candidate relationship structure change
as declared?

The architecture is therefore:

```text
rewrite-clj lossless tree -----------+
                                      +-> join by snapshot and range
clj-kondo analysis records ----------+
                                      |
                                      v
                           proof-carrying code graph
                                      |
                                      v
                 tree / callers / callees / paths / impact views

named nREPL ---------------- optional loaded and observed overlay
Git commits ---------------- optional history overlay
```

First experiment: create one pure normalizer from clj-kondo analysis records
to typed forward and reverse edges, join those edges to existing clj-surgeon
form ranges, and expose a read-only dossier for one named top-level form. Test
direct calls, non-call references, macros, locals, protocols, CLJC platforms,
generated usages without locations, and unresolved dynamic sites. Then replay
five real caller/callee investigations and compare correctness, source reads,
tool actions, evidence bytes, and complete wall time with native search.

## Ten other big bets

These ideas are independent experiments. None is earned merely because it is
possible with an AST.

### 1. Semantic zoom: a codebase that behaves like a map

The reader should be able to zoom continuously:

```text
project -> namespace neighborhood -> form -> branch -> exact source
```

One request would declare a question and an evidence budget. The response
would begin with the smallest structural view capable of answering it, while
retaining stable handles for deeper expansion. A project map might show
namespace edges and public forms; expanding one form might reveal branch or
binding structure; exact source would appear only at the final necessary
level.

Why this could be 3x: agents currently alternate between outlines that are too
broad and source reads that are too detailed. Budgeted semantic zoom could
replace several guess-and-narrow turns without increasing source output.

Boundary: the kernel may summarize mechanically observable structure—names,
types, arities, ranges, counts, requires, and dependencies. It must not invent
purpose, importance, or architecture labels.

First experiment: freeze questions that currently require `:ls`, one or more
`:cat` calls, and an `:xray`; test whether one zoom request returns the same
facts with at most one-third the evidence bytes and source-bearing turns.

### 2. The structural time machine

Git history is line-oriented, but many code-reading questions are about a
form's identity:

- When did this function acquire this branch?
- Was this definition moved or rewritten?
- Which commit changed this dependency edge?
- What did this multimethod implementation look like before the regression?

A structural time machine would follow named owners and lossless fingerprints
through commits, distinguish move from modification, and return a compact
timeline of form-level changes with Git commits as evidence locators.

Why this could be 3x: code archaeology often means repeated `git log`, blame,
show, and manual line-range adjustment as forms move. One structural-history
query could own commit traversal, form relocation, and evidence assembly.

Boundary: Git remains the history authority. Similarity may rank candidates
but may not silently assert identity. Ambiguous lineage must return every
candidate and one refinement remedy.

First experiment: collect five real archaeology questions involving moved or
renamed forms and compare complete task wall, Git calls, commits inspected,
and source bytes with native Git.

### 3. Counterfactual X-ray over the future snapshot

The intent compiler already constructs complete future files before writing
them. Let the reader query that candidate world:

```text
compile decision without commit
  -> run structural questions against the future snapshot
  -> compare exact before/after facts
  -> commit the already-proved candidate
```

Questions could include changed requires, new dependency edges, remaining old
call shapes, declare order, owner counts, or whether a forbidden form still
exists. The model would inspect consequences without reopening rewritten
source or materializing a temporary worktree.

Why this could be 3x: preview today often means reading a large diff, mentally
simulating it, changing the plan, and recompiling. Counterfactual X-ray moves
bounded computation to the candidate data and preserves the exact compiled
artifact for commit.

Boundary: it proves syntax and declared structural invariants, not behavior.
Tests, linters, compilers, and live systems remain separate authorities.

First experiment: use multi-file migrations whose review questions can be
stated as exact counts or graph facts; compare future-X-ray receipts with
aggregate-diff review.

### 4. Ripple radar: an evidence-backed impact corridor

Given a form, var, namespace, protocol method, keyword, or route, show the
smallest known corridor of mechanically evidenced relationships:

```text
definition
  -> direct references
  -> namespace dependents
  -> registrations or dispatch sites
  -> relevant tests and entry points
```

The result should distinguish evidence authorities: syntax references from
clj-surgeon, resolved references from clj-kondo or a language server, loaded
definitions from an nREPL, and test ownership from repository configuration.
Unknown must remain different from absent.

Why this could be 3x: readers often reconstruct impact with several searches,
dependency queries, test-name guesses, and source reads. One corridor request
could batch those authorities and deduplicate the result.

Boundary: ripple radar reports relationships and uncertainty. It does not say
that a change is safe, that a caller is dead, or that a test is sufficient.

First experiment: freeze real “what could this change affect?” questions and
score exact evidence records, false absence, tool calls, and time to a correct
impact summary.

### 5. Structural census and outlier detection

Ask the repository questions such as:

- What shapes do our Ring handlers actually have?
- How many state transitions return each result form?
- Which route declarations differ from the common shape?
- What are all the arities and option-key combinations used for this call?

The tool would return a histogram of structural shapes, counts by owner/file,
and bounded representatives for each variant instead of dumping every match.
The caller could then expand only an interesting class.

Why this could be 3x: migration and consistency work begins with a census, but
today the model often receives hundreds of near-duplicate source snippets and
does the grouping in context. Computing the grouping beside the parser could
turn dozens of reads into one compact fact table.

Boundary: “outlier” means statistically or structurally uncommon, not wrong.
The model decides which variation is intentional.

First experiment: replay a real API migration or convention audit and measure
whether one census replaces search, sample reads, manual classification, and a
second completeness search.

### 6. Pattern crystallization by structural anti-unification

When a reader identifies several related examples, the kernel could compute
their least-general common structural pattern and expose the differing
subtrees as candidate captures:

```clojure
(views/static "app.css")
(views/static asset-path)
(views/static (theme-file theme))

=> (views/static ?asset)
```

The response would show the proposed pattern, capture table, exact match count
in an explicit scope, and counterexamples. The model could accept, narrow, or
discard it before using it in a read or transaction.

Why this could be 3x: callers currently invent wildcard patterns, test them,
discover over- or under-matching, and iterate. Mechanical anti-unification can
compile examples into a reviewable starting hypothesis in one pass.

Boundary: the tool generalizes syntax only. It does not claim that examples
mean the same thing or that the resulting pattern is an appropriate edit
scope.

First experiment: take observed migrations with three or more source shapes
and compare pattern construction attempts, refusals, matched scope, and final
caller corrections.

### 7. A runtime telescope joined to exact source

A persistent nREPL can answer questions syntax alone cannot:

- Which implementation is loaded?
- What is the current value or shape of this var?
- Which branch receives real values?
- Does the running process contain the same source revision?

The telescope would connect a structural selection to a named runtime and
return source hash, loaded-var identity, bounded value shape, and explicitly
requested observations. A short-lived trace could attach samples to exact
form handles rather than emitting an unstructured log stream.

Why this could be 3x: live diagnosis repeatedly alternates among source search,
nREPL probes, log inspection, and manual reconciliation. A joined receipt can
prove which source and runtime facts belong together.

Boundary: read-only evaluation is the default. Every runtime, process, and
snapshot is named. A standalone analysis JVM must never masquerade as the
production or test process. Values are bounded and secrets are redacted by
declared policy.

First experiment: select real incidents where static inspection left two
plausible explanations; measure time and probes needed to connect the loaded
behavior to its exact source.

### 8. Structural watchpoints: turn discoveries into durable invariants

Exploration often produces facts worth preserving:

- this registration must remain unique;
- every handler in this scope must include one authorization call;
- no use of the retired namespace may remain;
- each platform branch must define the same public names.

A reader could promote a successful bounded query into a versioned structural
watchpoint. The same pure query would run locally or in CI and report a stable
diagnostic when its count, distribution, or shape changes.

Why this could be 3x: teams repeatedly rediscover the same structural
assumptions during reviews and migrations. A watchpoint turns one exploration
into permanent, cheap evidence and prevents future archaeology.

Boundary: watchpoints encode assertions explicitly chosen by the human or
model. clj-surgeon does not infer which observed regularities should become
policy.

First experiment: mine five recurring search-and-check procedures from real
work, encode them as watchpoints, and verify that the next relevant change
gets one useful diagnostic without false success.

### 9. An executable expedition notebook

Large investigations are not always one query, but many questions are known
at the same time. Let the caller state a question graph once:

```text
outline these namespaces
  -> find these owners
  -> compute these branch counts
  -> follow these dependency edges
  -> return only the requested facts
```

The kernel would topologically execute independent reads against shared
snapshots, deduplicate parsing, retain named intermediate selections, and emit
one compact evidence graph. The notebook itself would be immutable data that
can be rerun on another commit.

Why this could be 3x: an architecture diagnosis has already required 26
structural calls and 45.7 seconds of direct tool time. A compiled question DAG
could remove process boundaries and prevent the model from carrying the
partial answer between calls.

Boundary: the caller supplies the questions and dependencies. The kernel may
schedule and memoize them but may not decide which question should be asked
next.

First experiment: replay that 26-call diagnosis from a frozen snapshot as one
question manifest and compare correctness, wall time, result bytes, and facts
the caller must reconstruct.

### 10. Verification cartography

After a change, the compiler knows exact changed forms, namespaces, dependency
edges, and source hashes. A verification map could translate those mechanical
facts into the repository's declared formatter, linter, compile, and test
entrances:

```text
changed selections
  -> affected namespace closure
  -> available repository verification commands
  -> evidence already proved vs evidence still required
```

It could return a machine-readable checklist or invoke one explicitly
authorized repo-owned verification target. The final receipt would separate
parse proof, lint proof, test proof, unavailable checks, and checks not run.

Why this could be 3x: agents repeatedly rediscover commands, guess test scope,
run overlapping checks, and interpret intermediate success as completion. A
repo-owned verification map can turn that ceremony into one trustworthy
handoff.

Boundary: repository configuration and external tools remain the authorities.
clj-surgeon may compute affected code and orchestrate declared commands; it
must not claim tests are sufficient or invent project policy.

First experiment: sample completed changes with known accepted verification
and compare redundant commands, missed gates, elapsed time, and final evidence
quality.

## Which ideas I would bet on first

The highest-leverage sequence is:

1. **Batched inspection with snapshot-bound handles.** It closes the
   perception-to-action gap and directly attacks repeated source-bearing
   turns.
2. **Executable expedition notebooks.** They test whether complete read plans
   can receive the same compiler treatment as complete edit plans.
3. **Structural census.** It moves grouping and completeness accounting out of
   model context without encoding architectural judgment.
4. **Counterfactual X-ray.** It makes transaction review factual and compact
   while reusing the compiler's already-built future state.
5. **Structural watchpoints.** They make every useful exploration capable of
   permanently reducing future work.

The runtime telescope and ripple radar may ultimately be more transformative,
but they introduce multiple authorities and uncertainty contracts. They
should follow only after the snapshot and evidence model is excellent.

## The 3x acceptance rule

Do not count parser microbenchmarks as a product win. For the structural
stratum being tested, require correctness first and then ask whether the new
surface provides:

- at most one batched perception action for questions knowable at the outset;
- one complete mutation action when a decision follows;
- no copied source between read and write requests;
- no post-success source reread or duplicate diff;
- no caller-managed line numbers, hashes, temporary manifests, or receipt
  paths;
- one stable refusal with a bounded executable remedy;
- at most one-third the source-bearing turns and evidence bytes of the
  strongest correct control;
- a threefold wall-time claim only when replicated on the relevant task
  stratum, never hidden inside an aggregate that includes native-favored work.

The north star is not maximal clj-surgeon adoption. It is a code reader who can
ask larger questions, retain less mechanical state, and move from evidence to
decision without losing the shape of the thought.
