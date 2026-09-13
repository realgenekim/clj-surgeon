# Data-not-code round 2: phase-one review

Recorded 2026-09-12T20:32:30Z. Starting HEAD:
`81328496d25916b096e29764128af81276360a34` on `fable/data-not-code-local`;
the worktree was clean.

Verdict: **REVIEW-REQUIRED; implementation unfinished.** Gene's destination
envelope contract resolves the previous STOP-CONTRACT. No further product
contract change is requested. The concrete review artifact is
[envelope-design.md](envelope-design.md).

The supplied repository AGENTS.md, under Linked-Intent Development, requires:
“Stop after each phase for user review.” Item 1 changes its explicitly scoped
operation-algebra and intent-transaction files. This handoff stops at the
phase-one design review before requirements, red witnesses, and implementation.
The linked-intent-testing skill was found through the installed symlink at
`/home/forge/.claude/skills/linked-intent-testing/SKILL.md` and read. That skill
provides the promise/registry/witness method; the phase-stop requirement comes
from AGENTS.md, not from the skill.

## Findings

- The existing trusted context schema and CLI/MCP context values lack the
  destination envelope; these are the intended authority entry points.
- The artifact boundary validates roots and resolves directories and targets,
  but currently has no independent destination authority.
- Alias telemetry creates its directory before checking ledger symlinks.
  Concrete directory and ledger admission must precede that creation.
- Consumers that construct filenames after `directory` need a final-target
  audit. Admitting a parent alone cannot establish final-file containment.
- The dependency design keeps environment/passwd/filesystem discovery out of
  the pure operation algebra. Coverage remains the shared artifact boundary.

## Verification and dogfood

Read-only inspection and documentation edits only. No production source or test
edits, red or green test runs, lint, suites, prewarm gates, probe measurements,
server starts, shared installs, pushes, tags, or records writes occurred.
nREPL discovery found only an unrelated workspace server; it was not used.
The lease-path listing showed no entries at inspection time; no JVM was started.

| Source edit | Intent | Mechanism | Refusal | Repair text sufficient |
|---|---|---|---|---|
| None | Phase-one design review | Native reads and documentation patch | No Surgeon call | Not applicable |

The enclosing Git commit contains only these round2 documents. There are no
implementation commits and no demonstrated safety, wall-time, or sublime-score
improvements to report.

## Least sure, disagreements, and owed work

Least sure: whether the new build instruction intends to waive the separately
supplied per-phase review requirement. No waiver has been assumed.

Disagreements: none with the launcher product contract. Candidate selection
must remain separate from authority, and failed telemetry cannot erase a
previously completed source mutation.

Owed: phase-one review; item-1 requirements, committed red witnesses, source
implementation, refusal registration and green publication evidence; items
2–4 red/fix/green in order; item-5 six-task preregistration with Fable's and
Astra's bets; item-6 lint, focused tests, lease-gated test-fast and prewarm,
and final implementation report. The binding brief remains unfinished.
