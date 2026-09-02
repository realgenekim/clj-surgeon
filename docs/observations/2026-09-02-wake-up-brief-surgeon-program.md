# Wake-up brief, 2026-09-02 (bridge, for Gene; draft to prune, not a blank page)

## The headline

On the verified shipped Surgeon build, on the medium task (bridge4 controls + mic gate), six
paired draws against native edits on Anvil:

| axis | native | shipped Surgeon | reading |
|---|---|---|---|
| wall | 367 s | 677 s | Surgeon 1.8x slower, direction held 5 of 6 pairs |
| tokens carried | 1.24 M | 2.30 M | the wall gap IS the token gap (rho 0.87) |
| non-test actions | 10.0 | 21.2 | Surgeon is layered on the native loop, not substituted for it |
| acceptance (failed assertions of 39) | 1.83 | 3.33 | native more conformant |
| blind clarity (2 judges, /20) | 17.8 | 17.1 | native slightly ahead, inside noise |
| correctness defect class (stale onset timestamp) | 0 of 6 | 3 of 6 | 6 Surgeon instances vs 0 native across the night |

The cost is not tool latency (tool execution is 3 to 4 percent of wall, 87 percent is model
time between actions). It is extra steps: the Surgeon arm still runs more shell calls than
native, adds 8.5 MCP calls, and applies a third as many patches.

## What was settled tonight

1. **The wave build's clarity deficit has a cause.** The insertion-gap fix introduces a refusal
   (`ambiguous-insertion-gap`) that fires on ordinary code with comments between top-level
   forms: 3 of 3 runs on that build, 0 elsewhere. Agents escape it (new file, or give up). The
   overlap fix is exonerated and halves refusals. Beads: clj-surgeon-f5e (P1), -vcz (P2).
2. **The largest adoption cost is the intent grammar.** `invalid-intent-form` is two thirds of
   every refusal the agents drew, on every build. Bead clj-surgeon-xio (P1).
3. **Prompt rules do not fix it.** Turn budget, report-only counting, deliberate 3-plan
   selection: none separates on quality at n=3; merely asking the agent to count its actions
   removed 26 percent of them; the budget arithmetic added overruns and dropped tests.
4. **Two receipts of the night were withdrawn** because the verifier was blind to its subject:
   every Anvil Surgeon row before 05:30Z called another seat's production server (the
   runner printed a sha it never read), and the "8 suite runs" count was a grep over
   mentions. Both are logged with their corrections; both earned ratchets.

## Decisions waiting for you

- Doctrine v2 "turns are the clock: count, don't budget; conformance gate mandatory"
  (inbox inb-beecb9). Ship to house rules or hold.
- Ratifications still queued: MCP-OP-INSERT-001..006, string-symbol outline contract,
  insert_pair design (from earlier tonight).
- Whether the program continues: the substitution question ("why does the agent not drop
  native steps when Surgeon is present") is now the hill; l1 (21-owner large rung) and b2
  (bisect replication) are queued and will run without you. The fleet's diagnosis and the
  two cheapest discriminating experiments are in the captain's log's last receipts.

Full receipts: `docs/observations/2026-09-02-captains-log-bridge-wall-clock-ideal-program.md`.
