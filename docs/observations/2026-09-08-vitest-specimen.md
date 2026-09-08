# TypeScript + vitest specimen — the same stopwatch as the kaocha watch measurement

Measured 2026-09-08T23:26Z on forge@anvil (16 cores, 30 GiB). Method copied from
`2026-09-08-kaocha-watch/` and the captain's-log entry *"kaocha watch vs vitest, MEASURED"*:
sub-millisecond stamps applied to the runner's own stdout lines
(`perl -ne 'use Time::HiRes; printf "%.6f\t%s", Time::HiRes::time(), $_'`), saves performed
in place with `O_TRUNC` + `fsync`, 10 iterations red then green, median and p95 = max at n=10,
RSS read from `/proc/<pid>/status` after the last iteration. Stopwatch t0 is taken immediately
before the save; the verdict stamp is the reporter's own summary line (`Tests  N failed | ...`).

## Specimen

`remeda/remeda` @ `09afe15535be715c13c437a1d4d06b08ba4cdbcd` (shallow clone, GitHub, MIT).
Pure TypeScript, zero runtime dependencies, vitest **4.1.11**, node **v22.23.2** (`/usr/bin/node`,
system install already present — no user-local install was needed). Measured project is the
repo's own `runtime` vitest project (`src/**/*.test.ts`); the `types` and `prop` projects were
not run.

- **174 test files, 2,235 tests** (2,224 passed / 10 expected-fail / 1 todo).
- Deviation from the brief's "≈50–100 files, ≈500 tests": this is ~4x mvr's 579 tests. No
  well-known TS library with a vitest suite was found at exactly mvr's size (radash 9 files,
  hono 123 files / 2,814 tests, zod 199 files). remeda was chosen for shape fidelity — a pure
  logic suite with no DOM, no jsdom, no network — over exact test count. **Size turns out not to
  matter for the headline number**, because vitest never runs the whole suite on a change (below).
- Only the deps needed for the `runtime` project were installed
  (`vitest vite-tsconfig-paths @fast-check/vitest typescript @types/node type-fest@5.8.0`),
  not the repo's full devDependency set.

## Headline table — same columns as the kaocha table

| specimen | mode | save→verdict RED median (p95) | GREEN median (p95) | verdict covers | RSS |
|---|---|---:|---:|---|---:|
| remeda | cold `vitest run --project runtime` | 4.599 s | — | 174 files / 2,235 tests | — |
| remeda | `vitest --watch`, **test-file** edit | **0.222 s** (0.232) | **0.213 s** (0.221) | 1 file / 6 tests, both directions | 266 MiB |
| remeda | `vitest --watch`, **source-file** edit | 3.116 s (3.336) | 3.058 s (3.446) | 8 files / 84 tests (of 174 / 2,235) | 276 MiB |
| remeda | cold `vitest run <one file>` (probe stand-in) | 0.969 s | — | 1 file / 6 tests | — |

kaocha reference, for the same columns (from `2026-09-03-captains-log-anvil-seat.md`):

| specimen | mode | RED median | GREEN median | verdict covers | RSS |
|---|---|---:|---:|---|---:|
| mvr | cold `bin/kaocha` | 16.6 s | — | 579 / 7833 | — |
| mvr | `kaocha --watch` | 12.5 s | 0.040 s | red = **full suite**; green = the 1 failing test | 1.91 GiB |
| mvr | warm nREPL probe | 0.142 s | 0.144 s | 2 / 4 | 617 MiB |

## The learning, in one line

**vitest has the affected-test selection on the FIRST RED that kaocha's watcher lacks** — the
one thing the kaocha entry named as "worth building, as a plugin": editing a test file re-runs
that file alone in **0.222 s red / 0.213 s green**, versus kaocha's 12.5 s red (full suite) /
0.040 s green, so vitest's red is **56x faster than kaocha's watch red** and only 1.6x slower
than our warm nREPL probe (0.142 s) — while kaocha's *green* remains 5x faster than vitest's,
because a warm JVM re-running one deftest beats a Vite module-graph invalidation plus worker
dispatch. The asymmetry the kaocha run found (green fast, red slow) does not exist here: vitest
is flat in both directions, and the price of that flatness is a green that is 0.21 s instead of
0.04 s.

## One caveat

