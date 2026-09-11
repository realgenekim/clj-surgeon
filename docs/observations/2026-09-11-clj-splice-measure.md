# clj-splice measurements

Recorded 2026-09-11T05:16:20.945825+00:00. Base: `04648059`; tested implementation/catalog tip: `82e7aad9`. Branch `fable/clj-splice`. No push.

## Executed assertion budget

| Inventory | Before deftests / expanded assertions | After deftests / expanded assertions | Ceiling |
|---|---:|---:|---:|
| rename_alias | 24 / 556 | 10 / 215 | 220 assertions |
| insert_forms | 24 / 548 | 10 / 171 | 220 assertions |
| Shared envelope + committed-receipt booleans | 2 / 45 | 4 / 72 | shared allocation |
| Standalone library | absent | 5 / 57 | shared allocation |
| Shared including library | 2 / 45 | 9 / 129 | 140 assertions |
| Total | 50 / 1,149 | 29 / 515 | 580 assertions |

The retained 4,000-line performance witness is separate: one deftest/two assertions, 1.585418547 seconds of planning, PASS against five seconds. It is outside both semantic totals. The 773 lane/intent audit assertions are existing repository-wide infrastructure and are also outside this relocated semantic burden.

## Cold wall and results

| Lane | Before | After | Result / receipt |
|---|---:|---:|---|
| Focused JVM, same instrumented runner | 33.54 s | 17.10 s | PASS; 50/1,149 → 24/458 without library |
| Library + corresponding pure planner groups, JVM | 7.60 s | 8.54 s | PASS; 20/412 → 13/206 |
| Same pure groups, bb 1.13.219 | 7.17 s | 7.18 s | PASS; 20/412 → 13/206 |
| Pure groups, minimum bb 1.12.209 | not measured before | 29.63 s | PASS; 13/206 |
| Final standalone library, JVM / rewrite-clj 1.2.50 | new library | 2.69 s | PASS; 5/57 |
| Final library wired Make target, bb 1.13.219 / rewrite-clj 1.2.55 | new library | 3.54 s | PASS; 5/57 |
| Library, minimum bb 1.12.209 / rewrite-clj 1.2.50 | new library | untimed independent check | PASS; 5/57 |
| Full fast inventory, direct single-JVM diagnostic | not measured | 52.00 s | PASS; 755 tests / 7,931 assertions; 61 namespaces; zero isolation violations |
| Normal `make test-fast` coordinator | NOT RUN | NOT RUN | Conflicts with one-JVM fence; direct full-inventory result above is not its gate receipt |
| `~/bin/suite-run make landing-gate-prewarm` | n/a | NOT RUN | Coordinator plus children and integration servers conflict with hard execution fence |

The library's Make prerequisite itself was executed as `make test-clj-splice-bb`; the normal multiprocess fast coordinator was not executed. The direct fast runner explicitly prints `SERIAL/NOT-A-GATE`.

The initial untouched isolated focus passed 50/1,149 in 37.91 s. A preliminary consolidated isolated run passed 24/469 in 19.04 s; subsequent consolidation changed that inventory, so it is not the final count. Another final instrumented run passed 24/458 in 17.58 s. These are observations under changing contention, not a six-run variance floor, native comparison, routing admission, or controlled speedup. The pure-planner wall did not improve.

## Fixture and process accounting

The same metering wrappers counted actual public planner calls, filesystem-fixture executions, and `clojure.java.shell/sh` calls. These are execution counts, not asserted counts of unique hazards; planner calls inside separate bb children are not included in the parent planner columns.

| Observed executions | Before | After |
|---|---:|---:|
| Parent insertion planner calls | 112 | 109 |
| Parent rename planner calls | 123 | 101 |
| Filesystem fixtures | 76 | 60 |
| bb CLI children | 50 | 8 |
| findmnt children | 133 | 92 |
| All observed suite child processes | 183 | 100 |

Each measured JVM lane used one suite JVM. Clojure CLI classpath/bootstrap startup is inside cold wall but its separate bootstrap process count was not instrumented. No simultaneous suite JVMs were run. Boolean scenarios execute once per verb: three insertion scenarios and five rename scenarios; the completed-write observation also supplies behavior-not-run.

Before metering used immutable `04648059` planner/test overlays under `baseline/`, including original boolean tests. Before CLI children received the same baseline classpath explicitly. Unchanged transport/runtime dependencies came from the owned checkout. After uses the committed implementation. Pure groups correspond through the witness map; grouping, redundant token spellings and newly admitted newline cases change row counts. There is no comparison of a bb subset against the complete JVM suite.

## Exact-byte and mutation evidence

- E4 rename: `02332a74caf1ead4d70c555b530230b8b03aebc306bd72f5d129f111c876da0c`; lines 153/1112 and preorders 537/4661 remain unchanged.
- E4 insertion: remove only normalized-speaker-name and its following blank gap from the frozen E4 output, then reinsert after ns; exact same complete-file SHA above. Four E4 source/anchor/count/parse refusals are retained in that group.
- All three parser/runtime combinations emit projection SHA `eb7f6099906935cd68d29b6a7c99b96a4e129fee2ef9b4640f4b70e76260b622` over the literal witnesses and both complete before/after Opus corpora.
- Additional direct zipper compatibility probe passes on leading trivia + nested discards, quote/metadata, synthetic qualifier, Unicode, and empty source (`preorder.log`).
- Actual committed-receipt class test deliberately fails three times under a temporary `unregistered_splice_probe` catalog entry and `unregistered_splice_field`: missing registry, missing scenario, missing field. Scoped redefinitions are restored; no mutant remains. The normal test is green (`final-verify.log`).
- Final lint: zero errors/warnings through `~/bin/clj-kondo`. Lane audit: 27/213 PASS; intent audit tests: 22/560 PASS; current intent violations: empty.

## Execution caveat and retained logs

At 04:42:33 UTC one command printed load 20.75 and nevertheless launched because uptime was observational rather than conditional. It exited 96 on missing new lane registrations before tests ran; it was already gone when the owned-process check ran. It is excluded from measurements. Subsequent launches use `jvm-guard.py`: an owned file lock, uptime, and a 60-second wait above load 14. All JVM commands use `-J-Xmx1g` and JAVA_TOOL_OPTIONS tempdir under this directory; bb commands set java.io.tmpdir explicitly, with BABASHKA_PRELOADS also set by the final guard for child bb processes.

Raw receipts: `baseline-focused.log`, `focused-before-meter.log`, `focused-after-meter.log`, their `.time` files, `pure-before-*-matched.*`, `pure-after-jvm.*`, `pure-after-bb-final.*`, `pure-after-bb-min.*`, `library-*-final.*`, `library-bb-min.log`, `fast-serial-final.*`, `planner-performance.log`, `final-verify.log`, `lint.log`. Scripts and the immutable baseline overlay remain beside these receipts for review.
