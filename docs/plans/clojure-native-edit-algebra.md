# Clojure-Native Edit Algebra Experiment

**Status:** Source-derived `transform` passed the clean-agent keep gate.

## Stage B CLI probe contract

Extend the existing plan-only `:edit` operation with `:expr`. Do not add a new
operation or executor.

```bash
clj-surgeon :op :edit :file src/state.clj \
  :expr "(-> (form 'transition) (match :finish) right (replace '(assoc state :status :complete)))" \
  :plan-out plan.edn
```

The caller must supply exactly one of `:query` and `:expr`. Existing `:query`
behavior stays unchanged. `:expr` compiles through the sandboxed SCI boundary
and then delegates to the same `structural-lens/edit-file` function. The caller
must still supply `:file` and `:plan-out`.

On success, the saved plan schema, diff, hashes, operation, and executor remain
identical to the corresponding `:query` plan. The returned EDN adds no second
plan format. Planning never changes source. Application remains a later
`:replace-subform!` command.

Refuse these inputs before reading or writing source:

- neither `:query` nor `:expr`;
- both `:query` and `:expr`;
- an invalid or unsafe SCI expression;
- an expression that returns non-vector data;
- any existing unsupported consent-like argument.

SCI failures must preserve the compiler's stable reason, allowed symbols,
allowed signatures, and remedy in CLI EDN. The CLI must return a nonzero status
without a stack trace. Existing plan files and source bytes must remain exact
on every refusal.

This probe intentionally keeps `:plan-out` explicit. A generated plan path is
a separate experiment because it changes artifact ownership and cleanup. First
measure whether native authoring improves clean-agent behavior while every
other workflow variable remains fixed.

## Stage B implementation result

`:edit :expr` now compiles through SCI and delegates to the existing edit
planner. The legacy query and native expression produce identical plan data
apart from their requested plan paths. The same existing executor applies both
plans.

The pure and SCI tests pass 21 tests with 367 assertions. The CLI edit tests
pass 11 tests with 154 assertions. The combined edit, help, and structural-lens
suite passes 99 tests with 961 assertions. The tests cover these boundaries:

- broad pure Clojure collection and higher-order composition;
- every structural builder;
- exact legacy/native plan equivalence;
- real CLI parsing and verbatim expression transport;
- compiler independence from the caller's current namespace;
- transform execution only after exact-one selection;
- concrete, function-free transform plans and existing-executor replay;
- unchanged source during planning;
- the unchanged saved plan schema;
- verified application through `:replace-subform!`;
- missing, conflicting, unsafe, multiple, and non-vector expressions;
- refusal before source or plan I/O;
- agent-facing help, README, vision, changelog, and both skills.
- installed skill fits in one standard 240-line read.

### 2026-09-01 SCI host-interop Andon repair

The compatibility expansion for no-default `case` added the internal SCI
symbols `case*`, `throw`, and `new`, plus an `IllegalArgumentException` class
mapping. Direct source use of the three internal symbols remained refused, but
Clojure constructor shorthand such as `IllegalArgumentException.` did not
contain the source symbol `new`. SCI lowered that shorthand after source
validation, constructing a real host exception. Dot invocation could then call
`printStackTrace`, perform observable stderr I/O, and still return a valid edit
query.

The repaired boundary rejects executable source symbols whose names begin or
end with `.` before SCI evaluation. This covers constructor shorthand, method
shorthand, field shorthand, and the explicit `.` form. Quoted forms remain
inert structural data and are not rejected. `case*`, `throw`, and `new` remain
available only to macro expansion; direct and qualified executable source use
remains refused.

`case` without a default remains supported. A matching clause returns the
normal query. An unmatched value reaches the macro-generated throw path and is
reported as `:evaluation-failed`; callers cannot author `throw` directly.
Permanent regressions cover direct and qualified internal symbols, simple and
qualified constructors, dot method and field forms, host objects in otherwise
valid replacement queries, captured stderr side effects, quoted inert data,
and the pre-change empty-class-map causal control.

## Stage B clean-agent result

The 24-run matched-skill A/B was byte-exact in every run, but clean agents used
`:expr` only once in 12 post runs. Literal `case` and binding edits stayed at a
median three shell calls. Outer `cond` edits regressed from five to six calls,
101,630 to 125,395 input tokens, and 46.96 to 61.67 seconds. The one native
attempt took eight calls.

This rejects the hypothesis that Clojure-shaped query construction alone is a
better default. Literal EDN remains the right surface for literal paths and
replacements.

## Stage C source-derived transform contract

Add `(transform path pure-function)` as a terminal native builder. After the
selection returns exactly one form, pass that form's Clojure data to the
function. Do not invoke the function for zero or many matches. Convert its
result into the existing concrete `[:replace FORM]` query and build the normal
plan.

The saved plan must contain no SCI function or executable expression. It must
contain the concrete replacement, one diff, source and result hashes, and the
existing provenance. `:replace-subform!` remains the only executor and must not
load SCI. This preserves the inert-EDN review boundary.

