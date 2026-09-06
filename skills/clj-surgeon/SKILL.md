---
name: clj-surgeon
description: >-
  Use for advanced clj-surgeon workflows: semantic preparation, computed preview, extraction or movement, CLI fallback, MCP recovery, and troubleshooting. Do not invoke for ordinary inspect_clojure or edit_clojure calls; always-loaded routing and tool schemas cover them.
---

# Strictly better, or native
Route automatically ONLY when the task matches one of the two witnessed contracts
below and the complete receipt path is available; otherwise native. "Strictly
better" means a better EXPECTED complete verified wall on an eligible task with a
bounded escape — not a per-invocation guarantee. The always-loaded routing plate
carries one complete call for each; act from the plate, and open this file only for
the unusual case. Ratios, fixtures and caveats:
`docs/observations/2026-09-06-strictly-better-evidence.md`.
- FAN-OUT: one `apply_clojure_changes` `edits` list — known old/new form, a complete
  bounded set of NAMED owners in hand, a valid proof profile.
- ALIAS migration: one `alias_migration` — known old/new alias intent and an eligible
  scope. No proof profile and no pre-enumerated match set; the measured run used
  neither. Run the repository's required load and tests afterward.
Outside those two, native is the PERFORMANCE default — that is a default, not an
impossibility claim: an explicit user request or a separately approved experiment may
use any other capability. `inspect_clojure` is a SUPPORTING read, never an automatic
route: use it when structural information is actually needed and native discovery has
not already supplied it. Every lib, Var, file, form and count in an example is a task
input; include `workspace_root` when the target is not the server's default project.

## One repair, then native

Repair ONE clear, safely correctable argument error from the refusal text. A stale or
conflicting snapshot needs fresh evidence, not a repair; an unavailable capability
goes native immediately. Before any fallback, read the receipt's mutation and commit
status — a refusal does not imply that nothing was written, and a completed change is
never reapplied blindly.

## Receipts test values, not field names

Optimize complete verified task time — orientation, refusals, retries, emission and
proof; server runtime was about 2% of every measured wall, and tool runtime is never
subtracted. Proof is `verification_complete=true` TOGETHER WITH the named successful
checks over the current snapshot; a false or pending field is not evidence. An atomic
commit with bytes read back proves the WRITE, not task semantics: do not re-verify a
proven write, and do not treat it as behavioural proof. Run the outstanding required
checks and repair any failure before claiming completion. No receipt retires
user-required review, independent acceptance, or a check never performed.

## Meter and kill switch

Required per routed class: first-attempt success, refusal rate, fallback rate, and
complete request-to-verified wall, plus a periodic preregistered native pair. A
correctness failure SUSPENDS the class. A wall loss is assessed against that class's
controls, never banned on one noisy pair. Unknown telemetry means unknown
performance — use native pending investigation, not a recorded loss.

## Syntax trip-wire

Every CLI call is `:op <name>` plus key-value pairs; positional guesses produce
`Unknown op`. Put any nontrivial plan on stdin with `:spec-file -`. Smoke test:

```bash
clj-surgeon :op :ls :file src/my/ns.clj
```

## References

The unusual case only; never a prerequisite for a routed call. Read
[CLI fallback](references/cli-fallback.md) for full syntax and receipts,
[advanced CLI operations](references/advanced-operations.md) for extraction,
moves, renames, or CLJC, and [advanced MCP routes](references/mcp-advanced.md)
only for explicit development-service work. Do not reopen a reference already consumed.
