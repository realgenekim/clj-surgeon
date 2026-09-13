# Sol round-four F1 repair

Authorized sequence: red reproduction, shared parser repair, focused witnesses,
one fast suite, one restricted diagnostic gate, real prewarm with at most one
repair and final retry. BB-PROBE-003 owns the closure guarantee; BB-PROBE-004
owns the bounded receipt. The existing high-level warm-probe intent is unchanged.

Reuse and extend form-identity's ns dependency parser; do not retain a second
probe parser. Cover prefix and nested prefix lists, vector and bare/string
libspecs, require/macros/use, clj/default/spliced reader conditionals, unknown
forms, and reload failure with fewer completed names than expected. Preserve
test-root authorization, canonical path confinement, source cap, and serial
reload. Tests use literal independent expected closures, plus Sol's filesystem
fixture and a real stale-dependency execution witness. Unknown dependency forms
must refuse before reload with file and form. No budget or runtime change.

All new artifacts live here. Native exact-form patches are the binding brief's
authorized source entrance; record each edited file and any repair in dogfood.md.
Independent Fable/Sol review remains separate from this branch-only build.
