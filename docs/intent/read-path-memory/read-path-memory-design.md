# Read-Path Memory Bounds

Status: draft LLD; `MCP-OP-MEM-015`, `MCP-OP-MEM-005` and `MCP-OP-MEM-003` implemented.

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

---

# Parser admission (`MCP-OP-MEM-005`)

Status: implemented 2026-09-03.

## Context

The memory battery's round-2 adversarial arms (2026-09-03) showed heap sized by
a file's **shape**, on corpora 20x and 364x smaller than its 10,000-file tree:
`cli-ls-tree` peaked at 386.4 MB on ONE 1.9 MiB file and 285.7 MB on ONE 111 KB
file nested 300 `{:k [` levels deep, against a 248 MB budget.

Isolating those two cells to one `outline-source` call per JVM
(`make memory-red`, anvil, 2026-09-03) found something the battery could not
see, because the battery runs its adversarial arms last:

| shape | -Xmx | warm-ups | outcome | peak | source bytes |
|---|---|---|---|---|---|
| nested | 512m | 0 | **StackOverflowError in 42 ms** | 33.4 MB | 111,183 |
| nested | 512m | 200 | completed | **312.4 MB** | 111,183 |
| giant | 128m | 0 | **OutOfMemoryError** | 126.6 MB | 1,992,594 |
| giant | 512m | 0 | completed | 339.9 MB | 1,992,594 |

The deep file does not merely cost heap. On a **cold** JVM the rewrite-clj parse
recurses once per nesting level in interpreted frames and overflows the default
1 MB stack. `core/safe-outline` catches `Exception`, not `Error`, so a single
such file **kills the entire `ls-tree` scan** rather than one entry — verified
directly against `run-ls-tree`. Once the parser's hot path is JIT-compiled the
same file at the same `-Xmx` completes instead, consuming 2,876x its own source.
Which of the two failures a caller gets depends on JIT state.

## Boundary

- **This is a shape ceiling, not a size ceiling.** The per-file and aggregate
  BYTE ceilings are `MCP-OP-MEM-002`'s, and they run first. MEM-005 exists for
  inputs whose node count or nesting is pathological *relative to their bytes* —
  a file the byte ceiling would happily admit. The 1.9 MiB `giant` cell is
  MEM-002's subject; MEM-005 refuses it today only as a second guard, because
  MEM-002's byte ceiling does not exist yet, and will stop being the refusing
  control once it does.
- **It is not a resident-memory bound**, and it makes no promise about peak used
  heap, which is heap-size dependent under G1. Its promise is that a tree is
  never constructed for an input above the ceiling.
- **It does not change outline content for admitted files.** A file at exactly
  `max_parse_nodes` or exactly `max_parse_depth` projects byte-identically to
  the ungated path.
- **A refusal is a per-file skip, never an aborted scan.** A tree-scale
  operation completes, and names and counts the refusal as
  `parser_admission_refused` in its receipt.
- The estimate is lexical and single-pass over the raw string. It is an
  *estimate*: measured against Sol's rewrite-clj node count for
  `src/clj_surgeon/intent_transaction.clj` (21,996 nodes over 126,596 bytes) it
  reports 19,528 — about 11% low, because it counts a whitespace run as one node
  where rewrite-clj splits whitespace from newlines. The ceilings are derived
  from measurements of *this* estimator, so the estimator's offset is not a
  correctness question; a drift in it is caught by the margin witnesses.

## The ceilings, and where the numbers come from

`max_parse_nodes = 200,000` · `max_parse_depth = 150`

**Depth.** Measured cold-JVM ladder at `-Xmx512m`, default 1 MB stack, one
bracket tower per file: 100, 200, 300, 400, 410, 425 and 440 levels complete;
460 overflowed on one of two runs; 480 and 600 overflowed on both. The lowest
observed cold StackOverflowError is **460**. The deepest of the 163
`.clj`/`.cljc`/`.cljs` files under `src/` and `test/` is **22**
(`intent_transaction.clj`); the battery's default corpus reaches 8 and its cljc
corpus 5. 150 sits **6.8x above** the deepest real file and **3.07x below** the
lowest observed overflow. The 3x factor is not decoration: the overflow point
moves with JIT state, thread stack size, and JVM, and the *cold* threshold is
the lower of the two branches, so the ceiling is placed against the cold one.
The adversarial `nested` file is 601 levels — 4.0x over.

