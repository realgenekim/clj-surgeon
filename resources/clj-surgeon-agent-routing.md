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

## Strictly better, or native (witnessed contracts, 2026-09-06)

**Strictly better, or native.** Route automatically only when the task matches a witnessed contract and the complete receipt path is available; otherwise use native. On one clear refusal repair once, then native fallback with a receipt. Meter complete verified wall, first-attempt success, fallback and unknown telemetry; retire a route when evidence no longer clears its native control.

"Strictly better" means better EXPECTED complete verified wall on an ELIGIBLE task
with a bounded escape. It is not a per-invocation guarantee: learning, queue,
provider and refusal costs are real and are charged to the route by the meter.
Three classes are witnessed. Each complete call is HERE, so no routed call requires
opening the skill or any reference first.

### Read route — one bounded snapshot

Outline a large file once instead of reading it; batch known reads and structural
matches into ONE `inspect_clojure` call, one request per file. Use
`include_source=false` on `forms` requests when only names, ranges, counts and
hashes are needed. The root-level `expect` is REQUIRED (it refused 2/2 arms that
omitted it):

```json
{"requests": [{"id": "r1", "operation": "outline", "file": "src/app/core.clj"},
              {"id": "r2", "operation": "match", "file": "src/app/core.clj",
               "match": "(store/find-event _)"},
              {"id": "r3", "operation": "match", "file": "src/app/db.clj",
               "match": "(store/find-event _)"}],
 "expect": {"requests": 3, "files": 2}}
```

Receipts: one match batch over 20 files returned 59 sites with their owning forms in
0.33 s (`docs/observations/2026-09-06-fanout-J-preregistration.md` hand-probe; 0.24 s
on the rebuilt server); ~150x more token-efficient than reading whole files is the
HOUSE RULE's older figure, not tonight's receipt. The claim is TOKENS and
determinism, not wall: served discovery did not cut wall on the fan-out (cohort J).

### Fan-out route — one call, many named owners

Preconditions: known old/new intent, eligible scope, a complete bounded match set
from the read route, and a valid proof profile. The procedure is the Fan-out route
section below; the complete call is:

```json
{"edits": [{"file": "src/app/core.clj", "within": {"form": "handle-event"},
            "from": "(store/find-event id)", "to": "(store2/fetch-event id)",
            "matches": 1},
           {"file": "src/app/db.clj", "within": {"form": "load-row"},
            "from": "(store/find-event id)", "to": "(store2/fetch-event id)",
            "matches": 2}]}
```

### Alias migration route — one whole-repository call

Preconditions: known old/new alias intent (one `from`/`to` lib+var plus an alias
policy), an eligible path scope with its expected file count, and the repository's
own tests available to run ONCE afterwards. No proof profile and no pre-enumerated
match set are required -- the winning run used neither. Complete call:

```json
{"op": "alias_migration",
 "from": {"lib": "acid.fanout.store", "var": "find-event"},
 "to": {"lib": "acid.fanout.store2", "var": "fetch-event",
        "alias_policy": ["store2"], "refer_policy": "alias-qualify"},
 "scope": {"paths": ["src"], "exclude": []},
 "expect": {"files": 21},
 "verify": "clojure -M:test"}
```

Evidence: 1.38x proof-inclusive median (42.9 s native vs 31.1 s tool), 3/3 paired
actors and 6/6 fresh controls correct, one call each, no refusal or native fallback.
Boundary: the fixture was a FIXED NO-COLLISION repository (21 targets, 63 qualified
uses, zero alias collisions), so collision resolution is NOT witnessed; the
preregistered 1.5x prediction was MISSED; controls were serial and pairs concurrent.

### Native by rule everywhere else

Single-feature work (measured 1.03x and 1.83x AS LONG), small edits, extraction
(105.5 s tool vs 98.0 s native median — extraction stays NATIVE after this loss),
anything needing a semantic provider, and anything outside a route's exact
preconditions. If a precondition is unavailable, native IS the fast path.

### One repair, then native

Repair ONE clear argument or refusal error from the refusal text. Then take the
documented native fallback, record the exact refusal, and count zero tool-committed
sites. A second refusal, a stale-source refusal, a refusal loop, or an unavailable
verb leaves the route.

### Receipts retire only the proof they contain

A receipt retires exactly the proof it names over its exact snapshot and nothing
else. BYTE-LEVEL proof, do NOT re-verify: `written bytes read back and verified`,
`verification_complete`. NOT semantic proof: `caller proof · structural candidates
only; not semantic completeness`, and `caller proof unavailable` -- for those, run
the repository's own tests ONCE and stop. No receipt retires user-required review,
independent acceptance, or proof that was never performed.

### Meter and kill switch

The usage collector reports, per routed class, first-attempt success, refusal rate,
fallback rate, and full request-to-verified wall; discovery, schema repair and any
second read are charged to the route, and tool runtime is never subtracted. Each
class also carries a periodic preregistered native pair.

**Kill switch.** Stop routing a class and re-run a preregistered native pair when
the weekly real-work meter shows the class losing its native control, its fallback
or refusal rate rising, or its telemetry unknown. A class that loses a preregistered
pair LEAVES this plate.

**Unwitnessed Surgeon calls** — allowed only when no witnessed route fits and one
of these applies; each has no native equivalent or measured as a win, none has a
preregistered pair, and none is auto-routed:

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
substitute for MCP (a second layer, refuses 2.2x); `:extract!` and any
extraction route (1.08x as long, 105.5 s vs 98.0 s); whole-feature or
single-feature work through any gate or typist (1.03x and 1.83x as long).

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
