# Data-not-code: approved build contract

Parent: [approved envelope design](../round2/envelope-design.md), itself linked
to the high-level design and operation algebra. The user pre-approved the
remaining requirements, red, implementation, green and gate phases for items
1–6. This document is the implementation plan and atomic acceptance matrix.

## Envelope admission

- DATACODE-ENV-001: When a shared artifact consumer chooses a final target,
  it shall refuse a resolved destination outside the trusted envelope before
  creating any directory or publishing any bytes.
- DATACODE-ENV-002: When no launcher value is supplied, admission shall use
  the bounded policy default (disk temp, passwd seat state, startup workspace).
- DATACODE-ENV-003: When a request carries destination authority, its decoder
  shall refuse that field; trusted narrower authority shall remain narrower.
- DATACODE-ENV-004: When publication succeeds, its receipt shall identify the
  admitting envelope.

Misreadings to exclude: admitting the parent grants arbitrary filenames;
canonicalizing only existing files suffices; request workspace widens startup
authority; a missing envelope means unrestricted writes; ledger failure erases
an already committed operation. Witness matrix: owned disk outside, absent
parents, ancestor symlink, final file symlink, authorized publication, default,
narrow launcher, unknown request field, ledger directory and file escapes.
Resolution follows existing ancestors and retains the absent suffix. The
coverage is the shared artifact boundary, not every writer or filesystem races.
Landlock is unchanged.

## Subsequent leaves, in order

- DATACODE-SCI-001: When the sandbox denies a symbol, classification shall
  use refusal data independently of exception prose on JVM and Babashka.
  Witnesses: three existing xray cases plus changed presentation text.
- DATACODE-ROWS-001: When the statistical fold publishes namespace evidence,
  it shall refuse missing receipts. The manifest shall consume those exact
  validated rows; declared policy remains policy. Existing statistical oracles
  retain their expected values.
- DATACODE-SLEEP-001: When checking temporal exemptions, the checker shall
  identify owner, ordinal and purpose, deriving the line only for diagnostics.
  Unrelated line insertion preserves acceptance; changed call, owner, count or
  purpose fails by name.

No probe experiment runs in this build. Preregistration fixes six edit/verdict
tasks, native/probe commands, matched order, three repetitions, complete wall,
success/refusal/unknown accounting, falsifiers, and both authors' bets.

## Verification and delivery

Red witnesses precede each implementation; logs record actual outcomes. Format,
serialized lint and focused witnesses precede one test-fast and one prewarm
(one repair and final retry permitted), each only with the suite lease free.
REPORT.md and report.edn retain source-edit dogfood, measurements, uncertainties,
disagreements and debt. Commit named files on the owned branch, report last;
never push. No performance claim without the preregistered experiment.
