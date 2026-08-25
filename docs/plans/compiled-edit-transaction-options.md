# Compiled edit transaction: five candidate architectures

**Decision handle:** `compiled-edit-v1`
**Target:** >=5x faster verified materialization than native editing after the
model can state the complete change.

## Ground truth

The whole-task experiments locate a routing crossover but do not meet the
editor-golf goal. A native-positive change became 1.70x faster after a prompt
intervention eliminated unnecessary MCP use. A complex multi-owner task used
one structural read and five fewer actions, yet model deliberation erased the
saved wall time.

The relevant clock begins later:

```text
open-ended judgment                 compiled materialization
discover -> understand -> decide | express -> guard -> commit -> prove
                                  ^ clock starts here
```

The model retains judgment. The tool can own syntax addresses, stale-source
guards, future-file construction, formatting, atomicity, read-back, focused
verification, and undo.

## Common scenario

Use the same already-decided change to evaluate every option:

```text
Move four named handlers from server.clj to scheduling.clj.
Rewrite two exact caller sites.
Delete two obsolete owners.
Preserve unrelated bytes.
Run one hot extraction law.
```

Every option must refuse before write if the source changed. Every successful
option must commit all files or none and return a hash-fenced undo receipt.

## A — Guarded keystroke microkernel

```json
{"edits":[
  {"file":"src/app.clj","owner":"route","old":"(old x)","with":"(new x)"},
  {"file":"src/app.clj","owner":"obsolete","delete":true}
]}
```

**Why it might be right:** This is the smallest bitter-lesson surface. The
model already knows exact syntax and can emit several compare-and-swap edits in
one call. The kernel adds safety without encoding refactoring opinions.

**Cost:** The model must construct destination namespaces, require changes,
caller rewrites, ordering, and every deletion itself. Large moves become long
payloads and repeat source text.

**Assumption underneath:** Better models will reliably carry all mechanical
bookkeeping once exact local edits are expressible.

**Likely LLM failure:** Correct architectural intent becomes an incomplete set
of low-level edits or a quoting-heavy malformed request.

## B — Stateless explicit decision packet

```json
{"snapshot":"sha256:...",
 "changes":[
   {"forms":["handle-a","handle-b"],"delete":true},
   {"forms":["routes"],"find":"old","replace":"new"}
 ],
 "expect":{"changes":2,"edits":3,"files":2}}
```

**Why it might be right:** This is close to today's transaction compiler. One
self-contained request is replayable, reviewable, easy to log, and independent
of server session state. Exact counts make omissions visible.

**Cost:** The caller repeats files, owners, old forms, hashes, and counts that
the preceding read already established. Payload construction remains a model
task and one malformed field can cost a recovery round.

**Assumption underneath:** Better schema language and examples can make a
large stateless packet first-call reliable.

**Likely LLM failure:** The model understands the edit but mistranslates its
decision into nested JSON, mismatched counts, or redundant guards.

## C — Snapshot-bound handles

```text
see -> {snapshot: S7, refs: {handle-a: F17, routes/site-2: P42}}

bang {snapshot: S7,
      edits: [{at: F17, delete: true},
              {at: P42, old: old-form, with: new-form}]}
```

**Why it might be right:** The structural read becomes an editor selection.
The model reuses compact, opaque references instead of reacquiring locations or
resubmitting source identity. Each handle is already bound to workspace, file,
owner, structural path, and source hash.

**Cost:** Handles add lifecycle, expiry, observability, and recovery design.
They can become magical if the response does not also show human-recognizable
file and owner identity. A caller that never needed a read gains nothing.

**Assumption underneath:** Most dense edits follow one useful structural read,
and models will copy short handles more reliably than full selectors.

**Likely LLM failure:** The model mixes snapshots, loses the handle-to-intent
mapping, or treats an expired handle as a tool outage instead of stale source.

## D — High-level refactor compiler

```json
{"program":[
  {"op":"extract-owners",
   "from":"src/server.clj",
   "owners":["handle-a","handle-b","handle-c","handle-d"],
   "to":"src/scheduling.clj",
   "require_policy":"minimal"},
  {"op":"rewrite-callers","sites":["routes/site-1","routes/site-2"],
   "replacements":["scheduling/handle-a","scheduling/handle-b"]},
  {"op":"delete-owners","owners":["legacy-a","legacy-b"]}
]}
```

