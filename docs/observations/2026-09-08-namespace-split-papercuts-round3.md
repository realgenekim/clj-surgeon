# Namespace split round 3: warm paper-cut loop

Recorded 2026-09-08T00:59:47.250703+00:00, branch `astra/namespace-split`,
starting at `5b78bed2`. Gene authorized the complete REPL repair/proof cycle.
No cc-split-* worktree was modified; no shared server or installed trunk entrance
was used. Fresh fixture: `/var/tmp/forge/plan2/build/cc-r3`, curtaincall-cfp d9205abc.

The final emitted tree has **PAPERCUTS: 0**. Round-2 ownership, imports,
docstrings, alias continuation alignment and advisories remain covered.
NS-SPLIT-028 fixes forms_test.clj:13 and polish_test.clj:3: replace the retired
require at its exact position, even when test-helpers follows views in an unsorted
group. The former NS-SPLIT-026 lexical-position promise is explicitly superseded,
not silently rewritten. The new oracle-derived header witness failed in four
cases before the repair. The old test lacked neighboring project entries and
therefore passed the wrong placement rule.

NS-SPLIT-029/030 add a live-workspace nREPL probe and honest probe-only receipts.
Discovery is bounded and ignores absent/stale/foreign ports. Only destinations,
rewritten callers and selected affected test namespaces are explicitly reloaded;
transitive dependencies determine order without reloading unrelated intermediates.
Reload or test failure rolls back immediately and never invokes cold commands.
`:proof :cold` remains default; `:proof :warm` skips the configured cold profile
command list, reports every skipped command pending, and never claims complete
verification. The profile is configured in `.clj-surgeon.edn`, not inferred from
command names. Thus a profile containing both kaocha and the owner oracle lists
both as pending in warm mode. No caller receives a manufactured cold PASS.

NS-SPLIT-031 corrects oracle attribution: compare HEAD and candidate lint by
file/type/message multiplicity, independent of line movement. Analyzer failure
counts as a failed grade. The oracle is retained in
`test/oracles/namespace_split_papercut_oracle.py` and copied byte-identically to
the requested external path. Its Python regression runs from `make mcp-test`.

## Image loop and clock

One surgeon image, started via `make nrepl`, pid 768475 / port 46155, remained warm.
It loaded the compiler and affected tests. Each loop reloads code, runs
`clojure.test/run-tests`, resets the fixture, executes the compiler's real write
boundary with a no-op profile, and calls the Python oracle. After adding the warm
tier, only this local harness disables app discovery to preserve the requested
no-op loop profile; the product path and final proofs use real discovery. Reset
re-copies the architecture guard because git clean removes untracked test files.
The fixture's separately owned application nREPL, needed for verb verification,
is pid 959335 / port 41987; neither verb nor tests launch a new JVM.

**8 reset/split/oracle iterations; 95.121 seconds inside logged iterations.**
The elapsed span from image-harness setup to last logged iteration was
**1425.1 seconds (23.75 minutes)**, including authoring, fail-first
probes, real application proof and the failed gate/harness repair. This is not an
84-second end-to-end feature claim. The initial emitted-tree fix reached zero at
iteration 02; the later iterations develop and verify the newly requested tier.

| Iteration | Reset + split (s) | Warm tests (s) | Entire iteration (s) | Oracle total | Test failures |
|---|---:|---:|---:|---:|---:|
| 00-baseline | 9.237 | 0.700 | 11.131 | 7 | 0 |
| 01-layout | 8.722 | 0.410 | 10.329 | 5 | 0 |
| 02-baseline-oracle | 8.611 | 0.330 | 10.943 | 0 | 0 |
| 03-warm-tier | 9.137 | 0.306 | 11.443 | 0 | 1 |
| 04-warm-witnesses | 9.416 | 1.911 | 13.500 | 0 | 2 |
| 05-reload-order | 8.630 | 1.937 | 12.863 | 0 | 0 |
| 06-final-oracle-witness | 8.766 | 2.057 | 13.419 | 0 | 0 |
| 07-suite-harness | 8.963 | 0.542 | 11.493 | 0 | 0 |

