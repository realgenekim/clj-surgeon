# RESUME HERE — bridge seat, surgeon wall-clock-ideal program (refreshed 2026-09-02 12:25Z; read the UPDATE blocks bottom-up, the newest supersedes)

STANDING ORDERS (memory files): headlines first (table: arm, wall, correct? + one learning + one
caveat); record every experiment in clj-surgeon docs/observations/2026-09-02-captains-log-bridge-wall-clock-ideal-program.md
the moment its receipt lands; one unit at a time on a NEW apparatus, poll every 2 min, fail fast;
JVM/multi-arm experiments on Anvil never Buster; after each result poll Sol+Opus (fleet) for
interpretations + next wave, run several in parallel, never pick one; stay on the main quest.
Gene away (son to college); works to ~06:00 PT. Mayor at skiff is live and drains the channel.
MAYOR'S TOP QUESTION (05:02Z): does the tool make an agent write BETTER code — clarity/conformance
above wall. Report quality alongside wall or the number is worthless.

WHERE THINGS ARE
- Anvil: ssh tester@100.66.152.23. ~/acid/{mvr,surgeon-main,surgeon-shipped}; runners ~/acid-cohort.sh
  (v1, old sequential chain), ~/acid-cohort-v3.sh (rung table: M medium, S small, R1 surgeon real
  task, R2 mvr real task; arms N native, A shipped(7888), B wave(7889), C wave no string symbols,
  P shipped+plan, Q native+plan, K CLI-only; CORES_PER_ARM env, TESTS_BASELINE=578); prompts
  ~/prompts/*.md and ~/acid-arm-prompt*.md; receipts ~/acid/receipts/<run>.log + per-arm
  report/diff/run.log/kaocha/pages/PLAN.md; rescore.sh + acid_acceptance_test.clj (9 spec tests)
  -> rescore-anvil*.txt; count_actions.py. Servers: shipped 7888 (cores 12,13), wave 7889 (14,15).
- Chain queued (sequential drivers): s7 (Q P N, running) -> r1 (R1 real task "N A|B") -> cal2
  (CORES_PER_ARM=2, six medium arms N A N A N A parallel) -> k1 (K|A N). Monitor task bl4dqd1bp.
- Buster scratchpad (this dir): packet.sh (fleet packet builder incl. acceptance + actions + plans),
  score/phases/clock/count scripts, quality-review-*/ (key.txt maps impl-N -> arm), RESUME-HERE.md,
  typist tool ~/bin/typist (OpenRouter gpt-oss, bounded), sol-yolo wrapper ~/bin/sol-yolo.
- Buster: keep light (memory). No dev instance running. Worktrees mvr-acid-{A,B,N,T}, mvr-accept.

RESULTS (Anvil, seconds, all correct): native 243,287,194,263,283 ; A 243,237,317,273,244 ;
B 303,272,579,330 ; C 328,333 ; P 258 ; Q 221,338. Buster run2: N 191, A 303, B 319, T(typist) 409.
Actions: wall ≈ 9 s/action + 40-60 s/suite run; wave arms take more actions.
Blind quality (Sol+Opus, noise floor ±1): native ≈ A ≈ 17-18; B ≈ 14-15 (clarity 2/5), C 12-17.5.
Acceptance (9 spec tests, mean failed assertions/diff): A 1.0, N 2.4, B 2.5, C 5.0, T 2.0.
Findings: refusal fixes did not move wall (retained negative); string-symbol payload NOT the
clarity cause (C low too); suspects = the two edit-path fixes shared by B and C (overlap fix,
insertion-gap fix); planning arms predict step costs well but miss ~1/3 of wall (boundaries);
typist added actions (negative). Mayor holds skiff's refusal-recoverability branch on this.

NEXT (in order): 1) s7 receipt + s6/s7 blind reviews (2 judges) + rescore -> fleet round 2 packet
(packet.sh) to Sol + Opus; 2) r1 real job: rescore/verify, merge best correct diff to surgeon main
(bridge may merge; docs-first); 3) cal2 -> if 2-core walls match, use 6 slots; 4) discriminating
experiment: wave build with overlap fix and gap fix reverted one at a time (arms B-minus-overlap,
B-minus-gap), 8 per cell via small/medium rung; 5) k1 CLI arm (follow-up inb-ce2f15, low priority);
6) R2 real job (F5 observe meter) after r1 clean; 7) large multi-owner rung still unrun.
PENDING HUMANS: Gene ratifications (MCP-OP-INSERT-001..006, string-outline contract, insert_pair);
surgeon1 verdict on SCI allowlist; mayor's 11-agent hypothesis fleet output on the clarity finding;
branch retirement held by mayor until fleet quiet.

