# Preregistration repair addendum: config-validated cohort

Frozen after killed cohort commit `2236337c7922f219c53a3ed259086d360e7f7ce9` and before any executor-model call.

## Why a separate cohort is necessary

The original fixed schedule ran 12 `codex exec` processes. Strict config rejected `update_plan_enabled` before session creation. Every cell has no thread, provider usage, hook, tool call, or mutation. The original cohort remains killed and is not replaced.

This addendum registers a new cohort in `runs-repair/`. It is a separate experiment with the same task, predictions, schedule, sample, metrics, validity fields, kill criterion, and decision rule.

## Single mechanical repair

Remove exactly one line from both generated arm configs:

```toml
update_plan_enabled = false
```

Do not change another config field. Codex can expose its generic planning tool in both arms. That tool is non-mutating and matched across arms.

Before any model call, the repair runner must:

1. Generate both final arm configs.
2. Load each config with `codex debug models`.
3. Require exit code zero for both config loads.
4. Confirm that the resolved native model advertises `apply_patch_tool_type=freeform`.
5. Confirm that the resolved Surgeon model advertises `apply_patch_tool_type=null`.
6. Confirm that the N config has no MCP server.
7. Confirm that the S config enables only `edit_clojure` on a healthy isolated server.
8. Write hashes and command receipts to `repair-preflight/`.

## Cohort and inference

Run the unchanged schedule `N S S N / S N N S / N S S N`, with `n=6` per arm and no replacement. Use the predictions and gates committed in `preregistration.md`:

- point prediction: S uses 55% fewer action tokens, with a 45% minimum gate;
- point prediction: S is approximately 4 seconds and 20% faster, with minimum gates of 2.5 seconds and 12%;
- at least 5/6 valid episodes in each arm succeed in one mutation call.

The confirmatory label for this addendum applies only because no executor-model call occurred before this addendum. Report the original killed cohort separately. Do not combine its zero-call process times with the repair cohort.

The repair cohort still tests one warm-server, 137-line, six-occurrence fixture. Any claim about other task shapes remains an external-validity judgment, not a measured result.
