# Replay

Use Python 3.10 with `tiktoken` 0.9.0 and SciPy 1.10.0.

Verify the committed raw receipts and recompute the aggregate without model calls:

```sh
cd /path/to/clj-surgeon
/Users/genekim/anaconda3/bin/python \
  bench/results/2026-08-30-multisite-headtohead/run_experiment.py verify
```

Reproduce the measured cohort from the preregistration commit in a fresh worktree. This command uses ChatGPT subscription authentication and removes `OPENAI_*` variables from every child process:

```sh
/Users/genekim/anaconda3/bin/python \
  bench/results/2026-08-30-multisite-headtohead/run_experiment.py preflight
git add bench/results/2026-08-30-multisite-headtohead
git commit -m "Preregister multi-site mutation head-to-head"
/Users/genekim/anaconda3/bin/python \
  bench/results/2026-08-30-multisite-headtohead/run_experiment.py run
```

The runner refuses to replace an existing preflight or episode directory. Use a fresh worktree for a new cohort.
