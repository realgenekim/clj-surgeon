# Native read controls

One frozen read task per fresh Codex context, in an archived pinned repository.
Measures command actions, tool-output characters, Surgeon calls/refusals, tokens,
wall milliseconds and final answers. Scoring sees only task and answer, never arm.
Class/arm medians compare arm3 with the best of arm1/arm2, and arm2 with arm1.
Promotion requires >=30% fewer tool characters, no extra actions, and no loss
in correct answers (counts, so use balanced schedules). Missing/zero controls refuse.

TASKS.edn is a vector of maps with string `:id`, `:repo` (absolute), `:sha`
(pinned commit), `:class`, `:prompt`, `:answer-format`, `:answer-kind` (`:exact`
or `:set`) and `:expected` (string or vector of strings, respectively).
Arm text comes from `arms/arm1.md` to `arms/arm4.md` (arm4: arm3 plus routing by where the answer sits).

For one run, or a generated schedule:

```bash
bb bench/native-control/run.clj run TASKS.edn RUN_ID TASK_ID ARM REP
bb bench/native-control/schedule.clj TASKS.edn arm1,arm2,arm3,arm4 2 42 > SCHEDULE.tsv
bb bench/native-control/run.clj schedule TASKS.edn SCHEDULE.tsv
bb bench/native-control/summarize.clj TASKS.edn /var/tmp/forge/native-control
make benchmark-native-control-self-test
```

Environment defaults: `NC_RESULT_ROOT=/var/tmp/forge/native-control`,
`NC_MODEL=gpt-6-astra`, `NC_REASONING=medium`, `NC_TIMEOUT_S=900`,
`NC_SURGEON_BB_CONFIG` = this checkout's absolute bb.edn,
`NC_CODEX_AUTH=~/.codex/auth.json`, `NC_KEEP=0` (1 retains workspace/home).
Raw `runs/RUN_ID/{run.json,events.jsonl,answer.txt,stderr.txt}` and generated
`rows.tsv`/`table.md` live outside the repository; never commit them.
Completed run.json skips retries; nonzero exits are recorded, not retried.
Review exit-code, tree-changed and skipped-lines in rows.tsv before interpreting
verdicts; the requested promotion rule does not add validity gates.
The arm AGENTS.md is included in the clean baseline before execution.
TSV cells escape backslashes, tabs and newlines; answer.txt overrides event text.

Linux only. Identifiers are validated as path components before any run filesystem call.
Cleanup runs in try/finally; setup or preflight failures leave `failure.json` and remove
workspace and credential copy unless `NC_KEEP=1`.
`run` exits with codex's exit code (2 for a refusal before anything ran, 3 for a failure after);
`schedule` exits 0 only when every run exited 0, else 1; a refusal before any run is 2.
The credential is copied as 0600 inside a 0700 home. The sandbox defaults to `workspace-write`,
with two extra writable roots only for wrapper arms, both in the INVOKER's home:
`~/.local/state/clj-surgeon` and `~/.local/state/diagnose-skiff-cpu-memory`.
The tree comparison includes ignored files, so any remaining write is `tree_changed`.
Every arm's PATH must carry `rg`. Schedules read tasks once and validate all rows and task ids before running,
close process stdin, continue after failures, and print total/failed counts.