Different repo, different runtime, different box load, no plugins: remeda is 4x mvr in tests and
has no I/O or database in its suite, so this is a *pure-logic* comparison against a Clojure suite
that touches a database. No vitest plugins beyond the repo's own `vite-tsconfig-paths`. The
watcher was driven **without a TTY** (`--watch` passed explicitly); its latency under a real
interactive TTY reporter was not measured. Box load moved during the session (1-minute average
1.3–1.5 for the cold, B, C and sed runs; ~12 by the end of the first source-edit run), so both
watch rows were **re-measured at load 5.1 → 2.7** — see the replication below. Load did not
move them: the numbers are process-start and module-graph bound, not CPU bound at this load.

## Replication at a different box load

Both watch rows were re-run after the load spike, 5 iterations each, load average 5.11 falling to
2.74 across the two runs (versus 1.3 for the originals):

| row | original (n=10 / n=5) | replication (n=5) | delta |
|---|---:|---:|---:|
| watch, test-file edit, RED median | 0.222 s | 0.224 s | +0.9 % |
| watch, test-file edit, GREEN median | 0.213 s | 0.215 s | +0.9 % |
| watch, source-file edit, RED median | 3.116 s | 3.024 s | −3.0 % |
| watch, source-file edit, GREEN median | 3.058 s | 3.019 s | −1.3 % |

Same affected-set counts in both (`1 file` for the test-file edit, `2 failed | 6 passed (8)` for
the source-file edit). **A 4x swing in box load moved every number by under 3 %**, so the table
is not load-sensitive at this range and the source-edit row stands.

## C — is there an on-demand probe?

**No.** vitest has no warm out-of-band probe equivalent to `clj-nrepl-eval` → `kaocha.repl/run`:
there is no long-lived addressable process a second client can ask to run a subset. The watcher
owns its worker pool and answers only to file events (and to its own interactive `t`/`f`/`p`
filter keys on stdin, which is not a callable interface). `vitest run <file>` always pays a full
Vite + worker cold start.

Honest stand-in, measured cold, 3 runs: **0.969 / 0.982 / 0.969 s** to the verdict line
(process wall 1.040 / 1.051 / 1.043 s). That is **6.8x slower than our 0.142 s warm nREPL probe**
and 4.4x slower than vitest's own watcher answering the same edit — so on this stack the watcher
*is* the probe, and an agent that shells out to `vitest run <file>` per edit is paying ~0.8 s of
avoidable process start each time.

## D — watcher RSS after iteration 10

`vitest --watch` process group, from `/proc/<pid>/status` (VmRSS):

| run | total | node | npm | esbuild | perl+sh |
|---|---:|---:|---:|---:|---:|
| B (test-file edits, 10 iterations) | **266.3 MiB** | 159.9 MiB | 74.5 MiB | 21.2 MiB | 8.8 MiB |
| S (source-file edits, 5 iterations) | 275.7 MiB | 169.1 MiB | 75.1 MiB | 20.9 MiB | 8.7 MiB |

**7.3x lighter than kaocha's watcher on mvr (1.91 GiB)** and 2.3x lighter than the warm nREPL
image (617 MiB). Consistent with the kaocha finding that RSS is the cost of *running the suite*
rather than of watching: vitest never ran the whole suite after the initial pass.

## E — the sed -i test (does chokidar see an in-place sed?)

**YES — 10/10 fired**, at the same latency as the `O_TRUNC`+`fsync` saves.

| direction | n | median | p95 (max) | min |
|---|---:|---:|---:|---:|
| red (`sed -i s/toBe(6);/toBe(7);/`) | 5 | 0.213 s | 0.222 s | 0.211 s |
| green (`sed -i s/toBe(7);/toBe(6);/`) | 5 | 0.213 s | 0.216 s | 0.205 s |

This is the **opposite** of the kaocha result (5/5 NOFIRE — kaocha's directory watcher cannot see
`sed -i`, so a script patching with sed gets a silently stale green attributed to the wrong edit).
GNU sed `-i` writes a temporary file and renames it over the target; vite's chokidar watcher
watches the file's *directory* and fires on the rename, so the class of defect kaocha has here
does not exist on this stack. **The sed blindness is a kaocha property, not a watcher-architecture
property** — which strengthens the case that it is fixable in kaocha rather than inherent.

## F — bytes an agent must read to find file:line

vitest's watch-mode failure block, ANSI codes stripped:

| failing tests | block bytes | block lines | bytes to first `file:line` |
|---:|---:|---:|---:|
| 1 (test-file edit) | **936** | 33 | 424 |
| 7 (source-file edit, 2 files) | 4,572 | 151 | 856 |

