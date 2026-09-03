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

- [x] **MCP-OP-MEM-007**: Before the first write, clj-surgeon shall hold the transaction lock and revalidate every file that influenced the plan, the NOFOLLOW type and file identity of every pinned path, and the SEALED SCOPE-MEMBERSHIP DIGEST it was planned against - never the member count, which cannot distinguish one set from another of the same size; and before each path's own replacement it shall copy that path's staged bytes into the path's own directory FIRST and compute that path's current digest BEFORE taking the workspace publish lock, and then, while holding that lock, recheck the path's NOFOLLOW type, file identity, size and both nanosecond timestamps against the stat taken beside that digest and rename, re-reading the digest inside the lock only when that stat moved and reporting in the receipt how many times it did, so that neither byte copying nor a file read happens between the recheck and the rename. Because an atomic rename is not a compare-and-swap, a writer that does not take the publish lock and lands inside that residual recheck-to-rename window can still be overwritten; clj-surgeon shall bound that window to one NOFOLLOW stat comparison, one journal fsync and one rename, none of which reads or copies the target - a bound whose size term is thereby reduced about four-fold rather than eliminated, the in-window fsync still tracking the writeback the pre-lock staging copy left - shall report its measured width in the receipt beside the measured size term rather than a flat-bound claim, and shall keep the pinned pre-image journal as its recovery; and every write the kernel itself performs - commit, undo and crash recovery alike - shall be taken while holding that same publish lock; and because cooperation with that lock is PER-WRITER, clj-surgeon shall offer ordinary atomic writes an opt-in that takes it and shall name, in `docs/txn-journal.md`, every source-mutating site that does not; and because an OS file lock is held per PROCESS and not per thread, clj-surgeon shall serialise the threads of one process on that lock BEFORE the OS lock is taken, so that a second thread waits rather than being thrown `OverlappingFileLockException`, shall decide re-entrancy by the identity of the THREAD that holds the lock and never by a conveyed dynamic binding, so that a future, agent send or `pmap` task spawned inside the lock takes the lock itself, and shall end the transaction - releasing the project lock and marking the journal - on EVERY exception path out of a commit, so that no failure can leave a workspace held by a live holder that neither `begin!` nor recovery is permitted to break.

- [x] **MCP-OP-MEM-012**: While a transaction is open, when a read-set entry is recorded, clj-surgeon shall write it to the sorted on-disk manifest and shall retain no per-path record and no source text of it in the transaction value; and when the read set is sealed it shall fold scope membership into one digest and a count rather than retaining the walked path list.

- [x] **MCP-OP-MEM-013**: While an unfinished transaction journal exists, when recovery runs, clj-surgeon shall restore the pinned pre-image bytes of every path the journal recorded as begun, shall verify each restored digest against the digest pinned before the first write, and shall discard the journal only when every restoration verified - recording `:restore-failed` and retaining the whole journal otherwise; and while a project lock exists, clj-surgeon shall record its holder as the checkable triple of pid, process start ticks and boot id, shall break that lock exactly once and name the break in a typed receipt line and a durable journal line when that triple proves the holder is not a live process, shall never break a lock whose holder is live or whose holder cannot be read, shall break it only by renaming the EXACT claim it read - verified, at the moment the rename lands, by that claim's content and its (device, inode) identity, and restored untouched with nothing broken when either has changed - shall keep the renamed claim on disk as the break's evidence, shall unlink a lock only while it still names the releasing transaction, shall stamp the claim with its format version and treat a claim that records a pid without a boot id as UNREADABLE rather than as a live holder - naming that format in a typed refusal whose remedy is an explicit recovery flag, and breaking such a claim only on a receipt of the holder's death, being that the recorded pid names no live process AND the claim is older than a published minimum age, and when recovery runs shall release a lock that has no live holder even when recovery found no transaction to recover; and when recovery removes publication temporaries from the tree it shall delete only those its own journal recorded, never a sibling temporary another transaction prepared.

