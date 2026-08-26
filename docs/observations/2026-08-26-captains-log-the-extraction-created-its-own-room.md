# Captain's Log: The Extraction Created Its Own Room

Date: 2026-08-26

## Mission

Test whether the compiled extraction route can generalize the earlier compact
editor win to a materially different historical refactor: move 15 formatting
forms out of Sessionize's 4,594-line `views.clj` into a new nested namespace,
preserve callers and comments, make exactly one private helper public, and pass
the affected lint gate.

The fixture freezes the real historical before/after pair. Exact bytes remain
useful secondary evidence, but correctness means lossless Clojure syntax after
removing only whitespace and commas. Comment, metadata, reader-discard, string,
regex, form-order, and source-anchor changes remain failures.

## The first immutable Anvil pair was ugly and useful

Commit `895647f`, dev-a, Sol/high, MCP-first, one replicate:

| Arm | Complete wall | Harness correct | Exact | Actions | Failed mutations | Route adherent |
|---|---:|---:|---:|---:|---:|---:|
| MCP plan + apply | 79.679s | false | false | 5 | 1 | false |
| Native | 114.959s | false | false | 5 | 0 | true |

Those booleans hid two different stories.

- The MCP receipt hashes were byte-identical to both frozen after files. Its
  `views.clj` false negative came from raw `node/sexpr` equality: reader-generated
  symbols inside unchanged `#(...)` forms are not stable across independent
  parses. The lossless syntax comparison was true.
- Native made a genuinely different program surface. It omitted the required
  `:as format` alias and omitted the destination namespace docstring.
- MCP's first required plan call safely refused with
  `target-parent-not-found`. The destination was
  `src/cfp_scheduler_killer/views/format.clj`, and the `views/` directory did
  not exist. A shell `mkdir` plus a repeated plan made the mandated two-call
  route impossible before the model reached the edit.

The 79.679s versus 114.959s timing is therefore not yet a claimed win. The
first cohort is retained as immutable evidence of route and scoring defects.

## Four ratchets fell directly out of the failure

### 1. A new namespace owns safe parent creation

Planning now resolves a nested absent target under its nearest existing real,
project-confined ancestor without creating anything. Apply creates each still
absent directory shallowest-first, records only directories it actually
created, and includes them in the extraction receipt.

Undo and verification rollback remove the new file and those directories in
reverse order. If a path appears between plan and commit, apply refuses rather
than adopting or deleting another actor's directory.

### 2. Formatting is scoped to created namespaces

Extraction no longer sends every future source and caller file through the
whole-file formatter. Structural deletion, require insertion, and guarded
caller edits already preserve surrounding bytes. Only newly created files need
whole-file formatting.

This removes broad unrelated reindentation from the transaction and shrinks
formatter work from two files to one on the frozen case.

### 3. The source require is inserted where a human expects it

The target namespace libspec is inserted before the first lexically greater
namespace instead of always being appended after third-party dependencies.
The insertion preserves existing trivia. A comment-bearing require clause
keeps the old append behavior so a new entry cannot silently capture a comment
that described its former neighbor.

### 4. Lossless syntax is the correctness authority

The portfolio scorer still publishes raw semantic equality as diagnostic
evidence, but `correct` now follows the lossless syntax comparison. A permanent
test proves that unchanged anonymous-function source with whitespace drift is
correct even when generated-symbol identity makes raw `sexpr` equality false.
Existing tests still reject comment loss or alteration, reader-discard loss,
metadata loss, string changes, regex changes, and parse failures.

## Local full-fixture proof after the ratchets

The real 15-form fixture was replayed through the hot project nREPL:

- plan: successful; exact required public form was `not-blank`;
- apply: successful, 15 edits across two files, no refusal;
- apply wall: 6.494s;
- formatter: one file, 153ms, zero additional target changes;
- destination namespace: byte-exact against the historical target;
- existing source: meaning-preserved, presentation-only drift;
- clj-kondo: 0 errors, 17 inherited warnings;
- undo and deliberately failed verification both restored the original source
  and removed the new file plus newly created directory.

Warm focused proof: path tests 1/10, extraction tests 6/48, and the public MCP
extraction witnesses passed. Cold `make test` passed 610 core tests / 5,240
assertions and 228 MCP tests / 1,872 assertions, plus heap, cclsp, stdio,
benchmark, skill, and evidence gates.

## What this changes in the hill climb

The complete extraction decision can now genuinely fit the intended route:

```text
exact roots + destination
        |
        v
one snapshot-bound plan  ---> complete manifest + public_forms + next_call
        |
        v
one atomic apply          ---> create directories + format target + receipt
        |
        v
one proportional lint gate
```

The next Anvil cohort must run from the new immutable commit, include both
orders, and retain every failed arm. Success means both correctness and route
adherence before speed is interpreted. The hypothesis is now sharper: the win
comes from compiling directory creation, dependency planning, visibility,
source require placement, target formatting, mutation, and rollback into two
model-visible calls—not from making any individual file operation faster.
