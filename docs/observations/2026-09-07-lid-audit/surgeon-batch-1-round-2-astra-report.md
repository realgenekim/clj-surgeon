# clj-surgeon LID batch 1 — round 2

Generated: 2026-09-07T18:08:10.000487+00:00

Worktree: `/home/forge/src/clj-surgeon-lidfix`
Branch: `astra/intent-contract-all-prefixes`
Round-2 base: `bd854b91e901440219eed08d9ceec58c4e212572`
Commit: `f61769da7a93de94b19e0cffc70dba6f7267ad1d`
Author: environment (`forge-anvil <forge-anvil@anvil>`).

## Result

Replaced the prefix exemption with `docs/intent/unlinked-spec-ids.edn`, an exact
map of 123 IDs to 144 missing implementation/test witness pairs. The gate
blocks unlisted missing pairs, stale repaired pairs, and absent IDs; malformed
ledger shapes also fail closed. It preserves all parsed evidence and reports
only admitted pairs as pending. The ledger is loaded from the audited root,
never regenerated at runtime. Passing `{}` still performs the unrestricted audit.

TRACE-005 and the matcher comment now explicitly say: legacy MCP-OP amendment
markers still witness parents; non-MCP amendment markers do not. No parser
behavior changed. The non-MCP witness already existed and is now qualified by
name; the legacy witness also directly asserts successful parent linkage.

Removed only `provider` and `upstream` from unused telemetry destructuring.
The complete `event` still reaches `mission-fields`, preserving field handling.
Default paved changed-files kondo exits 0 without `--fail-level`.

HLD → operation-contract design → TRACE-005 → red tests → implementation was
followed under the explicit round-2 authorization. As in batch 1,
`linked-intent-dev` was absent from local skill locations; the installed
`/home/forge/.claude/skills/linked-intent-testing/SKILL.md` supplied the
linked requirement and direct-witness guidance.

No push, merge, other-worktree edit, server start, prohibited-port contact,
or battery run. Temp files and logs stayed under `/var/tmp/forge/lid-batch/`.

## Diff stat (round 2 against bd854b91)

```text
docs/high-level-design.md                          |   4 +-
 .../mcp-operation-contract-design.md               |  23 ++--
 .../mcp-operation-contract-specs.md                |  19 +--
 docs/intent/unlinked-spec-ids.edn                  | 128 +++++++++++++++++++++
 docs/plans/intent-contract-all-prefixes.md         |  32 ++++--
 docs/tech-tree.md                                  |  12 ++
 src/clj_surgeon/mcp_intent_contract.clj            |  54 +++++----
 src/clj_surgeon/telemetry_events.clj               |   3 +-
 test/clj_surgeon/lane_manifest_test.clj            |   4 +-
 test/clj_surgeon/mcp_intent_contract_test.clj      |  71 +++++++++---
 10 files changed, 291 insertions(+), 59 deletions(-)
```

## (b) Red-first regression and exact real-input replay

Before implementation, the new permanent tests ran against the prefix gate:
**20 tests / 479 assertions, 15 failures, 0 errors, exit 15**. They expose
stale debt, the unlisted sibling witness kind, orphan IDs, and invalid ledgers.
Full red output follows (not just a selected tail):

