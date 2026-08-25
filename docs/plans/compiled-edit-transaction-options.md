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

## Same-data comparison

| Option | Model expression burden | Calls after decision | Server state | Bitter-lesson risk | Plausible >=5x stratum |
|---|---:|---:|---:|---:|---|
| A — keystrokes | high | 1 | none | lowest | exact nested and supplied literal batches |
| B — packet | medium-high | 1 | none | low | heterogeneous multi-edit decisions |
| C — handles | low after `see` | 1 | snapshot cache | low-medium | read-then-edit work with costly reacquisition |
| D — refactor compiler | lowest | 1 | none or snapshot cache | highest | dense moves, extraction, deletion |
| E — change buffer | medium | 3+ | mutable buffer | medium | long decisions assembled incrementally |

## Discriminating experiments

Use exact correctness as an admission gate. Measure complete caller wall from
the decision boundary, first mutation success, tool actions, payload bytes,
failed mutations, foreground verification cycles, and stale-source behavior.

1. **Six exact heterogeneous edits:** A versus B versus C. This isolates schema
   and handle burden without rewarding a high-level refactor operation.
2. **Seventeen owner deletions:** B versus C versus D. The model receives all
   owner names. No discovery is allowed.
3. **Real namespace extraction:** A versus B versus C+D versus E. The model
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

Codex Sol/high and Fable will rank the five options against the same scenario
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

## Recommendation

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

## Build and test order

1. Run the historical A drift corpus. Measure exact first-call acceptance;
   classify spelling, count, ambiguity, and wrong-owner failures.
2. Race B, C, and D on the 17-owner deletion. C may reuse a supplied handle
   snapshot but may not add a read inside the clock.
3. Race A, B, C+D, and native on one real namespace extraction decision packet.
4. Add C only if step 2 or 3 clears its 30% keep gate. Add no general stateful
   buffer.
5. Claim >=5x only for a stratum with all fresh callers correct, zero failed
   mutations, and a replicated median >=5x over matched native materialization.