- [x] **MCP-OP-MEM-014**: When a transaction commits or refuses, clj-surgeon shall state its isolation as optimistic serializability with conflict detection and exact rollback, shall name the residual recheck-to-rename window it does not close and the operations actually inside it, and shall not claim snapshot isolation against a writer that ignores the lock; and every statement it makes about an instrument shall be true of that instrument in general, not only of the case that motivated it - in particular clj-surgeon shall not claim that file identity distinguishes two files with identical bytes, because a (device, inode) pair carries no generation counter and a deleted file recreated with the same bytes can be handed the same inode.

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
| MEM-007 | "The publish lock excludes other writers." It excludes writers that ASK for it. A clause whose referent is empty - one call site in the whole repository - reads as a guarantee and is a description of nothing; the honest form names who cooperates and who does not. |
| MEM-007 | "No byte COPYING inside the window means the window is O(1)." A digest recheck is a full read of the target: measured, the window was 846 us at 1 KB and 3.0 ms at 2 MiB while the receipt truthfully said `:staging-copy-inside false`. The expensive read belongs outside the lock, with a stat on each side of it; inside, only the stat is compared. |
| MEM-007 | "A file lock excludes the second thread too." `FileChannel/lock` is a per-PROCESS view of the OS lock: a second thread in the same JVM is thrown `OverlappingFileLockException` rather than made to wait. An exception is not mutual exclusion - it escaped `commit!` before `finish!` could run, and a commit that never finishes is a project LOCK held by a LIVE pid, which is precisely the holder no recovery may break. Threads of one process must serialise in-process before the OS lock is taken. |
| MEM-007 | "The thread that is inside the lock is the one whose binding says so." Clojure CONVEYS dynamic bindings to `future`, `send`, `pmap` and every `bound-fn`, so a binding-based guard means 'some frame on my binding stack took it', not 'this thread holds it'. A future spawned inside the lock inherited the claim, outlived the owning thread's dynamic extent, and wrote with no OS lock at all against a lock another process was holding - a cooperating writer that silently stopped cooperating. Re-entrancy must be read off the thread that holds the monitor. |
| MEM-007 | "The commit path catches the failures that can happen." It caught the ones it anticipated. Any other throw between taking the publish lock and returning skipped `finish!` and stranded the workspace for the life of the process; the release belongs in a `finally`, not in the list of expected causes. |
| MEM-007 | "Moving the read out of the lock makes the window O(1) in the target's size." It makes it hold no read and no copy of the target, which is a different claim. Measured on ext4 2026-09-03, 2 MiB is still 1.9x 1 KB - a 570,537 ns residual size term - because the in-window fsync pays for the writeback the pre-lock staging copy left. MEM-014 requires the statement to be true of the instrument in general; the honest form is the measurement, and it belongs in the contract value, the docstring and this requirement alike. |
| MEM-007 | "Recheck-then-rename is a compare-and-swap." It is two syscalls. The kernel narrows the gap between them to one stat, one fsync and one rename and reports its width; it does not close it, and a receipt that implies otherwise is the defect. |
| MEM-012 | "A map of path to hash is only thirty-two bytes per file." Paths plus Clojure object overhead make a repository-sized map a repository-sized heap. |
| MEM-012 | "Retention only matters for source text." A path-keyed collection that grows with the repository is the same defect at a smaller constant. |
| MEM-013 | "Recovery can re-derive what to restore from the manifest." The manifest is the read set; only the journal knows which paths were begun. |
| MEM-013 | "A restored file is restored." Restoration is verified against the pinned digest or it is not restoration. |
| MEM-006 | "Undo is recovery, so it needs no recheck." Recovery repairs a tree a crash left part-written; undo reverses a commit the tree has been LIVE with since. Republishing H0 over whatever landed after the commit is a silent clobber, and it is the kernel's own writer ignoring the kernel's own lock. |
| MEM-013 | "A finished transaction's journal is garbage." A committed receipt is undoable only while its pre-images exist, and a FAILED restoration's journal is the only material that can repair the tree. |
| MEM-006 | "A missing lease means nothing references the journal." A missing lease means the refcount is UNKNOWN, and an unknown refcount is not zero. `(:receipt-refs lease 0)` is the shape of a refcount that fails open: one deleted file and a quota sweep destroys the pre-images a live receipt still needs. |
| MEM-013 | "Any `.clj-surgeon-publish-*` file beside a begun path is my litter." Two state homes on one workspace root do not exclude each other, so a prefix sweep can delete another transaction's PREPARED temporary between its prepare and its rename. The journal records the names it made; those are the only ones it may remove. |
| MEM-013 | "A lock file on disk means somebody holds the lock." A lock is a claim by a PROCESS; a process dies without unlinking it. A recorded pid nobody reads back is decoration, and releasing only when recovery found work leaves the one stranded case - a lock with no journal beside it - deadlocked for ever. |
| MEM-013 | "Judging a lock stale and then deleting it is one operation." It is two, and the gap between them is reachable: a second transaction can break the same stale claim and acquire inside it, after which the first breaker deletes a LIVE holder's brand new LOCK and acquires as well - two live transactions holding one project lock, the first to finish unlinking the other's claim. A break must remove the exact claim it judged: rename it, then verify the content and the (device, inode) that actually moved. |
| MEM-013 | "A lock we cannot check is a lock somebody holds." A claim written before the checkable triple existed records a pid and nothing else, so both mismatch clauses are dead and only `:process-not-alive` can fire - and a REUSED pid then reads as a live holder for as long as that number is in use, which is the permanent deadlock again on any workspace whose LOCK predates the format. An unverifiable format is an unknown: it fails closed with its own name, and the remedy that clears it demands a receipt of death, not a judgement. |
| MEM-013 | "A transaction's `finish!` releases the project lock." It unlinked whatever LOCK it found. A release must name its own claim, or one transaction's ordinary ending removes another's. |
| MEM-013 | "Checking the pid is enough." A pid is unique only within one boot and is reused inside one. Only pid plus start ticks plus boot id names one process, and an unreadable holder is an unknown, not a corpse: `begin!` refuses it and the explicit `recover!` remedy is what clears it. |
| MEM-014 | "Hashing every file at validation time gives us a snapshot." It does not. A writer that ignores the lock can land between validation and rename. |
| MEM-014 | "Detected therefore prevented." The racing write is detected at read-back and rolled over; the receipt must not imply it was excluded. |
| MEM-014 | "File identity tells two byte-identical files apart." It tells a REGULAR file from a SYMLINK, and it notices an inode change the OS happens to expose. It does not tell a file from its own recreation: ext4 hands back the same `(dev, ino)` after a delete-and-recreate, with no generation counter, and the commit succeeds with no conflict. A docstring that claims more than the instrument does is the same defect class as a receipt that claims more than the kernel does. |
| MEM-020 | "The aggregate byte ceiling can be checked from the directory entries." A file that grows during the walk must be stopped against the remaining budget, from bytes actually read. |
| MEM-020 | "Walk entries means matching files." Then an include glob conceals an unbounded walk. |
| MEM-020 | "The reservation is the largest file's parse." The walk also holds every discovered path for the whole stream; with many small files that list is the larger term and was invisible. |

