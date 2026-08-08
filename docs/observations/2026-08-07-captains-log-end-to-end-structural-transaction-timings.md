# Captain's Log: structural transactions reduce actions before they reduce wall time

Date: 2026-08-07

Beads issue: `clj-surgeon-xey`

## Question

Does clj-surgeon's lower transaction and source-reconstruction cost reduce the
complete agent turn, or does tool routing slow the conversational loop?

Correctness is a gate. This log reports only lanes with four correct fresh
replicas. Raw prompts, event streams, workspaces, and MCP telemetry remain
outside Git. The tables below are compact aggregate evidence.

## Measurement contract

The Codex harness now records these distinct quantities:

- complete turn wall: elapsed time around `codex exec`, excluding benchmark
  fixture setup and isolated MCP bootstrap;
- user-visible agent turn: one `turn.completed` event;
- internal tool round trip: one started command execution, file change, or MCP
  tool call;
- discovery round trip: an internal tool round trip before the first mutation;
- post-decision round trip: the first mutation and every later internal tool
  round trip;
- direct MCP time: `timings_ms.total_ms` from private MCP telemetry;
- recovery round: a refused mutation call followed by another mutation call.

Interaction rates are computed per correct run before medians are taken. Runs
with no mutation classify every internal tool round trip as discovery.

The Codex JSON event schema does not publish native file-change duration. Native
direct tool time is therefore not observable in this harness; complete turn
wall and action counts remain comparable. This missing duration is reported
rather than inferred.

## Experiment 1: the CLI-plus-skill lane lost badly

Capsule: `decision-batch-edit`, a complete supplied decision containing six
exact edits across two Clojure files. Four current CLI-plus-skill runs were
compared with four fresh native controls.

| Metric | CLI + matched skill | Native control |
|---|---:|---:|
| Correct replicas | 4 / 4 | 4 / 4 |
| Complete-turn wall, median | 85.496 s | 58.791 s |
| Complete-turn wall values | 75.021, 113.535, 95.970, 73.936 s | 50.897, 55.538, 73.025, 62.043 s |
| Internal tool round trips, median | 7.5 | 3.0 |
| Discovery round trips, median | 5.0 | 1.0 |
| Post-decision round trips, median | 2.5 | 2.0 |
| Input tokens, median | 153,761 | 75,348 |
| Source bytes surfaced, median | 846 | 3,475 |
| User-visible turns/task | 1 | 1 |

The CLI lane was 26.705 seconds, or 45.4%, slower on median wall. It paid to
read the skill and fallback reference, enumerate skill files, and ask for
general and operation-specific help before mutation. This was not an MCP result:
all treatment rows contained zero MCP calls.

## Experiment 2: persistent MCP nearly reached native wall time

The corrected matrix used `mcp:mcp-rule-no-skill` versus a new native control.
The task prompt and expected bytes were identical. The MCP lane removed the CLI
and skill, started an isolated server before the timed agent turn, and supplied
one routing rule to use `apply_clojure_changes` for the complete multi-edit
decision.

| Metric | Surgeon MCP | Native control |
|---|---:|---:|
| Correct replicas | 4 / 4 | 4 / 4 |
| Complete-turn wall, median | 50.619 s | 47.215 s |
| Complete-turn wall values | 48.332, 52.906, 46.431, 53.210 s | 44.199, 50.230, 41.425, 53.386 s |
| Mean wall / sample SD | 50.220 / 3.370 s | 47.310 / 5.470 s |
| Internal tool round trips, median | 2.0 | 3.5 |
| Discovery round trips, median | 0.0 | 1.0 |
| Post-decision round trips, median | 2.0 | 2.5 |
| Shell calls, median | 0 | 2.5 |
| Input tokens, median | 60,468 | 75,122 |
| Output tokens, median | 1,829 | 1,517 |
| Source bytes surfaced, median | 0 | 3,473 |
| MCP output bytes, median | 368 | 0 |
| User-visible turns/task | 1 | 1 |
| Turns/minute | 1.19 | 1.28 |
| Seconds/turn | 50.62 | 47.21 |
| Tool actions/turn | 2.0 | 3.5 |