Log: [astra-r3-loop.log](namespace-split-r3/astra-r3-loop.log).
The initial split was 9.237 s, above the requested <5 s target: its two serialized
analyzer calls alone consumed 5.530 s. Subsequent reset/splits remained 8.61–9.42 s.
This target was **not met**, and no analyzer check was bypassed to make it appear so.
The final affected warm run is 33 tests / 404 assertions, zero failures/errors.

Red evidence remains under `/var/tmp/forge/plan2/cellC/`: layout-red, warm-mode-red,
and warm-boundary-red (the new boundary tests executed against the branch-tip
boundary loaded into the image, then the implementation reloaded). An omitted
intent witness failed iteration 03; dependency ordering failed iteration 04.
A reload delimiter error was repaired before emitting iteration 04 and did not
produce a fixture/oracle iteration. These failures were not erased from the log.

## What the warm loop found, and what it could not see

The output oracle reproduced the misplaced blocks that the prior cohort and
round-2 witness did not catch. It also exposed its own false attribution of five
pre-existing warnings. The affected-test matrix found a hidden ordering edge
through an unchanged intermediary; the corrected selection puts runtime reloads
before the dependent tests.

The first final make gate failed: subprocess witnesses had been placed in the
fast lane, and an embedded test nREPL inherited outer test-reporting bindings.
Focused run-tests had not exercised the lane checker. The corrected integration
witness stubs captured analysis (the analyzer is independently exercised on the
real application) and logs its actual `/usr/bin/true` profile invocation, with an
exact, documented spawn allowance. Deliberate failures have isolated test reporting.
Its isolation probe passed 113 assertions with no violations in 0.553 s. Python
oracle tests run at the Make entrance, not inside a process-free JVM test lane.
The second cold attempt passed behavior and isolation, but found the pinned
corpus counts and missing @spec links for the new EARS rows. Both audit namespaces
then passed in the warm image: 46 tests / 597 assertions in 0.766 s. The third
`make test` passed. The intended single cold attempt was **not achieved**. No cold kaocha ran in a paper-cut iteration.

Moving the test Vars between namespaces exposed stale Vars in the development
image itself. The moved Vars were explicitly unmapped in that owned image; the
product never unmaps or removes caller namespaces. Warm tests cannot prove the
absence of stale definitions, cached state, startup/classpath problems, dynamic
callers or unselected tests. The real warm probe ran 168 tests / 1,767 assertions,
whereas cold acceptance ran 231 tests / 2,258 assertions. Caller comprehension
and host-style quality still require independent review/oracles; a green runtime
suite did not reveal the original require-layout problem.

## Real profile proof

Both calls reset the d9205abc fixture and re-copy the architecture test. The real
profile preserves D1's order: kaocha unit, then cc-oracle.py. All application
commands used the branch image, with the original request repointed to cc-r3.
The stock application nREPL was loaded before measurement. One preliminary
application load overlapped a fixture reset and failed; it was reloaded successfully
before either measured call. That setup attempt is excluded from in-call timings.

| Mode | In-call wall | Warm probe | Cold kaocha | Owner oracle | Receipt |
|---|---:|---:|---:|---:|---|
| cold | 42.579 s | 11.609 s | 22.222 s | 0.028 s | committed; verification_complete true |
| warm | 19.359 s | 10.698 s | pending | pending | committed-probe-only; verification_complete false |

The actual warm pending list is
`["bin/kaocha unit --fail-fast" "python3 /var/tmp/forge/plan2/cellC/cc-oracle.py /var/tmp/forge/plan2/build/cc-r3"]`.
This is measured proof-tier cost, not a new matched-native speed claim.
Nine boundary cases (default/cold/warm × green/reload failure/test failure) include
a destination that throws only in its new namespace, with the original source
load verified first. All failing probes return in <2 s and leave no cold command
log. Green cold cases report both clocks separately. A Babashka branch-classpath
probe also exercised the shared bencode client against the stock nREPL.

