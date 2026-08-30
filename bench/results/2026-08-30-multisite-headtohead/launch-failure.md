# Registered cohort launch failure

The preregistered 12-cell cohort ran in the fixed schedule. Every `codex exec` process exited before session creation because strict configuration rejected `update_plan_enabled` as an unknown field.

All 12 cells remain in `runs/`. Each cell has:

- exit code 1;
- approximately 0.07–0.09 seconds of process wall time;
- no thread event;
- no provider usage event;
- no tool hook;
- no mutation call; and
- unchanged fixture bytes.

The registered confirmatory cohort therefore triggered its kill criterion. These cells are not latency or payload observations and are not replacements for model runs.

A separate repair cohort requires a committed addendum before any executor-model call. The only permitted configuration change is removal of the unsupported `update_plan_enabled = false` line. The generic planning tool can remain available in both arms as a matched non-mutation tool.
