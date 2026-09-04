# Astra fanout comparison — fresh wall-primary cohort

Status: FROZEN before measured migration arms at 2026-09-04T22:26:34.515866+00:00. Both model
smokes and nine adapter tests passed. Live alias migration and independent
acceptance passed. Pwd-only model smokes and hand-drives are excluded.

## Hypothesis and task

The caller model can change both native strategy and Surgeon refusal/recovery.
Compare each model's native and live alias_migration routes on the same fixture.
The target is a repeatable complete-task advantage; no 10× result is assumed.

Fixture: existing fanout-k1/repo-21, base
65fe39a9071083f478ed091ab64ebdf05c02abbd, 21 affected owners, 63 sites, 100 namespaces.
External canonical and manifest are copied from the same frozen fixture family.
This is a known synthetic task family, not a held-out real repository.

The common prompt corrects the historical contradiction about protected old-name
literals and makes identical acceptance obligations apply to both routes.
Native has ordinary tools, including scripts; tool has the same plus a required
live MCP service and a mandate to try alias_migration for the write. Native fallback
is allowed on tool failure and counted. Optional adoption is a separate later cell.

## Frozen factors

- Models: gpt-5.6-sol, gpt-6-astra; reasoning high.
- Common executable: /home/forge/.local/bin/codex, 0.153.3; vendor binary hash recorded.
- Ignore user config/rules; reject project .codex config. Session model validated.
- CPU affinity 12,13 for both model drivers and arm servers; 512 MB JVM maximum.
- Each arm gets fresh fixture and artifacts; no reuse of edited trees.
- Tool server source: 5b531d3b709b64fcae0dfcc9398942fe795da145.
  New experiment Python/docs do not alter Clojure server bytes.
- Each tool arm has its own server project binding; startup and health/identity
  attestation precede model launch. Warm agent-task wall and cold-inclusive setup
  wall are separate reported quantities, never substituted silently.

## Order and stopping rules

Calibration: six native migrations per model, alternating Sol/Astra, swap which
model leads each successive pair. Do not select a favorable run as the baseline.
Then six matched native/tool pairs per model, interleaved:
odd replicate: Sol N,T then Astra T,N; even: Astra N,T then Sol T,N.
The later native runs protect against drift after the initial calibration.

This is a maximum budget, not an obligation to waste samples on a broken instrument.
Before calibration, both model smokes, adapter tests, canonical positive acceptance,
unchanged-tree negative acceptance, and one live tool hand-drive must pass.
If the tool hand-drive fails, stop measured arms and diagnose the product first.
If an arm fails correctness, record the failure; do not quietly replace it with
a successful rerun. Any instrument-invalid run gets a fresh suffixed identity.
Two repeated identical tool refusals trigger a contract investigation before
further tool arms. A 900-second arm limit is a failed/timed-out task, not a fast
missing observation. The four-hour program may end with an incomplete cohort;
report sample size and exploratory status, never a complete result by implication.

## Resources and measurement

This is not the old exclusive-box E-SCALE-WALL experiment. Use the agreed shared
slot and owner window; launch below 1-minute load 10, one model arm at a time.
No new peer JVM suites during the current owner's <=20-minute window; already
running work drains. Log start/end load, slot wait, arm wall and server startup.
If load exceeds 10 during an arm, preserve it as a contaminated observation,
exclude it from a clean-wall claim, and report the counts. Do not condition
correctness reporting on load or success. Native/tool order stays frozen.

Primary: watcher request-to-completion wall for externally accepted tasks, plus
task failure/timeout frequency. Independently measured acceptance wall is reported
separately and also added for a verified-completion total. Secondary: calls,
returns, payload characters, refusal reasons/recovery, scripts versus literal
patches, output volume, and post-write rereads. Report model reasoning only where
the client provides completed-item clocks; gaps remain unattributed otherwise.

Report within-model native/tool medians and paired ratios with all observations,
not Astra tool divided by Sol native. A speed claim requires the improvement to
clear two standard deviations of the model's native calibration floor, survive
contemporaneous native controls, and preserve correctness. Small-sample intervals
and shared-load limitations travel with the result. A ratio below 2× cannot be
called the requested large wall win; a substep 10× is explicitly a substep result.

## Acceptance and retained evidence

Use pinned bench/fanout/rescore-FAN.sh and its structural checker with immutable
fixture manifest/canonical. Require all six checks: exact changed files, form-tree
equality modulo whitespace with protected syntax, protected literal hashes,
100 namespace loads, 21 behavioral tests/147 assertions, no wrong references or
alias collisions. Independent guard checks require test and runner bytes/modes
unchanged. Canonical byte identity is an additional diagnostic.

Preserve prompt and binary hashes, requested/observed model, session-bound rollout,
watcher stream, stdout/stderr, server ready evidence, tool telemetry, external
acceptance output, staged diff including new files, and terminal outcome. No agent
self-count or summary is a counting/correctness authority.

## Frozen input hashes

```json
{
  "/var/tmp/forge/astra-program/prompts/fanout-common.txt": "440c64e95e9380cdc016f451ec9349c412005a0f9c2dedd9ff8abe561134ecbf",
  "/var/tmp/forge/astra-program/prompts/fanout-native.txt": "47891a0fe5201d4c8b86a1eeee39892dc1bbd3a41ddb66124cc5461b0853ba70",
  "/var/tmp/forge/astra-program/prompts/fanout-tool.txt": "8c6d8dc8cad3bf4d4dd49d9c63ff217e690f97f1b5b3a799436e715185dbf24c",
  "/var/tmp/forge/astra-program/repo/bench/astra/adapter.py": "8f6c909ffe25836a3599a2eec45f5da5a35d3fdc94541356c4778b440372b449",
  "/var/tmp/forge/astra-program/repo/bench/astra/test_adapter.py": "3e1a817aa08b149790179a32555886f9152ffcb7f6bd7f71032144d353ff8256",
  "/var/tmp/forge/astra-program/repo/bench/anvil-arms/watch.py": "aebfffef7ee069814aa7e92436910bbd139e14eb11638e96be81603a04e4b7b4",
  "/var/tmp/forge/astra-program/repo/bench/anvil-arms/score.py": "f0f198f44d1dbf9af37f14f1111435751c9d5e2d292cebe6d28bfa001f5a3f8e",
  "/var/tmp/forge/astra-program/repo/bench/fanout/rescore-FAN.sh": "97486d75ff5f051b831c7997452d31ca15560fc85476ae1d57e12c69b9560eaa",
  "/var/tmp/forge/astra-program/repo/bench/fanout/fan_check.clj": "003dcda183b89b44f7af3241f71e21e13b637ec64aec67cb6155bc3673b312ff"
}
```
