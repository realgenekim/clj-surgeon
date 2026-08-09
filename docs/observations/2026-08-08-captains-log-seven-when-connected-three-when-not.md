# Captain's Log: seven when connected, three when not

Mothership's source-reader retirement was a useful dogfood session because the
work crossed large Clojure view namespaces, exact nested render branches, and a
legacy JavaScript strangler seam.

My overall rating for clj-surgeon in this session is **7/10**. Its structural
read surface was consistently useful. `:ls` exposed named form boundaries in
1,485-, 719-, 568-, and 1,153-line files; named-form `:cat` then returned only
the renderers and tests needed for each decision. Most importantly, one guessed
patch failed because the live form shape differed from remembered context.
Returning to the structural read prevented a broader, riskier edit. The CLI
reads generally completed in roughly 0.2–1.3 seconds.

The persistent MCP experience in the same session was only **3/10 for
availability**. An early semantic prepare found the intended source-fragment
surface, but the later cclsp call returned `invalid-mcp-session` and asked for a
reconnect. The prescribed `clj-surgeon up` recovery then hit the known SCI
`FileLock.close` refusal. The current agent catalog did not expose usable
`inspect_clojure`, `apply_clojure_changes`, or cclsp resolution tools, so the
rest of the work used the sanctioned CLI structural reads, narrow patches, and
execution gates.

The split verdict matters. The product is already better than text search for
orientation and exact-form reading, and the connected semantic surface can
turn a refactor into a bounded transaction. But a capability that disappears
from the agent catalog or refuses at session recovery cannot be the default
paved road. That is why usage looked lower than expected: it was not rejected;
it was unavailable at the points where the next structural write was ready.

The next hill climb is operational, not another query feature: make agent-side
discovery and one reconnect reliably restore the three-tool path—inspect,
resolve, apply—without restarting shared services. If that becomes routine,
this same workflow would rate 9/10.

## The reset button replaced the recovery loop

The reported path is now repaired at every boundary that failed. cclsp returns
the Streamable HTTP session-expiry status, not a generic 400 refusal. Surgeon's
semantic client distinguishes `invalid-mcp-session` from an ordinary semantic
refusal and reconnects exactly once. The Babashka onboarding path no longer
calls a forbidden method on `FileLock`; closing the owning channel releases the
lock, and the ordinary Babashka suite now executes that regression.

`clj-surgeon recover [WORKSPACE]` turns the runbook into one executable test.
The first successful live receipt took 919 ms: onboarding 725 ms and the proof
194 ms. It opened fresh MCP sessions, listed both Surgeon tools, read one exact
source anchor, resolved `clj-surgeon.analyze/file->zloc` under one cclsp LSP
session, applied one guarded edit to a uniquely named tool-owned fixture,
verified the written bytes, and removed the fixture. No shared service
restarted. The terminal result was `:recovered` with `:next-action :none`.

Dogfooding improved the probe twice. Java's default HTTP negotiation stalled
against the Bun server even though curl succeeded; pinning the recovery client
to HTTP/1.1 removed the ten-second false failure. Streamable HTTP tool results
then arrived as server-sent events rather than bare JSON; the probe now accepts
both wire shapes. A third correction preserved the boundary between cclsp's
`status: "ok"` contract and Surgeon's `ok: true` contract instead of pretending
the joined tools share one response schema.

Failure is also terminal. Recovery writes one phase-labeled EDN receipt and
returns one `report-failure` command. The reporter retains only a fixed
diagnostic allowlist, fingerprints it, and creates or updates one local Bead.
Source, prompts, URLs, and workspace paths never enter the report. Off the
development laptop, the same redacted issue draft is returned as data and no
write occurs.

The new operational score is **9/10 on the exercised path**. The remaining
point is empirical: other live agents must show that protocol-correct 404 plus
one bounded reset reliably refreshes their own deferred tool catalog without a
session restart.

## The repeated `documentSymbol` tax

The SMW recovery witness made one wire-level cost visible. The string
`textDocument/documentSymbol` is an LSP method name, not Clojure source text.
The slash separates the LSP `textDocument` method family from its
`documentSymbol` operation.

