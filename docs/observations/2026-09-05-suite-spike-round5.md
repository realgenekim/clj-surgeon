# Suite spike, round five — the round-three landing review's four blockers

Written 2026-09-04T23:59:40Z by forge-anvil on `bridge/suite-spike`.

The reviewer's coverage comparison is the finding this round answers. The merge
gate got faster by moving **510 of 957 tests** to the battery lane — the same
corpus, not the same gate coverage — and the discipline that makes that safe was
not enforced on the landing path. Three of the four blockers are the same class
in different clothes: **a control that observes a NAME where the rule is about
BEHAVIOUR**. A target that exists is not a target that runs your namespace. A
tripwire that exists is not a tripwire on the path. A pid set is not a record of
what executed.

## Finding 4 (BLOCKING) — exclusions now prove runner MEMBERSHIP

`clj-surgeon.runner-membership` resolves a named runner to the namespace set it
actually executes: a Makefile rule to its prerequisites, its `$(MAKE)`
sub-targets and its `-M:clj-surgeon/<alias>`; an alias to its `:main-opts`;
`-m clj-surgeon.mcp-test-runner <lane>...` to the lane manifest; any other
`-m <ns>` to that runner file's own spellings. It **fails closed**: a runner whose
selection cannot be read is `:unresolved-runner`, because *I could not work out
what that runs* must never read like *it runs yours*.