UPDATE 06:05Z (supersedes older lines above where they conflict)
- Servers on Anvil (all tester-owned, port ratchet PORT-NOT-MINE in runner): 7893 = my shipped 64eac2e (A/P/U/V/W arms),
  7889 = surgeon-main at 41eee738 (mayor's verified SCI fix; B/C arms), 7891 = wave minus insertion-gap fix (arm G),
  7892 = wave minus overlap fix (arm O). 7888 on Anvil is ANOTHER USER'S production Surgeon (e7f72e2): never call it;
  all Anvil "A" rows before 05:30Z are relabeled "production e7f72e2" (log 06:00Z receipt). Andon CLOSED at skiff.
- Runner: ~/acid-cohort-v3.sh == v5 (arms N A B C P Q K U V W G O; CORES_PER_ARM; TESTS_BASELINE=578; port ratchet).
  Chains: k1 (K done 451 s; A,N running) -> e3 M "A U V W A U|V W A U V W" (2-core wide; U=A+report-only,
  V=A+budget rule, W=A+deliberate 3-plan selection; score on ACTIONS + acceptance + 2 judges; wall has contention
  caveat) -> b1 M "G O A|G O A|G O A" (bisect). Monitor task bjjew05fm (2-min poll, reads s7/r1/cal2/k1/r2/s8/s9 only;
  add e3/b1 to its file list if restarted).
- New results: cal2 six-wide 2-core: N 223/289/469, A 232/250/355 (tails inflate; wide = not for wall). r1 real job:
  defect did not exist (sandbox artifact); best regression test merged 2b3177d; bead 9yy amended by mayor.
  k1 K (CLI-only) 451 s correct. Stage 0 refusals-vs-failures rho -0.14 (undetermined). Opus round 2: input tokens
  carried predicts wall better than actions (R2 .78), Surgeon arms carry MORE context than native; drop priced
  planning, keep read-gate. Turn-budget doctrine drafted (turn-budget-rule.md) -> skiff inbox inb-5a2d7b.
- Pending humans: Gene ratifications (INSERT-001..006, string-outline contract, insert_pair); mayor: readable copy
  of production 7888 telemetry 03:09-05:20Z; retirement pass when fleet quiet; surgeon1 may accept witness-test help.
- NEXT after compaction: (1) read e3 receipts -> count actions (count_actions.py on Anvil rollouts) + acceptance
  (rescore.sh) + two judges (Sol via quality-review dirs, Opus via agent) -> log headline table -> decide how much of
  the turn-budget paragraph survives (Hawthorne control U vs V vs W). (2) b1 bisect -> which edit fix causes the wave
  clarity/conformance deficit -> log + tell mayor (slice-0 hold). (3) k1 A/N -> K action count -> inb-ce2f15 note.
  (4) R2 real job (F5 observe meter, prompts R2-*) one unit. (5) Large multi-owner rung still unrun (E4).

UPDATE 06:40Z (supersedes above where they conflict) — COMPACTION INSTRUCTIONS, durable copy (Gene: "Otherwise memento problem")
- On first action after compaction, read this note (clj-surgeon main docs/observations/2026-09-02-resume-here-bridge-program.md,
  same text as scratchpad RESUME-HERE.md) before doing anything else; it supersedes the summary wherever they disagree.
- Then: ssh -n tester@100.66.152.23 'tail -5 ~/acid/receipts/e3.log'; if monitor bjjew05fm is gone, restart it with e3 and b1 in
  its file list. Do NOT relaunch anything before reading receipts: chains are queued and a second launch collides on cores.
- Anvil hygiene ratchets learned 06:30Z: (1) never `sed -i` a bash chain script while it is running (bash reads by offset; the
  edit shifted the group string and dropped arm W and the cp line); (2) chain drivers must not copy runners — v5 is canonical,
  copy only while nothing runs; (3) "<run> done" markers must be written from a trap, the k1 driver died on a syntax error
  after its arms and the marker never appeared (chain-e3 waited 12 min; marker written by hand); (4) pkill -f patterns must
  live in a scp'd script, never in the ssh argv (killed my own ssh twice); (5) use `ssh -n` when the remote backgrounds a job.
- e3 state: first launch 05:48Z ABORTED (runner had A/U/V at 7888 = not mine, ~1 min of calls went to that server; disclose to
  mayor); relaunched 05:50Z "A U V W A U|V W A U V W" on 7893 (receipts e3.log; aborted copy e3-aborted-7888.log). Caveat: the
  W start line printed server_sha=41eee738 while its url is 7893 — a per-arm sha capture race; verify from 7893 telemetry.
  chain-bisect.sh still waits on "e3 done" then runs b1 "G O A|G O A|G O A".
- Edit-wall retro DONE (log receipt 06:25Z, commit a369097): tests are 2–48 s per arm, subtracting them reorders nothing;
  ~87% of wall is model time between actions; suite_invocations grep over-counts up to 12x; 9 s/action is an average
  (K: 45 s/action). Sol predicted a big shrink (trusted my bad premise); Opus checked the premise and was right.

UPDATE 06:50Z — RECORD CORRECTION: all Anvil A/P rows before e3 (s2..k1, incl. cal2 and k1) called 7888 (identity
unverified; mayor: another seat's production e7f72e2). The runner's server_sha field is the LOCAL checkout sha, not the
server's. Shipped-64eac2e rows = Buster acid2/A + e3 arms on 7893 only. Acceptance headline "shipped beats native" is
withdrawn until e3 rescore (rescore-anvil-cal2k1s7.txt running; e3 rescore next). Log receipt 06:50Z.

UPDATE 08:05Z (supersedes above where they conflict)
- e3 COMPLETE (12 arms, A/U/V/W x3, all gates green, all on 7893). Group 1 scored + logged (9ed0a39 quality, 3f6fa0c
  actions/acceptance, 7e872f3 fleet round 3). Group 2 scoring in flight: blind judges Sol (scratchpad quality-review-e3g2/
  review-sol.md) + Opus (review-opus.md; key.txt maps impl-N -> worktree), Anvil scorer writes ~/acid/receipts/e3-g2-score.md
  (script ~/e3_final.py pins rollouts by worktree path; trust it over edit_wall.py/count_actions.py for doubled arms).
  Group-2 acceptance: rescore-anvil-e3g2.txt. When all three land: log the group-2 table + per-arm means n=3 + one learning.
- Chains on Anvil: ~/acid/chain-next.sh (running): b1 "G O A|G O A|G O A" at 4 cores (started 06:13Z) -> n1 "A N A N A N|N A
  N A N A" (clean shipped vs native, 2-core) -> k2 "A K A K A K". Then ~/acid/chain-after-k2.sh: v1 "A A A A A A|V V V A A A".
  Runner v5 now writes per-slot diffs/logs and slot= on end/gate lines (v3 refreshed from v5 by chain-next after e3).
  Monitor bq3bcj15n relays e3/b1 lines; when b1 done, RESTART a monitor listing n1 k2 v1 too (its file list is fixed).
- For each cohort when done: bash ~/anvil-freeze.sh <run> (idempotent per-worktree diffs) -> scp diffs -> shuffle into
  quality-review-<run>/ with key.txt + prompt.md (copy from quality-review-e3g1) -> Sol judge (codex exec read-only) +
  Opus judge agent -> rescore.sh on Anvil -> scorer (message the Anvil scorer agent or a new Opus agent with e3_final.py)
  -> log receipt -> fleet round (Sol + Opus) -> queue next -> tell mayor (policy-10 shape).
- Aborted first e3 launch (7888): rollouts ended 05:49:10-18Z, zero-byte diffs, no survivors; exposure ~1 min as disclosed.
  Pre-e3 A/P rows (s2..k1) DID call 7888 for their full runs (14 arm-runs); disclosed to mayor 06:50Z.

UPDATE 09:20Z — chain-next/chain-after-k2 KILLED (parents only); replaced by ~/acid/chain-2.sh: waits "b1 done" + driver exit,
cp v5->v3, fullgate.sh on b1 diffs (b1-fullgate.txt), then n1 -> k2 -> v1. b1 diffs frozen per group as b1-g1/g2/g3-<arm>-<slot>.diff
(watcher b1-g2-freeze.sh). Runner v5: gate serial, names <run>-g<gi>-<arm>-<slot>, diff vs base, g= on end lines. Acceptance
b1 g1: rescore-anvil-b1g1.txt. Judge all 9 b1 diffs in one round when b1 done (rubric clause: appended reassignment is prescribed;
clarity may score body duplication only; judges cite the spec clause). Doctrine v2 = inb-beecb9. Rung L design in scratchpad rungL/.

UPDATE 10:05Z — rung L INSTALLED on Anvil (v5 L) entry = M gate; prompts ~/prompts/L-*.md; acceptance ~/acid/receipts/acid_L_acceptance_test.clj
+ rescore-L.sh; base = 12 tests 39 failures; TESTS_BASELINE for L = 577). chain-3.sh queued: after "v1 done" runs l1 L "A N A N A N|N A N A N A".
Known L prompt nits (unused by l1): U/V/W variants carry two TURNS formats; planning blocks quote the full-suite price. Mayor merged
analyzer-flake fix 33e03075 (pulled); sweep of Anvil receipts for that signature: 0 files.

UPDATE 11:40Z — b1 DONE and logged (3b99ea6 actions, 383b2e6 quality, aece0dd fleet round 5). Reading: overlap fix exonerated;
insertion-gap fix suspect via "productive refusal" (fix removed the re-read that produced DRY code); indentation-regex finding sent
to skiff for surgeon1. b2 replication: ~/acid/chain-4b.sh after "l1 done", randomised O/G order, one wave. Capture ratchets: runner
+ freeze stage before diff; anvil-freeze.sh now has a completeness gate (INCOMPLETE suffix if any worktree path is absent from the
diff). Monitor bfmmu5zhr relays n1/k2/v1/l1 (+ chain log); add b2 when restarting. n1 g1: N 231/259/521, A 426/714/722, all green.
Pending: scorer's refusal-reason table for b1 (b1-score.md "refusal reasons"); n1 g2 -> freeze -> judges (prompt from
quality-review-b1/prompt.md, 12 impls) -> rescore -> scorer -> log -> fleet -> mayor.

