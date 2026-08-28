# Selector continuation removed the reread but did not remove the recovery turn

## Decision

Reject commit `8125854c9c40c278898185fba6f685fb26131e29` as a proven
complete-wall improvement on the first clean-context counterfactual. Keep its
snapshot-bound continuation mechanism as a safety foundation.

The POST caller used the preserved sibling and did not reread it. However, it
omitted the still-required aggregate `expect` from its first guarded retry.
Surgeon refused safely in 17.02 ms. The model repaired the payload in a third
MCP call, so POST was slower than PRE.

The next bounded option is a mechanically complete guarded retry template. It
should retain the pending original requests, recompute the aggregate `expect`,
and include every snapshot guard. The model should change only the selector
that failed. This is not selector authority and does not require server-side
retained state.

## Frozen comparison

- PRE: `f5431352418caa5d75605644291db898753e311d`
- POST: `8125854c9c40c278898185fba6f685fb26131e29`
- caller: fresh ephemeral Codex, `gpt-5.6-sol`, reasoning `high`
- cohort: one PRE and one POST run, in PRE then POST order
- prompt, corpus, output schema, model, reasoning, and scorer: identical
- runtime: one isolated MCP JVM at a time, `-Xms64m -Xmx512m`, port 7895
- shared ports `:7888` and `:7890`: untouched
- cclsp and clj-kondo: not launched

The first call contained two ordered requests against two files. The first
request selected `resolve-source-path` exactly. The second intentionally
misspelled the semantic target. PRE could recover only by retrying the complete
batch. POST exposed the completed prefix, pending ID, and guards for both
original files.

## Results

| Arm | Final meaning | Route gate | Complete wall | MCP calls | Native fallback | Repeated request | Repeated source work |
|---|---:|---:|---:|---:|---:|---:|---:|
| PRE | correct | pass | 25.838 s | 2 | 0 | 1 | 1,610 bytes |
| POST | correct | fail | 41.456 s | 3 | 0 | 0 | 0 bytes |

`Repeated source work` is the UTF-8 size of a successfully selected request
that appeared in both the initial batch and the retry. It measures repeated
server selection/read work, not duplicate public response bytes. PRE discarded
the successful prefix from the refusal and selected it again. POST published it
once in `continuation.completed_results` and did not request it again.

POST returned 15,315 public result bytes versus PRE's 14,059 bytes. The
continuation included the completed 1,610-byte source, and the extra refusal
added envelope bytes. The current feature trades repeated server work for more
public evidence; on this small form, bytes are not the win.

## Event clock

```text
PRE
  inspect batch starts
    0.260 s later: selector refusal (server elapsed 202.02 ms)
    5.704 s later: complete corrected batch starts
    0.334 s later: success (server elapsed 258.91 ms)
    4.632 s later: final JSON

POST
  inspect batch starts
    0.328 s later: selector refusal + continuation
                     (server elapsed 282.52 ms)
   11.359 s later: pending-only guarded retry starts
    0.042 s later: missing-fields refusal (server elapsed 17.02 ms)
    8.595 s later: pending-only guarded retry with aggregate expect starts
    0.179 s later: success (server elapsed 137.11 ms)
    2.284 s later: final JSON
```

The decisive lost time was not inside Surgeon. It was the two model recovery
intervals. The visible continuation remedy said to retry the pending ID with
`continuation.snapshot_guards`, but it did not supply or remind the caller of
the required aggregate `expect`. The exact second call proves the model had
already made the correct semantic decision and copied the complete guard map.

## Safety falsifier

The focused permanent test on the exact POST commit passed:

```text
guarded-selector-retry-preserves-completed-siblings-across-retries
1 test, 12 assertions, 0 failures, 0 errors
```

The test carries a guard-only completed sibling across successive pending-only
retries, mutates that completed sibling, and requires a pre-evaluation
`snapshot-guard-mismatch`. Thus the proposed retry template can remain
stateless: the client supplies the complete snapshot guard set, and the server
recaptures and verifies the union before evaluating the pending suffix.

## Exact commands

```bash
bb bench/run_selector_continuation_benchmark.clj --self-test

BENCH_RESULT_DIR=/tmp/clj-surgeon-selector-continuation-counterfactual-20260827T2300Z \
  bb bench/run_selector_continuation_benchmark.clj

clojure -J-Xms64m -J-Xmx512m \
  -Sdeps '{:paths ["src" "test"] :deps {nrepl/nrepl {:mvn/version "1.3.1"} io.github.bhauman/clojure-mcp {:git/tag "v0.2.6" :git/sha "35a660b"} org.eclipse.jetty.ee10/jetty-ee10-servlet {:mvn/version "12.0.13"} org.slf4j/slf4j-nop {:mvn/version "2.0.17"}}}' \
  -M -e "(require '[clojure.test :as t] 'clj-surgeon.mcp-inspect-tool-test) (binding [t/*report-counters* (ref t/*initial-report-counters*)] (t/test-vars [#'clj-surgeon.mcp-inspect-tool-test/guarded-selector-retry-preserves-completed-siblings-across-retries]) (prn @t/*report-counters*))"
```

## Immutable evidence

- raw directory:
  `/tmp/clj-surgeon-selector-continuation-counterfactual-20260827T2300Z`
- raw archive:
  `/tmp/clj-surgeon-selector-continuation-counterfactual-20260827T2300Z.tar.gz`
- archive SHA-256:
  `fdea57ea52a1de78af8c77d9432226abef84f3563fcf1ebad05536a9a2bb775c`
- receipt SHA-256:
  `47526d5a1b98ea84c8b1cb069cb1906a70c0f6ad6a54c1038d879dd31fbeb403`
- PRE events SHA-256:
  `ef2783b3101270e7c7d5569c65bd870eb48a3476fd358e9c5658fd3b157ef186`
- POST events SHA-256:
  `e60c4db039bffb5cbb047a73c4e7c0652092f719892d49a3632c7dc030735c5f`
- PRE and POST prompt SHA-256:
  `657fdb109b75df7ead6fa5f29e318322b63b8aad4f50a944cd4b27c7a0f9822a`

## Limitations and next gate

This is an N=1 screen, not a release cohort. PRE ran before POST, so service and
cache order are confounds. The result is nevertheless decisive for the current
payload shape: POST made one extra call for an exact, observed missing field.

Do not spend an ABBA cohort on the unchanged payload. First add a pure compiler
for a complete, snapshot-bound retry template and test that the model can fill
only the failed selector. Then rerun N=1/arm with the frozen task. Promote to
ABBA only if POST uses two MCP calls, zero repeated requests, zero native
fallback, and remains semantically correct. Keep the stale completed-sibling
falsifier as a release gate.
