# Uniform MCP Elapsed-Time Contract

## Observable promise

Every public clj-surgeon MCP operation reports its own server execution time.
The four current operations are `inspect_clojure`, `apply_clojure_changes`,
`edit_clojure`, and `transform_clojure`.

For every successful or refused call:

- structured content contains a finite, non-negative numeric `elapsed_ms`;
- the compact human summary renders that value with two decimal places and the
  `ms` unit;
- elapsed time measures handler-side request execution, not model deliberation,
  transport latency, benchmark setup, or later cold verification;
- existing mutation, rollback, read-back, and error contracts remain unchanged.

`inspect_clojure` already satisfies the ordinary-read structured and summary
contract. This change closes its alternate-summary and uninitialized-server
paths and gives the same contract to the three write operations.

## Behavior matrix

| Surface | Success | Refusal / unavailable | Human summary |
|---|---|---|---|
| ordinary inspect | retain existing `elapsed_ms` | always attach `elapsed_ms` | request duration |
| prepare-change / retained basis | retain existing `elapsed_ms` | always attach `elapsed_ms` | request duration |
| cold-verification view | retain request and job durations separately | always attach request `elapsed_ms` | label job versus request time |
| apply / compact edit | attach `elapsed_ms` | attach before callback | edit/file counts or refusal plus duration |
| transform preview / commit | attach `elapsed_ms` | attach before callback | guarded-edit count or refusal plus duration |

## Design

Measure once at each of the three public handler seams. Associate the duration
with the result before generating JSON, compact text, or invoking the callback.
Do not add timing at the transaction compiler, filesystem writer, or benchmark
harness: those locations would miss refusal paths or confuse partial phase time
with the public operation.

Declare `elapsed_ms` in all three output schemas. This is an additive response
contract and changes the live tool-contract hashes, so publication uses
`make mcp-reload`; it does not restart the MCP server.

## Tests and gates

1. Pure summary tests cover success and refusal rendering for all summary
   variants, including cold verification's distinct job/request labels.
2. Handler tests prove callbacks and JSON bodies receive numeric
   `elapsed_ms` on success and uninitialized refusal paths.
3. Server catalog tests prove every advertised output schema declares and
   requires `elapsed_ms`.
4. Run focused warm-nREPL tests, format changed Clojure files, then run
   `make mcp-test` and `make test` at the milestone.
5. Hot-reload the live registry and dogfood one real `inspect_clojure` and one
   real `edit_clojure` call. The displayed duration must agree with structured
   content to the rendered precision.

## Non-goals

- replacing benchmark wall-clock measurement;
- timing model planning or client/tool round trips;
- exposing internal phase spans in this change;
- changing any request schema or edit semantics.

## Phase 6 verification record

Phase 6 completed the production path on
`experiment/submission-row-counterfactual`:

- one shared finalizer now measures and publishes every public operation;
- the canonical registry enumerates every allowed success, pending, failure,
  preview, commit, and typed-refusal outcome;
- the Linked-Intent audit found 23 implementation witnesses and 23 test
  witnesses with no violations;
- the Prolog shadow model exposed two legacy gaps—pending verification could
  masquerade as completion, and failed verification lacked an independent
  clock law—which the native contract now closes;
- the cold `make mcp-test` gate passed 213 tests and 1,751 assertions with zero
  failures or errors under `-Xms64m -Xmx512m`;
- live `inspect_clojure` returned a bounded 450-character form and reported
  `71.290208` ms, rendered as `71.29 ms`;
- a malformed live `edit_clojure` request refused before writing and reported
  `71.336666` ms, rendered as `71.34 ms`;
- the corrected guarded request committed one edit in one file, read the bytes
  back, and reported `83.963625` ms, rendered as `83.96 ms`.

Dogfooding also exposed a restart race: launchd could drop ownership before the
old endpoint finished draining, allowing an immediate `mcp-start` to mistake
the orphaned health response for a durable service. Startup now requires
agreement among endpoint health, the readiness file, and launchd ownership,
then waits for both ownership and health to disappear before relaunching. The
remove-and-immediate-start reproduction now returns a healthy launchd-owned
server, and `make mcp-reload` synchronizes the four public contracts without a
process restart.
