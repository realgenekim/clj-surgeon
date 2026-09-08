# Astra row-2 artifact paper-cut repair

Recorded 2026-09-08T09:41:33.993231+00:00. Branch `astra/namespace-split`, base `3d55fa34`.
Branch-only work at `/home/forge/src/clj-surgeon-split`; no push or merge.

## Finding and contract

The six frozen D arms introduced untracked `.clj-surgeon/alias-migration/detail-*.edn`
bookkeeping. The new fail-first Git witness reproduced detail, manifest and inverse
pollution (four failed assertions), then passed after external placement.
Registered `ALIAS-MIGRATION-001/002`: HLD → alias migration design →
`docs/intent/alias-migration/receipt-artifacts-specs.md` and accompanying EDN registry
→ direct annotated witnesses → shared `receipt_artifacts.clj` boundary.
Plan: `docs/plans/row2-receipt-artifacts.md`.

Verb bookkeeping uses `/var/tmp/forge/<verb>-receipts/<workspace-sha256>/`.
Details and inverse paths are absolute; alias library retirement is also external.
Committed Git receipts publish `workspace_clean_except` only after the actual
post-write porcelain command proves no changes outside the verb's file set.
Unrelated dirt and non-Git roots publish explicit unsuccessful/unavailable workspace
status evidence while preserving the actual mutation state. This avoids a false
cleanliness promise over pre-existing dirt. The required path list supersedes the
old constant-size alias receipt claim. N-arm source layout was not changed.

## Writer census and witnesses

| Writer | External destination / witness |
|---|---|
| alias_migration detail, manifest, inverse, retired source | alias-migration-receipts; Git var and whole-library regression, descendant symlink refusal, frozen P1-D replay |
| namespace_split inverse/detail | namespace-split-receipts; real boundary witness with fixture analysis and synchronous proof |
| require_change inverse/detail/compact overflow | require-change-receipts; real boundary and compact evidence persistence witnesses |
| helper_extraction inverse/detail/failure detail | helper-extraction-receipts; complete 51-test helper suite, OS publication-failure rollback and wire detail-path assertions |
| compact/general edit transactions | edit-clojure-receipts; real HTTP and routed workspace tests |
| prepared apply-basis inverse | apply-clojure-changes-receipts; real HTTP prepare/decide/apply test |
| legacy CLI extract! inverse | extract-receipts; extraction and guarded undo suite updated to consume returned path |
| legacy CLI change! inverse | change-receipts; actual handler witness checks external file, source edit and absent requested workspace receipt |
| typist candidates/detail/inverse | typist-receipts; real proof/commit/undo and failure witness suite |
| workspace advisory lock | workspace-lock-receipts; retains existing opt-in cross-process semantics without writing a workspace ignore file |
| admit focused proof report | admit-clojure-patch-receipts; existing snapshot/report-bound proof witnesses |
| cold verification jobs | beneath the workspace-specific external edit receipt directory; identical job IDs in two workspaces cannot overwrite |
| default transaction journals/preimages | transaction-receipts; explicit internal state-home fixture isolation remains supported |

Literal `.clj-surgeon` grep receipts: `artifact-audit-final.txt` in the evidence
folder (initial full writer grep: `/var/tmp/forge/row2-artifact-audit.txt`). Remaining
workspace dotdir references are read-only configuration, ignore/exclusion rules,
lock opt-in detection and historical doc examples; temporary source-adjacent atomic
replacement files are cleaned transaction machinery. Explicit caller-requested
review-plan exports are not receipt/detail/undo publication. Mission ledgers and
non-receipt local-state caches keep their existing interfaces.

## Frozen hand-drive

Fresh detached worktree `/var/tmp/forge/row2-papercut/fixture` was cut from
`/var/tmp/forge/row2/seed-repo` at `6dcdbc9db67a91179b5940a16c16b760583f2603`,
never from the shared checkout. Frozen `/var/tmp/forge/row2/P1-D/request.json` was
rebound only in workspace_root. The actual MCP handler ran in the owned worktree
nREPL image (port 41931), with source namespaces reloaded; no shared service was
restarted. Initial handler setup lacked runtime initialization and returned
server-not-initialized before any mutation; that setup refusal is retained as
`setup-refusal.json`. Initializing the owned handler runtime then ran the unchanged
request successfully. This is a functional repair replay, not a new cohort arm or
first-attempt/performance claim.

