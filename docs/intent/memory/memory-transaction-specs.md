---
parent: memory-transaction-design
prefix: MCP-OP-MEM
status: "kernel implemented on bridge/txn-journal; adopted by no verb yet"
---

# Memory Transaction Specifications

These IDs are stable and must not be reused if a requirement is deleted.

The MCP-OP-MEM range is shared by two builds and is allocated from Sol's
ordered plan in `docs/observations/2026-09-03-memory-design-sol-answer-2.md`,
so the two builders cannot collide:

| ids | owner | subject |
|---|---|---|
| MEM-001, MEM-011 | the memory-battery build | the per-operation heap meter, the serialized receipt/record ceiling, and the battery release gate |
| MEM-002 – MEM-005 | reserved | unified discovery admission, streaming `ls-tree`, the streaming workspace-sources API, parser/lexical admission |
| MEM-006, MEM-007 | this build | the disk journal and read-set revalidation |
| MEM-008 – MEM-010 | reserved | measured worker reservation, the projection cache, streaming fold-diff |
| MEM-015 | the read-path build | the streaming outline/`ls-tree` read path (observed in use on `clj-surgeon-readpath`, 2026-09-03) |
| MEM-012 – MEM-014, MEM-020 | this build | retention, crash recovery, the contract statement, the bounded scope walk |

Reserved ids are not gaps to fill opportunistically. They name work Sol
sequenced ahead of adoption, and taking one for something else would make the
plan unreadable.

- [x] **MCP-OP-MEM-001**: When a tree-scale operation terminates, clj-surgeon shall report its work and its attributable reserved peak in a bounded receipt, and shall refuse rather than truncate a receipt that would exceed its configured record or byte ceiling.

  This row belongs to the memory-battery build, which owns MEM-001. It is
  recorded here because the kernel carries two of its clauses - the streaming
  reader's request-lowerable receipt ceiling with a server cap, and the
  accountant's attributable reserved peak - and the traceability contract needs
  the id registered wherever the witnesses live. On merge, fold this row into
  the battery's own and keep one.

- [x] **MCP-OP-MEM-006**: When a mutation is staged, clj-surgeon shall pin each write-set pre-image and future file durably by digest before live mutation and shall not retain repository-wide original or future source maps.

- [x] **MCP-OP-MEM-007**: Before the first write, clj-surgeon shall hold the transaction lock and revalidate every file and scope-membership fact that influenced the plan.

- [x] **MCP-OP-MEM-012**: While a transaction is open, when a read-set entry is recorded, clj-surgeon shall write it to the sorted on-disk manifest and shall retain no per-path record and no source text of it in the transaction value.

- [x] **MCP-OP-MEM-013**: While an unfinished transaction journal exists, when recovery runs, clj-surgeon shall restore the pinned pre-image bytes of every path the journal recorded as begun and shall verify each restored digest against the digest pinned before the first write.

- [x] **MCP-OP-MEM-014**: When a transaction commits or refuses, clj-surgeon shall state its isolation as optimistic serializability with conflict detection and exact rollback and shall not claim snapshot isolation against a writer that ignores the lock.

- [x] **MCP-OP-MEM-020**: When a scope is walked, clj-surgeon shall admit work exactly through its walk-entry, depth, per-file byte and aggregate byte ceilings, shall refuse the next unit before reading it with a narrowing `next_call`, and shall retain no file's source after the planner callback returns.


## Misreadings each requirement forbids

| id | a plausible wrong reading a maintainer might implement |
|---|---|
| MEM-006 | "Pinning is a copy-on-write optimisation, so it can be skipped when the file is small." Pinning is the rollback guarantee; an unpinned staged path is a refusal, not a fast path. |
| MEM-006 | "Keeping the original source string in memory is a cheaper pre-image than a file." That is the defect this kernel exists to remove. |
| MEM-007 | "Revalidate the files we are about to write." A caller or alias that shaped the plan can live in a file the transaction never touches. |
| MEM-007 | "Membership is fixed once discovery has run." A file that appears after planning can introduce a new caller, so an addition is a conflict. |
| MEM-012 | "A map of path to hash is only thirty-two bytes per file." Paths plus Clojure object overhead make a repository-sized map a repository-sized heap. |
| MEM-012 | "Retention only matters for source text." A path-keyed collection that grows with the repository is the same defect at a smaller constant. |
| MEM-013 | "Recovery can re-derive what to restore from the manifest." The manifest is the read set; only the journal knows which paths were begun. |
| MEM-013 | "A restored file is restored." Restoration is verified against the pinned digest or it is not restoration. |
| MEM-014 | "Hashing every file at validation time gives us a snapshot." It does not. A writer that ignores the lock can land between validation and rename. |
| MEM-014 | "Detected therefore prevented." The racing write is detected at read-back and rolled over; the receipt must not imply it was excluded. |
| MEM-001 | "A receipt that is too big can be truncated with a continuation." A read-only projection may paginate; a mutation receipt that hides work that was done may not. |
| MEM-020 | "The aggregate byte ceiling can be checked from the directory entries." A file that grows during the walk must be stopped against the remaining budget, from bytes actually read. |
| MEM-020 | "Walk entries means matching files." Then an include glob conceals an unbounded walk. |

## Boundaries

| id | edge | concurrent | failure |
|---|---|---|---|
| MEM-006 | pinned and staged bytes exactly equal the journal quota | a second transaction cannot open while the lock is held | the last injected write fails and every path returns to `H0` |
| MEM-007 | exactly the maximum read-set count | a read-only file drifts after sealing | a write-set file drifts between revalidation and its own rename |
| MEM-012 | twenty thousand recorded entries | — | an unsorted entry is refused rather than written out of order |
| MEM-013 | killed between pin and rename | killed between rename N and N+1 | a pre-image object is missing, which is reported, never assumed |
| MEM-014 | — | an external writer lands after the rename | read-back mismatch rolls the transaction back |
| MEM-020 | exactly at each ceiling | — | one unit past each ceiling refuses before the read |

## Falsifiers

| Requirement | Falsifying observation |
|---|---|
| MCP-OP-MEM-001 | A receipt is silently truncated, grows without a ceiling, or reports no attributable reserved peak. |
| MCP-OP-MEM-006 | A staged path is written without a durable pinned pre-image, or a transaction retains a repository-wide original or future source map. |
| MCP-OP-MEM-007 | A transaction commits after a file that shaped its plan changed, or after the scope gained or lost a member, or without holding the lock. |
| MCP-OP-MEM-012 | Recording N read-set entries makes the transaction value's per-path record count or retained string bytes grow with N. |
| MCP-OP-MEM-013 | A crash leaves a path holding neither its pre-image nor its verified result, or recovery reports success without verifying a digest. |
| MCP-OP-MEM-014 | A receipt claims snapshot isolation, omits the isolation statement, or a racing external write goes undetected. |
| MCP-OP-MEM-020 | A walk reads a file past a ceiling, follows a symbolic link, or retains a source after its planner callback returned. |
