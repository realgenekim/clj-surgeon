# Memory design — Sol-2 and Opus reconciled (2026-09-03T04:37Z)

Both consults measured independently (Sol: 44.7 heap bytes per source byte, outlines 14.9 KiB/file; Opus:
48.4×, 12.7–13.5 KB/file) and reached the same structure. Where they differ, the ruling and why:

| topic | Sol-2 | Opus | ruling |
|---|---|---|---|
| zipper vs node | no resident win; keep nodes for read-only safety | same, 1.2% | agreed — no zipper→node refactor |
| the missing control | unified admission (files, bytes, entries, depth) as MEM-002 | max_aggregate_bytes from File.length() during the walk, refuse before parse | same thing; Opus's evidence (450 × 1.9 MB = 855 MB passed both per-file ceilings) is the OOM's cause; MEM-002 is the fix; q5z's bounded scope walker is the host |
| outline double parse | noted as the reason 44.7× is a lower bound | measured: 21% wall, 31% allocation; :source built then dissoc'd, 76 MB garbage per 52 KB | new leaf **MEM-015** (single parse, no discarded :source), byte-identical outlines as the witness — cheapest win, ships first |
| pre-image | pinned to a disk journal under the workspace state root before mutation; read-set revalidation under a lock (MEM-006/007) | same, plus "hash at discovery is a GAIN except for rollback, which moves to disk" | agreed; B1 is building it |
| parser fast path | behind streaming/lifetime/peak controls | core reader 9× faster, 128 MB at 10k, 0.6% fallbacks after priming, differential gate | agreed as step 9 (last); Opus's priming recipe and the 151/158 residuals go into the leaf |
| pool | weighted semaphore by byte bucket from p99 reservation; unknown profile → 1 worker; 2 MiB file ≈ 89 MiB tree | budget formula, factor 200/40, cap 4; **2 MiB per-file ceiling exceeds one thread's budget at 512m** | both say the same number: the per-file ceiling must drop (~512 KiB) or be admitted one-at-a-time; log the derivation; Sol's semaphore is the mechanism, Opus's formula the default |
| cache | hash still mandatory, second-hit policy, only where parse ≫ read | same; never cache hashes; corrupt = miss | agreed |
| fold-diff | sorted length-delimited records, two cursors; downstream (other repo) | digest pass then materialise changed only | agreed; curtain-call lane, after round 3 |
| battery pass line | reserved_peak ≤ 192 MiB; peak ≤ min(start+224, 0.8×Xmx); 10k ≤ 1k + 32 MiB peak / +8 MiB retained; parity | every read-only op at 10k in 512m; retained/N ≤ 1.5× N=100; min-Xmx(10k) ≤ 2× min-Xmx(1k); no -Xmx by judgement | union: Sol's numeric lines are the gate (MEM-011); Opus's min-Xmx ladder is added as the capacity row; **Opus's G1 caveat goes into the receipt field's doc** (peak-used depends on -Xmx; it is a trend, the min-Xmx ladder is the requirement) |
| receipt | MEM-001 bounded receipt: sampled heap peak, reserved peak, files, bytes, largest, records, workers, spill, journal, cache | `resources` block always on + reader_fallbacks + cache hits/misses | union of fields under MEM-001 |

Lanes: **B1** (running) = MEM-006/007/012–014 kernel, TDD from the reproduced OOM. **Battery** (running) = MEM-001/011.
**B2 read-path** (next) = MEM-015 single parse + MEM-001 receipt on ls_tree/census; then MEM-002 aggregate admission
once q5z round 3 lands (its walker owns the scope). **B3 adoption** = alias_migration/extract on the kernel, gated by
the battery. Ordering follows Opus's "smallest measurable win first"; ids follow Sol's registry proposal.
