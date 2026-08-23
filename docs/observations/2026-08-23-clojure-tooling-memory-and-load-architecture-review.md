# Clojure tooling is consuming the machine's failure margin

<!-- agent-usage-window-end: 2026-08-23T13:22:51.926321Z -->

**Usage window:** 2026-08-12 09:35:34–2026-08-23 13:22:51 UTC;
2026-08-12 02:35:34–2026-08-23 06:22:51 PDT

**Live process samples:** 2026-08-23 06:23–06:43 PDT. Every live process
reference below includes its current working directory (CWD), because that is
the most useful recognition aid on this machine.

## Verdict

The memory and lifecycle problem is proven. The claim that clj-surgeon alone
causes load averages near 400 is not.

At the live sample, three independently retained Clojure-tooling families had
large physical footprints:

- the shared clj-surgeon/cclsp stack—broker CWDs
  `/Users/genekim/src.local/clj-surgeon` and
  `/Users/genekim/src.local/cclsp-structural-results`, plus the 17 child CWDs
  in the appendix—at about **6.7 GiB**;
- three persistent project nREPLs—two with CWD
  `/Users/genekim/src.local/sessionize-sched-killer` and one with CWD
  `/Users/genekim/src.local/curtain-call-sheets-form`—at **6.453 GiB** in the
  exact family `footprint` snapshot (about 6.6 GiB in the earlier estimate);
  and
- 21 clojure-mcp-backed agent servers—CWDs
  `/Users/genekim/src.local/itrev-mcp-server`,
  `/Users/genekim/src.local/sessionize-sched-killer`, and
  `/Users/genekim/src.local/gaiwan/does/video-publisher`—at **10.137 GiB**.

Those family measurements must not be added as if they were an exact resident
set: macOS footprint includes compressed memory, libraries are shared, and the
snapshots were not one atomic sample. They are nevertheless valid family
rankings. On a 24 GiB machine whose live incident had 23 GiB used, only 166 MiB
unused, 6.68 GiB compressed, and 26.125 GiB of swap used, retaining even one of
these families materially removes failure margin.

The sampled JVMs and clojure-lsp children were CPU-idle. They prove retained
memory and failed ownership, not the immediate CPU initiator. The non-Clojure
leaders and the available or unavailable CWD evidence for each are enumerated
in the CPU section below. The correct causal claim is therefore:

> Our Clojure tooling creates a large, persistent memory-pressure floor. The
> evidence is consistent with that floor amplifying unrelated bursts into
> compression, swap, and scheduler emergencies. Semantic fan-out and
> initialization can also create their own bursts, but the present evidence
> does not assign every load-400 event to clj-surgeon.

## What the three `java` times actually are

They are not clj-surgeon and they are not cclsp. They are full project nREPL
JVMs running `nrepl.cmdline`, listening only on localhost:

| PID | CWD | Port | Age | CPU total/current | Current / peak footprint | Heap committed / used / max |
|---:|---|---:|---:|---:|---:|---:|
| 71419 | `/Users/genekim/src.local/sessionize-sched-killer` | 53202 | 5d 8h | 10:54 / 0.0% | 2.521 / 3.669 GiB | 2.191 / 0.581 / 6.000 GiB |
| 69194 | `/Users/genekim/src.local/sessionize-sched-killer` | 59056 | 1d 21h | 5:12 / 0.0% | 2.119 / 2.829 GiB | 1.824 / 0.547 / 6.000 GiB |
| 17014 | `/Users/genekim/src.local/curtain-call-sheets-form` | 64202 | 4d 7h | 7:16 / 0.0% | 1.813 / 1.860 GiB | 1.582 / 0.135 / 6.000 GiB |

Together they had a 6.453 GiB family physical footprint and 5.598 GiB of
committed heap holding only 1.263 GiB of used heap. None has an explicit heap
cap, so each advertises a 6 GiB maximum heap—18 GiB of possible heap growth
across the three.

