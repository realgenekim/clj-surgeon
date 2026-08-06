# Captain's Log: From Microscope to Intent Transaction

<!-- agent-usage-window-end: 2026-08-06T19:34:59.859342Z -->

**Window:** 2026-08-06 16:28:00.284–19:34:59.859 UTC

**Pacific:** 2026-08-06 09:28:00.284–12:34:59.859 PDT
**Question:** What would make clj-surgeon materially faster than native patching,
not merely as safe and nearly as fast?

## Sampling and exclusions

`make study-agent-usage` produced one schema-versioned, anonymized receipt. It
hashed session identities and emitted no transcript prose or workspace paths.
The window contained three Codex sessions, two Clojure-relevant Codex sessions,
and no Clojure-relevant Claude sessions.

One Codex session was this clj-surgeon product-development run. It deliberately
exercised refusal matrices, old aliases, malformed plans, simulated write
failures, and clean-agent probes. Its 78 calls are valid tool evidence but are
excluded from naturalistic error-rate claims.

The second relevant Codex session was a real multi-repository service and
viewer feature. Narrow transcript inspection was limited to its user goals and
tool transitions. Repository, domain, account, and application names are
replaced here with neutral labels.

## Research instrument and exact tools

The study used this bounded sequence:

1. `make study-agent-usage` selected the window from the newest observation
   marker and produced the anonymized counting receipt.
2. `rg --files` located only the two receipt-named Codex evidence files under
   the local session archive.
3. `jq` selected user-message events inside the exact UTC window to recover
   task boundaries and goals. This prose stayed local and is not quoted here.
4. A second bounded `jq` query selected only `custom_tool_call` events from the
   one naturalistic task interval. Inputs were truncated for local inspection.
5. `rg` and the receipt's operation counters cross-checked the repeated
   `:cat`, `:ls`, structural-search, plan/apply, and native-patch routes.
6. The repository self-test verified marker discovery, both provider formats,
   counting, and the privacy contract.

This manual final mile revealed the useful higher-level object: not isolated
commands, but the collapsed route sequence by task. The repo-owned collector is
now schema v2 and emits privacy-safe `route_phases` directly. A phase contains
only behavioral kinds, action counts, Surgeon-call counts, input/output sizes,
and wall time. It never emits prompts, source, command text, or workspace paths.

Future studies should start with:

```bash
make study-agent-usage
```

Raw transcript inspection is permitted only when the receipt's route phases
cannot explain a recovery or failure, and must remain bounded to one named
evidence file and task interval.

## Scoreboard

| Measure | Codex | Claude |
|---|---:|---:|
| Sessions in window | 3 | 0 |
| Clojure-relevant sessions | 2 | 0 |
| Skill visible / loaded | 2 / 2 | — |
| clj-surgeon calls | 379 | 0 |
| Outer Surgeon actions | 179 | 0 |
| Surgeon output | 795,128 chars | 0 |
| Native Clojure patch actions | 60 | 0 |
| Native Clojure shell actions | 94 | 0 |
| Direct Surgeon wall | 232.527 s | 0 |
| Median / p90 Surgeon action | 852 ms / 2.453 s | — |
| Median native patch action | 86 ms | — |

The Claude column is an absence of sampled work, not a comparative result.

## The naturalistic session

The real service-A session touched 89 Clojure files while diagnosing and
implementing a broad data-serving and UI-performance change.

| Operation | Calls |
|---|---:|
| `:cat` | 165 |
| `:ls` | 55 |
| legacy structural search spellings | 26 |
| `:edit` | 14 |
| `:replace-subform` plan | 10 |
| `:replace-subform!` apply | 13 |
| X-ray | 0 |
| Native patch | 51 |

Those calls occupied 146 outer Surgeon actions and 194.175 seconds of direct
tool wall. The five completed task turns took about 95.6 minutes, so direct
Surgeon execution was only 3.4% of turn wall. Tool startup is worth improving,
but it cannot explain or fix the dominant latency. Model/tool round trips,
reconstruction, decisions, and verification own the larger share.

The session had a genuine success: it used no unbounded native Clojure file
reads. `:cat` kept individual inspections precise. Surgeon emitted 616,225
characters inside 2,115,538 total tool-output characters.

The session also exposed the ceiling. The agent used Surgeon as a manual
microscope:

```text
guess names
  -> cat several forms
  -> discover a wrong path
  -> list a namespace
  -> cat neighboring forms
  -> search references
  -> decide
  -> apply a broad native patch
  -> repeat in the next file
```

