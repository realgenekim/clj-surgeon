# Memory battery: the RED baseline on main (2026-09-03T04:44Z)

`make memory-battery` at `4ec01da` (= `origin/main`), on Anvil, `-Xmx512m`, 5 reps,
N = 100 / 1,000 / 10,000, four arms. **Exit 1 — FAIL (INCOMPLETE).**

Receipt: `/home/forge/tmp/membat/receipts/20260903T044440.111653040Z-battery.edn`.
Unbounded reference (`-Xmx4g`): `20260903T043939.516042311Z-reference.edn`.
Intents: `MCP-OP-MEM-001`, `MCP-OP-MEM-011` (`docs/intent/memory-boundedness/`).
Host: 16 cores, 30 GB, load average 6–11 from other tenants throughout; JVM 21.0.12.

Trees: 100 / 1,000 / 10,000 files, mean 4,047 B/file, largest 15,150 B,
40,472,773 B at N=10,000. All three built in **1.13 s wall** (10,000 files in
973 ms).

---

## The table, verbatim

```
memory battery — Xmx 512m, pass lines {:reserved-peak-mb 192, :peak-headroom-mb 224, :peak-xmx-percent 80, :scale-peak-slack-mb 32, :scale-retained-slack-mb 8, :scale-small-n 1000, :scale-large-n 10000}
peak_mb = continuously sampled process-wide used-heap PEAK (not a post-GC delta); held_mb = after-GC used heap while the result is still referenced, minus start (the receipt's retained size); afterGC_mb = after-GC used heap once the result is released (leak check).
---------------------------------------------------------------------------------------------------------------
op                              N  phase  wall_ms  peak_mb  held_mb afterGC_mb  files      bytes  OOM?  verdict
---------------------------------------------------------------------------------------------------------------
cli-ls-tree                   100  fresh      169    193.2      0.5       23.8    100     404332    no       ok
cli-ls-tree                   100   warm      160    192.1      0.9       23.8    100     404332    no       ok
cli-ls-tree                  1000  fresh      763    274.8      9.6       23.9   1000    4045282    no     FAIL
cli-ls-tree                  1000   warm      835    301.3      9.5       23.9   1000    4045282    no     FAIL
cli-ls-tree                 10000  fresh     6934    418.3     94.0       24.2  10000   40472773    no     FAIL
cli-ls-tree                 10000   warm     7547    433.2     93.6       24.3  10000   40472773    no     FAIL
rename-ns-plan-full-match     100  fresh      128     73.3      0.1       24.3    100     404332    no       ok
rename-ns-plan-full-match     100   warm      133     73.8      0.1       24.3    100     404332    no       ok
rename-ns-plan-full-match    1000  fresh     1240    195.0      1.0       24.3   1000    4045282    no       ok
rename-ns-plan-full-match    1000   warm     1227    195.5      1.0       24.3   1000    4045282    no       ok
rename-ns-plan-full-match   10000  fresh    11471    203.7      9.8       24.3  10000   40472773    no       ok
rename-ns-plan-full-match   10000   warm    11892    203.5      9.8       24.5  10000   40472773    no       ok
rename-ns-plan-narrow         100  fresh      399     75.6      0.1       24.3    100     404332    no       ok
rename-ns-plan-narrow         100   warm      138     73.6      0.1       24.3    100     404332    no       ok
rename-ns-plan-narrow        1000  fresh     1264    195.4      0.1       24.3   1000    4045282    no       ok
rename-ns-plan-narrow        1000   warm     1274    195.0      0.2       24.3   1000    4045282    no       ok
rename-ns-plan-narrow       10000  fresh    11604    196.5      0.1       24.3  10000   40472773    no       ok
rename-ns-plan-narrow       10000   warm    11600    196.7      0.1       24.3  10000   40472773    no       ok
workspace-sources-read-all    100  fresh       25     29.2      0.4       24.3    100     404332    no       ok
workspace-sources-read-all    100   warm        8     29.7      0.4       24.3    100     404332    no       ok
workspace-sources-read-all   1000  fresh       43     72.2      4.1       24.3   1000    4045282    no       ok
workspace-sources-read-all   1000   warm       44     73.2      4.1       24.3   1000    4045282    no       ok
workspace-sources-read-all  10000  fresh      468    199.8     40.7       24.3  10000   40472773    no       ok
workspace-sources-read-all  10000   warm      476    203.8     41.0       24.3  10000   40472773    no       ok
---------------------------------------------------------------------------------------------------------------
verdict: FAIL (INCOMPLETE)   exit 1
  FAIL peak-over-budget {:op :cli-ls-tree, :n 1000, :phase :fresh, :observed 274.8, :limit 247.8}
  FAIL peak-over-budget {:op :cli-ls-tree, :n 1000, :phase :warm, :observed 301.3, :limit 247.8}
  FAIL peak-over-budget {:op :cli-ls-tree, :n 10000, :phase :fresh, :observed 418.3, :limit 247.9}
  FAIL peak-over-budget {:op :cli-ls-tree, :n 10000, :phase :warm, :observed 433.2, :limit 248.2}
  FAIL peak-scales-with-n {:op :cli-ls-tree, :observed 433.2, :limit 333.3, :small-n-observed 301.3, :slack-mb 32}
  FAIL peak-scales-with-n {:op :workspace-sources-read-all, :observed 203.8, :limit 105.2, :small-n-observed 73.2, :slack-mb 32}
  UNMEASURED reserved-peak-over-budget {:op :cli-ls-tree, :detail "no operation on this branch reports an attributable reserved peak; the sampled process-wide peak is not a substitute"}
  UNMEASURED reserved-peak-over-budget {:op :rename-ns-plan-full-match, :detail "..."}
  UNMEASURED reserved-peak-over-budget {:op :rename-ns-plan-narrow, :detail "..."}
  UNMEASURED reserved-peak-over-budget {:op :workspace-sources-read-all, :detail "..."}
```