**Nodes.** Two independent bounds, and 200,000 is the value that satisfies both.

*Upper bound — it must never pre-empt MEM-002 on a file the byte ceiling
admits.* The highest node density measured over every corpus in evidence is
273.6 nodes/KiB (the battery's synthetic filler); this repository's own sources
run 102.6–189.9. At MEM-002's per-file byte ceiling of ~512 KiB (Opus's consult,
section 5) the densest admitted source yields at most 512 x 273.6 = **140,288**
nodes. 200,000 leaves 1.42x of headroom above that, so an ordinary file the byte
ceiling admits cannot reach the node ceiling.

*Lower bound — it must actually bound heap.* Scaling linearly from the measured
`giant` cell (532,424 nodes peaked 339.9 MB at `-Xmx512m`), 200,000 nodes
projects to about **128 MB** of transient peak, inside the battery's 247.8 MB
per-operation budget. 300,000 would project to 191 MB and 500,000 to 319 MB —
over it.

*Margins.* 10.2x above this repository's largest file (19,528 nodes,
`intent_transaction.clj`) and 50x above the battery's default corpus (3,980).
The adversarial `giant` file is 532,424 nodes — 2.66x over.

## Misreadings this row forbids

- *A byte ceiling makes this redundant.* It does not. The `nested` file is
  111 KB — a fifth of the 512 KiB byte ceiling, admitted by every byte control
  in the design, and it still either crashes the scan or costs 312 MB.
- *Refusal means the operation fails.* It does not. A refused file is one typed,
  named, counted skip; the scan completes and the other files' output is
  unchanged. Aborting the scan is the behaviour this row is replacing.
- *The node ceiling is where large files get stopped.* It is not. That is
  MEM-002's per-file byte ceiling, which carries the byte remedy and the
  narrowing `next_call`. Tuning `max_parse_nodes` down until it refuses large
  ordinary files converts this control into a worse copy of that one.
- *The estimate must equal rewrite-clj's node count.* It must not. It is a
  cheap single-pass lower bound; the ceilings are derived from it, and the
  margin witnesses are what keep the two in step.
- *Deep nesting is only a heap problem.* It is a stack problem first. A ceiling
  chosen from heap measurements alone would sit above the cold overflow point
  and let the crash through.

## Boundaries the witnesses must hold

- Delimiters inside strings, regex literals, character literals (`\(`), and
  line comments do not count toward depth or nodes — proved by a zero balance
  on all 163 real files, not by a hand-written fixture.
- A source with exactly `max_parse_nodes` nodes, and one with exactly
  `max_parse_depth` levels, outline byte-identically to the ungated path.
- `N+1` and `D+1` refuse with **zero** calls into the rewrite-clj parse entry.
- A refusal names `:reason`, `:limit`, `:observed`, and `:remedy`. It carries no
  `:next_call`, and that is deliberate: every clj-surgeon structural read of a
  refused file builds the same tree, so there is no narrower call to name. The
  executable narrowing for an oversized *scope* belongs to MEM-002.
- An empty source, a whitespace-only source, and a source with unbalanced
  delimiters are admitted (the parser, not the admission gate, reports a syntax
  error).

# Bounded `ls-tree` output budget (`MCP-OP-MEM-003`)

Status: implemented 2026-09-03.

## Context

The memory battery at `8a55dbc` (`MEMBAT_ROOT=/home/forge/tmp/stream/membat`,
`-Xmx512m`, 5 reps) reports `cli-ls-tree` retaining:

| N | held_mb (fresh / warm) | per file |
|---:|---|---:|
| 100 | 0.6 / 0.9 | ~9 KB |
| 1,000 | 9.6 / 9.5 | 9.5 KB |
| 10,000 | 94.0 / 93.6 | 9.4 KB |

```
FAIL held-scales-with-n {:op :cli-ls-tree, :profile :default, :observed 94.0,
                         :limit 11.6, :small-n-observed 9.6, :slack-mb 2.0}
```

`held_mb` is the after-GC used heap **while the result is still referenced**: the
retained size of what the operation hands back. It is a straight line in N
because the operation has no output ceiling — `outline-all-files` realises every
outline into one retained vector, and both formatters then traverse the complete
set. The result IS the repository.

## The control

