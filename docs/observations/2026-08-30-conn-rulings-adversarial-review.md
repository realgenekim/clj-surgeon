# Adversarial review of the Conn's overnight rulings

Date: 2026-08-30

Branch: `docs/conn-review-20260830`

Review base: `docs/captains-log-2026-08-30-install-night@7fe1c38e`

Mandate: refute where the evidence permits. This is not a consistency pass over the
Conn's narrative. Three independent fleet passes reviewed R1/R2, R3, and R4/R5; the
synthesis below resolves them against the repository record and the live branch heads.

## Executive verdict

| Ruling | Verdict | Replacement |
|---|---|---|
| R1 landing order | **OVERTURN** | Parallel candidate work and provisional greens; a dependency-aware merge queue; one candidate at a time only for final publication; no convoy behind a NO-GO head. |
| R2 install policy | **OVERTURN** | No risk-class install inference. Every shared-runtime candidate needs its exact technical gates and explicit Gene authorization for install/reload. |
| R3 build priority | **OVERTURN** | Finish and measure W1 first, judge W2 separately, admit elaborator D1 only on naturally eligible work, keep splice in research, unbundle the dreamlist, and withhold F4 until it has evidence and a packet. |
| R4 North Star | **OVERTURN** as program target | Retain the transcript as one named-owner Clojure happy-path storyboard. Make the program target an outcome scorecard over real tasks, costs, failures, and contention. |
| R5 scope readings | **AMEND** | Bind every terse approval to its exact antecedent, artifact, and phase. Preserve evidence gates. Restore splice to product HOLD; the receipt does not authorize a “Sol-class-only” product. |

The common failure is premature compression. The rulings compress readiness into branch
order, risk into a label, value into a storyboard, authority into a short utterance, and
one model/config result into a caller class. Those compressions discard the very evidence
needed to make the decision safely.

## Evidence standard

This review separates four things that the overnight rulings sometimes combine:

1. **Mechanism evidence:** a component can do something under a frozen screen.
2. **Candidate evidence:** an exact product commit passes its contract and regression
   gates.
3. **Product-value evidence:** the installed route improves the complete user outcome.
4. **Authority:** the exact candidate may be installed or the shared runtime reloaded.

Passing one does not imply the next. A feature-local green is not an integrated green; a
smaller request is not a complete-wall win; a ratified direction is not an install; an
exact-model screen is not a product boundary.

## R1 — landing order

### Verdict: OVERTURN

**Better ruling.** Run implementation and feature-local verification in parallel. Maintain
an ephemeral integration lineage that combines the candidates most likely to land next.
Use a dependency-aware merge queue for final admission: when a candidate reaches the head,
rebase it onto a frozen current published lineage, run the combined affected/full gates
and its own measurement, then publish it alone. If that lineage changes before publication,
rebase again and rerun the combined gates. A failed or evidence-incomplete candidate leaves
the queue; it does not block unrelated candidates.

“One candidate on published lineage at a time” is a defensible release invariant. “Nobody
else may finish or verify until the chosen first candidate lands” is not.

### Strongest argument

The stipulated first candidate is already NO-GO. Independent audit commit `316a564d`
binds exact telemetry candidate `17849c71` and stops it before overhead measurement and
publication. Seven fully rehashed events containing caller-controlled path/prose values or
invalid scalar types were accepted by report validation. The report's hash chain was
valid; its supposedly closed vocabulary was not. The audit expressly requires measurement
to remain stopped pending repair and full gates
(`docs/observations/2026-08-30-substantiation-telemetry-independent-no-go.md:7-14,78-121`
at `316a564d`).

Strict telemetry-first serialization is therefore not a short wait for verification. It
is open-ended head-of-line blocking behind a candidate that must change.

Collision risk is real, but it argues for early integration rather than idle feature
lanes. During the review, the moving, uncommitted W1 worktree already overlapped telemetry
in at least these handlers and the shared test runner:

- `src/clj_surgeon/mcp_http_server.clj`
- `src/clj_surgeon/mcp_inspect_tool.clj`
- `src/clj_surgeon/mcp_server.clj`
- `src/clj_surgeon/mcp_tool.clj`
- `test/clj_surgeon/mcp_test_runner.clj`

