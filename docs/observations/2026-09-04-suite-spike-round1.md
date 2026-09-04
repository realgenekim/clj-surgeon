# Suite spike, round one — measure and classify (2026-09-04, forge@anvil)

Spec: `docs/observations/2026-09-04-suite-spike-spec.md`. Branch `bridge/suite-spike`, base
`e8090624`. Round one is read-only on `src/` and `test/`; the harness lives under
`dev/experiments/` (`suite_timing.clj`, `suite_classify.clj`, `suite_report.clj`) and the
`:clj-surgeon/mcp-test` alias was driven without editing `deps.edn`.

## The table

`clojure -M:clj-surgeon/mcp-test`, one JVM, production namespace order read from
`test/clj_surgeon/mcp_test_runner.clj`. **716.7 s wall, 49 namespaces, 865 tests, 0 failures.**
Box context: start `load 5.18 6.59 7.40`, `pgrep -c java = 19`; end `load 5.21 7.03 7.87`,
`java = 22`. Not a quiet box — 19-22 other-seat JVMs throughout.

| # | namespace | wall s | % | cum % | tests | asserts | sampled child procs |
|---|---|---:|---:|---:|---:|---:|---:|
| 1 | `clj-surgeon.reader-eval-fence-test` | 464.92 | 64.9% | 64.9% | 7 | 74 | 2 |
| 2 | `clj-surgeon.mcp-relation-census-launcher-test` | 63.71 | 8.9% | 73.8% | 5 | 186 | 3 |
| 3 | `clj-surgeon.mcp-alias-migration-test` | 59.89 | 8.4% | 82.1% | 126 | 3148 | 1 |
| 4 | `clj-surgeon.mcp-relation-census-test` | 36.21 | 5.1% | 87.2% | 82 | 2533 | 2 |
| 5 | `clj-surgeon.mcp-prepared-wire-test` | 18.57 | 2.6% | 89.8% | 3 | 27 | 7 |
| 6 | `clj-surgeon.txn-journal-test` | 16.97 | 2.4% | 92.1% | 80 | 545 | 1 |
| 7 | `clj-surgeon.mcp-hot-verify-test` | 10.02 | 1.4% | 93.5% | 4 | 19 | 0 |
| 8 | `clj-surgeon.admit-patch-test` | 6.48 | 0.9% | 94.4% | 114 | 2122 | 19 |
| 9 | `clj-surgeon.mcp-compact-relations-test` | 6.37 | 0.9% | 95.3% | 8 | 473 | 0 |
| 10 | `clj-surgeon.mcp-tool-test` | 6.03 | 0.8% | 96.2% | 48 | 490 | 0 |
| 11 | `clj-surgeon.outline-differential-test` | 5.67 | 0.8% | 97.0% | 5 | 17 | 0 |
| 12 | `clj-surgeon.mcp-process-test` | 3.59 | 0.5% | 97.5% | 19 | 70 | 9 |
| 13 | `clj-surgeon.core-discovery-test` | 3.01 | 0.4% | 97.9% | 7 | 35 | 1 |
| 14 | `clj-surgeon.mcp-http-server-test` | 1.28 | 0.2% | 98.1% | 13 | 158 | 0 |
| 15 | `clj-surgeon.mcp-operation-registry-test` | 0.87 | 0.1% | 98.2% | 1 | 51 | 0 |

The remaining 34 namespaces total **9.2 s** (1.3%). Full 49-row table:
`2026-09-04-suite-spike-round1-timing.md`. Raw receipt — per-namespace counters, the distinct
child argv the sampler saw, and the per-namespace temp-root diff:
`2026-09-04-suite-spike-round1-timing.edn`. The classification receipt is not committed (166 KB
of pprint); regenerate it with `bb dev/experiments/suite_classify.clj`.

**One line of learning:** the suite is not slow — **eleven namespaces that launch cold JVM/bb/CLI
child processes are 674.0 s of the 716.7 s (94%)**, one of them (`reader-eval-fence-test`, ~20
cold launcher drives) is 65% by itself, and the other **36 namespaces finish 865-test-worth of
work in 20.9 s**; the parallelism gate is therefore not paid for by the tests as a body, it is
paid for by a handful of subprocess batteries plus **two load-fragile namespaces** that only fail
under contention — a teardown race and a set of hardcoded millisecond deadlines.