The measured MCP median remained 3.404 seconds, or 7.2%, slower. With four
replicas and overlapping ranges, this is not strong evidence of a stable wall
penalty. It is strong evidence that MCP reduced interaction work: 42.9% fewer
internal tool actions and 19.5% fewer input tokens.

### Direct tool time was not the wall bottleneck

Every MCP run first sent a stale direct-change shape with `owner` as a string.
The live contract requires named owners in `forms`, while the optional `owner`
field is a structured namespace-owner object. Each first call failed closed and
changed no source. Each second call used `forms` and atomically committed all
six edits across both files with terminal verification.

| MCP component | Median | Four values |
|---|---:|---|
| Refusal tool time | 9.689 ms | 7.618, 17.561, 9.821, 9.557 ms |
| Successful six-edit transaction | 348.954 ms | 454.733, 509.271, 243.175, 191.156 ms |
| Both MCP calls combined | 357.673 ms | 462.350, 526.832, 252.995, 200.713 ms |
| Model gap between refusal and retry | 20.460 s | 18.983, 22.431, 15.101, 21.938 s |

The kernel used well under one second. A roughly ten-millisecond schema refusal
triggered about twenty seconds of model recovery. The next crossover experiment
must teach the exact `forms` contract and re-run four one-shot replicas; simply
subtracting the recovery gap from observed wall would be an unsupported
counterfactual.

### Startup is separate from the hot-turn result

The benchmark intentionally started one isolated MCP server per run. Bootstrap
was completed before the timed `codex exec` turn.

| Startup metric | Value |
|---|---:|
| Isolated MCP bootstrap, median | 21.233 s |
| Bootstrap values | 16.684, 16.703, 25.763, 29.768 s |

Adding cold bootstrap to every task would make MCP uncompetitive. The product
uses a shared hot service, so the 50.619-second agent-turn median is the relevant
steady-state value; bootstrap remains a deployment and recovery cost that must
not be hidden.

## Experiment 3: exact contract guidance produced a one-shot wall-clock win

The recovery result was actionable rather than merely descriptive. The MCP tool
description said that a direct change could use `forms` or `owner`, but did not
make the type boundary salient: named top-level defs use `forms`, while `owner`
is a structured namespace-owner object and is never a string. All four clean
agents made the same mistake.

The description now states that boundary explicitly and its wording is pinned
in both the tool and server contract tests. The identical MCP-versus-native
matrix then ran five replicas. All five MCP runs were correct. Four of five
native runs were valid and correct; the fifth spent 138.611 seconds probing for
clj-surgeon before editing and was invalidated by the native-isolation gate. It
is retained as a failed control, not silently removed.

| Metric | One-shot Surgeon MCP | Fresh native control |
|---|---:|---:|
| Correct efficiency replicas | 5 / 5 | 4 / 5 |
| Complete-turn wall, median | **27.976 s** | 68.932 s |
| Correct wall values | 26.901, 27.302, 27.976, 28.374, 29.312 s | 41.626, 61.143, 76.721, 80.595 s |
| User-visible turns/task | 1 | 1 |
| Turns/minute | 2.14 | 0.88 |
| Seconds/turn | 27.98 | 68.93 |
| Internal tool actions/turn | **1.0** | 3.5 |
| Discovery round trips | **0.0** | 1.0 |
| Post-decision round trips | **1.0** | 2.5 |
| Shell calls | **0** | 2.5 |
| Input tokens, median | **44,001** | 83,628 |
| Output tokens, median | **848** | 1,719 |
| Source bytes surfaced | **0** | 3,577 |
| One-shot transaction completion | **100%** | not applicable |
| Recovery rounds | **0** | 0 |

For this complete supplied six-edit decision, one-shot MCP was 40.956 seconds
faster on median wall. Native took 2.46 times as long; equivalently, MCP reduced
complete-turn wall by 59.4%. MCP also reduced internal tool actions by 71.4%
and input tokens by 47.4%.

