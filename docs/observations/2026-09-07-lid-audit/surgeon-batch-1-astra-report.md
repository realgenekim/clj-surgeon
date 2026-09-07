# clj-surgeon LID batch 1

Generated: 2026-09-07T17:31:16.116520+00:00
Worktree: `/home/forge/src/clj-surgeon-lidfix`
Branch: `astra/intent-contract-all-prefixes`
Base: `3085e737f1c2050b7a698ab1ade5d34a748e1afb`
Commit: `bd854b91e901440219eed08d9ceec58c4e212572`
Author: environment (`forge-anvil <forge-anvil@anvil>`).
No push, other worktree edits, server starts, prohibited-port contact, or battery run.
Temporary files, fixture homes, telemetry and logs stayed under `/var/tmp/forge/lid-batch/`.

## Result

Prefix-agnostic parsing now sees all 165 previously invisible rows, including five
lowercase TEST-ISO amendments. MCP-OP matching retains its original regex arm;
162 old/new literal cases produced byte-identical `pr-str` audit results.
The unrestricted original committed tree has **145 violations**: **59 missing
implementation witnesses, 85 missing test witnesses, one unknown implementation ID**.
Only TELEMETRY-EVENTS-001 was repaired. The default audit is green with **690 spec
IDs**, zero blocking violations, and **144 pending missing-witness violations across
123 IDs**. All pending violations remain available in the result.

Registered widening as MCP-OP-TRACE-005. Added the existing telemetry promise under
`docs/intent/telemetry-events/`, with direct markers on `record!` and its existing
JSONL and telemetry-off tests. No telemetry executable code changed. Updated the
corpus arithmetic by seven new contract tests: 1432 + 7 = 1439; no lane was added.

HLD → operation-contract/telemetry leaf designs → specs → witnesses → implementation
is recorded in the branch. The requested `linked-intent-dev` skill was not found
in the available catalogs or local skill locations; the installed
`/home/forge/.claude/skills/linked-intent-testing/SKILL.md` supplied the linked
requirement, misreading and direct-witness guidance. Gene's explicit bounded batch
brief supplied authorization to carry this cascade through to the branch commit.

## Diff stat

```text
 docs/high-level-design.md                          |   9 ++
 .../mcp-operation-contract-design.md               |  16 ++
 .../mcp-operation-contract-specs.md                |  15 ++
 .../telemetry-events/telemetry-events-design.md    |  15 ++
 .../telemetry-events/telemetry-events-specs.md     |  20 +++
 docs/plans/intent-contract-all-prefixes.md         |  52 +++++++
 docs/tech-tree.md                                  |   8 +
 src/clj_surgeon/mcp_intent_contract.clj            |  54 +++++--
 src/clj_surgeon/telemetry_events.clj               |   1 +
 test/clj_surgeon/lane_manifest_test.clj            |   4 +-
 test/clj_surgeon/mcp_intent_contract_test.clj      | 173 +++++++++++++++++++--
 test/clj_surgeon/telemetry_events_test.clj         |   2 +
 12 files changed, 341 insertions(+), 28 deletions(-)
```
## TDD: first RED on unchanged implementation

Fixture choice: literal `WTL-FIXTURE-001`, an implemented spec row with no implementation
or test annotations. This did not edit real WTL rows. The production parser was
unchanged for this run. Exact missing-witness diagnostics were expected; actual
violations were `[]`. The contract namespace exited 1.

Command (after sourcing the saved `env.sh`):
`clojure -M:clj-surgeon/test-deps -m clj-surgeon.mcp-test-runner --ns clj-surgeon.mcp-intent-contract-test`

