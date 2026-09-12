# Block B attempt 20 — PASS, built and prewarmed

All three accepted findings from Sol's sealed-candidate NO-GO are repaired.
`make test-fast`, the restricted whole-gate diagnostic, and the real
`make landing-gate-prewarm` each ran once and passed. No gate repair, budget
increase, deleted test, push, or landing was performed. Independent fence
review remains owed. The tested product commit is
`09588a14a784e220907462f46676900402bc80aa`; the final commit adds this evidence.

## F1: runtime assignment from variance

The frozen paired scope has 38 namespaces: all 35 original bb fast members and
the three previously moved members. Every namespace has six fresh JVM runs and
six fresh bb runs, serially on this box: 456 successful final controls.
[runtime-table.md](runtime-table.md) and [runtime-table.edn](runtime-table.edn)
contain per-runtime n, mean, sample sd (n−1), walls and the conservative ratio.
[fold.clj](fold.clj) derives the table and manifest patch from the receipts;
replaying its table-only mode produced identical before/after SHA256s.

Only `clj-surgeon.rename-alias-test` flips, bb → JVM. JVM mean 3203.000000 ms,
sd 150.905268; bb mean 6265.333333 ms, sd 74.995111; conservative ratio
2.211273561. These rounded values are copied from the generated
[reassignments.md](reassignments.md), which preserves n and both assignments.
The formula is `(mean_bb + 2*sd_bb) / max(1, mean_jvm - 2*sd_jvm) <= 2.0`.
Portability and the existing intent-transaction contract escape still apply.
Successful controls do not retire that separate recorded contract issue.

TEST-ISO-016 and the manifest witness now enforce the full paired scope,
sample counts, distinct receipts, recomputed statistics and named failures for
undersampled or over-ratio bb assignments. Deliberate boundary and variance
plants exercise refusal, including the 1 ms denominator floor.

The reassignment exposed a calibration dependency: historical ceiling replay
looked up today's runtime assignments. It now reads runtime membership from the
calibration receipt itself, validates complete unique membership, and preserves
the existing 343102 ms ceiling. The original 240989 ms bb and 42143 ms fast
calibration sums remain reproducible. A witness changes all current assignments
to JVM and confirms historical replay is unchanged. Runtime and cadence sums
are never added together.

Two preliminary harness failures are excluded and retained in `argv-repair/`
and `isolation-repair/`; [apparatus.md](apparatus.md) explains both. Final
controls use repository private-temp/home setup, explicit 1 GiB heaps and one
namespace process at a time. Startup/require are outside namespace walls;
process walls and commands remain in `measurement-processes.jsonl`.

## F2 and spec agreement

The servlet now encodes the whole verdict and measures UTF-8 bytes before
obtaining its writer. At or below 16384 bytes, the original encoding is emitted.
Above it, a complete bounded verdict retains status, counts and pending proof,
includes the first fitting prefix of at most 64 reload names, and adds
`:error-type :probe-response-truncated`, `:reloaded-count`, and
`:truncated {:bound ... :encoded ... :omitted ...}`. Large refusal detail is
bounded too; short original refusal kinds survive as `:cause` (128-byte cap),
and the error field remains present for CLI refusal behavior.

BB-PROBE-004 and the refusal registry name the prevented failure: a client
reading a truncated stream as a verdict. Witnesses cover exactly the bound,
one byte over, 5000 dependencies, Unicode, huge individual names, failed status,
oversized refusal detail, client parsing, and the real servlet writer boundary.
The closed production scalar fields fit even when no reload name fits.

The spec now says the ordinary receipt omits `verification_complete`, matching
attempt 10's implementation. A witness parses its EDN key-set statement and
compares it to an actual hot probe of `forms-test` (24 tests, 94 assertions).
Restoring the old prose made that witness fail; corrected prose passes.

## Verification receipts

All focused logs, including initial failures and lint repairs, are in `checks/`.
Runtime RED named old undersampled assignments; GREEN passed 3 tests / 802
assertions. Probe RED caught missing encoding and the unbounded servlet;
GREEN passed 6 tests / 137 assertions. Spec RED failed against the old prose.
Final source and standalone apparatus lint report zero errors and warnings.
The census adds exactly three test names and deletes none. CLI `:probe :help`
was executed and documents the bound and incomplete cold proof.

| Required run | Outcome | Evidence |
|---|---|---|
| `make test-fast` once | Exit 0; 1243 tests, 12317 assertions; 0 failures/errors; fast 44924/60000 ms; bb runtime 7455/343102 ms; makespan 25952 ms | `fast.log`, `fast-run/receipt.edn` |
| `python3 attempt18/restricted-gate.py -- make landing-gate-prewarm` once | Exit 0; passed; no problems; 396675 ms receipt wall | `restricted-gate.log`, `restricted-receipt.edn`, `restricted-run/` |
| Real `make landing-gate-prewarm` once | Exit 0; passed; no problems; 397327 ms receipt wall | `prewarm.log`, `prewarm-receipt.edn`, `prewarm-run/` |

