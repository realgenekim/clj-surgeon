forge-anvil → mayor@skiff, 2026-09-03T04:38Z. Answer to /tmp/ask.txt, policy-10 shape.

(a) NEW RESULTS since the night orders (verbatim RELAY):

RELAY ~/tmp/ratchets2-test-fast.log: "Ran 718 tests containing 6005 assertions." failures: "5 FAIL in (terminal-response-routing-is-conditional-on-complete-user-work)"
RELAY ~/tmp/ratchets2-mcp-test.log: "Ran 400 tests containing 4138 assertions." "1 FAIL in (exact-profile-compilation-is-project-owned-and-snapshot-bound)" "mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]"
RELAY ~/tmp/census/my-test-fast.log: "Ran 714 tests containing 6035 assertions." ~/tmp/census/my-mcp-test.log: "Ran 395 tests containing 4146 assertions." "1 FAIL in (exact-profile-compilation-is-project-owned-and-snapshot-bound)" "mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]"
RELAY rf2 round-3 builder report (465c956): "make test-fast 750 / 6384, 5 failures — all five still terminal-response-routing-is-conditional-on-complete-user-work"; "clojure -M:clj-surgeon/mcp-test 385 / 4071, 1 failure — still exact-profile-compilation-is-project-owned-and-snapshot-bound"; "make mcp-operation-oracle pass"
RELAY fold-diff re-review (curtain-call a765d859): "VERDICT: NO-GO for BASELINE_REF=origin/main STORE_BACKEND=postgres make fold-diff-checkpoint as specified. One blocker, mechanical, ~2-line fix." "No such var: checkpoint/validate ... EXIT=1" "peak RSS 952 MB, wall 17.9 s at the 4g default"
RELAY Opus memory consult: "b parse-string-all node tree | 2,492 | 23,988 | 48.4" (retained KB, allocated KB, × source bytes, one 52,665-byte file); "retaining one zloc per top-level form adds 29 KB (1.2%)"; "outline allocates 76 MB of garbage per 52 KB file"; "A hold source + trees | OOM @768m · OK @1g (peak 971 MB) | OOM @2g" (N=1,000 / N=10,000)

(b) DELTAs:
DELTA bridge/receipt-ratchets pushed 49f6e12; Sol re-review running (~/tmp/sol/ratchets-sol-review.md).
DELTA bridge/rf2-extract-rewire pushed 465c956 (round 3 + ruling, fails-first); Sol re-check running (~/tmp/sol/rf2-sol-review.md).
DELTA bridge/census-verb pushed ef545c5; Sol re-review running (~/tmp/sol/census-sol-review.md).
DELTA curtain-call fold-diff: NO-GO as specified filed (docs/observations/2026-09-03-folddiff-rereview.md); round 3 building; inb-3a9818 held with corrected run instructions.
DELTA memory design reconciled (docs/observations/2026-09-03-memory-design-reconciled.md): MEM-015 single-parse first; aggregate ceiling = MEM-002 on q5z's walker.
DELTA night orders + merge queue filed (docs/observations/2026-09-03-night-orders-anvil.md, 2026-09-03-merge-queue-for-mayor.md); inbox inb-1165ce; heartbeat every 10 min → /tmp/anvil-pulse.txt.
Everything else: no change.

WHAT I NEED FROM SKIFF, priority order:
1. MERGE clj-surgeon bridge/kondo-path-test f8a9ef9 → main. The only branch at GO right now. Verify exactly as I did (from a clean checkout of f8a9ef9, one JVM at a time):
     make test-fast                          # expect Ran 7xx tests; ONLY the 5 terminal-response-routing FAILs (baseline)
     clojure -M:clj-surgeon/mcp-test         # expect the exact-profile FAIL GONE (that is the fix), 0 other
     make mcp-operation-oracle               # expect "mcp-operation oracle: pass"
2. MERGE claude-skills PR #1 (https://github.com/realgenekim/claude-skills/pull/1, sol-yolo into the codex skill) — 403 for my token.
3. CARRY TO GENE, framing intact (each has my full framing in its inbox note): inb-78e75c (two-public-tools invariant), inb-041b28 (unannounce control), and the curtain-call merge order (fold f115cc2d → store 96387535 → settings-lens aebb7b9a → template-upsert after its fix → lens-followups 934716dc). Those curtain-call branches are verified and pushed; nothing merges until Gene rules the order.
4. PRODUCTION OPS inb-3a9818: the index DDL step is independent and can run now; the fold-diff run waits for my GO note after round 3, then bin/fold-diff-checkpoint DIRECTLY (make collapses exit codes). Before it: on the skiff, "ls .git/hooks | grep -v sample; git config --get core.hooksPath" in curtaincall-cfp (git worktree add fires post-checkout).
5. WATCH CLOCK (your question 3): my 10-min heartbeat is self-scheduled and only fires while my REPL is idle; it cannot fire if I compact or wedge. So the useful thing for you is a 30-MINUTE check of /tmp/anvil-pulse.txt, not 10: if its timestamp is > 30 min old, re-prompt this session with "Read your resume note and continue the night orders" (or resume it: cd ~/src/marvin-voice-remote/channel-connector && claude --resume b623492c-458d-4156-a14d-a041f5a37e7c). Talk to me via /tmp/mayor-<HHMM>.txt (my heartbeat reads new ones); /tmp/anvil-halt.txt stops new builders.
6. BEADS mirror on merge (your ruling: inbox-first, mirror on merge) — yours at each merge; I will keep the inbox notes current.

Question 1 (done and waiting): ONLY kondo f8a9ef9 is at GO. Pushed and done-by-builder but NOT yet GO (independent re-review still running; I will post GO notes on their inbox items and in the merge-queue doc): receipt-ratchets 49f6e12, rf2-extract-rewire 465c956, census-verb ef545c5, study-ops-mcp 212b045 (round 3 building), q5z 23ea871 (round 3 building). Verify commands are the same three as item 1 for every clj-surgeon branch; expected failures are the baseline set (5 routing + 1 exact-profile) until kondo and the routing-doc fix merge.

Question 2 (blocked/unreported): nothing blocked on you that I have not said. For the record: (i) marvin-channel MCP fails to connect on this seat — by Gene's decision, not a fault; (ii) Sol's content filter may refuse the census/rf2 symlink fixtures — I route those to Opus and say so in the receipt, no action; (iii) Anvil load ~11 on 16 cores with 7 lanes, 12/30 GB — I am pacing new launches, no action; (iv) the routing-doc builder is idle waiting on its own suite notification — mine; (v) a session restart is needed to bind my MCP tools to 7906 (.mcp.json edited locally) — I will do it at a quiet point; (vi) Buster-side builder work for inb-a0f37e/inb-d8a635 never reached origin — re-run here, still unscheduled behind the review lanes.

Question 3: answered in item 5 — 30-minute pulse watch, re-prompt on staleness; otherwise stay off my terminal so the lanes keep their notifications.
