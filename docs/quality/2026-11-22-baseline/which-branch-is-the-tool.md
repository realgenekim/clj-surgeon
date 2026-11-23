# Which branch is the tool?

Offline local-ref snapshot; rerun `bash docs/quality/2026-11-22-baseline/bin/branches.sh`. No checkout/fetch/merge. Main means origin/main = 08bf1d23859db426691e4018aa633edad91d6708 (the local main of this clone is affdfe3fd59a63b3fbbb50da16b82509fafea907, the fork point of every large branch except the experiment family, which forks earlier; see the merge-base column). Full measured ref SHAs: [branch-refs.tsv](branch-refs.tsv).

Counts use git ls-tree + git show: physical lines including blanks/comments in src/**/*.clj* and test/** (all tracked file types); deftest is textual `(deftest` count in test/**/*.clj*. Merge bases use git merge-base origin/main REF and git show -s --format=%cs; ahead uses git rev-list --count origin/main..REF. Operations are :name registrations (including same-file def string constants), CLI operation map keys or :aliases hits in core/registry/dispatch/MCP source; this is presence evidence, not a runtime or behavioral check. Full hit locations: [branch-operations.tsv](branch-operations.tsv). Only the 15 requested spellings are scanned; omitted spellings had no hits.

| Ref | Src lines | Test lines | deftest | Operation spellings present | Merge base / date | Not on main |
| --- | --- | --- | --- | --- | --- | --- |
| origin/main | 30533 | 35262 | 1173 | ls, cat, xray, apply_clojure_changes, edit_clojure, inspect_clojure | 08bf1d23859d 2026-11-22 | 0 |
| origin/astra/namespace-split | 69971 | 96560 | 2555 | ls, cat, xray, alias_migration, split-ns!, apply_clojure_changes, admit_clojure_patch, edit_clojure, helper_extraction, inspect_clojure, feature_thread, require_change, namespace_split | affdfe3fd59a 2026-09-04 | 2493 |
| origin/MCP/main | 73906 | 110468 | 2777 | ls, cat, xray, alias_migration, split-ns!, apply_clojure_changes, admit_clojure_patch, edit_clojure, helper_extraction, inspect_clojure, feature_thread, require_change, namespace_split, insert-forms!, rename-alias! | affdfe3fd59a 2026-09-04 | 2907 |
| origin/MCP/astra-helper-performance-2026-09-05 | 52368 | 70067 | 1812 | ls, cat, xray, alias_migration, apply_clojure_changes, admit_clojure_patch, edit_clojure, inspect_clojure | affdfe3fd59a 2026-09-04 | 971 |
| origin/astra/gate-lanes-receipt-2dfbc286 | 70306 | 99742 | 2581 | ls, cat, xray, alias_migration, split-ns!, apply_clojure_changes, admit_clojure_patch, edit_clojure, helper_extraction, inspect_clojure, feature_thread, require_change, namespace_split | affdfe3fd59a 2026-09-04 | 2552 |
| origin/bb-rewrite-tower | 72908 | 106313 | 2688 | ls, cat, xray, alias_migration, split-ns!, apply_clojure_changes, admit_clojure_patch, edit_clojure, helper_extraction, inspect_clojure, feature_thread, require_change, namespace_split, insert-forms!, rename-alias! | affdfe3fd59a 2026-09-04 | 2801 |
| origin/bb-rewrite-tower-land | 73009 | 106487 | 2691 | ls, cat, xray, alias_migration, split-ns!, apply_clojure_changes, admit_clojure_patch, edit_clojure, helper_extraction, inspect_clojure, feature_thread, require_change, namespace_split, insert-forms!, rename-alias! | affdfe3fd59a 2026-09-04 | 2805 |
| origin/bridge/apply-expect-honoured | 66865 | 89604 | 2350 | ls, cat, xray, alias_migration, apply_clojure_changes, admit_clojure_patch, edit_clojure, helper_extraction, inspect_clojure, feature_thread | affdfe3fd59a 2026-09-04 | 2104 |
| origin/experiment/result-decision-chord-screen | 24293 | 28020 | 949 | ls, cat, xray, apply_clojure_changes, edit_clojure, inspect_clojure | 543798a140a1 2026-08-26 | 329 |
| origin/fable/regns-entrance | 74505 | 109760 | 2776 | ls, cat, xray, alias_migration, split-ns!, apply_clojure_changes, admit_clojure_patch, edit_clojure, helper_extraction, inspect_clojure, feature_thread, require_change, namespace_split, insert-forms!, rename-alias! | affdfe3fd59a 2026-09-04 | 2905 |
| origin/records/MCP-main | 70080 | 96663 | 2556 | ls, cat, xray, alias_migration, split-ns!, apply_clojure_changes, admit_clojure_patch, edit_clojure, helper_extraction, inspect_clojure, feature_thread, require_change, namespace_split | affdfe3fd59a 2026-09-04 | 2969 |
| origin/skiff/darwin-passwd-home | 73468 | 107395 | 2720 | ls, cat, xray, alias_migration, split-ns!, apply_clojure_changes, admit_clojure_patch, edit_clojure, helper_extraction, inspect_clojure, feature_thread, require_change, namespace_split, insert-forms!, rename-alias! | affdfe3fd59a 2026-09-04 | 2847 |
| origin/skiff-worker/7h2-onboarding-required-false | 73906 | 110468 | 2777 | ls, cat, xray, alias_migration, split-ns!, apply_clojure_changes, admit_clojure_patch, edit_clojure, helper_extraction, inspect_clojure, feature_thread, require_change, namespace_split, insert-forms!, rename-alias! | affdfe3fd59a 2026-09-04 | 2906 |
| origin/study/2026-11-22 | 70080 | 96663 | 2556 | ls, cat, xray, alias_migration, split-ns!, apply_clojure_changes, admit_clojure_patch, edit_clojure, helper_extraction, inspect_clojure, feature_thread, require_change, namespace_split | affdfe3fd59a 2026-09-04 | 2997 |

