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

- [x] **MCP-OP-MEM-003**: When the CLI `ls-tree` encoder encodes a repository scan, it shall consume and discard each outline while retaining no more than the bounded output result, the fixed materialisation window, and an active worker set no larger than the declared outline pool; and when it serves a continuation page it shall serve that page from the immutable manifest snapshot pinned when the cursor was issued and only after re-deriving that snapshot's address from the rows stored under it, reporting as `:returned` the record count MEASURED by the encoder that produced the page, and refusing by name a cursor it did not mint, a cursor whose snapshot this root does not hold or whose stored rows no longer prove the address they are filed under or cannot supply the slice they promised, a genuine offset past the end of that snapshot, a manifest row that does not resolve to a source file inside the scanned root — its parent directory resolving outside that root, or its leaf still existing as a directory entry that does not resolve to a regular file — a page whose pinned file — checked once, at page start — no longer held its recorded content as of that check, and a page that encoded no records while rows remained; and it shall address that snapshot by the content of its manifest together with the canonical root it was taken of, resolve every manifest row through one confined resolver at both the staleness check and the read, and authenticate a cursor's offset with a per-snapshot secret that no result publishes; and it shall DISCOVER as candidates exactly the regular files and the symlinks that resolve to regular files whose names match `*.clj`, `*.cljs` or `*.cljc` — never a directory, a dangling symlink, a FIFO or a socket — reading the discovery command's output as NUL-delimited records so that a candidate name containing a newline is counted once, never split into two candidates — and shall refuse by name, without opening it, a source that is not a regular file at read time; and a result at or under the ceiling shall be encoded whole and carry NO HASHED RECEIPT — nothing that says something was withheld — its unhashed resource meter excepted.

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

  *Amendment, 2026-09-04 (the subject of every byte-identity claim):* the
  determinism rows and the no-receipt row take the **HASHED CHANNEL** as their
  subject, not the whole result. `MCP-OP-MEM-005` requires the ls-tree receipt
  to publish the scan's own cost `scan_ms` UNCONDITIONALLY, which a COMPLETE
  result therefore also carries; a wall-clock reading cannot be byte-identical
  across two scans, and this row's falsifier is `nondeterministic:4` — the same
  defect class arriving from the other lane. Measured with both lanes composed:

  ```
  FAIL two-scans-of-an-unchanged-tree-are-byte-identical-and-pin-one-snapshot
       the two EDN results differ in exactly :scan_ms (2.888 vs 2.782)
  FAIL a-result-exactly-at-the-ceiling-is-complete
       expected: (nil? (receipt at))
       actual:   (not (nil? {:receipt {:resources {:scan_ms 7.906, :bytes_scanned 674}}}))
  ```

  Two resolutions existed and both amended a ratified gate. Narrowing MEM-005 —
  publishing the meter only on refused scans — was REJECTED: a gauge wired to
  the rare branch is a gauge nobody sees move, which is the argument MEM-005 was
  ratified on. So this row is narrowed instead, to what its own falsifiers were
  always about — **the cursor and the ceiling receipt**. "No receipt" becomes
  "no HASHED receipt": a complete result says nothing was withheld, and its
  unhashed `:resources` block meters the scan beside it. "Byte-identical
  results" becomes "byte-identical HASHED CHANNELS", and the witnesses assert
  in the same breath that the unprojected results still DIFFER — so the row
  cannot be satisfied by deleting the meter, which would satisfy both rows and
  lose MEM-005. `clj-surgeon.measured` owns the partition.

## Falsifiers

