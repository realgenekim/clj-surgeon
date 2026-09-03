# memory-battery 2bae68b — Sol executed instrument review: GO-WITH-FIX as tooling (round 2 launched)

The instrument is useful, but it can currently report success for an incomplete or invalid run. Verdict: **GO-WITH-FIX as tooling**. The measured operations on main remain **RED**, exactly as expected; that is not the objection.

The fixes needed before merge-queue entry are: make INCOMPLETE block the release gate without calling it FAIL, verify corpus bytes rather than trusting the manifest, bind reference hashes to corpus/code identity, and gate `held_mb` scaling directly.

### Instrument rulings

1. `peak_mb` is honest as a measurement, but not stable enough to be a universal requirement. It is a 5 ms sampled, process-wide used-heap peak containing garbage, and G1 changes it with `-Xmx`. This rerun demonstrates the instability: `cli-ls-tree` N=1,000 fresh fell from 274.8 to 246.5 MB—28.3 MB—and crossed from FAIL to ok for identical work.

   At 512 MiB, the 80% bound is 409.6 MiB, but the actual enforced bound is `start + 224`, approximately 248 MiB. Therefore the documentation’s “about 410 MiB” statement is mathematically wrong.

   The pass-line disposition should be:

   - `oom`: hard gate at the fixed 512 MiB configuration.
   - Output parity: hard gate once the reference is attested.
   - Attributable reserved peak: hard gate once implemented.
   - Over-budget refusal/rollback: hard gate once implemented.
   - `peak-over-budget` and `peak-scales-with-n`: trend/regression signals under identical JVM, collector and heap settings—not proofs of live boundedness.
   - `afterGC_mb` scaling: useful leak signal, but not result/receipt retention.
   - `held_mb` scaling: missing and necessary.

2. `held_mb` is not precisely “result-retained size.” It computes:

   ```text
   after-GC heap while result held − pre-call heap
   ```

   That includes any cache or leak created by the call. Result-exclusive retention is closer to:

   ```text
   held-used − after-release-used
   ```

   and persistent growth should separately be:

   ```text
   after-release-used − start-used
   ```

   The receipt already records enough absolute values to calculate the latter, but the table does not show it. A fixed leak, a leak below 8 MiB, or one established before the 1,000-file cell can pass. Warm-run accumulation can also be absorbed into the next run’s start.

3. UNMEASURED should be separate from FAIL—but must still block a release gate. Internally the code already separates them, yet an all-green-but-unmeasured run returns:

   ```clojure
   {:pass? true :complete? false :exit 0}
   ```

   and prints `PASS (INCOMPLETE)`. That contradicts the requirement that an unobserved line is never a pass. Use three terminal states—PASS, FAIL, INCOMPLETE—and a distinct nonzero release-gate exit for INCOMPLETE. This baseline prints `FAIL (INCOMPLETE)` only because independent peak lines failed.

4. Add this exact MEM-011 line:

   ```text
   max(held_mb at N=10,000) ≤ max(held_mb at N=1,000) + 2.0 MiB
   ```

   The 2.0 MiB slack is derived from this run: it is twice the full-match arm’s 1.0 MiB held value at N=1,000 and ten times the largest 0.2 MiB bounded-arm jitter reported in the baseline, while still catching the measured 8.8 MiB growth to 9.8 MiB.

   That empirical cross-N line belongs in **MCP-OP-MEM-011**. It does not replace the direct serialized receipt/block ceiling owned by **MCP-OP-MEM-001**.

5. The corpus misses all the requested shapes:

   - Cheap now: a `.cljc` profile containing reader conditionals; one 1.9 MiB giant file; shallow token-dense and deeply nested adversarial files as separate arms.
   - Moderate: a 17 KiB-mean real-file profile. It is easy to generate but makes the 10,000-file battery roughly four times heavier.
   - Expensive before admission exists: 450 × 1.9 MiB, about 855 MiB of source. Once aggregate admission exists, it becomes a cheap refusal arm because parsing should never start.

6. `make -n` verifies the current target graph:

   - `test` reaches only `memory-battery-self-test`.
   - That self-test runs the generator self-test and the millisecond verdict tests.
   - `test-fast`, `mcp-test`, and `runtests` contain no full battery invocation.

   The hand-written Make parser remains weaker than `make -n` for variable-expanded or script-hidden invocations, but it is correct for the current graph.

