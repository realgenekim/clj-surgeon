# Design question for Sol (and Opus): memory as the number of files grows (2026-09-03T04:03Z)

Gene, verbatim: *"I'm so afraid of huge heaps — but dont want to OOM. Can you have sol figure out clever way
to reduce memory usage as num files grow."*

## Measured facts (tonight, on Anvil)

- ls-tree before the security round: 1,072 files outlined → **618 MB heap**, 2.86 s, to return 3 files;
  ≈ 0.55 MB per file resident (rewrite-clj parse + zipper + source string), linear in files. After the
  round: discovery split from outlining, lazy outline to the byte budget, 1,100 files → 333 ms at 512 MB.
- alias_migration: the whole scope is slurped into one realised vector and RETAINED for the transaction
  (frozen read); each file parsed twice plus a raw pass; ceilings 2,000 files / 2 MiB per file — and the
  re-review witnessed OOM at 512 MB with 450 files × 1.9 MB, each under both ceilings. Aggregate bytes were
  unbounded; an accumulator is being added tonight.
- extract!/rewire: `file-seq` + slurp of every matching file into one sorted-map for the whole call.
- census: reads every scanned file before `defines-arms?` filters; requested files bypassed the size cap.
- fold-diff (curtain-call): both projections + the checkpoint parsed twice + the baseline EDN as one String;
  `-Xmx` now 4g by judgement.
- The transactional need underneath: a consistent PRE-IMAGE for atomic multi-file writes (read-back hashes,
  rollback), which is why sources are retained.

## The question

Design the memory posture for clj-surgeon's tree-scale operations so heap is bounded by the WORK the
receipt can carry, not by the size of the repository — without giving up the pre-image guarantee. Please
give a committed design, not a survey, covering:
1. Retention policy: what must stay resident (per-file content hash + spans + the projection the receipt
   needs) vs what is re-read on demand under a hash check (source text). Is "capture hashes at discovery,
   re-verify at write" an acceptable pre-image for the transaction, and what does it cost in correctness?
2. Streaming/bounded working set: per-file read → outline/plan → drop; working set = pool × chunk; where
   the transaction needs cross-file knowledge (callers, aliases), what is the smallest resident index?
3. Zipper cost: rewrite-clj zippers are the 0.55 MB/file; for read-only ops (outline, deps, topo, census
   classification) is a lighter reader (tools.reader with positions, or rewrite-clj node without zipper
   navigation) sufficient, and where must the zipper stay?
4. Ceilings as contracts: file count, per-file bytes, AGGREGATE bytes, walk entries, depth — which are
   request parameters, which are server config, and how a refusal names the remedy (`next_call` that
   narrows scope, never a wall).
5. Parallelism vs memory: claypoole pool sizing as a function of a memory budget (pool × max-file-bytes
   × parse factor ≤ budget), measured not guessed.
6. Off-heap/disk: content-addressed cache under `.clj-surgeon` for outlines keyed by file hash (already the
   state dir); when is re-parse cheaper than caching?
7. The fold-diff shape (two full projections in memory): a streaming diff over sorted paths, or a
   digest-per-relation first pass, so the tool never holds both whole projections.
8. Measurement plan: a per-op heap receipt (`heap_used_mb` at peak, files, bytes) so every future claim is
   a number, and a memory-vs-N battery on Anvil (100 / 1,000 / 10,000 files) with a pass line.
Name the trade-offs and what you would NOT do. Executed reasoning welcome (the repo is read-only to you).
