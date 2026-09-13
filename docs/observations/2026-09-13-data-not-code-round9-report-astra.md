# Data-not-code round 9 — GO

Local verification is complete on `ce08cb0158503a50f98bc85c4af2f0761d04882b`, branch `fable/data-not-code-local`, from `76cc24a4`. All requested gates passed on their first invocation. Nothing was pushed or merged. Prewarm remains **not landing authority**.

The completed narrow baseline reproduced the counted battery: **one failure and 74 errors in txn-journal-test**; the other 81 selected namespaces passed individually. The original battery reported lane 3's aggregate failure. [Original battery](battery-reference.log), [baseline inventory](impact-before.json), and [journal red](before/clj-surgeon.txn-journal-test.log) retain the evidence. The red oracle commit names its log path.

`test/diff-impact` now rejects any set `CLJ_SURGEON_*_TMP` or `CLJ_SURGEON_ARTIFACT_ROOT`, including empty values, before taking a lane. It creates a fresh `/var/tmp/forge/clj-surgeon-suite-<pid>-<8 UUID hex>` root, matching the battery's `tmp-leak-support/secure-tmpdir!` convention behind suite-run. TMPDIR and startup java.io.tmpdir use that root. Inventory and namespace receipts expose the envelope; the final oracle checks actual child entry context before loading a test namespace. Six real override refusals and generated environment mutations are retained in [override-refusals.json](override-refusals.json) and [self-check](oracle-launcher-self-test.log).

The [scratch census](scratch-census.md) found four active defaults to repair: txn-journal, mission, and both memory helpers. Each now defaults to java.io.tmpdir and keeps its explicit override first. Other home/tmp matches are locks, provenance, or negative fixtures. The new census witness scans all test sources, including excluded memory tests. Regeneration added three test names and removed none.

The state-home decision is **policy-bounded registration**: admit the resolved substitute against current roots, then register it in invocation context. A bound state-home under TMPDIR succeeds; outside-policy, narrower-context, missing-parent, and final symlink escapes refuse without creating directories or changing the sentinel. Request maps cannot grant state-home/envelope authority. Nil and the next invocation retain their original context. The [standalone decision](../../../intent/alias-migration/state-home-decision.md) and DATACODE-ENV-005 record this interpretation of the brief's explicit negative.

| Check | Tests | Assertions | Failures / errors | Evidence |
|---|---:|---:|---:|---|
| Narrow baseline | 1,690 | 24,681 | 1 / 74 | [oracle-before.log](oracle-before.log) |
| New witnesses, red | 3 | 35 | 9 / 0 | [focused-red.log](focused-red.log) |
| New witnesses, green | 3 | 38 | 0 / 0 | [focused-green.log](focused-green.log) |
| Warm regressions | 120 | 902 | 0 / 0 | [warm-regressions.log](warm-regressions.log) |
| Narrow final oracle | 1,693 | 25,136 | 0 / 0 | [oracle-after.log](oracle-after.log) |
| make test-fast, once | 1,249 | 12,685 | 0 / 0 | [test-fast.log](test-fast.log) |
| make test-battery, once | 1,178 | 18,667 | 0 / 0 | [test-battery.log](test-battery.log) |

All **82/82** final impacted namespaces ran, with matching entry environments and no missing or duplicate results. [Final inventory](impact-after.json), [summary](summary-after.json). The full 600-file memory-journal scenario ran before and after. Worker-wall sums were 1,284,779 ms and 1,299,932 ms; these are verification costs, not performance comparisons.

The ten post-oracle focused commands all exited zero: envelope, sandbox JVM/BB, evidence rows/sleep identities, authority, probe closure/HTTP boundaries, refusal completeness, receipt shape, and scratch defaults. [Commands and outcomes](focused.log). The fast prerequisite also passed its 5 clj-splice tests / 59 assertions. Lint through `~/bin/clj-kondo` found zero errors and four unchanged baseline warnings (raw exit 2); [current](lint.log) and [baseline](lint-baseline.log) retain identical diagnostic kinds. Changed forms were formatted; unrelated formatter churn was reverted.

The counted battery took 174 seconds by its ledger clock, with zero skips, isolation violations, or leaks. The [counted receipt](counted-battery-receipt.edn) names the final SHA. The canonical append-only ledger keeps exactly that one new line **uncommitted**, with a full copy under round9. [Battery receipt](battery-receipt.edn).

Prewarm passed all 7 stages: admit-transaction-recovery-battery, alias-migration-test, mcp-test, test-bb, test-bb-diagnostic, repository-hygiene, intent-audit. Every stage exited zero; `:problems []`, `:state :passed`, `:landing? false`. It took 403,219 ms, with **zero repairs and no retry**. [Receipt](prewarm-receipt.edn), [complete log](prewarm.log).

Fast and prewarm share source digest `edfb3ca932e0ec82977e64604d92eebc09e9eb56c541112d10cf49bd71a2a21b` over **484** runner inputs, independently recomputed before and after all gates. Battery's normal receipt has no source digest; its counted SHA, unchanged committed sources, and the surrounding digest checks bind its subject. [Source binding](source-binding-after.json).

[gate.md](gate.md) is the byte-for-byte concatenation of test-fast.log, test-battery.log, and prewarm.log in execution order, with no headings, omissions, or added annotations. Its SHA-256 is `1a050fefbba5b3623fa5ac536f740dce99857255dc29ed91fdcd82ac70e38f26`. [Verbatim binding](gate-binding.json).

Commits:

- `30c2a285` — test(diff-impact): reproduce battery red in a narrow gate environment
- `98b69b82` — test(data-not-code): pin bounded state context and all-test scratch defaults
- `ce08cb01` — fix(data-not-code): bound state substitutions and use JVM test scratch roots

All round9 evidence remains uncommitted. The only tracked worktree deltas are the battery runner's required ledger append and generated namespace timing evidence; both are copied into round9 and remain uncommitted. No agent instructions, installed launcher, shared service, or frozen probe task was changed. Native reads and patches were used under the current routing rule; no Surgeon mutation/fallback or routing admission occurred.

One failed launcher draft compiled no tests because its new alias was required inside the form that referenced it. It was stopped and retained under [oracle-draft/](oracle-draft/); the corrected complete baseline is separate. The baseline captured post-test envelopes: HTTP-server-test initialized a narrower fixture launcher while retaining the narrow temp values. The final oracle captures entry context instead. [Environment notes](environment-notes.md) preserve that distinction. Both oracle roots and all 72 outside scratch roots named in baseline diagnostics were cleaned. The owned development nREPL was stopped before cold verification.

Remaining limits are unchanged: independent fence review is required before any future merge; mcp_telemetry.clj:70–83,141 and mcp_http_server.clj:238–244 still bypass the shared artifact boundary, and telemetry's user.home/passwd-home mismatch remains. Admission does not cover adversarial filesystem races or bind mounts; Darwin was not exercised. The impact graph covers declared clj-platform dependencies. Retained rows are evidence, not attestation; the existing shared CLI output-size defect remains outside this work. No timing experiment, speed claim, or all-writers claim follows. Structured findings, measurements, least-sure points, disagreements and owed work are in [report.edn](report.edn).
