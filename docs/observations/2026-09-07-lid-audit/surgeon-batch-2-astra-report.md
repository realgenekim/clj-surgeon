# clj-surgeon LID gate/HLD batch — B3 report

Generated: 2026-09-07T19:16:27.713538+00:00
Worktree: `/home/forge/src/clj-surgeon-lidfix2`
Branch: `astra/lid-batch-2-perf-gate-hld`
Base: `42c55d964fc8ef321063493bffb2eca90b6b0a00`
Commit: `94715053e02388444aaf3e35990c4b1db63a5a2f` (not pushed; clean worktree).
Author from environment: `forge-anvil <forge-anvil@anvil> 1788808587 +0000`

## Result and scope

Gap 3 is wired into `make mcp-test`, with the 50-row sentinel audit and a
permanent scratch-copy self-test. The audit had a second defect: it actually
reported **49/49**, silently omitting `PERF-SENT-LEDGER-ROOT-001` on both sides.
The repaired parser audits all **50/50** and rejects that ID's disappearance.
The full sentinel behavioral suite remains separate. No generic source-policy
expansion or debt-ledger change was made.

The HLD is append-only: 124 added lines, three seven-sentence flagship
sections, mission-name disambiguation, the gate placement, and references for
all 15 leaves named by the audit. Mission-ledger implementation/intent work
is deferred to the next batch, as requested.

## Lane decision and linked intent

Read `test/clj_surgeon/lane_manifest.clj`, its witness namespace,
`docs/intent/test-isolation/test-isolation-design.md`, and the Make lanes.
The JVM `:fast` lane forbids child processes. The shell audit therefore runs
as a prerequisite of the ordinary **Make merge gate**, alongside its existing
shell checks, rather than as a subprocess inside the fast JVM. The existing
full sentinel target reuses the same prerequisite. `runtests`, `landing-gate`,
`test`, and `test-full` reach `mcp-test`; the bare fast alias checks the wiring
as data and does not execute shell.

HLD ordinary-gate promise → operation-contract LLD → **MCP-OP-TRACE-006** →
`sentinel-intent-audit-is-required-by-the-merge-gate` and the permanent shell
self-test → Make prerequisite and multipart parser. The checked-in plan is
`docs/plans/sentinel-intent-gate-and-hld.md`. The working-tree Surgeon skill
selects native edits; the available installed LID skill is
`/home/forge/opt/claude-skills/linked-intent-testing/SKILL.md` (the repository's
older `linked-intent-dev` name was not present). The explicit batch brief
authorized the bounded cascade through final commit.

## Diff stat

```text
Makefile                                           |  10 +-
 docs/high-level-design.md                          | 124 +++++++++++++++++++++
 .../mcp-operation-contract-design.md               |  19 ++++
 .../mcp-operation-contract-specs.md                |   9 ++
 docs/plans/sentinel-intent-gate-and-hld.md         |  43 +++++++
 docs/tech-tree.md                                  |  13 +++
 test/clj_surgeon/lane_manifest_test.clj            |   4 +-
 test/clj_surgeon/mcp_intent_contract_test.clj      |  17 +++
 ...ormance_regression_sentinel_intent_self_test.sh |  48 ++++++++
 .../performance_regression_sentinel_intent_test.sh |   4 +-
 10 files changed, 286 insertions(+), 5 deletions(-)
```

## (a) Red / green

The new fast wiring witness ran against the original Makefile and failed all
three execution-edge assertions (namespace command corrected to the dedicated
`test-deps` alias; the first attempt appended `--ns` to `test-fast` and was
refused as an unknown lane before tests ran):

```text
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-intent-contract-test

FAIL in (sentinel-intent-audit-is-required-by-the-merge-gate) (mcp_intent_contract_test.clj:40)
the ordinary merge gate must execute the sentinel witness audit
expected: (some #{target} (:prerequisites merge-gate))
  actual: (not (some #{"performance-regression-sentinel-intent-test"} ["mcp-operation-oracle"]))

FAIL in (sentinel-intent-audit-is-required-by-the-merge-gate) (mcp_intent_contract_test.clj:42)
the full sentinel suite must reuse the same audit
expected: (some #{target} (:prerequisites sentinel))
  actual: (not (some #{"performance-regression-sentinel-intent-test"} []))

FAIL in (sentinel-intent-audit-is-required-by-the-merge-gate) (mcp_intent_contract_test.clj:44)
the prerequisite must execute the audit and propagate its exit
expected: (= "bash test/performance_regression_sentinel_intent_test.sh" (some-> (:recipe audit) str/trim))
  actual: (not (= "bash test/performance_regression_sentinel_intent_test.sh" nil))

Ran 21 tests containing 473 assertions.
3 failures, 0 errors.

test-isolation: 0 violations across 1 namespace(s) (TEST-ISO-002/003/004/005/007/010)
```

Deleting every multipart ledger-root witness from a scratch copy exposed the
old parser's false green (the generic audit still tracks this ID as explicit
test-witness debt):

