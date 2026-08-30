# Codex catalog floor sweep — frozen protocol

**Date:** 2026-08-29  
**Seat:** `surgeon1 <surgeon1@skiff>`  
**Host for calls:** Anvil `dev-a`  
**Question:** Does Codex turn latency scale with the client-visible MCP catalog,
or does enabling MCP add a fixed handshake?

## Why the old 490 ms contrast is not the answer

The retained clean condition has a 3,927 ms median, 280 ms MAD, and nine
samples. The retained fleet condition has a 4,417 ms median, 463 ms MAD, and
five samples. Their 490 ms difference is smaller than the combined 743 ms MAD.
The arms were not counterbalanced and the fleet catalog identity was not
frozen. The contrast motivated this experiment; it does not prove a recurring
490 ms catalog tax.

## Frozen arms

All MCP arms use the same production HTTP transport, fixed server instructions,
prestarted bounded JVM, and no-effect handler. Only the static catalog differs.

| Arm | Catalog | Purpose |
|---|---|---|
| C | no MCP server | wrapper/model floor |
| T | one minimal tool | fixed MCP handshake plus the smallest catalog |
| D | one tool, about 64 KiB of description | byte scaling with tool count fixed |
| P | one tool, about 64 KiB carried by parameters | parameter-shape check |
| M | sixteen tools, about 64 KiB total | tool-count check |
| I | real server list, only `inspect_clojure` enabled | projection-size check with transport fixed |
| R | frozen real four-tool clj-surgeon catalog | external-validity check |

Before calls, a token-free Codex app-server projection must record each arm's
exact client-visible bytes, tool count, parameter count, and SHA-256. Server
processes start before timing. Every model call receives a fresh `CODEX_HOME`
and the same prompt, model, reasoning effort, binary, workspace, and auth
identity.

## Schedule and validity

Fourteen blocks use seven rotations and their reverses. Every arm therefore occurs
twice in every position. No arm may stop early because its sign is favorable.

A row is admitted only when these independent fields are true:

- `environment_valid`: Anvil `dev-a`, load at or below 4.0, exact Codex binary
  and version, exact catalog identity, and unchanged experiment commit/tree;
- `semantic_correct`: exit zero and final assistant text exactly `ok`;
- `route_adherent`: zero MCP tool calls, zero shell calls, and zero file changes.

Invalid rows remain in the ledger. Complete-block estimates use only blocks
with all six admitted arms.

## Clocks and verdict

Every JSON event is arrival-timestamped. The retained facts include complete
process wall, process-to-turn start, turn-to-answer, turn-to-completion, and
completion-to-process-exit.

- `T - C` estimates the fixed MCP handshake plus the tiny catalog.
- `D - T` estimates about 64 KiB of added declaration bytes while tool count is
  unchanged.
- `P - D` and `M - D` test whether equal-sized catalogs differ because bytes
  are arranged as parameters or tools.
- `R - I` tests full versus one-tool client projection while the server returns
  the same full catalog in both arms.
- A block-stratified Theil-Sen slope estimates milliseconds per exact
  client-visible byte.

The practical-equivalence margin is 125 ms, one quarter of the motivating 490
ms contrast. Paired block medians use a deterministic whole-block bootstrap.

- **proportional:** the 95% interval for `D - T` is above 125 ms;
- **fixed:** the 95% interval for `T - C` is above 125 ms and `D - T` is
  contained within ±125 ms;
- **mixed:** both effects clear 125 ms;
- **noise or unresolved:** the intervals still admit competing explanations.

This experiment can close or redirect a latency hill. It cannot promote a
product speed claim.

## Result — the 490 ms claim did not reproduce

The complete Anvil cohort ran fourteen counterbalanced blocks across all seven
arms: 98 calls total. All 98 calls were environment-valid, semantically exact,
and route-adherent. The scorer's complete-wall verdict was
`noise-or-unresolved`.

