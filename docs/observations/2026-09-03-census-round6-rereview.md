# census-verb aac5db3 — Sol executed round-6 re-check: NO-GO (walk + narrowing CLOSED; two continuation-contract gaps; one load-dependent red gate elsewhere) — round 7 launched

Round-six verdict: **NO-GO**. The walk and narrowing regressions are closed, but the continuation contract remains incomplete in two executable cases, and the one-shot MCP gate was red.

Builder’s judgment call is acceptable: discovered oversized files are already skipped and reported, so `source-too-large` should offer a continuation only for caller-supplied `files`. Synthesizing thousands of discovered paths would be harmful.

Additional findings:

- Stream closure is sound: bb’s 1,000-refusal run held FDs at 5→5; renaming a directory while its stream was open also produced 0 delta. The `finally` covers iteration exceptions, although I could not force a real `DirectoryIteratorException` on ext4.
- Refusal evidence is deterministic absent concurrent filesystem mutation. Twenty MCP and bb runs agreed, and all 1,000 stress runs reported `observed=50001`, `entries_yielded=50000`, `files_scanned=0`. Unordered names from the terminating directory never enter ranking because that directory and its ancestors are partial.
- The refusal-time restat handles growth: an injected second file grew beyond the cap after collection and was included in `files_removed`. A later post-construction mutation remains an unavoidable replay race.
- Fit is checked before ranking: in an equal-entry tie, the 4,001-candidate subtree was rejected and `src/a` won on JVM and bb.
- The 8 MCP + 8 CLI serialized-refusal witness passed 63 assertions with no `<`.

NO-GO

1. **CLOSED — streamed entry bound.** [census_discovery.clj:176](src/clj_surgeon/census_discovery.clj:176) — exactly 50,000 entries with the only source sorting last succeeded with nine arms on MCP/JVM/bb; 51,000 refused at 50,001 with only 50,000 names yielded. The parent implementation reproduced `observed=50001` after `.list` had obtained all 51,000.

2. **CLOSED — useful narrowing.** [census_discovery.clj:281](src/clj_surgeon/census_discovery.clj:281) — the 49,997-entry junk control offered `/src/a`, never junk, and replay found nine arms on all three entrances.

3. **PARTIAL, blocking — continuations.** [mcp_relation_census.clj:650](src/clj_surgeon/mcp_relation_census.clj:650) — placeholders are gone, but explicit `files=["helpers_only.clj"]` returned `no-fold-arms-found` with the identical request as `next_call` and no remedy, contrary to CENSUS-014; the test explicitly blesses this at [mcp_relation_census_test.clj:1414](test/clj_surgeon/mcp_relation_census_test.clj:1414).

4. **OPEN, blocking — oversized replay loses caller semantics.** [mcp_relation_census.clj:628](src/clj_surgeon/mcp_relation_census.clj:628) — a request with mixed files, `doors=["upsert-by"]`, and `pool_size=1` returned only workspace/files; both options were dropped, so it was not the caller’s request minus only oversized sources.

5. **CLOSED — stream lifetime.** [census_discovery.clj:197](src/clj_surgeon/census_discovery.clj:197) — bound refusal, vanished-directory race, and 1,000 bb refusals leaked zero descriptors; iteration exceptions execute the close at line 216.

6. **CLOSED — fit before rank.** [census_discovery.clj:305](src/clj_surgeon/census_discovery.clj:305) — `fits?` filters candidates before sorting; an equal-entry 4,001-candidate subtree lost to the fitting subtree on JVM and bb.

7. **CLOSED — refusal-time growth check.** [mcp_relation_census.clj:235](src/clj_surgeon/mcp_relation_census.clj:235) — injected growth between collection and refusal produced both oversized paths in `files_removed` and only the small source in `next_call`.

8. **CLOSED — placeholder serialization.** [mcp_relation_census_test.clj:1328](test/clj_surgeon/mcp_relation_census_test.clj:1328) — eight MCP plus eight JVM/bb CLI receipts serialized without `<`; empty no-arms and invalid workspace correctly supplied remedies without continuations.

9. **RED GATE.** [mcp_process_test.clj:224](test/clj_surgeon/mcp_process_test.clj:224) — MCP: 415 tests / 4,479 assertions / **1 failure**, an unchanged 1-second host-admission timeout; test-fast: 716 / 6,057 / 0; oracle passed. No rerun was made. Fixtures were removed, port 7908 was never started, and the checkout remains clean at `aac5db3`.