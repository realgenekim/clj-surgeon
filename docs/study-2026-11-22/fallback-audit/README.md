# Fall-back labels, corrected by hand (phase 1, step C)

Source: calls.tsv rows for every split-ns! and alias_migration call (80) and 60 cat calls sampled with random.Random(20261122); context extracted by bin/extract-context.py (rows.md, rows.tsv); labels by the seat after reading each row's call, output and next six actions (labels.tsv, one reason per row).

Labels: used = the result fed the next actions; repaired = a refusal or error followed by a corrected call that was then used; abandoned = the tool was left and the task finished natively; unrelated = the next actions are not about this call.

| op | rows | used | repaired | abandoned | unrelated | study "fallback" | corrected abandonment |
|---|---|---|---|---|---|---|---|
| split-ns! | 62 | 48 | 5 | 2 | 7 | 39 of 62 (63%) | 2 of 62 (3%) |
| alias_migration | 18 | 9 | 0 | 9 | 0 | 1 of 17 (6%), 8 refusals | 9 of 18 (50%) |
| cat | 60 | 44 | 6 | 1 | 9 | 276 of 1,256 (22%) | 1 of 60 (2%) |

## What the miner counted as a fall-back
- split-ns!: the adoption cohort (row 5, plan 2 cell C) parsed every receipt with small python scripts and inspected the tree with git diff; the miner called a python heredoc a native edit. Only arm F2 (rows 27, 28) left the tool and wrote the destination namespace by hand.
- alias_migration: the opposite error. The eight refusals (unknown-verification-profile, then alias-migration-indirect-reference) in four cohort tasks were followed by apply_clojure_changes attempts and python rewrites inside Codex exec wrappers the miner could not resolve, so it counted one fall-back where there were four abandoned tasks. Every successful migration (9 of 18 rows) was used: tests ran next.
- cat: one refusal was abandoned for rg; six refusals or errors were repaired with a corrected selector and used; nine rows had no related next action; the rest fed the work.

## Three examples per label (row numbers in rows.md)
- used: 9 (alias migration, 21 files, then bin/fan-test and bb test/load_all); 26 (split-ns! cell C D2, then the kaocha focused suite on the split tree); 91 (cat of alias_migration.clj forms, then a REPL check of the same functions).
- repaired: 136 (cat refused ambiguous-form, five corrected cats on the same file); 120 (cat refused with remedies, corrected :forms call); 74 (split-ns! EXIT=1, a no-op verification profile written, the next call committed).
- abandoned: 1 (alias_migration refused twice, requires rewritten by python); 27 (split-ns! F2, destination file created by hand with cat > and sed -i); 82 (cat refused forward-reference-analysis-failed, rg then apply_patch).
- unrelated: 49 (a process observer counting clj-surgeon processes); 107 (git worktree remove after a review); 130 (a message to the mayor, nothing else in the turn).
