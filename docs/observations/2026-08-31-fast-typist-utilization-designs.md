# Fast REPL typist: utilization designs

Date: 2026-08-31

Status: ideation only. No product mechanism or speed claim is proposed as
earned.

Branch: `docs/fast-typist-designs-20260831`

Gene, verbatim: "if you have someone who can type 100-1000x faster than you
into repl, how could you best utilize them?"

## Executive answer

Use the fast typist as a **bounded experiment executor**, not as a smaller
programmer. Give it a frozen worksheet that names the subject, legal probe
grammar, bounds, and result schema. Let it spend abundant mechanical work
inside the REPL. Make it return data, counterexamples, and provenance at the
first judgment boundary. Fable decides what the evidence means and what to ask
next.

The strongest near-term uses are not large source emissions. They are repeated
small probes whose syntax is cheap, whose observations come from the runtime,
and whose results can be checked mechanically. Tonight's inverse surprise
applies directly. Strict surfaces made Spark-class execution reliable. Freedom
produced semantic mush, malformed rewrites, and later walls built from the mush.

The 16 patterns below are **orthogonal workloads**, not new rungs. Any pattern
can begin as R1 substitution, move its local iteration to R2, and eventually
leave an R3 predicate or harness behind. The rank asks a different question:
where should the live rig spend its next ten minutes?

## Evidence boundary

The designs take tonight's receipts as their starting evidence:

- warm Spark bangs completed in about 1.3--2.4 seconds through the production
  edit route; the retained warm-executor screen reports a 2.288-second median;
- the ladder finished 5W/3L: strict scoped fills and walls won, while large
  re-emissions, vague intent, and hard rewrites lost;
- one 35-token decision expanded successfully into a 15-line multi-arity form;
- output near and beyond roughly 30 lines exposed truncation risk;
- a valid but vague result became semantic mush, and that mush propagated when
  a later wall used it as input; and
- three independent screens found the same direction: a weaker model became
  more reliable as the surface became stricter.

The speedup ranges below are **design estimates against Fable typing and
iterating every probe at about 60 tokens/second**. They are not measurements.
The ranges include avoided caller turns and relocated loops, not only the raw
Spark/Fable decode ratio. Each ten-minute experiment must record complete wall,
eval count, caller-emitted tokens, correctness, and judgment interventions
before any estimate is promoted.

## The mush firewall

Every pattern uses the same five-part boundary:

1. **Frozen worksheet.** The caller names exact subjects, allowed operators,
   bounds, and the terminal result schema. Vague intent is not executable.
2. **Read-only by default.** The typist may define session-local helpers and
   call pure functions. It may not write files, mutate application state, call
   effectful endpoints, or repair failures automatically.
3. **Data, not prose, crosses iterations.** A later loop consumes the original
   worksheet plus caller-selected facts. It never consumes the typist's free-
   form explanation as a new specification.
4. **Fail closed.** Time, eval-count, output-byte, exception-count, and search-
   space bounds are explicit. Truncation, stale namespaces, or schema failure
   stop the loop and return partial counts honestly.
5. **Judgment stays slow.** Fable owns hypotheses, predicates, equivalence
   relations, semantic labels, acceptance, and source changes. The fast typist
   owns expansion, execution, collection, shrinking, and replay.

## Live cathedral substrate

The ten-minute experiments target the live worktree at
`/private/tmp/cathedral`. At observation time it was a **standalone analysis
nREPL**, not the production MCP JVM:

| fact | observed value |
|---|---|
| worktree commit | `9af88fbae9ee720613599feaf8cf58432c5898bb` |
| nREPL port | `51493` from `.nrepl-port` |
| JVM PID | `39518` |
| Clojure | `1.12.1` |
| JVM working directory | `/private/tmp/cathedral` |
| scratch seed | untracked `dev/explain_eligibility.clj` |

The scratch seed is useful because it converts 22 opaque prepared-request
eligibility conditions into `{:eligible? boolean :failing [...]}`. It is not on
the nREPL classpath. Experiments may load it explicitly with `load-file`. They
must not edit it. Each experiment begins by rechecking port, PID, working
directory, commit, and JVM kind. Each experiment ends by preserving only a
bounded result and resetting session-local helpers when provenance is unclear.

