# MCP-OP-MEM-015 — the outline read path parses once and builds no discarded source

Anvil, `bridge/read-path-memory`, 2026-09-03. Red a845215 · registry b6aebbc ·
green 46a61d2. Every JVM run below was serialized behind
`flock /home/forge/tmp/suite.lock`, one JVM at a time.

## Headline — 1,000-file corpus

| arm | wall (best of 3) | allocated (best of 3) | per source byte | outline hash |
|---|---:|---:|---:|---|
| BEFORE — two parses, `:source` built then discarded | 11,784.0 ms | 25,279,854,216 B | 1459.4x | `b3864d59…22a0e4` |
| AFTER — one parse, no discarded `:source` | 6,620.6 ms | 14,947,489,200 B | 862.9x | `b3864d59…22a0e4` |
| **delta** | **−43.8%** | **−40.9%** | **−40.9%** | **identical** |

Every run, so the noise floor is visible rather than asserted:

| run | arm | wall | allocated |
|---|---|---:|---:|
| 1 | BEFORE | 12,708.5 ms | 25,279,854,216 B |
| 2 | BEFORE | 11,784.0 ms | 25,305,467,392 B |
| 1 | AFTER | 7,361.2 ms | 14,947,489,200 B |
| 2 | AFTER | 6,982.3 ms | 15,006,389,800 B |
| 3 | AFTER (final code) | 6,620.6 ms | 15,006,360,720 B |

The first 1,000 files of `/home/forge/tmp/corpus`, 17,321,542 source bytes,
20,207 outlined forms, one JVM at a time at `-Xmx1g`, allocation measured with
`com.sun.management.ThreadMXBean/getThreadAllocatedBytes` on the working
thread, `System/gc` before the clock starts. Harness:
`/home/forge/tmp/readpath/corpus.clj`; the BEFORE arm is the same harness with
the pre-change `outline.clj` ahead of `src` on the classpath, so nothing but
that one file differs.

`outline_sha256` is SHA-256 over `pr-str` of every outline in path order.
**The same digest on both sides is a 1,000-file differential**, on top of the
160-file one in the test suite.

Opus's projection for removing the second parse alone was 11.30 s / 24.6 GB →
8.96 s / 17.0 GB. Removing the discarded `:source` as well takes it to
6.6–7.4 s / 15.0 GB: the second parse and the discarded string are worth about
as much as each other.

## Unit figures — one 48,097-byte file

Frozen fixture `test-fixtures/memory/mem_015_outline_fixture.clj` (a copy of
`src/clj_surgeon/mcp_inspect_tool.clj`), min of five calls after three
warm-ups, `-Xmx1g`:

| arm | allocated | per source byte |
|---|---:|---:|
| `outline-source`, two parses + discarded `:source` | 62,686,992 B | 1303.3x |
| `outline-source`, one parse, no discarded `:source` | 37,583,552 B | **781.4x** |
| `top-level-form-records` alone (one parse, keeps `:source`) | 45,631,496 B | 948.7x |
| parse + walk alone — the irreducible rewrite-clj node tree | 36,019,832 B | 748.9x |

The remaining outline cost is **4.3% above the node tree itself**. Everything
else this projection does — names, arglists, platform sets, attached comments,
requires — is noise next to building the tree. That is the honest ceiling for
this leaf: going lower means not building a rewrite-clj tree for a read-only
projection, which is a different intent behind a differential gate.

`rewrite-clj.zip/of-string` calls per outline: **2 → 1**.

## What changed

`clj-surgeon.outline` only.

1. `outline-source` builds one `z/of-string` root and uses it twice.
   `cwalk/top-level-forms-from-zloc` already existed for exactly this; the `ns`
   lookup now runs `z/find-value` over the same persistent zipper. Both sides
   start from the same root at the same position, so the `ns` resolution is
   identical by construction rather than by resemblance — including the
   file-with-no-`ns` and `ns`-bound-as-a-local shapes, where the old lookup's
   behaviour was incidental and had to be preserved exactly.
2. The record builder is factored into `form-records-from-walked`, and
   `top-level-form-records` grows `{:include-source? false}`. The
   `(z/string zloc)` call is then never made, rather than made and thrown away.
   `outline-source` passes false unless `include-string-symbols` is set, because
   the symbol scanner reads `:source`. Every other caller — extract, show_form,
   compact-location, the source anchor, the change buffer, intent-transaction,
   mcp-inspect — keeps the default and still receives exact `:source`.

## Witnesses

- `outline-of-one-file-allocates-within-its-ceiling` — allocated bytes per
  source byte for one outline, ceiling **980x** = the measured single-parse
  path (782.6x) plus 25%. Deliberately far above the "tens" a reader might
  expect: see the node-tree row above.
- `outline-parses-each-file-exactly-once` — `rewrite-clj.zip/of-string` is
  redef'd to a counting wrapper; exactly one call per outline.
