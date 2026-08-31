# Substantiation telemetry/W1 rebase overhead protocol

Date: 2026-08-31

Status: frozen before rerun. This protocol authorizes the exact MCP-OP-SUBST-018
overhead screen only. It does not authorize installation, reload, publication,
or shared-runtime action.

## Exact subject

- Candidate: `4e2cf27b2226997508356ac5ecbdeaed18d8132c`, tree
  `4c7265c660ce6e698e0dbff45d4e697fe05994e2`.
- Execute product namespaces from a clean detached worktree at that exact
  commit. Load only the immutable screen namespace from the measurement branch
  through an absolute extra path.
- Screen semantics differ from the accepted `de70e06f` instrument only in the
  two candidate identity literals. The repository commit hook reformatted
  indentation; `git diff --ignore-all-space 7dcf1fab --
  dev/experiments/substantiation_overhead_screen.clj` shows only those two
  literals. Every sample count, schedule, semantic stratum, normalization rule,
  and threshold remains unchanged.

## Unchanged frozen gates

| Gate | Limit |
|---|---:|
| Pure projection p95, 10,000 events | `< 0.5 ms/event` |
| Append p50, 1,000 records | `< 1 ms/record` |
| Append p95, 1,000 records | `< 5 ms/record` |
| Append maximum | `< 25 ms/record` |
| Live p50 delta, 100 calls/arm | `<= 2 ms` |
| Live p95 delta, 100 calls/arm | `<= 5 ms` |
| Event maximum | `<= 32,768 bytes` |
| Public semantics | exact in all 6 strata after the same named exclusions |

Run one counterbalanced `off/on/on/off` live schedule with 50 measured calls
per block, plus the unchanged warmups, pure projection, append, and parity
screens. No model or external network call is allowed.

Report the complete new receipt and its delta from the accepted `de70e06f`
receipt. A pass establishes only that the exact rebased candidate remains
within the frozen telemetry-overhead envelope. It cannot establish adoption,
saved recovery turns, model-wall improvement, or provider billing cost.
