# Structural Lens Query Hill Climb

**Status:** Implemented through CLI/documentation summit; clean-context hill climb in progress

**Rollback tag:** `lens-hillclimb-v0-baseline`

## Outcome

Give coding agents a small jq-like algebra over Clojure's concrete syntax tree
so one expression can select an exact nested object and the same selector can
produce a guarded replacement plan. Existing `:cat`, `:grep-form`, and
`:replace-subform` commands remain the humane standard library; the query
algebra is their compositional substrate and the escape hatch for shapes that
do not have a standalone subtree wrapper.

The target is behavioral:

```text
one structural read when judgment requires it → one reviewed plan → one verified apply
```

jq is the reference model because its filters compose both traversal and
transformation. `path`, `getpath`, `setpath`, `delpaths`, assignment, and `|=`
make a structural path into a getter and an updater. clj-surgeon's equivalent
must preserve concrete Clojure syntax and insert a review boundary before any
write, but it should keep that same compositional property.

A clean agent must choose that route without `rg`, `sed`, line-number recovery,
help detours, source rereads, plan editing, or chained plan/application.

When the requested target relationship and replacement are already exact, the
query ending in `[:replace FORM]` may be the first non-mutating call. Its output
is both the selection evidence and reviewed plan. Requiring a separate getter
in that case would add ceremony, not a judgment boundary.

## Why this experiment exists

`case`, `cond`, map entries, binding pairs, and some anonymous-function bodies
are represented as adjacent syntax. Existing subtree matching can select a
contained value, but it cannot express "find this key or guard, then select its
semantic right sibling." Agents compensate with textual context or a second
structural read.

The missing primitive is general zipper navigation, not a catalog of
macro-specific edit operations.

## Candidate APIs

| Candidate | Decision | Reason |
|---|---|---|
| jq-shaped string such as `.forms[] | select(...)` | Reject for v1 | Requires a new parser and disguises Clojure/CST semantics behind familiar punctuation. |
| Macro-specific selectors such as `{:case :finish}` | Reject | Encodes special cases and violates the bookkeeping-versus-judgment boundary. |
| EDN pipeline of structural steps | Try | Data-shaped, safely parsed, composable, mechanically interpretable, and natural in Clojure agent contexts. |

The first candidate to test is:

```bash
clj-surgeon :op :q :file src/state.clj \
  :query '[[:form transition] [:find :finish] :right]'
```

The canonical operation is `:lens`; `:q` is its short shell alias. As in jq,
the same pipeline is both a read path and an update path. A pipeline containing
only navigation is read-only. A terminal `[:replace FORM]` step emits a plan but
never writes source:

```bash
clj-surgeon :op :q :file src/state.clj \
  :query '[[:form transition]
            [:find :finish]
            :right
            [:replace (assoc state :status :complete)]]' \
  :plan-out plan.edn

clj-surgeon :op :replace-subform! :plan plan.edn
```

## V1 query algebra

A query is a nonempty EDN vector with at most 32 steps. It starts with every
top-level semantic form and pipes a stream of located zipper nodes through each
step.

| Step | Stream behavior |
|---|---|
| `[:form NAME]` | Keep top-level defining forms with the exact unqualified name. Valid only as the first step. |
| `[:find PATTERN]` | For every current node, emit every descendant-or-self whose s-expression structurally matches `PATTERN`; `_` matches one subtree. |
| `[:where {:tag TAG}]` | Keep nodes with the rewrite-clj tag. |
| `[:where {:parent-tag TAG}]` | Keep nodes whose semantic parent has the rewrite-clj tag. |
| `:right`, `:left`, `:up`, `:down` | Move each node once through rewrite-clj's semantic zipper, which skips whitespace and comments. Missing navigation emits no node. |
| `[:replace FORM]` | Terminal update step. Require one selected node and emit a hash-bound single-edit plan; never write source. |

Every step deduplicates by complete-file preorder address. Query execution
returns a trace with the input and output count for each step. Reads report the
total match count and at most 100 result records. Every record contains exact
source, complete-file preorder address, line range, semantic path, rewrite-clj
tag, and enclosing top-level name when mechanically available.

## Target programs

V1 must express these without macro-specific code:

| Shape | Query target |
|---|---|
| `case` clause | `[[:form transition] [:find :finish] :right]` |
| `cond` branch | `[[:form classify] [:find (eligible? user)] :right]` |
| map entry | `[[:form settings] [:find :timeout-ms] :right]` |
| `let` binding with later uses | `[[:form fetch] [:find cache-key] [:where {:parent-tag :vector}] :right]` |
| comments between siblings | `:right` skips the comment but preserves it during replacement |
| duplicate labels | Read reports all; planning refuses rather than choosing one |

At least one real-program-derived fixture and one self-hosting query against
clj-surgeon must pass.

## Read contract

