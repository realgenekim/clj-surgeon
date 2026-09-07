## Verdict: NO-GO for MCP/main

### Findings

1. **JUDGMENT — blocking correctness:** alias reuse conflates namespace aliases with referred Vars. [`ns-binding-namespaces`](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/alias_migration.clj:1092) records both identically, and [`reusable?`](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/alias_migration.clj:1117) treats either as a reusable alias.

   Executed public-path counterexample:

   ```clojure
   [example.unrelated :as newlib]
   [example.new :refer [newlib]]
   [example.old :as old]
   ```

   `alias_migration` returned `ok=true`, `committed=true`, and rewrote the call to `newlib/fetch-event`. That qualifier resolves through `example.unrelated`, not `example.new`. A refer-only variant produces an unresolved qualifier. This is a silent semantic mismigration.

2. **JUDGMENT — blocking byte-fidelity regression:** [`remove-libspec`](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/alias_migration.clj:764) walks backward across every node rejected by [`meaningful?`](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/alias_migration.clj:17), including comments and reader-discard forms. An executed probe showed both an adjacent comment and `#_[example.decoy :as d]` disappearing. That contradicts [MCP-OP-ALIAS-010/011](/home/forge/src/clj-surgeon-fence/docs/intent/alias-migration/alias-migration-specs.md:33).

3. **JUDGMENT — blocking intent mismatch:** the current contract says referred names are collisions and the first non-colliding policy entry is selected ([ALIAS-007/008](/home/forge/src/clj-surgeon-fence/docs/intent/alias-migration/alias-migration-specs.md:27)). The implementation changes that behavior without updating design/EARS, and the binding-scope change is incorrectly annotated as ALIAS-008 rather than ALIAS-066.

4. **JUDGMENT — test weakness:** [`f2-refusal-locator-names-a-line-not-a-docstring`](/home/forge/src/clj-surgeon-fence/test/clj_surgeon/alias_migration_test.clj:1158) wraps all assertions in `when (= false (:ok plan))`; it passes vacuously if the planner unexpectedly succeeds. It also does not assert the claimed “not a docstring” evidence directly.

5. **JUDGMENT — correct portions:** genuine `[example.new :as newlib]` reuse works; `:reuse-keep` emits only the necessary form edit; binding-scope refusal now includes `file`, `line`, and `col`; lib-mode continues selecting the first free policy alias. These do not offset the two correctness blockers.

### Verification

- Focused equivalent: **183 tests, 3,953 assertions; 0 failures, 0 errors**.
- The requested `clojure -M:test --focus ...` invocation exits 1 because this checkout has no `:test` alias. I ran the two namespaces directly through `clojure.test` using `:clj-surgeon/test-deps`.
- `~/bin/clj-kondo --lint` on both changed files: **0 errors, 0 warnings**; one informational note.
- `git diff --check`: clean.
- Dry merge against current `origin/MCP/main` (`4c190c48`): clean.
- Worktree: clean, detached at candidate `1000e58c`.

Landing requires separating alias bindings from referred-name collisions, preserving comments/discards during libspec removal, aligning the intent documents, and adding public-path regression witnesses. I followed the repository’s in-tree [clj-surgeon skill](/home/forge/src/clj-surgeon-fence/skill.md) for structural reads and lint routing.