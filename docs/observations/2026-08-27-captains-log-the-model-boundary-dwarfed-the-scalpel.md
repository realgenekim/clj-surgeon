# Captain's Log: The model boundary dwarfed the scalpel

<!-- agent-usage-window-end: 2026-08-27T23:26:30Z -->

## Outcome

Gene kept noticing the same product smell: Surgeon would return in a fraction
of a second, and Codex would appear silent for several more seconds before the
next call. The earlier frozen extraction benchmark converted that observation
into event clocks and ultimately found the verified terminal-response win. This
study turns the same microscope into a reusable natural-history instrument.

`study-agent-usage` now emits privacy-safe Codex `event_clock` evidence from the
client's own completed-item timestamps. `make study-agent-timeline` renders an
ASCII storyboard that separates:

```text
model-reasoning -> Surgeon -> model-reasoning -> next visible act
```

from shell work, native changes, messages, collaboration, compaction, and
unattributed gaps. No reasoning text, transcript prose, command, argument,
result, CWD, workspace path, or source enters the receipt.

The first 24-hour replay confirms the central design thesis. Direct Surgeon
execution is usually tiny. The expensive unit is another model/tool decision
boundary.

## Window and method

- UTC: `2026-08-26T23:26:30Z` through `2026-08-27T23:26:30Z`.
- Pacific: 2026-08-26 16:26:30 through 2026-08-27 16:26:30 PDT.
- Counting authority:
  `/tmp/clj-surgeon-agent-usage-24h-clock-20260827.json`.
- Receipt schema: `clj-surgeon.agent-usage-ethnography.v4`.
- Sources: bounded local Codex and Claude histories, Surgeon MCP telemetry, and
  cclsp/clojure-lsp flight-recorder events in the same exact window.
- Intent reconstruction: three receipt-named Codex transcripts were read only
  around CLI transitions. Published evidence retains only neutral behavior
  classes and counts.

The clock uses completed Codex items. `model-reasoning` means Codex recorded a
Reasoning item with an exact start and finish. It does not expose hidden chain
of thought. `unattributed-gap` means no completed item owns that interval. It
can include inference, scheduling, prompt ingestion, serialization, transport,
logging, or UI delay.

Long turns can contain overlapping work, such as a background command while
reasoning continues. The receipt retains both items but computes measured
coverage as an interval union. It never manufactures more than 100 percent of
complete wall.

## Population

| Meter | Codex | Claude |
|---|---:|---:|
| Sessions in window | 22 | 8 |
| Clojure-relevant sessions | 16 | 2 |
| Task turns with compatible clocks | 81 | 0 |
| Surgeon-using task turns | 19 | 0 |
| Agent-observed Surgeon operations | 277 | 0 |

Claude history in this window supports route aggregation but not equivalent
task/item clocks. The study does not fabricate cross-provider timing parity.

## The primary finding

The event clock found 188 Surgeon action items with a measurable following
boundary:

| Meter | Result |
|---|---:|
| Direct item wall | 73.714s total |
| Direct item median | 0.243s in the agent-continuation subset |
| Completion to next visible act | 53m08.573s total |
| Boundary median / p90 | 8.901s / 36.535s |
| Explicit reasoning in boundary | 26m17.980s total |
| Reasoning median / p90 | 3.002s / 26.612s |
| Direct-wall to following-boundary ratio | 1 : 43.3 |

Eleven boundaries ended in human input, collaboration, or coordination. After
removing those, the 177 agent-continuation boundaries retained the same shape:

- direct tool median: 0.243s;
- next-act boundary median: 9.124s;
- next-act boundary p90: 36.633s;
- explicit reasoning median: 3.246s;
- explicit reasoning was 49.1 percent of aggregate boundary wall.

This is observational, not a randomized transport comparison. A long boundary
can contain necessary design work. The causal statement is narrower: every
unnecessary call creates another opportunity to pay a many-second decision
boundary, while the median tool itself consumes hundreds of milliseconds.

The endpoints show where the model went next:

| Next visible act | Boundaries |
|---|---:|
| another Surgeon read | 75 |
| native read | 29 |
| model message | 25 |
| native patch | 24 |
| shell action | 22 |
| coordination or collaboration | 7 |
| human input | 4 |
| Surgeon apply | 1 |
| verification | 1 |

The 75 read-to-read boundaries are the clearest product opportunity. The model
often knew a coherent question only one fragment at a time because the tool or
routing contract did not compile the complete read mission.

## Two storyboards

One five-minute MCP turn contained eight Surgeon reads. Surgeon consumed only
1.640 seconds, while measured model items consumed 4m24.99s and unattributed
gaps consumed 1m24.53s. A representative passage was:

```text
Surgeon inspect refusal   0.068s
unattributed boundary     3.005s
model reasoning           1.008s
unattributed boundary     3.231s
Surgeon inspect success   0.171s
unattributed boundary     3.258s
model reasoning          14.264s
unattributed boundary     4.456s
Surgeon inspect success   0.498s
```

The scalpel was not slow. The route repeatedly returned control without enough
compiled evidence to finish the decision.

One 1m46.63s CLI turn batched nine CLI reads into four process launches. The
CLI itself consumed 1.951s; model items consumed 1m17.61s and unattributed gaps
27.067s. Even good process batching did not remove the repeated decide/read
cycle:

```text
CLI :cat x2   0.467s -> boundary + reasoning
CLI :cat x3   0.514s -> boundary + 12.060s reasoning
CLI :cat x2   0.532s -> boundary + 26.703s reasoning
CLI :cat x2   0.438s -> boundary + 15.931s reasoning
```

This is why hot process startup alone cannot deliver the desired interaction
compression.

