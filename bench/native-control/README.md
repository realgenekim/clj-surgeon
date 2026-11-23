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
Arm text comes from `arms/arm1.md`, `arms/arm2.md`, `arms/arm3.md`.

```bash
bb bench/native-control/schedule.clj TASKS.edn arm1,arm2,arm3 2 42 > /var/tmp/forge/schedule.tsv
bench/native-control/run-schedule.sh TASKS.edn /var/tmp/forge/schedule.tsv
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
