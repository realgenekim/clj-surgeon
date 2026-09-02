# Substantiation telemetry/W1 rebase overhead screen

Date: 2026-08-31

Verdict: **GO to the telemetry breakfast/install card only.** Exact candidate
`4e2cf27b2226997508356ac5ecbdeaed18d8132c`, tree
`4c7265c660ce6e698e0dbff45d4e697fe05994e2`, remains inside every frozen
MCP-OP-SUBST-018 overhead bound after the W1 rebase. This receipt does not
authorize installation, reload, publication, or shared-runtime action.

## Result and prior-receipt delta

| Gate | Frozen limit | Prior `de70e06f` | Exact `4e2cf27b` | New minus prior | Result |
|---|---:|---:|---:|---:|---|
| Pure projection p95, 10,000 events | `< 0.5 ms` | 0.050750 ms | 0.068083 ms | +0.017333 ms | pass |
| Append p50, 1,000 records | `< 1 ms` | 0.165166 ms | 0.145542 ms | -0.019624 ms | pass |
| Append p95, 1,000 records | `< 5 ms` | 0.444542 ms | 0.419958 ms | -0.024584 ms | pass |
| Append maximum | `< 25 ms` | 7.231083 ms | 6.654125 ms | -0.576958 ms | pass |
| Live p50 delta, 100 calls/arm | `<= 2 ms` | +1.124333 ms | **+1.288084 ms** | +0.163751 ms | pass |
| Live p95 delta, 100 calls/arm | `<= 5 ms` | +1.906709 ms | **+1.568334 ms** | -0.338375 ms | pass |
| Event maximum | `<= 32,768 B` | 1,522 B | 1,522 B | 0 B | pass |
| Public semantics | exact 6/6 | 6/6 | 6/6 | unchanged | pass |

The exact candidate's paired live medians were 2.656666 ms with telemetry off
and 3.944750 ms with telemetry on. Its p95 values were 5.053500 ms and
6.621834 ms. The paired delta is the overhead authority; absolute values are
not evidence that the rebase made the product faster or slower than the prior
day's host state.

The ledger used 1,451,828 bytes for 500 completed calls, or 2,903.656 bytes
per completed call. Each call emits a start and finish record. The complete
screen made 260 loopback HTTP requests, including 252 tool calls, with zero
model calls and zero external network calls.

Exact normalized public results matched with telemetry off/on for all six
strata: eligible prepared read, operation-less read, mixed-ID refusal,
complete transform count-mismatch refusal, ordinary committed transform, and
transform preview.

## Earned instrument repair and retained loss

The first exact-hash run passed every timing threshold but correctly failed
semantic parity on the two read strata. W1 now returns a session-bound
`descriptor_sha256`; the private off/on MCP sessions minted different valid
digests. The accepted pre-W1 `de70e06f` screen had no such field, so its frozen
normalizer could not name it.

Independent EDN diff showed the two digest values were the only differences.
The repair normalizes only a present, valid 64-lower-hex digest value to
`<SESSION-BOUND-DIGEST>`. It refuses malformed values and preserves field
presence, so a missing confirmation remains a parity failure. No threshold,
sample count, schedule, stratum, or other exclusion changed. The failed run is
retained under `invalid-run-1`; receipt SHA-256
`d1ace26098453e5974e4013884d989619a6484a7ff85663a5bd8fb1e9990733d`.

A later run passed after that repair, but the repository formatter then
changed the instrument bytes. It is retained under `unscored-preformat-run`
and contributes no scored metric. The table above comes only from the final
post-format run.

## Claim boundary

This screen establishes only that the exact rebased candidate stays within the
frozen telemetry-overhead envelope. It does not establish feature adoption,
recovery reduction, saved model wall time, or provider billing cost. The live
delta is one zero-model paired measurement, not a repeated performance claim.
The prior-to-current differences are descriptive because the runs occurred at
different times and host states.

## Evidence

- Exact candidate receipt SHA-256:
  `d082bf9e0b8dd0ee14de4c856581aeb6cb91d4c3ea570992ad2857b603188668`
- Exact candidate manifest SHA-256:
  `125b9d788a2e53a0b5fc543c86ff13836df88fc9870cb3cccf3c225d77b7156e`
- Screen source SHA-256:
  `fe5977266214be8e7e49d73ca94e523d86d50b73a35d61189a1efc41a850139b`
- Protocol SHA-256 before this receipt:
  `0097b02df3937a0779a06478a28d7466355415e2ec26914d6f977c7a126e80d9`
- Accepted prior `de70e06f` receipt SHA-256:
  `6dedcd155da9ab2ed33924b55b67fb620694944ca3510a8472cf3c4b09a69e11`
- New parity SHA-256:
  `1f4b7973545d5b11902bb517e4d4f6d5465d709d8cdaf0e953773dff598d6ef3`
- New live samples SHA-256:
  `c280c1ef5182409d77f62b1c998ca8128b641a7dabdca72f6a470c8664e7d2d8`

## Exact replay

Run product code from a clean detached worktree at exact `4e2cf27b`, loading
only this branch's immutable screen namespace through the absolute extra path:

```sh
clojure -J-Xms64m -J-Xmx512m \
  -Sdeps '{:aliases {:screen {:extra-paths ["/private/tmp/clj-surgeon-telemetry-overhead-receipt.mdHG3v/worktree/dev/experiments"] :extra-deps {io.github.bhauman/clojure-mcp {:git/tag "v0.2.6" :git/sha "35a660b"} nrepl/nrepl {:mvn/version "1.3.1"} org.eclipse.jetty.ee10/jetty-ee10-servlet {:mvn/version "12.0.13"} org.slf4j/slf4j-nop {:mvn/version "2.0.17"}}}}}' \
  -M:screen -m substantiation-overhead-screen RESULT_DIR
```

The result directory must be absent or empty. The screen exits nonzero on any
timing, event-size, no-model/network, or six-stratum parity failure.

## Dogfood ledger

Two exact edits to an established Clojure experiment file were eligible for
the Surgeon route: rebinding the two identity literals, and adding the
session-bound digest normalizer. Both used native `apply_patch`. This was an
operator routing loss, not a tool refusal: the persistent editor could have
targeted the isolated worktree. No fallback was technically required. The
measurement remains valid because product code executed from a separate clean
exact candidate worktree, but the builders-as-users ledger for this task is
0/2 eligible edits through Surgeon.