```text
GUARDRAILS IS ENABLED. RUNTIME PERFORMANCE WILL BE AFFECTED.
Mode: :runtime  Async? false  Throw? false
Guardrails was enabled because the guardrails.enabled property is set to a (any) value.
WARNING: reset! already refers to: #'clojure.core/reset! in namespace: cfp-scheduler-killer.replay, being replaced by: #'cfp-scheduler-killer.replay/reset!
[([31mF[m)]
Randomized with --seed 1972490362

[31mFAIL[m in cfp-scheduler-killer.intent-contract-test/unlinked-spec-file-id-is-reported-test (intent_contract_test.clj:146)
AGENDA-WRITER-001 must be reported as a spec with no test witness
expected: (boolean (some (fn* [p1__78255#] (and (= :fail (:type p1__78255#)) (str/includes? (:message p1__78255#) "AGENDA-WRITER-001") (str/includes? (:message p1__78255#) "test witness"))) (clojure.core/deref reports)))
  actual: (not (boolean nil))
[31m1 tests, 1 assertions, 1 failures.[m
make: *** [Makefile:138: runtests-focus] Error 1
```
## Widened real-tree findings

The first unrestricted, numeric-only audit on the working tree found 138 violations
(`widened-before.log` / `widened-before.edn`). The permanent coverage assertion then
correctly failed: it saw 160 of the expected 165 original non-MCP rows. Five real
TEST-ISO amendment IDs end in a lowercase letter. The numeric witness regex also
incorrectly treated an amendment annotation as a witness for its parent.

An additional literal amendment witness went RED before repairing that grammar:

```text
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge/lid-batch/tmp -Xms64m -Xmx768m -XX:ActiveProcessorCount=2

FAIL in (amendment-identifiers-are-not-parent-witnesses) (mcp_intent_contract_test.clj:101)
expected: (= {parent :implemented, amendment :implemented} (:specs result))
  actual: (not (= {"TEST-ISO-001" :implemented, "TEST-ISO-001a" :implemented} {"TEST-ISO-001" :implemented}))

FAIL in (amendment-identifiers-are-not-parent-witnesses) (mcp_intent_contract_test.clj:102)
expected: (= #{amendment} (:implementation-witnesses result))
  actual: (not (= #{"TEST-ISO-001a"} #{"TEST-ISO-001"}))

FAIL in (amendment-identifiers-are-not-parent-witnesses) (mcp_intent_contract_test.clj:103)
expected: (= #{amendment} (:test-witnesses result))
  actual: (not (= #{"TEST-ISO-001a"} #{"TEST-ISO-001"}))

FAIL in (amendment-identifiers-are-not-parent-witnesses) (mcp_intent_contract_test.clj:104)
expected: (= [{:type :missing-implementation-witness, :intent parent, :source-kind :implementation} {:type :missing-test-witness, :intent parent, :source-kind :test}] (:violations result))
  actual: (not (= [{:type :missing-implementation-witness, :intent "TEST-ISO-001", :source-kind :implementation} {:type :missing-test-witness, :intent "TEST-ISO-001", :source-kind :test}] []))
```
The final grammar supports the existing one-letter amendments for non-MCP IDs,
with a boundary preventing partial matches. A separate legacy witness preserves
historical MCP behavior, including its lowercase-suffix handling.

For the complete pre-repair inventory, `baseline-input.py` read unchanged files from
this worktree and original bytes of touched files from the recorded base commit.
`baseline-audit.clj` passed those literal source maps through the widened contract.
It used no second worktree, checkout, or server. The 688 original discovered IDs
are 523 MCP-OP IDs plus the 165 newly covered IDs. This checkout is newer than the
ledger's audited commit; counts below are from this batch's actual base.
The final real-worktree audit adds TRACE-005 and TELEMETRY-EVENTS-001 for 690 IDs.

The existing exclusions for embedded-elaborator and substantiation-telemetry remain
unchanged. No new spec-document exclusions were added.

| Allowlisted TODO prefix | Missing implementation | Missing test | Total |
|---|---:|---:|---:|
| `MEASURE-` | 0 | 4 | 4 |
| `OP-ALG-` | 18 | 19 | 37 |
| `PERF-SENT-` | 0 | 50 | 50 |
| `TEST-ISO-` | 11 | 7 | 18 |
| `WTL-` | 30 | 5 | 35 |

