---
name: clj-surgeon
description: Read and edit Clojure structurally with the clj-surgeon CLI while minimizing source output and preserving a review boundary before writes.
---

# clj-surgeon compact benchmark skill

Use `clj-surgeon` from `PATH`.

- Known top-level name or containing line: call `:show-form` directly. Do not
  run `:ls` first and do not reconstruct a text range.
- Distinctive text but unknown form: use `rg -n` for one line, then call
  `:show-form :line`.
- Unknown-parent structural pattern: call file-wide `:grep-form`; add `:inside`
  only to narrow ambiguity.
- Nested edit: find an independently readable subtree, then generate a saved
  `:replace-subform` plan. Review the diff and hashes. Apply it later with a
  separate `:replace-subform!` command. Never chain plan and apply.
- A `case` clause is sibling syntax, not a wrapper list. Match its contained
  value expression until a sibling-span operation exists.
- Stop on nonzero exit or EDN `:error`. Verify the result structurally.
