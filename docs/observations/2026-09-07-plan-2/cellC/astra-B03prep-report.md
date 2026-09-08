# Astra B03 prep — branch only

Written: 2026-09-08T04:30:08.195137+00:00
Worktree: `/home/forge/src/clj-surgeon-split`; branch `astra/namespace-split`.
Status: **COMPLETE — branch prep verified; operator installation remains pending.**
Completed: 2026-09-08T04:52:38.469765+00:00
No global installation, push or merge was performed. This does not close B03's
installed-delivery gate, B02 observation qualification, or row 1 friction-low.

## Change and doctrine

- `3c2eb6a9`: routing doctrine, design, HLD link and stable intent registry.
- `ec64f322`: plate suspension/admission, verifier, Make mode and witnesses.
- `cfe024c1`: fresh passing cold battery receipt over ec64f322.
- `d31c3af7`: add the new leaf and three ROUTING IDs to exact intent-census expectations.
- All commits use `forge-anvil <forge-anvil@anvil>` and
  `Co-Authored-By: Gene Kim <genek@itrevolution.com>`.

The suspension preserves Gene's requested exact label and immediately explains
that 0.68×/0.59× are native/tool wall ratios: tool was slower at 3 and 21 sites.
The capability schema remains; neither alias migration nor all fan-out sizes are
suspended by analogy. Unknown-owner discovery does not become a new default.
Split admission is restricted to the frozen Cell C d9205abc views source AND
141-owner / 20-destination manifest, 87 static sites, five callers, explicit
promotion/alias policies, delete-source, roots and named cold verification.
Proof checks actual committed/completed/empty-pending/exit-zero values, including
papercuts when registered; escape and kill rules and non-claims are explicit.

`ROUTING-FANOUT-001`, `ROUTING-SPLIT-001`, `ROUTING-PARITY-001` live in
`docs/intent/agent-routing/agent-routing-specs.md`. The existing checker now
reports doctrine commit and per-target hashes from the same captured original
managed bytes it checks. Doctrine text outside the managed block cannot satisfy
a missing section. Empty installed-target lists refuse. Plate-only validates
markers, required doctrine and budget without reading installed targets.
The local linked-intent-testing workflow at
`/home/forge/opt/claude-skills/linked-intent-testing/SKILL.md` supplied registration
and independent-witness guidance; no separate linked-intent-dev SKILL.md was found.

Budget: before **188 lines / 10,738 UTF-8 bytes**, after **183 lines / 10,661 bytes**
(−5 lines / −77 bytes). No previous executable cap was found; the old measured
budget is now enforced. Removed the obsolete fan-out default recipe and its
reopening footer; condensed the historical introduction and optional-capability
list. Kept alias migration, optional supporting-read schema, receipt and terminal
contracts. Canonical SHA-256:
`57a2abb971b7e6703049d19c17eef5f4ad4d20a182ee4a47a4bd8d126237bbde`.
Pinned doctrine: `3c2eb6a9`.

## Evidence and validation

- Pair-1 September 7: `docs/observations/2026-09-07-pair-1-result.md`, raw
  `docs/observations/2026-09-07-pair-1/`.
- Cell C September 7–8: `docs/observations/2026-09-07-plan-2-cellC-result.md`,
  round-3 observation and `/var/tmp/forge/round/r5-report.md`.
- Row-1 ledger was absent from this checkout. Read its existing frozen copy at
  `/home/forge/src/clj-surgeon-records/docs/observations/2026-09-08-row1-split-ledger.md`;
  the plate cites that actual path. No foreign branch was merged for a prose copy.
- `/var/tmp/forge/coldstart/ledger.txt` p9–p12 all PASS after September 8 regrade.
  These workflow cells are not fresh split controls or observer certification.
- Warm image created by `make nrepl`, port 39065, exact worktree user.dir asserted.
  Red witness log `B03prep/red.log` records 24 intended assertion failures; green **10 tests / 142 assertions**, 0 failures
  or errors (`B03prep/green-final.log`). Tests use disposable installation targets.