Four acceptance lines (suite and owner output from the real cold call; architecture
re-run separately in the app image):

```text
231 tests, 2258 assertions, 0 failures.
views.clj gone: true
PASS: views.clj gone; all 141 owners occur exactly once in their 20 listed destination files.
Architecture guard: 1 test, 7 assertions, 0 failures, 0 errors (7/7).
```

Raw evidence: [cold](namespace-split-r3/astra-r3-final-cold.txt),
[warm](namespace-split-r3/astra-r3-final-warm.txt),
[architecture](namespace-split-r3/astra-r3-architecture.txt).

The five formerly counted lint exclusions are exact HEAD diagnostic matches:

- `src/cfp_scheduler_killer/server.clj:56:5`: unused `ring.util.response`.
- `test/cfp_scheduler_killer/replay_test.clj:19:23`: unused `LocalDate` import.
- `test/cfp_scheduler_killer/reviews_test.clj:13:14`: unused `cfp-scheduler-killer.seed`.
- `test/cfp_scheduler_killer/sinks_test.clj:13:14`: unused `cfp-scheduler-killer.exports`.
- `test/cfp_scheduler_killer/store_test.clj:6:14`: unused `clojure.java.io`.

Server is a rewritten file: excluding whole untouched files would have been an
incorrect explanation. The oracle additionally displays other baseline diagnostics
that were never counted and retains stale-prose/unresolved-namespace advisories.

Post-real-cold papercut output (tracked log churn is advisory):

