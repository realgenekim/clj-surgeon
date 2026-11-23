# streaming-ls-tree 8c1087f (MEM-003) — Sol executed review: NO-GO (memory result real and reproduced; cursor stat-based and unauthenticated; per-page re-walk; pool exceeds its bound) — round 2 launched

NO-GO for the mayor’s merge queue. The memory result is real, but the continuation cursor does not provide the repository-snapshot integrity promised by the EARS/design.

The independent battery reproduced the GREEN result: 1,000-file held heap 9.5/9.5 MB, 10,000-file 9.3/9.6 MB, fresh 10,000-file wall 676 ms, and no `cli-ls-tree held-scales-with-n` failure. A retained-batch control measured 93.45 MB versus 9.35 MB streamed. The battery still failed only on the two pre-existing other-lane retention failures.

Gates reproduced exactly:

- `test-fast`: 749 tests / 6,162 assertions / 0 failures
- MCP test: 387 / 3,982 / 0
- operation oracle: pass, two baseline legacy counterexamples
- battery self-test: 24 / 138 / 0
- Full battery run once under the required lock; receipt: `/home/forge/tmp/membat-sol3/receipts/20260903T091531.724787337Z-battery.edn`

Numbered findings:

1. **BLOCKER — cursor digest is stat-based, not content-based.** [result_budget.clj:109](src/clj_surgeon/result_budget.clj:109), [core.clj:495](src/clj_surgeon/core.clj:495) — replacing a file’s bytes while preserving path, size, and mtime was accepted and page 2 returned the changed namespace; a cursor from another root with identical stats likewise returned different content instead of `:stale-result-cursor`.

2. **BLOCKER — cursor offsets are neither authenticated nor range-checked.** [core.clj:748](src/clj_surgeon/core.clj:748) — a forged valid-digest cursor at offset 99 on a three-record tree returned an empty vector with no receipt, falsely presenting it as a complete result.

3. **FIX — malformed numeric fields can escape as exceptions.** [result_budget.clj:72](src/clj_surgeon/result_budget.clj:72), [result_budget.clj:129](src/clj_surgeon/result_budget.clj:129) — ordinary malformed cursors are typed, but 40-digit offsets and `:max-results` values throw `NumberFormatException` instead of returning the documented typed refusals.

4. **PASS — hard ceiling behavior itself is correct, but its permanent witness is below the hard ceiling.** [ls_tree_budget_test.clj:86](test/clj_surgeon/ls_tree_budget_test.clj:86) — my 1,000/1,001-file probe produced exactly 1,000 complete with no receipt, then a `1000:<64-hex>` continuation or the correct complete-result refusal; the checked-in test exercises a caller-lowered fixture ceiling of 12 rather than server `R=1000`.

5. **PASS — the differential oracle is genuinely encoder-independent.** [ls_tree_memory_test.clj:160](test/clj_surgeon/ls_tree_memory_test.clj:160) — it shares discovery and `safe-outline`, appropriately, but routes through retained `outline-all-files` plus the old formatters rather than either streaming encoder; execution reproduced `src` 69/0 mismatches and `test` 99/0, with text and order equal.

6. **PASS with worker fix — out-of-order completion preserves output order, but concurrency exceeds the declared pool.** [core.clj:465](src/clj_surgeon/core.clj:465), [core.clj:516](src/clj_surgeon/core.clj:516) — on a 73-file large-plus-tiny fixture, tiny files finished first while output remained sorted and batch-equal; nevertheless 32 outlines were simultaneously active against the documented 18-worker pool because chunked `pmap` realization outruns the stated bound.

7. **FIX — every continuation page re-walks the complete manifest.** [core.clj:711](src/clj_surgeon/core.clj:711), [core.clj:723](src/clj_surgeon/core.clj:723) — two 1,000-record pages on the 10,000-file corpus took 1,305 ms and 661 ms; each folded exactly 10,000 stat rows and outlined 1,000 files, confirming `O(pages × N)` discovery/digest work.

8. **PASS — retained heap bound reproduced independently.** [ls_tree_memory_test.clj:104](test/clj_surgeon/ls_tree_memory_test.clj:104) — the 400-file/R=50 fixture retained 1.62 MB, 1.72 MB result-exclusive, versus a recomputed 4.725 MB bound; the unbounded control retained 13.85 MB and after-release growth was −0.10 MB.

9. **FIX — do not scale the materialization window with N.** [core.clj:473](src/clj_surgeon/core.clj:473) — the measured 9–15% cost at 1,000 is acceptable beside the 10,000-file win, but scaling with N would defeat boundedness; use an explicitly bounded executor or unchunked scheduling and test its actual maximum concurrency.

10. **FIX — the central ID ownership table still calls MEM-003 reserved.** [memory-boundedness/design.md:39](docs/intent/memory-boundedness/design.md:39) — the leaf registry correctly owns and implements MEM-003, but the merge-facing table still says `MCP-OP-MEM-002 … 010 | reserved`.

11. **PASS with missing ratchet — the incidental empty-scan fix works but has no witness.** [core.clj:717](src/clj_surgeon/core.clj:717) — direct current-branch CLI returned “No Clojure files found…” with exit 1 instead of the former shadowed-`format` NPE; no test names or executes this boundary.

12. **PASS — scope documentation correctly excludes the MCP entrance.** [read-path-memory-design.md:291](docs/intent/read-path-memory/read-path-memory-design.md:291), [streaming receipt:311](docs/observations/2026-09-03-mem-003-streaming-ls-tree.md:311) — the boundary explicitly assigns adoption to study-ops, though the EARS should be tightened to say “CLI `ls-tree` encoder” because discovery still retains an N-sized path collection.

**Final verdict: NO-GO** until cursor identity covers content or an immutable pinned manifest, offsets and numeric overflow refuse safely, and those counterexamples become linked witnesses.