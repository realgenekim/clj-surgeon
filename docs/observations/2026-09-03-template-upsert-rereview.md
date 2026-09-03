# curtain-call template-upsert fdccfc8 — Sol executed re-check: GO-WITH-FIX (blank-string id contract; dangling provenance) — round 2 launched

**GO-WITH-FIX.** The merge order remains correct:

`fold → store → settings-lens → template-upsert → lens-followups`

Hold `template-upsert` until the blank-string-ID behavior is explicitly decided and characterized. I recommend treating only `nil`/missing IDs as anonymous; a present blank string should remain raw, preserving the old loop’s behavior.

1. [folds.clj:293](src/cfp_scheduler_killer/folds.clj:293) — Executed blank-ID fixture: two `" "` IDs with one changed field produce **2 rows now versus 1 in the old loop**, an uncharacterized semantic change beyond the documented nil-ID case.

2. [registry.edn:540](docs/intent/registry.edn:540) — EARS says a *present* ID identifies by that ID, while the implementation treats blank strings as absent; either narrow fallback to `nil` or explicitly revise and pin the contract.

3. [fold_relation_policy_test.clj:258](test/cfp_scheduler_killer/fold_relation_policy_test.clj:258) — Exact pre-fix body was red: `1/1/1`; restored HEAD was green: `1/4/0`.

4. [folds.clj:258](src/cfp_scheduler_killer/folds.clj:258) — Executed raw-equality matrix: `"x"`/`"X"` → 2; `5`/`5N` → 1 under Clojure `=`; `5`/`5.0` → 2; `:x`/`"x"` → 2, matching the old raw loop.

5. [comms_templates.clj:21](src/cfp_scheduler_killer/comms_templates.clj:21) — The UI posts `template-id` as a string, and the sole writer trims it and mints a string UUID for nil/blank input; malformed blank/nonnative IDs are replay-only shapes.

6. [fold-characterization.edn:99](test/fixtures/fold-characterization.edn:99) — Parent and HEAD retain byte-identical requested digests: nil-open-case `092f6f50…fc6be` and reachable `c45327bd…935850`.

7. [fold-characterization.edn:152](test/fixtures/fold-characterization.edn:152) — New numeric/string and trailing-space fixtures are present with two-row projections and digests `419425c6…` and `0ea13857…`.

8. [fold_relation_policy_test.clj:669](test/cfp_scheduler_killer/fold_relation_policy_test.clj:669) — Full characterization passed `1/40/0`; every pre-existing digest, including the 3,246-fact judge-sandbox digest, is unchanged. That log contains no `comms.template-saved` facts.

9. [folds.clj:266](src/cfp_scheduler_killer/folds.clj:266) — `blank->nil` still canonicalizes ID-bearing `:person-id` fields here and at [folds.clj:305](src/cfp_scheduler_killer/folds.clj:305); live IDs are string UUIDs, but FOLD-IDEM-002/003 deserve a separate raw-ID audit.

10. [folds.clj:285](src/cfp_scheduler_killer/folds.clj:285) — Four references point to missing `docs/observations/2026-09-03-folddiff-lens-redteam.md`; include that document in the merge stack or replace the dangling provenance.

Validation:

- Unit: `1055 tests, 13168 assertions, 0 failures`
- Compile-check: green, `-Xmx1g`
- Registry suites: `6/278/0`, `2/498/0`, `1/116/0`
- HEAD restored exactly to `fdccfc8581bfd27bc9abc6e8a6464f44bfb031f8`
- Worktree clean; no PostgreSQL or forbidden ports contacted; no commit/stash/push performed.