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
- [x] **MCP-OP-MEM-005**: Before allocating a full rewrite-clj tree, clj-surgeon shall reject inputs whose lexical node estimate or nesting depth exceeds the server parser ceiling.
- [x] **MCP-OP-MEM-003**: When `ls-tree` scans a repository, it shall consume and discard each outline while retaining no more than the active worker set and the bounded output encoder.

## Falsifiers

| ID | Defensible opposite to test | Required witness families |
|---|---|---|
| `MCP-OP-MEM-015` | A second parse and a discarded per-form string are cheap enough to ignore, or removing them changes the outline. | Allocated bytes per source byte for one outline on a frozen ~48 KB fixture, under a ceiling derived from the single-parse path plus 25%; a count of calls into the rewrite-clj parse entry, exactly one per file; a differential comparison of `pr-str` outlines between the previous two-parse path and the current path over every `.clj`, `.cljc`, and `.cljs` file under `src/` and `test/`. |
| `MCP-OP-MEM-005` | A file's byte count already bounds the heap of a tree-scale op, so a lexical pre-scan is redundant ceremony; or the refusal may abort the scan. | A single-pass lexical scan whose delimiter balance is zero on every `.clj`, `.cljc`, and `.cljs` file under `src/` and `test/` (proof it respects strings, regex literals, character literals, and comments); admission AT each configured ceiling — a source with exactly `max_parse_nodes` nodes and one with exactly `max_parse_depth` levels outline byte-identically to the ungated path, and `N+1` / `D+1` refuse with ZERO calls into the rewrite-clj parse entry, counted by redefining it; a refusal carrying `:reason`, `:limit`, `:observed`, and `:remedy`; every file under `src/` and `test/` admitted under the shipped defaults, with the margin asserted; a tree-scale `ls-tree` over a directory containing one refused file COMPLETING, with the refusal named and counted as `parser_admission_refused` in both the text and EDN receipts and the admitted files' output unchanged. |
| `MCP-OP-MEM-003` | Retaining every outline is fine because an outline is small; or a ceiling on the result is a silent truncation and therefore worse than an unbounded read. | The battery's `held_mb` for `cli-ls-tree` at N=10,000 within `max(held_mb at N=1,000) + 2.0 MiB`; a millisecond retained-heap pair proving retention tracks the ceiling `R` and not the file count `N`, measured by forced GC with the result referenced and then released, together with an unbounded control run proving the pair measures something; a result of exactly `R` records complete and identical to the unbounded result, and `R+1` candidates yielding either a continuation whose cursor carries offset and manifest digest or, for a caller that asked for a complete result, a refusal naming `R`, the observed count, and what fits; pages that concatenate to the unbounded result in the same order; a cursor refused after the tree changed; and a differential of both the text and EDN encodings against the batch encoder over every `.clj`, `.cljc`, and `.cljs` file under `src/` and `test/`. |
