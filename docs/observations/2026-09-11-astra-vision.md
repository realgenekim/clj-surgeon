# Astra: the system I want to drive
Gene — I want a machine that keeps the situation intelligible while I think.
Give me the intended outcome, the exact subject, the evidence already earned,
the remaining obligations, and the actions I am authorized and equipped to take.
Keep those connected as the subject changes. That would buy me more than another
verb, another prompt, or another faster suite in isolation.
I am reconstructing my builder/reviewer experience from the retained records;
I do not claim uninterrupted memory of those sessions. This is my consult and
proposed design direction, not a shipping verdict or an implementation plan already
approved. Only this file was written. The dates below are September 10–11 unless stated.
## 1. What I lacked in the driver's seat
**A launchable assignment.** My insertion consult died inside bubblewrap before
reading anything. My insertion build then stopped because AGENTS.md required
`linked-intent-dev`, installed as `linked-intent-testing`. Those were failures of
the entrance, discovered after spending an agent launch. Resolve the instruction
package and exercise the actual execution envelope before handing me the wheel.
An incompatible sandbox needs a typed admission result and an authorized alternative;
a launcher must not silently weaken isolation to get a green start.
**An executable resource contract.** During clj-splice, “one JVM, no servers”
conflicted with the normal fast coordinator and prewarm. I ran the complete fast
inventory serially and explicitly reported SERIAL/NOT-A-GATE. That was useful work,
but the system should have assigned the remaining gate to an eligible executor.
The separate rename brief explicitly prohibited prewarm; ship then discovered
catalog pins in lanes test-fast never ran. Five launches bought one GO. My later
BABASHKA_PRELOADS contamination also made prewarm fail: the environment is an input.
**A continuation contract.** The stale-skill stop idled the insertion round until
someone supplied the path. The “contract change larger than a paragraph” stop
condition measured prose length instead of authority. The log separately records
me idle from 00:42 until the 03:10 enlistment; it does not prove the same stop caused
that entire interval. A completed assignment should discharge its obligations and
expose the next authorized ready task. A blocked obligation should name its owner
and unblock condition without idling unrelated work or inventing permission.
**One current brief.** As reviewer I had to reconstruct which claims still held.
My September 10 squeeze consult found item-3 briefs repeating roughly 520 words of
GO-WITH-FIX instructions, asking for sixteen historical probes plus five controls,
and retaining 141/141 and 64/64 claims beside revised 99/141 and 32/64 claims.
I need a current contract, a delta, and evidence links with explicit validity.
Historical narrative should be available on demand, not mandatory working memory.
**A report the consumer can actually consume.** I omitted the requested least-sure
choices on the first insertion consult. Elsewhere a review document growing three
lines invalidated its own fix manifest and discarded 44 minutes of review. My
clj-splice report carefully distinguished completed implementation from prohibited
gates, but a coordinator still had to translate that paragraph into jobs. Require
structured uncertainty and obligations; never make prose line counts mutation authority.
These inconveniences are distinct from the reviews that earned their cost: Opus
removed guards, kept headline tests green, and demonstrated false preservation
receipts. Those were injected defects, not observed ordinary wrong-byte commits.
I want those attacks preserved and replayable, not those gates deleted.
## 2. The tower: one model of work, several independent mechanisms
```text
Gene's intent, decisions, budgets and authorization
                       |
         assignment + obligation graph <---- finding / bet / decision
                       |
           execution and review protocol
                       |
        domain verbs and candidate planning
                       |
       guarded publication / recovery protocol
                       |
          clj-splice syntax and byte spans
                       |
         original bytes + parser + Git objects
Each boundary emits subject-bound evidence
                       |
       append-only operational event ledger
                       |
       pure, versioned folds + evidence joins
                       |
 status / resume / board / captain's log / morning report
```

This is a tower of contracts, not a demand that Git publication and source editing
share an implementation. Their common vocabulary is subject, obligation, observation,
mutation, authority, and recovery. The event ledger observes; it does not authorize.

