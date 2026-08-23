# Bounded cclsp workspace lifecycle

**Status:** Implemented and live-verified 2026-08-23
**Motivating issue/incidents:** `clj-surgeon-dkz`; the 2026-08-23 live
[architecture review](../observations/2026-08-23-clojure-tooling-memory-and-load-architecture-review.md)

## Outcome

The one shared cclsp service may serve many repositories and worktrees without
retaining every workspace it has ever touched. Four workspace roots may hold
semantic leases concurrently. Up to eight recently used roots remain warm.
Initialization is serialized. A ten-minute idle expiry and idle-only LRU
eviction keep the resident set bounded without killing an active coding
session's semantic worker.

The eight-worker default converts the measured mean of about 310 MiB per
`clojure-lsp` child into a target near 2.5 GiB. The portable in-process control
is the exact worker count. macOS `footprint` remains an external acceptance
measurement; cclsp must not synchronously launch platform-specific process
inspection on the semantic request path.

## Done

- Implemented and live-verified four active workspace leases, eight warm
  residents, one initializer, a ten-minute TTL, and idle-only deterministic
  LRU.
- Removed the unbounded all-configured-workspace semantic fallback. Incomplete
  source-root metadata now produces a typed refusal without starting sibling
  workers.
- Added canonical CWD, lease, queue, initialization, outstanding-request, idle,
  and eviction evidence to `inspect_runtime`.
- Added linked intent witnesses, exhaustive native policy tests, and the
  retained Prolog oracle that found the failed-worker resident-slot
  counterexample.
- Made clj-surgeon onboarding mark its Clojure entries and prune only missing
  managed or exact legacy-managed CWDs while preserving unrelated servers.
- Converged the live shared configuration from 39 roots to 23 existing CWDs.
- Verified that the live clj-surgeon MCP JVM, PID 48029 with CWD
  `/Users/genekim/src.local/clj-surgeon`, has the new onboarding behavior loaded:
  it emits `managedBy`, rejects a falsely marked non-Clojure server, and prunes
  a missing managed root.
- Installed and self-tested the fail-closed dead-agent JVM audit skill. Its
  live run found zero eligible `:cli-assist` JVMs, so no active coding session
  was signaled.

## Recommended next

1. Run the `:cli-assist` replacement canary at CWD
   `/Users/genekim/src.local/sessionize-sched-killer`: new sessions use one
   leased nREPL per `(canonical CWD, runtime profile)` plus a small stdin-safe
   `clj-nrepl-eval` wrapper; existing session-owned JVMs drain naturally.
2. Add an explicit owner ledger and parent-death/stdio-EOF semantics before
   enabling automatic `:cli-assist` reaping. Keep exact PID start identity,
   CWD, live socket owner, and `TERM`-only receipts as hard gates.
3. Convert the global clojure-mcp-backed service at CWD
   `/Users/genekim/src.local/itrev-mcp-server` into one shared HTTP daemon or
   give every stdio instance a verifiable agent lease. This is now the largest
   clearly redundant MCP-backed family.
4. Add time-aligned incident telemetry joining CWD, PID/start time, footprint,
   CPU, swap/compressor deltas, cclsp initialization/queue events, and agent
   actions. Use it to test amplification during the next scheduler incident
   rather than claiming that idle JVMs directly caused load 400.
5. After a two-week bounded-pool and nREPL canary, decide among keeping the
   split service, subsuming cclsp into clj-surgeon, CLI-only structural tooling,
   or no clj-surgeon. Choose from matched correctness, refusal quality,
   task-wall time, action count, cold starts, and retained footprint—not daemon
   count alone.

## Decision and alternatives

The full ten-option comparison remains in the architecture review. This
implementation selects its bounded-pool option, refined after the live
inventory proved that branches and worktrees—not only repositories—are distinct
workspace roots:

1. **Selected: four active / eight warm.** Four concurrent workspace leases,
   eight residents, one initializer, ten-minute TTL, idle-only LRU.
2. One or two total workers. Rejected because four repositories plus concurrent
   worktrees would turn ordinary navigation into repeated cold starts.
3. Memory-budget-only eviction. Deferred because cross-platform physical-memory
   measurement is neither cheap nor stable enough to be a request-path oracle.
4. One leased nREPL per `(canonical CWD, runtime profile)`. Complementary; it
   addresses project JVM duplication, not cclsp's native children.
5. Parent-death reaping for per-agent MCP JVMs. Complementary and intentionally
   separate from shared cclsp ownership.
6. Subsume cclsp into clj-surgeon. Deferred until the bounded lifecycle proves
   itself; daemon consolidation without bounds preserves the defect.
7. Static semantic index first. Promising later control for the modest semantic
   call volume, but requires an agreement corpus before replacing LSP evidence.
8. Structural MCP with explicitly leased semantic mode. Compatible future
   refinement after lease telemetry exists.
9. CLI-only structural tooling plus nREPL. Retained as a matched canary, not the
   first production change because current structural MCP benchmarks show value.
10. No clj-surgeon control lane. Retained as the radical ROI control.

