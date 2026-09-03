# The transaction journal

`clj-surgeon.txn-journal` and `clj-surgeon.scope-stream` are the B1 kernel:
a disk-journaled transaction with a byte-budgeted heap. **They are adopted by no
verb yet.** Adoption of `alias_migration` and `extract!` is a separate build,
after their open rounds land.

The owning design and requirements are in
[`docs/intent/memory/`](intent/memory/); this page is the operator's view: what
the contract promises, what it refuses to promise, what is on disk, and how
recovery works.

## The contract

**Optimistic serializability with conflict detection and exact rollback.**

**Not snapshot isolation.** Hashing every file at validation time does not
create a simultaneous repository snapshot. A writer that ignores the project
lock can land between validation and rename. The kernel DETECTS that at
read-back and restores every path it began writing to the exact pre-image bytes
it pinned before the first mutation, verified by digest. It does not exclude the
race, and it does not make a multi-file commit instantaneously atomic to an
unrelated reader. `(txn-journal/contract)` returns this statement in full, and
every receipt carries a compact `:isolation` form of it.

**The residual commit window.** An atomic rename replaces; it does not
compare-and-swap. The pre-image recheck and the rename are two syscalls, so
there is a window between them that no POSIX filesystem lets us close. The
kernel narrows it and then names it rather than hiding it:

- the staged bytes are copied into the TARGET's own directory **before** the
  window opens, so the expensive half of publication is outside it;
- the recheck and the rename are taken under an advisory `PUBLISH.lock` on the
  workspace state root, which excludes any writer that asks for it;
- the target's current digest is read **before** the lock is taken, with a
  NOFOLLOW stat on each side of it, so what remains inside is one stat
  comparison (type, `(device, inode)`, size, `mtime` and `ctime` in
  nanoseconds), one `write-begin` fsync and one rename — and none of the three
  reads or copies the target. The size term is **reduced about four-fold, not
  eliminated**: the in-window fsync still pays for the writeback the pre-lock
  staging copy left behind, and 2 MiB measures 1.9x 1 KB after the change
  against 4.4x before it (see the measured table below; `:size-term` in the
  contract carries the same statement into every receipt). The digest is
  re-read inside the lock only when that stat moved, and every receipt reports how often that happened in
  `:digest-rereads`, so a fast path can never be mistaken for a skipped check.
  `(:commit-window (txn-journal/contract))` names the operations, and every
  commit receipt carries the same map with the widest observed `:max-ns`;
- a writer that does not take the publish lock and lands inside that window is
  **overwritten**, not detected. The pinned pre-image journal is its recovery.

A writer that lands anywhere EARLIER — after the staged copy, before the
recheck — is refused with `:txn-conflict` and zero writes.

What it guarantees:

- every edit was planned against the pre-image digest recorded in the manifest;
- no path is knowingly overwritten unless it still holds that digest, rechecked
  immediately before its own replacement;
- exact rollback bytes are durable before any path is changed;
- a crash leaves a journal from which every begun path is restored and verified.

What it does not promise:

- a simultaneous repository snapshot against a writer that ignores the lock;
- instantaneous atomicity of a multi-file commit to an unrelated reader;
- protection from a racing external write — that is detected and rolled over,
  not prevented.

**Cooperation is PER-WRITER, and most writers in this repository do not
cooperate yet.** An advisory lock excludes only writers that ask for it. For a
long time the kernel's own commit path was the ONLY caller of
`file-ops/with-publish-lock*` in the whole repository, so the clause "it
excludes any writer that asks for it" was true with an empty referent and the
residual window was the normal path rather than the exception.

`file-ops/atomic-write!` can now take the lock, but only when a caller opts in
by binding `file-ops/*publish-lock-dir*` — `txn-journal/with-cooperating-writes`
is that opt-in. Unbound, which is still the default everywhere, it takes no
lock. **These are the source-mutating sites that do NOT cooperate today**, each
publishing through `file-ops/atomic-write!` / `atomic-create!` with no lock:

| namespace | sites |
|---|---|
| `extract.clj` | `:543-544`, `:594`, `:674`, `:689`, `:695` |
| `move.clj` | `:434` |
| `structural_lens.clj` | `:1048`, `:1077`, `:1313` |
| `mcp_change_buffer.clj` | `:1558` |
| `mcp_extraction.clj` | `:435`, `:543` |
| `intent_transaction.clj` | `:2186` |
| `workspace_onboarding.clj` | `:236`, `:261`, `:383` |
| `agent_routing.clj` | `:124` |
| `mcp_cold_verify.clj` | `:56` |
| `mcp_tool.clj` | `:381` |
| `extract.clj` (receipt) | `:242` |
| `intent_transaction.clj` | `:2187` (`atomic-create!`) |
| `worktree_lifecycle_io.clj` | `:399` — a PRIVATE duplicate `atomic-write!`, called at `:438`. **Binding `*publish-lock-dir*` cannot reach it**, so this one needs a code change, not an opt-in. |