**Why it might be right:** This deletes the most work. The model states the
architectural decision at its natural granularity. The compiler owns namespace
headers, dependency ordering, exact deletion boundaries, future-file parsing,
atomicity, and undo. The original 20-call and 118-call field refactors asked for
exactly this compression.

**Cost:** Each high-level operation needs a complete contract and real-program
tests. A growing catalog can become a brittle model of refactoring judgment and
lose to better models or native tools.

**Assumption underneath:** A small stable set of operations can remain purely
mechanical: extract, move, delete, rewrite declared callers, and update declared
namespace dependencies.

**Likely LLM failure:** The model expects the compiler to infer an undeclared
caller or architectural policy, or chooses the wrong high-level operation when
low-level edits would be simpler.

## E — Stateful editor macro / change buffer

```text
stage S7 (delete F17)
stage S7 (replace P42 old -> new)
preview buffer B9
commit B9 verify hot
```

**Why it might be right:** This matches how an Emacs expert works. The model
can accumulate a coherent macro while thinking, inspect the aggregate diff,
then execute one atomic commit. Small stage calls avoid one giant JSON payload.

**Cost:** It adds server-side mutable session state, abandoned buffers,
cross-agent ownership, expiry, concurrency, preview drift, and cleanup. Several
stage calls still incur several model/tool boundaries before the one commit.

**Assumption underneath:** Incremental staging prevents more malformed packets
than the extra round trips and lifecycle complexity cost.

**Likely LLM failure:** The caller forgets which buffer is active, stages a
decision twice, commits an incomplete buffer, or leaves durable garbage after
the coding session dies.

## F — Programmable structural transaction

```clojure
(transaction
  (-> (form 'route-event)
      (match :finish)
      right
      (expect-count 1)
      (transform #(assoc % :status :complete)))
  (-> (forms 'legacy-a 'legacy-b)
      delete))
```

The existing capability-limited Clojure interpreter evaluates a pure path and
transform program against one frozen `.clj`, `.cljs`, or `.cljc` snapshot. It
cannot perform I/O, start processes, mutate host state, or commit source. The
planner discards the executable function after evaluation, retains only
concrete A-style guarded edits, validates cardinality and every future file,
then uses the ordinary atomic transaction and receipt machinery.

**Why it might be right:** This is the literal Emacs model. The LLM writes a
small, one-use editor program in the same homoiconic language as the code. A
general path algebra plus ordinary pure Clojure can express computed changes
without a growing catalog of refactor opinions. Better models make this surface
more capable rather than obsolete.

**Cost:** The model must write and debug a second program before changing the
first. SCI syntax, quoting, structural-node/data conversion, cardinality, and
concrete-source preservation can create refusal rounds. A pure sandbox limits
capabilities but does not prove termination. Cross-file namespace mechanics and
semantic caller completeness still need explicit inputs or libraries.

**Assumption underneath:** Fresh models write short pure Clojure transforms
more reliably than they fill a large nested transaction schema or enumerate
many literal replacements.

**Likely LLM failure:** The transform selects the wrong structural level,
canonicalizes spelling or comments unintentionally, returns the wrong shape,
performs unbounded work, or expresses an architectural inference that the
mechanical compiler cannot verify.

## Same-data comparison

| Option | Model expression burden | Calls after decision | Server state | Bitter-lesson risk | Plausible >=5x stratum |
|---|---:|---:|---:|---:|---|
| A — keystrokes | high | 1 | none | lowest | exact nested and supplied literal batches |
| B — packet | medium-high | 1 | none | low | heterogeneous multi-edit decisions |
| C — handles | low after `see` | 1 | snapshot cache | low-medium | read-then-edit work with costly reacquisition |
| D — refactor compiler | lowest | 1 | none or snapshot cache | highest | dense moves, extraction, deletion |
| E — change buffer | medium | 3+ | mutable buffer | medium | long decisions assembled incrementally |
| F — programmable transaction | low for relational edits | 1 | none or snapshot cache | lowest if kept mechanical | computed and repeated structural changes |

## Discriminating experiments

Use exact correctness as an admission gate. Measure complete caller wall from
the decision boundary, first mutation success, tool actions, payload bytes,
failed mutations, foreground verification cycles, and stale-source behavior.

