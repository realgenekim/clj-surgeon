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

**Do not use (measured losers):** per-form writes -- N separate calls -- for a
fan-out change (one native patch does 21 owners in one cell; the batched
single call is the Fan-out route below); `apply_clojure_changes` with
`owner {:kind "namespace"}` or forms-scoped `find`+`replace` for insertion (it
re-prints the whole owner — hundreds of untouched lines); the CLI wrapper as a
substitute for MCP (a second layer, refuses 2.2x).

## Fan-out route (experimental default, 2026-09-06)

For a Clojure edit that changes the same call or symbol inside MANY named
top-level forms across files -- a batched, known-intent fan-out -- this is the
default route (cohort I, preregistered fresh actors: 1.75x on the
proof-inclusive median, 101.2 s native vs 57.8 s, tool 4/4 vs native 3/4):

1. Make helper and `require`/alias changes natively with `apply_patch` first.
2. Discover owners with ONE `inspect_clojure` `match` batch -- one request per
   file from `rg -l` -- reading the per-(file, `inside`) owner counts from the
   result (`owner_counts` where the server supplies them); above ~100 owners,
   split into two batches. Scan BEFORE patching the helper, or exclude the
   file just written: a helper spelled like the target matches itself, and the
   route would rewrite its body into self-recursion (cohort J scored 0/4).
3. Make ONE `apply_clojure_changes` call with edits
   `[{file, within {form}, from, to, matches}]` built from those counts, using
   the alias each file already binds.
4. If refused, read the refusal and repair the arguments; do not fall back to
   native editing of the call sites.

**Class boundary:** the witnessed batched known-intent fan-out class only --
experimental, because one four-pair class does not earn all multi-owner edits
or a production release. It is NOT a general Clojure editing default;
whole-feature work stays native (both whole-task pairs lost, 0.97x and 0.55x).

*Derived from doctrine commit 7a682b9e (clj-surgeon MCP/main,
`docs/observations/2026-09-06-two-hour-trial-closeout.md`; receipts in
`2026-09-06-fanout-I-result.md` and `2026-09-06-fanout-J-ethnography.md`).*

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
