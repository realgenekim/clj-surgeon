# Row 2 pilot 2 — does a SCHEMA-LOADED fresh Claude cell take the routed alias path? (forge@anvil, 2026-09-08)

The parent pilot (`2026-09-08-row2-fresh-seat-pilot.md`, `f929f55d`) found one Claude cell
(PA2) that issued **22 Bash calls, zero ToolSearch, zero MCP** and never weighed a route at
all, and named the cheap next cell verbatim: *"a Claude arm with the `alias_migration` schema
pre-loaded rather than deferred; if that closes the gap, the finding is a surfacing defect and
not a model difference."* This is that cell, run four times.

Amendment 1 to the parent preregistration, frozen before any cell:
`/var/tmp/forge/row2-pilot2/design/amendment-1.md`, sha256
`8ba702d41e1a37232eb4ad213e4b102e9c8518bc04db78e53285b18813619ddd`. Everything not restated
there is inherited unchanged: fixture, seed commit, task bytes, golden seal, oracle, wall
endpoints, route witness, and the route-NEUTRAL brief (template sha256 `0394a72f…`, verified
equal to the parent's frozen value; every rendered brief verified identical to PB1's modulo
the cell name).

## Headline — all six Claude cells

| cell | schema | route (independent witness) | acceptance class | caller work | primary wall | actions | apparatus | ToolSearch | MCP |
|---|---|---|---|---|---|---|---|---|---|
| PA1 | deferred | **ROUTED** | accepted | 95.8 s | 295.3 s | 18 | 4 (22.2%) | 1 | 1 |
| PA2 | deferred | native | accepted-with-minor | 121.2 s | 252.5 s | 22 | 4 (18.2%) | **0** | **0** |
| PB1 | **loaded** | **ROUTED** | accepted | 123.4 s | 235.4 s | 21 | 4 (19.0%) | 1 | 1 |
| PB2 | **loaded** | **ROUTED** | accepted | 106.6 s | 645.0 s † | 17 | 3 (17.6%) | 1 | 1 |
| PB3 | **loaded** | **ROUTED** | accepted | 146.0 s | 410.5 s † | 20 | 4 (20.0%) | 1 | 1 |
| PB4 | **loaded** | native | accepted-with-minor ‡ | 146.7 s | 404.9 s † | 19 | 4 (21.1%) | **0** | **0** |

**Registered gate: PASS, exactly at threshold.** Schema-loaded Claude routed **3/4**
(>= 3/4 required), against the deferred-schema control's 1/2. Accepted **4/4**.

† **PRIMARY wall is contaminated by operator latency and is not comparable across these
cells.** Its endpoint is `orient-start -> oracle-complete`, so it includes however long the
operator took to start grading: about 1 min for PB1, about 4 min for PB3/PB4, and about 8 min
for PB2 (a waiter timed out). CALLER work — `orient-start -> last candidate-complete`, entirely
inside the cell — is the clean figure and is the one the parent compared to stage 3.

‡ Both numbers published, as the parent did for PS1. PB4's **raw oracle is O2 FAIL + O5 FAIL,
`ACCEPTED: NO`**; the registered stage-3 classifier `classify3.py` — which the preregistration
names as the acceptance measure and whose ruling counts *"valid insertion position of the new
target libspec, and whitespace / continuation-line re-indentation"* as MINOR — returns
**`accepted-with-minor`**. Raw: `PB4/oracle-run.txt`. Neither the oracle nor its allowlist was
modified.

### Caller work, cold

| group | n | median caller work |
|---|---|---|
| routed, Claude only (PA1, PB1-PB3) | 4 | **115.0 s** |
| routed, all cold cells incl. the parent's two Sol cells | 6 | **101.2 s** |
| native (PA2, PB4) | 2 | **133.9 s** |

Routed / native = 0.859 Claude-only (**1.16x less caller work**), 0.756 pooled (**1.32x**).
The pooled figure sits almost exactly on the parent's registered 1.36x. With n=2 native these
are signals, not estimates.

## The result that matters most: the routed cells agree byte-for-byte

