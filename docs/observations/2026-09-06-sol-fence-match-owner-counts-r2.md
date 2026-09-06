# HOLD

The public `inspect_clojure` handler does not enforce the 32,768-byte ceiling for ordinary match results.

Executed counterexamples on tip:

- 200 literal owners: helper measured 62,160 B and refused, but the public callback returned `ok=true`, 62,166 B, all 200 sites, and all 200 owner counts.
- Long-path/long-owner non-compressible request: helper measured 44,569 B and refused; public callback returned `ok=true`, 44,575 B and all 101 sites.
- No truncation occurred—the oversized results were published whole instead of being refused.

The cause is visible in [mcp_inspect_tool.clj](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/mcp_inspect_tool.clj:1076): `enforce-result-budget` gates prepared/special modes and continuations, but ordinary results take `:else raw-result`. The public handler reaches that path at [line 1133](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/mcp_inspect_tool.clj:1133). The new fence tests call `enforce-public-result-budget` manually rather than invoking the public handler, so they do not witness this bypass ([test helper](/home/forge/src/clj-surgeon-fence/test/clj_surgeon/mcp_inspect_tool_test.clj:1374)).

Execution results:

- Focused namespaces: 77 tests, 668 assertions, 0 failures/errors, 0 isolation violations.
- Round-2 parent `850f8955`:
  - 101: 34,339 B, helper refused; public callback admitted at 34,345 B.
  - 200: refused by the inner request-result limit, 66,385 > 65,536.
- Tip:
  - 101: 31,807 B, admitted.
  - 200: 62,160 B, helper refused; public callback admitted at 62,166 B.
- The specified MCP/main base `d95e6304` is not the 34,339-byte comparison point: it admitted 101 owners and its 200-owner helper result measured about 60 KB. The quoted 34,339 B belongs to round-2 parent `850f8955`.
- Differing whitespace/comment literal sources were present.
- Wildcard sources were present on every site.
- Two-file owner counts preserved first occurrence, including `inside=nil`, and summed to `[4,1]`.
- Literal-site hashes, file hashes, addresses, distinct sites, counts, and ordering were retained.
- `expect` success and cardinality-refusal behavior were identical on base and tip.
- [MCP-OP-FIELD-008](/home/forge/src/clj-surgeon-fence/docs/intent/mcp-operation-contract/mcp-operation-contract-specs.md:168) contains the reconstruction rule and fixture caveat.
- [Tool guidance](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/mcp_inspect_tool.clj:54) has the required from/to, wildcard, and `inside=null` qualifications.

END RECEIPT: `4aeb50276aa364ba38f0e43ae4495c4ed0e39509`, detached clean worktree, 2026-09-06T18:30:47Z. Temporary review fixtures were removed; nothing was landed or pushed.

> END RECEIPT (fence-run): worktree HEAD at review exit = 4aeb50276aa364ba38f0e43ae4495c4ed0e39509 = fenced sha.
