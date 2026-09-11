# Astra consult: clj-splice before the build

Verdict: **GO with the changes below.** Build the shared span/splice seam and port both verbs; do not promise wholesale preflight deletion or all byte-shape admissions in the same block.

Reviewed trunk `04648059bd666fb3e40bfdaee7e0e327c273740d`, the September 11 proposal, and my prior review §§3–4. Inspected both planners and installed rewrite-clj 1.2.50 jar sources. Executed small parser/node probes on that jar in a bounded JVM and on local bb 1.13.219. This is consultation, not an implementation, full-suite run, upstream submission, or Git commit.

## 1. Both sides of the three-function fence

**For:** byte indexing, literal spans and exact replacement deserve one pure implementation. Moving those witnesses out of transactions makes experimentation cheaper. A small local library is a useful extraction seam.
**Against:** three names do not bound responsibility. A top-level inventory cannot implement either current verb; stuffing owner resolution, body grammar and alias policy into `spans` would merely hide Surgeon inside a library. `recount` is a projection of parsing, not a third independent proof engine.

Keep three public functions, with this exact division:
- `spans(source)` returns a snapshot inventory: ordered physical roots, effective-root IDs/count, N+1 root gaps, and a nested node table (ID, parent, ordered children, tag, byte start/end, original row/col, synthetic-coordinate provenance). Keep source storage once; expose slices by intervals. Hash physical root intervals, not every ancestor at every depth.
- Include syntax facts sufficient to distinguish literal/string/regex, metadata, quote, discard, reader tag and qualifier children. Raw head/name token spans are useful; **remove authoritative def-like owner kind/name from the library contract**. Surgeon retains its finite canonical-head table, metadata unwrapping and supported-owner validation. No arbitrary macro classification.
- `splice(source, [start,end), replacement)` performs exact UTF-8 interval replacement, including insertion when start=end. Validate bounds and encoding boundaries. Surgeon authorizes the interval and formats payload. Rename needs prefix subspans inside tokens, not only whole-form boundaries; apply nonoverlapping edits against one snapshot, descending by offset (or one internal linear assembly).
- `recount(candidate)` returns the same structural projection/effective-root definition as `spans`; share the parser implementation. Surgeon compares candidate bodies, roles and preserved intervals. Do not call `recount` and then `spans` to parse the same candidate twice.

Insertion needs child/header/closer positions, literal ranges for indentation, metadata ownership, arity containers and direct `testing` labels. The first three are syntax data in `spans`; `body-of`, `resolve-anchor`, comment attachment and layout remain Surgeon policy. Rename needs token/qualifier positions and ancestry to implement tag-vs-payload, quote, discard, ns-binding and collision rules. Those positions belong in `spans`; `references`, `binding!` and `collision!` remain Surgeon.

A deterministic traversal of the already parsed tree removes `annotate → addresses`' second parse. Preserve the existing public preorder convention through a Surgeon projection: current `z/next` skips trivia, so a raw CST preorder is not automatically compatible. Use node IDs, not potentially colliding `[row,col,tag]` keys. This is richer return data, not a fourth navigation/search API.

Use `ExceptionInfo` with a small namespaced error category and coordinates; no custom JVM exception class. Surgeon translates it into refusal stages and receipts. Pure invalid-input diagnostics are compatible with the fence.

## 2. Byte coordinates: exact algorithm and actual API findings

The proposal must choose its input type. Recommendation: validated Unicode Strings at the public seam, with offsets explicitly in UTF-8 bytes; retain one encoded byte array internally. Surgeon strictly decodes disk bytes with malformed/unmappable input set to REPORT. Reject unpaired surrogates before encoding. Never permit replacement-character decoding to masquerade as preservation.

**Columns are UTF-16 code units on the tested JVM and bb, not Unicode code points or display columns.** Tabs advance one column; an emoji advances two. Metadata end coordinates are exclusive.

Compute the mapping once:
1. Walk original UTF-16 source once, recording original line starts and cumulative UTF-8 offsets at scalar boundaries. ASCII costs 1 byte, U+0080–07FF costs 2, other BMP scalars 3, a valid surrogate pair 4. Mark the interior of a surrogate pair invalid as an edit endpoint.
2. Match rewrite-clj's newline normalization: LF, CRLF, bare CR and CRFF each advance one parser row; paired endings consume both original characters/bytes. No syntax lexer is needed. Within a row, original UTF-16 index = line-start + col − 1. Translate that index through the byte map; bounds-check it. Keep BOM handling explicit below.
3. Obtain raw form/gap bytes only from original byte intervals. Compute lowercase SHA-256 over those exact bytes, never `node/string`, sexpr, normalized text, or platform-default encoding. For String output, assemble bytes and strictly decode once, or map valid endpoints back to UTF-16 indices before `subs`.

