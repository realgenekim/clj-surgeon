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
