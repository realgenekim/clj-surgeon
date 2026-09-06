# Astra: receipt fixture leak repaired

Recorded 2026-09-06T08:26:57.350919+00:00. Test-only repair; product receipt behavior is unchanged.

The failed-undo half of `owner-forms-resolves-only-the-selected-executor-entry` globally mocked `clojure.java.io/file` to return a relative proxy `receipt.edn`. Real telemetry therefore wrote its refusal event into the working directory instead of the selected events destination. This also exposed the root directory to the telemetry writer's permission changes.

The fixture now creates an actual absolute inverse receipt in an owned scratch directory, redirects only `events/events-file`, retains the real telemetry append, and cleans up in finally. It asserts the exact one refusal event, unchanged inverse receipt, unchanged root entries and supported POSIX permissions. Narrow undo/publication mocks remain. Existing test count is unchanged; seven assertions are added.

Faithful baseline ran the unchanged old test through an earlier classpath entry in a disposable cwd. All seven original assertions passed, but the external root-inventory witness failed: unexpected receipt.edn, exit1,4.975s. The repaired test passed fourteen assertions and root-inventory witness, exit0,4.925s. This is the motivating real failure, not a deliberately unrelated broken implementation. The initial BB invocation failed loading missing nREPL; it never ran a test and is not labeled RED. All logs remain in `/var/tmp/forge/astra-receipt-leak-fx/`; the baseline leaked artifact is retained there.

Actual MCP dogfood: the first edit refused `invalid-intent-form` in17.60ms because my require replacement contained two forms; source unchanged. The corrected complete require form and exact test let committed two edits in one file in696.19ms. Receipt hash4118f3a59b5ebf4ca49423893fbf1b75a1fc1588b656f3d1d4e9f756712a583f. Formatting and a native explicit telemetry require fix followed a lint warning; the paved `~/bin/clj-kondo` then passed with zero warnings/errors. This mixed route includes a real refusal cost and is not a speed comparison.

Separate reviewer gave static GO: real absolute destination, actual append, precise event count/type, cleanup and root invariants, no test-count or lane change. Focused JVM execution then supplied RED/GREEN. Normal combined landing gate remains pending; the companion Node fix has its own independently verified full normal gate.
