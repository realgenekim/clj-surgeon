# Curtain Call LID batch 1 — Astra

Committed as `e36d9a73a2fc1be8e0720d55a7d7970a8132feb8` on `astra/lid-batch-1`, based on `92a7ca14fd904e4d962df38614b9af3a7ef41a11`.
The contract now enforces all **93 IDs in six spec files**, with **46 explicitly allowlisted linkage debts**.
Both requested full Clojure runs passed: **1,024 tests, 12,310 assertions, zero failures**.

## Change

- Harvest both checked and unchecked checklist rows from `docs/intent/*/*-specs.md`, without an opt-in flag.
- Harvest every comma-separated `@spec` ID from Clojure, JavaScript, and shell comment markers in the existing source scopes. `INTENT:` code and `INTENT-TEST:` test markers also satisfy spec linkage.
- Require code and test witnesses, reject unknown markers, duplicate spec declarations, an empty spec census, unknown allowance entries, and allowance entries whose two witnesses now exist. A new spec without witnesses and without an allowance entry fails.
- Keep all 51 existing registry rows byte-for-byte intact through the last row. Append `LID-SPEC-001`, with one EARS outcome, misreadings, and three immediately tagged test witnesses. Existing active/status/marker-dialect/operational-entry-point/behavioral checks remain in force; known marker targets expand to include spec declarations.
- Add a short HLD engineering-practice paragraph and the owning `docs/intent/spec-traceability.md` design contract. The real writer regression plus a 16-case linkage/checkbox/allowance state space and parser/reverse-link cases witness the change.
- Repair the full-entry shell fixture to initialize its bare remote explicitly on `main`.

## Diff stat

```text
 bin/test-new-mission-worktree                      |   2 +-
 docs/high-level-design.md                          |   9 ++
 docs/intent/registry.edn                           |  20 ++-
 docs/intent/spec-traceability.md                   |  44 ++++++
 docs/intent/unlinked-spec-ids.edn                  |  48 +++++++
 src/cfp_scheduler_killer/intent_traceability.clj   |  44 ++++++
 test/cfp_scheduler_killer/intent_contract_test.clj | 153 +++++++++++++++------
 7 files changed, 278 insertions(+), 42 deletions(-)
```

## Red / green

Before widening, `make runtests-focus FOCUS=cfp-scheduler-killer.intent-contract-test/unlinked-spec-file-id-is-reported-test` failed against the existing gate (exit 2 from make; nested test process failure). The only contract change at that point was the new witness. Exact red tail, ANSI stripped:

```text
FAIL in cfp-scheduler-killer.intent-contract-test/unlinked-spec-file-id-is-reported-test (intent_contract_test.clj:146)
AGENDA-WRITER-001 must be reported as a spec with no test witness
expected: (boolean (some (fn* [p1__78255#] (and (= :fail (:type p1__78255#)) (str/includes? (:message p1__78255#) "AGENDA-WRITER-001") (str/includes? (:message p1__78255#) "test witness"))) (clojure.core/deref reports)))
  actual: (not (boolean nil))
1 tests, 1 assertions, 1 failures.
make: *** [Makefile:138: runtests-focus] Error 1
```

The permanent witness uses the real spec declaration and real gate with an empty allowance. It hides the writer's test marker if a later batch adds it, so retiring the actual writer debt will not break this regression test. The string naming the writer is not a marker claiming behavioral coverage.

After widening, running only `every-enforced-ears-spec-is-traceable` with an empty allowance reported **59 missing links across 46 IDs**: **21 missing code**, **38 missing test**, **13 missing both**. Raw failure: `widened-unallowlisted.log` beside this report. Complete findings:

