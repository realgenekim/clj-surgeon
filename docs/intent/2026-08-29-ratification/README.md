# Guardrail Ratification Packet

Status: draft for review. Nothing in this directory is active product intent.
The existing HLD, LLDs, EARS registries, tests, and code remain authoritative
until each proposed phase is ratified in order.

## LID configuration

The repository is configured for scoped Linked-Intent Development:

- `AGENTS.md` declares LID mode `Scoped`, version `1.3.0`.
- `docs/high-level-design.md` is the product HLD.
- `docs/intent/` contains active LLD and EARS leaves.
- The scoped workflow is HLD -> LLD -> EARS -> tests -> code, with review at
  every boundary.

The repository refers to `skills/linked-intent-dev`, but that skill is absent
from the working tree and from `origin/docs/captains-logs-2026-08-29`. The
embedded LID contract in `AGENTS.md` is sufficient to review this packet. The
missing skill should be restored separately so future agents do not have to
reconstruct its additional guidance.

## Classification

| Candidate | Destination | Reason |
|---|---|---|
| Positional coordinates never grant direct mutation authority | Already active LID; no new draft | The HLD tenet **Structural identity over positional authority**, the positional-authority LLD, `MCP-OP-POS-AUTH-001..010`, executable `@spec` witnesses, and `:positional-mutation-authority-refused` already place this at the typed-refusal rung. |
| Counts never grant subject identity | Already active LID; no new draft | `MCP-OP-EDIT-006` defines aggregate counts as derived bookkeeping. `MCP-OP-POS-AUTH-010` and its duplicate-subject witnesses state that content, counts, parse, and read-back cannot recover caller intent. Direct change shapes still require files and exact owner/form scope. |
| `before`/`after` cannot be redefined to invert its existing meaning | Already active LID; no new draft | `MCP-OP-EDIT-017..019` fix the injective mapping to `from`/`to`. The property witness enumerates all 64 subsets of the six field names and refuses partial, mixed, and duplicate pairs before source access. |
| All read IDs are supplied or all are server-owned | EARS plus red witnesses | This is a closed compatibility rule over one typed inspect batch. Mixed ownership has a definite pre-snapshot refusal outcome. |
| Only a complete operation-less forms shape normalizes to `forms` | EARS plus red witnesses | The remaining `forms` and exact expectation fields prove one operation. Every other omission is ambiguous and has a definite pre-snapshot refusal outcome. |
| A wrong default must fail loudly | Drop as a standalone requirement | Calling the default “wrong” makes the opposite indefensible. The concrete operation-less negative matrix is the useful requirement and carries the behavior without a slogan. |
| Missing optional evidence is omitted rather than encoded as zero | Measurement-evidence EARS | Absence and observed zero are different receipt facts. This is a concrete projection rule for optional telemetry evidence, not a general MCP result rule. The MCP cold-verification leaf already has its own narrower omission requirements. |
| Turn-derived wall figures carry `coverage_ratio`; low-coverage aggregates refuse | Measurement-evidence EARS | This is a concrete receipt and aggregation behavior. It applies to event-clock wall, not independently measured server `elapsed_ms` or process duration. |
| Compress repetition, never compress identity | Existing HLD rationale; do not add an absolute tenet | The qualified product rule already exists as **Structural identity over positional authority** plus snapshot-fenced plan and retained-basis exceptions. The absolute slogan would contradict safe prepared artifacts whose opaque IDs consume previously proved subject identity. |
| Measure the term before optimizing it | Agent research method | It directs experiment order, not product behavior. Keep it in research doctrine and measurement skills. |
| Measure adoption before optimizing per-call cost | Agent research method | It is a triggered portfolio decision and fails the HLD tenet test. Keep it in the speed-mission or research doctrine. |
| Register predictions and kill criteria before running | Agent research method | It governs experiment conduct. Keep it in benchmark protocols and the study skill. |
| Run the zero-model screen first | Agent research method | It governs experiment sequencing, not a product request or result. |
| Report a failed prediction as failed | Agent research method | It governs evidence reporting. Keep it in research doctrine. |
| Report quota headroom rather than consumption | Agent operating method | It governs seat-meter communication. Keep it in the seat/quota skill or house doctrine. |
| Verify before relaying; treat a single-source number as a hypothesis | Agent research method | It governs claim confidence and independent reproduction. Keep it in the study skill and house doctrine. |
| A frozen basis proves received old bytes, not intended new meaning | Existing HLD boundary | This is the existing **Bookkeeping over judgment** boundary. A generic typed refusal is impossible without independent effect authority. |
| Add a native pre-landing reader gate | Do not promote | The independent audit found zero catchable post-state reader failures among eight genuine loops and did not clear false-refusal evidence. This remains a stopped option, not product intent. |

## HLD disposition

No new top-level tenet is proposed. The defensible content is already present
in these HLD choices:

- **Bookkeeping over judgment** limits what a snapshot, parser, read-back hash,
  or verifier can prove about intended meaning.
- **Structural identity over positional authority** prevents a direct write
  from acquiring its subject through position, count, similarity, or another
  compressed proxy.
- **Complete verified task time over tool adoption** names the product metric;
  experiment ordering remains method rather than architecture.

If the read-normalization leaf is ratified, add this paragraph under **Compress
a coherent read mission without guessing** rather than creating another
tenet:

> The typed inspect entrance may remove call-local bookkeeping only through a
> closed request-shape proof. A batch either preserves every caller-supplied
> request ID or assigns every ID in input order. An omitted operation denotes
> `forms` only when the remaining complete forms shape proves that operation.
> Mixed ID ownership and every other operation omission refuse before snapshot
> capture. Normalization changes neither file, owner, form, basis, snapshot, nor
> result authority.

## Draft artifacts and phase stops

- Ratified read-request normalization artifacts now live in [the permanent LID leaf](../read-request-normalization/).
- `measurement-evidence-design.md` is a separate telemetry-product LLD.
- `measurement-evidence-specs.md` is its proposed EARS registry with stable
  IDs `MEASURE-EVID-001` and `MEASURE-WALL-001..003`.
- `draft-tests/*` contains executable red witness drafts. They are outside the
  active `test/` tree because HLD, LLD, and EARS approval must precede the test
  phase.

Ratification should proceed one leaf at a time. Read-request normalization can
advance independently of measurement evidence. Neither leaf authorizes code,
installation, MCP reload, or a benchmark cohort.

## Evidence basis

- `origin/docs/captains-logs-2026-08-29`
- `experiment/read-golf-buildability` at `96baa2a`
- `experiment/body-fidelity-audit` at `2290192`
- `experiment/native-prelanding-gate-audit` at `a02368a`
- `experiment/adoption-census-independent` at `79b4cfe`
- `experiment/emission-compression-screen` at `dacf768`
- `screen/ceremony-attribution` at `7682abf`
- `experiment/next-call-copy-and-consumption-meter` at `a6b4392`
- `bench/prefill-decode-ratio` at `93d9918`

## Ratification record

2026-08-30: Gene ratified BOTH leaves (verbatim answer: "Go") after the conn independently
verified the ratchet self-test (passed under its own run) and read the normalization specs
verbatim. Implementation contract: red witnesses activate first and must show exactly the
registered red count before any implementation lands; install remains gated on green plus a
live-route token measurement.
