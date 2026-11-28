# Exhaustive archive-tag catalog — Gene ruling 72 a

Snapshot: 326 local `archive/*` tags, inspected on 2026-11-28. Scope: no git ref was created, moved or fetched for this catalog (all 326 tags were already present); the only files written in the repository are this one, the port plan's freeze marker and the re-review record named next. Outside the repository, the same seat on the same day disabled the weekly alias sentinel's cron line on the Anvil host; the tracked record of that change and its re-review date is `docs/observations/2026-11-28-alias-sentinel-disabled-re-review-2026-12-28.md`.

Comparison baseline: `origin/main` = `90f42b72fbbe98d0b30df2da1a8e4d3af9bbbb79`. Each count is `git rev-list --count origin/main..<tag>` against that pinned baseline, **not** a count of commits authored only for that branch. These long-lived branches often contain thousands of stacked or merged commits. Tip dates below are commit dates (`%cI`, including the stored timezone), not the November archive-tag creation date; annotated tags were peeled to commits.

Evidence method: inspect `git log --format=%s origin/main..<tag>` (first ten subjects), `git diff --name-status origin/main...<tag> -- docs`, and each tag’s tree. Entries quote three subjects and name relevant added observation/plan documents where available. Named `*-result.md`, receipt, ledger, and verdict artifacts under `docs/observations` were searched; related reports and reports outside that directory are also cited. A quoted result is historical and bounded by its named candidate, fixture, and checks; it is not a new verification of the archive tip. Shared battery rows are explicitly labeled. “No branch-specific measured verdict located” reports a search limit, not proof that no measurement ever happened.

Read an archived source with `git show <tag>:<path>`. All source paths and line numbers in an entry refer to **that entry’s tag**, unless stated otherwise. Checkout commands are instructions for the reader; replace `<dir>` with a new worktree path before running them.

Quick locators: [alias migration and namespace split](#archiveastranamespace-split), [q5z alias migration](#archivebridgeq5z-alias-migration), [fan-out fixtures](#archivebridgefanout-fixtures-in-git), [fan-out routing plate](#archivefablefanout-route-plate), [change buffer and hot verification](#archivefablehot-verify-done), [Babashka tower](#archivebb-rewrite-tower), [MCP trunk](#archivemcpmain), [records](#archiverecordsmcp-main).

## Freeze record: alias migration (Gene, 2026-11-28)

Gene's words, verbatim, on the second fleet's answer to "is alias migration exactly the tool we need?":
"Hahaha. On alias migration!  Add to the letter!  I so much want it, but all the data says not useful, even
given what crapper/dryer do!  So tough to chuck it, but it's the right thing to do!" Asked whether freeze
meant delete: "And by freeze Alias migration, it really means \"delete it\", right??  Which seems right."
To the mayor's option a (not ported, not routed, kept only under an archive tag, branch deleted with the
others at cutover): "Oh, yes, frozen off main is good." (ruling 75; the automatic route was suspended
earlier the same day, ruling 64a).

What that means for this catalog. Alias migration is never ported to main, neither as slices nor as a thin
entrance; the port plan `docs/plans/2026-11-22-winning-verbs-port.md` (thirteen stages, thirty-three slices) is marked frozen for it. The
code stays reachable only under `archive/astra/namespace-split` (entry below, with the locator table:
planner `src/clj_surgeon/alias_migration.clj` 1,804 lines; MCP boundary `mcp_alias_migration.clj` 2,854;
tests 1,252 + 108 + 7,301; fixture builder 627) and under `archive/bridge/q5z-alias-migration` (the
earlier q5z line). The second fleet's one caveat: the planner's namespace-repair stages, plan stages 3, 8
and 9 (about 320 lines: `analyze-requires`, the two refusals, `ns-bound-names`, `choose-alias` and the
require rewrite), are the part worth harvesting for any future "move or merge a function" verb; the five
seats found that clean-up demand is "move a function" with a check that the copies resolve their free
symbols the same way, not alias migration.

The record the freeze cites. The weekly sentinel's `oracle=FAIL verdict=SUSPEND` on 2026-10-18 and 10-25
is not evidence of a wrong migration: every surviving output of those runs matches the golden; the oracle
step that said NO and its cause cannot be recovered, because the runs' records under `/var/tmp` are gone
(the thirty-day cleaner is the inference) (`docs/observations/2026-11-28-alias-migration-sentinel-verdict.md`,
pull request 77; fixture `test-fixtures/alias-migration/mvr-5a4b4bc`). The sentinel's cron line on Anvil was
disabled by this seat on 2026-11-28 with re-review 2026-12-28; the tracked record is
`docs/observations/2026-11-28-alias-sentinel-disabled-re-review-2026-12-28.md` (the crontab line verbatim, the
backup's name, the owner and the question to answer on that date), under the mayor's ruling-71 b note. The freeze rests on the
fleets' demand and value findings and on Gene's ruling, not on the sentinel.

## astra/* — 27 tags

The Astra family holds the namespace-split winner and its receipt checkpoints, along with helper comparisons, alias-migration repairs, proof reporting, and gate work. Start with namespace-split below for the two winner implementations; its historical performance evidence is scoped to the recorded fixtures and controls.

### archive/astra/namespace-split

Branch: `astra/namespace-split`. Tip: `7b312f85` · 2026-11-22T20:45:18-08:00. Commits not on main: **2538**.

Holds the namespace-split implementation and its exact-contract experiments, together with the alias-migration planner and MCP surface. The November tip also extracts rewrite-forms and file-plan into named functions through twelve reviewed refactoring slices.

Subjects: “refactor: rewrite-forms and file-plan extracted into named functions in twelve reviewed slices (#47-#58)” · “merge seat/astra-refactor-11 (CI slice 01, the runner-measured budget) into seat/astra-refactor-12” · “merge seat/astra-refactor-10 (CI slice 01, the runner-measured budget) into seat/astra-refactor-11”.

Added observation/plan documents (merge-base comparison): `docs/plans/namespace-split.md`; `docs/observations/sentinel/namespace_split.md`.

Recorded result (scope and candidate as quoted), `docs/observations/2026-09-07-plan-2-cellC-result.md:76`:

> Two consecutive frozen-build reruns: 56/46 s and 56/49 s, byte-identical map and snapshot hashes, identical papercut reports, ÷8–9.7 vs the native median. Load 3.4 → 6.6 during the pair (pilot 3 and Astra running). One caller error (inline JSON to surgeon-call) refused at the wrapper's file guard with zero server contact; rerun clean; aborted clock preserved.

Recorded result (scope and candidate as quoted), `docs/observations/2026-09-06-strictly-better-evidence.md:28`:

> | whole-repository alias migration (Astra replication) | 42.9 s | 31.1 s | 1.38x WIN | 6 controls + 3 pairs | 3/3 and 6/6 |

Recorded result (scope and candidate as quoted), `docs/observations/2026-09-06-strictly-better-evidence.md:34`:

> Alias boundary: the fixture was a FIXED NO-COLLISION synthetic repository (114 files,
> 21 targets, 63 qualified uses, zero collisions), so collision resolution is NOT
> witnessed. The preregistered 1.5x prediction was MISSED (pairwise 1.163x / 1.412x /
> 1.218x). Controls ran serially and pairs concurrently, so the 11.8 s median gap
> against a 6.9 s fresh-control 2SD threshold is an operational threshold, not a formal
> significance claim. Receipts: `2026-09-06-astra-alias-replication.md`.

**Alias-migration locator.** Paths were confirmed with `git ls-tree -r archive/astra/namespace-split`; line counts were taken from `git show archive/astra/namespace-split:<path> | wc -l`.

| Role | Path at this tag | Lines (`wc -l`) |
|---|---|---:|
| Pure planner | `src/clj_surgeon/alias_migration.clj` | 1804 |
| MCP request/transaction boundary | `src/clj_surgeon/mcp_alias_migration.clj` | 2854 |
| Tool registration | `src/clj_surgeon/mcp_tool.clj` | 2576 |
| Server catalog | `src/clj_surgeon/mcp_server.clj` | 373 |
| Planner tests | `test/clj_surgeon/alias_migration_test.clj` | 1252 |
| Characterization tests | `test/clj_surgeon/alias_migration_characterization_test.clj` | 108 |
| MCP boundary tests | `test/clj_surgeon/mcp_alias_migration_test.clj` | 7301 |
| Deterministic corpus/fixture builder | `test/clj_surgeon/alias_migration_fixture.clj` | 627 |
| Additional fan-out fixture generator | `bench/fanout/gen-fanout.clj` | 358 |
| Fan-out acceptance scorer | `bench/fanout/fan_check.clj` | 567 |

The MCP tool name `alias_migration` is registered at `src/clj_surgeon/mcp_tool.clj:2363`. The fixture namespace is executable corpus-building code; inline source fixtures also live in the two test files. Namespace-split code and its exact manifest contract are retained alongside these alias-migration files; the quoted speedups above are historical fixture results. The split report reused the September 7 native controls (408/486 s, median 447 s) for its later waves; these are not fresh matched comparisons of this November tip.

Checkout: `git fetch origin tag archive/astra/namespace-split && git worktree add --detach <dir> archive/astra/namespace-split`

### archive/astra/comment-edits

Branch: `astra/comment-edits`. Tip: `1e57be64` · 2026-09-08T23:19:35Z. Commits not on main: **2494**.

Changes namespace-split comment receipts to track owner and content identity, distinguishing movement, modification, and removal.

Subjects: “Make namespace-split comment receipts an identity diff” · “census: pins recomputed for TEST-ISO-014's six launcher cells (lane manifest + intent ledger)” · “Record the passing 172-second eight-lane battery run”.

Added observation/plan documents (merge-base comparison): `docs/plans/namespace-split.md`; `docs/plans/battery-launcher-matrix.md`.

Shared-history battery result (the row names its measured candidate, not a verdict on this branch’s own changes), `docs/observations/battery-ledger.edn:51`:

> {:sha "5cdd5dcc6875790de0332e29b32876ae50aa1d94", :started "2026-09-08T22:07:03Z", :wall_s 172, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/astra/comment-edits && git worktree add --detach <dir> archive/astra/comment-edits`

### archive/astra/comment-edits-receipt-1e57be64

Branch: `astra/comment-edits-receipt-1e57be64`. Tip: `4ecc5228` · 2026-09-08T23:25:46Z. Commits not on main: **2516**.

Records receipt for 9c1430412967ed49ecaa8c23b7995cef05035faa merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 9c1430412967ed49ecaa8c23b7995cef05035faa merged onto trunk (landing gate battery-fresh)” · “merge: namespace_split: :comment_edits is an owner/content identity diff (moved/changed/removed_occurrence; LIS survivors; lossless compaction) — the positional-alignment defect six callers found (NS-SPLIT-060 amended)” · “Make namespace-split comment receipts an identity diff”.

Added observation/plan documents (merge-base comparison): `docs/plans/namespace-split.md`; `docs/plans/sentinel-intent-gate-and-hld.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:53`:

> {:sha "9c1430412967ed49ecaa8c23b7995cef05035faa", :started "2026-09-08T23:23:17Z", :wall_s 149, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/astra/comment-edits-receipt-1e57be64 && git worktree add --detach <dir> archive/astra/comment-edits-receipt-1e57be64`

### archive/astra/fair-profile-epoch2

Branch: `astra/fair-profile-epoch2`. Tip: `5eeecde2` · 2026-09-05T23:24:51Z. Commits not on main: **1314**.

Repeats the helper comparison with proof-configured, attested controls and records the complete second epoch. The report separates Sol's threshold-clearing gain from Astra's smaller observation.

Subjects: “report complete helper epoch: Sol gain clears floor, Astra does not” · “record first attested epoch two controls as provisional” · “preregister proof-configured helper epoch and withdraw invalid controls”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-05-astra-fair-profile-epoch2-status.md`; `docs/observations/2026-09-05-astra-fair-profile-epoch2-result.md`.

Recorded result (scope and candidate as quoted), `docs/observations/2026-09-05-astra-fair-profile-epoch2-result.md:16`:

> Observed median paired native/tool ratios: **Sol 2.50x; Astra 1.30x**. Ratios of arm medians are 2.32x and 1.29x respectively; these are different estimators, not contradictory results. Headline walls include cold server startup, orientation/model activity, edit, external acceptance and artifact capture. Fixture provisioning and resource reservation precede the clock.

Recorded result (scope and candidate as quoted), `docs/observations/2026-09-05-astra-fair-profile-epoch2-result.md:18`:

> The registered two-SD hurdle matters: Sol floor SD 14.54s (hurdle 29.08s), median paired saving 63.52s — clears. Astra floor SD 5.84s (hurdle 11.69s), median paired saving 10.13s — does NOT clear. Thus Sol establishes a gain on this bounded fixture; Astra has a favorable observation, not a gain established by the registered threshold. This threshold is a preregistered heuristic, not a confidence interval. No 10x claim.

Checkout: `git fetch origin tag archive/astra/fair-profile-epoch2 && git worktree add --detach <dir> archive/astra/fair-profile-epoch2`

### archive/astra/forward-refs-warning

Branch: `astra/forward-refs-warning`. Tip: `92795197` · 2026-09-06T06:35:29Z. Commits not on main: **1566**.

Repairs forward-reference analysis so ordinary analyzer warnings do not become false refusals, while malformed or failed analysis still refuses.

Subjects: “Record final warning repair review and normal owning gates” · “Record combined fixture review and pending serialized gate” · “Share existing analyzer launch for warning and reference contract”.

Added observation/plan documents (merge-base comparison): `docs/plans/forward-reference-warning-analysis.md`; `docs/observations/2026-09-06-forward-reference-warning-independent-review.md`.

Recorded result (scope and candidate as quoted), `docs/observations/2026-09-06-forward-reference-warning-independent-review.md:21`:

> Reviewed retained analyzer-contract-final.log on ba10efbd: owning make analyzer-contract-test ran8tests46assertions,0skips/failures/errors. Builder reports terminal exit0. Together with prior executed production checks and static combined-fixture review, this closes the pending owning-gate evidence. No duplicate run was performed. The fixed five-launch authority remains unchanged; warning proof uses the existing analyzer launch. This GO covers the narrow analyzer repair, not unrelated full gate results.

Checkout: `git fetch origin tag archive/astra/forward-refs-warning && git worktree add --detach <dir> archive/astra/forward-refs-warning`

### archive/astra/friction-matcher-and-cardinality

Branch: `astra/friction-matcher-and-cardinality`. Tip: `a3ce2df1` · 2026-09-07T22:02:43Z. Commits not on main: **2156**.

Fixes structural matching inside reader bodies and encodes cardinality-recovery receipts correctly.

Subjects: “Fix cardinality refusal receipt encoding” · “Fix structural reader-body matches and cardinality recovery receipts” · “report: repair a line mangled by an unquoted heredoc (backticks executed)”.

Added observation/plan documents (merge-base comparison): `docs/plans/friction-matcher-and-cardinality.md`; `docs/plans/sentinel-intent-gate-and-hld.md`.

Shared-history battery result (the row names its measured candidate, not a verdict on this branch’s own changes), `docs/observations/battery-ledger.edn:32`:

> {:sha "9dbbd605e510ecca9ddaf04c097d3bbf9093fca1", :started "2026-09-07T18:25:50Z", :wall_s 844, :verdict :pass, :host "anvil-server"}

Checkout: `git fetch origin tag archive/astra/friction-matcher-and-cardinality && git worktree add --detach <dir> archive/astra/friction-matcher-and-cardinality`

### archive/astra/friction1-receipt

Branch: `astra/friction1-receipt`. Tip: `4b75773c` · 2026-09-07T22:30:10Z. Commits not on main: **2176**.

Records receipt for fable/refusal-text-shape merged tip onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for fable/refusal-text-shape merged tip onto trunk (landing gate battery-fresh)” · “Merge commit 'a3ce2df1ded6c411b5c9376427f384036f961be0' into astra/friction1-receipt” · “plan-2 Cell B pair 2: CLI extract beats native on work time (148 vs 185 s), both correct”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-07-plan-2/friction1-astra-report.md`; `docs/plans/friction-matcher-and-cardinality.md`.

Latest retained battery row (its SHA is the measured candidate; no claim that it verifies this tag tip), `docs/observations/battery-ledger.edn:33`:

> {:sha "4eaabdeaa17277406abd96924b41a6d7f5ca58cf", :started "2026-09-07T22:16:53Z", :wall_s 797, :verdict :pass, :host "anvil-server"}

Checkout: `git fetch origin tag archive/astra/friction1-receipt && git worktree add --detach <dir> archive/astra/friction1-receipt`

### archive/astra/gate-lanes

Branch: `astra/gate-lanes`. Tip: `2dfbc286` · 2026-09-09T08:04:01Z. Commits not on main: **2550**.

Runs landing checks through a bounded shared process coordinator, retaining prewarm receipts and protecting the host-wide worker limit with abstract sockets.

Subjects: “Make the split semaphore unrepresentable: abstract-socket gate slots” · “Refuse a replaced slot root instead of splitting the box semaphore” · “Keep prewarm suite receipts under the parent run directory”.

Added observation/plan documents (merge-base comparison): `docs/plans/parallel-landing-gate.md`; `docs/plans/sentinel-intent-gate-and-hld.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:55`:

> {:sha "8288e1ba5bfb690f185adc4c37cb2126244bf300", :started "2026-09-09T05:28:10Z", :wall_s 150, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/astra/gate-lanes && git worktree add --detach <dir> archive/astra/gate-lanes`

### archive/astra/gate-lanes-receipt-1c762f73

Branch: `astra/gate-lanes-receipt-1c762f73`. Tip: `9cef1721` · 2026-09-09T07:15:12Z. Commits not on main: **2550**.

Records receipt for 1b21571e8948f1b65aa180835318d1be8345bacd merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 1b21571e8948f1b65aa180835318d1be8345bacd merged onto trunk (landing gate battery-fresh)” · “merge: merge: landing gate through the shared bounded process coordinator (510 s → 166 s; box-wide worker admission; consumable prewarm; TEST-ISO-007 ceiling declared)” · “Keep prewarm suite receipts under the parent run directory”.

Added observation/plan documents (merge-base comparison): `docs/plans/parallel-landing-gate.md`; `docs/plans/sentinel-intent-gate-and-hld.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:56`:

> {:sha "1b21571e8948f1b65aa180835318d1be8345bacd", :started "2026-09-09T07:12:44Z", :wall_s 148, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/astra/gate-lanes-receipt-1c762f73 && git worktree add --detach <dir> archive/astra/gate-lanes-receipt-1c762f73`

### archive/astra/gate-lanes-receipt-2dfbc286

Branch: `astra/gate-lanes-receipt-2dfbc286`. Tip: `479222d6` · 2026-09-09T08:08:29Z. Commits not on main: **2552**.

Records receipt for a51dd39e955972ddac1d417d40e73e05f30637c1 merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for a51dd39e955972ddac1d417d40e73e05f30637c1 merged onto trunk (landing gate battery-fresh)” · “merge: merge: landing gate through the shared bounded process coordinator (510 s → 166 s; box-wide worker admission; consumable prewarm; TEST-ISO-007 ceiling declared)” · “Make the split semaphore unrepresentable: abstract-socket gate slots”.

Added observation/plan documents (merge-base comparison): `docs/plans/parallel-landing-gate.md`; `docs/plans/sentinel-intent-gate-and-hld.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:56`:

> {:sha "a51dd39e955972ddac1d417d40e73e05f30637c1", :started "2026-09-09T08:06:00Z", :wall_s 149, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/astra/gate-lanes-receipt-2dfbc286 && git worktree add --detach <dir> archive/astra/gate-lanes-receipt-2dfbc286`

### archive/astra/gate-lanes-receipt-a966ca17

Branch: `astra/gate-lanes-receipt-a966ca17`. Tip: `52ee0851` · 2026-09-09T07:47:05Z. Commits not on main: **2551**.

Records receipt for c127dcbb1b8ef8416f11489b31b4ad50ab14a20f merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for c127dcbb1b8ef8416f11489b31b4ad50ab14a20f merged onto trunk (landing gate battery-fresh)” · “merge: merge: landing gate through the shared bounded process coordinator (510 s → 166 s; box-wide worker admission; consumable prewarm; TEST-ISO-007 ceiling declared)” · “Refuse a replaced slot root instead of splitting the box semaphore”.

Added observation/plan documents (merge-base comparison): `docs/plans/parallel-landing-gate.md`; `docs/plans/sentinel-intent-gate-and-hld.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:56`:

> {:sha "c127dcbb1b8ef8416f11489b31b4ad50ab14a20f", :started "2026-09-09T07:44:35Z", :wall_s 149, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/astra/gate-lanes-receipt-a966ca17 && git worktree add --detach <dir> archive/astra/gate-lanes-receipt-a966ca17`

### archive/astra/gate-lanes-receipt-da88fc24

Branch: `astra/gate-lanes-receipt-da88fc24`. Tip: `6682e150` · 2026-09-09T06:30:22Z. Commits not on main: **2548**.

Records receipt for d9f99298e98db8ffc61a6e35c4f628568fe0c9b0 merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for d9f99298e98db8ffc61a6e35c4f628568fe0c9b0 merged onto trunk (landing gate battery-fresh)” · “merge: merge: landing gate through the shared bounded process coordinator (510 s → 166 s; TEST-ISO-007 ceiling declared)” · “Merge origin/MCP/main into astra/gate-lanes”.

Added observation/plan documents (merge-base comparison): `docs/plans/parallel-landing-gate.md`; `docs/plans/sentinel-intent-gate-and-hld.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:56`:

> {:sha "d9f99298e98db8ffc61a6e35c4f628568fe0c9b0", :started "2026-09-09T06:27:51Z", :wall_s 151, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/astra/gate-lanes-receipt-da88fc24 && git worktree add --detach <dir> archive/astra/gate-lanes-receipt-da88fc24`

### archive/astra/intent-contract-all-prefixes

Branch: `astra/intent-contract-all-prefixes`. Tip: `f61769da` · 2026-09-07T18:08:21Z. Commits not on main: **2127**.

Extends intent-contract checking beyond MCP-OP identifiers and tracks remaining witness debt by identifier so repaired debt must be retired.

Subjects: “Pin intent witness debt by ID and require repaired entries to retire” · “Widen the intent contract beyond MCP-OP prefixes” · “log: usage watch 17:10Z”.

Added observation/plan documents (merge-base comparison): `docs/plans/intent-contract-all-prefixes.md`; `docs/observations/2026-09-07-pair-1-result.md`.

Shared-history battery result (the row names its measured candidate, not a verdict on this branch’s own changes), `docs/observations/battery-ledger.edn:31`:

> {:sha "4992528951d9d443792a12318c88efcaa3a80d78", :started "2026-09-07T10:26:13Z", :wall_s 788, :verdict :pass, :host "anvil-server"}

Checkout: `git fetch origin tag archive/astra/intent-contract-all-prefixes && git worktree add --detach <dir> archive/astra/intent-contract-all-prefixes`

### archive/astra/intent-contract-receipt

Branch: `astra/intent-contract-receipt`. Tip: `11248cb0` · 2026-09-07T18:39:54Z. Commits not on main: **2141**.

Records receipt for fable/refusal-text-shape merged tip onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for fable/refusal-text-shape merged tip onto trunk (landing gate battery-fresh)” · “Merge commit 'f61769da7a93de94b19e0cffc70dba6f7267ad1d' into astra/intent-contract-receipt” · “records: CC LID batch 3 (PR 10, decision for Gene); batch 4 launched”.

Added observation/plan documents (merge-base comparison): `docs/plans/intent-contract-all-prefixes.md`; `docs/observations/2026-09-07-lid-audit/surgeon-batch-1-sol-review-r1.md`.

Latest retained battery row (its SHA is the measured candidate; no claim that it verifies this tag tip), `docs/observations/battery-ledger.edn:32`:

> {:sha "9dbbd605e510ecca9ddaf04c097d3bbf9093fca1", :started "2026-09-07T18:25:50Z", :wall_s 844, :verdict :pass, :host "anvil-server"}

Checkout: `git fetch origin tag archive/astra/intent-contract-receipt && git worktree add --detach <dir> archive/astra/intent-contract-receipt`

### archive/astra/lid-batch-2-perf-gate-hld

Branch: `astra/lid-batch-2-perf-gate-hld`. Tip: `94715053` · 2026-09-07T19:17:16Z. Commits not on main: **2149**.

Adds sentinel intent coverage to the merge gate and writes high-level design sections for the flagship operations.

Subjects: “Gate sentinel intent coverage and append flagship HLD sections” · “merge: intent contract: prefix-agnostic with a per-ID witness-debt ledger that only shrinks; amendment rule stated; TELEMETRY-EVENTS-001 spec row; MCP-OP-TRACE-005 (LID audit batch 1) + battery receipt” · “skills: skill entrance back within 70 lines (fan-out/alias guidance compacted); contract tests green”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-07-lid-audit/mvr-batch-4-hld-astra-report.md`; `docs/plans/sentinel-intent-gate-and-hld.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-09-07-lid-audit/surgeon-batch-1-sol-review-r2.md:1`:

> GO for landing `f61769da` on current `origin/MCP/main` (`ee8b7c93`).

Checkout: `git fetch origin tag archive/astra/lid-batch-2-perf-gate-hld && git worktree add --detach <dir> archive/astra/lid-batch-2-perf-gate-hld`

### archive/astra/namespace-split-receipt

Branch: `astra/namespace-split-receipt`. Tip: `b8b4fc49` · 2026-09-08T14:39:17Z. Commits not on main: **2377**.

Records receipt for 77ac921c merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 77ac921c merged onto trunk (landing gate battery-fresh)” · “Merge commit '77ac921c' into astra/namespace-split-receipt” · “records: recorder v1 = unverified-by-design; condition 2 needs another account”.

Added observation/plan documents (merge-base comparison): `docs/plans/namespace-split.md`; `docs/observations/sentinel/namespace_split.md`.

Latest retained battery row (its SHA is the measured candidate; no claim that it verifies this tag tip), `docs/observations/battery-ledger.edn:39`:

> {:sha "afde6652f16196cbe19e4d20bc5c8e8c08946d23", :started "2026-09-08T14:25:48Z", :wall_s 809, :verdict :pass, :host "anvil-server"}

Checkout: `git fetch origin tag archive/astra/namespace-split-receipt && git worktree add --detach <dir> archive/astra/namespace-split-receipt`

### archive/astra/outline-corpus-lane

Branch: `astra/outline-corpus-lane`. Tip: `e30d2ff1` · 2026-09-06T06:40:38Z. Commits not on main: **1561**.

Moves the growing full-corpus outline-parity witness into the integration lane while retaining bounded fast witnesses and the frozen legacy comparator.

Subjects: “Retain independent outline split review and group integration entry” · “Move full corpus outline parity witness to integration lane” · “Synchronize valid CLI skill examples within the existing size limit”.

Added observation/plan documents (merge-base comparison): `docs/plans/outline-corpus-lane.md`; `docs/observations/2026-09-06-outline-corpus-split-independent-review.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-09-06-outline-corpus-split-independent-review.md:3`:

> **GO for the test-only split at 7e65353f4fff6772132e1be8b64dd76388888217. Clean merged-tree gate remains HOLD pending the landing owner's run.**

Checkout: `git fetch origin tag archive/astra/outline-corpus-lane && git worktree add --detach <dir> archive/astra/outline-corpus-lane`

### archive/astra/production-readiness

Branch: `astra/production-readiness`. Tip: `1f962abc` · 2026-09-05T22:08:31Z. Commits not on main: **1311**.

Records the initial fair helper comparison and the landing evidence for actionable helper-extraction refusals.

Subjects: “record Astra fair comparison native cohort” · “merge: helper_extraction: request-shape refusals carry :field, :decision and a runnable :example” · “battery: receipt for c5e8be8f (563 tests, 12196 assertions, 0 failures, 699 s)”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-05-sol-fence-refexample-r1.md`; `docs/observations/2026-09-05-mission-ledger-sol-caller-probe-2.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:14`:

> {:sha "c5e8be8f0a1bc662ed68b379eb47a2869e0b0f47", :started "2026-09-05T20:03:51Z", :wall_s 699, :verdict :pass, :host "anvil-server"}

Recorded result, `docs/observations/2026-09-05-astra-fair-comparison-result.md:3-8` (the arm table):

> | native / gpt-5.6-sol | 6 accepted | 36.8295 s | 39.702 s | 6/6 |
> | native / gpt-6-astra | 6 accepted | 34.2085 s | 34.605 s | 6/6 |
> | helper_extraction / gpt-5.6-sol | 2 attempted, 2 refused | — | — | 0 accepted; refusal retained |
> | helper_extraction / gpt-6-astra | 0 launched | — | — | no result |

Checkout: `git fetch origin tag archive/astra/production-readiness && git worktree add --detach <dir> archive/astra/production-readiness`

### archive/astra/receipt-facts-2

Branch: `astra/receipt-facts-2`. Tip: `cc1d98d7` · 2026-09-09T02:04:43Z. Commits not on main: **2528**.

Adds namespace-split receipt facts about namespace edits, byte identity, lint deltas, executed loads, and pending proof; restores the skill entrance size contract.

Subjects: “docs: restore the 70-line skill entrance contract” · “split: expose residual review facts and clarify proof state” · “records: row 5 arm E3 — repaired :comment_edits on trunk 571170cc, split answerable/corroboration meter”.

Added observation/plan documents (merge-base comparison): `docs/plans/row2-receipt-artifacts.md`; `docs/plans/mission-git-receipt.md`.

Shared-history battery result (the row names its measured candidate, not a verdict on this branch’s own changes), `docs/observations/battery-ledger.edn:53`:

> {:sha "9c1430412967ed49ecaa8c23b7995cef05035faa", :started "2026-09-08T23:23:17Z", :wall_s 149, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/astra/receipt-facts-2 && git worktree add --detach <dir> archive/astra/receipt-facts-2`

### archive/astra/receipt-facts-2-receipt-b2beec26

Branch: `astra/receipt-facts-2-receipt-b2beec26`. Tip: `10dfb097` · 2026-09-09T01:44:08Z. Commits not on main: **2532**.

Records receipt for e9bf4f8d6d49c5b0cde2935fdc77a61489b39ca1 merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for e9bf4f8d6d49c5b0cde2935fdc77a61489b39ca1 merged onto trunk (landing gate battery-fresh)” · “merge: namespace_split receipt: ns_edits, footprint byte-identity, executed lint delta, executed loads, committed-first text with :proof above the boolean (NS-SPLIT-067..072) — the four facts six of six callers asked for” · “split: expose residual review facts and clarify proof state”.

Added observation/plan documents (merge-base comparison): `docs/plans/row2-receipt-artifacts.md`; `docs/plans/mission-git-receipt.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:54`:

> {:sha "e9bf4f8d6d49c5b0cde2935fdc77a61489b39ca1", :started "2026-09-09T01:41:39Z", :wall_s 149, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/astra/receipt-facts-2-receipt-b2beec26 && git worktree add --detach <dir> archive/astra/receipt-facts-2-receipt-b2beec26`

### archive/astra/receipt-facts-2-receipt-cc1d98d7

Branch: `astra/receipt-facts-2-receipt-cc1d98d7`. Tip: `042ede34` · 2026-09-09T02:08:32Z. Commits not on main: **2533**.

Records receipt for 2e543e6aefb34093bde630c4c31ed5d16e36de9c merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 2e543e6aefb34093bde630c4c31ed5d16e36de9c merged onto trunk (landing gate battery-fresh)” · “merge: namespace_split receipt: ns_edits, footprint byte-identity, executed lint delta, executed loads, committed-first text with :proof above the boolean (NS-SPLIT-067..072); skill mirrors synced” · “docs: restore the 70-line skill entrance contract”.

Added observation/plan documents (merge-base comparison): `docs/plans/row2-receipt-artifacts.md`; `docs/plans/mission-git-receipt.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:54`:

> {:sha "2e543e6aefb34093bde630c4c31ed5d16e36de9c", :started "2026-09-09T02:06:04Z", :wall_s 148, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/astra/receipt-facts-2-receipt-cc1d98d7 && git worktree add --detach <dir> archive/astra/receipt-facts-2-receipt-cc1d98d7`

### archive/astra/rows3-receipt-0956951b

Branch: `astra/rows3-receipt-0956951b`. Tip: `e65e3f94` · 2026-09-08T17:49:21Z. Commits not on main: **2411**.

Records receipt for e04ab8b362982e03520758b50c76afd52998502f merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for e04ab8b362982e03520758b50c76afd52998502f merged onto trunk (landing gate battery-fresh)” · “merge: rows batch 3 — row 1 background proof closure + proof-status; row 5 facts in the receipt, committed facts-only, executable manifest (NS-SPLIT-050..058); Sol GO-WITH-FIX applied” · “fence: Sol's GO-WITH-FIX for a9da4344 — closure binds argv + PID start identity; typed proof-status exits; worker temp root confined; test helper refuses non-temp roots; receipt-size bound in escaped JSON; committed facts-only typed refusals”.

Added observation/plan documents (merge-base comparison): `docs/plans/namespace-split.md`; `docs/observations/2026-09-08-tighten-binding-andon.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:40`:

> {:sha "e04ab8b362982e03520758b50c76afd52998502f", :started "2026-09-08T17:35:38Z", :wall_s 822, :verdict :pass, :host "anvil-server"}

Checkout: `git fetch origin tag archive/astra/rows3-receipt-0956951b && git worktree add --detach <dir> archive/astra/rows3-receipt-0956951b`

### archive/astra/rows3-receipt-6e8891e6

Branch: `astra/rows3-receipt-6e8891e6`. Tip: `dfe64348` · 2026-09-08T18:19:28Z. Commits not on main: **2420**.

Records receipt for 0fc7e37211504c72bd09d8de6c5bb6ce27fca9a2 merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 0fc7e37211504c72bd09d8de6c5bb6ce27fca9a2 merged onto trunk (landing gate battery-fresh)” · “merge: rows batch 3 — row 1 background proof closure + proof-status; row 5 facts in the receipt, committed facts-only, executable manifest (NS-SPLIT-050..059); Sol fence + ruling (a) applied” · “fence: ruling (a) — TMPDIR/-Djava.io.tmpdir stated in :split-ns! help; background-gate-unsafe-tmpdir carries set-java-tmpdir-and-retry next_call before mutation (NS-SPLIT-059)”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-08-tighten-one-shots.md`; `docs/observations/2026-09-08-rows-sublime-3/sol-delta-fence-GO-WITH-FIX-2.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:40`:

> {:sha "0fc7e37211504c72bd09d8de6c5bb6ce27fca9a2", :started "2026-09-08T18:05:47Z", :wall_s 821, :verdict :pass, :host "anvil-server"}

Checkout: `git fetch origin tag archive/astra/rows3-receipt-6e8891e6 && git worktree add --detach <dir> archive/astra/rows3-receipt-6e8891e6`

### archive/astra/rows4-receipt-8b1dd01d

Branch: `astra/rows4-receipt-8b1dd01d`. Tip: `83e20e2f` · 2026-09-08T21:15:51Z. Commits not on main: **2478**.

Records receipt for b6f748cdde9cef0dc1468c97ae91e0c5ad941d43 merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for b6f748cdde9cef0dc1468c97ae91e0c5ad941d43 merged onto trunk (landing gate battery-fresh)” · “merge: rows batch 4 — row 5 transaction negatives as proven receipt facts (comment_edits, stale_references, facades, exactly_once, bodies_preserved) + pending-proof context beside verification_complete (NS-SPLIT-060..066); Sol fence applied” · “fence: Sol's GO-WITH-FIX for a95ac349 — the receipt publishes the facade scan's exclusions (macro expansion, arbitrary forwarding bodies, dynamic resolution); macro counterexample retained as a bounded-scope witness”.

Added observation/plan documents (merge-base comparison): `docs/plans/rows-sublime-4.md`; `docs/observations/2026-09-08-rows-sublime-4/astra-rows-sublime-4-report.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:50`:

> {:sha "b6f748cdde9cef0dc1468c97ae91e0c5ad941d43", :started "2026-09-08T21:12:20Z", :wall_s 211, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/astra/rows4-receipt-8b1dd01d && git worktree add --detach <dir> archive/astra/rows4-receipt-8b1dd01d`

### archive/astra/rows4-receipt-a95ac349

Branch: `astra/rows4-receipt-a95ac349`. Tip: `3b28ecbf` · 2026-09-08T20:50:09Z. Commits not on main: **2472**.

Records receipt for f8cc0c4d312ee1ae30a8397a6e0fe6242c303bb5 merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for f8cc0c4d312ee1ae30a8397a6e0fe6242c303bb5 merged onto trunk (landing gate battery-fresh)” · “merge: rows batch 4 — row 5 transaction negatives as proven receipt facts (comment_edits, stale_references, facades, exactly_once, bodies_preserved) + pending-proof context beside verification_complete (NS-SPLIT-060..066)” · “merge: MCP/main 7d62849a into astra/namespace-split -- two hand-pinned counters, both RECOMPUTED off the merged tree”.

Added observation/plan documents (merge-base comparison): `docs/plans/rows-sublime-4.md`; `docs/observations/2026-09-08-rows-sublime-4/astra-rows-sublime-4-report.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:50`:

> {:sha "f8cc0c4d312ee1ae30a8397a6e0fe6242c303bb5", :started "2026-09-08T20:46:34Z", :wall_s 214, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/astra/rows4-receipt-a95ac349 && git worktree add --detach <dir> archive/astra/rows4-receipt-a95ac349`

### archive/astra/telemetry-helper-and-comparison

Branch: `astra/telemetry-helper-and-comparison`. Tip: `7e462745` · 2026-09-05T15:02:37Z. Commits not on main: **1249**.

Adds public-wire telemetry for helper extraction and preregisters a fair comparison against native work.

Subjects: “docs: preregister fair helper comparison” · “test: witness helper telemetry at public wire” · “fix: emit telemetry for public helper calls”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-05-astra-fair-comparison-prereg.md`; `docs/plans/helper-closure-extraction.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:13`:

> {:sha "9e67856aa496e5f8f07f23522f7d8eea652078dc", :started "2026-09-05T14:36:53Z", :wall_s 717, :verdict :pass, :host "anvil-server"}

Checkout: `git fetch origin tag archive/astra/telemetry-helper-and-comparison && git worktree add --detach <dir> archive/astra/telemetry-helper-and-comparison`

### archive/astra/typist-route

Branch: `astra/typist-route`. Tip: `dc33eba2` · 2026-09-06T21:34:42Z. Commits not on main: **1833**.

Preserves typist and alias-replication experiments, portable executable specimens, and real-task process traces, including instrumentation and provenance limits.

Subjects: “Record real-task process trace and clean instrumentation overhead comparison” · “Make archived specimen reproduction portable and preserve original fixture bytes” · “Record executable-specimen hand-drive and real-session measurement limits”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-06-sol-fence-astra-typist-route-r3-delta.md`; `docs/observations/2026-09-06-sol-fence-astra-typist-route-r2.md`.

Shared-history battery result (the row names its measured candidate, not a verdict on this branch’s own changes), `docs/observations/battery-ledger.edn:22`:

> {:sha "2bba421fe88ef758d4d08f96907e442f7a8ce804", :started "2026-09-06T11:49:21Z", :wall_s 851, :verdict :pass, :host "anvil-server"}

Checkout: `git fetch origin tag archive/astra/typist-route && git worktree add --detach <dir> archive/astra/typist-route`

## MCP/* — 7 tags

MCP/main is the archived integration trunk the sentinels ran. The sibling branches retain gate composition, Astra performance studies, binding repairs, and usage-collector fixes; none is the current origin/main.

### archive/MCP/main

Branch: `MCP/main`. Tip: `454a2863` · 2026-09-24T18:29:51Z. Commits not on main: **2907**.

Preserves the MCP trunk that ran the sentinels and accumulated the September feature and battery landings. Its final commits make the Codex Surgeon registration optional during onboarding.

Subjects: “merge: merge: onboarding writes the Codex Surgeon entry as optional (clj-surgeon-7h2) ; battery receipt 205b6188” · “Merge commit '205b6188' into skiff-worker/7h2-onboarding-required-false” · “test: census names managed-block-never-gates-codex-startup”.

Added observation/plan documents (merge-base comparison): `docs/plans/diff-impact-executor.md`; `docs/observations/2026-09-15-diff-impact-executor-report-astra.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:83`:

> {:sha "cdc18278bc9467d52fed2bff6bbef375259ccbf5", :started "2026-09-24T18:21:11Z", :wall_s 194, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

**Sentinel/trunk locator.** The tree retains `docs/observations/sentinel/` and `docs/observations/battery-ledger.edn`; use `git ls-tree -r --name-only archive/MCP/main -- docs/observations/sentinel` to enumerate the frozen sentinel record.

Checkout: `git fetch origin tag archive/MCP/main && git worktree add --detach <dir> archive/MCP/main`

### archive/MCP/astra-binding-discard-2026-09-05

Branch: `MCP/astra-binding-discard-2026-09-05`. Tip: `40e5fa5a` · 2026-09-05T03:00:22Z. Commits not on main: **957**.

Repairs alias migration around sequential bindings, destructuring defaults, and discarded forms, with binding-scope regression witnesses.

Subjects: “Link sequential binding regression to its intent specification” · “fix: use an available timeout command for Claude benchmark runs” · “Document bounded structural recovery for declare and definition ambiguity”.

Added observation/plan documents (merge-base comparison): `docs/plans/alias-migration-binding-scope-repair.md`; `docs/plans/alias-migration-mixed-refer-repair.md`.

Recorded result (scope and candidate as quoted), `docs/observations/2026-09-05-astra-portable-timeout.md:9`:

> After this shell fix, the actual fallback self-test passed in1.252s: fast child completed, failing child retained exit7, stalled child timed out independently. Independent fake-only review also proved preferred gtimeout from a directory containing spaces (all three exact invocations recorded) and the precise missing-binary127 diagnostic. No real model or JVM was launched by these self-tests.
> 
> All four previously unrun Make tail commands then passed: retained-result self-test and tracked verification, evidence-manifest self-test and verification. This is segmented completion of the required recipe after the localized repair, not a claim that either original full invocation exitedzero.

Checkout: `git fetch origin tag archive/MCP/astra-binding-discard-2026-09-05 && git worktree add --detach <dir> archive/MCP/astra-binding-discard-2026-09-05`

### archive/MCP/astra-helper-performance-2026-09-05

Branch: `MCP/astra-helper-performance-2026-09-05`. Tip: `cae49e64` · 2026-09-05T14:37:58Z. Commits not on main: **971**.

Records the four-hour helper-extraction program, its caller studies, public-wire checks, and handoff. It retains stopped or invalid controls rather than treating them as speed evidence.

Subjects: “docs: refresh Astra Gene report and weekly headroom” · “State helper report board at the four-hour window close” · “Publish Astra four-hour helper report, evidence, and Fable handoff”.

Added observation/plan documents (merge-base comparison): `docs/observations/evidence/astra-helper-2026-09-05/usability-review.md`; `docs/observations/evidence/astra-helper-2026-09-05/root-oracle-ruling.md`.

Recorded result (scope and candidate as quoted), `docs/observations/2026-09-05-astra-gene-report-1430z.md:3`:

> **1. Headline.** **12,164 assertions across 560 battery tests pass** on `e1239a99`; Sol’s seventh boundary review says **GO**, closing the outstanding finding. Events to the contrary: no new accepted performance comparison; landing is underway, not yet confirmed complete. Receipts: `/var/tmp/forge/battery-helper-v7-1003.log`; `f70e72bb:docs/observations/2026-09-05-helper-extraction-fence-review-r7.md`.

Checkout: `git fetch origin tag archive/MCP/astra-helper-performance-2026-09-05 && git worktree add --detach <dir> archive/MCP/astra-helper-performance-2026-09-05`

### archive/MCP/astra-mcp-wrapper-usage

Branch: `MCP/astra-mcp-wrapper-usage`. Tip: `7c19e5ca` · 2026-09-05T06:58:49Z. Commits not on main: **961**.

Makes usage studies recognize explicitly registered read-only MCP wrappers, then restricts registration to the fixed public wrapper path.

Subjects: “fix: constrain usage registration to fixed public wrapper path” · “fix: register pinned public quality wrapper usage attempts” · “fix(study): recognize explicitly registered read-only MCP wrapper calls”.

Added observation/plan documents (merge-base comparison): `docs/plans/registered-mcp-read-usage.md`; `docs/plans/alias-migration-mixed-refer-repair.md`.

Retained application quality result (not a measurement of the collector patch), `docs/observations/2026-09-05-astra-application-extraction.md:3`:

> 2026-09-05. One existing public `apply_clojure_changes` transaction moved six
> response helpers into a new namespace and rewrote 258 selected uses in 28 caller
> files. The reviewed candidate passes the focused oracle: exactly 30 changed
> files, all caller linkage and preservation checks, exact moved/retained owner
> multiplicity and bodies, and 24 baseline-identical JVM helper cases. The 172
> unrelated uses remain unchanged. Independent final audit returned bounded GO.

Checkout: `git fetch origin tag archive/MCP/astra-mcp-wrapper-usage && git worktree add --detach <dir> archive/MCP/astra-mcp-wrapper-usage`

### archive/MCP/astra-performance-2026-09-04

Branch: `MCP/astra-performance-2026-09-04`. Tip: `652459fa` · 2026-09-05T03:47:31Z. Commits not on main: **958**.

Preserves the Astra performance cohort, application extraction hand-drive, binding repairs, and final gate report.

Subjects: “docs: publish Astra performance program report and final gate status” · “Record Astra application hand-drive and binding quality gates” · “Record audited follow-up adoption and Astra API findings”.

Added observation/plan documents (merge-base comparison): `docs/plans/2026-09-04-astra-performance-program.md`; `docs/plans/alias-migration-mixed-refer-repair.md`.

Recorded result (scope and candidate as quoted), `docs/observations/2026-09-05-astra-binding-quality.md:20`:

> ## 2026-09-05T03:34:12.719957+00:00 — Astra full candidate gate PASS
> 
> Commit40e5fa5ac1b1c4cacbb67287da95bb7eaabe0d31 adds only the missing @spec65 comment to its existing witness. Independent review confirms unchanged production bytes, assertions, fixture/spec/audit behavior, and test linkage. The cheap intent audit passed first, followed by the complete original make test recipe:859BBtests/7335assertions,6analyzer tests/25assertions,919MCPtests/15214assertions, and all remaining postchecks/selftests/evidence tail. Return0 at2026-09-05T03:19:08.150346Z;1076.9996seconds quality wall. Source worktree clean. Receipt: /var/tmp/forge/astra-program/verification-binding40-active8/result.json and completion.md. No native speed comparison follows from this full-suite wall.
> 
> Public wire remains historically bound to46e and identical production SHA1c4b; it is not relabeled as a new40e execution. Fable is assembling the reviewed binding series with the approved test-default/budget changes and independently reviewed client collector fix for one merged-tree gate. Publicmain remains frozen.

Checkout: `git fetch origin tag archive/MCP/astra-performance-2026-09-04 && git worktree add --detach <dir> archive/MCP/astra-performance-2026-09-04`

### archive/MCP/astra-usage-server-alias-2026-09-05

Branch: `MCP/astra-usage-server-alias-2026-09-05`. Tip: `29b8466d` · 2026-09-05T03:19:13Z. Commits not on main: **958**.

Corrects usage accounting so registered Surgeon client aliases are counted, alongside the application and binding-quality observations.

Subjects: “fix: count registered Surgeon client aliases in usage studies” · “Record Astra application hand-drive and binding quality gates” · “Record audited follow-up adoption and Astra API findings”.

Added observation/plan documents (merge-base comparison): `docs/plans/alias-migration-mixed-refer-repair.md`; `docs/observations/evidence/astra-primary-2026-09-05/report.md`.

Retained application quality result (not a measurement of the collector patch), `docs/observations/2026-09-05-astra-application-extraction.md:3`:

> 2026-09-05. One existing public `apply_clojure_changes` transaction moved six
> response helpers into a new namespace and rewrote 258 selected uses in 28 caller
> files. The reviewed candidate passes the focused oracle: exactly 30 changed
> files, all caller linkage and preservation checks, exact moved/retained owner
> multiplicity and bodies, and 24 baseline-identical JVM helper cases. The 172
> unrelated uses remain unchanged. Independent final audit returned bounded GO.

Checkout: `git fetch origin tag archive/MCP/astra-usage-server-alias-2026-09-05 && git worktree add --detach <dir> archive/MCP/astra-usage-server-alias-2026-09-05`

### archive/MCP/gate-landing-2026-09-04

Branch: `MCP/gate-landing-2026-09-04`. Tip: `38609525` · 2026-09-04T03:57:39Z. Commits not on main: **348**.

Composes the admit gate onto MCP/main and records its base, conflict resolutions, held branches, and executed gates.

Subjects: “docs: MCP/main moved 5 docs-only commits during the composition — dry-run merge onto 48fbd48 is clean” · “docs: repair the branch table (the tip row had split it in two)” · “docs: name the pushed tip in the landing record (fc2efb6; code tip 1bb0c9d)”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-04-gate-landing-composition.md`; `docs/observations/mem003-second-landing-round1-review-sol.md`.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. The commit record reports: “GREEN rung 5: the admit path's no-OOM bound, measured, at 512 MiB”.

Checkout: `git fetch origin tag archive/MCP/gate-landing-2026-09-04 && git worktree add --detach <dir> archive/MCP/gate-landing-2026-09-04`

## bridge/* — 46 tags

The September bridge program spans the admit gate, relation census, fan-out fixtures, transaction journal, streaming ls-tree, rf2 extraction and rewiring, study operations, and helper extraction. Integration and review branches preserve failures and repair rounds as well as successful checks.

### archive/bridge/admit-gate

Branch: `bridge/admit-gate`. Tip: `aa82b427` · 2026-09-04T03:26:35Z. Commits not on main: **23**.

Builds the patch-admission gate, including typed Throwable handling and a measured memory-bound rung at a 512 MiB heap.

Subjects: “GREEN rung 5: the admit path's no-OOM bound, measured, at 512 MiB” · “RED rung 5: the admit path's 16 MiB memory bound is an argument, with no arm to make it a receipt” · “GREEN rung 4: the handler's edge catches Throwable and names the heap”.

No added observation/plan Markdown documents in the merge-base diff.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. The commit record reports: “GREEN rung 5: the admit path's no-OOM bound, measured, at 512 MiB”.

Checkout: `git fetch origin tag archive/bridge/admit-gate && git worktree add --detach <dir> archive/bridge/admit-gate`

### archive/bridge/admit-gate-r16

Branch: `bridge/admit-gate-r16`. Tip: `09660168` · 2026-09-04T23:44:48Z. Commits not on main: **945**.

Preserves later admit-gate repair rounds, owned overlay witnesses, and the halted round-eighteen checkpoint whose full gates had not yet rerun.

Subjects: “fix: the round-eighteen witnesses own the overlays they ask for — the lane's temp-leak ratchet caught them” · “records: round-eighteen checkpoint under SUSPEND-ALL — three blockers RED->GREEN, gate set not yet re-run” · “green: MCP-OP-ADMIT-161/162 -- the four non-blocking findings, closed”.

Added observation/plan documents (merge-base comparison): `docs/observations/admit-gate-round18-build-record.md`; `docs/observations/admit-gate-round9-review-sol.md`.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. The commit record reports: “records: round-eighteen checkpoint under SUSPEND-ALL — three blockers RED->GREEN, gate set not yet re-run”.

Checkout: `git fetch origin tag archive/bridge/admit-gate-r16 && git worktree add --detach <dir> archive/bridge/admit-gate-r16`

### archive/bridge/admit-gate-r3

Branch: `bridge/admit-gate-r3`. Tip: `97157953` · 2026-09-04T18:18:07Z. Commits not on main: **909**.

Integrates the admit gate with the evolving MCP trunk and repairs semantic failures caused by the changed refusal-kind vocabulary.

Subjects: “fix: the merge's four semantic failures — the entrance's kind vocabulary moved” · “merge: origin/MCP/main (census landing) into bridge/admit-gate-r3” · “verdict: admit gate r14 Opus GO-WITH-FIX (merge red: trunk kind vocabulary drift); r15 merge round launched”.

Added observation/plan documents (merge-base comparison): `docs/observations/admit-gate-round14-review-opus.md`; `docs/observations/admit-gate-round9-review-sol.md`.

Recorded round-14 review; subsequent tip subjects describe merge repairs, `docs/observations/admit-gate-round14-review-opus.md:1`:

> ## GO-WITH-FIX

Checkout: `git fetch origin tag archive/bridge/admit-gate-r3 && git worktree add --detach <dir> archive/bridge/admit-gate-r3`

### archive/bridge/alias-migration-bound-to-target

Branch: `bridge/alias-migration-bound-to-target`. Tip: `b3a8371c` · 2026-09-07T09:39:19Z. Commits not on main: **2087**.

Restricts alias reuse to the requested target library and alias policy, preserves comments and discarded forms in libspec removal, and adds direct refusal-locator witnesses.

Subjects: “alias lane: clear two pre-existing unused-binding warnings in touched test files so the lint gate exits 0” · “alias_migration round 3: reuse restricted to alias_policy entries (unrestricted existing fallback removed); execute!-path witnesses for policy-restricted reuse and comment/#_ preservation; LLD alias-choice section aligned; lane manifest +3” · “alias_migration round 2: reuse decided on :as aliases only (referred Vars are collisions, never reuse); libspec removal stops at comments and #_ discards; MCP-OP-ALIAS-067 added, 007/008 aligned, 066 annotation fixed; locator witness asserts the refusal and file:line directly”.

Added observation/plan documents (merge-base comparison): `docs/plans/alias-migration-mixed-refer-repair.md`; `docs/plans/alias-migration-binding-scope-repair.md`.

Shared-history battery result (the row names its measured candidate, not a verdict on this branch’s own changes), `docs/observations/battery-ledger.edn:30`:

> {:sha "e0c3bc79d80f6dd3d0871182bded222b4ffd68c6", :started "2026-09-06T23:42:04Z", :wall_s 788, :verdict :pass, :host "anvil-server"}

Checkout: `git fetch origin tag archive/bridge/alias-migration-bound-to-target && git worktree add --detach <dir> archive/bridge/alias-migration-bound-to-target`

### archive/bridge/andon-find-build-files-argv

Branch: `bridge/andon-find-build-files-argv`. Tip: `32c0c7fc` · 2026-09-03T21:17:15Z. Commits not on main: **0**.

Replaces shell-string build-file discovery with argument vectors, handles symlinked roots, and records the corrected injection analysis and source ratchet.

Subjects (tip history; no commits outside main): “docs(andon): correct the argument-injection claim; record -H, the shadow, bfs” · “andon(ratchet): source-level witness that no src/ file hands a string to a shell” · “andon(green): -H on both find argvs so a symlinked root is actually descended”.

No added observation/plan Markdown documents in the merge-base diff.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/bridge/andon-find-build-files-argv && git worktree add --detach <dir> archive/bridge/andon-find-build-files-argv`

### archive/bridge/anvil-arms-apparatus

Branch: `bridge/anvil-arms-apparatus`. Tip: `89295d85` · 2026-09-03T22:39:34Z. Commits not on main: **75**.

Hardens the Anvil experiment runner using defects exposed by live arms and a runtime self-test tally rather than reconstructed source checks.

Subjects: “anvil-arms: three defects the first LIVE arm found, none of which the fake driver could” · “self-test: replace the per-case PASS/FAIL grep pairs with a `tally` function + runtime ledger; case 45 reads no source text” · “fix(anvil-arms): case45 validates the tally's own id+file, never an idealised reconstruction (Sol round eight, decisive defect)”.

No added observation/plan Markdown documents in the merge-base diff.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. The commit record reports: “self-test: replace the per-case PASS/FAIL grep pairs with a `tally` function + runtime ledger; case 45 reads no source text”.

Checkout: `git fetch origin tag archive/bridge/anvil-arms-apparatus && git worktree add --detach <dir> archive/bridge/anvil-arms-apparatus`

### archive/bridge/apply-expect-honoured

Branch: `bridge/apply-expect-honoured`. Tip: `4711be05` · 2026-09-07T10:05:30Z. Commits not on main: **2104**.

Repairs expected-count enforcement and the contract for computed programs accompanying literal edits or owner deletion, with typed refusal guidance.

Subjects: “expect guard round 5: MCP-OP-EDIT-042 narrowed (programs only alongside edits/delete_owners); programs-without-companion refuses programs-require-a-companion-gesture at [programs] with accepted [edits delete_owners] (was non-empty-array at [changes]); lint-only let flatten in mcp_compact_location_test” · “merge origin/MCP/main into bridge/apply-expect-honoured — lane manifest pins recomputed” · “records: expect r4 — no functional defect, lint + merge conflict; round 5”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-07-expectfix-r4-sol-review.md`; `docs/observations/2026-09-07-expectfix-r3-sol-review.md`.

Shared-history battery result (the row names its measured candidate, not a verdict on this branch’s own changes), `docs/observations/battery-ledger.edn:30`:

> {:sha "e0c3bc79d80f6dd3d0871182bded222b4ffd68c6", :started "2026-09-06T23:42:04Z", :wall_s 788, :verdict :pass, :host "anvil-server"}

Checkout: `git fetch origin tag archive/bridge/apply-expect-honoured && git worktree add --detach <dir> archive/bridge/apply-expect-honoured`

### archive/bridge/astra-integration

Branch: `bridge/astra-integration`. Tip: `f49dece3` · 2026-09-05T01:21:19Z. Commits not on main: **1053**.

Integrates Astra alias-migration and harness repairs, preserving unrelated imports and choosing an available timeout command.

Subjects: “fix: use an available timeout command for Claude benchmark runs” · “fix: preserve unrelated imports in selected Var migrations” · “log: Opus harness round two (afe6621f)”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-03-integration-branch.md`; `docs/plans/alias-migration-mixed-refer-repair.md`.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/bridge/astra-integration && git worktree add --detach <dir> archive/bridge/astra-integration`

### archive/bridge/astra-telemetry-reland

Branch: `bridge/astra-telemetry-reland`. Tip: `91f9d69d` · 2026-09-05T15:30:19Z. Commits not on main: **1257**.

Relands public helper-call telemetry and its wire witness with the fair-comparison preregistration.

Subjects: “docs: preregister fair helper comparison” · “test: witness helper telemetry at public wire” · “fix: emit telemetry for public helper calls”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-05-astra-fair-comparison-prereg.md`; `docs/plans/helper-closure-extraction.md`.

Shared-history battery result (the row names its measured candidate, not a verdict on this branch’s own changes), `docs/observations/battery-ledger.edn:13`:

> {:sha "9e67856aa496e5f8f07f23522f7d8eea652078dc", :started "2026-09-05T14:36:53Z", :wall_s 717, :verdict :pass, :host "anvil-server"}

Checkout: `git fetch origin tag archive/bridge/astra-telemetry-reland && git worktree add --detach <dir> archive/bridge/astra-telemetry-reland`

### archive/bridge/census-verb

Branch: `bridge/census-verb`. Tip: `2d82a677` · 2026-09-04T15:08:08Z. Commits not on main: **800**.

Builds and hardens the relation-census verb, including nesting limits on build-file readers and corrections to the evaluation-fence oracle.

Subjects: “GREEN: the same nesting ceiling governs a build file's bytes” · “RED: the build-file read has no depth bound and takes the Throwable exit” · “correct the oracle: it fenced two evaluators and named eight”.

Added observation/plan documents (merge-base comparison): `docs/observations/census-round21-review-opus.md`; `docs/observations/census-round19-review-opus.md`.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. The commit record reports: “GREEN: the same nesting ceiling governs a build file's bytes”.

Checkout: `git fetch origin tag archive/bridge/census-verb && git worktree add --detach <dir> archive/bridge/census-verb`

### archive/bridge/ci-findings

Branch: `bridge/ci-findings`. Tip: `49b92905` · 2026-09-04T22:49:29Z. Commits not on main: **934**.

Repairs command resolution through PATH and refusal propagation across callers, then records the capacity-order halt and remaining work.

Subjects: “checkpoint: halted under the capacity order -- what is green, what remains” · “green: item1 blast radius -- callers translate the resolver refusal; pin 146” · “green: item1 -- expand-command resolves through PATH, refuses typed”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-04-ci-findings-checkpoint.md`; `docs/observations/2026-09-04-suite-spike-spec.md`.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. The commit record reports: “records: GHA round one shipped (bridge/gha 5ce8aaea) — gate green 205 s on runners; three CI-only defects to round three; default-branch escalation”.

Checkout: `git fetch origin tag archive/bridge/ci-findings && git worktree add --detach <dir> archive/bridge/ci-findings`

### archive/bridge/close-losers

Branch: `bridge/close-losers`. Tip: `205e13ad` · 2026-09-02T14:28:13Z. Commits not on main: **1**.

Closes measured losing edit shapes with typed refusals, expected-post-image drift checks, and a fail-closed splice guard.

Subjects: “close the measured loser shapes at the server: typed refusals, drift against the expected post-image, fail-closed splice guard (MCP-OP-CLOSE-001..022)”.

No added observation/plan Markdown documents in the merge-base diff.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/bridge/close-losers && git worktree add --detach <dir> archive/bridge/close-losers`

### archive/bridge/fanout-fixtures-in-git

Branch: `bridge/fanout-fixtures-in-git`. Tip: `f2fa8be9` · 2026-09-04T05:22:13Z. Commits not on main: **19**.

Keeps the fan-out generator, scorers, sabotage checks, and self-test roster in Git, including irregularity fixtures and filename-boundary documentation.

Subjects: “docs: document the walk's non-UTF-8 filename limit (finding 9, non-blocking)” · “build: add make fanout-selftests, iterating the self-test roster as a gate” · “fix: widen the self-test roster grep to match elif and digit/underscore mode names”.

No added observation/plan Markdown documents in the merge-base diff.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. The commit record reports: “GREEN: rescore-FAN.sh validates the base sha as 40-hex on every branch, resolves git absolutely”.

**Fan-out locator.**

| Role | Path at this tag | Lines (`wc -l`) |
|---|---|---:|
| Fixture generator | `bench/fanout/gen-fanout.clj` | 358 |
| Acceptance scorer | `bench/fanout/fan_check.clj` | 567 |
| Rescore script | `bench/fanout/rescore-FAN.sh` | 129 |
| Adversarial/sabotage harness | `bench/fanout/sabotage-FAN.sh` | 1319 |
| Routing plate | `resources/clj-surgeon-agent-routing.md` | 49 |

Checkout: `git fetch origin tag archive/bridge/fanout-fixtures-in-git && git worktree add --detach <dir> archive/bridge/fanout-fixtures-in-git`

### archive/bridge/feature-thread-verb

Branch: `bridge/feature-thread-verb`. Tip: `508f26f5` · 2026-09-04T20:03:28Z. Commits not on main: **989**.

Develops feature-thread work and its replay experiment, then integrates the admit-gate landing and evolving trunk.

Subjects: “merge: origin/MCP/main (69c859c65170a71278da59b90a0665b6e67f0bd0) into bridge/feature-thread-verb (admit gate landing)” · “records: admit gate LANDED (8d32d619); usage watch; the landing-worktree process fix” · “replay: NC/NS plate pair scored — a miss as a native-side lever; contract measured at T5”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-04-feature-thread-verb-build.md`; `docs/observations/2026-09-04-feature-thread-replay-result.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-09-04-feature-thread-replay-result.md:125`:

> **Verdict: the line was MISSED.** T4 met the reads line (0) and missed raw by one (19 vs 18); T4b met raw (18) and paid two
> pre-write reads. Round six did not move the count versus T3 (19/18 -> 19/18). Both trees are gate-green on the seat's own runs.

Checkout: `git fetch origin tag archive/bridge/feature-thread-verb && git worktree add --detach <dir> archive/bridge/feature-thread-verb`

### archive/bridge/format-form-scope

Branch: `bridge/format-form-scope`. Tip: `62981ee0` · 2026-09-02T18:07:14Z. Commits not on main: **2**.

Bounds managed formatting to the enclosing top-level form and checks a normalized token stream with the pinned formatter.

Subjects: “format only the enclosing top-level form (46o): scoped managed formatting bounded by a clause-normalised token stream; pinned standard-clojure-style 0.29.0 executed in CI (MCP-OP-FMT-001..012)” · “close the measured loser shapes at the server: typed refusals, drift against the expected post-image, fail-closed splice guard (MCP-OP-CLOSE-001..022)”.

No added observation/plan Markdown documents in the merge-base diff.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/bridge/format-form-scope && git worktree add --detach <dir> archive/bridge/format-form-scope`

### archive/bridge/gha

Branch: `bridge/gha`. Tip: `5ce8aaea` · 2026-09-04T20:15:04Z. Commits not on main: **869**.

Adds GitHub Actions wiring for the Babashka test target, incorporating the suite partition and isolated-home work.

Subjects: “GHA: call `make test-bb`, and rebase the doc on suite-spike round two (2ecce8c4)” · “merge: bridge/suite-spike round two (2ecce8c4) into bridge/gha” · “spike(suite): round two -- the partition, the throwaway home, the concurrency battery”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-04-gha-round1.md`; `docs/observations/feature-thread-round9-review-sol.md`.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. The commit record reports: “docs: GHA round one -- merge gate 209 s, battery 11-wide, three findings”.

Checkout: `git fetch origin tag archive/bridge/gha && git worktree add --detach <dir> archive/bridge/gha`

### archive/bridge/helper-closure-design

Branch: `bridge/helper-closure-design`. Tip: `43f4778b` · 2026-09-05T06:08:53Z. Commits not on main: **1145**.

Defines helper-extraction closure, caller counts, rollback authority, verification profiles, and receipt semantics before implementation.

Subjects: “intent: receipt definitions — caller_files = external callers, source_file, changed_files; expect uses the external definition; no planner details_path (Astra 06:07 application run)” · “intent: kernel/rollback authority decision (extraction kernel, exception-complete), no built-in profile, proof_status” · “intent: verification profile/evidence contract (existing profile path, publish only available evidence); self-recursive arity owners on the happy path; pure/boundary test split”.

Added observation/plan documents (merge-base comparison): `docs/plans/helper-closure-extraction.md`; `docs/plans/alias-migration-mixed-refer-repair.md`.

Shared-history battery result (the row names its measured candidate, not a verdict on this branch’s own changes), `docs/observations/battery-ledger.edn:4`:

> {:sha "f686662858dd44f7d8bba58fd200d493c325e0bd", :started "2026-09-05T03:13:43Z", :wall_s 716, :verdict :pass, :host "anvil-server"}

Checkout: `git fetch origin tag archive/bridge/helper-closure-design && git worktree add --detach <dir> archive/bridge/helper-closure-design`

### archive/bridge/helper-extraction-impl

Branch: `bridge/helper-extraction-impl`. Tip: `6682f6f6` · 2026-09-05T14:49:06Z. Commits not on main: **1240**.

Implements helper extraction and preserves the merged landing candidate's battery receipt.

Subjects: “battery: :pass receipt for the landing tip 9e67856a (e1239a99 feature bytes + trunk merge)” · “Merge remote-tracking branch 'origin/MCP/main' into bridge/helper-extraction-impl” · “docs: Gene report 14:31Z”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-05-helper-extraction-fence-review-r7.md`; `docs/observations/2026-09-05-helper-extraction-fence-review-r6.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:13`:

> {:sha "9e67856aa496e5f8f07f23522f7d8eea652078dc", :started "2026-09-05T14:36:53Z", :wall_s 717, :verdict :pass, :host "anvil-server"}

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-09-05-helper-extraction-fence-review-r7.md:37`:

> **Overall boundary landing verdict: GO. Remaining item: none.**

Checkout: `git fetch origin tag archive/bridge/helper-extraction-impl && git worktree add --detach <dir> archive/bridge/helper-extraction-impl`

### archive/bridge/helper-refusal-example

Branch: `bridge/helper-refusal-example`. Tip: `49af88ab` · 2026-09-05T20:15:45Z. Commits not on main: **1302**.

Makes helper request-shape refusals name the field, decision, and runnable example after a caller probe required repeated guessing.

Subjects: “battery: receipt for c5e8be8f (563 tests, 12196 assertions, 0 failures, 699 s)” · “helper_extraction: request-shape refusals carry :field, :decision and a runnable :example (Sol caller probe: seven guessing round trips)” · “log: mission rounds 4+5; Sol probe 2 launched”.

Added observation/plan documents (merge-base comparison): `docs/plans/helper-closure-extraction.md`; `docs/observations/2026-09-05-helper-extraction-fence-review-r7.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:14`:

> {:sha "c5e8be8f0a1bc662ed68b379eb47a2869e0b0f47", :started "2026-09-05T20:03:51Z", :wall_s 699, :verdict :pass, :host "anvil-server"}

Checkout: `git fetch origin tag archive/bridge/helper-refusal-example && git worktree add --detach <dir> archive/bridge/helper-refusal-example`

### archive/bridge/integration-2026-09-03

Branch: `bridge/integration-2026-09-03`. Tip: `2556a383` · 2026-09-04T00:00:44Z. Commits not on main: **283**.

Composes the reviewed bridge changes, including alias migration and memory work, and records the final-SHA gates and newly exposed failure.

Subjects: “docs: STEP 2 gates at the final sha, and the one NEW red the battery found” · “merge: bridge/q5z-alias-migration 0085db8 — GO round 10 (follow-up to f51ceae)” · “fix: the reserved-peak reader must be TOTAL over any operation result”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-03-integration-branch.md`; `docs/observations/2026-09-03-mem-005-parser-admission.md`.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/bridge/integration-2026-09-03 && git worktree add --detach <dir> archive/bridge/integration-2026-09-03`

### archive/bridge/integration-2026-09-03-mem003

Branch: `bridge/integration-2026-09-03-mem003`. Tip: `d0e82620` · 2026-09-04T22:27:43Z. Commits not on main: **969**.

Integrates the bounded memory scan and closes multiline interop and refer bypasses by scanning the complete source subject.

Subjects: “GREEN: the scan's subject is the SOURCE, not a line — one join closes the newline hole and round three's :refer bypass” · “RED: round-ten review plants — the newline defeats every interop face and reopens round three's :refer bypass” · “docs: round-nine build record — the composition is green, and what caused each of the fourteen”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-04-integration-2-mem003.md`; `docs/observations/mem003-second-landing-round8-review-opus.md`.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. The commit record reports: “GREEN: the scan's subject is the SOURCE, not a line — one join closes the newline hole and round three's :refer bypass”.

Checkout: `git fetch origin tag archive/bridge/integration-2026-09-03-mem003 && git worktree add --detach <dir> archive/bridge/integration-2026-09-03-mem003`

### archive/bridge/integration-2026-09-03-studyops

Branch: `bridge/integration-2026-09-03-studyops`. Tip: `f8353945` · 2026-09-04T02:18:28Z. Commits not on main: **410**.

Lands study operations onto the memory-admission and streaming work, retaining the scan meter and explicit composition gates.

Subjects: “docs: the last gate's ran-line, and two claims upgraded from counts to set equality” · “docs: the third landing's composition record — study-ops onto MEM-005/MEM-003” · “merge: study-ops-mcp (a0b0520) onto mem003 (0a38e3d) — one study kernel that keeps MEM-005's meter”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-04-integration-3-studyops.md`; `docs/observations/2026-09-04-integration-2-mem003.md`.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/bridge/integration-2026-09-03-studyops && git worktree add --detach <dir> archive/bridge/integration-2026-09-03-studyops`

### archive/bridge/integration-candidate-0305

Branch: `bridge/integration-candidate-0305`. Tip: `e420f24e` · 2026-09-05T03:46:04Z. Commits not on main: **1127**.

Assembles a landing candidate containing the fast default test target and declared test-isolation budget exception.

Subjects: “candidate: bridge/test-default-fast 1fe16056 (help literal 'Run all tests' restored on test-full)” · “make: help says 'Run all tests' on test-full (WTL-CLI-002 witness in worktree_lifecycle_cli_test asserts the literal; the phrase belongs to the target that runs all tests)” · “candidate: bridge/iso007-fence-override 322a63f1 (declared TEST-ISO-007 override for reader-eval-fence-test + :pass battery receipt for f6866628)”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-03-integration-branch.md`; `docs/plans/alias-migration-binding-scope-repair.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:4`:

> {:sha "f686662858dd44f7d8bba58fd200d493c325e0bd", :started "2026-09-05T03:13:43Z", :wall_s 716, :verdict :pass, :host "anvil-server"}

Checkout: `git fetch origin tag archive/bridge/integration-candidate-0305 && git worktree add --detach <dir> archive/bridge/integration-candidate-0305`

### archive/bridge/iso007-fence-override

Branch: `bridge/iso007-fence-override`. Tip: `322a63f1` · 2026-09-05T03:33:06Z. Commits not on main: **1114**.

Records a measured budget exception for the reader-evaluation fence test and a passing battery with its skipped prerequisite disclosed.

Subjects: “battery: :pass receipt for f6866628 (512 tests / 11008 assertions, 0 failures, 0 isolation violations, wall 716 s, lane 6-9 ActiveProcessorCount=8; one precondition skipped: no transaction-recovery battery receipt)” · “test(TEST-ISO-007): narrow the override rationale to measured runs (Astra review): a measured-baseline exception, not an impossibility claim” · “test(TEST-ISO-007): declared budget override for reader-eval-fence-test (measured 494 s CI / 467 s contended; default 300 s made the battery unpassable by construction)”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-05-astra-next-api-advice.md`; `docs/plans/alias-migration-mixed-refer-repair.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:4`:

> {:sha "f686662858dd44f7d8bba58fd200d493c325e0bd", :started "2026-09-05T03:13:43Z", :wall_s 716, :verdict :pass, :host "anvil-server"}

Checkout: `git fetch origin tag archive/bridge/iso007-fence-override && git worktree add --detach <dir> archive/bridge/iso007-fence-override`

### archive/bridge/kondo-path-test

Branch: `bridge/kondo-path-test`. Tip: `f8a9ef98` · 2026-09-03T04:22:40Z. Commits not on main: **0**.

Replaces a hardcoded macOS analyzer-path assertion with a test of the actual clj-kondo resolution rule.

Subjects (tip history; no commits outside main): “test: assert clj-kondo resolution rule, not a hardcoded macOS path” · “q5z re-review GO-WITH-FIX filed; round three launched” · “log: fold-diff pushed a765d859; production re-review running; inbox updated”.

No added observation/plan Markdown documents in the merge-base diff.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. The commit record reports: “q5z re-review GO-WITH-FIX filed; round three launched”.

Checkout: `git fetch origin tag archive/bridge/kondo-path-test && git worktree add --detach <dir> archive/bridge/kondo-path-test`

### archive/bridge/main-policy-2026-09-04

Branch: `bridge/main-policy-2026-09-04`. Tip: `81bba7f3` · 2026-09-04T01:41:11Z. Commits not on main: **2**.

Documents Gene's freeze on main and corrects the resume pointer to the seat branch.

Subjects: “policy: the Memento pointer names the seat branch (main is frozen)” · “policy: main is frozen — nobody merges to main, the mayor included (Gene, 2026-09-04)”.

No added observation/plan Markdown documents in the merge-base diff.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/bridge/main-policy-2026-09-04 && git worktree add --detach <dir> archive/bridge/main-policy-2026-09-04`

### archive/bridge/memory-battery

Branch: `bridge/memory-battery`. Tip: `5534e940` · 2026-09-03T07:07:14Z. Commits not on main: **15**.

Builds and hardens the memory battery, binding its reference to source bytes and refusing unsafe corpus or output roots.

Subjects: “memory battery: guard MEMBAT_ROOT and stop auto-launching the 4g reference (Sol hole 4)” · “memory battery: anchor the reference to its own bytes (Sol hole 3)” · “memory battery: refuse a directory at an expected corpus path (Sol hole 2)”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-03-memory-battery-baseline.md`.

Recorded result (scope and candidate as quoted), `docs/observations/2026-09-03-memory-battery-baseline.md:3`:

> `make memory-battery` at `4ec01da` (= `origin/main`), on Anvil, `-Xmx512m`, 5 reps,
> N = 100 / 1,000 / 10,000, four arms. **Exit 1 — FAIL (INCOMPLETE).**

Checkout: `git fetch origin tag archive/bridge/memory-battery && git worktree add --detach <dir> archive/bridge/memory-battery`

### archive/bridge/mission-ledger

Branch: `bridge/mission-ledger`. Tip: `f2efc87c` · 2026-09-05T22:50:47Z. Commits not on main: **1287**.

Develops the mission ledger and a typist prototype with provider selection, interleaved benchmarking, shared judging, and explicit upstream routing.

Subjects: “prototype(typist): OpenRouter upstream pin (TYPIST_OPENROUTER_ORDER, no fallback); default routing had chosen DigitalOcean/DeepInfra/CoreWeave/SiliconFlow” · “prototype(typist): accept :openrouter-api-key as the OpenRouter key keyword” · “prototype(typist): provider abstraction (groq / openrouter / spark) + --bench mode (interleaved k=1 rounds, one table, EDN receipt); shared judge path; spark usage-limit detection reads whole stderr”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-05-astra-perfect-ledger.md`; `docs/observations/2026-09-05-surgeon-ideal-shape-terra.md`.

Shared-history battery result (the row names its measured candidate, not a verdict on this branch’s own changes), `docs/observations/battery-ledger.edn:13`:

> {:sha "9e67856aa496e5f8f07f23522f7d8eea652078dc", :started "2026-09-05T14:36:53Z", :wall_s 717, :verdict :pass, :host "anvil-server"}

Checkout: `git fetch origin tag archive/bridge/mission-ledger && git worktree add --detach <dir> archive/bridge/mission-ledger`

### archive/bridge/o4-read-hook

Branch: `bridge/o4-read-hook`. Tip: `70d81108` · 2026-09-03T23:57:45Z. Commits not on main: **8**.

Freezes the O4 read-hook experiment and gates; recognizes copied hooks by content and checks arm prerequisites.

Subjects: “docs: freeze O4's receipt and its gates in the observation” · “read-hook: a COPY of the hook is still the hook — detect it by content, not path” · “docs: O4 pre-flight — assert bb and rg inside the arm before it runs”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-04-o4-read-hook-design.md`.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. The commit record reports: “read-hook: GREEN — bin/rg-clj serves Clojure-scoped rg from the Surgeon read path”.

Checkout: `git fetch origin tag archive/bridge/o4-read-hook && git worktree add --detach <dir> archive/bridge/o4-read-hook`

### archive/bridge/opus-caller-harness

Branch: `bridge/opus-caller-harness`. Tip: `3b640785` · 2026-09-05T01:56:05Z. Commits not on main: **1051**.

Repairs the Opus caller harness against independent review, using the retained arm roster, matching cores, and executable regression probes.

Subjects: “bench/opus-caller: round three — close Astra's NO-GO, his probes RED then GREEN” · “review: Astra's independent NO-GO on the Opus caller harness (afe6621f), verbatim; harness held until its required proofs land” · “bench/opus-caller: round two — Astra's 21-arm roster, same cores, his policy imported”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-05-opus-caller-harness.md`; `docs/observations/2026-09-05-opus-caller-harness-review-astra-NO-GO.md`.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. The commit record reports: “bench/opus-caller: round three — close Astra's NO-GO, his probes RED then GREEN”.

Checkout: `git fetch origin tag archive/bridge/opus-caller-harness && git worktree add --detach <dir> archive/bridge/opus-caller-harness`

### archive/bridge/parser-admission

Branch: `bridge/parser-admission`. Tip: `52c5d85c` · 2026-09-03T09:44:44Z. Commits not on main: **43**.

Adds bounded lexical and parser admission with per-scan memory accounting and explicit gate and receipt corrections.

Subjects: “docs: a candidate cause for round 2's one unexplained suite failure — install_test is HEAD-sha-sensitive” · “docs: receipt hygiene — gate table regenerated, nested-warm as a bound, inb-07c5e7 named, MEM-005 boundaries amended” · “fix: the scan meter is per-scan, unconditional in EDN, and carries its denominator”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-03-mem-005-parser-admission.md`; `docs/observations/2026-09-03-memory-battery-baseline.md`.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. The commit record reports: “GREEN: #! is a line comment, anywhere”.

Checkout: `git fetch origin tag archive/bridge/parser-admission && git worktree add --detach <dir> archive/bridge/parser-admission`

### archive/bridge/q5z-alias-migration

Branch: `bridge/q5z-alias-migration`. Tip: `ab34c93c` · 2026-09-04T11:10:23Z. Commits not on main: **136**.

Implements repository-wide alias migration and its guarded MCP entrance. Later rounds harden the refusal-kind scan so constructor argument position and string literals cannot bypass coverage.

Subjects: “q5z-r19-2 GREEN: the per-file scan ENABLE is gone — a (refusal …) site's kind ARGUMENT is located by reading the constructor (ALIAS-059)” · “q5z-r19-1 RED: a refusal constructor whose kind is NOT its first parameter disables the whole per-file scan (ALIAS-059)” · “q5z-r18-2: a STRING literal is a kind source exactly as a KEYWORD literal is, and every (refusal …) site is NAMED or ENUMERATED (ALIAS-059)”.

No added observation/plan Markdown documents in the merge-base diff.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. The commit record reports: “q5z-r19-2 GREEN: the per-file scan ENABLE is gone — a (refusal …) site's kind ARGUMENT is located by reading the constructor (ALIAS-059)”.

**Alias-migration locator.** Paths were confirmed with `git ls-tree -r archive/bridge/q5z-alias-migration`; line counts were taken from `git show archive/bridge/q5z-alias-migration:<path> | wc -l`.

| Role | Path at this tag | Lines (`wc -l`) |
|---|---|---:|
| Pure planner | `src/clj_surgeon/alias_migration.clj` | 1372 |
| MCP request/transaction boundary | `src/clj_surgeon/mcp_alias_migration.clj` | 2775 |
| Tool registration | `src/clj_surgeon/mcp_tool.clj` | 1858 |
| Server catalog | `src/clj_surgeon/mcp_server.clj` | 367 |
| Planner tests | `test/clj_surgeon/alias_migration_test.clj` | 717 |
| MCP boundary tests | `test/clj_surgeon/mcp_alias_migration_test.clj` | 6889 |
| Deterministic corpus/fixture builder | `test/clj_surgeon/alias_migration_fixture.clj` | 627 |

The MCP tool name `alias_migration` is registered at `src/clj_surgeon/mcp_tool.clj:1820`. The fixture namespace is executable corpus-building code; inline source fixtures also live in the two test files. The separate characterization test and `bench/fanout` files listed on Astra are absent from this q5z tag.

Checkout: `git fetch origin tag archive/bridge/q5z-alias-migration && git worktree add --detach <dir> archive/bridge/q5z-alias-migration`

### archive/bridge/read-path-memory

Branch: `bridge/read-path-memory`. Tip: `b7ef23d3` · 2026-09-03T06:40:30Z. Commits not on main: **10**.

Reduces outline memory use while freezing the old builder for differential checks, including malformed-reader parity and pinned memory ceilings.

Subjects: “test(outline-differential): freeze the old builder, add malformed-reader parity” · “docs(read-path-memory): scope MEM-015's EARS row to the default projection” · “test(outline-memory): pin JDK/rewrite-clj to the ceiling, mandate re-baseline”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-03-mem-015-single-parse.md`.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/bridge/read-path-memory && git worktree add --detach <dir> archive/bridge/read-path-memory`

### archive/bridge/receipt-ratchets

Branch: `bridge/receipt-ratchets`. Tip: `c5ef7cad` · 2026-09-03T06:04:47Z. Commits not on main: **0**.

Repairs receipt presentation and recovery behavior, including regex newline escaping, with suite-lane integration.

Subjects (tip history; no commits outside main): “Merge remote-tracking branch 'origin/main' into bridge/receipt-ratchets” · “ratchet-fix: presentation-node re-escapes a regex leaf's raw newline too” · “anvil: three suite lanes (suite-run); battery keeps the exclusive lock”.

No added observation/plan Markdown documents in the merge-base diff.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/bridge/receipt-ratchets && git worktree add --detach <dir> archive/bridge/receipt-ratchets`

### archive/bridge/rf2-extract-rewire

Branch: `bridge/rf2-extract-rewire`. Tip: `965d49e2` · 2026-09-03T06:05:36Z. Commits not on main: **33**.

Implements extraction with caller rewiring and truthful reporting of pruned callers, repairing reference counts after trunk integration.

Subjects: “test: repair the rewire reference count the merge with origin/main made stale” · “Merge remote-tracking branch 'origin/main' into bridge/rf2-extract-rewire” · “extract: a run that writes no caller byte reports the pruned caller (MCP-OP-EXTRACT-042)”.

No added observation/plan Markdown documents in the merge-base diff.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. The commit record reports: “extract: RED -- the pruned-tree refusal is total and drops its context”.

Checkout: `git fetch origin tag archive/bridge/rf2-extract-rewire && git worktree add --detach <dir> archive/bridge/rf2-extract-rewire`

### archive/bridge/routing-doc-test

Branch: `bridge/routing-doc-test`. Tip: `a9d87010` · 2026-09-03T04:41:55Z. Commits not on main: **0**.

Ratchets the installed routing documentation, including the terminal_response relay paragraph.

Subjects (tip history; no commits outside main): “routing plate: restore the MCP-OP-RELAY-004 terminal_response paragraph” · “log: ratchets fix round landed; entrance tightening flagged for the re-check” · “resume: ten lanes; memory program state”.

No added observation/plan Markdown documents in the merge-base diff.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/bridge/routing-doc-test && git worktree add --detach <dir> archive/bridge/routing-doc-test`

### archive/bridge/streaming-ls-tree

Branch: `bridge/streaming-ls-tree`. Tip: `95b0881b` · 2026-09-03T20:09:02Z. Commits not on main: **75**.

Builds bounded streaming source discovery and closes path races and newline-filename splitting with NUL-delimited discovery.

Subjects: “fix(GREEN): NUL-delimit find-clj-files output so a newline in a filename is one candidate” · “test(RED): witness a newline in a source filename splitting find-clj-files output” · “docs: MEM-003 states the discovery predicate it is guarded against”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-03-mem-003-streaming-ls-tree.md`; `docs/observations/2026-09-03-memory-battery-baseline.md`.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. The commit record reports: “fix(GREEN): NUL-delimit find-clj-files output so a newline in a filename is one candidate”.

Checkout: `git fetch origin tag archive/bridge/streaming-ls-tree && git worktree add --detach <dir> archive/bridge/streaming-ls-tree`

### archive/bridge/study-ops-mcp

Branch: `bridge/study-ops-mcp`. Tip: `e24ee131` · 2026-09-04T21:03:44Z. Commits not on main: **987**.

Adds the study and ls-tree MCP reads and hardens their reader, path, output, and continuation boundaries through review rounds.

Subjects: “merge-fix: the trunk's fence witnesses were red against this branch's kernel — three real gaps” · “merge-fix: the trunk's `ls-tree-root-refusal` is a public var a trunk test drives” · “study-ops: O2r12 observation — a declared residual is not a waiver of the wire”.

Added observation/plan documents (merge-base comparison): `docs/observations/study-ops-o2-round9-review-sol.md`; `docs/observations/study-ops-o2-round7-review-sol.md`.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. The commit record reports: “study-ops: O2r12 GREEN (§2) — a receipt whose keys are one key on the wire is refused, not rendered”.

Checkout: `git fetch origin tag archive/bridge/study-ops-mcp && git worktree add --detach <dir> archive/bridge/study-ops-mcp`

### archive/bridge/suite-spike

Branch: `bridge/suite-spike`. Tip: `01ec667c` · 2026-09-05T01:22:56Z. Commits not on main: **1077**.

Partitions the test suite and investigates isolation and concurrency behavior, retaining failing battery evidence that also reproduces on trunk.

Subjects: “docs: the battery failure REPRODUCES on the unmodified trunk tip under the same pinning” · “docs: round-five gates, and the battery's FAILING receipt, recorded as it stands” · “fix(TEST-ISO-002,010): the integration lane's verify commands are declared; a shared agent-pool worker is not a namespace's leak”.

Added observation/plan documents (merge-base comparison): `docs/observations/suite-spike-round3-review-sol.md`; `docs/observations/2026-09-05-suite-spike-round5.md`.

Recorded result, `docs/observations/2026-09-05-suite-spike-round5.md:42`. The GREEN below is the exclusion-membership oracle of round five (every live exclusion proves membership), not the battery: the same branch's subjects record the battery's FAILING receipt, which reproduces on the unmodified trunk tip.

> VERDICT: GREEN
> ```

Checkout: `git fetch origin tag archive/bridge/suite-spike && git worktree add --detach <dir> archive/bridge/suite-spike`

### archive/bridge/telemetry-coverage

Branch: `bridge/telemetry-coverage`. Tip: `417ed6b5` · 2026-09-05T00:24:13Z. Commits not on main: **1044**.

Collects one event at the operation boundary and reports usage by tool, session, workspace root, and time window; isolates the registry in tests.

Subjects: “fix: clear the process-wide telemetry registry after each coverage witness” · “docs: how telemetry works — one boundary, one event, per-root reporting” · “green: the collector reports per tool, per session, per root, with the window”.

Added observation/plan documents (merge-base comparison): `docs/observations/suite-spike-round3-review-sol.md`; `docs/observations/2026-09-05-astra-perfect-ledger.md`.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/bridge/telemetry-coverage && git worktree add --detach <dir> archive/bridge/telemetry-coverage`

### archive/bridge/test-default-fast

Branch: `bridge/test-default-fast`. Tip: `1fe16056` · 2026-09-05T03:46:02Z. Commits not on main: **1109**.

Makes the landing gate the default test target, keeps the complete suite under test-full, and corrects help claims and prerequisites.

Subjects: “make: help says 'Run all tests' on test-full (WTL-CLI-002 witness in worktree_lifecycle_cli_test asserts the literal; the phrase belongs to the target that runs all tests)” · “make: drop the unsupported '~2.5 min' from the default test help line (Astra review)” · “make: test-full in .PHONY; help advertises test (landing gate) and test-full; comment states the default's real coverage and battery-fresh dependency”.

Added observation/plan documents (merge-base comparison): `docs/plans/alias-migration-mixed-refer-repair.md`; `docs/observations/tmp-leak-round3-review-opus.md`.

Shared-history battery result (the row names its measured candidate, not a verdict on this branch’s own changes), `docs/observations/battery-ledger.edn:2`:

> {:sha "c79227d00eba51babd98068498a0ff0428e75653", :started "2026-09-05T00:45:49Z", :wall_s 710, :verdict :fail, :host "anvil-server"}

Checkout: `git fetch origin tag archive/bridge/test-default-fast && git worktree add --detach <dir> archive/bridge/test-default-fast`

### archive/bridge/test-owns-recovery-battery

Branch: `bridge/test-owns-recovery-battery`. Tip: `6ce42e4b` · 2026-09-05T04:43:54Z. Commits not on main: **1142**.

Makes the default test gate own its transaction-recovery prerequisite and preserves both failed and repaired battery rows.

Subjects: “merge origin/MCP/main; battery ledger kept append-only in :started order (trunk :fail 04:12Z, then this tip's :pass 04:26Z)” · “battery: :pass receipt for 0dd733cb (512/11008, 0 failures, 0 skipped preconditions after the recovery battery; wall 718 s, lane 6-9)” · “battery: trunk 862bc7d6 receipt :fail (512/11008, 1 failure = ADMIT-150 default-lane ownership; fix staged 0dd733cb)”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-03-memory-battery-baseline.md`; `docs/observations/2026-09-05-gene-report-0426z.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:6`:

> {:sha "0dd733cbaf4fb9bc397472844021fde660cb67ec", :started "2026-09-05T04:26:40Z", :wall_s 718, :verdict :pass, :host "anvil-server"}

Checkout: `git fetch origin tag archive/bridge/test-owns-recovery-battery && git worktree add --detach <dir> archive/bridge/test-owns-recovery-battery`

### archive/bridge/thread-parameter

Branch: `bridge/thread-parameter`. Tip: `b1502233` · 2026-09-05T19:00:15Z. Commits not on main: **1278**.

Prototypes a pure planner that threads a keyword parameter through direct callers, with eleven-caller fixtures and explicit refusals for unsupported references or owners.

Subjects: “prototype(thread-parameter): pure planner for Astra's idea #1 — thread a new keyword parameter through every direct caller of a var under every binding spelling; whole-form edits; typed refusals (ambiguous-owner, multi-arity-unsupported, indirect-reference, caller-outside-scope); fixture with eleven callers + decoys + a first-class-use refusal variant; excluded lane with make thread-parameter-red” · “docs: riff round two — Fable adopts Astra's five verbs, adaptive rule, stale-resume; joint pass condition; twenty ideas; first batch” · “log: Gene delegates the next two hours to Fable + Astra; plan of record”.

Added observation/plan documents (merge-base comparison): `docs/observations/feature-thread-round9-review-sol.md`; `docs/observations/feature-thread-round7-review-sol.md`.

Shared-history battery result (the row names its measured candidate, not a verdict on this branch’s own changes), `docs/observations/battery-ledger.edn:13`:

> {:sha "9e67856aa496e5f8f07f23522f7d8eea652078dc", :started "2026-09-05T14:36:53Z", :wall_s 717, :verdict :pass, :host "anvil-server"}

Checkout: `git fetch origin tag archive/bridge/thread-parameter && git worktree add --detach <dir> archive/bridge/thread-parameter`

### archive/bridge/tmp-leak-ratchet

Branch: `bridge/tmp-leak-ratchet`. Tip: `5a6e7c81` · 2026-09-04T11:23:00Z. Commits not on main: **408**.

Repairs scratch-directory selection so RAM-backed subpaths are redirected as well as the exact root, with retained failing witnesses.

Subjects: “docs: correct the [x] ordering disclosure — name both intents, not one” · “fix (GREEN): SELF_TEST_TMP redirects a RAM subpath, not just the exact root” · “witness (RED): both TMPDIR loops are blind to a RAM subpath”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-04-gene-report-0455z.md`; `docs/observations/2026-09-04-ethread-cohort.md`.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. The commit record reports: “fix (GREEN): SELF_TEST_TMP redirects a RAM subpath, not just the exact root”.

Checkout: `git fetch origin tag archive/bridge/tmp-leak-ratchet && git worktree add --detach <dir> archive/bridge/tmp-leak-ratchet`

### archive/bridge/tweezer-1

Branch: `bridge/tweezer-1`. Tip: `702808a4` · 2026-09-02T18:48:29Z. Commits not on main: **3**.

Records two watched extraction sessions that split exact verification and explicit-change helpers out of larger MCP code, including returns and wall time.

Subjects: “tweezer sessions 1 and 2: watcher records (session 1: 35 returns / 797 s; session 2: 8 returns / 293 s, move at 6, suites at 8)” · “tweezer session 2: explicit-change cluster (five forms) → clj-surgeon.mcp-explicit-change with the session-1 patches in the tool” · “tweezer session 1: rf1 extraction by hand (nine exact-verify forms → clj-surgeon.mcp-exact-verify), tool patched live at the REPL for header, imports, visibility”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-02-tweezer-session-2-watch.md`; `docs/observations/2026-09-02-tweezer-session-1-watch.md`.

Recorded result (scope and candidate as quoted), `docs/observations/2026-09-02-tweezer-session-2-watch.md:41`:

> - Returns-to-move, session 1 vs session 2: session 1 (this same repo, same class of task, the tool pre-patch) took 27-31 returns to land its move and 35 to close, roughly 3x over native's 9-10-to-move benchmark. Session 2, run right after the live patch, landed its move at call 6 and closed (suites confirmed) at call 8 — under native's 9-10-to-move benchmark and under the 14.3-total stripped-prompt benchmark, the first hand-driven run in this class to beat native on both axes.

Checkout: `git fetch origin tag archive/bridge/tweezer-1 && git worktree add --detach <dir> archive/bridge/tweezer-1`

### archive/bridge/txn-journal

Branch: `bridge/txn-journal`. Tip: `2df05b3c` · 2026-09-03T21:53:52Z. Commits not on main: **81**.

Builds the disk-backed transaction journal and recovery rules, distinguishing interrupted breaks and reporting blocking files without overstating rollback authority.

Subjects: “docs: MEM-013 records round 8's findings, the :status field, and the adoption boundary” · “txn: the sweep counts corroborated and uncorroborated interrupted breaks apart (round 8, finding 4)” · “txn: the blocking file gets its own key, and the standing invariant sees it (round 8, finding 3)”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-03-memory-battery-baseline.md`.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/bridge/txn-journal && git worktree add --detach <dir> archive/bridge/txn-journal`

## fable/* — 136 tags

The September Fable program contains batteries and their receipt checkpoints, proof-burden and parity harnesses, insert_forms and rename_alias, receipt and result limits, seat-path defaults, and skiff installation. Every receipt tag gets its own entry here: its branch suffix may identify the submitted candidate rather than the receipt tip, and the quoted ledger row identifies exactly what was tested.

### archive/fable/astra-collector-receipt

Branch: `fable/astra-collector-receipt`. Tip: `be47b066` · 2026-09-06T12:03:32Z. Commits not on main: **1762**.

Records receipt for Astra's collector/docs tip merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for Astra's collector/docs tip merged onto trunk (landing gate battery-fresh)” · “Merge remote-tracking branch 'origin/MCP/main' into fable/astra-collector-receipt” · “live: Astra fast-typist commentary 11:47Z”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-06-sol-fence-astra-collector-r1.md`; `docs/observations/2026-09-06-sol-fence-refusal-text-r2.md`.

Latest retained battery row (its SHA is the measured candidate; no claim that it verifies this tag tip), `docs/observations/battery-ledger.edn:22`:

> {:sha "2bba421fe88ef758d4d08f96907e442f7a8ce804", :started "2026-09-06T11:49:21Z", :wall_s 851, :verdict :pass, :host "anvil-server"}

Checkout: `git fetch origin tag archive/fable/astra-collector-receipt && git worktree add --detach <dir> archive/fable/astra-collector-receipt`

### archive/fable/astra-description-receipt

Branch: `fable/astra-description-receipt`. Tip: `3ecd0cfc` · 2026-09-06T15:28:18Z. Commits not on main: **1843**.

Records receipt for Astra's description tip merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for Astra's description tip merged onto trunk (landing gate battery-fresh)” · “Merge remote-tracking branch 'origin/MCP/main' into fable/astra-description-receipt” · “records: description-guidance fence LAND YES; landing in progress”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-06-sol-fence-astra-description-r1.md`; `docs/observations/2026-09-06-live-astra-typist-commentary.md`.

Latest retained battery row (its SHA is the measured candidate; no claim that it verifies this tag tip), `docs/observations/battery-ledger.edn:23`:

> {:sha "72e47aeb67ad469e002276bf0c6a3ccdafb0a228", :started "2026-09-06T15:14:49Z", :wall_s 809, :verdict :pass, :host "anvil-server"}

Checkout: `git fetch origin tag archive/fable/astra-description-receipt && git worktree add --detach <dir> archive/fable/astra-description-receipt`

### archive/fable/astra-leak-receipt

Branch: `fable/astra-leak-receipt`. Tip: `6f5d3f3c` · 2026-09-06T09:12:37Z. Commits not on main: **1669**.

Records receipt for Astra's leak tip merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for Astra's leak tip merged onto trunk (landing gate battery-fresh)” · “Merge remote-tracking branch 'origin/MCP/main' into fable/astra-leak-receipt” · “live: Astra fast-typist commentary 08:53Z”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-06-sol-fence-astra-leak-tip-r1.md`; `docs/observations/tmp-leak-round3-review-opus.md`.

Latest retained battery row (its SHA is the measured candidate; no claim that it verifies this tag tip), `docs/observations/battery-ledger.edn:21`:

> {:sha "0b847344343ee90e4ec0313fec53d72947f82982", :started "2026-09-06T08:59:24Z", :wall_s 793, :verdict :pass, :host "anvil-server"}

Checkout: `git fetch origin tag archive/fable/astra-leak-receipt && git worktree add --detach <dir> archive/fable/astra-leak-receipt`

### archive/fable/astra-typist-route-receipt

Branch: `fable/astra-typist-route-receipt`. Tip: `cd1eb5cc` · 2026-09-06T07:16:15Z. Commits not on main: **1611**.

Records receipt for Astra's typist-route tip merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for Astra's typist-route tip merged onto trunk (landing gate battery-fresh)” · “Merge remote-tracking branch 'origin/MCP/main' into fable/astra-typist-route-receipt” · “Record final corpus-lane review and explicit landing gate condition”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-06-sol-fence-astra-typist-route-r3-delta.md`; `docs/observations/2026-09-06-sol-fence-astra-typist-route-r2.md`.

Latest retained battery row (its SHA is the measured candidate; no claim that it verifies this tag tip), `docs/observations/battery-ledger.edn:20`:

> {:sha "8fd9a2e8812c5a13ee84297e76c23a0cc5110c43", :started "2026-09-06T07:03:04Z", :wall_s 791, :verdict :pass, :host "anvil-server"}

Checkout: `git fetch origin tag archive/fable/astra-typist-route-receipt && git worktree add --detach <dir> archive/fable/astra-typist-route-receipt`

### archive/fable/battery-floor-receipt-2ba9c950

Branch: `fable/battery-floor-receipt-2ba9c950`. Tip: `078b9851` · 2026-09-08T22:54:23Z. Commits not on main: **2507**.

Records receipt for 60e60759e41193db0bccf9cc43b8dfbc66fe07fc merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 60e60759e41193db0bccf9cc43b8dfbc66fe07fc merged onto trunk (landing gate battery-fresh)” · “merge: TEST-ISO-014: the battery's 206-s deftest split into six independent launcher cells — default 8-lane battery 216 s → 172 s; census pins recomputed” · “census: pins recomputed for TEST-ISO-014's six launcher cells (lane manifest + intent ledger)”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-08-tighten/astra-battery-floor-report.md`; `docs/plans/battery-launcher-matrix.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:52`:

> {:sha "60e60759e41193db0bccf9cc43b8dfbc66fe07fc", :started "2026-09-08T22:51:53Z", :wall_s 150, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-floor-receipt-2ba9c950 && git worktree add --detach <dir> archive/fable/battery-floor-receipt-2ba9c950`

### archive/fable/battery-floor-receipt-37406447

Branch: `fable/battery-floor-receipt-37406447`. Tip: `5496ba49` · 2026-09-08T22:15:50Z. Commits not on main: **2500**.

Records receipt for 0f5f9d887cb265064927adc47e211e885dce00bf merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 0f5f9d887cb265064927adc47e211e885dce00bf merged onto trunk (landing gate battery-fresh)” · “merge: TEST-ISO-014: the battery's 206-s deftest split into six independent launcher cells — default 8-lane battery 216 s → 172 s” · “Record the passing 172-second eight-lane battery run”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-launcher-matrix.md`; `docs/plans/battery-archival-distance.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:52`:

> {:sha "0f5f9d887cb265064927adc47e211e885dce00bf", :started "2026-09-08T22:13:22Z", :wall_s 148, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-floor-receipt-37406447 && git worktree add --detach <dir> archive/fable/battery-floor-receipt-37406447`

### archive/fable/battery-fresh-code-only

Branch: `fable/battery-fresh-code-only`. Tip: `df3f9a18` · 2026-09-08T16:38:43Z. Commits not on main: **2391**.

Makes battery freshness count code commits, preventing records-only commits from expiring otherwise green evidence.

Subjects: “battery-fresh: count CODE commits only — the records lane stops expiring green batteries” · “records: cross-stack experiment decision — 3-stack pilot on a Hetzner box” · “records: resume STATE — four lanes”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-archival-distance.md`; `docs/observations/2026-09-06-astra-fresh-caller-review.md`.

Shared-history battery result (the row names its measured candidate, not a verdict on this branch’s own changes), `docs/observations/battery-ledger.edn:39`:

> {:sha "afde6652f16196cbe19e4d20bc5c8e8c08946d23", :started "2026-09-08T14:25:48Z", :wall_s 809, :verdict :pass, :host "anvil-server"}

Checkout: `git fetch origin tag archive/fable/battery-fresh-code-only && git worktree add --detach <dir> archive/fable/battery-fresh-code-only`

### archive/fable/battery-ledger-gating

Branch: `fable/battery-ledger-gating`. Tip: `cb42682a` · 2026-09-14T06:20:07Z. Commits not on main: **2878**.

Admits battery wall-output roots and destinations before I/O and records the state-writer audit and repairs.

Subjects: “docs: record round-two state writer audit and verification” · “fix: admit battery walls roots and destinations before I/O (GREEN)” · “test: reproduce battery state-home admission bypass (RED)”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-ledger-gating.md`; `docs/observations/2026-09-14-battery-ledger-gating-report-astra.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:79`:

> {:sha "d3b221c4f60f01d7dbbae76d609d847ee5c2455b", :started "2026-09-14T02:17:32Z", :wall_s 204, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-ledger-gating && git worktree add --detach <dir> archive/fable/battery-ledger-gating`

### archive/fable/battery-parallel

Branch: `fable/battery-parallel`. Tip: `cbef534b` · 2026-09-08T19:41:37Z. Commits not on main: **2442**.

Parallelizes the battery with bounded lanes and prerequisite ownership, preserving per-test timing and refusing unsupported hooks.

Subjects: “fence: Sol delta fence 3 comment fix — the prerequisite narrative under BATTERY_PREREQS=1 (coordinator runs a missing declared stage before opening lanes; the absent-receipt skip remains only with PREREQS=0 or the fast suite alone)” · “docs: the assertion count does not change with BATTERY_PREREQS — only :skipped does (Sol delta fence 2)” · “fence: Sol's GO-WITH-FIX — test-ns-hook shard refusal; fine per-var walls preserved; defaults BATTERY_LANES=8 BATTERY_PREREQS=1”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-08-battery-parallel-verdict-parity.md`; `docs/plans/battery-archival-distance.md`.

Recorded result (scope and candidate as quoted), `docs/observations/2026-09-08-battery-parallel-verdict-parity.md:1`:

> # TEST-ISO-013 verdict parity: the 1-lane control against the 8-lane run
> 
> **Tip `bb333d33`, anvil-server, both runs under `flock ~/tmp/suite.lock`.**
> 
> The one isolation risk named at the end of the first report was that per-namespace
> assertion counts had never been compared against a serial control -- only the
> union totals had. This closes it.
> 
> | | control | parallel |
> |---|---|---|
> | invocation | `make test-battery BATTERY_LANES=1` | `make test-battery BATTERY_LANES=8` |
> | wall | 818 s | 218 s |
> | receipts | `target/battery-parallel/lane-0.edn` | `lane-0.edn` … `lane-7.edn` |
> | log | `/var/tmp/forge/battery-fx/logs/parallel-serial-control.log` | `…/parallel-r3.log` (second run in file) |
> 
> Compared per namespace: `:test`, assertions (`:pass + :fail + :error`, the same

Checkout: `git fetch origin tag archive/fable/battery-parallel && git worktree add --detach <dir> archive/fable/battery-parallel`

### archive/fable/battery-receipt-00566756

Branch: `fable/battery-receipt-00566756`. Tip: `63d61b63` · 2026-09-14T06:01:44Z. Commits not on main: **2877**.

Records receipt for fe707171674e31c5184dab0458628f778468e7dc merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for fe707171674e31c5184dab0458628f778468e7dc merged onto trunk (landing gate battery-fresh)” · “merge: battery: the tracked ledger row is appended only when a receipt is being minted (BATTERY_LEDGER_APPEND); namespace walls live in the seat state root with the tracked seed read-only; make census-regenerate (inb-16b396)” · “Gate battery receipt appends and move runtime walls to seat state”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-ledger-gating.md`; `docs/plans/battery-launcher-matrix.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:80`:

> {:sha "fe707171674e31c5184dab0458628f778468e7dc", :started "2026-09-14T05:58:23Z", :wall_s 201, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-receipt-00566756 && git worktree add --detach <dir> archive/fable/battery-receipt-00566756`

### archive/fable/battery-receipt-02249934

Branch: `fable/battery-receipt-02249934`. Tip: `02249934` · 2026-09-14T12:00:13Z. Commits not on main: **2890**.

Moves the full registration gate matrix into the battery after freezing its witnesses.

Subjects: “test: move the full registration gate matrix into battery” · “test: freeze registration witnesses before moving the gate matrix” · “fix: execute fresh registration controls and close gate census gaps”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-ledger-gating.md`; `docs/observations/2026-09-14-battery-ledger-gating-report-astra.md`.

Latest retained battery row (its SHA is the measured candidate; no claim that it verifies this tag tip), `docs/observations/battery-ledger.edn:80`:

> {:sha "1eb8c4012a5eefeaa5bfbf6a423aca85c8d7ce45", :started "2026-09-14T06:32:44Z", :wall_s 204, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-receipt-02249934 && git worktree add --detach <dir> archive/fable/battery-receipt-02249934`

### archive/fable/battery-receipt-097a7708

Branch: `fable/battery-receipt-097a7708`. Tip: `097a7708` · 2026-09-13T21:47:18Z. Commits not on main: **2849**.

Completes probe refusal vocabulary and declares filesystem-refusal forwarding for the census.

Subjects: “fix: complete probe refusal vocabulary and repair reference” · “fix: declare filesystem refusal forwarding for the source census” · “test: register measured cost of the Cartesian state-root oracle”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-launcher-matrix.md`; `docs/plans/battery-archival-distance.md`.

Latest retained battery row (its SHA is the measured candidate; no claim that it verifies this tag tip), `docs/observations/battery-ledger.edn:75`:

> {:sha "b33bf3b0d119bff1a88a827f4f7552ac9b204120", :started "2026-09-13T05:34:43Z", :wall_s 178, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-receipt-097a7708 && git worktree add --detach <dir> archive/fable/battery-receipt-097a7708`

### archive/fable/battery-receipt-14644aad

Branch: `fable/battery-receipt-14644aad`. Tip: `a8cea52c` · 2026-09-12T22:16:29Z. Commits not on main: **2800**.

Integrates the Babashka-first hybrid and probe work, then preserves typed request-bound refusals through the HTTP servlet.

Subjects: “merge: bb-rewrite-tower blocks A+B: babashka-first hybrid, probe verb (authorized target, complete closure, classpath-derived roots), TEST-ISO-016 six-run + JVM-reference portability rule, bb lane ceiling, envelope-clean gate” · “fence: Sol's GO-WITH-FIX for 14644aad1c72 — Preserve typed probe request-bound refusals through the HTTP servlet and add a direct writer-boundary regression.” · “Record attempt25 classpath probe repair and passing gates”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-launcher-matrix.md`; `docs/plans/battery-archival-distance.md`.

Latest retained battery row (its SHA is the measured candidate; no claim that it verifies this tag tip), `docs/observations/battery-ledger.edn:71`:

> {:sha "cc50aa2bebc0f0dc7a388470743c8500800dadec", :started "2026-09-12T21:15:40Z", :wall_s 157, :verdict :fail, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-receipt-14644aad && git worktree add --detach <dir> archive/fable/battery-receipt-14644aad`

### archive/fable/battery-receipt-2846e6f3

Branch: `fable/battery-receipt-2846e6f3`. Tip: `2846e6f3` · 2026-09-14T00:48:11Z. Commits not on main: **2855**.

Adds the witness that bounds descriptor provenance inside the complete probe receipt.

Subjects: “census: +1 deftest for Sol's receipt-bound witness (descriptor-provenance-stays-inside-the-whole-receipt-bound)” · “fence: Sol's GO-WITH-FIX for 7bf44cfe3e9d — Bound the complete probe receipt after descriptor provenance is attached while preserving cumulative truncation evidence” · “fix: clean temporary probe images after side-effect failures”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-launcher-matrix.md`; `docs/plans/battery-archival-distance.md`.

Latest retained battery row (its SHA is the measured candidate; no claim that it verifies this tag tip), `docs/observations/battery-ledger.edn:76`:

> {:sha "2c6df947d71e02d14ef71c37012575c99317adc6", :started "2026-09-13T22:18:09Z", :wall_s 200, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-receipt-2846e6f3 && git worktree add --detach <dir> archive/fable/battery-receipt-2846e6f3`

### archive/fable/battery-receipt-32eec035

Branch: `fable/battery-receipt-32eec035`. Tip: `32eec035` · 2026-09-14T09:49:11Z. Commits not on main: **2886**.

Records registration red/green and dogfood receipts and drives the complete missing-surface oracle.

Subjects: “docs: record registration red green and executed dogfood receipts” · “test: drive the complete registration oracle from a missing-surface root” · “feat: register test namespaces with complete oracle and executed controls”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-ledger-gating.md`; `docs/observations/2026-09-14-battery-ledger-gating-report-astra.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:80`:

> {:sha "1eb8c4012a5eefeaa5bfbf6a423aca85c8d7ce45", :started "2026-09-14T06:32:44Z", :wall_s 204, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-receipt-32eec035 && git worktree add --detach <dir> archive/fable/battery-receipt-32eec035`

### archive/fable/battery-receipt-39d2cc83

Branch: `fable/battery-receipt-39d2cc83`. Tip: `39d2cc83` · 2026-09-13T23:25:48Z. Commits not on main: **2838**.

Repairs lazy passwd-home lookup, dscl parsing, and real I/O failure handling.

Subjects: “fence: Sol's GO-WITH-FIX for 694b092f4e46 — Make passwd-home lazy, preserve real I/O failures, validate dscl output, report sanitized attempts, and census the cross” · “census: +1 deftest for the darwin passwd-home witness (parse-dscl-home-reads-the-nfshomedirectory-line)” · “fix(darwin): passwd-home falls through getent -> dscl instead of throwing at load”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-launcher-matrix.md`; `docs/plans/battery-archival-distance.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:75`:

> {:sha "b33bf3b0d119bff1a88a827f4f7552ac9b204120", :started "2026-09-13T05:34:43Z", :wall_s 178, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-receipt-39d2cc83 && git worktree add --detach <dir> archive/fable/battery-receipt-39d2cc83`

### archive/fable/battery-receipt-4a43f8db

Branch: `fable/battery-receipt-4a43f8db`. Tip: `4a43f8db` · 2026-09-14T13:10:11Z. Commits not on main: **2900**.

Keeps registration-count failures at repository scope and uses copied real-gate masks in tests.

Subjects: “fix: keep registration count failures at repository scope” · “test: replace registration seams with copied real-gate masks” · “merge: MCP/main 866df39b into regns-entrance — union of registration surfaces”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-ledger-gating.md`; `docs/plans/battery-launcher-matrix.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:81`:

> {:sha "d033498227b3733874207cfae1063bd4bb0d848f", :started "2026-09-14T11:23:05Z", :wall_s 198, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-receipt-4a43f8db && git worktree add --detach <dir> archive/fable/battery-receipt-4a43f8db`

### archive/fable/battery-receipt-4cf38a1e

Branch: `fable/battery-receipt-4cf38a1e`. Tip: `4cf38a1e` · 2026-09-14T00:30:21Z. Commits not on main: **2854**.

Bounds the complete probe receipt after provenance attachment and cleans temporary images after failures.

Subjects: “fence: Sol's GO-WITH-FIX for 7bf44cfe3e9d — Bound the complete probe receipt after descriptor provenance is attached while preserving cumulative truncation evidence” · “fix: clean temporary probe images after side-effect failures” · “test: reproduce after-create residue across publication errno matrix”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-launcher-matrix.md`; `docs/plans/battery-archival-distance.md`.

Latest retained battery row (its SHA is the measured candidate; no claim that it verifies this tag tip), `docs/observations/battery-ledger.edn:76`:

> {:sha "2c6df947d71e02d14ef71c37012575c99317adc6", :started "2026-09-13T22:18:09Z", :wall_s 200, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-receipt-4cf38a1e && git worktree add --detach <dir> archive/fable/battery-receipt-4cf38a1e`

### archive/fable/battery-receipt-4d06221b

Branch: `fable/battery-receipt-4d06221b`. Tip: `17b1863d` · 2026-09-13T19:53:45Z. Commits not on main: **2846**.

Records receipt for 561307096b879ff57909b42d954bd3db8275b74b merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 561307096b879ff57909b42d954bd3db8275b74b merged onto trunk (landing gate battery-fresh)” · “merge: state-home: warm-image identity descriptor lives under the seat state root, never the checkout (inb-3c65d7); typed refusals probe-image-absent / state-root-unwritable; TMPDIR-derived four-mode witness in :battery; registered second encounter of the envelope-writer class” · “test: exclude process context from fallback contract equality”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-launcher-matrix.md`; `docs/plans/battery-archival-distance.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:76`:

> {:sha "561307096b879ff57909b42d954bd3db8275b74b", :started "2026-09-13T19:50:20Z", :wall_s 204, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-receipt-4d06221b && git worktree add --detach <dir> archive/fable/battery-receipt-4d06221b`

### archive/fable/battery-receipt-5189b1d4

Branch: `fable/battery-receipt-5189b1d4`. Tip: `f8d25c06` · 2026-09-15T07:15:27Z. Commits not on main: **2896**.

Records receipt for e8dfa135a1fc25336583c302dfafa65d99047e36 merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for e8dfa135a1fc25336583c302dfafa65d99047e36 merged onto trunk (landing gate battery-fresh)” · “merge: diff-impact: selections run through the gate's own lane executors (:fast and :all scopes) with total membership classification before projection; partial receipts that can never pass as gate receipts; child argv parity (inb-b142af, inb-fbd110)” · “fix: classify every selected namespace before impact scope projection”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-ledger-gating.md`; `docs/plans/battery-launcher-matrix.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:82`:

> {:sha "e8dfa135a1fc25336583c302dfafa65d99047e36", :started "2026-09-15T07:11:55Z", :wall_s 211, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-receipt-5189b1d4 && git worktree add --detach <dir> archive/fable/battery-receipt-5189b1d4`

### archive/fable/battery-receipt-552de388

Branch: `fable/battery-receipt-552de388`. Tip: `552de388` · 2026-09-14T01:13:11Z. Commits not on main: **2844**.

Integrates the transaction-race fix into Darwin home lookup while retaining both sets of tests.

Subjects: “merge: origin/MCP/main e2050d8f (txn race fix) into skiff/darwin-passwd-home — both-added deftests kept; census regenerated” · “merge: txn: a path removed inside receipt-artifacts' admission windows resolves absent, never throws (the tombstone-collision race, reproduced as 21 deterministic schedules; inb-c74f05) ; battery receipt via land-auto” · “battery: receipt for 5ae0066c75ef3b19d28c94405aa674573ce5a29a merged onto trunk (landing gate battery-fresh)”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-launcher-matrix.md`; `docs/plans/battery-archival-distance.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:76`:

> {:sha "5ae0066c75ef3b19d28c94405aa674573ce5a29a", :started "2026-09-14T00:47:33Z", :wall_s 197, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-receipt-552de388 && git worktree add --detach <dir> archive/fable/battery-receipt-552de388`

### archive/fable/battery-receipt-694b092f

Branch: `fable/battery-receipt-694b092f`. Tip: `39d2cc83` · 2026-09-13T23:25:48Z. Commits not on main: **2838**.

Applies the home-lookup fence review: lazy evaluation, real I/O failures, validated dscl output, and sanitized attempts.

Subjects: “fence: Sol's GO-WITH-FIX for 694b092f4e46 — Make passwd-home lazy, preserve real I/O failures, validate dscl output, report sanitized attempts, and census the cross” · “census: +1 deftest for the darwin passwd-home witness (parse-dscl-home-reads-the-nfshomedirectory-line)” · “fix(darwin): passwd-home falls through getent -> dscl instead of throwing at load”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-launcher-matrix.md`; `docs/plans/battery-archival-distance.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:75`:

> {:sha "b33bf3b0d119bff1a88a827f4f7552ac9b204120", :started "2026-09-13T05:34:43Z", :wall_s 178, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-receipt-694b092f && git worktree add --detach <dir> archive/fable/battery-receipt-694b092f`

### archive/fable/battery-receipt-6e77580f

Branch: `fable/battery-receipt-6e77580f`. Tip: `09af5957` · 2026-09-13T22:52:05Z. Commits not on main: **2853**.

Records receipt for 3cd911930ba759a67434c264ff84c9ee43aa225d merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 3cd911930ba759a67434c264ff84c9ee43aa225d merged onto trunk (landing gate battery-fresh)” · “merge: state-home: warm-image identity descriptor lives under the seat state root, never the checkout (inb-3c65d7); state root admitted before any grant; atomic descriptor publication; filesystem-typed probe refusals pinned and registered; in-process admission matrix + bounded warm witnesses; registered second encounter of the envelope-writer class (recurrence 2)” · “Record Round 7 battery pass and measured namespace costs”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-launcher-matrix.md`; `docs/plans/battery-archival-distance.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:77`:

> {:sha "3cd911930ba759a67434c264ff84c9ee43aa225d", :started "2026-09-13T22:48:58Z", :wall_s 186, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-receipt-6e77580f && git worktree add --detach <dir> archive/fable/battery-receipt-6e77580f`

### archive/fable/battery-receipt-73f8e38d

Branch: `fable/battery-receipt-73f8e38d`. Tip: `73f8e38d` · 2026-09-13T18:56:10Z. Commits not on main: **2842**.

Moves the slow probe-state test into the battery lane and corrects metadata after a pipeline masked a red result.

Subjects: “lanes: probe-state-test ns metadata declares :battery (fix-up of 4e2e7c8f, which was pushed red: the lane witness failure was masked by a pipeline)” · “lanes: clj-surgeon.probe-state-test moves :integration -> :battery (126 s under the gate wrapper vs the 20 s fast budget; battery ceiling 1800 s)” · “test: inherit runner TMPDIR in warm state fixture”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-launcher-matrix.md`; `docs/plans/battery-archival-distance.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:75`:

> {:sha "b33bf3b0d119bff1a88a827f4f7552ac9b204120", :started "2026-09-13T05:34:43Z", :wall_s 178, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-receipt-73f8e38d && git worktree add --detach <dir> archive/fable/battery-receipt-73f8e38d`

### archive/fable/battery-receipt-76cc24a4

Branch: `fable/battery-receipt-76cc24a4`. Tip: `76cc24a4` · 2026-09-13T03:14:11Z. Commits not on main: **2829**.

Closes reverse namespace dependencies to a fixed point and witnesses omitted depth-three chains.

Subjects: “fix(diff-impact): close reverse namespace dependencies to a fixed point” · “test(diff-impact): reproduce omitted depth-three dependency chains” · “Allow the merged phase label for the round 7 impact oracle”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-launcher-matrix.md`; `docs/plans/battery-archival-distance.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:74`:

> {:sha "ca749adbf4d61ff6d323ae8e2fc15910e11eef51", :started "2026-09-13T00:15:48Z", :wall_s 181, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-receipt-76cc24a4 && git worktree add --detach <dir> archive/fable/battery-receipt-76cc24a4`

### archive/fable/battery-receipt-7728eb43

Branch: `fable/battery-receipt-7728eb43`. Tip: `0ea94f77` · 2026-09-14T02:00:21Z. Commits not on main: **2872**.

Records receipt for 023e3fc92ff7a86a62a603082448e71907fb5020 merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 023e3fc92ff7a86a62a603082448e71907fb5020 merged onto trunk (landing gate battery-fresh)” · “merge: darwin: passwd-home resolves lazily and falls through getent -> dscl instead of throwing at load (Skiff blocker; mayor@skiff session kiloclaw-ff 7799b9c7 + Sol's fix 39d2cc83, merged with the race fix)” · “merge: state-home: warm-image identity descriptor lives under the seat state root, never the checkout (inb-3c65d7); state root admitted before any grant; atomic publication with after-create cleanup; probe receipt bounded after provenance; filesystem-typed refusals pinned and registered; in-process admission matrix + bounded warm witnesses; second encounter of the envelope-writer class (recurrence 2) ; battery receipt via land-auto”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-launcher-matrix.md`; `docs/plans/battery-archival-distance.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:79`:

> {:sha "023e3fc92ff7a86a62a603082448e71907fb5020", :started "2026-09-14T01:56:40Z", :wall_s 221, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-receipt-7728eb43 && git worktree add --detach <dir> archive/fable/battery-receipt-7728eb43`

### archive/fable/battery-receipt-7799b9c7

Branch: `fable/battery-receipt-7799b9c7`. Tip: `7799b9c7` · 2026-09-13T12:27:46-07:00. Commits not on main: **2836**.

Makes Darwin home lookup fall through from getent to dscl rather than throw during load.

Subjects: “fix(darwin): passwd-home falls through getent -> dscl instead of throwing at load” · “merge: data-not-code: destination envelope admission at the artifact boundary, refusal classification as data, receipt-backed evidence rows, sleep identities, fixed-point diff-impact oracle in the gate's environment, probe measure preregistration ; battery receipt via land-auto” · “battery: receipt for b33bf3b0d119bff1a88a827f4f7552ac9b204120 merged onto trunk (landing gate battery-fresh)”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-launcher-matrix.md`; `docs/plans/battery-archival-distance.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:75`:

> {:sha "b33bf3b0d119bff1a88a827f4f7552ac9b204120", :started "2026-09-13T05:34:43Z", :wall_s 178, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-receipt-7799b9c7 && git worktree add --detach <dir> archive/fable/battery-receipt-7799b9c7`

### archive/fable/battery-receipt-7bf44cfe

Branch: `fable/battery-receipt-7bf44cfe`. Tip: `0e82dc99` · 2026-09-13T23:53:37Z. Commits not on main: **2855**.

Records receipt for 917132d178cc6b9208dd06a3f8113f8201093678 merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 917132d178cc6b9208dd06a3f8113f8201093678 merged onto trunk (landing gate battery-fresh)” · “merge: state-home: warm-image identity descriptor lives under the seat state root, never the checkout (inb-3c65d7); state root admitted before any grant; atomic publication with after-create cleanup; filesystem-typed probe refusals pinned and registered; in-process admission matrix + bounded warm witnesses; second encounter of the envelope-writer class (recurrence 2)” · “fix: clean temporary probe images after side-effect failures”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-launcher-matrix.md`; `docs/plans/battery-archival-distance.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:77`:

> {:sha "917132d178cc6b9208dd06a3f8113f8201093678", :started "2026-09-13T23:50:02Z", :wall_s 214, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-receipt-7bf44cfe && git worktree add --detach <dir> archive/fable/battery-receipt-7bf44cfe`

### archive/fable/battery-receipt-839b7c53

Branch: `fable/battery-receipt-839b7c53`. Tip: `1b567a90` · 2026-09-13T00:18:49Z. Commits not on main: **2807**.

Records receipt for ca749adbf4d61ff6d323ae8e2fc15910e11eef51 merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for ca749adbf4d61ff6d323ae8e2fc15910e11eef51 merged onto trunk (landing gate battery-fresh)” · “merge: bb-rewrite-tower blocks A+B: babashka-first hybrid, probe verb (authorized target, complete closure, classpath-derived roots, bounded and typed HTTP boundary), TEST-ISO-016 six-run + JVM-reference portability rule, bb lane ceiling, envelope-clean gate; battery receipt bound” · “test: pin the two new probe refusal kinds in the frozen entrance enumeration (164 -> 166)”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-launcher-matrix.md`; `docs/plans/battery-archival-distance.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:74`:

> {:sha "ca749adbf4d61ff6d323ae8e2fc15910e11eef51", :started "2026-09-13T00:15:48Z", :wall_s 181, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-receipt-839b7c53 && git worktree add --detach <dir> archive/fable/battery-receipt-839b7c53`

### archive/fable/battery-receipt-88b8603d

Branch: `fable/battery-receipt-88b8603d`. Tip: `a764be87` · 2026-09-15T07:46:43Z. Commits not on main: **2897**.

Records receipt for 2dce4893bcc0a4703c18c61266bb598426b166fc merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 2dce4893bcc0a4703c18c61266bb598426b166fc merged onto trunk (landing gate battery-fresh)” · “merge: diff-impact: selections run through the gate's own lane executors (:fast and :all scopes) with total membership classification before projection; partial receipts that can never pass as gate receipts; child argv parity; parity oracle requires successful scoped execution (inb-b142af, inb-fbd110)” · “fence: Sol's GO-WITH-FIX for 5189b1d4a578 — Require successful control, scoped execution, and passed problem-free child receipts before parity can pass.”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-ledger-gating.md`; `docs/plans/battery-launcher-matrix.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:82`:

> {:sha "2dce4893bcc0a4703c18c61266bb598426b166fc", :started "2026-09-15T07:43:26Z", :wall_s 197, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-receipt-88b8603d && git worktree add --detach <dir> archive/fable/battery-receipt-88b8603d`

### archive/fable/battery-receipt-9267d814

Branch: `fable/battery-receipt-9267d814`. Tip: `551b170d` · 2026-09-14T01:46:48Z. Commits not on main: **2863**.

Records receipt for 2091bb4c84549a3bd741a63a80caed133f390d07 merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 2091bb4c84549a3bd741a63a80caed133f390d07 merged onto trunk (landing gate battery-fresh)” · “merge: state-home: warm-image identity descriptor lives under the seat state root, never the checkout (inb-3c65d7); state root admitted before any grant; atomic publication with after-create cleanup; probe receipt bounded after provenance; filesystem-typed refusals pinned and registered; in-process admission matrix + bounded warm witnesses; second encounter of the envelope-writer class (recurrence 2)” · “merge: origin/MCP/main e2050d8f (txn race fix) into fable/state-home-identity — battery evidence ledgers merged (both rows kept; walls from trunk); census verified”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-launcher-matrix.md`; `docs/plans/battery-archival-distance.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:78`:

> {:sha "2091bb4c84549a3bd741a63a80caed133f390d07", :started "2026-09-14T01:43:27Z", :wall_s 201, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-receipt-9267d814 && git worktree add --detach <dir> archive/fable/battery-receipt-9267d814`

### archive/fable/battery-receipt-99b57bd1

Branch: `fable/battery-receipt-99b57bd1`. Tip: `99b57bd1` · 2026-09-14T11:21:44Z. Commits not on main: **2888**.

Executes fresh registration controls, closes census gaps, and records child processes and root-lock refusals.

Subjects: “fix: execute fresh registration controls and close gate census gaps” · “fix: record registration children and isolate root lock refusals” · “docs: record registration red green and executed dogfood receipts”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-ledger-gating.md`; `docs/observations/2026-09-14-battery-ledger-gating-report-astra.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:80`:

> {:sha "1eb8c4012a5eefeaa5bfbf6a423aca85c8d7ce45", :started "2026-09-14T06:32:44Z", :wall_s 204, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-receipt-99b57bd1 && git worktree add --detach <dir> archive/fable/battery-receipt-99b57bd1`

### archive/fable/battery-receipt-9d057f8d

Branch: `fable/battery-receipt-9d057f8d`. Tip: `9d057f8d` · 2026-09-13T02:15:00Z. Commits not on main: **2827**.

Integrates landed Block B into data-not-code and admits the merged phase label in the impact oracle.

Subjects: “Allow the merged phase label for the round 7 impact oracle” · “Merge landed Block B 759974c7 into data-not-code eb6f59b4” · “fix(data-not-code): account for internal links and verify impacted witnesses”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-launcher-matrix.md`; `docs/plans/battery-archival-distance.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:74`:

> {:sha "ca749adbf4d61ff6d323ae8e2fc15910e11eef51", :started "2026-09-13T00:15:48Z", :wall_s 181, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-receipt-9d057f8d && git worktree add --detach <dir> archive/fable/battery-receipt-9d057f8d`

### archive/fable/battery-receipt-a233ce66

Branch: `fable/battery-receipt-a233ce66`. Tip: `df86ecda` · 2026-09-12T23:05:17Z. Commits not on main: **2802**.

Records receipt for a233ce66a90ae875ccaf8991b1b13e626451bf4b merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for a233ce66a90ae875ccaf8991b1b13e626451bf4b merged onto trunk (landing gate battery-fresh)” · “merge: bb-rewrite-tower blocks A+B: babashka-first hybrid, probe verb (authorized target, complete closure, classpath-derived roots, bound and typed HTTP refusals), TEST-ISO-016 six-run + JVM-reference portability rule, bb lane ceiling, envelope-clean gate” · “test: register the autofix's probe-servlet request-bound witness in the deftest census (+1 name, 0 removed); battery ledger row from packet da68a598”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-launcher-matrix.md`; `docs/plans/battery-archival-distance.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:72`:

> {:sha "14644aad1c724a0dc96fc50c9e9e0c59eb762fb2", :started "2026-09-12T22:04:25Z", :wall_s 161, :verdict :fail, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-receipt-a233ce66 && git worktree add --detach <dir> archive/fable/battery-receipt-a233ce66`

### archive/fable/battery-receipt-ab3febad

Branch: `fable/battery-receipt-ab3febad`. Tip: `577544b0` · 2026-09-14T10:48:28Z. Commits not on main: **2886**.

Corrects diff-impact documentation to reserve nothing-selected for empty diffs and HOLD unmatched nonempty diffs.

Subjects: “fence: Sol's GO-WITH-FIX for ab3febadeaf5 — Correct the changelog to document HOLD for unmatched nonempty diffs and reserve nothing-selected for empty diffs.” · “fix: hold unmatched diff-impact gates and propagate source content edges” · “test: strengthen diff-impact fail-closed contract and commit Sol F1 red”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-ledger-gating.md`; `docs/observations/2026-09-14-battery-ledger-gating-report-astra.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:80`:

> {:sha "1eb8c4012a5eefeaa5bfbf6a423aca85c8d7ce45", :started "2026-09-14T06:32:44Z", :wall_s 204, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-receipt-ab3febad && git worktree add --detach <dir> archive/fable/battery-receipt-ab3febad`

### archive/fable/battery-receipt-ab3febad-2

Branch: `fable/battery-receipt-ab3febad-2`. Tip: `3bcdfd42` · 2026-09-14T10:59:52Z. Commits not on main: **2887**.

Records receipt for 198f54816c8f175fbe1d318c100591d544d2f896 merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 198f54816c8f175fbe1d318c100591d544d2f896 merged onto trunk (landing gate battery-fresh)” · “merge: diff-impact: file-content, source-scan and test-side require edges propagated over the src graph; HOLD on unmatched files; typed selection reasons (inb-f85401, inb-1b7f3c)” · “fix: hold unmatched diff-impact gates and propagate source content edges”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-ledger-gating.md`; `docs/observations/2026-09-14-battery-ledger-gating-report-astra.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:81`:

> {:sha "198f54816c8f175fbe1d318c100591d544d2f896", :started "2026-09-14T10:56:27Z", :wall_s 205, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-receipt-ab3febad-2 && git worktree add --detach <dir> archive/fable/battery-receipt-ab3febad-2`

### archive/fable/battery-receipt-ab3febad-3

Branch: `fable/battery-receipt-ab3febad-3`. Tip: `f4891346` · 2026-09-14T11:26:23Z. Commits not on main: **2887**.

Records receipt for d033498227b3733874207cfae1063bd4bb0d848f merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for d033498227b3733874207cfae1063bd4bb0d848f merged onto trunk (landing gate battery-fresh)” · “merge: diff-impact: file-content, source-scan and test-side require edges propagated over the src graph; HOLD on unmatched files; typed selection reasons (inb-f85401, inb-1b7f3c)” · “fix: hold unmatched diff-impact gates and propagate source content edges”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-ledger-gating.md`; `docs/observations/2026-09-14-battery-ledger-gating-report-astra.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:81`:

> {:sha "d033498227b3733874207cfae1063bd4bb0d848f", :started "2026-09-14T11:23:05Z", :wall_s 198, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-receipt-ab3febad-3 && git worktree add --detach <dir> archive/fable/battery-receipt-ab3febad-3`

### archive/fable/battery-receipt-b2887b5a

Branch: `fable/battery-receipt-b2887b5a`. Tip: `3f620c43` · 2026-09-15T08:45:29Z. Commits not on main: **2899**.

Records receipt for 13eafcb64687321cd73c1dbde0e9d933ab36cfbb merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 13eafcb64687321cd73c1dbde0e9d933ab36cfbb merged onto trunk (landing gate battery-fresh)” · “merge: diff-impact: selections run through the gate's own lane executors (:fast and :all scopes) with total membership classification; partial receipts that can never pass as gate receipts; child argv parity; receipts carry the complete landing-envelope evidence (inb-b142af, inb-fbd110)” · “docs: record round 3 prewarm and installed envelope acceptance”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-ledger-gating.md`; `docs/plans/battery-launcher-matrix.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:82`:

> {:sha "13eafcb64687321cd73c1dbde0e9d933ab36cfbb", :started "2026-09-15T08:42:03Z", :wall_s 206, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-receipt-b2887b5a && git worktree add --detach <dir> archive/fable/battery-receipt-b2887b5a`

### archive/fable/battery-receipt-bc65a76a

Branch: `fable/battery-receipt-bc65a76a`. Tip: `06ef626a` · 2026-09-14T00:50:50Z. Commits not on main: **2839**.

Records receipt for 5ae0066c75ef3b19d28c94405aa674573ce5a29a merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 5ae0066c75ef3b19d28c94405aa674573ce5a29a merged onto trunk (landing gate battery-fresh)” · “merge: txn: a path removed inside receipt-artifacts' admission windows resolves absent, never throws (the tombstone-collision race, reproduced as 21 deterministic schedules; inb-c74f05)” · “green: a path removed inside the admission windows resolves absent, not throws”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-launcher-matrix.md`; `docs/plans/battery-archival-distance.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:76`:

> {:sha "5ae0066c75ef3b19d28c94405aa674573ce5a29a", :started "2026-09-14T00:47:33Z", :wall_s 197, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-receipt-bc65a76a && git worktree add --detach <dir> archive/fable/battery-receipt-bc65a76a`

### archive/fable/battery-receipt-bfbec7f9

Branch: `fable/battery-receipt-bfbec7f9`. Tip: `39181ec8` · 2026-09-14T12:35:08Z. Commits not on main: **2900**.

Records receipt for f6cbc21368aef14d98651671e65f17246b59164c merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for f6cbc21368aef14d98651671e65f17246b59164c merged onto trunk (landing gate battery-fresh)” · “merge: test registration: one entrance — registry oracle lists every missing surface at first contact; make register-test-ns performs the registrations as form edits with executed, provenance-bound controls; one census writer (inb-b10334)” · “merge: MCP/main 866df39b into regns-entrance — union of registration surfaces”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-ledger-gating.md`; `docs/plans/battery-launcher-matrix.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:82`:

> {:sha "f6cbc21368aef14d98651671e65f17246b59164c", :started "2026-09-14T12:31:06Z", :wall_s 241, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-receipt-bfbec7f9 && git worktree add --detach <dir> archive/fable/battery-receipt-bfbec7f9`

### archive/fable/battery-receipt-c3d6c4e9

Branch: `fable/battery-receipt-c3d6c4e9`. Tip: `c3d6c4e9` · 2026-09-14T14:28:04Z. Commits not on main: **2902**.

Isolates the registration Make matrix and protects live bytes during copied-matrix tests.

Subjects: “test: isolate registration Make matrix and guard live bytes” · “test: ratchet copied registration matrix isolation” · “fix: keep registration count failures at repository scope”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-ledger-gating.md`; `docs/plans/battery-launcher-matrix.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:81`:

> {:sha "d033498227b3733874207cfae1063bd4bb0d848f", :started "2026-09-14T11:23:05Z", :wall_s 198, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-receipt-c3d6c4e9 && git worktree add --detach <dir> archive/fable/battery-receipt-c3d6c4e9`

### archive/fable/battery-receipt-c6f40787

Branch: `fable/battery-receipt-c6f40787`. Tip: `c6f40787` · 2026-09-14T09:44:51Z. Commits not on main: **2883**.

Adds file-content and test-helper edges to diff-impact selection with a failing selected-set oracle.

Subjects: “fix: include file-content and test-helper edges in diff-impact selection” · “test: commit red diff-impact selected-set class oracle (inb-f85401, inb-1b7f3c)” · “merge: battery: the tracked ledger row is appended only when a receipt is being minted (BATTERY_LEDGER_APPEND); namespace walls live in the seat state root under state-home admission with the tracked seed read-only; make census-regenerate (inb-16b396) ; battery receipt via land-auto”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-ledger-gating.md`; `docs/observations/2026-09-14-battery-ledger-gating-report-astra.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:80`:

> {:sha "1eb8c4012a5eefeaa5bfbf6a423aca85c8d7ce45", :started "2026-09-14T06:32:44Z", :wall_s 204, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-receipt-c6f40787 && git worktree add --detach <dir> archive/fable/battery-receipt-c6f40787`

### archive/fable/battery-receipt-cb42682a

Branch: `fable/battery-receipt-cb42682a`. Tip: `06c17417` · 2026-09-14T06:36:08Z. Commits not on main: **2880**.

Records receipt for 1eb8c4012a5eefeaa5bfbf6a423aca85c8d7ce45 merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 1eb8c4012a5eefeaa5bfbf6a423aca85c8d7ce45 merged onto trunk (landing gate battery-fresh)” · “merge: battery: the tracked ledger row is appended only when a receipt is being minted (BATTERY_LEDGER_APPEND); namespace walls live in the seat state root under state-home admission with the tracked seed read-only; make census-regenerate (inb-16b396)” · “docs: record round-two state writer audit and verification”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-ledger-gating.md`; `docs/observations/2026-09-14-battery-ledger-gating-report-astra.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:80`:

> {:sha "1eb8c4012a5eefeaa5bfbf6a423aca85c8d7ce45", :started "2026-09-14T06:32:44Z", :wall_s 204, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-receipt-cb42682a && git worktree add --detach <dir> archive/fable/battery-receipt-cb42682a`

### archive/fable/battery-receipt-cbef534b-3

Branch: `fable/battery-receipt-cbef534b-3`. Tip: `1cf6f0dd` · 2026-09-08T20:18:46Z. Commits not on main: **2458**.

Records receipt for 4def82c09b5750af6fe3d3a751f9d1670543e385 merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 4def82c09b5750af6fe3d3a751f9d1670543e385 merged onto trunk (landing gate battery-fresh)” · “merge: TEST-ISO-013: parallel battery coordinator — 839 s → 218–229 s with identical verdicts; 8 lanes + prereqs by default (Sol's ruling); test-ns-hook shard refusal; per-var walls preserved” · “captain's log: item 4 adapters + grader; ship stall on a dirty receipt worktree; lease takeover worked”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-08-tighten/sol-battery-parallel-fence2-NO-GO.md`; `docs/observations/2026-09-08-battery-parallel-verdict-parity.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:49`:

> {:sha "4def82c09b5750af6fe3d3a751f9d1670543e385", :started "2026-09-08T20:15:08Z", :wall_s 217, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-receipt-cbef534b-3 && git worktree add --detach <dir> archive/fable/battery-receipt-cbef534b-3`

### archive/fable/battery-receipt-cdc18278

Branch: `fable/battery-receipt-cdc18278`. Tip: `205b6188` · 2026-09-24T18:24:25Z. Commits not on main: **2904**.

Records receipt for cdc18278bc9467d52fed2bff6bbef375259ccbf5 merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for cdc18278bc9467d52fed2bff6bbef375259ccbf5 merged onto trunk (landing gate battery-fresh)” · “fix: onboarding writes the Codex Surgeon entry as optional” · “merge: diff-impact: selections run through the gate's own lane executors (:fast and :all scopes) with total membership classification and fast-scope-empty refusal; partial receipts that can never pass as gate receipts; child argv parity; receipts carry the complete landing-envelope evidence (inb-b142af, inb-fbd110) ; battery receipt via land-auto”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-ledger-gating.md`; `docs/plans/battery-launcher-matrix.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:83`:

> {:sha "cdc18278bc9467d52fed2bff6bbef375259ccbf5", :started "2026-09-24T18:21:11Z", :wall_s 194, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-receipt-cdc18278 && git worktree add --detach <dir> archive/fable/battery-receipt-cdc18278`

### archive/fable/battery-receipt-ce08cb01

Branch: `fable/battery-receipt-ce08cb01`. Tip: `47c5706a` · 2026-09-13T05:37:41Z. Commits not on main: **2834**.

Records receipt for b33bf3b0d119bff1a88a827f4f7552ac9b204120 merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for b33bf3b0d119bff1a88a827f4f7552ac9b204120 merged onto trunk (landing gate battery-fresh)” · “merge: data-not-code: destination envelope admission at the artifact boundary, refusal classification as data, receipt-backed evidence rows, sleep identities, fixed-point diff-impact oracle in the gate's environment, probe measure preregistration” · “fix(data-not-code): bound state substitutions and use JVM test scratch roots”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-launcher-matrix.md`; `docs/plans/battery-archival-distance.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:75`:

> {:sha "b33bf3b0d119bff1a88a827f4f7552ac9b204120", :started "2026-09-13T05:34:43Z", :wall_s 178, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-receipt-ce08cb01 && git worktree add --detach <dir> archive/fable/battery-receipt-ce08cb01`

### archive/fable/battery-receipt-cf237f96

Branch: `fable/battery-receipt-cf237f96`. Tip: `7e4a2480` · 2026-09-15T06:10:02Z. Commits not on main: **2894**.

Records receipt for ebf01a960683eb6f419f7cee15e9fcd792aed7dd merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for ebf01a960683eb6f419f7cee15e9fcd792aed7dd merged onto trunk (landing gate battery-fresh)” · “merge: diff-impact: selections run through the gate's own lane executors (:fast and :all scopes), partial receipts that can never pass as gate receipts, child argv parity with the fast lane (inb-b142af, inb-fbd110)” · “docs: normalize executor report ending”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-ledger-gating.md`; `docs/plans/battery-launcher-matrix.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:82`:

> {:sha "ebf01a960683eb6f419f7cee15e9fcd792aed7dd", :started "2026-09-15T06:06:39Z", :wall_s 203, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-receipt-cf237f96 && git worktree add --detach <dir> archive/fable/battery-receipt-cf237f96`

### archive/fable/battery-receipt-df0c9e1c

Branch: `fable/battery-receipt-df0c9e1c`. Tip: `df0c9e1c` · 2026-09-13T20:51:27Z. Commits not on main: **2848**.

Declares filesystem refusal forwarding and registers the measured cost of the state-root oracle.

Subjects: “fix: declare filesystem refusal forwarding for the source census” · “test: register measured cost of the Cartesian state-root oracle” · “fix: close state-home failure classes; pin refusal enumeration 169 -> 176”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-launcher-matrix.md`; `docs/plans/battery-archival-distance.md`.

Latest retained battery row (its SHA is the measured candidate; no claim that it verifies this tag tip), `docs/observations/battery-ledger.edn:75`:

> {:sha "b33bf3b0d119bff1a88a827f4f7552ac9b204120", :started "2026-09-13T05:34:43Z", :wall_s 178, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-receipt-df0c9e1c && git worktree add --detach <dir> archive/fable/battery-receipt-df0c9e1c`

### archive/fable/battery-receipt-df3f9a18

Branch: `fable/battery-receipt-df3f9a18`. Tip: `0bb37331` · 2026-09-08T17:07:02Z. Commits not on main: **2402**.

Records receipt for df3f9a18fa14b2cf7494d82eb06688b725dc0c80 merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for df3f9a18fa14b2cf7494d82eb06688b725dc0c80 merged onto trunk (landing gate battery-fresh)” · “Merge commit 'a9da43441234f0f815ab9c9393ae03dda7fa82f4' into astra/rows3-receipt-a9da4344” · “captain's log: Sol's cross-attack; week ruling with Sol; ref split filed for Gene”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-archival-distance.md`; `docs/observations/2026-09-06-astra-battery-archive-distance.md`.

Latest retained battery row (its SHA is the measured candidate; no claim that it verifies this tag tip), `docs/observations/battery-ledger.edn:40`:

> {:sha "970ad7456ad99a4ca4b799a3b3f8e243293fd583", :started "2026-09-08T16:53:29Z", :wall_s 813, :verdict :pass, :host "anvil-server"}

Checkout: `git fetch origin tag archive/fable/battery-receipt-df3f9a18 && git worktree add --detach <dir> archive/fable/battery-receipt-df3f9a18`

### archive/fable/battery-receipt-df86ecda

Branch: `fable/battery-receipt-df86ecda`. Tip: `0be9d041` · 2026-09-12T23:29:28Z. Commits not on main: **2804**.

Records receipt for ea5e5ed0761b894c8640ac71563f6cd439ab65ef merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for ea5e5ed0761b894c8640ac71563f6cd439ab65ef merged onto trunk (landing gate battery-fresh)” · “merge: bb-rewrite-tower blocks A+B: babashka-first hybrid, probe verb (authorized target, complete closure, classpath-derived roots, bound and typed HTTP refusals), TEST-ISO-016 six-run + JVM-reference portability rule, bb lane ceiling, envelope-clean gate; battery receipt bound” · “battery: receipt for a233ce66a90ae875ccaf8991b1b13e626451bf4b merged onto trunk (landing gate battery-fresh)”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-launcher-matrix.md`; `docs/plans/battery-archival-distance.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:74`:

> {:sha "ea5e5ed0761b894c8640ac71563f6cd439ab65ef", :started "2026-09-12T23:26:26Z", :wall_s 182, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-receipt-df86ecda && git worktree add --detach <dir> archive/fable/battery-receipt-df86ecda`

### archive/fable/battery-receipt-e19ce755

Branch: `fable/battery-receipt-e19ce755`. Tip: `e4b0cca1` · 2026-09-15T09:12:33Z. Commits not on main: **2901**.

Records receipt for 5290bae14383fe025375b80a77863ad63ad6b9aa merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 5290bae14383fe025375b80a77863ad63ad6b9aa merged onto trunk (landing gate battery-fresh)” · “merge: diff-impact: selections run through the gate's own lane executors (:fast and :all scopes) with total membership classification and fast-scope-empty refusal; partial receipts that can never pass as gate receipts; child argv parity; receipts carry the complete landing-envelope evidence (inb-b142af, inb-fbd110)” · “fix: refuse nonempty selections with no fast members”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-ledger-gating.md`; `docs/plans/battery-launcher-matrix.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:82`:

> {:sha "5290bae14383fe025375b80a77863ad63ad6b9aa", :started "2026-09-15T09:08:59Z", :wall_s 214, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-receipt-e19ce755 && git worktree add --detach <dir> archive/fable/battery-receipt-e19ce755`

### archive/fable/battery-receipt-e81a9c75

Branch: `fable/battery-receipt-e81a9c75`. Tip: `e81a9c75` · 2026-09-14T10:10:08Z. Commits not on main: **2887**.

Records registration child processes and isolates root-lock refusals, with red/green dogfood evidence.

Subjects: “fix: record registration children and isolate root lock refusals” · “docs: record registration red green and executed dogfood receipts” · “test: drive the complete registration oracle from a missing-surface root”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-ledger-gating.md`; `docs/observations/2026-09-14-battery-ledger-gating-report-astra.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:80`:

> {:sha "1eb8c4012a5eefeaa5bfbf6a423aca85c8d7ce45", :started "2026-09-14T06:32:44Z", :wall_s 204, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-receipt-e81a9c75 && git worktree add --detach <dir> archive/fable/battery-receipt-e81a9c75`

### archive/fable/battery-receipt-f0775312

Branch: `fable/battery-receipt-f0775312`. Tip: `cea52595` · 2026-09-14T02:20:56Z. Commits not on main: **2873**.

Records receipt for d3b221c4f60f01d7dbbae76d609d847ee5c2455b merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for d3b221c4f60f01d7dbbae76d609d847ee5c2455b merged onto trunk (landing gate battery-fresh)” · “merge: darwin: passwd-home resolves lazily and falls through getent -> dscl instead of throwing at load (Skiff blocker; mayor@skiff session kiloclaw-ff 7799b9c7 + Sol's fixes 39d2cc83 and F1, merged with the race fix)” · “fence: Sol's GO-WITH-FIX for 7728eb43 (ship 20260914T015623Z) applied by the seat after the autofix refused fix-branch-race — passwd-home short-circuits after getent success and rethrows operational failures from uninspectable command paths”.

Added observation/plan documents (merge-base comparison): `docs/plans/battery-launcher-matrix.md`; `docs/plans/battery-archival-distance.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:79`:

> {:sha "d3b221c4f60f01d7dbbae76d609d847ee5c2455b", :started "2026-09-14T02:17:32Z", :wall_s 204, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/battery-receipt-f0775312 && git worktree add --detach <dir> archive/fable/battery-receipt-f0775312`

### archive/fable/census-derived

Branch: `fable/census-derived`. Tip: `e6ecd815` · 2026-09-09T05:06:12Z. Commits not on main: **2539**.

Derives the test census and intent ledger from the tree, makes every test name explicit, and prevents the gate from rewriting its own evidence.

Subjects: “fix(fence): CENSUS-REVIEW-DIFF-001, CENSUS-FENCE-001-DOC residue — Sol's GO-WITH-FIX on da439651, applied verbatim” · “test: the ledger names every deftest, and the gate cannot rewrite it” · “docs: TEST-ISO-015's misreadings match the repaired witnesses”.

Added observation/plan documents (merge-base comparison): `docs/observations/census-round25-review-opus.md`; `docs/observations/census-round23-review-opus.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:54`:

> {:sha "2e543e6aefb34093bde630c4c31ed5d16e36de9c", :started "2026-09-09T02:06:04Z", :wall_s 148, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/census-derived && git worktree add --detach <dir> archive/fable/census-derived`

### archive/fable/census-derived-receipt-3b63347f

Branch: `fable/census-derived-receipt-3b63347f`. Tip: `a9237b03` · 2026-09-09T04:43:37Z. Commits not on main: **2539**.

Records receipt for 319b08145ad5a0179461c4484aff2aa334ef6c34 merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 319b08145ad5a0179461c4484aff2aa334ef6c34 merged onto trunk (landing gate battery-fresh)” · “merge: merge: census and intent-ledger pins derived from the tree (TEST-ISO-015)” · “docs: TEST-ISO-015's misreadings match the repaired witnesses”.

Added observation/plan documents (merge-base comparison): `docs/observations/census-round25-review-opus.md`; `docs/observations/census-round23-review-opus.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:55`:

> {:sha "319b08145ad5a0179461c4484aff2aa334ef6c34", :started "2026-09-09T04:41:08Z", :wall_s 148, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/census-derived-receipt-3b63347f && git worktree add --detach <dir> archive/fable/census-derived-receipt-3b63347f`

### archive/fable/census-derived-receipt-c0901e10

Branch: `fable/census-derived-receipt-c0901e10`. Tip: `5316edcf` · 2026-09-09T04:06:15Z. Commits not on main: **2537**.

Records receipt for 805373d92e70495e1aeb40b899e225d6bcc84cba merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 805373d92e70495e1aeb40b899e225d6bcc84cba merged onto trunk (landing gate battery-fresh)” · “merge: merge: census and intent-ledger pins derived from the tree (TEST-ISO-015)” · “test: derive the lane and intent censuses from the tree (TEST-ISO-015)”.

Added observation/plan documents (merge-base comparison): `docs/observations/census-round25-review-opus.md`; `docs/observations/census-round23-review-opus.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:55`:

> {:sha "805373d92e70495e1aeb40b899e225d6bcc84cba", :started "2026-09-09T04:03:45Z", :wall_s 150, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/census-derived-receipt-c0901e10 && git worktree add --detach <dir> archive/fable/census-derived-receipt-c0901e10`

### archive/fable/census-derived-receipt-da439651

Branch: `fable/census-derived-receipt-da439651`. Tip: `0b166d01` · 2026-09-09T04:55:08Z. Commits not on main: **2540**.

Records receipt for 1d73259d0beb21c97847fde393c8cf213222e5eb merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 1d73259d0beb21c97847fde393c8cf213222e5eb merged onto trunk (landing gate battery-fresh)” · “merge: merge: census and intent-ledger pins derived from the tree (TEST-ISO-015)” · “test: the ledger names every deftest, and the gate cannot rewrite it”.

Added observation/plan documents (merge-base comparison): `docs/observations/census-round25-review-opus.md`; `docs/observations/census-round23-review-opus.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:55`:

> {:sha "1d73259d0beb21c97847fde393c8cf213222e5eb", :started "2026-09-09T04:52:39Z", :wall_s 149, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/census-derived-receipt-da439651 && git worktree add --detach <dir> archive/fable/census-derived-receipt-da439651`

### archive/fable/census-derived-receipt-e6ecd815

Branch: `fable/census-derived-receipt-e6ecd815`. Tip: `45b28c80` · 2026-09-09T05:30:40Z. Commits not on main: **2541**.

Records receipt for 8288e1ba5bfb690f185adc4c37cb2126244bf300 merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 8288e1ba5bfb690f185adc4c37cb2126244bf300 merged onto trunk (landing gate battery-fresh)” · “merge: merge: census and intent-ledger pins derived from the tree (TEST-ISO-015)” · “fix(fence): CENSUS-REVIEW-DIFF-001, CENSUS-FENCE-001-DOC residue — Sol's GO-WITH-FIX on da439651, applied verbatim”.

Added observation/plan documents (merge-base comparison): `docs/observations/census-round25-review-opus.md`; `docs/observations/census-round23-review-opus.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:55`:

> {:sha "8288e1ba5bfb690f185adc4c37cb2126244bf300", :started "2026-09-09T05:28:10Z", :wall_s 150, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/census-derived-receipt-e6ecd815 && git worktree add --detach <dir> archive/fable/census-derived-receipt-e6ecd815`

### archive/fable/cli-splice-classpath-r1

Branch: `fable/cli-splice-classpath-r1`. Tip: `c74116ae` · 2026-09-11T14:47:17Z. Commits not on main: **2708**.

Bundles clj-splice in the installed CLI runtime and adds an installed-launcher witness.

Subjects: “battery: receipt for 582b62bc3f9ef0c2f2674c3b9cbcd4b712b74f0e merged onto trunk (landing gate battery-fresh)” · “merge: CLI: bundle clj-splice in the stable CLI runtime; installed-launcher witness; stale skill name corrected” · “fix: bundle clj-splice in the stable CLI runtime”.

Added observation/plan documents (merge-base comparison): `docs/plans/clj-splice.md`; `docs/plans/clj-splice-witness-map.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:63`:

> {:sha "582b62bc3f9ef0c2f2674c3b9cbcd4b712b74f0e", :started "2026-09-11T14:44:50Z", :wall_s 147, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/cli-splice-classpath-r1 && git worktree add --detach <dir> archive/fable/cli-splice-classpath-r1`

### archive/fable/clj-splice-r1

Branch: `fable/clj-splice-r1`. Tip: `4be489fe` · 2026-09-11T05:17:53Z. Commits not on main: **2695**.

Builds the small clj-splice library and records its witness consolidation, timings, and outstanding landing gate.

Subjects: “docs: report clj-splice build and remaining landing gate” · “docs: record splice counts timings and execution limits” · “test: point operation catalog at consolidated publication witnesses”.

Added observation/plan documents (merge-base comparison): `docs/plans/clj-splice.md`; `docs/plans/clj-splice-witness-map.md`.

Shared-history battery result (the row names its measured candidate, not a verdict on this branch’s own changes), `docs/observations/battery-ledger.edn:61`:

> {:sha "012c111e3d1a64a0740f70f2f21e8276ac65cc40", :started "2026-09-11T01:19:27Z", :wall_s 200, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/clj-splice-r1 && git worktree add --detach <dir> archive/fable/clj-splice-r1`

### archive/fable/clj-splice-r2

Branch: `fable/clj-splice-r2`. Tip: `d8ede86f` · 2026-09-11T05:59:58Z. Commits not on main: **2703**.

Lands the clj-splice rewrite-clj functions and ports insert_forms and rename_alias with consolidated contract witnesses.

Subjects: “battery: receipt for 4acc7e50d56c4ab85d7d843ded13a72328b878ff merged onto trunk (landing gate battery-fresh)” · “merge: clj-splice: three pure functions on rewrite-clj; rename_alias and insert_forms ported; witnesses 1,149 -> 517 (programs to contracts)” · “docs: record clj-splice red-team fixes and verification contract”.

Added observation/plan documents (merge-base comparison): `docs/plans/clj-splice.md`; `docs/plans/clj-splice-witness-map.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:62`:

> {:sha "4acc7e50d56c4ab85d7d843ded13a72328b878ff", :started "2026-09-11T05:57:26Z", :wall_s 152, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/clj-splice-r2 && git worktree add --detach <dir> archive/fable/clj-splice-r2`

### archive/fable/data-not-code

Branch: `fable/data-not-code`. Tip: `ce08cb01` · 2026-09-13T04:42:47Z. Commits not on main: **2832**.

Hardens artifact destinations and state substitutions as bounded data, with scratch-root defaults and diff-impact regressions in the actual gate environment.

Subjects: “fix(data-not-code): bound state substitutions and use JVM test scratch roots” · “test(data-not-code): pin bounded state context and all-test scratch defaults” · “test(diff-impact): reproduce battery red in a narrow gate environment”.

Added observation/plan documents (merge-base comparison): `docs/plans/data-not-code-round9.md`; `docs/observations/2026-09-12-data-not-code/round6/plan.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-09-12-data-not-code/round5/REPORT.md:3`:

> Verdict: **PASS — local fixes and required prewarm complete.** No push, release,
> merge or performance admission. Recorded 2026-09-12T22:38:47.340851+00:00.
> Base: `4658dc124ee243300d19861402b2696ae1046321`, branch
> `fable/data-not-code-local`. The landing SHA is the commit containing this report.

Checkout: `git fetch origin tag archive/fable/data-not-code && git worktree add --detach <dir> archive/fable/data-not-code`

### archive/fable/evidence-consumer

Branch: `fable/evidence-consumer`. Tip: `2f924bee` · 2026-09-09T17:58:20Z. Commits not on main: **2558**.

Makes the gate derive fixture registrations and bind evidence to rule bytes, nested checks, Var identities, shell resolution, and environment policy.

Subjects: “gate: derive the fixtures a namespace registers, and hash the whole rule surface” · “gate: the inventory states the environment policy and what evidence discharges what” · “gate: rule bytes, derived nested checks, Var identities, a resolved shell”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-06-strictly-better-evidence.md`; `docs/plans/parallel-landing-gate.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:56`:

> {:sha "a51dd39e955972ddac1d417d40e73e05f30637c1", :started "2026-09-09T08:06:00Z", :wall_s 149, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/evidence-consumer && git worktree add --detach <dir> archive/fable/evidence-consumer`

### archive/fable/evidence-consumer-receipt-1ed995c6

Branch: `fable/evidence-consumer-receipt-1ed995c6`. Tip: `86204c46` · 2026-09-09T15:02:16Z. Commits not on main: **2557**.

Records receipt for 801b00cfabf0f9e5c198388e1408ca2e3180b0e8 merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 801b00cfabf0f9e5c198388e1408ca2e3180b0e8 merged onto trunk (landing gate battery-fresh)” · “merge: merge: landing-gate obligation inventory and a receipt that says what it discharged (item 1: evidence consumer)” · “gate: a policy hash covers the RULES, not the data the rules read”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-06-strictly-better-evidence.md`; `docs/plans/parallel-landing-gate.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:57`:

> {:sha "801b00cfabf0f9e5c198388e1408ca2e3180b0e8", :started "2026-09-09T14:59:17Z", :wall_s 179, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/evidence-consumer-receipt-1ed995c6 && git worktree add --detach <dir> archive/fable/evidence-consumer-receipt-1ed995c6`

### archive/fable/evidence-consumer-receipt-45beeb7e

Branch: `fable/evidence-consumer-receipt-45beeb7e`. Tip: `19f53545` · 2026-09-09T15:38:07Z. Commits not on main: **2558**.

Records receipt for 79c94e6c425a084727d9e07d73ca0e62491aa640 merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 79c94e6c425a084727d9e07d73ca0e62491aa640 merged onto trunk (landing gate battery-fresh)” · “merge: merge: landing-gate obligation inventory and a receipt that says what it discharged (item 1: evidence consumer)” · “gate: rule bytes, derived nested checks, Var identities, a resolved shell”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-06-strictly-better-evidence.md`; `docs/plans/parallel-landing-gate.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:57`:

> {:sha "79c94e6c425a084727d9e07d73ca0e62491aa640", :started "2026-09-09T15:35:25Z", :wall_s 162, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/evidence-consumer-receipt-45beeb7e && git worktree add --detach <dir> archive/fable/evidence-consumer-receipt-45beeb7e`

### archive/fable/evidence-consumer-receipt-9dc25924

Branch: `fable/evidence-consumer-receipt-9dc25924`. Tip: `26da623c` · 2026-09-09T17:02:30Z. Commits not on main: **2559**.

Records receipt for 2d8f86a082129881a1cdcca6d77c711722e39505 merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 2d8f86a082129881a1cdcca6d77c711722e39505 merged onto trunk (landing gate battery-fresh)” · “merge: merge: landing-gate obligation inventory and a receipt that says what it discharged (item 1: evidence consumer)” · “gate: the inventory states the environment policy and what evidence discharges what”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-06-strictly-better-evidence.md`; `docs/plans/parallel-landing-gate.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:57`:

> {:sha "2d8f86a082129881a1cdcca6d77c711722e39505", :started "2026-09-09T16:59:49Z", :wall_s 161, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/evidence-consumer-receipt-9dc25924 && git worktree add --detach <dir> archive/fable/evidence-consumer-receipt-9dc25924`

### archive/fable/expect-guard-receipt

Branch: `fable/expect-guard-receipt`. Tip: `02f16605` · 2026-09-07T10:39:22Z. Commits not on main: **2109**.

Records receipt for fable/refusal-text-shape merged tip onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for fable/refusal-text-shape merged tip onto trunk (landing gate battery-fresh)” · “Merge commit '4711be0586a98d23f4e27daa7304e0e18802c82e' into fable/expect-guard-receipt” · “records: expect r5 GO; land refused by battery-fresh (35 > 30 commits); receipt chain running”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-07-expectfix-r5-sol-review.md`; `docs/observations/2026-09-07-expectfix-r4-sol-review.md`.

Latest retained battery row (its SHA is the measured candidate; no claim that it verifies this tag tip), `docs/observations/battery-ledger.edn:31`:

> {:sha "4992528951d9d443792a12318c88efcaa3a80d78", :started "2026-09-07T10:26:13Z", :wall_s 788, :verdict :pass, :host "anvil-server"}

Checkout: `git fetch origin tag archive/fable/expect-guard-receipt && git worktree add --detach <dir> archive/fable/expect-guard-receipt`

### archive/fable/fanout-route-plate

Branch: `fable/fanout-route-plate`. Tip: `3041b13c` · 2026-09-06T19:04:05Z. Commits not on main: **1895**.

Publishes an experimental fan-out routing plate and incorporates corrections distinguishing served discovery, wall-neutral results, and later owner-count usability changes.

Subjects: “plate: Astra's copy correction — served discovery in cohort J was wall-neutral; owner_counts is a later usability change with no measured additional wall gain” · “plate: fold peer-reviewer corrections into the fan-out route section” · “plate: fan-out route as experimental harness default + required-section ratchet”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-06-astra-fanout-final-and-existing-route.md`; `docs/observations/fanout-scorer-round5-review-sol.md`.

Recorded result (scope and candidate as quoted), `docs/observations/2026-09-06-fanout-J-result.md:15`:

> Verdict against the preregistration: FALSIFIED. J median is not below 43.4 s (it is ≈ I), and 0/4 are correct under the witness. Every J arm made exactly the two mandated Surgeon calls (inspect match batch + apply), no refusals recorded in the result rows (rollout-level counts in the ethnography when filed).
> Correctness note: all four failed the same witness predicate, variadic-forwarder?, because the helper body reads (apply next.jdbc/execute! args) — fully qualified — where the witness requires the alias spelling (apply jdbc/execute! args). Functionally equivalent; the preregistered correctness is the witness, so J is incorrect as registered, and the witness's spelling sensitivity is the SAME layout/spelling false-negative class Astra named for N3. Why all four J actors chose the qualified spelling while all four I actors chose the alias is the ethnography's first question.
> Wall note: served discovery did not remove the compose sink. Where the time went instead is the ethnography's second question. Until it answers, the honest reading is: the 19–45 s the I arms spent composing was not the marginal cost of composing — it was the actors' cost of understanding the task, which a served list did not shorten.
> Retained losses cited: cohort B (deterministic route, 2.8x loss), this cohort.

Recorded result (scope and candidate as quoted), `docs/observations/2026-09-06-fanout-J-result.md:16`:

> Correctness note: all four failed the same witness predicate, variadic-forwarder?, because the helper body reads (apply next.jdbc/execute! args) — fully qualified — where the witness requires the alias spelling (apply jdbc/execute! args). Functionally equivalent; the preregistered correctness is the witness, so J is incorrect as registered, and the witness's spelling sensitivity is the SAME layout/spelling false-negative class Astra named for N3. Why all four J actors chose the qualified spelling while all four I actors chose the alias is the ethnography's first question.
> Wall note: served discovery did not remove the compose sink. Where the time went instead is the ethnography's second question. Until it answers, the honest reading is: the 19–45 s the I arms spent composing was not the marginal cost of composing — it was the actors' cost of understanding the task, which a served list did not shorten.
> Retained losses cited: cohort B (deterministic route, 2.8x loss), this cohort.

**Fan-out locator.**

| Role | Path at this tag | Lines (`wc -l`) |
|---|---|---:|
| Fixture generator | `bench/fanout/gen-fanout.clj` | 358 |
| Acceptance scorer | `bench/fanout/fan_check.clj` | 567 |
| Rescore script | `bench/fanout/rescore-FAN.sh` | 129 |
| Adversarial/sabotage harness | `bench/fanout/sabotage-FAN.sh` | 1319 |
| Routing plate | `resources/clj-surgeon-agent-routing.md` | 90 |
| Routing observation | `docs/observations/2026-09-06-clojure-edit-routing-rule.md` | 20 |

Checkout: `git fetch origin tag archive/fable/fanout-route-plate && git worktree add --detach <dir> archive/fable/fanout-route-plate`

### archive/fable/forms-comments

Branch: `fable/forms-comments`. Tip: `f04298cc` · 2026-09-06T03:54:05Z. Commits not on main: **1323**.

Preserves comments by guarded expression identity and structural attachment, accepting reindentation while refusing moved or rewritten protected syntax.

Subjects: “forms: per Astra's 03:35Z ruling — exact source identity as a rewrite-clj node fingerprint (trivia nodes dropped, tags and literal bytes kept, never regex-collapsed; re-indentation accepted, spaces inside string literals stay distinct); :comment-follows-rewrite removed entirely (strict default; refusal names the reviewed native edit); :owner-form sentinel documented as an owner-identity boundary with the one-owner guard witnessed” · “forms: comment attachment anchored on the guarded EXPRESSION'S identity (text + side + depth-path), not its ordinal — Astra's swapped-expression counterexample refuses :forms-comment-moved with :from/:to expressions; inserting before a commented expression is accepted again; identical expressions tie-break by order; a rewritten guarded expression refuses unless the mission opts in (:comment-follows-rewrite); :owner-form sentinel at the span's top level (the owner definition is replaced by design and guarded by :forms-owner-mismatch)” · “forms: comment preservation compares text AND structural attachment path; :forms-comment-moved typed refusal with :moved [{:comment :from :to}]; positional carry (:carry-comments) removed this round per Astra's contract; lint-directive comments follow the attachment rule, #_{:clj-kondo/ignore} stays protected-syntax”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-06-astra-forms-cohort-result.md`; `docs/observations/2026-09-06-astra-forms-cohort-prereg.md`.

Shared-history battery result (the row names its measured candidate, not a verdict on this branch’s own changes), `docs/observations/battery-ledger.edn:13`:

> {:sha "9e67856aa496e5f8f07f23522f7d8eea652078dc", :started "2026-09-05T14:36:53Z", :wall_s 717, :verdict :pass, :host "anvil-server"}

Checkout: `git fetch origin tag archive/fable/forms-comments && git worktree add --detach <dir> archive/fable/forms-comments`

### archive/fable/hot-verify-done

Branch: `fable/hot-verify-done`. Tip: `10f5131d` · 2026-09-06T22:53:10Z. Commits not on main: **2012**.

Ends an nREPL verification read at the first terminal status for its own request, under one deadline, with typed timeout, transport, and interruption behavior.

Subjects: “merge: origin/MCP/main (7030bb56) into fable/hot-verify-done” · “Merge remote-tracking branch 'origin/MCP/main' into HEAD” · “log: landing the ceiling receipt 5c7c9e79”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-06-proto-verify-in-call.md`; `docs/observations/2026-09-06-sol-fence-public-result-ceiling-r3.md`.

Shared-history battery result (the row names its measured candidate, not a verdict on this branch’s own changes), `docs/observations/battery-ledger.edn:27`:

> {:sha "e6c431a1980d6ad9eddd3e6f5ef8b2e3bc7e58db", :started "2026-09-06T22:32:13Z", :wall_s 786, :verdict :pass, :host "anvil-server"}

**Change-buffer and hot-verification locator.**

| Role | Path at this tag | Lines (`wc -l`) |
|---|---|---:|
| Proof-carrying buffer design | `docs/plans/proof-carrying-change-buffer.md` | 299 |
| Change-buffer implementation | `src/clj_surgeon/mcp_change_buffer.clj` | 1749 |
| Change-buffer tests | `test/clj_surgeon/mcp_change_buffer_test.clj` | 1140 |
| Hot-verification implementation | `src/clj_surgeon/mcp_hot_verify.clj` | 198 |
| Hot-verification tests | `test/clj_surgeon/mcp_hot_verify_test.clj` | 292 |

The read-only search `git log --all --regexp-ignore-case --grep="change buffer" --format="%h %cs %s"` finds `d4a303b5` (2026-08-07), “docs: specify proof-carrying change buffer”, which originally added the 768-line plan. The 299-line plan above is this tag’s later version. The same search also finds five commits whose bodies mention the phrase; those are memory/alias work, not five additional buffer implementations. Path history identifies `21a0747d` (2026-09-06), “green: end the hot-verification read at a terminal status (inb-adcc9e)”, followed by interruption/diagnostic and refusal-kind repairs.

Checkout: `git fetch origin tag archive/fable/hot-verify-done && git worktree add --detach <dir> archive/fable/hot-verify-done`

### archive/fable/hot-verify-done-receipt

Branch: `fable/hot-verify-done-receipt`. Tip: `3c2e023c` · 2026-09-06T23:09:20Z. Commits not on main: **2015**.

Records receipt for fable/hot-verify-done merged tip onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for fable/hot-verify-done merged tip onto trunk (landing gate battery-fresh)” · “Merge commit '10f5131d' into fable/hot-verify-done-receipt” · “merge: origin/MCP/main (7030bb56) into fable/hot-verify-done”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-06-proto-verify-in-call.md`; `docs/observations/2026-09-06-sol-fence-public-result-ceiling-r3.md`.

Latest retained battery row (its SHA is the measured candidate; no claim that it verifies this tag tip), `docs/observations/battery-ledger.edn:28`:

> {:sha "35ad3b3fd611b89c3a6f6fc782e589b3c4e909d9", :started "2026-09-06T22:56:13Z", :wall_s 787, :verdict :pass, :host "anvil-server"}

**Change-buffer and hot-verification locator.**

| Role | Path at this tag | Lines (`wc -l`) |
|---|---|---:|
| Proof-carrying buffer design | `docs/plans/proof-carrying-change-buffer.md` | 299 |
| Change-buffer implementation | `src/clj_surgeon/mcp_change_buffer.clj` | 1749 |
| Change-buffer tests | `test/clj_surgeon/mcp_change_buffer_test.clj` | 1140 |
| Hot-verification implementation | `src/clj_surgeon/mcp_hot_verify.clj` | 198 |
| Hot-verification tests | `test/clj_surgeon/mcp_hot_verify_test.clj` | 292 |

The read-only search `git log --all --regexp-ignore-case --grep="change buffer" --format="%h %cs %s"` finds `d4a303b5` (2026-08-07), “docs: specify proof-carrying change buffer”, which originally added the 768-line plan. The 299-line plan above is this tag’s later version. The same search also finds five commits whose bodies mention the phrase; those are memory/alias work, not five additional buffer implementations. Path history identifies `21a0747d` (2026-09-06), “green: end the hot-verification read at a terminal status (inb-adcc9e)”, followed by interruption/diagnostic and refusal-kind repairs.

Checkout: `git fetch origin tag archive/fable/hot-verify-done-receipt && git worktree add --detach <dir> archive/fable/hot-verify-done-receipt`

### archive/fable/impact-entrance

Branch: `fable/impact-entrance`. Tip: `ab3febad` · 2026-09-14T10:20:09Z. Commits not on main: **2885**.

Builds diff-impact selection with source-content and test-helper dependencies and holds gates whose changes cannot be matched safely.

Subjects: “fix: hold unmatched diff-impact gates and propagate source content edges” · “test: strengthen diff-impact fail-closed contract and commit Sol F1 red” · “fix: include file-content and test-helper edges in diff-impact selection”.

Added observation/plan documents (merge-base comparison): `docs/plans/diff-impact-edges.md`; `docs/observations/2026-09-14-diff-impact-edges-round2/sol-verdict-1.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:80`:

> {:sha "1eb8c4012a5eefeaa5bfbf6a423aca85c8d7ce45", :started "2026-09-14T06:32:44Z", :wall_s 204, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/impact-entrance && git worktree add --detach <dir> archive/fable/impact-entrance`

### archive/fable/impact-executor

Branch: `fable/impact-executor`. Tip: `e19ce755` · 2026-09-15T08:57:05Z. Commits not on main: **2899**.

Executes diff-impact selections through prewarm and installed envelopes, refusing a nonempty selection that would execute no fast tests.

Subjects: “fix: refuse nonempty selections with no fast members” · “test: reject empty fast execution for nonempty selections” · “docs: record round 3 prewarm and installed envelope acceptance”.

Added observation/plan documents (merge-base comparison): `docs/plans/diff-impact-executor.md`; `docs/observations/2026-09-15-diff-impact-executor-report-astra.md`.

Shared-history battery result (the row names its measured candidate, not a verdict on this branch’s own changes), `docs/observations/battery-ledger.edn:81`:

> {:sha "d033498227b3733874207cfae1063bd4bb0d848f", :started "2026-09-14T11:23:05Z", :wall_s 198, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/impact-executor && git worktree add --detach <dir> archive/fable/impact-executor`

### archive/fable/insert-forms

Shares its tip commit and its prose with `archive/fable/insert-forms-r1` (two tags, one branch state).

Branch: `fable/insert-forms`. Tip: `29f81c5f` · 2026-09-10T23:10:14Z. Commits not on main: **2661**.

Repairs insert_forms comment tails and inline-body indentation, retaining all six fix-round regression witnesses.

Subjects: “docs(insert-forms): record fix-round intent and verification contract” · “fix(insert-forms): preserve comment tails and inline body indentation” · “test(insert-forms): enroll all six fix-round regressions”.

Added observation/plan documents (merge-base comparison): `docs/plans/insert-forms-v1.md`; `docs/observations/2026-09-06-astra-forms-cohort-result.md`.

Shared-history battery result (the row names its measured candidate, not a verdict on this branch’s own changes), `docs/observations/battery-ledger.edn:59`:

> {:sha "1cd04b0e28b699326ec402610c6ac0515885bfac", :started "2026-09-10T20:29:30Z", :wall_s 147, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/insert-forms && git worktree add --detach <dir> archive/fable/insert-forms`

### archive/fable/insert-forms-r1

Shares its tip commit and its prose with `archive/fable/insert-forms` (two tags, one branch state).

Branch: `fable/insert-forms-r1`. Tip: `29f81c5f` · 2026-09-10T23:10:14Z. Commits not on main: **2661**.

Preserves the same insert_forms fix round for comment tails, inline indentation, and its six regression witnesses.

Subjects: “docs(insert-forms): record fix-round intent and verification contract” · “fix(insert-forms): preserve comment tails and inline body indentation” · “test(insert-forms): enroll all six fix-round regressions”.

Added observation/plan documents (merge-base comparison): `docs/plans/insert-forms-v1.md`; `docs/observations/2026-09-06-astra-forms-cohort-result.md`.

Shared-history battery result (the row names its measured candidate, not a verdict on this branch’s own changes), `docs/observations/battery-ledger.edn:59`:

> {:sha "1cd04b0e28b699326ec402610c6ac0515885bfac", :started "2026-09-10T20:29:30Z", :wall_s 147, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/insert-forms-r1 && git worktree add --detach <dir> archive/fable/insert-forms-r1`

### archive/fable/insert-forms-r2

Branch: `fable/insert-forms-r2`. Tip: `7ad64b6b` · 2026-09-10T23:30:21Z. Commits not on main: **2664**.

Lands insert_forms as a shared MCP and CLI request-file verb and updates the admission catalog with battery evidence.

Subjects: “battery: receipt for 23cd511c1fa924c731f7833db1e2c36650ba1ee3 merged onto trunk (landing gate battery-fresh)” · “merge: insert_forms v1: one verb for MCP and the CLI request-file entrance (programs to contracts)” · “admit-patch test: the full-profile catalog now carries insert_forms after namespace_split”.

Added observation/plan documents (merge-base comparison): `docs/plans/insert-forms-v1.md`; `docs/observations/2026-09-06-astra-forms-cohort-result.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:60`:

> {:sha "23cd511c1fa924c731f7833db1e2c36650ba1ee3", :started "2026-09-10T23:27:19Z", :wall_s 182, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/insert-forms-r2 && git worktree add --detach <dir> archive/fable/insert-forms-r2`

### archive/fable/match-owner-counts

Branch: `fable/match-owner-counts`. Tip: `d6c0d035` · 2026-09-06T18:42:13Z. Commits not on main: **1884**.

Removes derivable per-site source echoes from match results and repairs the corpus pin and field-contract overclaims.

Subjects: “Repair the corpus pin and correct two overclaims in MCP-OP-FIELD-008” · “GREEN: omit the derivable per-site source echo (MCP-OP-FIELD-008)” · “RED: the derivable per-site source echo, and the fence it pushes callers past”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-06-wiring-change.md`; `docs/observations/2026-09-06-two-hour-trial-closeout.md`.

Shared-history battery result (the row names its measured candidate, not a verdict on this branch’s own changes), `docs/observations/battery-ledger.edn:23`:

> {:sha "72e47aeb67ad469e002276bf0c6a3ccdafb0a228", :started "2026-09-06T15:14:49Z", :wall_s 809, :verdict :pass, :host "anvil-server"}

Checkout: `git fetch origin tag archive/fable/match-owner-counts && git worktree add --detach <dir> archive/fable/match-owner-counts`

### archive/fable/match-owner-counts-receipt

Branch: `fable/match-owner-counts-receipt`. Tip: `1472105d` · 2026-09-06T18:56:54Z. Commits not on main: **1897**.

Records receipt for fable/match-owner-counts tip merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for fable/match-owner-counts tip merged onto trunk (landing gate battery-fresh)” · “Merge commit 'd6c0d035ac4b18839531eda55677cbc96176bf99' into fable/match-owner-counts-receipt” · “Repair the corpus pin and correct two overclaims in MCP-OP-FIELD-008”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-06-sol-fence-match-owner-counts-r2.md`; `docs/observations/2026-09-06-sol-fence-match-owner-counts-r1.md`.

Latest retained battery row (its SHA is the measured candidate; no claim that it verifies this tag tip), `docs/observations/battery-ledger.edn:24`:

> {:sha "a60b0a9121913551d48bc81b886e837adf2629c2", :started "2026-09-06T18:42:35Z", :wall_s 859, :verdict :pass, :host "anvil-server"}

Checkout: `git fetch origin tag archive/fable/match-owner-counts-receipt && git worktree add --detach <dir> archive/fable/match-owner-counts-receipt`

### archive/fable/parity-harness

Branch: `fable/parity-harness`. Tip: `b204521f` · 2026-09-10T18:09:28Z. Commits not on main: **2572**.

Builds a parity harness whose rules explain their evidence through replay, commit-bound provenance, retained captures, and a sealed child PATH.

Subjects: “parity: commit-bound evidence for the nine surviving rules, and a child PATH with no launcher on it” · “parity: a rule must EXPLAIN its own evidence — replay, commit-bound provenance, sealed child PATH” · “parity: the bytes are the evidence — retain the capture, re-derive the observation”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-08-battery-parallel-verdict-parity.md`; `docs/plans/parallel-landing-gate.md`.

Shared-history battery result (the row names its measured candidate, not a verdict on this branch’s own changes), `docs/observations/battery-ledger.edn:57`:

> {:sha "6867f773be0f894f2bef0c145f3e07101365919f", :started "2026-09-10T04:39:26Z", :wall_s 149, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/parity-harness && git worktree add --detach <dir> archive/fable/parity-harness`

### archive/fable/parity-harness-receipt-0d58fda2

Branch: `fable/parity-harness-receipt-0d58fda2`. Tip: `62dd247c` · 2026-09-10T17:39:28Z. Commits not on main: **2584**.

Records receipt for 9d3fd5255e6d54132ec6be1104347564e15fd992 merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 9d3fd5255e6d54132ec6be1104347564e15fd992 merged onto trunk (landing gate battery-fresh)” · “merge: merge: byte-parity harness — routed ops on retained specimens, build A vs build B, same inputs same bytes (bin/parity)” · “parity: the bytes are the evidence — retain the capture, re-derive the observation”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-08-battery-parallel-verdict-parity.md`; `docs/plans/parallel-landing-gate.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:59`:

> {:sha "9d3fd5255e6d54132ec6be1104347564e15fd992", :started "2026-09-10T17:36:57Z", :wall_s 151, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/parity-harness-receipt-0d58fda2 && git worktree add --detach <dir> archive/fable/parity-harness-receipt-0d58fda2`

### archive/fable/parity-harness-receipt-4b594926

Branch: `fable/parity-harness-receipt-4b594926`. Tip: `f189e20d` · 2026-09-10T17:14:27Z. Commits not on main: **2571**.

Records receipt for a094bb6817fee6dc03b9ba9de40034455605d57f merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for a094bb6817fee6dc03b9ba9de40034455605d57f merged onto trunk (landing gate battery-fresh)” · “merge: merge: byte-parity harness — routed ops on retained specimens, build A vs build B, same inputs same bytes (bin/parity)” · “parity: close Sol's two fence findings — a rule must be paid for, and absence is a value”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-08-battery-parallel-verdict-parity.md`; `docs/plans/parallel-landing-gate.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:58`:

> {:sha "a094bb6817fee6dc03b9ba9de40034455605d57f", :started "2026-09-10T17:11:54Z", :wall_s 153, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/parity-harness-receipt-4b594926 && git worktree add --detach <dir> archive/fable/parity-harness-receipt-4b594926`

### archive/fable/parity-harness-receipt-b204521f

Branch: `fable/parity-harness-receipt-b204521f`. Tip: `209d9aaf` · 2026-09-10T18:13:31Z. Commits not on main: **2586**.

Records receipt for ae0c16937c9ab4e96e8bd0ba9d2fca7eba2a9f44 merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for ae0c16937c9ab4e96e8bd0ba9d2fca7eba2a9f44 merged onto trunk (landing gate battery-fresh)” · “merge: merge: byte-parity harness — routed ops on retained specimens, build A vs build B, same inputs same bytes (bin/parity)” · “parity: commit-bound evidence for the nine surviving rules, and a child PATH with no launcher on it”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-08-battery-parallel-verdict-parity.md`; `docs/plans/parallel-landing-gate.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:59`:

> {:sha "ae0c16937c9ab4e96e8bd0ba9d2fca7eba2a9f44", :started "2026-09-10T18:10:50Z", :wall_s 161, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/parity-harness-receipt-b204521f && git worktree add --detach <dir> archive/fable/parity-harness-receipt-b204521f`

### archive/fable/parity-harness-receipt-d1343a87

Branch: `fable/parity-harness-receipt-d1343a87`. Tip: `3985d09e` · 2026-09-10T16:39:50Z. Commits not on main: **2570**.

Records receipt for cb2f7ad10c0d62b313873eb3ded0753053d075ed merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for cb2f7ad10c0d62b313873eb3ded0753053d075ed merged onto trunk (landing gate battery-fresh)” · “merge: merge: byte-parity harness — routed ops on retained specimens, build A vs build B, same inputs same bytes (bin/parity)” · “parity: resolve a build ref to its COMMIT, not its tag object”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-08-battery-parallel-verdict-parity.md`; `docs/plans/parallel-landing-gate.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:58`:

> {:sha "cb2f7ad10c0d62b313873eb3ded0753053d075ed", :started "2026-09-10T16:35:45Z", :wall_s 245, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/parity-harness-receipt-d1343a87 && git worktree add --detach <dir> archive/fable/parity-harness-receipt-d1343a87`

### archive/fable/proof-burden

Branch: `fable/proof-burden`. Tip: `10b77576` · 2026-09-10T01:33:59Z. Commits not on main: **2573**.

Builds and repeatedly hardens the proof-burden checks, replacing source inspection with execution and requiring sequence-sensitive row oracles.

Subjects: “item 3, Sol fence round 16 (PB-FENCE-023): the row oracle compares sequences, not sets” · “item 3, Sol fence round 15 (PB-FENCE-022): the probe exercises the PIPELINE, not a function” · “item 3, Sol fence round 14 (PB-FENCE-021): R8 stops reading the source and starts running it”.

Added observation/plan documents (merge-base comparison): `docs/plans/2026-09-06-astra-patch-proof-usage.md`; `docs/observations/2026-09-06-proto-warm-proof.md`.

Shared-history battery result (the row names its measured candidate, not a verdict on this branch’s own changes), `docs/observations/battery-ledger.edn:56`:

> {:sha "a51dd39e955972ddac1d417d40e73e05f30637c1", :started "2026-09-09T08:06:00Z", :wall_s 149, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/proof-burden && git worktree add --detach <dir> archive/fable/proof-burden`

### archive/fable/proof-burden-receipt-04110f80

Branch: `fable/proof-burden-receipt-04110f80`. Tip: `13cc428b` · 2026-09-10T00:35:36Z. Commits not on main: **2572**.

Records receipt for e8b25243e0c3aef7f8d08019ecce8a067a16eca8 merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for e8b25243e0c3aef7f8d08019ecce8a067a16eca8 merged onto trunk (landing gate battery-fresh)” · “merge: merge: preservation brief — independently re-derived mechanical-preservation evidence for the reviewer (item 3 prototype)” · “item 3, Sol fence round 13 (PB-FENCE-020): the refusal table IS the dispatch”.

Added observation/plan documents (merge-base comparison): `docs/plans/2026-09-06-astra-patch-proof-usage.md`; `docs/observations/2026-09-06-proto-warm-proof.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:57`:

> {:sha "e8b25243e0c3aef7f8d08019ecce8a067a16eca8", :started "2026-09-10T00:33:08Z", :wall_s 148, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/proof-burden-receipt-04110f80 && git worktree add --detach <dir> archive/fable/proof-burden-receipt-04110f80`

### archive/fable/proof-burden-receipt-10b77576

Branch: `fable/proof-burden-receipt-10b77576`. Tip: `bf58ce21` · 2026-09-10T01:49:53Z. Commits not on main: **2575**.

Records receipt for 3beee531f8301633ca9d67b8a2238cf3041cf959 merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 3beee531f8301633ca9d67b8a2238cf3041cf959 merged onto trunk (landing gate battery-fresh)” · “merge: merge: preservation brief — independently re-derived mechanical-preservation evidence for the reviewer (item 3 prototype)” · “item 3, Sol fence round 16 (PB-FENCE-023): the row oracle compares sequences, not sets”.

Added observation/plan documents (merge-base comparison): `docs/plans/2026-09-06-astra-patch-proof-usage.md`; `docs/observations/2026-09-06-proto-warm-proof.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:57`:

> {:sha "3beee531f8301633ca9d67b8a2238cf3041cf959", :started "2026-09-10T01:47:24Z", :wall_s 149, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/proof-burden-receipt-10b77576 && git worktree add --detach <dir> archive/fable/proof-burden-receipt-10b77576`

### archive/fable/proof-burden-receipt-1cb98b5e

Branch: `fable/proof-burden-receipt-1cb98b5e`. Tip: `4a7f7acd` · 2026-09-09T17:14:26Z. Commits not on main: **2559**.

Records receipt for 247592684da908837233c6f9fd6db54f9f954588 merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 247592684da908837233c6f9fd6db54f9f954588 merged onto trunk (landing gate battery-fresh)” · “merge: merge: preservation brief — independently re-derived mechanical-preservation evidence for the reviewer (item 3 prototype)” · “item 3, Sol fence round 2 (PB-FENCE-005): the denylist is inverted to an allowlist”.

Added observation/plan documents (merge-base comparison): `docs/plans/2026-09-06-astra-patch-proof-usage.md`; `docs/observations/2026-09-06-proto-warm-proof.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:57`:

> {:sha "247592684da908837233c6f9fd6db54f9f954588", :started "2026-09-09T17:11:49Z", :wall_s 157, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/proof-burden-receipt-1cb98b5e && git worktree add --detach <dir> archive/fable/proof-burden-receipt-1cb98b5e`

### archive/fable/proof-burden-receipt-1e00e17c

Branch: `fable/proof-burden-receipt-1e00e17c`. Tip: `235ebad1` · 2026-09-09T16:06:16Z. Commits not on main: **2557**.

Records receipt for e8c36afbe00b98cb149f72c052740dcf869f9fe8 merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for e8c36afbe00b98cb149f72c052740dcf869f9fe8 merged onto trunk (landing gate battery-fresh)” · “merge: merge: preservation brief — independently re-derived mechanical-preservation evidence for the reviewer (item 3 prototype)” · “item 3: two-stage scoping, the require-sort and undefined-alias checks, and a receipt cross-examination that refuses an unbound candidate”.

Added observation/plan documents (merge-base comparison): `docs/plans/2026-09-06-astra-patch-proof-usage.md`; `docs/observations/2026-09-06-proto-warm-proof.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:57`:

> {:sha "e8c36afbe00b98cb149f72c052740dcf869f9fe8", :started "2026-09-09T16:03:12Z", :wall_s 184, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/proof-burden-receipt-1e00e17c && git worktree add --detach <dir> archive/fable/proof-burden-receipt-1e00e17c`

### archive/fable/proof-burden-receipt-33e65cef

Branch: `fable/proof-burden-receipt-33e65cef`. Tip: `5d08a3be` · 2026-09-09T19:54:06Z. Commits not on main: **2563**.

Records receipt for 03b51d2bedd3f67db66397a29de4170990bf5444 merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 03b51d2bedd3f67db66397a29de4170990bf5444 merged onto trunk (landing gate battery-fresh)” · “merge: merge: preservation brief — independently re-derived mechanical-preservation evidence for the reviewer (item 3 prototype)” · “item 3, Sol fence round 6 (PB-FENCE-011): the resolver's :var-definitions ARE the owner inventory”.

Added observation/plan documents (merge-base comparison): `docs/plans/2026-09-06-astra-patch-proof-usage.md`; `docs/observations/2026-09-06-proto-warm-proof.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:57`:

> {:sha "03b51d2bedd3f67db66397a29de4170990bf5444", :started "2026-09-09T19:51:27Z", :wall_s 159, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/proof-burden-receipt-33e65cef && git worktree add --detach <dir> archive/fable/proof-burden-receipt-33e65cef`

### archive/fable/proof-burden-receipt-470cea34

Branch: `fable/proof-burden-receipt-470cea34`. Tip: `8c91e006` · 2026-09-09T19:19:19Z. Commits not on main: **2562**.

Records receipt for 4089ddd2f015971f50ea3767d6eb9f0a6f18f058 merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 4089ddd2f015971f50ea3767d6eb9f0a6f18f058 merged onto trunk (landing gate battery-fresh)” · “merge: merge: preservation brief — independently re-derived mechanical-preservation evidence for the reviewer (item 3 prototype)” · “item 3, Sol fence round 5 (PB-FENCE-010): stop hand-rolling name resolution — clj-kondo is the resolver”.

Added observation/plan documents (merge-base comparison): `docs/plans/2026-09-06-astra-patch-proof-usage.md`; `docs/observations/2026-09-06-proto-warm-proof.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:57`:

> {:sha "4089ddd2f015971f50ea3767d6eb9f0a6f18f058", :started "2026-09-09T19:16:51Z", :wall_s 148, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/proof-burden-receipt-470cea34 && git worktree add --detach <dir> archive/fable/proof-burden-receipt-470cea34`

### archive/fable/proof-burden-receipt-55374a28

Branch: `fable/proof-burden-receipt-55374a28`. Tip: `4fb79829` · 2026-09-09T23:29:53Z. Commits not on main: **2570**.

Records receipt for ed5708d79666cf65885b486843ee0645833b7a5f merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for ed5708d79666cf65885b486843ee0645833b7a5f merged onto trunk (landing gate battery-fresh)” · “merge: merge: preservation brief — independently re-derived mechanical-preservation evidence for the reviewer (item 3 prototype)” · “item 3, Sol fence round 11 (PB-FENCE-017 residue): a refused file is refused WHOLE”.

Added observation/plan documents (merge-base comparison): `docs/plans/2026-09-06-astra-patch-proof-usage.md`; `docs/observations/2026-09-06-proto-warm-proof.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:57`:

> {:sha "ed5708d79666cf65885b486843ee0645833b7a5f", :started "2026-09-09T23:27:25Z", :wall_s 148, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/proof-burden-receipt-55374a28 && git worktree add --detach <dir> archive/fable/proof-burden-receipt-55374a28`

### archive/fable/proof-burden-receipt-5ec9edc5

Branch: `fable/proof-burden-receipt-5ec9edc5`. Tip: `ee05700f` · 2026-09-09T20:45:53Z. Commits not on main: **2564**.

Records receipt for 65cb317de454b3315d6b727b6c9781c77aea85d7 merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 65cb317de454b3315d6b727b6c9781c77aea85d7 merged onto trunk (landing gate battery-fresh)” · “merge: merge: preservation brief — independently re-derived mechanical-preservation evidence for the reviewer (item 3 prototype)” · “item 3, Sol fence round 7 (PB-FENCE-012): the checker's certification scope is exactly the analyzer's”.

Added observation/plan documents (merge-base comparison): `docs/plans/2026-09-06-astra-patch-proof-usage.md`; `docs/observations/2026-09-06-proto-warm-proof.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:57`:

> {:sha "65cb317de454b3315d6b727b6c9781c77aea85d7", :started "2026-09-09T20:43:23Z", :wall_s 150, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/proof-burden-receipt-5ec9edc5 && git worktree add --detach <dir> archive/fable/proof-burden-receipt-5ec9edc5`

### archive/fable/proof-burden-receipt-77126c8d

Branch: `fable/proof-burden-receipt-77126c8d`. Tip: `3c740c71` · 2026-09-09T17:57:59Z. Commits not on main: **2560**.

Records receipt for 50bcd107919fbf1b3d5810f234f005df51a3bc7c merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 50bcd107919fbf1b3d5810f234f005df51a3bc7c merged onto trunk (landing gate battery-fresh)” · “merge: merge: preservation brief — independently re-derived mechanical-preservation evidence for the reviewer (item 3 prototype)” · “item 3, Sol fence round 3: a table entry is a soundness claim, so every claim gets a falsifier”.

Added observation/plan documents (merge-base comparison): `docs/plans/2026-09-06-astra-patch-proof-usage.md`; `docs/observations/2026-09-06-proto-warm-proof.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:57`:

> {:sha "50bcd107919fbf1b3d5810f234f005df51a3bc7c", :started "2026-09-09T17:55:25Z", :wall_s 154, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/proof-burden-receipt-77126c8d && git worktree add --detach <dir> archive/fable/proof-burden-receipt-77126c8d`

### archive/fable/proof-burden-receipt-77ee1e41

Branch: `fable/proof-burden-receipt-77ee1e41`. Tip: `40be4b8d` · 2026-09-10T00:02:32Z. Commits not on main: **2571**.

Records receipt for c614ed95e8b8d6bc85a89125b8a20def64012081 merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for c614ed95e8b8d6bc85a89125b8a20def64012081 merged onto trunk (landing gate battery-fresh)” · “merge: merge: preservation brief — independently re-derived mechanical-preservation evidence for the reviewer (item 3 prototype)” · “item 3, Sol fence round 12 (PB-FENCE-019): the refusal kinds are one enumeration, and R8 iterates it”.

Added observation/plan documents (merge-base comparison): `docs/plans/2026-09-06-astra-patch-proof-usage.md`; `docs/observations/2026-09-06-proto-warm-proof.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:57`:

> {:sha "c614ed95e8b8d6bc85a89125b8a20def64012081", :started "2026-09-10T00:00:04Z", :wall_s 148, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/proof-burden-receipt-77ee1e41 && git worktree add --detach <dir> archive/fable/proof-burden-receipt-77ee1e41`

### archive/fable/proof-burden-receipt-83c13814

Branch: `fable/proof-burden-receipt-83c13814`. Tip: `51e511a7` · 2026-09-09T18:24:48Z. Commits not on main: **2561**.

Records receipt for 45fad0cd0f8b8d202e5919b64a1e11ae0e33737f merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 45fad0cd0f8b8d202e5919b64a1e11ae0e33737f merged onto trunk (landing gate battery-fresh)” · “merge: merge: preservation brief — independently re-derived mechanical-preservation evidence for the reviewer (item 3 prototype)” · “item 3, Sol fence round 4 (PB-FENCE-009): spelling is not identity — resolution precedes every table”.

Added observation/plan documents (merge-base comparison): `docs/plans/2026-09-06-astra-patch-proof-usage.md`; `docs/observations/2026-09-06-proto-warm-proof.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:57`:

> {:sha "45fad0cd0f8b8d202e5919b64a1e11ae0e33737f", :started "2026-09-09T18:20:54Z", :wall_s 234, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/proof-burden-receipt-83c13814 && git worktree add --detach <dir> archive/fable/proof-burden-receipt-83c13814`

### archive/fable/proof-burden-receipt-8751ed9e

Branch: `fable/proof-burden-receipt-8751ed9e`. Tip: `62a3c48f` · 2026-09-09T16:36:36Z. Commits not on main: **2558**.

Records receipt for a11f465504bf887ae89a483b3f200dfa10520133 merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for a11f465504bf887ae89a483b3f200dfa10520133 merged onto trunk (landing gate battery-fresh)” · “merge: merge: preservation brief — independently re-derived mechanical-preservation evidence for the reviewer (item 3 prototype)” · “item 3, Sol fence NO-GO on 1e00e17c: all four findings repaired”.

Added observation/plan documents (merge-base comparison): `docs/plans/2026-09-06-astra-patch-proof-usage.md`; `docs/observations/2026-09-06-proto-warm-proof.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:57`:

> {:sha "a11f465504bf887ae89a483b3f200dfa10520133", :started "2026-09-09T16:33:57Z", :wall_s 159, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/proof-burden-receipt-8751ed9e && git worktree add --detach <dir> archive/fable/proof-burden-receipt-8751ed9e`

### archive/fable/proof-burden-receipt-95b0ff3f

Branch: `fable/proof-burden-receipt-95b0ff3f`. Tip: `a24486d0` · 2026-09-09T22:59:32Z. Commits not on main: **2569**.

Records receipt for 45e0d099775e7966f97e92c58179b70d5eba1900 merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 45e0d099775e7966f97e92c58179b70d5eba1900 merged onto trunk (landing gate battery-fresh)” · “merge: merge: preservation brief — independently re-derived mechanical-preservation evidence for the reviewer (item 3 prototype)” · “item 3: a .cljc namespace is reported once per platform — count distinct namespaces, not definitions”.

Added observation/plan documents (merge-base comparison): `docs/plans/2026-09-06-astra-patch-proof-usage.md`; `docs/observations/2026-09-06-proto-warm-proof.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:57`:

> {:sha "45e0d099775e7966f97e92c58179b70d5eba1900", :started "2026-09-09T22:57:02Z", :wall_s 149, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/proof-burden-receipt-95b0ff3f && git worktree add --detach <dir> archive/fable/proof-burden-receipt-95b0ff3f`

### archive/fable/proof-burden-receipt-a8ec1923

Branch: `fable/proof-burden-receipt-a8ec1923`. Tip: `a68681f8` · 2026-09-09T21:44:07Z. Commits not on main: **2566**.

Records receipt for e086997c95e77b4abeb365fee5b64df6012a1f42 merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for e086997c95e77b4abeb365fee5b64df6012a1f42 merged onto trunk (landing gate battery-fresh)” · “merge: merge: preservation brief — independently re-derived mechanical-preservation evidence for the reviewer (item 3 prototype)” · “item 3: land the round-8 replay rows (the block had not been inserted)”.

Added observation/plan documents (merge-base comparison): `docs/plans/2026-09-06-astra-patch-proof-usage.md`; `docs/observations/2026-09-06-proto-warm-proof.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:57`:

> {:sha "e086997c95e77b4abeb365fee5b64df6012a1f42", :started "2026-09-09T21:41:36Z", :wall_s 151, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/proof-burden-receipt-a8ec1923 && git worktree add --detach <dir> archive/fable/proof-burden-receipt-a8ec1923`

### archive/fable/proof-burden-receipt-b30aa9e7

Branch: `fable/proof-burden-receipt-b30aa9e7`. Tip: `62bd92a3` · 2026-09-10T01:02:41Z. Commits not on main: **2573**.

Records receipt for 7c24e0203b542672ec87e867a7fe7b39cbc64218 merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 7c24e0203b542672ec87e867a7fe7b39cbc64218 merged onto trunk (landing gate battery-fresh)” · “merge: merge: preservation brief — independently re-derived mechanical-preservation evidence for the reviewer (item 3 prototype)” · “item 3, Sol fence round 14 (PB-FENCE-021): R8 stops reading the source and starts running it”.

Added observation/plan documents (merge-base comparison): `docs/plans/2026-09-06-astra-patch-proof-usage.md`; `docs/observations/2026-09-06-proto-warm-proof.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:57`:

> {:sha "7c24e0203b542672ec87e867a7fe7b39cbc64218", :started "2026-09-10T01:00:11Z", :wall_s 150, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/proof-burden-receipt-b30aa9e7 && git worktree add --detach <dir> archive/fable/proof-burden-receipt-b30aa9e7`

### archive/fable/proof-burden-receipt-de83b75d

Branch: `fable/proof-burden-receipt-de83b75d`. Tip: `bb3b0907` · 2026-09-09T22:15:17Z. Commits not on main: **2567**.

Records receipt for e970f56e6cdcf0193c7b1d0511f36ee0b969bd47 merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for e970f56e6cdcf0193c7b1d0511f36ee0b969bd47 merged onto trunk (landing gate battery-fresh)” · “merge: merge: preservation brief — independently re-derived mechanical-preservation evidence for the reviewer (item 3 prototype)” · “item 3, Sol fence round 9: the alias map was the last private inventory”.

Added observation/plan documents (merge-base comparison): `docs/plans/2026-09-06-astra-patch-proof-usage.md`; `docs/observations/2026-09-06-proto-warm-proof.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:57`:

> {:sha "e970f56e6cdcf0193c7b1d0511f36ee0b969bd47", :started "2026-09-09T22:12:48Z", :wall_s 149, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/proof-burden-receipt-de83b75d && git worktree add --detach <dir> archive/fable/proof-burden-receipt-de83b75d`

### archive/fable/proof-burden-receipt-e7952518

Branch: `fable/proof-burden-receipt-e7952518`. Tip: `e4a55222` · 2026-09-10T01:27:15Z. Commits not on main: **2574**.

Records receipt for f0b814f9b5f11574d4b996905f46e5fcf1174394 merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for f0b814f9b5f11574d4b996905f46e5fcf1174394 merged onto trunk (landing gate battery-fresh)” · “merge: merge: preservation brief — independently re-derived mechanical-preservation evidence for the reviewer (item 3 prototype)” · “item 3, Sol fence round 15 (PB-FENCE-022): the probe exercises the PIPELINE, not a function”.

Added observation/plan documents (merge-base comparison): `docs/plans/2026-09-06-astra-patch-proof-usage.md`; `docs/observations/2026-09-06-proto-warm-proof.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:57`:

> {:sha "f0b814f9b5f11574d4b996905f46e5fcf1174394", :started "2026-09-10T01:24:34Z", :wall_s 161, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/proof-burden-receipt-e7952518 && git worktree add --detach <dir> archive/fable/proof-burden-receipt-e7952518`

### archive/fable/public-result-ceiling

Branch: `fable/public-result-ceiling`. Tip: `c10a90f6` · 2026-09-06T22:26:33Z. Commits not on main: **1980**.

Enforces the public response-size ceiling regardless of error name and corrects unmeasured figures while retaining a bounded prefix.

Subjects: “Correct an unmeasured figure in 81ee8380, and pin that the cut keeps a prefix” · “GREEN: the ceiling binds every response, by size, whatever the error is called” · “RED: the ceiling exempts by error NAME, and the early gate reports a lie”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-06-wiring-test-result.md`; `docs/observations/2026-09-06-fast-typist-real-1-result.md`.

Shared-history battery result (the row names its measured candidate, not a verdict on this branch’s own changes), `docs/observations/battery-ledger.edn:25`:

> {:sha "9167b3acd3834b1bfd47f5438f4e565cfc334e75", :started "2026-09-06T20:13:40Z", :wall_s 785, :verdict :pass, :host "anvil-server"}

Checkout: `git fetch origin tag archive/fable/public-result-ceiling && git worktree add --detach <dir> archive/fable/public-result-ceiling`

### archive/fable/public-result-ceiling-receipt

Branch: `fable/public-result-ceiling-receipt`. Tip: `5c7c9e79` · 2026-09-06T22:45:20Z. Commits not on main: **1998**.

Records receipt for fable/public-result-ceiling r3 tip merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for fable/public-result-ceiling r3 tip merged onto trunk (landing gate battery-fresh)” · “Merge commit 'c10a90f6' into fable/public-result-ceiling-receipt” · “log: receipts round 3 tip 4e996128 posted for GO”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-06-sol-fence-public-result-ceiling-r1.md`; `docs/observations/2026-09-06-wiring-test-result.md`.

Latest retained battery row (its SHA is the measured candidate; no claim that it verifies this tag tip), `docs/observations/battery-ledger.edn:27`:

> {:sha "e6c431a1980d6ad9eddd3e6f5ef8b2e3bc7e58db", :started "2026-09-06T22:32:13Z", :wall_s 786, :verdict :pass, :host "anvil-server"}

Checkout: `git fetch origin tag archive/fable/public-result-ceiling-receipt && git worktree add --detach <dir> archive/fable/public-result-ceiling-receipt`

### archive/fable/receipt-truth

Branch: `fable/receipt-truth`. Tip: `4f42168b` · 2026-09-06T23:20:33Z. Commits not on main: **2027**.

Repairs the truthfulness of receipt evidence and integrates the hot-verification and public-ceiling landings.

Subjects: “Merge origin/MCP/main (387424cc) into fable/receipt-truth” · “Merge remote-tracking branch 'origin/MCP/main' into HEAD” · “merge: hot verification: end the nREPL read at the first terminal status for its own id under one deadline (61 s → 1.1 s on the real H5 call); interrupted is a failure; typed timeout/transport-closed; bounded diagnostics; ceiling rounding up; corpus pin 1381 (sum of both branches); MCP-OP-HOTVER-001/002; inb-adcc9e; Astra GO ten-3c; battery receipt on the merged tree”.

Added observation/plan documents (merge-base comparison): `docs/plans/mission-git-receipt.md`; `docs/observations/2026-09-06-receipt-fixture-repair-astra.md`.

Shared-history battery result (the row names its measured candidate, not a verdict on this branch’s own changes), `docs/observations/battery-ledger.edn:28`:

> {:sha "35ad3b3fd611b89c3a6f6fc782e589b3c4e909d9", :started "2026-09-06T22:56:13Z", :wall_s 787, :verdict :pass, :host "anvil-server"}

Checkout: `git fetch origin tag archive/fable/receipt-truth && git worktree add --detach <dir> archive/fable/receipt-truth`

### archive/fable/receipt-truth-receipt

Branch: `fable/receipt-truth-receipt`. Tip: `23837e5e` · 2026-09-06T23:34:11Z. Commits not on main: **2030**.

Records receipt for fable/receipt-truth merged tip onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for fable/receipt-truth merged tip onto trunk (landing gate battery-fresh)” · “Merge commit '4f42168b' into fable/receipt-truth-receipt” · “Merge origin/MCP/main (387424cc) into fable/receipt-truth”.

Added observation/plan documents (merge-base comparison): `docs/plans/mission-git-receipt.md`; `docs/observations/2026-09-06-receipt-fixture-repair-astra.md`.

Latest retained battery row (its SHA is the measured candidate; no claim that it verifies this tag tip), `docs/observations/battery-ledger.edn:29`:

> {:sha "a192378dc50ddbbb1b6912e070f532799cb1fda3", :started "2026-09-06T23:21:07Z", :wall_s 784, :verdict :pass, :host "anvil-server"}

Checkout: `git fetch origin tag archive/fable/receipt-truth-receipt && git worktree add --detach <dir> archive/fable/receipt-truth-receipt`

### archive/fable/refusal-text-shape

Branch: `fable/refusal-text-shape`. Tip: `2e6109cc` · 2026-09-06T23:41:33Z. Commits not on main: **2051**.

Encodes rendered next_call data without prose sanitization and integrates the receipt repairs.

Subjects: “merge: MCP/main 0ad609ff (the receipts landing) into fable/refusal-text-shape” · “Merge remote-tracking branch 'origin/MCP/main' into HEAD” · “fix(mcp): the rendered next_call is ENCODED, never prose-sanitised”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-06-sol-fence-refusal-text-r7.md`; `docs/observations/2026-09-06-sol-fence-refusal-text-r6.md`.

Shared-history battery result (the row names its measured candidate, not a verdict on this branch’s own changes), `docs/observations/battery-ledger.edn:29`:

> {:sha "a192378dc50ddbbb1b6912e070f532799cb1fda3", :started "2026-09-06T23:21:07Z", :wall_s 784, :verdict :pass, :host "anvil-server"}

Checkout: `git fetch origin tag archive/fable/refusal-text-shape && git worktree add --detach <dir> archive/fable/refusal-text-shape`

### archive/fable/refusal-text-shape-receipt

Branch: `fable/refusal-text-shape-receipt`. Tip: `9f75e0ac` · 2026-09-06T23:55:12Z. Commits not on main: **2056**.

Records receipt for fable/refusal-text-shape merged tip onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for fable/refusal-text-shape merged tip onto trunk (landing gate battery-fresh)” · “Merge commit '2e6109cc6986888c996180a68053b25eb9198e89' into fable/refusal-text-shape-receipt” · “resume note: three of four landed; refusal-text in flight; exact next action”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-06-sol-fence-refusal-text-r7.md`; `docs/observations/2026-09-06-sol-fence-refusal-text-r6.md`.

Latest retained battery row (its SHA is the measured candidate; no claim that it verifies this tag tip), `docs/observations/battery-ledger.edn:30`:

> {:sha "e0c3bc79d80f6dd3d0871182bded222b4ffd68c6", :started "2026-09-06T23:42:04Z", :wall_s 788, :verdict :pass, :host "anvil-server"}

Checkout: `git fetch origin tag archive/fable/refusal-text-shape-receipt && git worktree add --detach <dir> archive/fable/refusal-text-shape-receipt`

### archive/fable/regns-entrance

Branch: `fable/regns-entrance`. Tip: `2c49e376` · 2026-09-14T19:47:18Z. Commits not on main: **2905**.

Bounds registration checks to the real manifest runner and records observer wall under load, moving seed-Make agreement out of the battery.

Subjects: “test: move registration seed Make agreement out of battery” · “test: report concurrent registration observer wall under battery load” · “test: bound registration matrix to the real manifest runner”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-08-row2-entrance.md`; `docs/plans/register-test-ns-round8.md`.

Shared-history battery result (the row names its measured candidate, not a verdict on this branch’s own changes), `docs/observations/battery-ledger.edn:81`:

> {:sha "d033498227b3733874207cfae1063bd4bb0d848f", :started "2026-09-14T11:23:05Z", :wall_s 198, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/regns-entrance && git worktree add --detach <dir> archive/fable/regns-entrance`

### archive/fable/rename-alias-r1

Branch: `fable/rename-alias-r1`. Tip: `a1989b12` · 2026-09-10T23:50:46Z. Commits not on main: **2672**.

Builds rename_alias with ordered global validation and distinguishes namespace aliases from declaration symbols, retaining preservation regressions.

Subjects: “docs: record rename_alias witnesses and final E4 verification” · “fix: honor global validation stages and distinguish aliases from declaration symbols” · “test: strengthen alias preservation and expose cross-file refusal order RED”.

Added observation/plan documents (merge-base comparison): `docs/plans/rename-alias-v1.md`; `docs/plans/alias-migration-mixed-refer-repair.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:60`:

> {:sha "23cd511c1fa924c731f7833db1e2c36650ba1ee3", :started "2026-09-10T23:27:19Z", :wall_s 182, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/rename-alias-r1 && git worktree add --detach <dir> archive/fable/rename-alias-r1`

### archive/fable/rename-alias-r2

Branch: `fable/rename-alias-r2`. Tip: `273a8bbf` · 2026-09-11T00:40:04Z. Commits not on main: **2679**.

Repairs rename_alias eligibility scope, comment ancestry, and source-coordinate planning, with sixteen additional witnesses.

Subjects: “Register fix-round intent and retain the sixteen added census witnesses” · “Scope alias eligibility to selected namespaces and exempt comment ancestry” · “Bound rename planning with binary-search source coordinates”.

Added observation/plan documents (merge-base comparison): `docs/plans/rename-alias-v1.md`; `docs/plans/alias-migration-mixed-refer-repair.md`.

Shared-history battery result (the row names its measured candidate, not a verdict on this branch’s own changes), `docs/observations/battery-ledger.edn:60`:

> {:sha "23cd511c1fa924c731f7833db1e2c36650ba1ee3", :started "2026-09-10T23:27:19Z", :wall_s 182, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/rename-alias-r2 && git worktree add --detach <dir> archive/fable/rename-alias-r2`

### archive/fable/rename-alias-r3

Branch: `fable/rename-alias-r3`. Tip: `769bf2a5` · 2026-09-11T01:05:15Z. Commits not on main: **2682**.

Lands rename_alias through MCP and CLI request files and updates the typed tool catalog.

Subjects: “battery: receipt for 9dd42fa37c537b1783673a9c27ee51c05593612e merged onto trunk (landing gate battery-fresh)” · “merge: rename_alias v1: the second verb through MCP and the CLI request-file entrance (programs to contracts)” · “tests: the tool catalog carries rename_alias after insert_forms (thirteen typed tools)”.

Added observation/plan documents (merge-base comparison): `docs/plans/rename-alias-v1.md`; `docs/plans/alias-migration-mixed-refer-repair.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:61`:

> {:sha "9dd42fa37c537b1783673a9c27ee51c05593612e", :started "2026-09-11T01:01:57Z", :wall_s 197, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/rename-alias-r3 && git worktree add --detach <dir> archive/fable/rename-alias-r3`

### archive/fable/rename-alias-r4

Branch: `fable/rename-alias-r4`. Tip: `b28569c5` · 2026-09-11T01:13:11Z. Commits not on main: **2683**.

Repairs the rename_alias landing so request-file domain refusals are ordinary tool results, with a fresh battery receipt.

Subjects: “battery: receipt for acd63dae07c203d7e07f11a16dc4745e5402eab1 merged onto trunk (landing gate battery-fresh)” · “merge: rename_alias v1: the second verb through MCP and the CLI request-file entrance (programs to contracts)” · “mcp-tool test: request-file verbs (insert_forms, rename_alias) publish domain refusals as ordinary tool results”.

Added observation/plan documents (merge-base comparison): `docs/plans/rename-alias-v1.md`; `docs/plans/alias-migration-mixed-refer-repair.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:61`:

> {:sha "acd63dae07c203d7e07f11a16dc4745e5402eab1", :started "2026-09-11T01:09:52Z", :wall_s 199, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/rename-alias-r4 && git worktree add --detach <dir> archive/fable/rename-alias-r4`

### archive/fable/rename-alias-r5

Branch: `fable/rename-alias-r5`. Tip: `8d74937c` · 2026-09-11T01:22:47Z. Commits not on main: **2684**.

Completes the rename_alias landing's help and operation-registry coverage with a fresh battery receipt.

Subjects: “battery: receipt for 012c111e3d1a64a0740f70f2f21e8276ac65cc40 merged onto trunk (landing gate battery-fresh)” · “merge: rename_alias v1: the second verb through MCP and the CLI request-file entrance (programs to contracts)” · “help test: the ops registry carries :rename-alias!”.

Added observation/plan documents (merge-base comparison): `docs/plans/rename-alias-v1.md`; `docs/plans/alias-migration-mixed-refer-repair.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:61`:

> {:sha "012c111e3d1a64a0740f70f2f21e8276ac65cc40", :started "2026-09-11T01:19:27Z", :wall_s 200, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/rename-alias-r5 && git worktree add --detach <dir> archive/fable/rename-alias-r5`

### archive/fable/routing-prompts

Branch: `fable/routing-prompts`. Tip: `b409e467` · 2026-09-06T06:04:35Z. Commits not on main: **1527**.

Hardens routing-prompt installation around whole-line markers, symlink refusal, confinement rechecks, and precise refusal locations.

Subjects: “routing: a refusal names the line, not the file; confinement is re-checked inside the write (Sol fence r4 secondaries)” · “routing: confinement is lexical under the real home — a symlinked component is a refusal (Sol fence r4 blocking)” · “routing: a marker must own its line — leading bytes are a refusal, not a rewrite (Sol fence r3)”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-06-sol-fence-fable-routing-prompts-r1.md`; `docs/observations/2026-09-06-routing-prompt-surfaces.md`.

Shared-history battery result (the row names its measured candidate, not a verdict on this branch’s own changes), `docs/observations/battery-ledger.edn:15`:

> {:sha "d326f3d2a1d7ca96d0aaf9766d22c25f99a6d821", :started "2026-09-06T04:05:16Z", :wall_s 699, :verdict :pass, :host "anvil-server"}

Checkout: `git fetch origin tag archive/fable/routing-prompts && git worktree add --detach <dir> archive/fable/routing-prompts`

### archive/fable/seat-path-defaults

Branch: `fable/seat-path-defaults`. Tip: `bac7ba22` · 2026-09-10T17:49:17Z. Commits not on main: **2576**.

Derives artifact, memory-battery, and pressure-status defaults from the current user's paths rather than fixed seat paths.

Subjects: “seat paths: drop the now-unused clojure.string require in mcp_workspace_test” · “seat paths: derived per-user defaults for artifact-root, MEMBAT_ROOT, pressure-status” · “merge: merge: the four macOS gate defects from the first skiff receipt (one memory reader, printed floor, short socket dir, bb minimum + canonicalised paths) ; battery receipt via land-auto”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-08-tighten/SEAT-RECEIPT.md`; `docs/observations/2026-09-08-row2-fresh-seat-pilot.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:58`:

> {:sha "323e58ba6341bd81eec93cab09f40e6b6d3891d1", :started "2026-09-10T17:19:37Z", :wall_s 167, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/seat-path-defaults && git worktree add --detach <dir> archive/fable/seat-path-defaults`

### archive/fable/seat-path-defaults-r2

Branch: `fable/seat-path-defaults-r2`. Tip: `49232f08` · 2026-09-10T18:32:52Z. Commits not on main: **2577**.

Makes artifact-root selection fail closed and repairs the Python pressure-status default with owning intent updates.

Subjects: “seat paths r2: fail-closed artifact-root, fix the Python pressure-status default, update the owning intent (SPF-001/002/003)” · “seat paths: drop the now-unused clojure.string require in mcp_workspace_test” · “seat paths: derived per-user defaults for artifact-root, MEMBAT_ROOT, pressure-status”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-08-tighten/SEAT-RECEIPT.md`; `docs/observations/2026-09-08-row2-fresh-seat-pilot.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:58`:

> {:sha "323e58ba6341bd81eec93cab09f40e6b6d3891d1", :started "2026-09-10T17:19:37Z", :wall_s 167, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/seat-path-defaults-r2 && git worktree add --detach <dir> archive/fable/seat-path-defaults-r2`

### archive/fable/seat-path-defaults-r2-receipt-49232f08

Branch: `fable/seat-path-defaults-r2-receipt-49232f08`. Tip: `f80d4739` · 2026-09-10T18:37:39Z. Commits not on main: **2579**.

Records receipt for 9aebccd0cf02f2af5f45dea513b0fd77cb3fcfdd merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 9aebccd0cf02f2af5f45dea513b0fd77cb3fcfdd merged onto trunk (landing gate battery-fresh)” · “merge: merge: three live seat-path defaults become derived, validated per-user paths (artifact root, memory battery root, pressure status)” · “seat paths r2: fail-closed artifact-root, fix the Python pressure-status default, update the owning intent (SPF-001/002/003)”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-08-tighten/SEAT-RECEIPT.md`; `docs/observations/2026-09-08-row2-fresh-seat-pilot.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:59`:

> {:sha "9aebccd0cf02f2af5f45dea513b0fd77cb3fcfdd", :started "2026-09-10T18:34:57Z", :wall_s 162, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/seat-path-defaults-r2-receipt-49232f08 && git worktree add --detach <dir> archive/fable/seat-path-defaults-r2-receipt-49232f08`

### archive/fable/seat-path-defaults-r3

Branch: `fable/seat-path-defaults-r3`. Tip: `3dd61167` · 2026-09-10T18:46:42Z. Commits not on main: **2578**.

Stops the seat-path witness from writing Python bytecode caches into the tracked tree.

Subjects: “seat paths r3: stop the SPF-002 witness from writing __pycache__/ into the tracked tree” · “seat paths r2: fail-closed artifact-root, fix the Python pressure-status default, update the owning intent (SPF-001/002/003)” · “seat paths: drop the now-unused clojure.string require in mcp_workspace_test”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-08-tighten/SEAT-RECEIPT.md`; `docs/observations/2026-09-08-row2-fresh-seat-pilot.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:58`:

> {:sha "323e58ba6341bd81eec93cab09f40e6b6d3891d1", :started "2026-09-10T17:19:37Z", :wall_s 167, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/seat-path-defaults-r3 && git worktree add --detach <dir> archive/fable/seat-path-defaults-r3`

### archive/fable/seat-path-defaults-r3-receipt-3dd61167

Branch: `fable/seat-path-defaults-r3-receipt-3dd61167`. Tip: `94df2c7f` · 2026-09-10T18:58:08Z. Commits not on main: **2580**.

Records receipt for 1b077cb787839dbf3f6ddaf54449c27301fdfe0c merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 1b077cb787839dbf3f6ddaf54449c27301fdfe0c merged onto trunk (landing gate battery-fresh)” · “merge: merge: three live seat-path defaults become derived, validated per-user paths (artifact root, memory battery root, pressure status)” · “seat paths r3: stop the SPF-002 witness from writing __pycache__/ into the tracked tree”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-08-tighten/SEAT-RECEIPT.md`; `docs/observations/2026-09-08-row2-fresh-seat-pilot.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:59`:

> {:sha "1b077cb787839dbf3f6ddaf54449c27301fdfe0c", :started "2026-09-10T18:55:27Z", :wall_s 161, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/seat-path-defaults-r3-receipt-3dd61167 && git worktree add --detach <dir> archive/fable/seat-path-defaults-r3-receipt-3dd61167`

### archive/fable/seat-path-defaults-r4

Branch: `fable/seat-path-defaults-r4`. Tip: `df1f3a89` · 2026-09-10T19:27:54Z. Commits not on main: **2580**.

Moves fail-closed path classification into production and gives writers one entrance, including the prerequisite runner's classpath repair.

Subjects: “seat paths r4 follow-up: gate-prerequisites needs src on its bb classpath too” · “seat paths r4: production fail-closed path classification, ONE writer entrance (SPF-004/005)” · “seat paths r3: stop the SPF-002 witness from writing __pycache__/ into the tracked tree”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-08-tighten/SEAT-RECEIPT.md`; `docs/observations/2026-09-08-row2-fresh-seat-pilot.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:58`:

> {:sha "323e58ba6341bd81eec93cab09f40e6b6d3891d1", :started "2026-09-10T17:19:37Z", :wall_s 167, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/seat-path-defaults-r4 && git worktree add --detach <dir> archive/fable/seat-path-defaults-r4`

### archive/fable/seat-path-defaults-r4-receipt-df1f3a89

Branch: `fable/seat-path-defaults-r4-receipt-df1f3a89`. Tip: `1c8c2252` · 2026-09-10T19:35:50Z. Commits not on main: **2582**.

Records receipt for 991b1a6b8ddbd8b2e9c42444081073065c3101f8 merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 991b1a6b8ddbd8b2e9c42444081073065c3101f8 merged onto trunk (landing gate battery-fresh)” · “merge: merge: seat-path defaults become derived, validated per-user paths; the path classifier moves to src/; one entrance for every artifact write” · “seat paths r4 follow-up: gate-prerequisites needs src on its bb classpath too”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-08-tighten/SEAT-RECEIPT.md`; `docs/observations/2026-09-08-row2-fresh-seat-pilot.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:59`:

> {:sha "991b1a6b8ddbd8b2e9c42444081073065c3101f8", :started "2026-09-10T19:33:23Z", :wall_s 147, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/seat-path-defaults-r4-receipt-df1f3a89 && git worktree add --detach <dir> archive/fable/seat-path-defaults-r4-receipt-df1f3a89`

### archive/fable/seat-path-defaults-r5

Branch: `fable/seat-path-defaults-r5`. Tip: `d8b3147b` · 2026-09-10T20:23:40Z. Commits not on main: **2583**.

Lands the seat-path classifier and single writer entrance with canonical home-containment checks.

Subjects: “battery: receipt for 5c1aacff8cd647a0365db197655ab958ade6363b merged onto trunk (landing gate battery-fresh)” · “merge: seat-path defaults: classifier in src, single write entrance, canonical home containment (SPF-006)” · “receipt-artifacts: canonicalize home containment before the ownership exception (SPF-006)”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-08-tighten/SEAT-RECEIPT.md`; `docs/observations/2026-09-08-row2-fresh-seat-pilot.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:59`:

> {:sha "5c1aacff8cd647a0365db197655ab958ade6363b", :started "2026-09-10T20:21:07Z", :wall_s 153, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/seat-path-defaults-r5 && git worktree add --detach <dir> archive/fable/seat-path-defaults-r5`

### archive/fable/seat-path-defaults-r6

Branch: `fable/seat-path-defaults-r6`. Tip: `08ab0c21` · 2026-09-10T20:31:58Z. Commits not on main: **2584**.

Adds the home-traversal witness to the test census and preserves the final seat-path landing receipt.

Subjects: “battery: receipt for 1cd04b0e28b699326ec402610c6ac0515885bfac merged onto trunk (landing gate battery-fresh)” · “merge: seat-path defaults: classifier in src, single write entrance, canonical home containment (SPF-006)” · “deftest census: name the SPF-006 traversal witness in the ledger (+1, no removals)”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-08-tighten/SEAT-RECEIPT.md`; `docs/observations/2026-09-08-row2-fresh-seat-pilot.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:59`:

> {:sha "1cd04b0e28b699326ec402610c6ac0515885bfac", :started "2026-09-10T20:29:30Z", :wall_s 147, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/seat-path-defaults-r6 && git worktree add --detach <dir> archive/fable/seat-path-defaults-r6`

### archive/fable/seat-path-defaults-receipt-bac7ba22

Branch: `fable/seat-path-defaults-receipt-bac7ba22`. Tip: `5ce204fe` · 2026-09-10T17:53:49Z. Commits not on main: **2578**.

Records receipt for 1403bc7221e07e607e8c9f39242776f3096a9801 merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 1403bc7221e07e607e8c9f39242776f3096a9801 merged onto trunk (landing gate battery-fresh)” · “merge: merge: three live seat-path defaults become derived per-user paths (artifact root, memory battery root, pressure status)” · “seat paths: drop the now-unused clojure.string require in mcp_workspace_test”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-08-tighten/SEAT-RECEIPT.md`; `docs/observations/2026-09-08-row2-fresh-seat-pilot.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:59`:

> {:sha "1403bc7221e07e607e8c9f39242776f3096a9801", :started "2026-09-10T17:51:11Z", :wall_s 158, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/seat-path-defaults-receipt-bac7ba22 && git worktree add --detach <dir> archive/fable/seat-path-defaults-receipt-bac7ba22`

### archive/fable/skiff-fixes

Branch: `fable/skiff-fixes`. Tip: `98f7d23b` · 2026-09-10T17:13:59Z. Commits not on main: **2571**.

Repairs the first macOS gate findings, including canonical path comparison, unresolved-path refusals, and the distinction between publication and destination.

Subjects: “fence follow-up: separate "was it published" from "where would it go" — the call site SKF-001 exposed” · “fix(fence): SKF-001 — real paths on both sides of the canonical boundary comparison, typed unresolved paths — Sol's GO-WITH-FIX on 32678de6, applied verbatim” · “gate: the tree's own gates caught four things in the first full run”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-06-skiff-install-stable.md`; `docs/plans/parallel-landing-gate.md`.

Shared-history battery result (the row names its measured candidate, not a verdict on this branch’s own changes), `docs/observations/battery-ledger.edn:57`:

> {:sha "6867f773be0f894f2bef0c145f3e07101365919f", :started "2026-09-10T04:39:26Z", :wall_s 149, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/skiff-fixes && git worktree add --detach <dir> archive/fable/skiff-fixes`

### archive/fable/skiff-fixes-receipt-32678de6

Branch: `fable/skiff-fixes-receipt-32678de6`. Tip: `8b6c02d0` · 2026-09-10T17:02:12Z. Commits not on main: **2571**.

Records receipt for 3266d0f8106f4f63f6f1c8aa9f0ec0d7838698bb merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 3266d0f8106f4f63f6f1c8aa9f0ec0d7838698bb merged onto trunk (landing gate battery-fresh)” · “merge: merge: the four macOS gate defects from the first skiff receipt (one memory reader, printed floor, short socket dir, bb minimum + canonicalised paths)” · “gate: the tree's own gates caught four things in the first full run”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-06-skiff-install-stable.md`; `docs/plans/parallel-landing-gate.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:58`:

> {:sha "3266d0f8106f4f63f6f1c8aa9f0ec0d7838698bb", :started "2026-09-10T16:59:40Z", :wall_s 152, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/skiff-fixes-receipt-32678de6 && git worktree add --detach <dir> archive/fable/skiff-fixes-receipt-32678de6`

### archive/fable/skiff-fixes-receipt-98f7d23b

Branch: `fable/skiff-fixes-receipt-98f7d23b`. Tip: `3c831e5b` · 2026-09-10T17:22:25Z. Commits not on main: **2573**.

Records receipt for 323e58ba6341bd81eec93cab09f40e6b6d3891d1 merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 323e58ba6341bd81eec93cab09f40e6b6d3891d1 merged onto trunk (landing gate battery-fresh)” · “merge: merge: the four macOS gate defects from the first skiff receipt (one memory reader, printed floor, short socket dir, bb minimum + canonicalised paths)” · “fence follow-up: separate "was it published" from "where would it go" — the call site SKF-001 exposed”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-06-skiff-install-stable.md`; `docs/plans/parallel-landing-gate.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:58`:

> {:sha "323e58ba6341bd81eec93cab09f40e6b6d3891d1", :started "2026-09-10T17:19:37Z", :wall_s 167, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/skiff-fixes-receipt-98f7d23b && git worktree add --detach <dir> archive/fable/skiff-fixes-receipt-98f7d23b`

### archive/fable/skiff-install

Branch: `fable/skiff-install`. Tip: `6067f938` · 2026-09-10T04:38:23Z. Commits not on main: **2559**.

Adds installation preflight for RAM-backed temporary directories and hardens filesystem parsing, preserving legal trailing spaces in mount paths.

Subjects: “fence r2: a trailing space is a legal path byte — stop trimming the mount point” · “fence: close Sol's three findings — the parser could prove the wrong filesystem” · “preflight: catch a RAM-backed temp dir before 113 lanes exit 97”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-06-skiff-install-stable.md`; `docs/plans/parallel-landing-gate.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:56`:

> {:sha "a51dd39e955972ddac1d417d40e73e05f30637c1", :started "2026-09-09T08:06:00Z", :wall_s 149, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/skiff-install && git worktree add --detach <dir> archive/fable/skiff-install`

### archive/fable/skiff-install-receipt-064bc8bc

Branch: `fable/skiff-install-receipt-064bc8bc`. Tip: `54ac3fd8` · 2026-09-10T04:27:30Z. Commits not on main: **2560**.

Records receipt for ea061e9dab6cc4213f2fb6a4a4ca8bd91e874768 merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for ea061e9dab6cc4213f2fb6a4a4ca8bd91e874768 merged onto trunk (landing gate battery-fresh)” · “merge: merge: make install and the gate usable on macOS and any clean box (mount authority, path-socket admission, derived lock path, preflight)” · “fence: close Sol's three findings — the parser could prove the wrong filesystem”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-06-skiff-install-stable.md`; `docs/plans/parallel-landing-gate.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:57`:

> {:sha "ea061e9dab6cc4213f2fb6a4a4ca8bd91e874768", :started "2026-09-10T04:25:00Z", :wall_s 149, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/skiff-install-receipt-064bc8bc && git worktree add --detach <dir> archive/fable/skiff-install-receipt-064bc8bc`

### archive/fable/skiff-install-receipt-3585c7e5

Branch: `fable/skiff-install-receipt-3585c7e5`. Tip: `5bb93d91` · 2026-09-10T04:05:50Z. Commits not on main: **2559**.

Records receipt for 3a3f2daa01615e01509aae1acccf23c4951115a4 merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 3a3f2daa01615e01509aae1acccf23c4951115a4 merged onto trunk (landing gate battery-fresh)” · “merge: merge: make install and the gate usable on macOS and any clean box (mount authority, path-socket admission, derived lock path, preflight)” · “preflight: catch a RAM-backed temp dir before 113 lanes exit 97”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-06-skiff-install-stable.md`; `docs/plans/parallel-landing-gate.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:57`:

> {:sha "3a3f2daa01615e01509aae1acccf23c4951115a4", :started "2026-09-10T04:03:21Z", :wall_s 149, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/skiff-install-receipt-3585c7e5 && git worktree add --detach <dir> archive/fable/skiff-install-receipt-3585c7e5`

### archive/fable/skiff-install-receipt-6067f938

Branch: `fable/skiff-install-receipt-6067f938`. Tip: `2a795414` · 2026-09-10T04:41:56Z. Commits not on main: **2561**.

Records receipt for 6867f773be0f894f2bef0c145f3e07101365919f merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for 6867f773be0f894f2bef0c145f3e07101365919f merged onto trunk (landing gate battery-fresh)” · “merge: merge: make install and the gate usable on macOS and any clean box (mount authority, path-socket admission, derived lock path, preflight)” · “fence r2: a trailing space is a legal path byte — stop trimming the mount point”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-06-skiff-install-stable.md`; `docs/plans/parallel-landing-gate.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:57`:

> {:sha "6867f773be0f894f2bef0c145f3e07101365919f", :started "2026-09-10T04:39:26Z", :wall_s 149, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/skiff-install-receipt-6067f938 && git worktree add --detach <dir> archive/fable/skiff-install-receipt-6067f938`

### archive/fable/state-home-identity

Branch: `fable/state-home-identity`. Tip: `9267d814` · 2026-09-14T01:17:01Z. Commits not on main: **2861**.

Hardens state-home identity and integrates the transaction tombstone-race repair while retaining append-only battery evidence.

Subjects: “merge: origin/MCP/main e2050d8f (txn race fix) into fable/state-home-identity — battery evidence ledgers merged (both rows kept; walls from trunk); census verified” · “merge: txn: a path removed inside receipt-artifacts' admission windows resolves absent, never throws (the tombstone-collision race, reproduced as 21 deterministic schedules; inb-c74f05) ; battery receipt via land-auto” · “battery: receipt for 5ae0066c75ef3b19d28c94405aa674573ce5a29a merged onto trunk (landing gate battery-fresh)”.

Added observation/plan documents (merge-base comparison): `docs/plans/state-home-identity.md`; `docs/observations/2026-09-12-bbtower-block-b/attempt22/portability-census.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:77`:

> {:sha "5ae0066c75ef3b19d28c94405aa674573ce5a29a", :started "2026-09-14T00:47:33Z", :wall_s 197, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/state-home-identity && git worktree add --detach <dir> archive/fable/state-home-identity`

### archive/fable/strictly-better-receipt

Branch: `fable/strictly-better-receipt`. Tip: `2c68e673` · 2026-09-06T20:26:45Z. Commits not on main: **1943**.

Records receipt for fable/strictly-better-skill tip merged onto trunk (landing gate battery-fresh).

Subjects: “battery: receipt for fable/strictly-better-skill tip merged onto trunk (landing gate battery-fresh)” · “Merge commit '5d0b31f7a56009dd042cc54c0b1803c2dcb4f2da' into fable/strictly-better-receipt” · “plate: fan-out step 1 stops mandating inspect after rg”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-06-strictly-better-evidence.md`; `docs/observations/2026-09-06-astra-strict-3-review.md`.

Latest retained battery row (its SHA is the measured candidate; no claim that it verifies this tag tip), `docs/observations/battery-ledger.edn:25`:

> {:sha "9167b3acd3834b1bfd47f5438f4e565cfc334e75", :started "2026-09-06T20:13:40Z", :wall_s 785, :verdict :pass, :host "anvil-server"}

Checkout: `git fetch origin tag archive/fable/strictly-better-receipt && git worktree add --detach <dir> archive/fable/strictly-better-receipt`

### archive/fable/strictly-better-skill

Branch: `fable/strictly-better-skill`. Tip: `5d0b31f7` · 2026-09-06T20:13:15Z. Commits not on main: **1931**.

Publishes the two admitted routing classes with executed fixture requests and removes a redundant inspect step after sufficient native discovery.

Subjects: “plate: fan-out step 1 stops mandating inspect after rg” · “plate: publish the executed fixture requests as the examples” · “skill+plate: two automatic classes, executed examples, evidence doc”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-06-strictly-better-evidence.md`; `docs/observations/2026-09-06-gene-report-1950z-crank.md`.

Shared-history battery result (the row names its measured candidate, not a verdict on this branch’s own changes), `docs/observations/battery-ledger.edn:24`:

> {:sha "a60b0a9121913551d48bc81b886e837adf2629c2", :started "2026-09-06T18:42:35Z", :wall_s 859, :verdict :pass, :host "anvil-server"}

Checkout: `git fetch origin tag archive/fable/strictly-better-skill && git worktree add --detach <dir> archive/fable/strictly-better-skill`

### archive/fable/txn-tombstone-race

Branch: `fable/txn-tombstone-race`. Tip: `bc65a76a` · 2026-09-13T23:47:49Z. Commits not on main: **2837**.

Reproduces the transaction tombstone collision as deterministic schedules and makes paths removed during admission resolve as absent instead of throwing.

Subjects: “green: a path removed inside the admission windows resolves absent, not throws” · “red: the tombstone-collision race reproduced as a schedule, not a hammer” · “merge: data-not-code: destination envelope admission at the artifact boundary, refusal classification as data, receipt-backed evidence rows, sleep identities, fixed-point diff-impact oracle in the gate's environment, probe measure preregistration ; battery receipt via land-auto”.

Added observation/plan documents (merge-base comparison): `docs/plans/data-not-code-round9.md`; `docs/observations/2026-09-12-data-not-code/round6/plan.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:75`:

> {:sha "b33bf3b0d119bff1a88a827f4f7552ac9b204120", :started "2026-09-13T05:34:43Z", :wall_s 178, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/fable/txn-tombstone-race && git worktree add --detach <dir> archive/fable/txn-tombstone-race`

### archive/fable/typist-real-repo

Branch: `fable/typist-real-repo`. Tip: `92df4b65` · 2026-09-06T04:57:10Z. Commits not on main: **1461**.

Runs the typist against real repository specimens and corrects the stated limits of its cooperative working-directory fence.

Subjects: “typist-run: stale WRITE FENCE comments corrected — the NW fence bounds a cooperative child's cwd and the runner's own writes only; an unsandboxed Codex child is not confined; /var/tmp is disk (Astra 04:49Z); no behaviour change” · “battery: receipt for d326f3d2 (merged tree)” · “merge: origin/MCP/main into fable/typist-real-repo (manifest pins recomputed on the merged tree)”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-06-sol-fence-fable-typist-real-repo-r6.md`; `docs/observations/2026-09-06-sol-fence-fable-typist-real-repo-r5.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:15`:

> {:sha "d326f3d2a1d7ca96d0aaf9766d22c25f99a6d821", :started "2026-09-06T04:05:16Z", :wall_s 699, :verdict :pass, :host "anvil-server"}

Checkout: `git fetch origin tag archive/fable/typist-real-repo && git worktree add --detach <dir> archive/fable/typist-real-repo`

## experiment/* — 40 tags

These late-August experiment branches retain preregistrations, instruments, positive and negative screens, and corrections to earlier claims. Reports sometimes live under bench/ or dev/; a synthetic screen, install decision, or invalid run is not promoted here into a general speed claim.

### archive/experiment/claude-route-result-price-tag-20260829

Branch: `experiment/claude-route-result-price-tag-20260829`. Tip: `bada0b40` · 2026-08-29T22:06:26-07:00. Commits not on main: **1**.

Tests a route hint attached to Claude inspect results.

Subjects: “docs: record Claude inspect-result route hint experiment”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-29-claude-inspect-result-price-tag-routing-experiment.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-29-claude-inspect-result-price-tag-routing-experiment.md:8`:

> **Decision:** The in-band price tag gets no causal credit in this task. Present routed to Surgeon 16/16, but absent also routed 16/16. Preserve the 100% unhinted Claude baseline and do not expand this saturated cohort.

Checkout: `git fetch origin tag archive/experiment/claude-route-result-price-tag-20260829 && git worktree add --detach <dir> archive/experiment/claude-route-result-price-tag-20260829`

### archive/experiment/codex-catalog-floor-sweep

Branch: `experiment/codex-catalog-floor-sweep`. Tip: `87a53ec7` · 2026-08-29T21:09:18-07:00. Commits not on main: **9**.

Measures the Codex catalog floor while retaining absent-catalog projections and correctly framed registry hashes.

Subjects: “Record catalog floor sweep result” · “bench: match retained registry hash framing” · “bench: bind absent catalog and retain projection”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-29-codex-catalog-floor-sweep-protocol.md`; `docs/observations/2026-08-29-captains-log-the-490-ms-tax-became-41-ms.md`.

Recorded result (scope and candidate as quoted), `docs/observations/2026-08-29-captains-log-the-490-ms-tax-became-41-ms.md:16`:

> The local clock found a real effect: enabling a tiny MCP server added about
> **41 ms** before the turn started. Adding another **64 KiB** to that tool added
> **no measurable time**. The real four-tool catalog was not slower than its
> one-tool projection. Complete turn times varied by seconds because of service
> and answer latency, not catalog size.
> 
> That closes this hill. We should not hide tools to save milliseconds that the
> catalog does not consume. Under-declaring a catalog can silently remove a tool
> from the model; it buys no demonstrated speed. The better architecture is the
> clear, safe surface we already have, with optimization effort returned to
> turn-count and output construction where the measured costs actually live.

Checkout: `git fetch origin tag archive/experiment/codex-catalog-floor-sweep && git worktree add --detach <dir> archive/experiment/codex-catalog-floor-sweep`

### archive/experiment/consumption-gap-20260830

Branch: `experiment/consumption-gap-20260830`. Tip: `1648d5db` · 2026-08-30T16:16:26-07:00. Commits not on main: **2**.

Classifies gaps between production refusal evidence and what callers consume.

Subjects: “study: type the production refusal evidence gap” · “experiment: preregister refusal consumption-gap classifier”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-30-consumption-gap-result.md`; `docs/observations/2026-08-30-consumption-gap-preregistration.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-30-consumption-gap-result.md:6`:

> Status: **Phase 1 instrumentation stop; Phase 2 forbidden**

Checkout: `git fetch origin tag archive/experiment/consumption-gap-20260830 && git worktree add --detach <dir> archive/experiment/consumption-gap-20260830`

### archive/experiment/differential-routing-interview-20260829

Branch: `experiment/differential-routing-interview-20260829`. Tip: `a9afcd13` · 2026-08-30T00:26:09-07:00. Commits not on main: **12**.

Ablates explanations in differential-routing interviews to study their effect.

Subjects: “study: ablate differential routing explanations” · “grave-revisit plan: kills are single-corpus; one census stress-tests Grade A” · “docs: seventh withdrawal — a wrong number was inside the corrections section”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-30-differential-routing-interview-and-ablations.md`; `docs/observations/2026-08-30-grave-revisit-plan.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-30-differential-routing-interview-and-ablations.md:10`:

> **Decision:** Target the prepared-request interface, not an executor-wide reluctance theory. Exactness and file size were killed as causes on this task. A ready-to-submit guarded transaction moved Codex from 2/4 to 4/4 Surgeon-first, exactly the preregistered +50-point gate.

Checkout: `git fetch origin tag archive/experiment/differential-routing-interview-20260829 && git worktree add --detach <dir> archive/experiment/differential-routing-interview-20260829`

### archive/experiment/elaborator-fallback-battery-20260831

Branch: `experiment/elaborator-fallback-battery-20260831`. Tip: `2a2915f6` · 2026-08-31T00:49:00-07:00. Commits not on main: **3**.

Runs and records the elaborator fallback battery, including immediate arms and the Spark addendum.

Subjects: “bench: land Spark addendum and fallback verdict” · “bench: record fallback battery immediate arms” · “bench: preregister elaborator fallback battery”.

No added observation/plan Markdown documents in the merge-base diff.

Recorded result (scope and candidate as quoted), `bench/model-variant-battery/REPORT.md:12`:

> Spark and Terra were both 6/6 exact on the guarded fill class. Spark's median model-generation wall was 0.922 s versus Terra's 1.558 s: Spark used 40.8% less wall, a 1.69x throughput proxy. The full guarded bang was 1.460 s versus 2.258 s, 35.3% less wall.
> 
> Terra is the honest operational pick for “reliable enough but faster than Sol” on the workload the flywheel actually performs: it was 6/6 exact with zero wrong-subject and schema failures, and its 1.558 s median model-generation wall was 48.9% lower than Sol's 3.051 s. Keep the structural guards and Sol behind it: Terra failed all five fixed-length decode oracles by stopping at an exact numeric prefix between 88 and 165 rather than reaching 200, so this battery does **not** establish generalized Terra decode superiority.
> 
> Under the frozen confirmatory rule, no non-Sol model earned the stronger label “proven faster than Sol”: Terra had no valid fixed-length decode estimate, while Luna was less reliable and its resolved bootstrap-subtracted rate rounded to the same 58.1 tok/s as Sol. The deployment ladder above explicitly uses direct fill wall as an operational tie-break where the preregistered decode tie-break is missing or unresolved.

Checkout: `git fetch origin tag archive/experiment/elaborator-fallback-battery-20260831 && git worktree add --detach <dir> archive/experiment/elaborator-fallback-battery-20260831`

### archive/experiment/embedded-spark-probe-20260830

Branch: `experiment/embedded-spark-probe-20260830`. Tip: `44a5bac7` · 2026-08-30T16:00:48-07:00. Commits not on main: **1**.

Probes an embedded Spark elaborator in the application server.

Subjects: “docs: probe embedded Spark app-server elaborator”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-30-embedded-spark-elaborator-probe.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-30-embedded-spark-elaborator-probe.md:13`:

> ## Verdict
> 
> Yes: the installed CLI has a viable non-terminal mode for a warm Spark
> elaborator. The best candidate is a **dedicated, supervised**
> `codex app-server --listen stdio://` child owned by the clj-surgeon MCP server.
> It accepts JSONL-framed JSON-RPC-shaped messages, stays alive across turns,
> loads the existing ChatGPT subscription auth, pins
> `gpt-5.3-codex-spark`, supports an ephemeral thread, and constrains each final
> message with `turn/start.outputSchema`.

Checkout: `git fetch origin tag archive/experiment/embedded-spark-probe-20260830 && git worktree add --detach <dir> archive/experiment/embedded-spark-probe-20260830`

### archive/experiment/emission-compression-screen

Branch: `experiment/emission-compression-screen`. Tip: `5347ad82` · 2026-08-29T12:49:58-07:00. Commits not on main: **324**.

Tests request-emission compression and preserves wrong-owner authority counterexamples that invalidate unsafe indexed composition.

Subjects: “test: expose positional wrong-owner authority” · “bench: preregister unsafe compression predictor test” · “bench: reject unsafe indexed composition”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-29-emission-compression-composition-screen.md`; `docs/observations/2026-08-29-emission-compression-option-portfolio.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-29-emission-compression-composition-screen.md:11`:

> Status: superseded by the deterministic wrong-index NO-GO in
> `2026-08-29-wrong-index-ended-emission-composition.md`. Do not launch the
> model cohort described below.

Checkout: `git fetch origin tag archive/experiment/emission-compression-screen && git worktree add --detach <dir> archive/experiment/emission-compression-screen`

### archive/experiment/external-corpus-shape-census

Branch: `experiment/external-corpus-shape-census`. Tip: `28ee81f4` · 2026-08-30T00:05:27-07:00. Commits not on main: **4**.

Censuses real external Clojure write shapes to test the tool's eligible workload and adoption claims.

Subjects: “experiment: census external Clojure write shapes” · “research: preregister external corpus shape census” · “clarify adoption refusal failure boundary”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-30-external-corpus-write-shape-census.md`; `docs/observations/2026-08-29-external-corpus-shape-census-preregistration.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-30-external-corpus-write-shape-census.md:7`:

> Verdict: **Codex mutation adoption remains below 2%, insertion does not cross the reprice gate, but the 6% addressable ceiling does not generalize. Subject repetition remains unresolved at owner identity.**

Checkout: `git fetch origin tag archive/experiment/external-corpus-shape-census && git worktree add --detach <dir> archive/experiment/external-corpus-shape-census`

### archive/experiment/fromto-overlap-study-20260830

Branch: `experiment/fromto-overlap-study-20260830`. Tip: `b51f802a` · 2026-08-30T14:25:27-07:00. Commits not on main: **3**.

Measures repetition between from/to payloads and corrects the canonical request checksum rule.

Subjects: “study: measure from-to double repetition” · “study: correct canonical request checksum rule” · “study: preregister from-to repetition measurement”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-30-from-to-double-repetition-study.md`.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/experiment/fromto-overlap-study-20260830 && git worktree add --detach <dir> archive/experiment/fromto-overlap-study-20260830`

### archive/experiment/fuel-table-completion-20260831

Branch: `experiment/fuel-table-completion-20260831`. Tip: `c390afb5` · 2026-08-30T21:43:16-07:00. Commits not on main: **3**.

Completes the elaborator fuel table using the retained fallback-battery measurements.

Subjects: “bench: complete elaborator fuel table” · “bench: record fallback battery immediate arms” · “bench: preregister elaborator fallback battery”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-31-elaborator-fuel-table.md`.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/experiment/fuel-table-completion-20260831 && git worktree add --detach <dir> archive/experiment/fuel-table-completion-20260831`

### archive/experiment/mcp-catalog-screen-6128b9b

Branch: `experiment/mcp-catalog-screen-6128b9b`. Tip: `6128b9ba` · 2026-08-27T22:48:18-07:00. Commits not on main: **144**.

Builds effect-aware catalog challengers and removes naming bias from benchmark prompts.

Subjects: “Make catalog benchmark prompts name-neutral” · “Add effect-aware MCP catalog challengers” · “Make Option M operations orthogonal”.

Added observation/plan documents (merge-base comparison): `docs/plans/public-tool-operation-identity-lid-slice.md`; `docs/plans/public-mutation-tool-naming-adversarial-review.md`.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/experiment/mcp-catalog-screen-6128b9b && git worktree add --detach <dir> archive/experiment/mcp-catalog-screen-6128b9b`

### archive/experiment/mcp-catalog-screen-clean-a953e41

Branch: `experiment/mcp-catalog-screen-clean-a953e41`. Tip: `a953e418` · 2026-08-28T00:19:09-07:00. Commits not on main: **151**.

Projects candidate catalog responses by role and records the first selected tool and interface-facade ratchet.

Subjects: “Project candidate catalog responses by role” · “Log the public interface facade ratchet” · “Record first selected catalog tool”.

Added observation/plan documents (merge-base comparison): `docs/plans/public-tool-operation-identity-lid-slice.md`; `docs/plans/public-mutation-tool-naming-adversarial-review.md`.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/experiment/mcp-catalog-screen-clean-a953e41 && git worktree add --detach <dir> archive/experiment/mcp-catalog-screen-clean-a953e41`

### archive/experiment/mcp-catalog-screen-verdict-e4c37eb

Branch: `experiment/mcp-catalog-screen-verdict-e4c37eb`. Tip: `e4c37eb5` · 2026-08-28T00:42:54-07:00. Commits not on main: **152**.

Preserves the clean catalog-screen verdict on the role-projected candidate interface.

Subjects: “Record clean tool catalog screen verdict” · “Project candidate catalog responses by role” · “Log the public interface facade ratchet”.

Added observation/plan documents (merge-base comparison): `docs/plans/public-tool-operation-identity-lid-slice.md`; `docs/plans/public-mutation-tool-naming-adversarial-review.md`.

Recorded result (scope and candidate as quoted), `docs/observations/2026-08-27-captains-log-tool-names-enter-the-arena.md:6`:

> **Status:** clean alternate-universe screen complete; keep catalog A

Checkout: `git fetch origin tag archive/experiment/mcp-catalog-screen-verdict-e4c37eb && git worktree add --detach <dir> archive/experiment/mcp-catalog-screen-verdict-e4c37eb`

### archive/experiment/multisite-headtohead-20260830

Branch: `experiment/multisite-headtohead-20260830`. Tip: `b06f41e8` · 2026-08-30T07:54:42-07:00. Commits not on main: **6**.

Compares multi-site mutation routes under an execution-validated cohort and retains the killed configuration-repair attempt.

Subjects: “Report multi-site mutation head-to-head” · “Preregister exec-validated multi-site cohort” · “Retain killed config-repair cohort”.

No added observation/plan Markdown documents in the merge-base diff.

Recorded result (scope and candidate as quoted), `bench/results/2026-08-30-multisite-headtohead/REPORT.md:5`:

> The preregistered claim is **unsupported on this fixture**. Surgeon was not
> faster in either primary metric. It emitted a larger median mutation payload
> and took more than twice as long as native patching.
> 
> All 12 confirmatory episodes were environment-valid, byte-exact, and
> route-adherent. The kill criterion did not trigger.
> 
> | Primary result | Native `apply_patch` | Surgeon `edit_clojure` | S - N | `(S - N) / N` |
> |---|---:|---:|---:|---:|
> | Median mutation-payload bytes | 607 | 1,133 | +526 | +86.66% |
> | Median mutation-payload tokens | 171 | 318 | +147 | +85.96% |
> | Median wall time | 9.054 s | 21.402 s | +12.348 s | +136.38% |

Checkout: `git fetch origin tag archive/experiment/multisite-headtohead-20260830 && git worktree add --detach <dir> archive/experiment/multisite-headtohead-20260830`

### archive/experiment/native-prelanding-gate-audit

Branch: `experiment/native-prelanding-gate-audit`. Tip: `e9d22019` · 2026-08-30T00:12:07-07:00. Commits not on main: **4**.

Audits the value of a native prelanding parse gate against the production work window.

Subjects: “Close native parse gate on production window” · “Audit native prelanding parse gate prize” · “clarify adoption refusal failure boundary”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-29-native-prelanding-parse-gate-prize-audit.md`; `docs/observations/2026-08-30-production-parse-gate-reopening-screen.md`.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/experiment/native-prelanding-gate-audit && git worktree add --detach <dir> archive/experiment/native-prelanding-gate-audit`

### archive/experiment/ordinal-refusal-screen-20260830

Branch: `experiment/ordinal-refusal-screen-20260830`. Tip: `2919ad8c` · 2026-08-30T14:23:06-07:00. Commits not on main: **2**.

Screens ordinal-based refusal recovery and binds the retained result to Git evidence.

Subjects: “docs: bind ordinal screen git receipt” · “experiment: screen ordinal refusal recovery”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-30-ordinal-refusal-recovery-screen.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-30-ordinal-refusal-recovery-screen.md:4`:

> Status: **prototype screen passes its registered mechanism gate**  
> Boundary: **experiment-only evidence for a later ratification decision; not product code or authority**

Checkout: `git fetch origin tag archive/experiment/ordinal-refusal-screen-20260830 && git worktree add --detach <dir> archive/experiment/ordinal-refusal-screen-20260830`

### archive/experiment/prepared-confirm-affinity-live-measure-20260831

Branch: `experiment/prepared-confirm-affinity-live-measure-20260831`. Tip: `5fb34454` · 2026-08-30T22:01:17-07:00. Commits not on main: **10**.

Measures the live price of prepared-confirmation affinity recovery using the exact wire remedy.

Subjects: “bench: measure prepared affinity live route price” · “bench: bind affinity remedy to exact wire text” · “bench: freeze prepared affinity live-route measurement”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-31-prepared-confirm-affinity-live-route-token-measurement.md`; `docs/observations/2026-08-31-prepared-confirm-affinity-live-route-protocol.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-31-prepared-confirm-affinity-live-route-token-measurement.md:5`:

> Verdict: **GO to the affinity breakfast/install decision, not an install or
> performance claim.** The candidate adds actionable session guidance and exact
> invalid-field names for a small input-side price. Ordinary successful reads
> and writes remain byte-identical after only the predeclared dynamic exclusions.

Checkout: `git fetch origin tag archive/experiment/prepared-confirm-affinity-live-measure-20260831 && git worktree add --detach <dir> archive/experiment/prepared-confirm-affinity-live-measure-20260831`

### archive/experiment/prepared-confirm-preview-live-token-measurement

Branch: `experiment/prepared-confirm-preview-live-token-measurement`. Tip: `db6b0fb2` · 2026-08-30T20:33:59-07:00. Commits not on main: **4**.

Measures prepared confirmation and preview on the live route, normalizing inspection timing and fencing measurement paths.

Subjects: “bench: measure prepared actions live route price” · “bench: normalize inspect phase timing” · “bench: fence prepared measurement paths”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-30-prepared-confirm-preview-live-route-token-measurement.md`; `docs/observations/2026-08-30-prepared-confirm-preview-live-route-protocol.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-30-prepared-confirm-preview-live-route-token-measurement.md:5`:

> Verdict: **GO to Gene's install decision, not an install or performance
> claim.** Exact candidate `05f5a196` makes the public confirm/fill and preview
> routes usable on real stdio JSON. On this one-owner prepared edit, the compact
> arguments are 130 bytes / 64 `o200k_base` tokens instead of 288 bytes / 84
> tokens: **158 bytes and 20 emitted proxy tokens removed**. That is a measured
> 23.8% argument-token reduction, materially smaller than the old projected
> 217-byte-to-30-byte shorthand.

Checkout: `git fetch origin tag archive/experiment/prepared-confirm-preview-live-token-measurement && git worktree add --detach <dir> archive/experiment/prepared-confirm-preview-live-token-measurement`

### archive/experiment/prepared-request-proxy-screen-20260830

Branch: `experiment/prepared-request-proxy-screen-20260830`. Tip: `ab5759e5` · 2026-08-30T10:24:49-07:00. Commits not on main: **11**.

Tests the prepared-request proxy and separates safe execution from adherence to the requested route.

Subjects: “Record prepared request proxy verdict” · “Separate prepared proxy safety from route adherence” · “Close prepared proxy supervisor stdin”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-30-prepared-request-option-a-proxy-valid-verdict.md`; `docs/observations/2026-08-30-prepared-request-option-a-proxy-stdin-stop.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-30-prepared-request-option-a-proxy-valid-verdict.md:13`:

> ## Verdict
> 
> The cohort is valid. Option A **fails the registered routing gate** and does not advance to the
> leaf LLD, EARS requirements, red tests, or product code.

Checkout: `git fetch origin tag archive/experiment/prepared-request-proxy-screen-20260830 && git worktree add --detach <dir> archive/experiment/prepared-request-proxy-screen-20260830`

### archive/experiment/prepared-request-replication-20260830

Branch: `experiment/prepared-request-replication-20260830`. Tip: `6277e067` · 2026-08-30T07:56:37-07:00. Commits not on main: **15**.

Replicates prepared requests with corrected cross-user preflight geometry and retained frozen inputs.

Subjects: “study: record prepared-request replication” · “experiment: repair cross-user preflight geometry” · “experiment: freeze prepared-request replication”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-30-prepared-request-replication.md`; `docs/observations/2026-08-30-grave-revisit-plan.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-30-prepared-request-replication.md:7`:

> **Decision:** The prepared-request routing effect is not a foundation for the product surface. The registered replication gate failed. Preserve the strong recovery/output signal as a separate hypothesis, not as rescue evidence for routing.

Checkout: `git fetch origin tag archive/experiment/prepared-request-replication-20260830 && git worktree add --detach <dir> archive/experiment/prepared-request-replication-20260830`

### archive/experiment/read-normalization-live-token-measurement

Branch: `experiment/read-normalization-live-token-measurement`. Tip: `abb70aea` · 2026-08-30T01:06:07-07:00. Commits not on main: **3**.

Measures the live wire cost of read normalization, separating timing variation from route equivalence.

Subjects: “bench: measure read normalization live wire cost” · “bench: ignore inspection timing in route equivalence” · “bench: freeze read normalization live-route measurement”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-30-read-normalization-live-route-token-measurement.md`; `docs/observations/2026-08-30-read-normalization-live-route-protocol.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-30-read-normalization-live-route-token-measurement.md:5`:

> Verdict: **measurement valid; semantic and route gates pass; the candidate is
> not a net token win for an ordinary short-lived MCP connection.** The new call
> shapes save 4 or 12 `o200k_base` proxy tokens per eligible request, but the
> `inspect_clojure` catalog grows by 317 tokens. At MCP wire startup, that growth
> requires 80 operation-less single calls or 27 omitted-ID two-request calls to
> break even. This receipt measures only. It does not authorize or perform an
> install.

Checkout: `git fetch origin tag archive/experiment/read-normalization-live-token-measurement && git worktree add --detach <dir> archive/experiment/read-normalization-live-token-measurement`

### archive/experiment/redesign-exemplars-20260830

Branch: `experiment/redesign-exemplars-20260830`. Tip: `a5d82674` · 2026-08-30T14:36:45-07:00. Commits not on main: **1**.

Develops redesigned examples for verbose structural transformation requests.

Subjects: “study: redesign transformation walls of text”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-30-transformation-redesign-exemplars.md`.

Recorded result (scope and candidate as quoted), `docs/observations/2026-08-30-transformation-redesign-exemplars.md:5`:

> The win is not a shorter spelling of `find` and `replace`. It is a closed JSON
> operation language in which the server owns the snapshot, labels, copied source,
> counts, and retry state, while the model emits only the delta.
> 
> Across the eight largest distinct transformation shapes, the honest outcome is
> bimodal:
> 
> - when the source already exists in a read or refused write, label-addressed
>   verbs or splices reduce one successful request by **10.8–24.0×**, and a retry
>   chain by **43.5–96.1×**;
> - when the source did not exist upstream, representation cannot repeal
>   authorship: the largest such wall falls only **1.96×**, from 28,537 bytes to
>   a 14,575-byte synthetic-twin request whose 13,923-byte decision dominates it.

Checkout: `git fetch origin tag archive/experiment/redesign-exemplars-20260830 && git worktree add --detach <dir> archive/experiment/redesign-exemplars-20260830`

### archive/experiment/rename-verb-screen-20260830

Branch: `experiment/rename-verb-screen-20260830`. Tip: `47aff4d1` · 2026-08-30T14:56:57-07:00. Commits not on main: **4**.

Tests a byte-efficient rename verb and retains its kill result and normalized evidence.

Subjects: “docs: normalize retained rename screen receipts” · “docs: retain rename verb screen kill result” · “bench: add byte-efficient rename verb screen”.

Added observation/plan documents (merge-base comparison): `docs/plans/2026-08-30-rename-verb-screen.md`; `docs/observations/2026-08-30-rename-verb-screen-results.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-30-rename-verb-screen-results.md:3`:

> **Decision:** KILL the proposed product surface after this screen.

Checkout: `git fetch origin tag archive/experiment/rename-verb-screen-20260830 && git worktree add --detach <dir> archive/experiment/rename-verb-screen-20260830`

### archive/experiment/result-decision-chord-screen

Branch: `experiment/result-decision-chord-screen`. Tip: `90b47d1b` · 2026-08-29T16:00:08-07:00. Commits not on main: **329**.

Screens result/decision interface relations with canonical effect scoring and isolated private server caches.

Subjects: “Score canonical relation effects independent of row order” · “Isolate private MCP server caches” · “Harden relation cohort runtime boundaries”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-29-result-decision-chord-capture-protocol.md`; `docs/observations/2026-08-29-three-arm-request-shape-model-screen-protocol.md`.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/experiment/result-decision-chord-screen && git worktree add --detach <dir> archive/experiment/result-decision-chord-screen`

### archive/experiment/routing-adoption-live-config-20260830

Branch: `experiment/routing-adoption-live-config-20260830`. Tip: `d66fcf3d` · 2026-08-30T09:25:30-07:00. Commits not on main: **9**.

Tests transfer of installed routing guidance and records the ceiling that stopped the screen.

Subjects: “experiment: stop routing transfer screen at ceiling” · “experiment: freeze installed-routing transfer screen” · “experiment: add installed-routing transfer screen”.

No added observation/plan Markdown documents in the merge-base diff.

Recorded result (scope and candidate as quoted), `bench/routing_tranche_20260830/screens/live-routing-guidance/result.md:13`:

> Control routed structural-first in **4/4** starts. All four runs were
> environment-valid, semantically exact, invalid-call-free, and
> wrong-subject-free. Walls were 15.608, 16.289, 18.344, and 21.734 seconds.
> 
> The preregistered pilot required at most 2/4 control structural-first so the
> treatment would have room to produce a measurable lift. The observed 4/4 is a
> ceiling stop. No treatment call or measured cohort ran.

Checkout: `git fetch origin tag archive/experiment/routing-adoption-live-config-20260830 && git worktree add --detach <dir> archive/experiment/routing-adoption-live-config-20260830`

### archive/experiment/routing-tranche-20260830

Branch: `experiment/routing-tranche-20260830`. Tip: `f8f95004` · 2026-08-30T08:39:37-07:00. Commits not on main: **6**.

Runs minimal-schema and refusal-handoff routing screens and retains the refusal-pilot ceiling repair.

Subjects: “experiment: publish refusal-handoff routing screen” · “experiment: freeze refusal pilot ceiling repair” · “experiment: publish minimal-schema routing screen”.

No added observation/plan Markdown documents in the merge-base diff.

Recorded result (scope and candidate as quoted), `bench/routing_tranche_20260830/screens/refusal-handoff/result.md:13`:

> In the 24-pair cohort, voluntary structural-first was 16/24 A and 19/24 B.
> Five B runs chose native first and received the non-mutating refusal. All 5/5
> transitioned immediately to `edit_clojure` and completed through that forced
> route: **100%**, above the frozen 70% kill threshold. End-to-end semantic
> success was 24/24 in both arms, a 0pp treatment drop versus the >10pp kill
> threshold. The final verdict is **clears-screen**.

Checkout: `git fetch origin tag archive/experiment/routing-tranche-20260830 && git worktree add --detach <dir> archive/experiment/routing-tranche-20260830`

### archive/experiment/spark-caller-screen-20260830

Branch: `experiment/spark-caller-screen-20260830`. Tip: `8510a9e2` · 2026-08-30T15:21:42-07:00. Commits not on main: **4**.

Measures Spark as a Surgeon caller and reconciles reported streaming rates with observed results.

Subjects: “docs: spark addendum — reported streaming rates, 1.31x reconciliation, convergence thesis” · “bench: characterize Spark as a Surgeon caller” · “bench: add catalog-discovered Spark identifier”.

Added observation/plan documents (merge-base comparison): `docs/plans/2026-08-30-gpt-5-3-spark-caller-screen.md`; `docs/observations/2026-08-30-gpt-5-3-spark-caller-screen.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-30-gpt-5-3-spark-caller-screen.md:3`:

> ## Verdict
> 
> `gpt-5.3-codex-spark` is reachable on the ChatGPT subscription route and is
> meaningfully faster than Sol on the trivial matched screen, but “very fast” is
> modest rather than magical at whole-turn scale: median wall was **3.954 s**
> versus **5.185 s** for `gpt-5.6-sol`, a **1.31×** Sol/Spark ratio or **23.7%**
> lower Spark wall.  Spark won all three pairs by 0.405, 1.574, and 1.478 seconds.

Checkout: `git fetch origin tag archive/experiment/spark-caller-screen-20260830 && git worktree add --detach <dir> archive/experiment/spark-caller-screen-20260830`

### archive/experiment/spark-isolation-screen-20260830

Branch: `experiment/spark-isolation-screen-20260830`. Tip: `3c2cc192` · 2026-08-30T16:57:10-07:00. Commits not on main: **3**.

Executes the Spark elaborator isolation screen and preserves its boundary evidence.

Subjects: “bench: prove Spark elaborator isolation boundary” · “docs: preregister Spark isolation screen” · “docs: probe embedded Spark app-server elaborator”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-30-spark-isolation-screen/RESULT.md`; `docs/observations/2026-08-30-spark-isolation-screen/DESIGN.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-30-spark-isolation-screen/RESULT.md:14`:

> ## Verdict: PASS, with a narrow production contract
> 
> The strongest tested OFF configuration, **H-S**, achieved zero model-driven
> actions across every frozen adversarial cell. All 15 H-S turns emitted only
> user, reasoning, and agent-message items. There were no command, file-change,
> MCP, dynamic-tool, app, web-search, image, process, or approval items; no new
> executor descendants; no canary reads or mutations; and no request to the
> harness-owned local HTTP canary. All 15 final values parsed and exactly matched
> the frozen `edit_clojure` candidate.

Checkout: `git fetch origin tag archive/experiment/spark-isolation-screen-20260830 && git worktree add --detach <dir> archive/experiment/spark-isolation-screen-20260830`

### archive/experiment/splice-adversarial-replication-20260830

Branch: `experiment/splice-adversarial-replication-20260830`. Tip: `b0432c25` · 2026-08-30T15:50:29-07:00. Commits not on main: **7**.

Adversarially replicates splice-reference trust against an immutable product and records the Spark stop.

Subjects: “experiment: adversarial splice trust screen finds Spark stop” · “experiment: bind replication to immutable product” · “experiment: preregister adversarial splice replication”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-30-splice-reference-adversarial-replication-result.md`; `docs/observations/2026-08-30-splice-reference-adversarial-replication-preregistration.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-30-splice-reference-adversarial-replication-result.md:5`:

> ## Verdict
> 
> **Overall product-trust verdict: KILL / HOLD. Do not promote splice-by-reference
> to product from this evidence.**

Checkout: `git fetch origin tag archive/experiment/splice-adversarial-replication-20260830 && git worktree add --detach <dir> archive/experiment/splice-adversarial-replication-20260830`

### archive/experiment/splice-reference-screen-20260830

Branch: `experiment/splice-reference-screen-20260830`. Tip: `588893fc` · 2026-08-30T14:50:41-07:00. Commits not on main: **4**.

Tests splice references on a synthetic screen while retaining invalid pilots and launcher repairs.

Subjects: “experiment: splice references survive synthetic screen” · “experiment: retain v2 handshake stop and freeze Java launch” · “experiment: preserve invalid pilot and freeze v2 launcher”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-30-splice-reference-screen-result.md`; `docs/observations/2026-08-30-splice-reference-screen-preregistration.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-30-splice-reference-screen-result.md:5`:

> ## Verdict
> 
> **The synthetic screen survives.** In the frozen 8/arm interleaved cohort,
> `gpt-5.6-sol` used snapshot-bound span references in every R attempt and
> reduced median mutation-request emission from **899 to 493 `o200k_base`
> tokens, −45.16%**. Canonical UTF-8 request bytes fell from **2,844 to 1,567,
> −44.90%**. Both arms completed exact expected bytes in 8/8 attempts. All 32 R
> span resolutions echoed readable identity; wrong-subject and typed reference
> refusals were both zero.

Checkout: `git fetch origin tag archive/experiment/splice-reference-screen-20260830 && git worktree add --detach <dir> archive/experiment/splice-reference-screen-20260830`

### archive/experiment/steering-ab-20260831

Branch: `experiment/steering-ab-20260831`. Tip: `af050cf5` · 2026-08-31T06:48:19-07:00. Commits not on main: **3**.

Runs a receipt-steering A/B comparison using a harness that holds the session open.

Subjects: “bench: seal receipt steering A/B evidence” · “bench: build session-held steering harness” · “bench: preregister receipt steering A/B”.

No added observation/plan Markdown documents in the merge-base diff.

Recorded result (scope and candidate as quoted), `bench/results/2026-08-31-steering-ab/report.md:5`:

> Primary: S 1/6; C 0/6; lift 16.667 percentage points.
> Safety gates: passed. Exactness S 6/6, C 6/6; wrong-subject 0.
> Preregistered <25pp kill: triggered.
> 
> The data-only receipt signal did not clear the frozen steering bar. It changed the prepared-route choice in one of six signal cells and zero of six controls, but that 16.7-point lift is below 25 points. No product promotion is supported.

Checkout: `git fetch origin tag archive/experiment/steering-ab-20260831 && git worktree add --detach <dir> archive/experiment/steering-ab-20260831`

### archive/experiment/submission-row-counterfactual

Branch: `experiment/submission-row-counterfactual`. Tip: `cd934115` · 2026-08-27T00:36:49-07:00. Commits not on main: **71**.

Preserves terminal-relay gate and publication work and closes the five-times-native usage window.

Subjects: “docs: close five-times-native usage window” · “Record live terminal relay publication” · “Promote paired terminal relay gate”.

Added observation/plan documents (merge-base comparison): `docs/plans/2026-08-26-five-times-native-next-12-hours.md`; `docs/observations/2026-08-27-agent-usage-release-window.md`.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/experiment/submission-row-counterfactual && git worktree add --detach <dir> archive/experiment/submission-row-counterfactual`

### archive/experiment/substantiation-overhead-20260830

Branch: `experiment/substantiation-overhead-20260830`. Tip: `7dcf1fab` · 2026-08-30T18:27:56-07:00. Commits not on main: **6**.

Measures the overhead of substantiation telemetry after call-identity and vocabulary repairs.

Subjects: “measure substantiation telemetry overhead” · “fix: bind substantiation call identity” · “fix: close substantiation nested vocabulary”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-30-substantiation-telemetry-overhead-screen.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-30-substantiation-telemetry-overhead-screen.md:4`:

> Verdict: **GO for the install decision gate only**  
> Product candidate: `de70e06fdc18f832b6774eabf81453ad4af9781f`  
> Candidate tree: `9ca1e9a0c3c8e6a71e79d72f364210b3cddbb6d1`

Checkout: `git fetch origin tag archive/experiment/substantiation-overhead-20260830 && git worktree add --detach <dir> archive/experiment/substantiation-overhead-20260830`

### archive/experiment/substantiation-overhead-w1-rebase-20260831

Branch: `experiment/substantiation-overhead-w1-rebase-20260831`. Tip: `3dde767a` · 2026-08-30T22:12:22-07:00. Commits not on main: **14**.

Measures W1-rebased telemetry overhead with formatted instrumentation and normalized session-bound parity.

Subjects: “experiment: seal W1 telemetry overhead receipt” · “bench: freeze formatted telemetry overhead instrument” · “bench: normalize session-bound telemetry parity”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-31-substantiation-telemetry-w1-rebase-overhead-screen.md`; `docs/observations/2026-08-31-substantiation-telemetry-w1-rebase-overhead-protocol.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-31-substantiation-telemetry-w1-rebase-overhead-screen.md:5`:

> Verdict: **GO to the telemetry breakfast/install card only.** Exact candidate
> `4e2cf27b2226997508356ac5ecbdeaed18d8132c`, tree
> `4c7265c660ce6e698e0dbff45d4e697fe05994e2`, remains inside every frozen
> MCP-OP-SUBST-018 overhead bound after the W1 rebase. This receipt does not
> authorize installation, reload, publication, or shared-runtime action.

Checkout: `git fetch origin tag archive/experiment/substantiation-overhead-w1-rebase-20260831 && git worktree add --detach <dir> archive/experiment/substantiation-overhead-w1-rebase-20260831`

### archive/experiment/three-arm-request-shape-prereq

Branch: `experiment/three-arm-request-shape-prereq`. Tip: `1b8c28cb` · 2026-08-29T01:41:20-07:00. Commits not on main: **251**.

Freezes the three-arm request-shape prerequisites and binds candidates to a complete server authority envelope.

Subjects: “docs: freeze authority-complete three-arm receipt” · “bench: close candidates in server authority envelope” · “bench: preserve candidate closure in client schema”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-29-three-arm-request-shape-zero-model-receipt.md`; `docs/observations/2026-08-29-three-arm-request-shape-model-screen-protocol.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-29-three-arm-request-shape-zero-model-receipt.md:177`:

> ## Verdict
> 
> **GO** for exactly six fresh, serial Sol/high capture-only runs in this order:

Checkout: `git fetch origin tag archive/experiment/three-arm-request-shape-prereq && git worktree add --detach <dir> archive/experiment/three-arm-request-shape-prereq`

### archive/experiment/turn-waste-mining-20260830

Branch: `experiment/turn-waste-mining-20260830`. Tip: `62269e7a` · 2026-08-30T08:18:22-07:00. Commits not on main: **2**.

Mines caller turns for avoidable work and records the preregistered study verdicts.

Subjects: “docs: report turn-waste mining verdicts” · “docs: preregister turn-waste mining studies”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-30-turn-waste-mining.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-30-turn-waste-mining.md:166`:

> ### Executive verdict
> 
> All three hypotheses are **KILL** under their preregistered gates. No PASS
> exists, so the ranked build-recommendation set is empty.

Checkout: `git fetch origin tag archive/experiment/turn-waste-mining-20260830 && git worktree add --detach <dir> archive/experiment/turn-waste-mining-20260830`

### archive/experiment/verb-algebra-census-20260830

Branch: `experiment/verb-algebra-census-20260830`. Tip: `abaf7e65` · 2026-08-30T14:39:10-07:00. Commits not on main: **2**.

Censuses which transformations the verb algebra can express.

Subjects: “study: census transform verb expressibility” · “experiment: preregister transform verb census”.

Added observation/plan documents (merge-base comparison): `docs/observations/evidence/verb-algebra-census-20260830/preregistration.md`; `docs/observations/2026-08-30-transform-verb-expressibility-census.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-30-transform-verb-expressibility-census.md:3`:

> ## Verdict
> 
> Gene's hypothesis is directionally right and product-incomplete.

Checkout: `git fetch origin tag archive/experiment/verb-algebra-census-20260830 && git worktree add --detach <dir> archive/experiment/verb-algebra-census-20260830`

### archive/experiment/walls-of-text-exhibit-20260830

Branch: `experiment/walls-of-text-exhibit-20260830`. Tip: `5d3834e7` · 2026-08-30T14:30:32-07:00. Commits not on main: **1**.

Preserves examples of oversized structural-write requests for redesign analysis.

Subjects: “study: exhibit walls of text in structural writes”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-30-walls-of-text-exhibit.md`.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/experiment/walls-of-text-exhibit-20260830 && git worktree add --detach <dir> archive/experiment/walls-of-text-exhibit-20260830`

### archive/experiment/warm-executor-screen-20260830

Branch: `experiment/warm-executor-screen-20260830`. Tip: `9b6c9708` · 2026-08-30T15:56:39-07:00. Commits not on main: **4**.

Screens warm-executor economics and retains the Spark cold-bang replacement and harness recovery.

Subjects: “Publish warm-executor economics screen” · “Register Spark cold-bang replacement” · “Freeze warm-executor harness recovery”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-30-warm-executor-screen-replacement-addendum.md`; `docs/observations/2026-08-30-warm-executor-screen-recovery-addendum.md`.

Recorded result (scope and candidate as quoted), `docs/observations/2026-08-30-captains-log-warm-executor-paid-for-itself.md:15`:

> The registered winning pattern passed.
> 
> One live Spark app-server and one live Spark thread cut the median prepared-edit
> round trip from **6.773 seconds cold to 2.288 seconds warm**, a **4.486-second
> (66.2%) reduction**. The ten-turn warm stream was 10/10 exact, 10/10 one-shot,
> and 0 wrong-subject. Its first five and last five were both 5/5 exact, so there
> was no registered quality-drift signal.
> 
> Sol preserved the same perfect quality. Its median prepared edit fell from
> **10.088 seconds cold to 6.350 seconds warm**, a **3.738-second (37.1%)
> reduction**. Warm Spark was 64.0% lower latency than warm Sol on this fixture.
> 
> This is a screen on one local machine, one subscription account, one CLI
> version, one exact-edit task family, and a short serial schedule. It earns the
> executor pattern for the next integration experiment; it is not a universal

Checkout: `git fetch origin tag archive/experiment/warm-executor-screen-20260830 && git worktree add --detach <dir> archive/experiment/warm-executor-screen-20260830`

### archive/experiment/write-refusal-001-live-token-measurement

Branch: `experiment/write-refusal-001-live-token-measurement`. Tip: `89d38197` · 2026-08-30T15:54:34-07:00. Commits not on main: **5**.

Measures the live route price of complete write refusals using exact fixtures and normalized receipts.

Subjects: “bench: retain exact write refusal fixtures” · “bench: measure write refusal live route price” · “bench: normalize per-run refusal receipts”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-30-write-refusal-001-live-route-token-measurement.md`; `docs/observations/2026-08-30-write-refusal-001-live-route-protocol.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-30-write-refusal-001-live-route-token-measurement.md:5`:

> Verdict: **GO to Gene's install decision, not an install or performance
> claim.** Exact candidate `9af88fba` adds no catalog or request cost and leaves
> ordinary success behavior unchanged after named dynamic receipt fields are
> excluded. On the realistic 27-owner refusal it adds 5,241 response bytes and
> 1,656 `o200k_base` input tokens so the caller receives the complete known
> match universe. Whether that evidence removes a recovery turn for this write
> family is unmeasured.

Checkout: `git fetch origin tag archive/experiment/write-refusal-001-live-token-measurement && git worktree add --detach <dir> archive/experiment/write-refusal-001-live-token-measurement`

## audit/* — 16 tags

The late-August audit branches hold independent checks of eligibility tests, prepared confirmation, session affinity, telemetry, and refusal completeness. GO and NO-GO quotes apply to the named historical candidate and review scope.

### archive/audit/adoption-gap-attribution-20260830

Branch: `audit/adoption-gap-attribution-20260830`. Tip: `628e6d18` · 2026-08-30T09:39:01-07:00. Commits not on main: **5**.

Attributes the external adoption gap using a census of actual Clojure write shapes and distinguishes eligibility from refusal and failure.

Subjects: “study: attribute external adoption gap” · “experiment: census external Clojure write shapes” · “research: preregister external corpus shape census”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-30-adoption-gap-attribution.md`; `docs/observations/2026-08-30-external-corpus-write-shape-census.md`.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/audit/adoption-gap-attribution-20260830 && git worktree add --detach <dir> archive/audit/adoption-gap-attribution-20260830`

### archive/audit/eligibility-characterization-20260831

Branch: `audit/eligibility-characterization-20260831`. Tip: `c5fed623` · 2026-08-30T22:05:14-07:00. Commits not on main: **2**.

Audits the first eligibility characterization suite and identifies why its apparent green result is insufficient release evidence.

Subjects: “docs: audit eligibility characterization cargo” · “test: eligibility characterization suite — 13 cases against project-result”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-31-prepared-request-eligibility-characterization-audit.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-31-prepared-request-eligibility-characterization-audit.md:5`:

> Verdict: **NO-GO as release-train test cargo; product behavior remains green**

Checkout: `git fetch origin tag archive/audit/eligibility-characterization-20260831 && git worktree add --detach <dir> archive/audit/eligibility-characterization-20260831`

### archive/audit/eligibility-characterization-fourth-audit-20260831

Branch: `audit/eligibility-characterization-fourth-audit-20260831`. Tip: `1386f8df` · 2026-08-30T22:17:30-07:00. Commits not on main: **6**.

Reviews the fourth eligibility-test candidate after the missing mutation cases and namespace loading were repaired.

Subjects: “audit: approve eligibility characterization fourth candidate” · “test: ns-form require for explainer — fixes runtime-require alias error” · “test: complete general agreement — all 12 mutations incl. seven-form”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-31-prepared-request-eligibility-fourth-audit.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-31-prepared-request-eligibility-fourth-audit.md:3`:

> ## Verdict
> 
> GO for test cargo at exact candidate
> `5573f3a803c0b0061e0532dd2944ed797b7bf8c2` (tree recorded below).

Checkout: `git fetch origin tag archive/audit/eligibility-characterization-fourth-audit-20260831 && git worktree add --detach <dir> archive/audit/eligibility-characterization-fourth-audit-20260831`

### archive/audit/eligibility-characterization-reaudit-20260831

Branch: `audit/eligibility-characterization-reaudit-20260831`. Tip: `394a262e` · 2026-08-30T22:09:11-07:00. Commits not on main: **3**.

Reaudits the repaired eligibility tests and records the remaining blockers.

Subjects: “docs: reaudit eligibility characterization successor” · “test: repair characterization suite per micro-verify NO-GO” · “test: eligibility characterization suite — 13 cases against project-result”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-31-prepared-request-eligibility-successor-reaudit.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-31-prepared-request-eligibility-successor-reaudit.md:5`:

> Verdict: **NO-GO as release-train test cargo; two prior blockers closed, two remain**

Checkout: `git fetch origin tag archive/audit/eligibility-characterization-reaudit-20260831 && git worktree add --detach <dir> archive/audit/eligibility-characterization-reaudit-20260831`

### archive/audit/eligibility-characterization-third-audit-20260831

Branch: `audit/eligibility-characterization-third-audit-20260831`. Tip: `ff7d17a6` · 2026-08-30T22:11:56-07:00. Commits not on main: **4**.

Checks the third eligibility-test candidate, including hermetic explainer binding and the still-missing agreement row.

Subjects: “docs: third-audit eligibility characterization” · “test: hermetic explainer binding per re-audit NO-GO” · “test: repair characterization suite per micro-verify NO-GO”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-31-prepared-request-eligibility-third-audit.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-31-prepared-request-eligibility-third-audit.md:5`:

> Verdict: **NO-GO as release-train test cargo; one agreement row remains missing**

Checkout: `git fetch origin tag archive/audit/eligibility-characterization-third-audit-20260831 && git worktree add --detach <dir> archive/audit/eligibility-characterization-third-audit-20260831`

### archive/audit/eligibility-legibility-refactor-20260831

Branch: `audit/eligibility-legibility-refactor-20260831`. Tip: `ecae7b10` · 2026-08-31T05:37:40-07:00. Commits not on main: **7**.

Independently checks the eligible-descriptor refactor into named, short-circuiting checks.

Subjects: “audit: verify eligible descriptor short-circuit refactor” · “refactor: eligible-descriptor as named short-circuiting checks” · “test: ns-form require for explainer — fixes runtime-require alias error”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-31-eligible-descriptor-legibility-refactor-audit.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-31-eligible-descriptor-legibility-refactor-audit.md:3`:

> ## Verdict
> 
> GO for staged product-code cargo at exact candidate
> `47d48390a673b68736593ed0b9e76395b08baf79`.

Checkout: `git fetch origin tag archive/audit/eligibility-legibility-refactor-20260831 && git worktree add --detach <dir> archive/audit/eligibility-legibility-refactor-20260831`

### archive/audit/prepared-confirm-affinity-7e0300fe-20260831

Branch: `audit/prepared-confirm-affinity-7e0300fe-20260831`. Tip: `9529f700` · 2026-08-30T21:47:44-07:00. Commits not on main: **7**.

Verifies the repair that makes prepared-confirmation session-affinity refusals actionable.

Subjects: “docs: independently verify prepared affinity repair” · “fix: make prepared confirmation affinity actionable” · “test: freeze prepared confirmation affinity repair red”.

Added observation/plan documents (merge-base comparison): `docs/plans/prepared-confirm-affinity-guidance-frozen-red-20260831.md`; `docs/observations/2026-08-31-prepared-confirmation-affinity-repair.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-31-prepared-confirmation-affinity-independent-go.md:3`:

> ## Verdict
> 
> GO for the ratified W1 session-affinity caller repair at exact candidate
> `7e0300fe0a75623fa6d7f275d2b99b57aa34f26d`, tree
> `1cfae15edb7ab167ce3c86f8157e8b0338c90790`.

Checkout: `git fetch origin tag archive/audit/prepared-confirm-affinity-7e0300fe-20260831 && git worktree add --detach <dir> archive/audit/prepared-confirm-affinity-7e0300fe-20260831`

### archive/audit/prepared-confirm-preview-05f5a196-20260830

Branch: `audit/prepared-confirm-preview-05f5a196-20260830`. Tip: `7370c966` · 2026-08-30T20:22:33-07:00. Commits not on main: **1**.

Records acceptance of the repaired prepared-confirmation public wire.

Subjects: “audit: accept repaired W1 public wire”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-30-prepared-confirm-preview-repaired-successor-independent-go.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-30-prepared-confirm-preview-repaired-successor-independent-go.md:5`:

> ## Verdict
> 
> **GO** for the bounded live-route measurement and install-card preparation of
> exact candidate `05f5a1962e5a0c5aa0365c673994eca9024c1a44` (tree
> `7cb0f58bdc4d8469d1f7757b0f0ee65e61f4fdc1`). This is not install authority;
> Gene must explicitly approve this exact candidate before publication.

Checkout: `git fetch origin tag archive/audit/prepared-confirm-preview-05f5a196-20260830 && git worktree add --detach <dir> archive/audit/prepared-confirm-preview-05f5a196-20260830`

### archive/audit/prepared-confirm-preview-90486da5-20260830

Branch: `audit/prepared-confirm-preview-90486da5-20260830`. Tip: `11791c75` · 2026-08-30T18:36:27-07:00. Commits not on main: **0**.

Records the public-wire normalization defect that invalidated the first prepared-confirmation green result.

Subjects (tip history; no commits outside main): “audit: block W1 wire-normalization false green” · “feat: add prepared confirm and preview” · “test: freeze prepared confirm and preview red”.

No added observation/plan Markdown documents in the merge-base diff.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-30-prepared-confirm-preview-independent-no-go.md:5`:

> ## Verdict
> 
> **NO-GO** for measurement, installation, publication, or promotion of exact
> candidate `90486da5d4d2d4ed7a6efed443142105a2b41d2d` (tree
> `5775162523df3008aa03e5698a08c590293465af`).

Checkout: `git fetch origin tag archive/audit/prepared-confirm-preview-90486da5-20260830 && git worktree add --detach <dir> archive/audit/prepared-confirm-preview-90486da5-20260830`

### archive/audit/prepared-confirm-session-affinity-20260831

Branch: `audit/prepared-confirm-session-affinity-20260831`. Tip: `c8b07168` · 2026-08-30T20:51:22-07:00. Commits not on main: **2**.

Exposes the session-affinity limit of prepared confirmation after its public-wire repair.

Subjects: “audit: expose W1 session-affinity limit” · “audit: accept repaired W1 public wire”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-31-prepared-confirmation-session-affinity-field-audit.md`; `docs/observations/2026-08-30-prepared-confirm-preview-repaired-successor-independent-go.md`.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/audit/prepared-confirm-session-affinity-20260831 && git worktree add --detach <dir> archive/audit/prepared-confirm-session-affinity-20260831`

### archive/audit/substantiation-telemetry-independent-20260830

Branch: `audit/substantiation-telemetry-independent-20260830`. Tip: `316a564d` · 2026-08-30T17:44:03-07:00. Commits not on main: **4**.

Independently audits substantiation telemetry and rejects incomplete validation.

Subjects: “docs: reject incomplete substantiation validation” · “feat: add substantiation telemetry ledger” · “test: freeze substantiation telemetry red”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-30-substantiation-telemetry-independent-no-go.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-30-substantiation-telemetry-independent-no-go.md:5`:

> ## Verdict
> 
> Exact candidate
> `17849c7172ad8079ceae768c0f15df795ac04e84` is **NO-GO** for the
> pre-install overhead measurement and publication gate.

Checkout: `git fetch origin tag archive/audit/substantiation-telemetry-independent-20260830 && git worktree add --detach <dir> archive/audit/substantiation-telemetry-independent-20260830`

### archive/audit/substantiation-telemetry-second-successor-20260830

Branch: `audit/substantiation-telemetry-second-successor-20260830`. Tip: `58dce20c` · 2026-08-30T18:23:42-07:00. Commits not on main: **7**.

Accepts the second telemetry successor after call-identity binding and ties it to the W1 witness.

Subjects: “docs: bind telemetry to W1 rebase witness” · “docs: accept telemetry second successor” · “fix: bind substantiation call identity”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-30-substantiation-telemetry-second-successor-independent-go.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-30-substantiation-telemetry-second-successor-independent-go.md:5`:

> ## Verdict
> 
> Exact successor
> `de70e06fdc18f832b6774eabf81453ad4af9781f` is **GO** for the frozen
> pre-install overhead measurement in MCP-OP-SUBST-018.

Checkout: `git fetch origin tag archive/audit/substantiation-telemetry-second-successor-20260830 && git worktree add --detach <dir> archive/audit/substantiation-telemetry-second-successor-20260830`

### archive/audit/substantiation-telemetry-successor-20260830

Branch: `audit/substantiation-telemetry-successor-20260830`. Tip: `01529dbf` · 2026-08-30T17:55:28-07:00. Commits not on main: **5**.

Reviews the first telemetry successor after closing nested vocabulary and records why validation remains incomplete.

Subjects: “docs: reject incomplete telemetry successor” · “fix: close substantiation nested vocabulary” · “feat: add substantiation telemetry ledger”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-30-substantiation-telemetry-successor-independent-no-go.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-30-substantiation-telemetry-successor-independent-no-go.md:5`:

> ## Verdict
> 
> Exact successor
> `143d6fbbad22d366bf945b593e074a2ebb57d0bc` is **NO-GO** for the
> overhead measurement and publication gate.

Checkout: `git fetch origin tag archive/audit/substantiation-telemetry-successor-20260830 && git worktree add --detach <dir> archive/audit/substantiation-telemetry-successor-20260830`

### archive/audit/substantiation-telemetry-w1-rebase-20260831

Branch: `audit/substantiation-telemetry-w1-rebase-20260831`. Tip: `fe45faad` · 2026-08-30T22:00:03-07:00. Commits not on main: **9**.

Audits telemetry rebased onto prepared confirmations and its W1 integration witness.

Subjects: “docs: audit telemetry W1 rebase” · “test: load substantiation W1 witness” · “feat: bind telemetry to prepared confirmations”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-31-substantiation-telemetry-w1-rebase-audit.md`; `docs/plans/substantiation-w1-cross-feature-witness-20260830.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-31-substantiation-telemetry-w1-rebase-audit.md:5`:

> Verdict: **GO for the implementation candidate; no install or runtime authority**

Checkout: `git fetch origin tag archive/audit/substantiation-telemetry-w1-rebase-20260831 && git worktree add --detach <dir> archive/audit/substantiation-telemetry-w1-rebase-20260831`

### archive/audit/write-refusal-001-independent-20260830

Branch: `audit/write-refusal-001-independent-20260830`. Tip: `29e417c6` · 2026-08-30T15:44:19-07:00. Commits not on main: **1**.

Independently verifies the write-refusal completeness change.

Subjects: “docs: verify write refusal completeness”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-30-write-refusal-001-independent-verification.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-30-write-refusal-001-independent-verification.md:5`:

> ## Verdict
> 
> **GO** for the implementation half at exact product candidate
> `9af88fbae9ee720613599feaf8cf58432c5898bb`, tree
> `6f9bc30316eb6417977c07c86caf8eb146dfbdb8`.

Checkout: `git fetch origin tag archive/audit/write-refusal-001-independent-20260830 && git worktree add --detach <dir> archive/audit/write-refusal-001-independent-20260830`

### archive/audit/write-refusal-completeness-20260830

Branch: `audit/write-refusal-completeness-20260830`. Tip: `e418c851` · 2026-08-30T07:37:02-07:00. Commits not on main: **1**.

Audits the completeness of write refusals and their recovery guidance.

Subjects: “docs: audit write refusal completeness”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-30-write-side-refusal-completeness-audit.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-30-write-side-refusal-completeness-audit.md:8`:

> Verdict: **yes — the write side has the same defect class.**

Checkout: `git fetch origin tag archive/audit/write-refusal-completeness-20260830 && git worktree add --detach <dir> archive/audit/write-refusal-completeness-20260830`

## docs/* — 22 tags

The late-August documentation branches preserve design and ratification packets, captain’s logs, economic explanations, postmortems, and evidence audits. A design-only branch may have no measured outcome of its own.

### archive/docs/acejump-designs-20260830

Branch: `docs/acejump-designs-20260830`. Tip: `9c57fa71` · 2026-08-30T14:02:33-07:00. Commits not on main: **4**.

Designs label-addressed write shortcuts alongside the prepared-request route-cost evidence.

Subjects: “docs: design label-addressed write shortcuts” · “bench: seal prepared request live route cost” · “bench: compare paid path by JSON semantics”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-30-label-addressed-write-designs.md`; `docs/observations/2026-08-30-prepared-request-live-route-token-measurement.md`.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/docs/acejump-designs-20260830 && git worktree add --detach <dir> archive/docs/acejump-designs-20260830`

### archive/docs/acejump-postmortem-20260830

Branch: `docs/acejump-postmortem-20260830`. Tip: `a78c6609` · 2026-08-30T14:08:41-07:00. Commits not on main: **1**.

Records the postmortem of the AceJump label-addressing proposals.

Subjects: “docs: postmortem AceJump label proposals”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-30-acejump-docs-postmortem.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-30-acejump-docs-postmortem.md:9`:

> ## Verdict
> 
> The AceJump metaphor contained one durable product insight and one bad product conclusion.

Checkout: `git fetch origin tag archive/docs/acejump-postmortem-20260830 && git worktree add --detach <dir> archive/docs/acejump-postmortem-20260830`

### archive/docs/acid-test-doctrine-20260830

Branch: `docs/acid-test-doctrine-20260830`. Tip: `a4d7feb1` · 2026-08-30T10:58:06-07:00. Commits not on main: **2**.

Defines the native-control acid test and an experiment-design skill for judging performance claims.

Subjects: “skill: designing experiments like a scientist” · “doctrine: the acid test — the ultimate measure of performance”.

No added observation/plan Markdown documents in the merge-base diff.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/docs/acid-test-doctrine-20260830 && git worktree add --detach <dir> archive/docs/acid-test-doctrine-20260830`

### archive/docs/captains-log-2026-08-30-install-night

Branch: `docs/captains-log-2026-08-30-install-night`. Tip: `74190f68` · 2026-08-31T00:44:24-07:00. Commits not on main: **9**.

Preserves the install-night captain's logs, verifier work, and fast-elaborator comparison observations.

Subjects: “docs: captain's log — the nine-hour watch” · “docs: OpenRouter fast-elaborator shootout — sub-second bangs on oss-120b@Cerebras” · “docs: captain's log — the night the verifiers ran the ship”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-30-captains-log-the-night-the-verifiers-ran-the-ship.md`; `docs/observations/2026-08-30-captains-log-the-night-the-arrow-walked-itself.md`.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/docs/captains-log-2026-08-30-install-night && git worktree add --detach <dir> archive/docs/captains-log-2026-08-30-install-night`

### archive/docs/captains-logs-2026-08-29

Branch: `docs/captains-logs-2026-08-29`. Tip: `8ac7cde9` · 2026-08-29T23:40:00-07:00. Commits not on main: **11**.

Preserves the August 29 captain's logs, failed predictions, corrected figures, and the plan to revisit single-corpus kills.

Subjects: “grave-revisit plan: kills are single-corpus; one census stress-tests Grade A” · “docs: seventh withdrawal — a wrong number was inside the corrections section” · “docs: captain's log — the predictions that failed, and what that teaches”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-30-grave-revisit-plan.md`; `docs/observations/2026-08-29-measurements-and-how-to-repeat-them.md`.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/docs/captains-logs-2026-08-29 && git worktree add --detach <dir> archive/docs/captains-logs-2026-08-29`

### archive/docs/conn-review-20260830

Branch: `docs/conn-review-20260830`. Tip: `2f391902` · 2026-08-30T17:55:42-07:00. Commits not on main: **7**.

Adversarially reviews the overnight Conn rulings and records the caller's proposed direction.

Subjects: “docs: adversarially review Conn overnight rulings” · “docs: the dream session — caller #1's north star” · “docs: captain's log — a second mind in the server”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-30-conn-rulings-adversarial-review.md`; `docs/observations/2026-08-30-the-dream-session-north-star.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-30-conn-rulings-adversarial-review.md:13`:

> ## Executive verdict
> 
> | Ruling | Verdict | Replacement |
> |---|---|---|
> | R1 landing order | **OVERTURN** | Parallel candidate work and provisional greens; a dependency-aware merge queue; one candidate at a time only for final publication; no convoy behind a NO-GO head. |
> | R2 install policy | **OVERTURN** | No risk-class install inference. Every shared-runtime candidate needs its exact technical gates and explicit Gene authorization for install/reload. |
> | R3 build priority | **OVERTURN** | Finish and measure W1 first, judge W2 separately, admit elaborator D1 only on naturally eligible work, keep splice in research, unbundle the dreamlist, and withhold F4 until it has evidence and a packet. |
> | R4 North Star | **OVERTURN** as program target | Retain the transcript as one named-owner Clojure happy-path storyboard. Make the program target an outcome scorecard over real tasks, costs, failures, and contention. |
> | R5 scope readings | **AMEND** | Bind every terse approval to its exact antecedent, artifact, and phase. Preserve evidence gates. Restore splice to product HOLD; the receipt does not authorize a “Sol-class-only” product. |

Checkout: `git fetch origin tag archive/docs/conn-review-20260830 && git worktree add --detach <dir> archive/docs/conn-review-20260830`

### archive/docs/dreamlist-designs-20260830

Branch: `docs/dreamlist-designs-20260830`. Tip: `7c937a40` · 2026-08-30T17:17:14-07:00. Commits not on main: **1**.

Designs the caller dream-list features and their intended contracts.

Subjects: “docs: design caller dream-list fulfillment”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-30-dream-list-fulfillment-designs.md`.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/docs/dreamlist-designs-20260830 && git worktree add --detach <dir> archive/docs/dreamlist-designs-20260830`

### archive/docs/embedded-elaborator-ratification-20260830

Branch: `docs/embedded-elaborator-ratification-20260830`. Tip: `eaba46b2` · 2026-08-30T16:53:39-07:00. Commits not on main: **3**.

Ratifies the embedded Spark elaborator and specifies warming it at MCP boot.

Subjects: “docs: warm elaborator at MCP boot” · “docs: ratify embedded Spark elaborator” · “docs: seal write refusal install receipt”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-30-write-refusal-001-installed-release.md`.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/docs/embedded-elaborator-ratification-20260830 && git worktree add --detach <dir> archive/docs/embedded-elaborator-ratification-20260830`

### archive/docs/fast-typist-designs-20260831

Branch: `docs/fast-typist-designs-20260831`. Tip: `9fc31104` · 2026-08-30T20:06:50-07:00. Commits not on main: **1**.

Designs ways to use a fast REPL typist.

Subjects: “docs: design fast REPL typist utilization patterns”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-31-fast-typist-utilization-designs.md`.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/docs/fast-typist-designs-20260831 && git worktree add --detach <dir> archive/docs/fast-typist-designs-20260831`

### archive/docs/fleet-opines-20260831

Branch: `docs/fleet-opines-20260831`. Tip: `f261fa96` · 2026-08-31T05:42:01-07:00. Commits not on main: **1**.

Records fleet opinions on the proposed tool experience.

Subjects: “docs: let the fleet opine on the magic”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-31-the-fleet-opines-on-the-magic.md`.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/docs/fleet-opines-20260831 && git worktree add --detach <dir> archive/docs/fleet-opines-20260831`

### archive/docs/grave-revisit-update-20260830

Branch: `docs/grave-revisit-update-20260830`. Tip: `f9eafb3c` · 2026-08-30T07:21:44-07:00. Commits not on main: **1**.

Updates the plan to revisit previously rejected approaches with the latest sweep results.

Subjects: “docs: update grave revisit with sweep results”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-30-grave-revisit-plan.md`.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/docs/grave-revisit-update-20260830 && git worktree add --detach <dir> archive/docs/grave-revisit-update-20260830`

### archive/docs/prepared-confirm-preview-ratification-20260830

Branch: `docs/prepared-confirm-preview-ratification-20260830`. Tip: `714cadab` · 2026-08-30T17:11:19-07:00. Commits not on main: **2**.

Ratifies the prepared-confirmation and preview design packet.

Subjects: “docs: ratify prepared confirm and preview packet” · “docs: seal write refusal install receipt”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-30-write-refusal-001-installed-release.md`.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/docs/prepared-confirm-preview-ratification-20260830 && git worktree add --detach <dir> archive/docs/prepared-confirm-preview-ratification-20260830`

### archive/docs/prepared-request-ratification-20260830

Branch: `docs/prepared-request-ratification-20260830`. Tip: `0228fbe8` · 2026-08-30T07:34:31-07:00. Commits not on main: **1**.

Prepares the guarded prepared-request ratification packet.

Subjects: “docs: prepare guarded request ratification packet”.

No added observation/plan Markdown documents in the merge-base diff.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/docs/prepared-request-ratification-20260830 && git worktree add --detach <dir> archive/docs/prepared-request-ratification-20260830`

### archive/docs/prepared-request-recovery-lld-20260830

Branch: `docs/prepared-request-recovery-lld-20260830`. Tip: `050f9ea0` · 2026-08-30T10:56:10-07:00. Commits not on main: **12**.

Completes the prepared-request recovery design and separates proxy safety from route adherence in the retained verdict.

Subjects: “Complete prepared request recovery LLD” · “Record prepared request proxy verdict” · “Separate prepared proxy safety from route adherence”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-30-prepared-request-option-a-proxy-valid-verdict.md`; `docs/observations/2026-08-30-prepared-request-option-a-proxy-stdin-stop.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-30-prepared-request-option-a-proxy-valid-verdict.md:13`:

> ## Verdict
> 
> The cohort is valid. Option A **fails the registered routing gate** and does not advance to the
> leaf LLD, EARS requirements, red tests, or product code.

Checkout: `git fetch origin tag archive/docs/prepared-request-recovery-lld-20260830 && git worktree add --detach <dir> archive/docs/prepared-request-recovery-lld-20260830`

### archive/docs/readme-economics-20260830

Branch: `docs/readme-economics-20260830`. Tip: `748bae34` · 2026-08-30T10:06:13-07:00. Commits not on main: **1**.

Explains the tool's economics and measured operating boundary in the README.

Subjects: “docs: explain Surgeon economics and measured boundary”.

No added observation/plan Markdown documents in the merge-base diff.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/docs/readme-economics-20260830 && git worktree add --detach <dir> archive/docs/readme-economics-20260830`

### archive/docs/speed-confirm-20260831

Branch: `docs/speed-confirm-20260831`. Tip: `01d96478` · 2026-08-31T06:10:47-07:00. Commits not on main: **1**.

Independently audits the overnight speed claims.

Subjects: “docs: independently audit overnight speed claims”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-31-speed-claims-confirmation-audit.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-31-speed-claims-confirmation-audit.md:5`:

> ## Verdict
> 
> The low-level performance story is real, but the receipts do not support every
> headline exactly as stated. Four claims survive with bounded caveats, one
> contains two separately confirmed measurements, and the nominated receipt for
> the exact `or-bang` range does not support that range. The audit treats Git
> commits, machine receipts, and raw trial rows as evidence; commit messages and
> the conn's prose are not evidence by themselves.

Checkout: `git fetch origin tag archive/docs/speed-confirm-20260831 && git worktree add --detach <dir> archive/docs/speed-confirm-20260831`

### archive/docs/splice-reference-ratification-20260830

Branch: `docs/splice-reference-ratification-20260830`. Tip: `f5504e68` · 2026-08-30T15:20:46-07:00. Commits not on main: **1**.

Proposes trusted splice references and their intended authority contract.

Subjects: “docs: propose trusted splice references”.

No added observation/plan Markdown documents in the merge-base diff.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/docs/splice-reference-ratification-20260830 && git worktree add --detach <dir> archive/docs/splice-reference-ratification-20260830`

### archive/docs/substantiation-telemetry-ratification-20260830

Branch: `docs/substantiation-telemetry-ratification-20260830`. Tip: `de70e06f` · 2026-08-30T18:01:20-07:00. Commits not on main: **5**.

Carries substantiation-telemetry ratification and the implementation repairs for nested vocabulary and call identity.

Subjects: “fix: bind substantiation call identity” · “fix: close substantiation nested vocabulary” · “feat: add substantiation telemetry ledger”.

No added observation/plan Markdown documents in the merge-base diff.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/docs/substantiation-telemetry-ratification-20260830 && git worktree add --detach <dir> archive/docs/substantiation-telemetry-ratification-20260830`

### archive/docs/sweep-lane1-complete-refusal-ab

Branch: `docs/sweep-lane1-complete-refusal-ab`. Tip: `c1e89d5d` · 2026-08-30T00:39:47-07:00. Commits not on main: **16**.

Records the first sweep lane's instrument defect, preregistered repair, and repaired refusal cohort.

Subjects: “docs: record sweep lane 1 audit defect” · “research: record sweep lane 1 repair cohort” · “research: preregister sweep lane 1 instrument repair”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-29-complete-owner-vocabulary-causal-ab.md`; `docs/observations/2026-08-30-grave-revisit-plan.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-29-complete-owner-vocabulary-causal-ab.md:5`:

> Status: **repair cohort complete; frozen causal screen passes**

Checkout: `git fetch origin tag archive/docs/sweep-lane1-complete-refusal-ab && git worktree add --detach <dir> archive/docs/sweep-lane1-complete-refusal-ab`

### archive/docs/transcript-audit-20260831

Branch: `docs/transcript-audit-20260831`. Tip: `6fd50f5b` · 2026-08-31T06:13:03-07:00. Commits not on main: **1**.

Audits what caller transcripts emit.

Subjects: “docs: audit caller transcript emissions”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-31-caller-transcript-emission-audit.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-31-caller-transcript-emission-audit.md:7`:

> ## Executive verdict
> 
> | Claim | Verdict | Transcript evidence |
> |---|---|---|
> | Refactor #1 emitted about 4.3 KB and paid the `from` tax. | **CAVEAT** | The tax is confirmed: `from` was 1,838 B and `to` was 2,946 B. The complete compact tool-input JSON was 5,063 B, however, not about 4.3 KB. |
> | Refactor #2 emitted roughly half as much because it sent `to` plus ceremony but no `from`. | **UNSUPPORTED as phrased** | The heredoc `to` body was 2,264 B, which is 47.32% of #1's 4,784 B old-plus-new source subtotal. But the complete command was 4,276 B because ceremony added 2,012 B. That is 84.46% of #1's complete 5,063 B input, not roughly half. |
> | Caller-turn walls are consistent with about 60 s versus 25--35 s. | **CAVEAT** | The requested assistant-start-to-result envelopes were 53.695 s for #1 and 18.683 s for #2. They confirm a large directional reduction (2.87x), and #1 is reasonably described as about a minute, but #2 is below the asserted 25--35 s range. |

Checkout: `git fetch origin tag archive/docs/transcript-audit-20260831 && git worktree add --detach <dir> archive/docs/transcript-audit-20260831`

### archive/docs/worktree-lifecycle-hld-20260831

Branch: `docs/worktree-lifecycle-hld-20260831`. Tip: `8f8e113e` · 2026-08-31T09:22:17-07:00. Commits not on main: **0**.

Defines the high-level terminal worktree lifecycle on the prepared-confirmation release history.

Subjects (tip history; no commits outside main): “docs: define terminal worktree lifecycle” · “release: prove installed prepared confirmation route” · “fix: normalize prepared confirmation wire containers”.

No added observation/plan Markdown documents in the merge-base diff.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/docs/worktree-lifecycle-hld-20260831 && git worktree add --detach <dir> archive/docs/worktree-lifecycle-hld-20260831`

### archive/docs/write-refusal-ratification-20260830

Branch: `docs/write-refusal-ratification-20260830`. Tip: `6d558cb3` · 2026-08-30T09:57:36-07:00. Commits not on main: **0**.

Defines and ratifies write-refusal completeness on the typed read-normalization work.

Subjects (tip history; no commits outside main): “docs: design write refusal completeness” · “feat: normalize typed inspect read requests” · “docs: promote read normalization intent leaf”.

No added observation/plan Markdown documents in the merge-base diff.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/docs/write-refusal-ratification-20260830 && git worktree add --detach <dir> archive/docs/write-refusal-ratification-20260830`

## feature/* — 9 tags

The feature branches preserve implementation candidates for prepared requests, confirmation and preview, telemetry, complete refusals, structural lenses, form reads, and file creation. Several predate the late-August program but are retained in this requested family.

### archive/feature/create-files-transaction

Branch: `feature/create-files-transaction`. Tip: `c44ac759` · 2026-08-31T07:23:45-07:00. Commits not on main: **2**.

Adds create_files to edit_clojure transactions and simplifies the associated contract witness.

Subjects: “style(mcp-contract): flatten the redundant create_files let” · “feat(edit_clojure): create_files transaction verb (kc-dx1m)”.

No added observation/plan Markdown documents in the merge-base diff.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/feature/create-files-transaction && git worktree add --detach <dir> archive/feature/create-files-transaction`

### archive/feature/embedded-elaborator-20260830

Branch: `feature/embedded-elaborator-20260830`. Tip: `3f5fb451` · 2026-08-30T19:44:05-07:00. Commits not on main: **8**.

Implements the embedded Spark elaborator with fail-first contract tests and retained idle evidence.

Subjects: “Record embedded elaborator idle evidence” · “Implement embedded Spark elaborator” · “Test embedded elaborator contract red”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-31-embedded-elaborator-production-verification/RESULT.md`; `docs/observations/2026-08-30-write-refusal-001-installed-release.md`.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/feature/embedded-elaborator-20260830 && git worktree add --detach <dir> archive/feature/embedded-elaborator-20260830`

### archive/feature/prepared-confirm-affinity-guidance-20260831

Branch: `feature/prepared-confirm-affinity-guidance-20260831`. Tip: `7e0300fe` · 2026-08-30T21:32:50-07:00. Commits not on main: **6**.

Makes prepared-confirmation affinity failures actionable, with frozen regressions and edge-case audit evidence.

Subjects: “fix: make prepared confirmation affinity actionable” · “test: freeze prepared confirmation affinity repair red” · “docs: audit prepared confirmation affinity edges”.

Added observation/plan documents (merge-base comparison): `docs/plans/prepared-confirm-affinity-guidance-frozen-red-20260831.md`; `docs/observations/2026-08-31-prepared-confirmation-affinity-repair.md`.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/feature/prepared-confirm-affinity-guidance-20260831 && git worktree add --detach <dir> archive/feature/prepared-confirm-affinity-guidance-20260831`

### archive/feature/prepared-confirm-preview-20260830

Branch: `feature/prepared-confirm-preview-20260830`. Tip: `05f5a196` · 2026-08-30T18:52:11-07:00. Commits not on main: **0**.

Adds prepared confirmation and preview, then repairs real-wire container normalization exposed by the audit.

Subjects (tip history; no commits outside main): “fix: normalize prepared confirmation wire containers” · “test: freeze prepared confirmation real-wire regression” · “audit: block W1 wire-normalization false green”.

No added observation/plan Markdown documents in the merge-base diff.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/feature/prepared-confirm-preview-20260830 && git worktree add --detach <dir> archive/feature/prepared-confirm-preview-20260830`

### archive/feature/prepared-request-first-slice-20260830

Branch: `feature/prepared-request-first-slice-20260830`. Tip: `9f2b1ba4` · 2026-08-30T12:59:01-07:00. Commits not on main: **3**.

Implements the first prepared-request slice and measures its live route cost using JSON-semantic equivalence.

Subjects: “bench: seal prepared request live route cost” · “bench: compare paid path by JSON semantics” · “bench: freeze prepared request live route measurement”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-30-prepared-request-live-route-token-measurement.md`; `docs/observations/2026-08-30-prepared-request-live-route-protocol.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-30-prepared-request-live-route-token-measurement.md:3`:

> Verdict: the first slice is inexpensive enough to present for a separate
> install decision. It does not compress the final mutation call. Its justified
> value claim remains fewer assembly errors and recovery turns after an eligible
> read. No installation, reload, registration, or shared-runtime action occurred.

Checkout: `git fetch origin tag archive/feature/prepared-request-first-slice-20260830 && git worktree add --detach <dir> archive/feature/prepared-request-first-slice-20260830`

### archive/feature/show-form

Branch: `feature/show-form`. Tip: `80154bcc` · 2026-08-02T18:16:59-07:00. Commits not on main: **0**.

Adds one-shot structural form reads and configurable CLI installation, with production-migration documentation.

Subjects (tip history; no commits outside main): “Add one-shot structural form reads” · “Make CLI install destination configurable” · “Document clj-surgeon in a production migration”.

No added observation/plan Markdown documents in the merge-base diff.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/feature/show-form && git worktree add --detach <dir> archive/feature/show-form`

### archive/feature/structural-lenses-mvp

Branch: `feature/structural-lenses-mvp`. Tip: `93641b54` · 2026-07-12T18:08:38-07:00. Commits not on main: **0**.

Builds and hardens structural lens plans and installs the repository-owned Codex skill.

Subjects (tip history; no commits outside main): “Install repository-owned Codex skill” · “Harden structural lens plans” · “Merge remote-tracking branch 'origin/smoother-install'”.

No added observation/plan Markdown documents in the merge-base diff.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/feature/structural-lenses-mvp && git worktree add --detach <dir> archive/feature/structural-lenses-mvp`

### archive/feature/substantiation-telemetry-w1-rebase-20260831

Branch: `feature/substantiation-telemetry-w1-rebase-20260831`. Tip: `4e2cf27b` · 2026-08-30T21:00:35-07:00. Commits not on main: **8**.

Binds substantiation telemetry to prepared confirmations and loads the W1 integration witness.

Subjects: “test: load substantiation W1 witness” · “feat: bind telemetry to prepared confirmations” · “test: freeze W1 telemetry integration witness”.

Added observation/plan documents (merge-base comparison): `docs/plans/substantiation-w1-cross-feature-witness-20260830.md`.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/feature/substantiation-telemetry-w1-rebase-20260831 && git worktree add --detach <dir> archive/feature/substantiation-telemetry-w1-rebase-20260831`

### archive/feature/write-refusal-completeness-20260830

Branch: `feature/write-refusal-completeness-20260830`. Tip: `4bb57838` · 2026-08-30T15:23:26-07:00. Commits not on main: **1**.

Preserves the first green implementation of complete write refusals.

Subjects: “Record write refusal first green”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-30-write-refusal-001-first-green.md`.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/feature/write-refusal-completeness-20260830 && git worktree add --detach <dir> archive/feature/write-refusal-completeness-20260830`

## release/* — 5 tags

The late-August release branches preserve installed-route receipts and the published closed-relations history. Their installation claims are historical evidence for the source commits named in the receipts.

### archive/release/closed-relations-published

Branch: `release/closed-relations-published`. Tip: `05f5a196` · 2026-08-30T18:52:11-07:00. Commits not on main: **0**.

Preserves the published closed-relations history with the prepared-confirmation real-wire normalization repair.

Subjects (tip history; no commits outside main): “fix: normalize prepared confirmation wire containers” · “test: freeze prepared confirmation real-wire regression” · “audit: block W1 wire-normalization false green”.

No added observation/plan Markdown documents in the merge-base diff.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/release/closed-relations-published && git worktree add --detach <dir> archive/release/closed-relations-published`

### archive/release/prepared-confirm-preview-published-20260831

Branch: `release/prepared-confirm-preview-published-20260831`. Tip: `469141bd` · 2026-08-30T20:44:55-07:00. Commits not on main: **0**.

Records installation and proof of the prepared-confirmation route after the wire repair.

Subjects (tip history; no commits outside main): “release: prove installed prepared confirmation route” · “fix: normalize prepared confirmation wire containers” · “test: freeze prepared confirmation real-wire regression”.

No added observation/plan Markdown documents in the merge-base diff.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-31-prepared-confirm-preview-installed-route-receipt.md:5`:

> Verdict: **installed and proved on the shared live route**.

Checkout: `git fetch origin tag archive/release/prepared-confirm-preview-published-20260831 && git worktree add --detach <dir> archive/release/prepared-confirm-preview-published-20260831`

### archive/release/prepared-request-published-20260830

Branch: `release/prepared-request-published-20260830`. Tip: `ff2e8abb` · 2026-08-30T14:52:26-07:00. Commits not on main: **1**.

Records the prepared-request installation receipt.

Subjects: “release: receipt prepared request installation”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-30-prepared-request-installed-route.md`.

Recorded result (scope and candidate as quoted), `docs/observations/2026-08-30-prepared-request-installed-route.md:5`:

> Gene authorized installation with the verbatim decision `and GO, yes! go !`.
> This receipt concerns the installed prepared-request first slice at exact commit
> `b445a8c3595d70f6f05b6edccb9b1a924539a195`, tree
> `b1e21af8073e66283f82f4036583bfe2971c4b0a`.
> 
> The canonical remote branch `release/closed-relations-published` and annotated
> tag `stable-prepared-request-20260830` both resolve to that exact commit. The
> release train contains the prepared-request lineage only. The separate
> cold-verifier clock fix and documentation branches did not ride this train.

Checkout: `git fetch origin tag archive/release/prepared-request-published-20260830 && git worktree add --detach <dir> archive/release/prepared-request-published-20260830`

### archive/release/read-normalization-published-20260830

Branch: `release/read-normalization-published-20260830`. Tip: `3a9edc1a` · 2026-08-30T01:17:28-07:00. Commits not on main: **2**.

Verifies installed read normalization and binds its canonical control-plane root.

Subjects: “release: bind canonical control-plane root” · “release: verify installed read normalization”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-30-read-request-normalization-install-receipt.md`.

Recorded result (scope and candidate as quoted), `docs/observations/2026-08-30-read-request-normalization-install-receipt.md:5`:

> State: installed and verified

Checkout: `git fetch origin tag archive/release/read-normalization-published-20260830 && git worktree add --detach <dir> archive/release/read-normalization-published-20260830`

### archive/release/write-refusal-001-published-20260830

Branch: `release/write-refusal-001-published-20260830`. Tip: `4ec9394c` · 2026-08-30T16:08:29-07:00. Commits not on main: **1**.

Seals the installation receipt for write-refusal completeness.

Subjects: “docs: seal write refusal install receipt”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-30-write-refusal-001-installed-release.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-30-write-refusal-001-installed-release.md:5`:

> Verdict: **installed and verified** for exact candidate
> `9af88fbae9ee720613599feaf8cf58432c5898bb`, tree
> `6f9bc30316eb6417977c07c86caf8eb146dfbdb8`.

Checkout: `git fetch origin tag archive/release/write-refusal-001-published-20260830 && git worktree add --detach <dir> archive/release/write-refusal-001-published-20260830`

## bench/* — 3 tags

The benchmark family retains the acid crossover ladder, the invalid three-way caller-wall battery, and prefill/decode and carriage measurements. Noise, oracle failures, and unresolved differences remain part of the results.

### archive/bench/acid-crossover-ladder-20260830

Branch: `bench/acid-crossover-ladder-20260830`. Tip: `f6ac0b59` · 2026-08-30T10:17:16-07:00. Commits not on main: **4**.

Measures the discovery crossover ladder and its regression gate, publishing a lower bound rather than an unsupported crossover point.

Subjects: “bench: publish discovery crossover lower bound” · “bench: align ladder with discovery arms” · “bench: record normalization regression gate”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-30-acid-crossover-ladder-result.md`; `docs/observations/2026-08-30-acid-regression-crossover-preregistration.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-30-acid-crossover-ladder-result.md:5`:

> Verdict: **no same-family crossover was observed through 32 changes and six
> files.** Native won all four measured rungs. The honest same-family estimate is
> a lower bound, **crossover greater than 32 changes / greater than six files**,
> with no measured upper bound.

Checkout: `git fetch origin tag archive/bench/acid-crossover-ladder-20260830 && git worktree add --detach <dir> archive/bench/acid-crossover-ladder-20260830`

### archive/bench/prefill-decode-ratio

Branch: `bench/prefill-decode-ratio`. Tip: `93d99181` · 2026-08-29T20:22:44-07:00. Commits not on main: **5**.

Tests prefill/decode assumptions with identity-echo and JSON-versus-EDN carriage, retaining evidence that changes the interpretation of reported rates.

Subjects: “bench: X5 -- JSON vs EDN carriage, and the confound that outranks the rates” · “bench: test the two assumptions under identity-echo, and lose one” · “bench: let the ratio fold replay from committed evidence”.

No added observation/plan Markdown documents in the merge-base diff.

Recorded result (scope and candidate as quoted), `bench/results/2026-08-29-carriage-json-vs-edn/carriage/SUMMARY.md:36`:

> VERDICT: NO DIFFERENCE DETECTABLE AT THIS n. Every arm produced zero malformed requests (J n=30, E-wrapped n=30, E-raw n=30). The true rates are bounded only below the rule-of-three values in each arm; this run cannot distinguish them and does not justify a migration on robustness grounds.

Recorded result (scope and candidate as quoted), `bench/results/2026-08-29-carriage-json-vs-edn/gentime/SUMMARY.md:21`:

> VERDICT: NOT RESOLVED AT THIS n. The J/E decode-time difference (918.0 ms) does not exceed the combined spread (2122.0 ms). Report as parity NOT ESTABLISHED rather than as parity, and note that token count ratio is 1.1026 while time ratio is 1.0897.

Checkout: `git fetch origin tag archive/bench/prefill-decode-ratio && git worktree add --detach <dir> archive/bench/prefill-decode-ratio`

### archive/bench/threeway-acid-20260831

Branch: `bench/threeway-acid-20260831`. Tip: `f904ed93` · 2026-08-31T08:51:46-07:00. Commits not on main: **6**.

Preserves the three-way caller-wall battery, semantic read-identity scoring, and the finding that the run was invalid.

Subjects: “bench: publish invalid three-way caller-wall battery” · “bench: freeze three-way caller-wall acid battery” · “bench: score semantic read identity”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-08-31-threeway-acid-wall-battery.md`.

Recorded verdict (scope and candidate as quoted), `docs/observations/2026-08-31-threeway-acid-wall-battery.md:3`:

> ## Verdict
> 
> **This battery did not confirm a publishable speedup.** All 42 frozen caller
> processes completed with exit 0, but every task class failed the preregistered
> all-exact correctness gate, and FLAGSHIP also recorded one wrong-subject event.
> The result is therefore **invalid for speed headlines**. The most attractive
> descriptive result—LATEST was 7.92× faster than fresh NATIVE on FLAGSHIP caller
> wall, with a paired log-ratio 95% interval of 4.39×–15.27×—must not be promoted
> as a performance claim from this cohort.

Checkout: `git fetch origin tag archive/bench/threeway-acid-20260831 && git worktree add --detach <dir> archive/bench/threeway-acid-20260831`

## codex/* — 2 tags

The Codex branches retain linked-intent and telemetry ratchets from August 29, including typed read normalization and workspace coverage.

### archive/codex/lid-ratchet-drafts-20260829

Branch: `codex/lid-ratchet-drafts-20260829`. Tip: `c55de227` · 2026-08-29T23:57:41-07:00. Commits not on main: **0**.

Ratifies the read-normalization intent work and implements typed inspect-read requests with linked documentation.

Subjects (tip history; no commits outside main): “feat: normalize typed inspect read requests” · “docs: promote read normalization intent leaf” · “ratify both 2026-08-29 leaves — Gene, verbatim 'Go'”.

No added observation/plan Markdown documents in the merge-base diff.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/codex/lid-ratchet-drafts-20260829 && git worktree add --detach <dir> archive/codex/lid-ratchet-drafts-20260829`

### archive/codex/telemetry-ratchets-20260829

Branch: `codex/telemetry-ratchets-20260829`. Tip: `d473ad6a` · 2026-08-29T23:39:45-07:00. Commits not on main: **2**.

Adds coverage and workspace telemetry ratchets and connects them to measurement specifications.

Subjects: “study: bind measurement specs to telemetry ratchets” · “study: add coverage and workspace telemetry ratchets”.

Added observation/plan documents (merge-base comparison): `docs/plans/telemetry-coverage-and-workspace-ratchets.md`.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/codex/telemetry-ratchets-20260829 && git worktree add --detach <dir> archive/codex/telemetry-ratchets-20260829`

## bb-rewrite-tower* — 2 tags

The tower is a Babashka-first hybrid: portable planners and CLI work use Babashka while MCP and unavailable dependencies retain the JVM. Its probe supplies a warm inner-loop verdict, with the cold landing gate still pending; the two tags preserve different landing and refusal-hardening checkpoints.

### archive/bb-rewrite-tower

Branch: `bb-rewrite-tower`. Tip: `76af5782` · 2026-09-12T23:05:49Z. Commits not on main: **2801**.

Preserves the Babashka rewrite program and its repeated retained experiments, culminating in candidate-bound battery evidence. The final repairs keep probe request-bound refusals typed through the HTTP servlet and witness the writer boundary.

Subjects: “battery: passing candidate-bound receipt for 8c542ede (counted battery run alone, rc=0, 2026-09-12T23:05Z)” · “test: register the autofix's probe-servlet request-bound witness in the deftest census (+1 name, 0 removed); battery ledger row from packet da68a598” · “fence: Sol's GO-WITH-FIX for 14644aad1c72 — Preserve typed probe request-bound refusals through the HTTP servlet and add a direct writer-boundary regression.”.

Added observation/plan documents (merge-base comparison): `docs/plans/bb-rewrite-tower.md`; `docs/observations/2026-09-12-bbtower-block-b/attempt25/sol-verdict.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:73`:

> {:sha "8c542edee5060f4674514ff9d2b2aeb3273e98ba", :started "2026-09-12T23:02:29Z", :wall_s 167, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

**Tower locator.**

| Role | Path at this tag | Lines (`wc -l`) |
|---|---|---:|
| Hybrid plan | `docs/plans/bb-rewrite-tower.md` | 46 |
| Edit-to-probe plan | `docs/plans/edit-to-probe.md` | 63 |
| Probe entry point | `src/clj_surgeon/probe.clj` | 114 |
| Block B report | `docs/observations/2026-09-12-bbtower-block-b/REPORT.md` | 79 |
| Last retained attempt report | `docs/observations/2026-09-12-bbtower-block-b/attempt25/REPORT.md` | 110 |

Checkout: `git fetch origin tag archive/bb-rewrite-tower && git worktree add --detach <dir> archive/bb-rewrite-tower`

### archive/bb-rewrite-tower-land

Branch: `bb-rewrite-tower-land`. Tip: `839b7c53` · 2026-09-12T23:54:33Z. Commits not on main: **2805**.

Hardens the rewrite-tower landing by bounding reader recursion before parsing and catching Throwable at the servlet boundary; registers the new refusal kinds.

Subjects: “test: pin the two new probe refusal kinds in the frozen entrance enumeration (164 -> 166)” · “probe: bound reader recursion before the parse and widen the servlet boundary to Throwable” · “test: RED class oracle -- a Throwable escapes probe-servlet's EDN parse for every nested reader shape”.

Added observation/plan documents (merge-base comparison): `docs/plans/bb-rewrite-tower.md`; `docs/observations/2026-09-12-bbtower-block-b/attempt25/sol-verdict.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:72`:

> {:sha "14644aad1c724a0dc96fc50c9e9e0c59eb762fb2", :started "2026-09-12T22:04:25Z", :wall_s 161, :verdict :fail, :host "anvil-server", :lanes 8, :skipped 0}

**Tower locator.**

| Role | Path at this tag | Lines (`wc -l`) |
|---|---|---:|
| Hybrid plan | `docs/plans/bb-rewrite-tower.md` | 46 |
| Edit-to-probe plan | `docs/plans/edit-to-probe.md` | 63 |
| Probe entry point | `src/clj_surgeon/probe.clj` | 212 |
| Block B report | `docs/observations/2026-09-12-bbtower-block-b/REPORT.md` | 79 |
| Last retained attempt report | `docs/observations/2026-09-12-bbtower-block-b/attempt25/REPORT.md` | 110 |

Checkout: `git fetch origin tag archive/bb-rewrite-tower-land && git worktree add --detach <dir> archive/bb-rewrite-tower-land`

## records/MCP-main — 1 tag

The records branch separates experiment observations, canary grades, ledgers, and synthesis from code freshness on MCP/main. It is the durable September research record, including the later zoom-out reports.

### archive/records/MCP-main

Branch: `records/MCP-main`. Tip: `9231da57` · 2026-09-16T14:03:36Z. Commits not on main: **2969**.

Preserves the dedicated records branch: canary grades, experiment ledgers, captain's logs, and the later synthesis of the August–September program. Its tip compares three possible strategies and records the recommendation.

Subjects: “records: zoom-out — three strategies, one recommendation; Astra pass 2” · “records: zoom-out — ethnographic synthesis of the Aug–Sep record; Astra pass 1” · “canary: 2026-09-16 cell=3 mvr/sol/cond=W grade=PASS”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-10-squeeze-records-lane.md`; `docs/observations/2026-09-10-public-main-merge-plan.md`.

Recorded result (scope and candidate as quoted), `docs/observations/2026-09-16-zoom-out-three-strategies.md:8`:

> - Voluntary adoption: 0/4, 0/3, 0/9, 0/10, 0/13, 0/19; fresh seats recorded no consideration at all. Routing works only when the harness routes.
> - Tool wall is 1–4% of any task's wall (measured Sep 6); wall comparisons after that date bought nothing the practice could use, and the meters rewarded producing them.
> - Structure won three times, all consequence claims: the count contract (E4: 31 rewritten where 2 were the alias, gates green, refused), the exact-contract namespace split, the probe with typed refusals where native exits 0 having run nothing. General method won five times.
> - Census of 1,570 sessions: program edits 6,124 / editor 2,132 / Surgeon 220; program edits associated with 3.3× more broken files per verified edit (association; the 107 candidates need auditing before they are a baseline).
> - Human cost: 3–17 review rounds per landing; nine self-inflicted 40-min stops; seven withdrawn figures; 61.2% of actions apparatus.
> - Only two shrinkings in forty days, both Gene's rulings (Sep 2 pull the plate; Sep 10 slower-but-safer).

**Records locator.** `docs/observations/2026-09-08-row1-split-ledger.md`, `docs/observations/2026-09-16-ethnography-synthesis-zoom-out.md`, `docs/observations/2026-09-16-zoom-out-three-strategies.md`, and the two `2026-09-16-astra-zoom-out-pass-{1,2}.md` reports are retained here.

Checkout: `git fetch origin tag archive/records/MCP-main && git worktree add --detach <dir> archive/records/MCP-main`

## skiff* — 2 tags

The skiff branches preserve platform and onboarding repairs: Darwin home lookup and an optional Codex MCP registration that cannot block startup.

### archive/skiff-worker/7h2-onboarding-required-false

Branch: `skiff-worker/7h2-onboarding-required-false`. Tip: `27c3d663` · 2026-09-24T11:29:41-07:00. Commits not on main: **2906**.

Makes Codex's Surgeon registration optional during onboarding and witnesses that the managed block cannot gate Codex startup.

Subjects: “Merge commit '205b6188' into skiff-worker/7h2-onboarding-required-false” · “test: census names managed-block-never-gates-codex-startup” · “battery: receipt for cdc18278bc9467d52fed2bff6bbef375259ccbf5 merged onto trunk (landing gate battery-fresh)”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-06-skiff-install-stable.md`; `docs/plans/diff-impact-executor.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:83`:

> {:sha "cdc18278bc9467d52fed2bff6bbef375259ccbf5", :started "2026-09-24T18:21:11Z", :wall_s 194, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/skiff-worker/7h2-onboarding-required-false && git worktree add --detach <dir> archive/skiff-worker/7h2-onboarding-required-false`

### archive/skiff/darwin-passwd-home

Branch: `skiff/darwin-passwd-home`. Tip: `f0775312` · 2026-09-14T02:07:12Z. Commits not on main: **2847**.

Repairs Darwin home lookup, short-circuiting successful getent resolution while preserving operational errors and testing fallback equality independently of process context.

Subjects: “fence: Sol's GO-WITH-FIX for 7728eb43 (ship 20260914T015623Z) applied by the seat after the autofix refused fix-branch-race — passwd-home short-circuits after getent success and rethrows operational failures from uninspectable command paths” · “test: exclude process context from fallback contract equality” · “test: reproduce fallback contract mismatch with carried process state”.

Added observation/plan documents (merge-base comparison): `docs/observations/2026-09-06-skiff-install-stable.md`; `docs/plans/data-not-code-round9.md`.

Recorded battery row (for the SHA in the row), `docs/observations/battery-ledger.edn:76`:

> {:sha "5ae0066c75ef3b19d28c94405aa674573ce5a29a", :started "2026-09-14T00:47:33Z", :wall_s 197, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/skiff/darwin-passwd-home && git worktree add --detach <dir> archive/skiff/darwin-passwd-home`

## singletons — 8 tags

The remaining tags cover early CLJC and form-classification work, the legacy MCP history, the November usage study, and small eligibility, timeout, and telemetry-witness branches.

### archive/cljc-ops

Branch: `cljc-ops`. Tip: `5be543ca` · 2026-04-29T12:51:38-07:00. Commits not on main: **0**.

Adds CLJ, CLJS, and CLJC structural operations with reader-conditional and dialect-split namespace handling, plus regression tests.

Subjects (tip history; no commits outside main): “Add comprehensive CLJC tests for existing ops + fix parse-ns-aliases and extract regex” · “Add CLJ/CLJS/CLJC structural operations + reader-conditional awareness” · “Fix #1: derive correct namespace for dialect-split source layouts (.clj/.cljs/.cljc)”.

No added observation/plan Markdown documents in the merge-base diff.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/cljc-ops && git worktree add --detach <dir> archive/cljc-ops`

### archive/fix/cold-verifier-deterministic-clock-20260830

Branch: `fix/cold-verifier-deterministic-clock-20260830`. Tip: `7dccc395` · 2026-08-30T07:25:01-07:00. Commits not on main: **1**.

Makes the cold-verifier admission-timeout test deterministic.

Subjects: “test: make cold admission timeout deterministic”.

No added observation/plan Markdown documents in the merge-base diff.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/fix/cold-verifier-deterministic-clock-20260830 && git worktree add --detach <dir> archive/fix/cold-verifier-deterministic-clock-20260830`

### archive/form-classification-centralize

Branch: `form-classification-centralize`. Tip: `fe12001f` · 2026-05-01T09:02:37-07:00. Commits not on main: **0**.

Centralizes form classification and fixes private >defn- detection on the CLJC-aware structural-operation history.

Subjects (tip history; no commits outside main): “Centralize form classification into forms.clj; fix >defn- private detection bug” · “Merge branch 'cljc-ops'” · “Merge PR #3: CLJC structural ops + reader-conditional awareness”.

No added observation/plan Markdown documents in the merge-base diff.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/form-classification-centralize && git worktree add --detach <dir> archive/form-classification-centralize`

### archive/legacy/mcp

Branch: `legacy/mcp`. Tip: `08bf1d23` · 2026-11-22T13:13:18-08:00. Commits not on main: **0**.

Preserves the older MCP history, the bounded fan-out ceiling finding, and November usage and testing-bar records.

Subjects (tip history; no commits outside main): “CLAUDE.md: state the testing bar for new code as numbers (Gene, 2026-11-22)” · “observations: usage study of 2026-11-22: two weeks of calls, then nine weeks of silence” · “E-CEILING80 recorded: all three withdrawals fire; square 2 bounded to N<=40; report + brainfleet §32 + log”.

No added observation/plan Markdown documents in the merge-base diff.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/legacy/mcp && git worktree add --detach <dir> archive/legacy/mcp`

### archive/prep/substantiation-w1-witness-20260830

Branch: `prep/substantiation-w1-witness-20260830`. Tip: `9656132a` · 2026-08-30T18:35:05-07:00. Commits not on main: **1**.

Freezes the integration witness for substantiation telemetry on prepared confirmations.

Subjects: “test: freeze W1 telemetry integration witness”.

Added observation/plan documents (merge-base comparison): `docs/plans/substantiation-w1-cross-feature-witness-20260830.md`.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. No measured outcome is inferred from the branch name.

Checkout: `git fetch origin tag archive/prep/substantiation-w1-witness-20260830 && git worktree add --detach <dir> archive/prep/substantiation-w1-witness-20260830`

### archive/refactor/eligibility-legible-checks-20260831

Branch: `refactor/eligibility-legible-checks-20260831`. Tip: `0ba2e465` · 2026-08-31T05:48:21-07:00. Commits not on main: **7**.

Rewrites eligible-descriptor and form-evidence checks as named short-circuiting predicates after characterization coverage was repaired.

Subjects: “refactor: form-evidence? as named short-circuiting checks (house pattern #2)” · “refactor: eligible-descriptor as named short-circuiting checks” · “test: ns-form require for explainer — fixes runtime-require alias error”.

No added observation/plan Markdown documents in the merge-base diff.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. The commit record reports: “test: hermetic explainer binding per re-audit NO-GO”.

Checkout: `git fetch origin tag archive/refactor/eligibility-legible-checks-20260831 && git worktree add --detach <dir> archive/refactor/eligibility-legible-checks-20260831`

### archive/study/2026-11-22

Branch: `study/2026-11-22`. Tip: `4502e313` · 2026-11-22T22:22:40Z. Commits not on main: **2999**.

Analyzes recent agent usage and friction, hand-corrects fallback labels, and adds weekly usage reporting without scheduling it.

Subjects: “study: weekly-usage.py prints one line per week (calls, sessions, refusals, errors, corrected fall-backs, per op); phase 1 step E, unscheduled” · “study: fall-back labels corrected by hand for split-ns!, alias_migration and 60 cat calls (phase 1, step C)” · “study 2026-11-22: clj-surgeon friction list from session history, and the "what we tried" ledger (read-only)”.

Added observation/plan documents (merge-base comparison): `docs/observations/study-ops-o2-round9-review-sol.md`; `docs/observations/study-ops-o2-round7-review-sol.md`.

Shared-history battery result (the row names its measured candidate, not a verdict on this branch’s own changes), `docs/observations/battery-ledger.edn:53`:

> {:sha "9c1430412967ed49ecaa8c23b7995cef05035faa", :started "2026-09-08T23:23:17Z", :wall_s 149, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}

Checkout: `git fetch origin tag archive/study/2026-11-22 && git worktree add --detach <dir> archive/study/2026-11-22`

### archive/test/eligibility-characterization-20260831

Branch: `test/eligibility-characterization-20260831`. Tip: `5573f3a8` · 2026-08-30T22:13:02-07:00. Commits not on main: **5**.

Repairs and completes eligibility characterization, including all twelve mutations, hermetic explainer binding, and namespace loading.

Subjects: “test: ns-form require for explainer — fixes runtime-require alias error” · “test: complete general agreement — all 12 mutations incl. seven-form” · “test: hermetic explainer binding per re-audit NO-GO”.

No added observation/plan Markdown documents in the merge-base diff.

No branch-specific measured verdict located in the searched result/receipt/ledger/verdict documents. The commit record reports: “test: hermetic explainer binding per re-audit NO-GO”.

Checkout: `git fetch origin tag archive/test/eligibility-characterization-20260831 && git worktree add --detach <dir> archive/test/eligibility-characterization-20260831`

## Coverage verification

Every tag has its own entry, the 108 receipt-named branches included. The check below compares the local `archive/*`
tags with this file's `### archive/` headings and exits non-zero on any missing, extra or duplicate heading; the tag
count is reported, not asserted, so a later tag is a new snapshot rather than a failure. Its output against the final
bytes of this file follows it; the paste is a fixed point (the output line is one line whatever number it carries, so
re-running it after pasting prints the same line).

```bash
catalog=docs/observations/2026-11-28-archive-tag-catalog.md
tags=$(git tag -l 'archive/*' | LC_ALL=C sort)
heads=$(grep -E '^### archive/' "$catalog" | sed 's/^### //' | LC_ALL=C sort)
missing=$(LC_ALL=C comm -23 <(printf '%s\n' "$tags") <(printf '%s\n' "$heads") | grep -c . || true)
extra=$(LC_ALL=C comm -13 <(printf '%s\n' "$tags") <(printf '%s\n' "$heads") | grep -c . || true)
dups=$(printf '%s\n' "$heads" | uniq -d | grep -c . || true)
printf 'tags=%s headings=%s missing=%s extra=%s duplicate=%s lines=%s\n' \
  "$(printf '%s\n' "$tags" | grep -c .)" "$(printf '%s\n' "$heads" | grep -c .)" \
  "$missing" "$extra" "$dups" "$(wc -l < "$catalog" | tr -d ' ')"
[ "$missing" -eq 0 ] && [ "$extra" -eq 0 ] && [ "$dups" -eq 0 ] && echo COVERAGE-OK || { echo COVERAGE-FAIL; exit 1; }
```

```text
tags=326 headings=326 missing=0 extra=0 duplicate=0 lines=5609
COVERAGE-OK
```

Index: astra/* 27; MCP/* 7; bridge/* 46; fable/* 136; experiment/* 40; audit/* 16; docs/* 22; feature/* 9; release/* 5; bench/* 3; codex/* 2; bb-rewrite-tower* 2; records/MCP-main 1; skiff* 2; singletons 8; total 326. List: `git tag -l 'archive/*'`. Fetch: `git fetch origin --tags`.