```text
AGENDA-BLOCK-001: missing test
AGENDA-BLOCK-002: missing test
AGENDA-CAPACITY-001: missing test
AGENDA-DRAFT-001: missing test
AGENDA-DRAFT-002: missing test
AGENDA-EVENT-001: missing test
AGENDA-IDENTITY-001: missing test
AGENDA-LEGACY-001: missing code
AGENDA-LEGACY-001: missing test
AGENDA-LEGACY-002: missing code
AGENDA-LEGACY-002: missing test
AGENDA-PRESENTER-002: missing test
AGENDA-PRIMARY-002: missing code
AGENDA-PRIMARY-003: missing test
AGENDA-PUBLIC-001: missing test
AGENDA-PUBLIC-002: missing test
AGENDA-SUBMISSION-001: missing code
AGENDA-SUBMISSION-001: missing test
AGENDA-SUBMISSION-002: missing code
AGENDA-SUBMISSION-002: missing test
AGENDA-WRITER-001: missing code
AGENDA-WRITER-001: missing test
AGENDA-WRITER-002: missing code
AGENDA-WRITER-002: missing test
AGENDA-WRITER-003: missing code
AGENDA-WRITER-003: missing test
CORR-APPLY-001: missing test
CORR-APPLY-002: missing test
CORR-BP-001: missing test
CORR-BP-002: missing test
CORR-BP-003: missing test
CORR-CHARLOTTE-001: missing test
CORR-CHARLOTTE-002: missing test
CORR-CHARLOTTE-003: missing test
CORR-IDENT-002: missing code
CORR-IDENT-002: missing test
CORR-IDENT-003: missing code
CORR-IDENT-003: missing test
CORR-IDENT-004: missing code
CORR-IDENT-004: missing test
CORR-PLAN-001: missing test
CORR-PLAN-002: missing test
CORR-PLAN-003: missing test
CORR-RECEIPT-001: missing test
CORR-VIEW-001: missing test
CORR-VIEW-002: missing test
ROUGH-PLAN-003: missing code
ROUGH-PLAN-003: missing test
ROUGH-PLAN-004: missing code
ROUGH-PLAN-004: missing test
SPK-EDIT-001: missing code
SPK-EDIT-002: missing code
SPK-EDIT-003: missing code
SPK-EDIT-004: missing code
SPK-EDIT-005: missing code
SPK-EDIT-006: missing code
SPK-EDIT-008: missing code
SPK-MULTI-001: missing code
SPK-MULTI-001: missing test
```

After loading the explicit allowance, the focused contract passed (exit 0):

```text
Guardrails was enabled because the guardrails.enabled property is set to a (any) value.
WARNING: reset! already refers to: #'clojure.core/reset! in namespace: cfp-scheduler-killer.replay, being replaced by: #'cfp-scheduler-killer.replay/reset!
9 tests, 158 assertions, 0 failures.
```

## Allowlist contents and counts

| Owning topic/spec file | Declared IDs | Allowlisted IDs |
| --- | ---: | ---: |
| canonical-agenda/canonical-agenda | 26 | 19 |
| append-only-correction/append-only-correction | 34 | 18 |
| canonical-agenda/rough-blocking-command | 5 | 2 |
| canonical-agenda/rough-blocking-drag | 14 | 0 |
| communications/canonical-letters | 3 | 0 |
| speaker-profile-edit/speaker-profile-edit | 11 | 7 |
| **Total** | **93** | **46** |

By ID prefix: AGENDA 19, CORR 17, ROUGH 2, SPK-EDIT 7, SPK-MULTI 1, LETTER 0. `SPK-MULTI-001` is owned by the append-only-correction spec file.

The single allowance file is `docs/intent/unlinked-spec-ids.edn`:

```edn
;; Each entry is existing linkage debt to be retired by later LID batches.
;; Do not add new debt. Remove an entry as soon as both witnesses exist.
#{:AGENDA-BLOCK-001
  :AGENDA-BLOCK-002
  :AGENDA-CAPACITY-001
  :AGENDA-DRAFT-001
  :AGENDA-DRAFT-002
  :AGENDA-EVENT-001
  :AGENDA-IDENTITY-001
  :AGENDA-LEGACY-001
  :AGENDA-LEGACY-002
  :AGENDA-PRESENTER-002
  :AGENDA-PRIMARY-002
  :AGENDA-PRIMARY-003
  :AGENDA-PUBLIC-001
  :AGENDA-PUBLIC-002
  :AGENDA-SUBMISSION-001
  :AGENDA-SUBMISSION-002
  :AGENDA-WRITER-001
  :AGENDA-WRITER-002
  :AGENDA-WRITER-003
  :CORR-APPLY-001
  :CORR-APPLY-002
  :CORR-BP-001
  :CORR-BP-002
  :CORR-BP-003
  :CORR-CHARLOTTE-001
  :CORR-CHARLOTTE-002
  :CORR-CHARLOTTE-003
  :CORR-IDENT-002
  :CORR-IDENT-003
  :CORR-IDENT-004
  :CORR-PLAN-001
  :CORR-PLAN-002
  :CORR-PLAN-003
  :CORR-RECEIPT-001
  :CORR-VIEW-001
  :CORR-VIEW-002
  :ROUGH-PLAN-003
  :ROUGH-PLAN-004
  :SPK-EDIT-001
  :SPK-EDIT-002
  :SPK-EDIT-003
  :SPK-EDIT-004
  :SPK-EDIT-005
  :SPK-EDIT-006
  :SPK-EDIT-008
  :SPK-MULTI-001}
```

