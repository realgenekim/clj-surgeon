# Suite spike, round four — the six runtime purity witnesses (2026-09-04, forge@anvil)

Spec: `2026-09-04-suite-spike-spec.md`. Rounds one–three:
`-round1.md`, `-round2.md`, `-round3.md`. Round three closed with the six
runtime witnesses open and named the reason a source scan cannot buy them.
Branch `bridge/suite-spike`, base `105f4b6f`, tip **`9e93d2247a01aaa768006c8a26889ed5a702c8d6`**. Written 2026-09-04T23:40:51Z.

## The table

| gate | result | wall | load |
|---|---|---:|---:|
| `make test-fast` (39 ns) | **393 tests, 3 763 assertions, 0 failures, 0 errors, 0 isolation violations** | **34.37 s** | 1.59 |
| `make mcp-test` = fast+integration (43 ns) | 464 tests, 4 517 assertions, 0 failures, 0 errors, 0 isolation violations | 142.24 s | ~4 |
| `make test-bb` (bb lane, untouched) | 840 tests, 6 921 assertions, 0 failures, 0 errors | 146.15 s | ~4 |
| `make mcp-operation-oracle` | `mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]` | 1.39 s | ~4 |
| `make repository-hygiene` | `repository hygiene: no machine-local build cache is tracked at any depth` | 1.36 s | ~4 |
| intent audit (in `test-fast`) | green — every TEST-ISO marker registered, every `[x]` claimed | — | — |
| **`make suite-concurrency-battery N=4`** | **NOT RUN — owed** (no window long enough) | — | — |
| `make test-battery` | not run this round (unchanged by this work) | — | — |

Round three's comparable numbers, for the cost of the fixture: `test-fast`
31.82 s at load 8.69 (38 ns), `mcp-test` 154.50 s at load 8.17 (42 ns). The
loads differ by more than the walls do, so **the honest statement is that the
per-namespace snapshot did not move either lane out of its budget**, not that
it cost 2.5 s. Both are single observations under contention.

## What was built

ONE mechanism, six witnesses. `clj-surgeon.ns-isolation/probe` photographs
every watched resource; `clj-surgeon.mcp-test-runner` now runs each namespace
with `test-ns` between a pair of snapshots instead of running the whole lane
inside one `run-tests`; `violations` folds the difference into typed refusals
naming the intent, the namespace and the resource.

| intent | subject | probe |
|---|---|---|
| TEST-ISO-002 | process spawn | `ProcessHandle.current().descendants()` diff; fails with PID **and command line** |
| TEST-ISO-003 | writes | diff of the run temp root, `target/`, and the working tree; fails with the path |
| TEST-ISO-004 | ports | listening sockets **this process owns** (`/proc/self/fd` ∩ `/proc/net/tcp*`), plus a port-0 allocator with a ledger |
| TEST-ISO-005 | global mutation | every `clj-surgeon.*` var **root identity** + the value of any atom/ref/volatile a root IS |
| TEST-ISO-007 | time | per-namespace budget (default per LANE) and per-lane total |
| TEST-ISO-010 | threads | live **non-daemon** thread set |

**The probe emits facts; the fold emits verdicts.** That split is not
decoration: it is what makes a witness for *a child process leaked* reachable
from the fast lane without spawning a child process. `violations` is a pure
function of (namespace, before, after, opts), so the seventeen witnesses in
`clj-surgeon.ns-isolation-test` plant the `after` map a real violation would
have produced — no cold JVM, no bound socket, no day passed.

**Per-namespace attribution is the point.** A run-level answer says the suite
is dirty and leaves you to bisect 39 namespaces. This names the one that did
it, in the run that found it.

**What each lane is held to is DATA, not a silence to be inferred**
(`enforced-intents-by-lane`): fast keeps all six; integration keeps 002, 007
and 010 — binding an ephemeral port and writing a per-test workspace are what
put a namespace in it; battery keeps 007 alone, because it exists to launch
cold child JVMs. A witness that fires on a lane's own definition is not a
ratchet; it is noise that teaches people to delete witnesses.

## RED: the first live run was red on the TREE, not on a plant

The mechanism's first run against the real fast lane produced **49 violations**
and one probe defect, before any violation was planted. Verbatim, deduplicated:

```
Execution error (IOException) at java.io.FileInputStream/available0. Invalid argument
```
**A defect in the probe itself, found before a single test ran.** `slurp` on
`/proc/net/tcp` throws — a procfs entry reports zero length and does not answer
`available()`. Every namespace would have errored. Read through
`Files/readAllBytes` instead, absent-file tolerant, so a non-Linux box degrades
to *no listeners observed* rather than to an exception inside every window.

