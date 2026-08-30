# Preregistration addendum: exec-validated cohort

Frozen after killed cohort commit `d52d29c4577e3e0506ec0b939315be2c82ef5a06`
and before any executor-model call.

## Prior launches

The original cohort and the config-repair cohort remain killed and retained.
Both failed during strict config loading. Across the 24 scheduled processes,
there was no model event, tool hook, provider usage, last message, or fixture
mutation. Those process times and zero payloads do not enter this cohort.

## Final mechanical config repair

Keep the first repair, which removes this obsolete field:

```toml
update_plan_enabled = false
```

Also remove this rejected `tools` table from both arm configs:

```toml
[tools]
view_image = false
web_search = false
```

No task, model, reasoning effort, catalog, mutation-tool routing, hook, MCP
setting, schedule, sample, metric, validity field, prediction, gate, or kill
criterion changes. `view_image` and `web_search` are not mutation routes.
Top-level web search remains disabled and the shell feature remains disabled.

## Strong no-model preflight

Before any executor call, the final runner must validate each exact arm config
through `codex exec --strict-config`. For this preflight only, a command-line
override selects the deliberately nonexistent provider
`__multisite_preflight_no_provider__`. A valid preflight must:

1. pass complete strict config parsing;
2. stop at the missing-provider error;
3. produce no completed model turn or tool hook;
4. resolve native `apply_patch_tool_type=freeform` and Surgeon
   `apply_patch_tool_type=null` in `codex debug models`;
5. confirm that N has no MCP server; and
6. confirm that S exposes only `edit_clojure` on a healthy isolated server.

Store the commands, output, configs, and hashes in `exec-valid-preflight/`.

## Cohort and inference

Run the unchanged schedule `N S S N / S N N S / N S S N`, with `n=6` per arm
and no replacement. Use the predictions, gates, kill criterion, and power bound
committed in `preregistration.md`. Store the cohort separately in
`runs-exec-valid/`.

The confirmatory label applies because the two prior launch failures exposed no
executor output or outcome data. The measured scope remains one warm-server,
137-line, six-occurrence fixture. Any claim about another task shape remains an
external-validity judgment.
