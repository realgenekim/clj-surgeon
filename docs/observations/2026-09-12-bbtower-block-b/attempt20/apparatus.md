# Control apparatus receipts

The final control entrance is `python3 attempt20/measure.py` from the repository
root. It drives `measure_runner.clj` with six fresh processes per runtime and
namespace, serially. The runner delegates private temp/home setup to the
repository's `secure-tmpdir!` and measures only the complete test namespace,
after require. Process wall, argv, subject, start time and exit status are
separate JSONL fields. Every namespace receipt records its actual temp/home
properties. JVM and bb heaps are explicitly capped at 1 GiB.

Two preparatory harness failures are retained and excluded from policy:

- `argv-repair/`: the Python bb argv list was reused and extended. The second
  invocation therefore wrote the first output path again. The missing expected
  receipt stopped the driver. Its log and argv retain the error; the fix copies
  the base argv before appending arguments. The first bb receipt was overwritten
  by the second invocation, so that directory cannot furnish a valid six-run set.
- `isolation-repair/`: attempt8's raw namespace runner does not establish the
  private temp/home environment asserted by fast-lane-isolation-test. These
  tests failed on both runtimes. The driver was paused, its active JVM was
  allowed to exit, and only then was the driver terminated. Its final completed
  child receipt is retained even though the paused parent did not append its
  process row. None of these timings are used for policy. The replacement
  entrance uses the existing repository isolation setup rather than changing
  or suppressing a test.

Only `measurements/` and `measurement-processes.jsonl` belong to the restarted
isolated controls. `baseline.edn`, written by `freeze.clj` before source edits,
preserves the original paired set, portability and runtime assignments so that
`fold.clj` reproduces assignment flips after the manifest has changed.

The pending `.patch` files were prepared while controls ran. They do not change
the source snapshot until explicitly applied after measurement. No suite is
run concurrently with another suite, gate or measurement.
