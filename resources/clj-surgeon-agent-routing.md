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

## Strictly better, or native (2026-09-06)

**Strictly better, or native.** Route automatically only when the task matches a witnessed contract and the complete receipt path is available; otherwise use native. On one clear refusal repair once, then native fallback with a receipt. Meter complete verified wall, first-attempt success, fallback and unknown telemetry; retire a route when evidence no longer clears its native control.

TWO classes are routed automatically, both mutations. Outside them native is the
PERFORMANCE default; an explicit user request or a separately approved experiment
may still use any other capability. Every lib, Var, file, form, path and count below
is a TASK INPUT, not a constant to copy. Include `workspace_root` whenever the
target is not the server's default project. Ratios, fixtures, caveats and how the
meter is operated live in
`docs/observations/2026-09-06-strictly-better-evidence.md`.

### Fan-out -- one call, many named owners

Trigger: one known old/new form, a complete bounded set of NAMED owners already in
hand, and a valid proof profile.

```json
{"workspace_root": "/absolute/repository",
 "edits": [{"file": "src/app/core.clj", "within": {"form": "handle-event"},
            "from": "(store/find-event id)", "to": "(store2/fetch-event id)",
            "matches": 1},
           {"file": "src/app/db.clj", "within": {"form": "load-row"},
            "from": "(store/find-event id)", "to": "(store2/fetch-event id)",
            "matches": 2}]}
```

<!-- executed 2026-09-06 20:03Z, this exact shape, against a scratch fixture:
     ok, atomic commit, 3 edits / 2 files, 293.94 ms. -->

### Alias migration -- one whole-repository call

Trigger: known old/new alias intent (`from` lib+Var, `to` lib+Var, an alias policy)
and an eligible path scope with its expected file count. No proof profile and no
pre-enumerated match set: the measured run used neither. Afterwards run the
repository's own required load and tests unless the receipt explicitly proves them.

```json
{"op":"alias_migration","workspace_root":"/absolute/repository",
 "from":{"lib":"acid.fanout.store","var":"find-event"},
 "to":{"lib":"acid.fanout.store2","var":"fetch-event",
       "alias_policy":["store2","st2","es","store-2"]},
 "scope":{"paths":["src"]},"expect":{"files":21}}
```

<!-- executed 2026-09-06 20:03Z, this exact shape, against a scratch fixture:
     ok, atomic commit, 2 files / 3 sites, 628.12 ms. -->

### Optional supporting read

`inspect_clojure` is a SUPPORTING read, not an automatic route: use it when
structural information is actually needed and native discovery has not already
supplied it. Never insert an inspect pass after sufficient native discovery. The
root-level `expect` is required. A wildcard `_` matches ONE subtree, so it does not
enumerate calls of arbitrary arity -- write the exact arity you mean.

```json
{"workspace_root": "/absolute/repository",
 "requests": [{"id": "r1", "operation": "outline", "file": "src/app/core.clj"},
              {"id": "r2", "operation": "match", "file": "src/app/core.clj",
               "match": "(store/find-event id)"},
              {"id": "r3", "operation": "match", "file": "src/app/db.clj",
               "match": "(store/find-event id)"}],
 "expect": {"requests": 3, "files": 3}}
```

<!-- executed 2026-09-06 20:03Z, this exact shape, against a scratch fixture:
     ok, read_complete=true, 3 requests / 3 files / 12 matches, 74.6 ms. -->

### Receipts: test values, not field names

Proof is `verification_complete=true` TOGETHER WITH the named successful checks over
the current snapshot; a false or pending field is not evidence. `atomic commit
complete` and `written bytes read back and verified` prove the WRITE, not task
semantics -- do not re-verify a proven write, and do not treat it as behavioural
proof. Run the outstanding required checks and repair any failure before claiming
completion. No receipt retires user-required review, independent acceptance, or a
check that was never performed.

### Escape rule

Repair ONE clear, safely correctable argument error from the refusal text. A stale
or conflicting snapshot needs fresh evidence, not a repair. An unavailable
capability goes native immediately. Before any fallback, read the receipt's mutation
and commit status: a refusal does not imply that nothing was written, and a
completed change is never reapplied blindly.

**Kill switch.** A correctness failure SUSPENDS a routed class immediately. A wall
loss is assessed against that class's controls, never banned on one noisy pair.
Unknown telemetry means unknown PERFORMANCE -- use native pending investigation, not
a recorded loss. Required per routed class: first-attempt success, refusal rate,
fallback rate, and complete request-to-verified wall; collector coverage of those is
itself unproven, so unknowns are retained as unknown.

**Other capabilities** — not automatically routed. Use them on an explicit
request, for a separately approved experiment, or when one of these applies;
each has no native equivalent or measured as a win:

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

Experimental, development-only; it changes other seats' prompts, so announce
before `make install-agent-routing`. For a Clojure edit changing the same call
or symbol inside MANY named top-level forms across files -- a batched,
known-intent fan-out -- this is the default route:

1. Discover owners FIRST: `rg -l`, then one `inspect_clojure` `match` batch,
   one request per file, in bounded file groups, keeping every site and count.
   Truncated output is never complete discovery: size follows source and path
   lengths and the public 32 KB enforcement is defective (inb-b60d6e). "~100
   owners" is a heuristic; splitting discovery never implies per-owner writes.
2. THEN patch helper and `require`/alias natively with `apply_patch`. A helper
   spelled like the target matches itself: exclude ONLY the new helper owner,
   never a whole file, which holds legitimate original sites. If preparation
   changes the discovery snapshot, get fresh guards and counts where required
   -- old observations are not current write authority.
3. ONE `apply_clojure_changes` call, edits
   `[{file, within {form}, from, to, matches}]`, using the alias each file
   binds. Counts convert to edits only for the same concrete from/to inside
   each NAMED owner: wildcard totals need not equal literal replacement counts,
   `inside` null is not a `within.form`, an omitted `source` means the result's
   `match` only under the documented exact-equality rule.
4. Clear argument error: repair once from the refusal. Route unavailable,
   unsupported, or refusing again: one native patch, record the reason --
   native fallback counts as zero tool-committed sites. Conflict or
   stale-source refusal: refresh the relevant evidence first.

**Evidence and boundary.** Cohort I measured the INFORMED BATCHED EDIT route
(fresh actors discovering owners themselves) at 1.75x proof-inclusive median,
101.2 s vs 57.8 s; frozen-witness outcomes tool 4/4, native 3/4 with a known
layout false negative -- no quality-superiority claim. Served discovery in cohort J was wall-neutral. `owner_counts` is a later
usability change with no measured additional wall gain; its 0/4 was that spelling-sensitive witness failing the
self-match workaround, not four self-recursion defects. This witnessed class
ONLY: not a general Clojure editing default; whole-feature work stays native.

*Derived from doctrine commit 7a682b9e on clj-surgeon MCP/main, whose receipts
are `docs/observations/2026-09-06-two-hour-trial-closeout.md`,
`2026-09-06-fanout-I-result.md` and `2026-09-06-fanout-J-ethnography.md`.*

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
