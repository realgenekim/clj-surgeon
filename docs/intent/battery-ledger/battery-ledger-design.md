# Battery evidence ownership

Parent: [high-level design](../../high-level-design.md).
Specifications: [stable promises](battery-ledger-specs.md).

The receipt ledger is reviewed history. A normal test invocation prints its
candidate entry without mutating that history. The append command gates its
write on the exact environment value `1`; both pass and fail receipts follow
the same rule. The low-level append primitive remains available to fixtures.

Scheduling measurements are mutable seat state. Resolve their destination via
`probe-state/state-root` and append `battery/namespace-walls.edn`. Read the
tracked seed only when that destination is absent, including its per-Var costs.
Subsequent complete runs update state and preserve earlier per-Var measurements.
Missing or unreadable measurements affect scheduling estimates, not acceptance.

The census is a test oracle. Ordinary make gates remain unable to rewrite it.
Only the explicit regeneration target removes make's environment markers for
the documented direct invocation. Compare parsed sets before writing, report
`+N/-M`, and refuse deletion or rename with the missing names and unchanged
bytes. The target propagates clojure.test failures as a nonzero exit.

Misreadings: a passing check is not permission to append; `true` is not `1`;
state does not move the receipt ledger; seed fallback is not permission to
rewrite the seed; an unchanged total does not make a census rename safe.
