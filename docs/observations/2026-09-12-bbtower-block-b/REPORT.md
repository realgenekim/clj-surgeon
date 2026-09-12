# Block B: BLOCKED

Subject remains detached at `978a0c485ea4d23504da4105f7572614268b4335`.
Manifest: `b28586b6b7e8c5295e949c019f56d25d63364df3b158813504708107e6ddfb30`.
No implementation changes or commits; only this observation directory was
written. No JVM, server, or prewarm attempt was launched.

## Findings

1. **The prescribed unchanged bb budget does not exist at the named base.**
   `eae1e43280635be9b4e317e176d29476fd358702:test/clj_surgeon/ns_isolation.clj:60`
   defines only `{:fast 60000 :integration 240000 :battery 1800000}`.
   The base coordinator explicitly calls `report!` with isolation disabled
   for suite `bb` (line 1069). Its original bb runner has no lane ceiling.
   A newly chosen ceiling cannot honestly be called restoration of that oracle.
   The brief also names `src/clj_surgeon/ns_isolation.clj`, while the existing
   implementation is under `test/clj_surgeon/ns_isolation.clj`.
2. **The fast failure was not the summed bb/fast union.** The retained log's
   full union is 305,686 ms. Its 68,210 ms refusal already charges precisely
   the original 61 JVM-fast members. The base and current fast memberships
   agree. The original bb set has 50 members and overlaps fast in
   `quoted-var-refs-test` and `workspace-onboarding-test`. Removing every
   bb-only member therefore leaves the same fast refusal. A read-only replay
   through the current coordinator reproduces it; no new timing is claimed.
3. **The requested probe witness list cannot be certified from this subject.**
   No probe `refusals.edn` is tracked and the receipt-boolean registry/scenarios
   cover two write verbs, not probe. See [probe-contract.md](probe-contract.md).

The step-one stop is an authority boundary: the packet freezes the oracle and
reserves amendments to Gene or the registered owner. Fable must identify a
corrected base/contract or supply the intended bb ceiling before implementation
can satisfy the assignment. The step-three gap also needs an identified
existing witness subject or a scope amendment, rather than a fabricated green.

## Measurements

All values below are historical Block A observations or explicit re-folds of
those observations. **After values are unknown; no new suite ran.**

| Measurement | Retained before | After | Evidence |
|---|---:|---|---|
| Original JVM-fast members, summed namespace walls | 68,210 ms | Not measured | [replay.log](replay.log) |
| Original bb members, summed namespace walls | 237,596 ms | Not measured | [reconstruction](baseline-reconstruction.json), [raw log](/var/tmp/forge/bbtower-fx/fast-1g.log) |
| Overlapping members, summed walls | 120 ms | Not measured | Same reconstruction and raw log |
| Full union, summed namespace walls | 305,686 ms | Not measured | [raw log](/var/tmp/forge/bbtower-fx/fast-1g.log) |
| Coordinator parallel makespan | 83,233 ms | Not measured | Same raw log |
| Feature-thread bb namespace wall | 727,579 ms | JVM comparison owed | [prewarm log](/var/tmp/forge/bbtower-fx/prewarm.log) |
| Integration sum | 758,486 ms | Not measured | Same prewarm log |

The bb/fast sums overlap; they must not be added without subtracting the
shared members. These numbers establish no speed comparison. The replay's
makespan is supplied from the retained log, not timed again.

The base source files are preserved byte-for-byte in `base-*.log` beside this
report. `baseline-reconstruction.json` records full member sets and the source
log hash. `retained-walls.edn` contains the logged namespace wall rows.
Reproduction: run the command in [gate.md](gate.md); its transcript names the
lane, sum, ceiling, and absent probe registration.

## Owed and uncertainty

- Fable: resolve the missing bb ceiling and false union premise; identify or
  authorize the probe contract witnesses and correct the isolation source path
  if changes there are intended.
- Astra: implement the approved contract with ceiling/ceiling-plus-one and
  runtime witnesses; measure feature-thread once on each runtime, then execute
  the required fast/integration checks. Stop if integration exceeds its ceiling.
- Astra or an eligible executor assigned by Fable: final-tip prewarm, any
  permitted repair and final retry, battery, and warm CLI example. Every
  unexecuted check remains owed; no historical result is final-tip proof.
- Fable: independent review and landing decision after actual green evidence.

Least sure: another intended base may contain the missing bb policy; historical
walls cannot predict the next run under the single-JVM lease; the requested
probe contracts may exist on another unprovided subject. Disagreement with the
brief is about witnessed source and oracle authority, not permission to relax
a budget. No stable tag, landing, push, or independent acceptance is claimed.

Machine-readable handoff: [report.edn](report.edn).
