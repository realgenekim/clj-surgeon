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

---

## 5. GREEN — what changed in the code

`run-ls-tree` no longer calls `outline-all-files`. It now:

1. discovers projects (unchanged) and folds the ordered candidate manifest into
   a SHA-256 in one pass — one `stat` per candidate, no row retained;
2. resolves the ceiling `R` (`:max-results`, lowering only) and the cursor;
3. takes the `R`-record window of candidates from the cursor's offset;
4. streams them: window of `4 x pool` -> the same bounded `pmap` the batch path
   used -> admit (MEM-005) -> parse once (MEM-015) -> outline -> **encode and
   drop**;
5. finishes with the totals, the admission block if any file was refused, and
   the continuation block if the ceiling bound.

Two encoders share that pipeline. The EDN encoder keeps a transient vector of
projected records. The text encoder appends one file at a time into one
`StringBuilder`; a project header carries a form total that is only known after
that project's last file is encoded, so it is **inserted at its recorded index**
when the project closes rather than buffered separately. The bytes are the batch
formatter's.

`format-ls-tree-text`, `format-ls-tree-edn` and `outline-all-files` remain in
`core.clj`. They are off the CLI path and exist as the **differential oracle**:
comparing the streaming encoder against a reconstruction of itself would be
tautological, so the old encoder is kept and run.

### The ms-scale witness, before and after

400-file fixture, 60 forms each, `-Xmx1g`, battery held/excl method:

| measurement | RED | GREEN |
|---|---:|---:|
| N=400, `:max-results 50` — held | 13.64 MB | **1.41 MB** |
| N=50, `:max-results 50` — held | 1.73 MB | 1.73 MB |
| N=400, unbounded control — held | 13.86 MB | 13.86 MB |
| `held(400,R=50) - held(50,R=50)` | **+11.91 MB** | **-0.32 MB** |
| bound: `(50 + 72 window) x 36,329 B + 0.5 MB` | — | 4.73 MB vs 1.41 MB observed |
| after-release | -0.30 MB | -0.18 MB |

The unbounded control row is the one that keeps the witness honest: it is
unchanged at 13.86 MB, so the pair is measuring retention and not a fixture that
stopped costing anything.

### The differential

Both encodings, against the batch encoder, over this repository:

| root | records | EDN mismatches | text equal | order equal |
|---|---:|---:|---|---|
| `src/` | 69 | **0** | yes | yes |
| `test/` | 99 | **0** | yes | yes |

168 records, zero mismatches, order preserved. A bounded result is the PREFIX of
the unbounded one, never a sample — asserted separately in the fast suite.

### Hand-driven, every mode

Not inferred from tests; each was run at the CLI and read:

```
$ clj-surgeon :op :ls-tree :dir src :max-results 3
── total: 3 files, 58 forms
── result_ceiling: 3 record(s), 3 of 69 file(s) shown from offset 0, 66 remaining
   next_call: clj-surgeon :op :ls-tree :dir src :max-results 3 :cursor 3:7b371d51…

$ clj-surgeon :op :ls-tree :dir src :max-results 3 :complete true
── result-ceiling-exceeded: ls-tree found 69 file(s); a complete result may hold at most 3 record(s)
   remedy: narrow :dir, add :grep, or drop :complete and page through the result with the :cursor in :next_call

$ clj-surgeon :op :ls-tree :dir src :max-results 3 :cursor 3:7b371d51…
clj_surgeon/cljc/analyze.clj  80 lines, 6 forms          <- the 4th file, as it should be

$ clj-surgeon :op :ls-tree :dir src :max-results 3 :cursor 3:0000…
── stale-result-cursor: the tree changed since this cursor was issued

$ clj-surgeon :op :ls-tree :dir src :max-results 0
── invalid-result-ceiling: :max-results must be a positive integer; got "0"
```

## 6. GREEN — `make memory-battery`, the second and last run

Same root, same `-Xmx512m`, same 5 reps, same six corpora, at `2c33eb6`.
Receipt: `/home/forge/tmp/stream/membat/receipts/20260903T084808.397813672Z-battery.edn`.

