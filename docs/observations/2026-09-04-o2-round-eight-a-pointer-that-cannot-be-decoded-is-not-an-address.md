# O2 round eight — a pointer that cannot be decoded is not an address

2026-09-04 14:56 UTC · branch `bridge/study-ops-mcp` · from round-seven tip `f572e461`

Sol's round-seven review was NO-GO on two blocking findings, both of the same
class: **the renderer and the audit agreed with each other while disagreeing
with the text a client receives.** Round eight closes both by making the
rendered line an INJECTIVE, SINGLE-LINE function of `(path, value)`.

## Finding 2 — two distinct leaves rendered the identical line

`leaf-label` concatenated path segments with a bare `.`, so the top-level key
`"a.b"` and the nested path `[:a :b]` both spelled `a.b`. `text-line-index` is
a SET, so one rendered `a.b: <value>` line discharged both leaves: 587
declaration/audit disagreements across the allowance band.

MCP-OP-STUDY-052. Every character this syntax spends is now escaped inside a
segment — `.` (the join), `[` and `]` (the index wrap), `:` and `=` (the two
pointer/spelling separators), `/` and `~` (RFC 6901's own rule, extended
rather than replaced), and `\` so the escape decodes. One
`clojure.string/escape` pass, so no escape can be re-escaped.

**`:` and `=` are not in the reviewer's proposal and are not optional.** With
a raw `:`, the path `["a: b"]` holding `"XXXXXXXXXXXXXXXX"` renders the same
line as the path `["a"]` holding `"b: XXXXXXXXXXXXXXXX"` — the same collision
one level down. The generated family finds it; the reviewer's pair alone does
not. That is the argument for a generated witness over a reproduced one.

Receipt, before and after (the reviewer's pair):

```text
BEFORE  labels= ["a.b" "a.b"]    section= "  receipt facts · 2 of 2 rendered\n  a.b: the-same-…\n  a.b: the-same-…"
AFTER   labels= ["a~2b" "a.b"]   section= "  receipt facts · 2 of 2 rendered\n  a~2b: the-same-…\n  a.b: the-same-…"
```

## Finding 3 — a text that claimed to be complete while omitting a leaf

A receipt keyed `bad\nkey` returned a complete, fitting 761-byte public pair
whose text said `receipt facts · 10 of 10 rendered` while its structured face
held one uncarried, UNDECLARED leaf. The renderer emitted `  bad` and
`key: <value>` as two lines; the carriage predicate searched for the unsplit
whole string, which no line was.

MCP-OP-STUDY-053. `\n`, `\r`, `\t` (and `\`) are escaped in POINTERS and in
VALUES, and the line is a single line by construction, so the line the
renderer counts is the line a splitter of the published text finds.

**The indented-block rendering of a multi-line value is withdrawn**, and not
only because it split a leaf: it removed the value's BLANK lines, so `"a\n\nb"`
and `"a\nb"` rendered byte-identically at one pointer — the same-type
substitution MCP-OP-STUDY-051 forbids, one level down. The reviewer did not
have to plant that one; it was already there.

Receipt, before and after:

```text
BEFORE  label= "bad\nkey"    declared= 0  audited= 1
        section= "  receipt facts · 1 of 1 rendered\n  bad\nkey: the-distinctive-pointer-value"
AFTER   label= "bad\\nkey"   declared= 0  audited= 0
        section= "  receipt facts · 1 of 1 rendered\n  bad\\nkey: the-distinctive-pointer-value"
```

## What it costs

SIX characters. Measured by redefining `escape-pointer-segment` and
`escape-line-breaks` to `identity` on the fixed two-file `outline` batch over
this repository's own sources: 5,777 published text characters escaped
against 5,771 unescaped, at 29 bytes of headroom inside the same 32,768-byte
budget.

One witness constant moved, and NOT because of this rule. That batch's text
FLOOR drops from 6,000 to 5,000 characters. Its fixture is this repository's
own sources, so the floor is coupled to their size: 465 of the 471 characters
lost since round seven are the ~90 lines this branch added to
`mcp_inspect.clj`, which grew the receipt inside a budget the text shares.
The bound with teeth there is the headroom bound, unchanged at 2,048 and
measuring 29.

**The product-change flag is unchanged.** The `ls-tree format=text` doubling
recorded in MCP-OP-STUDY-051 still awaits Gene's explicit acceptance; round
eight neither widened it nor re-blessed a golden.

## Sabotage, at this tip

Each new witness was re-run against a `git archive` copy of THIS tip with its
own fix reverted, so the witness is shown to detect the defect in the code
that ships rather than only in the code that was replaced.

```text
sabotage A (escape-pointer-segment -> identity):  26 failures / 133 assertions
sabotage B (escape-line-breaks -> identity, line breaks out of the pointer map):
                                                 151 failures / 563 assertions
```

## Composition

`origin/MCP/main` at `c2c19691` merges CLEAN.

MEM-003 at `ec1432022bccc86074e1f19ded0070478da8f2e5` is SEVEN conflicts, down
from round seven's ten: MEM-003 absorbed the trunk in its own round seven, so
`mcp_server.clj`, `mcp_tool.clj` and `mcp_alias_migration_test.clj` are no
longer in dispute. What remains is the genuine O2/MEM overlap —
`core.clj`, `mcp_inspect.clj`, `mcp_inspect_tool.clj`, `mcp_operation.clj`,
`core_discovery_test.clj`, `mcp_inspect_tool_test.clj`, `test/run_all.clj` —
and Sol's round-seven resolution recipe still applies to every one of them.
NOT merged from here.
