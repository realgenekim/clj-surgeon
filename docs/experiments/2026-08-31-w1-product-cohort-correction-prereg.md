# W1 product cohort correction preregistration

Frozen after rejected Cohort 0 and before any Cohort 1 model episode.

## Why Cohort 0 is rejected

All 16 intended Cohort 0 Codex processes ran, but every mutation was refused by
the Codex client before reaching clj-surgeon:

```text
MCP tool call requires approval, but approval policy is never
```

The harness copied `default_tools_approval_mode="writes"` from the read-only
inspect benchmark. The repository's canonical write-capable MCP config uses
`default_tools_approval_mode="approve"`. Cohort 0 therefore measured a shared
client-configuration denial, not either product arm. No source tree reached the
expected after bytes; the server received no mutation; and the cohort is
inadmissible for wall or emission claims about W1.

Cohort 0 remains retained in full and will receive its own archive SHA. It is
not dropped, repaired, or included in Cohort 1 statistics.

## Cohort 1 frozen correction

Run a new complete 16-episode cohort with exactly one arm-independent execution
change:

```toml
default_tools_approval_mode = "approve"
```

Everything else remains frozen from the original preregistration:

- product SHA `05f5a1962e5a0c5aa0365c673994eca9024c1a44`;
- subscription executor `gpt-5.6-sol`, reasoning `high`;
- same isolated streamable-HTTP server rules and session holding;
- same task, before/after oracle, prompts, arm definitions, and tokenizer;
- same eight matched pairs and 16-row `C O O C` block schedule;
- same clocks, caller-emission definition, exactness gates, preview-value law,
  and analysis.

The runner also skips blank schedule lines during execution. This post-run
packaging defect occurred only after all Cohort 0 intended rows and caused no
model call; it does not change the schedule.

## Scorer clarification

Wrong-subject means that the caller selected a file or owner other than
`src/bench/pair_view.clj` / `route-event`. Failure to mutate, non-exact future
bytes, or a route refusal is scored in its own gate and is not mislabeled as a
wrong-subject event. Preview-to-commit adjustment counts as a candidate catch
only after a successful inert preview, never after a client-side refusal.

Cohort 1 is the only cohort eligible to answer the requested product question.