## Boundaries

| id | edge | concurrent | failure |
|---|---|---|---|
| MEM-006 | pinned and staged bytes exactly equal the journal quota; a path naming a file inside the root through a `..` segment | a second transaction cannot open while the lock is held | the last injected write fails and every path returns to `H0` |
| MEM-007 | exactly the maximum read-set count; a scope whose members are swapped at an unchanged count | a read-only file drifts after sealing; a writer lands after the staged copy and before the pre-image recheck; a pinned regular file becomes a symbolic link to identical bytes; a SECOND THREAD of the same process holds the publish lock while a commit runs | a write-set file drifts between revalidation and its own rename; a writer lands inside the residual recheck-to-rename window and is overwritten, which the receipt reports |
| MEM-012 | twenty thousand recorded entries | — | an unsorted entry is refused rather than written out of order |
| MEM-013 | killed between pin and rename | killed between rename N and N+1; a live holder acquires between a breaker's read of a stale claim and its break | a pre-image object is missing, which is reported, never assumed, and its journal is retained rather than deleted |
| MEM-014 | — | an external writer lands after the rename | read-back mismatch rolls the transaction back |
| MEM-020 | exactly at each ceiling; a work budget exactly equal to the path list plus one file's parse | — | one unit past each ceiling refuses before the read; a path list one byte over the budget refuses before any read |

## Falsifiers

| Requirement | Falsifying observation |
|---|---|
| MCP-OP-MEM-006 | A staged path is written without a durable pinned pre-image, or a path containing a `..` segment is pinned or staged, or a path whose NOFOLLOW type or file identity changed after pinning is written, or a committed receipt cannot be undone because its journal was deleted, or a transaction retains a repository-wide original or future source map. |
| MCP-OP-MEM-007 | A transaction commits after a file that shaped its plan changed, or after the scope gained, lost or SWAPPED a member, or without holding the lock; or bytes are copied into the target directory INSIDE the recheck-to-rename window; or a receipt omits that window; or a second thread of the same process is thrown `OverlappingFileLockException` in place of waiting; or a task spawned inside the lock writes without taking it; or an exception out of a commit leaves the project lock held. |
| MCP-OP-MEM-012 | Recording N read-set entries makes the transaction value's per-path record count or retained string bytes grow with N. |
| MCP-OP-MEM-013 | A crash leaves a path holding neither its pre-image nor its verified result, or recovery reports success without verifying a digest, or a journal whose restoration failed is deleted, or a break removes a claim other than the one it judged, or a transaction's release unlinks a lock it does not own, or a claim in an unverifiable format is reported as a live holder or broken without a receipt of its holder's death. |
| MCP-OP-MEM-014 | A receipt claims snapshot isolation, omits the isolation statement, omits the residual commit window, or a racing external write goes undetected. |
| MCP-OP-MEM-020 | A walk reads a file past a ceiling, follows a symbolic link, retains a source after its planner callback returned, or reports a reserved peak that omits heap it holds for the whole stream. |
