# Sol: cross-attack of Astra's landing replacement

## Verdict

Astra correctly wants review, battery, and gates to overlap, but its sealed-`C`/external-proof design spends a large systems change to save seconds of merge setup. It also makes the busy records writer an adversary of every landing. Optimize the measured path first: remove records churn from `MCP/main`, overlap the gates with review under their real dependency graph, and shorten the one required review. Keep the current Git ledger until an external store is demonstrably at least as durable and auditable.

## 1. Proof outside the candidate

An exact-`C` receipt is stronger in one narrow respect: it proves the tested subject is the commit published. The committed ledger currently records tested SHA `P`, then creates receipt commit `R`; the landing proves `P` is an ancestor of `R`, not that every byte of `R` was tested.

But Astra's proposed external receipt is weaker as specified. “Immutable” is an adjective, not a mechanism. The committed ledger is fetched with the repository, Git-addressed, ordered, corruption-checked, reviewable, and records failures as well as passes. A loose per-run file plus a later records entry can be replaced, lost, omitted, or reordered unless a single-writer append-only ref and content hash make those states mechanically impossible.

Do not build an external receipt service this week. Keep `P -> R`, require `R` to add exactly one well-formed ledger entry, and bind the entry to `P`, the gate-manifest hash, runner/toolchain hashes, and an execution-input tree digest. Publish `R`. This admits only the proved inert evidence delta instead of pretending `R` itself ran.

If external proof is later needed, store the content-addressed bundle on a dedicated append-only Git evidence ref and put its hash in a queue event. It must record every attempt; a later RED for the same subject/manifest dominates an earlier PASS.

Moving to exact-`C` only also discards the current freshness service: age, ancestor relation, all-DAG distance, the closed archival-path classifier, unreadable-line refusal, newest-failure authority, and a receipt that travels with every clone. Exact identity makes distance irrelevant for that one landing, but it does not replace rolling/nightly freshness for descendants. Keep these as separate contracts.

## 2. The real queue/ref discipline

`STALE_BASE` on any advance is not a policy while records advance the same ref roughly 20 times/day; it is a near-certain retry generator.

Use three refs and one authority:

- `MCP/main`: code/integration publication only; exactly one queue coordinator may CAS-update it.
- `records/MCP-main`: high-frequency captain logs, reviews, and observations; never a candidate base and never a freshness-distance input.
- `evidence/battery`: optional append-only proof objects/pointers; not the candidate.

At queue head, fetch base `B`, prepare `P`, and hold a bounded publication lease from sealing through final CAS. Builders and reviewers continue elsewhere, but no other code landing advances `MCP/main`. On success publish `R` only if remote still equals `B`. On failure/timeout release the lease and retire that run. An emergency code advance explicitly cancels it; only that yields `STALE_BASE`.

Import records as a low-priority bounded batch between code candidates, or fold a chosen records snapshot into `P` before sealing. Never re-merge late docs into a proved commit: `docs/observations` already contains gate-consumed artifacts and scripts, so “docs-only” is not inert. The current `land` late-merge whitelist is too broad for exact-proof publication.

This changes the current rule that seat records go directly to `MCP/main`, so Gene must approve it. A short push lock is insufficient; the queue lease must cover preparation/proof, with a deadline so it cannot become an unbounded freeze.

## 3. Review barrier and ownership

Do not make “all reviewers” mean every reviewer someone launches. Freeze the minimum required set by risk class at admission: one independent reviewer by default; extra reviews are advisory unless named required before launch. Only required reviewers join the barrier.

Each required review gets a deadline. Timeout means HOLD, an atomic terminal TIMEOUT receipt, cancellation of that run's process group, closure of its generation token, and release of the publication lease. A late GO cannot revive a closed run. Requeue under a new run ID; reuse findings only after an impact check.

“Single-run ownership” should mean one renewable coordinator lease with heartbeat and takeover, not one process, one reviewer, or today's shared fence worktree. Duplicate `ship` calls attach to the same run. Jobs retain separate worktrees and supervisors. The current `fence-run` kills a prior reviewer, while `ship` watches only the Codex PID and can read the verdict before the detached END-receipt waiter finishes; neither is a sufficient barrier.

## 4. The SHIP line

Astra's bucket equation is not implementable from today's receipts. `ship` knows only its own start, an observed reviewer PID lifetime, mutable log mtime, `land-auto` wall, and total wall. It has no authoritative request/push event, queue intervals, retired attempts, idle/rework partition, monotonic cross-process timeline, or critical-path reconstruction. Printing those buckets would be a second unverifiable self-report.

This week print only observed facts: `SHIP status run candidate base landed ship_elapsed review_elapsed battery_elapsed gates_elapsed attempts events_hash request_to_landed=unknown`. Durations may overlap and must not be summed. Add `request_to_landed` only after upstream request/push events enter an append-only event journal and an independent reducer can reproduce the line. First fix `land`: its current command chain can print `LANDED` after a failed push or failed checkout sync.

## 5. Gates during the fence interval

Nothing forbids gates from running during review; their prerequisites forbid “start everything at once.” The concrete dependency is `make admit-transaction-recovery-battery -> test-battery/mcp-test consumer`. The 14:25 battery passed 743 tests but logged one skipped precondition because `receipt-chain` invokes `make test-battery` directly. `make test` creates the target receipt first. Run that prerequisite on the same candidate/worktree before its consumer and require skipped=0.

Also, `battery-fresh` cannot pass before durable battery evidence exists. Start independent oracle, alias, BB, hygiene, and audit lanes during review; schedule recovery then its consumers; aggregate battery evidence; run freshness last. A missing, timed-out, or skipped node is RED. Do not skip `make test` merely because today's battery passed: the two targets have different coverage.

## Build this week, in this order

1. **Fable + battery owner B — split records/evidence refs and retain the narrow legacy freshness verifier.** Wall removed: avoids the observed 809 s battery refresh and near-certain exact-`C` rebuilds caused only by records churn. Witness: 20 concurrent records commits do not move `MCP/main`; an unknown path, mode/symlink/rename, gate-consumed doc, failed newest receipt, or real code advance still refuses.
2. **Flat-layer owner A + B — harden `ship`, prepare `P` once, and run the prerequisite-aware gate DAG beside the required review.** Wall removed: about 8 min of post-GO gates plus the reported handoff gaps; battery is hidden when shorter than review. Witness: every named gate runs once on one execution digest, recovery skips=0, hung review releases the lease as HOLD, duplicate ship coalesces, and failed/ambiguous push never prints LANDED.
3. **Sol owns the review contract; Fable owns fixtures — one bounded delta review class.** Wall removed: target up to 10 min from the current approximately 20 min review, without batching delay. Witness: the same seeded defects and dependency-closure findings as the full review; timeout/expanded scope becomes HOLD/NEEDS_EXPANDED_REVIEW, never GO.

## Refuse to build now

Refuse the bespoke external receipt store, exact-`C` stale-on-any-record advance, an all-launched-reviewer barrier, eleven-way battery default, arbitrary docs-only late merge, and Astra's full bucket-accounting engine. Refuse to skip the landing gate or claim a wall reduction until the event-derived request-to-landed clock and repeated accepted landings show it.