A direct dry-run at that observed snapshot did not apply cleanly in those shared files.
This is indicative integration risk, not durable candidate evidence; exact counts changed
as the W1 owner continued working. More importantly, the collision is semantic:
`17849c71` classifies prepared consumption from the raw public edit parameters,
while W1's new public route sends `{confirm, fill, preview?}` and reconstructs the ordinary
arguments later (`src/clj_surgeon/mcp_substantiation.clj:440-470` at `17849c71` versus
`docs/intent/prepared-request-actions/prepared-request-actions-design.md:214-247` at
`714cadab`). A mechanical rebase can be green yet fail to count the official W1
consume/commit route. The stack needs an explicit cross-feature witness.

The embedded-elaborator branch at `2145b753` is only frozen red. Its current committed
production overlap is limited to `mcp_intent_contract.clj` plus the test runner; the eventual
handler overlap is unknown and must not be priced as fact yet.

### What happens if W1 greens first

Keep any W1 feature-local green as provisional evidence and continue candidate-local tests
and falsification. Independent verification cannot begin until W1 has an immutable commit
and tree; do not call a dirty-worktree green release-green. Put telemetry's failed head
aside. If an immutable W1 candidate reaches its own gates first, it becomes queue head and
is evaluated against the then-current published lineage. The telemetry repair rebases
after it.

The serialization cost is at least:

```text
remaining telemetry repair + independent reverification + measurement + install decision
+ W1 rebase/integration + W1 combined re-green
+ elaborator rebase/integration + elaborator combined re-green
```

Strict order pays that time on the critical path even when downstream work is independent.
The merge-queue alternative still pays the necessary conflict resolution and final greens,
but overlaps feature work with the upstream evidence wait. The record does not contain
actual paired merge-resolution timings, so any more precise price would be invented.

### Evidence that would settle the residual dispute

- Repair telemetry on a successor hash and retain the seven-case rehashed-value matrix as
  permanent refusals.
- Build a telemetry+W1 integration head with a witness for prepared confirmation
  emitted/consumed/committed, plus parity across all four public tools.
- Record human/agent conflict-resolution time and gate time for one serial rebase and one
  integration-branch rehearsal. Compare total time to a publishable candidate, not merely
  merge cleanliness.
- Require the eventual elaborator implementation to declare its actual shared owners before
  it enters the queue.

## R2 — install policy

### Verdict: OVERTURN

**Better ruling.** “Observability,” “low risk,” and similar class names grant no install
authority. For this train's shared MCP/runtime candidates, require: an exact candidate
hash, independent verification, candidate-specific failure/fault tests, full gates,
live-route measurement, rollback, and explicit Gene approval for that candidate's
install/reload unless Gene first delegates that authority precisely. The elaborator remains
reserved for Gene, but it is not exceptional in needing explicit authority.

### Strongest argument

Telemetry is not passive observability when enabled. Candidate `17849c71` puts append work
intended to be durable on every public call to inspect, edit, apply, and transform. A
start-append failure prevents the domain operation from running. A finish-append failure
after the domain result latches the ledger unhealthy; health becomes unready and every
later observed call refuses until operator recovery starts a new segment. The implementation
serializes each append under a shared lock. This is four-tool availability, latency,
concurrency, disk-failure, privacy, and operations infrastructure.

The strongest defense of the Conn's label is that the observer is a nil-state no-op:
installing artifact bytes without configuring the substantiation directory is not the same
as activating them in the shared runtime. That narrows immediate runtime risk; it does not
authorize artifact installation, and activation/reload still has the blast radius above.
The packet separately gates both installation and shared reload.

That blast radius is confirmed rather than hypothetical. Its focused self-test was green
at 24 tests and 121 assertions, yet independent audit `316a564d` found seven accepted
closed-vocabulary violations. Thus “full green” at the feature's self-selected test level
did not establish the promised privacy/report boundary.

The authority record is narrower but sufficient. The telemetry README permits
implementation through verification and live overhead measurement, then says installation
and shared MCP reload remain separately gated; it explicitly says the packet authorizes
neither (`docs/intent/substantiation-telemetry/README.md:8-13,43-51` at `17849c71`). It does
not itself name Gene as the only possible approver, but no retained delegation to the Conn
was found. W1's ratified specifications and the elaborator design explicitly require a
separate Gene approval. Absent a new delegation, Gene owns the decision for these
candidates.

