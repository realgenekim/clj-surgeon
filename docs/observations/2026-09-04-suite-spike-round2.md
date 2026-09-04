# Suite spike, round two — mechanism first (2026-09-04, forge@anvil)

Spec: `2026-09-04-suite-spike-spec.md` (on `MCP/main`), Gene's ruling "B. Go".
Round one: `2026-09-04-suite-spike-round1.md`. Branch `bridge/suite-spike`, base
`c4f69081`, tip **`63d317a5`**. Round two builds TEST-ISO-001 (lane declaration +
registry audit + cadence), TEST-ISO-006 (throwaway home and tmpdir) and TEST-ISO-009 (the
concurrency battery), and closes round one's two load-fragile namespaces.

## The table

| gate | result | wall |
|---|---|---:|
| `make test-fast` (36 ns, cold, load 16.04) | **358 tests, 3 569 assertions, 0 failures, 0 errors** | **30.31 s** |
| `make test-integration` (4 ns) | 71 tests, 750 assertions, 0 failures, 0 errors | — |
| `make mcp-test` = fast+integration (40 ns, full target: oracle + 8 self-tests) | 426 tests, 4 309 assertions, 0 failures, 0 errors | 149.82 s |
| `make test-battery` (11 ns, under `flock /home/forge/tmp/suite.lock`, load 8.27) | 456 tests, 8 809 assertions, 0 failures, 0 errors | 709.09 s |
| `~/bin/suite-run bb test/run_all.clj` (bb lane, untouched) | 840 tests, 6 919 assertions, 0 failures, 0 errors | — |
| `make mcp-operation-oracle` | `mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]` | — |
| `make repository-hygiene` | `repository hygiene: no machine-local build cache is tracked at any depth` | — |
| **`make suite-concurrency-battery N=4`, three runs** | **3/3 PASS, 12/12 clones 0 failures 0 errors** | 199 / 192 / 185 s |

**Fast lane 30.31 s against a < 60 s target, on a box at load 16 carrying other seats.**

**Nothing was dropped, and the arithmetic closes exactly.** Round one: 865 tests / 13 023
assertions over 49 namespaces. Round two: 358 + 71 + 456 = **885 tests**, 3 569 + 750 + 8 809 =
**13 128 assertions**, over 51 namespaces. Delta **+20 tests, +105 assertions** — and round two
added exactly 20 deftests (15 in `lane-manifest-test`, 3 in `fast-lane-isolation-test`, 2 in
`mcp-prepared-wire-test`). Round one's 865 are all still there, and
`the-partition-drops-nothing-round-one-measured` pins the 49-namespace set so that partitioning
can never quietly become dropping.

**One line of learning:** the partition was the easy half. The hard half was that **every one of
round two's own defects was found by a gate, never by reading** — the concurrency battery caught
a merge gate running without the isolation its own witness asserted; the repository's existing
tmp-leak ratchet caught a refusal-ordering regression the same hour; and four rounds of running
two namespaces under a CPU burner turned round one's "two load-fragile assertions" into **six
sites, and two of them inverted the lesson of the first four**.

**One caveat:** the box was never quiet — load 8 to 31 throughout, 28-29 other seats. The
concurrency battery's three PASSes are three runs, not a rate, and they measure `make mcp-test`
(fast + integration). The battery LANE has not been run four-wide and is not claimed to be
N-safe; it is the lane that launches cold JVMs and holds `flock suite.lock` precisely because it
measures the machine.

## The partition, as shipped

Single source of truth: `test/clj_surgeon/lane_manifest.clj`.

| lane | ns | cadence | wall | rule |
|---|---:|---|---:|---|
| `:fast` | 36 | `:every-run` | 30.31 s | No child process, no bind, no network, no read of the real `$HOME` or outside the run's own tmpdir subtree, no write into the working tree |
| `:integration` | 4 | `:merge-gate` | — | Binds an ephemeral port or drives a server in-process, or writes a per-test workspace into the repository root |
| `:battery` | 11 | `:landing-and-nightly` | 709.09 s | Launches a JVM/bb/CLI/`clj-kondo`/`git`/`strace`; or measures the machine; or reaches the NETWORK |

`make mcp-test` (the merge gate) = fast + integration. `make test` runs `test-battery` after.
Cadences are declared in the manifest beside the lane (Gene, 2026-09-04) and named in the
runner's refusal, because the lane chosen decides how often a test runs and a refusal that hides
that makes the choice look free. **The nightly cron and a cadence receipt ledger are round
three; nothing here schedules anything.**

