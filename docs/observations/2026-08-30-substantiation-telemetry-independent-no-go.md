# Substantiation telemetry independent NO-GO

Date: 2026-08-30

## Verdict

Exact candidate
`17849c7172ad8079ceae768c0f15df795ac04e84` is **NO-GO** for the
pre-install overhead measurement and publication gate.

The report validator accepts rehashed ledger events whose allowed fields carry
caller-controlled path or prose values. This contradicts
MCP-OP-SUBST-002, MCP-OP-SUBST-006, and MCP-OP-SUBST-014. A valid hash chain
therefore does not prove that the ledger uses the ratified closed vocabulary.

No product file, installed artifact, MCP process, or shared runtime was changed.

## Bound identity

- candidate commit:
  `17849c7172ad8079ceae768c0f15df795ac04e84`
- candidate tree:
  `effaf127056fec4b6dc660961859baee3682b409`
- frozen red commit:
  `bb4506ca4805ecbfe556a4dfe1e4905ee0c27e74`
- ratified packet:
  `4831b8a7a06839b4b3141a33e9ea072be720efb4`
- independent probe SHA-256:
  `86abd7101aa7e22387a02e688b492272e6e34020fb1e7ff4d9fd1e595eabcf53`

The clean detached candidate worktree resolved to the exact commit and tree.
The three advertised primary artifact hashes matched:

- `src/clj_surgeon/mcp_substantiation.clj`:
  `462b0879606f17904f263f8916161eaa0250ea0cfe64617c09b9d1195e500150`
- `bench/substantiation_report.clj`:
  `dddb36eb46f77e0547adf916adc107adcb25d6b452408da4e40a613510781653`
- `bench/substantiation_report_io.clj`:
  `e863c08415a267e21124be0fbfa22b92cec03bef2cda1ef5f406b49e5a5f1606`

## Frozen-red replay

The frozen red does not contain the later `substantiation-self-test` Make
target or the later report-test namespace. The executable red boundary is the
sole frozen namespace, `clj-surgeon.mcp-substantiation-test`.

Independent replay with a 512 MiB heap produced exactly:

```text
Ran 14 tests containing 70 assertions.
70 failures, 0 errors.
{:test 14, :pass 0, :fail 70, :error 0}
```

The complete red output SHA-256 was
`f9a9d97089380a7e63453a427bb1c04aa093de0b293152f6de22f6ebb494ba77`.

## Green self-test

At the candidate, `make substantiation-self-test` was green:

```text
Ran 24 tests containing 121 assertions.
0 failures, 0 errors.
```

The host load reported immediately before this focused gate was
`13.39 8.32 6.38`. The falsifier below is pure and does not depend on timing.

## Independent falsifier

The probe first recorded one ordinary inspect start/finish pair through
`begin-call!` and `complete-call!`. For each case, it changed one value in the
parsed event, rebuilt every sequence/prior/event digest through the public
canonical event function, and submitted the complete rehashed pair to
`substantiation-report/validate-chain`.

Every case below was accepted:

| Case | Rehashed value | Expected |
|---|---|---|
| top-level operation | `"src/private.clj"` | refuse unknown operation |
| request field presence | adds `"src/private.clj"` | refuse non-allowlisted field name |
| request count | `"src/private.clj"` | refuse non-integer count |
| result field presence | adds `"src/private.clj"` | refuse non-allowlisted field name |
| result semantic kind | `["src/private.clj"]` | refuse unknown semantic kind |
| result error type | `"src/private.clj"` | refuse unknown refusal type |
| result `ok` | `"src/private.clj"` | refuse non-boolean fact |

Observed result:

```clojure
[{:id :unknown-operation, :verdict :accepted}
 {:id :unknown-request-field-presence, :verdict :accepted}
 {:id :request-count-source-string, :verdict :accepted}
 {:id :unknown-result-field-presence, :verdict :accepted}
 {:id :unknown-result-semantic-kind, :verdict :accepted}
 {:id :unknown-result-error-type, :verdict :accepted}
 {:id :result-boolean-source-string, :verdict :accepted}]
```

This is not digest tampering. The events have internally valid sequence and
hash-chain evidence. The defect is that report validation closes map keys but
does not close or type the values inside several allowed fields.

## Required repair boundary

The smallest honest repair is report-side validation of every nested value
against the same closed vocabulary and scalar type laws used by the producer:

- operation, field-presence, semantic-kind, and refusal enums;
- booleans, nonnegative counts, bounded timings, and result cardinalities;
- feature IDs/stages/dimensions under the ratified future-feature rule;
- vectors and optional values with exact element types.

Permanent witnesses must rehash each malformed event before validation. A
test that only changes bytes without rebuilding the chain proves digest
tamper detection, not closed-vocabulary validation.

The frozen overhead measurement must remain stopped until this matrix refuses
and the affected/full gates are green on a successor hash.
