# Cell B — proving the oracle (mutation tests)

Every mutation below was planted on the acceptance owner's own hand-driven extraction
(untimed, correctness-only, in `/home/forge/src/cc-cellB-oracle`), one at a time, with all
eleven files restored from a clean snapshot between mutations. Each row is a **full**
`oracle.sh` run — every check group, not just the one expected to catch it.

| # | planted defect | oracle verdict | first checks that caught it |
|---|---|---|---|
| — | **none** (the clean hand-driven extraction) | `ORACLE RESULT: PASS (all checks)` | — |
| M1 | leave one moved reference behind | `ORACLE RESULT: FAIL (7 check group(s) failed)` | A4 resolved references to cfp-scheduler-killer.exports.calendar; A5 left-behind references to cfp-scheduler-killer.exports; B (destination-first) fresh-process load failed (rc=1); B (source-first) fresh-process load failed (rc=1) |
| M2 | redirect a retained mixed-caller reference | `ORACLE RESULT: FAIL (7 check group(s) failed)` | A6 retained references redirected or lost; B (destination-first) fresh-process load failed (rc=1); B (source-first) fresh-process load failed (rc=1); C focused cluster tests failed (rc=1) |
| M3 | omit a needed require | `ORACLE RESULT: FAIL (7 check group(s) failed)` | A4 resolved references to cfp-scheduler-killer.exports.calendar; B (destination-first) fresh-process load failed (rc=1); B (source-first) fresh-process load failed (rc=1); C focused cluster tests failed (rc=1) |
| M4 | leave a stale definition in exports.clj | `ORACLE RESULT: FAIL (3 check group(s) failed)` | A1 destination inventory; A2 stale definitions still in cfp-scheduler-killer.exports; A3b duplicate definitions; B (destination-first) fresh-process load failed (rc=1) |
| M5 | change one behaviour | `ORACLE RESULT: FAIL (3 check group(s) failed)` | C focused cluster tests failed (rc=1); D make runtests-once failed (rc=2); D bin/kaocha unit --fail-fast failed (rc=1) |

## Clean candidate (the control)

The hand-driven extraction that the mutations are planted on. It passes the frozen
oracle unmodified — so a FAIL below is the mutation, not the apparatus.

```
=== 0. preflight
PASS  preflight: worktree=/home/forge/src/cc-cellB-oracle base=92a7ca14fd904e4d962df38614b9af3a7ef41a11 kondo=clj-kondo v2026.08.04

=== A. structural inventory (clj-kondo analysis, resolved var usages — not string matching)
PASS  A1 all 25 closure forms defined exactly once, in cfp-scheduler-killer.exports.calendar (src/cfp_scheduler_killer/exports/calendar.clj)
PASS  A2 no closure form is still defined in cfp-scheduler-killer.exports
PASS  A3 no facade: cfp-scheduler-killer.exports contains no reference to cfp-scheduler-killer.exports.calendar (also the acyclic-require guard)
PASS  A3b no duplicate definition of any closure name outside cfp-scheduler-killer.exports.calendar
PASS  A4 all 43 external references across 9 namespaces resolve to cfp-scheduler-killer.exports.calendar
PASS  A5 no reference resolves a moved var to cfp-scheduler-killer.exports
PASS  A6 retained-var references unchanged (197 external refs still resolve to cfp-scheduler-killer.exports)
PASS  A7 arities, visibility and def form preserved for all 25 forms

=== A8. lint: no unresolved vars/namespaces in the touched files
PASS  A8 clj-kondo reports no unresolved var/namespace/symbol findings

=== B. fresh-process load in two orders (cycles, stale declares, stale interns)
PASS  B (destination-first) fresh process loaded clean; all 25 vars interned in the destination only
PASS  B (source-first) fresh process loaded clean; all 25 vars interned in the destination only
      bootstrap note: both orders use the plain `clojure -M` classpath (src only, no :run-tests alias),
      so they prove the production namespaces load standalone. They do NOT prove test-path loading;
      check C covers that.

=== C. behavioural: the focused tests over the cluster + one integration path
PASS  C focused cluster tests: 8 tests, 80 assertions, 0 failures.
PASS  C integration path (public-widgets .ics route → handler → calendar-ics-for): 28 tests, 461 assertions, 0 failures.

=== D. the repository's own required entry points
      precondition: init.defaultBranch is unset globally; the oracle pins it to main for this gate only
PASS  D make runtests-once green — 1021 tests, 12397 assertions, 0 failures.
      components: 5 Prolog oracle files, node --test, new-mission worktree test, bin/kaocha unit
PASS  D bin/kaocha unit --fail-fast green — 1021 tests, 12397 assertions, 0 failures.

=== E. protected content: everything outside the authorised footprint is byte-identical to 92a7ca14fd904e4d962df38614b9af3a7ef41a11
PASS  E every file outside the authorised footprint is byte-identical to 92a7ca14fd904e4d962df38614b9af3a7ef41a11
      footprint diff vs base:
       src/cfp_scheduler_killer/agent/commands.clj        |   3 +-
       src/cfp_scheduler_killer/exports.clj               | 363 +--------------------
       src/cfp_scheduler_killer/handlers/exports.clj      |   3 +-
       src/cfp_scheduler_killer/handlers/public_cfp.clj   |   4 +-
       .../handlers/public_widgets.clj                    |   3 +-
       src/cfp_scheduler_killer/inform.clj                |   4 +-
       src/cfp_scheduler_killer/session_invites.clj       |   5 +-
       test/cfp_scheduler_killer/comms_test.clj           |   7 +-
       test/cfp_scheduler_killer/exports_test.clj         |  43 +--
       test/cfp_scheduler_killer/schedule_test.clj        |  25 +-
       10 files changed, 56 insertions(+), 404 deletions(-)
      new file src/cfp_scheduler_killer/exports/calendar.clj: 376 lines

ORACLE RESULT: PASS (all checks)
EXIT=0
```

## M1 — leave one moved reference behind

**Planted:** `src/cfp_scheduler_killer/session_invites.clj` — one production call site reverted from `calendar/cancellation-for` back to `exports/cancellation-for` (the alias `exports` is still legitimately required by that file for a retained var, so the file still compiles as text).

