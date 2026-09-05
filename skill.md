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
clj-surgeon :op :change! :spec-file - <<'EDN'
{:changes [{:id :rename
            :in ["src/app.clj"]
            :forms [run]
            :find :old
            :do [:replace :new]
            :expect {:matches 1 :each-form 1}}]
 :expect {:changes 1 :edits 1 :files 1}}
EDN
```

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
