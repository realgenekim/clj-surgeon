NO-GO

Blocking finding F1: the sealed candidate fails its own mandatory battery-fresh landing gate.

`bb test/clj_surgeon/battery_ledger.clj check` returned:

```text
battery-fresh: distance {:commits-behind 3, :raw-commits-behind 3, :ignored-archive-commits 0}
battery-fresh: REFUSED (last-run-failed) -- newest receipt sha 14644aad..., started 2026-09-12T22:04:25Z, wall 161s, verdict :fail.
```

`git show a233ce66a90ae875ccaf8991b1b13e626451bf4b` showed that the tip itself adds this failing ledger entry. Inspection of [battery_parallel.clj](/home/forge/src/clj-surgeon-fence/test/clj_surgeon/battery_parallel.clj) showed `battery-fresh` is a required landing stage and is removed only from prewarm. Consequently, a green prewarm cannot substitute for a passing battery receipt.

The requested pressure checks otherwise held:

- `bb docs/observations/2026-09-12-bbtower-block-b/attempt20/fold.clj` returned `{:namespaces 38, :samples 456, :failed {}}`; the fold uses six samples per runtime, sample SD, and the stated two-SD conservative ratio.
- `bb test/clj_surgeon/bb_ceiling.clj docs/observations/2026-09-12-bbtower-block-b/attempt10/ceiling-run/receipt.edn` returned `{:bb-runtime-sum-ms 240989, :fast-cadence-sum-ms 42143, :ceiling-ms 343102}`.
- Focused merge/split/accounting tests returned `Ran 66 tests containing 336 assertions. 0 failures, 0 errors.` The CLJC fallback preserves platform-local form order and handles empty sides by emitting no forms for that side.
- Lane/portability/refusal witnesses returned `Ran 30 tests containing 1395 assertions. 0 failures, 0 errors.` Runtime and cadence totals are calculated independently and are not added together.
- Inspection of `test/run_all.clj` and the landing manifest showed the 14 battery-cadence moves, including `install-test` and `parser-admission-test`, remain in the landing `test-bb` stage rather than becoming nightly-only.
- Seven targeted JVM probe witnesses returned `{:test 7, :pass 128, :fail 0, :error 0}`, including the intentional inner stale-code failure. Request authorization precedes traversal, dependency discovery completes before reload, and the HTTP crossing applies the 16,384-byte encoded bound.

Required closure: run the counted battery on the repaired tip, commit a passing candidate-bound ledger receipt, and demonstrate that `bb test/clj_surgeon/battery_ledger.clj check` accepts it. This is missing execution evidence rather than a bounded source patch, so it is not suitable for `GO-WITH-FIX`.

> END RECEIPT (fence-run): worktree HEAD at review exit = 8c542edee5060f4674514ff9d2b2aeb3273e98ba = fenced sha.
