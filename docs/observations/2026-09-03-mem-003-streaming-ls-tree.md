# MCP-OP-MEM-003 — streaming `ls-tree` to its output budget: measurement receipt

Branch `bridge/streaming-ls-tree`, based on `bridge/parser-admission` at `8a55dbc`
(MEM-015 single parse + MEM-005 parser admission + the memory battery).
Anvil, JVM 21, 16 cores, other tenants at load 6–9 throughout.
`MEMBAT_ROOT=/home/forge/tmp/stream/membat`, so this lane never touched the
shared battery root. The battery ran exactly twice: once RED, once GREEN.

---

## 1. RED — the battery's own verdict, before any change

`make memory-battery MEMBAT_ROOT=/home/forge/tmp/stream/membat` at `8a55dbc`,
`-Xmx512m`, 5 reps, six corpora.
Receipt: `/home/forge/tmp/stream/membat/receipts/20260903T081744.082823318Z-battery.edn`.

`held_mb` is the battery's own definition: after-GC used heap **while the result
is still referenced**, minus start — the retained size of what the operation
hands back. It is the HARD line, not a trend.

```
op                            prof      N  phase  wall_ms  peak_mb  held_mb  excl_mb  grow_mb afterGC_mb  files      bytes  OOM?  verdict
-----------------------------------------------------------------------------------------------------------------------------------------
cli-ls-tree                default    100  fresh      122    192.1      0.6      0.9      0.0       24.0    100     404332    no       ok
cli-ls-tree                default    100   warm       96    178.7      0.9      0.9      0.0       24.0    100     404332    no       ok
cli-ls-tree                default   1000  fresh      552    268.4      9.6      9.5      0.1       24.0   1000    4045282    no    trend
cli-ls-tree                default   1000   warm      485    269.0      9.5      9.5      0.1       24.1   1000    4045282    no    trend
cli-ls-tree                default  10000  fresh     4213    430.0     94.0     93.6      0.4       24.4  10000   40472773    no    trend
cli-ls-tree                default  10000   warm     5021    424.1     93.6     93.6      0.0       24.4  10000   40472773    no    trend
cli-ls-tree                   cljc    100  fresh       43     63.4      0.1      0.4      0.0       24.1    100      77260    no       ok
cli-ls-tree                   cljc    100   warm       28     87.1      0.4      0.4      0.0       24.0    100      77260    no       ok
cli-ls-tree                  giant      1  fresh       23     33.5      0.0      0.0      0.0       24.0      1    1992594    no       ok
cli-ls-tree                  giant      1   warm       24     33.5      0.0      0.0      0.0       24.0      1    1992594    no       ok
cli-ls-tree                 nested      1  fresh        9     25.0      0.0      0.0      0.0       24.0      1     111183    no       ok
cli-ls-tree                 nested      1   warm        9     25.2      0.0      0.0      0.0       24.0      1     111183    no       ok
```

The RED line, verbatim from the verdict list:

```
FAIL held-scales-with-n {:op :cli-ls-tree, :profile :default, :observed 94.0,
                         :limit 11.6, :small-n-observed 9.6, :slack-mb 2.0}
```

**94.0 MB observed against a limit of 11.6 MB** — `max(held at N=1,000) + 2.0 MiB`.
Retention is 9.4–9.5 KB per file and is a straight line in N: the result IS the
repository. Two `TREND` lines follow from the same cause and are reported
alongside it:

```
TREND peak-scales-with-n {:op :cli-ls-tree, :profile :default, :observed 430.0,
                          :limit 301.0, :small-n-observed 269.0, :slack-mb 32}
TREND peak-over-budget   {:op :cli-ls-tree, :n 10000, :phase :fresh, :observed 430.0, :limit 248.0}
```

## 2. RED — the millisecond witness

The battery is a minutes-scale gate. The same defect at seconds scale, on a
400-file fixture of 60 forms each, measured with the battery's own held/excl
method (five `System/gc`, used heap while the result is referenced, then
released and re-measured), `-Xmx1g`:

| scan | ceiling asked for | held | after release |
|---|---|---:|---:|
| N=400 | `:max-results 50` | **13.64 MB** | -0.30 MB |
| N=50 | `:max-results 50` | 1.73 MB | — |
| N=400 | `:max-results 400` | 13.86 MB | — |

36,324 B per record on this fixture. The first row is the whole finding:
**asking for 50 records and getting 13.64 MB is asking for a bound that does not
exist.** `:max-results` was not a parameter at all before this intent, so the
bounded and unbounded scans of the same 400 files differ by 0.22 MB — inside
the noise. The gap the witness gates on is
`held(N=400, R=50) - held(N=50, R=50) = 11.91 MB`, against 0.50 MB of slack:
**24x the pass line.**

`make test-fast` at the RED commit fails to load, which is the honest state of a
registered intent with no implementation:

```
Type:     java.io.FileNotFoundException
Message:  Could not locate clj_surgeon/result_budget.clj on classpath.
Location: test/clj_surgeon/ls_tree_budget_test.clj:13:3
```

## 3. What the ceiling is, and why 1,000

`max-result-records = 1000`, a server hard cap a request may LOWER and may never
raise. Three independent bounds, all measured, agree on it:

1. **Retention.** 9.4–9.5 KB retained per record (the battery rows above).
   1,000 records pins retained result heap near 9.5 MB whatever the repository's
   size, and makes `held(10,000) <= held(1,000) + 2.0 MiB` hold **by
   construction**: both scales encode the same number of records.
2. **Real corpora.** This repository holds 163 `.clj`/`.cljc`/`.cljs` files under
   `src/` and `test/`. 1,000 is 6.1x that, so no ordinary single-project scan is
   truncated and no existing caller changes behaviour. The ceiling binds on
   repository-of-repositories scans — exactly where the unbounded result was
   already unusable.
3. **Output size.** The text encoding runs about 1.5 KB per file, so 1,000
   records is roughly 1.5 MB of output, already past what one CLI result or one
   model context absorbs.

Lowering it is always safe. Raising it re-opens the retention failure and has to
be re-measured on the battery first.

## 4. Boundaries this intent does NOT cross

- **It does not change outline CONTENT.** Every record under the ceiling is
  byte-identical to the batch encoder's, which is what the differential in
  §6 measures.
- **It does not bound the WALK.** Discovery still visits and stats every
  candidate; the per-file byte, aggregate-byte, entry and depth ceilings are
  MEM-002's, and the walker is q5z's. What MEM-003 bounds is what the ENCODER
  keeps.
- **It does not touch the MCP study-ops entrance.** `mcp_inspect_tool.clj` /
  `study.clj` are the `bridge/study-ops-mcp` lane's; see §8 for what that lane
  must adopt.
- **It does not bound `workspace-sources-read-all` or the rename planner.**
  Their `held-scales-with-n` failures are MEM-004's and their own lane's.
