# Block B attempt17: BLOCKED

The literal requested write envelope denies `/dev/null`, so the diagnostic
`make landing-gate-prewarm` exited before any stage could start. Make's
`command -v swipl >/dev/null` failed its redirect and falsely reported SWI
missing. SWI-Prolog is installed at `/home/forge/.local/bin/swipl` (10.0.0).

This is one diagnostic invocation, **not one of the two counted prewarms**.
No second whole-gate invocation, source repair, test-fast, or real prewarm
was run. The requested complete census and green receipt remain owed.
`gate.md` is the verbatim aborted diagnostic output, identical to
`envelope-red.log`; it is not a successful gate receipt.

## Evidence and concrete next step

- Starting HEAD: cb6330d474373dd4ca303187731d4e15ad9220ab, branch
  bb-rewrite-tower-local. Commit 9342bc03 preserves the sixth packet under
  `prewarm-checkonly-run6/` before other work.
- `restricted-gate.py` generalizes attempt15 to arbitrary argv and the three
  allowed roots. Each call creates a UUID packet-shaped TMPDIR and supplies
  JAVA_TOOL_OPTIONS with -Xmx1g and that java.io.tmpdir; _JAVA_OPTIONS is
  removed. It changes no shared permissions. The final wrapper also mediates
  TRUNCATE and refuses Landlock ABI below 3; the host reports ABI 8. That
  strengthening followed the aborted run and has not been gate-tested.
- `envelope-red.log`: exit 2, wall 1.9989320416934788 seconds; exact denied
  path `/dev/null`. No manifest stage or suite ran.
- `cell-b-red.log`: focused original-source oracle under the envelope, three
  tests and twelve errors, exit 1. All twelve exact /var/tmp/b07-lint-* paths
  are retained. This independently reproduces the reported fixture defect.
- `envelope-census.md`, `literal-scan.txt`, and `stage-literal-scan.txt` retain
  the partial census and direct-node path audit. Three static changes are
  identified: Cell B fixture root, papercut baseline mirror, and Cell B
  shell default. Static findings are not claimed as executed red nodes.

A question is pending: does the real packet permit writing the `/dev/null`
device, and may the wrapper grant that device-only exception and replace the
aborted diagnostic with the whole-gate run? The exact three-root instruction
is the reason this exception has not been assumed. No automatic approval
review or skill instruction rejected the action.

## DOGFOOD

| Source edit | Intent | Mechanism | Refusal | Repair text sufficient? |
|---|---|---|---|---|
| attempt17/restricted-gate.py (new) | Arbitrary-command envelope witness | Native file creation and apply_patch; executed Make and real Cell B oracle | Kernel EACCES for /dev/null and twelve fixture roots | Fixture repair is clear; device exception requires envelope clarification |

No production/test source file was edited; no Surgeon mutation was called.
The working-tree routing skill selects native for this ordinary Python work.
No performance or complete gate coverage claim follows from the wrapper.

## Owed and limits

Complete whole-gate diagnostic, any unreached-node probes, all source repairs
with red/green evidence, second whole-gate envelope run, test-fast once, real
prewarm with at most one repair/final retry, full receipt retention, and
Fable review remain owed. No budgets, test membership, runtime assignments,
skips, shared installs, servers, pushes, tags, or merges changed.

Least sure: the packet's actual device allowlist was not supplied. The
three-root filesystem rule alone cannot be equivalent to a packet that has
already run shell redirections successfully. No blanket /dev allowance is
proposed. The final evidence commit is last for this blocked checkpoint.

Recorded at 2026-09-12T13:34:34.655534+00:00.
