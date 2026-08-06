# Structural Intent Transactions

## Status

Implementation plan for the first `:change` / `:change!` transaction on the
`intent-transactions` branch.

The first transform is deliberately narrow: replace every losslessly exact
`:from` form with one exact `:to` form across an explicit set of files. A
transaction may contain many heterogeneous exact intents so one already-formed
model plan does not become 20 or more edit turns. The engine validates their
combined future state, commits the files as one failure-atomic unit, and emits
a hash-fenced inverse receipt.

## Why the old boundary changed

The previous vision rejected multi-edit lens plans because one plan and one
edit kept review and replay simple. Field observation found the cost of that
choice: one coherent change required 23 plan/apply calls. The agent repeatedly
expressed the same mechanical intent, paid process and review overhead for each
occurrence, and eventually returned to native patches.

The new boundary still leaves judgment with the model:

- The model chooses the scope, exact before-state, exact after-state, and
  expected cardinality.
- clj-surgeon locates exact syntax, applies declared mechanical transforms,
  proves the complete future files parse, and enforces snapshot hashes.
- Linters, compilers, tests, and live systems remain the semantic authorities.

This is not an autonomous `:refactor` operation. It does not infer architecture,
choose names, discover an open-ended scope, or widen a requested change.

## Long-term transaction model

```text
explicit scope
  -> declared structural intents
  -> exact captures and source-preserving transforms
  -> aggregate and per-intent guards
  -> combined future-file validation
  -> one failure-atomic commit
  -> one verified, reversible receipt
```

Later intent types may add captures, insertions, deletions, moves, and
graph-aware caller updates. They must compile to the same internal file plans
and transaction protocol. Exact `:from` / `:to` establishes that substrate; it
must not hard-code the engine around one replacement.

## The intent compiler

The model's planning output should become executable data with as little
translation as possible. Treat the public request as source code for a small
intent compiler:

```text
model planning
  -> declarative intent data
  -> normalize and type-check scope, transforms, and assertions
  -> compile selectors into concrete source-addressed edits
  -> link edits into per-file future states
  -> prove cardinality, non-overlap, parse, and snapshot invariants
  -> emit an executable transaction plan
```

The compiler is not a natural-language interpreter. The model performs the
semantic planning and emits explicit EDN. clj-surgeon checks and lowers that
intent into deterministic mechanical operations. This keeps the Bitter Lesson
boundary: stronger models can express stronger plans without requiring the
kernel to guess what the program ought to mean.

The compiler should eventually accept the same composable intent data from the
CLI, an agent tool call, or a saved artifact. No caller should need to manually
translate one coherent plan into 23 unrelated edit commands.

## Primary hypothesis

The dominant cost is not rewriting syntax. It is repeatedly externalizing and
reacquiring a plan that the model already formed. The current interface turns
one coherent decision into many shell launches, plan receipts, parsing steps,
and correction opportunities.

The first experiment therefore accepts a vector of different exact intents.
A structural global replacement is one intent, but it is not the limit of the
transaction:

```clojure
{:intents
 [{:files ["src/a.clj"]
   :from "(old-call x)"
   :to "(new-call x)"
   :expect-count 1}
  {:files ["src/b.clj" "src/c.clj"]
   :from ":body"
   :to ":body.ide-shell-page"
   :expect-count 2}
  {:files ["src/c.clj"]
   :from "[:title \"Mothership\"]"
   :to "[:title (str document-title \" — Mothership\")]"
   :expect-count 1}]
 :expect {:intent-count 3
          :edit-count 4
          :changed-file-count 3}}
```

All intents compile against the same original snapshots. Different intents may
touch disjoint nodes in one file. Identical, ancestor/descendant, or otherwise
overlapping targets refuse; the caller must state one consolidated intent.
Later intents do not search text inserted by earlier intents. This makes the
compiled result deterministic and independent of intent ordering in the first
version.

## Frontier development loop