Remote families with >100 commits not on main (prefix after origin/, before first /; slashless names are their own family; excludes the two explicitly named remote refs and symbolic HEAD). Ties use lexically greatest ref.

| Family | Branches | Most-ahead representative | Commits |
| --- | --- | --- | --- |
| MCP | 6 | origin/MCP/astra-helper-performance-2026-09-05 | 971 |
| astra | 26 | origin/astra/gate-lanes-receipt-2dfbc286 | 2552 |
| bb-rewrite-tower | 1 | origin/bb-rewrite-tower | 2801 |
| bb-rewrite-tower-land | 1 | origin/bb-rewrite-tower-land | 2805 |
| bridge | 28 | origin/bridge/apply-expect-honoured | 2104 |
| experiment | 6 | origin/experiment/result-decision-chord-screen | 329 |
| fable | 136 | origin/fable/regns-entrance | 2905 |
| records | 1 | origin/records/MCP-main | 2969 |
| skiff | 1 | origin/skiff/darwin-passwd-home | 2847 |
| skiff-worker | 1 | origin/skiff-worker/7h2-onboarding-required-false | 2906 |
| study | 1 | origin/study/2026-11-22 | 2997 |

`git merge-tree --write-tree origin/main origin/astra/namespace-split`: exit 0; 0 conflicting files. No textual merge conflicts, because origin/main is only 2 commits (documentation) past the fork point; a merge would import the whole branch (1385 files changed, 281072 insertions(+), 2847 deletions(-)), so taking the two winning verbs alone is a port, not a merge. Git object writes were redirected to temporary artifact-local storage; refs, index and worktree were untouched. This checks the entire branch, not a cherry-pick of just two features. Full output: [merge-tree.txt](merge-tree.txt). First 20 conflicting paths:

None.
