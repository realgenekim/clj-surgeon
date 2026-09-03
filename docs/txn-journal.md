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
- what remains inside is a digest recheck, one `write-begin` fsync and one
  rename, plus the identity stat — `(:commit-window (txn-journal/contract))` names them, and every
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
digest that was pinned before the first write; staging temporaries the dead
process left in the tree are removed; the lock is released. A transaction whose
`state.edn` says `:committed` or `:rolled-back` is not a candidate. A recovery whose
restoration verified deletes its journal; one that did not verify records
`:restore-failed` and keeps everything.

Two crash points are witnessed: killed between pin and rename (nothing was
written, so nothing is restored, and the lock is freed) and killed between
rename N and N+1 (exactly the begun paths are restored and verified).

## Ceilings

Request-lowerable under a server hard maximum. Exactly at the limit is admitted;
one unit past refuses BEFORE the effect the limit bounds, with a `next_call`
that narrows scope. A mutation receipt is refused, never truncated.

| limit | default | what it bounds |
|---|---:|---|
| `max-read-set-files` | 20,000 | files that may influence one plan |
| `max-staged-files` | 2,000 | files one transaction may modify |
| `max-journal-bytes` | 512 MiB | pinned plus staged bytes on disk |
| `max-walk-entries` | 200,000 | every visited entry, not only matches |
| `max-depth` | 40 | refused per entry, never truncated |
| `max-file-bytes` | 2 MiB | from bytes actually read |
| `max-aggregate-bytes` | 512 MiB | from bytes actually read, against the remaining budget |
| `work-budget-bytes` | 192 MiB | `bytes x parse-factor` reserved before the parse |
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

600 generated namespaces of 512 KiB, 314,772,270 bytes, every file a quarter of
the 2 MiB per-file ceiling and the count under a third of the 2000-file ceiling.

| arm | Xmx | result | wall | retained peak |
|---|---|---|---:|---:|
| frozen read | 256m | `OutOfMemoryError`, exit 3 | - | - |
| frozen read (reference) | 2g | completed, peaked at 2046.8 MB used | 150,949 ms | - |
| journal + scope-stream | 256m | committed 600 files | 157,679 ms | 14.0 MB |

Output parity is three-way and exact: the reference's digest over its 600 result
hashes, the journal arm's streamed digest, and a digest recomputed by the test
parent from the tree on disk after the commit all equal
`55423110f805a112cd6b353252ccd5183e035dfb8fe4b50da52e5f310a762440`.

Retention is flat: 13.93 MB at 60 files, 14.34 MB at 600. Ten times the files
cost 0.41 MB. The attributable reserved peak was 29,378,776 B, inside the
192 MiB work budget. Sampled used-heap peak was 251.7 MB for the journal arm and
252.5 MB for the eight-file control that retains 12 MB, which is why it is a
trend line and not a gate.
