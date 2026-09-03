The committed design should be an optimistic, disk-journaled transaction with a byte-budgeted heap. Repository size may increase I/O and temporary disk use, but must not increase retained heap.

The core pipeline is:

`stream discovery → bounded projection/index → stage writes → validate read set → commit/rollback`

No operation may realize the repository, its sources, its ASTs, or its path list into one Clojure collection.

## 1. Retention and the pre-image guarantee

Use a transaction manifest, not retained source strings.

For every observed file, the pre-image record is:

```clojure
{:path-id ...
 :path ...
 :kind :regular
 :source-bytes ...
 :sha256 ...
 :mode ...}
```

The complete manifest belongs in an operation-local, sorted file under `.clj-surgeon/transactions/<txid>/`, not in an in-memory map. Only the active chunk and files represented in the bounded receipt/write plan remain resident.

Resident for the transaction:

- Hash, path ID, and compact integer spans for files actually represented in the plan/receipt.
- Compact cross-file facts that have not yet been spilled.
- Replacement text or a reference to staged replacement text.
- Receipt projection up to its explicit byte/record ceiling.
- Small merge buffers and transaction state.

Not resident:

- Repository-wide source strings.
- Repository-wide rewrite-clj nodes or zipper locations.
- A Clojure map of all paths and hashes.
- Rollback source bytes.
- Both old and new projections.

For a file being modified:

1. Planning reads and hashes the exact bytes being parsed.
2. Staging rereads it and requires the same hash.
3. Before any live write, its exact pre-image bytes are copied/reflinked into the durable transaction CAS.
4. The new version is built in staging.
5. Commit uses per-path atomic rename, records progress durably, and verifies the resulting hash.
6. Rollback and crash recovery restore the pinned pre-image bytes and verify their hashes.

“Capture hashes at discovery, reverify at write” is acceptable, but only with a precise guarantee:

- Every edit was planned against the recorded `H0`.
- A path is not knowingly overwritten unless it still contains `H0`.
- Exact rollback bytes are durable before that path is changed.
- Any surgeon-cooperating writer is excluded by the project transaction lock.

Checking only the write set is insufficient for cross-file operations. Before the first write, revalidate:

- Every file whose facts influenced the plan.
- Scope membership, including additions, deletions, and renames that could introduce a caller or alias.
- The write set again immediately before replacement.

The correctness cost is an additional full hash pass over the semantic read set, more conflict aborts, and additional disk I/O.

There is one unavoidable qualification: hashes plus revalidation do not create a strict simultaneous repository snapshot against arbitrary external writers. An editor ignoring the advisory lock can race between verification and rename, and sequential discovery can observe mixed eras. Strict isolation against such writers requires a filesystem snapshot or a mandatory lock they respect. The honest contract should therefore be “optimistic serializability with conflict detection and exact rollback,” not claim arbitrary-writer snapshot isolation.

Likewise, multiple per-file renames are recoverably all-or-rollback, but not instantaneously atomic to unrelated readers.

## 2. Streaming and the smallest cross-file index

Replace `file-seq`, `slurp`, `mapv`, `into`, and global sorting with a bounded walker and external sorting:

- `Files/walkFileTree` or equivalent reducible traversal.
- Count every visited entry before filtering.
- A bounded descriptor queue, ideally no more than `2 × workers`.
- External sorted runs when deterministic path order is required.
- Read one bounded byte array, hash it, parse/project it, emit compact records, then release it.

The normal per-file lifecycle is:

```text
open → enforce actual byte budget → hash/decode → parse once
     → emit outline/index/plan facts → drop source and parse tree
```

For cross-file operations, retain facts, not syntax trees:

- Namespace definition: namespace ID, path ID, hash, declaration span.
- Alias/import/refer facts: namespace ID, local symbol ID, target ID, span.
- Definitions: qualified symbol ID, path ID, span.
- Relevant references/callers: target ID, path ID, span.
- Dependency edges: two integer IDs and edge kind.

Use a two-pass strategy when targets are known:

1. Build the small target/definition/alias key set.
2. Rescan and emit only facts that could affect those targets.

Records are compact integer/binary records. When their reserved bytes reach the index budget, flush sorted runs to transaction scratch space and merge them. Topological operations that require random indegree updates should use an operation-local disk table with a bounded page cache; do not translate it into nested Clojure maps.

A repository-wide hash manifest is also streamed to disk. Even “just 32 bytes per hash” becomes substantial after paths and Clojure object overhead are included.

Read-only receipts may paginate with a continuation token bound to the manifest digest. Mutations must never silently truncate: if the complete write plan or required receipt cannot fit its contract, refuse before staging or writing.

## 3. Zipper posture

For outline, dependency extraction, topology facts, and census classification, use a transient rewrite-clj node tree without zipper navigation.

