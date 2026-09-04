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

- [x] **MCP-OP-MEM-015**: When clj-surgeon outlines a source file with its default (non-`include_string_symbols`) projection, it shall parse the file once and shall not construct per-form source text that the outline does not return. `include_string_symbols` is explicitly out of scope: it deliberately builds and reparses each selected form's own source text by design.
- [x] **MCP-OP-MEM-005**: Before allocating a full rewrite-clj tree, the clj-surgeon read path shall reject inputs whose lexical node estimate or nesting depth exceeds the server parser ceiling, where nesting depth counts structural delimiters and the reader-macro prefixes `'`, `` ` ``, `~`, `~@`, `@`, `^`, `#'`, `#_`, `#=`, `#?` and `#?@` — `^` as the two-form prefix it is — and shall never itself throw on malformed source, which belongs to the parser; and it shall charge the scan's own cost in the tree-scale receipt UNCONDITIONALLY, with the deterministic denominator `bytes_scanned` on the receipt's HASHED channel and the wall-clock `scan_ms` on the receipt's MEASURED channel — published beside the hashed channel, never inside it.

  *Amendment, 2026-09-04 (where a measured field may live):* the meter is
  unchanged in what it measures and in when it is published; only its CHANNEL
  is now stated. The original wording put `scan_ms` "in the EDN receipt's
  `:resources` block UNCONDITIONALLY" and said nothing about hashing, because
  at the time nothing hashed a receipt. Two ratified rows then did:
  `MCP-OP-MEM-011` hashes an operation's whole result against an unbounded
  reference, and `MCP-OP-MEM-003` requires two scans of an unchanged tree to be
  byte-identical. A wall-clock reading cannot satisfy either. Measured, two
  back-to-back EDN scans of one unchanged tree:

  ```
  A receipt {:resources {:scan_ms 44.081, :bytes_scanned 111183}}
  B receipt {:resources {:scan_ms 23.054, :bytes_scanned 111183}}
  records equal (receipt dropped)? true
  ```

  — twelve `reference-mismatch` FAIL lines on `cli-ls-tree` alone, and
  `nondeterministic:4`: four output hashes over five reps of ONE operation on
  ONE unchanged corpus. **The ruling: a measured wall-clock field can never
  live inside a parity hash.** So the receipt is PARTITIONED rather than the
  meter narrowed. `bytes_scanned` is a count — deterministic, hashed, and a
  change in it is a real regression a parity line must catch. `scan_ms` moves
  under the well-known `:measured` key, which every determinism, parity and
  byte-identity row drops before it looks. The alternative — publishing the
  meter only on refused scans — was rejected for the reason MEM-005 was argued
  in the first place: a gauge wired to the rare branch is a gauge nobody sees
  move. `clj-surgeon.measured` owns the partition; the text encodings carry it
  as a labelled line prefix, because text has no keys.

## Falsifiers

| ID | Defensible opposite to test | Required witness families |
|---|---|---|
| `MCP-OP-MEM-015` | A second parse and a discarded per-form string are cheap enough to ignore, or removing them changes the outline. | Allocated bytes per source byte for one outline on a frozen ~48 KB fixture, under a ceiling derived from the single-parse path plus 25%; a count of calls into the rewrite-clj parse entry, exactly one per file; a differential comparison of `pr-str` outlines between the previous two-parse path and the current path over every `.clj`, `.cljc`, and `.cljs` file under `src/` and `test/`. |
| `MCP-OP-MEM-005` | A file's byte count already bounds the heap of a tree-scale op, so a lexical pre-scan is redundant ceremony; or the refusal may abort the scan. | A single-pass lexical scan whose delimiter balance is zero on every `.clj`, `.cljc`, and `.cljs` file under `src/`, `test/` and `bench/` — bench/ included because it holds all 20 of this repository's `#!` shebang files, the one construct the scan could not read (proof it respects strings, regex literals, character literals, and comments); admission AT each configured ceiling — a source with exactly `max_parse_nodes` nodes and one with exactly `max_parse_depth` levels outline byte-identically to the ungated path, and `N+1` / `D+1` refuse with ZERO calls into the rewrite-clj parse entry, counted by redefining it; a refusal carrying `:reason`, `:limit`, `:observed`, and `:remedy`; a run of N consecutive one-form reader-macro prefixes (`'`, `` ` ``, `~`, `~@`, `@`, `#'`, `#_`, `#=`, `#?`, `#?@`) measured as N nesting levels, and a 710-byte `@`-tower refused on depth with zero calls into the parse entry — `^` is excluded from that family and witnessed separately below, because a bare `^^^^` run is not valid Clojure and is the one caret shape a metadata-blind scan happens to count; every file under `src/`, `test/` and `bench/` admitted under the shipped defaults, with the margin asserted; a file whose parse throws `StackOverflowError` DESPITE admission becoming the same named, counted skip rather than killing the scan, witnessed with the estimator blind to it; the scan's own cost charged in the EDN receipt's `:resources` block UNCONDITIONALLY — a meter that only reports on the rare refusal branch is dark on the ~100% of scans a regression would appear in — PARTITIONED so that the deterministic denominator `bytes_scanned` is inside the hashed channel and the wall-clock `scan_ms` is under `:measured` beside it, witnessed by two scans of an unchanged tree whose hashed channels are byte-identical WHILE both still publish a positive `scan_ms`, so neither deleting the meter nor hashing it satisfies the row; from a PER-SCAN accumulator, witnessed by two concurrent tree scans each accounting for exactly their own tree's source bytes; MALFORMED source never throwing out of the scan — every checked-in fixture plus a generated family of unbalanced and truncated shapes (unmatched open, unmatched close, close at EOF, close inside a prefix run, `#_` at EOF, unterminated string, regex, comment and character literal), each recorded as a signed `:delimiter-balance`, each ADMITTED so the reader reports the syntax error it owns, and a differential of `outline` over those same bytes against the pre-branch path; a metadata run `^:a ^:b ... x` measured as N levels rather than a constant, with a 2,810-byte two-line `^` tower refused on depth with ZERO calls into the parse entry; a tree-scale `ls-tree` over a directory containing one refused file COMPLETING, with the refusal named and counted as `parser_admission_refused` in both the text and EDN receipts and the admitted files' output unchanged. |
