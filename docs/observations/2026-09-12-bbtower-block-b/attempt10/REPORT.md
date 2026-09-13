# Block B attempt 9 repairs — attempt10 evidence

**23,939 ms fast coordinator makespan; 42,653 ms fast sum; 14,851 ms bb sum.**
The standalone fast run reached the requested timing, but refused an inert
old-path assertion mistaken for a fixture. That scanner was repaired without
moving the pure test. The final prewarm passed on code tip
`7da1b44320de66d583ebf0a3b467db297a4c6088`, including all fast members and
the newly required no-argument diagnostic. **Verdict: GO-WITH-REVIEW**:
Fable's ceiling ratification and independent acceptance remain pending.

Baseline is `dcd6d26b`, on `bb-rewrite-tower-local`; the prior attempt9
report-only commit `a5df0e32` remains in history. This directory is committed
last. No push, tag, shared installation, merge, landing or performance certification.

## Findings delivered

1. **Refusal completeness:** extraction retains owner files and handles
   direct calls, requiring-resolve calls, keyword `:kind` arguments, and map
   refusal fields. Every declared owner contributes kinds. The servlet plant
   goes red naming `mcp_http_server.clj` and
   `:probe-servlet-unregistered-kind`; the reverse plant names
   `:probe-registered-but-never-emitted`. A final in-memory registry-reader
   plant copies a complete valid row: exactly one vocabulary failure, no
   row-shape failure. All source/registry plants were restored.
   Evidence: [servlet red](servlet-plant-red.log),
   [valid-row reverse red](dead-valid-row-red.log),
   [restored green](probe-final-green.log).
2. **Probe booleans:** chose option (a). Probe publishes no verification
   booleans, and retains `proof_pending [:landing-gate]` on passed, failed,
   empty and refused observations. The registry records why a constant false
   field is not a driven seam. Absence and registration have explicit witnesses.
   Evidence: [red](probe-boolean-red.log), [green](probe-final-green.log).
3. **Ceiling:** `ceil(240989 × 60000 / 38646) = 374149 ms` from a passed
   shipped-runtime calibration run. The original bb-runtime line is in
   [ceiling-run.log](ceiling-run.log), namespace facts in its
   [receipt](ceiling-run/receipt.edn), and the inputs in
   [ceiling-derivation.log](ceiling-derivation.log). Frozen cadence preserves
   the same-run denominator; replay uses shipped runtimes for every measured
   member, with no patched classification. The later outline runtime repair
   is outside that run and changes neither sum. Exact ceiling/+1 witnesses
   are [red](ceiling-red.log) and [green](ceiling-green.log).
4. **Diagnostic:** `bb -Xmx1g test/run_all.clj` changed from exit 96 to
   829 tests / 7,281 assertions, exit 0. Its default filters by executing
   runtime; explicit selections retain the mismatch refusal. The landing
   gate and prewarm own `test-bb-diagnostic` after the shared pool because
   explicit coordinator children could not detect this broken default.
   The final prewarm executed that real entrance successfully in 218,927 ms.
   Evidence: [before](diagnostic-red.log), [after](diagnostic-green.log),
   [final gate](prewarm-2.log).
5. **Cadence and budgets:** all 47 previously unassigned coordinator owners
   now have manifest and ns-metadata cadence: 33 fast, 14 battery, with no
   override. Every wall and reason is in [lane-moves.md](lane-moves.md).
   Fast selection no longer unions in the historical bb inventory. The parent
   derives bb namespace budget violations from wall facts; actual child runtime
   determines runtime sums. Missing budgets refuse before launch. The census
   adopts 869 existing test names and removes none; all 159 test files retain
   their original test names. Evidence: [negative budgets](lane-budget-red.log),
   [green witnesses](lane-budget-final.log), [census proof](census-proof.log).

## Measurements and gates

