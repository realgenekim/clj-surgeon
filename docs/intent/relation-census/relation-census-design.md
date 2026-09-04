# Relation Census

Status: draft LLD.

## Context

An event-sourced Clojure projection is a multimethod whose arms fold one fact
into state. Almost every durable defect in such a projection is the same shape:
a collection write inside an arm that appends without asking whether the thing
is already recorded. Replay, an at-least-once delivery, or a retried command
then duplicates a row that the product treats as an identity.

The repositories that carry these projections already encode the correct
answer as a small set of **identity doors** — `conj-once`, `cons-once`,
`upsert-by` and their relatives — and as inline guards that test membership
before appending. The vulnerability is the write that goes through neither.

Grep cannot find it. `rg 'conj'` returns every append in the repository, and
the reader must open each one to reject it. Worse, grep-shaped structural
matching produces confident false positives: a real review of
`curtaincall-cfp-lens` flagged `(update :chases (fnil conj []) chase)` as
unguarded because the match saw the write and not the `not-any?` on
`:chase-id` three lines above it, inside the `if` that dominates it.

`relation_census` is the finder. It answers the question grep answers wrong.

## What the census is, and what it is not

The census **locates review work**. It does not prove idempotency, it does not
gate a release, and it never claims a write is correct.

- `:raw` means *no recognised guard dominates this write*. It is a positive
  statement about the analyzer's recognised vocabulary, reached only after the
  enclosing chain has been walked and understood.
- `:unknown` means *this version declines to decide*, and carries the reason.
  `:raw` is never used for "the analyzer found nothing"; that is `:unknown`.
- `:door` and `:set` are statements about the write's route and target, not
  about the correctness of the surrounding logic.

Every site carries the evidence a human needs to overrule the census: the
write's location and source line, the resolved target path expression, the
candidate identity expression, the guard's location and source, the polarity,
and the uncertainty reason.

## Boundary

**Scope.** A site is a collection write **inside the body of a
`defmethod fold-event` arm**:

- a call whose head is `conj`, `cons`, `into`, or `concat`;
- `(fnil conj …)`, `(fnil cons …)`, `(fnil into …)` in the update-fn position
  of `update`, `update-in`, or `swap!`;
- a bare `conj`/`cons`/`into` symbol in that same update-fn position;
- a call whose head is a known identity door.

Writes in helpers outside every arm are **counted** as `:outside-arms` and are
never classified. The count is reported so a reader can see how much of the
projection's write surface the census did not judge.

**Classification order.** `:door`, then `:set`, then guard analysis.

**`:guarded` requires all four:**

1. **Dominance.** A recognised guard form encloses the write and the write sits
   in one of that form's branches — not merely later in the same body.
2. **Target.** The guard's membership idiom reads the same collection: the same
   resolved root plus path, or a single-assignment `let` alias of it.
3. **Identity.** The guard performs a keyword lookup whose keyword also occurs
   in the written value's expression, resolved through single-assignment `let`
   aliases.
4. **Polarity.** An append requires an *absent* check. `not-any?`, `not (some …)`,
   `not (contains? …)`, `(nil? (get …))`, `(empty? (filter …))`, and `if-not`/
   `when-not` over a presence test all satisfy it. A presence test with the
   wrong sense is `:unknown` with reason `:polarity`, never `:guarded` and never
   `:raw`.

**Recognised vocabulary.** Guard forms: `if`, `if-not`, `when`, `when-not`,
`cond`, `if-let`, `when-let`, `if-some`, `when-some`. Membership idioms:
`some`, `not-any?`, `every?`, set invocation, `contains?`, `get`/`get-in`
indexed lookup, `filter` composed with `empty?` or `seq`. Alias tracing: local
single-assignment `let`/`loop` bindings and `fn` parameters bound by an
enclosing update form. Nothing else.

**No inference through helpers or macros.** A dominating test that routes the
written value's identity or the target collection through an unrecognised call
is `:unknown` with reason `:helper-mediated-guard` and the helper named. An
unrecognised enclosing form between the arm body and the write is `:unknown`
with reason `:unsupported-container` and the form named. A target expression
the resolver cannot reduce is `:unknown` with reason `:unresolved-target`.

## Phases

A census publishes `phases_elapsed_ms`, inside its `measured` partition, for
exactly the phases that ran, and for
no others:

- `discover` — only when the census walked a tree. A caller who names files
  discovers nothing, and no `discover` timing is published.
