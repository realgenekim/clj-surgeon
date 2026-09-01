---
parent: worktree-lifecycle-design
requirements: worktree-lifecycle-specs
---

# Worktree Lifecycle Pre-Red Edge Audit

This audit resolves six implementation-defining ambiguities before the frozen
red phase. It does not widen the ratified MVP or authorize a real worktree
close.

1. A reviewed plan binds lifecycle-lease prestate `absent` plus the expected
   lease identity derived from that plan. Apply creates that lease after exact
   prestate validation; the plan does not claim that a not-yet-created lease
   already exists.
2. Supacode terminal state is outcome-preserving. A target that began
   authoritatively absent remains `absent`; a presented target must end
   `archived`.
3. Strict realpath equality applies to existing targets. A Supacode-only
   missing identity has no real path, so its strictly decoded lexical absolute
   path remains audit-only as `missing-prunable`.
4. The monotone journal records parking transitions as `:not-applicable` for
   landed and negative-experiment outcomes. Recovery never infers a skipped
   state.
5. A parked issue revision may advance only through the controller's
   byte-identical idempotent append for the same plan. Any unrelated issue
   change is drift and refuses.
6. Commit and tree identifiers use the repository-declared Git object format.
   No schema or test assumes 40-character SHA-1 identifiers.

These resolutions preserve the approved single-target trade-off, branch
non-deletion law, and hard separation between read-only audit and reviewed
apply. `WTL-CLI-004` is marked deferred and remains outside the MVP red/green
gate because global skill publication follows the real-corpus repository
trial.
