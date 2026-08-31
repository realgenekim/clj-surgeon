---
parent: substantiation-telemetry-design
status: 'ratified in advance (Gene, 2026-08-30, verbatim: "Go on all!!!")'
client_metadata_privacy: 'decided A (conn, 2026-08-30, under Gene window authority; subject to Gene override at review)'
---

# Substantiation telemetry ratification record

Gene directed: “Build enough telemetry to substantiate improvements and bring
inspiration and confidence.” He then ratified this leaf in advance with
`Go on all!!!` on 2026-08-30. This packet is the permanent design authority.
The implementation may proceed through frozen red, green, independent
SURGEON2 verification, and live overhead measurement. Installation and shared
MCP reload remain separately gated.

The leaf answers four production questions that the current telemetry cannot:

1. Was a prepared request emitted, later consumed, and committed?
2. Did a complete refusal lead to a same-file reread or a direct corrected
   retry?
3. What source-free semantic kinds did the recovery read return?
4. How often are read normalization and WRITE-REFUSAL-001 actually used?

The selected design is a separate privacy-safe append-only ledger plus one
claims-safe weekly report. It does not expand the existing full telemetry
payload, alter a tool request or result, or grant performance-promotion
authority.

## Evidence authority

- Installed product baseline:
  `9af88fbae9ee720613599feaf8cf58432c5898bb`.
- Frozen consumption-gap study:
  `experiment/consumption-gap-20260830@1648d5db3e5f8d107efd9b383012a857ac827bba`.
- Frozen classifier SHA-256:
  `0597202eec1714486b749be313fdb108fb2521f5f43bcb44a34e85cfac8bbac7`.
- Frozen classifier coverage: 0 of 119; verdict
  `instrumentation-repair-required`.
- Pre-install baseline marker:
  `2026-08-30T02:09:33.141926Z` from the 2026-08-29 emission study.
- Decode projection rate: 3.5237 ms per emitted request byte, correlational,
  never a measured saving.

## Phase chain

```text
ratified packet -> frozen red -> product green -> SURGEON2 verification
                 -> live overhead measurement -> separate install decision
```

No packet text authorizes installation, a shared-runtime change, a causal
speed claim, or a promotion claim.
