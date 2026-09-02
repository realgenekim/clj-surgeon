# Prepared Confirmation Affinity Guidance — Frozen Red

## Scope

This checkpoint freezes the executable witnesses for
`MCP-OP-PREP-ACT-019..021`. Product code and caller guidance remain unchanged
at this checkpoint.

## Identity

- Approved edge-audit parent: `247e58aaea2db3cd115943d5c02a369354485ae5`
- Branch: `feature/prepared-confirm-affinity-guidance-20260831`
- Test namespace: `clj-surgeon.mcp-prepared-confirmation-test`

## Exact red gate

Run the focused test namespace in a cold JVM with the repository MCP test
classpath.

Expected result:

- 22 tests
- 115 assertions
- 96 passes
- 19 failures
- 0 errors
- nonzero process exit

The 19 failures are closed and intentional:

- 9 failures: three session-affinity and explicit-fallback requirements across
  the inspect description, edit description, and skill text.
- 6 failures: two outcome-discriminator requirements across the same three
  caller surfaces.
- 4 failures: canonical visible invalid-field data, hostile-key escaping, and
  the complete unknown-confirmation remedy.

The existing boolean outcome-shape characterization assertions pass at this
checkpoint. Any different count or error is not this frozen red.

## Implementation gate

Implementation may begin only from this exact red checkpoint. Green requires
all 22 tests and 115 assertions to pass without weakening the frozen witnesses.
