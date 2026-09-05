# Suite spike, round three — closing the review (2026-09-04, forge@anvil)

Spec: `2026-09-04-suite-spike-spec.md`. Round one: `2026-09-04-suite-spike-round1.md`.
Round two: `2026-09-04-suite-spike-round2.md`. Round-two verdict: **NO-GO**, one blocking
finding and three required follow-ups. Branch `bridge/suite-spike`, base `2ecce8c4`,
merged with trunk `9fceefe0`, tip **`2522bd95`** (gates) — this doc and the ledger receipt land on top.

## The table

| gate | result | wall |
|---|---|---:|
| `make test-fast` (38 ns, load 8.69) | **376 tests, 3 654 assertions, 0 failures, 0 errors** | **31.82 s** |
| `make mcp-test` = fast+integration (42 ns, full target: oracle + self-tests, load 8.17) | 447 tests, 4 408 assertions, 0 failures, 0 errors | 154.50 s |
| `make test-bb` (bb lane, untouched, load 13.16) | 840 tests, 6 921 assertions, 0 failures, 0 errors | 165.02 s |
| `make test-battery` (11 ns, under `flock /home/forge/tmp/suite.lock`) | 510 tests, 11 000 assertions, 0 failures, 0 errors | 722.95 s |
| `make mcp-operation-oracle` | `mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]` | 1.62 s |
| `make repository-hygiene` | `repository hygiene: no machine-local build cache is tracked at any depth` | 1.65 s |
| intent audit | `{:ok true, :specs 426, :implementation-witnesses 408, :test-witnesses 411, :violations []}` | 3.18 s |
| **`make suite-concurrency-battery N=4`, two runs** | **2/2 PASS, 8/8 clones 447/4 408, 0 failures 0 errors** | 165 s / 165 s internal |
| `make battery-fresh`, both states | `REFUSED (no-entries)` on a fresh clone; `OK -- ... 0.2 h old, 0 commit(s) behind HEAD` after the battery | < 1 s |

## What the review asked for, and what landed

| # | review finding | what shipped |
|---|---|---|
| 10 | **BLOCKING** — `mcp-formatter-test` is an orphan: 3 green tests / 18 assertions over live production formatter paths, required by no runner and no Make target | adopted into `:fast` (cadence `:every-run`), **and the class closed**: an exclusion must now name a runner that EXISTS |
| 11 | MUST-FIX — three live descriptions and an agent brief still mean the old `test-fast` (the bb lane) | four sites corrected, plus a source witness that fails on the next one |
| 4 | the round-two headline 426 / 4 309 is stale | corrected to the tip's real 429 / 4 323, with a note saying why; round three's own counts pinned |
| 1 | the spike spec is absent from the branch | trunk merged in (one `.PHONY` conflict, resolved by union) |
| 8 | non-blocking — `scope_stream_test` asserts the collector finishes in 200 ms | replaced with a reachability deadline |
| — | spec round three | the battery **receipt ledger** and its **freshness tripwire** |

## The nothing-dropped arithmetic, recomputed

Round one measured **865 tests / 13 023 assertions across 49 namespaces**. The pin now shows
its working, in `the-corpus-only-ever-grows-and-the-arithmetic-is-shown`:

```
round one's 49 namespaces, today ........... 921 deftests   (>= the 865 it MEASURED)
adopted since round one .....................  36 deftests   (12 battery-ledger
                                                              + 18 lane-manifest
                                                              +  3 fast-lane-isolation
                                                              +  3 mcp-formatter)
                                              ---
manifest total ..............................  957 deftests
```

Three things are pinned rather than one, because each catches a different way the corpus can
shrink. A namespace leaving a lane fails `the-partition-drops-nothing-round-one-measured` **by
name**. Tests being deleted from a namespace round one measured fails the `>= 865`. A namespace
joining or leaving the corpus without a line at the pin fails the set equality — so the growth
has to be *declared*, with its count, next to the reason.

The census counts `deftest` FORMS from source, deliberately. Assertion counts are
context-sensitive — the round-two review measured 4 319 summing the lanes separately and 4 323
running them together — and **a pin that moves with the weather teaches people to re-bless it**.


## The arithmetic closes against the RUNTIME, not only the source

