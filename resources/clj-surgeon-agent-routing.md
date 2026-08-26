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
- When an extraction decision already supplies the exact source, destination,
  ordered forms, visibility changes, and complete caller accounting, submit one
  `apply_clojure_changes` extraction directly. Do not pay for a separate
  planning call. If the task supplies an exact external verifier, omit
  transaction `verify` and run that verifier once afterward. Use the
  non-mutating dependency/manifest route when any of those decisions remain
  unknown or need review.
- Use native patching for a small visible literal edit, prose, a new file, or
  an unsupported operation. Use heavyweight `apply_clojure_changes` only for
  prepared semantic decisions, unique operations, or gates that must roll back
  with the source transaction.
- If the MCP tools are unavailable, use the installed `clj-surgeon` CLI when
  its structural operation materially helps. Otherwise use native tools.
<!-- END CLJ-SURGEON ROUTING v:1 -->
