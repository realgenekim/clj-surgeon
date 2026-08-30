# Prepared-request live-route token measurement — 2026-08-30

Verdict: the first slice is inexpensive enough to present for a separate
install decision. It does not compress the final mutation call. Its justified
value claim remains fewer assembly errors and recovery turns after an eligible
read. No installation, reload, registration, or shared-runtime action occurred.

## Exact subjects and route

- control commit `c55de2279826af5ed21c90981591479dd2e802b2`, tree
  `565f009f0ff25fdedbc2fba5ad9ba5f55783e023`;
- candidate commit `b445a8c3595d70f6f05b6edccb9b1a924539a195`, tree
  `b1e21af8073e66283f82f4036583bfe2971c4b0a`;
- measurement protocol commit `919ae8a`;
- forward apparatus repair commit `d426126`; and
- tokenizer `tiktoken 0.9.0`, `o200k_base`.

Both arms were clean and ran as separate local 512 MiB MCP stdio JSON-RPC
processes with telemetry off and no nREPL. The harness did not touch the shared
server or registered MCP configuration. Controlled process termination after
the retained responses accounts for exit code 143 in each arm.

Validity was green: `environment_valid=true`, `route_adherent=true`, all read
semantics correct, read fixture byte-identical, filled and independently
constructed edit arguments equal as decoded JSON, and the paid edit returned
`committed=true` and `verification_complete=true` with the exact expected
future source hash.

## Wire measurements

All quantities are exact serialized JSON bytes and `o200k_base` proxy tokens.

| Surface | Control | Candidate | Delta |
|---|---:|---:|---:|
| full `tools/list` response | 49,490 B / 11,134 T | 50,550 B / 11,402 T | **+1,060 B / +268 T** |
| `inspect_clojure` input schema | 7,869 B / 1,958 T | 7,869 B / 1,958 T | **0 B / 0 T** |
| `inspect_clojure` output schema | 3,956 B / 912 T | 5,016 B / 1,180 T | **+1,060 B / +268 T** |
| eligible one-form response | 1,608 B / 526 T | 2,180 B / 673 T | **+572 B / +147 T** |
| eligible six-form response | 4,563 B / 1,637 T | 5,739 B / 1,981 T | **+1,176 B / +344 T** |
| ineligible seven-form response | 5,140 B / 1,862 T | 5,142 B / 1,862 T | **+2 B / 0 T** |
| final edit arguments, from scratch vs filled | 217 B / 68 T | 217 B / 69 T | **0 B / +1 T** |

The one-form descriptor itself is 320 B / 96 T. The six-form descriptor is
925 B / 293 T. The submitted paid edit request is 311 B / 98 T, including
217 B / 69 T of arguments. The independently constructed and descriptor-filled
argument objects have the same decoded JSON value. Their one-token difference
comes only from JSON member order, not from a public semantic difference.

## The retained loss

The ineligible response preserved the no-cue law but did not meet literal
wire-byte identity. It had no `prepared_request`, its ordinary structured
result matched the control after removing dynamic timing, and it added zero
tokens. The raw response was two bytes larger because the measured elapsed
time changed from 7.726750 ms (`7.73 ms` in rendered text) to 9.065459 ms
(`9.07 ms`). This is a measured loss against strict byte identity, not a
semantic or model-visible token cue. The result must not be reported as raw
byte-identical.

## Price model and install recommendation

The catalog and eligible-read deltas are caller input. They are cheap prefill,
and the catalog is cacheable. The retained route measured input at 1,284 times
the throughput of output. On that point estimate, the +268-token catalog costs
about 0.21 output-token equivalents once; the +147-token one-form result costs
about 0.11; and the +344-token six-form result costs about 0.27.

The final mutation call is not smaller: it is the same 217 bytes and differs by
one tokenizer token due to member order. Therefore this measurement supplies no
request-compression claim. The install case rests on reducing request assembly
and recovery. The closest product-shaped null-hole proxy measured median
completed wall from 69.291 seconds to 51.729 seconds, 912 fewer output tokens,
recovery actions from 20 to eight, and construction refusals from seven to four
across four attempts per arm. That is separate proxy evidence, not a guaranteed
product transfer. A sibling complete-request screen independently found the
same direction with 47.4% lower median output and six versus zero construction
refusals.

Recommendation: GO to the separate install decision. The input price is tiny,
the successful paid call remains an ordinary schema-valid `edit_clojure`
request, and the candidate has independent implementation verification. Do not
claim routing lift, final-call compression, or a product wall-time improvement
until a fresh product-shaped cohort measures it.

## Durable evidence and replay

- valid report SHA-256:
  `d9eb11033d9d7cb1f0bc04c807c0e9afb512938c56a0687ed4ab9431443b6249`;
- valid manifest SHA-256:
  `b930057cfe652302b1f8131405037e15a290eba7abd88e55e645fe9b738fd35f`;
- valid artifacts:
  `bench/results/2026-08-30-prepared-request-live-route/`;
- retained invalid-attempt manifest SHA-256:
  `3f46ff076f8969152a9f860c4a3711754edb7ce99032fc564b1913de1d43c65a`;
- retained invalid artifacts:
  `bench/results/2026-08-30-prepared-request-live-route-attempt1-invalid/`.

Exact valid-run command:

```sh
uv run --with tiktoken==0.9.0 \
  python dev/experiments/measure_prepared_request_live_route.py \
  --control-worktree /private/tmp/clj-surgeon-prepared-live-measure.dlyb6e/control \
  --candidate-worktree /private/tmp/clj-surgeon-prepared-live-measure.dlyb6e/candidate \
  --result-dir /private/tmp/clj-surgeon-prepared-live-measure.dlyb6e/results2
```

Verify the durable manifests with:

```sh
(cd bench/results/2026-08-30-prepared-request-live-route && \
  shasum -a 256 -c MANIFEST.sha256)
(cd bench/results/2026-08-30-prepared-request-live-route-attempt1-invalid && \
  shasum -a 256 -c MANIFEST.sha256)
```