## Ranking

`Win` and `tonight feasibility` are 1--5 screen-order priorities. Their product
orders the experiments. Win includes recurring leverage. Feasibility 5 means
the current cathedral nREPL can run the screen without a new file or dependency.
Ties favor the smaller judgment surface, then the result that makes later work
cheaper.

| rank | utilization pattern | win | tonight feasibility | product | estimated win vs caller-typed |
|---:|---|---:|---:|---:|---:|
| 1 | invariant watchdog | 5 | 5 | 25 | 8--30x across a ten-change session |
| 2 | parallel hypothesis fanout | 5 | 5 | 25 | 5--20x per question |
| 3 | exhaustive boundary characterization | 5 | 5 | 25 | 15--80x per behavior matrix |
| 4 | differential oracle farm | 5 | 4 | 20 | 10--80x per corpus |
| 5 | autonomous failure shrinker | 5 | 4 | 20 | 20--200x per failing case |
| 6 | metamorphic relation farm | 5 | 4 | 20 | 20--100x per relation set |
| 7 | REPL-session stenographer and replay compiler | 4 | 5 | 20 | 5--15x per cleaned exploration |
| 8 | runtime behavior and example atlas | 4 | 5 | 20 | 10--50x per namespace |
| 9 | exception and refusal atlas | 4 | 5 | 20 | 10--50x per input grammar |
| 10 | fixture and test-scaffold factory | 4 | 4 | 16 | 8--30x per case family |
| 11 | bounded state-space explorer | 5 | 3 | 15 | 50--500x per transition model |
| 12 | parameter and performance sweep | 3 | 5 | 15 | 8--25x per sweep |
| 13 | branch-witness finder | 4 | 3 | 12 | 20--100x per witness set |
| 14 | probe compactor | 3 | 4 | 12 | 4--12x now, then recurring savings |
| 15 | reproducibility packet compiler | 3 | 4 | 12 | 5--20x per handoff |
| 16 | before/after semantic delta census | 4 | 3 | 12 | 10--40x per change |

## 1. Invariant watchdog

- **Caller worksheet (~40--70 tokens):** subject Vars, namespace reload list,
  3--10 caller-approved predicates, prior-result hashes, a two-second budget,
  and `return = changed predicates plus provenance`.
- **Loop ownership:** a host trigger asks the fast typist to reload and run the
  fixed predicate vector after each change. The typist iterates over checks.
  Fable changes predicates and interprets failures.
- **Estimated win:** 8--30x over a ten-change session. The first check is only
  modestly cheaper. The win compounds because Fable stops retyping and
  remembering the same probes.
- **Failure, guard, judgment gate:** a stale JVM can produce a false green.
  Reconfirm cwd/PID/commit, require changed namespaces with `:reload`, return
  full predicate IDs and hashes, and never auto-repair. Fable decides whether a
  changed predicate is a regression, an intended contract change, or a bad
  predicate.
- **Ten-minute cathedral experiment:** load `dev/explain_eligibility.clj`. Run
  `explain` and the private one-argument `eligible-descriptor` predicate over a
  small retained result corpus; repeat after three explicit namespace reloads;
  return only changed booleans, failing-key sets, hashes, and elapsed times.

## 2. Parallel hypothesis fanout

- **Caller worksheet (~50--100 tokens):** one question, 3--8 mutually exclusive
  hypotheses, one observation expression template, and the fields allowed in
  each result row.
- **Loop ownership:** the typist expands every hypothesis into one bounded
  probe and evaluates the vector in one REPL submission. Fable reads all rows
  together and chooses the surviving hypothesis. The typist does not vote.
- **Estimated win:** 5--20x. Five independent probes become one caller turn and
  one returned table instead of five type/read/decide cycles.
- **Failure, guard, judgment gate:** hypotheses can be non-exclusive or probes
  can observe proxies instead of causes. Require a caller-authored observation
  template, return raw values and exceptions, and label `:inconclusive` when
  more than one row survives. Fable owns causal interpretation.