**One caveat:** the box was never quiet (load 5-15, 19-24 foreign JVMs). Every wall figure here is
an upper bound with unknown variance from *one* solo run, and the interference result is **two**
concurrent pairs — enough to name a mechanism, not enough to be a rate. No four-wide run was done,
and `~/.m2` / `~/.gitlibs` were warm throughout.

## What was measured, and how

`dev/experiments/suite_timing.clj` re-implements the production runner's `-main` with a wrapper
around `clojure.test/test-ns`, in the same single JVM, in the same order — and it **reads that
order out of `mcp_test_runner.clj`'s source** rather than restating it, so the harness cannot
drift from the lane it measures. Subprocesses are found by **sampling
`ProcessHandle/current .descendants` every 40 ms** rather than by redefining
`clojure.java.shell/sh`: `admit-patch-test` and `txn-journal-test` build `ProcessBuilder`
directly and name nothing a `with-redefs` could see. The sampler's blind spot is a child shorter
than one sample, which is why every claim below is corroborated by a static scan
(`suite_classify.clj`, file:line evidence) and a namespace is called a spawner when *either*
source says so.

## Classification — 101 test namespaces, both lanes

Counts (a namespace can carry several tags):

| property | JVM lane (49) | bb lane (52) |
|---|---:|---:|
| **pure** (no tag at all) | 7 | 13 |
| spawns a process | 15 static / 11 adjudicated | 17 |
| temp filesystem (real creation) | 33 | 34 |
| binds a port | 4 static / **2 real, both `:port 0`** | 3 |
| touches `target/` / `$HOME` / `.cpcache` | 5 static / **0 real** | 0 |
| global mutation | 20 static / **2 real `alter-var-root`** | 16 |
| sleeps or polls | 14 | 2 |

Per-namespace rows with file:line evidence:
`docs/observations/2026-09-04-suite-spike-round1-classification.md`.

Four static tags collapse under inspection, and the collapses are the useful part:

- **No fixed port anywhere in the JVM lane.** Every real bind is ephemeral:
  `mcp_hot_verify_test.clj:12,44` and `mcp_tool_test.clj:1464` start nREPL on
  `:bind "127.0.0.1" :port 0`; `mcp_http_server_test.clj:193,234,312,423,509,651` start Jetty on
  `:port 0`; `mcp_prepared_wire_test.clj:218` passes `":port" "0"` to its child and learns the
  real URL from a ready-file. The `:port 3000` hits (`analyze_test.clj:27`,
  `extract_test.clj:23`), `"http://localhost:9999"` (`mcp_http_server_test.clj:62`) and
  `"http://localhost:7889/mcp"` (`workspace_onboarding_test.clj:201`) are string data in
  assertions, never bound.
- **No namespace touches the repository's `target/`.** `mcp_alias_migration_test.clj:2755` and
  `scope_stream_test.clj:319` write `target/...` *inside a per-test temp workspace* — that is the
  point of those tests (a discovery exclusion). The repo's own `target/` is not referenced by the
  Makefile's test targets at all.
- **The spec's "battery receipt lives in `target/`" is wrong about the path, right about the
  hazard.** The shared root is `MEMBAT_ROOT ?= /home/forge/tmp/membat` (`Makefile:882`), with
  `PARSER_RED_ROOT ?= /home/forge/tmp/admit/parser-red` (`Makefile:985`) and the
  `flock /home/forge/tmp/suite.lock` in `memory-red` (`Makefile:989`). Those are absolute seat
  paths shared by **every checkout on the box**, which is a genuine cross-clone collision surface
  — it is simply not in `mcp-test`, and no clone-per-lane fixes it.
- **The in-JVM global-mutation surface is two namespaces.** `with-redefs` accounts for 18 of the
  20 static hits and is thread-local. The two that mutate a shared production var globally are
  `mcp_http_server_test.clj:603,605` and `mcp_server_test.clj:268,269`
  (`alter-var-root #'tool/handle-...`, restored afterwards). There is **no `System/setProperty` in
  any test** — the only occurrences are the docstrings in `tmp_leak_support.clj` explaining why it
  does not work.

