---
parent: worktree-lifecycle-design
requirements: worktree-lifecycle-specs
---

# Worktree Lifecycle Intent Consistency Report

## Reviewed authorities

- `docs/high-level-design.md`, section “Close Terminal Worktrees Without Erasing Experiments”
- `docs/intent/worktree-lifecycle/worktree-lifecycle-design.md`
- `docs/intent/worktree-lifecycle/worktree-lifecycle-specs.md`

## Coverage

- `WTL-INV-*` covers the Git, Supacode, remote, path, and closed-classification snapshot.
- `WTL-HAND-*` covers active owner handoff, legacy admission, and the lifecycle lease.
- `WTL-SEAL-*` covers landed, negative-experiment, and parked evidence without deleting their durable records.
- `WTL-PLAN-*` covers the closed dry-run request, exact fingerprint, canonical persisted plan, privacy, and replay authority.
- `WTL-APPLY-*` covers journaled archive, the post-archive safety gate, non-force removal, restoration, final receipts, and recovery.
- `WTL-CLI-*` covers the repository entrances and the later global-skill delegation boundary.

Every HLD and LLD behavior in the first slice has at least one stable requirement ID. Branch, tag, and remote-ref retirement; automatic pruning; cross-repository receipt projection; expired-parking escalation; and global skill publication remain explicitly deferred and receive no implementation requirement in this leaf.

## Contradiction check

No contradiction was found between the approved HLD, the leaf design, and these requirements. In particular:

- `negative-experiment` preserves both a human breadcrumb and exact machine pointers before the execution room is removed.
- Git registration, Supacode presentation state, remote durability, and Beads ownership remain separate authorities.
- An active Git lock is transferred through an explicit handoff and lifecycle lease; unlock state alone never grants removal authority.
- Apply removes one worktree only, without force, only after a reviewed plan and a second post-archive safety check.
- Branch deletion remains outside this leaf even after successful worktree removal.

## Context-free and testability check

Each requirement names the lifecycle subject, triggering condition, required result, and refusal boundary without relying on conversation history. Every `[ ]` item is intentionally unimplemented and is suitable for a direct red witness in the next LID phase after Gene approves this specification.
