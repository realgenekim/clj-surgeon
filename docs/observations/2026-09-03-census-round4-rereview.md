# census-verb 1e5eec7 — Sol executed round-4 re-check: NO-GO (success paths byte-identical on three entrances; refusal receipts drop evidence; no entry bound; exhaustion placeholder) — round 5 launched

NO-GO for the mayor’s merge queue. The main success paths are repaired, but refusal paths still violate CENSUS-028/030/032, and the resource-exhaustion continuation violates CENSUS-017.

### Six-item re-check

1. **PARTIAL — escaping symlink.** [census_discovery.clj:127](src/clj_surgeon/census_discovery.clj:127) — MCP/JVM/bb each returned `files 1`, `skipped_outside_root 1`; however, with no fold arms, both CLIs omit the skip figure entirely at [core.clj:634](src/clj_surgeon/core.clj:634).

2. **PARTIAL — link chain.** [census_discovery.clj:139](src/clj_surgeon/census_discovery.clj:139) — all three returned `files 1`, `duplicates_collapsed 2`, and `by_file` keyed by `real/folds.clj`; when the real file has no arms, the MCP and both CLIs drop `duplicates_collapsed`.

3. **CLOSED — symlinked root.** [census_discovery.clj:49](src/clj_surgeon/census_discovery.clj:49), [core.clj:466](src/clj_surgeon/core.clj:466) — all three canonicalized the root and returned `ok/files 1`.

4. **CLOSED — executable narrowing.** [census_discovery.clj:203](src/clj_surgeon/census_discovery.clj:203) — the 2,500-file `src/a` plus 1,501-file `src/b` tree narrowed to `src/a` on all three; replaying the tool’s returned call verbatim produced `ok`, `files_scanned 2500`.

5. **PARTIAL — `files_scanned`.** [core.clj:676](src/clj_surgeon/core.clj:676) — JVM and bb publish `files-scanned 4000` at the exact ceiling, as does MCP; but CLI `no-fold-arms-found` receipts still omit it while MCP publishes it at [mcp_relation_census.clj:544](src/clj_surgeon/mcp_relation_census.clj:544).

6. **PARTIAL — oversized omission evidence.** [mcp_relation_census.clj:391](src/clj_surgeon/mcp_relation_census.clj:391), [core.clj:677](src/clj_surgeon/core.clj:677) — with one valid arm source, all three listed 12 of 13 oversized files and reported omitted `1`; with only the 13 oversized files, all three returned `no-fold-arms-found` and published none of the oversized evidence.

### Round-four hunt

7. **OPEN — entry-count bound.** [census_discovery.clj:157](src/clj_surgeon/census_discovery.clj:157) — `.listFiles` materializes and sorts every entry; 60,000 non-sources were all walked because only candidate sources count toward the 4,000 ceiling.

8. **Depth/loop acceptable in the executed range.** [census_discovery.clj:62](src/clj_surgeon/census_discovery.clj:62) — there is no explicit depth bound, but a 1,700-level real tree completed on all entrances; `dir → ancestor` terminated because directory symlinks are never descended.

9. **Name and root edges pass.** [relation_census.clj:64](src/clj_surgeon/relation_census.clj:64) — extensions are deliberately case-sensitive (`.CLJ`/`.Clj` ignored); `x.clj/inside.clj` was correctly walked; symlink-into-`target`, trailing slash, and relative roots worked on both CLI runtimes.

10. **Narrowing edge cases pass.** [mcp_relation_census.clj:432](src/clj_surgeon/mcp_relation_census.clj:432) — flat 4,001 returned a remedy and no executable continuation (`null`/`nil`); a selected zero-arm subtree replayed to `no-fold-arms-found`, which is acceptable.

11. **CLOSED — JVM/bb parity.** [core.clj:557](src/clj_surgeon/core.clj:557) — after normalizing only `classify`/`merge` timing, both 841-byte receipts were byte-identical: SHA-256 `8f855b3302c17f925c863a683c7575d092c36987132f349f68021dffaa44f919`.

12. **OPEN, blocking — exhaustion placeholder.** [mcp_relation_census.clj:596](src/clj_surgeon/mcp_relation_census.clj:596) — an executed `OutOfMemoryError` returned `:files ["<a narrower file list>"]`; that is not executable and violates CENSUS-017 plus the argument-placeholder prohibition in [relation-census-specs.md:60](docs/intent/relation-census/relation-census-specs.md:60).

Gates under `/home/forge/bin/suite-run`:

- MCP: 407 tests, 4,320 assertions, 0 failures/errors; oracle passed.
- Fast: 716 tests, 6,057 assertions, 0 failures/errors.

Port 7908 is stopped, `/tmp/census5-fx` was removed, and the checkout remains clean at `1e5eec7`.