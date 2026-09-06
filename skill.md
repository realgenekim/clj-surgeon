---
name: clj-surgeon
description: >-
  Use for advanced clj-surgeon workflows: semantic preparation, computed preview, extraction or movement, CLI fallback, MCP recovery, and troubleshooting. Do not invoke for ordinary inspect_clojure or edit_clojure calls; always-loaded routing and tool schemas cover them.
---

# Production clj-surgeon routes (advanced router)

Optimize complete verified task time. The installed Babashka CLI is the
production entrance. Persistent MCP is a development-only, months-long
experiment; use it only when explicitly testing that service. Do not load this
skill for an ordinary bounded structural read or an already-decided compact
edit.
## Edit routing (policy revision 1, 2026-09-06)

The canonical Clojure edit-routing table lives in
[skills/clj-surgeon/SKILL.md](skills/clj-surgeon/SKILL.md), section
**"Edit routing (policy revision 1, 2026-09-06)"**. That working-tree file is the
SOURCE; installed skill mirrors follow it through `make install-claude-skill`
and `make install-codex-skill`. The table below is that section's table
reproduced verbatim; change it there first, then re-run
`bb bin/check-routing-parity.clj`.

| Situation | Route |
|---|---|
| Owner and line already known | Direct bounded read: `:op :cat :file F :form NAME`, or `sed -n 'A,Bp'` on the known range. No outline. |
| Owner unknown in a large file | One outline or one search (`:op :ls`, or `rg`), then read the named form. |
| Source already held in context | No reread. |
| Known small literal change in one region | Native `rg` plus `apply_patch`. This stays a legitimate production default. |
| Bounded mechanical edit (rename across call sites, move helpers, thread a parameter) | Choose native or a deterministic Surgeon route by COMPLETE VERIFIED TASK COST. There is no executor-first rule in production. |
| Extraction to a new namespace; namespace rename; a require added or changed across namespaces; a surgical edit inside one known form | The earned deterministic Surgeon routes: `:extract!`, `:rename-ns!`, `require_change`, `within` plus `from`/`to`. Kept from the 2026-09-02 ruling: no native equivalent, or measured zero churn. |
| Complete reference discovery required | Surgeon semantic preparation. `rg` is not a closure proof. |
| New code, new tests, prose, non-Clojure | Native. Ineligible for the experimental executor on this build; not forbidden territory. |
| Under the mandated dogfood EXPERIMENT only, explicitly opted into, an eligible bounded mechanical edit | Try the `bin/mission` executor FIRST, then write one ledger line. Executor-first is the experiment's rule; it does not govern production routing. |
| Fan-out via per-form MCP writes; `apply_clojure_changes` with a namespace owner; forms-scoped `find`+`replace` for insertion | Do not use. Measured losers 2026-09-02, not re-measured since. |

This file is a legacy working-tree copy kept for readers who open it
directly. It is NOT the installer source and it does not override the
canonical section above.

## Choose the cheapest authority

Use native `rg` and `apply_patch` for a known literal and one small region. Use
`:ls`, `:cat`, or `:match-form` for unknown structural owners; use `:edit` for
an exact nested replacement. Use `:change!`, `:extract!`, `:mv-with-deps`,
`:rename-ns!`, or `:fix-declares!` for guarded cross-file work. MCP is opt-in
development work only.
Surgeon earns its cost when one guarded operation replaces many owner reads and
writes. Historical favorable fan-out cohorts reached roughly 3–10x complete-task
speedup; tiny edits often favor native tools. Treat those figures as
workload-specific priors, never guarantees.
## Timing and safety

Count complete verified task time, including orientation, refusals, retries,
emission, and proof. Tool runtime alone is not end-to-end speed. Inspect the
EDN receipt, stop on `:error`, run focused tests or lint, and keep one coherent
operation per commit.
## Avoid shell quoting

For any nontrivial plan, put the structured request on stdin. This is the CLI
equivalent of MCP's structured arguments and avoids nested shell quoting:

```bash
clj-surgeon :op :change! :receipt-out ./change-receipt.edn :spec-file - <<'EDN'
{:changes [{:id :rename
            :in ["src/app.clj"]
            :forms [run]
            :find ":old"
            :do [:replace ":new"]
            :expect {:matches 1 :each-form 1}}]
 :expect {:changes 1 :edits 1 :files 1}}
EDN
```

`:find` and the replacement are source strings, even for a keyword literal.
`:change!` requires a writable `:receipt-out` path for its guarded undo receipt.
Use `:spec-file PATH` for a saved request. Attach stdin in the same shell
action; never invoke `:spec-file -` and wait for a later input stream.
Never run `clj-surgeon up` casually. It is development-only, edits workspace
agent configuration, starts local services, and requires an explicit guard:

```bash
clj-surgeon up /absolute/repository --force
```

## Syntax trip-wire

Every call is `:op <name>` plus key-value pairs; positional guesses produce
`Unknown op`. Known-good smoke test:

```bash
clj-surgeon :op :ls :file src/my/ns.clj
```
## References

Read [CLI fallback](skills/clj-surgeon/references/cli-fallback.md) for full syntax and receipts,
[advanced CLI operations](skills/clj-surgeon/references/advanced-operations.md) for extraction,
moves, renames, or CLJC, and [advanced MCP routes](skills/clj-surgeon/references/mcp-advanced.md)
only for explicit development-service work. Do not reopen a reference already consumed.
