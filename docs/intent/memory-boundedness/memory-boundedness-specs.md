---
parent: memory-boundedness-design
prefix: MCP-OP-MEM
status: "registered 2026-09-03; both ids are active gaps — the RED baseline is docs/observations/2026-09-03-memory-battery-baseline.md"
---

# Memory Boundedness Specifications

These IDs are stable and must not be reused if a requirement is deleted. Ids
002-010 and 012-014 are reserved; 006, 007 and 012-014 belong to the
streaming-kernel builder and are not registered here.

Both requirements below are **active gaps**: no operation on this branch
satisfies either one. Their witnesses are the battery and its millisecond
verdict test, so removing the battery breaks the contract audit rather than
turning the promise green by deletion.

In the requirements below:

- **tree-scale operation** means an operation whose file set is determined by
  walking a repository rather than by an explicit caller-supplied file list. On
  this branch that is `:ls-tree`, `mcp-workspace-sources/read-all` (and the
  identical inline walk in `extract/plan`), and `rename/plan`.
- **work budget** means the configured heap work budget for one operation,
  separate from the process `-Xmx`.
- **unbounded reference output** means the operation's result, hashed, from a
  run at a heap large enough that no admission limit or spill path can engage.
- **UNMEASURED** is a distinct third outcome from pass and fail: a line whose
  inputs were not observed. It is never reported as a pass.

## Requirements

- [ ] **MCP-OP-MEM-001**: While a tree-scale operation runs over a repository of
  N files, when it returns its result, clj-surgeon shall carry in that result a
  memory-and-work block whose own size does not grow with N, reporting the
  configured maximum heap, the used-heap start, continuously sampled
  process-wide peak, and end, the attributable reserved peak, and the work
  actually done — walk entries, files discovered, read, parsed and written,
  source bytes, largest file bytes, projection records, workers, spill bytes,
  journal bytes, and cache work — labelling the sampled peak as process-wide
  rather than attributable whenever another operation ran concurrently.

- [ ] **MCP-OP-MEM-011**: While the memory battery runs every tree-scale
  operation at the configured maximum N under the configured work budget in one
  bounded JVM, clj-surgeon shall for every operation reproduce the unbounded
  reference output exactly, complete without exhausting the heap, keep the
  attributable reserved peak at or below 192 MiB, keep the largest 10,000-file
  heap retained WHILE THE
  RESULT IS STILL REFERENCED within 2.0 MiB of the largest such value at 1,000
  files, keep the growth the call leaves behind after the result is
  released (after-release used heap minus start) at 10,000 files within 8 MiB of
  the same growth at 1,000 files, and refuse before any mutation when the request
  is over budget; shall additionally measure and report, WITHOUT GATING, the
  sampled process-wide peak against the tighter of the used-heap start plus
  224 MiB and 80 percent of the configured maximum heap, and the 10,000-file
  peak against the 1,000-file peak plus 32 MiB; and the battery shall report any
  of the gating lines it did not observe as UNMEASURED rather than as a pass, terminating in exactly one of
  PASS, FAIL or INCOMPLETE, where a run with no failures and at least one
  UNMEASURED line terminates INCOMPLETE with a nonzero exit distinct from both
  PASS and FAIL.

## Misreadings

MCP-OP-MEM-001:

- "Report the peak and call it this operation's peak." A sampled process-wide
  peak is not attributable to one operation. Under concurrency it must be
  labelled process-wide, and the attributable figure must come from the
  admission accountant.
- "Include the manifest of every observed file in the block so the receipt is
  complete." The block's own size is part of the promise; a per-file manifest
  makes the receipt grow with N.
- "Emit the block only when the operation succeeds." A refusal is exactly when
  the numbers are wanted.
- "Reserved peak equals the sampled peak when nothing else is running." They are
  different quantities measured by different instruments; equating them makes
  the 192 MiB line unfalsifiable.

MCP-OP-MEM-011:

- "The manifest says 10,000 files, so 10,000 files were measured." A manifest is
  a claim about a corpus, not the corpus. The bytes on disk are checked against
  the generator's deterministic promise before any cell is measured.
- "The 10,000-file case timed out, so compare 100 and 1,000 instead." The
  cross-N lines are defined at 1,000 and 10,000. A missing 10,000 is UNMEASURED.
- "No operation reports a reserved peak yet, so that line passes." An
  unobserved line is not a satisfied line.