Precise lenses reduced bytes per read but did not reduce the number of
judgment loops. The agent still reconstructed a project-wide model one form at
a time. When it was ready to write, native patch won because one invocation
could insert new definitions, change several existing forms, edit tests, and
cross file boundaries. Surgeon offered single-target plans and applies.

## Why matching native patch is still remarkable

For one supplied, unique literal edit, native patch is near the irreducible
lower bound. It accepts the desired bytes, writes immediately, and returns a
diff in tens of milliseconds. A structural tool that matches its full-task
speed while adding exact selection, comment preservation, source fencing,
atomic write, whole-file parse, and read-back proof has achieved something
important.

That task cannot honestly become four times faster. There is barely four times
as much workflow left to remove. The 2–4x opportunity begins when a task has
discovery, repetition, insertions, cross-file impact, or several coordinated
edits. Surgeon must beat patch there by accepting a smaller statement of
intent than the unified diff and proving more than the diff can prove.

## The perfect interaction model

Vi does not make the user manually traverse every character. A motion selects
the object; an operator changes it; `.` repeats the operation; `:argdo` applies
it across a set. The analogous agent surface is:

```text
impact target  ->  edit! intent transaction  ->  receipt
       semantic context        one commit         proof + inverse
```

For a known edit, even the impact read disappears:

```text
edit! intent transaction  ->  receipt
```

### 1. `impact` is structural `*` plus tags and references

One bounded project query should return the definition, direct callers,
callees, namespace edges, likely tests, and exact relevant source. It should
use a cached project index rather than ask the model to issue dozens of `:ls`,
`:cat`, and text searches.

```clojure
(impact 'get-known-items-page
        :include [:definition :callers :callees :tests]
        :depth 1)
```

The result is a compact context packet with hashes and explicit truncation. It
is not a namespace dump and does not evaluate code.

### 2. `edit!` accepts a transaction, not zipper choreography

The caller should state guarded changes as Clojure data. The common literal
substitution must not require a path expression plus a duplicated `:expect`.
The `:from` form is both selector and before-state guard.

```clojure
{:changes
 [{:file "src/service.clj"
   :inside 'route-event
   :from :done
   :to :complete}]
 :expect-count 1}
```

For several files, the same command accepts several guarded edits:

```clojure
{:changes
 [{:file "src/store.clj"
   :after 'get-known-items
   :insert [(defn get-known-items-page [owner page size] ...)]}
  {:file "src/handler.clj"
   :inside 'items-fragment
   :from '(get-known-items owner)
   :to '(get-known-items-page owner page size)}
  {:file "test/handler_test.clj"
   :after 'items-fragment-test
   :insert [(deftest items-page-is-bounded ...)]}]
 :verify [:format :parse]}
```

The engine resolves every anchor and match before changing bytes. It applies
all changes in memory, preserves concrete source outside selected spans,
parses every future file, and commits all files or none. Every edit reports its
match count. A mismatch refuses the entire transaction.

### 3. Captured rewrites are structural `.`

Homoiconicity should eliminate copy-and-reconstruct work. Named captures and a
variadic tail can express the desired difference while preserving unknown
source:

```clojure
(rewrite
  '(assoc ?state :status :done ?...)
  '(assoc ?state :status :complete ?...)
  :inside 'route-event
  :expect-count 1)
```

The concrete-syntax engine should replace only the differing literal span when
possible. Captured source is spliced, not pretty-printed. Comments, metadata,
reader forms, shorthand functions, commas, and layout survive.

The same rule can run across a declared impact set with an exact expected
count. That gives the agent a structural refactoring macro without silent bulk
mutation.

### 4. Insert, remove, move, and namespace edits are transaction primitives

The field session chose native patch for 51 actions partly because Surgeon
cannot insert a top-level form or combine several edit kinds in one commit.
The transaction needs a small algebra:

- `:from` / `:to` for exact substitution;
- `:before` or `:after` plus `:insert` for new forms;
- `:remove` with an exact source or hash guard;
- namespace require/refer updates;
- existing move, rename, extraction, and declare operations as transaction
  members.

Do not add a separate CLI command for every edit kind. Keep one transaction
data model and one executor.

### 5. The receipt is verification and undo

One compact receipt should include:

- matched targets and counts;
- original and result hashes for every file;
- exact changed spans and compact diff;
- whole-file parse and atomic-commit evidence;
- formatter and requested check results with elapsed time;
- an inverse, hash-fenced transaction.

