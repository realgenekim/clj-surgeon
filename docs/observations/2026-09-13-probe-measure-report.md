# Probe versus native — preregistered measure, attempt 2: COMPLETE

Subject `759974c718889c52a05a1d0746c9c21d0f00dcdb` (tag `stable/2026-09-13.1`). Instrument `round5/measure.py`
sha256 `d26701e225fb5547b31a5245c880ee61394e5b24e1319f3e742759bd728e20ae` — equals amendment 1's "after" value. Manifest sha256
`eec5ff495c3079b8b5d5594cfde915a56352ef677b8489ae02ba58160f0a944d`; all six patch bytes verified against `SHA256SUMS`.
Run 2026-09-13 (UTC) on forge@anvil under an exclusive suite lease. Nothing was pushed,
tagged, committed or installed. Both worktrees left in place.

## Headline

| Stratum | NATIVE median ms | PROBE median ms | P/N | n per arm | Unknown cells |
|---|---:|---:|---:|---:|---:|
| bb-portable (T1,T2,T3) | 3,525.08 | 661.66 | **0.1877** | 9 | 0 of 18 |
| JVM-only (T4,T5,T6) | 4,209.75 | 1,169.67 | **0.2778** | 9 | 0 of 18 |
| NATIVE T1 controls | 3,106.66 | n/a | n/a | 6 | 0 of 6 |
| NATIVE T4 controls | 2,007.28 | n/a | n/a | 6 | 0 of 6 |
| Planted non-test (N1,N2) | 3,034.28 (amend. 2) | 260.46 | n/a | 2 | **2 of 4** — PROBE 2/2 typed, NATIVE 0/2 (settled by amendment 2) |

**One line of learning.** The warm probe beats a cold focused JVM run by 5.3x on
bb-portable and 3.6x on JVM-only medians, clears its variance floor by ~48x, holds
acceptance and first-attempt success at 6/6 in both arms, and repays its 16.1 s setup in
**6 invocations** — but the saving is not a constant JVM start: it ranged 1,562 ms (T4) to
3,479 ms (T5), so P/N is a property of the target namespace, not of the verb.

**One caveat.** Six namespaces on one idle box, against a warm image whose whole dependency
closure had already been loaded by the untimed admission pass; per-task P/N spans 0.1189 to
0.3118, so these stratum medians describe these six targets and say nothing about image
staleness risk, long-session memory cost, or a namespace whose reload closure is large.

## Bets

| Bettor | bb-portable P/N | JVM-only P/N | First-attempt success | Probe refusal rate |
|---|---|---|---|---|
| **Fable** | **HELD** — 0.1877 ≤ 0.50 | **HELD** — 0.2778 ≤ 0.30 | **HELD** — both 6/6 in reps 1,2,3 | **HELD** (amendment 2) — PROBE 2/2 typed; NATIVE 0/2, both `:test 0` exit 0 |
| **Astra** | **HELD** — 0.1877 ≤ 0.50 | **HELD** — 0.2778 ≤ 0.45 | **HELD** — equal on valid targets | **SPLIT** — equal-on-valid HELD (0/18 vs 0/18); higher-with-non-test HELD (amendment 2: PROBE 2/2 vs NATIVE 0/2) |

Fable's stricter JVM bound of 0.30 is the only column separating the two rows, and it
**held with 0.0222 of margin** (0.2778 against 0.30). Astra's prediction of a
smaller JVM advantage than Fable is not falsified — the JVM ratio is materially worse than
bb-portable (0.2778 vs 0.1877) — but it did not cross Fable's line.

**Why the refusal column is unsettled, and it is not the verb.** PROBE refused both planted
non-test targets with the typed kind `probe-target-not-a-test-namespace` (exits 1, 258.0 ms
and 262.9 ms), exactly 2/2 as Fable bet. The two NATIVE planted cells produced **no verdict
at all**: the frozen NATIVE expression calls `clojure.test/run-tests` without requiring
`clojure.test`, and a non-test namespace does not pull it in transitively, so both runs died
with `Syntax error (ClassNotFoundException) compiling at (REPL:1:38). clojure.test` and
printed no summary map. The instrument recorded `status: unknown`. NATIVE therefore issued
zero typed refusals, but zero refusals out of **zero attested cells** is not the bet's
"NATIVE 0/2". The preregistration governs: *"unknown telemetry leaves the bet unsettled"* and
*"no replacement, imputation, outlier deletion or claim from incomplete cells is allowed."*
The command was not repaired — it is frozen, and repairing it would have been improvisation.

