# Captain's Log: The Analyzer Took the Lock With It

**Date:** 2026-08-27

**Incident owner:** `clj-surgeon-it1` (expands `clj-surgeon-qg2`)

**System:** Apple M2 MacBook Air, 8 logical CPUs, 24 GiB RAM

## The incident was multiplicative

At 18:11, the root Codex session launched five whole-repository clj-kondo
scans in three synchronous shell calls. The exact pattern was `1 + 2 + 2`:

1. one no-cache whole `src+test` scan in clj-surgeon: 3.79 seconds;
2. one cold and one warm whole `src+test` scan sharing a temporary cache:
   5.97 and 3.99 seconds;
3. one scan in each of two retained benchmark workspaces: 0.18 and 15.23
   seconds. The final scan reached about 1.32 GiB maximum RSS.

These analyzers were back-to-back, not concurrent. Each outer shell completed
before the next began. Load average nevertheless rose from 20.7 to 118.5
because swap, paging, Spotlight, and other runnable work overlapped and decayed
more slowly than the analyzer processes. One flight-recorder sample captured
clj-kondo PID 25583, CWD `/Users/genekim/src.local/clj-surgeon`, at about 98%
CPU and 443 MiB RSS. The machine already had about 34.9 GiB of swap occupied.

cclsp and the shared Surgeon MCP were idle during the primary interval. They
were not the cause of this spike.

The seven-day retained census exposed a second route: 11 of 12 observed
clj-kondo processes were direct children of Kaocha test JVMs. The parent
commands included `kaocha.runner unit --watch` and
`kaocha.runner unit --fail-fast`. A static test census first found 17 test vars
that launch 29 real analyzers per complete `make test`: 19 in the ordinary
runner and 10 in the MCP runner. The first zero-launch fast-runner proof found
four more namespaced CLI `:ls` integration/help tests, correcting the baseline
to 33 and the ordinary runner to 23. Removing a Makefile lint target would not
close this route because production functions inside the tests launch the
analyzer.

The first flight-recorder reconstruction also found observer amplification:

- Sessionize rich capture could launch about 117 subprocesses, including about
  34 JVM probes, for one diagnostic event. Some launchd probes also used a PATH
  that could not find `/usr/sbin/sysctl` or `/usr/sbin/lsof`.
- Mothership refreshed process vitals by starting `top -l 1 -s 0` about every
  ten seconds. A `top` child could consume 50–90% CPU for eight or nine seconds.
- Broad recursive history and workspace searches by coding agents added short,
  avoidable CPU bursts while the machine was already under pressure.

No one item explains every load spike. The recurring failure mode is several
reasonable local actions multiplying—or arriving back-to-back faster than the
machine can recover—on a swap-saturated eight-core laptop.

## The obvious lock was wrong

The first attractive design put a Java `FileChannel` lock in the Surgeon JVM:

```text
Surgeon JVM [owns lock]
    |
    +----> clj-kondo child
```

SURGEON2 falsified it with a fake analyzer. When the parent JVM died, the Java
lock disappeared while its analyzer child survived. A second caller then
acquired the lock and launched another analyzer. The design serialized parents,
not analyzer lifetimes.

The corrected design transfers authority to the process doing the expensive
work:

```text
caller
  |
  v
Python gate -- fcntl lock -- exec(exact clj-kondo)
                              |
                              +-- inherited lock descriptor
                              +-- same PID
                              +-- lock lives until analyzer exit
```

The gate uses a per-user, machine-wide `fcntl` record lock. It writes bounded
owner evidence—PID, canonical command CWD, entrance, executable, and wait
time—makes the lock descriptor inheritable, and then `exec`s the exact resolved
analyzer. Stale lock-file text is only diagnostic evidence. The operating
system lock is authority.

This is the main breakthrough: **the analyzer takes the lock with it.** Parent
death cannot create a second admission while the analyzer continues to run.

## The first production ratchet

All current Surgeon-owned analyzer entrances converge on the same wrapper:

- forward-reference and fix-declares analysis;
- local binding rename analysis;
- diagnostic, fast, and project-owned exact verification;
- cold verification jobs;
- direct agent shell use through `~/bin/clj-kondo`.

