# Data-not-code round 8 — GO-WITH-OWED

Recorded 2026-09-13T03:47:03.556479633Z. Branch `fable/data-not-code-local`; input HEAD `9d057f8d4e68348ba519d0fe016b17b1c29e1e6e`; delivered HEAD `76cc24a49110453234550d805367fe278a71301a`.

Sol F1 is fixed for the named class: dependency paths containing two or more intermediate namespaces. The oracle now reaches a fixed point over reverse edges in the parsed src/test namespace graph, independent of lane and without a depth bound. The shared `form-identity/source-require-entries` parser supplies `:require`, `:use`, and `:require-macros` dependencies, including its prefix and conditional libspec forms; unknown syntax refuses. Visited-node membership terminates cycles. Receipts retain one deterministic shortest path per reachable changed namespace; they do not enumerate every possible path.

The new inventory selects **82 test namespaces: one more than 81**, exactly `clj-surgeon.mcp-prepared-wire-test`, with **zero removals**. All 82 ran serially and passed. The newly selected battery namespace passed **5 tests / 31 assertions / 0 failures / 0 errors**. Its retained dependency witness is `mcp-prepared-confirmation → mcp-contract → mcp-extraction`, matching Sol's finding. [Inventory delta](inventory-delta.json), [full inventory](impact/impact-fixed-point.edn), [individual results](impact/results-fixed-point.edn).

## Red first, then green

- `dff76da1af2c737781ad4f824e0bb57c1116ef8b`: committed generated declaration-chain regression. Depths 1 and 2 select their leaf; **depth 3 selects nothing and fails by name**. [Red log](oracle-red.log).
- `76cc24a49110453234550d805367fe278a71301a`: fixed-point closure, shared libspec parser, `fixed-point` CLI phase, and expanded pure self-check. Depths **1–8** all select their leaf, with an unrelated namespace excluded. Cutting each of the **36 chain edges** makes its leaf unselected. The matrix also covers cycles, multiple changed seeds, test namespaces as intermediates, empty change sets, input-order independence, **42 libspec/clause combinations**, the exact Sol path, and process-exit/nonempty-counter completion criteria. [Green log](oracle-green-final.log).

The earlier self-check assertion that deliberately excluded the depth-three test is strengthened to require its inclusion. The old all-path assertion becomes a shortest-witness assertion under the explicit receipt contract above; test selection is expanded, not reduced. The red commit is the bounded-oracle mutation control. No fixture namespaces are loaded: declarations are parsed as data.

Two development-check failures are retained: the first green draft expected lexicographic vector ordering where Clojure sorts vectors by count first; correcting the expectation to set equality exposed extra closing delimiters in the added test block. Those were corrected before the final green and lint. [First draft](oracle-green.log), [parse failure](oracle-final.log), [initial lint](lint.log). They did not require any product-code repair or gate retry.

## Verification, in requested order

| Check | Executed result | Evidence |
|---|---|---|
| `bb test/diff_impact.clj --self-test` | Red at depth 3, then complete green matrix | [red](oracle-red.log), [green](oracle-green-final.log) |
| `~/bin/clj-kondo --lint test/diff_impact.clj` | 0 errors, 0 warnings after formatting | [lint](lint-final.log), [format](format-green-final.log) |
| `bash test/diff-impact 3b6d5357 docs/observations/2026-09-12-data-not-code/round8/impact fixed-point` | **82/82**, **1,690 tests / 25,101 assertions / 0 failures / 0 errors**, exit 0 | [log](diff-impact.log), [results](impact/results-fixed-point.edn) |
| Focused witnesses of both sides, once | **97 tests / 2,680 assertions / 0 failures / 0 errors** | [commands](focused-driver.log), [machine report](report.edn) |
| `make test-fast`, once | **1,249 tests / 12,685 assertions / 0 failures / 0 errors**, 0 isolation violations/leaks; prerequisite adds 5 tests / 59 assertions | [log](test-fast.log), [receipt](test-fast-receipt.edn) |
| `make landing-gate-prewarm`, first attempt | All seven stages exit 0; `:state :passed`, `:problems []`, `:landing? false`; **0 repairs, no second invocation** | [log](prewarm.log), [receipt](prewarm-receipt.edn) |

