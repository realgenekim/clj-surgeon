# Substantiation telemetry successor independent NO-GO

Date: 2026-08-30

## Verdict

Exact successor
`143d6fbbad22d366bf945b593e074a2ebb57d0bc` is **NO-GO** for the
overhead measurement and publication gate.

The successor correctly closes all seven malformed nested-value cases from
the first independent receipt. The wider independent matrix found three new
deterministic validity defects and one unresolved privacy boundary:

1. a valid future `elaborator.*` feature is rejected;
2. a reused event ID is accepted;
3. start and finish records under one call ID may disagree on session, key
   domain, and public tool and still validate; and
4. transport client name and version accept arbitrary path or prose values.

No product file, installed artifact, MCP process, shared runtime, measurement,
or baseline was changed.

## Bound identity

- successor commit:
  `143d6fbbad22d366bf945b593e074a2ebb57d0bc`
- successor tree:
  `ffdb3d48e3483dd026bfe19e27ee9a9919a712b5`
- predecessor:
  `17849c7172ad8079ceae768c0f15df795ac04e84`
- first independent receipt:
  `316a564d3cdb57d405ac89b090723e2d0e7861c5`
- exact prior probe SHA-256:
  `86abd7101aa7e22387a02e688b492272e6e34020fb1e7ff4d9fd1e595eabcf53`
- new independent probe SHA-256:
  `65a06fac8d031a8c9424c9e63527ac0f56a8835f62dd7780e318416ce0f50bc3`

The clean detached successor worktree resolved to the exact commit and tree.
All advertised artifact hashes matched:

- producer:
  `2d69d0742a5a167d814dfb9d546c8104ab5f5566e53034c0a25332e8239748b6`
- report:
  `7095ba19aa00ac94649bc9b77f815ca868220dde98e20813c181248b777dae1f`
- report test:
  `2d81297cb6840c3f5d217cf4d6162617ed8ae2e32a2b8a1fae21f40de6dcca36`

The successor changes only the producer, report validator, and report test.
The ratified 19-spec intent leaf is unchanged.

## Prior seven-case repair

The exact original probe was rerun without modification. All seven cases now
refuse with `:error-type :invalid-substantiation-ledger`:

| Case | Result |
|---|---|
| unknown top-level operation | refused |
| unknown request field presence | refused |
| path string in request count | refused |
| unknown result field presence | refused |
| unknown result semantic kind | refused |
| unknown result error type | refused |
| path string in result boolean | refused |

The probe exited zero. This is a real repair of the predecessor defect.

## New independent matrix

The new probe recorded one ordinary inspect start/finish pair. Each case
changed only the named value, then independently rebuilt the complete
sequence/prior/event hash chain before calling `validate-chain`.

| Case | Required result | Actual result | Verdict |
|---|---|---|---|
| unknown `elaborator.wall-fill` feature | accept as evidence, exclude until registered | refused | contradiction |
| duplicate valid event ID | refuse | accepted | contradiction |
| finish session-token drift | refuse | accepted | contradiction |
| finish key-domain drift | refuse | accepted | contradiction |
| finish public-tool drift | refuse | accepted | contradiction |
| finish operation drift | bind or explicitly permit by contract | accepted | unresolved |
| client name contains a path | refuse or deidentify under privacy law | accepted | unresolved |
| client version contains prose | refuse or deidentify under privacy law | accepted | unresolved |

The complete observed vector was:

```clojure
[{:id :future-elaborator-feature,
  :expect :accepted, :actual :refused, :pass false}
 {:id :duplicate-event-id,
  :expect :refused, :actual :accepted, :pass false}
 {:id :finish-session-drift,
  :expect :refused, :actual :accepted, :pass false}
 {:id :finish-key-domain-drift,
  :expect :refused, :actual :accepted, :pass false}
 {:id :finish-tool-drift,
  :expect :refused, :actual :accepted, :pass false}
 {:id :finish-operation-drift,
  :expect :refused, :actual :accepted, :pass false}
 {:id :client-name-path,
  :expect :refused, :actual :accepted, :pass false}
 {:id :client-version-prose,
  :expect :refused, :actual :accepted, :pass false}]
```

### Future-feature contradiction

MCP-OP-SUBST-012 explicitly requires unknown feature IDs, including future
`elaborator.*` IDs, to remain valid ledger evidence and remain excluded from
claims until registered. The successor feature-ID regex allows lowercase
letters, digits, and hyphens only. It rejects the required dot namespace
before registry admission can apply.

### Reused-ID contradiction

MCP-OP-SUBST-001 makes a reused ID invalid report input. The validator checks
UUID syntax but not event-ID uniqueness. A duplicate event ID therefore
survives a valid chain rebuild.

### Call-identity contradiction

A finish record with the same call ID can carry a different session token,
key ID, or public tool. Each individual field remains syntactically valid, so
the chain passes. Report joins and feature consumption depend on these fields;
the start/finish pair must therefore bind the same call identity rather than
validate each half independently.

### Transport privacy decision

MCP-OP-SUBST-005 requires client name and version to come from the exchange.
MCP-OP-SUBST-002 simultaneously prohibits path and prose in a record. MCP
initialize metadata remains caller-controlled even though it arrives through
the exchange. The current validator accepts arbitrary strings in both fields.
The successor needs either a closed/deidentified representation or an explicit
ratified narrowing of the privacy law. This audit does not silently choose.

## Focused gate

At host load `4.63 5.29 5.52`, the successor's focused gate was green:

```text
Ran 25 tests containing 131 assertions.
0 failures, 0 errors.
```

Broad and overhead gates were intentionally stopped after deterministic
contract failure. Running them cannot repair or qualify invalid evidence.

## Required successor boundary

Before another independent audit:

- allow the ratified future feature namespace while preserving bounded,
  source-free feature IDs;
- reject duplicate event IDs;
- bind start and finish transport identity, key domain, and public tool under
  one call ID;
- resolve the raw client-name/version privacy contradiction in intent before
  code if deidentification changes the ratified report strata; and
- add permanent rehashed witnesses for each required refusal and each required
  future-feature acceptance.

Measurement, install, and shared-runtime action must remain stopped until a
new immutable hash passes both independent probes and the wider matrix.
