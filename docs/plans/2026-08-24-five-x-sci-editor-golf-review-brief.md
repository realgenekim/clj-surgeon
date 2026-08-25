# Review brief: how does the SCI scalpel become five times faster than native editing?

**Status:** independent design review requested from Codex Sol and Fable  
**Decision boundary:** the model already knows the complete intended change  
**Target:** one-shot materialization in approximately five seconds, or a clear
account of why that target is physically or architecturally misplaced

## Goal

The desired experience is an expert Emacs or vi gesture:

```text
visualize the complete change
  -> express one small structural relation
  -> bang: every intended edit is committed atomically
  -> parser/hash/count/read-back proof is terminal
```

The tool must help an LLM do better work faster and safer. It must not maximize
its own use. Native `apply_patch` is a fearsome competitor and remains the
right control. The ambitious target is a 2--5x complete-wall improvement on
the strata where structural compilation genuinely contributes.

## Current interface

One known owner:

```clojure
(-> (form 'retry-policy)
    initializer
    (match :retry-delays)
    right
    (transform (fn [delays]
                 (mapv (partial + 100) delays))))
```

Every matching owner in one file:

```clojure
(-> []
    (match :retry-delays)
    right
    (transform (fn [delays]
                 (mapv (partial + 100) delays))))
```

The MCP request also supplies `file`, `commit=true`, exact `expect.matches`,
and `expect.max_changed_characters`. The request count is authoritative; a
redundant inline `expect-count` is no longer taught. The compiler selects exact
concrete nodes, computes replacements in capability-limited SCI, lowers them
to frozen structural addresses, hash-fences the source, commits atomically,
reparses and reads back, and returns an inverse receipt. The persistent MCP JVM
runs with `-Xmx512m`.

## What has been proved locally

### The executor is not the twenty-second bottleneck

- direct hot live preview: **0.5 s** end to end through the warm JVM;
- median `inspect_clojure` service wall over the latest study: **118 ms**;
- 39 inspections consumed **10.020 s total** service wall;
- hot implementation/schema reload after repair: **1.79--2.42 s** without a
  JVM restart;
- one fresh benchmark MCP cold bootstrap: **11.367 s**, but production uses a
  shared persistent service and the benchmark caller wall is measured after
  readiness.

### First-call ergonomics determines whole rounds

The first ten-site caller copied a one-owner example, selected a nonexistent
`retry-policy`, and safely refused ten expected versus zero found. Wall was
**38.227 s**. After the contract explicitly distinguished `(form 'owner)` from
the file-wide `[]` root, a fresh caller chose `[]` immediately and committed
the exact ten-site change in **25.815 s**. One description correction removed
one recovery round and improved wall by **1.48x**.

### SCI caller effort remains flat with edit count

All cells below used fresh Sol/high callers, one committed tool call, zero
source reads, and zero failed mutations after the root-contract repair:

| Sites | SCI semantic | SCI byte-exact | SCI wall | Native semantic | Native byte-exact | Native wall |
|---:|---:|---:|---:|---:|---:|---:|
| 1 | yes | yes | 25.712 s | yes | no | 21.347 s |
| 10 | yes | yes | 25.815 s | yes | no | 28.783 s |
| 30 | yes | yes | 25.033 s | yes | no | 45.551 s |
| 60 | yes | yes | 26.339 s | yes | no | 22.400 s |

Native deleted one trailing blank line in each sample. That is a useful
byte-fidelity diagnostic but is not considered a consequential correctness
failure here. Native is admitted to the speed comparison. The one-replicate
native wall is extremely noisy, and site count alone does not establish an SCI
speed advantage because one `apply_patch` call can carry many homogeneous
hunks.

### Caller context and output

The one-site Sol/high transform turn used:

- total input: **44,100 tokens**;
- cached input: **37,120**;
- uncached input: **6,980**;
- output: **735 tokens**, of which **360** were reasoning output;
- tool arguments: approximately **381 characters**;
- successful MCP result: approximately **395 characters**;
- complete caller wall: **25.712 s**.

The event route was only:

```text
turn start -> short agent message -> transform_clojure -> terminal receipt -> short final message
```