RED (the reviewer's sabotage, against the extracted round-three predicate):

```
SABOTAGE violations: 0 []
REAL-MAP violations: 0
resolve make test-fast -> 0
VERDICT: RED
```

GREEN:

```
SABOTAGE violations: 1 [:not-a-member]
  excluded namespace clj-surgeon.analyzer-contract-test names runner(s) make
  test-fast -- and none of them RUNS it. `make test-fast` runs 39
  namespace(s), not including this one.
REAL-MAP violations: 0
resolve make test-fast -> 39
VERDICT: GREEN
```

Every live exclusion proves membership, not existence:

| excluded namespace | runner it names | runs | member |
|---|---|---|---|
| `analyzer-contract-test` | `make analyzer-contract-test` | 1 | yes |
| `analyzer-contract-test` | `:clj-surgeon/analyzer-contract-test` | 1 | yes |
| `memory.journal-green-test` | `make memory-red-kernel` | 3 | yes |
| `memory.oom-reproduction-test` | `make memory-red-kernel` | 3 | yes |
| `worktree-lifecycle-prune-test` | `make worktree-lifecycle-test` | 4 | yes |
| `worktree-lifecycle-recovery-test` | `make worktree-lifecycle-recovery-test` | 1 | yes |

## Finding 2 (BLOCKING) — `make landing-gate` is the target `~/bin/land` runs

**`make landing-gate`.** That is the exact name. It runs, in order:
`battery-fresh`, `mcp-test`, `test-bb`, `repository-hygiene`. The freshness
tripwire runs **first** and costs a second of `bb`, so a stale receipt refuses
before seven minutes of JVM and the remedy arrives while the seat is still
looking at the screen. It is the single name to change when the landing gate's
contents change, so the seat tool never drifts from what the repository
considers a landing.

RED, before the target existed:

```
$ make -n landing-gate
make: *** No rule to make target 'landing-gate'.  Stop.
```

GREEN, sabotage on a `git archive` copy with the ledger aged to 40 h (1 of 1
refused):

```
$ make landing-gate
battery-fresh: REFUSED (stale) -- the newest battery receipt is 40.0 h old
(sha 2522bd95…, started 2026-09-03T07:50:17Z); the tripwire refuses past 26 h
REMEDY: run the battery and commit its receipt -- …
make: *** [Makefile:1061: landing-gate] Error 2      RC=2
```

## Finding 6 (BLOCKING) — the fast lane now sees a child that already exited

Two controls were blind to `mcp-inspect-tool-test` driving `/bin/sh` through the
production cold-verify helper, and they were blind for the same reason: **both
observe STATE, and a process that ran and exited leaves none.** The source scan
looks for spawn spellings in the *test* file; the spawn is three namespaces away
in `mcp-process/run-bounded!`. The pid diff is a set of *live* descendants; the
test waits for the child to exit.

So round five records the **event**. `clj-surgeon.spawn-ledger` is append-only;
all four repository-owned spawn helpers append at the moment of launch
(`mcp-process/run-bounded!`, `workspace-onboarding/run-command!`,
`failure-report/run-captured!`, `worktree-lifecycle-io`); the fixture diffs the
ledger across a namespace exactly as it diffs the live pid set, and a pid seen by
both is reported once as the live kind.

Then the lane was made true rather than the rule quieter: the one spawning test
moved to `clj-surgeon.mcp-inspect-cold-job-test` (`:battery`). Reclassifying the
whole namespace would have taken the other thirty-eight inspect-tool tests off
the merge gate to fix one — the same trade the review criticised at a larger
scale.

**Reach, stated:** the ledger sees only helpers that record.
`every-src-spawn-site-records-into-the-ledger` holds the src half closed by
enumerating the `ProcessBuilder.` spelling, and its own comment says it is a
spelling check, not a proof.

## What the ledger found that the review did not

The first live run of the ledger refused **six** spawns, not one. Verbatim from
`make test-fast`:

```
TEST-ISO-002 VIOLATION in clj-surgeon.mcp-change-buffer-test -- process spawn:
pid 3536417 was launched by this namespace through a repository spawn helper
and has already exited, so no live-descendant snapshot can see it:
/usr/bin/printf %s xxxx…
   … /usr/bin/false
   … /bin/sleep 1
TEST-ISO-002 VIOLATION in clj-surgeon.mcp-compact-relations-test …
   /usr/bin/true … /usr/bin/true … /usr/bin/false
TEST-ISO-007 VIOLATION in clj-surgeon.mcp-feature-thread-test -- time budget:
ran for 47333 ms, over its 8000 ms budget
TEST-ISO-007 VIOLATION in :fast -- lane time budget: the fast lane took
69428 ms, over its 60000 ms budget
```

`make mcp-test` then found five more in the integration lane
(`mcp-http-server-test`, `mcp-tool-test`). **The review read one file and found
one; the ledger reads execution and found eleven.** That is the argument for
records-of-execution over source scans and state snapshots, made by the
apparatus rather than asserted.

**The remedy is chosen by what the command IS**, and both of the review's two
options are used:

- Two drives belong to subsystems with a battery home, so they were
  reclassified: the cold-verification job and the `sed` oracle.
- The rest are cases where the command is the **subject**.
  `run-exact-verification!` is a runner of user-supplied verify commands, and a
  test of it that never runs one is a test of a mock. Moving those moves the
  boundary they prove off the merge gate — the exact coverage loss the review
  objected to.

So the contract was made **precise rather than loosened**. `:fast` now reads:
no child process, *except these exact commands, in these exact namespaces, for
these reasons* (`ns-isolation/fast-lane-spawn-allowlist`). Three properties keep
it a ratchet rather than an escape hatch, each with a witness:

1. `cold-runtime-command` — `java|clojure|clj|bb|babashka|clj-kondo|git|node|npm|python|make` — **can never be allowlisted**, so the 674 s the partition removes cannot return through this door. One witness plants a cold JVM *in* the allowlist and asserts it is still refused.
2. Matching is **per namespace, on a command prefix**: the same command in another namespace fails, and a new command in an allowlisted namespace fails by name.
3. A **live** child is refused regardless — the allowlist only ever excuses a child launched and reaped inside the same namespace.

`mcp-feature-thread-test` moved `:fast` → `:integration` with a declared 90 s
ceiling and the measurement at the pin (47.3 s; it alone blew the fast lane's
60 s whole-lane budget). It stays on `make mcp-test`: **no gate coverage lost.**

### A real defect in round four's own witness

TEST-ISO-010 attributed `clojure-agent-send-off-pool-N` — Clojure's global
cached pool behind `send-off`/`future`, non-daemon by design with a 60 s
keep-alive — to whichever namespace was running when it grew. Measured on this
branch: the same thread was blamed on `workspace-onboarding-test` on one run,
on `mcp-tool-test` on the next, and on nobody on a third. **A witness whose
verdict moves with the weather is not a ratchet; it is a thing people re-run
until it is green**, and that habit costs more than the check is worth. The pool
is exempted by name with the reason; a namespace's *own* non-daemon thread is
still refused by id and name, which the new witness asserts as its second half.

### Three self-inflicted defects, all one class

A scanner that reads its own text as its subject. The sleep census matched its
own regex literal and a docstring *citing* the fixed sleep it replaced; the src
spawn scan matched the scan string in the witness performing it; the probe
witness appended its own fixture to the real ledger, which the fixture then
correctly reported against it. **Prose about a defect is not the defect, and an
observer that pollutes what it observes is not an observation.**

## Finding 3 (BLOCKING) — confirmed closed, with a class-level pin

Round four's tmpdir fix holds. A scan of all 40 `:fast` namespaces finds only
`Files/createTempDirectory`/`createTempFile`, which derive from
`java.io.tmpdir` by construction, and TEST-ISO-006 has already made that root a
throwaway. The review found this by reading **one file**, so the ratchet scans
the whole lane for the three real defect shapes (an env temp override with a
seat-absolute fallback, an absolute-path literal fixture root,
`/home/<user>/tmp`) —
`no-fast-lane-namespace-roots-a-fixture-outside-its-own-tmpdir`.

## Non-blocking items

- **Bounded sleeps on the merge gate** are now an enumerated, declared-exemption
  list with the reason at each site; an undeclared sleep fails by file and line,
  and so does a declared site that no longer exists. Three are bounded polls
  that succeed on their condition; one is the only fixed sleep left on the gate
  (5 ms, census-pool, backing a claim about scheduling).
- **The rename scanner's blind spot** is asserted rather than merely disclosed:
  the scanner is proved NOT to see a `test-fast` mention with no babashka
  spelling. Whoever closes the gap deletes a failing test.
- **The GHA workflow** remains on `bridge/gha`, not this branch. Post-push and
  nightly battery discipline is that branch's landing; `make landing-gate` is
  the pre-merge half and does not depend on it.

## The trunk merge

`origin/MCP/main` at `a74d8407`, 94 commits, merged. Two conflicts, both
resolved toward the lane runner: the runner's hard-coded 49-namespace
`run-tests` and static `:require` block (HEAD kept on both hunks), and the
spec-doc vector (unioned: feature-thread + temp-dir-hygiene + test-isolation).

The trunk's `mcp-feature-thread-test` (69 deftests) is adopted into `:fast` with
`^{:lane :fast}` on its ns form. It shells out to `sed` in one `testing` block,
which the spawn ledger would now refuse by pid and command line, so that one
assertion moved verbatim to `mcp-feature-thread-sed-test` (`:battery`) — the same
trade as the inspect-tool cold job, for the same reason.

Pins re-derived on the merged result: **57 namespaces (40 fast, 4 integration,
13 battery)**; round one's 49 declare 920 deftests; adopted 136
(12+4+25+3+69+1+1+21); total **1056 = 920 + 136**. A move keeps the total; a
deletion does not, and only one of them passes that equality. Disk/manifest/ns
metadata cross-check on the merged tree: on-disk 108, unaccounted 0, phantom 0,
metadata mismatches 0.

## Gates run

All in the shared verification period from 00:27Z, one at a time, each as
`taskset -c 6-9 ~/bin/suite-run <cmd>` (the peer lead's suite held cores 2–5).
No timing claim is made from any of them.

| gate | start | 1-min load | result |
|---|---|---|---|
| `make test-fast` | 00:33Z | 7.83 | **RC=0** — Ran 406 tests / 3811 assertions, 0 failures, 0 errors; `test-isolation: 0 violations across 39 namespace(s)` |
| `make mcp-test` | 00:39Z | 4.14 | **RC=0** — Ran 547 tests / 6847 assertions, 0 failures, 0 errors; oracle `pass`; `test-isolation: 0 violations across 44 namespace(s)` |
| `make test-bb` | 00:42Z | 4.95 | **RC=0** — Ran 840 tests / 6921 assertions, 0 failures, 0 errors |
| `make repository-hygiene` | 00:45Z | 2.85 | **RC=0** — `no machine-local build cache is tracked at any depth` |
| intent audit | 00:45Z | 2.78 | **RC=0** — `{:ok true, … :violations []}` |
| `make test-battery` | 00:45Z | ~5 | **RC≠0 — 3 failures.** See below. |
| `make suite-concurrency-battery N=4` | — | — | **not run**, on the coordinator's instruction: four clones would spill past this lane. |

`git merge-tree --write-tree HEAD a74d8407` → `dc895292…`, exit 0, no conflict.

## The battery FAILED, and the landing gate refuses — which is the tripwire working

```
Ran 512 tests containing 11008 assertions.
3 failures, 0 errors.
TEST-ISO-007 VIOLATION in clj-surgeon.reader-eval-fence-test -- time budget:
ran for 466850 ms, over its 300000 ms budget
```

Receipt appended, verbatim:

```clojure
{:sha "c79227d0…", :started "2026-09-05T00:45:49Z", :wall_s 710, :verdict :fail, :host "anvil-server"}
```

and `make battery-fresh` therefore says:

```
battery-fresh: REFUSED (last-run-failed) -- the newest battery receipt FAILED:
sha c79227d0…, wall 710s, verdict :fail. A failing gate is not a fresh gate.
```

**So `make landing-gate` refuses this tip, correctly, and that refusal is left
standing.** The ledger is append-only and records the failure; nothing here
re-runs until green.

All three failures are in one trunk-owned test,
`mcp-relation-census-test/pool-size-one-and-pool-size-n-agree-byte-for-byte`,
and the first has a **proven environmental cause — this round's own core
pinning**:

```
expected: (= 8 (:pool_size parallel))
  actual: (not (= 8 4))
```

`relation-census/effective-pool-size` is
`(max 1 (min requested (.availableProcessors (Runtime/getRuntime))))`, and
`taskset -c 6-9 nproc` → **4** against `nproc` → **16**. A request for 8 is
clamped to 4, and the test asserts the request comes back unchanged. The two
byte-parity assertions compare a pool-1 census with that pool-4 census, and the
budget overrun on `reader-eval-fence-test` (466 s against 300 s) is the same
four-core constraint on a lane whose ceiling was measured on sixteen.

**ATTRIBUTION, MEASURED, NOT ASSERTED.** The same three assertions were driven
on an unmodified `git archive` copy of the **trunk tip `a74d8407`**, under the
identical `taskset -c 6-9` pinning — a tree with none of this round's changes
in it:

```
FAIL in (pool-size-one-and-pool-size-n-agree-byte-for-byte) (mcp_relation_census_test.clj:114)
  actual: (not (= 8 4))
FAIL in (pool-size-one-and-pool-size-n-agree-byte-for-byte) (mcp_relation_census_test.clj:115)
FAIL in (pool-size-one-and-pool-size-n-agree-byte-for-byte) (mcp_relation_census_test.clj:118)
```

Identical failures, identical line numbers, on the trunk. **The battery-lane
failure is pre-existing and environmental; it is not this round's change.**
(The same run also reported `no-machine-local-build-cache-is-tracked` — the
known gitignored-store precondition when a hygiene test runs from an archive
copy, and a separate artefact of the reproduction method, not of the trunk.)

**This is the `ambient-state-is-an-invisible-precondition` class**: a gate whose
verdict depends on a property of the machine that nothing in the gate declares.
The right ratchet, for whoever owns that test, is for it to assert the
*effective* pool size against `availableProcessors` — or to declare the core
count as a named precondition and refuse rather than fail — instead of assuming
that a request for 8 workers comes back as 8. That is a trunk-owned fix and is
deliberately not made here.

**GATE OWED: a clean `make test-battery`, in a window without four-core
pinning.** Until it produces a `:pass` receipt, `make landing-gate` refuses, and
that is the correct state for this tip.
