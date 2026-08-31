# Prepared-confirmation affinity live-route protocol

Date: 2026-08-31

Status: frozen before measurement. This protocol authorizes isolated local
measurement only. It does not authorize installation, reload, registration,
publication to a shared runtime, or a performance claim.

## Exact subjects

- Control: `05f5a1962e5a0c5aa0365c673994eca9024c1a44`, tree
  `7cb0f58bdc4d8469d1f7757b0f0ee65e61f4fdc1`.
- Candidate: `7e0300fe0a75623fa6d7f275d2b99b57aa34f26d`, tree
  `1cfae15edb7ab167ce3c86f8157e8b0338c90790`.
- Both worktrees must be clean and exact before either server starts.
- Transport: two private 512 MiB MCP stdio processes, JSON-RPC 2.0 protocol
  `2024-11-05`, telemetry and nREPL disabled.
- Token proxy: `tiktoken==0.9.0`, `o200k_base`, over exact serialized UTF-8
  JSON bytes. These are proxy tokens, not provider billing authority.

## Frozen calls and gates

Run the same ordered calls in each arm: initialize, tools/list, an ineligible
forms read, an invalid confirmation containing one hostile unknown key, a
valid-shaped never-served confirmation, and one ordinary direct edit.

`environment_valid=true` requires both exact clean identities and isolated
processes. `semantic_correct=true` requires:

1. both refusals retain the same stable error type and structured evidence;
2. the candidate renders the hostile field once as one canonical escaped JSON
   array, with no interpolation into instructions;
3. the candidate unknown remedy names both safe routes: reuse the MCP session,
   or submit the served `prepared_request.arguments` explicitly;
4. all refusal calls leave fixture bytes unchanged;
5. the ordinary inspect and edit paths remain byte-identical after removing
   only elapsed-time fields, receipt/undo identifiers, and private arm roots.

`route_adherent=true` requires every payload to cross real stdio JSON framing.
Do not use direct Vars, Clojure maps, test fixtures, or the shared MCP runtime.

## Claims and stop law

Report exact full-catalog, inspect-tool, and edit-tool response bytes/tokens.
Report exact invalid-fields and unknown-refusal response bytes/tokens. These
deltas are caller input and are priced as cheap/cacheable input, not emitted
decode. Report ordinary-path normalized hashes and the named exclusions.

Stop and retain the run if any identity, route, refusal, source-integrity, or
ordinary-path equality gate fails. A failed run cannot be repaired in place;
freeze a new script commit and use fresh output roots. No wall-time, adoption,
session-retention, or recovery-turn claim can be promoted from this screen.
