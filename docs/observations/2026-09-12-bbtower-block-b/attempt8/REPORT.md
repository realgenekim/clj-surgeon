# Block B attempt 8 — go-with-owed

The measured runtime rule, NEW bb ceiling, probe refusal/boolean contract, feature-thread assignment and required gates are built. Both prewarms passed on the final code commit; no prewarm repair was needed. No fast or integration budget changed. The CLI defect is shared, not bb-only; its explicitly allowed JVM escape is recorded without claiming it repairs the shared output contract. Independent acceptance remains Fable's.

## Findings

The runtime rule reassigns splice-envelope, insert-forms and rename-alias-receipt to JVM; rename-alias remains bb at 6478/3265 = 1.984073506891271, within the 2.0 threshold.

clj-splice's envelope test is 34x slower under bb than the JVM: 834 ms JVM versus 28,591 ms bb (attempt7/a-base and c-subject). That bounds where the babashka-first plan can put heavy rewrite-clj work. This is a finding, not a decision.

The first attempt8 fast sum is 36,944 ms. Its audit-link failure was repaired; the second fast run exposed a command-selection regression, also repaired with 40 coordinator tests / 250 assertions. Neither changed a budget.

CLI stdout has no bb-only leak. A controlled private-index replay prints 26,581 characters on BOTH runtimes against 2,876-character receipts; current-index outputs are 1,279 on both. Raw outputs differ only in receipt-path UUID. Normalized output bytes agree. Unbounded shared workspace-status paths remain owed; the JVM contract escape does not fix them.

The NEW bb ceiling is 399,155 ms: ceil(245,773 * 60,000 / 36,944). This is new intent, not a restored base budget.

Probe now registers seven native-failure refusal rows and its false verification_complete seam as the third verb. Four contract tests passed 84 assertions. Built, not certified.

Encounter scoring was attempted through the installed entrance and refused encounter-not-found receipt-booleans. The installed seed describes the class, but the active ledger has no registration; no score or acceptance was invented.

## Measurements

| Run | Original fast sum ms | All namespace sum ms | Makespan ms | State | Receipt |
|---|---:|---:|---:|---|---|
| a-base | 36335 | 36335 | 27638 | :passed | docs/observations/2026-09-12-bbtower-block-b/attempt7/a-base/receipt.edn |
| b-subject-jvm | 37661 | 37661 | 26957 | :passed | docs/observations/2026-09-12-bbtower-block-b/attempt7/b-subject-jvm/receipt.edn |
| c-subject | 68679 | 302525 | 82699 | :failed | docs/observations/2026-09-12-bbtower-block-b/attempt7/c-subject/receipt.edn |
| test-fast | 36944 | 272720 | 83361 | :failed | docs/observations/2026-09-12-bbtower-block-b/attempt8/test-fast/receipt.edn |
| test-fast-after-cli | 38898 | 275249 | 83069 | :failed | docs/observations/2026-09-12-bbtower-block-b/attempt8/test-fast-after-cli/receipt.edn |

Feature-thread at 9d76115a:

- :jvm: 43280 ms, {:test 69, :pass 2265, :fail 0, :error 0, :type :summary} ([log](feature-jvm.log)).
- :bb: 728265 ms, {:test 69, :pass 2265, :fail 0, :error 0, :type :summary} ([log](feature-bb.log)).

Integration passed: **58804 ms / 240000 ms**, 168 tests / 3798 assertions, zero failures, errors or isolation violations ([log](test-integration.log)).

| Prewarm | Wall ms | Fast sum ms | Integration sum ms | bb suite runtime sum ms | State | Receipt |
|---|---:|---:|---:|---:|---|---|
| 1 | 167657 | 36503 | 77480 | 232195 | :passed | [receipt](prewarm-1/receipt.edn) |
| 2 | 169863 | 40886 | 78499 | 233433 | :passed | [receipt](prewarm-2/receipt.edn) |

Both prewarms have landing? false by contract. Neither used a repair. The bb suite and MCP suite can overlap in namespace membership; their sums are not additive. Full counters, separate bb spans and paths are in report.edn.


[Verbatim gate lines](gate.md), [runtime decisions and unmeasured inventory](reassignments.md), [DOGFOOD every source edit](dogfood.md), [machine report](report.edn).

## Limits and disagreements

Single observations on one box; no variance floor or routing-performance certification.

Step1 uses attempt7 receipts, not new matched controls.

Both tip namespace measurements use 1 GiB; Make workers retain their shipped explicit heap caps.

The historical 11,806-character stdout was not saved, so its exact bytes cannot be claimed reconstructed.

Raw CLI byte identity across separate mutations is prevented by the generated receipt UUID.

Observation files are committed after gate execution, as instructed; the final archive commit is not itself a measured source snapshot.

The CLI output defect is shared and workspace-dependent, not specific to bb.

The bb ceiling charges bb-runtime namespace walls, separately from overlapping cadence budgets.

Independent Fable acceptance and ceiling ratification remain external.

## Owed

Fable: ratify the NEW 399,155 ms bb ceiling and independently review/accept this branch. Neither prewarm grants landing authority.

Shared CLI output defect: workspace_status.unexpected_paths is unbounded. Both runtimes reproduce it with a private historical index. The measured JVM contract escape preserves execution and does not fix that shared behavior. See owed.md and both raw stdout pairs.

Encounter ledger: installed measure encounter score refused encounter-not-found receipt-booleans. Restore/identify the registered encounter and score against properly registered later acceptance; no replacement registration or acceptance was fabricated. See encounter-score.log.

Performance certification, free-choice adoption and full nightly battery freshness are not claimed by this built/not-certified block or by prewarm.

[Shared CLI defect detail](owed.md). No push, tag, shared install or merge. The observation directory is committed last; its SHA is not self-embedded.

## Commits

- `85aff5ac78736be1c2cee30dd80deabf48ec3b98 Declare measured bb runtime eligibility and apply paired fast receipts`
- `04fcbeeaac1ae4b0dbb3a17de2c90f41e9a649a5 Link measured runtime intent at the fast Make entrance`
- `6644a89a11b388e1f09964f2f8094c5b4ca11372 Preserve library inventory when measured runtime selects JVM`
- `e8f21d978afc7126782d22e6933d4581d6d57188 Declare and enforce new measured bb runtime lane ceiling`
- `9d76115ad3a866b3e3b21fe202cd85dc212cb157 Register probe native refusals and third-verb false receipt seam`
- `21a714165ecb2208b455455a4a1c6bbe021be00e Assign feature-thread to JVM from complete paired namespace walls`