The clean-agent task must require a replacement derived from an unknown current
value. The post surface earns a keep only if agents can plan directly with
`transform`, while the pre surface needs a read before it can author the
replacement.

## Stage C clean-agent result

The eight-run A/B was byte-exact in every run. Post agents chose `transform` in
four of four runs and made the edit plan their first source-bearing call in four
of four runs. Pre agents did that in zero of four runs.

Median shell calls fell from five to three. Median cumulative input fell from
102,837 to 65,829 tokens, median output from 1,201 to 718 tokens, source output
from 2,150 to 1,527 bytes, and wall time from 42.36 to 27.67 seconds. The
source-derived surface passes the command, token, voluntary-use, and exactness
gates.

Keep both surfaces. Use EDN `:query` for literal selections and replacements.
Use native `:expr` plus `transform` when the replacement depends on the selected
form. Do not promote native query construction as a replacement for shorter
literal EDN.

## Final one-read replication

After reducing the installed skill from 241 to 240 lines and adding a permanent
line-count test, four of four clean agents completed the computed edit in
exactly three calls: one skill read, one transform plan, and one verified apply.
Every run was byte-exact, chose `:expr`, made the plan its first source-bearing
call, and avoided all text readers and read preflights.

Final medians were three shell calls, 65,806 cumulative input tokens, 14,075
uncached input tokens, 731 output tokens, 1,529 source-output bytes, and 28.22
seconds. Stage C is complete and passes the one-shot keep gate.

## Product hypothesis

The current structural kernel is safe and expressive, but agents must translate
Clojure intent into an EDN vector embedded in shell quoting:

```bash
clj-surgeon :op :edit :file state.clj \
  :query '[[:form transition] [:find :finish] :right [:replace (assoc state :status :complete)]]' \
  :plan-out plan.edn
```

Clojure is already active in the model's context. Ordinary Clojure functions
may be a shorter and more reliable authoring surface for Clojure structure:

```clojure
(edit "state.clj"
  (-> (form 'transition)
      (match :finish)
      right
      (replace '(assoc state :status :complete))))
```

This experiment asks whether that native expression reduces translation,
quoting, help, and recovery work. It does not replace the guarded planner.

## Baseline to beat

With the installed edit-first skill, the existing CLI completed 12 of 12 exact
edits across `case`, outer `cond`, and `let` binding shapes.

- The structural plan was the first source-bearing command in 11 of 12 runs.
- The case and binding tasks reached the 3-command floor: skill read, plan,
  apply.
- The outer-`cond` task had a 5-command median because agents sometimes refined
  an already-correct plan or read the owner first.
- One agent generated a correct plan but replayed its diff with `apply_patch`
  instead of the verified executor.

The native surface must beat this baseline. Merely looking more like Clojure is
not enough.

## Architectural boundary

The experiment adds an authoring library, not an evaluation feature in the
CLI. Callers execute normal Clojure or Babashka code and require the library.
clj-surgeon does not accept an arbitrary expression string and call `eval`.

Combinators build the existing query data. Planning delegates to the existing
`:edit`/lens implementation. Applying delegates to the unchanged verified
executor.

```text
Clojure combinators
  -> existing EDN query vector
  -> existing singular planner
  -> existing versioned plan
  -> separate existing executor
  -> existing verified receipt
```

Stage A permits no new selector, replacement, address, diff, hash, replay, or
write semantics.

## Stage A result

The first prototype implements only the pure builders below. Eight tests with
192 assertions cover these cases:

- Every public builder on empty, rooted, and previously navigated paths.
- Every builder on invalid paths and after both terminal edit steps.
- Literal Clojure data and metadata preservation.
- Positive-integer boundaries.
- Input persistence.
- Exact `case`, `cond`, and binding queries.

The implementation passes those tests and clj-kondo with no findings. This
proves semantic equivalence to the existing query data. It does not yet prove
that agents author the native form faster or that invoking it costs less.

## Stage A: pure query builders

Prototype only these functions:

```clojure
(form name)             ; => [[:form name]]
(match path pattern)    ; append [:find pattern]
(where path predicates) ; append [:where predicates]
(right path)            ; append :right
(left path)             ; append :left
(up path)               ; append :up
(down path)              ; append :down
(outermost path)         ; append :outermost
(span path n)            ; append [:span n]
(partition-all path n)   ; append [:partition-all n]
(replace path form)      ; append [:replace form]
(replace-span path & forms) ; append [:replace-span ...]
```

All functions are pure. They accept and return persistent Clojure data. They
perform no parsing, file I/O, evaluation, matching, or mutation. Thread-first
composition must produce the byte-for-byte existing query vector.

Do not add macros. Functions are easier to test, inspect, compose, and call
programmatically. Symbols such as `right` are functions, so thread-first syntax
remains ordinary Clojure.

## Stage B: one planning call

### SCI bridge