```text
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge/lid-batch
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge/lid-batch
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-intent-contract-test

FAIL in (invalid-witness-debt-ledgers-fail-closed) (mcp_intent_contract_test.clj:159)
nil
expected: (= :invalid-witness-debt-ledger (try (gate raw ledger) nil (catch clojure.lang.ExceptionInfo e (:type (ex-data e)))))
  actual: (not (= :invalid-witness-debt-ledger nil))

FAIL in (invalid-witness-debt-ledgers-fail-closed) (mcp_intent_contract_test.clj:159)
[]
expected: (= :invalid-witness-debt-ledger (try (gate raw ledger) nil (catch clojure.lang.ExceptionInfo e (:type (ex-data e)))))
  actual: (not (= :invalid-witness-debt-ledger nil))

FAIL in (invalid-witness-debt-ledgers-fail-closed) (mcp_intent_contract_test.clj:159)
{"WTL-001" #{}}
expected: (= :invalid-witness-debt-ledger (try (gate raw ledger) nil (catch clojure.lang.ExceptionInfo e (:type (ex-data e)))))
  actual: (not (= :invalid-witness-debt-ledger nil))

FAIL in (invalid-witness-debt-ledgers-fail-closed) (mcp_intent_contract_test.clj:159)
{"WTL-001" #{:other}}
expected: (= :invalid-witness-debt-ledger (try (gate raw ledger) nil (catch clojure.lang.ExceptionInfo e (:type (ex-data e)))))
  actual: (not (= :invalid-witness-debt-ledger nil))

FAIL in (invalid-witness-debt-ledgers-fail-closed) (mcp_intent_contract_test.clj:159)
{"WTL-001" [:test]}
expected: (= :invalid-witness-debt-ledger (try (gate raw ledger) nil (catch clojure.lang.ExceptionInfo e (:type (ex-data e)))))
  actual: (not (= :invalid-witness-debt-ledger nil))

FAIL in (invalid-witness-debt-ledgers-fail-closed) (mcp_intent_contract_test.clj:159)
{:WTL-001 #{:test}}
expected: (= :invalid-witness-debt-ledger (try (gate raw ledger) nil (catch clojure.lang.ExceptionInfo e (:type (ex-data e)))))
  actual: (not (= :invalid-witness-debt-ledger nil))

FAIL in (exact-witness-debt-rejects-growth-repairs-and-orphans) (mcp_intent_contract_test.clj:138)
one exact missing pair can be deferred, then must retire: :implementation
expected: (= [{:type :stale-witness-debt, :intent intent, :source-kind kind}] (:violations (gate (raw "x" true true) {intent #{kind}})))
  actual: (not (= [{:type :stale-witness-debt, :intent "WTL-APPLY-001", :source-kind :implementation}] []))

FAIL in (exact-witness-debt-rejects-growth-repairs-and-orphans) (mcp_intent_contract_test.clj:141)
the other witness kind cannot silently join the ledger
expected: (false? (:ok (gate (raw "x" false false) {intent #{kind}})))
  actual: (not (false? true))

FAIL in (exact-witness-debt-rejects-growth-repairs-and-orphans) (mcp_intent_contract_test.clj:138)
one exact missing pair can be deferred, then must retire: :test
expected: (= [{:type :stale-witness-debt, :intent intent, :source-kind kind}] (:violations (gate (raw "x" true true) {intent #{kind}})))
  actual: (not (= [{:type :stale-witness-debt, :intent "WTL-APPLY-001", :source-kind :test}] []))

FAIL in (exact-witness-debt-rejects-growth-repairs-and-orphans) (mcp_intent_contract_test.clj:141)
the other witness kind cannot silently join the ledger
expected: (false? (:ok (gate (raw "x" false false) {intent #{kind}})))
  actual: (not (false? true))

FAIL in (exact-witness-debt-rejects-growth-repairs-and-orphans) (mcp_intent_contract_test.clj:143)
partial repair forces removal of that pair, retaining the other debt
expected: (= [{:type :stale-witness-debt, :intent intent, :source-kind :implementation}] (:violations (gate (raw "x" true false) {intent #{:implementation :test}})))
  actual: (not (= [{:type :stale-witness-debt, :intent "WTL-APPLY-001", :source-kind :implementation}] []))

FAIL in (exact-witness-debt-rejects-growth-repairs-and-orphans) (mcp_intent_contract_test.clj:147)
status changes cannot retain obsolete exceptions
expected: (false? (:ok (gate (raw " " false false) {intent #{:implementation :test}})))
  actual: (not (false? true))

FAIL in (exact-witness-debt-rejects-growth-repairs-and-orphans) (mcp_intent_contract_test.clj:148)
status changes cannot retain obsolete exceptions
expected: (false? (:ok (gate (raw "D" false false) {intent #{:test}})))
  actual: (not (false? true))

FAIL in (exact-witness-debt-rejects-growth-repairs-and-orphans) (mcp_intent_contract_test.clj:150)
an orphan ledger ID is a failure even without any source annotations
expected: (= [{:type :unknown-intent-debt, :intent "WTL-GONE-999"}] (:violations (gate (raw "x" true true) {"WTL-GONE-999" #{:test}})))
  actual: (not (= [{:type :unknown-intent-debt, :intent "WTL-GONE-999"}] []))

FAIL in (explicit-id-debt-never-hides-unknown-or-new-prefixes) (mcp_intent_contract_test.clj:189)
expected: (= allowed (:witness-debt-ledger gated))
  actual: (not (= {"WTL-FIXTURE-001" #{:implementation :test}} nil))

Ran 20 tests containing 479 assertions.
15 failures, 0 errors.

test-isolation: 0 violations across 1 namespace(s) (TEST-ISO-002/003/004/005/007/010)
```

