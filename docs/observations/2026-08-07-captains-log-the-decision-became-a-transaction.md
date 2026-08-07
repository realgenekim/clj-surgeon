# Captain's Log: The Decision Became a Transaction

**Date:** 2026-08-07

## Question

Can one coherent model decision remain one verified edit transaction when the
goal names a Var but the exact definition, callers, and tests are not yet in the
model's context?

## The route we wanted

```text
goal
  |
  v
inspect_clojure
  mode: prepare-change
  subject: namespace/name
  intent: one semantic decision
  |
  |  cclsp + clojure-lsp resolve the semantic surface
  |  clj-surgeon retains hashes and lossless addresses
  v
┌──────────────────────────────────────────────┐
│ basis cb-...                                 │
│ s1 definition  (defn ...)       replace ?   │
│ s2 caller      (target x)       keep/replace│
│ s3 test        (target 1)       keep/replace│
└──────────────────────────────────────────────┘
  |
  |  model fills each hole once
  v
apply_clojure_changes
  |
  |  preflight hashes -> compile retained paths
  |  -> atomic commit -> changed-file checks
  |  -> read-back hashes -> inverse receipt
  v
terminal verified result
```

The caller does not restate files, owners, selectors, counts, hashes, or test
commands. Apply does not repeat semantic resolution.

## What became real

The branch now runs two persistent local services:

```text
clojure-lsp <-> cclsp :7890 <-> clj-surgeon :7888 <-> Codex or Claude
```

cclsp gained:

- a loopback Streamable HTTP server;
- a stable `/healthz` endpoint;
- a pinned repo-local Bun;
- restart-on-save TypeScript development;
- `resolve_var_surface`, which accepts one fully qualified Clojure Var and
  returns its exact definition plus owner-enriched references;
- HTTP, ambiguity, namespace-path, typecheck, lint, and full-suite tests.

clj-surgeon gained:

- `inspect_clojure` `prepare-change` mode;
- a bounded, one-hour, 32-entry basis store;
- lossless semantic-position-to-zipper-address selection;
- an addressed transaction compiler that does not rerun selectors;
- basis-backed `apply_clojure_changes`;
- stale-source preflight, overlap refusal, atomic write, verification rollback,
  read-back hashes, and durable inverse receipts;
- compact MCP text with complete evidence in `structuredContent`;
- one-command startup and status for both hot services.

## Dogfood drew blood

The first live prepare request failed even though unit tests passed. MCP
supplied a Java map. `clojure.walk/keywordize-keys` did not normalize it. A
transport-shaped Java-map regression now guards the JSON boundary.

The next call failed in the compact formatter. The existing formatter assumed
that every successful inspect result contained `source_character_count`.
Prepare results have decision-site counts instead. The handler now has a
separate source-free prepare summary.

The final full suite caught a schema defect that focused tests did not. The
new mutually exclusive basis/direct schema was valid JSON Schema, but
clojure-mcp published it as an empty input schema because the root contained
only `oneOf`. The real stdio smoke refused to accept that loss of closed-world
validation. The schema now has one closed root object, the union of legal
properties, and two mutually exclusive required-field branches. Basis requests
also reject unknown and mixed-route fields in the runtime before they read
retained state. The smoke and pure bypass regressions now guard both layers.

The first self-hosted apply committed correctly, but launchd's minimal `PATH`
could not find `clojure`. Verification failed and the transaction restored the
original file. That rollback was not a synthetic test. It was the live product
protecting its own source.

After the path fix, the same change succeeded and produced a durable receipt.
The change updated the docstring of
`clj-surgeon.mcp-contract/normalize-success-receipt`. Two resolved call sites
were explicit `keep` decisions.

## The performance surprise

The first successful apply exposed a bad verification policy. The profile named
`fast` ran the complete MCP suite.

| Stage | Before | After | Change |
|---|---:|---:|---:|
| Semantic prepare, one definition and two call sites | 0.45 s | 0.45 s | unchanged |
| clj-kondo | included in full suite | 0.19 s | changed files only |
| Standard Clojure Style | included in full suite | 2.50 s | changed files only |
| Inner-loop verification | 45.65 s | 2.69 s | 17.0× faster |