## Which line each op fails on

| arm | verdict | line failed | site |
|---|---|---|---|
| `cli-ls-tree` | **FAIL** | `peak-over-budget` at N=1,000 and N=10,000 (301.3 and 433.2 MB against a 248 MB budget), **and** `peak-scales-with-n` (433.2 vs 333.3) | `core.clj:202-250` discovery, `321-339` `outline-all-files` `pmap` into one vector, `463-481` `run-ls-tree` |
| `workspace-sources-read-all` | **FAIL** | `peak-scales-with-n` (203.8 vs 105.2) | `mcp_workspace_sources.clj:11-20`; `extract.clj:447-479` repeats the shape inline |
| `rename-ns-plan-narrow` | ok on every measured line | — | `rename.clj:110-160` |
| `rename-ns-plan-full-match` | ok on every measured line | — | same |

No `oom`. No `reference-mismatch`: every bounded result hashed identically to
the `-Xmx4g` reference at every N, for all four arms. `reserved-peak-over-budget`
is UNMEASURED for all four (no admission accountant exists), which is why the
verdict prints `(INCOMPLETE)`.

## What the numbers say

**Retention is dead linear in N for three of four arms.** `held_mb` — after-GC
used heap while the result is still referenced — is the sharpest instrument here:

| arm | held at 100 | at 1,000 | at 10,000 | heap per file | heap per source byte |
|---|---:|---:|---:|---:|---:|
| `cli-ls-tree` | 0.9 | 9.5 | 93.6 MB | 9.4 KB | 2.3 |
| `workspace-sources-read-all` | 0.4 | 4.1 | 41.0 MB | 4.1 KB | **1.01** |
| `rename-ns-plan-full-match` | 0.1 | 1.0 | 9.8 MB | 1.0 KB | 0.24 |
| `rename-ns-plan-narrow` | 0.1 | 0.2 | 0.1 MB | ~0 | ~0 |

`read-all` retaining **1.01 heap bytes per source byte** is the instrument
calibrating itself: a `sorted-map` of compact-string ASCII sources should cost
almost exactly the bytes on disk, and it does. That number is the best evidence
the `held_mb` measurement is sound.

**Against Sol's measured coefficients.** Sol reports final outlines at ≈ 14.9
KiB/file over this repository's own sources (mean 17 KB/file). This battery
measures 9.4 KB/file over 4.0 KB/file synthetic sources — 2.3 heap bytes per
source byte against Sol's ≈ 0.88. The direction is consistent and the gap is
explained: outline cost is per *form*, not per byte, and the synthetic files are
form-dense (a def, three helpers, a public entry, a three-arm multimethod, plus
padding helpers in every file). Nothing here contradicts Sol's numbers.

**The peak column above ~200 MB is GC scheduling, not live data.** `cli-ls-tree`
at 10,000 files peaks at 433 MB while holding 94 MB — ~340 MB of that peak is
uncollected garbage. With rewrite-clj at ≈ 45 heap bytes per source byte, the run
allocates on the order of 1.8 GB in total and the collector simply lets a few
hundred MB accumulate before running. That is a real consequence of the 512m
budget and it is exactly what the `peak-over-budget` line is for, but it means
`peak_mb` should not be read as "live data at the high-water mark". `held_mb` is
the number to compare when reasoning about what an operation retains.

## Two honest limits of this baseline

1. **The published pass lines do not catch a *small-constant* O(N) receipt.**
   `rename-ns-plan-full-match` retains 1.0 KB per file — unambiguously linear in
   N (0.1 → 1.0 → 9.8 MB per decade) — and passes every line, because 8.8 MB of
   growth fits inside a 224 MB peak headroom, and the `retained-scales-with-n`
   line is measured *after the result is released* and so sees only leaks
   (24.3 → 24.3 MB). Sol's set catches operations that are large and linear; it
   does not catch operations that are small and linear. If the intent is "not
   O(N) at all", a line on `held_mb` growth per decade is the one to add. Flagged
   for the kernel builder rather than invented here: this battery implements
   Sol's constants verbatim and does not add its own.

2. **`rename-ns-plan-narrow` would have reported a false `ok`.** Its prefix
   matches only 100 of 10,000 files, so the plan stays tiny at every N while the
   walk still parses everything. The `full-match` arm was added for exactly this
   reason. Every future arm needs the query shape that makes its result grow with
   N, or the battery grades the query instead of the operation.

## Reproduce

```bash
make memory-battery            # ~7 min at these scales on a loaded 16-core box
```

Wall-clock, worst case per fresh rep: `cli-ls-tree` 6.9 s, `rename-ns-plan-*`
11.5 s at N=10,000. Nothing came close to the 10-minute `MEMBAT_OP_TIMEOUT_MS`,
so the full 10,000-file case ran for every arm and nothing was skipped.