Babashka already embeds the Small Clojure Interpreter (SCI). Use SCI to compile
one native expression at the CLI boundary without requiring callers to set a
classpath or load a namespace. Configure SCI with an explicit capability
allowlist.

The compiler must accept exactly one expression and return query-vector data.
Give the expression Clojure's pure collection algebra, control forms, and
higher-order functions in addition to the Stage A builders. This includes
`assoc`, `update`, `mapv`, `filter`, `reduce`, `comp`, `juxt`, destructuring,
and bounded sequence operations.

The compiler must refuse host I/O, namespace loading, evaluation, concurrency,
class access, interop, mutable references, definitions, recursive control,
multiple expressions, and non-vector results. Quoted patterns and replacements
remain inert Clojure data. Do not use unrestricted SCI mode.

The allowlist omits recursive control and unbounded sequence constructors. This
is a practical editing DSL, not a termination proof. The clean-agent harness
retains its normal process timeout.

This bridge changes the earlier architectural boundary. clj-surgeon may
interpret a tightly capability-limited edit expression, but it must never call
Clojure `eval` or expose a general Babashka environment.

The first compiler checkpoint passed 13 tests and 271 assertions with only the
ten builders. The next red contract expands it to pure structural Clojure while
retaining both the syntax validator and SCI allowlist. Structured failures
must include capability categories, function signatures, and a concise remedy.

Two first-pass clean-agent probes produced mixed evidence. For the simple
`case` query, both the EDN and native agents returned exact output. The native
expression was 88 characters and the EDN query was 86. For the outer `cond`
query, the EDN agent returned an exact query, but the native agent invented
named path arguments and two plausible predicates that the API did not expose.
Ordinary Clojure syntax activates useful composition knowledge and plausible
but nonexistent APIs. The SCI boundary makes those inventions safe, but the
skill and error response must still make the correct grammar one-shot.

Only after Stage A wins query-authoring probes, test:

```clojure
(edit "state.clj" query)
```

The call may create a generated plan artifact and return its explicit path,
diff, hashes, and exact apply form. It must never modify source. Generated plan
names must be collision-safe and must not hide durable state.

The returned value must make the review boundary impossible to miss:

```clojure
{:status :review-required
 :plan-file "/absolute/path/to/plan.edn"
 :diff "..."
 :apply-form '(apply-edit! "/absolute/path/to/plan.edn")}
```

Application remains a later evaluation or command. Do not add an option that
plans and applies in one call.

## Deferred transformations

The screenshot names replace, insert, delete, splice, wrap, unwrap, and move.
That is a vision, not the MVP contract.

Stage A exposes only existing replacement semantics. Each additional
transformation must earn its own behavior matrix and prove that it is a general
concrete-syntax operation:

- `insert` must define exact parent, position, and trivia ownership;
- `delete` must define attached-comment and separator behavior;
- `splice` must define container and arity boundaries;
- `wrap` and `unwrap` must preserve metadata and reader forms;
- `move` must reconcile nested addressing with dependency-aware top-level
  movement.

Do not ship a name whose refusal and preservation matrix is not yet exact.

## Pure test matrix

- every builder on an empty path;
- every builder after `form` and after a prior navigation step;
- symbols, keywords, lists, vectors, maps, sets, metadata, discards, and reader
  conditionals as literal patterns or replacements;
- thread-first expressions equal the documented EDN vectors;
- builder inputs remain unchanged;
- invalid path values and invalid numeric arguments refuse with stable data;
- `replace` and `replace-span` must be terminal in validated programs;
- no builder evaluates its pattern or replacement;
- no builder performs file I/O;
- current `case`, nested-`cond`, and binding benchmark queries round-trip
  exactly.

## Clean-agent probes

Give clean Codex the same three edits and one read-only inventory. Compare:

1. current CLI plus edit-first skill;
2. native API reference with no example for the specific syntax shape;
3. compact native API reference;
4. outcome-only prompt with both routes available.

Measure:

- exact query/program correctness before any file operation;
- number of repair attempts;
- characters and tokens in the authored expression;
- shell quoting or temporary-script work;
- source-bearing calls;
- full edit correctness and separate apply;
- plan replay versus manual diff reproduction;
- cumulative/uncached tokens, tool-output bytes, and wall time.

## Keep gate

Keep the native surface only if it does at least one of the following across two
or more syntax shapes:

- removes a command from the 3/5-command baselines;
- cuts authored selector/transformation size by at least 25%;
- removes a demonstrated quoting or query-grammar repair;
- prevents the manual-diff replay failure by returning a clearer review/apply
  value;
- prompts clean agents to choose it voluntarily when both routes have neutral
  documentation.

It must retain 100% exactness, zero/many refusal, one plan/one edit, separate
consent, unchanged source during planning, atomic apply, complete-file parse,
and verified read-back hash.

If it only compiles into the same query with more setup or a temporary script,
record the negative result and keep the CLI algebra.
