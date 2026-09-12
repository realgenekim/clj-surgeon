# Probe crossing repair

Sol's production-verdict reproduction exposed an output crossing with no server
bound. The client already limits input to 16,384 characters. The server will
limit complete UTF-8 EDN output to 16,384 bytes before obtaining its writer.
This ensures every emitted stream is complete EDN within the existing guard.

The normal receipt is byte-for-byte its current pr-str encoding. An oversized
receipt keeps state, pending proof, test/assertion/failure counts and elapsed
time. It adds :error-type :probe-response-truncated, :reloaded-count, a prefix
of at most 64 reload names, and :truncated {:bound 16384 :encoded original-bytes
:omitted omitted-names}. Prefix length decreases until the whole encoding fits;
even a single giant or multibyte name may therefore produce an empty prefix.
Refusal messages can also be large, so oversized refusal output uses the same
bounded projection and preserves the original refusal type as :cause when its
keyword encoding fits 128 bytes. Oversized error prose becomes a bounded
message; the error field remains present for the CLI's refusal exit behavior.

Misreadings: cut encoded bytes at the boundary; drop the test verdict and return
only an error; measure characters rather than UTF-8 bytes; assume 64 names always
fit; truncate only successful verdicts; classify a degraded result as cold proof.

Witness matrix: a normal receipt, exactly 16,384 encoded bytes, 16,385 bytes,
5,000 reload names, one huge name, Unicode names, failed test status, oversized
refusal, and the actual servlet writer. The servlet witness substitutes only
the expensive reload/test executor and drives the production doPost boundary.
The spec shape witness parses the statement in bb-probe-specs.md and compares
it to a verdict built from an executed Clojure test summary.
