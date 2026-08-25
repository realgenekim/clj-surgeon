Make the generated Codex workspace configuration tolerate an unavailable cclsp service while
keeping clj-surgeon required. Add a focused regression assertion that proves the generated cclsp
table is optional. Preserve the existing loopback URL validation, enabled-tool lists, bounded
managed block, and unrelated configuration behavior.

Only modify:

- `src/clj_surgeon/workspace_onboarding.clj`
- `test/clj_surgeon/workspace_onboarding_test.clj`

Run the repository's Babashka test suite and stop when the requested behavior is verified.

