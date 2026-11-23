# Census round 10 — Sol executed re-check of bridge/census-verb at 48c64ac (2026-09-03T11:57Z)

Verdict: **NO-GO**. Round-nine items 1/4/6 CLOSED (MCP path). Blocking on the CLI entrance: the shape validator destructures only `threads` (`:doors conj` read the poisoned config); CLI continuations return the fixed `:dir .` so a replay from another cwd censuses another tree. PARTIAL: CLI/MCP validator parity (`:format edn`, `:max-files 1` accepted silently on the CLI); schema battery omits the non-string-root remedy branch. Round 11 launched.

## Sol verdict, verbatim

NO-GO for the mayor’s merge queue. The three round-nine findings are closed, but round ten leaves two blocking CLI defects.

Gates were green: direct MCP `422/4604/0`, `test-fast` `716/6057/0`, oracle pass, schema battery `1/97/0`. Port 7908 is stopped, HEAD remains `48c64ac`, and the worktree is clean. Evidence is under `/tmp/census11-sol-fx`.

1. **CLOSED — round-nine item 1.** [core.clj:1544](/home/forge/tmp/sol/census11-wt/src/clj_surgeon/core.clj:1544), [mcp_relation_census_test.clj:1007](/home/forge/tmp/sol/census11-wt/test/clj_surgeon/mcp_relation_census_test.clj:1007) — fresh `strace bb … :threads not-a-number` returned `invalid-pool-size`; excluding `execve` argv, zero filesystem syscalls named the unparseable-config workspace.

2. **CLOSED — round-nine item 4, MCP path.** [mcp_relation_census.clj:203](/home/forge/tmp/sol/census11-wt/src/clj_surgeon/mcp_relation_census.clj:203), [mcp_relation_census_test.clj:1909](/home/forge/tmp/sol/census11-wt/test/clj_surgeon/mcp_relation_census_test.clj:1909) — every battery continuation compares equal by real path; fresh named-root and symlink replays censused the requested fixture, not the server checkout.

3. **CLOSED — round-nine item 6.** [mcp_relation_census.clj:236](/home/forge/tmp/sol/census11-wt/src/clj_surgeon/mcp_relation_census.clj:236), [mcp_relation_census_test.clj:254](/home/forge/tmp/sol/census11-wt/test/clj_surgeon/mcp_relation_census_test.clj:254) — fresh unknown-field refusal published exactly `["doors","files","pool_size","workspace_root"]`, matching schema properties.

4. **OPEN, blocking — the CLI shape validator is incomplete.** [relation_census.clj:191](/home/forge/tmp/sol/census11-wt/src/clj_surgeon/relation_census.clj:191), [core.clj:1486](/home/forge/tmp/sol/census11-wt/src/clj_surgeon/core.clj:1486), [relation-census-specs.md:46](/home/forge/tmp/sol/census11-wt/docs/intent/relation-census/relation-census-specs.md:46) — it destructures only `threads`; `bb … :doors conj` against the poisoned workspace read `.clj-surgeon.edn` and returned its EDN error instead of the required pre-filesystem door refusal.

5. **OPEN, blocking — CLI continuations retarget across cwd.** [relation_census.clj:199](/home/forge/tmp/sol/census11-wt/src/clj_surgeon/relation_census.clj:199), [relation-census-specs.md:35](/home/forge/tmp/sol/census11-wt/docs/intent/relation-census/relation-census-specs.md:35) — a refusal for an absolute `:dir` returned the fixed `:dir .`; replay from `/tmp/census11-sol-fx/client-cwd` censused the `:client` fixture rather than the original `:one` workspace.

6. **PARTIAL — validator parity.** [mcp_relation_census.clj:143](/home/forge/tmp/sol/census11-wt/src/clj_surgeon/mcp_relation_census.clj:143), [relation_census.clj:161](/home/forge/tmp/sol/census11-wt/src/clj_surgeon/relation_census.clj:161) — MCP refuses string `doors`/`files`, string `pool_size`, `max_files`, and `format`; the CLI shape pass accepts or ignores all except invalid `threads`. Actual `:format edn` and `:max-files 1` invocations succeeded silently.

7. **CLOSED — relative, `..`, and symlink MCP routing.** [mcp_relation_census.clj:203](/home/forge/tmp/sol/census11-wt/src/clj_surgeon/mcp_relation_census.clj:203), [mcp_relation_census.clj:901](/home/forge/tmp/sol/census11-wt/src/clj_surgeon/mcp_relation_census.clj:901) — relative roots are carried verbatim then refused as non-absolute; absolute `..` paths canonicalize to the same target; symlink-root continuations become canonical paths that callers can replay successfully.

8. **PARTIAL, test gap — non-string-root remedy.** [mcp_relation_census.clj:207](/home/forge/tmp/sol/census11-wt/src/clj_surgeon/mcp_relation_census.clj:207), [mcp_relation_census_test.clj:1093](/home/forge/tmp/sol/census11-wt/test/clj_surgeon/mcp_relation_census_test.clj:1093), [mcp_relation_census_test.clj:1741](/home/forge/tmp/sol/census11-wt/test/clj_surgeon/mcp_relation_census_test.clj:1741) — the remedy is actionable: replacing `42` with an absolute string produced a same-root continuation that replayed successfully; however, the 97-assertion schema battery omits this branch and the standalone witness checks only that the remedy is a string.

9. **GREEN — executed gates.** [Makefile:175](/home/forge/tmp/sol/census11-wt/Makefile:175), [Makefile:817](/home/forge/tmp/sol/census11-wt/Makefile:817) — suite-run produced MCP `422/4604/0`, fast `716/6057/0`, oracle pass, and schema battery `1/97/0`, matching the builder’s reported counts.

**NO-GO**