For the single-failure case this is **inside our ~2 KB FAIL-record bound** with room to spare, and
`src/sum.test.ts:7:28` appears at byte 424 — under half a screen. The block is self-sufficient:
test path, describe/test names, the assertion diff (`- Expected / + Received`), the exact
`file:line:col`, and a 5-line source excerpt with a caret under the failing column. **The bytes
scale with the number of failures, not with suite size** (~650 bytes per additional failing test),
so the 2 KB bound holds for 1–2 failures and is exceeded from 3 failures upward; a FAIL record
capping at 2 KB would need to truncate by *failure count*, not by byte offset, to stay useful.

## Raw stopwatches

### A — cold `vitest run --project runtime`
```
t0	1788908978.607509
t1_exit	1788908983.253688
t_verdict_line	1788908983.206889
verdict	Test Files 174 passed (174) / Tests 2224 passed | 10 expected fail | 1 todo (2235)
wall_to_verdict_s	4.599
wall_process_s	4.646
```

### B — `vitest --watch`, test-file edit, 10x red/green (O_TRUNC+fsync saves)
```
iter	dir	delta_s	t_verdict	tests_line	files_line
initial	initial		1788909693.900028	Tests  2224 passed | 10 expected fail | 1 todo (2235)	Test Files  174 passed (174)
1	red	0.225	1788909696.126703	Tests  1 failed | 5 passed (6)	Test Files  1 failed (1)
1	green	0.211	1788909697.338675	Tests  6 passed (6)	Test Files  1 passed (1)
2	red	0.218	1788909698.561069	Tests  1 failed | 5 passed (6)	Test Files  1 failed (1)
2	green	0.221	1788909699.785101	Tests  6 passed (6)	Test Files  1 passed (1)
3	red	0.218	1788909701.004665	Tests  1 failed | 5 passed (6)	Test Files  1 failed (1)
3	green	0.210	1788909702.219324	Tests  6 passed (6)	Test Files  1 passed (1)
4	red	0.216	1788909703.437459	Tests  1 failed | 5 passed (6)	Test Files  1 failed (1)
4	green	0.212	1788909704.654826	Tests  6 passed (6)	Test Files  1 passed (1)
5	red	0.219	1788909705.877834	Tests  1 failed | 5 passed (6)	Test Files  1 failed (1)
5	green	0.220	1788909707.101769	Tests  6 passed (6)	Test Files  1 passed (1)
6	red	0.224	1788909708.329142	Tests  1 failed | 5 passed (6)	Test Files  1 failed (1)
6	green	0.214	1788909709.547062	Tests  6 passed (6)	Test Files  1 passed (1)
7	red	0.232	1788909710.783183	Tests  1 failed | 5 passed (6)	Test Files  1 failed (1)
7	green	0.221	1788909712.009027	Tests  6 passed (6)	Test Files  1 passed (1)
8	red	0.221	1788909713.232331	Tests  1 failed | 5 passed (6)	Test Files  1 failed (1)
8	green	0.214	1788909714.448535	Tests  6 passed (6)	Test Files  1 passed (1)
9	red	0.222	1788909715.674843	Tests  1 failed | 5 passed (6)	Test Files  1 failed (1)
9	green	0.212	1788909716.888988	Tests  6 passed (6)	Test Files  1 passed (1)
10	red	0.226	1788909718.115496	Tests  1 failed | 5 passed (6)	Test Files  1 failed (1)
10	green	0.210	1788909719.327744	Tests  6 passed (6)	Test Files  1 passed (1)
rss_total_kB	272668	[('2036138', 'sh', 2000), ('2036139', 'npm', 76240), ('2036140', 'perl', 6988), ('2036152', 'sh', 2004), ('2036153', 'node', 163760), ('2036161', 'esbuild', 21676)]
```

