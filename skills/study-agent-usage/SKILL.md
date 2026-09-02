---
name: study-agent-usage
description: Analyze recent Codex and Claude Code history plus clj-surgeon MCP, cclsp, and clojure-lsp telemetry when Gene asks to review agent usage, repeat an ethnographic study, compare callers, reconstruct tool routes, assess clj-surgeon adoption, or inspect behavior since the last study.
---

# Study Agent Usage

Produce an anonymized, evidence-backed comparison from one bounded receipt.
Do not rediscover either provider's history format by hand. The collector
classifies `:change` as planning and `:change!` / `:undo-change!` as structural
application, including Babashka launchers with options before `-m`.

## Collect once

From the clj-surgeon repository root, run:

```bash
make study-agent-usage
```

The collector reads the newest `agent-usage-window-end` marker under
`docs/observations/` as the lower bound. To override either boundary:

```bash
make study-agent-usage AGENT_USAGE_ARGS='--since 2026-08-05T00:00:00Z --until 2026-08-06T00:00:00Z'
```

The command prints a compact aggregate and writes the complete JSON receipt to
the reported temporary `receipt_path`. Pass `--receipt-out PATH` to choose the
location or `--full` only when a downstream program needs the complete receipt
on stdout. The complete receipt is the counting authority. It emits no
transcript prose, source bodies, raw service events, or workspace paths. It
reports hashed session keys, evidence filenames, skill visibility and loads,
CLI and MCP clj-surgeon operations, cclsp semantic reads, native Clojure
actions, tool payload sizes, direct tool wall, complete Codex turn wall, and
collapsed privacy-safe `route_phases` for each Codex task and provider session.

Codex task turns also contain an `event_clock`. It uses the client's completed
item clocks to distinguish measured model reasoning, model messages, MCP and
shell execution, file changes, collaboration, compaction, and unattributed
gaps. Render the longest Surgeon-using turns from an existing receipt with:

```bash
make study-agent-timeline RECEIPT=/tmp/receipt.json
```

Use `AGENT_TIMELINE_ARGS='--turn-key HASH --timeline-minimum-ms 0'` for one
complete turn, `--timeline-around-surgeon 3` for compact call-centered
storyboards, or `--all-turns` for the native control population. The renderer
reads only privacy-safe receipt fields. `model-reasoning` means Codex recorded
a completed Reasoning item; it does not expose hidden chain of thought.
`unattributed-gap` means no completed item owns that wall interval. It can
include inference, scheduling, prompt ingestion, serialization, transport,
logging, or UI delay. Never relabel all unattributed wall as model thinking.

Receipt schema v6 gives each completed clock item a privacy-safe action
ordinal. An `inspect_clojure` item also carries its request batch cardinality,
file and selector counts, operation counts, typed result outcome, a SHA-256
identity for the structural target with workspace paths and request bookkeeping
removed, and—when source-hash evidence was returned—a second SHA-256 over the
sorted source hashes. Adjacent reads receive only a categorical target relation:
exact, same files, overlapping files, disjoint files, or unknown. The receipt
never emits the target, workspace path, source, original source hash, request
ID, or expectation. Use these identities to shortlist repeated-read chains,
then inspect only the bounded receipt-named transcript region needed to judge
whether a later read was mechanically knowable earlier.

For structured MCP actions, schema v6 also retains canonical UTF-8 argument
byte count and a SHA-256 over the same arguments after replacing only the
top-level `workspace_root` with `<workspace>`. Surgeon results retain only
their canonical byte count. A post-Surgeon boundary copies those scalars and,
when the client recorded completed reasoning, measures the last completed
reasoning end to the next action start. It also records the clipped union of
background actions already overlapping the boundary. The digest is equality
evidence, not secret storage; the receipt never contains argument, result,
root, source, or reasoning content. Missing evidence is omitted, never
reported as zero. CLI arguments remain outside this byte law because their
quoting and truncation semantics are not comparable to structured MCP JSON.

Use the clock as a product microscope. Look for a repeated transition such as:

```text
model-reasoning -> surgeon-read -> model-reasoning -> surgeon-apply
```

The removable prize is usually a complete decision boundary, not milliseconds
inside the tool. A contract can win by returning the next guarded call,
compiling derivable facts internally, fusing exact verification, or supplying a
terminal mutation response. Preserve the model's authority over task scope.

The same receipt joins service telemetry for the exact time window:

- clj-surgeon MCP calls, outcomes, refusal types, request shapes, file reads,
  source-character volume, and wall distributions;
