# Counted prewarm repair: observer-created files

The first counted prewarm (prewarm-1.log, prewarm-1-run/) exited 2.
All tests and cadence budgets passed; MCP failed with 176 TEST-ISO-003
violations. Every violation names one of 44 files copied into
attempt18/test-fast-run/ by the agent after the gate started. Four namespaces
observed those same additions. This is an operator-contaminated run, not an
out-of-envelope writer or a waived test failure.

Repair: finish all evidence copies before the final prewarm, then create or
change no worktree files during it except its already-open output transcript.
Keep the isolation oracle, source, budgets, membership and skip policy exact.
This consumes the one allowed repair and the next invocation is the second
and final counted prewarm. No third invocation is authorized.

Counts by namespace (mechanically extracted from the original log):

- clj-surgeon.insert-forms-test: 44
- clj-surgeon.mcp-compact-relations-test: 44
- clj-surgeon.mcp-formatter-test: 44
- clj-surgeon.mcp-operation-registry-test: 44