The direct six-edit MCP transaction remained small relative to the turn:
422.360 ms median across five values of 173.975, 305.349, 422.360, 432.870,
and 474.050 ms. Isolated bootstrap was 11.359 seconds median and remained
outside the timed agent turn. The production shared service amortizes that
startup; a cold per-task process would weaken but not erase this measured
crossover.

This result also localizes the previous loss. The kernel did not become faster
between Experiments 2 and 3. Removing one ambiguous schema choice eliminated a
roughly twenty-second model recovery round and reduced the complete workflow to
one transaction.

## Wall-time scatter: three different stories

Each mark is one complete agent turn. The axis is wall-clock seconds. `x` marks
a correct and valid run; `!` marks the native run invalidated by the isolation
gate. Rows came from separate counterbalanced matrices, so comparisons are
within the named experiment rather than across every row.

```text
seconds       0        20        40        60        80       100       120       140
              |---------|---------|---------|---------|---------|---------|---------|
one-shot MCP              xxxx
recovered MCP                        x  xxx
native, one-shot matrix                  x         x       x x                            !
CLI + skill                                             xx          x        x
native, CLI matrix                            x  x   x     x
              |---------|---------|---------|---------|---------|---------|---------|
```

The point clouds tell a more useful story than a single speed ratio:

- one-shot MCP is both fast and tightly clustered: 26.901 to 29.312 seconds;
- the ambiguous MCP contract shifts the cluster to 46.431 to 53.210 seconds,
  even though both MCP calls together take only 358 ms median;
- valid native controls in the one-shot matrix range from 41.626 to 80.595
  seconds, entirely above the one-shot MCP cluster and with much higher spread;
- CLI plus skill is a different, losing route, not evidence about persistent
  MCP: it adds discovery and process work before the same structural mutation;
  and
- the 138.611-second `!` is operationally important but excluded from the
  efficiency median because that control violated native-lane isolation.

The revised timing interpretation is therefore narrower than “MCP is always
2x faster.” The demonstrated claim is: once the decision is already known and
the live direct-change contract is unambiguous, the persistent one-shot MCP
route is 2.46 times faster at the median in this six-edit, two-file stratum.
The structural transaction itself is subsecond. Most wall time belongs to model
routing, request construction, and avoidable recovery. Reader-led exploratory
work remains a separate falsifier.

## Harness findings

Dogfood found and fixed three benchmark defects:

1. macOS Bash 3 has no `BASHPID`; atomic receipt paths now fall back to `$$`;
2. expanding an intentionally empty flags array under Bash 3 `set -u` prevented
   MCP runs from starting; the Codex argument array is now always non-empty; and
3. the MCP success scorer searched human-readable text for JSON booleans even
   though current Codex events carry the receipt in `structured_content`. A
   synthetic event contract now pins successful commit and verification fields.

The interaction extractor and summary math pass their nREPL and command-line
self-tests. The edit-portfolio fixture, schedule, harness, and ShellCheck gates
pass. The broader `make test` run reached 561 tests and 4,977 assertions with
three failures from one pre-existing dirty-worktree condition: the root and
installed skill entrances contain 75 lines while their permanent guard requires
at most 70. Those unrelated skill edits were preserved.

## Falsifiable conclusion for the supplied multi-file stratum

For a complete supplied six-edit decision, an end-to-end wall-clock win is now
demonstrated:

- CLI-plus-skill is decisively slower than native;
- recovered MCP is close to native and materially more interaction-efficient;
- one-shot MCP is 2.46 times faster than the fresh valid native control median;
- direct MCP execution is fast enough that model routing and recovery dominate;
- the direct-change schema ambiguity, not kernel time, caused the earlier loss;
  and
- the reader path has not yet been tested, so this win must not be generalized
  to exploratory convergence.

The next falsifier was explicit and is now completed in Experiment 4 below:

1. an exploratory matched lane that uses
   `inspect_clojure -> decision -> apply_clojure_changes` against a fresh native
   control.

