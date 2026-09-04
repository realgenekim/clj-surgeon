# Census round 13 — Sol executed re-check of bridge/census-verb at 1a6e38c (2026-09-03T21:06Z)

Verdict: **NO-GO**. CLOSED: no-trim anchor (trailing-space root replays its own one file); one-phase door vocabulary on both entrances; whitespace paths (`"   "` carried absolute, `""` refused, `" "` legal); phase parity (restoring `:mcp-phase` fails the witness). BLOCKING: U+FFFD path + an unknown field → `unknown-arguments`/`unknown-fields` wins and the corrupt path is emitted in a continuation (the not-decodable row is not first in that combination); MCP narrowing continuations refuse over the ceiling but the remedy neither states the measured bytes nor is true ("no fitting subtree was known"); missing CLI `:file` → untyped `:invalid-arguments` (NoSuchFileException at core.clj:499); missing MCP `files` → typed `unreadable-source-path` but `next_call` repeats the rejected request verbatim, no remedy. Hygiene note from Sol: it ran `make mcp-test`, whose built-in readiness check touched 7890. Round 14 launched.

## Sol verdict, verbatim

# NO-GO

The round-twelve fixes work in isolation, but round thirteen exposes three blocking continuation defects plus the pre-existing missing-file failure.

1. **Item 1 — CLOSED.** [relation_census.clj:814](/home/forge/tmp/sol/census14-wt/src/clj_surgeon/relation_census.clj:814) — trailing-space root replayed from another cwd against `/root ` and censused exactly its one file, not the trimmed sibling’s two.

2. **Item 2 — PARTIAL / blocking.** [relation_census.clj:419](/home/forge/tmp/sol/census14-wt/src/clj_surgeon/relation_census.clj:419), [relation_census.clj:502](/home/forge/tmp/sol/census14-wt/src/clj_surgeon/relation_census.clj:502) — raw-byte `0xff` and standalone U+FFFD correctly refuse without continuation, but combining U+FFFD with `bogus` returns `unknown-arguments`/`unknown-fields` on CLI and MCP and emits the corrupt path in a continuation.

3. **Item 3 — PARTIAL / blocking.** [relation_census.clj:916](/home/forge/tmp/sol/census14-wt/src/clj_surgeon/relation_census.clj:916), [mcp_relation_census.clj:556](/home/forge/tmp/sol/census14-wt/src/clj_surgeon/mcp_relation_census.clj:556), [mcp_relation_census.clj:613](/home/forge/tmp/sol/census14-wt/src/clj_surgeon/mcp_relation_census.clj:613) — CLI correctly refused the 490-character/890-byte continuation and named 890 bytes; MCP refused 891-byte candidate and 887-byte entry narrowings, but neither remedy states the measured length and both incorrectly say no fitting subtree was known.

4. **Item 10 — CLOSED.** [relation_census.clj:584](/home/forge/tmp/sol/census14-wt/src/clj_surgeon/relation_census.clj:584) — `doors=conj,file="",threads=bad` returned `unknown-door-symbol` first on both actual entrances.

5. **Whitespace-path attack — CLOSED.** [relation_census.clj:814](/home/forge/tmp/sol/census14-wt/src/clj_surgeon/relation_census.clj:814) — `:dir "   "` carried `/tmp/census14-sol-fx/space-cwd/   ` and replayed one file; `"."` carried the absolute cwd, `""` refused without continuation, and `" "` remained a legal relative path.

6. **Phase-parity attack — CLOSED.** [mcp_relation_census_test.clj:3590](/home/forge/tmp/sol/census14-wt/test/clj_surgeon/mcp_relation_census_test.clj:3590) — production has no phase keys and skips only explicitly inexpressible rows; temporarily restoring `:mcp-phase :post-discovery` made the witness fail 2 assertions, after which the mutation was removed.

7. **Missing CLI file — OPEN / blocking.** [core.clj:499](/home/forge/tmp/sol/census14-wt/src/clj_surgeon/core.clj:499) — `:file /tmp/does-not-exist.clj` throws `java.nio.file.NoSuchFileException` and surfaces only untyped `:invalid-arguments`, with no anchor or remedy.

8. **Missing MCP file — OPEN / blocking.** [mcp_relation_census.clj:679](/home/forge/tmp/sol/census14-wt/src/clj_surgeon/mcp_relation_census.clj:679) — both absolute-invalid and relative-missing `files` refuse as typed `unreadable-source-path`, but `next_call` repeats the same rejected file request verbatim and provides no remedy.

9. **Gates — GREEN.** [Makefile:181](/home/forge/tmp/sol/census14-wt/Makefile:181), [Makefile:817](/home/forge/tmp/sol/census14-wt/Makefile:817) — reviewer runs under `suite-run`: MCP `437/5167/0`, test-fast `716/6057/0`, oracle pass, battery `1/115/0`; logs are in [/tmp/census14-sol-fx](/tmp/census14-sol-fx).

10. **Hygiene.** HEAD remains `1a6e38c`, worktree clean, port 7908 stopped, and no commit/stash/push occurred. The mandated `make mcp-test` itself performed its built-in 7890 readiness check despite the port restriction; I made no direct request to that port.

The repository-required `linked-intent-dev` skill was not present; no persistent source change was made.