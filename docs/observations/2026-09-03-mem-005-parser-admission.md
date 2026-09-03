# MCP-OP-MEM-005 — bounded lexical/parser admission: measurement receipt

Branch `bridge/parser-admission`, based on `bridge/read-path-memory` at `61cb9b5`
(MEM-015 single parse) with `bridge/memory-battery` at `c6a2264` merged in.
Anvil, JVM 21.0.12, 16 cores, load average 6–9 from other tenants throughout.

| commit | what |
|---|---|
| `a2d67ad` | merge of the battery lane (its adversarial corpus generators and `make memory-battery`) |
| `cb70340` | RED — `make memory-red`, the two adversarial cells isolated to one outline per JVM |
| `391e82e` | MEM-005 registered, twelve ms-scale witnesses, implementation absent |
| `02976e4` | GREEN — `clj-surgeon.parse-admission`, wired into both read-path tree constructors |

---

## 1. What the red witness found that the battery could not

The battery's round-2 baseline reported `cli-ls-tree` peaking at 386.4 MB on ONE
1.9 MiB file and 285.7 MB on ONE 111 KB 300-deep file, both `trend`, both with
`:errors []`. Isolating each to a single `outline-source` call in a fresh JVM
(`make memory-red`) showed the nested cell is **two different failures wearing
one number**:

| cell | -Xmx | warm-ups | outcome | peak | wall |
|---|---|---|---|---|---|
| nested (111,183 B) | 512m | 0 | **StackOverflowError** | 33.4 MB | 42 ms |
| nested (111,183 B) | 512m | 200 | completed | **312.4 MB** | 827 ms |
| giant (1,992,594 B) | 128m | 0 | **OutOfMemoryError** | 126.6 MB | 4,191 ms |
| giant (1,992,594 B) | 512m | 0 | completed | 339.9 MB | 1,364 ms |

On a **cold** JVM the rewrite-clj reader recurses once per nesting level in
interpreted frames and overflows the default 1 MB stack. `core/safe-outline`
catches `Exception`, not `Error`, so **one such file kills the entire `ls-tree`
scan** — verified directly against `run-ls-tree` over a directory holding only
that file. Once the parser's hot path is JIT-compiled the same file at the same
`-Xmx` completes instead, at 2,876x its own source.

The battery sees only the warm branch because it runs its adversarial arms after
10,000 files have warmed the parser. Which catastrophe a caller gets depends on
JIT state, which is not something a caller chooses.

## 2. The ceilings and how they were derived

`max_parse_depth = 150` · `max_parse_nodes = 200,000`

**Depth — bounded by the reader's stack, not by heap.** Cold-JVM ladder at
`-Xmx512m`, default 1 MB stack, one bracket tower per file:

| levels | 100 | 200 | 300 | 400 | 410 | 425 | 440 | 460 | 480 | 500 | 600 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| outcome | ok | ok | ok | ok | ok | ok | ok | **1 of 2 SOE** | SOE | SOE | SOE |

Lowest observed cold StackOverflowError: **460**. Deepest of the 163
`.clj`/`.cljc`/`.cljs` files under `src/` and `test/`: **22**
(`intent_transaction.clj`). Battery default corpus 8, cljc corpus 5. So 150 sits
**6.8x above real code** and **3.07x below the crash**, and is placed against the
COLD threshold because that is the lower of the two JIT branches. The
adversarial `nested` file is 601 levels — 4.0x over.

**Nodes — two independent bounds, and 200,000 satisfies both.**

