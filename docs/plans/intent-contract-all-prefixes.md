# Prefix-agnostic intent contract — batch 1

The September 7 audit found that the MCP-OP-only parser hid 165 intent rows
and the dangling TELEMETRY-EVENTS-001 annotation. The HLD's traceability rule
flows through the operation-contract design to MCP-OP-TRACE-005 and its direct
tests. This batch widens parsing without changing existing MCP-OP semantics.

The first regression uses a literal WTL-shaped implemented row without either
witness. Its expected result is the two exact missing-witness diagnostics.
The behavior matrix also covers all statuses, each witness direction, unknown
annotations, a future prefix, and legacy MCP-OP identifiers. Literal fixtures
avoid filesystem setup; the existing repository test covers real discovery.

Run the unrestricted real-tree audit before adding any exception. Preserve the
findings in the batch report. Repair only TELEMETRY-EVENTS-001 by registering
the existing ledger promise and linking its existing implementation and direct
test. Postpone other missing-witness debt only through explicit prefix TODOs;
retain every parsed row and never exempt unknown annotations. Source discovery,
Makefile witness policy, other leaf implementations, and suite wiring are out
of scope. The existing excluded spec documents remain excluded.

Acceptance: red fixture before implementation, green contract namespace,
`make mcp-operation-oracle`, `clojure -M:clj-surgeon/test-fast`, formatter and
`~/bin/clj-kondo` on changed Clojure files. No server, battery, push, or other
worktree access. Logs and the final report live under `/var/tmp/forge/lid-batch/`.
Gene's batch brief authorizes this bounded cascade and final branch commit.

## Explicit missing-witness debt

The first numeric-only run found 138 violations, but a real-tree coverage test
exposed five TEST-ISO amendment rows (001a/001b/001c/009a/009b) that need their
lowercase suffix preserved. The corrected parser audited the original committed
tree and found 145 violations: 144 missing witnesses across the prefixes below
and one unknown implementation annotation for TELEMETRY-EVENTS-001 in
`mcp_telemetry.clj`. These are linkage findings, not
proof that behavior is absent. The unchanged scanner reads implementation
from Clojure source and Makefile, and tests from Clojure and Prolog files.

| TODO prefix | Missing implementation / test | Removal condition |
|---|---:|---|
| `MEASURE-` | 0 / 4 | Link the four measurement rows to direct executable tests. |
| `OP-ALG-` | 18 / 19 | Reconcile the operation-algebra rows with implementation and direct test annotations. |
| `PERF-SENT-` | 0 / 50 | Decide and test the source policy for shell/bench witnesses, then reconcile all 50 rows. |
| `TEST-ISO-` | 11 / 7 | Admit runner implementations in test infrastructure deliberately and reconcile annotations, including amendment IDs. |
| `WTL-` | 30 / 5 | Reconcile worktree implementation and direct test annotations, including abbreviated marker lists. |

These five entries allow only missing-witness debt. They do not hide rows,
annotations, or unknown IDs. Inspect `:pending-witness-violations` in the
ordinary result; call `(audit-current-repository "." {})` for all failures.
New prefixes are enforced without configuration. Remove each exception once
its unrestricted missing-witness count reaches zero. Do not expand this table
without a separate bounded repair decision.
