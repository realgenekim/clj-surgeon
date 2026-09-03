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

**MCP-OP-MEM-001 is deliberately NOT registered in this leaf.** One requirement
gets one authority, and MEM-001's is
[`../memory-boundedness/memory-boundedness-specs.md`](../memory-boundedness/memory-boundedness-specs.md),
owned by the memory-battery build. This leaf previously carried a second,
checked copy of it; two spec files stating the same id, one `[x]` and one `[ ]`,
is a contract that cannot be audited - a maintainer reading either one would be
reading a statement the other contradicts.

The kernel does carry two of MEM-001's clauses - the streaming reader's
request-lowerable receipt ceiling under a server cap, and the accountant's
attributable reserved peak that the battery's `:journal-scope-stream` arm
measures - so kernel sources carry `@spec MCP-OP-MEM-001` and
`@spec MCP-OP-MEM-011` markers. The two rows below keep those markers
traceable WITHOUT restating the requirements: a deferred row states nothing and
demands no witness, it only says where the statement lives.

- [D] **MCP-OP-MEM-001**: Deferred to `../memory-boundedness/memory-boundedness-specs.md`, which owns the sole statement. Witnessed from this build by `scope_stream.clj`'s receipt ceilings and reservation block.

- [D] **MCP-OP-MEM-011**: Deferred to `../memory-boundedness/memory-boundedness-specs.md`, which owns the sole statement. Witnessed from this build by the battery arm that reads this kernel's reservation block.

- [x] **MCP-OP-MEM-006**: When a mutation is staged, clj-surgeon shall pin each write-set pre-image and future file durably by digest AND by NOFOLLOW type and file identity before live mutation, shall refuse a parent-traversal segment, a relative path, or an absolute path not named under the workspace root on the RAW path before canonicalisation, and shall not retain repository-wide original or future source maps; and when the transaction ends it shall RETAIN that pre-image journal after a commit until an explicit `forget!` or a quota-driven `evict!` that no receipt reference blocks, and shall never delete the journal of a restoration that did not verify; and when a retained journal's lease cannot be read, clj-surgeon shall treat its receipt reference count as UNKNOWN rather than zero - reporting the journal as not evictable and naming `:lease :unreadable` in the refusal - shall never let a quota-driven `evict!` reclaim it, and shall discard it only through an explicit `forget!` by a caller that presents the commit receipt; and when a retained journal is undone, clj-surgeon shall hold the workspace publish lock and shall recheck every begun path's digest and NOFOLLOW identity against the post-commit state the journal recorded, refusing the whole undo with zero writes and naming the path whenever any of them differs.

- [x] **MCP-OP-MEM-007**: Before the first write, clj-surgeon shall hold the transaction lock and revalidate every file that influenced the plan, the NOFOLLOW type and file identity of every pinned path, and the SEALED SCOPE-MEMBERSHIP DIGEST it was planned against - never the member count, which cannot distinguish one set from another of the same size; and before each path's own replacement it shall copy that path's staged bytes into the path's own directory FIRST and then, while holding the workspace publish lock, recheck the path's pre-image digest AND its file identity and rename, so that no byte copying happens between the recheck and the rename. Because an atomic rename is not a compare-and-swap, a writer that does not take the publish lock and lands inside that residual recheck-to-rename window can still be overwritten; clj-surgeon shall bound that window to a digest recheck, an identity recheck, one journal fsync and one rename, shall report its measured width in the receipt, and shall keep the pinned pre-image journal as its recovery; and every write the kernel itself performs - commit, undo and crash recovery alike - shall be taken while holding that same publish lock.

- [x] **MCP-OP-MEM-012**: While a transaction is open, when a read-set entry is recorded, clj-surgeon shall write it to the sorted on-disk manifest and shall retain no per-path record and no source text of it in the transaction value; and when the read set is sealed it shall fold scope membership into one digest and a count rather than retaining the walked path list.

