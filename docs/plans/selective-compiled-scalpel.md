# Selective Compiled Scalpel

**Status:** active experiment  
**Decision:** optimize complete verified task time, not structural-tool adoption  
**Primary gate:** corrected 51-edit / nine-file historical counterfactual

## Outcome

Make clj-surgeon decisively faster than native editing when a coding agent has
already made a complete mechanical Clojure decision, while routing discovery,
small visible edits, prose, and unsupported work to their cheaper native tools.

The intended interaction is not a general structural IDE. It is a compiler:

```text
Small visible edit        -> native patch -> proportional verification
Unknown surface or design -> rg/read/nREPL -> decide -> one chosen editor
Complete mechanical plan  -> one edit_clojure transaction -> terminal receipt
```

The success condition is a dependable church-organ gesture: the model names the
complete change once, the tool performs it atomically, and no second edit,
source reread, editor switch, or Surgeon-specific verification ceremony is
needed.

## Evidence that changed the question

A bounded 2026-08-25--26 usage receipt found 28 Codex turns that used Surgeon:

| Observed route | Turns | Median complete turn | Median actions | Median route phases |
|---|---:|---:|---:|---:|
| Surgeon used somewhere | 28 | 9.08 min | 11 | 6 |
| native patch and no Surgeon | 4 | 7.48 min | 3 | 2 |

This is observational, not causal: Surgeon is selected for harder tasks and the
native-only sample is only four turns. It nevertheless reveals the product
failure. Those Surgeon turns contained 230 structural-read phases but only 16
structural-apply phases. Nine Surgeon-using turns later used native patching.
Surgeon direct action wall was usually less than one percent of complete turn
wall.

The service itself is not slow at the action boundary:

| Action | Count | Median direct wall | p90 |
|---|---:|---:|---:|
| Surgeon | 248 | 427 ms | 1,656 ms |
| native `apply_patch` | 142 | 628 ms | 1,312 ms |

The primary drag is decision fragmentation and mixed-tool routing, not parser
latency. Optimizing another 100 ms cannot recover minutes spent rereading,
replanning, refusing malformed requests, or switching editors.

## Controlled evidence

Two completed historical strata establish the current crossover:

| Fully supplied decision | Compact median | Native median | Result |
|---|---:|---:|---:|
| six small edits across two files | 29.893 s | 31.378 s | compact 1.05x |
| 22-owner extraction cleanup | 32.546 s | 150.138 s | compact 4.61x |

The first falsifies “batching alone creates a 2--5x win.” Both routes can batch
small replacements, so model startup dominates. The second shows the actual
mechanism: owner-level intent prevents the model from reading and reproducing
hundreds of deleted source lines.

The active generalization case is derived from production commit
`fc014632a5160b0f199387e0ec48982e0d9be975`:

- 51 exact edit intents;
- nine source and test files;
- 14 obsolete top-level owners with attached comments;
- namespace require migrations and dispersed caller rewrites;
- 429 removed lines; and
- exact-byte plus Clojure-parse gates.

The first admitted exact pair on Anvil completed at 59.879 seconds compact
versus 269.173 seconds native, a 4.50x paired win. Compact used one MCP action
and zero shell commands. Native used five actions and its first patch safely
failed before the second succeeded.

The complete retained first cohort is deliberately messier:

| Seat | Compact | Native | Compact wall | Native wall | What the failure taught us |
|---|---|---|---:|---:|---|
| dev-a | exact-byte false | exact | 67.635 s | 135.080 s | prompt indentation was ambiguous |
| dev-b | exact-byte false | exact-byte false | 65.066 s | 439.382 s | same prompt defect; native retained 14 blank lines |
| dev-c | exact | exact | 59.879 s | 269.173 s | admitted 4.50x paired win |

All three compact arms completed the same 51-edit transaction in one MCP call,
without refusal. Two reproduced the task's six-space Markdown bytes literally;
one inferred the intended three-space source. This is why the corrected fixture
gets a new commit and a fresh cohort rather than a retroactive score. Native
also showed nontrivial variance: two arms needed a second patch attempt, and one
still failed the byte oracle after committing semantically correct source.

## Fixture defect discovered by dogfooding

One compact arm completed all 51 edits atomically in 67.635 seconds but missed
the exact-byte oracle in nine files. Forensics proved:

- every parsed Clojure form was semantically equal;
- all 17 mismatched logical lines were namespace indentation only;
- no formatter ran;
- `within.namespace` preserved the supplied literal replacement bytes; and
- the raw Markdown task supplied six leading spaces while the accepted files
  contained three.

The model faithfully compiled an ambiguous benchmark prompt. This is a fixture
defect, not a source-fidelity defect in Surgeon. The original receipt remains
invalid for an exact efficiency claim. The task must be corrected with
column-zero source fences, committed as a new benchmark version, and rerun. No
old result may be overwritten or reclassified as exact.

## Product decisions

### 1. Make the selective route explicit

Use compact `edit_clojure` only when the decision already identifies exact
files, owners, old forms, replacements, counts, computed relations, or owner
deletions. Do not preflight-read an already-decided change. Use native patching
for a small visible literal edit, prose, a new file, or an unsupported shape.

When the affected surface is unknown, use `rg`, a bounded source read, or nREPL
to decide. Use `inspect_clojure` only when structural ownership, graph evidence,
or a frozen multi-owner snapshot materially changes the decision, and batch the
complete read once.

### 2. Make `edit_clojure` the primary compiled entrance

One compact request must comfortably express:

- exact namespace edits;
- several owner-scoped literal substitutions;
- exact top-level owner deletion with attached comments;
- insertions adjacent to named owners;
- bounded pure computed replacements; and
- one atomic transaction across files.

Heavyweight `apply_clojure_changes` remains for retained semantic decisions,
operations absent from the compact editor, or verification gates that must
participate in rollback. It is not the ordinary editing entrance.

### 3. Make one-shot payloads forgiving but mutations strict

Derive aggregate counts instead of requiring the caller to duplicate them.
Ignore redundant top-level expectations when exact per-edit guards are already
authoritative. Accept harmless representation variance. Refuse ambiguity,
stale source, overlapping intent, malformed Clojure, or unsafe scope before
writing. Return the smallest actionable correction when refusal is necessary.

### 4. Preserve source literally

Literal replacements remain literal. Do not automatically format a compact
transaction merely to make a benchmark pass. A formatter may run only under the
same repository policy that would apply after native editing. Task fixtures
that promise exact forms must expose exact bytes without Markdown presentation
indentation.

### 5. Treat terminal evidence as terminal

After one successful mutation, do not reread or diff because Surgeon performed
the edit. Run the same proportional formatter, linter, and affected test that
the native route warrants. The tool receipt already owns atomic commit,
read-back, parsing, snapshot guards, and mutation counts.

## Corrective work

1. Correct the submission-row task's source fences to column zero.
2. Add a portfolio verifier regression that rejects indented Clojure fences so
   source-shaped prompt bytes cannot silently include Markdown presentation
   indentation.
3. Run the complete zero-model portfolio self-test and commit a new benchmark
   SHA without changing the retained first cohort.
4. Repeat three counterbalanced Sol/high Anvil pairs on the corrected SHA.
5. Require compact to be exact in 3/3 runs, one mutation action in 3/3, zero
   source reads, zero refusals, and zero editor switches.
6. Require a median paired speedup of at least 2x and report every native and
   compact failure separately. Do not exclude an incorrect arm from the
   correctness denominator.
7. Compare request shapes from every compact run. If callers choose materially
   different encodings for the same supplied decision, remove that API choice
   or make the compiler choose the source-preserving representation.
8. After the corrected case passes, replay one small known-owner edit and one
   discovery-heavy change to confirm the router abstains where native should
   win.

## Telemetry required for production learning

Record these at the task boundary rather than celebrating raw MCP call counts:

- structural reads before first mutation;
- distinct route phases;
- editor switches;
- refusals and failed mutations before success;
- source characters returned to the model;
- mutation actions and edit/file batch size;
- post-decision rereads;
- complete verified task wall; and
- semantic correctness separately from exact-byte agreement.

The product goal is not more Surgeon usage. It is fewer mechanics after the
model knows what should change.

## Falsification conditions

The selective compiler thesis is weakened if the corrected multi-file case
cannot reach 3/3 exact one-shot success, if request-shape variance remains high,
if compact callers reread supplied source, or if the speedup disappears when
native uses one correct batched patch.

If that happens, keep the proven large single-file owner-deletion route, narrow
the routing rule further, and stop expanding the API. Native remains the
default outside the evidence-backed stratum.