| ID | Defensible opposite to test | Required witness families |
|---|---|---|
| `MCP-OP-MEM-015` | A second parse and a discarded per-form string are cheap enough to ignore, or removing them changes the outline. | Allocated bytes per source byte for one outline on a frozen ~48 KB fixture, under a ceiling derived from the single-parse path plus 25%; a count of calls into the rewrite-clj parse entry, exactly one per file; a differential comparison of `pr-str` outlines between the previous two-parse path and the current path over every `.clj`, `.cljc`, and `.cljs` file under `src/` and `test/`. |
| `MCP-OP-MEM-005` | A file's byte count already bounds the heap of a tree-scale op, so a lexical pre-scan is redundant ceremony; or the refusal may abort the scan. | A single-pass lexical scan whose delimiter balance is zero on every `.clj`, `.cljc`, and `.cljs` file under `src/`, `test/` and `bench/` — bench/ included because it holds all 20 of this repository's `#!` shebang files, the one construct the scan could not read (proof it respects strings, regex literals, character literals, and comments); admission AT each configured ceiling — a source with exactly `max_parse_nodes` nodes and one with exactly `max_parse_depth` levels outline byte-identically to the ungated path, and `N+1` / `D+1` refuse with ZERO calls into the rewrite-clj parse entry, counted by redefining it; a refusal carrying `:reason`, `:limit`, `:observed`, and `:remedy`; a run of N consecutive one-form reader-macro prefixes (`'`, `` ` ``, `~`, `~@`, `@`, `#'`, `#_`, `#=`, `#?`, `#?@`) measured as N nesting levels, and a 710-byte `@`-tower refused on depth with zero calls into the parse entry — `^` is excluded from that family and witnessed separately below, because a bare `^^^^` run is not valid Clojure and is the one caret shape a metadata-blind scan happens to count; every file under `src/`, `test/` and `bench/` admitted under the shipped defaults, with the margin asserted; a file whose parse throws `StackOverflowError` DESPITE admission becoming the same named, counted skip rather than killing the scan, witnessed with the estimator blind to it; the scan's own cost charged in the EDN receipt's `:resources` block UNCONDITIONALLY — a meter that only reports on the rare refusal branch is dark on the ~100% of scans a regression would appear in — PARTITIONED so that the deterministic denominator `bytes_scanned` is inside the hashed channel and the wall-clock `scan_ms` is under `:measured` beside it, witnessed by two scans of an unchanged tree whose hashed channels are byte-identical WHILE both still publish a positive `scan_ms`, so neither deleting the meter nor hashing it satisfies the row; from a PER-SCAN accumulator, witnessed by two concurrent tree scans each accounting for exactly their own tree's source bytes; MALFORMED source never throwing out of the scan — every checked-in fixture plus a generated family of unbalanced and truncated shapes (unmatched open, unmatched close, close at EOF, close inside a prefix run, `#_` at EOF, unterminated string, regex, comment and character literal), each recorded as a signed `:delimiter-balance`, each ADMITTED so the reader reports the syntax error it owns, and a differential of `outline` over those same bytes against the pre-branch path; a metadata run `^:a ^:b ... x` measured as N levels rather than a constant, with a 2,810-byte two-line `^` tower refused on depth with ZERO calls into the parse entry; a tree-scale `ls-tree` over a directory containing one refused file COMPLETING, with the refusal named and counted as `parser_admission_refused` in both the text and EDN receipts and the admitted files' output unchanged. |
| `MCP-OP-MEM-003` | Retaining every outline is fine because an outline is small; or a ceiling on the result is a silent truncation and therefore worse than an unbounded read. | The battery's `held_mb` for `cli-ls-tree` at N=10,000 within `max(held_mb at N=1,000) + 2.0 MiB`; a millisecond retained-heap pair proving retention tracks the ceiling `R` and not the file count `N`, measured by forced GC with the result referenced and then released, together with an unbounded control run proving the pair measures something; a result of exactly `R` records complete — carrying no HASHED receipt — and identical to the unbounded result ON THE HASHED CHANNEL, and `R+1` candidates yielding either a continuation or, for a caller that asked for a complete result, a refusal naming `R`, the observed count, and what fits; the ceiling exercised AT its shipped server value `R = 1000`, not only at a caller-lowered fixture value; pages that concatenate to the unbounded result in the same order; a MEASURED maximum outline concurrency at or below the declared pool, taken with the scan instrumented and proved non-serial, and a materialisation window that is a fixed multiple of that pool at every corpus size; an empty scan that names what it searched and exits non-zero rather than rendering a receipt whose `:error` is nil; and a differential of both the text and EDN encodings' HASHED CHANNELS against the batch encoder over every `.clj`, `.cljc`, and `.cljs` file under `src/` and `test/`. |
| `MCP-OP-MEM-003` (cursor integrity) | A cursor is just an offset plus a cheap digest of the tree's stats, and re-deriving that digest per page is enough to prove page 2 belongs to page 1's repository. | A file whose BYTES change while path, size and mtime are preserved refusing `:stale-result-cursor` and NAMING the path — the stat-derived digest served this swap as unchanged; a cursor minted against a different root refusing `:unknown-result-cursor` rather than being served from whatever the current root contains; a forged offset on a valid cursor-id refusing `:invalid-result-cursor` under the MAC; a GENUINE cursor whose offset is past the end refusing `:result-cursor-out-of-range` with the offset and the manifest total, rather than returning an empty vector a caller reads as a complete result; a 40-digit offset and a 40-digit `:max-results` returning those typed refusals rather than throwing `NumberFormatException`; and a continuation page performing NO discovery — two pages over a 10,000-file corpus doing no glob, walk or stat of the tree, each folding the pinned manifest exactly ONCE to prove its address and RETAINING only its own slice. |
| `MCP-OP-MEM-003` (cursor addressing) | A cursor id is an opaque handle, so minting it from fresh entropy is free — identity of the manifest is the snapshot's business, not the identifier's; and a snapshot filed under an id may be served on the strength of that filename. | Two scans of an UNCHANGED tree WITHIN ONE WARM SNAPSHOT STORE producing byte-identical HASHED CHANNELS in both the text and EDN encodings, cursor included, while the unprojected EDN results still DIFFER in their measured scan cost — so the row cannot be satisfied by deleting the meter MCP-OP-MEM-005 requires — and leaving exactly ONE pinned snapshot with no build temporaries — and, against two COLD stores, the same manifest digest with DIFFERENT macs, so the qualification is witnessed rather than assumed — the falsifier's own failure mode is the battery's `nondeterministic:4`, four output hashes over five reps differing in exactly the cursor line; a tree whose content moved producing a different id, and an unmoved one the same id, so the address is a function of the tree and not of the scan; a receipt holder unable to mint a valid cursor for another offset when given EVERY mac derivable from what the receipt publishes (the id, the `:manifest_digest`, the offsets), together with the server-keyed mac for that same offset SERVING, so the refusals are the key's doing and not the offset's; and a snapshot whose stored rows no longer re-fold to the id they are filed under being REBUILT from the tree and its authenticator discarded, never served. |
| `MCP-OP-MEM-003` (serve-path integrity) | Verifying a snapshot before REUSING it is enough, because the bytes cannot change between the reuse and the serve; a continuation receipt may state what the manifest arithmetic implies, because the page always holds what the arithmetic says; and a manifest row is a path this server wrote, so it needs no confinement check. | Rows tampered to substitute one candidate for another, under an UNCHANGED cursor, REFUSING rather than serving `[m06 m01 m08 m09 m10]` with m01 standing in for m07 — the serve path resolved the snapshot with `read-meta` where `verified-snapshot` existed; a manifest that cannot supply the slice it promised refusing rather than encoding two records under a receipt claiming `:returned 5, :remaining 2` and a next cursor, with `:returned` taken from the ENCODER's own emission count so a receipt cannot outlive its page; a `..` row and an ABSOLUTE row — both inside a snapshot re-folded and re-filed so that it PASSES verification — each refusing `:unconfined-manifest-row` and NAMING the path, where the first encoded namespace `leaked.secret` from outside the scan root and the second threw `IllegalArgumentException: ... is not a relative path` out of an operation whose promise is a typed receipt; a symlinked `.clj` FILE inside the root paging exactly as a fresh scan discovers it (which is why the LEAF is lexical rather than realpath) beside a row through a symlinked DIRECTORY, `src/linkdir/secret.clj` with `src/linkdir -> OUTSIDE`, refusing `:unconfined-manifest-row` and NAMING the path where the lexical-only boundary encoded `leaked.secret`; 400 page-2 reads under a LIVE rows-file swap on a real filesystem, with no interposition, serving ZERO substituted pages, where the two-open shape served 92 of 400 under valid cursors with full receipts; a page that encodes ZERO records with rows still remaining refusing rather than minting a next cursor at its own offset; and a cursor from an identical TWIN checkout refusing `:unknown-result-cursor` rather than `:invalid-result-cursor`, because the canonical root is folded into the manifest address. |
| `MCP-OP-MEM-003` (discovery predicate) | A confinement guard may reason about what `find -type f` produces, because a source-file scan obviously lists only files; so a manifest row whose leaf names a DIRECTORY is a shape only a tampered manifest can hold, and refusing it costs nothing. | The discovery command MEASURED both ways on a tree of 12 `.clj` files plus a DIRECTORY named `src/mydir.clj` — 13 paths without a `-type` predicate, 12 with `( -type f -o ( -type l -xtype f ) )` — and that UNTAMPERED tree paging to exhaustion, its final page SERVING the last two files where the no-`-type` build refused `:unconfined-manifest-row` naming an in-root path and lost them; ONE tree carrying a `*.clj` DIRECTORY, a symlinked FILE, a symlinked DIRECTORY and a DANGLING symlink, walked to exhaustion and equal to the unbounded scan RECORD FOR RECORD, with exactly the symlinked file admitted; at least one `:unconfined-manifest-row` reached through a row DISCOVERY ITSELF produced — `src/fixt` becoming a symlink out of the root between the pin and the page, with byte-identical content so staleness cannot be what refused — rather than through a rewritten manifest, because a suite whose every confinement witness tampers with the manifest stays green while the guard is broken for real trees; a DANGLING-symlink leaf REFUSING rather than spending a page slot on a typed error record, beside a legitimately DELETED file still reaching `:stale-result-cursor`, so the predicate's line between a tamper and a deletion is witnessed on both sides; and a tree containing a FIFO named `*.clj` scanning to COMPLETION under a deadline, where `open(2)` on the FIFO previously blocked forever and survived SIGTERM, with the encoder's own source open refusing `:unreadable-source` on a FIFO while a symlinked regular file still outlines. |
| `MCP-OP-MEM-003` (source read window) | `:stale-result-cursor` is a read-time seal, so a page's files hold exactly their pinned bytes for as long as the page is being served. | A swapper thread rewriting an in-root, already-pinned SOURCE file between the page's ONE staleness check and the encoder's own later reopen of that same file, over 400 page-2 reads on a real filesystem with no interposition, serving the file's CURRENT (post-swap) bytes under a clean receipt reporting no staleness in 15–19 of 400 — a stated boundary, not a regression: the served path is always the pinned path and always inside the scanned root (never a different file, never one outside the root), and the rate is unchanged from the prior worktree (14 of 400), so the window is pre-existing. This is the documented cost of checking the pin ONCE, at page start, rather than re-sealing it at the encoder's reopen. |
