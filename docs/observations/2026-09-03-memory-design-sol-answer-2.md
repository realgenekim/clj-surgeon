The committed design remains:

> `stream discovery → bounded projection/index → disk-staged transaction → validate read set → commit/rollback`

Repository size may increase elapsed time and temporary disk use, but retained heap must be bounded by admitted work and receipt capacity.

The measurements materially change one attribution: the root zipper is not the large resident object—the rewrite-clj node tree is. The main win is shortening tree lifetime and bounding concurrency, not replacing a root zipper with a bare node while continuing to retain every tree.

## Measurements

Measured checkout: `37592c76e7640ab62d79c46cc2b356ecbc84f542`, exactly the local `origin/main`.

Method:

- One measurement JVM, `-Xms64m -Xmx1g -XX:+UseSerialGC`.
- Five trials per representation.
- Before and after each retained object, five `System/gc` calls followed by `Runtime.totalMemory - Runtime.freeMemory`.
- The retained root was held in an atom; each representation was released before the next trial.
- `node` was `rewrite-clj.parser/parse-string-all`.
- `zipper` was `z/of-node` over that tree with position tracking.
- `outline` was `clj-surgeon.outline/outline`.
- Node counts include the root, structural nodes, tokens, whitespace, and comments via `tree-seq node/inner? node/children`.
- These are after-GC used-heap deltas, not OS RSS and not transient parse peaks.

The checkout contains only 65 Clojure-family files in `src/`. The requested N=100 corpus is therefore the sorted 65-file corpus plus its first 35 files again. Its 100 reads total 2,044,632 source bytes. The unique 65-file corpus has 175.04 nodes/KiB, close to the repeated sample.

| Corpus | Nodes | Nodes/KiB | Slurp MiB/file | Node MiB/file | Zipper MiB/file | Outline MiB/file |
|---|---:|---:|---:|---:|---:|---:|
| `intent_transaction.clj`, 126,596 bytes | 21,996 | 177.92 | 0.1210 | 5.5872 | 5.5866 | 0.0700 |
| 100-file corpus, 2,044,632 bytes | 346,335 | 173.45 | 0.0260 | 0.8716 | 0.8719 | 0.0149 |

For the 100-file corpus, median totals were:

- Slurped strings: 2.599 MiB.
- Node trees: 87.158 MiB.
- Root zippers retaining those trees: 87.191 MiB.
- Final outline values: 1.493 MiB.

Consequences:

- The node representation retained about **44.7 heap bytes per source byte** on the 100-file corpus and **46.3×** for `intent_transaction.clj`.
- Node versus zipper differed by only about 348 bytes/file at the medians, below the representation-trial variation. A root zipper adds no meaningful repository-scale resident cost beyond its tree.
- Final outline values were about **58× smaller than node trees**, but 14.9 KiB/file still becomes roughly 149 MiB at 10,000 files if all results are retained.
- The large file confirms that “MB per file” is the wrong scheduling unit: its tree was 5.59 MiB while the average sampled tree was 0.87 MiB.

Noise:

- Idle after-GC jitter was 8,848 bytes across the JVM.
- Data/JIT/interning warm-up produced as much as 454,488 bytes total spread across the five 100-tree trials, or about 4.4 KiB/file. I therefore report medians.
- `System/gc` is advisory; production peak sizing still needs continuous sampling. In particular, these numbers do not measure the current outline’s transient double-parse peak.

No server was started, no prohibited port was contacted, JVMs ran sequentially with explicit `-Xmx`, and the worktree has no tracked changes.

## What current `origin/main` retains

The source audit explains the current risk precisely:

- `ls-tree` first realizes sorted vectors of paths, then `pmap`s every file, realizes all results with `group-by`, and attaches every outline to the project collection before formatting. Both text and EDN formatters then traverse the complete retained outline set; the text formatter also builds one complete `StringBuilder`. [core.clj:202–250](/home/forge/tmp/sol/memwt/src/clj_surgeon/core.clj:202), [core.clj:321–339](/home/forge/tmp/sol/memwt/src/clj_surgeon/core.clj:321), [core.clj:369–402](/home/forge/tmp/sol/memwt/src/clj_surgeon/core.clj:369), [core.clj:463–481](/home/forge/tmp/sol/memwt/src/clj_surgeon/core.clj:463)