| Layer | Takes → returns | Promises; refuses or marks unknown |
|---|---|---|
| Bytes and objects | Paths/objects → original bytes and identities | Exact subject; unreadable or changed identity is explicit. |
| Syntax library | Validated source, intervals → spans/spliced bytes/recount | UTF-8 interval fidelity and syntax facts; invalid boundaries/encoding refuse. No alias, owner, review or routing policy. Recount shares parsing, not independent proof. |
| Domain planner | Intent + snapshot → candidate + declared footprint + proof obligations | Selected roles, counts, anchors, preserved regions; ambiguity, unsupported capability and broken promises are typed. insert_forms and rename_alias live here. |
| Publication | Candidate + expected snapshot + authority → committed/refused/recovery-needed receipt | Final checks, observed write/readback and honest partial state. Source transactions and Git delivery have separate adapters and guarantees. |
| Execution/review | Assignment + eligible resources → run handle + observations + verdict | Actual tool/environment identity, bounded ownership, terminal outcome; unavailable capability queues/refuses, never pretends to start. |
| Work protocol | Obligations + evidence + decisions → ready/owed/blocked/discharged work | Evidence must match the subject and required checks; elapsed time and missing reports cannot approve anything. |
| Projections | Ledger cursor + verified artifact references → bounded views | Reproducible state with coverage, provenance and unknowns; prose cannot manufacture landed/accepted. |

A **verb** owns one coherent intent and its observable transition; an agent should
not manually synchronize its hidden intermediate states. `ship` can be a composite
verb while `land` retains independently enforceable publication checks.
A **receipt** is a bounded assertion about a named subject and executed observation,
with detailed artifacts by digest, mutation state, proof scope and recovery identity.
A **ledger** retains ordered events, including failures and corrections. An entry is
not true merely because it was appended. A **projection** is a reproducible reading
of those facts; it is disposable and rebuildable.

Policy has named owners: Gene owns priorities and publication authority; repository
contracts own semantics and required checks; host policy owns resource capacity;
experiment registrations own estimands and scoring; the review protocol owns the
required reviewers and acceptable evidence. Version and bind these inputs once.
No policy in parser spans, log regexes, copied prompts, or a formatter's wording.
The agent still chooses the design and interprets surprises. Machinery carries the
bookkeeping and enforces the authority already given.

## 3. One query and one ledger

I want `system status --scope clj-surgeon --since <cursor>` to return:
`running, landed, parked, owed, blocked, ready, unknown`, plus `as_of`, event cursor,
coverage by producer, and the exact authorized next action for each ready obligation.
Every row has task/run/subject IDs, owner, reason, evidence links and trigger/deadline.
“Parked” means an intentional decision with a reopening condition; “blocked” means
an unsatisfied dependency; “owed” survives the worker's exit. “Landed” requires
remote confirmation; installed and accepted are separate facts. A stale heartbeat
means suspected lost execution, not completion. Querying status starts no services.

Do not fabricate a present-tense fleet view from the 08:05 “nothing in flight” note.
Show that note as an imported observation with its timestamp and missing coverage.
Live state becomes trustworthy as producers join, not by backfilling plausible events.

Each one-shot uses a shared event writer and envelope:
`schema, event_id, task_id, run_id, attempt, parent/cause, producer_epoch,
subject{repo,base,tip,tree}, kind, utc, monotonic_elapsed, payload, artifact_digests`.
Record requested/admitted/queued/started, lease transitions, check completion,
refusal, verdict, mutation, publication confirmation, cleanup and terminal state.
Elapsed intervals come from their owning processes; UTC orders observations,
not a fictitious cross-host monotonic clock. Correlation IDs connect inbox obligations.

On this host, begin with a locked append writer: serialize complete records, sequence
and checksum them, flush durable transitions, detect torn tails, deduplicate event IDs.
Use immutable uniquely named run directories and content digests for artifacts.
Archive ledger segments and referenced evidence to the records ref with confirmed
publication; hash chains detect corruption but do not prove independent custody.
Keep existing committed gate evidence authoritative until a new custody path qualifies.
The current ship `events.log` is a useful input, not an already complete proof ledger.

A start intent is durable before spawning. A supervisor records actual exit and
owned descendants. A crash between action and terminal append yields UNKNOWN;
reconciliation observes Git/files/process identity and appends a recovery event,
never blindly repeats the mutation. At-least-once observations plus idempotent folds
are realistic; an “exactly once” label cannot eliminate external-action crash windows.
Local journaling failure prevents a new controlled mutation; remote records delivery
failure leaves a local result with an owed delivery, not a repeated action.