The three routed candidate patches are **byte-identical** — `PB1/candidate.patch`,
`PB2/candidate.patch`, `PB3/candidate.patch` all sha256
`0649dd0bad24821981c73355fb6299a8b959922830dd016d332bf78faedfec1b`, 266 lines each. Every one
reports the same receipt shape: 9 files, 21 sites, `alias_histogram {"mjson": 9}`, 3 collisions
resolved, `string_mentions 0`, and 579 tests / 7,833 assertions / 0 failures.

The native cell's patch is different and larger: `3ccb8bca…`, 282 lines. **The whole of the
difference is churn the task's minimality clause excludes.** PB4 inserted
`[marvin-voice-remote.json :as mjson]` in *alphabetical* position rather than replacing the
retired libspec in place, which reorders the require block in the three files that needed a new
libspec (`app_route`, `server`, `tts`) — that is the O2.d token-stream divergence, first
difference at token 7 in each. It then needed two `perl` passes to re-align continuation lines
"whose first-argument alignment shifted when the head token shortened by three characters."
None of the routed cells produced any of that, because none of them had to: the verb replaces
in place and does not touch layout.

## One learning

**Surfacing is the lever — and surfacing the definition is not the same as surfacing the
decision.** Making the tool definition present at launch moved fresh Claude cells from 1/2 to
3/4 routed, which is what the parent predicted and is the registered gate. But the honest
reading is narrower than "solved", for two reasons visible in the transcripts.

First, **PB4 reproduced PA2's signature exactly under a strictly stronger treatment**: 19 Bash
calls, **zero ToolSearch, zero MCP**, no sentence anywhere weighing a route — with the complete
tool definition sitting in the boot file it had read. It even *noticed the block*: it volunteered
that "`CLAUDE.md` was already modified in the worktree before I started". It saw the definition
and still made no decision. So the failure mode the parent named survives the fix aimed at it;
what schema presence changes is the odds, not the mechanism.

Second, **the three routed cells each still spent one `ToolSearch`** (`select:mcp__clj-surgeon__alias_migration`,
1/1/1). The boot block removed the *discovery* cost — knowing the verb exists and what shape it
takes — but not the *fetch* action. A genuine tool-list pre-load would save one further action
per cell, and this pilot cannot tell whether saving it changes anything else.

**And the routed cells reached for the verb for the same stated reason, in their own words.**
PB1: *"That made the task an exact match for the routed alias-migration class."* PB3: *"That
matched the routed alias-migration contract."* PB2: *"the auto-routed class in my doctrine."*
All three did a native survey FIRST — grep for sites, ns forms, per-file alias occupancy — to
establish the 9/21 shape before composing the request, exactly the "fresh caller wants a gate,
not a second editor" behaviour already on this seat's record. That survey is most of the cold
route's caller-work penalty over the directed stage-3 arms.

**The native tax showed up again, in a second form.** The parent's native cell nearly rewrote
protected docstring prose because a line-number `sed` guard was computed before a same-file
deletion. PB4 used the same technique — *"`bridge3_new.clj:280` … excluded by line number from
the sed pass"* — and got away with it, then paid a different native-only tax instead: layout
churn that no routed cell can produce. Two native cells, two distinct classes of avoidable
damage, zero for four routed cells.

## One caveat

**This is a boot-block emulation, not a real pre-load, and the emulation is visible.** The
Agent tool exposes no way to force an MCP schema into a subagent's tool list on this seat
(`~/.claude/agents/` does not exist, there is no project agent directory or settings key that
pins a deferred tool, and `subagent_type: "fork"` would inherit this session's entire context
including the preregistration). So the schema-loaded state was produced by appending the
verbatim tool definition — name, complete description, complete JSONSchema — plus the routing
plate's verbatim JSON example to the specimen worktree's own `CLAUDE.md`
(`/var/tmp/forge/row2-pilot2/design/schema-block.md`, sha256 `7856ac3f…`, 3,738 bytes, quoted
in full below). Three consequences, none of them hidden:

1. **The append leaves `CLAUDE.md` dirty in `git status`**, which PA1/PA2's worktrees were not.
   Three of the four cells noticed and said so unprompted. That tell could plausibly *raise*
   attention to the block, which would inflate the treatment — so the 3/4 is an upper bound on
   what a silent pre-load would achieve, not a lower one. (Against that: PB4 noticed it and
   still never looked.)
