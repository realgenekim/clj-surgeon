# Block B attempt 22 — NO-GO

The typed xray refusal defect is repaired, and the counted JVM battery is green.
The expanded portability rule correctly refuses twelve other namespaces with
failing controls. They are individually accounted for; no runtime was reassigned
to hide a failure. No push or landing was performed.

## Red, repair and green

| Control | Tests | Assertions | Failures | Errors | Evidence |
|---|---:|---:|---:|---:|---|
| Tip JVM, before repair | 26 | 456 | 3 | 0 | [red-tip-jvm.log](red-tip-jvm.log) |
| Tip bb, before repair | 26 | 456 | 0 | 0 | [red-tip-bb.log](red-tip-bb.log) |
| Trunk a15531ee JVM | 26 | 456 | 3 | 0 | [red-trunk-jvm.log](red-trunk-jvm.log) |
| Trunk a15531ee bb | 26 | 456 | 0 | 0 | [red-trunk-bb.log](red-trunk-bb.log) |
| Final xray, each runtime | 27 | 477 | 0 | 0 | [JVM](final-evaluator-jvm.log), [bb](final-evaluator-bb.log) |
| Final edit-DSL, each runtime | 28 | 419 | 0 | 0 | Same final evaluator logs |

The tip red ran at ac197797, which only preserves receipts above 357a2b79.
Trunk used the detached shared clone `target/attempt22-trunk`; no trunk source
was edited. The original failure is pre-existing: JVM SCI wraps typed host
exceptions and uses a different unresolved-symbol message. The repair preserves
typed exceptions and their data/cause identity, adapts the established path and
analyzer reasons, and attaches a cause message only to untyped evaluation errors.

The added actual-SCI builder witness also exposed typed cardinality re-wrapping:
six assertions failed on each runtime before the final correction. Its red and
green receipts, the initial output-size regression and its repair are detailed
in [xray-findings.md](xray-findings.md). The SCI allowlist and CLI output bound
are unchanged. Lint reports zero errors/warnings in [lint-final.log](lint-final.log).

## Every runtime assignment

The one complete census lists all **159** namespaces: **97 pass both runtimes**,
**50 have configured bb initial-load incompatibilities**, and **12 have failed
complete test controls**. Eleven of those twelve are asymmetric; prune fails on
both runtimes because its `/private/tmp` fixture base is absent on this host.
Every initial-load incompatibility has its exact reason in the table. It is
neither a passing test nor evidence of permanent incompatibility on every setup.

- [Every namespace, status and receipt](portability-census.md)
- [Individual accounts for all twelve failing-control namespaces](nonportable-findings.md)
- [Raw commands/results and observed process exits](controls/index.edn)
- [All 159 assignments unchanged](runtime-assignments-check.edn)

TEST-ISO-016 now requires complete passing counters and zero exits on both
runtimes for portability, independently of cadence and the 38 paired cost
members. Missing controls and wrong identities refuse. The manifest witness
prints each non-portable namespace even if its assigned runtime passes.
Both runtime-rule checks report 198 passing assertions and exactly twelve
live-control failures: [JVM](runtime-rule-jvm.log), [bb](runtime-rule-bb.log).
Accounting for those failures does not turn the gate green.

The census snapshot is 4492f9df. The later evaluator follow-up is independently
green on JVM and bb, and the final JVM battery runs the new xray witness again.
The runtime-rule code consumes the frozen census and was checked on both runtimes.
This is not a second full census or a claim that every final-snapshot control
passes. Paired runtime timing samples, budgets, cadence membership and runtime assignments are
unchanged. No performance claim is made from these correctness controls.
The battery's generated `battery-namespace-walls.edn` packing estimates were
retained as run output; they are separate from the six-sample runtime controls.

## Requested gates

| Gate, each executed once | Result | Observed wall | Evidence |
|---|---|---|---|
| `make test-fast` | 1,244 tests; twelve portability failures; zero isolation/budget violations | 29 s wrapper; 46,188 ms serial-equivalent | [fast log](checks/fast.log), [lane receipts](fast-run/) |
| `make test-battery` | PASS: 1,166 tests / 18,472 assertions; zero failures, errors, isolation violations or skips | 350 s counted ledger; 352 s outer wrapper; 347,765 ms coordinator makespan | [battery log](checks/battery.log), [summed counters](battery-total.edn), [lane receipts](battery-run/) |
| `make landing-gate-prewarm` | NO-GO: MCP's portability witness reports the same twelve names; alias, bb and shell work pass | 171 s wrapper | [verbatim gate.md](gate.md), [full lane receipts](prewarm-run/) |

The battery charges 1,000,933 ms serial-equivalent over 53 namespaces. Its
passing row names 14ae555f and is committed in 3fdd5af4. The prior failed
357a2b79 row and untracked check-only run were preserved first in ac197797;
the latter is under [prewarm-checkonly-run10](prewarm-checkonly-run10/).
That check-only record is not counted as this attempt's real prewarm.

Prewarm's reported MCP cadence sums are 47,143 ms fast against 60,000 and
69,226 ms integration against 240,000. The bb suite reports 242,253 ms actual
bb-runtime work against 343,102. The refusal is the named correctness controls,
not a raised or exceeded budget. `gate.md` is byte-identical to the full real
prewarm transcript, with [its digest](gate.sha256). No unchanged retry was run:
the twelve accounted defects remain, so the permitted repair/final-retry
allowance was not consumed. The owned detached trunk clone was removed after
its receipts were frozen; [cleanup record](trunk-cleanup.log).

## Commits and dogfood

- ac197797 — preserve the prior failed battery and check-only receipts.
- 4492f9df — initial typed SCI refusal repair and named witness, green on both runtimes.
- 43a36e6b — actual SCI builder witness and general typed-refusal preservation, green on both runtimes.
- 14ae555f — all-assignment portability rule, documentation and additive test ledger.
- 3fdd5af4 — counted passing battery row.
- 295ffa6e — retain the battery runner's generated namespace packing estimates.
- The commit containing this report is the final attempt22 evidence commit.

[Dogfood ledger](dogfood.md) records every source edit, mechanism and repair;
[commands](commands.md) records the entrances. No test was deleted or runtime
changed. The source ledger only adds the two new witness names. Initial bounded
`sed` source reads were a deviation from the brief; all Clojure source mutations
used native exact-form patches or the existing derived-ledger entrance.

## Owed, uncertainty and disagreement

The twelve named failing-control namespaces need their recorded reader/oracle,
dependency, launcher, reflection, fixture-root and recovery issues repaired
before portability can be certified. Fresh affected controls and a passing
fast/prewarm gate are then owed. The shared-cause diagnoses for launcher and
recovery failures are bounded by the retained evidence; recovery's exact
lock-release mechanism is not established here. Only xray received the requested
trunk reproduction; no trunk-reproduction claim is made for the other failures.

The earlier Sol GO covers the supplied candidate, not this new evaluator change.
Independent fence review of the resulting candidate remains owed before merge.
There is no disagreement with the new rule: the battery is green and the broader
portability gate is correctly red. This is a branch-only NO-GO handoff.