Result: committed true, 9 files, 21 sites, alias histogram mjson=9.
Tool-reported elapsed: 4228.543968 ms (not complete verified wall).
`status.txt` contains exactly the nine caller modifications before staging; then
`git add -A` and `candidate.patch` preserve the submitted path set.
`workspace_clean_except` equals those nine callers and status exit is zero.

Stage-2 `/var/tmp/forge/row2/oracle2/run_oracles_v2.sh`:
**O1 PASS, O2 PASS, O3 PASS, O4 PASS, O5 PASS; MINOR 0; ACCEPTED YES.**
Golden and candidate changed lines are both +21/-27. Oracle evidence:
`/var/tmp/forge/row2/astra-papercut/oracle/`, plus `oracles.log`.
Undo: `/var/tmp/forge/alias-migration-receipts/fcf3b5c758daea81ed5699f6665003c9b2585e73d574eb1a17269e7efc93781a/60513083-5e08-420f-a1d8-ae9429488dd8.edn`.
Details: `/var/tmp/forge/alias-migration-receipts/fcf3b5c758daea81ed5699f6665003c9b2585e73d574eb1a17269e7efc93781a/detail-7a22d8b1-57e1-4c85-80cd-407c33e7ed86.edn`.

## Verification and review

- `red.log`: four assertions fail on the original artifact behavior.
- `final-focused.log`: 8 row-2 tests, 42 assertions, zero failures/errors; intent audit ok, violations [].
- `helpers-final.log`: 62 helper/typist tests, 1,264 assertions, zero failures/errors.
- `extra-verbs.log`: extraction tests passed; the initial combined run also caught the obsolete typist CLI path assertion, subsequently repaired and rerun in helpers-final.
- `cli-witness.log`: actual legacy CLI handler witness passes.
- `review.md` and `artifact-review/`: executed adversarial review; retirement descendant symlink, compact proof-field loss and cold-job sharing findings repaired; four independent review witnesses pass (14 assertions).
- Formatter was run through Standard Clojure Style. An attempted selective application disturbed require ordering and one existing large form; these were repaired from HEAD and the bounded intended changes reapplied. Cold parsing, compilation and final suite are the authority over final bytes.
- Earlier make logs retain obsolete location/census expectations and intermediate edit/compile failures. No failed run is presented as passing proof.
- Final make and lint results are appended below after completion. `make test` is the repository landing gate and checks the existing battery freshness ledger; it does not rerun the entire cold battery. The affected boundary suites above were executed separately.

Evidence folder: `/var/tmp/forge/row2-papercut/`. Fresh fixture retained for review.

### Final lint and intent audit

Serialized `~/bin/clj-kondo` over all 30 changed Clojure files: 0 errors,
11 warnings versus 12 in the matched baseline;
16 informational findings versus 15. The only increased
identity/count is one redundant-str informational finding. No new warning or error.
`lint-summary.json` retains the exact comparison. `intent-final.log` reports
`ok true`, `violations []`; `git diff --check` is clean.

CLI follow-up: the named `execute-change-with-receipt!` adapter preserves the shared
transaction lifecycle and its authority; the registry witnesses name that adapter.
Both existing real CLI apply/undo witnesses now consume the emitted receipt path,
clean it in finally, and pass independently (`cli-undo-witnesses.log`).

### Final accepted gate — 2026-09-08T09:51:25.401998+00:00

`make test` exited **0** (`make-test-accepted.log`): **814 JVM tests / 10,372
assertions** and **888 Babashka tests / 7,858 assertions**, zero failures/errors.
Repository hygiene passes. The eight new row-2 battery witnesses also passed
separately in-image. No outstanding correctness finding remains from the executed
review. Final source layout and artifact cleanup changes were included in the
accepted test run. The final gate consumed the end of the requested timebox;
intermediate failures and all corrective iterations remain in the evidence folder.

Final branch commit: `827a751add48fbb8da364fbbf74544e1cf485d99`. Author and committer: forge-anvil <forge-anvil@anvil>. Gene trailer included. Working tree clean after commit. No push or merge.
