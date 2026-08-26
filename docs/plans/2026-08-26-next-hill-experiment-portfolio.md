# Next-Hill Experiment Portfolio

**Date:** 2026-08-26  
**Status:** recommended first batch; no new product mechanism admitted yet  
**Decision handle:** `HILL-1`  
**Default:** strengthen native, then measure the decision-completeness boundary

## Ground truth

The latest frozen historical extraction produced:

| Route | Exact outcome | Median complete wall | Median actions |
|---|---:|---:|---:|
| Direct extraction MCP | 2/2 correct | 37.871 s | 2 |
| Plan + apply MCP | 2/2 correct | 49.941 s | 3 |
| Native control | 0/2 correct | 122.278 s | 5.5 |

This proves one narrow law: a complete structural decision should not pay a
planning tax. It does not yet prove a general 2--5x advantage. The task was a
4,594-line, 15-owner extraction whose source, destination, owner set,
visibility change, caller scope, and verifier were all supplied. MCP was tuned
through several falsification cycles while native remained frozen. Native's
0/2 result means 3.23x is an observed route ratio, not a matched-correctness
speedup.

## Independent review

Codex Sol/high ranked these highest:

1. find the external-caller boundary for direct extraction;
2. generalize compiled decision chords beyond extraction; and
3. route by decision gaps, resolving only what is unknown.

Fable ranked these highest:

1. build a mechanically sampled representative historical corpus;
2. strengthen native until it stops being a strawman; and
3. measure a decision-completeness staircase to find where planning earns its
   call.

Both rejected prompt golf, universal heavyweight verification, JVM/parser
optimization as the primary hill, mandatory planning, and adoption as a
success metric.

## Ten hills

### 1. Champion-native audit

- **Why it might be right:** Native `apply_patch` is the real competitor. A
  native route allowed one coherent patch or script, one exact verifier, and
  repair after verifier failure may become correct and much faster.
- **Cost:** Very low; prompt and replay work only.
- **Assumption:** The previous native failures were strategy failures rather
  than an inherent inability to preserve the extraction.
- **Cheapest falsifier:** Have an agent explicitly incentivized to make native
  win design the prompt, then run two fresh Sol/high trials on the frozen task.
- **Gate:** Either obtain a correct native latency or strengthen the evidence
  that the correctness moat is structural. Never use incorrect arms in a
  matched-speed claim.

### 2. External-caller extraction boundary

- **Why it might be right:** Caller migration is the first realistic pressure
  that may make the planner earn its call. Exact supplied caller changes should
  still fit one atomic transaction; unknown callers must not.
- **Cost:** Low; one natural historical fixture plus three ablations.
- **Assumption:** Caller decisions, not raw source movement, dominate realistic
  extraction planning.
- **Cheapest falsifier:** Run no-caller, one-supplied-caller,
  several-supplied-callers, and genuinely-unknown-caller cells through direct,
  plan+apply, and champion-native routes.
- **Gate:** Fully supplied cells must be correct in one mutation plus one
  verifier and at least 20% faster than plan+apply. Unknown callers must route
  to planning or refuse without loss.

### 3. Decision-completeness staircase

- **Why it might be right:** "Plan only unknown decisions" needs a measured
  crossover, not a slogan. Removing caller accounting, visibility, owner
  closure, and destination policy one at a time reveals the price of each gap.
- **Cost:** Low to medium; uses the existing fixture and harness.
- **Assumption:** Prompt completeness can be varied without leaking the hidden
  answer through residual wording.
- **Cheapest falsifier:** One screening run per route/rung, followed by a
  second counterbalanced run only for cells that remain correct.
- **Gate:** Seek a monotone crossover. If direct correctness fails at the first
  missing decision, keep the fast path narrow. If planning never wins, its
  current contract is too expensive.

### 4. Representative historical corpus

- **Why it might be right:** One hero extraction can prove existence but not a
  product. Real commits provide before state, after state, and often the real
  verifier without inventing toy changes.
- **Cost:** Medium; most work is fixture curation and semantic scoring.
- **Assumption:** A mechanical sampling rule can resist selecting only tasks
  favorable to structural tools.
- **Cheapest falsifier:** Sample six Clojure-touching commits before inspecting
  their content, spanning extraction, rename/callers, move, heterogeneous
  batch, small edit, and namespace surgery.
- **Gate:** Expand only if Surgeon is correct on at least five of six and
  materially faster on at least four. A 2x claim requires matched correctness
  across at least eight representative tasks.

### 5. Decision-gap routing

- **Why it might be right:** Field latency came from repeated partial reads.
  Zero gaps should mutate directly; one gap should require one targeted read;
  closure-sensitive ambiguity should invoke the planner.
- **Cost:** Medium if implemented as policy; low if first tested as prompt
  routing over frozen packets.
- **Assumption:** Missing facts can be named mechanically rather than guessed
  by a brittle classifier.
- **Cheapest falsifier:** Replay one task family with zero, one, and three
  explicit missing facts; count phases and incorrect guesses.
