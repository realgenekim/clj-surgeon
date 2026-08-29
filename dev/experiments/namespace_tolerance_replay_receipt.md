# Namespace-tolerance replay receipt

This pure offline experiment replayed all eight retained calls at integration
HEAD `3e4a05e77c3abb523437e9430d73b524320e2780`. Candidate calls first passed
through the existing owner-aware symbol-migration lowerer; all four preserved
23 owner rows and 27 declared symbol matches. The resulting requests then went
through the real current parameter validator and intent compiler against the
frozen submission-row before sources.

## Original two-law claim: 7/8, not 8/8

- Law A recovered six calls. Each lowered nine exact namespace names from
  `within.form` to an explicitly named `within.namespace` only after proving a
  unique direct namespace and no competing named owner.
- Law B fully recovered one call (`05-candidate`). It also safely lowered nine
  namespace clauses in `01-control`, including the lossless singleton
  `files`-to-`file` shape normalization. Each accepted fingerprint is now
  proven to be a direct namespace-clause child; the namespace and whole-file
  counts prove there is no equal nested or outside candidate.
- `01-control` still refused at `edits[9]`. That edit omitted `within` for a
  complete `detail-controls` defn, so the required non-namespace falsifier
  correctly prevented Law B from guessing an owner.
- The seven recovered calls each compiled to 51 matches in nine files and
  produced every capsule future hash exactly.

The original two-law result is therefore an exact **7/8 ceiling**. It is not
reported as an all-eight success.

## Separate optional Law C: 8/8

Law C derives `within.form` only when `from` and `to` each parse as one complete
named top-level owner with the same kind and name, the lossless whole-owner
fingerprint occurs exactly once as a direct top-level form in the file, and the
declared match count is exactly one. It also losslessly lowers a singleton
`files` selector to `file` inside that complete proof.

Law C lowered only the residual `detail-controls` edit in `01-control`. With
A+B+C, all eight calls passed the real validator and compiler, each produced
51 matches across nine files, and every future hash equaled the capsule.

All required negative cases refused. A/B covered wrong namespace, competing
owner, multiple and reader-conditional namespaces, non-namespace missing
scope, stale count, mismatched clause kind, a nested-only clause, an identical
subtree outside the namespace, empty or multiple `files`, and simultaneous
`file` plus `files`. C covered zero or many whole-owner occurrences, anonymous
owners, different kind or name, nested-only occurrence, and stale count.

## Transaction storyboard

```text
CAPTURED JSON (8 calls)
        |
        | candidate only: existing symbol_migration lowerer first
        |                  23 owner rows / 27 declared matches preserved
        v
  +---------------- ORIGINAL A+B ----------------+
  | A: exact ns name in within.form  -> namespace |
  | B: exact same-kind ns clauses    -> namespace |
  +----------------------------------------------+
        | 7 exact futures
        | 01 stops at missing scope on complete defn
        v
  current validator -> current compiler -> frozen sources
        |                                  51 matches / 9 files
        +--------------------------------> capsule hashes exact (7/8)

  +------------- OPTIONAL C (separate) ----------+
  | exact unique whole named owner, same kind/name|
  | before and after                     -> form  |
  +----------------------------------------------+
        | rescues only 01 edit[9]
        v
  current validator -> current compiler -> capsule hashes exact (8/8)
```

## Verification and scope

- Experiment tests: 4 tests, 37 assertions, 0 failures, 0 errors.
- Fast suite: 636 tests, 5,467 assertions, 0 failures, 0 errors.
- Cold MCP suite rerun: 269 tests, 2,284 assertions, 0 failures, 0 errors.
- clj-kondo: 0 errors, 0 warnings.
- Standalone nREPL replay: 8 captures, A+B 7 exact, A+B+C 8 exact, all
  falsifiers refusing.

The first MCP-suite attempt was environmentally invalid because the experiment
nREPL was intentionally still active, causing two cold-admission expectations
to delegate. After stopping that process, the cold rerun passed completely.

No model was called. No product source, public schema, installation, reload,
shared MCP, or fixture source was changed. Only the pure experiment, tests, and
this receipt are in the isolated commit.
