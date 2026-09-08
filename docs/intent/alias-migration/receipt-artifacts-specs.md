# Receipt artifact isolation

- [x] **ALIAS-MIGRATION-001**: When a Surgeon verb publishes receipt, detail or undo artifacts, it shall place them under `/var/tmp/forge/<verb>-receipts/` outside the workspace and report their absolute paths.
- [x] **ALIAS-MIGRATION-002**: When a verb completes a write in a Git workspace, its receipt shall publish `workspace_clean_except` only after post-write `git status --porcelain --untracked-files=all` proves there are no changed paths outside the verb's changed-file set; failed or unavailable proof shall be explicit and shall preserve the actual commit state.
- [x] **ALIAS-MIGRATION-003**: When `make test` or `make landing-gate` runs, it shall execute the complete `clj-surgeon.mcp-alias-migration-test` and `clj-surgeon.receipt-artifacts-boundary-test` namespaces and propagate any failure; a battery freshness receipt shall not replace that execution.

- [x] **ALIAS-MIGRATION-004**: When an alias migration returns, its receipt shall report measured refusal_price and explicit fallback/unknown telemetry without manufacturing unobserved counts or caller verification.
- [x] **ALIAS-MIGRATION-005**: When an alias migration call completes, it shall append exactly one external telemetry ledger line, retaining the operation result and marking ledger failure unknown if publication fails.
