# Wake-up brief, 2026-09-02 (bridge, for Gene; draft to prune, not a blank page)

## The headline

On the verified shipped Surgeon build, on the medium task (bridge4 controls + mic gate), six
paired draws against native edits on Anvil:

| axis | native | shipped Surgeon | reading |
|---|---|---|---|
| wall | 367 s | 677 s | Surgeon 1.8x slower, 3.6 sd above the nine-identical-run floor |
| tokens carried | 1.24 M | 2.30 M | the wall gap IS the token gap (rho 0.87) |
| non-test actions | 10.0 | 21.2 | 3.8 sd above the floor; Surgeon is layered on the native loop, not substituted for it |
| acceptance (failed assertions of 39) | 1.83 | 3.33 | inside the noise floor (identical runs span 0 to 4); acceptance is a gate, not a score |
| blind clarity (2 judges, /20) | 17.8 | 17.1 | native slightly ahead, inside noise |
| correctness defect class (stale onset timestamp) | 0 of 6 | 3 of 6 | 6 Surgeon instances vs 0 native across the night |

The cost is not tool latency (tool execution is 3 to 4 percent of wall, 87 percent is model
time between actions). It is extra steps: the Surgeon arm still runs more shell calls than
native, adds 8.5 MCP calls, and applies a third as many patches.

## Why (fleet round 6 plus a free measurement)

Every primitive call in the twelve n1 rollouts was classified. The Surgeon arm keeps the
whole native workflow (102 native calls to native's 91) and adds 51 Surgeon calls that
displace almost nothing: native patches fall from 22 to 8, nothing else moves. It is not
receipt distrust (4 post-edit checks in 6 runs) and not literal-hunting (pre-edit 33 percent,
same ratio as native). It is layering. Both reviewers trace it to the write contract (the
tool needs exact literals it does not discover, so it can only sit on top of the native read
loop) and to permissive prompt framing ("Surgeon is available; plain edits are fine"). The
interface is not the cost: CLI-only on the same build matched MCP on wall (685 vs 725 s).

The next cohort answers the question directly: X = Surgeon optional with "fastest safe
completion"; Y = a substitution mandate (inspect replaces rg/sed, Surgeon writes replace
apply_patch, receipts are terminal). If Y closes the gap, the fix is the prompt. If Y is
obeyed and the gap stays, the tool's steps cost what they replace. If Y is not obeyed, the
contract cannot be substituted for.

## The discriminating experiment (s1, ran after the diagnosis)

| prompt | walls s | mean |
|---|---|---|
| Surgeon optional, "fastest safe completion" | 359, 383, 470 | 404 |
| native, same wave | 369, 481, 489 | 446 |
| shipped, Surgeon available and expected (the prompt used all night) | 616, 622, 787 | 675 |
| substitution mandate: inspect replaces grep, Surgeon writes replace patches, receipts terminal | 850, 891, 913 | 885 |

Mandating substitution made it slower still, 2.9 sd above the shipped mean. Making the tool
optional gave native speed. No prompt discipline recovers the cost; it is inherent to routing
edits through the tool on this task. Whether the optional-arm agents used the tool at all, and
whether the mandated agents obeyed, is in the captain's log's s1 receipts.

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
