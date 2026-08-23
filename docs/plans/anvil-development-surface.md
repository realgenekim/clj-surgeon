# Anvil as a durable development surface

**Status:** Directional architecture options; default recommendation recorded

## Ground truth

The 2026-08-23 Sol/high study proved that Anvil can accept a working-tree
snapshot, run repository-owned harness tests, start seat-local authenticated
Codex jobs, and retain results after Gene closes his laptop. Three detached
supervisors continued under PID 1 in these exact CWDs:

- `dev-a`: `/srv/fleet/dev-a/clj-surgeon-study-20260823-sol`
- `dev-b`: `/srv/fleet/dev-b/clj-surgeon-study-20260823-sol`
- `dev-c`: `/srv/fleet/dev-c/clj-surgeon-study-20260823-sol`

The same run also exposed the real cost: the Mac-tested harness assumed `zsh`,
and Codex's nested Bubblewrap sandbox could not create loopback on Anvil. The
first defect was repaired by installing the declared dependency. The second
required an explicit isolated-fixture sandbox mode and invalidation of the
confounded results. Remote capacity is already useful, but it is not yet a
transparent substitute for the laptop.

## Decision: where should development live?

### A. Local-first, Anvil only for batches

**Why it might be right:** Preserve today's lowest-latency editor, browser,
filesystem, credentials, and nREPL experience. Use Anvil only for controlled
experiments, long tests, and parallel agents that clearly benefit from remote
capacity.

**Cost:** The laptop remains the bottleneck and single point of interruption.
Remote execution stays exceptional, so environment drift is discovered during
urgent handoffs instead of continuously.

**Assumption underneath:** Most valuable work is latency-sensitive tweezer work
and the laptop's resource failures can be solved locally.

### B. Hybrid router: Anvil-default for bulk work, local for true tweezers

**Why it might be right:** Put branch development, research, builds, long tests,
benchmarks, daemon-heavy Clojure analysis, and multi-agent work near durable
compute. Keep Mac-only UI work, direct manipulation, local-browser diagnosis,
and very tight edit/evaluate loops local until remote latency is measured good
enough. Tailscale connects the two planes without pretending they are one
filesystem.

**Cost:** Two environments require a precise task router, reproducible setup,
branch/worktree ownership, artifact transfer, and a visible inventory of live
jobs and ports. A task routed incorrectly pays setup latency before useful
work begins.

**Assumption underneath:** Bulk versus tweezer work is recognizable early, and
Tailscale is reliable enough to expose narrow services while source and agents
remain co-located on Anvil.

### C. Thin laptop: nearly all development on Anvil, including tweezers

**Why it might be right:** The laptop becomes a display and input device. Agents,
source, JVMs, tests, and indexes stay on durable compute; closing the lid no
longer interrupts work. Tailscale plus a remote editor, browser endpoints, and
SSH-tunneled nREPL could make even fine-grained work portable.

**Cost:** Latency and network loss enter every edit. Remote browser state,
clipboard behavior, file watching, hot reload, port routing, and Mac-only apps
become product problems. A remote filesystem mount risks stale reads and poor
watcher semantics; direct tailnet exposure of an unauthenticated nREPL is a
security defect.

**Assumption underneath:** End-to-end interaction latency is consistently low,
the remote UI is pleasant, and the control plane can fail closed without
stranding or exposing live runtimes.

## Recommendation

Choose **B, the hybrid router**, and make it the silent default. The deciding
argument is that Anvil already proved the highest-value property—durable work
continues after the laptop closes—while the same experiment proved that host
differences still invalidate supposedly equivalent runs. Route bulk work to
Anvil now; earn tweezer work one measured loop at a time.

Fallback: if task classification creates more ceremony than it saves, use A
while retaining the same paved batch launcher. Promote to C only after a
Tailscale tweezer pilot matches local correctness and p90 interaction latency.

Decision handle: `anvil-hybrid`. If Gene says nothing, proceed with B.

## Target layout

```text
Mac control surface
  editor / browser / Director / dispatch
                 |
          Tailscale + SSH
                 |
Anvil execution plane
  seat -> repo -> branch/worktree -> agent/JVM/index -> durable receipt
```

The source tree, agent, formatter, tests, nREPL, and semantic services should
live on the same side of the network boundary. Do not edit through an SSHFS or
other remote mount by default. Move intent and receipts across the boundary,
not chatty filesystem operations.

## Tailscale tweezer pilot

Test one real hot-loop task with identical local and Anvil variants:

1. Create one named remote worktree with a durable owner, commit, and CWD
   receipt.
2. Start the application nREPL on loopback only. Reach it through Tailscale SSH
   port forwarding or an ACL-restricted authenticated broker; never expose raw
   nREPL broadly on the tailnet.
3. Run the editor or coding agent beside the remote source. Expose only the
   application/browser port needed for visual proof.
4. Measure keystroke-to-diagnostic, save-to-reload, focused-test, browser-refresh,
   reconnect, and failure-recovery p50/p90 against the local loop.
5. Kill the tunnel and laptop connection deliberately. The remote job and
   source must survive, while access endpoints fail closed and can be
   rediscovered from a receipt.

Promote a task class to remote tweezer work only when correctness is
non-inferior, p90 loop latency is acceptable to Gene, and reconnect requires
one paved command rather than filesystem or tmux archaeology.

## Paved-road requirements

- One command creates or resumes a seat-owned repo/worktree and prints its CWD,
  commit, branch, owner, model, quota, ports, and supervisor identity.
- One command dispatches a durable assignment and returns an ID plus result
  path; laptop disconnect is an ordinary tested event.
- One status command reports running, completed, failed, and stale jobs across
  all seats without inspecting tmux manually.
- Every long job writes terminal receipts incrementally, so a minute-five
  failure is still evidence.
- Host prerequisites and sandbox capability are preflighted before any paid
  model call.
- Per-worktree nREPL, MCP, cclsp, and browser endpoints have leases and CWDs;
  reaping is based on owner liveness, not process names alone.
- Results flow back as structured summaries and hashes. Raw transcripts remain
  local to the execution plane unless a bounded forensic read is required.

## First implementation slices

1. Extract the current Anvil experiment launcher into a tested admission command
   with task-set, model, reasoning, arm, and sandbox fields in its receipt.
2. Add the host-capability preflight that would have caught missing `zsh` and
   failed Bubblewrap before the first model call.
3. Add one cross-seat status command and one laptop-disconnect survival test.
4. Run the Tailscale tweezer pilot on a small nREPL-backed Clojure change.
5. Use those measurements—not taste—to decide which additional task classes
   move from local to Anvil by default.