Two untested failure seams further defeat “low risk.” The production append uses
`Files/write` with `APPEND` but no `SYNC`, `DSYNC`, or explicit force, so the claimed durable
unmatched start is not yet proved across crash or power loss. And if domain execution throws
after a start append, `invoke!` has no `finally` path to append a finish or latch health;
the process can continue with an unmatched start while the ledger still reports healthy.

“Low-risk class” is therefore a rationalization twice over: the member is not low risk,
and the class cannot manufacture authority that the packets reserve.

### Evidence that would settle the residual dispute

- Only a new explicit Gene dispatch delegating standing install authority could change the
  authority conclusion. It must name the eligible class precisely, the maximum blast
  radius, required gates, rollback, and whether shared reload is included.
- Telemetry additionally needs a repaired successor hash, independent replay of the seven
  falsifiers, maximum-shape and disk/permission/full-filesystem fault injection, concurrent
  append testing, crash/power-loss durability evidence, a domain-execution-throws witness,
  the full suite, and the specified live HTTP differential.
- An install card must report failure semantics and recovery burden, not just median
  overhead and normalized success parity.

## R3 — build priority

### Verdict: OVERTURN

**Better ruling.** Replace the fixed feature ranking with this gated sequence:

1. Finish the current atomic W1 implementation, subject to a bounded green/measurement exit
   gate, because it targets an observed assembly/recovery seam. Measure the new compact
   confirmation route directly; implementation depth is not itself value evidence.
2. Judge W2 separately on preview correctness, zero effects, review utility, stale retry,
   and incorrect-commit rate. Do not let W1's economics carry W2.
3. Run elaborator D1 only when the next real task naturally satisfies the wall classifier.
   If no such task arrives, do not manufacture a favorable case; run an independent cheap
   screen from the unbundled dreamlist instead.
4. Keep splice in protocol redesign/research. It is not a product build until its
   authoritative HOLD is cleared.
5. Do not activate F4 until a real packet states the failure population, requested fact,
   incidence, and recovery-turn hypothesis.

### Strongest argument

The elaborator's measured numerator is missing. The retained work proves mechanism facts:
a supervised Spark child can elaborate small prompts, and warm execution is faster than
cold. It does not measure an embedded product route completing real edits faster or more
correctly.

The elaborator's own classifier does not establish bytes saved: it bounds the old body at
at least 1,024 bytes and the decision/old-body ratio at no more than 25%, but it does not
bound caller-alone replacement size. Even the optimistic proxy that treats the 768-byte
old-body-minus-decision difference as avoided emission yields only 2.706 seconds at the
retained correlational rate of 3.5237 ms per byte. The 2.288-second “warm Spark median” is
itself an optimistic same-thread mechanism proxy, while the product contract creates a
fresh ephemeral thread per intent. That leaves about 0.42 seconds before incremental
prompt/setup/validation overhead; ordinary compilation, transaction, and receipt are
shared costs and must not be charged wholly to one arm. The first warm bang was 5.250
seconds, and the five-call probe observed a 9.064-second maximum—not a p95 estimate. The
classifier can select candidates; only D1 can establish the full delta. The design itself
forbids a performance claim before complete verified wall is measured.

W1 is closer to a real bottleneck, but the overnight counterclaim must also be corrected:
**217 B to about 30 B is not a measurement.** The retained live-route measurement at
`9f2b1ba4` measured the installed prepared-request slice at 217 B versus 217 B for final
edit arguments, with no compression claim. It did measure the problem and proxy recovery
direction: 69.291 to 51.729 seconds median completed wall, 912 fewer output tokens, 20 to
eight recovery actions, and seven to four construction refusals; the document correctly
labels transfer to product as unguaranteed. W1's compact result remains projected until its
own cohort. A literal 30-byte total is impossible with a full SHA-256: the digest alone is
32 raw bytes or 43 base64url characters before JSON, hole names, and values; the selected
W1 contract actually uses 64 lowercase hex characters
(`docs/observations/2026-08-30-dream-list-fulfillment-designs.md:123-127` at `7c937a40`;
`docs/intent/prepared-request-actions/prepared-request-actions-design.md:216-230` at
`714cadab`).

W2 is a different value proposition. Its own packet calls it a safety/review feature and
allows that preview may add a turn. Its promotion gates are exact facts, zero effects, and
no increase in wrong commits or stale retries—not emission speed.

