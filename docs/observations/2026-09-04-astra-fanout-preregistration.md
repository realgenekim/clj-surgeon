# Astra fanout comparison — fresh wall-primary cohort

Status: pre-registration draft pending final adapter hash and two model smokes;
no measured migration arms have run. Pwd-only model smokes are excluded.

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