2. **The tool's own `description` travels with the schema** and is persuasive prose
   ("in a single call whose payload does not grow with the number of affected files"). It is
   reproduced verbatim because it is part of what a schema-loaded cell sees; this pilot cannot
   separate the effect of the schema from the effect of that description.
3. **3/4 versus 1/2 is not a statistically distinguishable difference.** The gate was
   registered at >= 3/4 and it passed as registered, but at these n the result is consistent
   with no effect at all. It is a directional finding that earns a bigger cell, not a
   measurement.

## Apparatus defects, both mine, both named

1. **PB1's untracked-path check ran after staging.** The first invocation passed the worktree
   path where `check-untracked.sh` wants the ARM name, so it self-concatenated and reported a
   spurious FAIL. Re-run correctly it is **PASS** — exactly the nine caller files — but by then
   `grade-arm.sh` had already run `git add -A`, so PB1's check is post-staging while PB2-PB4 are
   pre-staging. The path *set* is unaffected by staging (the script strips the status prefix and
   compares paths). CX-1: an apparatus defect is never a behaviour FAIL.
2. **The attestation's `brief_identical_to_parent` cross-check reported NO for PB2-PB4** because
   it re-rendered the PARENT's template, which the contamination seal makes unreadable during a
   cell. The template in `row2-pilot2/design` is a verified byte-identical copy (sha256
   `0394a72f…`), and each rendered brief was verified identical to PB1's modulo the cell name.
   The flag is wrong; the briefs are not.

Contamination: `contamination_path_touches=none` in all four transcript summaries — no cell read
`/var/tmp/forge/row2`, the parent's cell directories, or either design directory. The parent's
per-cell evidence directories were additionally `chmod 000` for the whole of this pilot.

## The appended block, in full

```

## Tool definition already loaded in this environment

The definition below is reproduced here in full so that its shape is known without a
lookup step. In this harness the tool is listed as deferred, which means one
`ToolSearch` call with query `select:mcp__clj-surgeon__alias_migration` is what makes it
invocable; that call returns exactly the definition reproduced here and nothing further.

<functions>
<function>{"description": "Migrate one Var to a new namespace and name across every namespace that requires the old one, in a single call whose payload does not grow with the number of affected files. Send from {lib, var}, to {lib, var, alias_policy}, scope {paths}, and expect {files}. Surgeon discovers every requiring namespace and every call site itself under every spelling that file makes legal — each :as alias, the fully qualified name, and the bare referred name — chooses each file's alias as the first alias_policy entry bound to nothing in that file, rewrites the require and every site, and commits one failure-atomic transaction. Locals of the same name, strings, docstrings, comments, metadata, #_ discards, and every reader-conditional branch other than the file's own platform branch stay byte-identical. Never send a per-file, per-owner, or per-site table; Surgeon discovers them. The receipt is one constant-size object: files, sites, the alias histogram, collisions resolved, the kondo delta, the focused-test result, and a details_path holding per-file detail, retained best-effort: read it from the receipt rather than assume the path keeps. Its receipt is terminal evidence of the rewrite; do not re-read the files it changed. A refusal is fail-closed and carries an executable next_call: send that once.", "name": "mcp__clj-surgeon__alias_migration", "parameters": {"additionalProperties": false, "properties": {"expect": {"additionalProperties": false, "properties": {"files": {"minimum": 0, "type": "integer"}}, "required": ["files"], "type": "object"}, "from": {"additionalProperties": false, "properties": {"lib": {"minLength": 1, "type": "string"}, "var": {"minLength": 1, "type": ["string", "null"]}}, "required": ["lib", "var"], "type": "object"}, "op": {"const": "alias_migration", "type": "string"}, "scope": {"additionalProperties": false, "properties": {"exclude": {"items": {"minLength": 1, "type": "string"}, "type": "array"}, "paths": {"items": {"minLength": 1, "type": "string"}, "minItems": 1, "type": "array"}}, "required": ["paths"], "type": "object"}, "to": {"additionalProperties": false, "properties": {"alias_policy": {"items": {"minLength": 1, "type": "string"}, "minItems": 1, "type": "array"}, "lib": {"minLength": 1, "type": "string"}, "refer_policy": {"enum": ["preserve-refer", "alias-qualify"], "type": "string"}, "var": {"minLength": 1, "type": ["string", "null"]}}, "required": ["lib", "var", "alias_policy"], "type": "object"}, "verify": {"description": "Optional verification profile named by the repository in .clj-surgeon.edn under :verification-profiles. A name this workspace does not configure is refused before any write. lint is the only built-in and runs a lint and format gate: it is NOT a test profile.", "minLength": 1, "type": "string"}, "workspace_root": {"minLength": 1, "type": "string"}}, "required": ["from", "to", "scope", "expect"], "type": "object"}}</function>
</functions>

An example request instantiating that schema:

```json
{"op": "alias_migration", "workspace_root": "/var/tmp/forge/fable-strict-fx/scratch",
 "from": {"lib": "maven.db", "var": "get-ds2"},
 "to": {"lib": "maven.relaxed-search", "var": "tokenize",
        "alias_policy": ["rsearch", "rs", "relaxed", "r-search"]},
 "scope": {"paths": ["src"]}, "expect": {"files": 2}}