```
=== 0. preflight
PASS  preflight: worktree=/home/forge/src/cc-cellB-oracle base=92a7ca14fd904e4d962df38614b9af3a7ef41a11 kondo=clj-kondo v2026.08.04

=== A. structural inventory (clj-kondo analysis, resolved var usages — not string matching)
PASS  A1 all 25 closure forms defined exactly once, in cfp-scheduler-killer.exports.calendar (src/cfp_scheduler_killer/exports/calendar.clj)
PASS  A2 no closure form is still defined in cfp-scheduler-killer.exports
PASS  A3 no facade: cfp-scheduler-killer.exports contains no reference to cfp-scheduler-killer.exports.calendar (also the acyclic-require guard)
PASS  A3b no duplicate definition of any closure name outside cfp-scheduler-killer.exports.calendar
FAIL  A4 resolved references to cfp-scheduler-killer.exports.calendar: cfp-scheduler-killer.session-invites: 1 != 2
FAIL  A5 left-behind references to cfp-scheduler-killer.exports: src/cfp_scheduler_killer/session_invites.clj:28 cancellation-for
PASS  A6 retained-var references unchanged (197 external refs still resolve to cfp-scheduler-killer.exports)
PASS  A7 arities, visibility and def form preserved for all 25 forms

=== A8. lint: no unresolved vars/namespaces in the touched files
PASS  A8 clj-kondo reports no unresolved var/namespace/symbol findings

=== B. fresh-process load in two orders (cycles, stale declares, stale interns)
FAIL  B (destination-first) fresh-process load failed (rc=1)
      Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
      Syntax error compiling at (cfp_scheduler_killer/session_invites.clj:28:18).
      No such var: exports/cancellation-for
      
      Full report at:
      /var/tmp/forge/clojure-2249297620340197814.edn
FAIL  B (source-first) fresh-process load failed (rc=1)
      Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
      Syntax error compiling at (cfp_scheduler_killer/session_invites.clj:28:18).
      No such var: exports/cancellation-for
      
      Full report at:
      /var/tmp/forge/clojure-17140609341122948440.edn
      bootstrap note: both orders use the plain `clojure -M` classpath (src only, no :run-tests alias),
      so they prove the production namespaces load standalone. They do NOT prove test-path loading;
      check C covers that.

=== C. behavioural: the focused tests over the cluster + one integration path
FAIL  C focused cluster tests failed (rc=1): 1 tests, 1 assertions, 1 errors, 0 failures.
          cfp_scheduler_killer.routes_additive$eval55291$loading__6812__auto____55292.invoke (routes_additive.clj:1)
          cfp_scheduler_killer.routes_additive$eval55291.invokeStatic (routes_additive.clj:1)
          cfp_scheduler_killer.routes_additive$eval55291.invoke (routes_additive.clj:1)
          ...
          cfp_scheduler_killer.server$eval36926$loading__6812__auto____36927.invoke (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invokeStatic (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invoke (server.clj:1)
          ...
          cfp_scheduler_killer.acceptance_notify_test$eval5893$loading__6812__auto____5894.invoke (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invokeStatic (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invoke (acceptance_notify_test.clj:1)
          ...
      (Rest of stacktrace elided)
      Caused by: java.lang.RuntimeException: No such var: exports/cancellation-for
       at clojure.lang.Util.runtimeException (Util.java:221)
          ...
          cfp_scheduler_killer.routes_additive$eval55291$loading__6812__auto____55292.invoke (routes_additive.clj:1)
          cfp_scheduler_killer.routes_additive$eval55291.invokeStatic (routes_additive.clj:1)
          cfp_scheduler_killer.routes_additive$eval55291.invoke (routes_additive.clj:1)
          ...
          cfp_scheduler_killer.server$eval36926$loading__6812__auto____36927.invoke (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invokeStatic (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invoke (server.clj:1)
          ...
          cfp_scheduler_killer.acceptance_notify_test$eval5893$loading__6812__auto____5894.invoke (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invokeStatic (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invoke (acceptance_notify_test.clj:1)
          ...
      (Rest of stacktrace elided)
      [31m1 tests, 1 assertions, 1 errors, 0 failures.[m
FAIL  C integration path (POST /agenda/public-widgets/my.ics through the ring handler) failed: 1 tests, 1 assertions, 1 errors, 0 failures.
          cfp_scheduler_killer.routes_additive$eval55291$loading__6812__auto____55292.invoke (routes_additive.clj:1)
          cfp_scheduler_killer.routes_additive$eval55291.invokeStatic (routes_additive.clj:1)
          cfp_scheduler_killer.routes_additive$eval55291.invoke (routes_additive.clj:1)
          ...
          cfp_scheduler_killer.server$eval36926$loading__6812__auto____36927.invoke (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invokeStatic (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invoke (server.clj:1)
          ...
          cfp_scheduler_killer.acceptance_notify_test$eval5893$loading__6812__auto____5894.invoke (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invokeStatic (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invoke (acceptance_notify_test.clj:1)
          ...
      (Rest of stacktrace elided)
      Caused by: java.lang.RuntimeException: No such var: exports/cancellation-for
       at clojure.lang.Util.runtimeException (Util.java:221)
          ...
          cfp_scheduler_killer.routes_additive$eval55291$loading__6812__auto____55292.invoke (routes_additive.clj:1)
          cfp_scheduler_killer.routes_additive$eval55291.invokeStatic (routes_additive.clj:1)
          cfp_scheduler_killer.routes_additive$eval55291.invoke (routes_additive.clj:1)
          ...
          cfp_scheduler_killer.server$eval36926$loading__6812__auto____36927.invoke (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invokeStatic (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invoke (server.clj:1)
          ...
          cfp_scheduler_killer.acceptance_notify_test$eval5893$loading__6812__auto____5894.invoke (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invokeStatic (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invoke (acceptance_notify_test.clj:1)
          ...
      (Rest of stacktrace elided)
      [31m1 tests, 1 assertions, 1 errors, 0 failures.[m

=== D. the repository's own required entry points
      precondition: init.defaultBranch is unset globally; the oracle pins it to main for this gate only
FAIL  D make runtests-once failed (rc=2)
      [[31mE[m]
      Randomized with --seed 143402254
      
      [31mERROR[m in unit (routes_additive.clj:1)
      Failed loading tests:
      Exception: clojure.lang.Compiler$CompilerException: Syntax error compiling at (cfp_scheduler_killer/session_invites.clj:28:18).
      #:clojure.error{:phase :compile-syntax-check, :line 28, :column 18, :source "cfp_scheduler_killer/session_invites.clj"}
       at clojure.lang.Compiler.analyze (Compiler.java:7340)
          ...
          cfp_scheduler_killer.routes_additive$eval55291$loading__6812__auto____55292.invoke (routes_additive.clj:1)
          cfp_scheduler_killer.routes_additive$eval55291.invokeStatic (routes_additive.clj:1)
          cfp_scheduler_killer.routes_additive$eval55291.invoke (routes_additive.clj:1)
          ...
          cfp_scheduler_killer.server$eval36926$loading__6812__auto____36927.invoke (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invokeStatic (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invoke (server.clj:1)
          ...
          cfp_scheduler_killer.acceptance_notify_test$eval5893$loading__6812__auto____5894.invoke (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invokeStatic (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invoke (acceptance_notify_test.clj:1)
          ...
      (Rest of stacktrace elided)
      Caused by: java.lang.RuntimeException: No such var: exports/cancellation-for
       at clojure.lang.Util.runtimeException (Util.java:221)
          ...
          cfp_scheduler_killer.routes_additive$eval55291$loading__6812__auto____55292.invoke (routes_additive.clj:1)
          cfp_scheduler_killer.routes_additive$eval55291.invokeStatic (routes_additive.clj:1)
          cfp_scheduler_killer.routes_additive$eval55291.invoke (routes_additive.clj:1)
          ...
          cfp_scheduler_killer.server$eval36926$loading__6812__auto____36927.invoke (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invokeStatic (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invoke (server.clj:1)
          ...
          cfp_scheduler_killer.acceptance_notify_test$eval5893$loading__6812__auto____5894.invoke (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invokeStatic (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invoke (acceptance_notify_test.clj:1)
          ...
      (Rest of stacktrace elided)
      [31m1 tests, 1 assertions, 1 errors, 0 failures.[m
      make: *** [Makefile:132: runtests-once] Error 1
FAIL  D bin/kaocha unit --fail-fast failed (rc=1)
      WARNING: reset! already refers to: #'clojure.core/reset! in namespace: cfp-scheduler-killer.replay, being replaced by: #'cfp-scheduler-killer.replay/reset!
      [[31mE[m]
      Randomized with --seed 1128405575
      
      [31mERROR[m in unit (routes_additive.clj:1)
      Failed loading tests:
      Exception: clojure.lang.Compiler$CompilerException: Syntax error compiling at (cfp_scheduler_killer/session_invites.clj:28:18).
      #:clojure.error{:phase :compile-syntax-check, :line 28, :column 18, :source "cfp_scheduler_killer/session_invites.clj"}
       at clojure.lang.Compiler.analyze (Compiler.java:7340)
          ...
          cfp_scheduler_killer.routes_additive$eval55291$loading__6812__auto____55292.invoke (routes_additive.clj:1)
          cfp_scheduler_killer.routes_additive$eval55291.invokeStatic (routes_additive.clj:1)
          cfp_scheduler_killer.routes_additive$eval55291.invoke (routes_additive.clj:1)
          ...
          cfp_scheduler_killer.server$eval36926$loading__6812__auto____36927.invoke (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invokeStatic (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invoke (server.clj:1)
          ...
          cfp_scheduler_killer.acceptance_notify_test$eval5893$loading__6812__auto____5894.invoke (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invokeStatic (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invoke (acceptance_notify_test.clj:1)
          ...
      (Rest of stacktrace elided)
      Caused by: java.lang.RuntimeException: No such var: exports/cancellation-for
       at clojure.lang.Util.runtimeException (Util.java:221)
          ...
          cfp_scheduler_killer.routes_additive$eval55291$loading__6812__auto____55292.invoke (routes_additive.clj:1)
          cfp_scheduler_killer.routes_additive$eval55291.invokeStatic (routes_additive.clj:1)
          cfp_scheduler_killer.routes_additive$eval55291.invoke (routes_additive.clj:1)
          ...
          cfp_scheduler_killer.server$eval36926$loading__6812__auto____36927.invoke (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invokeStatic (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invoke (server.clj:1)
          ...
          cfp_scheduler_killer.acceptance_notify_test$eval5893$loading__6812__auto____5894.invoke (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invokeStatic (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invoke (acceptance_notify_test.clj:1)
          ...
      (Rest of stacktrace elided)
      [31m1 tests, 1 assertions, 1 errors, 0 failures.[m

=== E. protected content: everything outside the authorised footprint is byte-identical to 92a7ca14fd904e4d962df38614b9af3a7ef41a11
PASS  E every file outside the authorised footprint is byte-identical to 92a7ca14fd904e4d962df38614b9af3a7ef41a11
      footprint diff vs base:
       src/cfp_scheduler_killer/agent/commands.clj        |   3 +-
       src/cfp_scheduler_killer/exports.clj               | 363 +--------------------
       src/cfp_scheduler_killer/handlers/exports.clj      |   3 +-
       src/cfp_scheduler_killer/handlers/public_cfp.clj   |   4 +-
       .../handlers/public_widgets.clj                    |   3 +-
       src/cfp_scheduler_killer/inform.clj                |   4 +-
       src/cfp_scheduler_killer/session_invites.clj       |   3 +-
       test/cfp_scheduler_killer/comms_test.clj           |   7 +-
       test/cfp_scheduler_killer/exports_test.clj         |  43 +--
       test/cfp_scheduler_killer/schedule_test.clj        |  25 +-
       10 files changed, 55 insertions(+), 403 deletions(-)
      new file src/cfp_scheduler_killer/exports/calendar.clj: 376 lines

ORACLE RESULT: FAIL (7 check group(s) failed)
EXIT=1
```

