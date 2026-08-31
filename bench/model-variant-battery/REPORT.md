# Elaborator fallback model-variant battery

## Verdict

Run the flywheel with this deployment ladder:

1. **Spark** (`gpt-5.3-codex-spark`) — primary when quota is available.
2. **Terra** (`gpt-5.6-terra`) — guarded fill fallback.
3. **Sol** (`gpt-5.6-sol`) — rescue fallback when Terra fails a guard.
4. **Luna** (`gpt-5.6-luna`) — last; it produced one malformed Clojure replacement in six.

Spark and Terra were both 6/6 exact on the guarded fill class. Spark's median model-generation wall was 0.922 s versus Terra's 1.558 s: Spark used 40.8% less wall, a 1.69x throughput proxy. The full guarded bang was 1.460 s versus 2.258 s, 35.3% less wall.

Terra is the honest operational pick for “reliable enough but faster than Sol” on the workload the flywheel actually performs: it was 6/6 exact with zero wrong-subject and schema failures, and its 1.558 s median model-generation wall was 48.9% lower than Sol's 3.051 s. Keep the structural guards and Sol behind it: Terra failed all five fixed-length decode oracles by stopping at an exact numeric prefix between 88 and 165 rather than reaching 200, so this battery does **not** establish generalized Terra decode superiority.

Under the frozen confirmatory rule, no non-Sol model earned the stronger label “proven faster than Sol”: Terra had no valid fixed-length decode estimate, while Luna was less reliable and its resolved bootstrap-subtracted rate rounded to the same 58.1 tok/s as Sol. The deployment ladder above explicitly uses direct fill wall as an operational tie-break where the preregistered decode tie-break is missing or unresolved.

## Decode-rate results

Five B trials requested integers 1 through 200 and five interleaved C trials requested only `ok`. The oracle isolated structured agent messages, normalized comma/whitespace separators, and required exact semantic content. E2E rate is median decoded tokens divided by median B wall. The bootstrap-subtracted estimate is `(median B decoded - median C decoded) / (median B wall - median C wall)`.

| Model | Exact B | Median B wall | Median decoded B | E2E tok/s | Bootstrap-subtracted tok/s | Resolved? |
|---|---:|---:|---:|---:|---:|---:|
| Spark | 5/5 | 3.486 s | 533 | 152.9 | 5,243.9 | **No** |
| Terra | 0/5 | n/a | n/a | n/a | n/a | No valid trials |
| Luna | 5/5 | 10.688 s | 437 | 40.9 | 58.1 | Yes |
| Sol | 5/5 | 10.739 s | 403 | 37.5 | 58.1 | Yes |

Spark was 4.08x Sol's E2E decoded-token rate (+307.7%) and 3.74x Luna's (+273.8%). Its bootstrap-subtracted number is not defensible: B and C medians differed by only 82 ms, below the preregistered `2 * (MAD(B) + MAD(C)) = 1,092 ms` resolution threshold. Spark also spent a median 103 hidden-plus-visible decoded tokens on the one-word control, versus 533 on B. The floor subtraction therefore amplifies timing noise, and 5,243.9 tok/s must not be used as a point estimate.

Terra's five B responses were exact prefixes with last integers 133, 130, 109, 88, and 165. Their usage and wall receipts remain in the raw facts, but the frozen oracle excludes them from both rates. No prompt-echo or transcript grep participates in any score.

## Reliability on guarded fills

| Model | Exact | One-shot applied | Wrong subject | JSON schema fumbles | Clojure parse fumbles | Median model wall | Median bang wall |
|---|---:|---:|---:|---:|---:|---:|---:|
| Spark | 6/6 | 6/6 | 0 | 0 | 0 | 0.922 s | 1.460 s |
| Terra | 6/6 | 6/6 | 0 | 0 | 0 | 1.558 s | 2.258 s |
| Sol | 6/6 | 6/6 | 0 | 0 | 0 | 3.051 s | 3.742 s |
| Luna | 5/6 | 5/6 | 0 | 0 | 1 | 1.487 s | 2.002 s |

Luna's failed `fill-branch-call` response was a schema-valid replacement string missing one final closing parenthesis. Surgeon refused it before writing. It was neither retried nor misclassified as a JSON-schema fumble.

## Spark addendum

Spark was scheduled at 2026-08-30 21:35:52 PDT and started at exactly 2026-08-31 00:45:00 PDT, three minutes after the user-observed 00:42 quota reset. It completed at 00:45:52 PDT. Decode and daemon warmup exit codes were zero; no usage-limit, rate-limit, quota, reroute, or meter fact occurred.

The requested Spark-versus-best-fallback delta is:

- Guarded fill reliability: tied, 6/6 exact and 6/6 one-shot for Spark and Terra.
- Model-generation fill wall: Spark 0.922 s versus Terra 1.558 s; Spark is 40.8% lower wall / 1.69x throughput proxy.
- Full guarded bang wall: Spark 1.460 s versus Terra 2.258 s; Spark is 35.3% lower wall / 1.55x throughput proxy.
- Fixed-length decode: no valid Spark-versus-Terra delta exists because Terra produced 0/5 exact-length outputs. Against Sol, the fastest reliability-tied fallback with a valid decode series, Spark's E2E decoded-token rate is 4.08x; their bootstrap-subtracted rates are not comparable because Spark's subtraction was unresolved.

## Receipts and scope

- Frozen preregistration commit, pushed before the first battery call: `e3df8ee7e6c3a47cee0de3874305b3477ebaf4c2`.
- Immediate-arm and schedule checkpoint: `873ebfc9cb42f0d83fdc1286ad7e74e5995cb085`.
- Experiment base: `eb751875a877772e291305ac5b5239aceeb89bfa`.
- Supplied rig target: `4ec9394c59805addef05076cf2c78c463b8ea6e6`.
- Rig server SHA-256: `209aa9b799b5819bbd60332084698c36b59df0f996c30a6a07a32b8438ddbad8`.
- Rig client SHA-256: `898cb2d8c700b0f225c0f1907ec4056315815ae4fc2839c446666d3a58f57491`.
- Decode runner SHA-256: `510f95bd42ab783eafa2bbb03bb59dcef60e07e4f03f10aa39d285388118683c` at source commit `8d3c6f685f1605844e03c6f851f78304e0c7bf41`.
- Normalized scorer SHA-256 and every raw/derived artifact identity are in `MANIFEST.sha256`.

All arms used ChatGPT-account authentication, exact requested model names, low reasoning effort, a clean Codex profile, serial calls, and one warm rig model at a time. Sample sizes stayed frozen at five B plus five C decode trials and six fill trials per model. These are descriptive, quota-courteous measurements from one host and one time window, not population estimates.