Focused groups cover envelope admission, sandbox classification on JVM and Babashka, evidence rows and sleep identities, operation authority, probe authorization/closure/roots/nesting, refusal completeness, and receipt shape. Each command and counter result is retained. The all-lane run includes the unchanged 600-file memory-journal witness: **2 tests / 18 assertions**, **489,880 ms**, exit 0.

Impact worker-wall sum is **1,292,507 ms**. Fast command wall is **29268.065 ms**, with the measured fast sum **45,526 ms** under the unchanged 60,000 ms ceiling. Prewarm command wall is **402120.370 ms**; receipt gate wall is **399,954 ms**. These are verification costs, not performance comparisons.

The impact runner uses one fresh JVM per namespace, sequentially. Focused groups and Make invocations run one suite at a time under the host suite lock/lease. Namespace-owned subprocesses and Make's bounded internal workers retain their existing contracts. Temporary files use `/var/tmp/forge/datacode-fx`; no shared service was started or restarted.

| Prewarm stage | Exit | Wall ms |
|---|---:|---:|
| admit-transaction-recovery-battery | 0 | 11064 |
| alias-migration-test | 0 | 94191 |
| mcp-test | 0 | 95800 |
| test-bb | 0 | 91639 |
| test-bb-diagnostic | 0 | 225640 |
| repository-hygiene | 0 | 2096 |
| intent-audit | 0 | 2169 |

[gate.md](gate.md) is the byte-for-byte concatenation of the complete `test-fast.log` and `prewarm.log`, in that order, with no elisions or added annotations. Deliberate failure/refusal output inside witnesses is not a final counter failure.

## Snapshot binding and edit method

Fast and prewarm receipts share source digest `158aa255f5f988a8c6bbed1741dcabffc05c6df47f000eac426afa369100d092`. [Independent recomputation](source-binding.json) hashes all **483** runner-defined inputs and agrees with both receipts. The prewarm and actual impact inventory name the delivered HEAD. No tracked bytes changed after the green commit.

| Edited file | Intent | Mechanism | Refusal / repair text |
|---|---|---|---|
| `test/diff_impact.clj` | Red depth-class witness; fixed-point reverse closure; shared parser; phase label; mutation negatives and regression matrix | Native exact-form patches, standard-clj formatting, serialized clj-kondo, executed self-check | No Surgeon refusal; native route applies outside the two admitted automatic classes |

Only the two oracle commits were made. All new files are under **round8/** and remain **uncommitted in this worktree**. No push, merge, tag, shared install, records write, or probe/native measurement occurred.

## Limits and owed

The oracle discovers declared dependencies for the JVM `:clj` reader platform; computed dynamic requires and other platform branches are outside this claim. Rounds 6 and 7 established only their recorded two-edge sets; their broader coverage wording is superseded by this fixed-point run. Prewarm excludes `battery-fresh` and remains non-landing evidence. Independent acceptance of this repair is owed; this is the builder's local verification verdict.

Inherited limits remain: the shared artifact boundary is not every writer. `src/clj_surgeon/mcp_telemetry.clj:70-83,141` and `src/clj_surgeon/mcp_http_server.clj:238-244` still write outside that shared admission path; telemetry's `user.home` versus envelope passwd-home mismatch is unchanged. Hard-link evidence is admission-time only; races and bind mounts are not covered, and actual Darwin/non-ext4 behavior is untested. Receipts are evidence, not attestation; coordinated retained-row/receipt edits remain accepted by design. The shared CLI output-size defect is not repaired here. No new routing admission or speed claim follows.

Disagreements: none. Structured findings, measurements, commits, least-sure statements and owed work are in [report.edn](report.edn).
