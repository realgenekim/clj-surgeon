GO

- Candidate binding: `git rev-parse HEAD` returned `ca749adbf4d61ff6d323ae8e2fc15910e11eef51`; its tree matched the sealed candidate, and `839b7c53…` is an ancestor.
- CLJC semantics: focused merge/split tests passed 25 tests / 61 assertions; downstream analyze, existing-ops, and platform-selector tests passed 39 / 218. Empty sides, platform ordering, round-trip behavior, and consumers accept the non-splicing shape.
- Six-run rule: `bb …/attempt20/fold.clj` returned `{:namespaces 38, :samples 456, :failed {}}`. Inspection confirmed six samples per runtime, sample SD with `n−1`, and `(mean_bb+2sd)/max(1,mean_jvm−2sd)`. Generated table hashes matched the archived before/after hashes.
- Ceiling/accounting: the corrected receipt-bound command returned `{:bb-runtime-sum-ms 240989, :fast-cadence-sum-ms 42143, :ceiling-ms 343102}`. Focused accounting witnesses passed 3 / 24. `union-runs` folds each namespace once; runtime and cadence are independent projections, with no double execution or combined charge.
- Landing cadence: `make --no-print-directory print-gate-stages` listed `battery-fresh` before the landing suites. `bb test/clj_surgeon/battery_ledger.clj check` returned OK for `8c542ede…`, five commits behind HEAD and 1.3 hours old. The 14 battery assignments therefore remain mechanically required for landing, not nightly-only.
- Probe contract: authorization, dependency closure, require-shape, and refusal-completeness witnesses passed 8 / 144. The two printed inner failures were the deliberately induced stale-code assertions. The round-9 servlet class oracle passed 3 / 589.
- HTTP boundary: inspection confirmed `/probe` is loopback-only, mounted only when a probe image is configured, authorizes the canonical target beneath `test/` before traversal, completes discovery before reload, pre-bounds reader recursion, catches `Throwable`, and encodes one UTF-8-bounded EDN response.
- Targeted `~/bin/clj-kondo --lint …` completed with `errors: 0, warnings: 0`.
- Final `git diff --quiet HEAD --` returned `0`. The only remaining item is the pre-existing untracked `docs/observations/bbtower-block-b.md.codex.log`; no candidate bytes were changed.

> END RECEIPT (fence-run): worktree HEAD at review exit = ca749adbf4d61ff6d323ae8e2fc15910e11eef51 = fenced sha.