7. Generator determinism self-test passed, and the battery reported all manifests reused in 0–1 ms. The no-op is not trustworthy, however: it checks only `generator-version`, `n`, and the manifest’s claimed file count. It does not verify that files exist, reject extras, or hash their contents. The manifest digest itself contains only path and byte count and is never rechecked. The runner then copies those claims into the table. A missing same-manifest file can therefore make the table say N=10,000 while the operation sees fewer files.

   The 4 GiB reference cache is likewise accepted merely because the file exists. It is not bound to HEAD, generator digest, operation catalogue, JVM, or corpus identity, and the default root is shared across worktrees.

8. No new battery code opens a socket or references a port. The memory targets did not contact any prohibited port during this review. The wider pre-existing Makefile still defaults MCP services to 7888, 7889 and 7890, so those unrelated targets remain forbidden on this seat.

   The battery intentionally writes outside the worktree by default at `/home/forge/tmp/membat`, accepts an arbitrary `MEMBAT_ROOT`, and has no safe-root or marker check. A missing reference also causes `make memory-battery` to launch a sequential 4 GiB reference JVM before the 512 MiB battery JVM. This run reused the existing reference, so exactly one JVM ran.

### Battery rerun—verbatim table

```text
memory battery — Xmx 512m, pass lines {:reserved-peak-mb 192, :peak-headroom-mb 224, :peak-xmx-percent 80, :scale-peak-slack-mb 32, :scale-retained-slack-mb 8, :scale-small-n 1000, :scale-large-n 10000}
peak_mb = continuously sampled process-wide used-heap PEAK (not a post-GC delta); held_mb = after-GC used heap while the result is still referenced, minus start (the receipt's retained size); afterGC_mb = after-GC used heap once the result is released (leak check).
---------------------------------------------------------------------------------------------------------------
op                              N  phase  wall_ms  peak_mb  held_mb afterGC_mb  files      bytes  OOM?  verdict
---------------------------------------------------------------------------------------------------------------
cli-ls-tree                   100  fresh      140    190.9      0.5       23.7    100     404332    no       ok
cli-ls-tree                   100   warm      141    193.9      0.9       23.6    100     404332    no       ok
cli-ls-tree                  1000  fresh      862    246.5      9.5       23.7   1000    4045282    no       ok
cli-ls-tree                  1000   warm      801    271.2      9.4       23.7   1000    4045282    no     FAIL
cli-ls-tree                 10000  fresh     6789    421.9     94.0       24.1  10000   40472773    no     FAIL
cli-ls-tree                 10000   warm     6934    430.5     93.6       24.1  10000   40472773    no     FAIL
rename-ns-plan-full-match     100  fresh      140     71.1      0.1       24.1    100     404332    no       ok
rename-ns-plan-full-match     100   warm      148     72.6      0.1       24.1    100     404332    no       ok
rename-ns-plan-full-match    1000  fresh     1301    192.4      1.0       24.1   1000    4045282    no       ok
rename-ns-plan-full-match    1000   warm     1258    195.7      1.0       24.1   1000    4045282    no       ok
rename-ns-plan-full-match   10000  fresh    11448    202.9      9.8       24.1  10000   40472773    no       ok
rename-ns-plan-full-match   10000   warm    11591    202.7      9.8       24.1  10000   40472773    no       ok
rename-ns-plan-narrow         100  fresh      310     73.6      0.1       24.1    100     404332    no       ok
rename-ns-plan-narrow         100   warm      126     73.6      0.1       24.1    100     404332    no       ok
rename-ns-plan-narrow        1000  fresh     1237    194.4      0.1       24.1   1000    4045282    no       ok
rename-ns-plan-narrow        1000   warm     1305    194.7      0.1       24.1   1000    4045282    no       ok
rename-ns-plan-narrow       10000  fresh    11591    196.4      0.0       24.1  10000   40472773    no       ok
rename-ns-plan-narrow       10000   warm    11526    196.9      0.1       24.1  10000   40472773    no       ok
workspace-sources-read-all    100  fresh       18     29.6      0.4       24.1    100     404332    no       ok
workspace-sources-read-all    100   warm        7     29.5      0.4       24.1    100     404332    no       ok
workspace-sources-read-all   1000  fresh       43     72.0      4.1       24.1   1000    4045282    no       ok
workspace-sources-read-all   1000   warm       49     71.6      4.1       24.1   1000    4045282    no       ok
workspace-sources-read-all  10000  fresh      471    199.8     40.8       24.1  10000   40472773    no       ok
workspace-sources-read-all  10000   warm      459    202.0     40.8       24.1  10000   40472773    no       ok
---------------------------------------------------------------------------------------------------------------
verdict: FAIL (INCOMPLETE)   exit 1
  FAIL peak-over-budget {:op :cli-ls-tree, :n 1000, :phase :warm, :observed 271.2, :limit 247.6}
  FAIL peak-over-budget {:op :cli-ls-tree, :n 10000, :phase :fresh, :observed 421.9, :limit 247.7}
  FAIL peak-over-budget {:op :cli-ls-tree, :n 10000, :phase :warm, :observed 430.5, :limit 248.1}
  FAIL peak-scales-with-n {:op :cli-ls-tree, :observed 430.5, :limit 303.2, :small-n-observed 271.2, :slack-mb 32}
  FAIL peak-scales-with-n {:op :workspace-sources-read-all, :observed 202.0, :limit 104.0, :small-n-observed 72.0, :slack-mb 32}
  UNMEASURED reserved-peak-over-budget {:op :cli-ls-tree, :detail "no operation on this branch reports an attributable reserved peak; the sampled process-wide peak is not a substitute"}
  UNMEASURED reserved-peak-over-budget {:op :rename-ns-plan-full-match, :detail "no operation on this branch reports an attributable reserved peak; the sampled process-wide peak is not a substitute"}
  UNMEASURED reserved-peak-over-budget {:op :rename-ns-plan-narrow, :detail "no operation on this branch reports an attributable reserved peak; the sampled process-wide peak is not a substitute"}
  UNMEASURED reserved-peak-over-budget {:op :workspace-sources-read-all, :detail "no operation on this branch reports an attributable reserved peak; the sampled process-wide peak is not a substitute"}
```

