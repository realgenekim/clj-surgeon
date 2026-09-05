# Server-Side Telemetry Coverage — design

## The failure this leaf exists for

On 2026-09-05 the Astra program read an hour of clj-surgeon MCP server
telemetry (`~/.local/state/clj-surgeon/telemetry`, via `make
study-agent-usage`) and reported **zero tool calls** for an hour that
contained an attested `alias_migration` call. Three defects, one class:

1. `alias_migration` emitted no telemetry event at all. Telemetry was written
   by each tool's own handler, so a tool shipped without that call was simply
   invisible — and nothing failed when one was.
2. `edit_clojure` calls were written under the **internal** name
   `apply_clojure_changes`, because `mcp-telemetry/call-event` hardcoded that
   string and both public entrances share one handler. Every compact edit in
   the record was attributed to a tool the caller never invoked.
3. The collector inferred ownership from launcher names, so it could not
   attribute a call to the caller or session that made it, and an absent
   telemetry root was indistinguishable from a quiet hour.

The seat hit the same class on 2026-09-03: a telemetry-root mismatch between
`make mcp-serve`'s default and the collector's default produced a false zero.
**A reporting surface that can say "zero" for a busy hour is worse than no
surface, because zero terminates investigation.**

## The design

**One boundary, not eight opt-ins.** `mcp-server/dispatch-tool-fn` is the only
function through which `create-structured-async-tool` reaches a public tool's
callback, and both transports (stdio and streamable-HTTP) build every
specification through `configure-specification` → `create-async-tool` →
`create-structured-async-tool`. Recording there means a tool cannot opt out,
and a tool added later is recorded without its author doing anything.

**The public name, never the internal one.** The event names the entrance the
caller invoked. `edit_clojure` and `apply_clojure_changes` share a handler; the
entrance is the only name a per-caller report can honestly attribute.

**Refusals and throws are calls.** A refusal recorded nowhere is exactly what
makes real work read as zero. `record!` is compare-and-set, so a tool that
publishes through its callback and then throws still produces exactly one
event.

**Coverage is enforced by set equality, not by review.** The witness drives
every tool in `public-tool-registry` once and compares the set of names in
telemetry against a hand-written literal catalog — the refusal-kind
enumeration pattern. A tool added without an event fails in a count that names
it. A public tool registered without `:structured?` would bypass the boundary
invisibly, so `create-async-tool` refuses it rather than shipping a silent gap.

**The collector reports what it read.** Per public tool name, per session id,
per telemetry root, with the roots printed in the receipt and the window
stated. A root that exists but holds no files reports `no files under <root>`;
it never contributes a zero that reads as measured quiet.