```
op                            prof      N  phase  wall_ms  peak_mb  held_mb  excl_mb  grow_mb afterGC_mb  files      bytes  OOM?  verdict
-----------------------------------------------------------------------------------------------------------------------------------------
cli-ls-tree                default    100  fresh      159    167.1      0.5      0.9      0.0       24.0    100     404332    no       ok
cli-ls-tree                default    100   warm      103    192.5      0.9      0.9      0.0       24.0    100     404332    no       ok
cli-ls-tree                default   1000  fresh      668    232.2      9.3      9.3      0.0       24.0   1000    4045282    no       ok
cli-ls-tree                default   1000   warm      585    239.6      9.5      9.4      0.1       24.1   1000    4045282    no       ok
cli-ls-tree                default  10000  fresh      658    243.1      9.5      9.4      0.1       24.1  10000   40472773    no       ok
cli-ls-tree                default  10000   warm      769    263.8      9.4      9.4      0.0       24.1  10000   40472773    no    trend
cli-ls-tree                   cljc    100  fresh       39     63.7      0.4      0.4      0.0       24.0    100      77260    no       ok
cli-ls-tree                   cljc    100   warm       27     90.7      0.4      0.4      0.0       24.0    100      77260    no       ok
cli-ls-tree                  giant      1  fresh       23     33.6      0.0      0.0      0.0       24.0      1    1992594    no       ok
cli-ls-tree                  giant      1   warm       24     33.6      0.0      0.0      0.0       24.0      1    1992594    no       ok
cli-ls-tree                 nested      1  fresh        9     25.0      0.0      0.0      0.0       24.0      1     111183    no       ok
cli-ls-tree                 nested      1   warm       10     24.7      0.0      0.0      0.0       24.0      1     111183    no       ok
```

Side by side on the default corpus:

| N | held before | held after | peak before | peak after | wall before | wall after |
|---:|---:|---:|---:|---:|---:|---:|
| 100 | 0.6 / 0.9 | 0.5 / 0.9 | 192.1 / 178.7 | 167.1 / 192.5 | 122 / 96 | 159 / 103 |
| 1,000 | 9.6 / 9.5 | 9.3 / 9.5 | 268.4 / 269.0 | 232.2 / 239.6 | 552 / 485 | 668 / 585 |
| 10,000 | **94.0 / 93.6** | **9.5 / 9.4** | 430.0 / 424.1 | 243.1 / 263.8 | 4213 / 5021 | **658 / 769** |

**`held_mb` is flat from N=1,000 onward: 9.3–9.5 MB at 1,000 and 9.4–9.5 MB at
10,000, against 94.0 MB before.** That is what a ceiling of 1,000 records means —
both scales encode the same number of records, so the line holds by construction
rather than by luck.

Verdict lines that DISAPPEARED, all `cli-ls-tree`:

```
FAIL  held-scales-with-n  {:observed 94.0, :limit 11.6, :small-n-observed 9.6}   <- the RED line
TREND peak-scales-with-n  {:observed 430.0, :limit 301.0, :small-n-observed 269.0}
TREND peak-over-budget    {:n 1000,  :phase :fresh, :observed 268.4, :limit 248.0}
TREND peak-over-budget    {:n 1000,  :phase :warm,  :observed 269.0, :limit 248.0}
TREND peak-over-budget    {:n 10000, :phase :fresh, :observed 430.0, :limit 248.0}
```

One `cli-ls-tree` line remains, and it is a TREND, not a gate:

```
TREND peak-over-budget {:op :cli-ls-tree, :n 10000, :phase :warm, :observed 263.8, :limit 248.1}
```

263.8 MB against a 248.1 MB trend line, down from 424.1 MB. Sampled process-wide
peak is heap-size dependent under G1 and this row bounds retention, not peak;
the attributable reserved peak stays `UNMEASURED` until MEM-001's accountant
exists, and the battery says so rather than passing a line nobody measured.

The battery still exits 1. Its remaining HARD failures belong to other lanes and
are unchanged by this work:

```
FAIL held-scales-with-n {:op :rename-ns-plan-full-match, :observed 10.0, :limit 3.0}
FAIL held-scales-with-n {:op :workspace-sources-read-all, :observed 41.1, :limit 6.4}
```

### One honest caveat about the battery's parity check

The battery compares each arm's `result-hash` against a cached UNBOUNDED
reference, and that reference is rebuilt whenever the code changes — so at
N=10,000 both sides are now bounded and the parity check for `cli-ls-tree`
proves determinism, not equivalence with the old encoder. **The parity evidence
for this change is the differential in §5, not the battery.** Saying otherwise
would be a green light nobody measured.

### Wall — the cost, measured in one JVM rather than across two runs

