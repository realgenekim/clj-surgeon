# Census round 12 — Sol executed re-check of bridge/census-verb at 11e5e84 (2026-09-03T20:09Z)

Verdict: **NO-GO**. Round-11 items 2/3/5/6/7/8 CLOSED (post-scan anchor; table order moves both entrances; shell matrix incl. `;printf INJECTED` exact argv, no canary; 2^63 typed; duplicate `:file`; `:dir ""` zero syscalls). NEW blocking: trailing whitespace in `:dir` is `str/trim`-med → replay censuses the trimmed SIBLING; a non-UTF-8 argv byte (0xff) becomes U+FFFD → continuation names a nonexistent root. OPEN: the 512-byte continuation ceiling counts Java characters (890 UTF-8 bytes emitted at 490 chars); door-vocabulary ordering diverges (CLI `unknown-door-symbol` vs MCP `empty-file-list` for equivalent input, `:mcp-phase :post-discovery`). Round 13 launched.

## Sol verdict, verbatim

NO-GO for the mayor’s merge queue. Round-eleven defects are closed, and all prescribed gates pass, but round twelve found two silent-retarget defects plus a continuation-size contract violation.

1. **OPEN, blocking — trailing whitespace silently retargets.** [relation_census.clj:676](/home/forge/tmp/sol/census13-wt/src/clj_surgeon/relation_census.clj:676) — `str/trim` changed `/root ` to `/root`; replay succeeded against the trimmed sibling’s two files instead of the named root’s one.

2. **OPEN, blocking — non-UTF-8 argv silently retargets.** [relation_census.clj:695](/home/forge/tmp/sol/census13-wt/src/clj_surgeon/relation_census.clj:695) — raw filename byte `ff` became UTF-8 `efbfbd`; rendered/vector argv agreed only on the corrupted path, and replay returned `no-fold-arms-found` against a nonexistent U+FFFD root.

3. **OPEN — the byte ceiling counts Java characters.** [relation_census.clj:72](/home/forge/tmp/sol/census13-wt/src/clj_surgeon/relation_census.clj:72), [relation_census.clj:773](/home/forge/tmp/sol/census13-wt/src/clj_surgeon/relation_census.clj:773) — a valid Unicode continuation measuring 490 Java characters but 890 UTF-8 bytes was emitted despite the 512-byte limit.

4. **CLOSED — round-eleven item 2.** [core.clj:785](/home/forge/tmp/sol/census13-wt/src/clj_surgeon/core.clj:785) — post-scan undefined-door refusal named the absolute workspace and replayed from another cwd with exactly one workspace file, not the client’s two.

5. **CLOSED — round-eleven item 3.** [mcp_relation_census.clj:245](/home/forge/tmp/sol/census13-wt/src/clj_surgeon/mcp_relation_census.clj:245) — moving `files` before `doors` changed MCP from `doors-not-an-array` to `empty-file-list` and CLI from `doors-not-a-string` to `file-not-a-string`.

6. **CLOSED — round-eleven item 5.** [relation_census.clj:717](/home/forge/tmp/sol/census13-wt/src/clj_surgeon/relation_census.clj:717) — fresh `space root` and `;printf INJECTED` bash replays produced exact argv, censused the intended roots, emitted no injected output, and created no canary.

7. **CLOSED — round-eleven item 6.** [relation_census.clj:141](/home/forge/tmp/sol/census13-wt/src/clj_surgeon/relation_census.clj:141) — actual MCP `pool_size=9223372036854775808` returned typed `pool-size-out-of-range` with the magnitude serialized as text; no exception escaped.

8. **CLOSED — round-eleven item 7.** [core.clj:1606](/home/forge/tmp/sol/census13-wt/src/clj_surgeon/core.clj:1606) — repeated `:file` refused as `:duplicate-argument`, naming both values and performing no census.

9. **CLOSED — round-eleven item 8.** [relation_census.clj:381](/home/forge/tmp/sol/census13-wt/src/clj_surgeon/relation_census.clj:381) — `:dir ""` under `strace` returned `:dir-not-a-string`, no continuation, an uncomputable remedy, and zero `.clj-surgeon.edn` syscalls.

10. **OPEN — entrance ordering still diverges for door vocabulary.** [relation_census.clj:460](/home/forge/tmp/sol/census13-wt/src/clj_surgeon/relation_census.clj:460), [mcp_relation_census.clj:247](/home/forge/tmp/sol/census13-wt/src/clj_surgeon/mcp_relation_census.clj:247) — CLI `doors=conj,file="",threads=bad` chose `unknown-door-symbol`; equivalent MCP input chose `empty-file-list` because `:mcp-phase :post-discovery` skips the earlier row.

11. **ROUND-12 shell matrix.** [relation_census.clj:738](/home/forge/tmp/sol/census13-wt/src/clj_surgeon/relation_census.clj:738) — backslash, `-rf`, `*`, `~`, and control byte `0x01` replayed with exact argv and no canary; a 3,990-character path correctly received only a remedy because it exceeded the configured 512-character check. Literal NUL is not constructible in a POSIX filename or argv.

12. **ROUND-12 threads.** [relation_census.clj:141](/home/forge/tmp/sol/census13-wt/src/clj_surgeon/relation_census.clj:141) — `08` and `+8` were accepted as decimal integers; `8.0` and `0x8` refused typed as `:invalid-pool-size`.

13. **ROUND-12 duplicates.** [core.clj:1646](/home/forge/tmp/sol/census13-wt/src/clj_surgeon/core.clj:1646) — `:doors a :doors b` and identical `:doors a :doors a` refused as duplicates; `:doors a,b` was one valid list argument and reached the expected post-scan unknown-door refusal.

14. **ROUND-12 constructor audit.** [relation_census.clj:796](/home/forge/tmp/sol/census13-wt/src/clj_surgeon/relation_census.clj:796) — this is the only production `:next-command` construction site; no literal-map bypass or consumer that rejoins `:next-command-argv` with spaces exists.

15. **GREEN gates.** [Makefile:175](/home/forge/tmp/sol/census13-wt/Makefile:175), [Makefile:181](/home/forge/tmp/sol/census13-wt/Makefile:181), [Makefile:817](/home/forge/tmp/sol/census13-wt/Makefile:817) — serially under `suite-run`: MCP `433/5091/0` twice, `test-fast 716/6057/0`, oracle pass, focused battery `1/115/0`.

Evidence is under `/tmp/census13-sol-fx`. HEAD remains `11e5e84`, the worktree is clean, port 7908 was never started, and no commit/stash/push occurred.