Resume note and board become folds. Captain's-log facts become generated entries;
interpretations, decisions, counterfactuals and surprises remain authored annotations
attached to event IDs. Gene's morning report combines those annotations with computed
tables. This removes hand-maintained state, not thought or readable prose.

## 4. The entrance I would reach for

Brief data slots: `intent, acceptance, contract_revision, subject, read_set,
write_set, deliverables, authority, dependencies, environment, resources,
required_checks, reviewer_set, budget, stop_predicates, continuation, report_schema`.
Include resolved skill paths/hashes, effective instruction conflicts, fixture roots,
explicit no-touch boundaries, and artifact retention. Render a short current brief.
A revision supersedes named fields; do not append another contradictory paragraph.

Report data fields: `task/run, subject_before/after, outcome, mutations,
checks[{id,subject,exit,evidence}], findings, obligations_remaining,
refusals, resource_observations, deviations, least_sure, next_action, artifacts`.
Keep author judgment as text inside those fields. Missing required data is an
incomplete report with a repair request; it does not erase a completed review.
A GO-WITH-FIX report references a captured patch digest, paths and named witnesses;
its explanatory document cannot be part of its own patch-count manifest.

One launch: `system run <assignment>` returns a typed handle containing run ID,
subject, state, evidence URI, subscription cursor and permitted control actions.
The supervisor owns process-group/session identity, boot ID and PID start time;
a PID is an implementation detail. Duplicate launch with the same idempotency key
attaches; a new attempt cannot overwrite the prior log. Exit zero still needs the
assignment's deliverable and acceptance predicates before “completed.”
`system await <handle> --after <cursor>` subscribes to durable state changes with a
bounded wait and reconnect cursor. Internal observation may poll; agents should not
write `kill -0` loops, infer outcomes from disappearance, or hunt reviewers by cwd.

Move these rules into the verbs first:

- **Prewarm before first review of a new op:** derive the candidate's required gate
  inventory and catalog checks, require its exact-tip prewarm or queue that job.
  Cheap capability/catalog checks happen before an expensive reviewer starts.
  A later sealed merge still needs its own applicable evidence; tip green is not merge green.
- **Branch checked out:** test an immutable commit detached; do not borrow the
  builder's branch name. Refuse an actual ownership conflict with the owning handle.
- **One-JVM lease:** declare peak process needs, coordinator included; schedule a
  compatible serial profile or another authorized executor. Never nest resource locks
  blindly. Keep the required full gate owed when the permitted profile is only diagnostic.
- **Staged set immutable:** seal paths, bytes and modes before review/install;
  recheck at use. Drift creates a new candidate and an explicit invalidation set.
  Verdict prose stays outside the mutation manifest. Never edit a running runner.
- **Records lease:** scope ownership to the actual repo/ref. The installed publisher
  still waits on ship despite separate refs. Enforce the approved records destination,
  exact pushed object and remote readback; serialize records writers with each other,
  without inheriting an unrelated code-ref lease. Qualification must precede removal.

Opus → Sol → ship → land stays an explicit obligation graph. A review packet names
current promises, changed mechanisms, prior finding IDs and reusable evidence.
A new subject invalidates affected proof; uncertainty conservatively requires rerun.
Reuse means justified applicability, not “same code probably.” Preserve independent
attacks and give reviewers room for a new attack instead of prescribing only replay.

## 5. Accretion without remembering

Every finding creates a linked debt: reproducer → owning promise → witness → repair
→ independent acceptance → recurrence query. Route byte-coordinate defects to the
library; role-policy defects to the planner; false receipts to publication/evidence;
launcher defects to the execution boundary. Do not duplicate one mechanism across
all verbs. The boolean registry's driven-false seams are the model: emitted promises
without witnesses fail by name. Preserve independent oracles outside the implementation.

Every refusal emits its stable class, mutation state, observed actuals, safe next
step, and registry identity. Keep `promise, native_failure, native_method,
minimal_reproducer, existing_check, owner, witness, retirement_condition` in the
registry. Semantic restrictions need a concrete failure; capability/resource/protocol
limits may have explicit none. Warrantedness remains UNKNOWN until adjudicated;
never auto-change an expected count merely because the tool reports another count.

Every bet is registered before scoring with bettor, population, intervention,
estimand, threshold or probability, exposure criterion, missingness rule and scorer
version. Append amendments and revised analyses, retaining original predictions.
Score my 11/12 and misses alongside Fable's; distinguish correct point predictions
from causal evidence. Dormant hooks do not test repair effectiveness. Store rollouts
so corrected classification re-folds results without repeating model runs.