- **Ten-minute cathedral experiment:** use five named eligibility hypotheses
  (`:ok`, `:read-complete`, `:operation`, `:canonical-root`, and
  `:no-artifacts`). Have the typist issue one composite probe over one retained
  read result and compare each claim with `explain`'s failing-key vector.

## 3. Exhaustive boundary characterization

- **Caller worksheet (~60--120 tokens):** pure function, one base input,
  dimensions and finite values, maximum combinations, per-case timeout, and a
  compact row schema such as `[case-id input-delta value exception]`.
- **Loop ownership:** the typist creates the Cartesian product, executes every
  case, and groups identical outcomes. Fable chooses dimensions and assigns
  semantic names to the groups.
- **Estimated win:** 15--80x. A 50--500-cell table is paid for with one bounded
  worksheet instead of a caller-authored expression per cell.
- **Failure, guard, judgment gate:** combinatorics and giant results can hide
  truncation. Freeze the exact case count before eval, cap it, return group
  counts plus boundary witnesses, and set `complete=false` on any omission.
  Fable decides which axes are meaningful and whether two equal outputs mean
  equal behavior.
- **Ten-minute cathedral experiment:** delete or corrupt each of the 22 fields
  recognized by `explain` one at a time, plus five selected two-field pairs.
  Run both `explain` and `eligible-descriptor`. Return a complete grouped table
  with one witness per distinct failing-key set.

## 4. Differential oracle farm

- **Caller worksheet (~70--130 tokens):** two or more implementations, a fixed
  corpus or generator, a caller-approved normalization function, exact
  equality rule, and a maximum mismatch count.
- **Loop ownership:** the typist runs every implementation on each input and
  buckets exact agreements, mismatches, and exception-shape differences. Fable
  selects the oracle or decides that no implementation is authoritative.
- **Estimated win:** 10--80x, depending on corpus size. The valuable unit is a
  mismatch census, not faster typing of one comparison.
- **Failure, guard, judgment gate:** implementations may share one bug, and a
  lossy projection may manufacture agreement. Preserve raw hashes beside the
  normalized comparison, refuse result truncation, and never call majority
  agreement correctness. Fable owns the equivalence projection and verdict.
- **Ten-minute cathedral experiment:** compare
  `(:eligible? (explain result))` with `(boolean (eligible-descriptor result))`
  over the one-field mutation corpus from pattern 3. Return every disagreement
  with the exact input-delta and both raw result hashes. Zero mismatches is a
  harness result, not proof of complete equivalence.

## 5. Autonomous failure shrinker

- **Caller worksheet (~50--100 tokens):** one failing value, one exact pure
  failure predicate, legal shrink operators, stable-size metric, step/eval
  limits, and a trace schema.
- **Loop ownership:** the typist repeatedly proposes mechanical reductions and
  accepts only candidates that are strictly smaller and still satisfy the
  frozen failure predicate. Fable owns the predicate and judges whether the
  minimal witness explains the real problem.
- **Estimated win:** 20--200x. Tens or hundreds of delete-and-retest steps move
  behind one caller boundary.
- **Failure, guard, judgment gate:** a bad predicate yields a beautifully
  minimal irrelevant case. Hash the original value and predicate, preserve a
  monotone size measure, return the accepted shrink trace, and stop on a local
  minimum. Fable must ratify the predicate before shrinking and interpret the
  result afterward.
- **Ten-minute cathedral experiment:** start with a prepared-read result whose
  `explain` output contains several failures. Allow only map-key deletion,
  vector-element deletion, and string shortening. Find the smallest value that
  still triggers one caller-selected failing key, with at most 100 evals.

## 6. Metamorphic relation farm

- **Caller worksheet (~70--140 tokens):** pure function, seed corpus, 3--10
  caller-authored transformations, expected relation for each transformation,
  and exact failure rows.
- **Loop ownership:** the typist applies every transformation to every seed and
  checks the declared relation. Fable authors the relations and decides whether
  a violation is a product bug, a bad relation, or an incomplete input model.
- **Estimated win:** 20--100x. The loop multiplies a small semantic investment
  across many inputs without asking the caller to predict exact outputs.
