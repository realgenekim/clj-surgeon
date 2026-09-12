# Block B attempt24 — GO for branch handoff

Sol round-four F1 is repaired: the probe now discovers dependencies through the
shared form-identity ns parser and refuses unclassified require forms before
reload. The final real prewarm passed every required stage. The exact red
reproduction is committed separately. This is branch-only
work; no push, merge, budget change, runtime reclassification or test removal.

## Contract and evidence

The shared parser expands recursive list/vector prefixes, vector libspecs, bare
symbols, string libraries, require/macro/use clauses, JVM/default reader branches
and spliced conditionals. It ignores discarded forms and does not let a quoted
ns hide the actual top-level ns. Unknown forms refuse as
`:probe-require-unparsed` with `:form` and `:file`. The refusal registry names
the prevented native failure: a probe reporting green over a dependency it
never reloaded. Source size and canonical test-target authorization still run
before reload. Local .cljc dependencies are eligible alongside .clj.

The old code returns `[demo.probe-test]` for Sol's temporary project; the red
witness expects `[foo.bar demo.probe-test]`. [red.log](red.log) is committed in
92fe4252 before the implementation. The stronger execution witness first loads
a dependency with value 1, writes value 2 on disk, and sees the probe fail its
old-value assertion. An unknown dependency spec then refuses with zero observed
reload calls. This witnesses actual stale-image behavior, not only graph shape.

The receipt freezes `:closure-expected` before executing reloads. It survives
reload failure and bounded HTTP projection independently of `:reloaded`.
[A real worktree probe](real-probe-receipt.log) reports three names, expected
count 3, 24 tests / 94 assertions, no failures, and pending landing-gate.
The public verdict remains inner-loop evidence without verification_complete.

## Focused verification

| Check | Result | Receipt |
|---|---|---|
| Probe, HTTP, shared parser compatibility, refusal completeness, boolean contract | 210 tests / 4,861 assertions; zero failures/errors | focused-all.log |
| Sol's exact nine Vars | 9 tests / 1,135 assertions; zero failures/errors | sol-nine.log |
| Pinned reachable refusal census | 1 test / 4 assertions; 163 registered kinds | refusal-census.log |
| Census and unchanged sleep anchor after registration repair | 2 tests / 9 assertions; zero failures/errors | census-repair.log |
| Legacy help receipt oracle after diagnostic repair | 1 test / 23 assertions; zero failures/errors | help-repair.log |
| Lint through ~/bin/clj-kondo | Zero errors/warnings; two informational messages in existing alias tests | lint.log |

The stale-image regression intentionally emits an inner failed test, and asserts
that failed probe verdict. Its enclosing test is green. Initial test-authoring
delimiter and fixture-input errors are retained in focused-first/second.log;
the repair log explains both and the one empty fixture directory cleanup.

## Full checks

The single standalone fast run executed 1,244 tests / 12,560 assertions, with
three registration failures and zero errors. Its 45,425 ms fast sum was below
the unchanged 60,000 ms ceiling. The four new test names were absent from the
derived census and their insertion shifted one existing sleep from line 288 to
384. Commit 58b33e65 registers exactly those facts: four additions, no removals,
and no timing or membership change. The standalone fast suite was not repeated.
Evidence: checks/fast.log, fast-run/receipt.edn, census-repair.log.

The single restricted diagnostic exited 2 after 172 wrapper seconds
(171.65125091467053 seconds inside the diagnostic wrapper). Its MCP suite passed
1,420 tests / 16,487 assertions, and alias passed 182 / 3,662. The bb suite ran
902 tests / 8,029 assertions with one failure: the legacy help receipt oracle
omitted closure-expected. Its expected map was updated with literal count 1 in
c53fd9c5, and the focused witness passed. Evidence: restricted-gate.md,
restricted-run/, restricted-summary.edn, help-repair.log.

