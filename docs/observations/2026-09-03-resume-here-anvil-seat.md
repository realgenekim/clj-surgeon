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

## RESUME DELTA (twelfth refresh; commit time is the timestamp) — after the 429
At ~09:55Z the seat's Claude session limit (429, "resets 10am (UTC)") killed four subagents; codex/Sol lanes unaffected. All relaunched at 10:04Z from committed state. Live: Opus MEM-005 round-3 re-check (52c5d85), Opus kernel round-3 re-check (eb22036), arms round 5 (~/src/clj-surgeon-arms at 895eed0), MEM-003 round 2 (~/src/clj-surgeon-stream from its RED 98775cb), census round 8 (~/src/clj-surgeon-census at 5eee690: doors items schema-validated), fold-diff round 7 (~/src/curtaincall-cfp-folddiff at 885f58b3: witness independence, required-var guard, DATA_DIR under Postgres). Gene report refreshed 10:06Z; re-format per skills/gene-report/SKILL.md (in THIS repo — not ~/opt/claude-skills) running; final refresh ~13:00Z with that skill.
GO (nine) unchanged; fold-diff production read GO at 347fe6d3 stands; none of the nine has merged yet (origin/main checked 09:44Z).

## RESUME DELTA (thirteenth refresh; commit time is the timestamp)
Live reviews: Sol census round 8 (dae5d9c), Sol apparatus round 5 (e9a40dc), Opus MEM-005 round 3 (52c5d85), Opus kernel round 3 (eb22036). Builders: MEM-003 round 2 (~/src/clj-surgeon-stream), fold-diff round 7 (~/src/curtaincall-cfp-folddiff).
Gene report: docs/observations/2026-09-03-gene-report-night.md is in the skills/gene-report format (09b5d54); final refresh ~13:00Z with that skill.
GO (nine) unchanged; none merged yet by the mayor.

## RESUME DELTA (fourteenth refresh; commit time is the timestamp)
GO (ten): + parser-admission 52c5d85 (MEM-005; merge after read-path-memory b7ef23d + memory-battery 5534e94). Follow-ups: inb-114faa (estimator-blind tagged-literal / hat-meta towers; scan_ms sum + false zero).
Live: Sol census round 8 (dae5d9c), Sol apparatus round 5 (e9a40dc), Opus kernel round 3 (eb22036); builders MEM-003 round 2 (~/src/clj-surgeon-stream), fold-diff round 7 (~/src/curtaincall-cfp-folddiff).
Next memory leaf after MEM-003 lands: MEM-004 (workspace-sources read-all streaming fold/spill — the last battery RED row besides the ungated rename planner).

## RESUME DELTA (fifteenth refresh; commit time is the timestamp)
GO (ten) unchanged. Live: Sol census round 8 (dae5d9c), Sol apparatus round 5 (e9a40dc), Sol fold-diff round 6 at the tip (66325423; production GO stays at 347fe6d3); builders MEM-003 round 2 (~/src/clj-surgeon-stream), kernel round 4 (~/src/clj-surgeon-txn: in-JVM mutex before the file lock, per-thread re-entrancy, atomic stale-lock break, finish! on every exception path).
Memory added: jvm-file-locks-are-per-process (bash flock does not block FileChannel.lock; second thread → OverlappingFileLockException).

## RESUME DELTA (sixteenth refresh; commit time is the timestamp)
GO (ten) unchanged. Live: Sol fold-diff round 6 at the tip (66325423); builders kernel round 4 (~/src/clj-surgeon-txn), apparatus round 6 (~/src/clj-surgeon-arms), census round 9 (~/src/clj-surgeon-census: validation before routing), MEM-003 round 3 (~/src/clj-surgeon-stream: content-addressed cursor id + snapshot reuse; then Sol reviews the cursor surface).
Follow-ups since the fifteenth refresh: inb-276378 (MEM-003 cursor identity bead).
Memory added: cursor-mac-needs-an-unpublished-secret.

## RESUME DELTA (seventeenth refresh; commit time is the timestamp)
GO (ten) unchanged; production fold-diff read GO at 347fe6d3 stands (tip 66325423 NO-GO on the required-var scan; round 8 building).
No review in flight; five builders: kernel round 4 (~/src/clj-surgeon-txn), apparatus round 6 (~/src/clj-surgeon-arms), census round 9 (~/src/clj-surgeon-census), MEM-003 round 3 (~/src/clj-surgeon-stream), fold-diff round 8 (~/src/curtaincall-cfp-folddiff).
Next on each report: push → Sol (census, apparatus, fold-diff, MEM-003 cursor surface) or Opus (kernel) → file → GO note or next round. Gene report final refresh ~13:00Z with skills/gene-report/SKILL.md.

## RESUME DELTA (eighteenth refresh; commit time is the timestamp)
Live: Sol apparatus round 6 cohort-readiness review (8017789). Builders: kernel round 4, census round 9, MEM-003 round 3, fold-diff round 8 (worktrees as before). GO (ten) unchanged; production fold-diff read GO at 347fe6d3.

## RESUME DELTA 12:05Z (from eb56891)

State of the lanes right now (all builders/reviewers are subagents of the Anvil seat session; if you are reading this after compaction, their results arrive as task notifications — do NOT relaunch a lane that is listed as running until you have checked its worktree HEAD against the sha here):

| lane | branch | tip on origin | status | next |
|---|---|---|---|---|
| census | clj-surgeon bridge/census-verb | 48c64ac | HELD (Sol r10 NO-GO: CLI validator only `threads`; CLI continuation `:dir .` retargets) | Opus r11 builder in ~/src/clj-surgeon-census → push → Sol r11 re-check |
| MEM-003 | clj-surgeon bridge/streaming-ls-tree | 281e13b | HELD (Opus r4 GO-WITH-FIX: two-open race 89/400 SERVED-WRONG; symlinked dir escapes lexical confinement) | Opus r5 builder in ~/src/clj-surgeon-stream → push → Opus r5 re-check (Sol filter refuses this lane) |
| kernel | clj-surgeon bridge/txn-journal | ec93bd1 | HELD (Opus r4 GO-WITH-FIX: restore check-then-act 129/29,012 clobbered; finish-after-throw un-commits) | Opus r5 builder in ~/src/clj-surgeon-txn → push → Opus r5 re-check |
| apparatus | clj-surgeon bridge/anvil-arms-apparatus | 54f3b50 (tip) / **23a7643 GO cohort-ready** | Sol r8 NO-GO on the tip's case-45 meta-ratchet (wrong-file operand false green) | Sonnet r9 builder in ~/src/clj-surgeon-arms → push → Sol r9 confirm |
| fold-diff | curtaincall-cfp bridge/fold-diff-tool | 17fa3183 (tip) / **347fe6d3 production GO** | Sol r8 NO-GO at the tip (scan fails open via bindings; crash exit 1; precedence) | Opus r9 builder in ~/src/curtaincall-cfp-folddiff → push → Sol r9 re-check |

