# RESUME HERE — bridge seat, surgeon wall-clock-ideal program (refreshed 2026-09-02 06:05Z, pre-compaction)

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