- **Failure, guard, judgment gate:** model-invented invariants become mush with
  mathematical clothing. Accept relations only from the caller, preserve seed
  and transform IDs, cap expansion, and return counterexamples without a prose
  diagnosis. Fable owns every relation and verdict.
- **Ten-minute cathedral experiment:** declare that map-key order and one
  unrelated top-level key must not change eligibility, while duplicate form
  owners and a changed file hash may change it. Run the four relations over ten
  retained/synthetic result maps and return only violating pairs.

## 7. REPL-session stenographer and replay compiler

- **Caller worksheet (~30--60 tokens):** capture window, redaction keys,
  allowed namespaces, terminal deliverable (`ordered replay forms plus expected
  result hashes`), and a maximum script size.
- **Loop ownership:** the typist records submitted forms, results, exceptions,
  namespace changes, and dependencies during exploration. At the end it
  removes superseded probes and proves the retained sequence in a clean nREPL
  session. Fable names the conclusion and decides which observations matter.
- **Estimated win:** 5--15x for cleanup and handoff. The larger win is that an
  ephemeral exploration becomes executable evidence without a second manual
  reconstruction pass.
- **Failure, guard, judgment gate:** a neat script can erase false starts that
  explain provenance or depend on stale session state. Preserve an immutable
  raw event list, derive rather than overwrite the clean script, reset before
  replay, and mark any non-replayable form. Fable chooses the retained story.
- **Ten-minute cathedral experiment:** capture the first eight probes from the
  eligibility investigation, reset the client session, replay the minimal
  dependency-ordered subset, and require identical canonical result hashes.
  Report omitted event IDs and reasons.

## 8. Runtime behavior and example atlas

- **Caller worksheet (~50--100 tokens):** exact Vars, approved example inputs,
  safe/pure declaration, output fields (`value`, `type`, `exception`, `elapsed`),
  and per-call limits.
- **Loop ownership:** the typist reads metadata, calls each approved Var with
  each input, and emits one compact behavior card per pair. Fable decides which
  examples are representative and what, if anything, becomes documentation.
- **Estimated win:** 10--50x per namespace. Mechanical invocation and evidence
  capture dominate; semantic explanation remains with the caller.
- **Failure, guard, judgment gate:** examples can look normative while missing
  important cases, and an allegedly pure Var may perform effects. Use an
  allowlist, caller-supplied inputs, short timeouts, exception-as-data, and no
  automatic prose claims. Fable selects safe Vars and writes conclusions.
- **Ten-minute cathedral experiment:** build cards for
  `project-result`, `eligible-descriptor`, `suffix`, and `exact-sha256?` using
  five approved values each. Record private/public status and actual behavior;
  do not infer a contract from the examples.

## 9. Exception and refusal atlas

- **Caller worksheet (~60--120 tokens):** one valid or near-valid seed, named
  mutation operators, function under test, exception/refusal projection, and
  an exact case limit.
- **Loop ownership:** the typist produces invalid variants, invokes the target,
  and groups results by stable error key, exception class, and selected
  `ex-data`. Fable labels intended versus surprising behavior.
- **Estimated win:** 10--50x. A negative-input family becomes one table rather
  than many manually staged failures.
- **Failure, guard, judgment gate:** mutations can leave the intended input
  domain and inflate meaningless noise. The caller approves operators; the
  typist records the exact delta and never calls an exception a bug. Bound
  exception text and retain hashes when full data is too large.
- **Ten-minute cathedral experiment:** mutate the retained result with wrong
  booleans, missing counts, invalid SHA-256 strings, absolute paths, unknown
  suffixes, duplicate owners, and inserted artifact keys. Compare `explain`'s
  named failures with the actual descriptor result.

## 10. Fixture and test-scaffold factory

- **Caller worksheet (~80--150 tokens):** one approved test template, hole
  schema, case table or generator, expected assertion count, one-form chunk
  limit, and parse/execute checks.
- **Loop ownership:** the typist fills template holes one case at a time,
  evaluates each form in the standalone nREPL, and returns case specs plus
  passing scaffold forms. Fable owns the template, expected semantics, case
  admission, and any later source write.
