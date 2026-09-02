# RESUME HERE — bridge seat, surgeon wall-clock-ideal program (written 2026-09-02 04:15Z)

Standing orders (memory files): headlines first with A/B/native timings; record every
experiment in clj-surgeon docs/observations/2026-09-02-captains-log-bridge-wall-clock-ideal-program.md
the moment its receipt lands; one unit at a time on a new apparatus, poll every 2 min; JVM or
multi-arm experiments run on Anvil, never Buster. Gene is away (son to college); works to 06:00 PT.

WHERE THINGS ARE
- Anvil: ssh tester@100.66.152.23. ~/acid/{mvr,surgeon-main,surgeon-shipped} clones; runners
  ~/acid-cohort.sh (v1, in use by the running chain), ~/acid-cohort-v2.sh, ~/acid-cohort-v3.sh
  (rung table: R1 surgeon recovery-receipt task, R2 mvr F5 observe-meter task, M medium, S small);
  prompts in ~/prompts and ~/acid-arm-prompt*.md; receipts ~/acid/receipts/<run>.log plus
  <run>-<arm>-{report.md,diff,run.log,kaocha.log,pages.log,PLAN.md}. Surgeon servers: shipped on
  7888 (cores 12,13), wave (main) on 7889 (cores 14,15). Arms: N native, A shipped, B wave,
  C wave without string symbols, P shipped+planning-mode, Q native+planning-mode.
- Running chain (v1, sequential): s4 (C A N) -> s5 (N C B) -> s6 (P Q A) -> s7 (Q P N) -> then
  v3 r1 = R1 real task "N A|B". Monitor task bl4dqd1bp polls every 2 min.
- Scorers (Buster scratchpad): score_acid.py/score_acid2.py (server+client view), phases_acid.py,
  clock_acid.py; blind reviews: quality-review-*/ (key.txt maps impl-N to arm; NEVER capture diffs
  late — use the runner's frozen <run>-<arm>.diff).
- Buster: keep light. 7889 dev instance stopped; wt-surgeon-dev worktree exists on bridge/dev.

RESULTS SO FAR (all correct; seconds): native 243,287,194 | A 243,237,317 | B 303,272,579 |
C 328 | blind quality: native 18,19 ; A 17,17.5 ; B 14.5,15 (consistent ordering).
Findings: wall indistinguishable at this N; within-arm spread 40-300 s; quality ordering
native > A > B; hypothesis under test with C = string-symbol payload hurts edits.

NEXT (in order): review each triple blind as it lands (s3 review running); record s4/s5 with C
in the log; s6/s7 planning-mode arms: compare PLAN.md predicted vs actual; r1 real task: merge the
best correct diff into surgeon main (bridge seat may merge); then queue R2 (one unit at a time);
then small-rung routing/pricing experiment (8 per cell) with v2 runner parallel N/A.
PENDING HUMANS: mayor silent (relay backlog 9); brief filed inb-e6abf8; ratifications for
MCP-OP-INSERT-001..006, string-symbol outline contract, insert_pair design; skiff patches
inb-155f79 (codex-cli yolo), inb-caef1f (git stranded lessons), inb-32bcd1, inb-84ac23 (house rules).