Two battery runs minutes apart on a box shared with other tenants is a poor
timer. The same corpus, one JVM, 7 reps after 2 warm-ups, `-Xmx1g`:

| encoder | 1,000 files, min | median |
|---|---:|---:|
| batch (before), EDN | 374.9 ms | 412.6 ms |
| **stream (after), EDN** | **431.6 ms** | 458.1 ms |
| batch (before), text | 400.9 ms | 429.1 ms |
| **stream (after), text** | **437.4 ms** | 452.2 ms |

**+9% to +15% at 1,000 files**, and it is not free-lunch accounting: the
manifest pass costs a measured **5.3 ms** of it (1,000 stats folded into
SHA-256), and the remainder is re-entering `pmap` once per 72-record window
instead of once for the whole scan. The trade is bought back an order of
magnitude over: at 10,000 files the scan went from 4,213 ms to 658 ms, because
it no longer outlines 9,000 files whose records it would immediately discard.

## 7. Gates

| gate | result |
|---|---|
| `make test-fast` | Ran 749 tests containing 6162 assertions. 0 failures, 0 errors. (baseline 737 / 6103) |
| `clojure -M:clj-surgeon/mcp-test` | Ran 387 tests containing 3982 assertions. 0 failures, 0 errors. (baseline 385 / 3971) |
| `make mcp-operation-oracle` | pass; legacy counterexamples `[verification_failed, verification_pending]` — the baseline two, unchanged |
| `make memory-battery-self-test` | Ran 24 tests containing 138 assertions. 0 failures, 0 errors. |
| intent traceability contract | `:ok true`, zero violations, `MCP-OP-MEM-003 :implemented` |
| `make memory-battery` | `cli-ls-tree` held line GREEN; exit 1 on other lanes' unchanged failures |

All unit suites ran under `/home/forge/bin/suite-run`. The battery took the
exclusive `/home/forge/tmp/suite.lock` and ran exactly twice, on this lane's own
`MEMBAT_ROOT`. It queued behind another lane's `make memory-red` for four
minutes rather than sharing the box, which is the lock working.

## 8. What the study-ops lane must adopt

`bridge/study-ops-mcp` owns `mcp_inspect_tool.clj` and `study.clj` and was
deliberately not touched. To satisfy the same row at its own entrance it needs
four things, and `clj-surgeon.result-budget` is written so none of them have to
be re-invented:

1. **The same ceiling.** `budget/resolve-ceiling` on the request's own
   `max_results`, clamped by `budget/max-result-records`. A ceiling that differs
   between entrances is two policies, and the caller cannot tell which one it
   got.
2. **The same two typed answers.** `budget/continuation` and
   `budget/ceiling-refusal`, with `:complete` selecting between them. An MCP
   result that silently returns fewer records than the repository holds is the
   failure this row exists to stop, and it is invisible over the wire.
3. **A cursor bound to a manifest digest**, via `budget/digest-start` /
   `digest-candidate!` / `digest-hex` / `cursor-token` / `parse-cursor`. Over MCP
   the gap between two pages can be arbitrarily long, so the staleness check
   matters *more* there than at the CLI, not less.
4. **Streaming, not just capping.** Capping the returned vector while still
   building every outline first fixes the wire and leaves the heap exactly where
   it was. The retention promise is in `stream-outlines!` + an encoder that
   drops each outline — `core/outline-window-size` is public so that lane's own
   retained-heap witness can gate on `ceiling + window` too.

## 9. Gaps this measurement leaves open

- **Peak is still a trend, and still unattributed.** `cli-ls-tree` at N=10,000
  warm sits at 263.8 MB against a 248.1 MB trend line. Bounding the encoder does
  not bound the parser's transient peak; that is MEM-008's reservation
  accountant, and until MEM-001 reports an attributable reserved peak the
  battery correctly reports `UNMEASURED` rather than green.
- **The walk is still O(N) in path strings.** Discovery holds one path per
  candidate — about 1.2 MB at 10,000 files — because the bounded walker is
  q5z's and MEM-002's. This row bounds the encoder, and the 9.5 MB it now
  retains includes that.
- **`workspace-sources-read-all` (41.1 MB held at 10,000) and
  `rename-ns-plan-full-match` (10.0 MB) still fail the same line.** They are
  MEM-004's and the rename lane's; nothing here reaches them.
- **The 1,000-file wall regressed 9–15%.** A larger window would recover some of
  it and loosen the retention bound by the same factor. It was left at `4 x pool`
  because the number that matters is retention at 10,000 files, and the wall
  there improved 6.4x.