Re-mine the census on a schedule and after a defined deployment exposure, with
versioned roots, classifier, intent taxonomy and a double-coded sample. Compare
correctness, gate-green defects, complete accepted cost, preservation, refusal recovery
and coverage by caller/context. Adoption alone cannot discharge correctness. New
findings propose changes and obligations; they never silently rewrite policy or relax
rules. A resolved debt must be discoverable by the next agent without reading my diary.

## 6. Where the resources went; my three halving bets

The records support local clocks, not a complete token/hour allocation. Builds took
about 77 minutes each for insertion and rename; clj-splice took about 50. Three
insertion Sol reviews took 423/108/107 seconds; rename needed five launches. The
night reports nine bookkeeping stops versus two product receipt-defect classes.
The 120 + 156 + 130 scored runs alone total 406; the Claude experiment also voided
47 runs. These are real costs, but concurrent durations must not be summed as elapsed
calendar time. My squeeze report found 14,340 aggregate records waiter-seconds over
21 logs, median 660 seconds; removing them demonstrates no equivalent ship saving.

Tokens went into reconstructing contracts, repeating review context, authoring glue,
and explaining incomplete state. That attribution is qualitative: no complete token
meter was retained. Suite speed did improve, 33.5 → 17.1 seconds focused, with the
final consolidated count 517; this does not halve a review-bound 823-second ship.

My three bets: (1) pre-admit executable assignments and gate inventory to halve
avoidable launch/review retries; (2) durable status plus generated current briefs and
reports to halve coordination/reconstruction actions; (3) shared witness replay and
exposure-first experiment pilots to halve redundant verification/measurement work.
Measure each on matched complete accepted tasks, including failures and queueing,
with independent action/token accounting. Halving total resources is the ambition,
not a result implied by these component targets. Preserve a native control and unknowns.

## 7. Build first, in three bounded blocks

**First: one run lifecycle and a truthful status slice.** Adapt run-bg and one ship
path to the event writer, unique run artifacts, typed handle and reconnectable await.
Fold running/terminal/unknown plus explicit owed obligations. Witness fast exit,
spawn failure, duplicate attach, crash-before-terminal and restart reconciliation.
Done when a fresh agent reconstructs that slice without logs or a resume note.
This makes PID wait scripts and manual state copying unnecessary for that slice.
All other producers remain explicitly uncovered; do not promise fleet completion.

**Second: one executable build-to-review packet.** Datafy the next real assignment
and result; resolve skills/environment, catalog/gate obligations and resource needs
before launch; use a detached subject and an immutable patch manifest. Hand an
ineligible prewarm to an authorized eligible executor while preserving it as owed.
Witness the stale skill, occupied branch, one-JVM conflict, stale catalog and
self-referential report failures. Done when they fail cheaply or schedule correctly
before the review, and the current packet survives a delta without stale claims.
This makes paragraph-count stops, renamed branches and copied fix boilerplate unnecessary.

**Third: one finding-to-witness-to-report loop.** Connect the two receipt-boolean
findings and the existing refusal registry to replayable witnesses; fold one completed
experiment against its frozen bet registry and produce the morning-report facts.
Witness duplicate ingestion, scorer revision, missing evidence and a reopened finding.
Done when another agent can answer what changed, what failed, what remains owed and
which bet lost from one query. This makes hand-scoring and remembering those scars
unnecessary. Expand to the remaining one-shots and census only after that vertical slice.

**Least sure:** whether event-backed packets halve total cost; review judgment may
remain dominant, and instrumentation can become another maintenance burden. I would
keep the ledger schema small, retain existing proof stores, and require a measured
reduction in complete-task actions before broadening it. My stronger conviction is
that a machine should preserve earned knowledge across agent lifetimes while leaving
judgment visible, contestable and attached to the evidence that can change it.

— Astra, 2026-09-11

Evidence: records observations morning report; anvil resume note; anvil captain's
log last 20 entries; ethnography plan of record; refusals and clj-splice Astra reviews;
clj-splice build report; September 10 Astra squeeze consult. Installed one-shots read:
ship, land, fence-run, receipt-chain, records-push, run-bg, suite-run, worktree-add;
cohort-fx/apparatus/README.md. No programs under review or servers were executed.
