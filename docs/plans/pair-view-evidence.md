# Pair-View Evidence Experiment

**Status:** H1 accepted; `[:partition-all N]` contract fixed before implementation

## Question

Does a generic bidirectional pair projection make real agent work shorter and
safer than the shipped `:right` and `[:span 2]` primitives?

Clojure contains several flat alternating sibling sequences:

- `case` test/result arguments after the dispatch expression, with an optional
  unpaired default;
- `cond` test/result arguments;
- map key/value children;
- binding name/initializer children;
- APIs that conventionally accept alternating key/value arguments.

The shared representation is mechanical, but the offsets, tails, and meaning
are not identical. This experiment must distinguish a useful general lens from
an attractive macro-specific abstraction.

## Current baseline

The shipped algebra already supports singular work:

```clojure
[[:find :finish] :right]
[[:find :finish] :right [:replace NEW-VALUE]]
[[:find :finish] [:span 2]]
[[:find :finish] [:span 2] [:replace-span :finish NEW-VALUE]]
```

A known anchor therefore reaches its value or pair without a pair projection.
The plausible remaining advantage is bounded enumeration: return all pairs as
structured records without printing and interpreting a large owner form.

## Competing hypotheses

### H0: reject `:pairs`

Clean agents use one bounded `:cat` or `:q` read, correctly identify every pair
and optional tail, and do not request another structural operation. A pair view
would enlarge the grammar without reducing calls or errors.

### H1: build a pair view

Across at least two distinct syntax shapes, clean agents independently do one
or more of the following:

- print a large owner only to reconstruct pairs;
- manually count sibling offsets or misclassify an optional tail;
- require more than one structural read;
- emit substantially more source than the requested pair records;
- explicitly identify a reusable pair projection as the missing primitive.

## Clean-context tasks

1. **Case inventory:** Return every test/result pair and the optional default
   from a long function while excluding unrelated body syntax and preserving
   the exact source of each result.
2. **Cond inventory:** Return every guard/result pair from a form containing
   nested `cond` expressions. Do not mix inner and outer branches.
3. **Binding inventory:** Return every top-level binding name/initializer from a
   long `let` whose binding symbols recur many times in its body.
4. **Known-pair edit control:** Change both members of one commented case pair
   while preserving the comment and an unrelated duplicate expression. This
   should remain a `[:span 2]` plan followed by apply.
5. **Flattened-function control:** Address the apparent call body inside
   `#(...)`. This should remain an anchored `[:span N]` task, not evidence for
   pairs unless enumeration is genuinely required.

Agents receive only repository instructions, the installed skill, the task,
and the fixture. They do not receive candidate pair syntax or expected command
routes.

## Evidence recorded per run

- every shell and structural CLI call;
- source bytes returned by structural reads;
- exact pair/tail correctness;
- whether nested sequences remained scoped;
- help, text-reader, outline, or line-number detours;
- manual offset/default reasoning in the transcript;
- the agent's stated missing primitive;
- cumulative and uncached input tokens when run through the benchmark harness.

Correctness is a gate. Fewer calls do not compensate for a missed pair,
misclassified default, lost comment, or widened edit.

## Candidate API, intentionally unfixed

Do not implement from this sketch. If H1 wins, compare at least:

```clojure
;; Promote siblings beginning at the current node.
[... [:pairs {:tail :separate}]]

;; Partition selected parent's children after a caller-supplied offset.
[... [:pairs {:drop 2 :tail :separate}]]
```

A successful design must return located pair records whose `:key` and `:value`
remain individually addressable by the existing plan protocol. It must never
claim that `case`, `cond`, bindings, maps, or function arguments have identical
semantics merely because their concrete children alternate.

## Build threshold

Implement only if:

1. at least two independent clean contexts expose the same mechanical pairing
   detour across at least two syntax shapes;
2. the candidate removes a shell call, a large-owner read, or a demonstrated
   classification error;
3. it remains macro-agnostic and compositional with existing paths;
4. singular mutation remains one non-writing plan plus one verified apply;
5. its behavior and refusal matrix can be stated without semantic inference.

Otherwise record the negative result, retain `:right` and `[:span 2]`, and do
not grow the query language.

## Clean-context evidence

Three clean agents independently completed the inventory correctly. None saw a
candidate syntax or expected command route. Each agent received only the
repository instructions, installed skill, task, and fixture.

| Shape | Structural reads | Source returned | Mechanical work outside the tool | Requested primitive |
|---|---:|---:|---|---|
| `case` plus optional default | 10 (`:cat` + 9 `:q`) | ~1.3 KB | Issued one query per branch and one for the tail | Ordered case pairs plus an explicitly tagged default |
| outer `cond` with nested `cond` result | 2 (`:cat` + `:q`) | ~1.4 KB | Counted seven pairs, converted that to a 14-child span | Outer guard/result pairs |
| `let` binding vector | 2 overlapping `:q` reads | ~980 B | Counted 16 children and manually split eight pairs | `[:partition 2]` or binding pairs |

The `case` agent used `:right` correctly for every singular relation. That is
positive control evidence: the existing primitive is clear, but repetition is
the cost. The `cond` and binding agents used `[:span N]` correctly. That is a
second positive control: spans preserve boundaries, but callers must already
know and count the extent.

