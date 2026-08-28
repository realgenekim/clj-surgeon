# Captain's Log: Good Reads Survive a Bad Selector

**Date:** 2026-08-27  
**Branch:** `experiment/submission-row-counterfactual`  
**Issue:** `clj-surgeon-wjz`

## The hill

The 24-hour event-clock census found 46 maximal same-route Surgeon read chains,
containing 121 read actions and 1,084.371 seconds of raw inter-action boundary
time. That total is an opportunity upper bound, not a savings claim: many
boundaries contain useful model reasoning. Two retained cases exposed removable
work, however:

- a 47.1-second four-read route repeated a completed read after a selector miss;
- a 74.5-second two-read route split one already-known benchmark-harness read.

The product defect was exact. `inspect_clojure` captured every requested file
once and completed the prefix of an ordered batch, but a later selector-local
failure discarded the prefix. The caller then paid another model/tool boundary
and reread source that Surgeon had already proved.

We reproduced the defect while implementing its repair. One batched inspection
successfully resolved two source requests, then a mistyped test owner failed.
The public result reported the failed selector but erased the two successful
results. We had to issue another read. That dogfood failure became the primary
acceptance case.

## The change

A selector-local refusal can now carry an inline continuation:

```text
first batch
  request A ----------------------> complete result A
  request B ----------------------> selector refusal
  request C ----------------------> not evaluated

refusal
  ok=false, read_complete=false
  completed_request_ids=[A]
  pending_request_ids=[B,C]
  completed_results=[result A]
  snapshot_guards={all original files -> SHA-256}
  selector_authority=false
  write_authority=false
  next_call absent
```

The caller corrects B and retries only B/C, copying every snapshot guard. The
server captures the union of requested files and guard-only completed sibling
files once. It compares every hash before evaluating B. A stale guard refuses
the complete retry without results, continuation, source, or a next call.

```text
before
  read A+B+C -> B misses -> A erased
  think
  read A+B+C again

after
  read A+B+C -> B misses -> A preserved + all guards
  think
  guarded read B+C -> A rechecked, not retransmitted
```

There is no retained server state, continuation ID, fuzzy selection authority,
or automatic retry. The model still chooses the corrected exact owner. The
kernel proves that the retry observes the same files.

## The independent falsifier mattered

SURGEON2 found two P0 defects before the first focused run:

1. The optional `snapshot_guards` field had accidentally become required,
   which would have broken every ordinary inspect request.
2. A chained A/B/C -> B/C -> C retry could have dropped guard-only A from the
   second continuation.

Both became permanent tests. The final chain preserves A/B/C guards after the
second selector miss; changing A then defeats the C-only retry before C is
evaluated. The review also forced actual-string hash validation, snapshot-stage
path/read failures, a closed continuation schema, and full public-envelope
budgeting.

## Local feel and evidence

A warm local dogfood replay returned:

| Phase | Result | Server wall |
|---|---|---:|
| A succeeds, B selector misses | A preserved; A+B guarded | 12.442 ms |
| Corrected B-only retry | success; A and B both rechecked | 1.052 ms |

The important outcome is route geometry, not these millisecond kernel numbers:
the caller no longer rereads A or reconstructs the complete batch.

Focused verification is green:

- inspect contract, adapter, and server schema: 46 tests / 472 assertions in
  both the warm nREPL loop and a fresh 64 MiB initial / 512 MiB maximum JVM;
- malformed, non-string, missing, unreadable, outside-root, aliased, and stale
  guards fail closed;
- first-request selector, cardinality, parse, and output-budget failures expose
  no continuation;
- the concise refusal explains reuse but contains no source body.

The cold complete MCP run reached 266 tests / 2,218 assertions. The new inspect
tests passed. The repository gate still reported three pre-existing failures:
two cold-verifier assertions observe the installed clj-kondo admission wrapper
as `delegated` instead of the older timeout outcome, and the intent audit still
reports missing witnesses for `MCP-OP-ANALYZER-007/008`. These are outside the
read-continuation diff and remain explicit release blockers; they were not
papered over.

## Decision and next experiment

Keep the stateless continuation and guard mechanism. Do not yet add executable
selector correction, server-retained plans, or a general read graph.

The next experiment is a small clean-context counterfactual on retained routes:

1. replay the exact duplicate-read selector case before and after;
2. require the POST caller to retry only the pending suffix with every guard;
3. score semantic correctness, complete wall, tool calls, source bytes, native
   fallback, and stale-source refusal;
4. reject the interface if callers ignore the continuation or if total wall does
   not improve despite fewer reads.

This is Kent Beck's ratchet in product form: when one failed selector made the
next change expensive, preserve the already-proved work and make the retry
cheap without weakening the source fence.