## The 50% floor

Met. Both stratum ratios are at most 0.50; both clear the variance floor; acceptance and
first-attempt success are preserved; no stale green and no missed assertion.

| Gate | bb-portable | JVM-only |
|---|---:|---:|
| median(NATIVE) − median(PROBE) | 2,863.42 ms | 3,040.08 ms |
| 2 × sd(native controls) | 59.62 ms | 59.59 ms |
| Clears noise gate | YES (48x) | YES (51x) |
| sd NATIVE arm (9 runs) | 309.11 ms | 1,271.31 ms |
| sd PROBE arm (9 runs) | 154.24 ms | 430.46 ms |
| sd native controls (6 runs) | 29.81 ms | 29.79 ms |

The two control sample sds are near-identical (29.8104 vs 29.7946 ms) by coincidence on
distinct data, not by a folding error: the raw control walls are published in full below.

## Setup, amortized cost and break-even

| Item | Value |
|---|---:|
| Warm image startup (`make warm PORT=19066`, ready at `.clj-surgeon/probe.edn`) | 10,930.47 ms |
| Untimed admission probe, `clj-surgeon.forms-test` | 486.40 ms |
| Untimed admission probe, `clj-surgeon.analyze-test` | 764.29 ms |
| Untimed admission probe, `clj-surgeon.edit-dsl-test` | 714.95 ms |
| Untimed admission probe, `clj-surgeon.census-pool-test` | 533.73 ms |
| Untimed admission probe, `clj-surgeon.mcp-hot-verify-test` | 1,495.45 ms |
| Untimed admission probe, `clj-surgeon.mcp-formatter-test` | 1,189.84 ms |
| Admission total (6 probes) | 5,184.66 ms |
| **Total setup** | **16,115.13 ms** |
| Admission verdicts | 6/6 `:probe-passed`, 0 failures, exit 0 |

| Break-even (invocations to repay setup) | bb-portable | JVM-only | all 18 pairs |
|---|---:|---:|---:|
| Per-invocation saving (median) | 2,863.42 ms | 3,040.08 ms | 2,931.34 ms |
| vs startup only (10,930.47 ms) | 3.82 → **4** | 3.60 → **4** | **4** |
| vs startup + admission (16,115.13 ms) | 5.63 → **6** | 5.30 → **6** | **6** |

Across all six tasks pooled (18 pairs per arm): NATIVE median 3,601.40 ms,
PROBE median 670.06 ms, P/N 0.1861. Published as context only; the
preregistered primary statistic is per stratum.

## Per-task paired ratios (every pairing, every raw wall)

| Task | Stratum | Namespace | rep | NATIVE ms | PROBE ms | ratio | median ratio |
|---|---|---|---:|---:|---:|---:|---:|
| T1 | bb-portable | `clj-surgeon.forms-test` | 1 | 2,995.39 | 356.29 | 0.1189 | 0.1238 |
|  |  |  | 2 | 2,961.53 | 366.61 | 0.1238 |  |
|  |  |  | 3 | 2,961.52 | 370.93 | 0.1252 |  |
| T2 | bb-portable | `clj-surgeon.analyze-test` | 1 | 3,448.53 | 668.82 | 0.1939 | 0.1816 |
|  |  |  | 2 | 3,649.37 | 660.00 | 0.1809 |  |
|  |  |  | 3 | 3,644.46 | 661.66 | 0.1816 |  |
| T3 | bb-portable | `clj-surgeon.edit-dsl-test` | 1 | 3,607.33 | 693.80 | 0.1923 | 0.1923 |
|  |  |  | 2 | 3,595.47 | 671.29 | 0.1867 |  |
|  |  |  | 3 | 3,525.08 | 678.66 | 0.1925 |  |
| T4 | JVM-only | `clj-surgeon.census-pool-test` | 1 | 2,067.57 | 500.55 | 0.2421 | 0.2421 |
|  |  |  | 2 | 2,063.09 | 498.93 | 0.2418 |  |
|  |  |  | 3 | 2,101.28 | 539.08 | 0.2565 |  |
| T5 | JVM-only | `clj-surgeon.mcp-hot-verify-test` | 1 | 4,903.99 | 1,479.70 | 0.3017 | 0.3017 |
|  |  |  | 2 | 4,808.40 | 1,499.27 | 0.3118 |  |
|  |  |  | 3 | 4,959.06 | 1,479.90 | 0.2984 |  |
| T6 | JVM-only | `clj-surgeon.mcp-formatter-test` | 1 | 4,209.75 | 1,169.67 | 0.2778 | 0.2786 |
|  |  |  | 2 | 4,176.38 | 1,163.64 | 0.2786 |  |
|  |  |  | 3 | 4,233.86 | 1,182.82 | 0.2794 |  |