That is the safest initial choice because it preserves Clojure syntax, reader conditionals, comments, and lexical positions consistently. `tools.reader` is sufficient for predicates that need only semantic forms, but its discarded trivia and position behavior make it a poor universal replacement for exact source spans. Maintaining two subtly different syntax interpretations is not worth the initial saving.

Rules:

- Never put zipper locations into an outline, index, future, or receipt.
- Never retain a node tree beyond the active file.
- Use direct node traversal for read-only projections.
- Create a zipper only for a file that actually needs structural editing.
- Use that zipper during planning or staging, emit an edit script/result, then discard it.
- Reparse the write-set file during staging under the `H0` check; do not retain its planning zipper for the lifetime of the transaction.

Thus even if a pathological file needs tens of megabytes temporarily, the cost is per admitted worker, not multiplied by repository size.

Add parser-specific hard limits for syntax-node count and nesting depth. File bytes alone do not bound allocation for token-dense or deeply nested input.

## 4. Ceilings are admission contracts

Server configuration owns non-overridable hard ceilings. Requests may supply lower operational ceilings.

| Limit | Request-visible | Server hard maximum |
|---|---:|---:|
| Matching files | Yes | Yes |
| Bytes per file | Yes | Yes |
| Aggregate source bytes | Yes | Yes |
| Receipt/result records and bytes | Yes | Yes |
| Files to modify / staged bytes | Yes | Yes |
| Walk entries | Optional lower bound | Yes |
| Filesystem walk depth | Optional lower bound | Yes |
| Syntax nodes/nesting | No | Yes |
| Heap work budget | No raw byte override | Yes |
| Worker count | No, or only a lower preference | Yes |
| Cache, scratch, and rollback disk | No | Yes |

Important details:

- Explicitly requested files pass through exactly the same byte and parser gates as discovered files.
- Aggregate accounting uses actual bytes read, not only pre-walk `stat` values.
- Discovery may use sizes for early refusal, but a growing file must be stopped before it exceeds the remaining budget.
- Walk entries count directories, symlinks, unreadable entries, and nonmatching files. Otherwise an include glob can conceal an unbounded walk.
- Aggregate bytes are an I/O, CPU, validation-window, and denial-of-service contract even though streaming removes their direct relationship to heap.
- No side effects occur until discovery, planning, staging, and validation have all passed their ceilings.

A refusal should be structured:

```clojure
{:status :refused
 :code :limit/aggregate-bytes
 :limit {:requested 134217728
         :server-max 268435456
         :observed-at-least 134349912}
 :at {:path "src/x.clj" :files-seen 381}
 :complete false
 :next_call {:op :alias-migration
             :scope {:root "src/component-a"}}
 :semantic_scope_changed true}
```

`next_call` should concretely narrow a root, include set, namespace closure, depth, or continuation range. It must not merely say “increase heap.”

For an individually oversized file, excluding or splitting that file may change semantics; the refusal must say so. There is no honest correctness-preserving `next_call` for transforming a single file that exceeds the server’s hard parser limit.

## 5. Parallelism is derived from measured memory

Claypoole pool size must be an admission result, not a constant.

For parser/operation `op` and input-size bucket `s`, measure:

```text
worker_cost(op, s) =
  fixed_worker_p99
  + parse_factor_p99(op, s) × s
  + max_projection_bytes_per_file
```

Then:

```text
available =
  heap_work_budget
  - receipt_reserve
  - merge_reserve
  - emergency_headroom

pool =
  min(cpu_cap,
      server_pool_cap,
      floor(available / worker_cost(op, max_file_bytes)))
```

If one worst-case admitted file cannot fit, refuse before parsing. Unknown parser profiles begin at pool size 1.

Also use a weighted semaphore based on the file’s byte bucket. This permits several small files without admitting several maximum-sized files simultaneously.

The queue contains path descriptors, not source strings or parsed results. “Chunk” should mean a bounded output batch flushed to the index/receipt; an AST-bearing chunk size is always one per worker. The real inequality is:

```text
workers × per-worker peak
+ queued projection bytes
+ receipt/index buffers
≤ heap work budget
```

Parse factors must come from measured adversarial and representative fixtures, not average source size. Record parser version, operation, byte bucket, node density, and JVM version. Reserve additional heap headroom rather than depending on emergency GC.

## 6. Disk cache under `.clj-surgeon`

Cache only immutable projections:

```text
.clj-surgeon/cache/outlines/<schema>/<source-sha256>
```

Each entry includes parser/schema version, relevant options, source length, projection records, and a projection checksum. It must not contain source strings, nodes, zippers, or executable deserialization formats.

A cache hit still requires a trustworthy source hash. Without a filesystem watcher or snapshot generation, reading and hashing the file remains necessary; the cache avoids parsing, not validation.

Use a measured second-hit policy:

- Do not cache a projection on its first one-shot encounter.
- Admit it after the same hash is parsed a second time.
- Cache only where measured parse time is materially greater than cache read/decode time.
- Bound the cache by bytes and evict LRU/2Q-style.
- Treat corruption as a miss.

