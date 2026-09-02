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