Success returns:

```clojure
{:operation :lens
 :file "src/state.clj"
 :query [[:form transition] [:find :finish] :right]
 :trace [{:step [:form transition] :input-count 4 :output-count 1}
         {:step [:find :finish] :input-count 1 :output-count 1}
         {:step :right :input-count 1 :output-count 1}]
 :match-count 1
 :matches [{:inside "transition"
            :tag :list
            :address {:preorder 42}
            :line 12
            :end-line 12
            :source "(assoc state :status :done)"}]
 :source-hash "..."}
```

Zero and multiple results are successful discovery evidence. Invalid EDN,
empty or oversized queries, malformed steps, unsupported predicates, and a
non-first `[:form ...]` step refuse with `:error-type :invalid-query`, the step
index and offending step when available, and the supported grammar. Source
parse failures remain `:invalid-source`.

## Plan contract

A terminal `[:replace FORM]` changes the result from a read to a pure plan. It
must be the final step, and `FORM` must be exactly one complete form. Planning
requires exactly one result from the navigation prefix; zero refuses with
`:no-match`, multiple with `:ambiguous-match`. A transform in any other position
or more than one transform refuses with `:invalid-query`.

The emitted artifact uses the existing plan version and
`:operation :replace-subform`, so the existing applier remains the only write
path. Its selector is:

```clojure
{:query [[:form transition]
         [:find :finish]
         :right
         [:replace (assoc state :status :complete)]]
 :selection-query [[:form transition] [:find :finish] :right]
 :expected-match-count 1}
```

Application replays the recorded complete-file address against the unchanged
source hash; it never reruns the query. Every refusal leaves bytes unchanged.

## Non-goals for v1

- No arbitrary Clojure evaluation or user predicates.
- No jq punctuation parser.
- No macro expansion, scope inference, or semantic claims.
- No fuzzy matching, automatic first match, `:nth`, `:take`, or bulk writes.
- No `:delete`, insertion, wrapping, computed update, or multi-node span
  replacement in v1. Those are candidate terminal transformations only after
  `[:replace FORM]` proves the getter/setter model.
- No claim that reader-conditional platform filtering is solved. Raw CST
  queries may return multiple branches, and writes then refuse.
- No removal of existing high-frequency commands.

## Test matrix

Pure tests cover every step alone and in composition; empty, singleton, zero,
and multiple streams; nested and same-line nodes; comments; duplicate source
values with distinct addresses; malformed query/step/predicate combinations;
the 32-step bound; result evidence truncation; all target program shapes; and
query-plan replay, stale-source refusal, invalid replacement, and complete-file
parse preservation.

Boundary tests cover raw CLI query parsing, canonical and alias dispatch,
read and plan exit status/EDN, plan file output, help examples, and unchanged
bytes on refused planning/application. Documentation anti-drift assertions
cover README, help, changelog, repository instructions, and the installed skill.

## Hill-climb gates

1. Record the old route and metrics for the target tasks.
2. Add failing pure and CLI tests before implementation.
3. Implement the v1 algebra and tag the working read-only summit.
4. Reuse the selector for planning and tag the working mutation summit.
5. Run clean Codex sessions with only the installed help/skill and realistic
   goals. Inspect every command and strict verification field.
6. Treat each detour as a product defect, update the smallest owning surface,
   add a permanent regression, commit, tag, and rerun from a fresh context.
7. Stop only when the safe target route is both minimal and independently
   chosen on replication.

Completion requires formatter, clj-kondo, the complete test suite,
`make install`, real-program dogfood, benchmark evidence, updated README/help/
skill/changelog/vision, a captain's-log entry, and an annotated final summit
tag.

## Experiment record

| Summit | Commit/tag | Evidence |
|---|---|---|
| Baseline | `lens-hillclimb-v0-baseline` | Existing `:cat` plus subtree selector workflow. |
| Contract | `lens-hillclimb-v1-contract` | Getter/updater EDN algebra, failure matrix, and non-goals fixed before implementation. |
| Pure algebra | `lens-hillclimb-v2-pure-algebra` | Red-first exhaustive pure tests; case, cond, map, binding, comments, duplicates, bounds, refusals, planning, and apply. |
| CLI | `lens-hillclimb-v3-cli` | `:lens` / `:q`, raw query parsing, help, plan persistence, subprocess exits, and self-hosting query. |

The first clean read agent, before the skill taught `:q`, needed seven calls:
instructions, `:ls`, a wrong `:cat`, help, a wrong scoped search, the correct
owner read, and a final scoped search. One `:q` expression returned the same
exact node and semantic path during dogfood. A clean edit agent followed the
old skill perfectly in three structural calls (`:cat`, plan, apply); the
terminal updater can preserve the review boundary in two (plan, apply). These
transcripts are the input to the next fresh-context replication, not a claimed
final result.
