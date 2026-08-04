# Captain's Log: siblings became a sequence

The question began with a useful category error: if a Clojure `case` looks like
alternating keys and values, could a jq-like query enter it, split those
conditions into pairs, and edit the corresponding value?

The intuition was right about the structure and wrong about the semantics. A
`case` form is a list whose macro arguments contain a dispatch expression,
alternating test/result forms, and an optional default. It is not a map. A
`cond`, map, binding vector, and alternating function call also contain flat
sibling runs, but those siblings do not share one meaning.

That distinction gave us a falsifiable experiment. We did not implement
`:case-pairs`, `:key`/`:value`, or the first attractive
`[:pairs {:drop 2 :tail :separate}]` sketch. We first asked clean agents to
inventory three real-program-derived shapes with the shipped lens.

The results were correct and expensive in the same mechanical way:

| Task | Structural reads | Repeated work |
|---|---:|---|
| Eight `case` branches plus default | 10 | One `:cat`, eight value queries, and one tail query |
| Seven outer `cond` branches with a nested `cond` result | 2 | Count seven pairs, convert them to a 14-form span |
| Eight `let` bindings whose names recur later | 2 | Read the vector, read an overlapping 16-form span, then split it manually |

All three agents used existing primitives correctly. `:right` remained the
best operation for one known peer value. `[:span 2]` remained the best operation
for one known pair. The gap was enumeration: no operation turned a known
sibling suffix into all consecutive groups.

An independent API critique removed two ideas that the evidence did not earn.
First, `:drop` would preserve macro-dependent offsets inside the new feature.
Second, a configurable tail policy would ask the tool to decide whether an odd
suffix was valid. The smaller answer already existed in Clojure's vocabulary:

```clojure
[:partition-all 2]
```

The step starts at the current node and partitions that node plus all following
semantic siblings under the same parent. It returns located spans. A shorter
last span is retained and marked `:complete? false`. The tool does not call it
a `case` default, malformed `cond`, or trailing argument. The model interprets
the evidence.

This is the jq idea at the correct layer. jq provides a small algebra over
structure and lets the caller compose selection with update. clj-surgeon now
does the same over concrete Clojure syntax:

```clojure
;; One known value.
[[:find :finish] :right]

;; One known pair.
[[:find :finish] [:span 2]]

;; Every consecutive pair from a known first sibling.
[[:find :start] [:partition-all 2]]
```

The result does not synthesize a wrapper map or label members as key and value.
It returns neutral `:forms`, exact source, gaps, addresses, and partition
cardinality. A nested `cond` result stays one subtree because partitioning uses
direct semantic siblings. Internal comments belong to their partition;
comments between partitions belong to neither.

The updater also stayed small. A partition is the same located span shape that
`:replace-span` already understands. One equal-arity partition can produce the
existing hash-bound plan. Zero partitions refuse as `:no-match`. Multiple
partitions refuse as `:ambiguous-match`. Unequal arity refuses as
`:span-arity-mismatch`. Enumeration does not imply a bulk write.

The red suite was intentionally larger than the implementation. Before code,
9 tests and 98 assertions fixed:

- empty, singleton, even, and odd suffixes;
- `case`, nested `cond`, bindings, maps, anonymous functions, reader
  conditionals, discards, and metadata;
- comments inside and between partitions;
- overlapping anchors and identical-span deduplication;
- the 100-result evidence bound;
- malformed sizes and invalid pipeline positions;
- zero, many, unequal, and one-match updater behavior;
- exact address replay and complete-file hash fencing;
- a real-program-derived fixture and a subprocess CLI route.

The focused structural suites then passed 36 tests and 443 assertions with a
clean clj-kondo result. Existing tests were not weakened. Two red expectations
were corrected when they contradicted established contracts: identical address
vectors deduplicate, and stale apply reports `:source-hash-mismatch`.

The deeper result is methodological. The jq analogy helped us see a missing
structural operation, but the clean-context transcripts determined its shape.
The first design had options because we were imagining an API. The final design
has one positive integer because agents repeatedly performed one mechanical
operation. That is the Bitter Lesson boundary working as intended: improve the
general substrate, keep semantics and judgment outside the kernel, and turn
every observed confusion into a permanent test or instruction.

The post-implementation test was deliberately behavioral. Three fresh agents
received the original inventory tasks, the installed skill, and no design
history.

The `case` agent used one `:q` source invocation. It returned eight complete
pairs and one explicit incomplete remainder. The binding agent also used one
`:q` source invocation and returned eight exact pairs without confusing later
symbol uses. Neither agent used `:cat`, counted children, or reconstructed
pairs.

The nested-`cond` agent exposed the next selector gap. Its intuitive first query
used `[:find cond]`, matched both the outer and inner forms, and returned nine
correct partitions. The agent then issued a second query anchored at the first
outer guard and returned exactly seven outer pairs. `[:partition-all 2]` did
its job in both scopes, but the complete task was not one-shot.

That miss is now evidence for a separate, generic question: should the lens
filter a current stream to structurally outermost matches? Such a filter could
remove contained matches without choosing an arbitrary first result or knowing
what `cond` means. We will not hide the second call with a benchmark average or
add the operator from one appealing example. The next experiment must show
that the same contained-match detour recurs across independent shapes.