## M2 — redirect a retained mixed-caller reference

**Planted:** `src/cfp_scheduler_killer/handlers/exports.clj` — the FIRST retained reference in the file (`exports/<var>`) redirected to `calendar/<var>`. The var never moved; this is the mixed-caller mistake of pointing a still-retained reference at the new namespace.

```
=== 0. preflight
PASS  preflight: worktree=/home/forge/src/cc-cellB-oracle base=92a7ca14fd904e4d962df38614b9af3a7ef41a11 kondo=clj-kondo v2026.08.04

=== A. structural inventory (clj-kondo analysis, resolved var usages — not string matching)
PASS  A1 all 25 closure forms defined exactly once, in cfp-scheduler-killer.exports.calendar (src/cfp_scheduler_killer/exports/calendar.clj)
PASS  A2 no closure form is still defined in cfp-scheduler-killer.exports
PASS  A3 no facade: cfp-scheduler-killer.exports contains no reference to cfp-scheduler-killer.exports.calendar (also the acyclic-require guard)
PASS  A3b no duplicate definition of any closure name outside cfp-scheduler-killer.exports.calendar
PASS  A4 all 43 external references across 9 namespaces resolve to cfp-scheduler-killer.exports.calendar
PASS  A5 no reference resolves a moved var to cfp-scheduler-killer.exports
FAIL  A6 retained references redirected or lost: cfp-scheduler-killer.handlers.exports: 8 != 9; total external retained refs 196 != 197
PASS  A7 arities, visibility and def form preserved for all 25 forms

=== A8. lint: no unresolved vars/namespaces in the touched files
PASS  A8 clj-kondo reports no unresolved var/namespace/symbol findings

=== B. fresh-process load in two orders (cycles, stale declares, stale interns)
FAIL  B (destination-first) fresh-process load failed (rc=1)
      Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
      Syntax error compiling at (cfp_scheduler_killer/handlers/exports.clj:29:50).
      No such var: calendar/exports-for
      
      Full report at:
      /var/tmp/forge/clojure-2496592692168510814.edn
FAIL  B (source-first) fresh-process load failed (rc=1)
      Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
      Syntax error compiling at (cfp_scheduler_killer/handlers/exports.clj:29:50).
      No such var: calendar/exports-for
      
      Full report at:
      /var/tmp/forge/clojure-8790695146575102628.edn
      bootstrap note: both orders use the plain `clojure -M` classpath (src only, no :run-tests alias),
      so they prove the production namespaces load standalone. They do NOT prove test-path loading;
      check C covers that.

=== C. behavioural: the focused tests over the cluster + one integration path
FAIL  C focused cluster tests failed (rc=1): 1 tests, 1 assertions, 1 errors, 0 failures.
      Randomized with --seed 1483360373
      
      [31mERROR[m in unit (server.clj:1)
      Failed loading tests:
      Exception: clojure.lang.Compiler$CompilerException: Syntax error compiling at (cfp_scheduler_killer/handlers/exports.clj:29:50).
      #:clojure.error{:phase :compile-syntax-check, :line 29, :column 50, :source "cfp_scheduler_killer/handlers/exports.clj"}
       at clojure.lang.Compiler.analyze (Compiler.java:7340)
          ...
          cfp_scheduler_killer.server$eval36926$loading__6812__auto____36927.invoke (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invokeStatic (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invoke (server.clj:1)
          ...
          cfp_scheduler_killer.acceptance_notify_test$eval5893$loading__6812__auto____5894.invoke (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invokeStatic (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invoke (acceptance_notify_test.clj:1)
          ...
      (Rest of stacktrace elided)
      Caused by: java.lang.RuntimeException: No such var: calendar/exports-for
       at clojure.lang.Util.runtimeException (Util.java:221)
          ...
          cfp_scheduler_killer.server$eval36926$loading__6812__auto____36927.invoke (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invokeStatic (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invoke (server.clj:1)
          ...
          cfp_scheduler_killer.acceptance_notify_test$eval5893$loading__6812__auto____5894.invoke (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invokeStatic (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invoke (acceptance_notify_test.clj:1)
          ...
      (Rest of stacktrace elided)
      [31m1 tests, 1 assertions, 1 errors, 0 failures.[m
FAIL  C integration path (POST /agenda/public-widgets/my.ics through the ring handler) failed: 1 tests, 1 assertions, 1 errors, 0 failures.
      Randomized with --seed 765020848
      
      [31mERROR[m in unit (server.clj:1)
      Failed loading tests:
      Exception: clojure.lang.Compiler$CompilerException: Syntax error compiling at (cfp_scheduler_killer/handlers/exports.clj:29:50).
      #:clojure.error{:phase :compile-syntax-check, :line 29, :column 50, :source "cfp_scheduler_killer/handlers/exports.clj"}
       at clojure.lang.Compiler.analyze (Compiler.java:7340)
          ...
          cfp_scheduler_killer.server$eval36926$loading__6812__auto____36927.invoke (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invokeStatic (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invoke (server.clj:1)
          ...
          cfp_scheduler_killer.acceptance_notify_test$eval5893$loading__6812__auto____5894.invoke (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invokeStatic (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invoke (acceptance_notify_test.clj:1)
          ...
      (Rest of stacktrace elided)
      Caused by: java.lang.RuntimeException: No such var: calendar/exports-for
       at clojure.lang.Util.runtimeException (Util.java:221)
          ...
          cfp_scheduler_killer.server$eval36926$loading__6812__auto____36927.invoke (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invokeStatic (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invoke (server.clj:1)
          ...
          cfp_scheduler_killer.acceptance_notify_test$eval5893$loading__6812__auto____5894.invoke (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invokeStatic (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invoke (acceptance_notify_test.clj:1)
          ...
      (Rest of stacktrace elided)
      [31m1 tests, 1 assertions, 1 errors, 0 failures.[m

=== D. the repository's own required entry points
      precondition: init.defaultBranch is unset globally; the oracle pins it to main for this gate only
FAIL  D make runtests-once failed (rc=2)
      # duration_ms 69.287416
      PASS: fetched origin/main beats stale local main and refusals hold
      Running fast unit tests (fail-fast)...
      bin/kaocha unit --fail-fast
      Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
      GUARDRAILS IS ENABLED. RUNTIME PERFORMANCE WILL BE AFFECTED.
      Mode: :runtime  Async? false  Throw? false
      Guardrails was enabled because the guardrails.enabled property is set to a (any) value.
      [[31mE[m]
      Randomized with --seed 31266677
      
      [31mERROR[m in unit (server.clj:1)
      Failed loading tests:
      Exception: clojure.lang.Compiler$CompilerException: Syntax error compiling at (cfp_scheduler_killer/handlers/exports.clj:29:50).
      #:clojure.error{:phase :compile-syntax-check, :line 29, :column 50, :source "cfp_scheduler_killer/handlers/exports.clj"}
       at clojure.lang.Compiler.analyze (Compiler.java:7340)
          ...
          cfp_scheduler_killer.server$eval36926$loading__6812__auto____36927.invoke (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invokeStatic (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invoke (server.clj:1)
          ...
          cfp_scheduler_killer.acceptance_notify_test$eval5893$loading__6812__auto____5894.invoke (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invokeStatic (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invoke (acceptance_notify_test.clj:1)
          ...
      (Rest of stacktrace elided)
      Caused by: java.lang.RuntimeException: No such var: calendar/exports-for
       at clojure.lang.Util.runtimeException (Util.java:221)
          ...
          cfp_scheduler_killer.server$eval36926$loading__6812__auto____36927.invoke (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invokeStatic (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invoke (server.clj:1)
          ...
          cfp_scheduler_killer.acceptance_notify_test$eval5893$loading__6812__auto____5894.invoke (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invokeStatic (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invoke (acceptance_notify_test.clj:1)
          ...
      (Rest of stacktrace elided)
      [31m1 tests, 1 assertions, 1 errors, 0 failures.[m
      make: *** [Makefile:132: runtests-once] Error 1
FAIL  D bin/kaocha unit --fail-fast failed (rc=1)
      Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
      GUARDRAILS IS ENABLED. RUNTIME PERFORMANCE WILL BE AFFECTED.
      Mode: :runtime  Async? false  Throw? false
      Guardrails was enabled because the guardrails.enabled property is set to a (any) value.
      [[31mE[m]
      Randomized with --seed 185529718
      
      [31mERROR[m in unit (server.clj:1)
      Failed loading tests:
      Exception: clojure.lang.Compiler$CompilerException: Syntax error compiling at (cfp_scheduler_killer/handlers/exports.clj:29:50).
      #:clojure.error{:phase :compile-syntax-check, :line 29, :column 50, :source "cfp_scheduler_killer/handlers/exports.clj"}
       at clojure.lang.Compiler.analyze (Compiler.java:7340)
          ...
          cfp_scheduler_killer.server$eval36926$loading__6812__auto____36927.invoke (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invokeStatic (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invoke (server.clj:1)
          ...
          cfp_scheduler_killer.acceptance_notify_test$eval5893$loading__6812__auto____5894.invoke (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invokeStatic (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invoke (acceptance_notify_test.clj:1)
          ...
      (Rest of stacktrace elided)
      Caused by: java.lang.RuntimeException: No such var: calendar/exports-for
       at clojure.lang.Util.runtimeException (Util.java:221)
          ...
          cfp_scheduler_killer.server$eval36926$loading__6812__auto____36927.invoke (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invokeStatic (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invoke (server.clj:1)
          ...
          cfp_scheduler_killer.acceptance_notify_test$eval5893$loading__6812__auto____5894.invoke (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invokeStatic (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invoke (acceptance_notify_test.clj:1)
          ...
      (Rest of stacktrace elided)
      [31m1 tests, 1 assertions, 1 errors, 0 failures.[m

=== E. protected content: everything outside the authorised footprint is byte-identical to 92a7ca14fd904e4d962df38614b9af3a7ef41a11
PASS  E every file outside the authorised footprint is byte-identical to 92a7ca14fd904e4d962df38614b9af3a7ef41a11
      footprint diff vs base:
       src/cfp_scheduler_killer/agent/commands.clj        |   3 +-
       src/cfp_scheduler_killer/exports.clj               | 363 +--------------------
       src/cfp_scheduler_killer/handlers/exports.clj      |   5 +-
       src/cfp_scheduler_killer/handlers/public_cfp.clj   |   4 +-
       .../handlers/public_widgets.clj                    |   3 +-
       src/cfp_scheduler_killer/inform.clj                |   4 +-
       src/cfp_scheduler_killer/session_invites.clj       |   5 +-
       test/cfp_scheduler_killer/comms_test.clj           |   7 +-
       test/cfp_scheduler_killer/exports_test.clj         |  43 +--
       test/cfp_scheduler_killer/schedule_test.clj        |  25 +-
       10 files changed, 57 insertions(+), 405 deletions(-)
      new file src/cfp_scheduler_killer/exports/calendar.clj: 376 lines

ORACLE RESULT: FAIL (7 check group(s) failed)
EXIT=1
```