They exist for a good reason: keep the application/test classpath warm, retain
loaded namespaces and helper definitions, and make `clojure_eval` or
`clj-nrepl-eval` cheap. Their current lifecycle is the bug. Two nREPLs serve
the same CWD, their launching `make`/shell ancestry outlived the initiating
agent, and nothing enforces one listener per runtime, a heap budget, an active
lease, or an idle expiry. CWD
`/Users/genekim/src.local/sessionize-sched-killer` has no `.nrepl-port`, so its
two listeners are not even discoverable through that repo's advertised port
file. CWD `/Users/genekim/src.local/curtain-call-sheets-form` has a
`.nrepl-port` containing 64202, which correctly identifies PID 17014.

### Outside-in tests versus nREPL

Outside-in testing and nREPL answer different questions. Outside-in tests
prove the delivered boundary—fresh process, real command, filesystem and
classpath assumptions, formatting, exit status, and user-visible result.
nREPL is the preferable hot inner loop once the boundary has failed narrowly:
load the changed namespace, probe the real transformation against real values,
rerun a focused test, and keep diagnostic helpers alive.

The critique is that we currently pay for persistent nREPLs without consistently
routing the inner loop through them. That is the worst combination: cold
outside-in latency plus hot-JVM memory. The target loop should be:

`one outside-in failure -> many nREPL probes/focused tests -> one outside-in acceptance run`.

An nREPL earns persistence only while an active repo lease exists or its reuse
rate clears a measured threshold. Production/test-process semantics that rely
on clean startup remain outside-in authority.

## Live resource inventory

### Shared structural and semantic stack

| Process family | CWD | Count | Current physical footprint | CPU/lifecycle evidence |
|---|---|---:|---:|---|
| clj-surgeon Java server, PID 48029 | `/Users/genekim/src.local/clj-surgeon` | 1 | 1.4 GiB; 2.0 GiB historical peak | 0.0% current; `-Xms64m -Xmx2g`; heap committed 1.29 GiB, used about 313 MiB |
| cclsp Bun broker, PID 48012 | `/Users/genekim/src.local/cclsp-structural-results` | 1 | 86.4 MiB; 90.4 MiB peak | 0.0% current |
| cclsp-owned `clojure-lsp` native children | each child's project CWD; see appendix | 17 | 5,278.3 MiB summed current footprint; mean 310.5 MiB | all ready and idle; 74.6 cumulative CPU minutes across multi-day lives |

The 17 individual historical peaks sum to 10,387.9 MiB, but those peaks did
not necessarily coincide. The current per-process footprint sum is useful for
ranking the retained family, though shared pages prevent treating it as exact
machine-wide consumption.

cclsp currently knows 39 workspace roots. Sixteen no longer exist, 22 roots
are cold, and 17 have a retained child. All 17 children had zero active,
outstanding, or queued requests at the sample. Twelve children were launched
within about 23 ms on 2026-08-17. The current cclsp command does **not** contain
`--preload`; “cclsp starts every daemon at boot” is false today. A semantic
request caused the fan-out.

### clojure-mcp-backed agent servers

There are two distinct populations; calling all of them project
`clojure-mcp` would be misleading.

| Family | CWD | PIDs/count | Current physical footprint | Current / cumulative CPU | Lifecycle evidence |
|---|---|---|---:|---:|---|
| global Claude `itrev-corpus` stdio app, implemented with clojure-mcp | `/Users/genekim/src.local/itrev-mcp-server` | 15 | 6.061 GiB; individual peak sum 7.206 GiB | 0.0% / 1:21:45 | six Java PIDs have PPID 1; one app JVM starts per Claude session |
| project `clojure-mcp :cli-assist` | `/Users/genekim/src.local/sessionize-sched-killer` | PIDs 8922, 19874, 96181, 25546, 79022 | part of 4.076 GiB current / 4.403 GiB individual-peak-sum family | 0.0% / part of 0:34:13 | five simultaneous façades for one repo CWD |
| project `clojure-mcp :cli-assist` | `/Users/genekim/src.local/gaiwan/does/video-publisher` | PID 90433 | part of 4.076 GiB current / 4.403 GiB individual-peak-sum family | 0.0% / part of 0:34:13 | one façade |