Unchanged GOs for the mayor (see the merge-queue doc): receipt-ratchets c5ef7ca, rf2 965d49e, study 4480e3d, memory-battery 5534e94, read-path-memory b7ef23d, q5z f51ceae, parser-admission 52c5d85, template-upsert 25b98a83 (order-gated on Gene), apparatus 23a7643. No merges observed from the mayor as of this delta; consolidated order stands on inb-1165ce. E3/E6 prerequisite "apparatus GO" satisfied; still waiting on the q5z/read-path/parser-admission merges.

Sol lanes: launched via ~/bin/sol-yolo with a background waiter loop per lane; verdicts land in /home/forge/tmp/sol/<lane>-sol-review.md, EXIT line in <lane>-sol.log. Usage watch unchanged all night (96/49/47). Heartbeat cron every 10 min, usage watch hourly at :23. Load ~4–6 on 16 cores, ~20 GB free.

## RESUME DELTA 19:05Z — after the weekly-limit outage (13:03–19:03Z)

The seat's Claude weekly limit (resets 19:00 UTC) killed three subagents and silenced the main loop for six hours; Sol kept working. Relaunched at 19:03Z from committed state. Gene's nine-hour night window (from ~04:00Z) is over; he has not written since. Lanes now:

| lane | tip on origin | status | running now |
|---|---|---|---|
| census | 772b29f | HELD — Sol r11 NO-GO (post-scan `:dir .`; unescaped `:next-command` = injection syntax; oversized pool_size throws) | Opus r12 builder in ~/src/clj-surgeon-census |
| MEM-003 | 0914a37 (origin) / 3cedd44 (local, r6 four items committed, gates NOT run) | Opus r5 GO-WITH-FIX; r6 = docs + assertion + 2 small fixes | Sonnet gates-only agent on 3cedd44 → push → Opus confirm → GO |
| kernel | 9aa5baa | r6 pushed; Opus r6 re-check was killed after item 1 | fresh Opus r6 re-check in ~/tmp/sol/txn6-wt |
| fold-diff | b223f64e (production pin 347fe6d3) | r9 pushed; Sol refused the brief; Opus review killed after attack (a) | fresh Opus r9 review in ~/tmp/sol/folddiff9-wt |
| apparatus | **77e6237 GO cohort-ready** (Sol r10) | CLOSED | — |

Ten GOs unmerged; no merges observed from the mayor all night. Gene report at 2f05a46 (13:00Z). Usage watch unchanged 96/49/47.

## RESUME DELTA 21:35Z — evening state (after the andon)

**ANDON inb-d27b79 (shell injection in `core/find-build-files` on main)** — pulled 20:26Z, acked by the mayor 20:35Z (/tmp/mayor-2035.txt): release-lane freeze GRANTED (no install/reload/merge touching find-build-files or callers until the fix merges with adversarial review); mayor owns the 7888 blast-radius question; bead clj-surgeon-0me gated. Fix branch **bridge/andon-find-build-files-argv 32c0c7f** (11 commits on 11413f2): Opus adversarial review GO-WITH-FIX at 811f4b0, both required items landed; seat probes clean at the tip; handed to the mayor 21:21Z (session msg e0895374 + inb-d27b79) for their independent probe + merge. **Waiting on the mayor.** Follow-up inb-75aaf7 (find expression-start tokens).

| lane | tip on origin | status | running now |
|---|---|---|---|
| census | d338554 | r14 pushed; 13 executed rounds so far (last Sol NO-GO on U+FFFD+unknown-field continuation, MCP remedies, missing file — all addressed in r14) | Sol r14 re-check (~/tmp/sol/census15-*) |
| MEM-003 | **95b0881 GO** (Opus r8 "merge 95b0881") | CLOSED — eleventh GO | — |
| kernel | **5a2d254 = merge point** (Opus r8: replaces 11c7377) | r9 pre-adoption hardening building | Opus r9 builder in ~/src/clj-surgeon-txn |
| fold-diff | 3d344432 (production pin 347fe6d3) | r11 pushed (self-owned log, receipt/exit binding, rebind, driver-aborted) | Opus r11 review in ~/tmp/sol/folddiff11-wt — asks whether the tip may replace the pin |
| apparatus | **77e6237 GO** | CLOSED | — |

GOs for the mayor now: the ten from the night + MEM-003 95b0881 + apparatus 77e6237 + kernel 5a2d254 (latent). No merges observed from the mayor yet. Weekly Claude limit resets 19:00Z (hit once today, 13:03–19:03Z outage). Usage watch unchanged 96/49/47. Follow-ups filed today: inb-ef6dd6 (MEM-005 absolute path in :error), inb-eca3b1 (System/exit in ls-tree op), inb-1f9a27, inb-00d296, inb-75aaf7.

## RESUME DELTA 23:02Z — evening, after Gene's rulings

Gene's rulings tonight (verbatim in the log): Gene reports = vs-native perf first, wins/losses, learnings, what's next, ALWAYS pasted to chat; "target 50% functional work (overall). But your call."; delegated approvals ("If you can approve responsibly, go for it!!!"); "Tell mayor to review CC merge with sol, and merge into main". ANDON inb-d27b79 CLOSED (merged a6df86ee, freeze lifted). Folder-trust dialog was the approval on Gene's phone → 112 dirs trusted, `~/bin/trust-dir` + `~/bin/worktree-add`.