**Two deviations from round one's proposed partition**, both because a lane's rule has to be true
of its members: `mcp-tool-test` binds a real nREPL (`mcp_tool_test.clj:1464`) and writes a
`.hot-transaction-<uuid>` workspace into the repository root, and `mcp-server-test` starts an
embedded nREPL (observed binding 44909/44227). Both moved fast → integration. Neither changes
what `make mcp-test` runs.

**Network is a battery property, explicitly**, and it is in the manifest's prose. Round one's
runtime sampler caught `mcp-prepared-wire-test` spawning `clojure -X:clj-surgeon/mcp`, which
spawns `git remote-https origin https://github.com/bhauman/clojure-mcp` through `~/.gitlibs`. No
source scan of that namespace names a URL. It was already in the battery lane; the RULE is now
written down, because the next namespace to acquire a network dependency will not announce it.

**One rename, stated loudly** because a silent one is the `memory-red` collision again:
`make test-fast` USED TO MEAN `bb test/run_all.clj`. It now means the JVM fast lane. The
babashka lane is unchanged in content and is `make test-bb`. Documents dated before 2026-09-04
that say "make test-fast — 647 tests" are quoting the bb lane.

## Per-item RED → GREEN

| item | RED | GREEN | verbatim RED |
|---|---|---|---|
| TEST-ISO-001 lane declaration | `da36d42c` | `7cb8792f` | `350 tests / 3537 assertions, 37 failures` — 34 × `loaded-namespaces-carry-their-lane-at-runtime`, 1 × `every-manifest-namespace-declares-its-lane-in-its-own-ns-form`, 2 × `the-runner-refuses-an-undeclared-namespace` |
| TEST-ISO-006 throwaway home | `13c30f9d` | `64086cd3`, scope fixed in `a0d9ab3f` | `expected: (str/includes? (.getCanonicalPath home) root-name)` / `actual: (not (str/includes? "/home/forge" "clj-surgeon-suite-4142306-8fb6b45d"))` + 6 × the throwaway containing `.m2 .gitlibs .ssh .config .claude src` |
| race 1, prepared-wire teardown | `a48a6e1c` | `c8b0ad19` | `0 failures, 2 errors` + `temp-leak: 1 entries left under … prepared-wire-teardown-witness-1453751780545819951` — the throw AND the leak, one race, exactly round one's arithmetic |
| race 2, wall-clock deadlines | contention run of `a48a6e1c` | `c8b0ad19`, `be2088b4`, `881427bd`, `9c09398f` | `5 failures, 4 errors` across two clones under a 12-way burner |
| TEST-ISO-009 battery | `5bae67e0` (shell bug) | `2e9513c5` | `test/suite_concurrency_battery.sh: 52: Bad substitution` |
| TEST-ISO-001c cadence + ordering | — | `63d317a5` | `tmp-leak-ratchet witness FAILED: 9: clj-surgeon.mcp-test-runner ran (exit 96) with TMPDIR=/tmp instead of refusing` |

## The three concurrency battery runs (TEST-ISO-009), verbatim

All three at tip `63d317a5`, `make suite-concurrency-battery N=4`, four **real `git clone`s** of
the tip under `/var/tmp/forge/suite-battery-fx`, each running `make mcp-test`.

```
suite-battery: N=4 target=mcp-test sha=63d317a5
suite-battery: load at start 8.09 12.00 13.98 12/2093 1248562
suite-battery: load at end   24.92 18.40 16.07 4/2162 1341880
suite-battery: wall 199 s
--- clone 1: exit 0 | Ran 429 tests containing 4323 assertions. 0 failures, 0 errors.
--- clone 2: exit 0 | Ran 429 tests containing 4323 assertions. 0 failures, 0 errors.
--- clone 3: exit 0 | Ran 429 tests containing 4323 assertions. 0 failures, 0 errors.
--- clone 4: exit 0 | Ran 429 tests containing 4323 assertions. 0 failures, 0 errors.
suite-battery: VERDICT PASS -- all 4 clones 0 failures, 0 errors

suite-battery: N=4 target=mcp-test sha=63d317a5
suite-battery: load at start 9.31 14.37 14.85 5/2133 1380929
suite-battery: load at end   25.02 19.02 16.51 10/2103 1477037
suite-battery: wall 192 s
--- clone 1: exit 0 | Ran 429 tests containing 4323 assertions. 0 failures, 0 errors.
--- clone 2: exit 0 | Ran 429 tests containing 4323 assertions. 0 failures, 0 errors.
--- clone 3: exit 0 | Ran 429 tests containing 4323 assertions. 0 failures, 0 errors.
--- clone 4: exit 0 | Ran 429 tests containing 4323 assertions. 0 failures, 0 errors.
suite-battery: VERDICT PASS -- all 4 clones 0 failures, 0 errors

suite-battery: N=4 target=mcp-test sha=63d317a5
suite-battery: load at start 7.87 14.47 15.15 5/1993 1520079
suite-battery: load at end   22.30 17.89 16.31 9/2064 1616861
suite-battery: wall 185 s
--- clone 1: exit 0 | Ran 429 tests containing 4323 assertions. 0 failures, 0 errors.
--- clone 2: exit 0 | Ran 429 tests containing 4323 assertions. 0 failures, 0 errors.
--- clone 3: exit 0 | Ran 429 tests containing 4323 assertions. 0 failures, 0 errors.
--- clone 4: exit 0 | Ran 429 tests containing 4323 assertions. 0 failures, 0 errors.
suite-battery: VERDICT PASS -- all 4 clones 0 failures, 0 errors
```