## Acceptance (frozen oracle, not self-report)

| Task | tests NATIVE/PROBE | assertions NATIVE/PROBE | unpatched assertions | delta | frozen delta | fail/error |
|---|---|---|---:|---:|---:|---|
| T1 | 24/24 | 95/95 | 94 | 1 | 1 | 0/0 |
| T2 | 19/19 | 63/63 | 63 | 0 | 0 | 0/0 |
| T3 | 28/28 | 420/420 | 419 | 1 | 1 | 0/0 |
| T4 | 3/3 | 9/9 | 8 | 1 | 1 | 0/0 |
| T5 | 20/20 | 158/158 | 158 | 0 | 0 | 0/0 |
| T6 | 5/5 | 39/39 | 37 | 2 | 2 | 0/0 |

All six assertion deltas equal the frozen manifest (1, 0, 1, 1, 0, 2). Test and assertion
counts are identical between arms for every task and every repetition. The instrument
independently verified each file's `after_sha256` inside the accepted verdict.

**Stale-result negative control (untimed, both arms).** One assertion in
`clj-surgeon.forms-test` was falsified in each owned checkout:

| Command | falsified | restored |
|---|---|---|
| NATIVE `clojure -M:clj-surgeon/test-deps …` | exit 1, `1 failures, 0 errors`, `{:test 24 :pass 93 :fail 1}` | exit 0, `0 failures`, `{:test 24 :pass 94 :fail 0}` |
| PROBE `clj-surgeon :probe :ns …` | exit 1, `:state :probe-failed`, `:failures 1` | exit 0, `:state :probe-passed`, `:failures 0` |

Both report failure, then pass after restoration. No stale green from the warm image.

## First-attempt success and refusal telemetry

| Repetition | NATIVE | PROBE | equal |
|---|---:|---:|---|
| 1 | 6/6 | 6/6 | YES |
| 2 | 6/6 | 6/6 | YES |
| 3 | 6/6 | 6/6 | YES |

Zero retries, zero repairs; every cell is a first call. No cell was relabelled.

| Denominator | NATIVE refused | PROBE refused | typed kinds |
|---|---|---|---|
| 18 valid-target first calls per arm | 0/18 | 0/18 | — |
| 2 planted non-test first calls per arm | 0 typed, **2 UNKNOWN** | **2/2 typed** | `probe-target-not-a-test-namespace` |

Denominators kept separate, as the document requires.

## Per-cell table (all 52 cells)