## Why agents chose the CLI

CLI use was concentrated in three Codex sessions:

| CLI evidence | Count |
|---|---:|
| Operations | 86 |
| Reads (`:cat`, `:ls`, `:show`) | 85 |
| Writes (`:change!`) | 1 |
| Outer shell actions | 53 |
| Direct outer-action wall | 41.298s |
| Median direct outer-action wall | 0.665s |
| Adjacent CLI-to-CLI boundaries | 26 |
| CLI-to-CLI boundary wall | 305.969s |
| CLI-to-CLI median / p90 | 6.464s / 23.637s |

No observed operation required CLI-only capability. Form reads map to
`inspect_clojure`, outlines map to its outline operation, and the exact owner
deletion maps to `edit_clojure.delete_owners`.

The bounded transcript classification explains the choices:

1. **MCP already active: 29 operations.** These were avoidable coexistence or
   route habit.
2. **MCP discovered late: 17 operations.** The caller used CLI reads, later
   queried the deferred catalog, and then successfully used MCP. This is a
   proven discovery failure.
3. **MCP never discovered: 40 operations.** The caller opened the visible CLI
   fallback reference immediately before the burst. CLI was locally defensible
   because no direct schema was visible, but systemically this is likely an
   interface-salience failure.

The event clock contains 46 CLI outer action items because one shell action can
batch several of the 86 operations and not every historical action has a
compatible completed-item clock. The operation counter remains the adoption
authority; the clocked subset remains the timing authority.

The smallest routing ratchet is not a faster CLI. Surface `inspect_clojure` and
`edit_clojure` directly. If they must remain deferred, require one catalog
lookup before CLI fallback and remember that discovery for the session. Do not
pay a meta-discovery turn before every edit.

## cclsp encapsulation status

The concise status is:

```text
config-encapsulated
live-client-incomplete
runtime-delegated
replacement-prototype-only
```

What is complete:

- cutover commit `55d6afb` is included in installed `7185271`;
- current workspace configuration emits only the Surgeon MCP;
- current repository configuration contains no direct cclsp registration.

What remains:

- this long-lived Codex session can retain cached direct cclsp schemas;
- repository guidance and one tool description still mention direct cclsp;
- `prepare-change` still calls Surgeon semantic client -> cclsp
  `resolve_var_surface` -> clojure-lsp before capturing source files;
- `syntax_var_refs` is pure and tested but has no production caller;
- candidate-bounded clj-kondo and snapshot-guarded caching remain experiments.

At audit time the broker was PID 3893, CWD
`/Users/genekim/src.local/cclsp-structural-results`, and Surgeon was PID 65458,
CWD `/Users/genekim/src.local/clj-surgeon`. All broker-managed clojure-lsp
workspaces were cold with no child. Two old direct cclsp/LSP pairs belonged to
long-lived Claude parents in CWD
`/Users/genekim/src.local/social-media-writer`; they were legacy clients, not
evidence that the new configuration still published cclsp. The subsequent
direct-client cutover removed all four legacy processes and stopped the shared
external broker while preserving Surgeon PID 65458 in its stated CWD. See
`2026-08-27-captains-log-direct-cclsp-clients-retired.md`.

The 24-hour demand was small but ceremonious:

- five `resolve_var_surface` calls;
- three LSP `initialize` calls;
- one actual `textDocument/references` call;
- 9.603s of initialization versus 1.903s of useful semantic request wall;
- initialization was 83.46 percent of LSP wall;
- one repeat candidate existed, but no snapshot-safe cache hit is claimed.

The next product slice should compile an exact frozen single-Var surface from
definition source, namespace aliases, `syntax_var_refs`, and quoted-Var
evidence. A complete result terminates inside Surgeon. A named bare-symbol or
other proof gap escalates once to today's cclsp path. Candidate-bounded
clj-kondo should be added only for that exact gap, and only after the pure
reducer earns itself. Caching and broker renaming come later.

## Decisions

1. Keep optimizing complete verified task time, not tool milliseconds.
2. Treat repeated read-to-read boundaries as the primary evidence of a missing
   compiled read mission.
3. Make the event clock part of `study-agent-usage`; do not leave it as a
   benchmark-only probe.
4. Stop interpreting every silence interval as hidden thinking. Report explicit
   Reasoning items and unattributed gaps separately.
5. Treat CLI use as a visibility/routing problem in this window, not proof that
   agents prefer process startup.
6. Describe cclsp as encapsulated configuration with internal delegation still
   active. Do not claim subsumption yet.

## Next falsifiable hill

Add privacy-safe action ordinal, transport, operation, batch cardinality, and a
hash of the structural target/snapshot to each clocked read. On a frozen
historical cohort, detect repeated reads of the same decision surface and
compile the largest coherent group into one `inspect_clojure` request.

The gate is complete-task geometry, not adoption:

- at least 50 percent fewer read actions on the selected cohort;
- zero loss of required source evidence;
- zero added native rediscovery;
- at least 25 percent lower complete-turn wall in a small counterbalanced
  clean-context experiment.

If target hashing cannot be made privacy-safe or the grouped questions were
not knowable at the first call, stop. The clock remains useful evidence even if
that compiler option loses.

## Verification

- `make study-agent-usage-self-test`: green after a red missing-clock witness.
- The self-test covers exact completed-item durations, gaps, overlap-safe union
  coverage, turn clamping, CLI/MCP transport, rendering, and private canaries.
- The identical 24-hour bounds were rerun once after the instrument changed.
- The final receipt reports `status=ok` and schema v4.
- `make study-agent-timeline` successfully rendered complete and
  Surgeon-centered storyboards from the final receipt.
