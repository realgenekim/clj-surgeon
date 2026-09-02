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

## Missing-registration resolution audit

Gene ratified the missing-registration LLD and EARS requirements on
2026-09-01. This second pre-red audit resolves the implementation seams for
`WTL-INV-008` and `WTL-PRUNE-001..010`. It does not authorize pruning a real
registration.

1. The ratified `:clj-surgeon.worktree-lifecycle-snapshot/v1` shape supersedes
   the shorter, unpublished prototype shape. There is no compatibility reader
   for prototype artifacts. Every newly compiled plan binds the ratified
   lexical path, path state, real path or nearest-parent proof, and the exact
   full registration row. Previously persisted prototype plans refuse closed
   validation rather than being upgraded implicitly.
2. Request dispatch is by the exact schema before field validation. A
   registration-prune request never passes through ordinary close outcome or
   handoff validation, and an ordinary close request cannot borrow prune
   fields. A prune plan has `:operation :prune-missing-registration`, no
   `:outcome`, and handoff plus both parking transitions are explicitly
   `:not-applicable`.
   The HLD's `clean-safe` automation rule governs ordinary close only. The
   only automated `missing-prunable` case is the narrower, separately proved
   Git-registered/path-absent prune operation; every other
   `missing-prunable` row remains audit-only.
3. Filesystem authority is a no-follow component walk from the lexical target
   toward the root. Any object at the final path, including a dangling
   symlink, refuses. Every existing component must be non-symlink; the nearest
   existing ancestor is captured by canonical path plus device and inode when
   available. Unavailable lookup data produces `:unknown`, never `:absent`.
4. A missing registration derives its tree from the registered HEAD in the
   controller repository. It never runs status, submodule, or checkout-tree
   commands in the absent target. Status and removal preflight are the literal
   value `:not-applicable`; they do not trigger the ordinary dirty-blocked
   rule.
5. Supacode ownership is closed and fail-closed. A successful stable bracket
   must state the target absent or expose its exact identity, status, focus,
   and live fact. Focused, pinned, live, or contradictory `:main` evidence is
   active; an unknown live fact refuses prune planning. The bracket admits
   exactly zero matching rows (absent) or one exact non-main, unpinned,
   unfocused, non-live row; duplicates refuse. An absent surface has
   deterministic absent pre/post-state and requires no archive command.
6. Preservation authority joins one configured remote name and one effective
   fetch URL digest to one exact `ls-remote` row. Symbolic-only rows are never
   endpoints. Annotated tags use the exact peeled commit; lightweight tags and
   branches use the advertised object. The local branch must still name the
   registered HEAD, and the endpoint commit must exist locally before either
   equality or ancestry can pass.
7. The Git compatibility witness is executable and version-bound. A supported
   Git must remove an absent registered path with exactly `git worktree remove
   <path>` and must refuse a reappeared file, directory, dangling symlink, or
   lock while preserving a peer registration. Failure of any witness refuses
   before Supacode mutation. Global prune, force, and direct administration
   deletion have no fallback entrance.
   The first implementation supports only Git versions explicitly named by
   the executable fixture manifest; an unlisted version refuses. Adding a
   version requires running the same compatibility matrix, not a prose claim.
8. `prepared` is the external-effect boundary. If the selected registration is
   already absent before the journal durably reaches `prepared`, apply returns
   typed `:already-resolved`, creates no lease or receipt, and does not touch
   Supacode. If it disappears after `prepared`, recovery may converge only
   after all remaining path, branch, preservation, ownership, and Supacode
   postconditions pass; the receipt then records
   `:effect-observed :controller-or-external`.
   A convergence path may archive the one exact planned Supacode row after
   `prepared`; a planned-absent row must remain absent.
9. Journal identity includes immutable operation kind. A close journal cannot
   resume a prune plan and a prune journal cannot resume a close plan. Exact
   replay returns the immutable receipt only when plan hash, operation kind,
   absent path, absent registration, local branch, preservation endpoint, and
   Supacode terminal state remain exact; any recreated path refuses
   `:path-reused`.
   Outcome-, Beads-, and handoff-specific `WTL-APPLY-*` clauses remain scoped
   to ordinary close. Prune reuses only the mechanical lock, lease, canonical
   storage, monotone journal, archive/restore, recovery, and immutable-receipt
   envelope named by `WTL-PRUNE-005..010`; existing close witnesses do not
   count as prune witnesses.
10. A prune receipt reports only `:registration-pruned`, the before row, the
    preservation proof, and the observed-effect class. It never asserts
    landed, negative-experiment, or parked meaning. Branches, tags, refs,
    breadcrumbs, issues, archives, and retained evidence remain untouched.
    Rebase-landed patch equivalence remains design-only under `WTL-SEAL-008`;
    field witness `c44ac759` to `main` `64eac2ee` is retained for that later,
    separately ratified algorithm.
11. Peer registrations are diagnostics during planning and unrelated drift
    before the effect. The exact remove command is nevertheless bracketed by
    registration snapshots: every peer present immediately before the command
    must be byte-identical immediately afterward. Peer loss or change inside
    that bracket is a typed partial failure and can never produce success.
12. Ownership is cross-kind. Any unconsumed close or prune plan for the same
    target fingerprint blocks a new plan. A prune plan binds
    `:handoff :not-applicable` only after proving no target handoff exists;
    lease prestate is absent, and recovery accepts only the exact lease derived
    from that prune plan.

The frozen red phase must witness each resolution at the pure boundary and
must use only controller-owned temporary repositories for executable Git and
recovery tests.