(The 429/4323 here versus 426/4309 in the solo `make mcp-test` above is the three cadence
witnesses, added between the two runs.)

The load ceiling worked as designed and is worth recording: every run waited at 12-25 before
starting and ran from 8-9. **Four-wide `mcp-test` costs about 190 s where one costs 150 s** —
the lane cap was never buying throughput, exactly as round one found at 2-wide.

## What the battery found — the best thing it did

**Run 1 of the previous batch failed IDENTICALLY in all four clones.** Not a race; a rule that
was only true under one spelling of an invocation:

```
--- clone 1: exit 2 | Ran 426 tests containing 4309 assertions. 7 failures, 0 errors.
FAIL in (the-fast-lane-home-is-a-throwaway-inside-the-run-root) (fast_lane_isolation_test.clj:48)
6 x FAIL in (the-fast-lane-home-is-a-throwaway-inside-the-run-root) (fast_lane_isolation_test.clj:58)
[identical in clones 2, 3 and 4]
```

`isolate-home?` was `(= #{:fast} (set lanes))`. `make test-fast` was isolated. **`make mcp-test`
— fast + integration, THE MERGE GATE, the thing every builder actually runs — was not**, while a
fast-lane witness asserted unconditionally that it was. The witness was right and the mechanism
was wrong, and the only invocation that exercised the pair was the one nobody had run yet.

This is the same class as **doctrine that disagrees with the installed prompt**: true where it is
written, false where it takes effect. The fix (`a0d9ab3f`) makes the decision a property of the
RESOLVED NAMESPACE SET rather than of how the caller spelled it — isolate unless a battery
namespace is in the set, which also covers `--ns` invocations naming no lane — and the runner now
PRINTS the decision (`home-isolated true`), because a silent one is what let it sit.

Fixing that introduced a second defect within the hour, and **the repository's own tmp-leak
ratchet caught it in all four clones**:

```
--- runner clj-surgeon.mcp-test-runner (exit=96) ---
lane-refused: no lane named. [...]
tmp-leak-ratchet witness FAILED: 9: clj-surgeon.mcp-test-runner ran (exit 96) with TMPDIR=/tmp instead of refusing
```

Hoisting the "no lane named" refusal above `secure-tmpdir!` — done so the home decision could see
the resolved set — meant a run with `TMPDIR=/tmp` exited 96 for the wrong reason instead of 97
for the right one. **A run about to be refused for some other reason must still not be allowed to
reach a tmpfs on its way there.** The set is still resolved first (`lane-namespaces` is pure);
nothing acts on it until after the temp guard.

## The two races — six sites, four commits, and the lesson inverting twice

Every RED and every GREEN below was run **under contention** — two copies of the namespaces from
two archive copies of the tip, behind a 12-way CPU burner, at load 22-31. An example test on a
quiet box is a measurement of how idle the box was.

**RED (`a48a6e1c`, load 13.95 → 27.75, wall 96 s):**

