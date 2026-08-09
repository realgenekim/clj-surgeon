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

The same receipt joins service telemetry for the exact time window:

- clj-surgeon MCP calls, outcomes, refusal types, request shapes, file reads,
  source-character volume, and wall distributions;
- cclsp MCP admissions when available;
- clojure-lsp method counts, completions, timeouts, document syncs, recoveries,
  and wall distributions as recorded by cclsp.

Old cclsp logs can predate durable MCP-admission events. Preserve that as a
coverage limit. Do not infer zero cclsp calls from a zero admission count when
agent route phases show cclsp use.

Read `route_phases` as the agent's keystroke sequence. Each phase contains only
behavioral kinds, action and Surgeon-call counts, input/output sizes, and wall:

```text
skill-load -> surgeon-read -> live-probe -> native-patch -> verify
```

Adjacent equivalent actions are collapsed. One outer action can carry several
kinds when it batches routes. Use these phases to locate turn amplification,
fallbacks, and duplicated discovery without reading raw commands.

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
Surgeon and LSP service aggregation, and the privacy contract.
