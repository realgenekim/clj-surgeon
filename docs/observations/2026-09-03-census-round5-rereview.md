# census-verb 869bbce — Sol executed round-5 re-check: NO-GO (listing still enumerates before charging; narrowing offers junk; three placeholders + CENSUS-014 amendment) — round 6 launched

# Mayor’s merge-queue verdict: NO-GO

1. **CLOSED — refusal discovery evidence.** [relation_census.clj:94](src/clj_surgeon/relation_census.clj:94) — MCP/JVM/bb agreed: escape=`files_scanned 1`, `skipped_outside_root 1`; link chain=`files_scanned 1`, `duplicates_collapsed 2`; oversized-only=`files_scanned 0`, 13 skipped, `over00`…`over11` identically ordered, omitted `1`.

2. **PARTIAL, blocking — entry bound.** [census_discovery.clj:176](src/clj_surgeon/census_discovery.clj:176) — the 60,064-entry tree stopped at `observed 50001`, maximum `50000`, lower-bound true, `files_read 0` on all entrances; builder 1.33s, MCP 0.62s, JVM 6.26s, bb 1.06s. The `src/junk/d00` narrowing fit both bounds and replayed, but returned `no-fold-arms-found`.

3. **PARTIAL, blocking — the bound still enumerates before charging.** [census_discovery.clj:176](src/clj_surgeon/census_discovery.clj:176) — `.list` materializes the complete directory and `sort` realizes all names before the counter at line 179; an exact 50,000-entry flat directory with the only source sorted last produced no continuation, while a nested control offered the 49,997-entry junk subtree instead of the arm-bearing subtree.

4. **CLOSED — forced OOM continuation.** [mcp_relation_census.clj:662](src/clj_surgeon/mcp_relation_census.clj:662) — forced `OutOfMemoryError` returned `census-resource-exhausted`, `files_read 0`, a remedy, no `next_call` key, and no `<` anywhere in serialized JSON.

5. **OPEN, blocking — three placeholder continuations remain.** [mcp_relation_census.clj:595](src/clj_surgeon/mcp_relation_census.clj:595), [core.clj:621](src/clj_surgeon/core.clj:621), [core.clj:711](src/clj_surgeon/core.clj:711), [mcp_relation_census.clj:708](src/clj_surgeon/mcp_relation_census.clj:708) — executed `source-too-large`, CLI `no-fold-arms-found`, and invalid workspace receipts put `<…>` in argument positions; oversized-only MCP additionally returned the original workspace call unchanged.

6. **REQUIREMENT FIX REQUIRED.** [relation-census-specs.md:35](docs/intent/relation-census/relation-census-specs.md:35) — CENSUS-027’s placeholder prohibition is grammatically scoped to ceiling discovery, but these calls violate the same invariant and CENSUS-014’s “executable next_call”; amend 014 to require an executable continuation when computable, otherwise no continuation plus a remedy, and state the placeholder prohibition globally.

7. **CLOSED — pre-walk discovery-fact rule.** [relation-census-specs.md:40](docs/intent/relation-census/relation-census-specs.md:40), [relation-census-specs.md:66](docs/intent/relation-census/relation-census-specs.md:66) — invalid pool size omitted every walk fact on MCP/JVM/bb; omission is correct because validation precedes filesystem work, whereas `files_scanned 0` would falsely claim a completed walk.

8. **CLOSED — corrected fail-first assertion is genuinely red.** [mcp_relation_census_test.clj:320](test/clj_surgeon/mcp_relation_census_test.clj:320) — running the corrected witness with the old exhaustion implementation failed both “no `next_call`” and “remedy present” assertions.

9. **NOT A CENSUS DEFECT — install-test race confirmed.** [install_test.clj:50](test/clj_surgeon/install_test.clj:50), [Makefile:16](Makefile:16) — the test snapshots HEAD at namespace load while each nested Make invocation samples HEAD again; a mid-suite commit can therefore stamp a different SHA and fail line 321 independently of census behavior.

10. **GREEN GATES.** [Makefile:181](Makefile:181), [Makefile:817](Makefile:817) — under `suite-run`: oracle passed; MCP 413 tests / 4,388 assertions / 0 failures; fast 716 / 6,057 / 0. Port 7908 is stopped, `/tmp/census6-fx` removed, and the checkout remains clean at `869bbce`.