If the reader lane lost, the current wall-clock win would remain real but
bounded to complete supplied structural decisions. It won: clj-surgeon crossed
over from transaction acceleration to end-to-end exploratory convergence on
the paired fixture reported below.

## The twenty-second interval is duplicate transaction work

The event boundary is exact. The recovery interval starts when Codex receives
the refusal and ends when Codex starts the corrected MCP call. It does not
include either tool execution:

```text
complete decision supplied
  -> construct obsolete owner-shaped request
  -> refusal returned                         9.7 ms median
  -> interpret mismatch
  -> reconstruct the six-edit request        20.46 s median
  -> submit corrected forms-shaped request
  -> commit and verify                        349.0 ms median
```

No source discovery occurred in this interval. The first request had already
encoded the complete decision. The retry therefore made the model translate
and serialize the same decision a second time. This is recovery and payload
reconstruction cost, not Clojure analysis or mutation cost.

The event stream does not expose how much of the interval is model inference,
service scheduling, refusal interpretation, or JSON generation. Therefore,
20.46 seconds is an observed removable round, not a promised saving. A correct
first request still requires one payload construction. The old run only proves
that the second construction and its surrounding recovery took 15.101 to
22.431 seconds.

This distinction changes the optimization target. A faster parser cannot
materially improve a route whose two MCP calls already total 358 ms. The
product must prevent the recovery round and make the first request correct.

## Experiment: remove one complete recovery round

The experiment changes guidance before it changes the transaction language.
The frozen task, fixture, expected bytes, caller, and native control remain
unchanged.

### Stage 1: make the live contract self-consistent

1. Replace obsolete `owner`, `before`, and `after` direct-change guidance with
   the live `files`, `forms`, `find`, `replace`, and `expect` contract.
2. Put one complete six-edit-shaped example in the tool description.
3. Validate that example through the production parameter validator in a
   permanent test.
4. Add a contract test that rejects obsolete field names in direct-change
   documentation.
5. Reload the live registry and verify the result through `tools/list`.

The schema, example, and description must agree. A caller must not need the
skill, source, help, or a refusal to discover the accepted fields.

### Stage 2: measure the one-shot route

Run four fresh MCP replicas and four counterbalanced native replicas. Record
these event times for each correct run:

| Boundary | Meaning |
|---|---|
| Agent start -> first mutation start | Decision translation and request construction |
| Mutation start -> mutation result | Direct tool time |
| Refusal result -> retry start | Recovery interval; zero when the first call succeeds |
| Successful result -> turn complete | Receipt interpretation and final response |
| Agent start -> turn complete | Complete task wall |

The one-shot keep gate is:

- 4 / 4 exact results;
- 4 / 4 first mutation calls accepted;
- one MCP mutation call per run;
- zero discovery, shell, help, skill, and post-success source actions;
- at least 10 seconds lower median wall than the flawed 50.619-second MCP
  lane; and
- at least 5 seconds lower median wall than the fresh matched native lane.

These gates require a measured win. They do not subtract the old recovery
interval from the result.

### Stage 3: localize any remaining wall cost

If first-call adoption is 4 / 4 but the wall gate fails, use the event
boundaries to select one next experiment:

| Dominant interval | Next experiment |
|---|---|
| Agent start -> first mutation | Reduce request-construction load; test a compact owner-grouped representation against the same task before changing the public schema |
| Mutation execution | Profile the transaction kernel, but only if the median is no longer subsecond |
| Successful result -> final response | Make the terminal receipt smaller and strengthen the `next_action=none` instruction |
| High variance without one dominant interval | Increase counterbalanced replicas before changing the product |

Do not add a compact input shape only because it is aesthetically smaller.
Keep it only if a clean caller uses it correctly and it removes at least five
seconds without weakening counts, atomicity, or verification.

### Stage 4: test the exploratory route separately

After the supplied-decision lane closes, run the existing exploratory
falsifier. Experiment 4 below completes this stage:

```text
inspect_clojure -> one model decision -> apply_clojure_changes
```

That lane measures a different advantage: one coherent semantic/source
snapshot can replace several native discovery reads. Do not mix its results
with the zero-discovery supplied-decision lane.