- Outline currently parses once through `cwalk/top-level-forms`, temporarily builds records containing exact `:source` for every form, then parses the entire source again to find namespace data. The public projection removes each form’s `:source`, which is why final retained outlines are small even though transient working heap is not. [walk.clj:86–96](/home/forge/tmp/sol/memwt/src/clj_surgeon/cljc/walk.clj:86), [outline.clj:225–273](/home/forge/tmp/sol/memwt/src/clj_surgeon/outline.clj:225), [outline.clj:275–307](/home/forge/tmp/sol/memwt/src/clj_surgeon/outline.clj:275), [outline.clj:309–315](/home/forge/tmp/sol/memwt/src/clj_surgeon/outline.clj:309)

- CLI extraction walks the complete project, slurps each matching source, and realizes a `sorted-map` before invoking the planner. [extract.clj:447–479](/home/forge/tmp/sol/memwt/src/clj_surgeon/extract.clj:447)

- The shared MCP workspace reader likewise maps canonical paths to slurped strings and realizes the complete workspace as a sorted map. Both extraction planning and extraction execution call it. [mcp_workspace_sources.clj:11–19](/home/forge/tmp/sol/memwt/src/clj_surgeon/mcp_workspace_sources.clj:11), [mcp_extraction_plan.clj:118–149](/home/forge/tmp/sol/memwt/src/clj_surgeon/mcp_extraction_plan.clj:118), [mcp_tool.clj:310–423](/home/forge/tmp/sol/memwt/src/clj_surgeon/mcp_tool.clj:310)

- The generic transaction reads every scoped file into a map. Compilation retains full `:original-sources`, full `:future-sources`, and the concatenated diff; commit keeps both maps because rollback currently restores from `:original-sources`. [intent_transaction.clj:1880–1890](/home/forge/tmp/sol/memwt/src/clj_surgeon/intent_transaction.clj:1880), [intent_transaction.clj:1394–1473](/home/forge/tmp/sol/memwt/src/clj_surgeon/intent_transaction.clj:1394), [intent_transaction.clj:2003–2026](/home/forge/tmp/sol/memwt/src/clj_surgeon/intent_transaction.clj:2003), [intent_transaction.clj:2175–2274](/home/forge/tmp/sol/memwt/src/clj_surgeon/intent_transaction.clj:2175)

- Receipts deliberately omit whole original/future files, but inverse edit strings and created-file content can still grow with the admitted change. [intent_transaction.clj:2312–2359](/home/forge/tmp/sol/memwt/src/clj_surgeon/intent_transaction.clj:2312)

- The existing durable state location is not project-local `.clj-surgeon`; receipts live under the workspace-digested `~/.local/state/clj-surgeon/workspaces/...` hierarchy. The cache, manifests, and transaction CAS should use that same state root. [mcp_workspace.clj:40–52](/home/forge/tmp/sol/memwt/src/clj_surgeon/mcp_workspace.clj:40)

There is no `clj-surgeon.study`, `alias_migration`, `relation_census`, or fold-diff implementation in this `origin/main` source tree. The similarly named study skill is not the operation. I therefore cannot honestly attach current-main source citations to the supplied branch facts about frozen alias reads or census filtering.

## Committed design

### Retention and pre-image

Each operation gets an on-disk, sorted manifest:

```clojure
{:path-id …
 :relative-path …
 :file-identity …
 :source-bytes …
 :sha256 …
 :kind :regular}
```

Heap retains only:

- Active per-worker source and parse state.
- Hashes and integer spans for files represented in the plan.
- A bounded compact relation/index buffer.
- Staged replacement references.
- A bounded receipt projection and merge buffers.

Source strings, repository-wide path maps, node trees, zippers, rollback bytes, and old/new whole-file projections do not remain resident.

For every write:

1. Read and hash the planning bytes as `H0`.
2. Emit edits/facts and drop the source/tree.
3. Reread under `H0` while staging the future file.
4. Pin exact pre-image bytes into a content-addressed transaction journal before any live mutation.
5. Acquire the project transaction lock.
6. Revalidate the complete semantic read set and scope membership.
7. Recheck each write-set hash immediately before atomic replacement.
8. Read back the result hash.
9. On failure, restore the pinned pre-images and verify every `H0`.

Thus “hash at discovery, reverify at write” is acceptable as **optimistic serializability with conflict detection and exact rollback**. It is not arbitrary-writer snapshot isolation: a noncooperating editor can race between sequential validations, and multi-file renames are not instantaneously atomic to unrelated readers.

The correctness cost is another full hash pass over the semantic read set, scope-membership validation, more conflict refusals, disk staging, and durable journal I/O. Hashing only the write set is insufficient when callers, aliases, or dependencies from other files influenced the plan.

New receipts should be versioned and reference immutable CAS objects rather than embed large inverse strings. Referenced pre-images cannot be evicted while a receipt remains undoable; if disk quota cannot pin them, the mutation refuses before writing.

### Streaming and cross-file knowledge

Use a bounded NIO walker, not `file-seq` into a collection:

```text
visit/count entry → confine → enforce stat and actual-byte limits
→ hash/decode → parse once → emit compact facts → drop source/tree
```

The queue contains at most `2 × workers` path descriptors, never source strings or ASTs.

The smallest resident cross-file facts are integer-coded records:

- Namespace/path/hash/declaration span.
- Definition ID/path/span.
- Alias, refer, or import target/path/span.
- Relevant reference/caller target/path/span.
- Dependency edge IDs and kind.

Known-target operations use two passes: determine the target/alias key set, then rescan and emit only matching facts. When the index buffer fills, flush sorted runs to operation scratch and merge them. Mutations must refuse if their complete plan or receipt cannot fit; read-only results may paginate against a manifest digest.

### Parser and zipper posture

Direct node traversal remains the safest initial read-only implementation because it preserves comments, reader conditionals, and exact positions. But the measurement says it is not a resident-memory improvement over a root zipper: both retain the same 44.7–46.3× tree.

Therefore:

- Never store a node or zipper in a result, future, index, or receipt.
- At most one AST-bearing file per admitted worker.
- Use node traversal for outline/dependency/census projection.
- Create zipper locations only while planning an edit in the active file.
- Reparse write-set files during staging under `H0`.
- Evaluate `tools.reader` only behind projection-parity tests and measured heap/peak gates.

Token/node and nesting ceilings must be enforced by a bounded lexical preflight or parser instrumentation before constructing an unbounded full tree. Counting nodes only after `parse-string-all` is too late to prevent OOM.

### Ceilings

Caller parameters may lower, never raise, server policy.

| Contract | Request may lower | Server hard cap |
|---|---:|---:|
| Matching files | Yes | Yes |
| Per-file bytes | Yes | Yes |
| Aggregate actual bytes | Yes | Yes |
| Result/receipt records and bytes | Yes | Yes |
| Modified files and staged bytes | Yes | Yes |
| Walk entries and depth | Yes | Yes |
| Syntax tokens/nodes and nesting | No | Yes |
| Heap work reserve and workers | No | Yes |
| Cache, scratch, journal disk | No | Yes |

Explicit files pass through the same gates as discovered files. Walk accounting includes directories, symlinks, nonmatches, and unreadable entries. Aggregate accounting uses actual bytes read, not only `stat`.

Every refusal names the observed boundary and a concrete narrowing call:

```clojure
{:error-type :memory-limit-exceeded
 :limit {:kind :aggregate-bytes
         :requested …
         :server-max …
         :observed-at-least …}
 :complete false
 :source-unchanged true
 :next_call {:op …
             :scope {:root "src/component-a"}}}
```

The remedy narrows root, include set, closure, depth, or continuation range. It never says merely “increase heap.”

### Parallelism

The measured 44.7× node factor is a lower bound for an active parser, not a safe pool coefficient: current outline transiently combines form-source projections with a second parse.

Use measured per-operation, per-byte-bucket p99 peaks:

```text
worker-reserve(op, bucket) =
  fixed-p99
  + peak-bytes-per-source-byte-p99 × bucket
  + per-file projection cap

workers =
  min(cpu-cap,
      server-cap,
      floor((work-budget - receipt - merge - emergency reserves)
            / worker-reserve))
```

A weighted semaphore admits workers by bucket. Unknown profiles start at one worker.

For intuition, a 2 MiB file at the measured 44.7× resident factor already implies about 89 MiB for one tree. Two such trees consume about 178 MiB before source, output, double-parse, and emergency reserves, so a 192 MiB work budget must initially admit only one until actual p99 peak data proves otherwise.

### Cache and fold-diff

Store immutable outline/index projections under the existing workspace state root, keyed by source hash plus parser/schema/options versions. Never cache source, nodes, zippers, or executable serialized objects.

Hashing remains mandatory; cache hits avoid parsing, not validation. Use a second-hit policy and retain only buckets where measured parse time materially exceeds cache read/decode time. Small one-shot files should be reparsed. Cache quota and transaction-journal quota are separate.

Fold-diff should emit sorted, length-delimited canonical relation records and merge two cursors. Only the current left/right record and one bounded output batch remain resident. A relation digest can skip equal groups, but direct sorted merge is authoritative. Legacy monolithic EDN gets a hard byte ceiling and one-time streaming conversion. This is a downstream plan because fold-diff is not implemented in this repository.

## Ordered implementation plan

Each item is a separate LID leaf and must proceed HLD → owning design → EARS → red witness → code, with the mandated review stop after each phase. These IDs are proposals, not entries already present in the tree.

1. **Meter first — `MCP-OP-MEM-001`.**  
   EARS: “When a tree-scale operation terminates, clj-surgeon shall report process-wide sampled heap peak, attributable reserved peak, files, bytes, largest file, records, workers, spill, journal, and cache work in a bounded receipt.”  
   Fail-first ceiling witness: with reservations summing exactly to configured work budget `B`, admission succeeds and reports `reserved_peak=B`; `B+1` refuses. This dynamically exercises the configured ceiling rather than asserting a constant.

2. **Add unified admission to existing whole-workspace reads — `MCP-OP-MEM-002`.**  
   EARS: “When discovery reaches any configured file, byte, entry, or depth ceiling, clj-surgeon shall admit work exactly through that ceiling and refuse the next unit before parsing or mutation with a narrowing `next_call`.”  
   Witness: sources whose actual bytes total exactly aggregate ceiling `L` succeed, including an explicitly requested last file; adding one byte refuses before its slurp/parse observer runs.

3. **Stream `ls-tree` to its output budget — `MCP-OP-MEM-003`.**  
   EARS: “When `ls-tree` scans a repository, it shall consume and discard each outline while retaining no more than the active worker set and bounded output encoder.”  
   Witness: an encoded result exactly at result ceiling `R` is complete; one additional record yields a valid continuation/refusal and the 10,000-file retained-heap delta remains within the pass line.

4. **Replace `workspace-sources/read-all` with a streaming fold/spill API — `MCP-OP-MEM-004`.**  
   EARS: “When extraction or another cross-file operation requires workspace facts, it shall retain compact facts or sorted spill runs rather than canonical-path-to-source maps.”  
   Witness: an index buffer exactly filling reserve `I` completes without premature refusal; the next record spills, and merged output equals the unbounded reference byte-for-byte.

5. **Introduce bounded lexical/parser admission — `MCP-OP-MEM-005`.**  
   EARS: “Before allocating a full rewrite-clj tree, clj-surgeon shall reject inputs whose lexical node estimate or nesting exceeds the server parser ceiling.”  
   Witness: fixtures with exactly node/token ceiling `N` and depth `D` parse and project identically; `N+1` or `D+1` refuses before the full-tree constructor is invoked.

6. **Move transaction originals/futures to the disk journal — `MCP-OP-MEM-006`.**  
   EARS: “When a mutation is staged, clj-surgeon shall pin each write-set pre-image and future file durably by digest before live mutation and shall not retain repository-wide original or future source maps.”  
   Witness: staged plus rollback bytes exactly equal disk quota `Q`, the last injected write fails, and every file is restored to `H0`; `Q+1` refuses with zero writes.