- [x] **MCP-OP-MEM-013**: While an unfinished transaction journal exists, when recovery runs, clj-surgeon shall restore the pinned pre-image bytes of every path the journal recorded as begun, shall verify each restored digest against the digest pinned before the first write, and shall discard the journal only when every restoration verified - recording `:restore-failed` and retaining the whole journal otherwise; and while a project lock exists, clj-surgeon shall record its holder as the checkable triple of pid, process start ticks and boot id, shall break that lock exactly once and name the break in a typed receipt line and a durable journal line when that triple proves the holder is not a live process, shall never break a lock whose holder is live or whose holder cannot be read, and when recovery runs shall release a lock that has no live holder even when recovery found no transaction to recover.

- [x] **MCP-OP-MEM-014**: When a transaction commits or refuses, clj-surgeon shall state its isolation as optimistic serializability with conflict detection and exact rollback, shall name the residual recheck-to-rename window it does not close and the operations inside it, and shall not claim snapshot isolation against a writer that ignores the lock.

- [x] **MCP-OP-MEM-020**: When a scope is walked, clj-surgeon shall admit work exactly through its walk-entry, depth, per-file byte and aggregate byte ceilings, shall refuse the next unit before reading it with a narrowing `next_call`, shall charge the retained discovered-path list to the same work budget as the per-file parser reservation and refuse before the first read when that list alone does not fit, and shall retain no file's source after the planner callback returns.


## Misreadings each requirement forbids

| id | a plausible wrong reading a maintainer might implement |
|---|---|
| MEM-006 | "Pinning is a copy-on-write optimisation, so it can be skipped when the file is small." Pinning is the rollback guarantee; an unpinned staged path is a refusal, not a fast path. |
| MEM-006 | "Keeping the original source string in memory is a cheaper pre-image than a file." That is the defect this kernel exists to remove. |
| MEM-006 | "The journal quota is a policy number." It is derived: the journal holds a pre-image AND a future image of every staged byte, so a default below twice the read path's aggregate ceiling refuses scopes the reader admits. |
| MEM-006 | "Canonicalising the path is the confinement check." `getCanonicalPath` DELETES `..` before any rule can see it, so `<root>/src/../src/in.clj` passes a canonical-only check. The lexical refusal must come first. |
| MEM-007 | "Revalidate the files we are about to write." A caller or alias that shaped the plan can live in a file the transaction never touches. |
| MEM-007 | "Membership is fixed once discovery has run." A file that appears after planning can introduce a new caller, so an addition is a conflict. |
| MEM-006 | "A matching content digest means it is the same file." A regular file swapped for a symbolic link to identical bytes has the same digest. Writing through it replaces something the transaction never read. |
| MEM-007 | "Equal member counts mean the same scope." `[a b]` and `[c d]` have the same count. Membership is compared as a sealed digest over path, type and file identity. |
| MEM-007 | "Recheck-then-rename is a compare-and-swap." It is two syscalls. The kernel narrows the gap between them to two stats, one fsync and one rename and reports its width; it does not close it, and a receipt that implies otherwise is the defect. |
| MEM-012 | "A map of path to hash is only thirty-two bytes per file." Paths plus Clojure object overhead make a repository-sized map a repository-sized heap. |
| MEM-012 | "Retention only matters for source text." A path-keyed collection that grows with the repository is the same defect at a smaller constant. |
| MEM-013 | "Recovery can re-derive what to restore from the manifest." The manifest is the read set; only the journal knows which paths were begun. |
| MEM-013 | "A restored file is restored." Restoration is verified against the pinned digest or it is not restoration. |
| MEM-006 | "Undo is recovery, so it needs no recheck." Recovery repairs a tree a crash left part-written; undo reverses a commit the tree has been LIVE with since. Republishing H0 over whatever landed after the commit is a silent clobber, and it is the kernel's own writer ignoring the kernel's own lock. |
| MEM-013 | "A finished transaction's journal is garbage." A committed receipt is undoable only while its pre-images exist, and a FAILED restoration's journal is the only material that can repair the tree. |
| MEM-006 | "A missing lease means nothing references the journal." A missing lease means the refcount is UNKNOWN, and an unknown refcount is not zero. `(:receipt-refs lease 0)` is the shape of a refcount that fails open: one deleted file and a quota sweep destroys the pre-images a live receipt still needs. |
| MEM-013 | "A lock file on disk means somebody holds the lock." A lock is a claim by a PROCESS; a process dies without unlinking it. A recorded pid nobody reads back is decoration, and releasing only when recovery found work leaves the one stranded case - a lock with no journal beside it - deadlocked for ever. |
| MEM-013 | "Checking the pid is enough." A pid is unique only within one boot and is reused inside one. Only pid plus start ticks plus boot id names one process, and an unreadable holder is an unknown, not a corpse: `begin!` refuses it and the explicit `recover!` remedy is what clears it. |
| MEM-014 | "Hashing every file at validation time gives us a snapshot." It does not. A writer that ignores the lock can land between validation and rename. |
| MEM-014 | "Detected therefore prevented." The racing write is detected at read-back and rolled over; the receipt must not imply it was excluded. |
| MEM-020 | "The aggregate byte ceiling can be checked from the directory entries." A file that grows during the walk must be stopped against the remaining budget, from bytes actually read. |
| MEM-020 | "Walk entries means matching files." Then an include glob conceals an unbounded walk. |
| MEM-020 | "The reservation is the largest file's parse." The walk also holds every discovered path for the whole stream; with many small files that list is the larger term and was invisible. |