- **Gate:** Zero-gap stays at two actions; one-gap uses at most one read plus
  mutation and beats full planning by 30%; ambiguous cases never guess.

### 6. Read-mission compression

- **Why it might be right:** The observational cohort contained 230 structural
  read phases but only 16 apply phases. A single coherent read snapshot may
  turn `read -> read -> read -> decide` into `read once -> mutate once`.
- **Cost:** Low for hindsight replay; high only if a new mission compiler is
  later earned.
- **Assumption:** Most later read targets were nameable from the original task,
  not discovered sequentially.
- **Cheapest falsifier:** Replay five retained read-heavy turns and ask whether
  one batch, constructed only from the original prompt, would have supplied
  every fact used by the final decision.
- **Gate:** At least three of five routes compress to no more than two
  structural calls without hindsight-only inputs.

### 7. Compiled decision chords across operation families

- **Why it might be right:** Historical tasks often contain a complete
  multi-file decision disguised as editing work. One chord can batch exact
  replacements, deletions, renames, visibility changes, and movement.
- **Cost:** Low if current `edit_clojure`/`apply_clojure_changes` already cover
  the task; high if bespoke operations proliferate.
- **Assumption:** The advantage transfers beyond extraction and owner deletion.
- **Cheapest falsifier:** Six historical tasks across at least three
  non-extraction shapes, using current APIs before adding machinery.
- **Gate:** At least five of six correct and at least 2x faster than a correct
  champion-native arm. Stop if two shapes require bespoke product semantics.

### 8. Snapshot/manifest reference instead of retranscription

- **Why it might be right:** When a read or plan was genuinely necessary, the
  model should not retype owners, hashes, and guards it just received. A
  snapshot-bound handle plus explicit decisions could preserve attention and
  stale-source safety while shrinking output.
- **Cost:** Medium; introduces retained identity and lifecycle.
- **Assumption:** Argument transcription is a material share of complete wall
  and reviewing the manifest does not require retransmitting it.
- **Cheapest falsifier:** First measure winning request bytes and timestamps.
  Build nothing unless transcription plausibly costs at least 15% of wall.
- **Gate:** Same stale-source/refusal behavior and at least 15% complete-wall
  reduction. Reject if the handle becomes an unread rubber stamp.

### 9. Refusal-recovery mean, not happy-path median

- **Why it might be right:** A 24% happy-path win disappears if one common
  refusal adds 90 seconds. Complete owner vocabulary and executable correction
  may halve recovery turns without weakening authority.
- **Cost:** Low; most evidence exists in retained telemetry.
- **Assumption:** Deliberate product tests can be separated from natural caller
  refusals.
- **Cheapest falsifier:** Measure refusal-to-next-success wall for retained
  production-like failures and replay ten common refusal types.
- **Gate:** Natural refusal recovery costs under 10% of aggregate route wall,
  or becomes the next engineering priority. Non-authoritative hints never
  select or mutate.

### 10. Verification-boundary compression

- **Why it might be right:** A repository-declared exact verifier could execute
  inside the atomic transaction, removing one model/tool boundary while
  retaining rollback.
- **Cost:** Medium and security-sensitive; arbitrary task commands are not an
  acceptable API.
- **Assumption:** The boundary costs enough to matter and repository-declared
  profiles can exactly preserve warning/error semantics.
- **Cheapest falsifier:** Replay four distinct verifier contracts, including
  `clj-kondo --fail-level error`, through external and declared-profile routes.
- **Gate:** Identical accept/reject behavior and at least 15% lower complete
  wall. Reject on any semantic strengthening or weakening.

## Recommended attack sequence

```text
Batch 0: make native dangerous
  champion-native on current hero task
        |
        +-- correct -> first matched-correctness ratio
        |
        `-- incorrect -> stronger evidence for the safety/correctness moat

Batch 1: find the routing boundary
  external-caller fixture + completeness staircase
        |
        +-- direct wins with 0--1 gaps -> widen compiled-decision chords
        |
        +-- planner wins with >=2 gaps -> build decision-gap/read compression
        |
        `-- neither wins reliably -> fix refusal/verification ergonomics first

Batch 2: test the product, not the anecdote
  mechanically sampled six-task historical corpus
        |
        +-- >=2x at matched correctness across strata -> expand to 8+ tasks
        |
        `-- win confined to extraction -> market it and route it as extraction
```

## Recommendation

Choose `HILL-1`: run the champion-native audit and freeze the
external-caller/completeness staircase before adding product code.

The deciding argument is information value. Strengthening native cannot lose:
it either produces the first honest matched-correctness speed ratio or proves
that Surgeon's meaning-preserving transaction is the moat. The staircase then
turns the shipped routing law into a measured boundary and tells us whether the
next implementation hill is compiled writes or compressed reads.

Fallback: if experiment capacity is limited, run only two champion-native
trials and one natural supplied-caller cell. Do not optimize the JVM, add a
routing classifier, or enlarge the API until those results arrive.
