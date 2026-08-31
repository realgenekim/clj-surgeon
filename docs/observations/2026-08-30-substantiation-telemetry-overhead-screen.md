# Substantiation telemetry overhead screen

Date: 2026-08-30  
Verdict: **GO for the install decision gate only**  
Product candidate: `de70e06fdc18f832b6774eabf81453ad4af9781f`  
Candidate tree: `9ca1e9a0c3c8e6a71e79d72f364210b3cddbb6d1`

This receipt does not authorize installation, reload, publication, or shared
runtime action. It measures MCP-OP-SUBST-018 after the independent feature
verification receipt at `9e7c51f4b4538ba5b7e48ead33ca3c38f6587ad9`.

## Result

The first replayable, semantically valid run passed every frozen threshold:

| Gate | Frozen limit | Measured | Result |
|---|---:|---:|---|
| Pure projection p95, 10,000 events | `< 0.5 ms/event` | `0.050750 ms` | pass |
| Append p50, 1,000 records | `< 1 ms/record` | `0.165166 ms` | pass |
| Append p95, 1,000 records | `< 5 ms/record` | `0.444542 ms` | pass |
| Append maximum | `< 25 ms/record` | `7.231083 ms` | pass |
| Live p50 delta, 100 calls/arm | `<= 2 ms` | `+1.124333 ms` | pass |
| Live p95 delta, 100 calls/arm | `<= 5 ms` | `+1.906709 ms` | pass |
| Event maximum | `<= 32,768 bytes` | `1,522 bytes` | pass |
| Public semantic parity | exact after named dynamic exclusions | `6/6 strata` | pass |

The live client-observed medians were `2.529500 ms` with substantiation off
and `3.653833 ms` with substantiation on. The p95 values were `5.328375 ms`
and `7.235084 ms`. The clock covers the complete loopback HTTP request, so
the before/after observer hooks cannot sit outside their own measurement.

The append ledger used `1,451,831` bytes for 500 completed calls, or
`2,903.662` bytes per completed call. Each call emits a start and a finish
record. No model calls or external network calls occurred. The screen made
260 loopback HTTP requests, of which 252 were tool calls.

Exact normalized public results matched for:

- an eligible prepared read;
- an operation-less read;
- a mixed-ID refusal;
- a complete transform count-mismatch refusal;
- an ordinary committed transform;
- a transform preview.

Normalization removes only the workspace spelling, formatted elapsed text,
the numeric `elapsed_ms` and `inspection_elapsed_ms` fields, the derived
`result_character_count`, and transaction receipt hashes. Source and result
hashes remain exact comparison evidence. Map keys are rendered as strings so
absolute-path keys can receive the same workspace normalization.

## Retained invalid attempts

No observed attempt was deleted or promoted after repair:

1. `2026-08-30-substantiation-overhead/` failed semantic parity. The first
   harness compared internal timing fields and used a hand-built compact-edit
   write stratum that did not cross the Java JSON-container boundary as a
   successful write.
2. `2026-08-30-substantiation-overhead-attempt-2/` remained non-authoritative
   because its EDN writer used namespaced-map shorthand for absolute-path
   keyword keys; the receipt could not be replayed by `clojure.edn/read-string`.
3. `2026-08-30-substantiation-overhead-attempt-3/` was replayable but failed
   ordinary-write parity because absolute-path map keys and the nested
   transaction receipt hash were not yet named as dynamic evidence.
4. `2026-08-30-substantiation-overhead-attempt-4/` is the accepted run. It
   changed no frozen threshold and retains all 100 samples per arm.

An earlier loader command also inherited the repository MCP test alias and
started the cold suite instead of this screen. It reproduced the two already
characterized cold-admission timing failures, was stopped at the natural
process boundary, and produced no measurement artifact.

## Replay

From the exact candidate checkout, with a new absent or empty result path:

```sh
clojure -J-Xms64m -J-Xmx512m \
  -Sdeps '{:aliases {:screen {:extra-paths ["dev/experiments"] :extra-deps {io.github.bhauman/clojure-mcp {:git/tag "v0.2.6" :git/sha "35a660b"} nrepl/nrepl {:mvn/version "1.3.1"} org.eclipse.jetty.ee10/jetty-ee10-servlet {:mvn/version "12.0.13"} org.slf4j/slf4j-nop {:mvn/version "2.0.17"}}}}}' \
  -M:screen -m substantiation-overhead-screen RESULT_DIR
```

Accepted artifact SHA-256 values:

| Artifact | SHA-256 |
|---|---|
| `dev/experiments/substantiation_overhead_screen.clj` | `493a053db374974a7d1200c5d8957c79d63a636dcd9c48a3527b43f97858c34c` |
| `receipt.edn` | `6dedcd155da9ab2ed33924b55b67fb620694944ca3510a8472cf3c4b09a69e11` |
| `parity.edn` | `423bc9cdd094921ad7097ed23f5661761cb62a82d3803400fe2825d4eb5929d8` |
| `pure-samples.tsv` | `07e6ee971d9c7193217dc49ff7f14b58a0492040ab0aa0a58296090943ed54f9` |
| `append-samples.tsv` | `d0c9da81070e430dd60b2ed12cd85a8ef51c17180ca9cda438b6568f8a8c6e26` |
| `live-samples.tsv` | `98af71681ff993828f86fed2409b9b11ecc84c133ec0b9650b1d04169d0ec643` |

## Claim boundary and next gate

This earns an install card for the telemetry candidate. It does not establish
feature adoption, recovery reduction, or saved model wall time. Those remain
unmeasured until an installed ledger captures real production calls.

The later W1 integration ratchet also remains binding: after W1 publishes and
telemetry rebases, a new independent witness must prove `serve descriptor ->
official confirm/fill consumption -> ordinary commit -> exactly one prepared
consumption count`. A mechanical rebase or raw-parameter-only witness is not
publication evidence.