Concurrent callers wait only inside their bounded process window. If admission
expires, the wrapper exits with a typed temporary-failure result and launches
no analyzer. Verification maps that result to `unverified`; it is not a lint
finding and never grants blind-retry authority.

This is deliberately not yet a generic scheduler or persistent worker. One
machine-wide slot is the smallest mechanically safe change that removes the
O(number of simultaneous callers) load multiplier. The `1 + 2 + 2` incident
also proves serialization alone is insufficient: pressure admission and
cooldown must prevent a back-to-back convoy while the machine is still paging.

An arbitrary absolute call to `/opt/homebrew/bin/clj-kondo` remains an advisory
lock bypass. Installation and global agent routing make `~/bin/clj-kondo` the
paved shell entrance, but this version does not claim kernel-enforced control
over unrelated programs.

The gate now also refuses new work when a fresh flight-recorder sample is red
or critical, or current normalized one-minute load is red. It rechecks before
waiting, while waiting, and immediately after lock acquisition. A pressure
refusal launches no analyzer, records bounded request/CWD/scope evidence, and
does not claim that clj-kondo caused the observed pressure.

The first mutation amplifier is gone as well. `fix-declares!` used to run
clj-kondo for its plan, begin writing, and then reacquire forward-reference
analysis once per move. Execution now uses the exact `:move-before` owners from
the frozen plan and re-resolves only those owners after line shifts. Analyzer
authority is therefore acquired before the first write, never mid-mutation.

## Test integrity becomes cheaper and stronger

The corrected 33 real launches mostly repeat policy proofs. The replacement
pyramid moves combinatorial behavior to normalized provider fixtures, keeps
fake processes for process-control laws, and retains five mandatory sequential
real contracts: forward-reference planning, binding analysis, one batched move
corpus lint, a diagnostic baseline, and a future-snapshot diagnostic check.

That projects an 85% reduction for the milestone suite and zero real analyzer
launches in the everyday runner. It also adds a real binding-analysis contract
that the old suite surprisingly lacked. The detailed census and fixture plan
are in `2026-08-27-clj-kondo-trigger-taxonomy-and-test-pyramid.md`.

## Observer repairs

Two independent repairs are now live:

- Sessionize commit `5de99e9e` defers rich diagnostics at red or critical
  pressure, permits automatic rich capture only at yellow, uses absolute system
  tool paths, and prevents overlapping captures with a shared admission lock.
- Mothership commit `3c8aa3d` removes `top` from its hot vitals route and adds a
  single-flight around process-summary collection.

The live Sessionize monitor is PID 463, CWD
`/Users/genekim/src.local/sessionize-sched-killer`. The live Mothership JVM is
PID 97284, CWD `/Users/genekim/src.local/mothership`. After publication,
Mothership showed no recurring `top` child.

## Evidence earned so far

The warm bounded admission, cold-verification, rollback, and transaction
cohort is green: 30 tests and 196 assertions. The process namespace contributes
15 analyzer-boundary tests and 54 assertions. The cohort proves:

- non-overlap across concurrent JVM callers and independent wrapper processes;
- bounded timeout with no second analyzer launch;
- stale-owner recovery;
- analyzer-death lock release without deleting the lock file;
- the direct shell shim and Surgeon use the same lock;
- non-clj-kondo commands bypass admission;
- exact-verifier and cold-verifier admission loss remains `unverified`.
- admission wait and analyzer execution share one deadline;
- 30-minute cold profile deadlines cross the gate;
- exec failure is recorded rather than disappearing;
- red pressure launches no analyzer;
- `fix-declares!` performs one analysis before writing and none afterward.

The wrapper compiles with Python 3, the temporary-HOME installation witness
passes, both installed artifacts are executable, changed Clojure files conform
to Standard Clojure Style, and `git diff --check` is clean.

The first test-pyramid ratchets are now measured rather than projected. The
ordinary Babashka runner passed 625 tests and 5,342 assertions with its analyzer
event count unchanged. Eleven fix-declares policy cases consume explicit frozen
forward-reference facts, two CLI dispatch cases and four CLI integration/help
cases use namespace-free fixtures, and six move validity scans are represented
by one uniquely namespaced six-source analyzer contract. The fast lane is
`make test-fast`; the real-provider lane is `make analyzer-contract-test`; the
complete `make test` still requires both.

