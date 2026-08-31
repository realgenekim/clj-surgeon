# Substantiation telemetry / W1 rebase independent audit

Date: 2026-08-31  
Auditor: SURGEON2  
Verdict: **GO for the implementation candidate; no install or runtime authority**

## Immutable subject

- Candidate: `4e2cf27b2226997508356ac5ecbdeaed18d8132c`
- Tree: `4c7265c660ce6e698e0dbff45d4e697fe05994e2`
- Worktree state before the audit: clean, detached at the exact candidate
- Ratified cross-feature contract: `MCP-OP-SUBST-008`
- Frozen public-route witness SHA-256:
  `1449b2112a33bc4418c78b6f4720083da52603f8a5a8231a3d2d15efbf8a9cff`

The audited law is narrower than “a request contains `confirm`.” An official
prepared confirmation counts only when the same session emitted the
descriptor, the server completed lookup and validation, the registry consumed
the confirmation once, and the process-local result marker rejoins the exact
session-bound descriptor. Preview, raw confirmation parameters, bad fill,
unknown digest, wrong session, and replay carry no consumption authority.

## Public-route witness

The frozen real Streamable HTTP witness ran in a fresh 512 MiB JVM at the exact
candidate:

```text
Testing clj-surgeon.mcp-substantiation-w1-witness-test
Ran 1 tests containing 12 assertions.
0 failures, 0 errors.
```

It exercised one SDK session from `inspect_clojure` through official
`edit_clojure {confirm, fill}`, ordinary source commit, telemetry append, and
consumed replay. The ledger contained exactly one emitted, one consumed, and
one committed prepared-request feature. Replay added none.

## Independent false-green matrix

The following cases ran through the public substantiation observer in a
standalone analysis JVM rooted at the exact candidate. Every case first served
one session-bound descriptor. `returned-identical=true` in every row proves the
observer did not alter the domain result.

| Case | Emitted | Consumed | Committed | Refused | Required result |
|---|---:|---:|---:|---:|---|
| Raw `{confirm, fill}` plus an unmarked successful result | 1 | 0 | 0 | 0 | zero authority |
| Preview | 1 | 0 | 0 | 0 | zero authority |
| Bad fill | 1 | 0 | 0 | 0 | zero authority |
| Unknown digest | 1 | 0 | 0 | 0 | zero authority |
| Process-local marker with a wrong digest | 1 | 0 | 0 | 0 | zero authority |
| Correct consumed marker, later transaction refusal | 1 | 1 | 0 | 1 | consumed but not committed |
| Correct consumed marker, successful ordinary commit | 1 | 1 | 1 | 0 | exact positive route |

### Independent fourth falsifier: correct marker, wrong session

The strongest false-green probe served the descriptor under SDK session A,
then supplied the correct process-local marker while completing the call under
SDK session B. It returned:

```clojure
{:consumed 0
 :committed 0
 :raw-digest-present false
 :feature-count 4}
```

This closes the dangerous join error where a valid digest could otherwise
impersonate authority across sessions. The marker is Clojure metadata attached
inside the product process; no JSON request field can create it. The server
still requires its session-keyed emitted-descriptor mapping before counting.

## Privacy, denominator, and claims

An emitted-and-consumed audit case searched the complete JSONL ledger for the
raw confirmation digest, file path, owner, old content, replacement content,
and caller-hole path. All six were absent. The four-record ledger was 5,957
bytes and retained only the closed/HMACed evidence vocabulary.

The report compiler returns exactly:

```clojure
{:actionable_adoption_denominator :unavailable
 :reason :caller-session-affinity-not-observable}
```

It does not reinterpret per-call-session callers as opportunities. Counts
remain measured, historical comparison remains observed-before-after, decode
seconds remain projected, and `promotion_authority` remains false.

## Gates

- Frozen HTTP witness: 1 test / 12 assertions / 0 failures / 0 errors.
- Focused prepared-confirmation, wire, telemetry, report, and W1 set:
  49 tests / 272 assertions / 0 failures / 0 errors, clean process exit.
- `make substantiation-self-test`: 26 tests / 141 assertions / 0 failures /
  0 errors.
- Repository intent audit: `ok=true`, `violations=[]`.
- Full `make mcp-test` at load averages 5.90 / 6.12 / 5.53:
  368 tests / 3,929 assertions / exactly two failures / 0 errors. Both failures
  were the characterized wall-clock race in
  `cold-clj-kondo-admission-timeout-is-unverified`: expected
  `:clj-kondo-admission-timeout` / `:admission-timeout`, observed
  `:clj-kondo-admission-unverified` / `:delegated`. The telemetry/W1 focused
  namespaces were green.
- Scoped clj-kondo: 0 errors and one existing warning,
  `clojure.test/testing` referred but unused in
  `mcp_substantiation_test.clj`.
- Candidate checkout remained clean through all executable probes.

## Decision

GO. The candidate counts the official W1 route exactly once, refuses every
tested false authority source, separates consumed/refused from committed,
preserves privacy, and keeps the adoption denominator unavailable for callers
whose session affinity cannot be observed. The known cold-admission timing
flake is independent of this rebase and is recorded rather than hidden.

This receipt authorizes neither overhead measurement, installation, reload,
nor shared-runtime mutation. Final publication remains separately gated.
