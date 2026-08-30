# Prepared-request replication replay

This directory is a sealed, one-shot n=10/arm Codex routing experiment. Run it
only in a fresh checkout/copy on Anvil `dev-a`, using ChatGPT subscription auth,
with no `OPENAI_API_KEY` present. It never installs or reloads shared software.

Before launch, use raw tmux keystrokes on the exact `dev-a` Codex pane for
`/status`; do not send slash commands through Agent Bridge. Record model
`gpt-5.6-sol`, reasoning `high`, account plan, and weekly headroom in
`quota-preflight.json`. Agent Bridge carries the durable execution assignment.

Exact replay:

```bash
cd bench/prepared_request_replication_20260830
python3 run_experiment.py self-test
python3 run_experiment.py freeze
python3 run_experiment.py preflight
python3 run_experiment.py cohort
python3 run_experiment.py archive
```

`replay.sh` runs the same sequence. The freeze and cohort are deliberately
one-shot. Every started attempt remains a denominator observation; never delete
or replace a run directory. `archive` may be repeated and is deterministic for
unchanged retained inputs except for the separately written receipt timestamp.

Expected outputs:

- `freeze.json` and `preflight/preflight.json`;
- `attempts.jsonl` and 20 `runs/*/events.jsonl` raw streams;
- per-run proxy logs, stderr, diffs, final sources, tests, and scores;
- `aggregate.json` with Wilson/Newcombe intervals and the frozen verdict; and
- `archives/prepared-request-replication-20260830.tar.gz`, its SHA-256 receipt,
  and `artifact-manifest.sha256`.