What the runtime sampler adds that no scan could:

- `mcp-prepared-wire-test` spawns `clojure -X:clj-surgeon/mcp`, which spawns
  `git remote-https origin https://github.com/bhauman/clojure-mcp` — **the JVM lane reaches the
  network** and writes the seat-shared `~/.gitlibs` / `~/.m2` caches.
- `admit-patch-test` runs `clj-kondo --lint` **18 times** in 6.5 s.
- `mcp-relation-census-test` runs `strace -f -e ...` as a child.
- `mcp-process-test` and `mcp-cold-verify-test` spawn `sleep` and `dash -c 'sleep 5 & wait'`.
- **Zero temp entries survived any namespace** in the solo run — the tmp-leak ratchet's own
  witness came back empty for all 49.

## Interference hunt

Two real `git clone`s of `e8090624` under `/var/tmp/forge/suite-spike-fx`, the **production**
`clojure -M:clj-surgeon/mcp-test` run concurrently in both, diffed against the solo run.

| run | started | wall | result |
|---|---|---|---|
| solo (instrumented) | 17:36:27Z, load 5.18 | 12m 01s | 865 tests, 13 023 assertions, **0 failures** |
| pair 1 `cloneA` | 17:48:55Z, load 4.31 | 12m 44s | 865 tests, 13 009 assertions, **1 error + 1 temp leak, exit 2** |
| pair 1 `cloneB` | 17:48:55Z, load 4.31 | 12m 44s | 865 tests, 13 023 assertions, **0 failures** |
| pair 2 `cloneC` | 18:04:43Z, load 7.91 | 12m 47s | 865 tests, 13 009 assertions, **2 failures + 1 error + 1 temp leak, exit 4** |
| pair 2 `cloneD` | 18:04:43Z, load 7.91 | 12m 47s | 865 tests, 13 023 assertions, **0 failures** |

Load reached **15.34** by the end of pair 1 and **11.49** by the end of pair 2. **In both pairs
exactly one member failed and the other was clean** — the signature of a scheduler race, not of a
shared file or port, which would tend to break both.

**Concurrency is nearly free in wall:** 2-wide costs **12m 44s / 12m 47s against a 12m 01s solo**
— about 6% — because 94% of the wall is subprocesses blocked on their own JVM startup, not on
this JVM's CPU. The lane cap is not buying throughput. It is hiding two flaky namespaces.

**The colliding pairs are not namespace-against-namespace — they are two namespaces against the
box's CPU:**

- **`clj-surgeon.mcp-prepared-wire-test` /
  `prepared-confirm-preview-commit-and-replay-cross-the-real-http-wire`** —
  `java.io.IOException: Stream closed`, raised out of
  `mcp_prepared_wire_test.clj:48` (`stop-child!`) via `FutureTask.report`.
  Mechanism, exactly: `start-child!` (line 35) reads the child's stderr in a
  `future`; `stop-child!` closes the child's streams and `.destroy`s it (lines 41-45) and *then*
  `(deref stderr 5000 "")` (line 48). `deref`'s default value covers a **timeout**, not an
  **exception**: when the destroy wins the race against the still-running `slurp`, the future
  completes exceptionally and the deref rethrows. On a quiet box the slurp finishes first; under
  contention it does not.
- **The second-order failure is the interesting one.** The throw escapes the test's
  `(finally (stop-child! child) (delete-tree! project))` (line 244) *before* `delete-tree!` runs,
  so the temp workspace `prepared-wire-http-12804229785494378652` survived, and the tmp-leak
  ratchet correctly failed the run a second time. **Exit 2 = one error plus one leak, from one
  race.** A cleanup sequenced after a call that can throw is not cleanup.
  **Reproduced in both pairs**, on a different clone each time.
