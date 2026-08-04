# Make Benchmark Result Directories Single-Writer

**Status:** Resolved in worktree
**Severity:** P1 evidence integrity

## Evidence

`/private/tmp/clj-surgeon-xray-tree-seq-pilot-20260804` was corrupted when a
resume run started before the original background execution had exited. The
runner protects individual `runs.tsv` appends with a directory lock, but it
does not own the whole result directory for the lifetime of a benchmark. A
stale row lock can also wait forever if a writer dies.

## Required Outcome

Give each result directory one run-level owner. Refuse a second live writer,
record owner PID/start/command metadata, and make stale ownership recovery
explicit. Bound row-lock acquisition and preserve every completed child
receipt even when another child or the summarizer fails.

Parallel children inside one owner remain required; serializing the benchmark
is not the fix.

## Tests and Verification

- A self-test proves a concurrent second runner refuses before writing.
- A stale-owner fixture produces an actionable recovery receipt.
- A killed row writer cannot cause an infinite wait.
- Resume skips complete rows and never duplicates or truncates them.
- Summary generation starts only after all scheduled children have terminal
  receipts.

## Done When

It is impossible to corrupt a result directory by launching resume too early,
and every wait has a bounded, diagnosable failure mode.

## Resolution

`bench/run_clean_codex.sh` now atomically owns each result directory for the
runner lifetime and records PID, host, process-start, UTC-start, and command
metadata. A second live writer refuses before benchmark artifacts change.
Stale or unverifiable ownership also refuses unless
`BENCH_RECOVER_STALE_OWNER=true` is supplied; recovery preserves the old lock
as a timestamped receipt and is intended to be paired with `BENCH_RESUME=true`.

Row-lock acquisition has a configurable finite timeout, and appends recheck
the run ID while holding the lock so a resumed or duplicate schedule cannot add
a second row. Every child receives an atomic terminal receipt. The supervisor
reaps all children even after a failure, verifies all receipts, and starts the
summarizer only when they are present. Summary output uses a temporary file and
atomic rename, preserving an existing summary if generation fails.

`make benchmark-harness-self-test` proves live-owner refusal, explicit stale
recovery with a preserved receipt, bounded killed-row-writer failure,
duplicate-free resume appends, and native-control executable isolation without
making model calls. The same self-test runs under `make test`.