---

# Addendum, 2026-09-03 (later the same day) — Sol's NO-GO, and what §8 above now gets wrong

Everything above §7 stands: the retention result reproduced independently
(1,000-file held 9.5/9.5 MB, 10,000-file 9.3/9.6 MB, retained-batch control
93.45 MB versus 9.35 MB streamed). Sol's executed review nonetheless returned
**NO-GO on cursor integrity**, and two of its twelve findings were silent wrong
results rather than refusals. This addendum is appended rather than edited into
the text above, because that text is the record of what was believed at the
time. **Section 8 item 3 is now actively wrong guidance and the study-ops lane
must not follow it.**

## What was wrong

1. **The cursor digest was stat-based, not content-based** (finding 1, BLOCKER).
   Replacing a file's bytes while preserving path, size and mtime was accepted,
   and page 2 served the changed namespace. A cursor minted against a different
   root with matching stats was likewise served.
2. **Offsets were neither authenticated nor range-checked** (finding 2, BLOCKER).
   A forged offset of 99 on a three-record tree returned an empty vector with no
   receipt — which every caller reads as a complete result.
3. **Every continuation page re-walked the whole manifest** (finding 7): two
   1,000-record pages over the 10,000-file corpus took 1,305 ms and 661 ms, each
   folding all 10,000 stat rows.
4. **Concurrency exceeded the declared pool** (findings 6 and 9): 32-33 outlines
   active against a documented 18-worker pool, because `pmap` realises its input
   32 at a time and the chunk size — not the pool constant — set concurrency.
5. **40-digit numeric fields threw `NumberFormatException`** instead of the
   documented typed refusals (finding 3).

## What §8 item 3 should say instead

Not "a cursor bound to a manifest digest via `digest-start` /
`digest-candidate!` / `digest-hex`" — **those functions no longer exist.** The
study-ops lane must adopt the **pinned immutable manifest snapshot** in
`clj-surgeon.ls-tree-snapshot`:

- `snapshot/write-snapshot!` on the first page that needs a cursor — ordered
  candidate rows, each with the SHA-256 of its **content**, written streaming
  and addressed by the SHA-256 of the canonical root path;
- `budget/cursor-token` / `parse-cursor` for the `<cursor-id>:<offset>:<mac>`
  grammar, with `snapshot/mac` keyed on the snapshot's private secret — **never**
  on the published manifest digest, which would let any holder of a receipt mint
  any offset;
- all **four** typed refusals, not one: `:invalid-result-cursor` (not ours),
  `:unknown-result-cursor` (not this root, or gone), `:result-cursor-out-of-range`
  (ours, position absent), `:stale-result-cursor` (ours, and a pinned file's
  content moved — naming the path);
- `snapshot/read-rows` for the page's own slice, so a page does **no** discovery.

The point Sol's finding makes for that lane is sharper than the original: over
MCP the gap between two pages can be arbitrarily long, so a cursor that cannot
detect a content change is not merely imprecise there — it is the primary
failure mode.

## Measured after the repair

| | before (Sol) | after |
|---|---:|---:|
| page 1 over 10,000 files, `:max-results 1000` | 1,305 ms | 565 ms |
| page 2 | 661 ms | **152 ms** |
| manifest rows folded per page | 10,000 | **1,000** |
| tree walks on page 2 | 1 | **0** |
| max concurrent outlines vs declared pool of 18 | 33 | **18** |

The row-fold count is the number that matters: page cost was `O(pages x N)` and
is now `O(page)`. Wall time follows it, but wall time alone cannot distinguish a
faster walk from no walk, which is why the witness counts calls.

The price is stated in the LLD and repeated here: **a continuation is a snapshot
read, not a live one.** Files created after the pin never appear on later pages,
and a file whose bytes moved refuses when its own page is served. Every digest
is taken at issue time rather than lazily per page — the lazy variant would pin
a changed file's *new* bytes and call them unchanged, which is finding 1 moved
later in the sequence.

## Gap this addendum does NOT close

§9's second bullet stands and has been promoted into the EARS: **discovery still
retains an N-sized path collection**, roughly 1.2 MB at 10,000 files. The
requirement now names the *CLI `ls-tree` encoder* rather than `ls-tree`, because
the earlier wording claimed a bound the code does not hold (finding 12). A green
battery line at 10,000 files is not "`ls-tree` is bounded in N."