The inverse makes undo safe in a dirty worktree: it applies only when the
current result hashes still match. Git remains the durable project history,
but the tool can reverse its own transaction without guessing which unrelated
work belongs to the user.

### 6. Persistence is invisible

The CLI should auto-connect to a per-workspace process that caches parsed
concrete-syntax trees and a reference index, then fall back to one-shot mode if
the process is absent. No human should manage the daemon.

Target latency:

| Route | Target |
|---|---:|
| Cached single-file guarded edit | under 100 ms |
| Cached impact query | under 250 ms |
| Ten-edit multi-file transaction before external tests | under 500 ms |

These targets will not by themselves create the 2–4x full-task win. Their main
value is making one or two intent calls feel instantaneous after the larger
turn collapse.

## What not to build

Do not put another language model inside Surgeon to interpret unrestricted
English. The calling model already owns design judgment. A second model would
add latency, nondeterminism, cost, and an attribution problem.

Do not overload structural matching with regex. Text discovery and structural
identity have different guarantees.

Do not auto-apply a computed transformation whose before-state or match count
the caller did not declare. Exact intent removes review ceremony; inferred
intent still earns a plan.

Do not make adoption the metric. Native patch remains correct for new files,
comment-heavy prose changes, and edits whose full desired bytes are already
the shortest safe specification.

## Smallest falsifiable product experiment

Build one vertical slice of the transaction model. The first draft assumed one
flat list of changes:

```clojure
{:changes [{:file FILE :inside FORM :from OLD :to NEW} ...]
 :expect-counts [1 ...]}
```

Early design review sharpened the hypothesis. The expensive boundary is not
one global replacement. It is repeatedly externalizing and reacquiring a
heterogeneous plan that the model already formed. The first compiler therefore
accepts exact per-intent scopes and aggregate transaction guards:

```clojure
{:intents [{:files [FILE ...]
            :from "EXACT SOURCE FORM"
            :to "EXACT SOURCE FORM"
            :expect-count N}
           ...]
 :expect {:intent-count I
          :edit-count E
          :changed-file-count F}}
```

It must support one or many files, apply atomically, preserve unrelated bytes,
parse every result, and emit an inverse receipt. No captures, insertions, or
project daemon are required for the first experiment.

Replay four completed real prompts that each required at least five Clojure
edits. Compare three lanes from the same parent commit:

| Lane | Surface |
|---|---|
| Native | normal reads and patching |
| Current Surgeon | installed skill and current operations |
| Transaction | one context packet at most, then one guarded transaction |

Correctness, preservation, and truthful verification are gates. Keep the
transaction surface only if it achieves all of these across correct runs:

- at least 2x lower median full-task wall than current Surgeon;
- no slower than native;
- at least 50% fewer source-bearing actions than both controls;
- at least 50% less source output than native;
- zero partial writes after any refused member;
- exact inverse succeeds, and refuses after an intervening change.

If the transaction wins, add captured rewrites and insertion. If it does not,
do not build the daemon or expand the algebra.

## Bottom line

The current tool is a strong structural microscope. The perfect tool is a
structural intent compiler and transaction engine.

```text
CURRENT
ls -> cat -> cat -> match -> cat -> decide -> edit -> plan -> apply -> verify

PERFECT
impact -> edit! -> receipt

KNOWN CHANGE
edit! -> receipt
```

That is the plausible route to a 2–4x win: not faster parsing alone, but turning
dozens of navigation and replay decisions into one guarded statement of intent.

## Frontier update: the first intent compiler dogfood

**Time:** 2026-08-06 13:13 PDT

The microscope baseline is preserved at commit `5d3e262` and annotated tag
`local-microscope-optimum`. The `intent-transactions` branch contains the
contract and first pure compiler.

The red tests found an immediate structural bug: the overlap detector counted
whitespace nodes, but rewrite-clj's semantic zipper addresses skip them. Two
adjacent edits therefore appeared to overlap. Fixing node intervals to use the
same semantic node model produced a permanent regression test before any write
path existed.

The pure compiler then materialized three different hypothetical edits in the
same real namespace:

| Measure | Result |
|---|---:|
| Intent count | 3 |
| Concrete edit count | 3 |
| Changed files | 1 |
| Review diff | 383 bytes |
| Source writes | 0 |

The first CLI dogfood used two different intents in two real repository files:

| Measure | Result |
|---|---:|
| Intent count | 2 |
| Concrete edit count | 2 |
| Changed files | 2 |
| CLI wall | 0.55 s including process startup and pretty printing |
| Source writes | 0 |

