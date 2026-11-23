# Memory-vs-files design — Opus consult (executed, scratch clone at 1673eaa, corpus 10,000 files cycled from 158 real ones, mean 17.3 KB, max 90 KB)

Method: retention = totalMemory − freeMemory after 4× System/gc (artifact pinned in an atom); allocation = ThreadMXBean.getThreadAllocatedBytes (deterministic); peak = 5 ms sampler; capacity = a real JVM at fixed -Xmx catching OutOfMemoryError, one at a time. Noise ±3% above 1 MB, ±5 KB absolute on small arms. Caveat that matters: **peak-used-heap is heap-size dependent under G1** (identical 1,000-file outline pass: 140 MB at -Xmx256m, 280 MB at -Xmx2g); peak is a trend signal; the requirement is the minimum -Xmx that completes.

## One 1,074-line file (52,665 bytes), k=30

| arm | retained KB | allocated KB | × source bytes |
|---|---:|---:|---:|
| slurp alone | 100 | 388 | 1.95 |
| parse-string-all node tree | **2,492** | 23,988 | **48.4** |
| zipper over it, root retained, walked | 2,502 | 43,569 | 48.6 |
| one zloc per top-level form (what cwalk returns) | 2,521 | 43,571 | 49.0 |
| top-level-form-records (holds :source) | 87 | 52,398 | 1.69 |
| outline-source output | 33 | 76,300 | 0.64 |
| core-reader outline (no rewrite-clj) | 17 | 2,375 | 0.32 |

**The zipper is not the cost.** The node tree is 48× source; a walked zipper root adds 0.4%, one zloc per form 1.2%. The "zippers are the 0.55 MB/file" premise was wrong by two orders of magnitude. Second headline: `outline` **allocates 76 MB of garbage per 52 KB file** (1,450×) because it parses twice and builds a `:source` string per form that it then dissocs.

## 100-file tree (1.97 MB), -Xmx3g

| arm | wall | retained/file |
|---|---:|---:|
| A hold source + node tree (old ls_tree shape) | 670 ms | **908 KB** |
| B outline, retain outlines | 1,676 ms | 13.5 KB |
| C core reader, retain projections | **176 ms** | 12.2 KB |
| D hash + spans digest | 1,577 ms | 6.7 KB |

At 1,000 files: B 12.7 KB/file, D 4.6 KB/file. At 10,000 a thin index (path, hash, bytes, form-count) retains **0.2 KB/file — 2.0 MB total** (450× below A).

## Capacity — minimum -Xmx that completes

| arm | N=1,000 | N=10,000 (173 MB source) |
|---|---|---|
| A hold source + trees | OOM @768m, OK @1g (peak 971 MB) | **OOM @2g** |
| B outline retain | OK @128m, 11.7 s | — |
| D hash+spans (rewrite-clj) | OK @256m, 11.4 s | OK @512m, 108.6 s |
| C core reader + fallback | OK @128m, **1.35 s** | OK @128m, **12.4 s** |
| F thin index | — | OK @128m, 12.3 s |
| G slurp + SHA-256 only | — | OK @128m, 704 ms |
| H warm content-addressed cache | — | OK @128m, 662 ms |

## Pool sweep, N=1,000 — peak ≈ base + pool × k; k = 15.6 MB/thread (rewrite-clj), 2.8 MB/thread (reader); speedup saturates at pool 4

| pool | rewrite-clj | core reader |
|---|---|---|
| 1 | 11.4 s / 129 MB | 1.35 s / 69 MB |
| 4 | 4.6 s / 216 MB | 0.71 s / 73 MB |
| 8 | 3.8 s / 238 MB | 0.66 s / 88 MB |
| 16 | 3.3 s / 473 MB (clipped @512m) | — |

## Core reader viability (158 real files)
rewrite-clj threw 0; core reader threw 4 (2.5%), all auto-resolved keywords with unknown aliases; with alias priming (regex-scrape `:as`/`:as-alias` from the first 8 KB, create-ns + addAlias, no require) 1/158 (0.6%). Name-set identity 151/158 (residuals: defmethod multiplicity, .cljc reader-conditional double-yield). On 10,000: 63 fallbacks (0.63%).

## Micro-fix isolation, 1,000 files
outline-source (two parses) 11.30 s / 24.6 GB allocated → top-level-form-records only (one parse) 8.96 s / 17.0 GB: **the second parse costs 21% wall, 31% allocation.**

## Cache
10,000 warm lookups 662 ms at -Xmx128m (164× a cold rewrite-clj pass, 19× the cold reader). Corpus dedupes to 158 blobs — cache SIZE row is optimistic, latency and per-file numbers are not.