The exact real-input attack script is `b2-real-probes.clj` beside this report.
It reads the same specs and implementation/test sources as the repository
audit, then changes immutable source maps: removes the real WTL-APPLY-001
test marker or adds a MEASURE-EVID-001 test marker. It does not mutate live
worktree source. Its coherence assertion is `(:ok result)`, so an induced
contract failure deliberately exits 1. An orphan ledger probe is included.

Original implementation (all three incorrectly green, exit 0):

```text
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge/lid-batch
{:mode "remove-witness", :baseline-ok true, :baseline-pending 144, :ok true, :pending 145, :violations []}
{:test 0, :pass 1, :fail 0, :error 0}
```

```text
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge/lid-batch
{:mode "add-marker", :baseline-ok true, :baseline-pending 144, :ok true, :pending 143, :violations []}
{:test 0, :pass 1, :fail 0, :error 0}
```

```text
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge/lid-batch
{:mode "orphan-ledger", :baseline-ok true, :baseline-pending 144, :ok true, :pending 144, :violations []}
{:test 0, :pass 1, :fail 0, :error 0}
```

After the fix: remove a real witness → RED, exit 1; pending debt stays 144.

```text
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge/lid-batch
{:mode "remove-witness", :baseline-ok true, :baseline-pending 144, :ok false, :pending 144, :violations [{:type :missing-test-witness, :intent "WTL-APPLY-001", :source-kind :test}]}

FAIL in () (b2-real-probes.clj:45)
[{:type :missing-test-witness, :intent "WTL-APPLY-001", :source-kind :test}]
expected: (:ok result)
  actual: false
{:test 0, :pass 0, :fail 1, :error 0}
```

Add the missing marker → RED, exit 1; the stale entry must be removed.

```text
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge/lid-batch
{:mode "add-marker", :baseline-ok true, :baseline-pending 144, :ok false, :pending 143, :violations [{:type :stale-witness-debt, :intent "MEASURE-EVID-001", :source-kind :test}]}

FAIL in () (b2-real-probes.clj:45)
[{:type :stale-witness-debt, :intent "MEASURE-EVID-001", :source-kind :test}]
expected: (:ok result)
  actual: false
{:test 0, :pass 0, :fail 1, :error 0}
```

A nonexistent ledger ID → RED, exit 1.

```text
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge/lid-batch
{:mode "orphan-ledger", :baseline-ok true, :baseline-pending 144, :ok false, :pending 144, :violations [{:type :unknown-intent-debt, :intent "WTL-GONE-999"}]}

FAIL in () (b2-real-probes.clj:45)
[{:type :unknown-intent-debt, :intent "WTL-GONE-999"}]
expected: (:ok result)
  actual: false
{:test 0, :pass 0, :fail 1, :error 0}
```

Keep the added marker and remove its exact ledger entry → GREEN, exit 0.
This temporary replay has 143 pending pairs; the checked-in original tree
retains its 144-pair ledger because that unrelated witness repair is not
part of this batch.

