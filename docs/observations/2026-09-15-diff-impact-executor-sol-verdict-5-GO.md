GO

Reviewed sealed candidate `5290bae14383fe025375b80a77863ad63ad6b9aa`.

- `clojure -M:clj-surgeon/test-deps … diff-impact-test battery-parallel-test`:
  `78 tests / 957 assertions`, `0 failures, 0 errors`.
- Fresh other-lane-only `:fast` probe:
  `{:exit 1, :error "Diff-impact refused: fast-scope-empty", :data {:error-type :fast-scope-empty, :scope :fast, :classification [{:namespace other-test, :classification :other-lane-member, :lane :integration, :suite "mcp"}]}, :child-calls 0}`.
- Current manifest query found `fast ∩ lane-manifest/excluded = ()`; that exact contradictory tip state does not exist. The exhaustive fixture classified and refused both `load-excluded` and `bb-ineligible` under `:fast` and `:all` as `selected-namespace-unclassified`, while an admitted fast JVM member remains runnable.
- Classification probe:

| Selection | Classification | `:fast` | `:all` |
|---|---|---|---|
| renamed | `unregistered/renamed` | refused | refused |
| load-excluded | `load-excluded` | refused | refused |
| bb-ineligible | `bb-ineligible` | refused | refused |
| other lane | `integration/mcp` | `fast-scope-empty` at coordinator | routed to `mcp` |
| fast | `fast/fast` | routed to `fast` | routed to `fast` |

No nonempty case reached exit 0 with zero executions.

- `/var/tmp/forge/impact-fx/exec/round2/sol-probes.clj` reproduced:
  - battery freshness: `partial-receipt-not-a-gate-receipt`
  - census gate: `partial-receipt-not-a-gate-receipt`
  - prewarm and landing entrances: `partial-receipt-not-a-gate-receipt`
  - landing receipt-chain: `eligible false`
  - selected omission and unselected execution: both `:state :failed`, with missing/unexpected namespace census and `:partial-child-census-mismatch`.
- A real one-namespace partial receipt recorded `:discharged-by-this-receipt []`. `gate-envelope.clj` serialized it with zero executions, but `gate-consume.clj` refused it, exit `1`; it cannot become landing authority.
- `/home/forge/bin/suite-run make landing-gate-prewarm` at the sealed SHA exited `0`: all seven stages exited `0`, receipt `:state :passed`, `:problems []`.
- `bb /home/forge/bin/gate-envelope.clj …` on that fresh receipt:
  `58995 bytes state=:complete obligations=11 executions=7 pending=[]`, exit `0`.
- The product’s toolchain and obligation assertions alone are self-reported and do not bind execution. The independent binding is the installed consumer’s candidate-tree/policy derivation, execution matching, and observer custody. With no independent observer on this box, `gate-consume.clj` returned:
  `DECISION run-required reason=custody-unverified`, exit `1`. Thus forged producer evidence cannot be consumed instead of running the landing gate.
- Parity positive: `:parity :passed`, `:delta []`, exit `0`. The retained `-Duser.home` omission negative remains red on `fast-lane-isolation-test`; argv comparison is `{:argv-prefix-delta []}`.
- Fresh D2/D3 replay using byte-identical candidate overlay:

| Diff | Complete | Fast | Selection ms | Fast wall ms | Total ms | Exit |
|---|---:|---:|---:|---:|---:|---:|
| D2 | 9 | 7 | 4,270 | 8,188 | 12,458 | 0 |
| D3 | 101 | 51 | 4,189 | 30,893 | 35,082 | 1 |

D3’s only fast failure was the expected historical `clj-surgeon.lane-manifest-test`. The retained `:all` receipt reports all 101 namespaces separately: battery `160,219 ms`, dedicated `513,078 ms`, fast `29,559 ms`, MCP `57,471 ms`; these are not blended into the fast wall.

- SHA-256 comparison against `866df39b` proved the selector prefix, HOLD/nothing-selected block, and both intent-document prefixes byte-identical.
- `git diff --check` passed. `git diff HEAD --numstat` is empty; only the pre-existing untracked Codex log remains. No `make test` was run.

> END RECEIPT (fence-run): worktree HEAD at review exit = 5290bae14383fe025375b80a77863ad63ad6b9aa = fenced sha.
