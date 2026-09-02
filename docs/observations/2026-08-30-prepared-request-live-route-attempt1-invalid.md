# Prepared-request live-route attempt 1 — invalid

Durable attempt root:
`bench/results/2026-08-30-prepared-request-live-route-attempt1-invalid`

Original attempt root:
`/private/tmp/clj-surgeon-prepared-live-measure.dlyb6e/results`

Manifest SHA-256:
`3f46ff076f8969152a9f860c4a3711754edb7ce99032fc564b1913de1d43c65a`

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
