# Exec-valid cohort replay

Use Python 3.10 with `tiktoken` 0.9.0 and SciPy 1.10.0.

## Verify without model calls

From the repository root, recompute the confirmatory aggregate from all 12
episode scores and verify its 213-file SHA-256 manifest:

```sh
/Users/genekim/anaconda3/bin/python \
  bench/results/2026-08-30-multisite-headtohead/run_exec_valid.py verify
```

Verify the separately retained config-repair launch cohort:

```sh
/Users/genekim/anaconda3/bin/python \
  bench/results/2026-08-30-multisite-headtohead/run_repair.py verify
```

Verify the original killed launch cohort directly from its manifest:

```sh
cd bench/results/2026-08-30-multisite-headtohead
shasum -a 256 -c SHA256SUMS
```

The original `run_experiment.py verify` compares equivalent JSON objects using
insertion-order-sensitive strings. Use the manifest command above for that
cohort. The final and repair verifiers compare decoded objects.

## Reproduce the confirmatory cohort

Create a fresh worktree at the immutable preregistration boundary, then run the
fixed schedule:

```sh
git worktree add /private/tmp/clj-surgeon-multisite-replay \
  fef881231a44561624084a7b35e87919270182e7
cd /private/tmp/clj-surgeon-multisite-replay
/Users/genekim/anaconda3/bin/python \
  bench/results/2026-08-30-multisite-headtohead/run_exec_valid.py run
```

This uses ChatGPT subscription authentication and removes every `OPENAI_*`
variable from each child process. The committed preflight already validated the
exact configs through `codex exec --strict-config` without a model request.

The runner refuses to replace an existing episode directory. Use a fresh
worktree for a new cohort. Reproduction consumes 12 new subscription sessions
and can vary because it resamples the model.