## Revised prediction

The contract correction should remove one failed call and one recovery round.
It is reasonable to expect a material wall improvement because the old caller
already paid to construct the decision before the refusal. The size of the
improvement remains unknown until the four clean replicas finish. The claim to
earn is not “the kernel is fast.” That is already established. The claim to
earn is “the first typed request is correct, and the complete agent turn beats
native by at least five seconds.”

## Server2 field report: safe at scale, two points short of effortless

The next production dogfood task exposed a useful contrast. Surgeon completed
one exact transaction containing 38 symbol edits across nine named Clojure forms
and verified the written result atomically. That is the product's strongest
story: once the decision is expressed, the mutation kernel is precise, bounded,
and trustworthy.

The complete workflow was not yet one-shot. Preserving the public URL key
`:sort-by` while renaming its local binding required a native preparatory patch
before the MCP transaction. Earlier in the same session, all shared-service
preflight signals were green while two actual structural reads returned
`server-not-initialized`; replacing only the server2 semantic session restored
the same read in 140.56 ms.

Assessment: **8 / 10**. Keep the mutation guarantees exactly as they are. Earn
the remaining points by making readiness predictive and by adding a
binding-aware local rename that preserves destructuring keys while changing only
the selected binding and its resolved references. The detailed evidence and
minimum contracts are recorded in the companion friction report.

## The 8 / 10 gaps became compiler operators

The next increment did not add another MCP tool. It added two actions to the
existing verified transaction.

`rename_binding` turns the nine-owner `sort-by` repair into one decision. It
uses clj-kondo binding IDs, preserves the public `:sort-by` key, changes the
`:or` local default, excludes `clojure.core/sort-by`, and retains the normal
atomic commit and undo receipt. The live production-shaped probe completed five
logical occurrences with `verification_complete=true` and restored the exact
original source on undo.

`assoc_entry` separates semantic identity from source trivia. A comment-free
map and a map with line comments can match the same Clojure value. The operator
adds one entry without replacing the map's existing bytes. `inside` selects a
semantic ancestor when equal maps occur in the same owner. This is the narrow
solution to the field failure; it is not a general promise that comments are
disposable.

The urgent concurrency probe found the same principle on the read side:
unseen work is model load. cclsp now bounds semantic surface concurrency per
workspace and publishes active work, queue positions, ages, and deadlines. The
six-query test finishes with typed results instead of an unobservable wait, and
another workspace remains available.

The live handoff also found a control-plane identity bug. “Healthy on port
7890” did not prove “watching the shared workspace config.” cclsp now returns
its canonical config path in health. `clj-surgeon up` compares that identity
before reuse, reloads only its own managed service when the paths differ, and
refuses an unknown port owner. After the one-time correction, server2 joined
the shared stack in 2.0 seconds. Its first semantic request completed in 21.4
seconds and left no active, queued, or outstanding work.

## Experiment 4: the exploratory reader lane also crossed over

The exploratory capsule reuses the exact before and accepted after bytes from
`decision-batch-edit`, but replaces its six source-level instructions with one
behavioral goal. The caller knows only that the IDE and source-reader
experience must change. It must discover the files, candidate owners, exact
forms, replacements, and counts before mutation.

The final MCP route was:

```text
enumerate src/bench filenames
  -> one inspect_clojure batch
       four structural matches
       two files read once
       four candidate defns
       872 source characters
  -> one model decision
  -> one apply_clojure_changes transaction
       six edits
       two files
       exact read-back hashes
  -> terminal answer
```

The matched native lane received the same behavioral task and exact oracle.
Four sequential runs per lane were counterbalanced by reversing lane order for
half the runs.