No source discovery or recovery action occurred.

### Model-routing probe, not the main strategy

One exact cell each produced: Sol/high 25.712 s, Sol/medium 22.936 s,
Terra/high 20.435 s, Terra/medium 20.552 s, and Terra/low 20.262 s. All were
one-shot and exact. This suggests a faster materializer model may be safe after
judgment is complete, but model shopping is not the requested product strategy
and does not yield 5x.

## Ethnographic warning

Fast tools do not guarantee fast work. In the latest bounded receipt,
`inspect_clojure` had 39 calls but a median batch size of one request. The
primary seat spent roughly four human-visible minutes resolving a tiny builder
composition bug even though each structural read took 39--792 ms. The caller
serialized perception across model turns. The desired default is:

```text
one batched perception snapshot
  -> one compiled guarded transaction
  -> one warm semantic proof
```

## Current hypotheses

1. Complete wall is dominated by model pre-tool inference and context loading,
   not the SCI compiler or filesystem commit.
2. Site count is the wrong independent variable when both SCI and native can
   express a homogeneous change in one tool action.
3. The largest wins come from eliminating model boundaries: reacquisition
   reads, malformed first calls, recovery attempts, fragmented mutations, and
   redundant verification.
4. The current surface compiles one relation in one file. Real visualized work
   is often heterogeneous and multi-file. A `programs` transaction that
   compiles several bounded SCI relations into one atomic receipt may expose
   the actual 2--5x stratum.
5. Long tool descriptions and a 44k-token caller context may impose a latency
   floor even when most input is cached.
6. Asking the frontier model to translate a decided change into an editor
   program may itself be redundant. A smaller deterministic expression
   compiler, direct path/transform fields, selection handles, or a faster
   materializer seat may be needed.

## Questions for independent reviewers

1. Where is the observed 20--26 seconds most likely spent? Separate pre-tool
   model inference, cached/uncached context processing, tool execution,
   receipt interpretation, and final narration. Name measurements that would
   falsify your decomposition.
2. Why is SCI already competitive with native `apply_patch`, despite requiring
   the model to write a small program? What mechanism is doing useful work?
3. What concrete interface could plausibly reduce an already-decided change
   from about 25 seconds to about 5 seconds without moving architectural
   judgment into the tool?
4. Should the next surface be:
   - the current one-expression SCI tool with better teaching;
   - direct `path` plus `transform` fields;
   - snapshot-bound selection handles;
   - a multi-program, multi-file atomic transaction;
   - a tiny catalog of high-frequency mechanical gestures;
   - a faster materializer model behind the primary agent;
   - or something else?
5. Which proposal respects the bitter-lesson boundary and which merely hides
   brittle refactoring opinions in a DSL?
6. What benchmark would make a 5x claim credible? Include semantic correctness,
   consequential unrelated changes, complete wall, model/tool actions, payload
   size, refusal/recovery rounds, and historical counterfactual edits.
7. Identify the smallest next experiment with the highest information gain.
8. Be adversarial: explain why 5x may be impossible or the wrong objective for
   one-shot single-action tasks, and identify the task stratum where it becomes
   plausible.

## Evidence paths

```text
/tmp/clj-surgeon-local-computed-pair-dogfood-20260824T203306/
/tmp/clj-surgeon-local-computed-10site-sol-high-20260824T205252/
/tmp/clj-surgeon-local-computed-10site-rootfix-sol-high-20260824T205646/
/tmp/clj-surgeon-local-computed-30site-sol-high-20260824T205822/
/tmp/clj-surgeon-local-computed-60site-sol-high-20260824T210122/
/tmp/clj-surgeon-local-transform-terra-high-20260824T204021/
/tmp/clj-surgeon-local-transform-sol-medium-20260824T204146/
/tmp/clj-surgeon-local-transform-terra-medium-20260824T204310/
/tmp/clj-surgeon-local-transform-terra-low-20260824T204505/
/tmp/clj-surgeon-agent-usage-20260825T004916Z-20260825T033020863771Z.json
```

The repository observation with implementation and test detail is
`docs/observations/2026-08-24-sci-programmable-edit-dogfood.md`.