The diagnostic's runtime sums meet every budget: fast 46,281 ms, integration
67,183 ms and bb runtime 243,172 ms. These overlapping dimensions are never
added. Its source digest is
`b672e951a0267f91c58fef48598c78cbcb83d15b36e6f363f09d4578e4b941df`.
The coordinator refuses at the failed runtime pool and does not emit an overall
receipt; the preserved per-suite receipts and complete log are the evidence.
The diagnostic was not repeated.

The first real prewarm exited 2 after 172 wrapper seconds. All assertions and
budgets passed, but the MCP suite reported four TEST-ISO-003 working-tree
violations: this agent created restricted-summary.edn and updated source.patch
while two test namespaces were taking purity snapshots. This is an observer
error, not an excused gate pass. Those writes are named verbatim in
prewarm-first-gate.md; all per-suite receipts are in prewarm-first-run/.
The operational repair is to finish artifacts before launching the allowed
final prewarm and perform no workspace writes while it runs. No test, budget or
purity check is changed.

The final allowed real prewarm **passed**, with every required stage exit zero
and `:problems []`: 398,053 coordinator ms / 400 wrapper seconds. The first real
run and final retry have the identical source digest
`60b573b5d2693d14703925855429ab0733ad0d264647346b22a58538cf2c5384`,
and both name c53fd9c59b44e3e72b4c9fd9ee4203d7018de01d. The final evidence commit
changes only attempt24 artifacts, leaving the tested source snapshot intact.

| Final prewarm suite | Tests / assertions | Outcome | Relevant sum / ceiling |
|---|---|---|---|
| MCP | 1,420 / 16,487 | Zero failures/errors/isolation/leak violations | fast 46,547 / 60,000 ms; integration 67,157 / 240,000 ms |
| bb runtime suite | 902 / 8,029 | Zero failures/errors/isolation/leak violations | bb runtime 242,146 / 343,102 ms |
| Alias | 182 / 3,662 | Zero failures/errors/isolation/leak violations | battery cadence 86,337 / 1,800,000 ms |

The post-pool bb diagnostic also passed, taking 224,319 ms. It is a required
stage of the same gate, not an extra prewarm. Recovery-battery prerequisites,
repository hygiene and intent audit all passed. [gate.md](gate.md) is the
verbatim complete final log, including every refusal-census and budget line;
gate.sha256 records equality with checks/prewarm-final.log. Machine receipts:
prewarm.edn, prewarm-summary.edn and prewarm-final-run/.

Run counts are explicit: one standalone fast suite, one restricted diagnostic,
two real prewarms (initial plus the allowed final retry). No failed run is
discarded or relabeled. No further retry was used.

## Commits and scope

- c40cb1ac: preserve pending battery row and move run11 records as requested.
- 92fe4252: exact Sol reproduction, committed red.
- 5674f516: shared parser, typed refusal, receipt count and behavioral witnesses.
- 58b33e65: census additions and unchanged sleep anchor registration.
- c53fd9c5: legacy exact receipt expectation includes the requested count.
- Final attempt24 evidence commit: the commit containing this completed report.

[Dogfood table](dogfood.md) records every source edit, mechanism and repair.
[Commands](commands.md), [source patch](source.patch), [Sol's verdict](sol-verdict.md)
and all raw check artifacts are retained here. The owned project nREPL was
stopped before the full suites. No protected shared port or shared install was
touched; all JVMs had explicit heaps and temp roots under /var/tmp.

Independent Fable/Sol fence review remains owed. The historical fold, bb ceiling
derivation and portability controls were not remeasured; Sol's nine witnesses
and the requested gates exercise the continuing contracts. No speed claim or
shipping authorization is inferred from this build.

Least sure: independent review must assess the expanded shared parser beyond
the witnessed shape matrix; passing traceability and gates are not independent
fence certification. The existing parser did not already implement every shape
listed in the request, so this change extends that shared parser rather than
introducing another probe-only one. There is no disagreement with the required
closure guarantee. The run11 battery failure is preserved as historical evidence,
not claimed as repaired by this probe-focused attempt.