| Metric | Exploratory Surgeon MCP | Fresh native control |
|---|---:|---:|
| Correct replicas | 4 / 4 | 4 / 4 |
| Complete-turn wall, median | **62.876 s** | 81.730 s |
| Complete-turn wall values | 60.183, 69.181, 55.705, 65.568 s | 75.508, 98.541, 83.705, 79.755 s |
| Mean wall / sample SD | 62.659 / 5.930 s | 84.377 / 10.018 s |
| User-visible turns/task | 1 | 1 |
| Turns/minute | **0.96** | 0.73 |
| Seconds/turn | **62.88** | 81.73 |
| Internal tool actions/turn | **3.0** | 6.5 |
| Discovery round trips | **2.0** | 3.0 |
| Post-decision round trips | **1.0** | 3.5 |
| Shell calls | **1** | 6 |
| MCP calls | 2 | 0 |
| Input tokens, median | **81,187** | 125,869 |
| Uncached input tokens, median | 18,764 | **17,403** |
| Output tokens, median | **2,173** | 2,513 |
| Source surfaced, median | **872 characters** | 4,072 bytes |
| Failed mutations | **0** | 0 |
| One-shot transaction completion | **4 / 4** | not applicable |

Surgeon was 18.855 seconds faster on median wall. Native took 1.30 times as
long; equivalently, Surgeon reduced complete-turn wall by 23.1%. It reduced
internal tool actions by 53.8%, total input tokens by 35.5%, and surfaced about
78.6% less source. The uncached-input median was 7.8% higher, so the total-token
win should not be misread as less novel prompt work in every run.

### Direct reading and writing remained subsecond

| MCP component | Median | Four values |
|---|---:|---|
| Batched structural read | 65.734 ms | 77.264, 70.287, 48.421, 61.180 ms |
| Six-edit transaction | 132.285 ms | 115.345, 118.251, 146.320, 199.753 ms |
| Read plus write | 193.675 ms | 192.609, 188.537, 194.741, 260.934 ms |
| Isolated bootstrap, outside turn clock | 5.052 s | 5.047, 5.056, 5.027, 9.097 s |

The 18.855-second median advantage did not come from a parser optimization.
The complete read and write mechanics consumed about 194 ms median. The win
came from replacing several native discovery, patch, and verification rounds
with one bounded snapshot and one terminal transaction.

### Negative pilots identified the mechanism instead of being hidden

The accepted matrix followed four invalid or recovery-contaminated pilots:

1. the isolated client config exposed only `apply_clojure_changes`, so the MCP
   caller correctly refused to use prohibited native readers;
2. the behavioral task said “load” a stylesheet when the byte oracle required
   replacement, so native made a reasonable additive change;
3. the model-visible schema advertised `verify` for a direct change while the
   live validator rejected it, producing a correct 131.820-second recovery run;
   and
4. an outline-only read led to an exact-form guess, refusal, and two redundant
   rereads, producing a correct 210.488-second recovery run.

The harness now exposes both MCP tools, the capsule says “replace,” total MCP
calls and MCP-surfaced source are scored correctly, and the routing rule states
that an outline alone is not edit evidence. The schema/validator mismatch is
tracked as `clj-surgeon-pgu`; it was not subtracted from any measured result.

## Experiment 5: one exact nested edit is a smaller but replicated win

The lower-bound capsule supplies one file, one named owner, the exact old and
new nested values, and a match count of one. It preserves the attached comment,
audit payload, and all unrelated bytes. Its neutral native control contains no
guarded-editor language and both snapshots have one conventional terminal
newline.

The first four-run MCP matrix was not valid mechanism evidence: every caller
sent the schema-advertised `verify` field, refused, and retried. Its 39.808-
second median lost narrowly to native's 37.548 seconds. After the treatment
stated the actual direct-change contract—send only `changes` and `expect`—the
identical task and fresh native control were rerun.