| lane | state | running now |
|---|---|---|
| integration branch bridge/integration-2026-09-03 | composing the GOs onto main (mayor's ask); registry ratchet first; merge points: kernel 2df05b3, study-ops NEWER tip when O2 lands, apparatus 89295d8 | Opus integrator in ~/src/clj-surgeon-integ |
| E6-Lb | DONE: adoption 0/3 (0/13 program), no wall claim — docs/observations/2026-09-04-e6-lb-cohort.md | — |
| E3-P | DONE: T 49.3 s vs N 137.0 s (0.36× direction, inside the 172 s floor), 3/3 both, pass line fails on refusals; RECORD CORRECTED (next_call existed in structuredContent) — docs/observations/2026-09-04-e3-p-cohort.md | — |
| E6-Q (square 3 read-only, N/M/F) | running, port 7909 | Opus runner, /home/forge/tmp/arms/e6q |
| E6-C (WHEN plate vs bare) | running, ports 7909/7910 | Opus runner, /home/forge/tmp/arms/e6c |
| O2 ls-tree text rows + payload bound | building on bridge/study-ops-mcp | Opus, ~/src/clj-surgeon-study |
| q5z fix (bare dir scope; text ⊇ structured for refusals; quoted symbols) | building on bridge/q5z-alias-migration | Opus, ~/src/clj-surgeon-q5z |
| --k irregularity knob | building on bridge/fanout-fixtures-in-git | Sonnet, ~/src/clj-surgeon-fanout |
| E-REG (k sweep) + E-SLOPE80 | QUEUED behind the q5z fix and the knob; brief in brainfleet §16 | — |
| census | HELD e2d70d0; round 16 (one fence for every path) building | Opus, ~/src/clj-surgeon-census |
| fold-diff (curtain-call) | GO 3d344432, pin may move; approved order to the mayor with Sol review | mayor |
| kernel | GO 2df05b3 (merge point) | in the integration branch |

Mayor: draining via the integration branch; owns curtain-call merges (Sol-reviewed per Gene). Weekly Claude limit resets 19:00Z. Usage watch 96/49/47 flat by construction (cohort servers log elsewhere).

## RESUME DELTA 2026-09-04T01:41Z — MAIN IS FROZEN; this note now lives on bridge/anvil-seat-docs-2026-09-04

- **Gene, verbatim (01:4xZ):** "no one should be merging to main, even mayor. People are using public repo, and I don't want to publish anything on main until we have clear and decisive winner that is tested and dogfooded for months." Nothing from this seat goes to origin/main — code OR docs. The ~/src/clj-surgeon checkout is on `bridge/anvil-seat-docs-2026-09-04`; all seat records commit and push THERE.
- **Integration line:** `bridge/main-candidate-2026-09-04` @ 3411e3b = origin/main 56efeff + 2556a38 (code tree byte-identical to 2556a38; ten-gate seat pass applies). Next onto it, after their Sol verdicts: 0a38e3d (MEM-003 second landing), then the study-ops composition (bridge/integration-2026-09-03-studyops, building). The mayor fast-forwards NOTHING until Gene names a winner.
- **Policy commit:** bridge/main-policy-2026-09-04 (CLAUDE.md first section + AGENTS.md + this pointer) — Gene decides whether it lands on main.
- **Mayor:** OFFLINE since ~01:3xZ; told via inb-1165ce + queued session messages.
- **Reviews running (Sol):** O2 r2 a0b0520, MEM-003 0a38e3d, census r17 fb7f3b1, q5z r12 61dd334 — verdicts at /home/forge/tmp/sol/{o2r2,mem003,census17,q5z12}-sol-review.md; file them on the seat branch.
- **Cohorts:** E-HARNESS-2 (10 arms, both write-path flanks) on the lock; E-NSWEEP (12 native arms, N ∈ {30,40,55}) queued; E-GATE-R replay (0 arms) + chars/s vs load running. Standing: square 2 WIN bounded to N ≲ 40 (E-CEILING80: native writes a generator at N=80 and beats the tool on chars); squares single-edit/3/4 withdrawn; square 1 pending E-GATE-R.
- **Single next action:** when a Sol verdict lands, file it on the seat branch, update the queue row, and if GO put the tip onto the candidate branch (merge --no-ff in ~/src/clj-surgeon-cand, push the branch).

## RESUME DELTA 01:56Z — the working trunk is `MCP/main` (Gene); this note and all seat records live there

- Gene, verbatim: "Get everything onto a branch called MCP/. Let's have that be our 'main' branch, if you know what I mean". `MCP/main` @ a93768f = candidate 3411e3b + seat records + policy. The ~/src/clj-surgeon checkout is ON `MCP/main`; commit and push seat records there. `main` stays frozen (public); nobody pushes it.
- Landing rule on MCP/main: reviewed GO tips only, `merge --no-ff` from ~/src/clj-surgeon-cand (also on MCP/main) or this checkout; run mcp-test as the trailing check after each landing. bridge/anvil-seat-docs-2026-09-04 and bridge/main-candidate-2026-09-04 are retired (merged); do not push to them.
- Pending landings: 0a38e3d after MEM-003 r2 GO; study-ops after O2 r3 GO; census after r17 GO; q5z after r12 GO; admit-gate after its fix round GO. Then restart the seat's MCP server (7906) on MCP/main and produce Gene's ls-tree-over-MCP receipt.

## RESUME DELTA 05:04Z — temp files go to /var/tmp/forge, never /tmp (Gene)

Set in ~/.bashrc and ~/.profile (TMPDIR/TMP/TEMP + JAVA_TOOL_OPTIONS); ~/bin/seat-tmp-guard.sh refuses a tmpfs temp dir and is sourced by suite-run and sol-yolo. Every brief from here names /var/tmp/forge/<lane>-fx. Repo ratchet building on bridge/tmp-leak-ratchet (runners fail on leaked temp entries and refuse tmpfs). Heartbeats: df -i /tmp + count of forge-owned entries.

## RESUME DELTA 2026-09-04T05:43Z — full state for a post-compaction seat (Gene asked for the prompt at ~06:00Z)

**Rules in force (Gene, tonight, all verbatim in the captain's log):** origin/main FROZEN for everyone (public repo; nothing publishes until a months-dogfooded winner) — this seat pushed ~40 docs commits to main before 01:40Z, disclosed, not rewritten; `MCP/main` is the working trunk (seat checkout /home/forge/src/clj-surgeon is ON it; landings by `merge --no-ff` from the seat after an executed review); temp files to /var/tmp/forge never /tmp (profile + ~/bin/seat-tmp-guard.sh + Claude settings env; repo ratchet building on bridge/tmp-leak-ratchet); reviews Sol first, Opus on filter refusal (nine refusals tonight, all on permission/symlink/lock/trust-root lanes); the meter is TOOL CALLS; keep cclsp running — but this seat has none (inb-41c1cc: repo 404, launchd-only start; 7890 is the surgeon seat's, never touch); cohorts with a tool arm void arms that hit semantic-provider-unavailable.

**Landed on MCP/main:** the six-lane integration (2556a38 via the candidate), the admit gate (9b7220c3, fresh-clone GO-WITH-FIX; trailing suite 711/8436/0). **Standing sentence (Gene report 04:55Z, 2026-09-04-gene-report-0455z.md):** square 2 fan-out wins 2–7× in emitted chars for two callers at N ≤ 21 on the apply_patch harness; bounded at N* = 23 (E-NSWEEP), by write path (E-HARNESS-2: Bash-only native 0.68×), survives a second caller (E-CALLER 2.2×/3.7×); squares 1 (detection, E-GATE-R), 3, 4 withdrawn; the feature-thread study measured (E-THREAD: native 5/5, 0 false-completes; the script halves calls on SMW) — verdict "not a verb" was then REFRAMED by Gene on tool calls: build `feature_thread` v2 (bodies + sha + anchor + sibling one-axis + rules; 10/16 KB) and measure tool calls to a correct Dequote/Format edit (study §12; the mayor's transcript analysis: six read rounds is the human baseline; Dequote/Format is a NAMED TEST CASE with five assertions + a second fixture at the moment it broke).

**Lanes and where their state lives (agent IDs do not survive compaction; branches and verdict files do):**
| lane | branch @ tip | state | next |
|---|---|---|---|
| MEM-003 second landing | bridge/integration-2026-09-03-mem003 @ 3692e9b (r4 building: trunk merged in, deftype reading, :refer forbidden) | r3 Opus NO-GO | on push: review (Sol then Opus), land |
| O2 study-ops (MCP ls-tree) | bridge/study-ops-mcp @ 515e8109 (r5 building: budgeted dropped line, monotone fit, mid-band witness, with-envelope shape-agnostic) | r4 Opus NO-GO (regression) | on push: review; then recompose onto MEM-003 landing (template f835394) |
| census | bridge/census-verb @ 563c300d | r19 Opus review running (/home/forge/tmp/sol/census19-opus-review.md) | file; r20 or land |
| q5z alias_migration | bridge/q5z-alias-migration @ b6d1d17b | r15 Sol review running (q5z15-sol-review.md) | file; r16 or land |
| admit gate r3/r4 | bridge/admit-gate-r3 @ 95e7aed9 (r4 building on current trunk) | r3 Sol NO-GO (4) | on push: review; land |
| FAN scorer | bridge/fanout-fixtures-in-git @ f2fa8be9 | r5 Sol review running (fanout5-sol-review.md; Opus on refusal) | file; land (apparatus) |
| tmp-leak ratchet | bridge/tmp-leak-ratchet (building) | — | review; land |
| feature_thread verb | bridge/feature-thread-verb (building; three amendments: named test case, fleet refinements, §12 deltas) | — | review; then the 30-arm adoption cohort (N/F/M-range/M-body/K ×6) |
| E-SCALE-WALL | pre-registered (2026-09-04-escalewall-preregistration.md) | waits for a QUIET box (load < 2 before each arm) | run alone, after the review lanes land |
| curtaincall | bridge/cc-integration-2026-09-03 @ 37978a74 seat-gated green | with the mayor (offline since ~01:30Z) | mayor's Sol review + landing |

**Verdict files:** /home/forge/tmp/sol/<lane><n>-sol-review.md or -opus-review.md; logs /home/forge/tmp/sol/<lane><n>.log, finished when the LAST line is `EXIT n`. Inbox ids: inb-1165ce (mayor landings), inb-3e298e (O2), inb-b873d0 (q5z), inb-cbca17 (admit refusal text), inb-9483a4 (/tmp inode leak), inb-62a674 (enumerating-witness rule), inb-41c1cc (cclsp), inb-2f150d/84f801/10f4cd (gate defects), inb-55e00e (study, closed).

**Single next action:** heartbeat; file any finished verdict; relaunch the next round for any NO-GO; land any GO by merge --no-ff onto MCP/main from the seat checkout with the trailing suite; keep the mayor's queue current on inb-1165ce.

## RESUME DELTA 05:48Z (post-compaction, first action done)
- Compaction happened ~05:5xZ; Gene fed the resume prompt; note re-read. Memento pointer is LINE 1 of CLAUDE.md on MCP/main (7751e1f8).
- **FAN scorer LANDED on MCP/main = 804febcb** (Sol r5 GO, merge --no-ff; a .PHONY union conflict in Makefile resolved by hand; trailing `make fanout-selftests` log at /home/forge/tmp/trunk-fanout-selftests.log). Row F closed; inb-9c18e2 completed.
- Still in flight: MEM-003 r4, O2 r5, gate r4, tmp-leak ratchet, feature_thread verb (builders); census r19 (Opus), q5z r15 (Sol) reviews. Branch tips unchanged since 05:21Z.
- Next action: file census19/q5z15 verdicts when their logs end in EXIT; land MEM-003 r4 → recompose study-ops; E-SCALE-WALL waits for load < 2.0.

## RESUME DELTA 06:10Z
- feature_thread verb BUILT @ 02e823e7 on bridge/feature-thread-verb; Sol r1 review running (ft1.log / ft1-sol-review.md; clone /home/forge/tmp/sol/ft1-wt; reviewer ports 8126–8128). Inbox inb-cc9a4a. Next after GO: land on MCP/main, then the adoption cohort on tool calls.
- census r19 NO-GO filed; r20 builder merges the trunk in first (12-file conflict). q5z r15 GO-WITH-FIX filed; r16 building (ports 8123–8125).

## RESUME DELTA 06:32Z — FOREGROUND PROGRAM (Gene): the 10x tweezer on feature_thread
- Mission: prove tool-calls-to-a-green-edit 10x on the Dequote/Format replay; foreground tweezer, background orchestration. Baseline 17 calls to first patch (21 + patch to done). nREPL 43791 on ~/src/clj-surgeon-thread (bridge/feature-thread-verb @ ad49908c + round-two builder running on that worktree: implementation leg, governance anchors, co-primary tests, verify row, budget 24 KB + edit-aware elision). Receipt dump /var/tmp/forge/tweezer/r3-receipt.txt; watcher ~/bin/call-watcher. Next: naive-reader probe (Sol+Opus), then the measured replay: an agent given ONLY the receipt writes the patch; count calls with the watcher; then the cohort.

## RESUME DELTA 07:17Z
- Lanes: feature_thread r2 building (Opus, on ~/src/clj-surgeon-thread; r1 NO-GO folded in); q5z r17 building; tmp-leak r2 building; gate r5 building; MEM-003 r5 building; census r21 Sol review; O2 r5 Sol review. All reviews except O2/census are on Opus after Sol filter refusals (14 tonight). Trunk MCP/main last landing: the FAN scorer (804febcb). Foreground: the tweezer waits on feature_thread r2 for the measured replay (baseline 17 calls; target zero reads before the write).

## RESUME DELTA 07:29Z
- feature_thread r2 = 9139b2c5 (Sol r3 review running: ft3.log / ft3-sol-review.md). REPLAY in progress: clone /home/forge/tmp/replay/smw-base @ 2df99c98; arms N / T1 / T2 pre-registered in the log; receipt for the clone to be computed from the nREPL (port 43791, root = the clone); counts from the codex logs (exec_command + apply_patch), never from the driver.

## RESUME DELTA 08:20Z
- REPLAY DONE and verified (docs/observations/2026-09-04-feature-thread-replay-result.md; Gene report 2026-09-04-gene-report-0815z.md): N 32 / T1 24 / X 25 / P 31 / T1b 22 raw; "edit basis" withdrawn for this run, "discovery accelerator" stands; 10× is a harness claim (admit-gate write + no ceremony). N2 replicate still running (50+ calls). Next arms pre-registered: T2 (MCP-attached), T3 (round-three receipt; spec in 2026-09-04-feature-thread-round3-spec.md + the recall item), G (admit-gate write), C (no-ceremony repo).
- Lanes: feature_thread r2 = 9139b2c5 (Opus r3 review running); q5z r17 = 15fdf59c (Opus review); MEM-003 r5 = dc6ee93f (Sol review); tmp-leak r2 = 86bd9de3 (Sol review; one-line .PHONY conflict to compose at landing); census r22, gate r5, O2 r6 building. Trunk MCP/main last landing 804febcb (scorer). Sol filter refusals: 17 tonight — proposal to route census/gate to Opus first is with Gene.
- Housekeeping: /var/tmp/forge swept of own suite leaks >60 min each heartbeat until tmp-leak r2 lands; nREPL 43791 on ~/src/clj-surgeon-thread (feature_thread worktree); replay clones under /home/forge/tmp/replay/smw-*; meters ~/bin/rollout-calls (codex) and ~/bin/call-watcher (this session).

## RESUME DELTA 08:54Z — replay complete for tonight (11 arms verified green; full record docs/observations/2026-09-04-feature-thread-replay-result.md)
- STANDING SENTENCE: the feature_thread receipt is a DISCOVERY ACCELERATOR (reads before the first patch 11.7 → 1.3 at n=3; task-core 2.2×; raw 1.7×), NOT an edit basis (its own withdrawal line fired). The effect is content (placebo ≈ native); on a patch harness the BODIES carry it (stale ranges/shas ≈ correct receipt). Ceremony was NOT the diluter (withdrawn by the C pair). The MCP-attached arm halves reads but re-reads the lines after each anchor (after_context → round four). 10× is a harness claim: the write as one admit-gate call (arm G, needs the gate landed) + the round-three receipt (T3, needs r4).
- Lanes: feature_thread r4 building (review B3/B1′/B2′ + the round-three spec + after_context); q5z r18 building; tmp-leak r3 fix round building (then LAND with the one-line .PHONY composition); gate r5 Sol review; MEM-003 r5 Opus review; census r22 + O2 r6 building. T2 server on :8165 stopped. Usage refusals ledgered inb-5f5edd; collector needs a warm path (inb-65c941).

## RESUME DELTA 11:13Z
- Outage ~09:30Z (session limit) → all lanes relaunched 10:03Z from local commits; rules now in every brief: builders push per item, reviewers write verdicts incrementally.
- Lanes: feature_thread r4 building (ft3 NO-GO folded + round-three spec + after_context); q5z r19 fix round (r18 Sol NO-GO on one: kind in the second parameter); tmp-leak r4 Sonnet fix (r3 Opus GO-WITH-FIX: $(filter) subpaths) → then LAND with the one-line .PHONY composition; census r23 Opus review (r22 built; Sol refused); gate r6 building (r5 NO-GO: caller field echoed verbatim); MEM-003 r6 building (r5 NO-GO: ._launder interop + ns-resolve); study-ops r6 building (r5 Sol NO-GO). Trunk MCP/main last landing 804febcb (scorer).
- Replay complete (11 arms verified; standing sentence in the 10:5xZ delta). Gene reports: 0815z, 1015z; captain's log entry "the night of the tweezer" at a96a665e.

## RESUME DELTA 11:30Z
- tmp-leak ratchet LANDED: MCP/main = d0b4e1ca (merge --no-ff of 5a6e7c81; gate + bb green on the merged tree; trailing mcp-test log /var/tmp/forge/land-tmpleak-mcp-test.log). Landings tonight: integration, scorer, tmp-leak. Row TMP closed; inb-9483a4 completed.

## RESUME DELTA 12:14Z
- Landings tonight: integration, scorer, tmp-leak (d0b4e1ca), alias_migration/q5z (44e70af5 + re-pin merge-fix). Trunk MCP/main last landing 44e70af5.
- Replay: T3 (round-four receipt) = 19 raw, ZERO reads before the write, verified green — the edit-basis line for T3 is MET; T3b replicate running (12:13Z). Peers are elided by the trunk's 32,640 structured cap (round-five item). Arm G waits on the admit gate landing.
- Lanes: feature_thread r5 (Sol review of bb3b6360; ft1-wt clone); MEM-003 r6 (Sol review of 432268cf, trunk-merged); gate r6 (Opus review of ed20fa35); census r24 fix build (trunk merge + :paths fence); study-ops r7 build (one carriage predicate). nREPL on ~/src/clj-surgeon-thread port 45759.

## RESUME DELTA 12:54Z
- Lanes: feature_thread r6 building (Sol r5 NO-GO: escaping conventions globs; saveDraft route from a docstring; + the round-five spec); MEM-003 r6 Opus review (Sol refused; 432268cf); census r24 fix build (trunk merge + :paths fence; from r23 GO-WITH-FIX) → lands after; study-ops r7 build (r6 Sol NO-GO: one carriage predicate); gate r7 fix build (r6 Opus GO-WITH-FIX: stale over-budget annotations; empty trim annotated) → lands after; then arm G. T4 waits on ft r6.
- Sol filter refusals: 24 tonight. Landings: 4 (integration, scorer, tmp-leak d0b4e1ca, q5z 44e70af5).

## RESUME DELTA 14:14Z
- Lanes (all builders, no reviews running): gate r8 fix (r7 Sol NO-GO: the gate must own its battery receipt; witness the cheap-move-first ORDER) → lands on GO; study-ops r8 (r7 Sol NO-GO: escape dotted pointers; single-line rendering) — the ls-tree text-doubling product change awaits Gene; census r24 fix (r23 GO-WITH-FIX: :paths fence + oracle names + trunk merge) → lands after; feature_thread r6 (r5 Sol NO-GO: escaping globs; route from a docstring; + the round-five spec) → T4 after; MEM-003 r7 (r6 Opus NO-GO: three spellings per derived name; floor manifest; calls not lines).
- Sol refusals: 24; every gate lane except gate r7 and O2 went to Opus. Landings: 4. Reviewers write incrementally; builders push per item (the census builder needed a nudge at 13:4xZ and complied).


## RESUME DELTA 14:44Z — T4 pair scored
- feature_thread r6 tip 529755f0; Sol r7 review RUNNING (ft7-sol-review.md, ft1-wt clone). T4 pair (round-six receipt, real repo): raw 19/18, pre-write reads 0/2 — line MISSED on T4b; residual = dispatch/fold seam + registry tail (see 2026-09-04-feature-thread-replay-result.md 'T4 pair'). Next lever is arm G (admit gate) once the gate lane lands. Receipt at 32k with nothing elided saved: /var/tmp/forge/tweezer/replay-receipt-T4-full32k.txt.
- Builders live: gate r8, O2 r8, census r24, MEM-003 r7.


## RESUME DELTA 16:52Z — census LANDED; Gene's wall-clock refocus
- LANDED: census bridge/census-verb 2d82a677 as merge bafc273a on MCP/main (fifth landing). Trunk MCP/main head 49aeef9d.
- GENE'S RULING (16:4xZ, this morning): chase >=2x on WALL CLOCK, not fractional gains. Standing meter = wall per turn species
  (script in the replay doc, 'Wall clock by turn species'). Receipt effect is done (-3.2 min code reads). Levers: (1) SMW landing
  contract — untrack .beads/.local_version (already in .beads/.gitignore, tracked by commit 638ec8c9), replace the bd boilerplate
  block (CLAUDE.md L610-654) with an 8-line chain, distill the binding rules to the top — WAITS ON GENE'S GO (his repo); then rerun
  native pair on the slimmed plate as the new baseline; (2) admit gate arm G (write+verify in one call) when the gate lane lands.
  feature_thread FREEZES after its landing review (r9 Sol running on 3dfe0895). No new polish rounds unless a landing blocker.
- Lanes: ft r9 Sol review (ft9-sol-review.md); gate r13 building on b489a276 (Sonnet; catch Throwable, present-but-nil = failed,
  edn/read-string) then Sol/Opus review; study-ops r11 Sol review on dda9fa29; MEM-003 r8 Opus review on a2a15cc0 (Sol refused).
- Gene decisions outstanding: SMW landing-contract branch go; cclsp repo (inb-41c1cc); ls-tree text doubling (STUDY-051).
- Usage collector: 12 consecutive 120 s timeouts under load (ledger inb-65c941). maven-w is not installed on this seat.


## RESUME DELTA 21:30Z — the "in charge" evening (Gene delegated decisions at 19:1xZ)
- LANDED today (6): integration, scorer, tmp-leak, alias_migration, census (bafc273a), ADMIT GATE (8d32d619). MCP/main head bc0bec26.
- ONE-SHOTS in ~/bin (use them, never the hand versions): sol-finished, replay-arm (MCP_URL env), usage-watch, land (runs in
  /home/forge/src/clj-surgeon-land, detached; pushes HEAD:MCP/main only on green). Memories: anvil-seat-one-shots, landing-runs-in-its-own-worktree.
- WALL-CLOCK RULE in vision.md; the receipt is the only measured win (1.45x); plate (NC/NS) and gate-as-write-path (G/GN, G2) WITHDRAWN;
  G3/GN3 running on the r16 gate (server 8173 from 2ac33278; 8171 = landed trunk) with the gate's own prescription (JS natively, clj/edn via gate).
- SUITE SPIKE: bridge/suite-spike 2ecce8c4 (merge gate 717 s -> 150 s; fast lane 30 s; N=4 battery 12/12; lane manifest with cadence;
  make test-fast = JVM fast lane, bb = make test-bb) — Sol review running (suite2). On GO: ~/bin/land, then nightly cron + receipt ledger + tripwire.
- GHA: bridge/gha 5ce8aaea — workflow green on runners (gate 205 s; battery 11-wide 10/11); lands with the spike. Nightly DORMANT until the repo's
  default branch is MCP/main — seat lacks admin; request filed for the mayor in the merge queue doc; Gene ruled "y".
- SMW: landing contract MERGED into realgenekim/social-media-writer main d5b1ba53 (Gene "a: go"); replay bases smw-base@2df99c98, smw-base2@d5b1ba53.
- Lanes: feature_thread 508f26f5 Sol r12 LANDING review (ft12) then FREEZE; MEM-003 a9963bd3 Opus r10 landing review; admit gate r16 2ac33278
  Opus r17 review (Sol refused) + next_call-loop attack; study-ops r12 building (residual as refusal; STUDY-051 ACCEPTED by the seat, capped);
  CI fixes bridge/ci-findings (Opus takeover; Sonnet stalls on backgrounded suites — Opus for any brief that runs a suite).
- Gene rulings today: wall-clock rule; "B. Go" (spike r2 = mechanism first); invest in suite/CI ("option value"); STUDY-051 delegated; SMW "a: go".

## RESUME DELTA 02:00Z
- Trunk MCP/main head = 87ee0ae5 (suite landed on Gene's order) on 23c0c34d (Astra integration). Landing one-shot ~/bin/land runs in clj-surgeon-land; being patched to call `make landing-gate`.
- Astra (codex gpt-6-astra, pane forge-anvil:1.0) holds a logical reservation of the box after both landings; file pair /var/tmp/forge/{astra-fable-coordination,fable-to-astra}.md; nothing of mine starts a JVM without announcing there first. Opus harness r3 tip 3b640785 awaits his review (no live Opus arm).
- Servers 7906/8171 still run the pre-suite trunk head (0dec06ea-era); restart on 87ee0ae5 once Astra clears a window. Gene wants the best stable build installed.
- Owed: telemetry 417ed6b5, CI 49b92905, gate r18 09660168, MEM-003 d0e82620, study-ops e24ee131 — all gates held for Astra's allocation.
- Lesson: chain waiters bind to a PID, never argv text (memory waiters-bind-to-pid-not-argv).

- 02:13Z: default `make test` is still the FULL run (battery + tail); fast lane = `make mcp-test`; landing list = `make landing-gate`. Do not tell Gene "faster by default" until bridge/test-default-fast lands (needs his ratification + green battery-fresh).

- 02:45Z: 7906 PID 2164551 / 8171 PID 2164553 on trunk b8249bc5 (cores 6–9). Battery running (battery-clean-0243.log). Flip tip 971bc4a7 awaits Astra confirm → land. Then telemetry gates on the lane. Read docs/observations/2026-09-05-astra-next-api-advice.md (Astra's API direction).

## RESUME DELTA 03:53Z
- Trunk MCP/main = 2209c61e (candidate 0305 landed as 68f24f51). `make test` = landing gate; `make test-full` = full. ~/bin/land runs battery-fresh first and keeps its log (tee -a). Servers 7906 PID 3534456 / 8171 PID 3534458 on trunk, lane 6–9.
- Astra directs the API effort (six-helper closure design is HIS next lane). Coordination file pair unchanged. Telemetry 417ed6b5 HELD (touches the collector Astra owns; his counterexample pending). Opus harness r3 3b640785 NO-GO. Owed: fence launcher split, recovery-battery receipt, N=4 battery (after Astra's lanes drain), MEM-003/study-ops/CI/gate-r18 gates.

- 04:03Z: Astra IDLE (goal achieved). Waiting on him (or Gene) for: phase-2 GO on helper_extraction (bridge/helper-closure-design 4fbe8346), telemetry 417 counterexample, Opus harness r3 review. If Gene asks "what next": the owed non-runtime work is the fence-launcher split design and the Opus harness preparation fixes; runtime work (recovery-battery receipt, N=4, MEM-003/study-ops/CI/gate-r18 gates) needs an announced window now that the box is quiet.

- 04:53Z: trunk = fbd68a1c (6ef3de2b landed). Servers 7906 PID 523110 / 8171 PID 523113. Design rev 2 = 9d4b54bb awaiting Astra; his two-hour proposal awaits Gene. Ratchet owed: battery ledger one-file-per-receipt (or union-sort in land); landing list should include admit_patch_test + worktree_lifecycle_cli_test Makefile witnesses; fence launcher split.

- 06:26Z: helper_extraction FROZEN CANDIDATE ee03b49a on bridge/helper-extraction-impl (pure 34/440/0, boundary 16/179/0, fast lane 547/6862/0). Sol fence r1 running (~/tmp/sol/helper-fence-r1.log). Astra owns public preflight + measurement in his window (to 08:56Z). Design branch bridge/helper-closure-design merged into impl. Landing on trunk only after fence GO + Astra's public proof; land runs battery-fresh first.

- 07:14Z: helper_extraction CORRECTED FROZEN CANDIDATE d337964e (impl branch). Sol r2 + battery running on it; Astra preflight-02 on it; land HELD during his timing. Frozen sha also in /var/tmp/forge/helper-frozen-sha.txt. Owed follow-ups: telemetry emission for the verb; factor the shared commit+verify block; alias_migration :per-arity-scopes adoption (alias owner's call); memory-lane witness split registration at GREEN.

## RESUME DELTA 08:01Z
- helper_extraction EXPERIMENT SHA = 05df8b6d (bridge/helper-extraction-impl); gate commit a3caddec (+ two docs). Full gate set green (pure 34/440, boundary 30/600, fast 547/6862, bb 859/7337, battery 512/11008 :pass, recovery 3/3, hygiene); Sol r1 NO-GO → r2/r3 GO-WITH-FIX (schema strictness only); Astra public positive/negative 03 GO. Astra runs the timed cohort on 05df8b6d; box QUIET; land HELD; nothing of mine may start a JVM until his cohort ends.
- v4 WRITTEN, UNRUN, uncommitted in clj-surgeon-helperimpl: mcp_schema.clj (per-variant matrix def helper-extraction-receipt-variants → oneOf) + mcp_helper_extraction_test.clj (generated matrix witnesses). After the cohort: send "RUN" to both builders (boundary: schema probe + mcp-helper-extraction-red + mcp-test; tests: mcp-helper-extraction-red), commit by path → v4 sha → focused gates → battery → Sol r4 delta (schema only) via ~/bin/fence-run → landing needs lane enrollment (pure→:fast, boundary→:battery, delete red targets) + land after Astra's word.
- Standing servers: 7906 PID 523110, 8171 PID 523113 only (stale 8173/8175/study/thread JVMs stopped 08:01Z). Scratch reviewer worktree clj-surgeon-fence (detached). One-shots: ~/bin/fence-run, ~/bin/land (battery-fresh first).
- Owed: telemetry emission for the verb; shared expected-tool-list Var (five test literals); factor shared commit+verify block; :per-arity-scopes adoption in alias_migration (owner's call); telemetry 417ed6b5 NO-GO; Opus harness r3 NO-GO.

- 09:10Z: v5 = 60d6e0e2 (bridge/helper-extraction-impl); v4 gate commit e40651d8 passed make test. Sol r5 + v5 battery running; then receipt commit → make test → if Sol GO: ask Astra/Gene for the landing word; land via ~/bin/land (battery-fresh first) → MCP/main. Astra window CLOSED 08:56Z; his handoff = plan of record (queue doc). Fast-lane budget fragility under load noted for the owners of mcp-compact-relations-test / outline-differential-test.

- 09:48Z: v6 = ce162604 (gate commit 818083aa; battery 553/11951 :pass; make test running). Sol r6 overall boundary landing verdict GO-WITH-FIX: (a) admit an honest unknown-proof verification shape (throw path emits {status "unknown"}) without fabrication; (b) type/pin remaining nested invariants (closure.grammar, integer counts, booleans). v7 in flight (boundary: schema/mapper; tests: throw-path exemplar + type witnesses). Loop per round: commit → focused (both ns via classpath) → battery + Sol rN via fence-run → receipt commit → make test on gate commit. Landing request only on a clean Sol GO; Astra closeout forbids automatic landing.

## RESUME DELTA 10:23Z
- helper_extraction LANDING-READY at e1239a99 (bridge/helper-extraction-impl; gate commit f70e72bb). All gates green, Sol r7 GO. Waiting for Astra's or Gene's landing word; then `~/bin/land e1239a99 "<title>"` (runs battery-fresh first; the e1239a99 receipt is on the branch). Do NOT land without the word. Follow-ups per Astra handoff in the queue doc. Builders idle.

## RESUME DELTA 14:54Z
- helper_extraction LANDED: MCP/main = 3ffc6fae (merge 3ffc6fae). Servers 7906 PID 3937847 / 8171 PID 3937849 on it. Astra tasked (Gene 14:5xZ): telemetry blind spot fix + 4-hour fair replicated comparison (his handoff item 7); WATCH: his pane shows gpt-5.6-luna now. Mayor instructed via the queue doc + /tmp/anvil-to-mayor.txt to pull+install 3ffc6fae. Next: relay Astra's ack/plan; hold my lane quiet during his timed arms; follow-ups per the handoff.

- 15:04Z: RECORDS NOW LIVE IN /home/forge/src/clj-surgeon-records (branch records/MCP-main tracking origin/MCP/main); NEVER commit/push from /home/forge/src/clj-surgeon (shared with the pane agent, who switches its branch). 7906 runs from clj-surgeon-srv7906; 8171 from clj-surgeon-srv8171. Trunk head after the revert: 57343cb5.

- 15:35Z: MCP/main = 01f66435 (Astra telemetry landed as 2b5b3d97). Servers 7906 PID 539986 / 8171 PID 539988 on it. land refuses ancestor tips/empty merges now. Astra idle after his block (telemetry + prereg delivered; no accepted comparison). Next per his handoff: oracle revision, then a fresh preregistered epoch with six accepted controls.

- 16:26Z: MCP/main = 32910d14 (Astra production-readiness landed as 32910d14). Servers 7906 PID 1565316 / 8171 PID 1565318 on it; CLI/skills installed. The pane agent keeps using the shared seat checkout on its own branches — read its branch there before assuming trunk.

- 21:23Z: Spark (gpt-5.3-codex-spark) still limited; Codex says "try again at 11:37 PM" (box time UTC). Next action after that: the three-way test (native apply_patch / Spark emits mission request / Spark emits native patch), effort low, dossier prompt, 5-min cap, gate outside; design in docs/observations/2026-09-05-ideal-tool-riff-fable.md §8.

## 2026-09-05 23:0xZ — state after the fast-typist program (read this before anything else)
- Trunk MCP/main head includes 13c12401 (request-shape :example refusals) and all tonight's records; local install (CLI, skills, 7906/8171 servers) is at 13c12401.
- Typist prototype lives on bridge/mission-ledger (runner bin/typist-run, tip f2efc87c): missions scope-roots / onesite / fanout; providers groq / openrouter (pin via TYPIST_OPENROUTER_ORDER=Cerebras, no fallback) / spark; --bench mode. Keys: ~/secrets/groq.edn {:key …}, ~/secrets/openrouter.edn {:openrouter-api-key …} — never echo, never env.
- Results docs on MCP/main: 2026-09-05-fast-typist-cohort-1.md (four cohorts + standing summary), 2026-09-05-fast-typist-provider-bench.md (Groq / OpenRouter / Cerebras rows; Cerebras k=5 fan-out 6/6 at 1.95 s vs cold Sol 26.66 s). NOT KEEP for search on easy dossiers; search real on fan-out; all cold-vs-cold on a five-file fixture.
- PENDING: Spark provider row after its usage-limit reset (23:37Z): `cd /home/forge/src/clj-surgeon-mission && bin/typist-run --bench --providers spark,groq --dossier bin/typist-dossier-onesite.md --rounds 6` then the same on fanout; prediction on record: Spark 5–10 s (process start), falsifier <3 s median on onesite. Then add the rows to the provider-bench doc.
- Astra runs epoch 2 of his fair comparison (attested receipts in /var/tmp/forge/fable-to-astra.md under "ASTRA EXPERIMENT"); friction item inb-a9b30e (helper_extraction preflight refusal x2).
- Owed to Gene: a morning report in the four-section format (vs-native table first) covering the aperture window, the typist program, and Astra's epoch 2 when it lands.