Each prefix has a TODO row and removal condition in
`docs/plans/intent-contract-all-prefixes.md`. Only missing witnesses can be deferred.
Unknown IDs remain blocking in both source directions, even within these prefixes;
new prefixes are enforced automatically. The unrestricted audit is
`(clj-surgeon.mcp-intent-contract/audit-current-repository "." {})`.
The default result exposes `:pending-witness-violations` and the explicit
`:missing-witness-prefix-allowlist`; it does not filter out specs or witness sets.

### Dangling ID repaired

`TELEMETRY-EVENTS-001`: `:unknown-intent-witness`, `:source-kind :implementation`.
The actual harvested marker was the `mcp-telemetry/ledger!` docstring in
`src/clj_surgeon/mcp_telemetry.clj`. The references in `telemetry_events.clj` and
Makefile were prose rather than literal `@spec` annotations. Registering the row
and linking the already existing tests removes this violation without suppressing
unknown IDs or changing runtime behavior.

### Complete missing-witness inventory (144 diagnostics, 123 IDs)

These are scanner findings, not claims that all associated behavior is absent.
Existing discovery accepts implementation annotations in `src` Clojure files and
Makefile, and test annotations in `test` Clojure/Prolog files. It does not admit
shell or bench witnesses, expand abbreviated `/003/004` marker lists, or treat
implementation code under `test` as a source implementation. Those policies were
not broadened in this batch.

