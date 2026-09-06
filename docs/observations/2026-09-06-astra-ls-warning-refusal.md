# Actual `:ls` refusal diagnosis

Read-only reproduction on branch `astra/ls-refusal-diagnosis` at root revision `6c864e10`. No source changes, services, JVMs, provider calls, or cohorts. Worktree remains clean.

`~/bin/clj-surgeon :op :ls :file src/clj_surgeon/mission_cli.clj` reproduces `:forward-reference-analysis-failed`, exit 2, empty diagnostic. `:cat :form -main` on the same file succeeds (exit 0).

The installed CLI launcher runs its pinned Babashka source bundle. Its forward_refs/run-kondo demands an admitted, finished subprocess with exit zero, then parses JSON. On nonzero it only exposes stderr. The exact declared clj-kondo invocation returned valid JSON containing 33 definitions and 618 usages, zero errors, and one `redundant-let` warning at row 429. Its stderr was empty. Local clj-kondo help documents warning as the default fail threshold. This is a lint/analyzer exit-policy mismatch, not a missing binary or missing analysis.

## Narrow recovery and proposed fix

For callers now: use named `:cat :form OWNER` where the owner is known; this was actually verified for `-main`. Do not disable lint globally or alter the target merely to make an outline work.

For a bounded repair: add `--fail-level error` to this analyzer invocation, retaining admission, completion/timeout, output bounds, JSON validation, and genuine error rejection. The exact command with this one flag exits 0 and preserves the analysis and findings byte-decoded values identically. This removes the observed warning-only refusal without accepting arbitrary nonzero exits. Errors can remain refusals; a later decision to consume analysis with errors would need a separate contract. Diagnostics should eventually expose bounded structured findings when stderr is empty, but that is separable from the minimum fix.

Required regression before landing: warning-only valid source produces its forward-reference result; a genuine analyzer error/nonzero or malformed response still refuses. No implementation or scoped core edit was made in this investigation. Confirm the owning intent before implementation.

This deserves priority as a routine structural discovery entrance blocked by an unrelated warning. It does not show compiler invalidity, lost analysis, or a performance result. Artifacts and hashes: result.json; original ls/cat receipts; default and error-threshold analyzer JSON/stderr/exit receipts. Direct analyzer use was a bounded diagnostic after the actual Surgeon refusal, not a replacement production route.