The 15 `itrev-corpus` PIDs were 8921, 71083, 7332, 9796, 11114, 15783,
16034, 19873, 90432, 96179, 25544, 79021, 41413, 7595, and 43725; every one
had CWD `/Users/genekim/src.local/itrev-mcp-server`. The PPID-1 subset was
71083, 7332, 9796, 11114, 16034, and 43725, with that same CWD.

Across all 21 JVMs, current family footprint was 10.137 GiB and the sum of
each process's own historical peak was 11.609 GiB. The peak sum does not prove
that all peaks coincided.

The global Claude registration explains the first population: it runs
`clojure -J-Xmx512m -X:serve` after changing to CWD
`/Users/genekim/src.local/itrev-mcp-server`. Eighteen Claude project entries
register `clojure-mcp`; the six live `:cli-assist` instances are Claude-owned.
Codex's global `:cli-assist` entry is disabled.

These are stdio MCP children created at agent startup. Their stdin/stdout pipe
is part of the owning Claude or Codex session's tool channel. Killing a live
child may leave that session unable to use the tool and may require restarting
the entire agent session; current clients cannot be assumed to respawn or
reattach it. Therefore an idle process is not safe to kill merely because its
CPU is zero. Safe reaping requires client EOF, verified parent/session death,
or an explicit cooperative detach. Migration must drain existing children and
change only new-session registrations.

A follow-up safe-reap audit at 2026-08-23T14:16:48Z found **zero eligible
children**, so no process was killed. Each of the six live `:cli-assist` JVMs
still had a live Claude ancestor: PIDs 8922, 19874, 25546, 79022, and 96181 had
CWD `/Users/genekim/src.local/sessionize-sched-killer`; PID 90433 had CWD
`/Users/genekim/src.local/gaiwan/does/video-publisher`. PID 8922 was owned
through live `claude bg-spare`/`bg-pty-host` ancestors; the other five were
owned by live interactive Claude ancestry under ZMX. Age and zero CPU were not
treated as proof of dead ownership.

The verified Claude Code skill
`/Users/genekim/.claude/skills/reap-dead-agent-jvms/SKILL.md` now makes this
audit reusable. Its default is a JSON dry run. Apply requires the exact
confirmation `dead-agent-owners-only`, rescans PID start identity, command,
CWD, agent-origin markers, ancestry, and Unix-socket peers, sends only
`TERM`, and never escalates to `KILL`.

### `:cli-assist` is a façade, not the nREPL

The installed profile advertises `clojure_eval`, `clojure_edit`, and
`list_nrepl_ports`, plus project information. In this installed revision it
also leaves newer `paren_repair` and `deps_*` tools enabled. “Minimal” applies
mostly to the visible tool surface: startup first requires and constructs all
tool namespaces—including the heavier agent/langchain builders—and filters
components afterward.

The six live `:cli-assist` processes do not auto-start nREPL. Neither live
project supplies `:start-nrepl-cmd`; these 4.076 GiB are MCP protocol façades
waiting to discover and connect to a different JVM. Removing the registration
does not remove the project nREPL.

That opens a strong replacement route. Keep one bounded repo nREPL; invoke
`/Users/genekim/.local/bin/clj-nrepl-eval --discover-ports` or read the repo's
`.nrepl-port`; use clj-surgeon/native patch for editing; and keep delimiter
repair as a formatter/parinfer step. A small typed `clojure-eval` wrapper that
reads code from stdin can preserve MCP's real ergonomic advantage—safe
payload/quoting and a model-visible eval affordance—without a JVM per agent.