```
TEST-ISO-005 VIOLATION in clj-surgeon.mcp-inspect-tool-test -- var root:
  #'clj-surgeon.mcp-inspect-tool/execute-inspect-in-context! has a different root
  object than before this namespace ran ...            [x45 vars in that namespace]
TEST-ISO-005 VIOLATION in clj-surgeon.mcp-inspect-tool-test -- global container:
  the value held by #'clj-surgeon.mcp-change-buffer/basis-store changed ...
TEST-ISO-005 VIOLATION in clj-surgeon.mcp-semantic-client-test -- global container:
  the value held by #'clj-surgeon.mcp-semantic-client/runtime changed ...
TEST-ISO-005 VIOLATION in clj-surgeon.workspace-onboarding-test -- global container:
  the value held by #'clj-surgeon.workspace-onboarding/cclsp-config-locks changed ...
TEST-ISO-003 VIOLATION in clj-surgeon.mcp-compact-relations-test -- temp root:
  home appeared or changed directly under java.io.tmpdir ...
```

Dispositions, each with its reason at the pin:

- **45 var roots in `mcp-inspect-tool`** — real and deliberate:
  `handler-namespace-reload-preserves-the-live-runtime` calls
  `(require ... :reload)`, and reloading the handler IS its subject.
  `declared-namespace-reloads` exempts a **named production namespace for a
  named test namespace** — never a test namespace wholesale.
- **three global containers** — a prepared-change cache, a memoised connection
  handle, an interning table of per-path locks. Allowlisted, each with the
  reason it is legitimately mutable.
- **`home` under the temp root** — structural: the throwaway `user.home`
  TEST-ISO-006 launches the JVM on. Owned by the RUN, by no namespace. Read
  from `tmp-leak-support/isolated-home-name` rather than spelled, because a
  witness built on a spelling of another mechanism's name reports that
  mechanism's *rename* as a purity violation.

Then the merge gate produced a sixth finding on its own first run:

```
TEST-ISO-007 VIOLATION in clj-surgeon.mcp-hot-verify-test -- time budget:
  ran for 10128 ms, over its 8000 ms budget
```
Correct arithmetic, wrong rule: that namespace is `:integration` *because* it
drives an in-process server and waits on it. **A lane is a cost class**, so the
default is now per lane — fast 8 s, integration 20 s (~2x the measured 10.1 s
worst case, so contention cannot manufacture a refusal), battery 300 s.

## RED: all six against a REAL violation, through the production path

A planted `after` map proves the fold. It cannot prove the probe. On an
archive copy of the tip (`git archive HEAD`, `/var/tmp/forge/suite4-fx`) a
throwaway `:fast` namespace really spawns, really binds, really leaks. All six
fired in one run — **6 tests, 0 failures, and the suite still refused**:

```
Ran 6 tests containing 6 assertions.
0 failures, 0 errors.

TEST-ISOLATION: 9 violation(s) -- the suite's own purity rules, per namespace:
   TEST-ISO-002 VIOLATION in clj-surgeon.planted-violation-test -- process spawn: pid 2476355 is a live descendant that did not exist before this namespace ran: /usr/lib/cargo/bin/coreutils/sleep 300
   TEST-ISO-003 VIOLATION in clj-surgeon.planted-violation-test -- temp root: planted-stray-dir appeared or changed directly under java.io.tmpdir (/var/tmp/forge/clj-surgeon-suite-2470022-d33bbdda); a fast-lane namespace may write only inside its own subdir nsiso-clj-surgeon.planted-violation-test
   TEST-ISO-003 VIOLATION in clj-surgeon.planted-violation-test -- target/: target/planted-target-file.txt appeared or changed; the build output is shared by every lane running from this checkout
   TEST-ISO-003 VIOLATION in clj-surgeon.planted-violation-test -- working tree: planted-worktree-file.txt was created in the repository working tree
   TEST-ISO-004 VIOLATION in clj-surgeon.planted-violation-test -- listening socket: port 47823 is still listening after this namespace finished (NOT allocated through the port-0 allocator -- a fixed port literal is the usual cause)
   TEST-ISO-005 VIOLATION in clj-surgeon.planted-violation-test -- var root: #'clj-surgeon.ns-isolation/namespace-tmp-dir-name has a different root object than before this namespace ran -- a with-redefs, an alter-var-root or a :reload leaked out of its scope
   TEST-ISO-005 VIOLATION in clj-surgeon.planted-violation-test -- global container: the value held by #'clj-surgeon.planted-violation-test/kept changed across this namespace; if that is legitimate, add it to clj-surgeon.ns-isolation/mutable-global-allowlist WITH the reason
   TEST-ISO-007 VIOLATION in clj-surgeon.planted-violation-test -- time budget: ran for 9106 ms, over its 8000 ms budget; declare an override in clj-surgeon.ns-isolation/namespace-budget-overrides WITH the reason, or move it to a slower lane
   TEST-ISO-010 VIOLATION in clj-surgeon.planted-violation-test -- non-daemon thread: thread 42 (planted-leaked-thread) is alive and non-daemon after this namespace finished; a leaked non-daemon thread keeps the JVM from exiting even when every test passed
```

