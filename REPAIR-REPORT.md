# create_files SOL Blocker Repair Report

Date: 2026-08-31

Branch: `repair/create-files-blockers`

Base: `cbf336c6` (`lqbm-build`)

## Scope

This repair closes SOL blockers B1-B4 and heals B5 by carrying the decisions
through the scoped Linked-Intent Development chain in order:

```text
LLD -> EARS -> frozen red tests -> implementation -> full verification
```

The owning MCP operation-contract LLD now treats creation as a first-class
transaction effect. EARS requirements `MCP-OP-EDIT-033` through
`MCP-OP-EDIT-036` specify identity suppression, publication-failure rollback,
atomic create-if-absent publication, and commit-time ancestor confinement.

## Phase receipts

| Phase | Commit | Evidence |
|---|---|---|
| LLD and EARS | `03777826` | Contract and decision updates committed before tests or code |
| Frozen red | `f4592bd0` | Four permanent blocker probes committed before implementation |
| Implementation | `5b1fddfa` | Focused create-files suite green: 17 tests, 95 assertions, 0 failures, 0 errors |
| Full gate and report | this commit | `make test` exit 0 and touched-file clj-kondo clean |

All commits are local on `repair/create-files-blockers`. No push was performed.

## Frozen-red evidence

After the B2 witness was routed directly through the transaction publication
boundary, the pre-implementation MCP milestone run reported:

```text
374 tests, 3933 assertions, 25 failures, 0 errors
```

The four new blocker witnesses accounted for 22 failures:

- B1: 4 failures. Two requests with the same edit but different created paths
  and bytes exposed the same edit-only canonical identity and no suppression
  reason.
- B2: 13 failures across create-only and mixed transactions. Receipt
  publication failure returned `:transaction-recovery-required`; rollback was
  false, created files and directories remained, and the mixed edit remained
  changed.
- B3: 2 failures. A foreign file placed after the transaction's final
  existence guard was overwritten and the transaction reported success.
- B4: 3 failures. A planned absent parent replaced by a symlink was followed;
  the sibling edit remained committed and a file appeared outside the
  workspace root.

The other three milestone failures were not blocker witnesses: two were the
environmental clj-kondo admission-state assertions, and one was the intent
audit waiting for Phase 3 implementation annotations.

## Per-blocker result

### B1: canonical effect identity

Before: both creation-bearing requests published the same version-1 identity
SHA-256, `8a11b069c6d87e3ecce82dff91fb5438f41f2a13e9bacd4f5adc9a07bd718db9`,
despite different created paths and bytes.

After: any request carrying `create_files` omits
`canonical_effect_identity` and publishes
`canonical_effect_identity_suppressed_reason: "create-files-present"`.
Extending the versioned identity projection remains a follow-up requiring
ratification.

### B2: receipt-publication rollback

Before: `compile-inverse` received only edited future sources, so creations
were outside inverse write authority. Create-only and mixed publication
failures required manual recovery.

After: rollback source authority is the edited future-source map plus guarded
read-back bytes for every successfully created file. Both create-only and
mixed probes return `:receipt-write-failed`, report successful rollback, remove
created files and directories, and restore original edited bytes.

### B3: late target appearance

Before: creation reused replacement-oriented `atomic-write!`, whose final move
carried `REPLACE_EXISTING`; a late foreign target was clobbered.

After: `atomic-create!` fully writes a same-directory temporary inode and
publishes it with `Files/createLink`. The link operation is atomic
create-if-absent: it fails if the target exists and cannot replace it. Recovery
records a created file only after publication succeeds. The deterministic
barrier now refuses the transaction and preserves the exact foreign bytes.

This deliberately uses hard-link publication instead of `ATOMIC_MOVE` without
`REPLACE_EXISTING`. Java specifies implementation-dependent behavior when an
atomic move targets an existing file, and common Unix providers may replace
it. The hard-link primitive is the stronger, deterministic realization of the
required no-clobber contract.

### B4: late ancestor symlink

Before: path confinement was checked only during planning. A missing parent
could become a symlink before commit and redirect the creation outside the
workspace.

After: each resolved creation carries the canonical planning root through
validation and compilation. Immediately before atomic publication, every
existing descendant ancestor is checked with no-follow semantics for root
confinement, directory type, and symbolic links. A changed ancestor refuses;
ordinary transaction recovery restores prior sibling edits. The probe leaves
the outside workspace untouched.

## Verification

Focused blocker suite after implementation:

```text
clj-surgeon.mcp-create-files-test
17 tests, 95 assertions, 0 failures, 0 errors
```

Touched-file lint, through the serialized entrance:

```text
~/bin/clj-kondo --lint \
  src/clj_surgeon/file_ops.clj \
  src/clj_surgeon/intent_transaction.clj \
  src/clj_surgeon/mcp_tool.clj \
  test/clj_surgeon/mcp_create_files_test.clj \
  test/clj_surgeon/operation_algebra_test.clj

errors: 0, warnings: 0
```

Final full gate:

```text
make test                           exit 0
test-fast                           647 tests / 5562 assertions / 0 failures / 0 errors
analyzer-contract-test                4 tests /   20 assertions / 0 failures / 0 errors
mcp-test                            374 tests / 3933 assertions / 0 failures / 0 errors
mcp-operation oracle               pass
mcp stdio smoke                    pass
all remaining self-tests           pass
```

The first post-implementation full gate exposed the operation-algebra runtime
inventory's expected new names, `create-source!` and
`file-ops/atomic-create!`; its permanent allowlist was updated. A subsequent
full attempt passed `test-fast` but the external admission controller deferred
all analyzer work with `clj-kondo-pressure-deferred`. After the bounded
pressure window decayed, touched-file lint and one complete `make test` run
both passed.

## Tool routes

`inspect_clojure` supplied batched, snapshot-bound source for the exact owners
in `intent_transaction.clj`, `mcp_tool.clj`, `file_ops.clj`, and
`mcp_paths.clj`, plus the existing publication-failure and runtime-inventory
witnesses. Native literal patching was used for the new top-level primitive,
cross-cutting control-flow edits, prose, and new tests because those edits were
already decided and clearer as one visible patch than as prepared semantic
operations.

## Honest gaps and follow-ups

- The non-blocking steering-note fail-soft telemetry counter/log line was not
  added. It is outside B1-B5 correctness and remains a follow-up.
- The canonical identity projection is intentionally not extended for
  creations. The conservative suppression rung remains in force until a new
  projection version is ratified.
