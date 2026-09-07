NO-GO for landing `bd854b91` on current `origin/MCP/main` (`e7d4e009`).

| Probe | Verdict | Result |
|---|---|---|
| (a) MCP behavior | NOT-REPRODUCED | Same-input MCP projections were byte-identical; matching SHA-256 `41b3bcd…b54`. |
| (b) Allowlist only shrinks | CONFIRMED-DEFECT | Removing a valid `WTL-APPLY-001` witness grew pending debt from 144 to 145 while the contract suite still passed 18/458. Repairing one allowlisted ID likewise did not force the exception to shrink. |
| (c) New prefix fails | NOT-REPRODUCED | Unwitnessed `FUTURE2-001` produced both blocking missing-witness findings and failed the focused suite. |
| (d) Amendments | JUDGMENT | Non-MCP amendments do not witness parents. Legacy MCP amendments still do, deliberately, to satisfy byte-identical MCP behavior; the claim therefore needs non-MCP qualification. |
| (e) Lane integration | NOT-REPRODUCED | `mcp-intent-contract-test` executes in `test-fast` and in `make mcp-test` through its fast lane. |
| (f) Manifest pins | NOT-REPRODUCED | Recomputed values were `990 + 449 = 1439`; intent-contract tests increased exactly 11→18. |

Additional blocker: the required paved changed-files lint exited **2**, with two unused-binding warnings in `telemetry_events.clj:243`. The author obtained exit 0 by adding `--fail-level error`, which is not the requested default lint invocation. The warnings are pre-existing, but the branch newly touches that file.

Other gates:

- Intent contract: 18 tests / 458 assertions, exit 0
- Oracle: exit 0
- Fast lane: 580 / 6030, exit 0
- `make mcp-test`: 745 / 9518, exit 0
- Dry merge: clean, exit 0; tree `d230f310…`
- `git diff --check`: exit 0

Full evidence: [surgeon-b1-review-bd854b91.md](/var/tmp/forge/lid-batch/surgeon-b1-review-bd854b91.md). Scratch worktrees were removed; no merge or push was performed.