Cost: O(N) indexing/encoding plus O(M) node projection and O(N) total hashing over disjoint roots; O(N+M) memory. A sparse sorted-endpoint map can trade dense storage for O(M log M) sorting. Do not encode `(subs source 0 position)` independently at every site: that tends toward O(N×sites). Repeated full-string splices similarly cost O(N×edits); linear assembly can stay private under `splice`.

**Executed Unicode witness:** source `"é😀" (def x 1)` has the def at row 1/col 7, UTF-16 index 6, **byte interval [9,18)**. Its SHA-256 is `f5c134bf6c2d0b925524242efe4dd5564dbec67f832b7e484f0a4bd1963c8939`. Both parser runtimes returned those character coordinates; Python independently supplied byte positions/hash. The build witness must splice at byte 9, prove prefix/suffix identity, and compare resulting hashes under both runtimes. Add the same fixture with CRLF before the anchor to catch line-start conversion, not just same-line Unicode.

Other verified adapter facts:
- One-line and escaped-`\n` strings have tag `:token`; physically multiline strings have `:multi-line`. Internal StringNode type is `:string`, but `rewrite-clj.node/node-type` is not a public callable API in local bb. Use supported predicates/raw syntax or one isolated adapter, not that invented public function.
- `node/string` changes `a\r\nb` into `a\nb`, including CRLF inside multiline strings. Reconstructed node text is unsuitable for source bytes or lengths.
- `#::ev{}` has a synthetic `:map-qualifier` with nil metadata and rendering `::ev`. Derive its interval from parent start + one ASCII `#` byte, through UTF-8 length of the validated qualifier spelling; assert the original slice matches. Alias itself starts at parent+3. `#:é{}` witnesses non-ASCII qualifier length. Empty maps must still expose one qualifier site.
- Synthetic coordinates are not unique to qualifiers: `#?`/`#?@` children `?`/`?@` also lack metadata (bb probe and 1.2.50 parser source). Annotate known synthetic prefixes explicitly or mark unsupported; never invent generic nil-coordinate defaults. This does not admit branch-dependent editing.
- Leading U+FEFF parses as a **token**, not trivia. Removing the BOM refusal alone adds an effective form and can break ns-first/BOF selection. Defer BOM admission until a documented leading-preamble policy, rebased offsets and actual consumer compatibility witness exist; preserve all three bytes. Do not silently ignore interior BOMs.
- Public parser/node namespaces load and the probes work in bb. The JVM dependency pin does not establish the bundled bb rewrite-clj version; pin/test the supported bb release as well, including repository minimum 1.12.209 before claiming that minimum. Avoid reloading internal parser deftypes through SCI.

Fix the proposed properties: physical roots and effective roots differ (`#_#_1 2 3` has one effective root, `3`). Reassemble `g0,f0,…,fN−1,gN`: plain `interleave gaps forms` drops the final gap. Recount equals effective-root count, not physical-root count. Only disjoint unaffected spans retain hashes and translate by delta; an enclosing body-owner span grows and changes hash. A parse-valid insertion can merge tokens or swallow a sibling, so preservation/recount must be supplemented by the retained placement/role mutants.

## 3. Speed: credible forecast, not a receipt

**For:** the ten-fixture require/parse/print probe took about 0.02 s under bb and 1.7 s with a directly assembled JVM classpath. This establishes a cheap parser entrance, not the speed of a future suite. Literal planner tests can bypass filesystem fixtures.
**Against:** today's planners are already pure; changing their dependency alone does not delete transactions, CLI launches, evidence serialization, or MCP loading. The claim that all 1,151 assertions are slow because of transactions has not been profiled. Assertion reduction is not proportional wall reduction.

Planning estimates for one serial launch per lane, on this host without contention:
| Future lane | JVM cold wall | bb cold wall |
|---|---:|---:|
| Library adapter/property suite | 2–5 s | 0.2–2 s |
| Library + both pure planner groups | 3–10 s | 1–5 s |
| Complete focused gate, retained transaction/receipt/entrance witnesses | 20–50 s | No all-bb equivalent claimed |

Against the supplied 60–100 s JVM reference, 20–50 s is plausible **only after fixture/process consolidation**, not guaranteed by the port. The broader fast suite may barely move. Measure the same namespace inventory, process count and assertion/fixture rows before/after; separate cold startup from warm execution and report repeated runs/spread. Do not compare a bb pure subset with the complete JVM gate as a speedup.

Keep transaction publication, final-stale, rollback/foreign bytes, recovery and disk-derived receipt gates in the JVM integration lane, along with actual MCP dispatch/server dependencies. Transactions are not inherently impossible in bb; this port does not establish an equivalent all-bb gate. The standalone library, pure selectors/planners and bb CLI smoke can run under bb; MCP remains JVM. Do not pull the MCP runtime transitively into the library test classpath.

## 4. Upstream: neither function as proposed, as-is

**For:** complete original-source coordinates are reusable beyond Surgeon. **Against:** per-form SHA, owner kinds, ordinals, effective-form policy and replacement authorization are application choices. A byte-array concatenator alone offers little rewrite-clj-specific value.

