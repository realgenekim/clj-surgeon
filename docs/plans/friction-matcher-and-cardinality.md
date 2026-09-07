# Structural matcher and cardinality recovery

Gene-approved Recommendation 4, batch 1; execution brief dated 2026-09-07.
Evidence: pair-1/A-T1 requests 1–3 and responses 1–3, inb-f313b8.

The implementation follows the structural match recovery section of the
[operation-contract design](../intent/mcp-operation-contract/mcp-operation-contract-design.md)
and EARS MCP-OP-MATCH-001..003. HLD → that LLD → those rows → additive red
witnesses → code is the authorized cascade within this leaf.

No new transport, service, semantic resolution, mutation authority, benchmarks,
or routing policy. Keep every pre-existing match test byte-identical. Preserve
concrete source and addresses rather than inventing an expanded call site.

Gates: affected namespaces; `make mcp-operation-oracle`;
`clojure -M:clj-surgeon/test-fast`; `bb test/run_all.clj`;
`~/bin/clj-kondo --lint` changed Clojure files with recorded exit codes.
Temporary artifacts and report: `/var/tmp/forge/plan2/`. No push or servers.

Round 2 repairs Sol's confirmed receipt forgery through a hostile cardinality
request ID. MCP-OP-MATCH-004 specializes the existing MCP-OP-EDIT-038 encoder
contract without changing selection or structured identities. Red-first public
handler and pure renderer witnesses cover separators, glyphs, escaping, bounds,
and complete ordered failure evidence. Reader-discard exclusion remains the
unimplemented follow-up MCP-OP-MATCH-005. The same gates apply; report:
`/var/tmp/forge/plan2/friction1-r2-report.md`.