### S — `vitest --watch`, source-file edit (src/sum.ts), 5x red/green
```
iter	dir	delta_s	t_verdict	tests_line	files_line
initial	initial		1788909813.345066	Tests  2224 passed | 10 expected fail | 1 todo (2235)	Test Files  174 passed (174)
1	red	3.336	1788909818.681500	Tests  7 failed | 77 passed (84)	Test Files  2 failed | 6 passed (8)
1	green	3.446	1788909823.129346	Tests  84 passed (84)	Test Files  8 passed (8)
2	red	3.268	1788909827.402659	Tests  7 failed | 77 passed (84)	Test Files  2 failed | 6 passed (8)
2	green	3.058	1788909831.464166	Tests  84 passed (84)	Test Files  8 passed (8)
3	red	3.071	1788909835.536122	Tests  7 failed | 77 passed (84)	Test Files  2 failed | 6 passed (8)
3	green	3.074	1788909839.614619	Tests  84 passed (84)	Test Files  8 passed (8)
4	red	3.116	1788909843.734702	Tests  7 failed | 77 passed (84)	Test Files  2 failed | 6 passed (8)
4	green	3.015	1788909847.751438	Tests  84 passed (84)	Test Files  8 passed (8)
5	red	3.059	1788909851.815570	Tests  7 failed | 77 passed (84)	Test Files  2 failed | 6 passed (8)
5	green	3.052	1788909855.871267	Tests  84 passed (84)	Test Files  8 passed (8)
rss_total_kB	282284	[('2074864', 'sh', 1996), ('2074866', 'npm', 76868), ('2074867', 'perl', 6912), ('2074995', 'sh', 2004), ('2074997', 'node', 173148), ('2075084', 'esbuild', 21356)]
```

### E — `vitest --watch`, edits applied with `sed -i`, 5x red/green
```
iter	dir	delta_s	t_verdict	tests_line	files_line
initial	initial		1788909771.323221	Tests  2224 passed | 10 expected fail | 1 todo (2235)	Test Files  174 passed (174)
1	red	0.222	1788909773.548958	Tests  2 failed | 4 passed (6)	Test Files  1 failed (1)
1	green	0.213	1788909774.766976	Tests  6 passed (6)	Test Files  1 passed (1)
2	red	0.212	1788909775.982619	Tests  2 failed | 4 passed (6)	Test Files  1 failed (1)
2	green	0.213	1788909777.199600	Tests  6 passed (6)	Test Files  1 passed (1)
3	red	0.211	1788909778.415547	Tests  2 failed | 4 passed (6)	Test Files  1 failed (1)
3	green	0.208	1788909779.623718	Tests  6 passed (6)	Test Files  1 passed (1)
4	red	0.213	1788909780.840686	Tests  2 failed | 4 passed (6)	Test Files  1 failed (1)
4	green	0.205	1788909782.049730	Tests  6 passed (6)	Test Files  1 passed (1)
5	red	0.216	1788909783.267048	Tests  2 failed | 4 passed (6)	Test Files  1 failed (1)
5	green	0.216	1788909784.484969	Tests  6 passed (6)	Test Files  1 passed (1)
rss_total_kB	270112	[('2059614', 'sh', 1996), ('2059616', 'npm', 76840), ('2059617', 'perl', 6920), ('2059717', 'sh', 2004), ('2059718', 'node', 160712), ('2059808', 'esbuild', 21640)]
```

### C — cold `vitest run --project runtime src/sum.test.ts`, 3x
```
iter	t0	t_verdict	wall_to_verdict_s	wall_process_s
1	1788909741.312000	1788909742.281451	0.969	1.040
2	1788909742.373000	1788909743.355158	0.982	1.051
3	1788909743.443000	1788909744.411730	0.969	1.043
```

### F — the failure block as vitest prints it (1 failing test, ANSI stripped, 936 bytes)
```
 RERUN  src/sum.test.ts x1

 ❯ |runtime| src/sum.test.ts (6 tests | 1 failed) 7ms
     × should return the sum of numbers in an array 5ms

⎯⎯⎯⎯⎯⎯⎯ Failed Tests 1 ⎯⎯⎯⎯⎯⎯⎯

 FAIL  |runtime| src/sum.test.ts > dataFirst > should return the sum of numbers in an array
AssertionError: expected 6 to be 7 // Object.is equality

- Expected
+ Received

- 7
+ 6

 ❯ src/sum.test.ts:7:28
      5| describe("dataFirst", () => {
      6|   test("should return the sum of numbers in an array", () => {
      7|     expect(sum([1, 2, 3])).toBe(7);
       |                            ^
      8|     expect(sum([4, 5, 6])).toBe(15);
      9|     expect(sum([-1, 0, 1])).toBe(0);

⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯[1/1]⎯

 Test Files  1 failed (1)
      Tests  1 failed | 5 passed (6)
   Start at  23:21:36
   Duration  22ms
```

## Reproduction

Fixture (ephemeral, `/var/tmp/forge/vitest-fx`): shallow clone of remeda at the sha above,
`npm install --no-save --ignore-scripts vitest@^4.1.10 vite-tsconfig-paths@^6.1.1
@fast-check/vitest@^0.4.1 typescript@^6.0.2 @types/node@^26.4.0 type-fest@5.8.0` inside
`packages/remeda`, then the driver `bin/watchdrive.py` (starts `npx vitest --watch --project
runtime` with its stdout through the perl stamper, applies each edit, polls the stamped log for
the next reporter summary line, and reads the process-group RSS after the last iteration).

