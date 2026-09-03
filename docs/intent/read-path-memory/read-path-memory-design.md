# Read-Path Memory Bounds

Status: draft LLD; first slice (`MCP-OP-MEM-015`) implemented.

## Context

The measured read path is the outline projection. On 2026-09-03 an Opus consult
measured `outline-source` against a 52,665-byte file and found it allocated
76 MB — roughly 1,450 times the source — while retaining 33 KB. Two causes were
isolated, both of them work the projection throws away:

1. **The file is parsed twice.** `top-level-form-records` builds one
   `rewrite-clj` zipper to walk top-level forms; `outline-source` then builds a
   second, independent zipper over the same string purely to locate the `ns`
   form and read its `:require` clauses. Isolated on a 1,000-file corpus the
   second parse cost 21% of wall time and 31% of allocation
   (11.30 s / 24.6 GB with it, 8.96 s / 17.0 GB without).
2. **A `:source` string is built for every top-level form and then discarded.**
   `top-level-form-records` puts the exact form text in each record because
   structural readers (`extract`, `show_form`, the source anchor) need it.
   `outline-source` `dissoc`s that key from every record it returns.

Neither cost is the rewrite-clj node tree itself. The tree is irreducible for
this projection: parsing and walking one 48,097-byte file allocates 36.1 MB
(750x the source) with nothing else running. This leaf does not promise to
replace the parser; it promises the outline pays for it once and builds nothing
it discards.

## Boundary

- The requirement is stated in **allocated bytes per source byte for one
  `outline-source` call**, measured with
  `com.sun.management.ThreadMXBean/getThreadAllocatedBytes`. That meter is
  deterministic and thread-local; it is not affected by heap size, GC choice,
  or a concurrent suite.
- **It is not a resident-memory bound.** Retained bytes after collection, peak
  used heap, and the minimum `-Xmx` that completes a corpus are the memory
  battery's subject (`MCP-OP-MEM-001` / `MCP-OP-MEM-011`), not this leaf's.
  Peak used heap under G1 is heap-size dependent and cannot carry a
  requirement.
- **It changes no outline content.** The compact `:ls` projection — `:ns`,
  `:file`, `:lines`, `:form-count`, `:forms`, `:requires`, `:forward-refs`, and
  every field of every form record including insertion order — is byte-identical
  before and after. The acceptance artifact is a differential test that
  reconstructs the previous two-parse path from public functions and compares
  `pr-str` of both outlines over every `.clj`, `.cljc`, and `.cljs` file under
  `src/` and `test/`.
- **Structural readers keep `:source`.** `top-level-form-records` still returns
  the exact form text by default. Only a caller that passes
  `{:include-source? false}` opts out, and `outline-source` is the only such
  caller — and only when it is not asked for string symbols, which are derived
  from `:source`.
- The single-parse rule is scoped to `outline-source`. It says nothing about
  `include_string_symbols`, which re-parses each selected form's own text by
  design, nor about callers that outline the same file twice.

## Why parse-once and no-discarded-source are one intent

The observable this leaf promises is a single number: bytes allocated per
source byte for one outline. Two named causes contribute to it, and a
maintainer who fixed one and reintroduced the other would leave the observable
unmet. Splitting the row would let each half pass while the promise fails, so
the row is one intent with two witnesses — an allocation ceiling (the outcome)
and a parse count (the cause a refactor is most likely to reintroduce).

## Misreadings this row forbids

- *"Parse once" means memoize `z/of-string` behind a cache.* It does not. A
  cache keyed on a mutable source string is a correctness hazard and a
  retention hazard; the fix is to thread the one zipper the walker already
  built, not to keep parses alive.
- *"Do not build `:source`" means remove `:source` from the record.* It does
  not. `extract`, `show_form`, the compact-location normalizer, the source
  anchor, and the change buffer all read it. Only the outline projection opts
  out.
- *The ceiling can be tightened to the low tens.* Not without replacing
  rewrite-clj for read-only projections. That is a separate, later step behind
  a differential gate; a maintainer who "tightens" this ceiling without it will
  find the parser, not a defect.
- *An allocation win implies a resident or peak win.* It does not. This leaf's
  measurement is transient allocation; retention is unchanged and is the
  battery's subject.

## Boundaries the witnesses must hold

- A file with no `ns` form: the outline still reports `:ns nil` and
  `:requires []`, and still parses once.
- A `.cljc` file whose top-level forms live inside reader conditionals: forms
  reached through `#?` and `#?@` keep their exact platform sets.
- `include_string_symbols true`: `:source` is still built, because the symbol
  scan reads it; the outline output is unchanged.
- An empty or whitespace-only source: no parse-count or allocation regression,
  and no exception.
