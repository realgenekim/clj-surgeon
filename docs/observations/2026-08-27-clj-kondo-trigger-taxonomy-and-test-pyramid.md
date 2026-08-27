# clj-kondo Trigger Taxonomy and Test Pyramid

**Date:** 2026-08-27

**Owner:** `clj-surgeon-it1`

**Purpose:** Explain every known analyzer entrance, reconstruct the retained
`1 + 2 + 2` incident, and preserve test integrity with fewer real processes.

## Why a test JVM launches clj-kondo

Kaocha does not decide to lint. Tests call production functions whose semantic
engine or verifier is clj-kondo. The immediate process tree is therefore:

```text
make test (sometimes)
  -> Kaocha JVM
       -> fix-declares / move / MCP verification code
            -> clj-kondo
```

Eleven of twelve retained clj-kondo process observations across six of seven
incidents were direct children of Kaocha JVMs. One Aug-23 parent ran
`kaocha.runner unit --watch`; five later retained parents ran
`kaocha.runner unit --fail-fast`. cclsp and the shared Surgeon MCP were not the
launcher. The recorder cannot always prove whether Make started the Kaocha JVM,
so removing a Makefile lint target alone would not close the entrance.

## Production and developer entrances

| Entrance | Trigger | Analyzer count and scope |
|---|---|---|
| Direct agent shell | Agent requests lint after a change or experiment | Arbitrary; changed files are cheap, `--lint src test` is whole-repository |
| CLI `:ls` / `:outline` | Namespace outline with enrichment | One file per call; MCP `inspect_clojure` does not need this process |
| CLI `:declares` / `:fix-declares` | Declare analysis and planning | One file per plan |
| Prior CLI `:fix-declares!` | Execute declare movement | One plan plus one hidden analysis per move; now removed in favor of the frozen exact owner plan |
| Binding rename | `rename_binding` in CLI or MCP transaction | One addressed source through stdin |
| MCP `verify=fast` | Diagnostic-delta verification | Two per transaction: pre-write baseline and future snapshot; changed files only, cache false |
| MCP `verify=exact` | Project-owned closed exact profile | One declared command inside the atomic transaction |
| MCP without verify | Ordinary mutation | Zero unless the operation itself needs binding analysis |
| MCP `verify=full` | Cold suite selected | Indirect; the suite can launch many analyzers |
| Non-fused benchmark | Model is asked to run external exact lint | One per model run; prior harness parallelism could multiply it |
| Fused benchmark | Mutation owns `verify=exact` | One internal verifier and no later shell lint |
| Move scorer | Validate generated candidate | One per candidate in the old shape |
| Candidate-bounded Var fallback | Proposed semantic escalation | Not yet a launch entrance at base `d3dfd60` |

## Exact test-suite census

A static census found 17 test vars launching 29 real analyzer processes per
complete `make test`.

| Test surface | Real launches | Actual intent |
|---|---:|---|
| Two CLI dispatch tests | 2 | String/alias parsing and dispatch |
| Eleven `fix-declares` cases | 11 | Planning and filesystem execution policy |
| Eager def/macro move case | 2 | Movement policy plus candidate validity |
| Two real-program move fixtures | 4 | Baseline and transformed-candidate validity |
| Diagnostic-delta scenario matrix | 6 | Three baseline/future policy scenarios |
| MCP transaction diagnostic test | 4 | Commit on unchanged findings, rollback on introduced finding |
| **Total** | **29** | Ordinary runner 19; MCP runner 10 |

Admission, timeout, and process-lifetime tests use fake executables, held locks,
`sleep`, or `true`. They do not launch the real analyzer and should remain.

## The retained `1 + 2 + 2` incident

Authoritative source:

- Codex session `01a02ec5-5a82-7870-ba86-5916e8b130ae`;
- turn `01a04465-a387-7450-9895-26df6c835be7`;
- rollout
  `/Users/genekim/.codex/sessions/2026/08/23/rollout-2026-08-23T06-18-00-01a02ec5-5a82-7870-ba86-5916e8b130ae.jsonl`.

Chronological order was one, then two, then two—not five concurrent analyzers:

1. **18:11:12Z:** one no-cache whole `src+test` scan in
   `/Users/genekim/src.local/clj-surgeon`; 3.79 seconds and about 14.3 MB JSON.
