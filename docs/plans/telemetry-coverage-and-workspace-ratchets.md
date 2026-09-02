# Telemetry coverage and workspace ratchets

**Status:** implementation delegated

## Outcome

Extend the privacy-safe `study-agent-usage` receipt so aggregate post-decision
wall cannot silently include low-coverage turns, and so structural reads can be
joined by workspace and returned snapshot identity without retaining paths,
source, raw hashes, arguments, results, or request IDs.

## Observable contract

- Each Codex turn exposes `coverage_ratio` beside its turn wall when the clock
  has a nonzero duration. Missing coverage is omitted.
- Each turn with a completed structural write exposes post-decision turn-end
  wall and the overlap-safe union of completed post-decision clock items. Both
  carry that turn's `coverage_ratio`.
- The Codex provider reports post-decision wall on three named bases: full
  turn-end wall, coverage-admitted turn-end wall, and completed-item union.
- Full turn-end aggregation refuses when any contributing turn has missing or
  sub-threshold coverage. Refusal has no aggregate `wall_ms` and reports the
  excluded hashed turn keys, per-turn wall, coverage, threshold, and reason.
- The coverage-admitted aggregate reports only admitted turns and explicitly
  reports the excluded count. It never silently drops turns.
- The default threshold is stated in the receipt and can be overridden from
  the collector CLI with a ratio in `[0, 1]`.
- Every structured MCP action with a string `workspace_root` receives a
  domain-separated SHA-256 workspace identity. Inspect actions with returned
  source-hash evidence also receive a SHA-256 identity joining that workspace
  identity to the existing re-hashed snapshot evidence.
- Missing workspace or snapshot evidence is omitted, never synthesized as
  zero, `null`, an empty hash, or a current-filesystem guess.

## Privacy invariants and non-goals

- Never emit workspace paths, transcript prose, source bodies, raw arguments
  or results, raw source hashes, or request IDs.
- Hashes are equality evidence only. The collector does not persist hash input.
- Snapshot identity covers the source-hash evidence returned for that action;
  it does not claim an unobserved whole-workspace tree hash.
- Existing receipt field meanings and consumers remain intact. The additive
  contract advances the receipt schema from v6 to v7.
- This change does not alter action classification or infer missing clocks.

## Behavior matrix

| Case | Required result |
|---|---|
| all turns meet threshold | full and admitted turn-end aggregates agree |
| one turn is sub-threshold | full aggregate refuses; exclusion is explicit |
| one turn lacks coverage | full aggregate refuses; missing field stays omitted |
| completed items overlap | union wall counts the overlap once |
| zero measured wall is observed | report the known zero, not missing evidence |
| no structural-write turn | omit per-turn post-decision wall |
| workspace root present | emit only its domain-separated identity hash |
| workspace root absent | omit workspace identity |
| returned source hashes present | emit workspace/snapshot identity |
| returned source hashes absent | omit workspace/snapshot identity |
| same workspace and snapshot | identities compare equal across actions |
| different workspace or snapshot | joined identities differ |

## Verification

1. Extend the hermetic self-test with the production-shaped low-coverage
   refusal, missing-evidence cases, overlap-safe union, and privacy canaries.
2. Run `make study-agent-usage-self-test` before and after implementation.
3. Replay `2026-08-22T00:00:00Z` through
   `2026-08-30T02:09:33.141926Z` with the 1% threshold. Confirm the 0.0046
   turn is explicitly excluded, admitted turn-end wall is 9.74 h, and measured
   union wall is 5.68 h.
4. Confirm no workspace path or original source hash appears in the receipt.
5. Stage only this plan, the collector, and the study skill; commit without
   pushing.
