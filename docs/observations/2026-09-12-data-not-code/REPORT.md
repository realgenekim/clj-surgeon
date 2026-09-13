# Data-not-code: stopped at launcher authority contract

Recorded 2026-09-12T20:27:11Z from `date -u`. Subject:
`3b6d5357`, branch `fable/data-not-code-local`, initially clean.

**Verdict: STOP-CONTRACT. No implementation or green claim.** The build brief
requires stopping if an item needs a product contract change beyond its written
contract. Item 1 defines admission and refusal once an invocation envelope exists,
but the inspected tree supplies no destination envelope and the supplied consult
does not define authority for launchers that supply none. I asked for the reconciled
inb-162207 launcher contract; its text was unavailable during this inspection.

## Evidence and decision

- `src/clj_surgeon/operation_algebra.clj`, `context-keys` and
  `derive-capabilities`: trusted context has operation, version, entrance, policy,
  and lifecycle. It derives effect permissions, not allowed destination roots;
  unknown authority fields are rejected.
- `src/clj_surgeon/intent_transaction.clj`, `cli-change-context` and
  `mcp-change-context`: the declared adapter contexts contain no destination
  envelope.
- `src/clj_surgeon/receipt_artifacts.clj`, `default-artifact-root`,
  `*artifact-root*`, `writable-root!`, `directory`, and `target`: environment or
  dynamic binding selects the candidate; ownership, disk classification, and
  descendant containment validate it. There is no independent invocation
  destination authority.
- `src/clj_surgeon/mcp_alias_migration.clj`, `append-telemetry!`: the ledger calls
  `writable-root!`, then constructs its directory and creates it. Its symlink
  check follows directory creation. Admission must cover the concrete ledger
  destination before that creation, not merely the selected artifact root.
- `test/clj_surgeon/receipt_artifacts_boundary_test.clj`,
  `the-artifact-boundary-canonicalizes-both-sides`, and existing alias migration
  fixtures explicitly bind alternate artifact roots without any envelope.
  Unconditionally refusing absent authority would change existing behavior.

Making the selected destination its own permission would defeat the requested
independent admission. A nil/unrestricted default would leave ordinary launches
outside the new protection. Denying absent authority is the proposed choice, but
requires a launcher contract and rollout decision, rather than silently choosing
one in the artifact helper. These are implementation alternatives, not measured
defects in a new implementation.

Concrete contract proposed for review (not implemented or approved): trusted
launcher context supplies an envelope ID, permitted effects, and absolute allowed
roots before invocation; absent or malformed authority refuses publication;
request fields never supply a fallback. Resolve destination ancestry before
creating directories, check the concrete workspace or ledger destination, and
preserve the brief's exact `:write-outside-envelope` data. The launcher must define
how it obtains that authority independently of candidate selection and how existing
CLI, MCP, and fixture launches receive it. If inb-162207 already specifies this,
its text resolves the stop without a new design decision.

The intended coverage is **the shared artifact boundary**, not every writer.
Landlock remains in place. Admission does not eliminate filesystem races or
mediate arbitrary subprocess effects.

## Verification, dogfood, and impact

Read-only source and design inspection only. No red witnesses, source edits,
lint, focused tests, suites, probe measurements, servers, or prewarm gates ran.
The requested lease-path listing returned no entries at inspection time; no JVM
was launched on that basis. No push, tag, install, records write, or protected
port access occurred. A broad read-only search for inb-162207 was stopped by its
observed process ID; failure to locate it is not proof that the record is absent.

| Source edit | Intent | Mechanism | Refusal | Repair text sufficient |
|---|---|---|---|---|
| None | Stopped before implementation | Native read-only inspection | No Surgeon call/refusal | Not applicable |

Only this report and its EDN companion are committed, using a native patch.
The enclosing report commit identifies itself in Git; there is no implementation
commit to cite.

Sublime impact: **no demonstrated improvement from this stop**. For the proposed
completed block, my qualitative estimate is improvement in diagnostic leverage
(typed sandbox refusals), evidence fidelity (consumed measurement rows), and
operational burden (stable sleep identities). Artifact admission could improve
safety and repair clarity if launchers deliver real independent authority. No
numeric aggregate or feedback-latency gain is supported by this inspection.
The profile in `docs/observations/2026-09-08-tighten/astra-sublime-axes.md` explicitly
distinguishes estimates from measurements; the probe's wall bets remain untested.

## Least sure, disagreements, and owed work

Least sure: inb-162207 may already contain the missing launcher decision. The
consult and brief were available; that reconciliation was not.

Disagreement: none with the five bounded mechanisms. I decline to treat root
selection as authority or to silently choose absent-envelope compatibility.

Owed: resolve item 1's launcher contract, then its committed red witnesses, fix,
green publication witnesses and refusal registration; items 2–4 with their red/
green commits; item 5's six-task preregistration and both bets; item 6's lint,
focused checks, lease-gated test-fast and prewarm, and final implementation
report. The explicit ordered stop takes precedence over proceeding to later items.