Retrofitting them belongs to the lanes that own those files, not to the kernel
build; until it happens, read every one of them as a non-cooperating writer and
read the residual window as the ordinary case rather than the exception.

> **FOLLOW-UP, and an adoption obligation.** Retrofitting the table above is
> not part of this kernel build and is not optional before a verb adopts the
> kernel: an adopting verb that mutates sources through any of those sites is a
> writer its own transaction cannot exclude. Done when every site either binds
> `file-ops/*publish-lock-dir*` or is recorded here as deliberately
> non-cooperating, and this table is empty or annotated.

**What the opt-in is safe for, and what it still is not.** Cooperation is now
safe in both concurrency directions, which it was not when it was introduced:

- **Threads of one process serialise before the OS lock is requested.**
  `FileChannel/lock` is a per-PROCESS view: a second thread asking for a lock
  this JVM already holds is thrown `OverlappingFileLockException` rather than
  made to wait, and that throw escaped `commit!` before `finish!` could release
  the project `LOCK` — stranding the workspace behind a LIVE pid neither
  `begin!` nor `recover!` may break. `with-publish-lock*` now takes a
  process-wide `ReentrantLock` keyed by the lock file's canonical path first,
  so threads queue.
- **Re-entrancy is the identity of the THREAD**, read off that monitor's hold
  count. It used to be a dynamic var, and Clojure conveys dynamic bindings to
  `future`, `send`, `pmap` and every `bound-fn`: a task spawned inside the lock
  inherited the claim and wrote with no lock at all. A spawned task now takes
  the lock itself and waits for whoever holds it, including another process.
- **Every exception path out of `commit!` ends the transaction**, so a failure
  inside the lock releases the project `LOCK` and marks the journal rather than
  leaving the workspace held.

What it still does not buy: the lock is advisory, so a writer that does not bind
`*publish-lock-dir*` is not excluded — including the private duplicate
`atomic-write!` in the table above, which no binding can reach.

**The project `LOCK` is scoped to the STATE HOME, not to the workspace root.**
`begin!` locks `workspace/transactions-dir root state-home`, so two
transactions on one workspace root with two different `:state-home` values both
acquire a lock, both reach commit, and neither excludes the other — they are
separated only by the optimistic digest recheck, which is the design, and their
`PUBLISH.lock` files are two different files. "The transaction lock" means one
lock per (workspace root, state home) pair, not one per workspace.

A consequence that reads like a bug until you see the rule: a transaction
restores what IT changed and never clobbers a write it did not make. A file that
drifts BEFORE its own rename is a conflict and keeps the other writer's bytes; a
file that drifts AFTER its rename is rolled back and the other writer's bytes
are lost.

## The disk layout

Nothing is written into the tree being mutated. Everything hangs off the
workspace's own durable state root, beside the receipts:

    ~/.local/state/clj-surgeon/workspaces/<sha256 of canonical root>/
      receipts/
      transactions/
        LOCK                     one holder: {:txid :pid :acquired-at}
        <txid>/
          manifest.tsv           sorted read set, streamed as it is observed
                                 path-id \t path \t bytes \t sha256 \t mode
          journal.log            fsynced progress, one line per event
          objects/<sha256>       pinned pre-image bytes, content addressed
          staging/<path-id>.new  future bytes
          state.edn              {:txid :workspace-root :status ...}
          lease.edn              why the journal is RETAINED and what references it

`mcp-workspace/state-dir` resolves the root and `transactions-dir` names this
directory; `receipt-dir` is unchanged. The staging file is published through a
temporary in the TARGET's own directory, because the state root may sit on a
different filesystem where `ATOMIC_MOVE` is unavailable; the observable
replacement is still one atomic rename, and the target's permissions survive it.

## The order of operations

1. `begin!` — take the project lock, create the transaction directory.
2. `record-read!` — one read-set entry at a time, in ascending path order,
   straight to the manifest. A step backwards is refused (`txn-manifest-unsorted`)
   rather than sorted in memory.
3. `pin!` — copy a path's exact pre-image bytes into `objects/<digest>` and record
   the path's NOFOLLOW type and file identity (device/inode). No path may be
   written until this has succeeded for it.
