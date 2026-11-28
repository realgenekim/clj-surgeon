# Plan: port the winning verbs onto main, 150 lines at a time (D.1, 2026-11-22)

> **FROZEN for alias migration, 2026-11-28 (Gene, ruling 75: "frozen off main is good"; see the freeze record in
> `docs/observations/2026-11-28-archive-tag-catalog.md`).** Stages 1 to 13 below are not executed; alias migration stays
> under `archive/astra/namespace-split`. Stages 3, 8 and 9 (the namespace-repair code, about 320 lines) are named as
> harvestable for a future move-a-function verb. The namespace-split half of this plan is superseded by the third
> fleet's port plan under Gene's ruling 69 a (slice 0 a held-out run; ruling 70 a byte-identical moves) and waits for
> Gene's decision 88. The tally below stays at 0 of ~33 by ruling, not by delay.

Gene, 2026-11-22: "And I love the idea of merging in only the winning verbs for surgeon, 150 LoC at a time, leaving out
losing ones; and validation that we don't need MCP access anymore, and we can retire all of that on its own branch."

Precondition (amended handoff D): no slice starts before the gate is green and the mayor confirms branch protection.

Scope: alias migration first, then the namespace split, from `astra/namespace-split` (2ba9c950; 2,493 commits off main,
merge-base affdfe3f) onto main. Every slice is one pull request of 150 changed lines or fewer including its tests, leaves
main green and loading, and meets the testing bar in CLAUDE.md (115 test lines per 100 source lines). Those two rules
together allow about 70 source lines per slice, and the table below is cut at that size. Evidence: the read-only closure
study `~/handoffs/surgeon-seat-alias-migration-closure.md` (Opus, over both worktrees, file:line cited), spot-checked by
a second independent reader against the branch.

## What alias migration is, on the branch

- Planner: `src/clj_surgeon/alias_migration.clj`, 1,733 lines, pure (requires only cheshire, clojure.string and
  rewrite-clj, all on main).
- MCP verb: `src/clj_surgeon/mcp_alias_migration.clj`, 2,854 lines, about 800 of them pure (validation, scope globbing,
  suggestion helpers, `plan->spec` at 1439-1471, receipt and refusal shaping). The rest is scanning, `plan!` (1044-1438,
  395 lines in one function), `commit!` (from 2357, 330 lines), detail files, telemetry.
- It writes through two namespaces that ALREADY exist on main in compatible form: `intent_transaction`
  (`execute-mcp-change!`, `execute-undo!`; 91 lines differ, mostly docstrings plus a write-boundary hook) and
  `mcp_change_buffer` (`capture-verification-baseline!`, `run-verification!`; used only when a `verify` profile is given).
- There is NO command-line entrance on the branch; one must be written new. The branch's MCP wrapper cannot load under
  babashka without nREPL (through `mcp_hot_verify`), so the entrance loads verification lazily or not at all.
- Tests on the branch: 7,301 lines (143 deftests, 104 temp-file uses) for the MCP verb, 1,252 pure planner tests
  (60 deftests), a 627-line generated fixture corpus, 272 lines of receipt-artifact boundary tests.
- Intent files: `docs/intent/alias-migration/` (design 451 lines, specs 151, receipt-artifacts.edn 40, its specs 8).
  Each ported slice carries the intent ids it satisfies (LID is mandatory on main); the specs for behaviour that stays
  behind (glob scope, lib rename, detail files, telemetry) stay behind with it, marked deferred in the design file.
- Frozen benchmark: none survives in reproducible form; the 1.38x replication's trees and receipts under /var/tmp are
  gone. The reproducible case is the row-2 natural migration on marvin-voice-remote 5a4b4bc (21 sites, 9 files, alias
  `mjson`; request in clj-surgeon-records `2026-09-08-row2-alias-preregistration.md` lines 160-166). Stage 13 freezes a
  pinned copy of the nine files under `test-fixtures/alias-migration/mvr-5a4b4bc/` with the request and expected receipt.

## Stages in dependency order, cut into slices at the bar

