# Namespace split: Cell C paper-cut round

Recorded 2026-09-07T23:35:05.714540+00:00 on `astra/namespace-split`.

The baseline/candidate lint gate now refuses new error findings before publication.
Error type/message multisets ignore moved coordinates; removed different errors
cannot cancel newly introduced errors. Analyzer output without a findings vector
refuses. Receipt rows label nonzero baseline lint explicitly and show baseline,
post-candidate counts, signed delta and introduced error count.

Caller requires are spliced at their lexical library position with inherited
indentation, removing retired standalone lines. No existing require or comment is
reprinted or reordered. Destination ns docstrings retain their raw string tokens.
The receipt reports surviving prose as advisory candidate file/line/text rows,
including the library short name in callers with no direct require. The fixture's
profile explicitly runs its owner oracle before kaocha. Profile order and the
shared fail-fast/rollback mechanism are preserved, and no verification is removed.
Rows now name `cc-oracle.py` and `kaocha unit` with their own exits and measured wall.

## Intent witnesses

The append-only registry is `docs/intent/helper-extraction/namespace-split-papercuts.edn`.
INTENT/INTENT-TEST links are checked in both directions in the ordinary suite.

| ID | Permanent behavioral witnesses |
|---|---|
| NS-SPLIT-016 | `baseline-lint-is-relative-and-new-errors-block`, `candidate-lint-blocks-before-publication`; unchanged errors, removed errors, increased multiplicity, changed error at equal total, warnings, missing findings, unchanged bytes on refusal |
| NS-SPLIT-017 | `caller-requires-have-no-layout-paper-cuts`; first/middle/last retired require × three indentation styles, inline block, whole-string untouched-line equality |
| NS-SPLIT-018 | `retired-prose-is-advisory`; comments, multiline strings, full lib, alias, short-name prose without a require, unrelated prefixes, exact candidate line coordinates |
| NS-SPLIT-019 | `destination-docstrings-preserve-token-spelling`; physical newlines, escaped quotes and literal backslash-n |
| NS-SPLIT-020 | `proof-rows-name-the-command`; actual argv, exit, wall and readable command label |
| NS-SPLIT-021 | `early-oracle-failure-restores-without-suite`; first-command failure restores source/destinations and skips kaocha using the existing shared runner |

Eight new tests, including `paper-cut-intents-cannot-evaporate`. Initial retained
red run: 23 tests / 178 assertions, 22 failures and one error (the old boundary
published and deleted the source despite injected candidate lint damage).
Missing-findings follow-up was separately red before its guard. Final focused run:
23 tests / 209 assertions, zero failures/errors. The fail-fast runner itself was
already correct; the new regression exercises its use by the split boundary.
Marker audit proves traceability; the independent literal and fixture witnesses
provide behavioral evidence.

## Fresh-fixture acceptance

Created `/var/tmp/forge/plan2/build/cc-papercuts` from curtaincall-cfp `d9205abc`.
Copied D1-request.edn, changed only workspace_root. Copied the supplied architecture
oracle from D1. Configured the same two profile commands in owner-oracle-first
order and repointed the oracle root. No `cc-split-*` worktree was modified.
The captured snapshot hash matches D1/D2:
`43b15c857ebcc69b0a4bb8c051d13a319b6eed823a6330a382191e1bb762f938`.

Exact invocation:

```sh
bb --classpath src -m clj-surgeon.core :op :split-ns! :request-file /var/tmp/forge/plan2/build/papercuts-evidence/request.edn
```

Exit 0, committed, verification_complete true: 141 forms, 20 destinations,
five caller files, 87 sites, 26 changed files. All four oracle lines verbatim
(ANSI color bytes removed from kaocha output):

```text
231 tests, 2258 assertions, 0 failures.
views.clj gone: PASS
PASS: views.clj gone; all 141 owners occur exactly once in their 20 listed destination files.
1 tests, 7 assertions, 0 failures.
```

The separate `bin/kaocha unit` and focused architecture commands both exited 0.
Independent layout proof confirms sorted three-space requires, no whitespace-only
retired line and all 20 ns docstring tokens byte-identical to the original.
The receipt contains the three expected advisory prose mentions: server.clj at
candidate lines 442/1772 and replay_test.clj at 286.

Lint: baseline 108 errors / 195 warnings / 5 info; candidate 108 / 199 / 5;
introduced errors 0. Four net extra warnings are advisory, not hidden. The retained
diagnostic comparison lists six additions and two removals: some import/private
Var warnings change with the partition; there is also a static unresolved-store
namespace warning. These were not treated as error-class split damage.

## Wall and retained evidence

| Measurement | In-call wall | Kaocha unit |
|---|---:|---:|
| D1 original | 30.665544 s | 22.197205 s |
| D2 original | 30.382231 s | 21.835361 s |
| Final paper-cut invocation | 33.866225 s | 21.477374 s |

Final candidate lint costs 3.375491 s; baseline analysis 3.438739 s, parse
0.032634 s, owner oracle 0.046679 s. The successful call is about 3.2–3.5 s longer
than the original calls because it now proves the candidate lint delta. This is a
single functional replay, with other host/gate work active, not a replicated
comparative timing claim. Complete fresh-caller wall has not been remeasured.
The first repair replay took 50.430160 s because the new prose scan redundantly
outlined source and recursively converted lists to values. Reusing the compiler's
parsed inputs and scanning only string/comment nodes removed that avoidable cost;
the first receipt and its guarded restore are retained, not overwritten.

Artifacts: `/var/tmp/forge/plan2/build/papercuts-evidence/` contains the red/green
logs, original and final receipts, details and inverse, request, staged complete
fixture diff, server diff, four oracle outputs, layout proof, warning investigation
and branch test/lint logs. The fresh fixture is retained there for review.

Formatting: Standard Clojure Style on the changed compiler, boundary and test file.
`~/bin/clj-kondo --lint src test`: no diagnostics in any of the four touched Clojure
files; the broad scan retains 29 errors / 68 warnings in untouched files/fixtures.
The first branch gate found a missed adopted-test subtotal pin; that accounting
value was corrected alongside the per-namespace and total census.

Final `make test` exited 0: JVM fast/integration 776 tests / 9,784 assertions;
Babashka 873 tests / 7,511 assertions; all zero failures/errors. Operation oracle,
sentinel intent audit, isolation, recovery battery and repository hygiene passed.
The standing battery freshness gate accepted its recorded full battery; this run
did not rerun that separate battery. Final gate log: `make-test-final.log`.