Two mechanical notes for whoever repeats this:

1. **`npx vitest` without a TTY does not watch.** The first attempt produced 10/10 NOFIRE
   because vitest defaults `watch` to `isTTY && !CI`, ran once, and exited — the driver was
   stopwatching a dead process. `--watch` must be passed explicitly. A NOFIRE row from a watcher
   that is not running looks exactly like a watcher that cannot see the edit; the driver should
   assert the watcher is alive before recording a NOFIRE.
2. **Match the verdict on the reporter's summary line only.** `Tests\s+\d` also matches vitest's
   `⎯⎯ Failed Tests 1 ⎯⎯` banner, which is printed *before* the run completes and yields a
   too-fast stamp. The anchor is `^\s*Tests\s{2,}\d` applied after stripping the timestamp column.

### S2 / B2 — replication after the load spike (5x red/green each)
```
load before S2: 5.11 6.89 4.82   after S2: 3.29 6.16 4.68   after B2: 2.74 5.85 4.61
-- S2 (source-file edit) --
iter	dir	delta_s	t_verdict	tests_line	files_line
initial	initial		1788910027.374798	Tests  2224 passed | 10 expected fail | 1 todo (2235)	Test Files  174 passed (174)
1	red	3.082	1788910032.458901	Tests  7 failed | 77 passed (84)	Test Files  2 failed | 6 passed (8)
1	green	3.019	1788910036.479775	Tests  84 passed (84)	Test Files  8 passed (8)
2	red	3.031	1788910040.514716	Tests  7 failed | 77 passed (84)	Test Files  2 failed | 6 passed (8)
2	green	3.018	1788910044.536355	Tests  84 passed (84)	Test Files  8 passed (8)
3	red	3.024	1788910048.563408	Tests  7 failed | 77 passed (84)	Test Files  2 failed | 6 passed (8)
3	green	3.024	1788910052.591863	Tests  84 passed (84)	Test Files  8 passed (8)
4	red	3.006	1788910056.601994	Tests  7 failed | 77 passed (84)	Test Files  2 failed | 6 passed (8)
4	green	3.011	1788910060.618012	Tests  84 passed (84)	Test Files  8 passed (8)
5	red	3.021	1788910064.641193	Tests  7 failed | 77 passed (84)	Test Files  2 failed | 6 passed (8)
5	green	3.027	1788910068.670322	Tests  84 passed (84)	Test Files  8 passed (8)
rss_total_kB	290332	[('2155483', 'sh', 1996), ('2155484', 'npm', 76800), ('2155485', 'perl', 7044), ('2155497', 'sh', 2000), ('2155498', 'node', 179392), ('2155511', 'esbuild', 23100)]
-- B2 (test-file edit) --
iter	dir	delta_s	t_verdict	tests_line	files_line
initial	initial		1788910075.102000	Tests  2224 passed | 10 expected fail | 1 todo (2235)	Test Files  174 passed (174)
1	red	0.243	1788910077.346386	Tests  1 failed | 5 passed (6)	Test Files  1 failed (1)
1	green	0.213	1788910078.561516	Tests  6 passed (6)	Test Files  1 passed (1)
2	red	0.225	1788910079.786774	Tests  1 failed | 5 passed (6)	Test Files  1 failed (1)
2	green	0.218	1788910081.007849	Tests  6 passed (6)	Test Files  1 passed (1)
3	red	0.224	1788910082.236919	Tests  1 failed | 5 passed (6)	Test Files  1 failed (1)
3	green	0.207	1788910083.447844	Tests  6 passed (6)	Test Files  1 passed (1)
4	red	0.218	1788910084.665967	Tests  1 failed | 5 passed (6)	Test Files  1 failed (1)
4	green	0.227	1788910085.897431	Tests  6 passed (6)	Test Files  1 passed (1)
5	red	0.218	1788910087.115571	Tests  1 failed | 5 passed (6)	Test Files  1 failed (1)
5	green	0.215	1788910088.334096	Tests  6 passed (6)	Test Files  1 passed (1)
rss_total_kB	271584	[('2169927', 'sh', 2000), ('2169929', 'npm', 77120), ('2169930', 'perl', 6992), ('2170022', 'sh', 2008), ('2170023', 'node', 159920), ('2170111', 'esbuild', 23544)]
```