First issue: “Parsed synthetic qualifier nodes lack source positions; support original-source spans across normalized newlines.” Include 1.2.50, minimal `#::ev{}`/`#:é{}` and CRLF/emoji reproducers, actual nil metadata/normalized rendering, desired exclusive coordinates, and compatibility notes. Ask whether to add synthetic metadata first and an opt-in original-offset facility separately. Offer small tests/patches; no hash/owner/receipt schema. Mention conditional prefix nodes as another concrete coordinate gap, without demanding branch semantics.

Do not propose `spans` wholesale. A stripped source-position component is the candidate. Keep `splice` local initially; optionally ask about a generic source-interval edit utility after maintainer feedback, using a name distinguishable from zipper `z/splice` (which raises children). No upstream response or acceptance is assumed. The public [parser source](https://github.com/clj-commons/rewrite-clj/blob/main/src/rewrite_clj/parser.cljc) documents the parser/node entrance; exact-version behavior above is grounded in the installed jar and probes.

## 5. Port order, deletion gate and ≤580 budget

1. Freeze baseline scope, E4 at `02332a74`, both red-team corpora and named guard mutants. Specify span units, effective roots, ancestry and unchanged receipt/address conventions before code.
2. Build the standalone library and shared Unicode/newline/qualifier/literal witnesses on JVM and bb. Keep it independently green.
3. Port rename first: replace tree/byte indexing and duplicate address parse, keep role/binding/capture and candidate/inverse checks. Run E4 exact bytes, role mutants, column-one and disk-evidence witnesses.
4. Port insertion next: consume literal/nested spans, keep body/header/layout selection and parse-valid wrong-offset mutant. Admit mixed LF/CRLF with exact old-byte preservation and a deterministic inserted-newline rule. Treat tab indentation and BOM as separate admission changes; defer them if they threaten the block.
5. Consolidate pure fixtures, then transaction/boolean scenarios and entrances; delete superseded tests only against a named witness map. Re-run complete focused/fast gates and both runtime pure suites, with actual counts/wall.

**Hard deletion gate:** 1.2.50 `parse-string-all` has no exposed depth/node/work-budget option; recursive parsing happens before a postwalk can enforce a cap. Input bytes alone do not preserve the current depth/prefix protection. Do not delete `lexical!`/`shape-limit!` until a bounded parser entrance is demonstrated on both runtimes. No global `with-redefs` of parser internals in a concurrent server, and no claim that catching stack overflow is equivalent to a work bound.

Moreover, `lexical!` serves `read-request` before EDN reading, and supplies insertion literal ranges and unsupported-reader policy. Move literal discovery to spans; retain capability checks on parsed syntax; separate the shared request-reader envelope. Source parsing with rewrite-clj does not replace exactly-one-EDN protocol validation. Retaining the old preflight temporarily is explicit technical debt, not completion of its deletion. A parser-budget patch may require another block; do not build another grammar scanner inside clj-splice.

Budget the entire relocated burden, **including the new library**: ≤220 expanded assertions per verb plus ≤140 shared library/envelope/boolean assertions = ≤580 (20 verb witness groups). This revises my earlier 250+250+80 allocation to prevent library tests becoming off-budget. Count fixture rows and process launches too. Share observed false receipts across registered fields; keep `unregistered-verbs-and-fields-fail-by-name` failing under its deliberate mutant. Preserve the four E4 refusal witnesses, each parse stage, independent preservation mutants and disk-derived facts.

A reduced parity port plus consolidation is feasible in one build block, conditionally on the span adapter. The full proposal—resource-bound replacement, both ports, BOM/tab semantics and measured gates—is not a responsible one-block promise. Cut English-remedy regexes and repeated transport/transaction matrices first. Defer BOM/tab admission and upstream packaging/submission next; retain preflight if its replacement is unfinished. Never cut a unique guard mutant or false-boolean witness to hit 580; report a missed budget rather than conceal it.

## 6. Commit and least sure: three

**GO with exact changes:** richer syntax-only `spans`; interval replacement rather than whole-form-only splice; strict UTF-8/UTF-16 mapping over original bytes; corrected physical/effective/gap properties; shared library tests inside 580; rename-first port; bounded-parser deletion gate; BOM/tab admission separable; conditional timing targets. This approves building the reduced seam/port, not shipping the untouched proposal's full claims.

1. **Resource-bound replacement:** the hardest dependency. A portable parser-level budget may exceed this block; retaining preflight limits immediate simplification.
2. **Measured wall and count:** 20–50 s and ≤580 are forecasts. Consolidated fixture execution, not function count, decides whether they are met.
3. **Span compatibility:** preserving current preorder addresses and qualifying BOM/newline/synthetic-node semantics across pinned JVM and supported bb may reveal extra adapter work; upstream appetite remains unknown.

Review timestamp: 2026-09-11 04:26:27 UTC
