# Astra ethnography: fourth hour

<!-- agent-usage-window-end: 2026-09-05T02:05:00Z -->

Window: September 5 01:05–02:05 UTC, September 4 18:05–19:05 PDT. Collected once after the quiet measurement window, at 2026-09-05T02:39:57.934083Z, using explicit bounds and program-owned server root. Receipt: /var/tmp/forge/astra-program/usage-hour-4.json, status ok.

| Provider | Sessions | Clojure-relevant | Recognized client Surgeon calls | Native read actions | Native patch actions | Verification actions |
|---|---:|---:|---:|---:|---:|---:|
| Codex | 6 | 5 | 0 | 86 | 1 | 24 |
| Claude | 3 | 3 | 0 | 3 | 0 recorded | 15 |

These provider populations are not matched cohorts. Python HTTP gateways remain opaque to client MCP classification, and shell action labels do not prove every read touched existing Clojure. Zero recognized client calls does not mean zero use. The owned server records 25 calls, 21 successful and four refused, plus one startup. Typed refusals: change-owner-mismatch once, invalid-intent-form twice, invalid-mcp-request once. Its historical emitter labels 14 writes apply_clojure_changes and 11 inspections; alias migration coverage remains incomplete. Do not add independently observed calls to these counts without deduplication.

Recorded service wall totals 8.659 seconds, median 160 ms, maximum 1.079 seconds. Inspections account for 0.671 seconds, writes 7.988 seconds. These intervals exclude model decisions, reviews, suite waits and integration. Codex event-clock totals (milliseconds across this population, not one elapsed task) are {"collaboration": 10006, "context-compaction": 306527, "git": 2, "human-input": 0, "model-message": 89136, "model-reasoning": 1235979, "native-patch": 10, "native-read": 8866, "shell": 30077, "verify": 0}. Claude has no equivalent complete clock evidence.

Route phases predominantly classify reads, verification and git; the gateway gap prevents reliable fallback inference. The controlled cohort and individual public-wire receipts supply the actual route evidence separately. This period includes integration waiting and a reservation mistake: a quiet file held before an existing landing drained delayed its next test process. Waiting time is not test execution and cannot support a suite speed claim.

The falsifiable next improvement remains dual-sided operation coverage and explicit process phases. Fable's telemetry branch must show one success and one refusal under their actual public names, while the watcher must separate runnable work from reservations and tests. No new performance or adoption claim comes from this aggregate. The unchanged collector self-test already passed in the earlier full verification stage; it was not rerun without a relevant change.