- "Mark the run incomplete but exit 0, since nothing measured failed." Exit
  status is what a release gate reads. An INCOMPLETE run that exits 0 is a pass
  in every way that matters, whatever the table prints above it.
- "INCOMPLETE is just a FAIL, so give it exit 1." Then no caller can tell an
  operation that broke a line from a line nobody measured, and the two have
  opposite remedies.
- "The bounded run produced a result, so output parity holds." Parity is against
  a cached unbounded reference hash; with no reference, parity is UNMEASURED.
- "A reference-hashes file is present, so parity can be checked." Existence is
  not attestation. The default corpus root is shared between worktrees, so a
  present file may hold hashes from other code over another corpus; a reference
  not bound to this run's operation sources, generator, corpus digests and JVM
  is refused, never compared.
- "The attestation fields all match, so the reference is trustworthy." Attested
  fields say WHAT was measured; they say nothing about whether the `:hashes`
  on disk are the bytes that measurement actually produced. A hand-written
  reference carrying today's correct attestation and a forged or extra
  `:hashes` key passed attestation cleanly. A reference is also anchored
  (`:reference-unanchored` when its sha256 sidecar is missing or does not
  match its own canonical bytes) and its `:hashes` are checked against the ops
  catalogue exactly (`:reference-ops-mismatch`) — see the honest-boundary note
  in `docs/memory-battery.md`: this is a stale/hand-edit check, not a
  signature; a party with write access to both the reference and its sidecar
  can still forge both together.
- "Raise `-Xmx` until the battery is green." The budget is the requirement.
- "The sampled peak crossed its budget, so the operation is unbounded." The
  sampled peak is process-wide, contains garbage, and G1 moves it with heap and
  collector settings: an identical cell moved 28.3 MiB across the line between
  two runs of unchanged work. It is a regression signal under identical
  settings, not a proof about live boundedness.
- "The peak lines are only advisory, so drop them." They are the cheapest early
  warning the battery has. Reported every run, compared run to run; just never
  the thing that decides the verdict.
- "Retention after the result is released is flat, so retained heap is bounded."
  That measures leaks. What a receipt itself retains is measured with the result
  still referenced, and has its own cross-N line.
- "The peak line already bounds what the result retains." It does not. A result
  that grew from 1.0 to 9.8 MiB between 1,000 and 10,000 files sat far below
  every peak line and was reported `ok`; held heap needs its own gate.
- "Run the battery in `make test` so nobody forgets it." It is minutes-scale and
  needs a dedicated bounded JVM; wiring it into a fast gate gets it disabled.

## Boundaries

MCP-OP-MEM-001:

- Temporary disk, spill files, journal bytes, and I/O time MAY grow with N; only
  the block and the retained heap it accounts for may not.
- The guarantee is per operation at the configured budget. It is not a claim
  about the sum of concurrent operations.
- A single pathological file may exceed the per-file budget; that is a refusal
  with a named code, not a silently larger block.
- On a JVM whose management interface is unavailable the block reports the
  fields it can obtain and names the rest as unavailable; it does not invent
  them.

MCP-OP-MEM-011:

- The battery measures this branch's operations as black boxes through their
  public entrances. It never reaches inside an operation, and it changes none.
- Output parity is claimed only against an unbounded reference attested to this
  run's arms, operation sources, generator, corpus digests and JVM. The commit
  sha is recorded for forensics but not compared: the source digest already
  covers every change that could alter an operation's output, and binding to
  HEAD would force a minutes-long reference rebuild on every unrelated commit.
  The reference is also anchored to its own bytes via a sha256 sidecar written
  only by `memory-battery-reference`, and its `:hashes` are checked against
  the ops catalogue exactly — a hand-edited or partially-forged reference
  refuses (`:reference-unanchored`, `:reference-ops-mismatch`) even when every
  attested field matches. This anchor is a stale/hand-edit check, not a
  signature: a party able to write both the reference and its sidecar can
  still forge both together.
- Two of Sol's pass lines are NOT implemented by this battery and are out of
  scope for its current arms: the 450 x 1.9 MiB aggregate-admission case, and
  injected conflict at staging, validation, every commit boundary, and rollback.
  They belong with the transactional kernel.
- A run that both fails a measured line and leaves another unobserved is a FAIL:
  the failure outranks the unmeasured line for the exit code, and the verdict row
  says `FAIL (INCOMPLETE)` so the unobserved line is not lost.