- `make check-agent-routing PLATE_ONLY=1`: **PASS**, zero installed targets read,
  exact plate hash/doctrine/budget (`B03prep/plate-check.log`).
- CLI boundaries: explicit plate-only exit 0; missing targets, malformed plate and
  unknown operation exit 2 (`B03prep/cli-boundaries.json`).
- Default real `make check-agent-routing`: expected exit 2, old installed doctrine
  refused (`B03prep/installed-check-expected-drift.log`). Both globals unchanged;
  file hashes captured in `B03prep/global-hashes-before.json`.
- Formatter: standard-clojure-style on both changed Clojure files. Lint through
  `~/bin/clj-kondo`: 0 errors / 0 warnings (`B03prep/lint-final.log`).
- Direct intent audit: **ok true**, violations [], all three new IDs implemented;
  **144 pre-existing pending witness pairs** retained (`B03prep/intent-audit.log`).
- First `make test`: recovery battery 3/3 passed, then freshness refused the old
  4eaabdea receipt at 32 counted commits behind HEAD (limit 30).
  `B03prep/make-test.log` preserves this refusal. Refresh uses the prescribed
  `flock /home/forge/tmp/suite.lock make test-battery`, without changing limits.
- Refreshed battery: **694 tests / 13,332 assertions**, 0 failures/errors/skips,
  0 isolation violations across 32 namespaces; **791 s**, runner-appended receipt
  names ec64f322 (`B03prep/battery.log`). Receipt committed as cfe024c1.
- Second full attempt reached 795 JVM tests and exposed exactly two stale
  intent-census expectations introduced by this new leaf (missing expected path,
  old non-MCP count 193). `B03prep/make-test-final.log` preserves both failures.
  d31c3af7 adds the exact leaf and count 196 plus a ROUTING-prefix count of 3;
  no audit rule or exclusion was weakened. Warm census gate: **21 / 473**, clean
  lint (`B03prep/census-green.log`, `B03prep/lint-census.log`).

- Final `flock /home/forge/tmp/suite.lock make test`: **exit 0**.
  JVM **795 tests / 10,166 assertions**, Babashka **878 / 7,675**, zero
  failures/errors; required shell checks and repository hygiene passed.
  Full retained output: `B03prep/make-test-green.log`.
- Both global file hashes still equal their before-check hashes after all gates
  (`B03prep/global-hashes-after.json`). Git working tree is clean at d31c3af7.
  No install, push or merge occurred. The worktree-local nREPL started for this prep was shut down after verification.

## Operator installation and verification — not executed by Astra

Before installing, announce to the Codex and Claude seats using this shared home:

> I am installing the reviewed B03 routing plate on anvil. This changes
> /home/forge/.codex/AGENTS.md and /home/forge/.claude/CLAUDE.md for all seats
> using them: informed fan-out is suspended at the witnessed 3/21-site regimes;
> namespace_split is admitted experimentally only for the exact frozen Cell C
> contract. Alias migration remains routed. Active sessions may retain old
> instructions; reload them or start a fresh session after verification.

After review, run these commands **on this seat** from the branch worktree:

```bash
cd /home/forge/src/clj-surgeon-split
make check-agent-routing PLATE_ONLY=1
make install-agent-routing CODEX_GLOBAL_INSTRUCTIONS=/home/forge/.codex/AGENTS.md CLAUDE_GLOBAL_INSTRUCTIONS=/home/forge/.claude/CLAUDE.md
make check-agent-routing CODEX_GLOBAL_INSTRUCTIONS=/home/forge/.codex/AGENTS.md CLAUDE_GLOBAL_INSTRUCTIONS=/home/forge/.claude/CLAUDE.md
```