```
--- copy 1 (exit 10) ---
ERROR in (a-teardown-failure-still-deletes-the-workspace) (FutureTask.java:122)
ERROR in (prepared-confirm-preview-commit-and-replay-cross-the-real-http-wire) (mcp_prepared_wire_test.clj:57)
ERROR in (prepared-confirm-preview-commit-and-replay-cross-the-real-stdio-wire) (mcp_prepared_wire_test.clj:75)
ERROR in (stop-child-reports-a-stderr-reader-failure-as-a-typed-fact) (FutureTask.java:122)
FAIL in (exec-owner-death-releases-admission-without-stale-file-cleanup) (mcp_process_test.clj:251)
FAIL in (exec-owner-death-releases-admission-without-stale-file-cleanup) (mcp_process_test.clj:252)
FAIL in (direct-shell-shim-uses-the-same-host-admission) (mcp_process_test.clj:275)
FAIL in (direct-shell-shim-uses-the-same-host-admission) (mcp_process_test.clj:280)
FAIL in (admission-wait-and-analyzer-share-one-deadline) (mcp_process_test.clj:357)
Ran 24 tests containing 77 assertions.
5 failures, 4 errors.
temp-leak: 1 entries left under … prepared-wire-teardown-witness-1679118153756043139
--- copy 2 (exit 5) ---
[the same four ERRORs]
contention: VERDICT RED
```

Round one named **two** sites. Contention found **six**, and the 650 ms ceiling round one
predicted would fail *was not among them*.

**GREEN (`9c09398f`), three replications:**

| run | load before | load after | wall | copy 1 | copy 2 |
|---|---|---|---:|---|---|
| 1 | 17.83 | 22.31 | 107 s | 24 tests / 102 asserts, 0 failures | same |
| 2 | 20.60 | 27.68 | 115 s | 24 tests / 102 asserts, 0 failures | same |
| 3 | 27.68 | 29.09 | 118 s | 24 tests / 102 asserts, 0 failures | same |

**3/3 ALL-GREEN.**

### Race 1 — a cleanup step sequenced after a call that can throw is not cleanup

`start-child!` slurps the child's stderr in a `future`; `stop-child!` closed the child's streams
and destroyed it and *then* called `(deref stderr 5000 "")`. **`deref`'s default value covers a
TIMEOUT, not an EXCEPTION**, so when the destroy won the race the future completed exceptionally
and the deref rethrew — out of the FIRST step of the test's `finally`, before `delete-tree!`.
One error plus one leak, from one race.

Fixed at two rungs. `stop-child!` returns a typed receipt (`:stderr-reader-failed` /
`:process-teardown-failed`) and never throws; and the workspace's deletion is **owned by an
`:each` fixture**, not by the tail of a `finally`. Round one's leak was not a forgotten cleanup —
there *was* a `finally` — so a fixture, which nothing the body does can skip, is the
unrepresentable rung rather than the detected one. `wire-timeout-ms` also went 30 s → 300 s: it
is a rendezvous bound on a **cold `clojure -X` child**, and round one measured one such launcher
drive at 65 s at load 7 and ~4 min at load 12. Two-wide behind a burner it timed out in *both*
copies.

### Race 2 — two kinds of number were being conflated, and then two more things were

A **RENDEZVOUS** bound answers *did the other party ever show up?* Its size is arbitrary; only its
finiteness is a requirement. Nine of them now share one named `rendezvous-timeout-ms` (15 s,
`CLJ_SURGEON_TEST_RENDEZVOUS_MS`). *A short rendezvous wait is not a stricter test, it is a
flakier one: it asserts nothing extra when it passes and something false about the machine when
it fails.*

A **CONTRACT** bound answers *was the deadline honoured?* It stays, asserted where the contract
lives. `admission-wait-and-analyzer-share-one-deadline` now asserts `elapsed >= budget` — the
deadline was not undercut — and gives the subject a child that cannot finish on its own, so
`:finished? false` is true **by construction** rather than because the box was slow enough this
time. Its one genuine ceiling could not simply be made generous (it is what discriminates *the
admission wait was INSIDE the budget* from *it was ADDED to it*), so **the signal was widened
rather than the tolerance narrowed**: a 2 000 ms hold against a 3 000 ms budget puts the two
hypotheses 2 000 ms apart with the ceiling 1 000 ms from each — twenty times round one's 50 ms.

Then the lesson inverted, twice, and these two are the ones worth carrying:

- **A generous ceiling fixes "the other party was slow." It cannot fix "the other party had
  already left."** An owner held a lock for ONE SECOND while the observer waited patiently on a
  15 s ceiling; at load 27 the owner's sleep finished before the observation happened, the waiter
  was admitted instead of refused, and the test reported `expected 75, got 0` — *calling a
  working refusal broken*. Waiting longer makes that worse. **A rendezvous has to be WINNABLE**:
  the window in which the other party is observable must outlast the observation latency. The
  owner's hold is now `owner-hold-ms` (10 s) and the owner is killed once the observation is done
  — `kill-tree!`, because these owners are shell shims whose real work is a `/bin/sleep` child.