The pin counts `deftest` forms in source and says 957. The three lanes, run separately, report
**376 + 71 + 510 = 957 tests** (integration is `mcp-test` 447 minus `test-fast` 376). The source
census and the runner agree exactly, which is the strongest form this receipt takes: the number
the pin defends is the number the machine runs.

Round one measured 865 tests / 13 023 assertions. Round three: **957 tests / 15 408 assertions**
(3 654 + 754 + 11 000) — every one of round one's still present, +92 tests, and every namespace
that joined named at the pin with its count.

## The two ratchets, and why each is a class and not an instance

**An exclusion is a REDIRECTION, never a declaration of orphanhood.** Round two's answer to the
orphan was to declare it: an entry in `excluded` with the reason "required by no runner and no
Make target". That is genuinely better than silence — but the review's counter is right, and it
generalises: *declaring an omission makes it visible; it does not make it non-loss.* So
`every-exclusion-names-a-runner-that-actually-exists` requires each exclusion's reason to name a
`make <target>` or a `:clj-surgeon/<alias>` that is really in the tree. Written RED first, it
failed naming `mcp-formatter-test` and quoting its own reason back at it. A namespace nothing
runs can now be adopted or deleted; it can no longer be filed.

**A rename whose old meaning survives in living prose is worse than no rename.** `test-fast`
meant `bb test/run_all.clj` until 2026-09-04 and now means the JVM fast lane. The target
resolves either way, the suite is green either way, and the reader runs the wrong lane.
`no-living-prose-still-calls-the-bb-lane-by-its-old-name` scans the LIVING set for a sentence
equating the old name with a babashka spelling, exempting a window that also names `test-bb`
(that is the rename being *explained*). Historical receipts under `docs/observations` are out of
scope on purpose: they say `test-fast`, they mean the old lane, and **rewriting evidence to
match today's names is falsifying it.**

Written RED first, it named three of the four defects with file and line. **It cannot see the
fourth.** `docs/observations/2026-09-02-anvil-builder-seat-brief.md:33` said "suites via
`make test-fast` / `make mcp-test`" and names no babashka spelling at all, so it is
indistinguishable from a correct line to any scanner built on spellings. That blind spot is
recorded here rather than papered over — it is the same lesson as the scanner-brief finding:
*a source-scanning control derived from names cannot see a claim that names nothing.*

## The battery ledger and its freshness tripwire (TEST-ISO-009a/b)

Taking the eleven cold-launcher namespaces out of the merge gate is the point of the partition,
and it is also the risk. **A gate that does not run on every merge is a gate whose absence is
silent.** `make mcp-test` goes green either way, and nothing on the screen distinguishes "the
battery passed last night" from "the battery has not run since Tuesday". Gene's delivery
invariant 17 names that shape exactly: a refusal nobody hears is indistinguishable from silent
data loss.

- **The ledger.** `make test-battery` appends one line to `docs/observations/battery-ledger.edn`
  — `{:sha :started :wall_s :verdict :host}` — pass or **fail**. Append-only: it never reads what
  is already there. A ledger of successes only cannot tell you the gate is broken. The runner
  writes the file; the seat commits it, so the receipt enters history through an act that can be
  reviewed.
- **The tripwire.** `make battery-fresh` refuses when the newest receipt is older than 26 h,
  records a failure, names a sha that is not an ancestor of HEAD, is more than N=30 commits
  behind HEAD, or when the ledger is empty or has a line that does not read. Every refusal names
  its subject and its number and prints the exact remedy command.

The verdict is a **pure function** of (entries, instant, injected ancestry lookup); only the CLI
shells out to `git`. That is what puts its 12 witnesses in the fast lane: every refusal state is
reachable without a clone, a `git` call, or waiting a day. **A tripwire whose refusals can only
be reproduced by letting a day pass is a tripwire nobody ever proves.**

Both states, live:

