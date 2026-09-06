# Astra: events/path hardening merge receipt

Recorded 2026-09-06T02:46:46.860169+00:00. Base 6e10fdf1; incoming
Fable b4ec5548012895fe919d67defd4821caf5d8e4b2. Merge on an isolated branch.

The merge preserves all raw-form, executor and mission enrollments. Source
census: 46 fast + 5 integration + 18 battery = 69 namespaces; 1234 tests,
921 original + 313 adopted. telemetry-events grows from 9 to 16 tests.

The semantic conflict is resolved in favor of CLOSED optional mission fields,
as directed by Astra lead: Fable's arbitrary scalar pass-through could carry
raw source or a credential outside known regexes. Unknown scalar keys and
nested values are dropped. Approved mission enums/counts survive, with Fable's
scrubbing and UTF-8 bounds. Provider/upstream retain fixed admitted enums;
unapproved values are null. Cost/token fields remain: token counts are
nonnegative integers bounded by Long/MAX_VALUE; USD is finite and nonnegative;
only the actual `provider-reported` cost_source is admitted. Raw M-id retention
keeps the existing 12-digit bound; other ids are hashed. Incoming witnesses
were reconciled to these contracts, including an unknown-scalar refusal.
Fable's parent-permission tightening and final-line byte guards remain.

Verification: offline bin/typist-run-test passes all checks after the apparatus
repair below. Merged mission-events, telemetry-events and lane-manifest suites
pass 48 tests / 287 assertions, zero failures/errors. Changed Clojure formatting
and git diff --check pass. No performance claim is made by this merge.

## Offline test breach and ratchet

The incoming test claimed no network but its NW refusal fixture assumed the
worktree was outside /var/tmp/forge. In this mandated scratch worktree it was
inside, so the test admitted a real Codex warm session:
01a07495-27c3-70e2-bf12-d8a521249e84. The accidental receipt is retained under
/var/tmp/forge/astra-events-hardening-merge-fx/target/fence-probe-fx/
NW-scope-roots-1788662326-1005197-0/receipt.edn. It records warmup_wall_s 12.199;
usage is unknown here and this is excluded from every performance cohort.
No relevant process remained when checked. The visible run artifacts are in
this isolated worktree's ignored target tree. The unsandboxed child's absence
of writes elsewhere has not been independently proved and is not claimed.

With lead approval, runner subprocess tests now install a Python audit guard
before loading the runner: all descendant subprocess launches, socket activity
and real credential-file opens fail closed. An explicit attempted codex launch
witness proves the guard. The NW path witness uses a test-local scratch root,
independent of where the worktree lives, and mocks only proof-command lookup.
It still invokes actual main() in a subprocess and verifies exit 4 before any
child launch. No production runner behavior was added for this test repair.

Independent Fable fence review remains external to this merge receipt.

## Astra third-round integration — 2026-09-06T03:09:07.463888+00:00

Merged f0c180da runner hardening and unknown-cost accounting. Retained our closed telemetry field schema and its stricter numeric bounds: incoming extra-name normalization and pass-through are superseded by rejecting unknown fields altogether, as agreed with Fable. Did not add an open extras surface or claim a dropped-fields counter. Existing mission and lane witnesses retained. Both offline guards remain: production typed refusal and test bootstrap audit guard. The bootstrap witness now probes os.system directly because the production wrapper correctly refuses Codex first. Incoming session witness compared most-recent mtime and failed on active seats appending older rollouts; changed to the set of session file identities. Before this correction the suite failed, after it all offline checks pass; no new provider calls. Receipt: /var/tmp/forge/astra-telemetry-fx/r3-runner-merge.log.
