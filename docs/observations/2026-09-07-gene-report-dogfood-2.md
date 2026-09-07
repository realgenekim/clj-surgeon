# Gene report — dogfood-2 block (2026-09-07T10:52Z; five-hour budget from ~07:00Z, closed 10:5xZ)

Order: "Let's repeat dogfooding with safe refactor of curtain call and Marvin dictation. Use management system to observe friction and use as opportunity to make surgeon perfect." "Time budget 5 hours."

## 1. vs-NATIVE performance (say plainly: no controlled pair was measured)

| task | native caller | tool caller | tool-written sites | first attempt | caveat |
|---|---|---|---|---|---|
| Curtain Call: shared speaker-identity helper (3 owners) | 215.1 s to green (1021 tests) | 141.7 s to green | 3 of 3 | yes, 0 refusals | different callers, n=1 each, native paid a refusal detour first; NOT a pair |
| Marvin dictation: one JSON write policy (21 sites / 9 files) | 194.9 s (phase 1) / 107.8 s (phase 2 native fallback) | verb refused twice on the old build | 0 → after the fix: 21 of 21 | after fix: yes | migration CALL 4.3 s live; no timed native arm for the call alone |

Server time inside every green run was under 5 s. The wall is the caller's, as measured all week.

## 2. Block ledger

| block | kind | wall | outcome |
|---|---|---|---|
| dogfood-2 phase 1 (two Opus callers, MCP client) | tock | 07:03–07:11Z | both green natively; 0 tool sites; **P0: session MCP client bound to port 7888** (config loaded before the repoint) |
| dogfood-2 phase 2 (same tasks via raw HTTP to 7906) | tock | 07:15–07:18Z | CC fan-out first attempt through the tool; mvr alias_migration two false refusals |
| alias fix lane | tock | 07:22–09:46Z | **LANDED b3a8371c → 9b7e22f8** after 4 Sol rounds |
| expect-guard lane | tock | 07:27–10:45Z | **LANDED 4711be05 → 2d1a3c98** after 5 Sol rounds + battery receipt |
| live proofs on 7906 | — | 09:54Z, 10:51Z | alias replay 9 files / 21 sites / 0 refusals; guard refuses a wrong count in 12.6 ms with 0 bytes written, right count commits |

## 3. Wins and losses

Wins:
- Two real defects found by real refactors and FIXED ON THE TRUNK the same morning, each proven live on the seat's server against the caller's original bytes. The alias verb refused the exact incremental case it exists for; the fan-out verb silently discarded the caller's expect counts (a mis-stated fan-out size returned success). Both are now typed refusals with corrected next_call.
- Sol's executed reviews found four more holes the builders' own tests missed (alias/refer conflation → silent mismigration; comment deletion; policy bypass; programs under-count where a request declaring 1 edit committed 2). Every one is a witness now.
- The Curtain Call fan-out is the cleanest tool run to date: inspect's per-owner breakdown made the request first-attempt-correct in one 143 ms call.

Losses:
- **This seat has been calling port 7888 since 2026-09-03.** Every Surgeon call from the Claude client, all week, hit the other seat's production server (nothing written; all refused). A config repointed after session start never reached the running client. Fix is a session restart (yours to make); the wrapper ~/bin/surgeon-call names its port and refuses the others.
- No controlled native pair was run; every wall above is n=1. The wins are correctness receipts, not speed claims.
- Review cost: 9 Sol rounds for two fixes, plus one round I killed myself by launching a second review on the single-lane fence.

## 4. Learnings → ratchets
- MCP client config binds at session start: boot check (.mcp.json mtime vs session start) or first-call default-root probe. inb-69ac74. Memory written.
- A refusal that says "does not exist" about something you can ls means the OTHER side cannot see it: split not-found / not-permitted, publish allowed_roots. inb-6b045b, inb-9d21c6, inb-c3356a (verify on trunk first — they describe the 7888 build).
- expect is a guard, never bookkeeping (landed; EARS 039–042).
- fence-run is single-lane (memory). Battery-fresh counts docs commits (memory). `clojure -X` launcher pid ≠ the JVM holding the port (memory).
- Still open: mvr F3 whole-run refusal without a composed scope.exclude next_call (inb-42433c); CC F2/F3 verification flag semantics (inb-4e0009, inb-2cf9e6); pre-existing programs + delete_owners stale-subform (inb-0e8dc5).

## 5. What's next (recommendations)
1. Restart this session so the MCP client reloads .mcp.json (7906). Until then this seat uses the wrapper only.
2. Tag stable/2026-09-07b at 2d1a3c98 once you accept the two landings (both dogfood-proven live).
3. Decide the four voice-remote / Curtain Call branches: mvr fable/json-write-helper (native) and fable/json-write-helper-live (tool-written, identical modulo require order) — keep one; CC fable/speaker-identity-helper (native). None merged by this seat.
4. Next block: a controlled pair (same caller, same task, native vs tool) on the CC fan-out class — the one place the tool wrote everything first attempt.
