# Unify Claude and Codex Skill Packaging

**Status:** Closed on 2026-08-06
**Severity:** P0 release blocker

## Evidence

The measured canonical Codex skill is
`skills/clj-surgeon/SKILL.md`: 89 lines and 563 words. README tells Claude Code
to read root `skill.md`: 249 lines and 1,512 words. `make install` installs the
CLI and Codex skill only. There is no tested Claude installation target.

The compact-skill wall result therefore cannot be generalized to Claude.

## Required Outcome

Choose one authoritative skill source and generate, link, or validate every
agent-specific entrance from it. If Claude and Codex genuinely require
different wrappers, keep their shared contract in one source and test the
intentional differences. Make installation and README instructions name the
exact artifact each agent receives.

## Tests and Verification

- Installation tests cover Claude and Codex destinations without replacing
  unrelated user files.
- Drift tests assert the same X-ray input, plan/apply, refusal, and receipt
  contracts across both installed surfaces.
- Clean-agent benchmarks hash and use the installed artifact, not an ad hoc
  copied substitute.
- Progressive references remain discoverable for advanced operations.

## Done When

A user can install once, identify exactly what Claude and Codex loaded, and
both clean-agent suites pass against those installed bytes.

## Resolution

`skills/clj-surgeon` is the authoritative package. The repository-native
Claude entrance and root compatibility entrance have drift tests against that
package. `make install` creates one content-addressed, read-only package and
points both the Codex and Claude destinations to it. Each destination has an
owned installation receipt. The installers refuse unrelated files, links, and
receipts.

Installation tests cover stable copies, development links, custom homes,
unrelated destination refusal, shared contract text, progressive references,
and byte-identical installed Claude and Codex surfaces. The Codex and Claude
benchmark targets use the installed package and record its hash. The original
line and word counts in the evidence section describe the pre-fix state; they
are retained as the failure evidence.
