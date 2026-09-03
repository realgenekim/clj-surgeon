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

---

# Round 2 baseline — the instrument after Sol's review (2026-09-03T06:22Z)

`make memory-battery` at `a9d2d4b` on `bridge/memory-battery` (main merged at
`8075db0`), Anvil, `-Xmx512m`, 5 reps. **Exit 1 — FAIL (INCOMPLETE).**

This is the same measurement apparatus after the eight fixes from Sol's executed
instrument review (GO-WITH-FIX). **No operation was changed.** The numbers moved
only where a line moved: `held_mb` is now gated, the peak lines are now trends,
persistent growth is separated from result retention, and three adversarial
corpora were added.

Receipt: `/home/forge/tmp/membat/receipts/20260903T062227.526708549Z-battery.edn`.
Unbounded reference: `20260903T061511.008933081Z-reference.edn`, attested to
`{:head-sha "a9d2d4bcce94311a05d93b6e61e04775b1e65195", :jvm "21.0.12"}`.
Host: 16 cores, load average 8.07 from other tenants; JVM 21.0.12.

**The attestation fired twice in the field, unprompted.** The pre-existing
reference was refused as `unattested-reference`; after the last commit changed
`src/`, the rebuilt one was refused again as
`stale-reference {:fields [:src-digest]}` and rebuilt. Before this round, both
would have been used silently for output parity.

Corpora, all verified byte-for-byte before measurement (~2.1 s):
100 / 1,000 / 10,000 default files (40,472,773 B at N=10,000), plus
`cljc-100` (77,260 B), `giant-1` (1,992,594 B in ONE file), `nested-1`
(111,183 B, 300-deep, 20,000-token).

## The table, verbatim

