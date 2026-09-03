# Resume here — the Anvil seat (forge@anvil), written 2026-09-03T01:20Z

**Read this first after any compaction, together with
`docs/observations/2026-09-02-resume-here-bridge-program.md` (the program state).**

## Host facts (changed under the seat on 2026-09-03 ~01:15Z; the mayor moved the transcript + 60 memory files)

- Host **Anvil** (100.66.152.23), 16 cores; user **forge**, home `/home/forge`; **no sudo**, by design.
- Project keys rewritten `-home-genek-forge-…` → `-home-forge-…`; cwd `~/src/marvin-voice-remote/channel-connector`.
- Identity exported in the shell: `forge-anvil <forge-anvil@anvil>`; every commit carries
  `Co-Authored-By: Gene Kim <genek@itrevolution.com>` (plus the session trailers).
- claude 2.1.259 (`~/.local/bin`), account marvin.openclaw@itrevolution.net — the mayor's pool, so long
  batteries are not free. Remote control ACTIVE (Gene can reach this seat from his phone).
- GitHub: `~/secrets/gh-token` (600 in a 700 dir), `gh` as marvin-openclaw777. Never an env var.
- Repos: `~/src/clj-surgeon` (main), `~/src/marvin-voice-remote`. Worktrees from Buster do NOT exist here;
  every branch is on origin (list below). Recreate with `git worktree add` when needed.
- Present: bd (/usr/local/bin), clj-kondo (/usr/local/bin — the bare binary, not the `~/bin` wrapper),
  clojure, java, codex, claude.

## Not here yet (the mayor provisions in the morning; say if urgent)

- The marvin-channel connector: **the phone channel terminates on Buster; the reply tool does not exist
  here.** Reach Gene/the mayor durably: captain's log, maven inbox (when creds arrive), commit messages,
  and tell the mayor.
- maven reader/writer creds (`maven-r`/`maven-w`), clj-nrepl-eval (+ XDG_RUNTIME_DIR), the `~/bin` scripts
  (bridge-reply, check-prompt-plate, mvr-logs), `/opt/claude-skills` (so `~/.claude/CLAUDE.md`'s doctrine
  @-import does not resolve — house rules live in memory + the repo CLAUDE.md until then), the two crons.

## Hard boundaries on this box

- NEVER contact port 7888 (another seat's production Surgeon, user `surgeon`), nor 7894/7895 (cohort
  servers). Start my own Surgeon on a free port: `clojure -X:clj-surgeon/mcp :port <N>` from a worktree
  (`make mcp-serve` ignores MCP_PORT until inb-d8a635 lands).
- NEVER touch `~/acid/GO-*`, `~/acid/.cohort-lock`, `chain-*.sh`, or any curtain-call fleet directory
  (tester, dev-a/b/c, foreman, merger, kentbeck). A stray write wrecks a running battery.
- Nothing merges from here. Push branches; the mayor merges after independent verification.
- Suites: this box has 16 cores; still one full suite per repo at a time under
  `flock ~/tmp/suite.lock`; never while a GO file or the cohort lock exists.

## Program state at the move (details in the bridge resume note)

