# Intent link inventory

Baseline 08bf1d23859db426691e4018aa633edad91d6708. Read-only textual audit; a reference is not a passing behavioral witness. Definitions are IDs in headings, checkbox requirement definitions, @spec or INTENT: lines; duplicate IDs within each folder count once, including archived variants and deferred requirements. Test links include any whole bare ID in tracked test/** files (including fixtures); bench/** is outside this requested test scope. All IDs are listed individually below.

| Folder | Specs | Linked | Unlinked IDs |
| --- | --- | --- | --- |
| 2026-08-29-ratification | 4 | 0 | MEASURE-EVID-001, MEASURE-WALL-001, MEASURE-WALL-002, MEASURE-WALL-003 |
| 2026-08-30-prepared-request-ratification | 9 | 9 | — |
| 2026-08-30-splice-reference-ratification | 0 | 0 | — |
| embedded-elaborator | 19 | 0 | MCP-OP-ELAB-001, MCP-OP-ELAB-002, MCP-OP-ELAB-003, MCP-OP-ELAB-004, MCP-OP-ELAB-005, MCP-OP-ELAB-006, MCP-OP-ELAB-007, MCP-OP-ELAB-008, MCP-OP-ELAB-009, MCP-OP-ELAB-010, MCP-OP-ELAB-011, MCP-OP-ELAB-012, MCP-OP-ELAB-013, MCP-OP-ELAB-014, MCP-OP-ELAB-015, MCP-OP-ELAB-016, MCP-OP-ELAB-017, MCP-OP-ELAB-018, MCP-OP-ELAB-019 |
| insertion-boundary-and-gap | 10 | 10 | — |
| mcp-operation-contract | 134 | 127 | MCP-OP-READ-AUTH-001, MCP-OP-READ-MISSION-001, MCP-OP-READ-MISSION-002, MCP-OP-READ-RESOLVE-001, MCP-OP-READ-RESOLVE-002, MCP-OP-READ-RETRY-001, MCP-OP-READ-RETRY-002 |
| operation-algebra | 38 | 30 | OP-ALG-CLI-002, OP-ALG-EXPAND-001, OP-ALG-PARITY-002, OP-ALG-PERF-001, OP-ALG-PERF-002, OP-ALG-VERIFY-001, OP-ALG-VERIFY-002, OP-ALG-VERIFY-004 |
| performance-regression-sentinel | 50 | 15 | PERF-SENT-ATTEMPT-001, PERF-SENT-ATTEMPT-002, PERF-SENT-ATTEMPT-003, PERF-SENT-AUTH-001, PERF-SENT-AUTH-002, PERF-SENT-AUTH-003, PERF-SENT-BACKFILL-001, PERF-SENT-BASELINE-001, PERF-SENT-CLIENT-001, PERF-SENT-CUTOVER-001, PERF-SENT-CUTOVER-002, PERF-SENT-CUTOVER-003, PERF-SENT-IMPORT-001, PERF-SENT-IMPORT-002, PERF-SENT-LEDGER-001, PERF-SENT-LEDGER-002, PERF-SENT-LEDGER-003, PERF-SENT-LEDGER-004, PERF-SENT-LEDGER-ROOT-001, PERF-SENT-PROJECT-001, PERF-SENT-PROMOTION-002, PERF-SENT-PUBLISH-001, PERF-SENT-PUBLISH-002, PERF-SENT-RECEIPT-002, PERF-SENT-RECONCILE-001, PERF-SENT-RECOVERY-001, PERF-SENT-RECOVERY-002, PERF-SENT-RECOVERY-003, PERF-SENT-RECOVERY-004, PERF-SENT-RELEASE-001, PERF-SENT-SCHEDULE-003, PERF-SENT-TIME-001, PERF-SENT-VERDICT-001, PERF-SENT-VERDICT-002, PERF-SENT-VERDICT-003 |
| positional-mutation-authority | 0 | 0 | — |
| prepared-request | 9 | 9 | — |
| prepared-request-actions | 18 | 18 | — |
| read-request-normalization | 5 | 5 | — |
| shell-argv-safety | 3 | 3 | — |
| sibling-pair-edit | 10 | 0 | MCP-OP-PAIR-EDIT-001, MCP-OP-PAIR-EDIT-002, MCP-OP-PAIR-EDIT-003, MCP-OP-PAIR-EDIT-004, MCP-OP-PAIR-EDIT-005, MCP-OP-PAIR-EDIT-006, MCP-OP-PAIR-EDIT-007, MCP-OP-PAIR-EDIT-008, MCP-OP-PAIR-EDIT-009, MCP-OP-PAIR-EDIT-010 |
| substantiation-telemetry | 19 | 0 | MCP-OP-SUBST-001, MCP-OP-SUBST-002, MCP-OP-SUBST-003, MCP-OP-SUBST-004, MCP-OP-SUBST-005, MCP-OP-SUBST-006, MCP-OP-SUBST-007, MCP-OP-SUBST-008, MCP-OP-SUBST-009, MCP-OP-SUBST-010, MCP-OP-SUBST-011, MCP-OP-SUBST-012, MCP-OP-SUBST-013, MCP-OP-SUBST-014, MCP-OP-SUBST-015, MCP-OP-SUBST-016, MCP-OP-SUBST-017, MCP-OP-SUBST-018, MCP-OP-SUBST-019 |
| worktree-lifecycle | 53 | 51 | WTL-CLI-004, WTL-SEAL-008 |
| write-refusal-completeness | 8 | 1 | MCP-OP-WRITE-REFUSAL-002, MCP-OP-WRITE-REFUSAL-003, MCP-OP-WRITE-REFUSAL-004, MCP-OP-WRITE-REFUSAL-005, MCP-OP-WRITE-REFUSAL-006, MCP-OP-WRITE-REFUSAL-007, MCP-OP-WRITE-REFUSAL-008 |

AGENTS.md explicitly scopes the intent folders mcp-operation-contract and operation-algebra; the other 15 intent folders are outside that declared LID scope. Its source/test file globs and docs/high-level-design.md/Makefile are also scoped.

Coherence checks exist: make runtests → mcp-test includes test/clj_surgeon/mcp_intent_contract_test.clj, exercising src/clj_surgeon/mcp_intent_contract.clj for unknown IDs and missing implementation/test witnesses. make performance-regression-sentinel-test runs test/performance_regression_sentinel_intent_test.sh, checking missing/unknown PERF-SENT IDs (also using bench tests). These are scoped checks, not a repository-wide check for all 17 folders. Search: rg -n -i "coherence|traceability|intent|LID" Makefile test resources bench; inspected the named gates, not executed them.

## 2026-08-29-ratification

| Spec ID | Referenced in test/** |
| --- | --- |
| MEASURE-EVID-001 | no |
| MEASURE-WALL-001 | no |
| MEASURE-WALL-002 | no |
| MEASURE-WALL-003 | no |

## 2026-08-30-prepared-request-ratification

| Spec ID | Referenced in test/** |
| --- | --- |
| MCP-OP-PREP-REQ-001 | yes |
| MCP-OP-PREP-REQ-002 | yes |
| MCP-OP-PREP-REQ-003 | yes |
| MCP-OP-PREP-REQ-004 | yes |
| MCP-OP-PREP-REQ-005 | yes |
| MCP-OP-PREP-REQ-006 | yes |
| MCP-OP-PREP-REQ-007 | yes |
| MCP-OP-PREP-REQ-008 | yes |
| MCP-OP-PREP-REQ-009 | yes |

## 2026-08-30-splice-reference-ratification

No spec IDs defined by the scanned markers.

## embedded-elaborator

| Spec ID | Referenced in test/** |
| --- | --- |
| MCP-OP-ELAB-001 | no |
| MCP-OP-ELAB-002 | no |
| MCP-OP-ELAB-003 | no |
| MCP-OP-ELAB-004 | no |
| MCP-OP-ELAB-005 | no |
| MCP-OP-ELAB-006 | no |
| MCP-OP-ELAB-007 | no |
| MCP-OP-ELAB-008 | no |
| MCP-OP-ELAB-009 | no |
| MCP-OP-ELAB-010 | no |
| MCP-OP-ELAB-011 | no |
| MCP-OP-ELAB-012 | no |
| MCP-OP-ELAB-013 | no |
| MCP-OP-ELAB-014 | no |
| MCP-OP-ELAB-015 | no |
| MCP-OP-ELAB-016 | no |
| MCP-OP-ELAB-017 | no |
| MCP-OP-ELAB-018 | no |
| MCP-OP-ELAB-019 | no |

## insertion-boundary-and-gap

| Spec ID | Referenced in test/** |
| --- | --- |
| MCP-OP-INSERT-001 | yes |
| MCP-OP-INSERT-002 | yes |
| MCP-OP-INSERT-003 | yes |
| MCP-OP-INSERT-004 | yes |
| MCP-OP-INSERT-005 | yes |
| MCP-OP-INSERT-006 | yes |
| MCP-OP-INSERT-007 | yes |
| MCP-OP-INSERT-008 | yes |
| MCP-OP-INSERT-009 | yes |
| MCP-OP-INSERT-010 | yes |

## mcp-operation-contract

| Spec ID | Referenced in test/** |
| --- | --- |
| MCP-OP-ANALYZER-001 | yes |
| MCP-OP-ANALYZER-002 | yes |
| MCP-OP-ANALYZER-003 | yes |
| MCP-OP-ANALYZER-004 | yes |
| MCP-OP-ANALYZER-005 | yes |
| MCP-OP-ANALYZER-006 | yes |
| MCP-OP-ANALYZER-007 | yes |
| MCP-OP-ANALYZER-008 | yes |
| MCP-OP-ANALYZER-009 | yes |
| MCP-OP-ASYNC-001 | yes |
| MCP-OP-ASYNC-002 | yes |
| MCP-OP-ASYNC-003 | yes |
| MCP-OP-ASYNC-004 | yes |
| MCP-OP-ASYNC-005 | yes |
| MCP-OP-COVERAGE-001 | yes |
| MCP-OP-COVERAGE-002 | yes |
| MCP-OP-DISPATCH-001 | yes |
| MCP-OP-DISPATCH-002 | yes |
| MCP-OP-DISPATCH-003 | yes |
| MCP-OP-DISPATCH-004 | yes |
| MCP-OP-DISPATCH-005 | yes |
| MCP-OP-EDIT-001 | yes |
| MCP-OP-EDIT-002 | yes |
| MCP-OP-EDIT-003 | yes |
| MCP-OP-EDIT-004 | yes |
| MCP-OP-EDIT-005 | yes |
| MCP-OP-EDIT-006 | yes |
| MCP-OP-EDIT-007 | yes |
| MCP-OP-EDIT-008 | yes |
| MCP-OP-EDIT-009 | yes |
| MCP-OP-EDIT-010 | yes |
| MCP-OP-EDIT-011 | yes |
| MCP-OP-EDIT-012 | yes |
| MCP-OP-EDIT-013 | yes |
| MCP-OP-EDIT-014 | yes |
| MCP-OP-EDIT-015 | yes |
| MCP-OP-EDIT-016 | yes |
| MCP-OP-EDIT-017 | yes |
| MCP-OP-EDIT-018 | yes |
| MCP-OP-EDIT-019 | yes |
| MCP-OP-EDIT-020 | yes |
| MCP-OP-EDIT-021 | yes |
| MCP-OP-EDIT-022 | yes |
| MCP-OP-EDIT-023 | yes |
| MCP-OP-EDIT-024 | yes |
| MCP-OP-EDIT-025 | yes |
| MCP-OP-EDIT-026 | yes |
| MCP-OP-EDIT-027 | yes |
| MCP-OP-EDIT-028 | yes |
| MCP-OP-EDIT-029 | yes |
| MCP-OP-EDIT-030 | yes |
| MCP-OP-EDIT-031 | yes |
| MCP-OP-EDIT-032 | yes |
| MCP-OP-EDIT-033 | yes |
| MCP-OP-EDIT-034 | yes |
| MCP-OP-EDIT-035 | yes |
| MCP-OP-EDIT-036 | yes |
| MCP-OP-FIELD-001 | yes |
| MCP-OP-FIELD-002 | yes |
| MCP-OP-FIELD-003 | yes |
| MCP-OP-FIELD-005 | yes |
| MCP-OP-FIELD-006 | yes |
| MCP-OP-MATCHED-001 | yes |
| MCP-OP-MATCHED-002 | yes |
| MCP-OP-MATCHED-003 | yes |
| MCP-OP-MATCHED-004 | yes |
| MCP-OP-MATCHED-005 | yes |
| MCP-OP-ORACLE-001 | yes |
| MCP-OP-PLAN-001 | yes |
| MCP-OP-PLAN-002 | yes |
| MCP-OP-PLAN-003 | yes |
| MCP-OP-PLAN-004 | yes |
| MCP-OP-PLAN-005 | yes |
| MCP-OP-PLAN-006 | yes |
| MCP-OP-PLAN-007 | yes |
| MCP-OP-PLAN-008 | yes |
| MCP-OP-PLAN-009 | yes |
| MCP-OP-PLAN-010 | yes |
| MCP-OP-POS-AUTH-001 | yes |
| MCP-OP-POS-AUTH-002 | yes |
| MCP-OP-POS-AUTH-003 | yes |
| MCP-OP-POS-AUTH-004 | yes |
| MCP-OP-POS-AUTH-005 | yes |
| MCP-OP-POS-AUTH-006 | yes |
| MCP-OP-POS-AUTH-007 | yes |
| MCP-OP-POS-AUTH-008 | yes |
| MCP-OP-POS-AUTH-009 | yes |
| MCP-OP-POS-AUTH-010 | yes |
| MCP-OP-READ-AUTH-001 | no |
| MCP-OP-READ-CONT-001 | yes |
| MCP-OP-READ-CONT-002 | yes |
| MCP-OP-READ-DIAG-001 | yes |
| MCP-OP-READ-DIAG-002 | yes |
| MCP-OP-READ-DIAG-003 | yes |
| MCP-OP-READ-GUARD-001 | yes |
| MCP-OP-READ-HYP-001 | yes |
| MCP-OP-READ-HYP-002 | yes |
| MCP-OP-READ-MISSION-001 | no |
| MCP-OP-READ-MISSION-002 | no |
| MCP-OP-READ-PARITY-001 | yes |
| MCP-OP-READ-RESOLVE-001 | no |
| MCP-OP-READ-RESOLVE-002 | no |
| MCP-OP-READ-RETRY-001 | no |
| MCP-OP-READ-RETRY-002 | no |
| MCP-OP-RELAY-001 | yes |
| MCP-OP-RELAY-002 | yes |
| MCP-OP-RELAY-003 | yes |
| MCP-OP-RELAY-004 | yes |
| MCP-OP-RELAY-005 | yes |
| MCP-OP-RESULT-001 | yes |
| MCP-OP-RESULT-002 | yes |
| MCP-OP-RESULT-003 | yes |
| MCP-OP-RESULT-004 | yes |
| MCP-OP-RESULT-005 | yes |
| MCP-OP-RESULT-006 | yes |
| MCP-OP-SCHEMA-001 | yes |
| MCP-OP-TIME-001 | yes |
| MCP-OP-TIME-002 | yes |
| MCP-OP-TIME-003 | yes |
| MCP-OP-TIME-004 | yes |
| MCP-OP-TRACE-001 | yes |
| MCP-OP-TRACE-002 | yes |
| MCP-OP-TRACE-003 | yes |
| MCP-OP-TRACE-004 | yes |
| MCP-OP-VERIFY-001 | yes |
| MCP-OP-VERIFY-002 | yes |
| MCP-OP-VERIFY-003 | yes |
| MCP-OP-VERIFY-004 | yes |
| MCP-OP-VERIFY-005 | yes |
| MCP-OP-VERIFY-006 | yes |
| MCP-OP-VERIFY-007 | yes |
| MCP-OP-VERIFY-008 | yes |
| MCP-OP-VERIFY-009 | yes |
| MCP-OP-VERIFY-010 | yes |

## operation-algebra

| Spec ID | Referenced in test/** |
| --- | --- |
| OP-ALG-CATALOG-001 | yes |
| OP-ALG-CLI-001 | yes |
| OP-ALG-CLI-002 | no |
| OP-ALG-COMMIT-001 | yes |
| OP-ALG-COMMIT-002 | yes |
| OP-ALG-COMMIT-003 | yes |
| OP-ALG-COMMIT-004 | yes |
| OP-ALG-COMPILE-001 | yes |
| OP-ALG-CONTEXT-001 | yes |
| OP-ALG-CONTEXT-002 | yes |
| OP-ALG-DECODE-001 | yes |
| OP-ALG-EFFECT-001 | yes |
| OP-ALG-EFFECT-002 | yes |
| OP-ALG-EFFECT-003 | yes |
| OP-ALG-EFFECT-004 | yes |
| OP-ALG-EFFECT-005 | yes |
| OP-ALG-EXPAND-001 | no |
| OP-ALG-IDENTITY-001 | yes |
| OP-ALG-MCP-001 | yes |
| OP-ALG-OUTCOME-001 | yes |
| OP-ALG-OUTCOME-002 | yes |
| OP-ALG-OUTCOME-003 | yes |
| OP-ALG-PARITY-001 | yes |
| OP-ALG-PARITY-002 | no |
| OP-ALG-PERF-001 | no |
| OP-ALG-PERF-002 | no |
| OP-ALG-PREVIEW-001 | yes |
| OP-ALG-PREVIEW-002 | yes |
| OP-ALG-RECEIPT-001 | yes |
| OP-ALG-RECEIPT-002 | yes |
| OP-ALG-RECEIPT-003 | yes |
| OP-ALG-REFUSE-001 | yes |
| OP-ALG-RUNTIME-001 | yes |
| OP-ALG-SHADOW-001 | yes |
| OP-ALG-STALE-001 | yes |
| OP-ALG-VERIFY-001 | no |
| OP-ALG-VERIFY-002 | no |
| OP-ALG-VERIFY-004 | no |

## performance-regression-sentinel

| Spec ID | Referenced in test/** |
| --- | --- |
| PERF-SENT-ADMIT-001 | yes |
| PERF-SENT-ADMIT-002 | yes |
| PERF-SENT-ATTEMPT-001 | no |
| PERF-SENT-ATTEMPT-002 | no |
| PERF-SENT-ATTEMPT-003 | no |
| PERF-SENT-AUTH-001 | no |
| PERF-SENT-AUTH-002 | no |
| PERF-SENT-AUTH-003 | no |
| PERF-SENT-BACKFILL-001 | no |
| PERF-SENT-BASELINE-001 | no |
| PERF-SENT-CLIENT-001 | no |
| PERF-SENT-CONFIG-001 | yes |
| PERF-SENT-CUTOVER-001 | no |
| PERF-SENT-CUTOVER-002 | no |
| PERF-SENT-CUTOVER-003 | no |
| PERF-SENT-IDENT-001 | yes |
| PERF-SENT-IDENT-002 | yes |
| PERF-SENT-IMPORT-001 | no |
| PERF-SENT-IMPORT-002 | no |
| PERF-SENT-INVALID-001 | yes |
| PERF-SENT-INVALID-002 | yes |
| PERF-SENT-LEDGER-001 | no |
| PERF-SENT-LEDGER-002 | no |
| PERF-SENT-LEDGER-003 | no |
| PERF-SENT-LEDGER-004 | no |
| PERF-SENT-LEDGER-ROOT-001 | no |
| PERF-SENT-PROJECT-001 | no |
| PERF-SENT-PROMOTION-001 | yes |
| PERF-SENT-PROMOTION-002 | no |
| PERF-SENT-PUBLISH-001 | no |
| PERF-SENT-PUBLISH-002 | no |
| PERF-SENT-RECEIPT-001 | yes |
| PERF-SENT-RECEIPT-002 | no |
| PERF-SENT-RECONCILE-001 | no |
| PERF-SENT-RECOVERY-001 | no |
| PERF-SENT-RECOVERY-002 | no |
| PERF-SENT-RECOVERY-003 | no |
| PERF-SENT-RECOVERY-004 | no |
| PERF-SENT-RELEASE-001 | no |
| PERF-SENT-RETAIN-001 | yes |
| PERF-SENT-SCHEDULE-001 | yes |
| PERF-SENT-SCHEDULE-002 | yes |
| PERF-SENT-SCHEDULE-003 | no |
| PERF-SENT-SCHEDULE-004 | yes |
| PERF-SENT-SURFACE-001 | yes |
| PERF-SENT-THRESHOLD-001 | yes |
| PERF-SENT-TIME-001 | no |
| PERF-SENT-VERDICT-001 | no |
| PERF-SENT-VERDICT-002 | no |
| PERF-SENT-VERDICT-003 | no |

## positional-mutation-authority

No spec IDs defined by the scanned markers.

## prepared-request

| Spec ID | Referenced in test/** |
| --- | --- |
| MCP-OP-PREP-REQ-001 | yes |
| MCP-OP-PREP-REQ-002 | yes |
| MCP-OP-PREP-REQ-003 | yes |
| MCP-OP-PREP-REQ-004 | yes |
| MCP-OP-PREP-REQ-005 | yes |
| MCP-OP-PREP-REQ-006 | yes |
| MCP-OP-PREP-REQ-007 | yes |
| MCP-OP-PREP-REQ-008 | yes |
| MCP-OP-PREP-REQ-009 | yes |

## prepared-request-actions

| Spec ID | Referenced in test/** |
| --- | --- |
| MCP-OP-PREP-ACT-001 | yes |
| MCP-OP-PREP-ACT-002 | yes |
| MCP-OP-PREP-ACT-003 | yes |
| MCP-OP-PREP-ACT-004 | yes |
| MCP-OP-PREP-ACT-005 | yes |
| MCP-OP-PREP-ACT-006 | yes |
| MCP-OP-PREP-ACT-007 | yes |
| MCP-OP-PREP-ACT-008 | yes |
| MCP-OP-PREP-ACT-009 | yes |
| MCP-OP-PREP-ACT-010 | yes |
| MCP-OP-PREP-ACT-011 | yes |
| MCP-OP-PREP-ACT-012 | yes |
| MCP-OP-PREP-ACT-013 | yes |
| MCP-OP-PREP-ACT-014 | yes |
| MCP-OP-PREP-ACT-015 | yes |
| MCP-OP-PREP-ACT-016 | yes |
| MCP-OP-PREP-ACT-017 | yes |
| MCP-OP-PREP-ACT-018 | yes |

## read-request-normalization

| Spec ID | Referenced in test/** |
| --- | --- |
| MCP-OP-READ-NORM-001 | yes |
| MCP-OP-READ-NORM-002 | yes |
| MCP-OP-READ-NORM-003 | yes |
| MCP-OP-READ-NORM-004 | yes |
| MCP-OP-READ-NORM-005 | yes |

## shell-argv-safety

| Spec ID | Referenced in test/** |
| --- | --- |
| MCP-OP-SHELL-ARGV-001 | yes |
| MCP-OP-SHELL-ARGV-002 | yes |
| MCP-OP-SHELL-ARGV-003 | yes |

## sibling-pair-edit

| Spec ID | Referenced in test/** |
| --- | --- |
| MCP-OP-PAIR-EDIT-001 | no |
| MCP-OP-PAIR-EDIT-002 | no |
| MCP-OP-PAIR-EDIT-003 | no |
| MCP-OP-PAIR-EDIT-004 | no |
| MCP-OP-PAIR-EDIT-005 | no |
| MCP-OP-PAIR-EDIT-006 | no |
| MCP-OP-PAIR-EDIT-007 | no |
| MCP-OP-PAIR-EDIT-008 | no |
| MCP-OP-PAIR-EDIT-009 | no |
| MCP-OP-PAIR-EDIT-010 | no |

## substantiation-telemetry

| Spec ID | Referenced in test/** |
| --- | --- |
| MCP-OP-SUBST-001 | no |
| MCP-OP-SUBST-002 | no |
| MCP-OP-SUBST-003 | no |
| MCP-OP-SUBST-004 | no |
| MCP-OP-SUBST-005 | no |
| MCP-OP-SUBST-006 | no |
| MCP-OP-SUBST-007 | no |
| MCP-OP-SUBST-008 | no |
| MCP-OP-SUBST-009 | no |
| MCP-OP-SUBST-010 | no |
| MCP-OP-SUBST-011 | no |
| MCP-OP-SUBST-012 | no |
| MCP-OP-SUBST-013 | no |
| MCP-OP-SUBST-014 | no |
| MCP-OP-SUBST-015 | no |
| MCP-OP-SUBST-016 | no |
| MCP-OP-SUBST-017 | no |
| MCP-OP-SUBST-018 | no |
| MCP-OP-SUBST-019 | no |

## worktree-lifecycle

| Spec ID | Referenced in test/** |
| --- | --- |
| WTL-APPLY-001 | yes |
| WTL-APPLY-002 | yes |
| WTL-APPLY-003 | yes |
| WTL-APPLY-004 | yes |
| WTL-APPLY-005 | yes |
| WTL-APPLY-006 | yes |
| WTL-APPLY-007 | yes |
| WTL-APPLY-008 | yes |
| WTL-APPLY-009 | yes |
| WTL-APPLY-010 | yes |
| WTL-APPLY-011 | yes |
| WTL-APPLY-012 | yes |
| WTL-CLI-001 | yes |
| WTL-CLI-002 | yes |
| WTL-CLI-003 | yes |
| WTL-CLI-004 | no |
| WTL-HAND-001 | yes |
| WTL-HAND-002 | yes |
| WTL-HAND-003 | yes |
| WTL-HAND-004 | yes |
| WTL-HAND-005 | yes |
| WTL-INV-001 | yes |
| WTL-INV-002 | yes |
| WTL-INV-003 | yes |
| WTL-INV-004 | yes |
| WTL-INV-005 | yes |
| WTL-INV-006 | yes |
| WTL-INV-007 | yes |
| WTL-INV-008 | yes |
| WTL-PLAN-001 | yes |
| WTL-PLAN-002 | yes |
| WTL-PLAN-003 | yes |
| WTL-PLAN-004 | yes |
| WTL-PLAN-005 | yes |
| WTL-PLAN-006 | yes |
| WTL-PRUNE-001 | yes |
| WTL-PRUNE-002 | yes |
| WTL-PRUNE-003 | yes |
| WTL-PRUNE-004 | yes |
| WTL-PRUNE-005 | yes |
| WTL-PRUNE-006 | yes |
| WTL-PRUNE-007 | yes |
| WTL-PRUNE-008 | yes |
| WTL-PRUNE-009 | yes |
| WTL-PRUNE-010 | yes |
| WTL-SEAL-001 | yes |
| WTL-SEAL-002 | yes |
| WTL-SEAL-003 | yes |
| WTL-SEAL-004 | yes |
| WTL-SEAL-005 | yes |
| WTL-SEAL-006 | yes |
| WTL-SEAL-007 | yes |
| WTL-SEAL-008 | no |

## write-refusal-completeness

| Spec ID | Referenced in test/** |
| --- | --- |
| MCP-OP-WRITE-REFUSAL-001 | yes |
| MCP-OP-WRITE-REFUSAL-002 | no |
| MCP-OP-WRITE-REFUSAL-003 | no |
| MCP-OP-WRITE-REFUSAL-004 | no |
| MCP-OP-WRITE-REFUSAL-005 | no |
| MCP-OP-WRITE-REFUSAL-006 | no |
| MCP-OP-WRITE-REFUSAL-007 | no |
| MCP-OP-WRITE-REFUSAL-008 | no |