## M3 — omit a needed require

**Planted:** `src/cfp_scheduler_killer/session_invites.clj` — the `[cfp-scheduler-killer.exports.calendar :as calendar]` require deleted from the `ns` form while the call sites still say `calendar/…`.

```
=== 0. preflight
PASS  preflight: worktree=/home/forge/src/cc-cellB-oracle base=92a7ca14fd904e4d962df38614b9af3a7ef41a11 kondo=clj-kondo v2026.08.04

=== A. structural inventory (clj-kondo analysis, resolved var usages — not string matching)
PASS  A1 all 25 closure forms defined exactly once, in cfp-scheduler-killer.exports.calendar (src/cfp_scheduler_killer/exports/calendar.clj)
PASS  A2 no closure form is still defined in cfp-scheduler-killer.exports
PASS  A3 no facade: cfp-scheduler-killer.exports contains no reference to cfp-scheduler-killer.exports.calendar (also the acyclic-require guard)
PASS  A3b no duplicate definition of any closure name outside cfp-scheduler-killer.exports.calendar
FAIL  A4 resolved references to cfp-scheduler-killer.exports.calendar: cfp-scheduler-killer.session-invites: 0 != 2
PASS  A5 no reference resolves a moved var to cfp-scheduler-killer.exports
PASS  A6 retained-var references unchanged (197 external refs still resolve to cfp-scheduler-killer.exports)
PASS  A7 arities, visibility and def form preserved for all 25 forms

=== A8. lint: no unresolved vars/namespaces in the touched files
PASS  A8 clj-kondo reports no unresolved var/namespace/symbol findings

=== B. fresh-process load in two orders (cycles, stale declares, stale interns)
FAIL  B (destination-first) fresh-process load failed (rc=1)
      Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
      Syntax error compiling at (cfp_scheduler_killer/session_invites.clj:16:3).
      No such namespace: calendar
      
      Full report at:
      /var/tmp/forge/clojure-8258105167989774417.edn
FAIL  B (source-first) fresh-process load failed (rc=1)
      Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
      Syntax error compiling at (cfp_scheduler_killer/session_invites.clj:16:3).
      No such namespace: calendar
      
      Full report at:
      /var/tmp/forge/clojure-6700541708197559356.edn
      bootstrap note: both orders use the plain `clojure -M` classpath (src only, no :run-tests alias),
      so they prove the production namespaces load standalone. They do NOT prove test-path loading;
      check C covers that.

=== C. behavioural: the focused tests over the cluster + one integration path
FAIL  C focused cluster tests failed (rc=1): 1 tests, 1 assertions, 1 errors, 0 failures.
          cfp_scheduler_killer.routes_additive$eval55291$loading__6812__auto____55292.invoke (routes_additive.clj:1)
          cfp_scheduler_killer.routes_additive$eval55291.invokeStatic (routes_additive.clj:1)
          cfp_scheduler_killer.routes_additive$eval55291.invoke (routes_additive.clj:1)
          ...
          cfp_scheduler_killer.server$eval36926$loading__6812__auto____36927.invoke (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invokeStatic (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invoke (server.clj:1)
          ...
          cfp_scheduler_killer.acceptance_notify_test$eval5893$loading__6812__auto____5894.invoke (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invokeStatic (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invoke (acceptance_notify_test.clj:1)
          ...
      (Rest of stacktrace elided)
      Caused by: java.lang.RuntimeException: No such namespace: calendar
       at clojure.lang.Util.runtimeException (Util.java:221)
          ...
          cfp_scheduler_killer.routes_additive$eval55291$loading__6812__auto____55292.invoke (routes_additive.clj:1)
          cfp_scheduler_killer.routes_additive$eval55291.invokeStatic (routes_additive.clj:1)
          cfp_scheduler_killer.routes_additive$eval55291.invoke (routes_additive.clj:1)
          ...
          cfp_scheduler_killer.server$eval36926$loading__6812__auto____36927.invoke (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invokeStatic (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invoke (server.clj:1)
          ...
          cfp_scheduler_killer.acceptance_notify_test$eval5893$loading__6812__auto____5894.invoke (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invokeStatic (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invoke (acceptance_notify_test.clj:1)
          ...
      (Rest of stacktrace elided)
      [31m1 tests, 1 assertions, 1 errors, 0 failures.[m
FAIL  C integration path (POST /agenda/public-widgets/my.ics through the ring handler) failed: 1 tests, 1 assertions, 1 errors, 0 failures.
          cfp_scheduler_killer.routes_additive$eval55291$loading__6812__auto____55292.invoke (routes_additive.clj:1)
          cfp_scheduler_killer.routes_additive$eval55291.invokeStatic (routes_additive.clj:1)
          cfp_scheduler_killer.routes_additive$eval55291.invoke (routes_additive.clj:1)
          ...
          cfp_scheduler_killer.server$eval36926$loading__6812__auto____36927.invoke (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invokeStatic (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invoke (server.clj:1)
          ...
          cfp_scheduler_killer.acceptance_notify_test$eval5893$loading__6812__auto____5894.invoke (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invokeStatic (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invoke (acceptance_notify_test.clj:1)
          ...
      (Rest of stacktrace elided)
      Caused by: java.lang.RuntimeException: No such namespace: calendar
       at clojure.lang.Util.runtimeException (Util.java:221)
          ...
          cfp_scheduler_killer.routes_additive$eval55291$loading__6812__auto____55292.invoke (routes_additive.clj:1)
          cfp_scheduler_killer.routes_additive$eval55291.invokeStatic (routes_additive.clj:1)
          cfp_scheduler_killer.routes_additive$eval55291.invoke (routes_additive.clj:1)
          ...
          cfp_scheduler_killer.server$eval36926$loading__6812__auto____36927.invoke (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invokeStatic (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invoke (server.clj:1)
          ...
          cfp_scheduler_killer.acceptance_notify_test$eval5893$loading__6812__auto____5894.invoke (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invokeStatic (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invoke (acceptance_notify_test.clj:1)
          ...
      (Rest of stacktrace elided)
      [31m1 tests, 1 assertions, 1 errors, 0 failures.[m

=== D. the repository's own required entry points
      precondition: init.defaultBranch is unset globally; the oracle pins it to main for this gate only
FAIL  D make runtests-once failed (rc=2)
      [[31mE[m]
      Randomized with --seed 413149471
      
      [31mERROR[m in unit (routes_additive.clj:1)
      Failed loading tests:
      Exception: clojure.lang.Compiler$CompilerException: Syntax error compiling at (cfp_scheduler_killer/session_invites.clj:16:3).
      #:clojure.error{:phase :compile-syntax-check, :line 16, :column 3, :source "cfp_scheduler_killer/session_invites.clj"}
       at clojure.lang.Compiler.analyze (Compiler.java:7340)
          ...
          cfp_scheduler_killer.routes_additive$eval55291$loading__6812__auto____55292.invoke (routes_additive.clj:1)
          cfp_scheduler_killer.routes_additive$eval55291.invokeStatic (routes_additive.clj:1)
          cfp_scheduler_killer.routes_additive$eval55291.invoke (routes_additive.clj:1)
          ...
          cfp_scheduler_killer.server$eval36926$loading__6812__auto____36927.invoke (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invokeStatic (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invoke (server.clj:1)
          ...
          cfp_scheduler_killer.acceptance_notify_test$eval5893$loading__6812__auto____5894.invoke (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invokeStatic (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invoke (acceptance_notify_test.clj:1)
          ...
      (Rest of stacktrace elided)
      Caused by: java.lang.RuntimeException: No such namespace: calendar
       at clojure.lang.Util.runtimeException (Util.java:221)
          ...
          cfp_scheduler_killer.routes_additive$eval55291$loading__6812__auto____55292.invoke (routes_additive.clj:1)
          cfp_scheduler_killer.routes_additive$eval55291.invokeStatic (routes_additive.clj:1)
          cfp_scheduler_killer.routes_additive$eval55291.invoke (routes_additive.clj:1)
          ...
          cfp_scheduler_killer.server$eval36926$loading__6812__auto____36927.invoke (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invokeStatic (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invoke (server.clj:1)
          ...
          cfp_scheduler_killer.acceptance_notify_test$eval5893$loading__6812__auto____5894.invoke (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invokeStatic (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invoke (acceptance_notify_test.clj:1)
          ...
      (Rest of stacktrace elided)
      [31m1 tests, 1 assertions, 1 errors, 0 failures.[m
      make: *** [Makefile:132: runtests-once] Error 1
FAIL  D bin/kaocha unit --fail-fast failed (rc=1)
      WARNING: reset! already refers to: #'clojure.core/reset! in namespace: cfp-scheduler-killer.replay, being replaced by: #'cfp-scheduler-killer.replay/reset!
      [[31mE[m]
      Randomized with --seed 1076659373
      
      [31mERROR[m in unit (routes_additive.clj:1)
      Failed loading tests:
      Exception: clojure.lang.Compiler$CompilerException: Syntax error compiling at (cfp_scheduler_killer/session_invites.clj:16:3).
      #:clojure.error{:phase :compile-syntax-check, :line 16, :column 3, :source "cfp_scheduler_killer/session_invites.clj"}
       at clojure.lang.Compiler.analyze (Compiler.java:7340)
          ...
          cfp_scheduler_killer.routes_additive$eval55291$loading__6812__auto____55292.invoke (routes_additive.clj:1)
          cfp_scheduler_killer.routes_additive$eval55291.invokeStatic (routes_additive.clj:1)
          cfp_scheduler_killer.routes_additive$eval55291.invoke (routes_additive.clj:1)
          ...
          cfp_scheduler_killer.server$eval36926$loading__6812__auto____36927.invoke (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invokeStatic (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invoke (server.clj:1)
          ...
          cfp_scheduler_killer.acceptance_notify_test$eval5893$loading__6812__auto____5894.invoke (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invokeStatic (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invoke (acceptance_notify_test.clj:1)
          ...
      (Rest of stacktrace elided)
      Caused by: java.lang.RuntimeException: No such namespace: calendar
       at clojure.lang.Util.runtimeException (Util.java:221)
          ...
          cfp_scheduler_killer.routes_additive$eval55291$loading__6812__auto____55292.invoke (routes_additive.clj:1)
          cfp_scheduler_killer.routes_additive$eval55291.invokeStatic (routes_additive.clj:1)
          cfp_scheduler_killer.routes_additive$eval55291.invoke (routes_additive.clj:1)
          ...
          cfp_scheduler_killer.server$eval36926$loading__6812__auto____36927.invoke (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invokeStatic (server.clj:1)
          cfp_scheduler_killer.server$eval36926.invoke (server.clj:1)
          ...
          cfp_scheduler_killer.acceptance_notify_test$eval5893$loading__6812__auto____5894.invoke (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invokeStatic (acceptance_notify_test.clj:1)
          cfp_scheduler_killer.acceptance_notify_test$eval5893.invoke (acceptance_notify_test.clj:1)
          ...
      (Rest of stacktrace elided)
      [31m1 tests, 1 assertions, 1 errors, 0 failures.[m

=== E. protected content: everything outside the authorised footprint is byte-identical to 92a7ca14fd904e4d962df38614b9af3a7ef41a11
PASS  E every file outside the authorised footprint is byte-identical to 92a7ca14fd904e4d962df38614b9af3a7ef41a11
      footprint diff vs base:
       src/cfp_scheduler_killer/agent/commands.clj        |   3 +-
       src/cfp_scheduler_killer/exports.clj               | 363 +--------------------
       src/cfp_scheduler_killer/handlers/exports.clj      |   3 +-
       src/cfp_scheduler_killer/handlers/public_cfp.clj   |   4 +-
       .../handlers/public_widgets.clj                    |   3 +-
       src/cfp_scheduler_killer/inform.clj                |   4 +-
       src/cfp_scheduler_killer/session_invites.clj       |   4 +-
       test/cfp_scheduler_killer/comms_test.clj           |   7 +-
       test/cfp_scheduler_killer/exports_test.clj         |  43 +--
       test/cfp_scheduler_killer/schedule_test.clj        |  25 +-
       10 files changed, 55 insertions(+), 404 deletions(-)
      new file src/cfp_scheduler_killer/exports/calendar.clj: 376 lines

ORACLE RESULT: FAIL (7 check group(s) failed)
EXIT=1
```