```text
All PERF-SENT-LEDGER-ROOT-001 witnesses removed from scratch.
Sentinel intent witness audit passed: 49/49 requirements named
exit=0; expected nonzero (false green)
```

The new permanent shell self-test against the old parser fails:

```text
FAIL: sentinel audit accepted missing PERF-SENT-LEDGER-ROOT-001
exit=1
```

After the fix, the **real Make merge-gate entrance** fails with the marker
removed. Only the unrelated Prolog prerequisite was marked already handled
for this scratch drive; the failed prerequisite prevents any JVM recipe:

```text
make -C /var/tmp/forge/lid-batch/b3-sentinel-fx -o mcp-operation-oracle mcp-test
make: Entering directory '/var/tmp/forge/lid-batch/b3-sentinel-fx'
bash test/performance_regression_sentinel_intent_test.sh
Missing sentinel witnesses:
  PERF-SENT-LEDGER-ROOT-001
make: *** [Makefile:823: performance-regression-sentinel-intent-test] Error 1
make: Leaving directory '/var/tmp/forge/lid-batch/b3-sentinel-fx'
exit=2
```

Restored real-tree gate and permanent missing-simple, missing-multipart,
unknown-multipart self-test (exit 0):

```text
bash test/performance_regression_sentinel_intent_test.sh
Sentinel intent witness audit passed: 50/50 requirements named
bash test/performance_regression_sentinel_intent_self_test.sh
Sentinel intent audit self-test passed: missing simple/multipart and unknown multipart IDs refused
```

The full `make mcp-test` output contains these actual lane lines:

```text
Sentinel intent witness audit passed: 50/50 requirements named
Sentinel intent audit self-test passed: missing simple/multipart and unknown multipart IDs refused
```

### WTL and OP-ALG audit

Both are already covered by the widened generic contract and the exact per-ID
ledger, not by a prefix exemption. All 53 WTL rows and all 39 OP-ALG rows are
parsed. Their existing linkage debt is **35** and **37** missing pairs,
respectively, and was not repaired or enlarged in this batch. Removing an
existing test witness or inventing a new witness blocks each prefix:

```text
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge/lid-batch
{:ok true, :rows 691, :blocking [], :pending-pairs 144}
{:prefix "WTL-", :rows 53, :implementation-witnessed 21, :test-witnessed 46, :pending-by-kind {:implementation 30, :test 5}}
{:prefix "OP-ALG-", :rows 39, :implementation-witnessed 16, :test-witnessed 15, :pending-by-kind {:test 19, :implementation 18}}
{:prefix "PERF-SENT-", :rows 50, :implementation-witnessed 0, :test-witnessed 0, :pending-by-kind {:test 50}}
{:removed "WTL-APPLY-001", :ok false, :violations [{:type :missing-test-witness, :intent "WTL-APPLY-001", :source-kind :test}]}
{:invented "WTL-UNKNOWN-999", :ok false, :violations [{:type :unknown-intent-witness, :intent "WTL-UNKNOWN-999", :source-kind :test}]}
{:removed "OP-ALG-CATALOG-001", :ok false, :violations [{:type :missing-test-witness, :intent "OP-ALG-CATALOG-001", :source-kind :test}]}
{:invented "OP-ALG-UNKNOWN-999", :ok false, :violations [{:type :unknown-intent-witness, :intent "OP-ALG-UNKNOWN-999", :source-kind :test}]}
{:r1 993, :adopted 449, :total 1442, :intent-tests 21, :lane-namespaces {:integration 6, :fast 50, :battery 32}, :adopted-pin-mismatches {}}
```

Probe: `clojure -M:clj-surgeon/test-deps /var/tmp/forge/lid-batch/b3-audit-pins.clj`,
exit 0. The removal/invention cases operate on immutable maps of actual
repository input bytes; the production `audit-contract` and
`defer-missing-witnesses` functions return the shown failures. Generic pending
debt remains 144 pairs, including 50 PERF-SENT test pairs because shell/bench
sources are deliberately outside that scanner. The now-mandatory shell audit
supplies the separate sentinel check rather than silently deleting debt.

## (b) HLD section list and reference verification

1. Alias migration: one intent across requiring namespaces — 67-row leaf;
   caller counts, `alias_histogram`, `details_path`, verification status and
   refusal recovery, including alias-policy exhaustion with no invented retry.
2. Feature thread: bounded evidence across a feature's parts — 52-row leaf;
   part status, ranges/digests, grounded verify advice, bounded text/structured
   evidence and captured pre-image clock.
3. Relation census: locate collection-write review work — 35-row leaf;
   classes, uncertainty, totals, truncation/incompleteness, actual pool/phases,
   confined discovery and recovery.
4. Two things called mission — read snapshot/continuation versus durable EDN
   MISSION LEDGER, with source and executor-plan pointers; no ledger ratification.
5. Ordinary execution of the sentinel intent gate.
6. References for previously omitted or thin intent leaves — all 15 audit rows:
   alias-migration, feature-thread, relation-census, helper-extraction,
   embedded-elaborator, substantiation-telemetry, test-isolation,
   prepared-request-actions, temp-dir-hygiene, insertion-boundary-and-gap,
   sibling-pair-edit, write-refusal-completeness, shell-argv-safety,
   read-request-normalization, hot-verification.

