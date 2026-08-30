# Substantiation telemetry frozen red declaration

Status: declared before product implementation.

The first red namespace shall contain exactly **14 tests, 70 assertions,
70 failures, and 0 errors** against installed baseline
`9af88fbae9ee720613599feaf8cf58432c5898bb`. Each test owns exactly five
observable assertions. A missing production Var must become assertion failure,
not namespace-load error.

| Test | Laws | Five observations |
|---|---|---|
| `closed-chained-event-envelope` | 001 | schema, sequence, prior digest, event digest, call ID |
| `privacy-tokens-preserve-equality-without-content` | 002 | HMAC equality, inequality, no raw subject, no plain digest, key ID |
| `segment-is-private-new-and-append-only` | 003 | exclusive create, permissions, append prefix, no rewrite, active retention refusal |
| `ledger-write-failures-are-loud-and-gap-visible` | 004 | start blocks execute, finish preserves result, unhealthy latch, alarm, next-call block |
| `caller-identity-comes-only-from-exchange` | 005 | session token, client name, client version, model unknown, forged fields ignored |
| `all-public-tools-project-closed-call-shapes` | 006 | inspect, edit, apply, transform, no public-result change |
| `read-normalization-stages-are-exact` | 007 | omitted operation, omitted IDs, generated count, mixed refusal, explicit control |
| `prepared-request-lifecycle-requires-exact-skeleton` | 008 | emitted, exact consumed, changed shape refused, committed, failed not committed |
| `write-refusal-counters-and-continuation-are-inert` | 009 | firing, rows, omitted, continuation, inertness |
| `recovery-chain-classification-honors-bounds` | 010 | reread, direct retry, other, seventh-call edge, ten-minute edge |
| `classifier-episode-projection-is-complete-or-explicitly-unknown` | 011 | names, locations, kinds, duplicate/cap facts, unknown fields |
| `feature-envelope-admits-elaborator-without-schema-change` | 012 | common shape, registered inclusion, unknown retention, unknown exclusion, elaborator acceptance |
| `marker-and-report-refuse-claims-upgrades` | 013–016 | marker digest, zero counts, measured count, projected rate, promotion refusal |
| `overhead-and-no-model-gates-are-closed` | 017–019 | event bound, pure threshold, append threshold, live threshold, no model/network |

The red commit shall contain intent and tests only. Product namespaces,
handlers, Make targets, and runtime configuration must remain byte-identical to
`9af88fba`. Green must turn this exact namespace to 14/70/0/0 without deleting,
renaming, weakening, or dynamically skipping a test.