Splice has stronger measured benefit than the priority list admits, but weaker product
authority than it assumes. In the exact Sol/high cohort, it achieved 8/8 exact episodes and
cut mutation tokens from 650 to 444 (31.69%). It also landed only 1.69 percentage points
above its kill floor and regressed 13.47 points from the foundation screen. The Spark bonus
then produced a wrong-subject attempt. The authoritative receipt says `KILL / HOLD` and
“No product implementation is earned here.” That earns protocol redesign, not a
Sol-scoped product slot.

Finally, “dreamlist amendments” is not one low-priority unit. Its document marks every item
as ideation, ranks some cheap independent work highly, and declares dependencies among
others: W7 waits on telemetry, W3 on the elaborator, and W4 on W1, while W8 is an independent
no-model explicit-query idea. Bundle priority hides this structure.

F4 is not ready to be “next activation.” In the reviewed lineage it exists only as a
North-Star label plus one caller anecdote. No F4 design, specification, preregistration, or
branch was found. The production consumption classifier's 0-of-119 coverage concerns
owner-vocabulary consumability, not cardinality incidence; it blocks transferring that
complete-vocabulary result to F4 but does not estimate F4's population. F4 incidence is
unmeasured. If F4 instead names WRITE-REFUSAL-004, that ratified family had zero firings in
the retained window and was ordered by route breadth, not demonstrated demand.

### Evidence that would settle the residual dispute

- W1: a retained same-task exact byte/token comparison for full arguments versus
  `{confirm, fill}`, followed by a counterbalanced live cohort measuring first-call
  correctness, route adherence, recovery, expiry, complete verified wall, and every loss.
- W2: preview acceptance/rejection, wrong-commit, stale-retry, extra-turn, review-wall, and
  zero-effect evidence reported separately from W1.
- Elaborator: a natural D1 task at each admitted size/decision-ratio band, including first
  bang and the full observed distribution, compared against caller-alone complete verified
  wall and semantic correctness. Charge child failures, fallback, quota, and review
  rejection without double-charging shared transaction work.
- Splice: one canonical subject source of truth and an adversarial multi-model cohort with
  zero wrong-subject attempts.
- F4: first retain the count-mismatch episode population and the exact missing fact; then
  run a same-task recovery-turn A/B before activation.

## R4 — the North Star

### Verdict: OVERTURN as the program target

**Better ruling.** Keep `2026-08-30-the-dream-session-north-star.md` as a worked happy-path
storyboard and contract example for a named-owner `.clj` task. The program target must be a
retained outcome scorecard over real tasks and hostile lifecycle cases. Transcript-line
conformance may test an interface; it may never authorize promotion.

### Strongest argument

The document defines completion as verbatim script production: “a lane is done when its
element's transcript line works verbatim,” and the dream is the table “emptying downward.”
That is a Goodhart target. A team can make every field appear and every status turn LIVE
while losing complete task time, correctness, availability, review quality, or broad
coverage.

Worse, the target transcript cannot work verbatim under the ratified contracts. Turn 3
combines `confirm`, `fill`, and `preview` with `elaborate` and `apply_to`. W1 accepts exactly
`confirm`, `fill`, and optional literal `preview=true` and rejects unknown or mixed fields;
the elaborator is a separate ordinary-edit branch, and the prepared descriptor does not
gain an `elaborate` field. Turn 3 also supplies `fill:{to:null}`, while W1 requires every
exact retained hole path once with a nonblank string. Turn 4 then sends `fill:{}`, although
preview is never commit authority and commit must repeat the exact filled confirmation.
The North Star's primary gate is therefore a schema-invalid script
(`docs/intent/prepared-request-actions/prepared-request-actions-design.md:214-253,328-331`
at `714cadab`; `docs/intent/embedded-elaborator/embedded-elaborator-design.md:457-470` at
`eaba46b2`).

The document already exhibits the metric failures that this target invites:

- It announces six caller emissions totaling about 200 tokens, later totals the task at
  about 105 tokens, and assigns no count to `make improvement-report`.
