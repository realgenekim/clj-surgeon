# Prepared-request live-route attempt 1 — invalid

Attempt root:
`/private/tmp/clj-surgeon-prepared-live-measure.dlyb6e/results`

The first frozen run stopped at its paid-path validity gate. All read-side
semantic checks passed, the ineligible response carried no descriptor, the
read fixture remained byte-identical, and the filled descriptor committed the
exact expected edit with `verification_complete=true`.

The invalid condition was the harness requirement that independently
constructed and descriptor-filled JSON argument objects be byte-identical.
They decoded to the same object but serialized object members in different
orders:

- filled: `workspace_root`, then `edits`; and
- from scratch: `edits`, then `workspace_root`.

JSON object-member order has no public contract meaning. Treating it as
semantic made a correct public call fail the instrument. This attempt is not
rescored. The forward protocol requires exact decoded JSON equality, retains
both raw serializations, and measures their bytes and `o200k_base` tokens
separately. A new whole run uses new result and workspace paths.
