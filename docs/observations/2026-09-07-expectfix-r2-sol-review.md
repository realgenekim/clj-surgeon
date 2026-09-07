# NO-GO for landing `3c69c7be` on `MCP/main`

Two blocking defects remain, plus a narrower fidelity defect.

## Blocking findings

1. **`programs + expect` does not honor the aggregate guard.** The schema admits this route, but counts are derived only from lowered `changes` in [mcp_contract.clj](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/mcp_contract.clj:1005).

   Executed counterexamples:

   - Programs-only `{expect: {1,1,1}}` refused with derived `{0,0,0}` and returned a `next_call` rejected by the same positive-integer schema.
   - Mixed edit+program declared `{changes:1, edits:1, files:1}`, passed the guard, and committed an actual `{changes:2, edits:2, files:1}` result. Both transformations appeared in the source.

   This contradicts MCP-OP-EDIT-039/040/042 in [mcp-operation-contract-specs.md](/home/forge/src/clj-surgeon-fence/docs/intent/mcp-operation-contract/mcp-operation-contract-specs.md:66) and the schema’s admitted programs route in [mcp_schema.clj](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/mcp_schema.clj:544).

2. **Linked intent is still contradictory.** The rewritten EARS requirements make aggregate `expect` authoritative, but the owning LLD still says it is derived bookkeeping and disagreement must not override the transaction: [mcp-operation-contract-design.md](/home/forge/src/clj-surgeon-fence/docs/intent/mcp-operation-contract/mcp-operation-contract-design.md:549). Under the repository’s mandatory HLD → LLD → EARS chain, round 1’s intent blocker is not closed.

3. **Optional-root `next_call` is not “only expect replaced.”** With an explicit `workspace_root`, both routes now round-trip correctly. When the caller validly omits the optional root, [mcp_tool.clj](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/mcp_tool.clj:986) adds one to `next_call`; executed equality with the caller request plus only corrected `expect` was false. Pinning the resolved workspace may be operationally sensible, but it violates the stated exact-shape contract and lacks a witness for the omitted-root case.

## Requested probe classifications

| Item | Verdict | Executed result |
|---|---|---|
| (a) Files match, edits mismatch | **NOT-REPRODUCED** | Editor and direct routes refused `expect-mismatch`, named only `edits`, preserved bytes, and created no receipt entries. |
| (b) Create-only + expect | **JUDGMENT** | Safely refuses `expect-unsupported-on-route`, creates nothing, and publishes no `next_call`. Admitting the syntax to provide a typed semantic refusal is acceptable; the schema description should expose this exception. |
| (c) `changes: 0` | **JUDGMENT** | Still refuses `positive-integer`; schema also denies it. This is consistent with the published minimum and no caller dependency was found, but there remains no focused runtime witness—only schema-minimum inspection at [mcp_expect_guard_test.clj](/home/forge/src/clj-surgeon-fence/test/clj_surgeon/mcp_expect_guard_test.clj:232). |
| (d) `next_call` fidelity | **CONFIRMED-DEFECT** | Explicit-root round-1 failure is fixed: one top-level `expect`, exact JSON equality, replay schema-valid on both routes. Omitted-root requests gain `workspace_root`, violating only-expect-replaced equality. |
| (e) Silently dropped/under-counted declared field | **CONFIRMED-DEFECT** | Mixed edit+program committed two edits after aggregate `expect` authorized only one. Programs-only also returns an invalid zero-count replay. |
| (f) Existing callers/load-bearing old behavior | **NOT-REPRODUCED** | Existing matching callers passed. Routing plates that omit aggregate `expect` remain valid; existing program callers omit it. The focused alias-migration caller witness passed 1 test/22 assertions. |
| (g) Text ⊇ structured | **NOT-REPRODUCED** | The registered public tool callback’s text included the reason, expected/derived values, corrected aggregate, and source-unchanged statement. |

## Gates

- `make mcp-test`: **PASS** — 732 tests, 9,020 assertions, zero failures/errors.
- `~/bin/clj-kondo --lint <changed Clojure files>`: 0 errors, 1 warning, 1 informational finding. Both are outside changed hunks; the warning is the pre-existing unused `ctx` at `lane_manifest_test.clj:211`.
- Merge simulation against `origin/MCP/main` (`34fbddcf`): clean.
- Worktree clean at `3c69c7be1a09366d510a12048c899a6dd9694197`; review fixtures removed.