# Diff-impact executor parity (inb-b142af, inb-fbd110)

Use the existing battery-parallel-runner preparation, runtime assignments,
partitioning, admission, prerequisites, child launchers and result validation.
The only new execution input is an explicit nonempty lane subset with a SHA-256
of the selection receipt. Reject malformed, duplicate and out-of-suite inputs
before effects. Partial receipts retain that subset and provenance and cannot
serve either full-suite validation or battery freshness. Preserve full defaults.

Fixed-point takes an optional fast/all scope (default both): fast intersects the
selection with the fast manifest; all partitions every selected namespace by its
own cadence, using the existing observed runner for registered dedicated members. Dedicated
work is serial, analyzer work retains its mission, and memory work resolves and
holds the same exclusive lock as its Make entrance. Unknown members refuse
explicitly; none silently disappear. Record independent scoped results,
wall to verdict and per-lane receipts. Empty/HOLD behavior is unchanged.

Commit failing pure admission/census/freshness/routing witnesses before code.
Retain a pre-change real isolation failure. Compare one frozen fixture's selected
fast subset with a full fast run on the same tree, including per-namespace verdicts
and measured walls, without asserting speed. Reuse registered test namespaces.
Run requested JVM/bb tests with 1024 MiB, lint, census regeneration and protected
ledger hash verification. No make test or test-battery. All phases have standing
approval. This is correctness evidence, not the five-diff repaired replay.

### Total selected membership (IMPACT-EXEC-01)

Before fast/all projection, classify every selected namespace against the on-disk
inventory and admitted manifest: fast-member; other-lane-member with named lane;
unregistered/renamed; load-excluded; or bb-ineligible. An existing admitted JVM
or dedicated lane takes precedence over BB incompatibility. Missing on-disk
namespaces cannot inherit stale manifest admission. Only explicitly admitted
BB-only members use the BB suite.

Report the complete classification table in successful and refused observations.
Fast runs fast members and reports other-lane members as belonging to all.
Both scopes refuse all remaining members together with error type
`selected-namespace-unclassified` and one `selected-namespace-unclassified <ns>`
reason per namespace, before any child or output directory. An explicit
`:selection-exclusion {:reason <classification>}` on that namespace in the
selection receipt accounts for an excluded member; mismatched or absent reasons
refuse. Require-edge selection reasons never count as exclusion authorization.
The oracle is `fast-scope-total-membership`, including Sol's exact renamed-away
invocation. Replace the contradictory unknown-fast `{}` expectation in the same
committed red as the stronger oracle. Standing approval covers all phases.
