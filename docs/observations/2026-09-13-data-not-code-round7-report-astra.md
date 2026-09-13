# Data-not-code round 7 — GO-WITH-OWED

Recorded 2026-09-13T02:50:30.119548+00:00. Branch `fable/data-not-code-local`, verified HEAD `9d057f8d4e68348ba519d0fe016b17b1c29e1e6e`.

The landed Block B trunk is merged and both sides' witnesses pass. All **81 impacted namespaces** passed, followed by **97 focused tests**, the **once-only fast gate**, and the **final prewarm**. Prewarm explicitly records `landing? false`; this is local verification, not landing authority. No push occurred. This entire round7 directory is **uncommitted worktree evidence**, for Fable to copy to records.

## Merge and preservation

`f07f16867e07a8e8d7555e4eca0696f849249889` merges branch parent `eb6f59b4f6b5f127f6f0019a6d0030baa2747f3f` and landed trunk `759974c718889c52a05a1d0746c9c21d0f00dcdb`. The only textual conflicts were the obsolete line-based sleep map and the refusal-count pin. The branch's owner/ordinal/purpose/call identities survive, including the moved trunk stimulus. Refusals union to 168 kinds. `CENSUS_REGENERATE=1` produces exactly the parent test-set union: 2,600 tests, ten additions and zero removals relative to the branch parent. See [resolution](merge-resolution.md), [source preservation](merge-preservation.json), [census diff](census.diff), and [union proof](census-union.edn).

Trunk's probe classpath roots, shared namespace parser, complete closure/refusal before reload, test-target authorization, request nesting bound, Throwable boundary, and receipt closure facts survive. HTTP startup also retains destination-envelope initialization. The branch's artifact envelope, data-based sandbox classification, receipt-backed runtime evidence, and sleep identities remain intact.

`9d057f8d4e68348ba519d0fe016b17b1c29e1e6e` is the sole follow-up: the requested `merged` oracle label was missing from its accepted phase set. The change adds that label without changing discovery or pass criteria; the existing oracle self-check passes. No new product contract or routing admission was introduced. Native Git/read/patch workflow was used; neither automatically routed Surgeon class applies.

## Verification in requested order

| Check | Result | Evidence |
|---|---|---|
| Diff-impact from `3b6d5357`, phase `merged` | 81/81 namespaces; 1,685 tests; 25,070 assertions; zero failures/errors | [inventory](impact/impact-merged.edn), [results](impact/results-merged.edn), [log](diff-impact.log) |
| Focused witnesses of both sides | 97 tests; 2,680 assertions; zero failures/errors | [summary](verification-summary.edn), [commands](focused-commands.log) |
| `make test-fast`, only invocation | 1,249 tests; 12,685 assertions; zero failures/errors, leaks or isolation violations; prerequisite adds 5 tests/59 assertions | [receipt](test-fast-receipt.edn), [log](test-fast.log) |
| First prewarm | Exit 2; MCP tests 1,431 / 17,228 / 0 / 0; four builder-induced isolation violations | [failed run](prewarm-run/mcp/receipt.edn), [log](prewarm.log) |
| Final prewarm | All seven stages exit 0; `state :passed`, `problems []`, `landing? false` | [receipt](prewarm-final-receipt.edn), [log](prewarm-final.log) |
| Merge follow-up lint, through `~/bin/clj-kondo` | Zero errors/warnings | [lint](lint.log) |

The impact oracle used one fresh JVM per namespace, serially, including the battery-lane journal tests and the 600-file memory-journal witness (2 tests/18 assertions, 491,411 ms). It covered every changed src file's declared direct and one-intermediate test dependents, independent of lane. Namespace-owned subprocesses remain part of those tests. Focused suites likewise ran serially, then the fast and prewarm commands ran one at a time under the host suite lock/lease; Make retains its own bounded coordinator workers. The selected probe witnesses add 14 tests/745 assertions. The sandbox denial witness ran on JVM and Babashka with changed presentation text.

Impact worker-wall sum is 1,264,560 ms. Fast command wall is 31286.622 ms. First/final prewarm command walls are 173594.764/402220.482 ms. These are verification costs, not performance comparisons.

## The one repair

I created `round7/summarize.clj` during the first prewarm. Four concurrently checked namespaces correctly reported that new working-tree file as TEST-ISO-003 violations. No test assertion failed and no product regression was found. I finished preparing the evidence scripts before the final invocation and made no workspace edits while it ran. No test, assertion, isolation policy or source code was changed to obtain the pass. The first failure is retained, and the single allowed final attempt passed. See [repair record](prewarm-repair.md). The initial failed coordinator emitted suite/pool receipts but no completed top-level gate receipt.

[gate.md](gate.md) contains the full fast, initial-prewarm and final-prewarm command output verbatim. The deliberate stale-fixture FAIL lines inside the probe tests are consumed by those witnesses and do not represent final test-counter failures.

## Final stages and source binding

| Stage | Exit | Wall ms |
|---|---:|---:|
| admit-transaction-recovery-battery | 0 | 11004 |
| alias-migration-test | 0 | 95259 |
| mcp-test | 0 | 95277 |
| test-bb | 0 | 91216 |
| test-bb-diagnostic | 0 | 226288 |
| repository-hygiene | 0 | 2104 |
| intent-audit | 0 | 2135 |

Both successful gate receipts name source digest `eceb26eadf9d591cd70c068f7500d84c0184d29081a55fa98af7dddef7fc9069`. [Independent source binding](source-binding.json) recomputes that exact digest over the current 483 runner-defined inputs. The final prewarm names the exact delivered Git HEAD. Report/evidence files are outside that digest's input set; no tested source changed after verification.

## Limits, least sure, disagreements, owed

Prewarm excludes `battery-fresh` and is not full landing authority. Independent acceptance and the preregistered probe/native measurement remain separate; no warm-image experiment or wall bet ran here. The frozen experiment subject remains `759974c7`, distinct from this merged branch.

The oracle discovers declared dependencies through two edges, not arbitrary dynamic requires. Explicit probe witnesses supplement it. Artifact admission covers the shared artifact boundary only: telemetry's direct writer at `src/clj_surgeon/mcp_telemetry.clj:70-83,141` and readiness publication at `src/clj_surgeon/mcp_http_server.clj:238-244` remain uncovered. The telemetry `user.home` versus envelope passwd-home mismatch remains. Hard-link admission is check-time evidence, not race elimination; actual Darwin/non-ext4 behavior is untested. Receipts remain evidence, not attestation. These limits are inherited, not silently widened by the merge.

Disagreements: none. No push, tag, install, records write, or probe/native experiment occurred. Only the merge and oracle-label follow-up were committed; round7 evidence remains off the tip. Machine-readable details are in [report.edn](report.edn).
