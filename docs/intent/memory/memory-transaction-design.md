# Memory transaction design (the B1 kernel)

Gene chose option B on 2026-09-03: *"B. Go."* — Sol's disk-pinned pre-image.
This document is the owning design for `MCP-OP-MEM-006`, `-007`, `-012`, `-013`,
`-014` and `-020`, plus the two clauses of `MCP-OP-MEM-001` the kernel carries.
The requirements themselves are in `memory-transaction-specs.md`.

## The defect

`alias_migration` slurps every scoped source into one realised vector and RETAINS
it for the whole call, because the transaction needs a consistent pre-image:
read-back hashes and rollback both read from it. `extract!`/rewire does the same
through `file-seq` plus `slurp` into one sorted map. The retention is therefore
not an accident to be tidied away — it is load-bearing, and removing it means
replacing what it was load-bearing FOR.

Measured on Anvil, 2026-09-03: 600 generated namespaces of 512 KiB
(314,772,270 bytes), every file a quarter of the 2 MiB per-file ceiling and the
count under a third of the 2000-file ceiling, kills a `-Xmx256m` JVM with
`OutOfMemoryError: Java heap space` before the parse phase begins. The aggregate
byte ceiling added the same night refuses that scope; it does not make the
retention correct, and a ceiling is an argument where this is a guard.

## The shape

    stream discovery -> bounded projection -> pin -> stage -> revalidate -> commit / rollback

Two namespaces, adopted by no verb yet:

- `clj-surgeon.scope-stream` walks the scope and hands one file at a time to a
  planner callback, dropping the source when the callback returns.
- `clj-surgeon.txn-journal` keeps the transaction's durable state: a sorted
  read-set manifest, a content-addressed pre-image store, staging files, an
  fsynced progress journal, and a project lock.

Nothing repository-sized is realised into a Clojure collection: not the sources,
not the parse trees, not the path list, not the hash map.

## What is resident, and what is not

Resident, per open transaction:

- one running membership digest and the previous path (two values, not a
  collection);
- the write set's records — path, pre-image digest, result digest, staging file
  name — bounded by the `max-staged-files` ceiling;
- one file's source and one parse tree, inside the planner callback;
- counters.

Not resident: repository-wide source strings, repository-wide node trees or
zipper locations, a map of all paths to hashes, rollback bytes, or both sides of
a diff.

## Why hashes plus revalidation is an acceptable pre-image

Because the guarantee is stated precisely and the rollback bytes are durable:

1. every edit was planned against the digest recorded in the manifest;
2. a path is not knowingly overwritten unless it still holds that digest,
   rechecked immediately before its own replacement;
3. exact rollback bytes are pinned into the transaction's object store before
   that path is changed;
4. the whole semantic read set — every file whose facts influenced the plan, not
   only the write set — is revalidated before the first write, and scope
   membership is re-derived so a file that ARRIVED after planning is a conflict.

The correctness cost is one extra full hash pass over the read set, more
conflict aborts, and more disk I/O.

## The contract, stated honestly

**Optimistic serializability with conflict detection and exact rollback.**
**Not snapshot isolation.**

Hashing every file at validation time does not create a simultaneous snapshot.
A writer that ignores the project lock can land between our validation and our
rename. What the kernel promises is that such a race is DETECTED — at read-back —
and that every path this transaction began writing returns to its pinned bytes,
verified by digest. It does not promise that the race was excluded, and it does
not promise that a multi-file commit is instantaneously atomic to an unrelated
reader. `contract` returns this statement, every receipt carries a compact form
of it, and a witness demonstrates the limit rather than asserting the claim.

One consequence is worth stating because it is easy to misread as a bug: a
transaction restores what IT changed and never clobbers a write it did not make.
A file that drifts before its own rename is a conflict, and it keeps the other
writer's bytes; a file that drifts after its rename is rolled back to the
pre-image, and the other writer's bytes are lost. Both are in the witnesses.

## Ceilings are admission contracts

Every limit is request-lowerable under a server hard maximum, and every witness
has Sol's shape: exactly at the limit is admitted, one unit past refuses BEFORE
the effect the limit bounds, with a `next_call` that narrows a root, an exclude,
a depth, or a file count. Never a wall, and never a silent truncation of a
mutation receipt.

| limit | bounds |
|---|---|
| `max-read-set-files` | how many files may influence one plan |
| `max-staged-files` | how many files one transaction may modify |
| `max-journal-bytes` | pinned pre-image plus staged future bytes on disk |
| `max-walk-entries` | every visited entry, not only matching files |
| `max-depth` | refused per entry, never truncated out of the found count |
| `max-file-bytes` | from bytes actually read |
| `max-aggregate-bytes` | from bytes actually read, checked against the REMAINING budget |
| `work-budget-bytes` | `bytes x parse-factor` reserved before the parse |
| `max-receipt-records` / `max-receipt-bytes` | the serialized receipt (registered under MEM-001) |

## The meters, and which ones are hard lines

- **`heap-retained-peak-mb`** — used heap after a forced full collection at
  checkpoints. This is the retention meter and it is a hard line.
- **`heap-reserved-peak-bytes`** — the accountant's attributable peak
  reservation, `largest admitted file x parse-factor`. Hard line: it must stay
  inside the work budget. This is the figure the memory battery was reporting as
  UNMEASURED.
- **`heap-used-peak-mb`** — sampled on a timer. A TREND line, never a gate.
  Under default G1 at a small heap it measures how close allocation ran to the
  ceiling, not what the arm holds: an eight-file control that retained 12 MB
  peaked at 251 MB of used heap at `-Xmx256m`.

The parse factor is measured, not guessed: 54.6 heap bytes per source byte for a
rewrite-clj node tree on this repository's corpus (Sol measured about 45 on its
own). The default reserves at 56.

## What this design does not do

- It does not raise `-Xmx` as a memory posture.
- It does not retain raw sources to make rollback possible.
- It does not trust hashes without pinning rollback bytes.
- It does not validate only the files being written.
- It does not size parallelism from CPU count; the kernel is single-file-at-a-
  time and the measured worker reservation is `MCP-OP-MEM-008`, a separate leaf.
- It does not add a memory slope line; `max(held_mb at N=10,000) <=
  max(held_mb at N=1,000) + 2.0 MiB` belongs to `MCP-OP-MEM-011`, the battery
  gate.
- It is adopted by no verb. Adoption of `alias_migration` and `extract!` is B2,
  after their open rounds land.