1. **Six exact heterogeneous edits:** A versus B versus C versus F. This
   isolates schema, handle, and transform-program burden without rewarding a
   high-level refactor operation.
2. **Seventeen owner deletions:** B versus C versus D versus F. The model receives all
   owner names. No discovery is allowed.
3. **Real namespace extraction:** A versus B versus C+D versus E versus F. The model
   receives the chosen owners, destination, caller decisions, and hot law. The
   tool must not infer architecture.
4. **Concurrent edit:** Change one addressed form after decision capture. Every
   option must refuse with zero bytes changed and one executable recovery.
5. **Fresh-caller translation:** Give only the public schema and decision
   packet. Count malformed first calls before measuring speed.

Run three fresh Sol/high and three Fable callers per admitted cell before
expanding. A >=5x claim requires all callers correct, zero failed mutations,
and a median >=5x advantage over the matched native materialization control.

## Decision pending independent review

Codex Sol/high and Fable first ranked five options against the same scenario
and gates. Their reasoning will be recorded here before implementation chooses
a primary architecture and fallback.

## Independent reviews

### Codex Sol/high

Sol ranked **C > D > B > A > E** and chose snapshot-bound handles as the
primary architecture with guarded keystrokes as the fallback.

Its deciding argument was preservation of pointing decisions. If inspection
already proved the exact owner or site, the write should say "edit the thing I
just selected" instead of retransmitting old source, selectors, and hashes.
Sol recommended typed, human-labelled handles over a mutable server session,
with all operations lowering to A-style guarded edits. It would add D later as
sugar over that substrate.

Sol's primary warning was coverage: handles are safe only for sites that the
read actually exposed. Undiscovered callers remain a semantic-proof problem.
It proposed adversarial stale snapshots, duplicate names, reader conditionals,
and an intentionally undiscovered caller as required trials.

### Fable

Fable ranked **A > B > D > C > E** and chose guarded keystrokes as the primary
architecture with the stateless decision packet as fallback.

Its deciding argument was the benchmark premise: the clock begins after the
model has visualized the change. Requiring a new `see` call at that boundary
taxes C with a two-call floor and re-derives evidence the model may already
hold. A is the smallest durable interface and should improve as models improve
at rendering exact source. Owner scope plus compare-and-swap turns ambiguity
or spelling drift into safe refusal.

Fable's primary warning was exact-source drift. It proposed a hard empirical
gate: if fresh callers' first A request accepts at least 85% of real historical
edits, keep A primary; if acceptance falls below roughly 60%, C's premise wins.
It treated E's three-call floor and mutable lifecycle as disqualifying for this
decision boundary.

## The disagreement that matters

The choice is not "opaque handles or exact source" in the abstract. It depends
on whether a useful structural read already occurred during visualization:

```text
no prior structural selection        prior structural selection exists
-----------------------------        ---------------------------------
A: old + new + owner guard            C: handle + new
one call                              one additional call
```

Forcing C to manufacture a handle is ceremony. Discarding a handle that a
necessary read already produced is reacquisition. The public architecture must
support both without making either the universal preflight.

## Revote after adding F

The first ballot omitted machinery that already exists. `:edit :expr` runs a
capability-limited Clojure program in SCI, selects exact source through the
structural lens, computes a replacement, and then discards the function. Its
output is an ordinary concrete, hash-bound replacement plan. F therefore is
not a speculative language implementation; it is an incomplete model-facing
frontend over working components.

### Codex Sol/high revote

Sol reranked the set **F > C > D > B > A > E**. It chose F as the primary
model-facing language for computed structural work, C-style snapshot handles
as its preferred inputs, and A as its sole commit IR and direct fallback.

Its strongest argument is asymptotic: A requires the model to materialize each
changed byte and repeat each site, while F expresses one relation whose cost is
independent of site count. D can become a library of audited helpers over F
rather than the ceiling of what the editor can express. Sol's strongest warning
is lossless syntax: ordinary Clojure values do not retain comments, spelling,
reader conditionals, metadata placement, or whitespace. Whole-form read/print
churn is a go/no-go failure. It proposed lossless round-trip, first-run success,
computed-edit crossover, semantic-extraction, and containment experiments.

### Fable revote