*Upper bound: it must never pre-empt MEM-002 on a file the byte ceiling admits.*
Per the coordinator's ownership split, the per-file BYTE ceiling is MEM-002's,
and tuning a node ceiling down until it refuses large *ordinary* files would
make MEM-005 a worse copy of it. Highest node density measured over any corpus
in evidence: **273.6 nodes/KiB** (the battery's synthetic filler); this
repository's own sources run 102.6–189.9. At MEM-002's ~512 KiB per-file ceiling
(Opus's consult, section 5) the densest admitted source yields at most
512 x 273.6 = **140,288** nodes. 200,000 leaves 1.42x of headroom above that.

*Lower bound: it must actually bound heap.* Scaling linearly from the measured
`giant` cell (532,424 nodes peaked 339.9 MB at `-Xmx512m`), 200,000 nodes
projects to about **128 MB** of transient peak — inside the battery's 247.8 MB
per-operation budget. 300,000 projects to 191 MB and 500,000 to 319 MB.

*Margins.* 10.2x above the largest source here (19,528 nodes) and 50x above the
battery's default corpus (3,980). The `giant` file is 532,424 nodes — 2.66x over,
and MEM-005 refuses it **only as a second guard**: when MEM-002's byte ceiling
lands, that file will refuse on bytes first, with the byte remedy and the
narrowing `next_call`.

**Which control fires on which shape, by design:**

| shape | bytes | nodes | depth | refused by | owner |
|---|---|---|---|---|---|
| `nested` | 111,183 | 41,252 (admitted) | 601 | `max_parse_depth` | MEM-005 |
| `giant` | 1,992,594 | 532,424 | 6 (admitted) | `max_parse_nodes` | MEM-002's byte ceiling first, once it exists |

The nested file is a fifth of the 512 KiB byte ceiling. No byte control in the
design can see it. That is precisely the gap MEM-005 exists for.

**The estimator.** `scan-shape` is a single pass over the raw string; delimiters
inside strings, regex literals, character literals and comments are text, never
structure. Validated against Sol's rewrite-clj node count for
`intent_transaction.clj`: 21,996 real nodes, **19,528 estimated** — 11% low,
because a whitespace run counts once here where rewrite-clj splits whitespace
from newlines. Its delimiter balance is **zero on all 163 real sources**, which
is the witness that the string/regex/char/comment handling is right.

## 3. `make memory-red`, before and after — same cells, same -Xmx

| cell | -Xmx | before | after |
|---|---|---|---|
| nested cold | 512m | StackOverflowError, 42 ms, **the whole scan dies** | refused `max_parse_depth` (limit 150, observed 601), 19 ms, peak 44.6 MB |
| nested warm | 512m | completed, peak **312.4 MB**, 827 ms | refused, 4 ms, peak **52.5–134.8 MB** |
| giant | 128m | **OutOfMemoryError** | refused `max_parse_nodes`, 65 ms, peak 52.6 MB |
| giant | 512m | completed, peak **339.9 MB**, 1,364 ms | refused, 76 ms, peak 65.9 MB |

The nested-warm peak is stated as a RANGE because the single figure did not
reproduce. This run measured 52.5 MB; Opus's independent green re-run on
2026-09-03, on a box under load 6–9, measured **134.8 MB** for the same cell
with the same -Xmx, while the other three cells landed within noise (45.9 /
60.1 / 59.8 MB against 44.6 / 52.6 / 65.9). The verdict is identical either
way — 2.3x under the 247.8 MB budget and 2.3x below the 312.4 MB pre-fix
figure — but a published cell that does not reproduce on a loaded box is a
figure, not a receipt, and it is restated as the range that does.

After the fix the peaks are the JVM's own baseline (44–66 MB), not the file's
shape. Refusal latency on the two 111 KB cells is **19 ms and 4 ms**, both under
the 50 ms line. The giant cell's 65 ms wall includes reading 1.9 MiB from disk;
the control's own cost there is `scan-ms` = 9 ms.

## 4. `make memory-battery`, before and after

Round 2 at `a9d2d4b` (`docs/observations/2026-09-03-memory-battery-baseline.md`)
against this run at `02976e4`, both `-Xmx512m`, 5 reps, six corpora. Receipt:
`/home/forge/tmp/admit/membat/receipts/20260903T072709.430803127Z-battery.edn`
(`MEMBAT_ROOT=/home/forge/tmp/admit/membat`, so this lane never touched the
shared root).

**The two adversarial `cli-ls-tree` cells:**

| profile | phase | before wall | before peak | before verdict | after wall | after peak | after verdict |
|---|---|---|---|---|---|---|---|
| giant (1 file, 1,992,594 B) | fresh | 2,058 ms | **386.4 MB** | trend | **27 ms** | **33.4 MB** | **ok** |
| giant | warm | 1,975 ms | **376.5 MB** | trend | **23 ms** | **33.5 MB** | **ok** |
| nested (1 file, 111,183 B) | fresh | 834 ms | **264.7 MB** | trend | **15 ms** | **25.3 MB** | **ok** |
| nested | warm | 784 ms | **285.7 MB** | trend | **10 ms** | **24.6 MB** | **ok** |

Both `TREND peak-over-budget` lines for `cli-ls-tree` on giant and on nested are
**gone from the verdict list**. Peak fell 11.5x on the giant cell and 11.6x on
the nested cell; wall fell 76x and 78x.

**No default-corpus cell changed.** Held retention is identical to round 2 —
`cli-ls-tree` 0.5/0.9, 9.5/9.5, 94.0/93.7 MB at N=100/1,000/10,000 against
0.5/0.9, 9.4/9.5, 94.0/93.6 before; `workspace-sources-read-all` 40.8/40.9
against 40.8/41.0; `rename-ns-plan-narrow` flat at 0.1.

Output parity was checked directly rather than assumed. Comparing the two
receipts' `result-hash` values, all eight `cli-ls-tree` default and cljc cells
are **byte-identical**; the only `cli-ls-tree` cells that changed are giant and
nested, which now carry the refusal receipt. The other arms' hashes differ
between receipts **because their results embed the corpus root path**, and this
run used a different root. That was verified, not inferred: running each
battery arm with the CURRENT code against the OLD corpus root reproduces the
round-2 hashes exactly —

```
workspace-sources-read-all   OLD root 479af8c6fea7   NEW root 7c988153833d
rename-ns-plan-narrow        OLD root 76bbe769dd54   NEW root 089b6ff885bd
rename-ns-plan-full-match    OLD root 80b3e8d0a95a   NEW root 0dd41606513d
cli-ls-tree                  OLD root 6ff025c52fcb   NEW root 6ff025c52fcb
```

where the OLD-root column equals the round-2 receipt value in every row.
`cli-ls-tree` is root-independent because its output uses relative paths.

The three `held-scales-with-n` HARD failures and the remaining `peak` trends on
the default corpus are unchanged and are not MEM-005's subject: they are the
retain-everything shapes MEM-002/003/004 own.

## 5. The gap this measurement exposed, and its correction

As first published, this section reported that `rename-ns-plan-full-match` and
`rename-ns-plan-narrow` still peak at 298.8 and 306.9 MB on the giant file
because `clj-surgeon.analyze` calls `rewrite-clj.zip/of-string` directly — a
**third tree constructor** outside the two outline entries — and argued for
leaving it ungated, on the ground that "a refusal is only safe where the caller
turns it into a named, counted, per-file skip … gating `analyze` today would
convert an operation that COMPLETES into one that throws."

**The doctrine is right and the factual premise was wrong.** Opus's executed
review measured `analyze/string->zloc` on the shape in question: it does not
complete, it throws `StackOverflowError` — an `Error` no caller handles.
Gating it swaps an uncatchable Error for a typed `ExceptionInfo`, which is
strictly better for every caller even with no receipt to carry it. And the
review found a **fourth** ungated constructor this section never mentioned:
`clj-surgeon.structural-lens/find-subforms` / `find-file`, reached from the MCP
read surface at `mcp_inspect.clj:530` and the CLI `:find-subform` op at
`core.clj:737` — the read path's own `find_subforms` verb, which is a gap in
this leaf and not a neighbouring lane's.

Both are gated now, each with the minimal skip surface the doctrine demands:
`core/named-plan-refusal` turns the typed refusal into a named plan refusal at
the five analyze-driven CLI ops, and `find-subforms` returns it with `:reason`,
`:limit`, `:observed` and `:remedy` intact rather than flattened to a string.
`show_form` was flattening it the same way and no longer does.

Two further corrections to what this receipt claimed:

- The estimator counted DELIMITER depth, not nesting depth. A 710-byte
  `(def x @@@…y)` scanned at parse-depth 1, was admitted, and killed a whole
  `ls-tree` scan — the same catastrophe, at 1/155th the size of the 111 KB
  file above. `scan-shape` now counts every construct the reader recurses
  into; the corpus cost over 252 sources is zero refusals and no change to the
  deepest file (22).
- `safe-outline` caught only `Exception`, so the scan-kill this receipt says is
  fixed was fixed only for shapes the estimator can see. It now catches
  `StackOverflowError` and turns it into the same named, counted skip, which
  closes the class independent of estimator completeness.
  `OutOfMemoryError` is deliberately still not caught: it belongs to the typed
  resource refusal.

`rename`'s own planner parses through `rewrite-clj.zip/of-string` directly
(`rename.clj:67,166`), NOT through `analyze`, so the two `rename-ns-plan-*`
peaks above are still not reached by this intent. That belongs to whichever
lane owns the rename receipt.

## 6. Unit figures for the control itself

`scan-shape`, on this box, after a primitive rewrite:

| input | cold | warm |
|---|---|---|
| 1,992,594 B (`giant`) | 54 ms | 8.7 ms (~229 MB/s) |
| 126,596 B (`intent_transaction.clj`) | — | 1.26 ms |
| 48,097 B (`mcp_inspect_tool.clj`) | — | 370 us |

370 us on a 48 KB file is a few percent of the outline it precedes.

The first draft of the scan cost **804 ms** on the 126 KB file — a 638x
regression caused by `(long (.charAt ...))`, because `clojure.lang.RT/longCast`
has no `char` overload and every character went through reflection. Every test
passed. It was found by turning on `*warn-on-reflection*` and looking at a
number that felt wrong, not by any gate — worth a ratchet of its own for
whichever lane owns build hygiene, since nothing in this repository fails on a
reflection warning.

## 7. Gates

| gate | result |
|---|---|
| `make test-fast` | Ran 737 tests containing 6103 assertions. 0 failures, 0 errors. (baseline 726 / 6050) |
| `clojure -M:clj-surgeon/mcp-test` | Ran 385 tests containing 3971 assertions. 0 failures, 0 errors. |
| `make mcp-operation-oracle` | pass; legacy counterexamples `[verification_failed, verification_pending]` — the baseline two, unchanged |
| `make memory-battery-self-test` | Ran 24 tests containing 138 assertions. 0 failures, 0 errors. |
| `make memory-red PARSER_RED_EXPECT=green` | 6/6 assertions held |
| intent traceability contract | `:ok true`, zero violations |

All unit suites ran under `~/bin/suite-run`; the battery and `memory-red` took
the exclusive `~/tmp/suite.lock`. The battery ran exactly once.