- `single-parse-outline-is-byte-identical-over-the-repository` — the acceptance
  artifact. The two-parse path is reconstructed **from public functions only**
  (`top-level-form-records`, a fresh `z/of-string`, `extract-ns-requires`), so
  the comparison cannot go tautological, and `pr-str` of both outlines is
  compared over all **160** `.clj`/`.cljc`/`.cljs` files under `src/` and
  `test/`: **0 mismatches**. `pr-str` rather than `=` so array-map insertion
  order is compared too.
- `single-parse-outline-is-byte-identical-on-boundary-shapes` — seven shapes
  the repository tree does not contain: no `ns` form, empty source,
  whitespace-only, comments-only, a `.cljc` whose forms live inside `#?` and
  `#?@`, `ns` bound as a local, attached comments.
- `string-symbol-outlines-still-see-form-source` and
  `structural-readers-still-receive-exact-source` — the two callers that must
  keep `:source`.

The allocation witnesses are JVM-only (`com.sun.management`), so they live in
`clojure -M:clj-surgeon/mcp-test`, not in the babashka `test-fast` runner.

## What this does NOT promise

- **Not a resident or peak-heap bound.** This is transient allocation on one
  thread. Retained bytes, sampled peak, and the minimum `-Xmx` that completes a
  corpus belong to the memory battery (MCP-OP-MEM-001 / -011). Peak used heap
  under G1 is heap-size dependent and cannot carry a requirement.
- **Not a change to outline content.** Proven, not asserted: see the two
  differential witnesses and the corpus digest.
- **Not the removal of `:source` from `top-level-form-records`.** Six call
  sites read it.

## Memory battery — before/after (MCP-OP-MEM-001 / -011 instrument)