## Design (Opus)
1. Retention: resident always = {path, sha256, bytes, mtime-ns, ns-name, form-count} (0.2 KB/file); spans only for the touch set; never source, tree, zipper. Hash-at-discovery + re-verify-at-write costs 704 ms serial / 532 ms at pool 4 per 10,000 files in 128 MB and converts a silent clobber into a named refusal; ABA and TOCTOU are no worse than retention. The one real loss is rollback → **pre-image on disk** under a txn dir, content-addressed, bounded by the touch set; required, not optional.
2. Streaming: discover → per chunk read → project → drop → fold into an accumulator bounded by the receipt, never by N. Cross-file indexes store paths, hashes, symbols, spans — **never a tree or a source string**; alias index scoped to one require-hop; callers via raw-bytes prescan then parse candidates.
3. Do NOT replace zippers with bare nodes (1.2%). The lever is not building a rewrite-clj tree for read-only projections: core reader as a fast path with per-file fallback + `reader_fallbacks` counter, alias priming, *read-eval* false, sandbox ns, :read-cond :preserve, placeholder data readers, gated on a differential test (151/158 today).
4. Ceilings: request-lowerable paths/max_files/max_bytes_total/max_depth/budget_bytes; server max_file_bytes (2 MiB is currently WRONG, see 5), **max_aggregate_bytes (the missing one; explains the alias_migration OOM: 450 × 1.9 MB = 855 MB of source passed both per-file ceilings)**, max_walk_entries, pool, heap_headroom; refuse from File.length() during the walk, before any parse; refusal names limit + observed + count that would fit + executable next_call.
5. Pool = clamp(1, floor((budget − base − accumulator)/(factor × max_file_bytes)), min(cpus,4)); factor ≈200 rewrite-clj, ≈40 reader. At Xmx=512m/budget 256/max_file 2 MiB → 378 MB per thread: **one thread already exceeds the budget**; drop max_file_bytes to ~512 KiB or raise the heap; log the derivation at startup; cap at 4.
6. Cache: sha256(bytes)+projection-version, atomic writes, LRU by atime, corrupt = miss, never cache hashes (hashing is the key cost), never cache anything derived from mutable process state unless in the key.
7. fold-diff: digest per relation streamed in sorted path order (0.2 KB/file/side), sorted-merge → changed set → materialise only those; a file-hash match with a digest mismatch is projection nondeterminism → alarm; baseline written sorted line-delimited; -Xmx4g replaced by the battery number.
8. Receipt `resources` block on every tree-scale op, always on (files_scanned/read, bytes_read, heap_used_mb_peak with the G1 caveat in its doc, heap_max, pool, wall, reader_fallbacks, cache hits/misses). Battery N ∈ {100, 1k, 10k}, min -Xmx from {128m..2g}; pass lines: every read-only op completes at 10k in 512m; retained/N flat (≤1.5× the N=100 value); min-Xmx(10k) ≤ 2 × min-Xmx(1k); no -Xmx "by judgement".

Would NOT: replace zippers with nodes; make the core reader the only reader; raise -Xmx as the remedy; add mmap/off-heap; keep the pre-image in heap or delete rollback; use System/gc accounting as a gate; pool > 4 by default; a "streaming mode" flag (streaming is the only mode).

## Plan — smallest measurable win first
| # | step | witness |
|---|---|---|
| 1 | resource receipt on every tree-scale op | keys present and numeric on a fixture |
| 2 | aggregate-bytes ceiling + pre-read refusal with next_call | 450 × 1.9 MB scope refuses < 100 ms, files_read 0 |
| 3 | drop outline-source's second parse | 1,000 files 11.3 s/24.6 GB → 8.96 s/17.0 GB; byte-identical outlines on 158 files |
| 4 | stop building the discarded :source | allocation per 52 KB file 76 MB → toward 24 MB; outlines unchanged |
| 5 | convert the four retain-all sites to streaming folds (ls_tree discovery, census, extract!/rewire, alias_migration scope) | battery lines 1–3 at 10k; alias_migration plan-only completes at 512m on the OOM shape |
| 6 | pre-image to disk, hash-at-discovery / re-verify-at-write | injected concurrent modification refuses `pre_image_changed`; mid-write failure rolls back byte-identical |
| 7 | content-addressed outline cache | 662 ms warm vs 108.6 s cold; corrupt = miss; version bump misses all |
| 8 | streaming fold-diff | curtain-call -Xmx 4g → battery number |
| 9 | core-reader fast path + priming + fallback counter behind a differential gate | 158/158; 10k ls_tree 108.6 s → ~12 s at 128 MB |
| 10 | pool from the budget formula, logged, capped at 4 | peak ≈ base + pool × k within 15% |

Harnesses left for Sol to re-run: /home/forge/tmp/memcheck/dev/mem/{memprobe,treeprobe,cap,micro,agree}.clj; corpus /home/forge/tmp/corpus (173 MB).