UPDATE 12:25Z — n1 DONE (12 arms, verified 7893): walls N 231/259/521/363/402/427 (mean 367), A 426/714/722/592/754/851 (mean 677);
all gates green; logged 1163443. Diffs frozen complete (12/12); judges running (quality-review-n1/, 12 impls, key.txt); scorer
(Anvil agent, ~/b1_score.py + b1_refusals.py) writes n1-score.md with typed refusals + tokens carried; rescore-anvil-n1.txt.
Chain: k2 "A K A K A K" RUNNING (07:20Z) -> v1 -> l1 -> b2 (chain-4b, randomised). Bisect settled (3ed0f84): gap fix introduces
ambiguous-insertion-gap; beads filed by mayor: clj-surgeon-f5e (P1), -vcz (P2, credible not proven), -xio (P1 invalid-intent-form
tax). Mayor's sol fixed the analyzer flake (33e03075). NEXT: n1 tables -> log -> fleet round -> mayor (this is Gene's wake-up headline).

UPDATE 13:55Z — n1 scored + quality logged (6e4ff8f, fac1449); fleet round 6 (cc22b63): write contract makes Surgeon additive;
substitution is the hill. Wake-up brief for Gene: docs/observations/2026-09-02-wake-up-brief-surgeon-program.md (ff52189).
Arms X (M-optional: Surgeon optional) and Y (M-receipt + L-receipt: substitution mandate + trusted receipts) installed in v5;
chain-3b (replaces chain-3): after "v1 done" runs s1 M "X N X N X N|Y A Y A Y A" then l1 L "A N Y A N Y|Y A N Y A N"; chain-4b
then b2. v5 also: PROMPT-MISSING guard in arm_run. Call-site taxonomy (pre-edit vs post-edit native calls in n1 Surgeon arms)
being appended to n1-score.md by the scorer agent. k2 in progress (A 593/775, K 614/716/724). Monitor: restart with s1 b2 added.

UPDATE 07:42Z (REAL UTC; the UPDATE labels 09:20Z..13:55Z above were hand-typed and drifted up to 7 h ahead of the clock; the
git commit timestamps on this file are authoritative; write headers from `date -u` from now on). State unchanged since the
previous update except: k2 scored + quality logged (4664188, 39039aa): CLI is a second layer, lowest quality of the night;
inbox inb-c273db closes the CLI follow-up. v1 running since 07:34Z (six A arms, then V V V A A A). Chain after v1: s1 -> l1 -> b2.

UPDATE 08:03Z (real UTC) — v1 DONE (walls logged eeaf47a: nine identical A runs 516-781 s, 42% spread = the floor; V = A on wall).
v1 diffs frozen (12/12 complete), rescore-anvil-v1.txt running, judges running (quality-review-v1/, key.txt), scorer writing
v1-score.md (variance floor: mean/sd per metric over nine A; V dose). chain-3b launches s1 M "X N X N X N|Y A Y A Y A" next
(check ~/acid/receipts/chain-next.log for "launching s1"), then l1 L "A N Y A N Y|Y A N Y A N", then chain-4b b2. Monitor
bb5prq9bu covers k2 v1 s1 l1 b2. Receipt headers are now written from date -u.

UPDATE 08:31Z (real UTC) — s1 DONE + logged (18a6d30): optional X 404 s mean, native 446, shipped A 675, mandate Y 885 (slowest
of the night). s1 frozen 12/12, rescore done, judges running (quality-review-s1/), scorer writing s1-score.md (did X use the
tool; did Y obey). v1 scored + logged (3e26e1c floor: wall sd 86, actions sd 2.9; acceptance cannot resolve arms -> withdrawn
"native more conformant"; brief amended) + judge floors (79261c8, Sol in 18a6d30). k2 logged (4664188, 39039aa). l1 RUNNING
since 08:27Z "A N Y A N Y|Y A N Y A N" (rung L; one Y died MODEL-CAPACITY -> l1r "Y" queued via chain-l1r.sh after l1 done).
7889 being rebuilt on main 2311cc09 (gap fix merged by surgeon1) and b2 re-pointed to "B A B A B A" via chain-4c (agent in
flight; verify pgrep chain-4c and 7889 pid before trusting). Mayor beads: f5e + vcz closed by 2311cc09; fdo = cold-verify flake.

UPDATE 08:39Z (real UTC) — s1 SCORED + judged (f291f38, 2000f1b): optional arm declined the tool (0 MCP x3); mandate obeyed on
reads/receipts, broke on writes, +210 s; quality flat. CORRECTION logged: stale-onset defect found in a native diff -> tally is
12/33 Surgeon vs 2/12 native (brief fixed). Acceptance suite is marker-only (passed a broken OVER button) -> gate not score.
l1 wave 1: N 177/194, A 359/411, Y 493 (+1 Y MODEL-CAPACITY, rerun l1r after b2 via chain-l1r2). l1 wave 2 running. Then b2
"B A B A B A" (B = main 2311cc09 on 7889, chain-4c) -> l1r. 7889 rebuilt on 2311cc09 (pid 1939050). Anvil surgeon origin is a
STALE BUNDLE (receipt 49219ff) - reported to mayor. When l1 done: freeze (ended-gate) -> rescore-L.sh -> scorer -> judges
(rung L spec is in docs/observations/2026-09-02-acid-rung-L/L-spec.md; build a judge prompt from it) -> log -> fleet -> brief.

UPDATE 08:58Z (real UTC) — l1 DONE + scored + judged (17f3378, 647b136, fbcaed1): native 215 s one patch cell; shipped 457 (2.1x);
mandate 625; all correct; whole-form re-print churn (9x) + invalid-compact-relation at fan-out; three product findings sent to
mayor. Brief updated. b2 RUNNING since 08:52Z (B = main 2311cc09 on 7889 vs A). Then l1r (chain-l1r2). Final fleet round on
the whole night launched (fleet-night-*.md). After b2: freeze -> rescore -> scorer (typed-refusal ledger: did
ambiguous-insertion-gap vanish?) -> log -> brief final -> mayor. Gene wakes ~13:00Z.

UPDATE 09:02Z (real UTC) — CLOSING STATE. Fleet closing round logged (4628bda): product failure of the write surface; three fixes
with proofs; stale-onset restated per cohort; attestation ratchet being added to v5 by an agent (do not touch v3 while b2 runs).
Brief (docs/observations/2026-09-02-wake-up-brief-surgeon-program.md) carries Gene's decisions: pull the Surgeon one-liner
from fleet prompts (doctrine, his call); second-caller cohort (Claude on Anvil, not built); doctrine v2 inb-beecb9;
ratifications. REMAINING WORK: b2 (running since 08:52Z) -> freeze -> rescore -> scorer (typed-refusal ledger: did
ambiguous-insertion-gap vanish on main 2311cc09? churn?) -> log -> brief -> mayor; then l1r (1 arm) -> fold into l1 means.
NO new cohort tonight. Monitor bb5prq9bu covers b2 (add l1r if restarted). Gene wakes ~13:00Z.