## Bitter-Lesson Boundary

The pool owns mechanics: admission, ownership, bounded initialization,
retention, and deterministic reaping. It does not guess which repository is
important, merge worktree semantics, infer architectural relationships, or
return a semantic answer from an incomplete search.

The broad `resolve_var_surface` fallback is removed because starting every
configured root is neither search nor judgment—it is an unbounded side effect.
When source-root metadata cannot produce a bounded candidate set, the operation
returns a typed incomplete-selection refusal.

## Public contract

Defaults, each overridable by a positive integer environment value:

| Setting | Default | Meaning |
|---|---:|---|
| `CCLSP_MAX_ACTIVE_WORKSPACES` | 4 | Distinct roots holding semantic leases |
| `CCLSP_MAX_RESIDENT_WORKSPACES` | 8 | Running `clojure-lsp` children retained |
| `CCLSP_MAX_INITIALIZATIONS` | 1 | Concurrent initialization handshakes |
| `CCLSP_IDLE_TTL_MS` | 600000 | Unleased idle age before expiry |

`inspect_runtime` reports the pool limits and, for every configured workspace,
its canonical `workspace_root` (the child CWD), lease count, last-use time,
idle age, and eviction eligibility.

When the active-workspace limit is reached, a new root waits in a bounded FIFO
lease queue. When capacity is full, cclsp first reaps expired eligible workers,
then the least-recently-used eligible worker. If no worker is eligible it
refuses with `lsp-resident-capacity-exhausted`; it never kills an initializing,
leased, or outstanding worker.

Configuration reload removes and terminates children whose exact configuration
disappeared. Missing workspace directories are pruned by clj-surgeon before it
publishes the shared cclsp configuration.

## Safety invariants

- A worker with a positive semantic lease count is never an eviction candidate.
- A worker initializing or holding an outstanding JSON-RPC request is never an
  eviction candidate.
- At most one initialization handshake runs by default; subsequent cold roots
  wait without spawning children.
- Capacity and TTL select only from one deterministic `(last-used, CWD)` LRU
  ordering.
- Eviction removes routing authority before sending `SIGTERM`; no new request
  can attach to the retiring child.
- A dead child or removed configuration cannot retain a lease or resident slot.
- A broad semantic miss never starts all configured workspaces.
- Runtime evidence always includes canonical CWD because PID alone is not a
  useful recognition or ownership aid.

## Linked intents

The cclsp repository owns an append-only registry under
`docs/intent/registry.json`. Stable IDs cover active-worker retention, idle
expiry, serialized initialization, bounded cross-workspace resolution, and CWD
runtime evidence. `src/intent-contract.test.ts` enforces code and test witnesses
in both directions.

The policy has a small state space and receives exhaustive native table tests.
A Prolog shadow oracle may be used during design because eviction relates
workers, leases, initialization, requests, and capacity. Per linked-intent
policy, it remains in the repository only if it finds a counterexample the
native matrix missed.

## Implementation shape

### Pure core

`src/lsp/workspace-pool-policy.ts` accepts captured worker facts and returns a
closed decision: retain, expire, capacity-evict, wait, or typed refusal. It has
no clocks, processes, files, timers, or mutable maps.

### Imperative shell

`ServerManager` owns lease counts, the initialization FIFO, last-use clocks,
the periodic idle sweep, exact map removal, and `SIGTERM`. `LSPClient` acquires
and releases leases around semantic operations and projects the pool state into
`inspect_runtime`.

`resolve_var_surface` queries only source-root-derived candidates. Missing or
incomplete metadata produces a typed refusal containing the requested CWD,
candidate evidence, and retry/remedy information; it does not use
`Promise.all` over configured roots.

clj-surgeon onboarding computes the next config from currently existing
canonical roots. It retains unrelated non-managed server entries and prunes
only managed Clojure workspace entries whose CWD no longer exists.

## Contract-exhaustive behavior matrix

| Resident state | Lease | Init | Outstanding | Idle | Capacity action |
|---|---:|---:|---:|---:|---|
| ready | 0 | no | 0 | below TTL | retain unless idle LRU is needed |
| ready | 0 | no | 0 | at/over TTL | expire |
| ready | >0 | no | any | any | retain |
| warming | any | yes | any | any | retain |
| ready | 0 | no | >0 | any | retain |
| dead/removed | 0 | no | 0 | any | remove accounting; no signal if exited |
| full, eligible idle workers | 0 | no | 0 | any | evict exact LRU |
| full, no eligible workers | any | any | any | any | wait or typed refusal; never kill |

Additional intersections cover repeated same-root leases, a fifth active root,
an eighth and ninth resident, simultaneous TTL/capacity pressure, failed
initialization releasing the FIFO, config removal, deterministic CWD tie-break,
and incomplete namespace metadata with many configured roots.

## Test plan

1. Pure policy tests enumerate the matrix and all two-worker LRU ties.
2. Scheduler tests hold four distinct roots, prove the fifth does not start,
   release one, and prove FIFO admission.
3. Server-manager tests use inert child fixtures to prove serialized spawn,
   active/initializing protection, TTL expiry, count eviction, and map removal
   before termination.