```text
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge/lid-batch
{:mode "remove-repaired-entry", :baseline-ok true, :baseline-pending 144, :ok true, :pending 143, :violations []}
{:test 0, :pass 1, :fail 0, :error 0}
```

## (d) Amendment rule and witness sensitivity

The production behavior was already correct; this is a spec/comment
clarification, not a newly repaired parser defect. The existing non-MCP
test was retained and renamed. A deliberate in-memory mutant truncating
non-MCP amendments to parents makes it RED (3 failures, exit 1), followed
by the unchanged production matcher GREEN (2 tests / 6 assertions, exit 0).
This is mutation sensitivity evidence, not a claim that the original
production matcher failed. Script: `b2-amendment-probe.clj`.

```text
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge/lid-batch

FAIL in (non-mcp-amendment-identifiers-are-not-parent-witnesses) (mcp_intent_contract_test.clj:119)
expected: (= #{amendment} (:implementation-witnesses result))
  actual: (not (= #{"TEST-ISO-001a"} #{"TEST-ISO-001"}))

FAIL in (non-mcp-amendment-identifiers-are-not-parent-witnesses) (mcp_intent_contract_test.clj:120)
expected: (= #{amendment} (:test-witnesses result))
  actual: (not (= #{"TEST-ISO-001a"} #{"TEST-ISO-001"}))

FAIL in (non-mcp-amendment-identifiers-are-not-parent-witnesses) (mcp_intent_contract_test.clj:121)
expected: (= [{:type :missing-implementation-witness, :intent parent, :source-kind :implementation} {:type :missing-test-witness, :intent parent, :source-kind :test}] (:violations result))
  actual: (not (= [{:type :missing-implementation-witness, :intent "TEST-ISO-001", :source-kind :implementation} {:type :missing-test-witness, :intent "TEST-ISO-001", :source-kind :test}] [{:type :missing-implementation-witness, :intent "TEST-ISO-001a", :source-kind :implementation} {:type :missing-test-witness, :intent "TEST-ISO-001a", :source-kind :test}]))
{:mode "mutant", :counters {:test 2, :pass 3, :fail 3, :error 0}}
```

```text
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge/lid-batch
{:mode "original", :counters {:test 2, :pass 6, :fail 0, :error 0}}
```

## Lint red → green

Both invocations used exactly:

```bash
~/bin/clj-kondo --lint src/clj_surgeon/mcp_intent_contract.clj \
  src/clj_surgeon/telemetry_events.clj test/clj_surgeon/lane_manifest_test.clj \
  test/clj_surgeon/mcp_intent_contract_test.clj test/clj_surgeon/telemetry_events_test.clj
```

Before: exit 2.

```text
src/clj_surgeon/telemetry_events.clj:243:12: warning: unused binding provider
src/clj_surgeon/telemetry_events.clj:243:21: warning: unused binding upstream
linting took 121ms, errors: 0, warnings: 2
```

After: exit 0; no warning suppression. Includes all batch-1 changed Clojure
files as well as all round-2 changes.

```text
linting took 115ms, errors: 0, warnings: 0
```

Formatter: exit 0.

```text
standard-clj fix v0.29.0

✓ /src/clj_surgeon/mcp_intent_contract.clj [8.01ms]
✓ /src/clj_surgeon/telemetry_events.clj [7.54ms]
✓ /test/clj_surgeon/lane_manifest_test.clj [16.55ms]
✓ /test/clj_surgeon/mcp_intent_contract_test.clj [10.66ms]
✓ /test/clj_surgeon/telemetry_events_test.clj [8.43ms]

All 5 files formatted with Standard Clojure Style 👍 [59.33ms]
```

## Ledger count by prefix and parity