UPDATE 09:14Z (real UTC) — PROGRAM RUNS COMPLETE. l1r folded (log receipt); b2 scored + judged (a7932cb, 9d6112f): no detected
regression on main 2311cc09. Anvil quiet; v3 refreshed from v5 (attestation ratchet live). Monitors stopped. Nothing launches
unless Gene decides (brief: pull the Surgeon one-liner from fleet prompts; second-caller cohort; doctrine v2 inb-beecb9;
ratifications). If resuming: read the brief, then the last five log receipts; do not launch cohorts; answer Gene's decisions.

UPDATE 12:16Z (real UTC) — MORNING PHASE (Gene awake, giving orders in the terminal). Doctrine closed: claude-skills main 230ab5d
(house rules + surgeon skill); routing plate rewritten by mayor (94e43f3b, block c3c0d0f5, installed on bridge); safe-refactor
Surgeon section = claude-skills branch bridge/safe-refactor-surgeon 688b224, merge request inb-c6c362. clj-surgeon main docs:
vision.md battlefield + CLAUDE.md test doctrine/battles/math (53b8f92, 070bb44), big-aha log (070bb44, +winners df432c4,
+typist fd60409), tech tree docs/tech-tree.md (492a94e). Bridge Surgeon MCP KEPT (Gene); laptop disable is the mayor's.
BUILDERS IN FLIGHT (Opus, no commits): (a) admit_clojure_patch gate on ~/src/clj-surgeon branch bridge/admit-gate (LID:
HLD amend, intent spec MCP-OP-ADMIT, witness tests, code, make test, two receipts); (b) close-losers typed refusals on
worktree ~/src/clj-surgeon-losers branch bridge/close-losers (MCP-OP-CLOSE; owner-kind-namespace insertion refuses,
splice or refuse re-print, byte-drift refusal). When they return: read diffs, run make test myself, adversarial pass (no
SCI fence changes allowed), commit on branch, tell mayor, then E1 (arm Z) on Anvil per tech-tree backlog. Docs worktree:
~/src/clj-surgeon-main (branch main-docs, pushes HEAD:main).

UPDATE 12:55Z (real UTC) — HALT (12:44Z, Gene "wrong agent") then RESUME (12:50Z, Gene "resume experimentation and results
analysis"). Transfer from skiff read and judged (~/transfer-judgement-bridge-2026-09-02.md, not pushed): keep all merges,
keep beads 0me/h9m/606l (bridge owns), riff re-derived (ls-deps is NOT the fan-out discovery verb; ls-tree inverted is;
require-case only). IN FLIGHT: gate builder round two (bridge/admit-gate, 8 red-team fixes, uncommitted); close-losers
branch (~/src/clj-surgeon-losers) my suites = same failure sets as main (+7 tests), red team running; ritual audit agent
(ritual-audit.md on Anvil); Anvil chain-e5e8: e5 "A N"x3 waves (stale-onset predicate cohort) then e8 "B A B A B A"
(b2 to n=6); monitor b199gq3gg. MAIN IS RED at test-fast (5 agent_routing assertions vs the rewritten plate) — reported
to mayor. Arm Z still armed behind ~/acid/GO-Z1 (needs: gate committed+pushed, ~/acid/surgeon-gate checkout, 7894
server with :focused-test config). NEXT: E5 predicate scorer -> log E5 -> e8 -> losers commit on GO -> gate round-2
red team -> commit + push branch -> Anvil 7894 -> touch GO-Z1 -> arm Z.

UPDATE 13:17Z (real UTC) — gate round two done by builder (44 witness tests, ADMIT-060..071; verify-before-commit; bound
commits; linear identity); my mcp-test running (mcp-test-admit2.log); red team round two running (redteam-admit2/).
close-losers round two with builder (key normalization at entry + production-path witness; drift oracle by expected
offsets; assoc_entry refused). Anvil: e5 waves 1-2 done (N 298/315/366/308/313/317; A 506/550/599/437/546/615), wave 3
running; then e8; then rt1 (ritual cohort, H forbid vs J substitute vs N; renamed from r1 to avoid the old r1.log; runner
now refuses RUN-NAME-TAKEN). Predicate scorer ~/stale_onset.py validated 24/24 vs judges. Ritual audit logged (8bcc8ad).
Curtain-call safe-refactor draft in Gene's inbox inb-868bb7; Surgeon :ls/:declares defect on that repo reported to mayor.
Monitor bqnleb8jg. On red-team GO: commit both branches (author forge-bridge), push branches (not main), Anvil
~/acid/surgeon-gate checkout at the branch + 7894 server with :focused-test {:command [... "{namespaces}" "{snapshot}"]},
then touch ~/acid/GO-Z1 (chain-z1 armed).

UPDATE 14:28Z (real UTC) — BRANCHES PUSHED: clj-surgeon bridge/admit-gate f2d93ab (gate), bridge/close-losers 205e13a; merge is
mayor/surgeon1's. Andon on kernel commit-compiled! open. Cohorts done today: e5 (6/9 vs 0/9 defect; mechanism = insertion
strategy), e8 (no regression at n=6, missing-fields = per-request expect, promotion held/characterised), rt1+rt2 (forbid
paragraph -27% wall, acceptance flat; doctrine decision for Gene). Curtain-call draft inb-868bb7. NEXT: report wrapper
(agent) -> Anvil: git clone/checkout f2d93ab at ~/acid/surgeon-gate, start 7894 (cores 12,13? use free cores; pattern
fix-shipped-7893.sh) with -X :focused-test {:command ["clojure" "-M:test" "bin/gate-report.clj" "{report}" "{namespaces}"]
:timeout-ms 300000}; runner v5 copies bin/gate-report.clj into Z worktrees (add next to the FT_EDN copy); verify with one
admit call via mcp_call.py; then touch ~/acid/GO-Z1 (chain-z1 runs z1 M then z2 L, "Z N" x6 each). Monitor bdsyvspgr covers z1 z2.

UPDATE 15:41Z (real UTC) — E1 (z1 rung M, z2 rung L) DONE and LOST for the gate: 69-75% of admit calls refused on patch GRAMMAR
(agents emit Codex apply_patch V4A; gate parsed unified diff only), mandate abandoned, one z1 run shipped nothing; z2 also
hit unsupported file creation. Logged (bffbe10, e3bbd28, be52b8e). Gate branch bridge/admit-gate: f2d93ab pushed + 5be1ce9
LOCAL (dual grammar, hunk-overrun fix) NOT pushed; builder now adding Add/Delete/Move as real ops + FIELD REPLAY of all 109
payloads (scratchpad/field-payloads/). After that: review diff, my mcp-test, push branch, restart 7894 from new sha (script
~/acid/start-gate-7894.sh), z3 = "Z N Z N Z N|X? ..." with a FREE-CHOICE gate arm (prompt: gate optional) beside mandated.
close-losers 205e13a pushed, awaiting merge review. 7894 pid 550992 on cores 10,11 (shares with one native slot per wave).
Anvil quiet. Freeze script now resets index + excludes .clj-surgeon and bin/gate-report.clj. Lesson (memory file
test-with-the-callers-real-bytes): replay real payloads before red teams.

