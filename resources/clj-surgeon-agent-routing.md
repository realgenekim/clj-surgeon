<!-- BEGIN CLJ-SURGEON ROUTING v:1 -->
## Clojure editing

**Native `rg` plus a native patch is the default route for reading and editing
Clojure.** Do not reach for clj-surgeon for ordinary edits. Measured 2026-09-02
(81 arm-runs, verified servers, two blind judges; receipts in clj-surgeon
`docs/observations/2026-09-02-captains-log-bridge-wall-clock-ideal-program.md`):
an agent told Surgeon is expected pays about 2x wall and 2x actions with no
quality meter clearing the noise floor, because it keeps its native read/patch
loop and layers the tool on top. Given a free choice, agents decline it, and
decline it correctly.

**Call Surgeon only for these, and only when one applies** — each has no native
equivalent or measured as a win:

- `:extract!` — move forms to a new namespace.
- `:rename-ns!` — structural namespace rename.
- `:fix-declares!` — eliminate removable `declare`s.
- MCP `require_change` — add or change a require across many namespaces
  (measured: nine namespaces, zero churn).
- MCP `within` + `from`/`to` — a surgical edit inside one known form
  (measured: zero churn).
- `:ls-deps` / `:topo` — dependency structure before a large refactor.

**Do not use (measured losers):** per-form writes for a fan-out change (one
native patch does 21 owners in one cell); `apply_clojure_changes` with
`owner {:kind "namespace"}` or forms-scoped `find`+`replace` for insertion (it
re-prints the whole owner — hundreds of untouched lines); the CLI wrapper as a
substitute for MCP (a second layer, refuses 2.2x).

**Every Surgeon MCP operation relays the same terminal-response contract.**
If `terminal_response` is present and this mutation completes all remaining
user-requested work, return its value exactly. Do not add text, reread, or
reverify. If work remains, do not return `terminal_response`. Treat it as
terminal evidence for this operation and continue. `next_action=none` and
`terminal_response` describe only the completed mutation. They never prove
that the complete user request is finished.

**Lint through `~/bin/clj-kondo`**, always. This paved entrance serializes
analyzers across agents, repositories, and JVMs; an absolute Homebrew path
bypasses that serialization and is the cause of contention failures.

**Direct cclsp and clojure-lsp MCP clients are retired.** Do not discover,
register, start, or call them from an agent session.

*Reversible: re-open the default route when clj-surgeon-q5z (batch intent across
N owners) and clj-surgeon-az8 (unrecoverable refusal classes) land and the acid
apparatus shows rung-L non-test actions at or below native's.*
<!-- END CLJ-SURGEON ROUTING v:1 -->
