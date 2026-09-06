<!-- BEGIN CLJ-SURGEON ROUTING v:2 -->
## Clojure edit routing (policy revision 1, 2026-09-06)

Canonical text: the `clj-surgeon` skill, section "Edit routing (policy revision 1, 2026-09-06)", in `skills/clj-surgeon/SKILL.md`. This block is a pointer AND the routing summary: it is sufficient on its own for an ordinary edit. Read the canonical section when the route is not already decided by the rules below, or when an advanced workflow needs its references. Do not load a skill per edit.

- Reads: owner and line known — direct bounded read, no outline. Owner unknown in a large file — one outline or one search, then the form. Source already held — no reread.
- Known small literal change in one region: native `rg` plus `apply_patch` remains a legitimate production default.
- Any other bounded mechanical edit: production chooses native or a deterministic Surgeon route by COMPLETE VERIFIED TASK COST. There is no executor-first rule in production.
- The earned deterministic Surgeon routes: `:extract!` to a new namespace, `:rename-ns!`, `require_change` across namespaces, `within` plus `from`/`to` inside one known form. Measured winners 2026-09-02; unchanged.
- Under the mandated dogfood EXPERIMENT ONLY, explicitly opted into: try the `bin/mission` executor first on an eligible edit, then one ledger line. Executor-first is the experiment's rule, not production's. The executor is a 2026-09-05 PROTOTYPE; its kernel source commit is not yet a git commit.
- Typed refusal: codes arrive as `:error_type` strings beginning `mission-` (`"mission-workspace-required"`), as `:error-type` keywords from the kernel (`:forms-protected-syntax`, `:forms-comment-lost`, `:forms-comment-moved`, `:typist-all-candidates-rejected`), and as NESTED diagnostics under `:candidates`/`:proof`/`:decision`. Read the reason; retry only when new evidence or a concrete supported correction lifts it; otherwise finish natively and record the provenance. Finishing natively is legitimate.
- New code, new tests, prose, non-Clojure: native — ineligible for the experimental executor on this build, not forbidden. Where complete reference discovery is required, keep Surgeon semantic preparation; `rg` is not a closure proof.
- Never: per-form MCP writes for fan-out, `apply_clojure_changes` with a namespace owner, forms-scoped `find`+`replace` for insertion (measured losers 2026-09-02). The installed CLI is the production entrance: do not start an MCP server for an ordinary edit; persistent MCP is explicit development work; provider calls only in opted-in repositories or profiles.
- Tie-break is complete verified task cost — orientation, refusals, proof — not the existence of a receipt. Reassess at each Gene report; nothing expires silently. Lint through `~/bin/clj-kondo`. Do not discover, register, start, or call cclsp or clojure-lsp.

**Every Surgeon MCP operation relays the same terminal-response contract.**
If `terminal_response` is present and this mutation completes all remaining
user-requested work, return its value exactly. Do not add text, reread, or
reverify. If work remains, do not return `terminal_response`. Treat it as
terminal evidence for this operation and continue. `next_action=none` and
`terminal_response` describe only the completed mutation. They never prove
that the complete user request is finished.

Numbers with their evidence, surfaces and install procedure: `docs/observations/2026-09-06-routing-prompt-surfaces.md`.
<!-- END CLJ-SURGEON ROUTING v:2 -->