Removing repo registrations for new sessions would avoid this multiplication
and recover the measured 4.076 GiB only after existing owning sessions end and
their live children drain. It would not affect the larger 6.061 GiB
`itrev-corpus` family. The global app must separately become a shared HTTP
service or obey parent-death/session leases.

## How our architecture creates retention and bursts

The live state is explained by three concrete mechanisms:

1. clj-surgeon workspace onboarding upserts roots into cclsp configuration but
   does not prune old roots or expired worktrees. The configuration therefore
   grows across work rather than representing live work.
2. cclsp caches every started child in its server-manager map. A child exits
   only when its configuration is removed, the broker is disposed, or an
   explicit restart occurs. There is no idle TTL, LRU bound, or lease owner.
3. `resolve_var_surface` first shortlists source roots, but a miss falls back
   to every configured root and issues `workspace/symbol` calls with
   `Promise.all`. Cold roots can therefore initialize concurrently and then
   remain resident indefinitely.

The same ownership defect recurs outside cclsp: stdio MCP servers outlive
their clients, agent sessions each start another façade, project nREPLs and
their `make`/shell wrappers outlive the initiating agent without a lease, and
even this study accidentally launched two expensive collectors in parallel
until the duplicate was interrupted. These are not isolated memory-tuning
errors. The architecture lacks a system-wide invariant:

> Every persistent worker has one discoverable owner, a bounded resource
> budget, an active lease, and a deterministic reap path.

## Implemented convergence on 2026-08-23

The selected correction is now implemented: four active workspace leases,
eight warm residents, one initializer, a ten-minute TTL, and idle-only
deterministic LRU. The full contract, alternatives, linked intents, tests, and
live receipts are recorded in the
[bounded cclsp workspace lifecycle HLD](../plans/bounded-cclsp-workspace-lifecycle.md).

The hot-restarted shared broker (PID 3893, CWD
`/Users/genekim/src.local/cclsp-structural-results`) initially reported zero
resident children rather than the prior 17. After one real semantic request it
reported one ready worker (PID 26377, CWD
`/Users/genekim/src.local/clj-surgeon`), zero leases, zero outstanding
requests, and about 430 MB physical footprint. The broker itself measured about
52 MB. Onboarding also pruned 16 missing managed roots, reducing configured
CWDs from 39 to 23 without removing any existing workspace directory.

The separate session-local worker remains intentionally protected: PID 7668,
CWD `/Users/genekim/src.local/social-media-writer`, owned by cclsp PID 7596,
CWD `/Users/genekim/src.local/social-media-writer`, whose coding-agent parent
is live. The dead-owner `:cli-assist` audit likewise found zero eligible JVMs;
no agent session was interrupted to obtain the reduction.

## CPU and load: what is and is not proven

The live diagnostic incident `db3253c8-b416-4e79-b762-7fa8939487b3` recorded
load 14.35/9.59/7.95 on eight logical CPUs, 91.4% CPU busy, 6.68 GiB compressed,
and 26.125 GiB swap used. The immediate sustained CPU leader was a Microsoft
Edge renderer, PID 13455 with CWD `/`, at up to 229%, not one of the Clojure
processes above.

An earlier 2026-08-21 incident recorded load 49.66/104.75/177.31 and 31.05 GiB
swap used; its sampled leaders included Mothership Java PID 807 (the still-live
process has CWD `/Users/genekim/src.local/mothership`), Teams, WindowServer,
and concurrent `gh` clients. The preserved incident did not record CWDs for
those other processes. A separate 2026-08-16 reconstruction observed a peak
near 302 with ZoomClips, WindowServer, Supacode, Chrome, and a runaway `ugrep`
prominent; that historical reconstruction did not preserve their CWDs.