The command read each file once, compiled both intents against the original
snapshots, returned per-intent and per-file counts, concrete addresses, source
and result hashes, one combined diff, and whole-file parse proof. A deliberately
wrong per-intent count exited 1 with `:expect-count-mismatch` and the actual
per-file count.

This is not the speed result yet: no guarded commit, rollback, inverse, or
native control exists. It is the first evidence for the interface hypothesis.
One model plan can already become one bounded review artifact without plan/file
writes or repeated edit turns.

## Queued read-side field failure: three guesses, then surrender

A separate clean caller reported this exploration route on large Clojure
namespaces:

```text
:cat by distinctive text -> zero
:cat by different distinctive text -> zero
:cat by distinctive text -> ambiguous
skill's three-read ceiling -> rg + bounded line reads
```

The reads were bounded, not whole-file dumps. That limits damage but does not
meet the product standard. The caller also skipped the repository's stricter
first-inspection `:ls` rule for files over 500 lines. The skill and project
instructions therefore disagreed about the recovery route, and neither made
the successful structural path obvious.

This is the read-side analogue of repeated edit materialization. The model had
one exploration goal, but the interface required independent selector guesses.
Each failure should have compiled into evidence for the next move.

Queue a faithful, anonymized regression study after the transaction slice:

- replay the exact zero/zero/ambiguous selector sequence;
- inspect the actual candidate and remedy EDN, not only the caller's summary;
- require zero-match output to offer a directly runnable nearest-owner route;
- require ambiguity output to return a compact reusable candidate table;
- test one bounded resolver that accepts the accumulated clues instead of
  forcing a fallback to line reads;
- reconcile the three-source-read ceiling with the mandatory first `:ls` rule;
- measure whether the repaired route beats `rg` plus bounded source reads in
  calls, wall time, and source bytes.

The long-term surface remains symmetric:

```text
impact -> change! -> receipt
```

`change!` materializes the model's write plan once. `impact` should eventually
materialize its exploration question once.

## Queued experiment: discharge decisions into an EDNL intent stack

One large `:spec` still asks the model to retain every decided edit until the
end of planning. An optional append-only EDNL stack could externalize each
operation as soon as the model decides it. Pushes would validate proposal data
only and return a tiny count plus chain hash; they would never read or write
source. One later `:change` or `:change!` would compile the hash-fenced stack
against a single set of source snapshots.

This may beat 23 plan/apply cycles even if it uses several shell calls: pushes
carry no source, require no review diff, and avoid reacquiring earlier plan
state after compaction or a long investigation. It may still lose to one large
spec because Babashka process startup is not free. Benchmark rather than assume:

| Lane | Hypothesized advantage |
|---|---|
| One large `:spec` | Fewest process calls |
| EDNL pushes plus one commit | Lowest model working-memory and reconstruction cost |
| Independent plans/applies | Existing safety baseline, highest ceremony |

The compiler must normalize both the in-memory spec and EDNL records into the
same intent representation. Mutation stays out of scope until the direct spec
transaction, rollback, and inverse are proven.

## Frontier update: the failure-atomic commit substrate

The first mutation batch remains internal; `:change!` is not public until a
durable inverse receipt exists. The commit protocol now proves these cases with
injected source I/O:

| Failure point | Required outcome | Result |
|---|---|---|
| Stale file during all-file preflight | Zero writes | passed |
| Second write throws after changing bytes | Restore both exact originals | passed |
| Read-back verification lies once | Detect, restore, verify originals | passed |
| Later file changes concurrently | Restore owned result; preserve unknown bytes | recovery required, passed |
| Rollback write itself fails | Report exact partial state | recovery required, passed |

Rollback classifies current bytes by hash. Original bytes need no action;
transaction-result bytes are safe to restore; any third state is never
overwritten. This prevents an “atomic” recovery claim from clobbering a user's
concurrent edit.

Filesystem dogfood copied the two realistic UI fixtures, compiled two intents
into three edits across both files, committed them with real atomic per-file
renames, and verified both read-back hashes. Complete planning plus commit took
0.42 seconds in the Babashka process. This was substrate evidence; the next
dogfood exercised the public boundary.

## Frontier update: the durable transaction and inverse work end to end

The first public mutation dogfood supplied one EDN spec containing three
heterogeneous intents. It changed four exact forms across two copied Clojure
files, published one inverse receipt, and then restored both original files:

| Measure | Result |
|---|---:|
| Intent count | 3 |
| Concrete edit count | 4 |
| Changed files | 2 |
| Forward plus undo wall | approximately 0.5 s in one Babashka process |
| Console result | compact counts, hashes, verification, and receipt path |
| Durable receipt | 3,466 bytes |
| Final file hashes | exactly equal to both starting hashes |