| Cell | status | exit | typed kind | complete wall ms | command-only ms |
|---|---|---:|---|---:|---:|
| `T1-NATIVE-1` | accepted | 0 | — | 2,995.39 | 2,982.19 |
| `T1-PROBE-1` | accepted | 0 | — | 356.29 | 343.86 |
| `T1-NATIVE-2` | accepted | 0 | — | 2,961.53 | 2,947.27 |
| `T1-PROBE-2` | accepted | 0 | — | 366.61 | 351.43 |
| `T1-NATIVE-3` | accepted | 0 | — | 2,961.52 | 2,946.61 |
| `T1-PROBE-3` | accepted | 0 | — | 370.93 | 355.98 |
| `T2-NATIVE-1` | accepted | 0 | — | 3,448.53 | 3,434.00 |
| `T2-PROBE-1` | accepted | 0 | — | 668.82 | 655.35 |
| `T2-NATIVE-2` | accepted | 0 | — | 3,649.37 | 3,636.40 |
| `T2-PROBE-2` | accepted | 0 | — | 660.00 | 646.25 |
| `T2-NATIVE-3` | accepted | 0 | — | 3,644.46 | 3,630.83 |
| `T2-PROBE-3` | accepted | 0 | — | 661.66 | 647.29 |
| `T3-NATIVE-1` | accepted | 0 | — | 3,607.33 | 3,593.01 |
| `T3-PROBE-1` | accepted | 0 | — | 693.80 | 679.31 |
| `T3-NATIVE-2` | accepted | 0 | — | 3,595.47 | 3,581.68 |
| `T3-PROBE-2` | accepted | 0 | — | 671.29 | 656.34 |
| `T3-NATIVE-3` | accepted | 0 | — | 3,525.08 | 3,510.55 |
| `T3-PROBE-3` | accepted | 0 | — | 678.66 | 664.68 |
| `T4-NATIVE-1` | accepted | 0 | — | 2,067.57 | 2,054.36 |
| `T4-PROBE-1` | accepted | 0 | — | 500.55 | 486.71 |
| `T4-NATIVE-2` | accepted | 0 | — | 2,063.09 | 2,049.82 |
| `T4-PROBE-2` | accepted | 0 | — | 498.93 | 486.26 |
| `T4-NATIVE-3` | accepted | 0 | — | 2,101.28 | 2,087.46 |
| `T4-PROBE-3` | accepted | 0 | — | 539.08 | 524.32 |
| `T5-NATIVE-1` | accepted | 0 | — | 4,903.99 | 4,889.37 |
| `T5-PROBE-1` | accepted | 0 | — | 1,479.70 | 1,465.22 |
| `T5-NATIVE-2` | accepted | 0 | — | 4,808.40 | 4,794.32 |
| `T5-PROBE-2` | accepted | 0 | — | 1,499.27 | 1,486.39 |
| `T5-NATIVE-3` | accepted | 0 | — | 4,959.06 | 4,944.22 |
| `T5-PROBE-3` | accepted | 0 | — | 1,479.90 | 1,466.27 |
| `T6-NATIVE-1` | accepted | 0 | — | 4,209.75 | 4,195.59 |
| `T6-PROBE-1` | accepted | 0 | — | 1,169.67 | 1,154.86 |
| `T6-NATIVE-2` | accepted | 0 | — | 4,176.38 | 4,161.95 |
| `T6-PROBE-2` | accepted | 0 | — | 1,163.64 | 1,148.48 |
| `T6-NATIVE-3` | accepted | 0 | — | 4,233.86 | 4,219.73 |
| `T6-PROBE-3` | accepted | 0 | — | 1,182.82 | 1,167.23 |
| `N1-NATIVE-1` | unknown | — | — | 3,205.35 | — |
| `N1-PROBE-1` | refused | 1 | probe-target-not-a-test-namespace | 258.04 | 247.12 |
| `N2-NATIVE-1` | unknown | — | — | 3,253.85 | — |
| `N2-PROBE-1` | refused | 1 | probe-target-not-a-test-namespace | 262.89 | 251.61 |
| `control-T1-NATIVE-1` | accepted | 0 | — | 3,172.39 | 3,157.41 |
| `control-T1-NATIVE-2` | accepted | 0 | — | 3,103.62 | 3,090.10 |
| `control-T1-NATIVE-3` | accepted | 0 | — | 3,105.06 | 3,091.36 |
| `control-T1-NATIVE-4` | accepted | 0 | — | 3,090.49 | 3,076.66 |
| `control-T1-NATIVE-5` | accepted | 0 | — | 3,108.27 | 3,094.12 |
| `control-T1-NATIVE-6` | accepted | 0 | — | 3,134.29 | 3,120.32 |
| `control-T4-NATIVE-1` | accepted | 0 | — | 2,000.85 | 1,987.23 |
| `control-T4-NATIVE-2` | accepted | 0 | — | 1,999.62 | 1,984.94 |
| `control-T4-NATIVE-3` | accepted | 0 | — | 1,965.32 | 1,951.25 |
| `control-T4-NATIVE-4` | accepted | 0 | — | 2,053.77 | 2,039.68 |
| `control-T4-NATIVE-5` | accepted | 0 | — | 2,013.72 | 1,999.10 |
| `control-T4-NATIVE-6` | accepted | 0 | — | 2,027.93 | 2,014.26 |

