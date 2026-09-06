<!-- BEGIN CLJ-SURGEON ROUTING v:2 -->
## Clojure edit routing (measured 2026-09-06, build >= 13c12401)

Canonical text: the `clj-surgeon` skill, section "Edit routing". This block is a pointer; read that section before the first Clojure edit of a task. It holds the full decision table, the entrance commands, the refusal handling, and the numbers with their qualifiers.

- Reads: owner and line known — direct bounded read, no outline. Owner unknown in a large file — one outline or one search, then the form. Source already held — no reread.
- Known small literal change in one region: native `rg` plus `apply_patch` remains a legitimate production default.
- Bounded mechanical edit (rename across call sites, move helpers, thread a parameter, add a require across namespaces) AND scope, proof profile, provider permission and measured admission facts already fit: try the `bin/mission` executor first (`bin/mission run --spec-file -`, or `propose` then `apply`). Do not invent a profile or a prior to force eligibility. The executor is a 2026-09-05 PROTOTYPE; its kernel source commit is not yet a git commit.
- Typed refusal (`:error_type` starting `mission-`): read the reason; retry only when new evidence or a concrete supported correction lifts it; otherwise finish natively and record the provenance. Finishing natively is legitimate.
- New code, new tests, prose, non-Clojure: native — ineligible for this executor on this build, not forbidden. Where complete reference discovery is required, keep Surgeon semantic preparation; `rg` is not a closure proof.
- Never: per-form MCP writes for fan-out, `apply_clojure_changes` with a namespace owner, forms-scoped `find`+`replace` for insertion. Measured losers 2026-09-02.
- The installed CLI is the production entrance. Do not start an MCP server for an ordinary edit; persistent MCP is explicit development work; provider calls only in opted-in repositories or profiles.
- Tie-break is complete verified task cost — orientation, refusals, proof — not the existence of a receipt. Reassess at each Gene report; nothing expires silently.
- Lint through `~/bin/clj-kondo`. Do not discover, register, start, or call cclsp or clojure-lsp.

**Every Surgeon MCP operation relays the same terminal-response contract.**
If `terminal_response` is present and this mutation completes all remaining
user-requested work, return its value exactly. Do not add text, reread, or
reverify. If work remains, do not return `terminal_response`. Treat it as
terminal evidence for this operation and continue. `next_action=none` and
`terminal_response` describe only the completed mutation. They never prove
that the complete user request is finished.

Derived from `docs/observations/2026-09-06-clojure-edit-routing-rule.md`; surfaces and install procedure in `docs/observations/2026-09-06-routing-prompt-surfaces.md`.
<!-- END CLJ-SURGEON ROUTING v:2 -->