The same missing operation therefore appeared in three syntax shapes: group a
selected node and its following semantic siblings into consecutive located
spans. The operation does not need to know what `case`, `cond`, or `let` means.

### Threshold decision

H1 passes all five build gates:

1. Three independent contexts exposed the same pairing detour across three
   syntax shapes.
2. One pair view replaces either nine repeated reads or a redundant owner/span
   read plus manual counting.
3. Suffix partitioning from an explicit structural anchor is mechanical,
   macro-agnostic, and compositional.
4. Singular mutation remains the shipped read-or-plan path through `:right` or
   `[:span 2]`; pair inventory does not authorize silent bulk mutation.
5. The contract and refusal matrix below require no semantic inference.

The agents' macro-specific names are evidence about the user need, not the API
design. We will not add `:case-branches`, `:cond-branches`, or
`:binding-pairs`.

## Earned contract: `[:partition-all N]`

The smallest addition is not a semantic `:pairs` view. It is the enumeration
counterpart to the shipped singular `[:span N]`:

```clojure
[:partition-all POSITIVE-COUNT]
```

Given each current semantic node, the step:

1. starts with that node;
2. takes it and every following semantic sibling under the same parent;
3. partitions that suffix into consecutive located spans of size `N`;
4. emits a final shorter span when a remainder exists;
5. never crosses the parent;
6. preserves the exact concrete-syntax addresses and trivia of every span.

It is valid only as the final read step or immediately before
`[:replace-span FORM ...]`. The existing result limit bounds returned evidence;
`:match-count` remains authoritative and truncation remains explicit.

Each result reuses the existing span schema and adds only partition evidence:

```clojure
{:tag :span
 :address {:preorders [101 102]}
 :count 2
 :forms [":start" "(assoc state :status :running)"]
 :gaps ["\n      "]
 :source ":start\n      (assoc state :status :running)"
 :partition {:size 2 :index 0 :complete? true}}
```

An odd suffix is never silently dropped and never interpreted by the tool. A
`case` default, malformed `cond`, or arbitrary trailing function argument all
have the same mechanical representation:

```clojure
{:count 1
 :forms ["(assoc state :last-unknown-event event-type)"]
 :partition {:size 2 :index 8 :complete? false}
 ...}
```

This is intentionally Clojure's `partition-all`, not `partition`: retaining the
remainder is the safe behavior. The caller decides what that remainder means.

### One-shot routes

No owner-form read or known branch name is required:

```clojure
;; `case`: skip list head and dispatch expression, then enumerate the suffix.
[[:form route-event] [:find case] :up :down :right :right
 [:partition-all 2]]

;; `cond`: skip only the list head.
[[:form classify-request] [:find cond] :up :down :right
 [:partition-all 2]]

;; `let`: enter the binding vector and enumerate from its first child.
[[:form prepare-request] [:find let] :up :down :right :down
 [:partition-all 2]]
```

The tool still does not claim those forms are maps. It exposes their shared
flat sibling structure. In the `case` interpretation, callers may treat the
first and second `:forms` entries as test/result or key/value; in a `let`, they
are name/initializer.

### Getter/updater boundary

`[:partition-all N]` returns located spans, so the existing
`[:replace-span FORM ...]` is its updater without a second mutation language.
The existing gates remain unchanged:

- zero partitions: `:no-match`;
- multiple partitions: `:ambiguous-match`;
- one partition with different replacement arity: `:span-arity-mismatch`;
- one equal-arity partition: one hash-bound `:replace-span` plan.

Normal singular edits should continue to use an anchored `[:span 2]`. The new
step earns its place by making bounded inventories one shot, not by replacing
the clearer singular operation.

### Deliberate exclusions

- No `:drop`, `:take`, `:tail`, `:nth`, arbitrary slicing, or computed-update
  sublanguage.
- No `:key`/`:value`, `:test`/`:result`, or binding labels in generic output.
- No macro expansion, default detection, validation, or semantic inference.
- No silent bulk mutation: multiple produced spans still refuse planning.
- No ownership guess for trivia between adjacent partitions. Trivia inside a
  partition is retained exactly; trivia between partitions belongs to neither.

### Required refusal and boundary matrix

Tests must fix:

- zero, negative, string, missing, and surplus arguments;
- nonterminal use, transformations other than `:replace-span`, and application
  after an existing span;
- empty, singleton, even, and odd sibling suffixes;
- multiple input anchors and overlapping suffixes;
- comments inside versus between partitions;
- maps, binding vectors, `case`, outer `cond` with a nested result, flattened
  `#(...)`, reader conditionals, discards, and metadata;
- the 100-result truncation boundary and trace counts;
- zero/many/one-match updater behavior, replacement arity, stale hashes, and
  exact plan replay.

## Implementation gates

- [x] Fix the observable contract before code.
- [x] Record a real-program-derived fixture and clean-context baseline.
- [ ] Add red pure tests for the full grammar, boundary, trivia, and updater
  matrix.
- [ ] Implement the smallest contract without weakening existing tests.
- [ ] Integrate CLI help, README, skill, vision, and changelog with anti-drift
  assertions.
- [ ] Format, lint, run focused and full tests, and run `make install`.
- [ ] Replicate all three inventories in fresh clean contexts.
- [ ] Record call/token/source-output deltas and the Captain's Log.
- [ ] Tag every reversible hill-climb point, commit, push, merge to `main`, and
  remove the experiment branch after verification.