- **`clj-surgeon.mcp-process-test`** — surfaced in pair 2 only, two failures:
  - `direct-shell-shim-uses-the-same-host-admission` (`mcp_process_test.clj:275`) — a
    `(wait-until 1000 ...)` for a lock file written by a shim whose real binary is `/bin/sleep`
    and whose admission timeout is `CLJ_SURGEON_CLJ_KONDO_TIMEOUT_MS=100`. One second of wall is
    not one second of scheduling at load 12.
  - `admission-wait-and-analyzer-share-one-deadline` (`mcp_process_test.clj:369`) —
    `(is (false? (:finished? result)))` for a `sleep 0.30` child bounded at `:timeout-ms 350`,
    then `(is (< (:elapsed_ms result) 650.0))`. **A 50 ms margin between "must not finish yet" and
    "must not take longer than" cannot survive contention**; under load the child either finishes
    inside the 350 ms window or blows the 650 ms ceiling, and the test asserts against both edges.

  This is a wall-clock-calibrated test, not a shared-resource test. It belongs in the battery lane
  for the same reason the launcher drives do — it measures the machine.

**Shared resources named, with their verdicts:**

| shared resource | reachable from `mcp-test`? | collided? |
|---|---|---|
| a fixed TCP port | no — every bind is `:port 0` | no |
| the repository's `target/` | no — never referenced | no |
| `java.io.tmpdir` | isolated per run by `tmp-leak-support/secure-tmpdir!` | no |
| `~/.m2`, `~/.gitlibs`, per-clone `.cpcache` | yes, via `clojure -X` + `git remote-https` in `mcp-prepared-wire-test` | not observed (both clones already warm) |
| `/home/forge/tmp/membat`, `/home/forge/tmp/admit/parser-red`, `suite.lock` | no — battery targets only | n/a this round |
| **CPU / scheduler latency** | yes | **yes — the only collision observed, in both pairs** |

**The four-way run was NOT performed.** The spec gates it on "if two were clean," and two were
not. A second 2-way replication was run instead — on two *fresh* clones, so a residue from pair 1
could not carry over — and it reproduced the wire defect and added the process-test pair.

**Evidence strength: one solo run (clean) and two concurrent pairs, 2/2 with exactly one failing
member.** That is four concurrent suite-runs' worth of evidence, not a rate with a confidence
interval. It does not need one: both mechanisms are visible in the source and neither depends on
a statistic to be believed. What the evidence does *not* cover is the four-wide case, cold caches
(`~/.m2` and `~/.gitlibs` were warm in every run, so the wire test's `git remote-https` never
raced), or a genuinely quiet box.

## Partition proposal

| lane | namespaces | measured wall (solo, this box) | rule |
|---|---:|---:|---|
| **fast** | 36 | **20.9 s** of test time (+ ~10.5 s JVM boot and requires) | no child process, no bind, no shared absolute path; only `java.io.tmpdir` subdirs it creates itself |
| **integration** | 2 | 11.3 s | binds an ephemeral port or drives a server in-process, per-test unique resources |
| **battery** | 11 | **674.0 s** | spawns a JVM, `bb`, a CLI, `clj-kondo`, `git`, or `strace` |

**Fast lane (36):** everything not named below. Projected wall **≈ 31 s** cold in one JVM,
against the < 60 s target. Cost to move: **zero** — none of them is referenced by the others and
none carries a tag that survives inspection. Its three slowest are
`mcp-compact-relations-test` 6.37 s, `mcp-tool-test` 6.03 s, `outline-differential-test` 5.67 s.

**Integration lane (2):** `mcp-hot-verify-test` (10.02 s, nREPL on `:port 0`),
`mcp-http-server-test` (1.28 s, Jetty on `:port 0`). Cost to move: zero; both already allocate
ephemerally. `mcp-http-server-test` also holds one of the two real `alter-var-root` sites, so it
must not run N-wide *inside one JVM* with anything that reads those vars — across processes it is
free.

**Battery lane (11):** `reader-eval-fence-test` (464.92), `mcp-relation-census-launcher-test`
(63.71), `mcp-alias-migration-test` (59.89), `mcp-relation-census-test` (36.21),
`mcp-prepared-wire-test` (18.57), `txn-journal-test` (16.97), `admit-patch-test` (6.48),
`mcp-process-test` (3.59), `core-discovery-test` (3.01), `mcp-cold-verify-test` (0.59),
`repository-hygiene-test` (0.05). Cost to move: the lane becomes the long pole and everything
else stops waiting on it. **`mcp-server-test` (0.01 s) stays in the fast lane but carries the
second `alter-var-root`** — it is listed here so round two does not lose it.

