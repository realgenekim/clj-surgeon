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