**Every test passed and the run still failed.** That is the shape the family
exists for: `clojure.test` cannot see any of these, because none of them is an
assertion.

## SABOTAGE — and the one thing it found

The sabotage is the edit somebody under deadline actually makes. `enforced`
is the single choke point every violation passes through; its body was
replaced with `(vec [])` on the same archive copy, and the identical planted
namespace re-run:

```
Ran 6 tests containing 6 assertions.
0 failures, 0 errors.

test-isolation: 0 violations across 1 namespace(s) (TEST-ISO-002/003/004/005/007/010)
```

**A two-line edit silences all six, and the line it prints is the reassuring
one.** That is the finding, and it is why the sabotage was worth running.

The ratchet already existed and it holds. Running the real witness namespace
against the sabotaged tree:

```
FAIL in (each-lane-is-held-only-to-the-rules-that-lane-can-keep) (ns_isolation_test.clj:406)
  filtering DROPS the rule but never invents one
  expected: (= 1 (count (iso/enforced :integration vs)))    actual: (not (= 1 0))
FAIL ... expected: (= "TEST-ISO-007" (:intent (first (iso/enforced :integration vs))))
FAIL ... expected: (= 2 (count (iso/enforced :fast vs)))    actual: (not (= 2 0))

Ran 17 tests containing 107 assertions.
3 failures, 0 errors.
```

| sabotage | violations still committed | fixture reported | caught by | count |
|---|---:|---|---|---:|
| none (planted namespace) | 9 | **9 refusals, all six intents** | — | 9/9 |
| `enforced` returns `[]` | 9 | `0 violations` (green-that-should-be-red) | `each-lane-is-held-only-to-the-rules-that-lane-can-keep` | **3 failures** |

This is a *reintroduce-the-defect* red, not an authoring-time red — the
distinction the marker-audit lesson turns on.

## Two exemption ceilings, both witnessed

An allowlist that can cover every class adjacent to it is an off switch, and
the first person under a deadline will use it as one.

- **The mutable-global allowlist cannot exempt a leaked var ROOT.** A root
  swap has no legitimate form; naming the var in the allowlist must not help.
  `the-allowlist-cannot-exempt-a-leaked-var-root`.
- **A declared reload is scoped to one production namespace for one test
  namespace.** A leak in any other namespace still fails, and a namespace that
  declared nothing is exempt from nothing.
  `a-declared-reload-is-exempt-for-that-namespace-and-nobody-else`.
- Both time ceilings assert **at the boundary and one ms past it**, per lane —
  an assertion far from the boundary cannot tell a correct `>` from `>=`.

## The deliberate deviation from the round-four brief, and why

The brief specified `git status --porcelain` before/after each namespace as
part of TEST-ISO-003. **It is implemented as an in-process working-tree walk
instead**, and the substitution is deliberate on two grounds:

1. `git status` is a **child process**, and running one inside the window
   TEST-ISO-002 is measuring makes the verifier blind to its own subject —
   the fixture would have to allowlist its own spawn, which is the hole.
2. The walk is **strictly stronger** for this subject: it sees creations,
   modifications AND deletions, attributed per namespace, and it also sees
   `.gitignore`d writes that `git status` reports as clean.

## What this round did NOT close

- **`make suite-concurrency-battery N=4` is OWED.** It is the spike's merge
  gate and it did not fit the window. Until it runs, the claim *the fixture is
  safe N-wide* is unproven; the fixture reads `/proc` and the working tree,
  and four clones doing that at once is exactly the condition round one found
  interference under.
- **`make test-battery`** was not re-run; the battery lane is untouched by
  this work, and its ledger receipt is round three's.
- **A write to a seat-absolute path outside every watched subject is
  invisible.** `scope_stream_test` is fixed at the unrepresentable rung
  (fixtures come from `java.io.tmpdir`, which the runner has already replaced),
  but the CLASS is not closed by detection and cannot be: a witness that
  watches subjects cannot see a write to a path that is none of them. The same
  shape as a scan that cannot see a call naming nothing.
- **Load-time side effects are outside every window.** Namespaces are
  `require`d before the first probe, because TEST-ISO-001's metadata refusal
  must be able to refuse a run before any test executes. A top-level `def`
  that binds a socket is therefore seen by no witness here.
- **`git merge-tree --write-tree HEAD origin/MCP/main` CONFLICTS.** Trunk has
  moved to `be94b8a3` (round three merged `9fceefe0`) and the conflict is in
  `test/clj_surgeon/mcp_intent_contract_test.clj` — a file this round does not
  touch. Pre-existing trunk drift, not this work; it needs a merge before
  landing.
- **TEST-ISO-008, 011, 012** remain filed.

## One caveat

Every wall here is a single observation on a box shared with 28 other seats,
inside a 18-minute capacity window, at 1-minute loads between 1.6 and 5.5. They
are upper bounds, not distributions, and the round-three comparison is across
different loads.