Verified the HLD begins byte-for-byte with `git show HEAD:docs/high-level-design.md`,
every appended local link exists, all 44 cited flagship IDs have spec rows,
and all 15 reference bullets are present. Frozen/excluded leaves are labelled
as such; a reference does not upgrade their implementation status.

## Pins recomputed from the tree

Source census: **993 original + 449 adopted = 1,442 deftests**;
intent-contract namespace **21**. All adopted namespace pins match their
source counts. Membership remains **50 fast + 6 integration + 32 battery = 88**.
Only the total pin changes, 1441 → 1442, with the reason recorded beside it.
No value was copied from another branch or inferred by conflict resolution.

## Requested gates

### Intent-contract namespace

`clojure -M:clj-surgeon/test-deps -m clj-surgeon.mcp-test-runner --ns clj-surgeon.mcp-intent-contract-test` — exit **0**.

```text
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge/lid-batch
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge/lid-batch
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-intent-contract-test

Ran 21 tests containing 473 assertions.
0 failures, 0 errors.

test-isolation: 0 violations across 1 namespace(s) (TEST-ISO-002/003/004/005/007/010)
```

### Operation oracle

`make mcp-operation-oracle` — exit **0**.

```text
# @spec MCP-OP-ORACLE-001
swipl -q -f test/mcp_operation_contract_oracle.pl
mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]
```

### Fast lane

`clojure -M:clj-surgeon/test-fast` — exit **0**.

```text

Testing clj-surgeon.scope-stream-test

Testing clj-surgeon.telemetry-events-test

Testing clj-surgeon.workspace-onboarding-test

Ran 583 tests containing 6045 assertions.
0 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.

test-isolation: 0 violations across 50 namespace(s) (TEST-ISO-002/003/004/005/007/010)
```

### Paved kondo

`~/bin/clj-kondo --lint test/clj_surgeon/mcp_intent_contract_test.clj test/clj_surgeon/lane_manifest_test.clj` — exit **0**.

```text
linting took 55ms, errors: 0, warnings: 0
```

### Merge gate

`make mcp-test` — exit **0**. JVM summary and actual Make tail:

```text
Ran 748 tests containing 9533 assertions.
0 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.

test-isolation: 0 violations across 56 namespace(s) (TEST-ISO-002/003/004/005/007/010)
```

```text
--- SELF_TEST_TMP with TMPDIR=/dev/shm/probe -> /var/tmp ---
--- SELF_TEST_TMP with TMPDIR unset -> /var/tmp ---
--- SELF_TEST_TMP with TMPDIR=/var/tmp/forge/lid-batch/clj-surgeon-tmpleak-witness.i1u9uh -> /var/tmp/forge/lid-batch/clj-surgeon-tmpleak-witness.i1u9uh ---
--- SELF_TEST_TMP with TMPDIR=/var/tmp/forge -> /var/tmp/forge ---
tmp-leak ratchet witness passed
Installed analyzer gate /var/tmp/forge/lid-batch/clj-surgeon-kondo-path.Y3wgah/home/bin/clj-kondo-admission and shell entrance /var/tmp/forge/lid-batch/clj-surgeon-kondo-path.Y3wgah/home/bin/clj-kondo
clj-kondo admission path regression passed
cclsp already ready at http://127.0.0.1:7890/mcp
cclsp-start transient-health regression passed
cclsp ready at http://127.0.0.1:7890/mcp with restart-on-save TypeScript
cclsp launch PATH regression passed
direct cclsp client audit self-test: ok
```

The 7890 URLs in the Make tail are emitted by `cclsp_start_test.sh` with
fake `curl`, `launchctl` and Bun commands, not real service contacts. The
temporary-directory ratchet invokes battery-named runner entrypoints solely
to assert refusal before any workload; no battery was executed.

## Doubts and limits

- A traceability pass establishes linked witnesses, not correctness of every
  behavior those witnesses name. No performance claim or sentinel experiment
  was made, and the sentinel behavioral suite/battery was not run.
- The separate shell audit currently owns this 50-row active-gap leaf; the
  generic scanner still excludes shell/bench sources. Its 50 explicit missing
  test pairs remain visible rather than being presented as generically repaired.
- WTL/OP-ALG existing debt and the two excluded pre-product leaves remain.
- The fast alias witnesses Make wiring without spawning shell. Actual execution
  is proved by the retained Make red drive and complete `make mcp-test` green.
- No shared server was started or contacted. The explicitly required merge
  suite owns ephemeral in-process test servers; their startup/cleanup is in
  its log and covered by its isolation witness. No prohibited port was used.
- No push, routing/skill install, or mission-ledger implementation change.
  Tracked `skills/` files are unchanged; no skill sync was necessary.
- Scratch fixtures were removed after receipts were retained; logs and the audit
  probe remain under `/var/tmp/forge/lid-batch/`.

Completed: 2026-09-07T19:17:32.494360+00:00