- `read` — resolving the scan through the path fence and reading it. Each
  candidate file is read once; a file already read under another name is
  collapsed rather than read again.
- `classify` — parsing and classifying each retained source. There is no
  separate parse phase: parsing happens inside `classify`, on the same single
  pass over each file, so no receipt ever carries a parse timing.
- `merge` — re-keying and ordering the per-file results.

The `classify` phase runs in parallel on a `com.climate.claypoole` thread pool
sized to the available processors, inside `cp/with-shutdown!`, using the eager
unordered `cp/upmap`. The merge re-keys results by path and sorts by path, so
the pool cannot reorder the answer.

**Parallelism changes elapsed time and never the answer.** The complete ordered
output and the published receipt, minus `elapsed_ms`, are byte-identical at
pool size 1 and pool size N. A witness asserts exactly that.

A worker exception is surfaced as a typed refusal naming the file, not as a
partial census.

## Receipt

The receipt leads with state, is bounded at 4 KB, and carries no file text
beyond one-line site sources. It reports `census_version`, per-file counts by
class, every `:raw` site with its evidence, every `:guarded` site with its guard
line, every `:unknown` site with its reason, the `:outside-arms` count,
`phases_elapsed_ms` (inside `measured`) for each phase that ran, the
pool size, and a `next_action`. Listed evidence is trimmed until the receipt
fits its budget, and the trim is reported.

Every refusal carries an executable `next_call` WHEN ONE CAN BE COMPUTED — a
call the caller may replay verbatim that is not the request just refused: no
fold arms found where the scan named files to look at, an unparseable file
(naming it), an unknown door symbol (naming it and the known doors), a bounds
refusal that finished a subtree worth offering, and a request naming a source
too large to read (the same request MINUS the oversized sources, with
`files_removed`, because the caller named them and so they can be removed).

Where no continuation can be computed the refusal carries NO `next_call` key at
all, and says why in a `remedy` instead. That is the case for a bounds refusal
whose walk finished no subtree that both fits and holds a candidate source; a
`census-resource-exhausted` refusal, which has less than that, because the
walk's own per-directory aggregates were lost with the resource that held them;
a workspace root that does not resolve, leaving no root to narrow; a request
whose every named source is oversized, leaving no request; and an arm-less tree
that scanned nothing, where the only call left to offer is the one just
refused. All of them publish a `remedy` rather than a caption in an argument
position — a placeholder such as `files ["<a narrower file list>"]` is not a
smaller promise than a real continuation, it is an unexecutable one, and
MCP-OP-CENSUS-014 forbids it in every argument position of every refusal on
every entrance.

