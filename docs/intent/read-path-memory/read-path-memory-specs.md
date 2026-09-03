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
- [x] **MCP-OP-MEM-003**: When the CLI `ls-tree` encoder encodes a repository scan, it shall consume and discard each outline while retaining no more than the bounded output result, the fixed materialisation window, and an active worker set no larger than the declared outline pool; and when it serves a continuation page it shall serve that page from the immutable manifest snapshot pinned when the cursor was issued and only after re-deriving that snapshot's address from the rows stored under it, reporting as `:returned` the record count MEASURED by the encoder that produced the page, and refusing by name a cursor it did not mint, a cursor whose snapshot this root does not hold or whose stored rows no longer prove the address they are filed under or cannot supply the slice they promised, a genuine offset past the end of that snapshot, a manifest row whose parent directory does not resolve inside the scanned root, and a page whose pinned file no longer holds its recorded content; and it shall address that snapshot by the content of its manifest together with the canonical root it was taken of, resolve every manifest row through one confined resolver at both the staleness check and the read, and authenticate a cursor's offset with a per-snapshot secret that no result publishes.

  *Scope, 2026-09-03:* the subject is the **CLI `ls-tree` encoder**, not
  `ls-tree` at large. The earlier wording ("when `ls-tree` scans a repository")
  claimed more than the code delivers: discovery still retains one path string
  per candidate, an N-sized collection this row does not bound (Sol, finding
  12). The MCP study-ops entrance is likewise out of scope and must adopt the
  same ceiling and cursor shape in its own lane.

  *Amendment, 2026-09-03 (cursor addressing):* the snapshot is addressed by
  its manifest digest — SHA-256 over the ordered rows' `position ⇥
  project-index ⇥ path ⇥ content-digest`, seeded with the projection version —
  and not by fresh entropy. The random `cursor-id` this replaces made the
  result NONDETERMINISTIC (the battery's `nondeterministic:4`: four output
  hashes over five reps of one operation on one corpus, differing in exactly
  the cursor line) and pinned a new snapshot on every scan of an unchanged
  tree. Two boundaries travel with it. **Stat is not in the address** — size
  and mtime are not identity, and folding mtime would reintroduce the
  nondeterminism for a touched-but-unchanged tree. **The MAC key is not the
  address** — content-addressing publishes the id as the receipt's
  `:manifest_digest`, so the authenticator stays keyed on a per-snapshot
  random secret written only inside the snapshot file; an earlier brief's
  `sha256(cursor-id ‖ offset ‖ snapshot-digest)` is forgeable by any holder of
  a receipt.

  *Amendment, 2026-09-03 (row confinement, and the one open):* the confinement
  boundary resolves a row's PARENT DIRECTORY through symlinks and takes the
  final component LEXICALLY, and the requirement says so because the two halves
  are measured facts about discovery, not a compromise. `find-clj-files` shells
  to plain `find` with no `-L`: it LISTS a symlinked `.clj` file whose target is
  outside the root, and it NEVER DESCENDS a symlinked directory. Resolving the
  leaf would therefore refuse on page 2 what page 1 encoded — a page-1/page-2
  divergence introduced by the guard itself — while leaving the parent lexical
  admitted `src/linkdir/secret.clj` with `src/linkdir -> OUTSIDE`, a row no scan
  can produce, which round four's review had SERVED as `leaked.secret` inside a
  snapshot re-folded so that it passed verification. The guard refuses what
  discovery can never produce and defers to discovery on what it can.

  *Amendment, 2026-09-03 (one open):* a page's rows are folded, counted and
  SLICED in a single open of the rows file, and the slice is served only when
  those same bytes prove the address. Verifying in one open and slicing in a
  second is not a hairline window but the whole verifying fold, which is O(N)
  in the manifest and therefore GROWS with the corpus: 400 page-2 reads under a
  live rows-file swap, on a real filesystem with no interposition, served 92
  substituted pages under valid cursors with full receipts. The property is not
  atomicity of a file read — no such thing exists — it is that the digest is
  taken over exactly the bytes the caller is served.

## Falsifiers

| ID | Defensible opposite to test | Required witness families |
|---|---|---|
| `MCP-OP-MEM-015` | A second parse and a discarded per-form string are cheap enough to ignore, or removing them changes the outline. | Allocated bytes per source byte for one outline on a frozen ~48 KB fixture, under a ceiling derived from the single-parse path plus 25%; a count of calls into the rewrite-clj parse entry, exactly one per file; a differential comparison of `pr-str` outlines between the previous two-parse path and the current path over every `.clj`, `.cljc`, and `.cljs` file under `src/` and `test/`. |
| `MCP-OP-MEM-005` | A file's byte count already bounds the heap of a tree-scale op, so a lexical pre-scan is redundant ceremony; or the refusal may abort the scan. | A single-pass lexical scan whose delimiter balance is zero on every `.clj`, `.cljc`, and `.cljs` file under `src/` and `test/` (proof it respects strings, regex literals, character literals, and comments); admission AT each configured ceiling — a source with exactly `max_parse_nodes` nodes and one with exactly `max_parse_depth` levels outline byte-identically to the ungated path, and `N+1` / `D+1` refuse with ZERO calls into the rewrite-clj parse entry, counted by redefining it; a refusal carrying `:reason`, `:limit`, `:observed`, and `:remedy`; every file under `src/` and `test/` admitted under the shipped defaults, with the margin asserted; a tree-scale `ls-tree` over a directory containing one refused file COMPLETING, with the refusal named and counted as `parser_admission_refused` in both the text and EDN receipts and the admitted files' output unchanged. |
| `MCP-OP-MEM-003` | Retaining every outline is fine because an outline is small; or a ceiling on the result is a silent truncation and therefore worse than an unbounded read. | The battery's `held_mb` for `cli-ls-tree` at N=10,000 within `max(held_mb at N=1,000) + 2.0 MiB`; a millisecond retained-heap pair proving retention tracks the ceiling `R` and not the file count `N`, measured by forced GC with the result referenced and then released, together with an unbounded control run proving the pair measures something; a result of exactly `R` records complete and identical to the unbounded result, and `R+1` candidates yielding either a continuation or, for a caller that asked for a complete result, a refusal naming `R`, the observed count, and what fits; the ceiling exercised AT its shipped server value `R = 1000`, not only at a caller-lowered fixture value; pages that concatenate to the unbounded result in the same order; a MEASURED maximum outline concurrency at or below the declared pool, taken with the scan instrumented and proved non-serial, and a materialisation window that is a fixed multiple of that pool at every corpus size; an empty scan that names what it searched and exits non-zero rather than rendering a receipt whose `:error` is nil; and a differential of both the text and EDN encodings against the batch encoder over every `.clj`, `.cljc`, and `.cljs` file under `src/` and `test/`. |
| `MCP-OP-MEM-003` (cursor integrity) | A cursor is just an offset plus a cheap digest of the tree's stats, and re-deriving that digest per page is enough to prove page 2 belongs to page 1's repository. | A file whose BYTES change while path, size and mtime are preserved refusing `:stale-result-cursor` and NAMING the path — the stat-derived digest served this swap as unchanged; a cursor minted against a different root refusing `:unknown-result-cursor` rather than being served from whatever the current root contains; a forged offset on a valid cursor-id refusing `:invalid-result-cursor` under the MAC; a GENUINE cursor whose offset is past the end refusing `:result-cursor-out-of-range` with the offset and the manifest total, rather than returning an empty vector a caller reads as a complete result; a 40-digit offset and a 40-digit `:max-results` returning those typed refusals rather than throwing `NumberFormatException`; and a continuation page performing NO discovery — two pages over a 10,000-file corpus folding one page of rows each, not the whole manifest per page. |
| `MCP-OP-MEM-003` (cursor addressing) | A cursor id is an opaque handle, so minting it from fresh entropy is free — identity of the manifest is the snapshot's business, not the identifier's; and a snapshot filed under an id may be served on the strength of that filename. | Two scans of an UNCHANGED tree WITHIN ONE WARM SNAPSHOT STORE producing byte-identical results in both the text and EDN encodings, cursor included, and leaving exactly ONE pinned snapshot with no build temporaries — and, against two COLD stores, the same manifest digest with DIFFERENT macs, so the qualification is witnessed rather than assumed — the falsifier's own failure mode is the battery's `nondeterministic:4`, four output hashes over five reps differing in exactly the cursor line; a tree whose content moved producing a different id, and an unmoved one the same id, so the address is a function of the tree and not of the scan; a receipt holder unable to mint a valid cursor for another offset when given EVERY mac derivable from what the receipt publishes (the id, the `:manifest_digest`, the offsets), together with the server-keyed mac for that same offset SERVING, so the refusals are the key's doing and not the offset's; and a snapshot whose stored rows no longer re-fold to the id they are filed under being REBUILT from the tree and its authenticator discarded, never served. |
| `MCP-OP-MEM-003` (serve-path integrity) | Verifying a snapshot before REUSING it is enough, because the bytes cannot change between the reuse and the serve; a continuation receipt may state what the manifest arithmetic implies, because the page always holds what the arithmetic says; and a manifest row is a path this server wrote, so it needs no confinement check. | Rows tampered to substitute one candidate for another, under an UNCHANGED cursor, REFUSING rather than serving `[m06 m01 m08 m09 m10]` with m01 standing in for m07 — the serve path resolved the snapshot with `read-meta` where `verified-snapshot` existed; a manifest that cannot supply the slice it promised refusing rather than encoding two records under a receipt claiming `:returned 5, :remaining 2` and a next cursor, with `:returned` taken from the ENCODER's own emission count so a receipt cannot outlive its page; a `..` row and an ABSOLUTE row — both inside a snapshot re-folded and re-filed so that it PASSES verification — each refusing `:unconfined-manifest-row` and NAMING the path, where the first encoded namespace `leaked.secret` from outside the scan root and the second threw `IllegalArgumentException: ... is not a relative path` out of an operation whose promise is a typed receipt; a symlinked `.clj` FILE inside the root paging exactly as a fresh scan discovers it (which is why the LEAF is lexical rather than realpath) beside a row through a symlinked DIRECTORY, `src/linkdir/secret.clj` with `src/linkdir -> OUTSIDE`, refusing `:unconfined-manifest-row` and NAMING the path where the lexical-only boundary encoded `leaked.secret`; 400 page-2 reads under a LIVE rows-file swap on a real filesystem, with no interposition, serving ZERO substituted pages, where the two-open shape served 92 of 400 under valid cursors with full receipts; a page that encodes ZERO records with rows still remaining refusing rather than minting a next cursor at its own offset; and a cursor from an identical TWIN checkout refusing `:unknown-result-cursor` rather than `:invalid-result-cursor`, because the canonical root is folded into the manifest address. |