## Boundaries

| id | edge | concurrent | failure |
|---|---|---|---|
| MEM-006 | pinned and staged bytes exactly equal the journal quota; a path naming a file inside the root through a `..` segment | a second transaction cannot open while the lock is held | the last injected write fails and every path returns to `H0` |
| MEM-007 | exactly the maximum read-set count; a scope whose members are swapped at an unchanged count | a read-only file drifts after sealing; a writer lands after the staged copy and before the pre-image recheck; a pinned regular file becomes a symbolic link to identical bytes | a write-set file drifts between revalidation and its own rename; a writer lands inside the residual recheck-to-rename window and is overwritten, which the receipt reports |
| MEM-012 | twenty thousand recorded entries | — | an unsorted entry is refused rather than written out of order |
| MEM-013 | killed between pin and rename | killed between rename N and N+1 | a pre-image object is missing, which is reported, never assumed, and its journal is retained rather than deleted |
| MEM-014 | — | an external writer lands after the rename | read-back mismatch rolls the transaction back |
| MEM-020 | exactly at each ceiling; a work budget exactly equal to the path list plus one file's parse | — | one unit past each ceiling refuses before the read; a path list one byte over the budget refuses before any read |

## Falsifiers

| Requirement | Falsifying observation |
|---|---|
| MCP-OP-MEM-006 | A staged path is written without a durable pinned pre-image, or a path containing a `..` segment is pinned or staged, or a path whose NOFOLLOW type or file identity changed after pinning is written, or a committed receipt cannot be undone because its journal was deleted, or a transaction retains a repository-wide original or future source map. |
| MCP-OP-MEM-007 | A transaction commits after a file that shaped its plan changed, or after the scope gained, lost or SWAPPED a member, or without holding the lock; or bytes are copied into the target directory INSIDE the recheck-to-rename window; or a receipt omits that window. |
| MCP-OP-MEM-012 | Recording N read-set entries makes the transaction value's per-path record count or retained string bytes grow with N. |
| MCP-OP-MEM-013 | A crash leaves a path holding neither its pre-image nor its verified result, or recovery reports success without verifying a digest, or a journal whose restoration failed is deleted. |
| MCP-OP-MEM-014 | A receipt claims snapshot isolation, omits the isolation statement, omits the residual commit window, or a racing external write goes undetected. |
| MCP-OP-MEM-020 | A walk reads a file past a ceiling, follows a symbolic link, retains a source after its planner callback returned, or reports a reserved peak that omits heap it holds for the whole stream. |
