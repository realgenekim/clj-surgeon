# Privacy-safe agent event clock

**Status:** complete
**Owner:** `clj-surgeon-tmr.8`

## Outcome

Extend `study-agent-usage` so one bounded receipt shows the observable wall-time
sequence inside each Codex task turn. The clock must distinguish measured model
work, tool execution, messages, and unattributed boundary gaps without emitting
reasoning text, transcript prose, commands, source, arguments, results, or paths.

The primary question is not "how fast was Surgeon?" It is "which complete-turn
interval can a better tool contract delete?"

## Observable contract

Each Codex task turn gains an `event_clock` with:

- one ordered item per completed Codex item;
- `kind`, turn-relative `offset_ms`, and `wall_ms` for every item;
- only bounded public labels for structural operations, status, and message phase;
- explicit `unattributed-gap` items between measured items;
- totals by kind, measured coverage, unattributed wall, and coverage ratio.

Allowed kinds are model reasoning, model message, Surgeon read/plan/apply,
semantic read, native read/patch, verification, shell, coordination,
collaboration, compaction, human input, other tool, and unattributed gap.

`model-reasoning` means Codex recorded a completed `Reasoning` item. It does not
expose or reconstruct hidden chain of thought. `unattributed-gap` means only
that no completed item owns the interval; it can include inference, scheduling,
transport, serialization, logging, or UI delay.

## Invariants and non-goals

- Never emit item content, summary text, raw content, command, CWD, arguments,
  result, stdout, stderr, source, account, URL, or workspace path.
- Preserve the existing version-3 receipt fields and counting semantics.
- Clamp malformed or out-of-turn item clocks to the turn boundary.
- Preserve overlapping items as evidence, but compute measured coverage as the
  union of intervals so totals cannot exceed complete turn wall.
- Claude remains aggregate-only until its retained history supplies equivalent
  item clocks. Do not fabricate parity.
- Do not describe unattributed gaps as hidden reasoning.

## Behavior matrix

| Case | Required result |
|---|---|
| reasoning item | exact duration as `model-reasoning`; no summary text |
| clj-surgeon MCP inspect/apply | bounded operation label and measured tool wall |
| CLI Surgeon command | route-classified read/plan/apply; no command text |
| native file change | `native-patch` |
| interval between completed items | explicit `unattributed-gap` |
| overlapping items | both retained; union coverage remains bounded |
| item partly outside turn | clamped to turn |
| unknown item/tool | stable generic kind, no raw payload |
| in-progress turn | bounded by receipt `until`, marked incomplete |
| Claude session | existing aggregate unchanged; no invented event clock |

## Real-program evidence

Re-run the exact 24-hour window `2026-08-26T23:26:30Z` through
`2026-08-27T23:26:30Z`. Render the Surgeon-using turns as ASCII timelines,
classify CLI choices from only receipt-named narrow regions, and compare the
measured model/tool/gap distributions with the existing Surgeon MCP and cclsp
service telemetry.

## Verification

1. Extend the hermetic collector self-test with completed-item fixtures,
   overlap, gaps, and privacy canaries.
2. Run `make study-agent-usage-self-test`.
3. Re-run the exact 24-hour receipt and assert no private canary or workspace
   path is present.
4. Add the event-clock invocation and interpretation law to the installed
   `study-agent-usage` skill.
5. Write a new Captain's Log with the receipt boundary, CLI classification,
   cclsp status, dominant timelines, limitations, and smallest falsifiable
   next improvement.

All five gates passed for receipt
`/tmp/clj-surgeon-agent-usage-24h-clock-20260827.json`. The repository-wide
full test was also attempted; its core suite passed, while three unrelated
existing MCP assertions remained red in cold analyzer admission and the intent
audit. The focused event-clock, install/help, privacy, and receipt gates passed.