## Full verification and Prolog evidence

All commands ran from `/home/forge/src/cc-lid2`, with `TMPDIR=/var/tmp/forge/lid-batch`, `JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=/var/tmp/forge/lid-batch`, and `~/bin` first on PATH. Full logs are beside this report. Tails below strip ANSI and the single very long progress-dot line only.

1. `make runtests-once` — **exit 0**, after the fixture repair. `make-runtests-once.log`:

```text
# skipped 0
# todo 0
# duration_ms 72.813947
PASS: fetched origin/main beats stale local main and refusals hold
Running fast unit tests (fail-fast)...
bin/kaocha unit --fail-fast
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge/lid-batch
GUARDRAILS IS ENABLED. RUNTIME PERFORMANCE WILL BE AFFECTED.
Mode: :runtime  Async? false  Throw? false
Guardrails was enabled because the guardrails.enabled property is set to a (any) value.
WARNING: reset! already refers to: #'clojure.core/reset! in namespace: cfp-scheduler-killer.replay, being replaced by: #'cfp-scheduler-killer.replay/reset!
1024 tests, 12310 assertions, 0 failures.
```

2. `bin/kaocha unit` — **exit 0**. `kaocha-unit.log`:

```text
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge/lid-batch
GUARDRAILS IS ENABLED. RUNTIME PERFORMANCE WILL BE AFFECTED.
Mode: :runtime  Async? false  Throw? false
Guardrails was enabled because the guardrails.enabled property is set to a (any) value.
WARNING: reset! already refers to: #'clojure.core/reset! in namespace: cfp-scheduler-killer.replay, being replaced by: #'cfp-scheduler-killer.replay/reset!
1024 tests, 12310 assertions, 0 failures.
```

3. `~/bin/clj-kondo --lint src/cfp_scheduler_killer/intent_traceability.clj test/cfp_scheduler_killer/intent_contract_test.clj docs/intent/registry.edn docs/intent/unlinked-spec-ids.edn` — **exit 0**. `clj-kondo.log`:

```text
linting took 38ms, errors: 0, warnings: 0
```

The changed shell fixture also passed `bash -n bin/test-new-mission-worktree`; `git diff --check` passed.

**Prolog did actually run.** `Makefile:119` checks for `swipl`, fails if missing, then executes `swipl -g run_tests -t halt "$f"` for every `test/oracles/*.pl`; `runtests-once` depends on that target. Installed engine: `SWI-Prolog version 10.0.0 for x86_64-linux`. The full-entry log contains five real plunit runs, **34 oracle tests total**:

```text
oracle: test/oracles/admin_event_slack_contract.pl
% End unit admin_event_slack_contract: passed (0.006 sec CPU)
% All 9 tests passed in 0.009 seconds (0.009 cpu)
oracle: test/oracles/review_board_contract.pl
% End unit review_board_contract: passed (0.006 sec CPU)
% All 6 tests passed in 0.009 seconds (0.009 cpu)
oracle: test/oracles/rough_drag_projection.pl
% End unit rough_drag_projection: passed (0.006 sec CPU)
% All 5 tests passed in 0.009 seconds (0.009 cpu)
oracle: test/oracles/speaker_profile_edit_contract.pl
% End unit speaker_profile_edit_contract: passed (0.006 sec CPU)
% All 8 tests passed in 0.009 seconds (0.009 cpu)
oracle: test/oracles/talk_agenda_contract.pl
% End unit talk_agenda_contract: passed (0.006 sec CPU)
% All 6 tests passed in 0.009 seconds (0.009 cpu)
```