A **result ceiling `R`, counted in RECORDS, applied to the ENCODER** — not to the
walk. The walk still visits and stats every candidate. What is bounded is what
the encoder keeps: each file is admitted (MEM-005), parsed once (MEM-015),
outlined, encoded, and **dropped**. The vector of all outlines is never built.

`max-result-records = 1000` is a server hard cap; a request may lower it with
`:max-results` and may never raise it. The derivation is in
`docs/observations/2026-09-03-mem-003-streaming-ls-tree.md` §3: retention
(9.4–9.5 KB/record makes the battery's held line hold by construction at this
cap), real corpora (163 files here, 6.1x headroom), and output size (~1.5 MB of
text at the cap).

When the ceiling binds the caller gets one of two TYPED answers:

- a **continuation** — the first `R` records plus `:next_call`, whose cursor is
  `<offset>:<manifest-digest>`; or
- a **refusal** — `:result-ceiling-exceeded`, when the caller passed
  `:complete true`, naming `R`, the observed count and what fits.

The cursor is bound to a **manifest digest**, not to an offset alone: SHA-256
over the ordered `<relative-path>\t<size>\t<mtime>` lines of the candidate list,
folded one candidate at a time and never materialised. A page taken after the
tree changed would silently interleave records from two different repositories,
so it refuses with `:stale-result-cursor`.

## Boundary

- **It does not change outline CONTENT.** Every result at or under `R` is
  byte-identical to the batch encoder's, in both the text and EDN encodings, and
  in record order. The ceiling is invisible until it binds.
- **It does not bound the WALK.** Per-file bytes, aggregate bytes, walk entries
  and depth are `MCP-OP-MEM-002`'s; the bounded walker is q5z's. This row bounds
  the ENCODER only, which is why discovery still holds one path string per
  candidate.
- **It is not a peak-heap promise.** Peak is heap-size dependent under G1 and is
  a trend line, not a gate. The promise is on retained result heap.
- **A ceiling is never a silent truncation.** Every bounded result carries either
  a continuation naming the exact resuming call or a typed refusal. A result
  with no receipt is complete, and that is the only way to read one.
- **It does not touch the MCP study-ops entrance.** `mcp_inspect_tool.clj` and
  `study.clj` belong to the `bridge/study-ops-mcp` lane; they must adopt the same
  ceiling, the same cursor shape, and the same two typed answers.

## Misreadings this row forbids

- *An outline is small, so retaining them all is fine.* 9.4 KB each is small;
  10,000 of them is 94 MB, in a 512 MB budget shared with the parser's transient
  peak.
- *A bounded read is a truncated read.* It is not. Bounded means the result
  names its own boundary and the call that continues past it. The unbounded read
  is the one that silently hands back whatever the repository happened to
  contain.
- *`R` should be raised until nobody hits it.* That re-opens the failure. `R` may
  be lowered by any request; raising the server cap requires re-measuring the
  battery.
- *A cursor is just an offset.* An offset alone silently interleaves two
  different trees. The digest is what makes a second page honest.
- *The materialiser window can be unbounded because the outlines are dropped.*
  The window is what the parallel materialiser holds in flight; it is a constant
  (`4 x pool`), and the retained-heap witness gates on `R + window`, not on `R`.

## Boundaries the witnesses must hold

- A result of exactly `R` records is COMPLETE, carries no receipt, and equals
  the unbounded result exactly.
- `R+1` candidates yield a continuation whose `:next_call` cursor parses to
  `{:offset R :manifest-digest <64 hex>}`, and whose pages concatenate to the
  unbounded result in the same order — a bounded result is the PREFIX of the
  unbounded one, never a sample.
- With `:complete true`, `R+1` candidates refuse with `:error-type
  :result-ceiling-exceeded`, `:complete false`, `:source-unchanged true`, and a
  `:limit` naming `:requested`, `:server-max`, `:observed` and `:fits`. The
  remedy narrows the scope; it never says raise the heap.
- A cursor issued before a file was added is refused as `:stale-result-cursor`
  with the two digests named.
- A malformed `:max-results` is `:invalid`, never silently promoted to the cap.
- Retained heap tracks `R`, not `N`: at a fixed `R`, scanning 8x the files must
  not retain measurably more, and an unbounded control on the same corpus must
  retain measurably more than both — otherwise the witness is measuring nothing.
- The differential over `src/` and `test/` is zero mismatches in text, in EDN,
  and in record order.
