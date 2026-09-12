# Warm probe requirements

Probe is built, not certified. `docs/intent/probe/refusals.edn` must enumerate
the literal client/server refusal vocabulary, with the native failure each
prevents. The shared envelope completeness witness compares the registry to
the source vocabulary and requires native-failure records. RECEIPT-BOOL-001
includes probe as the third verb, discovered from the CLI operation catalog:
every boolean in a passed probe verdict requires a driven literal-false seam.
A real `probe/verdict` with tests executed and no failures supplies
verification_complete=false; no boolean is fabricated by the witness.

- [x] **BB-PROBE-001**: When a namespace probe runs tests, it shall return a passed or
failed inner-loop verdict with verification_complete false, pending landing-gate,
executed test/assertion/failure counts, reloaded namespaces and elapsed_ms.
Zero tests is failed. Misreading: zero failures without executed tests is proof.

- [x] **BB-PROBE-002**: When worktree root, startup generation or classpath/server
fingerprint differs, the probe shall refuse as stale-probe-image before reload.
Malformed namespace/request data shall refuse without evaluation. Misreading:
the port or a surviving descriptor alone identifies the correct image.

- [x] **BB-PROBE-003**: When an admitted probe names a local test namespace, the warm
MCP image shall serially reload its local dependency closure before running
that namespace. Missing source and oversized source have typed refusals.
Misreading: start a second analysis JVM for each probe or silently run old tests.

Implementation tags: probe.clj and mcp_hot_verify.clj. Pure witnesses extend
help-test/resolve-op-canonical-ops, preserving its census identity. Live red,
fixed, stale and wall witnesses are recorded in docs/plans/edit-to-probe.md.
The client is babashka; the server is the repository's existing MCP JVM.
The inventory is not a declaration that every portable namespace is safe for hot reload.