| Stage | Source (alias_migration.clj unless noted) | Source lines | Slices |
|---|---|---|---|
| 1 | `receipt_artifacts.clj` (new file, 74) with a narrowed boundary test | 74 | 2 |
| 2 | Planner node and binding-name helpers, 17-191 | 175 | 3 |
| 3 | ns and libspec facts, require clause, 192-279 | 88 | 2 |
| 4 | Reader-conditional branches and `token-facts`, 280-455 | 176 | 3 |
| 5 | `rewrite-forms`, 456-666: ONE function of 211 lines. Refactor on the branch into named locals first, then port | 211 | 4 |
| 6 | Arity, binding-vector and require-clause rewrites, `remove-libspec`, 667-801 | 135 | 2 |
| 7 | Helpers the later stages call: `excluding-call` 884-923, `prefix-list-nodes`, `ns-declared-name`, `indirect-facts` 977-1005 | 69 | 1 |
| 8 | `analyze-requires`, the two refusals, `ns-bound-names`, `choose-alias`, 1006-1173 | 168 | 3 |
| 9 | `ns-form-edit`, `libspec-with-refer`, 1174-1237 | 64 | 1 |
| 10 | `file-plan`, 1238-1468: ONE function of 231 lines. Refactor first, as stage 5 | 231 | 4 |
| 11 | `plan` in var mode, `refusal`, `base-call`, 802-841 and 1639-1733. This is an EDIT of branch code, not a copy: the calls to `lib-rename-plan` and `string-mentions` are removed and the var-only contract is stated in the docstring and a refusal | 135 | 2 |
| 12 | Fixture corpus subset (4 of the 12 generated namespaces) and golden `plan` tests. Mostly data; ruling on the 150-line rule for fixture data applies | ~200 data | 2 |
| 13 | New entrance `clj-surgeon :op :alias-migration!` in core.clj: `validate-request` (mcp_alias_migration.clj 106, 72 lines), explicit `scope.paths` through main's `mcp-paths/resolve-source-path`, `plan->spec` (37 lines), commit through main's `intent-transaction/execute-change!` (the kernel `:change!` already uses), `:receipt-out`. Then dispatch, help, refusal exits, undo through the existing `:undo-change!`, and the frozen 5a4b4bc fixture with its committed receipt | ~190 | 4 |
| Later, optional | glob scope (`scan-scope`, ~230), lib rename with `:create-files`, `verify` with lazily loaded change_buffer, detail files, telemetry; only if a measured task needs them | ~700 | ~10 |

Totals for the lean port: about 1,720 source lines, about 1,980 test lines at the bar, about 200 lines of fixture data,
so about 3,900 lines in about 33 slices. A faithful port of everything the branch has is about 15,400 lines and over
100 slices; it is not proposed. The closure study's "about 25 slices" undercounted because it packed up to 175 source
lines in a slice; this table is the corrected count.

Stages 5 and 10 cannot pass the rule as the branch stands: each is one function over 200 lines. Per CLAUDE.md "lower
the cost of change first", each is refactored on the branch into named local functions BEFORE porting, behaviour
unchanged and the branch's own tests green, then ported in four slices. If the refactor is refused, the alternative is
a one-time exception of about 280 lines per slice, which needs a ruling.

## Namespace split (second)

New files on the branch: `namespace_split` 1,035, `namespace_split_io` 574, `split_proof_gate` 319,
`synchronous_verification` 309, `namespace_split_warm` 110, `mcp_namespace_split` 29. Changed on main: `mcp_operation`
(198 lines differ), `mcp_extraction`, `extract`, `analyze`. A command-line entrance exists: `:split-ns!` (core.clj:2153)
calls `namespace-split-io/cli!`. Tests: 1,809 lines in five files plus the in-repo fixture `test-fixtures/namespace-split`.
Cell C: request at `/var/tmp/forge/plan2/cellC/D1-request.edn`; a committed receipt with `verification_complete=true`
and `proof_pending=[]` at `/var/tmp/forge/round/runs/sentinel-ns-20261025T070002Z/`. Shared with alias migration:
intent_transaction, receipt_artifacts, mcp_paths, mcp_process, verification_process, file_ops. Its stage table is
written after alias migration lands, with the same method; expect about 2,400 source lines and about 45 slices.

## Cell C cold replay, round 3 (2026-11-28)

Replacement: replaces PR 89 (closed on round 3, the unlisted-file rule); inventory must pass before copying only manifest-listed sources.

The fixture runner's bounded contract is to verify frozen input hashes, install the recovered cold
profile with scratch/fixture paths, attempt commit once, and compare the eight recorded facts. Only a
typed, explicitly unmutated/uncommitted refusal permits one plan-only diagnostic. Its tests cover
request rebinding, profile argv/path preservation, safe fallback, and honest missing/different facts.
This fixture work does not port the verb or admit a new split contract.

