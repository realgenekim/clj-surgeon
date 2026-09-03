# census-verb f43ac03 — Sol executed round-3 re-check: NO-GO (CLI walk escapes root, duplicate reads, symlinked-root parity, false continuation) — round 4 launched

NO-GO for the mayor’s merge queue. Four original defects are closed, two are partial, and the new CLI walk introduces a root-confinement vulnerability plus cross-entrance inconsistency.

### Six-item re-check

| # | Status | Executed witness |
|---|---|---|
| 1 | PARTIAL | [mcp_relation_census.clj:494](src/clj_surgeon/mcp_relation_census.clj:494), [core.clj:638](src/clj_surgeon/core.clj:638) — MCP: 4,000 → `ok:true`, `read_complete:true`, `files_scanned:4000`; 4,001 → `too-many-candidate-files`, `files_read:0`, maximum/fits 4000, observed 4001 lower-bound. Babashka and JVM CLI refuse correctly at 4,001, but successful 4,000 receipts omit `files-scanned`, and neither continuation is executable. |
| 2 | CLOSED | [mcp_relation_census.clj:259](src/clj_surgeon/mcp_relation_census.clj:259), [core.clj:495](src/clj_surgeon/core.clj:495) — all three executions read a valid 2,097,152-byte arm source; 2,097,153 bytes appeared in `oversized_skipped` and forced `read_complete:false`. |
| 3 | PARTIAL | [core.clj:466](src/clj_surgeon/core.clj:466) — both Babashka and JVM now stop at ceiling+1 and refuse before reads, but the replacement walk follows file symlinks, can escape the root, and does not canonicalize duplicates. |
| 4 | CLOSED | [mcp_relation_census.clj:180](src/clj_surgeon/mcp_relation_census.clj:180) — `"8"`, `null`, `8.0`, `true`, and `[]` each returned `pool-size-not-an-integer` in 0.25–0.36 ms against the 4,001-file tree, proving rejection before discovery. |
| 5 | CLOSED for named MCP inputs; PARTIAL globally | [mcp_relation_census.clj:296](src/clj_surgeon/mcp_relation_census.clj:296) — 512 identical names produced `files:1`, `arms:9`, `duplicates_collapsed:511`; two names through an in-root symlink produced `duplicates_collapsed:1`. CLI discovery bypasses this canonicalization and counted a two-link chain three times. |
| 6 | CLOSED | [relation-census-design.md:93](docs/intent/relation-census/relation-census-design.md:93) — the LLD names only `discover`, `read`, `classify`, and `merge`; it explicitly says parsing occurs inside `classify`. |

### Full gates

```text
Ran 716 tests containing 6056 assertions.
0 failures, 0 errors.

Ran 400 tests containing 4230 assertions.
0 failures, 0 errors.

mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]
```

All ran once through `/home/forge/bin/suite-run`; JVMs were capped at `-Xmx1g`.

### Round-three findings

1. **NO-GO — CLI root escape:** [core.clj:480](src/clj_surgeon/core.clj:480) — an escaping `src/escape.clj` symlink was followed by both Babashka and JVM; CLI reported `files:2`, `arms:15`, while MCP skipped it and reported `files:1`, `arms:6`, `skipped_outside_root:1`.

2. **NO-GO — CLI duplicate reads:** [core.clj:495](src/clj_surgeon/core.clj:495) — `link1.clj → link2.clj → real/folds.clj` produced `files:3`, `arms:27` on both CLIs; MCP read the real file once.

3. **NO-GO — symlinked-root parity:** [core.clj:513](src/clj_surgeon/core.clj:513) — MCP canonicalized a workspace-root symlink and read one file; both CLIs returned `no-fold-arms-found`, reading zero.

4. **NO-GO — false executable continuation:** [mcp_relation_census.clj:511](src/clj_surgeon/mcp_relation_census.clj:511), [relation-census-specs.md:60](docs/intent/relation-census/relation-census-specs.md:60) — the ceiling `next_call` retains the over-limit root and supplies `"<at most 4000 named sources under this root>"`; replay returned `unreadable-source-path` and repeated the same call. It computes no narrower subtree from the lower bound.

5. **FIX — incomplete CLI success receipt:** [core.clj:551](src/clj_surgeon/core.clj:551), [core.clj:680](src/clj_surgeon/core.clj:680) — discovery records `:scanned 4000`, but `run-relation-census` drops it; neither CLI can substantiate the requested successful scan count.

6. **FIX — oversized-list truncation is implicit:** [core.clj:685](src/clj_surgeon/core.clj:685), [mcp_relation_census.clj:27](src/clj_surgeon/mcp_relation_census.clj:27) — 20 oversized files yielded 12 names and total count 20, but no explicit “8 omitted”/truncation field; CLI hard-codes `12` instead of sharing `max-listed-files`.

7. **CLOSED — receipt growth:** [mcp_relation_census.clj:400](src/clj_surgeon/mcp_relation_census.clj:400) — complete MCP wire responses were 2,996 bytes for 512 duplicates and 1,237 bytes for 4,001 candidates; CLI refusal output was 440 bytes under Babashka. Growth remained bounded.

Port 7908 is stopped, synthetic fixtures are deleted, and the checkout remains clean at `f43ac03`.