- **Estimated win:** 8--30x for a 10--40-case family.
- **Failure, guard, judgment gate:** a large generated test wall can truncate,
  duplicate a semantic mistake, or bless current behavior accidentally. Emit
  one small form per case, parse and run it before continuing, return exact
  case IDs, and stop before writing files. Fable approves the template and
  expected outcomes before generation, then reviews the case table.
- **Ten-minute cathedral experiment:** generate 12 EDN case specs for the
  eligibility conditions, expand only three into individual `clojure.test`
  forms in the session, and verify their assertion counts. Do not create or
  edit a test file.

## 11. Bounded state-space explorer

- **Caller worksheet (~90--180 tokens):** pure transition function, initial
  state, finite event alphabet, caller-owned invariants, depth/state limits,
  canonical state key, and shortest-trace result schema.
- **Loop ownership:** the typist performs breadth-first exploration, deduplicates
  states, checks invariants, and returns the shortest trace for each violation.
  Fable owns the state model, event meaning, invariants, and remediation.
- **Estimated win:** 50--500x when hundreds or thousands of event sequences fit
  behind one boundary.
- **Failure, guard, judgment gate:** an incomplete model gives a complete answer
  to the wrong world. Require pure transitions, a frozen alphabet, exact visited
  and omitted counts, bounded depth, and `complete=false` at the cap. Fable
  judges model fidelity before acting on a trace.
- **Ten-minute cathedral experiment:** treat result-map mutations as events
  (`drop field`, `change count`, `duplicate owner`, `add artifact`). Explore to
  depth three over a small seed and return the shortest sequence that reaches
  each of five selected `explain` failure keys, capped at 1,000 states.

## 12. Parameter and performance sweep

- **Caller worksheet (~40--80 tokens):** exact expression/function, parameter
  vector, warmup and sample counts, timeout, summary statistics, and environment
  fields.
- **Loop ownership:** the typist runs warmups and samples inside the same JVM,
  then returns raw counts plus median/p90/min/max. Fable chooses the benchmark
  question and decides whether the measurement is decision-relevant.
- **Estimated win:** 8--25x for a 10--50-point sweep.
- **Failure, guard, judgment gate:** microbenchmarks invite causal fiction.
  Record JVM, cwd, commit, warm/cold status, sample count, and expression hash;
  do not compare unlike JVMs or call a local timing a product-wall result.
  Fable owns interpretation and any performance claim.
- **Ten-minute cathedral experiment:** measure `explain` and
  `eligible-descriptor` over corpus sizes 1, 10, 100, and 1,000 after three
  warmups, with five samples per size. Return complete raw durations and a
  compact summary, explicitly labeled standalone-nREPL evidence.

## 13. Branch-witness finder

- **Caller worksheet (~70--130 tokens):** pure classifier, finite input grammar,
  target outcome labels, size order, search limit, and one witness per label.
- **Loop ownership:** the typist enumerates inputs in size order and retains the
  first verified witness for each requested label. Fable defines meaningful
  labels and checks whether a witness is representative.
- **Estimated win:** 20--100x where several branches require fiddly values.
- **Failure, guard, judgment gate:** accidental reachability can produce ugly
  or domain-invalid witnesses. Constrain the grammar, preserve search order and
  counts, verify each witness twice, and label unfound branches honestly.
  Fable decides whether a witness belongs in a test or document.
- **Ten-minute cathedral experiment:** search bounded result-map variants for a
  witness that uniquely triggers each of five eligibility failures. Return the
  smallest exact input delta for every found key and counts for unfound keys.

## 14. Probe compactor

- **Caller worksheet (~50--90 tokens):** ordered original probe forms, required
  output labels, equality rule, allowed composition operators, and output-size
  limit.
- **Loop ownership:** the typist builds one composite expression, runs original
  and compact versions against the same frozen state, and iterates until outputs
  match exactly or the limit is hit. Fable decides whether the compact probe is
  readable and whether lost intermediate provenance matters.
- **Estimated win:** 4--12x in the current exploration, with recurring savings
  each time the compact probe is reused.
- **Failure, guard, judgment gate:** compaction can hide exceptions, ordering,
  laziness, or provenance. Preserve per-probe labels, realize bounded results,
  compare exceptions as data, and keep the originals in the stenography log.
  Fable approves the replacement.