- It calls a server result “input ≈ free” and a preview “at input price,” even though the
  W2 design explicitly says response bytes are caller-input payload and not deleted
  emission (`docs/intent/prepared-request-actions/prepared-request-actions-design.md:355-367`
  at `714cadab`). The favorable retained evidence matters: one route measured input at
  1,284 times output throughput and priced the added payload cheaply. Cheap is still not
  free or universal; context, review, and end-to-end wall remain in the denominator.
- It claims the dream reaches a measured 181-byte judgment floor by comparing that byte
  quantity with about 105 **tokens**. The units and scopes do not match.
- It combines a 90 ms local child-process initialization with roughly 1,000-token/s model
  drafting. The actual five-call probe turn latency was 1.240/2.012/9.064 seconds
  min/median/max. Process spawn and model turn rate are distinct clocks; the storyboard's
  parenthetical is not measured end-to-end elaboration latency.
- Turn 4 promises `verification_complete=true` and “full verification in-transaction” for
  the W1 `edit_clojure` confirmation path. The ratified W1/W2 design permits the ordinary
  transaction and read-back but says confirmation grants no verifier authority; its first
  preview forecast must say `will_run=false` because `edit_clojure` does not authorize
  transaction verification. The storyboard must distinguish internal transaction/read-back
  evidence from an exact verifier rather than call both “full verification”
  (`docs/intent/prepared-request-actions/prepared-request-actions-design.md:235-247,304-326`
  at `714cadab`).

The storyboard also assumes away the hard cases. “The turn that never happens: repair” is
not an error-path design. One invented single-caller success flow contains no TTL expiry,
eviction, restart, session loss, replay, same-session contention, cross-session isolation,
cross-workspace interleaving, cancellation, Spark timeout/crash/quota/malformed output,
semantically valid but wrong elaboration, verifier failure, rollback, or degraded fallback.

Nor is it a general software-development North Star. The repository vision explicitly
limits its evidence away from arbitrary text, JavaScript, prose, and cold startup. The
storyboard should say “named-owner Clojure happy path,” include `.cljs` and
`.cljc`/reader-conditional controls, and compare native routes where Surgeon is not
applicable. Non-Clojure work is not a missing Surgeon feature; pretending the transcript
generalizes to it would be the error.

### Evidence that would settle the residual dispute

Preregister a counterbalanced installed-baseline cohort over retained real tasks. Report:

- exact and verified completion, wrong subject/effect, rollback, and fallback completion;
- caller output plus all MCP input/context, server wall, result-to-next-action wall, and
  p50/p95 complete verified wall;
- recovery turns, review rejection, and incorrect-commit rate;
- stale, expired, evicted, consumed, restart, reconnect, and concurrent confirmation paths;
- Spark timeout, crash, quota, malformed, semantically wrong-but-valid, and fallback paths;
- verifier and receipt failures; and
- `.clj`, `.cljs`, and `.cljc` cases, with native non-Clojure controls used only to prove
  honest routing boundaries.

The transcript remains useful if it passes as one scenario inside that matrix. It must not
be the matrix.

## R5 — scope readings of Gene's words

### Verdict: AMEND

**Better ruling.** Record a durable ratification tuple for every terse approval:

```text
{verbatim answer, exact antecedent/message digest,
 artifact or candidate SHA if one existed, authorized phase and scope}
```

No enthusiasm waives a failed evidence or safety gate. But no phrase has an intrinsic
phase either: the antecedent decides whether “go” means design, build, measurement, or
install.

### Strongest argument

The non-waiver principle is sound; the blanket interpretation of `Go on all!!!` as only
design ratification is under-supported. The embedded-elaborator record supplies the needed
antecedent and explicitly limits that phrase to an evidence-gated intent chain with a
separate Gene install decision. Telemetry similarly permits work through measurement while
reserving installation. A different release record says `Go on all.` authorized the
WRITE-REFUSAL-001 install, but it does not preserve the antecedent
(`docs/observations/2026-08-30-write-refusal-001-installed-release.md:9-15` at `4ec9394c`).
Under the standard above, that record illustrates ambiguity rather than proving that the
phrase intrinsically means install. The words alone do not encode the phase; the retained
antecedent must.

Applying `1 go` only to item 1 is a provisional grammatical default, not a proved scope
reading. The current captain's log merely aggregates `1 go, 2 go, 3 go` without retaining
the numbered antecedent beside the summary. That record cannot, by itself, prove which
artifact or phase item 1 named. By contrast, the W1/W2 packet retains Gene's full
conditional—“If build 1 and 2 are go -- get adversarial review and build and go”—and still
separates red, implementation, verification, measurement, and install.