```
memory battery — Xmx 512m, pass lines {:reserved-peak-mb 192, :peak-headroom-mb 224, :peak-xmx-percent 80, :scale-peak-slack-mb 32, :scale-retained-slack-mb 8, :scale-held-slack-mb 2.0, :scale-small-n 1000, :scale-large-n 10000}
peak_mb = continuously sampled process-wide used-heap PEAK (not a post-GC delta); held_mb = after-GC used heap while the result is still referenced, minus start (the receipt's retained size, INCLUDING any cache or leak the call created); excl_mb = held minus after-release, the result-EXCLUSIVE retention; grow_mb = after-release minus start, the PERSISTENT growth the call left behind (this is the gated leak figure); afterGC_mb = absolute after-GC used heap once the result is released.
TREND lines are reported, never gated: peak_mb is a sampled process-wide figure that G1 moves by tens of MB on identical work. HARD lines: oom, reference parity, reserved peak, held_mb across N, persistent growth across N.
-----------------------------------------------------------------------------------------------------------------------------------------
op                            prof      N  phase  wall_ms  peak_mb  held_mb  excl_mb  grow_mb afterGC_mb  files      bytes  OOM?  verdict
-----------------------------------------------------------------------------------------------------------------------------------------
cli-ls-tree                default    100  fresh      155    194.5      0.5      0.9      0.0       23.8    100     404332    no       ok
cli-ls-tree                default    100   warm      128    196.2      0.9      0.9      0.0       23.8    100     404332    no       ok
cli-ls-tree                default   1000  fresh      756    269.1      9.4      9.4      0.0       23.8   1000    4045282    no    trend
cli-ls-tree                default   1000   warm      755    287.9      9.5      9.5      0.1       23.9   1000    4045282    no    trend
cli-ls-tree                default  10000  fresh     9873    426.0     94.0     93.6      0.4       24.2  10000   40472773    no    trend
cli-ls-tree                default  10000   warm     7943    429.2     93.6     93.6      0.1       24.3  10000   40472773    no    trend
cli-ls-tree                   cljc    100  fresh       48     71.8      0.0      0.4      0.0       23.9    100      77260    no       ok
cli-ls-tree                   cljc    100   warm       42     70.7      0.4      0.4      0.0       23.8    100      77260    no       ok
cli-ls-tree                  giant      1  fresh     2058    386.4      3.1      3.1      0.0       23.8      1    1992594    no    trend
cli-ls-tree                  giant      1   warm     1975    376.5      3.2      3.2      0.0       23.8      1    1992594    no    trend
cli-ls-tree                 nested      1  fresh      834    264.7      0.0      0.0      0.0       23.8      1     111183    no    trend
cli-ls-tree                 nested      1   warm      784    285.7      0.0      0.0      0.0       23.8      1     111183    no    trend
rename-ns-plan-full-match  default    100  fresh      154     83.2      0.1      0.1      0.0       24.3    100     404332    no       ok
rename-ns-plan-full-match  default    100   warm      191    135.1      0.1      0.1      0.0       24.3    100     404332    no       ok
rename-ns-plan-full-match  default   1000  fresh     1252    193.9      1.0      1.0      0.0       24.3   1000    4045282    no       ok
rename-ns-plan-full-match  default   1000   warm     1347    195.5      1.0      1.0      0.0       24.3   1000    4045282    no       ok
rename-ns-plan-full-match  default  10000  fresh    11562    202.7      9.8      9.8      0.0       24.3  10000   40472773    no       ok
rename-ns-plan-full-match  default  10000   warm    11702    203.8      9.9      9.9      0.0       24.3  10000   40472773    no       ok
rename-ns-plan-full-match     cljc    100  fresh       33     66.8      0.1      0.1      0.0       24.3    100      77260    no       ok
rename-ns-plan-full-match     cljc    100   warm       33     72.8      0.1      0.1      0.0       24.3    100      77260    no       ok
rename-ns-plan-full-match    giant      1  fresh      909    259.8      0.0      0.0      0.0       20.1      1    1992594    no    trend
rename-ns-plan-full-match    giant      1   warm      851    294.5      0.0      0.0      0.0       20.1      1    1992594    no    trend
rename-ns-plan-full-match   nested      1  fresh       90     53.9      0.0      0.0      0.0       20.1      1     111183    no       ok
rename-ns-plan-full-match   nested      1   warm       87     57.8      0.0      0.0      0.0       20.1      1     111183    no       ok
rename-ns-plan-narrow      default    100  fresh      226    165.2      0.1      0.1      0.0       24.3    100     404332    no       ok
rename-ns-plan-narrow      default    100   warm      161    136.5      0.1      0.1      0.0       24.3    100     404332    no       ok
rename-ns-plan-narrow      default   1000  fresh     1329    196.9      0.1      0.1      0.0       24.3   1000    4045282    no       ok
rename-ns-plan-narrow      default   1000   warm     1251    194.7      0.1      0.1      0.0       24.3   1000    4045282    no       ok
rename-ns-plan-narrow      default  10000  fresh    11560    196.5      0.1      0.1      0.0       24.3  10000   40472773    no       ok
rename-ns-plan-narrow      default  10000   warm    11624    196.9      0.1      0.1      0.0       24.3  10000   40472773    no       ok
rename-ns-plan-narrow         cljc    100  fresh       41     73.8      0.1      0.1      0.0       24.3    100      77260    no       ok
rename-ns-plan-narrow         cljc    100   warm       30     72.3      0.1      0.1      0.0       24.3    100      77260    no       ok
rename-ns-plan-narrow        giant      1  fresh      908    250.5      0.0      0.0      0.0       24.3      1    1992594    no    trend
rename-ns-plan-narrow        giant      1   warm      871    314.3      0.0      0.0      0.0       24.3      1    1992594    no    trend
rename-ns-plan-narrow       nested      1  fresh       76     67.3      0.0      0.0      0.0       24.3      1     111183    no       ok
rename-ns-plan-narrow       nested      1   warm       89     72.8      0.0      0.0      0.0       24.3      1     111183    no       ok
workspace-sources-read-all default    100  fresh       18     30.0      0.4      0.4      0.0       23.8    100     404332    no       ok
workspace-sources-read-all default    100   warm        5     30.1      0.4      0.4      0.0       23.8    100     404332    no       ok
workspace-sources-read-all default   1000  fresh       46     72.8      4.4      4.1      0.3       24.1   1000    4045282    no       ok
workspace-sources-read-all default   1000   warm       39     71.8      4.3      4.1      0.2       24.3   1000    4045282    no       ok
workspace-sources-read-all default  10000  fresh      475    203.5     40.8     40.8      0.0       24.3  10000   40472773    no       ok
workspace-sources-read-all default  10000   warm      465    203.1     41.0     41.0      0.0       24.3  10000   40472773    no       ok
workspace-sources-read-all    cljc    100  fresh        3     25.8      0.1      0.1      0.0       24.3    100      77260    no       ok
workspace-sources-read-all    cljc    100   warm        3     26.8      0.1      0.1      0.0       24.3    100      77260    no       ok
workspace-sources-read-all   giant      1  fresh        4     26.5      2.0      2.0      0.0       24.3      1    1992594    no       ok
workspace-sources-read-all   giant      1   warm        5     29.0      2.0      2.0      0.0       24.3      1    1992594    no       ok
workspace-sources-read-all  nested      1  fresh        0     24.3      0.1      0.1      0.0       24.3      1     111183    no       ok
workspace-sources-read-all  nested      1   warm        0     24.3      0.1      0.1      0.0       24.3      1     111183    no       ok
-----------------------------------------------------------------------------------------------------------------------------------------
verdict: FAIL (INCOMPLETE)   exit 1
  FAIL held-scales-with-n {:op :cli-ls-tree, :profile :default, :observed 94.0, :limit 11.5, :small-n-observed 9.5, :slack-mb 2.0}
  FAIL held-scales-with-n {:op :rename-ns-plan-full-match, :profile :default, :observed 9.9, :limit 3.0, :small-n-observed 1.0, :slack-mb 2.0}
  FAIL held-scales-with-n {:op :workspace-sources-read-all, :profile :default, :observed 41.0, :limit 6.4, :small-n-observed 4.4, :slack-mb 2.0}
  TREND peak-over-budget {:op :cli-ls-tree, :n 1000, :phase :fresh, :profile :default, :observed 269.1, :limit 247.8}
  TREND peak-over-budget {:op :cli-ls-tree, :n 1000, :phase :warm, :profile :default, :observed 287.9, :limit 247.8}
  TREND peak-over-budget {:op :cli-ls-tree, :n 10000, :phase :fresh, :profile :default, :observed 426.0, :limit 247.8}
  TREND peak-over-budget {:op :cli-ls-tree, :n 10000, :phase :warm, :profile :default, :observed 429.2, :limit 248.2}
  TREND peak-over-budget {:op :cli-ls-tree, :n 1, :phase :fresh, :profile :giant, :observed 386.4, :limit 247.8}
  TREND peak-over-budget {:op :cli-ls-tree, :n 1, :phase :warm, :profile :giant, :observed 376.5, :limit 247.8}
  TREND peak-over-budget {:op :cli-ls-tree, :n 1, :phase :fresh, :profile :nested, :observed 264.7, :limit 247.8}
  TREND peak-over-budget {:op :cli-ls-tree, :n 1, :phase :warm, :profile :nested, :observed 285.7, :limit 247.8}
  TREND peak-over-budget {:op :rename-ns-plan-narrow, :n 1, :phase :fresh, :profile :giant, :observed 250.5, :limit 248.3}
  TREND peak-over-budget {:op :rename-ns-plan-narrow, :n 1, :phase :warm, :profile :giant, :observed 314.3, :limit 248.3}
  TREND peak-over-budget {:op :rename-ns-plan-full-match, :n 1, :phase :fresh, :profile :giant, :observed 259.8, :limit 248.3}
  TREND peak-over-budget {:op :rename-ns-plan-full-match, :n 1, :phase :warm, :profile :giant, :observed 294.5, :limit 244.1}
  TREND peak-scales-with-n {:op :cli-ls-tree, :profile :default, :observed 429.2, :limit 319.9, :small-n-observed 287.9, :slack-mb 32}
  TREND peak-scales-with-n {:op :workspace-sources-read-all, :profile :default, :observed 203.5, :limit 104.8, :small-n-observed 72.8, :slack-mb 32}
  UNMEASURED reserved-peak-over-budget {:op :cli-ls-tree, :detail "no operation on this branch reports an attributable reserved peak; the sampled process-wide peak is not a substitute"}
  UNMEASURED reserved-peak-over-budget {:op :rename-ns-plan-full-match, :detail "no operation on this branch reports an attributable reserved peak; the sampled process-wide peak is not a substitute"}
  UNMEASURED reserved-peak-over-budget {:op :rename-ns-plan-narrow, :detail "no operation on this branch reports an attributable reserved peak; the sampled process-wide peak is not a substitute"}
  UNMEASURED reserved-peak-over-budget {:op :workspace-sources-read-all, :detail "no operation on this branch reports an attributable reserved peak; the sampled process-wide peak is not a substitute"}
```