```
$ make battery-fresh                      # STATE A: a fresh clone, no receipt yet
battery-fresh: REFUSED (no-entries) -- no battery receipt in docs/observations/battery-ledger.edn
  -- the battery lane has never recorded a run here, so nothing on this tree distinguishes
  `it passed` from `it was never run`
REMEDY: run the battery and commit its receipt --
  flock /home/forge/tmp/suite.lock make test-battery
  git add docs/observations/battery-ledger.edn && git commit
The battery is the only gate that drives the eleven cold-launcher namespaces; `make mcp-test`
cannot stand in for it.
make: *** [Makefile:1029: battery-fresh] Error 1

$ flock /home/forge/tmp/suite.lock make test-battery
Ran 510 tests containing 11000 assertions.
0 failures, 0 errors.
battery-ledger: appended {:sha "2522bd9566dce8f56ac7788f0a54ed4127d8dd89",
                          :started "2026-09-04T22:17:31Z", :wall_s 721,
                          :verdict :pass, :host "anvil-server"}
GATE_WALL=722.95 EXIT=0

$ make battery-fresh                      # STATE B: the receipt the battery just wrote
battery-fresh: OK -- newest receipt sha 2522bd9566dce8f56ac7788f0a54ed4127d8dd89,
  started 2026-09-04T22:17:31Z, wall 721s, 0.2 h old, 0 commit(s) behind HEAD

The other four refusals, driven by hand against the same target:
  REFUSED (stale)           the newest battery receipt is 48.0 h old ...; refuses past 26 h
  REFUSED (last-run-failed) ... wall 30s, verdict :fail. A failing gate is not a fresh gate.
  REFUSED (not-an-ancestor) names sha deadbeef... which is NOT an ancestor of HEAD
  REFUSED (no-entries)      (above)
```

**Writing the witness paid for itself before the mechanism ever landed.** The first live run
refused a receipt it had just written as `verdict :pass`, reporting `last-run-failed`. The cause
was `(keyword (str :pass))` = `:":pass"`, which is not `:pass` — so the tripwire would have
refused **every** receipt, forever, as a failure. A tripwire that cries wolf at 100% is worse
than none, because the first thing anyone does is stop reading it.

## What is still open

- **TEST-ISO-002…005, 007, 010** — the six per-namespace runtime witnesses on the snapshot
  fixture. Round three's spec item; not built here.
- **`scope_stream_test` writes outside its own tmpdir.** It roots its fixtures at
  `CLJ_SURGEON_MEMORY_TMP` or the literal `/home/forge/tmp`, which is a real TEST-ISO-003
  violation in a `:fast` namespace. Found while fixing finding 8, out of scope for it, and
  written down here so it is not found a third time.
- **A fast-lane namespace can depend on a spawner without the scan seeing it.**
  `battery-ledger-test` is `:fast` and requires `clj-surgeon.battery-ledger`, whose CLI half
  holds the only `ProcessBuilder` in the mechanism. No process is ever spawned — every witness
  injects the ancestry lookup, which is why the decision was written pure — but
  `no-fast-lane-namespace-spells-a-child-process` scans TEST files only, so it would not have
  noticed either way. The runtime descendant count (TEST-ISO-002) is the witness that actually
  closes this; the source scan is a spelling check and was always documented as one.
- **The nightly cron and the GHA merge** are the seat's, not this branch's. The tripwire is the
  half that makes a missing nightly loud; nothing yet makes it *run*.

## Checkpoint — what is NOT run (capacity order, 22:35Z)

Astra leads the box for four hours from here, so no further JVM suite or battery was launched
after the N=4 concurrency run that was already in flight. Every gate in this round's list has
been run once on the fresh clone and is green above. **Unrun, and owed on resume:**

- **the `test-bb` rename SABOTAGE** — reverting one corrected line and watching
  `no-living-prose-still-calls-the-bb-lane-by-its-old-name` go red. The witness was written RED
  first and named three of the four defects with file and line (that transcript is the evidence
  this round has); a deliberate re-introduction after the fix costs one ~20 s fast-lane run and
  has not been done. Until it is, this ratchet has an authoring-time red, not a
  reintroduce-the-defect red — which is exactly the distinction the marker-audit lesson turns on.
- a second and third `suite-concurrency-battery N=4` (the round asked for one; two ran).

## One caveat

The box was never quiet: 1-minute load ran 6 to 22 with 29 other seats on it throughout. Every
wall below is an upper bound measured under contention, and the gate walls are single
observations, not distributions.
