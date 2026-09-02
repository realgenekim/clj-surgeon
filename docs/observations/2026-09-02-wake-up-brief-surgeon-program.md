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
| correctness defect class (stale onset timestamp) | 0 of 9 | 6 of 9 | E5, one build, one prompt, pre-registered predicate, no judge; p about 0.009; the acceptance suite passed all six. Mechanism: every run had seen the reset line; the Surgeon arm chose the head-of-function guard shape 7 of 9 times, native 1 of 9, and that shape is defective 6 of 8 |

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
optional gave native speed because all three optional-arm agents made zero Surgeon calls:
given the choice, the agent declines the tool. The mandated agents obeyed on reads and
receipts (zero native .clj reads, zero re-reads after a write) and broke on writes in two of
three runs: reads went into the tool, writes went around it, and the price was 210 s of
wall and four new refusal classes. Every prompt lever moves compliance; none moves the cost
below native.

Large rung (21-owner cross-file clock hoist across 11 namespaces, the tool's advertised
shape), all eleven completed runs identical and correct on the acceptance suite:

| axis | native (4) | shipped (4) | mandate (3) |
|---|---|---|---|
| wall | 215 s | 457 s | 625 s |
| actions | 10.5 | 21.0 | 27.7 |
| how the writes happened | one apply_patch cell, +59/-34 over 11 files | 7.8 per-form MCP writes on top of one native patch | 10 on top of 1.7 |
| typed refusals | 0 | 6.0 | 14.7 |
| blind quality (2 judges, /20) | 19.4 | 16.8 | 17.3 |

Fan-out is the per-form write API's worst case, not its best: one patch does what N per-form
writes do, and two shipped diffs shipped nine times the line churn because the tool
re-prints every form it edits and reformats the untouched remainder.

## The gate's first field test: lost, on a byte-level mismatch (E1, measured after you woke)

Native plus the gate versus native, six a side, medium rung: 742 s against 330 s, 69 percent
of admit calls refused because the agents emit Codex's apply_patch grammar and the gate parsed
unified diff only; every run abandoned the mandate; no hazard caught in situ. Three red-team
rounds missed it because they fed the gate unified diffs. The fix is small (accept both
grammars) and the lesson is the doctrine's own: run the free-choice arm first.

## The cheapest win of the summer (measured after you woke)

Half of what agents do on a task, nothing asked for: git diff in 80 of 81 runs, git status,
bd create-claim-close arcs on throwaway worktrees, hand-run syntax probes. One paragraph
forbidding them (native, rung M, nine against twelve) cut wall 27 percent, actions 24
percent, tokens 32 percent, ritual sub-commands 91 percent, with acceptance flat (p 0.91).
Naming substitutes instead of forbidding did nothing. Decision for you: put the paragraph in
delegation prompts for throwaway worktrees; the text is in the big-aha log.

## The ideal tool shape (design position, logged at your request, receipt a3d2375)

Not a better per-form editor. Three shapes, each with the measurement that would confirm
it: an inspect that returns exactly what the next write needs so it replaces the locate
step; an intent verb over N owners where the tool does the fan-out and splices without
re-printing; and, first, a structural gate on native patches that validates and runs the
focused suite in one receipt, so the agent keeps its route and every native edit becomes a
verified one. Only the intent verb can make wall go positive; the gate is the smallest
change with value that does not depend on the agent choosing the tool.

## Three product changes the night points at (filed via the mayor)

1. Whole-form re-print must preserve source text outside the edited span.
2. A batch write verb: one intent across N owners in one call.
3. The refusal classes that no agent recovered from within their fields: invalid-intent-form
   (two thirds of all refusals at the medium rung) and invalid-compact-relation (every Surgeon
   run at the large rung), plus the insertion-gap refusal already fixed by surgeon1 tonight.


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
- The fleet's action item, which is a doctrine change and therefore yours: pull "Surgeon is
  available and expected" from fleet agent prompts (the delegation one-liner every Clojure
  agent prompt carries) until the three product fixes land, since it is a standing 2x tax
  with no measured return. Both reviewers recommend it; I have not changed any prompt.
- Whether to run one more cohort: rung L, native vs shipped, driven by a second caller
  (Claude) rather than Sol, because the caller is the one variable that never varied and
  the tax is inferred entirely from Sol. Needs Claude Code on Anvil with the MCP config and
  a subscription login; I did not build it unilaterally.
- Substitution was answered (s1): optional, the agent declines the tool; mandated, it
  complies on reads and escapes on writes. b2 closed the insertion-gap story: surgeon1's
  merged fix (main 2311cc09) shows no detected regression on the typed-refusal ledger at
  n=3, removes one refusal class shipped still emits, and is safe to promote from the
  ledger's standpoint; the hold decision sits with the mayor.

Full receipts: `docs/observations/2026-09-02-captains-log-bridge-wall-clock-ideal-program.md`.
