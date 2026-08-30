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
| R | frozen real four-tool clj-surgeon catalog | external-validity check |

Before calls, a token-free Codex app-server projection must record each arm's
exact client-visible bytes, tool count, parameter count, and SHA-256. Server
processes start before timing. Every model call receives a fresh `CODEX_HOME`
and the same prompt, model, reasoning effort, binary, workspace, and auth
identity.

## Schedule and validity

Twelve blocks use six rotations and their reverses. Every arm therefore occurs
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