4. The field regression models the twelve-worktree fan-out and asserts one
   incomplete-metadata request starts zero sibling workers.
5. Runtime-status tests require canonical CWD, limits, leases, idle age, and
   eligibility.
6. clj-surgeon pure onboarding tests retain live roots, prune missing managed
   roots, and preserve unrelated server entries. One filesystem boundary test
   uses real live/missing directories.

## Documentation and release checklist

- Update the 2026-08-23 review from the provisional two-worker recommendation
  to the selected four-active/eight-warm design.
- Document environment overrides and the exact runtime-status fields in cclsp.
- Keep every process inventory and reap receipt CWD-bearing.
- Record the post-reload PID/CWD/footprint inventory and any protected workers.
- Do not remove project `:cli-assist` registrations or live session-owned JVMs
  as part of this change.

## Verification gates

1. New tests fail against the unbounded implementation.
2. cclsp formatter, lint, typecheck, focused tests, and full suite pass.
3. Changed Clojure files are formatted before focused and full clj-surgeon
   suites; `make test` passes.
4. A live `inspect_runtime` snapshot proves at most eight resident children,
   at most one warming child, and CWD-bearing lifecycle evidence.
5. A controlled ninth-root request evicts only an unleased LRU worker or returns
   the typed capacity refusal.
6. After ten idle minutes—or a test-clock equivalent—eligible children are no
   longer resident, while a held lease survives the same sweep.
7. macOS `footprint` for the retained shared child family is at or below the
   2.5 GiB target after policy convergence.

## Definition of done

The shared cclsp server can serve concurrent repositories and branches while
its child population remains bounded, every resident is recognizable by CWD,
cold starts cannot stampede, incomplete cross-workspace discovery cannot fan
out, and a permanent linked test makes any future eviction of active work or
unbounded retention fail loudly.

## Implementation and live evidence

The policy is live in shared cclsp instance
`cclsp-3893-06b4b9ff-68a7-4539-a2cf-04a42aab9671` (broker PID 3893, CWD
`/Users/genekim/src.local/cclsp-structural-results`). A real
`get_diagnostics` request for
`/Users/genekim/src.local/clj-surgeon/src/clj_surgeon/workspace_onboarding.clj`
created one worker: PID 26377, CWD
`/Users/genekim/src.local/clj-surgeon`. The subsequent `inspect_runtime`
receipt reported four active slots, eight resident slots, one initialization
slot, a 600000 ms TTL, one ready resident, zero active leases, zero outstanding
requests, and zero queued work. macOS `footprint` measured about 430 MB for the
worker and 52 MB for the broker, below the 2.5 GiB converged-family target.

The onboarding boundary then reduced shared configuration from 39 to 23
existing CWDs. It removed only these missing legacy managed roots:

- `/private/var/folders/wc/ltz22xt962q_f899cgl31drm0000gn/T/clj-surgeon-cold-lease.XXXXXX.X9UxmW5Tl1`
- `/private/var/folders/wc/ltz22xt962q_f899cgl31drm0000gn/T/clj-surgeon-cold-lease2.XXXXXX.jnGBVauK4P`
- `/private/var/folders/wc/ltz22xt962q_f899cgl31drm0000gn/T/clj-surgeon-cold-final.sSN5u9`
- `/private/var/folders/wc/ltz22xt962q_f899cgl31drm0000gn/T/clj-surgeon-supervised-cold.HAsU54`
- `/Users/genekim/src.local/social-media-writer-file-docs`
- `/private/tmp/72cd-core-hold.SxZzOY/repo`
- `/private/tmp/abs-lanes.qBmFCb/repo`
- `/private/tmp/cfp-pure-control`
- `/Users/genekim/src.local/ssk-anvil-repl-integrated`
- `/private/tmp/cfp-anvil-eval-ops`
- `/private/tmp/cfp4-burn-staleness`
- `/private/tmp/cfp4-overnight-laneD-main`
- `/private/tmp/cfp4-review-reconciler`
- `/private/tmp/code-director-z5o3`
- `/private/tmp/cfp4-eval-inbox.RT5d2j`
- `/Users/genekim/src.local/sessionize-sched-killer-woodchipper`

Verification evidence:

- cclsp: Prolog oracle 5/5; Bun 326 passed, 5 skipped, 0 failed; TypeScript
  typecheck and Biome lint passed.
- clj-surgeon: 604 core tests with 5221 assertions passed; the two focused
  pruning boundary tests passed in the warm project nREPL at CWD
  `/Users/genekim/src.local/clj-surgeon`.
- The broader pre-existing dirty MCP suite has one unrelated schema expectation
  mismatch in `mcp_server_test.clj`: insert actions currently omit the `find`
  requirement expected by the test. This lifecycle change does not edit that
  schema or test and does not claim that independent gate as green.
- The dead-agent JVM reaper self-test passed. Its live audit found zero eligible
  `:cli-assist` JVMs and protected all six because each still had a live coding
  agent and socket owner; no session-owned JVM received a signal.
