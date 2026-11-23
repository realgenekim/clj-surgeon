# census-verb 5eee690 — Sol executed round-7 re-check: NO-GO (both gaps CLOSED; one new: a malformed doors item copied into a schema-invalid next_call) — round 8 launched

NO-GO for the mayor’s merge queue. The two round-six gaps are closed, but the hunt found a new executable-continuation violation: malformed `doors` elements can be copied into a `next_call` rejected by the published schema.

1. **CLOSED — explicit arm-less files.** [mcp_relation_census.clj:657](src/clj_surgeon/mcp_relation_census.clj:657) — my `files=["helpers_only.clj"]` run returned `no-fold-arms-found`, no `next_call`, and a remedy naming `helpers_only.clj`.

2. **CLOSED — oversized replay preserves options.** [mcp_relation_census.clj:632](src/clj_surgeon/mcp_relation_census.clj:632) — `["huge.clj","small.clj"]`, `doors=["upsert-by"]`, `pool_size=1` produced a deep-equal continuation with only `huge.clj` removed; replay succeeded with pool size 1.

3. **OPEN, blocking — copied continuation is not schema-validated.** [mcp_relation_census.clj:180](src/clj_surgeon/mcp_relation_census.clj:180), [mcp_relation_census.clj:632](src/clj_surgeon/mcp_relation_census.clj:632) — `doors=[1]` passed server validation and was copied into `next_call`, although [the schema requires string items](src/clj_surgeon/mcp_relation_census.clj:60); that continuation is not executable through the published schema.

4. **CLOSED — no forbidden-key leakage.** [mcp_relation_census.clj:132](src/clj_surgeon/mcp_relation_census.clj:132) — after canonical `workspace_root` routing, `_meta`, cursors, alternate workspace spellings, and other keys are rejected as unknown before copying.

5. **PARTIAL, non-blocking — discovered-list asymmetry.** [mcp_relation_census.clj:657](src/clj_surgeon/mcp_relation_census.clj:657) — 20 arm-less files plus one oversized file yielded exactly 12 pinned non-oversized paths, never 4,000; however, replay deterministically reaches the explicit no-arms refusal, so the asymmetry is spec-consistent but operationally weak.

6. **CLOSED — amended tests are genuinely red under old source.** [mcp_relation_census_test.clj:140](test/clj_surgeon/mcp_relation_census_test.clj:140), [mcp_relation_census_test.clj:1409](test/clj_surgeon/mcp_relation_census_test.clj:1409) — restoring only the source file to `aac5db3` made both amended test vars red: 89 pass, 7 fail, 2 errors; the errors were follow-on nil-remedy assertions. This does not reproduce the commit message’s narrower `3 failures / 0 errors` count.

7. **GREEN — requested gates.** [mcp_process_test.clj:224](test/clj_surgeon/mcp_process_test.clj:224) — oracle passed; `test-fast` 716/6057/0; direct `clojure -M:clj-surgeon/mcp-test` passed 415/4490/0 twice, with no timeout.

Recommended fix: validate every `doors` item server-side against the advertised string contract before filesystem work, and add a witness proving every emitted continuation passes the input schema. The checkout is clean; port 7908 was never started.