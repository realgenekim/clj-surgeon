# Warm probe requirements

Probe is built, not certified. `docs/intent/probe/refusals.edn` must enumerate
the literal client/server refusal vocabulary, with the native failure each
prevents. The shared envelope completeness witness compares the registry to
the source vocabulary and requires native-failure records. RECEIPT-BOOL-001
includes probe as the third verb, discovered from the CLI operation catalog:
every boolean in a passed probe verdict requires a driven literal-false seam.
A real `probe/verdict` with tests executed and no failures omits
verification_complete; no constant boolean is fabricated by the witness.
Receipt keys (EDN): `#{:state :proof_pending :reloaded :closure-expected :roots :external :tests :assertions :failures :elapsed_ms}`
This is the complete untruncated verdict shape for passed and failed test runs;
a caught execution exception may additionally carry :error. The shape witness
parses this statement and compares it to an executed successful probe receipt.

- [x] **BB-PROBE-001**: When a namespace probe runs tests, it shall return a passed or
failed inner-loop verdict without verification_complete, pending landing-gate,
executed test/assertion/failure counts, reloaded namespaces and elapsed_ms.
Zero tests is failed. Misreading: zero failures without executed tests is proof.

- [x] **BB-PROBE-002**: When worktree root, startup generation or classpath/server
fingerprint differs, the probe shall refuse as stale-probe-image before reload.
Malformed namespace/request data shall refuse without evaluation.
A bounded request is not a safe request: `clojure.edn`'s reader recurses per
container AND per prefix form, so 8,192 nested `[` fits the 8,192-character
bound and overflows the stack. Before reading any request, the server shall
count reader-recursive openers over the request characters -- `(`, `[`, `{`
(including `#{`), a tagged literal, `^` metadata and `#_` discard, skipping
openers inside a string, a character literal or a comment -- and refuse a
request deeper than the declared `probe/request-depth-bound` (64, against a
closed request's own depth of two) as `:probe-request-too-deep`, carrying
`:bound`, the measured `:depth` and the request `:bytes`, before any parse.
The servlet's request boundary shall be Throwable, not Exception: a
StackOverflowError is not an Exception, and one that escapes kills the thread
of the shared warm image while its client reads zero bytes as a verdict.
Any throwable at that boundary refuses as `:probe-request-unreadable`,
carrying `:throwable-class` and no stack. Both halves hold together: the bound
keeps the parse shallow, and the Throwable boundary holds when the bound is
wrong. Misreadings: the port or a surviving descriptor alone identifies the
correct image; a character bound also bounds parse depth; counting `[` alone
enumerates the class; an escaping error is a server-side detail because the
HTTP status still says 200.

- [x] **BB-PROBE-003**: When an identity-admitted probe requests a namespace,
the warm MCP image shall authorize its resolved canonical source under the
repository's `test/` inventory root before dependency traversal or reload.
A resolved target outside that root refuses as
`:probe-target-not-a-test-namespace`, carrying the requested symbol, resolved
repository-relative source, authorized roots and an empty reload list.
Target refusal keys (EDN): `#{:state :error-type :error :proof_pending :requested :source :authorized-roots :reloaded :elapsed_ms}`
An absent local source retains `:probe-namespace-not-found`. For an authorized
target, serially reload its local dependency closure before running that namespace;
Local source roots shall be derived from the image's actual `java.class.path`:
canonical directory entries in classpath order, never a literal root list.
Resolve each required namespace through the image classloader. A source file
under a local classpath root enters the reload order; a jar entry is external,
skipped and counted once per namespace in `:external`. A dependency with no
classpath resource (including a namespace already loaded in the image), or a
file outside every root, refuses as `:probe-dependency-unresolved` with
`:ns`, `:resolved-to` (resource URL or nil), `:roots`, and zero reloads.
Execution receipts list the canonical roots used in `:roots` for comparison
with the image classpath. Discovery must finish before the first reload.
The shared ns parser shall expand prefix lists (including nested lists), vector
libspecs, bare symbols and strings in `:require`, `:require-macros` and `:use`,
selecting the `:clj` reader-conditional branch (or reader default). Any dependency
form it cannot classify shall refuse as `:probe-require-unparsed`, carrying
`:form` and `:file`, before any reload. Successful and failed execution receipts
shall carry `:closure-expected`, the number of namespaces in the complete local
closure computed before reload, independently of the completed `:reloaded` list.
Bounded HTTP projections shall retain that count. Misreading: a missing prefix
namespace means its children are external; a green target reload proves that
already-loaded dependencies were refreshed.
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