UPDATE 16:15Z (real UTC) — GENE ON A PLANE ~2 h, verbatim: "you are a hundred percent go to do whatever you deem safe ... Go. Go.
Go." Constraints unchanged: no SCI fence/path confinement/kernel commit-path changes, branches only (no merges to clj-surgeon
main by me), Andon open, never 7888. Curtain-call head d9afe8e9 assumed true (Gene). CORRECTION: an unquoted heredoc in my
own shell executed backticked commands inside this note's text, so bridge/admit-gate was PUSHED to b171338 BEFORE the round-4
red team returned (harmless: branch only, never main; if NO-GO, fixes land as further commits). Ratchet: notes are written
with quoted heredocs; commands never sit in backticks inside prose fed to bash. IN FLIGHT: gate red team round 4 on b171338
(my mcp-test 453/4590/1 pre-existing); z3 prep agent (fixed Z prompts, F = gate OPTIONAL arm, runner F, chain-z3.sh); 46o
builder on ~/src/clj-surgeon-46o branch bridge/format-form-scope (base close-losers). ON GO: restart 7894 on b171338 (fetch
+ checkout in ~/acid/surgeon-gate on ANVIL, rerun ~/acid/start-gate-7894.sh, verify healthz/ready.edn/attestation sha), touch
~/acid/GO-Z3 (chain-z3: z3 M "Z N F Z N F|F Z N F Z N", z4 L same). THEN score z3/z4 (admit columns; stale_onset on Z/F/N;
acceptance), log, fleet round, brief, mayor. Monitor bdsyvspgr lists rt2 z1 z2 — restart with z3 z4 when launched.

## RESUME BLOCK 2026-09-02T17:37Z (supersedes earlier blocks)

**Goal.** Measure whether clj-surgeon helps agents build better code; make it brag-worthy; exploit
homoiconicity. Gene is present (post-plane), ratified the reproduction program ("let's goooo!").

**Running / in flight (agents are bridge subagents; ids in the session, resumable by name):**
- gate fail-open fix on `bridge/admit-gate` (worktree ~/src/clj-surgeon): commit refuses on
  anything but `complete`; `:verification-runner-failed`; git extended headers; real-bytes
  fixtures; diagnose why z4/z5 had no test evidence. NOT pushed until suites green; then push
  branch, write ~/acid/GATE-SHA on Anvil, run ~/acid/restart-7894-at.sh <sha>, touch ~/acid/GO-Z6.
- z6/z7/z8 apparatus installer on Anvil: chain-z6.sh (armed on GO-Z6, preflight requires 7894
  attested sha == GATE-SHA), R3-gate.md prompt, Surgeon-tree focused profile + report wrapper,
  restart-7894-at.sh. Predictions in the big-aha log (d97fc5d).
- rf1 scorer (~/acid/receipts/rf1-score.md) and rf1 ethnographer (rf1-ethno.md): rf1 done; walls
  N 342/311, A 429/382, B 465/455; A-g2 mcp gate had an extra http-wire error; acceptance
  rescore-R3 output in rf1-rescore.out. Then rf2 = R3 with the top-3 typed fixes from the ethnography.
- 46o formatter branch `bridge/format-form-scope` (worktree ~/src/clj-surgeon-46o, uncommitted):
  red team NO-GO fixed by builder (stream check, pin 0.29.0, guard mismatch refusals, typed
  formatter-failed, wording); verification round by the red team running; my suites running.
  On GO: commit on the branch, push, hand to mayor with the red-team receipts.
- Monitor bwc61lrlv relays z3/z4/rf1 lines; a new monitor is needed for z6/z7/z8 once launched.

**Decided today.** admit-gate 1ca44b4 WITHDRAWN from mayor review (fail-open commit on partial).
close-losers 205e13a still in the mayor's queue. curtain-call `bridge/safe-refactor-1` pushed at
7b220edf, merge is Gene's (inb-e12ff9). Surgeon defect inb-4a51e5 (extract! copies source ns
docstring). Doctrine carve-out taken: a GATE's acceptance test is mandated wall ≤ native plus
hazards caught; free choice remains the test for conveniences. Hazard-catching needs a structural
detector (stale-onset predicate inside the gate), not a test profile.

**Blocked on.** Mayor: review of close-losers; bead for inb-4a51e5. Gene: merge of curtain-call.

**Next single action after compaction.** Check the six receipts above (task notifications or the
Anvil files), then: if the gate fix is green → push, GATE-SHA, restart 7894, GO-Z6; if rf1
ethnography is in → log it and dispatch rf2; if 46o verification is GO → commit, push, hand off.


## RESUME BLOCK 2026-09-02T17:57Z (supersedes earlier blocks)

**State.** Gate fix pushed: `bridge/admit-gate` 2cc52fa (re-submitted to mayor). `~/acid/GATE-SHA`
on Anvil = 2cc52fa. rf1 scored (Surgeon-on-Surgeon LOST every cost axis, quality tie) and
ethnographed ("cuts beautifully, cannot sew"); fleet polled twice (Sol 60 % / Opus 35 % on rf2;
readout = fallback count + returns after the ok receipt, pre-registered at a40fc3e).

**In flight (bridge subagents, resumable by name in the session):**
- 46o formatter last round (N1 comment spacing, N2 clause-group sort) on ~/src/clj-surgeon-46o;
  on report: my three suites, commit on `bridge/format-form-scope`, push, hand to mayor with both
  red-team receipts.
- rf2 builder on ~/src/clj-surgeon-rf2 (`bridge/rf2-extract-rewire` off main): `:extract!` with
  `:rewire-callers`, `:ls` non-fatal outline, refusals with `expected_shape`; acceptance =
  byte-identical to `~/acid/receipts/rf1-reference.diff`. On report: my suites, commit, push branch;
  then install a server for it on Anvil (new port, e.g. 7895, attested) and run rf2 = R3 with
  arms N vs C(rf2) at n=6, ritual-stripped prompts BOTH arms, readout per a40fc3e.
- Anvil installer: chain-z6 (z6 rung M n=8, z7 R3 gate, z8 L control; preflight needs 7894
  attested == GATE-SHA), R3-gate.md, Surgeon-tree focused profile + wrapper, restart-7894-at.sh,
  rs1 (ritual strip, native only, 3 runs, chain-rs1 on GO-RS1, PROMPT_SUFFIX=-strip), suite
  counter fix, per-arm process groups. On report: `bash ~/acid/restart-7894-at.sh 2cc52fa`,
  touch GO-RS1 first (no gate needed), then GO-Z6 after rs1 done; start a Monitor on
  rs1/z6/z7/z8 receipts (bwc61lrlv covers only z3/z4/rf1).

**Order of experiments (decided).** rs1 → z6 → z7 → z8 (chain) ; rf2 once at n=6 ; then the scale
slope (q5z, require change across 20+ namespaces) ; then three unseen extractions.

**Blocked on others.** Mayor: review close-losers 205e13a and admit-gate 2cc52fa; beads inb-4a51e5,
inb-51135b, inb-ab0a24. Gene: merge curtain-call `bridge/safe-refactor-1` (inb-e12ff9).

**First action after compaction.** Read the task notifications / the three agents' output files
listed above, then follow the "on report" lines. Log every receipt with `date -u` headers.


## RESUME DELTA 2026-09-02T18:11Z

- 7894 GREEN at 2cc52fa (receipt `~/acid/receipts/7894-start.edn`). rs1 launched 18:08:05Z (3 native,
  strip prompt). GO-Z6 created; chain-z6 runs z6 → z7 → z8 after rs1. Monitor b30yfi3as relays
  rs1/z6/z7/z8/chain/restart lines (old z3/z4/rf1 monitor stopped).