Round 3 hardening contract: refuse optimized Python with exit 2 before opening any input; require
exactly one frozen oracle manifest literal before substitution; clear `PYTHONOPTIMIZE` in the profile.
The donor receives only PATH, HOME, JAVA_HOME and LANG when present. With SMTP_HOST absent and no
fixture `secrets/smtp.edn`, frozen mail uses dev-render. Request and receipt files live in a unique
sibling directory outside scratch. Boundary witnesses run the real runner CLI against a stub donor
for acceptance, nonzero exit and typed refusal, and run the Python wrapper in optimized mode and
against missing/duplicate manifest literals. Frozen sources and oracle remain byte-identical.

Inputs and detailed receipts: [Cell C fixture README](../../test-fixtures/namespace-split/cellc-d9205abc/README.md#replay-usage-and-result).
The missing `split-unit` configuration is now frozen and installed. The four test callers are frozen
byte-identically from `d9205abc`, together with the three launch/config files, 19 application namespace
dependencies, one test helper, and three letter templates needed for `bin/kaocha unit --fail-fast`.
The requested architecture test exists only in the recovered working tree, not at the pinned commit;
its provenance gap remains open. Port check: 35 pairs, 34 OK, 1 NO-SOURCE; all 39 SHA-256 entries pass.

PR 89 review round 2 corrects the acceptance contract: every invocation must exit zero;
`created` requires footprint evidence, and unexpected paths must be a subset of the three recorded
artefacts. Missing facts fail. Hashing is in-process; copying preserves executable attributes; the
oracle manifest is an explicit fixture-relative argument. Tests load the frozen receipt and profile.
Real donor replay, 2026-11-28, Anvil, donor 7b312f85, outside any sandbox: commit exit 0, profile green
(~9.3 s), every comparable fact same (87 sites / 5 callers, 20 created, 20 destinations, committed, verified, same
unrequired refs), unexpected paths expected-different, exit 0; the receipt is checked in beside the fixture as
`replay-receipt-2026-11-28-anvil-7b312f85.edn`. The unlisted-file refusal was probed on a fixture copy (exit 2, no
scratch). Self-test: 7 tests / 77 assertions; kondo clean; all 39 digests verified. Checked-in subprocess probes
exercise runner acceptance and refusal with a stub donor and a poisoned caller environment, plus the Python
optimization and manifest-substitution guards. The fixture is now a complete cold oracle for "the port must
reproduce this receipt" (stage 13).

## Deliberately left behind, and how we know

- The losing operations, by name: `admit_clojure_patch`, `apply_clojure_changes`, `edit_clojure` (with
  `symbol_migration`/`require_change` as its fields), `transform_clojure`, `inspect_clojure` (batching, snapshot guards,
  semantic prepare-change through cclsp), `change!`/`insert-forms!` as MCP gestures, helper extraction with caller
  changes, prepared confirmations, the change-buffer basis store, worktree lifecycle, the bb tower, the routing-plate
  generator, fan-out and per-form writes. Measured losers or never measured against native; receipts in
  `docs/observations/2026-11-22-usage-study/LEDGER-condensed.md`.
- `intent_transaction`: alias migration emits only `:do [:replace original]` with `:expect {:matches 1}` plus optional
  `:create-files` (`plan->spec`, mcp_alias_migration.clj 1456-1471). It never reaches `compile-intent-edits`,
  `compile-binding-rename-edits`, `insertion-gap` or the assoc-entry path. Nothing to delete: the file is on main.
- `mcp_change_buffer`: 39 of 53 definitions (about 1,420 lines) are the `apply_clojure_changes`/`edit_clojure` basis
  machinery, unreachable from the two verification entry points (closure study's reachability probe; not re-derived by
  the second reader). Verification itself is reached only with `verify`, which the lean port omits.
- `relation_census` (2,225) and `parse_admission` (511): pulled in only by the branch's newer `mcp_paths`, `outline`
  and `structural_lens`; main's versions serve the four path functions alias migration needs.
- MCP registration, schema and server wiring: about 33 alias-tagged sites in mcp_tool, mcp_server, mcp_schema (closure
  study count). The command line replaces them; this is the "we do not need MCP access" validation for this verb.
- The branch's `plan!`/`commit!` scanning, detail files and telemetry.

## Tally (kept current; the "harvest" number)

| Slices done | Lines ported | Lines left behind on the branch |
|---|---|---|
| 0 of ~33 (alias migration) | 0 | about 11,500 of alias migration's 15,400, plus every losing operation |

## Gates that must hold before a slice merges

Green `make test-product`; the slice's own tests at or above 115 test lines per 100 source lines; one independent read
before the first push; one push per review round; after a third failed review, stop and write the design on one page.
A verb counts as ported only when its frozen fixture produces the committed receipt on main (stage 13).
