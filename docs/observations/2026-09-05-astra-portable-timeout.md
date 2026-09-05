# Astra: portable Claude harness timeout

Recorded 2026-09-05T01:14:48.282578+00:00

The required Claude harness self-test failed before every fake child started: Linux has `/usr/bin/timeout` (uutils0.8.0), but this script required `gtimeout`. The selected binary now prefers `gtimeout` when present, otherwise `timeout`, and refuses with exit127 when neither exists. Execution quotes the resolved path and preserves TERM, one-second KILL escalation, output retention, and timeout status handling. No caller, task, model, or scoring policy changes.

The original full gate at01d9f0af remains failed in its receipt. Its Clojure stages passed with the JVM explicitly reporting eight available processors while physically pinned to cores2–5: BB850tests7203assertions, analyzer6tests25assertions, MCP919tests15214assertions, zero failures/errors. This setting addresses a separate unchanged census test that hardcodes eight workers; four-core execution exposed three failures in that test. No source fix or test waiver hides that portability issue.

After this shell fix, the actual fallback self-test passed in1.252s: fast child completed, failing child retained exit7, stalled child timed out independently. Independent fake-only review also proved preferred gtimeout from a directory containing spaces (all three exact invocations recorded) and the precise missing-binary127 diagnostic. No real model or JVM was launched by these self-tests.

All four previously unrun Make tail commands then passed: retained-result self-test and tracked verification, evidence-manifest self-test and verification. This is segmented completion of the required recipe after the localized repair, not a claim that either original full invocation exitedzero.

Receipts under `/var/tmp/forge/astra-program/`: `verification-0027`, `gate3-focus/diagnosis.md`, `verification-active8`, `claude-harness-selftest-diagnosis/REPORT.md`, `claude-harness-portable/result.json`, `claude-timeout-independent/result.json`, and `verification-tail/result.json`. Independent review covers script SHA256 `61b4a772855a4124ed87acb836f53aca151e2375ee87fb966a560e90f2cfaa93`.
