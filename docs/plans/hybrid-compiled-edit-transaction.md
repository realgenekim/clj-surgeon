# Hybrid compiled edit transaction

**Status:** Accepted experiment

## Outcome

Make `edit_clojure` the smallest public compiled editor gesture for an
already-decided Clojure batch. One request may contain exact literal `edits`
and bounded computed `programs`. The server compiles every operation against
one original multi-file snapshot, rejects overlaps or stale assumptions, and
performs one reversible atomic commit.

This is an experiment to beat native `apply_patch`, not an API-preservation
exercise. Existing `edits` requests remain valid because that costs little and
reduces deployment risk, but compatibility must not force extra fields,
aggregate expectations, sequencing semantics, or another public tool.

## Public contract

```json
{
  "workspace_root": "/workspace",
  "edits": [
    {
      "file": "src/app.clj",
      "within": {"form": "ide-shell"},
      "from": ":body",
      "to": ":body.ide-shell-page",
      "matches": 1
    }
  ],
  "programs": [
    {
      "file": "src/policy.clj",
      "expression": "(-> [] (match :retry-delays) right (transform (fn [xs] (mapv (partial + 100) xs))))",
      "expect": {
        "matches": 12,
        "max_changed_characters": 384
      }
    }
  ]
}
```

For the first implementation, `edits` remains the required non-empty array and
`programs` is an optional non-empty array. Singular or program-only preview
work remains on `transform_clojure`. This restriction avoids inventing a
second execution path before the mixed batch proves its value.

No `commit`, operation discriminator, program ID, aggregate expectation, or
verification mode is required. `edit_clojure` is the committed gesture; array
order is diagnostic only.

## Semantics

1. Resolve every path beneath one workspace root.
2. Read every distinct file at most once.
3. Compile direct edits and every SCI expression against the original bytes.
4. Enforce exact per-edit and per-program match counts.
5. Enforce each program's changed-character budget and aggregate server caps.
6. Refuse computed replacement of a comment-bearing selected subtree.
7. Reject overlapping concrete addresses, including direct/program overlap.
8. Parse every future file and retain original whole-file hashes.
9. Compare-and-swap and commit all changed files once; roll back all writes on
   failure.
10. Read back every file and return one inverse receipt. A successful terminal
    response must not echo every expanded concrete edit.

Programs never observe another edit or program's proposed output. If the task
requires sequential visibility, the decision is not a frozen edit batch and
does not belong on this surface.

## Non-goals

- no sequencing or general-purpose editor-control DSL;
- no inference of callers, architectural changes, or additional files;
- no formatter, linter, test, or cold-verification orchestration;
- no computed preservation of comments or exact source spelling;
- no claim that literal-only batches must beat native wall time;
- no immediate removal of `transform_clojure` or
  `apply_clojure_changes` during the experiment.

## Behavior matrix

| Case | Required result |
|---|---|
| legacy direct batch | unchanged successful contract |
| mixed same-file batch | one combined commit |
| mixed multi-file batch | one combined commit and receipt |
| program count mismatch | typed refusal, zero writes |
| program churn overflow | typed refusal, zero writes |
| comment-bearing computed selection | typed refusal, zero writes |
| direct/program overlap | typed refusal, zero writes |
| stale file after compilation | compare-and-swap refusal, zero partial state |
| second-file write failure | rollback all earlier writes |
| malformed SCI | typed program-indexed refusal, zero writes |

## Local proof

Use the existing `decision-batch-edit` historical capsule. Encode four supplied
literal changes as direct edits and two as SCI programs. Make no pre-read.
Require:

- one public `edit_clojure` call;
- six concrete edits across two files;
- one atomic commit and inverse receipt;
- exact accepted bytes and declared after hashes;
- a counted-refusal variant that leaves both files byte-identical;
- request bytes, response bytes, compile/commit wall, and first-call validity.

Also compare the same outcome encoded all-direct and all-SCI. This is an
ergonomics and payload comparison, not evidence that either route beats native.

## Fresh-agent falsification

Give fresh Sol/high callers a historical mixed decided batch: several literal
changes across two files plus one current-value-dependent relation over 10--30
narrow leaves. Supply paths, owners, exact literal before/after forms, computed
relationship, exact cardinalities, and churn bound, but no diff, line numbers,
surrounding source, or accepted hashes.

Run eight paired trials in ABBA order:

- Surgeon arm: `edit_clojure` is the only mutation tool.
- Native arm: `apply_patch` is the only mutation tool.
- Both may read if they choose; both may batch one mutation action.

Pre-register the win:

- 8/8 correct Surgeon outcomes;
- zero silent wrong-site or consequential unrelated edits;
- Surgeon wins at least 6/8 paired walls;
- median paired advantage exceeds 20% and five seconds;
- no worse p90 complete wall;
- materially smaller mutation payload.

Measure complete wall, time to first tool, service wall, post-tool continuation,
reads, mutation attempts, refusals, argument/result bytes, uncached tokens, and
first-call validity. Failure to clear the gate falsifies the complete-wall
advantage for this stratum even if the engine remains fast and safer.

## Reviewer synthesis

Fable preferred adding transform items to `apply_clojure_changes` because it
already owns heterogeneous multi-file batches. Sol preferred `edit_clojure`
because the larger tool already exposes basis, decisions, changes, extraction,
verification, and several expectation meanings. Both rejected SCI-only literal
encoding and a sequencing DSL.

The deciding argument is model-side simplicity: engine execution is already
78.399 ms while complete turns cost 20--26 seconds. The winning surface must
reduce schema reading, output tokens, tool-selection thought, and recovery
rounds. `edit_clojure` with sibling `edits` and `programs` adds the least new
vocabulary and no new catalog choice.
