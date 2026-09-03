---
parent: mcp-operation-contract-design
prefix: MCP-OP-MEM
status: "measurement half implemented; the read path is bounded (MCP-OP-MEM-003/005/015, read-path-memory leaf); the streaming kernel for the write/whole-workspace paths (006, 007, 012 … 014) is still an open gap"
---

# Memory Boundedness

## Context

Every tree-scale operation on this branch sizes its heap by the repository,
not by the work its receipt carries.

- `:ls-tree` (`src/clj_surgeon/core.clj:202-250`, `321-339`, `463-481`)
  discovers every project, `pmap`s an outline over every file, and holds all
  outlines in one vector before formatting.
- `mcp-workspace-sources/read-all` (`src/clj_surgeon/mcp_workspace_sources.clj:11-20`)
  slurps every `.clj`/`.cljc`/`.cljs` file in the workspace into one
  `sorted-map`. `extract/plan` (`src/clj_surgeon/extract.clj:447-479`) repeats
  the same shape inline.
- `rename/plan` (`src/clj_surgeon/rename.clj:110-160`) walks `src` and `test`,
  parses every file, and additionally `file-seq`s the whole root to slurp every
  non-Clojure file looking for textual references.

Sol's measured design (`docs/observations/2026-09-03-memory-design-sol-answer.md`
and its second answer) sets the target posture: stream discovery, project into
bounded compact records, spill to operation-local scratch, and let disk and I/O
grow with N while retained heap does not.

This leaf owns the **measurement** half of that program. It does not change any
operation. Two things must exist before a kernel change can be believed:

1. Every operation must be able to say, in its own receipt, what memory and
   work it actually consumed (MCP-OP-MEM-001).
2. A battery must run every tree-scale operation at the configured maximum N,
   at the configured work budget, in one bounded JVM, and refuse to call it
   green on a line it did not actually measure (MCP-OP-MEM-011).

## Ownership of the MCP-OP-MEM prefix

| ids | owner | status |
|---|---|---|
| MCP-OP-MEM-001 | this leaf — the per-operation memory/work receipt block | implemented |
| MCP-OP-MEM-002 | reserved | — |
| MCP-OP-MEM-003 | the `read-path-memory` leaf — the bounded `ls-tree` output budget | implemented |
| MCP-OP-MEM-004 | reserved | — |
| MCP-OP-MEM-005 | the `read-path-memory` leaf — bounded lexical/parser admission | implemented |
| MCP-OP-MEM-008 … 010 | reserved | — |
| MCP-OP-MEM-011 | this leaf — the battery as a release gate | implemented |
| MCP-OP-MEM-015 | the `read-path-memory` leaf — the single-parse outline read path | implemented |
| MCP-OP-MEM-006, 007, 012 … 014 | the streaming-kernel builder | open |

Ids are forever stable. They are retired or superseded, never renumbered or
repurposed.

**This table is merge-facing, and it lied.** Until 2026-09-03 it read
`MCP-OP-MEM-002 … 010 | reserved` while 003, 005 and 015 were owned,
specified and implemented in the `read-path-memory` leaf — so a reviewer who
consulted the central table to ask "who owns 003?" was told nobody did (Sol,
finding 10). A registry that disagrees with the leaves is worse than no
registry: it is the artifact people trust INSTEAD of looking. Ids implemented
elsewhere are listed here by owner, not folded into a `reserved` range; the
authority for each id's requirement text remains its owning leaf's specs file
(`docs/intent/read-path-memory/read-path-memory-specs.md`).

## What the battery is

`make memory-battery`:

1. generates deterministic synthetic Clojure trees at N = 100 / 1,000 / 10,000
   files (`bench/memory_battery/generate_tree.clj`; ~1 s for all three,
   40.5 MB at 10,000 files, mean 4,047 B/file — deliberately smaller than this
   repository's own 17 KB mean so the battery cannot be accused of being sized
   to fail);
2. runs each operation once at a large heap to record the **unbounded reference
   output hash** for every (op, N), cached under `$MEMBAT_ROOT`;
3. launches **one** JVM at `-Xmx$(MEMBAT_XMX)` (default 512m), warms the class
   loader, then runs every operation against every tree fresh and warm
   (`$MEMBAT_REPS`, default 5) with a continuous heap sampler on its own daemon
   thread;
4. writes a receipt EDN and a one-screen table, and exits 0 pass / 1 fail /
   2 refusal (bad environment) / 3 tool failure.

## Why it is not in `make test`

It is minutes-scale and it needs a dedicated bounded JVM whose heap must not be
shared with a test runner. `make test` runs `memory-battery-self-test` instead:
the generator's determinism self-test plus the millisecond witness that the
verdict applies the published pass lines exactly and that `memory-battery` is
absent from every fast gate's target closure.

## The measurement honesty rules

- `heap_used_peak_mb` is a **continuously sampled process-wide** used-heap peak
  at 5 ms. It is not the five-`System/gc` used-heap delta used elsewhere to
  derive per-byte coefficients, and it is not attributable to one operation.
  The receipt says so in `:measurement-note`.
- `heap_reserved_peak_mb` is the admission accountant's attributable figure. No
  operation on this branch has an admission accountant, so this line is
  reported **UNMEASURED**, never passed on the sampled number.
- A cross-N line with no cells at one of its two N values is **UNMEASURED**,
  never passed.
- Output parity with no cached unbounded reference is **UNMEASURED**, never
  passed.
- `:complete?` is false whenever any line is unmeasured, and the table prints
  `(INCOMPLETE)`.

## Not covered by this leaf

Two of Sol's pass lines are boundaries on MCP-OP-MEM-011 rather than constants
in `pass-lines`, because this battery does not yet run their arms:

- the 450 x 1.9 MiB aggregate-admission case (refuse with < 32 MiB heap growth,
  or complete under a larger aggregate-I/O allowance without raising the heap
  line);
- injected conflict at staging, validation, every commit boundary, and rollback
  (zero writes, or verified restoration to all H0 hashes).

They belong with the transactional kernel, which does not exist on this branch.