`make memory-battery` from `bridge/memory-battery` (2bae68b), cherry-picked onto
a scratch branch `bridge/read-path-memory-battery` on top of the green commit.
`-Xmx512m`, 5 reps, N = 100 / 1,000 / 10,000, one JVM at a time under the suite
lock, `MEMBAT_ROOT=/home/forge/tmp/readpath/membat` (its own root so the battery
lane's concurrent runs are not disturbed). Reference hashes regenerated in that
root at `-Xmx4g` with the GREEN code; the BEFORE arm is the identical tree and
the identical reference with only `src/clj_surgeon/outline.clj` reverted to
a845215. **BEFORE ran first in this pair**, so the AFTER numbers are not
flattered by a warm page cache.

### `cli-ls-tree` — the only arm that outlines

| N | phase | wall BEFORE | wall AFTER | Δ wall | peak BEFORE | peak AFTER | Δ peak | held BEFORE | held AFTER |
|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|
| 100 | fresh | 123 ms | 99 ms | −19.5% | 198.2 MB | 176.7 MB | −21.5 MB | 0.5 MB | 0.6 MB |
| 100 | warm | 121 ms | 97 ms | −19.8% | 194.4 MB | 181.7 MB | −12.7 MB | 0.9 MB | 0.9 MB |
| 1000 | fresh | 691 ms | 504 ms | −27.1% | 264.4 MB | 255.7 MB | −8.7 MB | 9.4 MB | 9.4 MB |
| 1000 | warm | 688 ms | 469 ms | −31.8% | 281.1 MB | 256.2 MB | −24.9 MB | 9.5 MB | 9.6 MB |
| 10000 | fresh | 6,375 ms | 4,824 ms | −24.3% | 431.7 MB | 408.0 MB | −23.7 MB | 94.1 MB | 94.1 MB |
| 10000 | warm | 6,500 ms | 3,995 ms | **−38.5%** | 431.1 MB | 423.5 MB | −7.6 MB | 93.6 MB | 93.7 MB |

The other three arms — `workspace-sources-read-all`, `rename-ns-plan-narrow`,
`rename-ns-plan-full-match` — do not go through the outline projection and move
only within noise (`workspace-sources-read-all` N=10000 warm: 203.2 → 206.1 MB
peak, 467 → 476 ms). That is the control: an allocation fix in the outline path
should not move an arm that never calls it, and it does not.

**`held_mb` is unchanged at every N.** MEM-015 is an allocation fix, not a
retention fix. `cli-ls-tree` still retains 94 MB at N=10,000 (9.4 KB/file), and
that is what the aggregate-admission and streaming-ls-tree leaves own.

### Which pass lines moved: none

Both runs are `verdict: FAIL (INCOMPLETE)   exit 1`, with the identical set of
failing lines. Verbatim:

```
BEFORE
  FAIL peak-over-budget {:op :cli-ls-tree, :n 1000, :phase :fresh, :observed 264.4, :limit 247.8}
  FAIL peak-over-budget {:op :cli-ls-tree, :n 1000, :phase :warm, :observed 281.1, :limit 247.8}
  FAIL peak-over-budget {:op :cli-ls-tree, :n 10000, :phase :fresh, :observed 431.7, :limit 247.8}
  FAIL peak-over-budget {:op :cli-ls-tree, :n 10000, :phase :warm, :observed 431.1, :limit 248.2}
  FAIL peak-scales-with-n {:op :cli-ls-tree, :observed 431.7, :limit 313.1, :small-n-observed 281.1, :slack-mb 32}
  FAIL peak-scales-with-n {:op :workspace-sources-read-all, :observed 203.2, :limit 104.3, :small-n-observed 72.3, :slack-mb 32}

AFTER
  FAIL peak-over-budget {:op :cli-ls-tree, :n 1000, :phase :fresh, :observed 255.7, :limit 247.7}
  FAIL peak-over-budget {:op :cli-ls-tree, :n 1000, :phase :warm, :observed 256.2, :limit 247.7}
  FAIL peak-over-budget {:op :cli-ls-tree, :n 10000, :phase :fresh, :observed 408.0, :limit 247.7}
  FAIL peak-over-budget {:op :cli-ls-tree, :n 10000, :phase :warm, :observed 423.5, :limit 248.1}
  FAIL peak-scales-with-n {:op :cli-ls-tree, :observed 423.5, :limit 288.2, :small-n-observed 256.2, :slack-mb 32}
  FAIL peak-scales-with-n {:op :workspace-sources-read-all, :observed 206.1, :limit 105.6, :small-n-observed 73.6, :slack-mb 32}
```

Both runs also report the same four `UNMEASURED reserved-peak-over-budget`
lines: no operation on that branch reports an attributable reserved peak.

Read honestly:

- **Four `peak-over-budget` lines: closer, still red.** The N=1,000 pair moved
  from 8–33 MB over budget to 8 MB over budget — the nearest line is now
  255.7 vs 247.7, a 3.2% miss. The N=10,000 pair is still 160–176 MB over. A
  40% allocation cut buys a single-digit-percent peak cut, because under G1 the
  peak is dominated by what is **live** at once, not by what is allocated and
  immediately dead. That is the same G1 caveat the battery's own doc carries.
- **`peak-scales-with-n` for `cli-ls-tree` got nominally worse, and the reason
  matters.** That line compares the 10,000 peak against the 1,000 peak plus 32
  MB slack. MEM-015 cut the 1,000 peak (281.1 → 256.2) more than the 10,000
  peak (431.1 → 423.5), so the derived limit fell from 313.1 to 288.2 and the
  gap widened from 118.6 to 135.3. **Nothing regressed**; the line is a ratio
  and both terms improved by different amounts. It is the right line to keep —
  it is measuring the thing MEM-015 explicitly does not fix, which is that
  ls-tree holds every outline at once.
- **Output parity at battery scale.** The reference was generated from the
  green code, and the BEFORE run reproduced **every** reference hash for all
  four ops at N = 100 / 1,000 / 10,000 — zero `reference-mismatch` lines. That
  is a third independent differential, after the 160-file suite test and the
  1,000-file corpus digest.

## Gates

On the final commit 46a61d2, run under `/home/forge/bin/suite-run` (the
three-lane unit-suite runner; the battery keeps the exclusive `suite.lock`):

| gate | result | baseline on `origin/main`, same box, same day |
|---|---|---|
| `make mcp-operation-oracle` | pass | pass |
| `clojure -M:clj-surgeon/mcp-test` | 385 tests, 3,967 assertions, **1 failure** | 1 failure |
| `make test-fast` | 702 tests, 5,912 assertions, **5 failures** | 5 failures |

No new failures. The two pre-existing ones are unrelated to this change:

- `mcp-change-buffer-test/exact-profile-compilation-is-project-owned-and-snapshot-bound`
  expects `/opt/homebrew/bin/clj-kondo` and finds `/usr/local/bin/clj-kondo`
  on Linux.
- `agent-routing-test/terminal-response-routing-is-conditional-on-complete-user-work`
  (5 assertions) — `resources/clj-surgeon-agent-routing.md` no longer contains
  any `terminal_response` text, removed by 01f0739.

One failure was caused by this change and fixed before landing: the first draft
had `top-level-form-records`'s 3-arity call its own 4-arity, which
`show_form_test/show-forms-builds-top-level-records-once` counts as two calls
through the var. `make test-fast` reported 6 failures against the 5-failure
baseline; every arity now delegates to one private `parse-and-build-records`.

## Artifacts

- Corpus harness: `/home/forge/tmp/readpath/corpus.clj`, unit probe
  `/home/forge/tmp/readpath/probe.clj`, pre-change overlay
  `/home/forge/tmp/readpath/oldsrc/clj_surgeon/outline.clj`.
- Battery logs: `/home/forge/tmp/readpath/battery2.log` (the clean pair quoted
  above); `/home/forge/tmp/readpath/battery.log` is an earlier pair whose
  reference hashes were copied from another root and are path-dependent — its
  `reference-mismatch` lines are an artifact of that copy, present identically
  in both arms, and its `cli-ls-tree` rows agree with the clean pair.
- Battery scratch branch (local only, not for merge):
  `bridge/read-path-memory-battery` = 46a61d2 + a cherry-pick of 2bae68b.
