# Isolate Installed Artifacts from the Active Checkout

**Status:** Open
**Severity:** P0 release blocker

## Evidence

`~/bin/clj-surgeon` adds this repository's live `src` directory to its
classpath. `~/.codex/skills/clj-surgeon` is a symlink into this repository.
Switching branches therefore changes the effective installed CLI and skill
without running `make install`.

The audit's instruction not to install experimental candidates does not
provide isolation on this machine.

## Required Outcome

Choose and document one model:

- immutable copied/versioned installation;
- explicit development-link installation whose branch-coupling is prominent;
  or
- separate stable and development commands/skill destinations.

Benchmarks must use commit-specific wrappers and skill copies, and release
claims must distinguish those from the developer's live-linked entrance.

## Tests and Verification

- Installation receipts show source commit/hash and destination.
- Switching the repository branch cannot silently change a stable install, or
  a dedicated test proves and labels the development-link behavior.
- Install refuses unsafe replacement of unrelated files.
- Clean benchmarks resolve the intended commit-specific CLI and skill.

## Done When

“Installed version” has one unambiguous, testable meaning and a branch switch
cannot silently invalidate benchmark or release isolation.