- **Ten-minute cathedral experiment:** compact five separate eligibility checks
  into one labeled result map. Run both forms over ten inputs and require exact
  equality of values and exception shapes before retaining the composite.

## 15. Reproducibility packet compiler

- **Caller worksheet (~40--80 tokens):** failing eval ID, allowed environment
  fields, redaction keys, dependency boundary, replay timeout, and packet size.
- **Loop ownership:** the typist gathers the exact form, input hash/value when
  permitted, namespace/Var metadata, cwd, commit, JVM identity, seed, result or
  exception, and ordered dependencies. It then tests replay in a reset session.
  Fable decides what may leave the machine and whether the packet proves the
  reported issue.
- **Estimated win:** 5--20x per handoff or delayed resumption.
- **Failure, guard, judgment gate:** packets can leak source/secrets or silently
  depend on session-local state. Use an allowlist rather than a denylist,
  redaction before rendering, exact hashes for omitted values, clean-session
  replay, and `replayable=false` on failure. Fable approves disclosure.
- **Ten-minute cathedral experiment:** package one deliberately failing
  eligibility probe with commit, PID, cwd, Clojure version, form hash, input
  hash, exception/value, and required load forms. Reset and replay it. Retain
  the packet in the REPL result only, not a file.

## 16. Before/after semantic delta census

- **Caller worksheet (~70--140 tokens):** subject Vars, frozen corpus, canonical
  projection, before/after reload actions, equality rule, and maximum deltas.
- **Loop ownership:** the typist captures before values, performs only the
  caller-approved reload, captures after values, and returns exact changed,
  unchanged, added, removed, and exception buckets. Fable owns the corpus,
  projection, and decision to accept the semantic delta.
- **Estimated win:** 10--40x for a 20--200-input corpus.
- **Failure, guard, judgment gate:** stale classloaders, non-determinism, and a
  lossy projection can hide changes. Reconfirm Var roots after reload, run a
  no-change control first, preserve raw hashes, repeat nondeterministic rows,
  and never infer source correctness from output parity. Fable judges the
  semantic meaning and authorizes source work separately.
- **Ten-minute cathedral experiment:** run a no-source-change control: capture
  `explain` and `eligible-descriptor` over the mutation corpus, reload the
  prepared-request namespace, and repeat. The expected zero delta validates
  the census harness. Any delta is evidence of stale/session behavior to
  diagnose before using the pattern around a real change.

## What should not be delegated

The fast typist should not own these decisions, even if it can phrase or type
them faster:

- inventing the invariant, oracle, equivalence relation, or causal hypothesis;
- deciding that an observed example is the intended contract;
- turning an exception, majority vote, or minimal counterexample into a bug
  verdict;
- promoting generated prose into the next worksheet;
- selecting a source target, rewrite scope, or verification policy from vague
  intent;
- auto-repairing a failed watchdog or applying a scaffold to source; or
- interpreting a standalone nREPL result as production state.

These are the points where zero judgment costs more than fast syntax saves.

## Recommended first three ten-minute screens

Run patterns 1--3 in order on the same eligibility corpus:

1. **Watchdog:** prove the fixed-check loop survives reload and returns only
   changed evidence.
2. **Fanout:** prove one caller question can receive five independently labeled
   observations without a narrative bridge.
3. **Boundary characterization:** prove 20+ cases can run behind one worksheet
   with complete counts and no truncation.

They need no source edit, no new dependency, and no product JVM. Together they
test the three parts that matter before attempting the more ambitious designs:
strict worksheet obedience, loop relocation, and trustworthy result
compression. If any result requires Fable to reverse-engineer free-form prose,
the screen fails even when the Clojure expressions are valid.

## Decision

The best use of a 100--1000x REPL typist is not “write what I would have
written, faster.” It is “spend mechanical abundance behind a semantic
checkpoint.” The caller should emit a small, exact worksheet. The fast typist
should execute a large bounded experiment. The REPL should supply syntax and
runtime facts. The caller should resume at the first place judgment is required.

That division converts speed into breadth, repeatability, and shorter feedback
loops without letting fast semantic mush become tomorrow's premise.
