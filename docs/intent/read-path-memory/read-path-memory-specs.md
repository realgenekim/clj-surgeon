---
parent: read-path-memory-design
prefix: MCP-OP-MEM
status: implemented
---

# Read-Path Memory Bounds Specifications

This file is the stable intent registry for the read-path memory leaf. IDs are
never reused. The status marker records whether the current code and tests
witness the requirement.

IDs in the `MCP-OP-MEM` series are allocated across several lanes. This leaf
owns only the IDs listed below; the transaction-journal and memory-battery
lanes own their own rows in their own leaves.

## Outline read path

- [x] **MCP-OP-MEM-015**: When clj-surgeon outlines a source file, it shall parse the file once and shall not construct per-form source text that the outline does not return.
- [x] **MCP-OP-MEM-005**: Before allocating a full rewrite-clj tree, the clj-surgeon read path shall reject inputs whose lexical node estimate or nesting depth exceeds the server parser ceiling, where nesting depth counts structural delimiters and the reader-macro prefixes `'`, `` ` ``, `~`, `~@`, `@`, `^`, `#'`, `#_`, `#=`, `#?` and `#?@` — `^` as the two-form prefix it is — and shall never itself throw on malformed source, which belongs to the parser.

## Falsifiers

| ID | Defensible opposite to test | Required witness families |
|---|---|---|
| `MCP-OP-MEM-015` | A second parse and a discarded per-form string are cheap enough to ignore, or removing them changes the outline. | Allocated bytes per source byte for one outline on a frozen ~48 KB fixture, under a ceiling derived from the single-parse path plus 25%; a count of calls into the rewrite-clj parse entry, exactly one per file; a differential comparison of `pr-str` outlines between the previous two-parse path and the current path over every `.clj`, `.cljc`, and `.cljs` file under `src/` and `test/`. |
| `MCP-OP-MEM-005` | A file's byte count already bounds the heap of a tree-scale op, so a lexical pre-scan is redundant ceremony; or the refusal may abort the scan. | A single-pass lexical scan whose delimiter balance is zero on every `.clj`, `.cljc`, and `.cljs` file under `src/`, `test/` and `bench/` — bench/ included because it holds all 20 of this repository's `#!` shebang files, the one construct the scan could not read (proof it respects strings, regex literals, character literals, and comments); admission AT each configured ceiling — a source with exactly `max_parse_nodes` nodes and one with exactly `max_parse_depth` levels outline byte-identically to the ungated path, and `N+1` / `D+1` refuse with ZERO calls into the rewrite-clj parse entry, counted by redefining it; a refusal carrying `:reason`, `:limit`, `:observed`, and `:remedy`; a run of N consecutive one-form reader-macro prefixes (`'`, `` ` ``, `~`, `~@`, `@`, `#'`, `#_`, `#=`, `#?`, `#?@`) measured as N nesting levels, and a 710-byte `@`-tower refused on depth with zero calls into the parse entry — `^` is excluded from that family and witnessed separately below, because a bare `^^^^` run is not valid Clojure and is the one caret shape a metadata-blind scan happens to count; every file under `src/`, `test/` and `bench/` admitted under the shipped defaults, with the margin asserted; a file whose parse throws `StackOverflowError` DESPITE admission becoming the same named, counted skip rather than killing the scan, witnessed with the estimator blind to it; the scan's own cost charged as `scan_ms` WITH its `bytes_scanned` denominator in the EDN receipt's `:resources` block UNCONDITIONALLY — a meter that only reports on the rare refusal branch is dark on the ~100% of scans a regression would appear in — from a PER-SCAN accumulator, witnessed by two concurrent tree scans each accounting for exactly their own tree's source bytes; MALFORMED source never throwing out of the scan — every checked-in fixture plus a generated family of unbalanced and truncated shapes (unmatched open, unmatched close, close at EOF, close inside a prefix run, `#_` at EOF, unterminated string, regex, comment and character literal), each recorded as a signed `:delimiter-balance`, each ADMITTED so the reader reports the syntax error it owns, and a differential of `outline` over those same bytes against the pre-branch path; a metadata run `^:a ^:b ... x` measured as N levels rather than a constant, with a 2,810-byte two-line `^` tower refused on depth with ZERO calls into the parse entry; a tree-scale `ls-tree` over a directory containing one refused file COMPLETING, with the refusal named and counted as `parser_admission_refused` in both the text and EDN receipts and the admitted files' output unchanged. |