Small files will usually be cheaper to reparse than to open, decode, and maintain cache metadata. Large or frequently revisited files, fold checkpoints, and stable dependency projections are the useful cases.

Transaction scratch and rollback CAS are mandatory correctness storage and have separate quotas from this optional performance cache. Memory-mapping source files is not a solution; it merely moves pressure into native/address-space and page-cache accounting.

## 7. Fold-diff becomes a streaming merge

Introduce a baseline/checkpoint format consisting of sorted, length-delimited canonical relation records. Do not store the next format as one EDN vector or map.

Each projection is produced as sorted runs with records such as:

```text
relation-key | canonical-value-length | canonical-value | digest
```

Then merge two cursors:

- Key on only the left: removal.
- Key on only the right: addition.
- Same key and equal canonical value: unchanged.
- Same key and different value: modification.

Only the current pair and a bounded output batch are resident. The checkpoint is parsed once into its record stream. Baseline v1’s monolithic EDN should have a legacy byte ceiling and a one-time streaming conversion path.

A per-relation digest is useful for fast equality and cache keys, but the direct sorted merge is the primary shape. Where exactness beyond the cryptographic-hash assumption is desired, equal digests can still be confirmed by streaming canonical-byte comparison without materializing either projection.

Diff results are streamed into the receipt encoder or paged. If the receipt budget is reached, return a continuation bound to both projection digests; do not accumulate the remaining differences.

## 8. Measurement and pass lines

Every operation receipt should include:

```clojure
{:memory {:xmx-mb ...
          :heap-used-start-mb ...
          :heap-used-peak-mb ...
          :heap-used-end-mb ...
          :heap-reserved-peak-mb ...
          :gc-pause-ms ...
          :concurrent-ops ...}
 :work {:walk-entries ...
        :files-discovered ...
        :files-read ...
        :files-parsed ...
        :files-written ...
        :source-bytes ...
        :largest-file-bytes ...
        :projection-records ...
        :receipt-bytes ...
        :spill-bytes ...
        :rollback-bytes ...
        :cache-hits ...
        :workers ...}}
```

Use a continuous JVM heap sampler for the process-wide peak and the admission accountant for attributable reserved peak. Under concurrent operations, label `heap-used-peak` as process-wide; pretending it is precisely attributable would be misleading. CI can additionally use JFR and after-GC object histograms to locate retention. Do not call `System.gc` in production.

The Anvil battery should run each tree-scale operation at 100, 1,000, and 10,000 files in fresh and warmed JVMs, five repetitions, with `-Xmx512m`. Include:

- Representative small/medium source distribution.
- Token-dense and deeply nested files.
- The witnessed 450 × 1.9 MiB alias case.
- Cache cold and warm.
- Empty diff, sparse diff, and full diff.
- Conflict injected during staging, validation, every commit boundary, and rollback.
- Add/delete/rename injected into the cross-file scope.

With a 192 MiB operation work budget, the pass line should be:

- No OOM.
- `heap_reserved_peak_mb ≤ 192`.
- `heap_used_peak_mb ≤ min(heap_start + 224, 0.80 × Xmx)`; at 512 MiB, no more than about 410 MiB.
- The 10,000-file peak is no more than 32 MiB above the 1,000-file peak.
- After-GC retained heap at 10,000 files is no more than 8 MiB above 1,000 files.
- Outputs and hashes match the unbounded reference implementation.
- Over-budget cases return a structured refusal before mutation.
- The 450 × 1.9 MiB case either refuses during stat/streaming aggregate admission with less than 32 MiB heap growth, or completes under an explicitly larger aggregate-I/O budget without changing the heap pass line.
- Every injected conflict produces either zero writes or verified restoration to all `H0` hashes.

The key graph should be peak-versus-N: after the bounded buffers fill, it must visibly flatten. Runtime and spill bytes may grow with N; retained heap may not.

## What I would not do

- Do not raise `-Xmx` to 4 GiB as the memory posture.
- Do not retain raw sources merely to provide rollback; pin them durably on disk.
- Do not trust hashes without pinning rollback bytes.
- Do not validate only files being written when read-only files influenced the plan.
- Do not retain zipper locations or repository-wide node trees.
- Do not return lazy sequences whose head can retain the walker or parsed objects.
- Do not sort paths, relations, or files into a repository-sized in-memory map/vector.
- Do not size parallelism from CPU count alone.
- Do not let explicit files bypass any discovery limit.
- Do not silently truncate a transactional receipt.
- Do not claim strict arbitrary-writer snapshot isolation when the filesystem does not provide it.

I could not inspect the repository itself in this session: even read-only shell commands failed before execution with `bwrap: loopback: Failed RTM_NEWADDR`. The design above is therefore grounded in the measured implementation facts in the question rather than verified source locations.