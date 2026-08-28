# Captain's Log: One Algebra, Two Entrances

Date: 2026-08-27

## Decision

Keep the shared operation algebra and publish it after the remaining release
gates. The CLI and MCP now assign separate trusted contexts, but both compile
and commit `:change` through one transaction engine. Public CLI and MCP result
shapes remain transport-owned.

This closes the architectural defect where `:category :write` described both
previews and mutations. Category is now presentation metadata. Mutation
authority comes only from the catalog, lifecycle, and an internal entrance
profile that request data cannot forge.

## What changed

- `operation-algebra` owns canonical operation identity, lifecycle capability
  derivation, effect authorization, and legal terminal-state classification.
- CLI commit and MCP commit use separate private trusted contexts and the same
  compiler and transaction writer.
- Every CLI terminal path is observed and validated before the unchanged
  legacy result is returned.
- The architecture test inventories the exact preview and commit call graph.
  It rejects category-based authority, unknown writers, lower-layer process
  exit, and unapproved formatter or verifier effects.
- Direct editor requests now split one insertion string containing several
  complete Clojure forms and derive redundant aggregate counts from the exact
  per-change guards. They do not balance parentheses or infer malformed code.
  Invalid syntax refuses before write with the exact input path.

## Independent evidence

### Transport and candidate identity

The same-candidate qualification materialized the CLI and MCP from one exact
checkout. Seven requested forms produced identical semantic facts with SHA-256
`33207cf97f0d082d3a01e8e9c0c57cc331ec66f70c64f0a3a744b91ffb2d47c8`.
The integrated transport differential passed 6 tests and 37 assertions. It
used no model, analyzer, formatter, or verifier.

### Commit and receipt compatibility

The retained five-case matrix covered success, compile refusal, stale source,
restored write failure, and receipt-publication failure. Pre-cutover CLI,
candidate CLI, and candidate MCP matched on every canonical domain outcome.
The candidate preserved exact receipt source, accepted old receipts, and the
old undo compiler accepted the candidate receipt. Fake comparisons performed
zero authoritative commits; one isolated live candidate run performed exactly
one commit and one receipt publication.

Receipt: `docs/observations/2026-08-27-operation-algebra-commit-parity-receipt.md`.

### No-model performance

The algebra did not create a meaningful CLI tax on the frozen
apostrophe-bearing preview:

| Arm | p50 | p95 |
|---|---:|---:|
| Pre-cutover `91b2190` | 158.510 ms | 250.591 ms |
| Candidate `b05b3a0` | 159.765 ms | 256.706 ms |
| Regression | **+0.79%** | **+2.44%** |
| Allowed | +5.00% | +5.00% |

The aggregate contains 128 corrected counterbalanced runs per arm. The earlier
8-run and 20-run cohorts failed their p95 gates under Spotlight and
AddressBook pressure. Those failures remain in the receipt. A 100-run cohort
then passed independently, and the pooled result includes the ugly runs rather
than selecting only favorable evidence.

Receipt: `docs/observations/2026-08-27-operation-algebra-no-model-subprocess-parity.md`.

### Tolerant one-shot compiler

The end-to-end dogfood test submits three edits across two files. One insertion
string contains two complete `deftest` forms, and the supplied aggregate says
31 changes, 32 edits, and 33 files. The compiler derives the authoritative
aggregate as 3 edits across 2 files, commits once, and undo restores both input
files exactly. The paired malformed request has one extra close parenthesis;
it refuses as `invalid-intent-form`, names the exact array path, and changes no
source.

Focused hot verification is 123 tests / 1,376 assertions. Cold core is 636 /
5,466. Cold MCP is 268 / 2,260. All are green.

## A useful adjacent repair

The cold MCP milestone exposed two known analyzer-contract blockers that were
already recorded elsewhere: the timeout test did not provide the new priority
lock path, and implemented analyzer intents 007/008 lacked linked witnesses.
The repaired test now exercises the real temporary lock boundary; the
repository intent audit returns no violations. This did not weaken analyzer
admission or launch a real analyzer.

## Performance interpretation

The algebra is not itself the earlier 5–6x model-level win. It makes that win
safer to retain and easier to extend. The established extraction route remains
the headline: a compiled one-shot mutation plus exact in-transaction verifier
and terminal relay has beaten the retained correct-native median of 122.278 s.
The algebra removes duplicated policy and ensures that future CLI improvements
can reuse the same proven kernel without forcing MCP and CLI into one public
shape.

The immediate naming question should therefore be judged against the final
architecture:

```text
public editor gesture  -> compact guarded transaction
prepared semantic task -> shared operation algebra -> transaction kernel
CLI adapter -----------^                         ^--- MCP adapter
```

The Brain Fleet naming review starts only after release publication. It should
decide whether `edit_clojure` and `apply_clojure_changes` communicate this
boundary, and whether reshaping is better than a cosmetic rename.

## Remaining release gates

1. Run the complete serialized milestone suite and one analyzer admission/lint
   gate without concurrency.
2. Tag the exact green commit.
3. Install CLI and both agent skills from that immutable commit.
4. Announce one shared-MCP window, record PID and CWD, perform one hot reload,
   and prove both tolerant insertion branches in an isolated workspace.
5. Close the window without blind retry. Then begin the naming review owned by
   `clj-surgeon-x9d`.
