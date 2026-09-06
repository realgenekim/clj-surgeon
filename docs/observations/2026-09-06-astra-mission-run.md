# Astra: one-process owner-form mission entrance

Recorded 2026-09-06T02:00:07.191528+00:00.

The first keeper had a sub-three-second executor surrounded by two cold command
starts. `bin/mission run` targets that process boundary: one owner_forms proposal
is durably saved and, only if ready, applied by id in the same JVM. It forwards
neither the original request nor supplied profiles into apply; the saved mission
retains authority. Existing propose/apply and all other mission verbs remain.

This is a predicted mechanism, not a speedup measurement. No live provider or
performance run was made in this lane. This is an optional explicit write
command; callers wanting review use propose first. `help run` and `run --help`
name that distinction and link the owner_forms request instead of presenting a
helper-extraction spec as usable input.

Verification: the eight new tests first produced 30 failures, zero errors on the
old code. After implementation, the new, legacy mission and lane-manifest suites
pass 60 tests / 465 assertions, zero failures/errors. They cover one plan and
one execution, saved-plan authority before execution, typed blocked/refused
stops, unchanged source on a real CLI refusal, EDN and exit status, launcher
routing to exactly one JVM, parsing and both help entrances. Providers are
replaced in successful executor tests. Formatter ran on changed Clojure files;
unrelated pre-existing formatting differences were restored. `bash -n
bin/mission` and `git diff --check` pass.

Command: `SLOT_OWNER=astra ~/bin/suite-run clojure -J-Xmx512m
-M:clj-surgeon/test-deps -e "(require 'clj-surgeon.mission-run-test
'clj-surgeon.mission-test 'clj-surgeon.lane-manifest-test)
(let [r (clojure.test/run-tests 'clj-surgeon.mission-run-test
'clj-surgeon.mission-test 'clj-surgeon.lane-manifest-test)]
(System/exit (if (zero? (+ (:fail r) (:error r))) 0 1)))"`.

Broader repository gates, live dogfood of this entrance, matched timing, and
independent executed fence review remain root-lane integration work. This
checkpoint does not claim production completion or comparative speed.