## Warm image identity

| Field | Value |
|---|---|
| Root | `/var/tmp/forge/probe-measure-fx/subject-probe` |
| HEAD | `759974c718889c52a05a1d0746c9c21d0f00dcdb` |
| Port | 19066 (never a reserved shared port; 7906 untouched) |
| java PID | 1251694 (stopped by exact PID at the end; port 19066 confirmed closed) |
| Generation | `e8f3a0d5-1303-468d-98f4-8b1c0beeff12` |
| Fingerprint | `3e5a5565a6814223974419f7b0f909a45078bcc15a43b8d319b23528cb7ca7f1` |
| Classpath sha256 | `2cb8f24c513991b2f493fa5c5fe5ec3ef1c90028f34e24a76bf30c7ad3d081f2` |
| Full argv | `results-2/setup/warm-argv.txt` |
| `.clj-surgeon/probe.edn` | `results-2/setup/probe.edn` |

One generation served every PROBE cell; no identity file changed mid-run, so no cell was
invalidated on that ground.

## Apparatus notes — every deviation, verbatim

1. **The frozen NATIVE command cannot execute a non-test namespace, and this is the run's
   only unknown-cell cause.** The document's NATIVE expression is
   `"(require '$task_ns) (let [r (clojure.test/run-tests '$task_ns)] …)"`. For a test
   namespace, requiring it loads `clojure.test` transitively; for `clj-surgeon.forms` and
   `clj-surgeon.analyze` it does not, so both planted NATIVE cells raised
   `Syntax error (ClassNotFoundException) compiling at (REPL:1:38). clojure.test` and wrote
   no summary map. The instrument caught it at `assert isinstance(receipt, dict)` and wrote
   `status: "unknown"`, `error: "missing verdict is unknown"`. Complete walls were still
   recorded (3,205.35 ms and 3,253.85 ms) and are retained, but they certify nothing. I did
   not repair the command; it is frozen and the document forbids substitution.
2. **`measure.py init` was run from `subject-native`** (clean, at the subject) into the fresh
   `results-2/`, as amendment 1 requires. The STOP run's `results/subject.json` was neither
   reused nor overwritten.
3. **A driver script (`drive.py`) sequenced the cells.** It contains no cell logic: for each
   scheduled cell it asserts the target file's sha256 equals `before_sha256`, invokes the
   frozen `measure.py cell` unmodified, then restores with `git checkout -- <file>` and
   asserts `before_sha256` again. Every assertion passed for all 52 cells; no restore failed.
4. **The six untimed admission probes used the document's own shell function verbatim**,
   which passes `-Djava.io.tmpdir` but no `-Xmx`. Every JVM that produced a measured number
   ran at `-Xmx1g`: the warm image (`clojure -J-Xmx1g`, per `Makefile:373`), the NATIVE arm
   (`clojure -J-Xmx1024m`, in the instrument) and the PROBE client (`bb -Xmx1g`, in the
   instrument). `JAVA_TOOL_OPTIONS=-Xmx1024m …` was exported for every command.
5. **"Separate fresh agent sessions per arm" was not applicable and is recorded as such.**
   The frozen patches are applied mechanically by the instrument, not authored by a model, so
   no agent session influenced any measured number. `model` and `effort` in `subject.json`
   are `claude-opus-5[1m]` and `default`; `effort` is the runner operator's declared value
   and is not externally attested.
6. **An untimed unpatched baseline was run per namespace after all measured cells**, through
   the PROBE client in `subject-probe`, purely to compute the assertion deltas the document
   requires for acceptance. It is not a cell, is outside the matched order, and the checkout
   was verified clean afterwards. Receipts: `results-2/setup/assertion-delta/`.
7. **Exactly one arm was ever active; cells ran strictly sequentially in the document's
   matched order** — reps 1 (T1–T6, then N1/N2), 2 (T3–T6, T1–T2), 3 (T5–T6, T1–T4), with
   arm orders N/P,P/N,N/P for odd tasks and P/N,N/P,P/N for even tasks. The twelve NATIVE
   controls ran before the 36 matched cells, as "before comparing arms" requires.
