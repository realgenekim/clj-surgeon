# Astra captain's log — first live owner-forms dogfood

Recorded 2026-09-06 01:41Z. Source change: `981372ee`, engine: `fb678c57`, branch
`astra/typist-route`. Fence review and landing gates requested from Fable; no
merge or public-main push performed.

The tool changed its own real `diagnostic_delta.clj`: `finding-identity` became
`finding-fingerprint`, and private `field` became `finding-field`, with their
references updated. Five definitions were selected. The two test references
were prepared separately through Surgeon's guarded CLI before freezing proof.
One live Cerebras candidate passed the actual focused test and separately
authored acceptance witness. The kernel committed the source; the live result
was kept, not undone. The preceding fake-provider hand-drive had proved exact
undo on the frozen real fixture.

## Two clocks, not one claim

| Phase | Observed wall |
|---|---:|
| Cold `bin/mission propose` | 6.830 s |
| Cold `bin/mission apply` | 8.232 s |
| Sum of those two commands | 15.062 s |
| Executor inside apply | 2.850 s |
| Transport, including Python process | 1.716 s |
| Provider request | 1.659 s |
| Focused gate | 18.9 ms |
| Independent acceptance | 14.7 ms |

This was one preregistered functional hand-drive, not an A/B or a demonstrated
speedup. Human preparation, the separate red-gate edit and inspection between
commands are not in the 15.062 s sum. Approximately 12.21 s of the command sum
lies outside the executor: proposal work and cold apply overhead. Selling the
2.850 s internal number as the complete CLI task would be wrong.

The upstream response named Cerebras and `openai/gpt-oss-120b`. Usage: 1530 prompt
tokens, 3171 completion tokens, including 2324 reasoning tokens. One candidate,
terminal stop, both proofs green, original protected bytes and modes checked,
and zero live transport processes at commit. Initial one-minute loads were
0.85 at proposal and 1.30 at apply; the shared quiet window was declared and
released. Calls ran under `slot -t`.

Routing used the latest measured class/provider prior, Fable's whole-file 18/20,
so the current rule selected k=1. The preregistration and dossier explicitly label
this as a **transferred representation prior**. Forms now have one observed
success; this is not evidence that their success probability is 90%. Future
calibration must include edit representation, not only class/provider.

## Friction and next decisions

- Preparing the test with `:change!` hit two typed refusals: missing explicit
  `:receipt-out`, then a replacement requiring source-string syntax. The third
  call succeeded. Existing skill examples did not make those requirements clear.
- The live receipt incorrectly reports `match-count 0`, despite five replacement
  forms. Its source/proof guarantees passed independently. Fix the counter and
  expose formatter timing; do not infer no-op from this faulty field.
- Cold command overhead dominates this small task. Keep this loss term visible
  while comparing CLI, resident execution and native tools.
- Fable's first warm comparator had four valid controls, not the requested six;
  orientation failures cannot count as controls. Six valid controls have been
  requested before the new forms comparison cohort.

Artifacts: `/var/tmp/forge/astra-live-real1-fx` contains preregistration, source
and gate hashes, both phase clocks, full mission output and retained proof.
The candidate, close receipt and inverse live under
`receipts/mission-11935165992443803525`. The full mission receipt is also in the
source commit body. No credentials were placed in the dossier or receipts.

## Astra 01:56Z — second mission refused; telemetry repaired

The next real edit targeted three executor owners to preserve actual form count and formatter metadata. Its frozen proposal at `/var/tmp/forge/astra-telemetry-fx/spec.edn` blocked before provider call or write with `:forms-protected-syntax`: the commit owner contains protected comments. Comments remained intact; the authorized native fallback changed the three small receipt handoffs. This is a recorded coverage loss, with no timing or speedup claim.

Three assertions in the existing real-proof/commit/undo witness failed on the pre-fix code (zero count and missing formatter status/time). After the fix, the executor plus lane-accounting suite passes **31 tests, 121 assertions**; formatting and diff checks pass. Raw logs: `astra-telemetry-fx/red.log` and `green-corrected.log`. An intervening test command used nonexistent alias `:mcp` and failed before loading tests; it is retained in `green.log`, not counted as a test result. The actual gate uses `-M:clj-surgeon/test-deps`.

Receipt `:format` now preserves the kernel formatter subprocess time, status and owned-fragment counts. It is not the complete staging/formatting phase wall. `:match-count` reflects submitted, successfully compiled owner replacements instead of a hardcoded zero. The prior live keeper receipt is historical evidence and was not rewritten.

## Astra 02:08Z — one-process entrance hand-driven

The real-1 fresh fixture verified through `bin/mission run` in7.681950729s complete command wall; internal executor2.313261372s; gate19.415ms; acceptance14.418ms; owned-fragment formatter494.449ms. Receipt correctly reports five owners and one changed file. k1 pinned Cerebras, no fallback, no surviving transport process. Raw prereg, stdout, stderr and wall are in `/var/tmp/forge/astra-one-shot-live-fx`; this fixture run was retained rather than a second repository rename.

**Timing qualification:** Fable reported that fence-review PID290129 had started concurrently. The pilot start sampled load1=1.195; Fable separately reported1.89. It is functional evidence, excluded from the clean timing cohort, not a replicated comparison or startup-only effect estimate. The subsequent four-pair preregistration waits for that actual process to exit.

## Astra 02:10Z — delegated event-hook dogfood

The event-hook builder attempted the actual owner_forms planner on mission_cli/propose! before editing. It returned forms-protected-syntax with committed=false and mutation-attempted=false; zero provider calls. Receipt `/var/tmp/forge/mission-events-admission.log`. Native fallback preserves the protected comments. This is another observed coverage refusal, not an adoption or speed win.

## Astra 02:35Z — raw Clojure hand-drive verified

Engineb1559a95, one fresh real-1 mission, k3 from explicitly transferred JSON3/4 prior. All three provider responses completed; candidate2 was selected, passed gate+witness, and committed five owners. No live processes/cancelled work. A later non-writing replay proves all three retained candidates compile, format and pass the same gate+witness. This is one functional mission plus3retained-candidate proofs, not3independent missions or a calibrated reliability rate.

Total provider-reported cost$0.0056541;4536prompt tokens,5422completion including3607reasoning. Raw receipts/prereg/replay are in `/var/tmp/forge/astra-raw-live-fx`. Forensic elapsed7.630s includes low-priority scheduling under concurrent fence848070; internal executor2.127s, owned formatter977.3ms. These are excluded from performance claims.

The saved response-format dispatch regression failed before integration: the old executor accepted a JSON response despite a frozen raw request. It now refuses JSON in raw mode and honors saved format even if the apply request names another format; commit/undo restores exact source. Combined45tests349assertions passed, then the added one-file-only refusal assertion passed in5tests166assertions.