- Pushed, awaiting merge — clj-surgeon (mayor's queue): rf2 5e6cdd2, census 7244141, study ops b3c17bb
  (+ possibly the S1/S3 builder's commits if they were pushed before the move — check origin), q5z
  2753f23, admit-gate 17125fe, close-losers 205e13a, format-form-scope 62981ee.
  curtain-call (Gene, in order): fold f115cc2d → store 96387535 → settings-lens aebb7b9a; fold-diff
  f2d8f6eb for the mayor's production check (inb-3a9818); specimen settings-lens-native 74a3d7e6.
- A builder for inb-a0f37e (names-only `ls-tree`) + inb-d8a635 (MCP_PORT) was running on Buster at the
  move; its worktree `~/src/clj-surgeon-study` stayed on Buster. Treat its result as UNKNOWN until
  origin shows commits after b3c17bb on `bridge/study-ops-mcp`; if none, re-run it here.
- Prosecution list (12 inbox items with owners/triggers) in the bridge resume note delta of 00:xxZ.
- Waiting on: the mayor's `pg_get_indexdef` paste + fold-diff output; Gene's decisions (report §9).

## Routing

Answer Gene in the surface he wrote from (terminal here; remote control counts as terminal). No reply
tool on this seat. Memory `reply-seat-bridge-until-requested` still applies when the connector arrives.

## RESUME DELTA 2026-09-03T01:42Z

- Cleared by Gene ("Super!!! Let's go!!"). Builder a7b68e5d3a6c673f5 running on `~/src/clj-surgeon-study`
  (bridge/study-ops-mcp @ b3c17bb): inb-a0f37e + inb-d8a635. After its report: my own suites under
  `flock ~/tmp/suite.lock`, push, log; tell the mayor via the log/inbox (no reply tool here).
- Today's log: `docs/observations/2026-09-03-captains-log-anvil-seat.md`.
- `~/acid` is not readable by forge; ports 7888–7895 are all held by other seats — pick ≥ 7900.

## RESUME DELTA 02:01Z

- Four lanes running (Gene: crank up parallelism): a7b68e5d3a6c673f5 study S1/S3; a02f87b29b2d27eff
  receipt ratchets (`~/src/clj-surgeon-ratchets`); a62066cb655ae61ab lens follow-ups
  (`~/src/curtaincall-cfp-lens2`, curtain-call cloned at `~/src/curtaincall-cfp`); ae828674aa8328b03
  Opus red-team of study ops (read-only). Each lands → my own suites under the lock → push → log.
- **Morning items for the mayor, add:** codex auth on this seat (401 now; Sol unavailable), maven creds,
  connector, clj-nrepl-eval, /opt/claude-skills, crons.

## RESUME DELTA 02:02Z

- Lane 5: ac6a55658002e3ef2 template-upsert (`~/src/curtaincall-cfp-tmpl`). Gated and NOT started: S2 (q5z merge), E6 (S1), C2 (Gene's product call), A1 (tester account), inbox notes (maven creds).

## RESUME DELTA 2026-09-03T02:20Z

- **Study-ops branch is NO-GO** (docs/observations/2026-09-03-study-ops-redteam-NO-GO.md): 12 items,
  1–4 blocking (rg flag injection via `grep`; read-eval on deps.edn; symlink escape; unbounded pre-parse).
  Security round runs on `~/src/clj-surgeon-study` AFTER the S1/S3 builder (a7b68e5d3a6c673f5) reports
  and stops; then an independent re-review; only then back in the queue. Item 2 is pre-existing on main.
- Running: ratchets a02f87b29b2d27eff (`~/src/clj-surgeon-ratchets`); lens follow-ups a62066cb655ae61ab
  (`~/src/curtaincall-cfp-lens2`); red-teams a45426b8de19c383b (census 7244141) and af2fbde760adead45
  (rf2 5e6cdd2 + q5z 2753f23) — their verdicts decide whether those branches stay in the queue.
- Lane 5 DONE: `bridge/template-upsert` bd483802+619d3192 (`~/src/curtaincall-cfp-tmpl`); my unit run
  waiter b7sl1zgqh (`~/tmp/tmpl-unit.log`); push when green; a merge note for Gene (nil-id collide→append).
- Doctrine: CLAUDE.md "Fence review scope" (ab40192); memory security-boundary-review-before-merge widened.


## RESUME DELTA 02:24Z

- S1/S3 DONE on `bridge/study-ops-mcp` (8557161, 8a52931; NO-GO item 1 closed). **Security round
  aab6a773af84935ee running in `~/src/clj-surgeon-study`** (items 2–12). After it: my suites (test-fast +
  `clojure -M:clj-surgeon/mcp-test`; no swipl here) → push → an independent re-review (Opus) → only then
  the branch re-enters the queue.
- Lens follow-ups DONE (`~/src/curtaincall-cfp-lens2`, 6da4150f + 7359b8f9); my unit run waiter
  brnogxq9w (`~/tmp/lens2-unit.log`); push when green.
- Still running: ratchets a02f87b29b2d27eff; red-teams a45426b8de19c383b (census), af2fbde760adead45
  (rf2+q5z).

## RESUME DELTA 2026-09-03T02:29Z

- Census GO-WITH-FIX filed (docs/observations/2026-09-03-census-redteam-GO-WITH-FIX.md); fix round a4fe4601c68f5db01 running in `~/src/clj-surgeon-census` (items 1–11). After: my suites → push → re-review → queue. Lens follow-ups pushed 7359b8f9. Still running: ratchets a02f87b29b2d27eff, study security round aab6a773af84935ee, rf2+q5z red-team af2fbde760adead45.

## RESUME DELTA 2026-09-03T02:31Z — six lanes

- rf2 fix round abd835410547b9851 (`~/src/clj-surgeon-rf2`, NO-GO items 1–8); q5z fix round aaa35fb0f121fdb9a (`~/src/clj-surgeon-q5z`, items 1–7); census fix round a4fe4601c68f5db01 (`~/src/clj-surgeon-census`); ratchets a02f87b29b2d27eff (`~/src/clj-surgeon-ratchets`); study security round aab6a773af84935ee (`~/src/clj-surgeon-study`); fold-diff + lens red-team aceddb474ac916ded (read-only).
- Each fix round → my suites under the lock → push → an independent Opus re-review → only then back in the mayor's queue. Verdict docs: study-ops NO-GO, census GO-WITH-FIX, rf2-q5z-redteam.
- Standing fact for the mayor: of the three "ready" clj-surgeon branches red-teamed tonight, none was mergeable as it stood (study NO-GO, rf2 NO-GO, census/q5z GO-WITH-FIX). The queue is frozen until the rounds land.

## RESUME DELTA 2026-09-03T02:38Z

- Ratchets landed (0434aae, 29090e9, ece8c1c); my suites running (`~/tmp/ratchets-*.log`, waiter b61xhgakh); push when only the baseline six fail.
- **New prosecution item (no maven creds here; file it when they arrive), S9:** `inspect_clojure` `forms` should accept the `{kind defmethod name dispatch}` owner map — today an arm can be WRITTEN by an address it cannot be READ by; a contract change to an existing verb → needs its own witness set and a red-team; trigger: after the ratchets branch merges.

## RESUME DELTA 2026-09-03T02:52Z — nine lanes

- curtain-call fix rounds launched from docs/observations/2026-09-03-folddiff-lens-redteam.md: fold-diff aa14abf5d75ba48e8 (`~/src/curtaincall-cfp-folddiff`, 7 fixes incl. both sides as REFS); template-upsert abd7fc65de9301f76 (`~/src/curtaincall-cfp-tmpl`, raw-id identity); lens-followups ab73fea07362149df (`~/src/curtaincall-cfp-lens2`, slug cross-check). settings-lens aebb7b9a is GO as measured.
- clj-surgeon fix rounds still running: census a4fe4601c68f5db01, study aab6a773af84935ee, rf2 abd835410547b9851, q5z aaa35fb0f121fdb9a. Ratchets suites: waiter b61xhgakh.
- Merge order for Gene (curtain-call): fold → store → settings-lens → template-upsert (after fix) → lens-followups (after fix); goldens re-run at each step. The fold-diff tool runs against production only after its fix round.

## RESUME DELTA 2026-09-03T03:08Z

- rf2 fix round DONE (5ccb4f0, 8 commits on 5e6cdd2); my suites waiter bwxaqew6c (`~/tmp/rf2-*.log`);
  push when only the baseline six fail; then an Opus re-review before the queue.
- Ratchets: review GO-WITH-FIX filed; fix round a90f9ebd74d4100af running on `~/src/clj-surgeon-ratchets`.
- Running: census a4fe4601c68f5db01, study aab6a773af84935ee, q5z aaa35fb0f121fdb9a, fold-diff
  aa14abf5d75ba48e8, template-upsert abd7fc65de9301f76 (waiting on its suite), lens-followups ab73fea07362149df.
- Rule for every builder prompt from now on: NEVER `git add -A`; `.cpcache/` is ignored on main (ef5a538)
  but branches cut earlier lack the ignore line.

## RESUME DELTA 2026-09-03T03:19Z

- lens-followups pushed 934716dc (my run 1055/13171/0). curtain-call merge order stands: fold → store → settings-lens → template-upsert (fix round abd7fc65de9301f76 still running) → lens-followups. rf2 pushed 5ccb4f0, re-review a9f6694ffe16d0d1e running. Fix rounds running: census, study, q5z, fold-diff, ratchets.

## RESUME DELTA 2026-09-03T03:28Z

- Environment: swipl 10 + plunit at `~/.local/bin/swipl` (micromamba env `~/opt/mamba/envs/swipl`); `~/opt/claude-skills`; `~/.claude/CLAUDE.md` (seat header + imports); `~/src/the-gene-maven`; connector deps installed; seat-local `.mcp.json` → 7906 (uncommitted). Wishlist for the mayor: docs/observations/2026-09-03-anvil-seat-wishlist-for-mayor.md; Gene drives it; his restart command is in that file.
- q5z round landed (cc4e38c…0f01440); ruling: drop the pre-read expect.files bound (anchor idiom); builder aaa35fb0f121fdb9a resumed for one commit; then my suites → push → re-review.
- rf2 round three a59ea3ce2a6b2a7fc running (re-review items 1–7). Still running: census, study, fold-diff, template-upsert, ratchets fix rounds. Oracle proof on main running (`~/tmp/oracle-main.log`) → PASS; the full `make mcp-test` runs here now.

## RESUME DELTA 2026-09-03T03:38Z — provisioning mostly done

- Missing now: ONLY codex auth (Gene, interactive) → Sol. Everything else landed: maven (`maven-r`/`maven-w`
  work), `~/bin`, XDG in .bashrc (new shells), check-prompt-plate cron, swipl, doctrine, port table.
- Ports: 7906 mine (up, telemetry); 7907–7910 mine for branch servers/hand-drives; never anything else.
  7888 = fleet's production Surgeon at e7f72e2 with dev-a/b/c live on it.
- No phone channel on this seat by ruling (seat=bridge lives on Buster). Gene: terminal. Mayor: log + inbox.
- Session restart (binds MCP tools to 7906): `cd ~/src/marvin-voice-remote/channel-connector && claude --resume
  b623492c-458d-4156-a14d-a041f5a37e7c` — do it at a safe point (no live builders).
- Inbox current as of 03:38Z; S9 = inb-0817fe.

## RESUME DELTA 2026-09-03T03:43Z

- q5z pushed 23ea871 (full mcp-test incl. oracle PASS); re-review acb7e66e0b6224894 running. Study security round complete at 212b045 (unpushed); my suites waiter buh7b1v0k (`~/tmp/study2-*.log`); push when only the baseline six fail and the oracle passes; then an Opus re-review (flag: host-absolute dir in refusals; grep continuation carries the rejected pattern; 65,536-char study limit; find-config-file walk).
- Running: rf2 round three a59ea3ce2a6b2a7fc, census a4fe4601c68f5db01, fold-diff aa14abf5d75ba48e8, template-upsert abd7fc65de9301f76, ratchets a90f9ebd74d4100af.

## RESUME DELTA 2026-09-03T03:43Z

- Study pushed 212b045; re-review a4bf90d6d86f75204 running — the NO-GO doc stands until it says GO. q5z re-review acb7e66e0b6224894 running. Fix rounds: rf2 round three, census, fold-diff, template-upsert, ratchets.

## RESUME DELTA 2026-09-03T03:59Z — eight lanes

- Re-reviews: study a4bf90d6d86f75204, fold-diff a94e2e37c80843f35 (production gate). Fix rounds: rf2 round three a59ea3ce2a6b2a7fc, census a4fe4601c68f5db01, template-upsert abd7fc65de9301f76, ratchets a90f9ebd74d4100af, q5z round three a4f713cb01afc03cb (`~/src/clj-surgeon-q5z`), kondo-path test af5a3aed64de01ebb (`~/src/clj-surgeon-kondo`, branch bridge/kondo-path-test from main).
- Verdict docs on main now: study-ops NO-GO, census GO-WITH-FIX, rf2-q5z, rf2-rereview, q5z-rereview, folddiff-lens, ratchets. Queue frozen until GO verdicts.

## RESUME DELTA 2026-09-03T04:02Z

- Study: re-review GO-WITH-FIX (docs/observations/2026-09-03-study-ops-rereview.md; NO-GO doc marked superseded); round three acffa7722710273de running on `~/src/clj-surgeon-study` (items 1–11; blockers 1–4). After: my suites (full mcp-test) → push → a short re-check → queue.
- Still running: fold-diff production re-review a94e2e37c80843f35; rf2 round three; census; template-upsert; ratchets; q5z round three; kondo-path test. Eight lanes.

## RESUME DELTA 2026-09-03T04:05Z — Sol live; provisioning closed

- Sol WORKS here (genekkanban pool): `codex exec -m gpt-5.6-sol -c model_reasoning_effort="high" --skip-git-repo-check --sandbox read-only -o out.md "…" </dev/null`. Reviews route to Sol first; Opus for filter-refused (symlink/confinement fixtures) and as the second voice.
- Nothing missing now except by ruling: no phone channel; repo-watchstander stays on Buster; GCP deferred.
- Running: Sol memory design answer (`~/tmp/sol/memory-sol-answer.md`, waiter bm3owhhzt); Opus memory consult a2d936e48f3423a0c; fold-diff production re-review; rf2/q5z/study round threes; census; template-upsert; ratchets; kondo-path test.

## RESUME DELTA 2026-09-03T04:09Z

- Gene's rule: memory fixes (and all fix rounds) add LID intents via `~/opt/claude-skills/linked-intent-testing`
  (registry id, EARS, boundaries, fail-first linked witnesses, contract test) — memory prefix `MCP-OP-MEM-`.
  Builder-prompt clause: "New requirements enter as linked intents per the linked-intent-testing skill
  (read it first): registry entry with a stable id + EARS + boundaries, witnesses that fail first and are
  linked by id; a ceiling's witness asserts behaviour AT the ceiling, never the constant."
- Memory plan: reconcile Sol (`~/tmp/sol/memory-sol-answer.md`) and Opus (a2d936e48f3423a0c) answers →
  ordered builds, smallest measurable win first, each with its MEM intent and a heap receipt.

## RESUME DELTA 2026-09-03T04:17Z

- Memory build brief clauses (Gene): (1) LID intents MCP-OP-MEM- for every new requirement; (2) the no-OOM proof is `make memory-battery` (100/1k/10k files, -Xmx512m, numeric pass lines), NOT in the fast suites; fast suites keep only ms-scale admission/ceiling/receipt witnesses; battery = merge gate for MEM changes, linked by intent id. Decision card sent (A keep in-memory snapshot / B disk-pinned pre-image / C tiers; recommend B); awaiting Gene's letter; tier-1 (read verbs) is authorised and starts when study round three + census land.

## RESUME DELTA 2026-09-03T04:19Z — Gene chose B; memory program is GO

- B1 kernel build on `~/src/clj-surgeon-txn` (`bridge/txn-journal`) launching now; B2 adoption after q5z r3 / rf2 r3 land. Battery build a0e2b73e06754ebf3 on `~/src/clj-surgeon-membat`.
- rf2 round three landed (70836c0…5839b52) with one ruling sent back (no writes into skipped trees via links → typed refusal); my suites + push after that commit.
- Sol YOLO memory answer waiter b7qt28zev; Opus consult a2d936e48f3423a0c; fold-diff production re-review a94e2e37c80843f35; census, template-upsert, ratchets, q5z r3, study r3, kondo lanes running.

## RESUME DELTA 2026-09-03T04:21Z — ten lanes; memory program B1 running

- Memory program: B1 kernel a2b6bbcbdc96b2925 (`~/src/clj-surgeon-txn`, TDD: OOM repro first, `make memory-red`); battery a0e2b73e06754ebf3 (`~/src/clj-surgeon-membat`, `make memory-battery`, MEM-001, red baseline on main); Sol YOLO answer waiter b7qt28zev (`~/tmp/sol/memory-sol-answer-2.md`); Opus consult a2d936e48f3423a0c. Reconcile Sol+Opus → fold numbers into B2 (adoption in alias_migration + extract after their rounds).
- Review/fix lanes: fold-diff production re-review a94e2e37c80843f35 (gates the mayor's run; inb-3a9818 says DO NOT RUN YET); rf2 r3 a59ea3ce2a6b2a7fc (ruling: no writes into skipped trees via links) → my suites → push → re-check; census a4fe4601c68f5db01; template-upsert abd7fc65de9301f76; ratchets a90f9ebd74d4100af; q5z r3 a4f713cb01afc03cb; study r3 acffa7722710273de; kondo-path af5a3aed64de01ebb.
- Load ~8/16, 18 GB free, forge JVM RSS ~9.6 GB. Sol-yolo PR: realgenekim/claude-skills#1.

## RESUME DELTA 2026-09-03T04:26Z

- Memory: Sol's measured design filed (2026-09-03-memory-design-sol-answer-2.md: 45 heap bytes per source byte = the node tree; lifetime + concurrency are the win; state root is `~/.local/state/clj-surgeon/workspaces/<digest>/`; MEM-001..011 plan with at-the-ceiling witnesses). B1 (a2b6bbcbdc96b2925) and the battery (a0e2b73e06754ebf3) re-briefed with the state root and id numbering (battery: 001 + 011; kernel: 006, 007, 012–014). Opus consult a2d936e48f3423a0c still measuring; reconcile → B2.
- Census fix round complete ef545c5; my suites waiter b75uqudhe (`~/tmp/census/my-*.log`); push → Sol re-review (Opus for the symlink item if the filter refuses).
- Also pending my suites: ratchets 49f6e12 (b1q8ixsp0, `~/tmp/ratchets2-*.log`), kondo f8a9ef9 (bvjc78bi1, `~/tmp/kondo-mcp-test.log`).
- Running: fold-diff production re-review; rf2 r3 ruling commit; template-upsert; q5z r3; study r3; routing-doc.
- Builder logs now go under `~/tmp/<lane>/` (a sibling lane overwrote a shared log).

## RESUME DELTA 2026-09-03T04:34Z — NIGHT ORDERS IN FORCE
Gene is asleep for ~9 h from 2026-09-03T04:34Z. Read `2026-09-03-night-orders-anvil.md` (goals + mayor help) and
`2026-09-03-merge-queue-for-mayor.md`; keep `/tmp/anvil-pulse.txt` fresh (cron heartbeat every 10 min:
read new `/tmp/mayor-*.txt`, honour `/tmp/anvil-halt.txt`). Live lanes at this write: ratchets Sol
re-review (~/tmp/sol/ratchets-sol-review.md), census suites (~/tmp/census/my-*.log), rf2 r3, q5z r3,
study r3, template-upsert fix, fold-diff r3, battery, B1 kernel, routing-doc fix. Opus memory consult
LANDED (`2026-09-03-memory-design-opus-answer.md`): the node tree is 48× source, zippers add 1.2% — the
zipper premise was wrong; outline double-parses (76 MB garbage per 52 KB file); aggregate-bytes ceiling
is the missing control that explains the alias_migration OOM. Next: reconcile with Sol-2, launch B2
read-path lane.

## RESUME DELTA 2026-09-03T05:19Z — night orders, mid-night state
MERGED to main by surgeon1: kondo f8a9ef9 (acda1b3), routing-doc a9d8701 (3ebeafd/d1c5330); main fast-suite baseline is ZERO (702/5912/0, ~/tmp/main/test-fast-981a9f1.log). Queue doc: `2026-09-03-merge-queue-for-mayor.md`.
Live lanes and where their receipts land (Sol reviews: ~/tmp/sol/<lane>-sol-review.md, log ends with EXIT):
- ratchets fe7a1a1 pushed → Sol round-3 re-check (ratchets3).
- rf2 465c956 pushed → Opus found the MCP entrance bypasses the walk (wrote into .git/hooks) → round 4 building on ~/src/clj-surgeon-rf2.
- q5z ca677bc pushed → Sol NO-GO (undo receipt pruned; aggregate next_call IS possible) → round 4 building on ~/src/clj-surgeon-q5z.
- census ef545c5 pushed → Sol NO-GO (silent truncation at 4,000) → round 3 building on ~/src/clj-surgeon-census.
- study round 3 building on ~/src/clj-surgeon-study; template-upsert fix on ~/src/curtaincall-cfp-tmpl; fold-diff round 3 on ~/src/curtaincall-cfp-folddiff (waiting on its suite).
- memory battery 2bae68b pushed → Sol GO-WITH-FIX as tooling → round 2 building on ~/src/clj-surgeon-membat (INCOMPLETE≠PASS, verified corpus, attested reference, held_mb line: max(held 10k) ≤ max(held 1k) + 2.0 MiB).
- B1 kernel on ~/src/clj-surgeon-txn (TDD from the OOM; owns the reserved-peak accountant); B2 MEM-015 single-parse on ~/src/clj-surgeon-readpath (gate = battery lines on ls-tree).
- arms apparatus 598139c pushed (bridge/anvil-arms-apparatus) → Sol instrument review (arms). E3/E6 spec: `2026-09-04-e3-e6-prestaged.md`. Owed before any cohort: PF-1..4, PF-6; PF-4 = G1 hand-drive of alias_migration/ls-tree (needs q5z on main and my MCP tools bound to 7906 — session restart required, NOT tonight while lanes run).
Crons in this session: heartbeat */10 (pulse /tmp/anvil-pulse.txt, reads /tmp/mayor-*.txt, honours /tmp/anvil-halt.txt); usage watch hourly at :23. Inbox: inb-1165ce (night orders to the mayor), inb-2f78f5 (collateral handler-identity assertion), inb-3a9818 (fold-diff HOLD).
Docs added tonight: vision.md "How we work" + "When the work is delegated" (+ the-how techniques doc), magic-moments chronicle, memory-design reconciled, night orders, answer to the mayor.
Next on wake: read the newest Sol/Opus verdicts, file, launch rounds; when lanes go quiet, write the Gene report (tables first) and refresh this note.

## RESUME DELTA 2026-09-03T06:26Z — night orders, second refresh
GO so far: kondo (MERGED acda1b3), routing-doc (MERGED 3ebeafd/d1c5330), receipt-ratchets c5ef7ca (GO, on inb-1165ce as GO #3). Main baseline is zero (702/5912/0).
Rule changes tonight: unit suites run under `~/bin/suite-run <cmd>` (three lanes); only the memory battery / memory-red take the exclusive suite.lock. `~/bin/sol-yolo` disables every repo-declared MCP server unless a url is passed (curtaincall-cfp's .codex/config.toml declares 7888 required — incident logged 06:05Z, mayor told on inb-1165ce).
Live (receipts under ~/tmp/sol/<lane>-sol-review.md, EXIT line in the log): Sol: census round 3 (f43ac03), arms apparatus round 2 (6c1cf0a), MEM-015 read-path (61cb9b5), battery round 2 (c6a2264). Opus: rf2 round 4 (965d49e). Builders: study round 4 (ReDoS step budget; ~/src/clj-surgeon-study), template-upsert round 2 (blank-id contract; ~/src/curtaincall-cfp-tmpl), q5z round 5 (detail ownership by manifest+marker, collision before mkdirs, marker at the real write; ~/src/clj-surgeon-q5z), fold-diff round 3 (~/src/curtaincall-cfp-folddiff, waiting on its suite), B1 kernel (~/src/clj-surgeon-txn), B3 MEM-005 parser admission (~/src/clj-surgeon-admit, based on the MEM-015 tip).
Inbox: inb-1165ce (night orders + GO notes + the 7888 incident), inb-2f78f5 (candidate-catalog handler identity), inb-46f90f (usage-watch friction: telemetry needs port/session tags), inb-c19ce6 (curtain-call raw-id audit), inb-3a9818 (fold-diff HOLD).
Next on wake: newest verdicts → file → GO note or next round; at ~08:30Z start the Gene report (tables first) from the captain's log; refresh this note.

## RESUME DELTA 2026-09-03T06:56Z — night orders, third refresh
GO so far (five): kondo (MERGED acda1b3), routing-doc (MERGED), receipt-ratchets c5ef7ca, template-upsert 25b98a83 (order-gated on Gene), rf2-extract-rewire 965d49e. All on inb-1165ce with verify commands.
Live Sol reviews (receipts ~/tmp/sol/<lane>-sol-review.md): txn-journal kernel (B1, 1cece9a), q5z round 5 (50098e6), fold-diff round 3 (2b56a484, curtain-call).
Live builders: study round 4 (ReDoS step budget; ~/src/clj-surgeon-study), census round 4 (one discovery kernel for MCP + both CLIs; ~/src/clj-surgeon-census), B3 MEM-005 parser admission (~/src/clj-surgeon-admit), arms apparatus round 3 (~/src/clj-surgeon-arms), MEM-015 round 2 (~/src/clj-surgeon-readpath), battery round 3 (~/src/clj-surgeon-membat).
Rules in force: unit suites via ~/bin/suite-run (three lanes); battery/memory-red keep the exclusive suite.lock; sol-yolo disables repo-declared MCP servers (7888 incident 06:05Z, reported).
Inbox: inb-1165ce (night orders, GO notes, incident), inb-2f78f5, inb-46f90f, inb-c19ce6, inb-5aaad4 (rf2 fence follow-ups), inb-3a9818 (fold-diff HOLD until Sol GO).
Next on wake: file newest verdicts; at ~08:30Z the Gene report (tables first) from the captain's log; refresh this note.

## RESUME DELTA 2026-09-03T07:18Z — night orders, fourth refresh
GO (seven): kondo + routing-doc MERGED; receipt-ratchets c5ef7ca; template-upsert 25b98a83 (order-gated on Gene); rf2 965d49e; read-path-memory 2aa648a (MEM-015); memory-battery 5534e94 (tooling; main RED under it by design). fold-diff 2b56a484: GO for the mayor's exact production read with Sol's conditions (inb-3a9818 HOLD lifted); round 4 building for residuals.
Live reviews: Sol census round 4 (1e5eec7), Sol q5z round 6 (9d72bcf); Opus study round 4 (ec5a592).
Live builders: B3 MEM-005 (~/src/clj-surgeon-admit, RED committed, GREEN in progress), arms apparatus round 3 (~/src/clj-surgeon-arms), B1 kernel round 2 (~/src/clj-surgeon-txn: recheck→rename race, membership digest, , identity pinning, pre-image lifetime, battery accountant), fold-diff round 4 (~/src/curtaincall-cfp-folddiff).
Gene report DRAFT committed: docs/observations/2026-09-03-gene-report-night.md (refresh at the end; tables from the log).
Rules in force unchanged (suite-run lanes; battery/memory-red exclusive; sol-yolo neutralises repo .codex/config.toml — two 7888 contacts reported on inb-1165ce).
Next on wake: file newest verdicts; when lanes go quiet or at ~08:30Z, refresh the Gene report and this note.

## RESUME DELTA (fifth refresh; see the commit time) — night orders, final third
GO (seven) unchanged: kondo + routing-doc MERGED; receipt-ratchets c5ef7ca; template-upsert 25b98a83 (order-gated on Gene); rf2 965d49e; read-path-memory 2aa648a; memory-battery 5534e94 (tooling). fold-diff: production read GO PINNED at 2b56a484 (inb-3a9818, Sol's conditions); branch tip d5b9e132 needs round 5 (own-copy fallback) before it replaces the pin.
Live: Sol q5z round 7 (f4d196b). Builders: study round 5 (length-scaled budget term; ~/src/clj-surgeon-study), census round 5 (refusal evidence, entry bound, exhaustion; ~/src/clj-surgeon-census), B1 kernel round 2 (~/src/clj-surgeon-txn), MEM-005 round 2 (prefix depth, Error catch, 4th constructor; ~/src/clj-surgeon-admit), arms apparatus round 3 relaunched (~/src/clj-surgeon-arms; previous builder wedged, stopped 07:41Z), fold-diff round 5 (~/src/curtaincall-cfp-folddiff).
Gene report DRAFT at docs/observations/2026-09-03-gene-report-night.md — refresh at ~08:30Z from the captain's log (every number verbatim with its entry time).
Sol content filter refuses: rf2, study, MEM-005 branches → Opus fallback each time (recorded per lane). sol-yolo neutralises repo .codex/config.toml (two 7888 contacts reported on inb-1165ce at 06:05Z and 06:57Z).
Inbox: inb-1165ce (night orders, seven GO notes, incidents), inb-3a9818 (fold-diff production read GO with conditions), inb-2f78f5, inb-46f90f, inb-c19ce6, inb-5aaad4, inb-07c5e7 (MEM-005 follow-ups).

## RESUME DELTA (sixth refresh; commit time is the timestamp) — cadence corrected
Gene's nine hours end ~13:35Z. Gene report DRAFT (docs/observations/2026-09-03-gene-report-night.md) refreshes at ~10:30Z and ~13:00Z, from the captain's log, numbers verbatim with entry times.
Live Sol reviews: census round 5 (869bbce), kernel round 2 (7c9a9b1), fold-diff round 5 (347fe6d3, curtain-call). Opus: q5z round 7 (f4d196b). Builders: study round 5 (~/src/clj-surgeon-study), MEM-005 round 2 (~/src/clj-surgeon-admit), arms apparatus round 3 relaunched (~/src/clj-surgeon-arms), B4 MEM-003 streaming ls-tree (~/src/clj-surgeon-stream, based on the MEM-005 tip 8a55dbc).
fold-diff production read: pinned GO at 2b56a484 on inb-3a9818; 347fe6d3 replaces it only after Sol confirms.
Everything else as in the fifth refresh.

## RESUME DELTA (seventh refresh; commit time is the timestamp)
GO (eight): kondo + routing-doc MERGED; receipt-ratchets c5ef7ca; template-upsert 25b98a83 (order-gated); rf2 965d49e; read-path-memory 2aa648a; memory-battery 5534e94 (tooling); study-ops 4480e3d. fold-diff: production read GO at the tip 347fe6d3 on inb-3a9818 (keep the whole receipt); round 6 building for the verdict caveat + scan robustness.
Live reviews: Opus kernel round 2 (7c9a9b1). Builders: MEM-005 round 2 (~/src/clj-surgeon-admit), arms apparatus round 3 relaunched (~/src/clj-surgeon-arms), B4 MEM-003 streaming ls-tree (~/src/clj-surgeon-stream), q5z round 8 (~/src/clj-surgeon-q5z: constant-true source_unchanged), fold-diff round 6 (~/src/curtaincall-cfp-folddiff), census round 6 (~/src/clj-surgeon-census: streaming listing, candidate-aware narrowing, placeholders + CENSUS-014).
Opus-first branches (Sol's filter refuses): rf2, study, MEM-005, q5z, kernel. Two 7888 contacts + the 7890 self-test observation are on inb-1165ce.
Gene report DRAFT refreshes ~10:30Z and ~13:00Z (nine hours end ~13:35Z).

## RESUME DELTA (eighth refresh; commit time is the timestamp)
GO (eight) unchanged; fold-diff production read at 347fe6d3 (inb-3a9818).
Live reviews: Sol MEM-003 streaming ls-tree (8c1087f); Opus kernel round 2 (7c9a9b1); Opus MEM-005 round 2 (ad439f4). Builders: fold-diff round 6 (~/src/curtaincall-cfp-folddiff), census round 6 (~/src/clj-surgeon-census), q5z round 9 (~/src/clj-surgeon-q5z), arms apparatus round 4 (~/src/clj-surgeon-arms: whitelist Make grammar, child subreaper, schema v2).
Memory program state: MEM-015 GO (2aa648a) · battery GO (5534e94) · MEM-005 round 2 under Opus (ad439f4) · MEM-003 under Sol (8c1087f: ls-tree held 94 → 9.5 MB at 10k) · kernel round 2 under Opus (7c9a9b1) · remaining battery RED rows: workspace-sources-read-all 41.1 MB (MEM-004 streaming fold/spill, unbuilt) and rename-ns-plan-full-match 10.0 MB (the ungated rename planner, inb-07c5e7).
Follow-ups filed tonight: inb-2f78f5, inb-46f90f, inb-c19ce6, inb-5aaad4, inb-07c5e7, inb-ddb845 (7890 self-test), inb-fa5d68 (MCP ls-tree adoption of MEM-003).

## RESUME DELTA (ninth refresh; commit time is the timestamp)
GO (eight) unchanged; fold-diff production read at 347fe6d3.
Live: Sol MEM-003 (8c1087f); Opus q5z round 9 (f51ceae — rule the builder's dispute first: :rolled-back true on an undo = migration still in place). Builders: fold-diff round 6 (~/src/curtaincall-cfp-folddiff), arms apparatus round 4 (~/src/clj-surgeon-arms), kernel round 3 (~/src/clj-surgeon-txn: stale LOCK, lease fail-closed, undo! under lock), MEM-005 round 3 (~/src/clj-surgeon-admit: unmatched-close clamp + malformed corpus, meta counting), census round 7 (~/src/clj-surgeon-census: two continuation gaps).
Follow-ups filed since the eighth refresh: inb-9c5826 (timing/HEAD-stamp test fragility under parallel lanes).

## RESUME DELTA (tenth refresh; commit time is the timestamp)
GO (nine): + q5z-alias-migration f51ceae (Opus round 9 withdrew its own item after reproducing the builder's counter-proof). E3's prerequisite amended in the prestaged doc: q5z merged + alias_migration admits the rung's scope (not "battery green").
Live: Sol apparatus round 4 (895eed0), Sol fold-diff round 5 (885f58b3). Builders: kernel round 3 (~/src/clj-surgeon-txn), MEM-005 round 3 (~/src/clj-surgeon-admit), census round 7 (~/src/clj-surgeon-census), MEM-003 round 2 (~/src/clj-surgeon-stream: pinned manifest + MAC cursor).
Follow-ups since the ninth refresh: inb-c95f37 (alias_migration deletions count / retire-failed).

## RESUME DELTA (eleventh refresh; commit time is the timestamp)
GO (nine) unchanged. Live: Sol apparatus round 4 (895eed0), Sol fold-diff round 5 (885f58b3), Sol census round 7 (5eee690); Opus kernel round 3 (eb22036). Builders: MEM-005 round 3 (~/src/clj-surgeon-admit), MEM-003 round 2 (~/src/clj-surgeon-stream).
Follow-ups since the tenth refresh: inb-2c5b2a (publish-lock retrofit obligation, ~15 writer sites).
Gene report refresh: launch the refresh agent at ~10:20Z (tables from the captain's log, numbers verbatim with entry times), commit by ~10:35Z; final at ~13:00Z.