Receipt: `/home/forge/tmp/membat/receipts/20260903T050344.204452590Z-battery.edn`.

Compared with the documented baseline, the conclusions are unchanged: no OOM, no reference mismatch, the same two peak-scaling failures, the same linear held slopes, and reserved peak remains unmeasured. The rerun has five failures rather than six because `cli-ls-tree` N=1,000 fresh moved below the threshold. Held values reproduced almost exactly: 94.0/93.6, 40.8, and 9.8 MiB versus 94.0/93.6, 40.7–41.0, and 9.8 in the baseline. That reproducibility supports `held_mb`; the 28.3 MiB peak swing demonstrates the G1 caveat.

### Merge-queue verdict: GO-WITH-FIX

Main being RED under the battery is expected. The tooling should enter the mayor’s merge queue only after the first four findings below are addressed.

1. [memory_battery.clj:167](src/clj_surgeon/memory_battery.clj:167) — UNMEASURED sets `complete? false` but can still produce `pass? true`, `PASS (INCOMPLETE)`, and exit 0.

2. [generate_tree.clj:137](bench/memory_battery/generate_tree.clj:137) — the no-op trusts manifest claims without verifying actual files, extras, contents, or its path/size-only digest.

3. [Makefile:846](Makefile:846) — reference parity trusts any existing shared `reference-hashes.edn`, with no commit, corpus, generator, or operation-catalogue attestation.

4. [memory_battery.clj:167](src/clj_surgeon/memory_battery.clj:167) — verdict gates `afterGC_mb` but never `held_mb`; the measured 1.0→9.8 MiB full-match result therefore passes.

5. [memory_battery_runner.clj:179](src/clj_surgeon/memory_battery_runner.clj:179) — `held-start` conflates result retention with call-created leaks; record `held-after-release` and `after-release-start` separately.

6. [memory_battery.clj:78](src/clj_surgeon/memory_battery.clj:78) — a sampled G1 used-heap peak is enforced as a hard requirement even though this rerun moved one identical cell by 28.3 MiB across the verdict line.

7. [docs/memory-battery.md:69](docs/memory-battery.md:69) — “about 410 MiB at 512m” is false for the implemented `min`; with the measured start, the enforced limit is about 248 MiB.

8. [memory_battery_runner.clj:45](src/clj_surgeon/memory_battery_runner.clj:45) — all arms use homogeneous small `.clj` files; `.cljc`, deep nesting, a giant file, 17 KiB mean, and 450×1.9 MiB are absent.

9. [memory_battery.clj:297](src/clj_surgeon/memory_battery.clj:297) — the custom closure parser is limited, but `make -n` independently confirmed the full battery is absent from all four ordinary gates.

10. [Makefile:829](Makefile:829) — the default corpus/reference/receipt root is outside and shared among worktrees, and arbitrary overrides have no safe-root marker check.

11. [Makefile:24](Makefile:24) — forbidden port defaults 7888/7889/7890 remain in unrelated pre-existing targets; the new memory targets have no dependency on them and contacted none.