**Where the 674 s actually is, and the withdrawal question the spec asked.** It is **~20 cold
launcher drives in one namespace**: `reader_eval_fence_test.clj:81-83` builds
`["bb" "-cp" … "-m" "clj-surgeon.core"]` and
`["java" "-cp" (System/getProperty "java.class.path") "clojure.main" "-m" "clj-surgeon.core"]`,
and five deftests drive that matrix (`:jvm`×3 + `:bb`×3, then `:jvm`×2 + `:bb`×6, then 2, 2, 2).
Each `java` drive is a **cold Clojure runtime load over the full test classpath**. These are *not*
batchable into one warm JVM without weakening the witness: the whole point is that a **real,
separately-launched launcher process** does not evaluate a build file it discovers. So the spec's
withdrawal clause partly applies — **the JVM spawns are necessary** — but its conclusion does not
follow, because the necessary spawns are **isolated in eleven namespaces**, and the *other 38*
never needed the lane cap at all. Round two's win is not making the batteries fast; it is
**letting the 20.9 s of real tests stop queueing behind them**.

## The ratchet for round two

A fast-lane witness that fails when a fast-lane namespace acquires any battery property. Both
halves are required, and the second is the one that catches what the first cannot:

1. **Source-scanning half** — for each namespace in the fast-lane list, refuse on a spelling of
   `ProcessBuilder`, `clojure.java.shell`, `babashka.process`, a literal `"java"`/`"bb"`/
   `"clojure"`/`"make"` argv head, a numeric `:port`, or an absolute path outside
   `java.io.tmpdir`. **This half is a marker check and can be defeated by a name it does not
   know** (a helper in another namespace, a port from a var) — it is the cheap half, not the
   proof.
2. **Runtime half, which is the real gate** — around the fast lane, assert (a) a **descendant
   process count of zero**, sampled the way `suite_timing.clj` samples, since it watches the
   kernel rather than the source's spelling; (b) a **port ledger** of zero binds outside the
   process's own ephemeral allocations; (c) a **temp-root diff of zero** — the mechanism already
   exists and already returns empty in `tmp_leak_support/report-and-sweep-leak!`; (d) **no read
   under the repository's `target/`**.
3. **Prove it goes red.** Have a reviewer reintroduce each defect — add a `sh` call, a fixed
   port, a write outside the run root — and require the witness to fail by name for each. A
   presence audit that nobody has watched fail is an opinion.

And the two defects this round found should be closed in round two, each with its own witness, at
the highest rung that fits:

- **`stop-child!` must not let a teardown exception escape** (the leak is downstream of it), and
  the test's `finally` must run every cleanup step **independently of the previous one's
  success**. The typed-refusal rung is available here: make the leak unrepresentable by having the
  fixture own the temp workspace's deletion rather than the test body's `finally` tail.
- **`mcp-process-test` must stop asserting on wall-clock margins it does not control.** A 50 ms
  gap between "must not have finished" and "must be under 650 ms" is not a requirement, it is a
  measurement of an idle box. Either assert on the *ordering* the deadline is meant to produce, or
  move the numeric margin behind an explicit, documented ceiling the test can widen under load and
  the ratchet can check.

For both, **the red witness must be run under contention** — the failures are races, so an example
test on a quiet box is not the reproduction. The cheapest honest form is the one this round used:
two concurrent suites from two clones, and require the failure to appear.

## Receipts

- Harness: `dev/experiments/suite_timing.clj`, `suite_classify.clj`, `suite_report.clj` (commit
  `34e0e82a`).
- Raw timing EDN: `2026-09-04-suite-spike-round1-timing.edn` (this run, verbatim).
- Fixtures: `/var/tmp/forge/suite-spike-fx` (four clones, all logs), removed at the end of the
  session; nothing under `/tmp`.
- Commands: `TMPDIR=/var/tmp/forge clojure -Sdeps '{:aliases {:suite-timing {:main-opts ["-m"
  "suite-timing"]}}}' -M:clj-surgeon/mcp-test:suite-timing <out.edn>` (solo, instrumented);
  `TMPDIR=/var/tmp/forge clojure -M:clj-surgeon/mcp-test` in each clone (concurrent).