Fable reranked the set **A > F > B > D > C > E**. It kept A as the default and
chose F as the computed/bulk escape hatch that compiles to A.

Its strongest argument is modal cost: for one already-known literal change,
`from`/`to` has less vocabulary and a smaller failure surface than a generated
program. A wrong F program can produce a confidently wrong edit at scale, and
the author has not independently materialized the bytes. Fable would require
explicit cardinality and churn budgets for one-shot commit; otherwise F must
return its compiled A edits for one confirmation. It would keep D only for
semantic operations whose laws and namespace/caller mechanics should not be
reimplemented ad hoc.

### Shared conclusion

The reviewers disagree about routing frequency, not the stack:

```text
optional C handles -> F program -----------+
                                             v
optional D helpers --------------------> A guarded concrete edits -> atomic commit
                                             ^
direct literal edit ------------------------+
```

F does not replace A. It is a compiler frontend whose executable code must
vanish before commit. A remains the auditable, replayable, compare-and-swap IR.

## Local dogfood of the existing F machinery

The first probes ran inside the existing clj-surgeon MCP JVM through its
embedded nREPL. No second JVM was started and no source file was written. The
live namespace was reloaded before evaluation.

### Computed leaf edit: excellent

This expression selected one vector and computed its new value:

```clojure
(-> (form 'retry-policy)
    (match :delays)
    right
    (transform #(mapv (partial + 100) %)))
```

Given a map containing `[100 250 500] ; preserve me`, it compiled to a concrete
single-node plan whose diff changed only that vector to `[200 350 600]`. The
adjacent inline comment and every unrelated byte survived. The resulting plan
contained the path, preorder address, before source, after source, source hash,
result hash, and concrete `[:replace [200 350 600]]`; the SCI function was gone.

This felt like the intended editor: state the relation once, receive the exact
mechanical diff, then bang the guarded plan.

### Whole-map sexpr transform: unacceptable without a lossless guard

Transforming the enclosing map with `(assoc % :max-attempts 4)` produced this
proposal:

```diff
-{:delays [100 250 500] ; preserve me
-   :jitter? true}
+{:delays [100 250 500], :max-attempts 4, :jitter? true}
```

The proposal correctly remained plan-only, so nothing was damaged. But plain
sexpr transformation lost the comment and canonicalized layout. F cannot be
defined as arbitrary data-in/printed-data-out over broad forms. It must either
operate on a lossless CST, emit smaller structural edit instructions such as
`assoc_entry`, or refuse when trivia/churn exceeds an explicit budget.

### Repeated matches: the prototype stops too early

A path selecting three `:timeout` values safely refused with
`ambiguous-match`; current `transform` requires exactly one selected node.
Transforming their enclosing vector worked computationally but collapsed three
formatted map lines into one canonical line. The missing high-leverage primitive
is therefore not an interpreter. It is **bounded transform-each lowering**:

```clojure
(-> (form 'configs)
    (match :timeout)
    right
    (expect-count 3)
    (transform-each #(+ % 50)))
```

That program should compile to three disjoint A edits against the frozen
snapshot, preserving every byte between the three selected values. It must
refuse without an exact count, on overlap, or when any generated value is not
readable Clojure.

### The retained-address seam works

A live nREPL prototype connected the existing query result to
`compile-addressed-transaction`. The first attempt incorrectly retained the
semantic path as well as the exact preorder address. Three maps contained the
same `:timeout` path, so that non-unique path could not identify the later
sites. Removing it and using the frozen preorder addresses produced one valid
three-edit transaction:

```diff
-100
+150
-250
+300
-500
+550
```

A second fixture used duplicate values, metadata, commas, multiline maps, and
inline comments. Two `100` leaves became two `150` leaves, and the complete
future source was byte-identical to a replacement of only those six numeral
characters. An incorrect expected count refused before compilation. A virtual
commit succeeded with whole-file read-back hashes; adding a concurrent comment
to the source before commit produced `source-hash-mismatch` and no write.

This proves the important seam already exists:

```text
SCI relation
  -> exact N-node frozen selection
  -> N distinct retained addresses
  -> N concrete A edits
  -> whole-file parse
  -> compare-and-swap commit
```

### First MCP surface and self-edit