The fixtures contained a preserved anonymous function, a nearby textual
lookalike, a body-attached comment, Hiccup, and unrelated source. The exact
`:from` forms changed. `#()` did not become `fn*`, the textual lookalike did not
match, and the comment remained byte-for-byte intact.

The first easy inverse passed, but a stronger permanent test immediately found
a real positional bug. Two sibling forms were each replaced by a larger tree.
The forward transaction succeeded, but inverse replay interpreted a stored
root coordinate relative to the first top-level form instead of the synthetic
whole-file forms node. Preorder fallback could not safely compensate because
the first replacement had changed later preorder positions.

The fix made semantic child-index paths authoritative whenever they are
present and replayed them from the whole-file forms root. A corrupt semantic
path now refuses as an invalid receipt; it never falls back to a coincidentally
matching preorder address. The original failing shape-changing case remains a
permanent test.

Public-boundary tests now prove:

- exact forward apply and byte-exact inverse across two files;
- a second undo refuses before writing;
- one stale file refuses the complete inverse;
- corrupt paths, counts, hashes, and versions refuse;
- source/receipt aliasing and missing receipt parents refuse before mutation;
- a plan refusal preserves an existing receipt;
- receipt publication failure restores all source and preserves the prior
  receipt;
- handled write, read-back, and rollback failures retain their earlier atomic
  guarantees.

The complete repository gate passed 510 tests and 4,131 assertions with zero
failures. This is still not the comparative speed result. It proves that one
externalized model plan can safely become one transaction and one reversible
artifact. Batch 5 must now compare clean callers against the tagged microscope
and native patching.

The EDNL queue hypothesis is narrower after this run. Externalization itself
clearly helped: three decisions left model working memory as one executable
spec and did not become repeated plan/apply turns. Incremental pushes remain
unproven. They should be added only if long-plan trials show that tiny,
source-free appends reduce reconstruction cost enough to repay extra process
startups.

## Clean callers kept the plan whole

Two clean Codex lanes and one clean Claude Fable lane received the same task:
three exact intents, four edits, two copied files, a durable inverse, and exact
restoration. The prompts did not name a clj-surgeon operation. The native lane
forbade clj-surgeon. The skill lanes could read only the installed 90-line
skill and the named fixtures.

| Lane | Edit route | Correct | Edit one-shot | Calls reported | Durable inverse |
|---|---|---:|---:|---:|---:|
| Installed-skill Codex | one `:change!`, one `:undo-change!` | yes | yes | 9 shell calls, including rejected cleanup | 2,864 B receipt |
| Installed-skill Claude Fable, first guidance | refused `:change!`, corrected `:change!`, `:undo-change!` | yes | no | 4 shell calls, 3 Surgeon invocations | 2,612 B receipt |
| Native control | one `apply_patch`, archive restore | yes | yes | 5 tool actions, including rejected cleanup | 7,168 B tar archive |

Codex read no fixture source before mutation. It expressed the complete plan as
one spec, committed it once, and restored both exact starting hashes. Its
reported direct tool wall was approximately 3.9 seconds. The native control
used one targeted `rg`, one multi-file patch, and a tar archive; it reported 25
seconds from temporary setup through verified restoration.

Those wall values are not a controlled speed comparison. The Codex lane
reported summed tool wall, while the native lane reported elapsed workflow time.
Claude's external process reported 98.9 seconds, seven turns, and 6,759 output
tokens, but it used a different model and execution surface. Preserve the route
evidence and do not aggregate these values into a speed claim.

The clean Claude run found one actionable skill defect. It saw
`:expect-count` in the example but did not understand that every intent
requires it. The first call refused with `:invalid-expect-count` and changed no
bytes. The second call inferred all counts and committed the complete plan.
The 90-line skill now states both requirements explicitly: every intent needs a
positive `:expect-count`, and aggregate `:expect` needs exact intent, edit, and
changed-file counts. This sentence is a permanent cross-agent contract test.

The result narrows the performance hypothesis. For a short task, native
`apply_patch` remains a fearsome one-action competitor. `:change!` does not win
merely by reducing the mutation action from one to one. It earns its place by
letting the caller skip source reconstruction, structurally excluding textual
lookalikes, compiling all exact intents against one snapshot, and producing a
smaller machine-verifiable inverse. A replicated harness must measure complete
turn wall and tokens before claiming a speed win.
