GO for landing `913020c8` on `MCP/main`. No confirmed product defects were found in the requested probes.

The landing must compose two additive conflicts; do not select either side wholesale.

| Probe | Classification | Executed result |
|---|---|---|
| Double-`/src/` namespace identity | NOT-REPRODUCED | Correctly derived `sample.moved` beneath `/parent/src/work/src/clj/...`. |
| Extraction lib/path mismatch | NOT-REPRODUCED | Typed refusal before write. |
| `versioned` docstring bystander | NOT-REPRODUCED | Zero false caller candidates. |
| Decisions continuation | NOT-REPRODUCED | Refusal listed the real caller; returned `next_call` succeeded and reproduced that candidate. |
| Fresh 141-form split | NOT-REPRODUCED | Committed 141 forms into 20 destinations, rewrote 5 caller files/87 sites, 8 promotions, acyclic graph, 0 unknowns, 30.58 s. |
| Four CurtainCall oracles | NOT-REPRODUCED | Unit suite passed 231 tests/2,258 assertions, source retired, all owners occurred once in the correct destination, and the included architecture guard passed. Evidence came from the receipt and its durable details artifact; no external oracle rerun was used. |
| Induced final-graph cycle | NOT-REPRODUCED | Moving `fmt-date` from foundation `format` into product `event-setup` produced `:cycle`; no writes. |
| Unmapped form | NOT-REPRODUCED | `:unmapped-owner`; no writes. |
| Compiler lib/path mismatch | NOT-REPRODUCED | `:destination-lib-path-mismatch`; no writes. |
| Unauthorized promotions | NOT-REPRODUCED | Eight `:undecided-promotion` blockers; no writes. |
| Drift after reviewed plan | NOT-REPRODUCED | `:snapshot-drift`; planted bytes remained unchanged and no destinations appeared. |
| Read-only destination failure | NOT-REPRODUCED | State `rolled-back`; all 26 recovery entries succeeded, source/tree restored byte-for-byte, zero destinations remained. |
| Persisted inverse | NOT-REPRODUCED | Actual CLI undo restored all 26 files to the pristine archive and removed all destinations. |
| `verification_complete` | NOT-REPRODUCED | True only on successful guarded proof; false for pre-write refusals, write rollback, and real `/bin/false` proof rollback. |
| `unknown_count` scope | JUDGMENT | Static unresolved `v/missing` produced count 1 and refusal. A constructed dynamic lookup remained count 0, while coverage explicitly states `dynamic_references: not claimed`; this is narrow but honest. |
| MCP text ⊇ structured | NOT-REPRODUCED | Text JSON and structured content were identical after JSON round-trip, including all 21 top-level fields. |
| CLI/MCP plan parity | NOT-REPRODUCED | Analysis projections were identical for the same live request. |
| CLI `TMPDIR` adoption | NOT-REPRODUCED | The real CLI placed receipt/inverse artifacts beneath the supplied `/var/tmp/.../tmp-cli-split`. |
| Skills | NOT-REPRODUCED | No skill changes; both mirrors are 70 lines and synchronization check exited 0. |
| Lane pins | NOT-REPRODUCED | Candidate: 52 fast, 6 integration, 32 battery; 90 namespaces; 998 + 464 = 1,462 tests. |
| Paved kondo | NOT-REPRODUCED | Exit 0; 0 errors, 0 warnings. |
| Babashka corpus | NOT-REPRODUCED | 873 tests, 7,511 assertions, 0 failures/errors. |
| `make mcp-test` | NOT-REPRODUCED | Oracle passed; 768 tests, 9,706 assertions, 0 failures/errors, 0 isolation violations. |
| Dry merge | JUDGMENT | Against observed current `origin/MCP/main` `8a64a092`, merge-tree reports exactly two additive conflicts. The composed tree passes `diff --check` and 62 focused tests/371 assertions. |

Required conflict composition:

- `docs/tech-tree.md`: retain both trunk’s structural-match/cardinality entries and the two namespace-split entries.
- `test/clj_surgeon/lane_manifest_test.clj`: retain trunk’s seven friction tests and the candidate’s twenty tests; the merged pin is `1469`, not `1449` or `1462`.

Key retained evidence: [real receipt](/var/tmp/forge/ns-split-review-913020c8-r1/cc-receipt.edn), [atomic-failure receipt](/var/tmp/forge/ns-split-review-913020c8-r1/atomic.receipt.edn), [MCP gate](/var/tmp/forge/ns-split-review-913020c8-r1/mcp.log), [Babashka gate](/var/tmp/forge/ns-split-review-913020c8-r1/bb.log), and [final dry merge](/var/tmp/forge/ns-split-review-913020c8-r1/merge-tree-final.out).

The normal full landing gate should still run on the resolved merge tree before pushing.