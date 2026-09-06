# Saved mission usage summary

A caller of `mission show` can inspect token and provider-reported cost totals in
`:receipt :usage` without parsing transport artifact files. The executor derives
this summary from its actual completed transport snapshot before committing a
winner. It preserves the snapshot across a later commit refusal. A refusal before
any snapshot is available reports `:status :unavailable`, not free execution.

The summary admits at most five completed candidates and two provider attempts
per candidate. Nested `:attempts` replace repeated wrapper usage. For legacy flat
records, a finite nonnegative `:request_wall_s` is evidence of a dispatched request;
an explicit false `:request_started` always wins. Nonstarted attempts are counted
separately. Cancelled indices already in completed are excluded from cancelled
counts. An unfinished or evidence-incomplete candidate contributes an unknown
candidate, whose actual number of provider attempts is unknown.

Each metric has `:known-total` (nil if none) and `:unknown-attempts`. Reasoning is
reported separately as a subset of completion; it is never added to completion.
Cost uses only finite nonnegative values marked `provider-reported`, without
pricing estimates or display rounding. Zero is a known value. A `:partial` summary
retains observed totals alongside unknown counts and is not a complete bill.
`:complete` describes accounting coverage, not task correctness or provider quality.
Invalid bounds/identities produce a typed unavailable summary. Raw content and
credentials never enter this projection. Compact display retains its existing
size guard; `--full` remains the authoritative saved receipt view.

## Witnesses and verification

`mission-usage-test` has seven fast tests covering the retained T1 scalar receipt,
fallback wrapper duplication, cancellation overlap, unknown/nonstarted evidence,
invalid values and real zero, input bounds, and compact display. The two battery
`mission-usage-executor-test` tests mock all transport/proof/file side effects and
cover success, rejection, post-close commit failure, and pre-transport refusal.
They make no provider calls. Combined focused JVM gate: 9 tests, 47 assertions,
zero failures/errors. The first test load failed because the new summary namespace
had not yet been implemented; this is not a behavioral field-failure replay.

Lane registration is delegated to the parent integration owner. New feature
helpers/tests are native-ineligible for the mechanical executor. Existing source
owners were inspected through Surgeon; known literal receipt/require edits used
native patches under the working-tree skill. A guessed test-owner read refused
and returned the actual owner list; the subsequent named read succeeded. No
unsupported structural operation was invented. Test events were isolated under
`/var/tmp/forge/mission-usage/events.jsonl`.

The wider display regression needs JVM dependencies: its BB run reported three
missing-nREPL load errors (retained in `display-gate.txt`), not passing evidence.
The corrected JVM run, including both new namespaces and the existing display
namespace, passed 16 tests / 87 assertions (`combined-gate.txt`). Verification:

```sh
CLJ_SURGEON_EVENTS_FILE=/var/tmp/forge/mission-usage/events.jsonl \
TMPDIR=/var/tmp/forge \
JAVA_TOOL_OPTIONS='-Xms64m -Xmx512m -XX:ActiveProcessorCount=2 -Djava.io.tmpdir=/var/tmp/forge' \
SLOT_OWNER=astra /home/forge/bin/suite-run clojure -M:clj-surgeon/test-deps -e \
'(require (quote clj-surgeon.mission-display-test) (quote clj-surgeon.mission-usage-test) (quote clj-surgeon.mission-usage-executor-test)) (let [r (clojure.test/run-tests (quote clj-surgeon.mission-display-test) (quote clj-surgeon.mission-usage-test) (quote clj-surgeon.mission-usage-executor-test))] (System/exit (+ (:fail r) (:error r))))'
```