8. **No `pkill`, no `pgrep -f` against a measured process, no outer `timeout`.** The warm
   image was stopped by its exact PID (1251694) and port 19066 verified closed. An exclusive
   `flock` on `/home/forge/tmp/suite-1.lock` was held for the whole run by a holder this seat
   owned (sleep PID 1229720) and released by exact PID; the lock was verified free afterwards.
9. **Stratum classifications were attested from the frozen subject before running**, against
   `test/clj_surgeon/lane_manifest.clj` `def portability-runtimes`: `forms-test`,
   `analyze-test`, `edit-dsl-test` are `:bb`; `census-pool-test`, `mcp-hot-verify-test`,
   `mcp-formatter-test` are `:jvm`. This agrees with the manifest strata for all six. All six
   owner deftests are present in `deftest_census.edn`. Both planted sources hash to the
   values in amendment 1 table (e) and contain no `deftest` form. Receipt:
   `results-2/setup/stratum-attestation.txt`.
10. **`subject-probe` retains the untracked `.clj-surgeon/` directory** created by the
    prescribed `make warm`, admitted for the PROBE arm exactly as amendment 1 (b) specifies.
    The stale one left by attempt 1 was removed before starting; both worktrees were verified
    at `759974c7` and clean before init, and are clean and in place now.
11. **No measured cell used the shared installed `~/bin/clj-surgeon`.** Ports 7888, 7890,
    7894, 7895, 7906 and 8300–8339 were never contacted; 7906 was confirmed still listening,
    untouched, at the end. Nothing was pushed, tagged, committed or installed, and no
    `clj-surgeon-bbtower` or `-datacode` working tree was modified.

## Retained evidence

Everything is under `/var/tmp/forge/probe-measure-fx/results-2/`: `subject.json`,
`summary.json` (every number in this report), `cells.jsonl`, 52 cell directories each with
`request.json`, `verdict.json`, `timing.json`, `stdout`, `stderr`, `subject.diff` (and
`probe-identity.edn` for PROBE cells), `driver/` (runner stdout/stderr per cell), and
`setup/` (warm image identity and argv, admission receipts, stale-result control receipts,
assertion-delta baselines, stratum attestation). The verified instrument copy is
`/var/tmp/forge/probe-measure-fx/instrument-2/`.

## Amendment 2 rerun — 2026-09-13 05:53:16 UTC — the refusal column settles

Preregistration amendment 2 (records 47f7f88c) repaired the instrument defect: the frozen
NATIVE expression now requires `clojure.test` before the task namespace. Only the two
planted NATIVE cells reran, from `instrument-3/measure.py`
(sha256 `e58dcc47503ce044df47955caddf5009b4b813fc1603396c6fec0b00db4f8230`, a one-line
change from instrument-2), into the fresh `results-3/` after a fresh `init` from the clean
subject `759974c7`. The 36 valid-target cells and both PROBE planted cells are untouched.
Ran after the data-not-code landing's battery finished; no other suite was running.

| Cell | Status | Typed kind | Summary | Exit | Wall ms |
|---|---|---|---|---|---:|
| N1-NATIVE-1 (`clj-surgeon.forms`) | accepted | none | `{:test 0, :pass 0, :fail 0, :error 0}` | 0 | 3,029.93 |
| N2-NATIVE-1 (`clj-surgeon.analyze`) | accepted | none | `{:test 0, :pass 0, :fail 0, :error 0}` | 0 | 3,028.63 |

NATIVE issued zero typed refusals on both non-test targets and reported a zero-test run as
success. PROBE (results-2, unchanged) refused both with `probe-target-not-a-test-namespace`.

- **Fable's refusal bet — HELD.** Exactly PROBE 2/2 typed refusals and NATIVE 0/2.
- **Astra's refusal bet — HELD on both arms.** Equal on the six valid targets (0/18 vs 0/18,
  unchanged) and higher when the non-test safety controls are included (2/2 vs 0/2).
- **What the column says about the product.** A cold focused run over a non-test namespace
  is a green that certifies nothing (zero tests, exit 0); the probe verb refuses it before a
  JVM is touched. That is the "false green terminates investigation" class from the house
  rules, closed at the verb boundary.
- The two walls (about 3.03 s each) are recorded and enter no ratio.

Evidence: `2026-09-13-probe-measure-report/amendment2/` (request, verdict, timing, stdout,
stderr per cell; `subject.json`; the instrument copy).