The current anchored route performs these operations for
`cli.export-markdown/atomic-write!`.

First, Surgeon reads the exact named form through MCP:

```json
{
  "name": "inspect_clojure",
  "arguments": {
    "workspace_root": "/workspace",
    "requests": [{
      "id": "owner",
      "operation": "forms",
      "file": "src/cli/export_markdown.clj",
      "forms": ["atomic-write!"],
      "expect": {"forms": 1}
    }],
    "expect": {"requests": 1, "files": 1}
  }
}
```

The result proves the file, owner, whole-form range, and SHA-256 source hash.
cclsp then receives that proof through its MCP surface:

```json
{
  "name": "resolve_var_surface",
  "arguments": {
    "workspace_root": "/workspace",
    "var": "cli.export-markdown/atomic-write!",
    "include_declaration": true,
    "source_anchor": {
      "file": "src/cli/export_markdown.clj",
      "source_sha256": "40a3813be5bf34ea464001d4ed40843e5d7e0675d4fe17594c731546972d0a2a",
      "owner": "atomic-write!",
      "range": {"start": {"line": 11, "character": 0},
                "end": {"line": 32, "character": 0}}
    }
  }
}
```

cclsp synchronizes the exact bytes with clojure-lsp. The first LSP message is
a notification, so it has no request ID and no response:

```json
{
  "jsonrpc": "2.0",
  "method": "textDocument/didOpen",
  "params": {
    "textDocument": {
      "uri": "file:///workspace/src/cli/export_markdown.clj",
      "languageId": "clojure",
      "version": 1,
      "text": "<the hash-verified file bytes>"
    }
  }
}
```

Today cclsp then asks clojure-lsp for every declared symbol in that file:

```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "textDocument/documentSymbol",
  "params": {
    "textDocument": {
      "uri": "file:///workspace/src/cli/export_markdown.clj"
    }
  }
}
```

The response is an array of `DocumentSymbol` values. cclsp locally filters the
array for the one symbol named `atomic-write!` whose range lies inside the
Surgeon anchor. That filtering is ordinary TypeScript computation, not another
LSP command. Finally, cclsp sends the query that supplies the actual semantic
value:

```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "method": "textDocument/references",
  "params": {
    "textDocument": {
      "uri": "file:///workspace/src/cli/export_markdown.clj"
    },
    "position": {"line": 11, "character": 7},
    "context": {"includeDeclaration": true}
  }
}
```

The SMW file is only 72 lines. Nevertheless, two cold clojure-lsp children each
spent 10 seconds on `documentSymbol` and timed out. The second failure produced
a correct `semantic-provider-timeout` after 21.755 seconds. No references query
ran. The expensive step was rediscovering syntax identity that Surgeon already
knew.

## Proposed anchored fast path

Surgeon should extend `source_anchor` with the exact range of the owner-name
token:

```clojure
{:file "src/cli/export_markdown.clj"
 :source_sha256 "40a381..."
 :owner "atomic-write!"
 :range {:start {:line 11 :character 0}
         :end {:line 32 :character 0}}
 :selection_range {:start {:line 11 :character 7}
                   :end {:line 11 :character 20}}}
```

cclsp can then verify the file hash, range containment, and exact token bytes.
It can synchronize the file and call `textDocument/references` directly at
`selection_range.start`. The definition comes from the hash-bound Surgeon
anchor. The references come from clojure-lsp. Surgeon maps returned reference
ranges back to exact top-level owners while it prepares the change basis.

This preserves the authority boundary:

```text
Surgeon       exact syntax, owner token, file hash, reference-owner mapping
cclsp/LSP     resolved reference relationships and one LSP session
Surgeon       guarded transaction, verification, and receipt
```

For an anchored request, the target contract is zero
`textDocument/documentSymbol` calls. Unanchored and ambiguous exploration can
retain `documentSymbol` as a fallback. The acceptance test must inspect the
cclsp flight recorder, prove zero anchored `documentSymbol` requests, preserve
the exact reference set and hashes, and compare cold and warm SMW timings. The
expected large gain is the removal of one or two 10-second discovery timeouts;
the benchmark, not the design, must establish the realized wall-clock gain.