Build the feature in small, vertically useful batches. Dogfood each batch on
real source before adding the next guarantee. Early friction is product
evidence, not noise to defer until the API is complete.

| Batch | First usable capability | Immediate dogfood question |
|---|---|---|
| 0 | Pure compiler for heterogeneous exact intents over in-memory files | Can one model plan become one artifact without overlap or ordering surprises? |
| 1 | Read-only `:change` over explicit real files | Are scope, matching, comments, diffs, and result size right in one call? |
| 2 | Guard and diagnostic refinement from early dogfood | Does refusal teach the model to repair the whole intent once? |
| 3 | Guarded `:change!` with staged writes and rollback | Can known-safe work beat repeated plan/apply without weakening refusal behavior? |
| 4 | Durable hash-fenced receipt and `:undo-change!` | Is recovery obvious, compact, and independently verifiable? |
| 5 | Clean-context replay and tagged/native controls | Does the whole route reduce turns and wall time on realistic work? |

For every batch:

1. add the smallest failing pure contract tests;
2. implement only enough vertical plumbing to invoke it;
3. run it on copied or read-only real repository source;
4. record surprises in the captain's log and convert them into permanent tests;
5. commit the independently useful increment before expanding the compiler.

Do not wait for the mutation shell to learn whether the intent language is
pleasant. Do not add captures or semantic refactors until exact intents prove
the transaction shape under real use.

## Public surface: first slice

### Preview

```bash
clj-surgeon :op :change \
  :spec '{:intents [{:files ["src/app/a.clj" "src/app/b.clj"]
                      :from "(old-api account)"
                      :to "(new-api account)"
                      :expect-count 3}]
          :expect {:intent-count 1 :edit-count 3 :changed-file-count 2}}'
```

`:change` performs every read, match, guard, and future-state validation. It
does not write source. Its EDN result contains the complete transaction plan,
including per-file edits, hashes, and one aggregate diff.

### Guarded one-shot apply

```bash
clj-surgeon :op :change! \
  :spec '{:intents [{:files ["src/app/a.clj" "src/app/b.clj"]
                      :from "(old-api account)"
                      :to "(new-api account)"
                      :expect-count 3}]
          :expect {:intent-count 1 :edit-count 3 :changed-file-count 2}}' \
  :receipt-out /tmp/api-change-receipt.edn
```

`:change!` accepts the same declarative spec. Every intent requires a positive
`:expect-count`; the aggregate `:expect` map guards the materialized plan as a
whole. Together they are the caller's consent to mutate every exact match in
the explicit scopes. It plans from one snapshot, validates the complete
combined future state, writes the files, reads them back, and emits one receipt.
It does not require a separate plan/apply round trip.

### Hash-fenced inverse

```bash
clj-surgeon :op :undo-change! \
  :receipt /tmp/api-change-receipt.edn
```

The receipt contains concrete reverse edits for each file. Undo requires every
current file hash to equal the transaction's recorded result hash. One stale
file refuses the whole inverse before any write.

## Input contract

| Input | Contract |
|---|---|
| `:spec` | Required EDN map containing non-empty `:intents` and aggregate `:expect`. |
| intent `:files` | Required non-empty vector of distinct `.clj`, `.cljs`, or `.cljc` paths. No implicit project discovery in the first slice. |
| intent `:from` | Required string containing exactly one complete Clojure form. No `_`, regex, or fuzzy matching. |
| intent `:to` | Required string containing exactly one complete Clojure form. Its literal comments, metadata, reader syntax, commas, and layout are preserved. |
| intent `:expect-count` | Required positive integer. That intent's exact match count across its scope must equal it. |
| spec `:expect` | Required exact `:intent-count`, `:edit-count`, and `:changed-file-count` for the compiled transaction. |
| `:receipt-out` | Required for `:change!`; must not alias a source file. Must end in `.edn`. |
| `:receipt` | Required for `:undo-change!`; must contain a supported receipt schema and pass every hash and path guard. |