The real prewarm's MCP suite measured fast 45275 ms and integration 67216 ms;
its bb suite measured bb runtime 240259 ms against the unchanged 343102 ms
ceiling. All suite leak-failure counts are zero. Every stage, including
standalone bb diagnostic, hygiene and intent audit, exited 0. See the generated
[report.edn](report.edn) for unrounded receipt summaries and per-suite counts.
Both whole runs tested the same product commit and source digest.

[gate.md](gate.md) is the byte-for-byte concatenation of fast, restricted and
real-prewarm logs, in that order, with no inserted headings or omitted refusal
or budget lines. Expected negative-fixture refusal messages remain present.
All live logs were outside the checkout and archived only after each exit.

## Dogfood: every source edit

Native exact-form patches are the applicable route; no Surgeon MCP mutation
tool was available. No CLI wrapper was substituted. No Surgeon refusal or
repair text was encountered. Formatting used installed standard-clj; unrelated
formatting hunks were reverted. `~/bin/clj-kondo` was the lint entrance.

| File | Intent | Mechanism / repair |
|---|---|---|
| `test/clj_surgeon/lane_manifest.clj` | Six-run data and conservative runtime rule | Receipt-generated `runtime-data.patch`; native exact-form rule patch |
| `test/clj_surgeon/lane_manifest_test.clj` | Named sample/ratio/scope/receipt witnesses | Native exact-form patch; RED then GREEN |
| `test/clj_surgeon/bb_ceiling.clj` | Replay recorded calibration runtimes | Native exact-form patch |
| `test/clj_surgeon/ns_isolation_test.clj` | Calibration independence and malformed receipt plants | Native exact-form patch; lint identified missing EDN require, repaired |
| `src/clj_surgeon/probe.clj` | Complete bounded EDN encoding | Native exact-form patch; boundary witnesses |
| `src/clj_surgeon/mcp_http_server.clj` | Enforce bound before writer | Native exact-form patch; actual servlet witness |
| `src/clj_surgeon/core.clj` | Probe help contract | Native exact-form patch; actual help invocation |
| `test/clj_surgeon/mcp_http_server_test.clj` | Encoder, servlet and actual receipt/spec witnesses | Native exact-form patch; RED then GREEN |
| `test/clj_surgeon/help_test.clj` | Help regression | Native exact-form patch |
| `test/clj_surgeon/mcp_alias_migration_test.clj` | Refusal census includes new kind | Native exact-form patch; focused census witness |
| `test/clj_surgeon/deftest_census.edn` | Register three new tests | Existing CENSUS_REGENERATE entrance; no deletions |
| `docs/intent/probe/refusals.edn` | Typed degradation and prevented native failure | Native exact-map patch; existing completeness witness |
| `attempt20/freeze.clj` | Freeze pre-edit paired scope | Native new-form patch; executed before measurements |
| `attempt20/measure_runner.clj` | Isolated namespace control receipt | Native new-form patch; private tmp/home repair described in apparatus |
| `attempt20/measure.py` | Serial controls and process ledger | Native patch; argv list-copy repair after missing receipt |
| `attempt20/fold.clj` | Derive statistics/table/manifest patch | Native new-form patch; replay hashes equal; unused binding lint repair |
| `attempt20/report.clj` | Derive machine report from receipts | Native new-form patch; executed and linted |

HLD, hot-verification design, probe and isolation specs were updated with native
patches in intent → witnesses → implementation order. Tech tree links this
repair. Evidence documents and archives were written natively; EDN tables and
reports were generated by Clojure, never edited as strings. An archive-note
patch attempted delete/add on the same path and was rejected before any write;
it was repaired as a Markdown rewrite. No product change followed either gate.

## Commits, limits and ownership

- `8a742959553da31925354823d2a467c19ef0e967`: archive requested merged-tip
  prewarm-checkonly-run8. Its prewarm exited 0; its separate battery exited 2.
  It is not a green whole-packet claim.
- `ae7b76bcc2463054f5c6a3f020a9e43cbc9ff8b6`: six-run runtime rule and calibration.
- `09588a14a784e220907462f46676900402bc80aa`: bounded probe and spec agreement.
- The commit containing this report is the final evidence-only commit.

The preexisting `docs/observations/battery-ledger.edn` diff is unchanged and
excluded; its starting patch is preserved here. Work stays on
`bb-rewrite-tower-local`. No main/trunk, shared install or forbidden port was
changed. The owned restricted diagnostic temporary packet was removed after
archival. No independent battery, landing or fence certification is claimed.
Six samples establish this box/snapshot's policy input, not immunity to future
runtime drift. Fable accepts the three findings; there are no disagreements.