2. **18:11:32Z:** one cold and one warm whole `src+test` scan in a sequential
   loop; 5.97 and 3.99 seconds, each about 14.3 MB JSON.
3. **18:12:00Z:** two retained-workspace probes in a sequential loop; 0.18 and
   15.23 seconds. The last used about 1.32 GiB maximum RSS.

Flight-recorder evidence captured clj-kondo PID 25583, CWD
`/Users/genekim/src.local/clj-surgeon`, at about 98% CPU and 443 MiB RSS.
Spotlight and paging were active, and the host already held roughly 34.9 GiB of
swap. Back-to-back work arrived faster than load and paging pressure decayed,
so load rose from 20.7 to 118.5 after the individual processes exited.

The experiment asked three reasonable questions with unnecessarily broad
work:

```text
Can clj-kondo answer the relation? -> scan one whole workspace
Does caching help?                 -> scan it twice more
Does it generalize?                -> scan two more workspaces
```

The later syntax-first experiment narrowed each query to two to four candidate
files and reproduced all four retained Var surfaces. Whole-tree scans were not
required.

## Seven-day context

Recorder coverage is incomplete—3,897 of 10,080 expected minutes—but it
contains 45 episodes at load 100 or greater, 26 at 200 or greater, eight at 400
or greater, and one recorded peak of 767.624. Retained clj-kondo processes used
80–98% CPU and about 196–519 MiB RSS. Other proven contributors include
Supacode `gh`/git churn, observer amplification, cold JVM suites, Spotlight,
and one long-running Babashka process. Unknown-cause intervals remain unknown.

## Integrity-preserving test pyramid

### Pure semantic layer

Replay normalized, provenance-bearing clj-kondo evidence for the combinatorial
policy matrix:

- forward references become a pure projection of definitions and usages;
- fix-declares planning consumes forms, dependencies, cycles, and normalized
  forward references;
- filesystem execution consumes a frozen exact plan;
- diagnostic-delta matrices consume frozen before/future findings;
- move policy consumes parsed fixture and candidate manifests.

Fixtures record provider version, source hash, exact command/config, and only
the fields consumed by the pure core. Product code accepts harmless additional
provider keys but refuses missing or malformed required fields.

### Fake boundary layer

Fake processes prove locking, admission timeout, pressure defer, shared
deadlines, cancellation, child-tree cleanup, output bounds, rollback, and
unavailable-authority classification. They must never pretend to prove analyzer
semantics.

### Mandatory real contract layer

Retain five sequential real launches:

1. one forward-reference/fix-plan end-to-end case;
2. one binding-analysis case, which closes a current schema-drift hole;
3. one batched no-cache lint for all move baseline/candidate fixtures;
4. one real diagnostic baseline;
5. one real future-snapshot diagnostic verification.

The everyday runner goes from 19 real launches to zero. The complete milestone
suite goes from 29 to five—83% fewer—without dropping provider drift detection.
The real contract target runs on Anvil or under an explicit fresh-green laptop
mission lease.

## Admission and convoy law

The analyzer-owned `fcntl` lock prevents overlap across agents, JVMs, and
repositories. Red or critical pressure now defers before waiting, while
waiting, and after acquisition. This is necessary but not sufficient for the
retained convoy.

A raw or untrusted request gets one launch per fresh pressure sample. A paved
multi-launch suite may receive a closed mission lease with exact owner start
identity, CWD, scope/profile hash, launch count, and time budget. Every next
child requires a complete fresh green pressure sample timestamped after the
prior child exits. The lease never overrides pressure and blocks independent
heavy work. A provisional per-launch 60-second cooldown was rejected as the
only policy because a 32-launch logical suite would take at least 31 minutes.

## Implementation sequence

1. Install and verify the analyzer-owned host gate and direct shell shim.
2. Keep red/critical pressure defer and bounded launch telemetry at the gate.
3. Remove hidden analyzer re-entry after partial mutation.
4. Reduce the everyday test runner to zero real launches.
5. Add the five-launch sequential contract target.
6. Translate the pure mission-lease state machine after its LID review.
7. Route cold suites and benchmark cohorts to Anvil unless Skiff owns a fresh
   green lease.
