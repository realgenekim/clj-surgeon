# Data-not-code round 5 — red-team fixes

Verdict: **PASS — local fixes and required prewarm complete.** No push, release,
merge or performance admission. Recorded 2026-09-12T22:38:47.340851+00:00.
Base: `4658dc124ee243300d19861402b2696ae1046321`, branch
`fable/data-not-code-local`. The landing SHA is the commit containing this report.

F1 now opens every cited receipt. A pure comparison binds namespace, runtime and
elapsed wall, derives n, mean, sample sd and conservative ratio from receipt
walls, then checks the selected runtime. Refusals name namespace and field;
absent files report `:missing-evidence-receipts`. The consistent 6→9000ms forgery
with `:bb→:jvm` and untouched receipts fails at
`clj-surgeon.battery-ledger-test`, field `[:bb :walls-ms]`. An independent
runtime swap fails at `:runtime`. Replaying the red-team's original p3.clj also
refuses its exact forged `clj-surgeon.mcp-recovery-test` row at
`[:bb :walls-ms]`, and refuses its nonexistent paths. That independent script
reports 38 rows / 456 receipts / zero disagreements (`redteam-p3-replay.log`).
All shipped **38 rows / 456 receipts** pass;
the rows and runtime assignments are unchanged. DATACODE-ROWS-001 now promises
these bindings. Receipt contents remain evidence inputs, not cryptographic
provenance: coordinated receipt tampering is not attestation.

F2 explicitly asserts that `policy-envelope-roots` resolves, and a named test
pins the policy witness at **12 executed assertions**. The actual definition
and default-call rename plant now fails the Var guard and count guard; the old
implementation silently executed only 11, or 9 after removing the two literals.
The isolated counter guard repeats the policy witness without double-counting
its assertions in the namespace's reported total.

F3 checks `unix:nlink` on the resolved final regular file. A count above one
refuses with `:write-outside-envelope`, `:reason :hard-link`, and the existing
path/effect/envelope fields. The real outside-file hard-link witness went red
with the sentinel appended to; it now refuses before APPEND and preserves the
sentinel. Directories are excluded from that file check. Existing successful
publication and symlink cases pass. Darwin and filesystem races remain unproved.

Coverage is **the shared artifact boundary and its inventoried consumers, not
every writer**. Specifically still uncovered and unchanged:
`src/clj_surgeon/mcp_telemetry.clj:70-78` and
`src/clj_surgeon/mcp_http_server.clj:234-240`. Telemetry resolves **user.home**
(`mcp_telemetry.clj:17-20`); the envelope resolves **passwd home**
(`receipt_artifacts.clj:62-71`). A user.home override can therefore move telemetry
outside the envelope. The exact follow-up inventory is in [owed.md](owed.md).
This block does not expand coverage to those writers.

The amended [preregistration](../round3/probe-measure-preregistration.md) makes
P/N a ratio of medians of complete wall in ms, reports sd per arm, and states the
variance comparison in ms. Equal success requires both arms 6/6 in each valid
repetition. Fable bets PROBE refuses exactly both planted non-test targets with
typed kinds, while NATIVE refuses neither; Astra's row remains. All six patch
bytes and SHA-256s are committed under tasks/. The common measure.py records
HEAD before task 1 and refuses drift, timestamps T0 immediately before the edit
and T1 after durable verdict publication. Static syntax/hash/apply checks pass.
**No runner init/cell, warm measurement image or experiment arm was run.**
The instrument has no end-to-end execution claim; future admission controls
remain required. Preparation/model time is disclosed separately from the
mechanical request-to-verdict interval.

| Check | Actual result | Evidence |
|---|---|---|
| Red-first regressions | 14 failures across all three named defects | red.log |
| New focused witnesses | 3 tests / 42 assertions, zero failures/errors | green-final.log |
| Policy rename plant | Var and count guards fail by name; source restored | policy-rename-red.log |
| Whole focused JVM namespaces | 62 / 1,659, zero failures/errors | focused.log, focused-summary.edn |
| BB lane-manifest namespace | 31 / 1,391, zero failures/errors | focused-bb.log |
| Lint, four changed Clojure files | 0 errors / 0 warnings, base also 0 / 0 | lint.sh, lint.log, lint-baseline.log |
| Evidence-script lint | 0 errors / 0 warnings after explicit require | lint-evidence-scripts-final.log |
| make test-fast, exactly once | 1,247 / 12,666; isolation 0; skipped 0; 31283.778 ms command wall | test-fast.log, test-fast-command.json |
| landing-gate-prewarm | 7 stages pass, no problems; 436264 ms gate wall | prewarm-receipt.edn, prewarm-final.log |
| Deftest census | 3 added, 0 removed | test/clj_surgeon/deftest_census.edn diff |
| Frozen patches | 6 hashes and git apply --check pass; no experiment run | preregistration-static-check.json |

Gate source digest: `44e8998d7eca6d1379aaff333e336c82d06d178ab01b46359592afcb3ffb87ce`. The report writer independently
recomputed it over the coordinator's exact source-path set after completion;
it matches the verified snapshot. Prewarm is not cold landing authority.
The nREPL and gate both waited for `/home/forge/tmp/suite-1.lock`; the JVM was
stopped before gates. Owned lease markers are released, no other seat stopped.

The first prewarm failed TEST-ISO-003 because this agent created focused.clj and
modified docs/tech-tree.md while rename-alias-test was observing the worktree.
Its 8 tests / 147 assertions passed, but those two outside changes correctly
failed isolation. No code or test was weakened. The one repair was to finish
all documentation preparation before the final run and leave the tree untouched
throughout it. Initial command wall was 204170.225277 ms, exit 2; full receipts
are in prewarm-red/. The final attempt is the only retry; test-fast was not repeated.

Two initial F1 delimiter errors and one evidence-script lint warning are retained
with their repairs. Formatting, native edit accounting, exact entrances and
census regeneration are documented in [commands.md](commands.md). No test was
removed, no runtime row edited, no budget changed, and no warning suppressed.
The current instruction says commit last, so red and green receipts land together.
Remaining review notes F4/F5/F8 are retained as debt, not claimed fixed. The
independent Opus report reviewed the base; independent review of this correction
and Darwin validation remain owed. No disagreement with the requested contracts.