One earlier quasi-experiment supports memory-pressure amplification without
proving load-400 attribution. After Chrome was stopped and roughly 6 GiB was
released, the same class of Surgeon write transaction fell from about 83
seconds to 9 seconds, while a focused cold Kaocha run fell from several minutes
to about 10 seconds. The observation was not controlled—Chrome removal changed
CPU demand as well as memory—but the order-of-magnitude step is consistent with
GC/paging pressure dominating tool latency under saturation.

Idle memory can still be causal without appearing as the top `%CPU` process.
It reduces file-cache and free-memory headroom, leaves more pages to compress
or swap during a burst, and can move work into kernel/system CPU. Load average
also counts runnable and some blocked work rather than measuring one process's
CPU percentage. To prove the stronger load-400 claim, we need time-aligned
sampling of PID, PID start time, CWD, CPU, footprint, swap/compressor deltas,
LSP initialization/queue events, and agent actions through the onset of an
incident.

## Eleven-day usage and value evidence

The privacy-safe receipt was collected once from the prior observation marker;
the duplicate collector was stopped rather than allowed to compete for another
full scan.

| Measure | Codex | Claude |
|---|---:|---:|
| Sessions in window | 186 | 309 |
| Clojure-relevant sessions | 83 | 107 |
| Skill visible in relevant sessions | 83 | 93 |
| Skill loaded | 75 | 0 |
| Surgeon calls | 6,930 | 3 (`:help` only) |
| Surgeon reads / plans / applies | 4,476 / 115 / 611 | 3 / 0 / 0 |
| Native reads / patches | 7,081 / 1,925 | 4,183 / 670 edits |
| Live probes | 1,398 | 476 |

Codex used Surgeon in 304 of 963 recorded task turns (31.6%). The service itself
handled 2,792 calls: 2,327 succeeded and 465 failed closed, with 147 ms median
and 2.841 s p90 wall. It returned 9.0 million source characters from 4,252 file
reads. Across 299 completed Surgeon-using turns, direct Surgeon action wall was
about 1.38 hours inside 165.6 hours of complete turn wall; the median recorded
share was 0.7%. Surgeon-using turns were much longer than non-Surgeon turns,
but task selection is confounded—complex Clojure work invokes Surgeon—so this
is not evidence that Surgeon causes the difference.

cclsp served only 133 MCP calls in the same window: 97
`resolve_var_surface` and 36 reference requests. Those expanded into 356 LSP
requests across 46 workspace keys. Eighty-five timed out, including 11 of 63
initializations and 72 of 278 `workspace/symbol` calls. Initialization median
was 18.7 seconds and p90 about 120 seconds. The semantic layer therefore has a
large retained footprint, modest observed use, and a costly cold path.

This does not support deleting clj-surgeon reflexively. Existing matched
benchmarks show real structural wins: one six-edit prepared task completed in
27.976 seconds versus 68.932 seconds, and an exploratory six-edit task in
62.876 versus 81.730 seconds. Codex has genuine adoption and bounded structural
reads. It does support questioning the current always-hot semantic layout,
Claude-facing ROI, and per-agent `:cli-assist` JVMs.

Progress against the product goal is mixed: the structural transaction has
earned its keep for targeted, correctness-sensitive tasks, while the system as
a whole has not met the “cheap enough to disappear” operational gate. We
should optimize for net task value, not tool adoption.

## Ten architecture/layout options

The options are ordered from the best current evidence/risk tradeoff to the
most radical control. Savings are hypotheses until measured with the stated
gate.

