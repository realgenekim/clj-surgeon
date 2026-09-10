NO-GO

# Seat-path defaults fence — sealed candidate `1403bc7221e07e607e8c9f39242776f3096a9801`

## Findings

### SPF-001 — artifact-root selection is not fail-closed

`default-artifact-root` says the store is private to the invoking user, but accepts
`CLJ_SURGEON_ARTIFACT_ROOT` and `XDG_STATE_HOME` without validating emptiness,
absoluteness, ownership/containment, or filesystem type. The writer's only guard
rejects a root inside the current workspace; it does not apply the repository's
temp-hygiene `base-refusal`.

Observed against the sealed tree:

- with both variables unset, the root is
  `/home/forge/.local/state/clj-surgeon/artifacts`;
- `XDG_STATE_HOME=/tmp` produces `/tmp/clj-surgeon/artifacts`, although calling
  the existing `base-refusal` on that result returns `:ram-path-prefix`;
- `CLJ_SURGEON_ARTIFACT_ROOT=/home/someone-else/state` is accepted;
- a blank `CLJ_SURGEON_ARTIFACT_ROOT` produces `""`, and a blank
  `XDG_STATE_HOME` produces `/clj-surgeon/artifacts`.

This contradicts both the candidate's own per-user/private-state contract and the
requested temp-hygiene invariant. Repair requires one explicit configuration and
refusal contract, with red witnesses for blank, relative, foreign-state, and
RAM-backed roots. It must retain a controlled external override for the parity
harness rather than silently removing that use case. That is broader than a safe
fence one-liner, so no fix patch is attached to this verdict.

The in-flight parity harness has the expected variable name,
`CLJ_SURGEON_ARTIFACT_ROOT`. Candidate semantics are first-non-nil selection; the
stable build does not read the variable and already writes `/var/tmp/forge`, so
pinning the candidate to that same real-disk path makes the two locations agree.
The successor must preserve this explicit, documented harness case while refusing
uncontrolled widening.

### SPF-002 — the shipped Python admission entrance retains the same live defect

`resources/clj-kondo-admission.py` still defaults to
`~/.local/state/diagnose-skiff-cpu-memory/monitor/status.json`. This is not dead
duplication: direct-shell mode constructs `pressure_status` from
`CLJ_SURGEON_PRESSURE_STATUS`, and when it is absent `pressure_status_path` uses
the Python literal without passing through `mcp_process.clj`. It must change with
this candidate to `~/.local/state/clj-surgeon/pressure-status.json` and gain a
witness covering the Python fallback. Deferring it would ship two defaults for
the same admission contract.

### SPF-003 — owning intent still specifies the removed literal root

`docs/intent/alias-migration/receipt-artifacts-specs.md` still requires publication
under `/var/tmp/forge/<verb>-receipts/`, while the candidate implements a derived
per-user root. The successor must update the owning intent/EARS before tests and
code so the contract does not contradict the implementation.

## Checks that passed

- HEAD is the sealed merge `1403bc7221e07e607e8c9f39242776f3096a9801`.
- Focused new witnesses passed: artifact default (4 assertions), pressure-status
  default (2), literal-root scanner (5), and MEMBAT default (3).
- The repaired artifact assertions are substantive: every affected call site uses
  `under-root?` or `published-under-root?`, not `includes?` of an empty string.
  `mcp_workspace_test/receipt-directories-are-deterministic-and-workspace-isolated`
  passed normally (3 assertions); redefining its expected verb root to
  `/definitely/wrong-root` produced exactly one failure.
- No `make test` was run; the builder's supplied seven-stage green gate was not
  repeated.


> END RECEIPT (fence-run): worktree HEAD at review exit = 1403bc7221e07e607e8c9f39242776f3096a9801 = fenced sha.