## M4 — leave a stale definition in exports.clj

**Planted:** `src/cfp_scheduler_killer/exports.clj` — a second, divergent `(defn ics-escape …)` left behind in the source namespace, as an incomplete cut would.

```
=== 0. preflight
PASS  preflight: worktree=/home/forge/src/cc-cellB-oracle base=92a7ca14fd904e4d962df38614b9af3a7ef41a11 kondo=clj-kondo v2026.08.04

=== A. structural inventory (clj-kondo analysis, resolved var usages — not string matching)
FAIL  A1 destination inventory: ics-escape: 2 definitions in ['cfp-scheduler-killer.exports', 'cfp-scheduler-killer.exports.calendar']
FAIL  A2 stale definitions still in cfp-scheduler-killer.exports: ics-escape
PASS  A3 no facade: cfp-scheduler-killer.exports contains no reference to cfp-scheduler-killer.exports.calendar (also the acyclic-require guard)
FAIL  A3b duplicate definitions: ics-escape@cfp-scheduler-killer.exports
PASS  A4 all 43 external references across 9 namespaces resolve to cfp-scheduler-killer.exports.calendar
PASS  A5 no reference resolves a moved var to cfp-scheduler-killer.exports
PASS  A6 retained-var references unchanged (197 external refs still resolve to cfp-scheduler-killer.exports)
PASS  A7 arities, visibility and def form preserved for all 25 forms

=== A8. lint: no unresolved vars/namespaces in the touched files
PASS  A8 clj-kondo reports no unresolved var/namespace/symbol findings

=== B. fresh-process load in two orders (cycles, stale declares, stale interns)
FAIL  B (destination-first) fresh-process load failed (rc=1)
      Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
      Execution error (ExceptionInfo) at user/eval38885 (REPL:13).
      still interned in exports: [ics-escape]
      
      Full report at:
      /var/tmp/forge/clojure-16563079116575861931.edn
FAIL  B (source-first) fresh-process load failed (rc=1)
      Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
      Execution error (ExceptionInfo) at user/eval38885 (REPL:13).
      still interned in exports: [ics-escape]
      
      Full report at:
      /var/tmp/forge/clojure-4422185977802152771.edn
      bootstrap note: both orders use the plain `clojure -M` classpath (src only, no :run-tests alias),
      so they prove the production namespaces load standalone. They do NOT prove test-path loading;
      check C covers that.

=== C. behavioural: the focused tests over the cluster + one integration path
PASS  C focused cluster tests: 8 tests, 80 assertions, 0 failures.
PASS  C integration path (public-widgets .ics route → handler → calendar-ics-for): 28 tests, 461 assertions, 0 failures.

=== D. the repository's own required entry points
      precondition: init.defaultBranch is unset globally; the oracle pins it to main for this gate only
PASS  D make runtests-once green — 1021 tests, 12397 assertions, 0 failures.
      components: 5 Prolog oracle files, node --test, new-mission worktree test, bin/kaocha unit
PASS  D bin/kaocha unit --fail-fast green — 1021 tests, 12397 assertions, 0 failures.

=== E. protected content: everything outside the authorised footprint is byte-identical to 92a7ca14fd904e4d962df38614b9af3a7ef41a11
PASS  E every file outside the authorised footprint is byte-identical to 92a7ca14fd904e4d962df38614b9af3a7ef41a11
      footprint diff vs base:
       src/cfp_scheduler_killer/agent/commands.clj        |   3 +-
       src/cfp_scheduler_killer/exports.clj               | 368 +--------------------
       src/cfp_scheduler_killer/handlers/exports.clj      |   3 +-
       src/cfp_scheduler_killer/handlers/public_cfp.clj   |   4 +-
       .../handlers/public_widgets.clj                    |   3 +-
       src/cfp_scheduler_killer/inform.clj                |   4 +-
       src/cfp_scheduler_killer/session_invites.clj       |   5 +-
       test/cfp_scheduler_killer/comms_test.clj           |   7 +-
       test/cfp_scheduler_killer/exports_test.clj         |  43 +--
       test/cfp_scheduler_killer/schedule_test.clj        |  25 +-
       10 files changed, 61 insertions(+), 404 deletions(-)
      new file src/cfp_scheduler_killer/exports/calendar.clj: 376 lines

ORACLE RESULT: FAIL (3 check group(s) failed)
EXIT=1
```