RULING (Sol's round-seven review, item 5, non-blocking): a `no-fold-arms-found`
continuation from a DISCOVERED (unnamed) file list names at most
`max-listed-files` of the sources the walk found, never the full
`max-scanned-files` the walk may have scanned. A tree of, say, 20 arm-less
files plus one oversized file therefore pins exactly the listed handful, not
every scanned path. This is spec-consistent — `max-listed-files` is a stated
receipt-listing bound, not a promise that the continuation is exhaustive — and
replay still deterministically reaches the same explicit no-arms refusal
naming those files, so the continuation remains executable. It is
operationally weak in the sense that a caller cannot recover the FULL scanned
set from the refusal alone, but that is a receipt-size tradeoff already
governed by MCP-OP-CENSUS-013's 4 KB budget, not a new defect. Left as is.

## Versioning

The receipt carries `census_version`. A change to the recognised vocabulary,
the four `:guarded` conditions, or the reason taxonomy increments it, because a
consumer comparing two censuses must be able to tell an analyzer change from a
code change.

## Surfaces

The capability is exposed as the `relation_census` MCP tool and as the
`:relation-census` CLI op, and both run the same pure kernel over the same
bounds.

The pool is resolved at RUN time, not at load time, so the CLI keeps loading
under babashka without the JVM dependency and still uses the pool when it has
one. On the JVM both entrances run the `classify` phase on `census_pool`'s
shutdown-bound claypoole pool. Under babashka, where that namespace cannot
load, the CLI runs it serially at pool size 1 and reports the size that was
asked for alongside the pool that actually ran — `pool_size_requested` on the
tool, `:pool-size-requested` on the CLI — so a receipt never claims a pool it
did not use.

## Enumeration posture

`workspace_root` is unrestricted for every clj-surgeon tool: the caller names
the workspace and the path fence confines every read to it. The census inherits
that posture, but it is the first tool that ENUMERATES a tree rather than
reading paths the caller named, so it is worth stating plainly what the walk
does and does not do:

- there is exactly ONE walk. `clj-surgeon.census-discovery` is the kernel both
  entrances call; the tool and the CLI op each hand it a root and read its
  result, and neither carries a discovery rule of its own. It is written in
  `java.io.File` listing plus `java.nio.file.Files` predicates rather than a
  `SimpleFileVisitor`, because babashka has no `SimpleFileVisitor` and that
  absence is what grew a second walk — with different answers — the first time;
- the root is CANONICALISED before the walk begins, so a `workspace_root` or a
  `:dir` naming a symlink walks the workspace it points at rather than
  enumerating a link and finding nothing;
- a symbolic link is never followed out of the canonical root: a discovered
  path whose real location escapes is skipped and counted in
  `skipped_outside_root`, never read, and never fatal;
- the discovered path SET is a set of REAL paths, so a chain of links onto one
  source is one source; what collapsed is published as `duplicates_collapsed`,
  which also carries the repeats in a caller's explicit `files` list;
- `.git`, `node_modules`, `target` and their relatives are pruned before they
  are read, not filtered out afterwards;
- the walk stops at `max-scanned-files`, and reaching that ceiling is a
  REFUSAL (`too-many-candidate-files`, naming the ceiling, the count that
  fits, and the observed lower bound) rather than a truncated scan published
  as a complete census. Its continuation is COMPUTED, not described: the walk
  aggregates candidates per directory as it goes, and the refusal offers the
  largest subtree the walk FINISHED whose count fits and which holds at least
  one candidate source — ties by most candidates, then deepest, then
  lexicographic — as a `next_call` (a `:next-command` at the CLI) the
  caller can replay verbatim, bounded by `max-next-call-bytes`. Every ancestor
  of the file the walk stopped on is excluded, because those counts are lower
  bounds. When nothing is known to fit there is no continuation at all, and a
  `remedy` says why;
- the walk also stops at `max-walk-entries` — 50,000 — counting EVERY entry it
  visits, of any name, and charging the bound BEFORE it stats the entry. Each
  directory is STREAMED, one name at a time, and the bound is charged as the
  filesystem yields each name: a complete listing is never materialised or
  ordered before the counter runs, because a walk that built and sorted the
  name array of a 60,000-entry directory has already spent what the bound
  exists to protect. Only the ADMITTED names are ordered, so a walk under the
  bound visits the tree in the same order it always did, and the refusal
  publishes `entries_yielded` — the names it actually obtained from the
  filesystem, never more than the bound. The
  scanned-file ceiling bounds what the census will READ; it does not bound what
  the walk COSTS, and a tree of 60,000 images or fixtures holds no candidate at
  all, so the ceiling never fires while the walk enumerates the lot. Reaching
  this bound is the refusal `too-many-walk-entries`, with the same shape as the
  ceiling refusal — the bound, the entry count visited and that it is a lower
  bound, `files_read` 0 — and a continuation computed from the walk's own
  per-directory ENTRY aggregates: the largest fully-walked subtree that fits
  under BOTH bounds AND holds at least one candidate source, so the narrowing
  can neither replay into the other refusal nor hand back a subtree of the very
  junk that tripped the bound — the entry bound fires on trees of junk, so the
  biggest finished subtree is usually junk, and a census of junk is
  `no-fold-arms-found` on a workspace that has arms;
- a discovered source above `max-source-bytes` is never read, and never
  dropped in silence: it is counted in `oversized_skipped`, at most
  `max-listed-files` of them are named, and `oversized_skipped_omitted` states
  how many were counted but not named — zero when the list is complete. That
  census publishes `read_complete` false;
- every one of those discovery figures — `files_scanned`,
  `skipped_outside_root`, `duplicates_collapsed`, `oversized_skipped` and its
  omission count — is built by ONE kernel fn and published on EVERY receipt
  shape either entrance can return: the success, `no-fold-arms-found`, and
  every refusal reached after a walk. A receipt that carries its evidence when
  the census succeeds and drops it when the census refuses hides the walk
  exactly where the caller needs to audit it: `no-fold-arms-found` on a tree
  whose only sources were skipped for size would otherwise read the same as
  `no-fold-arms-found` on an empty directory;
- only the sources that define arms are retained, and the receipt carries
  one-line excerpts, never file text.

A caller who points the census at a workspace is asking for that workspace to be
enumerated. Nothing outside it is read.
