# Measurement Evidence Integrity

Status: draft LLD for ratification.

## Context

The agent-usage collector projects privacy-safe evidence from heterogeneous
client and service records. Some fields are directly observed, some are
derived from observed intervals, and some are unavailable in an older schema,
another telemetry mode, or a record that never crossed the measured boundary.

An absent observation and an observed numeric zero are different facts. A turn
duration and the subset covered by completed item clocks are also different
facts. Receipt projections and aggregate wall reports must preserve those
distinctions.

## Scope

This design governs agent-usage receipt fields and analyses derived from the
receipt event clock. It does not govern:

- MCP `elapsed_ms`, which has its own server-owned timing contract;
- verifier job clocks, which have their own operation-contract requirements;
- subprocess durations measured directly at their process boundary; or
- benchmark promotion gates owned by a separate frozen protocol.

## Optional evidence

An optional evidence field is emitted only when its observation and derivation
requirements are satisfied. If the source record, join key, timestamp, or
schema field is unavailable, the projection omits the field. It does not emit
`0`, `0.0`, an empty digest, or another sentinel that is also a valid observed
value.

Observed zero remains zero. For example, a known completed boundary with no
overlapping background interval may report zero milliseconds. A boundary that
was not observed omits the boundary evidence instead.

## Turn clock coverage

Every nonzero-duration turn event clock reports:

```text
measured_coverage_ms = union duration of completed item intervals
unattributed_wall_ms = turn_duration_ms - measured_coverage_ms
coverage_ratio       = measured_coverage_ms / turn_duration_ms
```

The ratio is bounded from zero through one. Overlapping items are unioned, not
summed. A zero-duration turn has no defined ratio and omits `coverage_ratio`.

Any presentation of turn-duration-derived wall must carry the corresponding
per-turn ratio or an exact summary of the included ratios. A renderer must not
turn a missing ratio into `0%`.

## Aggregate admission

An aggregate over turn-duration-derived wall declares a
`minimum_coverage_ratio`. Before computing the wall figure, the aggregator
checks every included turn that contributes wall:

- if every turn has a defined ratio at or above the threshold, compute the
  aggregate and report the threshold, included turn count, minimum observed
  ratio, and aggregate basis;
- if a contributing turn has no ratio or falls below the threshold, return the
  typed refusal `insufficient-clock-coverage` and no aggregate wall figure.

The aggregator does not silently include a low-coverage turn, silently remove
it, replace missing coverage with zero, or relabel unattributed gaps as model,
tool, ceremony, or verification work. A measured-item-union aggregate is a
different basis and must be labeled as such; it does not authorize a claim
about complete turn wall.

## Refusal contract

```json
{
  "ok": false,
  "reason": "insufficient-clock-coverage",
  "minimum_coverage_ratio": 0.5,
  "included_turn_count": 279,
  "failing_turn_count": 125,
  "minimum_observed_coverage_ratio": 0.0046
}
```

The refusal may include privacy-safe turn keys and bounded distribution
evidence. It does not include a headline wall total.

## Behavior matrix

| Evidence state | Projection or aggregate outcome |
|---|---|
| Optional source field present with observed zero | Emit zero. |
| Optional source field unavailable | Omit the field. |
| Nonzero turn duration with no completed item interval | Emit `coverage_ratio=0.0` because zero coverage is observed. |
| Zero turn duration | Omit `coverage_ratio`; no denominator exists. |
| Overlapping completed item intervals | Union intervals before computing coverage. |
| Every included turn meets the declared threshold | Emit aggregate wall with coverage evidence and basis. |
| One included turn falls below the threshold | Typed refusal; no aggregate wall figure. |
| One included turn lacks coverage evidence | Typed refusal; no aggregate wall figure. |

## Alternatives rejected

- **Encode unavailable evidence as zero.** Downstream folds cannot distinguish
  absence from an observed zero.
- **Publish wall and add a caveat afterward.** A headline can escape its
  coverage qualification. Admission must precede computation.
- **Drop low-coverage rows silently.** This changes the population and can
  remove the dominant row without making the selection decision visible.
- **Use one repository-wide threshold.** Different analyses may need different
  coverage, but each must declare its threshold before aggregation and retain
  it in the result.

## Verification

Permanent tests must distinguish missing from observed zero, cover zero and
nonzero turn duration, prove interval union under overlap, and inject a
dominant low-coverage turn whose wall would control the aggregate if admitted.
The aggregate witness must prove that refusal occurs before a headline wall
field is constructed. A property test should generate interval sets and prove
`0 <= measured_coverage_ms <= duration_ms`, `unattributed_wall_ms` is the exact
remainder, and `coverage_ratio` agrees with the same union.
