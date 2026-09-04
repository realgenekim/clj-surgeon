# Preserve unrelated imports during selected Var migration

Status: reviewed candidate, ready for a branch commit; public wire and full-suite gates remain pending. Independent GO applies to source SHA-256 `9528d0290648ea0c7ef4ab21ebf91f61563d75774c031fc7ba1a38684633e361`. No release or performance claim.

## Problem and current contract

The 2026-09-04 HTTP probe migrated `example.old/find-event` to `example.new/fetch-event` in a valid client requiring `:refer [find-event other-event]`. The committed client lost the old require and could no longer resolve `other-event`. The repair partitions selected and unrelated imports at the pure planner boundary.

A selected Var migration preserves every unrelated explicit referred name at its original library, including unused imports, metadata-bearing symbols, metadata wrappers on retained refer vectors, original ordering and trivia. It retains the original alias when other qualified uses survive. It removes the selected entry from the old explicit refer vector unless a deliberately retained reader branch still needs it; that exception continues to depend on the old definition. Without that exception, the result works when the old library no longer defines the selected Var. An empty retained refer vector is valid.

Selected calls remain alias-qualified under both policy spellings; refer_policy controls whole-library migration only. Selected-Var :refer :all remains at the original library for its unknown unrelated imports. Reader-discarded entries are ignored when identifying refer symbols, rename keys and libspec option/value pairs; their concrete nodes survive within retained libspecs. Metadata inspection unwraps nodes to token identity without executing readers.

A selected :rename binding is explicitly unsupported: refuse with error_type alias-migration-indirect-reference, reason unsupported-selected-renamed-refer, the file, source_unchanged true, a remedy naming the unsupported binding, no planned files and no executable next_call. This also applies when metadata wraps the rename map or a discarded form appears before the live map. Unrelated rename entries remain supported. Full renamed-binding migration is a separate capability; refusal counts as benchmark noncompletion, never fast success.

## Implementation and ownership

Only src/clj_surgeon/alias_migration.clj, test/clj_surgeon/alias_migration_test.clj, this plan and the owning alias design/specification change. Two pure node helpers support changes to libspec-facts, ns-form-edit and file-plan. Existing scope, count, ownership-refusal, reader-branch and transaction behavior remains. No MCP schema, optional-count API, transaction kernel, benchmark adapter/oracle/preregistration, or main-branch changes.

## Current verification and remaining gates

- Formatter: cached standard-clojure-style on changed source/tests, before targeted tests.
- Pure matrix: 44 tests, 641 assertions, zero failures/errors (repair-fourth-green.txt).
- Independent GO: 36 scenarios; 28 successful plans return baseline-identical values, 26 survive old selected-definition retirement and two are the documented retained-reader exceptions; eight selected-rename refusals have no planned files or next_call. Twelve independent reader-effect probes produced no callbacks or markers, with a working positive trap control. See alias-independent-fourth-verdict.json and alias-repair-independent-review.md.
- Seventeen fresh owned wire fixtures have frozen valid behavior baselines. They cover the original three controls, same-name migrations, refer-all, metadata on symbols/containers, reader discards, unrelated renames and four explicit refusal cases. Parent will start a fresh server pinned to the reviewed candidate; the repair lane will run public HTTP requests, check exact hashes and behavior, remove the selected old definition through edit_clojure for successful cases, and check behavior again.
- Targeted lint result is retained at repair-final-lint.txt. Public wire replay and repository full-suite gates remain pending; candidate status does not imply release readiness.

BB and lint processes run sequentially on CPUs 2,3; no new JVM or server reload during timed calibration. Parent owns server creation and any broader test allocation. Parent reviews and commits only the five owned files on the experiment branch; main stays frozen.

## Evidence chronology and dogfood findings

All receipts are under /var/tmp/forge/astra-program/.

1. Original HTTP failure: alias-adversarial/review.md, with two passing controls and valid baselines. Clean plain-import RED: repair-red-corrected.txt (36 tests / 468 assertions / 33 failures / zero errors). Expanded GREEN: repair-green-matrix2.txt (38 / 520 / zero failures/errors).
2. Independent metadata and renamed-binding failures: alias-independent-behavior-02.json. Clean RED: repair-review-red2.txt (41 / 585 / 36 failures / zero errors); GREEN: repair-review-green.txt (41 / 585 / zero failures/errors).
3. Independent reader-discard regression and metadata-container failures: alias-independent-rereview-behavior-03.json. Clean RED: repair-third-red.txt (43 / 621 / 20 failures / zero errors); GREEN: repair-third-green.txt (43 / 621 / zero failures/errors).
4. Interposed discarded option values: alias-independent-third-behavior-04.json. Clean RED: repair-fourth-red.txt (44 / 641 / 12 failures / zero errors); GREEN: repair-fourth-green.txt (44 / 641 / zero failures/errors). Final source freeze and independent verdict are identified above.

Compact edit_clojure rejected appending multiple test forms and incomplete-form substrings, both with unchanged source. New test/helper insertion used the documented native apply_patch construction fallback; all existing production owner updates used edit_clojure. Initial malformed assertions and one test syntax mistake were corrected before the clean RED receipts listed above. Tool mutation receipts prove applied bytes, not git commits. Call milliseconds retained in these quality probes are not competitive timing measurements.