Unknown arguments refuse. Duplicate or canonically aliased file paths refuse.
`:from` equal to `:to` refuses as a no-op. Missing files, unsupported
extensions, unreadable source, and invalid source all refuse before mutation.

## Exact-match contract

The first transform uses lossless structural equality:

- Whitespace may differ between `:from` and an occurrence.
- Comments, metadata, reader macros, token spelling, collection type, and tree
  position inside the selected form are part of the match.
- An occurrence with an undeclared interior comment does not match and cannot
  silently lose that comment.
- Surrounding bytes and whitespace remain byte-for-byte unchanged.
- The literal `:to` source is inserted exactly. `#()` must not become `fn*`.
- Matching is against the original snapshot. A replacement that itself
  contains `:from` is not rewritten again in the same intent.

The CLI and pure compiler both accept `:intents [...]`. This is the essential
test of the hypothesis: a model can materialize one heterogeneous edit plan in
one transaction instead of translating it into repeated calls.

## Successful preview result

The stable fields are:

```clojure
{:ok true
 :operation :change
 :transaction-version 1
 :intent-count 1
 :match-count 3
 :changed-file-count 2
 :files
 [{:file "src/app/a.clj"
   :match-count 2
   :source-hash "..."
   :result-hash "..."
   :edits [...]}
  {:file "src/app/b.clj"
   :match-count 1
   :source-hash "..."
   :result-hash "..."
   :edits [...]}]
 :diff "..."
 :validated {:whole-files-parsed true
             :file-count 2}}
```

`:change!` adds:

```clojure
{:operation :change!
 :committed true
 :receipt-file "/canonical/path/to/receipt.edn"
 :verified {:whole-files true
            :read-back-hashes {"src/app/a.clj" "..." ...}}
 :inverse {:operation :undo-change!
           :guarded-file-count 2}}
```

The saved receipt contains the concrete reverse edits and hashes. Console EDN
may summarize them, but the saved artifact is sufficient for inverse replay.

## Refusal contract

Every refusal is EDN, has `:error-type`, exits nonzero through the CLI, and
performs no source or receipt write.

| Condition | `:error-type` |
|---|---|
| Invalid or empty `:files` | `:invalid-files` |
| Canonically duplicated path | `:duplicate-file` |
| Unsupported source extension | `:unsupported-file` |
| Missing or invalid source file | `:invalid-source` |
| `:from` or `:to` is not exactly one form | `:invalid-intent-form` |
| `:from` and `:to` are losslessly equal | `:no-op-intent` |
| Missing, zero, negative, or non-integer expectation | `:invalid-expect-count` |
| An intent's actual count differs | `:expect-count-mismatch` |
| A complete future file does not parse | `:invalid-result-source` |
| Receipt path aliases source or is not EDN | `:invalid-receipt-path` |
| Source changes between plan and commit | `:source-hash-mismatch` |
| A write fails and rollback succeeds | `:transaction-write-failed` with `:rolled-back true` |
| A write and its rollback both fail | `:transaction-recovery-required` with recovery evidence |
| Undo sees any stale result hash | `:result-hash-mismatch` |
| Unknown receipt schema or corrupt inverse | `:invalid-transaction-receipt` |

## Transaction and atomicity contract

The pure core takes `{path source}` values and returns either a complete set of
future files or one refusal. It performs no I/O.

The imperative shell:

1. canonicalizes and reads every file once;
2. builds all future sources from those snapshots;
3. parses every complete future source;
4. rechecks every source hash immediately before commit;
5. stages every future file and the receipt;
6. replaces source files;
7. reads back and verifies every result hash;
8. publishes the receipt only after all source verification succeeds.

If an ordinary write or verification exception occurs, the shell restores all
already replaced files from their original snapshots and verifies the original
hashes. Therefore the operation is failure-atomic for handled process errors.

Portable filesystems do not provide one atomic rename spanning unrelated
files. A host crash or power loss during the rename sequence can expose a
partial state. The implementation must not call that crash-atomic. The staged
receipt and recovery evidence are the path toward a later durable journal if
field use proves it necessary.