| Intent ID | Missing implementation | Missing test |
|---|---|---|
| `MEASURE-EVID-001` |  | yes |
| `MEASURE-WALL-001` |  | yes |
| `MEASURE-WALL-002` |  | yes |
| `MEASURE-WALL-003` |  | yes |
| `OP-ALG-COMMIT-002` |  | yes |
| `OP-ALG-COMMIT-004` | yes | yes |
| `OP-ALG-CONTEXT-002` |  | yes |
| `OP-ALG-DECODE-001` | yes | yes |
| `OP-ALG-EFFECT-002` | yes |  |
| `OP-ALG-EFFECT-003` | yes |  |
| `OP-ALG-EFFECT-004` | yes | yes |
| `OP-ALG-MCP-001` |  | yes |
| `OP-ALG-OUTCOME-001` | yes |  |
| `OP-ALG-OUTCOME-002` |  | yes |
| `OP-ALG-OUTCOME-003` | yes | yes |
| `OP-ALG-PARITY-001` | yes | yes |
| `OP-ALG-PARITY-002` | yes | yes |
| `OP-ALG-PERF-001` | yes | yes |
| `OP-ALG-PERF-002` | yes | yes |
| `OP-ALG-PREVIEW-001` | yes | yes |
| `OP-ALG-PREVIEW-002` | yes | yes |
| `OP-ALG-RECEIPT-001` | yes | yes |
| `OP-ALG-RECEIPT-002` | yes | yes |
| `OP-ALG-RECEIPT-003` |  | yes |
| `OP-ALG-REFUSE-001` | yes | yes |
| `OP-ALG-RUNTIME-001` |  | yes |
| `OP-ALG-SHADOW-001` | yes |  |
| `OP-ALG-STALE-001` | yes |  |
| `PERF-SENT-ADMIT-001` |  | yes |
| `PERF-SENT-ADMIT-002` |  | yes |
| `PERF-SENT-ATTEMPT-001` |  | yes |
| `PERF-SENT-ATTEMPT-002` |  | yes |
| `PERF-SENT-ATTEMPT-003` |  | yes |
| `PERF-SENT-AUTH-001` |  | yes |
| `PERF-SENT-AUTH-002` |  | yes |
| `PERF-SENT-AUTH-003` |  | yes |
| `PERF-SENT-BACKFILL-001` |  | yes |
| `PERF-SENT-BASELINE-001` |  | yes |
| `PERF-SENT-CLIENT-001` |  | yes |
| `PERF-SENT-CONFIG-001` |  | yes |
| `PERF-SENT-CUTOVER-001` |  | yes |
| `PERF-SENT-CUTOVER-002` |  | yes |
| `PERF-SENT-CUTOVER-003` |  | yes |
| `PERF-SENT-IDENT-001` |  | yes |
| `PERF-SENT-IDENT-002` |  | yes |
| `PERF-SENT-IMPORT-001` |  | yes |
| `PERF-SENT-IMPORT-002` |  | yes |
| `PERF-SENT-INVALID-001` |  | yes |
| `PERF-SENT-INVALID-002` |  | yes |
| `PERF-SENT-LEDGER-001` |  | yes |
| `PERF-SENT-LEDGER-002` |  | yes |
| `PERF-SENT-LEDGER-003` |  | yes |
| `PERF-SENT-LEDGER-004` |  | yes |
| `PERF-SENT-LEDGER-ROOT-001` |  | yes |
| `PERF-SENT-PROJECT-001` |  | yes |
| `PERF-SENT-PROMOTION-001` |  | yes |
| `PERF-SENT-PROMOTION-002` |  | yes |
| `PERF-SENT-PUBLISH-001` |  | yes |
| `PERF-SENT-PUBLISH-002` |  | yes |
| `PERF-SENT-RECEIPT-001` |  | yes |
| `PERF-SENT-RECEIPT-002` |  | yes |
| `PERF-SENT-RECONCILE-001` |  | yes |
| `PERF-SENT-RECOVERY-001` |  | yes |
| `PERF-SENT-RECOVERY-002` |  | yes |
| `PERF-SENT-RECOVERY-003` |  | yes |
| `PERF-SENT-RECOVERY-004` |  | yes |
| `PERF-SENT-RELEASE-001` |  | yes |
| `PERF-SENT-RETAIN-001` |  | yes |
| `PERF-SENT-SCHEDULE-001` |  | yes |
| `PERF-SENT-SCHEDULE-002` |  | yes |
| `PERF-SENT-SCHEDULE-003` |  | yes |
| `PERF-SENT-SCHEDULE-004` |  | yes |
| `PERF-SENT-SURFACE-001` |  | yes |
| `PERF-SENT-THRESHOLD-001` |  | yes |
| `PERF-SENT-TIME-001` |  | yes |
| `PERF-SENT-VERDICT-001` |  | yes |
| `PERF-SENT-VERDICT-002` |  | yes |
| `PERF-SENT-VERDICT-003` |  | yes |
| `TEST-ISO-001a` | yes | yes |
| `TEST-ISO-001b` | yes | yes |
| `TEST-ISO-001c` | yes | yes |
| `TEST-ISO-003` | yes |  |
| `TEST-ISO-004` | yes |  |
| `TEST-ISO-005` | yes |  |
| `TEST-ISO-006` | yes |  |
| `TEST-ISO-007` | yes |  |
| `TEST-ISO-008` |  | yes |
| `TEST-ISO-009` |  | yes |
| `TEST-ISO-010` | yes |  |
| `TEST-ISO-011` |  | yes |
| `TEST-ISO-012` |  | yes |
| `TEST-ISO-RACE-001` | yes |  |
| `TEST-ISO-RACE-002` | yes |  |
| `WTL-APPLY-002` | yes |  |
| `WTL-APPLY-003` | yes |  |
| `WTL-APPLY-004` | yes |  |
| `WTL-APPLY-006` | yes |  |
| `WTL-APPLY-007` | yes |  |
| `WTL-APPLY-008` | yes |  |
| `WTL-APPLY-010` | yes |  |
| `WTL-APPLY-011` | yes |  |
| `WTL-APPLY-012` | yes |  |
| `WTL-CLI-002` | yes |  |
| `WTL-CLI-003` | yes |  |
| `WTL-HAND-002` | yes |  |
| `WTL-HAND-004` | yes | yes |
| `WTL-HAND-005` | yes |  |
| `WTL-INV-002` | yes |  |
| `WTL-INV-003` | yes |  |
| `WTL-INV-005` | yes |  |
| `WTL-INV-006` | yes |  |
| `WTL-INV-008` | yes |  |
| `WTL-PLAN-002` | yes | yes |
| `WTL-PLAN-003` | yes |  |
| `WTL-PLAN-004` | yes |  |
| `WTL-PLAN-006` | yes | yes |
| `WTL-PRUNE-008` | yes | yes |
| `WTL-PRUNE-009` | yes | yes |
| `WTL-SEAL-002` | yes |  |
| `WTL-SEAL-003` | yes |  |
| `WTL-SEAL-004` | yes |  |
| `WTL-SEAL-006` | yes |  |
| `WTL-SEAL-007` | yes |  |