## What it says

**Three HARD failures, all on the new `held_mb` line** — the one Sol added and
the one the first baseline had no gate for:

| op | held at N=1,000 | held at N=10,000 | limit |
|---|---|---|---|
| `cli-ls-tree` | 9.5 MB | **94.0 MB** | 11.5 |
| `workspace-sources-read-all` | 4.4 MB | **41.0 MB** | 6.4 |
| `rename-ns-plan-full-match` | 1.0 MB | **9.9 MB** | 3.0 |

Every one of them passed the round-1 battery. They are the same operations, the
same corpus, and the same measured numbers — what changed is that a line now
looks at what the result costs to hold. `rename-ns-plan-narrow` stays flat at
0.1 MB, which is the control: the walk is bounded, the *plan* is not.

**Persistent growth (`grow_mb`) is ≤ 0.4 MB everywhere.** Nothing leaks. The
round-1 leak line compared the absolute post-release heap, which was flat for the
same reason — it was measuring the JVM's floor, not the calls.

**Output parity holds on every arm, including the three new corpora.** No
`reference-mismatch` anywhere, against an unbounded reference that is now
attested to this exact code, generator, corpus and JVM.

**No OOM anywhere**, at any arm, including the 1.9 MiB single file.

**The adversarial arms earn their place immediately.** `cli-ls-tree` peaks at
**386.4 MB on ONE 1.9 MiB file** and **285.7 MB on ONE 111 KB nested file** —
against a 248 MB budget, on corpora 20× and 364× smaller than the 10,000-file
tree. That is heap sized by a file's *shape*, not by the repository's size, and
no tree-scale arm could have shown it. Both are trends, not failures, and both
are the kind of trend worth watching: at `-Xmx4g` the same nested arm peaked at
1,322 MB.

**Reserved peak remains UNMEASURED on all four ops**, so the run terminates
INCOMPLETE as well as FAIL. No admission accountant exists on this branch; the
sampled process-wide peak is a different quantity and is not substituted for it.

## Reading it against round 1

Nothing regressed and nothing improved: the operations are untouched. Held values
reproduced almost exactly across the three runs (94.0 / 93.6 / 94.0 for
`cli-ls-tree`; 9.8 / 9.8 / 9.9 for the full-match rename), which is what makes
them fit to gate. The peak column moved by tens of MB on identical work between
the same three runs (`cli-ls-tree` N=1,000 fresh: 274.8 → 246.5 → 269.1), which
is what makes it unfit to gate — and is why it is now a trend.