- The two peak lines are trend signals valid only BETWEEN RUNS UNDER IDENTICAL
  JVM, COLLECTOR AND HEAP SETTINGS. They are reported on every run and never
  decide the verdict.
- Wall time and spill bytes MAY grow with N; the battery records them and does
  not gate on them, except that an operation exceeding `MEMBAT_OP_TIMEOUT_MS`
  causes its larger N cells to be recorded as skipped, which makes the cross-N
  lines UNMEASURED.
- Beside the representative small/medium trees, the battery measures three
  adversarial corpora as SEPARATE arms, each at one size: 100 `.cljc` files whose
  forms sit behind reader conditionals, one ~1.9 MiB source file, and one file
  combining ~300-deep nesting with a 20,000-token literal. Cross-N lines compare
  the default corpus only — an adversarial arm exists at one size, so comparing
  it against the default trees would be a statement about two different corpora
  rather than about scaling; every per-cell line still applies to it.
- Two shapes remain out of scope and are not claimed: a 17 KiB-mean real-file
  profile (roughly four times the 10,000-file battery's weight), and 450 x
  1.9 MiB (~855 MiB of source), which only becomes cheap once aggregate
  admission exists and parsing never starts.
- The corpus is verified, not asserted: every promised file's existence, byte
  count and content digest are checked against the deterministic generator, and
  unlisted files under the tree are a refusal, before any cell is measured.
  "Existence" is checked with `NOFOLLOW_LINKS`: a symlink standing at an
  expected path is a refusal (`:symlink-at-expected-path`), even when its
  target holds byte-identical content, because `.isFile`/`.length`/a plain
  read all follow the link and would otherwise verify a substituted file as
  clean. A directory standing at an expected path is the same kind of refusal
  (`:directory-at-expected-path`), typed and raised before any write, not an
  untyped I/O exception raised mid-regeneration. A cell's reported file and
  byte counts are therefore the corpus, not a manifest's
  claim about it.
- An arm measures one operation under one query shape. An operation whose result
  is small under the battery's query is not thereby bounded: `rename/plan` under
  a narrow prefix touches every file but retains almost nothing, and is measured
  by a second full-match arm as well. A new arm is incomplete until the query
  that makes its result grow with N is also measured.
- Retention while the result is referenced includes any cache or leak the call
  created, so result-exclusive retention (held minus after-release) and
  persistent growth (after-release minus start) are recorded as separate figures
  and only the second is gated as the leak line. A fixed leak, a leak below the
  slack, or one established before the 1,000-file cell is not caught by a cross-N
  comparison, and warm-run accumulation can be absorbed into the next run's
  start.
- A shared build host perturbs wall time and, through GC scheduling, the sampled
  peak. The receipt records host load context; a single failing run near a line
  is re-run before it is called a regression.
- `MEMBAT_ROOT` is guarded, not merely a path variable: a fresh root is
  created and marked (`.membat-root`) by the battery itself; a root that
  already exists WITHOUT that marker is refused (`:membat-root-unmarked`)
  rather than written into, because the battery cannot tell whether an
  existing directory it did not mark is its own corpus or something else
  entirely; and the root's canonical path must resolve inside
  `/home/forge/tmp` unless the caller explicitly sets
  `MEMBAT_ALLOW_ANY_ROOT=1` (`:membat-root-outside-allowed` otherwise). None of
  this claims MEMBAT_ROOT's *contents* are trustworthy — that is the corpus
  and reference checks above — only that the battery does not create or write
  into a directory it has no evidence it owns.
- A stale or missing cached reference does NOT silently launch the 4 GiB
  reference JVM as a side effect of `make memory-battery`. `MEMBAT_REFERENCE`
  (default `require`) refuses that case with a typed line
  (`:membat-reference-required`) naming the explicit remedy
  (`make memory-battery-reference`); `MEMBAT_REFERENCE=auto` restores the
  side-effecting rebuild for a caller who wants it. This is an operational
  guard on WHEN the reference build runs, not a claim about the reference
  itself — see the anchoring and ops-catalogue boundaries above for that.

## Rationale

Registered 2026-09-03 from Gene's instruction — "Make sure to write a test (not
a unit test) that confirms we don't OOM; don't want to slow the make run tests
too much, tho!" and "When fixing, add LID (see skill) to add new requirements
in." — against Sol's measured memory design of the same date. The RED baseline
that motivates both ids is
`docs/observations/2026-09-03-memory-battery-baseline.md`.
