# No-worse baseline: 08bf1d23859db426691e4018aa633edad91d6708

Rerun everything: `bash docs/quality/2026-11-22-baseline/bin/baseline.sh`. No network, coverage execution, source writes, commits, or checkouts. Python 3 stdlib drives the two pinned external tools.

Scope: `src/**/*.clj*`, 65 tracked files, 30533 physical lines (including blanks/comments). 1345 functions scored; 40 duplicate pairs. The entire `src` directory is passed explicitly; duplicate defaults: threshold 0.82, min-lines 4, min-nodes 20.

## Commands and versions

The script supplies an empty temporary `$scratch` under this artifact directory as metrics/coverage root, removes it on exit, and passes the absolute source path. Exact expanded commands (only scratch path parameterized):

```sh
~/.local/state/pr-check/venv/bin/crapper --root "$scratch" --source-root <repo>/src --use-existing-coverage
~/.local/state/pr-check/venv/bin/dryer --root "$scratch" --source-root <repo>/src
```

crapper 0.1.0: {"url": "https://github.com/realgenekim/crapper", "vcs_info": {"commit_id": "e344bb98b42d51d4ea58524b5d285591c9bfc86b", "requested_revision": "e344bb98b42d51d4ea58524b5d285591c9bfc86b", "vcs": "git"}}
dryer 0.1.0: {"url": "https://github.com/realgenekim/dryer", "vcs_info": {"commit_id": "99009614bbcac144863982da931159042f9b73b6", "requested_revision": "99009614bbcac144863982da931159042f9b73b6", "vcs": "git"}}

`--use-existing-coverage` reads the empty scratch root: no coverage exists. Crapper assigns missing functions 0% and computes **CRAP = CC² + CC**. These are conservative assumed-zero scores, not measured test coverage or evidence that functions are untested. No test suite was run.

## Same-commit control

crapper: stdout diff empty (0 bytes); stderr byte-identical; no stripping in either control.
dryer: stdout diff empty (0 bytes); stderr byte-identical; no stripping in either control. The `.diff` files retain the empty diffs; `.txt` files are raw stdout. Stderr is retained separately with only the temporary root replaced by `$scratch` for a stable artifact. Both invocations share one unchanged source snapshot and scratch root.

## Layout limits

The tools accept `.cljc`, but this snapshot has 0 such files under src, so reader-conditional correctness is untested. `bb.edn` is configuration, not a scored source file; scripts outside src and `test-fixtures/` are excluded by the requested scope. Crapper's default Clojure coverage launcher assumes `clj -M:cov --lcov`, which is not evidence of compatibility with this repository's Babashka tasks; it is bypassed. Dryer's top-level forms include non-functions. Structural fingerprints are candidates, not proof of equivalent behavior. No observed parse warnings are hidden; see stderr reports.

## 30 most complex functions

| Function | File | CC | CRAP (assumed 0% coverage) |
| --- | --- | --- | --- |
| normalize-refusal | src/clj_surgeon/mcp_contract.clj | 74 | 5550.0 |
| parse-query | src/clj_surgeon/structural_lens.clj | 56 | 3192.0 |
| apply-close-plan-file! | src/clj_surgeon/worktree_lifecycle_io.clj | 53 | 2862.0 |
| validate-change! | src/clj_surgeon/mcp_contract.clj | 47 | 2256.0 |
| editor-gestures->direct-params | src/clj_surgeon/mcp_contract.clj | 45 | 2070.0 |
| apply-prune-plan-file! | src/clj_surgeon/worktree_lifecycle_io.clj | 44 | 1980.0 |
| concise-summary | src/clj_surgeon/mcp_tool.clj | 43 | 1892.0 |
| select-form | src/clj_surgeon/show_form.clj | 42 | 1806.0 |
| validate-change! | src/clj_surgeon/intent_transaction.clj | 41 | 1722.0 |
| execute-explicit-change! | src/clj_surgeon/mcp_tool.clj | 38 | 1482.0 |
| validate-receipt! | src/clj_surgeon/intent_transaction.clj | 37 | 1406.0 |
| insertion-gap | src/clj_surgeon/intent_transaction.clj | 35 | 1260.0 |
| inspect-summary | src/clj_surgeon/mcp_inspect_tool.clj | 35 | 1260.0 |
| execute-request-in-context! | src/clj_surgeon/mcp_tool.clj | 35 | 1260.0 |
| apply-basis! | src/clj_surgeon/mcp_change_buffer.clj | 34 | 1190.0 |
| delete-subform-source | src/clj_surgeon/mcp_change_buffer.clj | 34 | 1190.0 |
| validate-read-spec | src/clj_surgeon/show_form.clj | 34 | 1190.0 |
| validate-prune-plan | src/clj_surgeon/worktree_lifecycle.clj | 34 | 1190.0 |
| compile-plan | src/clj_surgeon/extract.clj | 31 | 992.0 |
| execute-extraction! | src/clj_surgeon/mcp_tool.clj | 31 | 992.0 |
| -main | src/clj_surgeon/core.clj | 29 | 870.0 |
| execute-change-with-context! | src/clj_surgeon/intent_transaction.clj | 29 | 870.0 |
| classify-target | src/clj_surgeon/worktree_lifecycle.clj | 29 | 870.0 |
| kernel-refusal | src/clj_surgeon/mcp_inspect.clj | 27 | 756.0 |
| compile-prune-plan | src/clj_surgeon/worktree_lifecycle.clj | 27 | 756.0 |
| matching-edits | src/clj_surgeon/intent_transaction.clj | 25 | 650.0 |
| prepare-exact-owner! | src/clj_surgeon/mcp_change_buffer.clj | 25 | 650.0 |
| validate-outcome | src/clj_surgeon/operation_algebra.clj | 25 | 650.0 |
| analyze-file | src/clj_surgeon/rename.clj | 25 | 650.0 |
| plan | src/clj_surgeon/fix_declares.clj | 24 | 600.0 |

