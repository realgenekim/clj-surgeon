# Ordinal refusal recovery screen

This is an experiment-only MCP proxy screen. It is not product code, does not
install or reload clj-surgeon, and does not relax the product refusal invariant
ratified for consideration in `6d558cb3`. The proxy deliberately exposes an
otherwise forbidden confirmation surface solely to measure caller behavior.

Run the stages in order:

```bash
cd bench/ordinal_refusal_screen_20260830
python3 -B -m unittest -v
python3 -B run_experiment.py self-test
python3 -B run_experiment.py freeze
python3 -B run_experiment.py pilot
python3 -B run_experiment.py cohort
python3 -B run_experiment.py archive
```

`freeze` is one-shot. The four-position pilot is not pooled with the main
cohort. The main schedule is fixed and interleaved, stops when each arm has at
least eight fully valid episodes, and retains every launched loss. Any
wrong-subject mutation hard-stops the experiment.

Raw model streams and proxy logs stay in `results/raw/` and are SHA-receipted
by the archive. Credentials are copied into a private per-episode Codex home,
removed after the process exits, and never enter the archive or Git.