## M5 — change one behaviour

**Planted:** `src/cfp_scheduler_killer/exports/calendar.clj` — `ics-escape` stops escaping the semicolon (the `(str/replace ";" "\\;")` line deleted). RFC 5545 §3.3.11 violation; everything still compiles.

```
=== 0. preflight
PASS  preflight: worktree=/home/forge/src/cc-cellB-oracle base=92a7ca14fd904e4d962df38614b9af3a7ef41a11 kondo=clj-kondo v2026.08.04

=== A. structural inventory (clj-kondo analysis, resolved var usages — not string matching)
PASS  A1 all 25 closure forms defined exactly once, in cfp-scheduler-killer.exports.calendar (src/cfp_scheduler_killer/exports/calendar.clj)
PASS  A2 no closure form is still defined in cfp-scheduler-killer.exports
PASS  A3 no facade: cfp-scheduler-killer.exports contains no reference to cfp-scheduler-killer.exports.calendar (also the acyclic-require guard)
PASS  A3b no duplicate definition of any closure name outside cfp-scheduler-killer.exports.calendar
PASS  A4 all 43 external references across 9 namespaces resolve to cfp-scheduler-killer.exports.calendar
PASS  A5 no reference resolves a moved var to cfp-scheduler-killer.exports
PASS  A6 retained-var references unchanged (197 external refs still resolve to cfp-scheduler-killer.exports)
PASS  A7 arities, visibility and def form preserved for all 25 forms

=== A8. lint: no unresolved vars/namespaces in the touched files
PASS  A8 clj-kondo reports no unresolved var/namespace/symbol findings

=== B. fresh-process load in two orders (cycles, stale declares, stale interns)
PASS  B (destination-first) fresh process loaded clean; all 25 vars interned in the destination only
PASS  B (source-first) fresh process loaded clean; all 25 vars interned in the destination only
      bootstrap note: both orders use the plain `clojure -M` classpath (src only, no :run-tests alias),
      so they prove the production namespaces load standalone. They do NOT prove test-path loading;
      check C covers that.

=== C. behavioural: the focused tests over the cluster + one integration path
FAIL  C focused cluster tests failed (rc=1): 8 tests, 80 assertions, 1 failures.
      Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
      GUARDRAILS IS ENABLED. RUNTIME PERFORMANCE WILL BE AFFECTED.
      Mode: :runtime  Async? false  Throw? false
      Guardrails was enabled because the guardrails.enabled property is set to a (any) value.
      WARNING: reset! already refers to: #'clojure.core/reset! in namespace: cfp-scheduler-killer.replay, being replaced by: #'cfp-scheduler-killer.replay/reset!
      [(......)(..............................[31mF[m....................)(.......................)]
      Randomized with --seed 1783800477
      
      [31mFAIL[m in cfp-scheduler-killer.exports-test/ics-escaping-test (exports_test.clj:465)
      the four special characters are escaped, per RFC 5545
      Expected:
        [1;35m"a\\; b"[0m
      Actual:
        [31m-"a\\; b"[0m [32m+"a; b"[0m
      [31m8 tests, 80 assertions, 1 failures.[m
PASS  C integration path (public-widgets .ics route → handler → calendar-ics-for): 28 tests, 461 assertions, 0 failures.

=== D. the repository's own required entry points
      precondition: init.defaultBranch is unset globally; the oracle pins it to main for this gate only
FAIL  D make runtests-once failed (rc=2)
      # Subtest: move feedback never calls a refused response moved
      ok 17 - move feedback never calls a refused response moved
        ---
        duration_ms: 0.098137
        type: 'test'
        ...
      # Subtest: zero-duration notifications remain visible until replaced
      ok 18 - zero-duration notifications remain visible until replaced
        ---
        duration_ms: 0.053481
        type: 'test'
        ...
      1..18
      # tests 18
      # suites 0
      # pass 18
      # fail 0
      # cancelled 0
      # skipped 0
      # todo 0
      # duration_ms 69.813535
      PASS: fetched origin/main beats stale local main and refusals hold
      Running fast unit tests (fail-fast)...
      bin/kaocha unit --fail-fast
      Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
      GUARDRAILS IS ENABLED. RUNTIME PERFORMANCE WILL BE AFFECTED.
      Mode: :runtime  Async? false  Throw? false
      Guardrails was enabled because the guardrails.enabled property is set to a (any) value.
      WARNING: reset! already refers to: #'clojure.core/reset! in namespace: cfp-scheduler-killer.replay, being replaced by: #'cfp-scheduler-killer.replay/reset!
      [(.....)(.....................)(....)(...)(.......)(..............................................................................................................................................)(...............)(......)(........................)(....)(.....................................)(...)(.........................)(....)(....)(...............)(............................)(.....)(...................)(...........................)(...................................)(...................)(............)(................)(.......)(...................................................................................)(...............)(.............................................................................)(..................)(......................................................................................................................................................................................................................................................................................................................................................................................................................)(.............)(........................)(............................................)(...........................................................)(..........)(..............................................................)(.......)(..............................................................)(..................................................................................................................................................................................)(......)(.................................)(..............)(.....)(.......................)(.......................................)(..................................................)(................................................................................................................)(...............................................................................................)(...................................................................................)(.......................)(.......)(.................)(.........................................................................................................................................................................................................................................................................................................................)(.......................................)(......)(........................)(........................................)(...................)(.....)(............)(.......................................................................................................................................................................................................................................................................)(................................)(.....)(....................)(...............................................................)(......)(........................................................................................)(..........)(.....................................................................................)(...............)(........................)(.............................)(.................................................)(...........)(...................)(.............)(.........)(............)(....)(..................)(............)(.....)(.........................................................................................................)(......................................................................)(...................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................)(....)(...........................................................................................................................................................................................................................................................................................................)(..................................)(........................................................................................................................................................................................................................................................................)(....)(.......................)(.................................)(......)(.........................................................................................................................................................................................................)(....)(.................)(................................................................................................)(...........................................................................................................................................................)(...................)(....)(......................................)(......................................)(.............................................................................................................................................................................................................................................................................................................................................................................................................................................................................)(...........................)(.....................)(...............................................................................................................................................................................................)(......................................................................................................................................................................................................................................................................................................................................................................................................................................................................)(..................)(...................................)(..............)(................)(....)(......)(......................................................................................................................)(.............................)(.......)(.................)(.......................................................................................................................................................................................................................................................................................................................................................................................................................................)(.......)(.............................)(..................................................................................................................................................................................................)(..........................................)(............................................................................................................................)(.....)(........)(..............................)(.......)(...................................................)(...............................)(...........)(..................................................................................................................................................................................................................[31mF[m)]
      Randomized with --seed 2124827207
      
      [31mFAIL[m in cfp-scheduler-killer.exports-test/ics-escaping-test (exports_test.clj:465)
      the four special characters are escaped, per RFC 5545
      Expected:
        [1;35m"a\\; b"[0m
      Actual:
        [31m-"a\\; b"[0m [32m+"a; b"[0m
      [31m729 tests, 8190 assertions, 1 failures.[m
      make: *** [Makefile:132: runtests-once] Error 1
FAIL  D bin/kaocha unit --fail-fast failed (rc=1)
      Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
      GUARDRAILS IS ENABLED. RUNTIME PERFORMANCE WILL BE AFFECTED.
      Mode: :runtime  Async? false  Throw? false
      Guardrails was enabled because the guardrails.enabled property is set to a (any) value.
      WARNING: reset! already refers to: #'clojure.core/reset! in namespace: cfp-scheduler-killer.replay, being replaced by: #'cfp-scheduler-killer.replay/reset!
      [(............................................................................................................................................................................................................................................................................................................................................................)(.....)(.......)(......)(.......)(.......)(............................................................................................)(..................................................................................................................................................................)(...................................................................................)(.........)(...................................................................................................)(....)(.....)(....)(...)(...)(...............)(........................................................)(.....................................)(......................................)(..............)(......................................................................................................................................................................................................................................................................................................................................................................................................................)(.......)(........................................)(...............................................................................................)(...................)(........)(........)(..................................................................................................................................................................................................)(...............)(......)(.............................)(.........................................................................................................................................................................................................)(...............................................................)(.....)(.................)(..................................................................................................................................................................................................................................................................)(............................................)(.......)(..................................................................................)(................)(.......................................)(...........................................................................................................................................................)(......)(...................................)(.....................................................................................)(...............................................................................................................................................................................................)(...................)(.......)(............................................................................................................................................)(...........................................................)(......................................................................................................................................................................)(................................................................................................)(...........................................................................................................................................................................................................................................................................................................)(...................................................)(....................................)(...........................)(.......................................................................................................................................................................................................................................................................)(.........................)(..........)(........................................................................................)(............)(.....................................................................................)(....)(.............................................................)(.......)(...........)(............)(.....)(...............)(..............)(........................)(....................................................................................................................................)(.....................)(..................)(.................)(............................................................................................................................................................[31mF[m)]
      Randomized with --seed 548986175
      
      [31mFAIL[m in cfp-scheduler-killer.exports-test/ics-escaping-test (exports_test.clj:465)
      the four special characters are escaped, per RFC 5545
      Expected:
        [1;35m"a\\; b"[0m
      Actual:
        [31m-"a\\; b"[0m [32m+"a; b"[0m
      [31m450 tests, 4916 assertions, 1 failures.[m

=== E. protected content: everything outside the authorised footprint is byte-identical to 92a7ca14fd904e4d962df38614b9af3a7ef41a11
PASS  E every file outside the authorised footprint is byte-identical to 92a7ca14fd904e4d962df38614b9af3a7ef41a11
      footprint diff vs base:
       src/cfp_scheduler_killer/agent/commands.clj        |   3 +-
       src/cfp_scheduler_killer/exports.clj               | 363 +--------------------
       src/cfp_scheduler_killer/handlers/exports.clj      |   3 +-
       src/cfp_scheduler_killer/handlers/public_cfp.clj   |   4 +-
       .../handlers/public_widgets.clj                    |   3 +-
       src/cfp_scheduler_killer/inform.clj                |   4 +-
       src/cfp_scheduler_killer/session_invites.clj       |   5 +-
       test/cfp_scheduler_killer/comms_test.clj           |   7 +-
       test/cfp_scheduler_killer/exports_test.clj         |  43 +--
       test/cfp_scheduler_killer/schedule_test.clj        |  25 +-
       10 files changed, 56 insertions(+), 404 deletions(-)
      new file src/cfp_scheduler_killer/exports/calendar.clj: 375 lines

ORACLE RESULT: FAIL (3 check group(s) failed)
EXIT=1
```

