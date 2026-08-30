# Prepared-request proxy screen

This sealed experiment-only harness runs the ratified 4+4 efficacy and 2+2 read-only safety screens. It changes no product file and never installs or reloads clj-surgeon.

Zero-model apparatus checks:

```bash
cd bench/prepared_request_proxy_screen_20260830
python3 -B run_experiment.py self-test
python3 -B run_experiment.py freeze
```

`freeze` is one-shot. After independent review and explicit model-run authority, the remaining commands are:

```bash
python3 -B run_experiment.py preflight
python3 -B run_experiment.py cohort
python3 -B run_experiment.py archive
```

`preflight` launches fresh candidate-checkout HTTP MCP children against disposable fixture clones and checks both production mutation routes. It never contacts the shared port 7888. Each child uses an OS-assigned loopback port, bounded heap, no nREPL, and private logs/telemetry; preflight proves the child is reaped and its port closed. `cohort` is the only command that launches models and gives every slot the same private lifecycle. It runs the frozen four-attempt safety schedule first. Any mutation attempt or incomplete safety task hard-stops and records every remaining slot as not launched. Only a 4/4 green safety arm releases the frozen eight-attempt efficacy schedule. Every process start is retained; run directories are never replaced.

Routing gates use first completed successful mutation for comparability with the original causal screen. First attempted mutation route, construction refusals, post-refusal recovery actions/tool calls, and output-token medians are retained as descriptive diagnostics only; they cannot rescue a failed primary or safety gate.

The sibling complete-request replication is a separate experiment. This harness uses neither its checkout fixture nor its forced-inspection prompt or hardcoded future edit payload. The efficacy prompt contains no inspection, prepared-request, tool-name, or route cue. Safety uses a separate standalone `archive_status` repository. The treatment's whole-form null holes are deliberately product-faithful but may leave too much caller assembly; the verdict must disclose that weakness.