- cclsp MCP admissions when available;
- clojure-lsp method counts, completions, timeouts, document syncs, recoveries,
  and wall distributions as recorded by cclsp.

Old cclsp logs can predate durable MCP-admission events. Preserve that as a
coverage limit. Do not infer zero cclsp calls from a zero admission count when
agent route phases show cclsp use.

The Surgeon MCP telemetry section scans two roots by default and unions their
events (deduplicated by resolved absolute file path): the server's own default
(`~/.local/state/clj-surgeon/telemetry`, used whenever a launcher such as
`make mcp-serve` starts the server without `:telemetry-dir`) and
`$MCP_STATE_DIR/telemetry` (default `~/.local/state/clj-surgeon/mcp/telemetry`,
the Makefile launchd convention). `--surgeon-telemetry-root` still scans a
single explicit root when given. `services.clj_surgeon_mcp.status` is typed:
`"root-absent"` means neither root exists — never read this as zero calls;
`"no-events"` means a root exists but the window had no events; `"ok"` means
events were found. Both states list `roots_checked`, and the latter two also
list `roots_present`.

Read `route_phases` as the agent's keystroke sequence. Each phase contains only
behavioral kinds, action and Surgeon-call counts, input/output sizes, and wall:

```text
skill-load -> surgeon-read -> live-probe -> native-patch -> verify
```

Adjacent equivalent actions are collapsed. One outer action can carry several
kinds when it batches routes. Use these phases to locate turn amplification,
fallbacks, and duplicated discovery without reading raw commands.

When comparing CLI and MCP, separate capability from visibility. A CLI choice
is proven avoidable only when the operation had an MCP equivalent and the
caller had already used or discovered that MCP surface. Otherwise report it as
likely avoidable or a locally defensible fallback. Record whether the CLI was
under test, the MCP operation was absent, the session catalog was stale, or the
caller simply mixed transports. Do not infer intent from operation counts.

Stop on nonzero exit or a receipt status other than `ok`. Do not replace a
missing cutoff with a guessed date; supply `--since` from the prior study.

If one receipt-named transcript region proves that a real operation is absent
from the receipt, treat that as an analysis-tool field failure. Add a self-test,
repair the collector, and rerun the identical bounds once. Discard the
superseded receipt; never combine its counts with the corrected receipt. State
the instrumentation repair in the study.

## Interpret adversarially

Separate these stages:

```text
skill visible -> skill loaded -> binary invoked -> operation succeeded
```

Do not treat a mention, commentary promise, skill-list entry, or SKILL.md read
as a binary call. Do not treat mechanical tool success as semantic
correctness. Keep native Write as the expected control for new files and
prose-heavy work.

Assess breakthroughs as a ladder:

```text
capability implemented -> mechanism verified -> self-hosted ->
fresh caller succeeds -> controlled efficiency gate passes
```

Do not promote a self-hosting demonstration into an efficiency claim. Compare
complete task-turn wall, action count, context/output, correctness, and recovery
against the repository's explicit acceptance gate and a credible counterfactual.

Read narrow transcript regions only when the aggregate receipt cannot explain
a route, recovery, or failure. Never quote private project context. Replace
repositories and domains with neutral labels such as "service A" or "viewer
application."

When route phases still cannot explain intent, locate only the receipt's
`evidence_file`, constrain inspection to the receipt window and one task turn,
and read user goals plus tool-call transitions locally. Do not copy command
text, paths, URLs, account names, or source into the study. Record which local
tools were used for this final reconstruction.

## Write the study

Create a new file under `docs/observations/`; do not overwrite the prior
baseline. Include:

- exact UTC and Pacific window;
- sampling and exclusions;
- a Codex versus Claude scoreboard;
- actual structural operations and native fallbacks;
- dominant route-phase sequences and the task boundary they served;
- direct tool wall versus complete task-turn wall;
- successful behavior, failures, and counterfactual limits;
- progress against the repository's explicit product goals and acceptance gate;
- the smallest falsifiable improvement;
- the receipt's exact `next_marker` HTML comment.

Place the marker near the title only after the study is complete. It advances
the next default window, so never write it for an abandoned or partial study.

## Verify the paved road

Run:

```bash
make study-agent-usage-self-test
```

The self-test covers both transcript formats, marker discovery, skill
visibility versus loading, CLI and MCP route counting, native-action counting,
Surgeon and LSP service aggregation, completed-item event clocks, overlap-safe
coverage, and the privacy contract.