4. `stage!` — write the future bytes to a staging file. Nothing is retained.
5. `seal-read-set!` — close the manifest, freeze the read-set membership digest
   and, when the transaction carries a `:scope-walk`, the scope-membership digest
   over path, NOFOLLOW type and file identity.
6. `revalidate!` — re-hash the WHOLE read set, recheck every pinned path's type
   and file identity, and re-derive the scope-membership
   DIGEST, comparing it with the one sealed at plan time. A count is not a set:
   `[a b]` planned against `[c d]` observed is a conflict at equal count.
7. `commit!` — for each staged path in sorted order: copy the staged bytes into
   the target's own directory; then, under the workspace publish lock, recheck
   its pre-image digest and its file identity, fsync `write-begin` and rename; then release the lock,
   fsync `write-done`, read back and verify.
8. `rollback!` / `recover!` — restore the pinned bytes and verify each digest.
9. `undo!` / `forget!` / `evict!` — reverse a committed receipt from its retained
   pre-images, or release the journal.

## Retention: a receipt you cannot undo is not a receipt

A finished transaction's journal is not garbage.

| outcome | journal | why |
|---|---|---|
| `:committed` | **retained** | the pre-images are the only way to reverse the change the receipt describes |
| `:rolled-back` | discarded | every path verified back at H0; nothing is left to recover from |
| `:restore-failed` | **retained, un-evictable** | the tree is NOT at H0 and this is the only material that can repair it |

Staging files are reclaimed on commit — those bytes are now the bytes in the
tree — and the pre-image objects stay. `lease.edn` records the status, the
receipt refcount, and whether the row may be evicted.

- `undo!` republishes every path the journal recorded as BEGUN from its own
  pinned objects and verifies each against the digest pinned before the first
  write. A pinned path the transaction never wrote is not touched: republishing
  it would clobber a write somebody else made.
- `forget!` is the explicit release. It refuses a `:restore-failed` journal.
- `evict!` is the quota-driven release. It refuses a `:restore-failed` journal
  AND one a receipt still references; `release-receipt!` drops that reference.
- `retained-transactions` is what a quota sweep reads.

## Recovery

`recover!` reads only the journal. The `pin` lines say which pre-image bytes are
durable; the `write-begin` lines say which paths were begun. Each begun path is
republished from its pinned object in reverse order and verified against the
digest that was pinned before the first write; the publication temporaries the
dead process left in the tree are removed — only those THIS journal's own
`write-begin` lines named, never every `.clj-surgeon-publish-*` sibling, because
two state homes on one workspace root do not exclude each other and a prefix
sweep can delete another transaction's prepared temporary; the lock is released
whenever it has no live holder, including when there was nothing to recover.
A transaction whose
`state.edn` says `:committed` or `:rolled-back` is not a candidate. A recovery whose
restoration verified deletes its journal; one that did not verify records
`:restore-failed` and keeps everything.

Two crash points are witnessed: killed between pin and rename (nothing was
written, so nothing is restored, and the lock is freed) and killed between
rename N and N+1 (exactly the begun paths are restored and verified).

**The project `LOCK` records a checkable holder.** Pid alone is not an
identity — it is reused within a boot and repeated across boots — so the lock
carries pid, the holder process's start ticks and the boot id. `begin!` breaks
a lock exactly once when that triple PROVES the holder is gone, leaving
`:lock-broken {:reason :stale-holder :cause … :pid …}` on the transaction and a
`lock-broken` journal line. It never breaks a lock whose holder is live, and it
never breaks one it cannot read: an unparsable LOCK is an unknown, refused
fail-closed, and `recover!` is the remedy that clears it.

**`undo!` is a write, and behaves like one.** It takes the same publish lock as
the commit path and rechecks every begun path's digest and NOFOLLOW identity
against H1 — what the commit LEFT BEHIND, recorded in the `write-done` lines.
If any path moved since the commit, the whole undo refuses with
`:txn-undo-conflict`, names the path, and writes nothing. Crash recovery makes
no such check: a killed transaction left the tree part-written on purpose.

## Ceilings

Request-lowerable under a server hard maximum. Exactly at the limit is admitted;
one unit past refuses BEFORE the effect the limit bounds, with a `next_call`
that narrows scope. A mutation receipt is refused, never truncated.

One default is DERIVED rather than chosen, and the derivation is a rule a test
enforces: a journal holds one pre-image and one future image of every byte a
transaction stages, so `max-journal-bytes >= 2 x max-aggregate-bytes`. Until
2026-09-03 it was 512 MiB against a 512 MiB read ceiling, which meant a scope
the READ path admitted was one the journal refused to stage — and it is why the
red-to-green memory arm had to override the quota to run at all. The HARD
maxima are independent server caps: a request that raises the aggregate read
ceiling above 2 GiB must raise the journal quota explicitly and is refused by a
named ceiling if it does not.

