<!-- BEGIN CLJ-SURGEON ROUTING v:1 -->
## Clojure editing

**Native `rg` plus a native patch is the default route for reading and editing
Clojure.** Ordinary edits stay native: 81 arm-runs found 2x wall/actions and no
quality win. Receipt: `docs/observations/2026-09-02-captains-log-bridge-wall-clock-ideal-program.md`.

## Strictly better, or native (2026-09-08)

**Strictly better, or native.** Route automatically only when the task matches a witnessed contract and the complete receipt path is available; otherwise use native. On one clear refusal repair once, then native fallback with a receipt. Meter complete verified wall, first-attempt success, fallback and unknown telemetry; retire a route when evidence no longer clears its native control.

**The MCP surface is retired everywhere (Gene, 2026-11-28, ruling 71 b: "ready to retire permanently right now").** No clj-surgeon MCP server is registered or started by any seat; every operation below is the CLI (`clj-surgeon :op ...`). Nothing on main is deleted by this ruling.

NO class is routed automatically. Namespace split is being ported to the CLI (Gene, 2026-11-28, ruling 69 a, after the fleet confirms the assumptions) and is not routed until that port lands; alias migration is frozen off main (below). Outside them native is the
PERFORMANCE default; an explicit user request or a separately approved experiment
may still use any other capability. Measured ratios, fixtures and caveats live in `docs/observations/2026-09-06-strictly-better-evidence.md`.

### Fan-out -- one call, many named owners

**SUSPENDED 2026-09-08 (pair-1: tool 0.68×/0.59× native at 3 and 21 sites, equal acceptance; retest only as a whole-intent redesign)**
Capability example only; no automatic fan-out route.
0.68×/0.59× are native/tool wall ratios: tool was slower (181.3/123.7 s and
197.8/116.6 s tool/native). This loss is scoped to the informed 3/21-site tasks,
not every fan-out size. September 7 receipts:
`docs/observations/2026-09-07-pair-1-result.md` and `docs/observations/2026-09-07-pair-1/`.
Cohort I's discovery-dependent 1.75× remains historical evidence, not permission
to route unknown-owner discovery: `docs/observations/2026-09-06-fanout-I-result.md`.

### Alias migration -- frozen off main

**SUSPENDED and frozen off main 2026-11-28 (Gene: 64a "let's purge it from the code", then 75 "frozen off main is good"). Not routed, not ported to main; the verb and its tests are on `astra/namespace-split` and its archive tag `archive/astra/namespace-split`, never on main.**
The weekly sentinel's `verdict=SUSPEND` on 2026-10-18 and 10-25 was not a correctness loss of the verb: every surviving output matches the golden; the oracle apparatus failed and its records were reaped (`docs/observations/2026-11-28-alias-migration-sentinel-verdict.md` on main, pull request 77).

### Namespace split -- EXACT witnessed contract only

Only the frozen Cell C source shape AND manifest qualify (experimental).
Fully mapped single-source partition: every top-level `def`/`defn`/`defn-` owner
named once to a destination `{lib,file,forms,alias_policy[,doc]}`; supplied
`promotion_policy=promote-required`, `source_retirement=delete`,
`roots=[src,test]` and a named cold `verification.profile`.
Witness: curtaincall-cfp `d9205abc`, `src/cfp_scheduler_killer/views.clj`:
141 named owners / 20 absent destinations / 87 static sites / five caller files.
One call, once the port lands: `clj-surgeon :op :split-ns! :request-file X`.
X is the complete frozen request with `workspace_root` rebound to the owned
fixture. A changed mapping/policy/source shape needs new admission.

Proof over the current snapshot requires ALL of:
`state=committed`, `verification_complete=true`, `proof_pending=[]`,
every required profile check present with `:exit 0`; all failed checks refuse.
Nonzero exit is excused ONLY with a `:baseline` map on `captured-reference-analysis`
or `candidate-lint-delta`. Any other nonzero exit refuses, even `:status "passed"`.
`papercuts` must pass when the profile carries that oracle.
A warm `committed-probe-only` receipt is unfinished proof. Run outstanding
user-required checks/review; a commit alone proves no behavior.

Escape: plan-only first when the mapping is uncertain (`plan_only=true` in X).
A safely correctable typed refusal: repair once from the refusal's named next step, then native.
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

**Kill switch.** A correctness failure SUSPENDS a routed class immediately. A wall
loss is assessed against that class's controls, never banned on one noisy pair.
Unknown telemetry means unknown PERFORMANCE -- use native pending investigation, not
a recorded loss. Required per routed class: first-attempt success, refusal rate,
fallback rate, and complete request-to-verified wall; collector coverage of those is
itself unproven, so unknowns are retained as unknown.

**Other capabilities** (`:extract!`, `:rename-ns!`, `:fix-declares!`,
`:ls-deps`, `:topo`)
remain available by explicit request or separately approved experiment.

**Do not use (measured losers):** per-form writes -- N separate calls -- for a
fan-out change (one native patch does 21 owners in one cell).

*Derived from doctrine commit 2b405234 on clj-surgeon astra/namespace-split (2026-11-28).*
Intent and evidence: `docs/intent/agent-routing/agent-routing-design.md`.
Branch check: `make check-agent-routing PLATE_ONLY=1` (no installed agreement claim).
Operator: announce the suspension/admission and affected Codex/Claude seats before
`make install-agent-routing`; then `make check-agent-routing` must prove installed parity. Installation follows the read-only census of that machine (`bin/surgeon-footprint`).

**Lint through `~/bin/clj-kondo`**, always. This paved entrance serializes
analyzers across agents, repositories, and JVMs; an absolute Homebrew path
bypasses that serialization and is the cause of contention failures.

**Direct cclsp and clojure-lsp MCP clients are retired.** Do not discover,
register, start, or call them from an agent session.

<!-- END CLJ-SURGEON ROUTING v:1 -->
