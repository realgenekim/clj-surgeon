# Receipt artifact isolation

- [x] **ALIAS-MIGRATION-001**: When a Surgeon verb publishes receipt, detail or undo artifacts, it shall place them under `/var/tmp/forge/<verb>-receipts/` outside the workspace and report their absolute paths.
- [x] **ALIAS-MIGRATION-002**: When a verb completes a write in a Git workspace, its receipt shall publish `workspace_clean_except` only after post-write `git status --porcelain --untracked-files=all` proves there are no changed paths outside the verb's changed-file set; failed or unavailable proof shall be explicit and shall preserve the actual commit state.
