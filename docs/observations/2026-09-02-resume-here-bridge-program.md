# RESUME HERE — bridge seat, surgeon wall-clock-ideal program (refreshed 2026-09-02 05:05Z; context was 88%)

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
