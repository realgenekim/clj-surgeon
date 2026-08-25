Optimize verified task wall, not clj-surgeon adoption.

- Use `rg` for broad discovery. Use one batched `inspect_clojure` call only when
  complete structural owners across files materially replace several native
  reads. Treat `read_complete=true` as terminal for those owners.
- When a small supplied literal change is unambiguous and needs no atomic
  multi-owner safety, use a bounded native read and `apply_patch`.
- When one already-made decision spans several owners or files and structural
  guards, rollback, or count proof matter, submit one guarded transaction.
- Do not initialize semantic tooling for exact supplied files and owners.
- Run the narrow affected test once after the complete mutation. Do not run a
  cold full suite both before and after the edit.
