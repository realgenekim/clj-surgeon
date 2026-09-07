## Verdict: NO-GO for MCP/main

1. **CONFIRMED-DEFECT — blocking:** reuse bypasses `alias_policy`.

   Executed through `execute!`:

   - With `[example.new :as forbidden]`, policy `["newlib"]` committed `forbidden/fetch-event`.
   - With `[example.new :as forbidden] [example.unrelated :as newlib]`, it again committed under `forbidden` instead of refusing `alias-policy-exhausted`.

   Cause: the unrestricted `existing` fallback in [alias_migration.clj](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/alias_migration.clj:1158). It contradicts ALIAS-067’s requirement that reuse apply to a `to.alias_policy` entry and the explicit no-alias-outside-policy witness in [alias-migration-specs.md](/home/forge/src/clj-surgeon-fence/docs/intent/alias-migration/alias-migration-specs.md:124).

2. **NOT-REPRODUCED — Round 2 mixed alias/refer defect:** `[example.unrelated :as newlib]` plus `[example.new :refer [newlib]]` now refuses unchanged with single-entry policy, and selects `freshlib` with a second free entry.

3. **NOT-REPRODUCED — Round 2 refer-only defect:** refer-only likewise refuses unchanged or falls through to `freshlib`; it never writes `newlib/fetch-event`.

4. **NOT-REPRODUCED — comment/discard deletion:** a real committed `execute!` write preserved the adjacent comment and `#_[example.decoy :as d]` exactly while removing the retired libspec.

5. **NOT-REPRODUCED — alias plus different refer:** `[example.new :as newlib :refer [different]]` was retained unchanged, the old libspec was removed, and the call became `newlib/fetch-event`.

6. **NOT-REPRODUCED — `:as-alias`:** an `:as-alias` targeting `to.lib` was reused; one targeting another namespace collided and fell through to `freshlib`.

7. **NOT-REPRODUCED — locator:** through `execute!`, the unsupported-binding probe refused unchanged with `file=src/example/client.clj`, `line=3`, `col=1`, and no docstring in the error.

8. **NOT-REPRODUCED — lib-mode regression:** candidate and current `origin/MCP/main` (`0f6f474f`) produced byte-identical executed results, including the sharper case where `to.lib` already had alias `newlib`. Both selected `freshlib`, confirming reuse remains var-mode-only.

9. **JUDGMENT — pre-existing duplicate libspec:** requiring `to.lib` without a reusable alias still creates a second libspec. The behavior is valid Clojure, predates this branch, and is outside ALIAS-067’s declared scope; it is not this landing’s blocker.

10. **JUDGMENT — permanent coverage/design gap:** the checked-in preservation witness still exercises planner bytes rather than `execute!` ([test](/home/forge/src/clj-surgeon-fence/test/clj_surgeon/alias_migration_test.clj:1234)). Also, the LLD still says alias choice is simply the first non-colliding policy entry and does not document reuse ([design](/home/forge/src/clj-surgeon-fence/docs/intent/alias-migration/alias-migration-design.md:218)).

Gates:

- Alias namespaces: **186 tests, 3,983 assertions; 0 failures/errors**
- `clojure -M:clj-surgeon/test-fast`: **559 tests, 5,457 assertions; 0 failures/errors**
- `make mcp-operation-oracle`: **pass**
- `~/bin/clj-kondo --lint` changed Clojure files: **0 errors, 0 warnings**
- `git diff --check`: clean
- Dry merge with current `origin/MCP/main`: clean
- Worktree: clean at `d32a3c9d`

Landing requires removing or policy-constraining the unrestricted `existing` fallback, adding executed regression cases for off-policy target aliases, and aligning the LLD/public-boundary witness.