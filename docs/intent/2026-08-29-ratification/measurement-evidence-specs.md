---
parent: measurement-evidence-design
prefix: MEASURE
status: draft-for-ratification
---

# Measurement Evidence Specifications

IDs in this draft are stable and must not be reused if a requirement is
deleted. These requirements are not active until the parent design and this
registry are ratified.

## Optional evidence

- [ ] **MEASURE-EVID-001**: When an agent-usage receipt projection has the required source evidence for an optional numeric, digest, identity, or boundary field, it shall emit the observed or derived value, including a genuine zero; if the required evidence is unavailable, it shall omit the field rather than emit zero, null, an empty digest, or another value that can be mistaken for an observation.

## Event-clock wall

- [ ] **MEASURE-WALL-001**: When the collector emits an event clock for a nonzero-duration turn, it shall compute `measured_coverage_ms` from the union of completed item intervals clipped to that turn, emit `unattributed_wall_ms` as the exact non-negative remainder, and emit `coverage_ratio` as their bounded ratio; when the turn duration is zero, it shall omit `coverage_ratio` because the denominator is unavailable.
- [ ] **MEASURE-WALL-002**: When a receipt renderer or downstream projection publishes turn-duration-derived wall, it shall carry the corresponding per-turn `coverage_ratio` or an exact included-population coverage summary and shall never render a missing ratio as zero percent or attribute an uncovered gap to a work kind.
- [ ] **MEASURE-WALL-003**: When an analysis requests an aggregate over turn-duration-derived wall, it shall require a declared `minimum_coverage_ratio` and shall compute the aggregate only if every contributing turn has a defined ratio at or above that threshold; otherwise it shall return reason `insufficient-clock-coverage`, report bounded failure and threshold evidence, and omit every aggregate wall figure without silently including or excluding a turn.

## Falsifiers

| ID | Defensible opposite to test | Required witness families |
|---|---|---|
| `MEASURE-EVID-001` | Zero is a convenient universal sentinel for missing telemetry. | Missing source field; explicit zero; absent join key; older receipt schema; metrics-only record; empty but observed collection. |
| `MEASURE-WALL-001` | Summed item wall is close enough to interval coverage. | Disjoint intervals; complete overlap; partial overlap; clipping at both turn boundaries; no samples; zero duration. |
| `MEASURE-WALL-002` | Presentation may coerce missing coverage to `0%` if structured data remains precise. | Missing key; explicit `0.0`; `0.0046`; one turn carrying more than half the proposed wall headline. |
| `MEASURE-WALL-003` | A low-coverage outlier can be included with a footnote or silently filtered. | All rows pass; one row below; one row missing; threshold boundary equality; dominant failing row; no aggregate wall key on refusal. |
