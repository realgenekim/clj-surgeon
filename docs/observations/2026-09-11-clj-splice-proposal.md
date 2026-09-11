# Proposal: clj-splice — a thin pure layer on rewrite-clj (2026-09-11)

Gene: "Oh I love it. Layer on top of clj-rewrite. Tests could be super fast. Thoughts? If good, Astra review and then go!!!!"

## The idea in one paragraph
Everything Surgeon owns about bytes becomes three pure functions over strings, built directly on rewrite-clj's parser and node layer (no zipper policy, no I/O, no receipts): `spans` (bytes → ordered top-level forms with byte offsets, per-form SHA-256, gap bytes, tag, owner kind/name when it is a def-like form), `splice` (bytes + a span boundary + payload bytes → new bytes, preserving every byte outside the boundary; the A-prefix / inserted / A-suffix contract), and `recount` (bytes → effective top-level form count and a structure summary comparable to a prior `spans`). Surgeon keeps the contract: request schema, guard hash, refusals, transaction, receipts, entrances. The insertion planner's own lexical pass (strings/escapes/comments/reader prefixes/depth before rewrite-clj runs) is deleted; rewrite-clj is the only reader.

## Why
- Tests become fast: pure functions on strings, no worktree, no transaction, no disk; rewrite-clj is bundled in babashka, so the library's suite runs under `bb` in about a second and the same code serves the CLI (bb) and the MCP server (JVM). The two verbs' 1,151 expanded assertions are slow because of transaction fixtures, not logic.
- One implementation of the byte layer instead of two verbs' copies; the rename `annotate → addresses` duplicate parse goes.
- The two span mechanisms Astra identified as upstream-worthy (precise source spans with byte offsets; the synthetic map-qualifier coordinate) are shaped as a contribution from day one.
- Witness burden follows the code: splice edge cases (multi-line strings, `#_#_`, `\(`, regex with brackets, CRLF, BOM, tabs) are library tests, executed once, not per verb.

## Scope fence (the whole point)
Three functions. No zipper navigation API, no "find owner by name" search beyond what `spans` returns, no formatting, no I/O, no EDN request handling, no refusals — a pure function returns data or throws one typed exception class. Anything else is Surgeon. A fourth function needs Gene's word.

## Shape
- Location: `libs/clj-splice/` inside the clj-surgeon repository for now (own `deps.edn`, own `bb.edn`, own tests; `:local/root` dependency from the main deps.edn), separable into its own repository without code change. Namespace `clj-splice.core`.
- Coordinates: byte offsets (UTF-8), computed from rewrite-clj row/col positions and the source bytes; multi-byte characters covered by a witness.
- Tests: property-style over the E4 file and the two red-team fixture sets (the ones the independent bracket hasher agreed with, 47/47): for all spans, `(= bytes (apply str (interleave gaps forms)))`; splice then `spans` gives the prior spans shifted by the payload length outside the boundary; recount equals `(count spans)`. Under `bb` and under the JVM, same results, asserted by hash.

## Port (the audit round, same block)
insert_forms and rename_alias re-based on clj-splice: delete `insert_forms_plan/lexical!` and `shape-limit!`, delete rename's duplicate parse, consolidate to Astra's ten witness groups per verb, admit benign byte shapes (CRLF/BOM/tabs) with the promises intact, keep the four E4 refusals + parse errors + boolean seams, registry field `native_failure` per refusal. Target ≤580 expanded assertions across both verbs (from 1,151), and a focused suite under 60 s.

## Measure
Before/after: assertion count, focused-suite wall (JVM and bb), fast-suite wall, E4 through both verbs byte-identical (02332a74), the receipt-boolean class test still failing by name on an unregistered field.