7. **Validate the complete semantic read set — `MCP-OP-MEM-007`.**  
   EARS: “Before the first write, clj-surgeon shall hold the transaction lock and revalidate every file and scope-membership fact that influenced the plan.”  
   Witness: with exactly maximum read-set count `F`, changing the final read-only caller or adding the final scope member causes zero writes and a typed stale-read-set refusal.

8. **Admit parser workers by measured reservation — `MCP-OP-MEM-008`.**  
   EARS: “When parse work is scheduled, clj-surgeon shall acquire byte-bucketed reservations so active workers and buffers cannot exceed the operation work budget.”  
   Witness: two reservations whose sum is exactly `B` run concurrently; a third remains unstarted until release, and cancellation releases its full weight.

9. **Add the content-addressed projection cache — `MCP-OP-MEM-009`.**  
   EARS: “When an identical projection is requested for a second time, clj-surgeon may reuse a checksummed version-compatible cached projection while still hashing current source.”  
   Witness: entries totaling exactly cache quota `C` remain usable; the next insertion evicts before exceeding `C`, and corruption behaves as a miss with identical output.

10. **Convert fold-diff to sorted streaming records — `MCP-OP-MEM-010`.**  
    EARS: “When two projections are compared, the diff shall merge sorted canonical streams without holding both projections and shall bind any continuation to both projection digests.”  
    Witness: differences encoding exactly result ceiling `R` return complete; one more relation returns the correct continuation, and resume produces the same ordered diff as the unbounded reference.

11. **Make the memory battery a release gate — `MCP-OP-MEM-011`.**  
    EARS: “When the tree-scale battery runs at its configured maximum N and work budget, every operation shall preserve reference outputs, avoid OOM, stay within reservations, and refuse before mutation when over budget.”  
    Witness: at exactly 10,000 files, `-Xmx512m`, and a 192 MiB operation reserve, the full battery satisfies the numeric pass line below.

The release pass line remains:

- `reserved_peak ≤ 192 MiB`.
- Process heap peak ≤ `min(start + 224 MiB, 0.80 × Xmx)`; about 410 MiB at 512 MiB.
- 10,000-file peak ≤ 1,000-file peak + 32 MiB.
- 10,000-file after-GC retained heap ≤ 1,000-file value + 8 MiB.
- Exact output/hash parity with the unbounded reference.
- The 450 × 1.9 MiB case either refuses with under 32 MiB heap growth or completes under a larger aggregate-I/O allowance without raising the heap line.
- Every injected conflict yields zero writes or verified restoration to every `H0`.

## What changed, and what did not

Changed by measurement and source inspection:

- “Zippers cost 0.55 MB/file” is rejected as an attribution. The retained node tree dominates; a root zipper was indistinguishable at this scale.
- The memory coefficient is about 45 heap bytes per source byte on this corpus, not a fixed amount per file.
- Current final outlines retain about 14.9 KiB/file here, not 0.55 MB/file. The earlier 618 MB result remains valid for its code/corpus/peak methodology, but it is not explained by current-main final outline values.
- Parser replacement moved behind streaming, lifetime, and peak controls because node-without-zipper alone produces no meaningful resident win.
- The durable cache/journal belongs beside the existing workspace state, not in a new project-local cache.
- Study, alias-migration, census, and fold-diff branch implementations are absent from this main checkout and cannot be cited as current code.

Unchanged:

- Optimistic read-set validation rather than pretending hashes create arbitrary-writer snapshots.
- Exact rollback bytes pinned on disk before mutation.
- One source/tree per worker, compact cross-file indexes, and spillable sorting.
- Hard aggregate, walk, depth, parser, output, staged-byte, and disk ceilings.
- Worker count derived from measured memory, never CPU count alone.
- Hash-keyed immutable caches and streaming fold-diff.
- No 4 GiB “solution,” no source-retaining rollback map, no explicit-file bypass, no silent transactional truncation, and no lazy sequence allowed to retain a walker or AST.