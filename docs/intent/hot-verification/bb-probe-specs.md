# Warm probe requirements

Probe is built, not certified. `docs/intent/probe/refusals.edn` must enumerate
the literal client/server refusal vocabulary, with the native failure each
prevents. The shared envelope completeness witness compares the registry to
the source vocabulary and requires native-failure records. RECEIPT-BOOL-001
includes probe as the third verb, discovered from the CLI operation catalog:
every boolean in a passed probe verdict requires a driven literal-false seam.
A real `probe/verdict` with tests executed and no failures omits
verification_complete; no constant boolean is fabricated by the witness.
Receipt keys (EDN): `#{:state :proof_pending :reloaded :tests :assertions :failures :elapsed_ms}`
This is the complete untruncated verdict shape for passed and failed test runs;
a caught execution exception may additionally carry :error. The shape witness
parses this statement and compares it to an executed successful probe receipt.

- [x] **BB-PROBE-001**: When a namespace probe runs tests, it shall return a passed or
failed inner-loop verdict without verification_complete, pending landing-gate,
executed test/assertion/failure counts, reloaded namespaces and elapsed_ms.
Zero tests is failed. Misreading: zero failures without executed tests is proof.

- [x] **BB-PROBE-002**: When worktree root, startup generation or classpath/server
fingerprint differs, the probe shall refuse as stale-probe-image before reload.
Malformed namespace/request data shall refuse without evaluation. Misreading:
the port or a surviving descriptor alone identifies the correct image.

- [x] **BB-PROBE-003**: When an identity-admitted probe requests a namespace,
the warm MCP image shall authorize its resolved canonical source under the
repository's `test/` inventory root before dependency traversal or reload.
A resolved target outside that root refuses as
`:probe-target-not-a-test-namespace`, carrying the requested symbol, resolved
repository-relative source, authorized roots and an empty reload list.
Target refusal keys (EDN): `#{:state :error-type :error :proof_pending :requested :source :authorized-roots :reloaded :elapsed_ms}`
An absent local source retains `:probe-namespace-not-found`. For an authorized
target, serially reload its local dependency closure before running that namespace;
dependencies may resolve under `src`, `test`, and `libs/clj-splice/src`.
Oversized source retains its typed refusal. The refusal text names the prevented
native failure: a warm image executing production code on request, with no test
to bound it.
Misreadings: image identity authorizes any production target; all dependencies
must be tests; start a second analysis JVM for each probe or silently run old tests.

- [x] **BB-PROBE-004**: When the servlet encodes a probe result larger than
16,384 UTF-8 bytes, it shall emit a complete bounded EDN projection before
writing any response bytes. A bound must degrade, never delete: retain state,
pending proof, test/assertion/failure counts and elapsed time; include
:error-type :probe-response-truncated, :reloaded-count, a prefix of at most 64
reload names that fits the bound, and :truncated {:bound 16384 :encoded original-bytes
:omitted omitted-name-count}. Oversized refusal messages use the same projection;
their original keyword refusal type is retained as :cause when its encoding
fits 128 bytes.
An oversized :error is replaced by a bounded message, preserving the CLI's
nonzero failure exit.
At 16,384 bytes emit the original encoding; at 16,385 bytes degrade. Both failed
and passed verdicts retain their status. No partial EDN stream is permitted.
Misreadings: enforce only the client read guard; count characters instead of
UTF-8 bytes; assume 64 names always fit; drop the verdict to satisfy the bound.
Witnesses: mcp-http-server-test/probe-output-degrades-without-deleting-the-verdict
and mcp-http-server-test/probe-servlet-bounds-the-actual-writer include Sol's
5,000-dependency case and multibyte/oversized names.

Implementation tags: probe.clj and mcp_hot_verify.clj. Pure witnesses extend
help-test/resolve-op-canonical-ops, preserving its census identity. Live red,
fixed, stale and wall witnesses are recorded in docs/plans/edit-to-probe.md.
The client is babashka; the server is the repository's existing MCP JVM.
The inventory is not a declaration that every portable namespace is safe for hot reload.
