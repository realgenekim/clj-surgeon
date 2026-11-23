# clj-surgeon: what the record says (study of 2026-11-22)

Read-only study of Anvil's sessions and repositories. Every number is in a table beside this file (`counts.tsv`, `calls_per_week.tsv`, `fallback_by_op.tsv`, `refusal_codes.tsv`, `candidates.md`, `LEDGER-condensed.md`, `commits_by_verdict.tsv`).

## What agents do with the tool today

Nothing, on Anvil. The miner scanned 1,123 Codex and 680 Claude sessions and found 2,049 Surgeon calls, all between 2026-09-02 and 2026-09-15. Week one had 1,531 calls, week two 459, week three 59, then zero for nine weeks. The laptop and skiff stores are not on Anvil; this is Anvil's view only.

Of the 2,049 calls, 1,749 were CLI and 300 MCP. 1,256 were `cat` reads and 316 `ls`. Writes were a minority: `apply_clojure_changes` 118, `admit_clojure_patch` 74, `split-ns!` 62, `change!` 35, `alias_migration` 17.

## The top five frictions

Outcomes: 1,172 successes, 220 typed refusals, 86 errors, 3 timeouts, 568 unclassified (mostly CLI receipts). After 24 percent of all calls the agent fell back to a native edit in the same turn; after MCP calls, 35 percent.

1. Patch admission refused: `admit_clojure_patch` 36 refusals in 74 calls, 47 percent fallback (verification-failed 12, verification-incomplete 12, unsupported-patch-target 7).
2. Reads that miss: `cat` 106 refusals and 44 errors in 1,256 calls, 276 fallbacks; codes ambiguous-form 26, form-not-found 15, contains-not-found 14, batch-form-selection-failed 17.
3. Inspect refused or abandoned: `inspect_clojure` 19 refusals in 61 calls, 41 percent fallback (invalid-workspace-root 11, source-file-not-found 4).
4. Edits that need a repair loop: `apply_clojure_changes` 13 refusals, 16 errors, 46 retries, 32 repaired, 29 percent fallback (invalid-compact-relation 10).
5. Helper extraction never worked in the field: 7 calls, 7 errors, 7 fallbacks.

The 57-entry papercut list is about namespace-split output. It covers none of these five.

## Candidate whole-intent verbs

Native multi-action Clojure changes that clear the five-action bar (median native actions): add a require across files, 43 occurrences, median 29 actions over 17 files; extract a function, 89 occurrences, median 16; rename a namespace, 5 occurrences, median 20; alias migration done by hand, 6 occurrences, median 14.5. Arity change: 1 occurrence; moving a var: none detected. The classifier is lexical; these are counts of evidence, not confirmed refactorings.

## Where the effort went

5,994 commits: 1,396 on main, 4,598 on 554 branches, almost all between 2026-09-02 and 2026-09-24. The branches are stacked, so one commit is often reachable from several families; 2,788 commits sit under more than one verdict. Two experiments won and were routed: namespace split (56 s against 447 s native) and alias migration (1.38x). Four lost or were retired, with numbers: fan-out (0.68x and 0.59x), per-form writes (about 2x wall), the wall-clock study that explained why (87 percent model time, 3 to 4 percent tool time), and direct cclsp clients. The rest, 11 of 15 ledger rows and roughly 2,800 unique commits in the bb, verify, fence and late-September families, were never measured against native or ended in review verdicts without a control.

## Recommendation

Fix refusals only, the five above, then measure again. No candidate verb is worth building first: the two routed verbs have had no caller on Anvil for nine weeks, and a third verb with the same refusal surface would meet the same 24 to 47 percent fallback. If the refusals are fixed and the two routes still go unused, park the tool and keep the two winning routes as documented recipes.

## What I could not determine

Usage from the laptop and skiff. The meaning of the 568 unclassified CLI receipts. Whether the 1,035 "other multi-file" native turns hide a verb the classifier missed. A clean commit count per verdict: stacked branches defeat an exclusive partition without a branch-by-branch read.