The MCP behavior runner then fell from ten real analyzer launches to zero. Its
diagnostic multiset policy was already exhaustively pure. The transaction test
now injects baseline/future receipts while still proving that baseline sees
original bytes, verification sees staged bytes, success can be undone, and a
new blocking finding restores the original bytes. The distinct external
contract moved to the real-provider lane as two sequential calls: one baseline
and one coherent future snapshot using the project `:lint-as` configuration.
The affected warm cohort passed 63 tests and 571 assertions with the machine
event ledger unchanged.

The explicit real-provider lane now contains exactly five launches: one batched
move corpus, one diagnostic baseline/future pair, one forward-reference schema
contract, and one binding-analysis schema contract. The four test Vars compile
in the warm 512 MiB analysis JVM, and the move corpus contains all six promised
snapshots. The five-launch lane has not run on Skiff because the recorder is
red; until it passes remotely or under a fresh-green lease,
MCP-OP-ANALYZER-008 remains open.

## One physical gate, two logical lanes

The governing throughput law is Little's Law plus the Universal Scalability
Law, not Metcalfe's Law. Concurrent CPU- and memory-heavy analyzers increase
contention and paging; on this laptop they reduce completed-work throughput.
Serializing the scarce analyzer therefore improves both stability and often
throughput. The remaining lever is to reduce launch count and useful work per
launch, not to create a second analyzer slot.

The first mission-lease implementation keeps one physical capacity lock and
adds two logical lanes. Ordinary MCP and shell requests are interactive. The
repository-owned analyzer contract is one exact test mission with a five-call,
five-minute budget and a source-derived scope hash. Every child releases the
physical lock. The next child resamples pressure after the previous exit.
Interactive callers publish bounded waiter tickets and take the priority
turnstile between test children, so a test mission cannot monopolize the box.

The first fake-process implementation failed its priority witness: relying on
POSIX lock arrival order produced `mission-one, mission-two, interactive`.
That was useful evidence. The gate now uses an explicit live, deadline-bounded
interactive ticket and the same replay produces `mission-one, interactive,
mission-two`. The focused warm cohort is 18 tests and 69 assertions, and the
temporary-HOME installed-wrapper witness remains green. No real analyzer was
launched for this loop.

The lease is not exposed through MCP or the shell shim. Its owner is the exact
repository test runner. The wrapper validates the direct parent PID, owner and
command CWDs, scope digest, launch index, fixed limit, and expiry. A sixth
launch refuses before process creation. Pressure remains an unconditional
veto.

SURGEON2's independent executable design receipt is commit `dd29b6a` on
`experiment/clj-kondo-admission-gate-design`. Its prototype is evidence, not a
second production implementation.

## Remaining gates and next hills

The first shared hot-reload proof stopped safely. The live reload manifest
loaded `binding_rename` before the new `mcp_process` dependency, so compilation
refused at `run-bounded!`. PID 65458, CWD
`/Users/genekim/src.local/clj-surgeon`, did not restart. No retry occurred.
The repair moves `mcp_process` ahead of `forward-refs`, `fix-declares`, and
`binding-rename`, adds the previously omitted analyzer namespaces, and adds a
dry-run dependency-order regression. The repaired order loaded successfully in
the isolated 512 MiB analysis nREPL before a second window was considered.

Before shared publication:

1. Complete the static entrance audit and adversarial review.
2. Run the focused MCP cohort and intent oracle from one bounded 512 MiB JVM.
3. Commit an immutable green checkpoint.
4. Install the shell gate, hot-reload the shared MCP once, and prove continuity
   with fake analyzers and a bounded live operation—not a whole-repository lint.

After serialization is stable, the next ratchet is a closed mission lease.
SURGEON2's 20-test/104-assertion pure falsifier rejected a naive 60-second delay
after every launch: it blocks the retained convoy but makes 32 logical launches
take at least 31 minutes. Raw work receives one-sample/one-launch debt. A paved
serial workflow may receive an owner/CWD/scope/count/time-bounded lease, but
every next child still requires a fresh green sample observed after the prior
child exits. Pressure never yields to the lease.

The larger cclsp question remains separate. Structural and cached evidence may
replace much of its routine traffic, but the 2026-08-27 spike shows that any
remaining analyzer/provider work needs explicit machine-level admission before
we optimize semantic coverage.