`bin/kaocha unit` alone is the Clojure runner; it does not invoke this Makefile Prolog prerequisite. The full `make runtests-once` entry ran the oracles, 18 JavaScript tests, the worktree fixture, then Clojure. No Makefile change was needed to wire Prolog.

## What broke and what was fixed

- The first draft red witness captured full Clojure test-report objects; Kaocha exhausted its 512 MB reporter heap while printing the expected failure. Capture now retains only `:type` and `:message`, and the assertion reports a boolean. The clean red receipt above followed this repair.
- The initial full entry passed every Prolog and JavaScript test, then failed with `remote HEAD refers to nonexistent ref` / `ambiguous argument main`. The bare fixture remote inherited Git's default branch while the fixture author explicitly used main. Adding `--initial-branch=main` to that fixture's bare `git init` fixed it. The whole full entry was rerun successfully. Initial failure tail:

```text
# todo 0
# duration_ms 63.374299
warning: remote HEAD refers to nonexistent ref, unable to checkout
fatal: ambiguous argument 'main': unknown revision or path not in the working tree.
Use '--' to separate paths from revisions, like this:
'git <command> [<revision>...] -- [<file>...]'
make: *** [Makefile:190: new-mission-worktree-test] Error 128
```

## Doubts and limits

- The ledger's absolute structural claim is stale: this base already had partial opt-in enforcement for checked rough-command/rough-drag specs, plus reverse checks. It still missed AGENDA-WRITER-001 and captured only the first ID on each comma-separated marker line. This batch removes that blind spot.
- The audit's 22 missing-code count becomes 21 because existing registry-style markers satisfy the overlapping `CORR-IDENT-001` spec. The 38 missing-test and 13 missing-both counts remain. No product markers were added to pretend these debts were fixed.
- Traceability establishes named links, not correctness of product behavior. The 46 entries still need later behavioral/linkage work.
- The gate enforces stale/unknown allowance entries and unallowlisted gaps against the current tree. Preventing a deliberate edit that adds fresh debt to the allowance is a review constraint; a current-tree test cannot establish historical shrinkage without an independent immutable baseline.
- The repository-mentioned `linked-intent-dev` skill was absent from available skill locations. Applied the explicitly supplied `/home/forge/opt/claude-skills/linked-intent-testing/SKILL.md` and used the authorized batch's HLD → design → EARS → edge audit → tests → implementation flow.
- `bd prime` ran. Task creation refused because the checkout's Beads database is uninitialized (`issue_prefix config is missing`). No tracker was initialized or bootstrapped and no bead was available to close.
- No fleet/runtime work or prohibited port access was performed. Scratch and test temp paths were redirected under `/var/tmp/forge/lid-batch/`. No project remote push was performed; the required worktree fixture uses local temporary Git repositories.
- The pre-existing deletion of `.codex/config.toml` and untracked `.codex/config.toml.disabled-by-sol-yolo` remain untouched and unstaged.

## Commit receipt

```text
commit e36d9a73a2fc1be8e0720d55a7d7970a8132feb8
Author:     forge-anvil <forge-anvil@anvil>
AuthorDate: Mon Sep 7 17:28:50 2026 +0000
Commit:     forge-anvil <forge-anvil@anvil>
CommitDate: Mon Sep 7 17:28:50 2026 +0000

    Enforce spec-file intent traceability with explicit linkage debt
    
    Check every checked and unchecked spec ID in both directions, preserve
    registry rules, and reject completed or unknown allowance entries. Pin the
    AGENDA-WRITER-001 blind spot with a red/green witness and register LID-SPEC-001.
    
    Make the local worktree fixture independent of Git default-branch settings.
    Validated full make entry including Prolog, direct Kaocha unit suite, and lint.
    
    Co-Authored-By: Gene Kim <genek@itrevolution.com>
    Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>
```

Final status (only the original config differences remain):

```text
## astra/lid-batch-1...origin/main [ahead 1]
 D .codex/config.toml
?? .codex/config.toml.disabled-by-sol-yolo
```

Finished 2026-09-07T17:29:06.895372+00:00. Started approximately 17:15 UTC; within the 40-minute budget.