| Prefix | IDs | Missing implementation | Missing test | Pairs |
|---|---:|---:|---:|---:|
| MEASURE- | 4 | 0 | 4 | 4 |
| OP-ALG- | 24 | 18 | 19 | 37 |
| PERF-SENT- | 50 | 0 | 50 | 50 |
| TEST-ISO- | 15 | 11 | 7 | 18 |
| WTL- | 30 | 30 | 5 | 35 |
| Total | 123 | 59 | 85 | 144 |

The frozen ledger equals the raw missing-pair snapshot captured before
implementation. The complete raw audit (including MCP) is byte-identical
under `pr-str` for bd854b91 and the new implementation on identical current
repository inputs. The intended difference is the debt gate.
Script: `b2-parity-ledger.clj`; exit 0.

```text
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge/lid-batch
{:same-input-raw-audit-byte-identical true, :ledger-equals-original-snapshot true, :spec-ids 690, :ledger-ids 123, :missing-pairs 144, :default-audit-ok true}
{:prefix "MEASURE-", :ids 4, :missing-implementation 0, :missing-test 4}
{:prefix "OP-ALG-", :ids 24, :missing-implementation 18, :missing-test 19}
{:prefix "PERF-SENT-", :ids 50, :missing-implementation 0, :missing-test 50}
{:prefix "TEST-ISO-", :ids 15, :missing-implementation 11, :missing-test 7}
{:prefix "WTL-", :ids 30, :missing-implementation 30, :missing-test 5}
```

## Gates and manifest pins

Source recomputation: **992 + 449 = 1441**. Intent-contract tests increased
**18 → 20**; no namespace or lane was added (88 total namespaces). The
fast gate visibly executes both intent-contract and lane-manifest tests.

```text
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge/lid-batch
{:r1 992, :adopted 449, :total 1441, :sum 1441, :intent-contract-tests 20, :manifest-namespaces 88}
```

`clojure -M:clj-surgeon/test-deps -m clj-surgeon.mcp-test-runner --ns clj-surgeon.mcp-intent-contract-test` — exit 0:

```text
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge/lid-batch
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge/lid-batch
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-intent-contract-test

Ran 20 tests containing 470 assertions.
0 failures, 0 errors.

test-isolation: 0 violations across 1 namespace(s) (TEST-ISO-002/003/004/005/007/010)
```

`make mcp-operation-oracle` — exit 0:

```text
# @spec MCP-OP-ORACLE-001
swipl -q -f test/mcp_operation_contract_oracle.pl
mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]
```

`clojure -M:clj-surgeon/test-fast` — exit 0:

```text

Testing clj-surgeon.mission-typist-test

Testing clj-surgeon.mission-usage-test

Testing clj-surgeon.ns-isolation-test

Testing clj-surgeon.outline-differential-test

Testing clj-surgeon.outline-memory-test

Testing clj-surgeon.quoted-var-refs-test

Testing clj-surgeon.scope-stream-test

Testing clj-surgeon.telemetry-events-test

Testing clj-surgeon.workspace-onboarding-test

Ran 582 tests containing 6042 assertions.
0 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.

test-isolation: 0 violations across 50 namespace(s) (TEST-ISO-002/003/004/005/007/010)
```

`make mcp-test` — exit 0:

```text
tmp-refused: java.io.tmpdir base=/tmp is a RAM-backed path by name (/tmp or /dev/shm). Launch with -Djava.io.tmpdir=/var/tmp/forge, or export TMPDIR=/var/tmp/forge before invoking bb (bb does not read JAVA_TOOL_OPTIONS -- see ~/bin/suite-run / seat-tmp-guard.sh).
--- runner clj-surgeon.memory.memory-test-runner (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge/lid-batch
tmp-refused: java.io.tmpdir base=/tmp is a RAM-backed path by name (/tmp or /dev/shm). Launch with -Djava.io.tmpdir=/var/tmp/forge, or export TMPDIR=/var/tmp/forge before invoking bb (bb does not read JAVA_TOOL_OPTIONS -- see ~/bin/suite-run / seat-tmp-guard.sh).
--- runner clj-surgeon.memory-battery-runner (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge/lid-batch
tmp-refused: java.io.tmpdir base=/tmp is a RAM-backed path by name (/tmp or /dev/shm). Launch with -Djava.io.tmpdir=/var/tmp/forge, or export TMPDIR=/var/tmp/forge before invoking bb (bb does not read JAVA_TOOL_OPTIONS -- see ~/bin/suite-run / seat-tmp-guard.sh).
--- runner clj-surgeon.mcp-test-runner (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge/lid-batch
tmp-refused: java.io.tmpdir base=/tmp is a RAM-backed path by name (/tmp or /dev/shm). Launch with -Djava.io.tmpdir=/var/tmp/forge, or export TMPDIR=/var/tmp/forge before invoking bb (bb does not read JAVA_TOOL_OPTIONS -- see ~/bin/suite-run / seat-tmp-guard.sh).
--- SELF_TEST_TMP with TMPDIR=/tmp -> /var/tmp ---
--- SELF_TEST_TMP with TMPDIR=/tmp/probe -> /var/tmp ---
--- SELF_TEST_TMP with TMPDIR=/dev/shm -> /var/tmp ---
--- SELF_TEST_TMP with TMPDIR=/dev/shm/probe -> /var/tmp ---
--- SELF_TEST_TMP with TMPDIR unset -> /var/tmp ---
--- SELF_TEST_TMP with TMPDIR=/var/tmp/forge/lid-batch/clj-surgeon-tmpleak-witness.3yp3iQ -> /var/tmp/forge/lid-batch/clj-surgeon-tmpleak-witness.3yp3iQ ---
--- SELF_TEST_TMP with TMPDIR=/var/tmp/forge -> /var/tmp/forge ---
tmp-leak ratchet witness passed
Installed analyzer gate /var/tmp/forge/lid-batch/clj-surgeon-kondo-path.yXfMju/home/bin/clj-kondo-admission and shell entrance /var/tmp/forge/lid-batch/clj-surgeon-kondo-path.yXfMju/home/bin/clj-kondo
clj-kondo admission path regression passed
cclsp already ready at http://127.0.0.1:7890/mcp
cclsp-start transient-health regression passed
cclsp ready at http://127.0.0.1:7890/mcp with restart-on-save TypeScript
cclsp launch PATH regression passed
direct cclsp client audit self-test: ok
```

The cclsp URL lines above come from self-tests with fake `curl`, `launchctl`,
and `bun` executables; they do not contact the named service or start a server.
The temp-refused lines are expected refusal witnesses, not suite failures.

JVM lane summary:

```text

Ran 747 tests containing 9530 assertions.
0 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.

test-isolation: 0 violations across 56 namespace(s) (TEST-ISO-002/003/004/005/007/010)
# @spec MCP-OP-ALIAS-053
```

`git diff --cached --check`: exit 0.

## Doubts and limits

- This is traceability enforcement, not proof that every marker denotes
  a sufficient behavioral test. The 144 pre-existing missing pairs remain debt.
- The gate enforces exact membership and retirement against the checked-in
  ledger. Deliberately editing that ledger to admit new debt violates its
  shrink-only policy and remains a review boundary; no Git-history lookup
  or second hidden baseline was added to the runtime audit.
- Legacy MCP amendment matching is intentionally historical; the rule is
  now explicit rather than universally claiming parent separation.
- No independent round-2 review is claimed. No live server or battery was
  used; all requested local gates, including optional mcp-test, were run.

## Commit receipt

```text
commit f61769da7a93de94b19e0cffc70dba6f7267ad1d
Author: forge-anvil <forge-anvil@anvil>
Commit: forge-anvil <forge-anvil@anvil>

    Pin intent witness debt by ID and require repaired entries to retire
    
    Replace prefix exemptions with an exact missing-witness ledger, reject stale and orphan debt, clarify legacy MCP amendment matching, and clear unused telemetry bindings for default lint.
    
    Co-Authored-By: Gene Kim <genek@itrevolution.com>
    Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>
```

Post-commit working tree: clean. No push performed.
