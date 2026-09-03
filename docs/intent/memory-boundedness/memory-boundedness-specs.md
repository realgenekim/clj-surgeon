---
parent: memory-boundedness-design
prefix: MCP-OP-MEM
status: "registered 2026-09-03; both ids are active gaps — the RED baseline is docs/observations/2026-09-03-memory-battery-baseline.md"
---

# Memory Boundedness Specifications

These IDs are stable and must not be reused if a requirement is deleted. Ids
002-010 and 012-014 are reserved; 006, 007 and 012-014 belong to the
streaming-kernel builder and are not registered here.

Both requirements below are **active gaps**: no operation on this branch
satisfies either one. Their witnesses are the battery and its millisecond
verdict test, so removing the battery breaks the contract audit rather than
turning the promise green by deletion.

In the requirements below:

- **tree-scale operation** means an operation whose file set is determined by
  walking a repository rather than by an explicit caller-supplied file list. On
  this branch that is `:ls-tree`, `mcp-workspace-sources/read-all` (and the
  identical inline walk in `extract/plan`), and `rename/plan`.
- **work budget** means the configured heap work budget for one operation,
  separate from the process `-Xmx`.
- **unbounded reference output** means the operation's result, hashed, from a
  run at a heap large enough that no admission limit or spill path can engage.
- **UNMEASURED** is a distinct third outcome from pass and fail: a line whose
  inputs were not observed. It is never reported as a pass.

## Requirements

- [ ] **MCP-OP-MEM-001**: While a tree-scale operation runs over a repository of
  N files, when it returns its result, clj-surgeon shall carry in that result a
  memory-and-work block whose own size does not grow with N, reporting the
  configured maximum heap, the used-heap start, continuously sampled
  process-wide peak, and end, the attributable reserved peak, and the work
  actually done — walk entries, files discovered, read, parsed and written,
  source bytes, largest file bytes, projection records, workers, spill bytes,
  journal bytes, and cache work — labelling the sampled peak as process-wide
  rather than attributable whenever another operation ran concurrently.

- [ ] **MCP-OP-MEM-011**: While the memory battery runs every tree-scale
  operation at the configured maximum N under the configured work budget in one
  bounded JVM, clj-surgeon shall for every operation reproduce the unbounded
  reference output exactly, complete without exhausting the heap, keep the
  sampled process-wide peak at or below the tighter of the used-heap start plus
  224 MiB and 80 percent of the configured maximum heap, keep the attributable
  reserved peak at or below 192 MiB, keep the 10,000-file peak within 32 MiB of
  the 1,000-file peak, keep 10,000-file after-GC retention within 8 MiB of
  1,000-file after-GC retention, and refuse before any mutation when the request
  is over budget; and the battery shall report any of those lines it did not
  observe as UNMEASURED rather than as a pass.

## Misreadings

MCP-OP-MEM-001:

- "Report the peak and call it this operation's peak." A sampled process-wide
  peak is not attributable to one operation. Under concurrency it must be
  labelled process-wide, and the attributable figure must come from the
  admission accountant.
- "Include the manifest of every observed file in the block so the receipt is
  complete." The block's own size is part of the promise; a per-file manifest
  makes the receipt grow with N.
- "Emit the block only when the operation succeeds." A refusal is exactly when
  the numbers are wanted.
- "Reserved peak equals the sampled peak when nothing else is running." They are
  different quantities measured by different instruments; equating them makes
  the 192 MiB line unfalsifiable.

MCP-OP-MEM-011:

- "The 10,000-file case timed out, so compare 100 and 1,000 instead." The
  cross-N lines are defined at 1,000 and 10,000. A missing 10,000 is UNMEASURED.
- "No operation reports a reserved peak yet, so that line passes." An
  unobserved line is not a satisfied line.
- "The bounded run produced a result, so output parity holds." Parity is against
  a cached unbounded reference hash; with no reference, parity is UNMEASURED.
- "Raise `-Xmx` until the battery is green." The budget is the requirement.
- "Retention after the result is released is flat, so retained heap is bounded."
  That measures leaks. What a receipt itself retains is measured with the result
  still referenced, and is bounded by the peak line.
- "Run the battery in `make test` so nobody forgets it." It is minutes-scale and
  needs a dedicated bounded JVM; wiring it into a fast gate gets it disabled.

## Boundaries

MCP-OP-MEM-001:

- Temporary disk, spill files, journal bytes, and I/O time MAY grow with N; only
  the block and the retained heap it accounts for may not.
- The guarantee is per operation at the configured budget. It is not a claim
  about the sum of concurrent operations.
- A single pathological file may exceed the per-file budget; that is a refusal
  with a named code, not a silently larger block.
- On a JVM whose management interface is unavailable the block reports the
  fields it can obtain and names the rest as unavailable; it does not invent
  them.

MCP-OP-MEM-011:

- The battery measures this branch's operations as black boxes through their
  public entrances. It never reaches inside an operation, and it changes none.
- Two of Sol's pass lines are NOT implemented by this battery and are out of
  scope for its current arms: the 450 x 1.9 MiB aggregate-admission case, and
  injected conflict at staging, validation, every commit boundary, and rollback.
  They belong with the transactional kernel.
- Wall time and spill bytes MAY grow with N; the battery records them and does
  not gate on them, except that an operation exceeding `MEMBAT_OP_TIMEOUT_MS`
  causes its larger N cells to be recorded as skipped, which makes the cross-N
  lines UNMEASURED.
- The synthetic trees are representative small/medium Clojure sources. They do
  not include token-dense or deeply nested adversarial files; those are a
  separate arm.
- An arm measures one operation under one query shape. An operation whose result
  is small under the battery's query is not thereby bounded: `rename/plan` under
  a narrow prefix touches every file but retains almost nothing, and is measured
  by a second full-match arm as well. A new arm is incomplete until the query
  that makes its result grow with N is also measured.
- A shared build host perturbs wall time and, through GC scheduling, the sampled
  peak. The receipt records host load context; a single failing run near a line
  is re-run before it is called a regression.

## Rationale

Registered 2026-09-03 from Gene's instruction — "Make sure to write a test (not
a unit test) that confirms we don't OOM; don't want to slow the make run tests
too much, tho!" and "When fixing, add LID (see skill) to add new requirements
in." — against Sol's measured memory design of the same date. The RED baseline
that motivates both ids is
`docs/observations/2026-09-03-memory-battery-baseline.md`.