```text
papercut-oracle  root=/var/tmp/forge/plan2/build/cc-r3
  destinations=20  callers=6  pre-split defs=141  host :require indent=3

1. docstring-clone .......................... 0
     (every destination ns docstring is unique)

2. clj-kondo (new findings relative to HEAD)
     baseline findings: 24 [advisory, not counted]
       src/cfp_scheduler_killer/server.clj:56:5  namespace ring.util.response is required but never used
       src/cfp_scheduler_killer/server.clj:2226:14  unused binding event
       test/cfp_scheduler_killer/authz_security_test.clj:137:7  Redundant let expression.
       test/cfp_scheduler_killer/board_test.clj:86:21  Single argument to str already is a string
       test/cfp_scheduler_killer/board_test.clj:208:23  unused binding gene
       test/cfp_scheduler_killer/board_test.clj:321:23  unused binding gene
       test/cfp_scheduler_killer/board_test.clj:381:19  Unresolved namespace starfederation.datastar.clojure.api. Are you missing a require?
       test/cfp_scheduler_killer/comms_test.clj:61:23  unused binding submission
       test/cfp_scheduler_killer/comms_test.clj:140:13  unused binding ls
       test/cfp_scheduler_killer/comms_test.clj:328:17  unused binding event
       test/cfp_scheduler_killer/polish_test.clj:122:19  Unresolved namespace hiccup2.core. Are you missing a require?
       test/cfp_scheduler_killer/replay_test.clj:19:23  Unused import LocalDate
       test/cfp_scheduler_killer/replay_test.clj:224:23  unused binding a
       test/cfp_scheduler_killer/replay_test.clj:254:17  unused binding event
       test/cfp_scheduler_killer/reviews_test.clj:13:14  namespace cfp-scheduler-killer.seed is required but never used
       test/cfp_scheduler_killer/reviews_test.clj:222:9  unused binding zero
       test/cfp_scheduler_killer/server_test.clj:97:5  redundant do
       test/cfp_scheduler_killer/server_test.clj:168:5  redundant do
       test/cfp_scheduler_killer/server_test.clj:640:9  unused binding event
       test/cfp_scheduler_killer/server_test.clj:673:9  unused binding slug
       test/cfp_scheduler_killer/server_test.clj:674:5  redundant do
       test/cfp_scheduler_killer/sinks_test.clj:13:14  namespace cfp-scheduler-killer.exports is required but never used
       test/cfp_scheduler_killer/sinks_test.clj:559:9  unused binding event
       test/cfp_scheduler_killer/store_test.clj:6:14  namespace clojure.java.io is required but never used
     unused-import          0  (0 in split-touched files)
     unused-namespace       0  (0 in split-touched files)
     unused-referred-var    0  (0 in split-touched files)
     unresolved-symbol      0  (0 in split-touched files)
     unresolved-namespace   2   [advisory]  (2 in split-touched files)
       src/cfp_scheduler_killer/views/dashboard.clj:44:30  Unresolved namespace cfp-scheduler-killer.store. Are you missing a require?
       src/cfp_scheduler_killer/views/review.clj:421:18  Unresolved namespace cfp-scheduler-killer.store. Are you missing a require?

3. continuation-misalignment ................ 0  (lines)
     (no continuation line left at its pre-split column)

4. require-layout
     require-layout-dest    0  (files)
     require-layout-caller  0  (files)
     ns-whitespace-line     0  (lines)

5. stale-prose .............................. 3   [advisory]
     src/cfp_scheduler_killer/server.clj:442 (string)  `views/event-marquee` the page was first painted with, pushed down THIS
     src/cfp_scheduler_killer/server.clj:1772 (string)  "Everything `views/form-builder-page` needs. `extra` carries the state of a
     test/cfp_scheduler_killer/replay_test.clj:286 (comment)  ;; views/dev-strip) on 2026-08-09; the log page still carries it always.

6. tracked-log-churn ........................ DIRTY   [advisory, not counted]
     M 00SERVER-LOGS.txt

  CHECK                         COUNT  UNIT      COUNTED
  ------------------------------------------------------------
  docstring-clone                   0  files     yes
  unused-import                     0  findings  yes
  unused-namespace                  0  findings  yes
  unused-referred-var               0  findings  yes
  unresolved-symbol                 0  findings  yes
  unresolved-namespace              2  findings  advisory
  continuation-misalignment         0  lines     yes
  require-layout-dest               0  files     yes
  require-layout-caller             0  files     yes
  ns-whitespace-line                0  lines     yes
  stale-prose                       3  hits      advisory
  tracked-log-churn                 1  files     advisory
  ------------------------------------------------------------

PAPERCUTS: 0
```

## Repository gate

Final `make test` exited 0. JVM fast/integration: **786 tests / 9,983 assertions**,
zero failures/errors, zero isolation violations across 59 namespaces. Babashka:
**873 tests / 7,511 assertions**, zero failures/errors. The Python attribution
regression, operation oracle, sentinel audit, recovery/temp-path checks and
repository hygiene pass. The standing battery-fresh receipt is accepted; no new
full performance battery is claimed. Complete log:
`/var/tmp/forge/plan2/cellC/astra-r3-make-test-green.log`. The two preceding failed
attempt logs are retained beside it. Production behavior was unchanged after the
real cold/warm fixture calls; subsequent changes repair test wiring and annotations.
Touched-file lint: **0 errors / 0 warnings** across all nine changed Clojure files. Formatting was applied to changed files; unrelated existing layout
in the isolation registry was preserved. All fixtures and raw gate logs remain
under `/var/tmp/forge/plan2/cellC/` and `/var/tmp/forge/plan2/build/cc-r3` for review.
The two owned JVMs remain available; their identities are recorded in
`astra-r3-owned-jvms.edn`. No push or main update is authorized or performed.

Gate completion recorded 2026-09-08T01:09:16.546638+00:00; elapsed since
image-harness setup: 2133.0 s (35.55 minutes), including all authoring
and gate retries. The logged iteration sum remains 95.121 s; neither number is
substituted for the other.
