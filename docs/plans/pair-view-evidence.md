# Pair-View Evidence Experiment

**Status:** Evidence gathering; no feature authorized by this plan

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

## If implementation is earned

Before code, fix an observable contract for empty/singleton/even/odd streams,
offsets, tails, nested and overlapping sequences, comments, reader
conditionals, ambiguity, result bounding, path replay, and every malformed
option. Add red pure tests, one real-program-derived fixture, CLI/help/skill
anti-drift tests, clean-context replication, formatter/lint/full-suite gates,
and a reversible annotated tag.
