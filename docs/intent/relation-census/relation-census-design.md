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

## Plan phase

Discovery reads each candidate file once. The plan phase parses and classifies
files in parallel on a `com.climate.claypoole` thread pool sized to the
available processors, inside `cp/with-shutdown!`, using the eager unordered
`cp/upmap`. The merge re-keys results by path and sorts by path, so the pool
cannot reorder the answer.

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
`phases_elapsed_ms` for `:discover`, `:parse`, `:classify` and `:merge`, the
pool size, and a `next_action`. Listed evidence is trimmed until the receipt
fits its budget, and the trim is reported.

Every refusal carries an executable `next_call`: workspace not found, no fold
arms found (naming the files scanned), an unparseable file (naming it), and an
unknown door symbol (naming it and the known doors).

## Versioning

The receipt carries `census_version`. A change to the recognised vocabulary,
the four `:guarded` conditions, or the reason taxonomy increments it, because a
consumer comparing two censuses must be able to tell an analyzer change from a
code change.

## Surfaces

The capability is exposed as the `relation_census` MCP tool and as the
`:relation-census` CLI op. The CLI is the same pure kernel with a `map`-shaped
plan phase; only the MCP route carries the claypoole pool, so the babashka CLI
keeps loading without the JVM dependency.

## Enumeration posture

`workspace_root` is unrestricted for every clj-surgeon tool: the caller names
the workspace and the path fence confines every read to it. The census inherits
that posture, but it is the first tool that ENUMERATES a tree rather than
reading paths the caller named, so it is worth stating plainly what the walk
does and does not do:

- it walks with `Files/walkFileTree` and no `FOLLOW_LINKS`, so a symlinked
  directory is never descended;
- a discovered path whose real location escapes the root is skipped and counted
  in `skipped_outside_root`, never read;
- `.git`, `node_modules`, `target` and their relatives are pruned before they
  are read, not filtered out afterwards;
- the walk stops at `max-scanned-files`, and reaching that ceiling is a
  REFUSAL (`too-many-candidate-files`, naming the ceiling, the count that
  fits, the observed lower bound, and a narrowing `next_call`) rather than
  a truncated scan published as a complete census; a source above
  `max-source-bytes` is never read;
- only the sources that define arms are retained, and the receipt carries
  one-line excerpts, never file text.

A caller who points the census at a workspace is asking for that workspace to be
enumerated. Nothing outside it is read.