| limit | default | what it bounds |
|---|---:|---|
| `max-read-set-files` | 20,000 | files that may influence one plan |
| `max-staged-files` | 2,000 | files one transaction may modify |
| `max-journal-bytes` | 1 GiB | pinned plus staged bytes on disk — DERIVED as 2 x `max-aggregate-bytes`, one pre-image and one future image of every admitted byte |
| `max-walk-entries` | 200,000 | every visited entry, not only matches |
| `max-depth` | 40 | refused per entry, never truncated |
| `max-file-bytes` | 2 MiB | from bytes actually read |
| `max-aggregate-bytes` | 512 MiB | from bytes actually read, against the remaining budget |
| `work-budget-bytes` | 192 MiB | the retained discovered-path list plus `bytes x parse-factor`, reserved before the parse |
| `max-receipt-records` | 1,000 | serialized receipt records (MEM-001) |
| `max-receipt-bytes` | 64 KiB | serialized receipt bytes (MEM-001) |

## The meters

`heap-retained-peak-mb` (post-full-GC) is the retention meter and a hard line.
`heap-reserved-peak-bytes` is the accountant's attributable peak and a hard
line; it is the figure the memory battery reports as UNMEASURED without it.
`heap-used-peak-mb` is a TREND line and never a gate: under default G1 at a
small heap it measures how close allocation ran to the ceiling, not what the arm
holds — an eight-file control that retained 12 MB peaked at 251 MB at
`-Xmx256m`.

The memory battery on `bridge/memory-battery` owns `make memory-battery` and the
release gate `MCP-OP-MEM-011`, including the slope line
`max(held_mb at N=10,000) <= max(held_mb at N=1,000) + 2.0 MiB`. This kernel
adds no slope line of its own; it makes the reserved-peak figure measurable and
bounds the serialized receipt so a small-constant O(N) receipt is refused at a
known size rather than passing inside 224 MiB of headroom.

## Verification

`make memory-red` carries the whole red-to-green history and is deliberately
outside `make test-fast` and `make mcp-test`: it spawns child JVMs at explicit
heap ceilings, writes 300 MB of synthetic scope, and costs minutes of wall.

`make txn-kernel-warning-check` compiles both kernel namespaces with
`*warn-on-reflection*` and `*unchecked-math* :warn-on-boxed` and fails on ANY
warning. It rides `make mcp-test`. Reflection and boxed math are invisible in a
passing suite, and the sites are not cold: workspace confinement reflected
twice per staged file, journal cleanup reflected per artifact, and the reader's
arithmetic boxed once per admitted file and once per digest byte. Both
namespaces carry `(set! *warn-on-reflection* true)` so an ordinary load says so
too.

The unit witnesses are in `make mcp-test`
(`clj-surgeon.txn-journal-test`, `clj-surgeon.scope-stream-test`). Fail-first was
established per guard by mutating the implementation and re-running the single
witness:

| mutation | witness | result |
|---|---|---|
| defeat the pin guard | unpinned write | RED |
| defeat read-set revalidation | read-only drift | RED |
| defeat read-set revalidation | scope membership | RED |
| compare member counts instead of the digest | equal-count scope swap | RED |
| pin bytes without identity | file replaced by a symlink to identical bytes | RED |
| delete the journal on commit | undo a committed receipt | RED |
| delete the journal on a failed restore | failed restoration retention | RED |
| defeat pre-image restoration | crash between renames | RED |
| park a repository-wide read-set map in the transaction | retention | RED |
| claim snapshot isolation | contract statement | RED |
| ignore the requested journal quota | quota one past | RED |
| skip the pre-write recheck | write-set drift | RED |
| copy the staged bytes inside the window | writer before the recheck | RED |
| count only matching walk entries | walk-entry ceiling | RED |
| drop deep files silently | depth bound | RED |
| ignore the per-file cap | per-file ceiling | RED |
| cap from the file limit only | aggregate ceiling | RED |
| retain every source | reader retention | RED |
| truncate instead of refuse | receipt ceiling | RED |
| follow symbolic links | symlink refusal | RED |
| do not prune skip directories | pruning | RED |
| report no reservation | accountant | RED |
| charge only the largest parse | discovered path list accounting | RED |
| set the journal quota below 2x the read ceiling | derived default quota | RED |
| remove path confinement | outside-workspace pin and stage | RED |
| canonicalise before the lexical check | `..` traversal pin and stage | RED |

