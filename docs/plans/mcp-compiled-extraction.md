# MCP-Compiled Extraction

**Status:** Implemented

## Outcome

`apply_clojure_changes` accepts one typed extraction request that compiles a
complete structural decision into one failure-atomic transaction:

```text
source owners + target namespace + proven caller rewrites
  -> one coherent future snapshot
  -> parse and diagnostic gates
  -> one commit
  -> read-back verification
  -> one exact undo receipt
```

The caller decides which forms move and how proven callers change. The compiler
owns source capture, dependency closure, namespace construction, hashes, write
ordering, rollback, verification, and receipts.

## Bitter-Lesson Boundary

The tool compiles explicit structural intent. It does not decide architecture,
invent a destination, or silently move dependencies or callers. It makes a
human or model decision cheap to materialize without encoding project taste.

## Public Contract

Add `extraction` as a third, mutually exclusive `apply_clojure_changes`
entrance beside `basis` and `changes`:

```json
{
  "workspace_root": "/repo",
  "extraction": {
    "file": "src/app.clj",
    "forms": ["helper", "render"],
    "to": "src/app/render.clj",
    "require_policy": "copy-all",
    "caller_changes": [],
    "ignored_caller_files": [],
    "expect": {"forms": 2, "caller_edits": 0, "files": 2}
  },
  "verify": "fast"
}
```

`caller_changes` use the same exact, owner-scoped change objects as the direct
transaction entrance. Every caller candidate reported by extraction analysis
must be changed or named in `ignored_caller_files`; omission refuses before
mutation. Caller changes may not target the extraction source or destination.

## Safety Invariants

- A refusal changes no source bytes and retains no partial edit basis.
- The destination must be absent unless a future explicit merge contract is added.
- Concurrent source drift refuses before the first write.
- Rollback restores only bytes owned by this transaction. It never overwrites
  unknown concurrent bytes.
- The destination is deleted during rollback or undo only when its bytes equal
  the transaction's exact result hash.
- Source trivia outside removed owner ranges remains byte-identical.
- The destination is formatter-stable before commit.
- Existing diagnostic debt is tolerated; newly introduced diagnostics are not.
- Undo is hash-fenced across every created or updated file.
- Transaction verification and caller completeness remain separate. Direct
  extraction reports `structural-candidates-only`; it never upgrades syntax
  evidence into `semantic-complete` authority.

## Implementation Shape

1. Validate the closed request shape and mutually exclusive entrance.
2. Confine the existing source and absent destination to the workspace.
3. Capture each existing file once and retain its SHA-256.
4. Compile extraction with the existing pure extraction compiler.
5. Compile exact caller changes against the same original snapshot.
6. Prove form, caller-edit, and distinct-file counts.
7. Parse every future Clojure file before mutation.
8. Capture the diagnostic baseline.
9. Recheck every original hash and destination absence immediately before commit.
10. Create and update the complete file set as one failure-atomic transaction.
11. Read back every file, compare hashes, and reject new diagnostics.
12. Publish the receipt only after verification is complete.

Build in dogfoodable slices: confined absent-target resolution; pure
source-to-target compiler; mixed create/update commit and receipt; exact caller
composition; typed schema and concise MCP result.

## Test Plan

Pure tests exhaustively cover request validation, count proofs, path collisions,
candidate disposition, future-file composition, rollback plans, receipt inversion,
and deterministic ordering. Filesystem tests cover stale sources, destination
races, partial write failures, rollback drift, verification failure, exact undo,
formatter stability, comments, CLJC reader conditionals, declarations, quoted Var
references, and production-shaped multi-caller extraction. Existing tests are
never weakened.

## Documentation and Release Checklist

Update MCP descriptions and schemas, README, help, both installed skills, and
the Captain's Log. Hot-reload the shared MCP server and run `make install` only
after all gates pass.

## Verification Gates

Format changed Clojure files; run focused pure and filesystem tests, MCP tests,
lint, the full suite, one real extraction/undo/reapply dogfood transaction, and
a clean-context caller exercise.

## Definition of Done

One typed MCP call can create a destination namespace, remove explicitly named
source owners, rewrite every proven caller or account for it explicitly, verify
the coherent future state, and return a hash-fenced undo receipt. Any ambiguity,
count mismatch, drift, parse failure, new diagnostic, or write failure leaves
the original project intact.
