# Census round 14 — Sol executed re-check of bridge/census-verb at d338554 (2026-09-03T21:46Z)

Verdict: **NO-GO**. Round-13 items 2/3/7/8 CLOSED (not-decodable first even under triple violation; 891/887-byte remedies name `a-fit` and the ceiling; `file-not-found` typed + anchored; missing entry removed, options kept); 512/513 boundary exact with multibyte subtree names; enumeration ratchet live (`1 test, 104 pass, 1 fail` on a fake type). BLOCKING: chmod-000 `[good denied]` → `census-adapter-failure`, `exhausted=false`, no `[good]` continuation, false exhaustion remedy (mcp_paths.clj:67 checks regularity not readability; the failure escapes from `slurp` at :370); MCP candidates still constructed outside `narrowing-continuation` at 7 sites — a 600-char root on an unknown-field refusal emitted a 661-byte `next_call`. Round 15 launched.

## Sol verdict, verbatim

# NO-GO

Two blocking defects remain: permission-denied files escape the unreadable-path contract, and several MCP refusal paths bypass the 512-byte continuation ceiling.

1. **Round-13 item 2 — CLOSED.** [relation_census.clj:448](/home/forge/tmp/sol/census15-wt/src/clj_surgeon/relation_census.clj:448) — live CLI and MCP requests combining U+FFFD with `bogus` returned `dir-not-decodable` / `workspace-root-not-decodable`, remedy-only.

2. **Round-13 item 3 — CLOSED.** [mcp_relation_census.clj:536](/home/forge/tmp/sol/census15-wt/src/clj_surgeon/mcp_relation_census.clj:536) and [mcp_relation_census.clj:560](/home/forge/tmp/sol/census15-wt/src/clj_surgeon/mcp_relation_census.clj:560) — live candidate/entry refusals measured exactly 891/887 UTF-8 bytes, named `a-fit` and the 512-byte ceiling, and did not claim no subtree was known.

3. **Round-13 item 7 — CLOSED.** [core.clj:671](/home/forge/tmp/sol/census15-wt/src/clj_surgeon/core.clj:671) — `:file /tmp/does-not-exist.clj` returned `file-not-found`, the exact file anchor, remedy, no continuation, and exit 1.

4. **Round-13 item 8 — CLOSED for the reported missing-file case.** [mcp_relation_census.clj:752](/home/forge/tmp/sol/census15-wt/src/clj_surgeon/mcp_relation_census.clj:752) — live `[good missing]` returned `unreadable-source-path`, removed only `missing`, retained `[good]`, and preserved `doors` and `pool_size`.

5. **Table-head attack — CLOSED.** [relation_census.clj:404](/home/forge/tmp/sol/census15-wt/src/clj_surgeon/relation_census.clj:404) — U+FFFD plus non-integer pool size plus unknown field refused first as not-decodable on both entrances, with no continuation.

6. **512-byte boundary attack — CLOSED.** [mcp_relation_census.clj:556](/home/forge/tmp/sol/census15-wt/src/clj_surgeon/mcp_relation_census.clj:556) — exactly 512 bytes produced a `next-call`; 513 produced nil; multibyte subtree `子树-é-枝` was byte-counted and preserved in both candidate and overflow remedy.

7. **All-unreadable behavior — PARTIAL / blocking.** [mcp_relation_census.clj:759](/home/forge/tmp/sol/census15-wt/src/clj_surgeon/mcp_relation_census.clj:759) — all-missing and duplicate-missing lists correctly returned remedy-only, but chmod-000 `[good denied]` returned `census-adapter-failure`, `exhausted=false`, no viable `[good]` continuation, and a false resource-exhaustion remedy. The path resolver checks regularity but not readability at [mcp_paths.clj:67](/home/forge/tmp/sol/census15-wt/src/clj_surgeon/mcp_paths.clj:67), so the failure escapes from `slurp` at [mcp_relation_census.clj:370](/home/forge/tmp/sol/census15-wt/src/clj_surgeon/mcp_relation_census.clj:370).

8. **Enumeration ratchet — CLOSED.** [mcp_relation_census_test.clj:3213](/home/forge/tmp/sol/census15-wt/test/clj_surgeon/mcp_relation_census_test.clj:3213) — adding `:sol-fake-refusal` only to the scratch declaration failed exactly one assertion: `1 test, 104 pass, 1 fail`.

9. **Continuation construction audit — OPEN / blocking.** CLI `:next-command` is centralized at [relation_census.clj:1022](/home/forge/tmp/sol/census15-wt/src/clj_surgeon/relation_census.clj:1022), but MCP candidates are still constructed outside `narrowing-continuation` at lines 207, 689, 760, 805, 852, 898, and 1009. A live unknown-field refusal with a 600-character root emitted a 661-byte `next_call`, violating the global 512-byte rule at [mcp_relation_census.clj:207](/home/forge/tmp/sol/census15-wt/src/clj_surgeon/mcp_relation_census.clj:207).

10. **Gates and hygiene — GREEN.** MCP `440/5272/0` twice, test-fast `716/6057/0`, oracle pass, battery `1/128/0`; all serial under `suite-run`. HEAD remains `d338554`, worktree clean, port 7908 stopped, and no commit/stash/push or `make mcp-test` was performed.

Evidence: [live MCP probes](/tmp/census15-sol-fx/mcp-probe.log), [CLI probes](/tmp/census15-sol-fx/cli-probe.log), [boundary probe](/tmp/census15-sol-fx/narrowing-boundary.log), [mutation witness](/tmp/census15-sol-fx/enumeration-mutation.log), and [construction grep](/tmp/census15-sol-fx/continuation-grep.log).