The final command must exit 0 and report `:scope :installed`, `:target-count 2`,
`:doctrine-commit "3c2eb6a9"`, the canonical hash above and that same `:block-hash`
for each named target. It checks exact managed bytes while preserving unmanaged
text. Retain that output as the seat's installation receipt. Plate-only output
cannot substitute for installed agreement. Do not run `make install`, push or
merge as part of these commands. Other homes/seats require their own announced
installation and exact hash check; this receipt covers only the two named files.

## Exact plate diff

```diff
diff --git a/resources/clj-surgeon-agent-routing.md b/resources/clj-surgeon-agent-routing.md
index 7d7f9bf9..0e8b8c73 100644
--- a/resources/clj-surgeon-agent-routing.md
+++ b/resources/clj-surgeon-agent-routing.md
@@ -3,20 +3,18 @@
 
 **Native `rg` plus a native patch is the default route for reading and editing
 Clojure.** Do not reach for clj-surgeon for ordinary edits. Measured 2026-09-02
-(81 arm-runs, verified servers, two blind judges; receipts in clj-surgeon
+(81 arm-runs, verified servers, two blind judges; receipts:
 `docs/observations/2026-09-02-captains-log-bridge-wall-clock-ideal-program.md`):
-an agent told Surgeon is expected pays about 2x wall and 2x actions with no
-quality meter clearing the noise floor, because it keeps its native read/patch
-loop and layers the tool on top. Given a free choice, agents decline it, and
-decline it correctly.
+instructed Surgeon use cost about 2x wall and actions without a quality win;
+free-choice agents declined it.
 
-## Strictly better, or native (2026-09-06)
+## Strictly better, or native (2026-09-08)
 
 **Strictly better, or native.** Route automatically only when the task matches a witnessed contract and the complete receipt path is available; otherwise use native. On one clear refusal repair once, then native fallback with a receipt. Meter complete verified wall, first-attempt success, fallback and unknown telemetry; retire a route when evidence no longer clears its native control.
 
-TWO classes are routed automatically, both mutations. Outside them native is the
+TWO classes are routed automatically: alias migration and exact-contract namespace split. Outside them native is the
 PERFORMANCE default; an explicit user request or a separately approved experiment
-may still use any other capability. Each example below is a SCHEMA EXAMPLE INSTANTIATED against a fixture repository
+may still use any other capability. The JSON examples below are SCHEMA EXAMPLES INSTANTIATED against a fixture repository
 and executed there, published byte-identical to the request that ran. Substitute the
 task inputs: `workspace_root`, lib, Var, file, form, path and every count. Ratios, fixtures, caveats and how the
 meter is operated live in
@@ -24,8 +22,14 @@ meter is operated live in
 
 ### Fan-out -- one call, many named owners
 
-Trigger: one known old/new form, a complete bounded set of NAMED owners already in
-hand, and a valid proof profile.
+**SUSPENDED 2026-09-08 (pair-1: tool 0.68×/0.59× native at 3 and 21 sites, equal acceptance; retest only as a whole-intent redesign)**
+Capability example only; no automatic fan-out route.
+0.68×/0.59× are native/tool wall ratios: tool was slower (181.3/123.7 s and
+197.8/116.6 s tool/native). This loss is scoped to the informed 3/21-site tasks,
+not every fan-out size. September 7 receipts:
+`docs/observations/2026-09-07-pair-1-result.md` and `docs/observations/2026-09-07-pair-1/`.
+Cohort I's discovery-dependent 1.75× remains historical evidence, not permission
+to route unknown-owner discovery: `docs/observations/2026-09-06-fanout-I-result.md`.
 
 ```json
 {"workspace_root": "/var/tmp/forge/fable-strict-fx/scratch",
@@ -58,6 +62,45 @@ repository's own required load and tests unless the receipt explicitly proves th
 <!-- executed 2026-09-06 20:03Z, this exact request, against a scratch copy of the
      fanout-B seed: ok, atomic commit, 2 files / 3 sites, 628.12 ms. -->
 
+### Namespace split -- EXACT witnessed contract only
+
+Only the frozen Cell C source shape AND manifest qualify (experimental).
+Fully mapped single-source partition: every top-level `def`/`defn`/`defn-` owner
+named once to a destination `{lib,file,forms,alias_policy[,doc]}`; supplied
+`promotion_policy=promote-required`, `source_retirement=delete`,
+`roots=[src,test]` and a named cold `verification.profile`.
+Witness: curtaincall-cfp `d9205abc`, `src/cfp_scheduler_killer/views.clj`:
+141 named owners / 20 absent destinations / 87 static sites / five caller files.
+One call: `clj-surgeon :op :split-ns! :request-file X` or MCP `namespace_split`.
+X is the complete frozen request with `workspace_root` rebound to the owned
+fixture; MCP takes that same request data. A changed mapping/policy/source shape needs new admission.
+
+Proof over the current snapshot requires ALL of:
+`state=committed`, `verification_complete=true`, `proof_pending=[]`,
+every required profile check present with `:exit 0`;
+`papercuts` must pass when the profile carries that oracle.
+A warm `committed-probe-only` receipt is unfinished proof. Run outstanding
+user-required checks/review; a commit alone proves no behavior.
+
+Escape: plan-only first when the mapping is uncertain (`plan_only=true` in X).
+A safely correctable typed refusal: repair once from `next_call`, then native.
+Read mutation/commit status FIRST; stale/conflicting evidence requires refresh.
+Never re-run a committed split; finish pending proof or follow guarded recovery.
+Kill switch: a correctness failure or complete verified
+wall loss vs the registered controls suspends this route.
+
+Non-claims: dynamic references, open-ended decomposition, other source grammars
+(CLJC/CLJS/reader conditionals or unsupported macros), callers outside roots,
+partial source retention, and generic fully mapped partition routing.
+September 7–8 receipts and exact manifest:
+`docs/observations/2026-09-07-plan-2-cellC-result.md`,
+`docs/observations/2026-09-08-namespace-split-papercuts-round3.md`,
+`/var/tmp/forge/round/r5-report.md`, and the September 8 row-1 ledger at
+`/home/forge/src/clj-surgeon-records/docs/observations/2026-09-08-row1-split-ledger.md`.
+Historical cross-wave ratios are not fresh matched estimates; B02 owns observer
+qualification. The p9–p12 September 8 regrades in `/var/tmp/forge/coldstart/ledger.txt`
+are workflow PASS evidence, not split controls or friction-low certification.
+
 ### Optional supporting read
 
 `inspect_clojure` is a SUPPORTING read, not an automatic route: use it when
@@ -105,67 +148,22 @@ a recorded loss. Required per routed class: first-attempt success, refusal rate,
 fallback rate, and complete request-to-verified wall; collector coverage of those is
 itself unproven, so unknowns are retained as unknown.
 
-**Other capabilities** — not automatically routed. The only exceptions to the
-performance default are an explicit user request and a separately approved
-experiment:
-
-- `:extract!` — move forms to a new namespace.
-- `:rename-ns!` — structural namespace rename.
-- `:fix-declares!` — eliminate removable `declare`s.
-- MCP `require_change` — add or change a require across many namespaces
-  (measured: nine namespaces, zero churn).
-- MCP `within` + `from`/`to` — a surgical edit inside one known form
-  (measured: zero churn).
-- `:ls-deps` / `:topo` — dependency structure before a large refactor.
+**Other capabilities** (`:extract!`, `:rename-ns!`, `:fix-declares!`,
+MCP `require_change`, surgical `within` + `from`/`to`, `:ls-deps`, `:topo`)
+remain available by explicit request or separately approved experiment.
 
 **Do not use (measured losers):** per-form writes -- N separate calls -- for a
 fan-out change (one native patch does 21 owners in one cell; the batched
-single call is the Fan-out route below); `apply_clojure_changes` with
+single call remains a capability example above); `apply_clojure_changes` with
 `owner {:kind "namespace"}` or forms-scoped `find`+`replace` for insertion (it
 re-prints the whole owner — hundreds of untouched lines); the CLI wrapper as a
 substitute for MCP (a second layer, refuses 2.2x).
 