Three of those probes are findings in their own right, and each fix is in the
code or the tests. The first crash-recovery mutation did not go red, because
`(every? ... [])` is true and a recovery that restored nothing passed; both
crash witnesses now assert the COUNT of restored paths. The reader's retention
witness passed a mutation that retained the entire scope inside the returned
receipt, because the JVM may collect a local after its last use; the forced
collection now happens before the receipt is read. The confinement probe passed
when the `startsWith` test was inverted, because the `resolve-source-path` leg
catches the same escape independently; only removing the whole check turned it
red, which is the right answer - the promise is made by two checks.

## Measured, 2026-09-03 on Anvil

600 generated namespaces of 512 KiB, 314,772,270 bytes. Every DEFAULT ceiling
admits this workload, and the arm now runs with no override at all, which is
what makes "the defaults admit it" a claim rather than a hope:

| ceiling | default | observed | |
|---|---:|---:|---|
| per-file bytes | 2,097,152 | 524,621 | admits |
| aggregate bytes | 536,870,912 | 314,772,270 | admits |
| walk entries | 200,000 | 604 | admits |
| depth | 40 | 4 | admits |
| work budget | 201,326,592 | 29,446,956 | admits |
| read-set files | 20,000 | 600 | admits |
| staged files | 2,000 | 600 | admits |
| journal bytes | 1,073,741,824 | 629,544,540 | admits — **and did not before the quota was derived** |

The last row is the whole point of the derivation. At the old 512 MiB default
this arm refused its own workload and had to be handed a 2 GiB override.

| arm | Xmx | result | wall | retained peak |
|---|---|---|---:|---:|
| frozen read | 256m | `OutOfMemoryError`, exit 3 | - | - |
| frozen read (reference) | 2g | completed, peaked at 2046.9 MB used | 165,787 ms | - |
| journal + scope-stream | 256m | committed 600 files | 176,403 ms | 14.87 MB |

Re-measured 2026-09-03 after the reader override was removed, under the
exclusive suite lock but on a box at load ~12, which is why the walls are ~10%
longer than the first run's 150,949 / 157,679 ms. The arm's own receipt now
reads `:aggregate-bytes-max 536870912` beside `:journal-bytes-max 1073741824` -
exactly `quota = 2 x aggregate` - so it witnesses the derivation instead of
contradicting it.

Output parity is three-way and exact: the reference's digest over its 600 result
hashes, the journal arm's streamed digest, and a digest recomputed by the test
parent from the tree on disk after the commit all equal
`55423110f805a112cd6b353252ccd5183e035dfb8fe4b50da52e5f310a762440`.

Retention is flat: 12.08 MB at 60 files, 14.56 MB at 600. Ten times the files
cost 2.48 MB, and the earlier run measured 13.93 / 14.34 for the same two
points - the spread between the two runs is larger than the spread across a 10x
change in file count, which is exactly why retention is read as a trend and
never as a gate. The attributable reserved peak was 29,446,956 B - the
per-file parser reservation of 29,378,776 B plus the retained path list's
68,180 B, an identity that reproduces to the byte - inside the 192 MiB work
budget. Sampled used-heap peak was 254.1 MB for the journal arm and 249.7 MB
for the eight-file control that retains 12 MB, which is the other reason it is
a trend line.

**The residual commit window, measured before and after the digest moved out of
the lock** (this box, 9 commits per cell, median `:max-ns` from the receipt):

| target | before | after | size term |
|---|---:|---:|---:|
| 1 KB | 789,665 ns | 623,386 ns | |
| 2 MiB | 2,671,197 ns | 1,124,137 ns | |
| 2 MiB minus 1 KB | 1,881,532 ns | 500,751 ns | **-73%** |

The size term is not zero after the change and is no longer a read of the
target: what remains is the writeback pressure the 2 MiB pre-lock copy leaves
for the in-lock journal fsync.

The reviewer reproduced this on the same box with an independent before arm (a
`git archive` of the pre-change tree), and the two runs agree on direction and
magnitude: 672,439 ns / 2,939,460 ns before, 633,980 ns / 1,204,517 ns after,
a size term of 2,267,021 ns falling to 570,537 ns (-75%). Those are the numbers
`:size-term` quotes, because they carry an independent before arm. **What that
forbids saying:** the window is not `O(1)` in the target's size, and the
contract, the module docstring and MEM-007 all said it was. 2 MiB is still 1.9x
1 KB. MEM-014's rule - every statement about an instrument shall be true of it
in general - is the rule that sentence failed, and the honest form is the
measurement.