| Rank | Architecture/layout | Expected benefit | Cost, risk, and falsifiable gate |
|---:|---|---|---|
| 1 | **Bounded semantic worker pool.** Keep shared clj-surgeon with four concurrently leased workspace roots, eight warm `clojure-lsp` children (about 2.5 GiB at the measured mean), one initializer, ten-minute idle TTL, idle-only LRU eviction, and expired-root pruning. | Preserves four-repository work plus concurrent branch/worktree reuse while attacking the measured 5.28 GiB child family and fan-out burst. | Cold reuse gets slower beyond eight recent roots. After 50 semantic tasks: resident children <=8, idle LSP footprint <=2.5 GiB, zero active workers killed, zero unbounded cross-root fan-out, and p90 complete task wall no more than 20% worse. |
| 2 | **One leased nREPL per live repo/runtime profile; delete per-project `:cli-assist` for new sessions.** Add a tiny stdin-safe eval/discovery wrapper and make agents reuse the listener keyed by canonical CWD plus an explicit runtime profile; let existing stdio children drain with their sessions. | Saves the measured 4.076 GiB façade family while making nREPL the real hot inner loop; eliminates accidental duplicate same-CWD/profile nREPLs while allowing intentionally distinct app/test runtimes. | Lose MCP's ready-made eval schema and rarely used deps tools. Canary CWD `/Users/genekim/src.local/sessionize-sched-killer`: exactly one default-profile listener, zero `:cli-assist` JVMs in newly started sessions, zero forced session restarts, >=95% live-probe success, no quoting regressions, and no lower REPL-use frequency. |
| 3 | **Give all agent services leases and parent-death semantics.** Exit on stdio EOF or verified owner PID/start-time death; use process groups, exact-PID reap, and a supervisor ledger. Apply separately to global `itrev-corpus` at CWD `/Users/genekim/src.local/itrev-mcp-server`. An idle TTL may request cooperative detach but must not sever a live stdio channel. | Removes PPID-1 orphans and session multiplication; could recover its measured 6.061 GiB retained family. | Blind Java or idle-child killing is unsafe and may force an agent-session restart. Gate: zero orphan JVMs after 100 agent exits, zero active channels killed, zero forced session restarts, and every reap attributable by PID/start time/CWD and terminal lease evidence. |
| 4 | **Subsume cclsp into clj-surgeon—but still bound workers.** One service owns syntax, root registry, semantic scheduling, telemetry, TTL, and cancellation. | Removes a daemon/protocol hop and makes ownership enforceable at the transaction boundary. | Merging without a pool merely hides the same 17 children. Require semantic fixture parity, >=15% p50 latency improvement, and the rank-1 memory bounds before migration. |
| 5 | **Static semantic index first.** Maintain a persistent clj-kondo-derived definitions/references index; start LSP only for call hierarchy or uncertainty. | Most structural/reference questions avoid a JVM/native daemon per CWD; likely <500 MiB. | May miss macro/runtime semantics. Gate against a 200-Var corpus: >=99% agreement, zero stale answers after content-hash changes, typed uncertainty rather than false certainty. |
| 6 | **Structural MCP default; semantic mode explicitly leased.** Ordinary clj-surgeon calls never touch cclsp; a task opts into a short semantic lease and sees its cost. | Aligns cost with the 133 semantic calls rather than keeping 17 roots hot. | Cold semantic requests become visible. Gate: ordinary tasks start zero LSP children; semantic cold p90 <=15 seconds and warm p90 <=2 seconds. |
| 7 | **One stable shared HTTP `clojure-mcp`/eval broker.** Retain model-visible MCP ergonomics, but multiplex agents by an explicit canonical CWD on every request to the one repo nREPL; similarly make `itrev-corpus` one shared HTTP daemon. Put a stable supervisor/address in front so worker replacement does not invalidate every agent's configured endpoint. | Removes JVM-per-agent without moving all the way back to shell CLI and decouples service lifetime from any one session. | Requires request isolation, namespace/session handling, authentication, and reconnect behavior. Gate: no cross-CWD state leak in adversarial concurrency tests, zero agent-session restarts during broker worker replacement, and >=8 GiB reduction from the two current MCP-backed families. |
| 8 | **CLI-only clj-surgeon plus nREPL.** Stop the persistent Surgeon JVM; invoke a preferably native/Babashka structural CLI per transaction, and use the leased repo nREPL for live semantics. | Near-zero idle structural footprint and simple ownership. | Existing CLI/skill experiments sometimes lost badly; startup and repeated parsing may erase gains. Require refusal/correctness parity and complete task wall/action count no more than 15% worse on matched tasks. |
| 9 | **Remote semantic service.** Run indexed semantic workers on another host, cache by repo commit/content hash, and keep only structural parsing plus nREPL local. | Moves multi-gigabyte LSP footprint and initialization CPU off the laptop. | Network, privacy, dirty-worktree synchronization, and availability complexity. Gate zero stale dirty-file answers, explicit offline degradation, and local idle tooling <3 GiB. |
| 10 | **No clj-surgeon control lane.** Use native bounded reads, patches, formatter, tests, and leased nREPL; no Surgeon, cclsp, or project clojure-mcp. | Establishes the maximum memory saving and tests whether the whole product is worth its complexity. | Likely loses guarded structural transactions and increases malformed/wrong-form edits. Run a two-week matched canary; reject if byte/correctness/refusal regressions occur or task wall/actions worsen >10%. |