-## Fan-out route (experimental default, 2026-09-06)
-
-Experimental, development-only; it changes other seats' prompts, so announce
-before `make install-agent-routing`. For a Clojure edit changing the same call
-or symbol inside MANY named top-level forms across files -- a batched,
-known-intent fan-out -- this is the default route:
-
-1. Discover owners FIRST using native reads or one inspect_clojure match batch
-   when structural information is still needed. If native discovery already
-   supplies the complete owners and counts, skip inspect. Group requests by
-   bounded file sets, keeping every site and count.
-   Truncated output is never complete discovery: size follows source and path
-   lengths and the public 32 KB enforcement is defective (inb-b60d6e). "~100
-   owners" is a heuristic; splitting discovery never implies per-owner writes.
-2. THEN patch helper and `require`/alias natively with `apply_patch`. A helper
-   spelled like the target matches itself: exclude ONLY the new helper owner,
-   never a whole file, which holds legitimate original sites. If preparation
-   changes the discovery snapshot, get fresh guards and counts where required
-   -- old observations are not current write authority.
-3. ONE `apply_clojure_changes` call, edits
-   `[{file, within {form}, from, to, matches}]`, using the alias each file
-   binds. Counts convert to edits only for the same concrete from/to inside
-   each NAMED owner: wildcard totals need not equal literal replacement counts,
-   `inside` null is not a `within.form`, an omitted `source` means the result's
-   `match` only under the documented exact-equality rule.
-4. Clear argument error: repair once from the refusal. Route unavailable,
-   unsupported, or refusing again: one native patch, record the reason --
-   native fallback counts as zero tool-committed sites. Conflict or
-   stale-source refusal: refresh the relevant evidence first.
-
-**Evidence and boundary.** Cohort I measured the INFORMED BATCHED EDIT route
-(fresh actors discovering owners themselves) at 1.75x proof-inclusive median,
-101.2 s vs 57.8 s; frozen-witness outcomes tool 4/4, native 3/4 with a known
-layout false negative -- no quality-superiority claim. Served discovery in cohort J was wall-neutral. `owner_counts` is a later
-usability change with no measured additional wall gain; its 0/4 was that spelling-sensitive witness failing the
-self-match workaround, not four self-recursion defects. This witnessed class
-ONLY: not a general Clojure editing default; whole-feature work stays native.
-
-*Derived from doctrine commit 7a682b9e on clj-surgeon MCP/main, whose receipts
-are `docs/observations/2026-09-06-two-hour-trial-closeout.md`,
-`2026-09-06-fanout-I-result.md` and `2026-09-06-fanout-J-ethnography.md`.*
+*Derived from doctrine commit 3c2eb6a9 on clj-surgeon astra/namespace-split (2026-09-08).*
+Intent and evidence: `docs/intent/agent-routing/agent-routing-design.md`.
+Branch check: `make check-agent-routing PLATE_ONLY=1` (no installed agreement claim).
+Operator: announce the suspension/admission and affected Codex/Claude seats before
+`make install-agent-routing`; then `make check-agent-routing` must prove installed parity.
 
 **Every Surgeon MCP operation relays the same terminal-response contract.**
 If `terminal_response` is present and this mutation completes all remaining
@@ -182,7 +180,4 @@ bypasses that serialization and is the cause of contention failures.
 **Direct cclsp and clojure-lsp MCP clients are retired.** Do not discover,
 register, start, or call them from an agent session.
 
-*Reversible: re-open the default route when clj-surgeon-q5z (batch intent across
-N owners) and clj-surgeon-az8 (unrecoverable refusal classes) land and the acid
-apparatus shows rung-L non-test actions at or below native's.*
 <!-- END CLJ-SURGEON ROUTING v:1 -->
```
