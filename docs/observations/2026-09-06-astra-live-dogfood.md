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
