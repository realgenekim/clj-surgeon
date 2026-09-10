# forge@anvil — seat header (installed 2026-09-03 after the move from Buster)

This seat is forge on Anvil (anvil-server, 16 cores, no sudo). Commit identity `forge-anvil <forge-anvil@anvil>`
(exported in the shell), every commit `Co-Authored-By: Gene Kim <genek@itrevolution.com>`. Hard boundaries on
this box: never contact ports 7888 (another seat's production Surgeon), 7894, 7895 (cohort servers); never
touch `~/acid/GO-*`, `~/acid/.cohort-lock`, `chain-*.sh`, or any curtain-call fleet directory; nothing merges
from here — push branches, the mayor merges after independent verification. First action after any
compaction: read `~/src/clj-surgeon/docs/observations/2026-09-03-resume-here-anvil-seat.md`, then the bridge
program note it points to. Answer Gene in the surface he wrote from.

@/home/forge/opt/claude-skills/_global/HOUSE-RULES-CLAUDE.md
@/home/forge/opt/claude-skills/_doctrine/house-rules.md

<!-- BEGIN CLJ-SURGEON ROUTING v:1 -->
## Clojure editing

**Native `rg` plus a native patch is the default route for reading and editing
Clojure.** Ordinary edits stay native: 81 arm-runs found 2x wall/actions and no
quality win. Receipt: `docs/observations/2026-09-02-captains-log-bridge-wall-clock-ideal-program.md`.

## Strictly better, or native (2026-09-08)

**Strictly better, or native.** Route automatically only when the task matches a witnessed contract and the complete receipt path is available; otherwise use native. On one clear refusal repair once, then native fallback with a receipt. Meter complete verified wall, first-attempt success, fallback and unknown telemetry; retire a route when evidence no longer clears its native control.

TWO classes are routed automatically: alias migration and exact-contract namespace split. Outside them native is the
PERFORMANCE default; an explicit user request or a separately approved experiment
may still use any other capability. The JSON examples below are SCHEMA EXAMPLES INSTANTIATED against a fixture repository
and executed there, published byte-identical to the request that ran. Substitute the
task inputs: `workspace_root`, lib, Var, file, form, path and every count. Ratios, fixtures, caveats and how the
meter is operated live in
`docs/observations/2026-09-06-strictly-better-evidence.md`.

### Fan-out -- one call, many named owners

**SUSPENDED 2026-09-08 (pair-1: tool 0.68×/0.59× native at 3 and 21 sites, equal acceptance; retest only as a whole-intent redesign)**
Capability example only; no automatic fan-out route.
0.68×/0.59× are native/tool wall ratios: tool was slower (181.3/123.7 s and
197.8/116.6 s tool/native). This loss is scoped to the informed 3/21-site tasks,
not every fan-out size. September 7 receipts:
`docs/observations/2026-09-07-pair-1-result.md` and `docs/observations/2026-09-07-pair-1/`.
Cohort I's discovery-dependent 1.75× remains historical evidence, not permission
to route unknown-owner discovery: `docs/observations/2026-09-06-fanout-I-result.md`.

```json
{"workspace_root": "/var/tmp/forge/fable-strict-fx/scratch",
 "edits": [{"file": "src/maven/inbox.clj", "within": {"form": "add"},
            "from": "(mdb/get-ds)", "to": "(mdb/get-ds2)", "matches": 1},
           {"file": "src/maven/inbox.clj", "within": {"form": "snooze"},
            "from": "(mdb/get-ds)", "to": "(mdb/get-ds2)", "matches": 1},
           {"file": "src/maven/tweets.clj", "within": {"form": "search"},
            "from": "(mdb/get-ds)", "to": "(mdb/get-ds2)", "matches": 1}]}
```

<!-- executed 2026-09-06 20:03Z, this exact request, against a scratch copy of the
     fanout-B seed: ok, atomic commit, 3 edits / 2 files, 293.94 ms. -->

### Alias migration -- one whole-repository call

Trigger: known old/new alias intent (`from` lib+Var, `to` lib+Var, an alias policy)
and an eligible path scope with its expected file count. No proof profile and no
pre-enumerated match set: the measured run used neither. Afterwards run the
repository's own required load and tests unless the receipt explicitly proves them.

```json
{"op": "alias_migration", "workspace_root": "/var/tmp/forge/fable-strict-fx/scratch",
 "from": {"lib": "maven.db", "var": "get-ds2"},
 "to": {"lib": "maven.relaxed-search", "var": "tokenize",
        "alias_policy": ["rsearch", "rs", "relaxed", "r-search"]},
 "scope": {"paths": ["src"]}, "expect": {"files": 2}}
```

<!-- executed 2026-09-06 20:03Z, this exact request, against a scratch copy of the
     fanout-B seed: ok, atomic commit, 2 files / 3 sites, 628.12 ms. -->

### Namespace split -- EXACT witnessed contract only

Only the frozen Cell C source shape AND manifest qualify (experimental).
Fully mapped single-source partition: every top-level `def`/`defn`/`defn-` owner
named once to a destination `{lib,file,forms,alias_policy[,doc]}`; supplied
`promotion_policy=promote-required`, `source_retirement=delete`,
`roots=[src,test]` and a named cold `verification.profile`.
Witness: curtaincall-cfp `d9205abc`, `src/cfp_scheduler_killer/views.clj`:
141 named owners / 20 absent destinations / 87 static sites / five caller files.
One call: `clj-surgeon :op :split-ns! :request-file X` or MCP `namespace_split`.
X is the complete frozen request with `workspace_root` rebound to the owned
fixture; MCP takes that same request data. A changed mapping/policy/source shape needs new admission.

Proof over the current snapshot requires ALL of:
`state=committed`, `verification_complete=true`, `proof_pending=[]`,
every required profile check present with `:exit 0`; all failed checks refuse.
Nonzero exit is excused ONLY with a `:baseline` map on `captured-reference-analysis`
or `candidate-lint-delta`. Any other nonzero exit refuses, even `:status "passed"`.
`papercuts` must pass when the profile carries that oracle.
A warm `committed-probe-only` receipt is unfinished proof. Run outstanding
user-required checks/review; a commit alone proves no behavior.

Escape: plan-only first when the mapping is uncertain (`plan_only=true` in X).
A safely correctable typed refusal: repair once from `next_call`, then native.
Read mutation/commit status FIRST; stale/conflicting evidence requires refresh.
Never re-run a committed split; finish pending proof or follow guarded recovery.
Kill switch: a correctness failure or complete verified
wall loss vs the registered controls suspends this route.

Non-claims: dynamic references, open-ended decomposition, other source grammars
(CLJC/CLJS/reader conditionals or unsupported macros), callers outside roots,
partial source retention, and generic fully mapped partition routing.
September 7–8 receipts and exact manifest:
`docs/observations/2026-09-07-plan-2-cellC-result.md`,
`docs/observations/2026-09-08-namespace-split-papercuts-round3.md`,
`/var/tmp/forge/round/r5-report.md`, and the September 8 row-1 ledger at
`/home/forge/src/clj-surgeon-records/docs/observations/2026-09-08-row1-split-ledger.md`.
Historical cross-wave ratios are not fresh matched estimates; B02 owns observer
qualification. The p9–p12 September 8 regrades in `/var/tmp/forge/coldstart/ledger.txt`
are workflow PASS evidence, not split controls or friction-low certification.

### Optional supporting read

`inspect_clojure` is a SUPPORTING read, not an automatic route: use it when
structural information is actually needed and native discovery has not already
supplied it. Never insert an inspect pass after sufficient native discovery. The
root-level `expect` is required. A wildcard `_` matches ONE subtree, so it does not
enumerate calls of arbitrary arity -- write the exact arity you mean.

```json
{"workspace_root": "/var/tmp/forge/fable-strict-fx/scratch",
 "requests": [{"id": "r1", "operation": "outline", "file": "src/maven/db.clj"},
              {"id": "r2", "operation": "match", "file": "src/maven/inbox.clj",
               "match": "(mdb/get-ds)"},
              {"id": "r3", "operation": "match", "file": "src/maven/tweets.clj",
               "match": "(mdb/get-ds)"}],
 "expect": {"requests": 3, "files": 3}}
```

<!-- executed 2026-09-06 20:03Z, this exact request, against a scratch copy of the
     fanout-B seed: ok, read_complete=true, 3 requests / 3 files / 12 matches,
     74.6 ms. Note the topology: three DISTINCT files, so expect.files is 3. -->

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

**Other capabilities** (`:extract!`, `:rename-ns!`, `:fix-declares!`,
MCP `require_change`, surgical `within` + `from`/`to`, `:ls-deps`, `:topo`)
remain available by explicit request or separately approved experiment.

**Do not use (measured losers):** per-form writes -- N separate calls -- for a
fan-out change (one native patch does 21 owners in one cell; the batched
single call remains a capability example above); `apply_clojure_changes` with
`owner {:kind "namespace"}` or forms-scoped `find`+`replace` for insertion (it
re-prints the whole owner — hundreds of untouched lines); the CLI wrapper as a
substitute for MCP (a second layer, refuses 2.2x).

*Derived from doctrine commit 3c2eb6a9 on clj-surgeon astra/namespace-split (2026-09-08).*
Intent and evidence: `docs/intent/agent-routing/agent-routing-design.md`.
Branch check: `make check-agent-routing PLATE_ONLY=1` (no installed agreement claim).
Operator: announce the suspension/admission and affected Codex/Claude seats before
`make install-agent-routing`; then `make check-agent-routing` must prove installed parity.

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

<!-- END CLJ-SURGEON ROUTING v:1 -->

## Native policy (cohort arm N)
Edit Clojure files only with the Edit/Write tools or a reviewed apply_patch on the exact forms; batched patches are fine. Do not use clj-surgeon. Do not write scripts (sed/perl/awk/python/heredocs) to edit files. Read `git diff` before finishing.