| Run | Makespan / command wall | Cadence sums | bb runtime sum | Result and receipt |
|---|---:|---|---:|---|
| Calibration | 82,910 ms coordinator | fast 38,646 ms | 240,989 ms | Passed; [receipt](ceiling-run/receipt.edn) |
| One standalone `make test-fast` | 23,939 ms coordinator | fast 42,653 ms | 14,851 ms | Budgets pass; one scanner false positive, subsequently repaired; [receipt](test-fast/receipt.edn) |
| One `make test-integration` | 72.40 s command wall | integration 59,304 ms | JVM runner | 168 tests / 3,798 assertions, zero isolation violations; [log](test-integration.log) |
| Final prewarm MCP pool slice | 90,054 ms | fast 44,230 ms; integration 66,582 ms | 14,577 ms | 1,406 tests / 15,505 assertions, zero isolation violations; [receipt](prewarm-2/mcp/receipt.edn) |
| Final prewarm bb pool slice | 86,755 ms | fast 3,509 ms; battery 237,981 ms | 235,673 ms | 898 tests / 7,981 assertions; [receipt](prewarm-2/bb/receipt.edn) |
| Final prewarm overall | 388,351 ms receipt; 390.49 s command wall | Includes all stages | Runtime sums above overlap cadence sums | Passed, `landing? false`, `prewarm? true`; [receipt](prewarm-2/receipt.edn) |

The final prewarm's global fast phase took **22,854 ms**. Runtime and cadence
sums are overlapping classifications and must not be added together. The
clj-splice prerequisite also passed its five tests / 59 assertions in the
standalone fast invocation, separately from the 94-namespace coordinator.

Exactly two prewarms ran. The first refused outline-corpus-integration-test's
21,033 ms bb wall against its 20,000 ms namespace budget. Sequential isolated
follow-up measured 20,859 ms bb / 8,306 ms JVM, with identical test counters.
The existing >2.0 runtime rule selects JVM; cadence and budgets stay unchanged.
This was the single allowed repair; see [prewarm-repair.md](prewarm-repair.md).
The final prewarm passed alias, MCP, bb, diagnostic, hygiene and intent audit.
The standalone fast command was not repeated to erase its earlier refusal.

The date-based outer fast timer produced an invalid value and is discarded;
only the coordinator receipt's fast makespan is claimed. The initial
calibration mistakenly wrote live logs under docs and triggered isolation
plus the inherited unbounded dirty-worktree CLI receipt defect. That failed
run is retained in [ceiling-polluted.log](ceiling-polluted.log); subsequent
runs wrote outside the working tree and archived after completion.

Logic-file lint is clean ([log](lint-logic-final.log)); the broader metadata
edit set has the same 2 errors / 7 warnings at base and candidate, with no new
diagnostics ([base](lint-base.log), [candidate](lint.log)). The outline runtime
repair also passes its [focused check](prewarm-repair-green.log) and
[lint](lint-prewarm-repair.log). Verbatim census/budget/stage lines are in
[gate.md](gate.md); complete commands and archival procedure are in
[commands.md](commands.md). Every source edit is in [dogfood.md](dogfood.md).
The [final receipt check](final-verification.log) confirms that the source
digest still matches, all stages passed, and every current fast member is
covered and within its namespace and lane budgets.

## Commits, uncertainty, disagreements and owed

Implementation commits, in order: `3c774511`, `22bb9296`, `5343db60`,
`81607540`, `c6220242`, `7da1b443`. The containing commit is the final
report-only commit; [report.edn](report.edn) records full implementation SHAs.

Least sure: these are single-run timings, not variance-certified performance
estimates. The standalone fast transcript is a retained pre-repair failure;
the final prewarm supplies repaired fast-membership proof. Its positive
result does not retroactively change the standalone transcript.

No disagreement with the red-team defects. Two supporting source scans had
false positives on inert assertions; they were repaired while preserving
real constructor/call counterexamples, rather than moving pure tests.

Owed: Fable ratification of **374,149 ms** and independent acceptance. The
inherited shared CLI workspace-status output still needs bounding on dirty
trees. Encounter scoring was attempted and refused
`encounter-not-found receipt-booleans` ([log](encounter-score.log)); no
replacement registration or acceptance was invented. No further prewarm is
authorized in this attempt, and none is needed to describe the retained pass.
