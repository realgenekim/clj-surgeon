# Ethnography of use: two weeks of calls, then nine weeks of silence (study of 2026-11-22)

Gene, 2026-11-22: "Search for the clj surgeon paper cut list and study it. Come up with plan for improving." Then: "look on anvil for thr gazillion commits and experiments to try to improve (but ultimately futile)". On seeing the result: "Clj surgeon use -- stats!  Amazing, add to clj-surgeon repo somewhere (there's usage data in there somewhere, ethnographic usage)".

This page sits with the other ethnographies in this folder. The earlier ones watched a few sessions closely. This one counts every session on one machine. The tables and the two scripts that made them are in `2026-11-22-usage-study/`. The full study, including the per-call table, is on the branch `study/2026-11-22`.

Scope: the builder machine ("Anvil") only. Its Codex and Claude session stores were mined read-only. The laptop's sessions, where the two surgeon seats worked, are not included.

## What was counted

| | |
|---|---|
| Sessions scanned | 1,123 Codex, 680 Claude |
| Tool calls scanned | 46,097 Codex, 64,786 Claude |
| clj-surgeon calls found | 2,049 |
| Through the CLI | 1,749 (Codex 1,627, Claude 122) |
| Through the MCP tools | 300 (Codex 155, Claude 145) |
| First call | 2026-09-02 |
| Last call | 2026-09-15 |
| Transcript lines dropped because they matched a secret pattern | 1,108 |

## The shape of use

| Week starting | Calls |
|---|---|
| 2026-08-31 | 1,531 |
| 2026-09-07 | 459 |
| 2026-09-14 | 59 |
| every week since | 0 |

Nine weeks with no call. Nobody decided to stop. The two verbs that won their measurements (namespace split, alias migration) have had no caller on this machine since mid September.

## What the calls were

- 1,256 were `cat` reads and 316 were `ls`. Reading dominated.
- Writes were a minority: `apply_clojure_changes` 118, `admit_clojure_patch` 74, `split-ns!` 62, `change!` 35, `alias_migration` 17.

## How the calls ended

| Outcome | Calls |
|---|---|
| Success | 1,172 |
| Typed refusal | 220 |
| Error | 86 |
| Timeout | 3 |
| Could not be classified (mostly CLI receipts) | 568 |

After 24% of all calls (495 of 2,049) the agent went back to a native edit in the same turn. After MCP calls it was 35% (106 of 300).

## The five frictions that the counts show

1. **Patch admission refused.** `admit_clojure_patch`: 36 refusals in 74 calls; 47% fall back to native. Codes: verification-failed 12, verification-incomplete 12, unsupported-patch-target 7.
2. **Reads that miss.** `cat`: 106 refusals and 44 errors in 1,256 calls; 276 fallbacks. Codes: ambiguous-form 26, batch-form-selection-failed 17, form-not-found 15, contains-not-found 14.
3. **Inspect refused or abandoned.** `inspect_clojure`: 19 refusals in 61 calls; 41% fall back. Codes: invalid-workspace-root 11, source-file-not-found 4.
4. **Edits that need a repair loop.** `apply_clojure_changes`: 13 refusals, 16 errors, 46 retries, 32 repaired; 29% fall back. Code: invalid-compact-relation 10.
5. **Helper extraction never worked in the field.** 7 calls, 7 errors, 7 fallbacks.

The 57-entry papercut list (`docs/intent/helper-extraction/namespace-split-papercuts.edn`) is about the output of one verb, the namespace split. It covers none of these five. The list recorded what reviewers saw in the output. These counts record what agents hit while using the tool. They are different lists.

## What agents did natively instead

Multi-action Clojure changes done with native tools, where one call could have replaced five or more actions (median native actions in brackets): add a require across files, 43 occurrences (29 actions over 17 files); extract a function, 89 (16); rename a namespace, 5 (20); alias migration by hand, 6 (14.5). The classifier matches words, so these are counts of evidence, not confirmed refactorings.

## Where the effort went

- 5,994 commits: 1,396 on main, 4,598 on 554 branches, almost all between 2026-09-02 and 2026-09-24. 38 sibling working copies.
- Two experiments won and were routed: namespace split (56 s against 447 s native) and alias migration (1.38 times).
- Four lost or were retired with numbers: fan-out (0.68 and 0.59 times native), per-form writes (about twice the wall time), direct cclsp clients, and the wall-clock study that explained why (87% of the time is the model between actions; 3 to 4% is the tool).
- 11 of the 15 experiments in the condensed ledger, about 2,800 commits, were never measured against native or ended in a review verdict with no control.
- Machine-read verdicts over 1,205 observation series: won 2, lost 8 (4 of those are prose, not experiments), inconclusive 772, never measured 423.
- The branches are stacked, so an exact count of commits for each verdict is not possible.

## What this says, in the ethnographer's voice

The tool was used hard for two weeks by the same agents that were building it. Use fell by two thirds in the second week and to almost nothing in the third. The fall follows the measurements: by 2026-09-08 the routing had been cut back to two verbs, and the instruction to prefer the tool had been withdrawn on 2026-09-02. Agents that are not told to use it do not reach for it. Agents that were told to use it read with it and wrote around it: reads are 77% of all calls.

The friction that the builders recorded and the friction that the users met are different. The builders' list is about correctness of output. The users' friction is about getting in: a read that cannot find its form, a workspace root the tool does not accept, a patch that verification will not admit.

## Open questions

- Is the laptop the same? The two surgeon seats worked there, and their sessions are not in this count.
- What are the 568 unclassified CLI receipts?
- Do the 1,035 "other multi-file" native turns hide a verb the word-matching missed?

## Decision pending

The study recommends fixing the five refusal classes and measuring again, and parking the tool if the two routes still go unused. The mayor's recommendation to Gene is to park it now and keep the two winning routes as written recipes, because nine weeks of zero calls is the measurement. Gene has not ruled yet.

## Files

`2026-11-22-usage-study/`: `SUMMARY.md`, `LEDGER-condensed.md` (15 experiments, one row each, with receipts), `LESSONS.md` (7 lessons, each tied to two or more ledger rows), `calls_per_week`, `fallback_by_op`, `refusal_codes`, `friction_top20.md`, `candidates`, `actions_to_end.md`, `counts.tsv`, `commits_by_verdict.tsv`, `siblings.tsv`, and `METHOD.md` / `METHOD-B.md` (how the sessions and the repository were mined; secret lines are dropped before parsing). The per-call table (2,049 rows), the branch table and the scripts are on the branch `study/2026-11-22` under `docs/study-2026-11-22/`.
