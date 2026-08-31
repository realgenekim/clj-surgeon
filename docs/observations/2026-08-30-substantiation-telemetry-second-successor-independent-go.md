# Substantiation telemetry second successor independent GO

Date: 2026-08-30

## Verdict

Exact successor
`de70e06fdc18f832b6774eabf81453ad4af9781f` is **GO** for the frozen
pre-install overhead measurement in MCP-OP-SUBST-018.

This is not install, reload, publication, or shared-runtime authority. The
measurement remains owned by SURGEON1. The SUBST-CLIENT-1 Option A client
metadata clarification remains subject to Gene's stated override at review.

The successor closes the original seven malformed-ledger false greens and the
four ratified defects found in the first successor: future dotted feature IDs,
duplicate event IDs, start/finish call-identity drift, and raw client metadata.
An independent 12-case matrix also found no cross-session prepared or
continuation joins, raw metadata carriage, report-claim leakage, or
start/finish failure-law defect.

No product file, installed artifact, MCP process, shared runtime, measurement,
or baseline was changed by this audit.

## Bound identity

- candidate commit:
  `de70e06fdc18f832b6774eabf81453ad4af9781f`
- candidate tree:
  `9ca1e9a0c3c8e6a71e79d72f364210b3cddbb6d1`
- predecessor:
  `143d6fbbad22d366bf945b593e074a2ebb57d0bc`
- first independent NO-GO receipt:
  `316a564d3cdb57d405ac89b090723e2d0e7861c5`
- first-successor NO-GO receipt:
  `01529dbf`
- original seven-case probe SHA-256:
  `86abd7101aa7e22387a02e688b492272e6e34020fb1e7ff4d9fd1e595eabcf53`
- successor adversarial probe SHA-256:
  `65a06fac8d031a8c9424c9e63527ac0f56a8835f62dd7780e318416ce0f50bc3`
- independent expanded-matrix SHA-256:
  `e343c1384628e4e6ec481bd0299d48830672e5bb304bb02565daa26205b13cff`

The audit used a clean detached worktree at the exact candidate. The resolved
tree and all advertised artifact hashes matched:

| Artifact | SHA-256 |
|---|---|
| producer | `785a279f29a050d732b9723dfabadde039a8c0464d12b7266c0cb2d5d784721e` |
| report | `80d8ef3752df4fe4f3cc76ea3f1fd1d91850152dd3d84dd8c1000933e8b32835` |
| product test | `6096d5d499f97c3856468398d7956905d7eaa8a26cbcf20d580848ea68a4a8f4` |
| report test | `1b213f4eff5a2d1fea5d57805261c01a1e0936bb6ddc3475ab18da1ee78cd0b4` |
| design | `7a7653684d59d1a251caaadddeddbe9bb1bf7ba97f0b54118aebc6b070f17dda` |
| specifications | `ca69fbdfff96edcc1d7be88472b018a3eeabbf20902302cf82d5e0e8eb6df011` |

## Exact predecessor probes

The original seven-case probe was rerun without modification. All seven cases
refused with `:error-type :invalid-substantiation-ledger`:

| Case | Result |
|---|---|
| unknown top-level operation | refused |
| unknown request field presence | refused |
| path string in request count | refused |
| unknown result field presence | refused |
| unknown result semantic kind | refused |
| unknown result error type | refused |
| path string in result boolean | refused |

The successor adversarial probe was also rerun without modification. Seven of
its eight provisional expectations are binding under the current ratified
intent and all seven passed:

| Case | Required result | Actual result |
|---|---|---|
| `elaborator.wall-fill` evidence | accept | accepted |
| duplicate event ID | refuse | refused |
| finish session-token drift | refuse | refused |
| finish key-ID drift | refuse | refused |
| finish public-tool drift | refuse | refused |
| raw client name/path | refuse | refused |
| raw client version/prose | refuse | refused |

The frozen script's eighth provisional expectation required a normalized
finish operation to equal the request operation. Current SUBST-006 requires
both actual operation projections but does not require equality; public
normalization may legitimately change the operation name. Acceptance of that
case is therefore not silently counted as a pass and is not a candidate
contradiction.

## Independent expanded matrix

The new probe created fresh telemetry states, rebuilt each canonical event
chain, and exercised producer and report behavior. It passed 12 of 12 cases:

| Case | Result |
|---|---|
| client name/version are 64-hex tokens | pass |
| identical metadata is deterministic in one session | pass |
| identical metadata differs across session keys | pass |
| raw client metadata is absent from ledger bytes | pass |
| prepared descriptor is not consumed across sessions | pass |
| prepared descriptor is consumed in the same session | pass |
| continuation token is not consumed across sessions | pass |
| continuation token is consumed in the same session | pass |
| unregistered feature is excluded from claims | pass |
| registered `elaborator.wall-fill` evidence is counted | pass |
| append/start/finish failure law remains fail-open with unhealthy state | pass |
| ordinary 12-event ledger validates | pass |

This independently confirms the two cross-call joins remain session-fenced
after client rehashing and that caller-controlled client strings cannot reach
the ledger or report.

## Gates

### Focused telemetry gate

```text
Ran 26 tests containing 140 assertions.
0 failures, 0 errors.
```

### Full repository gate

```text
core:     647 tests / 5562 assertions / 0 failures / 0 errors
analyzer:   4 tests /   20 assertions / 0 failures / 0 errors
MCP:      345 tests / 3796 assertions / 2 failures / 0 errors
```

The only MCP failures were the two previously characterized assertions in
`cold-clj-kondo-admission-timeout-is-unverified`: the raced wall-clock path
returned `:clj-kondo-admission-unverified` and `:delegated` instead of
`:clj-kondo-admission-timeout` and `:admission-timeout`. The exact cold
namespace, run alone without the MCP test runner's full-suite main options,
was green:

```text
Ran 7 tests containing 50 assertions.
0 failures, 0 errors.
```

No substantiation test failed in the full run. The full-suite process result
is still recorded as nonzero; this receipt does not relabel it green.

### Ancillary evidence

- repository intent audit: `ok=true`, `violations=[]`;
- targeted clj-kondo on producer and report namespaces: 0 errors, 0 warnings;
- `git diff --check`: clean;
- candidate worktree: clean after moving one generated test workspace out of
  the candidate tree;
- all four existing public observation hooks remained present and their
  affected tests passed in the full MCP run.

## Measurement boundary

The evidence authorizes only the already-ratified, zero-model live HTTP
differential in MCP-OP-SUBST-018. That measurement must still enforce its own
public-result parity and p50/p95 limits. A semantic or overhead miss remains
release NO-GO. Installation, reload, publication, and shared-runtime changes
require a later explicit gate.
