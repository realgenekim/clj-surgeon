# O2 round ten — a segment that renders as nothing erases the position it names

*forge-anvil, 2026-09-04 16:43 UTC. Branch `bridge/study-ops-mcp`, from `e7bc588a`.
Answering Sol's round-nine review (`docs/observations/study-ops-o2-round9-review-sol.md`):
one BLOCKING finding and two non-blocking ones.*

## §1 (BLOCKING) — the one segment an escape map cannot reach

`clojure.string/escape` maps CHARACTERS. The empty string has none, so the
EMPTY path segment escaped to zero characters and the position it occupied was
simply **erased**: the distinct JSON paths `["" 0]` — an array under the empty
top-level key — and `[0]` — the integer top-level key, published as the object
key `"0"` — both spelled `[0]`.

That is not an abstract collision. At the reviewer's operating point
`fit-public-result` returns an ordinary, FITTING 32,731-byte result at evidence
allowance 237 whose text renders one `[0]` line, names the other `[0]` on its
`dropped:` line, and therefore declares 32 dropped where an audit of that same
text finds 31:

```text
padding 31640: a 32731-byte public result at evidence allowance 237 declared 32
dropped against 31 audited; text "…receipt facts · 1 of 33 rendered…
  dropped: [0], tail7, tail17, source_character_count, tail4, tail12,
  elapsed_ms, read_complete (+24 more)
  [0]: the-same-distinctive-value-rendered-twice…"
```

The committed 5,219-path family could not see it because its alphabet had no
empty segment. **A generated family witnesses only the characters it is given** —
that is the transferable lesson, and it is why the alphabet, not only the
encoder, is part of the fix.

The empty segment is now spelled `~7`. `~` is escaped to `~0` inside every
segment, so no non-empty segment can produce `~7`; the encoding stays injective
and decodable. The alphabet gains `""`, `"00"`, `"-1"`, whitespace-only
segments and U+2028/U+2029/U+0085: **16,275 paths, 16,275 pointers** (16,223
before the fix), and the reviewer's public rung is declared-equals-audited
across the band and at its exact operating point.

RESIDUAL, named so it is not rediscovered: `:a` and `"a"`, and `nil` and `""`,
still spell ONE pointer **deliberately**. `structuredContent` publishes
`{:a 1}` and `{"a" 1}` as the same JSON object key, and `{nil 1}` as `{"": 1}`
— `(json/generate-string {nil 1 "" 2})` is `{"":1,"":2}` — so they are the same
JSON path, and a pointer that separated them would name an address the caller
cannot address.

## §2 — a line boundary is whatever a splitter calls one

MCP-OP-STUDY-053 escaped `\n`, `\r` and `\t` and left the rest of the class
raw. `java.util.regex`'s `\R` — which is what a caller's splitter is — breaks
on U+0085 (NEL), U+2028, U+2029, the vertical tab and the form feed as well.

The fix is the CLASS, not the two characters the review named: both escape maps
now carry VT, FF, NEL, U+2028 and U+2029 alongside `\n`, `\r` and `\t`, each
spelled `\uXXXX`, so the decoder rule becomes *after `\` comes `\`, `n`, `r`,
`t`, or `u` and four hex digits.* The witness adds a Unicode-aware `\R` split
of every rendered line and VALUE-side injectivity over `\r\n`, `\n`, `\r`, a
literal backslash-n, tab, literal backslash-t, VT, FF, NEL, U+2028, U+2029 and
a literal backslash-u2028.

## §6 — a cost witness that reads the code it measures is not a fixture

The two-file cost batch read `src/clj_surgeon/mcp_inspect.clj` and
`src/clj_surgeon/mcp_inspect_tool.clj`. The receipt and the text share one
32,768-byte budget, so **every commit to the sources under measurement moved
the floor**, and it was lowered twice for a reason that was never the escaping:
465 of the 471 characters lost between round seven and round eight were this
branch's own ~90 added lines.

`review-batch-files` now names two FROZEN copies under
`test-fixtures/study/cost-batch/`, verbatim but for a renamed `ns` form so the
repository holds no duplicate namespace. Re-derived on that fixture:

```text
escaped=   {:text_chars 5549, :published_bytes 32749, :headroom 19, :limit 5216}
unescaped= {:text_chars 5541, :published_bytes 32741, :headroom 27, :limit 5205}
```

**EIGHT characters** for the whole of MCP-OP-STUDY-052 and MCP-OP-STUDY-053
together. The text floor is re-derived at 5,400 — 149 below the measurement,
so a rendering change that costs a few bytes is not a failure and one that
costs a rung is. The headroom bound is unchanged at 2,048 and measures 19.

**The product-change flag is unchanged.** The `ls-tree format=text` doubling
recorded in MCP-OP-STUDY-051 still awaits Gene's explicit acceptance; round ten
neither widened it nor re-blessed a golden.

## Sabotage, at this tip

Each fix reverted on a `git archive` copy of THIS tip, so the witness is shown
to detect the defect in the code that ships.

```text
sabotage A (empty-segment encoding reverted):        7 failures / 152 assertions
sabotage B (Unicode line-boundary escapes removed
            from both maps):                        23 failures / 1354 assertions
```

## Composition

`origin/MCP/main` at `e6a11a7f` merges CLEAN.

MEM-003 at `a2a15cc0` is the same SEVEN conflicts as round eight —
`core.clj`, `mcp_inspect.clj`, `mcp_inspect_tool.clj`, `mcp_operation.clj`,
`core_discovery_test.clj`, `mcp_inspect_tool_test.clj`, `test/run_all.clj` —
and Sol's round-seven resolution recipe still applies to every one of them.
NOT merged from here.