Whole-file parsing, hash preflight, atomic commit, read-back hashing, rollback,
and receipt publication remain in both routes. `full` still runs `make test` at
the release gate.

This removed more than 40 seconds from the mechanical inner loop. It does not
yet prove a 3x end-to-end clean-agent win. The next benchmark must compare
complete correct tasks against direct MCP and native patching.

## The clean caller completed the intended two-call route

The benchmark asked a fresh Codex caller to change a return contract and update
all direct callers and tests. The prompt named the behavior, not the patch. An
external harness checked the final files and did not ask the caller to run the
tests.

The hill climb had three treatment stages. The negative stages matter because
they identified product defects, not model mistakes.

| Stage | Correct | Wall | Route | What changed next |
|---|---:|---:|---|---|
| Native control | yes | 54.13 s | Two searches, two bounded reads, one native patch, and final checks | Control |
| MCP transport died before the task | yes | 86.24 s | Caller fell back to the CLI | Keep the benchmark services alive in the same harness lifetime |
| Reference sites contained only call expressions | yes | 95.75 s | Prepare succeeded, but the caller reread source and reconstructed a direct request before native fallback | Return each complete named owner and make the basis schema unambiguous |
| Complete owner forms and closed basis schema | yes | **31.00 s** | One prepare call and one basis apply call | Retain the route and replicate it |

The final run used no source reads after the skill, no shell commands, no
retry, and no native file change. Prepare returned four decision sites in two
files in 293 milliseconds. The caller replaced the definition, kept one
unchanged consumer, and replaced one production consumer and one test owner.

Apply made three structural edits across two files. Changed-file verification
took 1,355.25 milliseconds: 109.75 milliseconds for clj-kondo and 1,245.50
milliseconds for the style check. It returned the terminal receipt in the same
call.

The final treatment saved 23.13 seconds, reduced wall time by 42.7%, and ran
1.75x as fast as native. The change from the preceding treatment removed 64.75
seconds by deleting recovery and reconstruction work. The parser and transport
did not produce that gain. The return shape did.

This is one paired probe. It proves that the intended route is usable and can
beat native by double-digit seconds. It does not establish a median or satisfy
the 3x product goal.

## Hot reload proof

The clj-surgeon MCP stayed on PID 46329 and port 7888 while handler and kernel
namespaces reloaded through its embedded nREPL. The failed prepare request was
retried successfully without restarting the server.

cclsp stayed on PID 44130 and port 7890 while its TypeScript source changed.
The live server then returned the new `resolve_var_surface` behavior. A tool
schema change still requires an MCP server restart because the SDK publishes
schemas during initialization. Handler logic does not.

The final reconnect probe tested the harder direction. The cclsp process
changed from PID 4461 to PID 6961. The clj-surgeon process remained PID 4902.
The next prepare request reconnected without operator repair and returned the
same three sites across two files in 1.17 seconds.

## Release-gate evidence

The final verification did not weaken or bypass an existing gate:

| Surface | Result |
|---|---:|
| Existing clj-surgeon suite | 552 tests, 4,816 assertions |
| Focused clj-surgeon MCP suite | 52 tests, 525 assertions |
| cclsp suite | 263 passed, 5 skipped, 815 assertions |
| Real stdio MCP smoke | passed; two closed tool schemas and refusal channel verified |
| Changed Clojure lint | 0 errors, 0 warnings |
| cclsp lint and typecheck | passed |
| cclsp dependency audit | 0 known vulnerabilities |

The full suite found one integration defect: the Babashka test runner loaded
the optional Java MCP client even when no semantic provider was configured.
The semantic namespace now loads only when the live MCP configuration contains
a cclsp URL. Normal CLI startup and tests remain independent of the MCP SDK.

## What remains before a 3x claim

- Replicate the frozen clean-agent portfolio with this exact two-call contract.
- Compare correct total wall time, calls, input tokens, output tokens, and
  recovery rounds against direct MCP and native patching.
- Check whether 12,000 visible source characters are enough for representative
  Var surfaces.
- Study whether agents copy `next_call` without changing basis or site IDs.
- Add only features that remove a measured decision or recovery round.

## Bottom line

The semantic graph is rented. The transaction remains clj-surgeon's authority.
The model makes one decision, and the compiler now carries that decision from
resolved evidence to verified bytes.