## Hash-fenced inverse contract

Each forward file plan records:

- canonical path;
- original source hash;
- result source hash;
- every concrete original and replacement source slice;
- stable structural addresses;
- forward diff.

The inverse stores the edits in reverse direction and guards each file with its
forward result hash. It validates all inverse future files before any write,
uses the same failure-atomic commit protocol, and refuses the whole inverse if
one file is stale. Successful inverse read-back hashes must equal every
original source hash.

## Pure behavior matrix

| Dimension | Required cases |
|---|---|
| Scope | empty, singleton, multiple, duplicate spelling, canonical alias, unsupported extension, missing file |
| Forms | invalid zero-form, invalid multi-form, exact atom, list, vector, map, metadata, reader macro, anonymous function |
| Matching | zero, one, many in one file, many across files, whitespace variation, undeclared comment, metadata mismatch, token-spelling mismatch |
| Guards | exact count, too low, too high, absent, zero, negative, non-integer |
| Replacement | normal, contains original form, exact literal layout, parse-invalid future source, no-op |
| Preservation | leading comments, interior comments, commas, metadata, reader forms, unrelated forms, unchanged files |
| Composition substrate | one intent and two intents touching disjoint nodes; conflicting/overlapping intents refuse |
| Mutation | preview writes nothing, success updates every target, stale precommit hash writes nothing, failure after first write rolls back |
| Receipt | stable schema, canonical paths, per-file counts/hashes, aggregate diff, reverse edits, receipt published last |
| Inverse | success, one stale file, corrupt receipt, unsupported version, rollback on inverse failure |

## Field-failure and real-program evidence

Add a minimized fixture derived from the observed repeated-edit route. It must
contain multiple realistic namespaces and include:

- the same exact nested form in more than one file;
- one whitespace-varied occurrence that should match;
- a similar textual lookalike that must not match;
- an occurrence with an interior comment that must not match;
- metadata, `#()`, Hiccup, maps, and unrelated comments that must remain exact;
- a replacement whose spelling would be degraded by data printing.

The fixture's starting files must parse. The result must parse completely and
must differ only at the expected structural occurrences. A corpus test should
plan against copied real repository source without mutating the live files.

## CLI and documentation gates

- Add `:change`, `:change!`, and `:undo-change!` to global and per-op help.
- Put the one-shot guarded route before the preview/apply ceremony in the skill.
- Explain that `:change!` is the structural multi-file equivalent of a guarded
  `:argdo %s///g`, while future intents can share its transaction.
- Update README, `skill.md`, Codex/Claude copies, changelog, and vision together.
- Unknown or malformed usage must include the shortest correct recovery command
  without dumping unrelated global help.

## Completion gates

1. Failing contract tests precede implementation.
2. Pure tests cover every behavior-matrix row without filesystem I/O.
3. Boundary tests inject stale snapshots, write failure, read-back failure, and
   rollback failure.
4. CLI tests assert stdout EDN and success/refusal exit codes.
5. The exact documented commands run end to end.
6. Changed Clojure files pass Standard Clojure Style.
7. Targeted tests, the complete `make test` suite, and `make install` pass.
8. A clean-context Codex caller and a clean-context Claude caller receive only
   installed help plus a realistic task. Both must choose the one-shot guarded
   transaction when the before-state and cardinality are supplied.
9. Replay a representative repeated-edit task and compare shell calls, wall
   time, source bytes, and correction turns against the tagged
   `local-microscope-optimum` and native patching.

## Explicit non-goals for the first slice

- inferred architecture or open-ended natural-language refactoring;
- wildcard captures or template substitution;
- automatic scope expansion;
- graph-aware caller updates;
- formatting whole files;
- semantic compilation claims;
- crash-atomic replacement across unrelated filesystem paths.

These are not all permanent exclusions. Captures and graph-aware intents are
the next experiments once the transaction substrate beats the microscope and
native controls on real work.
