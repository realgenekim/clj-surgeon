# census-verb ef545c5 — Sol executed re-review: NO-GO (6 items; round 3 launched)

NO-GO for the mayor’s merge queue. The original classifier is unchanged, but the fix round still has two incomplete-boundary bugs plus new schema and receipt inconsistencies.

### Prior verdict re-review

| # | Status | File:line and executed witness |
|---|---|---|
| 1 | PARTIAL | [mcp_relation_census.clj:130](src/clj_surgeon/mcp_relation_census.clj:130) — `pool_size 0` refused server-side in 56 ms wire / 1.99 ms handler with `invalid-mcp-request` and `next_call`; 65, 513 files, and 33 doors also refused typed. However `"8"` is accepted despite the schema requiring an integer. |
| 2 | CLOSED | [mcp_relation_census.clj:260](src/clj_surgeon/mcp_relation_census.clj:260), [mcp_relation_census.clj:578](src/clj_surgeon/mcp_relation_census.clj:578) — oversized named input refused before read; 240 maximum-sized arm files caused a typed `census-resource-exhausted` response in 0.907 s, with `next_call`; server remained healthy. |
| 3 | PARTIAL | [mcp_relation_census.clj:215](src/clj_surgeon/mcp_relation_census.clj:215) — 20,000 escaping symlinks completed linearly in 230 ms, with all 20,000 counted. But a tree with 4,002 Clojure files silently stopped at 4,000 and returned `ok:true`, `read_complete:true`. |
| 4 | PARTIAL | [core.clj:466](src/clj_surgeon/core.clj:466) — `:threads 8` succeeds, invalid threads and `:doors conj` refuse typed. The CLI still performs `fs/glob` before `take 4000`, silently truncates, and publishes no truncation/refusal. |
| 5 | CLOSED | [mcp_relation_census_test.clj:102](test/clj_surgeon/mcp_relation_census_test.clj:102), [census_pool_test.clj:22](test/clj_surgeon/census_pool_test.clj:22) — two arm fixtures now exercise pool invariance and exact-once mapping. |
| 6 | CLOSED | [census_pool.clj:18](src/clj_surgeon/census_pool.clj:18), [core.clj:503](src/clj_surgeon/core.clj:503) — JVM CLI uses the shutdown-bound pool; Babashka honestly reports pool 1 and `pool-size-requested 8`. |
| 7 | CLOSED | [mcp_relation_census.clj:328](src/clj_surgeon/mcp_relation_census.clj:328) — long-path receipt test passed; even the 512-entry duplicate stress receipt was 3,968 bytes. |
| 8 | CLOSED | [mcp_relation_census.clj:450](src/clj_surgeon/mcp_relation_census.clj:450), [relation_census.clj:822](src/clj_surgeon/relation_census.clj:822) — explicit-file receipt reported only `read/classify/merge`; tree scan additionally reported `discover`; no synthetic `parse` phase. |
| 9 | CLOSED | [relation_census.clj:636](src/clj_surgeon/relation_census.clj:636) — hidden helper witness returned `raw 0`, `unrecognised_calls 1`, and a non-`none` action naming `record-event`. |
| 10 | CLOSED | [mcp_relation_census.clj:286](src/clj_surgeon/mcp_relation_census.clj:286) — `record-window` defined only in `helpers_only.clj` was accepted on both entrances; undefined doors refused. |
| 11 | CLOSED | [mcp_relation_census_test.clj:490](test/clj_surgeon/mcp_relation_census_test.clj:490) — README, CLAUDE, both skill copies, contract count, enumeration warning, and test labels passed their witnesses. |

The schema maxima are inclusive: 64 pool threads, 512 files, 32 doors, and 2 MiB source all succeeded; the first values above them refused typed with `next_call`. The problematic operational ceiling is discovery at 4,000, which succeeds falsely rather than refusing.

Classifier parity against the unbounded 7244141 implementation was byte-for-byte:

- `folds.clj`: 2,240 bytes versus 2,240, equal.
- `inventory_folds.clj`: 1,527 bytes versus 1,527, equal.

Contract evidence:

- `make mcp-operation-oracle`: passed.
- Census/intent/registry focus: 57 tests, 717 assertions, green.
- CLI dispatch: 20 tests, 207 assertions, green.
- Full MCP alias: 395 tests, 4,146 assertions, one unrelated machine-path failure at [mcp_change_buffer_test.clj:686](test/clj_surgeon/mcp_change_buffer_test.clj:686).
- `test-fast` is also red on existing agent-routing text expectations at [agent_routing_test.clj:106](test/clj_surgeon/agent_routing_test.clj:106).

### Merge-queue findings — NO-GO

1. [mcp_relation_census.clj:238](src/clj_surgeon/mcp_relation_census.clj:238) — 4,002 candidate sources produced a successful `read_complete:true` census claiming only 4,000 scanned.

2. [mcp_relation_census.clj:241](src/clj_surgeon/mcp_relation_census.clj:241) — discovery silently omitted a 2,097,153-byte arm source while publishing completion from the 2,097,152-byte sibling.

3. [core.clj:481](src/clj_surgeon/core.clj:481) — the CLI still enumerates through `fs/glob`, applies `take 4000`, and returns success without a typed ceiling receipt.

4. [mcp_relation_census.clj:177](src/clj_surgeon/mcp_relation_census.clj:177) — wire request `pool_size:"8"` succeeded although [the advertised schema](src/clj_surgeon/mcp_relation_census.clj:63) requires an integer.

5. [relation_census.clj:795](src/clj_surgeon/relation_census.clj:795) — 512 duplicate paths yielded `files:512`, `arms:4608`, and counts 512× larger while `by_file` contained one file with 9 arms and 7 sites.

6. [relation-census-design.md:112](docs/intent/relation-census/relation-census-design.md:112) — the LLD still promises a `parse` phase and says only MCP carries claypoole, contradicting the fixed implementation.

Port 7908 was stopped, synthetic `/tmp` data was removed, and the checkout is clean.