### Makefile-only implementation witnesses

21 IDs have implementation annotations only in Makefile under the current source
policy. The widening reveals four non-MCP IDs in addition to the ledger's 17 MCP
IDs. They were retained, not silently rewritten. A Makefile implementation marker
is accepted by the existing contract; absent test markers still appear above.

- `MCP-OP-ADMIT-130`
- `MCP-OP-ADMIT-150`
- `MCP-OP-ALIAS-036`
- `MCP-OP-ALIAS-053`
- `MCP-OP-ANALYZER-008`
- `MCP-OP-ORACLE-001`
- `MCP-OP-TMPHYG-001`
- `MCP-OP-TMPHYG-002`
- `MCP-OP-TMPHYG-003`
- `MCP-OP-TMPHYG-004`
- `MCP-OP-TMPHYG-006`
- `MCP-OP-TMPHYG-007`
- `MCP-OP-TMPHYG-008`
- `MCP-OP-TMPHYG-010`
- `MCP-OP-TMPHYG-011`
- `MCP-OP-TMPHYG-012`
- `MCP-OP-TMPHYG-013`
- `TEST-ISO-001`
- `TEST-ISO-009`
- `TEST-ISO-009a`
- `TEST-ISO-009b`

## Gate receipts

All commands source `/var/tmp/forge/lid-batch/env.sh`: TMPDIR and java.io.tmpdir are
under the batch directory; a bounded standalone JVM and isolated event destination
are used. No nREPL/server discovery or live service gate was run.

### Contract namespace — exit 0

`clojure -M:clj-surgeon/test-deps -m clj-surgeon.mcp-test-runner --ns clj-surgeon.mcp-intent-contract-test`

```text
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge/lid-batch/tmp -Xms64m -Xmx768m -XX:ActiveProcessorCount=2
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge/lid-batch/tmp -Xms64m -Xmx768m -XX:ActiveProcessorCount=2
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-intent-contract-test

Ran 18 tests containing 458 assertions.
0 failures, 0 errors.

test-isolation: 0 violations across 1 namespace(s) (TEST-ISO-002/003/004/005/007/010)
```
### Oracle — exit 0

`make mcp-operation-oracle`

```text
# @spec MCP-OP-ORACLE-001
swipl -q -f test/mcp_operation_contract_oracle.pl
mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]
```
### Fast lane — exit 0

`clojure -M:clj-surgeon/test-fast`

```text

Testing clj-surgeon.outline-memory-test

Testing clj-surgeon.quoted-var-refs-test

Testing clj-surgeon.scope-stream-test

Testing clj-surgeon.telemetry-events-test

Testing clj-surgeon.workspace-onboarding-test

Ran 580 tests containing 6030 assertions.
0 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.

test-isolation: 0 violations across 50 namespace(s) (TEST-ISO-002/003/004/005/007/010)
```
The first fast run caught the newly added test count against the old 1432 pin.
The manifest arithmetic was corrected to 1439 after the final seventh regression,
and the full requested lane was rerun. See `test-fast-first.log` for the first failure.

