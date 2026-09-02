# Prepared confirmation and preview repaired successor: independent GO

Date: 2026-08-30

## Verdict

**GO** for the bounded live-route measurement and install-card preparation of
exact candidate `05f5a1962e5a0c5aa0365c673994eca9024c1a44` (tree
`7cb0f58bdc4d8469d1f7757b0f0ee65e61f4fdc1`). This is not install authority;
Gene must explicitly approve this exact candidate before publication.

The deterministic blocker recorded at `11791c75` is closed. The original
independent probe now passes through both real MCP transports, and the complete
prepared-confirmation matrix remains green. No typed contradiction was found.

No install, reload, or shared MCP publication was performed.

## Identity and intent

- Candidate: `05f5a1962e5a0c5aa0365c673994eca9024c1a44`
- Tree: `7cb0f58bdc4d8469d1f7757b0f0ee65e61f4fdc1`
- Frozen real-wire red: `be80f9a3163cdbc739c00fab848f2e6329f27102`
- Prior NO-GO base: `90486da5d4d2d4ed7a6efed443142105a2b41d2d`
- Ratified packet: `714cadab`
- The owning HLD, prepared-request design/spec, W1/W2 design/spec, and
  adversarial-review files have no diff from the ratified packet.
- The detached candidate worktree was clean before and after verification.

## Registered red and repaired green

The permanent real-wire red at `be80f9a3` reproduced exactly:

- 2 tests
- 24 assertions
- 10 failures
- 0 errors

Both transports rejected the valid public request as
`invalid-prepared-confirmation`, and both fixtures remained byte-identical.

At the successor:

- Permanent real-wire namespace: 3 tests / 27 assertions / 0 failures / 0
  errors.
- Prepared confirmation plus real-wire namespaces: 22 tests / 119 assertions /
  0 failures / 0 errors.

The repair recursively converts SDK `java.util.Map` and `java.util.List`
containers at the shared callback boundary before any of the four public tool
handlers run. The confirmation validator also normalizes its direct public
envelope before keyword admission. Nested `fill` paths remain strings and the
closed unknown/duplicate-field laws remain intact.

## Original independent probe

Probe source: `/tmp/w1w2_transport_probe.clj`

Probe SHA-256:
`9541fd001fd77c64757e00aa7f8fd23b736de5df4fa01de6d2b6888d922cc92f`

Exact result at the successor:

```clojure
{:ok true,
 :results
 [{:transport :http,
   :ok true,
   :cross-session :unknown,
   :preview :inert,
   :commit :single-use}
  {:transport :stdio,
   :ok true,
   :preview :inert,
   :commit :single-use}]}
```

The probe obtained real descriptors from `inspect_clojure`, submitted
`{confirm, fill}` and `{confirm, fill, preview:true}` as JSON, and proved:

- both transports accept the official request shapes;
- another HTTP session receives the indistinguishable
  `prepared-confirmation-unknown` refusal;
- preview does not mutate source;
- commit produces the exact intended source once;
- replay receives `prepared-confirmation-consumed`;
- HTTP session cleanup retains no tombstone.

## Complete adversarial matrix

The 19-test pure/integration matrix and the real-wire tests jointly cover:

- same-session join and cross-session unknown identity;
- bounded registry expiry, eviction, collision disablement, tombstones, and
  consume-once before every terminal transaction outcome;
- exact request shape and hole set;
- reconstruction from the frozen descriptor and stale-snapshot refusal;
- preview compiler purity, three-use bound, result-size bounds, and no commit
  authority;
- exact verification forecast;
- ineligible/unsupported result object-identity restoration;
- byte-identical coaching and allowlisted telemetry only;
- no install or shared-runtime authority.

All passed.

## Other-tool Java-container spot check

The original real-wire probe exercises `inspect_clojure` through both JSON
transports before confirmation. An additional direct
`java.util.LinkedHashMap` plus nested `java.util.ArrayList` inspect request
validated as one ordinary forms request with `forms=["alpha"]`. The four-tool
stdio smoke also passed:

```clojure
{:ok true,
 :operation :mcp-stdio-smoke,
 :tools ["inspect_clojure" "apply_clojure_changes"
         "edit_clojure" "transform_clojure"],
 :response-count 3}
```

No classic-route regression was found.

## Repository gates

- Core: 647 tests / 5,562 assertions / 0 failures / 0 errors.
- Analyzer contract: 4 tests / 20 assertions / 0 failures / 0 errors.
- MCP: 341 tests / 3,776 assertions / exactly 2 failures / 0 errors.
- The two MCP failures are the previously characterized, unrelated
  `cold-clj-kondo-admission-timeout-is-unverified` wall-clock race: expected
  `:clj-kondo-admission-timeout` and `:admission-timeout`, observed
  `:clj-kondo-admission-unverified` and `:delegated`. The isolated namespace
  repeated the same 2 failures in 7 tests / 50 assertions. The deterministic
  clock repair exists on its separate next-release lane; W1/W2 does not touch
  that code.
- MCP heap, analyzer-admission path, analyzer target, cclsp launch, and direct
  cclsp-client audit self-tests passed.
- `mcp-smoke` passed.
- Diff check and candidate cleanliness passed.

The known cold-clock flake is recorded, not hidden or relabeled green. It is
not a W1/W2 contradiction and does not invalidate the exact public-wire proof.

## Next gates

1. Run the frozen live-route measurement and record the actual confirmation
   emission/time effect.
2. Send the exact candidate and measurement receipt to Gene for an explicit
   install decision.
3. After W1 lands, rebase substantiation telemetry and require the separate
   cross-feature witness that the official `{confirm, fill}` route produces
   exactly one prepared-consumption event.