## 20 strongest duplicate pairs

| Score | Left (inclusive lines) | Right (inclusive lines) |
| --- | --- | --- |
| 1.00 | <repo>/src/clj_surgeon/binding_rename.clj:18-21 | <repo>/src/clj_surgeon/mcp_formatter.clj:26-29 |
| 1.00 | <repo>/src/clj_surgeon/binding_rename.clj:70-76 | <repo>/src/clj_surgeon/intent_transaction.clj:1146-1152 |
| 1.00 | <repo>/src/clj_surgeon/cljc/require_ops.clj:23-30 | <repo>/src/clj_surgeon/cljc/split.clj:62-69 |
| 1.00 | <repo>/src/clj_surgeon/cljc/require_ops.clj:32-38 | <repo>/src/clj_surgeon/cljc/split.clj:71-77 |
| 1.00 | <repo>/src/clj_surgeon/cljc/require_ops.clj:32-38 | <repo>/src/clj_surgeon/mcp_compact_relations.clj:391-397 |
| 1.00 | <repo>/src/clj_surgeon/cljc/split.clj:22-26 | <repo>/src/clj_surgeon/outline.clj:195-199 |
| 1.00 | <repo>/src/clj_surgeon/cljc/split.clj:71-77 | <repo>/src/clj_surgeon/mcp_compact_relations.clj:391-397 |
| 1.00 | <repo>/src/clj_surgeon/edit_dsl.clj:170-173 | <repo>/src/clj_surgeon/edit_dsl.clj:175-178 |
| 1.00 | <repo>/src/clj_surgeon/edit_dsl.clj:170-173 | <repo>/src/clj_surgeon/edit_dsl.clj:220-223 |
| 1.00 | <repo>/src/clj_surgeon/edit_dsl.clj:175-178 | <repo>/src/clj_surgeon/edit_dsl.clj:220-223 |
| 1.00 | <repo>/src/clj_surgeon/fields.clj:95-106 | <repo>/src/clj_surgeon/fields.clj:153-162 |
| 1.00 | <repo>/src/clj_surgeon/fix_declares.clj:27-34 | <repo>/src/clj_surgeon/fix_declares.clj:36-43 |
| 1.00 | <repo>/src/clj_surgeon/intent_transaction.clj:35-43 | <repo>/src/clj_surgeon/structural_lens.clj:951-959 |
| 1.00 | <repo>/src/clj_surgeon/intent_transaction.clj:95-101 | <repo>/src/clj_surgeon/mcp_program_tool.clj:76-82 |
| 1.00 | <repo>/src/clj_surgeon/intent_transaction.clj:2792-2797 | <repo>/src/clj_surgeon/intent_transaction.clj:2799-2804 |
| 1.00 | <repo>/src/clj_surgeon/mcp_compact_edit_fields.clj:19-24 | <repo>/src/clj_surgeon/mcp_contract.clj:122-127 |
| 1.00 | <repo>/src/clj_surgeon/mcp_compact_relations.clj:51-54 | <repo>/src/clj_surgeon/mcp_compact_relations.clj:56-59 |
| 1.00 | <repo>/src/clj_surgeon/mcp_contract.clj:129-133 | <repo>/src/clj_surgeon/mcp_inspect.clj:49-53 |
| 1.00 | <repo>/src/clj_surgeon/mcp_contract.clj:176-180 | <repo>/src/clj_surgeon/mcp_inspect.clj:126-130 |
| 1.00 | <repo>/src/clj_surgeon/mcp_contract.clj:182-186 | <repo>/src/clj_surgeon/mcp_inspect.clj:132-136 |