## Recommended sequence

1. **Canary the cheap deletion.** In CWD
   `/Users/genekim/src.local/sessionize-sched-killer`, remove project
   `clojure-mcp :cli-assist` registration for new sessions only, retain exactly
   one heap-capped default-profile nREPL for the canary while preserving any
   explicitly named distinct runtime profiles, and route eval through an
   stdin-safe wrapper. Do not kill the five live children or force their owning
   sessions to restart; let them drain. Measure use and correctness for a week
   before removing the other 17 registrations.
2. **Put ownership before tuning.** Reap on client EOF/parent death, add
   PID-start-time/CWD leases, cap nREPL heap after a representative test, and
   make diagnostics globally singleton. Do not “kill child Clojure” by name,
   age, or apparent idleness, and make zero forced agent-session restarts an
   acceptance gate.
3. **Bound cclsp immediately.** Prune missing managed roots, eliminate the
   all-configured fallback, cap initialization concurrency at one, admit at
   most four concurrently leased roots, retain at most eight warm LSPs, and
   evict only idle children.
4. **Fix the larger agent-server population separately.** Convert the global
   `itrev-corpus` app at CWD
   `/Users/genekim/src.local/itrev-mcp-server` to a shared daemon or lease it to
   each client. Repo registration cleanup does not touch it.
5. **Then decide the product boundary.** Compare bounded full stack, static
   index, CLI-only, and no-Surgeon lanes on the same tasks. Subsuming cclsp is
   worthwhile only if it also subsumes lifecycle responsibility.

The smallest falsifiable improvement is rank 2's one-repo canary because it
removes a measured redundant layer without killing the useful nREPL. The
smallest semantic improvement is rank 1: prune dead roots and enforce the
four-active/eight-warm pool. Together, the success target is idle Clojure tooling below 3 GiB,
zero orphan JVMs, one nREPL per active `(canonical CWD, runtime profile)`, lower
swap-growth and p95 load during matched work, and unchanged correctness.

If severe load spikes recur while those invariants hold and sampled Clojure
tooling remains below 3 GiB, the claim that this architecture materially
amplifies the incidents is weakened. That is the experiment that can actually
disprove this report.

## Adversarial review

Codex Sol's adversarial review rejected the strongest causal wording and
forced three distinctions retained here: retained footprint is not current
CPU; deleting project `:cli-assist` does not delete the global `itrev-corpus`
population; and subsuming cclsp without worker bounds is daemon consolidation,
not resource control. It recommended the bounded pool and leased nREPL as the
first two choices.

Fable received the review request but declined to act on an inter-agent
message without direct authorization in Fable's own channel. Gene subsequently
canceled the Fable analysis and asked to finish with the evidence already
collected. No Fable technical verdict is attributed here, no Fable process or
seat was killed, and no unperformed review is presented as consensus.

