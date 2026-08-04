# Outermost Lens Filter

**Status:** Kept; implemented, fully verified, and independently one-shot on
the nested-`cond` and disjoint nested-`case` shapes
**Motivating evidence:** The post-`[:partition-all 2]` clean nested-`cond`
inventory required a second structural query solely to remove contained owner
matches.

## Outcome

A caller can find repeated nested syntax, promote each match to its owning
subtree, and retain all structurally outermost owners in the same query:

```clojure
[[:form classify-request]
 [:find cond]
 :up
 :outermost
 :down
 :right
 [:partition-all 2]]
```

The result contains the seven outer `cond` pairs. The nested `cond` remains one
result subtree. No owner read or fixture-specific first guard is required.

## Bitter-Lesson Boundary

`:outermost` is a generic relation over the current stream of concrete-syntax
locations. It does not know `cond`, `case`, macro expansion, scope, or meaning.
It does not choose the first match. It can retain many disjoint roots.

Do not add `:innermost`, depth parameters, ancestor predicates, `:first`,
indexing, or macro-specific owner filters in this experiment.

## Public Contract

`:outermost` is a bare stream step.

Given the current deduplicated stream of located nodes, it retains each item
for which no other distinct current item is a strict concrete-syntax-tree
ancestor. Equivalently, it keeps all maximal items under structural
containment.

- Zero items remain zero.
- One item remains one.
- An ancestor/descendant chain retains only the ancestor.
- Multiple disjoint maximal items all remain.
- Siblings and cousins all remain.
- Equal locations deduplicate before containment filtering.
- Output order remains the input preorder.
- Containment uses zipper ancestry, not lines, byte ranges, tags, head names,
  or evaluation.
- The step accepts node items only. Existing terminal span grammar prevents
  use after `[:span N]` or `[:partition-all N]`.
- Downstream navigation, filtering, reads, and singular planning continue to
  use the existing contracts.

Placement is explicit. These queries are different:

```clojure
;; Correct: promote head tokens to their containing lists before containment.
[[:find cond] :up :outermost]

;; The symbol nodes do not contain one another, so both remain.
[[:find cond] :outermost :up]
```

Malformed vector spellings such as `[:outermost]` and
`[:outermost ANYTHING]` refuse as `:invalid-query`. A bare `:outermost` after a
terminal span or partition also refuses.

## Safety Invariants

- The filter never converts many disjoint matches to one.
- A downstream mutation still requires exactly one final node or span.
- Plan application replays the recorded address and never reruns
  `:outermost`.
- Comments, whitespace, metadata, discards, and reader conditionals do not
  affect containment or source bytes.
- Existing result limits and trace counts remain authoritative.

## Implementation Shape

Add one supported bare step to the lens parser. In the pure query evaluator,
build the set of current location identities. Retain an item only when none of
its `z/up` ancestors appears in that set. Reuse existing stream deduplication,
ordering, trace, match, planning, and application code.

Do not compare line ranges or construct a second parsed tree.

## Test Plan

Add red pure tests before implementation for:

1. zero and singleton streams;
2. duplicate locations;
3. two-node and three-node ancestry chains;
4. siblings and cousins;
5. multiple disjoint outer trees containing nested matches;
6. containment across differing tags;
7. stable output order and per-step trace counts;
8. the exact nested-`cond` fixture query;
9. a different nested same-head `case` fixture with two disjoint outer cases;
10. one downstream guarded plan and an ambiguous disjoint-owner refusal;
11. vector spellings and placement after terminal spans/partitions;
12. metadata, reader conditionals, comments, and source preservation.

Run the existing lens and full suites without weakening assertions.

## Documentation and Release Checklist

- Add one exact nested-`cond` example to `:q --help`, README, canonical skill,
  legacy skill, repository instructions, vision, and changelog.
- Add anti-drift assertions for the operator and the required `:up` placement.
- Record pre/post clean transcripts in the Captain's Log.
- Keep `[:find FIRST-OUTER-GUARD]` documented as the shorter route when that
  guard is already known.

## Verification Gates

1. Format every changed Clojure file.
2. Run the pure focused tests.
3. Run clj-kondo on all changed Clojure files.
4. Run the full suite.
5. Run `make install` and verify the installed CLI and skill.
6. Give a fresh agent the unknown-source nested-`cond` task. Require one `:q`
   source invocation and no owner read.
7. Give a second fresh agent the nested-`case` task with two disjoint outer
   owners. Require one `:q` source invocation and both outer owners.

## Definition of Done

The experiment survives only if both clean agents independently use
`:up :outermost` and complete their tasks in one structural source invocation.
All focused and full tests must pass with a larger assertion count. If either
agent still needs a source read or positional choice, improve the instruction
once and repeat. If the second shape does not need the same relation, remove
the operator and retain the negative evidence.

## Verification Result

The focused suite passed 37 tests and 415 assertions. The full suite passed 409
tests and 2,789 assertions, with no weakened tests. clj-kondo reported zero
errors and zero warnings. The installed CLI exposed the documented help and
returned the expected trace for both motivating fixtures.

Two clean-context agents independently read the complete 240-line skill and
then chose `:up :outermost` without design history. The nested-`cond` agent
returned all seven outer pairs in its first and only source invocation. The
disjoint nested-`case` agent returned all four outer pairs from both owners in
its first and only source invocation. Neither read an owner, chose a position,
or retried. The operator therefore passes the keep gate.
