# Gene report — 2026-09-09 morning (window 03:35Z–08:30Z, the "grind to a genuine 9") — written 2026-09-09T13:12Z

## 1. Performance vs native

| row / arm | tool wall | native wall | ratio | notes |
|---|---|---|---|---|
| Row 5, E4 (6 paired arms, namespace split on the Cell C shape) | 147.2 s median | ~315 s (E-series native control, unchanged) | **0.47** | 6/6 first-attempt accept, 0 refusals, 0 fallbacks |

No other vs-native measurement ran in this window. Say it plainly: the window was apparatus work on t, not new capability cohorts. The E4 wall is the standing vs-native figure for row 5.

**t itself (the thing this window was for):**

| meter | 2026-09-08 morning | now | evidence |
|---|---|---|---|
| landing gate (`make test`) | 510 s serial | **175 s** on landed trunk (166 s at fence) | 3ea3803e; gate-trunk-timing |
| gate at landing time | rerun after GO, ~8 min serial | **consumed** from the prewarm run beside the review | e34e86da, 3ea3803e `gates=consumed` |
| battery | 839 s (yesterday) → 164 s | 165 s | unchanged tonight |
| routine landing wall | 12–16 min | **~10 min**, of which Sol's review is ~9.5 | ship 594 s |
| ship versions installed | v3 | v3.3, v3.4, v3.5, v3.6 | 142 fixture rows, 0 mismatches |

## 2. Wins and losses

**Wins**
- Gates during review, witnessed twice in the field: the prewarm runs beside Sol on a disposable worktree; land re-derives the receipt on the merged tree and consumes it when identical.
- The gate is one coordinator (the battery's, generalized), width derived per box from cores and live memory, workers admitted box-wide through abstract Unix-socket slots that no path can delete, serial only as a loud NOT-A-GATE debugging entrance. No flag to forget.
- Censuses derived, never remembered: lane manifest and intent ledger set-equal to the tree, the deftest ledger one fully qualified name per line, regenerate refuses under make. The five-landing pin tripwire is dead.
- Sol's self-fix path fired in the field for the first time (GO-WITH-FIX on the census branch); ship refused it correctly on a wrong candidate binding, and the patch landed as a Sol-authored commit.
- Row 5 split: of the reads the old meter called answerable, 1 of 15 is "the receipt did not say"; the rest is "I will not take the tool's word." E5 as registered cancelled on evidence; the trust residual belongs to the recorder user on anvil2.
- anvil2 provisioned (55 min, five seat users, toolchains pinned) with a finding: 2.5–3× slower per core than Anvil. Dev and records only, never timing.

**Losses**
- The 9 arrived 41 minutes after the four-hour window. Six ship runs on one branch; four holds were the fence doing its job on real defects, one was Sol's content filter refusing a brief that read like an exploit.
- Two stub fixtures blessed broken consumers (codex banner on stdout vs stderr; prewarm receipt path and shape). Both reached the field before being caught.
- An Astra run died on "model at capacity" mid-task; a builder stopped mid-gate and had to be resumed by pid.
- The harness killed a sleeping waiter twice on its own memory heuristic with 15–20 GB free.

## 3. Learnings → ratchets

| learning | ratchet |
|---|---|
| A fix block must bind to the sealed candidate, not the branch tip | ship v3.4: accepts sealed or composing tip; brief header prints the sealed sha |
| A stop path that leaves the fence worktree dirty kills the next run with the wrong reason | v3.4: fence released only after archiving every doomed file; `fence-worktree-dirty` named |
| Path-based semaphores can always be unlinked and recreated | branch: abstract-socket slots, no filesystem state (rung 5: unrepresentable) |
| A ship prewarm that omits a stage must fail closed | v3.5/v3.6: land consumes only when ran ∪ phase-B equals `print-gate-stages` on the merged tree |
| Two parallel builders on one contract need the producer's real bytes as the consumer's fixture | memory `test-with-the-callers-real-bytes` addendum |
| Sol's filter refuses "attack/leak" wording in a brief | memory `sol-live-on-anvil-seat` addendum; briefs in verification language |
| A wall-clock ceiling inside a test is a load-sensitive oracle | TEST-ISO-007: declared 18,000 ms bound with five repetitions under width 8 |
| Seat identity is the commit identity, not the hostname | `seat-id` shared by tighten and seat-receipt; typed refusal when unset |

## 4. What's next
1. **The review is now the whole serial path** (~9.5 of ~10 min). Options to cost with Astra: review beside a smaller sealed diff; two reviewers on disjoint halves; a warm reviewer worktree. This is the only lever left on t.
2. Row 5: no more receipt facts. The recorder user on anvil2 (independent UID attesting receipts) is the trust answer; scope it as a tweezer.
3. Open notes from the red-team: the ~90 s window in which fence_busy sees nobody; the missing `SHIP-FENCE-RELEASE-FAILED` fixture row; socket slot names per network namespace rather than per user.
4. Parked by Gene: kiloclaw copy + Anvil→anvil2 migration plan (inb-02f511), Hetzner timing box (inb-3fa126), Frame 7 stats after rows.