## Method, evidence, and limitations

The evidence joins:

- privacy-safe usage receipt
  `/tmp/clj-surgeon-agent-usage-20260812T093534079971Z-20260823T132251926321Z.json`;
- `make cclsp-status` as runtime root/child authority;
- `ps`, `lsof`, `jcmd`, `vmmap`, and `footprint` live samples, always recording
  CWD for recognition;
- durable scheduler incident
  `db3253c8-b416-4e79-b762-7fa8939487b3` and the sibling
  `sessionize-sched-killer` diagnostic ledger;
- the sibling
  [`sessionize-sched-killer` memory-pressure observation](../../../sessionize-sched-killer/logs/2026-08-10-captains-log-toward-a-sublime-clojure-environment.md),
  treated as a quasi-experiment rather than load-400 proof;
- source/config traces through clj-surgeon onboarding and the cclsp server
  manager/symbol fallback; and
- the existing matched benchmarks in [`docs/vision.md`](../vision.md) and
  earlier inventory in [`docs/memory-usage.md`](../memory-usage.md).

No service, nREPL, LSP child, or agent MCP server was killed for this study.
Only our own accidentally duplicated usage collector was interrupted. No repo
registration was changed. Current footprint is not identical to RSS, aggregate
service wall can overlap, task populations are not matched, and historical
per-process peaks are not simultaneous-family peaks. Those limitations narrow
the causal claim; they do not erase the measured retention or the ownership
mechanisms.

## Implementation decision update — 2026-08-23

Gene selected the refined `pool-4-active-8-warm` layout after the PID/CWD
inventory made the worktree dimension visible. The hard count is eight
residents; the observed mean of about 310 MiB makes that an operational target
near 2.5 GiB. Four distinct CWDs may hold semantic leases concurrently, a
fifth waits, and initialization concurrency is one. A worker is reapable only
when it has no lease, is not initializing, and has no outstanding request.

The implementation is owned by
[`docs/plans/bounded-cclsp-workspace-lifecycle.md`](../plans/bounded-cclsp-workspace-lifecycle.md)
and durable issue `clj-surgeon-dkz`. Linked intents and a retained Prolog
shadow oracle prevent later refactors from redefining age or zero CPU as proof
that a live agent's worker is safe to kill.

## Appendix: live cclsp child PIDs and CWDs

Every child below was ready and idle at the sample:

| PID | CWD |
|---:|---|
| 57802 | `/Users/genekim/src.local/social-media-writer` |
| 10712 | `/Users/genekim/src.local/social-media-writer-worktrees/distillery-outline-zoom` |
| 22073 | `/Users/genekim/src.local/sessionize-sched-killer` |
| 10710 | `/Users/genekim/src.local/social-media-writer-draft-preview` |
| 93424 | `/Users/genekim/src.local/curtaincall-cfp3-reconcile` |
| 93422 | `/Users/genekim/src.local/curtain-call-blind-notify` |
| 93423 | `/Users/genekim/src.local/curtain-call-staging` |
| 93426 | `/Users/genekim/src.local/sessionize-homepage-md.8Tml6p` |
| 93416 | `/Users/genekim/src.local/cc-home-story.fte5BF` |
| 93432 | `/Users/genekim/src.local/ssk-jobs` |
| 93436 | `/Users/genekim/src.local/ssk-papercuts` |
| 93435 | `/Users/genekim/src.local/ssk-merger-read` |
| 93430 | `/Users/genekim/src.local/ssk-deploy-read` |
| 93444 | `/Users/genekim/src.local/ssk-ship-read` |
| 93440 | `/Users/genekim/src.local/ssk-queue-backup` |
| 93429 | `/Users/genekim/src.local/ssk-capture-kernel` |
| 62009 | `/private/tmp/2969-hIx9fI` |