`transform_clojure` now exposes that seam locally. Its input is one project-
relative file, one existing SCI path ending in `transform`, an exact match
count, an explicit maximum changed-character budget, and optional
`commit=true`. Preview is the default. One-shot commit refuses when the selected
subtree itself contains a comment; the caller must narrow the selection or use
the reviewed route. The current implementation bounds a request to 128 matches
and 262,144 generated characters.

The new route passed 5 focused tests with 28 assertions, then the complete MCP
gate with 187 tests and 1,514 assertions under a 512 MiB heap. Hot reload added
the fourth tool without restarting the server.

It then edited its own implementation through the live MCP callback:

```clojure
(-> (form 'max-transform-matches)
    initializer
    (transform #(quot % 2)))
```

With `matches=1`, a three-character budget, and `commit=true`, the result was:

```diff
-(def max-transform-matches 256)
+(def max-transform-matches 128)
```

The call returned one compiled edit, atomic commit success, a complete-file
read-back hash, and a concrete inverse receipt. The SCI function was absent
from that receipt. This is the requested reflexive proof: the programmable edit
tool successfully and safely edited itself.

Dogfood found one payload defect: preview initially returned the entire future
file in structured output. That was removed; the compact response retains only
the file, counts, budget use, safety flag, source/result hashes, and diff.

## Recommendation before the F experiments

Choose **A as the universal edit instruction and default public gesture**.

- A is the internal guarded edit representation and the shortest direct
  `edit_clojure` path.
- B batches heterogeneous A instructions into one stateless atomic request.
- D supplies a deliberately small set of high-density mechanical compilers,
  beginning with exact-owner deletion and extraction. Every D operation lowers
  to B/A before commit.
- C is an optional compression of A when a necessary structural read already
  returned the exact snapshot-bound handle. C must never require an extra read.
- E is rejected for v1. Its state, cleanup, and call-count costs solve the wrong
  phase of work.

The deciding evidence is not only aesthetic. Direct `edit_clojure` callers
already achieved 3/3 exact first calls, and installed-skill callers achieved
10/10 exact guarded edits with zero failed mutations. A is a working paved
road. C and D must beat that road on the strata where they claim leverage.

**Fallback:** Elevate C to the default for read-followed edits if the historical
drift experiment puts A first-call acceptance below 85% or if C reduces median
materialization wall by at least 30% without adding a source read. Keep A for
no-read supplied decisions in either outcome.

## Revised recommendation

Build the **F -> A** seam now, but do not declare F the universal default yet.

- Keep A as the sole commit IR and the shortest interface for one literal edit.
- Expose the already-working single-node SCI transform through MCP without
  shell quoting. It must return a concrete diff first; applying it must consume
  that exact hash-bound plan rather than rerun the program.
- Add bounded `transform-each` as the first true F transaction primitive. It
  should lower one exact-count selection to several lossless, non-overlapping A
  edits.
- Reuse lossless operations (`assoc_entry`, rename, sibling insertion) as F
  plan builders. Do not ask an sexpr transform to reproduce CST details it
  never received.
- Keep semantic D operations for extraction, namespace mechanics, and complete
  caller rewrites. They may be callable from F, but their tested laws remain
  authoritative.
- Promote F to the default for computed/repeated edits only after fresh callers
  prove first-program success and zero unrelated-byte edits. A remains default
  for literal one-site work regardless.

This is one architecture with two gestures, not an indecisive pair: direct A
for literal materialization; F compiling to A when computation compresses the
decision.

## Build and test order

1. Run the historical A drift corpus. Measure exact first-call acceptance;
   classify spelling, count, ambiguity, and wrong-owner failures.
2. Implement and locally dogfood an MCP preview/apply surface over the existing
   single-node SCI compiler. The second call must apply the reviewed concrete
   plan without reevaluating SCI.
3. Add bounded `transform-each` and run lossless fixtures containing comments,
   metadata, discard forms, reader conditionals, commas, and odd formatting.
4. Race B, C, D, and F on the 17-owner deletion. C may reuse a supplied handle
   snapshot but may not add a read inside the clock.
5. Race A, B, C+D, F, and native on one real namespace extraction decision
   packet.
6. Add C only if a race clears its 30% keep gate. Add no general stateful
   buffer.
7. Claim >=5x only for a stratum with all fresh callers correct, zero failed
   mutations, and a replicated median >=5x over matched native materialization.
