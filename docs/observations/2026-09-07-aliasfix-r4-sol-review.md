## Verdict: NO-GO for MCP/main

No functional alias-migration defect was reproduced. The sole blocker is the required lint gate: it exits 2 with two warnings in changed files. Both warnings are pre-existing and reproduce byte-for-byte on `origin/MCP/main`, but the specified gate is not green.

1. **NOT-REPRODUCED — finding 1, write case.**  
   `[to.lib :as forbidden]`, policy `["newlib"]`, committed `[to.lib :as newlib]` and `newlib/fetch-event`; it never emitted `forbidden/fetch-event`.

2. **NOT-REPRODUCED — finding 1, refusal case.**  
   Adding `[example.unrelated :as newlib]` returned `alias-migration-alias-policy-exhausted`, `source_unchanged=true`, and byte-identical source.

3. **NOT-REPRODUCED — two-entry ordering.**  
   I tested both relevant orderings:

   - Off-policy target alias `forbidden`, `newlib` bound elsewhere, `freshlib` free → committed `freshlib/fetch-event`.
   - First policy entry `newlib` already bound to `to.lib`, second entry free → reused `newlib`, removed the old libspec, and did not introduce `freshlib`.

4. **NOT-REPRODUCED — `:as-alias`.**  
   `[to.lib :as-alias newlib]` with `newlib` in policy was reused correctly and preserved as `:as-alias`.

5. **NOT-REPRODUCED — prefix trap.**  
   Through `execute!`, `acid.fanout.store → acid.fanout.store2` removed the exact old libspec, produced `newlib/fetch-event`, and preserved `acid.fanout.storehouse/keep`.

6. **NOT-REPRODUCED — comment and `#_` preservation.**  
   The new real-write `execute!` witness passed in the focused suite.

7. **NOT-REPRODUCED — lib-mode drift.**  
   Candidate and freshly fetched `origin/MCP/main` at `6a25e338` produced byte-identical results:

   - Client SHA-256: `35dbf26d53093ec4dc4b8984fa79adc6ee2bb6536174f305770523a1c5317f30`
   - Renamed definition SHA-256: `57eadf4e3a30036d9b1c7727e43c43b84cf4ddb29232a09bc2e0bfdc42497a6e`

8. **NOT-REPRODUCED — design gap.**  
   The alias-choice section now explicitly documents alias-map-only, policy-restricted, var-mode-only reuse in [alias-migration-design.md](/home/forge/src/clj-surgeon-fence/docs/intent/alias-migration/alias-migration-design.md:218).

9. **JUDGMENT — duplicate target libspec.**  
   Case A still creates two aliases for one target library. This is valid Clojure, pre-existing, and outside this branch’s declared scope.

10. **JUDGMENT — lint baseline debt, blocking under the stated gates.**  
    `~/bin/clj-kondo --lint` reports:

    - Unused `ctx` in [lane_manifest_test.clj](/home/forge/src/clj-surgeon-fence/test/clj_surgeon/lane_manifest_test.clj:211)
    - Unused `details` in [mcp_alias_migration_test.clj](/home/forge/src/clj-surgeon-fence/test/clj_surgeon/mcp_alias_migration_test.clj:3650)

    Both warnings predate this branch and appear identically on `origin/MCP/main`, but lint exits 2.

Gates:

- Both alias namespaces: **189 tests, 4,001 assertions; 0 failures/errors**
- `clojure -M:clj-surgeon/test-fast`: **559 tests, 5,457 assertions; 0 failures/errors**
- `make mcp-operation-oracle`: **pass**
- `~/bin/clj-kondo --lint` changed Clojure files: **0 errors, 2 warnings; exit 2**
- `git diff --check`: **clean**
- Dry merge with `origin/MCP/main` `6a25e338`: **clean**
- Worktree: **clean at `f17b50e1`**

If the two baseline lint warnings are explicitly waived, the functional verdict is GO. Under the gates as written, it remains NO-GO.