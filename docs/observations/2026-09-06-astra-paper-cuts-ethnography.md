# Astra — where the paper-cut round spent its time

<!-- agent-usage-window-end: 2026-09-06T04:44:02Z -->

Window: 2026-09-06 02:57:00–04:44:02 UTC; 2026-09-05 19:57:00–21:44:02 America/Los_Angeles. This is a retrospective of a development/review window, not a matched performance experiment. The cutoff comes from the command clock. An initial collection accidentally requested a later cutoff; it is superseded, not pooled. Counting authority is `/var/tmp/forge/astra-paper-cuts-ethno-fx/receipt-fixed.json`, SHA-256 `c369b60021630beb814f34f045220db44709b5969413282fe721e89463fdd23d`. Collector status is `ok`; the repository collector self-test passes.

## Observed activity

| Transcript population | Codex | Claude |
|---|---:|---:|
| Sessions in window | 10 | 24 |
| Clojure-relevant sessions | 8 | 13 |
| Classified Surgeon invocations | 187 | 10 |
| `:cat` / `:ls` / `:xray` | 141 / 24 / 3 | 1 / 5 / 0 |
| `:change!` / `:fix-declares!` | 19 / 0 | 2 / 2 |
| Measured Surgeon-bearing outer actions | 119 | unavailable |
| Median / p90 outer action wall | 0.671 / 1.509 s | unavailable |
| Classified native Clojure actions | 4 apply_patch; 188 shell | 3 Edit; 1 Read; 181 shell; 4 Write |

These populations mix builders, reviewers, synthetic probes and tests. They are not equal tasks or equal observation coverage. A shell action may bundle several calls and unrelated work. An invocation count proves neither successful application nor voluntary adoption. Claude clock coverage is absent here, not zero time. Native shell/read routes also include construction, tests, plans and review: no avoidance or tool-policy violation is inferred from counts alone.

The default service-root scan found two server-start events and no public MCP calls. It is not a fleet census and excludes fixture-specific roots and this round's isolated test event files. The request concerns this development round; no box-wide usage or quietness claim is made from that scan. Direct transcript operations in this sample used CLI; no callable Surgeon MCP tools were exposed to the root seat, so CLI was locally justified. User preference remains production CLI, with shared services untouched.

## The time is between actions as well as inside them

Codex recorded 145 post-Surgeon boundaries: median 7.506 s, p90 44.422 s. Sixty-four led to another Surgeon read, twenty-two to a native read, seven to a Surgeon apply, seven to a native patch, and the remainder to shell, collaboration, messages or verification. These are aggregate boundary descriptions, not 145 redundant actions. Reads may resolve a newly discovered owner; batched outer calls do not imply one inference per inner call.

One completed turn (`d905aff9d8c4` in anonymized session `b3142519c37b`) spans 34m05.54s. Its event clock assigns 18m47.05s to recorded model items, 428ms to Surgeon, and 10m53.31s to unattributed intervals; coverage is 68.1%. The outer-action measurement for its Surgeon-bearing shell calls is larger because those actions can contain other work. Neither measure is the duration of a single edit. Unattributed time can include scheduling, transport, serialization or logging; it is not all reasoning. Overlapping sessions must not be summed into elapsed wall for the program.

The dominant route families are structural read → further read/probe → verification/Git, with occasional guarded structural application; the round is review-heavy. Timelines also show model and unattributed intervals between those actions. Eliminating milliseconds in the parser alone cannot remove these task boundaries. This does not prove a particular fused tool would save them: source judgment and independent safety review still have to happen.

## What actually improved, and what still failed the caller

A concrete deterministic dogfood operation patched the decided Git submodule-visibility flag through `:change!`. Its retained receipt verifies one edit in one file. Two faithful adversarial regressions failed before the fix and pass afterward. The operation was cheap; discovering why it was necessary took independent execution and review. No matched native arm was run, so there is no speedup claim for this edit.

The receipt classifies 64 Codex outer actions as containing refusal text. Do not call this a 64/119 live refusal rate: this window deliberately executes negative tests, and batched test output contains diagnostic codes. The same applies to apparent execution-error counts. The independent caller exercise is a cleaner usability signal: it could not construct a legitimate `owner_forms` request from public help/documentation without guessing nested typist fields, priors and proof facts. `help propose` returned unrelated generic help; publication restrictions were missing from undo/resume help. The exercise used only user-facing surfaces but inherited project context, so it is not a fully blind adoption cohort.

Those findings have separate remedies. The execution kernel has new scope and publication recovery guards, with a combined 101-test / 827-assertion gate. The CLI author is repairing discoverable request documentation and precise preview/recovery semantics. The next provider comparison is still held for four independent apparatus corrections; no latency result is manufactured by treating a prepared pilot as an accepted cohort.

## Smallest falsifiable next step

After the help correction, give a genuinely fresh caller only the CLI help, public request contract, a fixed task and supplied honest calibration/proof facts. Require it to identify exact commands and authority boundaries without consulting implementation or design plans. Measure first-command-to-verified-receipt wall plus preparation separately; compare against native on the same task and proof. Unknown priors should refuse or cede explicitly, never be invented to make the demo run.

The product target is a bounded request plus an executable next action and a trustworthy receipt. Keep general task judgment with the model; let the kernel derive identity, preserve untouched bytes and enforce proof. This goes with improving models because better models can supply better intent and candidates without changing the mechanical authority boundary. It is not yet proof of sustained efficiency: mechanism, self-hosting, fresh-caller success and matched efficiency remain distinct rungs.