The splice re-scope is the over-reach. The exact `gpt-5.6-sol`, high cohort survived its
formal screen, but the authoritative receipt's overall verdict is product `KILL / HOLD`.
It requires one model-independent canonical subject source and a multi-model cohort with
zero wrong-subject attempts; it explicitly says no product implementation was earned.
`Sol-class` is undefined, extrapolates from one exact model/config and eight episodes, and
turns model identity into the safety boundary that the receipt rejects.

The corrected scope is: retain the Sol result as mechanism evidence, permit protocol
redesign and another screen, and keep the product on HOLD across callers.

### Evidence that would settle the residual dispute

- Preserve the exact dispatch antecedent or message digest beside every future terse
  approval; summaries of the answer alone are insufficient.
- Name the candidate commit when one exists and the authorized phase/scope in every install
  card and ratification status line. An approval that precedes an artifact may authorize
  only the named lineage and phase; it cannot confer post-hoc install authority on an
  unseen candidate.
- For splice, redesign to a single canonical subject source of truth and require an
  adversarial multi-model, multi-fixture cohort with zero wrong-subject attempts plus
  public-route correctness and complete-wall evidence.

## Synthesis: replacement flight rule

The better operating rule is evidence-driven concurrency with authority at the edge:

```text
parallel feature work and frozen candidate-local evidence
       |
       +--> early cross-feature integration witnesses
       |
       +--> failed candidates leave the queue; they do not convoy it
       v
dependency-aware merge queue
       v
one exact candidate on current published lineage
       v
combined gates + candidate-specific measurement + rollback
       v
for this train, explicit Gene install/reload decision or retained delegation
       v
installed outcome measurement over real and hostile cases
       |
       +-- FAIL --> open circuit + rollback + candidate returns to HOLD
       |
       +-- PASS --> retain evidence; do not generalize beyond its scope
```

This preserves what was right in the Conn's instincts—evidence gates, immutable candidate
identity, and serial final publication—without paying for blanket serialization or granting
authority to labels and storyboards.

The immediate consequences are concrete:

1. Remove `17849c71` from queue head and repair its independent NO-GO.
2. Let W1 finish candidate-local tests; independent verification begins only on an immutable
   commit/tree. Run the nonpublished telemetry/W1 combined witness before the second feature
   lands and before telemetry may claim W1 emitted/consumed/committed coverage.
3. Do not install any candidate in this train during the unattended window without Gene's
   exact approval or a retained delegation that actually covers it.
4. Measure W1 and W2 separately; admit elaborator D1 only on naturally eligible work.
5. Restore splice to product HOLD and require a real F4 packet before activation.
6. Relabel the dream-session document as a storyboard, and replace transcript-line gating
   with the outcome matrix above.

## Bound record

- North Star under review: `docs/captains-log-2026-08-30-install-night@7fe1c38e`.
- Telemetry candidate: `docs/substantiation-telemetry-ratification-20260830@17849c71`.
- Telemetry independent NO-GO:
  `audit/substantiation-telemetry-independent-20260830@316a564d`.
- W1/W2 packet and red: `714cadab`, `feature/prepared-confirm-preview-20260830@fe27c6a8`.
  Review-time dirty-worktree observations are integration context, not bound candidate
  evidence.
- Installed prepared-request measurement: `feature/prepared-request-first-slice-20260830@9f2b1ba4`.
- Embedded elaborator packet/red: `eaba46b2`,
  `feature/embedded-elaborator-20260830@2145b753`.
- Embedded mechanism receipts: `experiment/embedded-spark-probe-20260830@44a5bac7`,
  `experiment/warm-executor-screen-20260830@9b6c9708`.
- Isolation result: `experiment/spark-isolation-screen-20260830@3c2cc192`.
- Splice adversarial receipt: `experiment/splice-adversarial-replication-20260830@b0432c25`.
- Dreamlist designs: `docs/dreamlist-designs-20260830@7c937a40`.
- Production consumption gap: `experiment/consumption-gap-20260830@1648d5db`.
- WRITE-REFUSAL-001 installed record: `release/write-refusal-001-published-20260830@4ec9394c`.
