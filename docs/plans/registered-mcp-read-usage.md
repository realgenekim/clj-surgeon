# Explicit HTTP wrapper coverage in agent usage studies

The retained 2026-09-05 04:55:58–05:55:58 UTC client window contains
literal `tools.exec_command({cmd: ...})` invocations of a Python HTTP MCP
wrapper, but the collector counts only direct registered MCP methods and the
Surgeon CLI. Zero counted client calls was therefore incomplete coverage.

This repair adds opt-in `--registered-mcp-read-wrapper PATH`. The exact source
must hash to the inspected single-call wrapper contract. Its default operation
is inspect_clojure; explicit relation_census and feature_thread are admitted.
This is explicit wrapper registration, not a guess from a script basename.
The pure classifier recognizes JSON double-quoted literal cmd as the first
property of an actual tools.exec_command call, with a complete supported argv.
It excludes JS strings/comments, unknown wrappers, dynamic commands, shell
composition, unknown flags, invalid ports, and duplicate options. No file
arguments or wrapper receipts are opened during classification. No wrapper
paths, request arguments, source, or transcript text enter the receipt.

Counts are invocation attempts, not proof that initialize succeeded or an RPC
reached the server. Counted outer-action wall includes Python startup, MCP
initialization, orchestration and potentially batched actions; it is not direct
MCP call wall. Completed shell-item clocks and unknown wrapper shapes remain
incomplete. Current wrapper bytes establish the registered contract, not an
attestation of historical wrapper bytes. Existing service aggregation is
unchanged. Do not combine service and client counts into an additive total.

The named self-test covers the observed default call, repeated literal calls,
explicit route counting, and negative mention/composition/registration cases.
The external fail-first witness preceded implementation; the existing paved
self-test must pass. Recollect the identical bounds once, retain the original
receipt as superseded, and report the corrected receipt as counting authority.
This bounded repair does not claim complete arbitrary Python/JavaScript tracing.

## Explicit public quality wrapper extension

The second field failure is the two 01 public quality runner invocations.
`--registered-public-wrapper PATH` verifies exact inspected runner SHA and both
case request SHA identities, plus tools/call and helper_extraction request
shape. Only the observed fixed `env TMPDIR=... python3 PATH CASE` argv is
recognized. The 02 runner, arbitrary env settings, extra argv and unknown cases
are excluded. Counts remain runner invocation attempts: no result, profile
success or RPC completion is synthesized. Current file identities are not
historical source attestation. The receipt identifies the new collector source
SHA and registration hashes without emitting paths or request content.

The helper_extraction name is a mutation in registered method route phases and
completed MCP clocks; the operation is retained explicitly. Existing generic
method counts and outcomes remain unchanged. A failed operation is still a
mutation attempt, not a successful edit.

The public self-test covers both IDs, preparation-plus-call versus mentions,
invalid argv/env/case, hash mismatch refusal, no manufactured result, and direct
MCP taxonomy. The earlier assertion-control-flow negative and read-wrapper
compatibility tests remain required. No hour-two collection or v2 registration
is part of this change. Retained pre-extension source remains outside the
worktree in usage-evidence/collector-before-public-registration.py.
