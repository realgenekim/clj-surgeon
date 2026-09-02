# Prepared-request installed-route receipt — 2026-08-30

## Authority and subject

Gene authorized installation with the verbatim decision `and GO, yes! go !`.
This receipt concerns the installed prepared-request first slice at exact commit
`b445a8c3595d70f6f05b6edccb9b1a924539a195`, tree
`b1e21af8073e66283f82f4036583bfe2971c4b0a`.

The canonical remote branch `release/closed-relations-published` and annotated
tag `stable-prepared-request-20260830` both resolve to that exact commit. The
release train contains the prepared-request lineage only. The separate
cold-verifier clock fix and documentation branches did not ride this train.

## Installed artifacts

`make install` reported:

- stable CLI source commit: `b445a8c3595d70f6f05b6edccb9b1a924539a195`
- stable CLI source hash: `3f705b41eb9e779189f3fc12da36f225c7e739c8701215544d95968ed66cdd06`
- CLI receipt SHA-256: `2840dab683a230ea03b0b7f404f49e776393c789b50138742957e0caf1d89c6b`
- Codex skill receipt SHA-256: `e3913cdfb1f55c4b59ff38842defe071f12f8c5e068f1677728f4017c9f78bc0`
- Claude skill receipt SHA-256: `1b4dd3ac8f4d5c46404a38b187d02a446f9feb81e74414ace931a3ee704f79dd`
- control-plane receipt SHA-256: `0af5ff1d058fcc64997c7d9e24681a60617983527cd15f13be9ce411b001cb22`

`make mcp-reload` synchronized only `inspect_clojure`. The live contract hash
changed from `63069bc8` to `74450147`, with four tools still advertised. The
server explicitly reported `server-restart-required false`. Port 7888 remained
owned by PID `65458` before and after reload, and `/healthz` returned
`tool_runtime=ready` and `tool_registry=ready`.

## Installed-route proofs

### Tool catalog

A fresh HTTP MCP session initialized against `http://127.0.0.1:7888/mcp` and
called `tools/list`. The exact JSON result was 50,551 UTF-8 bytes with SHA-256
`14208f23bc3e42e0c353a565a8c7f03c50984941a831b6d30f2e18fb14357d40`.
The selected `inspect_clojure` tool projection had SHA-256
`d3d42d4ef5397665a4a5f847530d1ff59e68b8a6f75374c7bcaeece399e34428`.
Its optional `outputSchema.properties.prepared_request` object was 2,174 bytes,
SHA-256 `d4280b2b025aada4825739847254fcc6a050c816a839cb4810772c87cca6d620`,
closed with required fields `tool`, `executable`, `write_authority`, `arguments`,
and `caller_holes`.

### Eligible read

The installed `inspect_clojure` route read exactly
`src/clj_surgeon/mcp_intent_contract.clj/audit-contract`. It returned one
descriptor whose tool was `edit_clojure`, with `executable=false`,
`write_authority=false`, one ordered edit, a null `to`, and the single caller
hole `arguments.edits[0].to`. The source hash was
`7960b671e5aa1f938c5afeb20eeadb90a2180247f04dea9b39148c70fbb93dbe`.

### Ineligible read

The installed route then outlined the same file. The ordinary result contained
no `prepared_request` field. Its source hash remained
`7960b671e5aa1f938c5afeb20eeadb90a2180247f04dea9b39148c70fbb93dbe`.

### Filled descriptor

A fresh isolated workspace began with source SHA-256
`a892bdd0d21676a48e9e31a6fc72c5a2fe25b1c24d2f81d1aeec5581a14fea06`.
The installed read route returned one descriptor for `greeting`. Filling only
its null replacement and submitting its existing arguments to installed
`edit_clojure` committed exactly one edit in one file. The terminal receipt
reported `verification_complete=true`, `next_action=none`, read-back SHA-256
`3997cc8d2364f8f82db7685d8ce4a4480a1d28c93566d91bd574834c160df263`,
canonical-effect SHA-256
`75d2575ab0b56a78ec3e5305d9e107c96c1f720ab46b9a00a00db9505fd7c1ee`,
and receipt hash
`415a7b86271f3eccee8875e1d2ad32d9004dcc6836ea55fc2f51b4f050ef3933`.

## Claim boundary

This release ships the independently verified prepared-request mechanism and
its measured near-free input-side price. It does not claim routing lift,
request compression, or a product wall-time improvement. Recovery-output and
wall-time gains remain cohort evidence and projections until a product-shaped
installed-route cohort measures them.

## Replay outline

1. Run `make install` at exact `b445a8c3595d70f6f05b6edccb9b1a924539a195`.
2. Record the port-7888 PID, run `make mcp-reload`, and prove the PID is unchanged.
3. Initialize a fresh HTTP MCP session and call `tools/list`.
4. Call installed `inspect_clojure` once with a one-form `forms` request and
   once with an `outline` request.
5. In a fresh isolated workspace, fill only the descriptor's null `to` value
   and submit the unchanged arguments once to installed `edit_clojure`.