## Apparatus defects found while proving the oracle (fixed in the oracle, never by weakening a check)

Both were found by running the oracle against a *known-good* candidate and getting a FAIL.
Neither was fixed by relaxing what is checked.

1. **`make runtests-once` failed at the base commit, for the environment, not the code.**
   Its `new-mission-worktree-test` target runs `bin/test-new-mission-worktree`, which builds
   throwaway git repos with `git init --initial-branch=main` and clones them. On this box
   `init.defaultBranch` is unset globally, so the bare remote's HEAD points at `master`, the
   clone has no `main`, and the target dies with
   `fatal: ambiguous argument 'main': unknown revision`. Verified: it fails identically at
   `92a7ca14` with no refactor applied, and passes when the setting is supplied. The oracle
   now pins `init.defaultBranch=main` for that one invocation and prints the precondition on
   every run. The check itself is unchanged and still required to pass.

2. **Check E called a symlink a deleted file.** The first implementation compared every
   tracked path's `git hash-object` against the base blob and used `[ ! -e path ]` for
   deletions. `bin/mothership-open` is a *dangling symlink* (mode `120000` →
   `../../kiloclaw/bin/mothership-open`), so `-e` is false and the clean candidate was
   reported as having deleted a protected file. Rewritten to use
   `git diff --name-only <base> -- .`, which understands symlinks, modes and type changes.
   The check still fails on any modification, deletion or addition outside the footprint.

3. **A runner defect that contaminated a candidate (worth knowing before the timed cohort).**
   `bash` defers `SIGTERM` while a foreground child is running: killing the oracle process
   let the mutation runner advance one step, apply M1, and leave the worktree mutated. The
   next run then snapshotted that tree as the "clean candidate". Caught because the clean
   control FAILED. Fixed with `trap 'restore' EXIT INT TERM` in the runner and by rebuilding
   the candidate deterministically from the base. **A control that is expected to pass is the
   only thing that catches this class**, which is the argument for keeping the clean run in
   the table above rather than only the mutations.