```
```

## Fixture and integrity

Inherited unchanged and re-attested per cell: seed repo `/var/tmp/forge/row2/seed-repo` at
`6dcdbc9db67a91179b5940a16c16b760583f2603` (history-isolated, no remote), seeded `json.clj`
`5e2b4151…`, task `ec4c2a9e24fad8fbc4f0733454f773df2d1b20c61129f2de0ff402d9b9c331a2`, golden
bytes `chmod 000` for the whole of every cell, MCP server on this seat's port **7906**, one arm
JVM at a time with `uptime` before each and a refusal above load 12 (the pilot waited out a
spike to 20.42 before the first cell; every attestation records its own loadavg).

Per cell the attestation additionally proves the manipulation did not corrupt the fixture: the
seed `CLAUDE.md` blob hash, the post-append hash, **the retained prefix hash equal to the seed
blob** (so the original bytes were not rewritten), the appended tail hash equal to the frozen
block, and `git status` showing `M CLAUDE.md` and nothing else. The route-instruction spelling
scan runs against the SEED boot bytes, before the append.

The oracle was used unmodified: `oracle2/run_oracles_v2.sh` `0bc09aec…`, `classify3.py`
`17c80f30…`, `check-untracked.sh` `944e2a5c…`, `wall2.sh` `d8b8b82d…`, `grade-arm.sh`
`5757ccd8…`.

Route witness for each cell is the MCP server's own receipt bundle, bound to the worktree by
absolute path: PB1 `…/4a239820…/b4f5da60-0496-4ddf-b6cb-b3ae99b0aee6.edn`, PB2
`…/460082c8…/6f54c129-34e5-44ad-9f9a-9e9f9a170ef7.edn`, PB3
`…/96c927af…/895c20f2-ad0d-452b-91d0-8d3bc0e77f93.edn`, PB4 **none**. Transcript and witness
agreed on all four cells.

One protocol deviation, self-reported by PB2: it never stamped `mutation-end`, and declined to
add it afterwards because the stamp would sort after `candidate-complete` and be false data.
No wall endpoint uses `mutation-end`, so no measure is lost.

## Paths

* amendment + frozen schema block + brief template: `/var/tmp/forge/row2-pilot2/design/`
* apparatus, per-arm attestations, defect notes: `/var/tmp/forge/row2-pilot2/apparatus/`
* per-cell evidence (brief, stopwatch, agent report, transcript summary, route witness, oracle
  outputs, candidate patch, wall): `/var/tmp/forge/row2-pilot2/{PB1,PB2,PB3,PB4}/`
* worktrees, preserved: `/home/forge/src/mvr-row2-{PB1,PB2,PB3,PB4}`
* parent pilot: `docs/observations/2026-09-08-row2-fresh-seat-pilot.md` (`f929f55d`),
  apparatus `/var/tmp/forge/row2-pilot/`
* stage 1-3: `docs/observations/2026-09-08-row2-alias-preregistration.md`,
  `docs/observations/2026-09-08-row2/`

Nothing was committed or pushed in any specimen or in clj-surgeon; this file is the only commit
this pilot makes.