| Metric | One-shot Surgeon MCP | Fresh native control |
|---|---:|---:|
| Correct replicas | 4 / 4 | 4 / 4 |
| Complete-turn wall, median | **21.595 s** | 26.749 s |
| Complete-turn wall values | 20.444, 19.079, 22.745, 23.417 s | 24.980, 26.611, 26.887, 27.946 s |
| User-visible turns/task | 1 | 1 |
| Turns/minute | **2.79** | 2.24 |
| Seconds/turn | **21.59** | 26.75 |
| Internal tool actions/turn | **1.0** | 3.0 |
| Discovery round trips | **0.0** | 1.0 |
| Post-decision round trips | **1.0** | 2.0 |
| Shell calls | **0** | 2 |
| Input tokens, median | **44,311** | 56,439 |
| Uncached input tokens, median | 8,727 | **8,328** |
| Output tokens, median | **443** | 709 |
| Source surfaced | **0** | 1,023 bytes |
| Failed mutations | **0** | 0 |
| One-shot transaction completion | **4 / 4** | not applicable |

Surgeon was 5.155 seconds faster on median wall. Native took 1.24 times as long;
equivalently, Surgeon reduced complete-turn wall by 19.3%. Every Surgeon value
was below every native value in this matrix, but the absolute advantage is much
smaller than the multi-edit result and should be rerun when model/runtime
conditions change.

The direct one-edit transaction took 115.168 ms median across 201.942, 91.777,
113.817, and 116.518 ms. Isolated bootstrap was 7.218 seconds median and stayed
outside the task clock. A cold process per edit would erase the measured
advantage; the result applies to the product's shared hot-service design.

## The delete/edit/delete stratum exposed a product blocker, not a timing

The fourth capsule names one obsolete Var but withholds its three-site surface:
one definition to delete, one live caller to edit, and one obsolete test to
delete. The intended Surgeon route is `prepare-change` followed by one basis
transaction.

No matched performance result is reported. The shared semantic provider
returned the isolated `/private/tmp` workspace as a path relative to another
configured root (`../../../../private/tmp/...`). `inspect_clojure` refused with
`semantic-evidence-incomplete` because the relative and absolute paths
disagreed. Repeated typed calls preserved source unchanged but could not produce
a basis. This path-identity defect is `clj-surgeon-g08`.

One native pilot completed the now-accepted three-file result in 60.185 seconds,
but there is no comparable Surgeon run, so it is not crossover evidence. The
earlier live field transaction still establishes 6.2 seconds of direct
delete/edit/delete tool time including formatting and lint, not complete agent
wall. This stratum remains the next performance experiment after `g08` closes.

## Revised crossover map

| Stratum | Surgeon median | Native median | Native / Surgeon | Wall reduction | Conclusion |
|---|---:|---:|---:|---:|---|
| One exact nested edit, supplied | **21.595 s** | 26.749 s | 1.24× | 19.3% | Small replicated hot-service win |
| Six edits / two files, supplied | **27.976 s** | 68.932 s | 2.46× | 59.4% | Decisive transaction win |
| Six edits / two files, exploratory | **62.876 s** | 81.730 s | 1.30× | 23.1% | End-to-end reader + transaction win |
| Delete/edit/delete from one Var | — | 60.185 s pilot | — | — | Blocked by semantic path identity |

The falsifiable conclusion is now stronger and still bounded. With a hot
persistent service and a correct first request, Surgeon demonstrated complete-
turn wall-clock advantages for one exact nested edit, a six-edit supplied
decision, and the paired exploratory version of that decision. The gain grows
when one transaction removes more native discovery and bookkeeping. Direct
tool work stays subsecond; schema ambiguity and recovery can erase the entire
advantage. Native patching remains the control for arbitrary text and prose,
and no claim is made here for JavaScript or for the blocked semantic deletion
surface.

The next acceptance run corrected the diagnosis again. The apparent
100-second semantic hang never entered the semantic scheduler: an earlier
cclsp reload had invalidated the caller's MCP session, and the stale-session
JSON-RPC error returned `id: null`. The client therefore waited for a response
ID that could never arrive. Echoing the original ID turned that invisible wait
into an immediate `invalid-mcp-session` refusal. An MCP admission ledger now
proves this boundary directly.

After the fix, the exact sequential request completed in 12.0 seconds and the
six-request acceptance completed in 5.21 seconds. This is a useful warning for
the whole project: a bounded inner kernel cannot bound work that never reaches
it. Deadlines and telemetry must cover the caller-to-kernel transport too.
