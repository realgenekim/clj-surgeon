<!-- BEGIN CLJ-SURGEON ROUTING v:1 -->
## Clojure structural editing

- Batch known Clojure structural reads in one `inspect_clojure` call. Set
  `include_source=false` only when names, ranges, counts, hashes, or source
  anchors are sufficient. Do not repeat a complete read.
- When the complete decision already names files, owners, old forms,
  replacements, exact counts, computed programs, or owner deletions, call
  `edit_clojure` once. Use `within.form` for a named top-level owner and
  `within.namespace` for the `ns` form. Use `delete_owners` for several exact
  top-level deletions.
- Treat resolved owner names, exact old forms, counts, and the frozen snapshot
  as stale-source guards. Do not preflight-read an already-decided edit.
- Treat `verification_complete=true` as terminal mutation evidence. Do not add
  a reread or diff only because Surgeon performed the edit. Run the same
  proportional formatter, linter, and tests that the native route requires.
- Invoke direct clj-kondo lint through `~/bin/clj-kondo`. This paved entrance
  serializes analyzers across agents, repositories, and Surgeon JVMs. Do not
  bypass it with an absolute Homebrew path unless the task is explicitly
  testing the bypass contract.
- When an extraction decision supplies the exact source, destination, ordered
  forms, and require policy, submit one `apply_clojure_changes` extraction
  directly. Supply known visibility and caller decisions; omit them when the
  frozen compiler can derive them. A mechanically complete zero-candidate case
  commits in that call. A genuine caller decision refuses before writing with
  the completed snapshot-bound plan; fill it and apply once without another
  discovery read. Do not pay for a separate planning call as preflight. If the
  workspace declares the matching project-owned exact profile, pass
  `verify="exact"` so the verifier participates in the transaction. Otherwise,
  omit transaction `verify` and run the supplied exact verifier once afterward.
- If `terminal_response` is present and this mutation completes all remaining
  user-requested work, return its value exactly. Do not add text, reread, or
  reverify. If work remains, do not return `terminal_response`. Treat it as
  terminal evidence for this operation and continue. `next_action=none` and
  `terminal_response` describe only the completed mutation. They never prove
  that the complete user request is finished.
- Use native patching for a small visible literal edit, prose, a new file, or
  an unsupported operation. Use heavyweight `apply_clojure_changes` only for
  prepared semantic decisions, unique operations, or gates that must roll back
  with the source transaction.
- If the MCP tools are unavailable, use the installed `clj-surgeon` CLI when
  its structural operation materially helps. Otherwise use native tools.
<!-- END CLJ-SURGEON ROUTING v:1 -->