### Formatting — exit 0

`npx --yes @chrisoakman/standard-clojure-style fix` on all five changed Clojure files.

```text
standard-clj fix v0.29.0

✓ /src/clj_surgeon/mcp_intent_contract.clj [7.85ms]
✓ /src/clj_surgeon/telemetry_events.clj [6.04ms]
✓ /test/clj_surgeon/lane_manifest_test.clj [12.29ms]
✓ /test/clj_surgeon/mcp_intent_contract_test.clj [5.75ms]
✓ /test/clj_surgeon/telemetry_events_test.clj [8.14ms]

All 5 files formatted with Standard Clojure Style 👍 [48.18ms]
```
### Kondo — exit 0, no new diagnostics

`~/bin/clj-kondo --lint src/clj_surgeon/mcp_intent_contract.clj src/clj_surgeon/telemetry_events.clj test/clj_surgeon/mcp_intent_contract_test.clj test/clj_surgeon/telemetry_events_test.clj test/clj_surgeon/lane_manifest_test.clj --cache false --fail-level error`

```text
src/clj_surgeon/telemetry_events.clj:243:12: warning: unused binding provider
src/clj_surgeon/telemetry_events.clj:243:21: warning: unused binding upstream
linting took 91ms, errors: 0, warnings: 2
```
Both warnings also occur on the original telemetry source, verified independently:

```text
/var/tmp/forge/lid-batch/baseline/src/clj_surgeon/telemetry_events.clj:243:12: warning: unused binding provider
/var/tmp/forge/lid-batch/baseline/src/clj_surgeon/telemetry_events.clj:243:21: warning: unused binding upstream
linting took 27ms, errors: 0, warnings: 2
```
The initial fresh-cache invocation crashed inside clj-kondo cache synchronization
with NullPointerException (`kondo-cache-error.log`). Disabling caching avoided it.
An introduced redundant-let warning was fixed. No unrelated telemetry binding was
changed to erase an existing warning. The final lint command explicitly fails on
errors; it is not a claim of warning-free source.

### MCP compatibility — exit 0

`clojure -M /var/tmp/forge/lid-batch/legacy-parity.clj` loads the original contract
under a separate namespace and compares serialized audit results over 162 cases:
three statuses × each implementation/test direction × six legacy ID shapes.

```text
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge/lid-batch/tmp -Xms64m -Xmx768m -XX:ActiveProcessorCount=2
{:cases 162, :byte-identical true, :failures []}
```
## Doubts and remaining limits

- All 165 original non-MCP rows are visible, but full missing-witness enforcement
  for the five allowlisted prefixes remains deferred. New missing-witness debt
  in those prefixes can also be deferred until their entries are removed.
- The unrestricted final audit is intentionally RED with 144 missing-witness
  diagnostics. The batch gate is green because those diagnostics are explicit
  pending debt, not because 144 unrelated links were repaired.
- Makefile-only witnesses, abbreviated marker lists, shell/bench discovery,
  test-infrastructure source classification, and the two pre-existing excluded
  leaves remain follow-up decisions. This batch does not solve other ledger gaps.
- Traceability proves named linkage, not the correctness of every claimed behavior.
  Telemetry's executable behavior was unchanged and its existing tests ran in the
  fast lane; no battery or live-service validation is claimed.
- MCP legacy quirks were preserved deliberately. Non-MCP IDs use the documented
  numeric grammar plus one lowercase amendment letter; arbitrary other future
  identifier grammars would need an explicit extension.

Machine-readable full original violations: `baseline-widened.json`.
Final real-worktree findings: `findings.json`; unrestricted final failures are its
`raw-violations`. Frozen branch patch: `batch1.patch`.

Final commit verified on the requested Astra branch with both requested co-author
trailers and the environment author. Worktree is clean. Transient JVM fixture
directories were removed; logs and replay evidence were retained.
Completed: 2026-09-07T17:31:16.165269+00:00