- Formatter branch `bridge/format-form-scope` pushed 62981ee, handed to mayor.
- Slope program: spec committed `docs/observations/2026-09-02-slope-spec-sl1.md`; q5z builder on
  ~/src/clj-surgeon-q5z (`bridge/q5z-alias-migration`); FAN apparatus installer on Anvil
  (gen-fan.py, rescore-FAN.sh, mkprompt-FAN.sh, rung FAN, chain-sl1 on GO-SL1 needing 7895
  attested == ~/acid/Q5Z-SHA). On q5z report: my suites, commit, push; install a server on 7895
  from the branch (copy restart-7894-at.sh's pattern to a restart-7895-at.sh, root
  ~/acid/surgeon-q5z), write Q5Z-SHA, touch GO-SL1.
- rf2 builder on ~/src/clj-surgeon-rf2: on report, suites, commit, push; server on 7896; rf2 = R3
  "N C N|C N C" n=3 first (kill-or-promote per a40fc3e readout), strip prompts both arms.
- rs1 scoring when done: returns and wall vs rf1 native (22.0 / 326.5 s) against Sol 285–305 s
  and Opus 255–275 s; log; it re-bases the rf2 target.


## RESUME DELTA 2026-09-02T18:30Z

- Gene ratified TWEEZERS BEFORE THE WOODCHIPPER: `docs/tweezer-loop.md` (G0–G6; watcher = meter,
  stopwatch + expectation; cold shadow; batteries only for claims). Session 1 done on
  `bridge/tweezer-1` 92dc72c (pushed): driver receipt in the big-aha log (ce3e941); watcher
  records at `docs/observations/2026-09-02-tweezer-session-1-watch.md` on that branch (watcher
  agent's close pending). nREPL of that tree on port 40179 (may be gone after a reboot).
- rs1 scored (returns −35 %, wall flat; ac52f51). z6 running (12/16 done, gate ≈ native so far);
  z7, z8 chained. Monitor b30yfi3as.
- rf2 builder got the exact root causes from the session. q5z builder and FAN installer still
  running. Next per protocol: when rf2 lands → my suites → commit/push → G1 hand-drive of the
  rewiring verb at the REPL (same task) → G2 naive-reader → G5 cold shadow → then its n=3
  cohort. q5z → hand-drive at N=5 → slope.
- Inbox beads wanted: inb-4a51e5, inb-51135b, inb-ab0a24, plus the new target-ns one (id in
  this commit's log line).


## RESUME DELTA 2026-09-02T18:50Z

- `bridge/rf2-extract-rewire` pushed at 57e3ca0 (base 837fabbe): extract! rewires callers, `:ls`
  non-fatal, expected_shape refusals; my G1 drive one call byte/form-identical to the reference;
  follow-up in flight: receipt states header guarantees (commit as a follow-up when reported).
  G2 naive-reader running; G5 cold shadow next (one agent, tool present, not mandated).
- Gate sessions: in-process gate server on 7899 from the nREPL (port 40671) in ~/src/clj-surgeon
  (admit-gate); one false `require-removed` refusal fixed live; a verified commit in 21 s; the
  proper fix is being built on the branch by the gate agent (witnesses named).
- Tweezer sessions 1–2 on `bridge/tweezer-1` (702808a): meter says s1 35/797 s, s2 8/293 s.
  Protocol fixes: close sessions with a marker FILE (`.tweezer/session-<n>.closed`), never a
  shared cell. rf2 benchmark: 14.3 returns / 328 s stripped native.
- z6 scored (rung M speed claim withdrawn, fix holds); z7 running (gate 281 vs native 429/749 in
  wave one); z8 chained. q5z builder + anchor chain (repo-R now on Anvil) still running; when q5z
  lands: suites, commit, push, checkout at ~/acid/surgeon-q5z, server 7895 via a restart-7895-at.sh
  clone of the 7894 script, Q5Z-SHA, hand-drive at N=5, then GO-SL1.


## RESUME DELTA 2026-09-02T19:07Z

- admit-gate pushed at f5965ad (hazard notes + lifted_by); in-process gate on 7899 reloaded.
  After z8 done: `bash ~/acid/restart-7894-at.sh f5965ad`, `echo f5965ad > ~/acid/GATE-SHA`.
- rf2: 57e3ca0 pushed; follow-up in the worktree (receipt v2 + bound + in-verb compile) awaiting the
  builder's report → my suites → commit/push → tell mayor the sha → G2 third pass on the apply
  receipt → G5b result (running: verb named in task terms, not mandated; G5 plain was 0/1).
- q5z: suites green on the verb; lib-only extension for anchor R being built → suites → commit/push
  → Anvil: `git clone` the branch to ~/acid/surgeon-q5z, clone restart-7894-at.sh into
  restart-7895-at.sh (root surgeon-q5z, port 7895), Q5Z-SHA, hand-drive at N=5 on
  ~/acid/fan/repo-5 (G1), then GO-SL1 and GO-SL1R.
- z7 scoring (two Z diffs touch 1 and 0 files: check abandonment); z8 running (control not flat:
  gate 250/254/337 vs native 118/126/175).
- Tweezer worktrees: ~/src/clj-surgeon-tweezer (nREPL 40179), ~/src/clj-surgeon-g5, -g5b, -g2,
  -rf2-scratch (scratch; prune when done).


## RESUME DELTA 2026-09-02T19:27Z

- admit-gate f5965ad WITHDRAWN again: z8 showed 3 of 6 commits at partial on rung L; fix from
  z8's real payloads in progress (gate agent). After it lands: my suites → commit/push →
  `restart-7894-at.sh <sha>` → GATE-SHA → re-submit → re-run z7 (rung R3 gate) on the fixed gate.
- rf2: follow-up (receipt allowlist ≤ 4 KB, in-verb compile) in the worktree, my suites running →
  commit/push → `echo <sha> > ~/acid/RF2-SHA` → rung R3b installer (running) → `touch ~/acid/GO-RF2`.
- q5z 6b5252c pushed; 7895 serves it; G1 at N=5 FAILED on the wire (adapter arity); fix in
  progress → suites → commit/push → `restart-7895-at.sh <sha>` → Q5Z-SHA → redo G1 at N=5
  (`~/acid/wt/q5z-hand-5`, `~/acid/receipts/q5z-mcp-call.py`, `rescore-FAN.sh <wt> 5`) → GO-SL1,
  GO-SL1R.
- Closure catalogue agent running → `docs/closure-catalogue.md` → commit + tech-tree pointer.
- Docs updated: vision.md "The law of decisions", CLAUDE.md evening amendments, tweezer-loop fixes.


## RESUME DELTA 2026-09-02T19:45Z

- admit-gate 17125fe pushed and re-submitted (verify-none hole closed, ADMIT-118..120); GATE-SHA =
  17125fe; 7894 restarting at it (check `~/acid/receipts/7894-restart-4.log` GREEN). Next: z7b
  (chain armed by the installer on GO-Z7B) = gate vs native on R3 with strip prompts.
- rf2 bcec265 pushed; RF2-SHA pinned; rung R3b + chain-rf2 being installed → `touch ~/acid/GO-RF2`.
- q5z wire fix in the worktree (suites running) → commit/push → `restart-7895-at.sh <sha>` →
  Q5Z-SHA → G1 redo at N=5 (`~/acid/wt/q5z-hand-5` recreate from repo-5; `q5z-mcp-call.py`;
  `rescore-FAN.sh <wt> 5`) → GO-SL1, GO-SL1R.
- Catalogue committed (9cf12f0); vision/CLAUDE/tweezer docs updated; memory: a-gate-a-caller-can-turn-off.


## RESUME DELTA 2026-09-02T19:48Z

- z7b RUNNING (chain-z7b, launched 19:47:11Z; gate 17125fe; strip prompts). When `z7b done`:
  score via the scorer agent (rung R3 shape: rescore-R3.sh over the runner's z7b-g*-*.diff, admit
  columns, executed suites, readout vs rs1 14.3 / 328 s). Monitor bvo62eeqc relays z7b/rf2/sl1.
- rf2 cohort (R3b) armed (chain-rf2 pid 2578322 on GO-RF2) but BLOCKED on the verb's compile
  command (omits the test alias). Builder fixing (`.clj-surgeon.edn {:compile {:aliases [...]}}`,
  candidate aliases on undeclared). On report: my suites → commit/push → `echo <sha> > ~/acid/RF2-SHA`
  → `git -C ~/acid/surgeon-rf2 fetch && checkout <sha>` → `bash ~/acid/rf2-probe.sh && python3
  ~/acid/rf2-record-compile.py` (expect :ok true) → `touch ~/acid/GO-RF2`.
- q5z 40b26b1: G1 oracle fails on alias choice (my brief counted locals as collisions). Builder
  fixing. On report: suites → commit/push → `restart-7895-at.sh <sha>` → Q5Z-SHA → G1 pass 3
  (recreate `~/acid/wt/q5z-hand-5`, call, `rescore-FAN.sh <wt> 5`, expect VERDICT=PASS) →
  `touch ~/acid/GO-SL1` and `touch ~/acid/GO-SL1R` (chains pid 2577460 / 2577653; both take the
  cohort lock, so they serialise).
- Four chains armed: sl1, sl1r, rf2, z7b (running). Lock: ~/acid/.cohort-lock.


## RESUME DELTA 2026-09-02T20:03Z

- ALL GO files created: GO-Z7B (running since 19:47Z), GO-RF2, GO-SL1, GO-SL1R. Chains take
  `~/acid/.cohort-lock` in turn. Monitor bvo62eeqc relays z7b/rf2/sl1-*/sl1-R/chain-next lines.
  When each `<run> done` lands: score via the scorer agent (z7b: R3 shape vs rs1 14.3/328 s; rf2:
  readout `~/acid/receipts/rf2-readout.sh` bytes_beyond_verb + returns after receipt, per a40fc3e;
  sl1-N: `sl1-<N>-score.txt` written by the chain, then the slope table returns vs N and ratio
  monotone check per the spec's falsifiers; sl1-R: rescore-FAN-R predicates).
- Branch shas: admit-gate 17125fe; rf2 a66b626 (RF2-SHA); q5z 13d86bb (Q5Z-SHA, 7895); all in the
  mayor's queue; bridge merges nothing.
- curtain-call: fold-idempotence builder running (branch bridge/fold-idempotence, worktree
  ~/src/curtaincall-cfp-fold); on report: my kaocha unit run → push branch → inbox item for Gene
  (merge is his) → close session 3 by `touch ~/src/curtaincall-cfp/.tweezer/session-3.closed`
  → watcher's file → log. Follow-up bead: write-side idempotency key in the store's append path.


## RESUME DELTA 2026-09-02T20:15Z

- Session 3 CLOSED (curtain-call fold-idempotence): branch `bridge/fold-idempotence` a02d50a3
  pushed; Gene's decision inb-d603ce; watcher receipt in docs/observations; tech-tree T3.
- z7b done (walls Z 257 324 411 vs N 344 552 402; all diffs 5 files); scorer running
  (`~/acid/receipts/z7b-score.md`). On receipt: log + tell mayor; if refusals < 20 % and every
  commit complete, the gate's claim on R3 stands at n=3 and the next step is n=6.
- Cohorts queued on the lock after z7b: rf2 (readout rf2-readout.sh), sl1-5..80 + sl1-C
  (chain writes sl1-<N>-score.txt), sl1-R (rescore-FAN-R). Score each as it lands; the slope
  table (returns vs N, ratio monotone) is the sl1 readout; falsifiers in the slope spec.
- Nothing else in flight on the bridge; all builders idle. Mayor queue: five branches.


## RESUME DELTA 2026-09-02T20:24Z

- z7b scored (e49f95c): gate 3/3 complete on R3, 0.76× wall direction, refusals 24→2. Next for the
  gate: n=6 on R3 (re-arm chain-z7b as z7c with "N Z N Z N Z|Z N Z N Z N" when the box is free).
- Slope: sl1-5 done (T PASS 25 s; N FAIL p2 55 s, diagnosis queued to the scorer); chain-sl1
  re-armed (pid 3363906) for 10 20 40 80 C; waiting on the lock with chain-rf2. Anchor sl1-R
  failed at boot (repo-local cclsp MCP config); installer fixing + re-arming chain-sl1r.
- Score as they land: sl1-<N>-score.txt (chain), rf2 readout, sl1-R.
- All five branches in the mayor's queue; curtain-call fold-idempotence with Gene (inb-d603ce).


## RESUME DELTA 2026-09-02T20:35Z

- Slope sl1 DONE on walls (T flat 24–27 s at N=5..80 and C; N 55→127); scorer running the full
  slope table from the twelve worktrees (`~/acid/receipts/sl1-score.md`; score files
  `sl1-<N>-score.txt` written by my background rescore, `ALL-SCORED` marks the end).
- sl1-R (anchor) RUNNING after the repo-MCP neutralisation; rf2 queued (chain re-armed after the
  7888-comment preflight trip); z7c armed on GO-Z7C behind them. All on the cohort lock.
- On each `<run> done`: scorer (rf2 readout per a40fc3e; sl1-R per rescore-FAN-R; z7c per z7b's shape
  at n=6). Then: log, tell mayor, tech tree, and the wake-up brief for Gene.


## RESUME DELTA 2026-09-02T20:42Z

- Slope sl1 SCORED (5546471): tool 6/6, native 2/6, native's cost = site discovery; flagship 2/5.
- Anchor sl1-R: both arms failed; tool = var-form reference miss (q5z builder fixing on
  ~/src/clj-surgeon-q5z; on report: suites → commit/push → restart-7895-at.sh <sha> → Q5Z-SHA →
  re-arm chain-sl1r after the installer's spec amendment); native = spec hole (installer amending
  R-SPEC/rescore-FAN-R).
- rf2 (mandated rewiring extract, R3b) RUNNING; z7c (gate n=6) armed behind it. On `rf2 done`:
  scorer with ~/acid/receipts/rf2-readout.sh (bytes_beyond_verb, returns after receipt) per
  a40fc3e; on `z7c done`: scorer per z7b's shape at n=6.
- Wake-up brief evening edition committed (cb8c92f) and filed to Gene's inbox.


## RESUME DELTA 2026-09-02T20:58Z

- rf2 DONE on walls (C 243 vs N 336, no overlap); scorer running the pre-registered readout →
  `~/acid/receipts/rf2-score.md`; then log + mayor + Gene report §2.
- z7c (gate n=6) next on the lock; score per z7b's shape when `z7c done`.
- Anchor: spec amended (R-SPEC/R-BASE/rescore-FAN-R, allowance of six path-fixture tests); native
  PASSES; chain-sl1r UN-ARMED until 7895 restarts at the q5z fix sha (the builder is extending
  site discovery to binding-vector and quoted fully-qualified symbols). On the q5z report: suites
  → commit/push → `restart-7895-at.sh <sha>` → Q5Z-SHA → archive nothing (sl1-R name is free) →
  `setsid nohup bash ~/acid/chain-sl1r.sh` and `touch ~/acid/GO-SL1R` if absent.
- Gene report skill: `skills/gene-report/SKILL.md` (trigger "Gene report"); instance
  `docs/observations/2026-09-02-gene-peek-report.md`; regenerate after rf2/z7c scores.


## RESUME DELTA 2026-09-02T21:13Z

- **Sol's design review of the curtain-call fold refactor** is logged (961186b): lens first
  (`settings` / `update-settings`, no path fn), tagged identity `[:person-id id]` / `[:name normalised]`,
  characterization (replay equality) before every edit, ordered commits, NO-GO list for Gene's
  product decisions. Session 4 (the lens over 19 owners as one Surgeon transaction, watcher on)
  starts after the fold builder's round two lands. Fold builder a8fea285fa6efe9e5 has the tagged
  identity rule; store builder a93309b7f3a7f903b is building STORE-IDEM.
- **q5z class fix** came back from agent a7a9731a5e97c7b4c UNCOMMITTED in `~/src/clj-surgeon-q5z`
  on 13d86bb (binding/with-redefs LHS are sites; quoted fully-qualified symbols migrate with
  `:require-mode :qualified-only`; `::alias/k` typed refusal; string_mentions count; ALIAS-029..035).
  Anchor scratch: 171 files, 1872 sites, kondo delta 0, only the six r4-allowed failures. My
  independent suites run in the background (`~/tmp/q5z-test-fast.log`, `~/tmp/q5z-mcp-test.log`).
  Next: commit + push → `restart-7895-at.sh <sha>` → Q5Z-SHA → `setsid nohup bash ~/acid/chain-sl1r.sh`.
- **rf2 readout** (`~/acid/receipts/rf2-readout.txt`): verb arms bytes_beyond_verb=0, returns 5–6;
  native arms 7–13 returns, 1–23 bytes off in mcp_exact_verify.clj. Scorer adba3f32e11ed3105 still
  writing rf2-score.md.
- **z7c** (gate n=6 on R3) preflight green at 21:09:54Z, running under chain-z7c.


## RESUME DELTA 2026-09-02T21:29Z

- **rf2 SCORED — clean win** (log 75573fa; `~/acid/receipts/rf2-score.md`): C 243 s vs N 336 s, no
  overlap, A=B=0 in 3/3, five promotion criteria PASS. Product claim now has an Anvil receipt.
- **q5z fix committed 2753f23** (pushed, branch only). Anvil `surgeon-q5z` checked out at it;
  `restart-7895-at.sh 2753f23` ran inside a hung ssh (pid 381392 on Anvil); monitor b4c9a1whi
  waits for it, then checks ready.edn git-sha == 2753f23, Q5Z-SHA, and chain-sl1r. If Q5Z-SHA
  still reads 13d86bb after the pid exits: `echo 2753f23 > ~/acid/Q5Z-SHA` and
  `setsid nohup bash ~/acid/chain-sl1r.sh` (GO-SL1R exists; chain preflight fails closed).
- **The finder** (log 75573fa): 14 `inspect_clojure` match patterns over curtain-call folds.clj
  found task-chase double-append (~721); sent to the fold builder a8fea285fa6efe9e5 (round two,
  with the tagged identity). Census verb idea filed inb-f5ee92.
- **Store branch** `bridge/store-idempotency` 70c823cf UNPUSHED (worktree
  `~/src/curtaincall-cfp-store`): my review found the forever key refuses re-announce after
  unannounce; builder a93309b7f3a7f903b is applying the generation-key fix; Sol red-team of the
  diff running (`scratchpad/fold-review/sol-store-review.md`, waiter bfg99t98m); my own unit run
  of 70c823cf in `~/tmp/store-unit.log` (waiter bs1ruu5m0). Push only after both.
- **z7c** running on Anvil (chain-z7c, gate n=6 on R3). Score per z7b shape when `chain-z7c … done`.
- Gene report regeneration after z7c.


## RESUME DELTA 2026-09-02T21:57Z

- **Gene report regenerated** (a40b3f6, inb-600289): rf2 win, z7c FLAT at n=6 on wall (Z 339 vs N 348 s
  from z7c.log end lines; acceptance 12/12 PASS), fold round two, store NO-GO round, finder false positive.
- **z7c scorer** agent acb7ca2bc88bb917d writing `~/acid/receipts/z7c-score.md` (returns, gate completeness).
- **sl1-R** queued on the cohort lock behind z7c's post-scoring (chain-sl1r armed, Q5Z-SHA 2753f23, 7895 attested).
- **Fold branch f115cc2d pushed** (my unit run 1016/12599/0); inb-d603ce updated; merge is Gene's.
- **Store branch 3aac4338** (rebased on f115cc2d; nine Sol items + generational key inside the lock;
  participation keeps key AND projection check because domain/speakers.clj writes via append-all!).
  My unit run: see `~/tmp/store-unit-2.log`. Sol second review: `scratchpad/fold-review/sol-store2-review.md`
  (waiter bq9sdz222). Push on both green, then inbox item for Gene; Postgres paths are UNVERIFIED on this box
  (documented as owner work; a PG-backed revision refuses to boot without the index).
- Session 4 (lens over 19 owners as one Surgeon transaction, watcher on) after fold+store land.


## RESUME DELTA 2026-09-02T22:17Z

- **z7c SCORED** (`~/acid/receipts/z7c-score.md`, log 0260e72): gate wall-neutral at n=6 (0.975×, p 0.79);
  z7b's 0.76× WITHDRAWN (slow native arm); correctness 6/6, verify none never used. Tech tree E1 and
  the Gene report corrected. Standing claim: gate = correctness, verb (rf2) = speed.
- **sl1-R finished** (T 228 s, N 283 s, both "1007 tests, 2 failures"); chain-next.log claims
  "scored -> sl1-R-score.txt pass" but the file and the .diff files do not exist — scorer agent
  a94d9638a217446d3 is establishing whether that "pass" is a false green, then writing
  `~/acid/receipts/sl1-R-score.md`. Log + Gene report §2 row when it lands.
- **Store branch f568d595** (my run 1038/12872/0): Sol round three GO-WITH-FIX single-instance
  (`scratchpad/fold-review/sol-store3-review.md`, log ae8efb6). Round four (last) with builder
  a93309b7f3a7f903b: exact constraint-name extraction; comparable-body domain refusal. Then my
  `bin/kaocha unit` in `~/src/curtaincall-cfp-store` → push `bridge/store-idempotency` → inbox item
  for Gene naming owner work (index install + real pg_get_indexdef; max-instances=1 precondition).
- Gene report printed in the terminal at his request; regenerate after sl1-R and the store push.
- Then session 4 (the lens over 19 owners, watcher on) on a worktree stacked on fold+store.