| Arm | Client-visible shape | Median complete process wall |
|---|---:|---:|
| C | no MCP | 4,402.5 ms |
| T | one 400-byte tool | 4,494.0 ms |
| D | one 64,384-byte description-heavy tool | 4,381.0 ms |
| P | one 65,551-byte parameter-heavy tool | 4,851.0 ms |
| M | sixteen tools, 68,529 bytes | 4,677.5 ms |
| I | real server, one projected tool, 13,154 bytes | 4,377.5 ms |
| R | real four-tool catalog, 48,045 bytes | 4,122.5 ms |

Complete wall is dominated by provider, network, and answer variance. Its
paired intervals cross zero:

- `T - C`: -760.5 ms, 95% whole-block bootstrap interval -1,211 to +598.5 ms;
- `D - T`: -114 ms, interval -1,435 to +760.5 ms;
- `R - I`: -143.5 ms, interval -847 to +276 ms.

The raw event clocks do resolve the local phase. The actual process-start to
`turn.started` contrast for `T - C` is **+40.75 ms**, interval **+22.83 to
+68.28 ms**, positive in 12 of 14 blocks. This is a small fixed MCP startup and
discovery cost. Adding about 64 KiB while holding the one-tool shape fixed
produces **-0.50 ms**, interval **-55.46 to +55.35 ms**. There is no detected
catalog-byte slope. A whole-block slope bootstrap also crosses zero.

The old 3,927 versus 4,417 ms contrast therefore cannot be described as a 490
ms recurring catalog tax. The corrected conclusion is:

> Codex pays about 41 ms of fixed local MCP startup in this route. Catalog size
> did not measurably change startup or complete turn time.

## Operational consequence

Do not shrink or hide the clj-surgeon catalog to reduce this floor. Projecting
only `inspect_clojure` reduced the real client-visible surface from 48,045 to
13,154 bytes without producing a latency win. A missing tool is a capability
loss: local Codex accepts an allowlist containing a nonexistent tool and
silently projects zero matching tools, so the model cannot call it and receives
no typed configuration refusal.

Codex already defers MCP tools from the direct function list on this model, but
it still initializes the MCP server, lists its tools up front, and exposes the
nested specifications through Code Mode. Local Codex 0.147.0 has static
`enabled_tools`, `disabled_tools`, and `omit_tools_from` controls; this audit
found no configuration that lazily initializes an MCP catalog on first use.

## Evidence and losses retained

- compact cohort archive:
  `bench/results/2026-08-29-codex-catalog-floor/catalog-floor-full-68aab21-evidence.tar.gz`
- archive SHA-256:
  `0a86cff91b7673ae9549c05a9f9748417cea0e9fdc6f760515c1a3c8ad0d547a`
- corrected phase audit:
  `bench/results/2026-08-29-codex-catalog-floor/catalog-floor-phase-audit.json`
- phase-audit SHA-256:
  `3d1c67b3cbfffc1c56b8866d9a467850ba9b6b3bd2d248fc78fb438c538f306a`
- phase-audit fold: `dev/experiments/catalog_floor_phase_audit.py`
- `score.json`: `0ff26aea345ab3342dcc41eb35690336b9225c1f54f299237ae4c46514f2aa15`
- `runs.tsv`: `2e22f6fa99cdf22130e217141c41422f7b95447b486d04bc32e96216d045184b`
- `catalogs.json`: `d094623e8092f51aeb23d468a340393a0f1fc7cc3e091ee7cce11a08160a93cb`
- `meta.json`: `c85238b0ff98d8dc27428aeff734c704c4caa501b8664a25a1bf38d2478f3a17`

Two later token-free registry timing attempts are not evidence. The first had
an invalid clean-tree predicate and mismatched hash framing. After those were
repaired, the tiny-arm JVM failed before measurement because it could not load
`clojure.main`. No registry timing row from either attempt enters this verdict.