- **One environment variable was doing two opposite jobs.**
  `CLJ_SURGEON_CLJ_KONDO_TIMEOUT_MS=100` was given to the owner *and* the waiter from one map.
  For the waiter it is the contract; for the owner it is a race against the box, and at load 31
  the owner's own admission could not complete in 100 ms, so it exited 75, never took the lock,
  and the waiter was **correctly** admitted. Split into `owner-environment` and
  `waiter-environment`.

**The general form, and the thing to carry out of this round:** *a test that fails under load is
not usually asserting something too strict; it is usually asserting something about the machine
while believing it is asserting something about the code.*

## Sabotage — every witness watched to fail, by name

An audit nobody has watched fail is an opinion. Each defect reintroduced on a `git archive` copy
of the tip.

**A — `mcp-schema-test`'s `{:lane :fast}` ns metadata removed.** The runner refuses and never
runs a test:

```
lane-refused: 1 namespace(s) whose own ns metadata disagrees with clj-surgeon.lane-manifest (TEST-ISO-001):
```

**B — a fast-lane test planted that spawns a child process** (`clojure.java.shell/sh`, declared
`:fast` in the manifest):

```
FAIL in (no-fast-lane-namespace-spells-a-child-process) (lane_manifest_test.clj:196)
expected: (empty? offenders)
  actual: (not (empty? ([clj-surgeon.sabotageb-test "clojure\.java\.shell"])))
Ran 359 tests containing 3571 assertions.
1 failures, 0 errors.
```

This is the SOURCE-SCANNING half only, and it is a spelling check, not a proof — a helper in
another namespace defeats it. TEST-ISO-002's runtime descendant-count witness is round three.

**C — the stderr rethrow restored in `stop-child!`, run two-wide under a 12-way burner**
(load 13.84 → 26.55). Both copies red:

```
--- sabotage-c  (exit 2) ---            --- sabotage-c2 (exit 2) ---
ERROR in (a-teardown-failure-still-deletes-the-workspace) (FutureTask.java:122)
ERROR in (stop-child-reports-a-stderr-reader-failure-as-a-typed-fact) (FutureTask.java:122)
Ran 5 tests containing 29 assertions.
0 failures, 2 errors.
```

**D — TEST-ISO-006, a planted fast-lane test that reads and writes the real home**, on an
archive copy of the GREEN tip. Both plants failed and the real home was untouched:

```
FAIL in (reads-the-real-home) (sabotage006_test.clj:7)
expected: (.exists (io/file (System/getProperty "user.home") ".m2"))
  actual: false
FAIL in (writes-into-the-real-home) (sabotage006_test.clj:13)
expected: (.exists (io/file "/home/forge" "sabotage006.txt"))
  actual: false
Ran 355 tests containing 3555 assertions.
2 failures, 0 errors.

$ ls -la /home/forge/sabotage006.txt
ls: cannot access '/home/forge/sabotage006.txt': No such file or directory
```

The same plant run against the RED tip (before `64086cd3`) DID write
`/home/forge/sabotage006.txt` — removed immediately — which is the counterfactual that makes the
green mean something.

## Trunk

```
$ git merge-tree --write-tree 63d317a5 origin/MCP/main
CONFLICT (content): Merge conflict in Makefile
```

Trunk `69c859c6`, **77 commits ahead** of this branch's base. **One conflict, textual and
trivial:** the `.PHONY` line, where this branch added `test-integration test-battery test-bb
suite-concurrency-battery` and trunk added `admit-transaction-recovery-battery`. The union of
both is the resolution. `test/clj_surgeon/admit_patch_test.clj` auto-merges. Nothing merges from
this seat.

## Receipts

- Tip `63d317a5`. Per-item shas in the RED→GREEN table above.
- Harness: `test/suite_concurrency_battery.sh` (TEST-ISO-009),
  `dev/experiments/contention_witness.sh` (the race witnesses).
- Intents: `docs/intent/test-isolation/` — 001, 001a, 001b, 001c, 006, 009, RACE-001, RACE-002
  implemented with witnesses named; 002-005, 007, 008, 010-012 filed for round three. The
  repository's own intent-contract audit caught the new document before it was registered, which
  is the mechanism working.
- Fixtures: `/var/tmp/forge/suite-spike-fx` and `/var/tmp/forge/suite-battery-fx`, removed at the
  end of the session; nothing under `/tmp`.
