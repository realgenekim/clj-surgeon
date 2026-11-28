# Refactor census, piece 1
Contract: `bb bin/refactor-census DRYER ROOT` reads only; it parses dryer blocks, merges overlapping ranges, and groups connected copies into sorted families. Adjacent ranges remain distinct copies.
Each accepted copy reports its top-level name, public/private visibility (`defn-`, `^:private`, `defn`, `def`), and bare/aliased caller-file counts across `src/` and `test/`; repeated references count once per file and spelling class, and `files` is their union. Qualified spellings include full namespace names. Static references include value uses; lexical locals and quoted data are excluded by the existing analyzer. This is a source census, not macro expansion or dynamic call analysis.
Inputs use `<repo>/` or ROOT-prefixed paths and ordinary vector requires (`:as`, explicit `:refer`). A range must start exactly at a current top-level form start and end within that form; otherwise print `DRIFT <path>:<start>-<end> (form boundaries moved)`, count `drifted=N`, and exclude it from owner counts; unreadable or unparseable input exits nonzero. No source evaluation or writes. The header gives dryer mtime and newest file mtime under `src/`. Copies include drifted locations; public/private counts include accepted owners only. Output has one line per family, the `refactor-census: families=N copies=M public-copies=P private-copies=Q drifted=D` summary, then `trap-check: piece 2`.
Pure matrix: empty/CRLF/root-prefixed parsing; disjoint, overlapping, transitive and repeated families; private/public definitions; same-namespace, referred, aliased and fully qualified callers; repeated uses, locals, quotes, strings, comments and zero callers; missing owners, moved boundaries, exact footer, and positive test count with zero fail/error (zero tests exit 1). Field evidence uses the frozen November 22 dryer report against this worktree.
Piece 2, a later pull request: compare free-symbol resolution across copies. The draft's identical `nonblank-string!` bodies call `refuse!`, but resolve to `src/a.clj:2` versus `src/b.clj:2`; do not infer that these copies can be merged safely. No trap-check implementation or tests ship in piece 1. Gate: script <=120 lines, tests >= source lines, every script line <120 columns; pure self-tests, paved clj-kondo lint, and the invocation below. No commit or push in this task.
## Field result on clj-surgeon, 2026-11-28
28 families, first eight shown; run it for the rest. Filesystem mtimes reflect this checkout, not the snapshot date.
`bb bin/refactor-census docs/quality/2026-11-22-baseline/dryer.txt .`
```text
dryer-mtime= 2026-11-28T08:25:52.312988943Z src-newest-mtime= 2026-11-28T08:25:52.32298881Z
Family 1: src/clj_surgeon/binding_rename.clj:18-21 source-language private bare=1 aliased=0 files=1; src/clj_surgeon/mcp_formatter.clj:26-29 suffix private bare=1 aliased=0 files=1
Family 2: src/clj_surgeon/binding_rename.clj:70-76 line-offsets private bare=1 aliased=0 files=1; src/clj_surgeon/intent_transaction.clj:1146-1152 line-offsets private bare=1 aliased=0 files=1
Family 3: src/clj_surgeon/cljc/merge.clj:24-33 parse-ns-form public bare=1 aliased=1 files=2; src/clj_surgeon/cljc/require_ops.clj:23-30 ns-zloc private bare=1 aliased=0 files=1; src/clj_surgeon/cljc/split.clj:62-69 ns-zloc private bare=1 aliased=0 files=1
Family 4: src/clj_surgeon/cljc/require_ops.clj:32-38 require-form-zloc private bare=1 aliased=0 files=1; src/clj_surgeon/cljc/split.clj:71-77 require-form-zloc private bare=1 aliased=0 files=1; src/clj_surgeon/mcp_compact_relations.clj:391-397 require-locs private bare=1 aliased=0 files=1
Family 5: src/clj_surgeon/cljc/split.clj:22-26 reader-cond? private bare=1 aliased=0 files=1; src/clj_surgeon/outline.clj:195-199 reader-cond-require? private bare=1 aliased=0 files=1
Family 6: src/clj_surgeon/edit_dsl.clj:170-173 match public bare=1 aliased=2 files=3; src/clj_surgeon/edit_dsl.clj:175-178 where public bare=1 aliased=2 files=3; src/clj_surgeon/edit_dsl.clj:220-223 replace public bare=1 aliased=1 files=2
Family 7: src/clj_surgeon/fields.clj:95-106 ->defn-arg-list public bare=1 aliased=0 files=1; src/clj_surgeon/fields.clj:153-162 ->first-vector public bare=1 aliased=0 files=1
Family 8: src/clj_surgeon/fix_declares.clj:27-34 form-def-line private bare=1 aliased=0 files=1; src/clj_surgeon/fix_declares.clj:36-43 form-end-line private bare=1 aliased=0 files=1
refactor-census: families=28 copies=62 public-copies=14 private-copies=48 drifted=0
trap-check: piece 2
```
Verification: 6 tests / 43 assertions, 0 failures / errors; forced zero-test CLI exits 1; clj-kondo: 0 errors / warnings.

The header prints the dryer file's mtime beside the newest mtime under src/; in a fresh checkout both are the checkout time, so the honest age of the snapshot is its path (`docs/quality/2026-11-22-baseline/`, produced 2026-11-22) and the DRIFT gate, not the mtimes. On 2026-11-28 every one of the 62 ranges still started at a current top-level form (drifted=0).
