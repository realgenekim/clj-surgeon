# Census round 9 — Sol executed re-check of bridge/census-verb at 459f46e (2026-09-03T11:19Z)

Verdict: **NO-GO**. Item 2 (doors → files/root ordering) CLOSED; item 1 PARTIAL blocking (bb entrance stats the workspace via `forms/init-from-file!` before `invalid-pool-size`; counter witness wraps later fns); item 4 OPEN blocking, introduced in round 9 (next_call omits workspace_root → replay censuses the server default root, 370 files); item 6 diagnostic (accepted-fields list omits workspace_root). Round 10 launched.

## Sol verdict, verbatim

# NO-GO

The MCP ordering defect is closed, but the Babashka entrance still touches the workspace before refusing. Round nine also introduced a schema-valid continuation that silently retargets the census to the server’s default workspace.

1. **PARTIAL, blocking — pre-routing refusal.** [core.clj:1476](/home/forge/tmp/sol/census10-wt/src/clj_surgeon/core.clj:1476), [mcp_relation_census_test.clj:853](/home/forge/tmp/sol/census10-wt/test/clj_surgeon/mcp_relation_census_test.clj:853) — MCP `doors=[1]` returned `doors-not-strings`; its cold trace never touched the requested workspace and its warmed trace made zero filesystem syscalls. But `bb … :threads not-a-number` returned `invalid-pool-size` only after `stat`ing the workspace, its `.clj-surgeon.edn`, and ancestor configs. The counter witness at line 877 wraps later functions and misses `forms/init-from-file!`.

2. **CLOSED — ordering doors → files/root.** [mcp_relation_census.clj:208](/home/forge/tmp/sol/census10-wt/src/clj_surgeon/mcp_relation_census.clj:208), [mcp_relation_census_test.clj:894](/home/forge/tmp/sol/census10-wt/test/clj_surgeon/mcp_relation_census_test.clj:894) — wire re-runs of `doors=[1]` with 513 files and with `workspace_root="relative/nope"` both returned `doors-not-strings`, never `too-many-files` or `invalid-workspace-root`.

3. **CLOSED — schema rejection concern.** [mcp_relation_census.clj:51](/home/forge/tmp/sol/census10-wt/src/clj_surgeon/mcp_relation_census.clj:51), [mcp_relation_census_test.clj:1670](/home/forge/tmp/sol/census10-wt/test/clj_surgeon/mcp_relation_census_test.clj:1670) — `workspace_root` is optional in the published schema, so `{tool, pool_size:8}` is schema-valid. The conformance battery passed `1 test / 89 assertions / 0 failures / 0 errors`.

4. **OPEN, blocking — continuation retargets the census.** [mcp_relation_census.clj:178](/home/forge/tmp/sol/census10-wt/src/clj_surgeon/mcp_relation_census.clj:178) — a refusal targeting `/tmp/census10-fx/workspace` returned `next_call={tool:"relation_census",pool_size:8}`. Replaying it produced no refusal: after routing, it censused `/home/forge/tmp/sol/census10-wt` and scanned 370 files. The continuation is schema-valid but is not a faithful narrowing of the caller’s request.

5. **CLOSED — `max_files` and `format` remain pure-validation refusals.** [mcp_relation_census.clj:188](/home/forge/tmp/sol/census10-wt/src/clj_surgeon/mcp_relation_census.clj:188), [mcp_relation_census.clj:837](/home/forge/tmp/sol/census10-wt/src/clj_surgeon/mcp_relation_census.clj:837) — each combined with an invalid root returned `unknown-fields`, proving refusal before workspace routing.

6. **PARTIAL, nonblocking diagnostic defect.** [mcp_relation_census.clj:192](/home/forge/tmp/sol/census10-wt/src/clj_surgeon/mcp_relation_census.clj:192) — those unknown-field refusals advertise accepted fields as only `doors/files/pool_size`, omitting the valid `workspace_root`.

7. **GREEN — gates.** [Makefile:175](/home/forge/tmp/sol/census10-wt/Makefile:175), [Makefile:817](/home/forge/tmp/sol/census10-wt/Makefile:817) — direct MCP runner `418/4575/0`, `test-fast` `716/6057/0`, and operation oracle passed under `suite-run`; the builder’s second MCP result was also reproduced by the same runner.

Port 7908 is stopped, the worktree is clean at `459f46e`, and trace artifacts remain under `/tmp/census10-fx`.