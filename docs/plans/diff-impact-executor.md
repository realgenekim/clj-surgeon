# Diff-impact executor parity (inb-b142af, inb-fbd110)

Use the existing battery-parallel-runner preparation, runtime assignments,
partitioning, admission, prerequisites, child launchers and result validation.
The only new execution input is an explicit nonempty lane subset with a SHA-256
of the selection receipt. Reject malformed, duplicate and out-of-suite inputs
before effects. Partial receipts retain that subset and provenance and cannot
serve either full-suite validation or battery freshness. Preserve full defaults.

Fixed-point takes an optional fast/all scope (default both): fast intersects the
selection with the fast manifest; all partitions every selected namespace by its
own cadence, using the existing bb suite for unlaned members. Unadmitted members
refuse explicitly; none silently disappear. Record independent scoped results,
wall to verdict and per-lane receipts. Empty/HOLD behavior is unchanged.

Commit failing pure admission/census/freshness/routing witnesses before code.
Retain a pre-change real isolation failure. Compare one frozen fixture's selected
fast subset with a full fast run on the same tree, including per-namespace verdicts
and measured walls, without asserting speed. Reuse registered test namespaces.
Run requested JVM/bb tests with 1024 MiB, lint, census regeneration and protected
ledger hash verification. No make test or test-battery. All phases have standing
approval. This is correctness evidence, not the five-diff repaired replay.
